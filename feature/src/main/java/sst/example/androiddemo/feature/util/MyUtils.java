package sst.example.androiddemo.feature.util;

import android.graphics.Paint;
import android.util.Log;

import java.util.logging.Logger;

public class MyUtils {
    public static final String TAG = "sst.example.androiddemo";
    public static float getBaseline(Paint mPaint){
        Paint.FontMetrics fontMetrics = mPaint.getFontMetrics();
        float distance = (fontMetrics.descent - fontMetrics.ascent) / 2 - fontMetrics.descent;
        return distance;
    }

    public static void log(String msg){
        Log.d(TAG,msg);
    }
    //todo activity不显示后  bitmap 内存优化
    //todo surfaceview 自定义view  opengl
    //http://weishu.me/2016/12/23/dive-into-android-optimize-vm-heap/
}
