https://www.jianshu.com/p/c89c99736c05

//todo  嵌套滑动机制的实现原理 https://www.jianshu.com/p/cb3779d36118

RecyclerView作为一个列表View,天生就可以滑动。作为一个使用者，我们可以不去了解它是怎么进行滑动，但是我们作为一个学习源码的人，
  必须得知道RecyclerView的滑动机制，所以，我们今天来看看RecyclerView滑动部分的代码。

同时，从RecyclerView的类结构上来看，我们知道RecyclerView实现了NestedScrollingChild接口，所以RecyclerView也是一个
可以产生滑动事件的View。我相信大家都有用过CoordinatorLayout和RecyclerView这个组合，这其中原理的也是嵌套滑动。本文在介绍普通滑动中，
可能会涉及到嵌套滑动的知识，所以在阅读本文时，需要大家掌握嵌套滑动的机制，具体可以参考我上面的文章：Android 源码分析 - 嵌套滑动机制的实现原理，
此文专门从RecyclerView的角度上来理解嵌套滑动的机制。
本文打算从如下几个方面来分析RecyclerView：
1 正常的TouchEvent
2 嵌套滑动(穿插着文章各个地方，不会专门的讲解)
3 多指滑动
4 fling滑动

1. 传统事件
   现在，我们正式分析源码，首先我们来看看onTouchEvent方法，来看看它为我们做了那些事情：
```
 public boolean onTouchEvent(MotionEvent e) {
        ...
        if (dispatchToOnItemTouchListeners(e)) {
            cancelScroll();
            return true;
        }
        ...
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                ...
            }
            break;

            case MotionEvent.ACTION_POINTER_DOWN: {
              ...
            }
            break;

            case MotionEvent.ACTION_MOVE: {
              ...
            }
            break;
            case MotionEvent.ACTION_POINTER_UP: {
               ...
            }
            break;
            case MotionEvent.ACTION_UP: {
               ....
            }
            break;
            case MotionEvent.ACTION_CANCEL: {
                cancelScroll();
            }
            break;
        }
        ..
        return true;
    }
``` 
其中ACTION_DOWN、ACTION_MOVE、ACTION_UP和ACTION_CANCEL这几个事件，这是View最基本的事件。
可能有人对ACTION_POINTER_DOWN和ACTION_POINTER_UP事件比较陌生，这两个事件就跟多指滑动有关，也是本文重点分析之一。

在分析源码之前，我先将上面的代码做一个简单的概述。
1 如果当前的mActiveOnItemTouchListener需要消耗当前事件，那么优先交给它处理。
2 如果mActiveOnItemTouchListener不消耗当前事件，那么就走正常的事件分发机制。这里面有很多的细节，稍后我会详细的介绍。

关于第一步，这里不用我来解释，它就是一个Listener的回调，非常的简单，我们重点的在于分析第二步。

(1). Down 事件
我们先来看看这部分的代码吧。
```
  case MotionEvent.ACTION_DOWN: {
                mScrollPointerId = e.getPointerId(0);
                mInitialTouchX = mLastTouchX = (int) (e.getX() + 0.5f);
                mInitialTouchY = mLastTouchY = (int) (e.getY() + 0.5f);

                int nestedScrollAxis = ViewCompat.SCROLL_AXIS_NONE;
                if (canScrollHorizontally) {
                    nestedScrollAxis |= ViewCompat.SCROLL_AXIS_HORIZONTAL;
                }
                if (canScrollVertically) {
                    nestedScrollAxis |= ViewCompat.SCROLL_AXIS_VERTICAL;
                }
                startNestedScroll(nestedScrollAxis, TYPE_TOUCH);
            }
            break;
```
这里主要是做了两件事。
1 记录下Down事件的x、y坐标。
2 调用startNestedScroll方法，询问父View是否处理事件

Down事件还是比较简单，通常来说就一些初始化的事情


