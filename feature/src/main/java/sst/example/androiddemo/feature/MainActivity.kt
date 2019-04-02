package sst.example.androiddemo.feature

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.flutter.facade.Flutter
import kotlinx.android.synthetic.main.activity_main.*
import sst.example.androiddemo.feature.Animation.LayoutAnimationActivity
import sst.example.androiddemo.feature.R.id.sample_text
import sst.example.androiddemo.feature.graphics.*

class  MainActivity : AppCompatActivity() {

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
        someContainer.setOnClickListener {
            val tx = supportFragmentManager.beginTransaction()
            tx.replace(R.id.someContainer, Flutter.createFragment("route1"))
            tx.commit()
        }
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
}
