
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
调用 mEglHelper.start 初始化 GL 的环境
调用 mEglHelper.createSurface 创建缓冲帧 EGLSurface
回调 Renderer 的生命周期  onSurfaceCreated,onSurfaceChanged,onDrawFrame
调用 mEglHelper.swap 将 EGLSurface 中的数据推入 SurfaceFlinger


mEglHelper.start
```
 private static class EglHelper {
     public void start() {
            ......
            // 获取 EGL10 对象
            mEgl = (EGL10) EGLContext.getEGL();

            // 1. 创建 EGLDisplay 描述本地窗口的连接
            mEglDisplay = mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
            ......
            // 1.2 初始化 EGLDisplay
            int[] version = new int[2];
            if(!mEgl.eglInitialize(mEglDisplay, version)) {
                throw new RuntimeException("eglInitialize failed");
            }
            
            GLSurfaceView view = mGLSurfaceViewWeakRef.get();
            if (view == null) {
                mEglConfig = null;
                mEglContext = null;
            } else {
                // 2. 创建 EGLConfig
                mEglConfig = view.mEGLConfigChooser.chooseConfig(mEgl, mEglDisplay);
                // 3. 创建 EGLContext
                mEglContext = view.mEGLContextFactory.createContext(mEgl, mEglDisplay, mEglConfig);
            }
            ......

            mEglSurface = null;
        }
 }
```
EglHelper.start 中的流程是非常清晰, 主要有三个步骤
1 创建 EGLDisplay 用于描述硬件屏幕
  初始化 EGL, 获取主次版本号信息
2 创建 EGLConfig
3 创建 EGLContext

Egl创建 android 
EGLContext.getEGL() 是EGLImpl


创建 EGLDisplay
frameworks/base/opengl/java/com/google/android/gles_jni/EGLImpl.java
```
public synchronized EGLDisplay eglGetDisplay(Object native_display) {
    long value = _eglGetDisplay(native_display);
    if (value == 0) {
        return EGL10.EGL_NO_DISPLAY;
    }
    if (mDisplay.mEGLDisplay != value)
        mDisplay = new EGLDisplayImpl(value);
    return mDisplay;
}
```



android-8.0_r36
/frameworks/base/core/jni/com_google_android_gles_jni_EGLImpl.cpp
```
{"_eglGetDisplay",   "(" OBJECT ")J", (void*)jni_eglGetDisplay },
 static jlong jni_eglGetDisplay(JNIEnv *_env, jobject _this, jobject native_display) {
      return reinterpret_cast<jlong>(eglGetDisplay(EGL_DEFAULT_DISPLAY));
  } 
```
/frameworks/native/opengl/libagl/egl.cpp
```
 EGLDisplay eglGetDisplay(NativeDisplayType display)
  {
....
      if (display == EGL_DEFAULT_DISPLAY) {
          //typedef void *EGLDisplay;
          EGLDisplay dpy = (EGLDisplay)1;
          // 获取 EGLDisplay 对应的结构体 egl_display_t
          egl_display_t& d = egl_display_t::get_display(dpy);
          // 保存显示的类型
          d.type = display;
          return dpy;
      }
      return EGL_NO_DISPLAY;
  }
  

 struct egl_display_t
  {
     ...
  
      static egl_display_t& get_display(EGLDisplay dpy);
  
      static EGLBoolean is_valid(EGLDisplay dpy) {
          return ((uintptr_t(dpy)-1U) >= NUM_DISPLAYS) ? EGL_FALSE : EGL_TRUE;
      }
      //egl1.2 egl1.3 名称的映射 typedef EGLNativeDisplayType NativeDisplayType;
      NativeDisplayType  type;
      std::atomic_size_t initialized;
  };
  
  static egl_display_t gDisplays[NUM_DISPLAYS];
  
  egl_display_t& egl_display_t::get_display(EGLDisplay dpy) {
      return gDisplays[uintptr_t(dpy)-1U];
  }  
```
Native 层 EGLDisplay 是一个显示设备的标示符

当传入 EGL_DEFAULT_DISPLAY 时, 默认为 1
EGLDisplay 对应了 egl_display_t 结构体



