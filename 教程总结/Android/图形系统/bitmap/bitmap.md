

https://juejin.cn/post/6844904182126739470
跨进程传递大图
1 把图片存到 SD 卡，然后把路径传过去，在别的进程读出来    效率低
2 Bitmap 实现了 Parcelable 接口，可以通过 Intent.putExtra(String name, Parcelable value) 方法直接放 
   Intent 里面传递      文件大小限制
3 通过 AIDL 使用 Binder 进行 IPC 就不受这个限制
bundle.putBinder("binder", new IRemoteGetBitmap.Stub() {
@Override
public Bitmap getBitMap() throws RemoteException {
return mBitmap;
}
});
intent.putExtras(bundle);
使用
intent.getBundleExtra("bin").getBinder()

面试官：Intent 直接传 Bitmap 会有什么问题?
Bitmap 太大会抛 TransactionTooLargeException 异常，原因是：底层判断只要 Binder Transaction 失败，
且 Intent 的数据大于 200k 就会抛这个异常了。（见：android_util_Binder.cpp 文件 signalExceptionForError 方法）
面试官：为什么 Intent 传值会有大小限制。
应用进程在启动 Binder 机制时会映射一块 1M 大小的内存，所有正在进行的 Binder 事务共享这 1M 的缓冲区	。
当使用 Intent 进行 IPC 时申请的缓存超过 1M - 其他事务占用的内存时，就会申请失败抛
TransactionTooLargeException 异常了。
为什么通过 putBinder 的方式传 Bitmap 不会抛 TransactionTooLargeException 异常
```
Android - 28 Bitmap.cpp
static jboolean Bitmap_writeToParcel(JNIEnv* env, jobject, ...) {
    // 拿到 Native 的 Bitmap                                
    auto bitmapWrapper = reinterpret_cast<BitmapWrapper*>(bitmapHandle);
    // 拿到其对应的 SkBitmap, 用于获取 Bitmap 的像素信息
    bitmapWrapper->getSkBitmap(&bitmap);

    int fd = bitmapWrapper->bitmap().getAshmemFd();
    if (fd >= 0 && !isMutable && p->allowFds()) {
   	 		// Bitmap 带了 ashmemFd && Bitmap 不可修改 && Parcel 允许带 fd
    		// 就直接把 FD 写到 Parcel 里，结束。
        status = p->writeDupImmutableBlobFileDescriptor(fd);
        return JNI_TRUE;
    }

    // 不满足上面的条件就要把 Bitmap 拷贝到一块新的缓冲区
    android::Parcel::WritableBlob blob;
  	// 通过 writeBlob 拿到一块缓冲区 blob
    status = p->writeBlob(size, mutableCopy, &blob);

    // 获取像素信息并写到缓冲区
    const void* pSrc =  bitmap.getPixels();
    if (pSrc == NULL) {
        memset(blob.data(), 0, size);
    } else {
        memcpy(blob.data(), pSrc, size);
    }
}
```
```Android - 28 Parcel.cpp
// Maximum size of a blob to transfer in-place.
static const size_t BLOB_INPLACE_LIMIT = 16 * 1024;

status_t Parcel::writeBlob(size_t len, bool mutableCopy, WritableBlob* outBlob)
{
    if (!mAllowFds || len <= BLOB_INPLACE_LIMIT) {
    // 如果不允许带 fd ，或者这个数据小于 16K
    // 就直接在 Parcel 的缓冲区里分配一块空间来保存这个数据
        status = writeInt32(BLOB_INPLACE);
        void* ptr = writeInplace(len);
        outBlob->init(-1, ptr, len, false);
        return NO_ERROR;
    }

		// 另外开辟一个 ashmem，映射出一块内存，后续数据将保存在 ashmem 的内存里
    int fd = ashmem_create_region("Parcel Blob", len);
    void* ptr = ::mmap(NULL, len, PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);
    ...
  	// parcel 里只写个 fd 就好了，这样就算数据量很大，parcel 自己的缓冲区也不用很大
    status = writeFileDescriptor(fd, true /*takeOwnership*/);
 		outBlob->init(fd, ptr, len, mutableCopy);
    return status;
}

```
allowFds 是什么时候被设置成 false 的呢
```// 启动 Activity 执行到 Instrumentation.java 的这个方法
public ActivityResult execStartActivity(..., Intent intent, ...){
  ...
  intent.prepareToLeaveProcess(who);
	ActivityManager.getService().startActivity(...,intent,...)
}

// Intent.java
public void prepareToLeaveProcess(boolean leavingPackage) {
 // 这边一层层传递到最后设置 Parcel 的 allowfds
  setAllowFds(false);
  ....
}
```
Android的共享匿名内存需要在进程间通过binder传递fd,禁用后就没法使用了
总结一下：较大的 bitmap 直接通过 Intent 传递容易抛异常是因为 Intent 启动组件时，系统禁掉了文件描述符 fd 机制 ,
bitmap 无法利用共享内存，只能拷贝到 Binder 映射的缓冲区，导致缓冲区超限, 触发异常; 
而通过 putBinder 的方式，避免了 Intent 禁用描述符的影响，bitmap 写 parcel 时的 allowFds 默认是 true , 
可以利用共享内存，所以能高效传输图片

