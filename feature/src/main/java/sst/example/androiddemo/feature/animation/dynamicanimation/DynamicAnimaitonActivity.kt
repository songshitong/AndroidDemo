package sst.example.androiddemo.feature.animation.dynamicanimation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import sst.example.androiddemo.feature.R

class DynamicAnimaitonActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dynamic_animaiton)
        val springTextView = findViewById<View>(R.id.springTextView)
        findViewById<View>(R.id.startSpringBtn).setOnClickListener {
            springTextView.translationY=600f
            val sp = SpringAnimation(springTextView,DynamicAnimation.TRANSLATION_Y,590f).setStartVelocity(5000f)

            sp.start()
        }

    }
}
