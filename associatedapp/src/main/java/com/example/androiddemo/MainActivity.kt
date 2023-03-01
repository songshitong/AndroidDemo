package com.example.androiddemo

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.androiddemo.aidl.AidlActivity
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread



class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        aidlActivity.setOnClickListener {
          startActivity(Intent(this, AidlActivity::class.java))
        }
        val client = OkHttpClient()
        val ENDPOINT = "https://api.github.com/repos/square/okhttp/contributors"

        // Create request for remote resource.

        // Create request for remote resource.
        val request: Request = Request.Builder()
            .url(ENDPOINT)
            .build()
        thread {
            client.newCall(request).execute()
        }

        sameTaskAffinityActivity.setOnClickListener {
            startActivity(Intent(this,SameTaskAffinityActivity::class.java))
        }
        singleInstanceActivity.setOnClickListener {
            val intent = Intent()
            //隐式action设置的名字
            intent?.action = "sst.example.androiddemo.feature.singleinstance"
            intent?.setPackage("sst.example.androiddemo.feature")
            startActivity(intent)
        }
    }

}
