package sst.example.androiddemo.feature.widget.practice.recyclerview.customLayoutManager;


import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import java.util.ArrayList;
import java.util.List;

public class CustomLayoutManger extends RecyclerView.LayoutManager {

    private int mItemViewHeight;
    private int mItemViewWidth;
    private int mItemCount;
    private int mScrollOffset = Integer.MAX_VALUE;

    private float mItemHeightWidthRatio;
    private float mScale;
    private boolean mHasChild;

    private final SnapHelper mSnapHelper = new CustomSnapHelper();

    public CustomLayoutManger(float itemHeightWidthRatio, float scale) {
        mItemHeightWidthRatio = itemHeightWidthRatio;
        mScale = scale;
    }

    @Override
    public void onAttachedToWindow(RecyclerView view) {
        super.onAttachedToWindow(view);
        mSnapHelper.attachToRecyclerView(view);
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(RecyclerView.LayoutParams.WRAP_CONTENT, RecyclerView.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (state.getItemCount() == 0 || state.isPreLayout()) {
            return;
        }
        if (!mHasChild) {
            mHasChild = true;
            mItemViewHeight = getVerticalSpace();
            mItemViewWidth = (int) (mItemViewHeight / mItemHeightWidthRatio);
        }
        mItemCount = getItemCount();
        mScrollOffset = makeScrollOffsetWithinRange(mScrollOffset);
        fill(recycler);
    }

    private void fill(RecyclerView.Recycler recycler) {
        // 1.初始化基本变量
        int bottomVisiblePosition = mScrollOffset / mItemViewWidth;
        final int bottomItemVisibleSize = mScrollOffset % mItemViewWidth;
        final float offsetPercent = bottomItemVisibleSize * 1.0f / mItemViewWidth;
        final int space = getHorizontalSpace();
        int remainSpace = space;
        final int defaultOffset = mItemViewWidth / 2;
        final List<ItemViewInfo> itemViewInfos = new ArrayList<>();
        // 2.计算每个ItemView的位置信息(left和scale)
        for (int i = bottomVisiblePosition - 1, j = 1; i >= 0; i--, j++) {
            //
            double maxOffset = defaultOffset * Math.pow(mScale, j - 1);
            int start = (int) (remainSpace - offsetPercent * maxOffset - mItemViewWidth);
            ItemViewInfo info = new ItemViewInfo(start, (float) (Math.pow(mScale, j - 1) * (1 - offsetPercent * (1 - mScale))));
            itemViewInfos.add(0, info);
            remainSpace -= maxOffset;
            if (remainSpace < 0) {
                info.setLeft((int) (remainSpace + maxOffset - mItemViewWidth));
                info.setScale((float) Math.pow(mScale, j - 1));
                break;
            }
        }
        // 3.添加最右边ItemView的相关信息
        if (bottomVisiblePosition < mItemCount) {
            final int left = space - bottomItemVisibleSize;
            itemViewInfos.add(new ItemViewInfo(left, 1.0f));
        } else {
            bottomVisiblePosition -= 1;
        }
        // 4.回收其他位置的View
        final int layoutCount = itemViewInfos.size();
        final int startPosition = bottomVisiblePosition - (layoutCount - 1);
        final int endPosition = bottomVisiblePosition;
        final int childCount = getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            final View childView = getChildAt(i);
            final int position = convert2LayoutPosition(i);
            if (position > endPosition || position < startPosition) {
                detachAndScrapView(childView, recycler);
            }
        }
        // 5.先回收再布局
        detachAndScrapAttachedViews(recycler);
        for (int i = 0; i < layoutCount; i++) {
            fillChild(recycler.getViewForPosition(convert2AdapterPosition(startPosition + i)), itemViewInfos.get(i));
        }
    }

    private void fillChild(View view, ItemViewInfo itemViewInfo) {
        addView(view);
        measureChildWithExactlySize(view);
        final int top = getPaddingTop();

        layoutDecoratedWithMargins(view, itemViewInfo.getLeft(), top, itemViewInfo.getLeft() + mItemViewWidth, top + mItemViewHeight);
        view.setScaleX(itemViewInfo.getScale());
        view.setScaleY(itemViewInfo.getScale());
    }


    private void measureChildWithExactlySize(View child) {
        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) child.getLayoutParams();
        final int widthSpec = View.MeasureSpec.makeMeasureSpec(mItemViewWidth - lp.leftMargin - lp.rightMargin, View.MeasureSpec.EXACTLY);
        final int heightSpec = View.MeasureSpec.makeMeasureSpec(mItemViewHeight - lp.topMargin - lp.bottomMargin, View.MeasureSpec.EXACTLY);
        child.measure(widthSpec, heightSpec);
    }

    @Override
    public boolean canScrollHorizontally() {
        return true;
    }

    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        int pendingScrollOffset = mScrollOffset + dx;
        mScrollOffset = makeScrollOffsetWithinRange(pendingScrollOffset);
        fill(recycler);
        return mScrollOffset - pendingScrollOffset + dx;
    }

    private int convert2LayoutPosition(int adapterPosition) {
        return mItemCount - 1 - adapterPosition;
    }

    public int convert2AdapterPosition(int layoutPosition) {
        return mItemCount - 1 - layoutPosition;
    }

    private int makeScrollOffsetWithinRange(int scrollOffset) {
        return Math.min(Math.max(mItemViewWidth, scrollOffset), mItemCount * mItemViewWidth);
    }

    public int getVerticalSpace() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }

    public int getHorizontalSpace() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    public int calculateDistanceToPosition(int targetPos) {
        int pendingScrollOffset = mItemViewWidth * (convert2LayoutPosition(targetPos) + 1);
        return pendingScrollOffset - mScrollOffset;
    }

    public int getFixedScrollPosition() {
        if (mHasChild) {
            if (mScrollOffset % mItemViewWidth == 0) {
                return RecyclerView.NO_POSITION;
            }
            float position = mScrollOffset * 1.0f / mItemViewWidth;
            return convert2AdapterPosition((int) (position - 0.5f));
        }
        return RecyclerView.NO_POSITION;
    }

}
