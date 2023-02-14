

matrix-android-lib/src/main/java/com/tencent/matrix/Matrix.java
matrix初始化
```
Matrix.Builder builder = new Matrix.Builder(this);
builder.pluginListener(new TestPluginListener(this));
TracePlugin tracePlugin = configureTracePlugin(dynamicConfig);
builder.plugin(tracePlugin);
Matrix.init(builder.build());
Matrix.with().startAllPlugins();


 public Matrix build() {
            if (pluginListener == null) {
                pluginListener = new DefaultPluginListener(application);
            }
            return new Matrix(application, pluginListener, plugins, mLifecycleConfig);
 }
 
  public static Matrix init(Matrix matrix) {
       ...
        synchronized (Matrix.class) {
            if (sInstance == null) {
                sInstance = matrix;
            } ...
        }
        return sInstance;
    }

public class Matrix {
    private static volatile Matrix sInstance;
    private final HashSet<Plugin> plugins;
    private final Application     application;
    private Matrix(Application app, PluginListener listener, HashSet<Plugin> plugins, MatrixLifecycleConfig config) {
        this.application = app;
        this.plugins = plugins;
        MatrixLifecycleOwnerInitializer.init(app, config);
        ProcessSupervisor.INSTANCE.init(app, config.getSupervisorConfig());
        for (Plugin plugin : plugins) {
            plugin.init(application, listener);
        }
    }
    
    public void startAllPlugins() {
        for (Plugin plugin : plugins) {
            plugin.start();
        }
    }
 }    
```
plugin的构造
matrix-android-lib/src/main/java/com/tencent/matrix/plugin/Plugin.java
```
com/tencent/matrix/plugin/IPlugin.java
public interface IPlugin {
    Application getApplication();
    void init(Application application, PluginListener pluginListener);
    void start();
    void stop();
    void destroy();
    String getTag();
    void onForeground(boolean isForeground);
}

public abstract class Plugin implements IPlugin, IssuePublisher.OnIssueDetectListener, IAppForeground {
    //回调plugin的生命周期
    private PluginListener pluginListener;
    //plugin处于哪个阶段
    private int status = PLUGIN_CREATE;

    @Override
    public void init(Application app, PluginListener listener) {
        ...
        status = PLUGIN_INITED;
        this.application = app;
        this.pluginListener = listener;
        listener.onInit(this);
        ProcessUILifecycleOwner.INSTANCE.addListener(this);//监听app切换到前台
    }

    //plugin接收到issue的处理
    @Override
    public void onDetectIssue(Issue issue) {
        if (issue.getTag() == null) {
            // set default tag
            issue.setTag(getTag());
        }
        issue.setPlugin(this);
        JSONObject content = issue.getContent();
        // add tag and type for default
        try {
            if (issue.getTag() != null) {
                content.put(Issue.ISSUE_REPORT_TAG, issue.getTag());
            }
            if (issue.getType() != 0) {
                content.put(Issue.ISSUE_REPORT_TYPE, issue.getType());
            }
            content.put(Issue.ISSUE_REPORT_PROCESS, MatrixUtil.getProcessName(application));
            content.put(Issue.ISSUE_REPORT_TIME, System.currentTimeMillis());

        } ...
        pluginListener.onReportIssue(issue);
    }
   ...
    @Override
    public void start() {
       ....
        pluginListener.onStart(this);
    }

    @Override
    public void stop() {
       ....
        pluginListener.onStop(this);
    }

    @Override
    public void destroy() {
       ....
        pluginListener.onDestroy(this);
    }

    @Override
    public String getTag() {
        return getClass().getName();
    }

    @Override
    public void onForeground(boolean isForeground) {

    }

}
```

TracePlugin负责初始化各种tracer，回调对应的生命周期，以及UIThreadMonitor启动，AppMethodBeat启动
com/tencent/matrix/trace/TracePlugin.java
```
public class TracePlugin extends Plugin {
    private EvilMethodTracer evilMethodTracer;
    private StartupTracer startupTracer;
    private FrameTracer frameTracer;
    private LooperAnrTracer looperAnrTracer;
    private SignalAnrTracer signalAnrTracer;
    private IdleHandlerLagTracer idleHandlerLagTracer;
    private TouchEventLagTracer touchEventLagTracer;
    private ThreadPriorityTracer threadPriorityTracer;
    
  @Override
    public void init(Application app, PluginListener listener) {
        super.init(app, listener);
        ...
        looperAnrTracer = new LooperAnrTracer(traceConfig);
        frameTracer = new FrameTracer(traceConfig, supportFrameMetrics);
        evilMethodTracer = new EvilMethodTracer(traceConfig);
        startupTracer = new StartupTracer(traceConfig);
    }
}

 @Override
    public void start() {
        super.start();
        ...
         UIThreadMonitor.getMonitor().init(traceConfig, supportFrameMetrics);
         ...
         AppMethodBeat.getInstance().onStart();
         ...
         UIThreadMonitor.getMonitor().onStart();
         ...
    }
```
tracer的构成
com/tencent/matrix/trace/tracer/ITracer.java
```
public interface ITracer extends IAppForeground {
    boolean isAlive();
    void onStartTrace();
    void onCloseTrace();
}

LooperObserver监听Looper，拿到dispatchMessage的开始和结束
com/tencent/matrix/trace/listeners/LooperObserver.java 
public abstract class LooperObserver {
    private boolean isDispatchBegin = false;
    @CallSuper
    public void dispatchBegin(long beginNs, long cpuBeginNs, long token) {
        isDispatchBegin = true;
    }
    
    public void doFrame(String focusedActivity, long startNs, long endNs, boolean isVsyncFrame, long intendedFrameTimeNs, long inputCostNs, long animationCostNs, long traversalCostNs) {
    }

    @CallSuper
    public void dispatchEnd(long beginNs, long cpuBeginMs, long endNs, long cpuEndMs, long token, boolean isVsyncFrame) {
        isDispatchBegin = false;
    }

    public boolean isDispatchBegin() {
        return isDispatchBegin;
    }
}

public abstract class Tracer extends LooperObserver implements ITracer {
    private volatile boolean isAlive = false;
    @CallSuper
    protected void onAlive() {
        MatrixLog.i(TAG, "[onAlive] %s", this.getClass().getName());

    }

    @CallSuper
    protected void onDead() {
        MatrixLog.i(TAG, "[onDead] %s", this.getClass().getName());
    }

    @Override
    final synchronized public void onStartTrace() {
        if (!isAlive) {
            this.isAlive = true;
            onAlive();
        }
    }

    @Override
    final synchronized public void onCloseTrace() {
        if (isAlive) {
            this.isAlive = false;
            onDead();
        }
    }
   ...
}
```


UIThreadMonitor负责监听dispatchMessage的开始和结束，frame帧率的回调
UIThreadMonitor、AppMethodBeat。
基于这两大基石，我们还可以写出更多好玩的东西：比如 后台渲染的检测，UIThreadMonitor判断后台渲染是否产生，由AppMethodBeat可以获取引发后台渲染的堆栈信息。
//todo

