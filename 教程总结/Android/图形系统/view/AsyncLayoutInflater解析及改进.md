https://mp.weixin.qq.com/s/wmFBPYkwGh8ijQMODF27Fw    布局异步加载    android8.0

布局加载的两大性能瓶颈，通过IO操作将XML加载到内存中并进行解析和通过反射创建View

当xml文件过大或页面文件过深，布局的加载就会较为耗时。
我们知道，当主线程进行一些耗时操作可能就会导致页面卡顿，更严重的可能会产生ANR，所以我们能如何来进行布局加载优化呢？

解决这个问题有两种思路，直接解决和侧面缓解。
直接解决就是不使用IO和反射等技术   flutter，android x2c
侧面缓解的就是既然耗时操作难以避免，那我们能不能把耗时操作放在子线程中，等到inflate操作完成后再将结果回调到主线程呢？
答案当然是可以的，Android为我们提供了AsyncLayoutInflater类来进行异步布局加载


AsyncLayoutInflater用法
AsyncLayoutInflater的使用非常简单，就是把setContentView和一些view的初始化操作都放到了onInflateFinished回调中:
```
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    new AsyncLayoutInflater(this).inflate(R.layout.activity_main,null, new AsyncLayoutInflater.OnInflateFinishedListener(){
        @Override
        public void onInflateFinished(View view, int resid, ViewGroup parent) {
            setContentView(view);
            rv = findViewById(R.id.tv_right);
            rv.setLayoutManager(new V7LinearLayoutManager(MainActivity.this));
            rv.setAdapter(new RightRvAdapter(MainActivity.this));
        }
    });
}
```


AsyncLayoutInflater源码分析


AsyncLayoutInflater的源码非常短，也比较容易理解，总共只有170行左右。


AsyncLayoutInflater构造方法和初始化
构造方法中做了三件事件
 1 创建BasicInflater LayoutInflater子类，解析加载view
 2 创建Handler    //该handler是主线程，用于接收InflateThread解析的结果
 3 创建InflateThread  子线程  使用ArrayBlockingQueue的生产消费者进行单线程处理

inflate方法创建一个InflateRequest对象，并将resid、parent、callback等变量存储到这个对象中，并调用enqueue方法向队列中添加一个请求：
```
public final class AsyncLayoutInflater {
    private static final String TAG = "AsyncLayoutInflater";

    LayoutInflater mInflater;
    Handler mHandler;
    InflateThread mInflateThread;

    public AsyncLayoutInflater(@NonNull Context context) {
        mInflater = new BasicInflater(context);
        mHandler = new Handler(mHandlerCallback);
        mInflateThread = InflateThread.getInstance();
    }

    @UiThread
    public void inflate(@LayoutRes int resid, @Nullable ViewGroup parent,
            @NonNull OnInflateFinishedListener callback) {
        if (callback == null) {
            throw new NullPointerException("callback argument may not be null!");
        }
        InflateRequest request = mInflateThread.obtainRequest();
        request.inflater = this;
        request.resid = resid;
        request.parent = parent;
        request.callback = callback;
        mInflateThread.enqueue(request);
    }
        ....
}
```


InflateThread
这个类的主要作用就是创建一个子线程，将inflate请求添加到阻塞队列中，并按顺序执行BasicInflater.inflate操作（BasicInflater实际上就是LayoutInflater的子类）。
不管inflate成功或失败后，都会将request消息发送给主线程做处理   
```
private static class InflateThread extends Thread {
    private static final InflateThread sInstance;
    static {
        sInstance = new InflateThread();
        sInstance.start();
    }

    public static InflateThread getInstance() {
        return sInstance;
    }
        //生产者-消费者模型，阻塞队列
    private ArrayBlockingQueue<InflateRequest> mQueue = new ArrayBlockingQueue<>(10);
    //使用了对象池来缓存InflateThread对象，减少对象重复多次创建，避免内存抖动
    private SynchronizedPool<InflateRequest> mRequestPool = new SynchronizedPool<>(10);

    public void runInner() {
        InflateRequest request;
        try {
            //从队列中取出一条请求，如果没有则阻塞
            request = mQueue.take();
        } catch (InterruptedException ex) {
            // Odd, just continue
            Log.w(TAG, ex);
            return;
        }

        try {
            //inflate操作（通过调用BasicInflater类）
            request.view = request.inflater.mInflater.inflate(
                    request.resid, request.parent, false);
        } catch (RuntimeException ex) {
            // 回退机制：如果inflate失败，回到主线程去inflate
            Log.w(TAG, "Failed to inflate resource in the background! Retrying on the UI"
                    + " thread", ex);
        }
        //inflate成功或失败，都将request发送到主线程去处理
        Message.obtain(request.inflater.mHandler, 0, request)
                .sendToTarget();
    }

    @Override
    public void run() {
        //死循环（实际不会一直执行，内部是会阻塞等待的）
        while (true) {
            runInner();
        }
    }

    //从对象池缓存中取出一个InflateThread对象
    public InflateRequest obtainRequest() {
        InflateRequest obj = mRequestPool.acquire();
        if (obj == null) {
            obj = new InflateRequest();
        }
        return obj;
    }

    //对象池缓存中的对象的数据清空，便于对象复用
    public void releaseRequest(InflateRequest obj) {
        obj.callback = null;
        obj.inflater = null;
        obj.parent = null;
        obj.resid = 0;
        obj.view = null;
        mRequestPool.release(obj);
    }

    //将inflate请求添加到ArrayBlockingQueue（阻塞队列）中
    public void enqueue(InflateRequest request) {
        try {
            mQueue.put(request);
        } catch (InterruptedException e) {
            throw new RuntimeException(
                    "Failed to enqueue async inflate request", e);
        }
    }
}
```

