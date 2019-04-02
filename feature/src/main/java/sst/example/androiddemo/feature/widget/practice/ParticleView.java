package sst.example.androiddemo.feature.widget.practice;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import androidx.annotation.Nullable;
import sst.example.androiddemo.feature.R;
import sst.example.androiddemo.feature.graphics.BitmapActivity;

import java.util.ArrayList;
import java.util.List;

//自定义粒子效果
public class ParticleView extends View {
    //粒子集合
    List<Ball> balls = new ArrayList<>();
    //默认小球半径 default r
    static final int dr = 5;
    Paint mPaint = new Paint();
    ValueAnimator valueAnimator;
    boolean isShowBitmap = true;
    Bitmap  bitmap;
     public ParticleView(Context context) {
        this(context, null);
    }

    public ParticleView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ParticleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mPaint.setAntiAlias(true);
        bitmap = BitmapActivity.getBitmap(getContext(), R.drawable.ic_launcher);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        for (int i = 0; i < width; i+=dr) {
            for (int j = 0; j < height; j+=dr) {
                Ball ball = new Ball();
                initBall(bitmap, i, j, ball);
                balls.add(ball);
            }
        }

        valueAnimator = ValueAnimator.ofFloat(0,1);
        valueAnimator.setDuration(2);
        //重复运行
        valueAnimator.setRepeatCount(-1);
        valueAnimator.setInterpolator(new LinearInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                //当前进度
                float current = animation.getAnimatedFraction();

                updateBall();
                invalidate();
            }
        });
    }

    private void initBall(Bitmap bitmap, int i, int j, Ball ball) {
        //设置每个小球的dr 即每个像素放大dr倍，整张图片放大dr*dr倍数
        ball.r = dr;
        ball.x = i*dr+dr;
        ball.y = j*dr+dr;
        //获取像素的argb
        ball.color = bitmap.getPixel(i,j);
        //初始化速度(-20,20)
        ball.vx = (float)(Math.pow(-1,Math.ceil(Math.random()*1000))*20*Math.random());
        ball.vy = rangInt(-15,35);
        //初始化加速度
        ball.ax =0f;
        ball.ay = 0.98f;
    }

    private int rangInt(int i,int j) {
      int max = Math.max(i,j);
      int min = Math.min(i,j) -1;
      //在0-（max,min）范围内变化，取大于x的最小整数，再随机
      return (int)(min + Math.ceil(Math.random()*(max -min)));
    }

    private void updateBall() {
        for (Ball ball : balls) {
            //距离=速度*t      0-1的变化即使速度累加
            ball.x += ball.vx;
            ball.y += ball.vy;

            // 速度= 加速度的累加
            ball.vx += ball.ax;
            ball.vy += ball.ay;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(MotionEvent.ACTION_DOWN == event.getAction()){
            //截断点击
            isShowBitmap = false;
            valueAnimator.start();
            return  true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.translate(300,300);
        if(!isShowBitmap){
            //用小球组合成图片     每个像素的颜色值拼接起来就是图片   图片就是不同颜色的像素值
            for (Ball ball : balls) {
                mPaint.setColor(ball.color);
                canvas.drawCircle(ball.x,ball.y,ball.r,mPaint);
            }
        }else {
            canvas.drawBitmap(bitmap,0,0,mPaint);
        }

    }

    //定义小粒子的对象
    class Ball {
        //半径
        int r;
        //颜色
        int color;
        //圆心坐标
        float x;
        float y;
        //x 轴速度
        float vx;
        //y 轴速度
        float vy;

        //x轴加速度
        float ax;
        //y轴加速度
        float ay;
    }
}
