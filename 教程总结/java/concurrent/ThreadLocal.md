jdk1.8  https://www.cnblogs.com/wupeixuan/p/12638203.html

ThreadLocal 应用场景#
ThreadLocal 的特性也导致了应用场景比较广泛，主要的应用场景如下：
线程间数据隔离，各线程的 ThreadLocal 互不影响  给每个线程分配一个ID
方便同一个线程使用某一对象，避免不必要的参数传递
全链路追踪中的 traceId 或者流程引擎中上下文的传递一般采用 ThreadLocal
Spring 事务管理器采用了 ThreadLocal
Spring MVC 的 RequestContextHolder 的实现使用了 ThreadLocal

java/lang/ThreadLocal.java
```
public class ThreadLocal<T> {
    // 当前 ThreadLocal 的 hashCode，由 nextHashCode() 计算而来，用于计算当前 ThreadLocal 在 ThreadLocalMap 中的索引位置
    private final int threadLocalHashCode = nextHashCode();
    // 哈希魔数，主要与斐波那契散列法以及黄金分割有关
    private static final int HASH_INCREMENT = 0x61c88647;
    // 返回计算出的下一个哈希值，其值为 i * HASH_INCREMENT，其中 i 代表调用次数
    private static int nextHashCode() {
        return nextHashCode.getAndAdd(HASH_INCREMENT);
    }
    // 保证了在一台机器中每个 ThreadLocal 的 threadLocalHashCode 是唯一的
    private static AtomicInteger nextHashCode = new AtomicInteger();
    
    //ThreadLocalMap 是 ThreadLocal 的静态内部类，当一个线程有多个 ThreadLocal 时，需要一个容器来管理多个 ThreadLocal，
    //ThreadLocalMap 的作用就是管理线程中多个 ThreadLocal
    static class ThreadLocalMap {
	/**
	 * 键值对实体的存储结构
	 */
	static class Entry extends WeakReference<ThreadLocal<?>> {
		// 当前线程关联的 value，这个 value 并没有用弱引用追踪
		Object value;

		/**
		 * 构造键值对 k 作 key,作为 key 的 ThreadLocal 会被包装为一个弱引用,v 作 value
		 */
		Entry(ThreadLocal<?> k, Object v) {
			super(k);
			value = v;
		}
	}

	// 初始容量，必须为 2 的幂
	private static final int INITIAL_CAPACITY = 16;

	// 存储 ThreadLocal 的键值对实体数组，长度必须为 2 的幂
	private Entry[] table;

	// ThreadLocalMap 元素数量
	private int size = 0;

	// 扩容的阈值，默认是数组大小的三分之二
	private int threshold;
}
}
```
todo ??这解释
其中的 HASH_INCREMENT 也不是随便取的，它转化为十进制是 1640531527，2654435769 
转换成 int 类型就是 -1640531527，2654435769 等于 (√5-1)/2 乘以 2 的 32 次方。(√5-1)/2 就是黄金分割数，近似为 0.618，
也就是说 0x61c88647 理解为一个黄金分割数乘以 2 的 32 次方，它可以保证 nextHashCode 生成的哈希值，均匀的分布在 2 的幂次方上，
  且小于 2 的 32 次方。

下面是 javaspecialists 中一篇文章对它的介绍：
This number represents the golden ratio (sqrt(5)-1) times two to the power of 31 ((sqrt(5)-1) * (2^31)). 
  The result is then a golden number, either 2654435769 or -1640531527.
下面用例子来证明下：
```
private static final int HASH_INCREMENT = 0x61c88647;

public static void main(String[] args) throws Exception {
    int n = 5;
    int max = 2 << (n - 1);
    for (int i = 0; i < max; i++) {
        System.out.print(i * HASH_INCREMENT & (max - 1));
        System.out.print(" ");

    }
}
```
运行结果为：0 7 14 21 28 3 10 17 24 31 6 13 20 27 2 9 16 23 30 5 12 19 26 1 8 15 22 29 4 11 18 25

可以发现元素索引值完美的散列在数组当中，并没有出现冲突。



ThreadLocal 的set方法
```
    public void set(T value) {
        // 返回当前ThreadLocal所在的线程
        Thread t = Thread.currentThread();
        // 返回当前线程持有的map
        ThreadLocalMap map = getMap(t);
        if (map != null)
            // 如果 ThreadLocalMap 不为空，则直接存储<ThreadLocal, T>键值对
            map.set(this, value);
        else
            // 否则，需要为当前线程初始化 ThreadLocalMap，并存储键值对 <this, firstValue>
            createMap(t, value);
    }
    
  ThreadLocalMap getMap(Thread t) {
        return t.threadLocals;
    } 
  //为当前线程创建ThreadLocalMap
  void createMap(Thread t, T firstValue) {
        t.threadLocals = new ThreadLocalMap(this, firstValue);
    }       
```
对于Thread，仅仅是保存了ThreadLocalMap
   ThreadLocal 的 ThreadLocalMap 是线程的一个属性，所以在多线程环境下 threadLocals 是线程安全的
