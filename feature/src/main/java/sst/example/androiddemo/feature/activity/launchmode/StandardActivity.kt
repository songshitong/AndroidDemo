package sst.example.androiddemo.feature.activity.launchmode

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import sst.example.androiddemo.feature.R

// taskid相同说明是相同的任务栈
// hashcode不同说明生成了不同的实例
//11-12 07:48:28.095 29435-29435/sst.example.androiddemo.feature I/LaunchActivity ====: LaunchActivity TaskId: 17769 hasCode:201124667
//11-12 07:48:28.097 29435-29435/sst.example.androiddemo.feature I/LaunchActivity ====: taskAffinity:sst.example.androiddemo.feature
//11-12 07:48:28.454 29435-29435/sst.example.androiddemo.feature D/MainActivity:  onStop ====
//11-12 07:48:34.909 29435-29435/sst.example.androiddemo.feature D/StandardActivity ====: onCreate ====
//11-12 07:48:34.910 29435-29435/sst.example.androiddemo.feature I/StandardActivity ====: StandardActivity TaskId: 17769 hasCode:250790970
//11-12 07:48:34.910 29435-29435/sst.example.androiddemo.feature I/StandardActivity ====: taskAffinity:sst.example.androiddemo.feature
//11-12 07:48:45.667 29435-29435/sst.example.androiddemo.feature D/StandardActivity ====: onCreate ====
//11-12 07:48:45.668 29435-29435/sst.example.androiddemo.feature I/StandardActivity ====: StandardActivity TaskId: 17769 hasCode:66862953
//11-12 07:48:45.668 29435-29435/sst.example.androiddemo.feature I/StandardActivity ====: taskAffinity:sst.example.androiddemo.feature
class StandardActivity : AppCompatActivity() {
    private val TAG = "StandardActivity ===="

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_standard)
        Log.d(TAG,"onCreate ====")
        printActivityInfo()
    }

    override fun onNewIntent(intent: Intent?) {
        Log.d(TAG," onNewIntent ==== ")

        super.onNewIntent(intent)
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
