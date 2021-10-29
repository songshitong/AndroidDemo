http://liuwangshu.cn/framework/wms/3-wms-remove.html  android 8.0

前言
在本系列文章中，我提到过：Window的操作分为两大部分，一部分是WindowManager处理部分，另一部分是WMS处理部分，Window的删除过程也不例外，
本篇文章会介绍Window的删除过程，包括了两大处理部分的内容。

//todo remove的初始方法
Window的删除过程
和Android解析WindowManagerService（二）WMS的重要成员和Window的添加过程这篇文章中Window的创建和更新过程类似，
要删除Window需要先调用WindowManagerImpl的removeView方法，removeView方法中又会调用WindowManagerGlobal的removeView方法，
我们就从这里开始讲起。为了表述的更易于理解，本文将要删除的Window（View）简称为V。WindowManagerGlobal的removeView方法如下所示
frameworks/base/core/java/android/view/WindowManagerGlobal.java
```
public void removeView(View view, boolean immediate) {
      if (view == null) {
          throw new IllegalArgumentException("view must not be null");
      }
      synchronized (mLock) {
          int index = findViewLocked(view, true);//1
          View curView = mRoots.get(index).getView();
          removeViewLocked(index, immediate);//2
          if (curView == view) {
              return;
          }
          throw new IllegalStateException("Calling with view " + view
                  + " but the ViewAncestor is attached to " + curView);
      }
  }
```

注释1处找到要V在View列表中的索引，在注释2处调用了removeViewLocked方法并将这个索引传进去，如下所示。
frameworks/base/core/java/android/view/WindowManagerGlobal.java
```
private void removeViewLocked(int index, boolean immediate) {
       ViewRootImpl root = mRoots.get(index);//1
       View view = root.getView();
       if (view != null) {
           InputMethodManager imm = InputMethodManager.getInstance();//2
           if (imm != null) {
               imm.windowDismissed(mViews.get(index).getWindowToken());//3
           }
       }
       boolean deferred = root.die(immediate);//4
       if (view != null) {
           view.assignParent(null);
           if (deferred) {
               mDyingViews.add(view);
           }
       }
   }
```

注释1处根据传入的索引在ViewRootImpl列表中获得V的ViewRootImpl。注释2处得到InputMethodManager实例，
如果InputMethodManager实例不为null则在注释3处调用InputMethodManager的windowDismissed方法来结束V的输入法相关的逻辑。
注释4处调用ViewRootImpl 的die方法，如下所示。

frameworks/base/core/java/android/view/ViewRootImpl.java
```
boolean die(boolean immediate) {
      //die方法需要立即执行并且此时ViewRootImpl不在执行performTraversals方法
      if (immediate && !mIsInTraversal) {//1
          doDie();//2
          return false;
      }
      if (!mIsDrawing) {
          destroyHardwareRenderer();
      } else {
          Log.e(mTag, "Attempting to destroy the window while drawing!\n" +
                  "  window=" + this + ", title=" + mWindowAttributes.getTitle());
      }
      mHandler.sendEmptyMessage(MSG_DIE);
      return true;
  }
```
Traversals  遍历    perform  [pəˈfɔːm] 执行，表演
//todo ViewRootImpl以及performTraversals
注释1处如果immediate为ture（需要立即执行），并且mIsInTraversal值为false则执行注释2处的代码，
mIsInTraversal在执行ViewRootImpl的performTraversals方法时会被设置为true，在performTraversals方法执行完时被设置为false，
因此注释1处可以理解为die方法需要立即执行并且此时ViewRootImpl不在执行performTraversals方法。
注释2处的doDie方法如下所示。
frameworks/base/core/java/android/view/ViewRootImpl.java
```
void doDie() {
    //检查执行doDie方法的线程的正确性
    checkThread();//1
    if (LOCAL_LOGV) Log.v(mTag, "DIE in " + this + " of " + mSurface);
    synchronized (this) {
        if (mRemoved) {//2
            return;
        }
        mRemoved = true;//3
        if (mAdded) {//4
            dispatchDetachedFromWindow();//5
        }
        if (mAdded && !mFirst) {//6
            destroyHardwareRenderer();
            if (mView != null) {
                int viewVisibility = mView.getVisibility();
                boolean viewVisibilityChanged = mViewVisibility != viewVisibility;
                if (mWindowAttributesChanged || viewVisibilityChanged) {
                    try {
                        if ((relayoutWindow(mWindowAttributes, viewVisibility, false)
                                & WindowManagerGlobal.RELAYOUT_RES_FIRST_TIME) != 0) {
                            mWindowSession.finishDrawing(mWindow);
                        }
                    } catch (RemoteException e) {
                    }
                }
                mSurface.release();
            }
        }
        mAdded = false;
    }
    WindowManagerGlobal.getInstance().doRemoveView(this);//7
}
```
//todo 检查线程的方法
注释1处用于检查执行doDie方法的线程的正确性，注释1的内部会判断执行doDie方法线程是否是创建V的原始线程，如果不是就会抛出异常，
 这是因为只有创建V的原始线程才能够操作V。