(2). Move事件
```
case MotionEvent.ACTION_MOVE: {
                final int index = e.findPointerIndex(mScrollPointerId);
                if (index < 0) {                 
                    return false;
                }

                final int x = (int) (e.getX(index) + 0.5f);
                final int y = (int) (e.getY(index) + 0.5f);
                int dx = mLastTouchX - x;
                int dy = mLastTouchY - y;

                if (mScrollState != SCROLL_STATE_DRAGGING) {
                    boolean startScroll = false;
                    if (canScrollHorizontally) {
                        if (dx > 0) {
                            dx = Math.max(0, dx - mTouchSlop);
                        } else {
                            dx = Math.min(0, dx + mTouchSlop);
                        }
                        if (dx != 0) {
                            startScroll = true;
                        }
                    }
                    if (canScrollVertically) {
                        if (dy > 0) {
                            dy = Math.max(0, dy - mTouchSlop);
                        } else {
                            dy = Math.min(0, dy + mTouchSlop);
                        }
                        if (dy != 0) {
                            startScroll = true;
                        }
                    }
                    if (startScroll) {
                        setScrollState(SCROLL_STATE_DRAGGING);
                    }
                }

                if (mScrollState == SCROLL_STATE_DRAGGING) {
                    mReusableIntPair[0] = 0;
                    mReusableIntPair[1] = 0;
                    if (dispatchNestedPreScroll(
                            canScrollHorizontally ? dx : 0,
                            canScrollVertically ? dy : 0,
                            mReusableIntPair, mScrollOffset, TYPE_TOUCH
                    )) {
                        dx -= mReusableIntPair[0];
                        dy -= mReusableIntPair[1];
                        // Updated the nested offsets
                        mNestedOffsets[0] += mScrollOffset[0];
                        mNestedOffsets[1] += mScrollOffset[1];
                        // Scroll has initiated, prevent parents from intercepting
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }

                    mLastTouchX = x - mScrollOffset[0];
                    mLastTouchY = y - mScrollOffset[1];

                    if (scrollByInternal(
                            canScrollHorizontally ? dx : 0,
                            canScrollVertically ? dy : 0,
                            e, TYPE_TOUCH)) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    if (mGapWorker != null && (dx != 0 || dy != 0)) {
                        mGapWorker.postFromTraversal(this, dx, dy);
                    }
                }
            }
            break;
            
            
boolean scrollByInternal(int x, int y, MotionEvent ev, int type) {
        int unconsumedX = 0;
        int unconsumedY = 0;
        int consumedX = 0;
        int consumedY = 0;

        consumePendingUpdateOperations();
        if (mAdapter != null) {
            mReusableIntPair[0] = 0;
            mReusableIntPair[1] = 0;
            scrollStep(x, y, mReusableIntPair);
            consumedX = mReusableIntPair[0];
            consumedY = mReusableIntPair[1];
            unconsumedX = x - consumedX;
            unconsumedY = y - consumedY;
        }
        if (!mItemDecorations.isEmpty()) {
            invalidate();
        }

        mReusableIntPair[0] = 0;
        mReusableIntPair[1] = 0;
        dispatchNestedScroll(consumedX, consumedY, unconsumedX, unconsumedY, mScrollOffset,
                type, mReusableIntPair);
        unconsumedX -= mReusableIntPair[0];
        unconsumedY -= mReusableIntPair[1];
        boolean consumedNestedScroll = mReusableIntPair[0] != 0 || mReusableIntPair[1] != 0;

        // Update the last touch co-ords, taking any scroll offset into account
        mLastTouchX -= mScrollOffset[0];
        mLastTouchY -= mScrollOffset[1];
        mNestedOffsets[0] += mScrollOffset[0];
        mNestedOffsets[1] += mScrollOffset[1];

        if (getOverScrollMode() != View.OVER_SCROLL_NEVER) {
            if (ev != null && !MotionEventCompat.isFromSource(ev, InputDevice.SOURCE_MOUSE)) {
                pullGlows(ev.getX(), unconsumedX, ev.getY(), unconsumedY);
            }
            considerReleasingGlowsOnScroll(x, y);
        }
        if (consumedX != 0 || consumedY != 0) {
            dispatchOnScrolled(consumedX, consumedY);
        }
        if (!awakenScrollBars()) {
            invalidate();
        }
        return consumedNestedScroll || consumedX != 0 || consumedY != 0;
    }
    
   void scrollStep(int dx, int dy, @Nullable int[] consumed) {
        startInterceptRequestLayout();
        onEnterLayoutOrScroll();

        TraceCompat.beginSection(TRACE_SCROLL_TAG);
        fillRemainingScrollValues(mState);

        int consumedX = 0;
        int consumedY = 0;
        if (dx != 0) {
            consumedX = mLayout.scrollHorizontallyBy(dx, mRecycler, mState);
        }
        if (dy != 0) {
            consumedY = mLayout.scrollVerticallyBy(dy, mRecycler, mState);
        }

        TraceCompat.endSection();
        repositionShadowingViews();

        onExitLayoutOrScroll();
        stopInterceptRequestLayout(false);

        if (consumed != null) {
            consumed[0] = consumedX;
            consumed[1] = consumedY;
        }
    }  
   //LinearLayoutManager 
   public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler,
            RecyclerView.State state) {
        if (mOrientation == VERTICAL) {
            return 0;
        }
        return scrollBy(dx, recycler, state);
    }
  int scrollBy(int delta, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getChildCount() == 0 || delta == 0) {
            return 0;
        }
        ensureLayoutState();
        mLayoutState.mRecycle = true;
        final int layoutDirection = delta > 0 ? LayoutState.LAYOUT_END : LayoutState.LAYOUT_START;
        final int absDelta = Math.abs(delta);
        updateLayoutState(layoutDirection, absDelta, true, state);
        //调用fill进行填充展示在客户面前的view
        final int consumed = mLayoutState.mScrollingOffset
                + fill(recycler, mLayoutState, state, false);
        if (consumed < 0) {
            return 0;
        }
        final int scrolled = absDelta > consumed ? layoutDirection * consumed : delta;
        mOrientationHelper.offsetChildren(-scrolled);
        mLayoutState.mLastScrollDelta = scrolled;
        return scrolled;
    }    
  //mOrientationHelper  
 public void offsetChildren(int amount) {
    this.mLayoutManager.offsetChildrenVertical(amount);
}   
//RecyclerView
  public void offsetChildrenVertical(@Px int dy) {
            if (mRecyclerView != null) {
                mRecyclerView.offsetChildrenVertical(dy);
            }
        } 
          
   public void offsetChildrenVertical(@Px int dy) {
        final int childCount = mChildHelper.getChildCount();
        for (int i = 0; i < childCount; i++) {
            //调用view.offsetTopAndBottom    
            mChildHelper.getChildAt(i).offsetTopAndBottom(dy);
        }
    }                  
```
这部分代码非常的简单，我将它分为如下几步：
1 根据Move事件产生的x、y坐标来计算dx、dy。
2 调用dispatchNestedPreScroll询问父View是否优先处理滑动事件，如果要消耗，dx和dy分别会减去父View消耗的那部分距离。
3 然后根据情况来判断RecyclerView是垂直滑动还是水平滑动，最终是调用scrollByInternal方法来实现滑动的效果的。
4 调用GapWorker的postFromTraversal来预取ViewHolder。这个过程会走缓存机制部分的逻辑，同时也有可能会调用Adapter的
  onBindViewHolder方法来提前加载数据。

