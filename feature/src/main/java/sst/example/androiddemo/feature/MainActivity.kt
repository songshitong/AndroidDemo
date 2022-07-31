package sst.example.androiddemo.feature

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ResolveInfo
import android.media.MediaPlayer
import android.os.*
import android.os.Build.VERSION
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.MutableLiveData
import com.google.android.material.snackbar.Snackbar
import com.sst.material.BottomNavigationActivity
import kotlinx.android.synthetic.main.activity_main.*
import sst.example.androiddemo.feature.animation.LayoutAnimationActivity
import sst.example.androiddemo.feature.animation.RevealAnimatorActivity
import sst.example.androiddemo.feature.animation.activity.ActivityAnimation
import sst.example.androiddemo.feature.animation.activity.ActivityTransition
import sst.example.androiddemo.feature.animation.dynamicanimation.DynamicAnimaitonActivity
import sst.example.androiddemo.feature.animation.evaluator.TypeEvaluatorActivity
import sst.example.androiddemo.feature.SystemBug.ToastBugActivity
import sst.example.androiddemo.feature.activity.*
import sst.example.androiddemo.feature.activity.launchmode.LaunchActivity
import sst.example.androiddemo.feature.activity.scrollNested.ScrollNestedActivity
import sst.example.androiddemo.feature.activity.testAnr.TestAnrActivity
import sst.example.androiddemo.feature.animation.KeyFrameActivity
import sst.example.androiddemo.feature.ffmpeg.FFmpegActivity
import sst.example.androiddemo.feature.graphics.*
import sst.example.androiddemo.feature.resources.XmlParserActivity
import sst.example.androiddemo.feature.util.MyUtils
import sst.example.androiddemo.feature.util.ToastUtil
import sst.example.androiddemo.feature.video.VideoParserActivity
import sst.example.androiddemo.feature.wallpaper.NormalWallpaperService
import sst.example.androiddemo.feature.webview.JumpActivity
import sst.example.androiddemo.feature.widget.layout.ConstrainLayoutActivity
import sst.example.androiddemo.feature.widget.layout.repeatMeasure.MeasureTestActivity
import sst.example.androiddemo.feature.widget.practice.recyclerview.customLayoutManager.RVCutsomLayoutManagerActivity

class  MainActivity : AppCompatActivity()  {

    private val TAG ="MainActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        getAndroidInfo()
        ToastUtil.customSnackbar(this,"测试SnackBar",Snackbar.LENGTH_LONG)
        var startTime = System.currentTimeMillis()
        val sp: SharedPreferences = getSharedPreferences("sp", Context.MODE_PRIVATE)
        sp.edit().putString("a","11111").apply();
        val value: String? = sp.getString("a", "")
        Log.e(TAG, "func1  :  ${System.currentTimeMillis() - startTime}")
        //android jvm不跟随Java jvm版本 所以无法获得具体的Java Version
        Log.d(TAG,"java version ${getVersion()}")
        Log.d(TAG,"java version ${Runtime::class.java.getPackage().implementationVersion}")
        Log.d(TAG, " onCreate ==== ")
        // Example of a call to a native method
//        sample_text.text = stringFromJNI()
        mainNormalActivity.setOnClickListener {
            val intent = Intent(this, TestOrientationActivity::class.java)
            startActivity(intent)
        }
        launchActivity.setOnClickListener {
            val intent = Intent(this, LaunchActivity::class.java)
            startActivity(intent)
        }
        main_dialog.setOnClickListener {
           val dialog =  AlertDialog.Builder(this).create()
            dialog.setTitle("dialog")
            dialog.setButton(DialogInterface.BUTTON_POSITIVE, "确定") { dialogInterface, i -> dialog.dismiss()
            }
            dialog.show()
        }
        mainDialogActivity.setOnClickListener {
            val intent = Intent(this, DialogActivity::class.java)
            startActivity(intent)
        }
        go2Fragment.setOnClickListener {
            val intent = Intent(this, FragmentActivity::class.java)
            startActivity(intent)
        }
        go2Menu.setOnClickListener {
            val intent = Intent(this, MenuActivity::class.java)
            startActivity(intent)
        }

        go2Service.setOnClickListener {
            val intent = Intent(this, ServicesActivity::class.java)
            startActivity(intent)
        }
        layoutWeight.setOnClickListener {
            val intent = Intent(this, LinearLayoutActivity::class.java)
            startActivity(intent)
        }

        sample_text.text = "BitMap"
        sample_text.setOnClickListener {
            val intent = Intent(this, BitmapActivity::class.java)
            startActivity(intent)
        }

