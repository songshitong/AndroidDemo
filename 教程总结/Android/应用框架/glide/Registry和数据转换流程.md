
https://juejin.cn/post/6844903758078410765

Registry是Glide中非常重要的知识，可以把它理解成连结各个核心功能模块的集中营或者挂载中心
本章要讨论的内容：
Registry的基本构成；
各个模块的功能和介绍；
数据的转换流程；

从Registry开始
Registry是一个组件管理类，它的主要用途是扩展和替换Glide组件，这些组件包括加载，编码，解码等逻辑；Registry内部支持的模块类型如下:
Registry.java
```
private final ModelLoaderRegistry modelLoaderRegistry;
private final EncoderRegistry encoderRegistry;
private final ResourceDecoderRegistry decoderRegistry;
private final ResourceEncoderRegistry resourceEncoderRegistry;
private final DataRewinderRegistry dataRewinderRegistry;
private final TranscoderRegistry transcoderRegistry;//Resource转换模块注册
private final ImageHeaderParserRegistry imageHeaderParserRegistry;//文件头解析模块注册
```

Registry并不是承当所有模块的注册工作，而是把各个模块分配的不同的Registry当中；
主要模块的功能：
ModelLoaderRegistry ：//数据加载模块注册
EncoderRegistry：//所有对数据进行编码模块的注册
ResourceDecoderRegistry：//处理过的解码模块注册
ResourceEncoderRegistry：//处理过的编码模块注册
DataRewinderRegistry ： //数据流重置起点模块注册
TranscoderRegistry： // Resource进行转换模块注册
ImageHeaderParserRegistry //图片头解析模块注册


Glide自身充当对外调用的门户，Registry提供了一下入口方法来实现各个模块的注册和调用；主要方法如下：
注册相关方法：
append() //尾步追加
prepend() //头部插入
register() //注册，相当于append() 
replace() //替换掉相同条件的所有模块
```
 private final ModelLoaderRegistry modelLoaderRegistry;
  private final EncoderRegistry encoderRegistry;
  private final ResourceDecoderRegistry decoderRegistry;
  private final ResourceEncoderRegistry resourceEncoderRegistry;
  private final DataRewinderRegistry dataRewinderRegistry;
  private final TranscoderRegistry transcoderRegistry;
  private final ImageHeaderParserRegistry imageHeaderParserRegistry;
  
 public <Data> Registry append(@NonNull Class<Data> dataClass, @NonNull Encoder<Data> encoder) {
    encoderRegistry.append(dataClass, encoder);
    return this;
  }

  public <Data, TResource> Registry append(
      @NonNull String bucket,
      @NonNull Class<Data> dataClass,
      @NonNull Class<TResource> resourceClass,
      @NonNull ResourceDecoder<Data, TResource> decoder) {
    decoderRegistry.append(bucket, decoder, dataClass, resourceClass);
    return this;
  }  
  
 public <TResource> Registry append(
      @NonNull Class<TResource> resourceClass, @NonNull ResourceEncoder<TResource> encoder) {
    resourceEncoderRegistry.append(resourceClass, encoder);
    return this;
  }
  
  public <Data> Registry prepend(@NonNull Class<Data> dataClass, @NonNull Encoder<Data> encoder) {
    encoderRegistry.prepend(dataClass, encoder);
    return this;
  }
  
 public <Data, TResource> Registry prepend(
      @NonNull String bucket,
      @NonNull Class<Data> dataClass,
      @NonNull Class<TResource> resourceClass,
      @NonNull ResourceDecoder<Data, TResource> decoder) {
    decoderRegistry.prepend(bucket, decoder, dataClass, resourceClass);
    return this;
  }
  
  public Registry register(@NonNull DataRewinder.Factory<?> factory) {
    dataRewinderRegistry.register(factory);
    return this;
  }
  
  public <Model, Data> Registry replace(
      @NonNull Class<Model> modelClass,
      @NonNull Class<Data> dataClass,
      @NonNull ModelLoaderFactory<? extends Model, ? extends Data> factory) {
    modelLoaderRegistry.replace(modelClass, dataClass, factory);
    return this;
  }
  ...
```

