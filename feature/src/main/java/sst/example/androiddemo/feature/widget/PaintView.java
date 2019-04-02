package sst.example.androiddemo.feature.widget;

import android.content.Context;
import android.graphics.*;
import android.os.Build;
import androidx.annotation.Nullable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
import sst.example.androiddemo.feature.R;
import sst.example.androiddemo.feature.graphics.BitmapActivity;

import java.util.Locale;

public class PaintView extends View {
    Paint mPaint = new Paint();
    TextPaint textPaint = new TextPaint();

    Bitmap bitmapB;
    Bitmap bitmapC;

    public PaintView(Context context) {
        this(context, null);
    }

    public PaintView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PaintView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    @SuppressWarnings("all")
    public PaintView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        // 需禁用硬件加速   硬件加速下不支持阴影！！！！！！！
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        bitmapB = BitmapActivity.getBitmap(context, R.mipmap.ic_launcher_round);
        bitmapC = BitmapActivity.getBitmap(context, R.mipmap.ic_launcher_round);

        int linkColor = textPaint.linkColor;
//        textPaint.setUnderlineText(GRAY,10);//设置下滑线颜色


    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //清空画笔
        mPaint.reset();
        mPaint.set(new Paint());//设置外来画笔
        mPaint.setFlags(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);//相当于调用相应的API

//        STROKE //描边
//                FILL //填充
//        FILL_AND_STROKE //描边加填充
        mPaint.setStyle(Paint.Style.STROKE);  //设置画笔模式为填充
        mPaint.setTypeface(Typeface.DEFAULT_BOLD);//设置字体
//        Android系统默认支持三种字体，分别为：“sans”, “serif”, “monospace"
        Typeface.Builder builder = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) { //api 26 android8.0
            builder = new Typeface.Builder("");
            builder.setWeight(100);
            Typeface tf = builder.build();
        }
        mPaint.setTypeface(Typeface.create("sans",Typeface.BOLD));

        mPaint.setAlpha(0);//设置透明度
        mPaint.setStrokeWidth(5);//设置画笔宽度
        mPaint.setAntiAlias(true);//设置画笔是否抗锯齿
        mPaint.setColor(Color.RED);//设置画笔颜色
        mPaint.setFakeBoldText(true);//是否使用伪粗体
        mPaint.setTextSkewX(-0.25f); //设置斜体
        mPaint.setUnderlineText(true);//设置下划线
        mPaint.setStrikeThruText(true);//设置删除线
        mPaint.setTextSize(60);//设置字体大小
        mPaint.setTextScaleX(1.5f);//设置文字横向放缩。也就是文字变胖变瘦
        mPaint.setLetterSpacing(0.05f);//设置字符间距。默认值是 0
        mPaint.setFontFeatureSettings("smcp");//设置 "small caps" 用 CSS 的 font-feature-settings 的方式来设置文字
        mPaint.setTextAlign(Paint.Align.CENTER);//设置文字的对齐方式。一共有三个值：LEFT CETNER 和 RIGHT。默认值为 LEFT
        mPaint.setTextLocale(Locale.CHINA);//设置绘制所使用的 Locale  Locale 直译是「地域」，其实就是你在系统里设置的「语言」或「语言区域」（具体名称取决于你用的是什么手机
//        不过在现在（ 2017 年），手机屏幕的像素密度已经非常高，几乎不会再出现字体尺寸小到需要靠 hinting 来修正的情况，所以这个方法其实……没啥用了
        mPaint.setHinting(Paint.HINTING_ON);//设置是否启用字体的 hinting （字体微调） 通过向字体中加入 hinting 信息，让矢量字体在尺寸过小的时候得到针对性的修正，从而提高显示效果
        mPaint.setElegantTextHeight(false);//true使用文字（泰文等比较高的文字）的原始（优雅）版本
