http://liuwangshu.cn/framework/ims/1-ims-produce.html  Android 8.1

前言
很多同学可能会认为输入系统是不是和View的事件分发有些关联，确实是有些关联，只不过View事件分发只能算是输入系统事件传递的一部分。
这个系列讲的输入系统主要是我们不常接触的，但还是需要去了解的那部分

1. 输入事件传递流程的组成部分
   输入系统是外界与Android设备交互的基础，仅凭输入系统是无法完成输入事件传递的，因此需要输入系统和Android系统的其他成员来共同完成事件传递。
   输入系统事件传递需要经过以下几个部分。
   输入系统事件传递.png 

输入事件传递流程可以大致的分为三个部分，分别是输入系统部分、WMS处理部分和View处理部分。下面分别对这几个部分进行简单的介绍。

输入系统部分
输入系统部分主要又分为输入子系统和InputManagerService组成（以下简称IMS），在Android中还有一个IMS(IP Multimedia Subsystem)意为为IP多媒体子系统，
  不要搞混了。
Android的输入设备有很多种，比如屏幕、键盘、鼠标、游戏手柄、操纵杆等等，其中应用开发接触最多的屏幕。当输入设备可用时，
   Linux内核会在/dev/input中创建对应的设备节点。
用户操作这些输入设备时会产生各种事件比如按键事件、触摸事件、鼠标事件等。
输入事件所产生的原始信息会被Linux内核中的输入子系统采集，原始信息由Kernel space的驱动层一直传递到User space的设备节点。
Android提供了getevent和sendevent两个工具帮助开发者从设备节点读取输入事件和写入输入事件。
输入系统的KenelSpace和UserSpace.png
IMS所做的工作就是监听/dev/input下的所有的设备节点，当设备节点有数据时会将数据进行加工处理并找到合适的Window，将输入事件派发给它

WMS处理部分
在Android解析WindowManagerService（一）WMS的诞生这篇文章中我讲过WMS的职责有四种，如下图所示
图自己找。。。
WMS的职责之一就是输入系统的中转站，WMS作为Window的管理者，会配合IMS将输入事件交由合适的Window来处理。

View处理部分
View处理部分应该是大家最熟悉的了，一般情况下，输入事件最终会交由View来处理，应用开发者就可以通过一些回调方法轻松得到这个事件的封装类并对其进行处理，
比如onTouchEvent(MotionEvent ev)方法。关于View体系可以查看View体系这一系列文章  //todo view体系


2. IMS的诞生
   输入事件传递流程的组成部分我们已经了解了，本系列主要讲解输入系统部分中IMS对输入事件的处理，在这之前我们需要了解IMS的诞生。

2.1 SyetemServer处理部分
与AMS、WMS、PMS一样，IMS的在SyetemServer进程中被创建的，SyetemServer进程用来创建系统服务，不了解它的可以查看 Android系统启动流程（三）解析SyetemServer进程启动过程 这篇文章。
从SyetemServer的入口方法main方法开始讲起，如下所示。
frameworks/base/services/java/com/android/server/SystemServer.java
```
public static void main(String[] args) {
       new SystemServer().run();
   }
```

main方法中只调用了SystemServer的run方法，如下所示。
frameworks/base/services/java/com/android/server/SystemServer.java
```
private void run() {
  ...
    try {
        traceBeginAndSlog("StartServices");
        //启动引导服务
        startBootstrapServices();//1
        //启动核心服务
        startCoreServices();//2
        //启动其他服务
        startOtherServices();//3
        SystemServerInitThreadPool.shutdown();
    } catch (Throwable ex) {
        Slog.e("System", "******************************************");
        Slog.e("System", "************ Failure starting system services", ex);
        throw ex;
    } finally {
        traceEnd();
    }
    ...
}
```
本文要讲的IMS属于其他服务，这里列出其他服务以及它们的作用，见下表
其他服务	            作用
CameraService	    摄像头相关服务
AlarmManagerService	全局定时器管理服务
InputManagerService	管理输入事件
WindowManagerService	窗口管理服务
VrManagerService	VR模式管理服务
BluetoothService	蓝牙管理服务
NotificationManagerService	通知管理服务
DeviceStorageMonitorService	存储相关管理服务
LocationManagerService	定位管理服务
AudioService	音频相关管理服务


查看启动其他服务的注释3处的startOtherServices方法。
frameworks/base/services/java/com/android/server/SystemServer.java
```
 private void startOtherServices() {
 ...
           inputManager = new InputManagerService(context);//1
           traceEnd();
           traceBeginAndSlog("StartWindowManagerService");
           // WMS needs sensor service ready
           ConcurrentUtils.waitForFutureNoInterrupt(mSensorServiceStart, START_SENSOR_SERVICE);
           mSensorServiceStart = null;
           wm = WindowManagerService.main(context, inputManager,
                   mFactoryTestMode != FactoryTest.FACTORY_TEST_LOW_LEVEL,
                   !mFirstBoot, mOnlyCore, new PhoneWindowManager());//2
           ServiceManager.addService(Context.WINDOW_SERVICE, wm);
           ServiceManager.addService(Context.INPUT_SERVICE, inputManager);
           traceEnd();
...           
}
```

