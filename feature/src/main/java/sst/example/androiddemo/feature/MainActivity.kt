package sst.example.androiddemo.feature

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.app.ActivityManager
import android.app.Application.getProcessName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ResolveInfo
import android.graphics.Point
import android.media.MediaPlayer
import android.os.*
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.provider.Settings
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.MutableLiveData
import sst.example.androiddemo.feature.SystemBug.ToastBugActivity
import sst.example.androiddemo.feature.activity.*
import sst.example.androiddemo.feature.activity.launchmode.LaunchActivity
import sst.example.androiddemo.feature.activity.scrollNested.ScrollNestedActivity
import sst.example.androiddemo.feature.activity.testAnr.TestAnrActivity
import sst.example.androiddemo.feature.animation.KeyFrameActivity
import sst.example.androiddemo.feature.animation.LayoutAnimationActivity
import sst.example.androiddemo.feature.animation.MotionLayoutActivity
import sst.example.androiddemo.feature.animation.RevealAnimatorActivity
import sst.example.androiddemo.feature.animation.ViewPropertyAnimatorActivity
import sst.example.androiddemo.feature.animation.activity.ActivityAnimation
import sst.example.androiddemo.feature.animation.activity.ActivityTransition
import sst.example.androiddemo.feature.animation.dynamicanimation.DynamicAnimaitonActivity
import sst.example.androiddemo.feature.animation.evaluator.TypeEvaluatorActivity
import sst.example.androiddemo.feature.ffmpeg.FFmpegActivity
import sst.example.androiddemo.feature.graphics.*
import sst.example.androiddemo.feature.resources.XmlParserActivity
import sst.example.androiddemo.feature.service.IntentServiceActivity
import sst.example.androiddemo.feature.util.MyUtils
import sst.example.androiddemo.feature.video.VideoParserActivity
import sst.example.androiddemo.feature.video.recorder.RecorderActivity
import sst.example.androiddemo.feature.wallpaper.NormalWallpaperService
import sst.example.androiddemo.feature.webview.JumpActivity
import sst.example.androiddemo.feature.widget.CameraMatrixActivity
import sst.example.androiddemo.feature.widget.SystemView
import sst.example.androiddemo.feature.widget.ViewOutlineProviderActivity
import sst.example.androiddemo.feature.widget.layout.constraint.ConstrainLayoutActivity
import sst.example.androiddemo.feature.widget.layout.LinearLayoutActivity
import sst.example.androiddemo.feature.widget.layout.repeatMeasure.MeasureTestActivity
import sst.example.androiddemo.feature.widget.practice.recyclerview.customLayoutManager.RVCustomLayoutManagerActivity
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.math.pow
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

  private val TAG = "MainActivity"

  @RequiresApi(VERSION_CODES.P)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
      Log.d(TAG, "PhysicsScreenSize: ${getPhysicsScreenSize(applicationContext)}")
    getAndroidInfo()
    getMethodTrace()
    // ToastUtil.customSnackbar(this,"测试SnackBar",Snackbar.LENGTH_LONG)
    var startTime = System.currentTimeMillis()
    val sp: SharedPreferences = getSharedPreferences("sp", Context.MODE_PRIVATE)
    sp.edit().putString("a", "11111").apply();
    val value: String? = sp.getString("a", "")
    Log.e(TAG, "func1  :  ${System.currentTimeMillis() - startTime}")
    //android jvm不跟随Java jvm版本 所以无法获得具体的Java Version
    Log.d(TAG, "java version ${getVersion()}")
    Log.d(TAG, "java version ${Runtime::class.java.getPackage().implementationVersion}")
    Log.d(TAG, " onCreate ==== ")
    // Example of a call to a native method
