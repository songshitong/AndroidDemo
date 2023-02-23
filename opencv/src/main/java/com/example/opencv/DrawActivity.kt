package com.example.opencv

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import org.opencv.android.Utils
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

class DrawActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_draw)
    //可以通过Imgproc相关api进行绘制，也可以将mat转为bitmap后使用canvas绘制 Canvas(Bitmap).drawCircle()
    // 常见api line（直线）、rectangle（矩形）、polylines（多边形）、circle（圆形）、ellipse（椭圆形）
    //putText 绘制文字  OpenCV对中文支持不太行，官方建议是将中文转换为图片再显示
    val loadMat = Utils.loadResource(this, R.drawable.shanji)
    Imgproc.line(
      loadMat, Point(0.0, 0.0), Point(
        loadMat.width().toDouble(),
        loadMat.height().toDouble()
      ), Scalar(255.0, 0.0, 0.0, 255.0) //Scalar是对应mat的颜色格式
    )
    val lineBitmap = Bitmap.createBitmap(loadMat.width(),loadMat.height(),Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(loadMat,lineBitmap)
    findViewById<ImageView>(R.id.img_draw_line).setImageBitmap(lineBitmap)
  }
}