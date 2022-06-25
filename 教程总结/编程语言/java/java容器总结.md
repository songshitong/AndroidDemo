
ArrayList底层使用数组，线程不安全   线程安全的是vector
1. ArrayList默认大小为0,最好初始化时设置初始大小，防止后续的多次扩容和空间浪费
2. 获取元素，通过坐标值来获取元素，无需从头遍历
3. 添加元素，先进行是否扩容判断，扩容操作会创建一个新的符合大小的数组，并将原数组中的数据迁移到新数组中，然后让 elementData 指向新数组
   如果直接向集合尾端添加数据，那么直接在该位置进行赋值
   如果向集合的中间位置 index 插入数据，则需要将数组中索引 index 后的所有数据向后推移一位，然后将数据插入到空出的位置上
   
   扩容相关： 添加元素数量超过数组容器大小开始扩容，扩容后的大小不能小于10，扩容倍数按1.5计算
   扩容会构建出一个新的符合大小的数组后，就将原数组中的元素复制到新数组中，元素移动使用System.arraycopy

4. 删除元素
   删除包含两步
   1 移动元素 将index后面的元素向前移动一位，最后一位是重复的的
   2 将数组最后元素置为null，数组大小-1

ArrayList和Vector都是用数组实现的，主要有这么四个区别：
1)Vector是多线程安全的，线程安全就是说多线程访问代码，不会产生不确定的结果。而ArrayList不是，这可以从源码中看出，
Vector类中的方法很多有synchronized进行修饰，这样就导致了Vector在效率上无法与ArrayLst相比；
2)两个都是采用的线性连续空间存储元素，但是当空间充足的时候，两个类的增加方式是不同。
3)Vector可以设置增长因子，而ArrayList不可以。
4)Vector是一种老的动态数组，是线程同步的，效率很低，一般不赞成使用

并发安全的list
new Vector<>()
Collections.synchronizedList(new ArrayList<>())
new CopyOnWriteArrayList<>()



LinkedList
LinkedList 内部通过双向链表的数据结构来实现的
不需要去请求一片连续的内存空间来存储数据，而是在每次有新的元素需要添加时再来动态请求内存空间，没有扩容

元素访问
对于双向链表可以先判断元素靠近头节点还尾节点，从而决定从头遍历还是从尾遍历

LinkedList与ArrayList对比
ArrayList 适合频繁访问，少量增删的
LinkedList适合频繁增删的，少量访问的


todo 容器常用接口，类总结


HashMap
数据结构 数组+链表+红黑树    数组的元素是Node<K,V>
    数组中每一项元素的类型分为四种可能：null、单独一个结点、链表、红黑树
元素获取 get(Object key)
 1. 计算hash，根据(n - 1) & hash得到index
 2. 如果是hash相等，key相等，k的equals相等，返回table[index]
 3. hash相等，key不等的情况 table[index]不为null，可能存在哈希冲突，从链表或红黑树获取   
 4. table[index]为null，返回null

元素插入 put(K key, V value)
1. 如果 table 还未初始化或者容量为 0 则进行resize初始化
2. 判断是否存在哈希冲突   通过hash函数计算hash，通过[(n - 1) & hash]定位元素index，如果插入位置已存在元素说明哈希冲突了
3. 如果不存在哈希冲突，则直接将该键值对存入计算出来的位置
4. 如果存在哈希冲突，则将键值对添加到该位置的红黑树或者链表上，并且在链表达到最大长度时将链表转换为红黑树
5. 当存在相同 key 的结点时，判断是否需要覆盖旧值
6. 为 LinkedHashMap 预留方法埋点  如果存在hash冲突afterNodeAccess，不存在hash冲突afterNodeInsertion
7. 当保存键值对后，进行必要的扩容resize

扩容时机
哈希数组的容量是 16，阈值loadFactor 是 0.75，容量大于12时，扩容一倍变为32
1.计算新的阈值，新建数组newTab
2. 如果table[i]不存在哈希冲突(e.next == null)，直接计算新的哈希,迁移元素newTab[e.hash & (newCap - 1)] = e 
3. 如果存在哈希冲突，将链表或红黑树的节点，通过e.hash & oldCap==0确定是否在原位置，否则是原位置+原容量
   红黑树有可能转为链表

