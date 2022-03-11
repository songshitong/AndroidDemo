handler源码总结

handler,MessageQueue,Looper关系
Looper创建会初始化MessageQueue   MessageQueue持有链表Message mMessages;
handler通过默认构造器持有looper，进而获取MessageQueue
```
public Handler() {
        this(null, false);
    }
    public Handler(@Nullable Callback callback, boolean async) {
       ...
        mLooper = Looper.myLooper();
        if (mLooper == null) {
            throw new RuntimeException(
                "Can't create handler inside thread " + Thread.currentThread()
                        + " that has not called Looper.prepare()");
        }
        mQueue = mLooper.mQueue;
        mCallback = callback;
        mAsynchronous = async;
    }
```

1.主线程loop启动   
/frameworks/base/core/java/android/app/ActivityThread.java
```
public class ActivityThread {
    static volatile Handler sMainThreadHandler;  // set once in main()
    public static void main(String[] args) {
        ......
        // 1. 做一些主线程消息循环的初始操作
        Looper.prepareMainLooper();       
        ......       
        // 2. 启动消息循环
        Looper.loop();
    }
}
```
Looper.prepareMainLooper  通过ThreadLocal<Looper>为主线程创建一个线程安全的looper，同时创建MessageQueue，
MessageQueue初始化nativeLooper，使用eventfd创建文件描述符并使用epoll监听
```
void Looper::rebuildEpollLocked() {
    ......
    // 2.1 创建一个 epoll 对象, 将其文件描述符保存在成员变量 mEpollFd 中 
    //EPOLL_SIZE_HINT=8 每个epoll实例默认的文件描述符个数
    mEpollFd = epoll_create(EPOLL_SIZE_HINT);
    ......
    // 2.2 使用 epoll 监听 mWakeEventFd, 用于线程唤醒
    int result = epoll_ctl(mEpollFd, EPOLL_CTL_ADD, mWakeEventFd, & eventItem);
    ......
}
```
2 消息循环的启动  Looper.loop
/frameworks/base/core/java/android/os/Looper.java
```
public static void loop() {
        // 获取 Looper 对应的消息队列
        final MessageQueue queue = me.mQueue;
        // 死循环, 不断的处理消息队列中的消息
        for (;;) {
            // 1. 获取消息队列中的消息, 取不到消息会阻塞
            Message msg = queue.next(); // might block
            if (msg == null) { 
                // 若取到的消息为 null, 这个 Looper 就结束了
                return;
            }
            ......
            try {
                // 2. 通过 Message 的 Handler 处理消息
                msg.target.dispatchMessage(msg);
            } finally {
                ......
            }
            msg.recycleUnchecked();
        }
```
取消息的过程
/frameworks/base/core/java/android/os/MessageQueue.java
```
Message next() {
        // 描述空闲事件处理者的数量, 初始值为 -1 
        int pendingIdleHandlerCount = -1; 
        // 描述当前线程没有新消息处理时, 可睡眠的时间
        int nextPollTimeoutMillis = 0;
        // 死循环, 获取可执行的 Message 对象
        for (;;) {
            ......
            // 1. 这里调用了 nativePollOnce, 则执行线程的睡眠, 时间由 nextPollTimeoutMillis 决定
            nativePollOnce(ptr, nextPollTimeoutMillis);
            // 2. 获取队列中下一条要执行的消息, 返回给 Looper
            synchronized (this) {
                final long now = SystemClock.uptimeMillis(); // 获取当前时刻
                Message prevMsg = null;
                Message msg = mMessages;
                // 2.1 若有同步屏障消息, 找寻第一个异步的消息执行
                // msg.target 为 null, 则说明队首是同步屏障消息
                if (msg != null && msg.target == null) {
                    //do while 当消息是同步消息时指向下一个，当找到异步消息时停止  
                    // 找寻第一个异步消息执行
                    do {
                        prevMsg = msg;
                        msg = msg.next;
                    } while (msg != null && !msg.isAsynchronous()); //消息不是异步即同步
                }
                // 2.2 直接找寻队首消息执行
                if (msg != null) {
                    // 2.2.1 若当前的时刻 早于 消息的执行时刻  还不到消息执行的时刻
                    if (now < msg.when) {
                        // 给 nextPollTimeoutMillis 赋值, 表示当前线程, 可睡眠的时间
                        nextPollTimeoutMillis = (int) Math.min(msg.when - now, Integer.MAX_VALUE);
                    } else {
                        // 2.2.2 若当前的时刻 不早于 消息的执行时刻, 则将该消息从队列中移除, 返回给 Looper 执行
                        mBlocked = false;// 更改标记位置
                        if (prevMsg != null) {
                            prevMsg.next = msg.next;
                        } else {
                            mMessages = msg.next;
                        }
                        msg.next = null;
                        // 将队首消息返回出去
                        return msg;
                    }
                } else {
                    //此时msg为null
                    // 2.3 若未找到可执行消息, 则给 nextPollTimeoutMillis 置为 -1
                    // 表示可以无限睡眠, 直至消息队列中有同步消息可读
                    nextPollTimeoutMillis = -1;
                }
                
                .......// 4. 收集空闲处理者
            }
        }
    }
```
若有同步屏障消息, 找寻第一个异步的消息执行
若未找到, 则找寻队首消息
   若当前的时刻 < 第一个同步的消息的执行时刻
       则更新 nextPollTimeoutMillis, 下次进行 for 循环时, 会进行睡眠操作
   若当前的时刻 >= 第一个同步的消息的执行时刻
      则将第一个同步消息的返回给 Looper 执行
