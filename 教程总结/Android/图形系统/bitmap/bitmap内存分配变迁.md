https://www.jianshu.com/p/d5714e8987f3
2.3之前的像素存储需要的内存是在native上分配的，并且生命周期不太可控，可能需要用户自己回收。 2.3-7.1之间，Bitmap的像素存储在Dalvik的Java堆上，
当然，4.4之前的甚至能在匿名共享内存上分配（Fresco采用），而8.0之后的像素内存又重新回到native上去分配，不需要用户主动回收，
8.0之后图像资源的管理更加优秀，极大降低了OOM
内存回收相关：
在 Android Android 2.2（API 级别 8）及更低版本上，当发生垃圾回收时，应用的线程会停止。这会导致延迟，从而降低性能。
Android 2.3 添加了并发垃圾回收功能，这意味着系统不再引用位图后，很快就会回收内存

官方文档https://developer.android.com/topic/performance/graphics/manage-memory
在 Android 3.0 及更高版本上管理内存
Android 3.0（API 级别 11）引入了 BitmapFactory.Options.inBitmap 字段。如果设置了此选项，那么采用 Options 对象的解码方法会在加载内容时尝试重复使用现有位图。
这意味着位图的内存得到了重复使用，从而提高了性能，同时移除了内存分配和取消分配。不过，inBitmap 的使用方式存在某些限制。
特别是在 Android 4.4（API 级别 19）之前，系统仅支持大小相同的位图


