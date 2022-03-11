https://juejin.cn/post/6894206842277199880 android 9.0
Choreographer: 编舞者，用来控制当收到 VSync 信号后才开始绘制任务，保证绘制拥有完整的16.6ms。
[ˌkɒriˈɒɡrəfə(r)]
In dance, choreography is the act of designing dance. Choreography may also refer to the design itself, 
which is sometimes expressed by means of dance notation. A choreographer is one who creates dances. 
Dance choreography is sometimes called dance composition


入口: scheduleTraversals
从 Android-Window机制原理 可知，在调用 context.startActivity 后，经过 AMS 的一些处理，
后面又通过 Binder 调用目标进程的 ActivityThread.handleResumeActivity 方法，在这个方法里会回调目标 Activity 的 onResume 和 makeVisible 方法，
在 makeVisible 方法里完成 WindowManager.addView 的过程，这个过程调用了 ViewRootImpl.setView方 法，内部又调用其 scheduleTraversals 方法，
最后会走到 performTraversals 方法，接着就到了熟悉的 measure，layout，draw 三大流程了。
另外，查看 View.invalidate 方法的源码，也可以发现最后会调用到 ViewRootImpl.scheduleTraversals 方法。
//todo display相关
/frameworks/base/core/java/android/view/ViewRootImpl.java
```
// ViewRootImpl
final ViewRootHandler mHandler = new ViewRootHandler();

void scheduleTraversals() {
    if (!mTraversalScheduled) {
        // 保证同时间多次更改只会刷新一次，例如TextView连续两次setText()也只会走一次绘制流程
        mTraversalScheduled = true;
        // 添加同步屏障，保证 VSync 到来立即执行绘制
        mTraversalBarrier = mHandler.getLooper().getQueue().postSyncBarrier();
        mChoreographer.postCallback(Choreographer.CALLBACK_TRAVERSAL, mTraversalRunnable, null);
        // ...
    }
}

// mTraversalRunnable 是一个 Runnable 实例
final class TraversalRunnable implements Runnable {
    @Override
    public void run() {
        doTraversal();
    }
}

void doTraversal() {
    if (mTraversalScheduled) {
        mTraversalScheduled = false;
        // 移除同步屏障
        mHandler.getLooper().getQueue().removeSyncBarrier(mTraversalBarrier);
        // 真正执行View的measure，layout，draw流程
        performTraversals();
    }
}

```
首先使用 mTraversalScheduled 字段保证同时间多次更改只会刷新一次，然后为当前线程的 MessageQueue 添加同步屏障来屏蔽同步消息，
保证 VSync 信号到来后立即执行绘制，而不是要等前面的同步消息。调用 mChoreographer.postCallback() 方法发送了一个会在下一帧执行的回调，
即在下一个 VSync 信号到来时会执行TraversalRunnable-->doTraversal()-->performTraversals()-->绘制流程。
同步屏障可以参考 Android消息机制。
// https://link.juejin.cn/?target=https%3A%2F%2Fljd1996.github.io%2F2020%2F01%2F06%2FAndroid%25E6%25B6%2588%25E6%2581%25AF%25E6%259C%25BA%25E5%2588%25B6%2F%23postSyncBarrier


Choreographer实例化
首先看一下 Choreographer 的实例化过程，它在 ViewRootImpl 构造方法中实例化，ViewRootImpl 的实例化时机
//todo 可以参考 Android-Window机制原理https://link.juejin.cn/?target=https%3A%2F%2Fljd1996.github.io%2F2020%2F08%2F27%2FAndroid-Window%25E6%259C%25BA%25E5%2588%25B6%25E5%258E%259F%25E7%2590%2586%2F
```
// ViewRootImpl在WindowManager.addView时创建
public ViewRootImpl(Context context, Display display) {
    // ...
    mChoreographer = Choreographer.getInstance();
    // ...
}

/frameworks/base/core/java/android/view/Choreographer.java
public final class Choreographer {
    private static volatile Choreographer mMainInstance;

    private static final ThreadLocal<Choreographer> sThreadInstance = new ThreadLocal<Choreographer>() {
        @Override
        protected Choreographer initialValue() {
            Looper looper = Looper.myLooper();
            // VSYNC_SOURCE_APP = 0; -- APP
            // VSYNC_SOURCE_SURFACE_FLINGER = 1; -- SurfaceFlinger
            Choreographer choreographer = new Choreographer(looper, VSYNC_SOURCE_APP);
            // ...
            return choreographer;
        }
    };

    public static Choreographer getInstance() {
        return sThreadInstance.get();
    }
}

```

