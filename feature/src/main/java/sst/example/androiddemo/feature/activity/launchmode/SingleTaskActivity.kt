package sst.example.androiddemo.feature.activity.launchmode

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_single_task.*
import sst.example.androiddemo.feature.R

//不指定taskAffinity

//launch - singleTask - other - singleTask
//adb shell dumpsys activity activities 查看running activity 有 mainActivity,launchActivity,SingleTaskActivity
//任务栈 main-launch-singleTask-other    other进入singleTask，singleTask不在栈顶，复用该activity，回调onNewIntent方法，othActivity出栈

//11-14 07:30:12.232  I/LaunchActivity ====: LaunchActivity TaskId: 17973 hasCode:130542444
//11-14 07:30:12.234  I/LaunchActivity ====: taskAffinity:sst.example.androiddemo.feature
//11-14 07:30:12.632  D/MainActivity:  onStop ====
//11-14 07:30:15.641  D/SingleTaskActivity:  onCreate ====
//11-14 07:30:15.642  I/SingleTaskActivity: SingleTaskActivity TaskId: 17973 hasCode:78098524 ====
//11-14 07:30:15.644  I/SingleTaskActivity: taskAffinity:sst.example.androiddemo.feature ====
//11-14 07:30:15.646  D/SingleTaskActivity:  onStart ====
//11-14 07:30:15.648  D/SingleTaskActivity:  onResume ====
//11-14 07:30:18.733  D/SingleTaskActivity:  onPause ====
//11-14 07:30:18.747  D/OtherActivity:  onCreate ====
//11-14 07:30:19.105  D/SingleTaskActivity:  onStop ====
//11-14 07:30:23.026  D/SingleTaskActivity:  onNewIntent ====
//11-14 07:30:23.027  D/SingleTaskActivity:  onRestart ====
//11-14 07:30:23.028  D/SingleTaskActivity:  onStart ====
//11-14 07:30:23.029  D/SingleTaskActivity:  onResume ====

//指定taskAffinity为com.singleTask
//SingleTaskActivity所属的任务栈的TaskId发生了变换，也就是说开启了一个新的Task，并且之后的OtherActivity也运行在了该Task上 taskId相同
//adb shell dumpsys activity activities 打印出信息也证明了存在两个不同的Task,android 最近任务栏会出现两个应用

//以将两个不同App中的Activity设置为相同的taskAffinity，这样虽然在不同的应用中，但是Activity会被分配到同一个Task中去
//https://blog.piasy.com/2016/03/19/Android-Task-And-Back-Stack/index.html
//task是一个从用户角度出发的概念，它是一些activity的组合，它们组合起来是为了让用户完成某一件工作（或者说操作）。
//task内的activity们以栈的形式组织起来，也就是back stack了。栈内的activity不会重新排序，只能push或者pop。栈内的activity可以来自不同的app，因此可以是运行在不同的进程，但是它们都属于同一个task内。
//安卓系统是实时多task系统，用户可以随意在多个task之间切换。当一个task的栈内所有activity都pop之后，task也就销毁了。有时系统为了回收内存，会销毁activity，但是task不会销毁

//11-14 07:48:12.098  I/LaunchActivity ====: LaunchActivity TaskId: 17975 hasCode:201124667
//11-14 07:48:12.099  I/LaunchActivity ====: taskAffinity:sst.example.androiddemo.feature
//11-14 07:48:12.477  D/MainActivity:  onStop ====
//11-14 07:48:22.261  D/SingleTaskActivity:  onCreate ====
//11-14 07:48:22.263  I/SingleTaskActivity: SingleTaskActivity TaskId: 17976 hasCode:2979400 ====
//11-14 07:48:22.264  I/SingleTaskActivity: taskAffinity:com.singleTask ====
//11-14 07:48:22.266  D/SingleTaskActivity:  onStart ====
//11-14 07:48:22.268  D/SingleTaskActivity:  onResume ====
//11-14 07:48:33.384  D/SingleTaskActivity:  onPause ====
//11-14 07:48:33.402  D/OtherActivity:  onCreate ====
//11-14 07:48:33.775  D/SingleTaskActivity:  onStop ====
//11-14 07:48:38.521  D/SingleTaskActivity:  onNewIntent ====
//11-14 07:48:38.522  D/SingleTaskActivity:  onRestart ====
//11-14 07:48:38.524  D/SingleTaskActivity:  onStart ====
//11-14 07:48:38.525  D/SingleTaskActivity:  onResume ====


//两个不同应用指定相同taskAffinity   这两个activity的taskId相同，被分配到同一个任务栈
//11-15 07:53:55.823 2979-2979/com.example.associatedapp I/SameTaskAffinity ====: SameTaskAffinityActivity TaskId: 18076 hasCode:153651961 ====
//11-15 07:53:55.824 2979-2979/com.example.associatedapp I/SameTaskAffinity ====: taskAffinity:com.singleTask ====
//11-15 07:54:06.449 1690-1690/sst.example.androiddemo.feature D/MainActivity:  onRestart ====
//11-15 07:54:06.450 1690-1690/sst.example.androiddemo.feature D/MainActivity:  onStart ====
//11-15 07:54:06.450 1690-1690/sst.example.androiddemo.feature D/MainActivity:  onResume ====
//11-15 07:54:09.427 1690-1690/sst.example.androiddemo.feature D/MainActivity:  onPause ====
//11-15 07:54:09.443 1690-1690/sst.example.androiddemo.feature I/LaunchActivity ====: LaunchActivity TaskId: 18071 hasCode:197227075
//11-15 07:54:09.443 1690-1690/sst.example.androiddemo.feature I/LaunchActivity ====: taskAffinity:sst.example.androiddemo.feature
//11-15 07:54:09.762 1690-1690/sst.example.androiddemo.feature D/MainActivity:  onStop ====
//11-15 07:54:11.307 1690-1690/sst.example.androiddemo.feature D/SingleTaskActivity:  onCreate ====
//11-15 07:54:11.308 1690-1690/sst.example.androiddemo.feature I/SingleTaskActivity: SingleTaskActivity TaskId: 18076 hasCode:220755757 ====
//11-15 07:54:11.308 1690-1690/sst.example.androiddemo.feature I/SingleTaskActivity: taskAffinity:com.singleTask ====

class SingleTaskActivity : AppCompatActivity() {
    private val TAG = "SingleTaskActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_single_task)
        Log.d(TAG," onCreate ==== ")
        printActivityInfo()
        singleTask2Other.setOnClickListener {
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
