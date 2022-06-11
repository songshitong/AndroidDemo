https://mp.weixin.qq.com/s/WE_LlG3Ho4i4K-AYvTFuDw

1. 什么是OOM
   OOM是Out Of Memory的缩写。即内存溢出

2. 为什么会发生OOM
   如果面试官问这个问题，我知道每个人都能说一大堆，但是我希望大家就回答下面三句话，言简意赅：
   1 业务正常运行起来就需要比较多的内存，而给JVM设置的内存过小。具体表现就是程序跑不起来，或者跑一会就挂了。
   2 GC回收内存的速度赶不上程序运行消耗内存的速度。出现这种情况一般就是往list、map中填充大量数据，内存紧张时JVM拆东墙补西墙补不过来了。
      所以查询记得分页啊！不需要的字段，尤其是数据量大的字段，就不要返回了！比如文章的内容。
  3 存在内存泄漏情况，久而久之也会造成OOM。哪些情况会造成内存泄漏呢？比如打开文件不释放、创建网络连接不关闭、不再使用的对象未断开引用关系
    、使用静态变量持有大对象引用……

3. 哪些区域会发生OOM
JVM运行时数据区五个区域中，除了程序计数器不会发生OOM，其他区域都有可能。
除了元空间、本地方法栈、虚拟机栈、堆外，还有一块区域大家容易忽略，即直接内存。
不知道什么是直接内存？看这张图
Java_jvm_内存区域_直接内存.jpeg


4. JVM挂了有哪些可能性
从Windows系统角度说，JVM进程如果不是你手动关闭的，那就是OOM导致的。但是在Linux下就不一定了，因为Linux系统有一种保护机制：OOM Killer。
   这个机制如果展开来说又能说一堆，这里我就大概说下吧，这个机制是Unix内核独有的，它的出现是为了保证系统在可用内存较少的情况下依旧能够运行，
   会选择杀掉一些分值较高的进程来回收内存。这个分值是Unix内核根据一些参数动态计算出来的，当然，我们也可以改变，感兴趣的小伙伴百度学习吧。
   作为Java程序员，了解到这个程度基本够用了，再底层的话，很多面试官也不知道，也不会问。
除了OOM Killer，剩下的就是OOM导致JVM进程挂了。


5. 生产环境如何快速定位问题
   前面说了，算上直接内存，共有五个区域会发生OOM：直接内存、元空间、本地方法栈、虚拟机栈、元空间。
本地方法栈与虚拟机栈的OOM咱们可以不用管，为什么呢？因为这两个区域的OOM你在开发阶段或在测试阶段就能发现。GET到了吗？小伙伴们。
   所以这两个区域的OOM是不会生成dump文件的。

好，开始正题。如果生产环境JVM挂了，这时候不要慌，有节奏的来分析来排除。首先排除是不是被Linux杀死了，怎么看呢？
通过命令[sudo egrep -i -r 'Out Of' /var/log]查看，如果是，关闭一些服务，或者把一些服务移走，腾出点内存。
```
/var/Log/syslog:Mar 4 23:34:02 ubuntu kernel: [223457.074614] Out of memory: Kill process 40408 (java) score 518 or sacrifice child
/var/Log/syslog:Mar 4 23:40:15 ubuntu kernel: [ 284.220317] Out of memory: KILL process 3768 (java) score 751 or sacrifice chitd
```

如果不是，这时候就可以确定是OOM导致的，那具体是哪个OOM导致的呢？看有没有生成dump文件。如果生成了，要么是堆OOM，要么是元空间OOM；
如果没生成，直接可以确定是直接内存导致的OOM。怎么解决呢？调优呗
```
java_pid484239.a2s.index
java_pid484239.domIn.index
java_pid484239.domOut.index
java_pid484239.hprof  //dump生成的hprof
java_pid484239.idx.index
java_pid484239.inbound.index
java_pid484239.index
java_pid484239.o2c.index
java_pid484239.o2hprof.index
java_pid484239.o2ret.index
java_pid484239.outbound.index
java_pid484239.threads
```

