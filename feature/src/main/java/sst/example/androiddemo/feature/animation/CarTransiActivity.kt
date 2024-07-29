package sst.example.androiddemo.feature.animation

import android.animation.ValueAnimator
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Path
import android.graphics.PathMeasure
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.addListener

data class Item(
  val view: View,
  val value:String="",
  val carSeries:String=""
)

class CarTransiActivity : AppCompatActivity() {
  lateinit var item1: Item
  lateinit var item2: Item
  lateinit var item3: Item
  lateinit var item4: Item
  lateinit var item5: Item
  private var windowHalf: Float = 0f
  private lateinit var valueView: TextView
  private lateinit var valueTipView: TextView
  private lateinit var carSeriesView : TextView
  private val itemList: MutableList<Item> = mutableListOf()
  private val time = (1.6 * 1000).toLong()
  private val transY = 600f
  private val transX = 300f
  private val interpolator = DecelerateInterpolator()
  private val bgAlpha = 0.7f
  private val diffAlpha = 1 - bgAlpha
  private val centerScale = 5.5f
  private val leftScale = 2f
  private val diffScale = centerScale - leftScale
  private val delayTime = 2000L
  private var centerIndex = 1
  private val valueDefault = "即将揭晓"
  private val mainHandler = Handler(Looper.getMainLooper())
  private val mRequestCode = 0x123

