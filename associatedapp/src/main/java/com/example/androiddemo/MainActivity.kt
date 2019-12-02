package com.example.androiddemo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.androiddemo.aidl.AidlActivity
import kotlinx.android.synthetic.main.activity_main.*


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
