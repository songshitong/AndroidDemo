http://liuwangshu.cn/framework/booting/4-launcher.html   android7.0

这一篇文章我们就来学习Android系统启动流程的最后一步：Launcher的启动流程，并结合本系列的前三篇文章的内容来讲解Android系统启动流程

1.Launcher概述
Android系统启动的最后一步是启动一个Home应用程序，这个应用程序用来显示系统中已经安装的应用程序，这个Home应用程序就叫做Launcher。
应用程序Launcher在启动过程中会请求PackageManagerService返回系统中已经安装的应用程序的信息，并将这些信息封装成一个快捷图标列表显示在系统屏幕上，
    这样用户可以通过点击这些快捷图标来启动相应的应用程序


2.Launcher启动流程
SyetemServer进程在启动的过程中会启动PackageManagerService，PackageManagerService启动后会将系统中的应用程序安装完成。
在此前已经启动的ActivityManagerService会将Launcher启动起来。
启动Launcher的入口为ActivityManagerService的systemReady函数，如下所示。
frameworks/base/services/java/com/android/server/SystemServer.java
```
 private void startOtherServices() {
 ... 启动other服务的最后
  mActivityManagerService.systemReady(new Runnable() {
            @Override
            public void run() {
                Slog.i(TAG, "Making services ready");
                mSystemServiceManager.startBootPhase(
                        SystemService.PHASE_ACTIVITY_MANAGER_READY);

...
}
...
}
```
在startOtherServices函数中，会调用ActivityManagerService的systemReady函数：
frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java
```
public void systemReady(final Runnable goingCallback) {
...
synchronized (this) {
           ...  代码的最后了
            mStackSupervisor.resumeFocusedStackTopActivityLocked();
            mUserController.sendUserSwitchBroadcastsLocked(-1, currentUserId);
        }
    }
```
systemReady函数中调用了ActivityStackSupervisor的resumeFocusedStackTopActivityLocked函数：
frameworks/base/services/core/java/com/android/server/am/ActivityStackSupervisor.java
```
boolean resumeFocusedStackTopActivityLocked(
            ActivityStack targetStack, ActivityRecord target, ActivityOptions targetOptions) {
        if (targetStack != null && isFocusedStack(targetStack)) {
            return targetStack.resumeTopActivityUncheckedLocked(target, targetOptions);//1
        }
        final ActivityRecord r = mFocusedStack.topRunningActivityLocked();
        if (r == null || r.state != RESUMED) {
            mFocusedStack.resumeTopActivityUncheckedLocked(null, null);
        }
        return false;
    }
```

在注释1处会调用ActivityStack的resumeTopActivityUncheckedLocked函数，ActivityStack对象是用来描述Activity堆栈的，
resumeTopActivityUncheckedLocked函数如下所示。

frameworks/base/services/core/java/com/android/server/am/ActivityStack.java
```
boolean resumeTopActivityUncheckedLocked(ActivityRecord prev, ActivityOptions options) {
       if (mStackSupervisor.inResumeTopActivity) {
           // Don't even start recursing.
           return false;
       }
       boolean result = false;
       try {
           // Protect against recursion.
           mStackSupervisor.inResumeTopActivity = true;
           if (mService.mLockScreenShown == ActivityManagerService.LOCK_SCREEN_LEAVING) {
               mService.mLockScreenShown = ActivityManagerService.LOCK_SCREEN_HIDDEN;
               mService.updateSleepIfNeededLocked();
           }
           result = resumeTopActivityInnerLocked(prev, options);//1
       } finally {
           mStackSupervisor.inResumeTopActivity = false;
       }
      return result;
   }
```

注释1调用了resumeTopActivityInnerLocked函数：
```
 private boolean resumeTopActivityInnerLocked(ActivityRecord prev, ActivityOptions options) {
   ...
   return isOnHomeDisplay() &&
          mStackSupervisor.resumeHomeStackTask(returnTaskType, prev, "prevFinished");
   ...                 
}
```

