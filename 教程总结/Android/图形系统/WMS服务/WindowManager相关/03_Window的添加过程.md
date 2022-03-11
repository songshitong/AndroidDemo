http://liuwangshu.cn/framework/wm/3-add-window.html
1.概述
WindowManager对Window进行管理，说到管理那就离不开对Window的添加、更新和删除的操作，在这里我们把它们统称为Window的操作。
对于Window的操作，最终都是交由WMS来进行处理。窗口的操作分为两大部分，一部分是WindowManager处理部分，另一部分是WMS处理部分。
我们知道Window分为三大类，分别是：Application Window（应用程序窗口）、Sub Windwow（子窗口）和System Window（系统窗口），
对于不同类型的窗口添加过程会有所不同，但是对于WMS处理部分，添加的过程基本上是一样的， WMS对于这三大类的窗口基本是“一视同仁”的
Window的操作结构图.png

本篇主要会讲解Window的操作的WindowManager处理部分，至于WMS处理部分会在后续的解析WMS系列文章中进行讲解

**2.系统窗口的添加过程 **
三大类窗口的添加过程会有所不同，这里以系统窗口StatusBar为例，StatusBar是SystemUI的重要组成部分，具体就是指系统状态栏，
用于显示时间、电量和信号等信息。我们来查看StatusBar的实现类PhoneStatusBar的addStatusBarWindow方法，
这个方法负责为StatusBar添加Window，如下所示。
//todo statusBar相关
frameworks/base/packages/SystemUI/src/com/android/systemui/statusbar/phone/PhoneStatusBar.java
```
 private void addStatusBarWindow() {
    makeStatusBarView();//1
    mStatusBarWindowManager = new StatusBarWindowManager(mContext);
    mRemoteInputController = new RemoteInputController(mStatusBarWindowManager,
            mHeadsUpManager);
    mStatusBarWindowManager.add(mStatusBarWindow, getStatusBarHeight());//2
}
                    
```

注释1处用于构建StatusBar的视图。在注释2处调用了StatusBarWindowManager的add方法，并将StatusBar的视图（StatusBarWindowView）
和StatusBar的传进去。
frameworks/base/packages/SystemUI/src/com/android/systemui/statusbar/phone/StatusBarWindowManager.java
```
public void add(View statusBarView, int barHeight) {
    mLp = new WindowManager.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            barHeight,
            WindowManager.LayoutParams.TYPE_STATUS_BAR,//1
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_TOUCHABLE_WHEN_WAKING
                    | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
                    | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                    | WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
            PixelFormat.TRANSLUCENT);
    mLp.flags |= WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
    mLp.gravity = Gravity.TOP;
    mLp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
    mLp.setTitle("StatusBar");
    mLp.packageName = mContext.getPackageName();
    mStatusBarView = statusBarView;
    mBarHeight = barHeight;
    mWindowManager.addView(mStatusBarView, mLp);//2
    mLpChanged = new WindowManager.LayoutParams();
    mLpChanged.copyFrom(mLp);
}
```

首先通过创建LayoutParams来配置StatusBar视图的属性，包括Width、Height、Type、 Flag、Gravity、SoftInputMode等，
不了Window属性的请查看Android解析WindowManager（二）Window的属性这篇文章。 关键在注释1处，设置了TYPE_STATUS_BAR，
表示StatusBar视图的窗口类型是状态栏。
在注释2处调用了WindowManager的addView方法，addView方法定义在WindowManager的父类接口ViewManager中，
而实现addView方法的则是WindowManagerImpl中，如下所示
frameworks/base/core/java/android/WindowManagerImpl.java
```
@Override
public void addView(@NonNull View view, @NonNull ViewGroup.LayoutParams params) {
    applyDefaultToken(params);
    mGlobal.addView(view, params, mContext.getDisplay(), mParentWindow);
}
```
在WindowManagerImpl的addView方法中，接着会调用WindowManagerGlobal的addView方法：

frameworks/base/core/java/android/view/WindowManagerGlobal.java
```
public void addView(View view, ViewGroup.LayoutParams params,
          Display display, Window parentWindow) {
    ...//参数检查
      final WindowManager.LayoutParams wparams = (WindowManager.LayoutParams) params;
      if (parentWindow != null) {
          parentWindow.adjustLayoutParamsForSubWindow(wparams);//1
      } else {
      ...
      }

      ViewRootImpl root;
      View panelParentView = null;
       ...
          root = new ViewRootImpl(view.getContext(), display);//2
          view.setLayoutParams(wparams);
          mViews.add(view);
          mRoots.add(root);//3
          mParams.add(wparams);
      }
      try {
          root.setView(view, wparams, panelParentView);//4
      } catch (RuntimeException e) {
         ...
      }
  }
```

