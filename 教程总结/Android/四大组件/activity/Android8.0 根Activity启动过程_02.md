http://liuwangshu.cn/framework/component/7-activity-start-2.html  android8.0

ActivityThread启动Activity的过程
通过前篇的介绍，我们知道目前的代码逻辑运行在应用程序进程中。先来查看ActivityThread启动Activity的过程的时序图。

我们接着来查看ApplicationThread的scheduleLaunchActivity方法，其中ApplicationThread是ActivityThread的内部类，
应用程序进程创建后会运行代表主线程的实例ActivityThread，它管理着当前应用程序进程的线程。ApplicationThread的scheduleLaunchActivity方法如下所示。

frameworks/base/core/java/android/app/ActivityThread.java
```
@Override
public final void scheduleLaunchActivity(Intent intent, IBinder token, int ident,
        ActivityInfo info, Configuration curConfig, Configuration overrideConfig,
        CompatibilityInfo compatInfo, String referrer, IVoiceInteractor voiceInteractor,
        int procState, Bundle state, PersistableBundle persistentState,
        List<ResultInfo> pendingResults, List<ReferrerIntent> pendingNewIntents,
        boolean notResumed, boolean isForward, ProfilerInfo profilerInfo) {
    updateProcessState(procState, false);
    ActivityClientRecord r = new ActivityClientRecord();
    r.token = token;
    r.ident = ident;
    r.intent = intent;
    r.referrer = referrer;
    ...
    updatePendingConfiguration(curConfig);
    sendMessage(H.LAUNCH_ACTIVITY, r);
}
```

scheduleLaunchActivity方法会将启动Activity的参数封装成ActivityClientRecord ，sendMessage方法向H类发送类型为LAUNCH_ACTIVITY的消息，
并将ActivityClientRecord 传递过去，sendMessage方法有多个重载方法，最终调用的sendMessage方法如下所示。
frameworks/base/core/java/android/app/ActivityThread.java
```
private void sendMessage(int what, Object obj, int arg1, int arg2, boolean async) {
     if (DEBUG_MESSAGES) Slog.v(
         TAG, "SCHEDULE " + what + " " + mH.codeToString(what)
         + ": " + arg1 + " / " + obj);
     Message msg = Message.obtain();
     msg.what = what;
     msg.obj = obj;
     msg.arg1 = arg1;
     msg.arg2 = arg2;
     if (async) {
         msg.setAsynchronous(true);
     }
     mH.sendMessage(msg);
 }
```

这里mH指的是H，它是ActivityThread的内部类并继承Handler，是应用程序进程中主线程的消息管理类。H的代码如下所示。
```
private class H extends Handler {
      public static final int LAUNCH_ACTIVITY         = 100;
      public static final int PAUSE_ACTIVITY          = 101;
...
public void handleMessage(Message msg) {
          if (DEBUG_MESSAGES) Slog.v(TAG, ">>> handling: " + codeToString(msg.what));
          switch (msg.what) {
              case LAUNCH_ACTIVITY: {
                  Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, "activityStart");
                  final ActivityClientRecord r = (ActivityClientRecord) msg.obj;//1
                  r.packageInfo = getPackageInfoNoCheck(
                          r.activityInfo.applicationInfo, r.compatInfo);//2
                  handleLaunchActivity(r, null, "LAUNCH_ACTIVITY");//3
                  Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
              } break;
              case RELAUNCH_ACTIVITY: {
                  Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, "activityRestart");
                  ActivityClientRecord r = (ActivityClientRecord)msg.obj;
                  handleRelaunchActivity(r);
                  Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
              } break;
            ...
}
```

查看H的handleMessage方法中对LAUNCH_ACTIVITY的处理，在注释1处将传过来的msg的成员变量obj转换为ActivityClientRecord。
在注释2处通过getPackageInfoNoCheck方法获得LoadedApk类型的对象并赋值给ActivityClientRecord 的成员变量packageInfo 。
应用程序进程要启动Activity时需要将该Activity所属的APK加载进来，而LoadedApk就是用来描述已加载的APK文件。
在注释3处调用handleLaunchActivity方法，代码如下所示。
frameworks/base/core/java/android/app/ActivityThread.java
```
private void handleLaunchActivity(ActivityClientRecord r, Intent customIntent, String reason) {
    ...
    WindowManagerGlobal.initialize();
    //启动Activity
    Activity a = performLaunchActivity(r, customIntent);//1
    if (a != null) {
        r.createdConfig = new Configuration(mConfiguration);
        reportSizeConfigurations(r);
        Bundle oldState = r.state;
        //将Activity的状态置为Resume
        handleResumeActivity(r.token, false, r.isForward,
                !r.activity.mFinished && !r.startsNotResumed, r.lastProcessedSeq, reason);//2
        if (!r.activity.mFinished && r.startsNotResumed) {
            performPauseActivityIfNeeded(r, reason);
            if (r.isPreHoneycomb()) {
                r.state = oldState;
            }
        }
    } else {
        try {
            //停止Activity启动
            ActivityManager.getService()
                .finishActivity(r.token, Activity.RESULT_CANCELED, null,
                        Activity.DONT_FINISH_TASK_WITH_ACTIVITY);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }
}
```

