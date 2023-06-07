package sst.example.androiddemo.feature.ffmpeg

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build.VERSION
import android.os.Process
import sst.example.androiddemo.feature.ffmpeg.CrashMonitor.CrashCallBack
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat

/**
 * @description:
 * @author: songshitong
 * @date: 2023/6/5
 */
interface Log {
  fun log(
    level: Int,
    threadId: String = "",
    threadName: String = "",
    methodName: String = "",
    methodParam: String = "",
    message: String = "",
  )

  /**
   * @Description: 清除所有日志
   */
  fun clear()
}

/**
 * @description 日志级别
 * @author songshitong
 * @date 2023/6/5
 */
class Level {
  companion object {
    const val DEBUG = 0
    const val INFO = 1
    const val WARN = 2
    const val ERROR = 3
  }
}

data class Configuration(
  //日志文件目录
  val fileDir: String,
  //日志名的前半部分  自动拼接-yyyyMM-dd-HH-mm-ss.log形式结尾  一秒内生成日志大于fileMaxLength可能发生文件覆盖
  val fileNamePrefix: String,
  //单条日志最大长度，超过自动截取
  val singleLogUnit: Int = 4 * 1024,
  //每条日志之间的分割符
  val logSpliterator: String = "\n",
  //日志信息之间的分隔符
  val strSplitter: String = "|",
  //缓存大小  .tmp为缓存文件
  val cacheBuffer: Int = 150 * 1024,
  //文件大小，超过自动分割
  val fileMaxLength: Int = 10 * 1024 * 1024,
  //日志时间格式
  val timePattern: String = "yyyy-MM-dd HH:mm:ss.SSS",
  //输出日志级别  默认全部输出，设置为Level.ERROR则等级以下的日志不输出
  val logLevel: Int = Level.DEBUG,
  //额外信息 文件的首行打印，可输出appVersion,sdk版本等
  val extraInfo: String = ""
)

@SuppressLint("SimpleDateFormat")
class AFOLog(context: Context, val config: Configuration) : Log {
  private val pid: String = Process.myPid().toString()
  private val pName: String
  private var sdf: SimpleDateFormat

  companion object {
    const val TAG = "AFOLog"
    const val VERSION = "1.0"
  }

  init {
    System.loadLibrary("native-lib")
    if (!File(config.fileDir).canWrite()) {
      android.util.Log.e(TAG, "当前目录没有写权限，请申请相关权限")
    }
    configMonitor()
    nInitLog(config)
    pName = context.applicationInfo.processName
    sdf = SimpleDateFormat(config.timePattern)
  }

  private fun configMonitor() {
    CrashMonitor(object : CrashCallBack {
      override fun onJavaCrash(e: Throwable) {
        val sw = StringWriter()
        e.printStackTrace(PrintWriter(sw))
        log(Level.ERROR, "", "", "", "", sw.toString())
        flushCache()
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
    if (level < config.logLevel) {
      return
    }
    val time = System.currentTimeMillis()
    nLog(
      sdf.format(time),
      level,
      pid,
      pName,
      threadId,
      threadName,
      methodName,
      methodParam,
      message
    )
  }

  override fun clear() {
    File(config.fileDir).listFiles()?.forEach {
      it.delete()
    }
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
    time: String,
    level: Int,
    processId: String,
    processName: String,
    threadId: String,
    threadName: String,
    methodName: String,
    methodParam: String,
    message: String
  )

  private external fun flushCache()

  private external fun nNativeCrashTest()

  private external fun nCloseLog()

  fun nativeCrashTest() {
    nNativeCrashTest()
  }
}