package sst.example.androiddemo.feature.activity.scrollNested;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

public  class  ExternalScrollView extends ScrollView {


    public ExternalScrollView(Context context) {
        super(context);
    }

    public ExternalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ExternalScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onInterceptHoverEvent(MotionEvent event) {
        if(!ScrollNestedActivity.handleNested){
            return  super.onInterceptHoverEvent(event);
        }
        boolean intercepted =true;
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                intercepted = false;
                super.onInterceptTouchEvent(event);
                break;
        }
        return intercepted;
    }
}
