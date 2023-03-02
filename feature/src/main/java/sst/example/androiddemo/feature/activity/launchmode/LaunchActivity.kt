package sst.example.androiddemo.feature.activity.launchmode

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import sst.example.androiddemo.feature.R

class LaunchActivity : AppCompatActivity() {
    private  val TAG = "LaunchActivity ===="
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)

        findViewById<View>(R.id.standardActivity).setOnClickListener {
            val intent = Intent(this, StandardActivity::class.java)
            startActivity(intent)
        }
          findViewById<View>(R.id.singleTaskActivity).setOnClickListener {
            val intent = Intent(this, SingleTaskActivity::class.java)
            startActivity(intent)
        }
          findViewById<View>(R.id.singleTopActivity).setOnClickListener {
            val intent = Intent(this, SingleTopActivity::class.java)
            startActivity(intent)
        }
          findViewById<View>(R.id.singleInstanceActivity).setOnClickListener {
            val intent = Intent(this, SingleInstanceActivity::class.java)
            startActivity(intent)
        }
        printActivityInfo()
    }

    fun printActivityInfo(){
        Log.i(TAG,  this.javaClass.simpleName + " TaskId: " + getTaskId() + " hasCode:" + this.hashCode())
        dumpTaskAffinity()

    }

    fun dumpTaskAffinity(){
        try {
            val info = this.packageManager
                .getActivityInfo(componentName, PackageManager.GET_META_DATA)
            Log.i(TAG, "taskAffinity:"+info.taskAffinity)
        } catch (e : PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }
}
