package sst.example.androiddemo.feature.monitor

import android.app.ActivityManager
import android.app.ActivityManager.MemoryInfo
import android.content.Context
import android.os.Build.VERSION_CODES
import androidx.annotation.RequiresApi

object MemoryMonitor {
  @RequiresApi(VERSION_CODES.M) fun getMemoryInfo(context: Context):MemoryInfo{
    val activityManager = context.applicationContext.getSystemService(ActivityManager::class.java)
    val memoryInfo = MemoryInfo()
    activityManager.getMemoryInfo(memoryInfo)
    return memoryInfo
  }
}