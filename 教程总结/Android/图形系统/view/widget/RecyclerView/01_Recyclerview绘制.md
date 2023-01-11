https://www.jianshu.com/p/61fe3f3bb7ec    androidx.recyclerview 1.2.0

1. 概述
   在分析RecyclerView源码之前，我们还是对RecyclerView有一个初步的了解，简单的了解它是什么，它的基本结构有哪些。
   RecyclerView是Google爸爸在2014年的IO大会提出来
   RecyclerView本身是一个展示大量数据的控件，相比较ListView,RecyclerView的4级缓存(也有人说是3级缓存，这些都不重要😂)就表现的非常出色，
   在性能方面相比于ListView提升了不少。同时由于LayoutManager的存在,让RecyclerView不仅有ListView的特点，同时兼有GridView的特点。
     这可能是RecyclerView受欢迎的原因之一吧。
   RecyclerView在设计方面上也是非常的灵活，不同的部分承担着不同的职责。其中Adapter负责提供数据，包括创建ViewHolder和绑定数据，
   LayoutManager负责ItemView的测量和布局,ItemAnimator负责每个ItemView的动画，ItemDecoration负责每个ItemView的间隙。
   这种插拔式的架构使得RecyclerView变得非常的灵活，每一个人都可以根据自身的需求来定义不同的部分。
   正因为这种插拔式的设计，使得RecyclerView在使用上相比较于其他的控件稍微难那么一点点，不过这都不算事，谁叫RecyclerView这么惹人爱呢😂。

RV相关状态
```
public class RecyclerView{
  //保存view信息用于动画
  final ViewInfoStore mViewInfoStore；
  //第一次布局完成  执行完onLayout置为true
  boolean mFirstLayoutComplete;
  //这是用来表示我们是否应推迟由RecyclerView的子级的布局请求引起的布局操作
  private int mInterceptRequestLayoutDepth = 0;
  //如果对requestLayout的调用被截获并阻止正常执行，并且我们计划稍后继续正常执行，则为True。 
  boolean mLayoutWasDefered;
  //告诉此RecyclerView抑制所有布局和滚动调用，直到稍后调用suppressLayout（false）禁用布局抑制。
  //一般用于滚动列表嵌套，例如scrollView嵌套recyclerview，只滑动scrollview，设置recyclerview.supressLayout(true)
  //使用后adapter失效，择时调用supressLayout(false)
  boolean mLayoutSuppressed;

    //用来记录RV的children在onLayout中是否需要布局和测量
  
    //我们跟踪此信息是因为存在一个优化路径，当LayoutManager#isAutoMeasureEnabled（）返回true时，
      //当RV的维度都是MeasureSpec.EXACTLY，我们将跳过测量和布局child，直到布局阶段。
    //然而，有些时候，我们第一次被测量是用EXACTLY以外的模式测量。在这种情况下，我们在onMeasure期间测量和布局chilldren。然后，
    //如果我们再次被使用EXACTLY测量，我们跳过测量，我们将得到一个不同于我们上次意识到被测量的尺寸。如果发生这种情况，我们不检查它，
    //我们可能不会重新测量children，这将是一个错误
    
    //mLastAutoMeasureNonExactMeasureResult跟踪我们在本例中最后已知的测量结果，以及mLastAutoMeasureSkippedDueToExact跟踪我们是否跳过。
    //因此，无论何时我们布局，我们都可以看到我们最后已知的测量信息是否与实际布局的大小不同，如果是，只有这样我们才能重新测量和重新布置孩子。
    private boolean mLastAutoMeasureSkippedDueToExact;
    //记录onMeasure自动测量的情况下 非Exact的宽度和高度
    private int mLastAutoMeasureNonExactMeasuredWidth = 0;
    private int mLastAutoMeasureNonExactMeasuredHeight = 0;
    
    //滚动的边缘效果  //todo EdgeEffect以及动画相关原理
    private EdgeEffectFactory mEdgeEffectFactory = new EdgeEffectFactory();
    private EdgeEffect mLeftGlow, mTopGlow, mRightGlow, mBottomGlow;

  public static class State {
       //recyclerView的状态主要是STEP_START，STEP_LAYOUT，STEP_ANIMATIONS
        static final int STEP_START = 1;
        static final int STEP_LAYOUT = 1 << 1;
        static final int STEP_ANIMATIONS = 1 << 2;
     // 预布局 动画开始前进行预布局
     boolean mInPreLayout = false;
     //处于测量中  
     boolean mIsMeasuring = false;  
     
     //运行简单动画   简单动画是高级动画的子集（这将导致预布局步骤）
     boolean mRunSimpleAnimations = false;
     //运行可预测动画  可能产生预布局步骤
     boolean mRunPredictiveAnimations = false; 
 }
}
```
方法名	                 作用
dispatchLayoutStep1	三大dispatchLayoutStep方法第一步。本方法的作用主要有三点：
                    1.处理Adapter更新;
                    2.决定是否执行ItemAnimator;
                    3.保存ItemView的动画信息。
                    本方法也被称为preLayout(预布局)，当Adapter更新了，这个方法会保存每个ItemView的旧信息(oldViewHolderInfo)
