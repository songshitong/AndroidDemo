https://www.jianshu.com/p/efe81969f69d

//todo 位图算法 https://mp.weixin.qq.com/s/xi7qUqGr5d3XiP-uS8wthA
//todo 缓存示例  https://ljd1996.github.io/2021/03/02/Android-RecyclerView%E7%AC%94%E8%AE%B0/
1. 概述
   在正式分析源码之前，我先对缓存机制做一个概述，同时也会对一些概念进行统一解释，这些对后面的分析有很大的帮助，因为如果不理解这些概念的话，
   后面容易看得雨里雾里的。

(1).四级缓存
首先，我将RecyclerView的缓存分为四级，这里统一说明一下每级缓存的意思。  ViewCacheExtension要求缓存View,其他缓存的都是ViewHolder
缓存级别	实际变量                       	含义
一级缓存	mAttachedScrap和mChangedScrap	这是优先级最高的缓存，RecyclerView在获取ViewHolder时,优先会到这两个缓存来找。
        (ArrayList)                     其中mAttachedScrap存储的是当前还在屏幕中的ViewHolder，mChangedScrap存储的是数据被更新的ViewHolder,
                                         比如说调用了Adapter的notifyItemChanged方法。
二级缓存	mCachedViews	   默认大小为2，通常用来存储预取的ViewHolder，同时在回收ViewHolder时，也会可能存储一部分的ViewHolder，
       (ArrayList)           这部分的ViewHolder通常来说，意义跟一级缓存差不多。
三级缓存	ViewCacheExtension	自定义缓存
四级缓存	 RecycledViewPool	根据ViewType来缓存ViewHolder，每个ViewType的数组大小为5，可以动态的改变。
(SparseArray<ScrapData(ArrayList)>)       可以通过RecyclerView的setRecycledViewPool()和getRecycledViewPool()共享RecycledViewPool缓存

Scrap [skræp] 碎片，小块(纸、织物等);丝毫;一丁点;残羹剩饭

如上表，统一的解释了每个缓存的含义和作用。在这里，我再来对其中的几个缓存做一个详细的解释。
mAttachedScrap：上表中说，它表示存储的是当前还在屏幕中ViewHolder。实际上是从屏幕上分离出来的ViewHolder，但是又即将添加到屏幕上去的ViewHolder。
  比如说，RecyclerView上下滑动，滑出一个新的Item，此时会重新调用LayoutManager的onLayoutChildren方法，
  从而会调用detachAndScrapAttachedViews将屏幕上所有的ViewHolder先scrap掉(含义就是废弃掉)，holder处于!holder.isUpdated()，没有更新
  添加到mAttachedScrap里面去，然后在重新布局每个ItemView时，会从优先mAttachedScrap里面获取，这样效率就会非常的高。
  这个过程不会重新onBindViewHolder。

mCachedViews：默认大小为2，不过通常是3，3由默认的大小2 + 预取的个数1。所以在RecyclerView在首次加载时，mCachedViews的size为3
  (这里以LinearLayoutManager的垂直布局为例)。通常来说，可以通过RecyclerView的setItemViewCacheSize方法设置大小，
  但是这个不包括预取大小；预取大小通过LayoutManager的setItemPrefetchEnabled方法来控制。

缓存流转
  当一级缓存满足下列情况 会进入mCachedViews
    1. 在非预布局阶段，holder被移除了 
    2. adapter设置了stable id,但是holder的ID发生了变化
    3. 非预布局阶段，holder的type发生了变化
 当mCachedViews满足以下情况，进入RecycledViewPool      recycleViewHolderInternal()
   1. 更改了mCachedViews和预拉取的大小，使其和小于等于0  可以查看updateViewCacheSize()
   2. mCachedViews满了，淘汰最老的到RecycledViewPool
   3. 如果holder失效了，被删除了，更新了，POSITION不知道

ViewHolder的几个状态值
我们在看RecyclerView的源码时，可能到处都能看到调用ViewHolder的isInvalid、isRemoved、isBound、isTmpDetached、isScrap和isUpdated
  这几个方法。这里我统一的解释一下。
方法名	    对应的Flag	含义或者状态设置的时机
isInvalid	FLAG_INVALID	表示当前ViewHolder是否已经失效。通常来说，在3种情况下会出现这种情况：
                            1.调用了Adapter的notifyDataSetChanged方法；     processDataSetCompletelyChanged()
                            2. 从mAttachedScrap/hidden/cachedViews中获取的holder失效了  tryGetViewHolderForPositionByDeadline()
                            3. 调用RecyclerView的setAdapter方法或者swapAdapter方法。  processDataSetCompletelyChanged()
