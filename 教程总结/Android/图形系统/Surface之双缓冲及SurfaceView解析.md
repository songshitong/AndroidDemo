https://juejin.cn/post/6897029276752625671   android 9.0

这篇文章再看看在 View 绘制过程中所用到的双缓冲技术，双缓冲的使用范围非常广泛，比如说在屏幕图像显示的时候就应用到了双缓冲 
-- 分为屏幕前缓冲区和屏幕后缓冲区，此外还有三缓冲的概念...这篇文章主要看看 View 在绘制的过程中是怎么使用双缓冲的。

双缓冲(View绘制过程)
一般来说将双缓冲用到的两块缓冲区称为 -- 前缓冲区(front buffer) 和 后缓冲区(back buffer)。显示器显示的数据来源于 front buffer 前缓存区，
而每一帧的数据都绘制到 back buffer 后缓存区，在 Vsync 信号到来后会交互缓存区的数据(指针指向)，这时 front buffer 和 back buffer 的
  称呼及功能倒转。

软件绘制中的双缓冲
通过之前 Android-Surface之创建流程及软硬件绘制 的解析知道软件绘制可分为三个步骤：
Surface.lockCanvas: 会调用到 Native 层的 Surface.lock 方法
View.draw: 将绘制数据写入缓存区
Surface.unlockCanvasAndPost: 会调用到 Native 层 Surface.unlockAndPost 方法

双缓冲的解析可以从 Native 层的 Surface.lock 方法开始看起：  //可以看到三个缓存区都是Graphic Buffer
/frameworks/native/libs/gui/Surface.cpp
```
status_t Surface::lock(ANativeWindow_Buffer* outBuffer, ARect* inOutDirtyBounds) {
    ANativeWindowBuffer* out;
    // 通过生产者从 QueueBuffer 队列中取出一块空闲的图形缓存区--GraphicBuffer
    status_t err = dequeueBuffer(&out, &fenceFd);
    // 将 GraphicBuffer 赋值给后缓存区 backBuffer
    sp<GraphicBuffer> backBuffer(GraphicBuffer::getSelf(out));
    const Rect bounds(backBuffer->width, backBuffer->height);
    // 计算新的脏区--1处图
    Region newDirtyRegion;
    if (inOutDirtyBounds) {
        // App 通过调用 lockCanvas(Rect inOutDirty) 传递了一个脏区
        // 则将传入的 inOutDirty 作为新的脏区
        newDirtyRegion.set(static_cast<Rect const&>(*inOutDirtyBounds));
        newDirtyRegion.andSelf(bounds);
    } else {// 否则将后缓存区大小作为脏区
        newDirtyRegion.set(bounds);
    }
    // 将正在显示的 mPostedBuffer 缓存区赋值给 frontBuffer 前缓存区
    const sp<GraphicBuffer>& frontBuffer(mPostedBuffer);
    // 是否需要将前缓存区拷贝到后缓存区
    // 前缓存区有内容 && 前后缓存区的长宽及格式一样
    // 第一次绘制时 frontBuffer 是没内容的
    const bool canCopyBack = (frontBuffer != 0 &&
            backBuffer->width  == frontBuffer->width &&
            backBuffer->height == frontBuffer->height &&
            backBuffer->format == frontBuffer->format);

    if (canCopyBack) {
        // 可以拷贝时--2处图
        // copy the area that is invalid and not repainted this round
        const Region copyback(mDirtyRegion.subtract(newDirtyRegion));
        if (!copyback.isEmpty()) {
            copyBlt(backBuffer, frontBuffer, copyback, &fenceFd);
        }
    } else {
        // 不能拷贝时，后缓存区直接取新脏区的区域，确保重绘整个区域
        newDirtyRegion.set(bounds);
        mDirtyRegion.clear();
        Mutex::Autolock lock(mMutex);
        for (size_t i=0 ; i<NUM_BUFFER_SLOTS ; i++) {
            mSlots[i].dirtyRegion.clear();
        }
    }
    // 锁定 backBuffer
    status_t res = backBuffer->lockAsync(GRALLOC_USAGE_SW_READ_OFTEN | GRALLOC_USAGE_SW_WRITE_OFTEN,
                newDirtyRegion.bounds(), &vaddr, fenceFd);
    if (res != 0) {
        err = INVALID_OPERATION;
    } else {
        // 将后缓存区赋值给 mLockedBuffer
        mLockedBuffer = backBuffer;
        outBuffer->width  = backBuffer->width;
        outBuffer->height = backBuffer->height;
        outBuffer->stride = backBuffer->stride;
        outBuffer->format = backBuffer->format;
        outBuffer->bits   = vaddr;
    }
    return err;
}
```
上图的注释都比较清晰了，看一下上面标数字的 1 和 2 处逻辑：
android_surface_双缓冲_流程.awebp

