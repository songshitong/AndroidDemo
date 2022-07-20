package sst.example.androiddemo.feature.animation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_view_property_animator.btn_view_property_animator
import sst.example.androiddemo.feature.R

class ViewPropertyAnimatorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_property_animator)
        //z 轴的变化 比较两张图片
//        todo自定义动画 animation
        btn_view_property_animator.setOnClickListener {
            btn_view_property_animator.animate().let {
                it.duration = 2000
                it.translationX(200f)
                it.start()
            }
        }
    }
}
