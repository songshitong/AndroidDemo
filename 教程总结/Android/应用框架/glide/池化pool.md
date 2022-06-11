

一般Options与bitmap一一对应，也需要进行池化减少GC
BitmapFactory.Options的池化复用  使用在DownsampleStrategy.java
```
private static final Queue<BitmapFactory.Options> OPTIONS_QUEUE = Util.createQueue(0);

private static synchronized BitmapFactory.Options getDefaultOptions() {
    BitmapFactory.Options decodeBitmapOptions;
    synchronized (OPTIONS_QUEUE) {
      decodeBitmapOptions = OPTIONS_QUEUE.poll();
    }
    if (decodeBitmapOptions == null) {
      decodeBitmapOptions = new BitmapFactory.Options();
      resetOptions(decodeBitmapOptions);
    }

    return decodeBitmapOptions;
  }
  
  private static void releaseOptions(BitmapFactory.Options decodeBitmapOptions) {
    //resetOptions将options的属性进行重置
    resetOptions(decodeBitmapOptions);
    synchronized (OPTIONS_QUEUE) {
      OPTIONS_QUEUE.offer(decodeBitmapOptions);
    }
  }  
```


https://juejin.cn/post/6844903761698095118#heading-1
Bitmap的池化复用BitmapPool
```
public interface BitmapPool {

  long getMaxSize();
  //将池的初始大小乘以给定的乘数，以动态同步地允许用户调整池的大小
  void setSizeMultiplier(float sizeMultiplier);


  void put(Bitmap bitmap);

  @NonNull
  Bitmap get(int width, int height, Bitmap.Config config);

  //与get类似，但是bitmap可能已被擦除，可能包含随机数据
  @NonNull
  Bitmap getDirty(int width, int height, Bitmap.Config config);

  //清除pool中的所有bitmap
  void clearMemory();

  //根据不同的内存级别执行对应的清理
  void trimMemory(int level);
}
```
BitmapPool是一个接口，实现类有BitmapPoolAdapter和LruBitmapPool这两个
  BitmapPoolAdapter不对bitmap进行复用，用于对应不使用池化的场景
```
//DrawableToBitmapConverter.java  
static Resource<Bitmap> convert(BitmapPool bitmapPool, Drawable drawable, int width, int height) {
    ... 
    //NO_RECYCLE_BITMAP_POOL的类型为BitmapPoolAdapter
    BitmapPool toUse = isRecycleable ? bitmapPool : NO_RECYCLE_BITMAP_POOL;
    return BitmapResource.obtain(result, toUse);
  }
```
  LruBitmapPool采用策略模式，它自身不处理具体逻辑，真正的逻辑在LruPoolStrategy中

