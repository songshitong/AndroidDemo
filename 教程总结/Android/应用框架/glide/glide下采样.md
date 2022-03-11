https://juejin.cn/post/6844903813191598087
glide 4.12  最好了解一下bitmap的下采样，复用

下采样策略结构
DownsampleStrategy.java
```
public abstract class DownsampleStrategy {
    //获取缩放因子
  public abstract float getScaleFactor(int sourceWidth, int sourceHeight, int requestedWidth, int requestedHeight);
  
  public abstract SampleSizeRounding getSampleSizeRounding(int sourceWidth, int sourceHeight, int requestedWidth, int requestedHeight);
  public enum SampleSizeRounding {
    //内存优先，采样比较大
    MEMORY,
    //质量优先，采样比较小
    QUALITY,
  }
  // DownsampleStrategy的默认实现  //todo option分析
  public static final Option<DownsampleStrategy> OPTION =
      Option.memory(
          "com.bumptech.glide.load.resource.bitmap.Downsampler.DownsampleStrategy", DEFAULT);
  public static final DownsampleStrategy DEFAULT = CENTER_OUTSIDE; 
  
  public static final DownsampleStrategy CENTER_OUTSIDE = new CenterOutside();
}
```
看一下CenterOutside的实现   其他策略可以通过Glide.fitCenter()或其他实现
```
private static class CenterOutside extends DownsampleStrategy {
    //缩放因子取最大的
    @Override
    public float getScaleFactor(
        int sourceWidth, int sourceHeight, int requestedWidth, int requestedHeight) {
      float widthPercentage = requestedWidth / (float) sourceWidth;
      float heightPercentage = requestedHeight / (float) sourceHeight;
      return Math.max(widthPercentage, heightPercentage);
    }
    //质量优先
    @Override
    public SampleSizeRounding getSampleSizeRounding(
        int sourceWidth, int sourceHeight, int requestedWidth, int requestedHeight) {
      return SampleSizeRounding.QUALITY;
    }
  }
```

