package sst.example.androiddemo.feature

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.flutter.facade.Flutter
import kotlinx.android.synthetic.main.activity_main.*
import sst.example.androiddemo.feature.Animation.LayoutAnimationActivity
import sst.example.androiddemo.feature.R.id.sample_text
import sst.example.androiddemo.feature.graphics.*

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

        shareBtn.setOnClickListener {
            shareFile("这是内容")
        }
        someContainer.setOnClickListener {
            val tx = supportFragmentManager.beginTransaction()
            tx.replace(R.id.someContainer, Flutter.createFragment("route1"))
            tx.commit()
        }


        //测试手机1S计算的次数
        val time = System.currentTimeMillis()
        var index =0
        Thread(Runnable {
            do {
                index++
            }while (time+1000 >= System.currentTimeMillis())
            Log.d(TAG,"index value  $index")
        }).start()
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



    private fun shareFile(content: String) {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "这是标题")//添加分享内容标题
        shareIntent.putExtra(Intent.EXTRA_TEXT,content)//添加分享内容
        this.startActivity(Intent.createChooser(shareIntent, "分享title"))
    }

}
