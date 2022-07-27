package sst.example.androiddemo.feature.animation

import android.animation.Keyframe
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.animation.BounceInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_key_frame.btn_key_frame
import sst.example.androiddemo.feature.R

class KeyFrameActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_key_frame)
    // https://blog.csdn.net/zhaoyanjun6/article/details/118963313
    // KeyFrame 主要用于自定义控制动画速率，KeyFrame 直译过来就是关键帧。
    // 而关键帧这个概念是从动画里学来的，一个关键帧必须包含两个原素，第一时间点，第二位置。所以这个关键帧是表示的是某个物体在哪个时间点应该在哪个位置上。
    // 比如我们要让一个球在 30 秒时间内，从（0,0）点运动到（300，200）点，那 flash 是怎么来做的呢，在 flash 中，
    // 我们只需要定义两个关键帧，在动画开始时定义一个，把球的位置放在(0,0)点；在 30 秒后，再定义一个关键帧，
    // 把球的位置放在（300，200）点。在动画 开始时，球初始在是（0，0）点，30 秒时间内就 adobe flash 就会自动填充，
    // 把球平滑移动到第二个关键帧的位置（300，200）点；
    //第一帧 百分之0，value为0
    val frame1 = Keyframe.ofFloat(0f, 0f);
    //第二帧  百分之50 value为100f
    val frame2 = Keyframe.ofFloat(1f, 60f);
    val holder = PropertyValuesHolder.ofKeyframe("translationY", frame1, frame2)

    val animator = ObjectAnimator.ofPropertyValuesHolder(btn_key_frame, holder)
    // animator.interpolator = BounceInterpolator()
    //等同于     ObjectAnimator.ofFloat(btn_key_frame,"translationY",0f,300f,0f)
    animator.duration = 700
    animator.repeatCount = ValueAnimator.INFINITE
    animator.repeatMode = ValueAnimator.REVERSE
    btn_key_frame.setOnClickListener {
      animator.start()
    }
  }
}