//todo https://www.jianshu.com/p/8e8ad414237e


Bitmap内存计算  可以看一下BitmapFactory.decodeResource，会根据density进行缩放
Bitmap 所占内存又如何计算呢，一般情况下这样计算：  

Bitmap Memory = widthPix * heightPix * 一个像素的大小   可使用 bitmap.getConfig() 获取 Bitmap 的格式然后计算像素，ARGB_8888是32位，也就是4字节

如果将图片放置在 Android 的资源文件夹中，计算方式如下：   可以看到是原来scale的四次方
```
scale = targetDensity / density

widthPix = originalWidth * scale

heightPix = orignalHeight * scale

Bitmap Memory = widthPix * scale * heightPix * scale * 一个像素大小
```
可以通过bitmap.getByteCount() 获取大小并验证 

inSampleSize在BitmapFactory最终设置为2的整次幂,文档注释有写
下采样  如果设定了inSampleSize，后续的内存发生变化    算上inSampleSize是原来的1/inSampleSize平方
```
inSampleSize = ？？   增加inSampleSize后的计算   一般是图片大小/view大小

scale = targetDensity / density

widthPix = originalWidth / inSampleSize * scale

heightPix = orignalHeight / inSampleSize * scale

Bitmap Memory = widthPix * scale * heightPix * scale * 一个像素大小
```
将一个分辨率为 2048 x 1536 的图像使用 inSampleSize 值为 4 去编码产生一个 512 x 384 的图像，这里假设位图配置为 ARGB_8888，
加载到内存中仅仅是 0.75M 而不是原来的 12M
2048 * 1536 *4 *1=12M  假设scale为1
2048 /4  * 1536 /4 *4 *1= 0.75M

一张大图 6000 x 4000 ,config为ARGB_8888，图片接近 92M
这里请求宽高为100x100,将图片放在 drawable-xhdpi 目录中，此时 drawable-xhdpi 所代表的 density 为 320，我的手机屏幕所代表的 density 是 480(targetDensity)，
将图片加载到内存中，此时 Bitmap 所代表的内存为：
```
inSampleSize = 4000 / 100 = 40

scale = targetDensity / density = 480 / 320 =1.5

widthPix = orignalWidth * scale = 6000 / 40 * scale = 225

heightPix = orignalHeight * scale = 4000 / 40 * scale = 150

BitmapMemory = widthPix * heightPix * 4 = 225 * 150 * 4 = 135000(Byte)=0.13M
```
// 放到Asset里面，scale为1  图片为8192*4608，大约是151M
inSampleSize 4
scale =1
widthPix = 8192/4=2048
heightPix = 4608 /4 =1152
bitmap size: 9    2048*1152*4= 9,437,184  大约是9M