dispatchLayoutStep2	三大dispatchLayoutStep方法第二步。在这个方法里面，真正进行children的布局。
dispatchLayoutStep3	三大dispatchLayoutStep方法第三步。这个方法的作用执行在dispatchLayoutStep1方法里面保存的动画信息并执行需要的清理工作
                    这个阶段也叫做postLayout


2. onMeasure
   不管RecyclerView是多么神奇，它也是一个View，所以分析它的三大流程是非常有必要的。同时，如果了解过RecyclerView的同学应该都知道，
   RecyclerView的三大流程跟普通的View比较，有很大的不同。
   首先，我们来看看measure过程，来看看RecyclerView的onMeasure方法
```
protected void onMeasure(int widthSpec, int heightSpec) {
        //1  layoutManager为空,执行defaultOnMeasure
        if (mLayout == null) {
            defaultOnMeasure(widthSpec, heightSpec);
            return;
        }
        //2 layoutManager开启了自动测量
        if (mLayout.isAutoMeasureEnabled()) {
           ...
        } else {
          //3 layoutManager没有开启自动测量
           ...
        }
    }
```   
//onMeasure1 LayoutManager为空
```
//默认测量
void defaultOnMeasure(int widthSpec, int heightSpec) {
        final int width = LayoutManager.chooseSize(widthSpec,
                getPaddingLeft() + getPaddingRight(),
                ViewCompat.getMinimumWidth(this));
        final int height = LayoutManager.chooseSize(heightSpec,
                getPaddingTop() + getPaddingBottom(),
                ViewCompat.getMinimumHeight(this));
        setMeasuredDimension(width, height);
    }
 //LayoutManager  根据不同的模式spec 选择对应的尺寸      
 public static int chooseSize(int spec, int desired, int min) {
            final int mode = View.MeasureSpec.getMode(spec);
            final int size = View.MeasureSpec.getSize(spec);
            switch (mode) {
                case View.MeasureSpec.EXACTLY:
                    return size;
                case View.MeasureSpec.AT_MOST:
                    return Math.min(size, Math.max(desired, min));
                case View.MeasureSpec.UNSPECIFIED:
                default:
                    return Math.max(desired, min);
            }
        }    
```

