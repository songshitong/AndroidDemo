android8.1
https://sharrychoo.github.io/blog/android-source/dc-handler#%E5%89%8D%E8%A8%80

应用进程主线程初始化的入口是在 ActivityThread.main() 中, 我们看看他是如何构建消息队列的
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
ActivityThread 中的消息循环构建过程如下
1 调用 Looper.prepareMainLooper, 做一些准备操作
2 调用 Looper.loop 真正的开启了消息循环

1 消息循环的准备
/frameworks/base/core/java/android/os/Looper.java
```
public final class Looper {
    private static Looper sMainLooper;  
    public static void prepareMainLooper() {
        // 1. 调用 prepare 方法真正执行主线程的准备操作
        prepare(false);
        synchronized (Looper.class) {
            if (sMainLooper != null) {
                throw new IllegalStateException("The main Looper has already been prepared.");
            }
            // 2. 调用了 myLooper 方法, 获取一个 Looper 对象给 sMainLooper 赋值
            sMainLooper = myLooper();
        }
    }
    static final ThreadLocal<Looper> sThreadLocal = new ThreadLocal<Looper>();
    private static void prepare(boolean quitAllowed) {
        if (sThreadLocal.get() != null) {
            throw new RuntimeException("Only one Looper may be created per thread");
        }
        // 1.1 new 了一个 Looper 对象
        // 1.2 将这个 Looper 对象写入 ThreadLocal 中
        sThreadLocal.set(new Looper(quitAllowed));
    }
    
    public static @Nullable Looper myLooper() {
        // 2.1 通过 ThreadLocal 获取这个线程中唯一的 Looper 对象
        return sThreadLocal.get();
    }
    
    final MessageQueue mQueue;
    final Thread mThread;
    private Looper(boolean quitAllowed) {
        // 创建了一个消息队列
        mQueue = new MessageQueue(quitAllowed);
        // 获取了当前的线程
        mThread = Thread.currentThread();
    }
    
}
```
可以看到 Looper.prepareMainLooper 中主要做了如下几件事情

1调用 prepare 方法真正执行主线程的准备操作
 创建了一个 Looper 对象
    创建了 MessageQueue 这个消息队列, 保存到成员变量 mQueue 中
    获取该对象创建线程, 保存到成员变量 mThread 中
 将这个 ThreadLocal 和 Looper 存入当前线程的 ThreadLocalMap 中    通过threadLocal.set方法保存在Thread的ThreadLocalMap
2调用 myLooper 方法, 从当前线程的 ThreadLocalMap 中获取 ThreadLocal 对应的 Looper 对象
3将这个 Looper 对象, 保存到静态变量 sMainLooper 中, 表示这个是当前应用进程主线程的 Looper 对象

好的, 可以看到在创建 Looper 对象的时候, 同时会创建一个 MessageQueue 对象, 将它保存到 Looper 对象的成员变量 mQueue 中, 
  因此每一个 Looper 对象都对应一个 MessageQueue 对象

我们接下来看看 MessageQueue 对象创建时, 做了哪些操作

