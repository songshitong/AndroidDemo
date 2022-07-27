package sst.example.androiddemo.feature.activity;

import android.content.pm.ActivityInfo;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import sst.example.androiddemo.feature.R;

public class LandscapeActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    //防止进入动画影响横竖屏切换
    //overridePendingTransition(0,0);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_landscape);
  }
}