//        sample_text.text = stringFromJNI()
    findViewById<View>(R.id.mainNormalActivity).setOnClickListener {
      val intent = Intent(this, TestOrientationActivity::class.java)
      startActivity(intent)
    }
    findViewById<View>(R.id.launchActivity).setOnClickListener {
      val intent = Intent(this, LaunchActivity::class.java)
      startActivity(intent)
    }
    findViewById<View>(R.id.main_dialog).setOnClickListener {
      val dialog = AlertDialog.Builder(this).create()
      dialog.setTitle("dialog")
      dialog.setButton(DialogInterface.BUTTON_POSITIVE, "确定") { dialogInterface, i ->
        dialog.dismiss()
      }
      dialog.show()
    }
    findViewById<View>(R.id.mainDialogActivity).setOnClickListener {
      val intent = Intent(this, DialogActivity::class.java)
      startActivity(intent)
    }
    findViewById<View>(R.id.go2Fragment).setOnClickListener {
      val intent = Intent(this, FragmentActivity::class.java)
      startActivity(intent)
    }
    findViewById<View>(R.id.go2Menu).setOnClickListener {
      val intent = Intent(this, MenuActivity::class.java)
      startActivity(intent)
    }

    findViewById<View>(R.id.go2Service).setOnClickListener {
      val intent = Intent(this, ServicesActivity::class.java)
      startActivity(intent)
    }
      findViewById<View>(R.id.layoutWeight).setOnClickListener {
      val intent = Intent(this, LinearLayoutActivity::class.java)
      startActivity(intent)
    }

      findViewById<TextView>(R.id.sample_text).text = "BitMap"
      findViewById<View>(R.id.sample_text).setOnClickListener {
      val intent = Intent(this, BitmapActivity::class.java)
      startActivity(intent)
    }

      findViewById<View>(R.id.pictureView).setOnClickListener {
      val intent = Intent(this, PictureDrawableActivity::class.java)
      startActivity(intent)
    }
    findViewById<View>(R.id.canvasActivity).setOnClickListener {
      val intent = Intent(this, CanvasActivity::class.java)
      startActivity(intent)
    }
      findViewById<View>(R.id. paintActivity).setOnClickListener {
      val intent = Intent(this, PaintActivity::class.java)
      startActivity(intent)
    }
      findViewById<View>(R.id.drawableActivity).setOnClickListener {
      val intent = Intent(this, DrawableActivity::class.java)
      startActivity(intent)
    }
      findViewById<View>(R.id.pathActivity).setOnClickListener {
      val intent = Intent(this, PathActivity::class.java)
      startActivity(intent)
    }

      findViewById<View>(R.id.layoutAnimationActivity).setOnClickListener {
      val intent = Intent(this, LayoutAnimationActivity::class.java)
      startActivity(intent)
    }
    findViewById<View>(R.id.particleActivity).setOnClickListener {
      val intent = Intent(this, ParticleActivity::class.java)
      startActivity(intent)
    }
      findViewById<View>(R.id.loadingActivity).setOnClickListener {
      val intent = Intent(this, LoadingActivity::class.java)
      startActivity(intent)
    }
      findViewById<View>(R.id.qqDragBubbleActivity).setOnClickListener {
      val intent = Intent(this, QQDragBubbleActivity::class.java)
      startActivity(intent)
    }
      findViewById<View>(R.id.stickyRecyclerviewAct).setOnClickListener {
      val intent = Intent(this, StickyActivity::class.java)
      startActivity(intent)
    }

      findViewById<View>(R.id.springAnimation).setOnClickListener {
      val intent = Intent(this, DynamicAnimaitonActivity::class.java)
      startActivity(intent)
    }
    findViewById<View>(R.id.oscillationView).setOnClickListener {
      val intent = Intent(this, OscillationActivity::class.java)
      startActivity(intent)
    }
      findViewById<View>(R.id.growingTreeView).setOnClickListener {
      val intent = Intent(this, GrowingTreeActivity::class.java)
      startActivity(intent)
    }
      findViewById<View>(R.id.xmlParserBtn).setOnClickListener {
      val intent = Intent(this, XmlParserActivity::class.java)
      startActivity(intent)
    }
      findViewById<View>(R.id.wallpaperBtn).setOnClickListener {
//            val intent = Intent(this, SettingActivity::class.java)
//            startActivity(intent)
      startActivity(MyUtils.getWallPaper(this, NormalWallpaperService::class.java))

    }
    findViewById<View>(R.id.jumpBtn).setOnClickListener {
      val intent = Intent(this, JumpActivity::class.java)
      startActivity(intent)
    }
      findViewById<View>(R.id.videoParser).setOnClickListener {
      val intent = Intent(this, VideoParserActivity::class.java)
      startActivity(intent)
    }
      findViewById<View>(R.id.shareBtn).setOnClickListener {
      shareText("这是内容")
    }
      findViewById<View>(R.id.ffmpeg).setOnClickListener {
      val intent = Intent(this, FFmpegActivity::class.java)
      startActivity(intent)
    }

