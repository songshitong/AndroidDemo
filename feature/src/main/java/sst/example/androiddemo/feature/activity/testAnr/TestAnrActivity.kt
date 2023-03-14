package sst.example.androiddemo.feature.activity.testAnr

import android.content.Intent
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import sst.example.androiddemo.feature.R

class TestAnrActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_anr)
        findViewById<View>(R.id.anrActivity).setOnClickListener {
            val intent = Intent(this,AnrActivity::class.java)
            startActivity(intent)
        }
        //连续点击4次才触发ANR
          findViewById<View>(R.id.clickEventAnr).setOnClickListener {
            Thread.sleep(10*1000)
        }
    }
}