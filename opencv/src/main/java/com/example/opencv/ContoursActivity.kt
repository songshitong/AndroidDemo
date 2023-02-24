package com.example.opencv

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

//https://zhuanlan.zhihu.com/p/61328775
class ContoursActivity : AppCompatActivity() {
  val tag = "Contours"
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_contours)

    val loadMat = Utils.loadResource(this,R.drawable.lunkuo)

    val inMat = Mat()
    val thresMat = Mat()
    Imgproc.cvtColor( loadMat,inMat, Imgproc.COLOR_BGRA2GRAY)
    Imgproc.threshold(inMat,thresMat, 0.0, 255.0, Imgproc.THRESH_BINARY_INV+ Imgproc.THRESH_OTSU)
    val contours = mutableListOf<MatOfPoint>()
    val outMat = Mat()
    //轮廓查找

    //hierarchy 可选的输出。包含轮廓之间的联系。4通道矩阵,元素个数为轮廓数量。通道 [ 0 ] ~通道 [ 3 ]对应保存:后个轮廓下标,前一个轮廓下标,父轮廓下标,内嵌轮廓下标。
    // 如 果没有后一个,前一个,父轮廓,内嵌轮廓,那么该通道的值为-1
    //mode
    // RETR_EXTERNAL 只检测最外围的轮廓
    // RETR_LIST 检测所有轮廓，不建立等级关系，彼此独立
    // RETR_CCOMP 检测所有轮廓，但所有轮廓只建立两个等级关系
    // RETR_TREE 检测所有轮廓，并且所有轮廓建立一个树结构，层级完整
    // method 轮廓近似法
    // CHAIN_APPROX_NONE 保存物体边界上所有连续的轮廓点
    // CHAIN_APPROX_SIMPLE 压缩水平方向，垂直方向，对角线方向的元素，只保留该方向的终点坐标，例如一个矩形轮廓只需要4个点来保存轮廓信息
    // CV_CHAIN_APPROX_TC89_L1 使用Teh-Chin链近似算法
    // CV_CHAIN_APPROX_TC89_KCOS 使用Teh-Chin链近似算法
    Imgproc.findContours(thresMat,
      contours,
      outMat,
      Imgproc.RETR_TREE,
      Imgproc.CHAIN_APPROX_SIMPLE)
    Log.d(tag,"轮廓数量：${contours.size}")

    val group = findViewById<ViewGroup>(R.id.container)
    contours.forEach {
      Log.d(tag,"轮廓 row:${it.rows()}")
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

    // 形状识别
    // 先要使用形状拟合来过滤一些突出的顶点，再用根据顶点识别出形状
    // 形状的拟合
    // 轮廓点集合找到以后我们可以通过多边形拟合的方式来寻找由轮廓点所组成的多边形的顶点。 approxPolyDP()函数功能是把·个连续光滑曲线折线化,
    // 对图像轮廓点进行多边形拟合。简单来说就是该函数是用1一条具有较少顶点的曲线/多边形去逼近另一条具有较多顶点的曲线或多边形。
    //
    // approxPolypDP()函数的原理如下：
    // 在曲线首尾两点A, B之间连接一条直线AB,该直线为曲线的弦;
    // 得到曲线上离该直线段距离最大的点C,计算其与AB的距离b;
    // 比较该距离与预先给定的阈值threshold的大小,如果小于threshold,则该直线段作为曲线的近似,该段曲线处理完毕。
    // 如果距离大于阈值,则用C将曲线分为两段AC和BC,并分别对两段取信进行1-3的处理。
    // 当所有曲线都处理完毕时,依次连接各个分割点形成的折线,即可以作为曲线的近似

    contours.forEach {
      //获取某一个形状的顶点的集合
      val contour2f = MatOfPoint2f()
      contour2f.fromList(it.toList())
      //逼近精度epsilon计算  也可以手动指定  获取轮廓的周长
      val epsilon = 0.04 * Imgproc.arcLength(contour2f, true)
      val approxCurve = MatOfPoint2f()
      Imgproc.approxPolyDP(contour2f, approxCurve, epsilon, true)
      Log.d(tag," 拟合曲线的行数:${approxCurve.rows()}")

      //轮廓面积
      Log.d(tag,"轮廓面积:${Imgproc.contourArea(it)}")

      // 形状的识别
      // 顶点判断法
      // 形状识别的方法有很多种,本案例采用最简单的一种就是直接根据多边形顶点的个数进行判断。这种方法最简单但**精度不高,**只能识别差别较大的几种形状
      //https://blog.csdn.net/weixin_43418331/article/details/121795193
      if(approxCurve.rows()==3){
        Log.d(tag,"三角形")
      }
      if(approxCurve.rows()==4){
        Log.d(tag,"矩形")
      }
      if(approxCurve.rows()>20){
        Log.d(tag,"圆形")
      }

      // 信号分析法
      // 使用Moments ()函数计算多边形的重点,求绕多边形一周重心到多边形轮廓线的距离。把距离值形成信号曲线图,我们可以看到不同的形状信号曲线图区别很大
      // 。信号分析法可以识别多种类型的多边形形状
    }
  }
}