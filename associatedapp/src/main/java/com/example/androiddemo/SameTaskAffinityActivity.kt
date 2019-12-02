package com.example.androiddemo

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class SameTaskAffinityActivity : AppCompatActivity() {
    private val TAG = "SameTaskAffinity ===="
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_same_task_affinity)
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
