http://liuwangshu.cn/framework/wms/1-wms-produce.html  android 8.0

前言
此前我用多篇文章介绍了WindowManager，这个系列我们来介绍WindowManager的管理者WMS，首先我们先来学习WMS是如何产生的。
本文源码基于Android 8.0，与Android 7.1.2相比有一个比较直观的变化就是Java FrameWork采用了Lambda表达式


1.WMS概述
WMS是系统的其他服务，无论对于应用开发还是Framework开发都是重点的知识，它的职责有很多，主要有以下几点：

窗口管理
   WMS是窗口的管理者，它负责窗口的启动、添加和删除，另外窗口的大小和层级也是由WMS进行管理的。
   窗口管理的核心成员有DisplayContent、WindowToken和WindowState。
窗口动画
   窗口间进行切换时，使用窗口动画可以显得更炫一些，窗口动画由WMS的动画子系统来负责，动画子系统的管理者为WindowAnimator。
输入系统的中转站
   通过对窗口的触摸从而产生触摸事件，InputManagerService（IMS）会对触摸事件进行处理，它会寻找一个最合适的窗口来处理触摸反馈信息，
   WMS是窗口的管理者，因此，WMS“理所应当”的成为了输入系统的中转站
Surface管理
  窗口并不具备有绘制的功能，因此每个窗口都需要有一块Surface来供自己绘制。为每个窗口分配Surface是由WMS来完成的。

WMS的职责可以简单总结为下图
WMS的职责.png

2.WMS的诞生
WMS的知识点非常多，在了解这些知识点前，我们十分有必要知道WMS是如何产生的。WMS是在SyetemServer进程中启动的，
不了解SyetemServer进程的可以查看在Android系统启动流程（三）解析SyetemServer进程启动过程这篇文章。
先来查看SyetemServer的main方法：
frameworks/base/services/java/com/android/server/SystemServer.java
```
public static void main(String[] args) {
       new SystemServer().run();
}
```

main方法中只调用了SystemServer的run方法，如下所示。
frameworks/base/services/java/com/android/server/SystemServer.java


run方法代码很多，这里截取了关键的部分，在注释1处加载了libandroid_servers.so。
在注释2处创建SystemServiceManager，它会对系统的服务进行创建、启动和生命周期管理。 
接下来的代码会启动系统的各种服务，
在注释3中的startBootstrapServices方法中用SystemServiceManager启动了ActivityManagerService、PowerManagerService、
   PackageManagerService等服务。
在注释4处的方法中则启动了BatteryService、UsageStatsService和WebViewUpdateService。
注释5处的startOtherServices方法中则启动了CameraService、AlarmManagerService、VrManagerService等服务，
   这些服务的父类为SystemService。
从注释3、4、5的方法名称可以看出，官方把大概100多个系统服务分为了三种类型，分别是引导服务、核心服务和其他服务，
  其中其他服务为一些非紧要和一些不需要立即启动的服务，WMS就是其他服务的一种。
我们来查看startOtherServices方法是如何启动WMS的：

frameworks/base/services/java/com/android/server/SystemServer.java
```
 private void startOtherServices() {
 ...
            traceBeginAndSlog("InitWatchdog");
            final Watchdog watchdog = Watchdog.getInstance();//1
            watchdog.init(context, mActivityManagerService);//2
            traceEnd();
            traceBeginAndSlog("StartInputManagerService");
            inputManager = new InputManagerService(context);//3
            traceEnd();
            traceBeginAndSlog("StartWindowManagerService");
            ConcurrentUtils.waitForFutureNoInterrupt(mSensorServiceStart, START_SENSOR_SERVICE);
            mSensorServiceStart = null;
            wm = WindowManagerService.main(context, inputManager,
                    mFactoryTestMode != FactoryTest.FACTORY_TEST_LOW_LEVEL,
                    !mFirstBoot, mOnlyCore, new PhoneWindowManager());//4
            ServiceManager.addService(Context.WINDOW_SERVICE, wm);//5
            ServiceManager.addService(Context.INPUT_SERVICE, inputManager);//6
            traceEnd();   
           ... 
           try {
            wm.displayReady();//7
               } catch (Throwable e) {
            reportWtf("making display ready", e);
              }
           ...
           try {
            wm.systemReady();//8
               } catch (Throwable e) {
            reportWtf("making Window Manager Service ready", e);
              }
            ...      
}
```

startOtherServices方法用于启动其他服务，其他服务大概有70多个，上面的代码只列出了WMS以及和它相关的IMS的启动逻辑，剩余的其他服务的启动逻辑也都大同小异。
在注释1、2处分别得到Watchdog实例并对它进行初始化，Watchdog用来监控系统的一些关键服务的运行状况，后文会再次提到它。
在注释3处创建了IMS，并赋值给IMS类型的inputManager对象。
注释4处执行了WMS的main方法，其内部会创建WMS，需要注意的是main方法其中一个传入的参数就是注释1处创建的IMS，WMS是输入事件的中转站，
 其内部包含了IMS引用并不意外。结合上文，我们可以得知WMS的main方法是运行在SystemServer的run方法中，换句话说就是运行在”system_server”线程”中，
  后面会再次提到”system_server”线程。
