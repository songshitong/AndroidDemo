


MarkerView绘制
com/github/mikephil/charting/components/MarkerView.java
```
public class MarkerView extends RelativeLayout implements IMarker {
   public MarkerView(Context context, int layoutResource) {
        super(context);
        setupLayoutResource(layoutResource);
    }

    //解析布局xml，并执行测量和布局
    private void setupLayoutResource(int layoutResource) {
        View inflated = LayoutInflater.from(getContext()).inflate(layoutResource, this);
        inflated.setLayoutParams(new LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
        inflated.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        inflated.layout(0, 0, inflated.getMeasuredWidth(), inflated.getMeasuredHeight());
    }
 

  //测量和布局
  public void refreshContent(Entry e, Highlight highlight) {
        measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        layout(0, 0, getMeasuredWidth(), getMeasuredHeight());
    }

  //绘制
  public void draw(Canvas canvas, float posX, float posY) {
        MPPointF offset = getOffsetForDrawingAtPoint(posX, posY);
        int saveId = canvas.save();
        //markerView在chart中的偏移位置
        canvas.translate(posX + offset.x, posY + offset.y);
        draw(canvas);
        canvas.restoreToCount(saveId);
    }
}
```
makerView的绘制
com/github/mikephil/charting/charts/BarLineChartBase.java
```
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        ...
        drawMarkers(canvas);
        ...
    }
    
 //
 protected void drawMarkers(Canvas canvas) {
        //没有marker，marker禁止，没有高亮不进行绘制
         if (mMarker == null || !isDrawMarkersEnabled() || !valuesToHighlight())
            return;
        for (int i = 0; i < mIndicesToHighlight.length; i++) {
            ...
            float[] pos = getMarkerPosition(highlight); //根据高亮条目确定markerView位置
            ...
            //每次绘制调用measure,layout,draw
            mMarker.refreshContent(e, highlight);
            mMarker.draw(canvas, pos[0], pos[1]);
        }
    }    
```