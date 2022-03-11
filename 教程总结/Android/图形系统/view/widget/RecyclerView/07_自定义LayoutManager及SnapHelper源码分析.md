https://www.jianshu.com/p/af91949db629
对于使用RecyclerView的我们来说，LayoutManager早已非常熟悉。可是，有没有想过我们所说的熟悉是哪种熟悉？对的，就是会使用而已，
这其中包括谷歌爸爸帮我们实现的几种LayoutManager,例如：LinearLayoutManager，GridLayoutManager等等。
仔细想一想，我们使用LayoutManager就像我们当初初学Android时使用各种基础控件，我们处于只会使用的阶段，如果后续有一些特殊的要求，
系统的实现已经不能满足我们自身的需求，此时自定义LayoutManager就必须出手了。同时，如果想要自定义LayoutManager，
我们就必须了解它相关的原理。所以，学习LayoutManager的源码是至关重要的

介于LayoutManger的特殊性，我们不可能将LayoutManager及其所有子类的代码都分析一遍，所以本文的源码分析重点是，
从源码角度来解释为什么这样自定义LayoutManager。自定义LayoutManager要求的门槛相对较高，它不是简单的照着模板来写，
而是需要了解它内部的原理，这其中包括回收机制(这个我们在分析RecyclerView的三大流程时已经从LinearLayoutManager内部看到了)，
滑动机制等等。所以，在自定义LayoutManager时，我默认大家都懂得这些原理

本文打算从如下几个角度来分析LayoutManager:
1 知识储备--相关方法的解释，这里的相关方法主要是自定义涉及到的方法
2 自定义一个LayoutManager
3 SnapHelper基本使用、源码分析和自定义SnapHelper

1. 概述
   在正式分析LayoutManager之前，我们先来对LayoutManager及其它的相关组件做一个简单的概述。
   我们都知道LayoutManager就是一个布局管理器，主要负责RecyclerView的ItemView测量和布局，所以自定义LayoutManager的过程
   跟自定义View的过程非常的相似。
   本文打算从一个Demo开始来介绍怎么自定义一个LayoutManager,效果如下：

同时在这里，我们还介绍了跟LayoutManager相关的两个组件--SnapHelper和SmoothScroller。这个其中SnapHelper主要负责来
调整RecyclerView的滑动距离，比如想要在滑动结束之后，ItemView停留在RecyclerView正中央,可以依靠SnapHelper

2. LayoutManager的相关方法
   我们在自定义LayoutManager之前，先来看一下LayoutManager的几个方法。

方法名	                    作用
generateDefaultLayoutParams	抽象方法，必须实现。这个方法的作用主要是给RecyclerView的ItemView生成LayoutParams
onMeasure	               用来测量RecyclerView的大小的。通常不用重写此方法，但是在一种情况下必须重写，就是LayouytManager不支持自动测量，
这种情况下RecyclerView不会进行自我测量，会调用LayoutManager的onMeasure方法来测量。
onLayoutChildren	      此方法的作用是布局ItemView。此方法就像是ViewGroup的onLayout方法，RecyclerView内部的ItemView怎么布局，
全看这个方法怎么实现。
canScrollHorizontally	设置该LayoutManager的RecyclerView是否可以水平滑动。与之对应的还有canScrollVertically，
用来设置RecyclerView是否垂直滑动
scrollHorizontallyBy	水平可以滑动的距离。此方法带一个dx参数，表示RecyclerView已经产生了dx的滑动距离，此时我们需要做的是调用相关方法，
进行重新布局。同时此方法的返回值表示水平可以滑动的距离。与之对应的方法是scrollVerticallyBy。

3. 自定义LayoutManager
   简单的了解了自定义LayoutManager的几个方法，现在我将带领来实现一个Demo，具体的效果就是上面的gif动图，我们来看看怎么
   自己实现一个LayoutManager
   第一个是0,下面是1,2,3。。。。   1在0左侧一点，0压住1,  1压住2,2在1左侧一点
   上边一个向右拖动，底下的出来   底下的会有一个缩小的效果，实现近大远小

