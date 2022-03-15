https://juejin.cn/post/6844904079152381959#heading-104
https://juejin.cn/post/6844904136937324552


JVM内存区域的划分，哪些区域会发生 OOM
JVM 的内存区域可以分为两类：线程私有和区域和线程共有的区域。 
 线程私有的区域：程序计数器、JVM 虚拟机栈、本地方法栈 
  线程共有的区域：堆、方法区、运行时常量池
程序计数器。 每个线程有有一个私有的程序计数器，任何时间一个线程都只会有一个方法正在执行，也就是所谓的当前方法。
  程序计数器存放的就是这个当前方法的JVM指令地址。
JVM虚拟机栈。 创建线程的时候会创建线程内的虚拟机栈，栈中存放着一个个的栈帧，对应着一个个方法的调用。JVM 虚拟机栈有两种操作，
   分别是压栈和出栈。栈帧中存放着局部变量表、方法返回值和方法的正常或异常退出的定义等等。
本地方法栈。 跟 JVM 虚拟机栈比较类似，只不过它支持的是 Native 方法。
堆。 堆是内存管理的核心区域，用来存放对象实例。几乎所有创建的对象实例都会直接分配到堆上。所以堆也是垃圾回收的主要区域，
  垃圾收集器会对堆有着更细的划分，最常见的就是把堆划分为新生代和老年代。
方法区。方法区主要存放类的结构信息，比如静态属性和方法等等。
运行时常量池。运行时常量池位于方法区中，主要存放各种常量信息。

其实除了程序计数器，其他的部分都会发生 OOM。
堆。 通常发生的 OOM 都会发生在堆中，最常见的可能导致 OOM 的原因就是内存泄漏。
JVM虚拟机栈和本地方法栈。 当我们写一个递归方法，这个递归方法没有循环终止条件，最终会导致 StackOverflow 的错误。
  当然，如果栈空间扩展失败，也是会发生 OOM 的。
方法区。方法区现在基本上不太会发生 OOM，但在早期内存中加载的类信息过多的情况下也是会发生 OOM 的。



GC机制
垃圾回收需要完成两件事：找到垃圾，回收垃圾。 
找到垃圾一般的话有两种方法：
引用计数法： 当一个对象被引用时，它的引用计数器会加一，垃圾回收时会清理掉引用计数为0的对象。
  但这种方法有一个问题，比方说有两个对象 A 和 B，A 引用了 B，B 又引用了 A，除此之外没有别的对象引用 A 和 B，
  那么 A 和 B 在我们看来已经是垃圾对象，需要被回收，但它们的引用计数不为 0，没有达到回收的条件。
  正因为这个循环引用的问题，Java 并没有采用引用计数法。
可达性分析法： 我们把 Java 中对象引用的关系看做一张图，从根级对象不可达的对象会被垃圾收集器清除。
   这个算法的实质在于将一系列 GC Roots 作为初始的存活对象合集（live set），
      然后从该合集出发，探索所有能够被该集合引用到的对象，并将其加入到该集合中，这个过程我们也称之为标记（mark）。最终，未被探索到的对象便是死亡的，
      是可以回收的。
  gcroot根级对象一般包括 Java 虚拟机栈中的对象、本地方法栈中的对象、方法区中的静态对象和常量池中的常量，已启动且未停止的 Java 线程。
回收垃圾的话有这么四种方法：
标记清除算法： 顾名思义分为两步，标记和清除。首先标记到需要回收的垃圾对象，然后回收掉这些垃圾对象。
   标记清除算法的缺点是清除垃圾对象后会造成内存的碎片化。
复制算法： 复制算法是将存活的对象复制到另一块内存区域中，并做相应的内存整理工作。
   复制算法的优点是可以避免内存碎片化，缺点也显而易见，它需要两倍的内存。
标记整理算法： 标记整理算法也是分两步，先标记后整理。它会标记需要回收的垃圾对象，清除掉垃圾对象后会将存活的对象压缩，
   避免了内存的碎片化。
分代算法： 分代算法将对象分为新生代和老年代对象。那么为什么做这样的区分呢？主要是在Java运行中会产生大量对象，
  这些对象的生命周期会有很大的不同，有的生命周期很长，有的甚至使用一次之后就不再使用。
  所以针对不同生命周期的对象采用不同的回收策略，这样可以提高GC的效率

