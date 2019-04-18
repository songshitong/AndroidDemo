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
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import androidx.annotation.Nullable;
import sst.example.androiddemo.feature.R;
import sst.example.androiddemo.feature.graphics.BitmapActivity;

import java.util.ArrayList;
import java.util.List;
//1    8个小球围绕圆心旋球
//2    向外扩散后向圆心内聚集
//3    聚集到圆心后，水波纹扩散出底部内容

public class LoadingView extends View {
    private static final String TAG = "LoadingView";
    List<Integer> colors ;
    //小球半径5
    float itemRadius = 15;
    //loading圆的半径
    float radius = 100;
    Paint mPaint = new Paint();
    float centerX;
    float centerY;
    LoadingState loadingState;
    Bitmap bitmap;
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
        bitmap = BitmapActivity.getBitmap(getContext(), R.drawable.dog);
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
        loadingState.setStateListener(new StateListener() {
            @Override
            public void onDone(LoadingState loadingState) {
                Log.d(TAG," loadingState "+loadingState.toString());
                if(loadingState.getClass().equals(RotateState.class) ){
                    Log.d(TAG,"RotateState is done ========> ");
                    LoadingView.this.loadingState = new MergeState();
                    //重新设置监听
                    LoadingView.this.loadingState.setStateListener(new StateListener() {
                        @Override
                        public void onDone(LoadingState loadingState) {
                            Log.d(TAG,"MergeState is done ========> ");
                            LoadingView.this.loadingState = new ExpandState();
                        }
                    });
                }
            }
        });
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
        centerX = w >> 1;
        centerY = h >> 1;
    }




    interface StateListener{
        void onDone(LoadingState loadingState);
    }
    abstract  class  LoadingState {
        StateListener stateListener;
        public abstract void draw(Canvas canvas);

        public void setStateListener(StateListener stateListener){
            this.stateListener = stateListener;
        }
    }


    //旋转状态
    class  RotateState extends  LoadingState{
        ValueAnimator valueAnimator;
        float current ;
        public RotateState() {
            valueAnimator =ValueAnimator.ofFloat(0,1);
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    current = (float) animation.getAnimatedValue();
                    invalidate();
                }
            });
            valueAnimator.setDuration(2000);
            valueAnimator.setRepeatCount(2);
            valueAnimator.setInterpolator(new LinearInterpolator());
            valueAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    if(null != stateListener){
                        stateListener.onDone(RotateState.this);
                    }
                }
            });
            valueAnimator.start();
        }

        @Override
        public void draw(Canvas canvas) {

            drawCircles(canvas,current,radius);
        }

    }


    //扩散聚合
    class  MergeState extends LoadingState{
        ValueAnimator valueAnimator;
        float currentRadius;
        public MergeState() {
            //从小球半径到大圆半径
            valueAnimator = ValueAnimator.ofFloat(itemRadius,radius);
            valueAnimator.setDuration(2000);
            //先反向运动，后正向        15f张力，数值越大，速度越快
            valueAnimator.setInterpolator(new OvershootInterpolator(15f));
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    currentRadius = (float) animation.getAnimatedValue();
//                    Log.d(TAG,"currentRadius "+currentRadius);
                    invalidate();

                }
            });
            valueAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    if(null != stateListener){
                        stateListener.onDone(MergeState.this);
                    }
                }
            });
            //从大圆变化到小圆
            valueAnimator.reverse();
        }

        @Override
        public void draw(Canvas canvas) {
            //current 角度不发生变化
            drawCircles(canvas,0,currentRadius);
        }
    }
    private void drawCircles(Canvas canvas,float current,float currentRadius) {
        //将一周平均分为几分
        float rotateAngle = (float) (Math.PI*2/colors.size());
        for (int i = 0; i < colors.size(); i++) {
            mPaint.setColor(colors.get(i));
//                Log.d(TAG,"centerX "+centerX+" centerY "+centerY);
            //小球的坐标 x= centerX+r*cos     y=centerY+r*sin
            //旋转动画 在每个角度的基础上加上增加的角度即可
            float itemAngle = (float) (rotateAngle*i+current*Math.PI*2);
            //扩散动画 radius 发生变化
            canvas.drawCircle((float) (centerX+Math.cos(itemAngle)*currentRadius), (float) (centerY+Math.sin(itemAngle)*currentRadius),itemRadius,mPaint);
        }
    }


    //扩散水波纹
    class  ExpandState extends LoadingState{
        ValueAnimator valueAnimator;
        //对角线
        float diagonal;
        float holeRadius;
        public ExpandState() {
            diagonal = (float) Math.hypot(centerX,centerY);
            valueAnimator= ValueAnimator.ofFloat(diagonal,itemRadius);
            valueAnimator.setDuration(2000);
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    holeRadius = (float) animation.getAnimatedValue();
                    invalidate();
                }
            });
            valueAnimator.setInterpolator(new LinearInterpolator());
            valueAnimator.reverse();
        }

        @Override
        public void draw(Canvas canvas) {
              canvas.drawBitmap(bitmap,centerX-bitmap.getWidth()/2,centerY-bitmap.getHeight()/2,mPaint);

              //抠洞法 空心圆代替水波,半径为手机对角线   洞越来越大    圆环的半径是内圆半径+环宽度的一半
              float realStroke = diagonal-holeRadius;
              float currentRadius = realStroke/2+holeRadius;
              mPaint.setStrokeWidth(realStroke);
              mPaint.setStyle(Paint.Style.STROKE);
              mPaint.setColor(Color.WHITE);
//              Log.d(TAG,"currentRadius "+currentRadius+" realStroke "+realStroke);
              canvas.drawCircle(centerX,centerY,currentRadius,mPaint);

              //todo 水波纹的多种画法
        }
    }
}