//相关代码在RVCutsomLayoutManagerActivity
(1). 重写generateDefaultLayoutParams方法
首先，自定义LayoutManager的第一步就是重写generateDefaultLayoutParams方法，这个方法的作用在上面我已经介绍了，在这里就不介绍了。
通常来说，我们这样来实现generateDefaultLayoutParams方法就行了：
```
    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(RecyclerView.LayoutParams.WRAP_CONTENT, RecyclerView.LayoutParams.WRAP_CONTENT);
    }
```
我们这里没有特殊的要求，所以让每个ItemView的自适应就行了。

(2). onLayoutChildren方法
然后，第二步就是重写onLayoutChildren方法，也是最复杂的一步。在这一步，我们主要完成两步：
1 定位每个ItemView的位置，然后布局。
2 适配滑动和缩放的效果。

整个效果我们可以这么来考虑，ItemView是从左往右开始布局，不过我们得从从右往左计算每个ItemView的宽高，因为最右边的ItemView宽高是最原始，
同时它的left位置也是最容易的计算(RecyclerView的水平空闲空间减去ItemView的width就行。)。
然后我们可以设置一个offset，后面的ItemView根据这个offset来重新定位。我们通过之前看LinearLayoutManager源码的经验，
发现LinearLayoutManager计算位置通过一个remainSpace变量来实现的。remainSpace表示当前RecyclerView的剩余空间，
每布局一个ItemView，remainSpace减去小消耗的距离就OK！

每一个item
```
public class ItemViewInfo {
    private int mLeft;
    private float mScale;
    public ItemViewInfo(int left, float scale) {
        this.mLeft = left;
        this.mScale = scale;
    }
    public void setLeft(int left) {
        this.mLeft = left;
    }
    public void setScale(float scale) {
        this.mScale = scale;
    }
    public int getLeft() {
        return mLeft;
    }
    public float getScale() {
        return mScale;
    }
}
```
下面我结合代码来具体分析：
```
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (state.getItemCount() == 0 || state.isPreLayout()) return;
        removeAndRecycleAllViews(recycler);
        if (!mHasChild) {
            mItemViewHeight = getVerticalSpace();
            //根据宽高比计算宽度 比如1.5f
            mItemViewWidth = (int) (mItemViewHeight / mItemHeightWidthRatio);
            mHasChild = true;
        }
        mItemCount = getItemCount();
        //滚动偏移 范围在[mItemViewWidth,mItemCount * mItemViewWidth] 第一个的左侧到最后一个的左侧
        mScrollOffset = makeScrollOffsetWithinRange(mScrollOffset);
        fill(recycler);
    }
 public int getVerticalSpace() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }    
```
在onLayoutChildren方法里面，我们初始化了几个变量，其中mItemViewHeight和mItemViewWidth两个变量分别表示ItemView的高和宽。
其次就是mScrollOffset的初始化：
```
    private int makeScrollOffsetWithinRange(int scrollOffset) {
        return Math.min(Math.max(mItemViewWidth, scrollOffset), mItemCount * mItemViewWidth);
    }
```

