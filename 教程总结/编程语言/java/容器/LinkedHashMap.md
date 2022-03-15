jdk 1.8   android sdk 30
https://juejin.cn/post/6931007634028003341#heading-12

HashMap 并不保证元素的存储顺序和迭代顺序能够和存入顺序保持一致，即 HashMap 本身是无序的。为了解决这一个问题，
 Java 提供了 LinkedHashMap 来实现有序的 HashMap

类声明
LinkedHashMap 是 HashMap 的子类，它保留了元素的插入顺序，其内部维护着一个按照元素插入顺序或者元素访问顺序来排列的链表，
  默认是按照元素的插入顺序来排列，就像使用 ArrayList 一样；如果是按照元素的访问顺序来排列，那么每次访问元素后该元素将移至链表的尾部，
 可以靠此来实现 LRUCache 缓存算法
```
    public class LinkedHashMap<K,V> extends HashMap<K,V> 
        implements Map<K,V>
```


结点类
HashMap 中每个存入的键值对都会被包装为 Node 对象，LinkedHashMap 则是包装为 Entry 对象，看 newNode 方法就知道了。
Entry 类在 Node 类的基础上扩展了两个新的成员变量：before 和 after，这两个变量就是 LinkedHashMap 来实现有序访问的关键。
每当保存了新的键值对，Entry 就会通过这两个变量将其和之前的键值对串联起来，保存为链表的尾结点，从而保留了键值对的顺序信息
不管 Entry 在 HashMap 内部为了解决哈希冲突采用的是链表还是红黑树，这两个变量的指向都不受数据结构变化的影响。
从这也可以看出集合框架在设计时一个很巧妙的地方：LinkedHashMap 内部没有新建一个链表用来维护元素的插入顺序，而是通过扩展父类来实现扩展功能
通过对数组/链表/红黑树中node新增before,after，新维护了一条链表，实现保持插入顺序
```
    static class Entry<K,V> extends HashMap.Node<K,V> {
        //用于指定上一个结点 before 和下一个结点 after
        Entry<K,V> before, after;
        Entry(int hash, K key, V value, Node<K,V> next) {
            super(hash, key, value, next);
        }
    }

    Node<K,V> newNode(int hash, K key, V value, Node<K,V> e) {
        LinkedHashMap.Entry<K,V> p = new LinkedHashMap.Entry<K,V>(hash, key, value, e);
        linkNodeLast(p);
        return p;
    }

    transient LinkedHashMap.Entry<K,V> head;

    transient LinkedHashMap.Entry<K,V> tail;

    private void linkNodeLast(LinkedHashMap.Entry<K,V> p) {
        LinkedHashMap.Entry<K,V> last = tail;
        tail = p;
        if (last == null)
            head = p;
        else {
            p.before = last;
            last.after = p;
        }
    }
```

变量
变量 accessOrder 用于决定 LinkedHashMap 中元素的排序方式，如果为 true 就按照元素访问顺序来排序，
  为 false 就按照元素插入顺序来排序，构造器中默认为false
```
    //序列化ID
    private static final long serialVersionUID = 3801124242820219131L;

    //指向双向链表的头结点
    transient LinkedHashMap.Entry<K,V> head;

    //指向最新访问的结点
    transient LinkedHashMap.Entry<K,V> tail;
    //排序方式
    final boolean accessOrder;
```

构造函数
默认情况下 LinkedHashMap 都是按照元素插入顺序来排序   accessOrder为true按照访问顺序排序
```
    public LinkedHashMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
        accessOrder = false;
    }

    public LinkedHashMap(int initialCapacity) {
        super(initialCapacity);
        accessOrder = false;
    }

    public LinkedHashMap() {
        super();
        accessOrder = false;
    }

    public LinkedHashMap(Map<? extends K, ? extends V> m) {
        super();
        accessOrder = false;
        putMapEntries(m, false);
    }

    public LinkedHashMap(int initialCapacity, float loadFactor, boolean accessOrder) {
        super(initialCapacity, loadFactor);
        this.accessOrder = accessOrder;
    }
```

