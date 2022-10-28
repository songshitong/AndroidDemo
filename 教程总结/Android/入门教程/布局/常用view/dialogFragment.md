
注意：dialog中不要进行计时，dialog会隐藏，又显示，计时的进度不好保存，
最好跟随页面或者其他


优点 
1 横竖屏切换保存dialog状态
2 管理生命周期，使得dialog可以跟随activity   dialog忘记消失很容易窗口泄漏


缺点：消失后再次显示走onCreateView，如果想保存消失前的内容比较繁琐
解决 避免每次解析view
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

https://www.jianshu.com/p/0861ee5b9028
创建 DialogFragment 有两种方式：
覆写其 onCreateDialog 方法 — ① 利用AlertDialog或者Dialog创建出Dialog。
覆写其 onCreateView 方法 — ② 使用定义的xml布局文件展示Dialog


展示
new DialogFragment(supportFragmentManager,"dialog").show

设置dialog相关属性
onCreateView{
getDialog().setCanceledOnTouchOutside(true);

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
editNameDialogFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.Dialog_FullScreen); //onCreate中设置style也可以
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