//      系统铃声 RingtoneManager  https://blog.csdn.net/ch853199769/article/details/78721003
      findViewById<View>(R.id.ringtone).setOnClickListener {
      val player: MediaPlayer = MediaPlayer.create(
        this,
        ///系统铃声
//                Settings.System.DEFAULT_RINGTONE_URI
        ///系统通知  华为手机崩溃
        Settings.System.DEFAULT_NOTIFICATION_URI
      )
      player.start()
    }
    findViewById<View>(R.id.startActivityForResult).setOnClickListener {
      val intent = Intent(this, StartForResultActivity::class.java)
      startActivity(intent)

    }

    findViewById<View>(R.id.whatsapp).setOnClickListener {
      val intentShareList: MutableList<Intent> = ArrayList()
      val shareIntent = Intent()
      shareIntent.action = Intent.ACTION_SEND
      shareIntent.type = "text/plain"
      //shareIntent.setType("image/*");
      //shareIntent.setType("image/*");
      val resolveInfoList: List<ResolveInfo> =
        packageManager.queryIntentActivities(shareIntent, 0)

      for (resInfo in resolveInfoList) {
        val packageName: String = resInfo.activityInfo.packageName
        val name: String = resInfo.activityInfo.name
        Log.d("System Out", "Package Name : $packageName")
        Log.d("System Out", "Name : $name")
        if (packageName.contains("com.facebook") ||
          packageName.contains("com.whatsapp")
        ) {
          Log.d("System Out", "Name : $name")

//                    val intent = Intent()
//                    intent.component = ComponentName(packageName, name)
//                    intent.action = Intent.ACTION_SEND
//                    intent.type = "text/plain"
//                    intent.putExtra(Intent.EXTRA_SUBJECT, articleName)
//                    intent.putExtra(
//                        Intent.EXTRA_TEXT,
//                        articleName.toString() + "\n" + articleContent
//                    )
//                    val dr: Drawable = ivArticleImage.getDrawable()
//                    val bmp: Bitmap = (dr.getCurrent() as GlideBitmapDrawable).getBitmap()
//                    intent.putExtra(Intent.EXTRA_STREAM, getLocalBitmapUri(bmp))
//                    intent.type = "image/*"
//                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//                    intentShareList.add(intent)
        }
      }
      try {
//                  //  https://faq.whatsapp.com/android/im-an-android-developer-how-can-i-integrate-whatsapp-with-my-app
        val vIt = Intent("android.intent.action.SEND")
        vIt.setPackage("com.whatsapp")
        vIt.type = "text/plain"
        vIt.putExtra(Intent.EXTRA_TEXT, "this is share")
        startActivity(vIt)
      } catch (ex: Exception) {
        Log.e(TAG, "whatsAppShare:$ex")
      }

    }

    //ExifInterface
    // 从指定路径下读取图片，并获取其EXIF信息
//        val exifInterface = ExifInterface(FileUtil.getPathFromUri(uri))
//        // 获取图片的旋转信息
//        // 获取图片的旋转信息
//        val orientation: Int = exifInterface.getAttributeInt(
//            ExifInterface.TAG_ORIENTATION,
//            ExifInterface.ORIENTATION_NORMAL
//        )
//        when (orientation) {
//            ExifInterface.ORIENTATION_ROTATE_90 -> degree = 90
//            ExifInterface.ORIENTATION_ROTATE_180 -> degree = 180
//            ExifInterface.ORIENTATION_ROTATE_270 -> degree = 270
//        }

    //测试手机1S计算的次数  红米note4大约20000次
    val time = System.currentTimeMillis()
    var index = 0
    Thread(Runnable {
      do {
        index++
      } while (time + 1000 >= System.currentTimeMillis())
      Log.d(TAG, "index value  $index")
    }).start();


    //handler原理为什么顺序是021  runnable运行的线程不是主线程吗
    //02是在本次消息,runnable的1需要另外的消息发送处理和执行
    findViewById<View>(R.id.startHandler).setOnClickListener {
      val runnable = Runnable {
        Log.d(TAG, "1 runnable")
      }
      val handler = Handler()
      Log.d(TAG, "0")
      handler.post(runnable)
      Log.d(TAG, "2")
    }

    val permissions = arrayOf(
      Manifest.permission.WRITE_EXTERNAL_STORAGE,
      Manifest.permission.READ_EXTERNAL_STORAGE,
      Manifest.permission.RECORD_AUDIO
    )
    ActivityCompat.requestPermissions(
      this,
      permissions, 0
    )


    //WindowManager.LayoutParams.FLAG_SECURE  禁止截屏
    //安全 监听截屏事件的产生  系统源码 TakeScreenshotService  GlobalScreenshot
    //  ContentObserver 监听图片的变化。。

    //方法论  结构化编程的经验显示，改进代码的一种主要方法即为将其分解为更小的片段