操作相关的方法：
getLoadPath()//获取加载路径
getDecodePaths() //获取解析路径
getRegisteredResourceClasses()//获取所有匹配的ResourceClasses;
isResourceEncoderAvailable()//ResourceEncoder是否可用；
getResultEncoder()//获取Encoder;
getRewinder()//获取Rewinder;
getModelLoaders()//获取ModelLoader；
```
public <Data, TResource, Transcode> LoadPath<Data, TResource, Transcode> getLoadPath(
      @NonNull Class<Data> dataClass,
      @NonNull Class<TResource> resourceClass,
      @NonNull Class<Transcode> transcodeClass) {
      
      }
private <Data, TResource, Transcode> List<DecodePath<Data, TResource, Transcode>> getDecodePaths(
      @NonNull Class<Data> dataClass,
      @NonNull Class<TResource> resourceClass,
      @NonNull Class<Transcode> transcodeClass) {
      }   
...         
```
总结：Registry通过内部Registry分别管理不同类型的组件，Registry提供统一的入口方法来实现注册和获取；
getLoadPath()等方法大部分在DecodeHelper中调用,DecodeHelper在DecodeJob.java中初始化
DecodeHelper.java
```
final class DecodeHelper<Transcode> {
  <Data> LoadPath<Data, ?, Transcode> getLoadPath(Class<Data> dataClass) {
    return glideContext.getRegistry().getLoadPath(dataClass, resourceClass, transcodeClass);
  }
}
```


下面对各个模块基本介绍：
ModelLoader
ModelLoader是通过ModelLoaderRegistry进行管理，ModelLoader需要接受两个泛型类型<Model,Data>，ModelLoader本身是一个工厂接口，
主要工作是将复杂数据模型转通过DataFetcher转换成需要的Data，LoadData是ModelLoader的内部类，是对DataFetcher和Key的封装实体，
ModelLoader的创建用ModelLoaderFactory
这个是用于生成用于读取的数据，比如说String转化为InputStream等。而且妙的一点那是，每个生成工厂之间可以嵌套，最后会一层层解析获取到一些参数，
  进入到最为基础的数据模型。  https://www.jianshu.com/p/4de87ebf5104
参考HttpGlideUrlLoader,用来将GlideUrl转换为InputStream
ModelLoaderRegistry.java
```
public class ModelLoaderRegistry {
  private final MultiModelLoaderFactory multiModelLoaderFactory;
  private final ModelLoaderCache cache = new ModelLoaderCache();
  private static class ModelLoaderCache {
    //使用HashMap存储class与 ModelLoader
    private final Map<Class<?>, Entry<?>> cachedModelLoaders = new HashMap<>();

    //存储多个ModelLoader
    private static class Entry<Model> {
      @Synthetic final List<ModelLoader<Model, ?>> loaders;    
    }
  }
}
```
ModelLoader.java
```
public interface ModelLoader<Model, Data> {

  class LoadData<Data> {
    public final Key sourceKey;
    public final List<Key> alternateKeys;
    public final DataFetcher<Data> fetcher;
    ..//构造器
  }

  //构建LoadData
  LoadData<Data> buildLoadData(
      @NonNull Model model, int width, int height, @NonNull Options options);

  boolean handles(@NonNull Model model);
}
```
DataFetcher数据获取的接口  
它的实现很多，例如HttpUrlFetcher，使用HttpURLConnection获取数据
   OkHttpStreamFetcher，使用OkHttp3请求网络
   VolleyStreamFetcher，使用Volley请求网络
