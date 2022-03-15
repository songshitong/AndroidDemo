package sst.example.androiddemo.feature.widget.practice;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.animation.*;


//振荡曲线  Oscillation 振荡，波动，波荡
//阻尼正弦波
//TODO 抛物线公式

// 1 小球在线上下移
// 2 小球在线上 上移
// 3 小球脱离绳子做自由落体运行

//重力公式 h=v*t+(1/2)*g*t*t     v = v初始+ g*t
//实现runnable接口，为了在子线程刷新UI
public class OscillationView extends SurfaceView implements SurfaceHolder.Callback,Runnable {

    public static final int LOADING_STATE_DOWN = 1;
    public static final int LOADING_STATE_UP = 2;
    public static final int LOADING_STATE_FREE = 3;
    private int loadingState= LOADING_STATE_DOWN;
    Canvas mCanvas;
    Paint mPaint;
    SurfaceHolder surfaceHolder;
    Path mPath;
    //小球颜色
    int ballColor;
    int lineColor;
    //线宽
    int strokeWidth;
    //线长
    int lineWidth;

    float downDistance;
    float upDistance;
    float freeDistanc;

    ValueAnimator downControl;
    ValueAnimator upControl;
    ValueAnimator freeControl;
    AnimatorSet  animatorSet;

     //false 动画是否在执行
    boolean isRunning;
    //false 动画不再展示
    boolean isAnimationShowing;

    public OscillationView(Context context) {
        this(context,null);
    }

