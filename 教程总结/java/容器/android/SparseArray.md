JDK 1.8，Android SDK 30
https://juejin.cn/post/6931563785245163534

android.util.SparseArray   Sparse [spɑːs]  稀疏;稀疏的;稀少的;稀稀落落;稀少的,稀疏的

SparseArray 和 ArrayMap 这两个 Android 系统独有的集合框架类，这两个容器在使用上类似于 HashMap，都是用于存储键值对。
由于 Android 系统对于内存比较敏感，所以 SparseArray 和 ArrayMap 在内存使用方面会比较克制，这里就来分析下其实现原理和优势点

数据结构两个数组
keys    values
1       "a"
100     "b"
100     "c" 

1、基本概念
SparseArray 的使用方式：
```    
SparseArray<String> sparseArray = new SparseArray<>();
sparseArray.put(100,"leavesC");
sparseArray.remove(100);
sparseArray.get(100);
sparseArray.removeAt(29);
```
SparseArray< E > 相当于 Map< Integer , E > ，key 值固定为 int 类型，在初始化时只需要声明 value 的数据类型即可，
其内部用两个数组分别来存储 key 和 value：int[] mKeys ; Object[] mValues
mKeys 和 mValues 按照如下规则对应起来：
 1 假设要向 SparseArray 存入 key 为 10，value 为 200 的键值对，则先将 10 存到 mKeys 中，假设 10 在 mKeys 中对应的索引值是 2，
   则将 value 存入 mValues[2] 中
2 mKeys 中的元素值按照递增的方法进行存储，每次存放新的键值对时都通过二分查找的方式将 key 插入到 mKeys 中
3 当要从 SparseArray 取值时，先通过二分查找法找到 key 在 mKeys 中对应的索引，然后根据该索引从 mValues 中取值
从以上可以看出来的一点就是：SparseArray 避免了 HashMap 每次存取值时的装箱拆箱操作，key 值保持为基本数据类型 int，
  减少了性能开销

2、类声明
SparseArray 本身并没有直接继承于任何类，内部也没有使用到 Java 原生的集合框架，所以 SparseArray 是 Android 系统自己
 实现的一个集合容器类
```
public class SparseArray<E> implements Cloneable
```

3、全局变量
MGarbage 是 SparseArray 的一个优化点之一，用于标记当前是否有需要垃圾回收(GC)的元素，当该值被置为 true 时，
  意味着当前存在无效元素，需要进行垃圾回收，但回收操作并不会马上进行，而是在后续操作中再统一进行
```
    //键值对被移除后对应的 value 会变成此值，用来当做 GC 标记位
    private static final Object DELETED = new Object();

    //用于标记当前是否有待垃圾回收(GC)的元素
    private boolean mGarbage = false;

    private int[] mKeys;

    private Object[] mValues;

    //当前集合元素的数量
    //该值并不一定是时时处于正确状态，因为有可能出现只删除 key 和 value 两者之一的情况
    //所以 size() 方法内都会先进行 GC
    private int mSize;
```

4、构造函数
Key 数组和 value 数组的默认大小都是 10，如果在初始化时已知最终数据量的大小，则可以直接指定初始容量，这样可以避免后续的扩容操作
```
    //设置数组的默认初始容量为10
    public SparseArray() {
        this(10);
    }

    public SparseArray(int initialCapacity) {
        if (initialCapacity == 0) {
            mKeys = EmptyArray.INT;
            mValues = EmptyArray.OBJECT;
        } else {
            mValues = ArrayUtils.newUnpaddedObjectArray(initialCapacity);
            mKeys = new int[mValues.length];
        }
        mSize = 0;
    }
```
私有API
```
 libcore/luni/src/main/java/libcore/util/EmptyArray.java
 public static final int[] INT = new int[0];
 public static final Object[] OBJECT = new Object[0];
 
 frameworks/base/core/java/com/android/internal/util/ArrayUtils.java
 public static Object[] newUnpaddedObjectArray(int minLen) {
        return (Object[])VMRuntime.getRuntime().newUnpaddedArray(Object.class, minLen);
    }
```

5、添加元素
添加元素的方法有几个，主要看 put(int key, E value) 方法就可以，该方法用于存入 key 和 value 组成的键值对
按照前面所说的 SparseArray 存储键值对的规则，put 方法会先判断当前 mKeys 中是否已经有相同的 key 值，有的话就用 value 
覆盖 mValues 中的旧值。如果不存在相同 key 值，在将 key 插入到 mKeys 后需要在 mValues 的相同索引位置插入 value。
由于 mKeys 是按照大小对元素值进行排序存储的，所以将 key 插入到 mKeys 可能会导致元素重新排序，从而连锁导致 mValues 也需要
 重新排序