LruBitmapPool
LruBitmapPool是策略的执行者，也是缓存大小的控制者；
LruBitmapPool.java
```
public class LruBitmapPool implements BitmapPool {
  //池化策略
  private final LruPoolStrategy strategy;
  //pool里面bitmap的内存大小
  private long currentSize;
  
  LruBitmapPool(long maxSize, LruPoolStrategy strategy, Set<Bitmap.Config> allowedConfigs) {
    this.initialMaxSize = maxSize;
    this.maxSize = maxSize;
    this.strategy = strategy;
    this.allowedConfigs = allowedConfigs;
    this.tracker = new NullBitmapTracker();
  }
  public LruBitmapPool(long maxSize) {
    this(maxSize, getDefaultStrategy(), getDefaultAllowedConfigs());
  }
  //根据不同的版本执行不同的策略
  private static LruPoolStrategy getDefaultStrategy() {
    final LruPoolStrategy strategy;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      strategy = new SizeConfigStrategy();
    } else {
      strategy = new AttributeStrategy();
    }
    return strategy;
  }
  
   private static Set<Bitmap.Config> getDefaultAllowedConfigs() {
    Set<Bitmap.Config> configs = new HashSet<>(Arrays.asList(Bitmap.Config.values()));
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      // GIFs, among other types, end up with a native Bitmap config that doesn't map to a java
      // config and is treated as null in java code. On KitKat+ these Bitmaps can be reconfigured
      // and are suitable for re-use.
      configs.add(null);
    }
    //硬件位图不缓存
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      configs.remove(Bitmap.Config.HARDWARE);
    }
    return Collections.unmodifiableSet(configs);
  }
  
  //回收bitmap
  public synchronized void put(Bitmap bitmap) {
    。。。
    if (!bitmap.isMutable()
        || strategy.getSize(bitmap) > maxSize
        || !allowedConfigs.contains(bitmap.getConfig())) {
      ...
      //bitmap不可变更，pool的大小满了，config不支持，此时不进行池化复用，回收对应的bitmap
      bitmap.recycle();
      return;
    }
    //放入不同的策略中
    final int size = strategy.getSize(bitmap);
    strategy.put(bitmap);
    tracker.add(bitmap);
    //记录大小
    puts++;
    currentSize += size;

    ...
    //日志打印
    dump();

    evict();
  }
  
  
  private void evict() {
    trimToSize(maxSize);
  }
  
  private synchronized void trimToSize(long size) {
    while (currentSize > size) {
      //所有bitmap的大小和大于maxSize，移除最后一个
      final Bitmap removed = strategy.removeLast();
      //  This shouldn't ever happen, see #331.
      if (removed == null) {
        。。。
        currentSize = 0;
        return;
      }
      tracker.remove(removed);
      currentSize -= strategy.getSize(removed);
      evictions++;
      。。。
      dump();
      removed.recycle();
    }
  }
  
  public Bitmap get(int width, int height, Bitmap.Config config) {
    Bitmap result = getDirtyOrNull(width, height, config);
    if (result != null) {
      // Bitmaps in the pool contain random data that in some cases must be cleared for an image
      // to be rendered correctly. we shouldn't force all consumers to independently erase the
      // contents individually, so we do so here. See issue #131.
      //将bitmap填充为透明  todo eraseColor
      result.eraseColor(Color.TRANSPARENT);
    } else {
      //没有找到匹配的，新建一个
      result = createBitmap(width, height, config);
    }

    return result;
  }
  private synchronized Bitmap getDirtyOrNull(
      int width, int height, @Nullable Bitmap.Config config) {
    。。。
    // Config will be null for non public config types, which can lead to transformations naively
    // passing in null as the requested config here. See issue #194.
    //从strategy获取bitmap
    final Bitmap result = strategy.get(width, height, config != null ? config : DEFAULT_CONFIG);
    if (result == null) {
      。。。
      misses++;
    } else {
      hits++;
      currentSize -= strategy.getSize(result);
      tracker.remove(result);
      normalize(result);
    }
    。。。
    return result;
  }
  
  //设置这两个值提供的位图本质上等同于从Bitmap.createBitmap返回的位图。
  private static void normalize(Bitmap bitmap) {
    //todo
    bitmap.setHasAlpha(true);
    maybeSetPreMultiplied(bitmap);
  }

  @TargetApi(Build.VERSION_CODES.KITKAT)
  private static void maybeSetPreMultiplied(Bitmap bitmap) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      //设置位图是否应该把它的数据作为预乘  //todo
      bitmap.setPremultiplied(true);
    }
  }
  
  private static Bitmap createBitmap(int width, int height, @Nullable Bitmap.Config config) {
    return Bitmap.createBitmap(width, height, config != null ? config : DEFAULT_CONFIG);
  }
  
  public void clearMemory() {
   ..
    trimToSize(0);
  }
  
  public void trimMemory(int level) {
    ...
    if ((level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND)
        || ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            && (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN))) {
      clearMemory();
    } else if ((level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN)
        || (level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL)) {
      trimToSize(getMaxSize() / 2);
    }
  }
}
```
其中操作缓存的核心方法在strategy中，LruPoolStrategy也是一个策略接口，真正策略的实现类是SizeConfigStrategy(4.4之上)
和AttributeStrategy;
LruPoolStrategy.java
```
interface LruPoolStrategy {
  void put(Bitmap bitmap);

  @Nullable
  Bitmap get(int width, int height, Bitmap.Config config);

  @Nullable
  Bitmap removeLast();

  String logBitmap(Bitmap bitmap);

  String logBitmap(int width, int height, Bitmap.Config config);

  int getSize(Bitmap bitmap);
}
```
看一下低版本策略AttributeStrategy.java
```
class AttributeStrategy implements LruPoolStrategy {
 //将对应的key也进行了池化
 private final KeyPool keyPool = new KeyPool();
  //实现LRU的map
  private final GroupedLinkedMap<Key, Bitmap> groupedMap = new GroupedLinkedMap<>();

  @Override
  public void put(Bitmap bitmap) {
    final Key key = keyPool.get(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
    groupedMap.put(key, bitmap);
  }

  @Override
  public Bitmap get(int width, int height, Bitmap.Config config) {
    final Key key = keyPool.get(width, height, config);
    return groupedMap.get(key);
  }

  @Override
  public Bitmap removeLast() {
    //移除末尾的元素
    return groupedMap.removeLast();
  }

  @Override
  public int getSize(Bitmap bitmap) {
    return Util.getBitmapByteSize(bitmap);
  }
}
```
AttributeStrategy.Key重写equals()方法和hashCode()方法，其中hashCode()是用来识别Lru内部LinkedHashMap中的bucket，
equal()是真正的对比;
AttributeStrategy这个策略的核心目的，就是在缓存的Key上面做对比，只有缓存中的Bitmap同时满足width、height、config相等才能命中
AttributeStrategy.java
```
static class Key implements Poolable {
   public boolean equals(Object o) {
      if (o instanceof Key) {
        Key other = (Key) o;
        return width == other.width && height == other.height && config == other.config;
      }
      return false;
    }

    @Override
    public int hashCode() {
      int result = width;
      result = 31 * result + height;
      result = 31 * result + (config != null ? config.hashCode() : 0);
      return result;
    }
}

  static class KeyPool extends BaseKeyPool<Key> {
    Key get(int width, int height, Bitmap.Config config) {
      Key result = get();
      //从队列中取出一个key，然后设置它的属性
      result.init(width, height, config);
      return result;
    }

    @Override
    protected Key create() {
      return new Key(this);
    }
  }
```
看一下key的池化  key与bitmap一般是一一对应放入map的，当bitmap回收时，key也需要进行回收，对内存利用到了极致
BaseKeyPool.java   
```
abstract class BaseKeyPool<T extends Poolable> {
  private static final int MAX_SIZE = 20;
  //生成一个20的队列
  private final Queue<T> keyPool = Util.createQueue(MAX_SIZE);

  T get() {
    //从队列中取一个，为空的时新建一个
    T result = keyPool.poll();
    if (result == null) {
      result = create();
    }
    return result;
  }

  //入队
  public void offer(T key) {
    if (keyPool.size() < MAX_SIZE) {
      keyPool.offer(key);
    }
  }

  abstract T create();
}
```