注释1处创建了IMS，注释2处执行了WMS的main方法，其内部会创建WMS。需要注意的是，main方法的其中一个参数就是注释1处创建的IMS，
在本地第1节中我们知道WMS是输入系统的中转站，其内部包含了IMS引用并不意外。紧接着将WMS和IMS添加到ServiceManager中进行统一的管理


2.2 InputManagerService构造方法
我们接着来查看IMS的构造方法。
frameworks/base/services/core/java/com/android/server/input/InputManagerService.java
```
public InputManagerService(Context context) {
        this.mContext = context;
        this.mHandler = new InputManagerHandler(DisplayThread.get().getLooper());//1
        mUseDevInputEventForAudioJack =
                context.getResources().getBoolean(R.bool.config_useDevInputEventForAudioJack);
        Slog.i(TAG, "Initializing input manager, mUseDevInputEventForAudioJack="
                + mUseDevInputEventForAudioJack);
        mPtr = nativeInit(this, mContext, mHandler.getLooper().getQueue());//2
        ...
    }
```

注释1处用android.display线程的Looper创建了InputManagerHandler，这样InputManagerHandler会运行在android.display线程，
android.display线程是系统共享的单例前台线程，这个线程内部执行了WMS的创建，具体见 Android解析WindowManagerService（一）WMS的诞生这篇文章。
注释2处调用了nativeInit方法，很明显是要通过JNI调用Navive方法。
frameworks/base/services/core/jni/com_android_server_input_InputManagerService.cpp
```
static jlong nativeInit(JNIEnv* env, jclass /* clazz */,
        jobject serviceObj, jobject contextObj, jobject messageQueueObj) {
    sp<MessageQueue> messageQueue = android_os_MessageQueue_getMessageQueue(env, messageQueueObj);
    if (messageQueue == NULL) {
        jniThrowRuntimeException(env, "MessageQueue is not initialized.");
        return 0;
    }
    NativeInputManager* im = new NativeInputManager(contextObj, serviceObj,
            messageQueue->getLooper());//1
    im->incStrong(0);
    return reinterpret_cast<jlong>(im);
}
```


注释1处创建了NativeInputManager，最后会调用reinterpret_cast运算符将NativeInputManager指针强制转换并返回（重新解释比特位）。
NativeInputManager的构造函数如下所示。
frameworks/base/services/core/jni/com_android_server_input_InputManagerService.cpp
```
NativeInputManager::NativeInputManager(jobject contextObj,
        jobject serviceObj, const sp<Looper>& looper) :
        mLooper(looper), mInteractive(true) {
    ...
    sp<EventHub> eventHub = new EventHub();
    mInputManager = new InputManager(eventHub, this, this);
}
```

NativeInputManager构造函数中创建了EventHub和InputManager，
EventHub通过Linux内核的INotify与Epoll机制监听设备节点，
通过EventHub的getEvent函数读取设备节点的增删事件和原始输入事件，本系列后续文章会详细介绍EventHub。InputManager的构造函数如下所示。
frameworks/native/services/inputflinger/InputManager.cpp
```
InputManager::InputManager(
        const sp<EventHubInterface>& eventHub,
        const sp<InputReaderPolicyInterface>& readerPolicy,
        const sp<InputDispatcherPolicyInterface>& dispatcherPolicy) {
    mDispatcher = new InputDispatcher(dispatcherPolicy);
    mReader = new InputReader(eventHub, readerPolicy, mDispatcher);
    initialize();
}

void InputManager::initialize() {
    mReaderThread = new InputReaderThread(mReader);
    mDispatcherThread = new InputDispatcherThread(mDispatcher);
}
```

InputManager构造函数中创建了InputReader和InputDispatcher，InputReader会不断循环读取EventHub中的原始输入事件，
将这些原始输入事件进行加工后交由InputDispatcher，InputDispatcher中保存了WMS中的所有Window信息
   （WMS会将窗口的信息实时的更新到InputDispatcher中），这样InputDispatcher就可以将输入事件派发给合适的Window。
InputReader和InputDispatcher都是耗时操作，因此在initialize函数中创建了供它们运行的线程InputReaderThread和InputDispatcherThread。
InputManagerService构造方法描绘了如下的IMS简图
IMS简图.png

从上面的简图可以看出来，IMS主要的工作都在Native层中，这些内容会在本系列的后续文章进行介绍。


总结
EventHub
EventHub通过Linux内核的INotify与Epoll机制监听设备节点，通过EventHub的getEvent函数读取设备节点的增删事件和原始输入事件
InputManager{
  InputReader   工作在InputReaderThread线程
     InputReader会不断循环读取EventHub中的原始输入事件，将这些原始输入事件进行加工后交由InputDispatcher
     线程不断执行threadLoop，事件读取后交由InputDispatcher，用epoll阻塞looper,就是handler的native looper
  InputDispatcher  工作在InputDispatcherThread线程
     InputDispatcher中保存了WMS中的所有Window信息，（WMS会将窗口的信息实时的更新到InputDispatcher中），
        这样InputDispatcher就可以将输入事件派发给合适的Window  通过InputChannel发送
}
InputManagerService   运行在SystemServer进程，里面的mHandler运行在android.display线程


