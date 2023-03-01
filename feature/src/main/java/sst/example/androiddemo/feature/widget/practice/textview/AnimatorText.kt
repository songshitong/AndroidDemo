package sst.example.androiddemo.feature.widget.practice.textview

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.animation.addListener

class AnimatorText : AppCompatTextView {
  private val textList = mutableListOf<AnimatorTextItem>()
  private var animateIndex = 0

  constructor(context: Context) : this(context, null)
  constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)
  constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : super(
    context,
    attributeSet,
    defStyleAttr
  ) {
  }

  fun addTextList(textList: List<AnimatorTextItem>) {
    this.textList.addAll(textList)
  }

  fun startAnim() {
    scheduleNext()
  }

  private fun scheduleNext() {
    if (animateIndex >= textList.size) {
      //执行完成
      return
    }
    val textItem = textList[animateIndex]
    textItem.setInvalidateCall {
      invalidate()
    }
    val animator = textItem.getAnimator()
    animator.addListener(onEnd = {
      animateIndex++
      scheduleNext()
    })
    animator.start()
  }

  override fun onDraw(canvas: Canvas?) {
    super.onDraw(canvas)
    if (null == canvas) return
    textList.forEach {
      it.drawText( canvas, paint)
    }
  }
}

// https://github.com/aagarwal1012/Animated-Text-Kit/blob/master/lib/src/animated_text.dart
abstract class AnimatorTextItem {
  //这里使用回调刷新 Animator不能监听进度，使用ObjectAnimator限制太死，有可能使用动画集合
  private var invalidateCall:(()->Unit)? = null

  fun setInvalidateCall(invalidateCall:()->Unit){
    this.invalidateCall = invalidateCall
  }

  abstract fun getAnimator(): Animator
  abstract fun drawText( canvas: Canvas, paint: Paint)

}

class RotateText : AnimatorTextItem() {
  val animator: ValueAnimator = ValueAnimator.ofFloat(0f, 1f)

  override fun getAnimator(): Animator {
    return animator
  }

  override fun drawText(canvas: Canvas, paint: Paint) {
    TODO("Not yet implemented")
  }
}