可知 Choreographer 和 Looper 一样都是线程单例的，由 ThreadLocal 实现，参考ThreadLocal原理。

//todo ThreadLocal https://link.juejin.cn/?target=https%3A%2F%2Fljd1996.github.io%2F2020%2F01%2F06%2FAndroid%25E6%25B6%2588%25E6%2581%25AF%25E6%259C%25BA%25E5%2588%25B6%2F%23ThreadLocal
```
public final class Choreographer {
    // 4.1以上默认是true
    // Enable/disable vsync for animations and drawing.
    private static final boolean USE_VSYNC = SystemProperties.getBoolean("debug.choreographer.vsync", true);

    // VSync事件接收器
    private final FrameDisplayEventReceiver mDisplayEventReceiver;

    private Choreographer(Looper looper, int vsyncSource) {
        mLooper = looper;
        mDisplayEventReceiver = USE_VSYNC ? new FrameDisplayEventReceiver(looper, vsyncSource) : null;
        // ...
        //上一次帧绘制时间点；
        mLastFrameTimeNanos = Long.MIN_VALUE;
        //帧间时长，一般等于16.7ms
        mFrameIntervalNanos = (long)(1000000000 / getRefreshRate());
    }
}

```
在 Choreographer 实例化时创建了一个 FrameDisplayEventReceiver 对象，它用来注册 Vsync 信号。


Vsync信号注册
DisplayEventReceiver
mDisplayEventReceiver 是 FrameDisplayEventReceiver 类型的实例，在Choreographer构造方法中实例化，
其父类为 DisplayEventReceiver。
/frameworks/base/core/java/android/view/DisplayEventReceiver.java
```
public abstract class DisplayEventReceiver {
    public static final int VSYNC_SOURCE_APP = 0;
    private long mReceiverPtr;

    public DisplayEventReceiver(Looper looper, int vsyncSource) {
        mMessageQueue = looper.getQueue();
        // 注册VSYNC信号监听者
        mReceiverPtr = nativeInit(new WeakReference<DisplayEventReceiver>(this), mMessageQueue, vsyncSource);
    }

    private static native long nativeInit(WeakReference<DisplayEventReceiver> receiver, MessageQueue messageQueue, int vsyncSource);
}

```

nativeInit
nativeInit是一个native方法，其实现在frameworks/base/core/jni/android_view_DisplayEventReceiver.cpp中：
```
static jlong nativeInit(JNIEnv* env, jclass clazz, jobject receiverWeak,
        jobject messageQueueObj, jint vsyncSource) {
    sp<MessageQueue> messageQueue = android_os_MessageQueue_getMessageQueue(env, messageQueueObj);
    sp<NativeDisplayEventReceiver> receiver = new NativeDisplayEventReceiver(env, receiverWeak, messageQueue, vsyncSource);
    status_t status = receiver->initialize();
    receiver->incStrong(gDisplayEventReceiverClassInfo.clazz); // retain a reference for the object
    return reinterpret_cast<jlong>(receiver.get());
}

```
NativeDisplayEventReceiver 继承自 DisplayEventDispatcher
/frameworks/base/libs/androidfw/DisplayEventDispatcher.cpp  
```
class DisplayEventDispatcher : public LooperCallback {
    public:
      DisplayEventDispatcher(const sp<Looper>& looper,
              ISurfaceComposer::VsyncSource vsyncSource = ISurfaceComposer::eVsyncSourceApp);
  
      status_t initialize();
    private:  
    DisplayEventReceiver mReceiver;
}
```

