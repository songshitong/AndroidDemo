

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
  
     private void doFrameBegin(long token) {
        this.isVsyncFrame = true;
    }
    
     private void doQueueBegin(int type) {
        queueStatus[type] = DO_QUEUE_BEGIN;
        queueCost[type] = System.nanoTime();
    }

    private void doQueueEnd(int type) {
        queueStatus[type] = DO_QUEUE_END;
        queueCost[type] = System.nanoTime() - queueCost[type];
        synchronized (this) {
            callbackExist[type] = false;
        }
    }    
}
```