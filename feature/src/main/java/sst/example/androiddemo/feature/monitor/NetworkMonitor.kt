package sst.example.androiddemo.feature.monitor

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build.VERSION_CODES
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

//https://www.jianshu.com/p/ed7c58ee2c11
object NetworkMonitor {

  @RequiresApi(VERSION_CODES.M) fun checkCapability(context: Context): Boolean {
    val connectivityManager: ConnectivityManager = context.getSystemService(
      ConnectivityManager::class.java
    )
    val networkCapabilities =
      connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
    return null != networkCapabilities && networkCapabilities.hasCapability(
      NetworkCapabilities.NET_CAPABILITY_VALIDATED
    )
  }

  //判断是否有外网连接（普通方法不能判断外网的网络是否连接，比如连接上局域网）
  fun checkNetworkUsePing(): Boolean {
    var result: String? = null
    try {
      val ip = "www.baidu.com" // ping 的地址，可以换成任何一种可靠的外网
      val p = Runtime.getRuntime().exec("ping -c 3 -w 100 $ip") // ping网址3次
      // 读取ping的内容，可以不加
      val input = p.inputStream
      val `in` = BufferedReader(InputStreamReader(input))
      val stringBuffer = StringBuffer()
      var content: String? = ""
      while (`in`.readLine().also { content = it } != null) {
        stringBuffer.append(content)
      }
      Log.d("------ping-----", "result content : $stringBuffer");
      // ping的状态  返回1的状态可能不同机型需要适配
      //  process.waitFor() 返回0，当前网络可用
      //  process.waitFor() 返回1，需要网页认证的wifi
      //  process.waitFor() 返回2，当前网络不可用
      val status = p.waitFor()
      if (status == 0) {
        result = "success"
        return true
      } else {
        result = "failed"
      }
    } catch (e: IOException) {
      result = "IOException"
    } catch (e: InterruptedException) {
      result = "InterruptedException"
    } finally {
//            Log.d("----result---", "result = " + result);
    }
    return false
  }
}