/frameworks/native/libs/gui/DisplayEventReceiver.cpp
```
DisplayEventReceiver::DisplayEventReceiver(ISurfaceComposer::VsyncSource vsyncSource) {
    sp<ISurfaceComposer> sf(ComposerService::getComposerService());
    if (sf != NULL) {
        mEventConnection = sf->createDisplayEventConnection(vsyncSource);
        if (mEventConnection != NULL) {
            mDataChannel = std::make_unique<gui::BitTube>();
            mEventConnection->stealReceiveChannel(mDataChannel.get());
        }
    }
}

/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp
sp<IDisplayEventConnection> SurfaceFlinger::createDisplayEventConnection(
        ISurfaceComposer::VsyncSource vsyncSource) {
    if (vsyncSource == eVsyncSourceSurfaceFlinger) {
        return mSFEventThread->createEventConnection();
    } else {
        // vsyncSource 是 APP
        return mEventThread->createEventConnection();
    }
}

```

从 SurfaceFlinger 启动与工作流程 可以知道 EventThread.createEventConnection 创建了一个对 Vsync 信号感兴趣的连接，
具体逻辑可以阅读这篇文章。initialize 方法如下：
```
// frameworks/base/libs/androidfw/DisplayEventDispatcher.cpp
status_t DisplayEventDispatcher::initialize() {
    // DisplayEventReceiver mReceiver;
    status_t result = mReceiver.initCheck();
    int rc = mLooper->addFd(mReceiver.getFd(), 0, Looper::EVENT_INPUT, this, NULL);
    if (rc < 0) {
        return UNKNOWN_ERROR;
    }
    return OK;
}

```
mReceiver 是 DisplayEventReceiver 类型实例，位于frameworks/native/libs/gui/DisplayEventReceiver.cpp。
mLooper->addFd(mReceiver.getFd(), 0, Looper::EVENT_INPUT, this, NULL) 用来监听 mReceiver 所获取的文件句柄，
当有存在对 Vsync 信号感兴趣的连接且接收到了 Vsync 信号时，会发送数据到 mReceiver, 
然后回调到 DisplayEventDispatcher 中的 handleEvent 方法，具体源码参考 SurfaceFlinger 启动与工作流程 中 addFd 的解析