一句话简述UIThreadMonitor
通过设置Looper中的printer，来判断Message的执行起止时间。然后hook Choreographer中的input animation traversal回调数组，
向其中添加Runnable来获取每个操作的耗时。最后将这些数据抛出给各个Tracer作为判断的依据。
1 获取线程中每个Message的执行起止时间
2 
com/tencent/matrix/trace/core/UIThreadMonitor.java
```
public class UIThreadMonitor implements BeatLifecycle, Runnable {
   public void init(TraceConfig config, boolean supportFrameMetrics) {
        ...
        //监听looper
        LooperMonitor.register(new LooperMonitor.LooperDispatchListener(historyMsgRecorder, denseMsgTracer) {
            @Override
            public boolean isValid() {
                return isAlive;
            }

            @Override
            public void dispatchStart() {
                super.dispatchStart();
                UIThreadMonitor.this.dispatchBegin();
            }

            @Override
            public void dispatchEnd() {
                super.dispatchEnd();
                UIThreadMonitor.this.dispatchEnd();
            }

        });
        this.isInit = true;
        //可以查看http://www.aospxref.com/android-13.0.0_r3/xref/frameworks/base/core/java/android/view/Choreographer.java
        // mFrameIntervalNanos为(long)(1000000000 / getRefreshRate() 也就是每一帧的时间
        frameIntervalNanos = ReflectUtils.reflectObject(choreographer, "mFrameIntervalNanos", Constants.DEFAULT_FRAME_DURATION);
        if (!useFrameMetrics) {
            choreographer = Choreographer.getInstance();
            //拿到对象锁mLock
            callbackQueueLock = ReflectUtils.reflectObject(choreographer, "mLock", new Object());
            //拿到CallbackQueue[] mCallbackQueues数组
            callbackQueues = ReflectUtils.reflectObject(choreographer, "mCallbackQueues", null);
            if (null != callbackQueues) {
                //分别拿到INPUT，ANIMATION，TRAVERSAL类型CallbackQueue的addCallbackLocked方法
                addInputQueue = ReflectUtils.reflectMethod(callbackQueues[CALLBACK_INPUT], ADD_CALLBACK, long.class, Object.class, Object.class);
                addAnimationQueue = ReflectUtils.reflectMethod(callbackQueues[CALLBACK_ANIMATION], ADD_CALLBACK, long.class, Object.class, Object.class);
                addTraversalQueue = ReflectUtils.reflectMethod(callbackQueues[CALLBACK_TRAVERSAL], ADD_CALLBACK, long.class, Object.class, Object.class);
            }
            //拿到FrameDisplayEventReceiver mDisplayEventReceiver;
            vsyncReceiver = ReflectUtils.reflectObject(choreographer, "mDisplayEventReceiver", null);
            ...
        }
    }
    
   @Override
    public synchronized void onStart() {
        ....
            if (!useFrameMetrics) {
                queueStatus = new int[CALLBACK_LAST + 1];
                queueCost = new long[CALLBACK_LAST + 1];
                //增加input类型的回调，this指向UIThreadMonitor的run方法
                addFrameCallback(CALLBACK_INPUT, this, true);
            }
        }
    } 
    
   
    private synchronized void addFrameCallback(int type, Runnable callback, boolean isAddHeader) {
       ...
        try {
            synchronized (callbackQueueLock) {
                Method method = null;
                switch (type) {
                    case CALLBACK_INPUT:
                        method = addInputQueue;
                        break;
                    case CALLBACK_ANIMATION:
                        method = addAnimationQueue;
                        break;
                    case CALLBACK_TRAVERSAL:
                        method = addTraversalQueue;
                        break;
                }
                if (null != method) {
                    //方法签名为addCallbackLocked(long dueTime, Object action, Object token)  action也就是调用的callback
                    method.invoke(callbackQueues[type], !isAddHeader ? SystemClock.uptimeMillis() : -1, callback, null);
                    callbackExist[type] = true;
                }
            }
        } ...
    } 
    
  
     public void run() {
        final long start = System.nanoTime();
        try {
            doFrameBegin(token);
            doQueueBegin(CALLBACK_INPUT);
            
            //添加ANIMATION回调
            addFrameCallback(CALLBACK_ANIMATION, new Runnable() {
                @Override
                public void run() {
                    doQueueEnd(CALLBACK_INPUT);
                    doQueueBegin(CALLBACK_ANIMATION);
                }
            }, true);
            //添加TRAVERSAL回调
            addFrameCallback(CALLBACK_TRAVERSAL, new Runnable() {
                @Override
                public void run() {
                    doQueueEnd(CALLBACK_ANIMATION);
                    doQueueBegin(CALLBACK_TRAVERSAL);
                }
            }, true);

        } 
    } 
  
    //标志位为true，这标志着当前Frame已经被纳入了统计  dispatchEnd()置为false
     private void doFrameBegin(long token) {
        this.isVsyncFrame = true;
    }
    
    //更新对应type的queueStatus标志位为DO_QUEUE_BEGIN，并用queueCost[type]记下此时的时间
     private void doQueueBegin(int type) {
        queueStatus[type] = DO_QUEUE_BEGIN;
        queueCost[type] = System.nanoTime();
    }

    //记录CallbackQueue中添加的Runnable的运行状态，分为默认状态（DO_QUEUE_DEFAULT）、已经添加的状态（DO_QUEUE_BEGIN）、运行结束的状态（DO_QUEUE_END）
    private int[] queueStatus = new int[CALLBACK_LAST + 1];
    //记录是否已经向该类型的CallbackQueue添加了Runnable，避免重复添加
    private boolean[] callbackExist = new boolean[CALLBACK_LAST + 1]; // ABA
    //录上面某种类型的Runnable的执行起始的耗时，这可以反映出当前这一次执行CallbackQueue里面的任务耗时有多久
    private long[] queueCost = new long[CALLBACK_LAST + 1];
    
    //更新对应type的queueStatus标志位为DO_QUEUE_END，并用当前时间减去queueCost[type]的时间，这个时间为当前frame执行时此type的CallbackQueue执行的总耗时，
    //记为queueCost[type]
    private void doQueueEnd(int type) {
        queueStatus[type] = DO_QUEUE_END;
        queueCost[type] = System.nanoTime() - queueCost[type];
        synchronized (this) {
            callbackExist[type] = false;
        }
    }    
}
```

