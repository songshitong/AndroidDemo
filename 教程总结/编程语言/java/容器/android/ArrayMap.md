jdk1.8  android sdk 30
https://juejin.cn/post/6931563785245163534#heading-12

Bundle与ArrayMap
Android为什么要设计Bundle而不是直接使用HashMap来直接进行数据传递?
Bundle内部是由ArrayMap实现的,ArrayMap在设计上比传统的HashMap更多考虑的是内存优化
Bundle使用的是Parcelable序列化,而HashMap使用Serializable序列化


ArrayMap是Android专门针对内存优化而设计的，用于取代Java API中的HashMap数据结构。为了更进一步优化key是int类型的Map，
Android再次提供效率更高的数据结构SparseArray，可避免自动装箱过程。对于key为其他类型则可使用ArrayMap。
HashMap的查找和插入时间复杂度为O(1)的代价是牺牲大量的内存来实现的，而SparseArray和ArrayMap性能略逊于HashMap，但更节省内存


ArrayMap 属于泛型类，继承了 Map 接口，其使用方式和 HashMap 基本一样，但在内部逻辑上有着很大差异，所以需要了解其实现原理后
才能明白 ArrayMap 到底适用于哪些场景
```
public final class ArrayMap<K, V> implements Map<K, V>
```
构造器
```
 //默认容量为0,当添加元素时扩容
 public ArrayMap() {
        this(0, false);
    }
 public ArrayMap(int capacity) {
        this(capacity, false);
    }
  static final int[] EMPTY_IMMUTABLE_INTS = new int[0];  
  public ArrayMap(int capacity, boolean identityHashCode) {
        mIdentityHashCode = identityHashCode;
        //capacity<0 说明不允许扩容，allocArrays时会进行检查
        if (capacity < 0) {
            mHashes = EMPTY_IMMUTABLE_INTS;
            mArray = EmptyArray.OBJECT;
        } else if (capacity == 0) {
            mHashes = EmptyArray.INT;
            mArray = EmptyArray.OBJECT;
        } else {
            //复用数组
            allocArrays(capacity);
        }
        mSize = 0;
    }        
```

1、存储机制
ArrayMap 中包含以下两个数组。mHashes 用于存储键值对中 key 的哈希值，mArray 则用于存储 key 和 value，即每个键值对会一起
 被存入 mArray 中
```   
    int[] mHashes;
    Object[] mArray;
```
两个数组
key的hash数组       object数组  key与value相邻存储
index=0,hash0     index=0,key="a"
                  index=1,value="a"
index=1,hash1     index=2,key="b"
                  index=3,value="b"
index=2,hash1     index=4,key="c"
                  index=5,value="c"
index=3,hash2     index=6,key="d"
                  index=6,value="d"

当向 ArrayMap 插入键值对时，会先计算出 key 的哈希值，将 keyHash 按照大小顺序存入 mHashes 中，拿到其位置索引 index。然后
  将 key 存入 mArray 的 index<<1 位置，将 value 存入 mArray 的 (index<<1 + 1) 位置，即 key 和 value 会存储在相邻的位置。
  从这个位置对应关系来看，mArray 的所需容量至少也需要是 mHashes 的两倍，且每个 key-value 的排列关系也是和 keyHash 的排列保持一致
当要通过 key 对象向 ArrayMap 取值时，就先计算出 keyHash，然后通过二分查找法找到 keyHash 在 mHashes 中的位置索引 index，
  然后在 (index<<1 + 1)位置从 mArray 拿到 value


2、添加元素
有几个用于添加元素的方法，当中重点看 put 方法即可，其它添加元素的方法都需要依靠该方法来实现。前文有讲到，key-value 最终是会
  相邻着存入 mArray 中的，而 key-value 在 mArray 中的位置是由 keyHash 和 mHashes 来共同决定的，put 方法的整体逻辑如下所述：