//onMeasure 2 layoutManager开启了自动测量
```
...
if (mLayout.isAutoMeasureEnabled()) {
            final int widthMode = MeasureSpec.getMode(widthSpec);
            final int heightMode = MeasureSpec.getMode(heightSpec);
            //1
            mLayout.onMeasure(mRecycler, mState, widthSpec, heightSpec);
            
            //决定和记录是否跳过测量 当widthMode和heightMode都是EXACTLY   
            mLastAutoMeasureSkippedDueToExact =
                    widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY;
            if (mLastAutoMeasureSkippedDueToExact || mAdapter == null) {
                return;
            }
            //2
            if (mState.mLayoutStep == State.STEP_START) {
                dispatchLayoutStep1();
            }  
            mLayout.setMeasureSpecs(widthSpec, heightSpec);
            mState.mIsMeasuring = true;
            dispatchLayoutStep2();
            mLayout.setMeasuredDimensionFromChildren(widthSpec, heightSpec);
            //3
            if (mLayout.shouldMeasureTwice()) {
                mLayout.setMeasureSpecs(
                        MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY));
                mState.mIsMeasuring = true;
                dispatchLayoutStep2();
                mLayout.setMeasuredDimensionFromChildren(widthSpec, heightSpec);
            }
            //记录自动测量的情况下 非Exact的宽度和高度
            mLastAutoMeasureNonExactMeasuredWidth = getMeasuredWidth();
            mLastAutoMeasureNonExactMeasuredHeight = getMeasuredHeight();
           ... 
        }
   //LayoutManager     
   public void onMeasure(@NonNull Recycler recycler, @NonNull State state, int widthSpec,
                int heightSpec) {
            mRecyclerView.defaultOnMeasure(widthSpec, heightSpec);
        }       
```
这段代码分为三步。我们来看看：
1 调用LayoutManager的onMeasure方法进行测量。 最终调用RecyclerView的defaultOnMeasure进行测量
2 如果mState.mLayoutStep为State.STEP_START的话，那么就会执行dispatchLayoutStep1方法，然后会执行dispatchLayoutStep2方法。
3 如果需要第二次测量的话，会再一次调用dispatchLayoutStep2 方法。
  对于二次测量的情况 
     如果RecyclerView的宽度和高度是non-exact，并且至少有一个孩子它的宽度和高度是non-exact，我们必须重新测量
     比如父子尺寸属性互相依赖的情况，要改变参数重新进行一次  父是wrap_content,子是match_content

