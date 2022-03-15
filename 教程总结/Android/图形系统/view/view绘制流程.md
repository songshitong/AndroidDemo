https://juejin.cn/post/6844904042175397902#heading-12
https://jsonchao.github.io/2018/10/28/Android%20View%E7%9A%84%E7%BB%98%E5%88%B6%E6%B5%81%E7%A8%8B/
https://juejin.cn/post/6844904136937324552

总结
视图绘制的起点在 ViewRootImpl 类的 performTraversals()方法，
   在这个方法内其实是按照顺序依次调用了 mView.measure()、mView.layout()、mView.draw()
View的绘制流程分为3步：测量、布局、绘制，分别对应3个方法 measure、layout、draw。
测量阶段。view.measure->view.onMeasure 在measure 方法中做一些优化和准备工作后会调用 onMeasure 方法进行实际的自我测量。
  onMeasure方法在View和ViewGroup做的事情是不一样的：
  View。 View 中的 onMeasure 方法会计算自己的尺寸并通过 setMeasureDimension 保存。
  ViewGroup。 ViewGroup 中的 onMeasure 方法会调用所有子view的measure 方法进行自我测量并保存。然后通过子View的尺寸和位置计算出自己的尺寸并保存。

布局阶段。view.layout->View.onLayout layout 方法会保存父 View 传进来的尺寸和位置，并调用 onLayout 进行实际的内部布局。
 onLayout 在 View 和 ViewGroup 中做的事情也是不一样的：
  View。 因为 View 是没有子 View 的，所以View的onLayout里面什么都不做。  
  ViewGroup。 ViewGroup 中的 onLayout 方法会调用所有子 View 的 layout 方法，把尺寸和位置传给他们，让他们完成自我的内部布局。

绘制阶段。draw 方法会做一些调度工作，然后会调用 onDraw 方法进行 View 的自我绘制。
draw 方法的调度流程大致是这样的：
绘制背景。对应 drawBackground(Canvas)方法。
绘制主体。对应 onDraw(Canvas)方法。
绘制子View。 对应 dispatchDraw(Canvas)方法。     ViewGroup重写了该方法，调用drawChild->child.draw
绘制滑动相关和前景。 对应 onDrawForeground(Canvas)      



绘制会从根视图ViewRoot的performTraversals()方法开始，从上到下遍历整个视图树，每个View控件负责绘制自己，
 而ViewGroup还需要负责通知自己的子View进行绘制操作。performTraversals()的核心代码如下
```
private void performTraversals() {
    ...
    int childWidthMeasureSpec = getRootMeasureSpec(mWidth, lp.width);
    int childHeightMeasureSpec = getRootMeasureSpec(mHeight, lp.height);
    ...
    //执行测量流程
    performMeasure(childWidthMeasureSpec, childHeightMeasureSpec);
    ...
    //执行布局流程
    performLayout(lp, desiredWindowWidth, desiredWindowHeight);
    ...
    //执行绘制流程
    performDraw();
}
```

