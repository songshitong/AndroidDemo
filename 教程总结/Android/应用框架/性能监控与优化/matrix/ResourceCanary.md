https://blog.yorek.xyz/android/3rd-library/matrix-resource/
https://github.com/Tencent/matrix/wiki/Matrix-Android-ResourceCanary

ResourceCanary 的设计目标是准确的检测 Activity 泄露，检测泄露需要 hprof 文件，而该文件还可以用来进行冗余 Bitmap 的检测。
此外，考虑到有人工分析 hprof 文件的情况，需要将用户生成的 hprof 文件上传到服务端，而这个文件通常比较大，所以我们需要对 hprof 进行裁剪。
hprof 文件的分析过程可以放到服务器端。

另外，ResourceCanary 里面还提供了一个 Activity 级别泄露解决方案 ActivityLeakFixer ：它可以解决 IME 泄露问题；
以及在 Activity 销毁时清除所有 View 的 Drawable、Listener 等，这样泄露的就是空壳了。
同理我们还可以自己弄一个 Fragment 级别的方案


ResourcesCanary 在设计时就主要关注两个部分：
内存泄露的检测，以及 hprof 文件的裁剪与上报
服务端对 hprof 文件进行的内存泄露引用链检测，以及重复 Bitmap 数据检测


关于内存泄露检测
与常用的 LeakCanary 组件相比，ResourcesCanary 在内存泄露检测方面做了一些优化，来减少误报的几率。
LeakCanary:
VM并没有提供强制触发GC的API，通过System.gc()或Runtime.getRuntime().gc()只能“建议”系统进行GC，如果系统忽略了我们的GC请求，
   可回收的对象就不会被加入ReferenceQueue
将可回收对象加入ReferenceQueue需要等待一段时间，LeakCanary采用延时100ms的做法加以规避，但似乎并不绝对管用
ResourcesCanary:
增加一个一定能被回收的“哨兵”对象，用来确认系统确实进行了GC
直接通过WeakReference.get()来判断对象是否已被回收，避免因延迟导致误判


主要意思就是说，通过在 GC 前临时创建一个哨兵对象，GC 后判断哨兵对象是否仍然存活，来确定是否已经发生了 GC 动作。
这个想法似乎很好，但是在2021年的一次更新中，去掉了这个优点：
```
//            final WeakReference<Object[]> sentinelRef = new WeakReference<>(new Object[1024 * 1024]); // alloc big object
            triggerGc();
//            if (sentinelRef.get() != null) {
//                // System ignored our gc request, we will retry later.
//                MatrixLog.d(TAG, "system ignore our gc request, wait for next detection.");
//                return Status.RETRY;
//            }
```
原因是 minor gc 可能不会使哨兵对象回收，得 full gc 才可以。如果 minor gc 回收不了哨兵对象的话，会使后面的内存泄露检测无法进行，这就会导致漏报。
漏报比误报要严重。
关于取消哨兵机制的原因，详细可以查看Matrix-issues#585。
https://github.com/Tencent/matrix/issues/585#issuecomment-850234705
由于哨兵机制失效，导致 ResourcesCanary 需要以 1 min 的间隔进行轮询，如果某项泄露累计检测到了10次，则触发内存泄露。


activity泄漏检测
matrix\matrix-android\matrix-resource-canary\matrix-resource-canary-android\src\main\java\com\tencent\matrix\resource\watcher\ActivityRefWatcher.java
activity监控
com/tencent/matrix/resource/watcher/ActivityRefWatcher.java
```
 @Override
    public void start() {
        stopDetect();
        final Application app = mResourcePlugin.getApplication();
        if (app != null) {
            app.registerActivityLifecycleCallbacks(mRemovedActivityMonitor);
            scheduleDetectProcedure();
            MatrixLog.i(TAG, "watcher is started.");
        }
    }
    
    private final Application.ActivityLifecycleCallbacks mRemovedActivityMonitor = new EmptyActivityLifecycleCallbacks() {

        @Override
        public void onActivityDestroyed(Activity activity) {
            //将activity加入mDestroyedActivityInfos，通知检测线程 
            pushDestroyedActivityInfo(activity);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {  //触发gc
                    triggerGc();
                }
            }, 2000);
        }
    };
    
  private void pushDestroyedActivityInfo(Activity activity) {
        ...
        final DestroyedActivityInfo destroyedActivityInfo
                = new DestroyedActivityInfo(key, activity, activityName);
        //ConcurrentLinkedQueue<DestroyedActivityInfo> mDestroyedActivityInfos;        
        mDestroyedActivityInfos.add(destroyedActivityInfo);
        synchronized (mDestroyedActivityInfos) {
            mDestroyedActivityInfos.notifyAll();
        }
        MatrixLog.d(TAG, "mDestroyedActivityInfos add %s", activityName);
    }       
```