新生代对象分为三个区域：Eden 区和两个 Survivor 区。新创建的对象都放在 Eden区，当 Eden 区的内存达到阈值之后会触发 Minor GC，
 这时会将存活的对象复制到一个 Survivor 区中，这些存活对象的生命存活计数会加一。这时 Eden 区会闲置，当再一次达到阈值触发 Minor GC 时，
  会将Eden区和之前一个 Survivor 区中存活的对象复制到另一个 Survivor 区中，采用的是我之前提到的复制算法，同时它们的生命存活计数也会加一

这个过程会持续很多遍，直到对象的存活计数达到一定的阈值后会触发一个叫做晋升的现象：新生代的这个对象会被放置到老年代中。
   老年代中的对象都是经过多次 GC 依然存活的生命周期很长的 Java 对象。当老年代的内存达到阈值后会触发 Major GC，采用的是标记整理算法

Full GC: 收集整个堆，包括新生代，老年代，永久代(在 JDK 1.8 及以后，永久代PermGen被移除，换为 metaspace 元空间)等所有部分的模式
//todo metaspace

STW
在 Java 虚拟机里，传统的垃圾回收算法采用的是一种简单粗暴的方式，那便是 Stop-the-world，停止其他非垃圾回收线程的工作，直到完成垃圾回收。
这也就造成了垃圾回收所谓的暂停时间（GC pause）   阻塞式GC

垃圾回收器
针对新生代的垃圾回收器共有三个：Serial，Parallel Scavenge 和 Parallel New。 这三个采用的都是标记 - 复制算法。
其中，Serial 是一个单线程的，
Parallel New 可以看成 Serial 的多线程版本。Parallel Scavenge 和 Parallel New 类似，但更加注重吞吐率。
此外，Parallel Scavenge 不能与 CMS 一起使用

垃圾回收器也有三个Serial Old 和 Parallel Old，以及 CMS
Serial Old 和 Parallel Old 都是标记-整理算法。 同样，前者是单线程的，而后者可以看成前者的多线程版本。
CMS 采用的是标记 - 清除算法，并且是并发的。除了少数几个操作需要 Stop-the-world 之外，它可以在应用程序运行过程中进行垃圾回收。
  在并发收集失败的情况下，Java 虚拟机会使用其他两个压缩型垃圾回收器进行一次垃圾回收。由于 G1 的出现，CMS 在 Java 9 中已被废弃[3]。

G1（Garbage First）是一个横跨新生代和老年代的垃圾回收器。实际上，它已经打乱了前面所说的堆结构，直接将堆分成极其多个区域。
每个区域都可以充当 Eden 区、Survivor 区或者老年代中的一个。它采用的是标记-整理算法，而且和 
  CMS 一样都能够在应用程序运行过程中并发地进行垃圾回收。

G1 能够针对每个细分的区域来进行垃圾回收。在选择进行垃圾回收的区域时，它会优先回收死亡对象较多的区域。这也是 G1 名字的由来



类加载过程
Java 中类加载分为 3 个步骤：加载、链接、初始化。
加载。 加载是将字节码数据从不同的数据源读取到JVM内存，并映射为 JVM 认可的数据结构，也就是 Class 对象的过程
  。数据源可以是 Jar 文件、Class 文件等等。如果数据的格式并不是 ClassFile 的结构，则会报 ClassFormatError。
链接。链接是类加载的核心部分，这一步分为 3 个步骤：验证、准备、解析。
    验证。 验证是保证JVM安全的重要步骤。JVM需要校验字节信息是否符合规范，避免恶意信息和不规范数据危害JVM运行安全。如果验证出错，
      则会报VerifyError。
    准备。 这一步会创建静态变量，并为静态变量开辟内存空间。
    解析。 这一步会将符号引用替换为直接引用。  如果符号引用指向一个未被加载的类，或者未被加载类的字段或方法，那么解析将触发这个类的加载（
        但未必触发这个类的链接以及初始化。）
初始化。 初始化会为静态变量赋值，并执行静态代码块中的逻辑。

对象布局 todo