图片加载
android 8.0
Bitmap加载    decodeResource没有开辟单独的线程，使用时需要注意
BitmapFactory.java
```
  public static Bitmap decodeResource(Resources res, int id) {
        return decodeResource(res, id, null);
    }
 public static Bitmap decodeResource(Resources res, int id, Options opts) {
        validate(opts);
        Bitmap bm = null;
        InputStream is = null; 
        
        try {
            final TypedValue value = new TypedValue();
            /**
              * 根据指定的id打开数据流读取资源，同时为TypeValue进行复制获取原始资源的density等信息
              * 如果图片在drawable-xxhdpi，那么density为480dpi
              //   DENSITY_XXXHIGH = 640;
              //   DENSITY_XHIGH = 320;
              //   DENSITY_HIGH = 240;
              //   DENSITY_MEDIUM = 160;DENSITY_DEFAULT=160
            is = res.openRawResource(id, value);
            
            //按照输入流生成Bitmap
            bm = decodeResourceStream(res, value, is, null, opts);
        } catch (Exception e) {
           ...
        } finally {
           ...
        }
        ....
        return bm;
    }
    
    
public static Bitmap decodeResourceStream(@Nullable Resources res, @Nullable TypedValue value,
            @Nullable InputStream is, @Nullable Rect pad, @Nullable Options opts) {
        validate(opts);
        if (opts == null) {
            opts = new Options();
        }

        //如果没有设置inDensity，资源文件夹所表示的density设置inDensity 默认为160
        if (opts.inDensity == 0 && value != null) {
            final int density = value.density;
            if (density == TypedValue.DENSITY_DEFAULT) {
                opts.inDensity = DisplayMetrics.DENSITY_DEFAULT;
            } else if (density != TypedValue.DENSITY_NONE) {
                opts.inDensity = density;
            }
        }
        
         // 同理，也可以通过BitmapFactory.Option对象设置inTargetDensity
        //inTargetDensity 表示densityDpi，也就是手机的density
        //使用DisplayMetrics对象.densityDpi获得
        if (opts.inTargetDensity == 0 && res != null) {
            opts.inTargetDensity = res.getDisplayMetrics().densityDpi;
        }
        
        return decodeStream(is, pad, opts);
    }    
```
设置完 inDensity 和 inTargetDensity 之后调用了 decodeStream() 方法，该方法返回完全解码后的 Bitmap 对象，具体如下：
```
public static Bitmap decodeStream(@Nullable InputStream is, @Nullable Rect outPadding,
            @Nullable Options opts) {
        // we don't throw in this case, thus allowing the caller to only check
        // the cache, and not force the image to be decoded.
        if (is == null) {
            return null;
        }
        validate(opts);

        Bitmap bm = null;

        Trace.traceBegin(Trace.TRACE_TAG_GRAPHICS, "decodeBitmap");
        try {
            if (is instanceof AssetManager.AssetInputStream) {
                final long asset = ((AssetManager.AssetInputStream) is).getNativeAsset();
                bm = nativeDecodeAsset(asset, outPadding, opts, Options.nativeInBitmap(opts),
                    Options.nativeColorSpace(opts));
            } else {
                //调用了native方法：nativeDecodeStream(is, tempStorage, outPadding, opts);
                bm = decodeStreamInternal(is, outPadding, opts);
            }

            if (bm == null && opts != null && opts.inBitmap != null) {
                throw new IllegalArgumentException("Problem decoding into existing bitmap");
            }
            //根据Options设置最新解码的Bitmap 
            setDensityFromOptions(bm, opts);
        } finally {
            Trace.traceEnd(Trace.TRACE_TAG_GRAPHICS);
        }

        return bm;
    }
    
 private static Bitmap decodeStreamInternal(@NonNull InputStream is,
            @Nullable Rect outPadding, @Nullable Options opts) {
        // ASSERT(is != null);
        byte [] tempStorage = null;
        //设置默认的inTempStorage 16 * 1024=16KB   用于解码的临时空间,但应该足够大，以避免太多小的调用到is.read()
        if (opts != null) tempStorage = opts.inTempStorage;
        if (tempStorage == null) tempStorage = new byte[DECODE_BUFFER_SIZE];
        return nativeDecodeStream(is, tempStorage, outPadding, opts,
                Options.nativeInBitmap(opts),
                Options.nativeColorSpace(opts));
    }    
```
显然，decodeStream() 方法主要调用了本地方法完成 Bitmap 的解码，跟踪源码发现 nativeDecodeAsset() 和 nativeDecodeStream() 方法
都调用了 dodecode() 方法，doDecode 方法关键代码如下：
/frameworks/base/core/jni/android/graphics/BitmapFactory.cpp
```
 static jobject nativeDecodeStream(JNIEnv* env, jobject clazz, jobject is, jbyteArray storage,
          jobject padding, jobject options) {
      jobject bitmap = NULL;
      //流转换  //todo CreateJavaInputStreamAdaptor
      std::unique_ptr<SkStream> stream(CreateJavaInputStreamAdaptor(env, is, storage));  
      if (stream.get()) {
          std::unique_ptr<SkStreamRewindable> bufferedStream(
                  SkFrontBufferedStream::Create(stream.release(), SkCodec::MinBufferedBytesNeeded()));
          SkASSERT(bufferedStream.get() != NULL);
          bitmap = doDecode(env, bufferedStream.release(), padding, options);
      }
      return bitmap;
  }
  
```
看一下doDecode
```
static jobject doDecode(JNIEnv* env, SkStreamRewindable* stream, jobject padding, jobject options) {
    // This function takes ownership of the input stream.  Since the SkAndroidCodec
    // will take ownership of the stream, we don't necessarily need to take ownership
    // here.  This is a precaution - if we were to return before creating the codec,
    // we need to make sure that we delete the stream.
    std::unique_ptr<SkStreamRewindable> streamDeleter(stream);

    // Set default values for the options parameters.
    int sampleSize = 1;
    bool onlyDecodeSize = false;
    SkColorType prefColorType = kN32_SkColorType;
    bool isHardware = false;
    bool isMutable = false;
    float scale = 1.0f;
    bool requireUnpremultiplied = false;
    //Java设置的Options.inBitmap
    jobject javaBitmap = NULL;
    sk_sp<SkColorSpace> prefColorSpace = nullptr;

    // Update with options supplied by the client.
    if (options != NULL) {
        sampleSize = env->GetIntField(options, gOptions_sampleSizeFieldID);
        // Correct a non-positive sampleSize.  sampleSize defaults to zero within the
        // options object, which is strange.
        if (sampleSize <= 0) {
            sampleSize = 1;
        }
        //只解码大小，设置了options.inJustDecodeBounds
        if (env->GetBooleanField(options, gOptions_justBoundsFieldID)) {
            onlyDecodeSize = true;
        }

        //初始化Options，防止失败
        // initialize these, in case we fail later on
        env->SetIntField(options, gOptions_widthFieldID, -1);
        env->SetIntField(options, gOptions_heightFieldID, -1);
        env->SetObjectField(options, gOptions_mimeFieldID, 0);
        env->SetObjectField(options, gOptions_outConfigFieldID, 0);
        env->SetObjectField(options, gOptions_outColorSpaceFieldID, 0);
        
        //获取options中的参数
        jobject jconfig = env->GetObjectField(options, gOptions_configFieldID);
        prefColorType = GraphicsJNI::getNativeBitmapColorType(env, jconfig);
        jobject jcolorSpace = env->GetObjectField(options, gOptions_colorSpaceFieldID);
        prefColorSpace = GraphicsJNI::getNativeColorSpace(env, jcolorSpace);
        isHardware = GraphicsJNI::isHardwareConfig(env, jconfig);
        isMutable = env->GetBooleanField(options, gOptions_mutableFieldID);
        requireUnpremultiplied = !env->GetBooleanField(options, gOptions_premultipliedFieldID);
        javaBitmap = env->GetObjectField(options, gOptions_bitmapFieldID);

        if (env->GetBooleanField(options, gOptions_scaledFieldID)) {
            const int density = env->GetIntField(options, gOptions_densityFieldID);
            const int targetDensity = env->GetIntField(options, gOptions_targetDensityFieldID);
            const int screenDensity = env->GetIntField(options, gOptions_screenDensityFieldID);
            if (density != 0 && targetDensity != 0 && density != screenDensity) {
                //计算scale
                scale = (float) targetDensity / density;
            }
        }
    }

    if (isMutable && isHardware) {
        doThrowIAE(env, "Bitmaps with Config.HARWARE are always immutable");
        return nullObjectReturn("Cannot create mutable hardware bitmap");
    }

    // Create the codec.
    NinePatchPeeker peeker;
    //解码器
    std::unique_ptr<SkAndroidCodec> codec(SkAndroidCodec::NewFromStream(
            streamDeleter.release(), &peeker));
    if (!codec.get()) {
        return nullObjectReturn("SkAndroidCodec::NewFromStream returned null");
    }

    // Do not allow ninepatch decodes to 565.  In the past, decodes to 565
    // would dither, and we do not want to pre-dither ninepatches, since we
    // know that they will be stretched.  We no longer dither 565 decodes,
    // but we continue to prevent ninepatches from decoding to 565, in order
    // to maintain the old behavior.
    if (peeker.mPatch && kRGB_565_SkColorType == prefColorType) {
        prefColorType = kN32_SkColorType;
    }

    //获取bitmap大小
    // Determine the output size.  //todo skia相关
    SkISize size = codec->getSampledDimensions(sampleSize);

    int scaledWidth = size.width();
    int scaledHeight = size.height();
    bool willScale = false;

    // Apply a fine scaling step if necessary.
    //采样的影响
    if (needsFineScale(codec->getInfo().dimensions(), size, sampleSize)) {
        willScale = true;
        scaledWidth = codec->getInfo().width() / sampleSize;
        scaledHeight = codec->getInfo().height() / sampleSize;
    }

    //获取colorType
    // Set the decode colorType
    SkColorType decodeColorType = codec->computeOutputColorType(prefColorType);
    sk_sp<SkColorSpace> decodeColorSpace = codec->computeOutputColorSpace(
            decodeColorType, prefColorSpace);

    // Set the options and return if the client only wants the size.
    if (options != NULL) {
        jstring mimeType = encodedFormatToString(
                env, (SkEncodedImageFormat)codec->getEncodedFormat());
        if (env->ExceptionCheck()) {
            return nullObjectReturn("OOM in encodedFormatToString()");
        }
        //更新Java层options  width，height，mimeType，outColorSpace，outConfig
        env->SetIntField(options, gOptions_widthFieldID, scaledWidth);
        env->SetIntField(options, gOptions_heightFieldID, scaledHeight);
        env->SetObjectField(options, gOptions_mimeFieldID, mimeType);

        SkColorType outColorType = decodeColorType;
        // Scaling can affect the output color type
        if (willScale || scale != 1.0f) {
            outColorType = colorTypeForScaledOutput(outColorType);
        }

        jint configID = GraphicsJNI::colorTypeToLegacyBitmapConfig(outColorType);
        if (isHardware) {
            configID = GraphicsJNI::kHardware_LegacyBitmapConfig;
        }
        jobject config = env->CallStaticObjectMethod(gBitmapConfig_class,
                gBitmapConfig_nativeToConfigMethodID, configID);
        //更新Java层的colorSpace        
        env->SetObjectField(options, gOptions_outConfigFieldID, config);

        env->SetObjectField(options, gOptions_outColorSpaceFieldID,
                GraphicsJNI::getColorSpace(env, decodeColorSpace, decodeColorType));
        //如果只解码大小，直接返回
        if (onlyDecodeSize) {
            return nullptr;
        }
    }

    // Scale is necessary due to density differences.
    if (scale != 1.0f) {
        //对宽高进行缩放
        willScale = true;
        scaledWidth = static_cast<int>(scaledWidth * scale + 0.5f);
        scaledHeight = static_cast<int>(scaledHeight * scale + 0.5f);
    }

    android::Bitmap* reuseBitmap = nullptr;
    unsigned int existingBufferSize = 0;
    if (javaBitmap != NULL) {
        //将javaBitmap转为reuseBitmap
        reuseBitmap = &bitmap::toBitmap(env, javaBitmap);
        if (reuseBitmap->isImmutable()) {
            ALOGW("Unable to reuse an immutable bitmap as an image decoder target.");
            javaBitmap = NULL;
            reuseBitmap = nullptr;
        } else {
            existingBufferSize = bitmap::getBitmapAllocationByteCount(env, javaBitmap);
        }
    }
    //todo HeapAllocator
    HeapAllocator defaultAllocator;
    //根据reuseBitmap初始化RecyclingPixelAllocator  RecyclingPixelAllocator复用bitmap，并对其进行配置
    RecyclingPixelAllocator recyclingAllocator(reuseBitmap, existingBufferSize);
    //ScaleCheckingAllocator对大小进行检查，如果复用的bitmap小于目标大小，不分配内存，返回false
    ScaleCheckingAllocator scaleCheckingAllocator(scale, existingBufferSize);
    SkBitmap::HeapAllocator heapAllocator;
    //最终的内存分配器
    SkBitmap::Allocator* decodeAllocator;
    if (javaBitmap != nullptr && willScale) {
        // This will allocate pixels using a HeapAllocator, since there will be an extra
        // scaling step that copies these pixels into Java memory.  This allocator
        // also checks that the recycled javaBitmap is large enough.
        decodeAllocator = &scaleCheckingAllocator;
    } else if (javaBitmap != nullptr) {
        decodeAllocator = &recyclingAllocator;
    } else if (willScale || isHardware) {
        // This will allocate pixels using a HeapAllocator,
        // for scale case: there will be an extra scaling step.
        // for hardware case: there will be extra swizzling & upload to gralloc step.
        decodeAllocator = &heapAllocator;
    } else {
        decodeAllocator = &defaultAllocator;
    }

    // Construct a color table for the decode if necessary
    sk_sp<SkColorTable> colorTable(nullptr);
    SkPMColor* colorPtr = nullptr;
    int* colorCount = nullptr;
    int maxColors = 256;
    SkPMColor colors[256];
    //todo colortype相关
    if (kIndex_8_SkColorType == decodeColorType) {
        colorTable.reset(new SkColorTable(colors, maxColors));

        // SkColorTable expects us to initialize all of the colors before creating an
        // SkColorTable.  However, we are using SkBitmap with an Allocator to allocate
        // memory for the decode, so we need to create the SkColorTable before decoding.
        // It is safe for SkAndroidCodec to modify the colors because this SkBitmap is
        // not being used elsewhere.
        colorPtr = const_cast<SkPMColor*>(colorTable->readColors());
        colorCount = &maxColors;
    }

    SkAlphaType alphaType = codec->computeOutputAlphaType(requireUnpremultiplied);

    const SkImageInfo decodeInfo = SkImageInfo::Make(size.width(), size.height(),
            decodeColorType, alphaType, decodeColorSpace);

    // For wide gamut images, we will leave the color space on the SkBitmap.  Otherwise,
    // use the default.
    SkImageInfo bitmapInfo = decodeInfo;
    if (decodeInfo.colorSpace() && decodeInfo.colorSpace()->isSRGB()) {
        bitmapInfo = bitmapInfo.makeColorSpace(GraphicsJNI::colorSpaceForType(decodeColorType));
    }

    if (decodeColorType == kGray_8_SkColorType) {
        // The legacy implementation of BitmapFactory used kAlpha8 for
        // grayscale images (before kGray8 existed).  While the codec
        // recognizes kGray8, we need to decode into a kAlpha8 bitmap
        // in order to avoid a behavior change.
        bitmapInfo =
                bitmapInfo.makeColorType(kAlpha_8_SkColorType).makeAlphaType(kPremul_SkAlphaType);
    }
    SkBitmap decodingBitmap;
    if (!decodingBitmap.setInfo(bitmapInfo) ||
            !decodingBitmap.tryAllocPixels(decodeAllocator, colorTable.get())) {
        // SkAndroidCodec should recommend a valid SkImageInfo, so setInfo()
        // should only only fail if the calculated value for rowBytes is too
        // large.
        // tryAllocPixels() can fail due to OOM on the Java heap, OOM on the
        // native heap, or the recycled javaBitmap being too small to reuse.
        return nullptr;
    }

    // Use SkAndroidCodec to perform the decode.
    SkAndroidCodec::AndroidOptions codecOptions;
    codecOptions.fZeroInitialized = decodeAllocator == &defaultAllocator ?
            SkCodec::kYes_ZeroInitialized : SkCodec::kNo_ZeroInitialized;
    codecOptions.fColorPtr = colorPtr;
    codecOptions.fColorCount = colorCount;
    codecOptions.fSampleSize = sampleSize;
    //解码信息放到decodingBitmap
    SkCodec::Result result = codec->getAndroidPixels(decodeInfo, decodingBitmap.getPixels(),
            decodingBitmap.rowBytes(), &codecOptions);
    switch (result) {
        case SkCodec::kSuccess:
        case SkCodec::kIncompleteInput:
            break;
        default:
            return nullObjectReturn("codec->getAndroidPixels() failed.");
    }

    jbyteArray ninePatchChunk = NULL;
    if (peeker.mPatch != NULL) {
        if (willScale) {
            scaleNinePatchChunk(peeker.mPatch, scale, scaledWidth, scaledHeight);
        }

        size_t ninePatchArraySize = peeker.mPatch->serializedSize();
        ninePatchChunk = env->NewByteArray(ninePatchArraySize);
        if (ninePatchChunk == NULL) {
            return nullObjectReturn("ninePatchChunk == null");
        }

        jbyte* array = (jbyte*) env->GetPrimitiveArrayCritical(ninePatchChunk, NULL);
        if (array == NULL) {
            return nullObjectReturn("primitive array == null");
        }

        memcpy(array, peeker.mPatch, peeker.mPatchSize);
        env->ReleasePrimitiveArrayCritical(ninePatchChunk, array, 0);
    }

    jobject ninePatchInsets = NULL;
    if (peeker.mHasInsets) {
        ninePatchInsets = env->NewObject(gInsetStruct_class, gInsetStruct_constructorMethodID,
                peeker.mOpticalInsets[0], peeker.mOpticalInsets[1],
                peeker.mOpticalInsets[2], peeker.mOpticalInsets[3],
                peeker.mOutlineInsets[0], peeker.mOutlineInsets[1],
                peeker.mOutlineInsets[2], peeker.mOutlineInsets[3],
                peeker.mOutlineRadius, peeker.mOutlineAlpha, scale);
        if (ninePatchInsets == NULL) {
            return nullObjectReturn("nine patch insets == null");
        }
        if (javaBitmap != NULL) {
            env->SetObjectField(javaBitmap, gBitmap_ninePatchInsetsFieldID, ninePatchInsets);
        }
    }

    SkBitmap outputBitmap;
    if (willScale) {
        // This is weird so let me explain: we could use the scale parameter
        // directly, but for historical reasons this is how the corresponding
        // Dalvik code has always behaved. We simply recreate the behavior here.
        // The result is slightly different from simply using scale because of
        // the 0.5f rounding bias applied when computing the target image size
        //缩放比
        const float sx = scaledWidth / float(decodingBitmap.width());
        const float sy = scaledHeight / float(decodingBitmap.height());

        // Set the allocator for the outputBitmap.
        SkBitmap::Allocator* outputAllocator;
        if (javaBitmap != nullptr) {
            outputAllocator = &recyclingAllocator;
        } else {
            outputAllocator = &defaultAllocator;
        }

        SkColorType scaledColorType = colorTypeForScaledOutput(decodingBitmap.colorType());
        // FIXME: If the alphaType is kUnpremul and the image has alpha, the
        // colors may not be correct, since Skia does not yet support drawing
        // to/from unpremultiplied bitmaps.
        outputBitmap.setInfo(
                bitmapInfo.makeWH(scaledWidth, scaledHeight).makeColorType(scaledColorType));
        if (!outputBitmap.tryAllocPixels(outputAllocator, NULL)) {
            // This should only fail on OOM.  The recyclingAllocator should have
            // enough memory since we check this before decoding using the
            // scaleCheckingAllocator.
            return nullObjectReturn("allocation failed for scaled bitmap");
        }

        SkPaint paint;
        // kSrc_Mode instructs us to overwrite the uninitialized pixels in
        // outputBitmap.  Otherwise we would blend by default, which is not
        // what we want.
        paint.setBlendMode(SkBlendMode::kSrc);
        paint.setFilterQuality(kLow_SkFilterQuality); // bilinear filtering

        SkCanvas canvas(outputBitmap, SkCanvas::ColorBehavior::kLegacy);
        //将canvas放大scale，然后绘制Bitmap
        canvas.scale(sx, sy);
        canvas.drawBitmap(decodingBitmap, 0.0f, 0.0f, &paint);
    } else {
        outputBitmap.swap(decodingBitmap);
    }

    if (padding) {
        if (peeker.mPatch != NULL) {
            GraphicsJNI::set_jrect(env, padding,
                    peeker.mPatch->paddingLeft, peeker.mPatch->paddingTop,
                    peeker.mPatch->paddingRight, peeker.mPatch->paddingBottom);
        } else {
            GraphicsJNI::set_jrect(env, padding, -1, -1, -1, -1);
        }
    }

    // If we get here, the outputBitmap should have an installed pixelref.
    if (outputBitmap.pixelRef() == NULL) {
        return nullObjectReturn("Got null SkPixelRef");
    }

    //设置为不可变更
    if (!isMutable && javaBitmap == NULL) {
        // promise we will never change our pixels (great for sharing and pictures)
        outputBitmap.setImmutable();
    }

    bool isPremultiplied = !requireUnpremultiplied;
    //如果设置了Options.inBitmap，对bitmap进行重新初始化，然后返回
    if (javaBitmap != nullptr) {
        bitmap::reinitBitmap(env, javaBitmap, outputBitmap.info(), isPremultiplied);
        outputBitmap.notifyPixelsChanged();
        return javaBitmap;
    }

    int bitmapCreateFlags = 0x0;
    if (isMutable) bitmapCreateFlags |= android::bitmap::kBitmapCreateFlag_Mutable;
    if (isPremultiplied) bitmapCreateFlags |= android::bitmap::kBitmapCreateFlag_Premultiplied;

    if (isHardware) {
        sk_sp<Bitmap> hardwareBitmap = Bitmap::allocateHardwareBitmap(outputBitmap);
        return bitmap::createBitmap(env, hardwareBitmap.release(), bitmapCreateFlags,
                ninePatchChunk, ninePatchInsets, -1);
    }
     //调用Bitmap的Java层构造器并返回
    // now create the java bitmap
    return bitmap::createBitmap(env, defaultAllocator.getStorageObjAndReset(),
            bitmapCreateFlags, ninePatchChunk, ninePatchInsets, -1);
}
```



