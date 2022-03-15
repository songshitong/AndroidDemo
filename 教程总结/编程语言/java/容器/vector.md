


https://juejin.cn/post/6844904079152381959
Vector有四个构造方法：
```
public Vector() // 使用指定的初始容量和等于零的容量增量构造一个空向量。    
public Vector(int initialCapacity) // 构造一个空向量，使其内部数据数组的大小，其标准容量增量为零。    
public Vector(Collection<? extends E> c)// 构造一个包含指定 collection 中的元素的向量  
public Vector(int initialCapacity, int capacityIncrement)// 使用指定的初始容量和容量增量构造一个空的向量
```
ArrayList和Vector都是用数组实现的，主要有这么四个区别：
1)Vector是多线程安全的，线程安全就是说多线程访问代码，不会产生不确定的结果。而ArrayList不是，这可以从源码中看出，
   Vector类中的方法很多有synchronized进行修饰，这样就导致了Vector在效率上无法与ArrayLst相比；
2)两个都是采用的线性连续空间存储元素，但是当空间充足的时候，两个类的增加方式是不同。
3)Vector可以设置增长因子，而ArrayList不可以。
4)Vector是一种老的动态数组，是线程同步的，效率很低，一般不赞成使用

适用场景：
1.Vector是线程同步的，所以它也是线程安全的，而ArraList是线程异步的，是不安全的。如果不考虑到线程的安全因素，一般用ArrayList效率比较高。
2.如果集合中的元素的数目大于目前集合数组的长度时，在集合中使用数据量比较大的数据，用Vector有一定的优势。
//todo