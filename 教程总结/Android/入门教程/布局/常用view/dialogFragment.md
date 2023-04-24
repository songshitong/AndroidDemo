
注意：dialog中不要进行计时，dialog会隐藏，又显示，计时的进度不好保存，
最好跟随页面或者其他
dialogFragment销毁后 dialog为空，可以用来判断dialogFragment是否为空

优点 
1 横竖屏切换保存dialog状态
2 管理生命周期，使得dialog可以跟随activity   dialog忘记消失很容易窗口泄漏


缺点：消失后再次显示走onCreateView，如果想保存消失前的内容比较繁琐
1 解决 避免每次解析view
全局DialogFragment df;
if(df == null){
  df = new DialogFragment();
}
```
 public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    //避免每次解析view 
    if(null == mRootView){
      mRootView = LayoutInflater.from(getContext()).inflate(R.layout.vi_view_live, null, false);
    }else{
     ViewGroup vg = (ViewGroup) mRootView.getParent();
     vg.removeView(mRootView);
    }
    return mRootView;
  }
```
2 基于dialog，创建新的window，相当于新建了一个图层，需要处理焦点，window样式等繁琐
不适合做悬浮窗类的效果，需要自己处理位置,window样式，不唤起导航。。。

https://www.jianshu.com/p/0861ee5b9028
创建 DialogFragment 有两种方式：
覆写其 onCreateDialog 方法 — ① 利用AlertDialog或者Dialog创建出Dialog。
覆写其 onCreateView 方法 — ② 使用定义的xml布局文件展示Dialog


展示
new DialogFragment(supportFragmentManager,"dialog").show

设置dialog相关属性,点击外部可以取消
onCreateView{
getDialog().setCanceledOnTouchOutside(true);
dialog.setCancelable(false);

软键盘尺寸的调整
getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
}
设置大小，其他与dialog类似
onResume{
WindowManager.LayoutParams params = getDialog().getWindow().getAttributes();
params.width = LayoutParams.MATCH_PARENT;
params.height = LayoutParams.MATCH_PARENT;
params.gravity = Gravity.BOTTOM; //布局在底部
getDialog().getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);
}

移除边距
```
 <style name="Dialog.FullScreen" parent="Theme.AppCompat.Dialog">
        <item name="android:padding">0dp</item>
        <item name="android:windowBackground">@android:color/transparent</item>
    </style>
//其他主题
<style name="Dialog.FullScreen" parent="android:style/Theme.Dialog">
    <item name="android:padding">0dp</item>
    <item name="android:windowBackground">@android:color/transparent</item>
    <item name="android:windowNoTitle">true</item>
  </style>    
```
editNameDialogFragment.setStyle(DialogFragment.setStyle, R.style.Dialog_FullScreen); //onCreate中设置style也可以
editNameDialogFragment.show(getSupportFragmentManager(), "edit");
onCreate中设置边距/style
```
 @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setStyle(BottomSheetDialogFragment.STYLE_NORMAL, R.style.XGDialogBlackCover);
  }
```

https://www.cnblogs.com/mr-wang1/p/13265827.html   
软键盘布局上顶
setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen);


设置位置和大小,控制window
```
 public void onStart() {
        super.onStart();
        Window win = getDialog().getWindow();
        // 一定要设置Background，如果不设置，window属性设置无效
        win.setBackgroundDrawable( new ColorDrawable(Color.TRANSPARENT));

        WindowManager.LayoutParams params = win.getAttributes();
        params.gravity = Gravity.BOTTOM;
        // 使用ViewGroup.LayoutParams，以便Dialog 宽度充满整个屏幕
        params.width =  ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        win.setAttributes(params);
    }
```
示例，显示在某个view的正下方
```
  public void showOnViewBottom(FragmentManager manager, View anchor, Window window) {
    int[] location = new int[2];
    anchor.getLocationOnScreen(location);
    int windowWidth = window.getWindowManager().getDefaultDisplay().getWidth();
    int windowHeight = window.getWindowManager().getDefaultDisplay().getHeight();
    int anchorCenterX = location[0] + anchor.getWidth() / 2;
    //dialog默认在屏幕的中间，将此window的中心与view的中心对齐  
    anchorX = -(windowWidth / 2 - anchorCenterX);  //移动位置是差距的负数
    anchorY = -(windowHeight / 2 - location[1]-anchor.getHeight());
    show(manager, SHOW_TAG);
  }

 @Override public void onStart() {
    WindowManager.LayoutParams lp = getDialog().getWindow().getAttributes();
    lp.width = VIScreenUtils.dpToPxInt(getContext(),408f);
    lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
    lp.x = anchorX;
    View root = getDialog().getWindow().getDecorView();
    root.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
    //位置修正 多减了一个root的一半高度
    lp.y = anchorY+root.getMeasuredHeight()/2;
    getDialog().getWindow().setAttributes(lp);
    //dialog show之前显示
    super.onStart();
  }
```


dialogFragment改变动画
1 设置theme
```
AlertDialog.Builder builder = 
            new AlertDialog.Builder( getActivity(), R.style.MyCustomTheme );
<style name="MyCustomTheme" parent="@android:style/Theme.Panel">
    <item name="android:windowAnimationStyle">@style/MyAnimation.Window</item>
</style>
<style name="MyAnimation.Window" parent="@android:style/Animation.Activity"> 
    <item name="android:windowEnterAnimation">@anim/anim_in</item>
    <item name="android:windowExitAnimation">@anim/anim_out</item>
</style>    

<?xml version="1.0" encoding="utf-8"?>
<set xmlns:android="http://schemas.android.com/apk/res/android">
    <scale
        android:interpolator="@android:anim/linear_interpolator"
        android:fromXScale="0.0"
        android:toXScale="1.0"
        android:fromYScale="0.0"
        android:toYScale="1.0"
        android:fillAfter="false"
        android:startOffset="200"
        android:duration="200" 
        android:pivotX = "50%"
        android:pivotY = "-90%"
    />
    <translate
        android:fromYDelta="50%"
        android:toYDelta="0"
        android:startOffset="200"
        android:duration="200"
    />
</set>          
```
2 通过window的decorView
```
final View decorView = getDialog()
            .getWindow()
            .getDecorView();

    decorView.animate().translationY(-100)
            .setStartDelay(300)
            .setDuration(300)
            .start();
```

隐藏导航栏
```
  @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
    Dialog dialog = super.onCreateDialog(savedInstanceState);
    
    dialog.getWindow().getDecorView().setSystemUiVisibility( View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    
    //设置焦点，部分手机存在焦点会唤起导航栏  style_no_input也可以
    //设置后，点击外部无法取消,内部无法点击  显示后需要设置焦点，但是部分手机只要获取焦点就拉起导航栏
    dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
    
    //显示后清除不可获焦    
     dialog.setOnShowListener(
        dialogInterface -> dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE));    
    return dialog;
  }
```