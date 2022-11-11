https://juejin.cn/post/6896382932639580167  android9.0

在解析了 Android-SurfaceFlinger启动与工作流程 和 Android-Choreographer工作原理 后，
明白了 Vsync 信号是怎么控制 SurfaceFlinger 进行合成 Layer 数据以及 Choreographer 是怎么控制开始 App 的绘制流程的，
另外 Android-View绘制流程 中贴出了从 Choreographer.postCallback 接收到 Vsync 信号后调用 ViewRootImpl.performTraversals 
开始 View 的 measure, layout, draw 流程的代码。接下来还有一个问题就是在 View 开始绘制后，
即 View.draw 方法中绘制的数据是怎么流入 SurfaceFlinger 进程中进行合成的。这里涉及到 Surface 的工作流程以及 
BufferQueue 处理图形缓存区的逻辑，关于 BufferQueue 在后面会分析到与它相关的几个重要方法，
不影响 Surface 的流程解析。
我会分为两篇文章来解析 Surface 相关的内容，这篇文章主要先看看 Surface 的创建流程以及软硬件绘制相关的逻辑，
下一篇再讲讲 Android-Surface之双缓冲及SurfaceView解析
//todo view 绘制流程  
https://link.juejin.cn/?target=https%3A%2F%2Fljd1996.github.io%2F2020%2F09%2F09%2FAndroid-View%25E7%25BB%2598%25E5%2588%25B6%25E5%258E%259F%25E7%2590%2586%2F

Surface创建
WMS.addWindow
从 Android-Window机制原理 知道当调用 WM.addView 方法时会调用到 ViewRootImpl.setView 方法，
然后通过 Binder 跨进程调用到 WMS.addWindow 方法，在该方法里创建了一个 WindowState 对象，进而调用其 attach 方法，
最终调用到 Session.windowAddedLocked 方法，Surface 的创建可以从这里开始解析：
//todo 验证session的流程
```
/frameworks/base/services/core/java/com/android/server/wm/Session.java
void windowAddedLocked(String packageName) {
    if (mSurfaceSession == null) {
        mSurfaceSession = new SurfaceSession();
    }
}

///frameworks/base/core/java/android/view/SurfaceSession.java
// SurfaceSession.java
private long mNativeClient; // SurfaceComposerClient*

/** Create a new connection with the surface flinger. */
public SurfaceSession() {
    mNativeClient = nativeCreate();
}

// frameworks/base/core/jni/android_view_SurfaceSession.cpp
static jlong nativeCreate(JNIEnv* env, jclass clazz) {
    SurfaceComposerClient* client = new SurfaceComposerClient();
    client->incStrong((void*)nativeCreate);
    return reinterpret_cast<jlong>(client);
}

```

可以看到 SurfaceSession 构造方法中通过 nativeCreate 方法返回了一个 SurfaceComposerClient 指针，它表示一个跟 SurfaceFlinger 的连接。

当其第一次被使用时会调用如下函数：
```
/frameworks/native/libs/gui/SurfaceComposerClient.cpp
void SurfaceComposerClient::onFirstRef() {
    sp<ISurfaceComposer> sf(ComposerService::getComposerService());
    sp<ISurfaceComposerClient> conn = (rootProducer != nullptr) ? sf->createScopedConnection(rootProducer) : sf->createConnection();
    if (conn != 0) {
        mClient = conn;
    }
    // ...
}

/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp
sp<ISurfaceComposerClient> SurfaceFlinger::createConnection() {
    return initClient(new Client(this)); // initClient方法只是调用initCheck检查了一下
}

```
Client
```
 /frameworks/native/services/surfaceflinger/Client.cpp
 Client::Client(const sp<SurfaceFlinger>& flinger)
      : Client(flinger, nullptr)
  {
  }
```
即创建了一个实现 ISurfaceComposerClient 接口的 Client 对象。
总结一下上面 WMS 中的相关操作：WMS 创建了一个 WindowState 对象表示客户端的一个 Window, 
接着调用 WindowState.attach 方法创建了一个 SurfaceSession 对象，SurfaceSession 表示一个跟 SurfaceFlinger 的连接，
它创建了一个 SurfaceComposerClient 对象，然后 SurfaceComposerClient 又创建了一个 Client 对象


创建Surface
一个 ViewRootImpl 对象对应了一个 Surface 对象，在其源码中有如下代码：
/frameworks/base/core/java/android/view/ViewRootImpl.java
```
// These can be accessed by any thread, must be protected with a lock.
      // Surface can never be reassigned or cleared (use Surface.clear()).
public final Surface mSurface = new Surface();

```

但此时这个 Surface 对象啥东西都没有，构造方法也是空的。根据 Android-Choreographer工作流程 中的分析，
ViewRootImpl.setView 方法会调用到 Choreographer 的逻辑，进而在 Vsync 信号到来后执行 ViewRootImpl.performTraversals 方法：
/frameworks/base/core/java/android/view/ViewRootImpl.java
```
private void performTraversals() {
    relayoutWindow(params, viewVisibility, insetsPending)
    // measure, layout, draw
}

private int relayoutWindow(...) throws RemoteException {
    // 注意最后一个参数 mSurface 便是之前创建的 Surface 对象
    // 会调用 WMS.relayoutWindow 方法
    mWindowSession.relayout(mWindow, ..., mSurface);
    // ...
}

/frameworks/base/services/core/java/com/android/server/wm/WindowManagerService.java
// WMS
public int relayoutWindow(Session session, ..., Surface outSurface) {
    result = createSurfaceControl(outSurface, result, win, winAnimator);
    // ...
}

private int createSurfaceControl(Surface outSurface, int result, WindowState win, WindowStateAnimator winAnimator) {
    WindowSurfaceController surfaceController = winAnimator.createSurfaceLocked(win.mAttrs.type, win.mOwnerUid);
    if (surfaceController != null) {
        surfaceController.getSurface(outSurface);
    } else {
        outSurface.release();
    }
    return result;
}

```
/frameworks/base/services/core/java/com/android/server/wm/WindowSurfaceController.java
```
SurfaceControl mSurfaceControl;
public WindowSurfaceController(SurfaceSession s, String name, int w, int h, int format,
              int flags, WindowStateAnimator animator, int windowType, int ownerUid) {
         mAnimator = animator;
          mSurfaceW = w;
          mSurfaceH = h;
          title = name;
          mService = animator.mService;
          final WindowState win = animator.mWin;
          mWindowType = windowType;
          mWindowSession = win.mSession;
          final SurfaceControl.Builder b = win.makeSurface()
                  .setParent(win.getSurfaceControl())
                  .setName(name)
                  .setSize(w, h)
                  .setFormat(format)
                  .setFlags(flags)
                  .setMetadata(windowType, ownerUid);
          mSurfaceControl = b.build();
      }
```

