package sst.example.androiddemo.feature.widget.layout.constraint;

import android.animation.Animator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewAnimationUtils;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.utils.widget.ImageFilterView;
import androidx.constraintlayout.widget.ConstraintHelper;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.os.Bundle;

import sst.example.androiddemo.feature.R;

public class ConstrainLayoutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_constrain_layout);
        //ConstraintLayout 不支持负数的margin，可以使用gudeline或者space控件
        //先布局gudeline或space    再设置约束或者正数的margin进行布局
        //constraintlayout-2.1.0-alpha2 已经支持负的margin了

        //https://constraintlayout.com/  一个关于constraintLayout的网站

        //Flow和Layer都是ConstraintHelper的子类，当两者不满足需求时，可以通过继承ConstraintHelper来实现想要的约束效果  可以参考AnimConstraintHelper
        //https://juejin.cn/post/6854573221312725000#heading-4
        //代码动态设置属性
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) findViewById(R.id.constrain_view1).getLayoutParams();
        params.height = 100;
        params.width = 50;
//        params.rightToRight =  viewId;
//        view.requestLayout(); 看情况调用
    }



}