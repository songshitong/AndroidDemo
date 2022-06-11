jdk 1.8
https://mp.weixin.qq.com/s?__biz=MzkxNDEyOTI0OQ==&mid=2247484437&idx=1&sn=3479c7483c098055b8af3a38235dda4e&chksm=c1726c1bf605e50dd25676423ae283887e05ff12d0540a68770fa89d49ef223014242f2209a8&scene=178&cur_album_id=1538024362992254978#rd
https://tech.meituan.com/2019/02/14/talk-about-java-magic-class-unsafe.html
//todo cleaner相关   https://tech.meituan.com/2019/02/14/talk-about-java-magic-class-unsafe.html

总结
使用Unsafe几乎可以操作一切：

（1）实例化一个类；

（2）修改私有字段的值；

（3）抛出checked异常；

（4）使用堆外内存；

（5）CAS操作；

（6）阻塞/唤醒线程；

Unsafe介绍
Unsafe为我们提供了访问底层的机制，这种机制仅供java核心类库使用，而不应该被普通用户使用
Unsafe是位于sun.misc包下的一个类，主要提供一些用于执行低级别、不安全操作的方法，如直接访问系统内存资源、自主管理内存资源等，
这些方法在提升Java运行效率、增强Java语言底层资源操作能力方面起到了很大的作用。但由于Unsafe类使Java语言拥有了类似C语言指针一样操作内存空间的能力，
这无疑也增加了程序发生相关指针问题的风险。在程序中过度、不正确使用Unsafe类会使得程序出错的概率变大，使得Java这种安全的语言变得不再“安全”，
因此对Unsafe的使用一定要慎重

unsafe的结构
```
public final class Unsafe {
  //构造器私有，外部无法通过new的方式获得
  private Unsafe() {}
  
  //只能是jdk内部使用，否则抛出SecurityException异常unsafe
  public static Unsafe getUnsafe() {
        Class<?> caller = Reflection.getCallerClass();
        if (!VM.isSystemDomainLoader(caller.getClassLoader()))
            throw new SecurityException("Unsafe");
        return theUnsafe;
    }
}
```
异常
```
Exception in thread "main" java.lang.SecurityException: Unsafe
	at sun.misc.Unsafe.getUnsafe(Unsafe.java:90)
	at Test.main(Test.java:12)
```

只能通过反射获得
```
 public static Unsafe getUnsafe() throws NoSuchFieldException, IllegalAccessException {
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        return (Unsafe) f.get(null);
    }
```

unsafe用法
用法1： 实例化一个类
```
 class User{
       public int age ;

        public User() {
            this.age = 10;
        }
    }
    
 User user = (User) unsafe.allocateInstance(User.class);
 System.out.println(user.age); // 打印0
```
Unsafe.allocateInstance()只会给对象分配内存，并不会调用构造方法，所以这里只会返回int类型的默认值0
对象操作其他用法
```
//返回对象成员属性在内存地址相对于此对象的内存地址的偏移量
public native long objectFieldOffset(Field f);
//获得给定对象的指定地址偏移量的值，与此类似操作还有：getInt，getDouble，getLong，getChar等
public native Object getObject(Object o, long offset);
//给定对象的指定地址偏移量设值，与此类似操作还有：putInt，putDouble，putLong，putChar等
public native void putObject(Object o, long offset, Object x);
//从对象的指定偏移量处获取变量的引用，使用volatile的加载语义  禁用编译重排
public native Object getObjectVolatile(Object o, long offset);
//存储变量的引用到对象的指定的偏移量处，使用volatile的存储语义  //禁用编译重排，刷新cpu缓存
public native void putObjectVolatile(Object o, long offset, Object x);
//有序、延迟版本的putObjectVolatile方法，不保证值的改变被其他线程立即看到。只有在field被volatile修饰符修饰时有效
public native void putOrderedObject(Object o, long offset, Object x);
//绕过构造方法、初始化代码来创建对象
public native Object allocateInstance(Class<?> cls) throws InstantiationException;
```
在Gson反序列化时，如果类有默认构造函数，则通过反射调用默认构造函数创建实例，否则通过UnsafeAllocator来实现对象实例的构造，
UnsafeAllocator通过调用Unsafe的allocateInstance实现对象的实例化，保证在目标类无默认构造函数时，反序列化不够影响


