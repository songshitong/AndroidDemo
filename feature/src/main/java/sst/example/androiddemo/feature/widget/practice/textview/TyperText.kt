package sst.example.androiddemo.feature.widget.practice.textview

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import androidx.core.animation.addListener
import kotlin.math.roundToInt

class TyperText(text: String,val enableWriteFlag:Boolean = true) : AnimatorTextItem(text) {
  private val animator: ValueAnimator = ValueAnimator.ofFloat(0f, 1f)
  private var index =0
  init {
    animator.duration = 5000
    animator.addUpdateListener {
      index = (text.length * it.animatedFraction).roundToInt() //要四舍五入，直接转int，部分index不会覆盖到
      getAnimatorTextListener()?.onUpdate(it.animatedFraction)
    }
    animator.addListener(onStart = {
      index =0
      isEnd = false
    }, onEnd = {
      getAnimatorTextListener()?.onEnd()
      getAnimatorTextListener()?.showNext()
      isEnd = true
    }
    )
  }
  var isEnd = false

  override fun drawText(canvas: Canvas, paint: Paint) {
    if(isEnd){
      return
    }
    canvas.save()
    val rect = Rect()
    //需要优化，绘制时不要每次都测量文字
    paint.getTextBounds(text, 0, text.length, rect)
    canvas.translate(0f, rect.height().toFloat())
    var drawText = text.take(index)
    if(enableWriteFlag && drawText.isNotEmpty()){
      drawText+="_"
    }
    canvas.drawText(drawText, 0f, 0f, paint)
    canvas.restore()
  }

  override fun startAnim() {
    animator.start()
  }
}