dispatchLayoutStep1解析
```
private void dispatchLayoutStep1() {
     mState.assertLayoutStep(State.STEP_START);
        fillRemainingScrollValues(mState);
        mState.mIsMeasuring = false;
        startInterceptRequestLayout();
        mViewInfoStore.clear();
        onEnterLayoutOrScroll();
        //处理adapter更新并决定执行哪种动画 简单/可预测
        processAdapterUpdatesAndSetAnimationFlags();
        saveFocusInfo();
        mState.mTrackOldChangeHolders = mState.mRunSimpleAnimations && mItemsChanged;
        mItemsAddedOrRemoved = mItemsChanged = false;
        mState.mInPreLayout = mState.mRunPredictiveAnimations;
        mState.mItemCount = mAdapter.getItemCount();
        findMinMaxChildLayoutPositions(mMinMaxLayoutPositions);

        if (mState.mRunSimpleAnimations) {
            // 找到没有被remove的ItemView,保存ViewInfoStore的OldViewHolder信息，准备预布局
            int count = mChildHelper.getChildCount();
            for (int i = 0; i < count; ++i) {
                final ViewHolder holder = getChildViewHolderInt(mChildHelper.getChildAt(i));
                if (holder.shouldIgnore() || (holder.isInvalid() && !mAdapter.hasStableIds())) {
                    continue;
                }
                final ItemHolderInfo animationInfo = mItemAnimator
                        .recordPreLayoutInformation(mState, holder,
                                ItemAnimator.buildAdapterChangeFlagsForAnimations(holder),
                                holder.getUnmodifiedPayloads());
                mViewInfoStore.addToPreLayout(holder, animationInfo);
                if (mState.mTrackOldChangeHolders && holder.isUpdated() && !holder.isRemoved()
                        && !holder.shouldIgnore() && !holder.isInvalid()) {
                    long key = getChangedHolderKey(holder);                   
                    mViewInfoStore.addToOldChangeHolders(key, holder);
                }
            }
        }
        if (mState.mRunPredictiveAnimations) {
          // 进行预布局
            saveOldPositions();
            final boolean didStructureChange = mState.mStructureChanged;
            mState.mStructureChanged = false;
            mLayout.onLayoutChildren(mRecycler, mState);
            mState.mStructureChanged = didStructureChange;

            for (int i = 0; i < mChildHelper.getChildCount(); ++i) {
                final View child = mChildHelper.getChildAt(i);
                final ViewHolder viewHolder = getChildViewHolderInt(child);
                if (viewHolder.shouldIgnore()) {
                    continue;
                }
                if (!mViewInfoStore.isInPreLayout(viewHolder)) {
                    int flags = ItemAnimator.buildAdapterChangeFlagsForAnimations(viewHolder);
                    boolean wasHidden = viewHolder
                            .hasAnyOfTheFlags(ViewHolder.FLAG_BOUNCED_FROM_HIDDEN_LIST);
                    if (!wasHidden) {
                        flags |= ItemAnimator.FLAG_APPEARED_IN_PRE_LAYOUT;
                    }
                    final ItemHolderInfo animationInfo = mItemAnimator.recordPreLayoutInformation(
                            mState, viewHolder, flags, viewHolder.getUnmodifiedPayloads());
                    if (wasHidden) {
                        recordAnimationInfoIfBouncedHiddenView(viewHolder, animationInfo);
                    } else {
                        mViewInfoStore.addToAppearedInPreLayoutHolders(viewHolder, animationInfo);
                    }
                }
            }
            clearOldPositions();
        } else {
            clearOldPositions();
        }
        onExitLayoutOrScroll();
        stopInterceptRequestLayout(false);
        mState.mLayoutStep = State.STEP_LAYOUT;
}    
```
主要有三点  主要与动画相关，在动画专题详细分析
1.处理Adapter更新以及执行哪种动画;
2.决定是否执行ItemAnimator;
3.保存ItemView的动画信息到ViewInfoStore


dispatchLayoutStep2 分析
```
 private void dispatchLayoutStep2() {
        startInterceptRequestLayout();
        onEnterLayoutOrScroll();
        mState.assertLayoutStep(State.STEP_LAYOUT | State.STEP_ANIMATIONS);
        mAdapterHelper.consumeUpdatesInOnePass();
        mState.mItemCount = mAdapter.getItemCount();
        mState.mDeletedInvisibleItemCountSincePreviousLayout = 0;
        if (mPendingSavedState != null && mAdapter.canRestoreState()) {
            if (mPendingSavedState.mLayoutState != null) {
                mLayout.onRestoreInstanceState(mPendingSavedState.mLayoutState);
            }
            mPendingSavedState = null;
        }
        // Step 2: Run layout
        mState.mInPreLayout = false;
        mLayout.onLayoutChildren(mRecycler, mState);

        mState.mStructureChanged = false;

        // onLayoutChildren may have caused client code to disable item animations; re-check
        mState.mRunSimpleAnimations = mState.mRunSimpleAnimations && mItemAnimator != null;
        mState.mLayoutStep = State.STEP_ANIMATIONS;
        onExitLayoutOrScroll();
        stopInterceptRequestLayout(false);
    }
```
主要调用了LayoutManager的onLayoutChildren对child进行布局
  onLayoutChildren是LayoutManager 必须要重写的方法，默认为空，后面分析LinearLayoutManager



