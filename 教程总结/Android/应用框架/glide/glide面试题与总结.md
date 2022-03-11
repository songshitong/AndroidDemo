
https://jishuin.proginn.com/p/763bfbd664ba
图片加载总体流程
1.封装参数：从指定来源，到输出结果，中间可能经历很多流程，所以第一件事就是封装参数，这些参数会贯穿整个过程；
2.解析路径：图片的来源有多种，格式也不尽相同，需要规范化；
3.读取缓存：为了减少计算，通常都会做缓存；同样的请求，从缓存中取图片（Bitmap）即可；
4.查找文件/下载文件：如果是本地的文件，直接解码即可；如果是网络图片，需要先下载；
5.解码：这一步是整个过程中最复杂的步骤之一，有不少细节；
6.变换：解码出Bitmap之后，可能还需要做一些变换处理（圆角，滤镜等）；
7.缓存：得到最终bitmap之后，可以缓存起来，以便下次请求时直接取结果；
8.显示：显示结果，可能需要做些动画（淡入动画，crossFade等）

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

Glide的缓存机制，主要分为2种缓存，一种是内存缓存，一种是磁盘缓存。    //todo 磁盘缓存的两种
内存缓存防止应用重复将图片读入到内存，造成内存资源浪费。
磁盘缓存防止应用重复的从网络或者其他地方下载和读取数据。

内存缓存
内存缓存其实分两个部分，ActiveResource缓存与LRU缓存
ActiveResources 就是一个弱引用的 HashMap ，用来缓存正在使用中的图片,使用 ActiveResources 来缓存正在使用中的图片，
   可以保护这些图片不会被 LruCache 算法回收掉
内存缓存加载顺序如下：                    //todo  流程顺序，活动资源的大小限制
1.根据图片地址，宽高，变换，签名等生成key
2.第一次加载没有获取到活动缓存。
3.接着加载内存资源缓存，先清理掉内存缓存，在添加进行活动缓存。
4.第二次加载活动缓存已经存在。
5.当前图片引用为 0 的时候，清理活动资源，并且添加进内存资源。
6.又回到了第一步，然后就这样环环相扣。
为什么设计两种内存缓存？
LruCache算法的实现，你会发现它其实是用一个LinkedHashMap来缓存对象的，每次内存超出缓存设定的时候，就会把最近最少使用的缓存去掉，
  因此有可能会把正在使用的缓存给误伤了，我还在用着它呢就给移出去了。因此这个弱引用可能是对正在使用中的图片的一种保护，
  使用的时候先从LruCache里面移出去，用完了再把它重新加到缓存里面


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
  也可以缓存变换后的图片   //todo

为什么需要两种磁盘缓存
DiskCacheStrategy.RESOURCE缓存的是变换后的资源，DiskCacheStrategy.DATA缓存的是变换前的资源
举个例子，同一张图片，我们先在100*100的View是展示，再在200*200的View上展示
如果不缓存变换后的类型相当于每次都要进行一次变换操作，如果不缓存原始数据则每次都要去重新下载数据
如下可以看出，两种缓存的key不一样   //todo
```
DiskCacheStrategy.RESOURCE
currentKey = new ResourceCacheKey(helper.getArrayPool(),sourceId,helper.getSignature(),helper.getWidth(),helper.getHeight(),transformation,resourceClass,helper.getOptions());

DiskCacheStrategy.DATA
DataCacheKey newOriginalKey = new DataCacheKey(loadData.sourceKey, helper.getSignature());
```

DiskLruCache   DiskLruCacheWrapper是glide对DiskLruCache包装
DiskLruCache会对操作保存到journal文件 文件主要保存四种操作  journal文件记录超过2000会进行重建，只记录DIRTY和CLEAN
 DIRTY：第六行以DIRTY前缀开始，后面跟着缓存文件的key，表示一个entry正在被写入。   edit()方法
 CLEAN：当写入成功，就会写入一条CLEAN记录，后面的数字记录文件的长度，如果一个key可以对应多个文件，那么就会有多个数字   
     调用edit()之后进行commit()
 REMOVE：表示写入失败，或者调用remove(key)方法的时候都会写入一条REMOVE记录
 READ：表示一次读取记录
存储文件目录名image_manager_disk_cache，默认大小250M
  超过250M，后台线程glide-disk-lru-cache-thread会根据journal文件的记录生成的LinkedHashMap<String, Entry>lruEntries
     按照LRU淘汰链表头部也就是最老的。每次访问文件都会把对应的entry移动到链表尾部


Glide做了哪些内存优化
1 尺寸优化
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

线程池
https://juejin.cn/post/7038795986482757669
GlideExecutor.java中有四个定义    可以在GlideBuilder.java中进行初始化
```
SourceExecutor  //加载源文件的线程池，包括网络加载
DiskCacheExecutor //加载硬盘缓存的线程池
UnlimitedSourceExecutor  //无限制的线程池
AnimationBuilder  //动画线程池
```