一. 处理数据流
从 Glide 的一次加载流程中可知, Glide 拿到数据流之后, 使用 Downsampler 进行采样处理并且反回了一个 Bitmap
Downsampler.java
```
public Resource<Bitmap> decode(
      InputStream is,
      int requestedWidth,
      int requestedHeight,
      Options options,
      DecodeCallbacks callbacks)
      throws IOException {
    return decode(
        new ImageReader.InputStreamImageReader(is, parsers, byteArrayPool),
        requestedWidth,
        requestedHeight,
        options,
        callbacks);
  }

   private Resource<Bitmap> decode(
      ImageReader imageReader,
      int requestedWidth,
      int requestedHeight,
      Options options,
      DecodeCallbacks callbacks)
      throws IOException {
    // 从缓存复用池中获取 byte 数据组     STANDARD_BUFFER_SIZE_BYTES: 64 * 1024=64KB
    byte[] bytesForOptions = byteArrayPool.get(ArrayPool.STANDARD_BUFFER_SIZE_BYTES, byte[].class);
    BitmapFactory.Options bitmapFactoryOptions = getDefaultOptions();
    // 获取 Bitmap.Options 并为其 BitmapFactory.Options.inTempStorage 分配缓冲区
    bitmapFactoryOptions.inTempStorage = bytesForOptions;
    //todo inTempStorage
    // 获取解码的类型, ARGB_8888, RGB_565.
    DecodeFormat decodeFormat = options.get(DECODE_FORMAT);
    //todo PreferredColorSpace p3色域
    PreferredColorSpace preferredColorSpace = options.get(PREFERRED_COLOR_SPACE);
    // 获取采样压缩的策略
    DownsampleStrategy downsampleStrategy = options.get(DownsampleStrategy.OPTION);
    //是否需要将 Bitmap 的宽高固定为请求的尺寸
    boolean fixBitmapToRequestedDimensions = options.get(FIX_BITMAP_SIZE_TO_REQUESTED_DIMENSIONS);
    // 用于判断 Bitmap 尺寸是否是可变的
    boolean isHardwareConfigAllowed =
        options.get(ALLOW_HARDWARE_CONFIG) != null && options.get(ALLOW_HARDWARE_CONFIG);

    try {
      // 调用 decodeFromWrappedStreams 获取 Bitmap 数据
      Bitmap result =
          decodeFromWrappedStreams(
              imageReader,
              bitmapFactoryOptions,
              downsampleStrategy,
              decodeFormat,
              preferredColorSpace,
              isHardwareConfigAllowed,
              requestedWidth,
              requestedHeight,
              fixBitmapToRequestedDimensions,
              callbacks);
      return BitmapResource.obtain(result, bitmapPool);
    } finally {
      //对bitmapFactoryOptions和bytesForOptions进行复用
      releaseOptions(bitmapFactoryOptions);
      // 回收数组数据
      byteArrayPool.put(bytesForOptions);
    }
  }
  
  private Bitmap decodeFromWrappedStreams(
      ImageReader imageReader,
      BitmapFactory.Options options,
      DownsampleStrategy downsampleStrategy,
      DecodeFormat decodeFormat,
      PreferredColorSpace preferredColorSpace,
      boolean isHardwareConfigAllowed,
      int requestedWidth,
      int requestedHeight,
      boolean fixBitmapToRequestedDimensions,
      DecodeCallbacks callbacks)
      throws IOException {
    long startTime = LogTime.getLogTime();
     // 通过数据流解析图片的尺寸
    int[] sourceDimensions = getDimensions(imageReader, options, callbacks, bitmapPool);
    int sourceWidth = sourceDimensions[0];
    int sourceHeight = sourceDimensions[1];
    String sourceMimeType = options.outMimeType;

    // If we failed to obtain the image dimensions, we may end up with an incorrectly sized Bitmap,
    // so we want to use a mutable Bitmap type. One way this can happen is if the image header is so
    // large (10mb+) that our attempt to use inJustDecodeBounds fails and we're forced to decode the
    // full size image.
    if (sourceWidth == -1 || sourceHeight == -1) {
      isHardwareConfigAllowed = false;
    }
    //获取图形的旋转角度等信息 
    int orientation = imageReader.getImageOrientation();
    int degreesToRotate = TransformationUtils.getExifOrientationDegrees(orientation);
    boolean isExifOrientationRequired = TransformationUtils.isExifOrientationRequired(orientation);
    // 获取目标的宽高
    int targetWidth =
        requestedWidth == Target.SIZE_ORIGINAL
            ? (isRotationRequired(degreesToRotate) ? sourceHeight : sourceWidth)
            : requestedWidth;
    int targetHeight =
        requestedHeight == Target.SIZE_ORIGINAL
            ? (isRotationRequired(degreesToRotate) ? sourceWidth : sourceHeight)
            : requestedHeight;
    // 解析图片封装格式, JPEG, PNG, WEBP, GIF.
    ImageType imageType = imageReader.getImageType();
    // 计算 Bitmap 的采样率存放到 options.inSampleSize 中
    calculateScaling(
        imageType,
        imageReader,
        callbacks,
        bitmapPool,
        downsampleStrategy,
        degreesToRotate,
        sourceWidth,
        sourceHeight,
        targetWidth,
        targetHeight,
        options);
    //计算 Bitmap 所需颜色通道, 保存到 options.inPreferredConfig 中    
    calculateConfig(
        imageReader,
        decodeFormat,
        isHardwareConfigAllowed,
        isExifOrientationRequired,
        options,
        targetWidth,
        targetHeight);

    //根据采样率计算期望的尺寸
    boolean isKitKatOrGreater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    // Prior to KitKat, the inBitmap size must exactly match the size of the bitmap we're decoding.
    if ((options.inSampleSize == 1 || isKitKatOrGreater) && shouldUsePool(imageType)) {
      int expectedWidth;
      int expectedHeight;
      if (sourceWidth >= 0
          && sourceHeight >= 0
          && fixBitmapToRequestedDimensions
          && isKitKatOrGreater) {
        expectedWidth = targetWidth;
        expectedHeight = targetHeight;
      } else {
        float densityMultiplier =
            isScaling(options) ? (float) options.inTargetDensity / options.inDensity : 1f;
        int sampleSize = options.inSampleSize;
        int downsampledWidth = (int) Math.ceil(sourceWidth / (float) sampleSize);
        int downsampledHeight = (int) Math.ceil(sourceHeight / (float) sampleSize);
        expectedWidth = Math.round(downsampledWidth * densityMultiplier);
        expectedHeight = Math.round(downsampledHeight * densityMultiplier);

        if (Log.isLoggable(TAG, Log.VERBOSE)) {
         。。。。
        }
      }
      // If this isn't an image, or BitmapFactory was unable to parse the size, width and height
      // will be -1 here.
      //根据期望的宽高从 BitmapPool 中取可以复用的对象, 存入 Options.inBitmap 中, 减少内存消耗
      if (expectedWidth > 0 && expectedHeight > 0) {
        setInBitmap(options, bitmapPool, expectedWidth, expectedHeight);
      }
    }
    //p3色域还是SRGB
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      boolean isP3Eligible =
          preferredColorSpace == PreferredColorSpace.DISPLAY_P3
              && options.outColorSpace != null
              && options.outColorSpace.isWideGamut();
      options.inPreferredColorSpace =
          ColorSpace.get(isP3Eligible ? ColorSpace.Named.DISPLAY_P3 : ColorSpace.Named.SRGB);
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      options.inPreferredColorSpace = ColorSpace.get(ColorSpace.Named.SRGB);
    }
    //根据配置好的 options 解析数据流
    Bitmap downsampled = decodeStream(imageReader, options, callbacks, bitmapPool);
    callbacks.onDecodeComplete(bitmapPool, downsampled);

    if (Log.isLoggable(TAG, Log.VERBOSE)) {
      ...
    }
    // 尝试对图片进行角度矫正
    Bitmap rotated = null;
    if (downsampled != null) {
     
      // If we scaled, the Bitmap density will be our inTargetDensity. Here we correct it back to
      // the expected density dpi.
      downsampled.setDensity(displayMetrics.densityDpi);
      // 尝试对图片进行旋转操作
      rotated = TransformationUtils.rotateImageExif(bitmapPool, downsampled, orientation);
      // 若返回了一个新的 Bitmap, 则将之前的 Bitmap 添加进享元复用池
      if (!downsampled.equals(rotated)) {
        bitmapPool.put(downsampled);
      }
    }

    return rotated;
  }  
```
好的, Downsampler.decode 解析数据流获取 Bitmap 对象一共有如下几个步骤

