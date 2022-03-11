package sst.example.androiddemo.feature.activity.testAnr

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import sst.example.androiddemo.feature.R

class AnrActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        //测试activity执行耗时方法 是否会执行anr
        Thread.sleep(10*1000)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_anr)

    }
}