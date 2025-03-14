package sst.example.androiddemo.feature.animation

import android.animation.LayoutTransition
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import sst.example.androiddemo.feature.R

class LayoutAnimationActivity : AppCompatActivity() {
    var count = 0
    @SuppressLint("ObjectAnimatorBinding")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_layout_animation)
//      xml的layout  animateLayoutChanges 开启Android自带layout动画
        findViewById<View>(R.id.layoutAdd).setOnClickListener {
            count++
            val tv = TextView(this)
            tv.setText("layout " + count)
            findViewById<ViewGroup>(R.id.layoutAnimationRoot).addView(tv, 0)
        }
        findViewById<View>(R.id.layoutRemove).setOnClickListener {
            if (findViewById<ViewGroup>(R.id.layoutAnimationRoot).childCount == 0) return@setOnClickListener
            findViewById<ViewGroup>(R.id.layoutAnimationRoot).removeViewAt(0)
        }
          findViewById<View>(R.id.groupBtn).setOnClickListener {
            val intent = Intent(this, ViewGroupActivity::class.java)
            startActivity(intent)
        }

        //自定义  LayoutTransition
//        LayoutTransition.APPEARING	子View添加到容器中时的过渡动画效果。
//        LayoutTransition.CHANGE_APPEARING	子View添加到容器中时，其他子View位置改变的过渡动画
//        LayoutTransition.DISAPPEARING	子View从容器中移除时的过渡动画效果。
//        LayoutTransition.CHANGE_DISAPPEARING	子View从容器中移除时，其它子view位置改变的过渡动画
//        LayoutTransition.CHANGING	子View在容器中位置改变时的过渡动画，不涉及删除或者添加操作
        val lt = LayoutTransition()
        val addAnimator =  ObjectAnimator.ofFloat(null, "translationX",  190f,0f).setDuration(1000)

        val addOtherAnimator =  ObjectAnimator.ofFloat(null, "translationY",  0f,30f).setDuration(1000)
        lt.setAnimator(LayoutTransition.APPEARING,addAnimator)
        lt.setAnimator(LayoutTransition.CHANGE_APPEARING,addOtherAnimator)

        lt.setAnimator(LayoutTransition.DISAPPEARING,addAnimator)
        findViewById<ViewGroup>(R.id.layoutAnimationRoot).layoutTransition = lt
    }
}