//onMeasure 3 layoutManager没有开启自动测量
```
   protected void onMeasure(int widthSpec, int heightSpec) {
   ...
        //1
        if (mHasFixedSize) {
            mLayout.onMeasure(mRecycler, mState, widthSpec, heightSpec);
            return;
        }
        //2
        // custom onMeasure
        if (mAdapterUpdateDuringMeasure) {
            startInterceptRequestLayout();
            onEnterLayoutOrScroll();
            processAdapterUpdatesAndSetAnimationFlags();
            onExitLayoutOrScroll();
    
            if (mState.mRunPredictiveAnimations) {
                mState.mInPreLayout = true;
            } else {
                // consume remaining updates to provide a consistent state with the layout pass.
                mAdapterHelper.consumeUpdatesInOnePass();
                mState.mInPreLayout = false;
            }
            mAdapterUpdateDuringMeasure = false;
            stopInterceptRequestLayout(false);
        } else if (mState.mRunPredictiveAnimations) { 
            //如果mAdapterUpdateDuringMeasure为false并且mRunPredictiveAnimations为true意味着：
            //onMeasure()已经调用过一次来处理adapter变更。 
            //当RV声明为layout_width=MATCH_PARENT并且是LinearLayout的子View,此时有可能发生两次onMeasure()调用
            // RV测量一个child时不能调用两次onMeasure()，因为getViewForPosition()会崩溃               
            setMeasuredDimension(getMeasuredWidth(), getMeasuredHeight());
            return;
        }
    
        if (mAdapter != null) {
            mState.mItemCount = mAdapter.getItemCount();
        } else {
            mState.mItemCount = 0;
        }
        startInterceptRequestLayout();
        mLayout.onMeasure(mRecycler, mState, widthSpec, heightSpec);
        stopInterceptRequestLayout(false);
        mState.mInPreLayout = false; // clear        
    }
```
这主要分为2步
1 如果mHasFixedSize为true(也就是调用了setHasFixedSize方法)，将直接调用LayoutManager的onMeasure方法进行测量
  默认为defaultOnMeasure()
2 如果mHasFixedSize为false，同时此时如果有数据更新，先处理数据更新的事务，然后调用LayoutManager的onMeasure方法进行测量


onLayout
```
 protected void onLayout(boolean changed, int l, int t, int r, int b) {
        TraceCompat.beginSection(TRACE_ON_LAYOUT_TAG);
        dispatchLayout();
        TraceCompat.endSection();
        mFirstLayoutComplete = true;
    }
void dispatchLayout() {
        //Adapter为空  不进行layout
        if (mAdapter == null) {
            return;
        }
        //LayoutManager为空 不进行layout
        if (mLayout == null) {
            return;
        }
        mState.mIsMeasuring = false;
        //在onMeasure中，RV跳过了测量和布局这些模式都是EXACTLY的children，但是现在模式变了，需要重新测量和布局
        boolean needsRemeasureDueToExactSkip = mLastAutoMeasureSkippedDueToExact
                        && (mLastAutoMeasureNonExactMeasuredWidth != getWidth()
                        || mLastAutoMeasureNonExactMeasuredHeight != getHeight());
        mLastAutoMeasureNonExactMeasuredWidth = 0;
        mLastAutoMeasureNonExactMeasuredHeight = 0;
        mLastAutoMeasureSkippedDueToExact = false;

        if (mState.mLayoutStep == State.STEP_START) {
            dispatchLayoutStep1();
            //设置模式为Exact
            mLayout.setExactMeasureSpecsFrom(this);
            dispatchLayoutStep2();
        } else if (mAdapterHelper.hasUpdates()
                || needsRemeasureDueToExactSkip
                || mLayout.getWidth() != getWidth()
                || mLayout.getHeight() != getHeight()) {
            //dispatchLayoutStep1(),dispatchLayoutStep2()已经在onMeasure()中调用过了，但是大小已经改变了
            //所以再次执行dispatchLayoutStep2()     
            mLayout.setExactMeasureSpecsFrom(this);
            dispatchLayoutStep2();
        } else {
            // always make sure we sync them (to ensure mode is exact)
            mLayout.setExactMeasureSpecsFrom(this);
        }
        dispatchLayoutStep3();   
}
```
onLayout主要是调用dispatchLayoutStep1(),dispatchLayoutStep2(),dispatchLayoutStep3()三个方法
并且在最后标记mFirstLayoutComplete为true