预留的方法
在 HashMap 中有三个预留的空方法，源码注释中也写明这三个函数就是为 LinkedHashMap 预留的
```
    // Callbacks to allow LinkedHashMap post-actions
    void afterNodeAccess(Node<K,V> p) { }
    void afterNodeInsertion(boolean evict) { }
    void afterNodeRemoval(Node<K,V> p) { }
```
accessOrder在构造器中置为true，将保留访问顺序
当 HashMap 中的某个结点被访问了（例如调用了 get 方法）且 accessOrder 为 true，那么afterNodeAccess 方法就会被调用，
 该方法用于将最新访问的键值对移至链表的尾部，由于链表内结点位置的改变仅仅是修改几个引用即可，所以这个操作还是非常轻量级的
LinkedHashMap
```
    //重写父类的get方法
    public V get(Object key) {
        Node<K,V> e;
        if ((e = getNode(hash(key), key)) == null)
            return null;
        if (accessOrder)
            afterNodeAccess(e);
        return e.value;
    }

    //当访问了结点 e 时调用
    //结点 e 是最新访问的一个结点，此时就将结点 e 置为链表的尾结点
    void afterNodeAccess(Node<K,V> e) {
        //last 用来指向链表的尾结点
        LinkedHashMap.Entry<K,V> last;
        //只有当 last 和 e 不相等时才需要进行下一步，如果相等说明 e 已经在链表尾部了
        if (accessOrder && (last = tail) != e) {
            LinkedHashMap.Entry<K,V> p = (LinkedHashMap.Entry<K,V>)e, b = p.before, a = p.after;
            //因为结点 p 将成为尾结点，所以 after 置为null
            p.after = null;
            //如果 b == null ，说明结点 p 此时是链表的头结点，那 a 就会成为新的头结点
            //如果 b != null ，则移除结点 b 对结点 p 的引用并和 a 串联起来
            if (b == null)
                head = a;
            else
                b.after = a;
            //如果 a != null，说明结点 p 此时不是链表的尾结点，则移除结点 a 对结点 p 的引用并和 b 串联起来
            //如果 a == null，则说明结点 p 此时是链表的尾结点，那 a 就会成为新的尾结点
            if (a != null)
                a.before = b;
            else
                last = b;
            //如果 last == null，说明原链表为空，则此时头结点就是结点 p
            //如果 last != null，则 p 就会成为新的尾结点
            if (last == null)
                head = p;
            else {
                p.before = last;
                last.after = p;
            }
            //最新一个引用到的结点就是 tail
            tail = p;
            ++modCount;
        }
    }
```

当 put 方法被调用时afterNodeInsertion 方法也会被调用，该方法用于判断是否移除最近最少使用的元素，依此可以来构建 LRUcache 缓存
```
    //HashMap
    final V putVal(int hash, K key, V value, boolean onlyIfAbsent, boolean evict) {
      ...
      afterNodeInsertion(evict);
     return null;
    }
    //在插入元素后调用，此方法可用于 LRUcache 算法中移除最近最少使用的元素
    void afterNodeInsertion(boolean evict) {
        LinkedHashMap.Entry<K,V> first;
        if (evict && (first = head) != null && removeEldestEntry(first)) {
            K key = first.key;
            removeNode(hash(key), key, null, false, true);
        }
    }

    //此方法就用于决定是否移除最旧的缓存，默认返回 false
	//可以通过重写该方法来实现按照特定规则移除旧数据
    protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
        return false;
    }
```
当 HashMap 内部移除了某个结点时，LinkedHashMap 也要通过 afterNodeRemoval 方法将对该结点的引用从维护的链表中移除
```
    //HashMap
    final Node<K, V> removeNode(int hash, Object key, Object value,
                                boolean matchValue, boolean movable) {
                ...
                afterNodeRemoval(node);
                ...                
                               }
    //在移除结点 e 后调用
    void afterNodeRemoval(Node<K,V> e) {
        LinkedHashMap.Entry<K,V> p = (LinkedHashMap.Entry<K,V>)e, b = p.before, a = p.after;
        //移除结点 p 对相邻结点的引用
        p.before = p.after = null;
        //如果 b == null，说明结点 p 是链表的头结点，则 a 将成为新的头结点
        //如果 b != null，则更新结点间的引用
        if (b == null)
            head = a;
        else
            b.after = a;
        //如果 a == null，说明结点 a 是尾结点，则移除结点 p 后最新一个访问的结点就是原倒数第二的结点
        //如果 a != null，则更新结点间的引用
        if (a == null)
            tail = b;
        else
            a.before = b;
    }
```

