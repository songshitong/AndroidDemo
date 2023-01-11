

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
            long intendedFrameTimeNs = startNs;
            if (isVsyncFrame) {
                doFrameEnd(token);
                intendedFrameTimeNs = getIntendedFrameTimeNs(startNs);
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