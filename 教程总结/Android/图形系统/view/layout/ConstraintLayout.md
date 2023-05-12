

ConstraintLayout-2.1.4
https://github.com/androidx/constraintlayout/tree/2.1.4
https://mp.weixin.qq.com/s?__biz=MzUyMDAxMjQ3Ng%3D%3D&mid=2247494156&idx=1&sn=e1402c29aa259c66c34e04ff870a5bd4&hmsr=joyk.com&utm_source=joyk.com&utm_medium=referral

继承ConstraintLayout的例子
material-1.6.1\jars\classes.jar
```
class TimePickerView extends ConstraintLayout implements TimePickerControls {
 public TimePickerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    //将layout添加进布局
    LayoutInflater.from(context).inflate(R.layout.material_timepicker, this);
    //直接从group解析
    clockFace = findViewById(R.id.material_clock_face);
    ....
  }
}

layout使用
material-1.6.1\res\layout\material_timepicker.xml
<?xml version="1.0" encoding="utf-8"?>
<!--使用merge减少层级-->
<merge xmlns:android="http://schemas.android.com/apk/res/android"
  xmlns:app="http://schemas.android.com/apk/res-auto"
  android:id="@+id/material_timepicker_container"
  android:layout_width="wrap_content"
  android:layout_height="wrap_content">
  ...
  <!--子child可以直接使用constraint属性-->
  <com.google.android.material.timepicker.ClockFaceView
    android:id="@+id/material_clock_face"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:layout_constraintStart_toStartOf="parent"
    app:layout_constraintEnd_toEndOf="parent"
    app:layout_constraintTop_toBottomOf="@+id/material_clock_display" />
</merge>
```


构造器
```
 public ConstraintLayout(@NonNull Context context) {
        super(context);
        init(null, 0, 0);
    }
 
  private void init(AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mLayoutWidget.setCompanionWidget(this);
        mLayoutWidget.setMeasurer(mMeasurer);
        mChildrenByIds.put(getId(), this);
        mConstraintSet = null;
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ConstraintLayout_Layout, defStyleAttr, defStyleRes);
            final int N = a.getIndexCount();
            for (int i = 0; i < N; i++) {
                int attr = a.getIndex(i);
                if (attr == R.styleable.ConstraintLayout_Layout_android_minWidth) {
                    mMinWidth = a.getDimensionPixelOffset(attr, mMinWidth);
                } else if (attr == R.styleable.ConstraintLayout_Layout_android_minHeight) {
                .....
                } else if (attr == R.styleable.ConstraintLayout_Layout_constraintSet) {
                    int id = a.getResourceId(attr, 0);
                    try {
                        mConstraintSet = new ConstraintSet();
                        mConstraintSet.load(getContext(), id);
                    } catch (Resources.NotFoundException e) {
                        mConstraintSet = null;
                    }
                    mConstraintSetId = id;
                }
            }
            a.recycle();
        }
        mLayoutWidget.setOptimizationLevel(mOptimizationLevel);
    }
```
1 解析xml中mMinWidth mMaxWidth等相关属性
2 解析xml中constraint相关属性，添加进ConstraintSet



onMeasure
```
protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        long time = 0;
        ...
        boolean sameSpecsAsPreviousMeasure = (mOnMeasureWidthMeasureSpec == widthMeasureSpec
                && mOnMeasureHeightMeasureSpec == heightMeasureSpec);
        sameSpecsAsPreviousMeasure = false; 
        //view添加移除 mDirtyHierarchy为TRUE
        if (!mDirtyHierarchy && !sameSpecsAsPreviousMeasure) {   
            final int count = getChildCount();
            for (int i = 0; i < count; i++) {
                final View child = getChildAt(i);
                if (child.isLayoutRequested()) { //child请求layout
                    ...
                    mDirtyHierarchy = true;
                    break;
                }
            }
        }
        //子view数量没有改变，child没有请求layout   
        if (!mDirtyHierarchy) {
            if (sameSpecsAsPreviousMeasure) {
                resolveMeasuredDimension(widthMeasureSpec, heightMeasureSpec, mLayoutWidget.getWidth(), mLayoutWidget.getHeight(),
                        mLayoutWidget.isWidthMeasuredTooSmall(), mLayoutWidget.isHeightMeasuredTooSmall());
                return;
            }
            if (OPTIMIZE_HEIGHT_CHANGE
                    && mOnMeasureWidthMeasureSpec == widthMeasureSpec
                    && MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY
                    && MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST
                    && MeasureSpec.getMode(mOnMeasureHeightMeasureSpec) == MeasureSpec.AT_MOST) {
                int newSize = MeasureSpec.getSize(heightMeasureSpec);
                ...
                if (newSize >= mLayoutWidget.getHeight() && !mLayoutWidget.isHeightMeasuredTooSmall()) {
                    mOnMeasureWidthMeasureSpec = widthMeasureSpec;
                    mOnMeasureHeightMeasureSpec = heightMeasureSpec;
                    resolveMeasuredDimension(widthMeasureSpec, heightMeasureSpec, mLayoutWidget.getWidth(), mLayoutWidget.getHeight(),
                            mLayoutWidget.isWidthMeasuredTooSmall(), mLayoutWidget.isHeightMeasuredTooSmall());
                    return;
                }
            }
        }
        mOnMeasureWidthMeasureSpec = widthMeasureSpec;
        mOnMeasureHeightMeasureSpec = heightMeasureSpec;
       ...

        mLayoutWidget.setRtl(isRtl());

        if (mDirtyHierarchy) {
            mDirtyHierarchy = false;
            if (updateHierarchy()) {
                mLayoutWidget.updateHierarchy();
            }
        }

        resolveSystem(mLayoutWidget, mOptimizationLevel, widthMeasureSpec, heightMeasureSpec);
        //调用setMeasuredDimension
        resolveMeasuredDimension(widthMeasureSpec, heightMeasureSpec, mLayoutWidget.getWidth(), mLayoutWidget.getHeight(),
                mLayoutWidget.isWidthMeasuredTooSmall(), mLayoutWidget.isHeightMeasuredTooSmall());
       ....
    }
```
子view数量没有改变，child没有请求layout 宽高没变，直接设置宽高
width为EXACTLY height为AT_MOST 设置宽高
updateHierarchy 更新child约束
resolveSystem 求解约束
根据measure测量结果，设置宽高setMeasuredDimension


