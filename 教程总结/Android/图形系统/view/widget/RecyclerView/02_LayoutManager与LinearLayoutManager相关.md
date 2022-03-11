https://zhuanlan.zhihu.com/p/263841552

LayoutManager.onLayoutChildren 分析，以LinearLayoutManager为例

布局相关
锚点(AnchorInfo) ，其实 LLM 的布局并不是从上往下一个个进行的。而是很可能从整个布局的中间某个点开始的，然后朝一个方向一个个填充，
填满可见区域后，朝另一个方向进行填充。至于先朝哪个方向填充，是根据具体的变量来确定的。
AnchorInfo 类需要能够有效的描述一个具体的位置信息  
大多数情况下，这是距离起点或终点最近的view，具有有效位置（例如，未删除）。 如果一个孩子有focus，他会得到优先权。
```
class AnchorInfo {
        //锚点参考View在整个数据中的position信息，即它是第几个View
        int mPosition;
        //锚点的具体坐标信息，填充子View的起始坐标。当positon=0的时候，如果只有一半View可见，那么这个数据可能为负数
        int mCoordinate;
        //是否从底部开始布局
        boolean mLayoutFromEnd;
        //是否有效
        boolean mValid;
 }
```
锚点信息的更新
```
private void updateAnchorInfoForLayout(RecyclerView.Recycler recycler, RecyclerView.State state, AnchorInfo anchorInfo) {
        //从挂起的数据更新锚点信息  这个方法一般不会调用到
        if (updateAnchorFromPendingData(state, anchorInfo)) {
            return;
        }
        //**重点方法 从子View来确定锚点信息（这里会尝试从有焦点的子View或者列表第一个位置的View或者最后一个位置的View来确定）
        if (updateAnchorFromChildren(recycler, state, anchorInfo)) {
            return;
        }
        //进入这里说明现在都没有确定锚点（比如设置Data后还没有绘制View的情况下），就直接设置RecyclerView的顶部或者底部位置为锚点
        //(按照默认情况，这里的mPosition=0)。
        anchorInfo.assignCoordinateFromPadding();
        anchorInfo.mPosition = mStackFromEnd ? state.getItemCount() - 1 : 0;
    }
    //获取除去padding的开始或结束位置
     void assignCoordinateFromPadding() {
            mCoordinate = mLayoutFromEnd
                    ? mOrientationHelper.getEndAfterPadding()
                    : mOrientationHelper.getStartAfterPadding();
        }
```
锚点的确定方案主要有3个：
1 从挂起的数据获取锚点信息。一般不会执行。
2 从子View来确定锚点信息。比如说notifyDataSetChanged方法的时候，屏幕上原来是有View的，那么就会通过这种方式获取
3 如果上面两种方法都无法确定，则直接使用0位置的View作为锚点参考position。
最后一种什么时候会发生呢？其实就是没有子View让我们作为参考。比如说第一次加载数据的时候，RecyclerView一片空白。
   这时候肯定没有任何子View能够让我们作为参考。

