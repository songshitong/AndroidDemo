https://cloud.tencent.com/developer/article/1578511
https://blog.csdn.net/fhcxiaosa1995/article/details/107343319
//todo GCIdler

if (mPendingIdleHandlers == null) {
mPendingIdleHandlers = new IdleHandler[Math.max(pendingIdleHandlerCount, 4)];
}
mPendingIdleHandlers = mIdleHandlers.toArray(mPendingIdleHandlers);
//todo mIdleHandlers为什么转为array

idle [ˈaɪdl] adj. 懈怠的;懒惰的;闲置的;没有工作的;闲散的  v. 混时间;闲荡;无所事事;空转;挂空挡;未熄火;(尤指暂时地)关闭工厂，使(工人)闲着

/frameworks/base/core/java/android/os/MessageQueue.java
```
public final class MessageQueue {

    // 空闲消息集合
    private final ArrayList<IdleHandler> mIdleHandlers = new ArrayList<IdleHandler>();
    // 空闲消息处理者的数组
    private IdleHandler[] mPendingIdleHandlers;
    
    Message next() {
        ...... 
        for (;;) {
            ......
            synchronized (this) {
                // 省略获取 msg 的代码
                ......
                // 1. 获取空闲消息
                // 1.1 从空闲消息集合 mIdleHandlers 中获取 空闲处理者 数量
                if (pendingIdleHandlerCount < 0
                        && (mMessages == null || now < mMessages.when)) {
                    pendingIdleHandlerCount = mIdleHandlers.size();
                }
                // 1.2 若无空闲处理者, 则进行下一次 for 循环
                if (pendingIdleHandlerCount <= 0) {
                    mBlocked = true;
                    continue;
                }
                ......
                // 1.3 将空闲消息处理者集合转为数组
                mPendingIdleHandlers = mIdleHandlers.toArray(mPendingIdleHandlers);
            }

            // 2. 处理空闲消息
            for (int i = 0; i < pendingIdleHandlerCount; i++) {
                // 获取第 i 给位置的空闲处理者
                final IdleHandler idler = mPendingIdleHandlers[i];
                mPendingIdleHandlers[i] = null; // 置空
                boolean keep = false;        
                try {
                    // 2.1 处理空闲消息
                    keep = idler.queueIdle(); 
                } catch (Throwable t) {
                    ......
                }
                if (!keep) {   
                    synchronized (this) {
                        // 2.2 走到这里表示它是一次性的处理者, 从 mIdleHandlers 移除
                        mIdleHandlers.remove(idler);
                    }
                }
            }
            ......
        }
    }
    
}
```

好的, 可以看到 MessageQueue.next 在获取不到 msg 时, 会进行一些空闲消息的处理

收集空闲消息
从空闲消息集合 mIdleHandlers 中获取 空闲处理者 数量
   若无空闲处理者, 则进行下一次 for 循环
   若存在空闲处理者, 则空闲消息处理者集合转为数组 mPendingIdleHandlers
处理空闲消息
调用 IdleHandler.queueIdle 处理空闲消息
  返回 true, 下次再 MessageQueue.next 获取不到 msg 的空闲时间会继续处理
   返回 false 表示它是一次性的处理者, 从 mIdleHandlers 移除
空闲处理者的添加方式如下
```
public final class MessageQueue {
    //Callback interface for discovering when a thread is going to block waiting for more messages
    public static interface IdleHandler {
        /**
         * 处理空闲消息  结果keep false表示执行完移除   ture表示一直存在，需要自己处理
         */
        boolean queueIdle();
    }
    // 空闲消息集合
    private final ArrayList<IdleHandler> mIdleHandlers = new ArrayList<IdleHandler>();
    public void addIdleHandler(@NonNull IdleHandler handler) {
        synchronized (this) {
            mIdleHandlers.add(handler);
        }
    }
    public void removeIdleHandler(@NonNull IdleHandler handler) {
          synchronized (this) {
              mIdleHandlers.remove(handler);
          }
      }
    
}
```

通过上述代码可以得到以下的信息
空闲处理者使用 IdleHandler 接口描述
空闲处理者通过 MessageQueue.addIdleHandler() 添加
空闲处理者使用 MessageQueue.mIdleHandlers 维护


IdleHandler就是当消息队列里面没有当前要处理的消息了，需要堵塞之前，可以做一些空闲任务的处理。
常见的使用场景有：启动优化。
我们一般会把一些事件（比如界面view的绘制、赋值）放到onCreate方法或者onResume方法中。但是这两个方法其实都是在界面绘制之前调用的，
也就是说一定程度上这两个方法的耗时会影响到启动时间。
所以我们可以把一些操作放到IdleHandler中，也就是界面绘制完成之后才去调用，这样就能减少启动时间了。
但是，这里需要注意下可能会有坑。
如果使用不当，IdleHandler会一直不执行，比如在View的onDraw方法里面无限制的直接或者间接调用View的invalidate方法。
 其原因就在于onDraw方法中执行invalidate，会添加一个同步屏障消息，在等到异步消息之前，会阻塞在next方法，
 而等到FrameDisplayEventReceiver异步任务之后又会执行onDraw方法，从而无限循环。
//todo https://mp.weixin.qq.com/s/dh_71i8J5ShpgxgWN5SPEw
