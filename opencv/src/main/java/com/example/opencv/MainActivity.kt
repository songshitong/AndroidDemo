package com.example.opencv

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import org.opencv.android.OpenCVLoader

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

    findViewById<View>(R.id.textRecognitionBtn).setOnClickListener {
      startActivity(Intent(this,TextRecognizeActivity::class.java))
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