在scrollByInternal方法内部，实际上是调用了LayoutManager的scrollHorizontallyBy方法或者scrollVerticallyBy方法来实现的。
 LayoutManager这两个方法实际上也没有做什么比较骚的操作，归根结底，最终调用了就是调用了每个Child的offsetTopAndBottom
  或者offsetLeftAndRight方法来实现的
//todo view.offsetTopAndBottom

什么是预取 https://blog.csdn.net/tangedegushi/article/details/88790754
预取就是界面没有展示出来的元素，是下一个即将要展示出来的元素，比如界面展示的是10条目，那么当你滑动的时候，那么即将展示的将是第11个
（还没显示出来），这时候RecyclerView就会提前将需要展示的第一个元素缓存到mCachedViews中（RecyclerView四级缓存中的第二级缓存），
  mCachedViews中缓存的view是不要重新绑定的，也就说不会执行adapter的onBindViewHolder（）方法，这个功能是在版本21之后加的。

预取功能是在21及以上版本才有的，这个功能可以通过LayoutManager的setItemPrefetchEnabled(boolean enabled)去关闭的；
如果RecyclerView中还有子RecyclerView（并且是用LinearLayoutManager），那么子RecyclerView可以通过LinearLayoutManager的
  setInitialPrefetchItemCount（）去设置预取的个数；

