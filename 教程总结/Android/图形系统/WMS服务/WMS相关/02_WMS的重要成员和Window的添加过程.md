http://liuwangshu.cn/framework/wms/2-wms-member.html  android8.0
前言
在本系列的上一篇文章中，我们学习了WMS的诞生，WMS被创建后，它的重要的成员有哪些？Window添加过程的WMS部分做了什么呢？
这篇文章会给你解答

1.WMS的重要成员
所谓WMS的重要成员是指WMS中的重要的成员变量，如下所示。
frameworks/base/services/core/java/com/android/server/wm/WindowManagerService.java
```
final WindowManagerPolicy mPolicy;
final IActivityManager mActivityManager;
final ActivityManagerInternal mAmInternal;
final AppOpsManager mAppOps;
final DisplaySettings mDisplaySettings;
...
final ArraySet<Session> mSessions = new ArraySet<>();
final WindowHashMap mWindowMap = new WindowHashMap();
final ArrayList<AppWindowToken> mFinishedStarting = new ArrayList<>();
final ArrayList<AppWindowToken> mFinishedEarlyAnim = new ArrayList<>();
final ArrayList<AppWindowToken> mWindowReplacementTimeouts = new ArrayList<>();
final ArrayList<WindowState> mResizingWindows = new ArrayList<>();
final ArrayList<WindowState> mPendingRemove = new ArrayList<>();
WindowState[] mPendingRemoveTmp = new WindowState[20];
final ArrayList<WindowState> mDestroySurface = new ArrayList<>();
final ArrayList<WindowState> mDestroyPreservedSurface = new ArrayList<>();
...
final H mH = new H();
...
final WindowAnimator mAnimator;
...
 final InputManagerService mInputManager
```


这里列出了WMS的部分成员变量，下面分别对它们进行简单的介绍。

mPolicy：WindowManagerPolicy
WindowManagerPolicy（WMP）类型的变量。WindowManagerPolicy是窗口管理策略的接口类，用来定义一个窗口策略所要遵循的通用规范，
并提供了WindowManager所有的特定的UI行为。它的具体实现类为PhoneWindowManager，这个实现类在WMS创建时被创建。
WMP允许定制窗口层级和特殊窗口类型以及关键的调度和布局

mSessions：ArraySet
ArraySet类型的变量，元素类型为Session。在Android解析WindowManager（三）Window的添加过程这篇文章中我提到过Session，
它主要用于进程间通信，其他的应用程序进程想要和WMS进程进行通信就需要经过Session，并且每个应用程序进程都会对应一个Session，
WMS保存这些Session用来记录所有向WMS提出窗口管理服务的客户端。

mWindowMap：WindowHashMap
WindowHashMap类型的变量，WindowHashMap继承了HashMap，它限制了HashMap的key值的类型为IBinder，value值的类型为WindowState。
WindowState用于保存窗口的信息，在WMS中它用来描述一个窗口。综上得出结论，mWindowMap就是用来保存WMS中各种窗口的集合。

mFinishedStarting：ArrayList
ArrayList类型的变量，元素类型为AppWindowToken，它是WindowToken的子类。要想理解mFinishedStarting的含义，需要先了解WindowToken是什么。
WindowToken主要有两个作用：
  可以理解为窗口令牌，当应用程序想要向WMS申请新创建一个窗口，则需要向WMS出示有效的WindowToken。
     AppWindowToken作为WindowToken的子类，主要用来描述应用程序的WindowToken结构，应用程序中每个Activity都对应一个AppWindowToken。
  WindowToken会将相同组件（比如Acitivity）的窗口（WindowState）集合在一起，方便管理。

mFinishedStarting就是用于存储已经完成启动的应用程序窗口（比如Acitivity）的AppWindowToken的列表。
除了mFinishedStarting，还有类似的mFinishedEarlyAnim和mWindowReplacementTimeouts，
  其中mFinishedEarlyAnim存储了已经完成窗口绘制并且不需要展示任何已保存surface的应用程序窗口的AppWindowToken。
  mWindowReplacementTimeout存储了等待更换的应用程序窗口的AppWindowToken，如果更换不及时，旧窗口就需要被处理

mResizingWindows：ArrayList
ArrayList类型的变量，元素类型为WindowState。
mResizingWindows是用来存储正在调整大小的窗口的列表。与mResizingWindows类似的还有mPendingRemove、
mDestroySurface和mDestroyPreservedSurface等等。其中mPendingRemove是在内存耗尽时设置的，里面存有需要强制删除的窗口。
mDestroySurface里面存有需要被Destroy的Surface。mDestroyPreservedSurface里面存有窗口需要保存的等待销毁的Surface，
为什么窗口要保存这些Surface？这是因为当窗口经历Surface变化时，窗口需要一直保持旧Surface，直到新Surface的第一帧绘制完成。