注释5和注释6处分别将WMS和IMS注册到ServiceManager中，这样如果某个客户端想要使用WMS，就需要先去ServiceManager中查询信息，
  然后根据信息与WMS所在的进程建立通信通路，客户端就可以使用WMS了。
注释7处用来初始化显示信息，
注释8处则用来通知WMS，系统的初始化工作已经完成，其内部调用了WindowManagerPolicy的systemReady方法。
我们来查看注释4处WMS的main方法，如下所示。
frameworks/base/services/core/java/com/android/server/wm/WindowManagerService.java
```
public static WindowManagerService main(final Context context, final InputManagerService im,
           final boolean haveInputMethods, final boolean showBootMsgs, final boolean onlyCore,
           WindowManagerPolicy policy) {
       DisplayThread.getHandler().runWithScissors(() ->//1
               sInstance = new WindowManagerService(context, im, haveInputMethods, showBootMsgs,
                       onlyCore, policy), 0);
       return sInstance;
   }
```
frameworks/base/services/core/java/com/android/server/DisplayThread.java
```
//ServiceThread继承HandlerThread
//DisplayThread是系统共享的单例前台线程
public final class DisplayThread extends ServiceThread {
    private static DisplayThread sInstance;
    private static Handler sHandler;

    private DisplayThread() {
        // DisplayThread runs important stuff, but these are not as important as things running in
        // AnimationThread. Thus, set the priority to one lower.
        super("android.display", Process.THREAD_PRIORITY_DISPLAY + 1, false /*allowIo*/);
    }

    private static void ensureThreadLocked() {
        if (sInstance == null) {
            sInstance = new DisplayThread();
            sInstance.start();
            sInstance.getLooper().setTraceTag(Trace.TRACE_TAG_ACTIVITY_MANAGER);
            sHandler = new Handler(sInstance.getLooper());
        }
    }
 }
```

在注释1处调用了DisplayThread的getHandler方法，用来得到DisplayThread的Handler实例。DisplayThread是一个单例的前台线程，
这个线程用来处理需要低延时显示的相关操作，并只能由WindowManager、DisplayManager和InputManager实时执行快速操作。
注释1处的runWithScissors方法中使用了Java8中的Lambda表达式，它等价于如下代码：
```
DisplayThread.getHandler().runWithScissors(new Runnable() {
        @Override
        public void run() {
         sInstance = new WindowManagerService(context, im, haveInputMethods, showBootMsgs,
                    onlyCore, policy);//2
        }
    }, 0);
```

在注释2处创建了WMS的实例，这个过程运行在Runnable的run方法中，而Runnable则传入到了DisplayThread对应Handler的runWithScissors方法中，
说明WMS的创建是运行在“android.display”线程中。需要注意的是，runWithScissors方法的第二个参数传入的是0，后面会提到。
来查看Handler的runWithScissors方法里做了什么
frameworks/base/core/java/android/os/Handler.java
```
public final boolean runWithScissors(final Runnable r, long timeout) {
       if (r == null) {
           throw new IllegalArgumentException("runnable must not be null");
       }
       if (timeout < 0) {
           throw new IllegalArgumentException("timeout must be non-negative");
       }
       if (Looper.myLooper() == mLooper) {//1
           r.run();
           return true;
       }
       BlockingRunnable br = new BlockingRunnable(r);
       return br.postAndWait(this, timeout);
   }
```
Scissors [ˈsɪzəz]  剪刀   剪断，删除  交叉
开头对传入的Runnable和timeout进行了判断，如果Runnable为null或者timeout小于0则抛出异常。
注释1处根据每个线程只有一个Looper的原理来判断当前的线程（”system_server”线程）是否是Handler所指向的线程（”android.display”线程），
如果是则直接执行Runnable的run方法，如果不是则调用BlockingRunnable的postAndWait方法，并将当前线程的Runnable作为参数传进去 ，
BlockingRunnable是Handler的内部类，代码如下所示。
frameworks/base/core/java/android/os/Handler.java
```
private static final class BlockingRunnable implements Runnable {
        private final Runnable mTask;
        private boolean mDone;
        public BlockingRunnable(Runnable task) {
            mTask = task;
        }
        @Override
        public void run() {
            try {
                mTask.run();//1
            } finally {
                synchronized (this) {
                    mDone = true;
                    notifyAll();
                }
            }
        }
        public boolean postAndWait(Handler handler, long timeout) {
            if (!handler.post(this)) {//2
                return false;
            }
            synchronized (this) {
                if (timeout > 0) {
                    final long expirationTime = SystemClock.uptimeMillis() + timeout;
                    while (!mDone) {
                        long delay = expirationTime - SystemClock.uptimeMillis();
                        if (delay <= 0) {
                            return false; // timeout
                        }
                        try {
                            wait(delay);
                        } catch (InterruptedException ex) {
                        }
                    }
                } else {
                    while (!mDone) {
                        try {
                            wait();//3
                        } catch (InterruptedException ex) {
                        }
                    }
                }
            }
            return true;
        }
    }
```

