
总结：
父类的边距调整，可以使用
```
canvas.translate(dx, 0);
super.onDraw(canvas);
canvas.restoreToCount(saveCount);
```

material 1.6
默认在drawable上混入颜色，可能导致自己设置的drawable显示异常
setUseMaterialThemeColors

material 1.7
1.7 checkBox改为两个drawable实现
app:buttonCompat 作为container外壳，app:buttonIcon作为icon
展示时将两个drawable进行合并
com/google/android/material/checkbox/MaterialCheckBox.java
```
 private void refreshButtonDrawable() {
    buttonDrawable =
        DrawableUtils.createTintableMutatedDrawableIfNeeded(
            buttonDrawable, buttonTintList, CompoundButtonCompat.getButtonTintMode(this));
    buttonIconDrawable =
        DrawableUtils.createTintableMutatedDrawableIfNeeded(
            buttonIconDrawable, buttonIconTintList, buttonIconTintMode);
    ...
    //设置button的drawable
    super.setButtonDrawable(
        DrawableUtils.compositeTwoLayeredDrawable(buttonDrawable, buttonIconDrawable));

    refreshDrawableState();
  }
```
合并方式使用layerDrawable
com/google/android/material/drawable/DrawableUtils.java
```
 public static Drawable compositeTwoLayeredDrawable(
      @Nullable Drawable bottomLayerDrawable, @Nullable Drawable topLayerDrawable) {
    if (bottomLayerDrawable == null) {
      return topLayerDrawable;
    }
    if (topLayerDrawable == null) {
      return bottomLayerDrawable;
    }
    LayerDrawable drawable =
        new LayerDrawable(new Drawable[] {bottomLayerDrawable, topLayerDrawable});
    ....
    if (VERSION.SDK_INT >= VERSION_CODES.M) {
      drawable.setLayerSize(1, topLayerNewWidth, topLayerNewHeight);
      drawable.setLayerGravity(1, Gravity.CENTER);
    } else {
      int horizontalInset = (bottomLayerDrawable.getIntrinsicWidth() - topLayerNewWidth) / 2;
      int verticalInset = (bottomLayerDrawable.getIntrinsicHeight() - topLayerNewHeight) / 2;
      drawable.setLayerInset(1, horizontalInset, verticalInset, horizontalInset, verticalInset);
    }
    return drawable;
  }
```

drawable的绘制  绘制时将drawable放在CheckBox的中心，默认是在left
MaterialCheckBox->AppCompatCheckBox->CheckBox->CompoundButton
com/google/android/material/checkbox/MaterialCheckBox.java
```
 protected void onDraw(Canvas canvas) {
    if (centerIfNoTextEnabled && TextUtils.isEmpty(getText())) {
      Drawable drawable = CompoundButtonCompat.getButtonDrawable(this);
      if (drawable != null) {
        int direction = ViewUtils.isLayoutRtl(this) ? -1 : 1;
        //(checkbox宽度-drawable宽度)/2   
        int dx = (getWidth() - drawable.getIntrinsicWidth()) / 2 * direction;
        int saveCount = canvas.save();
        //canvas平移后绘制drawable，此时drawable居中绘制
        canvas.translate(dx, 0);
        super.onDraw(canvas);
        canvas.restoreToCount(saveCount);
        ...
        return;
      }
    }
    super.onDraw(canvas);
  }
```

Drawable在父类的绘制
android/widget/CompoundButton.java
```
 public void setButtonDrawable(@Nullable Drawable drawable) {
        if (mButtonDrawable != drawable) {
            ...
            mButtonDrawable = drawable;
            ...
        }
    }
 protected void onDraw(Canvas canvas) {
        final Drawable buttonDrawable = mButtonDrawable;
         ...
        super.onDraw(canvas);
        if (buttonDrawable != null) {
            final int scrollX = mScrollX;
            final int scrollY = mScrollY;
            //绘制
            if (scrollX == 0 && scrollY == 0) {
                buttonDrawable.draw(canvas);
            } else {
                canvas.translate(scrollX, scrollY);
                buttonDrawable.draw(canvas);
                canvas.translate(-scrollX, -scrollY);
            }
        }
    }
```
TextView对drawable的绘制
```
  public void setCompoundDrawables(@Nullable Drawable left, @Nullable Drawable top,
            @Nullable Drawable right, @Nullable Drawable bottom) {
        Drawables dr = mDrawables;
        ...//将Drawable添进数组后重绘 
        invalidate();
        requestLayout();
    }
    
 protected void onDraw(Canvas canvas) {
       ....
        super.onDraw(canvas);
        ....
        final Drawables dr = mDrawables;
        if (dr != null) {
            ...
            //绘制left的drawable
            if (dr.mShowing[Drawables.LEFT] != null) {
                canvas.save();
                canvas.translate(scrollX + mPaddingLeft + leftOffset,
                        scrollY + compoundPaddingTop + (vspace - dr.mDrawableHeightLeft) / 2);
                dr.mShowing[Drawables.LEFT].draw(canvas);
                canvas.restore();
            }
        .....        
}
```