注释1处的performLaunchActivity方法用来启动Activity ，注释2处的代码用来将Activity 的状态置为Resume。
如果该Activity为null则会通知AMS停止启动Activity。来查看performLaunchActivity方法做了什么：
frameworks/base/core/java/android/app/ActivityThread.java
```
private Activity performLaunchActivity(ActivityClientRecord r, Intent customIntent) {
       //获取ActivityInfo类
       ActivityInfo aInfo = r.activityInfo;//1
       if (r.packageInfo == null) {
       //获取APK文件的描述类LoadedApk
           r.packageInfo = getPackageInfo(aInfo.applicationInfo, r.compatInfo,
                   Context.CONTEXT_INCLUDE_CODE);//2
       }

       ComponentName component = r.intent.getComponent();//3
       ...
       //创建要启动Activity的上下文环境
       ContextImpl appContext = createBaseContextForActivity(r);//4
       Activity activity = null;
       try {
           java.lang.ClassLoader cl = appContext.getClassLoader();
           //用类加载器来创建该Activity的实例
           activity = mInstrumentation.newActivity(
                   cl, component.getClassName(), r.intent);//5
         ...
       } catch (Exception e) {
         ...
       }

       try {
           //创建Application
           Application app = r.packageInfo.makeApplication(false, mInstrumentation);//6
           ...
           if (activity != null) {
              ...
               /**
               *7 初始化Activity
               */
               activity.attach(appContext, this, getInstrumentation(), r.token,
                       r.ident, app, r.intent, r.activityInfo, title, r.parent,
                       r.embeddedID, r.lastNonConfigurationInstances, config,
                       r.referrer, r.voiceInteractor, window, r.configCallback);

              ...
               if (r.isPersistable()) {
                   mInstrumentation.callActivityOnCreate(activity, r.state, r.persistentState);//8
               } else {
                   mInstrumentation.callActivityOnCreate(activity, r.state);
               }
              ...
           }
           r.paused = true;
           mActivities.put(r.token, r);
       } catch (SuperNotCalledException e) {
           throw e;
       } catch (Exception e) {
         ...
       }

       return activity;
   }
```

注释1处用来获取ActivityInfo，ActivityInfo用于存储代码和AndroidManifes设置的Activity和receiver节点信息，比如Activity的theme和launchMode。
在注释2处获取APK文件的描述类LoadedApk。
注释3处获取要启动的Activity的ComponentName类，ComponentName类中保存了该Activity的包名和类名。
注释4处用来创建要启动Activity的上下文环境。
注释5处根据ComponentName中存储的Activity类名，用类加载器来创建该Activity的实例。
注释6处用来创建Application，makeApplication方法内部会调用Application的onCreate方法。
注释7处调用Activity的attach方法初始化Activity，attach方法中会创建Window对象（PhoneWindow）并与Activity自身进行关联。//todo 与7.0对照
注释8处会调用Instrumentation的callActivityOnCreate方法来启动Activity，如下所示。
frameworks/base/core/java/android/app/Instrumentation.java
```
public void callActivityOnCreate(Activity activity, Bundle icicle,
          PersistableBundle persistentState) {
      prePerformCreate(activity);
      activity.performCreate(icicle, persistentState);//1
      postPerformCreate(activity);
  }
```

注释1处调用了Activity的performCreate方法，代码如下所示。
frameworks/base/core/java/android/app/Activity.java
```
final void performCreate(Bundle icicle, PersistableBundle persistentState) {
      restoreHasCurrentPermissionRequest(icicle);
      onCreate(icicle, persistentState);
      mActivityTransitionState.readState(icicle);
      performCreateCommon();
  }
```
performCreate方法中会调用Activity的onCreate方法，讲到这里，根Activity就启动了，即应用程序就启动了。
根Activity启动过程就讲到这里，下面我们来学习根Activity启动过程中涉及到的进程。

根Activity启动过程中涉及的进程  //todo systemserver getservice
在应用程序进程没有创建的情况下，根Activity启动过程中会涉及到4个进程，分别是Zygote进程、Launcher进程、AMS所在进程（SyetemServer进程）、
应用程序进程。它们之间的关系如下图所示。
根Activity启动过程中涉及的进程.png


首先Launcher进程向AMS请求创建根Activity，AMS会判断根Activity所需的应用程序进程是否存在并启动，如果不存在就会请求Zygote进程创建应用程序进程。
应用程序进程准备就绪后会通知AMS，AMS会请求应用程序进程创建根Activity。上图中步骤2采用的是Socket通信，步骤1和步骤4采用的是Binder通信。
上图可能并不是很直观，为了更好的理解，下面给出这四个进程调用的时序图。
//todo 3用的什么通信

根Activity启动过程中涉及的进程时序图.png

如果是普通Activity启动过程会涉及到几个进程呢？答案是两个，AMS所在进程和应用程序进程。实际上理解了根Activity的启动过程（根Activity的onCreate过程），
根Activity和普通Activity其他生命周期状态比如onStart、onResume等过程也会很轻松的掌握，这些知识点都是触类旁通的，
想要具体了解这些知识点的同学可以自行阅读源码
//todo 普通启动的流程