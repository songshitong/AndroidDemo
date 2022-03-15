jdk 1.8  android sdk 30(与Java open jdk部分代码不同)  
https://juejin.cn/post/6930632285301669895#heading-1


ArrayList总结
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
   

ArrayList 应该是大多数开发者使用得最为频繁的集合容器了，ArrayList 实现了 List 接口，是一个有序容器，即元素的存放顺序
与添加顺序保持一致，允许添加相同元素，包括 null 。ArrayList 底层通过数组来进行数据存储，当向 ArrayList 中添加元素时
如果发现数组空间不足，ArrayList 会自动对底层数组进行扩容并迁移现有数据

类声明
从 ArrayList 实现的接口可以看出来它是支持快速访问，可克隆，可序列化的
```
package java.util;
    public class ArrayList<E> extends AbstractList<E> 
        implements List<E>, RandomAccess, Cloneable, java.io.Serializable
```

成员变量
ArrayList 一共包含以下几个成员变量，主要看 elementData。elementData 是用于存放数据的底层数组，由于其数据类型声明为Object，
所以可以用来存放任何类型的数据。而 ArrayList 属于泛型类，如果我们在初始化时就指定了数据类型的话，
依靠 Java 泛型为我们提供的语法糖，我们在向 elementData 存取数据时编译器就会自动进行类型校验和类型转换，
确保存入和取出的数据类型是安全的
```
    //序列化ID
    private static final long serialVersionUID = 8683452581122892189L;
    //进行扩容操作后的最小容量
    private static final int DEFAULT_CAPACITY = 10;
    //如果外部为集合设置的初始化大小为 0，则将 elementData 指向此空数组
    private static final Object[] EMPTY_ELEMENTDATA = {};
    //如果在初始化集合时使用的是无参构造函数，则将 elementData 指向此空数组
    private static final Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = {};
    //用来存放元素的数组
    transient Object[] elementData;
    //集合大小
    private int size;
    //ArrayList 的快照版本号
    protected transient int modCount = 0;
```

构造函数
如果已经知道目标数据量大小的话，在初始化 ArrayList 的时候我们可以直接传入最终的容量值，这样效率会更高一些。
  因为如果 initialCapacity 过大，则会造成内存浪费；如果 initialCapacity 过小，可能会导致后续需要多次扩容，
  每次扩容都需要复制原有数据到新数组，这会降低运行效率
如果我们使用的是无参构造函数或者是指定的 initialCapacity 为 0，此时也只会将 elementData 指向空数组，
  并不会新建一个数组变量
```
    //指定集合的初始容量，以此来进行数组的初始化操作
    public ArrayList(int initialCapacity) {
        if (initialCapacity > 0) {
            this.elementData = new Object[initialCapacity];
        } else if (initialCapacity == 0) {
            this.elementData = EMPTY_ELEMENTDATA;
        } else {
            throw new IllegalArgumentException("Illegal Capacity: "+initialCapacity);
        }
    }

    //外部没有指定初始容量，暂且使用空数组
    public ArrayList() {
        this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
    }

    //传入一份初始数据来进行初始化
    public ArrayList(Collection<? extends E> c) {
        elementData = c.toArray();
        if ((size = elementData.length) != 0) {
            // c.toArray might (incorrectly) not return Object[] (see 6260652)
            if (elementData.getClass() != Object[].class)
                elementData = Arrays.copyOf(elementData, size, Object[].class);
        } else {
            this.elementData = EMPTY_ELEMENTDATA;
        }
    }

```


获取元素
在获取指定索引处的元素时，ArrayList 都是直接通过坐标值来获取元素，无需从头遍历，所以说 ArrayList 遍历和随机访问的效率较高
```
   E elementData(int index) {
        return (E) elementData[index];
    }
 public E get(int index) {
        //判断取值范围是否合法
        rangeCheck(index);
        return elementData(index);
    }
rangeCheck
//
//if (index > size || index < 0)
//            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));    
```