GapWorker的初始化
```
 protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mLayoutOrScrollCounter = 0;
        mIsAttached = true;
        mFirstLayoutComplete = mFirstLayoutComplete && !isLayoutRequested();
        if (mLayout != null) {
            mLayout.dispatchAttachedToWindow(this);
        }
        mPostedAnimatorRunner = false;
        //static final boolean ALLOW_THREAD_GAP_WORK = Build.VERSION.SDK_INT >= 21
        if (ALLOW_THREAD_GAP_WORK) {
            // Register with gap worker
            mGapWorker = GapWorker.sGapWorker.get();
            if (mGapWorker == null) {
                mGapWorker = new GapWorker();

                //只查询一次频率，很昂贵(> 1ms)  todo查询频率可能涉及ipc 
                Display display = ViewCompat.getDisplay(this);
                float refreshRate = 60.0f;
                if (!isInEditMode() && display != null) {
                    float displayRefreshRate = display.getRefreshRate();
                    if (displayRefreshRate >= 30.0f) {
                        refreshRate = displayRefreshRate;
                    }
                }
                //每一帧的时间
                mGapWorker.mFrameIntervalNs = (long) (1000000000 / refreshRate);
                GapWorker.sGapWorker.set(mGapWorker);
            }
            mGapWorker.add(this);
        }
    }
```

我们就简单的分析一下GapWorker是怎么进行预取的。我们来看看postFromTraversal方法：
//GapWorker.java
```
 void postFromTraversal(RecyclerView recyclerView, int prefetchDx, int prefetchDy) {
        if (recyclerView.isAttachedToWindow()) {
            if (RecyclerView.DEBUG && !mRecyclerViews.contains(recyclerView)) {
                throw new IllegalStateException("attempting to post unregistered view!");
            }
            if (mPostTimeNs == 0) {
                mPostTimeNs = recyclerView.getNanoTime();
                recyclerView.post(this);
            }
        }
        recyclerView.mPrefetchRegistry.setPrefetchVector(prefetchDx, prefetchDy);
    }
```