请求Vsync信号
上面已经注册了一个对 Vsync 信号感兴趣的连接，在 Vsync 信号到来后，会回调到 DisplayEventDispatcher.handleEvent 方法。
于是接下来我们需要请求 Vsync 信号。看一下上面调用的代码：
mChoreographer.postCallback(Choreographer.CALLBACK_TRAVERSAL, mTraversalRunnable, null)，
它的调用链是: Choreographer.postCallback -> Choreographer.postCallbackDelayedInternal -> 
Choreographer.scheduleFrameLocked -> Choreographer.scheduleVsyncLocked 方法，
```
//RootViewImpl传入的token为null
public void postCallback(int callbackType, Runnable action, Object token) {
        postCallbackDelayed(callbackType, action, token, 0);
    }
public void postCallbackDelayed(int callbackType,
        Runnable action, Object token, long delayMillis) {
    postCallbackDelayedInternal(callbackType, action, token, delayMillis);
}    
    
/frameworks/base/core/java/android/view/Choreographer.java
private void postCallbackDelayedInternal(int callbackType, Object action, Object token, long delayMillis) {
    synchronized (mLock) {
        final long now = SystemClock.uptimeMillis();
        final long dueTime = now + delayMillis;
        // 对应类型的 CallbackQueue 添加 Callback
        mCallbackQueues[callbackType].addCallbackLocked(dueTime, action, token);
        // ...
        
        if (dueTime <= now) {
                //立即执行
                scheduleFrameLocked(now);
            } else {
                //延迟运行，最终也会走到scheduleFrameLocked()
                Message msg = mHandler.obtainMessage(MSG_DO_SCHEDULE_CALLBACK, action);
                msg.arg1 = callbackType;
                msg.setAsynchronous(true);
                mHandler.sendMessageAtTime(msg, dueTime);
            }
    }
}
```
如果没有延迟会执行scheduleFrameLocked()方法，有延迟就会使用 mHandler发送MSG_DO_SCHEDULE_CALLBACK消息，
并且注意到 使用msg.setAsynchronous(true)把消息设置成异步，这是因为前面设置了同步屏障，只有异步消息才会执行。
我们看下mHandler的对这个消息的处理
```
 private final class FrameHandler extends Handler {
         public FrameHandler(Looper looper) {
             super(looper);
         }
         @Override
         public void handleMessage(Message msg) {
             switch (msg.what) {
                 case MSG_DO_FRAME:
                     // 执行doFrame,即绘制过程
                    doFrame(System.nanoTime(), 0);
                    break;
                case MSG_DO_SCHEDULE_VSYNC:
                    //申请VSYNC信号，例如当前需要绘制任务时
                    doScheduleVsync();
                    break;
                case MSG_DO_SCHEDULE_CALLBACK:
                    //需要延迟的任务，最终还是执行上述两个事件
                    doScheduleCallback(msg.arg1);
                    break;
            }
        }
    }
    
   void doScheduleCallback(int callbackType) {
         synchronized (mLock) {
             if (!mFrameScheduled) {
                 final long now = SystemClock.uptimeMillis();
                 if (mCallbackQueues[callbackType].hasDueCallbacksLocked(now)) {
                     scheduleFrameLocked(now);
                 }
             }
         }
    }    
```
延迟运行最终也会走到scheduleFrameLocked()
如果系统未开启 VSYNC 机制，此时直接发送 MSG_DO_FRAME 消息到 FrameHandler。注意查看上面贴出的 FrameHandler 代码，
   此时直接执行 doFrame 方法。
Android 4.1 之后系统默认开启 VSYNC，在 Choreographer 的构造方法会创建一个 FrameDisplayEventReceiver，
  scheduleVsyncLocked 方法将会通过它申请 VSYNC 信号。
isRunningOnLooperThreadLocked 方法，其内部根据 Looper 判断是否在原线程，否则发送消息到 FrameHandler。
  最终还是会调用 scheduleVsyncLocked 方法申请 VSYNC 信号。
到这里，FrameHandler的作用很明显里了：发送异步消息（因为前面设置了同步屏障）。
  有延迟的任务发延迟消息、不在原线程的发到原线程、没开启VSYNC的直接走 doFrame 方法取执行绘制。
```
private void scheduleVsyncLocked() {
    mDisplayEventReceiver.scheduleVsync();
}

/frameworks/base/core/java/android/view/DisplayEventReceiver.java
// DisplayEventReceiver
public void scheduleVsync() {
    if (mReceiverPtr == 0) {
        // ...
    } else {
        nativeScheduleVsync(mReceiverPtr);
    }
}
```

接着就到了 native 层代码：
```
// frameworks/base/core/jni/android_view_DisplayEventReceiver.cpp
static void nativeScheduleVsync(JNIEnv* env, jclass clazz, jlong receiverPtr) {
    sp<NativeDisplayEventReceiver> receiver = reinterpret_cast<NativeDisplayEventReceiver*>(receiverPtr);
    status_t status = receiver->scheduleVsync();
    // ...
}

// frameworks/base/libs/androidfw/DisplayEventDispatcher.cpp
status_t DisplayEventDispatcher::scheduleVsync() {
    if (!mWaitingForVsync) {
        // ...
        // mReceiver 是 DisplayEventReceiver 实例
        status_t status = mReceiver.requestNextVsync();
        mWaitingForVsync = true;
    }
    return OK;
}

// frameworks/native/libs/gui/DisplayEventReceiver.cpp
status_t DisplayEventReceiver::requestNextVsync() {
    if (mEventConnection != NULL) {
        // 请求接收下一次Vsync信号的回调
        mEventConnection->requestNextVsync();
        return NO_ERROR;
    }
    return NO_INIT;
}

```

