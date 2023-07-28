https://juejin.cn/post/6844903661726859271


设置方向  mOrientationHelper只有在setOrientation时才初始化，并且多次用到，不设置可能卡顿
recyclerview-1.2.1  帮助LinearLayoutManager保持滚动位置
LinearLayoutManager.java   
```
public void setOrientation(@RecyclerView.Orientation int orientation) {
        if (orientation != mOrientation || mOrientationHelper == null) {
            mOrientationHelper =
                    OrientationHelper.createOrientationHelper(this, orientation);
            mAnchorInfo.mOrientationHelper = mOrientationHelper;
            mOrientation = orientation;
            requestLayout();
        }
    }
```



RecyclerView做性能优化要说复杂也复杂，比如说布局优化，缓存，预加载等等。其优化的点很多，在这些看似独立的点之间，
  其实存在一个枢纽：Adapter。因为所有的ViewHolder的创建和内容的绑定都需要经过Adaper的两个函数onCreateViewHolder和onBindViewHolder。
因此我们性能优化的本质就是要减少这两个函数的调用时间和调用的次数   //这两个函数是运行在UI线程

onCreateViewHolder
 减少调用次数-缓存
    增加mCachedViews缓存数量(默认为2个，可以设为10或20)  recyclerView.setItemViewCacheSize(20);
    增加根据position的ViewHolder缓存
    增加RecycledViewPool缓存数量,每个type默认为5个
    多个RecyclerView复用RecycledViewPool   setRecycledViewPool()  场景ViewPager中多个RV具有相同的item结构
       如果LayoutManager是LinearLayoutManager或其子类，需要手动开启这个特性：layout.setRecycleChildrenOnDetach(true)
       开启后，一个RecyclerView从window中detach后，它的子view会进入回收池而被其他RecyclerView使用
    自定义缓存，在进入RecyclerView之前就构建好ViewHolder缓存，在onCreateViewHolder的时候直接返回自定义缓存，
        减少第一次进入RecyclerView的卡顿
       也可以自定义ViewCacheExtension，然后设置setViewCacheExtension()

 减少执行时间
     减少inflate时间
        减少View的层级 
           LayoutInspector查看层级
           使用ConstrainLayout减少层级
       减少View对象的创建
          一个稍微复杂的 Item 会包含大量的 View，而大量的 View 的创建也会消耗大量时间，所以要尽可能简化 ItemView；设计 ItemType 时，
             对多 ViewType 能够共用的部分尽量设计成自定义 View，减少 View 的构造和嵌套
     使用kotlin DSL或Jatpack Compose,去掉IO和反射   使用new对象的方法进行布局
     预加载  使用AsyncLayoutInflater将xml提前初始化，缓存
 

onBindViewHolder
  减少调用次数-缓存
    增加mCachedViews
    增加根据position的ViewHolder缓存 
  减少执行时间
    非UI展示的必须耗时操作后移
       在NotifyDataChanged之后添加延迟任务(例如800ms),确保在RecyclerView绘制完成之后再执行该耗时操作
       图片请求和加载放到SCROLL_STATE_IDLE的时候，其他状态暂停图片请求
          RecyclerView.addOnScrollListener(listener)滑动过程中停止加载耗时的操作
    减少ImageView渲染时间
      使用协程 + Glide 同步加载方法，以缩减加载图片耗时   glide初始化需要启动线程池，而线程池的构建是昂贵的//线程创建涉及内核调用  //todo glide的同步方法
  预加载
     Glide是不是可以提前初始化一次，大部分框架都提前初始化了吧
     日期的比较和日期的格式化，是很耗时的，需要提前准备好，然后直接设置
     富文本解析 mTextView.setText(Html.fromHtml(data).toString())，需要Html.fromHtml(data).toString()提前准备好


局部更新
1. 分页加载远端数据，对拉取的远端数据进行缓存，提高二次加载速度；
2. 对于新增或删除数据通过DiffUtil，来进行局部数据刷新，而不是一味的全局刷新数据
   DiffUtil是support包下新增的一个工具类，用来判断新数据和旧数据的差别，从而进行局部刷新。
   DiffUtil的使用，在原来调用mAdapter.notifyDataSetChanged()的地方：
```
// mAdapter.notifyDataSetChanged()
DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffCallBack(oldDatas, newDatas), true);
diffResult.dispatchUpdatesTo(mAdapter);
```  
4  如果item高度是固定的话，可以使用RecyclerView.setHasFixedSize(true);来避免requestLayout浪费资源


绘图缓存  //todo 绘图缓存
```
recyclerView.setDrawingCacheEnabled(true);
recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
```

数据预取  适用于一个RecyclerView嵌套在另一个  一般设置为展示的个数   //todo 预取的规则
LinearLayoutManager.setInitialPrefetchItemCount()
重写LinearLayoutManager.calculateExtraLayoutSpace()增加 RecyclerView 预留的额外空间（显示范围之外，应该额外缓存的空间）
```
 protected void calculateExtraLayoutSpace(@NonNull RecyclerView.State state,
            @NonNull int[] extraLayoutSpace) {
            }
```

减少ItemView监听器的创建 
对ItemView设置监听器，不要对每个item都创建一个监听器，而应该共用一个XxListener，然后根据ID来进行不同的操作，
   优化了对象的频繁创建带来的资源消耗。

回收资源
通过重写RecyclerView.onViewRecycled(holder)来回收资源
onDetachedFromRecyclerView() 也可以，视频停止，图片停止加载等 去除item数据等

