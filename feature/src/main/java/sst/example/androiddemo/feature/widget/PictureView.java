package sst.example.androiddemo.feature.widget;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.PictureDrawable;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

/**
 * picture 是记录了每个绘制的过程,然后可以重现
 */
public class PictureView extends View {
    Picture p = new Picture();
    Paint mPaint = new Paint();
    Paint mPaint1 = new Paint();
    boolean isShowContent = false;
    public PictureView(Context context) {
        this(context,null);
    }

    public PictureView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public PictureView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr,0);
    }

    public PictureView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mPaint.setColor(Color.RED);
        mPaint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Canvas canvas1 = p.beginRecording(getWidth(),getHeight());
//        canvas1.drawCircle(0,0,100,mPaint);
        mPaint1.setLetterSpacing(0.5f);
        mPaint1.setColor(Color.BLUE);
        mPaint1.setTextSize(50);
        canvas1.drawText("hhhhhhh",50,50,mPaint1);
        p.endRecording();
        if (isShowContent){
//            p.draw(canvas);
            PictureDrawable pd = new PictureDrawable(p);
            pd.setBounds(new Rect(0,0,200,500));
//            pd.setAlpha(100);
//            ColorMatrix colorMatrix = new ColorMatrix();
//            colorMatrix.setRotate(0,100);
//            pd.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
            pd.draw(canvas);
        }

    }

    public void setShowContent(boolean showContent) {
        isShowContent = showContent;
        invalidate();
    }


}
