package sst.example.androiddemo.feature.widget.layout.repeatMeasure

import android.app.Activity
import android.os.Bundle
import android.view.View
import sst.example.androiddemo.feature.R

class MeasureTestActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_measure_test)
        findViewById<View>(R.id.measureLinear1).tag="measure Linear 1"
          findViewById<View>(R.id.measureLinear2).tag="measure Linear 2"
          findViewById<View>(R.id.measureLinear3).tag="measure Linear 3"
          findViewById<View>(R.id.measureView1).tag="measure View 1"
          findViewById<View>(R.id.measureView2).tag="measure View 2"
          findViewById<View>(R.id.measureView0).tag="measure View 0"
    }
}

//都测量两次
//D/measure View 1: onMeasure
//D/measure View 2: onMeasure
//D/measure Linear 1: onMeasure
//D/measure View 1: onMeasure
//D/measure View 2: onMeasure
//D/measure Linear 1: onMeasure
//D/measure View 1: onLayout
//D/measure View 2: onLayout
//D/measure Linear 1: onLayout

//0设为match_parent 0测量4次 其他测量2次
//D/measure View 0: onMeasure
//D/measure View 1: onMeasure
//D/measure View 2: onMeasure
//D/measure View 0: onMeasure
//D/measure Linear 1: onMeasure
//D/measure View 0: onMeasure
//D/measure View 1: onMeasure
//D/measure View 2: onMeasure
//D/measure View 0: onMeasure
//D/measure Linear 1: onMeasure
//D/measure View 0: onLayout
//D/measure View 1: onLayout
//D/measure View 2: onLayout
//D/measure Linear 1: onLayout

//都设为match_parent  都测量4次
//D/measure View 0: onMeasure
//D/measure View 1: onMeasure
//D/measure View 2: onMeasure
//D/measure View 0: onMeasure
//D/measure View 1: onMeasure
//D/measure View 2: onMeasure
//D/measure Linear 1: onMeasure
//D/measure View 0: onMeasure
//D/measure View 1: onMeasure
//D/measure View 2: onMeasure
//D/measure View 0: onMeasure
//D/measure View 1: onMeasure
//D/measure View 2: onMeasure
//D/measure Linear 1: onMeasure
//D/measure View 0: onLayout
//D/measure View 1: onLayout
//D/measure View 2: onLayout
//D/measure Linear 1: onLayout


// 两层LinearLayout  0设为match_parent测量了6次  其他测量4次    注意多层LinearLayout，子的都是match_parent
//D/measure View 0: onMeasure
//D/measure View 1: onMeasure
//D/measure View 2: onMeasure
//D/measure View 0: onMeasure
//D/measure Linear 2: onMeasure
//D/measure View 0: onMeasure
//D/measure View 1: onMeasure
//D/measure View 2: onMeasure
//D/measure Linear 2: onMeasure
//D/measure Linear 1: onMeasure
//D/measure View 0: onMeasure
//D/measure View 1: onMeasure
//D/measure View 2: onMeasure
//D/measure View 0: onMeasure
//D/measure Linear 2: onMeasure
//D/measure View 0: onMeasure
//D/measure View 1: onMeasure
//D/measure View 2: onMeasure
//D/measure Linear 2: onMeasure
//D/measure Linear 1: onMeasure
//D/measure View 0: onLayout
//D/measure View 1: onLayout
//D/measure View 2: onLayout
//D/measure Linear 2: onLayout
//D/measure Linear 1: onLayout

//3层LinearLayout  0设为match_parent 测量9次  其他测量7次
//D/measure View 0: onMeasure
//D/measure View 1: onMeasure
//D/measure View 2: onMeasure
//D/measure View 0: onMeasure
//D/measure Linear 3: onMeasure
//D/measure View 0: onMeasure
//D/measure View 1: onMeasure
//D/measure View 2: onMeasure
//D/measure Linear 3: onMeasure
//D/measure Linear 2: onMeasure
//D/measure View 0: onMeasure
//D/measure View 1: onMeasure
//D/measure View 2: onMeasure
//D/measure Linear 3: onMeasure
//D/measure Linear 2: onMeasure
//D/measure Linear 1: onMeasure
//D/measure View 0: onMeasure
//D/measure View 1: onMeasure
//D/measure View 2: onMeasure
//D/measure View 0: onMeasure
//D/measure Linear 3: onMeasure
//D/measure View 0: onMeasure
//D/measure View 1: onMeasure
//D/measure View 2: onMeasure
//D/measure Linear 3: onMeasure
//D/measure Linear 2: onMeasure
//D/measure View 0: onMeasure
//D/measure View 1: onMeasure
//D/measure View 2: onMeasure
//D/measure Linear 3: onMeasure
//D/measure Linear 2: onMeasure
//D/measure Linear 1: onMeasure
//D/measure View 0: onLayout
//D/measure View 1: onLayout
//D/measure View 2: onLayout
//D/measure Linear 3: onLayout
//D/measure Linear 2: onLayout
//D/measure Linear 1: onLayout