看一下几个线程池的构建
SourceExecutor
```
 public static GlideExecutor newSourceExecutor() {
    return newSourceBuilder().build();
  }
  
 public static GlideExecutor.Builder newSourceBuilder() {
    return new GlideExecutor.Builder(/*preventNetworkOperations=*/ false)
        .setThreadCount(calculateBestThreadCount())
        .setName(DEFAULT_SOURCE_EXECUTOR_NAME); //DEFAULT_SOURCE_EXECUTOR_NAME = "source"
 } 
 
 
public static final class Builder {
    private UncaughtThrowableStrategy uncaughtThrowableStrategy = UncaughtThrowableStrategy.DEFAULT;
   
   public Builder setThreadCount(@IntRange(from = 1) int threadCount) {
      //核心与最大线程数设置为一样的
      corePoolSize = threadCount;
      maximumPoolSize = threadCount;
      return this;
    }
     
   public GlideExecutor build() {
      ...
      //创建线程池
      ThreadPoolExecutor executor =
          new ThreadPoolExecutor(
              corePoolSize,
              maximumPoolSize,
              /*keepAliveTime=*/ threadTimeoutMillis,
              TimeUnit.MILLISECONDS,
              //PriorityBlockingQueue是一个具有优先级的无界阻塞队列。也就是说优先级越高越先执行。
              new PriorityBlockingQueue<Runnable>(),
              new DefaultThreadFactory(name, uncaughtThrowableStrategy, preventNetworkOperations));
      //NO_THREAD_TIMEOUT=0 也就是默认不设置
      if (threadTimeoutMillis != NO_THREAD_TIMEOUT) {
        executor.allowCoreThreadTimeOut(true);
      }

      return new GlideExecutor(executor);
    }
} 

    
 public static int calculateBestThreadCount() {
    if (bestThreadCount == 0) {
       //如果cpu数超过4则核心线程数为4  如果Cpu数小于4那么使用Cpu数作为核心线程数量  则bestThreadCount<=4
      //MAXIMUM_AUTOMATIC_THREAD_COUNT = 4
      bestThreadCount =
          Math.min(MAXIMUM_AUTOMATIC_THREAD_COUNT, RuntimeCompat.availableProcessors());
    }
    return bestThreadCount;
  }  
```
DefaultThreadFactory定义了失败策略
```
private static final class DefaultThreadFactory implements ThreadFactory {
    private static final int DEFAULT_PRIORITY =
        android.os.Process.THREAD_PRIORITY_BACKGROUND
            + android.os.Process.THREAD_PRIORITY_MORE_FAVORABLE;
            
  public synchronized Thread newThread(@NonNull Runnable runnable) {
      final Thread result =
          new Thread(runnable, "glide-" + name + "-thread-" + threadNum) {
            @Override
            public void run() {
              android.os.Process.setThreadPriority(
                  DEFAULT_PRIORITY); // NOPMD AccessorMethodGeneration
              if (preventNetworkOperations) {
                //todo 禁止进行网络请求
                StrictMode.setThreadPolicy(
                    new ThreadPolicy.Builder().detectNetwork().penaltyDeath().build());
              }
              try {
                super.run();
              } catch (Throwable t) {
                //定义失败的策略
                uncaughtThrowableStrategy.handle(t);
              }
            }
          };
      threadNum++;
      return result;
    }           
}  

//失败策略接口
public interface UncaughtThrowableStrategy {
    //忽略，什么都不做
    UncaughtThrowableStrategy IGNORE =
        new UncaughtThrowableStrategy() {
          @Override
          public void handle(Throwable t) {
            // ignore
          }
        };
    
    //打印错误日志，也就是默认执行的策略
    UncaughtThrowableStrategy LOG =
        new UncaughtThrowableStrategy() {
          @Override
          public void handle(Throwable t) {
            if (t != null && Log.isLoggable(TAG, Log.ERROR)) {
              Log.e(TAG, "Request threw uncaught throwable", t);
            }
          }
        };
    
    //抛出异常
    UncaughtThrowableStrategy THROW =
        new UncaughtThrowableStrategy() {
          @Override
          public void handle(Throwable t) {
            if (t != null) {
              throw new RuntimeException("Request threw uncaught throwable", t);
            }
          }
        };

    
    UncaughtThrowableStrategy DEFAULT = LOG;

    void handle(Throwable t);
  }          
```

