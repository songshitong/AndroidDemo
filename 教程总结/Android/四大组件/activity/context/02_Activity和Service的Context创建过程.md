http://liuwangshu.cn/framework/context/2-activity-service.html  android7.0

这一篇我们接着来学习Activity和Service的Context创建过程。需要注意的是，本篇的知识点会和深入理解四大组件系列的部分文章的知识点相重合

Activity的Context创建过程
当我们在Activity中调用startActivity方法时，其实调用的是Context的startActivity方法，如果想要在Activity中使用Context提供的方法，
务必要先创建Context。Activity的Context会在Activity的启动过程中被创建，在Android深入四大组件（一）应用程序启动过程（后篇）的第二小节中，
讲到了ActivityThread启动Activity的过程，我们就从这里开始分析。
ActivityThread是应用程序进程的核心类，它的内部类ApplicationThread会调用scheduleLaunchActivity方法来启动Activity，
scheduleLaunchActivity方法如下所示
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
        ...
        sendMessage(H.LAUNCH_ACTIVITY, r);
}

```

scheduleLaunchActivity方法会将启动Activity的参数封装成ActivityClientRecord ，sendMessage方法向H类发送类型为LAUNCH_ACTIVITY的消息，
并将ActivityClientRecord 传递过去。sendMessage方法的目的是将启动Activity的逻辑放在主线程中的消息队列中，这样启动Activity的逻辑就会在主线程中执行。
H类的handleMessage方法中会对LAUNCH_ACTIVITY类型的消息进行处理，其中调用了handleLaunchActivity方法，
而handleLaunchActivity方法中又调用performLaunchActivity方法，这一过程在Android深入理解Context（一）Context关联类和Application Context创建过程已经讲过了，
我们来查看performLaunchActivity方法。
frameworks/base/core/java/android/app/ActivityThread.java
```
private Activity performLaunchActivity(ActivityClientRecord r, Intent customIntent) {
      ...
      Activity activity = null;
      try {
          java.lang.ClassLoader cl = r.packageInfo.getClassLoader();
          activity = mInstrumentation.newActivity(
                  cl, component.getClassName(), r.intent);//1
           ...
          }
      } catch (Exception e) {
         ...
      }

      try {
        ...
          if (activity != null) {
              Context appContext = createBaseContextForActivity(r, activity);//2
              ...
              /**
              *3
              */
              activity.attach(appContext, this, getInstrumentation(), r.token,
                      r.ident, app, r.intent, r.activityInfo, title, r.parent,
                      r.embeddedID, r.lastNonConfigurationInstances, config,
                      r.referrer, r.voiceInteractor, window); 
              ...
              if (r.isPersistable()) {
                  mInstrumentation.callActivityOnCreate(activity, r.state, r.persistentState);//4
              } else {
                  mInstrumentation.callActivityOnCreate(activity, r.state);
              }
             ...
      }

      return activity;
  }
```

performLaunchActivity方法中有很多重要的逻辑，这里只保留了Activity的Context相关的逻辑。在注释1处用来创建Activity的实例。
注释2处通过createBaseContextForActivity方法用来创建Activity的ContextImpl，并将ContextImpl传入注释3处的activity的attach方法中。
在注释4处Instrumentation的callActivityOnCreate方法中会调用Activity的onCreate方法。
我们先来查看注释2出的createBaseContextForActivity方法：
frameworks/base/core/java/android/app/ActivityThread.java
```
private Context createBaseContextForActivity(ActivityClientRecord r, final Activity activity) {
      ...
      ContextImpl appContext = ContextImpl.createActivityContext(
              this, r.packageInfo, r.token, displayId, r.overrideConfig);//1
      appContext.setOuterContext(activity);//2
      Context baseContext = appContext;
      ...
      return baseContext;
  }
```

在注释1处调用ContextImpl的createActivityContext方法来创建ContextImpl，注释2处调用了ContextImpl的setOuterContext方法，
将此前创建的Activity 实例赋值给ContextImpl的成员变量mOuterContext，这样ContextImpl也可以访问Activity的变量和方法。
我们再回到ActivityThread的performLaunchActivity方法，查看注释3处的Activity的attach方法，如下所示。
frameworks/base/core/java/android/app/Activity.java
```
final void attach(Context context, ActivityThread aThread,
           Instrumentation instr, IBinder token, int ident,
           Application application, Intent intent, ActivityInfo info,
           CharSequence title, Activity parent, String id,
           NonConfigurationInstances lastNonConfigurationInstances,
           Configuration config, String referrer, IVoiceInteractor voiceInteractor,
           Window window) {
       attachBaseContext(context);//1
       mFragments.attachHost(null /*parent*/);
       mWindow = new PhoneWindow(this, window);//2
       mWindow.setWindowControllerCallback(this);
       mWindow.setCallback(this);//3
       mWindow.setOnWindowDismissedCallback(this);
       ...
       mWindow.setWindowManager(
               (WindowManager)context.getSystemService(Context.WINDOW_SERVICE),
               mToken, mComponent.flattenToString(),
               (info.flags & ActivityInfo.FLAG_HARDWARE_ACCELERATED) != 0);//4
       if (mParent != null) {
           mWindow.setContainer(mParent.getWindow());
       }
       mWindowManager = mWindow.getWindowManager();//5
       mCurrentConfig = config;
   }