BasicInflater   作用类似于PhoneLayoutInflater
BasicInflater 继承自 LayoutInflater，只是覆写了 onCreateView：优先加载这三个前缀的 Layout，然后才按照默认的流程去加载，
  因为大多数情况下我们 Layout 中使用的View都在这三个 package 下
```
private static class BasicInflater extends LayoutInflater {
    private static final String[] sClassPrefixList = {
        "android.widget.",
        "android.webkit.",
        "android.app."
    };

    BasicInflater(Context context) {
        super(context);
    }

    @Override
    public LayoutInflater cloneInContext(Context newContext) {
        return new BasicInflater(newContext);
    }

    @Override
    protected View onCreateView(String name, AttributeSet attrs) throws ClassNotFoundException {
        for (String prefix : sClassPrefixList) {
            try {
                //优先加载"android.widget.”、 "android.webkit."、"android.app."
                View view = createView(name, prefix, attrs);
                if (view != null) {
                    return view;
                }
            } catch (ClassNotFoundException e) {
            }
        }

        return super.onCreateView(name, attrs);
    }
}
```

mHandlerCallback
这里就是在主线程中handleMessage的操作，这里有一个回退机制，就是当子线程中inflate失败后，会继续再主线程中进行inflate操作，
 最终通过OnInflateFinishedListener接口将view回调到主线程
```
private Callback mHandlerCallback = new Callback() {
    @Override
    public boolean handleMessage(Message msg) {
        InflateRequest request = (InflateRequest) msg.obj;
        if (request.view == null) {
            //view == null说明inflate失败
            //继续再主线程中进行inflate操作
            request.view = mInflater.inflate(
                    request.resid, request.parent, false);
        }
        //回调到主线程
        request.callback.onInflateFinished(
                request.view, request.resid, request.parent);
        mInflateThread.releaseRequest(request);
        return true;
    }
};
```


AsyncLayoutInflater的局限性及改进
使用AsyncLayoutInflate主要有如下几个局限性：
1 所有构建的View中必须不能直接使用 Handler 或者是调用 Looper.myLooper()，因为异步线程默认没有调用 Looper.prepare ()    
  //说的是handler是主线程的吧
2 异步转换出来的 View 并没有被加到 parent view中，AsyncLayoutInflater 是调用了 LayoutInflater.inflate(int, ViewGroup, false)，
   因此如果需要加到 parent view 中，就需要我们自己手动添加；
3 AsyncLayoutInflater 不支持设置 LayoutInflater.Factory 或者 LayoutInflater.Factory2
4 同时缓存队列默认 10 的大小限制如果超过了10个则会导致主线程的等待
5 使用单线程来做全部的 inflate 工作，如果一个界面中 layout 很多不一定能满足需求


那我们如何来解决这些问题呢？AsyncLayoutInflate类修饰为 final ，所以不能通过继承重写父类来实现。
庆幸的是AsyncLayoutInflate的代码非常短而且相对简单，所以我们可以直接把AsyncLayoutInflate的代码复制出来一份，然后在这基础之上进行改进优化。
接下来我们主要从两个方面来进行优化
1 引入线程池，减少单线程等待
2 手动设置setFactory2