上面调用 createSurfaceLocked 方法创建了一个 WindowSurfaceController 对象，然后在 WindowSurfaceController 的构造方法
中会创建一个 SurfaceControl 对象(mSurfaceControl, 通过构造方法实例化，代码不贴了)，顾名思义这个对象是用来维护 Surface 的，
我们看一下这个类的构造方法
```
/frameworks/base/core/java/android/view/SurfaceControl.java
private SurfaceControl(...) {
    // 返回 native SurfaceControl 指针
    mNativeObject = nativeCreate(session, name, w, h, format, flags, parent != null ? parent.mNativeObject : 0, windowType, ownerUid);
}

// frameworks/base/core/jni/android_view_SurfaceControl.cpp
static jlong nativeCreate(...) {
    // client 即上面创建的 SurfaceComposerClient 对象
    sp<SurfaceComposerClient> client(android_view_SurfaceSession_getClient(env, sessionObj));
    sp<SurfaceControl> surface;
    client->createSurfaceChecked(String8(name.c_str()), w, h, format, &surface, flags, parent, windowType, ownerUid);
    return reinterpret_cast<jlong>(surface.get());
}

/frameworks/native/libs/gui/SurfaceComposerClient.cpp
status_t SurfaceComposerClient::createSurfaceChecked(..., sp<SurfaceControl>* outSurface, ...) {
    // ...
    sp<IGraphicBufferProducer> gbp;
    err = mClient->createSurface(name, w, h, format, flags, parentHandle, windowType, ownerUid, &handle, &gbp);
    if (err == NO_ERROR) {
        *outSurface = new SurfaceControl(this, handle, gbp, true /* owned */);
    }
    return err;
}

/frameworks/native/services/surfaceflinger/Client.cpp
status_t Client::createSurface(const String8& name,
        uint32_t w, uint32_t h, PixelFormat format, uint32_t flags,
        const sp<IBinder>& parentHandle, int32_t windowType, int32_t ownerUid,
        sp<IBinder>* handle,
        sp<IGraphicBufferProducer>* gbp) {
    // ...
    flinger->createLayer(name, client, w, h, format, flags, windowType, ownerUid, handle, gbp, parent);
}
```

Surface 在 SurfaceFlinger 中对应的实体是 Layer 对象，在 createLayer 方法中会创建好几种 Layer。
可以知道在创建 SurfaceControl 对象时通过 SurfaceFlinger 创建了一个对应 Surface 的 Layer 对象。
接下来就到了 WindowSurfaceController.getSurface 方法：
```
 /frameworks/base/services/core/java/com/android/server/wm/WindowSurfaceController.java
void getSurface(Surface outSurface) {
    outSurface.copyFrom(mSurfaceControl);
}

/frameworks/base/core/java/android/view/Surface.java
// Surface.java
public void copyFrom(SurfaceControl other) {
    // 即上面返回的 native SurfaceControl 指针
    long surfaceControlPtr = other.mNativeObject;
    long newNativeObject = nativeGetFromSurfaceControl(surfaceControlPtr);

    synchronized (mLock) {
        if (mNativeObject != 0) {
            nativeRelease(mNativeObject);
        }
        setNativeObjectLocked(newNativeObject);
    }
}

private void setNativeObjectLocked(long ptr) {
    if (mNativeObject != ptr) {
        mNativeObject = ptr;
        if (mHwuiContext != null) {
            mHwuiContext.updateSurface();
        }
    }
}

// frameworks/base/core/jni/android_view_Surface.cpp
static jlong nativeGetFromSurfaceControl(JNIEnv* env, jclass clazz, jlong surfaceControlNativeObj) {
    // ctrl 是前面创建的 SurfaceControl 对象
    sp<SurfaceControl> ctrl(reinterpret_cast<SurfaceControl *>(surfaceControlNativeObj));
    sp<Surface> surface(ctrl->getSurface());
    if (surface != NULL) {
        surface->incStrong(&sRefBaseOwner);
    }
    return reinterpret_cast<jlong>(surface.get());
}


/frameworks/native/libs/gui/SurfaceControl.cpp
sp<Surface> SurfaceControl::getSurface() const
{
    Mutex::Autolock _l(mLock);
    if (mSurfaceData == 0) {
        return generateSurfaceLocked();
    }
    return mSurfaceData;
}

sp<Surface> SurfaceControl::generateSurfaceLocked() const
{
    // mGraphicBufferProducer 是上面创建的 gbp 对象，下面有从SurfaceFlinger的创建过程
    mSurfaceData = new Surface(mGraphicBufferProducer, false);
    return mSurfaceData;
}

```
上面通过 nativeGetFromSurfaceControl 方法返回了一个 native 层创建的 Surface 指针，并赋值给了 
Java 层 Surface 对象的 mNativeObject 属性


IGraphicBufferProducer
上面看到了一个 IGraphicBufferProducer gbp 对象，这是一个很重要的对象，我们看一下它是怎么被创建的
/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp
```
status_t SurfaceFlinger::createLayer(const String8& name, const sp<Client>& client, ..., sp<IGraphicBufferProducer>* gbp, ...) {
    switch (flags & ISurfaceComposerClient::eFXSurfaceMask) {
        case ISurfaceComposerClient::eFXSurfaceNormal:
            // 以 BufferLayer 为例
            result = createBufferLayer(client, uniqueName, w, h, flags, format, handle, gbp, &layer);
            break;
        // ...
    }
}

status_t SurfaceFlinger::createBufferLayer(const sp<Client>& client,
        const String8& name, uint32_t w, uint32_t h, uint32_t flags, PixelFormat& format,
        sp<IBinder>* handle, sp<IGraphicBufferProducer>* gbp, sp<Layer>* outLayer)
{
    sp<BufferLayer> layer = new BufferLayer(this, client, name, w, h, flags);
    status_t err = layer->setBuffers(w, h, format, flags);
    if (err == NO_ERROR) {
        *handle = layer->getHandle();
        *gbp = layer->getProducer(); // 获取 gbp
        *outLayer = layer;
    }
    return err;
}

//BufferLayer是SurfaceFlinger中Layer的一种实现
frameworks/native/services/surfaceflinger/BufferLayer.h
class BufferLayer : public Layer, public BufferLayerConsumer::ContentsChangedListener {}

/frameworks/native/services/surfaceflinger/BufferLayer.cpp
sp<IGraphicBufferProducer> BufferLayer::getProducer() const {
    return mProducer;
}
/frameworks/native/services/surfaceflinger/BufferLayer.cpp
void BufferLayer::onFirstRef() {
    // Creates a custom BufferQueue for SurfaceFlingerConsumer to use
    sp<IGraphicBufferProducer> producer;
    sp<IGraphicBufferConsumer> consumer;
    //创建BufferQueue及其对应的produer和consumer
    BufferQueue::createBufferQueue(&producer, &consumer, true);
    mProducer = new MonitoredProducer(producer, mFlinger, this);
    mConsumer = new BufferLayerConsumer(consumer, mFlinger->getRenderEngine(), mTextureName, this);   
    mConsumer->setConsumerUsageBits(getEffectiveUsage(0));
    mConsumer->setContentsChangedListener(this);
    mConsumer->setName(mName);
    //不支持三重缓冲的设置为2
    if (mFlinger->isLayerTripleBufferingDisabled()) {
        mProducer->setMaxDequeuedBufferCount(2);
    }
    // ...
}
```