http://www.aospxref.com/android-13.0.0_r3/xref/frameworks/base/core/java/android/view/Choreographer.java
```
    public void addCallbackLocked(long dueTime, Object action, Object token) {
              CallbackRecord callback = obtainCallbackLocked(dueTime, action, token);
              CallbackRecord entry = mHead;
              if (entry == null) {
                  mHead = callback;
                  return;
              }
              if (dueTime < entry.dueTime) {
                  callback.next = entry;
                  mHead = callback;
                  return;
              }
              while (entry.next != null) {
                  if (dueTime < entry.next.dueTime) {
                      callback.next = entry.next;
                      break;
                  }
                  entry = entry.next;
              }
              entry.next = callback;
          }
```
在UIThreadMonitor#onStart方法中，最后调用addFrameCallback方法将一个Runnable（自己）插到了INPUT类型的CallbackQueue的头部。
CallbackQueue是一个单链表组织起来的队列，里面按照时间从小到大进行组织

run方法
为什么这里没有调用doQueueEnd(CALLBACK_TRAVERSAL)呢。我们研究Choreographer发现，在CALLBACK_TRAVERSAL之后还有一个CALLBACK_COMMIT，
我们向CALLBACK_COMMIT这个队列添加一个callback就可以在合理的位置调用doQueueEnd(CALLBACK_TRAVERSAL)了。 但是很不幸，
CALLBACK_COMMIT在Android 6.0及以后才会有。
为了兼容更早的版本，我们得想出其他办法：还记得上面提到的LooperMonitor吗，我们提到过LooperMonitor可以捕获到Message的执行的起始。
Choreographer中的Vsync信号触发各种callback也是通过Android的消息机制来实现的，且该Message在执行完各种CallbackQueue就结束了，
某种程度上来说，以Message的执行结束时间作CALLBACK_TRAVERSAL的结束时间也是可以的。

dispatchEnd()
```
private void dispatchEnd() {
        ...
        if (config.isFPSEnable() && !useFrameMetrics) {
            long startNs = token;
            long intendedFrameTimeNs = startNs; //开始时间
            if (isVsyncFrame) {
                doFrameEnd(token);
                //获取FrameDisplayEventReceiver的mTimestampNanos vsync的当前时间
                intendedFrameTimeNs = getIntendedFrameTimeNs(startNs);
                ...
                //每帧回调
                observer.doFrame(AppActiveMatrixDelegate.INSTANCE.getVisibleScene(), startNs, endNs, isVsyncFrame, intendedFrameTimeNs, queueCost[CALLBACK_INPUT], queueCost[CALLBACK_ANIMATION], queueCost[CALLBACK_TRAVERSAL]);
            }
          ....
        }
       ...
        this.isVsyncFrame = false;
       ...
    }
```
在方法的最开始，如果isBelongFrame标志位为true，表明是UIThreadMonitor#run方法已经执行过了，因此需要闭合整个frame的监控，
闭合的这部分代码在doFrameEnd中
```
private void doFrameEnd(long token) {
        doQueueEnd(CALLBACK_TRAVERSAL);
        for (int i : queueStatus) {
            if (i != DO_QUEUE_END) {
                queueCost[i] = DO_QUEUE_END_ERROR;
                ...
            }
        }
        queueStatus = new int[CALLBACK_LAST + 1];
        addFrameCallback(CALLBACK_INPUT, this, true);
    }
```
这里调用了doQueueEnd(CALLBACK_TRAVERSAL)真正闭合了frame监控，然后恢复了queueStatus数组的状态，
最后又调用了addFrameCallback(CALLBACK_INPUT, this, true)方法开始监控下一帧



LooperMonitor
LooperMonitor的主要作用就是在Message执行前后回调出对应的方法。其实现原理是通过设置Looper#mLogging这个字段，让Message在执行前后打印出日志，
然后根据日志的特点来判断是开始执行还是执行结束  
分发前日志第一个字符是> 分发后日志第一个字符是<
android/os/Looper.java
```
public static void loop() {
    ...
    for (;;) {
       ...
        if (logging != null) {
            logging.println(">>>>> Dispatching to " + msg.target + " " +
                    msg.callback + ": " + msg.what);
        }
        ...
        try {
            msg.target.dispatchMessage(msg);
            ...
        } 
        ...
        if (logging != null) {
            logging.println("<<<<< Finished to " + msg.target + " " + msg.callback);
        }
        ...
    }
}
```
matrix-trace-canary/src/main/java/com/tencent/matrix/trace/core/LooperMonitor.java
```
public class LooperMonitor implements MessageQueue.IdleHandler {
   private LooperMonitor(Looper looper) {
       ....
        this.looper = looper;
        resetPrinter();
        addIdleHandler(looper);
    }
    
     private synchronized void resetPrinter() {
        //拿到原有的printer
        Printer originPrinter = null;
        try {
            if (!isReflectLoggingError) {
                originPrinter = ReflectUtils.get(looper.getClass(), "mLogging", looper);
               ...
        } ...
        //替换为自己的printer
        looper.setMessageLogging(printer = new LooperPrinter(originPrinter));
      ....
    } 
  
   private synchronized void addIdleHandler(Looper looper) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            looper.getQueue().addIdleHandler(this); //添加idleHandler，空闲执行
        } ...
    }  
   
    @Override
    public boolean queueIdle() {
        if (SystemClock.uptimeMillis() - lastCheckPrinterTime >= CHECK_TIME) {
            resetPrinter(); //每60s设置一次printer
            lastCheckPrinterTime = SystemClock.uptimeMillis();
        }
        return true;
    } 
}
```
向MessageQueue中添加了一个IdleHandler，在对应的queueIdle方法中会周期性（60s）的调用resetPrinter方法来保证Looper中的printer对象是我们自定义的LooperPrinter。

LooperPrinter
```
 class LooperPrinter implements Printer {
        public Printer origin;
        boolean isHasChecked = false;
        boolean isValid = false;

        LooperPrinter(Printer printer) {
            this.origin = printer;
        }

        @Override
        public void println(String x) {
            if (null != origin) {
                origin.println(x); //原始printer打印日志
                ...
            }

            if (!isHasChecked) {
                isValid = x.charAt(0) == '>' || x.charAt(0) == '<';//是否是标准日志，默认looper只有开始和结束两种日志
                isHasChecked = true; //只校验一次
                ...
            }

            if (isValid) {
                //回调onDispatchStar和onDispatchEnd  第一个字符为>是开始
                dispatch(x.charAt(0) == '>', x);
            }
        }
    }
    
 
  private void dispatch(boolean isBegin, String log) {
        synchronized (listeners) {
            for (LooperDispatchListener listener : listeners) {
                if (listener.isValid()) {
                    if (isBegin) {
                        if (!listener.isHasDispatchStart) {
                            if (listener.historyMsgRecorder) {
                                messageStartTime = System.currentTimeMillis();
                                latestMsgLog = log;
                                recentMCount++;
                            }
                            listener.onDispatchStart(log);
                        }
                    } else {
                        if (listener.isHasDispatchStart) {
                            if (listener.historyMsgRecorder) {
                                recordMsg(log, System.currentTimeMillis() - messageStartTime, listener.denseMsgTracer);
                            }
                            listener.onDispatchEnd(log);
                        }
                    }
                } else if (!isBegin && listener.isHasDispatchStart) {
                    listener.dispatchEnd();
                }
            }
        }
    }   
```


