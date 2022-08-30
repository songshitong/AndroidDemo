
以饼图为例
com/github/mikephil/charting/charts/PieRadarChartBase.java
```
public abstract class PieRadarChartBase<T extends ChartData<? extends IDataSet<? extends Entry>>>
        extends Chart<T>{
   protected void init() {
        super.init();
        //声明在父类chart中，类型为ChartTouchListener   
        mChartTouchListener = new PieRadarChartTouchListener(this);
    } 
 
    //触摸事件交由ChartTouchListener处理
    public boolean onTouchEvent(MotionEvent event) {
        if (mTouchEnabled && mChartTouchListener != null)
            return mChartTouchListener.onTouch(this, event);
        else
            return super.onTouchEvent(event);
    }      
}
```
PieRadarChartTouchListener相关
```
com/github/mikephil/charting/listener/ChartTouchListener.java
public abstract class ChartTouchListener<T extends Chart<?>> extends GestureDetector.SimpleOnGestureListener implements View.OnTouchListener {
  protected GestureDetector mGestureDetector;
    public ChartTouchListener(T chart) {
        this.mChart = chart;
        mGestureDetector = new GestureDetector(chart.getContext(), this);
    }
}
public class PieRadarChartTouchListener extends ChartTouchListener<PieRadarChartBase<?>> {
 public boolean onTouch(View v, MotionEvent event) {
        //GestureDetector处理单击，长按等事件
        if (mGestureDetector.onTouchEvent(event))
            return true;

        if (mChart.isRotationEnabled()) {
            float x = event.getX();
            float y = event.getY();
            switch (event.getAction()) {
                ...
                case MotionEvent.ACTION_MOVE:
                    //处理饼图的旋转
                    if (mChart.isDragDecelerationEnabled())
                        sampleVelocity(x, y);
                    if (mTouchMode == NONE
                            && distance(x, mTouchStartPoint.x, y, mTouchStartPoint.y)
                            > Utils.convertDpToPixel(8f)) {
                        mLastGesture = ChartGesture.ROTATE;
                        //设置当前为旋转模式
                        mTouchMode = ROTATE;
                        mChart.disableScroll();
                    } else if (mTouchMode == ROTATE) {
                        updateGestureRotation(x, y);
                        mChart.invalidate();
                    }
                    endAction(event);
                    break;
                case MotionEvent.ACTION_UP:
                 ...
                        //计算速度
                        mDecelerationAngularVelocity = calculateVelocity();
                        if (mDecelerationAngularVelocity != 0.f) {
                            mDecelerationLastTime = AnimationUtils.currentAnimationTimeMillis();
                            //触发重绘
                            Utils.postInvalidateOnAnimation(mChart); // This causes computeScroll to fire, recommended for this by Google
                        }
                 。。。
            }
        }
        return true;
    }
 
 //单击回调   
 public boolean onSingleTapUp(MotionEvent e) {
        。。。
        OnChartGestureListener l = mChart.getOnChartGestureListener();
        if (l != null) {
            l.onChartSingleTapped(e);
        }
        if(!mChart.isHighlightPerTapEnabled()) {
            return false;
        }
        //根据点击的位置获取Highlight
        Highlight high = mChart.getHighlightByTouchPoint(e.getX(), e.getY());
        performHighlight(high, e);
        return true;
    }
    
}
```
fling效果的实现
```
   //手势move时设置旋转角度
   public void updateGestureRotation(float x, float y) {
        mChart.setRotationAngle(mChart.getAngleForPoint(x, y) - mStartAngle);
    }

com/github/mikephil/charting/charts/PieRadarChartBase.java
  public void setRotationAngle(float angle) {
        mRawRotationAngle = angle;
        mRotationAngle = Utils.getNormalizedAngle(mRawRotationAngle);
    }  
 //手势Up时  计算角度的速度，自己采样
  private float calculateVelocity() {
        if (_velocitySamples.isEmpty())
            return 0.f;

        AngularVelocitySample firstSample = _velocitySamples.get(0);
        AngularVelocitySample lastSample = _velocitySamples.get(_velocitySamples.size() - 1);

        // Look for a sample that's closest to the latest sample, but not the same, so we can deduce the direction
        AngularVelocitySample beforeLastSample = firstSample;
        for (int i = _velocitySamples.size() - 1; i >= 0; i--) {
            beforeLastSample = _velocitySamples.get(i);
            if (beforeLastSample.angle != lastSample.angle) {
                break;
            }
        }

        // Calculate the sampling time
        float timeDelta = (lastSample.time - firstSample.time) / 1000.f;
        if (timeDelta == 0.f) {
            timeDelta = 0.1f;
        }

        // Calculate clockwise/ccw by choosing two values that should be closest to each other,
        // so if the angles are two far from each other we know they are inverted "for sure"
        boolean clockwise = lastSample.angle >= beforeLastSample.angle;
        if (Math.abs(lastSample.angle - beforeLastSample.angle) > 270.0) {
            clockwise = !clockwise;
        }

        // Now if the "gesture" is over a too big of an angle - then we know the angles are inverted, and we need to move them closer to each other from both sides of the 360.0 wrapping point
        if (lastSample.angle - firstSample.angle > 180.0) {
            firstSample.angle += 360.0;
        } else if (firstSample.angle - lastSample.angle > 180.0) {
            lastSample.angle += 360.0;
        }

        // The velocity
        float velocity = Math.abs((lastSample.angle - firstSample.angle) / timeDelta);

        // Direction?
        if (!clockwise) {
            velocity = -velocity;
        }

        return velocity;
    }
 //postInvalidateOnAnimation触发重绘，进而调用computeScroll
 com/github/mikephil/charting/charts/PieRadarChartBase.java
    public void computeScroll() {
        if (mChartTouchListener instanceof PieRadarChartTouchListener)
            ((PieRadarChartTouchListener) mChartTouchListener).computeScroll();
    }
  public void computeScroll() {
        //速度为0停止
        if (mDecelerationAngularVelocity == 0.f)
            return; // There's no deceleration in progress

        final long currentTime = AnimationUtils.currentAnimationTimeMillis();
        mDecelerationAngularVelocity *= mChart.getDragDecelerationFrictionCoef();
        final float timeInterval = (float) (currentTime - mDecelerationLastTime) / 1000.f;
        //更新chart的旋转角度
        mChart.setRotationAngle(mChart.getRotationAngle() + mDecelerationAngularVelocity * timeInterval);
        mDecelerationLastTime = currentTime;
        if (Math.abs(mDecelerationAngularVelocity) >= 0.001)
            //触发重绘，同时执行下一次computeScroll
            Utils.postInvalidateOnAnimation(mChart); // This causes computeScroll to fire, recommended for this by Google
        else
            stopDeceleration();
    }   
```

