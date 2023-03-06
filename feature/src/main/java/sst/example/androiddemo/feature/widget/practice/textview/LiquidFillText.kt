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

class LiquidFillText(text: String, val color: Int = Color.BLUE) : AnimatorTextItem(text) {
  private val animator: ValueAnimator = ValueAnimator.ofFloat(0f, 1f)
  private var animateFraction = 0f

  init {
    animator.duration = 5000
    animator.addUpdateListener {
      animateFraction = it.animatedFraction
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
    if (!isStart) {
      return
    }
    // canvas.save()
    val rect = Rect()
    //需要优化，绘制时不要每次都测量文字
    paint.getTextBounds(text, 0, text.length, rect)
    val width = rect.width().toFloat()
    val height = rect.height().toFloat()
    // canvas.translate(0f, height)
    val layerId = canvas.saveLayer(0f,0f, rect.width().toFloat(), rect.height().toFloat()+100,paint)
    val path = Path()
    val baseHeight =
    (height ) - (animator.animatedFraction * height)
    path.moveTo(0.0f, baseHeight)
    //遍历宽度每一个点
    for(i in 0 until width.toInt()){
      //根据i计算高度  代码来自https://github.com/aagarwal1012/Animated-Text-Kit/blob/master/lib/src/text_liquid_fill.dart
      path.lineTo(i.toFloat(),
        (baseHeight + kotlin.math.sin(2 * Math.PI * (i / width + (animator.animatedValue as Float))) * 8).toFloat()
      )
    }

    path.lineTo(width, height)
    path.lineTo(0.0f, height)
    path.close()

    val tempColor = paint.color
    paint.color = Color.WHITE
    canvas.drawText(text,0f,height,paint)

    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)  //保留DST，重合部分是SRC的颜色  试出来的模式。。。
    paint.color=color
    canvas.drawPath(path, paint)

    paint.xfermode = null
    paint.color = tempColor
    canvas.restoreToCount(layerId)
    // canvas.restore()
  }

  override fun startAnim() {
    animator.start()
  }
}