那么当有子View的时候，我们通过 updateAnchorFromChildren 方法来确定锚点位置。
```
    //从现有子View中确定锚定。大多数情况下，是起始或者末尾的有效子View(一般是未移除，展示在我们面前的View)。
private boolean updateAnchorFromChildren(RecyclerView.Recycler recycler,
            RecyclerView.State state, AnchorInfo anchorInfo) {
        if (getChildCount() == 0) {
          //没有数据，直接返回false
            return false;
        }
        final View focused = getFocusedChild();
        //优先选取获得焦点的子View作为锚点
        if (focused != null && anchorInfo.isViewValidAsAnchor(focused, state)) {
            anchorInfo.assignFromViewAndKeepVisibleRect(focused, getPosition(focused));
            return true;
        }
        if (mLastStackFromEnd != mStackFromEnd) {
            return false;
        }
        //根据锚点的设置信息，从底部或者顶部获取子View信息
        View referenceChild =
                findReferenceChild(
                        recycler,
                        state,
                        anchorInfo.mLayoutFromEnd,
                        mStackFromEnd);
        if (referenceChild != null) {
            anchorInfo.assignFromView(referenceChild, getPosition(referenceChild));
           // 如果在一次过程中删除了所有可见视图，则引用子对象可能超出范围。
           //如果是这种情况，请将其偏移回0，以便使用这些预布局子项。
            if (!state.isPreLayout() && supportsPredictiveItemAnimations()) {
                // validate this child is at least partially visible. if not, offset it to start
                final int childStart = mOrientationHelper.getDecoratedStart(referenceChild);
                final int childEnd = mOrientationHelper.getDecoratedEnd(referenceChild);
                final int boundsStart = mOrientationHelper.getStartAfterPadding();
                final int boundsEnd = mOrientationHelper.getEndAfterPadding();
                // b/148869110: usually if childStart >= boundsEnd the child is out of
                // bounds, except if the child is 0 pixels!
                boolean outOfBoundsBefore = childEnd <= boundsStart && childStart < boundsStart;
                boolean outOfBoundsAfter = childStart >= boundsEnd && childEnd > boundsEnd;
                if (outOfBoundsBefore || outOfBoundsAfter) {
                    anchorInfo.mCoordinate = anchorInfo.mLayoutFromEnd ? boundsEnd : boundsStart;
                }
            }
            return true;
        }
        return false;
    }
```
通过子View确定锚点坐标也是进行了3种情况的处理
1 没有数据，直接返回获取失败
2 如果某个子View持有焦点，那么直接把持有焦点的子View作为锚点参考点
3 没有子View持有焦点，一般会选择最上（或者最下面）的子View作为锚点参考点

