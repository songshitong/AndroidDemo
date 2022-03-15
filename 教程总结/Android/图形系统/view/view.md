android8.1

invalidate
/frameworks/base/core/java/android/view/View.java
```
public void invalidate() {
          invalidate(true);
 }
  public void invalidate(boolean invalidateCache) {
          //传入当前view的范围
          invalidateInternal(0, 0, mRight - mLeft, mBottom - mTop, invalidateCache, true);
      }
  
void invalidateInternal(int l, int t, int r, int b, boolean invalidateCache,
              boolean fullInvalidate) {
  if ((mPrivateFlags & (PFLAG_DRAWN | PFLAG_HAS_BOUNDS)) == (PFLAG_DRAWN | PFLAG_HAS_BOUNDS)
                  || (invalidateCache && (mPrivateFlags & PFLAG_DRAWING_CACHE_VALID) == PFLAG_DRAWING_CACHE_VALID)
                  || (mPrivateFlags & PFLAG_INVALIDATED) != PFLAG_INVALIDATED
                  || (fullInvalidate && isOpaque() != mLastIsOpaque)) {
      if (fullInvalidate) {
          mLastIsOpaque = isOpaque();
          mPrivateFlags &= ~PFLAG_DRAWN;
      }
      //给当前view 添加脏标记
      mPrivateFlags |= PFLAG_DIRTY;
      final AttachInfo ai = mAttachInfo;
      final ViewParent p = mParent;
      //调用ViewParent的invalidateChild
      if (p != null && ai != null && l < r && t < b) {
          final Rect damage = ai.mTmpInvalRect;
          damage.set(l, t, r, b);
          p.invalidateChild(this, damage);
      }

  }             
}
```
//todo view 的parent如何指向viewGroup的   https://juejin.cn/post/6844903781713313806#heading-8
/frameworks/base/core/java/android/view/ViewGroup.java
```
public final void invalidateChild(View child, final Rect dirty) {
  ViewParent parent = this;
  if (attachInfo != null) {
     do {
            View view = null;
            if (parent instanceof View) {
                view = (View) parent;
            }
            ...
            //循环找到根view，并调用invalidateChildInParent()方法
            parent = parent.invalidateChildInParent(location, dirty);
            if (view != null) {
                ...处理flasgs，动画,矩阵等
            }
        } while (parent != null);
  }
}
//将自己的parent返回
public ViewParent invalidateChildInParent(final int[] location, final Rect dir
    if ((mPrivateFlags & (PFLAG_DRAWN | PFLAG_DRAWING_CACHE_VALID)) != 0) {
        ...
        return mParent; 
    }
    return null;
}

```
DecorView的parent是RootViewImpl
/frameworks/base/core/java/android/view/ViewRootImpl.java
```
public ViewParent invalidateChildInParent(int[] location, Rect dirty) {
    checkThread();//划重点...这里检查线程。子线程不能更新UI
    if (dirty == null) {
        invalidate();
        return null;
    } else if (dirty.isEmpty() && !mIsAnimating) {
        return null;
    }
    ...
invalidateRectOnScreen(dirty);
    return null;
}

 private void invalidateRectOnScreen(Rect dirty) {
          final Rect localDirty = mDirty;
          if (!localDirty.isEmpty() && !localDirty.contains(dirty)) {
          // 将dirty加入localDirty中  view的重绘范围
          localDirty.union(dirty.left, dirty.top, dirty.right, dirty.bottom);
          final float appScale = mAttachInfo.mApplicationScale;
          final boolean intersected = localDirty.intersect(0, 0,
                  (int) (mWidth * appScale + 0.5f), (int) (mHeight * appScale + 0.5f));
          if (!intersected) {
              localDirty.setEmpty();
          }
          if (!mWillDrawSoon && (intersected || mIsAnimating)) {
              //计划遍历
              scheduleTraversals();
          }
      }
 void scheduleTraversals() {
      if (!mTraversalScheduled) {
          mTraversalScheduled = true;
          mTraversalBarrier = mHandler.getLooper().getQueue().postSyncBarrier();
          mChoreographer.postCallback(
                  Choreographer.CALLBACK_TRAVERSAL, mTraversalRunnable, null);
          if (!mUnbufferedInputDispatch) {
              scheduleConsumeBatchedInput();
          }
          notifyRendererOfFramePending();
          pokeDrawLockIfNeeded();
      }
  }  
  final TraversalRunnable mTraversalRunnable = new TraversalRunnable();     

  final class TraversalRunnable implements Runnable {
          @Override
          public void run() {
              doTraversal();
          }
      }
 void doTraversal() {
          if (mTraversalScheduled) {
              mTraversalScheduled = false;
              mHandler.getLooper().getQueue().removeSyncBarrier(mTraversalBarrier);
              performTraversals();
          }
      }   
      
 private void performTraversals() {
    ...
    performDraw();
    ...
 }          
```
有个dirty区域，重绘dirty区域，而且invalidate并不会引起measure，layout，draw的全流程，需要调用requestLayout才行 



