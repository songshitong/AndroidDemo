package com.example.opencv

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

//https://zhuanlan.zhihu.com/p/62643151
class TemplateMatchActivity : AppCompatActivity() {
  val tag = "Template"
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_template_match)
    //模板匹配是用来在一幅大图里搜寻模板图像位置的方法
    val imgMat = Utils.loadResource(this,R.drawable.shanji)
    val templMat = Utils.loadResource(this,R.drawable.skill_wake)
    val templGray = Mat()
    val grayMat = Mat()
    Imgproc.cvtColor(imgMat,grayMat,Imgproc.COLOR_RGB2GRAY)
    Imgproc.cvtColor(templMat,templGray,Imgproc.COLOR_RGB2GRAY)
    val resultMat = Mat()
    Imgproc.matchTemplate(grayMat,templGray,resultMat,Imgproc.TM_SQDIFF_NORMED)
    val mmr = Core.minMaxLoc(resultMat)
    val point = mmr.minLoc
    Log.d(tag,"point is $point")
    Imgproc.rectangle(
      imgMat, point, Point(point.x + templMat.cols(), point.y + templMat.rows()),
      Scalar(255.0,0.0,0.0,255.0), 5)
    val bitmap = Bitmap.createBitmap(imgMat.width(),imgMat.height(),Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(imgMat,bitmap)
    findViewById<ImageView>(R.id.math_result).setImageBitmap(bitmap)
  }
}