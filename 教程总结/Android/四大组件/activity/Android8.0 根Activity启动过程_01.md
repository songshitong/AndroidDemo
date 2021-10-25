http://liuwangshu.cn/framework/component/6-activity-start-1.html  android 8.0

在几个月前我写了Android深入四大组件（一）应用程序启动过程（前篇）和Android深入四大组件（一）应用程序启动过程（后篇）这两篇文章，
它们都是基于Android 7.0，当我开始阅读Android 8.0源码时发现应用程序（根Activity）启动过程照Android 7.0有了一些变化，
因此又写下了本篇文章，本篇文章照此前的文章不仅流程发生变化，而且增加了一些分析，算是升级版本。由于篇幅较长，
Android8.0 根Activity启动过程仍旧分为前篇和后篇来进行讲解

概述
Activity的启动过程分为两种，一种是根Activity的启动过程，另一种是普通Activity的启动过程，根Activity指的是应用程序启动的第一个Activity，
因此根Activity的启动过程一般情况下也可以理解为应用程序的启动过程。普通Activity指的是除了应用程序启动的第一个Activity之外的其他的Activity。
这里介绍的是根Activity的启动过程，它和普通Activity的启动过程是有重叠部分的，只不过根Activity的启动过程一般情况下指的就是应用程序的启动过程，
更具有指导性意义。想要了解普通Activity的启动过程的的同学可以参考根Activity的启动过程去自行阅读源码。

根Activity的启动过程比较复杂，因此这里分为三个部分来讲，分别是Launcher请求AMS过程、 AMS到ApplicationThread的调用过程
和ActivityThread启动Activity，本篇文章会介绍前两个部分。

Launcher请求AMS过程
Launcher启动后会将已安装应用程序的快捷图标显示到桌面上，这些应用程序的快捷图标就是启动根Activity的入口，
当我们点击某个应用程序的快捷图标时就会通过Launcher请求AMS来启动该应用程序。时序图如下图所示。
Launcher请求AMS.png

当我们点击应用程序的快捷图标时，就会调用Launcher的startActivitySafely方法，如下所示。
packages/apps/Launcher3/src/com/android/launcher3/Launcher.java
```
public boolean startActivitySafely(View v, Intent intent, ItemInfo item) {
     ...
     intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//1
     if (v != null) {
         intent.setSourceBounds(getViewBounds(v));
     }
     try {
         if (Utilities.ATLEAST_MARSHMALLOW
                 && (item instanceof ShortcutInfo)
                 && (item.itemType == Favorites.ITEM_TYPE_SHORTCUT
                  || item.itemType == Favorites.ITEM_TYPE_DEEP_SHORTCUT)
                 && !((ShortcutInfo) item).isPromise()) {
             startShortcutIntentSafely(intent, optsBundle, item);
         } else if (user == null || user.equals(Process.myUserHandle())) {
             startActivity(intent, optsBundle);//2
         } else {
             LauncherAppsCompat.getInstance(this).startActivityForProfile(
                     intent.getComponent(), user, intent.getSourceBounds(), optsBundle);
         }
         return true;
     } catch (ActivityNotFoundException|SecurityException e) {
         Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
         Log.e(TAG, "Unable to launch. tag=" + item + " intent=" + intent, e);
     }
     return false;
 }
```


在注释1处设置Flag为Intent.FLAG_ACTIVITY_NEW_TASK①，这样根Activity会在新的任务栈中启动。在注释2处会调用startActivity方法，
这个startActivity方法的实现在Activity中，如下所示。
frameworks/base/core/java/android/app/Activity.java
```
@Override
  public void startActivity(Intent intent, @Nullable Bundle options) {
      if (options != null) {
          startActivityForResult(intent, -1, options);
      } else {
          startActivityForResult(intent, -1);
      }
  }
```