添加元素
ArrayList 添加元素的操作就不是那么理想了。如果是直接向集合尾端添加数据，那么直接定位到该位置进行赋值即可；
如果是向集合的中间位置 index 插入数据，则需要将数组中索引 index 后的所有数据向后推移一位，然后将数据插入到空出的位置上。
此外，在插入数据前 elementData 可能已经空间不足了，那么还需要先进行扩容操作。扩容操作会创建一个新的符合大小的数组，
并将原数组中的数据迁移到新数组中，然后让 elementData 指向新数组
由此可以看出来，向集合添加数据和进行扩容都可能会导致数组元素大量移动，所以说 ArrayList 存入数据的效率并不高
```
    public boolean add(E e) {
        //在需要的时候进行扩容
        ensureCapacityInternal(size + 1);
        elementData[size++] = e;
        return true;
    }

    public void add(int index, E element) {
        rangeCheckForAdd(index);
        //在需要的时候进行扩容
        ensureCapacityInternal(size + 1);
        //将索引 index 后的所有数值向后推移一位 
        System.arraycopy(elementData, index, elementData, index + 1,size - index);
        elementData[index] = element;
        size++;
    }
```
以上说的是存入单个数据的情况，此外还有存入整个集合的情况
```
    //如果待添加的数据不为空则返回 true，否则返回 false
    public boolean addAll(Collection<? extends E> c) {
        Object[] a = c.toArray();
        int numNew = a.length;
        ensureCapacityInternal(size + numNew);
        //将数组 a 复制到 elementData 的尾端
        System.arraycopy(a, 0, elementData, size, numNew);
        size += numNew;
        return numNew != 0;
    }

    //从指定索引处添加数据，如果待添加的数据不为空则返回 true，否则返回 false
    public boolean addAll(int index, Collection<? extends E> c) {
        rangeCheckForAdd(index);
        Object[] a = c.toArray();
        int numNew = a.length;
        ensureCapacityInternal(size + numNew);
        //需要移动的数组元素数量
        int numMoved = size - index;
        //因为要添加的数据可能刚好是从数组最尾端开始添加，所以 numMoved 可能为 0
        //所以只在 numMoved > 0 的时候才需要对数组的元素值进行移动，以此空出位置给数组 a
        if (numMoved > 0)
            System.arraycopy(elementData, index, elementData, index + numNew, numMoved);
        //将数组 a 包含的数据添加到 elementData 中
        System.arraycopy(a, 0, elementData, index, numNew);
        size += numNew;
        return numNew != 0;
    }
```


移除元素
因为数组是一种内存地址连续的数据结构，所以移除某个元素同样可能导致大量元素移动
删除包含两步
1 移动元素 将index后面的元素向前移动一位，最后一位是重复的的
2 将数组最后元素置为null，数组大小-1
```
    //移除指定索引处的元素值，并返回该值
    public E remove(int index) {
        rangeCheck(index);
        modCount++;
        //待移除的元素值
        E oldValue = elementData(index);
        //因为要移除元素导致需要移动的元素数量
        int numMoved = size - index - 1;
        //因为要移除的元素可能刚好是数组最后一位，所以 numMoved 可能为 0
        //所以只在 numMoved > 0 的时候才需要对数组的元素值进行移动
        if (numMoved > 0)
            System.arraycopy(elementData, index+1, elementData, index, numMoved);
        //不管数组是否需要对元素值进行移动，数组的最后一位都是无效数据了
        //此处将之置为 null 以帮助GC回收                
        elementData[--size] = null;
        return oldValue;
    }

    //移除集合中包含的第一位元素值为 o 的对象
    //如果包含该对象，则返回 true ，否则返回 false
    public boolean remove(Object o) {
        if (o == null) {
            for (int index = 0; index < size; index++)
                if (elementData[index] == null) {
                    fastRemove(index);
                    return true;
                }
        } else {
            for (int index = 0; index < size; index++)
                if (o.equals(elementData[index])) {
                    fastRemove(index);
                    return true;
                }
        }
        return false;
    }
//与remove(int index)逻辑相同，只是没有返回删除的元素    
private void fastRemove(int index) {
    modCount++;
    int numMoved = size - index - 1;
    if (numMoved > 0)
        System.arraycopy(elementData, index+1, elementData, index,
                         numMoved);
    elementData[--size] = null; // clear to let GC do its work
}    
```


