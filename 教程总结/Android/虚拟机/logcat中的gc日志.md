https://blog.csdn.net/h_uang_Sir/article/details/105754879
https://developer.android.com/studio/debug/am-logcat.html#ARTLogMessages

Dalvik 日志消息
在 Dalvik（而不是 ART）中，每个 GC 都会将以下信息输出到 logcat 中：
```
D/dalvikvm(PID): GC_Reason Amount_freed, Heap_stats, External_memory_stats, Pause_time
```
dalvik更多的查看文档，这更多分析art

ART 日志消息
与 Dalvik 不同，ART 不会为未明确请求的 GC 记录消息。只有在系统认为 GC 速度较慢时才会输出 GC 消息。更确切地说，
仅在 GC 暂停时间超过 5 毫秒或 GC 持续时间超过 100 毫秒时。如果应用未处于可察觉到暂停的状态（例如应用在后台运行时，这种情况下，
用户无法察觉 GC 暂停），则其所有 GC 都不会被视为速度较慢。系统一直会记录显式 GC。 //通过调用System.gc()或者runtime.gc()

ART 会在其垃圾回收日志消息中包含以下信息：
```
I/art: GC_Reason GC_Name Objects_freed(Size_freed) AllocSpace Objects,
    Large_objects_freed(Large_object_size_freed) Heap_stats LOS objects, Pause_time(s)
 
I/art(801): Explicit concurrent mark sweep GC freed 65595(3MB) AllocSpace objects, 
    9(4MB) LOS objects, 34% free, 38MB/58MB, paused 1.195ms total 87.219ms      
```

GC Reason        Explicit 	触发垃圾回收事件的原因
GC Name          concurrent mark sweep GC   ART具有可以运行的各种不同的GC
Objects freed    65595(3MB)	从非大对象空间收集的对象垃圾数量。在此事件中，共有65595个唯一对象，其累积大小为3mb，是垃圾收集（即释放）的。
Large objects freed	9(4MB)	从大对象空间收集的对象垃圾数量。总共9个唯一对象，它们的累积大小为4mb，是垃圾收集（即释放）的
Heap usage	   38MB/58MB	在此特定的GC事件之后，38mb的对象仍然存在。此应用程序分配的堆总大小为58mb。
GC Pause Time	1.195ms	   在GC事件的某些阶段，应用程序被暂停。在此GC事件中，暂停时间为1.195ms。在暂停时间内，应用程序冻结。应该以低暂停时间为目标。
GC Total Time	87.219ms	此GC事件完成所需的时间。它还包括GC暂停时间

官方建议
如果您在 logcat 中看到大量 GC，请注意堆统计数据（上面示例中的 38MB/58MB 值）的增大情况。如果此值继续增大，且始终没有变小的趋势，
可能会出现内存泄漏。或者，如果您看到原因为“Alloc”的 GC，则您已快要达到堆容量上限，并且很快会出现 OOM 异常

不同类型的GC 原因
在Android运行时（ART）环境中，由于以下原因之一，可能会触发垃圾收集活动：
#	          描述
Concurrent	并发GC不会挂起应用程序线程。该GC在后台线程中运行，并且不会阻止分配。
Alloc	    启动GC是因为您的应用程序在堆已满时尝试分配内存。在这种情况下，垃圾回收发生在分配线程中。
Explicit	应用程序明确请求了垃圾回收，例如，通过调用gc()。与Dalvik一样，在ART中，最佳做法是您信任GC，并尽可能避免请求显式GC。
            不建议使用显式GC，因为它们会阻塞分配线程并不必要地浪费CPU周期。如果显式GC导致其他线程被抢占，
            则它们也可能导致卡顿（应用程序中的冻结，颤动或暂停）。
NativeAlloc	该收集是由本地分配（例如，位图或RenderScript分配对象）的本地内存压力引起的。
CollectorTransition  	由堆转换引起的回收；这由在运行时变更 GC 策略引起（例如应用在可察觉到暂停的状态之间切换时）。
                        回收器转换包括将所有对象从空闲列表空间复制到碰撞指针空间（反之亦然）。
                       回收器转换仅在以下情况下出现：在 Android 8.0 之前的低内存设备上，应用将进程状态从可察觉到暂停的状态
                        （例如应用在前台运行时，这种情况下，用户可以察觉 GC 暂停）更改为察觉不到暂停的状态（反之亦然）
HomogeneousSpaceCompact	 同构空间压缩是空闲列表空间到空闲列表空间压缩，通常在应用进入到察觉不到暂停的进程状态时发生。
                        这样做的主要原因是减少内存使用量并对堆进行碎片整理。
DisableMovingGc  	这不是真正的 GC 原因，但请注意，由于在发生并发堆压缩时使用了 GetPrimitiveArrayCritical，回收遭到阻止。
                 一般情况下，强烈建议不要使用 GetPrimitiveArrayCritical，因为它在移动回收器方面存在限制
HeapTrim	这不是 GC 原因，但请注意，在堆修剪完成之前，回收会一直受到阻止。



不同类型的GC名称
在Android运行时（ART）环境中，垃圾收集可能是以下类型之一：
#	                         描述
Concurrent mark sweep (CMS)	  整个堆回收器，会释放和回收除映像空间以外的所有其他空间。
Concurrent partial mark sweep	几乎整个堆回收器，会回收除映像空间和 Zygote 空间以外的所有其他空间。
Concurrent sticky mark sweep	分代回收器，只能释放自上次 GC 后分配的对象。此垃圾回收比完整或部分标记清除运行得更频繁，
                                 因为它更快速且暂停时间更短。
Mark sweep + semispace	一种非并发的复制GC，用于堆转换以及同构空间压缩（对堆进行碎片整理）。


GC 类型
GC 类型有如下三种：
1）、Full：与Dalvik的 FULL GC 差不多。
2）、Partial：跟 Dalvik 的局部 GC 差不多，策略时不包含 Zygote Heap。
3）、Sticky：另外一种局部中的局部 GC，选择局部的策略是上次垃圾回收后新分配的对象