```
public interface DataFetcher<T> {

  //回调
  interface DataCallback<T> {
    void onDataReady(@Nullable T data);
    void onLoadFailed(@NonNull Exception e);
  }
  
  void loadData(@NonNull Priority priority, @NonNull DataCallback<? super T> callback);

  void cleanup();
  void cancel();
  Class<T> getDataClass();
  DataSource getDataSource();
}
```
key与option
Key.java
一种接口，它唯一地标识数据的某个put，需要重写equals和hashcode
```
public interface Key {
  String STRING_CHARSET_NAME = "UTF-8";
  Charset CHARSET = Charset.forName(STRING_CHARSET_NAME);

  void updateDiskCacheKey(@NonNull MessageDigest messageDigest);

  @Override
  boolean equals(Object o);

  @Override
  int hashCode();
}
```
Options.java 应用在memory and disk cache keys
```
public final class Options implements Key {
  //将option与其对应的value存储在ArrayMap
  private final ArrayMap<Option<?>, Object> values = new CachedHashCodeArrayMap<>();
  }
```
看一下Option.java   一个option需要字符串key,value
```
public final class Option<T> {
  private static final CacheKeyUpdater<Object> EMPTY_UPDATER =
      new CacheKeyUpdater<Object>() {
        @Override
        public void update(
            @NonNull byte[] keyBytes, @NonNull Object value, @NonNull MessageDigest messageDigest) {
          // Do nothing.
        }
      };

  private final T defaultValue;
  private final CacheKeyUpdater<T> cacheKeyUpdater;
  private final String key;
  private volatile byte[] keyBytes;

  @NonNull
  public static <T> Option<T> memory(@NonNull String key) {
    return new Option<>(key, null, Option.<T>emptyUpdater());
  }

  @NonNull
  public static <T> Option<T> memory(@NonNull String key, @NonNull T defaultValue) {
    return new Option<>(key, defaultValue, Option.<T>emptyUpdater());
  }

 =
  @NonNull
  public static <T> Option<T> disk(
      @NonNull String key, @NonNull CacheKeyUpdater<T> cacheKeyUpdater) {
    return new Option<>(key, null, cacheKeyUpdater);
  }
  
  @NonNull
  public static <T> Option<T> disk(
      @NonNull String key, @Nullable T defaultValue, @NonNull CacheKeyUpdater<T> cacheKeyUpdater) {
    return new Option<>(key, defaultValue, cacheKeyUpdater);
  }

  private Option(
      @NonNull String key, @Nullable T defaultValue, @NonNull CacheKeyUpdater<T> cacheKeyUpdater) {
    this.key = Preconditions.checkNotEmpty(key);
    this.defaultValue = defaultValue;
    this.cacheKeyUpdater = Preconditions.checkNotNull(cacheKeyUpdater);
  }

  @SuppressWarnings("WeakerAccess")
  @Nullable
  public T getDefaultValue() {
    return defaultValue;
  }
  //更新
  public void update(@NonNull T value, @NonNull MessageDigest messageDigest) {
    cacheKeyUpdater.update(getKeyBytes(), value, messageDigest);
  }

  @NonNull
  private byte[] getKeyBytes() {
    if (keyBytes == null) {
      keyBytes = key.getBytes(Key.CHARSET);
    }
    return keyBytes;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof Option) {
      Option<?> other = (Option<?>) o;
      return key.equals(other.key);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }

  private static <T> CacheKeyUpdater<T> emptyUpdater() {
    return (CacheKeyUpdater<T>) EMPTY_UPDATER;
  }

  public interface CacheKeyUpdater<T> {
    void update(@NonNull byte[] keyBytes, @NonNull T value, @NonNull MessageDigest messageDigest);
  }
}
```
Options的应用
DecodeJob.java
```
private Options getOptionsWithHardwareConfig(DataSource dataSource) {
    options = new Options();
    options.putAll(this.options);
    //Downsampler.ALLOW_HARDWARE_CONFIG是一个option，isHardwareConfigSafe是他的value
    options.set(Downsampler.ALLOW_HARDWARE_CONFIG, isHardwareConfigSafe);
    return options;
}
```

一个基本的ModelLoader创建应该是这个样子的：
参考HttpGlideUrlLoader,用来将GlideUrl转换为InputStream
第一步：自定义类实现自ModelLoader，重写BuildLoadData()方法和handles()方法；
```
public class HttpGlideUrlLoader implements ModelLoader<GlideUrl, InputStream> {
  public static final Option<Integer> TIMEOUT =
      Option.memory("com.bumptech.glide.load.model.stream.HttpGlideUrlLoader.Timeout", 2500);
  //modelCache初始化在下面的fatory
  private final ModelCache<GlideUrl, GlideUrl> modelCache;
  
    public LoadData<InputStream> buildLoadData(
      @NonNull GlideUrl model, int width, int height, @NonNull Options options) {
    ///GlideUrls将已解析的URL存储起来，因此缓存它们可以节省一些对象实例化和解析URL所花费的时间
    GlideUrl url = model;
    if (modelCache != null) {
      url = modelCache.get(model, 0, 0);
      if (url == null) {
        modelCache.put(model, 0, 0, model);
        url = model;
      }
    }
    int timeout = options.get(TIMEOUT);
    return new LoadData<>(url, new HttpUrlFetcher(url, timeout));
  }
  public boolean handles(@NonNull GlideUrl model) {
    return true;
  }
}
```
buildLoadData()需要创建LoadData,需要传入Key和DataFetcher，handles()返回值代表是否接受当前model类型的，true代表接受，
  所以一般都是true;
