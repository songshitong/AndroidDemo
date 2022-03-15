为什么要重写equals和hashCode
基于hash工作，不重写发生大量hash冲突，散列表退化为链表和红黑树

https://blog.csdn.net/weixin_42373997/article/details/112085344
hashmap头插法和尾插法区别

https://zhuanlan.zhihu.com/p/114363420
JDK8则因为巧妙的设计，性能有了大大的提升：由于数组的容量是以2的幂次方扩容的，那么一个Entity在扩容时，
新的位置要么在原位置，要么在原长度+原位置的位置

面试题  减少hash碰撞的方式
https://blog.csdn.net/fly910905/article/details/88827657#t4


扩容因子为什么是0.75
https://mp.weixin.qq.com/s/X2J15ricC1QP191EJkZfvA
与泊松分布有关
链表变树的阈值也与泊松分布有关
https://zhuanlan.zhihu.com/p/358138626