调用notifyItemchange  rangeChange 传入payload时触发
public void onBindViewHolder(@NonNull VH holder, int position,
@NonNull List<Object> payloads)

处理刷新闪烁
1 调用notifyDataSetChange时，适配器不知道整个数据集中的那些内容以及存在，再重新匹配ViewHolder时会花生闪烁。
设置adapter.setHasStableIds(true)，并重写getItemId()来给每个Item一个唯一的ID
源码中，当hasStableIds()为true，进入getScrapOrCachedViewForId(..itemId)，再判断itemId拿到缓存实例。相当于用itemId做了一个绑定，
   就不用重新创建和加载数据，这样就避免了图片闪烁。
存在一个大大的坑：
 因为getItemId()方法返回值是索引下标值position，当使用数据源集合里的position的话作为返回值的时候，因为业务逻辑集合增删后，
  数据源的位置就发生了变化，这样进入判断itemId时不能对号入座，再通知子项刷新notifyDataSetChanged()的时候就会仍然出现闪烁
  最好返回数据的HashCode
2 由于RecyclerView缓存view复用导致图片错乱
Recyclerview的缓存机制，作者主要在对RecyclerView的ItemView某些图片进行了属性动画变换，这样就改变了ViewHolder中ImageView的属性，在滑动时，
RecyclerView的缓存复用机制可能导致ViewHolder不会重新创建，也不会重新bindView，这样某些ItemView的图片是View属性动画变换后的图片，
导致不是自己想要的结果。
可以重写Adapter里的void onViewDetachedFromWindow(VH holder)方法，在里面拿到holder找到修改过的ImageView，恢复他原来的属性，
  特别是有View被缓存复用的时候一定记得恢复原来的属性，否则就会出现这种混乱的情况。

另外的解释
由于RecyclerView或者LIstView的复用机制，网络加载图片开始的时候ImageView是第一个item的，加载成功之后ImageView由于复用
   可能跑到第10个item去了，在第10个item显示第一个item的图片肯定是错的。
常规的做法是给ImageView设置tag，tag一般是图片地址，更新ImageView之前判断tag是否跟url一致。
当然，可以在item从列表消失的时候，取消对应的图片加载任务。要考虑放在图片加载框架做还是放在UI做比较合适。




Demo场景
1 RecyclerView的layout过程中  填充表项是一个 while 循环，有多少表项需要被填充，就会循环多少次
onCreateViewHolder()和onBindViewHolder()都会在这个循环中被调用。所以，表项越多，绘制越耗时
为了优化首次加载列表的性能，可以把第一屏的所有表项都合并成一个表项
虽然这个做法，让首次加载 RecyclerView 提速不少，但也有缺点。它为列表新增了一种表项类型，而且这个表项的 ViewHolder 持有超多的View，
   难免增加内存压力。并且它无法被后续表项复用。
这个做法对于 Demo 这种场景，也不失为一种优化加载速度的方法，即将可能显示在首屏的表项都合并一个新的表项类型，当下拉刷新时，
  还是正常的一个个加载原有的表项

2 把所有内容绘制在Canvas
单个表项中控件越多，表项复杂度越高，构建表项消耗的 measure + layout 时间就越长。
有没有什么办法把单个表项内的多个控件变成一个控件
直接把文字和图片绘制在画布上


//todo ConstrainLayout 背后的算法
https://cloud.tencent.com/developer/article/1645586?from=15425
https://zhuanlan.zhihu.com/p/208408804

线程池的构建是昂贵的，耗时的   // 
可以使用下面优化
协程+glide加载图片
```
fun ImageView.load(url: String) {
    viewScope.launch {
        val bitmap = Glide.with(context).asBitmap().load(url).submit().get()
        withContext(Dispatchers.Main) { setImageBitmap(bitmap) }
    }
}
viewScope的声明
val View.viewScope: CoroutineScope
    get() {
        // 获取现有 viewScope 对象
        val key = "ViewScope".hashCode()
        var scope = getTag(key) as? CoroutineScope
        // 若不存在则新建 viewScope 对象
        if (scope == null) {
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            // 将 viewScope 对象缓存为 View 的 tag
            setTag(key,scope)
            val listener = object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View?) {
                }
                override fun onViewDetachedFromWindow(v: View?) {
                    // 当 view detach 时 取消协程的任务
                    scope.cancel()
                }

            }
            addOnAttachStateChangeListener(listener)
        }
        return scope
    }

```


```
public static void setMaxFlingVelocity(RecyclerView recyclerView, final BaseAdapter adapter, final int velocity) {
        try {
            Field field = recyclerView.getClass().getDeclaredField("mMaxFlingVelocity");
            field.setAccessible(true);
            field.set(recyclerView, velocity);
        } catch (Exception e) {
            e.printStackTrace();
        }

        recyclerView.setOnFlingListener(new RecyclerView.OnFlingListener() {
            @Override
            public boolean onFling(int xv, int yv) {//xv是x方向滑动速度，yv是y方向滑动速度。    
                if (yv >= velocity) {
                    adapter.setScrolling(true);
                }else{
                    adapter.setScrolling(false);
                }
                return false;
            }
        });
 }
```
系统默认惯性滑动最大值mMaxFlingVelocity是8000，这个值是可以通过反射修改的。值越大，惯性滑动距离越远，越丝滑。
可以把惯性值加倍，让用户体验翻倍！   //todo 惯性滑动相关的