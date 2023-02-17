package sst.example.androiddemo.feature.widget.practice

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.PointF
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.Animation.AnimationListener
import android.widget.FrameLayout
import sst.example.androiddemo.feature.R

//小船随波浪流动
//https://blog.csdn.net/jiaruihua_blog/article/details/50013665
class ShipWaveLayout : FrameLayout {
  //几条波浪线
  val waveCount = 3

  constructor(context: Context) : this(context, null)
  constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)
  constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : super(
    context,
    attributeSet,
    defStyleAttr
  ) {
    val wave1 = WaveView(context)
    addView(wave1)
    val wave2 = WaveView(context).also {
      it.waveSpeed = 5
      it.waveColor = Color.parseColor("#6000a9ff")
      it.waveHeight = 35
      it.trough = 3
    }
    addView(wave2)
    val wave3 = WaveView(context).also {
      it.waveSpeed = 7
      it.waveColor = Color.parseColor("#7010a1ea")
      it.waveHeight = 50
      it.trough = 6
    }
    addView(wave3)
    addView(ShipView(context))
    Handler(Looper.getMainLooper()).post {
      wave1.startWaveAnim()
      wave2.startWaveAnim()
      wave3.startWaveAnim()
    }
  }

  @SuppressLint("ClickableViewAccessibility")
  override fun onTouchEvent(event: MotionEvent?): Boolean {
    if (event?.action == MotionEvent.ACTION_DOWN) {
      for (i in 0 until childCount) {
        val view = getChildAt(i)
        if (view is ShipView) {
          view.start()
        }
      }
    }
    return super.onTouchEvent(event)
  }
}

open class WaveView : View {
  //波峰和波谷的数量
  var trough = 4

  //每帧移动的距离
  var waveSpeed = 3

  //水平线高度
  var waveTop = 400

  //波谷的高度
  var waveHeight = 40

  //波谷的宽度 计算获得
  var waveWidth = 0f

  //颜色
  var waveColor = Color.parseColor("#8800a9ff")

  //线宽
  var waveStrokeWith = 5

  //点的数量
  private val wavePointList = arrayListOf<PointF>()
  val paint = Paint()
  val path = Path()

  private var waveAnimator: ValueAnimator? = null
  private val removeList = mutableListOf<PointF>()

  constructor(context: Context) : this(context, null)
  constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)
  constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : super(
    context,
    attributeSet,
    defStyleAttr
  ) {
    paint.color = waveColor
    paint.strokeWidth = waveStrokeWith.toFloat()
    paint.style = Paint.Style.FILL
  }

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)
    if (wavePointList.isNotEmpty()) {
      wavePointList.clear()
    }
    //波谷/波峰   控制点数量
    //  1        3
    //  2        5
    //  3        7
    val pointSize = trough * 2 + 1
    val realPointSize = (trough + 1) * 2 + 1 //屏幕左侧多一个，超出后复位，用来实现波浪无限滚动
    waveWidth = w.toFloat() / trough  //每个波的宽度
    for (i in 0 until realPointSize) {
      var offsetY = getOffsetY(i)
      // -perWidth 整体左移一个波谷   两个点的距离是 波谷宽度/2
      val point = PointF((waveWidth / 2 * i) - waveWidth, (offsetY + waveTop).toFloat())
      wavePointList.add(point)
    }
    wavePointList.forEach {
      Log.d("point pos", "point init $it ")
    }
    if (waveAnimator == null) {
      waveAnimator = ValueAnimator.ofInt(0, waveWidth.toInt())
      waveAnimator!!.duration = 3000
      waveAnimator!!.repeatMode = ValueAnimator.RESTART
      waveAnimator!!.repeatCount = ValueAnimator.INFINITE
      waveAnimator!!.addUpdateListener {
        updatePoints(waveSpeed)
        checkResetPoints()
        // wavePointList.forEach {
        //   Log.d("point pos","point $it ")
        // }
        invalidate()
      }
    }
  }

  fun startWaveAnim() {
    waveAnimator?.start()
  }

  private fun getOffsetY(i: Int): Int {
    var offsetY = 0
    //确定点在水平之上还是之下   一个波峰+波谷需要5个点 4个点一循环
    val indexI = i % 4
    if (0 == indexI || 2 == indexI) {
      offsetY = 0
    } else if (1 == indexI) {
      offsetY = -waveHeight
    } else if (3 == indexI) {
      offsetY = waveHeight
    }
    return offsetY
  }

  open fun checkResetPoints() {
    val iterator = wavePointList.iterator()
    removeList.clear()
    val firstPoint = wavePointList.first()
    val secondPoint = wavePointList[1]
    val halfW = waveWidth / 2
    while (iterator.hasNext()) { //超出屏幕的点复位
      val point = iterator.next()
      if (point.x >= width + waveWidth) { //超出一个波谷再重置
        point.x = firstPoint.x - halfW //重置到第一点距离半个波谷的x  屏幕之外
        if (firstPoint.y < waveTop) {
          //第一个在谷底
          //新的点在水平线
          point.y = waveTop.toFloat()
        } else if (firstPoint.y > waveTop) {
          //第一个在谷峰
          //新的点在水平线
          point.y = waveTop.toFloat()
        } else {
          //第一个在水平，判断第二个
          if (secondPoint.y < waveTop) {
            //第二在谷底  新的点在谷峰
            point.y = (waveTop + waveHeight).toFloat()
          } else {
            point.y = (waveTop - waveHeight).toFloat()
          }
        }
        iterator.remove()
        removeList.add(point)
      }
    }
    wavePointList.addAll(0, removeList) //移除后，放到第一个
  }

  private fun updatePoints(offset: Int) {
    wavePointList.forEach {
      it.x += offset
    }
  }

  override fun onDraw(canvas: Canvas?) {
    super.onDraw(canvas)
    path.reset()
    val firstPoint = wavePointList[0]
    var startIndex = 0
    // 。           。    开始要从水平线上的点开始绘制    开始点从波谷或波峰画出来为直线
    // 。   。     。
    //         。

    if (firstPoint.y.toInt() != waveTop) {  //补一个水平点   无先滚动是超出屏幕外的一个点复位，不在水平点的需要补一个
      path.moveTo(firstPoint.x - waveWidth / 2, waveTop.toFloat())
    } else {
      path.moveTo(firstPoint.x, firstPoint.y)
      startIndex = 1
    }
    val pointSize = wavePointList.size
    drawPoint(canvas, firstPoint)

    for (i in startIndex until pointSize step 2) {
      if (i + 1 >= pointSize) break
      val point = wavePointList[i]
      val nextPoint = wavePointList[i + 1]
      // Log.d("onDraw"," index:$i point:$point nextPoint:$nextPoint")
      path.quadTo(point.x, point.y, nextPoint.x, nextPoint.y)
      drawPoint(canvas, point)
      drawPoint(canvas, nextPoint)
    }
    if (canPathClose()) {
      path.lineTo(width.toFloat(), 0f)
      path.lineTo(0f, 0f)
      path.close()
    }


    if (canDrawWave()) {
      //画波浪
      canvas?.drawPath(path, paint)
    }
  }

  open fun canDrawWave(): Boolean {
    return true
  }

  open fun canPathClose(): Boolean {
    return true
  }

  private fun drawPoint(canvas: Canvas?, point: PointF) {
    //绘制控制点
    // canvas?.drawCircle(point.x, point.y, 10F, paint)
  }
}