```

在注释2处创建PhoneWindow，它代表应用程序窗口。PhoneWindow在运行中会间接触发很多事件，比如点击事件、菜单弹出、屏幕焦点变化等事件，
这些事件需要转发给与PhoneWindow关联的Actvity，转发操作通过Window.Callback接口实现，Actvity实现了这个接口，
在注释3处将当前Activity通过Window的setCallback方法传递给PhoneWindow。
注释4处给PhoneWindow设置WindowManager，并在注释5处获取WindowManager并赋值给Activity的成员变量mWindowManager ，
这样在Activity中就可以通过getWindowManager方法来获取WindowManager。
在注释1处调用了ContextThemeWrapper的attachBaseContext方法，如下所示。

frameworks/base/core/java/android/view/ContextThemeWrapper.java
```
@Override
protected void attachBaseContext(Context newBase) {
    super.attachBaseContext(newBase);
}
```

attachBaseContext方法接着调用ContextThemeWrapper的父类ContextWrapper的attachBaseContext方法：
frameworks/base/core/java/android/content/ContextWrapper.java
```
protected void attachBaseContext(Context base) {
    if (mBase != null) {
        throw new IllegalStateException("Base context already set");
    }
    mBase = base;//1
}
```

注释1处的base指的是一路传递过来的Activity的ContextImpl，将它赋值给ContextWrapper的成员变量mBase。
这样ContextWrapper的功能就可以交由ContextImpl处理，举个例子：
frameworks/base/core/java/android/content/ContextWrapper.java
```
@Override
public Resources.Theme getTheme() {
    return mBase.getTheme();
}
```

当我们调用ContextWrapper的getTheme方法，其实就是调用的ContextImpl的getTheme方法。
Activity的Context创建过程就讲到这里。 总结一下，在启动Activity的过程中创建ContextImpl，并赋值给ContextWrapper的成员变量mBase中。
Activity继承自ContextWrapper的子类ContextThemeWrapper，这样在Activity中就可以使用ContextImpl了。
下面给出ActivityThread到ContextWrapper的调用时序图
ActivityThread到ContextWrapper的调用.png




Service的Context创建过程
Service的Context创建过程与Activity的Context创建过程类似，也是在Service的启动过程中被创建。
在Android深入四大组件（二）Service的启动过程 这篇文章的第二节中讲到了ActivityThread启动Service的过程，我们从这里开始分析。
ActivityThread的内部类ApplicationThread会调用scheduleCreateService方法来启动Service，如下所示。
frameworks/base/core/java/android/app/ActivityThread.java
```
public final void scheduleCreateService(IBinder token,
         ServiceInfo info, CompatibilityInfo compatInfo, int processState) {
     ...
     sendMessage(H.CREATE_SERVICE, s);
 }
```

sendMessage方法向H类发送CREATE_SERVICE类型的消息，H类的handleMessage方法中会对CREATE_SERVICE类型的消息进行处理，
其中调用了handleCreateService方法：
frameworks/base/core/java/android/app/ActivityThread.java
```
private void handleCreateService(CreateServiceData data) {
     ...
       try {
           if (localLOGV) Slog.v(TAG, "Creating service " + data.info.name);
           ContextImpl context = ContextImpl.createAppContext(this, packageInfo);//1
           context.setOuterContext(service);
           Application app = packageInfo.makeApplication(false, mInstrumentation);
           service.attach(context, this, data.info.name, data.token, app,
                   ActivityManagerNative.getDefault());//2
           service.onCreate();
         ...
       } catch (Exception e) {
         ... 
       }
   }
```

在注释1处创建了ContextImpl ，并将该ContextImpl传入注释2处service的attach方法中：
frameworks/base/core/java/android/app/Service.java
```
public final void attach(
           Context context,
           ActivityThread thread, String className, IBinder token,
           Application application, Object activityManager) {
       attachBaseContext(context);//1
       mThread = thread;           // NOTE:  unused - remove?
       mClassName = className;
       mToken = token;
       mApplication = application;
       mActivityManager = (IActivityManager)activityManager;
       mStartCompatibility = getApplicationInfo().targetSdkVersion
               < Build.VERSION_CODES.ECLAIR;
   }
```


注释1处调用了ContextWrapper的attachBaseContext方法。
frameworks/base/core/java/android/content/ContextWrapper.java
```
protected void attachBaseContext(Context base) {
    if (mBase != null) {
        throw new IllegalStateException("Base context already set");
    }
    mBase = base;
}
```
attachBaseContext方法在前文已经讲过，这里不再赘述。
Service的Context创建过程就讲解到这里，由于它和Activity的Context创建过程类似，因此，
可以参考前文给出的ActivityThread到ContextWrapper的调用时序图