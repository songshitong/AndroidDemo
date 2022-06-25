
默认情况下，View没有启用这个优化标记位，但是ViewGroup会默认启用这个优化标记位。
当我们的自定义控件继承于ViewGroup并且本身不具备绘制功能时，就可以开启这个标记位从而便于系统进行后续的优化。
当明确知道一个ViewGroup需要通过onDraw来绘制内容时，我们需要显示地关闭WILL_NOT_DRAW这个标记位。


https://www.jianshu.com/p/c84693096e41
自定义View我们大部分时候只需重写两个函数：onMeasure()、onDraw()。
onMeasure负责对当前View的尺寸进行测量，onDraw负责把当前这个View绘制出来。

当然了，你还得写至少写2个构造函数
    public MyView(Context context) {
        super(context);
    }

    public MyView(Context context, AttributeSet attrs) {
        super(context, attrs); 
    }
    
问题一    
我在xml文件中已经指定好了宽高尺寸了，我自定义View中有必要再次获取宽高并设置宽高吗？
在xml布局文件中，我们的layout_width和layout_height参数可以不用写具体的尺寸，而是wrap_content或者是match_parent。
其意思我们都知道，就是将尺寸设置为“包住内容”和“填充父布局给我们的所有空间”。这两个设置并没有指定真正的大小，可是我们绘制到屏幕上的View必须是要有具体的宽高的
没有具体的宽高就没法摆放位置了

View类给了默认的处理，但是如果View类的默认处理不满足我们的要求，我们就得重写onMeasure函数啦。
这里举个例子，比如我们希望我们的View是个正方形，如果在xml中指定宽高为`wrap_content`，如果使用View类提供的measure处理方式，
  显然无法满足我们的需求
正方形要求view的宽高是相同的而不是取决于子view


重写onMeasure
protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) 
参数中的widthMeasureSpec和heightMeasureSpec是个什么鬼？看起来很像width和height，没错，这两个参数就是包含宽和高的信息。
什么？包含？难道还要其他信息？是的！它还包含测量模式，也就是说，一个int整数，里面放了测量模式和尺寸大小。
那么一个数怎么放两个信息呢？我们知道，我们在设置宽高时有3个选择：wrap_content、match_parent以及指定固定尺寸，
而测量模式也有3种：UNSPECIFIED，EXACTLY，AT_MOST，当然，他们并不是一一对应关系哈，
但测量模式无非就是这3种情况，而如果使用二进制，我们只需要使用2个bit就可以做到，因为2个bit取值范围是[0,3]里面可以存放4个数足够我们用了。
那么Google是怎么把一个int同时放测量模式和尺寸信息呢？我们知道int型数据占用32个bit，
而google实现的是，将int数据的前面2个bit用于区分不同的布局模式，后面30个bit存放的是尺寸的数据。
那我们怎么从int数据中提取测量模式和尺寸呢？放心，不用你每次都要写一次移位<<和取且&操作，Android内置类MeasureSpec帮我们写好啦~，我们只需按照下面方法就可以拿到啦
int widthMode = MeasureSpec.getMode(widthMeasureSpec);
int widthSize = MeasureSpec.getSize(widthMeasureSpec);
测量模式	    表示意思
UNSPECIFIED	父容器没有对当前View有任何限制，当前View可以任意取尺寸   通常用于系统内部，应用开发中很少用到
EXACTLY	当前的尺寸就是当前View应该取的尺寸  match_parent或固定尺寸
AT_MOST	当前尺寸是当前View能取的最大尺寸    wrap_content

既然我们能通过widthMeasureSpec拿到宽度尺寸大小，那我们还要测量模式干嘛？测量模式会不会是多余的？请注意：
   这里的的尺寸大小并不是最终我们的View的尺寸大小，
而是父View提供的参考大小


