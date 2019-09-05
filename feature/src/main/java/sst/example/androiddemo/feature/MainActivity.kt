package sst.example.androiddemo.feature

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.blankj.utilcode.util.UriUtils
import kotlinx.android.synthetic.main.activity_main.*
import sst.example.androiddemo.feature.Animation.LayoutAnimationActivity
import sst.example.androiddemo.feature.Animation.dynamicanimation.DynamicAnimaitonActivity
import sst.example.androiddemo.feature.ffmpeg.FFmpegActivity
import sst.example.androiddemo.feature.graphics.*
import sst.example.androiddemo.feature.resources.XmlParserActivity
import sst.example.androiddemo.feature.webview.JumpActivity
import java.io.File

class  MainActivity : AppCompatActivity() {
    private val TAG ="MainActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Example of a call to a native method
//        sample_text.text = stringFromJNI()
        sample_text.text = "BitMap"
        sample_text.setOnClickListener {
            val intent = Intent(this,BitmapActivity::class.java)
            startActivity(intent)
        }

        pictureView.setOnClickListener {
            val intent = Intent(this,PictureDrawableActivity::class.java)
            startActivity(intent)
        }
        canvasActivity.setOnClickListener {
            val intent = Intent(this,CanvasActivity::class.java)
            startActivity(intent)
        }
        paintActivity.setOnClickListener{
            val intent = Intent(this,PaintActivity::class.java)
            startActivity(intent)
        }
        drawableActivity.setOnClickListener {
            val intent = Intent(this,DrawableActivity::class.java)
            startActivity(intent)
        }
        pathActivity.setOnClickListener {
            val intent = Intent(this,PathActivity::class.java)
            startActivity(intent)
        }
        shaderActivity.setOnClickListener {
            val intent = Intent(this,ShaderActivity::class.java)
            startActivity(intent)
        }
        layoutAnimationActivity.setOnClickListener {
            val intent = Intent(this,LayoutAnimationActivity::class.java)
            startActivity(intent)
        }
        particleActivity.setOnClickListener {
            val intent = Intent(this,ParticleActivity::class.java)
            startActivity(intent)
        }
        loadingActivity.setOnClickListener {
            val intent = Intent(this,LoadingActivity::class.java)
            startActivity(intent)
        }
        qqDragBubbleActivity.setOnClickListener {
            val intent = Intent(this,QQDragBubbleActivity::class.java)
            startActivity(intent)
        }
        stickyRecyclerviewAct.setOnClickListener {
            val intent = Intent(this,StickyActivity::class.java)
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
        jumpBtn.setOnClickListener {
            val intent = Intent(this, JumpActivity::class.java)
            startActivity(intent)
        }
        videoParser.setOnClickListener {
//            val intent = Intent(this, VideoParserActivity::class.java)
//            startActivity(intent)
        }
        shareBtn.setOnClickListener {
            shareText("这是内容")
        }
        ffmpeg.setOnClickListener {
            val intent = Intent(this, FFmpegActivity::class.java)
            startActivity(intent)
        }




        //测试手机1S计算的次数  红米note大约20000次
        val time = System.currentTimeMillis()
        var index =0
        Thread(Runnable {
            do {
                index++
            }while (time+1000 >= System.currentTimeMillis())
            Log.d(TAG,"index value  $index")
        }).start();
        //todo 文字动画 https://github.com/aagarwal1012/Animated-Text-Kit
        //todo AsyncTask  AsyncTaskLoader Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)  Android线程调度

        //todo handler原理为什么顺序是021  runnable运行的线程不是主线程吗  线程的创建需要时间，handler的唤醒需要时间
        startHandler.setOnClickListener {
            val runnable = Runnable {
                Log.d(TAG,"1 runnable")
            }
            val handler = Handler()
            Log.d(TAG,"0")
            handler.post(runnable)
            Log.d(TAG,"2")
        }
        val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO)
        ActivityCompat.requestPermissions(this,
            permissions, 0)

        //TODO docker  nginx(http://127.0.0.1/stat  nginx查看状态)
        // http://www.joshuachou.ink/archives/395/

        //WindowManager.LayoutParams.FLAG_SECURE  禁止截屏
        //安全 监听截屏事件的产生  系统源码 TakeScreenshotService  GlobalScreenshot
        //  ContentObserver 监听图片的变化。。


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
        shareIntent.putExtra(Intent.EXTRA_TEXT,content)//添加分享内容
        this.startActivity(Intent.createChooser(shareIntent, "分享title"))
    }
    private fun shareImg(content: String) {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "image/*"
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "这是标题")//添加分享内容标题
        shareIntent.putExtra(Intent.EXTRA_TEXT,content)//添加分享内容
        this.startActivity(Intent.createChooser(shareIntent, "分享title"))
    }

    private fun shareVideo(path: String) {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "video/*"
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "这是标题")//添加分享内容标题
        shareIntent.putExtra(Intent.EXTRA_STREAM,UriUtils.file2Uri(File(path)))//添加分享内容
        this.startActivity(Intent.createChooser(shareIntent, "分享title"))
    }
    //todo lottie svga(yy ued 开源)
    //不规则图形  点击图片就显色 bitmap.getPixel   1点击位置是否是不规则 2点击位置的颜色  需图片大小与控件大小  普通view加载背景图片的方式，fit?
    //dalvik system CloseGuard
    //ndk 稳定版16b  aiqiyi xhook elf hook原理

    //GPU编程 GPU的io瓶颈，相关原理


}
