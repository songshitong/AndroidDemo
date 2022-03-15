典型回答
final 可以用来修饰类、方法、变量，分别有不同的意义，final 修饰的 class 代表不可以继承扩展，final 的变量是不可以修改的，
  而 final 的方法也是不可以重写的（override）。
finally 则是 Java 保证重点代码一定要被执行的一种机制。我们可以使用 try-finally 或者 try-catch-finally 来进行类似关闭 JDBC 连接、
   保证 unlock 锁等动作。
finalize 是基础类 java.lang.Object 的一个方法，它的设计目的是保证对象在被垃圾收集前完成特定资源的回收。
  finalize 机制现在已经不推荐使用，并且在 JDK 9 开始被标记为 deprecated。