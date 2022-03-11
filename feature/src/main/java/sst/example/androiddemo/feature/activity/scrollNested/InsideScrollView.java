package sst.example.androiddemo.feature.activity.scrollNested;

import static java.lang.Math.abs;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

public  class  InsideScrollView extends ScrollView {


    private float lastX = 0f;

    private float lastY = 0f;

    public InsideScrollView(Context context) {
        super(context);
    }

    public InsideScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public InsideScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        if(!ScrollNestedActivity.handleNested){
            return  super.dispatchTouchEvent(motionEvent);
        }
        float x = motionEvent.getX();
        float y = motionEvent.getY();
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN : {
                //子view处理DOWN及后续的移动事件
                getParent().requestDisallowInterceptTouchEvent(true);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                float deltaX = x - lastX;
                float deltaY = y - lastY;
                if (abs(deltaX) < abs(deltaY)) { //上下滑动的操作
                    if (deltaY > 0) { //向下滑动
                        if (getScrollY() == 0) { //滑动到顶部了
                            //后续的MOVE事件由父View处理
                            getParent().requestDisallowInterceptTouchEvent(false);
                        }
                    } else { //向上滑动
                        if (getHeight() + getScrollY() >= computeVerticalScrollRange()) { //滑动到底部了
                            //后续的MOVE事件由父View处理
                            getParent().requestDisallowInterceptTouchEvent(false);
                        }
                    }
                }
                break;
            }

        }
        lastX = x;
        lastY = y;
        return super.dispatchTouchEvent(motionEvent);
    }


}