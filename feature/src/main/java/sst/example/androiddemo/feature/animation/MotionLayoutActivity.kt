package sst.example.androiddemo.feature.animation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.constraintlayout.motion.widget.MotionLayout
import kotlinx.android.synthetic.main.activity_motion_layout.motionLayoutFold
import sst.example.androiddemo.feature.R

// https://developer.android.com/training/constraint-layout/motionlayout?hl=zh-cn
// http://echo.vars.me/android/motionlayout/#recyclerview-%E5%AE%9E%E7%8E%B0%E5%8F%AA%E5%B1%95%E5%BC%80%E4%B8%80%E9%A1%B9
//motionLayout简介
//MotionLayout 是 ConstraintLayout 的子类  motionLayout动画不能改变自身，可以改变子view
//motionLayout将布局和动画分离，布局中直接替换ConstraintLayout，通过app:layoutDescription使用MotionScene(动画文件)
// MotionLayout 的所有直接子 View 都需赋予一个 id，否则会报 All children of ConstraintLayout must have ids to use ConstraintSet 错误
//
// motionLayout的属性
// app:applyMotionScene="boolean" 表示是否应用 MotionScene。此属性的默认值为 true
// app:showPaths="boolean" 表示在运动进行时是否显示运动路径。此属性的默认值为 false
// app:progress="float" 可让您明确指定转换进度。您可以使用从 0（转换开始）到 1（转换结束）之间的任意浮点值
// app:currentState="reference" 可让您指定具体的 ConstraintSet
// app:motionDebug 可让您显示与运动有关的其他调试信息。可能的值为“SHOW_PROGRESS”、“SHOW_PATH”或“SHOW_ALL”
//
// MotionScene 分为三个部分：StateSet、ConstraintSet 和 Transition。
// StateSet 用于描述状态，是可选的。
// ConstraintSet 用于定义一个场景的约束集，用来描述限制的位置。 定义了动画的不同状态或关键帧
//    Constraint，描述这一场景下view的状态，id指向对应的view
//   constraint支持位置，边界，alpha，visibility，elevation，rotation、rotationX、rotationY，translationX、translationY、translationZ，scaleX、scaleY
// Constraint可以自定义属性
// <CustomAttribute
// motion:attributeName="backgroundColor"
// motion:customColorValue="#D81B60"/>
// Transition 用于描述两个状态或者 ConstraintSet 间的变换,包括动画的触发
//   触发方式  OnClick/OnSwipe  targetId触发的view，clickAction执行的动作(toggle 开始和结束状态循环切换等) 下面的标签
       //OnSwipe touchAnchorId 在滑动之后移动的视图  touchAnchorSide 滑动所固定到的目标视图的一侧
       //dragDirection 用户滑动动作的方向  maxAcceleration 目标视图的最大加速度
       //dragScale 控制视图相对于滑动长度的移动距离
//   constraintSetStart/constraintSetEnd  开始结束的场景
//   motionInterpolator 插值器
//   app:layoutDuringTransition="" 动画中是否进行布局
// transition使用keyframe   <Transition><KeyFrameSet><KeyPosition>..    效果：开始约束，哥哥关键帧节点，结束约束
// 位置关键帧 Position keyframe : KeyPosition   keyPositionType 相对于父视图/相对于视图在整个运动序列过程中移动的距离
// 属性关键帧 Attribute keyframe : KeyAttribute
// 循环关键帧 Cycle keyframe : KeyCycle
// 周期关键整 TimeCycle keyframe : KeyTimeCycle
//关键帧属性
// 节点 motion:framePosition : 关键帧在过渡中（从0到100）的作用时机
// 目标 motion:target : 哪个对象受该关键帧影响
// 插值器 motion:transitionEasing : 使用哪种插值器（默认为线性）
// 曲线拟合motion:curveFit : 样条（默认）或线形——使用哪个曲线拟合关键帧。默认情况下是单调样条曲线，这使得过渡更加平滑，当然你也可以决定使用线性 (linear) 拟合。


//motionLayout在recyclerview的item中使用，不要设置motionScene的onclick，触发多个，最好在代码中控制


//应用示例 折叠动画  视差动画(对不同的元素设置Constraint即可)
class MotionLayoutActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_motion_layout)
    motionLayoutFold.setTransitionListener(object :MotionLayout.TransitionListener{
      override fun onTransitionStarted(p0: MotionLayout?, p1: Int, p2: Int) {
        Log.i("MotionLayoutActivity","onTransitionStarted")
      }

      override fun onTransitionChange(p0: MotionLayout?, p1: Int, p2: Int, p3: Float) {
        Log.i("MotionLayoutActivity","onTransitionChange")
      }

      override fun onTransitionCompleted(p0: MotionLayout?, p1: Int) {
        Log.i("MotionLayoutActivity","onTransitionCompleted")
      }

      override fun onTransitionTrigger(p0: MotionLayout?, p1: Int, p2: Boolean, p3: Float) {
        Log.i("MotionLayoutActivity","onTransitionTrigger")
      }
    })
    // 代码控制动画
    // motionLayoutFold.setTransition()  motionScene中的transition
    // motionLayoutFold.transitionToEnd()
    // motionLayout.setProgress(1); //可与viewpager等联动,设置动画在不同的阶段0是开始，1是结束
    // motionLayoutFold.currentState 执行到constraintSetEnd还是start
    //代码控制constraintSet
//    val startConstraintSet = motionLayout.getConstraintSet(R.id.scene_start)
//    val startConstraint = startConstraintSet.getConstraint(R.id.container)
//    startConstraint.layout.mHeight = foldHeight 控制不同场景的约束
  // constraint.propertySet.alpha = 0.1f


    //recyclerview中使用motionLayout
//    trigger.setOnClickListener {
//      data.getOrNull(holder.adapterPosition)?.let {
//        if (!it.expand) { //标记数据状态，开始动画
//          it.expand = true
//          motionLayout.transitionToEnd()
//        } else {
//          motionLayout.transitionToStart()
//        }
//      }
//    }
    //由于recyclerview的复用机制，每次onBindViewHolder都需要设置状态
//    if (itemData.expand) {
//      if (motionLayout.progress != 1.0f) {
//        notifyItem(itemData)
//      }
//      motionLayout.progress = 1.0f
//    } else {
//      if (motionLayout.progress != 0f) {
//        notifyItem(itemData)
//      }
//      motionLayout.progress = 0f
//    }

  }
}