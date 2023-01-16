https://www.jianshu.com/p/ab4a7e353076

如果一个无用对象（不需要再使用的对象）仍然被其他对象持有引用，造成该对象无法被系统回收，以致该对象在堆中所占用的内存单元无法被释放
  而造成内存空间浪费，这中情况就是内存泄露。
 //jvm使用可达性分析，不正确的引用，使得应该回收的对象未被回收

在Android开发中，一些不好的编程习惯会导致我们的开发的app存在内存泄露的情况。下面介绍一些在Android开发中常见的内存泄露场景及优化方案。

//android的内存泄露通常是activity,fragment,service，而activity里面有大量的内容，发生内存泄露往往是很严重的

单例导致内存泄露
单例模式在Android开发中会经常用到，但是如果使用不当就会导致内存泄露。因为单例的静态特性使得它的生命周期同应用的生命周期一样长，
  如果一个对象已经没有用处了，但是单例还持有它的引用，那么在整个应用程序的生命周期它都不能正常被回收，从而导致内存泄露。
```
public class AppSettings {

    private static AppSettings sInstance;
    private Context mContext;

    private AppSettings(Context context) {
        this.mContext = context;
    }

    public static AppSettings getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new AppSettings(context);
        }
        return sInstance;
    }
}
```
像上面代码中这样的单例，如果我们在调用getInstance(Context context)方法的时候传入的context参数是Activity、Service等上下文，
  就会导致内存泄露。

以Activity为例，当我们启动一个Activity，并调用getInstance(Context context)方法去获取AppSettings的单例，
  传入Activity.this作为context，这样AppSettings类的单例sInstance就持有了Activity的引用，当我们退出Activity时，
  该Activity就没有用了，但是因为sIntance作为静态单例（在应用程序的整个生命周期中存在）会继续持有这个Activity的引用，
  导致这个Activity对象无法被回收释放，这就造成了内存泄露。

为了避免这样单例导致内存泄露，我们可以将context参数改为全局的上下文：
```
private AppSettings(Context context) {
    this.mContext = context.getApplicationContext();
}
```
全局的上下文Application Context就是应用程序的上下文，和单例的生命周期一样长，这样就避免了内存泄漏。

单例模式对应应用程序的生命周期，所以我们在构造单例的时候尽量避免使用Activity的上下文，而是使用Application的上下文。
另外的方式初始context，将init和getInstance分离
```
 //application中init
 public static void init(Context context){
    //静态变量
    BleManager.applicationContext = context.getApplicationContext();
  }
  public static BleManager getInstance() {
    if (instance == null) {
      ....
      instance = new BleManager(bluetoothManager);
    }
    return instance;
  }
```




静态变量导致内存泄露
静态变量存储在方法区，它的生命周期从类加载开始，到整个进程结束。一旦静态变量初始化后，它所持有的引用只有等到进程结束才会释放。

比如下面这样的情况，在Activity中为了避免重复的创建info，将sInfo作为静态变量：
```
public class MainActivity extends AppCompatActivity {

    private static Info sInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (sInfo != null) {
            sInfo = new Info(this);
        }
    }
}

class Info {
    public Info(Activity activity) {
    }
}
```
Info作为Activity的静态成员，并且持有Activity的引用，但是sInfo作为静态变量，生命周期肯定比Activity长。所以当Activity退出后，
  sInfo仍然引用了Activity，Activity不能被回收，这就导致了内存泄露。

在Android开发中，静态持有很多时候都有可能因为其使用的生命周期不一致而导致内存泄露，所以我们在新建静态持有的变量的时候需要多考虑一下各个成员之间的引用关系，
并且尽量少地使用静态持有的变量，以避免发生内存泄露。当然，我们也可以在适当的时候讲静态量重置为null，使其不再持有引用，这样也可以避免内存泄露。





非静态内部类导致内存泄露
非静态内部类（包括匿名内部类）默认就会持有外部类的引用，当非静态内部类对象的生命周期比外部类对象的生命周期长时，就会导致内存泄露。
  //间接的引用使得外部的对象不能回收，外部对象的生命周期变长甚至一直存在

非静态内部类导致的内存泄露在Android开发中有一种典型的场景就是使用Handler，很多开发者在使用Handler是这样写的：
```
public class MainActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        start();
    }

    private void start() {
        Message msg = Message.obtain();
        msg.what = 1;
        mHandler.sendMessage(msg);
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                // 做相应逻辑
            }
        }
    };
}
```
也许有人会说，mHandler并未作为静态变量持有Activity引用，生命周期可能不会比Activity长，应该不一定会导致内存泄露呢，显然不是这样的！

