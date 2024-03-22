
Android 8.0引入的一种辅助自动回收native内存的一种机制，当Java对象因为GC被回收后，
NativeAllocationRegistry可以辅助回收Java对象所申请的native内存
bitmap中的使用
frameworks/base/graphics/java/android/graphics/Bitmap.java
```
 Bitmap(long nativeBitmap, int width, int height, int density,
            boolean requestPremultiplied, byte[] ninePatchChunk,
            NinePatch.InsetStruct ninePatchInsets, boolean fromMalloc) {
        ....
        final int allocationByteCount = getAllocationByteCount();
        NativeAllocationRegistry registry;
        if (fromMalloc) {
            registry = NativeAllocationRegistry.createMalloced(
                    Bitmap.class.getClassLoader(), nativeGetNativeFinalizer(), allocationByteCount);
        } else {
            registry = NativeAllocationRegistry.createNonmalloced(
                    Bitmap.class.getClassLoader(), nativeGetNativeFinalizer(), allocationByteCount);
        }
        registry.registerNativeAllocation(this, nativeBitmap);
    }
```


android-12.0.0_r34:libcore/luni/src/main/java/libcore/util/NativeAllocationRegistry.java
```
public class NativeAllocationRegistry {
 public static NativeAllocationRegistry createNonmalloced(
            @NonNull ClassLoader classLoader, long freeFunction, long size) {
        return new NativeAllocationRegistry(classLoader, freeFunction, size, false);
    }

  public static NativeAllocationRegistry createMalloced(
            @NonNull ClassLoader classLoader, long freeFunction, long size) {
        return new NativeAllocationRegistry(classLoader, freeFunction, size, true);
    }

    private NativeAllocationRegistry(ClassLoader classLoader, long freeFunction, long size,
            boolean mallocAllocation) {
       ...
        this.classLoader = classLoader;
        this.freeFunction = freeFunction;
        this.size = mallocAllocation ? (size | IS_MALLOCED) : (size & ~IS_MALLOCED);
    }
}
```

registerNativeAllocation
```
 public @NonNull Runnable registerNativeAllocation(@NonNull Object referent, long nativePtr) {
       ...
        CleanerThunk thunk;
        CleanerRunner result;
        try {
            thunk = new CleanerThunk();
            Cleaner cleaner = Cleaner.create(referent, thunk); //Java对象回收，执行cleaner的run，进而执行native的回收策略
            result = new CleanerRunner(cleaner); //可以执行cleaner的run
            registerNativeAllocation(this.size); //size告诉art,判断是否执行GC
        } catch (VirtualMachineError vme /* probably OutOfMemoryError */) {
            applyFreeFunction(freeFunction, nativePtr); //可能内存溢出了，直接回收
            throw vme;
        } // Other exceptions are impossible.
        // Enable the cleaner only after we can no longer throw anything, including OOME.
        thunk.setNativePtr(nativePtr);
        // Ensure that cleaner doesn't get invoked before we enable it.
        Reference.reachabilityFence(referent);
        return result;
    }
    
 
   private class CleanerThunk implements Runnable {
       ...
        public void run() {
            if (nativePtr != 0) {
                applyFreeFunction(freeFunction, nativePtr); //执行native的回收
                registerNativeFree(size); //告诉art 已经回收的大小
            }
        }
       ...
    } 
 
     private static void registerNativeFree(long size) {
        if ((size & IS_MALLOCED) == 0) {
            VMRuntime.getRuntime().registerNativeFree(size);
        }
    }   
  
   private static class CleanerRunner implements Runnable {
       ...
        public void run() {
            cleaner.clean();
        }
    }
    
    
  private static void registerNativeAllocation(long size) {
        VMRuntime runtime = VMRuntime.getRuntime();
        if ((size & IS_MALLOCED) != 0) {
            final long notifyImmediateThreshold = 300000;
            if (size >= notifyImmediateThreshold) {
                runtime.notifyNativeAllocationsInternal();
            } else {
                runtime.notifyNativeAllocation();
            }
        } else {
            runtime.registerNativeAllocation(size);
        }
    }
    
  public static native void applyFreeFunction(long freeFunction, long nativePtr);  
```