updateHierarchy
```
private boolean updateHierarchy() {
        final int count = getChildCount();
        boolean recompute = false;
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.isLayoutRequested()) {
                recompute = true;
                break;
            }
        }
        //子child requestLayout，更新布局约束
        if (recompute) {
            setChildrenConstraints();
        }
        return recompute;
    }


 private void setChildrenConstraints() {
        ...
        final int count = getChildCount();
        // Make sure everything is fully reset before anything else
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            //给每个child 创建ConstraintWidget，保存布局约束相关信息
            ConstraintWidget widget = getViewWidget(child);
            if (widget == null) {
                continue;
            }
            //重置
            widget.reset();
        }
        ....

        if (USE_CONSTRAINTS_HELPER && mConstraintSetId != -1) {
            for (int i = 0; i < count; i++) {
                final View child = getChildAt(i);
                if (child.getId() == mConstraintSetId && child instanceof Constraints) {
                    mConstraintSet = ((Constraints) child).getConstraintSet();
                }
            }
        }

        if (mConstraintSet != null) {
         //将约束相关设置到Barrier，Guideline，ConstraintHelper以及对应的view
            mConstraintSet.applyToInternal(this, true);
        }

        mLayoutWidget.removeAllChildren();

        final int helperCount = mConstraintHelpers.size();
        //回调ConstraintHelper.updatePreLayout
        if (helperCount > 0) {
            for (int i = 0; i < helperCount; i++) {
                ConstraintHelper helper = mConstraintHelpers.get(i);
                helper.updatePreLayout(this);
            }
        }

        //调用Placeholder.updatePreLayout
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child instanceof Placeholder) {
                ((Placeholder) child).updatePreLayout(this);
            }
        }

        mTempMapIdToWidget.clear();
        mTempMapIdToWidget.put(PARENT_ID, mLayoutWidget);
        mTempMapIdToWidget.put(getId(), mLayoutWidget);
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            ConstraintWidget widget = getViewWidget(child);
            mTempMapIdToWidget.put(child.getId(), widget);
        }

        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            //获取view对应的ConstraintWidget
            ConstraintWidget widget = getViewWidget(child);
            if (widget == null) {
                continue;
            }
            final LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
            mLayoutWidget.add(widget);
            //从LayoutParams获取约束信息
            applyConstraintsFromLayoutParams(isInEditMode, child, widget, layoutParams, mTempMapIdToWidget);
        }
    }    
```



onLayout
```
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        ...
        final int widgetsCount = getChildCount();
        final boolean isInEditMode = isInEditMode();
        for (int i = 0; i < widgetsCount; i++) {
            final View child = getChildAt(i);
            LayoutParams params = (LayoutParams) child.getLayoutParams();
            ConstraintWidget widget = params.widget;
            ...
            int l = widget.getX();
            int t = widget.getY();
            int r = l + widget.getWidth();
            int b = t + widget.getHeight();
            ...
            child.layout(l, t, r, b);
            if (child instanceof Placeholder) {
                Placeholder holder = (Placeholder) child;
                View content = holder.getContent();
                if (content != null) {
                    content.setVisibility(VISIBLE);
                    content.layout(l, t, r, b);
                }
            }
        }
        final int helperCount = mConstraintHelpers.size();
        if (helperCount > 0) {
            for (int i = 0; i < helperCount; i++) {
                ConstraintHelper helper = mConstraintHelpers.get(i);
                helper.updatePostLayout(this);
            }
        }
    }
```
1 执行child.layout
2 如果child是Placeholder，执行holder.getContent().layout()
3 执行ConstraintHelper.updatePostLayout      ConstraintHelper在onViewAdd()时添加


draw相关
```
  @Override
    protected void dispatchDraw(Canvas canvas) {
        if (mConstraintHelpers != null) {
            final int helperCount = mConstraintHelpers.size();
            if (helperCount > 0) {
                for (int i = 0; i < helperCount; i++) {
                    ConstraintHelper helper = mConstraintHelpers.get(i);
                    helper.updatePreDraw(this);
                }
            }
        }
        super.dispatchDraw(canvas);
  }
```
1 让ConstraintHelper具备一定的绘制能力