通过数据流解析出图形的原始宽高
获取图形的旋转角度等信息
获取这次图片请求的目标宽高
获取图像的封装格式 JPEG, PNG, WEBP, GIF...
计算 Bitmap 缩放方式
计算 Bitmap 颜色通道
根据采样率计算期望的尺寸
   根据期望的宽高从 BitmapPool 中取可以复用的对象, 存入 Options.inBitmap 中, 减少内存消耗
根据配置好的 options 解析数据流
   与获取图像原始宽高的操作一致
对图像进行角度矫正

好的, 可见 Glide 解析一次数据流做了很多的操作, 我们对重点的操作进行逐一分析

二. 通过数据流获取图像宽高
```
 private static int[] getDimensions(
      ImageReader imageReader,
      BitmapFactory.Options options,
      DecodeCallbacks decodeCallbacks,
      BitmapPool bitmapPool)
      throws IOException {
    //只解码大小  
    options.inJustDecodeBounds = true;
    decodeStream(imageReader, options, decodeCallbacks, bitmapPool);
    options.inJustDecodeBounds = false;
    return new int[] {options.outWidth, options.outHeight};
  }

  private static Bitmap decodeStream(
      ImageReader imageReader,
      BitmapFactory.Options options,
      DecodeCallbacks callbacks,
      BitmapPool bitmapPool)
      throws IOException {
    if (!options.inJustDecodeBounds) {
      // Once we've read the image header, we no longer need to allow the buffer to expand in
      // size. To avoid unnecessary allocations reading image data, we fix the mark limit so that it
      // is no larger than our current buffer size here. We need to do so immediately before
      // decoding the full image to avoid having our mark limit overridden by other calls to
      // mark and reset. See issue #225.
      callbacks.onObtainBounds();
      imageReader.stopGrowingBuffers();
    }
    //todo options.outwidth
    // BitmapFactory.Options out* variables are reset by most calls to decodeStream, successful or
    // otherwise, so capture here in case we log below.
    int sourceWidth = options.outWidth;
    int sourceHeight = options.outHeight;
    String outMimeType = options.outMimeType;
    final Bitmap result;
    TransformationUtils.getBitmapDrawableLock().lock();
    try {
       //解析 InputStream 将数据保存在 options 中  最终使用BitmapFactory解码为Bitmap
      result = imageReader.decodeBitmap(options);
    } catch (IllegalArgumentException e) {
      IOException bitmapAssertionException =
          newIoExceptionForInBitmapAssertion(e, sourceWidth, sourceHeight, outMimeType, options);
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(
            TAG,
            "Failed to decode with inBitmap, trying again without Bitmap re-use",
            bitmapAssertionException);
      }
      //若是因为 BitmapFactory 无法重用 options.inBitmap 这个位图, 则会清空inBitmap，然后重试
      if (options.inBitmap != null) {
        try {
          //将 inBitmap 添加到缓存池中
          bitmapPool.put(options.inBitmap);
          //将 options.inBitmap 置空后重新解析
          options.inBitmap = null;
          return decodeStream(imageReader, options, callbacks, bitmapPool);
        } catch (IOException resetException) {
          throw bitmapAssertionException;
        }
      }
      throw bitmapAssertionException;
    } finally {
      TransformationUtils.getBitmapDrawableLock().unlock();
    }
    //返回解析到的数据
    return result;
  }
```
具体的流程如上所示, 其中还是有很多细节值得我们参考和学习
在解析 Bitmap 的时候, 通过给 Options 中的 inBitmap 赋值, 让新解析的 Bitmap 复用这个对象以此来减少内存的消耗
若无法复用则会在异常处理中, 使用无 inBitmap 的方式再次解析

