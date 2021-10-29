http://liuwangshu.cn/framework/wm/1-windowmanager.html  android 7.1.2
WindowManagerService（WMS）和AMS一样，都是Android开发需要掌握的知识点，同样的，WMS也很复杂，需要多篇文章来进行讲解，
为何更好的理解WMS，首先要了解WindowManager，这一篇我们来学习WindowManager体系。

1.Window、WindowManager和WMS
Window我们应该很熟悉，它是一个抽象类，具体的实现类为PhoneWindow，它对View进行管理。 WindowManager是一个接口类，继承自接口ViewManager，
从名称就知道它是用来管理Window的，它的实现类为WindowManagerImpl。如果我们想要对Window进行添加和删除就可以使用WindowManager，
具体的工作都是由WMS来处理的，WindowManager和WMS通过Binder来进行跨进程通信，WMS作为系统服务有很多API是不会暴露给WindowManager的，
这一点与ActivityManager和AMS的关系有些类似。
关于WMS的功能，会在后续文章进行介绍，这里我们只需要知道它的主要功能包括Window管理和输入系统就可以了。这一系列文章的重点是WindowManager。
Window、WindowManager和WMS的关系可以简略的用下图来表示
Window与WindowManager和WMS的关系.png

Window包含了View并对View进行管理，Window用虚线来表示是因为Window是一个抽象概念，并不是真实存在，Window的实体其实也是View。
WindowManager用来管理Window，而WindowManager所提供的功能最终会由WMS来进行处理
//todo setcontentview
//context.getSystemService(Context.WINDOW_SERVICE)
//start activity

2.WindowManager体系
接下来我们从源码角度来分析WindowManager体系以及Window和WindowManager的关系。
WindowManager是一个接口类，继承自接口ViewManager，ViewManager中定义了三个方法，分别用来添加、更新和删除View：
frameworks/base/core/java/android/view/ViewManager.java
```
public interface ViewManager
{
    public void addView(View view, ViewGroup.LayoutParams params);
    public void updateViewLayout(View view, ViewGroup.LayoutParams params);
    public void removeView(View view);
}
```
WindowManager也继承了这些方法，而这些方法传入的参数都是View，说明WindowManager具体管理的是以View形式存在的Window。
WindowManager在继承ViewManager的同时，又加入很多功能，包括Window的类型和层级相关的常量、内部类以及一些方法，
其中有两个方法是根据Window的特性加入的，如下所示
```
public Display getDefaultDisplay();
public void removeViewImmediate(View view);
```


getDefaultDisplay方法会得知这个WindowManager实例将Window添加到哪个屏幕上了，换句话说，就是得到WindowManager所管理的屏幕（Display）。
removeViewImmediate方法则规定在这个方法返回前要立即执行View.onDetachedFromWindow()，来完成传入的View相关的销毁工作。
关于Window的类型和层级会在本系列后续的文章进行介绍。
Window是一个抽象类，它的具体实现类为PhoneWindow。在Activity启动过程中会调用ActivityThread的performLaunchActivity方法，
performLaunchActivity方法中又会调用Activity的attach方法，如果不了解这些请查看Android深入四大组件（一）应用程序启动过程（后篇）这篇文章。
我们从Activity的attach方法开始入手，如下所示。
frameworks/base/core/java/android/app/Activity.java
```
final void attach(Context context, ActivityThread aThread,
            Instrumentation instr, IBinder token, int ident,
            Application application, Intent intent, ActivityInfo info,
            CharSequence title, Activity parent, String id,
            NonConfigurationInstances lastNonConfigurationInstances,
            Configuration config, String referrer, IVoiceInteractor voiceInteractor,
            Window window) {
        attachBaseContext(context);
        mFragments.attachHost(null /*parent*/);
        mWindow = new PhoneWindow(this, window);//1
        ...
         /**
         *2
         */
        mWindow.setWindowManager(
                (WindowManager)context.getSystemService(Context.WINDOW_SERVICE),
                mToken, mComponent.flattenToString(),
                (info.flags & ActivityInfo.FLAG_HARDWARE_ACCELERATED) != 0);
      ...
```