扩容机制
再来看下数组的扩容机制的具体实现逻辑
ensureCapacity 方法的入参参数 minCapacity 就用于指定希望扩容后的最小空间，但 minCapacity 最终不会小于 DEFAULT_CAPACITY，
即扩容后的数组容量不会小于 10。之所以要进行最小容量的限制，是为了减少多次扩容的可能性，10 以内的数组很容易就发生扩容
如果在初始化 ArrayList 前已知目标数据的数据量，最好就使用ArrayList(int initialCapacity)来进行初始化，
直接让底层数组扩充到目标大小，或者是在添加数据前就调用 ensureCapacity 方法直接让数组扩容到目标大小，避免之后赋值过程中多次扩容
Capacity   [kəˈpæsəti] n.容量;容积;容纳能力;领悟(或理解、办事)能力;职位;职责
```
    public void ensureCapacity(int minCapacity) {
        int minExpand = (elementData != DEFAULTCAPACITY_EMPTY_ELEMENTDATA)
            ? 0
            : DEFAULT_CAPACITY;
        if (minCapacity > minExpand) {
            ensureExplicitCapacity(minCapacity);
        }
    }

    private void ensureCapacityInternal(int minCapacity) {
        if (elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
            //从10起步
            minCapacity = Math.max(DEFAULT_CAPACITY, minCapacity);
        }
        ensureExplicitCapacity(minCapacity);
    }

    private void ensureExplicitCapacity(int minCapacity) {
       //增加快照数
        modCount++;
        //如果当前数组大小的确是比需要的最小空间 minCapacity小，则进行扩容
        if (minCapacity - elementData.length > 0)
            grow(minCapacity);
    }
```
实际上完成扩容操作的是 grow(int minCapacity) 方法。在扩容前，会先判断如果将容量提升到当前的 1.5 倍是否能达到 minCapacity 的要求 ，
如果符合要求则直接将容量扩充到当前的 1.5 倍，否则扩充到 minCapacity，但最终容量不能大于 Integer.MAX_VALUE
构建出一个新的符合大小的数组后，就将原数组中的元素复制到新数组中，至此就完成了扩容
```
    //数组可扩容到的最大容量  有些虚拟机在数组中保留一些头字
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
    private void grow(int minCapacity) {
        int oldCapacity = elementData.length;
        //假设扩容后的空间大小是原先的1.5倍
        int newCapacity = oldCapacity + (oldCapacity >> 1);
        if (newCapacity - minCapacity < 0)
            newCapacity = minCapacity;
        if (newCapacity - MAX_ARRAY_SIZE > 0)
            newCapacity = hugeCapacity(minCapacity);
        elementData = Arrays.copyOf(elementData, newCapacity);
    }
  private static int hugeCapacity(int minCapacity) {
        if (minCapacity < 0) // overflow
            throw new OutOfMemoryError();
        return (minCapacity > MAX_ARRAY_SIZE) ?
            Integer.MAX_VALUE :
            MAX_ARRAY_SIZE;
    }    
```
//todo 位运算



扩容测试
```
    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        List<String> stringList = new ArrayList<>();
        for (int i = 0; i < 300000; i++) {
            stringList.add("leavesC " + i);
        }
        long endTime = System.currentTimeMillis();
        System.out.println("初始容量为0，添加300000所用时间：" + (endTime - startTime) + "毫秒");

        startTime = System.currentTimeMillis();
        List<String> stringList2 = new ArrayList<>(100000);
        for (int i = 0; i < 300000; i++) {
            stringList2.add("leavesC " + i);
        }
        endTime = System.currentTimeMillis();
        System.out.println("初始容量为100000，添加300000所用时间：" + (endTime - startTime) + "毫秒");

        startTime = System.currentTimeMillis();
        List<String> stringList3 = new ArrayList<>(300000);
        for (int i = 0; i < 300000; i++) {
            stringList3.add("leavesC " + i);
        }
        endTime = System.currentTimeMillis();
        System.out.println("初始容量为300000，添加300000所用时间：" + (endTime - startTime) + "毫秒");
    }
```
测试结果  cpu： AMD Ryzen 7 5700U with Radeon Graphics
```
初始容量为0，所用时间：65毫秒
初始容量为100000，所用时间：69毫秒
初始容量为300000，所用时间：21毫秒
```