注释2到注释3处的代码用于防止doDie方法被重复调用。
注释4处V有子View就会调用dispatchDetachedFromWindow方法来销毁View。
注释6处如果V有子View并且不是第一次被添加，就会执行后面的代码逻辑。
注释7处的WindowManagerGlobal的doRemoveView方法，如下所示。
frameworks/base/core/java/android/view/WindowManagerGlobal.java
```
void doRemoveView(ViewRootImpl root) {
      synchronized (mLock) {
          final int index = mRoots.indexOf(root);//1
          if (index >= 0) {
              mRoots.remove(index);
              mParams.remove(index);
              final View view = mViews.remove(index);
              mDyingViews.remove(view);
          }
      }
      if (ThreadedRenderer.sTrimForeground && ThreadedRenderer.isAvailable()) {
          doTrimForeground();
      }
  }
```

WindowManagerGlobal中维护了和 Window操作相关的三个列表，doRemoveView方法会从这三个列表中清除V对应的元素。
注释1处找到V对应的ViewRootImpl在ViewRootImpl列表中的索引，接着根据这个索引从ViewRootImpl列表、布局参数列表和View列表中删除与V对应的元素。
我们接着回到ViewRootImpl的doDie方法，查看注释5处的dispatchDetachedFromWindow方法里做了什么：
frameworks/base/core/java/android/view/ViewRootImpl.java
```
void dispatchDetachedFromWindow() {
    ...
      try {
          mWindowSession.remove(mWindow);
      } catch (RemoteException e) {
      }
      ...
  }
```

dispatchDetachedFromWindow方法中主要调用了IWindowSession的remove方法，IWindowSession在Server端的实现为Session，
Session的remove方法如下所示。
frameworks/base/services/core/java/com/android/server/wm/Session.java
```
public void remove(IWindow window) {
     mService.removeWindow(this, window);
 }
```
接着查看WMS的removeWindow方法：
frameworks/base/services/core/java/com/android/server/wm/WindowManagerService.java
```
void removeWindow(Session session, IWindow client) {
     synchronized(mWindowMap) {
         WindowState win = windowForClientLocked(session, client, false);//1
         if (win == null) {
             return;
         }
         win.removeIfPossible();//2
     }
 }
```

注释1处用于获取Window对应的WindowState，WindowState用于保存窗口的信息，在WMS中它用来描述一个窗口。
接着在注释2处调用WindowState的removeIfPossible方法，如下所示。
frameworks/base/services/core/java/com/android/server/wm/WindowState.java
```
Override
void removeIfPossible() {
    super.removeIfPossible();
    removeIfPossible(false /*keepVisibleDeadWindow*/);
}
```

又会调用removeIfPossible方法，如下所示。
frameworks/base/services/core/java/com/android/server/wm/WindowState.java
```
private void removeIfPossible(boolean keepVisibleDeadWindow) {
        ...条件判断过滤，满足其中一个条件就会return，推迟删除操作
	removeImmediately();//1
	if (wasVisible && mService.updateOrientationFromAppTokensLocked(false, displayId)) {
		mService.mH.obtainMessage(SEND_NEW_CONFIGURATION, displayId).sendToTarget();
	}
	mService.updateFocusedWindowLocked(UPDATE_FOCUS_NORMAL, true /*updateInputWindows*/);
	Binder.restoreCallingIdentity(origId);
}
```

removeIfPossible方法和它的名字一样，并不是直接执行删除操作，而是进行多个条件判断过滤，满足其中一个条件就会return，推迟删除操作。
比如这时V正在运行一个动画，这时就得推迟删除操作，直到动画完成。通过这些条件判断过滤就会执行注释1处的removeImmediately方法：
frameworks/base/services/core/java/com/android/server/wm/WindowState.java
```
@Override
void removeImmediately() {
    super.removeImmediately();
    if (mRemoved) {//1
        if (DEBUG_ADD_REMOVE) Slog.v(TAG_WM,
                "WS.removeImmediately: " + this + " Already removed...");
        return;
    }
    mRemoved = true;//2
    ...
    mPolicy.removeWindowLw(this);//3
    disposeInputChannel();
    mWinAnimator.destroyDeferredSurfaceLocked();
    mWinAnimator.destroySurfaceLocked();
    mSession.windowRemovedLocked();//4
    try {
        mClient.asBinder().unlinkToDeath(mDeathRecipient, 0);
    } catch (RuntimeException e) {          
    }
    mService.postWindowRemoveCleanupLocked(this);//5
}
```
//todo 各个方法
removeImmediately方法如同它的名字一样，用于立即进行删除操作。
注释1处的mRemoved为true意味着正在执行删除Window操作，
注释1到注释2处之间的代码用于防止重复删除操作。
注释3处如果当前要删除的Window是StatusBar或者NavigationBar就会将这个Window从对应的控制器中删除。
注释4处会将V对应的Session从WMS的ArraySet<Session> mSessions中删除并清除Session对应的SurfaceSession资源（
SurfaceSession是SurfaceFlinger的一个连接，通过这个连接可以创建1个或者多个Surface并渲染到屏幕上 ）。
注释5处调用了WMS的postWindowRemoveCleanupLocked方法用于对V进行一些集中的清理工作，这里就不在继续深挖下去，
  有兴趣的同学可以自行查看源码
Window的删除过程就讲到这里，虽然删除的操作逻辑比较复杂，但是可以简单的总结为以下4点：

检查删除线程的正确性，如果不正确就抛出异常。
从ViewRootImpl列表、布局参数列表和View列表中删除与V对应的元素。
判断是否可以直接执行删除操作，如果不能就推迟删除操作。
执行删除操作，清理和释放与V相关的一切资源。