MessageQueue 的创建
/frameworks/base/core/java/android/os/MessageQueue.java
```
public final class MessageQueue {
    private final boolean mQuitAllowed; // true 表示这个消息队列是可退出的
    private long mPtr;                  // 描述一个 Native 的句柄
    MessageQueue(boolean quitAllowed) {
        mQuitAllowed = quitAllowed;
        // 获取一个 native 句柄
        mPtr = nativeInit();
    }
    private native static long nativeInit();
}
```
native 层进行了怎样的初始化
frameworks/base/core/jni/android_os_MessageQueue.cpp
```
static jlong android_os_MessageQueue_nativeInit(JNIEnv* env, jclass clazz) {
    // 1. 创建了一个 NativeMessageQueue 对象
    NativeMessageQueue* nativeMessageQueue = new NativeMessageQueue();
    ......
    // 增加 env 对它的强引用计数
    nativeMessageQueue->incStrong(env);
    // 2. 将这个 NativeMessageQueue 对象强转成了一个句柄返回 Java 层
    return reinterpret_cast<jlong>(nativeMessageQueue);
}
class NativeMessageQueue : public MessageQueue, public LooperCallback {
public:
    NativeMessageQueue();
    ......
private:
    JNIEnv* mPollEnv;
    jobject mPollObj;
    jthrowable mExceptionObj;
};

NativeMessageQueue::NativeMessageQueue() :
        mPollEnv(NULL), mPollObj(NULL), mExceptionObj(NULL) {
    // 1.1 尝试调用 Looper.getForThread 获取一个 C++ 的 Looper 对象
    mLooper = Looper::getForThread();
    // 1.2 若当前线程没有创建过 Looper, 则走这个分支
    if (mLooper == NULL) {
        // 创建 Looper 对象
        mLooper = new Looper(false);
        // 给当前线程绑定这个 Looper 对象
        Looper::setForThread(mLooper);
    }
}
```
/system/core/libutils/Looper.cpp
```
sp<Looper> Looper::getForThread() {
      int result = pthread_once(& gTLSOnce, initTLSKey); 
      return (Looper*)pthread_getspecific(gTLSKey);
  }
  
 void Looper::setForThread(const sp<Looper>& looper) {
      sp<Looper> old = getForThread(); // also has side-effect of initializing TLS
      if (looper != NULL) {
          looper->incStrong((void*)threadDestructor);
      }
      pthread_setspecific(gTLSKey, looper.get());
      if (old != NULL) {
          old->decStrong((void*)threadDestructor);
      }
  }   
```
native端单独实现的looper功能与Java层类似，没有直接关系
Looper::getForThread()  功能类比于Java层的Looper.myLooper();
Looper::setForThread(mLooper)  功能类比于Java层的ThreadLocal.set();
好的可以看到 nativeInit 方法主要做了如下的操作
创建线程单例的 NativeMessageQueue 对象    注意是线程单例，不是全局单例，线程单例是因为looper一个线程只有一个
  因为 Java 的 MessageQueue 是线程单例的, 因此 NativeMessageQueue 对象也是线程单例的
创建线程单例的 Looper(Native)

可以看到这里的流程与 Java 的相反
  Java 是先创建 Looper, 然后在 Looper 内部创建 MessageQueue 
  Native 是先创建 NativeMessageQueue, 在其创建的过程中创建 Looper

