jdk 1.8   android sdk 30
https://juejin.cn/post/6931007634028003341
https://blog.csdn.net/weixin_42373997/article/details/112085344
了解容器的构造建议先学习数据结构
线程不安全查看 彤哥读源码-调试大法/14 实战_为什么HashMap不是线程安全的.md
线程安全ConcurrentHashMap   

Java_HashMap结构图

HashMap 是一种用于存储键值对的数据类型，基于哈希表的 Map 接口的非同步实现，key 可以为 null，不允许插入重复的 key，
允许 value 重复，value可以为null    //当key为null时取的hasHCode为0
HashMap 实际上是数组+链表+红黑树的结合体，其底层包含一个数组，数组中每一项元素的类型分为四种可能：null、单独一个结点、链表、红黑树
（JDK1.8 开始通过使用红黑树来提高元素查找效率）。当往 HashMap 中存入元素时，会先根据 key 的哈希值得到该元素在数组中的位置
（即数组下标），如果该位置上已经存放有其它元素了，那么在这个位置上的元素将以链表或者红黑树的形式来存放，如果该位置上没有元素，
就直接向该位置存放元素。因此 HashMap 要求 key 必须是不可变对象，即 key 的哈希值不能发生改变，否则就会导致后续访问时无法定位到
它的存放位置了

1、哈希
Hash，一般翻译做哈希或者散列，是把输入的任意对象通过哈希算法变换成固定长度的输出，该输出就是哈希值。不同的输入可能会哈希成相同的输出，
所以不可能从哈希值来确定唯一的输入值，但可以将哈希值作为这个对象的一个特征

HashMap 内部就采用了哈希算法来存储元素。但由于哈希算法对于不同的输入有可能会哈希成相同的输出，而且数组空间不可能是无限大的，
所以在同个数组位置上就不可避免的需要存储多个元素了，这种情况就叫做哈希冲突。此外，HashMap 不保证元素的存储顺序和迭代顺序，
因为根据需要 HashMap 会对元素重新哈希，元素的顺序也会被再次打乱，因此在不同时间段其存储顺序和迭代顺序都可能会发现变化。
此外，HashMap 也不保证线程安全，如果有多个线程同时进行写操作的话可能会导致数据错乱甚至线程死锁


2 类声明   泛型必须是类，数字作为key,value，需要装箱
```   
 public class HashMap<K, V> extends AbstractMap<K, V> 
        implements Map<K, V>, Cloneable, Serializable
```

3、常量
HashMap 中的全局常量主要看以下几个
```
    //哈希桶数组的默认容量  1*(2的4次方)=16
    static final int DEFAULT_INITIAL_CAPACITY = 1 << 4;

    //哈希桶数组能够达到的最大容量 1*(2的30次方)
    static final int MAXIMUM_CAPACITY = 1 << 30;
	
    //装载因子
    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    //为了提高效率，当链表的长度大于这个值时，就将链表转换为红黑树
    static final int TREEIFY_THRESHOLD = 8;
    //转换为树时数组的最小长度
    static final int MIN_TREEIFY_CAPACITY = 64
	
    //当红黑树的长度小于这个值时，就将红黑树转换为链表
    static final int UNTREEIFY_THRESHOLD = 6;
```
装载因子用于规定数组在自动扩容之前数据占有其容量的最高比例，即当数据量占有数组的容量达到这个比例后，数组将自动扩容。
  装载因子衡量的是一个散列表的空间的使用程度，装载因子越大表示散列表的装填程度越高，反之愈小。对于使用链表的散列表来说，
  查找一个元素的平均时间是O(1+a)，因此装载因子越大，对空间的利用程度就越高，相对应的是查找效率越低。如果装载因子太小，
  那么数组的数据将过于稀疏，对空间的利用率就变低，相应查找效率也会提升

官方默认的装载因子大小是 DEFAULT_LOAD_FACTOR，即 0.75，是平衡空间利用率和查找效率两者之后的结果。在实际情况中，
如果内存空间较多而对时间效率要求很高，可以选择降低装载因子大小；如果内存空间紧张而对时间效率要求不高，则可以选择加大装载因子

此外，即使装载因子和哈希算法设计得再合理，也难免会出现由于哈希冲突导致链表长度过长的情况，这也将影响 HashMap 的性能。
  为了优化性能，从 JDK1.8 开始引入了红黑树，当链表长度超出 TREEIFY_THRESHOLD 规定的值时，链表就会被转换为红黑树，
  利用红黑树快速增删改查的特点以提高 HashMap 的性能

4、变量     // transient 不进行序列化
```
    //哈希桶数组，在第一次使用时才初始化  putVal中通过resize()方法初始化
    //容量值应是2的整数倍
    transient Node<K, V>[] table;

    /**
     * Holds cached entrySet(). Note that AbstractMap fields are used
     * for keySet() and values().   //todo 
     */
    transient Set<Map.Entry<K, V>> entrySet;

    //Map的大小
    transient int size;

    //每当Map的结构发生变化时，此参数就会递增
    //当在对Map进行迭代操作时，迭代器会检查此参数值
    //如果检查到此参数的值发生变化，就说明在迭代的过程中Map的结构发生了变化，因此会直接抛出异常
    transient int modCount;

    //数组的扩容临界点，当数组的数据量达到这个值时就会进行扩容操作
    //计算方法：当前容量 x 装载因子
    int threshold;

    //使用的装载因子值
    final float loadFactor;
```
threshold   [ˈθreʃhəʊld]   门槛;门口;阈;界;起始点;开端;起点;入门