创建 EGLConfig
mEGLConfigChooser.chooseConfig
```
 private class SimpleEGLConfigChooser extends ComponentSizeChooser {
        public SimpleEGLConfigChooser(boolean withDepthBuffer) {
            //传入每个颜色的大小，depthSize
            super(8, 8, 8, 0, withDepthBuffer ? 16 : 0, 0);
        }
    }

 private class ComponentSizeChooser extends BaseConfigChooser {
        public ComponentSizeChooser(int redSize, int greenSize, int blueSize,
                int alphaSize, int depthSize, int stencilSize) {
            //定义 EGLConfig 配置数组    
            super(new int[] {
                    EGL10.EGL_RED_SIZE, redSize,
                    EGL10.EGL_GREEN_SIZE, greenSize,
                    EGL10.EGL_BLUE_SIZE, blueSize,
                    EGL10.EGL_ALPHA_SIZE, alphaSize,
                    EGL10.EGL_DEPTH_SIZE, depthSize,
                    EGL10.EGL_STENCIL_SIZE, stencilSize,
                    EGL10.EGL_NONE});     
       }  
  

  private abstract class BaseConfigChooser
            implements EGLConfigChooser {
        public BaseConfigChooser(int[] configSpec) {
            //适配mEGLContextClientVersion2和3
            mConfigSpec = filterConfigSpec(configSpec);
        }    
        
 private int[] filterConfigSpec(int[] configSpec) {
            if (mEGLContextClientVersion != 2 && mEGLContextClientVersion != 3) {
                return configSpec;
            }
            //mEGLContextClientVersion为2或者3 进行特殊处理
            int len = configSpec.length;
            int[] newConfigSpec = new int[len + 2];
            System.arraycopy(configSpec, 0, newConfigSpec, 0, len-1);
            newConfigSpec[len-1] = EGL10.EGL_RENDERABLE_TYPE;
            if (mEGLContextClientVersion == 2) {
                newConfigSpec[len] = EGL14.EGL_OPENGL_ES2_BIT;  /* EGL_OPENGL_ES2_BIT */
            } else {
                newConfigSpec[len] = EGLExt.EGL_OPENGL_ES3_BIT_KHR; /* EGL_OPENGL_ES3_BIT_KHR */
            }
            newConfigSpec[len+1] = EGL10.EGL_NONE;
            return newConfigSpec;
        }   
 
         public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
            int[] num_config = new int[1];
            //通过 EGL10.eglChooseConfig 获取符合配置的 EGLConfig 个数
            if (!egl.eglChooseConfig(display, mConfigSpec, null, 0,
                    num_config)) {
                 //获取失败   
                throw new IllegalArgumentException("eglChooseConfig failed");
            }

            int numConfigs = num_config[0];
            if (numConfigs <= 0) {
                throw new IllegalArgumentException(
                        "No configs match configSpec");
            }

            EGLConfig[] configs = new EGLConfig[numConfigs];
            //通过 EGL10.eglChooseConfig 获取所有符合配置的 EGLConfig
            if (!egl.eglChooseConfig(display, mConfigSpec, configs, numConfigs,
                    num_config)) {
                throw new IllegalArgumentException("eglChooseConfig#2 failed");
            }
            //调用了抽象方法 chooseConfig, 一般选择 EGLConfig[] 第 0 个元素
            EGLConfig config = chooseConfig(egl, display, configs);
            if (config == null) {
                throw new IllegalArgumentException("No config chosen");
            }
            return config;
        }

        abstract EGLConfig chooseConfig(EGL10 egl, EGLDisplay display,
                EGLConfig[] configs);                 
```
ComponentSizeChooser的实现  找到config的属性符合mRedSize，mGreenSize，mBlueSize，mAlphaSize
```
 public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display,
                EGLConfig[] configs) {
            for (EGLConfig config : configs) {
                int d = findConfigAttrib(egl, display, config,
                        EGL10.EGL_DEPTH_SIZE, 0);
                int s = findConfigAttrib(egl, display, config,
                        EGL10.EGL_STENCIL_SIZE, 0);
                if ((d >= mDepthSize) && (s >= mStencilSize)) {
                    int r = findConfigAttrib(egl, display, config,
                            EGL10.EGL_RED_SIZE, 0);
                    int g = findConfigAttrib(egl, display, config,
                             EGL10.EGL_GREEN_SIZE, 0);
                    int b = findConfigAttrib(egl, display, config,
                              EGL10.EGL_BLUE_SIZE, 0);
                    int a = findConfigAttrib(egl, display, config,
                            EGL10.EGL_ALPHA_SIZE, 0);
                    if ((r == mRedSize) && (g == mGreenSize)
                            && (b == mBlueSize) && (a == mAlphaSize)) {
                        return config;
                    }
                }
            }
            return null;
        }
        
 private int findConfigAttrib(EGL10 egl, EGLDisplay display,
                EGLConfig config, int attribute, int defaultValue) {
              //获取config的属性  
            if (egl.eglGetConfigAttrib(display, config, attribute, mValue)) {
                return mValue[0];
            }
            return defaultValue;
        }       
```
mEGLConfigChooser 的默认实现为 SimpleEGLConfigChooser 其具体的流程如下

