package sst.example.androiddemo.feature.widget;

import android.content.Context;
import android.graphics.*;
import android.os.Build;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import sst.example.androiddemo.feature.util.MyUtils;

public class PathView extends View {
    Path pathA = new Path();
    Path pathB ;
    Paint mPaint = new Paint();
    public PathView(Context context) {
        this(context,null);
    }

    public PathView(Context context,  @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public PathView(Context context,  @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr,0);
    }

    public PathView(Context context,  @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        //        将新的path赋值到现有path  // 大致相当于 path = src;
        pathA.set(new Path());
        //reset不保留内部数据结构，但会保留FillType.
        pathA.reset();
        //rewind会保留内部的数据结构，但不保留FillType
        pathA.rewind();
        pathA.addRect(new RectF(0,0,100,100),Path.Direction.CW);
        //需要多个path ,pathB从pathA获得属性
        pathB = new Path(pathA);

        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.RED);
        mPaint.setStyle(Paint.Style.FILL);//默认fill  path没有close会自动闭合



    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

//        addXxx()系列 ——添加子图形
//        顺时针 (CW clockwise) 和逆时针 (CCW counter-clockwise) 。对于普通情况，这个参数填 CW 还是填 CCW 没有影响。
//        它只是在需要填充图形 (Paint.Style 为 FILL 或  FILL_AND_STROKE) ，并且图形出现自相交时，用于判断填充范围的
        pathA.addCircle(100,100,50,Path.Direction.CW);
        pathA.addOval(new RectF(300,0,400,200),Path.Direction.CW);//椭圆
        pathA.addRoundRect(new RectF(500,0,600,200),10,100,Path.Direction.CW);
        pathA.addArc(new RectF(600,0,700,100),-90,90);


        //        xxxTo() ——画线（直线或曲线）
        //画直线系列   从当前位置向目标位置画一条直线， x 和 y 是目标位置的坐标
        pathA.lineTo(200, 200); //lineTo(x, y) 的参数是绝对坐标
//        rLineTo(x, y) 的参数是相对当前位置的相对坐标 （前缀 r 指的就是  relatively 「相对地」)。
//        当前位置：所谓当前位置，即最后一次调用画 Path 的方法的终点位置。初始值为原点 (0, 0)。
        pathA.rLineTo(50,20);  //起点
//        画二次贝塞尔曲线   二次贝塞尔曲线的起点就是当前位置，上一次绘制的终点   两个点分别是控制点和结束点
        pathA.quadTo(300,300,300,350);
        pathA.rQuadTo(50,50,50,100);//相对上一次坐标坐标
//       画三次贝塞尔曲线
        pathA.cubicTo(400,400,500,410,600,420);
        pathA.rCubicTo(50,50,150,10,250,20);
        //更高阶贝塞尔曲线降阶
        // 1 分为一段一段的  sin函数与x轴的交点可以分为一段段二阶贝塞尔  设计师设计也是一段段设计的？
        // 2 todo https://juejin.im/post/5c3988516fb9a049d1325c83
           //  https://github.com/GcsSloop/AndroidNote/blob/master/CustomView/Advance/%5B06%5DPath_Bezier.md
        //移动坐标  可以通过 moveTo(x, y) 或 rMoveTo() 来改变当前位置，从而间接地设置这些方法的起点。
        pathA.moveTo(0,600);//
        pathA.lineTo(300,600);

