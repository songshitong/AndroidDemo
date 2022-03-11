https://www.jianshu.com/p/bdd9f4bdd90a


熟悉RecyclerView的同学应该都知道，Adapter作为RecyclerView四大组成部分(Adapter，LayoutManager，ItemAnimator，ItemDecoration)之一，
 其重要性自然是不言而喻。今天，我们来分析一下Adapter的源码。我打算将Adapter的源码分析分为两个部分，一是，从普通的角度上来看Adapter，
  从源码的角度上来分析我们日常使用的一些操作；二是，分析DiffUtil，可能会涉及到Adapter的部分源码。所以Adapter源码分析分为两篇，本文是第一篇

1. 概述
   在分析Adapter源码之前，我们先来回顾一下，我们经常使用的几个方法。

方法名	             作用
onCreateViewHolder	创建一个ViewHolder对象，主要作用是将数据保存在ViewHolder，以供后面bind操作使用
onBindViewHolder	数据绑定方法
getItemCount	    当前Adapter拥有数据的数量，该方法必须被重写，否则RecyclerView展示不了任何数据
getItemViewType	    该方法带一个Position，主要是返回当前位置的ViewType。这个方法通常用于一个RecyclerView需要加载不同的布局。
getItemId	        该方法表示的意思是返回当前位置Item的id，此方法只在setHasStableIds设置为true才会生效
setHasStableIds	    设置当前RecyclerView的ItemView是否拥有固定id,跟getItemId方法一起使用。如果设置为true，
                     会提高RecyclerView的缓存效率。 

上表中所列的方法应该就是我们使用Adapter经常使用的方法，接下来，我将正式分析Adapter的相关代码。我打算从如下角度来分析：
1 重新从RecyclerView缓存角度来分析onCreateViewHolder和onBindViewHolder。
2 onBindViewHolder的一个重载方法--主要是用于局部刷新。
3 结合Adapter，分析ViewHolder的position。


1. onCreateViewHolder和onBindViewHolder
   onCreateViewHolder方法和onBindViewHolder方法算是我们使用次数最多的方法，很多自定义Adapter的框架也都是从这两个方法入手的。我们来看看这两个方法到底有什么作用。

(1).onCreateViewHolder
首先，我们来看一下onCreateViewHolder方法，从它的调用时机入手。
在本文之前，我分析过RecyclerView的缓存机制，当时我将RecyclerView的缓存分为4级缓存，其中分别是：
1 一级缓存：scrap数组
2 二级缓存：CachedView
3 三级缓存：ViewCacheExtension
4 四级缓存：RecyclerViewPool

LayoutManager会获取ViewHolder时，如果4级缓存都没有命中，就会调用Adapter的onCreateViewHolder方法来创建一个新的ViewHolder。
  我们来看看相关的代码：
```
ViewHolder tryGetViewHolderForPositionByDeadline(int position,boolean dryRun, long deadlineNs) {
...
if (holder == null) {
        long start = getNanoTime();
        if (deadlineNs != FOREVER_NS
                && !mRecyclerPool.willCreateInTime(type, start, deadlineNs)) {
            // abort - we have a deadline we can't meet
            return null;
        }
        holder = mAdapter.createViewHolder(RecyclerView.this, type);
        if (ALLOW_THREAD_GAP_WORK) {
            // only bother finding nested RV if prefetching
            RecyclerView innerView = findNestedRecyclerView(holder.itemView);
            if (innerView != null) {
                holder.mNestedRecyclerView = new WeakReference<>(innerView);
            }
        }

        long end = getNanoTime();
        mRecyclerPool.factorInCreateTime(type, end - start);                    
    }
...
}
```
之前，我们在分析缓存机制时，就已经仔细分析这个方法，这里我就不再赘述
从上面的代码上，我们看到这里是调用的是Adapter的createViewHolder方法来创建ViewHolder。我们来看看Adapter的createViewHolder方法：
```
public final VH createViewHolder(@NonNull ViewGroup parent, int viewType) {
            try {
                TraceCompat.beginSection(TRACE_CREATE_VIEW_TAG);
                final VH holder = onCreateViewHolder(parent, viewType);
                if (holder.itemView.getParent() != null) {
                    throw new IllegalStateException("ViewHolder views must not be attached when"
                            + " created. Ensure that you are not passing 'true' to the attachToRoot"
                            + " parameter of LayoutInflater.inflate(..., boolean attachToRoot)");
                }
                holder.mItemViewType = viewType;
                return holder;
            } finally {
                TraceCompat.endSection();
            }
        }
```
其实createViewHolder方法里面也没有做什么的操作，差不多就是调用onCreateViewHolder方法。简而言之，onCreateViewHolder有点
  带兜底的韵味，缓存都没有命中，只能乖乖的创建ViewHolder。
