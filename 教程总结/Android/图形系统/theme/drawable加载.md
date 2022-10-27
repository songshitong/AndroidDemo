
android sdk 30
imageView.setImageResource();
```
 public void setImageResource(@DrawableRes int resId) {
        ...
        updateDrawable(null);
        mResource = resId;
        mUri = null;
        //加载drawable
        resolveUri();
        if (oldWidth != mDrawableWidth || oldHeight != mDrawableHeight) {
            requestLayout();
        }
        invalidate();
    }

 private void resolveUri() {
        ...
        if (mResource != 0) {
            try {
                d = mContext.getDrawable(mResource);
            } catch (Exception e) {
              ...
            }
        } else if (mUri != null) {
         ...
    }    
```
android 10
frameworks/base/core/java/android/content/Context.java
```
    public final Drawable getDrawable(@DrawableRes int id) {
        return getResources().getDrawable(id, getTheme());
    }
```
frameworks/base/core/java/android/content/res/Resources.java
```
    public Drawable getDrawable(@DrawableRes int id, @Nullable Theme theme)
            throws NotFoundException {
        return getDrawableForDensity(id, 0, theme);
    }
    }
 
 public Drawable getDrawableForDensity(@DrawableRes int id, int density, @Nullable Theme theme) {
    final TypedValue value = obtainTempTypedValue();
    try {
        final ResourcesImpl impl = mResourcesImpl;
        impl.getValueForDensity(id, density, value, true);
        return loadDrawable(value, id, density, theme);
    } finally {
        releaseTempTypedValue(value);
    }
    
  Drawable loadDrawable(@NonNull TypedValue value, int id, int density, @Nullable Theme theme)
        throws NotFoundException {
    return mResourcesImpl.loadDrawable(this, value, id, density, theme);
}     
```

frameworks/base/core/java/android/content/res/ResourcesImpl.java
```
 Drawable loadDrawable(@NonNull Resources wrapper, @NonNull TypedValue value, int id,
            int density, @Nullable Resources.Theme theme)
            throws NotFoundException {
   //处理density相关          
   if (density > 0 && value.density > 0 && value.density != TypedValue.DENSITY_NONE) {
            if (value.density == density) {
                value.density = mMetrics.densityDpi;
            } else {
                value.density = (value.density * mMetrics.densityDpi) / density;
            }
        }  
   。。。
   //确定是普通drawable还是ColorDrawable
    if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT
                    && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                isColorDrawable = true;
                caches = mColorDrawableCache;
                key = value.data;
            } else {
                isColorDrawable = false;
                caches = mDrawableCache;
                key = (((long) value.assetCookie) << 32) | value.data;
            } 
    。。。
    //从缓存中复用ConstantState   ConstantState用来复用不同drawable中的状态和数据
    final Drawable.ConstantState cs;
            if (isColorDrawable) {
                cs = sPreloadedColorDrawables.get(key);
            } else {
                cs = sPreloadedDrawables[mConfiguration.getLayoutDirection()].get(key);
            }  
   ...
     Drawable dr;
    boolean needsNewDrawableAfterCache = false;
    if (cs != null) {
        dr = cs.newDrawable(wrapper);
    } else if (isColorDrawable) {
        dr = new ColorDrawable(value.data);
    } else {
        //ConstantState 没有缓存，创建新的
        dr = loadDrawableForCookie(wrapper, value, id, density);
    }      
   ...//创建后的复用和配置                      
 }
 
    private Drawable loadDrawableForCookie(@NonNull Resources wrapper, @NonNull TypedValue value,
            int id, int density) {
      ...
         final Drawable dr;
     ...
     //从xml或其他文件创建drawable
     if (file.endsWith(".xml")) {
            final String typeName = getResourceTypeName(id);
            if (typeName != null && typeName.equals("color")) {
                dr = loadColorOrXmlDrawable(wrapper, value, id, density, file);
            } else {
                dr = loadXmlDrawable(wrapper, value, id, density, file);
            }
        } else {
            final InputStream is = mAssets.openNonAsset(
                    value.assetCookie, file, AssetManager.ACCESS_STREAMING);
            final AssetInputStream ais = (AssetInputStream) is;
            dr = decodeImageDrawable(ais, wrapper, value);
        }   
     ...          
   }
```
xml解析drawable为例
loadXmlDrawable-> Drawable.createFromXmlForDensity-> createFromXmlInnerForDensity->
DrawableInflater.inflateFromXmlForDensity
frameworks/base/graphics/java/android/graphics/drawable/DrawableInflater.java
```
 Drawable inflateFromXmlForDensity(@NonNull String name, @NonNull XmlPullParser parser,
            @NonNull AttributeSet attrs, int density, @Nullable Theme theme)
            throws XmlPullParserException, IOException {
     ...
     Drawable drawable = inflateFromTag(name);
     ...       
}
 //创建不同类型的drawble
 private @Nullable Drawable inflateFromTag(@NonNull String name) {
        switch (name) {
            case "selector":
                return new StateListDrawable();
            case "animated-selector":
                return new AnimatedStateListDrawable();
            case "level-list":
                return new LevelListDrawable();
            case "layer-list":
                return new LayerDrawable();
            case "transition":
                return new TransitionDrawable();
            case "ripple":
                return new RippleDrawable();
            case "adaptive-icon":
                return new AdaptiveIconDrawable();
            case "color":
                return new ColorDrawable();
            case "shape":
                return new GradientDrawable();
            case "vector":
                return new VectorDrawable();
            case "animated-vector":
                return new AnimatedVectorDrawable();
            case "scale":
                return new ScaleDrawable();
            case "clip":
                return new ClipDrawable();
            case "rotate":
                return new RotateDrawable();
            case "animated-rotate":
                return new AnimatedRotateDrawable();
            case "animation-list":
                return new AnimationDrawable();
            case "inset":
                return new InsetDrawable();
            case "bitmap":
                return new BitmapDrawable();
            case "nine-patch":
                return new NinePatchDrawable();
            case "animated-image":
                return new AnimatedImageDrawable();
            default:
                return null;
        }
    }
```

