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
    val loadMat = Utils.loadResource(this, R.drawable.shanji)
    val grayMat = Mat()
    Imgproc.cvtColor(loadMat, grayMat, Imgproc.COLOR_BGRA2GRAY)
    val grayBitmap = Bitmap.createBitmap(loadMat.width(), loadMat.height(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(grayMat, grayBitmap)
    findViewById<ImageView>(R.id.img_gray).setImageBitmap(grayBitmap)

    // **位图模式（二值图）：**是图像中最基本的格式,图像只有黑色和白色像素,是色彩模式中占有空间最小的,同样也叫做黑白图,它包含的信息量最少,
    // 无法包含图像中的细节,相当于只有0或者1，所以也叫二值图。一副彩色图如果要转換成黑白模式,则一般不能直接转換,需要首先将图像转換成灰度模式

    // **灰度模式：**即使用单一色调来表示图像,与位图模式不同,不像位图只有0和1,使用256级的灰度来表示图像,一个像素相当于占用8为一个字节,
    // 每个像素值使用0到255的亮度值代表,其中0为黑色, 255为白色,相当于从黑->灰->白的过度,通常我们所说的黑白照片就是这种模式,与位图模式相比,
    // 能表现出一定的细节,占用空间也比位图模式较大

    // **RGB模式：**为我们经常见到的,被称为真色彩。RGB模式的图像有3个颜色通道,分布为红(Red) ,绿(Green)和蓝(Bule) ,
    // 每个都占用8位一个字节来表示颜色信息,这样每个颜色的取值范围为0-255,那么就三种颜色就可以有多种组合,当三种基色的值相等时表现出为灰色,
    // 三种颜色都为255即为白色,三种颜色都为0,即为黑色.RGB模式的图像占用空间要比位图,灰度图都要大,但表现出的细节更加明显

    // **HSV模式：**是根据日常生活中人眼的视觉对色彩的观察得而制定的一套色彩模式,最接近与人类对色彩的辨认的思考方式,所有的颜色都是用色彩三属性来描述
    // H（色相）：是指从物体反射或透过物体传播的颜色     用角度度量，取值范围0-360，红色为0，绿色为120，蓝色240
    // S（饱和度）：是指颜色的强度或纯度，表示色相中灰色成分所占的比例    取值范围0.0-1.0，0为低饱和度，1为高饱和度
    // V（亮度）：是指颜色相对明暗程度，通常100%定义为白色；0%定位为黑色  取值范围0.0-1.0，0为黑色，1为白色
    // opencv的范围 H: 0— 180  S: 0— 255  V: 0— 255
    // 常用颜色值
    // 颜色	最小值	最大值
    // 橘黄色	0	22
    // 黄色	22	38
    // 绿色	38	75
    // 蓝色	75	130
    // 紫色	130	160
    // 红色	160	179

    //位图
    val bitMat = Mat()
    // THRESH_BINARY：当像素点灰度值 大于 thresh，像素点值为maxval，反之0
    // THRESH_BINARY_INV：当像素点灰度值 大于 thresh，像素点值为0，反之maxval
    // THRESH_TRUNC：当像素点灰度值 大于 thresh，像素点值为thresh，反之不变
    // THRESH_TOZERO：当像素点灰度值 大于 thresh，像素点值不变，反之为0
    // THRESH_TOZERO_INV：当像素点灰度值 大于 thresh，像素点值为0，反之不变

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