FrameTracer
matrix-trace-canary\src\main\java\com\tencent\matrix\trace\tracer\FrameTracer.java
```
public class FrameTracer extends Tracer implements Application.ActivityLifecycleCallbacks {
 @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onActivityResumed(Activity activity) {
        lastResumeTimeMap.put(activity.getClass().getName(), System.currentTimeMillis());
        //android 26以上使用window,帧率监听 不使用looper消息监听
        if (useFrameMetrics) {
            if (frameListenerMap.containsKey(activity.hashCode())) {
                return;
            }
            this.refreshRate = (int) activity.getWindowManager().getDefaultDisplay().getRefreshRate();
            this.frameIntervalNs = Constants.TIME_SECOND_TO_NANO / (long) refreshRate;
            Window.OnFrameMetricsAvailableListener onFrameMetricsAvailableListener = new Window.OnFrameMetricsAvailableListener() {
                @RequiresApi(api = Build.VERSION_CODES.O)
                @Override
                public void onFrameMetricsAvailable(Window window, FrameMetrics frameMetrics, int dropCountSinceLastInvocation) {
                    FrameMetrics frameMetricsCopy = new FrameMetrics(frameMetrics);
                    long vsynTime = frameMetricsCopy.getMetric(FrameMetrics.VSYNC_TIMESTAMP);
                    long intendedVsyncTime = frameMetricsCopy.getMetric(FrameMetrics.INTENDED_VSYNC_TIMESTAMP);
                    frameMetricsCopy.getMetric(FrameMetrics.DRAW_DURATION);
                    notifyListener(ProcessUILifecycleOwner.INSTANCE.getVisibleScene(), intendedVsyncTime, vsynTime, true, intendedVsyncTime, 0, 0, 0);
                }
            };
            this.frameListenerMap.put(activity.hashCode(), onFrameMetricsAvailableListener);
            activity.getWindow().addOnFrameMetricsAvailableListener(onFrameMetricsAvailableListener, new Handler());
            MatrixLog.i(TAG, "onActivityResumed addOnFrameMetricsAvailableListener");
        }
    }



 //focusedActivity 可见的activity
 //start  Message执行前的时间   end Message执行完毕，调用LooperObserver#doFrame时的时间
  //intendedFrameTimeNs  如果调用执行dispatchEnd时，UIThreadMonitor#run执行过了，那么该值为上面的end-start的值；否则为0
  //inputCostNs、animationCostNs、traversalCostNs执行三种CallbackQueue的耗时
    @Override
    public void doFrame(String focusedActivity, long startNs, long endNs, boolean isVsyncFrame, long intendedFrameTimeNs, long inputCostNs, long animationCostNs, long traversalCostNs) {
        if (isForeground()) {
            notifyListener(focusedActivity, startNs, endNs, isVsyncFrame, intendedFrameTimeNs, inputCostNs, animationCostNs, traversalCostNs);
        }
    }
   

    private void notifyListener(final String focusedActivity, final long startNs, final long endNs, final boolean isVsyncFrame,
                                final long intendedFrameTimeNs, final long inputCostNs, final long animationCostNs, final long traversalCostNs) {
        long traceBegin = System.currentTimeMillis();
        try {
            final long jitter = endNs - intendedFrameTimeNs; //抖动=结束时间-vsync开始时间
            final int dropFrame = (int) (jitter / frameIntervalNs);
            if (dropFrameListener != null) {
                if (dropFrame > dropFrameListenerThreshold) {
                    try {
                        if (MatrixUtil.getTopActivityName() != null) {
                            //activity resume时间
                            long lastResumeTime = lastResumeTimeMap.get(MatrixUtil.getTopActivityName());
                            //回调掉帧
                            dropFrameListener.dropFrame(dropFrame, jitter, MatrixUtil.getTopActivityName(), lastResumeTime);
                        }
                    }...
                }
            }

            droppedSum += dropFrame;
            durationSum += Math.max(jitter, frameIntervalNs);
             ...
             //帧率的收集，回调
             listener.collect(focusedActivity, startNs, endNs, dropFrame, isVsyncFrame,
                                    intendedFrameTimeNs, inputCostNs, animationCostNs, traversalCostNs);
             ...
              listener.doFrameSync(focusedActivity, startNs, endNs, dropFrame, isVsyncFrame,
                                intendedFrameTimeNs, inputCostNs, animationCostNs, traversalCostNs);                       
             ...
            }
        } ...
    } 
}
```
FPSCollector 帧率的收集以及上报
```
java/com/tencent/matrix/trace/listeners/IDoFrameListener.java
 @CallSuper
    public void collect(String focusedActivity, long startNs, long endNs, int dropFrame, boolean isVsyncFrame,
                        long intendedFrameTimeNs, long inputCostNs, long animationCostNs, long traversalCostNs) {
        FrameReplay replay = FrameReplay.create();
        replay.focusedActivity = focusedActivity;
        ....
        list.add(replay);
        if (list.size() >= intervalFrame && getExecutor() != null) {
            final List<FrameReplay> copy = new LinkedList<>(list);
            list.clear();
            getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    doReplay(copy);
                    for (FrameReplay record : copy) {
                        record.recycle();
                    }
                }
            });
        }
    }
 
 java/com/tencent/matrix/trace/tracer/FrameTracer.java   
 private class FPSCollector extends IDoFrameListener {
       @Override
        public void doReplay(List<FrameReplay> list) {
            super.doReplay(list);
            for (FrameReplay replay : list) {
                doReplayInner(replay.focusedActivity, replay.startNs, replay.endNs, replay.dropFrame, replay.isVsyncFrame,
                        replay.intendedFrameTimeNs, replay.inputCostNs, replay.animationCostNs, replay.traversalCostNs);
            }
        }

        public void doReplayInner(String visibleScene, long startNs, long endNs, int droppedFrames,
                                  boolean isVsyncFrame, long intendedFrameTimeNs, long inputCostNs,
                                  long animationCostNs, long traversalCostNs) {
            ...
            if (!isVsyncFrame) return; //不是在vsync内跳过
            FrameCollectItem item = map.get(visibleScene);
            if (null == item) {
                item = new FrameCollectItem(visibleScene);
                //每个activity存储一个item
                map.put(visibleScene, item);
            }
            //收集帧率信息
            item.collect(droppedFrames);
            if (item.sumFrameCost >= timeSliceMs) { // report 默认超过10s
                map.remove(visibleScene);
                item.report(); //上报
            }
        }
    }
    
    private class FrameCollectItem {
        void collect(int droppedFrames) {
            float frameIntervalCost = 1f * FrameTracer.this.frameIntervalNs
                    / Constants.TIME_MILLIS_TO_NANO;
            sumFrameCost += (droppedFrames + 1) * frameIntervalCost; //累计掉帧耗时
            sumDroppedFrames += droppedFrames; //累计掉帧
            sumFrame++; //累计帧率
            if (droppedFrames >= frozenThreshold) { //42
                dropLevel[DropStatus.DROPPED_FROZEN.index]++;
                dropSum[DropStatus.DROPPED_FROZEN.index] += droppedFrames;
            } else if (droppedFrames >= highThreshold) { //24
                dropLevel[DropStatus.DROPPED_HIGH.index]++;
                dropSum[DropStatus.DROPPED_HIGH.index] += droppedFrames;
            } else if (droppedFrames >= middleThreshold) { //9
                dropLevel[DropStatus.DROPPED_MIDDLE.index]++;
                dropSum[DropStatus.DROPPED_MIDDLE.index] += droppedFrames;
            } else if (droppedFrames >= normalThreshold) {//3
                dropLevel[DropStatus.DROPPED_NORMAL.index]++;
                dropSum[DropStatus.DROPPED_NORMAL.index] += droppedFrames;
            } else { //1-2帧
                dropLevel[DropStatus.DROPPED_BEST.index]++;
                dropSum[DropStatus.DROPPED_BEST.index] += Math.max(droppedFrames, 0);
            }
        }
    }
    
    void report() {
            //当前的fps 
            float fps = Math.min(refreshRate, 1000.f * sumFrame / sumFrameCost);
            MatrixLog.i(TAG, "[report] FPS:%s %s", fps, toString());
            try {
                TracePlugin plugin = Matrix.with().getPluginByClass(TracePlugin.class);
                ...                
                Issue issue = new Issue();
                issue.setTag(SharePluginInfo.TAG_PLUGIN_FPS);
                issue.setContent(resultObject);
                //回调给plugin
                plugin.onDetectIssue(issue);

            },,,
        }
 }
```
开箱即用
matrix-trace-canary中有一个FrameDecorator的类，可以悬浮窗展示实时帧率，开箱即用，无需自己写逻辑。其底层实现与FrameTracer类似。