从文件流中创建Drawable
decodeImageDrawable
frameworks/base/core/java/android/content/res/ResourcesImpl.java
```
 private Drawable decodeImageDrawable(@NonNull AssetInputStream ais,
            @NonNull Resources wrapper, @NonNull TypedValue value) {
        ImageDecoder.Source src = new ImageDecoder.AssetInputStreamSource(ais,
                            wrapper, value);
        try {
            return ImageDecoder.decodeDrawable(src, (decoder, info, s) -> {
                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
            });
      ....
    }
```
decodeDrawable-> decodeDrawableImpl
frameworks/base/graphics/java/android/graphics/ImageDecoder.java
```
    private static Drawable decodeDrawableImpl(@NonNull Source src,
            @Nullable OnHeaderDecodedListener listener) throws IOException {
    ... //带动画的drawable
    if (decoder.mAnimated) {
                ImageDecoder postProcessPtr = decoder.mPostProcessor == null ?
                        null : decoder;
                decoder.checkState(true);
                Drawable d = new AnimatedImageDrawable(decoder.mNativePtr,
                        postProcessPtr, decoder.mDesiredWidth,
                        decoder.mDesiredHeight, decoder.getColorSpacePtr(),
                        decoder.checkForExtended(), srcDensity,
                        src.computeDstDensity(), decoder.mCropRect,
                        decoder.mInputStream, decoder.mAssetFd);
                // d has taken ownership of these objects.
                decoder.mInputStream = null;
                decoder.mAssetFd = null;
                return d;
            } 
      //普通的bitmap，后面转为Drawable返回                
      Bitmap bm = decoder.decodeBitmapInternal();
      bm.setDensity(srcDensity); 
      ... 如果是点九图，返回 NinePatchDrawable
      return new BitmapDrawable(res, bm);        
 }
 
   private Bitmap decodeBitmapInternal() throws IOException {
        checkState(false);
        return nDecodeBitmap(mNativePtr, this, mPostProcessor != null,
                mDesiredWidth, mDesiredHeight, mCropRect,
                mMutable, mAllocator, mUnpremultipliedRequired,
                mConserveMemory, mDecodeAsAlphaMask, getColorSpacePtr(),
                checkForExtended());
    }
```
android 12
frameworks/base/libs/hwui/jni/ImageDecoder.cpp
```
static jobject ImageDecoder_nDecodeBitmap(JNIEnv* env, jobject /*clazz*/, jlong nativePtr,
                                          jobject jdecoder, jboolean jpostProcess,
                                          jint targetWidth, jint targetHeight, jobject jsubset,
                                          jboolean requireMutable, jint allocator,
                                          jboolean requireUnpremul, jboolean preferRamOverQuality,
                                          jboolean asAlphaMask, jlong colorSpaceHandle,
                                          jboolean extended) {
     //拿到解码器                                     
    auto* decoder = reinterpret_cast<ImageDecoder*>(nativePtr);
    //设置大小
       if (!decoder->setTargetSize(targetWidth, targetHeight)) {
        doThrowISE(env, "Could not scale to target size!");
        return nullptr;
    }
    ...
    SkImageInfo bitmapInfo = decoder->getOutputInfo();
      SkBitmap bm;
      ..
    //创建bitmap  
    if (!bm.setInfo(bitmapInfo)) {
        doThrowIOE(env, "Failed to setInfo properly");
        return nullptr;
    }

    sk_sp<Bitmap> nativeBitmap;
    if (allocator == kSharedMemory_Allocator) {
        nativeBitmap = Bitmap::allocateAshmemBitmap(&bm);
    } else {
        nativeBitmap = Bitmap::allocateHeapBitmap(&bm);
    }
    ... 
    //解码
    SkCodec::Result result = decoder->decode(bm.getPixels(), bm.rowBytes());
    .... 没有进行缩放
   return bitmap::createBitmap(env, nativeBitmap.release(), bitmapCreateFlags, ninePatchChunk,
                                ninePatchInsets);  
 }
```
 测试加载resoure和加载bitmap的大小
feature/src/main/java/sst/example/androiddemo/feature/graphics/BitmapActivity.java
经测试android12 红米k30pro
图片放在mipmap不同目录，大小不发生变化
图片放在drawable不同目录，大小会发生缩放

//todo
android8.0后 mipmap解析为AdaptiveIconDrawable