//小船的view
//小船走一次   波浪会无限滚动，两者分离  这个拆分的不好！！  曲线的生成，控制需要一个基类，不实现具体行为，交给子类
class ShipView(context: Context) : WaveView(context) {
  lateinit var shipBitmap: Bitmap
  var shipW = 0
  var shipH = 0
  var shipX = 0f
  var shipY = 0f
  var shipAngle = 0f
  var isAnimationRunning = false
  private val pathMeasure = PathMeasure()
  lateinit var shipAnimator: ValueAnimator

  init {
    shipBitmap = BitmapFactory.decodeResource(resources, R.drawable.ship)
    shipW = shipBitmap.width
    shipH = shipBitmap.height
    shipAnimator = ValueAnimator.ofInt(0, 1)
    shipAnimator.duration = 3000
    shipAnimator.addUpdateListener {
      pathMeasure.setPath(path, false)
      val distance = pathMeasure.length * it.animatedFraction
      val positionArray = FloatArray(2)
      val tanArray = FloatArray(2)
      pathMeasure.getPosTan(distance, positionArray, tanArray)
      shipX = positionArray[0] - shipW / 2
      shipY = positionArray[1] - shipH / 2 //bitmap绘制在左上角，需要移动到中心
      shipAngle = Math.toDegrees(
        tanArray[1].toDouble()
      ).toFloat()
      // Log.d("distance ", " position $shipX $shipY  tan:${tanArray[0]},${tanArray[1]} $shipAngle")
      invalidate()
    }
    shipAnimator.addListener(object : AnimatorListenerAdapter() {
      override fun onAnimationStart(animation: Animator?) {
        super.onAnimationStart(animation)
        isAnimationRunning = true
      }

      override fun onAnimationEnd(animation: Animator?) {
        super.onAnimationEnd(animation)
        isAnimationRunning = false
      }
    })
  }

  override fun onDraw(canvas: Canvas?) {
    super.onDraw(canvas)
    //画小船
    canvas?.let {
      if (shipX == 0f && shipY == 0f) {
        return@let
      }
      if (!isAnimationRunning) {
        return@let //到头不绘制了，小船会突然消失
      }
      it.save()
      it.rotate(shipAngle, shipX, shipY)
      it.drawBitmap(shipBitmap, shipX, shipY - 50, paint) //-50:小船在水平线上
      it.restore()
    }
  }

  fun start() {
    shipAnimator.start()
  }

  override fun checkResetPoints() {
    //不进行复位
  }

  override fun canDrawWave(): Boolean {
    //不绘制  小船的行进路径的波浪不展示
    return false
  }

  override fun canPathClose(): Boolean {
    return false
  }
}