第二步：自定义Fetcher实现DataFetcher，重写loadData()、cleanup()、cancel()、getDataClass()、getDataSource()方法；
HttpUrlFetcher.java
```
public class HttpUrlFetcher implements DataFetcher<InputStream> {
   public void loadData(
      @NonNull Priority priority, @NonNull DataCallback<? super InputStream> callback) {
    long startTime = LogTime.getLogTime();
    try {
      //使用HttpUrlConnection获取流
      InputStream result = loadDataWithRedirects(glideUrl.toURL(), 0, null, glideUrl.getHeaders());
      callback.onDataReady(result);
    } catch (IOException e) {
      ..
      callback.onLoadFailed(e);
    } finally {
      ...
    }
  }
 //清理工作 
 public void cleanup() {
    if (stream != null) {
      try {
        stream.close();
      } catch (IOException e) {
        // Ignore
      }
    }
    if (urlConnection != null) {
      urlConnection.disconnect();
    }
    urlConnection = null;
  }

  @Override
  public void cancel() {
    isCancelled = true;
  }

  public Class<InputStream> getDataClass() {
    return InputStream.class;
  }

  public DataSource getDataSource() {
    return DataSource.REMOTE;
  }
}
```
第三步:创建Factory类，重写build()、teardown()方法，在build()方法中返回真正的MolderLoader对象；
HttpGlideUrlLoader.java
```
public static class Factory implements ModelLoaderFactory<GlideUrl, InputStream> {
    private final ModelCache<GlideUrl, GlideUrl> modelCache = new ModelCache<>(500);
    @Override
    public ModelLoader<GlideUrl, InputStream> build(MultiModelLoaderFactory multiFactory) {
      return new HttpGlideUrlLoader(modelCache);
    }
     @Override
    public void teardown() {
      // Do nothing.
    }
  }
```
接下来，就可以在Registry中注册ModelLoader了；在第一章我们简单说过模块的配置可以用Annotation和Manifest两种类型，
在registerComponents()方法中，可以拿到Registry，这样就可以调用registry的注册相关方法；
```
@Override
  public void registerComponents(Context context, Registry registry) {
    registry.replace(GlideUrl.class, InputStream.class, new HttpGlideUrlLoader.Factory(context));
  }
```
最后，说一下泛型<Model,Data>接受的范围，Model代表用户输入的类型，理论上可以用任意数据类型，主要的输入点在Glide.with().load(model)；
Data理论上也可以是任意数据类型，但基于后续流程的支持，一般都是File,InputStream和ByteBuffer;



ResourceDecoderRegistry.java
```
public class ResourceDecoderRegistry {
  private final List<String> bucketPriorityList = new ArrayList<>();
  //存在map
  private final Map<String, List<Entry<?, ?>>> decoders = new HashMap<>();
  //一种资源对应一种解码器
  private static class Entry<T, R> {
    private final Class<T> dataClass;
    @Synthetic final Class<R> resourceClass;
    @Synthetic final ResourceDecoder<T, R> decoder;

    public boolean handles(@NonNull Class<?> dataClass, @NonNull Class<?> resourceClass) {
      return this.dataClass.isAssignableFrom(dataClass)
          && resourceClass.isAssignableFrom(this.resourceClass);
    }
  }
}
```
ResourceDecoder
ResourceDecoder是一个解析Resource的接口，接受两个泛型<T,Z>，定义了两个方法handles()和decode 将T解码为Z
```
public interface ResourceDecoder<T, Z> {
  boolean handles(@NonNull T source, @NonNull Options options) throws IOException;

  Resource<Z> decode(@NonNull T source, int width, int height, @NonNull Options options)
      throws IOException;
}
```
Resource.java 包装特定的类型使其可以被池化和复用
```
public interface Resource<Z> {

  @NonNull
  Class<Z> getResourceClass();

  @NonNull
  Z get();

  int getSize();

  void recycle();
}
```
看一下BitmapResource的实现  对bitmap在BitmapPool的复用，回收进行了包装
```
public class BitmapResource implements Resource<Bitmap>, Initializable {
  private final Bitmap bitmap;
  private final BitmapPool bitmapPool;
  
  public BitmapResource(@NonNull Bitmap bitmap, @NonNull BitmapPool bitmapPool) {
    this.bitmap = Preconditions.checkNotNull(bitmap, "Bitmap must not be null");
    this.bitmapPool = Preconditions.checkNotNull(bitmapPool, "BitmapPool must not be null");
  }
    public Class<Bitmap> getResourceClass() {
    return Bitmap.class;
  }

  public Bitmap get() {
    return bitmap;
  }

  public int getSize() {
    return Util.getBitmapByteSize(bitmap);
  }
  //资源回收进入bitmapPool
  public void recycle() {
    bitmapPool.put(bitmap);
  }

  //todo prepareToDraw
  public void initialize() {
    bitmap.prepareToDraw();
  }
}
```
参考一个简单的StreamBitmapDecoder:  将InputStream解码为Bitmap
```
public class StreamBitmapDecoder implements ResourceDecoder<InputStream, Bitmap> {
  public boolean handles(@NonNull InputStream source, @NonNull Options options) {
    return downsampler.handles(source);
  }
  
  public Resource<Bitmap> decode(
      @NonNull InputStream source, int width, int height, @NonNull Options options)
      throws IOException {
    。。。
    try {
      return downsampler.decode(invalidatingStream, width, height, options, callbacks);
    } finally {
      。。。
    }
  }
}
```
泛型解析：T表示输入类型一般为File，InputStream或者ByteBuffer，Z表示输出类型，一般为Bitmap和Drawable