高亮的获取
com/github/mikephil/charting/charts/Chart.java
```
 public Highlight getHighlightByTouchPoint(float x, float y) {
      ....
            return getHighlighter().getHighlight(x, y);
    }
```
com/github/mikephil/charting/highlight/PieRadarHighlighter.java
```
 public Highlight getHighlight(float x, float y) {
        float touchDistanceToCenter = mChart.distanceToCenter(x, y);
        ...
            float angle = mChart.getAngleForPoint(x, y);

            if (mChart instanceof PieChart) {
                angle /= mChart.getAnimator().getPhaseY();
            }
            int index = mChart.getIndexForAngle(angle);

            // check if the index could be found
            if (index < 0 || index >= mChart.getData().getMaxEntryCountSet().getEntryCount()) {
                return null;
            } else {
                return getClosestHighlight(index, x, y);
            }
        }
    }
```
com/github/mikephil/charting/charts/PieRadarChartBase.java
```
//根据点的位置计算角度
public float getAngleForPoint(float x, float y) {

        MPPointF c = getCenterOffsets();
        double tx = x - c.x, ty = y - c.y;
        double length = Math.sqrt(tx * tx + ty * ty);
        double r = Math.acos(ty / length);
        float angle = (float) Math.toDegrees(r);
        if (x > c.x)
            angle = 360f - angle;
        angle = angle + 90f;
        if (angle > 360f)
            angle = angle - 360f;
        MPPointF.recycleInstance(c);

        return angle;
    }
```
com/github/mikephil/charting/charts/PieChart.java
```
 public int getIndexForAngle(float angle) {
        float a = Utils.getNormalizedAngle(angle - getRotationAngle());
        //返回第一个角度大于的
        for (int i = 0; i < mAbsoluteAngles.length; i++) {
            if (mAbsoluteAngles[i] > a)
                return i;
        }
        return -1; // return -1 if no index found
    }

```


