package com.sst.material

import android.animation.StateListAnimator
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.StateListDrawable
import android.graphics.drawable.shapes.RoundRectShape
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewOutlineProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.elevation.ElevationOverlayProvider
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.RoundedCornerTreatment
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.shape.TriangleEdgeTreatment
import  com.google.android.material.imageview.ShapeableImageView

class ShapeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shape)

        ///负责形状的角度，边缘(图标，聊天气泡的角)
//        RoundedCornerTreatment
//        TriangleEdgeTreatment  inside true 向内凹，false 向外凸
        ///注意父布局的切割 clipchildren clippadding
        val shapePathModel = ShapeAppearanceModel.builder()
            .setAllCorners(RoundedCornerTreatment())
            .setAllEdges(TriangleEdgeTreatment(10f,true))
            .setAllCornerSizes(10f)
            .build()

        ///MaterialShapeDrawable 复制shape的 shadows, elevation, scale and color
        val backgroundDrawable = MaterialShapeDrawable(shapePathModel).apply {
            setTint(Color.parseColor("#bebebe"))
            paintStyle = Paint.Style.FILL
            //缩放
            scale=1f

            //阴影
            initializeElevationOverlay(this@ShapeActivity)
            elevation=20f
            //阴影旋转角度 0在下面  360在上面
            /// offset和radius控制阴影位置
            /// drawableState.shadowCompatRadius = (int) Math.ceil(z * SHADOW_RADIUS_MULTIPLIER);
            //    drawableState.shadowCompatOffset = (int) Math.ceil(z * SHADOW_OFFSET_MULTIPLIER);
            shadowCompatRotation=360
        }
        findViewById<MaterialButton>(R.id.materialShapeBtn).background=backgroundDrawable


        val siv:ShapeableImageView = ShapeableImageView(this)
//        siv 通过 app:shapeAppearanceOverlay="@style/RectCornerStyle" 设置形状和角度
//        <style name="RectCornerStyle">
//        <item name="cornerFamily">rounded</item>
//        <item name="cornerSize">0dp</item>
//        </style>
    }
}