1 根据二分查找法获取到 keyHash 在 mHashes 中的索引位置 index
2 如果 index 大于等于 0，说明在 mArray 中 key 已存在，那么直接覆盖旧值即可，结束流程
3 如果 index 小于 0，说明在 mArray 中 key 不存在，~index 此时代表的是 keyHash 按照递增顺序应该插入 mHashes 的位置
4 判断是否需要扩容，需要的话则进行扩容。如果符合缓存标准的话，则会缓存扩容前的数组
5 对最终的数组进行数据迁移，插入 key-value 和 keyHash
```
    @Override
    public V put(K key, V value) {
        final int osize = mSize;
        final int hash;
        int index;
       	
      	//第一步
        if (key == null) {
            hash = 0; 
            index = indexOfNull();
        } else {
            hash = mIdentityHashCode ? System.identityHashCode(key) : key.hashCode();
            index = indexOf(key, hash);
        }
        
        //第二步
        if (index >= 0) {
            index = (index<<1) + 1;
            final V old = (V)mArray[index];
            mArray[index] = value;
            return old;
        }

      	//第三步
        index = ~index;
        
      	//第四步
        if (osize >= mHashes.length) {
            //ArrayMap 的扩容机制相比 HashMap 会比较克制
            //没超过baseszie，设为Basesize,超过设为2BaseSzie，当数组长度已超出 BASE_SIZE*2 后，数组容量按照 1.5 倍来扩容
            //base_size =4
            final int n = osize >= (BASE_SIZE*2) ? (osize+(osize>>1))
                    : (osize >= BASE_SIZE ? (BASE_SIZE*2) : BASE_SIZE);

            if (DEBUG) Log.d(TAG, "put: grow from " + mHashes.length + " to " + n);

            final int[] ohashes = mHashes;
            final Object[] oarray = mArray;
            allocArrays(n);

            if (CONCURRENT_MODIFICATION_EXCEPTIONS && osize != mSize) {
                throw new ConcurrentModificationException();
            }

            if (mHashes.length > 0) {
                if (DEBUG) Log.d(TAG, "put: copy 0-" + osize + " to 0");
                System.arraycopy(ohashes, 0, mHashes, 0, ohashes.length);
                System.arraycopy(oarray, 0, mArray, 0, oarray.length);
            }

            freeArrays(ohashes, oarray, osize);
        }

      	//第五步
        if (index < osize) {
            if (DEBUG) Log.d(TAG, "put: move " + index + "-" + (osize-index)
                    + " to " + (index+1));
            System.arraycopy(mHashes, index, mHashes, index + 1, osize - index);
            System.arraycopy(mArray, index << 1, mArray, (index + 1) << 1, (mSize - index) << 1);
        }

        if (CONCURRENT_MODIFICATION_EXCEPTIONS) {
            if (osize != mSize || index >= mHashes.length) {
                throw new ConcurrentModificationException();
            }
        }
        mHashes[index] = hash;
        mArray[index<<1] = key;
        mArray[(index<<1)+1] = value;
        mSize++;
        return null;
    }
```

append 方法也是用于添加元素的，带有一点“追加”的意思，如果外部可以确定本次插入的 key 的 hash 值比当前所有已有值都大的话，
  那么就可以直接向 mHashes 的尾部插入数据，从而节省了二分查找的过程。所以 append 方法会先和 mHashes 的最后一个元素值进行对比，
  如果 keyHash 比该值大的话就说明可以直接保存到尾部，校验不通过的话还是会调用 put 方法
