
1 idea设置vm options  该选项可能隐藏，设置modify options
2 java --xx


堆内存
-Xms20M    jvm初始最小内存   默认是物理内存的1/64
-Xmx20M    jvm最大内存   默认是物理内存的1/4
新生代：
-Xmn：设置年轻代大小
-Xmn2G：设置年轻代大小为2G。
-XX:NewSize设置新生代最小空间大小。
-XX:MaxNewSize设置新生代最大空间大小。
永久代  元空间
-XX:PermSize设置永久代最小空间大小。
-XX:MaxPermSize设置永久代最大空间大小。
元空间
-XX:MetaspaceSize   元空间初始大小  默认100M    测试时最小不能低于9M  -XX:MetaspaceSize=9M
-XX:MaxMetaspaceSize 元空间最大可分配大小

方法区
-XX:MetaspaceSize 默认21MB(64位JVM)，达到该值则会进行full gc进行类型加载，同时收集器对值进行调整。
-XX:MaxMetaspaceSize 默认无限(64位JVM)，即只限制于本地内存大小

栈
-Xss 默认1M，该值设置的越小，说明一个线程栈里面能分配的栈帧就越少，但是对JVM整体来说能开启的线程数就越多
设置每个线程的堆栈大小

日志
-verbose:gc

异常相关
-XX:+HeapDumpOnOutOfMemoryError   默认在项目目录
-XX:HeapDumpPath=/tmp   dump目录设置


GC相关
-XX:+PrintGCDetails