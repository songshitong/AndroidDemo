
创建和销毁的大概流程
创建PopupWindow的时候，先创建WindowManager，因为WindowManager拥有控制view的添加和删除、修改的能力。
然后是setContentView，保存contentView
显示PopupWindow，这个步骤稍微复杂点，创建并初始化LayoutParams，设置相关参数，作为以后PopupWindow在应用DecorView里哪里显示的凭据。
   然后创建PopupView，并且将contentView插入其中。最后使用WindowManager将PopupView添加到应用DecorView里。

销毁PopupView，WindowManager把PopupView移除，PopupView再把contentView移除，最后把对象置为null


PopupWindow和Dialog有什么区别？
两者最根本的区别在于有没有新建一个window，PopupWindow没有新建，而是将view加到DecorView；Dialog是新建了一个window，
   相当于走了一遍Activity中创建window的流程
从源码中可以看出，PopupWindow最终是执行了mWindowManager.addView方法，全程没有新建window



为何弹窗点击一下就dismiss呢？
PopupWindow通过为传入的View添加一层包裹的布局，并重写该布局的点击事件，实现点击PopupWindow之外的区域PopupWindow消失的效果


2022-07-24
https://juejin.cn/post/6844903689480568845
android-12.0.0_r3
/frameworks/base/core/java/android/widget/PopupWindow.java
```
 public PopupWindow(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
          mContext = context;
          mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
  
          final TypedArray a = context.obtainStyledAttributes(
                  attrs, R.styleable.PopupWindow, defStyleAttr, defStyleRes);
          final Drawable bg = a.getDrawable(R.styleable.PopupWindow_popupBackground);
          mElevation = a.getDimension(R.styleable.PopupWindow_popupElevation, 0);
          mOverlapAnchor = a.getBoolean(R.styleable.PopupWindow_overlapAnchor, false);
          ...
          final Transition enterTransition = getTransition(a.getResourceId(
                  R.styleable.PopupWindow_popupEnterTransition, 0));
          final Transition exitTransition;
          ...
  
          setEnterTransition(enterTransition);
          setExitTransition(exitTransition);
          setBackgroundDrawable(bg);
      }
```
构造器初始化WindowManager，设置enterTransition，exitTransition，背景，阴影等


setContentView
```
public void setContentView(View contentView) {
          if (isShowing()) {
              return;
          }
          mContentView = contentView;
          if (mContext == null && mContentView != null) {
              mContext = mContentView.getContext();
          }
  
          if (mWindowManager == null && mContentView != null) {
              mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
          }
    //在这里根据SDK版本而不是在构造函数中设置附加InDecor的默认设置，因为构造函数中可能没有上下文对象。我们只想在这里设置默认，
    //如果应用程序尚未设置附加InDecor。
          if (mContext != null && !mAttachedInDecorSet) {         
              setAttachedInDecor(mContext.getApplicationInfo().targetSdkVersion
                      >= Build.VERSION_CODES.LOLLIPOP_MR1);
          }
  
      }
      
   public void setAttachedInDecor(boolean enabled) {
          mAttachedInDecor = enabled;
          mAttachedInDecorSet = true;
      }     
```

showAsDropDown
```
 public void showAsDropDown(View anchor) {
          showAsDropDown(anchor, 0, 0);
      }

 public void showAsDropDown(View anchor, int xoff, int yoff) {
          showAsDropDown(anchor, xoff, yoff, DEFAULT_ANCHORED_GRAVITY);
      }

 public void showAsDropDown(View anchor, int xoff, int yoff, int gravity) {
          if (isShowing() || !hasContentView()) {
              return;
          }
          TransitionManager.endTransitions(mDecorView);
          //设置anchor及anchor的rootView
          attachToAnchor(anchor, xoff, yoff, gravity);
  
          mIsShowing = true;
          mIsDropdown = true;
          //通过createPopupLayoutParams方法创建和初始化WindowManager.LayoutParams
          final WindowManager.LayoutParams p =
                  createPopupLayoutParams(anchor.getApplicationWindowToken());
          //创建PopupDecorView处理事件分发，动画        
          preparePopup(p);
          ...
          //通过WindowManager.addView添加view，开始动画
          invokePopup(p);
      }
  
```

attachToAnchor
```
  protected void attachToAnchor(View anchor, int xoff, int yoff, int gravity) {
          detachFromAnchor();
          ..
          final View anchorRoot = anchor.getRootView();
          anchorRoot.addOnAttachStateChangeListener(mOnAnchorRootDetachedListener);
          anchorRoot.addOnLayoutChangeListener(mOnLayoutChangeListener);
          //通过弱引用设置anchor和anchor的rootView 
          mAnchor = new WeakReference<>(anchor);
          mAnchorRoot = new WeakReference<>(anchorRoot);
          mIsAnchorRootAttached = anchorRoot.isAttachedToWindow();
          mParentRootView = mAnchorRoot;
          ...
      }
```

