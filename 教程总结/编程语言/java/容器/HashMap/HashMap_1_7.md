https://github.com/xfhy/Android-Notes/blob/master/Blogs/Java/%E9%9B%86%E5%90%88/HashMap%E6%BA%90%E7%A0%81%E8%A7%A3%E6%9E%90.md
http://hg.openjdk.java.net/jdk7u/jdk7u60/jdk/file/33c1eee28403/src/share/classes/java/util/HashMap.java

JDK 1.8 的优化目的主要是：减少 Hash冲突 & 提高哈希表的存、取效率
Java1.7与1.8 HashMap对比
1 JDK1.7用的是头插法,JDK1.8及置换是尾插法. 且1.7插入时候顺序是与原来相反的,而1.8则还是原来的顺序
2 JDK1.7是数组+链表,JDK1.8是数组+链表+红黑树  Java8中链表超过8且数组长度大于64则会将链表树化
3 JDK1.7在插入数据之前进行扩容,JDK1.8是插入数据之后才扩容
4 JDK1.7是Entry来表示节点,而JDK1.8是Node
5 JDK1.7扩容后存储位置是用hash & (length-1)计算来的,中间可能存在重新计算hash的过程rehash,如果存在hash冲突，继续使用链表法解决
   而JDK1.8只需要判断hash值新增参与运算的位是0还是1就能快速计算出扩容后该放在原位置,还是需要放在 原位置+扩容的大小值 .
   元素移动只需要移动两个节点，原位置链表头，新位置链表头
6 计算hash值的时候,JDK1.7用了9次扰动处理(4次位运算，5次异或),而JDK1.8是2次(一次位运算，一次异或)
7 Java7将key为null的单独处理,Java8没有单独处理(虽然它们的hash都是0,都是放数组第0处)



1.7的HashMap底层数据结构是数组+链表
```
transient Entry<K,V>[] table;
static class Entry<K,V> implements Map.Entry<K,V> {
        final K key;
        V value;
        Entry<K,V> next;
        int hash;
  //构造器传入n为当前节点的next      
  Entry(int h, K k, V v, Entry<K,V> n) {
            value = v;
            next = n;
            key = k;
            hash = h;
        }      
}
```
负载因子和阈值
```
static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // aka 16
static final float DEFAULT_LOAD_FACTOR = 0.75f;
```

put方法
1.数组为空，进行初始化
2.key为null，放在table[0]及其链表
3.计算hash及index
4.如果table[index]及其链表存在相同的key，进行value替换
5.阈值检查，超过0.75进行扩容，扩容为原来的2倍
6. 使用头插法，将新建的节点(key,value)放在table[index]
```
  public V put(K key, V value) {
         //1. 数组为空 -> 初始化(创建)数组
        if (table == EMPTY_TABLE) {
            inflateTable(threshold);
        }
        //2. key为null,单独处理
        if (key == null)
            return putForNullKey(value);
        //3. 计算hash值    
        int hash = hash(key);
        //4. 计算该hash值该存放在数组的哪个索引处
        int i = indexFor(hash, table.length);
        //5. 遍历链表(数组的每个元素都是单链表的表头)  查找链表中是否已存在相同的key  如果有,则替换掉  
        for (Entry<K,V> e = table[i]; e != null; e = e.next) {
            Object k;
            if (e.hash == hash && ((k = e.key) == key || key.equals(k))) {
                V oldValue = e.value;
                e.value = value;
                e.recordAccess(this);
                return oldValue;
            }
        }

        modCount++;
        //6. 添加元素到数组中
        addEntry(hash, key, value, i);
        return null;
    }
```

inflateTable 数组初始化
```
private void inflateTable(int toSize) {
    // Find a power of 2 >= toSize
    int capacity = roundUpToPowerOf2(toSize);
     //更新阈值
    threshold = (int) Math.min(capacity * loadFactor, MAXIMUM_CAPACITY + 1);
    table = new Entry[capacity];
    initHashSeedAsNeeded(capacity);
}

private static int roundUpToPowerOf2(int number) {
    // assert number >= 0 : "number must be non-negative";
    return number >= MAXIMUM_CAPACITY
            ? MAXIMUM_CAPACITY
            : (number > 1) ? Integer.highestOneBit((number - 1) << 1) : 1;
}

//Integer.highestOneBit
public static int highestOneBit(int var0) {
    //求掩码
    var0 |= var0 >> 1;
    var0 |= var0 >> 2;
    var0 |= var0 >> 4;
    var0 |= var0 >> 8;
    var0 |= var0 >> 16; 
    
    //>>>：无符号右移。无论是正数还是负数，高位通通补0.  这里减了之后只剩下最高位为1
    return var0 - (var0 >>> 1);
}

  final boolean initHashSeedAsNeeded(int capacity) {
        boolean currentAltHashing = hashSeed != 0;
        boolean useAltHashing = sun.misc.VM.isBooted() &&
                (capacity >= Holder.ALTERNATIVE_HASHING_THRESHOLD);
        boolean switching = currentAltHashing ^ useAltHashing;
        //hashSeed默认为0  
        if (switching) {
            hashSeed = useAltHashing
                ? sun.misc.Hashing.randomHashSeed(this)
                : 0;
        }
        return switching;
    }
```
roundUpToPowerOf2方法是为了求一个比number大一点的2的幂次方的数,这里的代码看起来有点迷.它最后会求出数组应该初始化的长度,
  它可以自动将传入的容量转换为2的n次方.
