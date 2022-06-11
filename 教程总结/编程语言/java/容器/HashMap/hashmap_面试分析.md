扩容因子为什么是0.75
https://mp.weixin.qq.com/s/X2J15ricC1QP191EJkZfvA
与泊松分布有关
链表变树的阈值也与泊松分布有关
https://zhuanlan.zhihu.com/p/358138626


多线程的安全问题
在并发的情况，发生扩容时，可能会产生循环链表，在执行get的时候，会触发死循环，引起CPU的100%问题，所以一定要避免在并发环境下使用HashMap

Java1.7分析 https://www.jianshu.com/p/1e9cf0ac07f4

总结：

1.7链表新节点采用的是头插法，这样在线程一扩容迁移元素时，会将元素顺序改变，导致两个线程中出现元素的相互指向而形成循环链表

1.8中扩容时保持了原来链表中的顺序



HashMap的put方法实现：

1、判断key是否已经存在
```
public V put(K key, V value) {
    if (key == null)
        return putForNullKey(value);
    int hash = hash(key);
    int i = indexFor(hash, table.length);
    // 如果key已经存在，则替换value，并返回旧值
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
    // key不存在，则插入新的元素
    addEntry(hash, key, value, i);
    return null;
}
```
2、检查容量是否达到阈值threshold
```
void addEntry(int hash, K key, V value, int bucketIndex) {
    if ((size >= threshold) && (null != table[bucketIndex])) {
        resize(2 * table.length);
        hash = (null != key) ? hash(key) : 0;
        bucketIndex = indexFor(hash, table.length);
    }

    createEntry(hash, key, value, bucketIndex);
}
```
如果元素个数已经达到阈值，则扩容，并把原来的元素移动过去

3、扩容实现
```
void resize(int newCapacity) {
    Entry[] oldTable = table;
    int oldCapacity = oldTable.length;
    ...

    Entry[] newTable = new Entry[newCapacity];
    ...
    transfer(newTable, rehash);
    table = newTable;
    threshold = (int)Math.min(newCapacity * loadFactor, MAXIMUM_CAPACITY + 1);
}
```
这里会新建一个更大的数组，并通过transfer方法，移动元素。
```
void transfer(Entry[] newTable, boolean rehash) {
    int newCapacity = newTable.length;
    for (Entry<K,V> e : table) {
        while(null != e) {
            Entry<K,V> next = e.next;
            if (rehash) {
                e.hash = null == e.key ? 0 : hash(e.key);
            }
            int i = indexFor(e.hash, newCapacity);
            e.next = newTable[i];
            newTable[i] = e;
            e = next;
        }
    }
}
```
移动的逻辑也很清晰，遍历原来table中每个位置的链表，并对每个元素进行重新hash，在新的newTable找到归宿，并插入。


案例分析
假设HashMap初始化大小为4，插入3个节点，不巧的是，这3个节点都hash到同一个位置，如果按照默认的负载因子(0.75)的话，插入第3个节点就会扩容，
为了验证效果，假设负载因子是1.
0
1
2
3 -> a -> b -> c

插入第4个节点时，发生rehash，假设现在有两个线程同时进行，线程1和线程2，两个线程都会新建新的数组
//两个线程都扩容2倍，数组index为0...7    

假设 线程2 在执行到Entry<K,V> next = e.next;之后，cpu时间片用完了，这时变量e指向节点a，变量next指向节点b。

线程1继续执行，很不巧，a、b、c节点rehash之后又是在同一个位置7，开始移动节点

第一步，移动节点a
0
...
7 -> a

第二步，移动节点b
0
...
7 -> b ->a

注意，这里的顺序是反过来的，继续移动节点c
0
...
7 -> c ->b ->a

这个时候 线程1 的时间片用完，内部的table还没有设置成新的newTable， 线程2 开始执行，这时内部的引用关系如下：
java_HashMap_线程安全_循环链表1.webp

这时，在 线程2 中，变量e指向节点a，变量next指向节点b，开始执行循环体的剩余逻辑。
```
Entry<K,V> next = e.next;
int i = indexFor(e.hash, newCapacity);
e.next = newTable[i];
newTable[i] = e;
e = next;
```
执行之后的引用关系如下图
java_HashMap_线程安全_循环链表2.webp

执行后，变量e指向节点b，因为e不是null，则继续执行循环体，执行后的引用关系  //table[i]为b
java_HashMap_线程安全_循环链表3.webp

变量e又重新指回节点a，只能继续执行循环体，这里仔细分析下：
1、执行完Entry<K,V> next = e.next;，目前节点a没有next，所以变量next指向null； //e为节点a

2、e.next = newTable[i]; 其中 newTable[i] 指向节点b，那就是把a的next指向了节点b，这样a和b就相互引用了，形成了一个环；

3、newTable[i] = e 把节点a放到了数组i位置； //table[i]为a

4、e = next; 把变量e赋值为null，因为第一步中变量next就是指向null；

所以最终的引用关系是这样的：
java_HashMap_线程安全_循环链表4.webp

节点a和b互相引用，形成了一个环，当在数组该位置get寻找对应的key时，就发生了死循环。 //链表中出现了环

另外，如果线程2把newTable设置成到内部的table，节点c的数据就丢了，看来还有数据遗失的问题。 //b没有指向c，只有table[i]->a->b

总结
所以在并发的情况，发生扩容时，可能会产生循环链表，在执行get的时候，会触发死循环，引起CPU的100%问题，所以一定要避免在并发环境下使用HashMap。

曾经有人把这个问题报给了Sun，不过Sun不认为这是一个bug，因为在HashMap本来就不支持多线程使用，要并发就用ConcurrentHashmap。






