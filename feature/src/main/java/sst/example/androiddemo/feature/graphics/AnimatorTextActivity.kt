package sst.example.androiddemo.feature.graphics

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import sst.example.androiddemo.feature.R
import sst.example.androiddemo.feature.widget.practice.textview.AnimatorText
import sst.example.androiddemo.feature.widget.practice.textview.RotateText

class AnimatorTextActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_animator_text)
    findViewById<AnimatorText>(R.id.animator_text_rotate).let {animText->
      animText.addTextList(arrayListOf(RotateText("比卡丘！！！")))
      animText.setOnClickListener {
        animText.startAnim()
      }
    }
  }
}