SizeConfigStrategy
上面说了AttributeStrategy是面向低于Android4.4版本的Bitmap缓存策略，SizeConfigStrategy则是面向高版本的，从文章开头的部分我们知道，
高版本的inBitmap限制没有这么严格，至少在尺寸这一块是放开了，只有内存大小不小于需求就行；下面看看代码怎么实现的：
SizeConfigStrategy.java
```
public class SizeConfigStrategy implements LruPoolStrategy {
  private final KeyPool keyPool = new KeyPool();
  //缓存的bitmap
  private final GroupedLinkedMap<Key, Bitmap> groupedMap = new GroupedLinkedMap<>();
  // TODO NavigableMap   存储的是config,bitmap大小，这么大的bitmap的个数
  private final Map<Bitmap.Config, NavigableMap<Integer, Integer>> sortedSizes = new HashMap<>();
  
  public void put(Bitmap bitmap) {
    int size = Util.getBitmapByteSize(bitmap);
    //获取key
    Key key = keyPool.get(size, bitmap.getConfig());
    //保存到LRU
    groupedMap.put(key, bitmap);

    NavigableMap<Integer, Integer> sizes = getSizesForConfig(bitmap.getConfig());
    Integer current = sizes.get(key.size);
    //保存键值对，键是字节数大小，值是总共有多少个
    sizes.put(key.size, current == null ? 1 : current + 1);
  }
  
  private NavigableMap<Integer, Integer> getSizesForConfig(Bitmap.Config config) {
    NavigableMap<Integer, Integer> sizes = sortedSizes.get(config);
    if (sizes == null) {
      sizes = new TreeMap<>();
      sortedSizes.put(config, sizes);
    }
    return sizes;
  }
  
  
   public Bitmap get(int width, int height, Bitmap.Config config) {
    int size = Util.getBitmapByteSize(width, height, config);
    //获取最优的key
    Key bestKey = findBestKey(size, config);
    //从LRU中获取
    Bitmap result = groupedMap.get(bestKey);
    if (result != null) {
      // Decrement must be called before reconfigure.
      //操作sizeConfig集合，做减1操作或者移除
      decrementBitmapOfSize(bestKey.size, result);
      // 重新配置Bitmap宽高和config
      result.reconfigure(width, height, config);
    }
    return result;
  }
  
   private void decrementBitmapOfSize(Integer size, Bitmap removed) {
    Bitmap.Config config = removed.getConfig();
    NavigableMap<Integer, Integer> sizes = getSizesForConfig(config);
    Integer current = sizes.get(size);
    if (current == null) {
      throw new NullPointerException(
         ...
    }

    if (current == 1) {
      sizes.remove(size);
    } else {
      sizes.put(size, current - 1);
    }
  }
  
  static class KeyPool extends BaseKeyPool<Key> {

    public Key get(int size, Bitmap.Config config) {
      Key result = get();
      result.init(size, config);
      return result;
    }

    @Override
    protected Key create() {
      return new Key(this);
    }
  }
  static final class Key implements Poolable {
     public boolean equals(Object o) {
      if (o instanceof Key) {
        Key other = (Key) o;
        return size == other.size && Util.bothNullOrEqual(config, other.config);
      }
      return false;
    }

    @Override
    public int hashCode() {
      int result = size;
      result = 31 * result + (config != null ? config.hashCode() : 0);
      return result;
    }
  }
}
```
SizeConfigStrategy和AttributeStrategy有很多相似之处，但是复杂的多，相同的是都是用GroupedLinkedMap作为Lru存储，
不同之处是对于Key的获取以及多出一个辅助集合NavigableMap；Key的获取已经不依赖Width和Height了，而是size，它是Bitmap占用的字节数，
Key的hashCode()和equals()依赖的是size和config;
SizeConfigStrategy最关键的方法是getBestKey()，它的作用是获取最合适的Key；
```
 private Key findBestKey(int size, Bitmap.Config config) {
    //从pool里取出，肯定不为空
    Key result = keyPool.get(size, config);
    //获取匹配的Config,一般只有一个匹配
    for (Bitmap.Config possibleConfig : getInConfigs(config)) {
      NavigableMap<Integer, Integer> sizesForPossibleConfig = getSizesForConfig(possibleConfig);
      //获取不比size小的可能缓存的size，ceiling方法相当于是数学上的进一法 
      Integer possibleSize = sizesForPossibleConfig.ceilingKey(size);
      if (possibleSize != null && possibleSize <= size * MAX_SIZE_MULTIPLE) {
        //`size`不相等或者`config`不相等，此处的判断等于是判断了`!Key.equals()`逻辑，这时候才降低维度获取相近的key
        // config不相同，意味着是可以匹配的config，但是不用一样，增大命中率
        // 如果size和config都相同，直接返回上面的result,但是限制太严格了
        if (possibleSize != size
            || (possibleConfig == null ? config != null : !possibleConfig.equals(config))) {
          //result重新入池  
          keyPool.offer(result);
          //命中的key，他的size和目标相近但是肯定不完全一样
          result = keyPool.get(possibleSize, possibleConfig);
        }
        break;
      }
    }
    return result;
  }
  
   //根据config返回更多合适的config，对版本和特殊场景null进行了适配
   private static Bitmap.Config[] getInConfigs(Bitmap.Config requested) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      if (Bitmap.Config.RGBA_F16.equals(requested)) { // NOPMD - Avoid short circuiting sdk checks.
        return RGBA_F16_IN_CONFIGS;
      }
    }

    switch (requested) {
      case ARGB_8888:
        return ARGB_8888_IN_CONFIGS;
      case RGB_565:
        return RGB_565_IN_CONFIGS;
      case ARGB_4444:
        return ARGB_4444_IN_CONFIGS;
      case ALPHA_8:
        return ALPHA_8_IN_CONFIGS;
      default:
        return new Bitmap.Config[] {requested};
    }
  }
  
  private static final Bitmap.Config[] ARGB_8888_IN_CONFIGS;

  //8888能匹配8888，null，大于等于Android O 能匹配RGBA_F16
  static {
    Bitmap.Config[] result =
        new Bitmap.Config[] {
          Bitmap.Config.ARGB_8888,
          // The value returned by Bitmaps with the hidden Bitmap config.
          null,
        };
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      result = Arrays.copyOf(result, result.length + 1);
      result[result.length - 1] = Config.RGBA_F16;
    }
    ARGB_8888_IN_CONFIGS = result;
    //RGBA_F16_IN_CONFIGS和ARGB_8888_IN_CONFIGS一样
    private static final Bitmap.Config[] RGBA_F16_IN_CONFIGS = ARGB_8888_IN_CONFIGS;

  // We probably could allow ARGB_4444 and RGB_565 to decode into each other, but ARGB_4444 is
  // deprecated and we'd rather be safe.
  private static final Bitmap.Config[] RGB_565_IN_CONFIGS =
      new Bitmap.Config[] {Bitmap.Config.RGB_565};
  private static final Bitmap.Config[] ARGB_4444_IN_CONFIGS =
      new Bitmap.Config[] {Bitmap.Config.ARGB_4444};
  private static final Bitmap.Config[] ALPHA_8_IN_CONFIGS =
      new Bitmap.Config[] {Bitmap.Config.ALPHA_8};
  }
```
getBestKey()主要是通过getInConfigs()拿到能匹配到的sizesForPossibleConfig，通过辅助集合NavigableMap拿到size相近的possibleSize；
能匹配的第一个条件是possibleSize要小于等于size * MAX_SIZE_MULTIPLE,MAX_SIZE_MULTIPLE默认是8；如果大于8对内存的利用率很低，
  没有必要强制匹配缓存； //意思是服用后，新bitmap占用复用的比例低，空间复用率也低
