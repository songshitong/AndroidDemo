package sst.example.androiddemo.feature.widget.practice

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.PointF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
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
    addView(WaveView(context))
  }

  override fun onTouchEvent(event: MotionEvent?): Boolean {
    if(event?.action == MotionEvent.ACTION_DOWN){
      for (i in 0 until childCount){
        val waveView = getChildAt(i) as WaveView
        waveView.start()
      }
    }
    return super.onTouchEvent(event)
  }
}

class WaveView : View {
  //波峰和波谷的数量
  var trough = 4

  //每帧移动的距离
  var waveSpeed = 3

  //水平线高度
  var waveTop = 400

  //波谷的高度
  var waveHeight = 100

  //颜色
  var waveColor = Color.parseColor("#8800a9ff")

  //线宽
  var waveStrokeWith = 5
  lateinit var shipBitmap:Bitmap
  var shipW =0
  var shipH =0
  var shipX =0f
  var shipY =0f
  var shipAngle = 0f
  //点的数量
  private val wavePointList = arrayListOf<PointF>()
  val paint = Paint()
  val path = Path()
  val pathMeasure = PathMeasure()
  lateinit var shipAnimator: ValueAnimator
  lateinit var waveAnimator: ValueAnimator
  constructor(context: Context) : this(context, null)
  constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)
  constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : super(
    context,
    attributeSet,
    defStyleAttr
  ) {
    shipBitmap = BitmapFactory.decodeResource(resources, R.drawable.ship)
    shipW = shipBitmap.width
    shipH = shipBitmap.height

    paint.color = waveColor
    paint.strokeWidth = waveStrokeWith.toFloat()
    paint.style = Paint.Style.STROKE

    shipAnimator = ValueAnimator.ofInt(0,1)
    shipAnimator.duration = 3000
    shipAnimator.addUpdateListener{
      //todo 小船，波浪 速率不同，使用两个animator
      pathMeasure.setPath(path,false)
      val distance = pathMeasure.length * it.animatedFraction
      val positionArray = FloatArray(2)
      val tanArray = FloatArray(2)
      pathMeasure.getPosTan(distance,positionArray,tanArray)
      shipX = positionArray[0] - shipW/2
      shipY = positionArray[1] - shipH /2 //bitmap绘制在左上角，需要移动到中心
      shipAngle = Math.toDegrees(
        tanArray[1].toDouble()
      ).toFloat()
      Log.d("distance "," position $shipX $shipY  tan:${tanArray[0]},${tanArray[1]} $shipAngle")


      invalidate()
    }

    waveAnimator = ValueAnimator.ofInt(0,1)
    waveAnimator.duration = 3000
    waveAnimator.repeatMode = ValueAnimator.RESTART
    waveAnimator.addUpdateListener {
      moveLength +=waveSpeed
      if(moveLength>=waveSpeed){
        resetPoints()
      }
      updatePoints()
      invalidate()
    }
  }

  private fun resetPoints() {
    moveLength =0
  }

  private var moveLength =0

  private fun updatePoints() {
    wavePointList.forEach {
      it.x += waveSpeed
    }
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
    val perWidth = w / (pointSize - 1)  //2个点，一个段   3个点，2个段
    for (i in 0 until pointSize) {
      var offsetY = 0
      //确定点在水平之上还是之下   一个波峰+波谷需要4个点
      val indexI = i % 4
      if (0 == indexI || 2 == indexI ) {
        offsetY = 0
      } else if (1 == indexI) {
        offsetY = -waveHeight
      } else if (3 == indexI) {
        offsetY = waveHeight
      }
      val point = PointF((perWidth * i).toFloat(), (offsetY + waveTop).toFloat())
      wavePointList.add(point)
    }
  }

  override fun onDraw(canvas: Canvas?) {
    super.onDraw(canvas)
    path.reset()
    val firstPoint = wavePointList[0]
    path.moveTo(firstPoint.x, firstPoint.y)
    val pointSize = wavePointList.size
    drawPoint(canvas,firstPoint)

    for (i in 1 until pointSize step 2) {
      if (i + 1 >= pointSize) break
      val point = wavePointList[i]
      val nextPoint = wavePointList[i + 1]
      // Log.d("onDraw"," index:$i point:$point nextPoint:$nextPoint")
      path.quadTo(point.x, point.y, nextPoint.x, nextPoint.y)
      drawPoint(canvas,point)
      drawPoint(canvas,nextPoint)
    }
    //画波浪
    canvas?.let {
      it.drawPath(path, paint)
    }

    //画小船
    canvas?.let {
      if(shipX ==0f && shipY ==0f ){
        return@let
      }
      it.save()
      it.rotate(shipAngle,shipX,shipY)
      it.drawBitmap(shipBitmap,shipX,shipY,paint)
      it.restore()
    }
  }

  private fun drawPoint(canvas: Canvas?,point: PointF){
    canvas?.drawCircle(point.x,point.y, 10F,paint)
  }

  fun start() {
    shipAnimator.start()
  }
}