8.0之前是直接摘录的https://www.jianshu.com/p/d5714e8987f3
8.0之前Bitmap内存分配原理
其实，通过Bitmap的成员列表，就能看出一点眉目，Bitmap中有个byte[] mBuffer，其实就是用来存储像素数据的，很明显它位于java heap中
```
public final class Bitmap implements Parcelable {
     ...
    private byte[] mBuffer;
     ...
    }
```
接下来，通过手动创建Bitmap，进行分析：Bitmap.java
```
public static Bitmap createBitmap(int width, int height, Config config) {
    return createBitmap(width, height, config, true);
}
```
Java层Bitmap的创建最终还是会走向native层：Bitmap.cpp
```
 static jobject Bitmap_creator(JNIEnv* env, jobject, jintArray jColors,
                               jint offset, jint stride, jint width, jint height,
                               jint configHandle, jboolean isMutable) {
     SkColorType colorType = GraphicsJNI::legacyBitmapConfigToColorType(configHandle);
      ... 
 
     SkBitmap Bitmap;
     Bitmap.setInfo(SkImageInfo::Make(width, height, colorType, kPremul_SkAlphaType));
        <!--关键点1 像素内存分配-->
     Bitmap* nativeBitmap = GraphicsJNI::allocateJavaPixelRef(env, &Bitmap, NULL);
     if (!nativeBitmap) {
         return NULL;
     }
      ... 
     <!--获取分配地址-->
     jbyte* addr = (jbyte*) env->CallLongMethod(gVMRuntime, gVMRuntime_addressOf, arrayObj);
     ...
     <!--创建Bitmap-->
     android::Bitmap* wrapper = new android::Bitmap(env, arrayObj, (void*) addr,
             info, rowBytes, ctable);
     wrapper->getSkBitmap(Bitmap);
     Bitmap->lockPixels();
     return wrapper;
 }
```
这里只看关键点1，像素内存的分配：GraphicsJNI::allocateJavaPixelRef从这个函数名可以就可以看出，是在Java层分配，跟进去，也确实如此：
```
android::Bitmap* GraphicsJNI::allocateJavaPixelRef(JNIEnv* env, SkBitmap* bitmap,
                                             SkColorTable* ctable) {
    const SkImageInfo& info = bitmap->info();
    if (info.fColorType == kUnknown_SkColorType) {
        doThrowIAE(env, "unknown bitmap configuration");
        return NULL;
    }

    size_t size;
    if (!computeAllocationSize(*bitmap, &size)) {
        return NULL;
    }

    // we must respect the rowBytes value already set on the bitmap instead of
    // attempting to compute our own.
    const size_t rowBytes = bitmap->rowBytes();
   <!--关键点1 ，创建Java层字节数据，作为数据存储单元-->
    jbyteArray arrayObj = (jbyteArray) env->CallObjectMethod(gVMRuntime,
                                                             gVMRuntime_newNonMovableArray,
                                                             gByte_class, size);
    if (env->ExceptionCheck() != 0) {
        return NULL;
    }
    SkASSERT(arrayObj);
    jbyte* addr = (jbyte*) env->CallLongMethod(gVMRuntime, gVMRuntime_addressOf, arrayObj);
    if (env->ExceptionCheck() != 0) {
        return NULL;
    }
    SkASSERT(addr);
    android::Bitmap* wrapper = new android::Bitmap(env, arrayObj, (void*) addr,
            info, rowBytes, ctable);
    wrapper->getSkBitmap(bitmap);
    // since we're already allocated, we lockPixels right away
    // HeapAllocator behaves this way too
    bitmap->lockPixels();

    return wrapper;
}
```
由于只关心内存分配，同样只看关键点1，这里其实就是在native层创建Java层byte[]，并将这个byte[]作为像素存储结构，
之后再通过在native层构建Java Bitmap对象的方式，将生成的byte[]传递给Bitmap.java对象：
```
jobject GraphicsJNI::createBitmap(JNIEnv* env, android::Bitmap* bitmap,
        int bitmapCreateFlags, jbyteArray ninePatchChunk, jobject ninePatchInsets,
        int density) {
    ...<!--关键点1，构建java Bitmap对象，并设置byte[] mBuffer-->
    jobject obj = env->NewObject(gBitmap_class, gBitmap_constructorMethodID,
            reinterpret_cast<jlong>(bitmap), bitmap->javaByteArray(),
            bitmap->width(), bitmap->height(), density, isMutable, isPremultiplied,
            ninePatchChunk, ninePatchInsets);
    hasException(env); // For the side effect of logging.
    return obj;
}
```