可以看到最终调用了 requestNextVsync 函数，关于 requestNextVsync 的逻辑已经在 SurfaceFlinger 启动与工作流程 中解析过了，
它用来请求接收下一次 Vsync 信号，可以唤醒 EventThread 线程，等到 Vsync 信号到来后回调给 APP。


Vsync回调流程
在 Vsync 信号到来后，便来到了这里。
DisplayEventDispatcher::handleEvent
```
/frameworks/base/libs/androidfw/DisplayEventDispatcher.cpp
int DisplayEventDispatcher::handleEvent(int, int events, void*) {
    // ...
    // 分发Vsync，实现方法在子类中
    dispatchVsync(vsyncTimestamp, vsyncDisplayId, vsyncCount);
    // 这里返回 1 是为了能一直保持 addFd 中添加的监听 callback
    // 具体逻辑可以参考之前 SurfaceFlinger 的解析
    return 1; // keep the callback
}

/frameworks/base/core/jni/android_view_DisplayEventReceiver.cpp
void NativeDisplayEventReceiver::dispatchVsync(nsecs_t timestamp, int32_t id, uint32_t count) {
    JNIEnv* env = AndroidRuntime::getJNIEnv();
    ScopedLocalRef<jobject> receiverObj(env, jniGetReferent(env, mReceiverWeakGlobal));
    if (receiverObj.get()) {
        env->CallVoidMethod(receiverObj.get(), gDisplayEventReceiverClassInfo.dispatchVsync, timestamp, id, count);
    }
    mMessageQueue->raiseAndClearException(env, "dispatchVsync");
}

//jni注册
int register_android_view_DisplayEventReceiver(JNIEnv* env) {
    // ...
    jclass clazz = FindClassOrDie(env, "android/view/DisplayEventReceiver");
    gDisplayEventReceiverClassInfo.clazz = MakeGlobalRefOrDie(env, clazz);
    gDisplayEventReceiverClassInfo.dispatchVsync = GetMethodIDOrDie(env,
            gDisplayEventReceiverClassInfo.clazz, "dispatchVsync", "(JII)V");
    return res;
}

```

由上可知，会调用到Java层 android/view/DisplayEventReceiver 的 dispatchVsync 方法
/frameworks/base/core/java/android/view/DisplayEventReceiver.java
```
public abstract class DisplayEventReceiver {
    private void dispatchVsync(long timestampNanos, int builtInDisplayId, int frame) {
        onVsync(timestampNanos, builtInDisplayId, frame);
    }

    public void onVsync(long timestampNanos, int builtInDisplayId, int frame) {
    }
}

```
onVsync 实现在子类 FrameDisplayEventReceiver 中。

FrameDisplayEventReceiver.onVsync
/frameworks/base/core/java/android/view/Choreographer.java
```
private final class FrameDisplayEventReceiver extends DisplayEventReceiver implements Runnable {
    private boolean mHavePendingVsync;
    private long mTimestampNanos;
    private int mFrame;

    @Override
    public void onVsync(long timestampNanos, int builtInDisplayId, int frame) {
        if (builtInDisplayId != SurfaceControl.BUILT_IN_DISPLAY_ID_MAIN) {
            // 非主display
            Log.d(TAG, "Received vsync from secondary display, but we don't support "
                    + "this case yet.  Choreographer needs a way to explicitly request "
                    + "vsync for a specific display to ensure it doesn't lose track "
                    + "of its scheduled vsync.");
            scheduleVsync();
            return;
        }

        long now = System.nanoTime();
        if (timestampNanos > now) {
            timestampNanos = now;
        }

        if (mHavePendingVsync) {
            Log.w(TAG, "Already have a pending vsync event.  There should only be one at a time.");
        } else {
            mHavePendingVsync = true;
        }

        mTimestampNanos = timestampNanos;
        mFrame = frame;
        // 会调用run方法  
        Message msg = Message.obtain(mHandler, this);
        msg.setAsynchronous(true);
        mHandler.sendMessageAtTime(msg, timestampNanos / TimeUtils.NANOS_PER_MS);
    }

    @Override
    public void run() {
        mHavePendingVsync = false;
        doFrame(mTimestampNanos, mFrame);
    }
}

```


