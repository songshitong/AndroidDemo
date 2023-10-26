package sst.example.androiddemo.feature.monitor

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Build.VERSION_CODES
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

//https://www.jianshu.com/p/ed7c58ee2c11
object NetworkMonitor {
  //查询手机信号强度  点击状态信息里面的sim卡，-95dbm，45asu
  //dbm和asu   https://www.cnblogs.com/lfri/p/10295938.html
  // 首先明确：dBm 和 asu 是两个独立的单位，它们的换算关系不唯一。
  // 在 2G/3G 网络下：dBm = -113+2*asu
  // 在 4G 网络下：dBm = -140+asu
  //
  // dBm 和 asu 都用来表示手机信号强度，其中 dBm 是通用单位，asu 是安卓手机特有单位。
  // dBm 是一个表示功率绝对值的值（也可以认为是以 1mW 功率为基准的一个比值），计算公式为：10log(功率值/1mw)。
  // 　　[例] 如果功率 P 为 1mw，折算为 dBm 后为 0dBm。
  // 　　[例] 对于 0.01mW 的功率，按 dBm 单位进行折算后的值应为：10log(0.01/1) = -20dBm。
  // 这个数值越大，表明信号越好。由于手机信号强度一般较小，折算成为 dBm 一般都是负数
  //正常手机信号变化范围是从 -110dBm (差)到 -50dBm (好)之间，如果比 -50dBm 还小的话，说明你就站在基站的附近
  //
  // asu 是英文 alone signal unit 的简写，是 google 为 android 设备定义的信号强度单位。

  //获取运行商
  // gsm   2G
  // getEvdoDbm()	电信 3G
  // getCdmaDbm()	联通 3G
  // getLteDbm()	4G

  //dbm部分判断   需要判断网络类型然后计算
  //https://blog.csdn.net/YTYT5200/article/details/117448805
  // if (dbm > -75) {
  //   bin = "网络很好";
  // } else if (dbm > -85) {
  //   bin = "网络不错";
  // } else if (dbm > -95) {
  //   bin = "网络还行";
  // } else if (dbm > -100) {
  //   bin = "网络很差";
  // } else {
  //   bin = "网络错误";
  // }
  //asu部分判断
  // if (asu < 0 || asu >= 99) bin = "网络错误";
  // else if (asu >= 16) bin = "网络很好";
  // else if (asu >= 8) bin = "网络不错";
  // else if (asu >= 4) bin = "网络还行";
  // else bin = "网络很差";

  //信号强度0-4 数字越大信号越好
  fun getSingleLevel(context: Context):Int{
    val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    var result = -1
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
      telephonyManager.signalStrength?.level?.let {
        result = it
      }
    }
    return result
  }



  //判断是否可用
  @RequiresApi(VERSION_CODES.M) fun checkCapability(context: Context): Boolean {
    val connectivityManager: ConnectivityManager = context.applicationContext.getSystemService(
      ConnectivityManager::class.java
    )
    val networkCapabilities =
      connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
    return null != networkCapabilities && networkCapabilities.hasCapability(
      NetworkCapabilities.NET_CAPABILITY_VALIDATED
    )
  }

  fun getSpeed(context: Context):SpeedInfo{
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val nc = cm.getNetworkCapabilities(cm.activeNetwork)
    val downSpeed = nc?.linkDownstreamBandwidthKbps
    val upSpeed = nc?.linkUpstreamBandwidthKbps
    return SpeedInfo(downSpeed, upSpeed)
  }

  fun checkWifiSpeed(context: Context){
    val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    val linkSpeed = wifiManager.connectionInfo.linkSpeed
    val rssi = wifiManager.connectionInfo.rssi //信号强度
    val level = wifiManager.calculateSignalLevel(linkSpeed) //返回等级，类似右上WiFi的等级图标
  }

  //获取网络类型 0无网络，1移动网络,2wifi
  fun getNetworkType(context: Context):Int{
    val connectivityManager: ConnectivityManager = context.applicationContext.getSystemService(
      ConnectivityManager::class.java
    )
     //新api使用 networkCapabilities
    val type = connectivityManager.activeNetworkInfo?.type
    if(ConnectivityManager.TYPE_MOBILE == type){
      return 1
    }
    if(ConnectivityManager.TYPE_WIFI == type){
      return 2
    }
    return 0
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

data class SpeedInfo(val downSpeed:Int?,val upSpeed:Int?)