performHighlight
高亮设置
com/github/mikephil/charting/listener/ChartTouchListener.java
```
protected void performHighlight(Highlight h, MotionEvent e) {
        if (h == null || h.equalTo(mLastHighlighted)) {
            mChart.highlightValue(null, true);
            mLastHighlighted = null;
        } else {
            mChart.highlightValue(h, true);
            mLastHighlighted = h;
        }
    }
```

com/github/mikephil/charting/charts/Chart.java
设置高亮
```
  public void highlightValue(float x, float y, int dataSetIndex, boolean callListener) {
        highlightValue(x, y, dataSetIndex, -1, callListener);
    }
 public void highlightValue(float x, float y, int dataSetIndex, int dataIndex, boolean callListener) {
    if (dataSetIndex < 0 || dataSetIndex >= mData.getDataSetCount()) {
        highlightValue(null, callListener);
    } else {
        highlightValue(new Highlight(x, y, dataSetIndex, dataIndex), callListener);
    }
}   

 public void highlightValue(Highlight high, boolean callListener) {
        Entry e = null;
        if (high == null)
            mIndicesToHighlight = null;
        else {
          ...
            //根据highlight获取数据entry
            e = mData.getEntryForHighlight(high);
            if (e == null) {
                mIndicesToHighlight = null;
                high = null;
            } else {
                mIndicesToHighlight = new Highlight[]{
                        high
                };
            }
        }
        setLastHighlighted(mIndicesToHighlight);
        //重绘
        invalidate();
    }

com/github/mikephil/charting/data/ChartData.java
  public Entry getEntryForHighlight(Highlight highlight) {
        if (highlight.getDataSetIndex() >= mDataSets.size())
            return null;
        else {
            return mDataSets.get(highlight.getDataSetIndex()).getEntryForXValue(highlight.getX(), highlight.getY());
        }
    }
```

高亮的绘制
com/github/mikephil/charting/renderer/PieChartRenderer.java
```
public class PieChartRenderer extends DataRenderer {
  public void drawHighlighted(Canvas c, Highlight[] indices) {
     ...
     //颜色
     Integer highlightColor = set.getHighlightColor();
            if (highlightColor == null)
                highlightColor = set.getColor(index);
            mRenderPaint.setColor(highlightColor); 
      //角度      
      final float startAngleShifted = rotationAngle + (angle + sliceSpaceAngleShifted / 2.f) * phaseY;
      float sweepAngleShifted = (sliceAngle - sliceSpaceAngleShifted) * phaseY;
      ...
      //绘制直线和曲线
       mPathBuffer.moveTo(
                        center.x + highlightedRadius * (float) Math.cos(startAngleShifted * Utils.FDEG2RAD),
                        center.y + highlightedRadius * (float) Math.sin(startAngleShifted * Utils.FDEG2RAD));
        mPathBuffer.arcTo(
                highlightedCircleBox,
                startAngleShifted,
                sweepAngleShifted
        );
        
       ...
       mPathBuffer.lineTo(
                            center.x + innerRadius * (float) Math.cos(endAngleInner * Utils.FDEG2RAD),
                            center.y + innerRadius * (float) Math.sin(endAngleInner * Utils.FDEG2RAD));          
     ...           
  }
}
```

ChartHighlighter与Highlight关系
com/github/mikephil/charting/highlight/ChartHighlighter.java
```
public class ChartHighlighter<T extends BarLineScatterCandleBubbleDataProvider> implements IHighlighter
{
public Highlight getHighlight(float x, float y) {
        MPPointD pos = getValsForTouch(x, y);
        float xVal = (float) pos.x;
        MPPointD.recycleInstance(pos);
        Highlight high = getHighlightForX(xVal, x, y);
        return high;
    }
}
```