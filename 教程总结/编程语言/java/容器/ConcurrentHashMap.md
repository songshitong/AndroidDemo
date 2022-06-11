https://mp.weixin.qq.com/s/UXV34hYMHwsFBe9AQCZvLg
https://www.jianshu.com/p/c0642afe03e0
https://www.cnblogs.com/tong-yuan/p/ConcurrentHashMap-resize.html
jdk 1.8

总结
ConcurrentHashMap的数据结构是volatile Node<K,V>[] table + 链表或红黑树
ConcurrentHashMap只有对table修改时才会加锁，volatile保证修改后，读取数据是尽可能新的
扩容的处理 如果存在多个线程，每个负责一段进行扩容，扩容完成才进行其他操作   充分利用多核CPU的性能
CAS+synchronized的使用
通过cas控制sizeCtl，确定哪个线程有对table的初始化权   初始化的并操作通过临界条件sizeCtl控制而不是对整个方法上锁
通过cas控制sizeCtl和transferIndex，确定哪个线程对哪个段有扩容权限，获得权限后进行加锁，防止其他线程在扩容期间进行操作
  默认transferIndex为n,每有一个线程减stride作为自己的负责区间，直到transferIndex<=0分配完成
对于线程的控制
第一个线程进入时，sizeCtl是+2，后续线程进入时+1，退出时-1，然后检查-2后是否符合resizeStamp()移位后获得的值以判断扩容是否结束

ConcurrentHashMap加锁只对要操作的节点node加锁，锁的粒度足够细
分段锁的意义在于：各个区域拥有自己的锁，以使得线程去往不同的区域竞争条件，以此减少线程间的相互影响。
ConcurrentHashMap在table[i]上，存储了以Node为单位的不同数据集合，每个table[i]，就是一个锁，由此分散了竞争。
   transfer()方法，也是以table[i]的位置的Node为锁，再进行扩容操作。


todo 1.7的实现   unsafe   1.7 ReentrantLock变为JDK8后的Synchronized

ConcurrentHashMap的出现，是因为HashMap不是线程安全，HashTable线程安全却并发效率过低。ConcurrentHashMap仔细斟酌了两者的难处，
  在快速存储与高效并发间找到了微妙的平衡点。

//todo 记录到并发区域里面
回到并发编程来看，提高并发效率可从几方面入手：
减少临界区区域：这是容易理解的，在并发中，临界区是依次通过的，即临界区是串行的。
  //比如给方法A，B都上锁，a和b是依次执行的，假如所有方法都上锁就是单线程了，而且效率可能更低
加速通过临界区：使临界区内的逻辑代码执行得尽可能地快，这样，线程在临界区内停留的时间更短。
  //提高临界区域执行效率，比如以前是给方法A上锁，修改为只给A的一部分上锁，这样只有一小部分是同步代码，效率就提升了
减少竞争条件：通过去除不必要的竞争条件，或竞争不同的临界区，来减少线程间的相互影响。
 //假设1,2,3,4,5都在竞争条件A，梳理后发现5不需要(去除不必要的竞争)，A条件可以拆分为B和C，1,2需要竞争B，3，4需要竞争C(分段竞争)


ConcurrentHashMap维持了HashMap的工作特性，并在并发环境下也能如期进行。以HashTable来看，之所以不能胜任高并发环境下的工作，
  因其对所有方法上锁，导致临界区覆盖过大，竞争条件激烈，那么在并发量大时，每个线程受限于其他线程，与串行无异，违背了并发的初衷。


对于理解ConcurrentHashMap来说，一些属性是关键的：
```
// 与介绍HashMap相同，每个存储的Node该索引到的位置
transient volatile Node<K,V>[] table;
// 当容量不足时，创建容量更大的nextTable，并将table上所有的Node重新索引到nextTable上，再令table=nextTable
private transient volatile Node<K,V>[] nextTable;
// 加载因子，决定了table应该多满，越大则越容易发生碰撞，越小则越浪费空间，此消彼长。
// 与HashMap不同的是，恒为0.75，此值与ConcurrentHashMap的优化有关
private static final float LOAD_FACTOR = 0.75f;
//最大容量
private static final int MAXIMUM_CAPACITY = 1 << 30;
//默认大小
private static final int DEFAULT_CAPACITY = 16;

/**
* 是ConcurrentHashMap非常重要，并难以理解，sizeCtl用于并发控制与状态标记:
* =0时，ConcurrentHashMap为初始状态
* >0，表示当前的最大容量，构造器初始化会确定sizeCtl大小
* =-1，说明ConcurrentHashMap正在初始化  肯定只有一个线程在初始化，多个线程就有冲突了
* <-1，说明ConcurrentHashMap正在进行扩容，此状态下 sizeCtl = -(1 + 扩容线程数)  -1被初始化使用了，表示线程数就+1了
*/
private transient volatile int sizeCtl;

/**
* 因为sizeCtl处理了多种状态，需要其他的属性协助参与
*/
// 扩容时用到，通过sizeCtl生成标记，回归标记状态，说明扩容完毕，状态有效位为16位
private static int RESIZE_STAMP_BITS = 16;
// 扩容线程最大数量，为 2^16，
private static final int MAX_RESIZERS = (1 << (32 - RESIZE_STAMP_BITS)) - 1;
// 扩容时用到，还原sizeCtl记录的扩容标记
private static final int RESIZE_STAMP_SHIFT = 32 - RESIZE_STAMP_BITS;

//分段扩容的幅度
private static final int MIN_TRANSFER_STRIDE = 16

//transferIndex是扩容时，旧table还未处理的索引位置
private transient volatile int transferIndex;

//哈希相关  如果hash>0
//在ForwardingNode中使用
static final int MOVED     = -1; // hash for forwarding nodes
//在红黑树节点TreeBin中使用
static final int TREEBIN   = -2; // hash for roots of trees
//在ReservationNode中使用
static final int RESERVED  = -3; // hash for transient reservations
//保证hash值不超过Integer.MAX_VALUE
static final int HASH_BITS = 0x7fffffff; // usable bits of normal node hash

//链表与红黑树转换的阈值
static final int TREEIFY_THRESHOLD = 8;
static final int UNTREEIFY_THRESHOLD = 6;
```
stride [straɪd]  大步;一步(的距离);步态;步伐;进展;进步;发展;裤子

Node结构
```
class Node<K,V> implements Map.Entry<K,V> {
    final int hash;
    final K key;
    volatile V val;
    volatile Node<K,V> next;
    ... 省略部分代码
}
```
其中value和next都用volatile修饰，保证并发的可见性，新增节点或者改变val其他线程可以快速可见
ForwardingNode：一个特殊的Node节点，hash值为-1，其中存储nextTable的引用
```
final class ForwardingNode<K,V> extends Node<K,V> {
    //扩容时新的table
    final Node<K,V>[] nextTable;
    ForwardingNode(Node<K,V>[] tab) {
        super(MOVED, null, null, null);
        this.nextTable = tab;
    }
}
```
只有table发生扩容的时候，ForwardingNode才会发挥作用，作为一个占位符放在table中表示当前节点为null或则已经被移动