如果sizesForPossibleConfig和possibleSize有一个不和目标相等，就可以复用，否则说明两者的key肯定相等(参考Key.equals()方法)，
  两者相等没有必须再进行经纬度的匹配，直接返回就行；

在回头看看这个NavigableMap，通过调用getSizesForConfig()得到一个TreeMap，这个Map保存了每个缓存的Bitmap的size和相同size的count，
在getBestKey()方法中调用ceilingKey(size)方法，TreeMap默认会对key进行自然排序，ceilingKey(size)函数的意义是返回一个和size最接近的不小于size的key，
正好符合内存复用的价值；

疑问：为啥要用Map来保存size和该size对应的count，count有何用？
SizeConfigStrategy中有这么一个方法：decrementBitmapOfSize()；
该方法调用时机是当Bitmap从是缓存池中取出或者移除时，执行内容：操作该map，被移除的Bitmap对应的size减1或者把当前key移除，
 只有移除掉，在getBestKey()调用ceilingKey(size)时才知道该size在缓存中是否存在；  
  //意思是移除后这个size,count 就获取不到了

BitmapPool缓存大小的计算
首先，BitmapPool相对Glide对象是单例，在GlideBuilder.build()中创建，构造方法中需要传maxSize，maxSize的计算规则是
从MemorySizeCalculator.getBitmapPoolSize()获得；
GlideBuilder.java
```
if (bitmapPool == null) {
      int size = memorySizeCalculator.getBitmapPoolSize();
      if (size > 0) {
        bitmapPool = new LruBitmapPool(size);
      } else {
        bitmapPool = new BitmapPoolAdapter();
      }
    }
```
通过memorySizeCalculator获取size, 如果size等于0时，创建BitmapPoolAdapter，否则创建LruBitmapPool，什么时候情况下size等于0？
  我们还是看一些memorySizeCalculator的定义；
