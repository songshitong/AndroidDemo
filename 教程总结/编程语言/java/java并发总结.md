

并发编程的三个核心问题：分工、同步、互斥。
分工指的是如何高效地拆解任务并分配给线程
  Executor、Fork/Join、Future
  并发设计模式：生产者 - 消费者、Thread-Per-Message、Worker Thread 模式
同步指的是线程之间如何协作
  Executor、Fork/Join、Future
  CountDownLatch、CyclicBarrier、Phaser、Exchanger
  核心思想管程
互斥则是保证同一时刻只允许一个线程访问共享资源
  核心思想管程，实现互斥的核心技术就是锁，synchronized，Lock,ReadWriteLock、StampedLock
  无锁方案：原子类
  其他方案：不共享变量或者变量只允许读  Thread Local，Copy-on-write，final    //空间换时间
   //Copy-on-write或者读写锁  读的数据不是最新的，cpu缓存可能没有及时更新到缓存，另一个CPU可能没有及时读
   //Volatile 及时更新缓存


并发相关
java的内存模型   //其实并不是线程间内存共享，每个线程都有自己的私有区域：程序计数器，Java方法栈，native方法栈
Java内存模型规定了所有的变量都存储在主内存中，每条线程还有自己的工作内存，线程的工作内存中保存了该线程中是用到的变量的主内存副本拷贝，
线程对变量的所有操作都必须在工作内存中进行，而不能直接读写主内存。不同的线程之间也无法直接访问对方工作内存中的变量，
线程间变量的传递均需要自己的工作内存和主存之间进行数据同步进行

原子性
count += 1，至少需要三条 CPU 指令。
指令 1：首先，需要把变量 count 从内存加载到 CPU 的寄存器；
指令 2：之后，在寄存器中执行 +1 操作；
指令 3：最后，将结果写入内存（缓存机制导致可能写入的是 CPU 缓存而不是内存）

操作系统做任务切换，可以发生在任何一条 CPU 指令执行完
我们假设 count=0，如果线程 A 在指令 1 执行完后做线程切换，线程 A 和线程 B 按照下图的序列执行，
那么我们会发现两个线程都执行了 count+=1 的操作，但是得到的结果不是我们期望的 2，而是 1

我们把一个或者多个操作在 CPU 执行的过程中不被中断的特性称为原子性。CPU 能保证的原子操作是 CPU 指令级别的，而不是高级语言的操作符，
这是违背我们直觉的地方。因此，很多时候我们需要在高级语言层面保证操作的原子性

在Java中可以使用synchronized来保证方法和代码块内的操作是原子性的


可见性
一个线程对共享变量的修改，另外一个线程能够立刻看到，我们称为可见性。
多核时代，每颗 CPU 都有自己的缓存，这时 CPU 缓存与内存的数据一致性就没那么容易解决了，当多个线程在不同的 CPU 上执行时，
这些线程操作的是不同的 CPU 缓存。
比如下图中，线程 A 操作的是 CPU-1 上的缓存，而线程 B 操作的是 CPU-2 上的缓存，很明显，
这个时候线程 A 对变量 V 的操作对于线程 B 而言就不具备可见性了
volatile volatile禁用缓存(对这个变量的读写，不能使用 CPU 缓存，必须从内存中读取或者写入)和编译优化
除了volatile，Java中的synchronized和final两个关键字也可以实现可见性，happens-before
Happens-before 前面一个操作的结果对后续操作是可见的，约束了编译器的优化行为，虽允许编译器优化，
但是要求编译器优化后一定遵守 Happens-Before 规则
1. 程序的顺序性规则
   这条规则是指在一个线程中，按照程序顺序，前面的操作对于后续的任意操作是可见的
2. volatile 变量规则    对一个 volatile 变量的写操作相对于后续对这个 volatile 变量的读操作可见
3. 传递性这条规则是指如果 A Happens-Before B，且 B Happens-Before C，那么 A Happens-Before C
4. 管程中锁的规则
   对一个锁的解锁 Happens-Before 于后续对这个锁的加锁
5. 线程 start() 规则
   它是指主线程 A 启动子线程 B 后，子线程 B 能够看到主线程在启动子线程 B 前的操作
6. 线程 join() 规则
   主线程 A 等待子线程 B 完成（主线程 A 通过调用子线程 B 的 join() 方法实现），
   当子线程 B 完成后（主线程 A 中 join() 方法返回），主线程能够看到子线程的操作

有序性
有序性指的是程序按照代码的先后顺序执行
编译器为了优化性能，有时候会改变程序中语句的先后顺序
```
public class Singleton {
  static Singleton instance;
  static Singleton getInstance(){
    if (instance == null) {
      synchronized(Singleton.class) {
        if (instance == null)
          instance = new Singleton();
        }
    }
    return instance;
  }
}
```
我们以为的 new 操作应该是：
分配一块内存 M；
在内存 M 上初始化 Singleton 对象； //在内存初始化变量这一步比第三步慢，可能只是个CPU指令
然后 M 的地址赋值给 instance 变量。  

但是实际上优化后的执行路径却是这样的：
分配一块内存 M；
将 M 的地址赋值给 instance 变量；
最后在内存 M 上初始化 Singleton 对象。

我们假设线程 A 先执行 getInstance() 方法，当执行完指令 2 时恰好发生了线程切换，切换到了线程 B 上；
如果此时线程 B 也执行 getInstance() 方法，那么线程 B 在执行第一个判断时会发现 instance != null ，
所以直接返回 instance，而此时的 instance 是没有初始化过的，如果我们这个时候访问 instance 的成员变量就可能触发空指针异常。

解决有序性  volatile会禁止指令重排