postInvalidate 相关
/frameworks/base/core/java/android/view/View.java
```
 public void postInvalidate() {
          postInvalidateDelayed(0);
      }
      
 public void postInvalidateDelayed(long delayMilliseconds) {
          final AttachInfo attachInfo = mAttachInfo;
          if (attachInfo != null) {
              attachInfo.mViewRootImpl.dispatchInvalidateDelayed(this, delayMilliseconds);
          }
      }     
```

/frameworks/base/core/java/android/view/ViewRootImpl.java
```
 public void dispatchInvalidateDelayed(View view, long delayMilliseconds) {
          Message msg = mHandler.obtainMessage(MSG_INVALIDATE, view);
          mHandler.sendMessageDelayed(msg, delayMilliseconds);
 }  
 final ViewRootHandler mHandler = new ViewRootHandler();
 final class ViewRootHandler extends Handler {
    public void handleMessage(Message msg) {
              switch (msg.what) {
              case MSG_INVALIDATE:
                  ((View) msg.obj).invalidate();
                  break;
          ....        
   }
 }
```
invalidate与postInvalidate   postInvalidate增加主线程切换
主线程用invalidate，在子线程可以用postInvalidate



https://ljd1996.github.io/2020/09/11/Android-View%E5%8E%9F%E7%90%86%E4%B8%8E%E5%AE%9E%E8%B7%B5/
View.post
post的实现分为两种
1 通过attachInfo的handler
2 通过RunQueue
2 通过RunQueue
//todo 由Android-Window机制原理可知，mAttachInfo的赋值在AT.handleResumeActivity后调用dispatchAttachedToWindow方法，
因此在Activity.onCreate时，mAttachInfo为null，因此可以接下来看看getRunQueue方法：
/frameworks/base/core/java/android/view/View.java
```
public boolean post(Runnable action) {
      final AttachInfo attachInfo = mAttachInfo;
      if (attachInfo != null) {
          return attachInfo.mHandler.post(action);
      }
      //将action添加到mRunQueue
      getRunQueue().post(action);
      return true;
  }
private HandlerActionQueue getRunQueue() {
      if (mRunQueue == null) {
          mRunQueue = new HandlerActionQueue();
      }
      return mRunQueue;
  }  
```
/frameworks/base/core/java/android/view/HandlerActionQueue.java
```
public class HandlerActionQueue {
    private HandlerAction[] mActions;
    private int mCount;

    public void post(Runnable action) {
        postDelayed(action, 0);
    }
    public void postDelayed(Runnable action, long delayMillis) {
        final HandlerAction handlerAction = new HandlerAction(action, delayMillis);
        synchronized (this) {
            if (mActions == null) {
                // 初始化一个具有4个元素的数组
                mActions = new HandlerAction[4];
            }
            // 将handlerAction添加到mActions中
            mActions = GrowingArrayUtils.append(mActions, mCount, handlerAction);
            mCount++;
        }
    }
     //使用外部handler执行所有的action
      public void executeActions(Handler handler) {
          synchronized (this) {
              final HandlerAction[] actions = mActions;
              for (int i = 0, count = mCount; i < count; i++) {
                  final HandlerAction handlerAction = actions[i];
                  handler.postDelayed(handlerAction.action, handlerAction.delay);
              } 
              mActions = null;
              mCount = 0;
          }
      }
    // ...
    private static class HandlerAction {
        final Runnable action;
        final long delay;

        public HandlerAction(Runnable action, long delay) {
            this.action = action;
            this.delay = delay;
        }
        public boolean matches(Runnable otherAction) {
            return otherAction == null && action == null
                    || action != null && action.equals(otherAction);
        }
    }
}
```
执行对应的action
```
void dispatchAttachedToWindow(AttachInfo info, int visibility) {
    mAttachInfo = info;
        ···
        //执行对应的action并将mRunQueue置为null
        if (mRunQueue != null) {
            mRunQueue.executeActions(info.mHandler);
            mRunQueue = null;
        }
        ···
}
```
view.dispatchAttachedToWindow的调用时机是在RootViewImpl.performTraversals
/frameworks/base/core/java/android/view/ViewRootImpl.java
```
private void performTraversals() {
  if(mFirst){
    host.dispatchAttachedToWindow(mAttachInfo, 0);
  }
}
```
mActions 中的所有任务都会被插入到 mHandler 的 MessageQueue 中，等到主线程执行完 performTraversals() 方法后
就会来执行 mActions，所以此时我们依然可以获取到 View 的真实宽高

