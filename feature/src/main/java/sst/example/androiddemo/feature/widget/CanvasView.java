package sst.example.androiddemo.feature.widget;

import android.content.Context;
import android.graphics.*;
import android.os.Build;
import android.view.animation.BounceInterpolator;
import androidx.annotation.Nullable;
import android.text.DynamicLayout;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
import sst.example.androiddemo.feature.R;
import sst.example.androiddemo.feature.graphics.BitmapActivity;

public class CanvasView extends View {
    Bitmap bitmap;

    Canvas mCanvas;
    Paint mPaint =new Paint();
    Rect rect = new Rect();
    Rect rect2 = new Rect();
    Rect rect3 = new Rect();
    Path path = new Path();
    Bitmap bitmapB;
    //将图像分成多少格
    private int WIDTH = 200;
    private int HEIGHT = 200;
    //交点坐标的个数
    private int COUNT = (WIDTH + 1) * (HEIGHT + 1);
    //用于保存COUNT的坐标
    //x0, y0, x1, y1......
    private float[] verts = new float[COUNT * 2];
    //用于保存原始的坐标
    private float[] orig = new float[COUNT * 2];


    public CanvasView(Context context) {
        this(context, null);
    }

    public CanvasView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CanvasView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public CanvasView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        bitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888);
        bitmapB = BitmapActivity.getBitmap(context,R.mipmap.ic_launcher_round);
        //以bitmap 作为参数的canvas，后续的绘制都将绘制在该bitmap上
        mCanvas = new Canvas(bitmap);
        mPaint.setAntiAlias(true);
        rect.set(0,0,100,100);
        rect2.set(0,0,50,50);
        rect3.set(20,20,80,80);
        path.addCircle(50,50,50,Path.Direction.CW);
        mPaint.setColor(Color.RED);
        initView();

    }

    private void initView() {
        int index = 0;
        float bmWidth = bitmap.getWidth();
        float bmHeight = bitmap.getHeight();

        for (int i = 0; i < HEIGHT + 1; i++) {
            float fy = bmHeight * i / HEIGHT;
            for (int j = 0; j < WIDTH + 1; j++) {
                float fx = bmWidth * j / WIDTH;
                //X轴坐标 放在偶数位
                verts[index * 2] = fx;
                orig[index * 2] = verts[index * 2];
                //Y轴坐标 放在奇数位
                verts[index * 2 + 1] = fy;
                orig[index * 2 + 1] = verts[index * 2 + 1];
                index += 1;
            }
        }
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //layer系列
        canvas.save();
        //        canvas 变化系列  改变之后，任何的后续操作都会受到影响
        //当缩放倍数为负数时，会先进行缩放，然后根据不同情况进行图形翻转
        canvas.scale(2f,2f);
        canvas.scale(1,-1,100,100);
        //平移
        canvas.translate(100,100);
        //旋转
        canvas.rotate(90);
        //// 参数 sx = tan a ，sx>0时表示向X正方向倾斜（即向左）
        //// 参数 sy = tan b ，sy>0时表示向Y正方向倾斜（即向下）
        canvas.skew(1,-1);

        //todo 3d旋转
        Camera camera;
//        camera.rotateZ();  绕z轴旋转，即在屏幕所在平面移动
//          camera.rotateX(0); 绕x轴旋转，屏幕里外上下
//          camera.rotateY(0); 绕Y轴旋转，屏幕里外左右


        //draw 系列
        mCanvas.drawColor(Color.RED);


        //裁剪功能canvas.clipRect()---Region.Op.INTERSECT交集    clipOutRect---Region.Op.DIFFERENCE差集  在AndroidP(28)及以上只有这两个是有效的参数
        // 这种情况可以用path替换 Op.UNION并集     XOR补集  REVERSE_DIFFERENCE差集  REPLACE 不论A和B的集合状况，B的范围将全部进行显示，如果和A有交集，则将覆盖A的交集范围；

        //canvas区域与rect的交集
        canvas.clipRect(rect);
//        与下面相同同
//        canvas.clipRect(rect, Region.Op.INTERSECT);
        //当前区域与rect2的交集
        canvas.clipRect(rect2);
        //当前区域与rect3取差集
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            canvas.clipOutRect(rect3);
        }
        //与下面相同
//        canvas.clipRect(rect3, Region.Op.DIFFERENCE);
        //按path裁剪 Region.Op.INTERSECT
        canvas.clipPath(path);
        //Region.Op.DIFFERENCE
//        canvas.clipOutPath(path);
//        clipRegion被废弃不使用
//        canvas.clipRegion

        //定制bitmap
        canvas.drawBitmap(bitmap,0,0,mPaint);
        //取源bitmap一部分绘制道目标路径
        canvas.drawBitmap(bitmap,new Rect(100,100,50,50),new Rect(200,200,300,300),mPaint);
        canvas.restore();
        canvas.drawRect(0,0,50,50,mPaint);

//https://www.jianshu.com/p/51d8dd99d27d
        //网格交叉点坐标数组，长度为(meshWidth + 1) * (meshHeight + 1) * 2
//        vertOffset：控制verts数组中从第几个数组元素开始才对bitmap进行扭曲
//        drawBitmapMesh() 方法改变图像的方式，就是通过改变这个 verts 数组里的元素的坐标值来重新定位对应的图像块的位置，从而达到图像效果处理的功能
//        canvas.drawBitmapMesh(bitmapB, WIDTH, HEIGHT, verts, 100, null, 0, null);

        // TODO: 2018/12/17
        //使用drawbitmapmesh绘制效果



        //绘制系列
//        canvas.drawTextRun();   //加入了两项额外的设置——上下文和文字方向——用于辅助一些文字结构比较特殊的语言的绘制。

//        StaticLayout   对文字自动换行  文字不改变
//        width 是文字区域的宽度，文字到达这个宽度后就会自动换行；
//        align 是文字的对齐方向；
//        spacingmult 是行间距的倍数，通常情况下填 1 就好；
//        spacingadd 是行间距的额外增加值，通常情况下填 0 就好；
//        includeadd 是指是否在文字上下添加额外的空间，来避免某些过高的字符的绘制出现越界。
        StaticLayout staticLayout1 = new StaticLayout("text1", new TextPaint(), 600,
                Layout.Alignment.ALIGN_NORMAL, 1, 0, true);

//        DynamicLayout  文字改变 api 28加入
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            DynamicLayout dl = DynamicLayout.Builder.obtain("",new TextPaint(),0).build();
        }

    }

}
