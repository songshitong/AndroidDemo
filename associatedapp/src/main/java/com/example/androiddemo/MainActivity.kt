package com.example.androiddemo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.androiddemo.aidl.AidlActivity
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.*
import kotlin.concurrent.thread


//todo 判断asset文件是否可读写
//device file explorer 查看Linux的文件权限
//cdn 相关知识

// monkey 自动化测试
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        aidlActivity.setOnClickListener {
          startActivity(Intent(this,AidlActivity::class.java))
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