5、构造函数    
```
    //设置Map的初始化大小和装载因子
    public HashMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal initial capacity: " + initialCapacity);
        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("Illegal load factor: " + loadFactor);
        this.loadFactor = loadFactor;
        this.threshold = tableSizeFor(initialCapacity);
    }

    //设置初始化大小
    public HashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    //使用默认值
    public HashMap() {
        this.loadFactor = DEFAULT_LOAD_FACTOR;
    }

    //传入初始数据
    public HashMap(Map<? extends K, ? extends V> m) {
        this.loadFactor = DEFAULT_LOAD_FACTOR;
        putMapEntries(m, false);
    }
    //找到大于等于给定容量的最小2的次幂值
    //保证数组大小是2的n次方，输入10返回16  
    //算法就是让初始二进制右移 1，2，4，8，16 位，分别与自己异或，把高位第一个为 1 的数通过不断右移，把高位为 1 的后面全变为 1
     static final int tableSizeFor(int cap) {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }
    
   final void putMapEntries(Map<? extends K, ? extends V> m, boolean evict) {
        int s = m.size();
        if (s > 0) {
            if (table == null) { // pre-size
                float ft = ((float)s / loadFactor) + 1.0F;
                int t = ((ft < (float)MAXIMUM_CAPACITY) ?
                         (int)ft : MAXIMUM_CAPACITY);
                if (t > threshold)
                    threshold = tableSizeFor(t);
            }
            else if (s > threshold)
                resize();
            for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
                K key = e.getKey();
                V value = e.getValue();
                putVal(hash(key), key, value, false, evict);
            }
        }
    } 
```

6、插入键值对
在上边说过，HashMap 是 数组+链表+红黑树 的结合体，数组中每一项元素的类型分为四种可能：null、单独一个结点、链表、红黑树
每一个要插入的键值对都会被包装为 Node 对象，根据 key 的哈希值来决定 Node 对象在数组中的位置。如果计算出的位置此时不包含值
则直接将 Node 对象放到该位置即可；如果包含值则说明发生了哈希碰撞，此时就需要将 Node 对象插入到链表或者是红黑树中。
如果 key 与链表或红黑树中某个已有结点的 key 相等（hash 值相等且两者 equals 成立），则新添加的 Node 对象将覆盖原有数据
当哈希算法的计算结果越分散均匀，发生哈希碰撞的概率就越小，HashMap 的存取效率就会越高
Node 类的声明如下所示
```
    static class Node<K,V> implements Map.Entry<K,V> {
        //key 的哈希值
        final int hash;
        final K key;
        V value;
        //下一个结点
        Node<K,V> next;

        Node(int hash, K key, V value, Node<K,V> next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }

        public final K getKey()        { return key; }
        public final V getValue()      { return value; }
        public final String toString() { return key + "=" + value; }

        public final int hashCode() {
            return Objects.hashCode(key) ^ Objects.hashCode(value);
        }

        public final V setValue(V newValue) {
            V oldValue = value;
            value = newValue;
            return oldValue;
        }

        public final boolean equals(Object o) {
            if (o == this)
                return true;
            if (o instanceof Map.Entry) {
                Map.Entry<?,?> e = (Map.Entry<?,?>)o;
                if (Objects.equals(key, e.getKey()) &&
                    Objects.equals(value, e.getValue()))
                    return true;
            }
            return false;
        }
    }
    
    //TreeNode更多HashMap_TreeNode.md
    static final class TreeNode<K,V> extends LinkedHashMap.LinkedHashMapEntry<K,V> {
        TreeNode<K,V> parent;  // red-black tree links
        TreeNode<K,V> left;
        TreeNode<K,V> right;
        TreeNode<K,V> prev;    // needed to unlink next upon deletion
        boolean red;
        TreeNode(int hash, K key, V val, Node<K,V> next) {
            super(hash, key, val, next);
        }
   }
```

插入键值对的方法是 put(K key, V value)
```
    public V put(K key, V value) {
        return putVal(hash(key), key, value, false, true);
    }

    //计算 key 的哈希值
    static final int hash(Object key) {
        int h;
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }
```