注释1处创建了PhoneWindow，在注释2处调用了PhoneWindow的setWindowManager方法，这个方法的具体的实现在PhoneWindow的父类Window中。
frameworks/base/core/java/android/view/Window.java
```
public void setWindowManager(WindowManager wm, IBinder appToken, String appName,
        boolean hardwareAccelerated) {
    mAppToken = appToken;
    mAppName = appName;
    mHardwareAccelerated = hardwareAccelerated
            || SystemProperties.getBoolean(PROPERTY_HARDWARE_UI, false);
    if (wm == null) {
        wm = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);//1
    }
    mWindowManager = ((WindowManagerImpl)wm).createLocalWindowManager(this);//2
}
```
如果传入的WindowManager为null，就会在注释1处调用Context的getSystemService方法，并传入服务的名称Context.WINDOW_SERVICE（”window”），
具体的实现在ContextImpl中，如下所示。
//todo context.getSystemService
frameworks/base/core/java/android/app/ContextImpl.java
```
@Override
public Object getSystemService(String name) {
    return SystemServiceRegistry.getSystemService(this, name);
}

@Override
public String getSystemServiceName(Class<?> serviceClass) {
    return SystemServiceRegistry.getSystemServiceName(serviceClass);
}
```

最终会调用SystemServiceRegistry的getSystemServiceName方法。
frameworks/base/core/java/android/app/SystemServiceRegistry.java
```
public static String getSystemServiceName(Class<?> serviceClass) {
      return SYSTEM_SERVICE_NAMES.get(serviceClass);
  }
```


SYSTEM_SERVICE_NAMES是一个HashMap类型的数据，它用来存储服务的名称，那么传入的Context.WINDOW_SERVICE到底对应着什么？我们接着往下看。
frameworks/base/core/java/android/app/SystemServiceRegistry.java
```
final class SystemServiceRegistry {
...
 private SystemServiceRegistry() { }
 static {
 ...
   registerService(Context.WINDOW_SERVICE, WindowManager.class,
                new CachedServiceFetcher<WindowManager>() {
            @Override
            public WindowManager createService(ContextImpl ctx) {
                return new WindowManagerImpl(ctx);
            }});
...
 }
}
```

SystemServiceRegistry 的静态代码块中会调用多个registerService方法，这里只列举了和本文有关的一个。
registerService方法会将传入的服务的名称存入到SYSTEM_SERVICE_NAMES中。从上面代码可以看出，传入的Context.WINDOW_SERVICE对应的就是WindowManagerImpl实例，
因此得出结论，Context的getSystemService方法得到的是WindowManagerImpl实例。我们再回到Window的setWindowManager方法，
在注释1处得到WindowManagerImpl实例后转为WindowManager类型，
在注释2处调用了WindowManagerImpl的createLocalWindowManager方法：
frameworks/base/core/java/android/view/WindowManagerImpl
```
public WindowManagerImpl createLocalWindowManager(Window parentWindow) {
       return new WindowManagerImpl(mContext, parentWindow);
   }
```
createLocalWindowManager方法同样也是创建WindowManagerImpl，不同的是这次创建WindowManagerImpl时将创建它的Window作为参数传了进来，
这样WindowManagerImpl就持有了Window的引用，就可以对Window进行操作，比如
在Window中添加View，来查看WindowManagerImpl的addView方法：
frameworks/base/core/java/android/view/WindowManagerImpl
```
@Override
 public void addView(@NonNull View view, @NonNull ViewGroup.LayoutParams params) {
     applyDefaultToken(params);
     mGlobal.addView(view, params, mContext.getDisplay(), mParentWindow);//1
 }
```

//todo 桥接模式
注释1处调用了WindowManagerGlobal的addView方法，其中最后一个参数mParentWindow就是Window，可以看出WindowManagerImpl虽然是WindowManager的实现类，
但是却没有实现什么功能，而是将功能实现委托给了WindowManagerGlobal，这里用到的是桥接模式。关于在Window中添加View，本系列后续的文章会详细介绍。
我们来查看WindowManagerImpl中如何定义的WindowManagerGlobal：
frameworks/base/core/java/android/view/WindowManagerImpl
```
public final class WindowManagerImpl implements WindowManager {
    private final WindowManagerGlobal mGlobal = WindowManagerGlobal.getInstance();
    private final Context mContext;
    private final Window mParentWindow;//1
...
  private WindowManagerImpl(Context context, Window parentWindow) {
        mContext = context;
        mParentWindow = parentWindow;
    }
 ...   
}
```

可以看出WindowManagerGlobal是一个单例，说明在一个进程中只有一个WindowManagerGlobal实例。
注释1处说明WindowManagerImpl可能会实现多个Window，也就是说在一个进程中WindowManagerImpl可能会有多个实例。
通过如上的源码分析，Window和WindowManager的关系如下图所示
Window和WindowManager的关系图.png

PhoneWindow继承自Window，Window通过setWindowManager方法与WindowManager发生关联。WindowManager继承自接口ViewManager，
WindowManagerImpl是WindowManager接口的实现类，但是具体的功能都会委托给WindowManagerGlobal来实现