android8.1 https://sharrychoo.github.io/blog/android-source/dc-handler

消息的发送
Handler 主要有三种类型的消息
同步消息
Android 4.1 配合 Choreographer 引入
  同步屏障: 从同步屏障起的时刻跳过同步消息, 优先执行异步消息   
      通过postSyncBarrier方法添加的消息，特点是target为空，也就是没有对应的handler
  异步消息: 用于和同步区分, 并非多线程异步执行的意思  

消息的发送   其他线程唤醒阻塞的主线程
/frameworks/base/core/java/android/os/Handler.java
```
public class Handler {
    public final boolean sendMessage(Message msg) {
        // 回调了发送延时消息的方法
        return sendMessageDelayed(msg, 0);
    }
    
    public final boolean sendMessageDelayed(Message msg, long delayMillis) {
        if (delayMillis < 0) {
            delayMillis = 0;
        }
        // 回调了发送指定时刻消息的方法
        return sendMessageAtTime(msg, /*指定时刻 = 当前时刻 + 延时时间*/SystemClock.uptimeMillis() + delayMillis);
    }
    
    public boolean sendMessageAtTime(Message msg, long uptimeMillis) {
        MessageQueue queue = mQueue;
        ......
        // 回调了入消息队列的方法
        return enqueueMessage(queue, msg, uptimeMillis);
    }
    
    private boolean enqueueMessage(MessageQueue queue, Message msg, long uptimeMillis) {
        // 1. 将这个消息的处理者, 设置为其自身
        msg.target = this;
        // 2. 若 mAsynchronous 为 true
        if (mAsynchronous) {
            // 在投递到消息队列之前, 将其设置为异步消息
            msg.setAsynchronous(true);
        }
        // 3. 调用 MessageQueue 的 enqueueMessage 执行入队列的操作
        return queue.enqueueMessage(msg, uptimeMillis);
    }
 }
```
可以看到发送消息的操作, 进过了一系列方法的调用, 会走到 sendMessageAtTime 中, 表示发送指定时刻的消息, 然后会调用 
enqueueMessage 执行入消息队列操作

1将这个消息的 target 赋值为自身, 表示这个消息到时候会被当前 Handler 对象处理
2mAsynchronous 在 Handler 构造时传入, 若为 true, 则说明通过这个 Handler 发送的消息为异步消息  
3调用了 MessageQueue.enqueueMessage 将消息投递到消息队列中去

MessageQueue.enqueueMessage 做了哪些操作
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

可以看到 MessageQueue 在执行消息入队列时, 做了如下操作
1 队列中无消息	新消息要立即执行	新消息要执行的时间比队首的早时
   将该消息放置到队首
   若该 MessageQueue 之前为阻塞态, 则说明需要唤醒
2新消息执行的时间比队首的晚
  判断是否需要唤醒队列
     队列前有一个同步屏障, 并且该消息异步的, 则有机会唤醒队列
  将该消息插入到消息队列合适的位置
     若该消息并非第一个异步消息, 则不允许唤醒
3通过 needWake 处理该消息队列绑定线程的唤醒操作

这里我们可以看到, 新消息入队列只有两种情况才会将阻塞的队列唤醒
 1它插入到了队首, 需要立即执行
 2队首存在同步屏障并且它是第一个异步的消息

nativeWake 唤醒
frameworks/base/core/jni/android_os_MessageQueue.cpp
```
// frameworks/base/core/jni/android_os_MessageQueue.cpp
static void android_os_MessageQueue_nativeWake(JNIEnv* env, jclass clazz, jlong ptr) {
    NativeMessageQueue* nativeMessageQueue = reinterpret_cast<NativeMessageQueue*>(ptr);
    nativeMessageQueue->wake();
}

void NativeMessageQueue::wake() {
    mLooper->wake();
}

// system/core/libutils/Looper.cpp
void Looper::wake() {
    // 向 Looper 绑定的线程 mWakeEventFd 中写入一个新的数据   唤醒线程
    uint64_t inc = 1;
    ssize_t nWrite = TEMP_FAILURE_RETRY(write(mWakeEventFd, &inc, sizeof(uint64_t)));
    ....
}
```
//todo linux TEMP_FAILURE_RETRY    write
可以看到, nativeWake 是通过向 Looper 绑定的线程的 mWakeEventFd 写入一个数据的来唤醒目标线程
通过上一篇的分析可知, 此时 Looper::pollInner 会睡眠在 mWakeEventFd 的读操作上