putVal 方法较为复杂，因为该方法要考虑以下几种情况：
1如果 table 还未初始化或者容量为 0 则进行初始化
2判断是否存在哈希冲突
3如果不存在哈希冲突，则直接将该键值对存入计算出来的位置
4如果存在哈希冲突，则将键值对添加到该位置的红黑树或者链表上，并且在链表达到最大长度时将链表转换为红黑树，数组要求达到64否则进行扩容
5当存在相同 key 的结点时，判断是否需要覆盖旧值
6为 LinkedHashMap 预留方法埋点 如果存在hash冲突afterNodeAccess，不存在hash冲突afterNodeInsertion
7当保存键值对后，进行必要的扩容
```
    /**
     * @param hash         hash for key
     * @param key          the key
     * @param value        the value to put
     * @param onlyIfAbsent 为 true 表示不会覆盖有相同 key 的非 null value，否则会覆盖原有值
     * @param evict        if false, the table is in creation mode.
     * @return previous value, or null if none
     */
    final V putVal(int hash, K key, V value, boolean onlyIfAbsent, boolean evict) {
        Node<K, V>[] tab;
        //p 指向了键值对希望存入的数组位置
        Node<K, V> p;
        //n 数组长度，i是p在数组的index
        int n, i;
        //如果 table 还未初始化或者容量为0，则调用 resize 方法进行初始化
        if ((tab = table) == null || (n = tab.length) == 0)
            n = (tab = resize()).length;
        
        //判断要存入的 key 是否存在哈希冲突
        //p 指向了键值对希望存入的数组位置
        //p 等于 null 说明不存在冲突
        if ((p = tab[i = (n - 1) & hash]) == null)
            //直接在索引 i 处构建包含待存入元素的结点
            tab[i] = newNode(hash, key, value, null);
        
        else { //走入本分支，说明待存入的 key 存在哈希冲突
            
            Node<K, V> e;
            K k;
            //p 值已在上一个 if 语句中赋值了，此处就直接来判断 Node key 的相等性
            if (p.hash == hash && ((k = p.key) == key || (key != null && key.equals(k))))
                //会走进这里，说明 p 结点 key 和待存入的键值对 key 相等    需要对value进行操作
                //此时该位置可能只有一个结点，也有可能是红黑树或者链表，
                //那么 e 就指向该冲突结点
                //此时就已经找到了键值对待存入的位置了
                e = p;
            
            //如果 Node key 不相等，且头结点是 TreeNode 类型，说明此时该位置当前是采用红黑树来处理哈希冲突
            else if (p instanceof TreeNode)
                //如果红黑树中不存在相同 key 的话则插入保存键值对并返回 null，否则不保存并返回该该相同 key 的结点
                e = ((TreeNode<K, V>) p).putTreeVal(this, tab, hash, key, value);
            
            else { //该位置当前是采用链表来处理哈希冲突
                for (int binCount = 0; ; ++binCount) {
                    if ((e = p.next) == null) {
                        //会走进这里，说明遍历到了链表尾部，且链表中每个结点的 key 均不相等
                        //那么就将其添加到链表尾部
                        p.next = newNode(hash, key, value, null);
                        //如果链表的长度已达到允许的最大长度，那么就将链表转换为红黑树
                        //treeifyBin要求数组长度达到64,否则执行扩容操作
                        if (binCount >= TREEIFY_THRESHOLD - 1) // -1 for 1st
                            treeifyBin(tab, hash);
                        break;
                    }   
                    if (e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k))))
                        //找到了相同 key 的结点，即 e，后面将e的节点的value更新
                        break;
                    p = e;
                }
            }
            
            //如果 e != null，说明原先存在相同 key 的键值对
            //那么就再来判断下是否需要覆盖 value
            if (e != null) {    
                V oldValue = e.value;       
                //如果 onlyIfAbsent 为 false 或者 oldValue 为 null 则覆盖原有值
                if (!onlyIfAbsent || oldValue == null)
                    e.value = value;
                
                //用于 LinkedHashMap ，在 HashMap 中是空实现
                afterNodeAccess(e);
                return oldValue;
            }
        }
        
        ++modCount;
        
        //判断是否需要扩容
        if (++size > threshold)
            resize();
        
        //用于 LinkedHashMap ，在 HashMap 中是空实现
        afterNodeInsertion(evict);
        return null;
    }
    
   //todo 链表转红黑树的原理 
   final void treeifyBin(Node<K,V>[] tab, int hash) {
        int n, index; Node<K,V> e;
        if (tab == null || (n = tab.length) < MIN_TREEIFY_CAPACITY)
            resize();
        else if ((e = tab[index = (n - 1) & hash]) != null) {
            TreeNode<K,V> hd = null, tl = null;
            do {
                TreeNode<K,V> p = replacementTreeNode(e, null);
                if (tl == null)
                    hd = p;
                else {
                    p.prev = tl;
                    tl.next = p;
                }
                tl = p;
            } while ((e = e.next) != null);
            if ((tab[index] = hd) != null)
                hd.treeify(tab);
        }
    }   
    TreeNode<K,V> replacementTreeNode(Node<K,V> p, Node<K,V> next) {
        return new TreeNode<>(p.hash, p.key, p.value, next);
    }   
```