resumeTopActivityInnerLocked函数的代码很长，我们截取我们要分析的关键的一句：调用ActivityStackSupervisor的resumeHomeStackTask函数，代码如下所示。
frameworks/base/services/core/java/com/android/server/am/ActivityStackSupervisor.java
```
boolean resumeHomeStackTask(int homeStackTaskType, ActivityRecord prev, String reason) {
    ...
    if (r != null && !r.finishing) {
        mService.setFocusedActivityLocked(r, myReason);
        return resumeFocusedStackTopActivityLocked(mHomeStack, prev, null);
    }
    return mService.startHomeActivityLocked(mCurrentUser, myReason);//1
}
```
在注释1处调用了ActivityManagerService的startHomeActivityLocked函数，如下所示。
frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java
```
boolean startHomeActivityLocked(int userId, String reason) {
     if (mFactoryTest == FactoryTest.FACTORY_TEST_LOW_LEVEL
             && mTopAction == null) {//1
         return false;
     }
     Intent intent = getHomeIntent();//2
     ActivityInfo aInfo = resolveActivityInfo(intent, STOCK_PM_FLAGS, userId);
     if (aInfo != null) {
         intent.setComponent(new ComponentName(aInfo.applicationInfo.packageName, aInfo.name));
         aInfo = new ActivityInfo(aInfo);
         aInfo.applicationInfo = getAppInfoForUser(aInfo.applicationInfo, userId);
         ProcessRecord app = getProcessRecordLocked(aInfo.processName,
                 aInfo.applicationInfo.uid, true);
         if (app == null || app.instrumentationClass == null) {//3
             intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_NEW_TASK);
             mActivityStarter.startHomeActivityLocked(intent, aInfo, reason);//4
         }
     } else {
         Slog.wtf(TAG, "No home screen found for " + intent, new Throwable());
     }

     return true;
 }
```
注释1处的mFactoryTest代表系统的运行模式，系统的运行模式分为三种，分别是非工厂模式、低级工厂模式和高级工厂模式，
  mTopAction则用来描述第一个被启动Activity组件的Action，它的值为Intent.ACTION_MAIN。因此注释1的代码意思就是mFactoryTest
  为FactoryTest.FACTORY_TEST_LOW_LEVEL（低级工厂模式）并且mTopAction=null时，直接返回false。
注释2处的getHomeIntent函数如下所示
```
Intent getHomeIntent() {
    Intent intent = new Intent(mTopAction, mTopData != null ? Uri.parse(mTopData) : null);
    intent.setComponent(mTopComponent);
    intent.addFlags(Intent.FLAG_DEBUG_TRIAGED_MISSING);
    if (mFactoryTest != FactoryTest.FACTORY_TEST_LOW_LEVEL) {
        intent.addCategory(Intent.CATEGORY_HOME);
    }
    return intent;
}
```
getHomeIntent函数中创建了Intent，并将mTopAction和mTopData传入。mTopAction的值为Intent.ACTION_MAIN，
并且如果系统运行模式不是低级工厂模式则将intent的Category设置为Intent.CATEGORY_HOME。
我们再回到ActivityManagerService的startHomeActivityLocked函数，假设系统的运行模式不是低级工厂模式，在注释3处判断符合Action为Intent.ACTION_MAIN，
Category为Intent.CATEGORY_HOME的应用程序是否已经启动，如果没启动则调用注释4的方法启动该应用程序。这个被启动的应用程序就是Launcher，
因为Launcher的Manifest文件中的intent-filter标签匹配了Action为Intent.ACTION_MAIN，Category为Intent.CATEGORY_HOME。
Launcher的Manifest文件如下所示
packages/apps/Launcher3/AndroidManifest.xml
```
<manifest
    xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.android.launcher3">
    <uses-sdk android:targetSdkVersion="23" android:minSdkVersion="16"/>
 ...
 <application
        ...
        <activity
            android:name="com.android.launcher3.Launcher"
            android:launchMode="singleTask"
            android:clearTaskOnLaunch="true"
            android:stateNotNeeded="true"
            android:theme="@style/Theme"
            android:windowSoftInputMode="adjustPan"
            android:screenOrientation="nosensor"
            android:configChanges="keyboard|keyboardHidden|navigation"
            android:resumeWhilePausing="true"
            android:taskAffinity=""
            android:enabled="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.HOME" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.MONKEY"/>
            </intent-filter>
        </activity>
...
  </application> 
</manifest>         
```
这样，应用程序Launcher就会被启动起来，并执行它的onCreate函数。


Launcher中应用图标显示流程
Launcher的onCreate函数如下所示。
packages/apps/Launcher3/src/com/android/launcher3/Launcher.java
```
      @Override
    protected void onCreate(Bundle savedInstanceState) {
       ...
        LauncherAppState app = LauncherAppState.getInstance();//1
        mDeviceProfile = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE ?
                app.getInvariantDeviceProfile().landscapeProfile
                : app.getInvariantDeviceProfile().portraitProfile;

        mSharedPrefs = Utilities.getPrefs(this);
        mIsSafeModeEnabled = getPackageManager().isSafeMode();
        mModel = app.setLauncher(this);//2
        ....
        if (!mRestoring) {
            if (DISABLE_SYNCHRONOUS_BINDING_CURRENT_PAGE) {
                mModel.startLoader(PagedView.INVALID_RESTORE_PAGE);//2
            } else {
                mModel.startLoader(mWorkspace.getRestorePage());
            }
        }
...
    }
```