```
    public void append(K key, V value) {
        int index = mSize;
        final int hash = key == null ? 0
                : (mIdentityHashCode ? System.identityHashCode(key) : key.hashCode());
        if (index >= mHashes.length) {
            throw new IllegalStateException("Array is full");
        }
        //如果 mHashes 当前的最后一个值比 hash 大，hash 没法直接插到尾部，那么就还是需要调用 put 方法
        if (index > 0 && mHashes[index-1] > hash) {
            RuntimeException e = new RuntimeException("here");
            e.fillInStackTrace();
            Log.w(TAG, "New hash " + hash
                    + " is before end of array hash " + mHashes[index-1]
                    + " at index " + index + " key " + key, e);
            put(key, value);
            return;
        }
        //将 key-value 直接插入到数组尾部
        mSize = index+1;
        mHashes[index] = hash;
        index <<= 1;
        mArray[index] = key;
        mArray[index+1] = value;
    }
```

3、获取元素
获取元素的方法主要看 indexOf(Object key, int hash)方法即可，只要理解了该方法是如何获取 keyIndex 的，那么就能够对 ArrayMap 的
存储结构有更明确的认知
indexOf 方法用于获取和 key，hash 均能对应上的元素的哈希值在 mHashes 中的索引位置。我们知道，keyHash 是存储在 mHashes 中的，
  而 key-value 又是存储在 mArray 中的，但我们无法只根据 keyHash 就准确对应上 key-value，因为不同的 key 有可能有相同的 hash 值，
  即需要考虑哈希冲突的情况，所以 indexOf 方法除了需要对比 hash 值大小是否相等外还需要对比 key 的相等性
1 通过二分查找法获取到 mHashes 中和 hash 相等的值的索引 index
2 如果 index 小于 0，说明不存在该 key，那么就返回 index，~index 就是 hash 插入 mHashes 后的位置索引。结束流程
3 index 大于等于 0，说明 key 有可能存在，之所以说可能，因为存在 key 不同但 hash 值相等的情况
4 判断 mArray 中 index<<1 位置的元素是否和 key 相等，如果相等说明已经找到了目标位置，返回 index。结束流程
5 此时可以确定发生了哈希冲突，那么就需要对 mArray 进行相等性对比了，而之所以要分为两个 for 循环也是为了减少遍历次数，因为相同 hash 值
  是会靠拢在一起的，所以分别向两侧进行遍历查找。如果 key 和 keyHash 的相等性均校验通过，那么就返回对应的索引。结束流程
6 会执行到这里，说明还是没有找到和 key 相等的元素值，那么就拿到 hash 应该存入 mHashes 后的索引，~ 运算后返回
  此时的返回值就是哈希冲突后，value应该放入的位置

```
     public V get(Object key) {
        final int index = indexOfKey(key);
        return index >= 0 ? (V)mArray[(index<<1)+1] : null;
    }
    public int indexOfKey(Object key) {
        return key == null ? indexOfNull()
                : indexOf(key, mIdentityHashCode ? System.identityHashCode(key) : key.hashCode());
    }
    int indexOf(Object key, int hash) {
        final int N = mSize;
        if (N == 0) {
            return ~0;
       	}
      	//第一步
        int index = binarySearchHashes(mHashes, N, hash);

        //第二步
        if (index < 0) {
            return index;
        }

      	//第四步
        if (key.equals(mArray[index<<1])) {
            return index;
        }

        //第五步
        // Search for a matching key after the index.
        int end;
        //假设第一次出现哈希冲突  mHashes[end] ！= hash    mHashes[i] != hash
        // 此时end=index + 1,元素应该在第一次出现hash的后面
        for (end = index + 1; end < N && mHashes[end] == hash; end++) {
            if (key.equals(mArray[end << 1])) return end;
        }
        // Search for a matching key before the index.
        for (int i = index - 1; i >= 0 && mHashes[i] == hash; i--) {
            if (key.equals(mArray[i << 1])) return i;
        }
        
      	//第六步
        return ~end;
    }
    //查看SparseArray.md
    private static int binarySearchHashes(int[] hashes, int N, int hash) {
        try {
            return ContainerHelpers.binarySearch(hashes, N, hash);
        } catch (ArrayIndexOutOfBoundsException e) {
            if (CONCURRENT_MODIFICATION_EXCEPTIONS) {
                throw new ConcurrentModificationException();
            } else {
                throw e; // the cache is poisoned at this point, there's not much we can do
            }
        }
    }
```