dispatchLayoutStep1(),dispatchLayoutStep2()上面已经看过了，现在分析dispatchLayoutStep3()
```
 private void dispatchLayoutStep3() {
        mState.assertLayoutStep(State.STEP_ANIMATIONS);
        startInterceptRequestLayout();
        onEnterLayoutOrScroll();
        //重置状态为STEP_START
        mState.mLayoutStep = State.STEP_START;
        if (mState.mRunSimpleAnimations) {
            //找出事物现在的位置，并处理变更动画。
            //反向遍历列表，因为我们可以在循环中调用animateChange，这可能会删除ViewHolder
            for (int i = mChildHelper.getChildCount() - 1; i >= 0; i--) {
                ViewHolder holder = getChildViewHolderInt(mChildHelper.getChildAt(i));
                if (holder.shouldIgnore()) {
                    continue;
                }
                long key = getChangedHolderKey(holder);
                final ItemHolderInfo animationInfo = mItemAnimator
                        .recordPostLayoutInformation(mState, holder);
                ViewHolder oldChangeViewHolder = mViewInfoStore.getFromOldChangeHolders(key);
                if (oldChangeViewHolder != null && !oldChangeViewHolder.shouldIgnore()) {
                    //运行一个变更动画 
                     //如果Item已更改，但更新的版本正在消失，则会创建冲突案例。
                    //由于标记为“消失”的视图可能会超出范围，因此我们运行变更动画。两个视图的动画完成后将自动清理。
                    //另一方面，如果它是同一个view holder实例，我们将运行一个正在消失的动画，因为我们不打算重新绑定更新的VH，
                     //除非layout manager强制执行                   
                    final boolean oldDisappearing = mViewInfoStore.isDisappearing(
                            oldChangeViewHolder);
                    final boolean newDisappearing = mViewInfoStore.isDisappearing(holder);
                    if (oldDisappearing && oldChangeViewHolder == holder) {
                        //执行消失动画而不是变更
                        mViewInfoStore.addToPostLayout(holder, animationInfo);
                    } else {
                        final ItemHolderInfo preInfo = mViewInfoStore.popFromPreLayout(
                                oldChangeViewHolder);
                        // we add and remove so that any post info is merged.
                        mViewInfoStore.addToPostLayout(holder, animationInfo);
                        ItemHolderInfo postInfo = mViewInfoStore.popFromPostLayout(holder);
                        if (preInfo == null) {
                            handleMissingPreInfoForChangeError(key, holder, oldChangeViewHolder);
                        } else {
                            animateChange(oldChangeViewHolder, holder, preInfo, postInfo,
                                    oldDisappearing, newDisappearing);
                        }
                    }
                } else {
                    mViewInfoStore.addToPostLayout(holder, animationInfo);
                }
            }

            //处理viewInfo list 并且触发动画
            mViewInfoStore.process(mViewInfoProcessCallback);
        }

        mLayout.removeAndRecycleScrapInt(mRecycler);
        mState.mPreviousLayoutItemCount = mState.mItemCount;
        mDataSetHasChangedAfterLayout = false;
        mDispatchItemsChangedEvent = false;
        mState.mRunSimpleAnimations = false;

        mState.mRunPredictiveAnimations = false;
        mLayout.mRequestedSimpleAnimations = false;
        if (mRecycler.mChangedScrap != null) {
            mRecycler.mChangedScrap.clear();
        }
        if (mLayout.mPrefetchMaxObservedInInitialPrefetch) {
           // 初始预取已扩展缓存，因此在下次预取之前重置。
           //这将防止初始预取永久扩展缓存。
            mLayout.mPrefetchMaxCountObserved = 0;
            mLayout.mPrefetchMaxObservedInInitialPrefetch = false;
            mRecycler.updateViewCacheSize();
        }

        mLayout.onLayoutCompleted(mState);
        onExitLayoutOrScroll();
        stopInterceptRequestLayout(false);
        mViewInfoStore.clear();
        if (didChildRangeChange(mMinMaxLayoutPositions[0], mMinMaxLayoutPositions[1])) {
            dispatchOnScrolled(0, 0);
        }
        recoverFocusFromState();
        resetFocusInfo();
    }
```