/frameworks/native/services/surfaceflinger/MonitoredProducer.cpp
```
MonitoredProducer::MonitoredProducer(const sp<IGraphicBufferProducer>& producer,
          const sp<SurfaceFlinger>& flinger,
          const wp<Layer>& layer) :
      mProducer(producer),
      mFlinger(flinger),
      mLayer(layer) {}
      
status_t MonitoredProducer::requestBuffer(int slot, sp<GraphicBuffer>* buf) {
      return mProducer->requestBuffer(slot, buf);
  }     
```
mProducer 是 MonitoredProducer 实例，它是一个装饰类，实际功能都委托给了其 producer 属性
/frameworks/native/libs/gui/BufferQueue.cpp
```
void BufferQueue::createBufferQueue(sp<IGraphicBufferProducer>* outProducer,
        sp<IGraphicBufferConsumer>* outConsumer, bool consumerIsSurfaceFlinger) {
    sp<BufferQueueCore> core(new BufferQueueCore());
    sp<IGraphicBufferProducer> producer(new BufferQueueProducer(core, consumerIsSurfaceFlinger));
    sp<IGraphicBufferConsumer> consumer(new BufferQueueConsumer(core));
    *outProducer = producer;
    *outConsumer = consumer;
}
```

BufferLayerConsumer的创建  通过producer在BufferQueue中产生图像数据GraphicBuffer，然后交给BufferLayerConsumer消费
frameworks/native/services/surfaceflinger/BufferLayerConsumer.cpp
```
BufferLayerConsumer::BufferLayerConsumer(const sp<IGraphicBufferConsumer>& bq,
                                         RE::RenderEngine& engine, uint32_t tex, Layer* layer)
      : ConsumerBase(bq, false),
        ....
        mCurrentTexture(BufferQueue::INVALID_BUFFER_SLOT) {
    memcpy(mCurrentTransformMatrix, mtxIdentity.asArray(), sizeof(mCurrentTransformMatrix));
    mConsumer->setConsumerUsageBits(DEFAULT_USAGE_FLAGS);
}
```
消费过程   
调用流程SurfaceFlinger.handleMessageInvalidate->sf.handlePageFlip->layer.latchBuffer->BufferLayer.latchBuffer->BufferLayerConsumer.updateTexImage
frameworks/native/services/surfaceflinger/BufferLayerConsumer.cpp
```
status_t BufferLayerConsumer::updateTexImage(BufferRejecter* rejecter, const DispSync& dispSync,
                                             bool* autoRefresh, bool* queuedBuffer,
                                             uint64_t maxFrameNumber) {
   ...                                          
  BufferItem item;
  // 1. 调用acquireBufferLocked获取一个Slot
    // Acquire the next buffer.
    // In asynchronous mode the list is guaranteed to be one buffer
    // deep, while in synchronous mode we use the oldest buffer.
    status_t err = acquireBufferLocked(&item, computeExpectedPresent(dispSync), maxFrameNumber);    
   ...
   /2. 消费完毕，释放Slot
   // Release the previous buffer.
    err = updateAndReleaseLocked(item, &mPendingRelease);
    if (err != NO_ERROR) {
        return err;
    }                                           
}
```
acquireBufferLocked的实现如下：
```
frameworks/native/services/surfaceflinger/BufferLayerConsumer.cpp
status_t BufferLayerConsumer::acquireBufferLocked(BufferItem* item, nsecs_t presentWhen,
                                                  uint64_t maxFrameNumber) {
    status_t err = ConsumerBase::acquireBufferLocked(item, presentWhen, maxFrameNumber);
    ...
    return NO_ERROR;
}

frameworks/native/libs/gui/ConsumerBase.cpp
status_t ConsumerBase::acquireBufferLocked(BufferItem *item,
        nsecs_t presentWhen, uint64_t maxFrameNumber) {
  //mConsumer就是IGraphicBufferConsumer，BufferQueue的consumer      
  status_t err = mConsumer->acquireBuffer(item, presentWhen, maxFrameNumber);  
  ...
  return OK;     
}
```
updateAndReleaseLocked方法的流程如下：
```
status_t BufferLayerConsumer::updateAndReleaseLocked(const BufferItem& item,
      status_t err = NO_ERROR;

    int slot = item.mSlot;

    // Do whatever sync ops we need to do before releasing the old slot.
    if (slot != mCurrentTexture) {
        err = syncForReleaseLocked();
        if (err != NO_ERROR) {
            // Release the buffer we just acquired.  It's not safe to
            // release the old buffer, so instead we just drop the new frame.
            // As we are still under lock since acquireBuffer, it is safe to
            // release by slot.
            releaseBufferLocked(slot, mSlots[slot].mGraphicBuffer);
            return err;
        }
    }  
  ....                        
}

frameworks/native/libs/gui/ConsumerBase.cpp
status_t ConsumerBase::releaseBufferLocked(
        int slot, const sp<GraphicBuffer> graphicBuffer,
        EGLDisplay display, EGLSyncKHR eglFence) {
   //调用BufferQueueConsumer的releaseBuffer方法     
   status_t err = mConsumer->releaseBuffer(slot, mSlots[slot].mFrameNumber,
            display, eglFence, mSlots[slot].mFence);
    ....          
 }
```

小结
在 Java 层中 ViewRootImpl 实例中持有一个 Surface 对象，该 Surface 对象中的 mNativeObject 属性指向 native 层中创建的 Surface 对象，
native 层的 Surface 对应 SurfaceFlinger 中的 Layer 对象，它持有 Layer 中的 BufferQueueProducer 生产者指针，
在后面的绘制过程中 Surface 会通过这个生产者来请求图形缓存区，在 Surface 上绘制的内容就是存入到这个缓存区里的，
最终再交由 SurfaceFlinger 通过 BufferQueueConsumer 消费者取出这些缓存数据，并合成渲染送到显示器显示
 


硬件加速&软件绘制
概述
Resterization: 栅格化(光栅化)。栅格化把 Button, TextView 等组件拆分到不同的像素上进行显示，这是一个很费时的操作，
   GPU可以加快栅格化的操作。
Android 系统的 UI 从绘制到显示在屏幕上可分为两个步骤：
1 Android APP 进程: 将 UI 绘制到一个图形缓冲区 GraphicBuffer 中，然后通知 SurfaceFlinger 进行合成。
2 SurfaceFlinger 进程: 将 GraphicBuffer 数据合成并交给屏幕缓存区去显示，这一步本身就是通过硬件(OpenGL 和 HardWare Composer)去完成的。

因此我们说的硬件加速一般是指在 APP 进程中将图形通过 GPU 加速渲染到 GraphicBuffer 的过程。GPU 作为一个硬件，
   用户空间不能直接使用，GPU 厂商会按照 OpenGL 的规范实现一套 API 驱动来调用它的相关功能。


软件渲染
当 App 更新部分 UI 时，CPU 会遍历 View Tree 计算出需要重绘的脏区，接着在 View 层次结构中绘制所有跟脏区相交的区域，
     因此软件绘制会绘制到不需要重绘的视图。
软件绘制的绘制过程是在主线程进行的，可能会造成卡顿等情况。
软件绘制把要绘制的内容写进一个 Bitmap 位图，在之后的渲染过程中，这个 Bitmap 的像素内容会填充到 Surface 的缓存区里。
软件绘制使用 Skia 库。


