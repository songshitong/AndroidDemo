package com.example.androiddemo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.androiddemo.aidl.AidlActivity
import java.util.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.aidlActivity).setOnClickListener {
          startActivity(Intent(this, AidlActivity::class.java))
        }

        findViewById<Button>(R.id.sameTaskAffinityActivity).setOnClickListener {
            startActivity(Intent(this,SameTaskAffinityActivity::class.java))
        }
        findViewById<Button>(R.id.singleInstanceActivity).setOnClickListener {
            val intent = Intent()
            //隐式action设置的名字
            intent?.action = "sst.example.androiddemo.feature.singleinstance"
            intent?.setPackage("sst.example.androiddemo.feature")
            startActivity(intent)
        }
    }

}