    public OscillationView(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public OscillationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    void init(){
       ballColor = Color.GRAY;
        lineColor = Color.GRAY;
        lineWidth = 200;
        strokeWidth = 2;
      mPaint = new Paint();
      mPaint.setAntiAlias(true);
      mPaint.setStrokeWidth(strokeWidth);
      mPath = new Path();
      surfaceHolder = getHolder();
      surfaceHolder.addCallback(this);

      //初始化动画控制
        downControl = ValueAnimator.ofFloat(0,1);
        downControl.setDuration(500);
        downControl.setInterpolator(new DecelerateInterpolator());
        downControl.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
              downDistance = 50*((float)animation.getAnimatedValue());
            }
        });
        downControl.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
            }

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                loadingState = LOADING_STATE_DOWN;
                isAnimationShowing = true;
            }
        });

        upControl = ValueAnimator.ofFloat(0,1);
        upControl.setDuration(500);
        upControl.setInterpolator(new ShockInterpolator());
        upControl.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                upDistance = 50*((float)animation.getAnimatedValue());
                if(upDistance>=50&&!freeControl.isStarted()&&!freeControl.isRunning()){
                    freeControl.start();
                }
            }
        });
        upControl.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
            }

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                loadingState = LOADING_STATE_UP;
            }
        });
        //50=v*t+0.5*g*t*t  v=0;t=根号10   绳子上部分，一上一下 2*根号10
        freeControl = ValueAnimator.ofFloat(0, (float) (2*Math.sqrt(10)));
        //绳子上面的变化比绳子下面慢点，上下距离都是50
        freeControl.setDuration(600);
        freeControl.setInterpolator(new LinearInterpolator());
        freeControl.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float t = (float) animation.getAnimatedValue();
                float g = 10;
                //速度=时间*加速度
                float v = (float) (g*Math.sqrt(10));
                freeDistanc = (float) (v*t-0.5*g*t*t);
            }
        });
        freeControl.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                isAnimationShowing = false;
                startAllAnimation();
            }

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                loadingState = LOADING_STATE_FREE;
            }
        });

        animatorSet = new AnimatorSet();
        animatorSet.play(downControl).before(upControl);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        isRunning = true;
        new Thread(this).start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
       surfaceHolder = holder;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
     //资源回收
        surfaceHolder=null;
        isRunning = false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    @Override
    public void run() {
       while(isRunning){
           drawView();
           try {
               Thread.sleep(16);
           } catch (InterruptedException e) {
               e.printStackTrace();
           }
       }
    }

    private void drawView() {
        try {
           if(surfaceHolder != null){
               mCanvas = surfaceHolder.lockCanvas();

               //清屏
               mCanvas.drawColor(Color.WHITE);
               //绘制处理
               mPath.reset();
               mPath.moveTo(getWidth()/2-lineWidth/2,getHeight()/2);
               if(loadingState == LOADING_STATE_DOWN){
                 //下降时，小球还在绳子上
                   //贝塞尔  b=(1-t)*(1-t)P0+2*t*(1-t)*P1+t*t*P2  t在0和1之间，p0和p2是起始点，p1是控制点，b是曲线上的一点
                   //t=0.5   growX在两点之间，t也就是0.5，控制点在中间，控制点x不变，y改变就是绳子的运动
                   // cp[1].x=(cp[0].x+cp[2].x)/2   连线的中点
                   // float c0= (1-t)*(1-t) =0.25
                   // float c1 = 2*t*(1-t)  =0.5
                   // float c2 = t*t        =0.25
                   // growX = c0*cp[0].x+c1*cp[1].x+c2*cp[2].x;
                   // growY = c0*cp[0].y+c1*cp[1].y+c2*cp[2].y ;   //此时 cp[0].y=cp[2].y
                   //cp[1].y= 2*(growY-0.5*cp[0].y)=2*growY-cp[0].y;
                   //向下的偏移 -growY= c0*cp[0].y+c1*cp[1].y+c2*cp[2].y
                     //  cp[1].y=-2*growY-cp[0].y  此时y轴正向向上
                     //  cp[1].y= 2*downDistance+(getWidth/2-lineWidth/2) 此时y轴向下
                   // growY的绝对值是downDistance ,起始y是getHeight()/2
                   //相对于起点的偏移量
                   mPath.rQuadTo(lineWidth/2,2*downDistance,lineWidth,0);
                   mPaint.setColor(lineColor);
                   mPaint.setStyle(Paint.Style.STROKE);
                   mCanvas.drawPath(mPath,mPaint);
                   mPaint.setColor(ballColor);
                   mPaint.setStyle(Paint.Style.FILL);
                   //小球与绳子相接，减去小球半径接着减去绳子一半宽度
                   mCanvas.drawCircle(getWidth()/2,getHeight()/2+downDistance-10-strokeWidth/2,10,mPaint);
               }else if(loadingState == LOADING_STATE_UP){
                 //上升 小球在绳子上
                   mPath.rQuadTo(lineWidth/2,2*(50-upDistance),lineWidth,0);
                   mPaint.setColor(lineColor);
                   mPaint.setStyle(Paint.Style.STROKE);
                   mCanvas.drawPath(mPath,mPaint);
                   mPaint.setColor(ballColor);
                   mPaint.setStyle(Paint.Style.FILL);
                   mCanvas.drawCircle(getWidth()/2,getHeight()/2+(50-upDistance)-10-strokeWidth/2,10,mPaint);
               }else if(loadingState == LOADING_STATE_FREE){
                   mPath.rQuadTo(lineWidth/2,2*(50-upDistance),lineWidth,0);
                   mPaint.setColor(lineColor);
                   mPaint.setStyle(Paint.Style.STROKE);
                   mCanvas.drawPath(mPath,mPaint);

                   //自由落体
                   mPaint.setColor(ballColor);
                   mPaint.setStyle(Paint.Style.FILL);
                   mCanvas.drawCircle(getWidth()/2,getHeight()/2-freeDistanc-10-strokeWidth/2,10,mPaint);


               }
               //画小球
               mPaint.setColor(ballColor);
               mPaint.setStyle(Paint.Style.FILL);
               mCanvas.drawCircle(getWidth()/2-lineWidth/2,getHeight()/2,10,mPaint);
               mCanvas.drawCircle(getWidth()/2+lineWidth/2,getHeight()/2,10,mPaint);

           }
        }catch (Exception e){
           e.printStackTrace();
        }finally {
          //提交
            if(mCanvas != null){
                surfaceHolder.unlockCanvasAndPost(mCanvas);
            }
        }
    }

    public void startAllAnimation(){
     if(isAnimationShowing){
         return;
     }
     if(animatorSet.isRunning()){
         animatorSet.end();
         animatorSet.cancel();
     }

     animatorSet.start();

    }


    class  ShockInterpolator implements Interpolator{

        @Override
        public float getInterpolation(float input) {
            return (float) (1-Math.exp(-3*input)*Math.cos(10*input));
        }
    }
}