用法2：修改私有字段的值
```
class User{
    private int age ;

    public User() {
        this.age = 10;
    }

    public int getAge() {
        return age;
    }
}


User user = new User();
Field age = user.getClass().getDeclaredField("age");
//objectFieldOffset()获取age字段在对象中的偏移地址
unsafe.putInt(user,unsafe.objectFieldOffset(age),20);
System.out.println(user.getAge());
```

用法3： 抛出checked异常
我们知道如果代码抛出了checked异常，要不就使用try...catch捕获它，要不就在方法签名上定义这个异常，
但是，通过Unsafe我们可以抛出一个checked异常，同时却不用捕获或在方法签名上定义它。
```
// 使用正常方式抛出IOException需要定义在方法签名上往外抛
public static void readFile() throws IOException {
throw new IOException ();
}

// 使用Unsafe抛出异常不需要定义在方法签名上往外抛
public static void readFileUnsafe(){
 unsafe.throwException(new IOException());
}
```


用法4：使用堆外内存
使用堆外内存的原因
对垃圾回收停顿的改善。由于堆外内存是直接受操作系统管理而不是JVM，所以当我们使用堆外内存时，即可保持较小的堆内内存规模。
  从而在GC时减少回收停顿对于应用的影响。
提升程序I/O操作的性能。通常在I/O通信过程中，会存在堆内内存到堆外内存的数据拷贝操作，对于需要频繁进行内存间数据拷贝且生命周期较短的暂存数据，
  都建议存储到堆外内存

如果进程在运行过程中JVM上的内存不足了，会导致频繁的进行GC。理想情况下，我们可以考虑使用堆外内存，这是一块不受JVM管理的内存。

使用Unsafe的allocateMemory()我们可以直接在堆外分配内存，这可能非常有用，但我们要记住，这个内存不受JVM管理，
  因此我们要调用freeMemory()方法手动释放它。

假设我们要在堆外创建一个巨大的int数组，我们可以使用allocateMemory()方法来实现：
```
public class OffHeapArray {
    private static final int INT =4;
    private long size;
    private long address;

    private  static Unsafe unsafe;
    static {
        try {
            Field  f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafe= (Unsafe) f.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public OffHeapArray(long size) {
        this.size = size;
        //分配内存
        address = unsafe.allocateMemory(size*INT);
    }

    // 获取指定索引处的元素
    public int get(int i){
        return unsafe.getInt(getAddress( i));
    }

    private long getAddress(int i) {
        return address+ (long) i *INT;
    }
    // 设置指定索引处的元素
    public void set(int i,int value){
        unsafe.putInt(getAddress(i),value);
    }

    // 释放堆外内存
    public void freeMemory(){
        unsafe.freeMemory(address);
    }
}
```
使用
```
OffHeapArray array = new OffHeapArray(4);
array.set(0,0);
array.set(1,1);
array.set(2,2);
array.set(2,20);
//没有给3设置值，默认为0
System.out.println(array.get(3));
int sum=0;
for(int i=0;i<4;i++){
  sum+= array.get(i);
}
System.out.println(sum);
array.freeMemory();
```
典型应用
DirectByteBuffer是Java用于实现堆外内存的一个重要类，通常用在通信过程中做缓冲池，如在Netty、MINA等NIO框架中应用广泛。
DirectByteBuffer对于堆外内存的创建、使用、销毁等逻辑均由Unsafe提供的堆外内存API来实现
创建DirectByteBuffer的时候，通过Unsafe.allocateMemory分配内存、Unsafe.setMemory进行内存初始化，而后构建Cleaner对象用于跟踪DirectByteBuffer对象的垃圾回收
，以实现当DirectByteBuffer被垃圾回收时，分配的堆外内存一起被释放

数组相关操作
```
//返回数组中第一个元素的偏移地址
public native int arrayBaseOffset(Class<?> arrayClass);
//返回数组中一个元素占用的大小
public native int arrayIndexScale(Class<?> arrayClass);
```


用法5：CompareAndSwap操作
JUC下面大量使用了CAS操作，它们的底层是调用的Unsafe的CompareAndSwapXXX()方法。这种方式广泛运用于无锁算法，与java中标准的悲观锁机制相比，
它可以利用CAS处理器指令提供极大的加速。

比如，我们可以基于Unsafe的compareAndSwapInt()方法构建线程安全的计数器
```
public class Counter {
    private volatile int count =0;
    private  static   long countOffset;
    private  static Unsafe unsafe;
    static {
        try {
            Field  f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafe= (Unsafe) f.get(null);
            countOffset = unsafe.objectFieldOffset(Counter.class.getDeclaredField("count"));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
   public void increment(){
        int before = count;
        while (!unsafe.compareAndSwapInt(this,countOffset,before,before+1)){
            before = count;
        }
   }
   public int getCount(){
        return count;
   }
}
```
我们定义了一个volatile的字段count，以便对它的修改所有线程都可见，并在类加载的时候获取count在类中的偏移地址。

