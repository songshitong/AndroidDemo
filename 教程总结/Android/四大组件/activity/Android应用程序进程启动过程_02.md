android 7.0
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
        //传入的是false
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

```
    private void attach(boolean system) {
        sCurrentActivityThread = this;
        mSystemThread = system;
        if (!system) {
             ..
            final IActivityManager mgr = ActivityManagerNative.getDefault();
            try {
                mgr.attachApplication(mAppThread);
            } catch (RemoteException ex) {
                throw ex.rethrowFromSystemServer();
            }
             ...
        } else {
            ...
```

frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java
```
public final void attachApplication(IApplicationThread thread) {
        synchronized (this) {
            int callingPid = Binder.getCallingPid();
            final long origId = Binder.clearCallingIdentity();
            attachApplicationLocked(thread, callingPid);
            Binder.restoreCallingIdentity(origId);
        }
    }
 
private final boolean attachApplicationLocked(IApplicationThread thread,
            int pid) {
      ...      
        //系统处于ready状态或者该app为FLAG_PERSISTENT进程,则为true
        boolean normalMode = mProcessesReady || isAllowedWhileBooting(app.info);
        //获取provider
        List<ProviderInfo> providers = normalMode ? generateApplicationProvidersLocked(app) : null;
    
        //app进程存在正在启动中的provider,则超时10s后发送CONTENT_PROVIDER_PUBLISH_TIMEOUT_MSG消息
        if (providers != null && checkAppInLaunchingProvidersLocked(app)) {
            Message msg = mHandler.obtainMessage(CONTENT_PROVIDER_PUBLISH_TIMEOUT_MSG);
            msg.obj = app;
            mHandler.sendMessageDelayed(msg, CONTENT_PROVIDER_PUBLISH_TIMEOUT);
        }    
        ...
        try {
          ...
            ProfilerInfo profilerInfo = profileFile == null ? null
                    : new ProfilerInfo(profileFile, profileFd, samplingInterval, profileAutoStop);
            //调用ActivityThread.bindApplication      
            thread.bindApplication(processName, appInfo, providers, app.instrumentationClass,
                    profilerInfo, app.instrumentationArguments, app.instrumentationWatcher,
                    app.instrumentationUiAutomationConnection, testMode,
                    mBinderTransactionTrackingEnabled, enableTrackAllocation,
                    isRestrictedBackupMode || !normalMode, app.persistent,
                    new Configuration(mConfiguration), app.compat,
                    getCommonServicesLocked(app.isolated),
                    mCoreSettingsObserver.getCoreSettingsLocked());
            updateLruProcessLocked(app, false, null);
            app.lastRequestedGc = app.lastLowMemory = SystemClock.uptimeMillis();
        } catch (Exception e) {
            ...
            return false;
        }
       ...
       //处理activity
       // See if the top visible activity is waiting to run in this process...
        if (normalMode) {
            try {
                if (mStackSupervisor.attachApplicationLocked(app)) {
                    didSomething = true;
                }
            } catch (Exception e) {
                Slog.wtf(TAG, "Exception thrown launching activities in " + app, e);
                badApp = true;
            }
        }
        //处理services
        // Find any services that should be running in this process...
        if (!badApp) {
            try {
                didSomething |= mServices.attachApplicationLocked(app, processName);
            } catch (Exception e) {
                Slog.wtf(TAG, "Exception thrown starting services in " + app, e);
                badApp = true;
            }
        }
        //处理广播
        // Check if a next-broadcast receiver is in this process...
        if (!badApp && isPendingBroadcastProcessLocked(pid)) {
            try {
                didSomething |= sendPendingBroadcastsLocked(app);
            } catch (Exception e) {
                // If the app died trying to launch the receiver we declare it 'bad'
                Slog.wtf(TAG, "Exception thrown dispatching broadcasts in " + app, e);
                badApp = true;
            }
        } 
       ...
        return true;
    }    
```
可以处理bindApplication后，处理provider,broadcast,service,activity
http://gityuan.com/2016/10/09/app-process-create-2/

