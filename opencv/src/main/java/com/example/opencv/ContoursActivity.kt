package com.example.opencv

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.ImageView
import android.widget.LinearLayout
import com.example.opencv.MainActivity.Companion
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

//https://zhuanlan.zhihu.com/p/61328775
class ContoursActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_contours)

    val loadMat = Utils.loadResource(this,R.drawable.lunkuo)

    val inMat = Mat()
    val thresMat = Mat()
    Imgproc.cvtColor( loadMat,inMat, Imgproc.COLOR_RGBA2GRAY)
    Imgproc.threshold(inMat,thresMat, 0.0, 255.0, Imgproc.THRESH_BINARY_INV+ Imgproc.THRESH_OTSU)
    val contours = mutableListOf<MatOfPoint>()
    val outMat = Mat()
    //轮廓查找
    Imgproc.findContours(thresMat,
      contours,
      outMat,
      Imgproc.RETR_TREE,
      Imgproc.CHAIN_APPROX_SIMPLE)
    Log.d(MainActivity.TAG,"轮廓数量：${contours.size}")

    val group = findViewById<ViewGroup>(R.id.container)
    contours.forEach {
      Log.d(MainActivity.TAG,"轮廓 row:${it.rows()}")
      if(it.rows()<80){
        //简单过滤非圆形  圆形的row通常很大
        return@forEach
      }
      //Scalar是一个颜色组，取决于mat的模式  loadMat是RGBA
      //轮廓绘制
      Imgproc.drawContours(loadMat,contours,contours.indexOf(it), Scalar(255.0,0.0,0.0,255.0),10)
      val bitmap = Bitmap.createBitmap(loadMat.width(),loadMat.height(), Bitmap.Config.ARGB_8888)
      Utils.matToBitmap(loadMat,bitmap)
      val imgView = ImageView(this)
      val lp = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
      lp.topMargin = 80
      imgView.layoutParams = lp
      imgView.setImageBitmap(bitmap)
      group.addView(imgView)
    }
  }
}