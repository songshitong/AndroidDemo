package sst.example.androiddemo.feature.widget.layout.repeatMeasure;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

public class MeasureLinearLayout extends LinearLayout {
    public String tag="";
    public MeasureLinearLayout(Context context) {
        super(context);
    }

    public MeasureLinearLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MeasureLinearLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public MeasureLinearLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        Log.d(tag,"onMeasure");
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        Log.d(tag,"onLayout");
    }
}