为什么synchronized保证可见性不能解决
Synchronized的解锁对于后面的加锁可见，但是没有解决有序性


synchronized使用
锁静态方法synchronized static void bar()  锁住了class
锁方法synchronized void foo()   锁住了对象
锁代码块
```
void baz() {
    synchronized(obj) {
      // 临界区
    }
  }
```
双重检查 对于需要判断条件才加锁的情况进行double check    DLC
```
getInstance() {
    //第一次检查
    if(singleton==null){
      synchronized(Singleton.class){
        //获取锁后二次检查
        if(singleton==null){
          singleton=new Singleton();
        }
      }
    }
    return singleton;
  }
```
synchronized的实现   如何体现的是非公平锁，唤醒任意一个等待的队列
1 字节码实现：
当声明 synchronized 代码块时，编译而成的字节码将包含 monitorenter 和 monitorexit 指令。这两种指令均会消耗操作数栈上的
一个引用类型的元素（也就是 synchronized 关键字括号里的引用），作为所要加锁解锁的锁对象
当sychronized声明方法时，会有ACC_SYNCHRONIZED标记，该标记表示在进入该方法时，Java 虚拟机需要进行 monitorenter 操作。
而在退出该方法时，不管是正常返回，还是向调用者抛异常，Java 虚拟机均需要进行 monitorexit 操作

关于 monitorenter 和 monitorexit 的作用，我们可以抽象地理解为每个锁对象拥有一个锁计数器和一个指向持有该锁的线程的指针。
当执行 monitorenter 时，如果目标锁对象的计数器为 0，那么说明它没有被其他线程所持有。在这个情况下，Java 虚拟机会将该锁对象的持有线程
设置为当前线程，并且将其计数器加 1。

在目标锁对象的计数器不为 0 的情况下，如果锁对象的持有线程是当前线程，那么 Java 虚拟机可以将其计数器加 1，否则需要等待，直至持有线程释放该锁

当执行 monitorexit 时，Java 虚拟机则需将锁对象的计数器减 1。当计数器减为 0 时，那便代表该锁已经被释放掉了。
之所以采用这种计数器的方式，是为了允许同一个线程重复获取同一把锁。举个例子，如果一个 Java 类中拥有多个 synchronized 方法，
那么这些方法之间的相互调用，不管是直接的还是间接的，都会涉及对同一把锁的重复加锁操作。因此，我们需要设计这么一个可重入的特性，
来避免编程里的隐式约束
2 jvm实现
Java 虚拟机中 synchronized 关键字的实现，按照代价由高至低可分为重量级锁、轻量级锁和偏向锁三种。
重量级锁会阻塞、唤醒请求加锁的线程。它针对的是多个线程同时竞争同一把锁的情况。Java 虚拟机采取了自适应自旋，来避免线程在面对非常小的
synchronized 代码块时，仍会被阻塞、唤醒的情况
Java 虚拟机会阻塞加锁失败的线程，并且在目标锁被释放的时候，唤醒这些线程。 Java 线程的阻塞以及唤醒，都是依靠操作系统来完成的
比如Linux的pthread 的互斥锁mutex，这些操作将涉及系统调用，需要从操作系统的用户态切换至内核态，其开销非常之大
为了尽量避免昂贵的线程阻塞、唤醒操作，Java 虚拟机会在线程进入阻塞状态之前，以及被唤醒后竞争不到锁的情况下，进入自旋状态，
在处理器上空跑并且轮询锁是否被释放。如果此时锁恰好被释放了，那么当前线程便无须进入阻塞状态，而是直接获得这把锁

轻量级锁采用 CAS 操作，将锁对象的标记字段替换为一个指针，指向当前线程栈上的一块空间，存储着锁对象原本的标记字段。它针对的是多个线程
在不同时间段申请同一把锁的情况，也就是说没有锁竞争   //todo 实现

偏向锁只会在第一次请求时采用 CAS 操作，在锁对象的标记字段中记录下当前线程的地址。在之后的运行过程中，持有该偏向锁的线程的加锁操作将直接返回。
它针对的是锁仅会被同一线程持有的情况。  //对于同一个线程多次申请锁的情况，后面直接通过了，偏向哪个线程
当请求加锁的线程和锁对象标记字段保持的线程地址不匹配时，Java 虚拟机需要撤销该偏向锁，如果总撤销数超过另一个阈值，
Java 虚拟机会撤销该类实例的偏向锁，并且在之后的加锁过程中直接为该类实例设置轻量级锁   阈值默认20


死锁，活锁，饥饿锁
死锁：一组互相竞争资源的线程因互相等待，导致“永久”阻塞的现象
```
 // 转账
  void transfer(Account target, int amt){
    // 锁定转出账户
    synchronized(this){     ①
      // 锁定转入账户
      synchronized(target){ ②
        if (this.balance > amt) {
          this.balance -= amt;
          target.balance += amt;
        }
      }
    }
  } 
```
如何预防死锁
并发程序一旦死锁，一般没有特别好的方法，很多时候我们只能重启应用。因此，解决死锁问题最好的办法还是规避死锁。
这四个条件都发生时才会出现死锁：
1 互斥，共享资源 X 和 Y 只能被一个线程占用；
2 占有且等待，线程 T1 已经取得共享资源 X，在等待共享资源 Y 的时候，不释放共享资源 X；
3 不可抢占，其他线程不能强行抢占线程 T1 占有的资源；
4 循环等待，线程 T1 等待线程 T2 占有的资源，线程 T2 等待线程 T1 占有的资源，就是循环等待。
互斥没办法解决
解决2 占用且等待 可以一次性申请所有的资源，这样就不存在等待
增加第三方管理者，来管理共享资源，对于转账操作，一个是转入账户，一个是转出账户
解决3 不可抢占  要能够主动释放它占有的资源，这一点 synchronized 是做不到的，可以使用lock
解决4 循环等待  需要对资源进行排序，然后按序申请资源
我们假设每个账户都有不同的属性 id，这个 id 可以作为排序字段，申请的时候，我们可以按照从小到大的顺序来申请