//引用链 activity->handler->msg->MessageQueue->Looper  关键通过msg，MessageQueue持有了Handler,如果msg的生命周期超过activity就会发生问题
熟悉Handler消息机制的都知道，mHandler会作为成员变量保存在发送的消息msg中，即msg持有mHandler的引用，而mHandler是Activity的非静态内部类实例，
 即mHandler持有Activity的引用，那么我们就可以理解为msg间接持有Activity的引用。msg被发送后先放到消息队列MessageQueue中，
然后等待Looper的轮询处理（MessageQueue和Looper都是与线程相关联的，MessageQueue是Looper引用的成员变量，
而Looper是保存在ThreadLocal中的）。那么当Activity退出后，msg可能仍然存在于消息对列MessageQueue中未处理或者正在处理，
那么这样就会导致Activity无法被回收，以致发生Activity的内存泄露

还有就是handler中执行了耗时任务(任务在子线程)，或者耗时任务使用了handler，都会使得handler的生命周期变长

另外handler使用的runnable，post(Runnable)，runnable一般也是匿名内部类，会持有activity的引用，增大风险

//todo handler的其他问题 https://mp.weixin.qq.com/s/MwDlFsrH0LbohmrLZyGoEA

通常在Android开发中如果要使用内部类，但又要规避内存泄露，一般都会采用静态内部类+弱引用的方式
```
public class MainActivity extends AppCompatActivity {

    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHandler = new MyHandler(this);
        start();
    }

    private void start() {
        Message msg = Message.obtain();
        msg.what = 1;
        mHandler.sendMessage(msg);
    }

    private static class MyHandler extends Handler {

        private WeakReference<MainActivity> activityWeakReference;

        public MyHandler(MainActivity activity) {
            activityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = activityWeakReference.get();
            if (activity != null) {
                if (msg.what == 1) {
                    // 做相应逻辑
                }
            }
        }
    }
}
```

mHandler通过弱引用的方式持有Activity，当GC执行垃圾回收时，遇到Activity就会回收并释放所占据的内存单元。这样就不会发生内存泄露了。

上面的做法确实避免了Activity导致的内存泄露，发送的msg不再已经没有持有Activity的引用了，但是msg还是有可能存在消息队列MessageQueue中，
  所以更好的是在Activity销毁时就将mHandler的回调和发送的消息给移除掉。
```
@Override
protected void onDestroy() {
    super.onDestroy();
    mHandler.removeCallbacksAndMessages(null);
}
```

非静态内部类造成内存泄露还有一种情况就是使用Thread或者AsyncTask。

比如在Activity中直接new一个子线程Thread：
```
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 模拟相应耗时逻辑
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
```
或者直接新建AsyncTask异步任务：
```
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                // 模拟相应耗时逻辑
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();
    }
}
```
很多初学者都会像上面这样新建线程和异步任务，殊不知这样的写法非常地不友好，这种方式新建的子线程Thread和AsyncTask都是匿名内部类对象，
默认就隐式的持有外部Activity的引用，导致Activity内存泄露。要避免内存泄露的话还是需要像上面Handler一样使用静态内部类+弱应用的方式（参考上面Hanlder的正确写法）
destroy的时候取消task或者线程 thread.interrupt()

单例里面的方法使用内部类，将参数的对象泄漏
```
class SingleInstance{
//一直持有LeScanCallback
BluetoothAdapter.LeScanCallback leScanCallback = null;
 public void startScanAndConnect(String sn,ActivityResultLauncher<Intent> launcher) {
    this.leScanCallback = new BluetoothAdapter.LeScanCallback() {
      @Override
      public void onLeScan(BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
       //使用launcher
       connect(launcher);
      } 
    };
  }
}
```




未取消注册或回调导致内存泄露
比如我们在Activity中注册广播，如果在Activity销毁后不取消注册，那么这个广播会一直存在系统中，同上面所说的非静态内部类一样持有Activity引用，
导致内存泄露。因此注册广播后在Activity销毁后一定要取消注册。
```
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.registerReceiver(mReceiver, new IntentFilter());
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // 接收到广播需要做的逻辑
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(mReceiver);
    }
}
```
在注册观察则模式的时候，如果不及时取消也会造成内存泄露。比如使用Retrofit+RxJava注册网络请求的观察者回调，同样作为匿名内部类持有外部引用，
  所以需要记得在不用或者销毁的时候取消注册。

OnClickListener为什么不会导致内存泄露
一般view设置OnClickListener，View的生命周期与activity一致，跟随activity进行销毁，不会发生内存泄露
 所以注册监听时，注意被观察者的生命周期，一般被观察者都持有观察者的引用


