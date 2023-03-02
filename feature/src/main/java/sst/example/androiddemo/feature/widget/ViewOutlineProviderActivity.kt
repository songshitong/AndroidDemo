package sst.example.androiddemo.feature.widget

import android.graphics.Outline
import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewOutlineProvider
import sst.example.androiddemo.feature.R

///利用ViewOutlineProvider实现形状 https://developer.android.google.cn/training/material/shadows-clipping?hl=zh-cn
// 成本高，不要用于动画
/// xml
/// view.setOutLineProvdier  android5.0
class ViewOutlineProviderActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_outline_provider)
        findViewById<View>(R.id.outlineImage).outlineProvider=object : ViewOutlineProvider(){
            override fun getOutline(view: View?, outline: Outline?) {
                //矩形
                // outline.setRect(Rect(0,0,view.width,view.width))
                //圆形
                outline?.setOval(0,0,view?.width?:0,view?.height?:0)
                //圆角
                //outline.setRoundRect(0, 0, view.width, view.height, view.width / 2f)
                //设置路径
                // outline.setPath()
            }
        }

//将视图裁剪至其轮廓区域  xml中设置android:clipToOutline 也可以
        findViewById<View>(R.id.outlineImage).clipToOutline = true
    }
}