2 通过attachInfo的handler   https://juejin.cn/post/6939763855216082974
view中attachInfo的初始化
ViewRootImpl 的performTraversals()方法就会调用 DecorView 的 dispatchAttachedToWindow 方法并传入 mAttachInfo，
从而层层调用整个视图树中所有 View 的 dispatchAttachedToWindow 方法，使得所有 childView 都能获取到 mAttachInfo 对象
```
 final ViewRootHandler mHandler = new ViewRootHandler();

    public ViewRootImpl(Context context, Display display, IWindowSession session,
                        boolean useSfChoreographer) {
        ···
        mAttachInfo = new View.AttachInfo(mWindowSession, mWindow, display, this, mHandler, this,
                context);
        ···
    }

    private void performTraversals() {
        ···
        if (mFirst) {
            ···
            host.dispatchAttachedToWindow(mAttachInfo, 0);
    	    ···
        }
        ···
        performMeasure(childWidthMeasureSpec, childHeightMeasureSpec);
        performLayout(lp, mWidth, mHeight);
        performDraw();
        ···
    }

```
performTraversals()方法也负责启动整个视图树的 Measure、Layout、Draw 流程，只有当 performLayout 被调用后 View 才能确定自己的宽高信息。
而 performTraversals()本身也是交由 ViewRootHandler 来调用的，即整个视图树的绘制任务也是先插入到 MessageQueue 中，
后续再由主线程取出任务进行执行。由于插入到 MessageQueue 中的消息是交由主线程来顺序执行的，
所以 attachInfo.mHandler.post(action)就保证了 action 一定是在 performTraversals 执行完毕后才会被调用，
因此我们就可以在 Runnable 中获取到 View 的真实宽高了

ViewRootHandler 先执行performTraversals，触发view.dispatchAttachedToWindow,存在attachInfo,通过AttachInfo.mhandler
的post到主线程

有时候View的绘制工作并不是依次能够成功的，因此可能会重新发消息到主线程再执行绘制工作，这会导致View的宽或高以及位置发生变化，
这个消息就会排在View.post()发送的消息的后边，因此View.post()获取的宽高可能不正确

onCreate 方法直接使用 Handler.post()，是获取不到宽高的，因为该任务在 View 绘制任务之前（同一个线程队列机制）

post 兼容性  android6.0
frameworks/base/core/java/android/view/View.java
```
 public boolean post(Runnable action) {
        final AttachInfo attachInfo = mAttachInfo;
        if (attachInfo != null) {
            return attachInfo.mHandler.post(action);
        }
        ViewRootImpl.getRunQueue().post(action);
        return true;
    }
```
frameworks/base/core/java/android/view/ViewRootImpl.java   
```
static final ThreadLocal<RunQueue> sRunQueues = new ThreadLocal<RunQueue>();
static RunQueue getRunQueue() {
        RunQueue rq = sRunQueues.get();
        if (rq != null) {
            return rq;
        }
        rq = new RunQueue();
        sRunQueues.set(rq);
        return rq;
    }
```
//新版已经弃用sRunQueues  android12
```
 @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
       static final ThreadLocal<HandlerActionQueue> sRunQueues = new ThreadLocal<HandlerActionQueue>();
```
在 Android API 23 及之前的版本上，当 attachInfo 为 null 时，会将 Runnable 保存到 ViewRootImpl 内部的一个静态成员变量 
sRunQueues 中。而 sRunQueues 内部是通过 ThreadLocal 来保存 RunQueue 的，这意味着不同线程获取到的 RunQueue 是不同对象，
这也意味着如果我们在子线程中调用View.post(Runnable) 方法的话，该 Runnable 永远不会被执行，因为主线程根本无法获取到子线程的 RunQueue
android API 23 及之前应在主线程执行post方法