inSampleSize的确定  一般要求是2的指数
1. 直接采样  直接设定inSampleSize，比如inSampleSize=200  提前有预估
2. 计算采样  根据请求的宽高计算合适的 inSampleSize
计算方式1
```
public int calculateSampleSize1(BitmapFactory.Options option, int reqWidth, int reqHeight) {
    //获得图片的原宽高
    int width = option.outWidth;
    int height = option.outHeight;

    int inSampleSize = 1;
    //宽图计算高度的采样
    if (width > reqWidth || height > reqHeight) {
        if (width > height) {
            //  原始高度/目标高度
            inSampleSize = Math.round((float) height / (float) reqHeight);
        } else {
            inSampleSize = Math.round((float) width / (float) reqWidth);
        }
    }
    return inSampleSize;
}
```
计算方式2
```
public int calculateSampleSize2(BitmapFactory.Options options, int reqWidth, int reqHeight) {
    //获得图片的原宽高
    int height = options.outHeight;
    int width = options.outWidth;

    int inSampleSize = 1;
    if (height > reqHeight || width > reqWidth) {
        // 计算出实际宽高和目标宽高的比率
        final int heightRatio = Math.round((float) height / (float) reqHeight);
        final int widthRatio = Math.round((float) width / (float) reqWidth);
        /**
         * 选择宽和高中最小的比率作为inSampleSize的值，这样可以保证最终图片的宽和高
         * 一定都会大于等于目标的宽和高。
         */
        inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
    }
    return inSampleSize;
}
```
计算方式3   https://developer.android.com/topic/performance/graphics/load-bitmap#java
```
    public static int calculateInSampleSize(
                BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // 原始大小
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            //采样时每次放大2,直到小于等于目标大小  
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
    
```
获得采样比例之后就可以根据所需宽高处理较大的图片了，下面是根据所需宽高计算出来的 inSampleSize 对较大位图进行采样：
```
public Bitmap decodeSampleFromBitmap(Resources resources, int resId, int reqWidth, int reqHeight) {
    //创建一个位图工厂的设置选项
    BitmapFactory.Options options = new BitmapFactory.Options();
    //设置该属性为true,解码时只能获取width、height、mimeType  可以用来优化获取图片大小的速度和空间
    options.inJustDecodeBounds = true;   
    //解码  native层将解码获取的大小设置到Options  将图片大小写入option
    BitmapFactory.decodeResource(resources, resId, options);
    //计算采样比例        根据option计算采样
    int inSampleSize = options.inSampleSize = calculateSampleSize2(options, reqWidth, reqHeight);
    //设置该属性为false，实现真正解码
    options.inJustDecodeBounds = false;
    //解码
    Bitmap bitmap = BitmapFactory.decodeResource(resources, resId, options);
    return bitmap;
}
```
//todo 图片格式解析   https://developer.android.com/topic/performance/graphics  更多资源


