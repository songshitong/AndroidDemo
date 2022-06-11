
https://xie.infoq.cn/article/5b2731c61bd4e7966c898314d


总结
CAS 的本质就是：
lock cmpxchg 指令
但是cmpxchg这条 cpu 指令本身并不是原子性的，还是依赖了前面的lock指令

AtomicInteger.getAndIncrement()
AtomicInteger.java
```
public final int getAndIncrement() {
        return unsafe.getAndAddInt(this, valueOffset, 1);
    }
```
AtomicInteger通过Unsafe实现的原子类功能

Unsafe.java
```
   public final int getAndAddInt(Object o, long offset, int delta) {
        int v;
        do {
            v = getIntVolatile(o, offset);
        } while (!compareAndSwapInt(o, offset, v, v + delta));
        return v;
    }
    
  public final native boolean compareAndSwapInt(Object o, long offset,
                                                  int expected,
                                                  int x);    
```
jvm实现
http://hg.openjdk.java.net/jdk8/jdk8/hotspot/file/tip/src/share/vm/prims/unsafe.cpp
```
UNSAFE_ENTRY(jboolean, Unsafe_CompareAndSwapInt(JNIEnv *env, jobject unsafe, jobject obj, jlong offset, jint e, jint x))
  UnsafeWrapper("Unsafe_CompareAndSwapInt");
  oop p = JNIHandles::resolve(obj);
  jint* addr = (jint *) index_oop_from_field_offset_long(p, offset);
  return (jint)(Atomic::cmpxchg(x, addr, e)) == e;
UNSAFE_END
```
https://hg.openjdk.java.net/jdk8/jdk8/hotspot/file/87ee5ee27509/src/share/vm/runtime/atomic.cpp
```
jbyte Atomic::cmpxchg(jbyte exchange_value, volatile jbyte* dest, jbyte compare_value) {
  assert(sizeof(jbyte) == 1, "assumption.");
  uintptr_t dest_addr = (uintptr_t)dest;
  uintptr_t offset = dest_addr % sizeof(jint);
  volatile jint* dest_int = (volatile jint*)(dest_addr - offset);
  jint cur = *dest_int;
  jbyte* cur_as_bytes = (jbyte*)(&cur);
  jint new_val = cur;
  jbyte* new_val_as_bytes = (jbyte*)(&new_val);
  new_val_as_bytes[offset] = exchange_value;
  while (cur_as_bytes[offset] == compare_value) {
    //关键方法
    jint res = cmpxchg(new_val, dest_int, cur);
    if (res == cur) break;
    cur = res;
    new_val = cur;
    new_val_as_bytes[offset] = exchange_value;
  }
  return cur_as_bytes[offset];
}
```
各种系统下各种 cpu 架构，都有相关的实现方法
https://hg.openjdk.java.net/jdk8/jdk8/hotspot/file/87ee5ee27509/src/share/vm/runtime/atomic.inline.hpp
```
...
// Linux
#ifdef TARGET_OS_ARCH_linux_x86
# include "atomic_linux_x86.inline.hpp"
#endif
#ifdef TARGET_OS_ARCH_linux_sparc
# include "atomic_linux_sparc.inline.hpp"
#endif
#ifdef TARGET_OS_ARCH_linux_zero
# include "atomic_linux_zero.inline.hpp"
#endif
#ifdef TARGET_OS_ARCH_linux_arm
# include "atomic_linux_arm.inline.hpp"
#endif
#ifdef TARGET_OS_ARCH_linux_ppc
# include "atomic_linux_ppc.inline.hpp"
#endif
...
```
在 src/os_cpu/目录下面有各种系统下各种 cpu 架构的代码实现，其中 src/os_cpu/linux_x86/vm 下是基于 linux 下 x86 架构的代码，
cmpxchg 方法的最终实现：
https://hg.openjdk.java.net/jdk8/jdk8/hotspot/file/87ee5ee27509/src/os_cpu/linux_x86/vm/atomic_linux_x86.inline.hpp
```
inline jint     Atomic::cmpxchg    (jint     exchange_value, volatile jint*     dest, jint     compare_value) {
  int mp = os::is_MP();
  __asm__ volatile (LOCK_IF_MP(%4) "cmpxchgl %1,(%3)"
                    : "=a" (exchange_value)
                    : "r" (exchange_value), "a" (compare_value), "r" (dest), "r" (mp)
                    : "cc", "memory");
  return exchange_value;
}
```

其中__asm__ volatile (LOCK_IF_MP(%4) "cmpxchgl %1,(%3)"一段代码是核心，asm 指汇编语言，是机器语言，直接和 cpu 交互。
LOCKIFMP 指“如果是多个 CPU 就加锁”，MP 指 Multi-Processors。程序会根据当前处理器的数量来决定是否为 cmpxchg 指令添加 lock 前缀。
  如果程序是在多处理器上运行，就为 cmpxchg 指令加上 lock 前缀（lock cmpxchg）。反之，如果程序是在单处理器上运行，
  就省略 lock 前缀（单个处理器自身会维护单处理器内的顺序一致性，不需要 lock 前缀提供的内存屏障效果）
```
// Adding a lock prefix to an instruction on MP machine
#define LOCK_IF_MP(mp) "cmp $0, " #mp "; je 1f; lock; 1: "
```
cmpxchg  Compare and Exchange
Intel 白皮书,这本手册里可以查到 Intel 汇编指令的所有信息 //也有叫做ia32
《Intel® 64 and IA-32 Architectures Software Developer's Manual》 Volume 2 (2A, 2B, 2C & 2D): Instruction Set Reference, A-Z
另外可以参考
https://stackoverflow.com/questions/27837731/is-x86-cmpxchg-atomic-if-so-why-does-it-need-lock


总结
从上面可以看出，CAS 的本质就是：
lock cmpxchg 指令
但是cmpxchg这条 cpu 指令本身并不是原子性的，还是依赖了前面的lock指令
