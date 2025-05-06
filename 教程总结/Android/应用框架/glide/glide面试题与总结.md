
https://blog.csdn.net/u010302765/article/details/103193470
Glide加载一个一兆的图片（100 * 100），是否会压缩后再加载，放到一个300 * 300的view上会怎样，800*800呢，图片会很模糊，
怎么处理？
分析
（因为你缓存机制无论是看博客还是看一些面试宝典，如果只是考原理或者定义，光把上面的文字背诵下来就可以了，但是背诵和真正的理解是两回事，
自己没有形成感悟，不理解这个框架，只是一味的迎合面试，这个问题就可以卡住你，另外千万别和面试官嘚瑟，果然，这个面试的哥们，
这块就卡住了，支支吾吾的半天没答上来，果然是只看了博客，没真正的阅读过源码）
答案
当我们调整imageview的大小时，Picasso会不管imageview大小是什么，总是直接缓存整张图片，而Glide就不一样了，
它会为每个不同尺寸的Imageview缓存一张图片，也就是说不管你的这张图片有没有加载过，只要imageview的尺寸不一样，
那么Glide就会重新加载一次，这时候，它会在加载的imageview之前从网络上重新下载，然后再缓存。
举个例子，如果一个页面的imageview是300 * 300像素，而另一个页面中的imageview是100 * 100像素，这时候想要让两个imageview像是同一张图片，
  那么Glide需要下载两次图片，并且缓存两张图片。
看到了吧，缓存Key的生成条件之一就是控件的长宽。
```
public <R> LoadStatus load() {
    // 根据请求参数得到缓存的键
    EngineKey key = keyFactory.buildKey(model, signature, width, height, transformations,
        resourceClass, transcodeClass, options);
```



https://blog.csdn.net/u010302765/article/details/103193470
简单说一下内存泄漏的场景，如果在一个页面中使用Glide加载了一张图片，图片正在获取中，如果突然关闭页面，这个页面会造成内存泄漏吗？
分析
（注意一定要审题，因为之前问了这个小伙，内存泄漏的原因，无非是长生命周期引用了短生命周期的对象等等，然后突然画风一变，
直接问了Glide加载图片会不会引起图片泄漏，这个小伙想也没想，直接回答道会引起内存泄漏，可以用LeakCanary检测，巴拉巴拉。。。）
答案
因为Glide 在加载资源的时候，如果是在 Activity、Fragment 这一类有生命周期的组件上进行的话，会创建一个透明的 RequestManagerFragment 加入到FragmentManager 之中，
感知生命周期，当 Activity、Fragment 等组件进入不可见，或者已经销毁的时候，Glide 会停止加载资源。
但是如果，是在非生命周期的组件上进行时，会采用Application 的生命周期贯穿整个应用，所以 applicationManager 只有在应用程序关闭的时候终止加载。



https://jishuin.proginn.com/p/763bfbd664ba
图片加载总体流程
1.封装参数：从指定来源，到输出结果，中间可能经历很多流程，所以第一件事就是封装参数，这些参数会贯穿整个过程； todo
2.解析路径：图片的来源有多种，格式也不尽相同，需要规范化；
3.读取缓存：为了减少计算，通常都会做缓存；同样的请求，从缓存中取图片（Bitmap）即可；
4.查找文件/下载文件：如果是本地的文件，直接解码即可；如果是网络图片，需要先下载；
5.解码：这一步是整个过程中最复杂的步骤之一，有不少细节；
6.变换：解码出Bitmap之后，可能还需要做一些变换处理（圆角，滤镜等）；  transform
7.缓存：得到最终bitmap之后，可以缓存起来，以便下次请求时直接取结果；
8.显示：显示结果，可能需要做些动画（淡入动画，crossFade等）     transition

设备分级 glide activityManager.isLowRamDevice()
低内存设备申请小一点的缓冲池

注册onLowMemory的监听
applicationContext.registerComponentCallbacks
清除缓存
```
memoryCache.clearMemory();
bitmapPool.clearMemory();
arrayPool.clearMemory();
```

