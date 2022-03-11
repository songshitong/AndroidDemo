jdk1.8


已经不推荐使用了，根据注释，如果需要线程安全请使用ConcurrentHashMap，如果不需要请使用HashMap
HashTable的定位就很尴尬。。。  所以这个类就简单看一下

HashTable的数据结构是table[]+链表
线程安全靠对公开方法加锁synchronized实现，同一时刻只有一个现场能访问HashTable对象，效率不高

HashTable实现了Map接口，是一个线程安全的map
```
//Dictionary是一个空的抽象类   
public class Hashtable<K,V>
    extends Dictionary<K,V>
    implements Map<K,V>, Cloneable, java.io.Serializable {
  //存储数组  
  private transient Entry<?,?>[] table;  
     
    }
```

Entry的结构
```
  private static class Entry<K,V> implements Map.Entry<K,V> {
        final int hash;
        final K key;
        V value;
        Entry<K,V> next;
  }
```

初始化  默认容量11,扩容阈值11*0.75=8.25
```
public Hashtable() {
        this(11, 0.75f);
    }
  public Hashtable(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal Capacity: "+
                                               initialCapacity);
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("Illegal Load: "+loadFactor);

        if (initialCapacity==0)
            initialCapacity = 1;
        this.loadFactor = loadFactor;
        //直接创建数组
        table = new Entry<?,?>[initialCapacity];
        //MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8
        threshold = (int)Math.min(initialCapacity * loadFactor, MAX_ARRAY_SIZE + 1);
    }    
```

put方法
```
    //synchronized锁住方法
    public synchronized V put(K key, V value) {
        // Make sure the value is not null
        if (value == null) {
            throw new NullPointerException();
        }

        // Makes sure the key is not already in the hashtable.
        Entry<?,?> tab[] = table;
        int hash = key.hashCode();
        //index计算
        int index = (hash & 0x7FFFFFFF) % tab.length;
        @SuppressWarnings("unchecked")
        Entry<K,V> entry = (Entry<K,V>)tab[index];
        for(; entry != null ; entry = entry.next) {
            //存在链表   hash相同，key相等
            if ((entry.hash == hash) && entry.key.equals(key)) {
                //替换对应的value
                V old = entry.value;
                entry.value = value;
                return old;
            }
        }
        //没有找到old value，添加新的entry
        addEntry(hash, key, value, index);
        return null;
    }
 
private void addEntry(int hash, K key, V value, int index) {
        modCount++;

        Entry<?,?> tab[] = table;
        if (count >= threshold) {
            // Rehash the table if the threshold is exceeded
            rehash();

            tab = table;
            //重新计算hash与index
            hash = key.hashCode();
            index = (hash & 0x7FFFFFFF) % tab.length;
        }
         
        // Creates the new entry.
        @SuppressWarnings("unchecked")
        Entry<K,V> e = (Entry<K,V>) tab[index];
        //创建一个新的entry作为链表的头节点，原来值作为head.next
        tab[index] = new Entry<>(hash, key, value, e);
        count++;
    }  
    
 
 protected void rehash() {
        int oldCapacity = table.length;
        Entry<?,?>[] oldMap = table;

        // overflow-conscious code  
        //扩容为2倍+1 不超过MAX_ARRAY_SIZE
        int newCapacity = (oldCapacity << 1) + 1;
        if (newCapacity - MAX_ARRAY_SIZE > 0) {
            if (oldCapacity == MAX_ARRAY_SIZE)
                // Keep running with MAX_ARRAY_SIZE buckets
                return;
            newCapacity = MAX_ARRAY_SIZE;
        }
        //创建新的数组
        Entry<?,?>[] newMap = new Entry<?,?>[newCapacity];

        modCount++;
        //新的阈值
        threshold = (int)Math.min(newCapacity * loadFactor, MAX_ARRAY_SIZE + 1);
        table = newMap;
        
        //内容拷贝
        for (int i = oldCapacity ; i-- > 0 ;) {
            for (Entry<K,V> old = (Entry<K,V>)oldMap[i] ; old != null ; ) {
                Entry<K,V> e = old;
                old = old.next;

                int index = (e.hash & 0x7FFFFFFF) % newCapacity;
                e.next = (Entry<K,V>)newMap[index];
                newMap[index] = e;
            }
        }
    }     
```


看一个get方法
```
  public synchronized V get(Object key) {
        Entry<?,?> tab[] = table;
        //根据hash计算index
        int hash = key.hashCode();
        int index = (hash & 0x7FFFFFFF) % tab.length;
        //遍历链表
        for (Entry<?,?> e = tab[index] ; e != null ; e = e.next) {
            if ((e.hash == hash) && e.key.equals(key)) {
                return (V)e.value;
            }
        }
        return null;
    }
```


remove方法
```
public synchronized boolean remove(Object key, Object value) {
        Objects.requireNonNull(value);

        Entry<?,?> tab[] = table;
        //根据hash计算index
        int hash = key.hashCode();
        int index = (hash & 0x7FFFFFFF) % tab.length;
        @SuppressWarnings("unchecked")
        Entry<K,V> e = (Entry<K,V>)tab[index];
        //遍历链表
        for (Entry<K,V> prev = null; e != null; prev = e, e = e.next) {
            //hash相等  key相等 value相等  
            if ((e.hash == hash) && e.key.equals(key) && e.value.equals(value)) {
                modCount++;
                if (prev != null) {
                    //从链表移除
                    prev.next = e.next;
                } else {
                    //要移除的是头节点
                    tab[index] = e.next;
                }
                count--;
                e.value = null;
                return true;
            }
        }
        return false;
    }
```