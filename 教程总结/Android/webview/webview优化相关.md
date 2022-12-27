https://github.com/Justson/AgentWeb
https://www.jianshu.com/p/fc7909e24178

WebView启动优化。
1 WebView第一次创建比较耗时，可以预先创建WebView，提前将其内核初始化。
```
public class App extends Application {
    private WebView mWebView ;
    @Override
    public void onCreate() {
        super.onCreate();
        mWebView = new WebView(new MutableContextWrapper(this));
    }
}
```
2 使用WebView缓存池，用到WebView的地方都从缓存池取，缓存池中没有缓存再创建，注意内存泄漏问题。 //重置webview的context
注意在 WebView 进入 WebPools 之前 ， 需要重置 WebView ，包括清空注入 WebView 的注入对象 ， 否则非常容易泄露
```
public class WebPools {
    private final Queue<WebView> mWebViews;
    private Object lock = new Object();
    private static WebPools mWebPools = null;

    private static final AtomicReference<WebPools> mAtomicReference = new AtomicReference<>();
    private static final String TAG=WebPools.class.getSimpleName();

    private WebPools() {
        mWebViews = new LinkedBlockingQueue<>();
    }

    public static WebPools getInstance() {

        for (; ; ) {
            if (mWebPools != null)
                return mWebPools;
            if (mAtomicReference.compareAndSet(null, new WebPools()))
                return mWebPools=mAtomicReference.get();

        }
    }

    public void recycle(WebView webView) {
        recycleInternal(webView);
    }


    public WebView acquireWebView(Activity activity) {
        return acquireWebViewInternal(activity);
    }

    private WebView acquireWebViewInternal(Activity activity) {

        WebView mWebView = mWebViews.poll();

        LogUtils.i(TAG,"acquireWebViewInternal  webview:"+mWebView);
        if (mWebView == null) {
            synchronized (lock) {
                return new WebView(new MutableContextWrapper(activity));
            }
        } else {
            MutableContextWrapper mMutableContextWrapper = (MutableContextWrapper) mWebView.getContext();
            mMutableContextWrapper.setBaseContext(activity);
            return mWebView;
        }
    }


    private void recycleInternal(WebView webView) {
        try {
            if (webView.getContext() instanceof MutableContextWrapper) {
                MutableContextWrapper mContext = (MutableContextWrapper) webView.getContext();
                mContext.setBaseContext(mContext.getApplicationContext());
                LogUtils.i(TAG,"enqueue  webview:"+webView);
                mWebViews.offer(webView);
            }
            if(webView.getContext() instanceof  Activity){
//            throw new RuntimeException("leaked");
                LogUtils.i(TAG,"Abandon this webview  ， It will cause leak if enqueue !");
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
```
3 本地预置html和css，WebView创建的时候先预加载本地html，之后通过js脚本填充内容部分。

内存泄露
单独开一个进程,避免其他进程影响
1 单独进程的activity
在androidmanifest.xml的activity标签里加上android:process="packagename.web"
2 使用ApplicationContext代码创建webView
WebView mWebView = new WebView(getApplicationContext());
不一定要 Service ， 启动「web」 进程 Broadcast 广播也是可以的 ， 提前在进入 WebView 页面之前 ， 先启动 PreWebService 把 「web」 进程创建了 ，
当系统在启动 WebActivity 的时候 ， 系统发现了 「web」 进程已经创建存在了 ， 系统就不需要耗费时间 Fork 出新的「web」进程了


独立进程预加载
```
        <service
            android:name=".PreWebService"
            android:process=":web"/>
        <activity
            android:name=".WebActivity"
            android:process=":web"
            />
```

https://juejin.cn/post/6844904149608300552#heading-3
WebView后台耗电问题
在WebView加载页面的时候，WebView会自动开启线程去加载，如果没有很好地将WebView销毁的话，这些残余的线程会一直在后台运行，由此导致你的应用程序耗电量居高不下
解决方案：
在Activity的onstop和onresume里分别把setJavaScriptEnabled()；给设置成false和true。


开启软硬件加速   
开启软硬件加速这个性能提升还是很明显的，但是会耗费更大的内存   可以根据手机内存大小设置
```
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
 } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
}
```

提前显示进度条
WebView.loadUrl("url") 不会立马就回调 onPageStarted 或者 onProgressChanged 因为在这一时间段 ， WebView 有可能在初始化内核 ， 
也有可能在与服务器建立连接 ， 这个时间段容易出现白屏 ， 白屏用户体验是很糟糕的
```
private void go(String url) {
        this.mWebView.loadUrl(url); 
        this.mIndicator.show() //显示进度条
}
```