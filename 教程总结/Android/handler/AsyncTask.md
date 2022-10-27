
总结
1 使用AtomicInteger创建线程名字
2 使用Handler完成线程子线程到UI主线程的切换
3 使用FutureTask支持结果的获取，取消  
   AsyncTask初始化 futureTask使用WorkerRunnable，call时调用doInBackground
   执行完成回调onPostExecute，publishProgress需要在doInBackground中手动调用
  executeOnExecutor使用线程池执行future
4 默认串行执行，提供并发的线程池   如果有一个长时间耗时后面的没机会运行了
  串行执行的关键，将任务添加ArrayDeque的双向队列，任务执行完成，从队列取出，然后交由并发线程池去执行  
Runnable的逻辑就是运行完成后处理下一个
```
 private static class SerialExecutor implements Executor {
        final ArrayDeque<Runnable> mTasks = new ArrayDeque<Runnable>();
        Runnable mActive;
        public synchronized void execute(final Runnable r) {
            mTasks.offer(new Runnable() {
                public void run() {
                    try {
                        r.run();
                    } finally {
                        scheduleNext();
                    }
                }
            });
            if (mActive == null) {
                scheduleNext();
            }
        }
 
        protected synchronized void scheduleNext() {
            if ((mActive = mTasks.poll()) != null) {
                THREAD_POOL_EXECUTOR.execute(mActive);
            }
        }
    }
```

//todo
Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
Binder.flushPendingCommands();


缺点
https://blog.csdn.net/xuwenwen_2013/article/details/81808435
为什么不适合长耗时任务
1 AsyncTask的生命周期没有跟Activity的生命周期同步，activity销毁后，AsyncTask会继续运行，需要手动调用cancel方法，否则
容易崩溃(引用了view但是已经销毁)或者内存泄露
2 容易内存泄露
在Activity中作为内部类创建AsyncTask很方便。因为AsyncTask在执行完成或者执行中需要使用Activity中的view，因为内部类
可以直接访问外部类的域（也就是变量）。然而这意味着内部类会持有外部类的一个引用。
当长时间运行时，因为AsyncTask持有Activity的引用，所以即使当该Activity不再显示时Android也无法释放其占用的资源。
3.结果丢失
屏幕旋转或Activity在后台被系统杀掉等情况会导致Activity的重新创建，之前运行的AsyncTask会持有一个之前Activity的引用，
这个引用已经无效，这时调用onPostExecute()再去更新界面将不再生效。
4.并行还是串行  //版本兼容问题
在Android1.6之前的版本，AsyncTask是串行的，在1.6之后的版本，采用线程池处理并行任务，但是从Android 3.0开始，
为了避免AsyncTask所带来的并发错误，又采用一个线程来串行执行任务。可以使用executeOnExecutor()方法来并行地执行任务。



在api 30（Android 11)中AsyncTask被正式废弃
https://developer.android.com/reference/android/os/AsyncTask?hl=ja
被弃用后，Android给出了两个替代的建议：
java.util.concurrent包下的相关类，如Executor，ThreadPoolExecutor，FutureTask。
kotlin并发工具，那就是协程 - Coroutines了。
ThreadHandler也可以

```
AsyncTask was intended to enable proper and easy use of the UI thread. However, the most common use case was for integrating into UI,
  and that would cause Context leaks, missed callbacks, or crashes on configuration changes. 
  It also has inconsistent behavior on different versions of the platform, swallows exceptions from doInBackground, 
  and does not provide much utility over using Executors directly.

AysncTask意图提供简单且合适的UI线程使用，然而，它最主要的使用实例是作为UI的组成部分，会导致内存泄漏，回调遗失或改变配置时崩溃。
且它在不同版本的平台上有不一致的行为，吞下来自doInBackground的异常，并且不能提供比直接使用Executors更多的功能。
```


