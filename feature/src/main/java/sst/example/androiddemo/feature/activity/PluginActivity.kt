package sst.example.androiddemo.feature.activity

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import dalvik.system.BaseDexClassLoader
import dalvik.system.DexClassLoader
import sst.example.androiddemo.feature.R
import java.io.File

class PluginActivity : AppCompatActivity() {
  lateinit var pluginPath:String
  lateinit var dexOptimizePath:String
  lateinit var dexNativeLibPath:String
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_plugin)
    pluginPath = File(filesDir,"plugin.apk").absolutePath
    dexOptimizePath = File(filesDir, "dexout").absolutePath
    dexNativeLibPath = File(filesDir, "pluginlib").absolutePath
    loadApk()
    val pluginDexClassLoader = DexClassLoader(pluginPath,dexOptimizePath,dexNativeLibPath,this::class.java.classLoader)
    findViewById<View>(R.id.btn_start_plugin_activity).setOnClickListener {

    }
  }

  private fun loadApk() {
    //将apk拷贝到file目录
    File(pluginPath).writeBytes(assets.open("Material-debug.apk").readBytes())
  }
}

class PluginInstrument : Instrumentation() {
  override fun newActivity(cl: ClassLoader?, className: String?, intent: Intent?): Activity {
    return super.newActivity(cl, className, intent)
  }

  override fun startActivitySync(intent: Intent?): Activity {
    return super.startActivitySync(intent)
  }
}