第一次调用onLayoutChildren方法来初始化mScrollOffset时，mScrollOffset的值被设置为mItemCount * mItemViewWidth。
这有什么意义呢？我待会会解释。
在onLayoutChildren方法的最后，调用fill方法。fill方法才是真正计算每个ItemView的位置，我们来看看：
```
    private void fill(RecyclerView.Recycler recycler) {
        // 1.初始化基本变量
        //最右可见的index
        int bottomVisiblePosition = mScrollOffset / mItemViewWidth;
        //放置n个item，剩余的位置
        final int bottomItemVisibleSize = mScrollOffset % mItemViewWidth;
        //剩余的位置占item的百分比
        final float offsetPercent = bottomItemVisibleSize * 1.0f / mItemViewWidth;
        final int space = getHorizontalSpace();
        int remainSpace = space;
        //每个ItemView偏移的值(默认所有的ItemView都是左对齐)
        final int defaultOffset = mItemViewWidth / 2;
        final List<ItemViewInfo> itemViewInfos = new ArrayList<>();
        // 2.计算每个ItemView的位置信息(left和scale)  i从底下开始
        for (int i = bottomVisiblePosition - 1, j = 1; i >= 0; i--, j++) {
             //根据每个item的缩放计算偏移，因为缩放了所以需要偏移
            //Math.pow(mScale, j - 1) mScale的(j-1)次方  mScale可以设为0.85，底下的都是上面的0.85
            double maxOffset = defaultOffset * Math.pow(mScale, j - 1);
            //计算每个left位置
            int start = (int) (remainSpace - offsetPercent * maxOffset - mItemViewWidth);
            //计算每个的缩放  1 - offsetPercent * (1 - mScale)根据运动位置，缩放不断变化
            float scale = (float) (Math.pow(mScale, j - 1) * (1 - offsetPercent * (1 - mScale)));
            ItemViewInfo info = new ItemViewInfo(start, scale);
            //往第一个插入
            itemViewInfos.add(0, info);
            remainSpace -= maxOffset;
            if (remainSpace < 0) {                
                info.setLeft((int) (remainSpace + maxOffset - mItemViewWidth));                
                info.setScale((float) Math.pow(mScale, j - 1));
                break;
            }
        }
        // 3.添加最右边ItemView的相关信息  原始比例
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
        //从底到上布局，上面是adapter中最小的
        for (int i = 0; i < layoutCount; i++) {
            fillChild(recycler.getViewForPosition(convert2AdapterPosition(startPosition + i)), itemViewInfos.get(i));
        }
    }

 public int getHorizontalSpace() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }    
```
在分析上面的代码之前，我先来对几个变量做一个统一的解释。

变量名	                含义
bottomVisiblePosition	表示此时RecyclerView最右边能看见的ItemView的position。例如说，初始情况下，bottomVisiblePosition
                        就等于ItemCount，当然此时bottomVisiblePosition的结果肯定是不对的，后面在使用时会根据情况来调整。
bottomItemVisibleSize	这个变量没有特殊意义，主要的用来计算offsetPercent
offsetPercent	        滑动的百分比，从1.0f~0.0f变化。
defaultOffset	        每个ItemView偏移的值(默认所有的ItemView都是左对齐)

然后就是计算每个ItemView的位置了。这里需要注意一个问题，就是bottomVisiblePosition == mItemCount的情况。
  当bottomVisiblePosition == mItemCount时，也是最初的状态，这种情况下，第二步就是直接将最右边的ItemView的位置信息计算出来。
  当bottomVisiblePosition < mItemCoun时(没有大于的情况)时，也是在滑动的时，是在第三步时将最右边的ItemView的位置信息计算出来。
关于位置信息的计算，这里就不讨论了，都是一些常规的计算逻辑。
最后就是布局，调用的是fillChild方法：
```
    private void fillChild(View view, ItemViewInfo itemViewInfo) {
        addView(view);
        measureChildWithExactlySize(view);
        final int top = getPaddingTop();
        //放置每个child的位置
        layoutDecoratedWithMargins(view, itemViewInfo.getLeft(), top, itemViewInfo.getLeft() + mItemViewWidth, top + mItemViewHeight);
        //配置每个item的缩放
        view.setScaleX(itemViewInfo.getScale());
        view.setScaleY(itemViewInfo.getScale());
    }
 private void measureChildWithExactlySize(View child) {
        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) child.getLayoutParams();
        final int widthSpec = View.MeasureSpec.makeMeasureSpec(mItemViewWidth - lp.leftMargin - lp.rightMargin, View.MeasureSpec.EXACTLY);
        final int heightSpec = View.MeasureSpec.makeMeasureSpec(mItemViewHeight - lp.topMargin - lp.bottomMargin, View.MeasureSpec.EXACTLY);
        child.measure(widthSpec, heightSpec);
    }    
```

