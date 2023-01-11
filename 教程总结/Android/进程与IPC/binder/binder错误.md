


android.os.DeadObjectException
https://cloud.tencent.com/developer/article/1639712
```
I [2019-08-18 10:11:08 GMT+8] binder: 1433:1561 transaction failed 29201/-28, size 828-8 line 3135
W BroadcastQueue: Can't deliver broadcast to com.xxxx.xxxxx (pid 4712). Crashing it.
W BroadcastQueue: Failure sending broadcast Intent { act=android.intent.action.BATTERY_CHANGED flg=0x60000010 (has extras) }
W BroadcastQueue: android.os.DeadObjectException: Transaction failed on small parcel; remote process probably died
W BroadcastQueue:   at android.os.BinderProxy.transactNative(Native Method)
W BroadcastQueue:   at android.os.BinderProxy.transact(Binder.java:1127)
W BroadcastQueue:   at android.app.IApplicationThread$Stub$Proxy.scheduleRegisteredReceiver(IApplicationThread.java:1237)
W BroadcastQueue:   at com.android.server.am.BroadcastQueue.performReceiveLocked(BroadcastQueue.java:496)
W BroadcastQueue:   at com.android.server.am.BroadcastQueue.deliverToRegisteredReceiverLocked(BroadcastQueue.java:715)
W BroadcastQueue:   at com.android.server.am.BroadcastQueue.processNextBroadcastLocked(BroadcastQueue.java:875)
W BroadcastQueue:   at com.android.server.am.BroadcastQueue.processNextBroadcast(BroadcastQueue.java:834)
W BroadcastQueue:   at com.android.server.am.BroadcastQueue$BroadcastHandler.handleMessage(BroadcastQueue.java:172)
W BroadcastQueue:   at android.os.Handler.dispatchMessage(Handler.java:106)
W BroadcastQueue:   at android.os.Looper.loop(Looper.java:193)
W BroadcastQueue:   at android.os.HandlerThread.run(HandlerThread.java:65)
W BroadcastQueue:   at com.android.server.ServiceThread.run(ServiceThread.java:44)
```
表面问题是binder server无法申请足够的buffer
初步分析结论
广播的发送失败是原因，在一次binder通信中，无法向广播注册的App的binder驱动中映射的共享内存申请足够buffer
重大发现
出问题的应用注册了300多个广播，都是监听android.intent.action.BATTERY_CHANGED

当这个广播发送的时候，由于他的接受者有300多个，每一次接收都会在申请一次buffer，如果短时间一下子申请，非常有可能超过binder驱动的(1mb-8kb)/2的限制，
有人会问为什么是(1mb-8kb)/2而不是1mb-8kb，因为scheduleRegisteredReceiver是oneway的，对这个有疑问的，
可以看一下我的另外一个文章：[007]一次Binder通信最大可以传输多大的数据？
https://www.jianshu.com/p/ea4fc6aefaa8

水落石出
原来应用开发工程师，在registerReceiver和unregisterReceiver使用了不同的context，导致了unregisterReceiver的失败，
从而导致MyReceiver的无法被释放，而且这个代码还会导致MyActivity的内存泄露
```
public class MyActivity extends Activity {

    private MyReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("fdafdafsdafaffasdfad");
        receiver = new MyReceiver(this);
        getApplication().registerReceiver(receiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        try {
            //因为上下文不同，会导致unregisterReceiver失败，从而导致MyActivity和MyReceiver，无法被GC
            unregisterReceiver(receiver);
        } catch (Exception e) {

        }
        super.onDestroy();
    }

    public static class MyReceiver extends BroadcastReceiver {

        private Context mContext;

        public MyReceiver(Context context) {
            mContext = context;
        }

        @Override
        public void onReceive(Context context, Intent intent) {

        }
    }
}
```