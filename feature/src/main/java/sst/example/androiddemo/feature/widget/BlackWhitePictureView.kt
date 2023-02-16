package sst.example.androiddemo.feature.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff.Mode.DST_IN
import android.graphics.PorterDuff.Mode.DST_OUT
import android.graphics.PorterDuffXfermode
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import sst.example.androiddemo.feature.R
import kotlin.math.pow

//https://juejin.cn/post/6947700226858123271
// 思路1  两张图片 彩色底片，黑白前景   前景抠洞，漏出底片，洞不断扩大
// 思路2  改变像素颜色值，改变范围
//水墨画获取 PS图像->调整->黑白
class BlackWhitePictureView : View {
  private val tag = "BlackWhitePictureView"

  constructor(context: Context) : this(context, null)
  constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)
  constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : super(
    context,
    attributeSet,
    defStyleAttr
  ) {
    initView()
  }

  private fun initView() {
    colorfulBitmap = BitmapFactory.decodeResource(resources, R.drawable.img_black_white_colorful)
    initBitmap = BitmapFactory.decodeResource(resources, R.drawable.img_black_white_init)
  }

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)
    val colorFactor = colorfulBitmap.height.toDouble() / colorfulBitmap.width
    colorfulBitmap = Bitmap.createScaledBitmap(colorfulBitmap, w, (colorFactor * w).toInt(), false)

    val whiteFactor = initBitmap.height.toDouble() / initBitmap.width
    initBitmap = Bitmap.createScaledBitmap(initBitmap, w, (whiteFactor * w).toInt(), false)
  }

  val paint = Paint()
  lateinit var colorfulBitmap: Bitmap
  lateinit var initBitmap: Bitmap

  override fun onDraw(canvas: Canvas?) {
    super.onDraw(canvas)
    canvas?.let {
      //绘制  底片
      it.drawBitmap(colorfulBitmap, 0f, 0f, paint)

      // if(radius>0){
      //保存图层到堆栈
      val layerId: Int = it.saveLayer(
        0f,
        0f,
        width.toFloat(),
        height.toFloat(),
        paint,
      )

      //当前图层也是顶层图层绘制  前景
      it.drawBitmap(initBitmap, 0f, 0f, paint)

      //PorterDuffXfermode 设置画笔的图形混合模式   取下层绘制非交集部分  相当于扣了个洞
      paint.xfermode = PorterDuffXfermode(DST_OUT)//画圆

      //扩散的图形   可以改为其他图形
      it.drawCircle(circleX.toFloat(), circleY.toFloat(), radius.toFloat(), paint)

      //清除混合模式 其他bitmap也用到了paint，防止影响
      paint.xfermode = null
      //将结果绘制到底片上面
      it.restoreToCount(layerId)
      // }

    }
  }

  var circleX = -1
  var circleY = -1
  var radius = -1
  var isAnimate = false

  @SuppressLint("ClickableViewAccessibility")
  override fun onTouchEvent(event: MotionEvent?): Boolean {
    if (!isAnimate && event?.action == MotionEvent.ACTION_DOWN) {
      circleX = event.rawX.toInt()
      circleY = event.rawY.toInt()
      Log.d(tag, "circleX $circleX circleY:$circleY")
      val animator = ValueAnimator.ofInt(0, 1)
      animator.duration = 1500
      animator.addUpdateListener {
        //取斜角边
        val fullRadius = kotlin.math.sqrt(width.toDouble().pow(2) + height.toDouble().pow(2))
        radius = (fullRadius * it.animatedFraction).toInt()
        // Log.d(tag,"radius $radius")
        invalidate()
      }
      animator.interpolator = AccelerateInterpolator(1.5f) //让加速度快点
      animator.addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator?) {
          super.onAnimationEnd(animation)
          isAnimate = false
        }

        override fun onAnimationStart(animation: Animator?) {
          super.onAnimationStart(animation)
          isAnimate = true
        }
      })
      animator.start()
    }
    return super.onTouchEvent(event)
  }
}