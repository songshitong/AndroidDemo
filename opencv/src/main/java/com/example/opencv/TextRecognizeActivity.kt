package com.example.opencv

import android.Manifest.permission
import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.baidu.ai.edge.core.infer.InferConfig
import com.baidu.ai.edge.core.infer.InferManager
import com.googlecode.tesseract.android.TessBaseAPI
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class TextRecognizeActivity : AppCompatActivity() {
  val tag = "TextRecognitionActivity"
  lateinit var tessAPI:TessBaseAPI
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_text_recognition)
    val imgView = findViewById<ImageView>(R.id.text_recognize_result)
    tessAPI = TessBaseAPI { progressValues ->
      progressValues?.let {
        Log.d(tag, "progress $it.percent")
      }
    }
    // java.lang.IllegalArgumentException: Data path must contain subfolder tessdata!
    //需要下载对应语言的traineddata https://github.com/tesseract-ocr/tessdata
    val path = extractAssets(this,"eng.traineddata").path
    Log.d(tag,"data is $path")
    //路径不包括tessdata，SDK会自动拼接   OEM_LSTM_ONLY为精准模式
    val init = tessAPI.init(cacheDir.path,"eng",TessBaseAPI.OEM_LSTM_ONLY)
    Log.d(tag,"Tess api init:$init")


    val loadMat = Utils.loadResource(this,R.drawable.text_pic_eng)
    val inMat = Mat()
    val thresMat = Mat()
    Imgproc.cvtColor( loadMat,inMat, Imgproc.COLOR_BGRA2GRAY)
    Imgproc.threshold(inMat,thresMat, 0.0, 255.0, Imgproc.THRESH_BINARY_INV+ Imgproc.THRESH_OTSU)
    val bitmap = Bitmap.createBitmap(loadMat.width(),loadMat.height(),Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(thresMat,bitmap)
    imgView.setImageBitmap(bitmap)

    Thread{
      tessAPI.setImage(bitmap)
      // 触发识别进度
      tessAPI.getHOCRText(0)
      //阻塞接口
      val text: String = tessAPI.utF8Text
      Log.d(tag,"recognize text $text")
      tessAPI.stop()
    }.start()


    ActivityCompat.requestPermissions(
      this, arrayOf(
        permission.WRITE_EXTERNAL_STORAGE,
        permission.READ_PHONE_STATE,
        permission.READ_EXTERNAL_STORAGE
      ), 1
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
      val intent = Intent()
      intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
      startActivityForResult(intent, 123)
    }

    try {
      // step 1: 准备配置类
      val config = InferConfig(assets, "infer")

      // step 2: 准备预测 Manager
      val manager = InferManager(this, config, "")

      // step 3: 准备待预测的图像，必须为 Bitmap.Config.ARGB_8888 格式，一般为默认格式
      val image: Bitmap = BitmapFactory.decodeResource(resources,R.drawable.shanji)
      val imageMat = Mat()
      Utils.bitmapToMat(image,imageMat)
      val grayMat = Mat()
      Imgproc.cvtColor(imageMat,grayMat,Imgproc.COLOR_BGRA2GRAY)
      val tresMat = Mat()
      Imgproc.adaptiveThreshold(
        grayMat,
        tresMat,
        255.0,
        Imgproc.ADAPTIVE_THRESH_MEAN_C,
        Imgproc.THRESH_BINARY,
        5,
        5.0
      )
      val thresBitmap = Bitmap.createBitmap(image.width,image.height,Bitmap.Config.ARGB_8888)
      Utils.matToBitmap(tresMat,thresBitmap)

      // val paddleBitmap = Bitmap.createBitmap(image.width,image.height,Bitmap.Config.ARGB_8888)
      // step 4: 识别图像
      val results = manager.ocr(image, 0.3f)
      val canvas = Canvas(thresBitmap)
      // canvas.drawBitmap(image,0f,0f,null)
      val paint = Paint()
      paint.color = Color.GREEN
      paint.style = Paint.Style.FILL_AND_STROKE

      // step 5: 解析结果
      for (resultModel in results) {
        resultModel.points.forEach { //点是文字的四个角
          canvas.drawCircle(it.x.toFloat(), it.y.toFloat(),3f,paint)
        }
        Log.i(
         tag, "labelIndex=" + resultModel.labelIndex
            + ", labelName=" + resultModel.label
            + ", confidence=" + resultModel.confidence
        )
      }
      findViewById<ImageView>(R.id.text_paddle_result).setImageBitmap(thresBitmap)
      // step 6: 释放资源。预测完毕请及时释放资源
      manager.destroy()
    } catch (e: Exception) {
      Log.e(tag, e.message!!)
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    tessAPI.recycle()
  }


  fun extractAssets(context: Context,dataName:String):File {
    val am: AssetManager = context.assets
    val tessDir = File(cacheDir, "tessdata")
    if (!tessDir.exists()) {
      tessDir.mkdir()
    }
    val engFile = File(tessDir, dataName)
    if (!engFile.exists()) {
      copyFile(am, dataName, engFile)
    }
    return tessDir
  }

  private fun copyFile(
    am: AssetManager, assetName: String,
    outFile: File
  ) {
    try {
      am.open(assetName).use { `in` ->
        FileOutputStream(outFile).use { out ->
          val buffer = ByteArray(1024)
          var read: Int
          while (`in`.read(buffer).also { read = it } != -1) {
            out.write(buffer, 0, read)
          }
        }
      }
    } catch (e: IOException) {
      e.printStackTrace()
    }
  }
}