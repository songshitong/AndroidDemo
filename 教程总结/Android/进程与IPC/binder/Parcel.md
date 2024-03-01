
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
```