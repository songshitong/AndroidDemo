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