```
//determine cache sizes for a given device based on some constants and the devices screen density, width, and height
public final class MemorySizeCalculator {
  static final int BYTES_PER_ARGB_8888_PIXEL = 4;
  private static final int LOW_MEMORY_BYTE_ARRAY_POOL_DIVISOR = 2;
  
  MemorySizeCalculator(MemorySizeCalculator.Builder builder) {
    this.context = builder.context;
    //得到arrayPoolSize
    //低内存设备初始化除以2
    arrayPoolSize =
        isLowMemoryDevice(builder.activityManager)
            ? builder.arrayPoolSizeBytes / LOW_MEMORY_BYTE_ARRAY_POOL_DIVISOR
            : builder.arrayPoolSizeBytes;
    //最大总共内存缓存size        
    int maxSize =
        getMaxSize(
            builder.activityManager, builder.maxSizeMultiplier, builder.lowMemoryMaxSizeMultiplier);
    //屏幕宽度,高度  最终通过displayMetrics.widthPixels，displayMetrics.heightPixels获取
    int widthPixels = builder.screenDimensions.getWidthPixels();
    int heightPixels = builder.screenDimensions.getHeightPixels();
    //屏幕像素数，一个像素按照4字节算
    int screenSize = widthPixels * heightPixels * BYTES_PER_ARGB_8888_PIXEL;
    //目标bitmap池缓存Size
    int targetBitmapPoolSize = Math.round(screenSize * builder.bitmapPoolScreens);
    //目标内存缓存size
    int targetMemoryCacheSize = Math.round(screenSize * builder.memoryCacheScreens);
    //可用内存size
    int availableSize = maxSize - arrayPoolSize;
    //如果算出来的size相加小于可用内存，直接赋值
    if (targetMemoryCacheSize + targetBitmapPoolSize <= availableSize) {
      memoryCacheSize = targetMemoryCacheSize;
      bitmapPoolSize = targetBitmapPoolSize;
    } else {
      //按比例重新分配memoryCacheSize和bitmapPoolSize
      float part = availableSize / (builder.bitmapPoolScreens + builder.memoryCacheScreens);
      memoryCacheSize = Math.round(part * builder.memoryCacheScreens);
      bitmapPoolSize = Math.round(part * builder.bitmapPoolScreens);
    }
   ...
  }
  
  //4.4以下是低内存设备，以上根据activityManager.isLowRamDevice()判断 
  static boolean isLowMemoryDevice(ActivityManager activityManager) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      return activityManager.isLowRamDevice();
    } else {
      return true;
    }
  }
  
  private static int getMaxSize(
      ActivityManager activityManager, float maxSizeMultiplier, float lowMemoryMaxSizeMultiplier) {
    final int memoryClassBytes = activityManager.getMemoryClass() * 1024 * 1024;
    final boolean isLowMemoryDevice = isLowMemoryDevice(activityManager);
    return Math.round(
        memoryClassBytes * (isLowMemoryDevice ? lowMemoryMaxSizeMultiplier : maxSizeMultiplier));
  }
}
```
看一下MemorySizeCalculator.Builder
```
static final int MEMORY_CACHE_TARGET_SCREENS = 2;

    //在AndroidO+上，我们对所有大小合理的图像使用{@link Android.graphics.Bitmap.Config#HARDWARE}，除非我们是第一次创建缩略图。
    //因此，位图池在O上的重要性比在以前的版本上要小得多
    static final int BITMAP_POOL_TARGET_SCREENS =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O ? 4 : 1;

    static final float MAX_SIZE_MULTIPLIER = 0.4f;
    static final float LOW_MEMORY_MAX_SIZE_MULTIPLIER = 0.33f;
    // 4MB.
    static final int ARRAY_POOL_SIZE_BYTES = 4 * 1024 * 1024;
    
  public Builder(Context context) {
      this.context = context;
      activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
      screenDimensions =
          new DisplayMetricsScreenDimensions(context.getResources().getDisplayMetrics());

      //在Android O+上，位图是native分配的，ART在管理垃圾方面效率更高，而且我们严重依赖硬件位图，使得位图重用变得不那么重要。
     //我们更倾向在这些设备上保留RAM，并在加载非常小的图像或生成缩略图时不复用位图和纹理，从而降低性能。
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isLowMemoryDevice(activityManager)) {
        bitmapPoolScreens = 0;
      }
    }    
```
bitmapPoolScreens的值在这三种情况：
1 如果设备小于Android O，取值4;
2 如果设备大于等于Android O，低内存手机，取值0;
3 如果设备大于等于Android O，非低内存手机，取值1;
随着Android各个版本对Bitmap的不断进化，Glide也在不断的适应新的特性，高版本对Bitmap的复用也在不断地放松，或许有一天，
  我们不再为Bitmap内存问题所困扰，是不是就可以放弃这令人头大的Pool;

