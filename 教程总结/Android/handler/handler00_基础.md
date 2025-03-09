https://mp.weixin.qq.com/s/63zHksIEEgm3PFNLtxIUrg
handler使用方式
主线程
```
class MHandler extend Handler{
  public void handleMessage(Message msg){
  
  }
}
Handler mhandler = new MHandler();
```
子线程
```
new Thread(){
  public void run(){
    Looper.Prepare();
     ..//创建handler
     Looper.loop();
  }
}.start();
```
主线程和子现场使用handler不同，主线程的looper.prepareMainLooper,looper.loop已经在activityThread调用了
 子线程需要手动创建和启动looper

如何获取子线程looper和主线程looper? 
```
Handler.getLooper == Looper.myLooper()   看handler的创建线程
Looper.getMainLooper() 获取主线程的looper
```

Handler泄露的原因及正确写法?  看一下android常见内存泄露.md


Handler被设计出来的原因？有什么用？
一种东西被设计出来肯定就有它存在的意义，而Handler的意义就是切换线程。
作为Android消息机制的主要成员，它管理着所有与界面有关的消息事件，常见的使用场景有：
   跨进程之后的界面消息处理。
      比如Activity的启动，就是AMS在进行进程间通信的时候，通过Binder线程 将消息发送给ApplicationThread的消息处理者Handler，
      然后再将消息分发给主线程中去执行。
   网络交互后切换到主线程进行UI更新。
     当子线程网络操作之后，需要切换到主线程进行UI更新。
总之一句话，Handler的存在就是为了解决在子线程中无法访问UI的问题


handler是单线程模型
为什么建议子线程不访问（更新）UI？
因为Android中的UI控件不是线程安全的，如果多线程访问UI控件那还不乱套了。
那为什么不加锁呢？
   会降低UI访问的效率。本身UI控件就是离用户比较近的一个组件，加锁之后自然会发生阻塞，那么UI访问的效率会降低，
       最终反应到用户端就是这个手机有点卡。
   太复杂了。本身UI访问时一个比较简单的操作逻辑，直接创建UI，修改UI即可。如果加锁之后就让这个UI访问的逻辑变得很复杂，
      没必要。    自己：复杂也意味着稳定性下降，容易出现问题，例如死锁，编程的难度也上升了
     //对于多线程设计，不是加一个，是每层都要加锁（用户代码→GUI顶层→GUI底层…），这样也以为着 耗时，UI更新效率变低；如果每层共用同一把锁，那就是 单线程 了
所以，Android设计出了 单线程模型 来处理UI操作，再搭配上Handler，是一个比较合适的解决方案
    采用「单线程消息队列机制」，实现一个「伪锁」，使用队列产生先后顺序，没有并发的前提


同一线程的不同handler对应一个looper?
handler的looper通过ThreadLocal保存在Thread的ThreadLocalMap中，同一线程只有一个
为什么不同线程不共享looper？
线程之间产生了联系，复杂性提升，可能出现死锁，内存泄露

Message的what需要唯一吗？
不需要，只要message发送的handler里面唯一即可，多个主线程的handler，可以有相同what的msg，这些在分发时会发送给自己对应的handler

可以多次创建Looper吗？
不可以  prepare进行了校验
```
private static void prepare(boolean quitAllowed) {
          if (sThreadLocal.get() != null) {
              throw new RuntimeException("Only one Looper may be created per thread");
          }
          sThreadLocal.set(new Looper(quitAllowed));
      }  
```

Handler、Looper、MessageQueue、线程是一一对应关系吗？   
一个线程只会有一个Looper对象，所以线程和Looper是一一对应的。
MessageQueue对象是在new Looper的时候创建的，所以Looper和MessageQueue是一一对应的。
Handler的作用只是将消息加到MessageQueue中，并后续取出消息后，根据消息的target字段分发给当初的那个handler，
所以Handler对于Looper是可以多对一的，也就是多个Handler对象都可以用同一个线程、同一个Looper、同一个MessageQueue。
总结：Looper、MessageQueue、线程是一一对应关系，而他们与Handler是可以一对多的

Handler，Looper，MessageQueue三者的关系
Handler 获取当前线程中的 Looper 对象，Looper 用来从存放 Message 的MessageQueue 中取出 Message，
   再交由 Handler 进行 Message 的分发和处理
handler结构示例.webp


如何实现的线程切换
handler是如何将消息发送到其他线程的
Looper是每个线程都有的，handler持有当前线程的looper，不同线程之间就可以依靠拿到对方的handler和 Looper 来实现消息的跨线程处理

Message消息执行在哪条线程
判断 Handler 对象里面的 Looper 对象是属于哪条线程的，则由该线程来执行！
①当 Handler 对象的构造函数的参数为空，则为当前所在线程的 Looper；
②Looper.getMainLooper()得到的是主线程的 Looper 对象，Looper.myLooper()得到的是当前线程的 Looper 对象。



//nativepollonce  在等待Java层的消息，也在等待native层的消息
//MessageQueue同时支持Java事件，也支持native事件

