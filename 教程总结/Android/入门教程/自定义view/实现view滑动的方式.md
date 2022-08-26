http://liuwangshu.cn/application/view/2-sliding.html

1.layout()
```
case MotionEvent.ACTION_MOVE:
    //计算移动的距离
    int offsetX = x - lastX;
    int offsetY = y - lastY;
    //调用layout方法来重新放置它的位置
    layout(getLeft()+offsetX, getTop()+offsetY,
            getRight()+offsetX , getBottom()+offsetY);
    break;
```
2. offsetLeftAndRight()与offsetTopAndBottom()
```
case MotionEvent.ACTION_MOVE:
    //计算移动的距离
    int offsetX = x - lastX;
    int offsetY = y - lastY;
    //对left和right进行偏移
    offsetLeftAndRight(offsetX);
    //对top和bottom进行偏移
    offsetTopAndBottom(offsetY);
    break;
```

3.LayoutParams（改变布局参数）
```
LinearLayout.LayoutParams layoutParams= (LinearLayout.LayoutParams) getLayoutParams();
              layoutParams.leftMargin = getLeft() + offsetX;
              layoutParams.topMargin = getTop() + offsetY;
              setLayoutParams(layoutParams);
```

4.动画
```
ObjectAnimator.ofFloat(mCustomView,"translationX",0,300).setDuration(1000).start();
```

5.scollTo与scollBy   滚动与设置的值是反的
scollTo(x,y)表示移动到一个具体的坐标点，而scollBy(dx,dy)则表示移动的增量为dx、dy。其中scollBy最终也是要调用scollTo的。
scollTo、scollBy移动的是View的内容，如果在ViewGroup中使用则是移动他所有的子View。
```
((View)getParent()).scrollBy(-offsetX,-offsetY);
```
这里要实现CustomView随着我们手指移动的效果的话，我们就需要将偏移量设置为负值

6.Scroller
我们用scollTo/scollBy方法来进行滑动时，这个过程是瞬间完成的，所以用户体验不大好。这里我们可以使用Scroller来实现有过度效果的滑动，
这个过程不是瞬间完成的，而是在一定的时间间隔完成的。Scroller本身是不能实现View的滑动的，它需要配合View的computeScroll()方法才能弹性滑动的效果。
在这里我们实现CustomView平滑的向右移动。
首先我们要初始化Scroller：
```
public CustomView(Context context, AttributeSet attrs) {
      super(context, attrs);
      mScroller = new Scroller(context);
  }
```
接下来重写computeScroll()方法，系统会在绘制View的时候在draw()方法中调用该方法，这个方法中我们调用父类的scrollTo()方法
并通过Scroller来不断获取当前的滚动值，每滑动一小段距离我们就调用invalidate()方法不断的进行重绘，重绘就会调用computeScroll()方法，
这样我们就通过不断的移动一个小的距离并连贯起来就实现了平滑移动的效果：
```
@Override
public void computeScroll() {
    super.computeScroll();
    if(mScroller.computeScrollOffset()){
        ((View) getParent()).scrollTo(mScroller.getCurrX(),mScroller.getCurrY());
         //通过不断的重绘不断的调用computeScroll方法
         invalidate();
    }  
}
```
调用Scroller.startScroll()方法。我们在CustomView中写一个smoothScrollTo()方法，调用Scroller.startScroll()方法，
在2000毫秒内沿X轴平移delta像素
最后我们在ViewSlideActivity.java中调用CustomView的smoothScrollTo()方法：
```
//使用Scroll来进行平滑移动   这里我们是设定CustomView沿着X轴向右平移400像素
mCustomView.smoothScrollTo(-400,0);
```