ResourceEncoderRegistry与ResourceEncoderRegistry和其他Registry类似，就不分析了
Encoder和ResourceEncoder
Encoder表面意思为加密，本质上和加密没有关系，主要作用是将T持久化到本地cache；Encode接受泛型T,而这个T可以是InputStream、ByteBuffer、Resource<T>，
ResourceEncoder继承Encoder<Resource<T>>，接受泛型T，而Resource中的T一般取值范围为:Bitmap、BitmapDrawable、GifDrawable;
```
//Encoder.java
public interface Encoder<T> {

  boolean encode(@NonNull T data, @NonNull File file, @NonNull Options options);
}

//ResourceEncoder  将Resource持久化的类
public interface ResourceEncoder<T> extends Encoder<Resource<T>> {

  EncodeStrategy getEncodeStrategy(@NonNull Options options);
}
```

我们分析一下把Bitmap持久化到本地cache的类:BitmapEncoder
```
public class BitmapEncoder implements ResourceEncoder<Bitmap> {
  public boolean encode(
      @NonNull Resource<Bitmap> resource, @NonNull File file, @NonNull Options options) {
    final Bitmap bitmap = resource.get();
    Bitmap.CompressFormat format = getFormat(bitmap, options);
    。。。
    try {
      long start = LogTime.getLogTime();
      int quality = options.get(COMPRESSION_QUALITY);

      boolean success = false;
      OutputStream os = null;
      try {
        os = new FileOutputStream(file);
        if (arrayPool != null) {
          os = new BufferedOutputStream(os, arrayPool);
        }
        bitmap.compress(format, quality, os);
        os.close();
        success = true;
      } catch (IOException e) {
        。。。
      } finally {
        if (os != null) {
          try {
            os.close();
          } catch (IOException e) {
            // Do nothing.
          }
        }
      }

      。。。。
      return success;
    } finally {
      GlideTrace.endSection();
    }
  }
  
  public EncodeStrategy getEncodeStrategy(@NonNull Options options) {
    return EncodeStrategy.TRANSFORMED;
  }
}

public enum EncodeStrategy {
  //不修改数据直接持久化
  SOURCE,

  /** Writes the decoded, downsampled and transformed data for the resource to disk. */
  TRANSFORMED,

  //不持久化
  NONE,
}
```
BitmapEncoder重写encode()方法，该方法中入参file代表将要保存的cache文件；encode基本流程是：根据File得到输出流OutPutStream，
调用Bitmap.compress将bitmap写入输出流，然后关闭流等等处理；



DataRewinder
DataRewinder是一个接口，作用是将流进行rewinding ，主要的方法是rewindAndGet()；
```
public interface DataRewinder<T> {
  
  //创建DataRewinder的工厂类
  interface Factory<T> {
    DataRewinder<T> build(@NonNull T data);
    Class<T> getDataClass();
  }

  T rewindAndGet() throws IOException;

  void cleanup();
}
```
Rewind  [ˌriːˈwaɪnd]  倒帶;倒退;快退;返回;缠绕

Glide中DataRewinder有意义的实现类:
InputStreamRewinder和ByteBufferRewinder，ParcelFileDescriptorRewinder,简单看一下InputStreamRewinder和ByteBufferRewinder：
ByteBufferRewinder.java
```
public class ByteBufferRewinder implements DataRewinder<ByteBuffer> {
  private final ByteBuffer buffer;

  public ByteBufferRewinder(ByteBuffer buffer) {
    this.buffer = buffer;
  }

  public ByteBuffer rewindAndGet() {
    buffer.position(0);
    return buffer;
  }

  public void cleanup() {
    // Do nothing.
  }
```

InputStreamRewinder.java
```
public final class InputStreamRewinder implements DataRewinder<InputStream> {
  // 5MB.
  private static final int MARK_READ_LIMIT = 5 * 1024 * 1024;

  private final RecyclableBufferedInputStream bufferedStream;

  @Synthetic
  public InputStreamRewinder(InputStream is, ArrayPool byteArrayPool) {
    // We don't check is.markSupported() here because RecyclableBufferedInputStream allows resetting
    // after exceeding MARK_READ_LIMIT, which other InputStreams don't guarantee.
    bufferedStream = new RecyclableBufferedInputStream(is, byteArrayPool);
    bufferedStream.mark(MARK_READ_LIMIT);
  }

  public InputStream rewindAndGet() throws IOException {
    bufferedStream.reset();
    //主要通过RecyclableBufferedInputStream实现倒带操作
    return bufferedStream;
  }

  public void cleanup() {
    bufferedStream.release();
  }
}
```


