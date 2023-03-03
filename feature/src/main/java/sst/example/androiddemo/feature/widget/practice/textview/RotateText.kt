package sst.example.androiddemo.feature.widget.practice.textview

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import android.view.animation.AccelerateInterpolator
import androidx.core.animation.addListener

class RotateText(text: String,val rotateOut:Boolean=true) : AnimatorTextItem(text) {
  private val inAnimator: ValueAnimator = ValueAnimator.ofFloat(0f, 1f)
  private val outAnimator: ValueAnimator = ValueAnimator.ofFloat(0f, 1f)
  private val rotateHeightDefault =  100f
  private var rotateHeight = rotateHeightDefault
  private var alpha = 0

  init {
    inAnimator.duration = 1000
    // inAnimator.interpolator = AccelerateInterpolator()
    inAnimator.addUpdateListener {
      getAnimatorTextListener()?.onUpdate(it.animatedFraction)
      rotateHeight = rotateHeightDefault*(it.animatedFraction-1) //-1->0
      alpha = (it.animatedFraction * 255).toInt()
    }
    inAnimator.addListener(onStart = {
      alpha = 0
      rotateHeight = 100f
    }, onEnd = {
      if(rotateOut){
        outAnimator.start()
      }
      getAnimatorTextListener()?.showNext()
    }
    )

    outAnimator.duration=1000
    // outAnimator.interpolator = AccelerateInterpolator()
    outAnimator.addUpdateListener {
      getAnimatorTextListener()?.onUpdate(it.animatedFraction)
      rotateHeight =  rotateHeightDefault*it.animatedFraction
      alpha = ((1-it.animatedFraction) * 255).toInt()
    }
    outAnimator.addListener(onEnd = {
      getAnimatorTextListener()?.onEnd()
    }
    )
  }

  override fun drawText(canvas: Canvas, paint: Paint) {
    canvas.save()
    val rect = Rect()
    //需要优化，绘制时不要每次都测量文字
    paint.getTextBounds(text, 0, text.length, rect)
    Log.d("drawText"," rotateHeight:$rotateHeight")
    canvas.translate(0f, rotateHeight + rect.height())
    val originAlpha = paint.alpha
    paint.alpha = alpha
    canvas.drawText(text, 0f, 0f, paint)
    paint.alpha = originAlpha
    canvas.restore()
  }

  override fun startAnim() {
    inAnimator.start()
  }
}