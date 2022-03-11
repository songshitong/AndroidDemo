
对象池的用法  MyPooledClass是带有对象复用的类
```
public class MyPooledClass {
       //线程安全的对象池
       private static final SynchronizedPool  sPool =
               new SynchronizedPool (10);
       //获取一个对象，如果对象池为空，新建一个对象
       public static MyPooledClass obtain() {
           MyPooledClass instance = sPool.acquire();
           return (instance != null) ? instance : new MyPooledClass();
       }
       //对象复用到池子里面
       public void recycle() {
            // Clear state if needed.
            sPool.release(this);
       }
  
       . . .
   }
```

对象池原理
先看对象池接口:
  1.从对象池获取对象的方法
  2.回收复用对象的方法
```
 public interface Pool<T> {
        //注意： 获取对象的方法可能为空
        @Nullable
        T acquire();
 
        boolean release(@NonNull T instance);
    }

```

androidx 提供了两种对象池，一种是非线程安全的SimplePool，另一种是安全的SynchronizedPool

先看SimplePool
```
public final class Pools {
    public static class SimplePool<T> implements Pool<T> {
        private final Object[] mPool;
        private int mPoolSize;
        public SimplePool(int maxPoolSize) {
            if (maxPoolSize <= 0) {
                throw new IllegalArgumentException("The max pool size must be > 0");
            }
            mPool = new Object[maxPoolSize];
        }
        public T acquire() {
            if (mPoolSize > 0) {
                final int lastPooledIndex = mPoolSize - 1;
                T instance = (T) mPool[lastPooledIndex];
                mPool[lastPooledIndex] = null;
                mPoolSize--;
                return instance;
            }
            return null;
        }
        public boolean release(@NonNull T instance) {
            if (isInPool(instance)) {
                throw new IllegalStateException("Already in the pool!");
            }
            if (mPoolSize < mPool.length) {
                mPool[mPoolSize] = instance;
                mPoolSize++;
                return true;
            }
            return false;
        }

        private boolean isInPool(@NonNull T instance) {
            for (int i = 0; i < mPoolSize; i++) {
                if (mPool[i] == instance) {
                    return true;
                }
            }
            return false;
        }
    }

```
SimplePool使用数组存储所有的对象
获取对象时从可用的最后一个获取，对象池为空时返回null，时间复杂度是O(1)
回收对象时也是放到数组可用的最后一个，超出对象池最大容量时回收复用失败，返回false，回收时需要遍历判断是否在对象池内
  ,此时时间复杂度是O(n) 



再看一下线程安全的安全的SynchronizedPool
```
public final class Pools {
    public static class SynchronizedPool<T> extends SimplePool<T> {
        private final Object mLock = new Object();
        public SynchronizedPool(int maxPoolSize) {
            super(maxPoolSize);
        }

        public T acquire() {
            synchronized (mLock) {
                return super.acquire();
            }
        }

        public boolean release(@NonNull T element) {
            synchronized (mLock) {
                return super.release(element);
            }
        }
    }
}
```
SynchronizedPool就是在SimplePool的基础上增加了对象锁mLock，实现了线程并发安全