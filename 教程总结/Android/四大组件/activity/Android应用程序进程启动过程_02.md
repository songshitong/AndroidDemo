在前篇中我们讲到了Android应用程序进程启动过程，这一篇我们来讲遗留的知识点：在应用程序进程创建过程中会启动Binder线程池
以及在应用程序进程启动后会创建消息循环。

Binder线程池启动过程
我们首先来看RuntimeInit类的zygoteInit函数，如下所示
frameworks/base/core/java/com/android/internal/os/RuntimeInit.java
```
public static final void zygoteInit(int targetSdkVersion, String[] argv, ClassLoader classLoader)
          throws ZygoteInit.MethodAndArgsCaller {
      if (DEBUG) Slog.d(TAG, "RuntimeInit: Starting application from zygote");
      Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, "RuntimeInit");
      redirectLogStreams();
      commonInit();
      nativeZygoteInit();//1
      applicationInit(targetSdkVersion, argv, classLoader);
  }
```

注释1处会在新创建的应用程序进程中创建Binder线程池，来查看nativeZygoteInit函数：
```
private static final native void nativeZygoteInit();
```

很明显nativeZygoteInit是一个jni方法，它对应的函数是什么呢。在 AndroidRuntime.cpp的JNINativeMethod数组中我们得知
它对应的函数是com_android_internal_os_RuntimeInit_nativeZygoteInit，如下所示。
frameworks/base/core/jni/AndroidRuntime.cpp
```
static const JNINativeMethod gMethods[] = {
    { "nativeFinishInit", "()V",
        (void*) com_android_internal_os_RuntimeInit_nativeFinishInit },
    { "nativeZygoteInit", "()V",
        (void*) com_android_internal_os_RuntimeInit_nativeZygoteInit },
    { "nativeSetExitWithoutCleanup", "(Z)V",
        (void*) com_android_internal_os_RuntimeInit_nativeSetExitWithoutCleanup },
};
```

接着来查看 com_android_internal_os_RuntimeInit_nativeZygoteInit函数：
frameworks/base/core/jni/AndroidRuntime.cpp
```
static void com_android_internal_os_RuntimeInit_nativeZygoteInit(JNIEnv* env, jobject clazz)
{
    gCurRuntime->onZygoteInit();
}
```

gCurRuntime是在AndroidRuntime初始化就创建的。如下所示。
frameworks/base/core/jni/AndroidRuntime.cpp
```
AndroidRuntime::AndroidRuntime(char* argBlockStart, const size_t argBlockLength) :
        mExitWithoutCleanup(false),
        mArgBlockStart(argBlockStart),
        mArgBlockLength(argBlockLength)
{
   ...
    gCurRuntime = this;
}
```

在Android系统启动流程（二）解析Zygote进程启动过程这篇文章我们得知AppRuntime继承AndroidRuntime，
AppRuntime创建时就会调用AndroidRuntime的构造函数，gCurRuntime就会被初始化，它指向的是AppRuntime，
因此我们来查看AppRuntime的onZygoteInit函数，AppRuntime的实现在app_main.cpp中，如下所示。
frameworks/base/cmds/app_process/app_main.cpp
```
virtual void onZygoteInit()
   {
       sp<ProcessState> proc = ProcessState::self();
       ALOGV("App process: starting thread pool.\n");
       proc->startThreadPool();
   }
```

最后一行会调用ProcessState的startThreadPool函数：
frameworks/native/libs/binder/ProcessState.cpp
```
void ProcessState::startThreadPool()
{
    AutoMutex _l(mLock);
    if (!mThreadPoolStarted) {
        mThreadPoolStarted = true;
        spawnPooledThread(true);
    }
}
```
//todo binder 线程池
支持Binder通信的进程中都有一个ProcessState类，它里面有一个mThreadPoolStarted 变量，来表示Binder线程池是否已经被启动过，默认值为false。
在每次调用这个函数时都会先去检查这个标记，从而确保Binder线程池只会被启动一次。如果Binder线程池未被启动则设置mThreadPoolStarted为true，
最后调用spawnPooledThread函数来创建线程池中的第一个线程，也就是线程池的main线程，如下所示。
frameworks/native/libs/binder/ProcessState.cpp
```
void ProcessState::spawnPooledThread(bool isMain)
{
    if (mThreadPoolStarted) {
        String8 name = makeBinderThreadName();
        ALOGV("Spawning new pooled thread, name=%s\n", name.string());
        sp<Thread> t = new PoolThread(isMain);
        t->run(name.string());//1
    }
}
```

