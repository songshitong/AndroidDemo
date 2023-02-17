package sst.example.androiddemo.feature.widget

import android.content.Context
import android.content.Intent
import android.os.Build.VERSION_CODES
import android.provider.Settings
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.startActivity
import org.greenrobot.eventbus.EventBus
import sst.example.androiddemo.feature.R
import sst.example.androiddemo.feature.common.LiveEventBus

//全局弹窗不依赖activity上下文的弹窗。及时关闭app，同样可以浮在系统桌面上
//<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"/>
class SystemView : FrameLayout {
  private lateinit var windowManager:WindowManager
  companion object {
    @RequiresApi(VERSION_CODES.M)
    fun init(context: Context, wm: WindowManager) {
      if (Settings.canDrawOverlays(context)) {
        val view = SystemView(context,wm)
        val viewLayoutParams = WindowManager.LayoutParams()
        viewLayoutParams.width = 100
        viewLayoutParams.height =100
        view.layoutParams = viewLayoutParams
        wm.addView(
          view,
          WindowManager.LayoutParams().also {
            it.width = WindowManager.LayoutParams.WRAP_CONTENT
            it.height = WindowManager.LayoutParams.WRAP_CONTENT
            it.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            it.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            it.alpha = 0.95f //整个window的透明度
          }
        )
      } else {
        //若没有权限，提示获取.
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
        Toast.makeText(context, "需要取得权限以使用悬浮窗", Toast.LENGTH_SHORT).show()
        startActivity(context,intent,null)
      }

    }
  }

  constructor(context: Context,windowManager:WindowManager) : this(context, null){
    this.windowManager = windowManager
  }
  constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)
  constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : super(
    context,
    attributeSet,
    defStyleAttr
  ) {
    initView()
  }

  private fun initView() {
    LayoutInflater.from(context).inflate(R.layout.layout_system_view,this,true)
    findViewById<View>(R.id.system_view_close).setOnClickListener {
      windowManager.removeView(this)
    }

    findViewById<View>(R.id.system_gesture).setOnClickListener {
      // EventBus.getDefault().post("event")
    }
  }

}