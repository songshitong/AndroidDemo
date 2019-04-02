package sst.example.androiddemo.feature.widget.practice;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
//1    8个小球围绕圆心旋球
//2    向外扩散后向圆心内聚集
//3    聚集到圆心后，水波纹扩散出底部内容

public class LoadingView extends View {
    List<Integer> colors ;
    //小球半径5
    float itemRadius = 5;
    //loading圆的半径
    float radius = 50;
    Paint mPaint = new Paint();
    float centerX;
    float centerY;
    LoadingState loadingState;
    public LoadingView(Context context) {
        this(context,null);
    }

    public LoadingView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public LoadingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        colors = new ArrayList<>();
        colors.add(Color.BLUE);
        colors.add(Color.DKGRAY);
        colors.add(Color.GRAY);
        colors.add(Color.LTGRAY);
        colors.add(Color.RED);
        colors.add(Color.GREEN);
        colors.add(Color.BLUE);
        colors.add(Color.YELLOW);
        loadingState = new RotateState();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        loadingState.draw(canvas);
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        //在大小改变时拿到view的大小
        centerX = w;
        centerY = h;
    }

    abstract  class  LoadingState {
        public abstract void draw(Canvas canvas);
    }


    class  RotateState extends  LoadingState{

        @Override
        public void draw(Canvas canvas) {
            for (Integer color : colors) {
                mPaint.setColor(color);
                canvas.drawCircle(centerX,centerY,itemRadius,mPaint);
            }
        }
    }
}