在postFromTraversal方法内部也没有做多少事情，最核心在于调用了post方法，向任务队列里面添加了一个Runnable。看来重点的分析还是GapWorker的run方法:
```
public void run() {
        try {
            TraceCompat.beginSection(RecyclerView.TRACE_PREFETCH_TAG);
            if (mRecyclerViews.isEmpty()) {
                return;
            }
            final int size = mRecyclerViews.size();
            //上一次绘制的时间    //todo view.getDrawingTime()
            long latestFrameVsyncMs = 0;
            for (int i = 0; i < size; i++) {
                RecyclerView view = mRecyclerViews.get(i);
                if (view.getWindowVisibility() == View.VISIBLE) {
                    latestFrameVsyncMs = Math.max(view.getDrawingTime(), latestFrameVsyncMs);
                }
            }

            if (latestFrameVsyncMs == 0) {
                // abort - either no views visible, or couldn't get last vsync for estimating next
                return;
            }
            //mFrameIntervalNs 在RecyclerView.onAttachedToWindow中初始化
            //计算下一帧到来的时间，在这个时间内没有预取到那么就会预取失败，预取的本意就是为了滑动更流畅，如果预取在
			//下一帧到来时还没取到，还去取的话那么就会影响到绘制，得不偿失，
            long nextFrameNs = TimeUnit.MILLISECONDS.toNanos(latestFrameVsyncMs) + mFrameIntervalNs;

            prefetch(nextFrameNs);

        } finally {
            mPostTimeNs = 0;
            TraceCompat.endSection();
        }
    }
```
run方法的逻辑也是非常简单，首先计算获得下一帧的时间，然后调用prefetch方法进行预取ViewHolder   //todo prefetchRegistry预拉取的其他信息
```
    void prefetch(long deadlineNs) {
        buildTaskList();
        flushTasksWithDeadline(deadlineNs);
    }
     private void buildTaskList() {
        // Update PrefetchRegistry in each view
        final int viewCount = mRecyclerViews.size();
        int totalTaskCount = 0;
        //计算有多少个可见的RecyclerView
        for (int i = 0; i < viewCount; i++) {
            RecyclerView view = mRecyclerViews.get(i);
            if (view.getWindowVisibility() == View.VISIBLE) {
            //计算需要预取条目的位置，最终会调用到addPosition（），将位置信息保存到mPrefetchArray数组中
                view.mPrefetchRegistry.collectPrefetchPositionsFromView(view, false);
                totalTaskCount += view.mPrefetchRegistry.mCount;
            }
        }

        // Populate task list from prefetch data...
        mTasks.ensureCapacity(totalTaskCount);
        int totalTaskIndex = 0;
        for (int i = 0; i < viewCount; i++) {
            RecyclerView view = mRecyclerViews.get(i);
            if (view.getWindowVisibility() != View.VISIBLE) {
                // Invisible view, don't bother prefetching
                continue;
            }

            LayoutPrefetchRegistryImpl prefetchRegistry = view.mPrefetchRegistry;
            final int viewVelocity = Math.abs(prefetchRegistry.mPrefetchDx)
                    + Math.abs(prefetchRegistry.mPrefetchDy);
             //创建预取条目的task
            //mCount是当前预取位置的个数（比如当前可见的RecyclerView有两个，那么mCount就为2），这里*2是因为保存位置
            //的数组不仅保存了位置，还保存了到预取位置的距离
            for (int j = 0; j < prefetchRegistry.mCount * 2; j += 2) {
                final Task task;
                if (totalTaskIndex >= mTasks.size()) {
                    task = new Task();
                    mTasks.add(task);
                } else {
                    task = mTasks.get(totalTaskIndex);
                }
                //当前可见item到预取位置的距离
                final int distanceToItem = prefetchRegistry.mPrefetchArray[j + 1];
				//表示这个预取的item在下一帧是否会显示，通常为false，表示在下一帧不显示，为true就说明在下一帧是会显示的
                task.immediate = distanceToItem <= viewVelocity;
                //滑动的距离
                task.viewVelocity = viewVelocity;
                //到预取位置的距离
                task.distanceToItem = distanceToItem;
                //预取item的RecyclerView
                task.view = view;
                //预取item所处的位置（position）
                task.position = prefetchRegistry.mPrefetchArray[j];
				//预取的总个数
                totalTaskIndex++;
            }
        }

        // ... and priority sort
        //对需要预取的task进行排序，immediate =true的将会排在前面，这是因为immediate =true的将会在下一帧显示
        Collections.sort(mTasks, sTaskComparator);
    }
   
    
  private void flushTasksWithDeadline(long deadlineNs) {
        for (int i = 0; i < mTasks.size(); i++) {
            final Task task = mTasks.get(i);
            if (task.view == null) {
                break; // done with populated tasks
            }
            flushTaskWithDeadline(task, deadlineNs);
            task.clear();
        }
    }
    private void flushTaskWithDeadline(Task task, long deadlineNs) {
        long taskDeadlineNs = task.immediate ? RecyclerView.FOREVER_NS : deadlineNs;
        RecyclerView.ViewHolder holder = prefetchPositionWithDeadline(task.view,
                task.position, taskDeadlineNs);
        if (holder != null
                && holder.mNestedRecyclerView != null
                && holder.isBound()
                && !holder.isInvalid()) {
            prefetchInnerRecyclerViewWithDeadline(holder.mNestedRecyclerView.get(), deadlineNs);
        }
    }
 private void prefetchInnerRecyclerViewWithDeadline(@Nullable RecyclerView innerView,
            long deadlineNs) {
        if (innerView == null) {
            return;
        }
        if (innerView.mDataSetHasChangedAfterLayout
                && innerView.mChildHelper.getUnfilteredChildCount() != 0) {
            innerView.removeAndRecycleViews();
        }

        final LayoutPrefetchRegistryImpl innerPrefetchRegistry = innerView.mPrefetchRegistry;
        innerPrefetchRegistry.collectPrefetchPositionsFromView(innerView, true);

        if (innerPrefetchRegistry.mCount != 0) {
            try {
                TraceCompat.beginSection(RecyclerView.TRACE_NESTED_PREFETCH_TAG);
                innerView.mState.prepareForNestedPrefetch(innerView.mAdapter);
                for (int i = 0; i < innerPrefetchRegistry.mCount * 2; i += 2) {
                     //得到对应的position
                    final int innerPosition = innerPrefetchRegistry.mPrefetchArray[i];
                    prefetchPositionWithDeadline(innerView, innerPosition, deadlineNs);
                }
            } finally {
                TraceCompat.endSection();
            }
        }
    }  
private RecyclerView.ViewHolder prefetchPositionWithDeadline(RecyclerView view,
            int position, long deadlineNs) {
        if (isPrefetchPositionAttached(view, position)) {
            // don't attempt to prefetch attached views
            return null;
        }

        RecyclerView.Recycler recycler = view.mRecycler;
        RecyclerView.ViewHolder holder;
        try {
            view.onEnterLayoutOrScroll();
            holder = recycler.tryGetViewHolderForPositionByDeadline(
                    position, false, deadlineNs);

            if (holder != null) {
                if (holder.isBound() && !holder.isInvalid()) {                  
                    recycler.recycleView(holder.itemView);
                } else {
                   // 未绑定，因此无法缓存view，但它将保留在池中，直到下一次预取/traversal。如果一个视图绑定失败，
                    //这意味着我们在截止日期之前没有足够的时间（对于这种类型的其他实例，在这个GapWorker预取过程中也没有足够的时间）
                    recycler.addViewHolderToRecycledViewPool(holder, false);
                }
            }
        } finally {
            view.onExitLayoutOrScroll(false);
        }
        return holder;
    }              
```