4、缓存机制
ArrayMap 内部包含了对 mHashes 和 mArray 这两个数组进行缓存的机制，避免由于频繁创建数组而造成内存抖动，这一点还是比较有意义的。
在 Android 系统中 Bundle 是使用得很频繁的一个类，其内部就通过 ArrayMap 来存储键值对，这可以从 Bundle 的父类 BaseBundle 看到。
所以 ArrayMap 的数组缓存机制在我看来更多的是面对系统运行时的优化措施
```public class BaseBundle {
    
    @UnsupportedAppUsage
    ArrayMap<String, Object> mMap = null;
    
    public void putBoolean(@Nullable String key, boolean value) {
        unparcel();
        mMap.put(key, value);
    }

    void putByte(@Nullable String key, byte value) {
        unparcel();
        mMap.put(key, value);
    }

    void putChar(@Nullable String key, char value) {
        unparcel();
        mMap.put(key, value);
    }
    
    ···
    
}
```

Put 方法内部就使用到了数组的缓存和复用机制
```
    @Override
    public V put(K key, V value) {
        ···
        if (osize >= mHashes.length) {
           ..
          	//尝试通过数组复用机制来初始化 mHashes 和 mArray
            allocArrays(n);
            ...
			//尝试回收 ohashes 和 oarray
            freeArrays(ohashes, oarray, osize);
        }
        ···
        return null;
    }

```
缓存数组
ArrayMap是专为Android优化而设计的Map对象，使用场景比较高频，很多场景可能起初都是数据很少，为了减少频繁地创建和回收，特意设计了两个缓存池，
分别缓存大小为4和8的ArrayMap对象。要理解缓存机制，那就需要看看内存分配(allocArrays)和内存释放(freeArrays)

实现数组缓存逻辑对应的是 freeArrays 方法，该方法就用于缓存 mHashes 和 mArray。每当 ArrayMap 完成数组扩容后就会调用此方法
  对扩容前的数组进行缓存，但也不是所有数组都会进行缓存，而是有着数组长度和缓存总数这两方面的限制

首先，ArrayMap 包含了多个全局的静态变量和静态常量用于控制及实现数组缓存。从freeArrays方法可以看出来，if 和 else 语句块的逻辑
  是基本完全一样的，其区别只在于触发缓存的条件和使用的缓存池不一样

