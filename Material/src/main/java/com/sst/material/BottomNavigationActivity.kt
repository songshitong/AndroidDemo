package com.sst.material

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class BottomNavigationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bottom_navigation)
        ///labelVisibilityMode
//        labeled：所有按钮中都显示图标与标签。
//        unlabeled：所有按钮中都不显示标签，只显示图标。
//        selected：只有选中的按钮才显示标签与图标，其他未选中的只显示图标。
//        auto：自动模式，该模式使用item数来确定是否显示或隐藏标签。当按钮个数小于等于3个时使用labeled模式，大于3个时使用selected模式。

    }
}