活锁
线程虽然没有发生阻塞，但仍然会存在执行不下去的情况
现实世界里的例子，路人甲从左手边出门， 路人乙从右手边进门，两人为了不相撞，互相谦让，路人甲让路走右手边，路人乙也让路走左手边，
结果是两人又相撞了。这种情况，基本上谦让几次就解决了，因为人会交流啊。可是如果这种情况发生在编程世界了，就有可能会一直没完没了地“谦让”下去，
成为没有发生阻塞但依然执行不下去的“活锁”
解决“活锁”的方案很简单，谦让时，尝试等待一个随机的时间就可以了。例如上面的那个例子，路人甲走左手边发现前面有人，并不是立刻换到右手边，
而是等待一个随机的时间后，再换到右手边；同样，路人乙也不是立刻切换路线，也是等待一个随机的时间再切换。
由于路人甲和路人乙等待的时间是随机的，所以同时相撞后再次相撞的概率就很低了

饥饿锁
所谓“饥饿”指的是线程因无法访问所需资源而无法执行下去的情况
如果线程优先级“不均”， 在 CPU 繁忙的情况下，优先级低的线程得到执行的机会很小，就可能发生线程“饥饿”；持有锁的线程，如果执行的时间过长，
也可能导致“饥饿”问题
解决“饥饿”问题的方案很简单，有三种方案：一是保证资源充足，二是公平地分配资源，三就是避免持有锁的线程长时间执行。这三个方案中，
方案一和方案三的适用场景比较有限，因为很多场景下，资源的稀缺性是没办法解决的，持有锁的线程执行的时间也很难缩短。
倒是方案二的适用场景相对来说更多一些。
那如何公平地分配资源呢？在并发编程里，主要是使用公平锁。所谓公平锁，是一种先来后到的方案，线程的等待是有顺序的，
排在等待队列前面的线程会优先获得资源   ReentrantLock传入true表示公平锁，false非公平锁

volatile
理想情况下对 volatile 字段的使用应当多读少写，并且应当只有一个线程进行写操作
实现：
1. 在每个volatile写操作前插入StoreStore屏障，在写操作后插入StoreLoad屏障；
   在每个volatile读操作后插入LoadLoad，LoadStore屏障；
2.volatile的原理是在生成的汇编代码中多了一个lock前缀指令，这个前缀指令相当于一个内存屏障，这个内存屏障有3个作用：
确保指令重排的时候不会把屏障后的指令排在屏障前，确保不会把屏障前的指令排在屏障后。
修改缓存中的共享变量后立即刷新到主存中。
当执行写操作时会导致其他CPU中的缓存无效  //其他cpu立即从内存更新缓存，mesi协议？

//todo 其他源码的总结 cas  aqs  unsafe.park


ThreadLocal
ThreadLocal的作用是提供线程内的局部变量，说白了，就是在各线程内部创建一个变量的副本，相比于使用各种锁机制访问变量，
ThreadLocal的思想就是用空间换时间，使各线程都能访问属于自己这一份的变量副本，变量值不互相干扰，
减少同一个线程内的多个函数或者组件之间一些公共变量传递的复杂度
使用,重写initialValue方法
```
   private static final ThreadLocal<Integer> threadId =
           new ThreadLocal<Integer>() {
               @Override protected Integer initialValue() {
                   return nextId.getAndIncrement();
           }
       };
```
remove函数用来删除ThreadLocal绑定的值，需要手动调用，防止内存泄露


线程的6中状态
NEW（初始化状态）
RUNNABLE（可运行 / 运行状态）
BLOCKED（阻塞状态）
WAITING（无时限等待）
TIMED_WAITING（有时限等待）
TERMINATED（终止状态）

线程的常见方法
Thread.sleep(long millis)，一定是当前线程调用此方法，当前线程进入TIMED_WAITING状态，但不释放对象锁，millis后线程自动苏醒进入就绪状态。
作用：给其它线程执行机会的最佳方式
Thread.yield()，一定是当前线程调用此方法，当前线程放弃获取的CPU时间片，但不释放锁资源，由运行状态变为就绪状态，让OS再次选择线程。
作用：让相同优先级的线程轮流执行，但并不保证一定会轮流执行。实际中无法保证yield()达到让步目的，因为让步的线程还有可能被线程调度程序再次选中。
Thread.yield()不会导致阻塞。该方法与sleep()类似，只是不能由用户指定暂停多长时间
thread.join()/thread.join(long millis)，当前线程里调用其它线程thread的join方法，当前线程进入WAITING/TIMED_WAITING状态，
当前线程不会释放已经持有的对象锁。线程thread执行完毕或者millis时间到，当前线程进入就绪状态
thread.interrupt(),当前线程里调用其它线程thread的interrupt()方法,中断指定的线程。
如果指定线程调用了wait()方法组或者join方法组在阻塞状态，那么指定线程会抛出InterruptedException
Thread.interrupted()，一定是当前线程调用此方法，检查当前线程是否被设置了中断，该方法会重置当前线程的中断标志，返回当前线程是否被设置了中断
thread.isInterrupted()，当前线程里调用其它线程thread的isInterrupted()方法,返回指定线程是否被中断
thread.stop已经被废弃无法使用了，抛出不支持操作的异常UnsupportedOperationException