注释2处将当前的BlockingRunnable添加到Handler的任务队列中。前面runWithScissors方法的第二个参数为0，因此timeout等于0，
这样如果mDone为false的话会一直调用注释3处的wait方法使得当前线程（”system_server”线程）进入等待状态，那么等待的是哪个线程呢？
我们往上看，注释1处，执行了传入的Runnable的run方法（运行在”android.display”线程），执行完毕后在finally代码块中将mDone设置为true，
并调用notifyAll方法唤醒处于等待状态的线程，这样就不会继续调用注释3处的wait方法。
因此得出结论，”system_server”线程线程等待的就是”android.display”线程，一直到”android.display”线程执行完毕再执行”system_server”线程，
这是因为”android.display”线程内部执行了WMS的创建，显然WMS的创建优先级更高些。
WMS的创建就讲到这，最后我们来查看WMS的构造方法：

frameworks/base/services/core/java/com/android/server/wm/WindowManagerService .java
```
private WindowManagerService(Context context, InputManagerService inputManager,
         boolean haveInputMethods, boolean showBootMsgs, boolean onlyCore, WindowManagerPolicy policy) {
    ...
    mInputManager = inputManager;//1
    ...
     mDisplayManager = (DisplayManager)context.getSystemService(Context.DISPLAY_SERVICE);
     mDisplays = mDisplayManager.getDisplays();//2
     for (Display display : mDisplays) {
         createDisplayContentLocked(display);//3
     }
    ...
      mActivityManager = ActivityManager.getService();//4
    ...
     mAnimator = new WindowAnimator(this);//5
     mAllowTheaterModeWakeFromLayout = context.getResources().getBoolean(
             com.android.internal.R.bool.config_allowTheaterModeWakeFromWindowLayout);
     LocalServices.addService(WindowManagerInternal.class, new LocalService());
     initPolicy();//6
     // Add ourself to the Watchdog monitors.
     Watchdog.getInstance().addMonitor(this);//7
  ...
 }
```

注释1处用来保存传进来的IMS，这样WMS就持有了IMS的引用。
注释2处通过DisplayManager的getDisplays方法得到Display数组（每个显示设备都有一个Display实例），接着遍历Display数组，
在注释3处的createDisplayContentLocked方法会将Display封装成DisplayContent，DisplayContent用来描述一块屏幕。     多个屏幕是不是投屏？？
注释4处得到AMS实例，并赋值给mActivityManager ，这样WMS就持有了AMS的引用。
注释5处创建了WindowAnimator，它用于管理所有的窗口动画。
注释6处初始化了窗口管理策略的接口类WindowManagerPolicy（WMP），它用来定义一个窗口策略所要遵循的通用规范。
注释7处将自身也就是WMS通过addMonitor方法添加到Watchdog中，Watchdog用来监控系统的一些关键服务的运行状况（比如传入的WMS的运行状况），
   这些被监控的服务都会实现Watchdog.Monitor接口。Watchdog每分钟都会对被监控的系统服务进行检查，
   如果被监控的系统服务出现了死锁，则会杀死Watchdog所在的进程，也就是SystemServer进程。


查看注释6处的initPolicy方法，如下所示。
frameworks/base/services/core/java/com/android/server/wm/WindowManagerService.java
```
private void initPolicy() {
     UiThread.getHandler().runWithScissors(new Runnable() {
         @Override
         public void run() {
             WindowManagerPolicyThread.set(Thread.currentThread(), Looper.myLooper());
             mPolicy.init(mContext, WindowManagerService.this, WindowManagerService.this);//1
         }
     }, 0);
 }
```

//todo init 方法
initPolicy方法和此前讲的WMS的main方法的实现类似，注释1处执行了WMP的init方法，WMP是一个接口，
  init方法的具体实现在PhoneWindowManager（PWM）中。PWM的init方法运行在”android.ui”线程中，
 它的优先级要高于initPolicy方法所在的”android.display”线程，因此”android.display”线程要等PWM的init方法执行完毕后，
 处于等待状态的”android.display”线程才会被唤醒从而继续执行下面的代码。

在本文中共提到了3个线程，分别是”system_server”、”android.display”和”android.ui”，为了便于理解，下面给出这三个线程之间的关系。
WMS创建涉及到的线程.png

“system_server”线程中会调用WMS的main方法，main方法中会创建WMS，创建WMS的过程运行在”android.display”线程中，它的优先级更高一些，
因此要等创建WMS完毕后才会唤醒处于等待状态的”system_server”线程。
WMS初始化时会执行initPolicy方法，initPolicy方法会调用PWM的init方法，这个init方法运行在”android.ui”线程，并且优先级更高，
   因此要先执行完PWM的init方法后，才会唤醒处于等待状态的”android.display”线程。
PWM的init方法执行完毕后会接着执行运行在”system_server”线程的代码，比如本文前部分提到WMS的systemReady方法
//todo systemReady方法