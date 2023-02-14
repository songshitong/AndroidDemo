package com.example.opencv

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.core.content.ContextCompat
import org.opencv.android.Utils
import org.opencv.core.Mat
import kotlin.experimental.and

class PictureFilterActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_picture_filter)

    // 通过对RGB三个颜色分量的调整可以将照片处理成一种老照片的怀旧风格
    // 调整公式：
    // R=0.393 * r + 0.769 * g + 0.189 * b
    // G=0.349 * r + 0.686 * g + 0.168 * b
    // B=0.272 * r + 0.534 * g + 0.131 * b
    // 要完成对颜色分量的调整，可利用Mat的各种像素操作。
    // 通过不同的公式可以达到不同滤镜的效果。
    val bitmap = BitmapFactory.decodeResource(resources,R.drawable.shanji)
    findViewById<ImageView>(R.id.filter_img).setImageBitmap(modify(bitmap))
  }

  private fun modify(srcBitmap: Bitmap): Bitmap {
    val mat = Mat()
    Utils.bitmapToMat(srcBitmap, mat)

    //通道数
    val channels = mat.channels()
    //宽度
    val col = mat.cols()
    //高度 等同于srcMat.height()
    val row = mat.rows()
    //类型
    val type = mat.type()
    Log.d("TAG", "通道数： $channels 宽度：$col 高度：$row 类型：$type")

    //定义一个数组，用来存储每个像素点数据，数组长度对应图片的通道数
    //至于为什么用byte，那就要看文档的对照表，不同图片类型对应不同java数据类型，这里CV_8UC4，对应Java byte类型
    val p = ByteArray(channels)

    var r = 0
    var g = 0
    var b = 0

    //循环遍历每个像素点，对每个像素点进行操作
    for (h in 0 until row) {
      for (w in 0 until col) {
        //通过像素点位置得到该像素带点的数据，并存入p数组中   通过jni获取，有多少像素，调用多少次jni
        //将像素写回的mat.put也会调用jni
        //可以优化为 读取每一行或者全部读取后操作，内存占用会上升
        mat.get(h, w, p)
        if(p.isEmpty()) continue

        //得到一个像素点的RGB值
        //这里为啥要 & 0xff   补码的事情。byte类型的数字要&0xff再赋值给int类型，byte是一字节，而int类型是4字节，如果不做补码操作，
        // 就会导致二进制数据的一致性丢失掉,这个问题的产生的原因和计算机存储数据的方式有关，负数，会取反然后+1存储
        r = (p[0] and 0xff.toByte()).toInt()
        g = ((p[1] and 0xff.toByte()).toInt())
        b = (p[2] and 0xff.toByte()).toInt()

        //根据怀旧图片滤镜公式进行计算
        var AR = (0.393 * r + 0.769 * g + 0.189 * b)
        var AG = (0.349 * r + 0.686 * g + 0.168 * b)
        var AB = (0.272 * r + 0.534 * g + 0.131 * b)

        //防越界判断，byte最大数值是255。
        AR = limitParam(AR)
        AG = limitParam(AG)
        AB = limitParam(AB)

        //把修改后的数据重新写入数组
        p[0] = AR.toInt().toByte()
        p[1] = AG.toInt().toByte()
        p[2] = AB.toInt().toByte()

        //把数组写入像素点
        mat.put(h, w, p)
      }
    }

    val dstBitmap = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(mat, dstBitmap)
    mat.release()
    return dstBitmap
  }

  private fun limitParam(num: Double) = if (num > 255.0) {
    255.0
  } else {
    if (num < 0.0) {
      0.0
    } else {
      num
    }
  }
}