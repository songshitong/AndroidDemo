package sst.example.androiddemo.feature.graphics

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import sst.example.androiddemo.feature.R
import sst.example.androiddemo.feature.widget.practice.textview.AnimatorText
import sst.example.androiddemo.feature.widget.practice.textview.ColorizeText
import sst.example.androiddemo.feature.widget.practice.textview.LiquidFillText
import sst.example.androiddemo.feature.widget.practice.textview.RotateText
import sst.example.androiddemo.feature.widget.practice.textview.TyperText
import sst.example.androiddemo.feature.widget.practice.textview.WavyText

class AnimatorTextActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_animator_text)
    findViewById<AnimatorText>(R.id.animator_text_rotate).let { animText ->
      animText.addTextList(
        arrayListOf(
          RotateText("比卡丘！！！"),
          RotateText("~~~哔~~~兹~~~"),
          RotateText("~~~噼里啪啦~~~", rotateOut = false)
        )
      )
      animText.setOnClickListener {
        animText.startAnim()
      }
    }

    findViewById<AnimatorText>(R.id.animator_text_typer).let { animText->
      animText.addTextList(
        arrayListOf(
          TyperText("It is not enough to do your best,"),
          TyperText("you must know what to do,"),
          TyperText("and then do your best"),
          TyperText("- W.Edwards Deming"),
        )
      )
      animText.setOnClickListener {
        animText.startAnim()
      }
    }

    findViewById<AnimatorText>(R.id.animator_text_colorize).let {animText ->
       animText.addTextList(
         arrayListOf(
           ColorizeText("Larry Page"),
           ColorizeText("Bill Gates"),
           ColorizeText("Steve Jobs"),
         )
       )
       animText.setOnClickListener {
         animText.startAnim()
       }
    }

    findViewById<AnimatorText>(R.id.animator_text_liquidfill).let {animText ->
       animText.addTextList(
         arrayListOf(
           LiquidFillText("LIQUIDY")
         )
       )
       animText.setOnClickListener {
         animText.startAnim()
       }
    }

    findViewById<AnimatorText>(R.id.animator_text_wavy).let { animText->
      animText.addTextList(
        arrayListOf(
          WavyText("Hello World"),
          WavyText("Look at the waves")
        )
      )
      animText.setOnClickListener {
        animText.startAnim()
      }
    }
  }
}