java/lang/Thread.java
```
class Thread implements Runnable {
  ThreadLocal.ThreadLocalMap threadLocals = null;
  //继承的ThreadLocalMap  从创建线程获取ThreadLocalMap
  ThreadLocal.ThreadLocalMap inheritableThreadLocals = null;
  
   public Thread() {
        init(null, null, "Thread-" + nextThreadNum(), 0);
    }
  
     private void init(ThreadGroup g, Runnable target, String name,
                      long stackSize) {
        init(g, target, name, stackSize, null, true);
    }
    
  private void init(ThreadGroup g, Runnable target, String name,
                      long stackSize, AccessControlContext acc,
                      boolean inheritThreadLocals) {
        ...
        this.name = name;
        Thread parent = currentThread();
        ...
        if (inheritThreadLocals && parent.inheritableThreadLocals != null)
            this.inheritableThreadLocals =
                ThreadLocal.createInheritedMap(parent.inheritableThreadLocals);
        /* Stash the specified stack size in case the VM cares */
        this.stackSize = stackSize;

        /* Set thread ID */
        tid = nextThreadID();
    }
      //系统调用的方法，线程退出前的清理工作    
      private void exit() {
        if (group != null) {
            group.threadTerminated(this);
            group = null;
        }
        /* Aggressively null out all reference fields: see bug 4006245 */
        target = null;
        /* Speed the release of some of these resources */
        threadLocals = null;
        inheritableThreadLocals = null;
        inheritedAccessControlContext = null;
        blocker = null;
        uncaughtExceptionHandler = null;
    }    
}
```
ThreadLocalMap
ThreadLocalMap 其实就是一个简单的 Map 结构，底层是数组，有初始化大小，也有扩容阈值大小，数组的元素是 Entry，
Entry 的 key 就是 ThreadLocal 的引用，value 是 ThreadLocal 的值。ThreadLocalMap 解决 hash 冲突的方式采用的是线性探测法，
如果发生冲突会继续寻找下一个空的位置。   todo 线性探测法
ThreadLocalMap.set
```
private void set(ThreadLocal<?> key, Object value) {

            // We don't use a fast path as with get() because it is at
            // least as common to use set() to create new entries as
            // it is to replace existing ones, in which case, a fast
            // path would fail more often than not.

            Entry[] tab = table;
            int len = tab.length;
            // 计算 key 在数组中的下标
            int i = key.threadLocalHashCode & (len-1);
            // 遍历一段连续的元素，以查找匹配的 ThreadLocal 对象
            for (Entry e = tab[i];
                 e != null;
                 e = tab[i = nextIndex(i, len)]) {
                 // 获取该哈希值处的ThreadLocal对象
                ThreadLocal<?> k = e.get();
                // 键值ThreadLocal匹配，直接更改map中的value
                if (k == key) {
                    e.value = value;
                    return;
                }
                // 若 key 是 null，说明 ThreadLocal 被清理了，直接替换掉
                if (k == null) {
                    replaceStaleEntry(key, value, i);
                    return;
                }
            }
            //todo replaceStaleEntry,cleanSomeSlots,rehash
            // 直到遇见了空槽也没找到匹配的ThreadLocal对象，那么在此空槽处安排ThreadLocal对象和缓存的value
            tab[i] = new Entry(key, value);
            int sz = ++size;
            // 如果没有元素被清理，那么就要检查当前元素数量是否超过了容量阙值(数组大小的三分之二)，以便决定是否扩容
            if (!cleanSomeSlots(i, sz) && sz >= threshold)
                rehash();
        }
```

ThreadLocal 的 get 方法
```
    public T get() {
        // 返回当前 ThreadLocal 所在的线程
        Thread t = Thread.currentThread();
        // 从线程中拿到 ThreadLocalMap
        ThreadLocalMap map = getMap(t);
        if (map != null) {
            // 从 map 中拿到 entry 
            ThreadLocalMap.Entry e = map.getEntry(this);
            // 如果不为空，读取当前 ThreadLocal 中保存的值
            if (e != null) {
                @SuppressWarnings("unchecked")
                T result = (T)e.value;
                return result;
            }
        }
        // 若 map 为空，则对当前线程的 ThreadLocal 进行初始化，最后返回当前的 ThreadLocal 对象关联的初值，即 value
        return setInitialValue();
    }
    
    private T setInitialValue() {
        T value = initialValue();
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null)
            map.set(this, value);
        else
            createMap(t, value);
        return value;
    }
```
ThreadLocalMap.getEntry
```
      private Entry getEntry(ThreadLocal<?> key) {
            int i = key.threadLocalHashCode & (table.length - 1);
            Entry e = table[i];
            // 若 e 不为空，并且 e 的 ThreadLocal 的内存地址和 key 相同，直接返回
            if (e != null && e.get() == key)
                return e;
            else
               // 从 i 开始向后遍历找到键值对实体   todo
                return getEntryAfterMiss(key, i, e);
        }
```


remove方法
在使用完 ThreadLocal 变量后，需要我们手动 remove 掉，防止 ThreadLocalMap 中 Entry 一直保持对 value 的强引用，导致 value 不能被回收，
  防止内存泄露
```
  public void remove() {
         // 返回当前线程持有的 map
         ThreadLocalMap m = getMap(Thread.currentThread());
         if (m != null)
           // 从 map 中清理当前 ThreadLocal 对象关联的键值对
             m.remove(this);
     }   
```