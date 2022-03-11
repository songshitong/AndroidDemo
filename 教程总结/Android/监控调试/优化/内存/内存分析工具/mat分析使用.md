https://juejin.cn/post/6844904096541966350

Memory Analyzer (MAT)
The Eclipse Memory Analyzer is a fast and feature-rich Java heap analyzer that helps you find memory leaks 
  and reduce memory consumption

下载https://www.eclipse.org/mat/


android使用
从Android Studio进入Profile的Memory视图，选择需要分析的应用进程，对应用进行怀疑有内存问题的操作，结束操作后，主动GC几次，
  最后export dump文件
因为Android Studio保存的是Android Dalvik/ART格式的.hprof文件，所以需要转换成J2SE HPROF格式才能被MAT识别和分析。
  Android SDK自带了一个转换工具在SDK的platform-tools下，其中转换语句为：
```
./hprof-conv file.hprof converted.hprof
```
通过MAT打开转换后的HPROF文件。


MAT视图
在MAT窗口上，OverView是一个总体概览，显示总体的内存消耗情况和疑似问题。MAT提供了多种分析维度，
   其中Histogram、Dominator Tree、Top Consumers和Leak Suspects的分析维度是不同的。下面分别介绍下它们，如下所示：
1、Histogram
列出内存中的所有实例类型对象和其个数以及大小，并在顶部的regex区域支持正则表达式查找。
2、Dominator Tree
列出最大的对象及其依赖存活的Object。相比Histogram，能更方便地看出引用关系。
3、Top Consumers
通过图像列出最大的Object，对降低内存栈优化可用内存比较有帮助
4、Leak Suspects
通过MAT自动分析内存泄漏的原因和泄漏的一份总体报告。




分析内存最常用的是Histogram和Dominator Tree这两个视图，视图中一共有四列：
Class Name：类名。
Objects：对象实例个数。
Shallow Heap：对象自身占用的内存大小，不包括它引用的对象。非数组的常规对象的Shallow Heap Size由其成员变量的数量和类型决定，
  数组的Shallow Heap Size由数组元素的类型（对象类型、基本类型）和数组长度决定。真正的内存都在堆上，看起来是一堆原生的byte[]、char[]、int[]，
  对象本身的内存都很小。因此Shallow Heap对分析内存泄漏意义不是很大。
Retained Heap：是当前对象大小与当前对象可直接或间接引用到的对象的大小总和，包括被递归释放的。即：Retained Size就是当前对象被GC后，
  从Heap上总共能释放掉的内存大小。
   Retained Heap例子:
   一个ArrayList对象持有100个对象，每一个占用16 bytes，如果这个list对象被回收，那么其中100个对象也可以被回收，可以回收16*100 + X的内存，
     X代表ArrayList的shallow大小。
  所以，RetainedHeap可以更精确的反映一个对象实际占用的大小。

Shallow  浅的;肤浅的;浅薄的;(呼吸)浅的，弱的
Retained 保持;保留的;留存;留用;滞留


查看class创建的对象信息
选择一个Class，右键选择List objects > with incoming references


Leak Suspects
Leak Suspects 界面提示可能存在内存的泄露
点开Details进入详情页面，在详情页面Shortest Paths To the Accumulation Point表示GC root到内存消耗聚集点的最短路径，
   如果某个内存消耗聚集点有路径到达GC root，则该内存消耗聚集点不会被当做垃圾被回收。
  //此处存在重复的对象，或者大对象在消耗内存


Dominator_tree
当前所有实例的支配树，和Histogram区别时Histogram是类维度，dominator_tree是实例维度，可以查看所有实例的所占百分比和引用链
  //对前几个进行分析，是否可能发生内存泄露，应该被回收的，占比不合理


Thread_overview
查看当前所有线程信息


查找内存泄漏具体位置
常规方式
1、按照包名类型分类进行实例筛选或直接使用顶部Regex选取特定实例。
2、右击选中被怀疑的实例对象，选择Merge Shortest Paths to GC Root->exclude all phantom/weak/soft etc references。
  (显示GC Roots最短路径的强引用)
 被虚引用/弱引用/软引用的对象可以直接被GC给回收，我们要看的就是某个对象否还存在Strong 引用链（在导出HeapDump之前要手动出发GC来保证），
  如果有，则说明存在内存泄漏，然后再去排查具体引用。
3、分析引用链或通过代码逻辑找出原因。
假如说上面对象此时本来应该是被GC掉的，简单的办法就是将其中的某处置为null或者remove掉，使其到GC Root无路径可达，处于不可触及状态，
  垃圾回收器就可以回收了

还有一种更快速的方法就是对比泄漏前后的HPROF数据：
1、在两个HPROF文件中，把Histogram或者Dominator Tree增加到Compare Basket。
2、在Compare Basket中单击 ! ，生成对比结果视图。这样就可以对比相同的对象在不同阶段的对象实例个数和内存占用大小，如明显只需要一个实例的对象，
  或者不应该增加的对象实例个数却增加了，说明发生了内存泄漏，就需要去代码中定位具体的原因并解决。


如果目标不太明确，可以直接定位RetainedHeap最大的Object，通过Select incoming references查看引用链，定位到可疑的对象，
  然后通过Path to GC Roots分析引用链

当Hash集合中过多的对象返回相同的Hash值时，会严重影响性能，这时可以用 Map Collision Ratio 查找导致Hash集合的碰撞率较高的罪魁祸首


