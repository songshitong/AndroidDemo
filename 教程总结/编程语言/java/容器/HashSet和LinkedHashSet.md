https://juejin.cn/post/6931007634028003341#heading-19

总结
HashSet
HashSet 实现了 Set 接口，不允许插入重复的元素，允许包含 null 元素，且不保证元素的迭代顺序
HashSet内部通过HashMap实现元素不重复，HashMap的key是不能重复的，向 HashSet 添加的值都会被包装为一个键值对保存到 HashMap 中，
key 即外部传入的值，value 则由 HashSet 来提供一个常量，当添加重复元素时，key与value会覆盖旧值
```
private static final Object PRESENT = new Object();
```

LinkedHashSet
LinkedHashSet是HashSet的子类，构造器执行父类HashSet的逻辑，此时的Map是LinkedHashMap  即实现了元素不可以重复，有记录了添加顺序


HashSet 实现了 Set 接口，不允许插入重复的元素，允许包含 null 元素，且不保证元素的迭代顺序，源码十分简单，去掉注释后不到两百行，
因为其底层也是通过 HashMap 来实现的，看了上面关于 HashMap 源码的解析后再来看 HashSet 就会有一种“不过如此”的感觉了
我们知道，当向 HashMap 中插入一个存在相同 key 的键值对时，HashMap 中旧 key 不会被改动到，但旧 value 可能会被新 value 所覆盖，
HashSet 就依靠这个特性来实现自身的不可重复性。HashSet 中包含一个 HashMap，向 HashSet 添加的值都会被包装为一个键值对保存到 HashMap 中，
key 即外部传入的值，value 则由 HashSet 来提供，当 key 不重复时则正常保存，当 key 重复时则也只会改动到 value，
从而实现了 HashSet 元素不重复的特性

HashMap中key是唯一的，key相同时，value会取代,HashSet只用了HashMap的key,value是自己的，保证了元素的不重复
```
public class HashSet<E> extends AbstractSet<E> implements Set<E>, Cloneable, java.io.Serializable{

    static final long serialVersionUID = -5024744406713321676L;

    //HashSet 底层用 HashMap 来存放数据
    //Key 值由外部传入，Value 则由 HashSet 内部来维护
    private transient HashMap<E,Object> map;

    //HashMap 中所有键值对都共享同一个值
    //即所有存入 HashMap 的键值对都是使用这个对象作为值
    //Dummy value to associate with an Object in the backing Map
    private static final Object PRESENT = new Object();

    public HashSet() {
        map = new HashMap<>();
    }

    //使用默认的装载因子，并以此来计算 HashMap 的初始化大小
    //+1 是为了弥补精度损失
    public HashSet(Collection<? extends E> c) {
        map = new HashMap<>(Math.max((int) (c.size()/.75f) + 1, 16));
        addAll(c);
    }

    public HashSet(int initialCapacity, float loadFactor) {
        map = new HashMap<>(initialCapacity, loadFactor);
    }

    public HashSet(int initialCapacity) {
        map = new HashMap<>(initialCapacity);
    }

    //此构造函数为包访问权限，只用于支持 LinkedHashSet
    HashSet(int initialCapacity, float loadFactor, boolean dummy) {
        map = new LinkedHashMap<>(initialCapacity, loadFactor);
    }

    //将对 HashSet 的迭代转换为对 HashMap 的 Key 值的迭代
    public Iterator<E> iterator() {
        return map.keySet().iterator();
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    //如果 HashMap 中不包含 key 是 e 的键值对，则添加该元素并返回 true
    //如果包含则只会覆盖 value 而不会影响 key，同时返回 false
    //从而实现 HashSet key 不重复的特性
    public boolean add(E e) {
        return map.put(e, PRESENT)==null;
    }

    public boolean remove(Object o) {
        return map.remove(o)==PRESENT;
    }

    public void clear() {
        map.clear();
    }
    
}
```



LinkedHashSet
LinkedHashSet 其内部源码十分简单，简单到只有几十行代码，从其名字就可以猜出它是 HashSet 的子类，并且是依靠链表来实现有序的 HashSet
HashSet 为 LinkedHashSet 预留了一个构造函数，其 dummy 参数并没有实际意义，只是为了和其它构造函数区分开。其它构造函数会将 map 变量
初始化为 HashMap 类型，特意预留的构造函数则是会初始化为 LinkedHashMap 类型变量，从而通过 LinkedHashMap 内部的双向链表
 来实现 LinkedHashSet 自身存取有序，元素唯一的特性

LinkedHashSet通过父类的构造，父类HashSet实现元素唯一，父类内部的LinkedHashMap实现保存插入的顺序
```
public class HashSet<E> extends AbstractSet<E> implements Set<E>, Cloneable, java.io.Serializable {

    private transient HashMap<E,Object> map;
    
    HashSet(int initialCapacity, float loadFactor, boolean dummy) {
        map = new LinkedHashMap<>(initialCapacity, loadFactor);
    }
    
}


public class LinkedHashSet<E> extends HashSet<E> implements Set<E>, Cloneable, java.io.Serializable {

    private static final long serialVersionUID = -2851667679971038690L;

    public LinkedHashSet(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor, true);
    }

    public LinkedHashSet(int initialCapacity) {
        super(initialCapacity, .75f, true);
    }

    //使用默认的初始容量以及装载因子
    public LinkedHashSet() {
        super(16, .75f, true);
    }

    public LinkedHashSet(Collection<? extends E> c) {
        super(Math.max(2*c.size(), 11), .75f, true);
        addAll(c);
    }
    
    @Override
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator(this, Spliterator.DISTINCT | Spliterator.ORDERED);
    }

}
```