处理ContentProvider
```
    private final List<ProviderInfo> generateApplicationProvidersLocked(ProcessRecord app) {
        List<ProviderInfo> providers = null;
        //查询contentProvider
        try {
            providers = AppGlobals.getPackageManager()
                    .queryContentProviders(app.processName, app.uid,
                            STOCK_PM_FLAGS | PackageManager.GET_URI_PERMISSION_PATTERNS
                                    | MATCH_DEBUG_TRIAGED_MISSING)
                    .getList();
        } catch (RemoteException ex) {
        }
        ...
        int userId = app.userId;
        if (providers != null) {
            int N = providers.size();
            app.pubProviders.ensureCapacity(N + app.pubProviders.size());
            for (int i=0; i<N; i++) {
                //android待办: keep logic in sync with installEncryptionUnawareProviders
                ProviderInfo cpi =
                    (ProviderInfo)providers.get(i);
                boolean singleton = isSingleton(cpi.processName, cpi.applicationInfo,
                        cpi.name, cpi.flags);
                if (singleton && UserHandle.getUserId(app.uid) != UserHandle.USER_SYSTEM) {
                    // This is a singleton provider, but a user besides the
                    // default user is asking to initialize a process it runs
                    // in...  well, no, it doesn't actually run in this process,
                    // it runs in the process of the default user.  Get rid of it.
                    providers.remove(i);
                    N--;
                    i--;
                    continue;
                }

                ComponentName comp = new ComponentName(cpi.packageName, cpi.name);
                ContentProviderRecord cpr = mProviderMap.getProviderByClass(comp, userId);
                if (cpr == null) {
                    cpr = new ContentProviderRecord(this, cpi, app.info, comp, singleton);
                    mProviderMap.putProviderByClass(comp, cpr);
                }
                ...
                //添加到ProcessRecord的pubProviders
                app.pubProviders.put(cpi.name, cpr);
                if (!cpi.multiprocess || !"android".equals(cpi.packageName)) {
                    // Don't add this if it is a platform component that is marked
                    // to run in multiple processes, because this is actually
                    // part of the framework so doesn't make sense to track as a
                    // separate apk in the process.
                    app.addPackage(cpi.applicationInfo.packageName, cpi.applicationInfo.versionCode,
                            mProcessStats);
                }
                //更新PackageManagerService的LastPackageUsageTime
                notifyPackageUse(cpi.applicationInfo.packageName,
                                 PackageManager.NOTIFY_PACKAGE_USE_CONTENT_PROVIDER);
            }
        }
        return providers;
    }
```
ContentProvider被添加到ProcessRecord的pubProviders，后面会在installContentProviders时调用ContentProvider.create，
   可以查看Content_Provider的启动过程.md
查询ContentProvider
/frameworks/base/services/core/java/com/android/server/pm/PackageManagerService.java
```
    public @NonNull ParceledListSlice<ProviderInfo> queryContentProviders(String processName,
            int uid, int flags) {
        final int userId = processName != null ? UserHandle.getUserId(uid)
                : UserHandle.getCallingUserId();
        if (!sUserManager.exists(userId)) return ParceledListSlice.emptyList();
        flags = updateFlagsForComponent(flags, userId, processName);

        ArrayList<ProviderInfo> finalList = null;
        // reader
        synchronized (mPackages) {
            final Iterator<PackageParser.Provider> i = mProviders.mProviders.values().iterator();
            while (i.hasNext()) {
                final PackageParser.Provider p = i.next();
                PackageSetting ps = mSettings.mPackages.get(p.owner.packageName);
                if (ps != null && p.info.authority != null
                        && (processName == null
                                || (p.info.processName.equals(processName)
                                        && UserHandle.isSameApp(p.info.applicationInfo.uid, uid)))
                        && mSettings.isEnabledAndMatchLPr(p.info, flags, userId)) {
                    if (finalList == null) {
                        finalList = new ArrayList<ProviderInfo>(3);
                    }
                    ProviderInfo info = PackageParser.generateProviderInfo(p, flags,
                            ps.readUserState(userId), userId);
                    if (info != null) {
                        finalList.add(info);
                    }
                }
            }
        }

        if (finalList != null) {
            Collections.sort(finalList, mProviderInitOrderSorter);
            return new ParceledListSlice<ProviderInfo>(finalList);
        }

        return ParceledListSlice.emptyList();
    }
```

