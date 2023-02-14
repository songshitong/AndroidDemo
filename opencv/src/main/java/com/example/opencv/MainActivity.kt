package com.example.opencv

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.ImageView
import android.widget.LinearLayout
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

class MainActivity : AppCompatActivity() {
  companion object{
    const val TAG = "MainActivity"
  }
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    if(OpenCVLoader.initDebug()){
      Log.d(TAG,"open cv init")
    }

    findViewById<View>(R.id.imgSplitBtn).setOnClickListener {
      startActivity(Intent(this,ImgSplitActivity::class.java))
    }
    findViewById<View>(R.id.templateBtn).setOnClickListener {
      startActivity(Intent(this,TemplateMatchActivity::class.java))
    }
    findViewById<View>(R.id.ContoursBtn).setOnClickListener {
      startActivity(Intent(this,ContoursActivity::class.java))
    }
    findViewById<View>(R.id.imgFilterBtn).setOnClickListener {
      startActivity(Intent(this,PictureFilterActivity::class.java))
    }


  }
}