慢方法监控EvilMethodTracer
java/com/tencent/matrix/trace/tracer/EvilMethodTracer.java
```
public class EvilMethodTracer extends Tracer {
 @Override
    public void dispatchBegin(long beginNs, long cpuBeginMs, long token) {
        super.dispatchBegin(beginNs, cpuBeginMs, token);
        indexRecord = AppMethodBeat.getInstance().maskIndex("EvilMethodTracer#dispatchBegin");
    }


    @Override
    public void doFrame(String focusedActivity, long startNs, long endNs, boolean isVsyncFrame, long intendedFrameTimeNs, long inputCostNs, long animationCostNs, long traversalCostNs) {
        queueTypeCosts[0] = inputCostNs;
        queueTypeCosts[1] = animationCostNs;
        queueTypeCosts[2] = traversalCostNs;
    }

    @Override
    public void dispatchEnd(long beginNs, long cpuBeginMs, long endNs, long cpuEndMs, long token, boolean isVsyncFrame) {
        super.dispatchEnd(beginNs, cpuBeginMs, endNs, cpuEndMs, token, isVsyncFrame);
        long start = config.isDevEnv() ? System.currentTimeMillis() : 0;
        long dispatchCost = (endNs - beginNs) / Constants.TIME_MILLIS_TO_NANO;
        try {
            if (dispatchCost >= evilThresholdMs) { //默认700ms
               // 则解析出这段时间内函数的调用堆栈
                long[] data = AppMethodBeat.getInstance().copyData(indexRecord);
                long[] queueCosts = new long[3];
                System.arraycopy(queueTypeCosts, 0, queueCosts, 0, 3);
                String scene = AppActiveMatrixDelegate.INSTANCE.getVisibleScene();
                //queueCosts input,animation,traversal的耗时
                //子线程分析
                MatrixHandlerThread.getDefaultHandler().post(new AnalyseTask(isForeground(), scene, data, queueCosts, cpuEndMs - cpuBeginMs, dispatchCost, endNs / Constants.TIME_MILLIS_TO_NANO));
            }
        } 。。。
    }
    
   private class AnalyseTask implements Runnable {
    @Override
        public void run() {
            analyse();
        }
      
       void analyse() {
            //获取进程的priority以及nice，原理是读取/proc/<pid>/stat中的数据
            int[] processStat = Utils.getProcessPriority(Process.myPid());
            String usage = Utils.calculateCpuUsage(cpuCost, cost);
            LinkedList<MethodItem> stack = new LinkedList();
            if (data.length > 0) {
               //获取stack信息
                TraceDataUtils.structuredDataToStack(data, stack, true, endMs);
                TraceDataUtils.trimStack(stack, Constants.TARGET_EVIL_METHOD_STACK, new TraceDataUtils.IStructuredDataFilter() {
                    @Override
                    public boolean isFilter(long during, int filterCount) {
                        return during < filterCount * Constants.TIME_UPDATE_CYCLE_MS;
                    }

                    @Override
                    public int getFilterMaxCount() {
                        return Constants.FILTER_STACK_MAX_COUNT;
                    }

                    @Override
                    public void fallback(List<MethodItem> stack, int size) {
                        MatrixLog.w(TAG, "[fallback] size:%s targetSize:%s stack:%s", size, Constants.TARGET_EVIL_METHOD_STACK, stack);
                        Iterator iterator = stack.listIterator(Math.min(size, Constants.TARGET_EVIL_METHOD_STACK));
                        while (iterator.hasNext()) {
                            iterator.next();
                            iterator.remove();
                        }
                    }
                });
            }
            
            //生成stackKey
            long stackCost = Math.max(cost, TraceDataUtils.stackToString(stack, reportBuilder, logcatBuilder));
            String stackKey = TraceDataUtils.getTreeKey(stack, stackCost);

            //打印queueCost
            MatrixLog.w(TAG, "%s", printEvil(scene, processStat, isForeground, logcatBuilder, stack.size(), stackKey, usage, queueCost[0], queueCost[1], queueCost[2], cost)); // for logcat

            // report 上报问题
            try {
                TracePlugin plugin = Matrix.with().getPluginByClass(TracePlugin.class);
                ...
                JSONObject jsonObject = new JSONObject();
                jsonObject = DeviceUtil.getDeviceInfo(jsonObject, Matrix.with().getApplication());
                jsonObject.put(SharePluginInfo.ISSUE_STACK_TYPE, Constants.Type.NORMAL);
                ...
                Issue issue = new Issue();
                issue.setTag(SharePluginInfo.TAG_PLUGIN_EVIL_METHOD);
                issue.setContent(jsonObject);
                plugin.onDetectIssue(issue);
            } ...
        }  
   }   
}
```
在dispatchBegin方法中，记录下AppMethodBeat中目前的index，记为start；
在dispatchEnd中，读取目前AppMethodBeat中目前的index，记为end。这两者中间的数据则为这段时间内执行的方法的入栈、出栈信息。
当这个Message执行时间超过指定的阈值（默认700ms）时，认为可能发生了慢方法，此时会进行进一步的分析。
至于doFrame中记录的数据，没有啥具体的用处，这是在最后打印了log而已