检测任务线程
```
public void onForeground(boolean isForeground) {
        if (isForeground) {
          ...
            mDetectExecutor.setDelayMillis(mFgScanTimes); //每次执行1分钟
            mDetectExecutor.executeInBackground(mScanDestroyedActivitiesTask);
        } ...
    }
    
com/tencent/matrix/resource/watcher/RetryableTaskExecutor.java 
 public void executeInBackground(final RetryableTask task) {
        postToBackgroundWithDelay(task, 0);
    }

 private void postToBackgroundWithDelay(final RetryableTask task, final int failedAttempts) {
        mBackgroundHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                RetryableTask.Status status = task.execute(); //执行完成后重复执行
                if (status == RetryableTask.Status.RETRY) {
                    postToBackgroundWithDelay(task, failedAttempts + 1);
                }
            }
        }, mDelayMillis);
    }       
```
检测任务 mScanDestroyedActivitiesTask
```
 private final RetryableTask mScanDestroyedActivitiesTask = new RetryableTask() {

        @Override
        public Status execute() {
            //mDestroyedActivityInfos进行阻塞等待
            // If destroyed activity list is empty, just wait to save power.
            if (mDestroyedActivityInfos.isEmpty()) {
                MatrixLog.i(TAG, "DestroyedActivityInfo is empty! wait...");
                synchronized (mDestroyedActivityInfos) {
                    try {
                        while (mDestroyedActivityInfos.isEmpty()) {
                            mDestroyedActivityInfos.wait();
                        }
                    } ...
                }
                MatrixLog.i(TAG, "DestroyedActivityInfo is NOT empty! resume check");
                return Status.RETRY;
            }
            //debug模式，重试
            // Fake leaks will be generated when debugger is attached.
            if (Debug.isDebuggerConnected() && !mResourcePlugin.getConfig().getDetectDebugger()) {
                MatrixLog.w(TAG, "debugger is connected, to avoid fake result, detection was delayed.");
                return Status.RETRY;
            }

//            final WeakReference<Object[]> sentinelRef = new WeakReference<>(new Object[1024 * 1024]); // alloc big object
            triggerGc();
//            if (sentinelRef.get() != null) {
//                // System ignored our gc request, we will retry later.
//                MatrixLog.d(TAG, "system ignore our gc request, wait for next detection.");
//                return Status.RETRY;
//            }

            final Iterator<DestroyedActivityInfo> infoIt = mDestroyedActivityInfos.iterator();

            while (infoIt.hasNext()) {
                final DestroyedActivityInfo destroyedActivityInfo = infoIt.next();
               .。。
                triggerGc();
                //activity回收了
                if (destroyedActivityInfo.mActivityRef.get() == null) {
                    // The activity was recycled by a gc triggered outside.
                    MatrixLog.v(TAG, "activity with key [%s] was already recycled.", destroyedActivityInfo.mKey);
                    infoIt.remove();
                    continue;
                }
                //没有回收，次数+1
                ++destroyedActivityInfo.mDetectedCount;
                //小于10次
                if (destroyedActivityInfo.mDetectedCount < mMaxRedetectTimes
                        && !mResourcePlugin.getConfig().getDetectDebugger()) {
                    // Although the sentinel tell us the activity should have been recycled,
                    // system may still ignore it, so try again until we reach max retry times.
                    MatrixLog.i(TAG, "activity with key [%s] should be recycled but actually still exists in %s times, wait for next detection to confirm.",
                            destroyedActivityInfo.mKey, destroyedActivityInfo.mDetectedCount);

                    triggerGc();
                    continue;
                }

                MatrixLog.i(TAG, "activity with key [%s] was suspected to be a leaked instance. mode[%s]", destroyedActivityInfo.mKey, mDumpHprofMode);
                ...
                //检测次数超过10次，发布相关信息
                if (mLeakProcessor.process(destroyedActivityInfo)) {
                    MatrixLog.i(TAG, "the leaked activity [%s] with key [%s] has been processed. stop polling", destroyedActivityInfo.mActivityName, destroyedActivityInfo.mKey);
                    infoIt.remove();
                }
            }

            return Status.RETRY;
        }
    };
```


