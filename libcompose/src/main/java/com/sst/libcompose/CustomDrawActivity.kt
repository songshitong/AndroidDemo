package com.sst.libcompose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.sst.libcompose.ui.theme.AndroidDemoTheme

class CustomDrawActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      AndroidDemoTheme {
        // A surface container using the 'background' color from the theme
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          Column {
            //DrawScope提供绘制的方法  DrawScope.drawContext.canvas提供具体平台的canvas
            val radius = 100.dp
            Canvas(modifier = Modifier.size(radius, radius), onDraw = {
              drawCircle(
                brush = Brush.sweepGradient(
                  listOf(Color.Red, Color.Green, Color.Red),
                  Offset(radius.toPx() / 2f, radius.toPx() / 2f)
                ),
                radius = radius.toPx() / 2f,
                style = Stroke(
                  width = 20.dp.toPx()
                )
              )
            })
            Spacer(modifier = Modifier.size(100.dp, 20.dp))
            //canvas是由space和modifier.drawBehind实现的
            // fun Canvas(modifier: Modifier, onDraw: DrawScope.() -> Unit) =
            //   Spacer(modifier.drawBehind(onDraw))

            //也可以直接使用Modifier进行绘制
            //modifier.drawBehind 绘制在后面
            //Modifier.drawWithContent 手动控制层级  ContentDrawScope.drawContent绘制内容，其他顺序自己控制
            //Modifier.drawWithCache  drawWithCache缓存，onDrawWithContent内容Recompose
            var borderColor by remember {
              mutableStateOf(Color.Blue)
            }
            Card(modifier = Modifier
              .size(100.dp)
              .drawWithCache {
                println("drawWithCache 发生compose")
                val path = Path().apply {
                  moveTo(0f, 0f)
                  relativeLineTo(100.dp.toPx(), 0f)
                  relativeLineTo(0f, 100.dp.toPx())
                  relativeLineTo(-100.dp.toPx(), 0f)
                  relativeLineTo(0f, -100.dp.toPx())
                }
                onDrawWithContent {
                  println("onDrawWithContent 发生compose")
                  drawContent()
                  drawPath(path, color = borderColor, style = Stroke(10f))
                }
              }) {
               println("Text 发生compose")
              Text(text = "THIS IS A TEXT")
            }
            Button(onClick = { //点击按钮只有onDrawWithContent再次执行
              borderColor = if (borderColor == Color.Red) {
                Color.Blue
              } else {
                Color.Red
              }
            }) {
              Text(text = "改变边框颜色")
            }
          }
        }
      }
    }
  }
}


