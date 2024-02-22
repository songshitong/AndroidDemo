package com.sst.libcompose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sst.libcompose.ui.theme.AndroidDemoTheme

class SubComposeLayoutActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      AndroidDemoTheme {
        // A surface container using the 'background' color from the theme
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          Column {
            Text(text = "通过SubComposeLayout实现分割线居中对齐",Modifier.padding(bottom = 20.dp))
            //适用于：子组件存在依赖关系，子组件间通过测量的先后顺序直接相互影响
            //利用 SubcomposeLayout，我们可以做到将某个子组件的合成过程延迟至他所依赖的组件测量结束后进行。这也说明这个组件可以根据其他组件的测量信息确定自身的尺寸，
            //     从而具备取代固有特性测量的能力。
            //SubcomposeLayout 具有更强的灵活性，然而性能上不如常规 Layout，因为子组件的合成需要要迟到父组件测量时才能进行，
            // 并且需要还需要额外创建一个子 Composition，因此 SubcomposeLayout 可能并不适用在一些对性能要求比较高的 UI 部分
            SubComposeRow(
              text = {
                Text(text = "A", modifier = Modifier.wrapContentWidth(Alignment.Start))
                Text(text = "B", modifier = Modifier.wrapContentWidth(Alignment.End))
              },
              divider = { heightDp ->
                Divider(
                  color = Color.Red,
                  modifier = Modifier
                    .width(4.dp)
                    .height(heightDp)
                )
              }
            )
          }
        }
      }
    }
  }

  @Composable
  fun SubComposeRow(text: @Composable () -> Unit, divider: @Composable (Dp) -> Unit) {
    SubcomposeLayout(
      modifier = Modifier
        .fillMaxWidth()
    ) { constraints ->
      var maxHeight = 0

      //先测量 text 中的所有 LayoutNode。并根据测量结果计算出最大高度
      var placeables = subcompose("text", text).map {
        var placeable = it.measure(constraints)
        maxHeight = placeable.height.coerceAtLeast(maxHeight)
        placeable
      }

      //高度传入分隔符组件中并进行测量
      var dividerPlaceable = subcompose("divider") {
        divider(maxHeight.toDp())
      }.map {
        it.measure(constraints.copy(minWidth = 0)) //防止minWidth为宽度
      }

      //设置宽高
      layout(constraints.maxWidth, constraints.maxHeight) {
        placeables.forEach {
          it.placeRelative(0, 0) //放置文字
        }
        dividerPlaceable.forEach {//放置分隔符
          it.placeRelative(constraints.maxWidth / 2, 0)
        }
      }
    }
  }
}

