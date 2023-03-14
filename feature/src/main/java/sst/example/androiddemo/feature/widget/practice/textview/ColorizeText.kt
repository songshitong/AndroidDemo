package sst.example.androiddemo.feature.widget.practice.textview

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.util.Log
import androidx.core.animation.addListener
import kotlin.math.roundToInt

class ColorizeText(text: String) : AnimatorTextItem(text) {
 private val colorizeColors = intArrayOf(
    Color.GREEN,
    Color.BLUE,
    Color.YELLOW,
    Color.RED,
  )
  private val animator: ValueAnimator = ValueAnimator.ofFloat(0f, 1f)
  private var animateFraction =0f
  init {
    animator.duration = 2000
    animator.addUpdateListener {
      animateFraction= it.animatedFraction
      getAnimatorTextListener()?.onUpdate(it.animatedFraction)
    }
    animator.addListener(onStart = {
      animateFraction = 0f
      isStart = true
    }, onEnd = {
      getAnimatorTextListener()?.onEnd()
      getAnimatorTextListener()?.showNext()
      isStart = false
    }
    )
  }
  var isStart = false

  override fun drawText(canvas: Canvas, paint: Paint) {
    if(!isStart){
      return
    }
    canvas.save()
    val rect = Rect()
    //需要优化，绘制时不要每次都测量文字
    paint.getTextBounds(text, 0, text.length, rect)
    canvas.translate(0f, rect.height().toFloat())
    val tempShader = paint.shader
    //改变渐变的  位置为0时,显示红色  当值>0时，颜色按照colorizeColors排布，开始红色变为green，中间的blue,yellow在移动，结束为green
    paint.shader = LinearGradient(0f,0f,animateFraction*rect.width(),0f,colorizeColors,null,Shader.TileMode.CLAMP)
    canvas.drawText(text, 0f, 0f, paint)
    paint.shader = tempShader
    canvas.restore()
  }

  override fun startAnim() {
    animator.start()
  }
}