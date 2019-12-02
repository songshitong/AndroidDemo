package sst.example.androiddemo.feature.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import sst.example.androiddemo.feature.R

//https://mobile.51cto.com/abased-375428.html

// 第一个布局 先把3个EditText按wrap_content分配高度，(为什么是3个，第二个不分配无法确定第三个的位置)，然后把剩余的空间全部给EditText2

// 第二个布局，先把3个TextView按wrap_content分配宽度，把剩下空间按1：2：3分给TextView

// 第三个布局，先把3个TextView按match_parent分配宽度，每一个都填满父控件，剩余宽度为1parentWidth-parentWidth*3 = -2parentWidth
//   第一个TextView宽度 parentWidth+(-2parentWidth)*1/(1+2+2) = 3/5*parentWidth,第二个和第三个为1/5*parentWidth
//   三个TextView按3:1:1显示

// 第四个布局  宽度为match_parent，权重为1：2：3   第一个宽度为2/3*parentWidth，第二个宽度1/3*parentWidth ,第三个宽度为0
    //三个TextView的按2：1：0显示
class LayoutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_layout)
    }
}