(3). 水平滑动
接下来就是让RecyclerView支持水平滑动。要想支持水平滑动，我们必须重写canScrollHorizontally方法和scrollHorizontallyBy方法，我们来看看：
```
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
```

这个过程中，需要特别注意的是scrollHorizontallyBy方法，我们不能直接让mScrollOffset加上dx，
  因为mScrollOffset的范围在[mItemViewWidth,mItemCount * mItemViewWidth],所以在每次滑动之后需要调整，
  得再一次调用makeScrollOffsetWithinRange方法。

(3). 滑动之后最右边的ItemView都能完整显示
这个需求就非常的简单，自我实现一个SnaHelper，然后这样使用就OK了：  CustomSnapHelper在下面有分析
```
    private final SnapHelper mSnapHelper = new CustomSnapHelper();
    public void onAttachedToWindow(RecyclerView view) {
        super.onAttachedToWindow(view);
        mSnapHelper.attachToRecyclerView(view);
    }
```

4. SnapHelper
   SnaHelper的存在对于RecyclerView来说，可谓是如虎添翼。SnaHelper可见帮助我们实现一些特殊的效果，比如说，
       我们可以使用RecyclerView和SnapHelper去实现ViewPager的效果。
   通常来说，我们在日常开发中，使用RecyclerView很少遇到的SnapHelper，不过，如果你想要自定义LayoutManager来实现一些特殊效果，
     很大的可能性会遇到SnapHelper。
   那么SnapHelper到底是什么呢？是怎么使用的呢？它的实现原理又是什么呢？这是本文需要解答的三个问题。
   简单来说，SnapHelper就是一个Helper类，只是它的内部有两个监听接口：OnFlingListener和OnScrollListener，
     分别用来监听RecyclerView的scroll事件和fling事件。
   而SnapHelper的使用也是非常的简单，就是在LayoutManager的onAttachedToWindow方法调用SnapHelper的attachToRecyclerView方法即可。
     我们就从attachToRecyclerView方法为入口来分析SnapHelper的源码。

(1). SnapHelper的源码分析   Snap  [snæp]  (使喀嚓)断裂，绷断;(使啪地)打开，关上，移到某位置;厉声说;怒气冲冲地说;不耐烦地说
SnapHelper的原理实际上是非常的简单，大家不要害怕。我们在分析SnapHelper源码之前，先来了解SnapHelper几个比较重要的方法：

方法名	             	            含义
calculateDistanceToFinalSnap  计算RecyclerView最终滑动的距离。返回的是一个长度为2的数组，其中0位置表示水平滑动的滑动距离，
                                 1位置表示垂直滑动的距离。
findTargetSnapPosition		  这个方法表示fling操作最终能滑动到I的temView的position。这个position称为targetSnapPosition，
                              位置上对应的View就是targetSnapView。如果找不到position，就返回RecyclerView.NO_POSITION
findSnapView		        最终滑动位置对应的ItemView

在这里，我们必须区分一下findTargetSnapPosition方法和calculateDistanceToFinalSnap、findSnapView方法的区别。
1 findTargetSnapPosition:此方法表示fling滑动能滑到的位置。
2 calculateDistanceToFinalSnap和findSnapView:这两个方法表示正常滑动的能到达位置，其中calculateDistanceToFinalSnap表示距离，
  这个过程涉及到因为对齐操作而进行的距离重新调整；findSnapView方法表示正常滑动能到达的位置对应的ItemView。

所以，我们在自定义SnapHelper时，为了简单起见，不可以处理fling操作，也就是findTargetSnapPosition返回为RecyclerView.NO_POSITION,
 然后让RecyclerView自己进行fling ，等待滑动结束之后，会回调我们的calculateDistanceToFinalSnap和findSnapView来进行位置对齐。
 这样做的好处就是，我们不用既考虑fling又考虑普通滑动

