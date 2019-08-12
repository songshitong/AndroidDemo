package sst.example.androiddemo.feature.video;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import sst.example.androiddemo.feature.util.PaintUtils;
import sst.example.androiddemo.feature.util.TimeUtil;


public class ClipProgressBar extends View {
    public static final String TAG = "VideoParserActivity";

    //进度最大值
    private float max =0;
    //当前进度
    private float progress =0;
    //true展示进度线
    private boolean isShowProgress = false;
    int lineWidth =6;

    Paint mPaint;
    //开始浮窗的path
    Path  popupStartPath;
    //结束浮窗的path
    Path  popupEndPath;
    //滑块的宽度
    float rectWidth = 60f;
    //左侧滑块的起始位置
    float start = 0;
    //右侧滑块的结束位置
    float end = 0;

    //两个滑块最小间距
    float minDistance =150;
    //点击滑块的额外响应区域
    float responseWidth = rectWidth/2;

    //滑块中线属性
    int rectLineWidth = 2;
    int lineTop = 40;

    //滑块悬浮小球
    int circleRadius = 10;
    //小球距离底部
    int cicleBottom = 15;

    //浮窗宽度
    int popupWidth = 150;
    //浮窗高度
    int popupHeight = 100;

    //等腰三角形的高度
    int triAngleHeight = 20;
    //浮窗角度
    int popupRadius =10;

    int popupTextSize = 36;

    public ClipProgressBar(Context context) {
        this(context, null);
    }

    public ClipProgressBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ClipProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAll();
    }

    private void initAll() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setTextSize(popupTextSize);

        popupEndPath = new Path();
        popupStartPath = new Path();

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        end = w;
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        //进度线
        if (isShowProgress){
            float startX = progress/max*getWidth();
            startX = Math.max(startX,start);
            mPaint.setColor(Color.WHITE);
            Log.d(TAG,"start value  "+start);
            //播放开始 进度莫名减少
            Log.d(TAG,"start x  value "+startX +" progress "+progress +" max "+max);
            canvas.drawRect(startX,0,startX+lineWidth,getHeight(),mPaint);
        }

        //画开始方形
        mPaint.setColor(Color.WHITE);
        canvas.drawRect(start, 0, start + rectWidth, getHeight(), mPaint);

        //画结束方形
        canvas.drawRect(end - rectWidth, 0, end, getHeight(), mPaint);

        //画蒙层
        mPaint.setColor(Color.BLACK);
        mPaint.setAlpha(200);
        //左侧到做滑块的蒙层
        canvas.drawRect(0,0,start,getHeight(),mPaint);
        //右侧到右滑块的蒙层
        canvas.drawRect(end,0,getWidth(),getHeight(),mPaint);

        //画开始结束滑块的竖线

        mPaint.setColor(Color.BLACK);
        float halfStart = (start + rectWidth+start)/2;
        canvas.drawRect(halfStart-rectLineWidth,lineTop,halfStart+rectLineWidth,getHeight()-lineTop,mPaint);
        float halfEnd = (end - rectWidth+end)/2;
        canvas.drawRect(halfEnd-rectLineWidth,lineTop,halfEnd+rectLineWidth,getHeight()-lineTop,mPaint);

        //画开始悬浮窗
        if (isStartMove){
            drawPopup(canvas, halfStart,Math.round(start/getWidth()*max));
        }
        //画结束悬浮窗
        if(isEndMove){
            drawPopup(canvas, halfEnd,Math.round(end/getWidth()*max));
        }
    }

    private void drawPopup(Canvas canvas, float halfStart,int progress) {
        mPaint.setColor(Color.WHITE);
        //画小球
        canvas.drawCircle(halfStart,-cicleBottom-circleRadius,circleRadius,mPaint);
        float popupLeft = halfStart-popupWidth/2;
        float popupRight = halfStart+popupWidth/2;
        float popupTop = -cicleBottom-circleRadius*2-triAngleHeight-popupHeight;
        float popupBottom = -cicleBottom-circleRadius*2-triAngleHeight;
        RectF rectf = new RectF(popupLeft,popupTop,popupRight,popupBottom);
        popupStartPath.rewind();
        popupStartPath.addRoundRect(rectf,popupRadius,popupRadius, Path.Direction.CCW);
        //画倒三角 可以分成两个高度为triAngleHeight的等腰直角三角形
        popupStartPath.moveTo(halfStart-triAngleHeight,popupBottom);
        popupStartPath.lineTo(halfStart,popupBottom+triAngleHeight);
        popupStartPath.lineTo(halfStart+triAngleHeight,popupBottom);
        canvas.drawPath(popupStartPath,mPaint);

        //画文字
        mPaint.setColor(Color.BLACK);
        mPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(TimeUtil.getMsFromMilSeconds(progress),(popupRight+popupLeft)/2,(popupBottom+popupTop)/2+ PaintUtils.getBaseline(mPaint),mPaint);
    }

    boolean isStartMove = false;
    boolean isEndMove = false;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        float downX = 0;
        float downY = 0;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                //判断点击的滑块是哪个
                RectF startRect = new RectF(start-responseWidth, 0, start + rectWidth+responseWidth, getHeight());
                RectF endRect = new RectF(end - rectWidth-responseWidth, 0, end+responseWidth, getHeight());
                downX = event.getX();
                downY = event.getY();
                if (startRect.contains(downX, downY)) {
                    isStartMove = true;
                } else if (endRect.contains(downX, downY)) {
                    isEndMove = true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                isShowProgress = false;
                //move 事件 downx一直为0
                float moveX = event.getX();
                if (isStartMove) {
                    //滑块中心
                    float lastStart = start;
                    start = moveX;
                    Log.d(TAG, "action move start " + start);
                    //两个滑块的间距不能小于minDistance+rectWidth
                    if (end - start < minDistance  ){
                        //现在的位置已经移动到start
                        //重置到上一次，不然下一次判断，滑块不能运动
                        start = lastStart;
                        return true;
                    }
                    if(null != clipProgressBarListener){
                        clipProgressBarListener.clipProgressStartChange(start/getWidth()*max,end/getWidth()*max);
                    }
                    invalidate();
                } else if (isEndMove) {
                    float lastEnd = end;
                    end = moveX;
                    Log.d(TAG, "action move end " + end);
                    //两个滑块的间距不能小于minDistance+rectWidth
                    if (end - start < minDistance  ){
                        end = lastEnd;
                        return true;
                    }
                    if(null != clipProgressBarListener){
                        clipProgressBarListener.clipProgressEndChange(end/getWidth()*max,start/getWidth()*max);
                    }
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                Log.d(TAG,"start up "+start/getWidth()*max+" start "+start);
                isStartMove = false;
                isEndMove = false;
                invalidate();
                break;
        }

        return true;

    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
//            Log.d(TAG,"dispatchTouchEvent action move");
        }
        return onTouchEvent(event);
    }

    public void setMax(float max) {
        this.max = max;
        invalidate();
    }

    public void setProgress(float progress) {
        this.progress = progress;
        invalidate();

    }

    public void setShowProgress(boolean showProgress) {
        isShowProgress = showProgress;
        invalidate();

    }

    ClipProgressBarListener clipProgressBarListener;

    public void setClipProgressBarListener(ClipProgressBarListener clipProgressBarListener) {
        this.clipProgressBarListener = clipProgressBarListener;
    }

    public  interface  ClipProgressBarListener{
       void clipProgressStartChange(float clipStart,float clipEnd);
       void clipProgressEndChange(float clipEnd,float clipStart);

    }
}