处理activity
/frameworks/base/services/core/java/com/android/server/am/ActivityStackSupervisor.java
```
boolean attachApplicationLocked(ProcessRecord app) throws RemoteException {
        final String processName = app.processName;
        boolean didSomething = false;
        for (int displayNdx = mActivityDisplays.size() - 1; displayNdx >= 0; --displayNdx) {
            ArrayList<ActivityStack> stacks = mActivityDisplays.valueAt(displayNdx).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; --stackNdx) {
                final ActivityStack stack = stacks.get(stackNdx);
                if (!isFocusedStack(stack)) {
                    continue;
                }
                ActivityRecord hr = stack.topRunningActivityLocked();
                if (hr != null) {
                    if (hr.app == null && app.uid == hr.info.applicationInfo.uid
                            && processName.equals(hr.processName)) {
                        try {
                            if (realStartActivityLocked(hr, app, true, true)) {
                                didSomething = true;
                            }
                        } catch (RemoteException e) {
                              ....
                        }
                    }
                }
            }
        }
        if (!didSomething) {
            ensureActivitiesVisibleLocked(null, 0, !PRESERVE_WINDOWS);
        }
        return didSomething;
    }
```
realStartActivityLocked查看应用程序启动过程_02.md，启动activity


处理services
/frameworks/base/services/core/java/com/android/server/am/ActiveServices.java
```
    boolean attachApplicationLocked(ProcessRecord proc, String processName)
            throws RemoteException {
        boolean didSomething = false;
        // Collect any services that are waiting for this process to come up.
        if (mPendingServices.size() > 0) {
            ServiceRecord sr = null;
            try {
                for (int i=0; i<mPendingServices.size(); i++) {
                    sr = mPendingServices.get(i);
                    if (proc != sr.isolatedProc && (proc.uid != sr.appInfo.uid
                            || !processName.equals(sr.processName))) {
                        continue;
                    }

                    mPendingServices.remove(i);
                    i--;
                    proc.addPackage(sr.appInfo.packageName, sr.appInfo.versionCode,
                            mAm.mProcessStats);
                    //启动service        
                    realStartServiceLocked(sr, proc, sr.createdFromFg);
                    didSomething = true;
                    if (!isServiceNeeded(sr, false, false)) {
                        // We were waiting for this service to start, but it is actually no
                        // longer needed.  This could happen because bringDownServiceIfNeeded
                        // won't bring down a service that is pending...  so now the pending
                        // is done, so let's drop it.
                        bringDownServiceLocked(sr);
                    }
                }
            } catch (RemoteException e) {
                  ...
            }
        }
        // Also, if there are any services that are waiting to restart and
        // would run in this process, now is a good time to start them.  It would
        // be weird to bring up the process but arbitrarily not let the services
        // run at this point just because their restart time hasn't come up.
         //重启service
        if (mRestartingServices.size() > 0) {
            ServiceRecord sr;
            for (int i=0; i<mRestartingServices.size(); i++) {
                sr = mRestartingServices.get(i);
                if (proc != sr.isolatedProc && (proc.uid != sr.appInfo.uid
                        || !processName.equals(sr.processName))) {
                    continue;
                }
                mAm.mHandler.removeCallbacks(sr.restarter);
                mAm.mHandler.post(sr.restarter);
            }
        }
        return didSomething;
    }
```
realStartServiceLocked查看Service的启动过程.md

处理广播
```
  // The app just attached; send any pending broadcasts that it should receive
    boolean sendPendingBroadcastsLocked(ProcessRecord app) {
        boolean didSomething = false;
        for (BroadcastQueue queue : mBroadcastQueues) {
            didSomething |= queue.sendPendingBroadcastsLocked(app);
        }
        return didSomething;
    }
```
BroadcastQueue.sendPendingBroadcastsLocked的流程查看广播的注册、发送和接收过程.md