ActivityLeakFixer相关
com/tencent/matrix/resource/ResourcePlugin.java
```
public static void activityLeakFixer(Application application) {
        application.registerActivityLifecycleCallbacks(new EmptyActivityLifecycleCallbacks() {
            @Override
            public void onActivityDestroyed(Activity activity) {
                ActivityLeakFixer.fixInputMethodManagerLeak(activity);
                ActivityLeakFixer.unbindDrawables(activity);
                ActivityLeakFixer.fixViewLocationHolderLeakApi28(activity);
            }
        });
    }
```
matrix-resource-canary\matrix-resource-canary-android\src\main\java\com\tencent\matrix\resource\ActivityLeakFixer.java
```
 public static void fixInputMethodManagerLeak(Context destContext) {
        final long startTick = System.currentTimeMillis();

        do {
           ...
            final InputMethodManager imm = (InputMethodManager) destContext.getSystemService(Context.INPUT_METHOD_SERVICE);
            ...
            //ViewRootImpl mCurRootView;   
            //mServedView mCurRootView.getImeFocusController().getServedView()
            //mNextServedView    mCurRootView.getImeFocusController().getNextServedView()
            final String[] viewFieldNames = new String[]{"mCurRootView", "mServedView", "mNextServedView"};
            for (String viewFieldName : viewFieldNames) {
                try {
                    final Field paramField = imm.getClass().getDeclaredField(viewFieldName);
                    if (!paramField.isAccessible()) {
                        paramField.setAccessible(true);
                    }
                    final Object obj = paramField.get(imm);
                    if (obj instanceof View) {
                        final View view = (View) obj;
                        // Context held by InputMethodManager is what we want to split from reference chain.
                        if (view.getContext() == destContext) {
                            // Break the gc path.  //截断view的引用
                            paramField.set(imm, null);
                        } else {
                            // The first time we meet the context we don't want to split indicates that the rest context
                            // is not need to be concerned, so we break the loop in this case.
                            MatrixLog.i(TAG, "fixInputMethodManagerLeak break, context is not suitable, get_context=" + view.getContext() + " dest_context=" + destContext);
                            break;
                        }
                    }
                } catch (Throwable thr) {
                    MatrixLog.e(TAG, "failed to fix InputMethodManagerLeak, %s", thr.toString());
                }
            }
        } while (false);

        MatrixLog.i(TAG, "fixInputMethodManagerLeak done, cost: %s ms.", System.currentTimeMillis() - startTick);
    }


public static void unbindDrawables(Activity ui) {
        final long startTick = System.currentTimeMillis();
        if (ui != null && ui.getWindow() != null && ui.getWindow().peekDecorView() != null) {
            final View viewRoot = ui.getWindow().peekDecorView().getRootView();
            try {
                unbindDrawablesAndRecycle(viewRoot);
                if (viewRoot instanceof ViewGroup) { //将view从group移除
                    ((ViewGroup) viewRoot).removeAllViews();
                }
            } catch (Throwable thr) {
                MatrixLog.w(TAG, "caught unexpected exception when unbind drawables.", thr);
            }
        } else {
            MatrixLog.i(TAG, "unbindDrawables, ui or ui's window is null, skip rest works.");
        }
        MatrixLog.i(TAG, "unbindDrawables done, cost: %s ms.", System.currentTimeMillis() - startTick);
    }

private static void unbindDrawablesAndRecycle(View view) {
        if (view == null) {
            return;
        }
        if (view.getContext() == null) {
            return;
        }

        recycleView(view);
        //清除view的drawable及drawable的回调
        if (view instanceof ImageView) {
            recycleImageView((ImageView) view);
        }
        //处理drawable，setKeyListener等监听
        if (view instanceof TextView) {
            recycleTextView((TextView) view);
        }

        if (view instanceof ProgressBar) {
            recycleProgressBar((ProgressBar) view);
        }

        if (view instanceof android.widget.ListView) {
            recycleListView((android.widget.ListView) view);
        }

        if (view instanceof FrameLayout) {
            recycleFrameLayout((FrameLayout) view);
        }
        //去除Divider的drawable
        if (view instanceof LinearLayout) {
            recycleLinearLayout((LinearLayout) view);
        }

        if (view instanceof ViewGroup) {
            recycleViewGroup((ViewGroup) view);
        }

//        cleanContextOfView(view);
    }  
    

//移除各种事件，drawable,drawable的监听
 private static void recycleView(View view) {
        if (view == null) {
            return;
        }

        boolean isClickable = view.isClickable();
        boolean isLongClickable = view.isLongClickable();

        try {
            view.setOnClickListener(null);
        } catch (Throwable ignored) {
            // Ignored.
        }

        try {
            view.setOnCreateContextMenuListener(null);
        } catch (Throwable ignored) {
            // Ignored.
        }

        try {
            view.setOnFocusChangeListener(null);
        } catch (Throwable ignored) {
            // Ignored.
        }

        try {
            view.setOnKeyListener(null);
        } catch (Throwable ignored) {
            // Ignored.
        }

        try {
            view.setOnLongClickListener(null);
        } catch (Throwable ignored) {
            // Ignored.
        }

        try {
            view.setOnClickListener(null);
        } catch (Throwable ignored) {
            // Ignored.
        }

        try {
            view.setOnTouchListener(null);
        } catch (Throwable ignored) {
            // Ignored.
        }

        if (view.getBackground() != null) {
            view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {

                }

                @Override
                public void onViewDetachedFromWindow(View v) {
                    try {
                        v.getBackground().setCallback(null);
                        v.setBackgroundDrawable(null);
                    } catch (Throwable ignored) {
                        // Ignored.
                    }
                    try {
                        v.destroyDrawingCache();
                    } catch (Throwable thr) {
                        // Ignored.
                    }
                    v.removeOnAttachStateChangeListener(this);
                }
            });
        }

        view.setClickable(isClickable);
        view.setLongClickable(isLongClickable);
    }      
```
修复androidP的泄漏
```
 /**
     * In Android P, ViewLocationHolder has an mRoot field that is not cleared in its clear() method.
     * Introduced in https://github.com/aosp-mirror/platform_frameworks_base/commit
     * /86b326012813f09d8f1de7d6d26c986a909d
     *
     * This leaks triggers very often when accessibility is on. To fix this leak we need to clear
     * the ViewGroup.ViewLocationHolder.sPool pool. Unfortunately Android P prevents accessing that
     * field through reflection. So instead, we call [ViewGroup#addChildrenForAccessibility] with
     * a view group that has 32 children (32 being the pool size), which as result fills in the pool
     * with 32 dumb views that reference a dummy context instead of an activity context.
     *
     * This fix empties the pool on every activity destroy and every AndroidX fragment view destroy.
     * You can support other cases where views get detached by calling directly
     * [ViewLocationHolderLeakFix.clearStaticPool].
     */
    public static void fixViewLocationHolderLeakApi28(Context destContext) {
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.P) {
            return;
        }

        try {
            Context application = destContext.getApplicationContext();
            if (sGroupAndOutChildren == null) {
                ViewGroup sViewGroup = new FrameLayout(application);
                // ViewLocationHolder.MAX_POOL_SIZE = 32
                for (int i = 0; i < 32; i++) {
                    View childView = new View(application);
                    sViewGroup.addView(childView);
                }
                sGroupAndOutChildren = new Pair<>(sViewGroup, new ArrayList<View>());
            }

            sGroupAndOutChildren.first.addChildrenForAccessibility(sGroupAndOutChildren.second);
        } catch (Throwable e) {
            MatrixLog.printErrStackTrace(TAG, e, "fixViewLocationHolderLeakApi28 err");
        }
    }
```



