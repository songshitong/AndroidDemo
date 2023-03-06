package sst.example.androiddemo.feature.widget.practice.textview

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuff.Mode.DST_IN
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.Xfermode
import androidx.core.animation.addListener

class WavyText(text: String, val color: Int = Color.BLUE) : AnimatorTextItem(text) {
  private val animator: ValueAnimator = ValueAnimator.ofFloat(0f, 1f)
  private var animateFraction = 0f
  private var animatorIndex = 0
  init {
    animator.duration = 800
    animator.addUpdateListener {
      animateFraction = it.animatedFraction
      getAnimatorTextListener()?.onUpdate(it.animatedFraction)
    }
    animator.repeatCount = text.length
    animator.addListener(onStart = {
      animateFraction = 0f
      isStart = true
      if(animatorIndex >= text.length){
        animatorIndex = 0
      }
    }, onEnd = {
      getAnimatorTextListener()?.onEnd()
      getAnimatorTextListener()?.showNext()
      isStart = false
    }, onRepeat = {
      animatorIndex++
    }
    )
  }

  var isStart = false

  override fun drawText(canvas: Canvas, paint: Paint) {
    if (!isStart) {
      return
    }
    canvas.save()
    val rect = Rect()
    //需要优化，绘制时不要每次都测量文字
    paint.getTextBounds(text, 0, text.length, rect)
    val width = rect.width().toFloat()
    val height = rect.height().toFloat()
    canvas.translate(0f, height)
    if(animatorIndex< text.length){
      canvas.drawText(text,0f,0f,paint)
      //把要跳动的字母盖住



      val char = text[animatorIndex]
      val preRect = Rect()
      paint.getTextBounds(text,0,animatorIndex,preRect)
      // canvas.drawRect(preRect,paint)
      //0->-0.5  -0.5->0  减少，增加
      //0->0.5    0.5->1  一直增加      将前半段*-1,后半段-1
      var fra = animator.animatedFraction
      if(fra<0.5){
        fra *= -1
      }else{
        fra -= 1
      }
      canvas.drawText(char.toString(),preRect.width()+ paint.fontMetrics.leading,
        fra*50,paint)
    }

    canvas.restore()
  }

  override fun startAnim() {
    animator.start()
  }
}