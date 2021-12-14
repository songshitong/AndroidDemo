jdk 1.8   android sdk 30
https://juejin.cn/post/6975435256111300621#heading-2
可重入锁  内部通过AQS实现
1.ReentrantLock使用
上篇文章介绍的synchronized关键字是一种隐式锁，即它的加锁与释放是自动的，无需我们关心。而ReentrantLock是一种显式锁，
需要我们手动编写加锁和释放锁的代码。下面我们来看下ReentrantLock的使用方法。
```
public class ReentrantLockDemo {
    // 实例化一个非公平锁，构造方法的参数为true表示公平锁，false为非公平锁。
    private final ReentrantLock lock = new ReentrantLock(false);
    private int i;

    public void testLock() {
        // 拿锁，如果拿不到会一直等待
        lock.lock();
        try {
            // 再次尝试拿锁(可重入)，拿锁最多等待100毫秒
            if (lock.tryLock(100, TimeUnit.MILLISECONDS))
                i++;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            // 释放锁
            lock.unlock(); 
            lock.unlock();
        }
    }
}
```

2 源码解析
ReentrantLock类的代码结构
```
public class ReentrantLock implements Lock, java.io.Serializable {

    private final Sync sync;
    
    public ReentrantLock() {
        sync = new NonfairSync();
    }

    public ReentrantLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
    }
    
    // ...省略其它代码
}
```
可以看到，ReentrantLock的代码结构非常简单。它实现了Lock和Serializable两个接口，同时有两个构造方法，在无参构造方法中初始化了一个非公平锁，
  在有参构造方法中根据参数决定是初始化公平锁还是非公平锁

既然ReentrantLock实现Lock接口，它一定也实现了它的方法，看下ReentrantLock中的实现：
```
public class ReentrantLock implements Lock, java.io.Serializable {

    private final Sync sync;
    
    public void lock() {
        sync.acquire(1);
    }
    
    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }    
    
    public boolean tryLock() {
        return sync.nonfairTryAcquire(1);
    }
    
    public boolean tryLock(long timeout, TimeUnit unit)
            throws InterruptedException {
        return sync.tryAcquireNanos(1, unit.toNanos(timeout));
    }
    
    public void unlock() {
        sync.release(1);
    }
    
    // ...省略其它代码
}
```

可以看到ReentrantLock中对这几个方法的实现非常简单。都是调用了Sync中的相关方法。可见ReentrantLock所有拿锁和释放锁的操作都是通过Sync这个成员变量来实现的。
Sync是ReentrantLock中的一个抽象内部类，它的源码如下：
```
    abstract static class Sync extends AbstractQueuedSynchronizer {

        // 尝试获取非公平锁
        final boolean nonfairTryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            int c = getState();
            // 未上锁状态
            if (c == 0) {
                // 通过CAS尝试拿锁
                if (compareAndSetState(0, acquires)) {                    
                    // 设置持有排他锁的线程
                    setExclusiveOwnerThread(current);
                    return true;
                }
            }
            // 如果是已上锁状态，判断持有锁的线程是不是自己，这里即可重入锁的实现
            else if (current == getExclusiveOwnerThread()) {
                int nextc = c + acquires;
                if (nextc < 0) // overflow
                    throw new Error("Maximum lock count exceeded");
                setState(nextc);
                return true;
            }
            return false;
        }
        // 释放锁
        protected final boolean tryRelease(int releases) {
            int c = getState() - releases;
            if (Thread.currentThread() != getExclusiveOwnerThread())
                throw new IllegalMonitorStateException();
            boolean free = false;
            if (c == 0) {
                free = true;
                setExclusiveOwnerThread(null);
            }
            setState(c);
            return free;
        }
        //当前线程是否持有独占锁
        protected final boolean isHeldExclusively() {
            return getExclusiveOwnerThread() == Thread.currentThread();
        }
        // 获取持有锁的线程
        final Thread getOwner() {
            return getState() == 0 ? null : getExclusiveOwnerThread();
        }
        // 获取持有锁线程重入的次数
        final int getHoldCount() {
            return isHeldExclusively() ? getState() : 0;
        }
        // 是否上锁状态
        final boolean isLocked() {
            return getState() != 0;
        }

        // ...省略其他代码
    }
```
可以看到Sync中的代码逻辑也并不复杂。通过nonfairTryAcquire方法实现非公平锁的拿锁操作，tryRelease则实现了释放锁的操作。
其它还有几个与锁状态相关的方法。细心的同学会发现Sync对锁状态的判断都是通过state来实现的，state为0表示未加锁状态，
state大于0表示加锁状态

另外，由于Sync是一个抽象类，那必然有继承它的类。在ReentrantLock中有两个Sync的实现，分别为NonfairSync与FairSync。
从名字上可以看到一个是非公平锁，一个是非公平锁。
首先来看下NonfairSync非公平锁的实现：
```
    static final class NonfairSync extends Sync {
        private static final long serialVersionUID = 7316153563782823691L;
        protected final boolean tryAcquire(int acquires) {
            return nonfairTryAcquire(acquires);
        }
        final void lock() {
            if (compareAndSetState(0, 1))
                setExclusiveOwnerThread(Thread.currentThread());
            else
                acquire(1);
        }
    }
```
上边我们也提到了Sync中已经实现了非公平锁的逻辑了，所以NonfairSync的代码非常简单，仅仅在tryAcquire中直接调用了nonfairTryAcquire。
fairfair  [feə(r)]   公平的;合理的;恰当的;适当的;(按法律、规定)平等待人的，秉公办事的，公正的;(数量、大小)相当大的
FairSync公平锁的代码如下：
```
static final class FairSync extends Sync {
   final void lock() {
            acquire(1);
    }
    protected final boolean tryAcquire(int acquires) {
        final Thread current = Thread.currentThread();
        int c = getState();
        // 未上锁状态
        if (c == 0) {
            // 首先判断没有等待节点时才会开启CAS去拿锁
            if (!hasQueuedPredecessors() &&
                compareAndSetState(0, acquires)) {
                // 设置持有排他锁的线程
                setExclusiveOwnerThread(current);
                return true;
            }
        }
        // 与非公平锁一样，持有锁线程是自己，则可重入
        else if (current == getExclusiveOwnerThread()) {
            int nextc = c + acquires;
            if (nextc < 0)
                throw new Error("Maximum lock count exceeded");
            setState(nextc);
            return true;
        }
        return false;
    }
}
```

可以看到在tryAcquire中实现了公平锁的操作，这段代码与与非公平锁的实现其实只有一句之差。
即公平锁先去判断了同步队列中是否有在等待的线程，如果没有才会去进行拿锁操作。而非公平锁不会管是否有同步队列，先去拿了再说

ReentrantLock独占锁拿锁和排队的流程：ReentrantLock内部通过FairSync和NonfairSync来实现公平锁和非公平锁。它们都是继承自AQS实现，
在AQS内部通过state来标记同步状态，如果state为0，线程可以直接获取锁，如果state大于0，则线程会被封装成Node节点进入CLH队列并阻塞线程。
AQS的CLH队列是一个双向的链表结构，头结点是一个空的Node节点。新来的node节点会被插入队尾并开启自旋去判断它的前驱节点是不是头结点。
如果是头结点则尝试获取锁，如果不是头结点，则根据条件进行挂起操作