put 方法从 mKeys 查找 key 用的是 ContainerHelpers 类提供的二分查找方法：binarySearch，用于获取 key 在 mKeys 中的当前索引
（存在该 key 的话）或者是应该存放的位置的索引（不存在该 key），方法的返回值可以分为三种情况：
1如果 mKeys 中存在对应的 key，则直接返回对应的索引值
2如果 mKeys 中不存在对应的 key
  1假设 mKeys 中存在"值比 key 大且大小与 key 最接近的值的索引"为 presentIndex，则此方法的返回值为 ~presentIndex
  2如果 mKeys 中不存在比 key 还要大的值的话，则返回值为 ~mKeys.length
```
   //android.util.ContainerHelpers
   //This is Arrays.binarySearch(), but doesn't do any argument validation  从Arrays.改过来的，但是没有进行参数合法性验证
    static int binarySearch(int[] array, int size, int value) {
        int lo = 0;
        int hi = size - 1;
        while (lo <= hi) {
            final int mid = (lo + hi) >>> 1;
            final int midVal = array[mid];
            if (midVal < value) {
                lo = mid + 1;
            } else if (midVal > value) {
                hi = mid - 1;
            } else {
                return mid;  // value found
            }
        }
        return ~lo;  // value not present
    }
```
//todo 二分搜索确定元素位置   根据下标的正负确定是否查找成功
可以看到，如果 mKeys 存在目标 key，那么返回值即对应的索引位置。如果不存在目标 key，其返回值也指向了应该让 key 存入的位置，
因为当不存在目标 key 时，将计算出的索引值进行 ~ 运算后返回值一定是负数，从而与“找得到目标 key 的情况（返回值大于等于0）”的情况
区分开。从这里可以看出该方法的巧妙之处，单纯的一个返回值就可以区分出多种情况，且通过这种方式来存放数据可以使得 mKeys 的内部值
一直是按照值递增的方式来排序的

再来具体看看 put 方法的逻辑
1. 根据二分超找key在keys数组的位置index，如果不存在返回比key大的第一个位置的index的反码
2. 如果查到key的index，直接覆盖原值mValues[i] = value
3. 反码取反得到index，如果Values[i]没有元素，将key和value分别存入keys数组和value数组
4. 如果存在冗余数据，那么就先进行 GC
5. 索引 i 位置已经用于存储其它数据，将i 开始的所有数据都需要向后移动一位，并将 key 存到 mKeys[i]  这个期间可能存在扩容
```
    public void put(int key, E value) {
        //用二分查找法查找指定 key 在 mKeys 中的索引值
        int i = ContainerHelpers.binarySearch(mKeys, mSize, key);
        if (i >= 0) { //对应已经存在相同 key 的情况
            mValues[i] = value;
        } else {
            //取反，拿到真实的索引位置
            i = ~i;
            //如果目标位置还未赋值，则直接存入数据即可
            if (i < mSize && mValues[i] == DELETED) {
                mKeys[i] = key;
                mValues[i] = value;
                return;
            }
            //如果存在冗余数据，那么就先进行 GC
            if (mGarbage && mSize >= mKeys.length) {
                gc();
                //GC 后再次进行查找，因为值可能已经发生变化了
                i = ~ContainerHelpers.binarySearch(mKeys, mSize, key);
            }
            //索引 i 位置已经用于存储其它数据了，此时就需要对数组元素进行迁移
            //所以从索引 i 开始的所有数据都需要向后移动一位，并将 key 存到 mKeys[i]
            mKeys = GrowingArrayUtils.insert(mKeys, mSize, i, key);
            mValues = GrowingArrayUtils.insert(mValues, mSize, i, value);
            mSize++;
        }
    }

    //将索引 index 处的元素赋值为 value
    //知道目标位置的话可以直接向 mValues 赋值
    public void setValueAt(int index, E value) {
        if (index >= mSize && UtilConfig.sThrowExceptionForUpperArrayOutOfBounds) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        //如果需要则先进行垃圾回收
        if (mGarbage) {
            gc();
        }
        mValues[index] = value;
    }

    //和 put 方法类似
    //但在存入数据前先对数据大小进行了判断，有利于减少对 mKeys 进行二分查找的次数
    //所以在“存入的 key 比现有的 mKeys 值都大”的情况下会比 put 方法性能高
    public void append(int key, E value) {
        if (mSize != 0 && key <= mKeys[mSize - 1]) {
            put(key, value);
            return;
        }
        if (mGarbage && mSize >= mKeys.length) {
            gc();
        }
        mKeys = GrowingArrayUtils.append(mKeys, mSize, key);
        mValues = GrowingArrayUtils.append(mValues, mSize, value);
        mSize++;
    }
```

