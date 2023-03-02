package sst.example.androiddemo.feature.activity.launchmode

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import sst.example.androiddemo.feature.R

class OtherActivity : AppCompatActivity() {
   private val TAG = "OtherActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG," onCreate ==== ")
        setContentView(R.layout.activity_other)
        findViewById<View>(R.id.standardOther).setOnClickListener {
            val intent = Intent(this, StandardActivity::class.java)
            startActivity(intent)
        }
          findViewById<View>(R.id.singleTaskOther).setOnClickListener {
            val intent = Intent(this, SingleTaskActivity::class.java)
            startActivity(intent)
        }
          findViewById<View>(R.id.singleTopOther).setOnClickListener {
            val intent = Intent(this, SingleTopActivity::class.java)
            startActivity(intent)
        }
          findViewById<View>(R.id.singleInstanceOther).setOnClickListener {
            val intent = Intent(this, SingleInstanceActivity::class.java)
            startActivity(intent)
        }
        printActivityInfo()
    }

    fun printActivityInfo(){
        Log.i(TAG,  this.javaClass.simpleName + " TaskId: " + getTaskId() + " hasCode:" + this.hashCode()+" ====")
        dumpTaskAffinity()

    }

    fun dumpTaskAffinity(){
        try {
            val info = this.packageManager
                .getActivityInfo(componentName, PackageManager.GET_META_DATA)
            Log.i(TAG, "taskAffinity:"+info.taskAffinity+" ====")
        } catch (e : PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }
}
