上一篇文章中，我们提到可以用“多线程版本的 if”来理解 Guarded Suspension 模式，不同于单线程中的 if，这个“多线程版本的 if”是需要等待的，
而且还很执着，必须要等到条件为真。但很显然这个世界，不是所有场景都需要这么执着，有时候我们还需要快速放弃。

需要快速放弃的一个最常见的例子是各种编辑器提供的自动保存功能。自动保存功能的实现逻辑一般都是隔一定时间自动执行存盘操作，
存盘操作的前提是文件做过修改，如果文件没有执行过修改操作，就需要快速放弃存盘操作。下面的示例代码将自动保存功能代码化了，
很显然 AutoSaveEditor 这个类不是线程安全的，因为对共享变量 changed 的读写没有使用同步，那如何保证 AutoSaveEditor 的线程安全性呢？


class AutoSaveEditor{
  //文件是否被修改过
  boolean changed=false;
  //定时任务线程池
  ScheduledExecutorService ses = 
    Executors.newSingleThreadScheduledExecutor();
  //定时执行自动保存
  void startAutoSave(){
    ses.scheduleWithFixedDelay(()->{
      autoSave();
    }, 5, 5, TimeUnit.SECONDS);  
  }
  //自动存盘操作
  void autoSave(){
    if (!changed) {
      return;
    }
    changed = false;
    //执行存盘操作
    //省略且实现
    this.execSave();
  }
  //编辑操作
  void edit(){
    //省略编辑逻辑
    ......
    changed = true;
  }
}


解决这个问题相信你一定手到擒来了：读写共享变量 changed 的方法 autoSave() 和 edit() 都加互斥锁就可以了。这样做虽然简单，
但是性能很差，原因是锁的范围太大了。那我们可以将锁的范围缩小，只在读写共享变量 changed 的地方加锁，实现代码如下所示。

//自动存盘操作
void autoSave(){
  synchronized(this){
    if (!changed) {
      return;
    }
    changed = false;
  }
  //执行存盘操作
  //省略且实现
  this.execSave();
}
//编辑操作
void edit(){
  //省略编辑逻辑
  ......
  synchronized(this){
    changed = true;
  }
}  

如果你深入地分析一下这个示例程序，你会发现，示例中的共享变量是一个状态变量，业务逻辑依赖于这个状态变量的状态：当状态满足某个条件时，
执行某个业务逻辑，其本质其实不过就是一个 if 而已，放到多线程场景里，就是一种“多线程版本的 if”。这种“多线程版本的 if”的应用场景还是很多的，
所以也有人把它总结成了一种设计模式，叫做 Balking 模式。

Balking 模式的经典实现
Balking 模式本质上是一种规范化地解决“多线程版本的 if”的方案，对于上面自动保存的例子，使用 Balking 模式规范化之后的写法如下所示，
你会发现仅仅是将 edit() 方法中对共享变量 changed 的赋值操作抽取到了 change() 中，这样的好处是将并发处理逻辑和业务逻辑分开。


boolean changed=false;
//自动存盘操作
void autoSave(){
  synchronized(this){
    if (!changed) {
      return;
    }
    changed = false;
  }
  //执行存盘操作
  //省略且实现
  this.execSave();
}
//编辑操作
void edit(){
  //省略编辑逻辑
  ......
  change();
}
//改变状态
void change(){
  synchronized(this){
    changed = true;
  }
}


用 volatile 实现 Balking 模式
前面我们用 synchronized 实现了 Balking 模式，这种实现方式最为稳妥，建议你实际工作中也使用这个方案。不过在某些特定场景下，
也可以使用 volatile 来实现，但使用 volatile 的前提是对原子性没有要求。

在《29 | Copy-on-Write 模式：不是延时策略的 COW》中，有一个 RPC 框架路由表的案例，在 RPC 框架中，本地路由表是要和注册中心进行信息同步的，
应用启动的时候，会将应用依赖服务的路由表从注册中心同步到本地路由表中，如果应用重启的时候注册中心宕机，那么会导致该应用依赖的服务均不可用，
因为找不到依赖服务的路由表。为了防止这种极端情况出现，RPC 框架可以将本地路由表自动保存到本地文件中，如果重启的时候注册中心宕机，
那么就从本地文件中恢复重启前的路由表。这其实也是一种降级的方案。

自动保存路由表和前面介绍的编辑器自动保存原理是一样的，也可以用 Balking 模式实现，不过我们这里采用 volatile 来实现，实现的代码如下所示。
之所以可以采用 volatile 来实现，是因为对共享变量 changed 和 rt 的写操作不存在原子性的要求，而且采用 scheduleWithFixedDelay() 
这种调度方式能保证同一时刻只有一个线程执行 autoSave() 方法

