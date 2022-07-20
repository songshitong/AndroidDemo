package sst.example.androiddemo.feature.animation.dynamicanimation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import kotlinx.android.synthetic.main.activity_dynamic_animaiton.*
import sst.example.androiddemo.feature.R

class DynamicAnimaitonActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dynamic_animaiton)

        startSpringBtn.setOnClickListener {
            springTextView.translationY=600f
            val sp = SpringAnimation(springTextView,DynamicAnimation.TRANSLATION_Y,590f).setStartVelocity(5000f)

            sp.start()
        }

    }
}
