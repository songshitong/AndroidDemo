
Parcel介绍：
Container for a message (data and object references) that can be sent through an IBinder
android-9.0.0_r60

frameworks/base/core/java/android/os/Parcel.java
```
public final class Parcel {
   private long mNativePtr; // used by native code
   
   //支持6个Parcel的复用
    private static final int POOL_SIZE = 6;
    private static final Parcel[] sOwnedPool = new Parcel[POOL_SIZE];
    private static final Parcel[] sHolderPool = new Parcel[POOL_SIZE];
   public static Parcel obtain() {
        final Parcel[] pool = sOwnedPool;
        synchronized (pool) {
            Parcel p;
            for (int i=0; i<POOL_SIZE; i++) {
                p = pool[i];
                if (p != null) {
                    pool[i] = null; //解除引用
                    if (DEBUG_RECYCLE) {
                        p.mStack = new RuntimeException();
                    }
                    p.mReadWriteHelper = ReadWriteHelper.DEFAULT;
                    return p;
                }
            }
        }
        return new Parcel(0); 
    }

    
    public final void recycle() {
        if (DEBUG_RECYCLE) mStack = null;
        freeBuffer();

        final Parcel[] pool;
        if (mOwnsNativeParcelObject) {
            pool = sOwnedPool;
        } else {
            mNativePtr = 0;
            pool = sHolderPool;
        }

        synchronized (pool) {
            for (int i=0; i<POOL_SIZE; i++) {
                if (pool[i] == null) { //找到空位插入，引用增加
                    pool[i] = this;
                    return;
                }
            }
        }
    }
    
   //读取string 
   public final String readString() {
        return mReadWriteHelper.readString(this);
    }
    
     public static class ReadWriteHelper {
        public static final ReadWriteHelper DEFAULT = new ReadWriteHelper();
        public void writeString(Parcel p, String s) {
            nativeWriteString(p.mNativePtr, s);
        }
        public String readString(Parcel p) {
            return nativeReadString(p.mNativePtr);
        }
    }
   
   static native String nativeReadString(long nativePtr);
   
   //写入string
   public final void writeString(String val) {
        mReadWriteHelper.writeString(this, val);
    } 
   
   static native void nativeWriteString(long nativePtr, String val); 
   
   //设置接口名称
   public final void enforceInterface(String interfaceName) {
        nativeEnforceInterface(mNativePtr, interfaceName);
    }
   
   public final void writeInterfaceToken(String interfaceName) {
        nativeWriteInterfaceToken(mNativePtr, interfaceName);
    } 
    
   //读取通信的结果  有异常抛出 
   public final void readException() {
        int code = readExceptionCode();
        if (code != 0) { //读取错误码和string
            String msg = readString();
            readException(code, msg);
        }
    } 
   
    public final int readExceptionCode() {
        int code = readInt();
        if (code == EX_HAS_REPLY_HEADER) { //EX_HAS_REPLY_HEADER = -128;
            int headerSize = readInt();
            if (headerSize == 0) {
                Log.e(TAG, "Unexpected zero-sized Parcel reply header.");
            } else {
                // Currently the only thing in the header is StrictMode stacks,
                // but discussions around event/RPC tracing suggest we might
                // put that here too.  If so, switch on sub-header tags here.
                // But for now, just parse out the StrictMode stuff.
                StrictMode.readAndHandleBinderCallViolations(this);
            }
            // And fat response headers are currently only used when
            // there are no exceptions, so return no error:
            return 0;
        }
        return code;
    }
    
   public final int readInt() {
        return nativeReadInt(mNativePtr);
    }
 
    public final void readException(int code, String msg) {
        String remoteStackTrace = null;
        final int remoteStackPayloadSize = readInt();
        if (remoteStackPayloadSize > 0) {
            remoteStackTrace = readString();
        }
        Exception e = createException(code, msg);
        // Attach remote stack trace if availalble
        if (remoteStackTrace != null) {
            RemoteException cause = new RemoteException(
                    "Remote stack trace:\n" + remoteStackTrace, null, false, false);
            try {
                Throwable rootCause = ExceptionUtils.getRootCause(e);
                if (rootCause != null) {
                    rootCause.initCause(cause);
                }
            } catch (RuntimeException ex) {
                Log.e(TAG, "Cannot set cause " + cause + " for " + e, ex);
            }
        }
        SneakyThrow.sneakyThrow(e);  //Throw an exception with the given message
    }
}
```

