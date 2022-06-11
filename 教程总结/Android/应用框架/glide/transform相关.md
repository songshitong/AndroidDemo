
2022-04-27
https://blog.csdn.net/f409031mn/article/details/103546837
使用transform
```
RequestOptions requestOptions = new RequestOptions()
   .transforms(new CenterCrop(), new CircleCrop());
Glide.with(this)
     .load("图片url")
     .apply(requestOptions)
     .into(imageViewRes);
```
自定义 圆形图片带描边
```
public class CircleCropWithBorderCorp extends BitmapTransformation {
   private final Paint mPaint;

   public CircleCropWithBorderCorp(int dp) {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(dp);
        mPaint.setColor(Color.RED);
   }
  
  @Override
   protected Bitmap transform(BitmapPool pool,  Bitmap toTransform
     , int outWidth, int outHeight) {
        Bitmap bitmap = TransformationUtils.centerCrop(pool, toTransform, outWidth, outHeight);
        Bitmap result = TransformationUtils.circleCrop(pool, bitmap, outWidth, outHeight);
        int destMinEdge = Math.min(outWidth, outHeight);
        int radius = (int) (destMinEdge / 2f);
        Canvas canvas = new Canvas(result);
        canvas.drawCircle(radius, radius, radius - mPaint.getStrokeWidth() / 2, mPaint);
        return result;
    }
}
```

com/bumptech/glide/request/BaseRequestOptions.java  
使用多个变换
```
  public T transform(@NonNull Transformation<Bitmap>... transformations) {
    if (transformations.length > 1) {
      return transform(new MultiTransformation<>(transformations), /*isRequired=*/ true);
    } else if (transformations.length == 1) {
      return transform(transformations[0]);
    } else {
      return selfOrThrowIfLocked();
    }
  }
  
T transform(@NonNull Transformation<Bitmap> transformation, boolean isRequired) {
    if (isAutoCloneEnabled) {
      return clone().transform(transformation, isRequired);
    }
    
    DrawableTransformation drawableTransformation =
        new DrawableTransformation(transformation, isRequired);
    //写入option    
    transform(Bitmap.class, transformation, isRequired);
    transform(Drawable.class, drawableTransformation, isRequired);
    transform(BitmapDrawable.class, drawableTransformation.asBitmapDrawable(), isRequired);
    transform(GifDrawable.class, new GifDrawableTransformation(transformation), isRequired);
    return selfOrThrowIfLocked();
  } 
  
com/bumptech/glide/load/resource/bitmap/DrawableTransformation.java 
public class DrawableTransformation implements Transformation<Drawable> {
  private final Transformation<Bitmap> wrapped;
  private final boolean isRequired;
  public DrawableTransformation(Transformation<Bitmap> wrapped, boolean isRequired) {
    this.wrapped = wrapped;
    this.isRequired = isRequired;
  }  
  public Resource<Drawable> transform(
      @NonNull Context context, @NonNull Resource<Drawable> resource, int outWidth, int outHeight) {
    BitmapPool bitmapPool = Glide.get(context).getBitmapPool();
    Drawable drawable = resource.get();
    Resource<Bitmap> bitmapResourceToTransform =
        DrawableToBitmapConverter.convert(bitmapPool, drawable, outWidth, outHeight);
   ....
    //执行传入的transform
    Resource<Bitmap> transformedBitmapResource =
        wrapped.transform(context, bitmapResourceToTransform, outWidth, outHeight);

    if (transformedBitmapResource.equals(bitmapResourceToTransform)) {
      transformedBitmapResource.recycle();
      return resource;
    } else {
      return newDrawableResource(context, transformedBitmapResource);
    }
  }
 }  
```
MultiTransformation主要是收集多个Transformation，然后应用transformation.transform
com/bumptech/glide/load/MultiTransformation.java
```
public interface Transformation<T> extends Key { //变化接口
  Resource<T> transform(
      @NonNull Context context, @NonNull Resource<T> resource, int outWidth, int outHeight);
}

public class MultiTransformation<T> implements Transformation<T> {
  private final Collection<? extends Transformation<T>> transformations;
   public MultiTransformation(@NonNull Transformation<T>... transformations) {
    ...
    this.transformations = Arrays.asList(transformations);
  }
  
   public Resource<T> transform(
      @NonNull Context context, @NonNull Resource<T> resource, int outWidth, int outHeight) {
    Resource<T> previous = resource;

    for (Transformation<T> transformation : transformations) {
      Resource<T> transformed = transformation.transform(context, previous, outWidth, outHeight);
      if (previous != null && !previous.equals(resource) && !previous.equals(transformed)) {
        previous.recycle();
      }
      previous = transformed;
    }
    return previous;
  }
}    
```