//        这个例子创建了Integer类，其本身只定义了integer属性，然后增加了两个分类Arithmetic与Display以扩展类的功能。
//        虽然分类可以访问类的私有成员，但通常利用属性的访问方法来访问是一种更好的做法，可以使得分类与原有类更加独立。
//        这是分类的一种典型应用—另外的应用是利用分类来替换原有类中的方法，虽然用分类而不是继承来替换方法不被认为是一种好的做法

    //重构方法，将方法和判空一起重构，
    //方法的注释带例子，json,表结构，HTML实例等

    //自定义相册 content provider 或其他？
    findViewById<View>(R.id.testAnrActivity).setOnClickListener {
      startActivity(Intent(this, TestAnrActivity::class.java))
    }
      findViewById<View>(R.id.measureTestActivity).setOnClickListener {
      startActivity(Intent(this, MeasureTestActivity::class.java))
    }
      findViewById<View>(R.id.customLMActivity).setOnClickListener {
      startActivity(Intent(this, RVCustomLayoutManagerActivity::class.java))
    }
      findViewById<View>(R.id.scrollNestedActivity).setOnClickListener {
      startActivity(
        Intent(
          this,
          ScrollNestedActivity::class.java
        )
      )
    }

    findViewById<View>(R.id.toastBugActivity).setOnClickListener {
      startActivity(
        Intent(
          this,
          ToastBugActivity::class.java
        )
      )
    }

      findViewById<View>(R.id.bigPictureActivity).setOnClickListener {
      startActivity(
        Intent(
          this,
          BigPictureActivity::class.java
        )
      )
    }

    findViewById<View>(R.id.IntentServiceActivity).setOnClickListener {
      startActivity(
        Intent(
          this,
          IntentServiceActivity::class.java
        )
      )
    }
      findViewById<View>(R.id.IntentConstrainLayoutActivity).setOnClickListener {
      startActivity(
        Intent(
          this,
          ConstrainLayoutActivity::class.java
        )
      )
    }
      findViewById<View>(R.id.IntentRevealAnimatorActivity).setOnClickListener {
      startActivity(
        Intent(
          this,
          RevealAnimatorActivity::class.java
        )
      )
    }
    findViewById<View>(R.id.IntentTypeEvaluatorActivity).setOnClickListener {
      startActivity(
        Intent(
          this,
          TypeEvaluatorActivity::class.java
        )
      )
    }
    findViewById<View>(R.id.IntentActivityAnimation).setOnClickListener {
      startActivity(
        Intent(
          this,
          ActivityAnimation::class.java
        )
      )
    }
      findViewById<View>(R.id.IntentActivityTransition).setOnClickListener {
      startActivity(
        Intent(
          this,
          ActivityTransition::class.java
        ), ActivityOptionsCompat.makeSceneTransitionAnimation(this).toBundle()
      )
    }
      findViewById<View>(R.id.ActivityKeyFrame).setOnClickListener {
      startActivity(
        Intent(
          this,
          KeyFrameActivity::class.java
        )
      )
    }
    findViewById<View>(R.id.dialogFragmentBtn).setOnClickListener {
      startActivity(
        Intent(
          this,
          DialogFragmentActivity::class.java
        )
      )
    }
      findViewById<View>(R.id.viewOutlineProviderBtn).setOnClickListener {
      startActivity(
        Intent(
          this,
          ViewOutlineProviderActivity::class.java
        )
      )
    }
      findViewById<View>(R.id.motionLayoutBtn).setOnClickListener {
      startActivity(Intent(this, MotionLayoutActivity::class.java))
    }
      findViewById<View>(R.id. viewPropertyBtn).setOnClickListener {
      startActivity(Intent(this, ViewPropertyAnimatorActivity::class.java))
    }
    findViewById<View>(R.id.recorderBtn).setOnClickListener {
      startActivity(Intent(this, RecorderActivity::class.java))
    }
    findViewById<View>(R.id.blackWhitePictureBtn).setOnClickListener {
      startActivity(Intent(this, BlackWhitePictureActivity::class.java))
    }
      findViewById<View>(R.id.shipWaveBtn).setOnClickListener {
      startActivity(Intent(this, ShipWaveActivity::class.java))
    }
      findViewById<View>(R.id.animatorTextBtn).setOnClickListener {
      startActivity(Intent(this, AnimatorTextActivity::class.java))
    }
    findViewById<View>(R.id.cameraMatrixBtn).setOnClickListener {
      startActivity(Intent(this, CameraMatrixActivity::class.java))
    }

    //测试livedata连续调用
    val ld = MutableLiveData<String>()
    ld.observe(this) {
      Log.d(TAG, "ld observe $it")
    }
    ld.postValue("1")
    ld.postValue("2")

    getProcessInfo()

    if (!isAccessibilityServiceOn()) {
      open()
    }
    SystemView.init(this,windowManager)
  }



  fun open(){
    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
  }

  fun isAccessibilityServiceOn(): Boolean {
    var service = "${packageName}/${AccessibilityService::class.java.canonicalName}"
    var enabled = Settings.Secure.getInt(applicationContext.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED)
    var splitter = TextUtils.SimpleStringSplitter(':')
    if (enabled == 1) {
      var settingValue = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
      if (settingValue != null) {
        splitter.setString(settingValue)
        while (splitter.hasNext()) {
          var accessibilityService = splitter.next()
          //例如打开QQ的无障碍服务 com.tencent.qqpinyin/.accessibility.EmotionHelperService
          Log.d(TAG,"用户开启的无障碍服务有： $accessibilityService")
          if (accessibilityService.equals(service, ignoreCase = true)) {
            return true
          }
        }
      }
    }
    return false
  }

  /**
   * 计算方法：获取到屏幕的分辨率:point.x和point.y，再取出屏幕的DPI（每英寸的像素数量），计算长和宽有多少英寸，即：point.x / dm.xdpi，point.y / dm.ydpi，
   * 屏幕的长和宽算出来了，再用勾股定理，计算出斜角边的长度，即屏幕尺寸。
   *
   * 注意： 此处displayMetrics不要使用context.getApplicationContext().getResources().getDisplayMetrics()来获取。
  上面说到，DPI是由设备出厂时写死到设备里的，如果写入的DPI值不准确，当然计算不出准确的屏幕尺寸。所以计算出的屏幕尺寸只做参考
   *
   * 得到屏幕的物理尺寸，由于该尺寸是在出厂时，厂商写死的，所以仅供参考
   * 计算方法：获取到屏幕的分辨率:point.x和point.y，再取出屏幕的DPI（每英寸的像素数量），
   * 计算长和宽有多少英寸，即：point.x / dm.xdpi，point.y / dm.ydpi，屏幕的长和宽算出来了，
   * 再用勾股定理，计算出斜角边的长度，即屏幕尺寸。
   * @param context
   * @return  例如6.67英寸
   */
  fun getPhysicsScreenSize(context: Context): Double {
    val manager: WindowManager =
      context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val point = Point()
    manager.getDefaultDisplay().getRealSize(point)
    val dm: DisplayMetrics = context.resources.displayMetrics
    val densityDpi: Int = dm.densityDpi //得到屏幕的密度值，但是该密度值只能作为参考，因为他是固定的几个密度值。
    val x =
      (point.x / dm.xdpi).toDouble().pow(2.0) //dm.xdpi是屏幕x方向的真实密度值，比上面的densityDpi真实。
    val y =
      (point.y / dm.ydpi).toDouble().pow(2.0) //dm.xdpi是屏幕y方向的真实密度值，比上面的densityDpi真实。
    Log.d(TAG, "getPhysicsScreenSize x:$x y:$y")
    return sqrt(x + y)
  }

  private fun getAndroidInfo() {
    //android 版本 其他信息 https://blog.csdn.net/chyychfchx/article/details/59484332
    Log.i(TAG, "android version" + VERSION.RELEASE)
    Log.i(TAG, "手机型号" + Build.MODEL)
    Log.i(TAG, "手机厂商" + Build.BOARD)
  }

  private fun getProcessInfo() {
    //adb shell pidof sst.example.androiddemo.feature  31076

    //jdwp                     list pids of processes hosting a JDWP transport
    //adb jdwp 也可以获取，
//        adb获取  31076就是进程id   adb shell ps|findstr com.something  // windows
//        adb shell ps | grep sst.example.androiddemo.feature
//        u0_a65       31076  1870 1466352  94080 ep_poll             0 S sst.example.androiddemo.feature
    //https://blog.csdn.net/Danny_llp/article/details/122246236
    //获取当前进程号
    Log.i(TAG, "Process.myPid: " + Process.myPid());
    //获取当前线程号
    Log.i(TAG, "Process.myTid: " + Process.myTid());
    //当前调用该进程的用户号
    Log.i(TAG, "Process.myUid: " + Process.myUid());
    //当前线程ID
    Log.i(TAG, "Thread.currentThread().getId: " + Thread.currentThread().id);
    //主线程ID
    Log.i(TAG, "getMainLooper().getThread().getId: " + mainLooper.thread.id);
    //当前Activity所在栈的ID
    Log.i(TAG, "getTaskId: " + taskId);
    //当前调用该应用程序的用户号
    Log.i(TAG, "getApplicationInfo().uid: " + applicationInfo.uid);
    //当前调用该应用程序的进程名
    Log.i(TAG, "getApplicationInfo().processName): " + applicationInfo.processName);

    Log.i(TAG, "主进程名 " + packageManager.getApplicationInfo(packageName, 0).processName)
    getRunningAppProcessInfos(this).forEach {
      Log.i(TAG, "当前包含的进程有${it.processName}")
    }

    Log.i(TAG, "当前是否是主进程：${isMainProcess(context = this)}")
    Log.i(TAG, "当前是否是主进程：${packageName.equals(applicationContext.applicationInfo.processName)}")
  }

  fun isMainProcess(context: Context?): Boolean {
    try {
      if (null != context) {
        return if (VERSION.SDK_INT >= VERSION_CODES.P) {
          context.packageName.equals(getProcessName())
        } else {
          //低版本不可用
          context.packageName.equals(applicationInfo.processName)
        }
      }
    } catch (e: Exception) {
      return false
    }
    return true
  }

  //获取方法调用栈
  private fun getMethodTrace() {
    println("getMethodTrace start ==================================")
    val ste = Thread.currentThread().stackTrace
    ste.forEach {
      println(it)
    }
    println("getMethodTrace use Throwable ==================================")
    //两种输出的内容大体一致
    val sw = StringWriter()
    Throwable("").printStackTrace(PrintWriter(sw))
    val stackTrace: String = sw.toString()
    println(stackTrace)
    println("getMethodTrace use Throwable end ==================================")
  }

  //支持Throwable和子类exception
  fun exceptionToString(tr: Throwable): String {
    val sw = StringWriter()
    tr.printStackTrace(PrintWriter(sw))
    return sw.toString()
  }

  //获取所有的进程
  public fun getRunningAppProcessInfos(context: Context): List<ActivityManager.RunningAppProcessInfo> {
    val am: ActivityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    return am.runningAppProcesses
  }

  private fun getCurrentProcessName(): String {
    val pid = Process.myPid()
    var processName = "";
    val manager: ActivityManager =
      applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    manager.runningAppProcesses.forEach {
      if (it.pid == pid) {
        processName = it.processName;
      }
    }
    return processName;
  }