createPopupLayoutParams  生成WindowManager.LayoutParams及其配置
```
 protected final WindowManager.LayoutParams createPopupLayoutParams(IBinder token) {
          final WindowManager.LayoutParams p = new WindowManager.LayoutParams();
          p.gravity = computeGravity();
          p.flags = computeFlags(p.flags);
          p.type = mWindowLayoutType;
          p.token = token;
          p.softInputMode = mSoftInputMode;
          p.windowAnimations = computeAnimationResource();
  
          if (mBackground != null) {
              p.format = mBackground.getOpacity();
          } else {
              p.format = PixelFormat.TRANSLUCENT;
          }
  
          if (mHeightMode < 0) {
              p.height = mLastHeight = mHeightMode;
          } else {
              p.height = mLastHeight = mHeight;
          }
  
          if (mWidthMode < 0) {
              p.width = mLastWidth = mWidthMode;
          } else {
              p.width = mLastWidth = mWidth;
          }
          p.privateFlags = PRIVATE_FLAG_WILL_NOT_REPLACE_ON_RELAUNCH
                  | PRIVATE_FLAG_LAYOUT_CHILD_WINDOW_IN_PARENT_FRAME;
          p.setTitle("PopupWindow:" + Integer.toHexString(hashCode()));
  
          return p;
      }
```

preparePopup
```
 private void preparePopup(WindowManager.LayoutParams p) {
          ...
          if (mBackground != null) {
              //将view添加到PopupBackgroundView    PopupBackgroundView继承FrameLayout，没啥内容 
              mBackgroundView = createBackgroundView(mContentView);
              mBackgroundView.setBackground(mBackground);
          } else {
              mBackgroundView = mContentView;
          }
          //创建PopupDecorView，并添加mBackgroundView     PopupDecorView继承FrameLayout，处理按键(比如点击消失)，动画相关
          mDecorView = createDecorView(mBackgroundView);
          ...
      }
  

    private PopupBackgroundView createBackgroundView(View contentView) {
          final ViewGroup.LayoutParams layoutParams = mContentView.getLayoutParams();
          ...
          final PopupBackgroundView backgroundView = new PopupBackgroundView(mContext);
          final PopupBackgroundView.LayoutParams listParams = new PopupBackgroundView.LayoutParams(
                  MATCH_PARENT, height);
          backgroundView.addView(contentView, listParams);
          return backgroundView;
      }  
      

 private PopupDecorView createDecorView(View contentView) {
          final ViewGroup.LayoutParams layoutParams = mContentView.getLayoutParams();
          ...
          final PopupDecorView decorView = new PopupDecorView(mContext);
          //将view添加到PopupDecorView
          decorView.addView(contentView, MATCH_PARENT, height);
          decorView.setClipChildren(false);
          decorView.setClipToPadding(false);
  
          return decorView;
      } 

 private class PopupDecorView extends FrameLayout {
   public boolean onTouchEvent(MotionEvent event) {
             final int x = (int) event.getX();
             final int y = (int) event.getY();
             //点击消失
             if ((event.getAction() == MotionEvent.ACTION_DOWN)
                     && ((x < 0) || (x >= getWidth()) || (y < 0) || (y >= getHeight()))) {
                 dismiss();
                 return true;
             } else if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
            
                 dismiss();
                 return true;
             } else {
                 return super.onTouchEvent(event);
             }
         }
 }           
```


invokePopup
```
private void invokePopup(WindowManager.LayoutParams p) {
          ...
          //通过windowManager添加view
          mWindowManager.addView(decorView, p);
          //启动动画
          if (mEnterTransition != null) {
              decorView.requestEnterTransition(mEnterTransition);
          }
      }
```




dismiss 弹窗消失
```
public void dismiss() {
          ..
          //取消动画
          decorView.cancelTransitions();
          ... 
          if (exitTransition != null && decorView.isLaidOut()
                  && (mIsAnchorRootAttached || mAnchorRoot == null)) {
              final LayoutParams p = (LayoutParams) decorView.getLayoutParams();
              p.flags |= LayoutParams.FLAG_NOT_TOUCHABLE;
              p.flags |= LayoutParams.FLAG_NOT_FOCUSABLE;
              p.flags &= ~LayoutParams.FLAG_ALT_FOCUSABLE_IM;
              mWindowManager.updateViewLayout(decorView, p);
              ...
              decorView.startExitTransition(exitTransition, anchorRoot, epicenter,
                      new TransitionListenerAdapter() {
                          @Override
                          public void onTransitionEnd(Transition transition) {
                              dismissImmediate(decorView, contentHolder, contentView);
                          }
                      });
          } else {
              //立即消失
              dismissImmediate(decorView, contentHolder, contentView);
          }
          //Anchor移除监听，anchor，AnchorRoot置为空，将view从Parent中移除
          detachFromAnchor();
          //消失回调
          if (mOnDismissListener != null) {
              mOnDismissListener.onDismiss();
          }
      }
      
private void dismissImmediate(View decorView, ViewGroup contentHolder, View contentView) {
          //通过WindowManager.removeViewImmediate移除view
          if (decorView.getParent() != null) {
              mWindowManager.removeViewImmediate(decorView);
          }
          if (contentHolder != null) {
              //将view从Parent中移除
              contentHolder.removeView(contentView);
          }
          mDecorView = null;
          mBackgroundView = null;
          mIsTransitioningToDismiss = false;
      }      
```