circleCrop
com/bumptech/glide/load/resource/bitmap/BitmapTransformation.java
com/bumptech/glide/load/resource/bitmap/CircleCrop.java
```
public abstract class BitmapTransformation implements Transformation<Bitmap> {
  public final Resource<Bitmap> transform(
      @NonNull Context context, @NonNull Resource<Bitmap> resource, int outWidth, int outHeight) {
    ...
    //从BitmapPool中获取bitmap
    BitmapPool bitmapPool = Glide.get(context).getBitmapPool();
    Bitmap toTransform = resource.get();
    
    //对比得知这次图片转换过程中的图片裁剪尺寸
    int targetWidth = outWidth == Target.SIZE_ORIGINAL ? toTransform.getWidth() : outWidth;
    int targetHeight = outHeight == Target.SIZE_ORIGINAL ? toTransform.getHeight() : outHeight;
    
    //得到图片转换后的Bitmap对象 子类实现
    Bitmap transformed = transform(bitmapPool, toTransform, targetWidth, targetHeight);
    //将Bitmap对象包裹起来进行传递
    final Resource<Bitmap> result;
    if (toTransform.equals(transformed)) {
      result = resource;
    } else {
      result = BitmapResource.obtain(transformed, bitmapPool);
    }
    return result;
  }

  protected abstract Bitmap transform(
      @NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth, int outHeight);
}

public class CircleCrop extends BitmapTransformation {
  // The version of this transformation, incremented to correct an error in a previous version.
  // See #455.
  private static final int VERSION = 1;
  private static final String ID = "com.bumptech.glide.load.resource.bitmap.CircleCrop." + VERSION;
  private static final byte[] ID_BYTES = ID.getBytes(CHARSET);

  @SuppressWarnings("PMD.CompareObjectsWithEquals")
  @Override
  protected Bitmap transform(
      @NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth, int outHeight) {
    //使用TransformationUtils完成转换  
    return TransformationUtils.circleCrop(pool, toTransform, outWidth, outHeight);
  }

  @Override
  public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
    messageDigest.update(ID_BYTES);
  }
}
```
com/bumptech/glide/load/resource/bitmap/TransformationUtils.java
```
 //https://github.com/bumptech/glide/issues/738
  //在一些机器上，bitmap drawing不是线程安全的，只有在特定机器上才会上锁，其他的不会上锁，所以不会有性能影响
  //根据不同的机型使用不同的锁  NoLock实现Lock接口
  //Moto X gen 2,Moto G gen 1,Moto G gen 2
  private static final Lock BITMAP_DRAWABLE_LOCK =
      MODELS_REQUIRING_BITMAP_LOCK.contains(Build.MODEL) ? new ReentrantLock() : new NoLock();

 CIRCLE_CROP_BITMAP_PAINT = new Paint(CIRCLE_CROP_PAINT_FLAGS);
 CIRCLE_CROP_BITMAP_PAINT.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
  public static Bitmap circleCrop(
      @NonNull BitmapPool pool, @NonNull Bitmap inBitmap, int destWidth, int destHeight) {
    //计算圆形半径
    int destMinEdge = Math.min(destWidth, destHeight);
    float radius = destMinEdge / 2f;

    int srcWidth = inBitmap.getWidth();
    int srcHeight = inBitmap.getHeight();
    //计算出最大缩小倍数
    float scaleX = destMinEdge / (float) srcWidth;
    float scaleY = destMinEdge / (float) srcHeight;
    float maxScale = Math.max(scaleX, scaleY);
    //计算出裁剪的中心区域
    float scaledWidth = maxScale * srcWidth;
    float scaledHeight = maxScale * srcHeight;
    float left = (destMinEdge - scaledWidth) / 2f;
    float top = (destMinEdge - scaledHeight) / 2f;

    RectF destRect = new RectF(left, top, left + scaledWidth, top + scaledHeight);

    //从池中取出可复用的Bitmap(使用inBitmap重新绘制该复用的Bitmap)
    //减少内存抖动
    Bitmap toTransform = getAlphaSafeBitmap(pool, inBitmap);
    Bitmap.Config outConfig = getAlphaSafeConfig(inBitmap);
    //从池中取出一个可服用的Bitmap对象用于绘制toTransform
    //减少内存抖动
    Bitmap result = pool.get(destMinEdge, destMinEdge, outConfig);
    result.setHasAlpha(true);
    //上锁
    BITMAP_DRAWABLE_LOCK.lock();
    try {
      Canvas canvas = new Canvas(result);
      // Draw a circle 画一个圆形
      canvas.drawCircle(radius, radius, radius, CIRCLE_CROP_SHAPE_PAINT);
      // Draw the bitmap in the circle  在圆形上绘制bitmap   使用PorterDuff.Mode.SRC_IN
      canvas.drawBitmap(toTransform, null, destRect, CIRCLE_CROP_BITMAP_PAINT);
      //清空canvas
      clear(canvas);
    } finally {
      BITMAP_DRAWABLE_LOCK.unlock();
    }

    if (!toTransform.equals(inBitmap)) {
      //复用
      pool.put(toTransform);
    }

    return result;
  }
  
  private static Bitmap getAlphaSafeBitmap(
      @NonNull BitmapPool pool, @NonNull Bitmap maybeAlphaSafe) {
    Bitmap.Config safeConfig = getAlphaSafeConfig(maybeAlphaSafe);
    if (safeConfig.equals(maybeAlphaSafe.getConfig())) {
      return maybeAlphaSafe;
    }

    Bitmap argbBitmap = pool.get(maybeAlphaSafe.getWidth(), maybeAlphaSafe.getHeight(), safeConfig);
    new Canvas(argbBitmap).drawBitmap(maybeAlphaSafe, 0 /*left*/, 0 /*top*/, null /*paint*/);

    // We now own this Bitmap. It's our responsibility to replace it in the pool outside this method
    // when we're finished with it.
    return argbBitmap;
  }
  
   private static Config getAlphaSafeConfig(@NonNull Bitmap inBitmap) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      // Avoid short circuiting the sdk check.
      if (Bitmap.Config.RGBA_F16.equals(inBitmap.getConfig())) { // NOPMD
        return Bitmap.Config.RGBA_F16;
      }
    }
    return Bitmap.Config.ARGB_8888;
  }   
```