//    /**
//     * A native method that is implemented by the 'native-lib' native library,
//     * which is packaged with this application.
//     */
//    external fun stringFromJNI(): String
//
//    companion object {
//
//        // Used to load the 'native-lib' library on application startup.
//        init {
//            System.loadLibrary("native-lib")
//        }
//    }

  private fun shareText(content: String) {
    val shareIntent = Intent(Intent.ACTION_SEND)
    shareIntent.type = "text/plain"
    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "这是标题")//添加分享内容标题
    shareIntent.putExtra(Intent.EXTRA_TEXT, content)//添加分享内容
    this.startActivity(Intent.createChooser(shareIntent, "分享title"))
  }

  private fun shareImg(content: String) {
    val shareIntent = Intent(Intent.ACTION_SEND)
    shareIntent.type = "image/*"
    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "这是标题")//添加分享内容标题
    shareIntent.putExtra(Intent.EXTRA_TEXT, content)//添加分享内容
    this.startActivity(Intent.createChooser(shareIntent, "分享title"))
  }

  private fun shareVideo(path: String) {
    val shareIntent = Intent(Intent.ACTION_SEND)
    shareIntent.type = "video/*"
    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "这是标题")//添加分享内容标题
//        shareIntent.putExtra(Intent.EXTRA_STREAM, UriUtils.file2Uri(File(path)))//添加分享内容
    this.startActivity(Intent.createChooser(shareIntent, "分享title"))
  }




  //项目根gradle
