package sst.example.androiddemo.feature.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Camera
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import sst.example.androiddemo.feature.R

//https://juejin.cn/post/6844903440552820743
//graphics.camera主要用来生成3D变换的matrix
//applyToCanvas  将matrix应用到canvas
//getMatrix(Matrix matrix) 获取转换效果后的Matrix对象
//rotate(float x, float y, float z) 沿X、Y、Z坐标进行旋转
  //        camera.rotateZ();  绕z轴旋转，即在屏幕所在平面移动
  //        camera.rotateX(0); 绕x轴旋转，屏幕里外上下
  //        camera.rotateY(0); 绕Y轴旋转，屏幕里外左右
//translate(float x, float y, float z)沿X、Y、Z轴进行平移
//setLocation(float x, float y, float z)  设置相机位置 默认为0, 0, -8

//Camera的坐标系是左手坐标系。当手机平整的放在桌面上，X轴是手机的水平方向，Y轴是手机的竖直方向，Z轴是垂直于手机向里的那个方向
//camera.translate(10,50,-180)的意思是把观察物体右移(+x)10，上移(+y)50，向-z轴移180（即让物体接近camera，这样物体将会变大)
class CameraMatrixView : View {
  private val tag = "CameraMatrixView"

  constructor(context: Context) : this(context, null)
  constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)
  constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : super(
    context,
    attributeSet,
    defStyleAttr
  )
  private val anim = ValueAnimator.ofInt(0,1000)

  private val camera = Camera()
  private val pic:Drawable? = ContextCompat.getDrawable(context, R.drawable.dog)

  init {
    anim.duration = 1000*5
    anim.addUpdateListener {
      invalidate()
    }
    anim.repeatCount = ValueAnimator.INFINITE
    anim.start()
    pic?.bounds= Rect(0,0,pic!!.intrinsicWidth,pic.intrinsicHeight)
  }

  var centerX =0F
  var centerY =0F
  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)
     centerX = (w/2).toFloat()
     centerY = (h/2).toFloat()
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    if(anim.isRunning){
      anim.cancel()
    }
  }

  override fun onDraw(canvas: Canvas?) {
    super.onDraw(canvas)
    if(null == canvas)return
    camera.save()
    camera.translate(0f,-centerY,0f) //平移位置
    camera.rotate((anim.animatedValue as Int).toFloat(), 0F, 0F)
    camera.applyToCanvas(canvas)
    camera.restore()
    canvas.drawColor(Color.BLUE)
    pic?.draw(canvas)
  }
}