测量模式跟我们的布局时的wrap_content、match_parent以及写成固定的尺寸有什么对应关系呢？
match_parent--->EXACTLY。怎么理解呢？match_parent就是要利用父View给我们提供的所有剩余空间，而父View剩余空间是确定的，
  也就是这个测量模式的整数里面存放的尺寸。
wrap_content--->AT_MOST。怎么理解：就是我们想要将大小设置为包裹我们的view内容，那么尺寸大小就是父View给我们作为参考的尺寸，
   只要不超过这个尺寸就可以啦，具体尺寸就根据我们的需求去设定。
固定尺寸（如100dp）--->EXACTLY。用户自己指定了尺寸大小，我们就不用再去干涉了，当然是以指定的大小为主啦。


将当前的View以正方形的形式显示，即要宽高相等，并且默认的宽高值为100像素
```
 private int getMySize(int defaultSize, int measureSpec) {
        int mySize = defaultSize;

        int mode = MeasureSpec.getMode(measureSpec);
        int size = MeasureSpec.getSize(measureSpec);

        switch (mode) {
            case MeasureSpec.UNSPECIFIED: {//如果没有指定大小，就设置为默认大小
                mySize = defaultSize;
                break;
            }
            case MeasureSpec.AT_MOST: {//如果测量模式是最大取值为size
                //我们将大小取最大值,你也可以取其他值
                mySize = size;
                break;
            }
            case MeasureSpec.EXACTLY: {//如果是固定的大小，那就不要去改变它
                mySize = size;
                break;
            }
        }
        return mySize;
}

@Override
protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = getMySize(100, widthMeasureSpec);
        int height = getMySize(100, heightMeasureSpec);

        if (width < height) {
            height = width;
        } else {
            width = height;
        }

        setMeasuredDimension(width, height);
}
```

重写onDraw
```
  @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);   
    }
```

http://liuwangshu.cn/application/view/9-custom-view.html
自定义属性
values目录下创建 attrs.xml：
```
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <declare-styleable name="RectView">
        <attr name="rect_color" format="color" />
    </declare-styleable>
</resources>
```
代码加载
```
TypedArray mTypedArray=context.obtainStyledAttributes(attrs,R.styleable.RectView);
//提取RectView属性集合的rect_color属性，如果没设置默认值为Color.RED
mColor=mTypedArray.getColor(R.styleable.RectView_rect_color,Color.RED);
//获取资源后要及时回收
mTypedArray.recycle();
```
自定义属性使用
```
<com.example.liuwangshu.mooncustomview.RectView
      xmlns:app="http://schemas.android.com/apk/res-auto"
      ...
      app:rect_color="@android:color/holo_blue_light"
      />
```
使用自定义属性需要添加schemas： xmlns:app=”http://schemas.android.com/apk/res-auto"，其中app是 我们自定义的名字，
最后我们配置新定义的app:rect_color属性为android:color/holo_blue_light


自定义组合控件  主要用于复用
1 新建view_customtitle.xml，添加多个控件和布局
2 构造器中使用 LayoutInflater.from(context).inflate(R.layout.view_customtitle, this, true);解析布局
3 findViewById修改子控件属性，然后可以使用了


常用方法  https://www.jianshu.com/p/5ec0f278e0a3
invalidate
invalidate：
view的invalidate不会导致ViewRootImpl的invalidate被调用，而是递归调用父view的invalidateChildInParent，
直到ViewRootImpl的invalidateChildInParent，然后触发performTraversals，会导致当前view被重绘,由于mLayoutRequested为false，
不会导致onMeasure和onLayout被调用，而OnDraw会被调用

requestLayout
requestLayout会直接递归调用父窗口的requestLayout，直到ViewRootImpl,然后触发performTraversals，由于mLayoutRequested为true，
会导致onMeasure和onLayout被调用。不一定会触发OnDraw。requestLayout触发onDraw可能是因为在在layout过程中发现l,t,r,b和以前不一样，
那就会触发一次invalidate，所以触发了onDraw，也可能是因为别的原因导致mDirty非空（比如在跑动画）
只要刷新的时候就调用invalidate，需要重新measure就调用requestLayout，后面再跟个invalidate（为了保证重绘）

