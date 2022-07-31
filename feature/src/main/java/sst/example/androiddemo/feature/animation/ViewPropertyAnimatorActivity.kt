package sst.example.androiddemo.feature.animation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_view_property_animator.btn_view_property_animator
import sst.example.androiddemo.feature.R

class ViewPropertyAnimatorActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_view_property_animator)

    btn_view_property_animator.setOnClickListener {
      btn_view_property_animator.let { button ->
        button.alpha = 0.3f //设置开始alpha
        button.translationX=100f
        button.animate().let {
          it.duration = 2000
          it.translationX(200f)
          it.alpha(1f)//从当前alpha变为a
          it.start()
        }
      }
    }
  }
}