mAnimator：WindowAnimator
WindowAnimator类型的变量，用于管理窗口的动画以及特效动画。

mH：H
H类型的变量，系统的Handler类，用于将任务加入到主线程的消息队列中，这样代码逻辑就会在主线程中执行。

mInputManager：InputManagerService
InputManagerService类型的变量，输入系统的管理者。InputManagerService（IMS）会对触摸事件进行处理，
  它会寻找一个最合适的窗口来处理触摸反馈信息，WMS是窗口的管理者，因此，WMS“理所应当”的成为了输入系统的中转站，
  WMS包含了IMS的引用不足为怪。


2.Window的添加过程（WMS部分）
我们知道Window的操作分为两大部分，一部分是WindowManager处理部分，另一部分是WMS处理部分，如下所示。
在Android解析WindowManager（三）Window的添加过程这篇文章中，我讲解了Window的添加过程的WindowManager处理部分，
这一篇文章我们接着来学习Window的添加过程的WMS部分。
无论是系统窗口还是Activity，它们的Window的添加过程都会调用WMS的addWindow方法，由于这个方法代码逻辑比较多，这里分为3个部分来阅读。
//todo 之前是在addToDisplay 怎么到的WMS的addWindow
frameworks/base/services/core/java/com/android/server/wm/WindowManagerService.java

addWindow方法part1
```
 public int addWindow(Session session, IWindow client, int seq,
            WindowManager.LayoutParams attrs, int viewVisibility, int displayId,
            Rect outContentInsets, Rect outStableInsets, Rect outOutsets,
            InputChannel outInputChannel) {

        int[] appOp = new int[1];
        int res = mPolicy.checkAddPermission(attrs, appOp);//1
        if (res != WindowManagerGlobal.ADD_OKAY) {
            return res;
        }
        ...
        synchronized(mWindowMap) {
            if (!mDisplayReady) {
                throw new IllegalStateException("Display has not been initialialized");
            }
            final DisplayContent displayContent = mRoot.getDisplayContentOrCreate(displayId);//2
            if (displayContent == null) {
                Slog.w(TAG_WM, "Attempted to add window to a display that does not exist: "
                        + displayId + ".  Aborting.");
                return WindowManagerGlobal.ADD_INVALID_DISPLAY;
            }
            ...
            if (type >= FIRST_SUB_WINDOW && type <= LAST_SUB_WINDOW) {//3
                parentWindow = windowForClientLocked(null, attrs.token, false);//4
                if (parentWindow == null) {
                    Slog.w(TAG_WM, "Attempted to add window with token that is not a window: "
                          + attrs.token + ".  Aborting.");
                    return WindowManagerGlobal.ADD_BAD_SUBWINDOW_TOKEN;
                }
                if (parentWindow.mAttrs.type >= FIRST_SUB_WINDOW
                        && parentWindow.mAttrs.type <= LAST_SUB_WINDOW) {
                    Slog.w(TAG_WM, "Attempted to add window with token that is a sub-window: "
                            + attrs.token + ".  Aborting.");
                    return WindowManagerGlobal.ADD_BAD_SUBWINDOW_TOKEN;
                }
            }
           ...
}
...
}
```

WMS的addWindow返回的是addWindow的各种状态，比如添加Window成功，无效的display等等，这些状态被定义在WindowManagerGlobal中。
注释1处根据Window的属性，调用WMP的checkAddPermission方法来检查权限，具体的实现在PhoneWindowManager的checkAddPermission方法中，
  如果没有权限则不会执行后续的代码逻辑。
注释2处通过displayId来获得窗口要添加到哪个DisplayContent上，如果没有找到DisplayContent，
   则返回WindowManagerGlobal.ADD_INVALID_DISPLAY这一状态，其中DisplayContent用来描述一块屏幕。
注释3处，type代表一个窗口的类型，它的数值介于FIRST_SUB_WINDOW和LAST_SUB_WINDOW之间（1000~1999），这个数值定义在WindowManager中，
  说明这个窗口是一个子窗口，不了解窗口类型取值范围的请阅读Android解析WindowManager（二）Window的属性这篇文章。
注释4处，attrs.token是IBinder类型的对象，windowForClientLocked方法内部会根据attrs.token作为key值从mWindowMap中得到该子窗口的父窗口。
  接着对父窗口进行判断，如果父窗口为null或者type的取值范围不正确则会返回错误的状态。

