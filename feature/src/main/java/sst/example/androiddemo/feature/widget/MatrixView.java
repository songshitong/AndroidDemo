package sst.example.androiddemo.feature.widget;


import android.content.Context;
import android.graphics.*;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import sst.example.androiddemo.feature.R;
import sst.example.androiddemo.feature.graphics.BitmapActivity;

public class MatrixView extends View {
    Bitmap bitmap;
    Paint paint = new Paint();
    /**
     * 3*3矩阵    一次变换使用setScale等，多次使用post,pre
     */
    Matrix matrix = new Matrix();
    /**
     * 4*5矩阵
     */
    //https://juejin.cn/post/6925703843099443213
//    用4x5矩阵来表示，用于转换位图的颜色和alpha分量。
//            [ a, b, c, d, e,
//    f, g, h, i, j,
//    k, l, m, n, o,
//    p, q, r, s, t ]
//    计算如下:
//    R = a*R + b*G + c*B + d*A + e;
//    G = f*R + g*G + h*B + i*A + j;
//    B = k*R + l*G + m*B + n*A + o;
//    A = p*R + q*G + r*B + s*A + t;
//    通常见的默认标准矩阵
//[ 1 0 0 0 0               R=225            R1=225 + 0 + 0 + 0+偏移量=225
//            0 1 0 0 0       x       G=225     =      G1=225 + 0 + 0 + 0+偏移量=225  =  RGBA=[225,225,225,225]
//            0 0 1 0 0               B=225            B1=225 + 0 + 0 + 0+偏移量=225
//            0 0 0 1 0 ]             A=225            A1=225 + 0 + 0 + 0+偏移量=225
//
//    我想降低绿色和蓝色，那么我们可以将矩阵中的2行2列1变为0.5,3行3列的1变为0.5等操作。
    ColorMatrix colorMatrix = new ColorMatrix();
    public MatrixView(Context context) {
        this(context,null);
    }

    public MatrixView(Context context,  @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public MatrixView(Context context,  @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr,0);
    }

    public MatrixView(Context context,  @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        bitmap = BitmapActivity.getBitmap(context,R.mipmap.ic_launcher_round);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
//        https://github.com/GcsSloop/AndroidNote todo 自定义view系列
        //todo matrix相关 https://github.com/GcsSloop/AndroidNote/blob/master/CustomView/Advance/%5B09%5DMatrix_Basic.md
        //[2,7]倒影效果
//        matrix.setScale(1.0f, -1);
//        //post是在上一次修改的基础上进行再次修改  set 每次操作都是最新的 会覆盖上次的操作
//        matrix.postTranslate(0, bitmap.getHeight());

        //[2.7]镜面效果  如果2个方法一起用
      matrix.setScale(-1.0f, 1);
        //post是在上一次修改的基础上进行再次修改  set 每次操作都是最新的 会覆盖上次的操作
      matrix.postTranslate(bitmap.getWidth(), 0);
      //获取matrix的值，需要3*3数组
//      matrix.getValues();

        //设置sin90,cos0的结果值
//        matrix.setSinCos(1，1);
        canvas.drawBitmap(bitmap,matrix , paint);
        canvas.restore();
        matrix.reset();
        paint.reset();
        float[] src = {bitmap.getWidth()/2, bitmap.getHeight()/2};
        int DX = 50;
        float[] dst = {bitmap.getWidth()/2 + DX, bitmap.getHeight()/2 + DX};
        //通过点的位置变化来变化图片 可以设置0-4个点
        matrix.setPolyToPoly(src, 0, dst, 0, 1);
        //饱和度0-1
        colorMatrix.setSaturation(saturation);
        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        canvas.drawBitmap(bitmap,matrix,paint);

        //对颜色进行变换
        colorMatrix.setRotate(0,90);
        //https://juejin.cn/post/6925703843099443213  矩阵可以查看Rotate的源码
//        [ 1 0 0 0 0             R
//        0 1 0 0 0       x       G
//        0 0 1 0 0               B  =R G B A
//        0 0 0 1 0 ]             A

//          [ 1 0 0 0 0           R
//        0 0 1 0 0       x       G =R B -G A
//        0 -1 0 0 0              B
//        0 0 0 1 0 ]             A
//        结果红没变,绿变为蓝,蓝色变为负,我们知道红和蓝成黄色

    }

    private float saturation = 0;

    public void setSaturation(float saturation){
        this.saturation = saturation;
        invalidate();
    }
}
