package sst.example.androiddemo.feature.ffmpeg

import android.annotation.SuppressLint
import android.content.Context
import android.os.Process
import sst.example.androiddemo.feature.ffmpeg.CrashMonitor.CrashCallBack
import java.text.SimpleDateFormat

/**
 * @description:
 * @author: songshitong
 * @date: 2023/6/5
 */
interface Log {
  fun log(
    level:Int,
    threadId: String = "",
    threadName: String = "",
    methodName: String = "",
    methodParam: String = "",
    message:String="",
  )
}

/**
 * @description 日志级别
 * @author songshitong
 * @date 2023/6/5
 */
class Level{
  companion object{
    const val DEBUG = 0
    const val INFO = 1
    const val WARN = 2
    const val ERROR = 3
  }
}

data class Configuration(
  //日志文件目录
  val fileDir:String,
  //日志名的前半部分  自动拼接-yyyyMM-dd-HH-mm-ss.log形式结尾
  val fileNamePrefix:String,
  //单条日志最大长度，超过自动截取
  val singleLogUnit:Int = 4*1024,
  //每条日志之间的分割符
  val logSpliterator:String="\n",
  //日志信息之间的分隔符
  val strSplitter:String = "|",
  //缓存大小  .tmp为缓存文件
  val cacheBuffer:Int = 150*1024,
  //文件大小，超过自动分割
  val fileMaxLength:Int = 10*1024*1024,
  //日志时间格式
  val timePattern:String = "yyyy-MM-dd HH:mm:ss.SSS",
  //输出日志级别  默认全部输出，设置为Level.ERROR则等级以下的日志不输出
  val logLevel:Int = Level.DEBUG,
)

@SuppressLint("SimpleDateFormat")
class AFOLog(context: Context,val config:Configuration) : Log{
  private val pid:String =  Process.myPid().toString()
  private val pName:String
  private var sdf:SimpleDateFormat
  init {
    System.loadLibrary("native-lib")
    configMonitor()
    nInitLog(config)
    pName = context.applicationInfo.processName
    sdf = SimpleDateFormat(config.timePattern)
    //todo 目录权限校验
  }

  private fun configMonitor() {
    CrashMonitor(object : CrashCallBack {
      override fun onJavaCrash(e: Throwable) {
        flushCache()  //// TODO: 2023/5/29 多进程要单独处理吗？ 每个进程由业务方配置文件名字
      }
    })
  }

  override fun log(
    level: Int,
    threadId: String,
    threadName: String,
    methodName: String,
    methodParam: String,
    message: String
  ) {
    val time = System.currentTimeMillis()
    nLog(sdf.format(time),level,pid,pName,threadId,threadName,methodName,methodParam,message)
  }

  fun javaCrashTest() {
    val i = 0
    val sum = 10 / i
  }

  fun closeLog() {
    nCloseLog()
  }

  private external fun nInitLog(config: Configuration)

  private external fun nLog(
    time:String,
    level: Int,
    processId:String,
    processName:String,
    threadId: String,
    threadName: String,
    methodName: String,
    methodParam: String,
    message: String)

  private external fun flushCache()

  private external fun nNativeCrashTest()

  private external fun nCloseLog()

  fun nativeCrashTest() {
    nNativeCrashTest()
  }

}