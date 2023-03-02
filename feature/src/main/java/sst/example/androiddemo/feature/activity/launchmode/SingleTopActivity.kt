package sst.example.androiddemo.feature.activity.launchmode

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import sst.example.androiddemo.feature.R

//launch - singleTop - singleTop

//11-13 07:52:10.188  I/LaunchActivity ====: LaunchActivity TaskId: 17895 hasCode:66038913
//11-13 07:52:10.189  I/LaunchActivity ====: taskAffinity:sst.example.androiddemo.feature
//11-13 07:52:10.526  D/MainActivity:  onStop ====
//11-13 07:52:13.381  D/SingleTopActivity:  onCreate ====
//11-13 07:52:13.381  I/SingleTopActivity: SingleTopActivity TaskId: 17895 hasCode:241793112 ====
//11-13 07:52:13.382  I/SingleTopActivity: taskAffinity:sst.example.androiddemo.feature ====
//11-13 07:52:13.385  D/SingleTopActivity:  onStart ====
//11-13 07:52:13.386  D/SingleTopActivity:  onResume ====
//11-13 07:52:17.395  D/SingleTopActivity:  onPause ====
//11-13 07:52:17.395  D/SingleTopActivity:  onNewIntent ====
//11-13 07:52:17.396  D/SingleTopActivity:  onResume ====
//11-13 07:52:20.503  D/SingleTopActivity:  onPause ====
//11-13 07:52:20.504  D/SingleTopActivity:  onNewIntent ====
//11-13 07:52:20.505  D/SingleTopActivity:  onResume ====



//launch - singletop - other - singletop - singletop    跳转到自己是栈顶复用
// 任务栈从底部到顶部  launch- singletop - other 此时进入singleTop，singleTop不在栈顶，会重新创建，-singleTop,此时在栈顶，进行复用
// 07:44:55.501  I/LaunchActivity ====: LaunchActivity TaskId: 17895 hasCode:37618622
// 07:44:55.502  I/LaunchActivity ====: taskAffinity:sst.example.androiddemo.feature
// 07:44:55.883  D/MainActivity:  onStop ====
// 07:44:57.652  D/SingleTopActivity:  onCreate ====
// 07:44:57.653  I/SingleTopActivity: SingleTopActivity TaskId: 17895 hasCode:30949801 ====
// 07:44:57.654  I/SingleTopActivity: taskAffinity:sst.example.androiddemo.feature ====
// 07:44:57.656  D/SingleTopActivity:  onStart ====
// 07:44:57.657  D/SingleTopActivity:  onResume ====
// 07:45:06.675  D/SingleTopActivity:  onPause ====
//other activity  onCreate
// 07:45:07.053  D/SingleTopActivity:  onStop ====
// 07:45:13.093  D/SingleTopActivity:  onCreate ====
// 07:45:13.094  I/SingleTopActivity: SingleTopActivity TaskId: 17895 hasCode:170940899 ====
// 07:45:13.095  I/SingleTopActivity: taskAffinity:sst.example.androiddemo.feature ====
// 07:45:13.096  D/SingleTopActivity:  onStart ====
// 07:45:13.098  D/SingleTopActivity:  onResume ====
// 07:45:16.400  D/SingleTopActivity:  onPause ====
// 07:45:16.400  D/SingleTopActivity:  onNewIntent ====
// 07:45:16.401  D/SingleTopActivity:  onResume ====
// 07:45:18.230  D/SingleTopActivity:  onPause ====
// 07:45:18.230  D/SingleTopActivity:  onNewIntent ====
// 07:45:18.231  D/SingleTopActivity:  onResume ====
class SingleTopActivity : AppCompatActivity() {
    private val TAG = "SingleTopActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_single_top)
        Log.d(TAG," onCreate ==== ")
        printActivityInfo()
        findViewById<View>(R.id.singleTop2Other).setOnClickListener {
            val intent = Intent(this, OtherActivity::class.java)
            startActivity(intent)
        }
          findViewById<View>(R.id.singleTop2Self).setOnClickListener {
            val intent = Intent(this, SingleTopActivity::class.java)
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

        super.onNewIntent(intent)
    }

    fun printActivityInfo(){
        Log.i(TAG,   this.javaClass.simpleName + " TaskId: " + getTaskId() + " hasCode:" + this.hashCode()+" ====")
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


