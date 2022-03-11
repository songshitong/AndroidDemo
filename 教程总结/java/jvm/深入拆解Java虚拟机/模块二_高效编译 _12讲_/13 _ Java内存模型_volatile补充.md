

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
在每个volatile读操作前插入LoadLoad屏障，在读操作后插入LoadStore屏障；

由于内存屏障的作用，避免了volatile变量和其它指令重排序、线程之间实现了通信，使得volatile表现出了锁的特性