三. 获取图像封装格式   获取旋转方向的类似
imageReader 以InputStreamImageReader为例  
ImageReader.java
```
public ImageHeaderParser.ImageType getImageType() throws IOException {
      return ImageHeaderParserUtils.getType(parsers, dataRewinder.rewindAndGet(), byteArrayPool);
    }

    @Override
    public int getImageOrientation() throws IOException {
      return ImageHeaderParserUtils.getOrientation(
          parsers, dataRewinder.rewindAndGet(), byteArrayPool);
    }
```
ImageHeaderParserUtils.java
```
 public static ImageType getType(
      @NonNull List<ImageHeaderParser> parsers,
      @Nullable InputStream is,
      @NonNull ArrayPool byteArrayPool)
      throws IOException {
    if (is == null) {
      return ImageType.UNKNOWN;
    }

    if (!is.markSupported()) {
     //todo markSupported  RecyclableBufferedInputStream
      is = new RecyclableBufferedInputStream(is, byteArrayPool);
    }

    is.mark(MARK_READ_LIMIT);
    final InputStream finalIs = is;
    return getTypeInternal(
        parsers,
        new TypeReader() {
          @Override
          public ImageType getType(ImageHeaderParser parser) throws IOException {
            try {
              return parser.getType(finalIs);
            } finally {
              //todo 这是不是重置流的位置
              finalIs.reset();
            }
          }
        });
  }

  private static ImageType getTypeInternal(
      @NonNull List<ImageHeaderParser> parsers, TypeReader reader) throws IOException {
    //noinspection ForLoopReplaceableByForEach to improve perf
    for (int i = 0, size = parsers.size(); i < size; i++) {
     // 1. 获取解析器
      ImageHeaderParser parser = parsers.get(i);
      // 2. 使用解析器解析输入流获取图片类型
      ImageType type = reader.getType(parser);
      if (type != ImageType.UNKNOWN) {
        return type;
      }
    }

    return ImageType.UNKNOWN;
  }
  
```
好的, 首先是获取解析器, 这个解析器是 Glide 对象创建时注册的   todo glide对象创建
```
  registry = new Registry();
    registry.register(new DefaultImageHeaderParser());
    // Right now we're only using this parser for HEIF images, which are only supported on OMR1+.
    // If we need this for other file types, we should consider removing this restriction.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
      registry.register(new ExifInterfaceImageHeaderParser());
    }
```
Glide 中提供了两个解析器, 分别为 DefaultImageHeaderParser 和 ExifInterfaceImageHeaderParser, 
//todo 图片解析