addWindow方法part2
```
         ...
         AppWindowToken atoken = null;
         final boolean hasParent = parentWindow != null;
         WindowToken token = displayContent.getWindowToken(
                 hasParent ? parentWindow.mAttrs.token : attrs.token);//1
         final int rootType = hasParent ? parentWindow.mAttrs.type : type;//2
         boolean addToastWindowRequiresToken = false;

         if (token == null) {
             if (rootType >= FIRST_APPLICATION_WINDOW && rootType <= LAST_APPLICATION_WINDOW) {
                 Slog.w(TAG_WM, "Attempted to add application window with unknown token "
                       + attrs.token + ".  Aborting.");
                 return WindowManagerGlobal.ADD_BAD_APP_TOKEN;
             }
             if (rootType == TYPE_INPUT_METHOD) {
                 Slog.w(TAG_WM, "Attempted to add input method window with unknown token "
                       + attrs.token + ".  Aborting.");
                 return WindowManagerGlobal.ADD_BAD_APP_TOKEN;
             }
             if (rootType == TYPE_VOICE_INTERACTION) {
                 Slog.w(TAG_WM, "Attempted to add voice interaction window with unknown token "
                       + attrs.token + ".  Aborting.");
                 return WindowManagerGlobal.ADD_BAD_APP_TOKEN;
             }
             if (rootType == TYPE_WALLPAPER) {
                 Slog.w(TAG_WM, "Attempted to add wallpaper window with unknown token "
                       + attrs.token + ".  Aborting.");
                 return WindowManagerGlobal.ADD_BAD_APP_TOKEN;
             }
             ...
             if (type == TYPE_TOAST) {
                 // Apps targeting SDK above N MR1 cannot arbitrary add toast windows.
                 if (doesAddToastWindowRequireToken(attrs.packageName, callingUid,
                         parentWindow)) {
                     Slog.w(TAG_WM, "Attempted to add a toast window with unknown token "
                             + attrs.token + ".  Aborting.");
                     return WindowManagerGlobal.ADD_BAD_APP_TOKEN;
                 }
             }
             final IBinder binder = attrs.token != null ? attrs.token : client.asBinder();
             token = new WindowToken(this, binder, type, false, displayContent,
                     session.mCanAddInternalSystemWindow);//3
         } else if (rootType >= FIRST_APPLICATION_WINDOW && rootType <= LAST_APPLICATION_WINDOW) {//4
             atoken = token.asAppWindowToken();//5
             if (atoken == null) {
                 Slog.w(TAG_WM, "Attempted to add window with non-application token "
                       + token + ".  Aborting.");
                 return WindowManagerGlobal.ADD_NOT_APP_TOKEN;
             } else if (atoken.removed) {
                 Slog.w(TAG_WM, "Attempted to add window with exiting application token "
                       + token + ".  Aborting.");
                 return WindowManagerGlobal.ADD_APP_EXITING;
             }
         } else if (rootType == TYPE_INPUT_METHOD) {
             if (token.windowType != TYPE_INPUT_METHOD) {
                 Slog.w(TAG_WM, "Attempted to add input method window with bad token "
                         + attrs.token + ".  Aborting.");
                   return WindowManagerGlobal.ADD_BAD_APP_TOKEN;
             }
         }
   ...      
```
注释1处通过displayContent的getWindowToken方法来得到WindowToken。
注释2处，如果有父窗口就将父窗口的type值赋值给rootType，如果没有将当前窗口的type值赋值给rootType。接下来如果WindowToken为null，
  则根据rootType或者type的值进行区分判断，如果rootType值等于TYPE_INPUT_METHOD、TYPE_WALLPAPER等值时，
  则返回状态值WindowManagerGlobal.ADD_BAD_APP_TOKEN，说明rootType值等于TYPE_INPUT_METHOD、TYPE_WALLPAPER等值时
  是不允许WindowToken为null的。
通过多次的条件判断筛选，最后会在注释3处隐式创建WindowToken，这说明当我们添加窗口时是可以不向WMS提供WindowToken的，
  前提是rootType和type的值不为前面条件判断筛选的值。WindowToken隐式和显式的创建肯定是要加以区分的，
注释3处的第4个参数为false就代表这个WindowToken是隐式创建的。
接下来的代码逻辑就是WindowToken不为null的情况，根据rootType和type的值进行判断，
  比如在注释4处判断如果窗口为应用程序窗口，在注释5处会将WindowToken转换为专门针对应用程序窗口的AppWindowToken，
  然后根据AppWindowToken的值进行后续的判断。

