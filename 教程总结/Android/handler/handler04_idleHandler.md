https://cloud.tencent.com/developer/article/1578511
https://blog.csdn.net/fhcxiaosa1995/article/details/107343319



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
                // 1.3 将空闲消息处理者集合转为数组 临时存储数据用于遍历执行
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

IdleHandler具有一定的不可控特性,例如UI线程的任务一直不执行完


https://mp.weixin.qq.com/s/gVUf4uy7kkukYKziRWN5Mg
IdleHandler的适用场景
轻量级任务：IdleHandler主要用于执行轻量级的任务。由于它是在主线程空闲时执行，所以不适合执行耗时的任务。
主线程空闲时执行：IdleHandler通过在主线程空闲时被调用，避免了主线程的阻塞。因此，适用于需要在主线程执行的任务，并且这些任务对于用户体验的影响较小。
优先级较低的任务：如果有多个任务注册了IdleHandler，系统会按照注册的顺序调用它们的queueIdle方法。因此，适用于需要在较低优先级下执行的任务。
总的来说IdleHandler适用于需要在主线程空闲时执行的轻量级任务，以提升应用的性能和用户体验。
高级应用
性能监控与优化利用 IdleHandler 可以实现性能监控和优化，例如统计每次空闲时的内存占用情况，或者执行一些内存释放操作。
预加载数据在用户操作前，通过 IdleHandler 提前加载一些可能会用到的数据，提高用户体验。
动态资源加载利用空闲时间预加载和解析资源，减轻在用户操作时的资源加载压力。
性能优化技巧
虽然IdleHandler提供了一个方便的机制来在主线程空闲时执行任务，但在使用过程中仍需注意一些性能方面的问题。
任务的轻量级处理: 确保注册的IdleHandler中的任务是轻量级的，不要在空闲时执行过于复杂或耗时的操作，以免影响主线程的响应性能。
避免频繁注册和取消IdleHandler: 频繁注册和取消IdleHandler可能会引起性能问题，因此建议在应用的生命周期内尽量减少注册和取消的操作。
   可以在应用启动时注册IdleHandler，在应用退出时取消注册。
合理设置任务执行频率: 根据任务的性质和执行需求，合理设置任务的执行频率。不同的任务可能需要在不同的时间间隔内执行，这样可以更好地平衡性能和功能需求。



IdleHandler的任务队列
https://mp.weixin.qq.com/s/pXabtyyg9JhbQY1ZA7Mu6w
```
public class DelayInitDispatcher {

    private Queue<Task> mDelayTasks = new LinkedList<>();

    private MessageQueue.IdleHandler mIdleHandler = new MessageQueue.IdleHandler() {
        @Override
        public boolean queueIdle() {
            if(mDelayTasks.size()>0){
                Task task = mDelayTasks.poll();
                new DispatchRunnable(task).run();
            }
            //返回true时会继续监听，返回false结束监听
            return !mDelayTasks.isEmpty();
        }
    };

    public DelayInitDispatcher addTask(Task task){
        mDelayTasks.add(task);
        return this;
    }

    public void start(){
        Looper.myQueue().addIdleHandler(mIdleHandler);
    }

}
//调用
DelayInitDispatcher delayInitDispatcher = new DelayInitDispatcher();
delayInitDispatcher.addTask(new DelayInitTaskA())
        .addTask(new DelayInitTaskB())
        .start();
```