ResourceTranscoder
ResourceTranscoder是一个接口，作用是对两个Resource<?>进行转换，接受泛型<Z,R>，主要方法是transcode(),
一般泛型的接收范围是Bitmap、Drawable、byte[]等，将Z转换为R
Bitmap->byte[];Bitmap->BitmapDrawable;Drawable->byte[];GifDrawable->byte[]
```
public interface ResourceTranscoder<Z, R> {
  Resource<R> transcode(@NonNull Resource<Z> toTranscode, @NonNull Options options);
}
```
看一下简单的BitmapBytesTranscoder源码：  通过ByteArrayOutputStream将Bitmap转为byte[]
```
public class BitmapBytesTranscoder implements ResourceTranscoder<Bitmap, byte[]> {
  private final Bitmap.CompressFormat compressFormat;
  private final int quality;

  public BitmapBytesTranscoder() {
    this(Bitmap.CompressFormat.JPEG, 100);
  }
  
  public BitmapBytesTranscoder(@NonNull Bitmap.CompressFormat compressFormat, int quality) {
    this.compressFormat = compressFormat;
    this.quality = quality;
  }

  public Resource<byte[]> transcode(
      @NonNull Resource<Bitmap> toTranscode, @NonNull Options options) {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    toTranscode.get().compress(compressFormat, quality, os);
    toTranscode.recycle();
    return new BytesResource(os.toByteArray());
  }
}
```


ImageHeaderParser  解析图片文件头
```
public interface ImageHeaderParser {
  //未知方向
  int UNKNOWN_ORIENTATION = -1;

  enum ImageType {
    GIF(true),
    JPEG(false),
    RAW(false),
    /** PNG type with alpha. */
    PNG_A(true),
    /** PNG type without alpha. */
    PNG(false),
    /** WebP type with alpha. */
    WEBP_A(true),
    /** WebP type without alpha. */
    WEBP(false),
    /** Unrecognized type. */
    UNKNOWN(false);

    private final boolean hasAlpha;

    ImageType(boolean hasAlpha) {
      this.hasAlpha = hasAlpha;
    }

    public boolean hasAlpha() {
      return hasAlpha;
    }
  }

  ImageType getType(@NonNull InputStream is) throws IOException;

  ImageType getType(@NonNull ByteBuffer byteBuffer) throws IOException;

  int getOrientation(@NonNull InputStream is, @NonNull ArrayPool byteArrayPool) throws IOException;

  int getOrientation(@NonNull ByteBuffer byteBuffer, @NonNull ArrayPool byteArrayPool)
      throws IOException;
}
```
默认实现是DefaultImageHeaderParser，在下采样解析时分析了


加载/解析 数据转换流程
上面介绍一堆组件和一堆泛型，数据类型的转换到底是怎样？搞明白这一点还得从Registry提供的操作方法入手：
Registry提供getModelLoaders()和getLoadPath()，我们先从定义方法的泛型来看:
Registry.java
```
public <Model> List<ModelLoader<Model, ?>> getModelLoaders(@NonNull Model model) {...}

public <Data, TResource, Transcode> LoadPath<Data, TResource, Transcode> getLoadPath(
      @NonNull Class<Data> dataClass, @NonNull Class<TResource> resourceClass,
      @NonNull Class<Transcode> transcodeClass) {...}
```
getModelLoader()分析
getModelLoaders()入参类型为<Model>，返回类型为<Model,?>，<Model>具体类型就是我们调用Glide.with().load(model)时load()传入的类型，
  返回类型<?>是我们在Registry中注册的所有符合输入<Model>的类型，比如InputStream或者ByteBuffer；

LoadPath()分析
LoadPath()入参类型为<Data, TResource, Transcode>，其中<Data>是在getModelLoaders()返回的类型，例如InputStream或者ByteBuffer，
<TResource>是待定类型，调用者一般传?,
<Transcode>调用Glide.with().as(xxx)时as()传入的类型，Glide提供有asBitmap(),asFile(),asGif()，默认是Drawable类型；
  在调用时<TResource>是待定类型，肯定有逻辑获取它的目标类型