invalidate(Rect dirty)/invalidate(int l, int t, int r, int b)  限定刷新范围，逐渐被废弃
在API 14中切换到硬件加速渲染降低了脏矩形的重要性。在API 21中，给定的矩形被完全忽略，取而代之的是内部计算的面积。因此，
鼓励client直接调用invalidate（）。

https://blog.csdn.net/weixin_30321709/article/details/94861044
获取屏幕的高度和宽度
WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
//WindowManager wm = this.getWindowManager();
int width = wm.getDefaultDisplay().getWidth();
int height = wm.getDefaultDisplay().getHeight();

从父view中移除，加入到父view的顶端，最后一个绘制
View.bringToFront

afterDescendants 在所有子view不需要焦点时获得
descendantFocusAbility  父子焦点顺序放在根布局
setDuplicateParentStateEnabled  子view是否跟随父view的焦点状态


判断view是否可见
```
private boolean isVisible(View v) {
    return v.getLocalVisibleRect(new Rect());
}
```

获取可见区域
View.getGlobalVisibleRect(Rect);
View.getLocationOnScreen(Rect);
返回值true 全部或部分可见     false全部不可见

获得当前view在屏幕中的坐标
int[] location = new int[2];
view.getLocationOnScreen(location);
这样就可以得到该视图在全局坐标系中的x，y值，（注意这个值是要从屏幕顶端算起，也就是索包括了通知栏的高度）//获取在当前屏幕内的绝对坐标
数组中location[0]代表的是x坐标，location[1]代表的是y坐标

获取View到其父控件的距离
View到其父控件（ViewGroup）的距离：
getTop()：获取View自身顶边到其父布局顶边的距离
getLeft()：获取View自身左边到其父布局左边的距离
getRight()：获取View自身右边到其父布局左边的距离
getBottom()：获取View自身底边到其父布局顶边的距离

MotionEvent提供的方法
getX()：获取点击事件距离控件左边的距离，即视图坐标
getY()：获取点击事件距离控件顶边的距离，即视图坐标
getRawX()：获取点击事件距离整个屏幕左边距离，即绝对坐标
getRawY()：获取点击事件距离整个屏幕顶边的的距离，即绝对坐标


获得当前view的高度和宽度
//获取控件的高度
int height = imageView.getMeasuredHeight();
//获取控件的宽度
int width = imageView.getMeasuredWidth();
转载于:https://www.cnblogs.com/butterfly-clover/p/3382296.html

getMeasuredWidth与 getWidth
```
  public final int getMeasuredWidth() {
      return mMeasuredWidth & MEASURED_SIZE_MASK;
  }
```
mMeasuredWidth这个值在setMeasuredDimensionRaw中被设置，而此方法会在setMeasuredDimension中被调用。
 onMeasure最后会调用此方法设置View的测量宽高

getWidth方法实际上由mRight与mLeft相减而来，而mRight与其他三点顶点的坐标在setFrame中被设置。
setFrame方法最后又在View#layout方法中间接或直接调用
```
  public final int getWidth() {
      return mRight - mLeft;
  }
```
getWidth与getMeasuredWidth大部分情况下都是相等的，也有一些特殊情况会不相等。这些特殊情况就是在measure过程完成之后，
   在父布局的onLayout或者自身的onDraw方法中调用measure方法导致


获取content：
ViewGroup content = (ViewGroup)findViewById(android.R.id.content);
DecorView获取 Activity.getWindow().getDecorView()
获取设置的View：
content.getChildAt(0);

给View添加额外信息，存储一些View的数据
View.setTag()

