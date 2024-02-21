package com.sst.libcompose

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.tooling.preview.Preview
import androidx.constraintlayout.widget.ConstraintLayout
import com.sst.libcompose.ui.theme.AndroidDemoTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      var a by rememberSaveable { //可在activity重建时保存
        mutableStateOf("Android")
      }
      var show by remember {
        mutableStateOf(true)
      }
      AndroidDemoTheme {
        // A surface container using the 'background' color from the theme
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          Column(content = {
            Greeting(a)
            Button(onClick = {
              a = "ios"
              show = !show
            }) {
              Text(text = "改变内容")
            }
            //加载本地图片 网络图片使用coil
            Image(
              modifier = Modifier.background(Color.Blue),
              painter = BitmapPainter(
                image = BitmapFactory.decodeResource(
                  resources,
                  android.R.drawable.arrow_down_float
                ).asImageBitmap()
              ), contentDescription = ""
            )
            if (show) { //控制view是否显示，只需要控制是否添加到compose函数即可
              Text(text = "文字显示了！")
            }
            Button(onClick = {
              startActivity(Intent(this@MainActivity, CustomDrawActivity::class.java))
            }) {
              Text(text = "自定义绘制")
            }
            Button(onClick = {
              startActivity(Intent(this@MainActivity, CustomLayoutActivity::class.java))
            }) {
              Text(text = "自定义layout")
            }

            Button(onClick = {
              startActivity(Intent(this@MainActivity, IntrinsicSizeActivity::class.java))
            }) {
              Text(text = "固有测量IntrinsicSize")
            }

            Button(onClick = {
              startActivity(Intent(this@MainActivity, SubComposeLayoutActivity::class.java))
            }) {
              Text(text = "SubComposeLayout")
            }
          })
        }
      }
    }
  }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(
    text = "Hello $name!",
    modifier = modifier
  )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  AndroidDemoTheme {
    Greeting("Android")
  }
}