四. 计算 Bitmap 缩放方式
Glid 对于 Bitmap 缩放的计算过程比较复杂, 分别有如下几步
计算采样率
计算采样后图片的尺寸
将采样后图片的尺寸调整为目标尺寸
一) 计算采样率
```
private static void calculateScaling(
      ImageType imageType,
      ImageReader imageReader,
      DecodeCallbacks decodeCallbacks,
      BitmapPool bitmapPool,
      DownsampleStrategy downsampleStrategy,
      int degreesToRotate,
      int sourceWidth,
      int sourceHeight,
      int targetWidth,
      int targetHeight,
      BitmapFactory.Options options)
      throws IOException {
    // We can't downsample source content if we can't determine its dimensions.
    if (sourceWidth <= 0 || sourceHeight <= 0) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        ...
      }
      return;
    }
    计算采样率 
    // 获取源图片尺寸与目标尺寸的精确缩放比
    // downsampleStrategy 在构建 Request 时传入
    int orientedSourceWidth = sourceWidth;
    int orientedSourceHeight = sourceHeight;
    // If we're rotating the image +-90 degrees, we need to downsample accordingly so the image
    // width is decreased to near our target's height and the image height is decreased to near
    // our target width.
    //noinspection SuspiciousNameCombination
    //degreesToRotate == 90 || degreesToRotate == 270 旋转为90或270
    if (isRotationRequired(degreesToRotate)) {
      orientedSourceWidth = sourceHeight;
      orientedSourceHeight = sourceWidth;
    }
    //计算缩放因子  CenterOutSize 
    //widthPercentage=requestedWidth / (float) sourceWidth;  heightPercentage = requestedHeight / (float) sourceHeight;
    //Math.max(widthPercentage, heightPercentage)
    final float exactScaleFactor =
        downsampleStrategy.getScaleFactor(
            orientedSourceWidth, orientedSourceHeight, targetWidth, targetHeight);

    if (exactScaleFactor <= 0f) {
      throw new IllegalArgumentException(
          ....
    }
    //获取采样的类型: MEMORY(节省内存), QUALITY(更高质量)
    SampleSizeRounding rounding =
        downsampleStrategy.getSampleSizeRounding(
            orientedSourceWidth, orientedSourceHeight, targetWidth, targetHeight);
    if (rounding == null) {
      throw new IllegalArgumentException("Cannot round with null rounding");
    }
     // 计算缩放因子
    // 计算整型的尺寸(round 操作在原来值的基础上 + 0.5), 参考 Android 源码
    int outWidth = round(exactScaleFactor * orientedSourceWidth);
    int outHeight = round(exactScaleFactor * orientedSourceHeight);
    //计算宽高方向上的整型缩放因子
    int widthScaleFactor = orientedSourceWidth / outWidth;
    int heightScaleFactor = orientedSourceHeight / outHeight;

    // This isn't really right for both CenterOutside and CenterInside. Consider allowing
    // DownsampleStrategy to pick, or trying to do something more sophisticated like picking the
    // scale factor that leads to an exact match.
     // 根据采样类型, 确定整型缩放因子 scaleFactor
     // 若为 MEMORY, 则为宽高的最大值
     // 若为 QUALITY, 则为宽高的最小值
    int scaleFactor =
        rounding == SampleSizeRounding.MEMORY
            ? Math.max(widthScaleFactor, heightScaleFactor)
            : Math.min(widthScaleFactor, heightScaleFactor);

     //根据整型缩放因子, 计算采样率(即将 scaleFactor 转为 2 的幂次)
    int powerOfTwoSampleSize;
    // BitmapFactory does not support downsampling wbmp files on platforms <= M. See b/27305903.
    //Android 7.0 以下不支持缩放 webp, 缩放因子置为 1    image/vnd.wap.wbmp,image/x-ico两种类型
    if (Build.VERSION.SDK_INT <= 23
        && NO_DOWNSAMPLE_PRE_N_MIME_TYPES.contains(options.outMimeType)) {
      powerOfTwoSampleSize = 1;
    } else {
       //将 scaleFactor 转为 2 的幂次, 若为省内存模式, 则尝试近一步增加采样率 将powerOfTwoSampleSize*2
      powerOfTwoSampleSize = Math.max(1, Integer.highestOneBit(scaleFactor));
      if (rounding == SampleSizeRounding.MEMORY
          && powerOfTwoSampleSize < (1.f / exactScaleFactor)) {
        powerOfTwoSampleSize = powerOfTwoSampleSize << 1;
      }
    }
     
    // Here we mimic framework logic for determining how inSampleSize division is rounded on various
    // versions of Android. The logic here has been tested on emulators for Android versions 15-26.
    // PNG - Always uses floor
    // JPEG - Always uses ceiling
    // Webp - Prior to N, always uses floor. At and after N, always uses round.
    options.inSampleSize = powerOfTwoSampleSize;
    
    //根据采样率, 计算采样后图片的尺寸
    int powerOfTwoWidth;
    int powerOfTwoHeight;
    //todo 为什么对不同图片做不同处理
    //处理 JPEG
    if (imageType == ImageType.JPEG) {
      // libjpegturbo can downsample up to a sample size of 8. libjpegturbo uses ceiling to round.
      // After libjpegturbo's native rounding, skia does a secondary scale using floor
      // (integer division). Here we replicate that logic.
      //Libjpeg 最高支持单次 8 位的降采样, 超过 8 次则分步计算   todo Libjpeg
      int nativeScaling = Math.min(powerOfTwoSampleSize, 8);
      powerOfTwoWidth = (int) Math.ceil(orientedSourceWidth / (float) nativeScaling);
      powerOfTwoHeight = (int) Math.ceil(orientedSourceHeight / (float) nativeScaling);
      int secondaryScaling = powerOfTwoSampleSize / 8;
      //若 powerOfTwoSampleSize 比 8 大, 则再进行一次采样, 用于计算出最终的目标值
      if (secondaryScaling > 0) {
        powerOfTwoWidth = powerOfTwoWidth / secondaryScaling;
        powerOfTwoHeight = powerOfTwoHeight / secondaryScaling;
      }
    } else if (imageType == ImageType.PNG || imageType == ImageType.PNG_A) {
      //处理 PNG  对采样结果向下取整
      powerOfTwoWidth = (int) Math.floor(orientedSourceWidth / (float) powerOfTwoSampleSize);
      powerOfTwoHeight = (int) Math.floor(orientedSourceHeight / (float) powerOfTwoSampleSize);
    } else if (imageType == ImageType.WEBP || imageType == ImageType.WEBP_A) {
      // 处理 WEBP
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        // 7.0 以上对采样结果进行四舍五入
        powerOfTwoWidth = Math.round(orientedSourceWidth / (float) powerOfTwoSampleSize);
        powerOfTwoHeight = Math.round(orientedSourceHeight / (float) powerOfTwoSampleSize);
      } else {
        // 7.0 以下, 对采样结果向下取整
        powerOfTwoWidth = (int) Math.floor(orientedSourceWidth / (float) powerOfTwoSampleSize);
        powerOfTwoHeight = (int) Math.floor(orientedSourceHeight / (float) powerOfTwoSampleSize);
      }
    } else if (orientedSourceWidth % powerOfTwoSampleSize != 0
        || orientedSourceHeight % powerOfTwoSampleSize != 0) {
      //处理其他图片类型, 并且需要降采样  
      // If we're not confident the image is in one of our types, fall back to checking the
      // dimensions again. inJustDecodeBounds decodes do obey inSampleSize.
      //通过 Android 的 BitmapFactory 去获取尺寸
      int[] dimensions = getDimensions(imageReader, options, decodeCallbacks, bitmapPool);
      // Power of two downsampling in BitmapFactory uses a variety of random factors to determine
      // rounding that we can't reliably replicate for all image formats. Use ceiling here to make
      // sure that we at least provide a Bitmap that's large enough to fit the content we're going
      // to load.
      powerOfTwoWidth = dimensions[0];
      powerOfTwoHeight = dimensions[1];
    } else {
      //处理其他图片类型, 并且不需要降采样
      powerOfTwoWidth = orientedSourceWidth / powerOfTwoSampleSize;
      powerOfTwoHeight = orientedSourceHeight / powerOfTwoSampleSize;
    }
    //
    // 将采样尺寸调整成为目标尺寸
    // 计算采样尺寸与目标尺寸的缩放因子
    double adjustedScaleFactor =
        downsampleStrategy.getScaleFactor(
            powerOfTwoWidth, powerOfTwoHeight, targetWidth, targetHeight);

    // Density scaling is only supported if inBitmap is null prior to KitKat. Avoid setting
    // densities here so we calculate the final Bitmap size correctly.
    //通过调整 inTargetDensity 和 inDensity 来完成目标的显示效果
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      //调整目标的屏幕密度    //todo 这两个调整 进行debug
      options.inTargetDensity = adjustTargetDensityForError(adjustedScaleFactor);
      // 调整图片的像素密度
      options.inDensity = getDensityMultiplier(adjustedScaleFactor);
    }
    if (isScaling(options)) {
      options.inScaled = true;
    } else {
      options.inDensity = options.inTargetDensity = 0;
    }
    ...
  }
  
  private static int adjustTargetDensityForError(double adjustedScaleFactor) {
    int densityMultiplier = getDensityMultiplier(adjustedScaleFactor);
    int targetDensity = round(densityMultiplier * adjustedScaleFactor);
    float scaleFactorWithError = targetDensity / (float) densityMultiplier;
    double difference = adjustedScaleFactor / scaleFactorWithError;
    return round(difference * targetDensity);
  }
  
  private static int getDensityMultiplier(double adjustedScaleFactor) {
    return (int)
        Math.round(
            Integer.MAX_VALUE
                * (adjustedScaleFactor <= 1D ? adjustedScaleFactor : 1 / adjustedScaleFactor));
  }  
```
计算采样率的过程主要有如下几步
计算精确的缩放因子
获取采样的类型
   MEMORY: 省内存
   QUALITY: 高质量