修改元素
```
    //将索引 index 出的元素值置为 element，并返回原始数值
    public E set(int index, E element) {
    	rangeCheck(index);
    	E oldValue = elementData(index);
    	elementData[index] = element;
    	return oldValue;
    }
```

遍历数组
遍历数组的方法包含以下几个，逻辑都比较简单，直接看注释即可。一个比较重要的知识点是看方法内部对 modCount 的校验
在这里有个小细节，ArrayList 里多处使用到了 modCount 这个成员变量，modCount 相当于对 ArrayList 的一个简单“快照”，
即类似于 ArrayList 的一个版本号，每当添加、移除和修改元素时，modCount 都会递增
在我们遍历 ArrayList 的过程中，如果同时进行增(扩容时modeCount++)减元素的操作，或者是存在多线程同时增减元素，那么就会导致遍历结果变得不可靠，
或者是直接就导致数组越界异常，所以 ArrayList 就通过 modCount 来标记当前的迭代行为是否处于可靠状态。如果在遍历数组元素的过程中
判断到 modCount 的值前后发生了变化，就说明在遍历过程中 ArrayList 被改动了，此时就认定遍历结果不可靠，直接抛出异常。
需要注意的是，modCount 做的只是一个简单校验，无法准确判断出当前的遍历操作就真的是安全的
```
    @Override
    public void forEach(Consumer<? super E> action) {
        Objects.requireNonNull(action);
        final int expectedModCount = modCount;
        final E[] elementData = (E[]) this.elementData;
        final int size = this.size;
        //遍历时保证快照相等
        for (int i=0; modCount == expectedModCount && i < size; i++) {
            //将集合元素依次传递给 accept 方法
            action.accept(elementData[i]);
        }
        //如果 modCount 值被改动了，说明遍历过程中数组有被改动到
        //那么就停止遍历并抛出异常
        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
    }

    //按照给定规则对集合元素进行过滤，如果元素符合过滤规则那就将之移除
    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        Objects.requireNonNull(filter);
        //要移除的元素个数
        int removeCount = 0;
        //用于标记集合是哪个索引位置需要被移除
        final BitSet removeSet = new BitSet(size);
        final int expectedModCount = modCount;
        final int size = this.size;
        for (int i=0; modCount == expectedModCount && i < size; i++) {
            @SuppressWarnings("unchecked")
            final E element = (E) elementData[i];
            //依次判断集合元素是否符合过滤规则
            if (filter.test(element)) {
                //set 方法将导致索引位置 i 的元素变为 true
                removeSet.set(i);
                removeCount++;
            }
        }
        //不允许在排序的过程中集合被其它方法修改了数组（例如：移除元素）
        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
        //只有 removeCount > 0 才说明需要移除元素
        final boolean anyToRemove = removeCount > 0;
        if (anyToRemove) {
            //集合移除指定元素后的大小   //todo BitSet 数据聚集的过程没看懂
            final int newSize = size - removeCount;
            for (int i=0, j=0; (i < size) && (j < newSize); i++, j++) {
                //略过被标记为 true 的位置，直接跳到不需要移除元素的数组索引位
                i = removeSet.nextClearBit(i);
                //有效数据逐渐从尾部向头部聚集
                elementData[j] = elementData[i];
            }
            //移除尾部的无效数据，有利于GC回收
            for (int k=newSize; k < size; k++) {
                elementData[k] = null;
            }
            this.size = newSize;
            //不允许在排序的过程中集合被其它方法修改了数组（例如：移除元素）
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            modCount++;
        }
        return anyToRemove;
    }

    //将集合元素遍历传递给 operator，并将原始数据替换为 operator 的返回值
    @Override
    @SuppressWarnings("unchecked")
    public void replaceAll(UnaryOperator<E> operator) {
        Objects.requireNonNull(operator);
        final int expectedModCount = modCount;
        final int size = this.size;
        for (int i=0; modCount == expectedModCount && i < size; i++) {
            //依次传递数组元素给 apply 方法，并将其返回值替换原始数据
            elementData[i] = operator.apply((E) elementData[i]);
        }
        //不允许在排序的过程中集合被其它方法修改了数组（例如：移除元素）
        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
        modCount++;
    }
```
BitSet类实现了一个按需增长的位向量。位Set的每一个组件都有一个boolean值。用非负的整数将BitSet的位编入索引。
可以对每个编入索引的位进行测试、设置或者清除。通过逻辑与、逻辑或和逻辑异或操作，可以使用一个 BitSet修改另一个 BitSet的内容
https://www.cnblogs.com/xujian2014/p/5491286.html   todo

