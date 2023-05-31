package sst.example.androiddemo.feature.activity;

import android.content.pm.ActivityInfo;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import sst.example.androiddemo.feature.R;

public class LandscapeActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    //// 测试横屏进入竖屏activity 是否有问题，需要在xml中配置或者attachWindow中
    //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    //防止进入动画影响横竖屏切换
    //overridePendingTransition(0,0);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_landscape);
  }
}