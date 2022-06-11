volatile速度测试   
测试机器 AMD Ryzen 7 5700U with Radeon Graphics
L1d cache:           32K
L1i cache:           32K
L2 cache:            512K
L3 cache:            4096K
没有volatile字段
```
   static  int num=0;
    public static void main(String[] args)  {
        long start = System.currentTimeMillis();
        int length = 1000*10000;
        int[]  arr = new int[length];
        for(int i=0;i<length;i++){
           arr[i]= (++num);
        }
        System.out.println(System.currentTimeMillis()-start);
    }
```
结果 40ms
增加volatile字段
```
  static volatile int num=0;
    public static void main(String[] args)  {
        long start = System.currentTimeMillis();
        int length = 1000*10000;
        int[]  arr = new int[length];
        for(int i=0;i<length;i++){
           arr[i]= (++num);
        }
        System.out.println(System.currentTimeMillis()-start);
    }
```
135ms

提问没 volatile 修饰时一定不可见吗
没 volatile 修饰时，jvm也会尽量保证可见性。有 volatile 修饰的时候，一定保证可见性。  //跟CPU的缓存是否一致性有关

https://www.cnblogs.com/dolphin0520/p/3920373.html
《深入理解Java虚拟机》：
“观察加入volatile关键字和没有加入volatile关键字时所生成的汇编代码发现，加入volatile关键字时，会多出一个lock前缀指令”
lock前缀指令实际上相当于一个内存屏障（也成内存栅栏），内存屏障会提供3个功能：
1）它确保指令重排序时不会把其后面的指令排到内存屏障之前的位置，也不会把前面的指令排到内存屏障的后面；即在执行到内存屏障这句指令时，
  在它前面的操作已经全部完成；
2）它会强制将对缓存的修改操作立即写入主存；
3）如果是写操作，它会导致其他CPU中对应的缓存行无效。


https://www.jianshu.com/p/2ab5e3d7e510
Volatile语义在x86_64平台使用的CPU屏障指令是lock

内存屏障是硬件层的概念，不同的硬件平台实现内存屏障的手段并不是一样，java通过屏蔽这些差异，统一由jvm来生成内存屏障的指令
内存屏障是什么
硬件层的内存屏障分为两种：Load Barrier 和 Store Barrier即读屏障和写屏障。
内存屏障有两个作用：
  阻止屏障两侧的指令重排序；
  强制把写缓冲区/高速缓存中的脏数据等写回主内存，让缓存中相应的数据失效。

对于Load Barrier来说，在指令前插入Load Barrier，可以让高速缓存中的数据失效，强制从新从主内存加载数据；
对于Store Barrier来说，在指令后插入Store Barrier，能让写入缓存中的最新数据更新写入主内存，让其他线程可见

java内存屏障
java的内存屏障通常所谓的四种即LoadLoad,StoreStore,LoadStore,StoreLoad实际上也是上述两种的组合，完成一系列的屏障和数据同步功能。
LoadLoad屏障：对于这样的语句Load1; LoadLoad; Load2，在Load2及后续读取操作要读取的数据被访问前，保证Load1要读取的数据被读取完毕。
StoreStore屏障：对于这样的语句Store1; StoreStore; Store2，在Store2及后续写入操作执行前，保证Store1的写入操作对其它处理器可见。
LoadStore屏障：对于这样的语句Load1; LoadStore; Store2，在Store2及后续写入操作被刷出前，保证Load1要读取的数据被读取完毕。
StoreLoad屏障：对于这样的语句Store1; StoreLoad; Load2，在Load2及后续所有读取操作执行前，保证Store1的写入对所有处理器可见。
  它的开销是四种屏障中最大的。在大多数处理器的实现中，这个屏障是个万能屏障，兼具其它三种内存屏障的功能


volatile语义中的内存屏障
volatile的内存屏障策略非常严格保守，非常悲观且毫无安全感的心态：
在每个volatile写操作前插入StoreStore屏障，在写操作后插入StoreLoad屏障；
在每个volatile读操作后插入LoadLoad，LoadStore屏障；