//        和前面讲的字体 hinting 一样，由于现在手机屏幕像素密度已经很高，所以默认抗锯齿效果就已经足够好了，一般没必要开启次像素级抗锯齿，所以这个方法基本上没有必要使用
        mPaint.setSubpixelText(true);//是否开启次像素级的抗锯齿（ sub-pixel anti-aliasing ）根据程序所运行的设备的屏幕类型，来进行针对性的次像素级的抗锯齿计算，从而达到更好的抗锯齿效果

//        设置是否打开线性文本标识
//        在Android中文本的绘制需要使用一个bitmap作为单个字符的缓存，既然是缓存必定要使用一定的空间，我们可以通过setLinearText (true)告诉Android我们不需要这样的文本缓存
//        将有助于文本在LCD屏幕上的显示效果
        mPaint.setLinearText(true);

//        获取推荐的行距 即推荐的两行文字的 baseline 的距离。这个值是系统根据文字的字体和字号自动计算的。
//        它的作用是当你要手动绘制多行文字（而不是使用 StaticLayout）的时候，可以在换行的时候给 y 坐标加上这个值来下移文字   调整y来增大行距
        mPaint.getFontSpacing();

//        两行文字的 font spacing (即相邻两行的 baseline 的距离) 可以通过  bottom - top + leading (top 的值为负，前面刚说过，记得吧？）来计算得出
//       bottom - top + leading 的结果是要大于 getFontSpacing() 的返回值的。
//        getFontSpacing() 的结果并不是通过 FontMetrics 的标准值计算出来的，而是另外计算出来的一个值，它能够做到在两行文字不显得拥挤的前提下缩短行距，以此来得到更好的显示效果。
//        你要对文字手动换行绘制，多数时候应该选取 getFontSpacing() 来得到行距，不但使用更简单，显示效果也会更好
        Paint.FontMetrics fm = mPaint.getFontMetrics();//获取 Paint 的 FontMetrics

        mPaint.getTextBounds("hhh",0,2,new Rect());//获取文字的显示范围
//        getTextBounds: 它测量的是文字的显示范围（关键词：显示）。形象点来说，你这段文字外放置一个可变的矩形，然后把矩形尽可能地缩小，一直小到这个矩形恰好紧紧包裹住文字，那么这个矩形的范围，就是这段文字的 bounds
//        它测量的是文字绘制时所占用的宽度（关键词：占用）。前面已经讲过，一个文字在界面中，往往需要占用比他的实际显示宽度更多一点的宽度，以此来让文字和文字之间保留一些间距
        mPaint.measureText("hhh");//测量文字的宽度并返回
        mPaint.getTextWidths("hhh",new float[3]);//获取字符串中每个字符的宽度，并把结果填入参数 widths
        //breakText() 是在给出宽度上限的前提下测量文字的宽度。如果文字的宽度超出了上限，那么在临近超限的位置截断文字
//        返回值count是截取的文字个数（如果宽度没有超限，则是文字的总个数)
//        这个方法可以用于多行文字的折行计算
        int count = mPaint.breakText("hhh",0,2,true,500,new float[]{});//

//        对于一段文字，计算出某个字符处光标的 x 坐标
//        contextStart contextEnd 是上下文的起始和结束坐标；isRtl 是文字的方向；offset 是字数的偏移，即计算第几个字符处的光标
//        说是测量光标位置的，本质上这也是一个测量文字宽度的方法
//        start 和  contextStart 都是 0， end contextEnd 和 offset 都等于 text.length()。在这种情况下，它是等价于 measureText(text) 的，即完整测量一段文字的宽度
//        🇨🇳（五星红旗）符号虽然占了 4 个字符（\uD83C\uDDE8\uD83C\uDDF3），但当 offset 是表情中间处时，  getRunAdvance() 得出的结果并不会在表情的中间处。为什么？因为这是用来计算光标的方法啊，光标当然不能出现在符号中间啦。
//          width 是start 到offset的距离
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {//api 23 (android6.0)
           float width =  mPaint.getRunAdvance("hhh",0,2,0,2,true,2);
        }