libcore/luni/src/main/native/libcore_util_NativeAllocationRegistry.cpp
```
typedef void (*FreeFunction)(void*);
static void NativeAllocationRegistry_applyFreeFunction(JNIEnv*,
                                                       jclass,
                                                       jlong freeFunction,
                                                       jlong ptr) {
    void* nativePtr = reinterpret_cast<void*>(static_cast<uintptr_t>(ptr));
    FreeFunction nativeFreeFunction
        = reinterpret_cast<FreeFunction>(static_cast<uintptr_t>(freeFunction));
    nativeFreeFunction(nativePtr); //执行native方法
}
```
cleaner  https://sharrychoo.github.io/blog/android-source/bitmap-memory-evolution#%E4%B8%80-cleaner-%E7%9A%84%E5%88%9B%E5%BB%BA
Cleaner 的构造函数中有如下的注释
The cleanup code is run directly from the reference-handler thread, so it should be as simple and straightforward as possible.
从注释中可以看出, 这个 Cleaner.clean 方法由 引用处理线程 直接回调的, 并且提示我们不要让 thunk.run 执行耗时操作
看到这里我们大概就能够明白, 为什么 Bitmap 中找不到 finalize 方法, 也能释放 Native 内存了, 因为一个 Bitmap 对象会被包装成 Cleaner, 
成为 Cleaner 链表中的一员, 它的清理操作由 引用处理线程 直接回调 Cleaner.clean 执行数据清理, 同样能够及时释放内存
libcore/ojluni/src/main/java/sun/misc/Cleaner.java
```
public class Cleaner
    extends PhantomReference<Object>
{
 private static final ReferenceQueue<Object> dummyQueue = new ReferenceQueue<>();

 private final Runnable thunk;
 
 public static Cleaner create(Object ob, Runnable thunk) {
        ...
        return add(new Cleaner(ob, thunk));
    }

 private static synchronized Cleaner add(Cleaner cl) {
        if (first != null) {
            cl.next = first;
            first.prev = cl;
        }
        first = cl;
        return cl;
    }

 private Cleaner(Object referent, Runnable thunk) {
        super(referent, dummyQueue);
        this.thunk = thunk;
    }
    
 
 public void clean() {
        if (!remove(this)) //移除add添加的node
            return;
        try {
            thunk.run(); //执行清理方法
        } catch (final Throwable x) {
            ...
        }
    }   
}
```

bitmap nativeGetNativeFinalizer的实现  Java对象被回收时，删除native的bitmap
frameworks/base/libs/hwui/jni/Bitmap.cpp
```
static jlong Bitmap_getNativeFinalizer(JNIEnv*, jobject) {
    return static_cast<jlong>(reinterpret_cast<uintptr_t>(&Bitmap_destruct));
}

static void Bitmap_destruct(BitmapWrapper* bitmap) {
    delete bitmap;
}
```



VmRuntime相关
registerNativeAllocation
libcore/libart/src/main/java/dalvik/system/VMRuntime.java
```
 public native void registerNativeAllocation(long bytes);
```
art/runtime/native/dalvik_system_VMRuntime.cc
```
static void VMRuntime_registerNativeAllocation(JNIEnv* env, jobject, jlong bytes) {
 ...
  Runtime::Current()->GetHeap()->RegisterNativeAllocation(env, clamp_to_size_t(bytes));
}
```
art/runtime/gc/heap.cc
```
void Heap::RegisterNativeAllocation(JNIEnv* env, size_t bytes) {
 ...
  native_bytes_registered_.fetch_add(bytes, std::memory_order_relaxed);
  uint32_t objects_notified =
      native_objects_notified_.fetch_add(1, std::memory_order_relaxed);
  // 判断 Native 分配的内存是否触发了 GC 阈值, 超过了则进行 GC 操作释放 Java 和 Native 内存    
  if (objects_notified % kNotifyNativeInterval == kNotifyNativeInterval - 1
      || bytes > kCheckImmediatelyThreshold) {
    CheckGCForNative(ThreadForEnv(env));
  }
}
```

notifyNativeAllocation与notifyNativeAllocationsInternal
libcore/libart/src/main/java/dalvik/system/VMRuntime.java
```
 public void notifyNativeAllocation() {
        int myNotifyNativeInterval = notifyNativeInterval;
        if (myNotifyNativeInterval == 0) {
            // This can race. By Java rules, that's OK.
            myNotifyNativeInterval = notifyNativeInterval = getNotifyNativeInterval();
        }
        if (allocationCount.addAndGet(1) % myNotifyNativeInterval == 0) {
            notifyNativeAllocationsInternal();
        }
    }
```
NotifyNativeAllocations  
art/runtime/native/dalvik_system_VMRuntime.cc
```
void Heap::NotifyNativeAllocations(JNIEnv* env) {
  native_objects_notified_.fetch_add(kNotifyNativeInterval, std::memory_order_relaxed);
  CheckGCForNative(ThreadForEnv(env));
}
```


NativeAllocationRegistry
1 Java对象回收后自动移除native
2 Java对象使用native达到阈值会gc，确保内存充足