由于内存屏障的作用，避免了volatile变量和其它指令重排序、线程之间实现了通信，使得volatile表现出了锁的特性


https://www.jianshu.com/p/6ab7c3db13c3
作者介绍了一种思路，先看字节码，根据标志搜索jvm，对比汇编，搜索jvm
字节码
Volatile的字节码会添加ACC_VOLATILE的标志
volatile int anInt=1;
```
 volatile int anInt;
    descriptor: I
    flags: (0x0040) ACC_VOLATILE
```
在jvm源码中搜索ACC_VOLATILE
https://github.com/JetBrains/jdk8u_hotspot/blob/c39701c890a5960dfa7c325b78ee97085646f57d/agent/src/share/classes/sun/jvm/hotspot/oops/AccessFlags.java
```
public boolean isVolatile    () { return (flags & JVM_ACC_VOLATILE    ) != 0; }
```
后面没走通

对比带有volatile和不带的汇编，发现增加了lock addl $0x0,(%rsp)
搜索lock addl发现https://github.com/JetBrains/jdk8u_hotspot/blob/master/src/share/vm/runtime/orderAccess.hpp
fence函数使用了这个指令
并且这个文件的注释含有解释内存模型 （JSR-133）和他们处理内存模型的方法其中包含 lock addl 对应的 fence 和其他函数
//介绍了StoreStore,LoadLoad相关

查看该函数的实现
https://github.com/JetBrains/jdk8u_hotspot/blob/c39701c890a5960dfa7c325b78ee97085646f57d/src/os_cpu/linux_x86/vm/orderAccess_linux_x86.inline.hpp
```
inline void OrderAccess::loadload()   { acquire(); }
inline void OrderAccess::storestore() { release(); }
inline void OrderAccess::loadstore()  { acquire(); }
inline void OrderAccess::storeload()  { fence(); }

inline void OrderAccess::acquire() {
  volatile intptr_t local_dummy;
#ifdef AMD64
  __asm__ volatile ("movq 0(%%rsp), %0" : "=r" (local_dummy) : : "memory");
#else
  __asm__ volatile ("movl 0(%%esp),%0" : "=r" (local_dummy) : : "memory");
#endif // AMD64
}

inline void OrderAccess::release() {
  // Avoid hitting the same cache-line from
  // different threads.
  volatile jint local_dummy = 0;
}

inline void OrderAccess::fence() {
  if (os::is_MP()) {
    // always use locked addl since mfence is sometimes expensive   在x86中mfence更昂贵有时，所以使用lock; addl
#ifdef AMD64
    __asm__ volatile ("lock; addl $0,0(%%rsp)" : : : "cc", "memory");
#else
    __asm__ volatile ("lock; addl $0,0(%%esp)" : : : "cc", "memory");
#endif
  }
}
```

lock addl $0x0,(%rsp) 是什么
IA32 中对 lock 的说明是

https://docs.oracle.com/cd/E19455-01/806-3773/instructionset-128/index.html  这是oracle对lock的解读
The LOCK # signal is asserted during execution of the instruction following the lock prefix. 
This signal can be used in a multiprocessor system to ensure exclusive use of shared memory while LOCK # is asserted

LOCK 用于在多处理器中执行指令时对共享内存的独占使用。它的作用是能够将当前处理器对应缓存的内容刷新到内存，并使其他处理器对应的缓存失效。
另外还提供了有序的指令无法越过这个内存屏障的作用。

正是 lock 实现了 volatile 的「防止指令重排」「内存可见」的特性

lock说明  
https://www.intel.com/content/dam/www/public/us/en/documents/manuals/64-ia-32-architectures-software-developer-vol-3a-part-1-manual.pdf
Locked Instructions Have a Total Order
Loads and Stores Are Not Reordered with Locked Instructions