//        给出一个位置的像素值，计算出文字中最接近这个位置的字符偏移量（即第几个字符最接近这个坐标
//        getOffsetForAdvance() 配合上 getRunAdvance() 一起使用，就可以实现「获取用户点击处的文字坐标」的需求
//        advance 是给出的位置的像素值。填入参数，对应的字符偏移量将作为返回值返回。
        // TODO: 2018/12/12 实现这个需求
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {//api 23 (android6.0)
            int offset = mPaint.getOffsetForAdvance("hhh",0,2,0,2,true,1);
        }

//        检查指定的字符串中是否是一个单独的字形 (glyph） a-true    ad-false   🚩-true
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {//api 23 (android6.0)
            boolean isHasGlyph = mPaint.hasGlyph("hh");
        }

        canvas.drawText("h h h h 和", 500, 100, mPaint);
//        Cap.ROUND(圆形线冒)、  在原先基础上加上半圆
//        Cap.SQUARE(方形线冒)、 在原先基础上加上方形
//        Paint.Cap.BUTT(无线冒)
//        冒多出来的那块区域就是线帽！就相当于给原来的直线加上一个帽子一样，所以叫线帽
        mPaint.setStrokeCap(Paint.Cap.ROUND);//设置线冒样式

//        设置线段连接处样式     一条线段的转角处
//        1. Join.MITER（结合处为锐角）、
//        1. Join.Round(结合处为圆弧)、
//        1. Join.BEVEL(结合处为直线)
        mPaint.setStrokeJoin(Paint.Join.ROUND);

//设置笔画的倾斜度，setStrokeJoin() 的一个补充，它用于设置 MITER 型拐角的延长线的最大值。
// 所谓「延长线的最大值」，是这么一回事当线条拐角为 MITER 时，拐角处的外缘需要使用延长线来补偿
//        方法叫做setStrokeJoinMiterLimit(limit) 更合适
//        https://hencoder.com/ui-1-2/
//        默认情况下，夹角大于这个角的尖角会被保留，而小于这个夹角的就会被「削成平头」
        mPaint.setStrokeMiter(90);

//        设定是否使用图像抖动处理，会使绘制出来的图片颜色更加平滑和饱满、图像更加清晰。
//        不过对于现在（2017年）而言， setDither(dither) 已经没有当年那么实用了，
//        因为现在的 Android 版本的绘制，默认的色彩深度已经是 32 位的 ARGB_8888 ，效果已经足够清晰了。只有当你向自建的  Bitmap 中绘制，并且选择 16 位色的 ARGB_4444 或者 RGB_565 的时候，开启它才会有比较明显的效果。
        mPaint.setDither(true);

//        加上这一行，在放大绘制 Bitmap 的时候就会使用双线性过滤了
//        图像在放大绘制的时候，默认使用的是最近邻插值过滤，这种算法简单，但会出现马赛克现象；而如果开启了双线性过滤，就可以让结果图像显得更加平滑
        mPaint.setFilterBitmap(true);

//        在图形下面设置阴影层，产生阴影效果，radius为阴影的角度，dx和dy为阴影在x轴和y轴上的距离，color为阴影的颜色
//        模糊半径，radius越大越模糊，越小越清晰，但是如果radius设置为0，则阴影消失不见
//        setShadowLayer 不支持硬件加速；
        mPaint.setShadowLayer(10, 50, 50, Color.GREEN);
        mPaint.clearShadowLayer();//清楚阴影
        canvas.drawRect(new Rect(100, 100, 300, 300), mPaint);


        mPaint.setStyle(Paint.Style.FILL);
        //设置着色器Shader.TileMode.CLAMP 夹子  Shader.TileMode.repeat 重复    MIRROR镜像
//        BitmapShader：实现圆形图像
//        LinearGradient：实现渐变文字
//        SweepGradient：实现雷达效果
//        RadialGradient ：水波纹点击效果
//        LinearGradient 线性渐变
        Shader shader = new LinearGradient(300, 100, 500, 300, Color.RED, Color.GREEN, Shader.TileMode.CLAMP);
        Shader shader1 = new LinearGradient(300, 100, 500, 300, Color.RED, Color.GREEN, Shader.TileMode.CLAMP);