prefetch方法也简单，显示调用buildTaskList方法生成任务队列，然后调用flushTasksWithDeadline来执行task,
 这其中会调用RecyclerView的tryGetViewHolderForPositionByDeadline方法来获取一个ViewHolder

Up事件和Cancel事件，这两个事件更加的简单，都进行一些清理的操作，这里就不分析了。不过在Up事件里面，有一个特殊事件可能会产生--fling事件，
  待会我们会详细的分析。
```
case MotionEvent.ACTION_CANCEL: {
                cancelScroll();
            } break;
            
private void cancelScroll() {
    resetScroll();
    setScrollState(SCROLL_STATE_IDLE);
}            
```

2. 多指滑动
   大家千万不会误会这里多指滑动的意思，这里的多指滑动不是指RecyclerView能够相应多根手指的滑动，而是指当一个手指还没释放时，
   此时另一个手指按下，此时RecyclerView就不相应上一个手指的手势，而是相应最近按下手指的手势。
   我们来看看这部分的代码：
```
           case MotionEvent.ACTION_POINTER_DOWN: {
            mScrollPointerId = e.getPointerId(actionIndex);
            mInitialTouchX = mLastTouchX = (int) (e.getX(actionIndex) + 0.5f);
            mInitialTouchY = mLastTouchY = (int) (e.getY(actionIndex) + 0.5f);
        } break;
```
当另一个手指按下时，此时就会立即更新按下的坐标，同时会更新mScrollPointerId,表示后面只会响应最近按下手指的手势。
其次，我们来看看多指松开的情况：
```
 case MotionEvent.ACTION_POINTER_UP: {
                onPointerUp(e);
            } break;
 
   private void onPointerUp(MotionEvent e) {
        final int actionIndex = e.getActionIndex();
        if (e.getPointerId(actionIndex) == mScrollPointerId) {
            // Pick a new pointer to pick up the slack.
            final int newIndex = actionIndex == 0 ? 1 : 0;
            mScrollPointerId = e.getPointerId(newIndex);
            mInitialTouchX = mLastTouchX = (int) (e.getX(newIndex) + 0.5f);
            mInitialTouchY = mLastTouchY = (int) (e.getY(newIndex) + 0.5f);
        }
    }            
```
在这里也没有比较骚的操作，就是普通的更新


