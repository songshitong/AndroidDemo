http://liuwangshu.cn/framework/context/1-application-context.html android7.0

前言
Context也就是上下文对象，是Android较为常用的类，但是对于Context，很多人都停留在会用的阶段，这个系列会带大家从源码角度来分析Context，
从而更加深入的理解它。
//todo systemserver.getservice


1.Context概述
Context意为上下文或者场景，是一个应用程序环境信息的接口。
在开发中我们经常会使用Context，它的使用场景总的来说分为两大类，它们分别是：

使用Context调用方法，比如：启动Activity、访问资源、调用系统级服务等。
调用方法时传入Context，比如：弹出Toast、创建Dialog等。
Activity、Service和Application都是间接的继承自Context的，因此，我们可以计算出一个应用程序进程中有多少个Context，
 这个数量等于Activity和Service的总个数加1，1指的是Application的数量。

Context是一个抽象类，它的内部定义了很多方法以及静态常量，它的具体实现类为ContextImpl。和Context相关联的类，
除了ContextImpl还有ContextWrapper、ContextThemeWrapper和Activity等等，下面给出Context的关系图
Context的关系图.png

从图中我们可以看出，ContextImpl和ContextWrapper继承自Context，ContextWrapper内部包含有Context类型的mBase对象，
mBase具体指向的是ContextImpl。ContextImpl提供了很多功能，但是外界需要使用并拓展ContextImpl的功能，因此设计上使用了装饰模式，
ContextWrapper是装饰类，它对ContextImpl进行包装，ContextWrapper主要是起了方法传递作用，
ContextWrapper中几乎所有的方法实现都是调用ContextImpl的相应方法来实现的。
ContextThemeWrapper、Service和Application都继承自ContextWrapper，这样他们都可以通过mBase来使用Context的方法，
同时它们也是装饰类，在ContextWrapper的基础上又添加了不同的功能。
ContextThemeWrapper中包含和主题相关的方法（比如： getTheme方法），因此，需要主题的Activity继承ContextThemeWrapper，
而不需要主题的Service则继承ContextWrapper
//todo context相关类的主要内容


Application Context的创建过程
我们通过调用getApplicationContext来获取应用程序的全局的Application Context，那么Application Context是如何创建的呢？
当一个应用程序启动完成后，应用程序就会有一个全局的Application Context。那么我们就从应用程序启动过程开始着手。

在Android深入四大组件（一）应用程序启动过程（后篇）这篇文章的最后讲了ActivityThread启动Activity。ActivityThread作为应用程序进程的核心类，
它会调用它的内部类ApplicationThread的scheduleLaunchActivity方法来启动Activity，如下所示。

frameworks/base/core/java/android/app/ActivityThread.java
```
 private class ApplicationThread extends ApplicationThreadNative {
 ...
   @Override
    public final void scheduleLaunchActivity(Intent intent, IBinder token, int ident,
            ActivityInfo info, Configuration curConfig, Configuration overrideConfig,
            CompatibilityInfo compatInfo, String referrer, IVoiceInteractor voiceInteractor,
            int procState, Bundle state, PersistableBundle persistentState,
            List<ResultInfo> pendingResults, List<ReferrerIntent> pendingNewIntents,
            boolean notResumed, boolean isForward, ProfilerInfo profilerInfo) {
            updateProcessState(procState, false);
            ActivityClientRecord r = new ActivityClientRecord();
            ...
            sendMessage(H.LAUNCH_ACTIVITY, r);
    }
 ...   
}    
```

在ApplicationThread的scheduleLaunchActivity方法中向H类发送LAUNCH_ACTIVITY类型的消息，目的是将启动Activity的逻辑放在主线程中的消息队列中，
这样启动Activity的逻辑会在主线程中执行。我们接着查看H类的handleMessage方法对LAUNCH_ACTIVITY类型的消息的处理。

frameworks/base/core/java/android/app/ActivityThread.java
```
private class H extends Handler {
      public static final int LAUNCH_ACTIVITY         = 100;
...
public void handleMessage(Message msg) {
          if (DEBUG_MESSAGES) Slog.v(TAG, ">>> handling: " + codeToString(msg.what));
          switch (msg.what) {
              case LAUNCH_ACTIVITY: {
                  Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, "activityStart");
                  final ActivityClientRecord r = (ActivityClientRecord) msg.obj;
                  r.packageInfo = getPackageInfoNoCheck(
                          r.activityInfo.applicationInfo, r.compatInfo);//1
                  handleLaunchActivity(r, null, "LAUNCH_ACTIVITY");//2
                  Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
              } break;
            
            ...
}
```

H继承自Handler ，是ActivityThread的内部类。在注释1处通过getPackageInfoNoCheck方法获得LoadedApk类型的对象，
并将该对象赋值给ActivityClientRecord 的成员变量packageInfo，其中LoadedApk用来描述已加载的APK文件。
在注释2处调用handleLaunchActivity方法，如下所示。
frameworks/base/core/java/android/app/ActivityThread.java
```
private void handleLaunchActivity(ActivityClientRecord r, Intent customIntent, String reason) {
    ...
     Activity a = performLaunchActivity(r, customIntent);
    ...
 }
```