//        RadialGradient 辐射渐变 radius辐射半径
        shader = new RadialGradient((300 + 500) / 2, (100 + 300) / 2, 80, Color.RED, Color.GREEN, Shader.TileMode.CLAMP);
        Shader shader2 = new RadialGradient((300 + 500) / 2, (100 + 300) / 2, 80, Color.RED, Color.GREEN, Shader.TileMode.CLAMP);

//        SweepGradient 扫描渐变
        shader = new SweepGradient((300 + 500) / 2, (100 + 300) / 2, Color.RED, Color.GREEN);

//        BitmapShader     用Bitmap 的像素来作为图形或文字的填充
        shader = new BitmapShader(bitmapB, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);

//        ComposeShader 混合着色器  可以对各种图片和颜色渐变进行操作 Mode有17中模式
        shader = new ComposeShader(shader1, shader2, PorterDuff.Mode.SRC_OVER);

        //移动渐变
        Matrix mMatrix = new Matrix();
        mMatrix.setTranslate(10, 0);
        //给shader设置matrix,通过改变matrix来控制shader
        shader.setLocalMatrix(mMatrix);
//        mPaint.setShader(shader);
        canvas.drawRect(new Rect(300, 100, 500, 300), mPaint);

//        为绘制设置颜色过滤
//        模拟简单的光照效果的,
        //去掉红色
        // TODO: 2018/12/11 数学？？？
        ColorFilter cf = new LightingColorFilter(0x00ffff, 0x000000);
        //PorterDuffColorFilter  使用一个指定的颜色和一种指定的 PorterDuff.Mode 来与绘制对象进行合成
        cf = new PorterDuffColorFilter(Color.RED, PorterDuff.Mode.SRC_OVER);

//       ColorMatrixColorFilter 使用一个 ColorMatrix 来对颜色进行处理
        // TODO: 2018/12/11 效果没出来 ？？
        cf = new ColorMatrixColorFilter(new ColorMatrix());
        mPaint.setColorFilter(cf);
        canvas.drawBitmap(bitmapB, new Rect(100, 500, 100 + bitmapB.getWidth(), 500 + bitmapB.getHeight()), new Rect(100, 500, 100 + bitmapB.getWidth(), 500 + bitmapB.getHeight()), mPaint);


//        Xfermode 指的是你要绘制的内容和 Canvas 的目标位置的内容应该怎样结合计算出最终的颜色。
//        但通俗地说，其实就是要你以绘制的内容作为源图像，以 View 中已有的内容作为目标图像，选取一个  PorterDuff.Mode 作为绘制内容的颜色处理方案
//        要想使用 setXfermode() 正常绘制，必须使用离屏缓存 (Off-screen Buffer)(savelayer和restorecount) 把内容绘制在额外的层上，再把绘制好的内容贴回 View 中
        int saved = canvas.saveLayer(null, null, Canvas.ALL_SAVE_FLAG);
        Xfermode xfermode = new PorterDuffXfermode(PorterDuff.Mode.DST_IN);
        canvas.drawBitmap(bitmapB, 0, 0, mPaint); // 画方
        mPaint.setXfermode(xfermode); // 设置 Xfermode
        canvas.drawBitmap(bitmapC, 0, 0, mPaint); // 画圆
        mPaint.setXfermode(null); // 用完及时清除 Xfermode
        canvas.restoreToCount(saved);


        Path path = new Path();
        path.lineTo(500, 600);
        path.lineTo(600, 500);
        path.lineTo(700, 700);
        path.lineTo(800, 400);

        mPaint.setStyle(Paint.Style.STROKE);
//        setPathEffect  给图形的轮廓设置效果
//        CornerPathEffect  把所有拐角变成圆角
        PathEffect pe = new CornerPathEffect(20);