A.attachToRecyclerView方法
准备的差不多了，接下来我们正式分析SnapHelper的源码。我们来看看attachToRecyclerView方法：
```
    public void attachToRecyclerView(@Nullable RecyclerView recyclerView)
            throws IllegalStateException {
        if (mRecyclerView == recyclerView) {
            return; // nothing to do
        }
        if (mRecyclerView != null) {
            destroyCallbacks();
        }
        mRecyclerView = recyclerView;
        if (mRecyclerView != null) {
            setupCallbacks();
            mGravityScroller = new Scroller(mRecyclerView.getContext(),
                    new DecelerateInterpolator());
            snapToTargetExistingView();
        }
    }
```
attachToRecyclerView非常的简单，就是设置给RecyclerView设置了两个监听接口：
```
    private void setupCallbacks() throws IllegalStateException {
        if (mRecyclerView.getOnFlingListener() != null) {
            throw new IllegalStateException("An instance of OnFlingListener already set.");
        }
        mRecyclerView.addOnScrollListener(mScrollListener);
        mRecyclerView.setOnFlingListener(this);
    }
```
然后RecyclerView开心的滑动，就会回调到我们的两个监听事件里面来。

B.OnScrollListener
我们先来看看OnScrollListener接口的实现，看看它做了哪些事情：
```
    private final RecyclerView.OnScrollListener mScrollListener =
            new RecyclerView.OnScrollListener() {
                boolean mScrolled = false;
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                    if (newState == RecyclerView.SCROLL_STATE_IDLE && mScrolled) {
                        mScrolled = false;
                        snapToTargetExistingView();
                    }
                }
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    if (dx != 0 || dy != 0) {
                        mScrolled = true;
                    }
                }
            };
```
我们发现，当RecyclerView滑动结束之后，就会调用snapToTargetExistingView方法。那snapToTargetExistingView方法是干嘛的呢？
  其实就是保证对齐的。我们来看看：
```
    void snapToTargetExistingView() {
        if (mRecyclerView == null) {
            return;
        }
        LayoutManager layoutManager = mRecyclerView.getLayoutManager();
        if (layoutManager == null) {
            return;
        }
        View snapView = findSnapView(layoutManager);
        if (snapView == null) {
            return;
        }
        int[] snapDistance = calculateDistanceToFinalSnap(layoutManager, snapView);
        if (snapDistance[0] != 0 || snapDistance[1] != 0) {
            mRecyclerView.smoothScrollBy(snapDistance[0], snapDistance[1]);
        }
    }
```
我们发现，在这里先是调用了findSnapView方法找到滑动的最终ItemView,然后根据找到的SnapView,调用calculateDistanceToFinalSnap方法
  来计算滑动的距离，最后调用相关方法来进行对齐。整个过程就是这么的简单

C. OnFlingListener
SnapHelper内部本身没有一个OnFlingListener接口对象，而是自身实现了OnFingListener,所以当RecyclerView在fling时，
  会回调此onFling方法。我们来看看:
```
    @Override
    public boolean onFling(int velocityX, int velocityY) {
        LayoutManager layoutManager = mRecyclerView.getLayoutManager();
        if (layoutManager == null) {
            return false;
        }
        RecyclerView.Adapter adapter = mRecyclerView.getAdapter();
        if (adapter == null) {
            return false;
        }
        int minFlingVelocity = mRecyclerView.getMinFlingVelocity();
        return (Math.abs(velocityY) > minFlingVelocity || Math.abs(velocityX) > minFlingVelocity)
                && snapFromFling(layoutManager, velocityX, velocityY);
    }
```
首先，我们要明白一个东西，如果RecyclerView有一个OnFlingListener处理fling事件的话，那么RecyclerView就不会再处理fling事件。
所以SnapHelper是否处理fling事件，还需要看它的snapFromFling方法。我们来看看：
```
    private boolean snapFromFling(@NonNull LayoutManager layoutManager, int velocityX,
            int velocityY) {
        if (!(layoutManager instanceof ScrollVectorProvider)) {
            return false;
        }

        SmoothScroller smoothScroller = createScroller(layoutManager);
        if (smoothScroller == null) {
            return false;
        }

        int targetPosition = findTargetSnapPosition(layoutManager, velocityX, velocityY);
        if (targetPosition == RecyclerView.NO_POSITION) {
            return false;
        }

        smoothScroller.setTargetPosition(targetPosition);
        layoutManager.startSmoothScroll(smoothScroller);
        return true;
    }
```

