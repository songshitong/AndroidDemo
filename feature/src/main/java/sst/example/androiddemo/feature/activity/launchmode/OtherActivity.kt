package sst.example.androiddemo.feature.activity.launchmode

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_other.*
import sst.example.androiddemo.feature.R

class OtherActivity : AppCompatActivity() {
   private val TAG = "OtherActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG," onCreate ==== ")
        setContentView(R.layout.activity_other)
        standardOther.setOnClickListener {
            val intent = Intent(this, StandardActivity::class.java)
            startActivity(intent)
        }
        singleTaskOther.setOnClickListener {
            val intent = Intent(this, SingleTaskActivity::class.java)
            startActivity(intent)
        }
        singleTopOther.setOnClickListener {
            val intent = Intent(this, SingleTopActivity::class.java)
            startActivity(intent)
        }
        singleInstanceOther.setOnClickListener {
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
