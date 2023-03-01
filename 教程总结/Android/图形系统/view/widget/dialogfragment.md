

dialogFragment会监听声明周期，后台时隐藏dialog，切换到前台时显示dialog
```
    @Override
    public void onStart() {
        super.onStart();
        if (mDialog != null) {
            mViewDestroyed = false;
            mDialog.show();
        }
    }
  
      @Override
    public void onStop() {
        super.onStop();
        if (mDialog != null) {
            mDialog.hide();
        }
    }  
```
如果控制DialogFragment，隐藏建议使用dismiss而不是getDialog().hide，后者后台切换到前台会重新显示
执行dismiss 整体销毁
执行hide后，前后切换没有根据记录状态，会自动显示

可以自己记录，重写onStart方法


https://www.jianshu.com/p/50ae2531c9cc
fragment-1.1.0-sources.jar!\androidx\fragment\app\DialogFragment.java

```
public class DialogFragment extends Fragment
        implements DialogInterface.OnCancelListener, DialogInterface.OnDismissListener {
Dialog mDialog;
...
 }
```

创建dialog
```
 public LayoutInflater onGetLayoutInflater(@Nullable Bundle savedInstanceState) {
        ...
        mDialog = onCreateDialog(savedInstanceState);

        if (mDialog != null) {
            setupDialog(mDialog, mStyle);
            return (LayoutInflater) mDialog.getContext().getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
        }
        return (LayoutInflater) mHost.getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
    }
    

  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        //可以自定义dialog
        return new Dialog(requireContext(), getTheme());
    }  
    
     //根据不同的dialog style设置dialog的window样式
     public void setupDialog(@NonNull Dialog dialog, int style) {
        switch (style) {
            case STYLE_NO_INPUT:
                dialog.getWindow().addFlags(
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                // fall through...
            case STYLE_NO_FRAME:
            case STYLE_NO_TITLE:
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        }
    }     
```
1  创建dialog
2  设置dialog样式


设置dialog的内容
```
 public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ...
        //从fragment获取view,也就是onCreateView创建的
        View view = getView();
        if (view != null) {
            ...
            //将view设置给dialog
            mDialog.setContentView(view);
        }
        final Activity activity = getActivity();
        if (activity != null) {
            mDialog.setOwnerActivity(activity);
        }
        //默认点击外部消失
        mDialog.setCancelable(mCancelable);
        mDialog.setOnCancelListener(this);
        mDialog.setOnDismissListener(this);
        if (savedInstanceState != null) {
            Bundle dialogState = savedInstanceState.getBundle(SAVED_DIALOG_STATE_TAG);
            if (dialogState != null) {
                mDialog.onRestoreInstanceState(dialogState);
            }
        }
    }
```

dialog展示  DialogFragment执行onStart时，调用dialog的show方法
```
    public void showNow(@NonNull FragmentManager manager, @Nullable String tag) {
        mDismissed = false;
        mShownByMe = true;
        FragmentTransaction ft = manager.beginTransaction();
        ft.add(this, tag);
        ft.commitNow();
    }
    
    public void onStart() {
        super.onStart();
        if (mDialog != null) {
            mViewDestroyed = false;
            mDialog.show();
        }
    }   
```
1 添加fragment并展示
2  展示dialog

dialog消失
```
   public void dismiss() {
        dismissInternal(false, false);
    }

//allowStateLoss 允许状态丢失
//fromOnDismiss 没有调用dismiss()，fragment销毁view此时调用该方法dialog消失    
void dismissInternal(boolean allowStateLoss, boolean fromOnDismiss) {
       ....
        mDismissed = true;
        mShownByMe = false;
        if (mDialog != null) {
            mDialog.setOnDismissListener(null);
            mDialog.dismiss();
            if (!fromOnDismiss) {            
                if (Looper.myLooper() == mHandler.getLooper()) {
                    onDismiss(mDialog);
                } else {
                    mHandler.post(mDismissRunnable);
                }
            }
        }
        mViewDestroyed = true;
        if (mBackStackId >= 0) {
            requireFragmentManager().popBackStack(mBackStackId,
                    FragmentManager.POP_BACK_STACK_INCLUSIVE);
            mBackStackId = -1;
        } else {
            FragmentTransaction ft = requireFragmentManager().beginTransaction();
            ft.remove(this);
            if (allowStateLoss) {
                ft.commitAllowingStateLoss();
            } else {
                ft.commit();
            }
        }
    }    
```



fragment生命对dialog的管理
```
 @Override
    public void onStop() {
        super.onStop();
        //隐藏dialog
        if (mDialog != null) {
            mDialog.hide();
        }
    }
    
    
 @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mDialog != null) {         
            mViewDestroyed = true;
            mDialog.setOnDismissListener(null);
            mDialog.dismiss();
            if (!mDismissed) {
                onDismiss(mDialog);
            }
            mDialog = null;
        }
    }
```
fragment的view销毁时，将dialog移除并置为空



Dialog保存状态与恢复
```
  @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mDialog != null) {
            //取出dialog要保存的Bundle放入fragment
            Bundle dialogState = mDialog.onSaveInstanceState();
            if (dialogState != null) {
                outState.putBundle(SAVED_DIALOG_STATE_TAG, dialogState);
            }
        }
        //保存style,theme,cancelable,是否展示等
        if (mStyle != STYLE_NORMAL) {
            outState.putInt(SAVED_STYLE, mStyle);
        }
        if (mTheme != 0) {
            outState.putInt(SAVED_THEME, mTheme);
        }
        if (!mCancelable) {
            outState.putBoolean(SAVED_CANCELABLE, mCancelable);
        }
        if (!mShowsDialog) {
            outState.putBoolean(SAVED_SHOWS_DIALOG, mShowsDialog);
        }
        if (mBackStackId != -1) {
            outState.putInt(SAVED_BACK_STACK_ID, mBackStackId);
        }
    }
    
 
  @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mShowsDialog = mContainerId == 0;
        if (savedInstanceState != null) {
            mStyle = savedInstanceState.getInt(SAVED_STYLE, STYLE_NORMAL);
            mTheme = savedInstanceState.getInt(SAVED_THEME, 0);
            mCancelable = savedInstanceState.getBoolean(SAVED_CANCELABLE, true);
            mShowsDialog = savedInstanceState.getBoolean(SAVED_SHOWS_DIALOG, mShowsDialog);
            mBackStackId = savedInstanceState.getInt(SAVED_BACK_STACK_ID, -1);
        }
    }   
```
通过onSaveInstanceState保证，横竖屏切换dialog的状态不变