        pictureView.setOnClickListener {
            val intent = Intent(this, PictureDrawableActivity::class.java)
            startActivity(intent)
        }
        canvasActivity.setOnClickListener {
            val intent = Intent(this, CanvasActivity::class.java)
            startActivity(intent)
        }
        paintActivity.setOnClickListener{
            val intent = Intent(this, PaintActivity::class.java)
            startActivity(intent)
        }
        drawableActivity.setOnClickListener {
            val intent = Intent(this, DrawableActivity::class.java)
            startActivity(intent)
        }
        pathActivity.setOnClickListener {
            val intent = Intent(this, PathActivity::class.java)
            startActivity(intent)
        }

        layoutAnimationActivity.setOnClickListener {
            val intent = Intent(this, LayoutAnimationActivity::class.java)
            startActivity(intent)
        }
        particleActivity.setOnClickListener {
            val intent = Intent(this, ParticleActivity::class.java)
            startActivity(intent)
        }
        loadingActivity.setOnClickListener {
            val intent = Intent(this, LoadingActivity::class.java)
            startActivity(intent)
        }
        qqDragBubbleActivity.setOnClickListener {
            val intent = Intent(this, QQDragBubbleActivity::class.java)
            startActivity(intent)
        }
        stickyRecyclerviewAct.setOnClickListener {
            val intent = Intent(this, StickyActivity::class.java)
            startActivity(intent)
        }

        springAnimation.setOnClickListener {
            val intent = Intent(this, DynamicAnimaitonActivity::class.java)
            startActivity(intent)
        }
        oscillationView.setOnClickListener {
            val intent = Intent(this, OscillationActivity::class.java)
            startActivity(intent)
        }
        growingTreeView.setOnClickListener {
            val intent = Intent(this, GrowingTreeActivity::class.java)
            startActivity(intent)
        }
        xmlParserBtn.setOnClickListener {
            val intent = Intent(this, XmlParserActivity::class.java)
            startActivity(intent)
        }
        wallpaperBtn.setOnClickListener {
//            val intent = Intent(this, SettingActivity::class.java)
//            startActivity(intent)
            startActivity(MyUtils.getWallPaper(this, NormalWallpaperService::class.java))

        }
        jumpBtn.setOnClickListener {
            val intent = Intent(this, JumpActivity::class.java)
            startActivity(intent)
        }
        videoParser.setOnClickListener {
            val intent = Intent(this, VideoParserActivity::class.java)
            startActivity(intent)
        }
        shareBtn.setOnClickListener {
            shareText("这是内容")
        }
        ffmpeg.setOnClickListener {
            val intent = Intent(this, FFmpegActivity::class.java)
            startActivity(intent)
        }

        material.setOnClickListener {
            val intent = Intent(this, BottomNavigationActivity::class.java)
            startActivity(intent)

        }



//      系统铃声 RingtoneManager  https://blog.csdn.net/ch853199769/article/details/78721003
        ringtone.setOnClickListener {
            val player: MediaPlayer = MediaPlayer.create(
                this,
                ///系统铃声
//                Settings.System.DEFAULT_RINGTONE_URI
                ///系统通知  华为手机崩溃
                Settings.System.DEFAULT_NOTIFICATION_URI
            )
            player.start()
        }
        startActivityForResult.setOnClickListener {
            val intent = Intent(this, StartForResultActivity::class.java)
            startActivity(intent)

        }