Integer.highestOneBit是取传入的这个数的二进制形式最左边的最高一位且高位后面全部补零,最后返回int类型的结果.比如传入的是7(0111),
 则最后得到的是4(0100).它这里先将number-1,然后再左移一位,比如number是9,则number-1等于8(1000),左移一位等于10000就是16.
  这样最后它就将传入的容量转换为了2的n次方.
计算好了容量之后,计算阈值,然后初始化数组.


putForNullKey 添加null key
用了一个专门的方法用来操作key为null的情况
```
private V putForNullKey(V value) {
    for (Entry<K,V> e = table[0]; e != null; e = e.next) {
        if (e.key == null) {
            V oldValue = e.value;
            e.value = value;
            e.recordAccess(this);
            return oldValue;
        }
    }
    modCount++;
    addEntry(0, null, value, 0);
    return null;
}
```
1 如果已经存在key为null进行value替换
2 否则将元素存放到了数组的第一个位置.第一个位置也是一个桶,这桶里面只有一个元素的key可以是null,其他元素都是被hash算法分配到这里来的.



计算hash值
```
final int hash(Object k) {
        int h = hashSeed;
        if (0 != h && k instanceof String) {
            return sun.misc.Hashing.stringHash32((String) k);
        }

        h ^= k.hashCode();

        // This function ensures that hashCodes that differ only by
        // constant multiples at each bit position have a bounded
        // number of collisions (approximately 8 at default load factor).
        h ^= (h >>> 20) ^ (h >>> 12);
        return h ^ (h >>> 7) ^ (h >>> 4);
    }
```
做了9次扰动处理

indexFor 计算元素在数组中的索引
```
static int indexFor(int h, int length) {
    return h & (length-1);
}
```

addEntry 添加元素到数组中
```
void addEntry(int hash, K key, V value, int bucketIndex) {
    //1. 键值对数量超过阈值 && 该索引处数组不为空(说明这里之前已经存在元素)
    if ((size >= threshold) && (null != table[bucketIndex])) {
        //扩容->原来的2倍
        resize(2 * table.length);
        hash = (null != key) ? hash(key) : 0;
        //计算新的位置
        bucketIndex = indexFor(hash, table.length);
    }

    //2. 创建Entry节点
    createEntry(hash, key, value, bucketIndex);
}
//创建新的节点  
void createEntry(int hash, K key, V value, int bucketIndex) {
    //table[bucketIndex] 是放到新插入节点的后面,,所以这里是头插法
    //原来的节点
    Entry<K,V> e = table[bucketIndex];
    //更新为新的节点，next为原来的节点   
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

扩容操作
```
void resize(int newCapacity) {
    Entry[] oldTable = table;
    int oldCapacity = oldTable.length;
    //达到最大阈值了
    if (oldCapacity == MAXIMUM_CAPACITY) {
        threshold = Integer.MAX_VALUE;
        return;
    }

    //根据新的容量创建数组
    Entry[] newTable = new Entry[newCapacity];
    //转移数据到新数组   initHashSeedAsNeeded是否需要重新计算hash
    transfer(newTable, initHashSeedAsNeeded(newCapacity));
    table = newTable;
    //更新阈值
    threshold = (int)Math.min(newCapacity * loadFactor, MAXIMUM_CAPACITY + 1);
}

//转移数据到新数组
void transfer(Entry[] newTable, boolean rehash) {
    int newCapacity = newTable.length;
    for (Entry<K,V> e : table) {
        //元素非空 则转移
        while(null != e) {
            Entry<K,V> next = e.next;
            if (rehash) {
                e.hash = null == e.key ? 0 : hash(e.key);
            }
            //根据该节点hash值计算一下该节点该放到新数组的哪个索引处
            int i = indexFor(e.hash, newCapacity);
            //将桶内元素逐个转移到新的数组的新的索引处
            //注意: 这里桶内顺序会倒过来.
            //比如桶内是1->2->3   转移数据之后就是3->2->1
            e.next = newTable[i];
            newTable[i] = e;
            e = next;
        }
    }
}
```
迁移元素 假如元素是1->2->3
第一次  e.next=null;newTable[i]=1;e=2;
第二次  2.next=1;newTable[i]=2;e=3;
第三次  3.next=2;newTable[i]=3;e=null; 
扩容后元素为3->2->1



获取元素
1.如果key为null，从table[0]及其链表获取
2.根据key计算hash及index，从table[index]或其链表获取
```
 public V get(Object key) {
        if (key == null)
            //对于null的特殊处理，一般放在table[0]
            return getForNullKey();
        Entry<K,V> entry = getEntry(key);

        return null == entry ? null : entry.getValue();
    }

 private V getForNullKey() {
        if (size == 0) {
            return null;
        }
        for (Entry<K,V> e = table[0]; e != null; e = e.next) {
            if (e.key == null)
                return e.value;
        }
        return null;
    }    
    
 
 final Entry<K,V> getEntry(Object key) {
        if (size == 0) {
            return null;
        }
        int hash = (key == null) ? 0 : hash(key);
        //取得table[i]的元素，然后遍历链表
        for (Entry<K,V> e = table[indexFor(hash, table.length)];
             e != null;
             e = e.next) {
            Object k;
            if (e.hash == hash &&
                ((k = e.key) == key || (key != null && key.equals(k))))
                return e;
        }
        return null;
    }   
```