startActivity方法中会调用startActivityForResult方法，它的第二个参数为-1，表示Launcher不需要知道Activity启动的结果，
startActivityForResult方法的代码如下所示。
frameworks/base/core/java/android/app/Activity.java
```
public void startActivityForResult(@RequiresPermission Intent intent, int requestCode,
           @Nullable Bundle options) {
       if (mParent == null) {//1
           options = transferSpringboardActivityOptions(options);
           Instrumentation.ActivityResult ar =
               mInstrumentation.execStartActivity(
                   this, mMainThread.getApplicationThread(), mToken, this,
                   intent, requestCode, options);
          ...
       } else {
         ...
       }
   }
```
//todo  Instrumentation
注释1处的mParent是Activity类型的，表示当前Activity的父类。因为目前根Activity还没有创建出来，因此，mParent == null成立。
接着调用Instrumentation的execStartActivity方法，Instrumentation主要用来监控应用程序和系统的交互，execStartActivity方法的代码如下所示。
frameworks/base/core/java/android/app/Instrumentation.java
```
public ActivityResult execStartActivity(
          Context who, IBinder contextThread, IBinder token, Activity target,
          Intent intent, int requestCode, Bundle options) {
      ...
      try {
          intent.migrateExtraStreamToClipData();
          intent.prepareToLeaveProcess(who);
          int result = ActivityManager.getService()
              .startActivity(whoThread, who.getBasePackageName(), intent,
                      intent.resolveTypeIfNeeded(who.getContentResolver()),
                      token, target != null ? target.mEmbeddedID : null,
                      requestCode, 0, null, options);
          checkStartActivityResult(result, intent);
      } catch (RemoteException e) {
          throw new RuntimeException("Failure from system", e);
      }
      return null;
  }
```

首先会调用ActivityManager的getService方法来获取AMS的代理对象，接着调用它的startActivity方法。
这里与Android 7.0代码的逻辑有些不同，Android 7.0是通过ActivityManagerNative的getDefault来获取AMS的代理对象，
现在这个逻辑封装到了ActivityManager中而不是ActivityManagerNative中。首先我们先来查看ActivityManager的getService方法做了什么：

frameworks/base/core/java/android/app/ActivityManager.java
```
public static IActivityManager getService() {
       return IActivityManagerSingleton.get();
   }

   private static final Singleton<IActivityManager> IActivityManagerSingleton =
           new Singleton<IActivityManager>() {
               @Override
               protected IActivityManager create() {
                   final IBinder b = ServiceManager.getService(Context.ACTIVITY_SERVICE);//1
                   final IActivityManager am = IActivityManager.Stub.asInterface(b);//2
                   return am;
               }
           };
```

getService方法调用了IActivityManagerSingleton的get方法，我们接着往下看，IActivityManagerSingleton 是一个Singleton类。
注释1处得到名为”activity”的Service引用，也就是IBinder类型的AMS的引用。
接着在注释2处将它转换成IActivityManager类型的对象，这段代码采用的是AIDL，IActivityManager.java类是由AIDL工具在编译时自动生成的，
IActivityManager.aidl的文件路径为：frameworks/base/core/java/android/app/IActivityManager.aidl 。
要实现进程间通信，服务端也就是AMS只需要继承IActivityManager.Stub类并实现相应的方法就可以了。
注意Android 8.0 之前并没有采用AIDL，而是采用了类似AIDL的形式，用AMS的代理对象ActivityManagerProxy来与AMS进行进程间通信，
Android 8.0 去除了ActivityManagerNative的内部类ActivityManagerProxy，代替它的则是IActivityManager，它是AMS在本地的代理

回到Instrumentation类的execStartActivity方法中，从上面得知execStartActivity方法最终调用的是AMS的startActivity方法




AMS到ApplicationThread的调用过程
Launcher请求AMS后，代码逻辑已经走到了AMS中，接着是AMS到ApplicationThread的调用流程，时序图如图4-2所示
AMS到ApplicationThread的调用流程.png

AMS的startActivity方法如下所示。
frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java
```
@Override
  public final int startActivity(IApplicationThread caller, String callingPackage,
          Intent intent, String resolvedType, IBinder resultTo, String resultWho, int requestCode,
          int startFlags, ProfilerInfo profilerInfo, Bundle bOptions) {
      return startActivityAsUser(caller, callingPackage, intent, resolvedType, resultTo,
              resultWho, requestCode, startFlags, profilerInfo, bOptions,
              UserHandle.getCallingUserId());
  }
```


