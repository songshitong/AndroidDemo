package com.sst.material

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomappbar.BottomAppBarTopEdgeTreatment
import com.google.android.material.card.MaterialCardView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.MarkerEdgeTreatment
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.RoundedCornerTreatment
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.shape.TriangleEdgeTreatment
import kotlin.math.ceil

class ShapeActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_shape)

    ///负责形状的角度，边缘(图标，聊天气泡的角)
//        RoundedCornerTreatment圆角  CutCornerTreatment切角
//        TriangleEdgeTreatment  inside true 向内凹，false 向外凸   边上是否有三角形挖空   OffsetEdgeTreatment给个位移
    //可以自定义EdgeTreatment   MarkerEdgeTreatment外部画一个箭头       BottomAppBarTopEdgeTreatment 应该是中间带有悬浮按钮的效果
    ///注意父布局的切割 clipchildren clippadding
    val shapeModel = ShapeAppearanceModel.builder()
      .setAllCorners(RoundedCornerTreatment())
      .setRightEdge(TriangleEdgeTreatment(10f,true))
      .setBottomEdge(MarkerEdgeTreatment(200f))
      // 不设置阴影不生效  ShapeAppearanceModel.isRoundRect为true,直接绘制圆角矩形，不绘制path阴影
      //判断条件true=四条边都是EdgeTreatment&&四个角的角度一致&&四个角都是RoundedCornerTreatment
      .setAllCornerSizes(50f)
      .build()
    val view = findViewById<MaterialCardView>(R.id.materialShapeBtn)
    ///MaterialShapeDrawable 复制shape的 shadows, elevation, scale and color
    //注意使用新建的shapeModel，使用view.shapeAppearanceModel,后面的不生效
    val backgroundDrawable = object : MaterialShapeDrawable(shapeModel) {
      override fun requiresCompatShadow(): Boolean {
        //重写后shadowColor才生效
        return true
      }
    }.apply {
      //填充颜色 不能设置Color.TRANSPARENT，阴影效果出不来
      // setTint(Color.TRANSPARENT)
      alpha = 100
      paintStyle = Paint.Style.FILL
      fillColor = ColorStateList.valueOf(Color.TRANSPARENT)
      //缩放
      scale = 1f

      //圆角还是切角
      setCornerSize(10f)

      //阴影
      initializeElevationOverlay(this@ShapeActivity)
      elevation = 100f
      //阴影旋转角度 0在下面  360在上面
      /// offset和radius控制阴影位置
      //  constantState.shadowCompatRadius = ceil((z * .75f).toDouble()).toInt()
       // shadowCompatOffset = ceil((z * .75f).toDouble()).toInt()
      shadowRadius = 20
      shadowCompatRotation = 360
      //阴影的颜色com/google/android/material/shadow/ShadowRenderer.java
      //阴影的颜色渐变start 26%   middle 8%  end 0%
      setShadowColor(Color.GREEN)//需要重写requiresCompatShadow
    }
    view.background = backgroundDrawable

    val siv: ShapeableImageView = ShapeableImageView(this)
//        siv 通过 app:shapeAppearanceOverlay="@style/RectCornerStyle" 设置形状和角度
//        <style name="RectCornerStyle">
//        <item name="cornerFamily">rounded</item>
//        <item name="cornerSize">0dp</item>
//        </style>
  }
}