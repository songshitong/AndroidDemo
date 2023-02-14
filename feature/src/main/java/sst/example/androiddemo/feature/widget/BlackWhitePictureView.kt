package sst.example.androiddemo.feature.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff.Mode.DST_OUT
import android.graphics.PorterDuffXfermode
import android.util.AttributeSet
import android.view.View
import sst.example.androiddemo.feature.R

//https://juejin.cn/post/6947700226858123271
class BlackWhitePictureView : View {
  constructor(context: Context) : this(context, null)
  constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)
  constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : super(
    context,
    attributeSet,
    defStyleAttr
  ){
    initView()
  }

  private fun initView() {
    colorfulBitmap = BitmapFactory.decodeResource(resources, R.drawable.img_black_white_colorful)
    initBitmap = BitmapFactory.decodeResource(resources, R.drawable.img_black_white_init)
  }

  //水墨画获取 PS图像->调整->黑白
  val paint = Paint()
  lateinit var colorfulBitmap: Bitmap
  lateinit var initBitmap: Bitmap


  override fun onDraw(canvas: Canvas?) {
    super.onDraw(canvas)
    canvas?.let {
      //绘制彩色图片
      it.drawBitmap(colorfulBitmap, 0f, 0f, paint)
      //保存图层到堆栈
      // val layerId: Int = it.saveLayer(
      //   0f,
      //   0f,
      //   width.toFloat(),
      //   height.toFloat(),
      //   paint,
      // )
      //当前图层也是顶层图层绘制黑白Btmap
      it.drawBitmap(initBitmap, 0f, 0f, paint)

      //PorterDuffXfermode 设置画笔的图形混合模式   取下层绘制非交集部分
      paint.xfermode = PorterDuffXfermode(DST_OUT)//画圆
      it.drawCircle((width / 2).toFloat(), (height / 2).toFloat(), 250f, paint)
    }
  }
}