
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
这里举个例子，比如我们希望我们的View是个正方形，如果在xml中指定宽高为`wrap_content`，如果使用View类提供的measure处理方式，显然无法满足我们的需求
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
测量模式	表示意思
UNSPECIFIED	父容器没有对当前View有任何限制，当前View可以任意取尺寸
EXACTLY	当前的尺寸就是当前View应该取的尺寸
AT_MOST	当前尺寸是当前View能取的最大尺寸

既然我们能通过widthMeasureSpec拿到宽度尺寸大小，那我们还要测量模式干嘛？测量模式会不会是多余的？请注意：这里的的尺寸大小并不是最终我们的View的尺寸大小，
而是父View提供的参考大小


测量模式跟我们的布局时的wrap_content、match_parent以及写成固定的尺寸有什么对应关系呢？
match_parent--->EXACTLY。怎么理解呢？match_parent就是要利用父View给我们提供的所有剩余空间，而父View剩余空间是确定的，也就是这个测量模式的整数里面存放的尺寸。
wrap_content--->AT_MOST。怎么理解：就是我们想要将大小设置为包裹我们的view内容，那么尺寸大小就是父View给我们作为参考的尺寸，只要不超过这个尺寸就可以啦，具体尺寸就根据我们的需求去设定。
固定尺寸（如100dp）--->EXACTLY。用户自己指定了尺寸大小，我们就不用再去干涉了，当然是以指定的大小为主啦。


将当前的View以正方形的形式显示，即要宽高相等，并且默认的宽高值为100像素
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



重写onDraw
  @Override
    protected void onDraw(Canvas canvas) {
        //调用父View的onDraw函数，因为View这个类帮我们实现了一些
        // 基本的而绘制功能，比如绘制背景颜色、背景图片等
        super.onDraw(canvas);
        int r = getMeasuredWidth() / 2;//也可以是getMeasuredHeight()/2,本例中我们已经将宽高设置相等了
        //圆心的横坐标为当前的View的左边起始位置+半径
        int centerX = getLeft() + r;
        //圆心的纵坐标为当前的View的顶部起始位置+半径
        int centerY = getTop() + r;

        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        //开始绘制
        canvas.drawCircle(centerX, centerY, r, paint);

    }