frameworks/base/core/jni/android_os_Parcel.cpp  
https://cs.android.com/android/platform/superproject/+/android-wear-9.0.0_r34:frameworks/base/core/jni/android_os_Parcel.cpp
```
static void android_os_Parcel_writeString(JNIEnv* env, jclass clazz, jlong nativePtr, jstring val)
{
    Parcel* parcel = reinterpret_cast<Parcel*>(nativePtr);
    if (parcel != NULL) {
        status_t err = NO_MEMORY;
        if (val) {
            const jchar* str = env->GetStringCritical(val, 0);
            if (str) {
                err = parcel->writeString16(
                    reinterpret_cast<const char16_t*>(str),
                    env->GetStringLength(val));
                env->ReleaseStringCritical(val, str);
            }
        } else {
            err = parcel->writeString16(NULL, 0);
        }
        if (err != NO_ERROR) {
            signalExceptionForError(env, clazz, err);
        }
    }
}

static jstring android_os_Parcel_readString(JNIEnv* env, jclass clazz, jlong nativePtr)
{
    Parcel* parcel = reinterpret_cast<Parcel*>(nativePtr);
    if (parcel != NULL) {
        size_t len;
        const char16_t* str = parcel->readString16Inplace(&len);
        if (str) {
            return env->NewString(reinterpret_cast<const jchar*>(str), len);
        }
        return NULL;
    }
    return NULL;
}
```


frameworks/native/libs/binder/Parcel.cpp
```
status_t Parcel::writeString16(const std::unique_ptr<String16>& str)
{
    if (!str) {
        return writeInt32(-1);
    }

    return writeString16(*str);
}

status_t Parcel::writeString16(const String16& str)
{
    return writeString16(str.string(), str.size());
}

status_t Parcel::writeString16(const char16_t* str, size_t len)
{
    if (str == NULL) return writeInt32(-1);

    status_t err = writeInt32(len); //先写入数据长度
    if (err == NO_ERROR) {
        len *= sizeof(char16_t); //然后写入数据内容
        uint8_t* data = (uint8_t*)writeInplace(len+sizeof(char16_t));
        if (data) {
            memcpy(data, str, len); //将数据复制到data所指向的位置中
            *reinterpret_cast<char16_t*>(data+len) = 0;
            return NO_ERROR;
        }
        err = mError;
    }
    return err;
}

#define PAD_SIZE_UNSAFE(s) (((s)+3)&~3)  //~是取反 

static size_t pad_size(size_t s) {
    if (s > (SIZE_T_MAX - 3)) {
        abort();
    }
    return PAD_SIZE_UNSAFE(s);
}

void* Parcel::writeInplace(size_t len)
{
    ...
    const size_t padded = pad_size(len); 用于计算“当以4 对齐时，容纳 len 大小的数据需要多少空间”

    ...
    if ((mDataPos+padded) <= mDataCapacity) {
restart_write:
        //printf("Writing %ld bytes, padded to %ld\n", len, padded);
        uint8_t* const data = mData+mDataPos;

        // Need to pad at end?
        if (padded != len) { //对齐后需要填充尾部的情况
#if BYTE_ORDER == BIG_ENDIAN
            static const uint32_t mask[4] = {
                0x00000000, 0xffffff00, 0xffff0000, 0xff000000
            };
#endif
#if BYTE_ORDER == LITTLE_ENDIAN
            static const uint32_t mask[4] = {
                0x00000000, 0x00ffffff, 0x0000ffff, 0x000000ff
            };
#endif
            //printf("Applying pad mask: %p to %p\n", (void*)mask[padded-len],
            //    *reinterpret_cast<void**>(data+padded-4));
            *reinterpret_cast<uint32_t*>(data+padded-4) &= mask[padded-len];
            //先做了 uint32_t 的强制转换，所以后续操作就是以4 个字节为单位    在需要填充的地址进行赋值
        }

        finishWrite(padded); //更新mDataPos mDataSize
        return data;
    }

    status_t err = growData(padded); //扩容
    if (err == NO_ERROR) goto restart_write; //扩容后重新write
    return NULL;
}


//更新mDataPos mDataSize
status_t Parcel::finishWrite(size_t len)
{
    ....
    mDataPos += len;
    ALOGV("finishWrite Setting data pos of %p to %zu", this, mDataPos);
    if (mDataPos > mDataSize) {
        mDataSize = mDataPos;
        ALOGV("finishWrite Setting data size of %p to %zu", this, mDataSize);
    }
    ...
    return NO_ERROR;
}
```
其他的写入
```
status_t Parcel::writeInterfaceToken(const String16& interface)
{
    writeInt32(IPCThreadState::self()->getStrictModePolicy() |
               STRICT_MODE_PENALTY_GATHER);
    // currently the interface identification token is just its name as a string
    return writeString16(interface);
}
```