元素移除 remove(Object key)
1. 计算hash，定位元素(n - 1) & hash
2. 如果hash相等，key相等，key.equals相等，说明是头节点
3. 可能是链表或红黑树，进行查找
4. 节点移除，链表或table[i]指向node.next，红黑树执行removeTreeNode
5. 为LinkedHashMap预留方法 afterNodeRemoval
6。 table[index]为null，直接返回

扰动函数
(h = key.hashCode()) ^ (h >>> 16)  哈希与低16位异或
1一定要尽可能降低 hash 碰撞，越分散越好；   h右移16位，高16位补0，然后与h做异或，结果高16位是h的高16位，低位是h的高16位与低16位做异或
   hash 值的生成规则同时使用到了 hashCode 的高 16 位和低 16 位，在 hashCode 的基础上加大了随机性
2算法一定要尽可能高效，因为这是高频操作, 因此采用位运算；

index计算   效果等同hash%n，但是速度更快
tab[(n - 1) & hash]
1、保证不会发生数组越界
哈希桶数组的容量是 2 的幂次方，不管 hash 值是多少，通过 (n - 1) & hash 计算得到的索引值的大小都不会超出 n 本身
n=16  15&hash  0< 结果 <15
2.保证元素尽可能的均匀分布   hashcode的生成(h = key.hashCode()) ^ (h >>> 16)，加入扰动函数，增大随机，减少hash冲突

红黑树  查询复杂度log2n
1 根节点是黑色的；
2 每个叶子节点都是黑色的空节点（NIL），也就是说，叶子节点不存储数据；
3 任何相邻的节点都不能同时为红色，也就是说，红色节点是被黑色节点隔开的；
4 每个节点，从该节点到达其可达叶子节点的所有路径，都包含相同数目的黑色节点；

链表与红黑树转换
红黑树转换为链表 <= 6  
链表转换为红黑树 8  插入第9个进行转换，同时数组长度要大于64,否则执行扩容  经过debug测试,table[]小于64不进行扩容

HashMap JDK1.7是数组+链表
头插法
```
//创建新的节点  
void createEntry(int hash, K key, V value, int bucketIndex) {
    //table[bucketIndex] 是放到新插入节点的后面,所以这里是头插法
    Entry<K,V> e = table[bucketIndex];
    table[bucketIndex] = new Entry<>(hash, key, value, e);
    size++;
}
//entry的构造器
Entry(int h, K k, V v, Entry<K,V> n) {
        value = v;
        next = n;
        key = k;
        hash = h;
    }   
```


LinkedHashMap
HashMap本身无序，LinkedHashMap可以保证元素的存储顺序和迭代顺序能够和存入顺序保持一致
LinkedHashMap通过扩展父类来实现扩展功能 通过对数组/链表/红黑树中node新增before,after，新维护了一条链表，实现保持插入顺序   
  Iterator遍历时从head节点开始，可以通过node.after获取顺序
默认是插入顺序accessOrder=false，accessOrder=true将保留访问顺序
1 重写父类get方法，实现afterNodeAccess，如果accessOrder为true，将最近访问的节点置于尾部，同时记录该节点的after，before
2 父类Hash在没有hash冲突插入元素时会调用afterNodeInsertion
 afterNodeInsertion根据removeEldestEntry确定是否最终调用afterNodeRemoval，在这其中斩断移除node对after和before的引用
 确定新的after和before 


HashTable的数据结构是table[]+链表  key,value都不能为null
线程安全靠对公开方法加锁synchronized实现，同一时刻只有一个现场能访问或者修改HashTable对象，效率不高
已经不推荐使用了，如果需要线程安全请使用ConcurrentHashMap，如果不需要请使用HashMap

ConcurrentHashMap
ConcurrentHashMap的数据结构是volatile Node<K,V>[] table + 链表或红黑树
ConcurrentHashMap只有对table修改时才会加锁，volatile保证修改后，读取数据是尽可能新的
扩容的处理 如果存在多个线程，每个负责一段进行扩容，扩容完成才进行其他操作   充分利用多核CPU的性能
CAS+synchronized的使用
通过cas控制sizeCtl，确定哪个线程有对table的初始化权   初始化的并操作通过临界条件sizeCtl控制而不是对整个方法上锁
通过cas控制sizeCtl和transferIndex，确定哪个线程对哪个段有扩容权限，获得权限后进行加锁，防止其他线程在扩容期间进行操作
对于线程的控制
第一个线程进入时，sizeCtl是+2，后续线程进入时+1，退出时-1，然后检查-2后是否符合resizeStamp()移位后获得的值以判断扩容是否结束