构造器
```
 //空的map
 public ConcurrentHashMap() {
    }
    
    public ConcurrentHashMap(int initialCapacity) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException();
        int cap = ((initialCapacity >= (MAXIMUM_CAPACITY >>> 1)) ?
                   MAXIMUM_CAPACITY :
                   tableSizeFor(initialCapacity + (initialCapacity >>> 1) + 1));
        this.sizeCtl = cap;
    }  
    
   //todo 位运算  会根据参数调整table的大小，假设参数为100，最终会调整成256，确保table的大小总是2的幂次方
   private static final int tableSizeFor(int c) {
        int n = c - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }    
    
  public ConcurrentHashMap(Map<? extends K, ? extends V> m) {
        this.sizeCtl = DEFAULT_CAPACITY;
        putAll(m);
    }    
    
   public void putAll(Map<? extends K, ? extends V> m) {
        //尝试扩容
        tryPresize(m.size());
        //遍历，然后把值放到到table
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet())
            putVal(e.getKey(), e.getValue(), false);
    } 
    
    public ConcurrentHashMap(int initialCapacity, float loadFactor) {
        this(initialCapacity, loadFactor, 1);
    }
    
  public ConcurrentHashMap(int initialCapacity,
                             float loadFactor, int concurrencyLevel) {
        if (!(loadFactor > 0.0f) || initialCapacity < 0 || concurrencyLevel <= 0)
            throw new IllegalArgumentException();
        if (initialCapacity < concurrencyLevel)   // Use at least as many bins
            initialCapacity = concurrencyLevel;   // as estimated threads
        long size = (long)(1.0 + (long)initialCapacity / loadFactor);
        int cap = (size >= (long)MAXIMUM_CAPACITY) ?
            MAXIMUM_CAPACITY : tableSizeFor((int)size);
        this.sizeCtl = cap;
    }    
  
private final void tryPresize(int size) {
        //计算size 
        int c = (size >= (MAXIMUM_CAPACITY >>> 1)) ? MAXIMUM_CAPACITY :
            tableSizeFor(size + (size >>> 1) + 1);
        int sc;
        //初始化被其他线程竞争到了，不进入while
        while ((sc = sizeCtl) >= 0) {
            //不存在扩容的情况   记录table，此时tab为null或空的
            Node<K,V>[] tab = table; int n;
            if (tab == null || (n = tab.length) == 0) {
                //table为空
                //n是待初始化的容量
                n = (sc > c) ? sc : c;
                //cas更新sizectl为-1
                if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
                    //cas竞争成功，只有一个线程处理初始化
                    try {
                        //重新校验，table没有改变，仍然为空  table初始化或扩容后地址发生改变
                        if (table == tab) {
                            @SuppressWarnings("unchecked")
                            Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n];
                            //初始化table
                            table = nt;
                            //更新sizeCtl为0.75*n
                            sc = n - (n >>> 2);
                        }
                    } finally {
                        sizeCtl = sc;
                    }
                }
            }
            else if (c <= sc || n >= MAXIMUM_CAPACITY)
                //未达到扩容阈值，或者超过允许的最大容量
                break;
            else if (tab == table) {
                //table没有发生变化
                int rs = resizeStamp(n);
                if (sc < 0) {
                    //发现存在扩容
                    Node<K,V>[] nt;
                    if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 ||
                        sc == rs + MAX_RESIZERS || (nt = nextTable) == null ||
                        transferIndex <= 0)
                        break;
                    //开始扩容    
                    if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1))
                        transfer(tab, nt);
                }
                //sc>0 第一次触发扩容
                else if (U.compareAndSwapInt(this, SIZECTL, sc,
                                             (rs << RESIZE_STAMP_SHIFT) + 2))
                    transfer(tab, null);
            }
        }
    }
    
```
ConcurrentHashMap在构造函数中只会初始化sizeCtl值，并不会直接初始化table，而是延缓到第一次put操作。


table初始化
前面已经提到过，table初始化操作会延缓到第一次put行为。但是put是可以并发执行的，Doug Lea是如何实现table只初始化一次的？
让我们来看看源码的实现。
```
private final Node<K,V>[] initTable() {
        Node<K,V>[] tab; int sc;
        while ((tab = table) == null || tab.length == 0) {
            //如果一个线程发现sizeCtl<0，意味着另外的线程执行CAS操作成功，当前线程只需要让出cpu时间片  获得时间片后重新进入while判断
            if ((sc = sizeCtl) < 0)
                Thread.yield(); // lost initialization race; just spin
            else if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
                //SIZECTL 为当前对象sizeCtl的地址，通过自旋+cas确保sizeCtl为-1，代表正在初始化
                try {
                    if ((tab = table) == null || tab.length == 0) {
                        int n = (sc > 0) ? sc : DEFAULT_CAPACITY;
                        @SuppressWarnings("unchecked")
                        Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n];
                        table = tab = nt;
                        //默认是table大小的0.75倍， 1-1/4=0.75
                        sc = n - (n >>> 2);
                    }
                } finally {
                    //初始化成功，更新sizeCtl 
                    sizeCtl = sc;
                }
                break;
            }
        }
        return tab;
    }
```
sizeCtl默认为0，如果ConcurrentHashMap实例化时有传参数，sizeCtl会是一个2的幂次方的值。所以执行第一次put操作的线程会执行
 Unsafe.compareAndSwapInt方法修改sizeCtl为-1，有且只有一个线程能够修改成功，其它线程通过Thread.yield()让出CPU时间片等待table初始化完成。

put操作
假设table已经初始化完成，put操作采用CAS+synchronized实现并发插入或更新操作，具体实现如下
ConcurrentHashMap的key和value不能是null，抛出异常
1 如果桶数组未初始化，则初始化；  https://zhuanlan.zhihu.com/p/429503859
2 如果待插入的元素所在的桶为空，则尝试把此元素直接插入到桶的第一个位置(CAS插入)；
3 如果正在扩容，则当前线程一起加入到扩容的过程中；  //扩容的table[i]的hash为moved
4 如果待插入的元素所在的桶不为空且不在迁移元素，则锁住这个桶（分段锁）； //防止其他线程操作
5 如果当前桶中元素以链表方式存储，则在链表中寻找该元素或者插入元素；
6 如果当前桶中元素以红黑树方式存储，则在红黑树中寻找该元素或者插入元素；
7 如果元素存在，则返回旧值；
8 如果元素不存在，整个Map的元素个数加1，并检查是否需要扩容；
```
final V putVal(K key, V value, boolean onlyIfAbsent) {
        if (key == null || value == null) throw new NullPointerException();
        int hash = spread(key.hashCode());
        int binCount = 0;
        //不断循环处理下面的if情况
        for (Node<K,V>[] tab = table;;) {
            Node<K,V> f; int n, i, fh;
            if (tab == null || (n = tab.length) == 0)
               //初始化table然后重试
                tab = initTable();
                
            else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
                //table[i]为null  使用cas更新为新的node
                if (casTabAt(tab, i, null,
                             new Node<K,V>(hash, key, value, null)))
                    //使用cas更新成功，跳出循环  table[i]为null时不用加锁，使用cas更新的         
                    break;                   // no lock when adding to empty bin
                //cas更新失败，说明存在多线程的hash冲突，当前位置node已经不为null，竞争失败，进入for重试    
            }
            else if ((fh = f.hash) == MOVED)
                //当前node为FowardingNode   正在扩容
                //扩容完成再次进入for尝试
                tab = helpTransfer(tab, f);
            else {
             。。。。
        }
        //addCount的1L，代表记录map的大小+1  删除一个时传入-1
        addCount(1L, binCount);
        return null;
    }
 
 //通过unsafe获取table[]中的第i个   
 static final <K,V> Node<K,V> tabAt(Node<K,V>[] tab, int i) {
        return (Node<K,V>)U.getObjectVolatile(tab, ((long)i << ASHIFT) + ABASE);
    } 
    
 static final <K,V> boolean casTabAt(Node<K,V>[] tab, int i,
                                        Node<K,V> c, Node<K,V> v) {
        return U.compareAndSwapObject(tab, ((long)i << ASHIFT) + ABASE, c, v);
    }       
```
1.hash算法    异或本身和它的高16位    HASH_BITS为0x7fffffff，确保位运算不超过int最大值，Integer.MAX_VALUE也是0x7fffffff
```
 static final int spread(int h) {
        return (h ^ (h >>> 16)) & HASH_BITS;
    }
```
2.table中定位索引位置，n是table的大小  
i = (n - 1) & hash)