AMS的startActivity方法中return了startActivityAsUser方法，可以发现startActivityAsUser方法比startActivity方法多了一个参数UserHandle.getCallingUserId()，
这个方法会获得调用者的UserId，AMS会根据这个UserId来确定调用者的权限。
frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java
```
@Override
public final int startActivityAsUser(IApplicationThread caller, String callingPackage,
        Intent intent, String resolvedType, IBinder resultTo, String resultWho, int requestCode,
        int startFlags, ProfilerInfo profilerInfo, Bundle bOptions, int userId) {
    //判断调用者进程是否被隔离    
    enforceNotIsolatedCaller("startActivity");//1
    //检查调用者权限
    userId = mUserController.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(),
            userId, false, ALLOW_FULL_ONLY, "startActivity", null);//2
    return mActivityStarter.startActivityMayWait(caller, -1, callingPackage, intent,
            resolvedType, null, null, resultTo, resultWho, requestCode, startFlags,
            profilerInfo, null, null, bOptions, false, userId, null, null,
            "startActivityAsUser");
}
```

注释1处判断调用者进程是否被隔离，如果被隔离则抛出SecurityException异常，注释2处用于检查调用者是否有权限，如果没有权限也会抛出SecurityException异常。
最后调用了ActivityStarter的startActivityLocked方法，startActivityLocked方法的参数要比startActivityAsUser多几个，
需要注意的是倒数第二个参数类型为TaskRecord，代表启动的Activity所在的栈。最后一个参数”startActivityAsUser”代表启动的理由。 代码如下所示。

frameworks/base/services/core/java/com/android/server/am/ActivityStarter.java
```
final int startActivityMayWait(IApplicationThread caller, int callingUid,
            String callingPackage, Intent intent, String resolvedType,
            IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor,
            IBinder resultTo, String resultWho, int requestCode, int startFlags,
            ProfilerInfo profilerInfo, WaitResult outResult,
            Configuration globalConfig, Bundle bOptions, boolean ignoreTargetSecurity, int userId,
            IActivityContainer iContainer, TaskRecord inTask, String reason) {
         ...
        int res = startActivityLocked(caller, intent, ephemeralIntent, resolvedType,
                    aInfo, rInfo, voiceSession, voiceInteractor,
                    resultTo, resultWho, requestCode, callingPid,
                    callingUid, callingPackage, realCallingPid, realCallingUid, startFlags,
                    options, ignoreTargetSecurity, componentSpecified, outRecord, container,
                    inTask, reason);
         ...
         return res;
     }
 }
```

ActivityStarter是Android 7.0新加入的类，它是加载Activity的控制类，会收集所有的逻辑来决定如何将Intent和Flags转换为Activity，
并将Activity和Task以及Stack相关联。ActivityStarter的startActivityMayWait方法调用了startActivityLocked方法，如下所示。
frameworks/base/services/core/java/com/android/server/am/ActivityStarter.java
```
int startActivityLocked(IApplicationThread caller, Intent intent, Intent ephemeralIntent,
         String resolvedType, ActivityInfo aInfo, ResolveInfo rInfo,
         IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor,
         IBinder resultTo, String resultWho, int requestCode, int callingPid, int callingUid,
         String callingPackage, int realCallingPid, int realCallingUid, int startFlags,
         ActivityOptions options, boolean ignoreTargetSecurity, boolean componentSpecified,
         ActivityRecord[] outActivity, ActivityStackSupervisor.ActivityContainer container,
         TaskRecord inTask, String reason) {
     //判断启动的理由不为空
     if (TextUtils.isEmpty(reason)) {//1
         throw new IllegalArgumentException("Need to specify a reason.");
     }
     mLastStartReason = reason;
     mLastStartActivityTimeMs = System.currentTimeMillis();
     mLastStartActivityRecord[0] = null;
     mLastStartActivityResult = startActivity(caller, intent, ephemeralIntent, resolvedType,
             aInfo, rInfo, voiceSession, voiceInteractor, resultTo, resultWho, requestCode,
             callingPid, callingUid, callingPackage, realCallingPid, realCallingUid, startFlags,
             options, ignoreTargetSecurity, componentSpecified, mLastStartActivityRecord,
             container, inTask);
     if (outActivity != null) {
         outActivity[0] = mLastStartActivityRecord[0];
     }
     return mLastStartActivityResult;
 }
```