isRemoved	FLAG_REMOVED	表示当前的ViewHolder是否被移除。通常来说，数据源被移除了部分数据，然后调用Adapter的notifyItemRemoved方法。
isBound  	FLAG_BOUND	    表示当前ViewHolder是否已经调用了onBindViewHolder。
isTmpDetached	FLAG_TMP_DETACHED	表示当前的ItemView是否从RecyclerView(即父View)detach掉。通常来说有两种情况下会出现这种情况：
                                    1.手动了RecyclerView的detachView相关方法； detachViewAt(),moveView()(先detach后attach)
                                    2. 在从mHideViews里面获取ViewHolder,会先detach掉这个ViewHolder关联的ItemView,然后回收到Scrap
                                      getScrapOrHiddenOrCachedHolderForPosition()
isScrap	  无Flag来表示该状态，
      用mScrapContainer是否为null来判断	表示是否在mAttachedScrap或者mChangedScrap数组里面，进而表示当前ViewHolder是否被废弃。
                                        holder.unscrapView/Recycler.unscrapView 将holder从Scrap中移除
isUpdated	FLAG_UPDATE	    表示当前ViewHolder是否已经更新。通常来说，在3种情况下会出现情况：
                            1.isInvalid方法存在的三种情况；
                            2.调用了Adapter的onBindViewHolder方法；
                            3. 调用了Adapter的notifyItemChanged方法
shouldIgnore FLAG_IGNORE    是否忽略当前ViewHolder，忽略后对LayoutManager仍然可见，但是不可以scrap,remove,recycle直到LayoutManager替换它
                            通过LM.ignoreView()和stopIgnoringView()进行设置
isRecyclable FLAG_NOT_RECYCLABLE  holder是否可被回收，通过holder.setIsRecyclable(false/true)进行设置
                                  一般在动画期间holder禁止回收
isAdapterPositionUnknown FLAG_ADAPTER_POSITION_UNKNOWN或者isInvalid   bindViewHolder时会设置FLAG_ADAPTER_POSITION_UNKNOWN

Holder的flag重置的场景
1 将holder放入RecycledViewPool时
1 从RecycledViewPool取出holder
1 stopIgnoringView后会进行重置

//todo  位运算的mask
```
void setFlags(int flags, int mask) {
            mFlags = (mFlags & ~mask) | (flags & mask);
        }
```

ChildHelper的mHiddenViews
在四级缓存中，我们并没有将mHiddenViews算入其中。因为mHiddenViews只在动画期间才会有元素，当动画结束了，自然就清空了。
  所以mHiddenViews并不算入4级缓存中。

mAttachedScrap和mChangedScrap的区别
```
       //Recycler
        void scrapView(View view) {
            final ViewHolder holder = getChildViewHolderInt(view);
            if (holder.hasAnyOfTheFlags(ViewHolder.FLAG_REMOVED | ViewHolder.FLAG_INVALID)
                    || !holder.isUpdated() || canReuseUpdatedViewHolder(holder)) {
                if (holder.isInvalid() && !holder.isRemoved() && !mAdapter.hasStableIds()) {
                    throw new IllegalArgumentException("Called scrap view with an invalid view."
                            + " Invalid views cannot be reused from scrap, they should rebound from"
                            + " recycler pool." + exceptionLabel());
                }
                holder.setScrapContainer(this, false);
                mAttachedScrap.add(holder);
            } else {
                if (mChangedScrap == null) {
                    mChangedScrap = new ArrayList<ViewHolder>();
                }
                holder.setScrapContainer(this, true);
                mChangedScrap.add(holder);
            }
        }
```
这个根本的目的就是，判断ViewHolder的flag状态，从而来决定是放入mAttachedScrap还是mChangedScrap。从上面的代码，我们得出：
1 mAttachedScrap里面放的是两种状态的ViewHolder：1.被同时标记为remove和invalid；2.完全没有改变的ViewHolder。
这里还有第三个判断，这个跟RecyclerView的ItemAnimator有关，如果ItemAnimator为空或者ItemAnimator的canReuseUpdatedViewHolder方法为true，
  也会放入到mAttachedScrap。那正常情况下，什么情况返回为true呢？
从SimpleItemAnimator的源码可以看出来，当ViewHolder的isInvalid方法返回为true时，会放入到 mAttachedScrap里面。也就是说，
   如果ViewHolder失效了，也会放到mAttachedScrap里面。

2 那么mChangedScrap里面放什么类型flag的ViewHolder呢？当然是ViewHolder的isUpdated方法返回为true时，会放入到mChangedScrap里面去。
  所以，调用Adapter的notifyItemChanged方法时，并且RecyclerView的ItemAnimator不为空，会放入到mChangedScrap里面。