直接上代码   
```
public class AsyncLayoutInflatePlus {
    private static final String TAG = "AsyncLayoutInflatePlus";
    private Pools.SynchronizedPool<InflateRequest> mRequestPool = new Pools.SynchronizedPool<>(10);

    LayoutInflater mInflater;
    Handler mHandler;
    Dispather mDispatcher;


    public AsyncLayoutInflatePlus(@NonNull Context context) {
        mInflater = new BasicInflater(context);
        mHandler = new Handler(mHandlerCallback);
        mDispatcher = new Dispather();
    }

    @UiThread
    public void inflate(@LayoutRes int resid, @Nullable ViewGroup parent,
                        @NonNull OnInflateFinishedListener callback) {
        if (callback == null) {
            throw new NullPointerException("callback argument may not be null!");
        }
        InflateRequest request = obtainRequest();
        request.inflater = this;
        request.resid = resid;
        request.parent = parent;
        request.callback = callback;
        mDispatcher.enqueue(request);
    }

    private Handler.Callback mHandlerCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            InflateRequest request = (InflateRequest) msg.obj;
            if (request.view == null) {
                request.view = mInflater.inflate(
                        request.resid, request.parent, false);
            }
            request.callback.onInflateFinished(
                    request.view, request.resid, request.parent);
            releaseRequest(request);
            return true;
        }
    };

    public interface OnInflateFinishedListener {
        void onInflateFinished(@NonNull View view, @LayoutRes int resid,
                               @Nullable ViewGroup parent);
    }

    private static class InflateRequest {
        AsyncLayoutInflatePlus inflater;
        ViewGroup parent;
        int resid;
        View view;
        OnInflateFinishedListener callback;

        InflateRequest() {
        }
    }


    private static class Dispather {

        //获得当前CPU的核心数
        private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
        //设置线程池的核心线程数2-4之间,但是取决于CPU核数
        private static final int CORE_POOL_SIZE = Math.max(2, Math.min(CPU_COUNT - 1, 4));
        //设置线程池的最大线程数为 CPU核数 * 2 + 1
        private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
        //设置线程池空闲线程存活时间30s
        private static final int KEEP_ALIVE_SECONDS = 30;

        private static final ThreadFactory sThreadFactory = new ThreadFactory() {
            private final AtomicInteger mCount = new AtomicInteger(1);

            public Thread newThread(Runnable r) {
                return new Thread(r, "AsyncLayoutInflatePlus #" + mCount.getAndIncrement());
            }
        };

        //LinkedBlockingQueue 默认构造器，队列容量是Integer.MAX_VALUE
       // private static final BlockingQueue<Runnable> sPoolWorkQueue =
       //         new LinkedBlockingQueue<Runnable>();
        //缓冲池大小改为50, 防止LinkedBlockingQueue的内存溢出
        private static final BlockingQueue<Runnable> sPoolWorkQueue = new ArrayBlockingQueue<>(50);
        /**
         * An {@link Executor} that can be used to execute tasks in parallel.
         */
        public static final ThreadPoolExecutor THREAD_POOL_EXECUTOR;

        static {
            Log.i(TAG, "static initializer: " + " CPU_COUNT = " + CPU_COUNT + " CORE_POOL_SIZE = " + CORE_POOL_SIZE + " MAXIMUM_POOL_SIZE = " + MAXIMUM_POOL_SIZE);
            ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                    CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                    sPoolWorkQueue, sThreadFactory);
            threadPoolExecutor.allowCoreThreadTimeOut(true);
            THREAD_POOL_EXECUTOR = threadPoolExecutor;
        }

        public void enqueue(InflateRequest request) {
            THREAD_POOL_EXECUTOR.execute((new InflateRunnable(request)));

        }

    }

    private static class BasicInflater extends LayoutInflater {
        private static final String[] sClassPrefixList = {
                "android.widget.",
                "android.webkit.",
                "android.app."
        };

        BasicInflater(Context context) {
            super(context);
            if (context instanceof AppCompatActivity) {
                // 手动setFactory2，兼容AppCompatTextView等控件
                AppCompatDelegate appCompatDelegate = ((AppCompatActivity) context).getDelegate();
                if (appCompatDelegate instanceof LayoutInflater.Factory2) {
                    LayoutInflaterCompat.setFactory2(this, (LayoutInflater.Factory2) appCompatDelegate);
                }
            }
        }

        @Override
        public LayoutInflater cloneInContext(Context newContext) {
            return new BasicInflater(newContext);
        }

        @Override
        protected View onCreateView(String name, AttributeSet attrs) throws ClassNotFoundException {
            for (String prefix : sClassPrefixList) {
                try {
                    View view = createView(name, prefix, attrs);
                    if (view != null) {
                        return view;
                    }
                } catch (ClassNotFoundException e) {
                    // In this case we want to let the base class take a crack
                    // at it.
                }
            }

            return super.onCreateView(name, attrs);
        }
    }


    private static class InflateRunnable implements Runnable {
        private InflateRequest request;
        private boolean isRunning;

        public InflateRunnable(InflateRequest request) {
            this.request = request;
        }

        @Override
        public void run() {
            isRunning = true;
            try {
                request.view = request.inflater.mInflater.inflate(
                        request.resid, request.parent, false);
            } catch (RuntimeException ex) {
                // Probably a Looper failure, retry on the UI thread
                Log.w(TAG, "Failed to inflate resource in the background! Retrying on the UI"
                        + " thread", ex);
            }
            Message.obtain(request.inflater.mHandler, 0, request)
                    .sendToTarget();
        }

        public boolean isRunning() {
            return isRunning;
        }
    }


    public InflateRequest obtainRequest() {
        InflateRequest obj = mRequestPool.acquire();
        if (obj == null) {
            obj = new InflateRequest();
        }
        return obj;
    }

    public void releaseRequest(InflateRequest obj) {
        obj.callback = null;
        obj.inflater = null;
        obj.parent = null;
        obj.resid = 0;
        obj.view = null;
        mRequestPool.release(obj);
    }


    public void cancel() {
        mHandler.removeCallbacksAndMessages(null);
        mHandlerCallback = null;
    }
}
```
总结
本文介绍了通过异步的方式进行布局加载，缓解了主线程的压力。同时也介绍了AsyncLayoutInflate的实现原理以及如何定制自己的AsyncLayoutInflate。
本文的定制方式仅仅只是作为一个参考，具体的实现方式可以根据自己项目的实际情况来定制。