addWindow方法part3
```
 ...
final WindowState win = new WindowState(this, session, client, token, parentWindow,
                  appOp[0], seq, attrs, viewVisibility, session.mUid,
                  session.mCanAddInternalSystemWindow);//1
          if (win.mDeathRecipient == null) {//2
              // Client has apparently died, so there is no reason to
              // continue.
              Slog.w(TAG_WM, "Adding window client " + client.asBinder()
                      + " that is dead, aborting.");
              return WindowManagerGlobal.ADD_APP_EXITING;
          }

          if (win.getDisplayContent() == null) {//3
              Slog.w(TAG_WM, "Adding window to Display that has been removed.");
              return WindowManagerGlobal.ADD_INVALID_DISPLAY;
          }

          mPolicy.adjustWindowParamsLw(win.mAttrs);//4
          win.setShowToOwnerOnlyLocked(mPolicy.checkShowToOwnerOnly(attrs));
          res = mPolicy.prepareAddWindowLw(win, attrs);//5
          ...
          win.attach();
          mWindowMap.put(client.asBinder(), win);//6
          if (win.mAppOp != AppOpsManager.OP_NONE) {
              int startOpResult = mAppOps.startOpNoThrow(win.mAppOp, win.getOwningUid(),
                      win.getOwningPackage());
              if ((startOpResult != AppOpsManager.MODE_ALLOWED) &&
                      (startOpResult != AppOpsManager.MODE_DEFAULT)) {
                  win.setAppOpVisibilityLw(false);
              }
          }

          final AppWindowToken aToken = token.asAppWindowToken();
          if (type == TYPE_APPLICATION_STARTING && aToken != null) {
              aToken.startingWindow = win;
              if (DEBUG_STARTING_WINDOW) Slog.v (TAG_WM, "addWindow: " + aToken
                      + " startingWindow=" + win);
          }

          boolean imMayMove = true;
          win.mToken.addWindow(win);//7
           if (type == TYPE_INPUT_METHOD) {
              win.mGivenInsetsPending = true;
              setInputMethodWindowLocked(win);
              imMayMove = false;
          } else if (type == TYPE_INPUT_METHOD_DIALOG) {
              displayContent.computeImeTarget(true /* updateImeTarget */);
              imMayMove = false;
          } else {
              if (type == TYPE_WALLPAPER) {
                  displayContent.mWallpaperController.clearLastWallpaperTimeoutTime();
                  displayContent.pendingLayoutChanges |= FINISH_LAYOUT_REDO_WALLPAPER;
              } else if ((attrs.flags&FLAG_SHOW_WALLPAPER) != 0) {
                  displayContent.pendingLayoutChanges |= FINISH_LAYOUT_REDO_WALLPAPER;
              } else if (displayContent.mWallpaperController.isBelowWallpaperTarget(win)) {
                  displayContent.pendingLayoutChanges |= FINISH_LAYOUT_REDO_WALLPAPER;
              }
          }
       ...
```

在注释1处创建了WindowState，它存有窗口的所有的状态信息，在WMS中它代表一个窗口。从WindowState传入的参数，
  可以发现WindowState中包含了WMS、Session、WindowToken、父类的WindowState、LayoutParams等信息。
紧接着在注释2和3处分别判断请求添加窗口的客户端是否已经死亡、窗口的DisplayContent是否为null，如果是则不会再执行下面的代码逻辑。
注释4处调用了WMP的adjustWindowParamsLw方法，该方法的实现在PhoneWindowManager中，会根据窗口的type对窗口的LayoutParams的一些成员变量进行修改。
注释5处调用WMP的prepareAddWindowLw方法，用于准备将窗口添加到系统中。
注释6处将WindowState添加到mWindowMap中。
注释7处将WindowState添加到该WindowState对应的WindowToken中(实际是保存在WindowToken的父类WindowContainer中)，
  这样WindowToken就包含了相同组件的WindowState


addWindow方法总结
addWindow方法分了3个部分来进行讲解，主要就是做了下面4件事：
对所要添加的窗口进行检查，如果窗口不满足一些条件，就不会再执行下面的代码逻辑。
WindowToken相关的处理，比如有的窗口类型需要提供WindowToken，没有提供的话就不会执行下面的代码逻辑，
   有的窗口类型则需要由WMS隐式创建WindowToken。
WindowState的创建和相关处理，将WindowToken和WindowState相关联。
创建和配置DisplayContent，完成窗口添加到系统前的准备工作


结语
在本篇文章中我们首先学习了WMS的重要成员，了解这些成员有利于对WMS的进一步分析。接下来我们又学习了Window的添加过程的WMS部分，
将addWindow方法分为了3个部分来进行讲解，从addWindow方法我们得知WMS有3个重要的类分别是WindowToken、
WindowState和DisplayContent，关于它们会在本系列后续的文章中进行介绍

todo DisplayContent用来描述一块屏幕