com/tencent/matrix/resource/config/ResourceConfig.java
内存泄露检测模式：
```
 public enum DumpMode {
        NO_DUMP, // report only
        AUTO_DUMP, // auto dump hprof
        MANUAL_DUMP, // notify only
        SILENCE_ANALYSE, // dump and analyse hprof when screen off
        FORK_DUMP, // fork dump hprof immediately
        FORK_ANALYSE, // fork dump and analyse hprof immediately
        LAZY_FORK_ANALYZE, // fork dump immediately but analyze hprof until the screen is off
    }
```
NO_DUMP：直接对内存泄露阶段检测到的泄露项名称进行上报，不进行引用链的分析，自然就不需要dump hprof。
AUTO_DUMP：检测到内存泄露之后，先dump并到子进程中进行hprof文件裁剪，最后将泄露项名称、裁剪后的hprof等信息打成zip进行上报
MANUAL_DUMP：检测到内存泄露之后，发送通知。用户点击通知之后进行dump、分析。这里dump与分析都是发生在fork出来的进程中。
SILENCE_ANALYSE：锁屏时进行dump、分析
FORK_DUMP：fork出子进程，在子进程中进行dump，然后进行裁剪上报。
FORK_ANALYSE：先 fork dump，然后在fork出来的进程中进行分析。
LAZY_FORK_ANALYZE：先 fork dump，然后在锁屏时进行分析

ResourcesCanary 提供的内存泄露检测机制比较多，但都是几种基本功能的组合：
1 是否需要dump hprof
 Debug接口直接dump
 利用COW机制的fork dump
2 是否需要在客户端进行hprof文件的分析：
 不需要客户端分析（dump后裁剪hprof文件，带文件进行上报）
 需要客户端分析：
  使用HAHA分析
  使用native代码分析

todo
fork dump以及hprof文件裁剪  https://blog.yorek.xyz/android/3rd-library/hprof-shrink/

如何从 hprof 文件中解析出一个对象的引用链
1 假设，已知了一个Activity是泄露的，想要找到这个对象的引用链。那么，可以进行如下操作：
2 先通过 Activity 的全名在 STRING Record (也就是字符串池)中找到对应的 string id
通过 string id 在 LOAD CLASS Record 中找到对应的class id。有了泄露 Activity 的 class id 之后，在堆数据区的子 tag INSTANCE DUMP 中用
activity class id 找出该Activity类的所有实例信息。
当然，我们可以通过判断WeakReference里面的Activity是否存在，来获取到内存泄露发生时的Activity对象。这样可以避免对全量的Activity实例做引用链分析，
可以减少排查范围。
3 我们从 GC Root 这些对象出发（这些对象会在HEAP DUMP/HEAP DUMP SEGMENT Record中，以子tag ROOT_xxx 打头），
在子 tag CLASS DUMP 中遍历 GC Roots 的class id 以及所有的 static fields、instance fields。
static fields由field string id、type、value组成，当static fields会在类型是Object时，value直接是object id，这点比较方便。
instance fields只包含field string id以及type，具体对象里面的instance fields具体值，需要从子tag INSTANCE DUMP 中进行解析。
4 然后依据 GC Root class id，在子 tag INSTANCE DUMP 找出该 GC Root 类型的所有实例信息。
对于具体的实例来说，一条引用路径就是对象 A 里面的 static field 或者 instance field B 对应的对象引用了对象 A；
对象 B 里面的 static field 或者 instance field C 引用了 B。这样引用链就形成了。
5 对所有的 GC Root 对象，依次遍历实例所有的 static field 和 instance field，看看 field object id 是不是对应的上 activity object id。
若对应的上，说明这就找到了一条引用链；若对应不上，则继续对该实例里面的field object进行一样的操作，直到找不到可以进行下一步的对象，
又或者是找到了目标对象。这一步骤可以使用广度优先算法来实现

实际上在Matrix中，会判断DestroyedActivityInfo#mActivityRef这个弱引用的referent存不存在来断定该对象对应的Activity是否已经被回收。
所以，Matrix会先找到DestroyedActivityInfo对应的字符串ID，通过字符串ID找到class id，通过class id找到所有的 instance，
然后对instance里面的数据进行上面这个过滤。
com/tencent/matrix/resource/analyzer/model/DestroyedActivityInfo.java
```
//activity onDestroy后添加进WeakReference，监控是否被回收
public class DestroyedActivityInfo {
    public final String mKey;
    public final String mActivityName;

    public final WeakReference<Activity> mActivityRef;
    public int mDetectedCount = 0;

    public DestroyedActivityInfo(String key, Activity activity, String activityName) {
        mKey = key;
        mActivityName = activityName;
        mActivityRef = new WeakReference<>(activity);
    }
}
```

处理内存泄漏dump的过程
com/tencent/matrix/resource/processor/AutoDumpProcessor.java
```
 @Override
    public boolean process(DestroyedActivityInfo destroyedActivityInfo) {
        //通过Debug.dumpHprofData进行dump
        final File hprofFile = getHeapDumper().dumpHeap(true);
        if (hprofFile != null) {
            getWatcher().markPublished(destroyedActivityInfo.mActivityName);
            getWatcher().triggerGc();
            final HeapDump heapDump = new HeapDump(hprofFile, destroyedActivityInfo.mKey, destroyedActivityInfo.mActivityName);
            getHeapDumpHandler().process(heapDump);
        }....
        return true;
    }
    
  public File dumpHeap(boolean isShowToast) {
       ....
                Debug.dumpHprofData(hprofFile.getAbsolutePath());
             ...
    }   
```
com/tencent/matrix/resource/processor/BaseLeakProcessor.java
```
  protected AndroidHeapDumper.HeapDumpHandler getHeapDumpHandler() {
        if (mHeapDumpHandler == null) {
            mHeapDumpHandler = new AndroidHeapDumper.HeapDumpHandler() {
                @Override
                public void process(HeapDump result) {
                    CanaryWorkerService.shrinkHprofAndReport(mWatcher.getContext(), result);
                }
            };
        }

        return mHeapDumpHandler;
    }
```
com/tencent/matrix/resource/CanaryWorkerService.java
```
 private void doShrinkHprofAndReport(HeapDump heapDump) {
       ...
            new HprofBufferShrinker().shrink(hprofFile, shrinkedHProfFile);
           ....
    }
```
com/tencent/matrix/resource/hproflib/HprofBufferShrinker.java
```
 public void shrink(File hprofIn, File hprofOut) throws IOException {
       ...
            final HprofReader reader = new HprofReader(new BufferedInputStream(is));
            reader.accept(new HprofInfoCollectVisitor());
            // Reset.
            is.getChannel().position(0);
            reader.accept(new HprofKeptBufferCollectVisitor());
            // Reset.
            is.getChannel().position(0);
            reader.accept(new HprofBufferShrinkVisitor(new HprofWriter(os)));
        } ...
    }
```
裁剪的流程参考hprof裁剪