我这边是生成了的，所以需要进一步排查，是堆OOM还是元空间OOM。这时候需要把dump文件从服务器上下载下来，用visualvm分析。
当前其他工具如MAT、JProfiler都可以，我习惯用visualvm。很多小伙伴不会看dump日志哈，子牙老师教给你诀窍，学会了，受益无穷
HeapOverFlowTest2
```
Exception in thread "main" java.lang.OutOfMemoryError: Java heap space
	at HeapOverFlowTest2.<init>(HeapOverFlowTest2.java:7) //创建对象
	at HeapOverFlowTest2.run(HeapOverFlowTest2.java:23)
	at HeapOverFlowTest2.main(HeapOverFlowTest2.java:10)
```
如果你发现发生OOM的位置是创建对象，调用构造方法之类的代码，那一定是堆OOM。<init>就是构造方法的字节码格式。所以学点JVM底层知识还是有必要的啊
MetaspaceOverFlowTest.java
```
 "main" prio=5 tid=1 RUNNABLE
at javalang.OutOfMemoryError.<init> (OutOFMemoryErrorjava:48)
at java.lang.ClassLoader. defineClass1 (Native Method)   //类加载器
at java.lang. ClassLoader. defineClass (ClassLoader java:762)
at sun reflect. GeneratedMethodAccessort invoke (<unknown string>)
at sun reflect DelegatingMethodAccessorimpl invoke (DelegatingMethodAccessorimpljava:43)
at java lang.reflect. Method invoke (Method java:497)
at net.sf.cglib.core Reflect Utils defineClass (ReflectUtils java: 459)
at net.sf.cqlib. core AbstractClassGenerator.generate (AbstractClassGenerator java: 339)
at net.sf.cglib.proxy.Enhancer.generate (Enhancer java492)
at net.sf.cglib.core.AbstractClassGenerator$ClassLoaderData. get (AbstractClassGenerator.java:117)
at net.sf.cqlib. core AbstractClassGenerator. create (AbstractClassGenerator java:294) 
at net.sF.cglib.proxy EnhancercreateHetper(Enhancerjava'480) 
at net.sF.cglib.proxy.Enhancer. create (Enhancer java:305)
```
如果你发现发生OOM的位置是类加载器那些方法，那一定是元空间OOM。

怎么样，学会了吗？

如果发生OOM让JVM自动dump内存的设置你没开，那你可以跑路了，老板正在赶来的路上，手上拿着大刀！
-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp

idea设置VM options   jvm设置最大20M
-Xms20M
-Xmx20M
-XX:+HeapDumpOnOutOfMemoryError  
-verbose:gc

测试代码 https://gitee.com/songshitong/luban-jvm-research.git
DirectMemoryOOM.java
直接内存溢出
```
Exception in thread "main" java.lang.OutOfMemoryError
	at sun.misc.Unsafe.allocateMemory(Native Method)
	at oom.DirectMemoryOOM.main(DirectMemoryOOM.java:27)
OpenJDK 64-Bit Server VM warning: Attempt to deallocate stack guard pages failed.
OpenJDK 64-Bit Server VM warning: INFO: os::commit_memory(0x00007fafcdc88000, 12288, 0) failed; error='无法分配内存' (errno=12)
```

GCOverheadTest.java
gc频繁，但是可用空间不足
```
Exception in thread "main" java.lang.OutOfMemoryError: GC overhead limit exceeded
	at java.lang.Integer.toString(Integer.java:403)
	at java.lang.Integer.toString(Integer.java:935)
	at oom.GCOverheadTest.main(GCOverheadTest.java:18)
```

调用栈溢出
JavaVMStackSOF.java
```
stack length:18186
Exception in thread "main" java.lang.StackOverflowError
	at oom.JavaVMStackSOF.stackLeak(JavaVMStackSOF.java:13)
	at oom.JavaVMStackSOF.stackLeak(JavaVMStackSOF.java:13)
	at oom.JavaVMStackSOF.stackLeak(JavaVMStackSOF.java:13)
	at oom.JavaVMStackSOF.stackLeak(JavaVMStackSOF.java:13)
	....都是这一行
```

常量池溢出   测试时full gc一直触发  将short改为double类型才触发oom
RuntimeConstantPoolOOM.java
```
Exception in thread "main" java.lang.OutOfMemoryError: Java heap space
	at java.util.HashMap.newNode(HashMap.java:1750)
	at java.util.HashMap.putVal(HashMap.java:642)
	at java.util.HashMap.put(HashMap.java:612)
	at java.util.HashSet.add(HashSet.java:220)
	at oom.RuntimeConstantPoolOOM.main(RuntimeConstantPoolOOM.java:18)
```