注释1处判断启动的理由不为空，如果为空则抛出IllegalArgumentException异常。紧接着又调用了startActivity方法，如下所示。
frameworks/base/services/core/java/com/android/server/am/ActivityStarter.java
```
private int startActivity(IApplicationThread caller, Intent intent, Intent ephemeralIntent,
          String resolvedType, ActivityInfo aInfo, ResolveInfo rInfo,
          IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor,
          IBinder resultTo, String resultWho, int requestCode, int callingPid, int callingUid,
          String callingPackage, int realCallingPid, int realCallingUid, int startFlags,
          ActivityOptions options, boolean ignoreTargetSecurity, boolean componentSpecified,
          ActivityRecord[] outActivity, ActivityStackSupervisor.ActivityContainer container,
          TaskRecord inTask) {
      int err = ActivityManager.START_SUCCESS;
      final Bundle verificationBundle
              = options != null ? options.popAppVerificationBundle() : null;
      ProcessRecord callerApp = null;
      if (caller != null) {//1
          //获取Launcher进程
          callerApp = mService.getRecordForAppLocked(caller);//2
          if (callerApp != null) {
            //获取Launcher进程的pid和uid并赋值
              callingPid = callerApp.pid;
              callingUid = callerApp.info.uid;
          } else {
              Slog.w(TAG, "Unable to find app for caller " + caller
                      + " (pid=" + callingPid + ") when starting: "
                      + intent.toString());
              err = ActivityManager.START_PERMISSION_DENIED;
          }
      }
      ...
      //创建即将要启动的Activity的描述类ActivityRecord
      ActivityRecord r = new ActivityRecord(mService, callerApp, callingPid, callingUid,
              callingPackage, intent, resolvedType, aInfo, mService.getGlobalConfiguration(),
              resultRecord, resultWho, requestCode, componentSpecified, voiceSession != null,
              mSupervisor, container, options, sourceRecord); //2  
      if (outActivity != null) {
          outActivity[0] = r;//3
      }
      ...
          doPendingActivityLaunchesLocked(false);
          return startActivity(r, sourceRecord, voiceSession, voiceInteractor, startFlags, true,
              options, inTask, outActivity);//4
  }

```

ActivityStarter的startActivity方法逻辑比较多，这里列出部分我们需要关心的代码。注释1处判断IApplicationThread类型的caller是否为null，
这个caller是方法调用一路传过来的，指向的是Launche进程的ApplicationThread对象，在注释2处调用AMS的getRecordForAppLocked方法得到的是代表Launcher进程的callerApp对象，
它是ProcessRecord类型的，ProcessRecord用于描述一个应用程序进程。同样的，ActivityRecord用于描述一个Activity，
  用来记录一个Activity的所有信息。
在注释2处创建ActivityRecord，这个ActivityRecord用于描述即将要启动的Activity，并在注释3处将创建的ActivityRecord赋值给ActivityRecord[]类型的outActivity，
  这个outActivity会作为注释4处的startActivity方法的参数传递下去。
frameworks/base/services/core/java/com/android/server/am/ActivityStarter.java
```
private int startActivity(final ActivityRecord r, ActivityRecord sourceRecord,
            IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor,
            int startFlags, boolean doResume, ActivityOptions options, TaskRecord inTask,
            ActivityRecord[] outActivity) {
        int result = START_CANCELED;
        try {
            mService.mWindowManager.deferSurfaceLayout();
            result = startActivityUnchecked(r, sourceRecord, voiceSession, voiceInteractor,
                    startFlags, doResume, options, inTask, outActivity);
        }
        ...
        return result;
    }
```