ConcurrentHashMap加锁只对要操作的节点node加锁，锁的粒度足够细
分段锁的意义在于：各个区域拥有自己的锁，以使得线程去往不同的区域竞争条件，以此减少线程间的相互影响。
ConcurrentHashMap在table[i]上，存储了以Node为单位的不同数据集合，每个table[i]，就是一个锁，由此分散了竞争。
transfer()方法，也是以table[i]的位置的Node为锁，再进行扩容操作。

节点是怎么样的
它是实现Map.Entry<K,V>接口。里面存放了hash，key，val，以及next节点。它的value和next节点是用volatile进行修饰，
可以保证多线程之间的可见性。 新增节点，改变val，其他线程可见

初始容量为16,扩容为2倍

key和value可以是null吗
key和value不能是null，putVal进行了校验，会抛出异常

putVal的过程
1 如果桶数组未初始化，则初始化；
2 如果待插入的元素所在的桶为空，则尝试把此元素直接插入到桶的第一个位置(CAS插入)；
3 如果正在扩容，则当前线程一起加入到扩容的过程中；  //扩容的table[i]的hash为moved
4 如果待插入的元素所在的桶不为空且不在迁移元素，则锁住这个桶（分段锁）； //防止其他线程操作
5 如果当前桶中元素以链表方式存储，则在链表中寻找该元素或者插入元素；
6 如果当前桶中元素以红黑树方式存储，则在红黑树中寻找该元素或者插入元素；
7 如果元素存在，则返回旧值；
8 如果元素不存在，整个Map的元素个数加1，并检查是否需要扩容；

扩容过程
1 以stride为长度单位，将table划分各个区域     多核 stride=(length/8)/cpu核数   单核stride=length  stride最小为16
2 每个参与扩容的线程，通过CAS竞争更新transferIndex，分配到负责的区域   //每次竞争transferIndex减少stride  从n->0
3 每个线程知道自己负责的区域边界，对区域内的table[i]逐个进行扩容处理    //处理时对table[i]上锁synchronized
4 正在被处理的table[i]的位置，将被标记为ForwardingNode //hash小于0
5 每个table[i]上的Node，将可能去往nextTable的不同索引位置，nextTable[i]或nextTable[i + n]，通过新有的有效位加以区分
   区分方式hash&n是否为0,                     
6 当处理完区域后，向上返回。由更上层控制，要不要继续进入transfer()。

get(Object key)
1 计算 hash 值
2 根据 hash 值找到数组对应位置: (n - 1) & h
3 根据该位置处结点性质进行相应查找
如果该位置为 null，那么直接返回 null 就可以了
如果该位置处的节点刚好就是我们需要的，返回该节点的值即可
如果该位置节点的 hash 值小于 0，说明正在扩容，或者是红黑树  从扩容节点或红黑树节点查找   ReservationNode todo
没找到则从链表中查找

ConcurrentHashMap使用什么技术来保证线程安全？
jdk1.7：Segment+HashEntry来进行实现的；
jdk1.8：放弃了Segment臃肿的设计，采用Node+CAS+Synchronized来保证线程安全；


ConcurrentHashMap的get方法是否要加锁，为什么？
不需要，get方法采用了unsafe方法，来保证线程安全

ConcurrentHashmap是如何计算它的size大小的 todo

ConcurrentHashMap迭代器是强一致性还是弱一致性？HashMap呢？  todo
弱一致性，HashMap强一直性。
ConcurrentHashMap可以支持在迭代过程中，向map添加新元素，而HashMap则抛出了ConcurrentModificationException，
因为HashMap包含一个修改计数器，当你调用他的next()方法来获取下一个元素时，迭代器将会用到这个计数器。

ConcurrentHashMap1.7和1.8的区别  todo
jdk1.8的实现降低锁的粒度，jdk1.7锁的粒度是基于Segment的，包含多个HashEntry，而jdk1.8锁的粒度就是Node