硬件渲染
当 App 更新部分 UI 时，CPU 会计算出脏区，但是不会立即执行绘制命令，而是将 drawXXX 函数作为绘制指令(DrawOp)记录在一个列表(DisplayList)中，
    然后交给单独的 Render 线程使用 GPU 进行硬件加速渲染。
只需要针对需要更新的 View 对象的脏区进行记录或更新，无需更新的 View 对象则能重用先前 DisplayList 中记录的指令。
硬件加速是在单独的 Render 线程中完成绘制的，分担了主线程的压力，提高了响应速度。
硬件绘制使用 OpenGL 在 GPU 上完成，OpenGL 是跨平台的图形 API，为 2D/3D 图形处理硬件制定了标准的软件接口。听说在
    Android 新版本中，Google 开始逐渐让 Skia 接手 OpenGL，实现间接统一调用。
硬件加速有几个缺陷：兼容性（部分绘制函数不支持速），内存消耗，电量消耗（GPU耗电）等。
从 Android 3.0(API 11)开始支持硬件加速，Android 4.0(API 14)默认开启硬件加速。

关于 OpenGl ES 的简单使用可以参考：Android-OpenGL-ES笔记和官方文档
https://link.juejin.cn/?target=https%3A%2F%2Fljd1996.github.io%2F2019%2F08%2F19%2FAndroid-OpenGL-ES%25E7%25AC%2594%25E8%25AE%25B0%2F
https://link.juejin.cn/?target=https%3A%2F%2Fdeveloper.android.com%2Ftraining%2Fgraphics%2Fopengl


配置硬件加速
Application: 在 Manifest 文件的 application 标签添加 android:hardwareAccelerated="boolean"
Activity: 在 Manifest 文件的 activity 标签添加 android:hardwareAccelerated="boolean"
Window: getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
View: setLayerType(View.LAYER_TYPE_HARDWARE/*View.LAYER_TYPE_SOFTWARE*/, mPaint)

判断是否支持硬件加速：
```
// 如果Application、Activity配置了不开启硬件加速，则返回false
view.isHardwareAccelerated()

// 假如没有设置setLayerType，则受到Application、Activity的影响
// 假如设置了setLayerType，其返回值则受到setLayerType参数的影响
canvas.isHardwareAccelerated()

```

在绘制过程中会通过 VRImpl.enableHardwareAcceleration 方法去判断是否需要开启硬件加速：
/frameworks/base/core/java/android/view/ViewRootImpl.java
```
private void enableHardwareAcceleration(WindowManager.LayoutParams attrs) {
    mAttachInfo.mHardwareAccelerated = false;
    mAttachInfo.mHardwareAccelerationRequested = false;

    // Don't enable hardware acceleration when the application is in compatibility mode
    if (mTranslator != null) return;

    // Try to enable hardware acceleration if requested
    final boolean hardwareAccelerated = (attrs.flags & WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED) != 0;

    if (hardwareAccelerated) {
        // 创建硬件加速的渲染器
        mAttachInfo.mThreadedRenderer = ThreadedRenderer.create(mContext, translucent,
                          attrs.getTitle().toString());
    }
}

```




软件绘制
从 Android-View绘制流程 知道软件绘制从 VRImpl.drawSoftware 方法开始
//todo view
VRImpl.drawSoftware
```
private boolean drawSoftware(Surface surface, AttachInfo attachInfo, int xoff, int yoff,
        boolean scalingRequired, Rect dirty, Rect surfaceInsets) {

    // Draw with software renderer.
    final Canvas canvas;
    canvas = mSurface.lockCanvas(dirty);
    canvas.setDensity(mDensity);

    try {
        dirty.setEmpty();
        mView.draw(canvas);
    } finally {
        surface.unlockCanvasAndPost(canvas);
    }
    return true;
}

```

上面的软件绘制可以分成三个步骤：
通过 Surface.lockCanvas 方法向 SurfaceFlinger 申请图形缓存区
调用 View.draw 方法将绘制数据写入缓存区
通过 Surface.unlockCanvasAndPost 方法将填充了数据的图形缓存区入队列并通知 SurfaceFlinger 进行合成

Surface.lockCanvas
/frameworks/base/core/java/android/view/Surface.java
```
public Canvas lockCanvas(Rect inOutDirty) throws Surface.OutOfResourcesException, IllegalArgumentException {
    synchronized (mLock) {
        if (mLockedObject != 0) {
            // refuse to re-lock the Surface.
            throw new IllegalArgumentException("Surface was already locked");
        }
        mLockedObject = nativeLockCanvas(mNativeObject, mCanvas, inOutDirty);
        return mCanvas;
    }
}

```

这里调用 native 方法 nativeLockCanvas 来获取 mLockedObject 指针，上面说过 mNativeObject 指向 native 层创建的 Surface 对象。
```
// frameworks/base/core/jni/android_view_Surface.cpp
static jlong nativeLockCanvas(JNIEnv* env, jclass clazz, jlong nativeObject, jobject canvasObj, jobject dirtyRectObj) {
    sp<Surface> surface(reinterpret_cast<Surface *>(nativeObject));

    // 根据 Dirty 区域创建 native Rect 对象
    Rect dirtyRect(Rect::EMPTY_RECT);
    Rect* dirtyRectPtr = NULL;

    if (dirtyRectObj) {
        // 根据 Java 层 dirtyRectObj 初始化 dirtyRect 和 dirtyRectPtr
    }

    ANativeWindow_Buffer outBuffer;
    // 调用 lock 方法，取出一份空闲的图形缓存区并赋给 outBuffer
    status_t err = surface->lock(&outBuffer, dirtyRectPtr);
    // 根据 outBuffer 创建 SkImageInfo
    SkImageInfo info = SkImageInfo::Make(outBuffer.width, outBuffer.height, convertPixelFormat(outBuffer.format),
                                         outBuffer.format == PIXEL_FORMAT_RGBX_8888 ? kOpaque_SkAlphaType : kPremul_SkAlphaType,
                                         GraphicsJNI::defaultColorSpace());
    // 设置 bitmap
    SkBitmap bitmap;
    ssize_t bpr = outBuffer.stride * bytesPerPixel(outBuffer.format);
    bitmap.setInfo(info, bpr);
    if (outBuffer.width > 0 && outBuffer.height > 0) {
        bitmap.setPixels(outBuffer.bits);
    } else {
        // be safe with an empty bitmap.
        bitmap.setPixels(NULL);
    }
    // 将 native bitmap 设置给 native Canvas
    Canvas* nativeCanvas = GraphicsJNI::getNativeCanvas(env, canvasObj);
    nativeCanvas->setBitmap(bitmap);
    if (dirtyRectPtr) {
        nativeCanvas->clipRect(dirtyRect.left, dirtyRect.top, dirtyRect.right, dirtyRect.bottom, SkClipOp::kIntersect);
    }
    // 返回
    sp<Surface> lockedSurface(surface);
    lockedSurface->incStrong(&sRefBaseOwner);
    return (jlong) lockedSurface.get();
}
```