3.tabAt()      //todo 记录到volatile
获取table中对应索引的元素f。
Doug Lea采用Unsafe.getObjectVolatile来获取，也许有人质疑，直接table[index]不可以么，为什么要这么复杂？
在java内存模型中，我们已经知道每个线程都有一个工作内存，里面存储着table的副本，虽然table是volatile修饰的，
  但不能保证线程每次都拿到table中的最新元素，Unsafe.getObjectVolatile可以直接获取指定内存的数据，保证了每次拿到数据都是最新的

4.如果f为null，说明table中这个位置第一次插入元素，利用Unsafe.compareAndSwapObject方法插入Node节点。
 如果CAS成功，说明Node节点已经插入，随后addCount(1L, binCount)方法会检查当前容量是否需要进行扩容。
 如果CAS失败，说明有其它线程提前插入了节点，自旋重新尝试在这个位置插入节点   可能是红黑树或链表

5.如果f的hash值为-1，说明当前f是ForwardingNode节点，意味有其它线程正在扩容，则一起进行扩容操作
```
final Node<K,V>[] helpTransfer(Node<K,V>[] tab, Node<K,V> f) {
    Node<K,V>[] nextTab; int sc;
    // 如果桶数组不为空，并且当前桶第一个元素为ForwardingNode类型，并且nextTab不为空
    // 扩容时会把旧桶的第一个元素置为ForwardingNode，并让其nextTab指向新桶数组
    if (tab != null && (f instanceof ForwardingNode) &&
            (nextTab = ((ForwardingNode<K,V>)f).nextTable) != null) {
        int rs = resizeStamp(tab.length);
        // sizeCtl<0，说明正在扩容
        while (nextTab == nextTable && table == tab &&
                (sc = sizeCtl) < 0) {
            if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 ||
                    sc == rs + MAX_RESIZERS || transferIndex <= 0)
                  /**
                * 1.sc >>> RESIZE_STAMP_SHIFT) != rs 确定扩容标记是没有变化  如果发生变化，重新进入while判断情况
                * 2.sc == rs + 1 说明所有线程扩容完毕，不用去帮忙了  每有一个线程+1,第一个线程(rs << RESIZE_STAMP_SHIFT) + 2，第一个完成后(rs << RESIZE_STAMP_SHIFT) + 1
                * 3.sc == rs + MAX_RESIZERS 说明扩容线程达到了上限
                * 4.transferIndex <= 0 说明table所有区域分配完了，不需要帮忙了
                */
                break;
            // 扩容线程数加1
            if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1)) {
                // 当前线程帮忙迁移元素
                transfer(tab, nextTab);
                break;
            }
        }
        return nextTab;
    }
    return table;
}
```
int rs = resizeStamp(tab.length);     sc == rs + 1 || sc == rs + MAX_RESIZERS 在这是个bug
在jdk 12中 int rs = resizeStamp(tab.length) << RESIZE_STAMP_SHIFT;   rs先进行了移位，后续判等
http://hg.openjdk.java.net/jdk/jdk/file/9af672cab7cb/src/java.base/share/classes/java/util/concurrent/ConcurrentHashMap.java
https://stackoverflow.com/questions/53493706/how-the-conditions-sc-rs-1-sc-rs-max-resizers-can-be-achieved-in
截止Android11，仍然存在这个问题

6.其余情况把新的Node节点按链表或红黑树的方式插入到合适的位置，这个过程采用同步内置锁实现并发，代码如下: 
```
  //记录旧的值
  V oldVal = null;
    //使用synchronized确保 hash冲突时的插入
    //f是table[i] 所以锁住的是table[i]
    synchronized (f) {
        if (tabAt(tab, i) == f) {
            //fh为node的hash
            if (fh >= 0) {
                //记录binCount 
                binCount = 1;
                for (Node<K,V> e = f;; ++binCount) {
                    K ek;
                    //如果当前节点 hash相同，key地址相同或key相等
                    if (e.hash == hash &&
                        ((ek = e.key) == key ||
                         (ek != null && key.equals(ek)))) {
                        oldVal = e.val;
                        if (!onlyIfAbsent)
                            //更新node的值
                            e.val = value;
                        break;
                    }
                    Node<K,V> pred = e;
                    //更新e为下一个节点，如果e为null时,pred为最后一个  此时根据key,value新建节点并将放入链表的最后
                    if ((e = e.next) == null) {
                        pred.next = new Node<K,V>(hash, key,
                                                  value, null);
                        break;
                    }
                }
            }
            //红黑树的处理 
            else if (f instanceof TreeBin) {
                Node<K,V> p;
                //更新binCount为2
                binCount = 2;
                if ((p = ((TreeBin<K,V>)f).putTreeVal(hash, key,
                                               value)) != null) {
                    oldVal = p.val;
                    if (!onlyIfAbsent)
                        p.val = value;
                }
            }
        }
    }
    if (binCount != 0) {
        //链表 阈值为8 
        if (binCount >= TREEIFY_THRESHOLD)
            //转为红黑树后，重试
            treeifyBin(tab, i);
        if (oldVal != null)
            //插入链表或红黑树 成功返回旧的值
            return oldVal;
        break;
    }
```
absent [ˈæbsənt , æbˈsent]  缺席;不参加;不在
在节点f上进行同步，节点插入之前，再次利用tabAt(tab, i) == f判断，防止被其它线程修改。
1 如果f.hash >= 0，说明f是链表结构的头结点，遍历链表，如果找到对应的node节点，则修改value，否则在链表尾部加入节点。
2 如果f是TreeBin类型节点，说明f是红黑树根节点，则在树结构上遍历元素，更新或增加节点。
3 如果链表中节点数binCount >= TREEIFY_THRESHOLD(默认是8)，则把链表转化为红黑树结构。
根据注释文档，到达TREEIFY_THRESHOLD的概率为约为 0.00000006

table扩容  addCount
当table容量不足的时候，即table的元素数量达到容量阈值sizeCtl，需要对table进行扩容。
整个扩容分为两部分：
1 构建一个nextTable，大小为table的两倍。
2 把table的数据复制到nextTable中。
这两个过程在单线程下实现很简单，但是ConcurrentHashMap是支持并发插入的，扩容操作自然也会有并发的出现，这种情况下，
  第二步可以支持节点的并发复制，这样性能自然提升不少，但实现的复杂度也上升了一个台阶。