数据结构：jdk1.7 Segment+HashEntry；jdk1.8 数组+链表+红黑树+CAS+synchronized


HashSet
HashSet 实现了 Set 接口，不允许插入重复的元素，允许包含 null 元素，且不保证元素的迭代顺序
HashSet内部通过HashMap实现元素不重复，HashMap的key是不能重复的，向 HashSet 添加的值都会被包装为一个键值对保存到 HashMap 中，
key 即外部传入的值，value 则由 HashSet 来提供一个常量，当添加重复元素时，key与value会覆盖旧值
```
private static final Object PRESENT = new Object();
```

LinkedHashSet
LinkedHashSet是HashSet的子类，构造器执行父类HashSet的逻辑，此时的Map是LinkedHashMap  即实现了元素不可以重复，有记录了添加顺序
LinkedHashMap 都是按照元素插入顺序来排序accessOrder为false   accessOrder为true按照访问顺序排序



SparseArray
数据结构两个数组,int数组和Object数组 mKeys是递增的，用于二分查找
int[] mKeys    Object[] mValues
1              "a"
100            "b"
100            "c"

key数组的index查找
ContainerHelpers.binarySearch
1如果 mKeys 中存在对应的 key，则直接返回对应的索引值
2如果 mKeys 中不存在对应的 key
1假设 mKeys 中存在"值比 key 大且大小与 key 最接近的值的索引"为 presentIndex，则此方法的返回值为 ~presentIndex
2如果 mKeys 中不存在比 key 还要大的值的话，则返回值为 ~mKeys.length

put逻辑put(int key, E value)
1. 根据二分超找key在keys数组的位置index，如果不存在返回比key大的第一个位置的index的反码
2. 如果查到key的index，直接覆盖原值mValues[i] = value
3. 反码取反得到index，如果Values[i]没有元素，将key和value分别存入keys数组和value数组
4. 如果存在冗余数据并且没有多余的空间了，那么就先进行 GC
5. 索引 i 位置已经用于存储其它数据，将i 开始的所有数据都需要向后移动一位，并将 key 存到 mKeys[i]  这个期间可能存在扩容

扩容
由GrowingArrayUtils负责元素迁移和扩容  最终使用System.arraycopy
4以内扩充至8,大于4扩充为2倍

元素查找 get(int key)
1. 二分查找法查找指定 key 在 mKeys 中的索引值
2. index<0没找到返回null，index>0找到了，返回mValues[i]

元素移除 delete(int key)
1.用二分查找法查找指定 key 在 mKeys 中的索引值
2. index>0代表元素找到了，mValues[i] = DELETED，标记当前需要垃圾回收mGarbage=true

垃圾回收
因为 SparseArray 中会出现只移除 key 和 value 两者之一的情况，导致当前数组中的有效数据并不是全都紧挨着排列在一起的，
即存在无效值，因此 gc() 方法会根据 mValues 中到底存在多少有效数据，将 mKeys 和 mValues 中的数据进行重新排列，
将有意义的元素值紧挨着排序在一起
将非DELETED的元素，向前移动，覆盖DELETED的元素

SparseArray与HashMap对比
1 避免了基本数据类型 int 的装箱拆箱操作
2 和 HashMap 中每个存储结点都是一个类对象不同，SparseArray 不需要用于包装 key 的结构体，单个元素的存储成本更加低廉
3 在数据量不大的情况下，查找效率较高（二分查找法）
4 延迟了垃圾回收的时机，只在需要的时候才一次性进行
5 内存申请比较克制，默认申请数组大小是10

劣势有以下几点：
1 具有特定的适用范围，key 只能是 int 类型
2 插入键值对时可能需要移动大量的数组元素，key和value数组都需要移动
3 数据量较大时，查找效率（二分查找法）会明显降低，需要经过多次折半查找才能定位到目标数据。而 HashMap 在没有哈希冲突的情况下
只需要进行一次哈希计算即可定位到目标元素，有哈希冲突时也只需要对比链表或者红黑树上的元素即可


ArrayMap
数据结构：两个数组   hash数组是连续增长的
key的hash数组      object数组  key与value相邻存储
index=0,hash0     index=0,key="a"
                  index=1,value="a"