//        DiscretePathEffect  把线条进行随机的偏离，让轮廓变得乱七八糟。乱七八糟的方式和程度由参数决定
//        segmentLength 是用来拼接的每个线段的长度， deviation 是偏离量
        pe = new DiscretePathEffect(50, 5);

//        DashPathEffect  使用虚线来绘制线条。
//        第一个参数 intervals 是一个数组，它指定了虚线的格式：数组中元素必须为偶数（最少是 2 个），按照「画线长度、空白长度、画线长度、空白长度」……的顺序排列，例如上面代码中的 20, 5, 10, 5 就表示虚线是按照「画 20 像素、空 5 像素、画 10 像素、空 5 像素」的模式来绘制；
//         第二个参数 phase 是虚线的偏移量
        pe = new DashPathEffect(new float[]{20, 10, 5, 10}, 0);
        PathEffect pe1 = new DashPathEffect(new float[]{20, 10, 5, 10}, 0);

//        PathDashPathEffect
//        这个方法比 DashPathEffect 多一个前缀 Path ，所以顾名思义，它是使用一个 Path 来绘制「虚线」
//        shape 参数是用来绘制的 Path ；
// advance 是两个相邻的 shape 段之间的间隔，不过注意，这个间隔是两个 shape 段的起点的间隔，而不是前一个的终点和后一个的起点的距离； phase 和  DashPathEffect 中一样，是虚线的偏移；
// 最后一个参数 style，是用来指定拐弯改变的时候 shape 的转换方式。
        Path shape = new Path();
        shape.addRect(new RectF(0, 0, 10, 10), Path.Direction.CW);
//        TRANSLATE：位移
//        ROTATE：旋转
//        MORPH：变体
        pe = new PathDashPathEffect(shape, 40, 0, PathDashPathEffect.Style.TRANSLATE);
        PathEffect pe2 = new PathDashPathEffect(shape, 40, 0, PathDashPathEffect.Style.TRANSLATE);

//        SumPathEffect  这是一个组合效果类的 PathEffect 。它的行为特别简单，就是分别按照两种 PathEffect 分别对目标进行绘制
        pe = new SumPathEffect(pe1, pe2);

//        ComposePathEffect
//        这也是一个组合效果类的 PathEffect 。不过它是先对目标 Path 使用一个 PathEffect，然后再对这个改变后的 Path 使用另一个 PathEffect。
        pe = new ComposePathEffect(pe1,pe2);
        mPaint.setPathEffect(pe);
        canvas.drawPath(path, mPaint);


//        setMaskFilter  在绘制层上方的附加效果
//        BlurMaskFilter 模糊效果的 MaskFilter。
//        NORMAL: 内外都模糊绘制
//        SOLID: 内部正常绘制，外部模糊
//        INNER: 内部模糊，外部不绘制
//        OUTER: 内部不绘制，外部模糊（
        MaskFilter mf = new BlurMaskFilter(100, BlurMaskFilter.Blur.NORMAL);

//        EmbossMaskFilter  浮雕效果的 MaskFilter
        mf = new EmbossMaskFilter(new float[]{0, 1, 1}, 0.2f, 8, 10);
        mPaint.setMaskFilter(mf);
        canvas.drawBitmap(bitmapB,100,700,mPaint);


//        获取绘制的path 获取path后可以进行一些效果操作

//        获取src的实际path dst；
//        所谓实际 Path ，指的就是 drawPath() 的绘制内容的轮廓，要算上线条宽度和设置的 PathEffect。
        mPaint.getFillPath(path,new Path());

//        mPaint.getTextPath();
//        获取「文字的 Path」。
//        文字的绘制，虽然是使用 Canvas.drawText()方法，但其实在下层，文字信息全是被转化成图形，对图形进行绘制的。
        mPaint.getTextPath("hhhh",0,3,10,10,new Path());//获取的就是目标文字所对应的 Path 。这个就是所谓「文字的 Path」


    }

}