先看第一步，构建nextTable，毫无疑问，这个过程只能只有单个线程进行nextTable的初始化，具体实现如下：
```
private final void addCount(long x, int check) {
    //s为sum是使用的容量
    CounterCell[] as; long b, s;
    ... 省略部分代码
    //node插入后,check>0
    if (check >= 0) {
        Node<K,V>[] tab, nt; int n, sc;
        // 如果元素个数达到了扩容门槛，则进行扩容
        // 注意，正常情况下sizeCtl存储的是扩容门槛，即容量的0.75倍
        // table不为空已经初始化了并且table长度没有达到最大
        while (s >= (long)(sc = sizeCtl) && (tab = table) != null &&
               (n = tab.length) < MAXIMUM_CAPACITY) {
            //rs为0000 0000 0000 0000 1xxx xxx xxxx xxxx  
            int rs = resizeStamp(n);           
            if (sc < 0) {
              //有可能正在扩容
                // sizeCtl右移16位不等于rs
                // MAX_RESIZERS为(1<<16)-1
                if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 ||
                    sc == rs + MAX_RESIZERS || (nt = nextTable) == null ||
                    transferIndex <= 0)
                     /**
                    * 1.sc >>> RESIZE_STAMP_SHIFT) != rs 确定扩容标记是没有变化
                    * 2.sc == rs + 1 说明所有线程扩容完毕，不用去帮忙了
                    * 3.sc == rs + MAX_RESIZERS 说明扩容线程达到了上限
                    * 4.(nt = nextTable) == null  扩容完成会将nextTable置为null
                    * 5.transferIndex <= 0 说明table所有区域分配完了，不需要帮忙了                   
                    */
                    // 扩容已经完成了，退出循环
                    // 正常应该只会触发nextTable==null这个条件，其它条件没看出来何时触发
                    break;
                if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1))
                   //nt=nextTable 不为null  扩容未完成，则当前线程加入迁移元素中
                   // 并把扩容线程数加1
                    transfer(tab, nt);
            }
            //sc>0 第一次触发扩容
            else if (U.compareAndSwapInt(this, SIZECTL, sc,
                                         (rs << RESIZE_STAMP_SHIFT) + 2))
                // 这里是触发扩容的那个线程进入的地方
                // sizeCtl的高16位存储着rs这个扩容邮戳  最高位是1,代表是负的
                // sizeCtl的低16位存储着扩容线程数加1，即(1+nThreads)   第一次进入+2,本身一个，额外+1
                // 所以官方说的扩容时sizeCtl的值为 -(1+nThreads)是错误的                     
                //迁移元素                         
                transfer(tab, null);
            s = sumCount();
        }
    }
}

 //Must be negative when shifted left by RESIZE_STAMP_SHIFT.
 static final int resizeStamp(int n) {
        //numberOfLeadingZeros 返回一个数二进制从左数有多少个0
        //1 << (RESIZE_STAMP_BITS - 1)  RESIZE_STAMP_BITS是16   结果是1000 0000 0000 0000
        //所以resizeStamp的结果是 0000 0000 0000 0000 1xxx xxx xxxx xxxx
        // 根据注释介绍，当左移RESIZE_STAMP_SHIFT是必须是负的，左移16位后最高位为1
        return Integer.numberOfLeadingZeros(n) | (1 << (RESIZE_STAMP_BITS - 1));
    }
```
扩容时sizeCtl高位存储扩容邮戳(resizeStamp)，低位存储扩容线程数加1（1+nThreads）   sizeCtl<0
resizeStamp就是第一次进入扩容，将sizeCtl由正的变为负的， sizeCtl=(rs << RESIZE_STAMP_SHIFT) + 2
另外当sizeCtl再次回归到这个标记时，代表扩容结束

例子：map中存入12个，达到扩容阈值
rs = resizeStamp    // 32795     1000 0000 0001 1011  
sc = 12
(rs << RESIZE_STAMP_SHIFT) + 2   1000 0000 0001 1011 0000 0000 0000 0010    －2,145,714,174
进入逻辑transfer(tab, null)

为什么使用resizeStamp()进行处理得到标记的数字，而不是以某个特定的数值作为标记呢？

一方面是因为，如果以特定的数值，并发状态下table可能继续被扩容。假设以一个特定的值Z代表继续扩容，在T0时刻，参与扩容的的线程都记录了一个值Z，
长度从N扩容完成到了2N。在T1时刻，有一个参与扩容的线程醒来，发现值为Z，认为需要扩容，将长度从2N扩容到了4N。当然，可以通过加锁来保证一致性，
但将降低并发效率。因此，通过resizeStamp()获取到一个值X，当table长度改变时，通过resizeStamp()获取到一个值将得到X1，X与X1不同，
也就避免了这个问题。仔细一看，像不像CAS的ABA问题？
 //使用某个特定的值有可能发生ABA问题，更新为A，又更新为B，然后为A；对于第一次来说A没改变  如果没加锁可能多次扩容，如果加锁降低效率

另一方是因为，sizeCtl还将记录正在扩容的线程数，从ConcurrentHashMap的属性MAX_RESIZERS看，最多支持2^16 个线程协助扩容。
  使用 sizeCtl 的低16位足以表示


扩容逻辑
通过Unsafe.compareAndSwapInt修改sizeCtl值，保证只有一个线程能够初始化nextTable，扩容后的数组长度为原来的两倍，但是容量是原来的1.5。

节点从table移动到nextTable，大体思想是遍历、复制的过程。
1 首先根据运算得到需要遍历的次数i，然后利用tabAt方法获得i位置的元素f，初始化一个forwardNode实例fwd。
2 如果f == null，则在table中的i位置放入fwd，这个过程是采用Unsafe.compareAndSwapObjectf方法实现的，很巧妙的实现了节点的并发移动。
3 如果f是链表的头节点，就构造一个反序链表，把他们分别放在nextTable的i和i+n的位置上，移动完成，采用Unsafe.putObjectVolatile方法给table原位置赋值fwd。
4 如果f是TreeBin节点，也做一个反序处理，并判断是否需要untreeify，把处理的结果分别放在nextTable的i和i+n的位置上，移动完成，同样采用Unsafe.putObjectVolatile方法给table原位置赋值fwd。

遍历过所有的节点以后就完成了复制工作，把table指向nextTable，并更新sizeCtl为新数组大小的0.75倍 ，扩容完成。
  扩容完成原来的table会被废弃，此时的原table的fwd也就没用了


ConcurrentHashMap对于并发效率的优化
分段扩容

与其他文章不同的是，我想从ConcurrentHashMap的扩容方法，即从transfer()说起。因为transfer()的处理方式，包含了ConcurrentHashMap重要的
  核心的内容，值得细说，并在理解之后，观看其他方法语义实现，将简单地多。

与HashMap一样，当数据容量超过加载因子，如果不进行扩容，继续新的存储，那么，碰撞问题将加剧，碰撞问题严重的区域效率变低。
  此时，就需要扩容，以维持HashMap保持的效率平衡。

ConcurrentHashMap.transfer()内容很长，将分段进行讲解

确定分段长度
```
 private final void transfer(Node<K,V>[] tab, Node<K,V>[] nextTab) {
        int n = tab.length, stride;
        // 确定每一段的长度，NCPU代表CPU核数
        // 多核 stride=(length/8)/cpu核数   单核stride=length  stride最小为16
        if ((stride = (NCPU > 1) ? (n >>> 3) / NCPU : n) < MIN_TRANSFER_STRIDE)
            stride = MIN_TRANSFER_STRIDE; // subdivide range
        if (nextTab == null) {            // initiating
            // ①说明还没有创建新的table，创建新的
            try {
                //创建新的node数组，长度为length*2
                Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n << 1];
                nextTab = nt;
            } catch (Throwable ex) {      // try to cope with OOME
                sizeCtl = Integer.MAX_VALUE;
                return;
            }
            nextTable = nextTab;
            // transferIndex是扩容时，旧table还未处理的索引位置
            transferIndex = n;
        }
        int nextn = nextTab.length;
         // 创建一个特殊的节点，为Node子类的ForwardingNode   Node类持有nextTable
         //迁移完成的桶在里面放置一ForwardingNode类型的元素，标记该桶迁移完成
        ForwardingNode<K,V> fwd = new ForwardingNode<K,V>(nextTab);
        // 表示是否要向下一个区域进行扩容
        boolean advance = true;
         // 表示扩容是否结束了
        boolean finishing = false; // to ensure sweep before committing nextTab
       。。。 
```
比较有意思的变量是 stride，中文解释为“一步的距离”，应对于扩容场景，意思为“每个区域的大小”。从stride的获取方式，考虑了CPU核数，
直接说明了是支持并发扩容的。那么，每一个进行并发的参与者，一次负责一个stride长度的区域，stride最低为16。之后将马上见到这部分内容。

