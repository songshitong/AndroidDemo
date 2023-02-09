package sst.example.androiddemo.feature.video.recorder

import android.R.attr.bitmap
import android.annotation.SuppressLint
import android.app.Notification
import android.app.Notification.Builder
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Bitmap.Config.ARGB_8888
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.Image
import android.media.ImageReader
import android.media.ImageReader.OnImageAvailableListener
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.Parcelable
import android.util.DisplayMetrics
import android.util.Log
import java.nio.Buffer
import java.nio.ByteBuffer

//android10后mediaProjection必须在service中
class RecorderService : Service() {
  companion object {
    const val NOTIFICATION_CHANNEL_ID = "id"
    const val NOTIFICATION_CHANNEL_NAME = "recorder"
    const val NOTIFICATION_CHANNEL_DESC = "recorder"
    const val NOTIFICATION_ID = 0xf2343
  }

  var imageReader : ImageReader? = null
  var mediaProjection:MediaProjection?=null
  private val mainHandler = object : Handler(Looper.getMainLooper()){
    override fun dispatchMessage(msg: Message) {
      super.dispatchMessage(msg)
      mActivityMessenger = msg.replyTo
    }
  }
  private val mServiceMessenger: Messenger = Messenger(mainHandler)
  private var mActivityMessenger: Messenger? = null

  override fun onBind(intent: Intent): IBinder? {
    val resultCode = intent.getIntExtra("resultCode", -1)
    val data = intent.getParcelableExtra<Parcelable>("data") as Intent
    val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    //必须在通知显示之后调用
    projectionManager.getMediaProjection(resultCode, data)?.let {
      mediaProjection = it
      createReader(it)
    }
    return mServiceMessenger.binder
  }

  override fun onCreate() {
    notification() //通知显示可以写到onCreate中。不管是写到onCreate里面还是onStartCommand中，都要写到getMediaProjection方法调用之前
    super.onCreate()
  }


  private fun notification() {
    if (VERSION.SDK_INT >= VERSION_CODES.O) {
      //Call Start foreground with notification
      val notificationIntent = Intent(
        this,
        RecorderService::class.java
      )
      val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)
      val notificationBuilder = Builder(this, NOTIFICATION_CHANNEL_ID)
        .setContentTitle("Recorder Service")
        .setContentText("Recorder service")
        .setContentIntent(pendingIntent)
      if (Build.VERSION.SDK_INT >= VERSION_CODES.S) {
        notificationBuilder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
      }


      val notification = notificationBuilder.build()
      val channel = NotificationChannel(
        NOTIFICATION_CHANNEL_ID,
        NOTIFICATION_CHANNEL_NAME,
        NotificationManager.IMPORTANCE_DEFAULT
      )
      channel.description = NOTIFICATION_CHANNEL_DESC
      val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
      notificationManager.createNotificationChannel(channel)
      //notificationManager.notify(NOTIFICATION_ID, notification);
      startForeground(
        NOTIFICATION_ID,
        notification
      ) //必须使用此方法显示通知，不能使用notificationManager.notify，否则还是会报上面的错误
    }
  }

  @SuppressLint("WrongConstant")
  private fun createReader(it: MediaProjection) {
    val dm: DisplayMetrics = resources.displayMetrics

    //输出大小  maxImages缓存几帧  使用ImageFormat.RGB_565报错   使用ImageFormat.JPEG要求宽高一致，可以取最大值，但是有黑边
    imageReader = ImageReader.newInstance(dm.widthPixels, dm.heightPixels, PixelFormat.RGBA_8888, 4)

    //创建虚拟屏幕，也就是真实屏幕的拷贝 虚拟屏幕的大小，dpi，绘制的surface等
    it.createVirtualDisplay(
      "RecordScreen",
      dm.widthPixels, dm.heightPixels, dm.densityDpi,
      DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
      imageReader!!.surface, null, null
    )
    imageReader!!.setOnImageAvailableListener(ImageListener(), null)
  }

  inner class ImageListener : OnImageAvailableListener {
    override fun onImageAvailable(reader: ImageReader?) {
      //有新的image生成才会回调
      reader?.let {
        val image = it.acquireLatestImage()
        //planes[0]根据格式不同，可能不同
        val byteBuffer = image.planes[0].buffer
        val bitmap = createBitmapFromRGBBuffer(image.planes)
        val msg = Message.obtain()
        msg.obj = bitmap
        mActivityMessenger?.send(msg)
        image.close()
      }
    }
  }

  private fun createBitmapFromRGBBuffer(planes: Array<Image.Plane>): Bitmap {
    // val bytes = ByteArray(byteBuffer.remaining())
    // byteBuffer.get(bytes)
    // //ImageFormat.JPEG格式直接转化为Bitmap格式。  非RGB或jpeg等需要转换
    // return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    val width =1280
    val height =720
    val imageBuffer: Buffer = planes.get(0).getBuffer().rewind()

    val pixelStride: Int = planes.get(0).getPixelStride()
    val rowStride: Int = planes.get(0).getRowStride()
    val rowPadding: Int = rowStride - pixelStride * width
    // create bitmap
   val bitmap = Bitmap.createBitmap(
      width + rowPadding / pixelStride, height,
      ARGB_8888
    )
    bitmap.copyPixelsFromBuffer(imageBuffer)
    return bitmap
  }

  override fun onDestroy() {
    imageReader?.close()
    mediaProjection?.stop()
    super.onDestroy()
  }


}
