
总结
Dialog中的Window对象与Activity中的Window对象是相似的，都对应着一个WindowManager对象；Dialog和Activity的显示逻辑是相似的都是
   内部管理这一个Window对象，用Window对象实现界面的加载与显示逻辑。
窗口的取消绘制流程是相似的，包括Activity和Dialog等；通过调用WindowManager.removeViewImmediate方法，
开始执行Window窗口的取消绘制流程；Window窗口的取消绘制流程，通过清空bitma撤销draw的执行效果，通过置空View撤销meature和layout的执行效果


https://juejin.cn/post/6844903694408876040

android-12.0.0_r3
AlertDialog使用
```
private AlertDialog alertDialog=null;
public void showDialog(){
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setIcon(R.mipmap.ic_launcher);
    builder.setMessage("潇湘剑雨");
    builder.setTitle("这个是标题");
    builder.setView(R.layout.activity_main);
    builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            alertDialog.dismiss();
        }
    });
    builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            alertDialog.dismiss();
        }
    });
    alertDialog = builder.create();
    alertDialog.show();
}
```
dialog生命周期
```
/**
 * 类似于Activity的onCreate函数，可以在这个方法中进行Dialog的一些初始化操作
 * 包括调用setContentView方法
 */ 
protected void onCreate(Bundle savedInstanceState) { } 
/**
 * 当对话框启动的时候被调用.
 */ 
protected void onStart() { } 
/**
 * 当对话框停止的时候被调用.
 */ 
protected void onStop() { }
```


源码
/frameworks/base/core/java/android/app/AlertDialog.java
```
 public AlertDialog create() {
              //P是AlertController.AlertParams   
              final AlertDialog dialog = new AlertDialog(P.mContext, 0, false);
              P.apply(dialog.mAlert);
              dialog.setCancelable(P.mCancelable);
              if (P.mCancelable) {
                  dialog.setCanceledOnTouchOutside(true);
              }
              dialog.setOnCancelListener(P.mOnCancelListener);
              dialog.setOnDismissListener(P.mOnDismissListener);
              if (P.mOnKeyListener != null) {
                  dialog.setOnKeyListener(P.mOnKeyListener);
              }
              return dialog;
          }
          
public class AlertDialog extends Dialog implements DialogInterface {
  AlertDialog(Context context, @StyleRes int themeResId, boolean createContextThemeWrapper) {
          //调用dialog的构造逻辑
          super(context, createContextThemeWrapper ? resolveDialogTheme(context, themeResId) : 0,
                  createContextThemeWrapper);
          mWindow.alwaysReadCloseOnTouchAttr();
          //创建AlertController
          mAlert = AlertController.create(getContext(), this, getWindow());
      }
}    
      
```
apply(AlertController dialog)
/frameworks/base/core/java/com/android/internal/app/AlertController.java
```
public void apply(AlertController dialog) {
              if (mCustomTitleView != null) {
                  dialog.setCustomTitle(mCustomTitleView);
              } else {
                  if (mTitle != null) {
                      dialog.setTitle(mTitle);
                  }
                  if (mIcon != null) {
                      dialog.setIcon(mIcon);
                  }
                  if (mIconId != 0) {
                      dialog.setIcon(mIconId);
                  }
                  if (mIconAttrId != 0) {
                      dialog.setIcon(dialog.getIconAttributeResId(mIconAttrId));
                  }
              }
              if (mMessage != null) {
                  dialog.setMessage(mMessage);
              }
              ...//设置dialog各种属性
              if (mView != null) {
                  if (mViewSpacingSpecified) {
                      dialog.setView(mView, mViewSpacingLeft, mViewSpacingTop, mViewSpacingRight,
                              mViewSpacingBottom);
                  } else {
                      dialog.setView(mView);
                  }
              } else if (mViewLayoutResId != 0) {
                  dialog.setView(mViewLayoutResId);
              }
          }
```


dialog的构造器
/frameworks/base/core/java/android/app/Dialog.java
```
 Dialog(@UiContext @NonNull Context context, @StyleRes int themeResId,
              boolean createContextThemeWrapper) {
           //创建一个Context    
          if (createContextThemeWrapper) {
              if (themeResId == Resources.ID_NULL) {
                  final TypedValue outValue = new TypedValue();
                  context.getTheme().resolveAttribute(R.attr.dialogTheme, outValue, true);
                  themeResId = outValue.resourceId;
              }
              mContext = new ContextThemeWrapper(context, themeResId);
          } else {
              mContext = context;
          }
           //获取一个WindowManager对象
          mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
          //创建一个Window对象
          final Window w = new PhoneWindow(mContext);
          mWindow = w;
          //为Windowd对象设置回调，并且它本身实现了这些回调函数
          w.setCallback(this);
          w.setOnWindowDismissedCallback(this);
          w.setOnWindowSwipeDismissedCallback(() -> {
              if (mCancelable) {
                  cancel();
              }
          });
          //为Window对象设置WindowManager对象
          w.setWindowManager(mWindowManager, null, null);
          w.setGravity(Gravity.CENTER);
           //创建一个对话框监听Handler
          mListenersHandler = new ListenersHandler(this);
      }
```


