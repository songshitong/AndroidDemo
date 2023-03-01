package com.example.opencv

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

//https://blog.csdn.net/weixin_43418331/article/details/121795193
class ColorConvertActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_color_convert)
    //颜色转换
    //灰度图
    val loadMat = Utils.loadResource(this, R.drawable.shanji)
    val grayMat = Mat()
    Imgproc.cvtColor(loadMat, grayMat, Imgproc.COLOR_BGRA2GRAY)
    val grayBitmap = Bitmap.createBitmap(loadMat.width(), loadMat.height(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(grayMat, grayBitmap)
    findViewById<ImageView>(R.id.img_gray).setImageBitmap(grayBitmap)


    //位图
    val bitMat = Mat()
    // THRESH_BINARY：当像素点灰度值 大于 thresh，像素点值为maxval，反之0
    // THRESH_BINARY_INV：当像素点灰度值 大于 thresh，像素点值为0，反之maxval
    // THRESH_TRUNC：当像素点灰度值 大于 thresh，像素点值为thresh，反之不变
    // THRESH_TOZERO：当像素点灰度值 大于 thresh，像素点值不变，反之为0
    // THRESH_TOZERO_INV：当像素点灰度值 大于 thresh，像素点值为0，反之不变
    //THRESH_OTSU 自动取阈值  最适用于双波峰，灰度图的直方图
    //THRESH_TRIANGLE 自动取阈值  适用于单个波峰，最开始用于医学分割细胞
    Imgproc.threshold(grayMat, bitMat, 125.0, 255.0, Imgproc.THRESH_BINARY)
    val bitmap = Bitmap.createBitmap(loadMat.width(), loadMat.height(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(bitMat, bitmap)
    findViewById<ImageView>(R.id.img_bit).setImageBitmap(bitmap)

    //自动设置阈值  适用于复杂的画面，明亮不均匀等，效果更好，但是速度更慢
    // openCV支持均值算法和高斯均值算法。不是计算全局图像的阈值，而是根据图像不同区域亮度分布，计算其局部阈值，所以对于不同区域，
    // 能够自适应计算不同的阈值，所以被叫做自适应阈值法。
    val bitAutoMat = Mat()
    //maxValue – 分配给满足条件的像素的非零值
    //adaptiveMethod – 要使用的自适应阈值算法，
    //    ADAPTIVE_THRESH_MEAN_C(平均计算）  阈值等于窗口大小为blockSize的临近像素点的平均值减去C
    //    ADAPTIVE_THRESH_GAUSSIAN_C（高斯计算） 阈值等于窗口大小为blockSize的临近像素点高斯窗口互相关加权和减去C。
    //thresholdType – 必须THRESH_BINARY或THRESH_BINARY_INV的阈值类型
    //blockSize – 用于计算像素阈值的像素邻域的大小：3、5、7、11、13 等    值越小，效果越精细
    //C – 从平均值或加权平均值中减去的常量。通常，它是正数，但也可以是零或负数
    Imgproc.adaptiveThreshold(
      grayMat,
      bitAutoMat,
      255.0,
      Imgproc.ADAPTIVE_THRESH_MEAN_C,
      Imgproc.THRESH_BINARY,
      5,
      5.0
    )
    val bitAutoBitmap = Bitmap.createBitmap(loadMat.width(),loadMat.height(),Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(bitAutoMat,bitAutoBitmap)
    findViewById<ImageView>(R.id.img_bit_auto).setImageBitmap(bitAutoBitmap)

    //颜色检测  某个颜色区域图像位于图像的什么地方
    // 检测imgHSV图像的每一个像素是不是在lowHSV和heigHSV之间，如果是就设置为255，否则为0，保存在imgThresholded中
    //函数会将位于两个区域间的值置为255，位于区间外的值置为0
    val cyanLoadMat = Utils.loadResource(this,R.drawable.img_color_cyan)
    val hsvMat = Mat()
    //转为hsv
    Imgproc.cvtColor(cyanLoadMat,hsvMat,Imgproc.COLOR_BGR2HSV)
    val cyanRangeMat = Mat()
    Core.inRange(hsvMat, Scalar(78.0, 43.0, 46.0), Scalar(99.0, 255.0, 255.0),cyanRangeMat)
    val cyanRangeBitmap = Bitmap.createBitmap(cyanLoadMat.width(),cyanLoadMat.height(),Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(cyanRangeMat,cyanRangeBitmap)
    findViewById<ImageView>(R.id.img_color_range).setImageBitmap(cyanRangeBitmap)
  }
}