缓存篇
缓存类型	缓存代表	            说明
活动缓存	ActiveResources	    如果当前对应的图片资源是从内存缓存中获取的，那么会将这个图片存储到活动资源中。
内存缓存	LruResourceCache	图片解析完成并最近被加载过，则放入内存中
磁盘缓存-资源类型	DiskLruCacheWrapper	被解码后的图片写入磁盘文件中
磁盘缓存-原始数据	DiskLruCacheWrapper	网络请求成功后将原始数据在磁盘中缓存

Glide的缓存机制，主要分为2种缓存，一种是内存缓存，一种是磁盘缓存。    
内存缓存防止应用重复将图片读入到内存，造成内存资源浪费。   //避免重复读取文件
磁盘缓存防止应用重复的从网络或者其他地方下载和读取数据。   //避免重复下载文件

内存缓存
内存缓存其实分两个部分，ActiveResource缓存与LRU缓存
ActiveResources 就是一个弱引用的 HashMap ，用来缓存正在使用中的图片,使用 ActiveResources 来缓存正在使用中的图片，
   可以保护这些图片不会被 LruCache 算法回收掉，弱引用可以在gc时被回收
不再使用中的图片使用LruCache来进行缓存的功能
内存缓存加载顺序如下：                    //活动资源没有大小限制，ActiveResource的回收过程在后台线程
1.根据图片地址，宽高，变换，签名等生成key    不同的变换transform后图片是不同的，需要进行区分
2.第一次加载没有获取到活动缓存。
3.接着加载内存资源缓存，先清理掉内存缓存，再添加进行活动缓存,引用计数加1
4.第二次加载活动缓存已经存在,引用计数加1
5.当前图片引用为 0 的时候，清理活动资源，并且添加进内存资源。
6.又回到了第一步，然后就这样环环相扣。

ActiveResource缓存原理
ActiveResources采用HashMap+WeakReference方式保存EngineResource对象，没有对集合size做限制，在使用WeakReference的时候，
创建了一个ReferenceQueue，来记录被GC回收的EngineResource对象，而且在创建ReferenceQueue时生成了一个线程池后台线程"glide-active-resources"，
不断地执行cleanReferenceQueue()方法，一旦ReferenceQueue取出不为空，便取出ref对象，执行cleanupActiveReference()方法
释放缓存并重用到MemoryCache

为什么设计两种内存缓存？
LruCache算法的实现，你会发现它其实是用一个LinkedHashMap来缓存对象的，每次内存超出缓存设定的时候，就会把最近最少使用的缓存去掉，
  因此有可能会把正在使用的缓存给误伤了，我还在用着它呢就给移出去了。因此这个弱引用可能是对正在使用中的图片的一种保护，
  使用的时候先从LruCache里面移出去，用完了再把它重新加到缓存里面
内存资源缓存 (LRU回收) -> 活动缓存 (引用计数为0) ->内存资源缓存

https://www.jianshu.com/p/4de87ebf5104
首先即使是使用了LruCache最近最少用算法，也无法避免OOM的结果，毕竟加载图片很消耗内存。但是如果把正在使用的资源放在弱引用里面结果就不同了。
弱引用相当于打上一个标记，当gc来的时候就会回收掉。一来我正在使用这个资源，即使gc来了，经过分析对象可达性，
如果没有使用者也会尝试把这个列表里面的资源全部回收掉。这样就尽量保证了不会出现OOM的情况。
//被回收了怎么办,下次重新加载  正在显示图片已经上传到GPU了,内存里的被GC回收了

MemoryCache的实现类是LruResourceCache
LruResourceCache继承LruCache，LruCache利用LinkedHashMap实现LRU功能，最新访问过的放在尾部，淘汰头部最老的
//什么时候缓存清空？
onTrimMemory或onLowMemory