例如，如果 hashes 的数组长度是 BASE_SIZE * 2，且当前缓存总数没有超出 CACHE_SIZE=10，那么缓存的数组就保存在 mTwiceBaseCache 
```
//用于缓存长度为 BASE_SIZE 的数组
    static Object[] mBaseCache;
    //mBaseCache 已缓存的数组个数，可能存在多个缓存的数组
    static int mBaseCacheSize;
		
    //用于缓存长度为 BASE_SIZE * 2 的数组
    static Object[] mTwiceBaseCache;
    //mTwiceBaseCache 已缓存的数组个数
    static int mTwiceBaseCacheSize;

    private static final int BASE_SIZE = 4;
		
    //mBaseCacheSize 和 mTwiceBaseCacheSize 的最大缓存个数
    private static final int CACHE_SIZE = 10;

    //用来当做同步锁
    private static final Object sBaseCacheLock = new Object();
    private static final Object sTwiceBaseCacheLock = new Object();  
    
    //缓存 hashes 和 array
    private static void freeArrays(final int[] hashes, final Object[] array, final int size) {
        //base_size=4
        if (hashes.length == (BASE_SIZE*2)) {
            synchronized (sTwiceBaseCacheLock) {
                //cache_size=10
                if (mTwiceBaseCacheSize < CACHE_SIZE) {
                      //第一个元素指向 mTwiceBaseCache   //array[0]指向原来的缓存池
                    array[0] = mTwiceBaseCache;
                      //第二个元素指向 hashes
                    array[1] = hashes;
                    for (int i=(size<<1)-1; i>=2; i--) {
                      	//切除多余引用，避免内存泄漏，有利于 GC
                        array[i] = null;
                    }
                      //mTwiceBaseCache指向新加入缓存池的array
                    mTwiceBaseCache = array;
                    mTwiceBaseCacheSize++;
                    if (DEBUG) Log.d(TAG, "Storing 2x cache " + array
                            + " now have " + mTwiceBaseCacheSize + " entries");
                }
            }
        } else if (hashes.length == BASE_SIZE) {
            synchronized (sBaseCacheLock) {
                if (mBaseCacheSize < CACHE_SIZE) {
                    array[0] = mBaseCache;
                    array[1] = hashes;
                    for (int i=(size<<1)-1; i>=2; i--) {
                        array[i] = null;
                    }
                    mBaseCache = array;
                    mBaseCacheSize++;
                    if (DEBUG) Log.d(TAG, "Storing 1x cache " + array
                            + " now have " + mBaseCacheSize + " entries");
                }
            }
        }
    }

```
最初mTwiceBaseCache和mBaseCache缓存池中都没有数据，在freeArrays释放内存时，如果同时满足释放的array大小等于4或者8，
且相对应的缓冲池个数未达上限，则会把该array加入到缓存池中。
加入的方式是将数组array的第0个元素指向原有的缓存池，第1个元素指向hashes数组的地址，第2个元素以后的数据全部置为null。
   再把缓存池的头部指向最新的array的位置，并将该缓存池大小执行加1操作
android_ArrayMap_缓存回收.jpg      
freeArrays()触发时机:
1 当执行removeAt()移除最后一个元素的情况
2 当执行clear()清理的情况
3 当执行ensureCapacity()在当前容量小于预期容量的情况下, 先执行allocArrays,再执行freeArrays
4 当执行put()在容量满的情况下, 先执行allocArrays, 再执行freeArrays

复用数组    todo 缓存池的示意图    这个流程的正确性
缓存数组的目的自然就是为了后续复用，数组的复用逻辑对应的是 allocArrays 方法，该方法用于为 mHashes 和 mArray 申请一个
  更大容量的数组空间，通过复用数组或者全新初始化来获得
在进行数组缓存的时候会判断数组长度，只有当长度是 BASE_SIZE * 2 或 BASE_SIZE 时才会进行缓存，那么自然只有当数组的目标长度 size 
 是这两个值之一才符合复用条件了。如果不符合复用条件或者复用失败，那么就还是需要重新构建两个新的数组对象