Scrap数组和mHiddenViews的区别
mHiddenViews只存放动画的ViewHolder,动画结束了自然就清空了。之所以存在 mHiddenViews这个数组，我猜测是存在动画期间，进行复用的可能性，
 此时就可以在mHiddenViews进行复用了。而Scrap数组跟mHiddenViews两者完全不冲突，所以存在一个ViewHolder同时在Scrap数组和mHiddenViews的可能性。
  但是这并不影响，因为在动画结束时，会从mHiddenViews里面移除。



本文在分析RecyclerView的换出机制时，打算从两个大方面入手：1.复用；2.回收。
我们先来看看复用的部分逻辑，因为只有理解了RecyclerView究竟是如何复用的，对回收才能更加明白。

2. 复用
   RecyclerView对ViewHolder的复用，我们得从LayoutState的next方法开始。LayoutManager在布局itemView时，需要获取一个ViewHolder对象，
   就是通过这个方法来获取，具体的复用逻辑也是在这个方面开始调用的。我们来看看：
```
        View next(RecyclerView.Recycler recycler) {
            if (mScrapList != null) {
                return nextViewFromScrapList();
            }
            final View view = recycler.getViewForPosition(mCurrentPosition);
            mCurrentPosition += mItemDirection;
            return view;
        }
```
next方法里面其实也没做什么事，就是调用RecyclerView的getViewForPosition方法来获取一个View的。而getViewForPosition方法
 最终会调用到RecyclerView的tryGetViewHolderForPositionByDeadline方法。所以，RecyclerView真正复用的核心就在这个方法，
 我们今天来详细的分析一下这个方法
(1). 通过Position方式来获取ViewHolder
通过这种方式来获取优先级比较高，因为每个ViewHolder还没被改变，通常在这种情况下，都是某一个ItemView对应的ViewHolder被更新导致的，
  所以在屏幕上其他的ViewHolder,可以快速对应原来的ItemView。我们来看看相关的源码。
tryGetViewHolderForPositionByDeadline   
```
    if (mState.isPreLayout()) {
                holder = getChangedScrapViewForPosition(position);
                fromScrapOrHiddenOrCache = holder != null;
            }
            // 1) Find by position from scrap/hidden list/cache
            if (holder == null) {
                holder = getScrapOrHiddenOrCachedHolderForPosition(position, dryRun);
                if (holder != null) {
                    if (!validateViewHolderForOffsetPosition(holder)) {
                        //dryRun  将holder从scrap或cache中移除并回收到RecycledViewPool
                        if (!dryRun) {
                            // we would like to recycle this but need to make sure it is not used by
                            // animation logic etc.
                            holder.addFlags(ViewHolder.FLAG_INVALID);
                            if (holder.isScrap()) {
                                removeDetachedView(holder.itemView, false);
                                holder.unScrap();
                            } else if (holder.wasReturnedFromScrap()) {
                                holder.clearReturnedFromScrapFlag();
                            }
                            recycleViewHolderInternal(holder);
                        }
                        holder = null;
                    } else {
                        fromScrapOrHiddenOrCache = true;
                    }
                }
            }

//校验viewHolder的合法性
 boolean validateViewHolderForOffsetPosition(ViewHolder holder) {
            //如果ViewHolder被移除了，adapter中也不存在，不进行校验，返回是否预布局(存在动画)
            if (holder.isRemoved()) {
                return mState.isPreLayout();
            }
            //如果ViewHolder没有被移除，校验它的type和id
            ..
            if (!mState.isPreLayout()) {
                //非预布局阶段，holder的type发生改变，holder失效了
                final int type = mAdapter.getItemViewType(holder.mPosition);
                if (type != holder.getItemViewType()) {
                    return false;
                }
            }
            //ViewHolder使用了stableId，如果id改变了，holder失效
            if (mAdapter.hasStableIds()) {
                return holder.getItemId() == mAdapter.getItemId(holder.mPosition);
            }
            return true;
        }            
```
如上的代码分为两步：
1 从mChangedScrap里面去获取ViewHolder,这里面存储的是更新的ViewHolder。
2 分别mAttachedScrap、 mHiddenViews、mCachedViews获取ViewHolder

先来看看第一步
```
            if (mState.isPreLayout()) {
                holder = getChangedScrapViewForPosition(position);
                fromScrapOrHiddenOrCache = holder != null;
            }
```
对于预布局 要想真正开启预布局，必须有ItemAnimator，并且每个RecyclerView对应的LayoutManager必须开启预处理动画
为什么只在预布局的时候才从mChangedScrap里面去取呢？
首先，我们得知道mChangedScrap数组里面放的是什么类型的 ViewHolder。从前面的分析中，我们知道，只有当ItemAnimator不为空，
  被changed的ViewHolder会放在mChangedScrap数组里面。因为chang动画前后相同位置上的ViewHolder是不同的，所以当预布局时，
  从mChangedScrap缓存里面去，而正式布局时，不会从mChangedScrap缓存里面去，这就保证了动画前后相同位置上是不同的ViewHolder。
  为什么要保证动画前后是不同的ViewHolder呢？这是RecyclerView动画机制相关的知识，这里就不详细的解释，后续有专门的文章来分析它，在这里，
  我们只需要记住，动画执行的有一个前提就是动画前后是不同的ViewHolder。 

