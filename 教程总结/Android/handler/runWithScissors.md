
https://www.51cto.com/article/621768.html
runWithScissors() 是 Handler 的一个方法，被标记为 @hide，不允许普通开发者调用
注释解释：同步执行特定的task，当前线程直接执行，其他线程将任务推到handler线程并且等待执行完成(可能超时)

应用场景
1 刚刚设置handler thread，并且执行后续任务前，需要一些初始化操作  01_WMS的诞生.md有使用到
2 面试题：如何在子线程通过 Handler 向主线程发送一个任务，并等主线程处理此任务后，再继续执行?

执行流程
1 当前线程，直接运行，不进入handler的message queue
2 非当前线程，将任务post到对应的handler(handler.post(this))，post失败，返回false    post失败通常因为 message queue退出了或退出中
3 post成功，任务执行，同时当前线程等待
4 任务执行完成，唤醒等待线程；任务超时，返回false

存在的问题：
1 如果超时了，没有取消的逻辑
通过 runWithScissors() 发送 Runnable 时，可以指定超时时间。当超时唤醒时，是直接 false 退出。
当超时退出时，这个 Runnable 依然还在目标线程的 MessageQueue 中，没有被移除掉，它最终还是会被 Handler 线程调度并执行。
此时的执行，显然并不符合我们的业务预期
2 可能造成死锁
使用 runWithScissors() 可能造成调用线程进入阻塞，而得不到唤醒，如果当前持有别的锁，还会造成死锁。
我们通过 Handler 发送的 MessageQueue 的消息，一般都会得到执行，而当线程 Looper 通过 quit() 退出时，会清理掉还未执行的任务，此时发送线程，则永远得不到唤醒。
那么在使用 runWithScissors() 时，就要求 Handler 所在的线程 Looper，不允许退出，或者使用 quitSafely() 方式退出。
quit() 和 quitSafely() 都表示退出，会去清理对应的 MessageQueue，区别在于，qiut() 会清理 MessageQueue 中所有的消息，而 quitSafely() 只会清理掉当前时间点之后(when > now)的消息，当前时间之前的消息，依然会得到执行。
那么只要使用 quitSafely() 退出，通过 runWithScissors() 发送的任务，依然会被执行。
也就是说，安全使用 runWithScissors() 要满足 2 个条件：
Handler 的 Looper 不允许退出，例如 Android 主线程 Looper 就不允许退出;
Looper 退出时，使用安全退出 quitSafely() 方式退出;

代码分析
android-32\android\os\Handler.java   timeout为0，一直等待
```
 public final boolean runWithScissors(@NonNull Runnable r, long timeout) {
        ...
        if (Looper.myLooper() == mLooper) {
            r.run();
            return true;
        }

        BlockingRunnable br = new BlockingRunnable(r);
        return br.postAndWait(this, timeout);
    }
    

 private static final class BlockingRunnable implements Runnable {
        private final Runnable mTask;
        private boolean mDone;

        public BlockingRunnable(Runnable task) {
            mTask = task;
        }

        @Override
        public void run() {
            try {
                mTask.run();
            } finally {
                synchronized (this) { //任务完成，唤醒线程
                    mDone = true;
                    notifyAll();
                }
            }
        }

        public boolean postAndWait(Handler handler, long timeout) {
            if (!handler.post(this)) { //推到handler执行
                return false;
            }

            synchronized (this) {
                if (timeout > 0) {
                    final long expirationTime = SystemClock.uptimeMillis() + timeout; //预计超时时间
                    while (!mDone) {
                        long delay = expirationTime - SystemClock.uptimeMillis(); //还有多久超时
                        if (delay <= 0) {
                            return false; // 任务超时，返回false
                        }
                        try {
                            wait(delay);
                        } catch (InterruptedException ex) {
                        }
                    }
                } else {
                    while (!mDone) {
                        try {
                            wait();
                        } catch (InterruptedException ex) {
                        }
                    }
                }
            }
            return true;
        }
    }    
```