我们来看看第二方法，也就是onBindViewHolder方法

(2). onBindViewHolder
我们都知道，onBindViewHolder方法的作用是进行数据绑定，所以执行这个方法的条件相对于onCreateViewHolder有点苛刻。为什么呢？
 我们这么想一下吧，假设我们change了其中一个ItemView的数据，然后通过notifyItemChanged来通知数据源已经改变。在这种情况下，
 正常来说，都是只刷新对应位置的ItemView就行了，没必要刷新其他数据没有改变的ItemView(这里的刷新就是指执行onBindViewHolder方法)。
现在，我们来看看对应的执行代码：
```
ViewHolder tryGetViewHolderForPositionByDeadline(int position,boolean dryRun, long deadlineNs) {
...
boolean bound = false;
            if (mState.isPreLayout() && holder.isBound()) {
                // do not update unless we absolutely have to.
                holder.mPreLayoutPosition = position;
            } else if (!holder.isBound() || holder.needsUpdate() || holder.isInvalid()) {
                if (DEBUG && holder.isRemoved()) {
                    throw new IllegalStateException("Removed holder should be bound and it should"
                            + " come here only in pre-layout. Holder: " + holder
                            + exceptionLabel());
                }
                final int offsetPosition = mAdapterHelper.findPositionOffset(position);
                bound = tryBindViewHolderByDeadline(holder, offsetPosition, position, deadlineNs);
            }
...
}
```
从上面的代码，我们可以看出来，最后调用了tryBindViewHolderByDeadline方法。而调用tryBindViewHolderByDeadline方法条件比较苛刻，
 不过不管怎么苛刻，只要记住一点，如果对应位置的数据被更新了，该位置会执行一次onBindViewHolder方法。
我们继续看一下tryBindViewHolderByDeadline方法的代码：
```
private boolean tryBindViewHolderByDeadline(@NonNull ViewHolder holder, int offsetPosition,
                int position, long deadlineNs) {
            holder.mOwnerRecyclerView = RecyclerView.this;
            final int viewType = holder.getItemViewType();
            long startBindNs = getNanoTime();
            if (deadlineNs != FOREVER_NS
                    && !mRecyclerPool.willBindInTime(viewType, startBindNs, deadlineNs)) {
                // abort - we have a deadline we can't meet
                return false;
            }
            mAdapter.bindViewHolder(holder, offsetPosition);
            long endBindNs = getNanoTime();
            mRecyclerPool.factorInBindTime(holder.getItemViewType(), endBindNs - startBindNs);
            attachAccessibilityDelegateOnBind(holder);
            if (mState.isPreLayout()) {
                holder.mPreLayoutPosition = position;
            }
            return true;
        }
```

执行过程跟onCreateViewHolder方法差不多，都是在依靠Adapter内部一个对应的final方法来回调。这样所做的好处，
 可以在onBindViewHolder方法执行前后做一些其他的操作，比如初始化操作和清理操作