然后，我们再来看看第二步。
这一步理解起来比较容易，分别从mAttachedScrap、 mHiddenViews、mCachedViews获取ViewHolder。但是我们需要的是，如果获取的ViewHolder是无效的，
 得做一些清理操作，然后重新放入到缓存里面，具体对应的缓存就是mCachedViews和 RecycledViewPool。
  recycleViewHolderInternal方法就是回收ViewHolder的方法，后面再分析回收相关的逻辑会重点分析这个方法
依次获取缓存的方法 getScrapOrHiddenOrCachedHolderForPosition
```
    ViewHolder getScrapOrHiddenOrCachedHolderForPosition(int position, boolean dryRun) {
            final int scrapCount = mAttachedScrap.size();
            //从mAttachedScrap获取          
            for (int i = 0; i < scrapCount; i++) {
                final ViewHolder holder = mAttachedScrap.get(i);
                if (!holder.wasReturnedFromScrap() && holder.getLayoutPosition() == position
                        && !holder.isInvalid() && (mState.mInPreLayout || !holder.isRemoved())) {
                    holder.addFlags(ViewHolder.FLAG_RETURNED_FROM_SCRAP);
                    return holder;
                }
            }

            if (!dryRun) {
                //从mHiddenViews获取
                View view = mChildHelper.findHiddenNonRemovedView(position);
                if (view != null) {
                    //这个视图很适合使用。我们只需要打开隐藏，分离并移动到scrap list
                    final ViewHolder vh = getChildViewHolderInt(view);
                    mChildHelper.unhide(view);
                    int layoutIndex = mChildHelper.indexOfChild(view);
                    if (layoutIndex == RecyclerView.NO_POSITION) {
                        throw new IllegalStateException("layout index should not be -1 after "
                                + "unhiding a view:" + vh + exceptionLabel());
                    }
                    mChildHelper.detachViewFromParent(layoutIndex);
                    scrapView(view);
                    vh.addFlags(ViewHolder.FLAG_RETURNED_FROM_SCRAP
                            | ViewHolder.FLAG_BOUNCED_FROM_HIDDEN_LIST);
                    return vh;
                }
            }

            // 从mCachedViews获取
            final int cacheSize = mCachedViews.size();
            for (int i = 0; i < cacheSize; i++) {
                final ViewHolder holder = mCachedViews.get(i);
                if (!holder.isInvalid() && holder.getLayoutPosition() == position
                        && !holder.isAttachedToTransitionOverlay()) {
                    if (!dryRun) {
                        mCachedViews.remove(i);
                    }                   
                    return holder;
                }
            }
            return null;
        }
```

(2). 通过viewType方式来获取ViewHolder
前面分析了通过Position的方式来获取ViewHolder，这里我们来分析一下第二种方式--ViewType。不过在这里，我先对前面的方式做一个简单的总结，
  RecyclerView通过Position来获取ViewHolder，并不需要判断ViewType是否合法，因为如果能够通过Position来获取ViewHolder，
  ViewType本身就是正确对应的。
而这里通过ViewType来获取ViewHolder表示，此时ViewHolder缓存的Position已经失效了。ViewType方式来获取ViewHolder的过程，我将它分为3步
1 如果Adapter的hasStableIds方法返回为true，优先通过ViewType和id两个条件来寻找。如果没有找到，那么就进行第2步。
2 如果Adapter的hasStableIds方法返回为false，在这种情况下，首先会在ViewCacheExtension里面找，如果还没有找到的话，
  最后会在 RecycledViewPool里面来获取ViewHolder。
3 如果以上的复用步骤都没有找到合适的ViewHolder，最后就会调用Adapter的onCreateViewHolder方法来创建一个新的ViewHolder。

在这里，我们需要注意的是，上面的第1步 和 第2步有前提条件，就是两个都必须比较ViewType。接下来，我通过代码简单的分析一下每一步。

A. 通过id来寻找ViewHolder       
通过id寻找合适的ViewHolder主要是通过调用getScrapOrCachedViewForId方法来实现的，我们简单的看一下代码：
getScrapOrCachedViewForId方法就是分别从mAttachedScrap和mCachedViews数组寻找合适的ViewHolder
```
ViewHolder tryGetViewHolderForPositionByDeadline(int position,
                boolean dryRun, long deadlineNs) {
                 if (mAdapter.hasStableIds()) {
                    holder = getScrapOrCachedViewForId(mAdapter.getItemId(offsetPosition),
                            type, dryRun);
                    if (holder != null) {
                        // update position
                        holder.mPosition = offsetPosition;
                        fromScrapOrHiddenOrCache = true;
                    }
                }                
 }
```