图片复用
https://developer.android.com/topic/performance/graphics/manage-memory
Android 3.0（API 级别 11）引入了 BitmapFactory.Options.inBitmap 字段。如果设置了此选项，那么采用 Options 对象的解码方法会在加载内容时尝试重复使用现有位图。
这意味着位图的内存得到了重复使用，从而提高了性能，同时移除了内存分配和取消分配。不过，inBitmap 的使用方式存在某些限制。
特别是在 Android 4.4（API 级别 19）之前，系统仅支持大小相同的位图
以下代码段演示了如何存储现有位图，以供稍后在示例应用中使用。当应用在 Android 3.0 或更高版本上运行并且位图从 LruCache 删除时，
对位图的软引用会放置在 HashSet 中，以供稍后通过 inBitmap 重复使用：
```
    //复用图片的软引用
    Set<SoftReference<Bitmap>> reusableBitmaps;
    //Lru缓存
    private LruCache<String, BitmapDrawable> memoryCache;

    //Honeycomb以前创建为同步的
    if (Utils.hasHoneycomb()) {
        reusableBitmaps =
                Collections.synchronizedSet(new HashSet<SoftReference<Bitmap>>());
    }

    memoryCache = new LruCache<String, BitmapDrawable>(cacheParams.memCacheSize) {

        //缓存移除
        @Override
        protected void entryRemoved(boolean evicted, String key,
                BitmapDrawable oldValue, BitmapDrawable newValue) {
            if (RecyclingBitmapDrawable.class.isInstance(oldValue)) {
                // The removed entry is a recycling drawable, so notify it
                // that it has been removed from the memory cache.
                ((RecyclingBitmapDrawable) oldValue).setIsCached(false);
            } else {
                // The removed entry is a standard BitmapDrawable.
                if (Utils.hasHoneycomb()) {
                    // We're running on Honeycomb or later, so add the bitmap
                    // to a SoftReference set for possible use with inBitmap later.
                    reusableBitmaps.add
                            (new SoftReference<Bitmap>(oldValue.getBitmap()));
                }
            }
        }
    ....
    }
    
```
使用现有位图
在正在运行的应用中，解码器方法会检查是否存在可以使用的现有位图。例如：
```
    public static Bitmap decodeSampledBitmapFromFile(String filename,
            int reqWidth, int reqHeight, ImageCache cache) {

        final BitmapFactory.Options options = new BitmapFactory.Options();
        ...
        BitmapFactory.decodeFile(filename, options);
        ...
        // 从reusableBitmaps找到可复用的bitmap并设置在inBitmap
        if (Utils.hasHoneycomb()) {
            addInBitmapOptions(options, cache);
        }
        ...
        //返回的bitmap是可变更的
        return BitmapFactory.decodeFile(filename, options);
    }
 
     private static void addInBitmapOptions(BitmapFactory.Options options,
            ImageCache cache) {
        // inBitmap only works with mutable bitmaps, so force the decoder to
        // return mutable bitmaps.
        //inBitmap只能工作在可变更的bitmap上
        options.inMutable = true;

        if (cache != null) {
            // Try to find a bitmap to use for inBitmap.
            Bitmap inBitmap = cache.getBitmapFromReusableSet(options);

            if (inBitmap != null) {
                // If a suitable bitmap has been found, set it as the value of
                // inBitmap.
                options.inBitmap = inBitmap;
            }
        }
    }

    //根据options找到可复用的bitmap
    protected Bitmap getBitmapFromReusableSet(BitmapFactory.Options options) {
            Bitmap bitmap = null;
        if (reusableBitmaps != null && !reusableBitmaps.isEmpty()) {
            synchronized (reusableBitmaps) {
                final Iterator<SoftReference<Bitmap>> iterator
                        = reusableBitmaps.iterator();
                Bitmap item;

                while (iterator.hasNext()) {
                    item = iterator.next().get();

                    if (null != item && item.isMutable()) {
                        // Check to see it the item can be used for inBitmap.
                        if (canUseForInBitmap(item, options)) {
                            bitmap = item;

                            // Remove from reusable set so it can't be used again.
                            iterator.remove();
                            break;
                        }
                    } else {
                         //当前的bitmap为null，内存回收了，或者bitmap是不可更改的
                        // Remove from the set if the reference has been cleared.
                        iterator.remove();
                    }
                }
            }
        }
        return bitmap;
    }
    //确定要用于 inBitmap 的候选位图是否满足相应的大小条件
    static boolean canUseForInBitmap(
            Bitmap candidate, BitmapFactory.Options targetOptions) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            //新bitmap的内存大小 < 复用的内存大小
            int width = targetOptions.outWidth / targetOptions.inSampleSize;
            int height = targetOptions.outHeight / targetOptions.inSampleSize;
            int byteCount = width * height * getBytesPerPixel(candidate.getConfig());
            return byteCount <= candidate.getAllocationByteCount();
        }
        // 4.4以前
        // On earlier versions, the dimensions must match exactly and the inSampleSize must be 1
        return candidate.getWidth() == targetOptions.outWidth
                && candidate.getHeight() == targetOptions.outHeight
                && targetOptions.inSampleSize == 1;
    }

    //获取一个像素的大小
    static int getBytesPerPixel(Config config) {
        if (config == Config.ARGB_8888) {
            return 4;
        } else if (config == Config.RGB_565) {
            return 2;
        } else if (config == Config.ARGB_4444) {
            return 2;
        } else if (config == Config.ALPHA_8) {
            return 1;
        }
        return 1;
    }
           
```