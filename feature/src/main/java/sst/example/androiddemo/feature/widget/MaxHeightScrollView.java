package sst.example.androiddemo.feature.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ScrollView;
import sst.example.androiddemo.feature.R;

//scrollview 超过一定的高度才滚动
public class MaxHeightScrollView extends ScrollView {

  private int mMaxHeight;

  public MaxHeightScrollView(Context context) {
    super(context);
  }

  public MaxHeightScrollView(Context context, AttributeSet attrs) {
    super(context, attrs);
    initialize(context, attrs);
  }

  public MaxHeightScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initialize(context, attrs);
  }

  private void initialize(Context context, AttributeSet attrs) {
    TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MaxHeightScrollView);
    mMaxHeight = typedArray.getLayoutDimension(R.styleable.MaxHeightScrollView_maxHeight, mMaxHeight);
    typedArray.recycle();
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    if (mMaxHeight > 0) {
      heightMeasureSpec = MeasureSpec.makeMeasureSpec(mMaxHeight, MeasureSpec.AT_MOST);
    }
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
  }
}