B 从ViewCacheExtension寻找ViewHolder
ViewCacheExtension 是 RecyclerView 通过setViewCacheExtension设置 
```
if (holder == null && mViewCacheExtension != null) {
                    final View view = mViewCacheExtension
                            .getViewForPositionAndType(this, position, type);
                    if (view != null) {
                        holder = getChildViewHolder(view);                       
                    }
                }
```

C 从RecycledViewPool里面获取ViewHolder
 RecycledViewPool类结构
```
public static class RecycledViewPool {
        private static final int DEFAULT_MAX_SCRAP = 5; 
        static class ScrapData {
            final ArrayList<ViewHolder> mScrapHeap = new ArrayList<>();
            int mMaxScrap = DEFAULT_MAX_SCRAP;
            long mCreateRunningAverageNs = 0;
            long mBindRunningAverageNs = 0;
        }
        SparseArray<ScrapData> mScrap = new SparseArray<>();
  
  //根据viewType获取缓存ViewHolder      
  public ViewHolder getRecycledView(int viewType) {
            final ScrapData scrapData = mScrap.get(viewType);
            if (scrapData != null && !scrapData.mScrapHeap.isEmpty()) {
                final ArrayList<ViewHolder> scrapHeap = scrapData.mScrapHeap;
                for (int i = scrapHeap.size() - 1; i >= 0; i--) {
                    if (!scrapHeap.get(i).isAttachedToTransitionOverlay()) {
                        return scrapHeap.remove(i);
                    }
                }
            }
            return null;
 }          
}
```
简单的了解了RecycledViewPool的数据结构，接下来我们来看看复用的相关的代码：
```
 ViewHolder tryGetViewHolderForPositionByDeadline(int position,
                boolean dryRun, long deadlineNs) {
 ...
 if (holder == null) { // fallback to pool
        holder = getRecycledViewPool().getRecycledView(type);
        if (holder != null) {
            holder.resetInternal();
            if (FORCE_INVALIDATE_DISPLAY_LIST) {
                invalidateDisplayListInt(holder);
            }
        }
    }
 ...                                             
  }
```

D. 调用Adapter的onCreateViewHolder方法创建一个新的ViewHolder
```
ViewHolder tryGetViewHolderForPositionByDeadline(int position,boolean dryRun, long deadlineNs) {
....
  if (holder == null) {
        long start = getNanoTime();
        if (deadlineNs != FOREVER_NS
                && !mRecyclerPool.willCreateInTime(type, start, deadlineNs)) {
            return null;
        }
        holder = mAdapter.createViewHolder(RecyclerView.this, type);
        if (ALLOW_THREAD_GAP_WORK) {
            RecyclerView innerView = findNestedRecyclerView(holder.itemView);
            if (innerView != null) {
                holder.mNestedRecyclerView = new WeakReference<>(innerView);
            }
        }

        long end = getNanoTime();
        mRecyclerPool.factorInCreateTime(type, end - start);             
    }                      
  }   
```
上面的代码主要的目的就是调用Adapter的createViewHolder方法来创建一个ViewHolder，在这个过程就是简单计算了创建一个ViewHolder的时间。
关于复用机制的理解，我们就到此为止。其实RecyclerView的复用机制一点都不复杂，我觉得让大家望而却步的原因，是因为我们不知道为什么在这么做，
  如果了解这么做的原因，一切都显得那么理所当然。

E 调用Adapter的onBindViewHolder() 进行view数据绑定
```
ViewHolder tryGetViewHolderForPositionByDeadline(int position,boolean dryRun, long deadlineNs) {
...
 boolean bound = false;
    if (mState.isPreLayout() && holder.isBound()) {
        holder.mPreLayoutPosition = position;
    } else if (!holder.isBound() || holder.needsUpdate() || holder.isInvalid()) {
        final int offsetPosition = mAdapterHelper.findPositionOffset(position);
        //最后调用Adapter.onBindViewHolder 更多查看Adapter分析相关
        bound = tryBindViewHolderByDeadline(holder, offsetPosition, position, deadlineNs);
    }
    //处理LayoutParams
    final ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();
    final LayoutParams rvLayoutParams;
    if (lp == null) {
        rvLayoutParams = (LayoutParams) generateDefaultLayoutParams();
        holder.itemView.setLayoutParams(rvLayoutParams);
    } else if (!checkLayoutParams(lp)) {
        rvLayoutParams = (LayoutParams) generateLayoutParams(lp);
        holder.itemView.setLayoutParams(rvLayoutParams);
    } else {
        rvLayoutParams = (LayoutParams) lp;
    }
    rvLayoutParams.mViewHolder = holder;
    rvLayoutParams.mPendingInvalidate = fromScrapOrHiddenOrCache && bound;
    return holder;
}
```