给单例设置监听导致的泄漏
实例：
```
 public onCreate(){
   BleManager.getInstance().addListener(new DeviceListener(){
    ...//匿名内部类持有外部的activity
   });
 }
```
解决1 lifecycle
```
 public void addDeviceConnectListener(Lifecycle lifecycle,DeviceConnectListener deviceConnect) {
    lifecycle.addObserver(new DefaultLifecycleObserver() {
      @Override public void onDestroy(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onDestroy(owner);
        deviceConnectListeners.remove(deviceConnect);
        owner.getLifecycle().removeObserver(this);
      }
    });
    deviceConnectListeners.add(deviceConnect);
  }
```
解决2 listener使用weakReference包裹
解决3 静态内部类实现监听接口，使用class，然后注意onDestroy取消回调
private static class DeviceConnectListenerImpl implements DeviceConnectListener{
}



Timer和TimerTask导致内存泄露
Timer和TimerTask在Android中通常会被用来做一些计时或循环任务，比如实现无限轮播的ViewPager：
```
public class MainActivity extends AppCompatActivity {
    private ViewPager mViewPager;
    private PagerAdapter mAdapter;
    private Timer mTimer;
    private TimerTask mTimerTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        mTimer.schedule(mTimerTask, 3000, 3000);
    }

    private void init() {
        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        mAdapter = new ViewPagerAdapter();
        mViewPager.setAdapter(mAdapter);

        mTimer = new Timer();
        mTimerTask = new TimerTask() {
            @Override
            public void run() {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loopViewpager();
                    }
                });
            }
        };
    }

    private void loopViewpager() {
        if (mAdapter.getCount() > 0) {
            int curPos = mViewPager.getCurrentItem();
            curPos = (++curPos) % mAdapter.getCount();
            mViewPager.setCurrentItem(curPos);
        }
    }

    private void stopLoopViewPager() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer.purge();
            mTimer = null;
        }
        if (mTimerTask != null) {
            mTimerTask.cancel();
            mTimerTask = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLoopViewPager();
    }
}
```
当我们Activity销毁的时，有可能Timer还在继续等待执行TimerTask，它持有Activity的引用不能被回收，
  因此当我们Activity销毁的时候要立即cancel掉Timer和TimerTask，以避免发生内存泄漏。

Toast导致的内存泄漏
Toast的生命周期可能比activity长
可以使用applicationContext


属性动画造成内存泄露
动画同样是一个耗时任务，比如在Activity中启动了属性动画（ObjectAnimator），但是在销毁的时候，没有调用cancle方法，虽然我们看不到动画了，
但是这个动画依然会不断地播放下去，动画引用所在的控件，所在的控件引用Activity，这就造成Activity无法正常释放。
因此同样要在Activity销毁的时候cancel掉属性动画，避免发生内存泄漏。
```
@Override
protected void onDestroy() {
    super.onDestroy();
    mAnimator.cancel();
}
```


集合中的对象未清理造成内存泄露
这个比较好理解，如果一个对象放入到ArrayList、HashMap等集合中，这个集合就会持有该对象的引用。当我们不再需要这个对象时，
也并没有将它从集合中移除，这样只要集合还在使用（而此对象已经无用了），这个对象就造成了内存泄露。并且如果集合被静态引用的话，
集合里面那些没有用的对象更会造成内存泄露了。所以在使用集合时要及时将不用的对象从集合remove，或者clear集合，以避免内存泄漏。



资源未关闭或释放导致内存泄露
在使用IO、File流或者Sqlite、Cursor等资源时要及时关闭。这些资源在进行读写操作时通常都使用了缓冲，如果及时不关闭，
这些缓冲对象就会一直被占用而得不到释放，以致发生内存泄露。因此我们在不需要使用它们的时候就及时关闭，以便缓冲能及时得到释放，从而避免内存泄露。



WebView造成内存泄露
关于WebView的内存泄露，因为WebView在加载网页后会长期占用内存而不能被释放，因此我们在Activity销毁后要调用它的destory()方法来销毁它以释放内存。

另外在查阅WebView内存泄露相关资料时看到这种情况：
Webview下面的Callback持有Activity引用，造成Webview内存无法释放，即使是调用了Webview.destory()等方法都无法解决问题（Android5.1之后）。

最终的解决方案是：在销毁WebView之前需要先将WebView从父容器中移除，然后在销毁WebView。详细分析过程请参考这篇文章：WebView内存泄漏解决方法。
https://link.jianshu.com/?t=http://blog.csdn.net/xygy8860/article/details/53334476

```
@Override
protected void onDestroy() {
    super.onDestroy();
    // 先从父控件中移除WebView
    mWebViewContainer.removeView(mWebView);
    mWebView.stopLoading();
    mWebView.getSettings().setJavaScriptEnabled(false);
    mWebView.clearHistory();
    mWebView.removeAllViews();
    mWebView.destroy();
}
```


