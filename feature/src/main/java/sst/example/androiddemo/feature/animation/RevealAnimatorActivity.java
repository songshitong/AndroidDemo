package sst.example.androiddemo.feature.animation;

import androidx.appcompat.app.AppCompatActivity;

import android.animation.Animator;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewAnimationUtils;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;

import sst.example.androiddemo.feature.R;


public class RevealAnimatorActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reveal_animator);
        ImageView iv = findViewById(R.id.iv_revealAnimation);
        iv.post(new Runnable() {
            @Override
            public void run() {
                  int width = iv.getWidth();
                  int height = iv.getHeight();
                  int radius = (int) (Math.sqrt(width*width+height*height)/2);
                  //揭露动画的使用  android5.0
//                view ：要执行动画效果的View
//                centerX：圆心x坐标，相对于view的坐标
//                centerY：圆心y坐标，相对于view的坐标
//                startRadius：开始时的圆半径
//                endRadius：结束时的圆半径
//                返回值是一个Animator。
                Log.d("Reveal animation","width "+width+" height "+height+" radius "+radius);
                Animator animator = ViewAnimationUtils.createCircularReveal(iv,width/2,height/2,0,radius);
                animator.setDuration(3000);
                animator.setInterpolator(new AccelerateDecelerateInterpolator());
                animator.start();
            }
        });
    }
}