上面 nativeLockCanvas 方法根据脏区创建了一个 Rect 对象，然后调用 surface->lock:
/frameworks/native/libs/gui/Surface.cpp
```
status_t Surface::lock(ANativeWindow_Buffer* outBuffer, ARect* inOutDirtyBounds) {
    ANativeWindowBuffer* out;
    status_t err = dequeueBuffer(&out, &fenceFd); // 取出一个图形缓存区 GraphicBuffer 赋给 out
    sp<GraphicBuffer> backBuffer(GraphicBuffer::getSelf(out));
    // 锁定 GraphicBuffer
    status_t res = backBuffer->lockAsync(GRALLOC_USAGE_SW_READ_OFTEN | GRALLOC_USAGE_SW_WRITE_OFTEN, newDirtyRegion.bounds(), &vaddr, fenceFd);

    if (res != 0) {
        err = INVALID_OPERATION;
    } else {
        mLockedBuffer = backBuffer;
        outBuffer->width  = backBuffer->width;
        outBuffer->height = backBuffer->height;
        outBuffer->stride = backBuffer->stride;
        outBuffer->format = backBuffer->format;
        outBuffer->bits   = vaddr;
    }
    return err;
}

/frameworks/native/libs/gui/Surface.cpp
int Surface::dequeueBuffer(android_native_buffer_t** buffer, int* fenceFd) {
    // ...
    status_t result = mGraphicBufferProducer->dequeueBuffer(&buf, &fence, reqWidth, reqHeight,
        reqFormat, reqUsage, &mBufferAge, enableFrameTimestamps ? &frameTimestamps : nullptr);
    // ...
    *buffer = gbuf.get();
    // ...
}

```
mGraphicBufferProducer 是 Layer 中的 BufferQueueProducer -- graph buffer 的生产者，
  在绘制时通过 BufferQueueProducer 生产者从 BufferQueue 中取出一个 GraphicBuffer 缓存区用来绘制。

小结: Surface.lockCanvas 方法的作用是通过 BufferQueueProducer 生产者从 BufferQueue 队列中取出一个图形缓存区
GraphicBuffer(用来创建 Canvas 中的 Bitmap 对象) 并锁定该 Surface，然后将 Surface 的地址返回给 Java 层 Surface 中的 mLockedObject 属性


View.draw
见 Android-View绘制原理, 以 drawLines 为例
/frameworks/base/graphics/java/android/graphics/BaseCanvas.java
```
// BaseCanvas
public void drawLines(@Size(multiple = 4) @NonNull float[] pts, int offset, int count, @NonNull Paint paint) {
    nDrawLines(mNativeCanvasWrapper, pts, offset, count, paint.getNativeInstance());
}

/frameworks/base/core/jni/android_graphics_Canvas.cpp
static void drawPoint(JNIEnv*, jobject, jlong canvasHandle, jfloat x, jfloat y,
                        jlong paintHandle) {
      const Paint* paint = reinterpret_cast<Paint*>(paintHandle);
      get_canvas(canvasHandle)->drawPoint(x, y, *paint);
  }
 
//todo 怎么到的skia_canvas canvas的初始化
/frameworks/base/libs/hwui/SkiaCanvas.cpp
// {"nDrawLines", "(J[FIIJ)V", (void*) CanvasJNI::drawLines}
// nDrawLines 方法对应 SkiaCanvas::drawLines
void SkiaCanvas::drawLines(const float* points, int count, const SkPaint& paint) {
    this->drawPoints(points, count, paint, SkCanvas::kLines_PointMode);
}

void SkiaCanvas::drawPoints(const float* points, int count, const SkPaint& paint, SkCanvas::PointMode mode) {
    // ...
    // mCanvas 是 SkCanvas 类型指针
    mCanvas->drawPoints(mode, count, pts.get(), paint);
}

/external/skia/src/core/SkCanvas.cpp
void SkCanvas::drawPoints(PointMode mode, size_t count, const SkPoint pts[], const SkPaint& paint) {
    this->onDrawPoints(mode, count, pts, paint);
}


/external/skia/src/core/SkCanvas.cpp
void SkCanvas::onDrawPoints(PointMode mode, size_t count, const SkPoint pts[],
                            const SkPaint& paint) {
    // ...
    while (iter.next()) {
        iter.fDevice->drawPoints(mode, count, pts, looper.paint());
    }
}

```


在 Android 新版本中，Google 开始逐渐让 Skia 接手 OpenGL，实现间接统一调用，
因此无论是软件绘制还是硬件加速采用的都是 native 层 SkiaCanvas 对象，然后通过 fDevice->drawPoints 来真正实现绘制(或构建)
/external/skia/src/core/SkBitmapDevice.cpp
```
// 这里应该是软件绘制的 Device 类...吧，大概就是这么个意思   //todo ???
void SkBitmapDevice::drawPoints(SkCanvas::PointMode mode, size_t count, const SkPoint pts[], const SkPaint& paint) {
    // 绘制在 Bitmap 中
    BDDraw(this).drawPoints(mode, count, pts, paint, nullptr);
}
```

Surface.unlockCanvasAndPost
```
/frameworks/base/core/java/android/view/Surface.java
public void unlockCanvasAndPost(Canvas canvas) {
    synchronized (mLock) {
        checkNotReleasedLocked();
        if (mHwuiContext != null) { // 硬件绘制走这里
            mHwuiContext.unlockAndPost(canvas);
        } else { // 软件绘制
            unlockSwCanvasAndPost(canvas);
        }
    }
}

private void unlockSwCanvasAndPost(Canvas canvas) {
    // ...
    try {
        nativeUnlockCanvasAndPost(mLockedObject, canvas);
    } finally {
        nativeRelease(mLockedObject);
        mLockedObject = 0;
    }
}

```
这里通过之前锁定的 Surface 的地址调用了 native 层的 nativeUnlockCanvasAndPost 方法：
```
/frameworks/base/core/jni/android_view_Surface.cpp
static void nativeUnlockCanvasAndPost(JNIEnv* env, jclass clazz, jlong nativeObject, jobject canvasObj) {
    sp<Surface> surface(reinterpret_cast<Surface *>(nativeObject));

    // detach the canvas from the surface
    Canvas* nativeCanvas = GraphicsJNI::getNativeCanvas(env, canvasObj);
    nativeCanvas->setBitmap(SkBitmap());

    // unlock surface
    status_t err = surface->unlockAndPost();
}

/frameworks/native/libs/gui/Surface.cpp
status_t Surface::unlockAndPost() {
    int fd = -1;
    status_t err = mLockedBuffer->unlockAsync(&fd);
    err = queueBuffer(mLockedBuffer.get(), fd);
    mPostedBuffer = mLockedBuffer;
    mLockedBuffer = 0;
    return err;
}

```