Handler.Callback.handleMessage 和 Handler.handleMessage 有什么不一样？为什么这么设计？
接着上面的代码说，这两个处理方法的区别在于Handler.Callback.handleMessage方法是否返回true：
如果为true，则不再执行Handler.handleMessage。
如果为false，则两个方法都要执行。
那么什么时候有Callback，什么时候没有呢？这涉及到两种Handler的 创建方式：
```
val handler1= object : Handler(){
    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
    }
}
val handler2 = Handler(object : Handler.Callback {
    override fun handleMessage(msg: Message): Boolean {
        return true
    }
})
```
常用的方法就是第1种，派生一个Handler的子类并重写handleMessage方法。而第2种就是系统给我们提供了一种不需要派生子类的使用方法，
只需要传入一个Callback即可


Looper.loop方法是死循环，为什么不会卡死（ANR）？  
关于这个问题，强烈建议看看Gityuan的回答：
https://www.zhihu.com/question/34652589
大致总结下：
1、主线程本身就是需要一直运行的，因为要处理各个View，界面变化。所以需要这个死循环来保证主线程一直执行下去，不会被退出。
2、真正会卡死的操作是在某个消息处理的时候操作时间过长(dispatchMessage)，导致掉帧、ANR，而不是loop方法本身。
3、在主线程以外，会有其他的线程来处理接受其他进程的事件，比如Binder线程（ApplicationThread），会接受AMS发送来的事件。
4、在收到跨进程消息后，会交给主线程的Hanlder再进行消息分发。所以Activity的生命周期都是依靠主线程的Looper.loop，当
   收到不同Message时则采用相应措施，比如收到msg=H.LAUNCH_ACTIVITY，则调用ActivityThread.handleLaunchActivity()方法，
   最终执行到onCreate方法。
5、当没有消息的时候，会阻塞在loop的queue.next()中的nativePollOnce()方法里，此时主线程会释放CPU资源进入休眠状态，
  直到下个消息到达或者有事务发生。所以死循环也不会特别消耗CPU资源。

application启动时，可不止一个main线程，还有其他两个Binder线程：ApplicationThread 和 ActivityManagerProxy，
  用来和系统进程进行通信操作，接收系统进程发送的通知
1 当系统受到因用户操作产生的通知时，会通过 Binder 方式跨进程通知 ApplicationThread;    
2 它通过Handler机制，往 ActivityThread 的 MessageQueue 中插入消息，唤醒了主线程；
3 queue.next() 能拿到消息了,然后 dispatchMessage 完成事件分发；
PS：ActivityThread 中的内部类H中有具体实现     
//ActivityManagerProxy应该后来是IActivityManager


消息的类型
同步消息。也就是普通的消息。
异步消息。通过setAsynchronous(true)设置的消息。   不是多线程的异步，只是相对同步消息，没有按顺序执行，优先级较高，所以是异步
同步屏障消息。通过postSyncBarrier方法添加的消息，特点是target为空，也就是没有对应的handler

这三者之间的关系如何呢？
  正常情况下，同步消息和异步消息都是正常被处理，也就是根据时间when来取消息，处理消息。
  当遇到同步屏障消息的时候，就开始从消息队列里面去找异步消息，找到了再根据时间决定阻塞还是返回消息。
也就是说同步屏障消息不会被返回，他只是一个标志，一个工具，遇到它就代表要去先行处理异步消息了。
   所以同步屏障和异步消息的存在的意义就在于有些消息需要“加急处理”。