6、移除元素
上文说了，布尔变量 mGarbage 用于标记当前是否有待垃圾回收(GC)的元素，当该值被置为 true 时，即意味着当前状态需要进行垃圾回收，
但回收操作并不马上进行，而是在后续操作中再完成
以下几个方法在移除元素时，都只是切断了 mValues 对 value 的引用，而 mKeys 并没有进行回收，这个操作会留到 gc() 进行处理
1.用二分查找法查找指定 key 在 mKeys 中的索引值
2. index>0代表元素找到了，mValues[i] = DELETED，标记当前需要垃圾回收mGarbage=true
```
    public void delete(int key) {
        //用二分查找法查找指定 key 在 mKeys 中的索引值
        int i = ContainerHelpers.binarySearch(mKeys, mSize, key);
        if (i >= 0) {
            if (mValues[i] != DELETED) {
                mValues[i] = DELETED;
                //标记当前需要进行垃圾回收
                mGarbage = true;
            }
        }
    }

    public void remove(int key) {
        delete(key);
    }

    //和 delete 方法基本相同，差别在于会返回 key 对应的元素值
    public E removeReturnOld(int key) {
        int i = ContainerHelpers.binarySearch(mKeys, mSize, key);
        if (i >= 0) {
            if (mValues[i] != DELETED) {
                final E old = (E) mValues[i];
                mValues[i] = DELETED;
                mGarbage = true;
                return old;
            }
        }
        return null;
    }

    //删除指定索引对应的元素值
    public void removeAt(int index) {
        if (index >= mSize && UtilConfig.sThrowExceptionForUpperArrayOutOfBounds) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        if (mValues[index] != DELETED) {
            mValues[index] = DELETED;
            //标记当前需要进行垃圾回收
            mGarbage = true;
        }
    }

    //删除从起始索引值 index 开始之后的 size 个元素值
    public void removeAtRange(int index, int size) {
        //避免发生数组越界的情况
        final int end = Math.min(mSize, index + size);
        for (int i = index; i < end; i++) {
            removeAt(i);
        }
    }

    //移除所有元素值
    public void clear() {
        int n = mSize;
        Object[] values = mValues;
        for (int i = 0; i < n; i++) {
            values[i] = null;
        }
        mSize = 0;
        mGarbage = false;
    }

```


7、查找元素
查找元素的方法较多，逻辑都挺简单的
1. 二分查找法查找指定 key 在 mKeys 中的索引值
2. index<0没找到返回null，index>0找到了，返回mValues[i]
```    //根据 key 查找相应的元素值，查找不到则返回默认值
    @SuppressWarnings("unchecked")
    public E get(int key, E valueIfKeyNotFound) {
        //用二分查找法查找指定 key 在 mKeys 中的索引值
        int i = ContainerHelpers.binarySearch(mKeys, mSize, key);
        //如果找不到该 key 或者该 key 尚未赋值，则返回默认值
        if (i < 0 || mValues[i] == DELETED) {
            return valueIfKeyNotFound;
        } else {
            return (E) mValues[i];
        }
    }

    //根据 key 查找相应的元素值，查找不到则返回 null
    public E get(int key) {
        return get(key, null);
    }

    //因为 mValues 中的元素值并非一定是连贯的，有可能掺杂着 DELETED 
    //所以在遍历前需要先进行 GC，这样通过数组取出的值才是正确的
    @SuppressWarnings("unchecked")
    public E valueAt(int index) {
        if (mGarbage) {
            gc();
        }
        return (E) mValues[index];
    }

    //根据索引值 index 查找对应的 key 
    public int keyAt(int index) {
        if (mGarbage) {
            gc();
        }
        return mKeys[index];
    }

    //根据 key 对应的索引值
    public int indexOfKey(int key) {
        if (mGarbage) {
            gc();
        }
        return ContainerHelpers.binarySearch(mKeys, mSize, key);
    }

    //根据 value 查找对应的索引值
    public int indexOfValue(E value) {
        if (mGarbage) {
            gc();
        }
        for (int i = 0; i < mSize; i++) {
            if (mValues[i] == value) {
                return i;
            }
        }
        return -1;
    }

    //与 indexOfValue 方法类似，但 indexOfValue 方法是通过比较 == 来判断是否同个对象
    //而此方法是通过 equals 方法来判断是否同个对象
    public int indexOfValueByValue(E value) {
        if (mGarbage) {
            gc();
        }
        for (int i = 0; i < mSize; i++) {
            if (value == null) {
                if (mValues[i] == null) {
                    return i;
                }
            } else {
                if (value.equals(mValues[i])) {
                    return i;
                }
            }
        }
        return -1;
    }
```


