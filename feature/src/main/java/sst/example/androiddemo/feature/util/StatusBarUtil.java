package sst.example.androiddemo.feature.util;

import android.app.Activity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import sst.example.androiddemo.feature.R;

//可以参考 https://github.com/Zackratos/UltimateBarX
public class StatusBarUtil {
  //https://juejin.cn/post/6878195975966425101#heading-10
  //沉浸式状态栏
  //设置透明状态栏颜色 然后所有屏幕展示contentView，状态栏是颜色默认，但是内容顶上去了
  public void setTranslucentStatusBar(Window window,boolean statusTextBlack) {
    // 修改状态栏字体颜色，用AndroidX官方兼容API  https://blog.csdn.net/ysy950803/article/details/119869625
    WindowInsetsControllerCompat wic = WindowCompat.getInsetsController(window,window.getDecorView());
    if (wic != null) {
      // true表示Light Mode，状态栏字体呈黑色(整体是偏白色，字体是黑色)，反之呈白色
      wic.setAppearanceLightStatusBars(statusTextBlack);
    }
    window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
  }

  //配合透明状态栏使用
  //给需要留边的view使用(增加一个状态栏的高度的paddingTop)  一般不用在根布局，这样内容又被顶下来了
  //设置fitSystemWindow  https://stackoverflow.com/questions/30439661/androidfitssystemwindows-at-runtime
  //xml中android:fitsSystemWindows="true"
  public void fitSystemWindow(Activity activity){
    View view = activity.findViewById(android.R.id.content);
    view.setFitsSystemWindows(false);
    view.setPadding(0, 0, 0, 0);
  }


  //xml设置
  // <style name="AppTheme" parent="Theme.AppCompat.Light">
  //    <item name="colorPrimary">@color/color_primary</item>
  //    <item name="colorPrimaryDark">@color/color_secondary</item>
  //    <item name="colorAccent">@color/color_accent</item>
  //    <item name="android:statusBarColor">@color/color_primary</item>
  //    <item name="android:windowLightStatusBar">true</item> //改变状态栏系统文字的颜色 true深色 false浅色
  //</style>
  //设置状态栏颜色
  public void setStatusBarColor(Window window, int color) {
    //取消设置透明状态栏,使 ContentView 内容不再覆盖状态栏
    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
    //需要设置这个 flag 才能调用 setStatusBarColor 来设置状态栏颜色
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
    //设置状态栏颜色
    window.setStatusBarColor(color);
  }

  //设置全屏
  public static void setFullScreen(Activity activity) {
    activity.getWindow().getDecorView()
        .setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
  }
}