在①处，将有个疑问：此处并没上锁，那么怎么保证接下来的操作，使用的nextTab都是同一个？此处实际上是更上层的调用来进行保证的，
如果与transfer()做了约定，传入null时，将只有一个线程通过①处。
留下第一个问题需要回答：扩容时，如何保证nextTab是同一个?
putVal在成功加入新的数据后，再通过addCount()更新当前数据容量
在插入数据后，重新计算当前容量，如果大于sizeCtl，则进行扩容。
首次进入transfer()，将对 sizeCtl = sizeCtl + 2 进行更新，并传入null，
后续再进入transfer()，则将 sizeCtl = sizeCtl + 1 进行更新，并传入代表nextTable的nt，在退出时 将 sizeCtl = sizeCtl - 1 进行更新
当检查到 sizeCtl - 2 = resizeStamp() 时，说明回归到了扩容标记值，扩容结束
因此，只有首次进入transfer()的线程，才会创建nextTable，从而保证其他线程扩容时访问的是同一个。


确定每个线程负责的扩容区域
```
 private final void transfer(Node<K,V>[] tab, Node<K,V>[] nextTab) {
...
 //整个while循环就是在算i的值 i的值会从n-1依次递减 其中n是旧桶数组的大小，也就是说i从15开始一直减到1这样去迁移元素
 // i的赋值在for里面
 for (int i = 0, bound = 0;;) {
    Node<K,V> f; int fh;
    //advance初始为true，迁移完table[i]的node或链表或红黑树后，advance置为true 再次进入while
    // advance为true，开始寻找下一个要处理的
    while (advance) {
        // nextIndex：用于记录当前的transferIndex值
        // nextBound：当前正在处理的区域的边界
        int nextIndex, nextBound;
        if (--i >= bound || finishing)
            // 更新索引位置  bound<=i 正在处理分段区间的一个元素   --i<bound,finishing为true，扩容完成了，advance置为false
            // ①
            advance = false;
        else if ((nextIndex = transferIndex) <= 0) {
             // ②  待处理的index<=0，就是分段的区域分配完了   此时i在第一个元素完成了，由于是倒着遍历，不再更新索引，后面检查是否扩容完成
             i = -1;
            i = -1;
            advance = false;
        }
        else if (U.compareAndSwapInt
                 (this, TRANSFERINDEX, nextIndex,
                  nextBound = (nextIndex > stride ?
                               nextIndex - stride : 0))) {
            //i<bound，还没扩容完成进入这个if 请求下一个分段                   
            // 通过CAS竞争扩容区域的负责权  上面都没走，代表当前线程开始请求扩容一段
            //  nextBound = (nextIndex > stride ?nextIndex - stride : 0)  
             //nextIndex > stride带分配的不止一个段   nextIndex<=stride待分配的只有第一个段了，设为0即可
            // ③
            // 记住负责的扩容区域边界                   
            bound = nextBound;
            // 此时i就为当前负责的扩容区域的索引
            i = nextIndex - 1;
            advance = false;
        }  
        //cas竞争失败，当前段被其他线程竞争到了，重新进入while竞争， bound每次更新-stride往前申请
    }
  ...      
```
依据前面的内容，ConcurrentHashMap是支持并发扩容的，那么，每个线程将有自己负责进行扩的区域：
首先，advance为false时，表示当前线程正在处理某个区域的扩容任务；advance为true时，表示当前线程正在寻找下一个要扩容的区域，
  或在自己处理的区域上往前索引。
紧接着，transferIndex表示了还未被非配的区域的索引边界，当线程领取某个区域的扩容任务时，需要更新transferIndex值。因此，
  将在transferIndex上产生竞争条件，则需要nextIndex保存竞争条件前transferIndex的值，以通过CAS进行竞争。
最后，线程通过CAS竞争到了某个区域的扩容任务，bound记录了边界，i记录了当前位置。

那么， 在①处：意味着当前线程还没处理完此区域的扩容任务，因为还未超过边界bound；
在②处：意味着已经没有区域要分配了，要不就是都处理完了，要不就是所有区域都有其他线程在处理了，不再需要当前线程的参与了；
在③处：是否还记得stride？代表每个区域的长度。所有进入的线程，通过CAS拿到自己负责的区域，并更新transferIndex，记住边界bound，
  以及当前处理的索引位置；过程如图
java_ConcourrentHashMap_扩容1.webp


TRANSFERINDEX通过unsafe获取transferIndex属性的引用地址
可以简单说明的是，TRANSFERINDEX 为 transferIndex的内存地址，因对transferIndex的更新有竞争条件，
通过CAS操作就可以保证对transferIndex操作的原子性。CAS操作可以简单理解为由底层保证的单一变量的原子操作，接受对象地址，预期值，
要更新的值为参数，当预期值符合，并成功把值设置为要更新的值时，返回true。

因此，通过竞争更新transferIndex的值，线程就获得了table上某段区域的扩容任务，nextIndex(预期值)辅助了参与CAS竞争(要知道transferIndex可能在调用CAS前改变了)。

扩容前处理
```
 private final void transfer(Node<K,V>[] tab, Node<K,V>[] nextTab) {
...
if (i < 0 || i >= n || i + n >= nextn) {
    // 如果一次遍历完成了
    // 也就是整个map所有桶中的元素都迁移完成了
    int sc;
    if (finishing) {
        // 扩容结束了  成员属性nextTable置为空
        nextTable = null;
        // 指向新的table
        table = nextTab;
        // 更新当前table的最大容量  设置下一次扩容门槛为新桶数组容量的0.75倍   
        // (n << 1) - (n >>> 1)=2-0.5=1.5  新桶为2  sizeCtl仍为新桶的0.75  
        //sizeCtl=0.75*table.length()
        sizeCtl = (n << 1) - (n >>> 1);
        //扩容完成，退出for循环与transfer函数
        return;
    }
    //根据i判断扩容完成，更新sizeCtl  sizeCtl-1
    if (U.compareAndSwapInt(this, SIZECTL, sc = sizeCtl, sc - 1)) {
      ///*①*/ 说明当前线程完成了区域的扩容任务，更新sizeCtl的值成功  把扩容线程数-1
        if ((sc - 2) != resizeStamp(n) << RESIZE_STAMP_SHIFT) 
            //说明整个扩容还没结束，但是自己的扩容任务也完成了，直接返回
            // 扩容完成两边肯定相等   扩容完成 sizeCtl= (resizeStamp(n) << RESIZE_STAMP_SHIFT)+1 也就是sc+2 sc记录的是(sc-1)前的，所以扩容完成两边相等
            return;
        //更新sizeCtl完成后，将finishing置为true，这个操作只能由一个线程完成，所以有cas竞争    
        // 说明整个扩容结束，将再次进到循环，然后返回     finishing为true才会走到上面的if条件
        finishing = advance = true;
        // i重新赋值为n
        // 这样会再重新遍历一次桶数组，看看是不是都迁移完成了
        // 也就是第二次遍历都会走到下面的(fh = f.hash) == MOVED这个条件
        i = n; // recheck before commit
    }
}
else if ((f = tabAt(tab, i)) == null)
     // f记住了当前位置的 node
        // 将其更新为ForwardingNode，以标记此位置已经处理
        // 因为没有内容，尝试直接标记为处理了，失败会再进来for循环，失败可能被其他线程处理了
    //// 如果桶中无数据，直接放入ForwardingNode标记该桶已迁移   
    advance = casTabAt(tab, i, null, fwd);
    //重新进入for，处理下一个node
else if ((fh = f.hash) == MOVED)
    // 说明这个位置已经处理过了
    // 如果桶中第一个元素的hash值为MOVED
    // 说明它是ForwardingNode节点    ForwardingNode构造器会把hash设置为MOVED
    // 也就是该桶已迁移  重新进入for，处理下一个node
    advance = true; // already processed
```
在扩容前，通过f记录旧table[i]位置上的所有Node。如果当前位置没有内容，在旧table[i]上更新类型为ForwardingNode的Node，
  如此标记了此位置已经处理了。

扩容是并发的，在①处，处理了退出的逻辑。当前阶段，可以简要理解为，resizeStamp()得到一个标记值，当 (sc = sizeCtl) - 2 回归到这个标记值时，
  意味着并发扩容结束。前面有提到，sizeCtl表达了多种状态，这里就留下了第二问题需要回答：sizeCtl如何控制多种并发状态?
