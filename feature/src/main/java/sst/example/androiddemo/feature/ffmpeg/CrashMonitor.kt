package sst.example.androiddemo.feature.ffmpeg

/**
 * @description: 崩溃监控
 * @author: songshitong
 * @date: 2023/6/5
 */
class CrashMonitor(private val callBack: CrashCallBack) {
  init {
    nInitCrashMonitor() //监控native
    setJavaMonitor() //监控java
  }

  private fun setJavaMonitor() {
    val exceptionHandler = Thread.currentThread().uncaughtExceptionHandler
    Thread.currentThread().uncaughtExceptionHandler =
      Thread.UncaughtExceptionHandler { t: Thread?, e: Throwable ->
        callBack.onJavaCrash(e)
        if (t != null) {
          exceptionHandler?.uncaughtException(t, e)
        }
      }
  }

  private external fun nInitCrashMonitor()

  interface CrashCallBack{
    fun onJavaCrash(e: Throwable)
  }
}