Choreographer.doFrame
于是接收到 Vsync 信号后，又执行回到了 Choreographer:
/frameworks/base/core/java/android/view/Choreographer.java
```
private static final int SKIPPED_FRAME_WARNING_LIMIT = SystemProperties.getInt("debug.choreographer.skipwarning", 30);

void doFrame(long frameTimeNanos, int frame) {
    final long startNanos;
    synchronized (mLock) {
        if (!mFrameScheduled) {
            return; // no work to do
        }
        // 计划执行时间
        long intendedFrameTimeNanos = frameTimeNanos;
        startNanos = System.nanoTime();
        final long jitterNanos = startNanos - frameTimeNanos;
        if (jitterNanos >= mFrameIntervalNanos) {
            // 是否超过一帧的时间，因为虽然添加了同步屏障，但是如果有正在执行的同步任务，会导致doFrame延迟执行
            // 计算掉帧数
            final long skippedFrames = jitterNanos / mFrameIntervalNanos;
            if (skippedFrames >= SKIPPED_FRAME_WARNING_LIMIT) {
                // 默认掉帧超过30帧打印日志
                Log.i(TAG, "Skipped " + skippedFrames + " frames!  "
                    + "The application may be doing too much work on its main thread.");
            }
            final long lastFrameOffset = jitterNanos % mFrameIntervalNanos;
            frameTimeNanos = startNanos - lastFrameOffset;
        }

        if (frameTimeNanos < mLastFrameTimeNanos) {
            scheduleVsyncLocked(); // 请求下一次 Vsync 信号
            return;
        }

        if (mFPSDivisor > 1) {
            long timeSinceVsync = frameTimeNanos - mLastFrameTimeNanos;
            if (timeSinceVsync < (mFrameIntervalNanos * mFPSDivisor) && timeSinceVsync > 0) {
                scheduleVsyncLocked(); // 请求下一次 Vsync 信号
                return;
            }
        }

        mFrameInfo.setVsync(intendedFrameTimeNanos, frameTimeNanos);
        mFrameScheduled = false;
        mLastFrameTimeNanos = frameTimeNanos;
    }

    try {
        // 按类型执行，Choreographer中有四种类型 Input,animation,traversal,commit
        AnimationUtils.lockAnimationClock(frameTimeNanos / TimeUtils.NANOS_PER_MS);

        mFrameInfo.markInputHandlingStart();
        doCallbacks(Choreographer.CALLBACK_INPUT, frameTimeNanos);

        mFrameInfo.markAnimationsStart();
        doCallbacks(Choreographer.CALLBACK_ANIMATION, frameTimeNanos);

        mFrameInfo.markPerformTraversalsStart();
        doCallbacks(Choreographer.CALLBACK_TRAVERSAL, frameTimeNanos);

        doCallbacks(Choreographer.CALLBACK_COMMIT, frameTimeNanos);
    } finally {
        AnimationUtils.unlockAnimationClock();
    }
}
```
五种类型任务对应存入对应的CallbackQueue中，每当收到 VSYNC 信号时，Choreographer 将首先处理 INPUT 类型的任务，
   然后是 ANIMATION 类型，最后才是 TRAVERSAL 类型