forkdump的处理
com/tencent/matrix/resource/processor/ForkDumpProcessor.java
```
 public boolean process(DestroyedActivityInfo destroyedActivityInfo) {
...
        final long dumpStart = System.currentTimeMillis();

        final File hprof = getDumpStorageManager().newHprofFile();
...
        if (!MemoryUtil.dump(hprof.getPath(), 600)) {
            MatrixLog.e(TAG, String.format("heap dump for further analyzing activity with key [%s] was failed, just ignore.",
                    destroyedActivityInfo.mKey));
            return true;
        }
      ...
        getWatcher().markPublished(destroyedActivityInfo.mActivityName);
        getWatcher().triggerGc();
        getHeapDumpHandler().process(
                new HeapDump(hprof, destroyedActivityInfo.mKey, destroyedActivityInfo.mActivityName));
        return true;
    }
```
dump的过程
com/tencent/matrix/resource/MemoryUtil.kt
```
 @JvmStatic
    @JvmOverloads
    fun dump(
        hprofPath: String,
        timeout: Long = DEFAULT_TASK_TIMEOUT
    ): Boolean = initSafe { exception ->
       ...
        return when (val pid = forkDump(hprofPath, timeout)) {
           ....
        }
    }

    private external fun forkDump(hprofPath: String, timeout: Long): Int
```
matrix-resource-canary\matrix-resource-canary-android\src\main\cpp\memory_util\memory_util.cpp
```
Java_com_tencent_matrix_resource_MemoryUtil_forkDump(JNIEnv *env, jobject,
                                                     jstring java_hprof_path,
                                                     jlong timeout) {
    const std::string hprof_path = extract_string(env, java_hprof_path);
    int task_pid = fork_task("matrix_mem_dump", timeout);
    if (task_pid != 0) {
        return task_pid;
    } else {
        /* dump */
        execute_dump(hprof_path.c_str());
        /* end */
        _exit(TC_NO_ERROR);
    }
}

static int fork_task(const char *task_name, unsigned int timeout) {
    auto *thread = current_thread();
    //暂停
    suspend_runtime(thread);
    //fork
    int pid = fork();
    if (pid == 0) {
        task_process = true;
        if (timeout != 0) {
            alarm(timeout);
        }
        prctl(PR_SET_NAME, task_name);
    } else {
        //恢复
        resume_runtime(thread);
    }
    return pid;
}

static void execute_dump(const char *file_name) {
    _info_log(TAG, "task_process %d: dump", getpid());
    update_task_state(TS_DUMP);
    dump_heap(file_name);
}
```
cpp/memory_util/symbol/symbol.cpp
```
void dump_heap(const char *file_name) {
    dump_heap_(file_name, -1, false);
}
```
dump_heap_通过xhook的dlopen调用art的  art::hprof::DumpHeap()
```
bool initialize_symbols() {
    android_version_ = android_get_device_api_level();
    if (android_version_ <= 0) return false;
    ds_mode(android_version_);

    auto *art_lib = ds_open("libart.so");
  ...
#define load_symbol(ptr, type, sym, err)                    \
    ptr = reinterpret_cast<type>(ds_find(art_lib, sym));    \
    if ((ptr) == nullptr) {                                 \
        _error_log(TAG, err);                           \
        goto on_error;                                      \
    }

    load_symbol(dump_heap_,
                void(*)(const char *, int, bool ),
                "_ZN3art5hprof8DumpHeapEPKcib",
                "cannot find symbol art::hprof::DumpHeap()")
    ....
}
```