Object.wait() / Object.notify() Object.notifyAll()
任意一个Java对象，都拥有一组监视器方法（定义在java.lang.Object上）这些方法与synchronized同步关键字配合，可以实现等待/通知模式
obj.wait()是无限等待，直到obj.notify()或者obj.notifyAll()调用并唤醒该线程，该线程获取锁之后继续执行代码
obj.wait(long millis)是超时等待，我只等待long millis 后，该线程会自己醒来，醒来之后去获取锁，获取锁之后继续执行代码
obj.notify()是叫醒任意一个等待在该对象上的线程，该线程获取锁，线程状态从BLOCKED进入RUNNABLE
obj.notifyAll()是叫醒所有等待在该对象上的线程，这些线程会去竞争锁，得到锁的线程状态从BLOCKED进入RUNNABLE，其他线程依然是BLOCKED,
得到锁的线程执行代码完毕后释放锁，其他线程继续竞争锁，如此反复直到所有线程执行完毕  尽量使用NotifyAll而不是Notify

调用obj.wait()/obj.wait(long millis)，该线程会释放obj的锁，并阻塞
调用obj.notify()或者obj.notifyAll()，该线程会释放obj的锁，并叫醒在obj上等待的线程，然后线程重新竞争
使用
```
synchronized(obj){ 
//判断条件，这里使用while(obj满足/不满足 某个条件)
{ obj.wait() } }
```
放在while里面，是防止处于WAITING状态下线程监测的对象被别的原因调用了唤醒（notify或者notifyAll）方法，
但是while里面的条件并没有满足（也可能当时满足了，但是由于别的线程操作后，又不满足了），就需要再次调用wait将其挂起

Thread.sleep 和 Object.wait 的区别
sleep 方法是 Thread 类中的静态方法，wait 是 Object 类中的方法
sleep 并不会释放同步锁(其他不能获得锁)，而 wait 会释放同步锁(其他可获得锁)    todo 源码中怎么实现的
sleep 可以在任何地方使用，而 wait 只能在同步方法或者同步代码块中使用
sleep 中必须传入时间，而 wait 可以传，也可以不传，不传时间的话只有 notify 或者 notifyAll 才能唤醒，传时间的话在时间之后会自动唤醒
//为什么sleep是thread  wait是Object
//线程只能暂停，休眠     锁是记录在对象头的，object.wait可以释放锁

线程终止  两阶段终止模式
```
new Thread(()->{
      while (true) {
        try {
          Thread.sleep(2000);
        } catch (InterruptedException e) {  
        }
      }
    });
synchronized void stop(){
    rptThread.interrupt();
  }    
```

Java中生产者与消费者模式
生产者消费者模式要保证的是当缓冲区满的时候生产者不再生产对象，当缓冲区空时，消费者不再消费对象。
实现机制就是当缓冲区满时让生产者处于等待状态，当缓冲区为空时让消费者处于等待状态。当生产者生产了一个对象后会唤醒消费者，
当消费者消费一个对象后会唤醒生产者。
三种实现方式
1 wait 和 notify
```
//wait和notify
import java.util.LinkedList;
public class StorageWithWaitAndNotify {
    private final int                MAX_SIZE = 10;
    private       LinkedList<Object> list     = new LinkedList<Object>();
    public void produce() {
        synchronized (list) {
            while (list.size() == MAX_SIZE) {
                System.out.println("仓库已满：生产暂停");
                try {
                    list.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            list.add(new Object());
            System.out.println("生产了一个新产品，现库存为：" + list.size());
            list.notifyAll();
        }
    }
    public void consume() {
        synchronized (list) {
            while (list.size() == 0) {
                System.out.println("库存为0：消费暂停");
                try {
                    list.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            list.remove();
            System.out.println("消费了一个产品，现库存为：" + list.size());
            list.notifyAll();
        }
    }
}
```
2 BlockingQueue
```
import java.util.concurrent.LinkedBlockingQueue;
public class StorageWithBlockingQueue {
    private final int                         MAX_SIZE = 10;
    private       LinkedBlockingQueue<Object> list     = new LinkedBlockingQueue<Object>(MAX_SIZE);
    public void produce() {
        if (list.size() == MAX_SIZE) {
            System.out.println("缓冲区已满，暂停生产");
        }
        try {
            list.put(new Object());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("生产了一个产品，现容量为：" + list.size());
    }
    public void consume() {
        if (list.size() == 0) {
            System.out.println("缓冲区为空，暂停消费");
        }
        try {
            list.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("消费了一个产品，现容量为：" + list.size());
    }
}
```
3 await 和 signal
```
import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
class StorageWithAwaitAndSignal {
    private final int                MAX_SIZE = 10;
    private       ReentrantLock      mLock    = new ReentrantLock();
    private       Condition          mEmpty   = mLock.newCondition();
    private       Condition          mFull    = mLock.newCondition();
    private       LinkedList<Object> mList    = new LinkedList<Object>();
    public void produce() {
        mLock.lock();
        while (mList.size() == MAX_SIZE) {
            System.out.println("缓冲区满，暂停生产");
            try {
                mFull.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        mList.add(new Object());
        System.out.println("生产了一个新产品，现容量为：" + mList.size());
        mEmpty.signalAll();
        mLock.unlock();
    }
    public void consume() {
        mLock.lock();
        while (mList.size() == 0) {
            System.out.println("缓冲区为空，暂停消费");
            try {
                mEmpty.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        mList.remove();
        System.out.println("消费了一个产品，现容量为：" + mList.size());
        mFull.signalAll();
        mLock.unlock();
    }
}
```
线程池
为什么要使用线程池   控制线程数量，减少线程创建与销毁，减少线程上下文切换
线程池参数
```
 public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              ThreadFactory threadFactory,
                              RejectedExecutionHandler handler) {}
构造方法中有7个参数之多，我们逐个来看每个参数所代表的含义：
corePoolSize 表示线程池的核心线程数。当有任务提交到线程池时，如果线程池中的线程数小于corePoolSize,那么则直接创建新的线程来执行任务。
workQueue 任务队列，它是一个阻塞队列，用于存储来不及执行的任务的队列。当有任务提交到线程池的时候，如果线程池中的线程数大于等于corePoolSize，
    那么这个任务则会先被放到这个队列中，等待执行。
maximumPoolSize 表示线程池支持的最大线程数量。当一个任务提交到线程池时，线程池中的线程数大于corePoolSize,并且workQueue已满，
  那么则会创建新的线程执行任务，但是线程数要小于等于maximumPoolSize。
keepAliveTime 非核心线程空闲时保持存活的时间。非核心线程即workQueue满了之后，再提交任务时创建的线程，因为这些线程不是核心线程，
  所以它空闲时间超过keepAliveTime后则会被回收。
unit 非核心线程空闲时保持存活的时间的单位
threadFactory 创建线程的工厂，可以在这里统一处理创建线程的属性
handler 拒绝策略，当线程池中的线程达到maximumPoolSize线程数后且workQueue已满的情况下，再向线程池提交任务则执行对应的拒绝策略
```
从数据结构的角度来看，线程池主要使用了阻塞队列（BlockingQueue）和HashSet集合构成。 从任务提交的流程角度来看，对于使用线程池的外部来说，
线程池的机制是这样的：
```
1、如果正在运行的线程数 < coreSize，马上创建核心线程执行该task，不排队等待；
2、如果正在运行的线程数 >= coreSize，把该task放入阻塞队列；  task进行等待
3、如果队列已满 && 正在运行的线程数 < maximumPoolSize，创建新的非核心线程执行该task；
4、如果队列已满 && 正在运行的线程数 >= maximumPoolSize，线程池调用handler的reject方法拒绝本次提交。
```
理解记忆：1-2-3-4对应（核心线程->阻塞队列->非核心线程->handler拒绝提交）