迭代器
ArrayList 内部包含一个用于迭代元素的 Iterator 实现类，其用法如下所示
```
    public static void main(String[] args) {
        List<String> stringList = new ArrayList<>();
        stringList.add("aaaa");
        Iterator<String> iterator = stringList.iterator();
        if (iterator.hasNext()) {
            String next = iterator.next();
            System.out.println(next);
        }
    }
```
迭代器实现
```
    protected transient int modCount = 0;

    public Iterator<E> iterator() {
        return new Itr();
    }

    private class Itr implements Iterator<E> {
        
        //lastRet 指向的元素的下一个元素的索引
        int cursor;

        //最后一个返回的元素的索引
        //如果值为 -1，说明还未返回过元素或者改元素被移除了
        int lastRet = -1;

        //用于验证集合的数据结构在迭代的过程中是否被修改了
        int expectedModCount = modCount;

        //是否还有元素未被遍历
        public boolean hasNext() {
            return cursor != size;
        }

        //获取下一个元素
        @SuppressWarnings("unchecked")
        public E next() {
            checkForComodification();
            int i = cursor;
            //如果索引值超出取值范围则抛出异常
            if (i >= size)
                throw new NoSuchElementException();
            Object[] elementData = ArrayList.this.elementData;
            //如果索引值超出数组的可索引范围则抛出异常
            if (i >= elementData.length)
                throw new ConcurrentModificationException();
            cursor = i + 1;
            return (E) elementData[lastRet = i];
        }

        //移除 lastRet 指向的元素
        public void remove() {
            if (lastRet < 0)
                throw new IllegalStateException();
            checkForComodification();
            try {
                ArrayList.this.remove(lastRet);
                //因为 lastRet 位置原始的元素被移除了，所以此时 lastRet 指向的元素是原先 lastRet+1 位置的元素
                cursor = lastRet;
                lastRet = -1;
                //因为是 Itr 主动对集合进行修改，所以此处需要主动更新 expectedModCount 值，避免之后抛出异常
                expectedModCount = modCount;
            } catch (IndexOutOfBoundsException ex) {
                throw new ConcurrentModificationException();
            }
        }

        //遍历从索引 cursor 开始之后剩下的元素
        @Override
        @SuppressWarnings("unchecked")
        public void forEachRemaining(Consumer<? super E> consumer) {
            Objects.requireNonNull(consumer);
            final int size = ArrayList.this.size;
            int i = cursor;
            if (i >= size) {
                return;
            }
            final Object[] elementData = ArrayList.this.elementData;
            if (i >= elementData.length) {
                throw new ConcurrentModificationException();
            }
            //遍历调用 accept 方法
            while (i != size && modCount == expectedModCount) {
                consumer.accept((E) elementData[i++]);
            }
            cursor = i;
            lastRet = i - 1;
            checkForComodification();
        }

        //判断迭代器在遍历集合的过程中，集合是否被外部改动了（例如被其它迭代器移除了元素）
        //如果是的话则抛出异常
        final void checkForComodification() {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
        }
    }
```
迭代器为什么没有爆出异常
remove每次删除一个元素，都会将modCount的值重新赋值给expectedModCount，这样2个变量就相等了，
不会触发java.util.ConcurrentModificationException异常，其中对遍历的cursor进行了更新，防止数据异常


