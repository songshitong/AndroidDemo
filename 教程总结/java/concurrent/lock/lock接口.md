```
public interface Lock {
    // 获取锁  获取不到会阻塞
    void lock();
    // 获取可中断锁，即在拿锁过程中可中断，synchronized是不可中断锁。
    void lockInterruptibly() throws InterruptedException;
    // 尝试获取锁，成功返回true，失败返回false
    boolean tryLock();
    // 在给定时间内尝试获取锁，成功返回true，失败返回false
    boolean tryLock(long time, TimeUnit unit) throws InterruptedException;
    // 释放锁
    void unlock();
    // 等待与唤醒机制
    Condition newCondition();
}

```