3. fling滑动
   我们先来看看fling滑动产生的地方，也是Up事件的地方：
```
case MotionEvent.ACTION_UP: {
    mVelocityTracker.addMovement(vtev);
    eventAddedToVelocityTracker = true;
    mVelocityTracker.computeCurrentVelocity(1000, mMaxFlingVelocity);
    final float xvel = canScrollHorizontally
            ? -mVelocityTracker.getXVelocity(mScrollPointerId) : 0;
    final float yvel = canScrollVertically
            ? -mVelocityTracker.getYVelocity(mScrollPointerId) : 0;
    if (!((xvel != 0 || yvel != 0) && fling((int) xvel, (int) yvel))) {
        setScrollState(SCROLL_STATE_IDLE);
    }
    resetScroll();
} break;
```   

从上面的代码中，我们可以看出来，最终是调用fling方法来是实现fling效果的,我们来看看fling方法：
fling [flɪŋ]  猛扔;(尤指生气地)扔，掷，抛，丢;猛动(身体或身体部位)
```
 public boolean fling(int velocityX, int velocityY) {
        ...
        final boolean canScrollHorizontal = mLayout.canScrollHorizontally();
        final boolean canScrollVertical = mLayout.canScrollVertically();

        if (!canScrollHorizontal || Math.abs(velocityX) < mMinFlingVelocity) {
            velocityX = 0;
        }
        if (!canScrollVertical || Math.abs(velocityY) < mMinFlingVelocity) {
            velocityY = 0;
        }
        if (velocityX == 0 && velocityY == 0) {
            // If we don't have any velocity, return false
            return false;
        }

        if (!dispatchNestedPreFling(velocityX, velocityY)) {
            final boolean canScroll = canScrollHorizontal || canScrollVertical;
            dispatchNestedFling(velocityX, velocityY, canScroll);

            if (mOnFlingListener != null && mOnFlingListener.onFling(velocityX, velocityY)) {
                return true;
            }

            if (canScroll) {
                int nestedScrollAxis = ViewCompat.SCROLL_AXIS_NONE;
                if (canScrollHorizontal) {
                    nestedScrollAxis |= ViewCompat.SCROLL_AXIS_HORIZONTAL;
                }
                if (canScrollVertical) {
                    nestedScrollAxis |= ViewCompat.SCROLL_AXIS_VERTICAL;
                }
                startNestedScroll(nestedScrollAxis, TYPE_NON_TOUCH);

                velocityX = Math.max(-mMaxFlingVelocity, Math.min(velocityX, mMaxFlingVelocity));
                velocityY = Math.max(-mMaxFlingVelocity, Math.min(velocityY, mMaxFlingVelocity));
                mViewFlinger.fling(velocityX, velocityY);
                return true;
            }
        }
        return false;
    }
```
在fling方法里面,显示调用dispatchNestedPreFling方法询问父View是否处理fling事件，最后调用ViewFlinger的fling方法来实现fling效果,
  所以真正的核心在于ViewFlinger的fling方法里面，我们继续来看：//todo mOverScroller