定义 EGLConfig 配置数组
过滤配置数组
通过 EGL10.eglChooseConfig 获取符合配置的 EGLConfig 的个数
通过 EGL10.eglChooseConfig 获取所有符合配置的 EGLConfig
取所有符合配置的 EGLConfig 的首元素
有了 EGLConfig 之后便可以创建了 EGLContext 了


mEGLContextFactory.createContext
```
 private class DefaultContextFactory implements EGLContextFactory {
        private int EGL_CONTEXT_CLIENT_VERSION = 0x3098;

        public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig config) {
            int[] attrib_list = {EGL_CONTEXT_CLIENT_VERSION, mEGLContextClientVersion,
                    EGL10.EGL_NONE };

            return egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT,
                    mEGLContextClientVersion != 0 ? attrib_list : null);
        }

    }
```
/frameworks/native/opengl/libagl/egl.cpp
```
8  EGLContext eglCreateContext(EGLDisplay dpy, EGLConfig config,
                              EGLContext /*share_list*/, const EGLint* /*attrib_list*/)
  {
      //校验有效性
      if (egl_display_t::is_valid(dpy) == EGL_FALSE)
          return setError(EGL_BAD_DISPLAY, EGL_NO_SURFACE);
      // 创建 Native 层的 EGLContext
      ogles_context_t* gl = ogles_init(sizeof(egl_context_t));
      if (!gl) return setError(EGL_BAD_ALLOC, EGL_NO_CONTEXT);
      
     // 创建 egl_context_t 真正描述 egl 的上下文
      egl_context_t* c = static_cast<egl_context_t*>(gl->rasterizer.base);
      c->flags = egl_context_t::NEVER_CURRENT;
      c->dpy = dpy;
      c->config = config;
      c->read = 0;
      c->draw = 0;
      return (EGLContext)gl;
  }
```