遍历代码测试   参考https://blog.csdn.net/zwwhnly/article/details/104987143
增强for
```
public static void main(String[] args) {
    List<String> platformList = new ArrayList<>();
    platformList.add("博客园");
    platformList.add("CSDN");
    platformList.add("掘金");

    for (String platform : platformList) {
        if (platform.equals("博客园")) {
            platformList.remove(platform);
        }
    }

    System.out.println(platformList);
}

```
结果
```
Exception in thread "main" java.util.ConcurrentModificationException
	at java.util.ArrayList$Itr.checkForComodification(ArrayList.java:909)
	at java.util.ArrayList$Itr.next(ArrayList.java:859)
	at Test.main(Test.java:17)

```
查看Test.class   增强for被编译为iterator遍历
```
public static void main(String[] args) {
        List<String> platformList = new ArrayList();
        platformList.add("博客园");
        platformList.add("CSDN");
        platformList.add("掘金");
        Iterator var2 = platformList.iterator();

        while(var2.hasNext()) {
            String platform = (String)var2.next();
            if (platform.equals("博客园")) {
                platformList.remove(platform);
            }
        }

        System.out.println(platformList);
    }
```

如何修正for的错误
1 Iterator的remove
2 for循环正序+坐标修正
```
 List<String> platformList = new ArrayList<>();
    platformList.add("博客园");
    platformList.add("CSDN");
    platformList.add("掘金");

    for (int i = 0; i < platformList.size(); i++) {
        String item = platformList.get(i);

        if (item.equals("博客园")) {
            platformList.remove(i);
            i = i - 1;
        }
    }

    System.out.println(platformList);
```
结果
```
[CSDN, 掘金]
```
通过数组的下标来删除，不过有个注意事项就是删除元素后，要修正下下标的值：i = i - 1;
为什么要修正下标的值呢？
因为刚开始元素的下标是这样的：
```
0 博客园
1 CSDN
2 掘金
```
第1次循环将元素"博客园"删除后，元素的下标变成了下面这样：
```
0 CSDN
1 掘金
```
第2次循环时i的值为1，也就是取到了元素”掘金“，这样就导致元素"CSDN"被跳过检查了，所以删除完元素后，我们要修正下下标，
 这也是上面代码中i = i - 1;的用途。

3 for循环倒序遍历
```
List<String> platformList = new ArrayList<>();
    platformList.add("博客园");
    platformList.add("CSDN");
    platformList.add("掘金");

    for (int i = platformList.size() - 1; i >= 0; i--) {
        String item = platformList.get(i);

        if (item.equals("博客园")) {
            platformList.remove(i);
        }
    }

    System.out.println(platformList);
```
结果
```
[CSDN, 掘金]
```
刚开始元素的下标是这样的
```
0 博客园
1 CSDN
2 掘金
```
第1次循环将元素"博客园"删除后，元素的下标变成了下面这样：
```
0 CSDN
1 掘金
```
倒序第2次循环时i的值为1，也就是取到了元素”CSDN“，不会导致跳过元素，所以不需要修正下标

4 使用removeIf()方法(推荐)
从JDK1.8开始，可以使用removeIf()方法来代替 Iterator的remove()方法实现一边遍历一边删除，其实，IDEA中也会提示：
```
platformList.removeIf(platform -> "博客园".equals(platform));
```
看下removeIf()方法的源码，会发现其实底层也是用的Iterator的remove()方法：
```
    default boolean removeIf(Predicate<? super E> filter) {
        Objects.requireNonNull(filter);
        boolean removed = false;
        final Iterator<E> each = iterator();
        while (each.hasNext()) {
            if (filter.test(each.next())) {
                each.remove();
                removed = true;
            }
        }
        return removed;
    }
```
