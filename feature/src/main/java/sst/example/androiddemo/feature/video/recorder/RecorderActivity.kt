package sst.example.androiddemo.feature.video.recorder

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import sst.example.androiddemo.feature.R

//录屏思路
//MediaProjection和MediaProjectionManager 负责权限申请，使用一个surface作为输出
// MediaRecorder提供surface输出到文件，录音控制等
//ImageReader提供surface，实时获取图像数据
//mediaCode或者ffmpeg提供surface，然后进行图像数据处理
// mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
// mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
// mInputSurface = mEncoder.createInputSurface(); //这⾥输出的 Surface 可以输⼊给VirtualDisplay
// //直接开启编码器
// mEncoder.start();

class RecorderActivity : AppCompatActivity() {
  private val mRequestCode = 0x123
  var bitmap: Bitmap? = null
  lateinit var imageView : ImageView
  var serviceMessenger: Messenger?=null
  var activityMessenger: Messenger?=null
  private var connection: ServiceConnection? =null
  private val tag = "RecorderActivity"
  private val mainHandler = object :Handler(Looper.getMainLooper()){
    override fun dispatchMessage(msg: Message) {
      super.dispatchMessage(msg)
      save(msg.obj as Bitmap)
    }
  }


  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_recorder)
    imageView = findViewById(R.id.recorder_show)
    activityMessenger = Messenger(mainHandler)
    startActivityForResult(Intent(createProjectionIntent()), mRequestCode)
    OpenCVLoader.initDebug()
  }

  private fun createProjectionIntent(): Intent {
    return (getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager).createScreenCaptureIntent()
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (mRequestCode == requestCode && null != data) {
      connection =  object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
          //获取Service端的Messenger
          serviceMessenger = Messenger(service)

          val msg = Message.obtain()
          msg.replyTo = activityMessenger
          serviceMessenger!!.send(msg)
        }

        override fun onServiceDisconnected(name: ComponentName?) {}
      }
      val intent = Intent(
        this,
        RecorderService::class.java
      )
      intent.putExtra("resultCode",resultCode)
      intent.putExtra("data",data)
      bindService(intent, connection as ServiceConnection, Service.BIND_AUTO_CREATE)
    }
  }




  private fun save(data: Bitmap) {
    // if(null == bitmap){
      val pain = Paint()
      pain.color = Color.RED
      bitmap = data
      val canvas = Canvas(data)
      canvas.drawCircle(0f,0f,50f,pain)
      imageView.setImageBitmap(data)

    // }

    // val loadMat = Mat()
    // Utils.bitmapToMat(bitmap,loadMat)
    //
    // val inMat = Mat()
    // val thresMat = Mat()
    // Imgproc.cvtColor( loadMat,inMat, Imgproc.COLOR_RGBA2GRAY)
    // Imgproc.threshold(inMat,thresMat, 0.0, 255.0, Imgproc.THRESH_BINARY_INV+ Imgproc.THRESH_OTSU)
    // val contours = mutableListOf<MatOfPoint>()
    // val outMat = Mat()
    // //轮廓查找
    // Imgproc.findContours(thresMat,
    //   contours,
    //   outMat,
    //   Imgproc.RETR_TREE,
    //   Imgproc.CHAIN_APPROX_SIMPLE)
    // Log.d(tag,"轮廓数量：${contours.size}")

    findBitmap(data)
  }

  fun findBitmap(allBitmap:Bitmap):Bitmap{
    val imgMat = Mat()
    Utils.bitmapToMat(allBitmap,imgMat)
    val templMat = Utils.loadResource(this,R.drawable.skill_mingwang)
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
    return bitmap
  }



  override fun onDestroy() {
    super.onDestroy()
    connection?.let {
      unbindService(it)
    }
  }
}