anchorInfo.assignFromViewAndKeepVisibleRect()和anchorInfo.assignFromView()相似
```
//AnchorInfo
public void assignFromView(View child, int position) {
            if (mLayoutFromEnd) {              
                mCoordinate = mOrientationHelper.getDecoratedEnd(child)
                        + mOrientationHelper.getTotalSpaceChange();
            } else {
                //获取child包含decoration的上边距
                mCoordinate = mOrientationHelper.getDecoratedStart(child);
            }
            mPosition = position;
        }
 public void assignFromViewAndKeepVisibleRect(View child, int position) {
            final int spaceChange = mOrientationHelper.getTotalSpaceChange();
            if (spaceChange >= 0) {
                assignFromView(child, position);
                return;
            }
            mPosition = position;
            if (mLayoutFromEnd) {
                final int prevLayoutEnd = mOrientationHelper.getEndAfterPadding() - spaceChange;
                final int childEnd = mOrientationHelper.getDecoratedEnd(child);
                final int previousEndMargin = prevLayoutEnd - childEnd;
                mCoordinate = mOrientationHelper.getEndAfterPadding() - previousEndMargin;
                // ensure we did not push child's top out of bounds because of this
                if (previousEndMargin > 0) { // we have room to shift bottom if necessary
                    final int childSize = mOrientationHelper.getDecoratedMeasurement(child);
                    final int estimatedChildStart = mCoordinate - childSize;
                    final int layoutStart = mOrientationHelper.getStartAfterPadding();
                    final int previousStartMargin = mOrientationHelper.getDecoratedStart(child)
                            - layoutStart;
                    final int startReference = layoutStart + Math.min(previousStartMargin, 0);
                    final int startMargin = estimatedChildStart - startReference;
                    if (startMargin < 0) {
                        // offset to make top visible but not too much
                        mCoordinate += Math.min(previousEndMargin, -startMargin);
                    }
                }
            } else {
                final int childStart = mOrientationHelper.getDecoratedStart(child);
                final int startMargin = childStart - mOrientationHelper.getStartAfterPadding();
                mCoordinate = childStart;
                if (startMargin > 0) { // we have room to fix end as well
                    final int estimatedEnd = childStart
                            + mOrientationHelper.getDecoratedMeasurement(child);
                    final int previousLayoutEnd = mOrientationHelper.getEndAfterPadding()
                            - spaceChange;
                    final int previousEndMargin = previousLayoutEnd
                            - mOrientationHelper.getDecoratedEnd(child);
                    final int endReference = mOrientationHelper.getEndAfterPadding()
                            - Math.min(0, previousEndMargin);
                    final int endMargin = endReference - estimatedEnd;
                    if (endMargin < 0) {
                        mCoordinate -= Math.min(startMargin, -endMargin);
                    }
                }
            }
        }  
   
   //OrientationHelper.java  
   //返回开始的view包含其decoration的边距
   //例如view的左边距在20px,它的左侧decoration的大小是2px，左边距是3px，让返回20-2-3=15px
   public abstract int getDecoratedStart(View view){
   }           
```
referenceChild 查找开始或结束位置的view
```
//找到合适的锚点child
//由于adapter更新不明确或子项被删除，某些子项的位置可能无效。如果可能的话，此方法是在适配器边界内查找位置的最佳方法。
//它还按以下顺序从最好的到最差的顺序排列child的优先级：
//1 An in bounds child.
//2 An out of bounds child.
//3 An invalid child.
View findReferenceChild(RecyclerView.Recycler recycler, RecyclerView.State state,
            boolean layoutFromEnd, boolean traverseChildrenInReverseOrder) {
        ensureLayoutState();
        int start = 0;
        int end = getChildCount();
        int diff = 1;
        if (traverseChildrenInReverseOrder) {
            start = getChildCount() - 1;
            end = -1;
            diff = -1;
        }

        int itemCount = state.getItemCount();

        final int boundsStart = mOrientationHelper.getStartAfterPadding();
        final int boundsEnd = mOrientationHelper.getEndAfterPadding();

        View invalidMatch = null;
        View bestFirstFind = null;
        View bestSecondFind = null;

        for (int i = start; i != end; i += diff) {
            final View view = getChildAt(i);
            final int position = getPosition(view);
            final int childStart = mOrientationHelper.getDecoratedStart(view);
            final int childEnd = mOrientationHelper.getDecoratedEnd(view);
            if (position >= 0 && position < itemCount) {
                if (((RecyclerView.LayoutParams) view.getLayoutParams()).isItemRemoved()) {
                    if (invalidMatch == null) {
                        invalidMatch = view; // removed item, least preferred
                    }
                } else {
                    // b/148869110: usually if childStart >= boundsEnd the child is out of
                    // bounds, except if the child is 0 pixels!
                    boolean outOfBoundsBefore = childEnd <= boundsStart && childStart < boundsStart;
                    boolean outOfBoundsAfter = childStart >= boundsEnd && childEnd > boundsEnd;
                    if (outOfBoundsBefore || outOfBoundsAfter) {
                       // 该物品不在范围内。
                       // 我们希望找到最接近边界内项目的项目，因为我们总是线性地遍历项目，所以我们想要的两个项目是我们开始搜索的一侧的最后一个边界外项目，
                       // 以及我们结束的一侧的第一个边界外项目。我们最终要结束的那一方具有优先权，因为如果没有找到边界内的锚，
                       // 我们希望布局中稍后的项目向前移动
                        if (layoutFromEnd) {
                            if (outOfBoundsAfter) {
                                bestFirstFind = view;
                            } else if (bestSecondFind == null) {
                                bestSecondFind = view;
                            }
                        } else {
                            if (outOfBoundsBefore) {
                                bestFirstFind = view;
                            } else if (bestSecondFind == null) {
                                bestSecondFind = view;
                            }
                        }
                    } else {
                        // We found an in bounds item, greedily return it.
                        return view;
                    }
                }
            }
        }
        // We didn't find an in bounds item so we will settle for an item in this order:
        // 1. bestSecondFind
        // 2. bestFirstFind
        // 3. invalidMatch
        return bestSecondFind != null ? bestSecondFind :
                (bestFirstFind != null ? bestFirstFind : invalidMatch);
    }
```


