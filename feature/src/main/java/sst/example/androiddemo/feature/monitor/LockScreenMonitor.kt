package sst.example.androiddemo.feature.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log

object LockScreenMonitor { //https://juejin.cn/post/6844903633084121096
  const val TAG = "LockScreenMonitor"
  private var filter: IntentFilter? = null

  fun register(context: Context){
    filter = IntentFilter()
    filter?.addAction(Intent.ACTION_SCREEN_ON)
    filter?.addAction(Intent.ACTION_SCREEN_OFF)
    filter?.addAction(Intent.ACTION_USER_PRESENT)
    context.registerReceiver(receiver, filter)

  }

  private val receiver = object: BroadcastReceiver(){
    override fun onReceive(context: Context?, intent: Intent?) {
      when(intent?.action){
        Intent.ACTION_SCREEN_ON ->{ // 开屏  屏幕亮了，停留在锁屏页面，没有进入用户页面
          Log.d(TAG,"ACTION_SCREEN_ON")
        }
        Intent.ACTION_SCREEN_OFF ->{  // 锁屏
          Log.d(TAG,"ACTION_SCREEN_OFF")
        }
        Intent.ACTION_USER_PRESENT ->{  // 解锁  进入用户界面
          Log.d(TAG,"ACTION_USER_PRESENT")
        }
      }
    }
  }

  fun destroy(context: Context){
    try {
      context.unregisterReceiver(receiver)
    }catch (e:Exception){
      e.printStackTrace()
    }
  }

}