show
```
 public void show() {
          ...
          mCanceled = false;
  
          if (!mCreated) {
             //没有创建调用onCreate(savedInstanceState)进行创建
              dispatchOnCreate(null);
          } else {
              final Configuration config = mContext.getResources().getConfiguration();
              mWindow.getDecorView().dispatchConfigurationChanged(config);
          }
          //调用onStart
          onStart();
          //从window中获取decor
          mDecor = mWindow.getDecorView();
  
          if (mActionBar == null && mWindow.hasFeature(Window.FEATURE_ACTION_BAR)) {
              final ApplicationInfo info = mContext.getApplicationInfo();
              mWindow.setDefaultIcon(info.icon);
              mWindow.setDefaultLogo(info.logo);
              mActionBar = new WindowDecorActionBar(this);
          }
  
          WindowManager.LayoutParams l = mWindow.getAttributes();
          ...
          //通过WindowManager.addView添加mDecor
          mWindowManager.addView(mDecor, l);
          if (restoreSoftInputMode) {
              l.softInputMode &=
                      ~WindowManager.LayoutParams.SOFT_INPUT_IS_FORWARD_NAVIGATION;
          }
          mShowing = true;
          //回调OnShowListener
          sendShowMessage();
      }
  
  //添加view到window    
  public void setContentView(@NonNull View view) {
          mWindow.setContentView(view);
      }     
      
  protected void onCreate(Bundle savedInstanceState) {
          super.onCreate(savedInstanceState);
          mAlert.installContent();
      }   

/frameworks/base/core/java/com/android/internal/app/AlertController.java
 public void installContent(AlertParams params) {
          params.apply(this);
          installContent();
      }
  
      @UnsupportedAppUsage
      public void installContent() {
          int contentView = selectContentView();
          //将view添加到window  默认布局R.layout.alert_dialog  属性R.styleable.AlertDialog_layout
          mWindow.setContentView(contentView);
          setupView();
      }          
```

dismiss方法
```
 public void dismiss() {
          if (Looper.myLooper() == mHandler.getLooper()) {
              dismissDialog();
          } else {
              mHandler.post(mDismissAction);
          }
      }
  
      void dismissDialog() {
          ...//调用mWindowManager.removeViewImmediat
          try {
              mWindowManager.removeViewImmediate(mDecor);
          } finally {
              if (mActionMode != null) {
                  mActionMode.finish();
              }
              mDecor = null;
              mWindow.closeAllPanels();
              //调用onStop
              onStop();
              mShowing = false;
              sendDismissMessage();
          }
      }
```



点击外部消失
```
    public void setCanceledOnTouchOutside(boolean cancel) {
        if (cancel && !mCancelable) {
            mCancelable = true;
        }
        mWindow.setCloseOnTouchOutside(cancel);
    }
```
https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-13.0.0_r24/core/java/android/view/Window.java
window进行记录
```
 public void setCloseOnTouchOutside(boolean close) {
        mCloseOnTouchOutside = close;
        mSetCloseOnTouchOutside = true;
    }
    
 //window点击是否应该关闭  
 public boolean shouldCloseOnTouch(Context context, MotionEvent event) {
        final boolean isOutside =
                event.getAction() == MotionEvent.ACTION_UP && isOutOfBounds(context, event)
                || event.getAction() == MotionEvent.ACTION_OUTSIDE;
        if (mCloseOnTouchOutside && peekDecorView() != null && isOutside) {
            return true;
        }
        return false;
    }    
    
 //判断点击事件是否处于外部
 private boolean isOutOfBounds(Context context, MotionEvent event) {
        final int x = (int) event.getX();
        final int y = (int) event.getY();
        final int slop = ViewConfiguration.get(context).getScaledWindowTouchSlop();
        final View decorView = getDecorView();
        return (x < -slop) || (y < -slop)
                || (x > (decorView.getWidth()+slop))
                || (y > (decorView.getHeight()+slop));
    }   
```
dialog中的处理 查询window中记录的可关闭及相关逻辑后，执行取消方法
```
 public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (mCancelable && mShowing && mWindow.shouldCloseOnTouch(mContext, event)) {
            cancel();
            return true;
        }
        return false;
    }
```