我们来看看Adapter的bindViewHolder方法:
```
public final void bindViewHolder(@NonNull VH holder, int position) {
            holder.mPosition = position;
            if (hasStableIds()) {
                holder.mItemId = getItemId(position);
            }
            holder.setFlags(ViewHolder.FLAG_BOUND,
                    ViewHolder.FLAG_BOUND | ViewHolder.FLAG_UPDATE | ViewHolder.FLAG_INVALID
                            | ViewHolder.FLAG_ADAPTER_POSITION_UNKNOWN);
            TraceCompat.beginSection(TRACE_BIND_VIEW_TAG);
            onBindViewHolder(holder, position, holder.getUnmodifiedPayloads());
            holder.clearPayload();
            final ViewGroup.LayoutParams layoutParams = holder.itemView.getLayoutParams();
            if (layoutParams instanceof RecyclerView.LayoutParams) {
                ((LayoutParams) layoutParams).mInsetsDirty = true;
            }
            TraceCompat.endSection();
        }
```
从这里，我们可以简单发现，在执行onBindViewHolder方法前后，各自做了一些不同的操作。比如，在执行onBindViewHolder方法之前，
 更新了ViewHolder的mPosition属性和给ViewHolder设置了一些flag；在执行onBindViewHolder方法之后，清理了ViewHolder的payload，
 并且还是给ItemView的LayoutParams的mInsetsDirty属性设置为true。
这里额外的提出两个点:
1 payload主要是用于局部刷新的，待会我会详细解释怎么进行局部刷新。
2 关于LayoutParams的mInsetsDirty属性，这个属性尤为重要的，主要用于ItemView的绘制，后续我在分析ItemDecoration时会详细的解释这个属性。
//todo ItemDecoration
https://www.jianshu.com/p/20851e4e32a7



3. 局部刷新的基本使用和实现原理
   (1). 局部刷新的基本使用
   在分析局部刷新之前，我们先来讨论一下怎么进行布局刷新，也就是说怎么通过RecyclerView实现ItemView的局部刷新

点击一下下面灰色的Button，position为0的ItemView会改变显示的文字。如果我们不做局部刷新，出现什么问题呢？我们先来试试：
```
        mDataList.get(0).setString("change data");
        mAdapter.notifyItemChanged(0);
```

正常的实现应该就是上面的代码，非常的简单，也是我们经常书写的代码。这样书写有什么问题吗？有很大的问题！就是整个ItemView会闪烁一下，效果如下：
//todo 代码测试

网上给了一堆的原因分析，我个人觉得，原因非常的简单，就是第一个ItemView执行的change动画。所以介于这两个原因，我们可以找到两种解决方案：
1 设置RecyclerView的change动画时间为0，也就是调用ItemAnimator的setChangeDuration方法。
2 直接将RecyclerView的ItemAnimator设置为null

对于第二种方案，我不置可否。这样来想，我们直接将动画设置为null，那么RecyclerView就没有任何动画，是不是感觉有点得不偿失？
第一种方案比起第二种方案稍微要好一些，我们将change动画时间设置为0，只影响了change动画(相当于取消了change动画)，不会影响其他其他操作的动画。
  不过，还是感觉美中不足，相当于后面所有的change操作都没有了动画，如果我想有些change操作有动画呢？

此时就需要局部刷新出手了。我们先来看看怎么实现局部刷新：
首先，调用带两个参数的notifyItemChanged方法，如下
```
        mAdapter.notifyItemChanged(0, "");
```
然后我们得重写Adapter的onBindViewHolder方法(这里重写的是带三个参数的onBindViewHolder方法，带两个参数的onBindViewHolder该怎么写就怎么写)。
```
    public void onBindViewHolder(@NonNull RecyclerViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.contains("")) {
            holder.mTextView.setText(mDataList.get(position).getString());
        } else {
            super.onBindViewHolder(holder, position, payloads);
        }
    }
```

这里我们判断了一下payloads里面是否含有之前我们传递的空字符串，如果含有的话，直接更新显示文字即可，如果不含有则走默认逻辑。现在来我们看看效果：
是不是感觉perfect？