mLockedBuffer 就是前面的 backBuffer(在后面的双缓冲会专门讲到), mLockedBuffer->unlockAsync 
方法用来解除 GraphicBuffer 的锁定状态，然后看一下 queueBuffer 方法：
/frameworks/native/libs/gui/Surface.cpp
```
int Surface::queueBuffer(android_native_buffer_t* buffer, int fenceFd) {
    // ...
    int i = getSlotFromBufferLocked(buffer);
    // ...
    IGraphicBufferProducer::QueueBufferOutput output;
    IGraphicBufferProducer::QueueBufferInput input(timestamp, isAutoTimestamp,
            static_cast<android_dataspace>(mDataSpace), crop, mScalingMode,
            mTransform ^ mStickyTransform, fence, mStickyTransform, mEnableFrameTimestamps);
    // ...
    status_t err = mGraphicBufferProducer->queueBuffer(i, input, &output);
    // ...
    return err;
}

```

上面 mGraphicBufferProducer->queueBuffer 的具体代码就不看了，其逻辑是通过 mGraphicBufferProducer 生产者
将填充了绘制数据的图形缓存区入 BufferQueue 队列，在 queueBuffer 调用后，会调用到 SF.signalLayerUpdate 方法
```
/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp
void SurfaceFlinger::signalLayerUpdate() {
    mEventQueue->invalidate();
}

/frameworks/native/services/surfaceflinger/MessageQueue.cpp
void MessageQueue::invalidate() {
    mEvents->requestNextVsync();
}

```
到这里就回到了 Android-SurfaceFlinger启动与绘图原理 的流程，它用来请求下一次 Vsync 信号


小结
软件绘制可以简单分成以下三个步骤：

Surface.lockCanvas 方法通过 BufferQueueProducer.dequeueBuffer 函数从 BufferQueue 中取出一个图形缓存区
    GraphicBuffer(用来创建 Canvas 中的 Bitmap 对象) 并锁定该 Surface，然后将 Surface 的地址返回给 Java 层 Surface 中的 mLockedObject 属性。
   在这个方法中还会涉及到 Surface 的双缓冲逻辑，后面会具体讲解。
调用 View.draw 方法将内容绘制到 Canvas 对应的 Bitmap 中，其实就是往上面的图形缓存区 GraphicBuffer 填充绘制数据。
Surface.unlockCanvasAndPost 方法通过调用被锁定的 surface->unlockAndPost 方法解锁 Surface 且通过 queueBuffer 函数
   将填充了数据的图形缓存区 GraphicBuffer 存入 BufferQueue 队列中，然后通知给 SurfaceFlinger 进行合成(请求 Vsync 信号)。


BufferQueueProducer 中的两个重要函数：
dequeueBuffer: BufferQueueProducer 生产者通过 BufferQueue 请求一块空闲的缓存区(GraphicBuffer)
queueBuffer: BufferQueueProducer 生产者将填充了数据的缓存区(GraphicBuffer)入 BufferQueue 队列

与 BufferQueue 以及生产者/消费者相关的几个重要方法在后续文章里会讲到，有上面的生产者就肯定有消费者的工作，
   大概猜一下可以知道消费者应该就是在 SurfaceFlinger 进程中通过 BufferQueueConsumer 去从 BufferQueue 中取出 GraphicBuffer 中的数据进行合成的，
   具体逻辑有兴趣的话在参考了 Android-SurfaceFlinger启动与工作流程 后可以自行阅读源码。
对于软件绘制中的 Canvas 而言其绘制目标是一个 Bitmap 对象，绘制的内容会填充到 Surface 持有的缓存区(GraphicBuffer)里。



硬件绘制
参考: https://link.juejin.cn/?target=https%3A%2F%2Fwww.jianshu.com%2Fp%2F40f660e17a73
从 Android-View绘制流程 知道硬件绘制从 ThreadedRenderer.draw 方法开始

ThreadedRenderer.draw
/frameworks/base/core/java/android/view/ThreadedRenderer.java
```
void draw(View view, AttachInfo attachInfo, DrawCallbacks callbacks, FrameDrawingCallback frameDrawingCallback) {
    final Choreographer choreographer = attachInfo.mViewRootImpl.mChoreographer;
    choreographer.mFrameInfo.markDrawStart();
    // 构建 View 的 DrawOp 树
    updateRootDisplayList(view, callbacks);
    // 通知 RenderThread 线程进行绘制
    int syncResult = nSyncAndDrawFrame(mNativeProxy, frameInfo, frameInfo.length);
    // ...
}
```

可以将硬件绘制分为两个阶段：构建阶段 和 渲染阶段


构建阶段
/frameworks/base/core/java/android/view/ThreadedRenderer.java
```
private RenderNode mRootNode;

private void updateRootDisplayList(View view, DrawCallbacks callbacks) {
    // ...
    // 通过 RenderNode 获取 DisplayListCanvas
    DisplayListCanvas canvas = mRootNode.start(mSurfaceWidth, mSurfaceHeight);
    try {
        // ...  
        // 利用 DisplayListCanvas 构建并缓存所有的 DrawOp
        canvas.drawRenderNode(view.updateDisplayListIfDirty());
        // ...
    } finally {
        // 将 View 构建的 DrawOp 存入 RenderNode 中，完成构建
        mRootNode.end(canvas);
    }
}

// /frameworks/base/core/java/android/view/View.java
public RenderNode updateDisplayListIfDirty() {
    final RenderNode renderNode = mRenderNode;
    if ((mPrivateFlags & PFLAG_DRAWING_CACHE_VALID) == 0 || !renderNode.isValid() || (mRecreateDisplayList)) {
        // ...
        final DisplayListCanvas canvas = renderNode.start(width, height);
        try {
            if (layerType == LAYER_TYPE_SOFTWARE) {// 强制使用软件绘制
                buildDrawingCache(true);
                Bitmap cache = getDrawingCache(true);
                if (cache != null) {
                    canvas.drawBitmap(cache, 0, 0, mLayerPaint);
                }
            } else {
                // 如果自身不用绘制则直接递归子View
                if ((mPrivateFlags & PFLAG_SKIP_DRAW) == PFLAG_SKIP_DRAW) {
                    dispatchDraw(canvas);
                } else {// 调用 draw 方法，如果是 ViewGroup 会递归子View
                    draw(canvas);
                }
            }
        } finally {
            // 缓存构建Op
            renderNode.end(canvas);
        }
    }
    return renderNode;
}
```
todo  drawRenderNode做了什么

这里我们创建的 Canvas 是 DisplayListCanvas 类型实例，在调用 View.draw 方法后，使用 DisplayListCanvas 来绘图，
以 drawLines 为例：
/frameworks/base/core/java/android/view/DisplayListCanvas.java
```
// DisplayListCanvas
public final void drawLines(@Size(multiple = 4) @NonNull float[] pts, int offset, int count, @NonNull Paint paint) {
    nDrawLines(mNativeCanvasWrapper, pts, offset, count, paint.getNativeInstance());
}

//todo ??
// 由上面软件绘制 Canvas.draw 可知实现可能是在这里...
external/skqp/src/gpu/SkGpuDevice.cpp
void SkGpuDevice::drawPoints(SkCanvas::PointMode mode, size_t count, const SkPoint pts[], const SkPaint& paint) {
    // ...
    // 从 fRenderTargetContext 中的源码可以看出来这里面会构建 DrawOp   //todo drawop??
    fRenderTargetContext->drawPath(this->clip(), std::move(grPaint), GrAA(paint.isAntiAlias()), this->ctm(), path, style);
}

```

