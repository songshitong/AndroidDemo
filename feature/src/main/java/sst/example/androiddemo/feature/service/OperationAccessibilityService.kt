package sst.example.androiddemo.feature.service

import android.accessibilityservice.AccessibilityButtonController
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.app.Instrumentation
import android.graphics.Path
import android.graphics.Rect
import android.os.Build.VERSION_CODES
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi

//https://developer.android.com/guide/topics/ui/accessibility/service?hl=zh-cn
class OperationAccessibilityService : AccessibilityService() {
  private var mAccessibilityButtonController: AccessibilityButtonController? = null
  private var accessibilityButtonCallback:
    AccessibilityButtonController.AccessibilityButtonCallback? = null
  private var mIsAccessibilityButtonAvailable: Boolean = false
  val mainHandler = object : Handler(Looper.getMainLooper()) {
    override fun dispatchMessage(msg: Message) {
      super.dispatchMessage(msg)
    }
  }

  @RequiresApi(VERSION_CODES.O)
  override fun onServiceConnected() {
    super.onServiceConnected()
    mAccessibilityButtonController = accessibilityButtonController
    mIsAccessibilityButtonAvailable =
      mAccessibilityButtonController?.isAccessibilityButtonAvailable ?: false

    if (!mIsAccessibilityButtonAvailable) return

    serviceInfo = serviceInfo.apply {
      flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON
    }

    accessibilityButtonCallback =
      @RequiresApi(VERSION_CODES.O)
      object : AccessibilityButtonController.AccessibilityButtonCallback() {
        override fun onClicked(controller: AccessibilityButtonController) {
          Log.d("MY_APP_TAG", "Accessibility button pressed!")
        }

        override fun onAvailabilityChanged(
          controller: AccessibilityButtonController,
          available: Boolean
        ) {
          if (controller == mAccessibilityButtonController) {
            mIsAccessibilityButtonAvailable = available
          }
        }
      }

    accessibilityButtonCallback?.also {
      mAccessibilityButtonController?.registerAccessibilityButtonCallback(it, mainHandler)
    }
  }

  @RequiresApi(VERSION_CODES.N)
  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    //接受到辅助事件
    Log.d("onAccessibilityEvent", event.toString())
    if (null == event) return
    if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
      val className = event.className
      if (className == "com.tencent.mm.ui.LauncherUI") {
        //微信启动
      }
      // mainHandler.postDelayed({
      //   swipe(500f,500f,0f,500f)
      // },5000)
    }
    if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
      if (event.text.firstOrNull() == "Discover") { //点击了Discover
        val node = findNode("Chats", rootInActiveWindow)
        mainHandler.postDelayed({
          if (node != null) {
            // clickNode(node)
            val rect = Rect()
            node.getBoundsInScreen(rect)
            click(rect.exactCenterX(),rect.exactCenterY())
          }

        }, 2000)
      }
    }
  }

  //方向轮盘
  //布局： 一个是外面的圆盘，一个就随手指移动的滑块
  //交互规则：
  // 1 滑块不能到圆盘外面，手指移动滑块，人物按照一定速度移动
  // 2 滑块与圆心的角度，是人物移动的方向
  // 3 手指松开，滑块回到圆心  手指不松开，滑块一直在手指的位置
  // 4 直接点园的某一部分，滑块会移动吗？？

  //根据文字找到node
  fun findNodeByText(text: String, rootNode: AccessibilityNodeInfo): List<AccessibilityNodeInfo>{
   return rootNode.findAccessibilityNodeInfosByText(text)
  }
  fun findNodeById(viewId: String, rootNode: AccessibilityNodeInfo): List<AccessibilityNodeInfo>{
    return rootNode.findAccessibilityNodeInfosByViewId(viewId)
  }

  fun findNode(name: String, rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
    // Log.d("findNode", "name $name  text:${rootNode.text}")
    var result:AccessibilityNodeInfo?=null

    if (rootNode.text == name) {
      result = rootNode
    } else {
      for (i in 0 until (rootNode.childCount)) {
        val nextNode = rootNode.getChild(i)
        if(null != nextNode){
          val childNode = findNode(name, nextNode)
          if (null != childNode) {
            result = childNode
            break
          }
        }
      }
    }
    return result
  }

  fun clickNode(node: AccessibilityNodeInfo) {
    var parent: AccessibilityNodeInfo? = node.parent
    while (parent != null) {
      if (parent.isClickable) {
        parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        break
      }
      parent = parent.parent
    }
  }

  //模拟点击 android7.0  部分坐标可能不可点击，无效
  @RequiresApi(VERSION_CODES.N) fun click(x: Float, y: Float) {
    val path = Path()
    path.moveTo(x, y)
    //startTime多久后开始点击
    val stroke = StrokeDescription(path, 0, 100L)
    val builder = GestureDescription.Builder()
    builder.addStroke(stroke)
    dispatchGesture(builder.build(), null, null)
  }

  @RequiresApi(VERSION_CODES.N) fun swipe(startX: Float, startY: Float, endX: Float, endY: Float) {
    val path = Path()
    path.moveTo(startX, startY)
    path.lineTo(endX, endY)
    val stroke = StrokeDescription(path, 0, 1000L)
    val builder = GestureDescription.Builder()
    builder.addStroke(stroke)
    dispatchGesture(builder.build(), null, null)
  }

  override fun onInterrupt() {
    //当系统要中断服务正在提供的反馈（通常是为了响应将焦点移到其他控件等用户操作）时，会调用此方法。此方法可能会在服务的整个生命周期内被调用多次
  }
}