注释1处获取LauncherAppState的实例并在注释2处调用它的setLauncher函数并将Launcher对象传入，LauncherAppState的setLauncher函数如下所示。
packages/apps/Launcher3/src/com/android/launcher3/LauncherAppState.java
```
LauncherModel setLauncher(Launcher launcher) {
     getLauncherProvider().setLauncherProviderChangeListener(launcher);
     mModel.initialize(launcher);//1
     mAccessibilityDelegate = ((launcher != null) && Utilities.ATLEAST_LOLLIPOP) ?
         new LauncherAccessibilityDelegate(launcher) : null;
     return mModel;
 }
```

注释1处会调用LauncherModel的initialize函数：
```
public void initialize(Callbacks callbacks) {
    synchronized (mLock) {
        unbindItemInfosAndClearQueuedBindRunnables();
        mCallbacks = new WeakReference<Callbacks>(callbacks);
    }
}
```

在initialize函数中会将Callbacks，也就是传入的Launcher 封装成一个弱引用对象。因此我们得知mCallbacks变量指的就是封装成弱引用对象的Launcher，
这个mCallbacks后文会用到它。再回到Launcher的onCreate函数，在注释2处调用了LauncherModel的startLoader函数：
packages/apps/Launcher3/src/com/android/launcher3/LauncherModel.java
```
...
 @Thunk static final HandlerThread sWorkerThread = new HandlerThread("launcher-loader");//1
    static {
        sWorkerThread.start();
    }
    @Thunk static final Handler sWorker = new Handler(sWorkerThread.getLooper());//2
...
   public void startLoader(int synchronousBindPage, int loadFlags) {
        InstallShortcutReceiver.enableInstallQueue();
        synchronized (mLock) {
            synchronized (mDeferredBindRunnables) {
                mDeferredBindRunnables.clear();
            }
            if (mCallbacks != null && mCallbacks.get() != null) {
                stopLoaderLocked();
                mLoaderTask = new LoaderTask(mApp.getContext(), loadFlags);//3
                if (synchronousBindPage != PagedView.INVALID_RESTORE_PAGE
                        && mAllAppsLoaded && mWorkspaceLoaded && !mIsLoaderTaskRunning) {
                    mLoaderTask.runBindSynchronousPage(synchronousBindPage);
                } else {
                    sWorkerThread.setPriority(Thread.NORM_PRIORITY);
                    sWorker.post(mLoaderTask);//4
                }
            }
        }
    }
```
注释1处创建了具有消息循环的线程HandlerThread对象。
注释2处创建了Handler，并且传入HandlerThread的Looper。Hander的作用就是向HandlerThread发送消息。
在注释3处创建LoaderTask，在注释4处将LoaderTask作为消息发送给HandlerThread 。LoaderTask类实现了Runnable接口，
  当LoaderTask所描述的消息被处理时则会调用它的run函数，代码如下所示
/packages/apps/Launcher3/src/com/android/launcher3/LauncherModel.java
```
private class LoaderTask implements Runnable {
...
       public void run() {
           synchronized (mLock) {
               if (mStopped) {
                   return;
               }
               mIsLoaderTaskRunning = true;
           }
           keep_running: {
               if (DEBUG_LOADERS) Log.d(TAG, "step 1: loading workspace");
               loadAndBindWorkspace();//1
               if (mStopped) {
                   break keep_running;
               }
               waitForIdle();
               if (DEBUG_LOADERS) Log.d(TAG, "step 2: loading all apps");
               loadAndBindAllApps();//2
           }
           mContext = null;
           synchronized (mLock) {
               if (mLoaderTask == this) {
                   mLoaderTask = null;
               }
               mIsLoaderTaskRunning = false;
               mHasLoaderCompletedOnce = true;
           }
       }
  ...     
 }      
```
Launcher是用工作区的形式来显示系统安装的应用程序的快捷图标，每一个工作区都是来描述一个抽象桌面的，它由n个屏幕组成，每个屏幕又分n个单元格，
  每个单元格用来显示一个应用程序的快捷图标。