//table初始化 -1
// >0代表扩容阈值
//<-1 高位存储标记，低位存储1+线程数 第一个线程进入时sizeCtl，是+2，后续线程进入时+1，退出时-1，
//然后检查-2后是否符合resizeStamp()获得的值以判断扩容是否结束

并发结束时，以 sizeCtl = (n << 1) - (n >>> 1) 更新了最大容量。左移1位为2倍，右移1位为1/2，结果为n的1.5倍。即 n * 1.5 = 2n * 0.75，
  最大容量保持了新table长度的加载因子容量。

扩容处理
此时，拿到了table[i]上的Node —— f，无论f处是构成了链表或是红黑树，其表达的并发扩容处理控制是一致的。那么，就以链表为例，抛砖引玉。
```
private final void transfer(Node<K,V>[] tab, Node<K,V>[] nextTab) {
    ......
    // 锁住此位置，也就是分段锁  锁住node table[i]   此时synchronized位于遍历分段的for,每操作一个node就进行上锁
    // f是桶上的第一个元素，锁住后，其他线程就没法进入桶上的链表或红黑树了
    synchronized (f) {
        // 再次判断当前桶第一个元素是否有修改
         // 也就是可能其它线程先一步迁移了元素    
        if (tabAt(tab, i) == f) {
            // lh 表示索引位置不变的Node
            // hn 表示索引位置要改变的Node
            
            // 把一个链表分化成两个链表
            // 规则是桶中各元素的hash与桶大小n进行与操作
            // 等于0的放到低位链表(low)中，不等于0的放到高位链表(high)中
            // 其中低位链表迁移到新桶中的位置相对旧桶不变
            // 高位链表迁移到新桶中位置正好是其在旧桶的位置加n
            // 这也正是为什么扩容时容量在变成两倍的原因
            Node<K,V> ln, hn;
            //fh为node的hash
            if (fh >= 0) {  //可能是链表 也可能是数组上的一个
                // 这里与HashMap迁移算法基本类似
                // 唯一不同的是多了一步寻找lastRun
                // 这里的lastRun是提取出链表后面不用处理再特殊处理的子链表
                // 比如所有元素的hash值与桶大小n与操作后的值分别为 0 0 4 4 0 0 0
                // 则最后后面三个0对应的元素肯定还是在同一个桶中
                // 这时lastRun对应的就是倒数第三个节点     这样后面的三个只需移动一次
                // 至于为啥要这样处理，我也没太搞明白
                         
                // 哈希值大于 0 表示，table[i]上的Node构成了链表
                // ①
                //  n 可以理解为掩码，就runBit用来快速确定Node扩容后是否要去到新的位置   n为原来的长度
                int runBit = fh & n;   
                // 用来加快 ln 与 hn 的处理
                Node<K,V> lastRun = f;
                for (Node<K,V> p = f.next; p != null; p = p.next) {
                    int b = p.hash & n;
                    if (b != runBit) {
                        // ②
                        // 更新 lastRun 和 runBit
                        // 在此循环结束后，lastRun之后的所有Node，将去往相同的位置。
                        runBit = b;
                        lastRun = p;
                    }
                }
                // ③  记录lastRun之后的
                if (runBit == 0) {
                    // lastRun之后的所有Node在同样索引的位置
                    ln = lastRun;
                    hn = null;
                }
                else {
                   // lastRun之后的所有Node在将去往新的索引位置
                    hn = lastRun;
                    ln = null;
                }
                //记录lastRun之前的
                for (Node<K,V> p = f; p != lastRun; p = p.next) {
                    int ph = p.hash; K pk = p.key; V pv = p.val;
                    // ④
                    if ((ph & n) == 0)
                        ln = new Node<K,V>(ph, pk, pv, ln);
                    else
                        hn = new Node<K,V>(ph, pk, pv, hn);
                }
                // 将ln更新到新table的同样的索引位置
                // 低位链表的位置不变
                setTabAt(nextTab, i, ln);
                // ⑤
                // 将ln更新到新table的新的索引位置
                // 高位链表的位置是原位置加n
                setTabAt(nextTab, i + n, hn);
                // 标记原table[i]为处理了 更新为fwd
                setTabAt(tab, i, fwd);
                // 使当前线程能对当前区域的下一个位置进行处理
                // advance为true，返回上面进行--i操作   //f后面的链表处理完了，标记advance为true            
                advance = true;
            }
            else if (f instanceof TreeBin) {
                // 如果第一个元素是树节点
                // 也是一样，分化成两颗树
                // 也是根据hash&n为0放在低位树中
                // 不为0放在高位树中
                TreeBin<K,V> t = (TreeBin<K,V>)f;
                TreeNode<K,V> lo = null, loTail = null;
                TreeNode<K,V> hi = null, hiTail = null;
                int lc = 0, hc = 0;
                // 遍历整颗树，根据hash&n是否为0分化成两颗树
                for (Node<K,V> e = t.first; e != null; e = e.next) {
                    int h = e.hash;
                    TreeNode<K,V> p = new TreeNode<K,V>
                            (h, e.key, e.val, null, null);
                    if ((h & n) == 0) {
                        if ((p.prev = loTail) == null)
                            lo = p;
                        else
                            loTail.next = p;
                        loTail = p;
                        ++lc;
                    }
                    else {
                        if ((p.prev = hiTail) == null)
                            hi = p;
                        else
                            hiTail.next = p;
                        hiTail = p;
                        ++hc;
                    }
                }
                // 如果分化的树中元素个数小于等于6，则退化成链表
                ln = (lc <= UNTREEIFY_THRESHOLD) ? untreeify(lo) :
                        (hc != 0) ? new TreeBin<K,V>(lo) : t;
                hn = (hc <= UNTREEIFY_THRESHOLD) ? untreeify(hi) :
                        (lc != 0) ? new TreeBin<K,V>(hi) : t;
                // 低位树的位置不变
                setTabAt(nextTab, i, ln);
                // 高位树的位置是原位置加n
                setTabAt(nextTab, i + n, hn);
                // 标记该桶已迁移
                setTabAt(tab, i, fwd);
                // advance为true，返回上面进行--i操作
                advance = true;
            }
            ......
}
static final <K,V> void setTabAt(Node<K,V>[] tab, int i, Node<K,V> v) {
        U.putObjectVolatile(tab, ((long)i << ASHIFT) + ABASE, v);
    }
```
//下面这个解释是低位不变，高位是i+n
要理解上面的内容，需要了解是怎么在table上确定自己的索引位置的。可以直接确定的是，table的容量，总为2的倍数。确定位置的方法为，
通过Key的hash值处理得到一个值假设以ph表示，然后与table的长度做并操作，注意，table作为数组，索引从0开始。假设table长度为n，
 那么索引位置 = ph & (n - 1)。   //根据hash计算index就是 hash & (n-1)
那么：
java_ConcourrentHashMap_扩容2.webp

ph决定数据的索引位置的有效位位数，取决于（n-1）二进制最低位开始的连续为“1”的位数。在理解了这一点后，对table[i]的扩容处理，就变成了如图所示：
java_ConcourrentHashMap_扩容3.webp

在table上，i处所有的node，通过其 p & (n - 1) 均能索引到 i，并且都已经处于i位置上。
在nextTable上，nextTable长度发生了变化，意味着 (n-1) -> (2n-1)。而确定索引值是需要关心有效位的，此时有效位也放生了变化，
   因此，table[i]上的Node，将有可能在nextTable上处于不同的位置。

