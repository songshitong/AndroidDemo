
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
ViewGroup.LayoutParams params = getDialog().getWindow().getAttributes();
params.width = LayoutParams.MATCH_PARENT;
params.height = LayoutParams.MATCH_PARENT;
getDialog().getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);
}
移除边距
```
 <style name="Dialog.FullScreen" parent="Theme.AppCompat.Dialog">
        <item name="android:padding">0dp</item>
        <item name="android:windowBackground">@android:color/transparent</item>
    </style>
```
editNameDialogFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.Dialog_FullScreen);
editNameDialogFragment.show(getSupportFragmentManager(), "edit");