//路由表信息
public class RouterTable {
  //Key:接口名
  //Value:路由集合
  ConcurrentHashMap<String, CopyOnWriteArraySet<Router>> 
    rt = new ConcurrentHashMap<>();    
  //路由表是否发生变化
  volatile boolean changed;
  //将路由表写入本地文件的线程池
  ScheduledExecutorService ses=
    Executors.newSingleThreadScheduledExecutor();
  //启动定时任务
  //将变更后的路由表写入本地文件
  public void startLocalSaver(){
    ses.scheduleWithFixedDelay(()->{
      autoSave();
    }, 1, 1, MINUTES);
  }
  //保存路由表到本地文件
  void autoSave() {
    if (!changed) {
      return;
    }
    changed = false;
    //将路由表写入本地文件
    //省略其方法实现
    this.save2Local();
  }
  //删除路由
  public void remove(Router router) {
    Set<Router> set=rt.get(router.iface);
    if (set != null) {
      set.remove(router);
      //路由表已发生变化
      changed = true;
    }
  }
  //增加路由
  public void add(Router router) {
    Set<Router> set = rt.computeIfAbsent(
      route.iface, r -> 
        new CopyOnWriteArraySet<>());
    set.add(router);
    //路由表已发生变化
    changed = true;
  }
}

Balking 模式有一个非常典型的应用场景就是单次初始化，下面的示例代码是它的实现。这个实现方案中，我们将 init() 声明为一个同步方法，
这样同一个时刻就只有一个线程能够执行 init() 方法；init() 方法在第一次执行完时会将 inited 设置为 true，这样后续执行 init() 
方法的线程就不会再执行 doInit() 了

class InitTest{
  boolean inited = false;
  synchronized void init(){
    if(inited){
      return;
    }
    //省略doInit的实现
    doInit();
    inited=true;
  }
}

线程安全的单例模式本质上其实也是单次初始化，所以可以用 Balking 模式来实现线程安全的单例模式，下面的示例代码是其实现。
这个实现虽然功能上没有问题，但是性能却很差，因为互斥锁 synchronized 将 getInstance() 方法串行化了，那有没有办法可以优化一下它的性能呢？

class Singleton{
  private static
    Singleton singleton;
  //构造方法私有化  
  private Singleton(){}
  //获取实例（单例）
  public synchronized static 
  Singleton getInstance(){
    if(singleton == null){
      singleton=new Singleton();
    }
    return singleton;
  }
}

办法当然是有的，那就是经典的双重检查（Double Check）方案，下面的示例代码是其详细实现。在双重检查方案中，
一旦 Singleton 对象被成功创建之后，就不会执行 synchronized(Singleton.class){}相关的代码，也就是说，
此时 getInstance() 方法的执行路径是无锁的，从而解决了性能问题。不过需要你注意的是，这个方案中使用了 volatile 来禁止编译优化，
其原因你可以参考《01 | 可见性、原子性和有序性问题：并发编程 Bug 的源头》中相关的内容。至于获取锁后的二次检查，则是出于对安全性负责

class Singleton{
  private static volatile 
    Singleton singleton;
  //构造方法私有化  
  private Singleton() {}
  //获取实例（单例）
  public static Singleton 
  getInstance() {
    //第一次检查
    if(singleton==null){
      synchronize(Singleton.class){
        //获取锁后二次检查
        if(singleton==null){
          singleton=new Singleton();
        }
      }
    }
    return singleton;
  }
}


总结
Balking 模式和 Guarded Suspension 模式从实现上看似乎没有多大的关系，Balking 模式只需要用互斥锁就能解决，而 Guarded Suspension 
模式则要用到管程这种高级的并发原语；但是从应用的角度来看，它们解决的都是“线程安全的 if”语义，不同之处在于，Guarded Suspension 
模式会等待 if 条件为真，而 Balking 模式不会等待。

Balking 模式的经典实现是使用互斥锁，你可以使用 Java 语言内置 synchronized，也可以使用 SDK 提供 Lock；如果你对互斥锁的性能不满意，
可以尝试采用 volatile 方案，不过使用 volatile 方案需要你更加谨慎。

当然你也可以尝试使用双重检查方案来优化性能，双重检查中的第一次检查，完全是出于对性能的考量：避免执行加锁操作，因为加锁操作很耗时。
而加锁之后的二次检查，则是出于对安全性负责。双重检查方案在优化加锁性能方面经常用到，例如《17 | ReadWriteLock：如何快速实现一个完备的缓存？》
中实现缓存按需加载功能时，也用到了双重检查方案。

课后思考
下面的示例代码中，init() 方法的本意是：仅需计算一次 count 的值，采用了 Balking 模式的 volatile 实现方式，你觉得这个实现是否有问题呢？

class Test{
  volatile boolean inited = false;
  int count = 0;
  void init(){
    if(inited){
      return;
    }
    inited = true;
    //计算count的值
    count = calc();
  }
}  

zero
是有问题的，volatile关键字只能保证可见性，无法保证原子性和互斥性。所以calc方法有可能被重复执行。

leon
思考题代码相当于：
if（intied == false） { // 1
     inited = true; //2
     count = calc()
}

可能有多条线程同时到1的位置，判断到inited为false，都进入2执行。
解决方案：
（1）加锁保护临界区
（2） AtomicBoolean.compareAndSet(false, true)