若未找到可执行消息
   将 nextPollTimeoutMillis 置为 -1, 下次 for 循环时可以无限睡眠, 直至消息队列中有新的消息为止

阻塞线程的操作是
```
// system/core/libutils/Looper.cpp
int Looper::pollInner(int timeoutMillis) {
    //调用 epoll_wait 阻塞来监听 mEpollFd 中的 IO 事件, 超时机制由 timeoutMillis 决定
    //等待事件发生或者超时 在nativeWake()方法，向eventfd写入字符，则该方法会返回；
    int eventCount = epoll_wait(mEpollFd, eventItems, EPOLL_MAX_EVENTS, timeoutMillis);
}
```


发送消息的入队操作   其他线程唤醒阻塞的主线程
/frameworks/base/core/java/android/os/MessageQueue.java
```
public final class MessageQueue { 
    Message mMessages;
    boolean enqueueMessage(Message msg, long when) {
        ......
        synchronized (this) {
            ......
            msg.when = when;           // 将消息要执行的时刻写入 msg 成员变量
            Message p = mMessages;     // 获取当前队首的消息
            boolean needWake;          // 描述是否要唤醒该 MessageQueue 所绑定的线程
            // 1. 队列中无消息 | 新消息要立即执行 | 新消息要执行的时间比队首的早
            if (p == null || when == 0 || when < p.when) {
                // 1.1 将该消息放置到队首
                msg.next = p;
                mMessages = msg;
                // 1.2 若该 MessageQueue 之前为阻塞态, 则说明需要唤醒
                needWake = mBlocked;  
            } else {
                // 2. 新消息执行的时间比队首的晚
                // 2.1 判断是否需要唤醒, 队首是同步屏障, 并且该消息异步的, 则有机会唤醒队列
                needWake = mBlocked && p.target == null && msg.isAsynchronous();
                // 2.2 将该消息插入到消息队列合适的位置
                Message prev;
                for (;;) {
                    prev = p;
                    p = p.next;
                    //when小于某个节点的when，找到后插入
                    //具体方法就是通过死循环，使用快慢指针p和prev，每次向后移动一格，直到找到某个节点p的when大于我们要插入消息的when字段，
                    //则插入到p和prev之间。或者遍历到链表结束，插入到链表结尾
                    if (p == null || when < p.when) {     
                        break;
                    }
                    // 若该消息并非第一个异步消息, 则不允许唤醒
                    if (needWake && p.isAsynchronous()) {
                        needWake = false;
                    }
                }
                msg.next = p;
                prev.next = msg;
            }
            // 3. 处理该消息队列绑定线程的唤醒操作   native层通过epoll_wait阻塞了，有新的消息需要唤醒
            if (needWake) {
                nativeWake(mPtr);
            }
        }
        return true;
    }
    private native static void nativeWake(long ptr);
}
```
1 队列中无消息	新消息要立即执行	新消息要执行的时间比队首的早时
  将该消息放置到队首
  若该 MessageQueue 之前为阻塞态, 则说明需要唤醒
2新消息执行的时间比队首的晚
   判断是否需要唤醒队列
      队列前有一个同步屏障, 并且该消息异步的, 则有机会唤醒队列
  将该消息插入到消息队列合适的位置
    若该消息并非第一个异步消息, 则不允许唤醒
3通过 needWake 处理该消息队列绑定线程的唤醒操作

nativeWake 唤醒
frameworks/base/core/jni/android_os_MessageQueue.cpp
```
// system/core/libutils/Looper.cpp
void Looper::wake() {
    // 向 Looper 绑定的线程 mWakeEventFd 中写入一个新的数据   
    uint64_t inc = 1;
    ssize_t nWrite = TEMP_FAILURE_RETRY(write(mWakeEventFd, &inc, sizeof(uint64_t)));
    ....
}
```