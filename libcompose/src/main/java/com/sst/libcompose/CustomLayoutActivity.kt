package com.sst.libcompose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.sst.libcompose.WeightScopeInstance.weight
import com.sst.libcompose.ui.theme.AndroidDemoTheme
import kotlin.math.roundToInt

class CustomLayoutActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val text =
      "this is text but long long long long long long long long long long long long long long"
    setContent {
      AndroidDemoTheme {
        // A surface container using the 'background' color from the theme
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          Column {
            //modifier形式进行布局
            Text(
              modifier = Modifier
                .background(Color.Blue.copy(alpha = 0.2f))
                .customLayoutModifier(),
              text = text
            )
            Spacer(modifier = Modifier.size(50.dp))

            //layout形式进行布局
            Layout(
              content = {
                Text(text = text)
              }, modifier = Modifier
                .background(Color.Blue.copy(alpha = 0.2f)) //此处背景设置给了layout
            ) { measurables, constraints ->
              val placeables = measurables.map { measurable ->
                // Measure each child
                measurable.measure(constraints)
              }
              var yPosition = 50
              var height = 0
              placeables.forEach { //计算高度
                height += it.height
              }
              layout(constraints.maxWidth, height) {
                placeables.forEach {
                  //放置每个child
                  it.placeRelative(0, yPosition)
                  yPosition += it.height
                }
              }
            }

            Spacer(modifier = Modifier.size(50.dp))
            Text("测试parentData")
            //parentData子元素向父传递信息
            // ParentData 的实际场景主要集中在父布局对子微件的特殊位置和大小的控制上，比如 Box 的 align，Column 和Row 的 align、alignBy、weight 上
            WeightedVerticalLayout(Modifier.padding(16.dp).height(200.dp)){//限制总高度为200
              //子元素按照1:2:3自动填充父  通过parentData将为weight信息传给父元素
              Box(modifier = Modifier.width(40.dp).weight(1f).background(Color.Red))
              Box(modifier = Modifier.width(40.dp).weight(2f).background(Color.Green))
              Box(modifier = Modifier.width(40.dp).weight(3f).background(Color.Blue))
            }
          }
        }
      }
    }
  }
}

//可以定义为扩展函数  也可以每次使用Modifier.layout进行单独构建，或者构建Modifier的对象也可以
private fun Modifier.customLayoutModifier(): Modifier =
  this then Modifier.layout { measurable, constraints ->
    // measurable：子元素的测量句柄，通过提供的api完成测量与布局过程
    // constraints: 子元素的测量约束，包括宽度与高度的最大值与最小值
    val placeable = measurable.measure(constraints) //对子元素进行测量   每个子元素只允许被测量一次!!!
    //measurable.parentData 子元素向父元素传递信息

    //对当前元素的宽度与高度进行指定   限定宽度为50
    layout(placeable.width, 50.dp.roundToPx()) {
      //在父的坐标系中指定子元素位置
      placeable.placeRelative(0, 50)//设置top为20
    }
  }

//parentData是一种Modifier
class WeightParentData(val weight: Float = 0f) : ParentDataModifier{
  override fun Density.modifyParentData(parentData: Any?): Any{
    return this@WeightParentData
  }
}

//在scope内，可以使用weight
interface WeightScope{
  @Stable
  fun Modifier.weight(weight:Float):Modifier
}

object WeightScopeInstance : WeightScope{
  override fun Modifier.weight(weight: Float): Modifier {
    return this then WeightParentData(weight)
  }
}

@Composable
fun WeightedVerticalLayout(
  modifier: Modifier = Modifier,
  content: @Composable WeightScope.() -> Unit
){
  val measurePolicy = MeasurePolicy { measurables, constraints ->
    // 获取子元素各weight值
    val weights = measurables.map {
      (it.parentData as WeightParentData).weight
    }

    //总的高度和总weight
    val totalHeight = constraints.maxHeight
    val totalWeight = weights.sum()

    val placeables = measurables.mapIndexed { i, mesurable ->
      // 根据比例计算高度
      val h = (weights[i] / totalWeight * totalHeight).roundToInt()
      mesurable.measure(constraints.copy(minHeight = h, maxHeight = h, minWidth = 0))
    }
    // 宽度：最宽的一项
    val width = placeables.maxOf { it.width }

    layout(width, totalHeight) {
      var y = 0
      //纵向排列元素
      placeables.forEachIndexed { i, placeable ->
        placeable.placeRelative(0, y)
        y += placeable.height
      }
    }
  }

  //WeightScopeInstance为具体实现
  Layout(modifier = modifier, content = { WeightScopeInstance.content() }, measurePolicy=measurePolicy)
}