Node最终去向nextTable的索引位置，为nextTable[i] 或 nextTable[i + n]，这是如何得来的呢？
```
假设 n = 16 = 1 0000, n-1 = 15 = 1111。
此时确定索引位置的所有Node的ph有效位为低4位，假设都为 1010。
当 n = 2n，2n-1 = 31 = 1 1111时，原来 table[i]上的Node的ph有效位多了一位共为低5位。
有效位为 0 1010 或 1 0101。
因此，新多出来的有效位为0或1就决定了不同的去向.
为0去向nextTable[i]，为1去向nextTable[i + x].
即从前者位置 1010，后者位置 1 1010 = 1010 + n = 1010 + 1 0000.
即去向的不同取决于第五位。
```

那么:
在①处: ph & n 就可以确定新的有效位为0 或 1，决定Node的去向，runBit就代表了结果
在②处: 通过 runBit，确定从链表某个位置起，ph & n 的值不再改变，即lastRun位置起所有的Node将去往nextTable相同的位置
在③处，记录 runBit确认的那一段lastRun链表，runBit为0时，由ln记住；为1时，由hn记住，用于加快处理。
在④处，为lastRun之前的Node，按照 ph & n 的结果，都创建新的Node(同样的k,v,hash)，分别进入ln或hn
在⑤处，将ln放到nextTable[i]，hn放到nextTable[i + n]
构造ln和hn的过程如下：
java_ConcourrentHashMap_扩容4.webp

特别提醒的是，③和④表达的目的是，如何快速地整理出ln和hn以去向不同的nextTable索引位置，而不是一些文章所说的ln和hn是逆向的队列.
  一个有趣的地方是，④处创建新的Node而不是完全复用旧的Node，是因为这样可以重新分配竞争条件，可能加快并发，后续会继续说明。
  //新的node是不是可以让新的table[]快速应用(新的没上锁)，原来的node还锁着呢 后续还需要回收
  //todo 扩容过程中有可能发生hash冲突吗

而以二叉树为结构的table[i]，表达的也是同样语义，有兴趣的可以阅读，无须拘泥。


扩容小结
扩容的过程可以总结为：
1 以stride为长度单位，将table划分各个区域     多核 stride=(length/8)/cpu核数   单核stride=length  stride最小为16
2 每个参与扩容的线程，通过CAS竞争更新transferIndex，分配到负责的区域   //每次竞争transferIndex减少stride
3 每个线程知道自己负责的区域边界，对区域内的table[i]逐个进行扩容处理    //处理时对table[i]上锁synchronized
4 正在被处理的table[i]的位置，将被标记为ForwardingNode
5 每个table[i]上的Node，将可能去往nextTable的不同索引位置，nextTable[i]或nextTable[i + n]，通过新有的有效位加以区分
6 当处理完区域后，向上返回。由更上层控制，要不要继续进入transfer()。



get操作
get操作和put操作相比，显得简单了许多
```
public V get(Object key) {
    Node<K,V>[] tab; Node<K,V> e, p; int n, eh; K ek;
    //计算key的hash
    int h = spread(key.hashCode());
    if ((tab = table) != null && (n = tab.length) > 0 &&
        (e = tabAt(tab, (n - 1) & h)) != null) {
        //tabAt使用了unsafe，直接获取内存的table[index]，不需要上锁
        //e为 table[i]的元素
        if ((eh = e.hash) == h) {//再校验一次hash，防止元素变了？？
            //比较node的hash  key的地址  key相等
            if ((ek = e.key) == key || (ek != null && key.equals(ek)))
                return e.val;
        }
        else if (eh < 0)
          //从红黑树查找   eh<0   可能正在扩容，也可能是红黑树   扩容节点和红黑树节点重写了find方法
            return (p = e.find(h, key)) != null ? p.val : null;
        while ((e = e.next) != null) {
            //从链表查找
            if (e.hash == h &&
                ((ek = e.key) == key || (ek != null && key.equals(ek))))
                return e.val;
        }
    }
    return null;
}

 //扩容节点查找
 Node<K,V> find(int h, Object k) {
            // loop to avoid arbitrarily deep recursion on forwarding nodes
            outer: for (Node<K,V>[] tab = nextTable;;) {
                Node<K,V> e; int n;
                if (k == null || tab == null || (n = tab.length) == 0 ||
                    (e = tabAt(tab, (n - 1) & h)) == null)
                    return null;
                for (;;) {
                    int eh; K ek;
                    if ((eh = e.hash) == h &&
                        ((ek = e.key) == k || (ek != null && k.equals(ek))))
                        return e;
                    if (eh < 0) {
                        //如果节点是扩容的，取出新的table给tab，从新的table继续查找   扩容时会新建一个table并进行元素迁移
                        if (e instanceof ForwardingNode) {
                            tab = ((ForwardingNode<K,V>)e).nextTable;
                            continue outer;
                        }
                        else
                            return e.find(h, k);
                    }
                    if ((e = e.next) == null)
                        return null;
                }
            }
        }
```
1 计算 hash 值
2 根据 hash 值找到数组对应位置: (n - 1) & h
3 根据该位置处结点性质进行相应查找
如果该位置为 null，那么直接返回 null 就可以了
如果该位置处的节点刚好就是我们需要的，返回该节点的值即可
如果该位置节点的 hash 值小于 0，说明正在扩容，或者是红黑树  从扩容节点或红黑树节点查找   ReservationNode todo
没找到则从链表中查找


由于使用了分段锁，所以对于计数使用了CounterCell记录每一段的长度
计数
```
  public boolean isEmpty() {
        return sumCount() <= 0L;
 }
 public int size() {
        long n = sumCount();
        return ((n < 0L) ? 0 :
                (n > (long)Integer.MAX_VALUE) ? Integer.MAX_VALUE :
                (int)n);
    }
    
final long sumCount() {
        CounterCell[] as = counterCells; CounterCell a;
        //先取baseCount
        long sum = baseCount;
        //如果counterCells不为空，遍历counterCells，然后对每一段相加
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null)
                    sum += a.value;
            }
        }
        return sum;
 }    
```
CounterCell的结构很简单
```
static final class CounterCell {
       //volatile保证可见性
        volatile long value;
        CounterCell(long x) { value = x; }
}
```
addCount时更新counterCells与baseCount
```
//putVal进入addCount x=1
private final void addCount(long x, int check) {
    CounterCell[] as; long b, s;
    // 这里使用的思想跟LongAdder类是一模一样的（后面会讲）  //todo LongAdder
    // 把数组的大小存储根据不同的线程存储到不同的段上（也是分段锁的思想）
    // 并且有一个baseCount，优先更新baseCount，如果失败了再更新不同线程对应的段
    // 这样可以保证尽量小的减少冲突

    // 先尝试把数量加到baseCount上，如果失败再加到分段的CounterCell上
    if ((as = counterCells) != null ||
            !U.compareAndSwapLong(this, BASECOUNT, b = baseCount, s = b + x)) {
        CounterCell a; long v; int m;
        boolean uncontended = true;
        // 如果as为空
        // 或者长度为0
        // 或者当前线程所在的段为null
        // 或者在当前线程的段上加数量失败   CELLVALUE是CounterCell.value的地址
        if (as == null || (m = as.length - 1) < 0 ||
                (a = as[ThreadLocalRandom.getProbe() & m]) == null ||
                !(uncontended =
                        U.compareAndSwapLong(a, CELLVALUE, v = a.value, v + x))) {
            // 强制增加数量（无论如何数量是一定要加上的，并不是简单地自旋）
            // 不同线程对应不同的段都更新失败了
            // 说明已经发生冲突了，那么就对counterCells进行扩容
            // 以减少多个线程hash到同一个段的概率
            fullAddCount(x, uncontended);
            return;
        }
        if (check <= 1)
            return;
        // 计算元素个数
        s = sumCount();
    }
....   
```
使用 ThreadLocalRandom.getProbe() 得到线程的探针哈希值。
在这里，这个探针哈希值的作用是哈希线程，将线程和数组中的不用元素对应起来，尽量避免线程争用同一数组元素。探针哈希值和 map 里使用的哈希值的区别是，
当线程发生数组元素争用后，可以改变线程的探针哈希值，让线程去使用另一个数组元素，而 map 中 key 对象的哈希值，由于有定位 value 的需求，所以它是一定不能变的