```    
    private void allocArrays(final int size) {
        if (mHashes == EMPTY_IMMUTABLE_INTS) {
            throw new UnsupportedOperationException("ArrayMap is immutable");
        }
        //base_size=4
        if (size == (BASE_SIZE*2)) {
            synchronized (sTwiceBaseCacheLock) {
                if (mTwiceBaseCache != null) {  // 当缓存池不为空时
                    final Object[] array = mTwiceBaseCache;
                    mArray = array;   //从缓存池中取出mArray
                    try {
                      	//将缓存池指向上一条缓存地址
                        mTwiceBaseCache = (Object[]) array[0];  
                        //从缓存中mHashes
                        mHashes = (int[]) array[1];
                        if (mHashes != null) {
                          	//符合复用条件，切除多余引用
                            array[0] = array[1] = null;
                            mTwiceBaseCacheSize--;
                            if (DEBUG) {
                                Log.d(TAG, "Retrieving 2x cache " + mHashes
                                        + " now have " + mTwiceBaseCacheSize + " entries");
                            }
                            return;
                        }
                    } catch (ClassCastException e) {
                    }
                    // Whoops!  Someone trampled the array (probably due to not protecting
                    // their access with a lock).  Our cache is corrupt; report and give up.
                    Slog.wtf(TAG, "Found corrupt ArrayMap cache: [0]=" + array[0]
                            + " [1]=" + array[1]);
                  	//会执行到这里，说明缓存机制发现问题，则弃用之前的所有缓存
                    mTwiceBaseCache = null;
                    mTwiceBaseCacheSize = 0;
                }
            }
        } else if (size == BASE_SIZE) {
            synchronized (sBaseCacheLock) {
                if (mBaseCache != null) {
                    final Object[] array = mBaseCache;
                    mArray = array;
                    try {
                        mBaseCache = (Object[]) array[0];
                        mHashes = (int[]) array[1];
                        if (mHashes != null) {
                            array[0] = array[1] = null;
                            mBaseCacheSize--;
                            if (DEBUG) {
                                Log.d(TAG, "Retrieving 1x cache " + mHashes
                                        + " now have " + mBaseCacheSize + " entries");
                            }
                            return;
                        }
                    } catch (ClassCastException e) {
                    }
                    // Whoops!  Someone trampled the array (probably due to not protecting
                    // their access with a lock).  Our cache is corrupt; report and give up.
                    Slog.wtf(TAG, "Found corrupt ArrayMap cache: [0]=" + array[0]
                            + " [1]=" + array[1]);
                    mBaseCache = null;
                    mBaseCacheSize = 0;
                }
            }
        }
        // 分配大小除了4和8之外的情况，则直接创建新的数组
        mHashes = new int[size];
        mArray = new Object[size<<1];
    }
```
当allocArrays分配内存时，如果所需要分配的大小等于4或者8，且相对应的缓冲池不为空，则会从相应缓存池中取出缓存的mArray和mHashes。
从缓存池取出缓存的方式是将当前缓存池赋值给mArray，将缓存池指向上一条缓存地址，将缓存池的第1个元素赋值为mHashes，
 再把mArray的第0和第1个位置的数据置为null，并将该缓存池大小执行减1操作
android_ArrayMap_缓存复用.jpg
allocArrays触发时机:
当执行ArrayMap的构造函数的情况
当执行removeAt()在满足容量收紧机制的情况
当执行ensureCapacity()在当前容量小于预期容量的情况下, 先执行allocArrays,再执行freeArrays
当执行put()在容量满的情况下, 先执行allocArrays, 再执行freeArrays

总结
上文说了，只有长度为 BASE_SIZE 或者 BASE_SIZE * 2 的数组才会被缓存复用，而 mHashes 和 mArray 的扩容操作也会尽量使得
  扩容后的数组长度就是这两个值之一，这可以从 put 方法计算扩容后容量的算法看出来
http://gityuan.com/2019/01/13/arraymap/
这里需要注意的是只有大小为4或者8的内存分配才有可能从缓存池取数据，因为freeArrays过程放入缓存池的大小只有4或8，对于其他大小的内存分配
  则需要创建新的数组。 优化小技巧，对于分配数据不超过8的对象的情况下，一定要创建4或者8大小，否则浪费了缓存机制。比如ArrayMap[7]就是不友好的写法，
  建议写成ArrayMap[8]
```    @Override
    public V put(K key, V value) {
        final int osize = mSize;
        final int hash;
        ···
        if (osize >= mHashes.length) {
          	//计算数组扩容后的大小
            final int n = osize >= (BASE_SIZE*2) ? (osize+(osize>>1))
                    : (osize >= BASE_SIZE ? (BASE_SIZE*2) : BASE_SIZE);

            if (DEBUG) Log.d(TAG, "put: grow from " + mHashes.length + " to " + n);

            final int[] ohashes = mHashes;
            final Object[] oarray = mArray;
          
            allocArrays(n);
          
            ···
              
            freeArrays(ohashes, oarray, osize);
        }
        ···
        return null;
    }
```
所以说，虽然 ArrayMap 的构造函数中并没有直接将 BASE_SIZE 作为数组的默认长度，但是在扩容过程中会尽量往 BASE_SIZE 和 
  BASE_SIZE * 2 这两个值靠拢，这就有利于尽量实现数组复用