```
  private static final RequestOptions DECODE_TYPE_BITMAP = decodeTypeOf(Bitmap.class).lock();
  private static final RequestOptions DECODE_TYPE_GIF = decodeTypeOf(GifDrawable.class).lock();
  public RequestBuilder<Bitmap> asBitmap() {
    return as(Bitmap.class).apply(DECODE_TYPE_BITMAP);
  }
  public RequestBuilder<GifDrawable> asGif() {
    return as(GifDrawable.class).apply(DECODE_TYPE_GIF);
  }
  public <ResourceType> RequestBuilder<ResourceType> as(
      @NonNull Class<ResourceType> resourceClass) {
    return new RequestBuilder<>(glide, this, resourceClass, context);
  }
```

下面分析getLoadPath()方法一看究竟； 示例:输入ByteBuffer,Object,Drawable 输出LoadPath<ByteBuffer,Object,Drawable>
```
public <Data, TResource, Transcode> LoadPath<Data, TResource, Transcode> getLoadPath(
      @NonNull Class<Data> dataClass,
      @NonNull Class<TResource> resourceClass,
      @NonNull Class<Transcode> transcodeClass) {
    LoadPath<Data, TResource, Transcode> result =
        loadPathCache.get(dataClass, resourceClass, transcodeClass);
    if (loadPathCache.isEmptyLoadPath(result)) {
      return null;
    } else if (result == null) {
      List<DecodePath<Data, TResource, Transcode>> decodePaths =
          getDecodePaths(dataClass, resourceClass, transcodeClass);
      // It's possible there is no way to decode or transcode to the desired types from a given
      // data class.
      if (decodePaths.isEmpty()) {
        result = null;
      } else {
        result =
            new LoadPath<>(
                dataClass, resourceClass, transcodeClass, decodePaths, throwableListPool);
      }
      loadPathCache.put(dataClass, resourceClass, transcodeClass, result);
    }
    return result;
  }
```
LoadPath()方法从loadPathCache获取缓存对象，如果不存在，调用getDecodePaths()，经过判断，创建LoadPath对象，将获取的结果放入LoadPath，
最后放入loadPathCache并返回，LoadPath是对Data,TResource,Transcode和List<DecodePath<Data, TResource, Transcode>>的封装，
最终的逻辑还是再DecodePath中；
com/bumptech/glide/load/engine/LoadPath.java
```
public class LoadPath<Data, ResourceType, Transcode> {
  private final Class<Data> dataClass;
  private final Pool<List<Throwable>> listPool;
  private final List<? extends DecodePath<Data, ResourceType, Transcode>> decodePaths;
  private final String failureMessage;
 }
```
看一下getDecodePaths()方法定义：
```
 private <Data, TResource, Transcode> List<DecodePath<Data, TResource, Transcode>> getDecodePaths(
      @NonNull Class<Data> dataClass,
      @NonNull Class<TResource> resourceClass,
      @NonNull Class<Transcode> transcodeClass) {
    List<DecodePath<Data, TResource, Transcode>> decodePaths = new ArrayList<>();
    //1 获取所有dataClass对应的ResourceClasses
    List<Class<TResource>> registeredResourceClasses =
        decoderRegistry.getResourceClasses(dataClass, resourceClass);
    //遍历registeredResourceClass
    for (Class<TResource> registeredResourceClass : registeredResourceClasses) {
      //2 获取所有的registeredResourceClass对应的registeredTranscodeClasses
      List<Class<Transcode>> registeredTranscodeClasses =
          transcoderRegistry.getTranscodeClasses(registeredResourceClass, transcodeClass);
      //遍历registeredTranscodeClasses
      for (Class<Transcode> registeredTranscodeClass : registeredTranscodeClasses) {
        //3 获取dataClass和registeredResourceClass对应的所有ResourceDecoder
        List<ResourceDecoder<Data, TResource>> decoders =
            decoderRegistry.getDecoders(dataClass, registeredResourceClass);
        //4 获取registeredResourceClass和registeredTranscodeClasss对应的所有ResourceTranscoder    
        ResourceTranscoder<TResource, Transcode> transcoder =
            transcoderRegistry.get(registeredResourceClass, registeredTranscodeClass);
        //创建DecodePath,把相关信息封装  //将查到的信息封装
        DecodePath<Data, TResource, Transcode> path =
            new DecodePath<>(
                dataClass,
                registeredResourceClass,
                registeredTranscodeClass,
                decoders,
                transcoder,
                throwableListPool);
        //添加进集合        
        decodePaths.add(path);
      }
    }
    return decodePaths;
  }
```
到各个Registry查询注册的类
为了更清楚的分析代码，我可以将假设泛型的类型为<InputStream,?,Drawable>；
我在上面代码中做了标记：代码1通过调用transcoderRegistry.getTranscodeClasses()，返回的类型就是泛型?所未知的具体类型；
代码2通过调用transcoderRegistry.getTranscodeClasses()，返回所有符合条件的registeredTranscodeClasses;
代码3通过调用decoderRegistry.getDecoders()获取符合条件的List<ResourceDecoder>;
代码4通过调用transcoderRegistry.get()获取符合条件的ResourceTranscoder
  