eglHelper.createSurface()
```
 public boolean createSurface() {
            ...
            GLSurfaceView view = mGLSurfaceViewWeakRef.get();
            if (view != null) {
               // 1.  创建 EGLSurface
                mEglSurface = view.mEGLWindowSurfaceFactory.createWindowSurface(mEgl,
                        mEglDisplay, mEglConfig, view.getHolder());
            } else {
                mEglSurface = null;
            }
            ...
            // 2. 将当前线程的 EGL 上下文设置为当前的 mEglContext
            if (!mEgl.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)) {
                ...
                logEglErrorAsWarning("EGLHelper", "eglMakeCurrent", mEgl.eglGetError());
                return false;
            }
            return true;
        }
```
createWindowSurface
```
 private static class DefaultWindowSurfaceFactory implements EGLWindowSurfaceFactory {

        public EGLSurface createWindowSurface(EGL10 egl, EGLDisplay display,
                EGLConfig config, Object nativeWindow) {
            EGLSurface result = null;
            try {
                result = egl.eglCreateWindowSurface(display, config, nativeWindow, null);
            } catch (IllegalArgumentException e) {
              ....
            }
            return result;
        }
```
/frameworks/native/opengl/libagl/egl.cpp
```
 EGLSurface eglCreateWindowSurface(  EGLDisplay dpy, EGLConfig config,
                                      NativeWindowType window,
                                      const EGLint *attrib_list)
  {
      return createWindowSurface(dpy, config, window, attrib_list);
  }
  
  static EGLSurface createWindowSurface(EGLDisplay dpy, EGLConfig config,
          NativeWindowType window, const EGLint* /*attrib_list*/)
  {
      // 验证 EGLDisplay 是否有效
      if (egl_display_t::is_valid(dpy) == EGL_FALSE)
          return setError(EGL_BAD_DISPLAY, EGL_NO_SURFACE);
      if (window == 0)
          return setError(EGL_BAD_MATCH, EGL_NO_SURFACE);
     ...//校验EGLConfig
      egl_surface_t* surface;
      //创建egl_surface_t
      surface = new egl_window_surface_v2_t(dpy, config, depthFormat,
              static_cast<ANativeWindow*>(window));
  
      if (!surface->initCheck()) {
          delete surface;
          surface = 0;
      }
      return surface;
  }
```
egl_window_surface_v2_t是gl_surface_t的实现类，持有ANativeWindow，对应到Surface/SurfaceTexture
/frameworks/native/opengl/libagl/egl.cpp
```
 egl_window_surface_v2_t::egl_window_surface_v2_t(EGLDisplay dpy,
          EGLConfig config,
          int32_t depthFormat,
          ANativeWindow* window)
      : egl_surface_t(dpy, config, depthFormat),
      nativeWindow(window), buffer(0), previousBuffer(0), bits(NULL)
  {
      pixelFormatTable = gglGetPixelFormatTable();
      // keep a reference on the window
      nativeWindow->common.incRef(&nativeWindow->common);
      nativeWindow->query(nativeWindow, NATIVE_WINDOW_WIDTH, &width);
      nativeWindow->query(nativeWindow, NATIVE_WINDOW_HEIGHT, &height);
  }
```

eglMakeCurrent
/frameworks/native/opengl/libagl/egl.cpp
```
 EGLBoolean eglMakeCurrent(  EGLDisplay dpy, EGLSurface draw,
                              EGLSurface read, EGLContext ctx)
  {
     ...// 验证异常信息
     EGLContext current_ctx = EGL_NO_CONTEXT;
     ...
     //获取当前线程的EGLContext
     if (ctx == EGL_NO_CONTEXT) {
          current_ctx = (EGLContext)getGlThreadSpecific();
      } else {
          egl_context_t* c = egl_context_t::context(ctx);
          egl_surface_t* d = (egl_surface_t*)draw;
          egl_surface_t* r = (egl_surface_t*)read;
          if ((d && d->ctx && d->ctx != ctx) ||
              (r && r->ctx && r->ctx != ctx)) {
              // one of the surface is bound to a context in another thread
              return setError(EGL_BAD_ACCESS, EGL_FALSE);
          }
      }
      // 将传入的上下文置为当前线程的上下文
      ogles_context_t* gl = (ogles_context_t*)ctx;
      if (makeCurrent(gl) == 0) {
         if (ctx) {
                    ......// 处理 EGLSurface 与新的上下文绑定
                } else {
                    ......// 处理 EGLSurface 与上下文的解绑
                }
          return EGL_TRUE;
      }
      return setError(EGL_BAD_ACCESS, EGL_FALSE);
  }
  

static int makeCurrent(ogles_context_t* gl)
  {
      // 之前的上下文
      ogles_context_t* current = (ogles_context_t*)getGlThreadSpecific();
      if (gl) {
          // 传入的上下文
          egl_context_t* c = egl_context_t::context(gl);
          // 若传入的上下文已经标记为当前上下文
          if (c->flags & egl_context_t::IS_CURRENT) {
              if (current != gl) {
                  // 当前上下文与之不等, 这说明我们传入的 c 绑定的是其他线程的上下文
                  return -1;
              }
          } else {
             // 解除当前线程绑定的上下文
              if (current) {
                  // mark the current context as not current, and flush
                  glFlush();
                  egl_context_t::context(current)->flags &= ~egl_context_t::IS_CURRENT;
              }
          }
          // 将传入的 c 置为当前线程的上下文
          if (!(c->flags & egl_context_t::IS_CURRENT)) {
              // The context is not current, make it current!
              setGlThreadSpecific(gl);
              c->flags |= egl_context_t::IS_CURRENT;
          }
      } else {
         // 若 gl 为 null, 则意为取消当前线程上下文的绑定
          if (current) {
              // mark the current context as not current, and flush
              glFlush();
              egl_context_t::context(current)->flags &= ~egl_context_t::IS_CURRENT;
          }
          // this thread has no context attached to it
          setGlThreadSpecific(0);
      }
      return 0;
  }  
```
http://www.aospxref.com/android-8.0.0_r36/xref/frameworks/native/opengl/libagl/context.h#588
使用类似pthread_setspecific，pthread_getspecific的手段
```
#ifdef __ANDROID__
      // We have a dedicated TLS slot in bionic
     ....这里android自定义库了
  #else
      extern pthread_key_t gGLKey;
      inline void setGlThreadSpecific(ogles_context_t *value) {
          pthread_setspecific(gGLKey, value);
      }
      inline ogles_context_t* getGlThreadSpecific() {
          return static_cast<ogles_context_t*>(pthread_getspecific(gGLKey));
      }
  #endif
```
看到 makeCurrent 切换 OpenGL 渲染线程的上下文操作还是比较复杂的