onLayoutChildren 布局分析
布局算法：
1）通过检查children和其他变量，找到锚定坐标和锚定项目位置。
2）向起点填充，从底部堆放
3）向末端填充，从顶部堆放
4）滚动以满足从底部堆叠等要求。
//创建layout state
```
public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
  //1
   final View focused = getFocusedChild();
        if (!mAnchorInfo.mValid || mPendingScrollPosition != RecyclerView.NO_POSITION
                || mPendingSavedState != null) {
            mAnchorInfo.reset();
            mAnchorInfo.mLayoutFromEnd = mShouldReverseLayout ^ mStackFromEnd;
            // calculate anchor position and coordinate
            updateAnchorInfoForLayout(recycler, state, mAnchorInfo);
            mAnchorInfo.mValid = true;
        } else if (focused != null && (mOrientationHelper.getDecoratedStart(focused)
                >= mOrientationHelper.getEndAfterPadding()
                || mOrientationHelper.getDecoratedEnd(focused)
                <= mOrientationHelper.getStartAfterPadding())) {
            // This case relates to when the anchor child is the focused view and due to layout
            // shrinking the focused view fell outside the viewport, e.g. when soft keyboard shows
            // up after tapping an EditText which shrinks RV causing the focused view (The tapped
            // EditText which is the anchor child) to get kicked out of the screen. Will update the
            // anchor coordinate in order to make sure that the focused view is laid out. Otherwise,
            // the available space in layoutState will be calculated as negative preventing the
            // focused view from being laid out in fill.
            // Note that we won't update the anchor position between layout passes (refer to
            // TestResizingRelayoutWithAutoMeasure), which happens if we were to call
            // updateAnchorInfoForLayout for an anchor that's not the focused view (e.g. a reference
            // child which can change between layout passes).
            mAnchorInfo.assignFromViewAndKeepVisibleRect(focused, getPosition(focused));
        }
    ...
        
    if (mAnchorInfo.mLayoutFromEnd) {
            //2 fill towards start
            //倒着绘制的话，先从锚点往上，绘制完再从锚点往下
            //设置绘制方向信息为从锚点往上
            updateLayoutStateToFillStart(mAnchorInfo);
            mLayoutState.mExtraFillSpace = extraForStart;
            //布局填充信息
            fill(recycler, mLayoutState, state, false);
            startOffset = mLayoutState.mOffset;
            final int firstElement = mLayoutState.mCurrentPosition;
            if (mLayoutState.mAvailable > 0) {
                extraForEnd += mLayoutState.mAvailable;
            }
            //3 fill towards end
            //设置绘制方向信息为从锚点往下
            updateLayoutStateToFillEnd(mAnchorInfo);
            mLayoutState.mExtraFillSpace = extraForEnd;
            mLayoutState.mCurrentPosition += mLayoutState.mItemDirection;
            fill(recycler, mLayoutState, state, false);
            endOffset = mLayoutState.mOffset;

            if (mLayoutState.mAvailable > 0) {
                // end could not consume all. add more items towards start
                extraForStart = mLayoutState.mAvailable;
                //4 更新Layout State
                updateLayoutStateToFillStart(firstElement, startOffset);
                mLayoutState.mExtraFillSpace = extraForStart;
                fill(recycler, mLayoutState, state, false);
                startOffset = mLayoutState.mOffset;
            }
        } else {
            //从底部开始填充 与上面差不多
        }  
        
    ////更改可能会导致UI出现gap，请尝试修复这些gap。
   //官方TODO，如果stackFromEnd/reverseLayout/RTL值都没有更改，我们可能可以避免这种情况
     if (getChildCount() > 0) {
            //因为从末端开始的布局可能发生变更因为滚动位置的变化
           //我们重新计算它，找出应该检查哪一侧的gap 
            if (mShouldReverseLayout ^ mStackFromEnd) {
                int fixOffset = fixLayoutEndGap(endOffset, recycler, state, true);
                startOffset += fixOffset;
                endOffset += fixOffset;
                fixOffset = fixLayoutStartGap(startOffset, recycler, state, false);
                startOffset += fixOffset;
                endOffset += fixOffset;
            } else {
                int fixOffset = fixLayoutStartGap(startOffset, recycler, state, true);
                startOffset += fixOffset;
                endOffset += fixOffset;
                fixOffset = fixLayoutEndGap(endOffset, recycler, state, false);
                startOffset += fixOffset;
                endOffset += fixOffset;
            }
        }
        layoutForPredictiveAnimations(recycler, state, startOffset, endOffset);
        if (!state.isPreLayout()) {
            mOrientationHelper.onLayoutComplete();
        } else {
            mAnchorInfo.reset();
        }
        mLastStackFromEnd = mStackFromEnd;      
}
```
可以看到，根据不同的绘制方向，这里面做了不同的处理，只是填充的方向相反而已，具体的步骤是相似的。都是从锚点开始往一个方向进行View的填充，
  填充满以后再朝另一个方向填充。填充子View使用的是 fill() 方法。

