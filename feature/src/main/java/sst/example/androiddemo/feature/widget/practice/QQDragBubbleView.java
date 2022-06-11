package sst.example.androiddemo.feature.widget.practice;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.PointFEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import androidx.annotation.Nullable;
import sst.example.androiddemo.feature.R;
import sst.example.androiddemo.feature.graphics.BitmapActivity;

//模仿qq的消息气泡
//1 静止状态      一个气泡+消息数据
//2 连接状态      原来位置气泡+拖拽位置气泡+贝塞尔      原来位置气泡变小
//3 分离状态      移动气泡+消息数据
//4 消失状态      爆炸效果
public class QQDragBubbleView extends View {
    //状态
    final int BUBBLE_STATE_FIXED = 1;
    final int BUBBLE_STATE_CONNECT = 2;
    final int BUBBLE_STATE_APART = 3;
    final int BUBBLE_STATE_DISSMIS = 4;
    int bubbleState = BUBBLE_STATE_FIXED;

    //    初始气泡半径
    float bubbleRadius = 50;

    //移动气泡
    PointF bubbleMovableCenter = new PointF();
    float bubbleMovieRadius = bubbleRadius;
    Paint bubblePaint = new Paint();
    //固定气泡
    PointF bubbleFiexedCenter = new PointF();
    //两个气泡起始半径相同
    float bubbleFiexedRadius = bubbleRadius;

    //两个气泡的距离
    float dist;

    //文字
    Paint txtPaint = new Paint();
    String text = "22";
    Rect textRect = new Rect();

    //拖动的最大距离
    final float MAX_DIST = bubbleFiexedRadius * 8;
    //点击的有效距离,方便点击
    final float MOIVE_OFFSET = MAX_DIST / 4;

    //贝塞尔path
    Path bezierPath = new Path();

    //爆炸画笔
    Paint burstPaint = new Paint();
    //爆炸的外框
    Rect burstRect = new Rect();
    //图片index
    int burstIndex =0;
    Bitmap[] burstArray;

    public QQDragBubbleView(Context context) {
        this(context, null);
    }

    public QQDragBubbleView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public QQDragBubbleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        bubblePaint.setColor(Color.RED);
        bubblePaint.setAntiAlias(true);

        txtPaint.setColor(Color.WHITE);
        txtPaint.setTextSize(45);
        //获取文字的大小外框
        txtPaint.getTextBounds(text, 0, text.length(), textRect);
        txtPaint.setAntiAlias(true);

        burstPaint.setAntiAlias(true);
        bubblePaint.setFilterBitmap(true);