Choreographer.doCallbacks
```
void doCallbacks(int callbackType, long frameTimeNanos) {
    CallbackRecord callbacks;
    synchronized (mLock) {
        final long now = System.nanoTime();
        // 根据指定的类型CallbackkQueue中查找到达执行时间的CallbackRecord
        callbacks = mCallbackQueues[callbackType].extractDueCallbacksLocked(now / TimeUtils.NANOS_PER_MS);
        // ...
    }
    // 迭代执行队列所有任务
    for (CallbackRecord c = callbacks; c != null; c = c.next) {
        c.run(frameTimeNanos);
    }
}

private static final class CallbackRecord {
    public CallbackRecord next;
    public long dueTime;
    public Object action; // Runnable or FrameCallback
    public Object token;

    public void run(long frameTimeNanos) {
        if (token == FRAME_CALLBACK_TOKEN) {
           //通过postFrameCallback 或 postFrameCallbackDelayed，会执行这里
            ((FrameCallback)action).doFrame(frameTimeNanos);
        } else { // 直接调用 Runnable 的 run 方法
            ((Runnable)action).run();
        }
    }
}

```

于是到这里就可以开始执行真正的绘制操作了。
mChoreographer.postCallback传的token是null，所以取出action，就是Runnable，执行run()，
  这里的action就是 ViewRootImpl 发起的绘制任务mTraversalRunnable了，那么这样整个逻辑就闭环了

啥时候 token == FRAME_CALLBACK_TOKEN 呢？答案是Choreographer的postFrameCallback()方法
```
public void postFrameCallback(FrameCallback callback) {
         postFrameCallbackDelayed(callback, 0);
     }
 
     public void postFrameCallbackDelayed(FrameCallback callback, long delayMillis) {
         ...
        //也是走到是postCallbackDelayedInternal，并且注意是CALLBACK_ANIMATION类型，
        //token是FRAME_CALLBACK_TOKEN，action就是FrameCallback
        postCallbackDelayedInternal(CALLBACK_ANIMATION,
                callback, FRAME_CALLBACK_TOKEN, delayMillis);
    }

    public interface FrameCallback {
        public void doFrame(long frameTimeNanos);
    }
```
可以看到postFrameCallback()传入的是FrameCallback实例，接口FrameCallback只有一个doFrame()方法。并且也是走到postCallbackDelayedInternal，
FrameCallback实例作为action传入，token则是FRAME_CALLBACK_TOKEN，并且任务是CALLBACK_ANIMATION类型。

总结
//todo 这个总结好多没有体现出来
Choreographer: 使 CPU/GPU 的绘制是在 VSYNC 到来时开始。Choreographer 初始化时会创建一个表示对 Vsync 信号感兴趣的连接，
    当有绘制请求时通过 postCallback 方法请求下一次 Vsync 信号，当信号到来后才开始执行绘制任务。
只有当 App 注册监听下一个 Vsync 信号后才能接收到 Vsync 到来的回调。如果界面一直保持不变，那么 App 不会去接收每隔 16.6ms 一次的 Vsync 事件，
   但底层依旧会以这个频率来切换每一帧的画面(也是通过监听 Vsync 信号实现)。即当界面不变时屏幕也会固定每 16.6ms 刷新，
  但 CPU/GPU 不走绘制流程。
当 View 请求刷新时，这个任务并不会马上开始，而是需要等到下一个 Vsync 信号到来时才开始；measure/layout/draw 流程运行完后，
   界面也不会立刻刷新，而会等到下一个 VSync 信号到来时才进行缓存交换和显示。
造成丢帧主要有两个原因：一是遍历绘制 View 树以及计算屏幕数据超过了16.6ms；二是主线程一直在处理其他耗时消息，
  导致绘制任务迟迟不能开始(同步屏障不能完全解决这个问题)。