线程池的拒绝策略
在JDK中提供了
RejectedExecutionHandler接口来执行拒绝操作。实现RejectedExecutionHandler的类有四个，对应了四种拒绝策略。分别如下：
1 DiscardPolicy 当提交任务到线程池中被拒绝时，线程池会丢弃这个被拒绝的任务
2 DiscardOldestPolicy 当提交任务到线程池中被拒绝时，线程池会丢弃等待队列中最老的任务。
3 CallerRunsPolicy 当提交任务到线程池中被拒绝时，会在线程池当前正在运行的Thread线程中处理被拒绝额任务。即哪个线程提交的任务哪个线程去执行。
4 AbortPolicy 当提交任务到线程池中被拒绝时，直接抛出RejectedExecutionException异常。 默认的拒绝策略
使用建议:
自定义自己的拒绝策略,并和降级策略配合使用。

常见的四个线程池  已经不建议这么创建了
```
// 实例化一个单线程的线程池
ExecutorService singleExecutor = Executors.newSingleThreadExecutor();
// 创建固定线程个数的线程池
ExecutorService fixedExecutor = Executors.newFixedThreadPool(10);
// 创建一个可重用固定线程数的线程池
ExecutorService executorService2 = Executors.newCachedThreadPool();
创建一个定长线程池，支持定时及周期性任务执行
ExecutorService scheduledExecutor  = Executors.newScheduledThreadPool(10);
```

线程池的线程复用：  
这里就需要深入到源码addWorker()：它是创建新线程的关键，也是线程复用的关键入口。最终会执行到runWorker，它取任务有两个方式：
firstTask：这是指定的第一个runnable可执行任务，它会在Worker这个工作线程中运行执行任务run。并且置空表示这个任务已经被执行。
getTask()：这首先是一个死循环过程，工作线程循环直到能够取出Runnable对象或超时返回，这里的取的目标就是任务队列workQueue，
对应刚才入队的操作，有入有出。
其实就是任务在并不只执行创建时指定的firstTask第一任务，还会从任务队列的中通过getTask()方法自己主动去取任务执行，
而且是有/无时间限定的阻塞等待，保证线程的存活

线程复用  将线程包装为Worker然后存储在HashSet，核心线程没满将Runnable添加到Worker的firstTask;核心线程满了，将任务存储在阻塞队列BlockingQueue
线程第一次添加到Workers时，用ThreadFactory创建，然后启动线程
线程运行run时会从firstTask或者getTask从阻塞队列WorkQueue中取出Runnable执行，没有任务进行阻塞
firstTask执行完后进行阻塞，阻塞队列添加新任务后唤醒，取出任务继续执行
线程回收 线程run从阻塞队列WorkQueue取任务超时后返回null，进行线程回收，将worker从HashSet移除

线程池都有哪几种工作队列？   todo
1、ArrayBlockingQueue
是一个基于数组结构的有界阻塞队列，此队列按 FIFO（先进先出）原则对元素进行排序。
2、LinkedBlockingQueue
一个基于链表结构的阻塞队列，此队列按FIFO （先进先出） 排序元素，吞吐量通常要高于ArrayBlockingQueue。
静态工厂方法Executors.newFixedThreadPool()和Executors.newSingleThreadExecutor使用了这个队列。
3、SynchronousQueue
一个不存储元素的阻塞队列。每个插入操作必须等到另一个线程调用移除操作，否则插入操作一直处于阻塞状态，吞吐量通常要高于LinkedBlockingQueue，
静态工厂方法Executors.newCachedThreadPool使用了这个队列。
4、PriorityBlockingQueue
一个具有优先级的无限阻塞队列。