获取 value
获取 value 对应的是 get(Object key)方法
```
    public V get(Object key) {
        Node<K, V> e;
        return (e = getNode(hash(key), key)) == null ? null : e.value;
    }
    
    //获取值，没有赋值为default
    public V getOrDefault(Object key, V defaultValue) {
        Node<K,V> e;
        return (e = getNode(hash(key), key)) == null ? defaultValue : e.value;
    }

    //根据 key 获取结点
    final Node<K, V> getNode(int hash, Object key) {
        Node<K, V>[] tab;
        Node<K, V> first, e;
        int n;
        K k;
        //只有当 table 不为空且 hash 对应的位置不为 null 时说明才有可能存在该 key
        if ((tab = table) != null && (n = tab.length) > 0 && (first = tab[(n - 1) & hash]) != null) {
            if (first.hash == hash && ((k = first.key) == key || (key != null && key.equals(k))))
                //如果与头结点相等的话说明找到了对应值
                return first;
            // e != null 说明存在该位置存在链表或红黑树，那么就从这两者中获取
            if ((e = first.next) != null) {
                if (first instanceof TreeNode) //红黑树
                    return ((TreeNode<K, V>) first).getTreeNode(hash, key);
                do { //链表
                    if (e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k))))
                        return e;
                } while ((e = e.next) != null);
            }
        }
        return null;
    }
```


移除结点
从 Map 中移除键值对的操作，对于其底层数据结构的体现就是要移除对某个 Node 对象的引用，这个数据结构可能是数组、红黑树、或者链表
```
    //如果真的存在该 key，则返回对应的 value，否则返回 null
    public V remove(Object key) {
        Node<K, V> e;
        return (e = removeNode(hash(key), key, null, false, true)) == null ?
                null : e.value;
    }

    /**
     * @param value       key对应的值，只有当matchValue为true时才需要使用到，否则忽略该值
     * @param matchValue  如果为 true ，则只有当找到key和value均匹配的结点时才会移除该结点，否则只要key相等就直接移除该元素
     * @param movable if false do not move other nodes while removing
     * @return the node, or null if none
     */
    final Node<K, V> removeNode(int hash, Object key, Object value,
                                boolean matchValue, boolean movable) {
        Node<K, V>[] tab;
        //p = tab[index = (n - 1) & hash]  p可能是数组，或者链表和红黑树的第一个
        Node<K, V> p;
        //n = tab.length  哈希数组的长度
        //index = (n - 1) & hash  哈希数组的index
        int n, index;
        //只有当 table 不为空且 hash 对应的位置不为 null 时说明才有可能存在该 key
        if ((tab = table) != null && (n = tab.length) > 0 && (p = tab[index = (n - 1) & hash]) != null) {
            //node用于遍历链表或红黑树，也可能是数组那一个
            Node<K, V> node = null, e;
            K k;
            V v;
            if (p.hash == hash && ((k = p.key) == key || (key != null && key.equals(k))))
                //如果与头结点 p 的 key 相等，那么就已经找到了目标 node
                node = p;
            else if ((e = p.next) != null) { //存在红黑树或者链表
                if (p instanceof TreeNode) //红黑树
                    node = ((TreeNode<K, V>) p).getTreeNode(hash, key);
                else { //链表
                    do {
                        if (e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k)))) {
                            node = e;
                            break;
                        }
                        p = e;
                    } while ((e = e.next) != null);
                }
            }
            
            //node != null 说明存在 key 对应结点
            //如果 matchValue 为 false ，则此处就可以直接移除结点 node
            //如果 matchValue 为 true ，则当 value 相等时才需要移除该结点
            if (node != null && (!matchValue || (v = node.value) == value || (value != null && value.equals(v)))) {
                if (node instanceof TreeNode) //红黑树
                    ((TreeNode<K, V>) node).removeTreeNode(this, tab, movable);
                else if (node == p) //对应 key 与头结点相等的情况，此时直接将指针移向下一位即可
                    tab[index] = node.next;
                else //链表
                    p.next = node.next;
                ++modCount;
                --size;
                //用于 LinkedHashMap ，在 HashMap 中是空实现
                afterNodeRemoval(node);
                return node;
            }
        }
        return null;
    }
```

哈希算法
在插入、查询和移除键值对时，定位到哈希桶数组的对应位置都是很关键的第一步，只有 HashMap 中的元素尽量分布均匀，才能尽量
让数组中的每个位置都只保存一个 Node，避免频繁地去构建和遍历链表或者红黑树，这就需要依靠于一个比较好的哈希算法了
以下是 HashMap 中计算 key 值的哈希值以及根据哈希值获取其在哈希桶数组中位置的方法
```
    static final int hash(Object key) {
        int h;
        //高位参与运算
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }

    //根据 key 值获取 Value
    public V get(Object key) {
        Node<K, V> e;
        return (e = getNode(hash(key), key)) == null ? null : e.value;
    }
    	//查找指定结点
    final Node<K, V> getNode(int hash, Object key) {
		···
        //只有当 table 不为空且 hash 对应的位置不为 null 才有可获取的元素值
        if ((tab = table) != null && (n = tab.length) > 0 && (first = tab[(n - 1) & hash]) != null) {
           ···
        }
        return null;
    }
```
可以看到，key 的哈希值是按照 (h = key.hashCode()) ^ (h >>> 16)的算法来得到的，该算法可以拆解为三步：
通过 key.hashCode() 拿到 key 的 hashCode，即 h
通过 h >>> 16 将 h 的高 16 位迁移到低 16 位，高 16 位全变成 0
将以上两步得到的值进行异或运算，最终得到的结果值的高 16 位和 h 的高 16 位一样，低 16 位即 h的高16位和 h的低16位 的异或运算结果
  最终结果自己的高16位+(自己的高16位与自己的低16位相同的部分，其余为0)

