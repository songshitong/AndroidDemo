
android-32\android\opengl\GLSurfaceView.java
https://sharrychoo.github.io/blog/opengl-es-2.0/egl
```
//渲染线程
private GLThread mGLThread;
//渲染内容
private Renderer mRenderer;
public void setRenderer(Renderer renderer) {
        checkRenderThreadState();
        if (mEGLConfigChooser == null) {
            //创建RGB_888 surface 和 depth buffer
            mEGLConfigChooser = new SimpleEGLConfigChooser(true);
        }
        if (mEGLContextFactory == null) {
            //用来创建EGLContext
            mEGLContextFactory = new DefaultContextFactory();
        }
        if (mEGLWindowSurfaceFactory == null) {
            //用来创建EGLSurface
            mEGLWindowSurfaceFactory = new DefaultWindowSurfaceFactory();
        }
        mRenderer = renderer;
        mGLThread = new GLThread(mThisWeakRef);
        mGLThread.start();
    }
```

GLThread的实现
```
static class GLThread extends Thread {
   public void run() {
           ...
                guardedRun();
           ...
        }
        
        
    private void guardedRun() throws InterruptedException {
            mEglHelper = new EglHelper(mGLSurfaceViewWeakRef);
            mHaveEglContext = false;
            mHaveEglSurface = false;
            mWantRenderNotification = false;

            try {
                GL10 gl = null;
                boolean createEglContext = false;
                boolean createEglSurface = false;
                boolean createGlInterface = false;
                boolean lostEglContext = false;
                boolean sizeChanged = false;
                boolean wantRenderNotification = false;
                boolean doRenderNotification = false;
                boolean askedToReleaseEglContext = false;
                int w = 0;
                int h = 0;
                Runnable event = null;
                Runnable finishDrawingRunnable = null;

                while (true) {
                    synchronized (sGLThreadManager) {
                        while (true) {
                            //退出线程
                            if (mShouldExit) {
                                return;
                            }
                            //执行eventQueue 
                            if (! mEventQueue.isEmpty()) {
                                event = mEventQueue.remove(0);
                                break;
                            }
                           ....
                            // Ready to draw?
                            if (readyToDraw()) {
                                if (! mHaveEglContext) {
                                    if (askedToReleaseEglContext) {
                                        askedToReleaseEglContext = false;
                                    } else {
                                        try {
                                        //创建egl环境
                                            mEglHelper.start();
                                        } catch (RuntimeException t) {
                                        ...
                                }
                    ....
                    if (createEglSurface) {
                        // 2. 创建 GLSurface 描述帧缓冲
                        if (mEglHelper.createSurface()) {
                            synchronized(sGLThreadManager) {
                                mFinishedCreatingEglSurface = true;
                                sGLThreadManager.notifyAll();
                            }
                        ...
                    }

                    if (createGlInterface) {
                        gl = (GL10) mEglHelper.createGL();
                        createGlInterface = false;
                    }

                    if (createEglContext) {
                        //回调onSurfaceCreated
                        GLSurfaceView view = mGLSurfaceViewWeakRef.get();
                        if (view != null) {
                            try {
                                Trace.traceBegin(Trace.TRACE_TAG_VIEW, "onSurfaceCreated");
                                view.mRenderer.onSurfaceCreated(gl, mEglHelper.mEglConfig);
                            } finally {
                                Trace.traceEnd(Trace.TRACE_TAG_VIEW);
                            }
                        }
                        createEglContext = false;
                    }
                    //回调onSurfaceChanged
                    if (sizeChanged) {
                        if (LOG_RENDERER) {
                            Log.w("GLThread", "onSurfaceChanged(" + w + ", " + h + ")");
                        }
                        GLSurfaceView view = mGLSurfaceViewWeakRef.get();
                        if (view != null) {
                            try {
                                Trace.traceBegin(Trace.TRACE_TAG_VIEW, "onSurfaceChanged");
                                view.mRenderer.onSurfaceChanged(gl, w, h);
                            } finally {
                                Trace.traceEnd(Trace.TRACE_TAG_VIEW);
                            }
                        }
                        sizeChanged = false;
                    }
                    //回调onDrawFrame
                    {
                        GLSurfaceView view = mGLSurfaceViewWeakRef.get();
                        if (view != null) {
                            try {
                                Trace.traceBegin(Trace.TRACE_TAG_VIEW, "onDrawFrame");
                                view.mRenderer.onDrawFrame(gl);
                                if (finishDrawingRunnable != null) {
                                    finishDrawingRunnable.run();
                                    finishDrawingRunnable = null;
                                }
                            } ...
                        }
                    }
                    // 4. 将 EGLSurface 中的数据推入 SurfaceFlinger
                    int swapError = mEglHelper.swap();
                    //处理结果
                    switch (swapError) {
                        case EGL10.EGL_SUCCESS:
                            break;
                        case EGL11.EGL_CONTEXT_LOST:
                            if (LOG_SURFACE) {
                                Log.i("GLThread", "egl context lost tid=" + getId());
                            }
                            lostEglContext = true;
                            break;
                        default:
                            // Other errors typically mean that the current surface is bad,
                            // probably because the SurfaceView surface has been destroyed,
                            // but we haven't been notified yet.
                            // Log the error to help developers understand why rendering stopped.
                            EglHelper.logEglErrorAsWarning("GLThread", "eglSwapBuffers", swapError);
                            synchronized(sGLThreadManager) {
                                mSurfaceIsBad = true;
                                sGLThreadManager.notifyAll();
                            }
                            break;
                    }
             ....
        }     
}
```




其他方法
```
//运行在glthread
 public void queueEvent(Runnable r) {
        mGLThread.queueEvent(r);
    }
```