怎么理解无界队列和有界队列
有界队列
队列存在大小限制，在线程池中，达到有界队列满了之后执行拒绝策略
无界队列
与有界队列相比，除非系统资源耗尽，否则无界的任务队列不存在任务入队失败的情况
若任务创建和处理的速度差异很大，无界队列会保持快速增长，直到耗尽系统内存

多线程中的安全队列一般通过什么实现？
Java提供的线程安全的Queue可以分为阻塞队列和非阻塞队列，其中阻塞队列的典型例子是BlockingQueue，
非阻塞队列的典型例子是ConcurrentLinkedQueue.
对于BlockingQueue，想要实现阻塞功能，需要调用put(e) take() 方法。而ConcurrentLinkedQueue是基于链接节点的、无界的、线程安全的非阻塞队列

如何设置线程数量
CPU密集型  CPU核数+1
IO密集型  CPU 核数 * [ 1 +（I/O 耗时 / CPU 耗时）]  最好进行压测调整




synchronized关键字和Lock的区别你知道吗？为什么Lock的性能好一些？   
类别  synchronized                             Lock（底层实现主要是Volatile + CAS）
存在层次 Java的关键字，在jvm层面上                  是一个类
锁的释放 1、已获取锁的线程执行完同步代码，释放锁         在finally中必须释放锁，不然容易造成线程死锁。
2、线程执行发生异常，jvm会让线程释放锁。   
锁的获取 假设A线程获得锁，B线程等待。如果A线程阻塞，      分情况而定，Lock有多个锁获取的方式，大致就是可以尝试获得锁，线程可以不用一直等待
B线程会一直等待。
锁状态   无法判断                                 可以判断
锁类型  可重入 不可中断 非公平                      可重入 可判断 可公平（两者皆可）
性能    少量同步                                 大量同步

Lock（ReentrantLock）的底层实现主要是Volatile + CAS（乐观锁），而Synchronized是一种悲观锁，比较耗性能。
但是在JDK1.6以后对Synchronized的锁机制进行了优化，加入了偏向锁、轻量级锁、自旋锁、重量级锁，在并发量不大的情况下，
性能可能优于Lock机制。所以建议一般请求并发量不大的情况下使用synchronized关键字

//todo 伪共享 https://mp.weixin.qq.com/s?__biz=MzkxNDEyOTI0OQ==&mid=2247484432&idx=1&sn=7a221a539746252c23dc190fd7131b6c&source=41#wechat_redirect

原子类Atomic
CPU提供的CAS 指令（CAS，全称是 Compare And Swap，即“比较并交换”）
使用范式：自旋+cas
```
do {
      newValue = count+1; 
    }while(count != cas(count,newValue) 
```
ABA 问题
更新为A，又被更新B，最后再被更新为A，发现A没变
解决：增加一个版本号，相关实现AtomicReference、AtomicStampedReference，AtomicMarkableReference
优点：
无锁方案相对互斥锁方案，最大的好处就是性能。互斥锁方案为了保证互斥性，需要执行加锁、解锁操作，而加锁、解锁操作本身就消耗性能；
同时拿不到锁的线程还会进入阻塞状态，进而触发线程切换，线程切换对性能的消耗也很大。 相比之下，无锁方案则完全没有加锁、解锁的性能消耗
缺点：
可能出现饥饿和活锁问题，因为自旋会反复重试，此时的自旋会消耗大量的CPU资源在空转
原子类的方法都是针对一个共享变量的，如果你需要解决多个变量的原子性问题，建议还是使用互斥锁方案


CountDownLatch
主要用来解决一个线程等待多个线程的场景
CountDownLatch 的计数器是不能循环利用的
```
// 创建2个线程的线程池
Executor executor = Executors.newFixedThreadPool(2);
while(存在未对账订单){
  // 计数器初始化为2
  CountDownLatch latch = new CountDownLatch(2);
  // 查询未对账订单
  executor.execute(()-> {
    pos = getPOrders();
    latch.countDown();
  });
  // 查询派送单
  executor.execute(()-> {
    dos = getDOrders();
    latch.countDown();
  });
  
  // 等待两个查询操作结束
  latch.await();
  
  // 执行对账操作
  diff = check(pos, dos);
  // 差异写入差异库
  save(diff);
}
```

CyclicBarrier 
与CountDownLatch类似，计数器是可以循环利用的，而且具备自动重置的功能，一旦计数器减到 0 会自动重置到你设置的初始值。
除此之外，还可以设置回调函数
//适用于t3等待t2和t1，完成后重置计数器，继续等待的情况
```
// 订单队列
Vector<P> pos;
// 派送单队列
Vector<D> dos;
// 执行回调的线程池 
Executor executor = Executors.newFixedThreadPool(1);
final CyclicBarrier barrier = new CyclicBarrier(2, ()->{
       executor.execute(()->check());
  });
  
void check(){
  P p = pos.remove(0);
  D d = dos.remove(0);
  // 执行对账操作
  diff = check(p, d);
  // 差异写入差异库
  save(diff);
}
  
void checkAll(){
  // 循环查询订单库
  Thread T1 = new Thread(()->{
    while(存在未对账订单){
      // 查询订单库
      pos.add(getPOrders());
      // 等待
      barrier.await();
    }
  });
  T1.start();  
  // 循环查询运单库
  Thread T2 = new Thread(()->{
    while(存在未对账订单){
      // 查询运单库
      dos.add(getDOrders());
      // 等待
      barrier.await();
    }
  });
  T2.start();
}
```


Future接口 
获取任务的执行结果，任务取消
适用于任务之间有依赖关系，比如当前任务依赖前一个任务的执行结果
1 Future future  = ThreadPoolExecutor.submit(Runnable)
2 FutureTask 实现了Runnable和Future接口，可以配合Thread和线程池使用
3 CompletableFuture 提供了异步编程的方式，可以用来描述串行关系，描述AND汇聚关系，描述 OR 汇聚关系
  例如汇聚关系f3 = f1.thenCombine(f2, ()->{}) 

