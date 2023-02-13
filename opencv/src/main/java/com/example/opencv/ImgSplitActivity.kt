package com.example.opencv

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.imgproc.Imgproc

//https://blog.csdn.net/weixin_43418331/article/details/121795193
class ImgSplitActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_img_split)
    val initMat = Utils.loadResource(this,R.drawable.shanji)
    val splitRect = Rect(200,20,400,200)

    //手动切割
    val splitMat = Mat(initMat,splitRect)
    val rgbMat = Mat()
    //颜色转换为rgb
    Imgproc.cvtColor(splitMat,rgbMat,Imgproc.COLOR_BGR2RGB)
    val bitmap = Bitmap.createBitmap(rgbMat.width(),rgbMat.height(),Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(rgbMat,bitmap)
    findViewById<ImageView>(R.id.split_img).setImageBitmap(bitmap)
  }
}