传入 EGLContext 为无效, 则表示清空当前线程的上下文
   EGLSurface 会与之解绑
传入的 EGLContext 有效, 则将其置为当前线程的上下文
   若 EGLContext 已绑定为其他线程的上下文, 会爆出异常
更新 EGLSurface 绑定的上下文


EglHelper.swap
```
 public int swap() {
            if (! mEgl.eglSwapBuffers(mEglDisplay, mEglSurface)) {
                return mEgl.eglGetError();
            }
            return EGL10.EGL_SUCCESS;
        }
```
/frameworks/native/opengl/libagl/egl.cpp
```
 EGLBoolean eglSwapBuffers(EGLDisplay dpy, EGLSurface draw)
  {
      ... // 获取 EGLSurface 对应的结构体 egl_surface_t
      egl_surface_t* d = static_cast<egl_surface_t*>(draw);
      ...
      // post the surface
      d->swapBuffers();
      ..
      return EGL_TRUE;
  }
```
在 EGLSurface 创建的时候, 我们知道它的实现类为 egl_window_surface_v2_t, 
因此我们看看 egl_window_surface_v2_t 的 swapBuffers 的实现
```
EGLBoolean egl_window_surface_v2_t::swapBuffers()
{
       ...
      /*
       * Handle eglSetSwapRectangleANDROID()
       * We copyback from the front buffer
       */
     // 1. 从上一次重绘区域上拷贝数据
      if (!dirtyRegion.isEmpty()) {
          dirtyRegion.andSelf(Rect(buffer->width, buffer->height));
          if (previousBuffer) {
              // 判断是否需要从上一次重绘区域上取数据   声明一个叫copyBack的Region对象
               Region copyBack(Region::subtract(oldDirtyRegion, dirtyRegion));
              if (!copyBack.isEmpty()) {
                 // 从上一帧的缓冲区取数据, 覆盖到 buffer 中
                  void* prevBits;
                  if (lock(previousBuffer,
                          GRALLOC_USAGE_SW_READ_OFTEN, &prevBits) == NO_ERROR) {
                      // copy from previousBuffer to buffer
                      copyBlt(buffer, bits, previousBuffer, prevBits, copyBack);
                      unlock(previousBuffer);
                  }
              }
          }
          // 更新被重绘的区域
          oldDirtyRegion = dirtyRegion;
      }
  
      if (previousBuffer) {
          previousBuffer->common.decRef(&previousBuffer->common);
          previousBuffer = 0;
      }
      // 走到这里, 说明对 buffer 的操作已经完成了, 释放锁定
      unlock(buffer);
      // 将其置为上一帧数据 
      previousBuffer = buffer;
      //使用 nativeWindow 将这个缓冲区到 SurfaceFlinger 待绘制队列
      nativeWindow->queueBuffer(nativeWindow, buffer, -1);
      buffer = 0;
      获取一个新的缓冲区, 保存到 buffer 中
      // dequeue a new buffer
      int fenceFd = -1;
      if (nativeWindow->dequeueBuffer(nativeWindow, &buffer, &fenceFd) == NO_ERROR) {
          // 获取用存储栅格化数据共享内存的内存文件描述符
          sp<Fence> fence(new Fence(fenceFd));
          // 等待缓冲区内存分配完毕
          if (fence->wait(Fence::TIMEOUT_NEVER)) {
              nativeWindow->cancelBuffer(nativeWindow, buffer, fenceFd);
              return setError(EGL_BAD_ALLOC, EGL_FALSE);
          }
  
          // reallocate the depth-buffer if needed
          if ((width != buffer->width) || (height != buffer->height)) {
              width = buffer->width;
              height = buffer->height;
              if (depth.data) {
                  free(depth.data);
                  depth.width   = width;
                  depth.height  = height;
                  depth.stride  = buffer->stride;
                  uint64_t allocSize = static_cast<uint64_t>(depth.stride) *
                          static_cast<uint64_t>(depth.height) * 2;
                  if (depth.stride < 0 || depth.height > INT_MAX ||
                          allocSize > UINT32_MAX) {
                      setError(EGL_BAD_ALLOC, EGL_FALSE);
                      return EGL_FALSE;
                  }
                  depth.data    = (GGLubyte*)malloc(allocSize);
                  if (depth.data == 0) {
                      setError(EGL_BAD_ALLOC, EGL_FALSE);
                      return EGL_FALSE;
                  }
              }
          }
  
          // keep a reference on the buffer
          buffer->common.incRef(&buffer->common);
  
          // finally pin the buffer down
          if (lock(buffer, GRALLOC_USAGE_SW_READ_OFTEN |
                  GRALLOC_USAGE_SW_WRITE_OFTEN, &bits) != NO_ERROR) {
              ALOGE("eglSwapBuffers() failed to lock buffer %p (%ux%u)",
                      buffer, buffer->width, buffer->height);
              return setError(EGL_BAD_ACCESS, EGL_FALSE);
              // FIXME: we should make sure we're not accessing the buffer anymore
          }
      } else {
          return setError(EGL_BAD_CURRENT_SURFACE, EGL_FALSE);
      }
  
      return EGL_TRUE;
  }
```
swapBuffers 的主要任务, 即将绘制到 Buffer 中的数据, 推送到 SurfaceFlinger 的渲染队列, 其主要步骤如下
从上一次重绘区域上拷贝数据
使用 nativeWindow(Surface/SurfaceTexture) 将这个缓冲区到 SurfaceFlinger 进程的 Layer 队列中
获取一个新的缓冲区, 保存到 buffer 中


通过对 GLSurfaceView 中 EGL 分析, 我们得知 EGL 的使用流程如下所示
初始化环境
  创建屏幕的描述 EGLDisplay(egl_display_t)
  初始化 EGL 方法, 获取 EGL 版本号相关信息
  创建 EGL 配置信息 EGLConfig(egl_config_t)
  创建 EGL 上下文 EGLContext(egl_context_t)
  创建缓冲帧 EGLSurface(egl_surface_t)
在线程中使用 EGL
  通过 eglMakeCurrent 为当前线程绑定 EGLContext
/////////////////////////////////////////
  执行 OpenGL ES 的渲染管线
/////////////////////////////////////////
  交换缓冲, 将 EGLSurface 中的数据推送到 SurfaceFlinger 进程对应的 Layer 对象的队列中
  推送之后, 会立即获取一个新的缓冲
  绘制结束后销毁当前线程的 EGL 数据

todo anative_window相关
https://www.cnblogs.com/roger-yu/p/15773010.html




其他方法
```
//运行在glthread
 public void queueEvent(Runnable r) {
        mGLThread.queueEvent(r);
    }
```