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
import androidx.lifecycle.Observer
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import sst.example.androiddemo.feature.common.LiveEventBus
import kotlin.random.Random
import kotlin.random.nextInt

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

  init {
    EventBus.getDefault().register(this)
  }

  @RequiresApi(VERSION_CODES.N)
  @Subscribe(threadMode = ThreadMode.MAIN)
  fun onMessageEvent( event:String)
  {
    Log.d("receiveEvent","event $event")
   swipe(600f,1500f,200f,1500f)
    // click(2110f+ Random.Default.nextInt(-20,30), 798f+Random.Default.nextInt(-30,50))
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
  // 4 直接点园的某一部分，滑块可以移动
  // 5 人的行为是按住滑块，在外圆拖动

  //轮盘位置 调节   方便不同用户使用

  //轮盘类型
  //固定型  外圆整体固定不动
  //移动型  外圆跟随手指移动，松手后回到原位
  //悬浮性 一直跟随手指

  //摇杆类型
  //触控板  中间为滑块
  //键盘  类似手柄的四个方向键   是否支持同时两个方向取决于游戏是否支持
  //控制棒 与触控板类似，外圆小点，内园更大 类似一个大按钮在里面移动

  //控制方向 8个方向 16个方向 32个方向 控制方向移动的精度
  //移动敏感度  轮盘移动距离与人物移动距离的映射

  //移动方向引导线  显示人物移动方向  对于2D游戏距离判断有好处
  //攻击距离引导线  攻击时才显示，有一定误差， 不处于攻击状态时不显示，用户攻击距离与方向不好判断

  //技能触发方式
  // 点按  长按   拖拽    攻击键拖拽触发上挑，直刺，攻击并后跳等

  //按键半透明
  //按键空白区域是否为半透明还是纯色   纯色有利于脚本识别

  //技能排布
  //标准型  扇形(围绕攻击键，攻击键比较大,其他技能扇形)
  // 按键排列(排列紧凑，攻击键小，技能一排一排的)  容易误触，但是技能多时，操作区域更适合手小的
  // 组合型(某个位置放置一系列技能，减少技能栏数量，方便操作)      自由配置，用户调整整体技能区域

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
    val stroke = StrokeDescription(path, 20, 200)
    val builder = GestureDescription.Builder()
    builder.addStroke(stroke)
    dispatchGesture(builder.build(), object :GestureResultCallback(){
      override fun onCancelled(gestureDescription: GestureDescription?) {
        super.onCancelled(gestureDescription)
        Log.d("swipe","send swipe Cancelled")
      }

      override fun onCompleted(gestureDescription: GestureDescription?) {
        super.onCompleted(gestureDescription)
        Log.d("swipe","send swipe onCompleted")
      }

                                                                    } , null)
  }

  override fun onInterrupt() {
    //当系统要中断服务正在提供的反馈（通常是为了响应将焦点移到其他控件等用户操作）时，会调用此方法。此方法可能会在服务的整个生命周期内被调用多次
  }
}