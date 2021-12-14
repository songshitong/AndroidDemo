https://mp.weixin.qq.com/s/63zHksIEEgm3PFNLtxIUrg
ttps://www.jianshu.com/p/5eaa129432bf

IntentService
一个可以在子线程进行耗时任务，并且在任务执行后自动停止的Service
用法
```
public class MyService extends IntentService {
    //这里必须有一个空参数的构造实现父类的构造,否则会报异常
    //java.lang.InstantiationException: java.lang.Class<***.MyService> has no zero argument constructor
    public MyService() {
        super("");
    }
    @Override
    public void onCreate() {
        System.out.println("onCreate");
        super.onCreate();
    }
    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        System.out.println("onStartCommand");
        return super.onStartCommand(intent, flags, startId);

    }
    @Override
    public void onStart(@Nullable Intent intent, int startId) {
        System.out.println("onStart");
        super.onStart(intent, startId);
    }
    @Override
    public void onDestroy() {
        System.out.println("onDestroy");
        super.onDestroy();
    }
    //这个是IntentService的核心方法,它是通过串行来处理任务的,也就是一个一个来处理
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        System.out.println("工作线程是: "+Thread.currentThread().getName());
        String task = intent.getStringExtra("task");
        System.out.println("任务是 :"+task);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
//activity中使用
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent = new Intent(this,MyService.class);
        intent.putExtra("task","播放音乐");
        startService(intent);
        intent.putExtra("task","播放视频");
        startService(intent);
        intent.putExtra("task","播放图片");
        startService(intent);
    }
}
```
日志输出
```
 I/System.out: onCreate
 I/System.out: onStartCommand
 I/System.out: onStart
 I/System.out: onStartCommand
 I/System.out: onStart
 I/System.out: onStartCommand
 I/System.out: onStart
 I/System.out: 工作线程是:IntentService[]
 I/System.out: 任务是 :播放音乐
 I/System.out: 工作线程是: IntentService[]
 I/System.out: 任务是 :播放视频
 I/System.out: 工作线程是: IntentService[]
 I/System.out: 任务是 :播放图片
 I/System.out: onDestroy
```
从结果中可以看出我们startService()执行了三次, onCreate()方法只执行了一次,说明只有一个Service实例, 
onStartCommand()和onStart()也执行了三次,关键是onHandleIntent()也执行了三次,而且这三次是串行的,
也就是执行完一个再执行下一个,当最后一个任务执行完, onDestroy()便自动执行了

//todo 多次调用startService service的onCreate只执行了一次  为什么是这样
//任务是耗时的，连续启动三次service,执行了三次onStart,handler的MessageQueue存在三条消息，(任务的耗时要超过service的创建耗时)
//onHandleIntent执行了三次，由于任务的耗时是一样的，执行完service就Destroy了
//todo 如果任务的时间不一样，会不会有的没有执行完就退出了



android8.1
/frameworks/base/core/java/android/app/IntentService.java
```
public abstract class IntentService extends Service {
//处理消息的handler
private final class ServiceHandler extends Handler {
    public ServiceHandler(Looper looper) {
        super(looper);
    }

    @Override
    public void handleMessage(Message msg) {
        onHandleIntent((Intent)msg.obj);
        //处理完消息后，停止service
        stopSelf(msg.arg1);
    }
}

@Override
public void onCreate() {
    super.onCreate();
    //新建子线程handler
    HandlerThread thread = new HandlerThread("IntentService[" + mName + "]");
    thread.start();

    mServiceLooper = thread.getLooper();
    mServiceHandler = new ServiceHandler(mServiceLooper);
}

@Override
public void onStart(@Nullable Intent intent, int startId) {
    Message msg = mServiceHandler.obtainMessage();
    msg.arg1 = startId;
    msg.obj = intent;
    mServiceHandler.sendMessage(msg);
}


public final void stopSelf() {
      stopSelf(-1);
  }

public final void stopSelf(int startId) {
  if (mActivityManager == null) {
      return;
  }
  try {
      mActivityManager.stopServiceToken(
              new ComponentName(this, mClassName), mToken, startId);
  } catch (RemoteException ex) {
  }
}
```
原理通过HandlerThread来进行子线程任务的处理，处理完成后自动destroy