8、垃圾回收
因为 SparseArray 中会出现只移除 key 和 value 两者之一的情况，导致当前数组中的有效数据并不是全都紧挨着排列在一起的，
即存在无效值，因此 gc() 方法会根据 mValues 中到底存在多少有效数据，将 mKeys 和 mValues 中的数据进行重新排列，
将有意义的元素值紧挨着排序在一起
```
    private void gc() {
        int n = mSize;
        int o = 0;
        int[] keys = mKeys;
        Object[] values = mValues;
        for (int i = 0; i < n; i++) {
            Object val = values[i];
            //val 非 DELETED ，说明该位置可能需要移动数据   
            if (val != DELETED) {
                //将索引 i 处的值赋值到索引 o 处
                //所以如果 i == o ，则不需要执行代码了
                if (i != o) {
                    keys[o] = keys[i];
                    values[o] = val;
                    values[i] = null;
                }
                o++;
            }else{
               val=DELETED说明是废弃数据，跳过  此时o!=i   o的位置需要其他元素填充
            }
        }
        mGarbage = false;
        mSize = o;
    }
```
http://gityuan.com/2019/01/13/arraymap/
延迟回收机制的好处在于首先删除方法效率更高，同时减少数组数据来回拷贝的次数，比如删除某个数据后被标记删除，接着又需要在相同位置插入数据，
则不需要任何数组元素的来回移动操作。可见，对于SparseArray适合频繁删除和插入来回执行的场景，性能很好
调用时机：
1. put  setValueAt  append
2.size()
3.keyAt  valueAt    indexOfKey  indexOfValue  indexOfValueByValue

9、优劣势总结
从上文的介绍来看，SparseArray 的主要优势有以下几点：
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


10、关联类
SparseArray 属于泛型类，所以即使 value 是基本数据类型也会被装箱和拆箱，如果想再省去这一部分开销的话，可以使用
SparseBooleanArray、SparseIntArray 和 SparseLongArray 等三个容器类，这三个容器的实现原理和 SparseArray 相同，
但是 value 还是属于基本数据类型
此外，系统还提供了 LongSparseArray 这个容器类，其实现原理和 SparseArray 类似，但是 key 固定为 long 类型，value 
通过泛型来声明，对于日常开发中比较有用的一点是可以用来根据 viewId 来存储 view 对象

//todo 性能测试   速度和内存占用，gc怎么测？
//todo 原码，反吗，补码


GrowingArrayUtils 相关，内部API
 扩容总结： 4以内扩充至8,大于4扩充为2倍
```
   frameworks/base/core/java/com/android/internal/util/GrowingArrayUtils.java
   public static <T> T[] insert(T[] array, int currentSize, int index, T element) {
        //搬移元素并插入
        if (currentSize + 1 <= array.length) {
            System.arraycopy(array, index, array, index + 1, currentSize - index);
            array[index] = element;
            return array;
        }
       //扩容
        @SuppressWarnings("unchecked")
        T[] newArray = ArrayUtils.newUnpaddedArray((Class<T>)array.getClass().getComponentType(),
                growSize(currentSize));
        System.arraycopy(array, 0, newArray, 0, index);
        newArray[index] = element;
        System.arraycopy(array, index, newArray, index + 1, array.length - index);
        return newArray;
    }
    
    public static int growSize(int currentSize) {
        return currentSize <= 4 ? 8 : currentSize * 2;
    }
    
    //追加元素
      @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public static <T> T[] append(T[] array, int currentSize, T element) {
        if (currentSize + 1 > array.length) {
            @SuppressWarnings("unchecked")
            T[] newArray = ArrayUtils.newUnpaddedArray(
                    (Class<T>) array.getClass().getComponentType(), growSize(currentSize));
            System.arraycopy(array, 0, newArray, 0, currentSize);
            array = newArray;
        }
        array[currentSize] = element;
        return array;
    }
```