//    buildscript {
//        repositories {
//            google()
//            jcenter()
//            maven {
//                url 'http://maven.aliyun.com/nexus/content/repositories/releases/'
//            }
//        }
//
//        apply from: 'thirdparty-lib/config.gradle'
//
//        dependencies {
//            classpath externalAndroidBuildGradlePlugin
//        }
//    }
//    在config.gradle 中抽离共用版本
//    ext {
//
//        //==Android Third party,include Application Layer,SDK Layer==
//
//        //Build Gradle Plugin
//        externalAndroidBuildGradlePlugin = 'com.android.tools.build:gradle:3.1.0'
//
//        //Android support Libraries
//        externalAndroidAppCompatV7 = 'com.android.support:appcompat-v7:26.1.0'
//        externalAndroidAppCompatV7ToSnapSvideo = 'com.android.support:appcompat-v7:24.2.1'
//    }

  ///module工程 :AliyunRecorder:record    AliyunRecorder目录下没有build.gradle,record目录下存在
  ///依赖这种工程时，拷贝到主工程根目录下(区分此时是主工程还是module),在build.gradle目录下声明，setting中引入即可
  ///使用import module方式出错

//    MediaScannerConnection.scanFile

  //视频大小窗播放 https://www.jianshu.com/p/420f7b14d6f6