fill 填充方法
```
//在LinearLayoutManager中，进行界面重绘和进行滑动两种情况下，往屏幕上填充子View的工作都是调用fill()进行
int fill(RecyclerView.Recycler recycler, LayoutState layoutState,
            RecyclerView.State state, boolean stopOnFocusable) {
        //可用区域的像素数 
        final int start = layoutState.mAvailable;
        if (layoutState.mScrollingOffset != LayoutState.SCROLLING_OFFSET_NaN) {
            if (layoutState.mAvailable < 0) {
                layoutState.mScrollingOffset += layoutState.mAvailable;
            }
            //将滑出屏幕的View回收掉
            recycleByLayoutState(recycler, layoutState);
        }
        //剩余绘制空间=可用区域+扩展空间。
        int remainingSpace = layoutState.mAvailable + layoutState.mExtraFillSpace;
        LayoutChunkResult layoutChunkResult = mLayoutChunkResult;
        //循环布局直到没有剩余空间了或者没有剩余数据了
        while ((layoutState.mInfinite || remainingSpace > 0) && layoutState.hasMore(state)) {
            //初始化layoutChunkResult
            layoutChunkResult.resetInternal();
           //**重点方法  添加一个child，然后将绘制的相关信息保存到layoutChunkResult
            layoutChunk(recycler, state, layoutState, layoutChunkResult);
            //如果布局结束了(没有view了)，退出循环
            if (layoutChunkResult.mFinished) {
                break;
            }
            //根据所添加的child消费的高度更新layoutState的偏移量。mLayoutDirection为+1或者-1，通过乘法来处理是从底部往上布局，
            //还是从上往底部开始布局
            layoutState.mOffset += layoutChunkResult.mConsumed * layoutState.mLayoutDirection;
            if (!layoutChunkResult.mIgnoreConsumed || layoutState.mScrapList != null
                    || !state.isPreLayout()) {
                layoutState.mAvailable -= layoutChunkResult.mConsumed;
                //消费剩余可用空间
                remainingSpace -= layoutChunkResult.mConsumed;
            }

            if (layoutState.mScrollingOffset != LayoutState.SCROLLING_OFFSET_NaN) {
                layoutState.mScrollingOffset += layoutChunkResult.mConsumed;
                if (layoutState.mAvailable < 0) {
                    layoutState.mScrollingOffset += layoutState.mAvailable;
                }
                recycleByLayoutState(recycler, layoutState);
            }
            if (stopOnFocusable && layoutChunkResult.mFocusable) {
                break;
            }
        }
        return start - layoutState.mAvailable;
    }
    
    boolean hasMore(RecyclerView.State state) {
            return mCurrentPosition >= 0 && mCurrentPosition < state.getItemCount();
    } 
```
在 fill 方法中，会判断当前的是否还有剩余区域可以进行子View的填充。如果没有剩余区域或者没有子View，那么就返回。否则就通过 layoutChunk 来进行填充工作，
   填充完毕以后更新当前的可用区域，然后依次遍历循环，直到不满足条件为止。

