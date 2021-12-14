具体来说，了解 Java 虚拟机有如下（但不限于）好处。
首先，Java 虚拟机提供了许多配置参数，用于满足不同应用场景下，对程序性能的需求。学习 Java 虚拟机，你可以针对自己的应用，
最优化匹配运行参数
java -XX:+PrintFlagsFinal -XX:+UnlockDiagnosticVMOptions -version | wc -l   打印Java的参数


其次，Java 虚拟机本身是一种工程产品，在实现过程中自然存在不少局限性。学习 Java 虚拟机，可以更好地规避它在使用中的 Bug，
也可以更快地识别出 Java 虚拟机中的错误

再次，Java 虚拟机拥有当前最前沿、最成熟的垃圾回收算法实现，以及即时编译器实现。学习 Java 虚拟机，我们可以了解背后的设计决策，
今后再遇到其他代码托管技术也能触类旁通


最后，Java 虚拟机发展到了今天，已经脱离 Java 语言，形成了一套相对独立的、高性能的执行方案。除了 Java 外，Scala、Clojure、Groovy，
以及时下热门的 Kotlin，这些语言都可以运行在 Java 虚拟机之上。学习 Java 虚拟机，便可以了解这些语言的通用机制，
甚至于让这些语言共享生态系统



Specifications  规格，说明书  
java 说明说
https://docs.oracle.com/javase/specs/index.html

jdk在线
https://github.com/openjdk/jdk

http://hg.openjdk.java.net/jdk8  选择jdk还是hotspot 然后点击browse