同步屏障，阻止的是同步消息，突破屏障的是异步消息
MessageQueue.postSyncBarrier注释中关于同步屏障的解释   
Message processing occurs as usual until the message queue encounters the
 synchronization barrier that has been posted.  When the barrier is encountered,
 later synchronous messages in the queue are stalled (prevented from being executed)
 until the barrier is released by calling {@link #removeSyncBarrier} and specifying
the token that identifies the synchronization barrier.

This method is used to immediately postpone execution of all subsequently posted
synchronous messages until a condition is met that releases the barrier.
Asynchronous messages (see {@link Message#isAsynchronous} are exempt from the barrier
 and continue to be processed as usual.
stalled [stɔːl]  n货摊，摊位，售货亭(尤指集市上的);牲畜棚;马厩;牛棚;(房间内的)小隔间，淋浴室，洗手间
v.  (使)熄火，抛锚;故意拖延(以赢得时间);拖住(以赢得时间做某事)
postpone [poʊˈspoʊn] vt. 推迟;延期;延迟;展缓
exempt   [ɪɡˈzempt]   vt. 豁免;免除

Message.setAsynchronous的注释中关于异步消息的描述
Sets whether the message is asynchronous, meaning that it is not
 subject to {@link Looper} synchronization barriers.

 Certain operations, such as view invalidation, may introduce synchronization
 barriers into the {@link Looper}'s message queue to prevent subsequent messages
 from being delivered until some condition is met.  In the case of view invalidation,
 messages which are posted after a call to {@link android.view.View#invalidate}
are suspended by means of a synchronization barrier until the next frame is
 ready to be drawn.  The synchronization barrier ensures that the invalidation
request is completely handled before resuming.

 Asynchronous messages are exempt from synchronization barriers.  They typically
represent interrupts, input events, and other signals that must be handled independently
 even while other work has been suspended.

Note that asynchronous messages may be delivered out of order with respect to
   synchronous messages although they are always delivered in order among themselves.
If the relative order of these messages matters then they probably should not be
  asynchronous in the first place.  Use with caution.
同步屏障和异步消息有具体的使用场景吗？
使用场景就很多了，比如绘制方法scheduleTraversals
```
 /frameworks/base/core/java/android/view/ViewRootImpl.java
 void scheduleTraversals() {
    if (!mTraversalScheduled) {
        mTraversalScheduled = true;
        // 同步屏障，阻塞所有的同步消息
        mTraversalBarrier = mHandler.getLooper().getQueue().postSyncBarrier();
        // 通过 Choreographer 发送绘制任务
        mChoreographer.postCallback(
                Choreographer.CALLBACK_TRAVERSAL, mTraversalRunnable, null);
    }
}

mChoreographer.postCallback的实现
Message msg = mHandler.obtainMessage(MSG_DO_SCHEDULE_CALLBACK, action);
msg.arg1 = callbackType;
msg.setAsynchronous(true);
mHandler.sendMessageAtTime(msg, dueTime);
```
在该方法中加入了同步屏障，后续加入一个异步消息MSG_DO_SCHEDULE_CALLBACK，最后会执行到FrameDisplayEventReceiver，
用于申请VSYNC信号。


子线程访问UI的 崩溃原因 和 解决办法？
崩溃发生在ViewRootImpl类的checkThread方法中：
/frameworks/base/core/java/android/view/ViewRootImpl.java
```
public ViewRootImpl(Context context, Display display) {
  mThread = Thread.currentThread(); //ViewRootImpl在主线程创建的,mThread保存了主线程  //todo Thread.current为什么能获取到线程，线程切换了也可以。。
} 
void checkThread() {
      if (mThread != Thread.currentThread()) {
          throw new CalledFromWrongThreadException(
                  "Only the original thread that created a view hierarchy can touch its views.");
      }
  }
public void requestLayout() {
      if (!mHandlingLayoutInLayoutRequest) {
          checkThread();
          mLayoutRequested = true;
          scheduleTraversals();
      }
  }    
```
其实就是判断了当前线程 是否是 ViewRootImpl创建时候的线程，如果不是，就会崩溃。
而ViewRootImpl创建的时机就是界面被绘制的时候，也就是onResume之后，所以如果在子线程进行UI更新，就会发现当前线程（子线程）
和View创建的线程（主线程）不是同一个线程，发生崩溃。
解决办法有三种：
1、在新建视图的线程进行这个视图的UI更新，主线程创建View，主线程更新View。
2、在ViewRootImpl创建之前进行子线程的UI更新，比如onCreate方法中进行子线程更新UI。
3、子线程切换到主线程进行UI更新，比如Handler、view.post方法

todo
如何设计一个永不崩溃的app  //todo https://juejin.cn/post/6904283635856179214
我的理解是这样：loop方法中做的事情就是死循环获取消息队列中的消息并进行处理。 我们在已经开启死循环的Looper中加入一条消息
：Looper.loop，那么上一个Looper.loop的消息处理就会一直“卡”在新的消息——Looper.loop中，
以后的消息处理就会在新的Looper.loop中进行处理了。可以理解为把以前的消息处理从ActivityThread 中的Looper.loop()
转移到了新的Looper.loop()中了
```
Handler(Looper.getMainLooper()).post {
        while (true) {
            try {
                Looper.loop()
            } catch (e: Throwable) {
            }
        }
    }
```

Android6.0以后pipe更新为eventfd
MessageQueue没有消息时候会怎样？阻塞之后怎么唤醒呢？说说pipe/epoll机制？
当消息不可用或者没有消息的时候就会阻塞在next方法，而阻塞的办法是通过pipe/epoll机制。
epoll机制是一种IO多路复用的机制，具体逻辑就是一个进程可以监视多个描述符，当某个描述符就绪（一般是读就绪或者写就绪），
    能够通知程序进行相应的读写操作，这个读写操作是阻塞的。在Android中，会创建一个Linux管道（Pipe）来处理阻塞和唤醒。
当消息队列为空，管道的读端等待管道中有新内容可读，就会通过epoll机制进入阻塞状态。
当有消息要处理，就会通过管道的写端写入内容，唤醒主线程。



//冷门知识  稍后在看
//todo handler.runWithScissors    handler中mMessenger，binder相关
//handler.removeMessage    hasMessages   
//https://blog.csdn.net/hanshengjian/article/details/120398545
https://juejin.cn/post/6844904102963445767


todo
removeCallbacksAndMessages




//todo handler 问题  https://juejin.cn/post/6901682664617705485
主线程 Looper 什么时候退出循环
Handler 避免内存泄漏

//todo BlockCanary使用过吗？说说原理
https://mp.weixin.qq.com/s/63zHksIEEgm3PFNLtxIUrg