首先会对参数view、params和display进行检查。注释1处，如果当前窗口要作为子窗口，就会根据父窗口对子窗口的WindowManager.LayoutParams类型的wparams对象进行相应调整。
注释2处创建了ViewRootImp并赋值给root，紧接着在注释3处将root存入到ArrayList<ViewRootImpl>类型的mRoots中，
除了mRoots，mViews和mParams也是ArrayList类型的，分别用于存储窗口的view对象和WindowManager.LayoutParams类型的wparams对象。
注释4处调用了ViewRootImpl的setView方法。
ViewRootImpl身负了很多职责：
View树的根并管理View树
触发View的测量、布局和绘制
输入事件的中转站
管理Surface
负责与WMS进行进程间通信
frameworks/base/core/java/android/view/ViewRootImpl.java
```
public void setView(View view, WindowManager.LayoutParams attrs, View panelParentView) {
       synchronized (this) {
          ...
               try {
                   mOrigWindowType = mWindowAttributes.type;
                   mAttachInfo.mRecomputeGlobalAttributes = true;
                   collectViewAttributes();
                   res = mWindowSession.addToDisplay(mWindow, mSeq, mWindowAttributes,
                           getHostVisibility(), mDisplay.getDisplayId(),
                           mAttachInfo.mContentInsets, mAttachInfo.mStableInsets,
                           mAttachInfo.mOutsets, mInputChannel);
               } 
               ...
   }
```

setView方法中有很多逻辑，这里只截取了一小部分，主要就是调用了mWindowSession的addToDisplay方法。
mWindowSession是IWindowSession类型的，它是一个Binder对象，用于进行进程间通信，IWindowSession是Client端的代理，
它的Server端的实现为Session，此前包含ViewRootImpl在内的代码逻辑都是运行在本地进程的，而Session的addToDisplay方法则运行在WMS所在的进程。
frameworks/base/services/core/java/com/android/server/wm/Session.java
```
@Override
 public int addToDisplay(IWindow window, int seq, WindowManager.LayoutParams attrs,
         int viewVisibility, int displayId, Rect outContentInsets, Rect outStableInsets,
         Rect outOutsets, InputChannel outInputChannel) {
     return mService.addWindow(this, window, seq, attrs, viewVisibility, displayId,
             outContentInsets, outStableInsets, outOutsets, outInputChannel);
 }
```

addToDisplay方法中会调用了WMS的addWindow方法，并将自身也就是Session，作为参数传了进去，每个应用程序进程都会对应一个Session，
WMS会用ArrayList来保存这些Session。这样剩下的工作就交给WMS来处理，在WMS中会为这个添加的窗口分配Surface，并确定窗口显示次序，
可见负责显示界面的是画布Surface，而不是窗口本身。WMS会将它所管理的Surface交由SurfaceFlinger处理，
   SurfaceFlinger会将这些Surface混合并绘制到屏幕上。
窗口添加的WMS处理部分会在后续介绍WMS的系列文章进行讲解，系统窗口的添加过程的时序图如下所示
系统窗口的添加到WMS过程.png





**3.Activity的添加过程 **
无论是哪种窗口，它的的添加过程在WMS处理部分中基本是类似的，只不过会在权限和窗口显示次序等方面会有些不同。
但是在WindowManager处理部分会有所不同，这里以最典型的应用程序窗口Activity为例，Activity在启动过程中，
如果Activity所在的进程不存在则会创建新的进程，创建新的进程之后就会运行代表主线程的实例ActivityThread，
不了解的请查看Android应用程序进程启动过程（前篇）这篇文章。ActivityThread管理着当前应用程序进程的线程，
这在Activity的启动过程中运用的很明显，不了解的请查看Android深入四大组件（一）应用程序启动过程（后篇）这篇文章。
当界面要与用户进行交互时，会调用ActivityThread的handleResumeActivity方法，如下所示。

frameworks/base/core/java/android/app/ActivityThread.java
```
 final void handleResumeActivity(IBinder token,
            boolean clearHide, boolean isForward, boolean reallyResume, int seq, String reason) {
       ...   
         r = performResumeActivity(token, clearHide, reason);//1           
  ...
 if (r.window == null && !a.mFinished && willBeVisible) {
                r.window = r.activity.getWindow();
                View decor = r.window.getDecorView();
                decor.setVisibility(View.INVISIBLE);
                ViewManager wm = a.getWindowManager();//2
                WindowManager.LayoutParams l = r.window.getAttributes();
                a.mDecor = decor;
                l.type = WindowManager.LayoutParams.TYPE_BASE_APPLICATION;
                l.softInputMode |= forwardBit;
                if (r.mPreserveWindow) {
                    a.mWindowAdded = true;
                    r.mPreserveWindow = false;
                    ViewRootImpl impl = decor.getViewRootImpl();
                    if (impl != null) {
                        impl.notifyChildRebuilt();
                    }
                }
                if (a.mVisibleFromClient && !a.mWindowAdded) {
                    a.mWindowAdded = true;
                    wm.addView(decor, l);//3
                }
...                
}
```

注释1处的performResumeActivity方法最终会调用Activity的onResume方法。在注释2处得到ViewManager类型的wm对象，
在注释3处调用了wm的addView方法，而addView方法的实现则是在WindowManagerImpl中，此后的过程在上面的系统窗口的添加过程已经讲过，
唯一需要注意的是addView的第一个参数是DecorView

结语
ViewManager不只定义了addView方法用来添加窗口，还定义了updateViewLayout和removeView方法用来更新和删除窗口，如下所示。
```
package android.view;
public interface ViewManager
{
    public void addView(View view, ViewGroup.LayoutParams params);
    public void updateViewLayout(View view, ViewGroup.LayoutParams params);
    public void removeView(View view);
}
```

其定义的updateViewLayout和removeView方法的处理流程和addView方法是类似的，都是要经过WindowManagerGlobal处理，
最后通过Session与WMS进行跨进程通信，将更新和删除窗口的工作交由WMS来处理，这里不会对其进行介绍，
想了解可以查看源码或者查看《Android开发艺术探索》第八章