在拷贝后，后缓存区浅绿色的部分就是要重绘的区域，而绿色区域是之前前缓存区显示的内容，与脏区相交的部分要重绘，未相交的区域则不需要重绘，
下次显示时接着使用该区域即可。

接下来看看 Surface.unlockAndPost 方法，其中 mLockedBuffer 在 lock 方法中被赋值为 backBuffer：
```
status_t Surface::unlockAndPost() {
    int fd = -1;
    // 解锁 mLockedBuffer
    status_t err = mLockedBuffer->unlockAsync(&fd);
    // 将绘制后的缓存区入队列，等待被合成显示
    err = queueBuffer(mLockedBuffer.get(), fd);
    // 赋值给 mPostedBuffer，代表要被显示的数据
    mPostedBuffer = mLockedBuffer;
    mLockedBuffer = 0;
    return err;
}
```
小结
Surface.lock:
1 将出队列的空闲缓存区 GraphicBuffer 赋给后缓存区 backBuffer，将正在显示的 mPostedBuffer 赋给前缓存区。
2 计算新的脏区，并确定是否需要将前缓存区拷贝到后缓存区，依此计算出后缓存区 backBuffer 的最终数据。然后将 backBuffer 与应用层的 Canvas 关联，
  当操作 Canvas 绘图时会将数据绘制到 backBuffer 上。
3 锁定 backBuffer 且将 backBuffer 指针赋值给 mLockedBuffer。

Surface.unlockAndPost:
1 将存有绘制数据的 mLockedBuffer 解锁并将其赋值给 mPostedBuffer。
2 将 mLockedBuffer 入 BufferQueue 队列，等待被合成显示，在这里便相当于交换了前后缓冲区的指针，等到下次绘制时，接着重复上面的步骤。



硬件绘制中的双缓冲
由之前的解析可以知道硬件绘制最终会调用 CanvasContext.draw 方法来绘制：
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
通过 mRenderPipeline->draw, 将 RenderNode 中的 DisplayList 记录的数据绘制到 Surface 的缓冲区
通过 mRenderPipeline->swapBuffers 将缓冲区的数据推送到 Surface 的缓冲区中, 等待 SurfaceFlinger 的合成操作

具体的代码比较复杂(看不太懂了)，通过相关的调用大概可以看出硬件绘制时应该也是存在双缓冲的，有兴趣的话可以再深入看看，有大佬知道的欢迎指点
//todo



