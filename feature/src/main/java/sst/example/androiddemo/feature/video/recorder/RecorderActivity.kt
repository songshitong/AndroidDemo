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
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
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
    if(null == bitmap){
      val pain = Paint()
      pain.color = Color.RED
      bitmap = data
      val canvas = Canvas(data)
      canvas.drawCircle(0f,0f,50f,pain)
      imageView.setImageBitmap(data)
    }
  }



  override fun onDestroy() {
    super.onDestroy()
    connection?.let {
      unbindService(it)
    }
  }
}