可以看到Binder线程为一个PoolThread。注释1调用PoolThread的run函数来启动一个启动一个新的线程。来查看PoolThread类里做了什么：
frameworks/native/libs/binder/ProcessState.cpp
```
class PoolThread : public Thread
{
..
protected:
    virtual bool threadLoop()
    {
        IPCThreadState::self()->joinThreadPool(mIsMain);//1
        return false;
    }
    const bool mIsMain;
};
```

PoolThread类继承了Thread类。注释1处会将调用IPCThreadState的joinThreadPool函数，将当前线程注册到Binder驱动程序中，
这样我们创建的线程就加入了Binder线程池中，这样新创建的应用程序进程就支持Binder进程间通信了，Binder线程池启动过程就讲到这，
接下来我们来学习消息循环创建过程。



2.消息循环创建过程
首先我们回到上篇最后讲到的RuntimeInit的invokeStaticMain函数，代码如下所示。
frameworks/base/core/java/com/android/internal/os/RuntimeInit.java
```
private static void invokeStaticMain(String className, String[] argv, ClassLoader classLoader)
        throws ZygoteInit.MethodAndArgsCaller {
    Class<?> cl;
  ...
    throw new ZygoteInit.MethodAndArgsCaller(m, argv);
}
```

invokeStaticMain函数在上篇已经讲过，这里不再赘述，主要是看最后一行，会抛出一个MethodAndArgsCaller异常，
这个异常会被ZygoteInit的main函数捕获，如下所示。
frameworks/base/core/java/com/android/internal/os/ZygoteInit.java
```
public static void main(String argv[]) {
   ...
      try {
         ...
      } catch (MethodAndArgsCaller caller) {
          caller.run();//1
      } catch (RuntimeException ex) {
          Log.e(TAG, "Zygote died with exception", ex);
          closeServerSocket();
          throw ex;
      }
  }
```

注释1处捕获到MethodAndArgsCaller 时会执行caller的run函数，如下所示。
frameworks/base/core/java/com/android/internal/os/RuntimeInit.java
```
public static class MethodAndArgsCaller extends Exception
           implements Runnable {
       private final Method mMethod;
       private final String[] mArgs;
       public MethodAndArgsCaller(Method method, String[] args) {
           mMethod = method;
           mArgs = args;
       }
       public void run() {
           try {
               mMethod.invoke(null, new Object[] { mArgs });//1
           } catch (IllegalAccessException ex) {
               throw new RuntimeException(ex);
           }
           ...
               throw new RuntimeException(ex);
           }
       }
   }
```

根据上一篇文章我们得知，mMethod指的就是ActivityThread的main函数，mArgs 指的是应用程序进程的启动参数。
在注释1处调用ActivityThread的main函数，代码如下所示。
frameworks/base/core/java/android/app/ActivityThread.java
```
 public static void main(String[] args) {
        Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, "ActivityThreadMain");
        SamplingProfilerIntegration.start();
...
        Looper.prepareMainLooper();//1
        ActivityThread thread = new ActivityThread();//2
        thread.attach(false);
        if (sMainThreadHandler == null) {
            sMainThreadHandler = thread.getHandler();
        }
        if (false) {
            Looper.myLooper().setMessageLogging(new
                    LogPrinter(Log.DEBUG, "ActivityThread"));
        }
        Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
        Looper.loop();//3
        throw new RuntimeException("Main thread loop unexpectedly exited");
    }
```

注释1处在当前应用程序进程中创建消息循环，注释2处创建ActivityThread，注释3处调用Looper的loop，使得Looper开始工作，
开始处理消息。可以看出，系统在应用程序进程启动完成后，就会创建一个消息循环，用来方便的使用Android的消息处理机制。