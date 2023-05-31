package sst.example.androiddemo.feature.widget.layout.constraint;

import android.animation.Animator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewAnimationUtils;
import androidx.constraintlayout.widget.ConstraintHelper;
import androidx.constraintlayout.widget.ConstraintLayout;

//ConstraintHelper用来辅助布局，对一系列的view实现动画或者某种约束效果  注意：ConstraintHelper必须是ConstraintLayout的直接子布局才生效！！！！
//增加动画
//可以重写的方法有 updatePostLayout，updatePostMeasure，updatePostConstraints，updatePreDraw，resolveRtl
public  class AnimConstraintHelper extends ConstraintHelper {

  public AnimConstraintHelper(Context context) {
    super(context);
  }

  public AnimConstraintHelper(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public AnimConstraintHelper(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @Override public void updatePostLayout(ConstraintLayout container) {
    super.updatePostLayout(container);
    for (int i = 0; i < mCount; i++) {
      int id = mIds[i]; //对指定的id执行动画
      View view = container.getViewById(id);
      if (view != null) {
        Animator anim = ViewAnimationUtils.createCircularReveal(view, 0, 0, 0f, view.getWidth());
        anim.setDuration(5000);
        anim.start();
      }
    }
  }
}