同步屏障的发送
我们知道, 使用 Handler 发送消息时, 会按照时间的顺序添加到 MessageQueue 中, 这就说明了我们的消息一定会排在队列后面的执行, 
但我们的消息又非常的重要, 想让他尽快的执行, 这个时候就可以采用同步屏障的方式

它能够优先执行的原因, 我们在 MessageQueue.next 中已经分析过了, 下面我们看看它的发送和移除
1. 添加同步屏障   添加和移除通常是一对操作
同步屏障的使用
/frameworks/base/core/java/android/view/ViewRootImpl.java
```
void scheduleTraversals() {
  if (!mTraversalScheduled) {
      mTraversalScheduled = true;
      mTraversalBarrier = mHandler.getLooper().getQueue().postSyncBarrier();
      mChoreographer.postCallback(
              Choreographer.CALLBACK_TRAVERSAL, mTraversalRunnable, null);
      if (!mUnbufferedInputDispatch) {
          scheduleConsumeBatchedInput();
      }
      notifyRendererOfFramePending();
      pokeDrawLockIfNeeded();
  }
}

void unscheduleTraversals() {
  if (mTraversalScheduled) {
      mTraversalScheduled = false;
      mHandler.getLooper().getQueue().removeSyncBarrier(mTraversalBarrier);
      mChoreographer.removeCallbacks(
              Choreographer.CALLBACK_TRAVERSAL, mTraversalRunnable, null);
  }
}
```   
   
frameworks/base/core/java/android/os/MessageQueue.java
```
public int postSyncBarrier() {
        return postSyncBarrier(SystemClock.uptimeMillis());
    }

    private int postSyncBarrier(long when) {
        synchronized (this) {
            // 1. 为这个同步屏障的生成一个 token
            final int token = mNextBarrierToken++;
            // 2. 创建一个 Message
            final Message msg = Message.obtain();
            msg.markInUse();
            msg.when = when;
            // 2.1 将 token 保存到 arg1 中
            msg.arg1 = token;
            
            // 3. 找寻插入点
            Message prev = null;
            Message p = mMessages; 
            if (when != 0) {
                while (p != null && p.when <= when) {
                    prev = p;
                    p = p.next;
                }
            }
            // 4. 链入队列
            if (prev != null) { 
                msg.next = p;
                prev.next = msg;
            } else {
                msg.next = p;
                mMessages = msg;
            }
            // 5. 返回 token
            return token;
        }
    }
```   
可以看到这个添加同步屏障的过程非常简单, 可以说与将一个正常的消息添加到队列中做法一致, 那它是如何起到同步消息的屏障的作用的呢?
关键点在于这个 msg.target 为 null, 也就是说他没有绑定处理它的 Handler, 这就能够实现同步屏障的效果吗? 
  我们去 MessageQueue.next 中验证我们的想法

移除同步屏障
/frameworks/base/core/java/android/os/MessageQueue.java
```
public void removeSyncBarrier(int token) {
        synchronized (this) {
            Message prev = null;
            Message p = mMessages;
            // 1. 遍历消息队列, 找寻同步屏障及其前驱结点的 msg
            while (p != null && (p.target != null || p.arg1 != token)) {
                prev = p;
                p = p.next;
            }
           
            // 2.1 若存在前驱, 则说明这个同步屏障暂未被执行
            if (prev != null) {
                prev.next = p.next;
                needWake = false;// 不需要唤醒
            } else {
                // 2.2 若同步屏障生效了, 则根据其后继判断是否需要唤醒
                mMessages = p.next;
                needWake = mMessages == null || mMessages.target != null;
            }
            // 3. 回收消息
            p.recycleUnchecked();
            // 4. 判读是否需要唤醒在 Message.next 上的睡眠
            if (needWake && !mQuitting) {
                nativeWake(mPtr);
            }
        }
    }
```
可以看到移除同步屏障的过程也非常的简单, 即通过 token 移除这个 msg