注释1处调用loadAndBindWorkspace函数用来加载工作区信息，
注释2处的loadAndBindAllApps函数是用来加载系统已经安装的应用程序信息，loadAndBindAllApps函数代码如下所示
```
private void loadAndBindAllApps() {
    if (DEBUG_LOADERS) {
        Log.d(TAG, "loadAndBindAllApps mAllAppsLoaded=" + mAllAppsLoaded);
    }
    if (!mAllAppsLoaded) {
        loadAllApps();//1
        synchronized (LoaderTask.this) {
            if (mStopped) {
                return;
            }
        }
        updateIconCache();
        synchronized (LoaderTask.this) {
            if (mStopped) {
                return;
            }
            mAllAppsLoaded = true;
        }
    } else {
        onlyBindAllApps();
    }
}
```
如果系统没有加载已经安装的应用程序信息，则会调用注释1处的loadAllApps函数：
```
  private void loadAllApps() {
...
        mHandler.post(new Runnable() {
            public void run() {
                final long bindTime = SystemClock.uptimeMillis();
                final Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                if (callbacks != null) {
                    callbacks.bindAllApplications(added);//1
                    if (DEBUG_LOADERS) {
                        Log.d(TAG, "bound " + added.size() + " apps in "
                                + (SystemClock.uptimeMillis() - bindTime) + "ms");
                    }
                } else {
                    Log.i(TAG, "not binding apps: no Launcher activity");
                }
            }
        });
       ...
    }
```
在注释1处会调用callbacks的bindAllApplications函数，在前面我们得知这个callbacks实际是指向Launcher的，
因此我们来查看Launcher的bindAllApplications函数，代码如下所示
packages/apps/Launcher3/src/com/android/launcher3/Launcher.java
```
public void bindAllApplications(final ArrayList<AppInfo> apps) {
    if (waitUntilResume(mBindAllApplicationsRunnable, true)) {
        mTmpAppsList = apps;
        return;
    }
    if (mAppsView != null) {
        mAppsView.setApps(apps);//1
    }
    if (mLauncherCallbacks != null) {
        mLauncherCallbacks.bindAllApplications(apps);
    }
}
```
在注释1处会调用AllAppsContainerView的setApps函数，并将包含应用信息的列表apps传进去，AllAppsContainerView的setApps函数如下所示。
packages/apps/Launcher3/src/com/android/launcher3/allapps/AllAppsContainerView.java
```
public void setApps(List<AppInfo> apps) {
      mApps.setApps(apps);
  }
```

包含应用信息的列表apps已经传给了AllAppsContainerView，查看AllAppsContainerView的onFinishInflate函数：
```
 @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
...
        // Load the all apps recycler view
        mAppsRecyclerView = (AllAppsRecyclerView) findViewById(R.id.apps_list_view);//1
        mAppsRecyclerView.setApps(mApps);//2
        mAppsRecyclerView.setLayoutManager(mLayoutManager);
        mAppsRecyclerView.setAdapter(mAdapter);//3
        mAppsRecyclerView.setHasFixedSize(true);
        mAppsRecyclerView.addOnScrollListener(mElevationController);
        mAppsRecyclerView.setElevationController(mElevationController);
...
    }
```
onFinishInflate函数在加载完xml文件时就会调用，在注释1处得到AllAppsRecyclerView用来显示App列表，
并在注释2处将apps的信息列表传进去，并在注释3处为AllAppsRecyclerView设置Adapter。这样应用程序快捷图标的列表就会显示在屏幕上。
到这里Launcher启动流程就讲到这




Android系统启动流程
那么结合本篇以及本系列的前三篇文章，我们就可以得出Android系统启动流程，如下所示。
1.启动电源以及系统启动
当电源按下时引导芯片代码开始从预定义的地方（固化在ROM）开始执行。加载引导程序Bootloader到RAM，然后执行。
2.引导程序BootLoader
引导程序BootLoader是在Android操作系统开始运行前的一个小程序，它的主要作用是把系统OS拉起来并运行。
3.Linux内核启动
内核启动时，设置缓存、被保护存储器、计划列表、加载驱动。当内核完成系统设置，它首先在系统文件中寻找init.rc文件，并启动init进程。
4.init进程启动
初始化和启动属性服务，并且启动Zygote进程。
5.Zygote进程启动
创建JavaVM并为JavaVM注册JNI，创建服务端Socket，启动SystemServer进程。
6.SystemServer进程启动
启动Binder线程池和SystemServiceManager，并且启动各种系统服务。
7.Launcher启动
被SystemServer进程启动的ActivityManagerService会启动Launcher，Launcher启动后会将已安装应用的快捷图标显示到界面上。
Android系统启动流程图.png