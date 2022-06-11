https://blog.csdn.net/tangtong1/article/details/90745712  jdk1.8
问题
（1）条件锁是什么？

（2）条件锁适用于什么场景？

（3）条件锁的await()是在其它线程signal()的时候唤醒的吗？

简介
条件锁，是指在获取锁之后发现当前业务场景自己无法处理，而需要等待某个条件的出现才可以继续处理时使用的一种锁。

比如，在阻塞队列中，当队列中没有元素的时候是无法弹出一个元素的，这时候就需要阻塞在条件notEmpty上，等待其它线程往里面放入一个元素后，
   唤醒这个条件notEmpty，当前线程才可以继续去做“弹出一个元素”的行为。

注意，这里的条件，必须是在获取锁之后去等待，对应到ReentrantLock的条件锁，就是获取锁之后才能调用condition.await()方法。

在java中，条件锁的实现都在AQS的ConditionObject类中，ConditionObject实现了Condition接口，下面我们通过一个例子来进入到条件锁的学习中。

使用示例
```
public class ReentrantLockTest {
    public static void main(String[] args) throws InterruptedException {
        // 声明一个重入锁
        ReentrantLock lock = new ReentrantLock();
        // 声明一个条件锁
        Condition condition = lock.newCondition();

        new Thread(()->{
            try {
                lock.lock();  // 1
                try {
                    System.out.println("before await");  // 2
                    // 等待条件
                    condition.await();  // 3
                    System.out.println("after await");  // 10
                } finally {
                    lock.unlock();  // 11
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
        
        // 这里睡1000ms是为了让上面的线程先获取到锁
        Thread.sleep(1000);
        lock.lock();  // 4
        try {
            // 这里睡2000ms代表这个线程执行业务需要的时间
            Thread.sleep(2000);  // 5
            System.out.println("before signal");  // 6
            // 通知条件已成立
            condition.signal();  // 7
            System.out.println("after signal");  // 8
        } finally {
            lock.unlock();  // 9
        }
    }
}
```
结果
```
before await
before signal
after signal
after await
```

//todo 后面的原理