android 8.0
Bitmap创建
```
public final class Bitmap implements Parcelable {
    //native引用
    private final long mNativePtr;
    //todo
    private NinePatch.InsetStruct mNinePatchInsets; // may be null
    private ColorSpace mColorSpace;
    
    Bitmap(long nativeBitmap, int width, int height, int density,
            boolean requestPremultiplied, byte[] ninePatchChunk,
            NinePatch.InsetStruct ninePatchInsets) {
        this(nativeBitmap, width, height, density, requestPremultiplied, ninePatchChunk,
                ninePatchInsets, true);
    }

    // called from JNI and Bitmap_Delegate.
    Bitmap(long nativeBitmap, int width, int height, int density,
            boolean requestPremultiplied, byte[] ninePatchChunk,
            NinePatch.InsetStruct ninePatchInsets, boolean fromMalloc) {
        ...
        mWidth = width;
        mHeight = height;
        mRequestPremultiplied = requestPremultiplied;

        mNinePatchChunk = ninePatchChunk;
        mNinePatchInsets = ninePatchInsets;
        if (density >= 0) {
            mDensity = density;
        }

        mNativePtr = nativeBitmap;

        final int allocationByteCount = getAllocationByteCount();
        NativeAllocationRegistry registry;
        // Android 8.0引入的一种辅助自动回收native内存的一种机制，当Java对象因为GC被回收后，
        //NativeAllocationRegistry可以辅助回收Java对象所申请的native内存
        if (fromMalloc) {
            registry = NativeAllocationRegistry.createMalloced(
                    Bitmap.class.getClassLoader(), nativeGetNativeFinalizer(), allocationByteCount);
        } else {
            registry = NativeAllocationRegistry.createNonmalloced(
                    Bitmap.class.getClassLoader(), nativeGetNativeFinalizer(), allocationByteCount);
        }
        registry.registerNativeAllocation(this, nativeBitmap);

        if (ResourcesImpl.TRACE_FOR_DETAILED_PRELOAD) {
            sPreloadTracingNumInstantiatedBitmaps++;
            long nativeSize = NATIVE_ALLOCATION_SIZE + allocationByteCount;
            sPreloadTracingTotalBitmapsSize += nativeSize;
        }
    }
    
    //createBitmap多个重写方法
    public static Bitmap createBitmap(@NonNull Bitmap src) {
        return createBitmap(src, 0, 0, src.getWidth(), src.getHeight());
    }
    
    public static Bitmap createBitmap(@NonNull Bitmap source, int x, int y, int width, int height) {
        return createBitmap(source, x, y, width, height, null, false);
    }
    
    public static Bitmap createBitmap(@NonNull Bitmap source, int x, int y, int width, int height,
            @Nullable Matrix m, boolean filter) {

        checkXYSign(x, y);
        checkWidthHeight(width, height);
        ...
        
        // check if we can just return our argument unchanged
        if (!source.isMutable() && x == 0 && y == 0 && width == source.getWidth() &&
                height == source.getHeight() && (m == null || m.isIdentity())) {
            return source;
        }

        boolean isHardware = source.getConfig() == Config.HARDWARE;
        if (isHardware) {
            source.noteHardwareBitmapSlowCall();
            source = nativeCopyPreserveInternalConfig(source.mNativePtr);
        }

        int neww = width;
        int newh = height;
        Bitmap bitmap;
        Paint paint;

        Rect srcR = new Rect(x, y, x + width, y + height);
        RectF dstR = new RectF(0, 0, width, height);
        RectF deviceR = new RectF();
        //设置config 
        Config newConfig = Config.ARGB_8888;
        final Config config = source.getConfig();
        // GIF files generate null configs, assume ARGB_8888
        if (config != null) {
            switch (config) {
                case RGB_565:
                    newConfig = Config.RGB_565;
                    break;
                case ALPHA_8:
                    newConfig = Config.ALPHA_8;
                    break;
                case RGBA_F16:
                    newConfig = Config.RGBA_F16;
                    break;
                //noinspection deprecation
                case ARGB_4444:
                case ARGB_8888:
                default:
                    newConfig = Config.ARGB_8888;
                    break;
            }
        }

        ColorSpace cs = source.getColorSpace();
        //应用的matrix为null
        if (m == null || m.isIdentity()) {
            bitmap = createBitmap(null, neww, newh, newConfig, source.hasAlpha(), cs);
            paint = null;   // not needed
        } else {
            final boolean transformed = !m.rectStaysRect();

            m.mapRect(deviceR, dstR);

            neww = Math.round(deviceR.width());
            newh = Math.round(deviceR.height());

            Config transformedConfig = newConfig;
            if (transformed) {
                if (transformedConfig != Config.ARGB_8888 && transformedConfig != Config.RGBA_F16) {
                    transformedConfig = Config.ARGB_8888;
                    if (cs == null) {
                        cs = ColorSpace.get(ColorSpace.Named.SRGB);
                    }
                }
            }

            bitmap = createBitmap(null, neww, newh, transformedConfig,
                    transformed || source.hasAlpha(), cs);

            paint = new Paint();
            paint.setFilterBitmap(filter);
            if (transformed) {
                paint.setAntiAlias(true);
            }
        }

        // The new bitmap was created from a known bitmap source so assume that
        // they use the same density
        bitmap.mDensity = source.mDensity;
        bitmap.setHasAlpha(source.hasAlpha());
        bitmap.setPremultiplied(source.mRequestPremultiplied);
        //对新bitmap应用matrix
        Canvas canvas = new Canvas(bitmap);
        canvas.translate(-deviceR.left, -deviceR.top);
        canvas.concat(m);
        canvas.drawBitmap(source, srcR, dstR, paint);
        canvas.setBitmap(null);
        if (isHardware) {
            return bitmap.copy(Config.HARDWARE, false);
        }
        return bitmap;
    }
    
      public static Bitmap createBitmap(@Nullable DisplayMetrics display, int width, int height,
            @NonNull Config config, boolean hasAlpha, @NonNull ColorSpace colorSpace) {
        。。。
        //调用nativeCreate创建bitmap
        Bitmap bm = nativeCreate(null, 0, width, width, height, config.nativeInt, true,
                colorSpace == null ? 0 : colorSpace.getNativeInstance());

        if (display != null) {
            bm.mDensity = display.densityDpi;
        }
        bm.setHasAlpha(hasAlpha);
        if ((config == Config.ARGB_8888 || config == Config.RGBA_F16) && !hasAlpha) {
            nativeErase(bm.mNativePtr, 0xff000000);
        }
        // No need to initialize the bitmap to zeroes with other configs;
        // it is backed by a VM byte array which is by definition preinitialized
        // to all zeroes.
        return bm;
    }  
}
```
frameworks/base/core/jni/android/graphics/Bitmap.cpp
nativeCreate的c++方法是Bitmap_creator
```
static jobject Bitmap_creator(JNIEnv* env, jobject, jintArray jColors,
                              jint offset, jint stride, jint width, jint height,
                              jint configHandle, jboolean isMutable,
                              jfloatArray xyzD50, jobject transferParameters) {
    SkColorType colorType = GraphicsJNI::legacyBitmapConfigToColorType(configHandle);
    if (NULL != jColors) {
        size_t n = env->GetArrayLength(jColors);
        if (n < SkAbs32(stride) * (size_t)height) {
            doThrowAIOOBE(env);
            return NULL;
        }
    }
     //将ARGB_4444转为8888
    // ARGB_4444 is a deprecated format, convert automatically to 8888
    if (colorType == kARGB_4444_SkColorType) {
        colorType = kN32_SkColorType;
    }

    SkBitmap bitmap;
    sk_sp<SkColorSpace> colorSpace;

    if (colorType != kN32_SkColorType || xyzD50 == nullptr || transferParameters == nullptr) {
        colorSpace = GraphicsJNI::colorSpaceForType(colorType);
    } else {
        SkColorSpaceTransferFn p = GraphicsJNI::getNativeTransferParameters(env, transferParameters);
        SkMatrix44 xyzMatrix = GraphicsJNI::getNativeXYZMatrix(env, xyzD50);
        colorSpace = SkColorSpace::MakeRGB(p, xyzMatrix);
    }

    bitmap.setInfo(SkImageInfo::Make(width, height, colorType, kPremul_SkAlphaType, colorSpace));
    //native层创建bitmap，并分配native内存
    sk_sp<Bitmap> nativeBitmap = Bitmap::allocateHeapBitmap(&bitmap, NULL);
    if (!nativeBitmap) {
        ALOGE("OOM allocating Bitmap with dimensions %i x %i", width, height);
        doThrowOOME(env);
        return NULL;
    }

    if (jColors != NULL) {
        GraphicsJNI::SetPixels(env, jColors, offset, stride, 0, 0, width, height, bitmap);
    }
    //调用Bitmap的Java层构造器并返回
    return createBitmap(env, nativeBitmap.release(), getPremulBitmapCreateFlags(isMutable));
}

jobject createBitmap(JNIEnv* env, Bitmap* bitmap,
        int bitmapCreateFlags, jbyteArray ninePatchChunk, jobject ninePatchInsets,
        int density) {
    bool isMutable = bitmapCreateFlags & kBitmapCreateFlag_Mutable;
    bool isPremultiplied = bitmapCreateFlags & kBitmapCreateFlag_Premultiplied;
    // The caller needs to have already set the alpha type properly, so the
    // native SkBitmap stays in sync with the Java Bitmap.
    assert_premultiplied(bitmap->info(), isPremultiplied);
    BitmapWrapper* bitmapWrapper = new BitmapWrapper(bitmap);
    
    //调用Java层Bitmap构造器
    jobject obj = env->NewObject(gBitmap_class, gBitmap_constructorMethodID,
            reinterpret_cast<jlong>(bitmapWrapper), bitmap->width(), bitmap->height(), density,
            isMutable, isPremultiplied, ninePatchChunk, ninePatchInsets);

    if (env->ExceptionCheck() != 0) {
        ALOGE("*** Uncaught exception returned from Java call!\n");
        env->ExceptionDescribe();
    }
    return obj;
}
```
看一下native分配内存
frameworks/base/libs/hwui/hwui/Bitmap.cpp
```
sk_sp<Bitmap> Bitmap::allocateHeapBitmap(SkBitmap* bitmap) {
    return allocateBitmap(bitmap, &Bitmap::allocateHeapBitmap);
}
static sk_sp<Bitmap> allocateBitmap(SkBitmap* bitmap, AllocPixelRef alloc) {
    const SkImageInfo& info = bitmap->info();
    if (info.colorType() == kUnknown_SkColorType) {
        LOG_ALWAYS_FATAL("unknown bitmap configuration");
        return nullptr;
    }

    size_t size;

    // we must respect the rowBytes value already set on the bitmap instead of
    // attempting to compute our own.
    const size_t rowBytes = bitmap->rowBytes();
    if (!Bitmap::computeAllocationSize(rowBytes, bitmap->height(), &size)) {
        return nullptr;
    }
    //todo 直接alloc分配内存  auto wrapper
    auto wrapper = alloc(size, info, rowBytes);
    if (wrapper) {
        //创建native Bitmap
        wrapper->getSkBitmap(bitmap);
    }
    return wrapper;
}

void Bitmap::getSkBitmap(SkBitmap* outBitmap) {
#ifdef __ANDROID__ // Layoutlib does not support hardware acceleration
    if (isHardware()) {
        outBitmap->allocPixels(mInfo);
        uirenderer::renderthread::RenderProxy::copyHWBitmapInto(this, outBitmap);
        return;
    }
#endif
    outBitmap->setInfo(mInfo, rowBytes());
    outBitmap->setPixelRef(sk_ref_sp(this), 0, 0);
}
```