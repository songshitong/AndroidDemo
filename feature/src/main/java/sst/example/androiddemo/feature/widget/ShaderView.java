package sst.example.androiddemo.feature.widget;

import android.content.Context;
import android.graphics.Canvas;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

public class ShaderView extends View {
    public ShaderView(Context context) {
        this(context,null);
    }

    public ShaderView(Context context,  @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public ShaderView(Context context,  @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr,0);
    }

    public ShaderView(Context context,  @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }
}
