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
    Imgproc.cvtColor(splitMat,rgbMat,Imgproc.COLOR_BGRA2RGB)
    val bitmap = Bitmap.createBitmap(rgbMat.width(),rgbMat.height(),Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(rgbMat,bitmap)
    findViewById<ImageView>(R.id.split_img).setImageBitmap(bitmap)

    //自动切割
    // 自动切割的原理是对整个图进行轮廓搜索，将搜索出来的结果按轮廓面积进行分析，将面积最合适的那个作为识别区与进行切割。
    // 这种方法的优点在于不用手动去确定切割点坐标，缺点是由于画面内容比较复杂，找到的轮廓可能会很多，分析轮廓的运算较大对Android设备带来较大的运算负担，
  // 造成处理速度慢。
    //
    // ROI(region of interest)——感兴趣区域。
    //
    // 这个区域是图像分析所关注的重点。圈定这个区域，以便进行进一步的处理。而且，使用ROI指定想读入的目标，可以减少处理时间，增加精度，
  // 给图像处理带来不小的便利。


  }
}