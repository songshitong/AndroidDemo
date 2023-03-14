package sst.example.androiddemo.feature.widget.practice.textview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

// https://github.com/aagarwal1012/Animated-Text-Kit/blob/master/lib/src/animated_text.dart

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
    textItem.setAnimatorTextListener(object : AnimatorTextListener{

      override fun onUpdate(value: Float) {
         invalidate()
      }

      override fun showNext() {
        animateIndex++
        scheduleNext()
      }
    })
    textItem.startAnim()
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val tempText = text    //改变文字的宽度
    textList.forEach {
      text = text.toString()+it.text
    }
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    text = tempText
  }

  override fun onDraw(canvas: Canvas?) {
    super.onDraw(canvas)
    if (null == canvas) return
    val offset = paint.measureText(text.toString()) //需要优化，不要每次绘制都测量文字
    canvas.translate(offset,0f)
    textList.forEach {
      it.drawText( canvas, paint)
    }
  }
}

abstract class AnimatorTextItem(val text:String) {
  //这里使用回调刷新 Animator不能监听进度，使用ObjectAnimator限制太死，有可能使用动画集合
  private var animatorTextListener: AnimatorTextListener? = null

  fun getAnimatorTextListener():AnimatorTextListener?{
    return animatorTextListener
  }

  fun setAnimatorTextListener(animatorTextListener: AnimatorTextListener){
    this.animatorTextListener = animatorTextListener
  }

  abstract fun drawText( canvas: Canvas, paint: Paint)
  abstract fun startAnim()
}

interface AnimatorTextListener{
  fun onStart(){}
  fun onEnd(){}

  fun showNext(){}

  fun onUpdate(value:Float){}
}