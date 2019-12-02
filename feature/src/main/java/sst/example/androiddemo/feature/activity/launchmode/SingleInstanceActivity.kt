package sst.example.androiddemo.feature.activity.launchmode

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_single_instance.*
import sst.example.androiddemo.feature.R

//SingleInstance模式启动的Activity在系统中具有全局唯一性
//在两个应用中启动SingleInstanceActivity
//11-17 17:48:48.117 15459-15459/sst.example.androiddemo.feature I/LaunchActivity ====: LaunchActivity TaskId: 18309 hasCode:153006856
//11-17 17:48:48.118 15459-15459/sst.example.androiddemo.feature I/LaunchActivity ====: taskAffinity:sst.example.androiddemo.feature
//11-17 17:48:48.470 15459-15459/sst.example.androiddemo.feature D/MainActivity:  onStop ====
//11-17 17:48:50.316 15459-15459/sst.example.androiddemo.feature D/SingleInstanceActivity:  onCreate ====
//11-17 17:48:50.317 15459-15459/sst.example.androiddemo.feature I/SingleInstanceActivity: SingleInstanceActivity TaskId: 18310 hasCode:227691099 ====
//11-17 17:48:50.319 15459-15459/sst.example.androiddemo.feature I/SingleInstanceActivity: taskAffinity:sst.example.androiddemo.feature ====
//11-17 17:48:50.320 15459-15459/sst.example.androiddemo.feature D/SingleInstanceActivity:  onStart ====
//11-17 17:48:50.323 15459-15459/sst.example.androiddemo.feature D/SingleInstanceActivity:  onResume ====
//11-17 17:49:05.947 15459-15459/sst.example.androiddemo.feature D/SingleInstanceActivity:  onPause ====
//11-17 17:49:05.974 15459-15459/sst.example.androiddemo.feature D/SingleInstanceActivity:  onStop ====
////此时从另一个应用启动SingleInstanceActivity
//11-17 17:49:11.405 15459-15459/sst.example.androiddemo.feature D/SingleInstanceActivity:  onNewIntent ====
//11-17 17:49:11.408 15459-15459/sst.example.androiddemo.feature I/SingleInstanceActivity: SingleInstanceActivity TaskId: 18310 hasCode:227691099 ====
//11-17 17:49:11.409 15459-15459/sst.example.androiddemo.feature I/SingleInstanceActivity: taskAffinity:sst.example.androiddemo.feature ====
//11-17 17:49:11.410 15459-15459/sst.example.androiddemo.feature D/SingleInstanceActivity:  onRestart ====
//11-17 17:49:11.410 15459-15459/sst.example.androiddemo.feature D/SingleInstanceActivity:  onStart ====
//11-17 17:49:11.411 15459-15459/sst.example.androiddemo.feature D/SingleInstanceActivity:  onResume ====

//用SingleInstance启动activity，查看任务栈中存在几个activity  SingleInstance独占任务栈，OtherActivity与启动SingleInstanceActivity的LaunchActivity在同一任务栈
//11-17 18:15:12.633 20809-20809/sst.example.androiddemo.feature I/LaunchActivity ====: LaunchActivity TaskId: 18318 hasCode:70669510
//11-17 18:15:12.634 20809-20809/sst.example.androiddemo.feature I/LaunchActivity ====: taskAffinity:sst.example.androiddemo.feature
//11-17 18:15:12.967 20809-20809/sst.example.androiddemo.feature D/MainActivity:  onStop ====
//11-17 18:15:15.164 20809-20809/sst.example.androiddemo.feature D/SingleInstanceActivity:  onCreate ====
//11-17 18:15:15.165 20809-20809/sst.example.androiddemo.feature I/SingleInstanceActivity: SingleInstanceActivity TaskId: 18319 hasCode:209489462 ====
//11-17 18:15:15.166 20809-20809/sst.example.androiddemo.feature I/SingleInstanceActivity: taskAffinity:sst.example.androiddemo.feature ====
//11-17 18:15:15.167 20809-20809/sst.example.androiddemo.feature D/SingleInstanceActivity:  onStart ====
//11-17 18:15:15.168 20809-20809/sst.example.androiddemo.feature D/SingleInstanceActivity:  onResume ====
//11-17 18:15:23.754 20809-20809/sst.example.androiddemo.feature D/SingleInstanceActivity:  onPause ====
//11-17 18:15:23.766 20809-20809/sst.example.androiddemo.feature D/OtherActivity:  onCreate ====
//11-17 18:15:23.781 20809-20809/sst.example.androiddemo.feature I/OtherActivity: OtherActivity TaskId: 18318 hasCode:265312345 ====
//11-17 18:15:23.782 20809-20809/sst.example.androiddemo.feature I/OtherActivity: taskAffinity:sst.example.androiddemo.feature ====
//11-17 18:15:24.452 20809-20809/sst.example.androiddemo.feature D/SingleInstanceActivity:  onStop ====


class SingleInstanceActivity : AppCompatActivity() {
    private val TAG = "SingleInstanceActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_single_instance)
        Log.d(TAG," onCreate ==== ")
        printActivityInfo()
        singleInstance2Other.setOnClickListener {
            val intent = Intent(this, OtherActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG," onStart ==== ")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG," onResume ==== ")

    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG," onPause ==== ")

    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG," onStop ==== ")

    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG," onDestroy ==== ")

    }

    override fun onRestart() {
        super.onRestart()
        Log.d(TAG," onRestart ==== ")

    }

    override fun onNewIntent(intent: Intent?) {
        Log.d(TAG," onNewIntent ==== ")
        printActivityInfo()
        super.onNewIntent(intent)
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