todo 堆栈相关 stackkey



ANR监控AnrTracer
ANR如何进行判定。我们在Message执行开始时抛出一个定时任务，若该任务执行到了，则可以认为发生了ANR。若该任务在Message执行完毕之后被主动清除了，
  则说明没有ANR发生。这种思想与系统ANR的判定有相似之处。
在ANR监控中，若发生了ANR，则需要解析这段时间内的调用堆栈
matrix-trace-canary/src/main/java/com/tencent/matrix/trace/tracer/LooperAnrTracer.java
```
public class LooperAnrTracer extends Tracer {
  public void dispatchBegin(long beginNs, long cpuBeginMs, long token) {
        super.dispatchBegin(beginNs, cpuBeginMs, token);
        anrTask.beginRecord = AppMethodBeat.getInstance().maskIndex("AnrTracer#dispatchBegin");
        anrTask.token = token;
        ...
        long cost = (System.nanoTime() - token) / Constants.TIME_MILLIS_TO_NANO;
        //添加定时任务 5秒减去方法执行的时间  anrTask和lagTask为执行的runnable
        anrHandler.postDelayed(anrTask, Constants.DEFAULT_ANR - cost);  //5000-cost
        lagHandler.postDelayed(lagTask, Constants.DEFAULT_NORMAL_LAG - cost);//2000-cost
    }
    
  @Override
    public void dispatchEnd(long beginNs, long cpuBeginMs, long endNs, long cpuEndMs, long token, boolean isBelongFrame) {
        super.dispatchEnd(beginNs, cpuBeginMs, endNs, cpuEndMs, token, isBelongFrame);
      ....
        //移除定时任务
        anrHandler.removeCallbacks(anrTask);
        lagHandler.removeCallbacks(lagTask);
    }   
    
     class AnrHandleTask implements Runnable {
          @Override
        public void run() {
            long curTime = SystemClock.uptimeMillis();
            boolean isForeground = isForeground();
            // process 进程状态
            int[] processStat = Utils.getProcessPriority(Process.myPid());
            long[] data = AppMethodBeat.getInstance().copyData(beginRecord);
            beginRecord.release();
            String scene = AppActiveMatrixDelegate.INSTANCE.getVisibleScene();

            // memory 内存信息
            long[] memoryInfo = dumpMemory();

            // Thread state  线程状态
            Thread.State status = Looper.getMainLooper().getThread().getState();
            StackTraceElement[] stackTrace = Looper.getMainLooper().getThread().getStackTrace();
            String dumpStack;
            if (traceConfig.getLooperPrinterStackStyle() == TraceConfig.STACK_STYLE_WHOLE) {
                dumpStack = Utils.getWholeStack(stackTrace, "|*\t\t");
            } else {
                dumpStack = Utils.getStack(stackTrace, "|*\t\t", 12);
            }


            // frame  input,animation,traversal耗时
            UIThreadMonitor monitor = UIThreadMonitor.getMonitor();
            long inputCost = monitor.getQueueCost(UIThreadMonitor.CALLBACK_INPUT, token);
            long animationCost = monitor.getQueueCost(UIThreadMonitor.CALLBACK_ANIMATION, token);
            long traversalCost = monitor.getQueueCost(UIThreadMonitor.CALLBACK_TRAVERSAL, token);

            // trace 堆栈
            LinkedList<MethodItem> stack = new LinkedList();
            if (data.length > 0) {
                TraceDataUtils.structuredDataToStack(data, stack, true, curTime);
                TraceDataUtils.trimStack(stack, Constants.TARGET_EVIL_METHOD_STACK, new TraceDataUtils.IStructuredDataFilter() {
                    @Override
                    public boolean isFilter(long during, int filterCount) {
                        return during < filterCount * Constants.TIME_UPDATE_CYCLE_MS;
                    }

                    @Override
                    public int getFilterMaxCount() {
                        return Constants.FILTER_STACK_MAX_COUNT;
                    }

                    @Override
                    public void fallback(List<MethodItem> stack, int size) {
                        MatrixLog.w(TAG, "[fallback] size:%s targetSize:%s stack:%s", size, Constants.TARGET_EVIL_METHOD_STACK, stack);
                        Iterator iterator = stack.listIterator(Math.min(size, Constants.TARGET_EVIL_METHOD_STACK));
                        while (iterator.hasNext()) {
                            iterator.next();
                            iterator.remove();
                        }
                    }
                });
            }
            ...
            // stackKey
            String stackKey = TraceDataUtils.getTreeKey(stack, stackCost);
            ....
            // report
            try {
                TracePlugin plugin = Matrix.with().getPluginByClass(TracePlugin.class);
                 ...
                plugin.onDetectIssue(issue);
            } ...
        }
     }
}
```
IdleHandlerLagTracer idlehandler超时检测
替换looper的mIdleHandlers列表，替换为自己的idlehandler，在queueIdle()前添加定时器，执行后移除定时器，定时器触(2s)发后抛出问题
com/tencent/matrix/trace/tracer/IdleHandlerLagTracer.java
```
public class IdleHandlerLagTracer extends Tracer {
 @Override
    public void onAlive() {
        super.onAlive();
        if (traceConfig.isIdleHandlerTraceEnable()) {
            idleHandlerLagHandlerThread = new HandlerThread("IdleHandlerLagThread");
            idleHandlerLagRunnable = new IdleHandlerLagRunable();
            detectIdleHandler();
        }
    }
    
      private static void detectIdleHandler() {
        try {
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
                return;
            }
            MessageQueue mainQueue = Looper.getMainLooper().getQueue();
            Field field = MessageQueue.class.getDeclaredField("mIdleHandlers");
            field.setAccessible(true);
            //替换idleHandler的list
            MyArrayList<MessageQueue.IdleHandler> myIdleHandlerArrayList = new MyArrayList<>();
            field.set(mainQueue, myIdleHandlerArrayList);
            idleHandlerLagHandlerThread.start();
            idleHandlerLagHandler = new Handler(idleHandlerLagHandlerThread.getLooper());
        } ...
    }
    
     static class MyArrayList<T> extends ArrayList {
        Map<MessageQueue.IdleHandler, MyIdleHandler> map = new HashMap<>();
        @Override
        public boolean add(Object o) {
            if (o instanceof MessageQueue.IdleHandler) {
               //替换为自己的MyIdleHandler
                MyIdleHandler myIdleHandler = new MyIdleHandler((MessageQueue.IdleHandler) o);
                map.put((MessageQueue.IdleHandler) o, myIdleHandler);
                return super.add(myIdleHandler);
            }
            return super.add(o);
        }
    }   
    
    static class MyIdleHandler implements MessageQueue.IdleHandler {
        private final MessageQueue.IdleHandler idleHandler;
        MyIdleHandler(MessageQueue.IdleHandler idleHandler) {
            this.idleHandler = idleHandler;
        }
        @Override
        public boolean queueIdle() {
             //装饰器，在原有idleHandler增加功能
            //添加定时器，定时器触发执行IdleHandlerLagRunable 
            idleHandlerLagHandler.postDelayed(idleHandlerLagRunnable, traceConfig.idleHandlerLagThreshold);
            boolean ret = this.idleHandler.queueIdle();
            //移除定时器
            idleHandlerLagHandler.removeCallbacks(idleHandlerLagRunnable);
            return ret;
        }
    } 
    
    
    static class IdleHandlerLagRunable implements Runnable {
        @Override
        public void run() {
            try {
                //上报异常
                TracePlugin plugin = Matrix.with().getPluginByClass(TracePlugin.class);
                ....
                plugin.onDetectIssue(issue);
            } ...
        }
    }
 
}
```



