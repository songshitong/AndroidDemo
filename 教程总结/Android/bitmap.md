

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
总结一下：较大的 bitmap 直接通过 Intent 传递容易抛异常是因为 Intent 启动组件时，系统禁掉了文件描述符 fd 机制 ,
bitmap 无法利用共享内存，只能拷贝到 Binder 映射的缓冲区，导致缓冲区超限, 触发异常; 
而通过 putBinder 的方式，避免了 Intent 禁用描述符的影响，bitmap 写 parcel 时的 allowFds 默认是 true , 
可以利用共享内存，所以能高效传输图片