        burstArray = new Bitmap[]{
                BitmapActivity.getBitmap(getContext(), R.drawable.burst_1),
                BitmapActivity.getBitmap(getContext(),R.drawable.burst_2),
                BitmapActivity.getBitmap(getContext(),R.drawable.burst_3),
                BitmapActivity.getBitmap(getContext(),R.drawable.burst_4),
                BitmapActivity.getBitmap(getContext(),R.drawable.burst_5),

        };

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        bubbleFiexedCenter.set(w / 2, h / 2);
        bubbleMovableCenter.set(w / 2, h / 2);
    }

    @Override
    protected void onDraw(Canvas canvas) {
//        https://juejin.cn/post/6943590136424693767#heading-6  可以参考水滴效果
        super.onDraw(canvas);
        if (bubbleState == BUBBLE_STATE_CONNECT) {
            //固定小球
            canvas.drawCircle(bubbleFiexedCenter.x, bubbleFiexedCenter.y, bubbleFiexedRadius, bubblePaint);
            //贝塞尔曲线
            //控制点 两小球中心连线的中点
            int anchorX = (int) ((bubbleFiexedCenter.x + bubbleMovableCenter.x) / 2);
            int anchorY = (int) ((bubbleFiexedCenter.y + bubbleFiexedCenter.y) / 2);

            //theta 角度符号的英文
            float sinTheta = (bubbleMovableCenter.y - bubbleFiexedCenter.y) / dist;
            float cosTheta = (bubbleMovableCenter.x - bubbleFiexedCenter.x) / dist;

            //B的坐标
            float bubbleMoveStartX = bubbleMovableCenter.x + bubbleMovieRadius * sinTheta;
            float bubbleMoveStartY = bubbleMovableCenter.y - bubbleMovieRadius * cosTheta;

            //A
            float bubbleFixEndX = bubbleFiexedCenter.x + bubbleFiexedRadius * sinTheta;
            float bubbleFixEndY = bubbleFiexedCenter.y - bubbleFiexedRadius * cosTheta;

            //D
            float bubbleFixStartX = bubbleFiexedCenter.x - bubbleFiexedRadius * sinTheta;
            float bubbleFixStartY = bubbleFiexedCenter.y + bubbleFiexedRadius * cosTheta;

            //C
            float bubbleMoveEndX = bubbleMovableCenter.x - bubbleMovieRadius * sinTheta;
            float bubbleMoveEndY = bubbleMovableCenter.y + bubbleMovieRadius * cosTheta;
            //每次重新绘制
            bezierPath.reset();
            //移动到D
            bezierPath.moveTo(bubbleFixStartX, bubbleFixStartY);
            //通过控制点绘制二阶贝塞尔到C
            bezierPath.quadTo(anchorX, anchorY, bubbleMoveEndX, bubbleMoveEndY);
            //移动到B
            bezierPath.lineTo(bubbleMoveStartX, bubbleMoveStartY);
            //通过控制点绘制二阶贝塞尔到A
            bezierPath.quadTo(anchorX, anchorY, bubbleFixEndX, bubbleFixEndY);
            bezierPath.close();
            canvas.drawPath(bezierPath, bubblePaint);
        }

        if (bubbleState != BUBBLE_STATE_DISSMIS) {
            //绘制移动小球
            canvas.drawCircle(bubbleMovableCenter.x, bubbleMovableCenter.y, bubbleMovieRadius, bubblePaint);
            //绘制文字   没有完全居中
            canvas.drawText(text, bubbleMovableCenter.x - textRect.width() / 2, bubbleMovableCenter.y + textRect.height() / 2, txtPaint);
        }

        if(bubbleState == BUBBLE_STATE_DISSMIS && burstIndex < burstArray.length){
            burstRect.set(
                    (int)(bubbleMovableCenter.x-bubbleMovieRadius),
                    (int)(bubbleMovableCenter.y-bubbleMovieRadius),
                    (int)(bubbleMovableCenter.x+bubbleMovieRadius),
                    (int)(bubbleMovableCenter.y+bubbleMovieRadius)
            );
            canvas.drawBitmap(burstArray[burstIndex],null,burstRect,burstPaint);
        }


    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (bubbleState != BUBBLE_STATE_DISSMIS) {
                    //两个小球的中心点距离为 x平方+y平方的开方    x,y为距离
                    dist = (float) Math.hypot(event.getX() - bubbleFiexedCenter.x, event.getY() - bubbleFiexedCenter.y);
                    //改变气泡状态
                    if (dist < bubbleFiexedRadius + MOIVE_OFFSET) {
                        //当前为点击的有效区域，否则点击在气泡外面
                        //MOIVE_OFFSET 为扩展点击区域
                        bubbleState = BUBBLE_STATE_CONNECT;
                    } else {
                        bubbleState = BUBBLE_STATE_FIXED;
                    }
                }

                break;
            case MotionEvent.ACTION_MOVE:
                if (bubbleState != BUBBLE_STATE_FIXED) {
                    dist = (float) Math.hypot(event.getX() - bubbleFiexedCenter.x, event.getY() - bubbleFiexedCenter.y);
                    bubbleMovableCenter.set(event.getX(), event.getY());
                    //连接状态
                    if (bubbleState == BUBBLE_STATE_CONNECT) {
                        if (dist < MAX_DIST - MOIVE_OFFSET) { //拖动的有效距离
                            //根据拖动的范围调整不动气泡的大小
                            bubbleFiexedRadius = bubbleRadius - dist / 8;   //正好在分开状态时固定小球减少到0
                        } else {
                            //拖动距离大于最大范围时，变为分离状态
                            bubbleState = BUBBLE_STATE_APART;
                        }
                    }
                    invalidate();
                }

                break;
            case MotionEvent.ACTION_UP:
                if (bubbleState == BUBBLE_STATE_CONNECT) {
                    //气泡回弹，有个橡皮筋效果
                    bubbleResetAnim();
                } else if (bubbleState == BUBBLE_STATE_APART) {
                    //移动距离小于指定范围，气泡回弹
                    if (dist < 2 * bubbleRadius) {
                        bubbleResetAnim();
                    } else {
                        //爆炸效果
                        bubbleBurstAnim();
                    }
                }
                break;

        }


        return true;
    }

    //爆照动画  依次播放爆炸照片，模拟爆炸效果
    private void bubbleBurstAnim() {
       bubbleState = BUBBLE_STATE_DISSMIS;
       ValueAnimator valueAnimator = ValueAnimator.ofInt(0,burstArray.length);
       valueAnimator.setDuration(500);
       valueAnimator.setInterpolator(new LinearInterpolator());
       valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
           @Override
           public void onAnimationUpdate(ValueAnimator animation) {
               burstIndex = (int) animation.getAnimatedValue();
               invalidate();
           }
       });
       valueAnimator.start();
    }

    private void bubbleResetAnim() {
        //点估值器       将点转为PointF类型，更精确
        ValueAnimator valueAnimator = ValueAnimator.ofObject(new PointFEvaluator(), new PointF(bubbleMovableCenter.x, bubbleMovableCenter.y), new PointF(bubbleFiexedCenter.x, bubbleFiexedCenter.y));
        valueAnimator.setDuration(200);
        valueAnimator.setInterpolator(new OvershootInterpolator(5f));
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                bubbleMovableCenter = (PointF) animation.getAnimatedValue();
                invalidate();
            }
        });
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                bubbleState = BUBBLE_STATE_FIXED;
            }
        });
        valueAnimator.start();
    }
}