StartupTracer
java/com/tencent/matrix/trace/tracer/StartupTracer.java
```
  firstMethod.i       LAUNCH_ACTIVITY   onWindowFocusChange   LAUNCH_ACTIVITY    onWindowFocusChange
  ^                         ^                   ^                     ^                  ^
  |                         |                   |                     |                  |
  |---------app---------|---|---firstActivity---|---------...---------|---careActivity---|
  |<--applicationCost-->|
  |<--------------firstScreenCost-------------->|
  |<---------------------------------------coldCost------------------------------------->|
  .                         |<-----warmCost---->|
```
插件在编译器为 Activity#onWindowFocusChanged 织入 AppMethodBeat.at 方法，这样可以获取每个 Activity 的 onWindowFocusChanged 回调时间。
然后在第一个 AppMethodBeat.i 方法调用时，记录此时的时间作为进程 zygote 后的时间；hook ActivityThread 中的 mH 中的 Callback ，
通过检查第一个 Activity 或 Service 或 Receiver 的 what，以此时的时间作为 Application 创建结束时间，该时间与上面的时间之差记为 Application创建耗时。
在第一个 Activity 的 onWindowFocusChange 回调时，此时的时间减去 zygote 时间即为 首屏启动耗时 ；
第二个 Activity 的 onWindowFocusChange 回调时，时间减去 zygote 的时间即为 整个冷启动的时间。

我们顺着上面的这个线理一下代码，首先是AppMethBeat.i方法里面的相关代码
matrix-trace-canary\src\main\java\com\tencent\matrix\trace\core\AppMethodBeat.java
```
 public static void i(int methodId) {
      ...
        if (status == STATUS_DEFAULT) {
            synchronized (statusLock) {
                if (status == STATUS_DEFAULT) {
                    realExecute();
                    status = STATUS_READY;
                }
            }
        }
       ...
    }
    
  private static void realExecute() {
        ...
        sCurrentDiffTime = SystemClock.uptimeMillis() - sDiffTime;

        sHandler.removeCallbacksAndMessages(null);
        sHandler.postDelayed(sUpdateDiffTimeRunnable, Constants.TIME_UPDATE_CYCLE_MS);
        sHandler.postDelayed(checkStartExpiredRunnable = new Runnable() {
            @Override
            public void run() {
                synchronized (statusLock) {
                     ...
                    if (status == STATUS_DEFAULT || status == STATUS_READY) {
                        status = STATUS_EXPIRED_START;
                    }
                }
            }
        }, Constants.DEFAULT_RELEASE_BUFFER_DELAY);

        ActivityThreadHacker.hackSysHandlerCallback();
        LooperMonitor.register(looperMonitorListener);
    }
```
status默认状态是STATUS_DEFAULT，因此第一次执行AppMethodBeat#i方法肯定会执行到realExecute()方法，
  这里面相当于
1 初始化AppMethodBeat
2  hook了mH 的 mCallback，可以拿到Application 初始化结束的时间
3 初始化LooperMonitor
java/com/tencent/matrix/trace/hacker/ActivityThreadHacker.java
```
//进程启动的时间
 private static long sApplicationCreateBeginTime = 0L;
//四大组件首次执行到的时间
 private static long sApplicationCreateEndTime = 0L;
  public static void hackSysHandlerCallback() {
        try {
            //第一次执行方法，记录application创建的时间
            sApplicationCreateBeginTime = SystemClock.uptimeMillis();
            sApplicationCreateBeginMethodIndex = AppMethodBeat.getInstance().maskIndex("ApplicationCreateBeginMethodIndex");
            Class<?> forName = Class.forName("android.app.ActivityThread");
            Field field = forName.getDeclaredField("sCurrentActivityThread");
            field.setAccessible(true);
            Object activityThreadValue = field.get(forName);
            Field mH = forName.getDeclaredField("mH");
            mH.setAccessible(true);
            Object handler = mH.get(activityThreadValue);
            Class<?> handlerClass = handler.getClass().getSuperclass();
            if (null != handlerClass) {
                Field callbackField = handlerClass.getDeclaredField("mCallback");
                callbackField.setAccessible(true);
                Handler.Callback originalCallback = (Handler.Callback) callbackField.get(handler);
                //替换为HackCallback
                HackCallback callback = new HackCallback(originalCallback);
                callbackField.set(handler, callback);
            }
....

 private final static class HackCallback implements Handler.Callback {
  @Override
        public boolean handleMessage(Message msg) {
            if (IssueFixConfig.getsInstance().isEnableFixSpApply()) {
                if (Build.VERSION.SDK_INT >= 21 && Build.VERSION.SDK_INT <= 25) {
                    if (msg.what == SERIVCE_ARGS || msg.what == STOP_SERVICE
                            || msg.what == STOP_ACTIVITY_SHOW || msg.what == STOP_ACTIVITY_HIDE
                            || msg.what == SLEEPING) {
                            MatrixLog.i(TAG, "Fix SP ANR is enabled");
                            fix(); //修复sp的anr
                        }
                }
            }
            ...
            //消息类型是否为activity启动
            boolean isLaunchActivity = isLaunchActivity(msg);

            if (hasPrint > 0) {
                MatrixLog.i(TAG, "[handleMessage] msg.what:%s begin:%s isLaunchActivity:%s SDK_INT=%s", msg.what, SystemClock.uptimeMillis(), isLaunchActivity, Build.VERSION.SDK_INT);
                hasPrint--;
            }

            if (!isCreated) {
                if (isLaunchActivity || msg.what == CREATE_SERVICE
                        || msg.what == RECEIVER) { // 待办： for provider  这里还需要增加provider相关
                    //记录application创建结束的时间    
                    ActivityThreadHacker.sApplicationCreateEndTime = SystemClock.uptimeMillis();
                    ActivityThreadHacker.sApplicationCreateScene = msg.what;
                    isCreated = true;
                    sIsCreatedByLaunchActivity = isLaunchActivity;
                    ...
                    synchronized (listeners) {
                        for (IApplicationCreateListener listener : listeners) {
                            //回调
                            listener.onApplicationCreateEnd();
                        }
                    }
                }
            }
            //原有callBack的处理
            return null != mOriginalCallback && mOriginalCallback.handleMessage(msg);
        }
        
        //todo 结合源码查看
        private boolean isLaunchActivity(Message msg) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1) {
                if (msg.what == EXECUTE_TRANSACTION && msg.obj != null) {
                    try {
                        if (null == method) {
                            Class clazz = Class.forName("android.app.servertransaction.ClientTransaction");
                            method = clazz.getDeclaredMethod("getCallbacks");
                            method.setAccessible(true);
                        }
                        List list = (List) method.invoke(msg.obj);
                        if (!list.isEmpty()) {
                            return list.get(0).getClass().getName().endsWith(".LaunchActivityItem");
                        }
                    } catch (Exception e) {
                        MatrixLog.e(TAG, "[isLaunchActivity] %s", e);
                    }
                }
                return msg.what == LAUNCH_ACTIVITY;
            } else {
                return msg.what == LAUNCH_ACTIVITY || msg.what == RELAUNCH_ACTIVITY;
            }
        }
 }
```