分析RecyclerView的复用部分，接下来，我们来分析一下回收部分


3. 回收
   回收是RecyclerView复用机制内部非常重要。首先，有复用的过程，肯定就有回收的过程；其次，同时理解了复用和回收两个过程，
   这可以帮助我们在宏观上理解RecyclerView的工作原理；最后，理解RecyclerView在何时会回收ViewHolder，这对使用RecyclerView有很大的帮助。
   其实回收的机制也没有想象中那么的难，本文打算从几个方面来分析RecyclerView的回收过程。
  1 scrap数组
  2 mCachedViews数组
  3 mHiddenViews数组
  4 RecycledViewPool数组
   接下来，我们将一一的分析


(1). scrap数组
关于ViewHolder回收到scrap数组里面，其实我在前面已经简单的分析了，重点就在于Recycler的scrapView方法里面。
  我们来看看scrapView在哪里被调用了。有如下两个地方:
1 在getScrapOrHiddenOrCachedHolderForPosition方法里面，如果从mHiddenViews获得一个ViewHolder的话，
  会先将这个ViewHolder从mHiddenViews数组里面移除，然后调用Recycler的scrapView方法将这个ViewHolder放入到scrap数组里面，
  并且标记FLAG_RETURNED_FROM_SCRAP和FLAG_BOUNCED_FROM_HIDDEN_LIST两个flag。
```
ViewHolder getScrapOrHiddenOrCachedHolderForPosition(int position, boolean dryRun) {
  ...
  View view = mChildHelper.findHiddenNonRemovedView(position);
    if (view != null) {
        // This View is good to be used. We just need to unhide, detach and move to the
        // scrap list.
        final ViewHolder vh = getChildViewHolderInt(view);
        mChildHelper.unhide(view);
        int layoutIndex = mChildHelper.indexOfChild(view);
        if (layoutIndex == RecyclerView.NO_POSITION) {
            throw new IllegalStateException("layout index should not be -1 after "
                    + "unhiding a view:" + vh + exceptionLabel());
        }
        mChildHelper.detachViewFromParent(layoutIndex);
        //回收到scrap数组
        scrapView(view);
        //ViewHolder添加标记FLAG_RETURNED_FROM_SCRAP和FLAG_BOUNCED_FROM_HIDDEN_LIST
        vh.addFlags(ViewHolder.FLAG_RETURNED_FROM_SCRAP
                | ViewHolder.FLAG_BOUNCED_FROM_HIDDEN_LIST);
        return vh;
    }
  ...
}
```
2 在LayoutManager里面的scrapOrRecycleView方法也会调用Recycler的scrapView方法。而有两种情形下会出现如此情况：
  1. 手动调用了LayoutManager相关的方法;  手动调用detachAndScrapViewAt和detachAndScrapView
  2. RecyclerView进行了一次布局(调用了requestLayout方法)   LinearLayoutManager.onLayoutChildren调用了detachAndScrapAttachedViews
```
public void detachAndScrapViewAt(int index, @NonNull Recycler recycler) {
        final View child = getChildAt(index);
        scrapOrRecycleView(recycler, index, child);
    }

public void detachAndScrapView(@NonNull View child, @NonNull Recycler recycler) {
            int index = mChildHelper.indexOfChild(child);
            scrapOrRecycleView(recycler, index, child);
        }
public void detachAndScrapAttachedViews(@NonNull Recycler recycler) {
            final int childCount = getChildCount();
            for (int i = childCount - 1; i >= 0; i--) {
                final View v = getChildAt(i);
                scrapOrRecycleView(recycler, i, v);
            }
        }
private void scrapOrRecycleView(Recycler recycler, int index, View view) {
            final ViewHolder viewHolder = getChildViewHolderInt(view);
            if (viewHolder.shouldIgnore()) {
                return;
            }
            if (viewHolder.isInvalid() && !viewHolder.isRemoved()
                    && !mRecyclerView.mAdapter.hasStableIds()) {
                removeViewAt(index);
                recycler.recycleViewHolderInternal(viewHolder);
            } else {
                detachViewAt(index);
                recycler.scrapView(view);
                mRecyclerView.mViewInfoStore.onViewDetached(viewHolder);
            }
        }
```


(2). mCachedViews数组
mCachedViews数组作为二级缓存，回收的路径相较于一级缓存要多。关于mCachedViews数组，重点在于Recycler的recycleViewHolderInternal方法里面。
 我将mCachedViews数组的回收路径大概分为2类，我们来看看：