        whatsapp.setOnClickListener {
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
        var index =0
        Thread(Runnable {
            do {
                index++
            } while (time + 1000 >= System.currentTimeMillis())
            Log.d(TAG, "index value  $index")
        }).start();
        //todo 文字动画 https://github.com/aagarwal1012/Animated-Text-Kit
        //todo AsyncTask  AsyncTaskLoader Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)  Android线程调度

        //todo handler原理为什么顺序是021  runnable运行的线程不是主线程吗  线程的创建需要时间，handler的唤醒需要时间
        startHandler.setOnClickListener {
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

        //TODO docker  nginx(http://127.0.0.1/stat  nginx查看状态)
        // http://www.joshuachou.ink/archives/395/

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
        testAnrActivity.setOnClickListener {
            startActivity(Intent(this,TestAnrActivity::class.java))
        }
        measureTestActivity.setOnClickListener {
            startActivity(Intent(this, MeasureTestActivity::class.java))
        }
        customLMActivity.setOnClickListener {
            startActivity(Intent(this,RVCutsomLayoutManagerActivity::class.java))
        }
        scrollNestedActivity.setOnClickListener {
            startActivity(Intent(this,
                ScrollNestedActivity::class.java))
        }

        toastBugActivity.setOnClickListener {
            startActivity(Intent(this,
                ToastBugActivity::class.java))
        }

        bigPictureActivity.setOnClickListener {
            startActivity(Intent(this,
                BigPictureActivity::class.java))
        }

        IntentServiceActivity.setOnClickListener {
            startActivity(Intent(this,
                IntentServiceActivity::class.java))
        }
        IntentConstrainLayoutActivity.setOnClickListener {
            startActivity(Intent(this,
                ConstrainLayoutActivity::class.java))
        }
        IntentRevealAnimatorActivity.setOnClickListener {
            startActivity(Intent(this,
                RevealAnimatorActivity::class.java))
        }
        IntentTypeEvaluatorActivity.setOnClickListener {
            startActivity(Intent(this,
                TypeEvaluatorActivity::class.java))
        }
        IntentActivityAnimation.setOnClickListener {
            startActivity(Intent(this,
                ActivityAnimation::class.java))
        }
        IntentActivityTransition.setOnClickListener {
            startActivity(Intent(this,
                ActivityTransition::class.java),ActivityOptionsCompat.makeSceneTransitionAnimation(this).toBundle())
        }
      ActivityKeyFrame.setOnClickListener {
        startActivity(Intent(this,
          KeyFrameActivity::class.java))
      }
      dialogFragmentBtn.setOnClickListener {
        startActivity(Intent(this,
          DialogFragmentActivity::class.java))
      }
        //测试livedata连续调用
        val ld = MutableLiveData<String>()
        ld.observe(this) {
            Log.d(TAG,"ld observe $it")
        }
        ld.postValue("1")
        ld.postValue("2")

        getProcessInfo()
    }

  private fun getAndroidInfo() {
    //android 版本 其他信息 https://blog.csdn.net/chyychfchx/article/details/59484332
    Log.i(TAG ,"android version" + VERSION.RELEASE)
    Log.i(TAG ,"手机型号" + Build.MODEL)
    Log.i(TAG ,"手机厂商" + Build.BOARD)
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
        Log.i(TAG, "Process.myPid: "+Process.myPid());
        //获取当前线程号
        Log.i(TAG, "Process.myTid: "+Process.myTid());
        //当前调用该进程的用户号
        Log.i(TAG, "Process.myUid: "+Process.myUid());
        //当前线程ID
        Log.i(TAG, "Thread.currentThread().getId: "+Thread.currentThread().id);
        //主线程ID
        Log.i(TAG, "getMainLooper().getThread().getId: "+ mainLooper.thread.id);
        //当前Activity所在栈的ID
        Log.i(TAG, "getTaskId: "+ taskId);
        //当前调用该应用程序的用户号
        Log.i(TAG, "getApplicationInfo().uid: "+ applicationInfo.uid);
        //当前调用该应用程序的进程名
        Log.i(TAG, "getApplicationInfo().processName): "+ applicationInfo.processName);

        Log.i(TAG,"主进程名 "+packageManager.getApplicationInfo(packageName, 0).processName)
        getRunningAppProcessInfos(this).forEach {
          Log.i(TAG,"当前包含的进程有${it.processName}")
        }
    }

  //获取所有的进程
  public fun getRunningAppProcessInfos( context:Context):List<ActivityManager.RunningAppProcessInfo> {
    val am:ActivityManager  = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    return am.runningAppProcesses
  }

  private fun  getCurrentProcessName():String {
    val pid =Process.myPid()
    var processName = "";
    val manager:ActivityManager = applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
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
    //todo lottie svga(yy ued 开源)
    //不规则图形  点击图片就显色 bitmap.getPixel   1点击位置是否是不规则 2点击位置的颜色  需图片大小与控件大小  普通view加载背景图片的方式，fit?
    //dalvik system CloseGuard
    //ndk 稳定版16b  aiqiyi xhook elf hook原理


    //webview  好用的webview
    //todo loadUrl   Refusing to load URL as it exceeds 2097152 characters.
    //由loadUrl改为evaluateJavascript
    //实例化webviewcontext必须是activity，内部弹出alert需要activity context
    // Android webview无法全屏

    //todo 打日志总结  方法的出入口，日志不能有相同的描述，方便定位到具体代码行，相当于唯一标识？，尤其是同一个方法里面的
    //

    //todo Android字体，苹果字体，字体压缩




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
                getApplicationContext(), "HOME 键已被禁用...",
                Toast.LENGTH_LONG
            ).show()
        }
        return super.onKeyDown(keyCode, event) // 不会回到 home 页面
    }
}
