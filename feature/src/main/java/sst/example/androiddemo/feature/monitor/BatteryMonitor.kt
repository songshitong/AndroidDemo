package sst.example.androiddemo.feature.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_BATTERY_CHANGED
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build.VERSION_CODES
import android.os.PowerManager
import android.util.Log
import androidx.annotation.RequiresApi

object BatteryMonitor {
  const val TAG = "BatteryMonitor"
  var batteryLevel: Int=0
    private set
  var isCharging:Boolean = false
    private set
  @RequiresApi(VERSION_CODES.M) fun register(context: Context){
    val filter = IntentFilter()
    filter.addAction(ACTION_BATTERY_CHANGED)
    context.applicationContext.registerReceiver(receiver,filter)
    batteryLevel = getBatteryFromManager(context.applicationContext) //更新一次电量
  }

  //是否省电模式 华为，小米不适用 https://juejin.cn/post/7056354420246642719
  @RequiresApi(VERSION_CODES.M) fun isSaveMode(context: Context):Boolean{
    val powerManager = context.getSystemService(
      PowerManager::class.java
    )
    return powerManager.isPowerSaveMode
  }

  @RequiresApi(VERSION_CODES.M) fun getBatteryFromManager(context: Context):Int{
    val batteryManager = context.applicationContext.getSystemService(
      BatteryManager::class.java
    )
    // batteryManager.isCharging 获取不准  建议使用广播获取
    return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
  }

  fun destroy(context: Context){
    try {
      context.applicationContext.unregisterReceiver(receiver)
    }catch (e:Exception){
      e.printStackTrace()
    }
  }

  private val receiver = object :BroadcastReceiver(){
    override fun onReceive(context: Context?, intent: Intent?) {
       batteryLevel= intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)?:-1
       isCharging = (intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)?:-1) != 0
      Log.d(TAG,"batteryLevel $batteryLevel  isCharging $isCharging")
    }
  }
}