        //arcto  只用来画弧形
//        forceMoveTo 参数的意思是，绘制是要true「抬一下笔移动过去」，还是false「直接拖着笔过去」，区别在于是否留下移动的痕迹。
        // TODO: 2018/12/12 角度坐标系
        pathA.arcTo(new RectF(300,700,400,800),-90,90,true);

//        addArc() 只是一个直接使用了 forceMoveTo = true 的简化版 arcTo()  api28源码并不是这样的？？native方法是怎样？？
        pathA.addArc(new RectF(300,800,400,900),-90,90);

//        它的作用是把当前的子图形封闭，即由当前位置向当前子图形的起点绘制一条直线
//        close() 和 lineTo(起点坐标) 是完全等价的
//        当需要填充图形时（即 Paint.Style 为  FILL 或 FILL_AND_STROKE），Path 会自动封闭子图形。
//        子图形」：官方文档里叫做 contour 。但由于在这个场景下我找不到这个词合适的中文翻译（直译的话叫做「轮廓」），所以我换了个便于中国人理解的词：「子图形」。
//        前面说到，第一组方法是「添加子图形」，所谓「子图形」，指的就是一次不间断的连线。
//        一个 Path 可以包含多个子图形。当使用第一组方法，即 addCircle() addRect() 等方法的时候，每一次方法调用都是新增了一个独立的子图形；而如果使用第二组方法，即 lineTo() arcTo() 等方法的时候，则是每一次断线（即每一次「抬笔」），都标志着一个子图形的结束，以及一个新的子图形的开始
        pathA.close();

//        辅助的设置或计算
//        setFillType 设置填充方式（Paint发生填充时才生效）  用来设置图形自相交时的填充算法的：
//        简单粗暴总结：WINDING 是「全填充」（默认值），而 EVEN_ODD 是「交叉填充」     inverse_ 取反
//        即 even-odd rule （奇偶原则）：对于平面中的任意一点，向任意方向射出一条射线，这条射线和图形相交的次数（相交才算，相切不算哦）
//           如果是奇数，则这个点被认为在图形内部，是要被涂色的区域；
//           如果是偶数，则这个点被认为在图形外部，是不被涂色的区域
//        射线的方向无所谓，同一个点射向任何方向的射线，结果都是一样的

//        即 non-zero winding rule （非零环绕数原则）：首先，它需要你图形中的所有线条都是有绘制方向的：
//         然后，同样是从平面中的点向任意方向射出一条射线，
//        但计算规则不一样：以 0 为初始值，对于射线和图形的所有交点，遇到每个顺时针的交点（图形从射线的左边向右穿过）把结果加 1，遇到每个逆时针的交点（图形从射线的右边向左穿过）把结果减 1，
//        最终把所有的交点都算上，得到的结果如果不是 0，则认为这个点在图形内部，是要被涂色的区域；如果是 0，则认为这个点在图形外部，是不被涂色的区域
//        面的那个「简单粗暴」的总结，对于 WINDING 来说并不完全正确：如果你所有的图形都用相同的方向来绘制，那么 WINDING 确实是一个「全填充」的规则；但如果使用不同的方向来绘制图形，结果就不一样了。

//        图形的方向：对于添加子图形类方法（如 Path.addCircle() Path.addRect()）的方向，由方法的  dir 参数来控制，这个在前面已经讲过了；
//        而对于画线类的方法（如 Path.lineTo() Path.arcTo()）就更简单了，线的方向就是图形的方向。
        pathA.setFillType(Path.FillType.EVEN_ODD);


//        approximate 接近，大约    接近path，返回误差小于0.5f的坐标集合
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {//api26 Android8.0
            float[] fff = pathA.approximate(0.5f);
        }

//      计算Path所占用的空间以及所在位置 exact 是否精确测量，即将废弃该boolean值
        RectF rf = new RectF();
        pathA.computeBounds(rf,true);
        MyUtils.log(" path 所占用的空间是 "+rf.toString());

//        提示Path还有多少个点等待加入，可以让path更高效的分配内存     hint 暗示，提示
        pathA.incReserve(100);

//        判断path是否为凸多边形，如果是就为true，反之为false。
//        凸多边形的概念:
//        1.每个内角小于180度
//        2.任何两个顶点间的线段位于多边形的内部或边界上
//        三角形是凸的，矩形也是凸的，但是五角星形就不是凸的
//        手动绘制出 Outline 所需要的路径，这个路径是有要求的，必须是凸多边形
        boolean isConvex = false;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            isConvex = pathA.isConvex();
        }
        MyUtils.log("isConvex is "+isConvex);