SurfaceView
概述
SurfaceView 是一种较之 TextView, Button 等更为特殊的 View, 它不与其宿主的 Window 共享一个 Surface, 
而是有自己的独立 Surface。并且它可以在一个独立的线程中绘制UI(//执行canvas的draw方法)。因此 SurfaceView 一般用来实现比较复杂的图像或动画/视频的显示。


这里插入一个内容，关于 View 为啥不能在子线程中操作 UI 的话可以看看 ViewRootImpl.checkThread 这个方法，
参考 ViewRootImpl.checkThread 方法 其实就是因为每次操作 UI 都会去 check 线程，当然前提是 ViewRootImpl 已经被实例化了~
  1 SurfaceView 在绘图时实现了双缓冲机制(独立的 Surface)
  2 普通 View 在绘图时会绘制到 Bitmap 中，然后通过其所在 Window 的 Surface 对象实现双缓冲。

一般来说，每个 Window 都有对应的 Surface 绘制表面，它在 SurfaceFlinger 服务中对应一个 Layer。而对于存在 SurfaceView 的 Window 来说，
  它除了自己的 Surface 以外还会有另一个 SurfaceView 独有的 Surface 绘制表面，在 SurfaceFlinger 服务中也会存在着两个 Layer 
  分别对应它们。查看 SurfaceView 的源码可以看到 SurfaceView 类似于 ViewRootImpl 一样其内部都有一个单独的 Surface 实例，
  于是结合之前 Android-Surface之创建流程及软硬件绘制 的解析，就能理解刚才对 SurfaceView 的描述了

SurfaceView 官方注释有一段: The surface is Z ordered so that it is behind the window holding its SurfaceView;
   the SurfaceView punches a hole in its window to allow its surface to be displayed.
翻译一下大体意思是：Surface 是按照 Z 轴顺序排列的，SurfaceView 的 Surface 位于其宿主窗口的 Surface 后面；
   SurfaceView 在其窗口上打一个孔，以显示其 Surface。这个孔实际上是 SurfaceView 在其宿主窗口上设置了一块透明区域。

接下来看一个使用 SurfaceView 的示例：
```
class MySurfaceView(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) :
    SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback, Runnable {

    companion object {
        private const val TAG = "MySurfaceView"
    }

    private val surfaceHolder: SurfaceHolder = holder
    private var canvas: Canvas? = null

    @Volatile
    private var canDoDraw = false
    private val drawThread = Thread(this)
    private val lock = Object()

    private var xx = 0f
    private var yy = 400f
    private val path = Path()
    private val paint = Paint()

    constructor(context: Context?) : this(context, null, 0)
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)

    init {
        surfaceHolder.addCallback(this)
        isFocusable = true
        isFocusableInTouchMode = true
        keepScreenOn = true
        path.moveTo(xx, yy)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 10f
        paint.color = Color.RED
        drawThread.start()
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        Log.d(TAG, "surfaceCreated")
        setCanDraw(true)
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        Log.d(TAG, "surfaceChanged")
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        Log.d(TAG, "surfaceDestroyed")
        canDoDraw = false
        // 注意当APP到后台时会 destroy Surface, 回到前台会重新调用 surfaceCreated
        // 因此这里不能移除回调，否则会黑屏
        // surfaceHolder.removeCallback(this)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        Log.d(TAG, "onTouchEvent")
        setCanDraw(!canDoDraw)
        return super.onTouchEvent(event)
    }

    private fun setCanDraw(canDraw: Boolean) {
        if (canDraw) {
            synchronized(lock) {
                try {
                    lock.notifyAll()
                } catch (e: Exception) {
                }
            }
        }
        canDoDraw = canDraw
    }

    override fun run() {
        while (true) {
            synchronized(lock) {
                if (!canDoDraw) {
                    try {
                        lock.wait()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            draw()
            xx += 1
            yy = (100 * sin(xx * 2 * Math.PI / 180) + 400).toFloat()
            path.lineTo(xx, yy)
        }
    }

    private fun draw() {
        try {
            canvas = surfaceHolder.lockCanvas()
            canvas?.drawColor(Color.WHITE)
            canvas?.drawPath(path, paint)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            surfaceHolder.unlockCanvasAndPost(canvas ?: return)
        }
    }
}
```




源码解析
先看一下 SurfaceHolder 这个接口：
/frameworks/base/core/java/android/view/SurfaceHolder.java
```
public interface SurfaceHolder {
    public interface Callback {
        // 首次创建 Surface 后会调用此方法，在这个回调里应该开始绘制任务
        // 只有一个线程可以绘制到 Surface 中，如果渲染任务将在另一个线程中进行则不能在此处绘制 Surface
        public void surfaceCreated(SurfaceHolder holder);

        // 当 Surface 结构更改(format or size)后会调用此方法，在这个回调里应该更新 Surface 的图像
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height);

        // 在 Surface destroy 之前会调用此方法，在这个回调以后不应该尝试访问此 Surface
        public void surfaceDestroyed(SurfaceHolder holder);
    }

    public void addCallback(Callback callback);
    public void removeCallback(Callback callback);
    // 是否正在通过 Callback 方法创建 Surface
    public boolean isCreating();
    public void setType(int type); // Sets the surface's type.
    public void setFixedSize(int width, int height);
    public void setSizeFromLayout();
    public void setFormat(int format);
    // Enable or disable option to keep the screen turned on while this surface is displayed.
    public void setKeepScreenOn(boolean screenOn); // 默认 false
    public Canvas lockCanvas();
    public Canvas lockCanvas(Rect dirty);
    default Canvas lockHardwareCanvas() {
        throw new IllegalStateException("This SurfaceHolder doesn't support lockHardwareCanvas");
    }
    public void unlockCanvasAndPost(Canvas canvas);
    public Rect getSurfaceFrame();
    public Surface getSurface();
}
```


可以看出 SurfaceHolder 是用来管理 Surface 的类。接下来看下 SurfaceView 的 draw 源码：
/frameworks/base/core/java/android/view/SurfaceView.java
```
@Override
public void draw(Canvas canvas) {
    if (mDrawFinished && !isAboveParent()) {
        // draw() is not called when SKIP_DRAW is set
        if ((mPrivateFlags & PFLAG_SKIP_DRAW) == 0) {
            // punch a whole in the view-hierarchy below us
            canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        }
    }
    super.draw(canvas);
}
private boolean isAboveParent() {
          return mSubLayer >= 0;
      }

@Override
protected void dispatchDraw(Canvas canvas) {
    if (mDrawFinished && !isAboveParent()) {
        // draw() is not called when SKIP_DRAW is set
        if ((mPrivateFlags & PFLAG_SKIP_DRAW) == PFLAG_SKIP_DRAW) {
            // punch a whole in the view-hierarchy below us
            canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        }
    }
    super.dispatchDraw(canvas);
}
```
SurfaceView 方法中 draw 和 dispatchDraw 的参数 canvas 是从宿主的 Surface 中获取的，因此在该 canvas 上绘制的内容
  都会出现在宿主的 Surface 上。

所以可以看到 SurfaceView.draw 和 SurfaceView.dispatchDraw 方法的逻辑是：如果当前 SurfaceView 不是用作宿主窗口面板，
则 SurfaceView 在其宿主窗口 Surface 上的操作只是清空 Canvas 区域，因为 SurfaceView 的内容是需要展现在自己单独的 Surface 上的
  (像上面的示例一样，通过其 Surface 拿到一个 Canvas 并在一个独立线程在其上进行绘制)。
接着看一下 SurfaceView 中 SurfaceHolder 的实现：
```
public class SurfaceView extends View implements ViewRootImpl.WindowStoppedCallback {
    final ArrayList<SurfaceHolder.Callback> mCallbacks = new ArrayList<>();
    final ReentrantLock mSurfaceLock = new ReentrantLock();
    final Surface mSurface = new Surface();

    public SurfaceHolder getHolder() {
        return mSurfaceHolder;
    }

    private final SurfaceHolder mSurfaceHolder = new SurfaceHolder() {
        @Override
        public void addCallback(Callback callback) {
            synchronized (mCallbacks) { // 添加回调
                if (mCallbacks.contains(callback) == false) {
                    mCallbacks.add(callback);
                }
            }
        }

        @Override
        public void setFixedSize(int width, int height) {
            if (mRequestedWidth != width || mRequestedHeight != height) {
                mRequestedWidth = width;
                mRequestedHeight = height;
                requestLayout(); // 重新 layout
            }
        }

        @Override
        public void setKeepScreenOn(boolean screenOn) {
            runOnUiThread(() -> SurfaceView.this.setKeepScreenOn(screenOn));
        }

        @Override
        public Canvas lockCanvas() {
            return internalLockCanvas(null, false);
        }

        @Override
        public Canvas lockCanvas(Rect inOutDirty) {
            return internalLockCanvas(inOutDirty, false);
        }

        @Override
        public Canvas lockHardwareCanvas() {
            return internalLockCanvas(null, true);
        }

        // 锁定 Surface
        private Canvas internalLockCanvas(Rect dirty, boolean hardware) {
            mSurfaceLock.lock();

            Canvas c = null;
            if (!mDrawingStopped && mSurfaceControl != null) {
                try {
                    if (hardware) { // 硬件渲染
                        c = mSurface.lockHardwareCanvas();
                    } else { // 软件渲染
                        c = mSurface.lockCanvas(dirty);
                    }
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Exception locking surface", e);
                }
            }

            if (c != null) {
                mLastLockTime = SystemClock.uptimeMillis();
                return c;
            }

            // 当返回 null 时使 internalLockCanvas 被调用的间隔超过100ms
            long now = SystemClock.uptimeMillis();
            long nextTime = mLastLockTime + 100;
            if (nextTime > now) {
                try {
                    Thread.sleep(nextTime-now);
                } catch (InterruptedException e) {
                }
                now = SystemClock.uptimeMillis();
            }
            mLastLockTime = now;
            mSurfaceLock.unlock();

            return null;
        }

        @Override
        public void unlockCanvasAndPost(Canvas canvas) {
            mSurface.unlockCanvasAndPost(canvas);
            mSurfaceLock.unlock();
        }

        @Override
        public Surface getSurface() {
            return mSurface;
        }

        // ...
    }
}
```
可以看出调用 SurfaceHolder 的 lock 和 unlock 系列方法都是调用到了 Surface 中的方法。除了 lockHardwareCanvas 方法，
其他的在 Android-Surface之创建流程及软硬件绘制 中都已经看过了：
/frameworks/base/core/java/android/view/Surface.java
```
// Surface
public Canvas lockHardwareCanvas() {
    synchronized (mLock) {
        checkNotReleasedLocked();
        if (mHwuiContext == null) {
            mHwuiContext = new HwuiContext(false);
        }
        return mHwuiContext.lockCanvas(nativeGetWidth(mNativeObject), nativeGetHeight(mNativeObject));
    }
}

// HwuiContext
Canvas lockCanvas(int width, int height) {
    if (mCanvas != null) {
        throw new IllegalStateException("Surface was already locked!");
    }
    mCanvas = mRenderNode.start(width, height);
    return mCanvas;
}
```
可以看到 HwuiContext.lockCanvas 是使用硬件加速的方式，其调用的 RenderNode.start 之前已经看过了，
与之对应的 RenderNode.end 方法是在这里调用的：
```
// Surface
public void unlockCanvasAndPost(Canvas canvas) {
    synchronized (mLock) {
        checkNotReleasedLocked();

        if (mHwuiContext != null) { // 不为空时
            mHwuiContext.unlockAndPost(canvas);
        } else {
            unlockSwCanvasAndPost(canvas);
        }
    }
}

// HwuiContext
void unlockAndPost(Canvas canvas) {
    if (canvas != mCanvas) {
        throw new IllegalArgumentException("canvas object must be the same instance that "
                + "was previously returned by lockCanvas");
    }
    mRenderNode.end(mCanvas);
    mCanvas = null;
    nHwuiDraw(mHwuiRenderer);
}
```
即 SurfaceView 的绘制兼顾了软件绘制和硬件加速绘制。另外在 SurfaceView.updateSurface 方法中会更新 Surface 的状态
并将其回调给 SurfaceHolder.Callback 相关方法，具体逻辑便不给出了
//大概逻辑是监听窗口状态并更改和回调surface   例如setVisibility，onDetachedFromWindow等都会触发
```
protected void updateSurface() {
        if (!mHaveFrame) {
            return;
        }
        ViewRootImpl viewRoot = getViewRootImpl();
        if (viewRoot == null || viewRoot.mSurface == null || !viewRoot.mSurface.isValid()) {
            return;
        }

        mTranslator = viewRoot.mTranslator;
        if (mTranslator != null) {
            mSurface.setCompatibilityTranslator(mTranslator);
        }

        int myWidth = mRequestedWidth;
        if (myWidth <= 0) myWidth = getWidth();
        int myHeight = mRequestedHeight;
        if (myHeight <= 0) myHeight = getHeight();

        final boolean formatChanged = mFormat != mRequestedFormat;
        final boolean visibleChanged = mVisible != mRequestedVisible;
        final boolean creating = (mSurfaceControl == null || formatChanged || visibleChanged)
                && mRequestedVisible;
        final boolean sizeChanged = mSurfaceWidth != myWidth || mSurfaceHeight != myHeight;
        final boolean windowVisibleChanged = mWindowVisibility != mLastWindowVisibility;
        boolean redrawNeeded = false;

        if (creating || formatChanged || sizeChanged || visibleChanged || windowVisibleChanged) {
            getLocationInWindow(mLocation);

            try {
                final boolean visible = mVisible = mRequestedVisible;
                mWindowSpaceLeft = mLocation[0];
                mWindowSpaceTop = mLocation[1];
                mSurfaceWidth = myWidth;
                mSurfaceHeight = myHeight;
                mFormat = mRequestedFormat;
                mLastWindowVisibility = mWindowVisibility;

                mScreenRect.left = mWindowSpaceLeft;
                mScreenRect.top = mWindowSpaceTop;
                mScreenRect.right = mWindowSpaceLeft + getWidth();
                mScreenRect.bottom = mWindowSpaceTop + getHeight();
                if (mTranslator != null) {
                    mTranslator.translateRectInAppWindowToScreen(mScreenRect);
                }

                final Rect surfaceInsets = getParentSurfaceInsets();
                mScreenRect.offset(surfaceInsets.left, surfaceInsets.top);

                if (creating) {
                    mSurfaceSession = new SurfaceSession(viewRoot.mSurface);
                    mDeferredDestroySurfaceControl = mSurfaceControl;

                    updateOpaqueFlag();
                    final String name = "SurfaceView - " + viewRoot.getTitle().toString();

                    mSurfaceControl = new SurfaceControlWithBackground(
                            name,
                            (mSurfaceFlags & SurfaceControl.OPAQUE) != 0,
                            new SurfaceControl.Builder(mSurfaceSession)
                                    .setSize(mSurfaceWidth, mSurfaceHeight)
                                    .setFormat(mFormat)
                                    .setFlags(mSurfaceFlags));
                } else if (mSurfaceControl == null) {
                    return;
                }

                boolean realSizeChanged = false;

                mSurfaceLock.lock();
                try {
                    mDrawingStopped = !visible;
                    SurfaceControl.openTransaction();
                    try {
                        mSurfaceControl.setLayer(mSubLayer);
                        if (mViewVisibility) {
                            mSurfaceControl.show();
                        } else {
                            mSurfaceControl.hide();
                        }

                        // While creating the surface, we will set it's initial
                        // geometry. Outside of that though, we should generally
                        // leave it to the RenderThread.
                        //
                        // There is one more case when the buffer size changes we aren't yet
                        // prepared to sync (as even following the transaction applying
                        // we still need to latch a buffer).
                        // b/28866173
                        if (sizeChanged || creating || !mRtHandlingPositionUpdates) {
                            mSurfaceControl.setPosition(mScreenRect.left, mScreenRect.top);
                            mSurfaceControl.setMatrix(mScreenRect.width() / (float) mSurfaceWidth,
                                    0.0f, 0.0f,
                                    mScreenRect.height() / (float) mSurfaceHeight);
                        }
                        if (sizeChanged) {
                            mSurfaceControl.setSize(mSurfaceWidth, mSurfaceHeight);
                        }
                    } finally {
                        SurfaceControl.closeTransaction();
                    }

                    if (sizeChanged || creating) {
                        redrawNeeded = true;
                    }

                    mSurfaceFrame.left = 0;
                    mSurfaceFrame.top = 0;
                    if (mTranslator == null) {
                        mSurfaceFrame.right = mSurfaceWidth;
                        mSurfaceFrame.bottom = mSurfaceHeight;
                    } else {
                        float appInvertedScale = mTranslator.applicationInvertedScale;
                        mSurfaceFrame.right = (int) (mSurfaceWidth * appInvertedScale + 0.5f);
                        mSurfaceFrame.bottom = (int) (mSurfaceHeight * appInvertedScale + 0.5f);
                    }

                    final int surfaceWidth = mSurfaceFrame.right;
                    final int surfaceHeight = mSurfaceFrame.bottom;
                    realSizeChanged = mLastSurfaceWidth != surfaceWidth
                            || mLastSurfaceHeight != surfaceHeight;
                    mLastSurfaceWidth = surfaceWidth;
                    mLastSurfaceHeight = surfaceHeight;
                } finally {
                    mSurfaceLock.unlock();
                }

                try {
                    redrawNeeded |= visible && !mDrawFinished;

                    SurfaceHolder.Callback callbacks[] = null;

                    final boolean surfaceChanged = creating;
                    if (mSurfaceCreated && (surfaceChanged || (!visible && visibleChanged))) {
                        mSurfaceCreated = false;
                        if (mSurface.isValid()) {
                            //回调surfaceDestroyed
                            callbacks = getSurfaceCallbacks();
                            for (SurfaceHolder.Callback c : callbacks) {
                                c.surfaceDestroyed(mSurfaceHolder);
                            }
                            // Since Android N the same surface may be reused and given to us
                            // again by the system server at a later point. However
                            // as we didn't do this in previous releases, clients weren't
                            // necessarily required to clean up properly in
                            // surfaceDestroyed. This leads to problems for example when
                            // clients don't destroy their EGL context, and try
                            // and create a new one on the same surface following reuse.
                            // Since there is no valid use of the surface in-between
                            // surfaceDestroyed and surfaceCreated, we force a disconnect,
                            // so the next connect will always work if we end up reusing
                            // the surface.
                            if (mSurface.isValid()) {
                                mSurface.forceScopedDisconnect();
                            }
                        }
                    }

                    if (creating) {
                        mSurface.copyFrom(mSurfaceControl);
                    }

                    if (sizeChanged && getContext().getApplicationInfo().targetSdkVersion
                            < Build.VERSION_CODES.O) {
                        // Some legacy applications use the underlying native {@link Surface} object
                        // as a key to whether anything has changed. In these cases, updates to the
                        // existing {@link Surface} will be ignored when the size changes.
                        // Therefore, we must explicitly recreate the {@link Surface} in these
                        // cases.
                        mSurface.createFrom(mSurfaceControl);
                    }

                    if (visible && mSurface.isValid()) {
                        if (!mSurfaceCreated && (surfaceChanged || visibleChanged)) {
                            mSurfaceCreated = true;
                            mIsCreating = true;
                            //回调surfaceCreated
                            if (callbacks == null) {
                                callbacks = getSurfaceCallbacks();
                            }
                            for (SurfaceHolder.Callback c : callbacks) {
                                c.surfaceCreated(mSurfaceHolder);
                            }
                        }
                        if (creating || formatChanged || sizeChanged
                                || visibleChanged || realSizeChanged) {
                            if (callbacks == null) {
                                callbacks = getSurfaceCallbacks();
                            }
                            //回调surfaceChanged
                            for (SurfaceHolder.Callback c : callbacks) {
                                c.surfaceChanged(mSurfaceHolder, mFormat, myWidth, myHeight);
                            }
                        }
                        if (redrawNeeded) {
                            if (callbacks == null) {
                                callbacks = getSurfaceCallbacks();
                            }

                            mPendingReportDraws++;
                            viewRoot.drawPending();
                            SurfaceCallbackHelper sch =
                                    new SurfaceCallbackHelper(this::onDrawFinished);
                            //回调dispatchSurfaceRedrawNeededAsync        
                            sch.dispatchSurfaceRedrawNeededAsync(mSurfaceHolder, callbacks);
                        }
                    }
                } finally {
                    mIsCreating = false;
                    if (mSurfaceControl != null && !mSurfaceCreated) {
                        mSurface.release();

                        mSurfaceControl.destroy();
                        mSurfaceControl = null;
                    }
                }
            } catch (Exception ex) {
                Log.e(TAG, "Exception configuring surface", ex);
            }
        } else {
            // Calculate the window position in case RT loses the window
            // and we need to fallback to a UI-thread driven position update
            getLocationInSurface(mLocation);
            final boolean positionChanged = mWindowSpaceLeft != mLocation[0]
                    || mWindowSpaceTop != mLocation[1];
            final boolean layoutSizeChanged = getWidth() != mScreenRect.width()
                    || getHeight() != mScreenRect.height();
            if (positionChanged || layoutSizeChanged) { // Only the position has changed
                mWindowSpaceLeft = mLocation[0];
                mWindowSpaceTop = mLocation[1];
                // For our size changed check, we keep mScreenRect.width() and mScreenRect.height()
                // in view local space.
                mLocation[0] = getWidth();
                mLocation[1] = getHeight();

                mScreenRect.set(mWindowSpaceLeft, mWindowSpaceTop,
                        mWindowSpaceLeft + mLocation[0], mWindowSpaceTop + mLocation[1]);

                if (mTranslator != null) {
                    mTranslator.translateRectInAppWindowToScreen(mScreenRect);
                }

                if (mSurfaceControl == null) {
                    return;
                }

                if (!isHardwareAccelerated() || !mRtHandlingPositionUpdates) {
                    try {
                        setParentSpaceRectangle(mScreenRect, -1);
                    } catch (Exception ex) {
                        Log.e(TAG, "Exception configuring surface", ex);
                    }
                }
            }
        }
    }
```