// tab[(n - 1) & hash]
1、保证不会发生数组越界
key 在哈希桶数组的位置索引则是通过 (n - 1) & hash 来计算得到的，n 即哈希桶数组的容量。HashMap 要求哈希桶数组的容量是 2 的幂次方，
即要求 n 是 16、32、64、128 这种格式，相对应的 n -1 的二进制位是： 
n 等于 16，n -1 就等于 01111
n 等于 32，n -1 就等于 011111
n 等于 64，n -1 就等于 0111111
n 等于 128，n -1 就等于 01111111
//由于必是2的n次幂，n的二进制必是100...n个0   
//n-1的二进制必是0111... n个1，最高位是0, 和hash值相“与”，结果值一定不会比数组的长度值大，因此也就不会发生数组越界。

可以看出来，不管 hash 值是多少，通过 (n - 1) & hash 计算得到的索引值的大小都不会超出 n 本身，大于等于 0 且小于等于 n - 1，
这也符合我们对数组索引值范围的要求。
2.保证元素尽可能的均匀分布
hash 值的生成规则同时使用到了 hashCode 的高 16 位和低 16 位，在 hashCode 的基础上
加大了随机性，使得最终通过 (n - 1) & hash 计算得到的索引值的随机性也比较大，从而使得元素可以比较均匀地分布在哈希桶数组中，
减少了哈希冲突的概率

(n - 1) & hash 与运算，位数不同补0，结果不可能大于n-1       
map.put("z","1"); 此时n=16 hash=122   (n-1)&hash=1111 & 1111010=1010=10 放在tab[10]   122%16=10
map.put("a","1"); 此时n=16 hash=97   (n-1)&hash=1111 & 110 0001=1=1   放在tab[1]      97%16=1  
与hash%n对比
(n - 1) & hash= hash%n  但是位运算更快

(n-1)&hash
取模运算要求n为2的x次方
顺便说一下，这也正好解释了为什么 HashMap 的数组长度要取 2 的整数幂。因为这样（数组长度-1）正好相当于一个“低位掩码”。
“与”操作的结果就是散列值的高位全部归零，只保留低位值，用来做数组下标访问。以初始长度 16 为例，16-1=15。
2 进制表示是 00000000 00000000 00001111。和某散列值做“与”操作如下，结果就是截取了最低的四位值
  10100101 11000100 00100101
& 00000000 00000000 00001111
----------------------------------
  00000000 00000000 00000101    //高位全部归零，只保留末四位


(h = key.hashCode()) ^ (h >>> 16)
为什么这么设计
扰动函数，这么设计有二点原因：
1一定要尽可能降低 hash 碰撞，越分散越好；
2算法一定要尽可能高效，因为这是高频操作, 因此采用位运算；

把哈希值右移 16 位，也就正好是自己长度的一半，之后与原哈希值做异或运算，这样就混合了原哈希值中的高位和低位，增大了随机性

为什么采用 hashcode 的高 16 位和低 16 位异或能降低 hash 碰撞？hash 函数能不能直接用 key 的 hashcode？
因为 key.hashCode()函数调用的是 key 键值类型自带的哈希函数，返回 int 型散列值。int 值范围为-2147483648~2147483647，
前后加起来大概 40 亿的映射空间。只要哈希函数映射得比较均匀松散，一般应用是很难出现碰撞的。但问题是一个 40 亿长度的数组，内存是放不下的。你想，
如果 HashMap 数组的初始大小才 16，用之前需要对数组的长度取模运算，得到的余数才能用来访问数组下标。(来自知乎-胖君)





10、扩容
如果哈希桶数组很大，即使是较差的哈希算法，元素也会比较分散；如果哈希桶数组很小，即使是好的哈希算法也会出现较多哈希碰撞的情况，
所以就需要在空间成本和时间成本之间权衡，除了需要设计较好的哈希算法以便减少哈希冲突外，也需要在合适的的时机对哈希桶数组进行扩容

当 HashMap 中的元素越来越多时，因为数组的容量是固定的，所以哈希冲突的几率也会越来越高，为了提高效率，此时就需要对 HashMap 中的数组进行扩容，
 而扩容操作最消耗性能的地方就在于：原数组中的数据必须重新计算其在新数组中的位置并迁移到新数组中

那么 HashMap 扩容操作的触发时机是什么时候呢？当 HashMap 中的元素个数超出 threshold 时（数组容量 与 loadFactor 的乘积），
  就会进行数组扩容。例如，假设数组当前大小是 16，loadFactor 值是 0.75，那么当 HashMap 中的元素个数达到 12 个时，
  就会自动触发扩容操作，把数组的大小扩充到 2 * 16 = 32，即扩大一倍，然后重新计算每个元素在新数组中的位置，这是一个非常消耗性能的操作，
  所以如果已经预知到待存入 HashMap 的数据量，那么在初始化 HashMap 时直接指定初始化大小会是一种更为高效的做法