Handler.post方法
/frameworks/base/core/java/android/os/Handler.java
```
public final boolean post(Runnable r)
      {
         return  sendMessageDelayed(getPostMessage(r), 0);
      }
      
 private static Message getPostMessage(Runnable r) {
          Message m = Message.obtain();
          m.callback = r;
          return m;
      }  
 public void dispatchMessage(Message msg) {
          if (msg.callback != null) {
              handleCallback(msg);
          } else {
              if (mCallback != null) {
                  if (mCallback.handleMessage(msg)) {
                      return;
                  }
              }
              handleMessage(msg);
          }
 }  
 private static void handleCallback(Message message) {
          message.callback.run();
      }       
```
将Runnable附加到Message.callback中,进入MessageQueue等待分发
根据handler01_消息循环的创建与启动.md,Looper.loop，取到消息，然后执行到msg.target.dispatchMessage，
msg.callback不为空，执行回调


在队头插入消息
在消息队列的前端将消息排队，以便在消息循环的下一次迭代中进行处理。您将在{@link#handleMessage}中收到它，
它位于连接到此处理程序的线程中。此方法仅在非常特殊的情况下使用--它很容易使消息队列饥饿、导致排序问题或产生其他意外的副作用。
postAtFrontOfQueue  
/frameworks/base/core/java/android/os/Handler.java
```
public final boolean postAtFrontOfQueue(Runnable r)
{
  return sendMessageAtFrontOfQueue(getPostMessage(r));
}
public final boolean sendMessageAtFrontOfQueue(Message msg) {
  MessageQueue queue = mQueue;
  //队头添加消息
  return enqueueMessage(queue, msg, 0);
} 
private boolean enqueueMessage(MessageQueue queue, Message msg, long uptimeMillis) {
  msg.target = this;
  if (mAsynchronous) {
      msg.setAsynchronous(true);
  }
  return queue.enqueueMessage(msg, uptimeMillis);
}
```

http://gityuan.com/2015/12/27/handler-message-native/
native层向native MessageQueue发送消息     
```
/system/core/libutils/Looper.cpp
void Looper::sendMessage(const sp<MessageHandler>& handler, const Message& message) {
    nsecs_t now = systemTime(SYSTEM_TIME_MONOTONIC);
    sendMessageAtTime(now, handler, message);
}
void Looper::sendMessageAtTime(nsecs_t uptime, const sp<MessageHandler>& handler,
        const Message& message) {
    size_t i = 0;
    { //请求锁
        AutoMutex _l(mLock);
        size_t messageCount = mMessageEnvelopes.size();
        //找到message应该插入的位置i
        while (i < messageCount && uptime >= mMessageEnvelopes.itemAt(i).uptime) {
            i += 1;
        }
        MessageEnvelope messageEnvelope(uptime, handler, message);
        mMessageEnvelopes.insertAt(messageEnvelope, i, 1);
        //如果当前正在发送消息，那么不再调用wake()，直接返回。
        if (mSendingMessage) {
            return;
        }
    } //释放锁
    //当把消息加入到消息队列的头部时，需要唤醒poll循环。
    if (i == 0) {
        wake();
    }
}
```
native层类结构   ident ['aɪdent] 识别，标记短片
/system/core/libutils/include/utils/Looper.h
```
//looper对fd的监控类型
enum {
    //read操作
    ALOOPER_EVENT_INPUT = 1 << 0,
   //write操作
    ALOOPER_EVENT_OUTPUT = 1 << 1,
    //fd出错
    ALOOPER_EVENT_ERROR = 1 << 2,
    //fd hung up  例如pipe的远端close了
    ALOOPER_EVENT_HANGUP = 1 << 3,
    //fd INVALID     例如fd过早无效
    ALOOPER_EVENT_INVALID = 1 << 4,
    
//ALooper_pollOnce() and ALooper_pollAll()的返回结果
enum{
    POLL_WAKE = -1,POLL_CALLBACK = -2,POLL_TIMEOUT = -3,POLL_ERROR = -4,
}
struct Request { //请求结构体
    int fd;
    int ident;
    int events;
    int seq;
    sp<LooperCallback> callback;
    void* data;
    void initEventItem(struct epoll_event* eventItem) const;
};

struct Response { //响应结构体
    int events;
    Request request;
};

struct MessageEnvelope { //信封结构体
    MessageEnvelope() : uptime(0) { }
    MessageEnvelope(nsecs_t uptime, const sp<MessageHandler> handler,
            const Message& message) : uptime(uptime), handler(handler), message(message) {
    }
    nsecs_t uptime;
    sp<MessageHandler> handler;
    Message message;
};
//native层消息处理的handler
class MessageHandler : public virtual RefBase {
protected:
    virtual ~MessageHandler() { }
public:
    virtual void handleMessage(const Message& message) = 0;
};

class WeakMessageHandler : public MessageHandler {
protected:
    virtual ~WeakMessageHandler();
public:
    WeakMessageHandler(const wp<MessageHandler>& handler);
    virtual void handleMessage(const Message& message);
private:
    wp<MessageHandler> mHandler;
};

void WeakMessageHandler::handleMessage(const Message& message) {
    sp<MessageHandler> handler = mHandler.promote();
    if (handler != NULL) {
        handler->handleMessage(message); //调用MessageHandler类的处理方法()
    }
}
```
MessageEnvelope正如其名字，信封。MessageEnvelope里面记录着收信人(handler)，发信时间(uptime)，信件内容(message)
native层总结
MessageQueue通过mPtr变量保存NativeMessageQueue对象，从而使得MessageQueue成为Java层和Native层的枢纽，既能处理上层消息，
也能处理native层消息；
下面列举Java层与Native层的对应图
handler的Java层与native层对照.png
红色虚线关系：Java层和Native层的MessageQueue通过JNI建立关联，彼此之间能相互调用，搞明白这个互调关系，
   也就搞明白了Java如何调用C++代码，C++代码又是如何调用Java代码。
蓝色虚线关系：Handler/Looper/Message这三大类Java层与Native层并没有任何的真正关联，只是分别在Java层和Native层的handler消息模型
   中具有相似的功能。都是彼此独立的，各自实现相应的逻辑。
WeakMessageHandler继承于MessageHandler类，NativeMessageQueue继承于MessageQueue类
另外，消息处理流程是先处理Native Message，再处理Native Request，最后处理Java Message。理解了该流程，
  也就明白有时上层消息很少，但响应时间却较长的真正原因。

native Looper的常用方法
addFd  向Looper添加一个fd, 使用 epoll 监听fd, 用于线程唤醒
```
int Looper::addFd(int fd, int ident, int events, Looper_callbackFunc callback, void* data) {
    return addFd(fd, ident, events, callback ? new SimpleLooperCallback(callback) : NULL, data);
}

int Looper::addFd(int fd, int ident, int events, const sp<LooperCallback>& callback, void* data) {
    ...
    int epollResult = epoll_ctl(mEpollFd, EPOLL_CTL_ADD, fd, & eventItem);
    ...
    return 1;
}
```

removeFd 方法  移除对fd的监听
```
int Looper::removeFd(int fd) {
    return removeFd(fd, -1);
}

int Looper::removeFd(int fd, int seq) {
    ...
    int epollResult = epoll_ctl(mEpollFd, EPOLL_CTL_DEL, fd, NULL);
    ...
}
```



消息循环退出
```
/frameworks/base/core/java/android/os/Looper.java
quitAllowed是否退出消息循环
private Looper(boolean quitAllowed) {
          mQueue = new MessageQueue(quitAllowed);
          mThread = Thread.currentThread();
}
public void quit() {
          mQueue.quit(false);
      }
public void quitSafely() {
      mQueue.quit(true);
  } 
  
/frameworks/base/core/java/android/os/MessageQueue.java
void quit(boolean safe) {
    if (!mQuitAllowed) {
        throw new IllegalStateException("Main thread not allowed to quit.");
    }
    synchronized (this) {
        if (mQuitting) {
            return;
        }
        mQuitting = true;
        if (safe) {
            removeAllFutureMessagesLocked();
        } else {
            removeAllMessagesLocked();
        }
    }
}   
private void removeAllMessagesLocked() {
      Message p = mMessages;
      while (p != null) {
          Message n = p.next;
          p.recycleUnchecked();
          p = n;
      }
      mMessages = null;
  }
private void removeAllFutureMessagesLocked() {
  final long now = SystemClock.uptimeMillis();
  Message p = mMessages;
  if (p != null) {
      if (p.when > now) {
          removeAllMessagesLocked();
      } else {
          Message n;
          for (;;) {
              n = p.next;
              if (n == null) {
                  return;
              }
              if (n.when > now) {
                  break;
              }
              p = n;
          }
          p.next = null;
          do {
              p = n;
              n = p.next;
              p.recycleUnchecked();
          } while (n != null);
      }
  }
 }
 
```
quitAllowed代表是否可以退出消息循环  false代表不允许
safe代表是否安全退出
消息退出的方式：
 当safe =true时，只移除尚未触发的所有消息(when>now)，对于正在触发的消息并不移除；
 当safe =flase时，移除所有的消息
标记mQuitting为true
然后看看当调用quit方法之后，消息的发送和处理：
```
/frameworks/base/core/java/android/os/MessageQueue.java
//消息发送
boolean enqueueMessage(Message msg, long when) {
    synchronized (this) {
        if (mQuitting) {
            IllegalStateException e = new IllegalStateException(
                    msg.target + " sending message to a Handler on a dead thread");
            Log.w(TAG, e.getMessage(), e);
            msg.recycle();
            return false;
        }
    }
Message next() {
    for (;;) {
        synchronized (this) {
            if (mQuitting) {
                dispose();
                return null;
            } 
        }  
    }
}
private void dispose() {
      if (mPtr != 0) {
      nativeDestroy(mPtr);
      mPtr = 0;
      }
  }
```
当调用了quit方法之后，mQuitting为true，消息就发不出去了，会报错。
MessageQueue.next()会返回一个null消息，同时销毁native的MessageQueue
看一下Looper的处理
```
/frameworks/base/core/java/android/os/Looper.java
public static void loop() {
    for (;;) {
        Message msg = queue.next();
        if (msg == null) {
            // No message indicates that the message queue is quitting.
            return;
        }
    }
}
```
很明显，当mQuitting为true的时候，next方法返回null，那么loop方法中就会退出死循环。
那么这个quit方法一般是什么时候使用呢？
 主线程中，一般情况下肯定不能退出，因为退出后主线程就停止了。所以是当APP需要退出的时候，就会调用quit方法，
   涉及到的消息是EXIT_APPLICATION，大家可以搜索下。
 子线程中，如果消息都处理完了，就需要调用quit方法停止消息循环。

nativeDestroy相关
```
/frameworks/base/core/jni/android_os_MessageQueue.cpp
static void android_os_MessageQueue_nativeDestroy(JNIEnv* env, jclass clazz, jlong ptr) {
      NativeMessageQueue* nativeMessageQueue = reinterpret_cast<NativeMessageQueue*>(ptr);
      nativeMessageQueue->decStrong(env);
  }
```
nativeMessageQueue继承自RefBase类，所以decStrong最终调用的是RefBase.decStrong().
/system/core/libutils/RefBase.cpp
```
void RefBase::decStrong(const void* id) const
{
    weakref_impl* const refs = mRefs;
    refs->removeStrongRef(id); //移除强引用
    const int32_t c = android_atomic_dec(&refs->mStrong);
    if (c == 1) {
        refs->mBase->onLastStrongRef(id);
        if ((refs->mFlags&OBJECT_LIFETIME_MASK) == OBJECT_LIFETIME_STRONG) {
            delete this;
        }
    }
    refs->decWeak(id); // 移除弱引用
}
```