磁盘缓存策略
DiskCacheStrategy.NONE：表示不缓存任何内容。
DiskCacheStrategy.RESOURCE：在资源解码后将数据写入磁盘缓存，即经过缩放等转换后的图片资源。
DiskCacheStrategy.DATA：在资源解码前将原始数据写入磁盘缓存。
DiskCacheStrategy.ALL ：使用DATA和RESOURCE缓存远程数据，仅使用RESOURCE来缓存本地数据。
DiskCacheStrategy.AUTOMATIC：它会尝试对本地和远程图片使用最佳的策略。当你加载远程数据时，AUTOMATIC 策略仅会存储未被你的加载过程修改过的原始数据，
  因为下载远程数据相比调整磁盘上已经存在的数据要昂贵得多。对于本地数据，AUTOMATIC 策略则会仅存储变换过的缩略图，
  因为即使你需要再次生成另一个尺寸或类型的图片，取回原始数据也很容易。默认使用这种缓存策略

在了解磁盘缓存时我们主要需要明确一个概念，是当我们使用 Glide 去加载一张图片的时候，Glide 默认并不会将原始图片展示出来，
 而是会对图片进行压缩和转换，总之就是经过种种一系列操作之后得到的图片，就叫转换过后的图片。 我们既可以缓存变换之前的原始图片，
  也可以缓存变换后的图片   

为什么需要两种磁盘缓存    //采样变化后bitmap尺寸不同，避免了多次变换， 应用transform的图片也可以缓存
DiskCacheStrategy.RESOURCE缓存的是变换后的资源，DiskCacheStrategy.DATA缓存的是变换前的资源
举个例子，同一张图片，我们先在100*100的View是展示，再在200*200的View上展示
如果不缓存变换后的类型相当于每次都要进行一次变换操作，如果不缓存原始数据则每次都要去重新下载数据
如下可以看出，两种缓存的key不一样   
```
DiskCacheStrategy.RESOURCE
currentKey = new ResourceCacheKey(helper.getArrayPool(),sourceId,helper.getSignature(),helper.getWidth(),helper.getHeight(),transformation,resourceClass,helper.getOptions());

DiskCacheStrategy.DATA
DataCacheKey newOriginalKey = new DataCacheKey(loadData.sourceKey, helper.getSignature());
```


DiskLruCache   DiskLruCacheWrapper是glide对DiskLruCache包装
代码中使用LinkedHashMap实现LRU缓存，与journal文件对应
DiskLruCache会对操作保存到journal文件 文件主要保存四种操作和对应的文件key，key一般是url的md5，作为缓存文件的名字
 DIRTY：第六行以DIRTY前缀开始，后面跟着缓存文件的key，表示一个entry正在被写入。   edit()方法
    表示缓存项正在被创建或者被更新，这时的缓存项应该是不可读的 https://juejin.cn/post/7097204016735584263
 CLEAN：当写入成功，就会写入一条CLEAN记录，后面的数字记录文件的长度，如果一个key可以对应多个文件，那么就会有多个数字   
     调用edit()之后进行commit()
    表示缓存项成功写入缓存，这时的缓存是可读的
 REMOVE：表示写入失败，或者调用remove(key)方法的时候都会写入一条REMOVE记录
 READ：表示一次读取记录  文件读取需要更新
存储文件目录名image_manager_disk_cache，默认大小250M
  超过250M，后台线程glide-disk-lru-cache-thread会根据journal文件的记录生成的LinkedHashMap<String, Entry>lruEntries
     按照LRU淘汰链表头部也就是最老的。每次访问文件都会把对应的entry移动到链表尾部
journal文件记录超过2000会进行重建，只记录DIRTY和CLEAN
触发时机：
remove(String key)  写入一条REMOVE，删除LinkedHashMap中的entry，开始触发后台清理
setMaxSize(long maxSize)   
get(String key)   新增一条read记录，判断是否journal超出，开始触发后台清理
提交修改commit()   如果成功，写入一条CLEAN记录,否则，写入一条REMOVE记录，判断是否journal超出，开始触发后台清理
后台清理
删除文件与对应的记录，是否需要重建journal


