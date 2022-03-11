https://developer.android.com/studio/profile/memory-profiler?hl=zh-cn

Allocation Tracking的级别
Full：捕获内存中的所有对象分配。这是 Android Studio 3.2 及更低版本中的默认行为。如果您有一个分配了大量对象的应用，
    可能会在进行性能剖析时观察到应用的运行速度明显减慢。
Sampled：定期对内存中的对象分配情况进行采样。这是默认选项，在进行性能剖析时对应用性能的影响较小。
    在短时间内分配大量对象的应用仍可能会表现出明显的速度减慢。
Off：停止跟踪应用的内存分配。

内存计数中的类别如下：
Java：从 Java 或 Kotlin 代码分配的对象的内存。

Native：从 C 或 C++ 代码分配的对象的内存。
   即使您的应用中不使用 C++，您也可能会看到此处使用了一些原生内存，因为即使您编写的代码采用 Java 或 Kotlin 语言，
   Android 框架仍使用原生内存代表您处理各种任务，如处理图像资源和其他图形。

Graphics：图形缓冲区队列为向屏幕显示像素（包括 GL 表面、GL 纹理等等）所使用的内存。（请注意，这是与 CPU 共享的内存，不是 GPU 专用内存。）

Stack：您的应用中的原生堆栈和 Java 堆栈使用的内存。这通常与您的应用运行多少线程有关。

Code：您的应用用于处理代码和资源（如 dex 字节码、经过优化或编译的 dex 代码、.so 库和字体）的内存。

Others：您的应用使用的系统不确定如何分类的内存。

Allocated：您的应用分配的 Java/Kotlin 对象数。此数字没有计入 C 或 C++ 中分配的对象。


常见的堆
堆：
default heap：当系统未指定堆时。
image heap：系统启动映像，包含启动期间预加载的类。此处的分配确保绝不会移动或消失。
zygote heap：写时复制堆，其中的应用进程是从 Android 系统中派生的。
app heap：您的应用在其中分配内存的主堆。
JNI heap：显示 Java 原生接口 (JNI) 引用被分配和释放到什么位置的堆。



jni的分析
Java 原生接口 (JNI) 是一个允许 Java 代码和原生代码相互调用的框架。

JNI 引用由原生代码进行管理，因此原生代码使用的 Java 对象可能会保持活动状态过长时间。如果丢弃了 JNI 引用而未先明确将其删除，
  Java 堆上的某些对象可能会变得无法访问。此外，还可能会达到全局 JNI 引用限制。

如需排查此类问题，请使用内存性能分析器中的 JNI heap 视图浏览所有全局 JNI 引用，并按 Java 类型和原生调用堆栈对其进行过滤。
  借助此信息，您可以了解创建和删除全局 JNI 引用的时间和位置。

在您的应用运行时，选择您要检查的一部分时间轴，然后从类列表上方的下拉菜单中选择 JNI heap。 您随后可以像往常一样检查堆中的对象，
  还可以双击 Allocation Call Stack 标签页中的对象，以查看在代码中将 JNI 引用分配和释放到了什么位置



原生内存性能分析器
Android Studio 内存性能分析器包含原生内存性能分析器，该工具适用于部署到搭载 Android 10 的物理设备的应用；
  Android Studio 4.2 预览版现提供对 Android 11 设备的支持。
原生内存性能分析器会跟踪特定时间段内采用原生代码表示的对象的分配/解除分配情况，并提供以下信息：
Allocations：在选定时间段内通过 malloc() 或 new 运算符分配的对象数。
Deallocations：在选定时间段内通过 free() 或 delete 运算符解除分配的对象数。
Allocations Size：在选定时间段内所有分配的总大小（以字节为单位）。
Deallocations Size：在选定时间段内所有已释放内存的总大小（以字节为单位）。
Total Count：Allocations 列中的值减去 Deallocations 列中的值所得的结果。
Remaining Size：Allocations Size 列中的值减去 Deallocations Size 列中的值所得的结果