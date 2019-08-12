package sst.example.androiddemo.feature.util;

import android.graphics.Paint;

/**
 * 获得paint的distance
 */
public class PaintUtils {

    /**
     * 文字的居中效果 (centerX , centerY + PaintUtils.getBaseline(paint))
     * 设置文字的对齐  paint.setTextAlign(Paint.Align.RIGHT);
     * @param mPaint
     * @return
     */
    public static float getBaseline(Paint mPaint){
        Paint.FontMetrics fontMetrics = mPaint.getFontMetrics();
        float distance = (fontMetrics.descent - fontMetrics.ascent) / 2 - fontMetrics.descent;
        return distance;
    }
}