上面在 View.updateDisplayListIfDirty 方法中会遍历所有子 View 并通过 DisplayListCanvas 构建出一个 DrawOp 树，
在递归完成 DrawOp 的构建后，会调用 RenderNode.end 方法
/frameworks/base/core/java/android/view/RenderNode.java
```
public void end(DisplayListCanvas canvas) {
    long displayList = canvas.finishRecording();
    // 将 displayList 缓存到 native 层的 RenderNode 中
    nSetDisplayList(mNativeRenderNode, displayList);
    canvas.recycle();
}
```

RenderNode.end 方法用来将 displayList 缓存到 native 层的 RenderNode 中。在 updateDisplayListIfDirty 方法遍历了
子 View 并将缓存了 displayList 的 RenderNode 返回后，ThreadedRenderer 通过 DisplayListCanvas.drawRenderNode 方法
将之前返回的 RenderNode 合入 ThreadedRenderer 内部的 RenderNode 中，然后也通过 RenderNode.end 方法
将 displayList 缓存到 native 层的 RenderNode 里
//todo native 层的 RenderNode

渲染阶段
申请内存
软件绘制申请内存是通过 Surface.lockCanvas 方法借由 BufferQueueProducer 取出一个图形缓存区 GraphicBuffer。
至于硬件加速的内存是怎么申请的可以看看这部分代码(performTraversals 方法应该很熟悉了)：
/frameworks/base/core/java/android/view/ViewRootImpl.java
```
private void performTraversals() {
    // ...
    if (mAttachInfo.mThreadedRenderer != null) {
        try {
            hwInitialized = mAttachInfo.mThreadedRenderer.initialize(mSurface);
            if (hwInitialized && (host.mPrivateFlags & View.PFLAG_REQUEST_TRANSPARENT_REGIONS) == 0) {
                mSurface.allocateBuffers();
            }
        } catch (OutOfResourcesException e) {
            handleOutOfResourcesException(e);
            return;
        }
    }
    // ...
}

// /frameworks/base/core/java/android/view/Surface.java
public void allocateBuffers() {
    synchronized (mLock) {
        checkNotReleasedLocked();
        nativeAllocateBuffers(mNativeObject);
    }
}

// frameworks/base/core/jni/android_view_Surface.cpp
static void nativeAllocateBuffers(JNIEnv* /* env */ , jclass /* clazz */, jlong nativeObject) {
    sp<Surface> surface(reinterpret_cast<Surface *>(nativeObject));
    surface->allocateBuffers();
}

/frameworks/native/libs/gui/Surface.cpp
void Surface::allocateBuffers() {
    // 依旧是调用 BufferQueueProducer 生产者分配 buffer
    mGraphicBufferProducer->allocateBuffers(reqWidth, reqHeight, mReqFormat, mReqUsage);
}

```
虽然硬件加速申请内存调用的方法不一样，但看上去也是借助 Layer 中的 BufferQueueProducer 生产者从 BufferQueue 中出队列了一块空闲缓存区。
可以看到硬件加速请求 SurfaceFlinger 内存分配的时机会比软件绘制更前，硬件加速这么设计可以预先分配内存，避免在渲染的时候再申请，
防止分配内存失败时浪费了 CPU 之前的构建等工作，另外也可以将渲染线程的工作简化

渲染线程绑定Surface
接着看一下 Render 线程是怎么跟目标 Surface 绘图界面绑定的(因为同一时刻可能有多个 Surface 绘图界面，它需要绑定一个渲染的上下文)，
从上面看到申请内存前调用了 ThreadedRenderer.initialize 方法：
```
//  /frameworks/base/core/java/android/view/ThreadedRenderer.java
boolean initialize(Surface surface) throws OutOfResourcesException {
    boolean status = !mInitialized;
    mInitialized = true;
    updateEnabledState(surface);
    nInitialize(mNativeProxy, surface);
    return status;
}

// frameworks/base/core/jni/android_view_ThreadedRenderer.cpp
static void android_view_ThreadedRenderer_initialize(JNIEnv* env, jobject clazz, jlong proxyPtr, jobject jsurface) {
    RenderProxy* proxy = reinterpret_cast<RenderProxy*>(proxyPtr);
    sp<Surface> surface = android_view_Surface_getSurface(env, jsurface);
    proxy->initialize(surface);
}

/frameworks/base/libs/hwui/renderthread/RenderProxy.cpp
void RenderProxy::initialize(const sp<Surface>& surface) {
    // 向 Render 线程发送消息，执行 CanvasContext->setSurface 方法
    mRenderThread.queue().post([ this, surf = surface ]() mutable { mContext->setSurface(std::move(surf)); });
}
```

上面初始化时 CanvasContext 上下文通过 setSurface 方法将当前要渲染的 Surface 绑定到了 Render 线程中。

渲染
当渲染线程绑定了 Surface，且 Surface 内存分配以及 DrawOp 树构建完成后，便可以看一下渲染流程，
从上面的 nSyncAndDrawFrame 方法开始，其实现在 Native 层
```
// frameworks/base/core/jni/android_view_ThreadedRenderer.cpp
static int android_view_ThreadedRenderer_syncAndDrawFrame(JNIEnv* env, jobject clazz,
        jlong proxyPtr, jlongArray frameInfo, jint frameInfoSize) {
    RenderProxy* proxy = reinterpret_cast<RenderProxy*>(proxyPtr);
    env->GetLongArrayRegion(frameInfo, 0, frameInfoSize, proxy->frameInfo());
    return proxy->syncAndDrawFrame();
}

/frameworks/base/libs/hwui/renderthread/RenderProxy.cpp
int RenderProxy::syncAndDrawFrame() {
    return mDrawFrameTask.drawFrame();
}

/frameworks/base/libs/hwui/renderthread/DrawFrameTask.cpp
int DrawFrameTask::drawFrame() {
    postAndWait();
    return mSyncResult;
}

void DrawFrameTask::postAndWait() {
    AutoMutex _lock(mLock);
    mRenderThread->queue().post([this]() { run(); });
    mSignal.wait(mLock);
}

void DrawFrameTask::run() {
    // ...
    if (CC_LIKELY(canDrawThisFrame)) { // CanvasContext
        context->draw();
    } else {
        // wait on fences so tasks don't overlap next frame
        context->waitOnFences();
    }
}
```
接下来所有的 DrawOp 都会通过 OpenGL 被绘制到 GraphicBuffer 中，然后通知 SurfaceFlinger 进行合成，具体源码不贴了，因为看不大懂
//todo ??
/frameworks/base/libs/hwui/renderthread/CanvasContext.cpp
```
void CanvasContext::draw() {
    SkRect dirty;
    mDamageAccumulator.finish(&dirty);
    Frame frame = mRenderPipeline->getFrame();
    // 计算脏区
    SkRect windowDirty = computeDirtyRect(frame, &dirty);
    // 渲染
    bool drew = mRenderPipeline->draw(frame, windowDirty, dirty, mLightGeometry, &mLayerUpdateQueue,
                                      mContentDrawBounds, mOpaque, mWideColorGamut, mLightInfo,
                                      mRenderNodes, &(profiler()));
    // 交换缓存区
    bool didSwap = mRenderPipeline->swapBuffers(frame, drew, windowDirty, mCurrentFrameInfo, &requireSwap);
    // ...
}
```
可以看到最终的绘制操作是在 CanvasContext 中交由渲染管线去执行的, 这里主要有两个步骤
1 通过 mRenderPipeline->draw, 将 RenderNode 中的 DisplayList 记录的数据绘制到 Surface 的缓冲区
2 通过 mRenderPipeline->swapBuffers 将缓冲区的数据推送到 Surface 的缓冲区中, 等待 SurfaceFlinger 的合成操作
todo surface缓冲区？？mGraphicBufferProducer
todo draw的操作，离屏缓冲https://juejin.cn/post/7063810632289615885#heading-4

