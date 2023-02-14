package sst.example.androiddemo.feature.widget.practice;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.Choreographer;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import sst.example.androiddemo.feature.R;
import sst.example.androiddemo.feature.graphics.BitmapActivity;

import java.util.ArrayList;
import java.util.List;

//自定义粒子爆炸效果
public class ParticleView extends SurfaceView implements SurfaceHolder.Callback,Runnable{
    //粒子集合
    List<Ball> balls = new ArrayList<>();
    //默认小球半径 default r
    static final int dr = 5;
    Paint mPaint = new Paint();
    boolean isShowBitmap = true;

    //false 动画是否在执行
    boolean isRunning;
    Thread thread;
    SurfaceHolder surfaceHolder;
    float freshRate = 60;
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
        freshRate = ((WindowManager) getContext()
            .getSystemService(Context.WINDOW_SERVICE))
            .getDefaultDisplay()
            .getRefreshRate();
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
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

    }

    @Override protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        isRunning = false;
        if(null != thread){
            thread.interrupt();
        }
    }

    private void initBall(Bitmap bitmap, int i, int j, Ball ball) {
        //设置每个小球的dr 即每个像素放大dr倍，整张图片放大dr*dr倍数
        ball.r = dr;
        ball.x = i*dr+dr;
        ball.y = j*dr+dr;
        //获取像素的argb  会多次调用jni
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

    private   void updateBall() {
        for (Ball ball : balls) {
           changeBall(ball);
        }
    }

     static  void changeBall(Ball ball) {
        //距离=速度*t      0-1的变化即使速度累加
        ball.x += ball.vx;
        ball.y += ball.vy;

        // 速度= 加速度的累加
        ball.vx += ball.ax;
        ball.vy += ball.ay;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(MotionEvent.ACTION_DOWN == event.getAction()){
            //截断点击
            isShowBitmap = false;
            return  true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);


    }

    @Override public void surfaceCreated(@NonNull SurfaceHolder holder) {
        isRunning = true;
        (thread = new Thread(this)).start();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        surfaceHolder = holder;
    }

    @Override public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        //资源回收
        surfaceHolder=null;
        isRunning = false;
    }

    @Override public void run() {

        try {
            while(isRunning){
                drawView();
                try {
                    Thread.sleep((long) (1000/freshRate));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    private void drawView() {
         Canvas mCanvas = null;
        try {
            if(surfaceHolder != null){
                mCanvas = surfaceHolder.lockCanvas();
                mCanvas.drawColor(Color.WHITE);
                mCanvas.translate(300,300);
                if(!isShowBitmap){
                    updateBall();//子线程更新数据
                    //用小球组合成图片     每个像素的颜色值拼接起来就是图片   图片就是不同颜色的像素值
                    for (Ball ball : balls) {
                        mPaint.setColor(ball.color);
                        mCanvas.drawCircle(ball.x,ball.y,ball.r,mPaint);
                    }
                }else {
                    mCanvas.drawBitmap(bitmap,0,0,mPaint);
                }
                mCanvas.restore();
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(null != mCanvas && null!=surfaceHolder){
                surfaceHolder.unlockCanvasAndPost(mCanvas); //提交更新
            }
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