在increment()方法中，我们通过调用Unsafe的compareAndSwapInt()方法来尝试更新之前获取到的count的值，如果它没有被其它线程更新过，
  则更新成功，否则不断重试直到成功为止

多线程来测试一下
```
 Counter counter = new Counter();
//100个线程，每个自增10000 
ExecutorService threadPool = Executors.newFixedThreadPool(100);
IntStream.range(0,100).forEach(i->threadPool.submit(()->IntStream.range(0,10000).forEach(j->counter.increment())));
//确保任务提交完成
Thread.sleep(100);
System.out.println(counter.getCount());
threadPool.shutdown();
```
结果打印1000000


用法6：park/unpark
JVM在上下文切换的时候使用了Unsafe中的两个非常牛逼的方法park()和unpark()。

当一个线程正在等待某个操作时，JVM调用Unsafe的park()方法来阻塞此线程。

当阻塞中的线程需要再次运行时，JVM调用Unsafe的unpark()方法来唤醒此线程。

java中的集合时看到了大量的LockSupport.park()/unpark()，它们底层都是调用的Unsafe的这两个方法。


用法7：class相关
此部分主要提供Class和它的静态字段的操作相关方法，包含静态字段内存定位、定义类、定义匿名类、检验&确保初始化等。
```
//获取给定静态字段的内存地址偏移量，这个值对于给定的字段是唯一且固定不变的
public native long staticFieldOffset(Field f);
//获取一个静态类中给定字段的对象指针
public native Object staticFieldBase(Field f);
//判断是否需要初始化一个类，通常在获取一个类的静态属性的时候（因为一个类如果没初始化，它的静态属性也不会初始化）使用。 当且仅当ensureClassInitialized方法不生效时返回false。
public native boolean shouldBeInitialized(Class<?> c);
//检测给定的类是否已经初始化。通常在获取一个类的静态属性的时候（因为一个类如果没初始化，它的静态属性也不会初始化）使用。
public native void ensureClassInitialized(Class<?> c);
//定义一个类，此方法会跳过JVM的所有安全检查，默认情况下，ClassLoader（类加载器）和ProtectionDomain（保护域）实例来源于调用者
public native Class<?> defineClass(String name, byte[] b, int off, int len, ClassLoader loader, ProtectionDomain protectionDomain);
//定义一个匿名类
public native Class<?> defineAnonymousClass(Class<?> hostClass, byte[] data, Object[] cpPatches);
```

用法8： 内存屏障
在Java 8中引入，用于定义内存屏障（也称内存栅栏，内存栅障，屏障指令等，是一类同步屏障指令，是CPU或编译器在对内存随机访问的操作中的一个同步点，
使得此点之前的所有读写操作都执行后才可以开始执行此点之后的操作），避免代码重排序。
```
//内存屏障，禁止load操作重排序。屏障前的load操作不能被重排序到屏障后，屏障后的load操作不能被重排序到屏障前
public native void loadFence();
//内存屏障，禁止store操作重排序。屏障前的store操作不能被重排序到屏障后，屏障后的store操作不能被重排序到屏障前
public native void storeFence();
//内存屏障，禁止load、store操作重排序
public native void fullFence();
```
StampedLock.validate方法的源码实现，通过锁标记与相关常量进行位运算、比较来校验锁状态，在校验逻辑之前，
会通过Unsafe的loadFence方法加入一个load内存屏障，目的是避免copy变量到工作内存和StampedLock.validate中锁状态校验运算发生重排序
 导致锁状态校验不准确的问题
StampedLock.java
```
 public boolean validate(long stamp) {
        U.loadFence();
        return (stamp & SBITS) == (state & SBITS);
    }
```

用法9： 系统相关
这部分包含两个获取系统相关信息的方法。
```
//返回系统指针的大小。返回值为4（32位系统）或 8（64位系统）。
public native int addressSize();  
//内存页的大小，此值为2的幂次方。
public native int pageSize();
```
java.nio下的工具类Bits中计算待申请内存所需内存页数量的静态方法，其依赖于Unsafe中pageSize方法获取系统内存页大小实现后续计算逻辑。