/frameworks/base/libs/hwui/renderthread/OpenGLPipeline.cpp
```
  bool OpenGLPipeline::swapBuffers(const Frame& frame, bool drew, const SkRect& screenDirty,
                                   FrameInfo* currentFrameInfo, bool* requireSwap) {
       ...
      // Even if we decided to cancel the frame, from the perspective of jank
      // metrics the frame was swapped at this point
      currentFrameInfo->markSwapBuffers();
      *requireSwap = drew || mEglManager.damageRequiresSwap();
      if (*requireSwap && (CC_UNLIKELY(!mEglManager.swapBuffers(frame, screenDirty)))) {
          return false;
      }
      return *requireSwap;
  }
```
mEglManager.swapBuffers
/frameworks/base/libs/hwui/renderthread/EglManager.cpp
```
 bool EglManager::swapBuffers(const Frame& frame, const SkRect& screenDirty) {
      ...
      EGLint rects[4];
      frame.map(screenDirty, rects);
      eglSwapBuffersWithDamageKHR(mEglDisplay, frame.mSurface, rects, screenDirty.isEmpty() ? 0 : 1);
      ....
  }
```
/frameworks/native/opengl/libs/EGL/eglApi.cpp
```
 EGLBoolean eglSwapBuffersWithDamageKHR(EGLDisplay dpy, EGLSurface draw,
          EGLint *rects, EGLint n_rects)
  {
       ... sync有关
      if (CC_UNLIKELY(dp->traceGpuCompletion)) {
          EGLSyncKHR sync = eglCreateSyncKHR(dpy, EGL_SYNC_FENCE_KHR, NULL);
          if (sync != EGL_NO_SYNC_KHR) {
              FrameCompletionThread::queueSync(sync);
          }
      }
  
      if (CC_UNLIKELY(dp->finishOnSwap)) {
          uint32_t pixel;
          egl_context_t * const c = get_context( egl_tls_t::getContext() );
          if (c) {
              // glReadPixels() ensures that the frame is complete
              s->cnx->hooks[c->version]->gl.glReadPixels(0,0,1,1,
                      GL_RGBA,GL_UNSIGNED_BYTE,&pixel);
          }
      }
  
      if (!sendSurfaceMetadata(s)) {
          native_window_api_disconnect(s->getNativeWindow(), NATIVE_WINDOW_API_EGL);
          return setError(EGL_BAD_NATIVE_WINDOW, (EGLBoolean)EGL_FALSE);
      }
  
      if (n_rects == 0) {
          return s->cnx->egl.eglSwapBuffers(dp->disp.dpy, s->surface);
      }
    ...
  }
```
/frameworks/native/opengl/libagl/egl.cpp
```
  EGLBoolean eglSwapBuffers(EGLDisplay dpy, EGLSurface draw)
  {
      ...
      // post the surface
      d->swapBuffers();
      return EGL_TRUE;
  }
```
后面可以查看glsurfaceview的有关eglSwapBuffers操作


小结
硬件加速可以从两个阶段来看：
构建阶段：将 View 抽象成 RenderNode 节点，其每个绘制操作(drawLine...)都会抽象成 DrawOp 操作，
   它存在对应的 OpenGL 绘制命令并保存了绘图需要的数据。这个阶段会递归遍历所有 View 并通过 Canvas.drawXXX 
   将绘制操作转化成 DrawOp 存入 DisplayList 中，根据 ViewTree 模型，这个 DisplayList 虽然命名为 List，
   但其实更像一棵树。
绘制阶段：通过单独的 Render 线程，依赖 GPU 绘制上面的 DrawOp 数据。

其中硬件加速的内存申请跟软件绘制一样都是借助 Layer 中的 BufferQueueProducer 生产者从 BufferQueue 中
  出队列一块空闲缓存区 GraphicBuffer 用来渲染数据的，之后也都会通知 SurfaceFlinger 进行合成。
  不一样的地方在于硬件加速相比软件绘制而言算法可能更加合理，同时采用了一个单独的 Render 线程，减轻了主线程的负担



总结

Java 层的 Surface 对象中 mNativeObject 属性指向 native 层中创建的 Surface 对象。
Surface 对应 SurfaceFlinger 中的 Layer 对象，它持有 Layer 中的 BufferQueueProducer 指针(生产者)，
   通过这个生产者对象可以在绘制时向 BufferQueue 申请一块空闲的图形缓存区 GraphicBuffer，在 Surface 上绘制的内容会存入该缓存区内。
SurfaceFlinger 通过 BufferQueueConsumer 消费者从 BufferQueue 中取出 GraphicBuffer 中的数据进行合成渲染并送到显示器显示。


软件绘制
软件绘制可能会绘制到不需要重绘的视图，且其绘制过程在主线程进行的，可能会造成卡顿等情况。它把要绘制的内容写进一个 Bitmap 位图，
   其实就是填充到了 Surface 申请的图形缓存区里。
软件绘制可分为三个步骤：
Surface.lockCanvas -- dequeueBuffer 从 BufferQueue 中出队列一块缓存区。
View.draw -- 绘制内容。
Surface.unlockCanvasAndPost -- queueBuffer 将填充了数据的缓存区存入 BufferQueue 队列中，
  然后通知给 SurfaceFlinger 进行合成(请求 Vsync 信号)。


硬件绘制
硬件绘制会将绘制函数作为绘制指令(DrawOp)记录在一个列表(DisplayList)中，然后交给单独的 Render 线程使用 GPU 进行硬件加速渲染。
  它只需要针对需要更新的 View 对象的脏区进行记录或更新，无需更新的 View 对象则能重用先前 DisplayList 中记录的指令。
硬件绘制可分为两个阶段：
构建阶段：将 View 的绘制操作(drawLine...)抽象成 DrawOp 操作并存入 DisplayList 中。
绘制阶段：首先分配缓存区(同软件绘制)，然后将 Surface 绑定到 Render 线程，最后通过 GPU 渲染 DrawOp 数据。

硬件加速的内存申请跟软件绘制一样都是借助 Layer 中的 BufferQueueProducer 生产者从 BufferQueue 中
  出队列一块空闲缓存区 GraphicBuffer 用来渲染数据的，之后也都会通知 SurfaceFlinger 进行合成。
  不一样的地方在于硬件加速相比软件绘制而言算法可能更加合理，同时采用了一个单独的 Render 线程，减轻了主线程的负担。