startActivity方法紧接着调用了startActivityUnchecked方法：
frameworks/base/services/core/java/com/android/server/am/ActivityStarter.java
```
  private int startActivityUnchecked(final ActivityRecord r, ActivityRecord sourceRecord,
            IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor,
            int startFlags, boolean doResume, ActivityOptions options, TaskRecord inTask,
            ActivityRecord[] outActivity) {
...
 if (mStartActivity.resultTo == null && mInTask == null && !mAddingToTask
                && (mLaunchFlags & FLAG_ACTIVITY_NEW_TASK) != 0) {//1
            newTask = true;
            //创建新的TaskRecord
            result = setTaskFromReuseOrCreateNewTask(
                    taskToAffiliate, preferredLaunchStackId, topStack);//2
        } else if (mSourceRecord != null) {
            result = setTaskFromSourceRecord();
        } else if (mInTask != null) {
            result = setTaskFromInTask();
        } else {
            setTaskToCurrentTopOrCreateNewTask();
        }
       ...
 if (mDoResume) {
            final ActivityRecord topTaskActivity =
                    mStartActivity.getTask().topRunningActivityLocked();
            if (!mTargetStack.isFocusable()
                    || (topTaskActivity != null && topTaskActivity.mTaskOverlay
                    && mStartActivity != topTaskActivity)) {
               ...
            } else {
                if (mTargetStack.isFocusable() && !mSupervisor.isFocusedStack(mTargetStack)) {
                    mTargetStack.moveToFront("startActivityUnchecked");
                }
                mSupervisor.resumeFocusedStackTopActivityLocked(mTargetStack, mStartActivity,
                        mOptions);//3
            }
        } else {
            mTargetStack.addRecentActivityLocked(mStartActivity);
        }
        ...

}
```
Supervisor   [ˈsuːpəvaɪzə(r)]  监督人;指导者;主管人   visor  [ˈvaɪzə(r)](头盔上的)面甲，面罩，护面;遮阳帽舌;

startActivityUnchecked方法主要处理栈管理相关的逻辑。在标注①处我们得知，启动根Activity时会将Intent的Flag设置为FLAG_ACTIVITY_NEW_TASK，
这样注释1处的条件判断就会满足，接着执行注释2处的setTaskFromReuseOrCreateNewTask方法，其内部会创建一个新的TaskRecord，
TaskRecord用来描述一个Activity任务栈，也就是说setTaskFromReuseOrCreateNewTask方法内部会创建一个新的Activity任务栈。
Activity任务栈其实是一个假想的模型，并不真实的存在，关于Activity任务栈可以阅读Android解析ActivityManagerService（二）ActivityTask和Activity栈管理这篇文章。//todo
在注释3处会调用ActivityStackSupervisor的resumeFocusedStackTopActivityLocked方法，如下所示。
frameworks/base/services/core/java/com/android/server/am/ActivityStackSupervisor.java
```
boolean resumeFocusedStackTopActivityLocked(
        ActivityStack targetStack, ActivityRecord target, ActivityOptions targetOptions) {
    if (targetStack != null && isFocusedStack(targetStack)) {
        return targetStack.resumeTopActivityUncheckedLocked(target, targetOptions);
    }
    //获取要启动的Activity所在栈的栈顶的不是处于停止状态的ActivityRecord
    final ActivityRecord r = mFocusedStack.topRunningActivityLocked();//1
    if (r == null || r.state != RESUMED) {//2
        mFocusedStack.resumeTopActivityUncheckedLocked(null, null);//3
    } else if (r.state == RESUMED) {
        mFocusedStack.executeAppTransition(targetOptions);
    }
    return false;
}
```
注释1处调用ActivityStack的topRunningActivityLocked方法获取要启动的Activity所在栈的栈顶的不是处于停止状态的ActivityRecord。
注释2处如果ActivityRecord不为null，或者要启动的Activity的状态不是RESUMED状态，就会调用注释3处的ActivityStack的resumeTopActivityUncheckedLocked方法，
对于即将要启动的Activity，注释2的条件判断是肯定满足，因此我们来查看ActivityStack的resumeTopActivityUncheckedLocked方法，如下所示。
frameworks/base/services/core/java/com/android/server/am/ActivityStack.java
```
boolean resumeTopActivityUncheckedLocked(ActivityRecord prev, ActivityOptions options) {
      if (mStackSupervisor.inResumeTopActivity) {
          return false;
      }
      boolean result = false;
      try {
          mStackSupervisor.inResumeTopActivity = true;
          result = resumeTopActivityInnerLocked(prev, options);//1
      } finally {
          mStackSupervisor.inResumeTopActivity = false;
      }
      mStackSupervisor.checkReadyForSleepLocked();
      return result;
  }
```