Glide中transform的使用  transformations从Options一路传递到这
com/bumptech/glide/load/engine/DecodeHelper.java
```
<Z> Transformation<Z> getTransformation(Class<Z> resourceClass) {
    Transformation<Z> result = (Transformation<Z>) transformations.get(resourceClass);
    if (result == null) {
      for (Entry<Class<?>, Transformation<?>> entry : transformations.entrySet()) {
        if (entry.getKey().isAssignableFrom(resourceClass)) {
          result = (Transformation<Z>) entry.getValue();
          break;
        }
      }
    }
    ...
    return result;
  }
```
com/bumptech/glide/load/engine/DecodeJob.java
```
<Z> Resource<Z> onResourceDecoded(DataSource dataSource, @NonNull Resource<Z> decoded) {
    @SuppressWarnings("unchecked")
    Class<Z> resourceSubClass = (Class<Z>) decoded.get().getClass();
    Transformation<Z> appliedTransformation = null;
    Resource<Z> transformed = decoded;
    if (dataSource != DataSource.RESOURCE_DISK_CACHE) {
      appliedTransformation = decodeHelper.getTransformation(resourceSubClass);
      //应用变换
      transformed = appliedTransformation.transform(glideContext, decoded, width, height);
    }
    if (!decoded.equals(transformed)) {
      decoded.recycle();
    }

    final EncodeStrategy encodeStrategy;
    final ResourceEncoder<Z> encoder;
    if (decodeHelper.isResourceEncoderAvailable(transformed)) {
      encoder = decodeHelper.getResultEncoder(transformed);
      encodeStrategy = encoder.getEncodeStrategy(options);
    } else {
      encoder = null;
      encodeStrategy = EncodeStrategy.NONE;
    }

    Resource<Z> result = transformed;
    boolean isFromAlternateCacheKey = !decodeHelper.isSourceKey(currentSourceKey);
    if (diskCacheStrategy.isResourceCacheable(
        isFromAlternateCacheKey, dataSource, encodeStrategy)) {
      if (encoder == null) {
        throw new Registry.NoResultEncoderAvailableException(transformed.get().getClass());
      }
      final Key key;
      switch (encodeStrategy) {
        case SOURCE:
          key = new DataCacheKey(currentSourceKey, signature);
          break;
        case TRANSFORMED:
          key =
              new ResourceCacheKey(
                  decodeHelper.getArrayPool(),
                  currentSourceKey,
                  signature,
                  width,
                  height,
                  appliedTransformation,
                  resourceSubClass,
                  options);
          break;
        default:
          throw new IllegalArgumentException("Unknown strategy: " + encodeStrategy);
      }

      LockedResource<Z> lockedResult = LockedResource.obtain(transformed);
      deferredEncodeManager.init(key, encoder, lockedResult);
      result = lockedResult;
    }
    return result;
  }
```