package com.example.opencv

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

//形态学
//https://zhuanlan.zhihu.com/p/269215933/
//https://blog.csdn.net/weixin_43418331/article/details/121795193
class MorphologyActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_morphology)
    // 图像形态学操作是基于形状的一系列图像处理操作的合集，主要是基于集合论基础上的形态学数学
    //
    // 形态学有四个基本操作：侵蚀、膨胀、开、闭
    // 膨胀与腐蚀是图像处理中最常用的形态学操作手段
    // 膨胀就是图像中的高亮部分进行膨胀，“领域扩张”，效果图拥有比原图更大的高亮区域。侵蚀就是原图中的高亮部分被腐蚀，“领域被蚕食”，
    //  效果图拥有比原图更小的高亮区域

    // 膨胀与侵蚀的主要功能如下：
    // 消除噪声
    // 分割出独立的图像元素，在图像中连接相邻的元素
    // 寻找图像中的明显的极大值区域或极小值区域
    // 求出图像的梯度
    //erode 腐蚀图像
    //dilate 膨胀操作

    // 开运算和闭运算
    // 开运算：先腐蚀后膨胀，用于移除由图像噪音形成的斑点    消除小物体;在纤细点处分离物体;平滑较大物体的边界的同时并不明显改变其面积。
    //        比如在二值化图像没处理好的时候会有一些白色的噪点,可以通过开运算进行消除。
    // 闭运算：先膨胀后腐蚀，用来连接被误分为许多小块的对象
    //     通常是被用来填充前景物体中的小洞,或者抹去前景物体上的小黑点。因为可以想象,其就是先将白色部分变大,把小的黑色部分挤掉,
    //       然后再将一些大的黑色的部分还原回来，整体得到的效果就是:抹去前景物体上的小黑点了

    // 在优化图象时，可以先执行开运算消除背景上的白色噪点，在执行闭运算消除前景色上的黑色杂色。
    //
    // 在执行开运算和闭运算之前我们要确定一个运算核,这个运算核是一个小矩阵。腐蚀运算就是在整张图像上计算给定内核区域的局部最小值,
    // 用最小值替换对应的像素值,而膨胀运算就是在整张图像上计算给定内核区域的局部最大值,用最大值替换对应的像索值。

    findViewById<ImageView>(R.id.img_view_noise).setImageDrawable(getDrawable(R.drawable.img_noise))
    val noiseMat = Utils.loadResource(this,R.drawable.img_noise)
    val grayMat = Mat()
    Imgproc.cvtColor(noiseMat,grayMat,Imgproc.COLOR_BGR2GRAY)
    val threMat = Mat()
    Imgproc.adaptiveThreshold(
      grayMat,
      threMat,
      255.0,
      Imgproc.ADAPTIVE_THRESH_MEAN_C,
      Imgproc.THRESH_BINARY,
      5,
      5.0
    )
    val bitmap = Bitmap.createBitmap(noiseMat.width(),noiseMat.height(),Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(threMat,bitmap)
    findViewById<ImageView>(R.id.img_view_noise_thres).setImageBitmap(bitmap)


    //这就是一个运算核，一个3x3的矩阵
    val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
    val noiseRemoveMat = Mat()
    //进行开运算
    Imgproc.morphologyEx(threMat,noiseRemoveMat,Imgproc.MORPH_OPEN,kernel)
    val noiseRemoveBitmap = Bitmap.createBitmap(noiseMat.width(),noiseMat.height(),Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(noiseRemoveMat,noiseRemoveBitmap)
    findViewById<ImageView>(R.id.img_view_noise_remove).setImageBitmap(noiseRemoveBitmap)

    //闭运算
    val kernelClose = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(5.0, 5.0))
    val ldMat = Utils.loadResource(this,R.drawable.pic_loudong)
    val ldGrayMat  = Mat()
    Imgproc.cvtColor(ldMat,ldGrayMat,Imgproc.COLOR_BGR2GRAY)
    val closeMat = Mat()
    //进行开运算
    Imgproc.morphologyEx(ldGrayMat,closeMat,Imgproc.MORPH_CLOSE,kernelClose)
    val closeBitmap = Bitmap.createBitmap(ldMat.width(),ldMat.height(),Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(closeMat,closeBitmap)
    findViewById<ImageView>(R.id.img_view_morph_close).setImageBitmap(closeBitmap)
  }
}