在上面的代码中，我们发现，findTargetSnapPosition如果返回为RecyclerView.NO_POSITION，那么SnapHelper就不会处理fling事件。
而如果SnapHelper要处理fling事件的话，会通过LayoutManager的startSmoothScroll方法。这里面的原理实际上还是调用到RecyclerView的ViewFlinger里面去了。
整个SnapHelper的原理就是这样，非常的简单，接下来我们结合实际来看看怎么自定义一个SnapHelper。

//todo SmoothScroller 其他组件系列

(2).自定义SnapHelper
通常来说，我们自定义SnapHelper,实现三个抽象方法就已经差不多，分别是calculateDistanceToFinalSnap方法、findTargetSnapPosition方法
  和findSnapView方法就已经够了。我么来看看我们自己实现的CustomSnapHelper:
```
public class CustomSnapHelper extends SnapHelper {

    @Override
    public int[] calculateDistanceToFinalSnap(
            @NonNull RecyclerView.LayoutManager layoutManager, @NonNull View targetView) {
        if (layoutManager instanceof CustomLayoutManger) {
            int[] out = new int[2];
            if (layoutManager.canScrollHorizontally()) {
                out[0] = ((CustomLayoutManger) layoutManager).calculateDistanceToPosition(
                        layoutManager.getPosition(targetView));
                out[1] = 0;
            } else {
                out[0] = 0;
                out[1] = ((CustomLayoutManger) layoutManager).calculateDistanceToPosition(
                        layoutManager.getPosition(targetView));
            }
            return out;
        }
        return null;
    }
    @Override
    public int findTargetSnapPosition(RecyclerView.LayoutManager layoutManager, int velocityX,
                                      int velocityY) {
        return RecyclerView.NO_POSITION;
    }
    @Override
    public View findSnapView(RecyclerView.LayoutManager layoutManager) {
        if (layoutManager instanceof CustomLayoutManger) {
            int pos = ((CustomLayoutManger) layoutManager).getFixedScrollPosition();
            if (pos != RecyclerView.NO_POSITION) {
                return layoutManager.findViewByPosition(pos);
            }
        }
        return null;
    }
}
//CustomLayoutManger.java
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
 public int convert2AdapterPosition(int layoutPosition) {
        return mItemCount - 1 - layoutPosition;
    } 
 public int calculateDistanceToPosition(int targetPos) {
        int pendingScrollOffset = mItemViewWidth * (convert2LayoutPosition(targetPos) + 1);
        return pendingScrollOffset - mScrollOffset;
    } 
  private int convert2LayoutPosition(int adapterPosition) {
        return mItemCount - 1 - adapterPosition;
    }         
```

5. 总结
   到这里，我们对LayoutManager相关分析就差不多，在最后，我做一个小小的总结。

自定义LayoutManager需要注意四点：
  1.重写generateDefaultLayoutParams方法；
  2.重写onLayoutChildren方法，对ItemView进行布局；
  3. 处理滑动，例如水平滑动需要重写canScrollHorizontally和scrollHorizontallyBy；
  4. 如果需要处理对齐问题，可以使用SnapHelper。

自定义SnapHelper我们只需要重写它的三个抽象方法即可，分别是：
     calculateDistanceToFinalSnap、findTargetSnapPosition和findSnapView。
     需要注意的是，为了简单起见，我们可以直接在findTargetSnapPosition内部返回RecyclerView.NO_POSITION,
     让RecyclerView来帮助我们处理fling事件。