Glide做了哪些内存优化
1 尺寸优化  下采样的流程
当装载图片的容器例如ImageView只有100*100，而图片的分辨率为800 * 800，这个时候将图片直接放置在容器上，很容易OOM，
 同时也是对图片和内存资源的一种浪费。当容器的宽高都很小于图片的宽高，其实就需要对图片进行尺寸上的压缩，
  将图片的分辨率调整为ImageView宽高的大小，一方面不会对图片的质量有影响，同时也可以很大程度上减少内存的占用
我们通常使用inSampleSize对Bitmap进行尺寸缩放
如果inSampleSize 设置的值大于1，则请求解码器对原始的bitmap进行子采样图像，然后返回较小的图片来减少内存的占用，
  例如inSampleSize == 4，则采样后的图像宽高为原图像的1/4，而像素值为原图的1/16，也就是说采样后的图像所占内存也为原图所占内存的1/16；
  当inSampleSize <=1时，就当作1来处理也就是和原图一样大小。另外最后一句还注明，inSampleSize的值一直为2的幂，
  如1，2，4，8。任何其他的值也都是四舍五入到最接近2的幂。
计算流程
1.首先计算出图片与View的宽高比
2.根据缩放策略是省内存还是高品质，决定取宽高比的最大值还是最小值
3.当Build.VERSION.SDK_INT<=23时，一些格式的图片不能缩放，缩放因子置为 1   webp 
4.highestOneBit的功能是把我们计算的比例四舍五入到最接近2的幂
5.如果缩放策略为省内存，并且我们计算的SampleSize<exactScaleFactor,将inSampleSize*2   //todo exactScaleFactor的作用

2 图片格式优化
Bitmap所占内存大小，由宽*高*每像素所占内存决定，上面的尺寸优化决定宽高，图片格式优化决定每像素所占内存
在Glide4.0之前,Glide默认使用RGB565格式，比较省内存
  但是Glide4.0之后，默认格式已经变成了ARGB_8888格式了,这一优势也就不存在了。
  这本身也就是质量与内存之间的取舍，如果应用所需图片的质量要求不高，也可以修改默认格式

3 内存复用优化
Bitmap所占内存比较大，如果我们频繁创建与回收Bitmap，那么很容易造成内存抖动,所以我们应该尽量复用Bitmap内存
Glide主要使用了inBitmap与BitmapPool来实现内存的复用
inBitmap介绍
在 Android 3.0（API 级别 11）开始，系统引入了 BitmapFactory.Options.inBitmap 字段。如果设置了此选项，
那么采用 Options 对象的解码方法会在生成目标 Bitmap 时尝试复用 inBitmap，这意味着 inBitmap 的内存得到了重复使用，
从而提高了性能，同时移除了内存分配和取消分配。不过 inBitmap 的使用方式存在某些限制，在 Android 4.4（API 级别 19）
之前系统仅支持复用大小相同的位图，4.4 之后只要 inBitmap 的大小比目标 Bitmap 大即可

BitmapPool中传入宽高与格式Bitmap.config，得到一个可复用的对象，这样就实现了Bitmap的内存复用
BitmapPool的实现类LruBitmapPool通过策略模式处理不同版本SizeConfigStrategy(4.4之上)和AttributeStrategy
  都通过按LRU淘汰最老的
  存储格式GroupedLinkedMap<Key, Bitmap>
  SizeConfigStrategy通过宽高和格式计算size
  AttributeStrategy限定宽高的复用
LruBitmapPool存在默认限制


Glide如何管理生命周期
当我们在做一个网络请示时，页面退出时应该中止请示，不然容易造成内存泄漏
对于图片加载也是如此，我们在页面退出时应该中止请示，销毁资源。
但是我们使用Glide的时候却不需要在页面退出时做什么操作，说明Glide可以做到在页面关闭时自动释放资源
下面我们一起看下Glide是如何实现的
主要是两步:
1.调用时通过Glide.with传入context,利用context构建一个Fragment
  在当前Activity添加一个透明Fragment用于管理请示生命周期
2.监听Fragment生命周期，销毁时释放Glide资源  移除后续的请求
  构建RequestManager并传入Fragment生命周期

