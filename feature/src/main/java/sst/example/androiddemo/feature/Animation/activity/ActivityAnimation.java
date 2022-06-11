package sst.example.androiddemo.feature.Animation.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import sst.example.androiddemo.feature.R;

public class ActivityAnimation extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_animation);
//        https://blog.csdn.net/u010126792/article/details/85766747
//      overridePendingTransition(int enterAnim, int exitAnim)  Android 2.0之后可以设置
//        enterAnim:Activity进入动画资源id
//        exitAnim：Activity退出动画资源id
//        Theme设置切换动画存在四种动画：
//        activityOpenEnterAnimation
//        从ActivityA打开ActivityB，ActivityB展示进入动画，ActivityB的进入动画。
//        activityOpenExitAnimation
//        从ActivityA打开ActivityB，ActivityA展示退出动画，ActivityA的退出动画。
//        activityCloseEnterAnimation
//        从ActivityB回退到ActivityA，ActivityA展示进入动画，ActivityA的进入动画。
//        activityCloseExitAnimation
//        从ActivityB回退到ActivityA，ActivityB展示退出动画，ActivityB的退出动画。
        overridePendingTransition(R.anim.item_animation_from_bottom,R.anim.item_animation_fall_down);
    }
}