View绘制流程之Measure
1.Measure的基本流程
由前面的分析可知，页面的测量流程是从performMeasure方法开始的，相关的核心代码流程如下
frameworks/base/core/java/android/view/ViewRootImpl.java
```
private void performMeasure(int childWidthMeasureSpec, int childHeightMeasureSpec) {
    ...
    // 具体的测量操作分发给ViewGroup 
    mView.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    ...
}
//ViewGroup 提供了几个通用的测量下层 View 相关的方法：getChildMeasureSpec，measureChild，measureChildWithMargins，measureChildren
// 自定义ViewGroup可以手动调用
// 在ViewGroup中的measureChildren()方法中遍历测量ViewGroup中所有的View
protected void measureChildren(int widthMeasureSpec, int heightMeasureSpec) {
    final int size = mChildrenCount;
    final View[] children = mChildren;
    for (int i = 0; i < size; ++i) {
        final View child = children[i];
        // 当View的可见性处于GONE状态时，不对其进行测量
        if ((child.mViewFlags & VISIBILITY_MASK) != GONE) {
            measureChild(child, widthMeasureSpec, heightMeasureSpec);
        }
    }
}

// 测量某个指定的View
protected void measureChild(View child, int parentWidthMeasureSpec, int parentHeightMeasureSpec) {
    final LayoutParams lp = child.getLayoutParams();

    // 根据父容器的MeasureSpec和子View的LayoutParams等信息计算
    // 子View的MeasureSpec
    final int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec, mPaddingLeft + mPaddingRight, lp.width);
    final int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec, mPaddingTop + mPaddingBottom, lp.height);
    child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
}

// View的measure方法
public final void measure(int widthMeasureSpec, int heightMeasureSpec) {
    ...
    // ViewGroup没有定义测量的具体过程，因为ViewGroup是一个
    // 抽象类，其测量过程的onMeasure方法需要各个子类去实现
    onMeasure(widthMeasureSpec, heightMeasureSpec);
    ...
}

// 不同的ViewGroup子类有不同的布局特性，这导致它们的测量细节各不相同，如果需要自定义测量过程，则子类可以重写这个方法
protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    // setMeasureDimension方法用于设置View的测量宽高
    setMeasureDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec), 
    getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec));
}

// 如果View没有重写onMeasure方法，则会默认调用getDefaultSize来获得View的宽高
public static int getDefaultSize(int size, int measureSpec) {
    int result = size;
    int specMode = MeasureSpec.getMode(measureSpec);
    int specSize = MeasureSpec.getSize(measureSpec);
    switch (specMode) {
        case MeasureSpec.UNSPECIFIED:
            result = size;
            break;
        case MeasureSpec.AT_MOST:
        case MeasureSpec.EXACTLY:
            result = sepcSize;
            break;
    }
    return result;
}
```
todo  DecorView的MeasureSpec的创建过程   .LinearLayout的onMeasure方法实现解析   LinearLayout的onLayout方法实现解析



View的绘制流程之Layout
1.Layout的基本流程
```
// ViewRootImpl.java
private void performLayout(WindowManager.LayoutParams lp, int desiredWindowWidth, int desiredWindowHeight) {
    ...
    host.layout(0, 0, host.getMeasuredWidth(), host.getMeasuredHeight());
    ...
}

// View.java
public void layout(int l, int t, int r, int b) {
    ...
    // 通过setFrame方法来设定View的四个顶点的位置，即View在父容器中的位置
    boolean changed = isLayoutModeOptical(mParent) ? 
    set OpticalFrame(l, t, r, b) : setFrame(l, t, r, b);

    ...
    onLayout(changed, l, t, r, b);
    ...
}

// 空方法，子类如果是ViewGroup类型，则重写这个方法，实现ViewGroup中所有View控件布局流程  需要ViewGroup自己实现
protected void onLayout(boolean changed, int left, int top, int right, int bottom) {

}
```



View的绘制流程之Draw
1.Draw的基本流程
```
private void performDraw() {
    ...
    draw(fullRefrawNeeded);
    ...
}

private void draw(boolean fullRedrawNeeded) {
    ...
    if (!drawSoftware(surface, mAttachInfo, xOffest, yOffset, 
    scalingRequired, dirty)) {
        return;
    }
    ...
}

private boolean drawSoftware(Surface surface, AttachInfo attachInfo, 
int xoff, int yoff, boolean scallingRequired, Rect dirty) {
    ...
    mView.draw(canvas);
    ...
}

// 绘制基本上可以分为六个步骤
public void draw(Canvas canvas) {
    ...
    // 步骤一：绘制View的背景
    drawBackground(canvas);

    ...
    // 步骤二：如果需要的话，保持canvas的图层，为fading做准备
    saveCount = canvas.getSaveCount();
    ...
    canvas.saveLayer(left, top, right, top + length, null, flags);

    ...
    // 步骤三：绘制View的内容
    onDraw(canvas);

    ...
    // 步骤四：绘制View的子View   viewGroup已经帮做了
    dispatchDraw(canvas);

    ...
    // 步骤五：如果需要的话，绘制View的fading边缘并恢复图层
    canvas.drawRect(left, top, right, top + length, p);
    ...
    canvas.restoreToCount(saveCount);

    ...
    // 步骤六：绘制View的装饰(例如滚动条等等)
    onDrawForeground(canvas)
}
```