计算整型的缩放因子
将整型缩放因子转为 2 的幂次
   即转为 BitmapFactory 可用的采样率  //inSampleSize的文档说明了转为2的次幂

计算采样尺寸, Glide 并没有直接将采样率放入 options.inSampleSize 而是根据规则自行进行了运算, 降低了使用 BitmapFactory 
  调用 native 方法带来的性能损耗

可以看到将采样尺寸调整成为目标尺寸是通过调整 options 中 inTargetDensity 和 inDensity 的值, 来让图片缩放到目标显示效果尺寸的
好的, 到这里 Glide 计算 Bitmap 缩放的部分就解析完毕了, 我们光知道 Glide 默认会将图片加载的尺寸置为 ImageView 的大小, 
却不知道它为了还原的精度, 内部做了如何之多的细节处理, 其缜密性可见一斑


五. 选择颜色通道
```
private void calculateConfig(
      ImageReader imageReader,
      DecodeFormat format,
      boolean isHardwareConfigAllowed,
      boolean isExifOrientationRequired,
      BitmapFactory.Options optionsWithScaling,
      int targetWidth,
      int targetHeight) {

    if (hardwareConfigState.setHardwareConfigIfAllowed(
        targetWidth,
        targetHeight,
        optionsWithScaling,
        isHardwareConfigAllowed,
        isExifOrientationRequired)) {
      return;
    }

    // Changing configs can cause skewing on 4.1, see issue #128.
    if (format == DecodeFormat.PREFER_ARGB_8888
        || Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN) {
      optionsWithScaling.inPreferredConfig = Bitmap.Config.ARGB_8888;
      return;
    }
    //判断是否有 Alpha 通道
    boolean hasAlpha = false;
    try {
      hasAlpha = imageReader.getImageType().hasAlpha();
    } catch (IOException e) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        ...
      }
    }
    // 若存在 Alpha 通道则使用 RGB_8888, 反之使用 565
    optionsWithScaling.inPreferredConfig =
        hasAlpha ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
    if (optionsWithScaling.inPreferredConfig == Config.RGB_565) {
      //inDither废弃了,用来解决防抖，锯齿的马赛克
      optionsWithScaling.inDither = true;
    }
  }
```
好的, Bitmap 颜色通道的选取方式还是非常简单的
  对于存在透明通道的图片, 使用 ARGB_8888 保证图片不会丢失透明通道
  对于无透明通道图片, 使用 RGB_565 保证图片内存占用量最低


//is.reset 流的复用？？