fullAddCount
```
 // See LongAdder version for explanation
 private final void fullAddCount(long x, boolean wasUncontended) {
        //h 是线程的hash
        int h;
        //初始化当前线程探针  todo ThreadLocalRandom 与LongAdder的原理
        if ((h = ThreadLocalRandom.getProbe()) == 0) {
            ThreadLocalRandom.localInit();      // force initialization
            h = ThreadLocalRandom.getProbe();
            wasUncontended = true;
        }
        boolean collide = false;                // True if last slot nonempty
        for (;;) {
            CounterCell[] as; CounterCell a; int n; long v;
            if ((as = counterCells) != null && (n = as.length) > 0) {
              //counterCells 不为空
                //a 为counterCells[i]
                if ((a = as[(n - 1) & h]) == null) {
                    // cellsBusy 为1 代表counterCells正在创建或扩容
                    if (cellsBusy == 0) {            // Try to attach new Cell
                        CounterCell r = new CounterCell(x); // Optimistic create                        
                        if (cellsBusy == 0 &&
                            U.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
                            //更新cellsBusy为1成功
                            boolean created = false;
                            try {               // Recheck under lock
                                CounterCell[] rs; int m, j;
                                //再次校验counterCells不为空  counterCells[i]位置的元素为空
                                if ((rs = counterCells) != null &&
                                    (m = rs.length) > 0 &&
                                    rs[j = (m - 1) & h] == null) {
                                    //添加CounterCell到数组counterCells
                                    rs[j] = r;
                                    created = true;
                                }
                            } finally {
                                cellsBusy = 0;
                            }
                            if (created)
                                break;
                            continue;           // Slot is now non-empty
                        }
                    }
                    collide = false;
                }
                else if (!wasUncontended)       // CAS already known to fail
                    wasUncontended = true;      // Continue after rehash
                else if (U.compareAndSwapLong(a, CELLVALUE, v = a.value, v + x))
                    //更新counterCells[i]的值
                    break;
                else if (counterCells != as || n >= NCPU)
                    collide = false;            // At max size or stale
                else if (!collide)
                    collide = true;
                else if (cellsBusy == 0 &&
                         U.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
                    try {
                        if (counterCells == as) {// Expand table unless stale
                            //扩容为原来的2倍，复制元素
                            CounterCell[] rs = new CounterCell[n << 1];
                            for (int i = 0; i < n; ++i)
                                rs[i] = as[i];
                            counterCells = rs;
                        }
                    } finally {
                        cellsBusy = 0;
                    }
                    collide = false;
                    continue;                   // Retry with expanded table
                }
                //更新线程的hash
                h = ThreadLocalRandom.advanceProbe(h);
            }
            else if (cellsBusy == 0 && counterCells == as &&
                     U.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
                //counterCells为空或大小需要变更     
                boolean init = false;
                try {                           // Initialize table
                    if (counterCells == as) {
                        //初始化counterCells
                        CounterCell[] rs = new CounterCell[2];
                        rs[h & 1] = new CounterCell(x);
                        counterCells = rs;
                        init = true;
                    }
                } finally {
                    cellsBusy = 0;
                }
                if (init)
                    break;
            }
            else if (U.compareAndSwapLong(this, BASECOUNT, v = baseCount, v + x))
               //更新baseCount成功
                break;                          // Fall back on using base
        }
    }
```
fullAddCount 方法会调用 ThreadLocalRandom.localInit() 初始化当前线程的探针哈希值；当发生线程争用后，
  也会调用 ThreadLocalRandom.advanceProbe(h) 更改当前线程的探针哈希值


remove方法
```
 public V remove(Object key) {
        return replaceNode(key, null, null);
    }
  
  //cv是满足一定条件下才会移除节点  
  final V replaceNode(Object key, V value, Object cv) {
        int hash = spread(key.hashCode());
        //遍历table
        for (Node<K,V>[] tab = table;;) {
            Node<K,V> f; int n, i, fh;
            //table为空或者目标node为null，跳出
            if (tab == null || (n = tab.length) == 0 ||
                (f = tabAt(tab, i = (n - 1) & hash)) == null)
                break;
            //正在扩容中，帮助扩容    
            else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);
            else {
                V oldVal = null;
                boolean validated = false;
                synchronized (f) {
                    //锁住node，然后校验f是否发生改变
                    if (tabAt(tab, i) == f) {
                        if (fh >= 0) {
                            //f为链表或table[i]
                            validated = true;
                            for (Node<K,V> e = f, pred = null;;) {
                                K ek;
                                //node的hash相同 key地址相同或key相等
                                if (e.hash == hash &&
                                    ((ek = e.key) == key ||
                                     (ek != null && key.equals(ek)))) {
                                    V ev = e.val;
                                    if (cv == null || cv == ev ||
                                        (ev != null && cv.equals(ev))) {
                                        //传入的object为null 或者 oject==node.val 或者 node不为null并且object与node.val相等
                                        oldVal = ev;
                                        if (value != null)
                                            //传入的value不为null,进行替换 
                                            e.val = value;
                                        else if (pred != null)
                                            //移除node
                                            pred.next = e.next;
                                        else
                                            //node为第一个，将第一个设置为node.next
                                            setTabAt(tab, i, e.next);
                                    }
                                    break;
                                }
                                pred = e;
                                //遍历到最后了
                                if ((e = e.next) == null)
                                    break;
                            }
                        }
                        //f节点为红黑树
                        else if (f instanceof TreeBin) {
                            validated = true;
                            TreeBin<K,V> t = (TreeBin<K,V>)f;
                            TreeNode<K,V> r, p;
                            if ((r = t.root) != null &&
                                (p = r.findTreeNode(hash, key, null)) != null) {
                                V pv = p.val;
                                if (cv == null || cv == pv ||
                                    (pv != null && cv.equals(pv))) {
                                    oldVal = pv;
                                    if (value != null)
                                        p.val = value;
                                    else if (t.removeTreeNode(p))
                                        setTabAt(tab, i, untreeify(t.first));
                                }
                            }
                        }
                    }
                }
                if (validated) {
                    //有效节点，oldVal不为null
                    if (oldVal != null) {
                        if (value == null)
                            //传入-1 check<0不会发生扩容，更新对应的数量-1
                            addCount(-1L, -1);
                        return oldVal;
                    }
                    break;
                }
            }
        }
        return null;
    }   
```


总结
ConcurrentHashMap 是一个并发散列映射表的实现，它允许完全并发的读取，并且支持给定数量的并发更新。
相比于 HashTable 和同步包装器包装的 HashMap，
使用一个全局的锁来同步不同线程间的并发访问，同一时间点，只能有一个线程持有锁，也就是说在同一时间点，只能有一个线程能访问容器，
这虽然保证多线程间的安全并发访问，但同时也导致对容器的访问变成串行化的了。
1.6中采用ReentrantLock 分段锁的方式，使多个线程在不同的segment上进行写操作不会发现阻塞行为;1.8中直接采用了内置锁synchronized，
  难道是因为1.8的虚拟机对内置锁已经优化的足够快了？  //todo


https://zhuanlan.zhihu.com/p/104515829
ConcurrentHashMap有什么缺陷吗？
ConcurrentHashMap 是设计为非阻塞的。在更新时会局部锁住某部分数据，但不会把整个表都锁住。同步读取操作则是完全非阻塞的。
好处是在保证合理的同步前提下，效率很高。坏处是严格来说读取操作不能保证反映最近的更新。例如线程A调用putAll写入大量数据，期间线程B调用get，
则只能get到目前为止已经顺利插入的部分数据。


