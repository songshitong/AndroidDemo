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
    }

    private float saturation = 0;

    public void setSaturation(float saturation){
        this.saturation = saturation;
        invalidate();
    }
}