//    https://blog.csdn.net/u010072711/article/details/51517170

  override fun onStart() {
    super.onStart()
    Log.d(TAG, " onStart ==== ")
  }

  override fun onResume() {
    super.onResume()
    Log.d(TAG, " onResume ==== ")
  }

  override fun onPause() {
    super.onPause()
    Log.d(TAG, " onPause ==== ")
  }

  override fun onStop() {
    super.onStop()
    Log.d(TAG, " onStop ==== ")
  }

  //应用退出可以调用
  override fun onDestroy() {
    super.onDestroy()
    Log.d(TAG, " onDestroy ==== ")
  }

  override fun onRestart() {
    super.onRestart()
    Log.d(TAG, " onRestart ==== ")
  }

  private fun getVersion(): Int {
    var version = System.getProperty("java.version")
    if (version!!.startsWith("1.")) {
      version = version.substring(2, 3)
    } else {
      val dot = version.indexOf(".")
      if (dot != -1) {
        version = version.substring(0, dot)
      }
    }
    return version.toInt()
  }

  //     home 键禁用
  override fun onAttachedToWindow() {
    println("Page01 -->onAttachedToWindow")
    //适用android4.4以下
//        this.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD)
    super.onAttachedToWindow()
  }

  override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
    println("Page01 -->onKeyDown: keyCode: $keyCode")
    if (KeyEvent.KEYCODE_HOME === keyCode) {
      println("HOME has been pressed yet ...")
      // android.os.Process.killProcess(android.os.Process.myPid());
      Toast.makeText(
        applicationContext, "HOME 键已被禁用...",
        Toast.LENGTH_LONG
      ).show()
    }
    return super.onKeyDown(keyCode, event) // 不会回到 home 页面
  }
}