BitmapPool的使用
  主要用于BitmapResource获取bitmap
  DownSampler复用bitmap




ArrayPool  用于数组的池化与复用
ArrayPool.java
```
public interface ArrayPool {
 //默认大小64KB
  int STANDARD_BUFFER_SIZE_BYTES = 64 * 1024;

  <T> void put(T array);

  <T> T get(int size, Class<T> arrayClass);

  <T> T getExact(int size, Class<T> arrayClass);

  void clearMemory();

  void trimMemory(int level);
}
```
实现是LruArrayPool   LruArrayPool的实现与SizeConfigStrategy类似
Strategy  [ˈstrætədʒi] 策略;计策;行动计划;策划;规划;部署;统筹安排;战略;战略部署
```
public final class LruArrayPool implements ArrayPool {
  // 4MB.
  private static final int DEFAULT_SIZE = 4 * 1024 * 1024;

  static final int MAX_OVER_SIZE_MULTIPLE = 8;
  /** Used to calculate the maximum % of the total pool size a single byte array may consume. */
  private static final int SINGLE_ARRAY_MAX_SIZE_DIVISOR = 2;
  //使用GroupedLinkedMap 完成的lru策略
  private final GroupedLinkedMap<Key, Object> groupedMap = new GroupedLinkedMap<>();
  private final KeyPool keyPool = new KeyPool();
  private final Map<Class<?>, NavigableMap<Integer, Integer>> sortedSizes = new HashMap<>();
  //对ArrayAdapterInterface进行缓存
  private final Map<Class<?>, ArrayAdapterInterface<?>> adapters = new HashMap<>();
  private final int maxSize;
  private int currentSize;

  @VisibleForTesting
  public LruArrayPool() {
    maxSize = DEFAULT_SIZE;
  }

  public LruArrayPool(int maxSize) {
    this.maxSize = maxSize;
  }

  //缓存array
  @Override
  public synchronized <T> void put(T array) {
    //class类型
    Class<T> arrayClass = (Class<T>) array.getClass();

    ArrayAdapterInterface<T> arrayAdapter = getAdapterFromType(arrayClass);
    int size = arrayAdapter.getArrayLength(array);
    int arrayBytes = size * arrayAdapter.getElementSizeInBytes();
    //如果array太大就不进行复用了
    if (!isSmallEnoughForReuse(arrayBytes)) {
      return;
    }
    //获取可复用的key
    Key key = keyPool.get(size, arrayClass);

    groupedMap.put(key, array);
    NavigableMap<Integer, Integer> sizes = getSizesForAdapter(arrayClass);
    Integer current = sizes.get(key.size);
    sizes.put(key.size, current == null ? 1 : current + 1);
    currentSize += arrayBytes;
    //校验当前pool大小是否超出
    evict();
  }

  @Override
  public synchronized <T> T getExact(int size, Class<T> arrayClass) {
    Key key = keyPool.get(size, arrayClass);
    return getForKey(key, arrayClass);
  }

  @Override
  public synchronized <T> T get(int size, Class<T> arrayClass) {
    Integer possibleSize = getSizesForAdapter(arrayClass).ceilingKey(size);
    final Key key;
    if (mayFillRequest(size, possibleSize)) {
      key = keyPool.get(possibleSize, arrayClass);
    } else {
      key = keyPool.get(size, arrayClass);
    }
    return getForKey(key, arrayClass);
  }

  private <T> T getForKey(Key key, Class<T> arrayClass) {
    ArrayAdapterInterface<T> arrayAdapter = getAdapterFromType(arrayClass);
    //从groupedMap取出array
    T result = getArrayForKey(key);
    if (result != null) {
      currentSize -= arrayAdapter.getArrayLength(result) * arrayAdapter.getElementSizeInBytes();
      decrementArrayOfSize(arrayAdapter.getArrayLength(result), arrayClass);
    }

    if (result == null) {
      ...
      //新建一个size大小的array
      result = arrayAdapter.newArray(key.size);
    }
    return result;
  }

  // Our cast is safe because the Key is based on the type.
  @Nullable
  private <T> T getArrayForKey(Key key) {
    return (T) groupedMap.get(key);
  }

  private boolean isSmallEnoughForReuse(int byteSize) {
    return byteSize <= maxSize / SINGLE_ARRAY_MAX_SIZE_DIVISOR;
  }

  private boolean mayFillRequest(int requestedSize, Integer actualSize) {
    return actualSize != null
        && (isNoMoreThanHalfFull() || actualSize <= (MAX_OVER_SIZE_MULTIPLE * requestedSize));
  }

  private boolean isNoMoreThanHalfFull() {
    return currentSize == 0 || (maxSize / currentSize >= 2);
  }

  @Override
  public synchronized void clearMemory() {
    evictToSize(0);
  }

  @Override
  public synchronized void trimMemory(int level) {
    if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
      clearMemory();
    } else if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
        || level == android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
      evictToSize(maxSize / 2);
    }
  }

  private void evict() {
    evictToSize(maxSize);
  }

  private void evictToSize(int size) {
    while (currentSize > size) {
      Object evicted = groupedMap.removeLast();
      Preconditions.checkNotNull(evicted);
      ArrayAdapterInterface<Object> arrayAdapter = getAdapterFromObject(evicted);
      currentSize -= arrayAdapter.getArrayLength(evicted) * arrayAdapter.getElementSizeInBytes();
      decrementArrayOfSize(arrayAdapter.getArrayLength(evicted), evicted.getClass());
      ...
    }
  }

  private void decrementArrayOfSize(int size, Class<?> arrayClass) {
    NavigableMap<Integer, Integer> sizes = getSizesForAdapter(arrayClass);
    Integer current = sizes.get(size);
    if (current == null) {
      throw new NullPointerException(
          "Tried to decrement empty size" + ", size: " + size + ", this: " + this);
    }
    if (current == 1) {
      sizes.remove(size);
    } else {
      sizes.put(size, current - 1);
    }
  }

  private NavigableMap<Integer, Integer> getSizesForAdapter(Class<?> arrayClass) {
    NavigableMap<Integer, Integer> sizes = sortedSizes.get(arrayClass);
    if (sizes == null) {
      sizes = new TreeMap<>();
      sortedSizes.put(arrayClass, sizes);
    }
    return sizes;
  }

  @SuppressWarnings("unchecked")
  private <T> ArrayAdapterInterface<T> getAdapterFromObject(T object) {
    return (ArrayAdapterInterface<T>) getAdapterFromType(object.getClass());
  }

  //根据数组类型获取ArrayAdapterInterface
  private <T> ArrayAdapterInterface<T> getAdapterFromType(Class<T> arrayPoolClass) {
    ArrayAdapterInterface<?> adapter = adapters.get(arrayPoolClass);
    if (adapter == null) {
      if (arrayPoolClass.equals(int[].class)) {
        adapter = new IntegerArrayAdapter();
      } else if (arrayPoolClass.equals(byte[].class)) {
        adapter = new ByteArrayAdapter();
      } else {
        throw new IllegalArgumentException(
            "No array pool found for: " + arrayPoolClass.getSimpleName());
      }
      //缓存ArrayAdapterInterface，这样只初始化一次
      adapters.put(arrayPoolClass, adapter);
    }
    return (ArrayAdapterInterface<T>) adapter;
  }

  // VisibleForTesting
  int getCurrentSize() {
    int currentSize = 0;
    for (Class<?> type : sortedSizes.keySet()) {
      for (Integer size : sortedSizes.get(type).keySet()) {
        ArrayAdapterInterface<?> adapter = getAdapterFromType(type);
        currentSize += size * sortedSizes.get(type).get(size) * adapter.getElementSizeInBytes();
      }
    }
    return currentSize;
  }

  //可复用的key与其KeyPool
  private static final class KeyPool extends BaseKeyPool<Key> {
    Key get(int size, Class<?> arrayClass) {
      Key result = get();
      result.init(size, arrayClass);
      return result;
    }

    @Override
    protected Key create() {
      return new Key(this);
    }
  }

  private static final class Key implements Poolable {
    private final KeyPool pool;
    @Synthetic int size;
    private Class<?> arrayClass;

    Key(KeyPool pool) {
      this.pool = pool;
    }

    void init(int length, Class<?> arrayClass) {
      this.size = length;
      this.arrayClass = arrayClass;
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof Key) {
        Key other = (Key) o;
        return size == other.size && arrayClass == other.arrayClass;
      }
      return false;
    }

    
    @Override
    public void offer() {
      pool.offer(this);
    }
  }
}
```
ArrayAdapterInterface是对基本类型数组的包装，此时数组可能是byte[], int[]
```
interface ArrayAdapterInterface<T> {

  String getTag();

  int getArrayLength(T array);

  //新建一个指定大小的数组
  T newArray(int length);

  int getElementSizeInBytes();
}
```
看一下IntegerArrayAdapter
```
public final class IntegerArrayAdapter implements ArrayAdapterInterface<int[]> {
  private static final String TAG = "IntegerArrayPool";

  @Override
  public String getTag() {
    return TAG;
  }

  @Override
  public int getArrayLength(int[] array) {
    return array.length;
  }

  @Override
  public int[] newArray(int length) {
    return new int[length];
  }

  //一个int类型占4字节
  @Override
  public int getElementSizeInBytes() {
    return 4;
  }
}
```
ArrayPool的使用
1. Downsampler.java 对bitmapOptions.inTempStorage的池化复用
2.  BufferedOutputStream.java RecyclableBufferedInputStream.java 对于buffer的池化复用   
3.  StreamEncoder.java 对于buffer的池化复用
```
 public boolean encode(@NonNull InputStream data, @NonNull File file, @NonNull Options options) {
    byte[] buffer = byteArrayPool.get(ArrayPool.STANDARD_BUFFER_SIZE_BYTES, byte[].class);
    boolean success = false;
    OutputStream os = null;
    try {
      os = new FileOutputStream(file);
      int read;
      while ((read = data.read(buffer)) != -1) {
        os.write(buffer, 0, read);
      }
      os.close();
      success = true;
    } catch (IOException e) {
      ...
    } finally {
      ...
      byteArrayPool.put(buffer);
    }
    return success;
  }
```


//todo Pools.Pool<DecodeJob<?>> pool    Engine.java   Pools.Pool<EngineJob<?>> pool
Poolable.java
FactoryPools.java  里面也有poolable接口