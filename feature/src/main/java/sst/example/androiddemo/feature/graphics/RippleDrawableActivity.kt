package sst.example.androiddemo.feature.graphics

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import sst.example.androiddemo.feature.R

class RippleDrawableActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ripple_drawable)
        //ripple标签的使用
//        https://www.jianshu.com/p/f98dfa2027de
//        1 没有边界的Ripple（Ripple With No Mask） mask按下显示
//        <ripple>标签
//        Ripple标签，即对应一个RippleDrawable，当它被设置为一个控件的background属性时，控件在按下时，即会显示水波效果
//        <ripple xmlns:android="http://schemas.android.com/apk/res/android"
//        android:color="@android:color/holo_orange_dark">
//
//        </ripple>

//        2 用颜色作为Mask的Ripple（Ripple With Color Mask），然而颜色并没有什么卵用，对于限定边界还是有用的
//        如果在一个ripple标签中，添加一个item，其id为@android:id/mask，drawable属性为引用的颜色(color) ，则水波效果会限定在drawable对应的RippleDrawable本身矩形区域内部
//        <ripple xmlns:android="http://schemas.android.com/apk/res/android"
//        android:color="#FF0000" >
//        <item android:id="@android:id/mask"
//        android:drawable="@android:color/white" />
//        </ripple>

//        3 用图片作为Mask的Ripple（Ripple With Picture Mask）
//        如果在一个ripple标签中，添加一个item，其id为@android:id/mask，drawable属性为引用的图片(png,jpg)，
//        则水波效果会限定在图片drawable中非透明部分对应的区域内部
//        <item android:id="@android:id/mask"
//        android:drawable="@drawable/icon_folder_r" />

//         4用设定形状作为Mask的Ripple（Ripple With Shape Mask）  mask按下才显示
//        如果在一个ripple标签中，添加一个item，其id为@android:id/mask，drawable属性为引用的形状(shape) ，
//        则水波效果会限定在shape对应的区域内部。
//        <item
//        android:id="@android:id/mask"
//        android:drawable="@drawable/shape"/>
          //常用的写法 显示蓝色，圆角，按下灰色的水波纹
        // <ripple xmlns:android="http://schemas.android.com/apk/res/android"
        // android:color="@android:color/darker_gray">
        // <item >
        // <shape android:shape="rectangle">
        // <gradient android:type="linear"  android:startColor="#ff206cfe" android:endColor="#ff4c88ff" android:angle="270" />
        // <corners android:radius="40dp" />
        // </shape>
        // </item>
        // </ripple>

//          5 搭配selector作为Ripple（Ripple With Selector）
//        如果在一个ripple标签中，添加一个item，在item的内部写上<selector>标签，那么这个RippleDrawable在按下的时候，
//          同时具有水波效果和selector指定的图层。
//        <item>
//        <selector>
//           <item
//           android:drawable="@drawable/icon_folder_i"
//           android:state_pressed="true">
//            </item>
//           <item
//              android:drawable="@drawable/icon_folder_r"
//              android:state_pressed="false">
//            </item>
//         </selector>
//        </item>

    }
}