activity耗时统计
onActivityFocused是通过插庄得到的  当执行到特定方法时，调用AppMethodBeat.at(Activity activity, boolean isFocus)
java/com/tencent/matrix/trace/core/AppMethodBeat.java
```
 public static void at(Activity activity, boolean isFocus) {
        String activityName = activity.getClass().getName();
        if (isFocus) {
            if (sFocusActivitySet.add(activityName)) {
                synchronized (listeners) {
                    for (IAppMethodBeatListener listener : listeners) {
                        listener.onActivityFocused(activity);
                    }
                }
                ...
            }
        }...
    }
```
java/com/tencent/matrix/trace/tracer/StartupTracer.java
```
  @Override
    public void onActivityFocused(Activity activity) {
        ...
        String activityName = activity.getClass().getName();
        // 若coldCost为初始值0，则说明这段代码从来没有运行过，那么认为是冷启动
        if (isColdStartup()) {
            boolean isCreatedByLaunchActivity = ActivityThreadHacker.isCreatedByLaunchActivity();
            ...
            String key = activityName + "@" + activity.hashCode();
            Long createdTime = createdTimeMap.get(key);
            if (createdTime == null) {
                createdTime = 0L;
            }
            //每次onActivityCreated()会将创建时间记录到createdTimeMap
            createdTimeMap.put(key, uptimeMillis() - createdTime);

            //若firstScreenCost为初始值0，则说明这是第一个获取焦点的Activity，记录时间差为首屏启动耗时
            if (firstScreenCost == 0) {
                this.firstScreenCost = uptimeMillis() - ActivityThreadHacker.getEggBrokenTime();
            }
            if (hasShowSplashActivity) {
               //若已经展示过了首屏Activity，则此Activity是真正的MainActivity，记录此时时间差为冷启动耗时
                coldCost = uptimeMillis() - ActivityThreadHacker.getEggBrokenTime();
            } else {
                //splashActivities是用户配置的闪屏页
                if (splashActivities.contains(activityName)) {
                    //标记为闪屏页已经展示
                    hasShowSplashActivity = true;
                } else if (splashActivities.isEmpty()) { //process which is has activity but not main UI process
                    if (isCreatedByLaunchActivity) {
                      // 声明的首屏Activity列表为空，则整个冷启动耗时就为首屏启动耗时
                        coldCost = firstScreenCost;
                    } else {
                        //启动时间不是通过activity创建记录获得 首屏时间为0，冷启动为进程创建时间
                        firstScreenCost = 0;
                        coldCost = ActivityThreadHacker.getApplicationCost();
                    }
                } else {
                    //splashActivities 非空但不包含activityName
                    if (isCreatedByLaunchActivity) {
                        ...
                        coldCost = firstScreenCost;
                    } else {
                        firstScreenCost = 0;
                        coldCost = ActivityThreadHacker.getApplicationCost();
                    }
                }
            }
            if (coldCost > 0) { // 分析冷启动耗时
                Long betweenCost = createdTimeMap.get(key);
                if (null != betweenCost && betweenCost >= 30 * 1000) {
                    //activity创建时间异常，不进行分享
                    MatrixLog.e(TAG, "%s cost too much time[%s] between activity create and onActivityFocused, "
                            + "just throw it.(createTime:%s) ", key, uptimeMillis() - createdTime, createdTime);
                    return;
                }
                analyse(ActivityThreadHacker.getApplicationCost(), firstScreenCost, coldCost, false);
            }

        } else if (isWarmStartUp()) {
        // 是否是温启动，这里isWarmStartUp标志位还依赖于监听ActivityLifecycleCallbacks
        // 温启动时间是当前时间减去上一次launch Activity 的时间
            isWarmStartUp = false;
            long warmCost = uptimeMillis() - lastCreateActivity;
            ...
            if (warmCost > 0) {
                analyse(0, 0, warmCost, true);
            }
        }

    }
    
   @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        if (activeActivityCount == 0 && coldCost > 0) {
            //上一次launch Activity时间
            lastCreateActivity = uptimeMillis();
            isWarmStartUp = true; //activity为0，但是coldCost已经存在，当前为温启动
        }
        activeActivityCount++;
        if (isShouldRecordCreateTime) { //记录每个activity的创建时间
            createdTimeMap.put(activity.getClass().getName() + "@" + activity.hashCode(), uptimeMillis());
        }
    }  
  
  
    @Override
    public void onApplicationCreateEnd() {
        if (!isHasActivity) {
            long applicationCost = ActivityThreadHacker.getApplicationCost();
            ...
            analyse(applicationCost, 0, applicationCost, false);
        }
    }   
 
  private void analyse(long applicationCost, long firstScreenCost, long allCost, boolean isWarmStartUp) {
        ...
        long[] data = new long[0];
        if (!isWarmStartUp && allCost >= coldStartupThresholdMs) { // for cold startup  默认10S
            data = AppMethodBeat.getInstance().copyData(ActivityThreadHacker.sApplicationCreateBeginMethodIndex);
            ActivityThreadHacker.sApplicationCreateBeginMethodIndex.release();

        } else if (isWarmStartUp && allCost >= warmStartupThresholdMs) { //温启动  默认4S
            data = AppMethodBeat.getInstance().copyData(ActivityThreadHacker.sLastLaunchActivityMethodIndex);
            ActivityThreadHacker.sLastLaunchActivityMethodIndex.release();
        }
        //子线程分析   AnalyseTask是获取栈信息，stackKey，构建issue，抛出异常
        MatrixHandlerThread.getDefaultHandler().post(new AnalyseTask(data, applicationCost, firstScreenCost, allCost, isWarmStartUp, ActivityThreadHacker.sApplicationCreateScene));
    }  
    
 private class AnalyseTask implements Runnable {
    。。。
 }     
```
Matrix方案的实用性
Matrix的方案适用于多Activity的架构，不适用于单Activity多Fragment的架构。对于后者，在使用上还需要一定的修改来进行适配。