双亲委派模型    //todo 对象布局
类加载器大致分为3类：启动类加载器、扩展类加载器、应用程序类加载器。
启动类加载器主要加载 jre/lib下的jar文件。   bootstrap class loader  c++实现
扩展类加载器主要加载 jre/lib/ext 下的jar文件。   extension class loader
应用程序类加载器主要加载 classpath 下的文件。     application class loader
所谓的双亲委派模型就是当加载一个类时，会优先使用父类加载器加载，当父类加载器无法加载时才会使用子类加载器去加载。
  这么做的目的是为了避免类的重复加载

Android中的类加载器
PathClassLoader，只能加载系统中已经安装过的 apk
DexClassLoader，可以加载 jar/apk/dex，可以从 SD卡中加载未安装的 apk

编译器语法糖
1.自动装箱、自动拆箱，是通过加入[Wrapper].valueOf（如 Integer.valueOf）以及[Wrapper].[primitive]Value
（如 Integer.intValue）方法调用来实现的
2. Java 程序中的泛型信息会被擦除。具体来说，Java 编译器将选取该泛型所能指代的所有类中层次最高的那个，作为替换泛型的具体类
由于 Java 语义与 Java 字节码中关于重写的定义并不一致，因此 Java 编译器会生成桥接方法作为适配器。
3. foreach遍历iterator  for (Integer item : list)  ArrayList<Integer> list
   jvm转为iterator遍历
4. foreach遍历数组  for (int item : array)   int[] array   
   转为普通for遍历   for (int i = 0; i < length; i++)
5. switch   将switch的变量转为hashCode并用equals方法判断，减少哈希碰撞的影响   
todo try-with-resources   finally  catch代码块中捕获多种异常
6. java10 var  var value =1
  转换为int value = 1;


并发相关
java的内存模型   //其实并不是线程间内存共享
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
在内存 M 上初始化 Singleton 对象；
然后 M 的地址赋值给 instance 变量。

但是实际上优化后的执行路径却是这样的：
分配一块内存 M；
将 M 的地址赋值给 instance 变量；
最后在内存 M 上初始化 Singleton 对象。

我们假设线程 A 先执行 getInstance() 方法，当执行完指令 2 时恰好发生了线程切换，切换到了线程 B 上；
如果此时线程 B 也执行 getInstance() 方法，那么线程 B 在执行第一个判断时会发现 instance != null ，
所以直接返回 instance，而此时的 instance 是没有初始化过的，如果我们这个时候访问 instance 的成员变量就可能触发空指针异常。

解决有序性  volatile会禁止指令重排

todo 有的说synchronized也可以保证可见性   synchronized关键字保证同一时刻只允许一条线程操作

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
synchronized的实现
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
1. happens-before中对volatile  对一个 volatile 变量的写操作相对于后续对这个 volatile 变量的读操作可见
2.volatile的原理是在生成的汇编代码中多了一个lock前缀指令，这个前缀指令相当于一个内存屏障，这个内存屏障有3个作用：
  确保指令重排的时候不会把屏障后的指令排在屏障前，确保不会把屏障前的指令排在屏障后。
  修改缓存中的共享变量后立即刷新到主存中。
  当执行写操作时会导致其他CPU中的缓存无效



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

sleep 和 wait 的区别
sleep 方法是 Thread 类中的静态方法，wait 是 Object 类中的方法
sleep 并不会释放同步锁，而 wait 会释放同步锁
sleep 可以在任何地方使用，而 wait 只能在同步方法或者同步代码块中使用
sleep 中必须传入时间，而 wait 可以传，也可以不传，不传时间的话只有 notify 或者 notifyAll 才能唤醒，传时间的话在时间之后会自动唤醒



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
4 AbortPolicy 当提交任务到线程池中被拒绝时，直接抛出RejectedExecutionException异常。

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
线程运行run时会从firstTask或者阻塞队列WorkQueue中取出Runnable执行，没有任务进行阻塞
firstTask执行完后进行阻塞，阻塞队列添加新任务后唤醒，取出任务继续执行
线程回收  空闲线程超时后，将worker从HashSet移除

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




synchronized关键字和Lock的区别你知道吗？为什么Lock的性能好一些？   lock是啥
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




数据竞争，静态条件
分工，同步，互斥
继续总结    管程   读写锁
乐观锁  悲观锁