draw阶段
```
 public void draw(Canvas c) {
        //1
        super.draw(c);
        //2
        final int count = mItemDecorations.size();
        for (int i = 0; i < count; i++) {
            mItemDecorations.get(i).onDrawOver(c, this, mState);
        }
        //3
        //如果padding不是0，clipChildrenToPadding是false，为了正确绘制光晕，我们需要找到离边最近的子对象。不确定这是否值得努力
        boolean needsInvalidate = false;
        if (mLeftGlow != null && !mLeftGlow.isFinished()) {
            final int restore = c.save();
            final int padding = mClipToPadding ? getPaddingBottom() : 0;
            c.rotate(270);
            c.translate(-getHeight() + padding, 0);
            needsInvalidate = mLeftGlow != null && mLeftGlow.draw(c);
            c.restoreToCount(restore);
        }
        if (mTopGlow != null && !mTopGlow.isFinished()) {
            final int restore = c.save();
            if (mClipToPadding) {
                c.translate(getPaddingLeft(), getPaddingTop());
            }
            needsInvalidate |= mTopGlow != null && mTopGlow.draw(c);
            c.restoreToCount(restore);
        }
        if (mRightGlow != null && !mRightGlow.isFinished()) {
            final int restore = c.save();
            final int width = getWidth();
            final int padding = mClipToPadding ? getPaddingTop() : 0;
            c.rotate(90);
            c.translate(padding, -width);
            needsInvalidate |= mRightGlow != null && mRightGlow.draw(c);
            c.restoreToCount(restore);
        }
        if (mBottomGlow != null && !mBottomGlow.isFinished()) {
            final int restore = c.save();
            c.rotate(180);
            if (mClipToPadding) {
                c.translate(-getWidth() + getPaddingRight(), -getHeight() + getPaddingBottom());
            } else {
                c.translate(-getWidth(), -getHeight());
            }
            needsInvalidate |= mBottomGlow != null && mBottomGlow.draw(c);
            c.restoreToCount(restore);
        }
        //如果某些视图正在运行动画，ItemDecorator可能会随之移动/更改。
        //使RecyclerView无效以重新绘制装饰器。这仍然是高效的，因为children的显示列表不会失效。
        if (!needsInvalidate && mItemAnimator != null && mItemDecorations.size() > 0
                && mItemAnimator.isRunning()) {
            needsInvalidate = true;
        }

        if (needsInvalidate) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }
    
    public void onDraw(Canvas c) {
        super.onDraw(c);

        final int count = mItemDecorations.size();
        for (int i = 0; i < count; i++) {
            mItemDecorations.get(i).onDraw(c, this, mState);
        }
    }  
```
draw主要分为三个阶段
1.调用super.draw()将Children的绘制分发给ViewGroup  后面调用到onDraw()将装饰的绘制分发给ItemDecoration
2.调用ItemDecorations.onDrawOver   通过这个方法，我们在每个ItemView上面画上很多东西。
3.如果RecyclerView调用了setClipToPadding,会实现一种特殊的滑动效果--每个ItemView可以滑动到padding区域




requestLayout 相关
```
public void requestLayout() {
        if (mInterceptRequestLayoutDepth == 0 && !mLayoutSuppressed) {
            super.requestLayout();
        } else {
            mLayoutWasDefered = true;
        }
    }
```