DiskCacheExecutor
```
  public static GlideExecutor newDiskCacheExecutor() {
    return newDiskCacheBuilder().build();
  }
  
  public static GlideExecutor.Builder newDiskCacheBuilder() {
    return new GlideExecutor.Builder(/*preventNetworkOperations=*/ true)  //不进行网络请求
        .setThreadCount(DEFAULT_DISK_CACHE_EXECUTOR_THREADS)  //DEFAULT_DISK_CACHE_EXECUTOR_THREADS = 1
        .setName(DEFAULT_DISK_CACHE_EXECUTOR_NAME);  //DEFAULT_DISK_CACHE_EXECUTOR_NAME = "disk-cache"
  }
```

animationExecutor
```
 public static GlideExecutor newAnimationExecutor() {
    return newAnimationBuilder().build();
  }
  
 public static GlideExecutor.Builder newAnimationBuilder() {
    //bestThreadCount<=4
    int bestThreadCount = calculateBestThreadCount();
    // We don't want to add a ton of threads running animations in parallel with our source and
    // disk cache executors. Doing so adds unnecessary CPU load and can also dramatically increase
    // our maximum memory usage. Typically one thread is sufficient here, but for higher end devices
    // with more cores, two threads can provide better performance if lots of GIFs are showing at
    // once.
    int maximumPoolSize = bestThreadCount >= 4 ? 2 : 1;
    //maximumPoolSize是2或1  glide认为不需要大量的线程
    return new GlideExecutor.Builder(/*preventNetworkOperations=*/ true)
        .setThreadCount(maximumPoolSize)
        .setName(DEFAULT_ANIMATION_EXECUTOR_NAME);  //DEFAULT_ANIMATION_EXECUTOR_NAME = "animation"
  } 
```

UnlimitedSourceExecutor
```
  public static GlideExecutor newUnlimitedSourceExecutor() {
    return new GlideExecutor(
        new ThreadPoolExecutor(
            0,
            Integer.MAX_VALUE,
            KEEP_ALIVE_TIME_MS,  //KEEP_ALIVE_TIME_MS = TimeUnit.SECONDS.toMillis(10);
            TimeUnit.MILLISECONDS,
            new SynchronousQueue<Runnable>(),
            //DEFAULT_SOURCE_UNLIMITED_EXECUTOR_NAME = "source-unlimited";
            new DefaultThreadFactory(
                DEFAULT_SOURCE_UNLIMITED_EXECUTOR_NAME, UncaughtThrowableStrategy.DEFAULT, false)));
  }
```
UnlimitedSourceExecutor是一个核心为0,最大为Integer.MAX_VALUE，超时为10,使用无界队列

线程池对比
DiskCacheExecutor和SourceExecutor 采用固定核心线程数固定，适用于处理CPU密集型的任务，但是没有非核心线程。
  确保CPU在长期被工作线程使用的情况下，尽可能的少的分配线程，即适用执行长期的任务。
UnlimitedSourceExecutor采用无核心线程，非核心线程无限大适用于并发执行大量短期的小任务。在空闲的时候消耗资源非常少。
AnimationExecutor没有核心线程，非核心线程有限，同UnlimitedSourceExecutor的区别就是核心线程数量和工作队列不一致




线程池在EngineJob的使用
```
  public synchronized void start(DecodeJob<R> decodeJob) {
    this.decodeJob = decodeJob;
    //从disk加载使用diskCacheExecutor，否则
    // 如果使用无限制，则是sourceUnlimitedExecutor，否则
    //如果使用动画，则是animationExecutor，否则是sourceExecutor
    GlideExecutor executor =
        decodeJob.willDecodeFromCache() ? diskCacheExecutor : getActiveSourceExecutor();
    executor.execute(decodeJob);
  }
  private GlideExecutor getActiveSourceExecutor() {
    return useUnlimitedSourceGeneratorPool
        ? sourceUnlimitedExecutor
        : (useAnimationPool ? animationExecutor : sourceExecutor);
  }
```

Glide如何实现加载优先级
除了UnlimitedSourceExecutor其余的都是使用的PriorityBlockingQueue。PriorityBlockingQueue是一个具有优先级的无界阻塞队列。
 也就是说优先级越高越先执行。
我们知道图片的加载是在线程池中执行的DecodeJob，DecodeJob实现了Runnable和Comparable接口。当DecodeJob被提交到线程池的时候，
  如果需要加入工作队列会通过compareTo比较DecodeJob优先级
DecodeJob.java
```
  public int compareTo(@NonNull DecodeJob<?> other) {
    int result = getPriority() - other.getPriority();
    if (result == 0) {
      result = order - other.order;
    }
    return result;
  }
  
   private int getPriority() {
    return priority.ordinal();
  }
 
//优先级有4中 
public enum Priority {
  IMMEDIATE,
  HIGH,
  NORMAL,
  LOW,
}
```


开发者实现图片的优先级加载
```
Glide.with(context).load(url).priority(Priority.HIGH).into(view)
```
开发者指定使用UnlimitedSourceExecutor线程池
```
Glide.with(context).load("").useUnlimitedSourceGeneratorsPool(true).into(view)
```