校验interface是否一致，不一致抛出异常   同时会同步StrictModePolicy
enforceInterface
frameworks/base/core/jni/android_os_Parcel.cpp
```
static void android_os_Parcel_enforceInterface(JNIEnv* env, jclass clazz, jlong nativePtr, jstring name)
{
    Parcel* parcel = reinterpret_cast<Parcel*>(nativePtr);
    if (parcel != NULL) {
        const jchar* str = env->GetStringCritical(name, 0);
        if (str) {
            IPCThreadState* threadState = IPCThreadState::self();
            const int32_t oldPolicy = threadState->getStrictModePolicy();
            const bool isValid = parcel->enforceInterface(
                String16(reinterpret_cast<const char16_t*>(str),
                         env->GetStringLength(name)),
                threadState);
            env->ReleaseStringCritical(name, str);
            if (isValid) {
                const int32_t newPolicy = threadState->getStrictModePolicy();
                if (oldPolicy != newPolicy) {
                    // Need to keep the Java-level thread-local strict
                    // mode policy in sync for the libcore
                    // enforcements, which involves an upcall back
                    // into Java.  (We can't modify the
                    // Parcel.enforceInterface signature, as it's
                    // pseudo-public, and used via AIDL
                    // auto-generation...)
                    set_dalvik_blockguard_policy(env, newPolicy);
                }
                return;     // everything was correct -> return silently
            }
        }
    }

    // all error conditions wind up here
    jniThrowException(env, "java/lang/SecurityException",
            "Binder invocation to an incorrect interface");
}
```
frameworks/native/libs/binder/Parcel.cpp
```
bool Parcel::enforceInterface(const String16& interface,
                              IPCThreadState* threadState) const
{
    int32_t strictPolicy = readInt32(); //读取policy
    if (threadState == NULL) {
        threadState = IPCThreadState::self();
    }
    if ((threadState->getLastTransactionBinderFlags() &
         IBinder::FLAG_ONEWAY) != 0) {
      // For one-way calls, the callee is running entirely
      // disconnected from the caller, so disable StrictMode entirely.
      // Not only does disk/network usage not impact the caller, but
      // there's no way to commuicate back any violations anyway.
      threadState->setStrictModePolicy(0);
    } else {
      threadState->setStrictModePolicy(strictPolicy);
    }
    const String16 str(readString16());
    if (str == interface) {
        return true;
    } else {
       ...
        return false;
    }
}
```

writeInterfaceToken的native实现
frameworks/native/libs/binder/Parcel.cpp
```
// Write RPC headers.  (previously just the interface token)
status_t Parcel::writeInterfaceToken(const String16& interface)
{
    writeInt32(IPCThreadState::self()->getStrictModePolicy() |
               STRICT_MODE_PENALTY_GATHER);
    // currently the interface identification token is just its name as a string
    return writeString16(interface);
}
```