//        判断path中是否包含内容
        pathA.isEmpty();

//        判断path是否是一个矩形，如果是一个矩形的话，会将矩形的信息存放进参数rect中。
        pathA.isRect(new RectF());

        //判断filltype是不是INVERSE系列的
        pathA.isInverseFillType();

        //对当前path进行移动
        pathA.offset(0,10);

//        组合两个Path
//        Path.Op.DIFFERENCE从path中去除path2的部分，保留path的部分
//        Path.Op.INTERSECT 取path和path2相交的部分显示出来
//        Path.Op.REVERSE_DIFFERENCE  表示除去path的部分，只显示path2的部分
//        Path.Op.UNION  path和path2的部分都要显示出来
//        Path.Op.XOR  显示path和path2但是不包含二者的交集
        pathA.op(pathB, Path.Op.UNION);


        pathA.setFillType(Path.FillType.INVERSE_EVEN_ODD);
        //反转filltype
        pathA.toggleInverseFillType();

        //矩阵变化
        pathA.transform(new Matrix());

        //设置当前path的终点  移动终点，path.close() 形成闭合曲线
        pathA.setLastPoint(400,1000);


//        path测量
        PathMeasure pm = new PathMeasure(pathA,false);
        //设置path
        pm.setPath(pathA,false);

        //获取当前轮廓path长度    forceClosed为true时，测量的长度可能比实际长度要长
        MyUtils.log("path get length is "+pm.getLength()+"");

//        判断关联的Path是否是闭合状态
        pm.isClosed();

        //截取Path的一段，如果截取成功，返回true；反之返回false 开始距离，结束距离,存储片段的path，startwithmoveto
//        起点是否使用MoveTo，如果为true，则截取的path的第一个点不会变化，截取的path也不会改变，如果为false，则截取的path可能会发生形变
//        startWithMoveTo为false时，会将截取的path片段的起始点移动到dstPath的终点以保持dstPath的连续性。
//        如果目标path（pathB）存在内容，startwithmoveto为false 则截取的起点与pathB原先内容终点相连，true，跳过间隔，直接从截取片段的起点到终点开始绘制
//         true可以保证截取path的原样，false不能
        pm.getSegment(100,200,pathB,true);
        MyUtils.log(" pm.getsegment "+pathB.toString());

        Matrix matrix = new Matrix();
//        距离Path起始点的一段长度distance，
//        通过计算得到该位置坐标并返回一个处理好的矩阵，该矩阵以左上角为旋转点，如果Path不存在或者长度为0，该方法返回false
//        PathMeasure.POSITION_MATRIX_FLAG:位置信息 ，
//        PathMeasure.TANGENT_MATRIX_FLAG:切边信息，方位角信息，使得图片按path旋转
        pm.getMatrix(100,matrix,PathMeasure.POSITION_MATRIX_FLAG);

//        距离Path起始点的长度distance，通过计算返回该长度在Path上的坐标及该坐标的正切值并分别赋值给pos[]、tan[]。
        float[] pos = new float[5]; //坐标
        float[] tan = new float[5]; //角度
        pm.getPosTan(5,pos,tan);
        //tan转为角度
        Math.toDegrees(tan[0]);

//        如果Path由多条曲线组成且彼此不连接，则getLength getSegment getMatrix getPosTan方法都是针对当前正在操作的，
//        比如：如果Path由多条曲线组成且彼此不连接，getLength()返回的只是当前操作的曲线的长度，并不是所有曲线的长度，那么怎么跳转到下一条曲线上呢？
//        答案就是用nextContour()，跳转成功返回true;否则返回false。
        //跳转到下一个轮廓
        pm.nextContour();

        canvas.drawPath(pathA,mPaint);

    }

    // TODO: 2018/12/13
    //path 与svg String 转换
}