index=1,hash1     index=2,key="b"
                  index=3,value="b"

获取元素
1. 根据key的hashCode在mHash数组中查找位置，范围index
2. 如果index<0直接返回，key不存在
3. index>0，key.equals(mArray[index<<1] key相等，返回对应的index
4. index>0，key不相等，向index两侧查找，冲突时键值对是连续的，返回对应的index，没找到返回~(length-1)，应该存在的位置
5。 根据计算的index*2+1  index<0返回null
   
插入元素 put(K key, V value)
1 根据二分查找法获取到 keyHash 在 mHashes 中的索引位置 index
2 如果 index 大于等于 0，说明在 mArray 中 key 已存在，那么直接覆盖旧值即可，结束流程
3 如果 index 小于 0，说明在 mArray 中 key 不存在，~index 此时代表的是 keyHash 按照递增顺序应该插入 mHashes 的位置
4 判断是否需要扩容，需要的话则进行扩容。如果符合缓存标准的话，则会缓存扩容前的数组，不缓存扩容后就被回收了
5 对最终的数组进行数据迁移，插入 key-value 和 keyHash  在两个数组中，存入三个值

扩容机制  初始申请容量为0
没超过4，设为4,超过设为8，当数组长度已超出 8 后，数组容量按照 1.5 倍来扩容

移除元素
removeAt(int index)
1. ArrayMap只多有一个元素，则释放内存 freeArrays
2. 当内存使用量不足1/3的情况下，内存数组会收紧50%
3. 元素搬移

缓存机制
ArrayMap 内部包含了对 mHashes 和 mArray 这两个数组进行缓存的机制，避免由于频繁创建数组而造成内存抖动
分别缓存大小为4和8的ArrayMap对象
每当 ArrayMap 完成数组扩容后就会调用此方法对扩容前的数组进行缓存，但也不是所有数组都会进行缓存，而是有着数组长度和缓存总数这两方面的限制

对比总结
数据结构
ArrayMap和SparseArray采用的都是两个数组，Android专门针对内存优化而设计的
HashMap采用的是数组+链表+红黑树
内存优化
ArrayMap比HashMap更节省内存，综合性能方面在数据量不大的情况下，推荐使用ArrayMap；
Hash需要创建一个额外对象来保存每一个放入map的entry，且容量的利用率比ArrayMap低，整体更消耗内存
SparseArray比ArrayMap节省1/3的内存，但SparseArray只能用于key为int类型的Map，所以int类型的Map数据推荐使用SparseArray；
SparseArray不需要保存key所对应的哈希值
性能方面：
ArrayMap查找时间复杂度O(logN)；ArrayMap增加、删除操作需要移动成员，速度相比较慢，对于个数小于1000的情况下，性能基本没有明显差异
HashMap查找、修改的时间复杂度为O(1)；
SparseArray适合频繁删除和插入来回执行的场景，性能比较好 利用延迟gc
//二分搜索的性能可控，数据量增加性能不会急剧下降，但是HashMap随着数据量增加，哈希冲突也会增加
缓存机制
ArrayMap针对容量为4和8的对象进行缓存，可避免频繁创建对象而分配内存与GC操作，这两个缓存池大小的上限为10个，防止缓存池无限增大；
HashMap没有缓存机制
SparseArray有延迟回收机制，提供删除效率，同时减少数组成员来回拷贝的次数
扩容机制
ArrayMap是在容量满的时机触发容量扩大至原来的1.5倍，在容量不足1/3时触发内存收缩至原来的0.5倍，更节省的内存扩容机制   
HashMap是在容量的0.75倍时触发容量扩大至原来的2倍，且没有内存收缩机制。HashMap扩容过程有hash重建，相对耗时。所以能大致知道数据量，
可指定创建指定容量的对象，能减少性能浪费。
并发问题
ArrayMap是非线程安全的类，大量方法中通过对mSize判断是否发生并发，来决定抛出异常。但没有覆盖到所有并发场景，比如大小没有改变而成员内容改变的情况就没有覆盖
HashMap是在每次增加、删除、清空操作的过程将modCount加1，在关键方法内进入时记录当前mCount，执行完核心逻辑后，再检测mCount是否被其他线程修改，
来决定抛出异常。这一点的处理比ArrayMap更有全面。