紧接着查看注释1处ActivityStack的resumeTopActivityInnerLocked方法：
frameworks/base/services/core/java/com/android/server/am/ActivityStack.java
```
private boolean resumeTopActivityInnerLocked(ActivityRecord prev, ActivityOptions options) {
      ...
           mStackSupervisor.startSpecificActivityLocked(next, true, true);
       }
        if (DEBUG_STACK) mStackSupervisor.validateTopActivitiesLocked();
       return true;
}
```

resumeTopActivityInnerLocked方法代码非常多，我们只需要关注调用了ActivityStackSupervisor的startSpecificActivityLocked方法，代码如下所示。
frameworks/base/services/core/java/com/android/server/am/ActivityStackSupervisor.java
```
void startSpecificActivityLocked(ActivityRecord r,
           boolean andResume, boolean checkConfig) {
       //获取即将要启动的Activity的所在的应用程序进程
       ProcessRecord app = mService.getProcessRecordLocked(r.processName,
               r.info.applicationInfo.uid, true);//1
       r.getStack().setLaunchTime(r);

       if (app != null && app.thread != null) {//2
           try {
               if ((r.info.flags&ActivityInfo.FLAG_MULTIPROCESS) == 0
                       || !"android".equals(r.info.packageName)) {
                   app.addPackage(r.info.packageName, r.info.applicationInfo.versionCode,
                           mService.mProcessStats);
               }
               realStartActivityLocked(r, app, andResume, checkConfig);//3
               return;
           } catch (RemoteException e) {
               Slog.w(TAG, "Exception when starting activity "
                       + r.intent.getComponent().flattenToShortString(), e);
           }
       }
       mService.startProcessLocked(r.processName, r.info.applicationInfo, true, 0,
               "activity", r.intent.getComponent(), false, false, true);
   }
```

注释1处获取即将要启动的Activity的所在的应用程序进程，注释2处判断要启动的Activity的所在应用程序进程已经运行的话，
就会调用注释3处的realStartActivityLocked方法，需要注意的是，这个方法的第二个参数是代表要启动的Activity的所在的应用程序进程的ProcessRecord。
frameworks/base/services/core/java/com/android/server/am/ActivityStackSupervisor.java
```
final boolean realStartActivityLocked(ActivityRecord r, ProcessRecord app,
          boolean andResume, boolean checkConfig) throws RemoteException {
   ...
          app.thread.scheduleLaunchActivity(new Intent(r.intent), r.appToken,
                  System.identityHashCode(r), r.info, new Configuration(mService.mConfiguration),
                  new Configuration(task.mOverrideConfig), r.compat, r.launchedFromPackage,
                  task.voiceInteractor, app.repProcState, r.icicle, r.persistentState, results,
                  newIntents, !andResume, mService.isNextTransitionForward(), profilerInfo);
  ...      
      return true;
  }
```

这里的 app.thread指的是IApplicationThread，它的实现是ActivityThread的内部类ApplicationThread，
其中ApplicationThread继承了IApplicationThread.Stub。app指的是传入的要启动的Activity的所在的应用程序进程，
因此，注释1处的代码指的就是要在目标应用程序进程启动Activity。当前代码逻辑运行在AMS所在的进程（SyetemServer进程），
通过ApplicationThread来与应用程序进程进行Binder通信，换句话说，ApplicationThread是AMS所在进程（SyetemServer进程）和应用程序进程的通信桥梁，如下图所示
//todo 各个service是运行在SystemServer进程的吗


本文我们学习了根Activity的启动过程的前两个部分，分别是Launcher请求AMS过程、 AMS到ApplicationThread的调用过程，
完成第二个部分后代码逻辑就运行在了应用程序进程中，后篇会接着介绍ActivityThread启动Activity的过程以及根Activity启动过程中涉及的进程
AMS与ApplicationThread所在进程通信.png