默认情况下，哈希数组的容量是 16，loadFactor 是 0.75，这是平衡空间利用率和时间效率两者之后的结果

初始化数组和扩容数组这两个操作对应的是 resize()方法
```
    final Node<K, V>[] resize() {
        //扩容前的数组
        Node<K, V>[] oldTab = table;
        //扩容前数组的容量
        int oldCap = (oldTab == null) ? 0 : oldTab.length;
        //当前的扩容临界值
        int oldThr = threshold;
        //扩容后的数组容量和扩容临界值
        int newCap, newThr = 0;
        if (oldCap > 0) { 
            //oldCap > 0 对应的是 table 已被初始化的情况，此时是来判断是否需要进行扩容
            
            //如果数组已达到最大容量，则不再进行扩容，并将扩容临界点 threshold 提升到 Integer.MAX_VALUE，结束
            if (oldCap >= MAXIMUM_CAPACITY) {
                threshold = Integer.MAX_VALUE;
                return oldTab;
            } else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY && oldCap >= DEFAULT_INITIAL_CAPACITY) {
                //如果将数组的现有容量提升到两倍依然小于 MAXIMUM_CAPACITY，且现有容量大于等于 DEFAULT_INITIAL_CAPACITY
                //则将数组的容量和扩容临界值均提升为原先的两倍
                newThr = oldThr << 1;
            } 
            
            //此处应该还有一种情况
            //即将数组的现有容量提升到现在的两倍后大于等于 MAXIMUM_CAPACITY 的情况
            //此时 newThr 等于 0，newCap 等于 oldCap 的两倍值
            //此处并没有对 newCap 的数值进行还原，说明 HashMap 是允许扩容后容量超出 MAXIMUM_CAPACITY 的
            //只是在现有容量超出 MAXIMUM_CAPACITY 后，不允许再次进行扩容
        } else if (oldThr > 0) { 
            //oldCap <= 0 && oldThr > 0
            //对应的是 table 还未被初始化，且在调用构造函数时有传入 initialCapacity 或者 Map 的情况
            //此时就直接将容量提升为 threshold，在后边重新计算新的扩容临界值
            newCap = oldThr;
        } else { 
            //oldCap <= 0 && oldThr <= 0
            //对应的是 table 还未被初始化，且调用的是无参构造函数
            //将 table 的容量扩充到默认大小，并使用默认的装载因子来计算扩容临界值
            newCap = DEFAULT_INITIAL_CAPACITY;
            newThr = (int) (DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
        }
        if (newThr == 0) {
            float ft = (float) newCap * loadFactor;
            //计算扩容后新的扩容临界值
            newThr = (newCap < MAXIMUM_CAPACITY && ft < (float) MAXIMUM_CAPACITY ? (int) ft : Integer.MAX_VALUE);
        }
        threshold = newThr;
        @SuppressWarnings({"rawtypes", "unchecked"})
        Node<K, V>[] newTab = (Node<K, V>[]) new Node[newCap];
        table = newTab;
        //如果旧数组中存在值，则需要将其中的数据复制到新数组中
        if (oldTab != null) {
            for (int j = 0; j < oldCap; ++j) {
                //j代表e在oldTab的原位置
                Node<K, V> e;
                if ((e = oldTab[j]) != null) {
                    //清空旧的值
                    oldTab[j] = null;
                    //e.next == null 说明元素 e 没有产生 hash 冲突，因此可以直接转移该元素  没有链表或红黑树
                    if (e.next == null)
                        //计算元素 e 在新数组中的位置
                        newTab[e.hash & (newCap - 1)] = e;
                    else if (e instanceof TreeNode) //存在哈希冲突且是用了红黑树
                        //有可能将红黑树转为链表
                        ((TreeNode<K, V>) e).split(this, newTab, j, oldCap);
                    else { //存在哈希冲突且是用了链表  尽可能保证顺序
                        //新的位置要么在原位置，要么在原长度+原位置  分割为两个子链表
                        Node<K, V> loHead = null, loTail = null;
                        Node<K, V> hiHead = null, hiTail = null;
                        Node<K, V> next;
                        do {
                            next = e.next;
                            //找到(e.hash & oldCap) == 0的，连在一起  不等的也使用链表连接起来
                            if ((e.hash & oldCap) == 0) {
                                if (loTail == null)
                                    loHead = e;
                                else
                                    loTail.next = e;
                                loTail = e;
                            } else {
                                if (hiTail == null)
                                    hiHead = e;
                                else
                                    hiTail.next = e;
                                hiTail = e;
                            }
                        } while ((e = next) != null);
                        if (loTail != null) {
                           //低位  新的位置在原位置
                            loTail.next = null;
                            newTab[j] = loHead;
                        }
                        if (hiTail != null) {
                            hiTail.next = null;
                            // 高位 新的位置在原长度+原位置
                            newTab[j + oldCap] = hiHead;
                        }
                    }
                }
            }
        }
        return newTab;
    }
```
https://yikun.github.io/2015/04/01/Java-HashMap%E5%B7%A5%E4%BD%9C%E5%8E%9F%E7%90%86%E5%8F%8A%E5%AE%9E%E7%8E%B0/
resize链表部分，相对jdk1.7的优化       //todo 1.7的源码以及问题
当put时，如果发现目前的bucket占用程度已经超过了Load Factor所希望的比例，那么就会发生resize。在resize的过程，
简单的说就是把bucket扩充为2倍，之后重新计算index，把节点再放到新的bucket中。resize的注释是这样描述的：
```
Initializes or doubles table size. If null, allocates in accord with initial capacity target held in field threshold. 
Otherwise, because we are using power-of-two expansion, the elements from each bin must either stay at same index, 
or move with a power of two offset in the new table.
```
大致意思就是说，当超过限制的时候会resize，然而又因为我们使用的是2次幂的扩展(指长度扩为原来2倍)，所以，元素的位置要么是在原位置，
要么是在原位置再移动2次幂的位置。
例如我们从16扩展为32时，具体的变化如下所示
n-1=15                                           (n-1)&hash
n-1   0000 0000 0000 0000 0000 0000 0000 1111
hash1 1111 1111 1111 1111 0000 1111 0000 0101     0 0101
hash2 1111 1111 1111 1111 0000 1111 0001 0101     0 0101
n-1=31
n-1   0000 0000 0000 0000 0000 0000 0001 1111
hash1 1111 1111 1111 1111 0000 1111 0000 0101     0 0101
hash2 1111 1111 1111 1111 0000 1111 0001 0101     1 0101
根据位运算扩容后倒数第5位，只有0、1两种情况
00101=5还是原位置   10101=21=5+16=原位置+原长度
因此扩容的时候不需要重新计算hash来确定在数组中的位置，只要看倒数第5位是0或1    第5位=hash&n   第四位=hash&(n-1)  
n=oldCap=16                                       hash&oldCap
n     0000 0000 0000 0000 0000 0000 0001 0000
hash1 1111 1111 1111 1111 0000 1111 0000 0101     0 0000
hash2 1111 1111 1111 1111 0000 1111 0001 0101     1 0000