onCreate & onResume
在 onCreate,onResume 函数中为什么无法也直接得到 View 的真实宽高呢？
从结果反推原因，这说明当 onCreate、onResume被回调时 ViewRootImpl 的 performTraversals()方法还未执行，
那么performTraversals()方法的具体执行时机是什么时候呢？
这可以从 ActivityThread -> WindowManagerImpl -> WindowManagerGlobal -> ViewRootImpl 这条调用链上找到答案
首先，ActivityThread 的 handleResumeActivity 方法就负责来回调 Activity 的 onResume 方法，且如果当前 Activity 是第一次启动，
则会向 ViewManager（wm）添加 DecorView
/frameworks/base/core/java/android/app/ActivityThread.java
```
    @Override
    public void handleResumeActivity(IBinder token, boolean finalStateRequest, boolean isForward,
            String reason) {
        ···
        //Activity 的 onResume 方法
        final ActivityClientRecord r = performResumeActivity(token, finalStateRequest, reason);
        ···
        if (r.window == null && !a.mFinished && willBeVisible) {
            ···
            ViewManager wm = a.getWindowManager();
            if (a.mVisibleFromClient) {
                if (!a.mWindowAdded) {
                    a.mWindowAdded = true;
                    //重点
                    wm.addView(decor, l);
                } else {
                    a.onWindowAttributesChanged(l);
                }
            }
        } else if (!willBeVisible) {
            if (localLOGV) Slog.v(TAG, "Launch " + r + " mStartedActivity set");
            r.hideForNow = true;
        }
		···
    }

```
此处的 ViewManager 的具体实现类即 WindowManagerImpl，WindowManagerImpl 会将操作转交给 WindowManagerGlobal
/frameworks/base/core/java/android/view/WindowManagerImpl.java
```
    public void addView(@NonNull View view, @NonNull ViewGroup.LayoutParams params) {
          applyDefaultToken(params);
          mGlobal.addView(view, params, mContext.getDisplay(), mParentWindow);94      }
```
WindowManagerGlobal 就会完成 ViewRootImpl 的初始化并且调用其 setView 方法，该方法内部就会再去调用
performTraversals 方法启动视图树的绘制流程
/frameworks/base/core/java/android/view/WindowManagerGlobal.java
```
public void addView(View view, ViewGroup.LayoutParams params,
            Display display, Window parentWindow, int userId) {
        ···
        ViewRootImpl root;
        View panelParentView = null;
        synchronized (mLock) {
            ···
            root = new ViewRootImpl(view.getContext(), display);
            view.setLayoutParams(wparams);
            mViews.add(view);
            mRoots.add(root);
            mParams.add(wparams);
            try {
                root.setView(view, wparams, panelParentView, userId);
            } catch (RuntimeException e) {
                throw e;
            }
        }
    }
```
/frameworks/base/core/java/android/view/ViewRootImpl.java
```
public void setView(View view, WindowManager.LayoutParams attrs, View panelParentView) {
synchronized (this) {
  if (mView == null) {
      mView = view;
      ...
      requestLayout();
      ...
      }
      }
}
  public void requestLayout() {
      if (!mHandlingLayoutInLayoutRequest) {
          checkThread();
          mLayoutRequested = true;
          scheduleTraversals();
      }
  }
```
总结
performTraversals 方法的调用时机是在 onResume 方法之后，所以我们在 onCreate和onResume 函数中都无法获取到 View 的
实际宽高。当然，当 Activity 在单次生命周期过程中第二次调用onResume 方法时自然就可以获取到 View 的宽高属性



