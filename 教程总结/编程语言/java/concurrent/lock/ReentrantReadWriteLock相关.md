
https://mp.weixin.qq.com/s?__biz=MzkxNDEyOTI0OQ==&mid=2247484419&idx=1&sn=db47222364e2ae0c1ed135482aa361e5&source=41#wechat_redirect

总结：
1.ReentrantReadWriteLock默认非公平锁


问题
（1）读写锁是什么？

（2）读写锁具有哪些特性？

（3）ReentrantReadWriteLock是怎么实现读写锁的？

（4）如何使用ReentrantReadWriteLock实现高效安全的TreeMap？

简介
读写锁是一种特殊的锁，它把对共享资源的访问分为读访问和写访问，多个线程可以同时对共享资源进行读访问，但是同一时间只能有一个线程对共享资源进行写访问，
使用读写锁可以极大地提高并发量。    

特性
读写锁具有以下特性：
是否互斥	 读	 写
读	     否	 是
写	     是	 是


类结构
在看源码之前，我们还是先来看一下ReentrantReadWriteLock这个类的主要结构

ReentrantReadWriteLock中的类分成三个部分：

（1）ReentrantReadWriteLock本身实现了ReadWriteLock接口，这个接口只提供了两个方法 readLock()和 writeLock（）；
```
public interface ReadWriteLock {
    Lock readLock();
    Lock writeLock();
}
```

（2）同步器，包含一个继承了AQS的Sync内部类，以及其两个子类FairSync和NonfairSync；

（3）ReadLock和WriteLock两个内部类实现了Lock接口，它们具有锁的一些特性
```
public interface Lock {
    void lock();
    void lockInterruptibly() throws InterruptedException;
    boolean tryLock();
    boolean tryLock(long time, TimeUnit unit) throws InterruptedException;
    void unlock();
    Condition newCondition();
}
```

源码分析
主要属性
```
private final ReentrantReadWriteLock.ReadLock readerLock;
private final ReentrantReadWriteLock.WriteLock writerLock;
final Sync sync;
```
维护了读锁、写锁和同步器

主要构造方法
```
 public ReentrantReadWriteLock() {
        this(false);
    }
    public ReentrantReadWriteLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
        readerLock = new ReadLock(this);
        writerLock = new WriteLock(this);
    }
```
它提供了两个构造方法，默认构造方法使用的是非公平锁模式，在构造方法中初始化了读锁和写锁。

获取读锁和写锁的方法
```
public ReentrantReadWriteLock.WriteLock writeLock() { return writerLock; }
public ReentrantReadWriteLock.ReadLock  readLock()  { return readerLock; }
```
属性中的读锁和写锁是私有属性，通过这两个方法暴露出去。

下面我们主要分析读锁和写锁的加锁、解锁方法，且都是基于非公平模式的。

ReadLock.lock()
```
public void lock() {
            sync.acquireShared(1);
        }
//AbstractQueuedSynchronizer.java        
 public final void acquireShared(int arg) {
        //尝试获取共享锁 返回1成功，返回-1失败
        if (tryAcquireShared(arg) < 0)
            doAcquireShared(arg);
    }    
 
//ReentrantReadWriteLock.java 
protected final int tryAcquireShared(int unused) {
            Thread current = Thread.currentThread();
            // 状态变量的值
            // 在读写锁模式下，高16位存储的是共享锁（读锁）被获取的次数，低16位存储的是互斥锁（写锁）被获取的次数  todo
            int c = getState();
            // 互斥锁的次数
            // 如果其它线程获得了写锁，直接返回-1
            if (exclusiveCount(c) != 0 &&
                getExclusiveOwnerThread() != current)
                return -1;
            // 读锁被获取的次数    
            int r = sharedCount(c);
            
            // 下面说明此时还没有写锁，尝试去更新state的值获取读锁
            // 读者是否需要排队（是否是公平模式）   readerShouldBlock todo
            if (!readerShouldBlock() &&
                r < MAX_COUNT &&
                compareAndSetState(c, c + SHARED_UNIT)) {
                // 获取读锁成功
                if (r == 0) {
                     // 如果之前还没有线程获取读锁
                     // 记录第一个读者为当前线程
                    firstReader = current;
                    // 第一个读者重入的次数为1
                    firstReaderHoldCount = 1;
                } else if (firstReader == current) {
                    // 如果有线程获取了读锁且是当前线程是第一个读者    
                    // 则把其重入次数加1
                    firstReaderHoldCount++;
                } else {
                    HoldCounter rh = cachedHoldCounter;
                    if (rh == null || rh.tid != getThreadId(current))
                        cachedHoldCounter = rh = readHolds.get();
                    else if (rh.count == 0)
                        readHolds.set(rh);
                    rh.count++;
                }
                return 1;
            }
            return fullTryAcquireShared(current);
        }        
```
//todo