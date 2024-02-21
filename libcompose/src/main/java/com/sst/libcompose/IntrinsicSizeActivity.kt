package com.sst.libcompose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.sst.libcompose.ui.theme.AndroidDemoTheme

class IntrinsicSizeActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      AndroidDemoTheme {
        // A surface container using the 'background' color from the theme
        Surface {
          Column {
            Row (modifier= Modifier
              .fillMaxWidth()
              .height(IntrinsicSize.Min)){
              //weight设置text的宽度
              Text(text = "A",modifier = Modifier
                .weight(1f)
                .wrapContentWidth(Alignment.Start))
              Divider(color = Color.Red, modifier = Modifier
                .fillMaxHeight()
                .width(2.dp))
              Text(text = "B",modifier = Modifier
                .weight(1f)
                .wrapContentWidth(Alignment.End))
            }
            Spacer(modifier = Modifier.size(50.dp))
            CustomLayoutIntrinsic(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
              //设置普通元素的layoutId
              Text(text = "A", Modifier.wrapContentWidth(Alignment.Start).layoutId("main"))
              Divider(
                color = Color.Red,
                modifier = Modifier
                  .width(4.dp)
                  .fillMaxHeight()
                  .layoutId("divider")
              )
              Text(text = "B", Modifier.wrapContentWidth(Alignment.End).layoutId("main"))
            }
          }

        }
      }
    }
  }

  @Composable
  fun CustomLayoutIntrinsic(modifier: Modifier, content: @Composable () -> Unit){
     Layout(content = content, modifier = modifier,object : MeasurePolicy{
       override fun MeasureScope.measure(
         measurables: List<Measurable>,
         constraints: Constraints
       ): MeasureResult {
         //设置divider的宽度不为maxWidth
         var devideConstraints = constraints.copy(minWidth = 0)
         //测量其他元素
         var mainPlaceables = measurables.filter {
           it.layoutId == "main"
         }.map {
           it.measure(constraints)
         }
         //测量divider
         var devidePlaceable = measurables.first { it.layoutId == "divider"}.measure(devideConstraints)
         var midPos = constraints.maxWidth / 2
         return layout(constraints.maxWidth, constraints.maxHeight) {
           //放置其他元素
           mainPlaceables.forEach {
             it.placeRelative(0, 0)
           }
           //放置divider
           devidePlaceable.placeRelative(midPos, 0)
         }
       }

       //这里只适配Modifier.height(IntrinsicSize.Min)
       override fun IntrinsicMeasureScope.minIntrinsicHeight(
         measurables: List<IntrinsicMeasurable>,
         width: Int
       ): Int {
         var maxHeight = 0
         measurables.forEach {
           //maxIntrinsicHeight 即可获取到每个子组件在给定宽度下能够保证正确展示的最小高度，这个正确展示的高度是由子组件来保证的
           maxHeight = it.minIntrinsicHeight(width).coerceAtLeast(maxHeight)
         }
         //父组件包容子组件需要的最大高度
         return maxHeight
       }

     })
  }
}