所以hash&oldCap=0 代表在原位置，hash&oldCap!=1代表在原位置+原长度    这里面node的hash是计算key得来的hash(Object key)

这个设计确实非常的巧妙，既省去了重新计算hash值的时间，而且同时，由于新增的1bit是0还是1可以认为是随机的，因此resize的过程，
均匀的把之前的冲突的节点分散到新的bucket了。  自己：扩容后，之前的哈希冲突重新均匀分布在数组中，哈希冲突是因为数组的空间是有限的
扩容后用于哈希冲突的链表中,低位的节点保留在原位置,高位的节点分散在原位置+原长度,如果有多个高位仍然是一个链表
Java_HashMap_扩容_链表部分处理.png
//同一table[i]的hash可能是不同的  index=(n-1)&hash 
//例如数组长度为16   hash为5或21    15&5=5 15&21=5 这两个都落在index=5的位置  后续把多个e.hash & oldCap为0的连接在一起
//  不为0的串在另一条链上

为什么要这么干? 主要是计算简单,不需要像JDK 1.7那样还需要重新计算hash.还有就是让元素更分散.本来原来是一条链上的,
现在在2条链上(不同的数组索引处)了,查找更快了


//TODO 红黑树部分的处理   也是把节点分散在原位置或者原位置+原长度
https://blog.csdn.net/mameng1988/article/details/107718284


tableSizeFor的计算过程 https://www.jianshu.com/p/6fe1fa38ac72
首先把函数分为3部分
```
static final int tableSizeFor(int cap) {
    //part1
    int n = cap - 1;
    //part2
    n |= n >>> 1;
    n |= n >>> 2;
    n |= n >>> 4;
    n |= n >>> 8;
    n |= n >>> 16;
    //part3
    return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
}
```
先看part2
假设一个整型的二进制为1xxxxxxx，1前面全是0，位操作结果如下：

操作	         原始值	            结果值
n >>> 1	1xxxxxxx	                01xxxxxx
位或操作	1xxxxxxx | 01xxxxxx       	11xxxxxx
n >>> 2	11xxxxxx	                0011xxxx
位或操作	11xxxxxx | 0011xxxx         1111xxxx
n >>> 4	1111xxxx	                00001111
位或操作	1111xxxx | 00001111	        11111111=255
n >>> 8	11111111	                00000000
位或操作	11111111 | 00000000         11111111
n >>> 16	11111111	            00000000
位或操作	11111111 | 00000000	        11111111
因为Java中一个整型占32位，这样最大无符号向右移动16位之后可以保证，输入的整型二进制数第一个1之后的所有为均为1。
//一共右移了31位，结果为n个1，只要+1，就可以是2的n次方