DecodePath是对Data, TResource, Transcode,decoders,transcoder的封装；
```
public class DecodePath<DataType, ResourceType, Transcode> {
  private final Class<DataType> dataClass;
  private final List<? extends ResourceDecoder<DataType, ResourceType>> decoders;
  private final ResourceTranscoder<ResourceType, Transcode> transcoder;
  private final Pool<List<Throwable>> listPool;

  public DecodePath(
      Class<DataType> dataClass,
      Class<ResourceType> resourceClass,
      Class<Transcode> transcodeClass,
      List<? extends ResourceDecoder<DataType, ResourceType>> decoders,
      ResourceTranscoder<ResourceType, Transcode> transcoder,
      Pool<List<Throwable>> listPool) {
    this.dataClass = dataClass;
    this.decoders = decoders;
    this.transcoder = transcoder;
    this.listPool = listPool;
  }
}
```
第一层for循环理解
由于<TResource>是入参是未知类型，并不是用户定义的，是Registry模块支持的中间类型，它是靠入参类型<Data>进行筛选，所以就可能有可能有多个匹配；
第二层for循环理解
因为<Transcode>是用户传入，这个泛型是一个已确定类型，通常是Drawable，但是真正注册给transcoderRegistry可能是BitmapDrawable或则BitmapDrawable类型，
  这一刻还不确定是哪个Drawable，所以在这一步，registry返回给调用者多个;
总结：
加载过程是从getModelLoader()调用，数据从Model->Data;
解析过程是从getLoadPath()调用，中间经过decoder、transcoder，数据类型从Data->TResource->Transcode

写缓存数据转换流程
写缓存过程分为两类，一类是直接将原数据缓存，另一类是将变化后的数据写缓存，他们分别对应的是Encoder和ResourceEncoder;
Encoder流程
Encoder的使用场景在SourceGenerator.cacheData(dataToCache)方法中，最终通过调用Registry.getSourceEncoder()获取到Encode;
```
class SourceGenerator implements DataFetcherGenerator, DataFetcherGenerator.FetcherReadyCallback {
   private void cacheData(Object dataToCache) {
    long startTime = LogTime.getLogTime();
    try {
      //获取Encoder
      Encoder<Object> encoder = helper.getSourceEncoder(dataToCache);
      //将data写入文件
      DataCacheWriter<Object> writer =
          new DataCacheWriter<>(encoder, dataToCache, helper.getOptions());
      originalKey = new DataCacheKey(loadData.sourceKey, helper.getSignature());
      helper.getDiskCache().put(originalKey, writer);
     ...
    } finally {
      loadData.fetcher.cleanup();
    }

    sourceCacheGenerator =
        new DataCacheGenerator(Collections.singletonList(loadData.sourceKey), helper, this);
  }
}
```
上一节说过SourceGenerator是对原数据的获取，cacheData()中拿到的 dataToCache一般是加载过程返回的Data，
确切的说是InputStream或者ByteBuffer类型，而Encoder最终保存到文件，类型为File，所以Encoder数据是从Data->File的；
执行的时机在加载之后，和解析过程并列执行；

ResourceEncoder流程
ResourceEncoder的使用场景是在数据解析完毕后，将处理过的数据进行缓存，调用的地方在DecodeJob.onResourceDecoded()方法中，
  其最终通过调用Registry.getResultEncoder()获取；//缓存为file
DecodeJob.java
```
<Z> Resource<Z> onResourceDecoded(DataSource dataSource, @NonNull Resource<Z> decoded) {
    ...//Resource<Z> decoded 可能是BitmapResource 泛型是Bitmap
    final EncodeStrategy encodeStrategy;
    final ResourceEncoder<Z> encoder;
    if (decodeHelper.isResourceEncoderAvailable(transformed)) {  transformed可能是BitmapResources
      //获取ResourceEncoder    可能是BitmapEncoder
      encoder = decodeHelper.getResultEncoder(transformed);
      encodeStrategy = encoder.getEncodeStrategy(options);
    } else {
      encoder = null;
      encodeStrategy = EncodeStrategy.NONE;
    }
}
```
ResourceEncoder数据类型是从Resource<X>->File；执行的时机在解析流程之后；

glide_数据装换流程图.awebp