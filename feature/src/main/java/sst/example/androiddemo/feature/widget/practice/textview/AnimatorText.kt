package sst.example.androiddemo.feature.widget.practice.textview

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.animation.AccelerateInterpolator
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.animation.addListener

//缺点： 文字太多，整体刷新，可能有性能问题
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
    animateIndex =0
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

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
  }

  override fun onDraw(canvas: Canvas?) {
    super.onDraw(canvas)
    if (null == canvas) return
    val offset = paint.measureText(text.toString())
    canvas.translate(offset,0f)
    textList.forEach {
      it.drawText( canvas, paint)
    }
  }
}

// https://github.com/aagarwal1012/Animated-Text-Kit/blob/master/lib/src/animated_text.dart
abstract class AnimatorTextItem(val text:String) {
  //这里使用回调刷新 Animator不能监听进度，使用ObjectAnimator限制太死，有可能使用动画集合
  private var invalidateCall:(()->Unit)? = null

  fun setInvalidateCall(invalidateCall:()->Unit){
    this.invalidateCall = invalidateCall
  }

  fun getInvalidateCall():(()->Unit)?{
    return invalidateCall
  }

  abstract fun getAnimator(): Animator
  abstract fun drawText( canvas: Canvas, paint: Paint)

}

class RotateText(text: String) : AnimatorTextItem(text) {
  val animator: ValueAnimator = ValueAnimator.ofFloat(0f, 1f)
  var rotateHeight = 100f
  var inAlpha = 0
  init {
    animator.duration =1000
    animator.interpolator = AccelerateInterpolator()
    animator.addUpdateListener {
      getInvalidateCall()?.invoke()
      rotateHeight *= (1 - it.animatedFraction)
      inAlpha = (it.animatedFraction*255).toInt()
    }
    animator.addListener(onStart = {
      inAlpha =0
      rotateHeight = 100f
    })
  }

  override fun getAnimator(): Animator {
    return animator
  }

  override fun drawText(canvas: Canvas, paint: Paint) {
    canvas.save()
    val rect = Rect()
    paint.getTextBounds(text,0,text.length,rect)
    canvas.translate(0f,-rotateHeight+ rect.height())
    val originAlpha = paint.alpha
    paint.alpha = inAlpha
    canvas.drawText(text,0f,0f,paint)
    paint.alpha = originAlpha
    canvas.restore()
  }
}