analyze分析相关的  触发调用可以参考NativeForkAnalyzeProcessor.kt
cpp/memory_util/memory_util.cpp
```
execute_analyze(const char *hprof_path, const char *reference_key) {
    ...
    const int hprof_fd = open(hprof_path, O_RDONLY); //打开文件
   ....
    HprofAnalyzer::SetErrorListener(analyzer_error_listener);
    HprofAnalyzer analyzer(hprof_fd);
    ..
    return analyzer.Analyze([reference_key](const HprofHeap &heap) {
    // 从hprof中获取到类com.tencent.matrix.resource.analyzer.model.DestroyedActivityInfo的class id
        // 实际上也是先从字符串池中找到字符串id，然后通过字符串id在LOAD CLASS中找到class id
        const object_id_t leak_ref_class_id = unwrap_optional(
                heap.FindClassByName(
                        "com.tencent.matrix.resource.analyzer.model.DestroyedActivityInfo"),
                return std::vector<object_id_t>());
        std::vector<object_id_t> leaks;
         // 获取到DestroyedActivityInfo类的所有实例id，来源是INSTANCE DUMP或者OBJECT ARRAY DUMP
        for (const object_id_t leak_ref: heap.GetInstances(leak_ref_class_id)) {
           // 获取到DestroyedActivityInfo#mKey的值，这是一个字符串对象，所以保存的是对象id
            // 字符串对象的value数组是一个基本类型数组，值会保存在PRIMITIVE ARRAY DUMP中
            const object_id_t key_string_id = unwrap_optional(
                    heap.GetFieldReference(leak_ref, "mKey"), continue);
           // 从INSTANCE DUMP中获取到字符串对象的数据，解析出value所对应的object id
            // 然后在PRIMITIVE ARRAY DUMP中进行解析，最后得到字符串的字面量         
            const std::string &key_string = unwrap_optional(
                    heap.GetValueFromStringInstance(key_string_id), continue);
            // 如果解析出来的mKey的字面量不是发生泄露的那个Activity，则过滤掉        
            if (key_string != reference_key)
                continue;
            // 获取到DestroyedActivityInfo#mActivityRef的值，这是一个弱引用    
            const object_id_t weak_ref = unwrap_optional(
                    heap.GetFieldReference(leak_ref, "mActivityRef"), continue);
            // 获取到mActivityRef.referent的值，加入到leaks集合中        
            const object_id_t leak = unwrap_optional(
                    heap.GetFieldReference(weak_ref, "referent"), continue);
            leaks.emplace_back(leak);
        }
        return leaks;
    });
}
```
上面这段代码的操作主要就是获取到发生泄露的Activity的object id，然后将这些object id保存到集合leaks中，最后调用analyzer.Analyze进行引用链的解析。

main/analyzer.cpp
```
 std::optional<std::vector<LeakChain>>
    HprofAnalyzer::Analyze(
            const std::function<std::vector<object_id_t>(const HprofHeap &)> &leak_finder) {
        if (impl_ != nullptr) {
            return impl_->Analyze(leak_finder);
        } else {
            return std::nullopt;
        }
    }
 
    std::vector<LeakChain>
    HprofAnalyzerImpl::Analyze(
            const std::function<std::vector<object_id_t>(const HprofHeap &)> &leak_finder) {
        internal::heap::Heap heap;
        //hprof解析
        internal::reader::Reader reader(reinterpret_cast<const uint8_t *>(data_), data_size_);
        parser_->Parse(reader, heap, exclude_matcher_group_);
        return Analyze(heap, leak_finder(HprofHeap(new HprofHeapImpl(heap))));
    } 

 std::vector<LeakChain> HprofAnalyzerImpl::Analyze(const internal::heap::Heap &heap,
                                                      const std::vector<object_id_t> &leaks) {
        const auto chains = ({
            const HprofHeap hprof_heap(new HprofHeapImpl(heap));
             // 解析出leaks的引用链数组
            internal::analyzer::find_leak_chains(heap, leaks);
        });
        std::vector<LeakChain> result;
        // 将原始的引用链数组转为LeakChain对象
        for (const auto&[_, chain]: chains) {
            const std::optional<LeakChain> leak_chain = BuildLeakChain(heap, chain);
            if (leak_chain.has_value()) result.emplace_back(leak_chain.value());
        }
        return std::move(result);
    }      
```
看看internal::analyzer::find_leak_chains是如何完成引用链解析的。这个过程使用了广度优先算法，从GC ROOT开始遍历所有的对象，直到找到对象。
//todo hprof解析相关

首先造一个简单例子来指明几个字段的含义：
```
class A {
  private B b;
}
```
这描述的就是一条引用，其中referrer是A对象，referent是B对象，reference是instance类型、名称是b。
referent：被引用者
referrer：引用者
reference：引用对象，里面包含引用类型（static、instance、array element）、引用者名称的string id

matrix-hprof-analyzer\lib\analyzer\analyzer.cpp
```
  std::map<heap::object_id_t, std::vector<std::pair<heap::object_id_t, std::optional<heap::reference_t>>>>
    find_leak_chains(const heap::Heap &heap, const std::vector<heap::object_id_t> &tracked) {
        std::map<heap::object_id_t, std::vector<std::pair<heap::object_id_t, std::optional<heap::reference_t>>>> ret;

        for (const auto &leak: tracked) {
            // 保存对象id->ref_node_t的映射
            std::map<heap::object_id_t, ref_node_t> traversed;
            // 双端队列，BFS算法用到的结构
            std::deque<ref_node_t> waiting;
            
            // 从GC Root出发，构建引用链 
            for (const heap::object_id_t gc_root: heap.GetGcRoots()) {
                ref_node_t node = {
                        .referent_id = gc_root,
                        .super = std::nullopt,
                        .depth = 0
                };
                traversed[gc_root] = node;
                // 将所有的GC Roots加入到waiting中
                waiting.push_back(node);
            }

            bool found = false;
            while (!waiting.empty()) {
                const ref_node_t node = waiting.front();
                // waiting的队头数据
                waiting.pop_front();
                const heap::object_id_t referrer_id = node.referent_id;
                // 判断整个hprof中是否存在引用者为referrer_id的引用关系
               // 这些引用关系构建时依靠于CLASS DUMP里面的static field、INSTANCE DUMP里面的值以及OBJECT ARRAY DUMP里面的值
                if (heap.GetLeakReferenceGraph().count(referrer_id) == 0) continue;
                // 依次遍历这些引用关系
                for (const auto &[referent, reference]: heap.GetLeakReferenceGraph().at(referrer_id)) {
                    try {
                      // 这里判断深度是为了获取最短引用路径
                     // 如果traversed.at(referent)没有取到数据，会抛出out_of_range异常
                        if (traversed.at(referent).depth <= node.depth + 1) continue;
                    } catch (const std::out_of_range &) {}
                    // 构建下一层的引用链，super是用来溯源用的
                    ref_node_t next_node = {
                            .referent_id = referent,
                            .super = ref_super_t{
                                    .referrer_id = referrer_id,
                                    .reference = reference
                            },
                            .depth = node.depth + 1
                    };
                    traversed[referent] = next_node;
                    // 如果两个对象相等，说明找到了一条引用路径，此时跳转到traverse_complete，进行溯源
                    if (leak == referent) {
                        found = true;
                        goto traverse_complete;
                    } else {
                        waiting.push_back(next_node);
                    }
                }
            }
            traverse_complete:
            if (found) {
                ret[leak] = std::vector<std::pair<heap::object_id_t, std::optional<heap::reference_t>>>();
                std::optional<heap::object_id_t> current = leak;
                std::optional<heap::reference_t> current_reference = std::nullopt;
               // 进行溯源，完成之后ret[leak]里面保存的是从leak开始，到GC ROOT结束的引用链
                while (current != std::nullopt) {
                    ret[leak].push_back(std::make_pair(current.value(), current_reference));
                    const auto &super = traversed.at(current.value()).super;
                    if (super.has_value()) {
                        current = super.value().referrer_id;
                        current_reference = super.value().reference;
                    } else {
                        current = std::nullopt;
                    }
                }
                // 对这条引用链进行翻转，使其从GC ROOT开始
                std::reverse(ret[leak].begin(), ret[leak].end());
            }
        }

        return std::move(ret);
    }
```