此外，ArrayMap 的扩容操作在申请内存时也显得比较克制，在数组长度超出 BASE_SIZE * 2  后，只是扩容到当前的 1.5 倍，
  且也只在 mHashes 容量不足时才会触发扩容机制。而 HashMap 在达到负载因子设定的比例后（此时数组未满）就会触发扩容机制，
  而且也是按照扩充到两倍容量的方式进行扩容。所以说，ArrayMap 对于内存空间的利用效率会更高一些




5、优劣势总结
ArrayMap 的适用场景可以从它的缓存机制就看出来一些，它会缓存容量为 4 或者 8 的数组并进行后续复用，而这两个值可以说都是比较小的。
   Android 系统对于内存比较敏感，需要存储键值对时面对的往往是使用频率高但数据量小的场景。例如我们在跳转到 Activity 时往往是通过
   Bundle 来存储跳转参数，但数据量一般都很少，所以 Bundle 内部就使用到了 ArrayMap 来存储键值对。ArrayMap 在内存申请时
   相比 HashMap 会比较克制，键值对会以更加紧密的数据结构存储在一起，对内存利用率会更高一些
而相对的，ArrayMap 的这种存储结构也导致了其查找效率相比 HashMap 要低很多。在数据量大时，ArrayMap 可能需要通过多次二分查找
    才能定位到元素，而 HashMap 在没有哈希冲突的情况下只需要经过一次哈希计算即可定位到元素，即使有哈希冲突也只需要遍历发生冲突的部分元素即可
所以说， ArrayMap 适用于数据量较小的场景，此时查找效率也不会受多大影响，而内存利用率能够显著提高。如果数据量较大，那就可以考虑
  使用 HashMap 来替代了


6、关联类
系统还包含了一个用于存储不重复元素值的集合框架类：ArraySet，从名字就可以猜到 ArraySet 实现了 Set 接口。ArraySet 内部一样
  使用两个数组来存储 hash 和 value，即 mHashes 和 mArray，在实现逻辑上基本和 ArrayMap 一样，只是会在存值的时候判断 value 是否重复而已，
  这里就不再赘述了

