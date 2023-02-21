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
matrix-resource-canary\matrix-resource-canary-android\src\main\java\com\tencent\matrix\resource\ActivityLeakFixer.java