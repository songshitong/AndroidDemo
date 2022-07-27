https://www.jianshu.com/p/51aaa65d5d25  

service
 本地服务(local service)
 调用者和service在同一个进程里，所以运行在主进程的main线程中。所以不能进行耗时操作，可以采用在service里面创建一个Thread来执行任务。
   service影响的是进程的生命周期，讨论与Thread的区别没有意义。
 任何 Activity 都可以控制同一 Service，而系统也只会创建一个对应 Service 的实例
 
 远程服务(remote service) 调用者和service不在同一个进程中，service在单独的进程中的main线程，是一种垮进程通信方式，使用binder通信

 
 service启动方式
   startService
   使用
   1，定义一个类继承service
   2，manifest.xml文件中配置service
   3，使用context的startService(Intent)方法启动service
   4，不在使用时，调用stopService(Intent)方法停止服务
```
public class SimpleService extends Service {

    /**
     * 绑定服务时才会调用
     * 必须要实现的方法  
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * 首次创建服务时，系统将调用此方法来执行一次性设置程序（在调用 onStartCommand() 或 onBind() 之前）。
     * 如果服务已在运行，则不会调用此方法。该方法只被调用一次
     */
    @Override
    public void onCreate() {
        System.out.println("onCreate invoke");
        super.onCreate();
    }

    /**
     * 每次通过startService()方法启动Service时都会被回调。
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.out.println("onStartCommand invoke");
        return super.onStartCommand(intent, flags, startId);
    }
    @Override
    public void onDestroy() {
        System.out.println("onDestroy invoke");
        super.onDestroy();
    }
}
xml  enabled为false，service不启动了
<service
        android:name="com.cubic.vicar.activity.AHTencentLiveService"
        android:enabled="true"
        android:exported="false" />
```
   
   生命周期
   onCreate() -- > onStartCommand() -- > onDestroy()   onStartCommand调用onStart
   注意：如果服务已经开启，不会重复回调onCreate()方法，如果再次调用context.startService()方法，
      service而是会调用onStart()或者onStartCommand()方法。
   停止服务需要调用context.stopService()方法，服务停止的时候回调onDestroy被销毁。
   
   特点
   一旦服务开启就跟调用者（开启者）没有任何关系了。开启者退出了，开启者挂了，服务还在后台长期的运行，开启者不能调用服务里面的方法。
  
   bindService
   使用
   1，定义一个类继承Service
   2，在manifest.xml文件中注册service
   3，使用context的bindService(Intent,ServiceConnection,int)方法启动service
   4，不再使用时，调用unbindService(ServiceConnection)方法停止该服务
   
   生命周期
   onCreate() -- > onBind() --> onUnbind() -- > onDestroy()
   注意：绑定服务不会调用onStart()或者onStartCommand()方法
   
   特点：bind的方式开启服务，绑定服务，调用者挂了，服务也会跟着挂掉。
   绑定者可以调用服务里面的方法,bindService 的方式通过onServiceConnected方法，获取到Service对象，
     通过该对象可以直接操作到Service内部的方法，从而实现的Service 与调用者之间的交互
   多次调用bindService，onCreate及onBind都只执行一次

使用场景
如果想要启动一个后台服务长期进行某项任务，那么使用startService
如果只是短暂的使用，那么使用bindService。
如果想启动一个后台服务长期进行任务，且这个过程中需要与调用者进行交互，那么可以两者同时使用，或者使用
   startService + BoardCast/ EventBus 等方法。
对于既使用startService，又使用bindService的情况，结束服务时需要注意的事项：Service的终止，
  需要unbindService和stopService都调用才行；

前台服务与后台服务
前台Service和后台Service（普通）最大的区别就在于：
前台Service在下拉通知栏有显示通知，但后台Service没有；
前台Service优先级较高，不会由于系统内存不足而被回收；后台Service优先级较低，当系统出现内存不足情况时，很有可能会被回收
用法很简单，只需要在原有的Service类对onCreate()方法进行稍微修改即可
```
@Override
    public void onCreate() {
        super.onCreate();
        System.out.println("执行了onCreat()");

        //添加下列代码将后台Service变成前台Service
        //构建"点击通知后打开MainActivity"的Intent对象
        Intent notificationIntent = new Intent(this,MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0,notificationIntent,0);

        //新建Builer对象
        Notification.Builder builer = new Notification.Builder(this);
        builer.setContentTitle("前台服务通知的标题");//设置通知的标题
        builer.setContentText("前台服务通知的内容");//设置通知的内容
        builer.setSmallIcon(R.mipmap.ic_launcher);//设置通知的图标
        builer.setContentIntent(pendingIntent);//设置点击通知后的操作

        Notification notification = builer.getNotification();//将Builder对象转变成普通的notification
        startForeground(1, notification);//让Service变成前台Service,并在系统的状态栏显示出来

    }
```
   
   远程服务  使用隐式intent
   aidl：android interface definition language 安卓接口定义语言。
   aidl文件都是公有的，没有访问权限修饰符。
   使用
   在服务的内部创建一个内部类，提供一个方法，可以间接调用服务的方法
   把暴露的接口文件的扩展名改为.aidl文件 去掉访问修饰符
   实现服务的onbind方法，继承Binder和实现aidl定义的接口，提供给外界可调用的方法
   在AndroidManifest.xml中注册服务 & 声明为远程服务
   在activity 中绑定服务。bindService()
   在服务成功绑定的时候会回调 onServiceConnected方法 传递一个 IBinder对象
   aidl定义的接口.Stub.asInterface(binder) 调用接口里面的方法
   注意
     服务端与客户端aidl的包名要一致
   https://www.jianshu.com/p/34326751b2c6
//远程服务的AndroidManifest
```
<service
            android:name=".MyService"
            android:process=":remote"  //将本地服务设置成远程服务
            android:exported="true"      //设置可被其他进程调用
            >
            //该Service可以响应带有scut.carson_ho.service_server.AIDL_Service1这个action的Intent。
            //此处Intent的action必须写成“服务器端包名.aidl文件名”
            <intent-filter>
                <action android:name="scut.carson_ho.service_server.AIDL_Service1"/>
            </intent-filter>

        </service>
```
     
     
   IntentService
   Service本身的问题
     Service不会专门启动一条单独的进程，Service与它所在应用位于同一个进程中；
     Service也不是专门一条新线程，因此不应该在Service中直接处理耗时的任务
    
   IntentService特征:
   会创建独立的worker线程来处理所有的Intent请求；
   会创建独立的worker线程来处理onHandleIntent()方法实现的代码，无需处理多线程问题；
   所有请求处理完成后，IntentService会自动停止，无需调用stopSelf()方法停止Service；
   为Service的onBind()提供默认实现，返回null；
   为Service的onStartCommand提供默认实现，将请求Intent添加到队列中

   原理及场景
   这是一个基于消息的服务,每次启动该服务并不是马上处理你的工作,而是首先会创建对应的Looper,Handler并且在MessageQueue中
     添加的附带客户Intent的Message对象,当Looper发现有Message的时候接着得到Intent对象通过在onHandleIntent((Intent)msg.obj)中
    调用你的处理程序.处理完后即会停止自己的服务.意思是Intent的生命周期跟你的处理的任务是一致的.
   所以这个类用下载任务中非常好, 下载任务结束后服务自身就会结束退出.
   缺点： 利用的HandlerThread的串行消息机制，任务是一个接一个完成的，并发能力并不高

   
   
   https://zhuanlan.zhihu.com/p/70494771
   
   