readString的实现
frameworks/base/core/jni/android_os_Parcel.cpp
```
static jstring android_os_Parcel_readString(JNIEnv* env, jclass clazz, jlong nativePtr)
{
    Parcel* parcel = reinterpret_cast<Parcel*>(nativePtr);
    if (parcel != NULL) {
        size_t len;
        const char16_t* str = parcel->readString16Inplace(&len);
        if (str) {
            return env->NewString(reinterpret_cast<const jchar*>(str), len);
        }
        return NULL;
    }
    return NULL;
}
```
frameworks/native/libs/binder/Parcel.cpp
```
const char16_t* Parcel::readString16Inplace(size_t* outLen) const
{
    int32_t size = readInt32(); //先读长度
    // watch for potential int overflow from size+1
    if (size >= 0 && size < INT32_MAX) {
        *outLen = size; //然后读取内容
        const char16_t* str = (const char16_t*)readInplace((size+1)*sizeof(char16_t));
        if (str != NULL) {
            return str;
        }
    }
    *outLen = 0;
    return NULL;
}

const void* Parcel::readInplace(size_t len) const
{
  ...
    if ((mDataPos+pad_size(len)) >= mDataPos && (mDataPos+pad_size(len)) <= mDataSize
            && len <= pad_size(len)) {
        if (mObjectsSize > 0) { //校验data是否有效
            status_t err = validateReadData(mDataPos + pad_size(len));
            if(err != NO_ERROR) {
                // Still increment the data position by the expected length
                mDataPos += pad_size(len);
                ALOGV("readInplace Setting data pos of %p to %zu", this, mDataPos);
                return NULL;
            }
        }

        const void* data = mData+mDataPos;
        mDataPos += pad_size(len); //更新对齐后的mDataPos
        ALOGV("readInplace Setting data pos of %p to %zu", this, mDataPos);
        return data;
    }
    return NULL;
}


status_t Parcel::validateReadData(size_t upperBound) const
{
    // Don't allow non-object reads on object data
    if (mObjectsSorted || mObjectsSize <= 1) {
data_sorted:
        // Expect to check only against the next object
        if (mNextObjectHint < mObjectsSize && upperBound > mObjects[mNextObjectHint]) {
            // For some reason the current read position is greater than the next object
            // hint. Iterate until we find the right object
            size_t nextObject = mNextObjectHint;
            do {
                if (mDataPos < mObjects[nextObject] + sizeof(flat_binder_object)) {
                    // Requested info overlaps with an object
                    ALOGE("Attempt to read from protected data in Parcel %p", this);
                    return PERMISSION_DENIED;
                }
                nextObject++;
            } while (nextObject < mObjectsSize && upperBound > mObjects[nextObject]);
            mNextObjectHint = nextObject;
        }
        return NO_ERROR;
    }
    // Quickly determine if mObjects is sorted.
    binder_size_t* currObj = mObjects + mObjectsSize - 1;
    binder_size_t* prevObj = currObj;
    while (currObj > mObjects) {
        prevObj--;
        if(*prevObj > *currObj) {
            goto data_unsorted;
        }
        currObj--;
    }
    mObjectsSorted = true;
    goto data_sorted;

data_unsorted:
    // Insertion Sort mObjects
    // Great for mostly sorted lists. If randomly sorted or reverse ordered mObjects become common,
    // switch to std::sort(mObjects, mObjects + mObjectsSize);
    for (binder_size_t* iter0 = mObjects + 1; iter0 < mObjects + mObjectsSize; iter0++) {
        binder_size_t temp = *iter0;
        binder_size_t* iter1 = iter0 - 1;
        while (iter1 >= mObjects && *iter1 > temp) {
            *(iter1 + 1) = *iter1;
            iter1--;
        }
        *(iter1 + 1) = temp;
    }
    mNextObjectHint = 0;
    mObjectsSorted = true;
    goto data_sorted;
}
```

总结：  对于读的顺序，也是按照写入的顺序来读
写入一个String(writeString16)的步骤：
writeInt32(len)
memcpy
padding(有些情况下不需padding。而且源码实现中这一步是在memcpy 之前)