可通过Choreographer.getInstance().postFrameCallback()来监听帧率情况，其用法和原理参考 postFrameCallback用法。
https://mp.weixin.qq.com/s/cg8cpnejCBXNBxKBC7P5iQ
疑问解答
1 丢帧(掉帧) ，是说 这一帧延迟显示 还是丢弃不再显示 ？
答：延迟显示，因为缓存交换的时机只能等下一个VSync了
2 布局层级较多/主线程耗时 是如何造成 丢帧的呢？
答：布局层级较多/主线程耗时 会影响CPU/GPU的执行时间，大于16.6ms时只能等下一个VSync了
3 16.6ms刷新一次 是啥意思？是每16.6ms都走一次 measure/layout/draw ？
答：屏幕的固定刷新频率是60Hz，即16.6ms。不是每16.6ms都走一次 measure/layout/draw，而是有绘制任务才会走，
  并且绘制时间间隔是取决于布局复杂度及主线程耗时
4 measure/layout/draw 走完，界面就立刻刷新了吗?
答：不是。measure/layout/draw 走完后 会在VSync到来时进行缓存交换和刷新
5 如果界面没动静止了，还会刷新吗？
答：屏幕会固定没16.6ms刷新，但CPU/GPU不走绘制流程。
6 可能你知道VSYNC，这个具体指啥？在屏幕刷新中如何工作的？
答：当扫描完一个屏幕后，设备需要重新回到第一行以进入下一次的循环，此时会出现的vertical sync pulse（垂直同步脉冲）
  来保证双缓冲在最佳时间点才进行交换。并且Android4.1后 CPU/GPU的绘制是在VSYNC到来时开始
7 可能你还听过神秘的Choreographer，这又是干啥的？
答：用于实现——"CPU/GPU的绘制是在VSYNC到来时开始"
8 可能你还听过屏幕刷新使用 双缓存、三缓存，这又是啥意思呢？   todo
答：双缓存是Back buffer、Frame buffer，用于解决画面撕裂。三缓存增加一个Back buffer，用于减少Jank

阅读这篇文章建议先阅读 SurfaceFlinger 启动与工作流程 这篇文章，然后结合 Choreographer 的工作流程，
可以对 Vsync 信号是怎么协调 App 端的绘制任务以及 SurfaceFlinger 的合成任务有一个比较清晰的认识
//todo Choreographer 的工作流程

//todo  http://gityuan.com/2017/02/25/choreographer/
WMS调用scheduleAnimationLocked()方法来设置mFrameScheduled=true来触发动画



Choreographer的postFrameCallback()通常用来计算丢帧情况，使用方式如下：
```
    //Application.java
         public void onCreate() {
             super.onCreate();
             //在Application中使用postFrameCallback
             Choreographer.getInstance().postFrameCallback(new FPSFrameCallback(System.nanoTime()));
         }


    public class FPSFrameCallback implements Choreographer.FrameCallback {

      private static final String TAG = "FPS_TEST";
      private long mLastFrameTimeNanos = 0;
      private long mFrameIntervalNanos;

      public FPSFrameCallback(long lastFrameTimeNanos) {
          mLastFrameTimeNanos = lastFrameTimeNanos;
          mFrameIntervalNanos = (long)(1000000000 / 60.0);
      }

      @Override
      public void doFrame(long frameTimeNanos) {

          //初始化时间
          if (mLastFrameTimeNanos == 0) {
              mLastFrameTimeNanos = frameTimeNanos;
          }
          final long jitterNanos = frameTimeNanos - mLastFrameTimeNanos;
          if (jitterNanos >= mFrameIntervalNanos) {
              final long skippedFrames = jitterNanos / mFrameIntervalNanos;
              if(skippedFrames>30){
                  //丢帧30以上打印日志
                  Log.i(TAG, "Skipped " + skippedFrames + " frames!  "
                          + "The application may be doing too much work on its main thread.");
              }
          }
          mLastFrameTimeNanos=frameTimeNanos;
          //注册下一帧回调
          Choreographer.getInstance().postFrameCallback(this);
      }
  }
```