  override fun onCreate(savedInstanceState: Bundle?) {
    StatusBarUtil.setFullScreen(window)
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_car_transi)
    windowHalf = resources.displayMetrics.widthPixels/2f
    item1 = Item(findViewById(R.id.car1), "2*.98","问界M5")
    item2 = Item(findViewById(R.id.car2), "2*.98","理想L6")
    item3 = Item(findViewById(R.id.car3), valueDefault,"红旗国雅")
    item4 = Item(findViewById(R.id.car4), "4*.80","坦克700")
    item5 = Item(findViewById(R.id.car5), "2*.59","小米su7")
    valueView = findViewById(R.id.carvalue)
    carSeriesView = findViewById(R.id.carSeries)
    valueTipView = findViewById(R.id.carvalueTip)
    //按照左，中，右放置
    itemList.add(item2)
    itemList.add(item1)
    itemList.add(item3)
    itemList.add(item4)
    itemList.add(item5)
    item1.view.post {
      initPosition()
    }
    mainHandler.postDelayed({
      startAnim(itemList[0], itemList[1], itemList[2],itemList[3])
    }, delayTime)
    // startActivityForResult(Intent(createProjectionIntent()), mRequestCode)
  }

  private fun createProjectionIntent(): Intent {
    return (getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager).createScreenCaptureIntent()
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (mRequestCode == requestCode && null != data) {
      var connection: ServiceConnection? =null
      connection =  object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {

        }

        override fun onServiceDisconnected(name: ComponentName?) {}
      }
      val intent = Intent(
        this,
        RecorderService::class.java
      )
      intent.putExtra("resultCode",resultCode)
      intent.putExtra("data",data)
      bindService(intent, connection, Service.BIND_AUTO_CREATE)
    }
  }

  private fun initPosition() {
    item1.view.apply {
      scaleX = centerScale
      scaleY = centerScale
      translationY = transY*1.5f
      bringToFront()
    }

    item2.view.apply {
      translationX = -transX
      translationY = 250f
      scaleX = leftScale
      scaleY = leftScale
    }
    // item2.view.alpha = bgAlpha
    item3.view.apply {
      translationX = transX/2+150
      translationY = 50f
    }
    // item3.view.alpha = bgAlpha

    prepareEnterView()
    setCarValue()
  }

  private fun prepareEnterView() {
    item4.view.apply {
      translationX = windowHalf + item4.view.width / 2
    }
    item5.view.apply {
      translationX = windowHalf + item5.view.width / 2
    }
  }

  private fun setCarValue() {
    val valueShow = itemList[centerIndex].value
    if(valueShow == valueDefault){
      valueTipView.visibility = View.INVISIBLE
      valueView.text = valueShow
    }else{
      valueTipView.visibility = View.VISIBLE
      valueView.text =
        getString(R.string.app_value_show).replace("$",valueShow )
    }
    carSeriesView.text = itemList[centerIndex].carSeries
  }

  private fun startAnim(leftItem: Item, centerItem: Item, rightItem: Item,enterItem: Item) {
    val leftView = leftItem.view
    val centerView = centerItem.view
    val rightView = rightItem.view
    val enterView = enterItem.view
    //左侧
    val path = Path()
    path.moveTo(leftView.x, leftView.y)
    path.quadTo(leftView.x - transX * 1.5f, leftView.y + transY / 2, centerView.x, centerView.y)
    val pm = PathMeasure(path, false)
    val length = pm.length

    val animLeft = ValueAnimator.ofFloat(0f, length)
    animLeft.interpolator = interpolator
    animLeft.setDuration(time)
    animLeft.addUpdateListener {
      if(it.animatedFraction > 0.5){
        leftView.bringToFront()//要居中的view图层最高
      }

      val positionArray = FloatArray(2)
      val tanArray = FloatArray(2)
      pm.getPosTan(length * it.animatedFraction, positionArray, tanArray)
      leftView.x = positionArray[0]
      leftView.y = positionArray[1]
      leftView.scaleX = leftScale+diffScale * it.animatedFraction
      leftView.scaleY = leftScale+diffScale * it.animatedFraction
      // leftView.alpha = bgAlpha + diffAlpha * it.animatedFraction
    }

    //中间
    val pathCenter = Path()
    pathCenter.moveTo(centerView.x, centerView.y)
    // pathCenter.quadTo(
    //   centerView.x + transX * 2.5f,
    //   centerView.y - transY / 2,
    //   rightView.x,
    //   rightView.y
    // )
    pathCenter.lineTo(centerView.width/2+windowHalf*2,centerView.y)
    val pmCenter = PathMeasure(pathCenter, false)
    val lengthCenter = pmCenter.length

    val animCenter = ValueAnimator.ofFloat(0f, lengthCenter)
    animCenter.setDuration(time)
    animCenter.interpolator = interpolator
    animCenter.addUpdateListener {
      val positionArray = FloatArray(2)
      val tanArray = FloatArray(2)
      pmCenter.getPosTan(lengthCenter * it.animatedFraction, positionArray, tanArray)
      centerView.x = positionArray[0]
      centerView.y = positionArray[1]
      centerView.scaleX = centerScale - it.animatedFraction
      centerView.scaleY = centerScale - it.animatedFraction
      centerView.alpha = 1 - it.animatedFraction
    }

    //右边
    val pathRight = Path()
    pathRight.moveTo(rightView.x, rightView.y)
    pathRight.lineTo(leftView.x, leftView.y)
    val pmRight = PathMeasure(pathRight, false)
    val lengthRight = pmRight.length

    val animRight = ValueAnimator.ofFloat(0f, lengthRight)
    animRight.setDuration(time)
    animRight.interpolator = interpolator
    val rightDiffScale = leftScale -1
    animRight.addUpdateListener {
      val positionArray = FloatArray(2)
      val tanArray = FloatArray(2)
      pmRight.getPosTan(lengthRight * it.animatedFraction, positionArray, tanArray)
      rightView.x = positionArray[0]
      rightView.y = positionArray[1]
      rightView.scaleX = 1+rightDiffScale*it.animatedFraction
      rightView.scaleY = 1+rightDiffScale*it.animatedFraction
    }

    //进入动画
    val pathEnter = Path()
    pathEnter.moveTo(rightView.x-transX*2, rightView.y-transY)
    pathEnter.lineTo(rightView.x, rightView.y)
    val pmEnter = PathMeasure(pathEnter, false)
    val lengthEnter = pmEnter.length

    enterView.x = rightView.x
    enterView.y = rightView.y

    val animEnter = ValueAnimator.ofFloat(0f, lengthEnter)
    animEnter.setDuration(time)
    animEnter.interpolator = AccelerateInterpolator()
    animEnter.addUpdateListener {
      val positionArray = FloatArray(2)
      val tanArray = FloatArray(2)
      pmEnter.getPosTan(lengthEnter * it.animatedFraction, positionArray, tanArray)
      // enterView.x = positionArray[0]
      // enterView.y = positionArray[1]
      enterView.alpha = it.animatedFraction
    }

    animLeft.start()
    animCenter.start()
    animRight.start()
    animEnter.start()

    animLeft.addListener(
      onEnd = {
        val last = itemList.last()
        itemList.clear()
        //重新排序 新的左中右
        itemList.add(rightItem)
        itemList.add(leftItem)
        itemList.add(enterItem)
        itemList.add(last.apply(resetProperty())) //将要进入的重置
        itemList.add(centerItem)
        setCarValue()
        mainHandler.postDelayed({
          startAnim(itemList[0], itemList[1], itemList[2],itemList[3])
        }, delayTime)

      })
  }

  private fun resetProperty(): Item.() -> Unit = {
    view.alpha = 0f
    view.scaleX = 1f
    view.scaleY = 1f
  }
}