迭代遍历  关键nextNode()中next=current.after
```
abstract class LinkedHashIterator {
        LinkedHashMapEntry<K,V> next;
        LinkedHashMapEntry<K,V> current;
        int expectedModCount;

        LinkedHashIterator() {
            next = head;
            expectedModCount = modCount;
            current = null;
        }

        public final boolean hasNext() {
            return next != null;
        }

        final LinkedHashMapEntry<K,V> nextNode() {
            LinkedHashMapEntry<K,V> e = next;
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            if (e == null)
                throw new NoSuchElementException();
            current = e;
            next = e.after;
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
static class LinkedHashMapEntry<K,V> extends HashMap.Node<K,V> {
        LinkedHashMapEntry<K,V> before, after;
        LinkedHashMapEntry(int hash, K key, V value, Node<K,V> next) {
            super(hash, key, value, next);
        }
    }    
    
final class LinkedKeyIterator extends LinkedHashIterator
        implements Iterator<K> {
        public final K next() { return nextNode().getKey(); }
    }

final class LinkedValueIterator extends LinkedHashIterator
    implements Iterator<V> {
    public final V next() { return nextNode().value; }
}

final class LinkedEntryIterator extends LinkedHashIterator
    implements Iterator<Map.Entry<K,V>> {
    public final Map.Entry<K,V> next() { return nextNode(); }
}    
final class LinkedEntrySet extends AbstractSet<Map.Entry<K,V>> {
    public final int size()                 { return size; }
    public final void clear()               { LinkedHashMap.this.clear(); }
    public final Iterator<Map.Entry<K,V>> iterator() {
        return new LinkedEntryIterator();
    }
```

LRUCache
在 Android 端的应用开发中，LRUCache 算法（最近最少使用算法）是很常见的，一种典型的用途就是用来在内存中缓存 Bitmap，
因为从 IO 流中读取 Bitmap 的资源消耗较大，为了防止多次从磁盘中读取某张图片，所以通常会在内存中 Bitmap。
但内存空间也是有限的，所以也不能每张图片都进行缓存，需要有选择性地缓存一定数量的图片，LRUCache 就是最常见的缓存方案之一
这里利用 LinkedHashMap 可以按照元素使用顺序进行排列的特点，来实现一个 LRUCache 策略的缓存
```
 public class LRUCache {

    private static class LRUCacheMap<K, V> extends LinkedHashMap<K, V> {

        //最大的缓存数量
        private final int maxCacheSize;

        public LRUCacheMap(int maxCacheSize) {
            super(16, 0.75F, true);
            this.maxCacheSize = maxCacheSize;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > maxCacheSize;
            //也可以写成 return size() > capcity;
        }

    }

    public static void main(String[] args) {
        //最大缓存数量是 5
        LRUCacheMap<String, Integer> map = new LRUCacheMap<>(5);
        map.put("Java", 1);
        map.put("Jetpack", 2);
        map.put("Kotlin", 3);
        map.put("业志陈", 4);
        map.put("字节数组", 5);
        map.put("leaveC", 6);

        System.out.println();
        Set<String> keySet = map.keySet();
        //输出结果是：Jetpack Kotlin 业志陈 字节数组 leaveC
        keySet.forEach(key -> System.out.print(key + " "));

        //获取链表的头结点的值，使之移动到链表尾部
        map.get("Jetpack");
        System.out.println();
        keySet = map.keySet();
        //输出结果是：Kotlin 业志陈 字节数组 leaveC Jetpack
        keySet.forEach(key -> System.out.print(key + " "));

        //向链表添加元素
        map.put("Dart", 5);
        System.out.println();
        //输出结果是：业志陈 字节数组 leaveC Jetpack Dart
        keySet.forEach(key -> System.out.print(key + " "));
    }

}
```