1 在复用时，从一级缓存里面获取到ViewHolder，但是此时这个ViewHolder已经不符合一级缓存的特点了(比如Position失效了，跟ViewType对不齐)，
  就会从一级缓存里面移除这个ViewHolder，从添加到mCachedViews里面   对应tryGetViewHolderForPositionByDeadline()方法
2 当调用removeAnimatingView方法时，如果当前ViewHolder被标记为remove,会调用recycleViewHolderInternal方法来回收对应的ViewHolder。
  调用removeAnimatingView方法的时机表示当前的ItemAnimator已经做完了。 该方法在ItemAnimatorListener.onAnimationFinished()调用

```       
 void recycleViewHolderInternal(ViewHolder holder) {
            final boolean transientStatePreventsRecycling = holder
                    .doesTransientStatePreventRecycling();
            @SuppressWarnings("unchecked") final boolean forceRecycle = mAdapter != null
                    && transientStatePreventsRecycling
                    && mAdapter.onFailedToRecycleView(holder);
            boolean cached = false;
            boolean recycled = false;           
            if (forceRecycle || holder.isRecyclable()) {
                if (mViewCacheMax > 0
                        && !holder.hasAnyOfTheFlags(ViewHolder.FLAG_INVALID
                        | ViewHolder.FLAG_REMOVED
                        | ViewHolder.FLAG_UPDATE
                        | ViewHolder.FLAG_ADAPTER_POSITION_UNKNOWN)) {
                    // 回收缓存中最老的view
                    int cachedViewSize = mCachedViews.size();
                    if (cachedViewSize >= mViewCacheMax && cachedViewSize > 0) {
                        recycleCachedViewAt(0);
                        cachedViewSize--;
                    }

                    int targetCacheIndex = cachedViewSize;
                    if (ALLOW_THREAD_GAP_WORK
                            && cachedViewSize > 0
                            && !mPrefetchRegistry.lastPrefetchIncludedPosition(holder.mPosition)) {
                        int cacheIndex = cachedViewSize - 1;
                        while (cacheIndex >= 0) {
                            int cachedPos = mCachedViews.get(cacheIndex).mPosition;
                            if (!mPrefetchRegistry.lastPrefetchIncludedPosition(cachedPos)) {
                                break;
                            }
                            cacheIndex--;
                        }
                        targetCacheIndex = cacheIndex + 1;
                    }
                    //添加到mCachedViews
                    mCachedViews.add(targetCacheIndex, holder);
                    cached = true;
                }
                if (!cached) {
                    //如果不能添加到mCachedViews则添加到RecycledViewPool
                    addViewHolderToRecycledViewPool(holder, true);
                    recycled = true;
                }
            } else {
               
            }
            mViewInfoStore.removeViewHolder(holder);
            if (!cached && !recycled && transientStatePreventsRecycling) {
                holder.mBindingAdapter = null;
                holder.mOwnerRecyclerView = null;
            }
        }
        
   boolean removeAnimatingView(View view) {
        startInterceptRequestLayout();
        final boolean removed = mChildHelper.removeViewIfHidden(view);
        if (removed) {
            final ViewHolder viewHolder = getChildViewHolderInt(view);
            mRecycler.unscrapView(viewHolder);
            mRecycler.recycleViewHolderInternal(viewHolder);
        }
        stopInterceptRequestLayout(!removed);
        return removed;
    }       
```

(3). mHiddenViews数组
一个ViewHolder回收到mHiddenView数组里面的条件比较简单，如果当前操作支持动画，就会调用到RecyclerView的addAnimatingView方法，
  在这个方法里面会将做动画的那个View添加到mHiddenView数组里面去。通常就是动画期间可以会进行复用，因为mHiddenViews只在动画期间才会有元素。
```
    private void addAnimatingView(ViewHolder viewHolder) {
        final View view = viewHolder.itemView;
        final boolean alreadyParented = view.getParent() == this;
        mRecycler.unscrapView(getChildViewHolder(view));
        if (viewHolder.isTmpDetached()) {
            mChildHelper.attachViewToParent(view, -1, view.getLayoutParams(), true);
        } else if (!alreadyParented) {
            mChildHelper.addView(view, true);
        } else {
            mChildHelper.hide(view);
        }
    }
    
    //ChildHelper的attachViewToParent，addView，hide 都会将view添加到mHiddenViews
     private void hideViewInternal(View child) {
        mHiddenViews.add(child);
        mCallback.onEnteredHiddenState(child);
    }
```

(4). RecycledViewPool
1 RecycledViewPool跟mCacheViews,都是通过recycleViewHolderInternal方法来进行回收，所以情景与mCacheViews差不多，
  只不过当不满足放入mCacheViews时，才会放入到RecycledViewPool里面去。