ViewGroup.getChildCount 获取显示的View的数量
ViewGroup.getChildAt    获取第index的view
ViewGroup.indexOfChild  获取view的index
RecyclerView查找
RecyclerView.getLayoutManager.findViewByPosition
    findFirstVisibleItemPosition 第一个显示的index
    findFirstCompletelyVisibleItemPosition  第一个完全露出来的view   同理，可以查找最后一个显示的

坐标系转换
ViewGroup.offsetDescendantRectToMyCoords
 
setWillNotDraw的作用  ViewGroup需要开启后才能进行绘制
```
// 如果一个View不需要绘制任何内容，那么设置这个标记位为true以后，
// 系统会进行相应的优化。
public void setWillNotDraw(boolean willNotDraw) {
    setFlags(willNotDraw ? WILL_NOT_DRAW : 0, DRAW_MASK);
}
```
默认情况下，View没有启用这个优化标记位，但是ViewGroup会默认启用这个优化标记位。
当我们的自定义控件继承于ViewGroup并且本身不具备绘制功能时，就可以开启这个标记位从而便于系统进行后续的优化。
当明确知道一个ViewGroup需要通过onDraw来绘制内容时，我们需要显示地关闭WILL_NOT_DRAW这个标记位。


在Activity中获取某个View的宽高
由于View的measure过程和Activity的生命周期方法不是同步执行的，如果View还没有测量完毕，那么获得的宽/高就是0。
所以在onCreate、onStart、onResume中均无法正确得到某个View的宽高信息。解决方式如下：
Activity/View#onWindowFocusChanged
```
// 此时View已经初始化完毕
// 当Activity的窗口得到焦点和失去焦点时均会被调用一次
// 如果频繁地进行onResume和onPause，那么onWindowFocusChanged也会被频繁地调用
public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    if (hasFocus) {
        int width = view.getMeasureWidth();
        int height = view.getMeasuredHeight();
    }
}
```
view.post(runnable)
```
// 通过post可以将一个runnable投递到消息队列的尾部，// 然后等待Looper调用次runnable的时候，View也已经初
// 始化好了
protected void onStart() {
    super.onStart();
    view.post(new Runnable() {
        @Override
        public void run() {
            int width = view.getMeasuredWidth();
            int height = view.getMeasuredHeight();
        }
    });
}
```
ViewTreeObserver
```
// 当View树的状态发生改变或者View树内部的View的可见// 性发生改变时，onGlobalLayout方法将被回调
protected void onStart() {
    super.onStart();

    ViewTreeObserver observer = view.getViewTreeObserver();
    observer.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

        @SuppressWarnings("deprecation")
        @Override
        public void onGlobalLayout() {
            view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            int width = view.getMeasuredWidth();
            int height = view.getMeasuredHeight();
        }
    });
}
```
View.measure(int widthMeasureSpec, int heightMeasureSpec)



对getSuggestMinimumWidth的分析
```
protected int getSuggestedMinimumWidth() {
    return (mBackground == null) ? mMinWidth : max(mMinWidth, mBackground.getMinmumWidth());
}

protected int getSuggestedMinimumHeight() {
    return (mBackground == null) ? mMinHeight : max(mMinHeight, mBackground.getMinimumHeight());
}

public int getMinimumWidth() {
    final int intrinsicWidth = getIntrinsicWidth();
    return intrinsicWidth > 0 ? intrinsicWidth : 0;
}
```
如果View没有设置背景，那么返回android:minWidth这个属性所指定的值，这个值可以为0；如果View设置了背景，
  则返回android:minWidth和背景的最小宽度这两者中的最大值


ViewGroup.getChildDrawingOrder
```
protected int getChildDrawingOrder(int childCount, int drawingPosition) {
        return drawingPosition;
    }
```
ViewGroup允许子view自定义绘制顺序，可以解决GridView,RecyclerView中前不遮挡后遮挡的问题
调用顺序ViewGroup.dispatchDraw->buildOrderedChildList->getAndVerifyPreorderedIndex->getChildDrawingOrder