http://blog.csdn.net/singwhatiwanna/article/details/17596225
AsyncTask 原理解析
前言
什么是AsyncTask，相信搞过android开发的朋友们都不陌生。AsyncTask内部封装了Thread和Handler，可以让我们在后台进行计算并且把计算的结果
 及时更新到UI上，而这些正是Thread+Handler所做的事情，没错，AsyncTask的作用就是简化Thread+Handler，让我们能够通过更少的代码来
 完成一样的功能，这里，我要说明的是：AsyncTask只是简化Thread+Handler而不是替代，实际上它也替代不了。同时，AsyncTask从最开始到现在
 已经经过了几次代码修改，任务的执行逻辑慢慢地发生了改变，并不是大家所想象的那样：AsyncTask是完全并行执行的就像多个线程一样，
 其实不是的，所以用AsyncTask的时候还是要注意，下面会一一说明。另外本文主要是分析AsyncTask的源代码以及使用时候的一些注意事项
AsyncTask的一个例子
```
private class DownloadFilesTask extends AsyncTask<URL, Integer, Long> {
     protected Long doInBackground(URL... urls) {
         int count = urls.length;
         long totalSize = 0;
         for (int i = 0; i < count; i++) {
             totalSize += Downloader.downloadFile(urls[i]);
             //手动调用更新进度
             publishProgress((int) ((i / (float) count) * 100));
             // Escape early if cancel() is called
             if (isCancelled()) break;
         }
         return totalSize;
     }
 
     protected void onProgressUpdate(Integer... progress) {
         setProgressPercent(progress[0]);
     }
 
     protected void onPostExecute(Long result) {
         showDialog("Downloaded " + result + " bytes");
     }
 }
```
使用AsyncTask的规则
 1 AsyncTask的类必须在UI线程加载（从4.1开始系统会帮我们自动完成）
 2 AsyncTask对象必须在UI线程创建
 3 execute方法必须在UI线程调用
 4 不要在你的程序中去直接调用onPreExecute(), onPostExecute, doInBackground, onProgressUpdate方法
 5 一个AsyncTask对象只能执行一次，即只能调用一次execute方法，否则会报运行时异常   AsyncTask有执行状态，执行前进行了状态校验
 6 AsyncTask不是被设计为处理耗时操作的，耗时上限为几秒钟，如果要做长耗时操作，强烈建议你使用Executor，ThreadPoolExecutor以及FutureTask
 7 在1.6之前，AsyncTask是串行执行任务的，1.6的时候AsyncTask开始采用线程池里处理并行任务，但是从3.0开始，为了避免AsyncTask所带来的并发错误，
   AsyncTask又采用一个线程来串行执行任务

AsyncTask到底是串行还是并行？
给大家做一下实验，请看如下实验代码：代码很简单，就是点击按钮的时候同时执行5个AsyncTask，每个AsyncTask休眠3s，
   同时把每个AsyncTask执行结束的时间打印出来，这样我们就能观察出到底是串行执行还是并行执行。
```
    @Override
    public void onClick(View v) {
        if (v == mButton) {
            new MyAsyncTask("AsyncTask#1").execute("");
            new MyAsyncTask("AsyncTask#2").execute("");
            new MyAsyncTask("AsyncTask#3").execute("");
            new MyAsyncTask("AsyncTask#4").execute("");
            new MyAsyncTask("AsyncTask#5").execute("");
        }
 
    }
 
    private static class MyAsyncTask extends AsyncTask<String, Integer, String> {
        private String mName = "AsyncTask";
        public MyAsyncTask(String name) {
            super();
            mName = name;
        }

        @Override
        protected String doInBackground(String... params) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return mName;
        }
 
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Log.e(TAG, result + "execute finish at " + df.format(new Date()));
        }
    }
```
android 8.0执行结果   间隔3秒打印日志   默认串行执行任务
```
D/AsyncTask#1: AsyncTask#1execute finish at 2021-12-19 02:58:47
D/AsyncTask#2: AsyncTask#2execute finish at 2021-12-19 02:58:50
D/AsyncTask#3: AsyncTask#3execute finish at 2021-12-19 02:58:53
D/AsyncTask#4: AsyncTask#4execute finish at 2021-12-19 02:58:56
D/AsyncTask#5: AsyncTask#5execute finish at 2021-12-19 02:58:59
```