ActivityThread.bindApplication的过程
frameworks/base/core/java/android/app/ActivityThread.java
```
     public final void bindApplication(String processName, ApplicationInfo appInfo,
                List<ProviderInfo> providers, ComponentName instrumentationName,
                ProfilerInfo profilerInfo, Bundle instrumentationArgs,
                IInstrumentationWatcher instrumentationWatcher,
                IUiAutomationConnection instrumentationUiConnection, int debugMode,
                boolean enableBinderTracking, boolean trackAllocation,
                boolean isRestrictedBackupMode, boolean persistent, Configuration config,
                CompatibilityInfo compatInfo, Map<String, IBinder> services, Bundle coreSettings) {

            ...
            AppBindData data = new AppBindData(); //此时会将进程信息传递给ActivityThread
             ...
            sendMessage(H.BIND_APPLICATION, data);
        }
        
handler的处理        
case BIND_APPLICATION:
                    Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, "bindApplication");
                    AppBindData data = (AppBindData)msg.obj;
                    handleBindApplication(data);
                    Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
                    break;        
```
handleBindApplication
```
    private void handleBindApplication(AppBindData data) {
       ...
        try {
            // If the app is being launched for full backup or restore, bring it up in
            // a restricted environment with the base application class.
            //创建Application
            Application app = data.info.makeApplication(data.restrictedBackupMode, null);
            mInitialApplication = app;

            // don't bring up providers in restricted mode; they may depend on the
            // app's custom Application class
            if (!data.restrictedBackupMode) {
                if (!ArrayUtils.isEmpty(data.providers)) {
                    //安装contentProviders
                    installContentProviders(app, data.providers);
                    // For process that contains content providers, we want to
                    // ensure that the JIT is enabled "at some point".
                    mH.sendEmptyMessageDelayed(H.ENABLE_JIT, 10*1000);
                }
            }

            // Do this after providers, since instrumentation tests generally start their
            // test thread at this point, and we don't want that racing.
            try {
                mInstrumentation.onCreate(data.instrumentationArgs);
            }
            catch (Exception e) {
                throw new RuntimeException(
                    "Exception thrown in onCreate() of "
                    + data.instrumentationName + ": " + e.toString(), e);
            }

            try {
               //调用Application的onCreate
                mInstrumentation.callApplicationOnCreate(app);
            } catch (Exception e) {
                ...
            }
        } finally {
            StrictMode.setThreadPolicy(savedPolicy);
        }
    }
```
创建Application的过程
makeApplication
frameworks/base/core/java/android/app/LoadedApk.java
```
    public Application makeApplication(boolean forceDefaultAppClass,
            Instrumentation instrumentation) {
        if (mApplication != null) {
            return mApplication;
        }
        Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, "makeApplication");
        Application app = null;
        String appClass = mApplicationInfo.className;
        if (forceDefaultAppClass || (appClass == null)) {
            appClass = "android.app.Application";
        }

        try {
            java.lang.ClassLoader cl = getClassLoader();
            if (!mPackageName.equals("android")) {
                Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER,
                        "initializeJavaContextClassLoader");
                initializeJavaContextClassLoader();
                Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
            }           
            ContextImpl appContext = ContextImpl.createAppContext(mActivityThread, this);
            //关键 通过反射创建application
            app = mActivityThread.mInstrumentation.newApplication(
                    cl, appClass, appContext);
            appContext.setOuterContext(app);
        } catch (Exception e) {
           ....
        return app;
    }
```
frameworks/base/core/java/android/app/Instrumentation.java
```
  public Application newApplication(ClassLoader cl, String className, Context context)
            throws InstantiationException, IllegalAccessException, 
            ClassNotFoundException {
        return newApplication(cl.loadClass(className), context);
    }
   static public Application newApplication(Class<?> clazz, Context context)
            throws InstantiationException, IllegalAccessException, 
            ClassNotFoundException {
        Application app = (Application)clazz.newInstance();
        //绑定context
        app.attach(context);
        return app;
    }  
```
frameworks/base/core/java/android/app/Application.java
```
final void attach(Context context) {
        //绑定context
        attachBaseContext(context);
        mLoadedApk = ContextImpl.getImpl(context).mPackageInfo;
    }
```

Application的onCreate调用
frameworks/base/core/java/android/app/Instrumentation.java
```
 public void callApplicationOnCreate(Application app) {
        app.onCreate();
    }
```