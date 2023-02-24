package com.example.opencv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.opencv.R.raw
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfRect
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

class FaceDetectActivity : AppCompatActivity() {
  lateinit var  classifier:CascadeClassifier
  lateinit var  classifierEye:CascadeClassifier
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_face_detect)
    // 模型下载 data目录
    //https://github.com/opencv/opencv/tree/17234f82d025e3bbfbf611089637e5aa2038e7b8/data/haarcascades
    //https://github.com/opencv/opencv/tree/17234f82d025e3bbfbf611089637e5aa2038e7b8/data/lbpcascades
    initClassifier()
    initClassifierEye()

    val faceBitmap = face(BitmapFactory.decodeResource(resources,R.drawable.img_women))
    // val eyeBitmap =  eye(BitmapFactory.decodeResource(resources,R.drawable.img_women))
    findViewById<ImageView>(R.id.img_face_detect).setImageBitmap(faceBitmap)

  }


  //取决于模型，并不准确
  fun face(bitmap: Bitmap?): Bitmap {
    val mat = Mat()
    val matdst = Mat()
    Utils.bitmapToMat(bitmap, mat)
    //把当前数据复制一份给matdst
    mat.copyTo(matdst)

    //1.把图片转为灰度图 BGR2GRAY，注意是BGR
    Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY)

    //2.定义MatOfRect用于接收人脸位置
    val faces = MatOfRect()

    //3.开始人脸检测，把检测到的人脸数据存放在faces中
    //objects：表示检测到的对象个数，返回每个对象的矩形BOX坐标
    //scaleFactor 尺度变化的比率，基本在1.05~1.2之间比较好
    //minNeighbors：领域范围内符合条件的对象个数，它是输出检测最终BOX的重要阈值，太大，则条件比较苛刻，用以丢失检测对象，太小，则容易导致错误检测
    //minSize：对象检测的最小范围
    //maxSize：对象检测的最大范围
    classifier.detectMultiScale(mat, faces, 1.05, 3, 0, Size(30.0, 30.0), Size())
    val faceList: List<Rect> = faces.toList()

    //4.判断是否存在人脸
    if (faceList.isNotEmpty()) {
      for (rect in faceList) {
        //5.根据得到的人脸位置绘制矩形框
        //rect.tl() 左上角
        //rect.br() 右下角
        Imgproc.rectangle(matdst, rect.tl(), rect.br(), Scalar(255.0, 0.0, 0.0, 255.0), 4)
      }
    }
    val resultBitmap: Bitmap =
      Bitmap.createBitmap(matdst.width(), matdst.height(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(matdst, resultBitmap)
    mat.release()
    matdst.release()
    return resultBitmap
  }

  fun eye(bitmap: Bitmap?):Bitmap{
    //先要检测出人脸位置，根据人脸位置取上半部分作为识别区域，这样符合人体构造，同时运算也会小很多
    //这里没有处理
    val mat = Mat()
    val matdst = Mat()
    Utils.bitmapToMat(bitmap, mat)
    mat.copyTo(matdst)
    Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY)
    val faces = MatOfRect()
    classifierEye.detectMultiScale(mat, faces, 1.05, 3, 0, Size(1.0, 1.0), Size())
    val faceList: List<Rect> = faces.toList()
    if (faceList.isNotEmpty()) {
      for (rect in faceList) {
        Imgproc.rectangle(matdst, rect.tl(), rect.br(), Scalar(255.0, 0.0, 0.0, 255.0), 4)
      }
    }
    val resultBitmap: Bitmap =
      Bitmap.createBitmap(matdst.width(), matdst.height(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(matdst, resultBitmap)
    mat.release()
    matdst.release()
    return resultBitmap
  }

  private fun initClassifier() {
    try {
      //读取存放在raw的文件
      val `is`: InputStream = resources
        .openRawResource(raw.lbpcascade_frontalface_improved)
      val cascadeDir: File = getDir("cascade", Context.MODE_PRIVATE)
      val cascadeFile = File(cascadeDir, "lbpcascade_frontalface_improved.xml")
      val os = FileOutputStream(cascadeFile)
      val buffer = ByteArray(4096)
      var bytesRead: Int
      //将文件存储在磁盘
      while (`is`.read(buffer).also { bytesRead = it } != -1) {
        os.write(buffer, 0, bytesRead)
      }
      `is`.close()
      os.close()
      //通过classifier来操作人脸检测， 在外部定义一个CascadeClassifier classifier，做全局变量使用
      classifier = CascadeClassifier(cascadeFile.absolutePath)
      cascadeFile.delete()
      cascadeDir.delete()
    } catch (e: IOException) {
      e.printStackTrace()
    }
  }

  /**
   * 眼睛
   * 初始化级联分类器
   */
  fun initClassifierEye() {
    try {
      //读取存放在raw的文件
      val `is` = resources
        .openRawResource(raw.haarcascade_eye_tree_eyeglasses)
      val cascadeDir = getDir("cascade", Context.MODE_PRIVATE)
      val cascadeFile = File(cascadeDir, "haarcascade_eye_tree_eyeglasses.xml")
      val os = FileOutputStream(cascadeFile)
      val buffer = ByteArray(4096)
      var bytesRead: Int
      while (`is`.read(buffer).also { bytesRead = it } != -1) {
        os.write(buffer, 0, bytesRead)
      }
      `is`.close()
      os.close()
      classifierEye = CascadeClassifier(cascadeFile.absolutePath)
      cascadeFile.delete()
      cascadeDir.delete()
    } catch (e: IOException) {
      e.printStackTrace()
    }
  }
}