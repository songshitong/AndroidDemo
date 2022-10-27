package sst.example.androiddemo.feature.activity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import sst.example.androiddemo.feature.R;

public class TestOrientationActivity extends AppCompatActivity  {
    private static final String TAG = "NormalActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_normal);

        Log.d(TAG," onCreate ==== ");
        Button btn = findViewById(R.id.normal_btn);
        btn.setOnClickListener(v -> {
            //android没有全局配置全屏，只能在xml每个配置/基类/application中监听activity创建
            if(getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                //切换竖屏
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }else{
                //切换横屏
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        });
        findViewById(R.id.landscape_btn).setOnClickListener(v -> startActivity(new Intent(this,LandscapeActivity.class)));
        //NormalActivity先切换横屏，然后跳转到LandscapeActivity   LandscapeActivity会竖屏进入，然后切换为横屏
        //解决复写LandscapeActivity的进入动画

        //横竖屏会改变方向，影响下一个activity
        //NormalActivity竖屏  LandscapeActivity从右向左进入
        //NormalActivity横屏  LandscapeActivity从底部向上进入
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG," onRestart ==== ");

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG," onStart ==== ");

    }

    //在onStart和onRestoreInstanceState之后调用
    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        Log.d(TAG," onPostCreate ==== ");

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG," onResume ==== ");

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG," onPause ==== ");

    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG," onStop ==== ");

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG," onDestroy ==== ");

    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG," onSaveInstanceState ==== ");

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d(TAG," onRestoreInstanceState ==== ");

    }

    //只有设置了android:configChanges="orientation|keyboardHidden|screenSize"才不会重建activity，触发此回调
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG," onConfigurationChanged  ==== "+newConfig.toString());

    }
}