ReadWriteLock
读写锁，适用于读多写少场景
允许多个线程同时读共享变量；
只允许一个线程写共享变量；
如果一个写线程正在执行写操作，此时禁止读线程读共享变量。
实现一个缓存
```
class Cache<K,V> {
  final Map<K, V> m = new HashMap<>();
  final ReadWriteLock rwl = new ReentrantReadWriteLock();
  // 读锁
  final Lock r = rwl.readLock();
  // 写锁
  final Lock w = rwl.writeLock();
  // 读缓存
  V get(K key) {
    r.lock();
    try { return m.get(key); }
    finally { r.unlock(); }
  }
  // 写缓存
  V put(K key, V value) {
    w.lock();
    try { return m.put(key, v); }
    finally { w.unlock(); }
  }
}
```

StampedLock  todo    lost wake up problem todo

CompletionService
CompletionService 将线程池 Executor 和阻塞队列 BlockingQueue的功能融合在了一起
批量提交异步任务，能够让异步任务的执行结果有序化， 先执行完的先进入阻塞队列
```
ExecutorService executor = 
  Executors.newFixedThreadPool(3);
// 创建CompletionService
CompletionService<Integer> cs = new ExecutorCompletionService<>(executor);
// 异步向电商S1询价
cs.submit(()->getPriceByS1());
// 异步向电商S2询价
cs.submit(()->getPriceByS2());
// 异步向电商S3询价
cs.submit(()->getPriceByS3());
// 将询价结果异步保存到数据库
for (int i=0; i<3; i++) {
  Integer r = cs.take().get();
  executor.execute(()->save(r));
}
```

信号量模型
信号量实现互斥
```
static int count;
//初始化信号量
static final Semaphore s = new Semaphore(1);
//用信号量保证互斥    
static void addOne() {
  s.acquire();
  try {
    count+=1;
  } finally {
    s.release();
  }
}
```
信号量实现限流器
允许多个线程访问一个临界区，例如连接池、对象池、线程池等
```
class ObjPool<T, R> {
  final List<T> pool;
  // 用信号量实现限流器
  final Semaphore sem;
  // 构造函数
  ObjPool(int size, T t){
    pool = new Vector<T>(){};
    for(int i=0; i<size; i++){
      pool.add(t);
    }
    //限流的大小
    sem = new Semaphore(size);
  }
  // 利用对象池的对象，调用func
  R exec(Function<T,R> func) {
    T t = null;
    sem.acquire();
    try {
      t = pool.remove(0);
      return func.apply(t);
    } finally {
      pool.add(t);
      sem.release();
    }
  }
}
// 创建对象池
ObjPool<Long, String> pool = new ObjPool<Long, String>(10, 2);
// 通过对象池获取t，之后执行  
pool.exec(t -> {
    System.out.println(t);
    return t.toString();
});
```

Fork/Join 计算框架
ForkJoinTask的fork() 方法会异步地执行一个子任务，join() 方法则会阻塞当前线程来等待子任务的执行结果
```
static void main(String[] args){
  //创建分治任务线程池  
  ForkJoinPool fjp =  new ForkJoinPool(4);
  //创建分治任务
  Fibonacci fib = new Fibonacci(30);   
  //启动分治任务  
  Integer result = fjp.invoke(fib);
  //输出结果  
  System.out.println(result);
}
//递归任务
static class Fibonacci extends RecursiveTask<Integer>{
  final int n;
  Fibonacci(int n){this.n = n;}
  protected Integer compute(){
    if (n <= 1)
      return n;
    Fibonacci f1 =  new Fibonacci(n - 1);
    //创建子任务  
    f1.fork();
    Fibonacci f2 = new Fibonacci(n - 2);
    //等待子任务结果，并合并结果  
    return f2.compute() + f1.join();
  }
}
```

AQS todo


所谓管程，指的是管理共享变量以及对共享变量的操作过程，让他们支持并发。翻译为 Java 领域的语言，就是管理类的成员变量和成员方法，
  让这个类是线程安全的
管程是如何解决互斥问题的？
管程解决互斥问题的思路很简单，就是将共享变量及其对共享变量的操作统一封装起来
管程如何解决线程间的同步问题？
管程里入了条件变量的概念，而且每个条件变量都对应有一个等待队列
管程实现的阻塞队列
对于阻塞队列的入队操作，如果阻塞队列已满，就需要等待直到阻塞队列不满，所以这里用了notFull.await();。
对于阻塞出队操作，如果阻塞队列为空，就需要等待直到阻塞队列不空，所以就用了notEmpty.await();。
如果入队成功，那么阻塞队列就不空了，就需要通知条件变量：阻塞队列不空notEmpty对应的等待队列。
如果出队成功，那就阻塞队列就不满了，就需要通知条件变量：阻塞队列不满notFull对应的等待队列
//不满足条件使用Condition.await()，满足条件使用Condition.signal
```
public class BlockedQueue<T>{
  final Lock lock =new ReentrantLock();
  // 条件变量：队列不满  
  final Condition notFull =lock.newCondition();
  // 条件变量：队列不空  
  final Condition notEmpty = lock.newCondition();

  // 入队
  void enq(T x) {
    lock.lock();
    try {
      while (队列已满){
        // 等待队列不满 
        notFull.await();
      }  
      // 省略入队操作...
      //入队后,通知可出队
      notEmpty.signal();
    }finally {
      lock.unlock();
    }
  }
  // 出队
  void deq(){
    lock.lock();
    try {
      while (队列已空){
        // 等待队列不空
        notEmpty.await();
      }
      // 省略出队操作...
      //出队后，通知可入队
      notFull.signal();
    }finally {
      lock.unlock();
    }  
  }
}
```
数据竞争，静态条件


