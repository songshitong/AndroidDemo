package com.sst.libcompose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sst.libcompose.ui.theme.AndroidDemoTheme

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
            Spacer(modifier = Modifier.size(150.dp))

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

    //对当前元素的宽度与高度进行指定   限定宽度为50
    layout(placeable.width, 50.dp.roundToPx()) {
      //在父的坐标系中指定子元素位置
      placeable.placeRelative(0, 50)//设置top为20
    }
  }