我们接着查看performLaunchActivity方法：
frameworks/base/core/java/android/app/ActivityThread.java
```
private Activity performLaunchActivity(ActivityClientRecord r, Intent customIntent) {
...
    try {
        Application app = r.packageInfo.makeApplication(false, mInstrumentation);
       ...
    } 
    ...
    return activity;
}
```

performLaunchActivity方法中有很多重要的逻辑，这里只保留了Application Context相关的逻辑，
想要更多了解performLaunchActivity方法中的逻辑请查看Android深入四大组件（一）应用程序启动过程（后篇）这篇文章的第二小节。
这里ActivityClientRecord 的成员变量packageInfo是LoadedApk类型的，我们接着来查看LoadedApk的makeApplication方法，如下所示。
frameworks/base/core/java/android/app/LoadedApk.java
```
public Application makeApplication(boolean forceDefaultAppClass,
        Instrumentation instrumentation) {
    if (mApplication != null) {//1
        return mApplication;
    }
    ...
    try {
      ...
       java.lang.ClassLoader cl = getClassLoader();
      ...
        ContextImpl appContext = ContextImpl.createAppContext(mActivityThread, this);//2
        app = mActivityThread.mInstrumentation.newApplication(
                cl, appClass, appContext);//3
        appContext.setOuterContext(app);//4
    } catch (Exception e) {
       ...
    }
    mActivityThread.mAllApplications.add(app);
    mApplication = app;//5
    ...
    return app;
}
```

createAppContext
/frameworks/base/core/java/android/app/ContextImpl.java
```
 static ContextImpl createAppContext(ActivityThread mainThread, LoadedApk packageInfo) {
          if (packageInfo == null) throw new IllegalArgumentException("packageInfo");
          return new ContextImpl(null, mainThread,
                  packageInfo, null, null, 0, null, null, Display.INVALID_DISPLAY);
      }
```
注释1处如果mApplication不为null则返回mApplication，这里假设是第一次启动应用程序，因此mApplication为null。
在注释2处通过ContextImpl的createAppContext方法来创建ContextImpl。
注释3处的代码用来创建Application，在Instrumentation的newApplication方法中传入了ClassLoader类型的对象以及注释2处创建的ContextImpl 。
在注释4处将Application赋值给ContextImpl的Context类型的成员变量mOuterContext。
注释5处将Application赋值给LoadedApk的成员变量mApplication，在Application Context的获取过程中我们会再次用到mApplication。
我们来查看注释3处的Application是如何创建的，Instrumentation的newApplication方法如下所示。
frameworks/base/core/java/android/app/Instrumentation.java
```
static public Application newApplication(Class<?> clazz, Context context)
        throws InstantiationException, IllegalAccessException, 
        ClassNotFoundException {
    Application app = (Application)clazz.newInstance();//1
    app.attach(context);
    return app;
}
```

Instrumentation中有两个newApplication重载方法，最终会调用上面这个重载方法。注释1处通过反射来创建Application，
并调用了Application的attach方法，并将ContextImpl传进去：
frameworks/base/core/java/android/app/Application.java
```
/* package */ final void attach(Context context) {
    attachBaseContext(context);
    mLoadedApk = ContextImpl.getImpl(context).mPackageInfo;
}
```

attach方法中调用了attachBaseContext方法，它的实现在Application的父类ContextWrapper中，代码如下所示。
frameworks/base/core/java/android/content/ContextWrapper.java
```
protected void attachBaseContext(Context base) {
     if (mBase != null) {
         throw new IllegalStateException("Base context already set");
     }
     mBase = base;
 }
```

从上文我们得知，这个base指的是ContextImpl，将ContextImpl赋值给ContextWrapper的Context类型的成员变量mBase。
Application Context的创建过程就讲到这里，最后给出Application Context创建过程的时序图
Application Context创建过程.png




Application Context的获取过程
当我们熟知了Application Context的创建过程，那么它的获取过程会非常好理解。我们通过调用getApplicationContext方法来获得Application Context，
getApplicationContext方法的实现在ContextWrapper中，如下所示。
frameworks/base/core/java/android/content/ContextWrapper.java
```
@Override
public Context getApplicationContext() {
    return mBase.getApplicationContext();
}

```

从上文我们得知，mBase指的是ContextImpl，我们来查看 ContextImpl的getApplicationContext方法：
frameworks/base/core/java/android/app/ContextImpl.java
```
@Override
public Context getApplicationContext() {
    return (mPackageInfo != null) ?
            mPackageInfo.getApplication() : mMainThread.getApplication();
}
```

如果LoadedApk不为null，则调用LoadedApk的getApplication方法，否则调用AvtivityThread的getApplication方法。
由于应用程序这时已经启动，因此LoadedApk不会为null，则会调用LoadedApk的getApplication方法：
frameworks/base/core/java/android/app/LoadedApk.java
```
Application getApplication() {
     return mApplication;
 }
```
这里的mApplication我们应该很熟悉，它在上文LoadedApk的makeApplication方法的注释5处被赋值。
这样我们通过getApplicationContext方法就获取到了Application Context