part1
```
int n = cap - 1;
```
移位操作可以保证输入的整型第一个1之后的位都是1，即产生与当前输入整型相同位数的最大值。接下来结果加1就是所求的结果。
但是有一个例外，假如输入的整型本身就是2的幂次结果，经过移位操作后就会产生比其大的2的幂次结果，为了避免这种情况对输入的整型减1。
减1之后对于非2的幂次的整型数没有影响，而对于2的幂次的整型数，其位数会减少1位，这样最后结果加1就是它本身

part3
```
return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
```
n+1是求出最后的结果，而条件判断是要保证哈希表最大不要超过设置的最大容量MAXIMUM_CAPACITY


为什么要重写equals和hashCode
基于hash工作，不重写发生大量hash冲突，散列表退化为链表和红黑树


效率测试
这里来测试下不同的初始化大小和不同情况下的 hashCode 值对 HashMap 运行效率的影响
首先来定义作为键值对 key 的类，hashCode() 方法直接返回其 value 属性
```
public class Key {

    private int value;

    public Key(int value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Key key = (Key) o;
        return value == key.value;
    }

    @Override
    public int hashCode() {
        return value;
    }
}
```
初始化大小从 200 到 200000 之间以 10 倍的倍数递增，向不同 HashMap 存入同等数据量的数据，观察存入数据所需要的时间
```
public class Test {
    private static final int MAX_KEY = 200000;

    private static final Key[] KEYS = new Key[MAX_KEY];

    static {
        for (int i = 0; i < MAX_KEY; i++) {
            KEYS[i] = new Key(i);
        }
    }

    private static void test(int size) {
        long startTime = System.currentTimeMillis();
        Map<Key, Integer> map = new HashMap<>(size);
        for (int i = 0; i < MAX_KEY; i++) {
            map.put(KEYS[i], i);
        }
        long endTime = System.currentTimeMillis();
        System.out.println("初始化大小是：" + size + "，用时：" + (endTime - startTime) + "毫秒");
    }

    public static void main(String[] args) {
        for (int i = 20; i <= MAX_KEY; i *= 10) {
            test(i);
        }
    }

}
```
在上述例子中，各个 Key 对象之间的哈希值各不相同，所以键值对在哈希桶数组中的分布可以说是很均匀的了，此时主要影响性能的就是扩容机制了，
 由日志可以看出此时不同的初始化大小对 HashMap 的性能影响还不大
结果：   cpu： AMD Ryzen 7 5700U with Radeon Graphics
数据量大的时候，初始化性能影响还挺大的。。
```
初始化大小是：20，用时：831毫秒
初始化大小是：200，用时：634毫秒
初始化大小是：2000，用时：108毫秒
初始化大小是：20000，用时：89毫秒
初始化大小是：200000，用时：123毫秒
初始化大小是：2000000，用时：33毫秒
```
如果让 Key 类的 hashCode() 方法固定返回 100，那么每个 Key 对象在存在 HashMap 时肯定都会发生哈希冲突
```
    @Override
    public int hashCode() {
        return 100;
    }
```
可以看到此时存入同等数据量的数据所需要的时间就呈几何数增长了，说明如果存在大量哈希冲突的话对 HashMap 的影响还是很大的
200000和2000000没有测，耗时太长了
```
初始化大小是：20，用时：2632毫秒
初始化大小是：200，用时：2598毫秒
初始化大小是：2000，用时：2578毫秒
初始化大小是：20000，用时：2572毫秒
```


迭代遍历  迭代器的nextNode()  先遍历node.next，没有next然后table[index++]也就是table的下一个   所以遍历顺序与插入顺序不一致
```
 final class EntrySet extends AbstractSet<Map.Entry<K,V>> {
        public final int size()                 { return size; }
        public final void clear()               { HashMap.this.clear(); }
        public final Iterator<Map.Entry<K,V>> iterator() {
            return new EntryIterator();
        }
 }
abstract class HashIterator {
        Node<K,V> next;        // next entry to return
        Node<K,V> current;     // current entry
        int expectedModCount;  // for fast-fail
        int index;             // current slot

        HashIterator() {
            expectedModCount = modCount;
            Node<K,V>[] t = table;
            current = next = null;
            index = 0;
            if (t != null && size > 0) { // advance to first entry
                do {} while (index < t.length && (next = t[index++]) == null);
            }
        }

        public final boolean hasNext() {
            return next != null;
        }

        final Node<K,V> nextNode() {
            Node<K,V>[] t;
            Node<K,V> e = next;
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            if (e == null)
                throw new NoSuchElementException();
            if ((next = (current = e).next) == null && (t = table) != null) {
                do {} while (index < t.length && (next = t[index++]) == null);
            }
            return e;
        }

        public final void remove() {
            Node<K,V> p = current;
            if (p == null)
                throw new IllegalStateException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            current = null;
            K key = p.key;
            removeNode(hash(key), key, null, false, false);
            expectedModCount = modCount;
        }
    }
    
 final class EntryIterator extends HashIterator
        implements Iterator<Map.Entry<K,V>> {
        public final Map.Entry<K,V> next() { return nextNode(); }
    }    
```