(2). 局部刷新的实现原理
局部刷新的使用是非常的简单的，就是重写了Adapter带三个参数的onBindViewHolder方法，然后调用的也是带两个参数的notifyItemChanged方法。
但是，我们不禁好奇，为什么这样做ItemView就不会闪烁呢？我在这里就可以告诉大家答案，是因为没有执行change动画。为了保持求知若饥，
  虚心若愚的良好传统，大家肯定会进一步的问，为什么在这种情况下不会执行动画呢？
其实为了回答这个问题，我早就已经为大家打好铺垫，在理解局部刷新的原理之前，大家最好已经理解了RecyclerView的动画机制
前面频繁的源码追踪我们这里就不进行了，可以参考我的文章：RecyclerView 源码分析(四) - RecyclerView的动画机制，这里直接从根源入手。
 我们都知道，当我们调用Adapter的notifyItemChanged方法，会执行到AdapterHelper$Callback的markViewHoldersUpdated方法。
而我们这里不看markViewHoldersUpdated方法，而是看哪里调用了这个方法。
```
 public final void notifyItemChanged(int position, @Nullable Object payload) {
            mObservable.notifyItemRangeChanged(position, 1, payload);
        }

        public void notifyItemRangeChanged(int positionStart, int itemCount,
                @Nullable Object payload) {
            for (int i = mObservers.size() - 1; i >= 0; i--) {
                mObservers.get(i).onItemRangeChanged(positionStart, itemCount, payload);
            }
        } 
 public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
            assertNotInLayoutOrScroll(null);
            if (mAdapterHelper.onItemRangeChanged(positionStart, itemCount, payload)) {
                triggerUpdateProcessor();
            }
        }               
```
triggerUpdateProcessor后面在04_rv动画相关已经分析了，最后调用到AdapterHelper的consumeUpdatesInOnePass
对于UpdateOp.UPDATE，执行mCallback.markViewHoldersUpdated

根据我们辛苦的追踪代码，我们发现主要是有两个地方在调用markViewHoldersUpdated方法：
1. postponeAndUpdateViewHolders方法；  //AdapterHelper
2. consumeUpdatesInOnePass方法。   //AdapterHelper
这其中，consumeUpdatesInOnePass方法是我们的老朋友，该方法主要是在dispatchLayoutStep2方法，其作用也是不言而喻，
   主要是给消费之前添加的operation。而postponeAndUpdateViewHolders方法我们就感觉非常的陌生，这个方法是在哪里被调用呢？
根据我们的追踪，发现它的调用源头是AdapterHelper的preProcess方法。而preProcess方法又是在哪里被调用的呢？
   是在processAdapterUpdatesAndSetAnimationFlags方法:
```RecyclerView
    private void processAdapterUpdatesAndSetAnimationFlags() {
        // ······
        if (predictiveItemAnimationsEnabled()) {
            mAdapterHelper.preProcess();
        } else {
            mAdapterHelper.consumeUpdatesInOnePass();
        }
        // ······
    }
```   


而processAdapterUpdatesAndSetAnimationFlags方法只在dispatchLayoutStep1方法调用(这里不考虑非自动测量的情况)。这里，
我们就彻底明了。dispatchLayoutStep1方法阶段被预布局阶段，也就是说，change操作在预布局阶段就已经回调markViewHoldersUpdated方法

而markViewHoldersUpdated方法的作用是啥呢？其实在RecyclerView 源码分析(四) - RecyclerView的动画机制，我就已经解释过了，主要作用有两个：
1 给每个ViewHolder打了对应的flag
2 更新每个ViewHolder的position。
关于这两个作用的分析，flag我们可以直接跳过，position在后面我会详细的分析。
从而，我们知道，在预布局阶段，每个ViewHolder的position和flag就已经确定了，这个有什么作用呢？还记得我们之前分析RecyclerView的动画机制说过，
 在预布局阶段如果条件允许的话，会进行一次布局，也就是会调用LayoutManager的onLayoutChildren方法。

而onLayoutChildren方法会做啥呢？我主要介绍两点(这里以LinearLayoutManager为例)：
1.调用LayoutManager的detachAndScrapAttachedViews方法，回收所有的ViewHolder，将他们放入四级缓存中。
2 调用fill方法进行布局。在fill方法调用流程会调用RecyclerView的tryGetViewHolderForPositionByDeadline方法从缓存中获取ViewHolder。