view.requestLayout相关
/frameworks/base/core/java/android/view/View.java
```
public void requestLayout() {
    // 1. 清除测量记录
    if (mMeasureCache != null) mMeasureCache.clear();
    // 2. 增加PFLAG_FORCE_LAYOUT给mPrivateFlags
    mPrivateFlags |= PFLAG_FORCE_LAYOUT;
    mPrivateFlags |= PFLAG_INVALIDATED;
    // 3. 如果mParent没有调用过requestLayout，则调用之。换句话说，如果调用过，则不会继续调用
    if (mParent != null && !mParent.isLayoutRequested()) {
        mParent.requestLayout();
    }
}
//ViewGroup中isLayoutRequested是view实现的   ViewGroup的requestLayout也是由view实现的
 public boolean isLayoutRequested() {
    return (mPrivateFlags & PFLAG_FORCE_LAYOUT) == PFLAG_FORCE_LAYOUT;
}
```
ViewRootImpl.requestLayout
```
 public void requestLayout() {
  if (!mHandlingLayoutInLayoutRequest) {
      checkThread();
      mLayoutRequested = true;
      scheduleTraversals();
  }
}  
private void performTraversals() {
    //layoutRequested为true可能会触发layout
    boolean layoutRequested = mLayoutRequested && (!mStopped || mReportNextDraw);
    if (layoutRequested) {
      measureHierarchy
    }
    ...
    final boolean didLayout = layoutRequested && (!mStopped || mReportNextDraw);
    ...
    if (didLayout) {
       //performLayout会触发measureHierarchy
       performLayout(lp, mWidth, mHeight);}
    ...    
}
 measureHierarchy{
     performMeasure
 }
```
https://www.jianshu.com/p/5ec0f278e0a3
requestLayout会直接递归调用父窗口的requestLayout，直到ViewRootImpl,然后触发performTraversals，由于mLayoutRequested为true，
会导致onMeasure和onLayout被调用。不一定会触发OnDraw。requestLayout触发onDraw可能是因为在在layout过程中发现l,t,r,b和以前不一样，
那就会触发一次invalidate，所以触发了onDraw，也可能是因为别的原因导致mDirty非空（比如在跑动画）
https://cloud.tencent.com/developer/article/1684404
需要频繁更新内容的View来说，则可以通过固定宽高等方式来避免一直触发requestLayout




//todo view.postInvalidateOnAnimation();  Android动画相关 还是单独开一个
view.postAnimation
```
   public void postOnAnimation(Runnable action) {
        final AttachInfo attachInfo = mAttachInfo;
        if (attachInfo != null) {
            attachInfo.mViewRootImpl.mChoreographer.postCallback(
                    Choreographer.CALLBACK_ANIMATION, action, null);
        } else {
            getRunQueue().post(action);
        }
    }
  //Choreographer doFrame 里调用 Callbacks 的顺序：先是处理输入处理 INPUT（比如触控事件分发），
  //然后执行 ANIMATION（Animation 的回调会在这里发生，在 Traversal 之前先设置好 View 的属性），
  //再然后就是 INSETS_ANIMATION（为 Android 11 加入的 Inset 的 Animation），然后是重点的 TRAVERSAL（Measure，Layout 和 Draw），
  //再然后就是 COMMIT（比如会用来 TrimMemory，见 ActivityThread） 
  void doFrame(long frameTimeNanos, int frame) {
        // .....

        try {
            Trace.traceBegin(Trace.TRACE_TAG_VIEW, "Choreographer#doFrame");
            AnimationUtils.lockAnimationClock(frameTimeNanos / TimeUtils.NANOS_PER_MS);

            mFrameInfo.markInputHandlingStart();
            doCallbacks(Choreographer.CALLBACK_INPUT, frameTimeNanos);

            mFrameInfo.markAnimationsStart();
            doCallbacks(Choreographer.CALLBACK_ANIMATION, frameTimeNanos);
            doCallbacks(Choreographer.CALLBACK_INSETS_ANIMATION, frameTimeNanos);

            mFrameInfo.markPerformTraversalsStart();
            doCallbacks(Choreographer.CALLBACK_TRAVERSAL, frameTimeNanos);

            doCallbacks(Choreographer.CALLBACK_COMMIT, frameTimeNanos);
        } finally {
            AnimationUtils.unlockAnimationClock();
            Trace.traceEnd(Trace.TRACE_TAG_VIEW);
        }

        // ......
    } 
```



//todo ViewTreeObserver
https://ljd1996.github.io/2020/09/11/Android-View%E5%8E%9F%E7%90%86%E4%B8%8E%E5%AE%9E%E8%B7%B5/