2 在重新布局回收了。这种情况主要出现在调用了Adapter的notifyDataSetChanged方法,并且此时Adapter的hasStableIds方法返回为false。
从这里看出来，为什么notifyDataSetChange方法效率为什么那么低，同时也知道了为什么重写hasStableIds方法可以提高效率。
因为notifyDataSetChange方法使得RecyclerView将回收的ViewHolder放在RecycledViewPool，效率自然比较低。
// 为什么不同缓存存在速度差异  数据结构没有大差异，优先级问题，在RecycledViewPool查询时需要经过好几级的查询，相比第一级自然是慢了
notifyDataSetChanged的调用路径
mObservable.notifyChanged()-》RecyclerViewDataObserver.onChanged()->RecyclerView.processDataSetCompletelyChanged()
->markKnownViewsInvalid()->Recycler.markKnownViewsInvalid()

添加到RecycledViewPool通过addViewHolderToRecycledViewPool()方法
```
//Recycler
void markKnownViewsInvalid() {
            final int cachedCount = mCachedViews.size();
            for (int i = 0; i < cachedCount; i++) {
                final ViewHolder holder = mCachedViews.get(i);
                if (holder != null) {
                    holder.addFlags(ViewHolder.FLAG_UPDATE | ViewHolder.FLAG_INVALID);
                    holder.addChangePayload(null);
                }
            }
            //如果adapter没有重写stableIds回收并清空CachedViews 
            if (mAdapter == null || !mAdapter.hasStableIds()) {
                // we cannot re-use cached views in this case. Recycle them all
                recycleAndClearCachedViews();
            }
        }
 void recycleAndClearCachedViews() {
            final int count = mCachedViews.size();
            for (int i = count - 1; i >= 0; i--) {
                recycleCachedViewAt(i);
            }
            mCachedViews.clear();
            if (ALLOW_THREAD_GAP_WORK) {
                mPrefetchRegistry.clearPrefetchPositions();
            }
        }  
    //将 CachedView的viewHolder回收到RecycledViewPool   
    void recycleCachedViewAt(int cachedViewIndex) {
            ViewHolder viewHolder = mCachedViews.get(cachedViewIndex);
            addViewHolderToRecycledViewPool(viewHolder, true);
            mCachedViews.remove(cachedViewIndex);
        } 
            
void addViewHolderToRecycledViewPool(@NonNull ViewHolder holder, boolean dispatchRecycled) {
    clearNestedRecyclerViewIfNotNested(holder);
    View itemView = holder.itemView;
    if (mAccessibilityDelegate != null) {
        AccessibilityDelegateCompat itemDelegate = mAccessibilityDelegate.getItemDelegate();
        AccessibilityDelegateCompat originalDelegate = null;
        if (itemDelegate instanceof RecyclerViewAccessibilityDelegate.ItemDelegate) {
            originalDelegate =
                    ((RecyclerViewAccessibilityDelegate.ItemDelegate) itemDelegate)
                            .getAndRemoveOriginalDelegateForItem(itemView);
        }
        // Set the a11y delegate back to whatever the original delegate was.
        ViewCompat.setAccessibilityDelegate(itemView, originalDelegate);
    }
    if (dispatchRecycled) {
        //分发view回收的监听事件 
        dispatchViewRecycled(holder);
    }
    holder.mBindingAdapter = null;
    holder.mOwnerRecyclerView = null;
    //放到RecycledViewPool
    getRecycledViewPool().putRecycledView(holder);
}

public void putRecycledView(ViewHolder scrap) {
            //找到holder的类型
            final int viewType = scrap.getItemViewType();
            final ArrayList<ViewHolder> scrapHeap = getScrapDataForType(viewType).mScrapHeap;
            if (mScrap.get(viewType).mMaxScrap <= scrapHeap.size()) {
                return;
            }
            scrap.resetInternal();
            //根据type添加到mScrap.scrapHeap
            scrapHeap.add(scrap);
        }
void resetInternal() {
            mFlags = 0;
            mPosition = NO_POSITION;
            mOldPosition = NO_POSITION;
            mItemId = NO_ID;
            mPreLayoutPosition = NO_POSITION;
            mIsRecyclableCount = 0;
            mShadowedHolder = null;
            mShadowingHolder = null;
            clearPayload();
            mWasImportantForAccessibilityBeforeHidden = ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
            mPendingAccessibilityState = PENDING_ACCESSIBILITY_STATE_NOT_SET;
            clearNestedRecyclerViewIfNotNested(this);
        }        
```
注意添加到RecycledViewPool时会将holder的flag,position等属性进行重置
 其他重置的场景
   从RecycledViewPool取出holder
   stopIgnoringView后会进行重置