这里我们先来看回收部分，我们知道detachAndScrapAttachedViews方法最终会调用到Recycler的scrapView方法里面去。
  我们来看看scrapView方法(请大家睁大眼睛，这是寻找答案的第一条线索)：
```
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
从这里我们知道，scrapView方法的作用就是ViewHolder分别放到mAttachedScrap和mChanedScrap数组。
这里我们重点关注canReuseUpdatedViewHolder(holder)这个判断条件，我们来追踪这个方案的代码，最终我们找到了DefaultItemAnimator的
   canReuseUpdatedViewHolder方法:
```
    //SimpleItemAnimator.java
    public boolean canReuseUpdatedViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
        return !mSupportsChangeAnimations || viewHolder.isInvalid();
    }
    //DefaultItemAnimator
    public boolean canReuseUpdatedViewHolder(@NonNull ViewHolder viewHolder,
            @NonNull List<Object> payloads) {
        return !payloads.isEmpty() || super.canReuseUpdatedViewHolder(viewHolder, payloads);
    }
```

看到没？这里判断了一下payloads是否为空。这个有什么作用呢？我们回到scrapView方法来，如果payloads不为空的话，
   当前的ViewHolder会被回收到mAttachedScrap。这里，我们一定要记得，当ViewHolder的payloads不为空，那么在回收时，
   ViewHolder会被回收到mAttachedScrap。这个有什么作用呢？这就需要我们去寻找第二条线索。
第二条线索就藏在tryGetViewHolderForPositionByDeadline方法里面。我们来瞅瞅：
```
ViewHolder tryGetViewHolderForPositionByDeadline(int position,boolean dryRun, long deadlineNs) {
...
if (holder == null) {
                holder = getScrapOrHiddenOrCachedHolderForPosition(position, dryRun);
...                                
}
        ViewHolder getScrapOrHiddenOrCachedHolderForPosition(int position, boolean dryRun) {
            final int scrapCount = mAttachedScrap.size();

            // Try first for an exact, non-invalid match from scrap.
            for (int i = 0; i < scrapCount; i++) {
                final ViewHolder holder = mAttachedScrap.get(i);
                if (!holder.wasReturnedFromScrap() && holder.getLayoutPosition() == position
                        && !holder.isInvalid() && (mState.mInPreLayout || !holder.isRemoved())) {
                    holder.addFlags(ViewHolder.FLAG_RETURNED_FROM_SCRAP);
                    return holder;
                }
            }
            // ······
        }
```
结合上面的分析，当在预布局阶段，也就是dispatchLayoutStep1阶段进行布局，通过带两个参数的notifyItemChanged方法进行通知，
  肯定会在上面的代码返回一个ViewHolder。也就是说，在这种情况下，变化前后该ItemView的ViewHolder肯定是同一个ViewHolder
  //意思是先放到mAttachedScrap然后从里面取

如上就是第二条线索，那第二条线索有什么作用呢？就得看第三条线索了。那第三条线索在哪里呢？就在dispatchLayoutStep3方法里面。
我们都知道，dispatchLayoutStep3阶段被称为后布局，主要进行动画的执行，我们来看看我们的change操作会执行哪些代码：
```
private void dispatchLayoutStep3() {
        // ······
        if (mState.mRunSimpleAnimations) {
            for (int i = mChildHelper.getChildCount() - 1; i >= 0; i--) {
                           // ······
                        if (preInfo == null) {
                            handleMissingPreInfoForChangeError(key, holder, oldChangeViewHolder);
                        } else {
                            animateChange(oldChangeViewHolder, holder, preInfo, postInfo,
                                    oldDisappearing, newDisappearing);
                        }
                           // ······
        }
        // ······
    }
```
change操作肯定会执行到如上的代码，我们在分析动画机制时就已经分析过了。   //todo ???
我们来看看animateChange方法：
```
 private void animateChange(@NonNull ViewHolder oldHolder, @NonNull ViewHolder newHolder,
            @NonNull ItemHolderInfo preInfo, @NonNull ItemHolderInfo postInfo,
            boolean oldHolderDisappearing, boolean newHolderDisappearing) {
        oldHolder.setIsRecyclable(false);
        if (oldHolderDisappearing) {
            addAnimatingView(oldHolder);
        }
        if (oldHolder != newHolder) {
            if (newHolderDisappearing) {
                addAnimatingView(newHolder);
            }
            oldHolder.mShadowedHolder = newHolder;
            // old holder should disappear after animation ends
            addAnimatingView(oldHolder);
            mRecycler.unscrapView(oldHolder);
            newHolder.setIsRecyclable(false);
            newHolder.mShadowingHolder = oldHolder;
        }
        if (mItemAnimator.animateChange(oldHolder, newHolder, preInfo, postInfo)) {
            postAnimationRunner();
        }
    }
```
看到没，这就是最终的答案，只有两个ViewHolder不是同一个对象才会添加一个AnimatingView。
由于局部刷新的前后，ItemView的是同一个ViewHolder对象，才会导致局部刷新不会执行change动画，才会解决ItemView的闪烁。

有可能有人又有疑问了，为什么会全局刷新不是同一个ViewHolder呢？我们通过scrapView方法可以知道，如果全局刷新，
 那么change的ViewHolder会被回收到mChangedScrap数组里面去，而在tryGetViewHolderForPositionByDeadline方法里面，我们可以知道，
  只有预布局阶段才会从mChangedScrap数组里面获取ViewHolder对象：  
```
            if (mState.isPreLayout()) {
                holder = getChangedScrapViewForPosition(position);
                fromScrapOrHiddenOrCache = holder != null;
            }
```
所以预布局阶段和正式布局阶段同一个ItemView肯定是不同的ViewHolder,从而会执行change动画。   
//只有预布局是changedScrap，其他肯定不是，所以不一样

由于这个问题的答案寻找起来比较麻烦，这里我就针对这个问题做一个简单的总结：
布局刷新之所以能解决ItemView的闪烁问题，是因为在局部刷新的情况下，不会执行change动画。而不执行的chang动画的原因是因为在刷新前后
  都是同一个ViewHolder,并且都是从mAttachedScrap数组里面获得，所以在动画执行阶段，不会执行局部刷新导致的change动画，
  进而去掉闪烁的场景；而全局刷新由于刷新前后不是同一个ViewHolder,所以会执行change动画,所以出现闪烁的场景



4. ViewHolder的position
   在ViewHolder的内部有几个让人难以理解的问题，一个是flag，众多的flag让人非常的懵逼，这个我在缓存机制那一篇文章，我已经做了详细的总结，
     有兴趣的同学可以看看我的文章：RecyclerView 源码分析(三) - RecyclerView的缓存机制;另一个是position，本文来重点分析一下。
   这里主要分析两个方法，分别是getAbsoluteAdapterPosition和getLayoutPosition，对应着ViewHolder内部两个成员变量mPosition和mPreLayoutPosition两个属性。
   大家在使用这两个方法时，应该都对这两个方法有一定的疑问，这里我简单的解释一下这两个方法的区别(其实我们从这两个方法的注释就能看出区别)。
   我们先来看看这两个方法的代码，首先来看一下getAbsoluteAdapterPosition方法：
```
   public final int getAbsoluteAdapterPosition() {
            if (mOwnerRecyclerView == null) {
                return NO_POSITION;
            }
            return mOwnerRecyclerView.getAbsoluteAdapterPositionInRecyclerView(this);
        }
  int getAbsoluteAdapterPositionInRecyclerView(ViewHolder viewHolder) {
        if (viewHolder.hasAnyOfTheFlags(ViewHolder.FLAG_INVALID
                | ViewHolder.FLAG_REMOVED | ViewHolder.FLAG_ADAPTER_POSITION_UNKNOWN)
                || !viewHolder.isBound()) {
            return RecyclerView.NO_POSITION;
        }
        return mAdapterHelper.applyPendingUpdatesToPosition(viewHolder.mPosition);
    }
 
 ublic int applyPendingUpdatesToPosition(int position) {
        final int size = mPendingUpdates.size();
        for (int i = 0; i < size; i++) {
            UpdateOp op = mPendingUpdates.get(i);
            switch (op.cmd) {
                case UpdateOp.ADD:
                    if (op.positionStart <= position) {
                        position += op.itemCount;
                    }
                    break;
                case UpdateOp.REMOVE:
                    if (op.positionStart <= position) {
                        final int end = op.positionStart + op.itemCount;
                        if (end > position) {
                            return RecyclerView.NO_POSITION;
                        }
                        position -= op.itemCount;
                    }
                    break;
                case UpdateOp.MOVE:
                    if (op.positionStart == position) {
                        position = op.itemCount; //position end
                    } else {
                        if (op.positionStart < position) {
                            position -= 1;
                        }
                        if (op.itemCount <= position) {
                            position += 1;
                        }
                    }
                    break;
            }
        }
        return position;
    }          
```
别看getAbsoluteAdapterPosition方法比较麻烦，还调用了RecyclerView的getAbsoluteAdapterPositionInRecyclerView方法进行位置的计算。
但是它表达的意思是非常简单的，就是获取当前ViewHolder所绑定ItemView的真实位置。这里的真实位置说的比较笼统，这样来解释吧，当我们remove掉为position为0的item，
 正常来说，后面ViewHolder的position应该都减1。但是RecyclerView处理Adapter的更新采用的延迟处理策略，
 所以在正式处理之前获取ViewHolder的位置可能会出现误差，介于这个原因，getAbsoluteAdapterPosition方法就出现了。
getAbsoluteAdapterPosition方法是怎样保证每次计算都是正确的呢？包括在正式处理之前呢？
  我们知道，在RecyclerView中，延迟处理的实现是在notify阶段往一个叫mPendingUpdates数组里面添加Operation，
  分别在dispatchLayoutStep1阶段或者dispatchLayoutStep2阶段进行处理。通过追踪getAbsoluteAdapterPositionInRecyclerView方法，
  我们知道getAbsoluteAdapterPosition方法在计算位置时，考虑到mPendingUpdates数组的存在，所以在notify阶段和dispatchLayoutStep1阶段之间
  (这里假设dispatchLayoutStep1就会处理)，getAbsoluteAdapterPosition方法返回正确的位置。


而getLayoutPosition方法呢？getLayoutPosition方法就不能保证在notify阶段和dispatchLayoutStep1阶段之间获取的位置是正确的。
  为什么这么说呢？我们来看看getLayoutPosition方法的代码：

```
  public final int getLayoutPosition() {
            return mPreLayoutPosition == NO_POSITION ? mPosition : mPreLayoutPosition;
        }
```
getLayoutPosition方法返回的是mPosition或者mPreLayoutPosition,但是在dispatchLayoutStep1阶段之前，还未更新每个ViewHolder的position，
  所以获得不一定的是正确(只有在处理mPendingUpdates的操作时，position才会被更新，对应着的代码就是执行AdapterHelper$Callback接口的方法)。
但是getLayoutPosition方法为什么还有存在的必要呢？我们发现getLayoutPosition方法不会每次都计算，也就是说，
  getLayoutPosition方法的效率比getAdapterPosition方法高。当我们在Adapter这种调用方法来获取ViewHolder的位置时，
  可以优先考虑getLayoutPosition方法，因为Adapter的方法回调阶段不在mPendingUpdates处理之前，
  所以此时getLayoutPosition方法跟getAdapterPosition方法没有任何区别了。
但是需要注意，如果我们在其他地方获取ViewHolder的position，要特别注意这种情况，因为其他地方不能保证与RecyclerView状态同步，
  这种情况为了保证结果的正确性，我们应该优先考虑getAdapterPosition方法。