```
public void fling(int velocityX, int velocityY) {
            setScrollState(SCROLL_STATE_SETTLING);
            mLastFlingX = mLastFlingY = 0;
            if (mInterpolator != sQuinticInterpolator) {
                mInterpolator = sQuinticInterpolator;
                mOverScroller = new OverScroller(getContext(), sQuinticInterpolator);
            }
            mOverScroller.fling(0, 0, velocityX, velocityY,
                    Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
            postOnAnimation();
        }
```

在ViewFlinger的fling方法里面,先是调用了OverScroller的fling来计算fling相关的参数，包括fling的距离和fling的时间。
  这里就不深入的分析计算相关的代码，因为这里面都是一些数学和物理的计算。最后就是调用了postOnAnimation方法
//todo fling的计算
```
void postOnAnimation() {
            if (mEatRunOnAnimationRequest) {
                mReSchedulePostAnimationCallback = true;
            } else {
                internalPostOnAnimation();
            }
        }

        private void internalPostOnAnimation() {
            removeCallbacks(this);
            ViewCompat.postOnAnimation(RecyclerView.this, this);
        }
```
可能大家有可能看不懂上面的代码，其实跟View的post差不多,所以最终还是得看ViewFlinger的run方法。
ViewFlinger的run方法比较长，这里我将它简化了一下
```
        public void run() {
            ...
            final OverScroller scroller = mOverScroller;
            //1 更新滚动信息，并判断是否滚动完成 true,滚动未完成
            if (scroller.computeScrollOffset()) {
                ...
                if (mAdapter != null) {
                    mReusableIntPair[0] = 0;
                    mReusableIntPair[1] = 0;
                    //2 滚动特定距离
                    scrollStep(unconsumedX, unconsumedY, mReusableIntPair);
                    consumedX = mReusableIntPair[0];
                    consumedY = mReusableIntPair[1];
                    unconsumedX -= consumedX;
                    unconsumedY -= consumedY;
                ... 
                final boolean doneScrolling = scroller.isFinished()
                        || ((scrollerFinishedX || unconsumedX != 0)
                        && (scrollerFinishedY || unconsumedY != 0));

                SmoothScroller smoothScroller = mLayout.mSmoothScroller;
                boolean smoothScrollerPending =
                        smoothScroller != null && smoothScroller.isPendingInitialRun();
                //3 滚动完成
                if (!smoothScrollerPending && doneScrolling) {                 
                    if (getOverScrollMode() != View.OVER_SCROLL_NEVER) {
                        final int vel = (int) scroller.getCurrVelocity();
                        int velX = unconsumedX < 0 ? -vel : unconsumedX > 0 ? vel : 0;
                        int velY = unconsumedY < 0 ? -vel : unconsumedY > 0 ? vel : 0;
                        absorbGlows(velX, velY);
                    }

                    if (ALLOW_THREAD_GAP_WORK) {
                        mPrefetchRegistry.clearPrefetchPositions();
                    }
                } else {
                   //3 滚动没有完成 继续调用postOnAnimation递归执行滚动
                    postOnAnimation();
                    if (mGapWorker != null) {
                        mGapWorker.postFromTraversal(RecyclerView.this, consumedX, consumedY);
                    }
                }
            }
```
整个fling核心就在这里，通过上面的三步，最终就是实现了fling的效果，上面的注意已经非常的清晰了，这里就不继续分析了。
我们分析了RecyclerView的fling事件，有什么帮助呢？在日常的开发中，如果需要fling的效果，我们可以根据RecyclerView实现方式来实现，
  是不是就觉得非常简单呢？对的，这就是我们学习源码的目的，不仅要理解其中的原理，还需要学以致用😂。

4. 总结
   RecyclerView的滑动机制相比较来说，还是非常简单，我也感觉没有什么可以总结。不过从RecyclerView的源码，我们可以学习两点：
1 多指滑动。我们可以根据RecyclerView的源码，来实现自己的多指滑动，这是一种参考，也是学以致用
2 fling滑动。RecyclerView实现了fling效果，在日常开发过程中，如果我们也需要实现这种效果，我们可以根据RecyclerView的源码来实现。

//todo https://zhuanlan.zhihu.com/p/594778527