http://gityuan.com/2019/01/13/arraymap/
元素删除
```
 public V removeAt(int index) {
        if (index >= mSize && UtilConfig.sThrowExceptionForUpperArrayOutOfBounds) {
            throw new ArrayIndexOutOfBoundsException(index);
        }

        final Object old = mArray[(index << 1) + 1];
        final int osize = mSize;
        final int nsize;
        if (osize <= 1) {
            //ArrayMap只多有一个元素，则释放内存
            final int[] ohashes = mHashes;
            final Object[] oarray = mArray;
            mHashes = EmptyArray.INT;
            mArray = EmptyArray.OBJECT;
            freeArrays(ohashes, oarray, osize);
            nsize = 0;
        } else {
            nsize = osize - 1;
            if (mHashes.length > (BASE_SIZE*2) && mSize < mHashes.length/3) {
                 //根据情况来收紧容量
                 //当数组内存的大小大于8，且已存储数据的个数mSize小于数组空间大小的1/3的情况下，需要收紧数据的内容容量，分配新的数组，
                 // 老的内存靠虚拟机自动回收。
                 // 如果mSize<=8，则设置新大小为8；
                // 如果mSize> 8，则设置新大小为mSize的1.5倍。
                //也就是说在数据较大的情况下，当内存使用量不足1/3的情况下，内存数组会收紧50%   假设mHashes.length=12，size=3,则n=5,内存收紧
                final int n = osize > (BASE_SIZE*2) ? (osize + (osize>>1)) : (BASE_SIZE*2);

                final int[] ohashes = mHashes;
                final Object[] oarray = mArray;
                ///分配一个更下容量的内容
                allocArrays(n);
                //禁止并发
                if (CONCURRENT_MODIFICATION_EXCEPTIONS && osize != mSize) {
                    throw new ConcurrentModificationException();
                }
                //元素搬移
                if (index > 0) {
                    if (DEBUG) Log.d(TAG, "remove: copy from 0-" + index + " to 0");
                    System.arraycopy(ohashes, 0, mHashes, 0, index);
                    System.arraycopy(oarray, 0, mArray, 0, index << 1);
                }
                if (index < nsize) {
                    if (DEBUG) Log.d(TAG, "remove: copy from " + (index+1) + "-" + nsize
                            + " to " + index);
                    System.arraycopy(ohashes, index + 1, mHashes, index, nsize - index);
                    System.arraycopy(oarray, (index + 1) << 1, mArray, index << 1,
                            (nsize - index) << 1);
                }
            } else {
                //当被移除的元素不是数组最末尾的元素时，则需要将后面的数组往前移动
                if (index < nsize) {
                    System.arraycopy(mHashes, index + 1, mHashes, index, nsize - index);
                    System.arraycopy(mArray, (index + 1) << 1, mArray, index << 1,
                            (nsize - index) << 1);
                }
                //再将最后一个位置设置为null
                mArray[nsize << 1] = null;
                mArray[(nsize << 1) + 1] = null;
            }
        }
        if (CONCURRENT_MODIFICATION_EXCEPTIONS && osize != mSize) {
            throw new ConcurrentModificationException();
        }
        mSize = nsize;
        return (V)old;
    }
```
remove()过程：通过二分查找key的index，再根据index来选择移除动作；当被移除的是ArrayMap的最后一个元素，则释放该内存，
  否则只做移除操作，这时会根据容量收紧原则来决定是否要收紧，当需要收紧时会创建一个更小内存的容量

元素清除
```
//清空数据并执行freeArrays()方法来回收内存
public void clear() {
       //当容量中元素不为空的情况 才会执行内存回收操作
        if (mSize > 0) {
            final int[] ohashes = mHashes;
            final Object[] oarray = mArray;
            final int osize = mSize;
            mHashes = EmptyArray.INT;
            mArray = EmptyArray.OBJECT;
            mSize = 0;
            freeArrays(ohashes, oarray, osize);
        }
        if (CONCURRENT_MODIFICATION_EXCEPTIONS && mSize > 0) {
            throw new ConcurrentModificationException();
        }
    }
 //只会清空数组内的数据，并不会回收内存。  
 public void erase() {
        if (mSize > 0) {
            final int N = mSize<<1;
            final Object[] array = mArray;
            for (int i=0; i<N; i++) {
                array[i] = null;
            }
            mSize = 0;
        }
    }    
```
总结
从以下几个角度总结一下：
数据结构
  ArrayMap和SparseArray采用的都是两个数组，Android专门针对内存优化而设计的    //SparseArray避免装箱拆箱
  HashMap采用的是数组+链表+红黑树
内存优化
  ArrayMap比HashMap更节省内存，综合性能方面在数据量不大的情况下，推荐使用ArrayMap；
  Hash需要创建一个额外对象来保存每一个放入map的entry，且容量的利用率比ArrayMap低，整体更消耗内存
  SparseArray比ArrayMap节省1/3的内存，但SparseArray只能用于key为int类型的Map，所以int类型的Map数据推荐使用SparseArray；
      SparseArray不需要保存key所对应的哈希值
性能方面：
  ArrayMap查找时间复杂度O(logN)；ArrayMap增加、删除操作需要移动成员，速度相比较慢，对于个数小于1000的情况下，性能基本没有明显差异
  HashMap查找、修改的时间复杂度为O(1)；   //hashMap不需要移动数据
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