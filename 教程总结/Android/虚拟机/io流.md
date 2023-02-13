
android-13.0.0_r1:libcore/ojluni/src/main/java/java/io/InputStream.java
java/io/InputStream.java
```
public abstract class InputStream implements Closeable {
   public abstract int read() throws IOException;
}
```
libcore/ojluni/src/main/java/java/io/FileInputStream.java
```
  public FileInputStream(File file) throws FileNotFoundException {      
       ....
        fd = IoBridge.open(name, O_RDONLY);
          ...
    }

    public int read(byte b[], int off, int len) throws IOException {
        ...
        return IoBridge.read(fd, b, off, len);
    }
    
     public void close() throws IOException {
       ...
       IoBridge.closeAndSignalBlockedThreads(fd);
    }       
```
通过IoBridge进行文件操作

libcore/luni/src/main/java/libcore/io/IoBridge.java
```
public final class IoBridge {
   public static @NonNull FileDescriptor open(@NonNull String path, int flags) throws FileNotFoundException {
        FileDescriptor fd = null;
        try {
            fd = Libcore.os.open(path, flags, 0666);
           ...
        }
    }
    
     public static int read(@NonNull FileDescriptor fd, @NonNull byte[] bytes, int byteOffset, int byteCount) throws IOException {
       ....
        try {
            int readCount = Libcore.os.read(fd, bytes, byteOffset, byteCount);
            if (readCount == 0) {
                return -1;
            }
            return readCount;
        } ...
    }
    
  
   public static void closeAndSignalBlockedThreads(@NonNull FileDescriptor fd) throws IOException {
       ...
        FileDescriptor oldFd = fd.release$();
        if (!oldFd.valid()) {
            return;
        }
        AsynchronousCloseMonitor.signalBlockedThreads(oldFd);
        try {
            Libcore.os.close(oldFd);
        } catch (ErrnoException errnoException) {
            throw errnoException.rethrowAsIOException();
        }
    }  
}
```
通过Libcore.os进行控制
libcore/luni/src/main/java/libcore/io/Libcore.java
```
public static final Os rawOs = new Linux();
public static volatile Os os = new BlockGuardOs(rawOs);
```
libcore/luni/src/main/java/libcore/io/BlockGuardOs.java
```
  @Override public int read(FileDescriptor fd, ByteBuffer buffer) throws ErrnoException, InterruptedIOException {
        BlockGuard.getThreadPolicy().onReadFromDisk();
        return super.read(fd, buffer);
    }
    
   @Override public FileDescriptor open(String path, int flags, int mode) throws ErrnoException {
        BlockGuard.getThreadPolicy().onReadFromDisk();
        BlockGuard.getVmPolicy().onPathAccess(path);
        if ((flags & O_ACCMODE) != O_RDONLY) {
            BlockGuard.getThreadPolicy().onWriteToDisk();
        }
        return super.open(path, flags, mode);
    }

  @Override public void close(FileDescriptor fd) throws ErrnoException {
       ...
        super.close(fd);
    }
```
BlockGuardOs在os的基础上新加了blockGuard的一些限制
BlockGuardOs的父类使用代理模式，真正实现是linuxOs
libcore/luni/src/main/java/libcore/io/Linux.java
```
public final class Linux implements Os {
  public native FileDescriptor open(String path, int flags, int mode) throws ErrnoException;
  
   public int read(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws ErrnoException, InterruptedIOException {
        return readBytes(fd, bytes, byteOffset, byteCount);
    }
    private native int readBytes(FileDescriptor fd, Object buffer, int offset, int byteCount) throws ErrnoException, InterruptedIOException;
    
   public native void close(FileDescriptor fd) throws ErrnoException; 
}
```
linuxOS的实现为native方法
libcore/luni/src/main/native/libcore_io_Linux.cpp
```
static jobject Linux_open(JNIEnv* env, jobject, jstring javaPath, jint flags, jint mode) {
    ScopedUtfChars path(env, javaPath);
    ...
    int fd = throwIfMinusOne(env, "open", TEMP_FAILURE_RETRY(open(path.c_str(), flags, mode)));
    return createFileDescriptorIfOpen(env, fd);
}

static jint Linux_readBytes(JNIEnv* env, jobject, jobject javaFd, jobject javaBytes, jint byteOffset, jint byteCount) {
    ScopedBytesRW bytes(env, javaBytes);
    if (bytes.get() == NULL) {
        return -1;
    }
    return IO_FAILURE_RETRY(env, ssize_t, read, javaFd, bytes.get() + byteOffset, byteCount);
}


static void Linux_close(JNIEnv* env, jobject, jobject javaFd) {
   ...
    int fd = jniGetFDFromFileDescriptor(env, javaFd);
    jniSetFileDescriptorOfFD(env, javaFd, -1);

#if defined(__BIONIC__)
    static jmethodID getOwnerId = env->GetMethodID(JniConstants::GetFileDescriptorClass(env),
                                                   "getOwnerId$", "()J");
    jlong ownerId = env->CallLongMethod(javaFd, getOwnerId);

    // Close with bionic's fd ownership tracking (which returns 0 in the case of EINTR).
    throwIfMinusOne(env, "close", android_fdsan_close_with_tag(fd, ownerId));
#else
    // Even if close(2) fails with EINTR, the fd will have been closed.
    // Using TEMP_FAILURE_RETRY will either lead to EBADF or closing someone else's fd.
    // http://lkml.indiana.edu/hypermail/linux/kernel/0509.1/0877.html
    throwIfMinusOne(env, "close", close(fd));
#endif
}
```
open 函数
bionic/libc/include/bits/fortify/fcntl.h
```
__BIONIC_FORTIFY_INLINE
int open(const char* const __pass_object_size pathname, int flags, mode_t modes)
        __overloadable
        __clang_warning_if(!__open_modes_useful(flags) && modes,
                           "'open' " __open_useless_modes_warning) {
    return __open_real(pathname, flags, modes);
}
int __open_real(const char*, int, ...) __RENAME(open)
```
read函数
libcore/luni/src/main/native/libcore_io_Linux.cpp
```
#define IO_FAILURE_RETRY(jni_env, return_type, syscall_name, java_fd, ...) ({ \
   ...
   _rc = syscall_name(_fd, __VA_ARGS__);
   ...
   })
```

close
在android平台
bionic/libc/bionic/fdsan.cpp
```
int android_fdsan_close_with_tag(int fd, uint64_t expected_tag) {
  ...
  __close(fd);
  ...
}

extern "C" int __close(int fd);
```
非android平台
prebuilts/gcc/linux-x86/host/x86_64-linux-glibc2.17-4.8/sysroot/usr/include/unistd.h
```
extern int close (int __fd);
```