接下来看看这个 C++ 的 Looper 对象在实例化的过程中做了哪些事情
system/core/libutils/Looper.cpp
```
Looper::Looper(bool allowNonCallbacks) :
        mAllowNonCallbacks(allowNonCallbacks), mSendingMessage(false),
        mPolling(false), mEpollFd(-1), mEpollRebuildRequired(false),
        mNextRequestSeq(0), mResponseIndex(0), mNextMessageUptime(LLONG_MAX) {
    // 1. 通过 Linux 创建 eventfd, 进行线程间通信
    // Android 6.0 之前为 pipe, 6.0 之后为 eventfd
    mWakeEventFd = eventfd(0, EFD_NONBLOCK | EFD_CLOEXEC);
    AutoMutex _l(mLock);
    // 2. 使用 epoll 监听 eventfd
    rebuildEpollLocked();
}
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

好的, 可以看到 Looper 实例化时做了如下的操作
1 创建了一个名为 mWakeEventFd 的 eventfd
  Android 6.0 之前使用的 pipe
  Android 6.0 之后使用的 eventfd
     比起管道, 它只有一个文件描述符, 更加轻量级
2 创建了一个 IO 多路复用的 epoll 对象
3 让 epoll 对其进行监听 mWakeEventFd

回顾  消息循环创建
Looper 的 prepare 操作主要是创建当前线程单例的 MessageQueue 对象, 保存在 mQueue 中
创建线程单例的 NativeMessageQueue 对象
    因为 Java 的 MessageQueue 是线程单例的, 因此 NativeMessageQueue 对象也是线程单例的
创建线程单例的 Looper(Native)
   创建了一个名为 mWakeEventFd 的 eventfd
   创建了一个 IO 多路复用的 epoll 对象
   让 epoll 对其进行监听 mWakeEventFd


消息循环的启动  Looper.loop
/frameworks/base/core/java/android/os/Looper.java
```
public static void loop() {
        // 获取调用线程的 Looper 对象
        final Looper me = myLooper();
        ......
        // 获取 Looper 对应的消息队列
        final MessageQueue queue = me.mQueue;
        // 死循环, 不断的处理消息队列中的消息
        for (;;) {
            // 1. 获取消息队列中的消息, 取不到消息会阻塞
            Message msg = queue.next(); // might block
            if (msg == null) {  //todo 投毒机制
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
好的, 可以看到 Looper 的 loop 内处理的事务非常清晰
  通过 MessageQueue.next() 获取下一条要处理的 Message
  通过 Message 的 Handler 处理这条消息        msg.target是Handler类型    
  最后消息分发完成，消息回收

好的, 可以看到获取消息的方式是通过 MessageQueue.next 拿到的, 我们接下来看看它是如何获取的
MessageQueue.next 获取下一条消息
/frameworks/base/core/java/android/os/MessageQueue.java
```
Message next() {
        // 获取 NativeMessageQueue 的句柄
        final long ptr = mPtr;
        if (ptr == 0) {
            return null;
        }
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
                    // 2.2.1 若当前的时刻 早于 消息的执行时刻
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

            ......// 4.1 处理空闲事件
            
            // 走到这里, 说明所有的空闲事件都已经处理好了
            // 将需要处理的空闲事件,置为 0 
            pendingIdleHandlerCount = 0;

            // 5. 因为处理空闲事件是耗时操作, 期间可能有新的 Message 入队列, 因此将可睡眠时长置为 0, 表示需要再次检查
            nextPollTimeoutMillis = 0;
        }
    }
    
    // native 方法
    private native void nativePollOnce(long ptr, int timeoutMillis); 
```
提问MessageQueue用的什么数据结构来存储数据
答：基于链表实现的队列
队列的特点是什么？先进先出，一般在队尾增加数据，在队首进行取数据或者删除数据。
MessageQueue的消息是按时间进行排序的(异步消息除外)

可以看到 MessageQueue.next 内部维护了一个死循环, 用于获取下一条 msg, 这个 for 循环做了如下的操作

1调用 nativePollOnce 根据 nextPollTimeoutMillis 时长, 执行当前线程的睡眠操作
2睡眠结束后, 取队列中可执行的 Msg 返回给 Looper 分发
  若有同步屏障消息, 找寻第一个异步的消息执行
  若未找到, 则找寻队首消息直接执行
     若当前的时刻 < 第一个同步的消息的执行时刻
         则更新 nextPollTimeoutMillis, 下次进行 for 循环时, 会进行睡眠操作
     若当前的时刻 >= 第一个同步的消息的执行时刻
         则将第一个同步消息的返回给 Looper 执行
  若未找到可执行消息
     将 nextPollTimeoutMillis 置为 -1, 下次 for 循环时可以无限睡眠, 直至消息队列中有新的消息为止
3未找到可执行 Message, 进行空闲消息的收集和处理: 到后面分析

这里我们主要看看 nativePollOnce Native 休眠机制 和 空闲消息处理 的流程
1. nativePollOnce 休眠
```
// frameworks/base/core/jni/android_os_MessageQueue.cpp
static void android_os_MessageQueue_nativePollOnce(JNIEnv* env, jobject obj,
        jlong ptr, jint timeoutMillis) {
    NativeMessageQueue* nativeMessageQueue = reinterpret_cast<NativeMessageQueue*>(ptr);
    // 调用了 NativeMessageQueue 的 pollOnce
    nativeMessageQueue->pollOnce(env, obj, timeoutMillis);
}

void NativeMessageQueue::pollOnce(JNIEnv* env, jobject pollObj, int timeoutMillis) {
    ......
    // 1. 调用了 Looper 的 pollOne
    mLooper->pollOnce(timeoutMillis);
    ......
}

// system/core/libutils/Looper.cpp
int Looper::pollOnce(int timeoutMillis, int* outFd, int* outEvents, void** outData) {
    int result = 0;
    for (;;) {   //死循环  不断执行pollInner，直到result不为0
        // 先处理没有Callback方法的 Response事件
        while (mResponseIndex < mResponses.size()) {
            const Response& response = mResponses.itemAt(mResponseIndex++);
            int ident = response.request.ident;
            if (ident >= 0) { //ident大于0，则表示没有callback, 因为POLL_CALLBACK = -2,
                int fd = response.request.fd;
                int events = response.events;
                void* data = response.request.data;
                if (outFd != NULL) *outFd = fd;
                if (outEvents != NULL) *outEvents = events;
                if (outData != NULL) *outData = data;
                return ident;
            }
        }
        // result 不为 0 说明读到了消息
        if (result != 0) {
            ......
            return result;
        }
        // 2. 若未读到消息, 则调用 pollInner
        result = pollInner(timeoutMillis);
    }
}


int Looper::pollInner(int timeoutMillis) {
    ......
    int result = POLL_WAKE;
    mResponses.clear();
    mResponseIndex = 0;
    mPolling = true; //即将处于idle状态
    struct epoll_event eventItems[EPOLL_MAX_EVENTS];  //fd最大为16
    // 3. 调用 epoll_wait 阻塞来监听 mEpollFd 中的 IO 事件, 超时机制由 timeoutMillis 决定
    //等待事件发生或者超时 在nativeWake()方法，向eventfd写入字符，则该方法会返回；
    int eventCount = epoll_wait(mEpollFd, eventItems, EPOLL_MAX_EVENTS, timeoutMillis);
    mPolling = false; //不再处于idle状态
    mLock.lock();  //请求锁
    if (mEpollRebuildRequired) {
        mEpollRebuildRequired = false;
        rebuildEpollLocked();  // epoll重建，直接跳转Done;
        goto Done;
    }
    if (eventCount < 0) {
        if (errno == EINTR) {
            goto Done;
        }
        result = POLL_ERROR; // epoll事件个数小于0，发生错误，直接跳转Done;
        goto Done;
    }
    if (eventCount == 0) {  //epoll事件个数等于0，发生超时，直接跳转Done;
        result = POLL_TIMEOUT;
        goto Done;
    }

    
    // 4. 走到这里, 说明睡眠结束了
    //循环遍历，处理所有的事件
    for (int i = 0; i < eventCount; i++) {
        int fd = eventItems[i].data.fd;              // 获取文件描述
        uint32_t epollEvents = eventItems[i].events;
        // 5. 若存在唤醒事件的文件描述, 则执行唤醒操作
        if (fd == mWakeEventFd) {
            if (epollEvents & EPOLLIN) {
                awoken();// 已经唤醒了，则读取并清空管道数据
            } 
        } else {
            ssize_t requestIndex = mRequests.indexOfKey(fd);
            if (requestIndex >= 0) {
                int events = 0;
                if (epollEvents & EPOLLIN) events |= EVENT_INPUT;
                if (epollEvents & EPOLLOUT) events |= EVENT_OUTPUT;
                if (epollEvents & EPOLLERR) events |= EVENT_ERROR;
                if (epollEvents & EPOLLHUP) events |= EVENT_HANGUP;
                //处理request，生成对应的reponse对象，push到响应数组
                pushResponse(events, mRequests.valueAt(requestIndex));
            }
        }
    }
 Done: ;
    //再处理Native的Message，调用相应回调方法
    mNextMessageUptime = LLONG_MAX;
    while (mMessageEnvelopes.size() != 0) {
        nsecs_t now = systemTime(SYSTEM_TIME_MONOTONIC);
        const MessageEnvelope& messageEnvelope = mMessageEnvelopes.itemAt(0);
        if (messageEnvelope.uptime <= now) {
            {
                sp<MessageHandler> handler = messageEnvelope.handler;
                Message message = messageEnvelope.message;
                mMessageEnvelopes.removeAt(0);
                mSendingMessage = true;
                mLock.unlock();  //释放锁
                handler->handleMessage(message);  // 处理消息事件
            }
            mLock.lock();  //请求锁
            mSendingMessage = false;
            result = POLL_CALLBACK; // 发生回调
        } else {
            mNextMessageUptime = messageEnvelope.uptime;
            break;
        }
    }
    mLock.unlock(); //释放锁

    //处理带有Callback()方法的Response事件，执行Reponse相应的回调方法
    for (size_t i = 0; i < mResponses.size(); i++) {
        Response& response = mResponses.editItemAt(i);
        if (response.request.ident == POLL_CALLBACK) {
            int fd = response.request.fd;
            int events = response.events;
            void* data = response.request.data;
            // 处理请求的回调方法
            int callbackResult = response.request.callback->handleEvent(fd, events, data);
            if (callbackResult == 0) {
                removeFd(fd, response.request.seq); //移除fd
            }
            response.request.callback.clear(); //清除reponse引用的回调方法
            result = POLL_CALLBACK;  // 发生回调
        }
    }
    return result;
}
void Looper::awoken() {
    uint64_t counter;
    //不断读取管道数据，目的就是为了清空管道内容
    TEMP_FAILURE_RETRY(read(mWakeEventFd, &counter, sizeof(uint64_t)));
}
```
好的可以看到 JNI 方法 nativePollOnce, 其内部流程如下
1将 Poll 操作转发给了 Looper.pollOne
2若未读到消息, 则调用 Looper.pollInner
3调用 epoll_wait 阻塞监听 mEpollFd 中的 IO 事件   //todo epoll_wait
   若无事件, 则睡眠在 mWakeEventFd 文件读操作上, 时长由 timeoutMillis 决定
4睡眠结束后调用 awoken 唤醒
好的, 至此线程是睡眠的机制也明了了, 下面看看空闲消息处理机制

2. 空闲处理者执行
  分析转移到 handler04_idleHandler.md

   
好的, 到这里我们的消息获取的流程就完成了, 下面看看 Looper 获取到 Message 之后是如何执行的



消息的处理
当 MessageQueue.next 返回一个 Message 时, Looper 中的 loop 方法便会处理消息的执行, 先回顾一下代码
/frameworks/base/core/java/android/os/Looper.java
```
public static void loop() {
   msg.target.dispatchMessage(msg);
}
```
好的, 可以看到当 MessageQueue.next 返回一个 Message 时, 便会调用 msg.target.dispatchMessage(msg) 去处理
这个 msg

msg.target 为一个 Handler 对象, 在上面的分析中可知, 在通过 Handler 发送消息的 enqueueMessage 方法中, 
   会将 msg.target 设置为当前的 Handler
可以看到, 这个 msg 正是由将它投递到消息队列的 Handler 处理了, 它们是一一对应的
接下来我们看看 Handler 处理消息的流程
/frameworks/base/core/java/android/os/Handler.java
```
public class Handler {
    public void dispatchMessage(Message msg) {
        // 1. 若 msg 对象中存在 callback, 则调用 handleCallback
        if (msg.callback != null) {
            handleCallback(msg);
        } else {
            // 2. 若当前 Handler 设值了 Callback, 进入这个分支
            if (mCallback != null) {
                // 2.1 若这个 Callback 的 handleMessage 返回 true, 则不会将消息继续向下分发
                if (mCallback.handleMessage(msg)) {
                    return;
                }
            }
            // 3. 若消息没有被 mCallback 拦截, 则会调用 handleMessage 进行最后的处理
            handleMessage(msg);
        }
    } 
    
    //方式一: Message.callback优先级最高     一般handler.post执行这个分支
    private static void handleCallback(Message message) {
        message.callback.run();
    }
    
    public interface Callback {
       //方式二: 优先级次高
        public boolean handleMessage(Message msg);
    }
     
    public Handler(Callback callback, boolean async) {
        ......
        // Callback 由构造函数传入
        mCallback = callback;
        ......
    } 
    
   //方式三: 这个处理消息的方式, 由子类重写, 优先级最低
    public void handleMessage(Message msg) {
    
    }
    
 }
```
以看到 Handler 的 dispatchMessage 处理消息主要有三种方式
1若是 Message 对象内部设置了 callback, 则调用 handleCallback 方法直接处理, 不会再往下分发
2若 Handler 设置 Callback, 则会调用 Callback.handleMessage 方法
   Callback.handleMessage 返回 false, 则会将消息处理继续分发给 Handler.handleMessage

回顾  消息循环的启动 looper.loop
Looper.loop 主要是通过死循环, 不断地处理消息队列中的数据, 流程如下
通过 MessageQueue.next() 获取下一条要处理的 Msg
   调用 nativePollOnce 根据 nextPollTimeoutMillis 时长, 执行当前线程的睡眠操作
      调用 epoll_wait 来监听 mEpollFd 中的 IO 事件, 若无事件, 则睡眠在 mWakeEventFd 文件读操作上, 时长由 timeoutMillis 决定
      睡眠结束后调用 awoken 唤醒
   睡眠结束, 获取队列中可执行 Message
      若有同步屏障消息, 找寻第一个异步的消息执行
      若未找到, 则找寻队首消息直接执行
        若当前的时刻 < 第一个同步的消息的执行时刻
           则更新 nextPollTimeoutMillis, 下次进行 for 循环时, 会进行睡眠操作
        若当前的时刻 >= 第一个同步的消息的执行时刻
           则将第一个同步消息的返回给 Looper 执行
      若未找到可执行消息
        将 nextPollTimeoutMillis 置为 -1, 下次 for 循环时可以无限睡眠, 直至消息队列中有新的消息为止
   未找到可执行 Message, 进行空闲消息的收集和处理
     收集空闲消息
       从空闲消息集合 mIdleHandlers 中获取 空闲处理者 数量
       若无空闲处理者, 则进行下一次 for 循环
       若存在空闲处理者, 则空闲消息处理者集合转为数组 mPendingIdleHandlers
   处理空闲消息
      调用 IdleHandler.queueIdle 处理空闲消息
      返回 true, 下次再 MessageQueue.next 获取不到 msg 的空闲时间会继续处理
      返回 false 表示它是一次性的处理者, 从 mIdleHandlers 移除
通过 msg.target.dispatchMessage(msg); 分发处理消息
   若是 Message 对象内部设置了 callback, 则调用 handleCallback 方法直接处理, 不会再往下分发
   若 Handler 设置 Callback, 则会调用 Callback.handleMessage 方法
      Callback.handleMessage 返回 false, 则会将消息处理继续分发给 Handler.handleMessage