重复Bitmap检测
由于我们可以从hprof文件中得到所有Bitmap的快照信息，所以我们也可以检测里面的mBuffer字段的值，对所有Bitmap的mBuffer进行计算，
就可以算出有那些重复的Bitmap。
实际上SquareUp有一个HAHA(Headless Android Heap Analyzer)库，可以帮助我们忽略解析hprof的过程。基于这个库，我们可以很方面的直接做各种二次开发。
但是需要注意一点，Bitmap#mBuffer在Android 8及以上就不存在于Java Heap中了，所以这个方案有API限制
com/tencent/matrix/resource/analyzer/CLIMain.java
```
 private static void doAnalyze() throws IOException {
       ...
            analyzeAndStoreResult(tempHprofFile, sdkVersion, manufacturer, leakedActivityKey, extraInfo);
       ....
    }
    

private static void analyzeAndStoreResult(File hprofFile, int sdkVersion, String manufacturer,
                                              String leakedActivityKey, JSONObject extraInfo) throws IOException {
        final HeapSnapshot heapSnapshot = new HeapSnapshot(hprofFile);
        final ExcludedRefs excludedRefs = AndroidExcludedRefs.createAppDefaults(sdkVersion, manufacturer).build();
        final ActivityLeakResult activityLeakResult
                = new ActivityLeakAnalyzer(leakedActivityKey, excludedRefs).analyze(heapSnapshot);

        DuplicatedBitmapResult duplicatedBmpResult = DuplicatedBitmapResult.noDuplicatedBitmap(0);
        if (sdkVersion < 26) {
            final ExcludedBmps excludedBmps = AndroidExcludedBmpRefs.createDefaults().build();
            duplicatedBmpResult = new DuplicatedBitmapAnalyzer(mMinBmpLeakSize, excludedBmps).analyze(heapSnapshot);
        } else {
           //android8.0及以上不支持
            System.err.println("\n ! SDK version of target device is larger or equal to 26, "
                    + "which is not supported by DuplicatedBitmapAnalyzer.");
        }
       ....
    }
```
com/tencent/matrix/resource/analyzer/DuplicatedBitmapAnalyzer.java
```
  @Override
    public DuplicatedBitmapResult analyze(HeapSnapshot heapSnapshot) {
       ...
            return findDuplicatedBitmap(analysisStartNanoTime, snapshot);
       ...
    }

private DuplicatedBitmapResult findDuplicatedBitmap(long analysisStartNanoTime, Snapshot snapshot) {
        //haha库
        final ClassObj bitmapClass = snapshot.findClass("android.graphics.Bitmap");
        ...
        final Map<ArrayInstance, Instance> byteArrayToBitmapMap = new HashMap<>();
        final Set<ArrayInstance> byteArrays = new HashSet<>();

        final List<Instance> reachableInstances = new ArrayList<>();
        for (Heap heap : snapshot.getHeaps()) {
            //跳过默认，系统的
            if (!"default".equals(heap.getName()) && !"app".equals(heap.getName())) {
                continue;
            }

            final List<Instance> bitmapInstances = bitmapClass.getHeapInstances(heap.getId());
            for (Instance bitmapInstance : bitmapInstances) {
                if (bitmapInstance.getDistanceToGcRoot() == Integer.MAX_VALUE) {
                    continue;
                }
                reachableInstances.add(bitmapInstance);
            }
            for (Instance bitmapInstance : reachableInstances) {
                ArrayInstance buffer = HahaHelper.fieldValue(((ClassInstance) bitmapInstance).getValues(), "mBuffer");
                if (buffer != null) {
                    // sizeof(byte) * bufferLength -> bufferSize
                    final int bufferSize = buffer.getSize();
                    //小图的bitmap跳过  5000
                    if (bufferSize < mMinBmpLeakSize) {
                        // Ignore tiny bmp leaks.
                        System.out.println(" + Skiped a bitmap with size: " + bufferSize);
                        continue;
                    }
                    if (byteArrayToBitmapMap.containsKey(buffer)) {
                        buffer = cloneArrayInstance(buffer);
                    }
                    //存为映射
                    byteArrayToBitmapMap.put(buffer, bitmapInstance);
                } else {
                    System.out.println(" + Skiped a no-data bitmap");
                }
            }
            byteArrays.addAll(byteArrayToBitmapMap.keySet());
        }
        ...
        final List<DuplicatedBitmapEntry> duplicatedBitmapEntries = new ArrayList<>();

        final List<Set<ArrayInstance>> commonPrefixSets = new ArrayList<>();
        final List<Set<ArrayInstance>> reducedPrefixSets = new ArrayList<>();
        commonPrefixSets.add(byteArrays);

        // Cache the values since instance.getValues() recreates the array on every invocation.
        final Map<ArrayInstance, Object[]> cachedValues = new HashMap<>();
        for (ArrayInstance instance : byteArrays) {
            cachedValues.put(instance, instance.getValues());
        }

        int columnIndex = 0;
        while (!commonPrefixSets.isEmpty()) {
            for (Set<ArrayInstance> commonPrefixArrays : commonPrefixSets) {
                Map<Object, Set<ArrayInstance>> entryClassifier = new HashMap<>(
                        commonPrefixArrays.size());

                for (ArrayInstance arrayInstance : commonPrefixArrays) {
                    final Object element = cachedValues.get(arrayInstance)[columnIndex];
                    if (entryClassifier.containsKey(element)) {
                        entryClassifier.get(element).add(arrayInstance);
                    } else {
                        Set<ArrayInstance> instanceSet = new HashSet<>();
                        instanceSet.add(arrayInstance);
                        entryClassifier.put(element, instanceSet);
                    }
                }

                for (Set<ArrayInstance> branch : entryClassifier.values()) {
                    if (branch.size() <= 1) {
                        // Unique branch, ignore it and it won't be counted towards duplication.
                        continue;
                    }

                    final Set<ArrayInstance> terminatedArrays = new HashSet<>();

                    // Move all ArrayInstance that we have hit the end of to the candidate result list.
                    for (ArrayInstance instance : branch) {
                        if (HahaHelper.getArrayInstanceLength(instance) == columnIndex + 1) {
                            terminatedArrays.add(instance);
                        }
                    }
                    branch.removeAll(terminatedArrays);

                    // Exact duplicated arrays found.
                    if (terminatedArrays.size() > 1) {
                        byte[] rawBuffer = null;
                        int width = 0;
                        int height = 0;
                        final List<Instance> duplicateBitmaps = new ArrayList<>();
                        for (ArrayInstance terminatedArray : terminatedArrays) {
                            final Instance bmpInstance = byteArrayToBitmapMap.get(terminatedArray);
                            duplicateBitmaps.add(bmpInstance);
                            if (rawBuffer == null) {
                                final List<FieldValue> fieldValues = ((ClassInstance) bmpInstance).getValues();
                                width = HahaHelper.fieldValue(fieldValues, "mWidth");
                                height = HahaHelper.fieldValue(fieldValues, "mHeight");
                                final int byteArraySize = HahaHelper.getArrayInstanceLength(terminatedArray);
                                rawBuffer = HahaHelper.asRawByteArray(terminatedArray, 0, byteArraySize);
                            }
                        }

                        final Map<Instance, Result> results = new ShortestPathFinder(mExcludedBmps)
                                .findPath(snapshot, duplicateBitmaps);
                       //构建引用链         
                        final List<ReferenceChain> referenceChains = new ArrayList<>();
                        for (Result result : results.values()) {
                            if (result.excludingKnown) {
                                continue;
                            }
                            ReferenceNode currRefChainNode = result.referenceChainHead;
                            while (currRefChainNode.parent != null) {
                                final ReferenceNode tempNode = currRefChainNode.parent;
                                if (tempNode.instance == null) {
                                    currRefChainNode = tempNode;
                                    continue;
                                }
                                final Heap heap = tempNode.instance.getHeap();
                                if (heap != null && !"app".equals(heap.getName())) {
                                    break;
                                } else {
                                    currRefChainNode = tempNode;
                                }
                            }
                            final Instance gcRootHolder = currRefChainNode.instance;
                            if (!(gcRootHolder instanceof ClassObj)) {
                                continue;
                            }
                            final String holderClassName = ((ClassObj) gcRootHolder).getClassName();
                            boolean isExcluded = false;
                            for (ExcludedBmps.PatternInfo patternInfo : mExcludedBmps.mClassNamePatterns) {
                                if (!patternInfo.mForGCRootOnly) {
                                    continue;
                                }
                                if (patternInfo.mPattern.matcher(holderClassName).matches()) {
                                    System.out.println(" + Skipped a bitmap with gc root class: "
                                            + holderClassName + " by pattern: " + patternInfo.mPattern.toString());
                                    isExcluded = true;
                                    break;
                                }
                            }
                            if (!isExcluded) {
                                referenceChains.add(result.buildReferenceChain());
                            }
                        }
                        if (referenceChains.size() > 1) {
                            duplicatedBitmapEntries.add(new DuplicatedBitmapEntry(width, height, rawBuffer, referenceChains));
                        }
                    }

                    // If there are ArrayInstances that have identical prefixes and haven't hit the
                    // end, add it back for the next iteration.
                    if (branch.size() > 1) {
                        reducedPrefixSets.add(branch);
                    }
                }
            }

            commonPrefixSets.clear();
            commonPrefixSets.addAll(reducedPrefixSets);
            reducedPrefixSets.clear();
            columnIndex++;
        }

        return DuplicatedBitmapResult.duplicatedBitmapDetected(duplicatedBitmapEntries, AnalyzeUtil.since(analysisStartNanoTime));
    }
    
```