并行代码  手动指定用于并发的Executor
```
MyAsyncTask("AsyncTask#1").executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"")
MyAsyncTask("AsyncTask#2").executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"")
MyAsyncTask("AsyncTask#3").executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"")
MyAsyncTask("AsyncTask#4").executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"")
MyAsyncTask("AsyncTask#5").executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"")
```
运行结果Android 8.0   3个完成在11秒，2个完成在14秒
```
D/AsyncTask#1: AsyncTask#1execute finish at 2021-12-19 06:20:11
D/AsyncTask#3: AsyncTask#3execute finish at 2021-12-19 06:20:11
D/AsyncTask#2: AsyncTask#2execute finish at 2021-12-19 06:20:11
D/AsyncTask#4: AsyncTask#4execute finish at 2021-12-19 06:20:14
D/AsyncTask#5: AsyncTask#5execute finish at 2021-12-19 06:20:14
```


源码分析  
初始化参数
```
public abstract class AsyncTask<Params, Progress, Result> {
    //线程池核心容量
    private static final int CORE_POOL_SIZE = 1;
    //线程池最大容量
    private static final int MAXIMUM_POOL_SIZE = 20;
    // 备份线程池大小
    private static final int BACKUP_POOL_SIZE = 5;
    //过剩的空闲线程的存活时间
    private static final int KEEP_ALIVE_SECONDS = 3;
    //创建线程名称  AtomicInteger使用原子整数保证并发和性能
    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            return new Thread(r, "AsyncTask #" + mCount.getAndIncrement());
        }
    };
    
    //线程池中任务执行失败 使用备份线程池和其无界队列
    private static ThreadPoolExecutor sBackupExecutor;
    //无界队列
    private static LinkedBlockingQueue<Runnable> sBackupExecutorQueue;

    //线程池拒绝策略
    private static final RejectedExecutionHandler sRunOnSerialPolicy =
            new RejectedExecutionHandler() {
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {           
            synchronized (this) {
                if (sBackupExecutor == null) {
                    //被拒绝后使用备用线程池重新执行
                    sBackupExecutorQueue = new LinkedBlockingQueue<Runnable>();
                    sBackupExecutor = new ThreadPoolExecutor(
                            BACKUP_POOL_SIZE, BACKUP_POOL_SIZE, KEEP_ALIVE_SECONDS,
                            TimeUnit.SECONDS, sBackupExecutorQueue, sThreadFactory);
                    //被拒绝后运行核心线程超时        
                    sBackupExecutor.allowCoreThreadTimeOut(true);
                }
            }
            sBackupExecutor.execute(r);
        }
    };
    
   //注意线程池是静态的，多个AsyncTask之间共享线程池  
  // 静态并发线程池，可以用来并行执行任务，尽管从3.0开始，AsyncTask默认是串行执行任务
  // 但是我们仍然能构造出并行的AsyncTask
   public static final Executor THREAD_POOL_EXECUTOR;
    static {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(), sThreadFactory);
        threadPoolExecutor.setRejectedExecutionHandler(sRunOnSerialPolicy);
        THREAD_POOL_EXECUTOR = threadPoolExecutor;
    } 
    //静态串行任务执行器，其内部实现了串行控制，
	// 循环的取出一个个任务交给上述的并发线程池去执行
    public static final Executor SERIAL_EXECUTOR = new SerialExecutor();
    //默认线程池
    private static volatile Executor sDefaultExecutor = SERIAL_EXECUTOR;
    
    private static final int MESSAGE_POST_RESULT = 0x1;
    private static final int MESSAGE_POST_PROGRESS = 0x2;
    //主线程的handler  可知finish和onProgressUpdate运行在主线程
    //静态Handler，用来发送上述两种通知，采用UI线程的Looper来处理消息
	 //这就是为什么AsyncTask必须在UI线程调用，因为子线程
	 //默认没有Looper无法创建下面的Handler，程序会直接Crash
    private static InternalHandler sHandler;
    //默认为sHandler  sHandler是单例的
    private final Handler mHandler;
    
    //mWorker是FutureTask执行的任务
    private final WorkerRunnable<Params, Result> mWorker;
    //使用FutureTask 来支持任务的结果获取，取消
    private final FutureTask<Result> mFuture;
    //任务状态 默认是挂起 使用volatile刷新状态
    private volatile Status mStatus = Status.PENDING;
    //任务是否取消
    private final AtomicBoolean mCancelled = new AtomicBoolean();
    //任务是否被执行过
    private final AtomicBoolean mTaskInvoked = new AtomicBoolean(); 
    
     //任务状态 任务等待执行，运行中，执行完成
     public enum Status {
        PENDING,
        RUNNING,
        FINISHED,
    }
    
    private static abstract class WorkerRunnable<Params, Result> implements Callable<Result> {
        Params[] mParams;
    }
    
    private static Handler getMainHandler() {
        synchronized (AsyncTask.class) {
            if (sHandler == null) {
                sHandler = new InternalHandler(Looper.getMainLooper());
            }
            return sHandler;
        }
    }

    private Handler getHandler() {
        return mHandler;
    }

    //隐藏API 
    public static void setDefaultExecutor(Executor exec) {
        sDefaultExecutor = exec;
    }
   
    //构造器初始化
    public AsyncTask() {
        this((Looper) null);
    }
    public AsyncTask(@Nullable Handler handler) {
        this(handler != null ? handler.getLooper() : null);
    }   
   public AsyncTask(@Nullable Looper callbackLooper) {
        //默认为sHandler = new InternalHandler()
        mHandler = callbackLooper == null || callbackLooper == Looper.getMainLooper()
            ? getMainHandler()
            : new Handler(callbackLooper);
        //初始化FutureTask及其任务
        mWorker = new WorkerRunnable<Params, Result>() {
            public Result call() throws Exception {
                mTaskInvoked.set(true);
                Result result = null;
                try {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                    result = doInBackground(mParams);
                    Binder.flushPendingCommands();
                } catch (Throwable tr) {
                    mCancelled.set(true);
                    throw tr;
                } finally {
                    //任务执行完 发送消息MESSAGE_POST_RESULT，然后调用finish方法
                    postResult(result);
                }
                return result;
            }
        };

        mFuture = new FutureTask<Result>(mWorker) {
            @Override
            protected void done() {
                try {
                    postResultIfNotInvoked(get());
                } catch (InterruptedException e) {
                    android.util.Log.w(LOG_TAG, e);
                } catch (ExecutionException e) {
                    throw new RuntimeException("An error occurred while executing doInBackground()",
                            e.getCause());
                } catch (CancellationException e) {
                    postResultIfNotInvoked(null);
                }
            }
        };
    }
 
    private void postResultIfNotInvoked(Result result) {
        final boolean wasTaskInvoked = mTaskInvoked.get();
        if (!wasTaskInvoked) {
            postResult(result);
        }
    }
    
       //doInBackground执行完毕，发送消息
  private Result postResult(Result result) {
        @SuppressWarnings("unchecked")
        Message message = getHandler().obtainMessage(MESSAGE_POST_RESULT,
                new AsyncTaskResult<Result>(this, result));
        message.sendToTarget();
        return result;
    } 
    
    
   //处理消息的主线程handler   实现子线程到UI线程的切换 
   private static class InternalHandler extends Handler {
        public InternalHandler(Looper looper) {
            super(looper);
        }
        public void handleMessage(Message msg) {
            AsyncTaskResult<?> result = (AsyncTaskResult<?>) msg.obj;
            switch (msg.what) {
                case MESSAGE_POST_RESULT:
                    result.mTask.finish(result.mData[0]);
                    break;
                case MESSAGE_POST_PROGRESS:
                    result.mTask.onProgressUpdate(result.mData);
                    break;
            }
        }
    }  
}
```
核心逻辑 
```
  //这段代码在Executor的注释中也有
 //串行执行器的实现，我们要好好看看，它是怎么把并行转为串行的
	//目前我们需要知道，asyncTask.execute(Params ...)实际上会调用
	 //SerialExecutor的execute方法，这一点后面再说明。也就是说：当你的asyncTask执行的时候，
	 //首先你的task会被加入到任务队列，然后排队，一个个执行
    private static class SerialExecutor implements Executor {
		//线性双向队列，用来存储所有的AsyncTask任务   Runnable的逻辑就是运行完成后处理下一个
        final ArrayDeque<Runnable> mTasks = new ArrayDeque<Runnable>();
		//当前正在执行的AsyncTask任务
        Runnable mActive;
 
        public synchronized void execute(final Runnable r) {
			//将新的AsyncTask任务加入到双向队列中
            mTasks.offer(new Runnable() {
                public void run() {
                    try {
						//执行AsyncTask任务
                        r.run();
                    } finally {
						//当前AsyncTask任务执行完毕后，进行下一轮执行，如果还有未执行任务的话
						//这一点很明显体现了AsyncTask是串行执行任务的，总是一个任务执行完毕才会执行下一个任务
                        scheduleNext();
                    }
                }
            });
			//如果当前没有任务在执行，直接进入执行逻辑
            if (mActive == null) {
                scheduleNext();
            }
        }
 
        protected synchronized void scheduleNext() {
			//从任务队列中取出队列头部的任务，如果有就交给并发线程池去执行
            if ((mActive = mTasks.poll()) != null) {
                THREAD_POOL_EXECUTOR.execute(mActive);
            }
        }
    }

  //在doInBackground之前调用，用来做初始化工作 所在线程：UI线程  
  protected void onPreExecute() {}  
  //这个方法是我们必须要重写的，用来做后台计算 所在线程：后台线程
  protected abstract Result doInBackground(Params... params);
  // 在doInBackground之后调用，用来接受后台计算结果更新UI 所在线程：UI线程
  protected void onPostExecute(Result result) {} 
  //调用publishProgress之后才会调用，用来更新计算进度 所在线程：UI线程 
  protected void onProgressUpdate(Progress... values) {} 
  //任务取消
  public final boolean cancel(boolean mayInterruptIfRunning) {
        mCancelled.set(true);
        return mFuture.cancel(mayInterruptIfRunning);
  }
   //cancel被调用并且doInBackground执行结束，会调用onCancelled，表示任务被取消
   //这个时候onPostExecute不会再被调用，二者是互斥的，分别表示任务取消和任务执行完成
    //所在线程：UI线程
    protected void onCancelled(Result result) {
        onCancelled();
    } 
   //发布进度 
   protected final void publishProgress(Progress... values) {
        if (!isCancelled()) {
            getHandler().obtainMessage(MESSAGE_POST_PROGRESS,
                    new AsyncTaskResult<Progress>(this, values)).sendToTarget();
        }
    }  
   // 任务结束的时候会进行判断，如果任务没有被取消，则onPostExecute会被调用
   private void finish(Result result) {
        if (isCancelled()) {
            onCancelled(result);
        } else {
            onPostExecute(result);
        }
        mStatus = Status.FINISHED;
    }


 /**
     * 这个方法如何执行和系统版本有关，在AsyncTask的使用规则里已经说明，如果你真的想使用并行AsyncTask，
	 * 也是可以的，只要稍作修改
	 * 必须在UI线程调用此方法
     */
    public final AsyncTask<Params, Progress, Result> execute(Params... params) {
		//串行执行
        return executeOnExecutor(sDefaultExecutor, params);
		//如果我们想并行执行，这样改就行了，当然这个方法我们没法改
		//return executeOnExecutor(THREAD_POOL_EXECUTOR, params);
    }

/**
     * 通过这个方法我们可以自定义AsyncTask的执行方式，串行or并行，甚至可以采用自己的Executor
	 * 为了实现并行，我们可以在外部这么用AsyncTask：
	 * asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, Params... params);
	 * 必须在UI线程调用此方法
     */
    public final AsyncTask<Params, Progress, Result> executeOnExecutor(Executor exec,
            Params... params) {
        if (mStatus != Status.PENDING) {
            switch (mStatus) {
                case RUNNING:
                    throw new IllegalStateException("Cannot execute task:"
                            + " the task is already running.");
                case FINISHED:
                    throw new IllegalStateException("Cannot execute task:"
                            + " the task has already been executed "
                            + "(a task can be executed only once)");
            }
        }
 
        mStatus = Status.RUNNING;
		//这里#onPreExecute会最先执行
        onPreExecute();
 
        mWorker.mParams = params;
		//然后后台计算#doInBackground才真正开始
        exec.execute(mFuture);
		//接着会有#onProgressUpdate被调用(需要手动调用publishProgress)，最后是#onPostExecute
 
        return this;
    }
    
    //这是AsyncTask提供的一个静态方法，方便我们直接执行一个runnable
    public static void execute(Runnable runnable) {
        sDefaultExecutor.execute(runnable);
    }

```

