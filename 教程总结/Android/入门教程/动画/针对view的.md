Android中的动画有哪几类，它们的特点和区别是什么        
Android中动画大致分为3类：
帧动画、 补间动画（Tween Animation）、属性动画（Property Animation）。

帧动画：通过xml配置一组图片，动态播放。很少会使用。
补间动画（Tween Animation）：大致分为旋转、透明、缩放、位移四类操作。很少会使用。
属性动画（Property Animation）：属性动画是现在使用的最多的一种动画，它比补间动画更加强大。
  属性动画大致分为两种使用类型，分别是 ViewPropertyAnimator 和 ObjectAnimator。 
  前者适合一些通用的动画，比如旋转、位移、缩放和透明，使用方式也很简单通过 View.animate() 即可得到 ViewPropertyAnimator，
   之后进行相应的动画操作即可。
 后者适合用于为我们的自定义控件添加动画，当然首先我们应该在自定义 View 中添加相应的 getXXX() 和 setXXX() 相应属性的 
    getter 和 setter 方法，这里需要注意的是在 setter 方法内改变了自定义 View 中的属性后要调用 invalidate() 来刷新View的绘制。
   之后调用 ObjectAnimator.of 属性类型()返回一个 ObjectAnimator，调用 start() 方法启动动画即可。
  

补间动画与属性动画的区别：
补间动画是父容器不断的绘制 view，看起来像移动了效果,其实 view 没有变化，还在原地。
属性动画是通过不断改变 view 内部的属性值，真正的改变 view。


属性动画
Animator类提供了关于创造动画的一系列基本的结构，是一个抽象类。
ValueAnimator是整个属性动画框架的核心类
```
ValueAnimator valueAnim = ValueAnimator.ofFloat(0f, 1f);
valueAnim.setDuration(250);
fadeAnim.start();
valueAnim.addUpdateListener(new AnimatorUpdateListener() {
    @Override
        public void onAnimationUpdate(ValueAnimator animation) {
        int frameValue = (Integer)animation.getAnimatedValue();
        //根据frameValue指定相应的透明度，位移，旋转，缩放等相应的动画
        balls.setAlpha(frameValue);

    }
});

valueAnim.addListener(new AnimatorListenerAdapter() {
    public void onAnimationEnd(Animator animation) {
        //当动画结束时移除相应对象
        balls.remove(((ObjectAnimator)animation).getTarget());
    }
});
```
ObjectAnimator
对象动画，继承ValueAnimator, 允许指定target object，并且target object需要有setter方法。
```
ObjectAnimator anim = ObjectAnimator.ofFloat(targetObject, "alpha", 0f, 1f);
anim.setDuration(1000);
anim.start();
```
AnimatorSet
动画的集合，用于组合一系列动画。
```
AnimatorSet  animatorSet = new AnimatorSet();
animatorSet.play(bounceAnim).before(squashAnim1);
animatorSet.play(squashAnim1).with(squashAnim2);
animatorSet.play(bounceBackAnim).after(stretchAnim2);
animatorSet.start();
```
PropertyValuesHolder
PropertyValuesHolder类只能多个动画一起执行
```
PropertyValuesHolder valuesHolder1 = PropertyValuesHolder.ofFloat('scaleX', 1.0f, 1.5f);
PropertyValuesHolder valuesHolder2 = PropertyValuesHolder.ofFloat('rotationX', 0.0f, 90.0f, 0.0F);
PropertyValuesHolder valuesHolder3 = PropertyValuesHolder.ofFloat('alpha', 1.0f, 0.3f, 1.0F);
ObjectAnimator objectAnimator = ObjectAnimator.ofPropertyValuesHolder(imageView,  valuesHolder1, valuesHolder2, valuesHolder3);
objectAnimator.setDuration(2000).start();
```
xml中使用属性动画
res/animator文件夹下，新建scale.xml  
```
<?xml version="1.0" encoding="utf-8"?>
<objectAnimator xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:duration="1000"
    android:propertyName="scaleX"
    android:valueFrom="1.0"
    android:valueTo="2.0"
    android:valueType="floatType"
    >
</objectAnimator>
```
程序加载
```
Animator animator=AnimatorInflater.loadAnimator(this,R.animator.scale);
animator.setTarget(view);
animator.start();
```

https://blog.51cto.com/u_15127637/4623018
Android Tween Animation ( 补间动画 ) 只需指定 动画开始 ，以及 动画结束 "关键帧"， 而动画变化的 "中间帧" 则由系统计算并补齐
在res/anim目录定义标签
alpha 渐变透明度动画效果
scale 渐变尺寸伸缩动画效果
translate 画面转换位置移动动画效果
rotate 画面转移旋转动画效果
在Java中定义
```
new AlphaAnimation(0.1f, 1.0f);
new ScaleAnimation(0.0f, 1.4f, 0.0f, 1.4f,Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
new TranslateAnimation(30.0f, -80.0f, 30.0f, 300.0f)
new RotateAnimation(0.0f, +350.0f,Animation.RELATIVE_TO_SELF,0.5f,Animation.RELATIVE_TO_SELF, 0.5f);
```
使用
```
//加载动画
Animation hyperspaceJumpAnimation =AnimationUtils.loadAnimation(this, R.anim.hyperspace_jump);
//使用ImageView显示动画
spaceshipImage.startAnimation(hyperspaceJumpAnimation);
```

https://blog.51cto.com/u_15127637/4623018
Frame Animation
在res\anim中定义
根结点是animation-list，包含多个item子节点，每个item节点定义一帧动画、当前帧的drawable资源和当前帧持续的时间
drawable 当前帧引用的drawable资源
duration 当前帧显示的时间（毫秒为单位）
oneshot 如果为true，表示动画只播放一次停止在最后一帧上，如果设置为false表示动画循环播放。
variablePadding If true, allows the drawable’s padding to change based on the current state that is selected.
visible 规定drawable的初始可见性，默认为false；
java中定义
```
AnimationDrawable
```
启动Frame Animation动画的代码rocketAnimation.start();不能在OnCreate()中，因为在OnCreate()中AnimationDrawable还没有完全的与ImageView绑定，
在OnCreate()中启动动画，就只能看到第一张图片。这里实在拖曳事件中实现的
使用
```
ImageView img = (ImageView)findViewById(R.id.wheel_image);
img.setBackgroundResource(R.drawable.frame_animation);
AnimationDrawable ad = (AnimationDrawable) img.getBackground();
ad.start();
```
属性动画原理
todo http://gityuan.com/2015/09/06/android-anaimator-4/

TimeInterpolator（时间插值器）
作用：根据时间流逝的百分比计算出当前属性值改变的百分比系统已有的插值器：
LinearInterpolator（线性插值器）：匀速动画。
AccelerateDecelerateInterpolator（加速减速插值器）：动画两头慢，中间快。
DecelerateInterpolator（减速插值器）：动画越来越慢。

TypeEvaluator（类型估值算法，即估值器）：    //todo   完善常用动画
作用：根据当前属性改变的百分比来计算改变后的属性值。
系统已有的估值器：
IntEvaluator：针对整型属性
FloatEvaluator：
针对浮点型属性
ArgbEvaluator：针对Color属性