互斥锁  //锁的概念更倾向于许可，拿到锁就对线程操作变量的许可
互斥：同一时刻只有一个线程执行
一段需要互斥执行的代码称为临界区。线程在进入临界区之前，首先尝试加锁 lock()，如果成功，则进入临界区，此时我们称这个线程持有锁；
否则呢就等待，直到持有锁的线程解锁；持有锁的线程执行完临界区的代码后，执行解锁 unlock()

数据竞争（Data Race）
当多个线程同时访问同一数据，并且至少有一个线程会写这个数据的时候，如果我们不采取防护措施，那么就会导致并发 Bug
竞态条件，指的是程序的执行结果依赖线程执行的顺序
```
CountDownLatch latch = new CountDownLatch(2);
Test1 test1 = new Test1();
    new Thread(()->{
        test1.add10K();
        latch.countDown();
    }).start();
new Thread(()->{
    test1.add10K();
    latch.countDown();
}).start();
latch.await();
System.out.println(test1.get());
```
如果两个线程完全同时执行，那么结果是 1；如果两个线程是前后执行，那么结果就是 2

用锁的最佳实践  Doug Lea《Java 并发编程：设计原则与模式》  加锁原则
永远只在更新对象的成员变量时加锁
永远只在访问可变的成员变量时加锁
永远不在调用其他对象的方法时加锁

读写锁
允许多个线程同时读共享变量；
只允许一个线程写共享变量；
如果一个写线程正在执行写操作，此时禁止读线程读共享变量。
应用场景读多写少场景，缓存
锁的升级
先是获取读锁，然后再升级为写锁
读锁还没有释放，此时获取写锁，会导致写锁永久等待，最终导致相关线程都被阻塞，永远也没有机会被唤醒
ReadWriteLock 并不支持锁升级，支持锁的降级

可重入锁与非可重入锁
可重入锁又名递归锁，是指同一个线程在获取外层同步方法锁的时候，再进入该线程的内层同步方法会自动获取锁（前提锁对象得是同一个对象或者class），
   不会因为之前已经获取过还没释放而阻塞。
  ReentrantLock
非可重入锁与可重入锁是对立的关系，即一个线程在获取到外层同步方法锁后，再进入该方法的内层同步方法无法获取到锁，即使锁是同一个对象
NonReentrantLock  todo https://blog.csdn.net/zdhpdf/article/details/122211297
```
public class NonReentrantLock implements Lock, Serializable {
    //静态内部类，用于辅助
    private static class Sync extends AbstractQueuedSynchronizer {
        @Override
        protected boolean tryAcquire(int arg) {
            assert arg == 1;//如果state为0，则尝试获取锁
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }
 
        @Override
        protected boolean tryRelease(int arg) {
            assert arg == 1;//如果state为0，则尝试获取锁
            if (getState() == 0) {
                throw new IllegalMonitorStateException();
            }
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }
 
        @Override
        protected boolean isHeldExclusively() {
            // 是否锁已经被持有
            return getState() == 1;
        }
 
        //提供条件变量接口
        public Condition newCondition() {
            return new ConditionObject();
        }
    }
 
    Sync sync = new Sync();
 
    @Override
    public void lock() {
        sync.acquire(1);
    }
 
    @Override
    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }
 
    @Override
    public boolean tryLock() {
        return sync.tryAcquire(1);
    }
 
    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireNanos(1, unit.toNanos(time));
    }
 
    @Override
    public void unlock() {
        sync.release(1);
    }
 
    @Override
    public Condition newCondition() {
        return sync.newCondition();
    }
}
```


排他锁与共享锁
排他锁也叫独占锁，是指该锁一次只能被一个线程所持有。如果线程T对数据A加上排它锁后，则其他线程不能再对A加任何类型的锁。
获得排它锁的线程即能读数据又能修改数据。
共享锁是指该锁可被多个线程所持有。如果线程T对数据A加上共享锁后，则其他线程只能对A再加共享锁，不能加排它锁。获得共享锁的线程只能读数据，
不能修改数据


公平锁与非公平锁
公平锁是指多个线程按照申请锁的顺序来获取锁，线程直接进入同步队列中排队，队列中最先到的线程先获得锁。
非公平锁是多个线程加锁时每个线程都会先去尝试获取锁，如果刚好获取到锁，那么线程无需等待，直接执行，如果获取不到锁才会被加入
同步队列的队尾等待执行
公平锁和非公平锁各有优缺点，适用于不同的场景。
公平锁的优点在于各个线程公平平等，每个线程等待一段时间后，都有执行的机会，而它的缺点相较于于非公平锁整体执行速度更慢，吞吐量更低。
同步队列中除第一个线程以外的所有线程都会阻塞，CPU唤醒阻塞线程的开销比非公平锁大。
而非公平锁非公平锁的优点是可以减少唤起线程的开销，整体的吞吐效率高，因为线程有几率不阻塞直接获得锁，CPU不必唤醒所有线程。
它的缺点呢也比较明显，即队列中等待的线程可能一直或者长时间获取不到锁。

乐观锁：乐观锁在操作数据时非常乐观，认为别人不会同时修改数据。因此乐观锁不会上锁，只是在执行更新的时候判断一下在此期间别人是否修改了数据：
  如果别人修改了数据则放弃操作，否则执行操作。  CAS
悲观锁：悲观锁在操作数据时比较悲观，认为别人会同时修改数据。因此操作数据时直接把数据锁住，直到操作完成后才会释放锁；上锁期间其他人不能修改数据。
  synchronized