循环中的填充是通过 layoutChunk 来实现的。 布局一块
```
 void layoutChunk(RecyclerView.Recycler recycler, RecyclerView.State state,
            LayoutState layoutState, LayoutChunkResult result) {
        //通过缓存获取当前position所需要展示的ViewHolder的View    
        View view = layoutState.next(recycler);
        if (view == null) {           
            //如果我们将视图放置在废弃视图中，这可能会返回null，这意味着没有更多的项需要布局。
            result.mFinished = true;
            return;
        }
        RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();
        if (layoutState.mScrapList == null) {
            //根据方向调用addView方法添加子View
            if (mShouldReverseLayout == (layoutState.mLayoutDirection
                    == LayoutState.LAYOUT_START)) {
                addView(view);
            } else {
                addView(view, 0);
            }
        } else {
             //这里是即将消失的View，但是需要设置对应的移除动画
            if (mShouldReverseLayout == (layoutState.mLayoutDirection
                    == LayoutState.LAYOUT_START)) {
                addDisappearingView(view);
            } else {
                addDisappearingView(view, 0);
            }
        }
        //调用measure测量view。这里会考虑到父类的padding
        measureChildWithMargins(view, 0, 0);
        //将本次子View消费的区域设置为子view的高(或者宽)
        result.mConsumed = mOrientationHelper.getDecoratedMeasurement(view);
        //根据mOrientation 找到view的四个边角位置
        int left, top, right, bottom;
        if (mOrientation == VERTICAL) {
            if (isLayoutRTL()) {
                right = getWidth() - getPaddingRight();
                left = right - mOrientationHelper.getDecoratedMeasurementInOther(view);
            } else {
                left = getPaddingLeft();
                right = left + mOrientationHelper.getDecoratedMeasurementInOther(view);
            }
            if (layoutState.mLayoutDirection == LayoutState.LAYOUT_START) {
                bottom = layoutState.mOffset;
                top = layoutState.mOffset - result.mConsumed;
            } else {
                top = layoutState.mOffset;
                bottom = layoutState.mOffset + result.mConsumed;
            }
        } else {
            ...
        }
       //调用child.layout方法进行布局(这里会考虑到view的ItemDecorator等信息)
        layoutDecoratedWithMargins(view, left, top, right, bottom);
        // Consume the available space if the view is not removed OR changed
        if (params.isItemRemoved() || params.isItemChanged()) {
            result.mIgnoreConsumed = true;
        }
        result.mFocusable = view.hasFocusable();
    }
    
    //如果需要，将视图添加到当前连接的RecyclerView。
    //LayoutManager应使用此方法添加从RecyclerView.Recycler 获得的视图。
     public void addView(View child) {
            addView(child, -1);
        }
     public void addView(View child, int index) {
            addViewInt(child, index, false);
        }    
         
     //只在onLayoutChildren中调用，用来添加一个即将消失的view到layout中，因为它已被移除，或者因为它实际上不在容器的可见部分
    //但它的布局是为了告知RecyclerView如何在视图外设置项目动画。
    //dispatchLayout过程完成后，通过此方法添加的视图将对LayoutManager不可见。
    //无法通过getChildAt（int）检索它们，或者它们不会包含在getChildCount（）方法中。
     public void addDisappearingView(View child) {
            addDisappearingView(child, -1);
        }
     public void addDisappearingView(View child, int index) {
            addViewInt(child, index, true);
        } 
        private void addViewInt(View child, int index, boolean disappearing) {
            final ViewHolder holder = getChildViewHolderInt(child);
            if (disappearing || holder.isRemoved()) {
                // 这些视图将隐藏在布局过程的末尾
                mRecyclerView.mViewInfoStore.addToDisappearedInLayout(holder);
            } else {
                //这看起来可能是不必要的，但如果布局管理器支持预测性布局，并且适配器已删除，然后重新添加相同的项，则可能会发生这种情况。
                //在这种情况下，添加的版本将在后期布局中可见（因为添加被延迟），但RV仍将其绑定到同一视图。
                //所以，若一个视图重新出现在后期布局过程中，请将其从消失列表中删除。
                mRecyclerView.mViewInfoStore.removeFromDisappearedInLayout(holder);
            }
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (holder.wasReturnedFromScrap() || holder.isScrap()) {
                if (holder.isScrap()) {
                    holder.unScrap();
                } else {
                    holder.clearReturnedFromScrapFlag();
                }
                mChildHelper.attachViewToParent(child, index, child.getLayoutParams(), false);
                if (DISPATCH_TEMP_DETACH) {
                    ViewCompat.dispatchFinishTemporaryDetach(child);
                }
            } else if (child.getParent() == mRecyclerView) { // it was not a scrap but a valid child
                // ensure in correct position
                int currentIndex = mChildHelper.indexOfChild(child);
                if (index == -1) {
                    index = mChildHelper.getChildCount();
                }
                if (currentIndex == -1) {              
                }
                if (currentIndex != index) {
                    mRecyclerView.mLayout.moveView(currentIndex, index);
                }
            } else {
                mChildHelper.addView(child, index, false);
                lp.mInsetsDirty = true;
                if (mSmoothScroller != null && mSmoothScroller.isRunning()) {
                    mSmoothScroller.onChildAttachedToWindow(child);
                }
            }
            if (lp.mPendingInvalidate) {
                holder.itemView.invalidate();
                lp.mPendingInvalidate = false;
            }
        }         
```
这里主要做了5个处理
1 通过 layoutState 获取要展示的View
2 通过 addView 方法将子View添加到布局中
3 调用 measureChildWithMargins 方法测量子View   主要调用child.measure()
4 调用 layoutDecoratedWithMargins 方法布局子View   主要调用child.layout()
5 根据处理的结果，填充LayoutChunkResult的相关信息，以便返回之后，能够进行数据的计算。
//todo layout()需要干什么   addViewInt没看懂


