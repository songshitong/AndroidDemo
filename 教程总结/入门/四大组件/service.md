https://www.jianshu.com/p/51aaa65d5d25  

todo service的限制
service
 本地服务(local service)
 调用者和service在同一个进程里，所以运行在主进程的main线程中。所以不能进行耗时操作，可以采用在service里面创建一个Thread来执行任务。service影响的是进程的生命周期，讨论与Thread的区别没有意义。
 任何 Activity 都可以控制同一 Service，而系统也只会创建一个对应 Service 的实例
 
 远程服务 调用者和service不在同一个进程中，service在单独的进程中的main线程，是一种垮进程通信方式
 
 service启动方式
   startService
   
   使用
   1，定义一个类继承service
   2，manifest.xml文件中配置service
   3，使用context的startService(Intent)方法启动service
   4，不在使用时，调用stopService(Intent)方法停止服务
   
   生命周期
   onCreate() -- > onStartCommand() -- > onDestory()   onStartCommand调用onStart
   注意：如果服务已经开启，不会重复回调onCreate()方法，如果再次调用context.startService()方法，service而是会调用onStart()或者onStartCommand()方法。停止服务需要调用context.stopService()方法，服务停止的时候回调onDestory被销毁。
   
   特点
   一旦服务开启就跟调用者（开启者）没有任何关系了。开启者退出了，开启者挂了，服务还在后台长期的运行，开启者不能调用服务里面的方法。
  
   bindService
   
   使用
   1，定义一个类继承Service
   2，在manifest.xml文件中注册service
   3，使用context的bindService(Intent,ServiceConnection,int)方法启动service
   4，不再使用时，调用unbindService(ServiceConnection)方法停止该服务
   
   生命周期
   onCreate() -- > onBind() --> onUnbind() -- > onDestory()
   注意：绑定服务不会调用onStart()或者onStartCommand()方法
   
   特点：bind的方式开启服务，绑定服务，调用者挂了，服务也会跟着挂掉。绑定者可以调用服务里面的方法
   
   todo 显式，隐式intent
   
   远程服务  使用隐式intent
   aidl：android interface definition language 安卓接口定义语言。
   aidl文件都是公有的，没有访问权限修饰符。
   使用
   在服务的内部创建一个内部类，提供一个方法，可以间接调用服务的方法
   把暴露的接口文件的扩展名改为.aidl文件 去掉访问修饰符
   实现服务的onbind方法，继承Bander和实现aidl定义的接口，提供给外界可调用的方法
   在activity 中绑定服务。bindService()
   在服务成功绑定的时候会回调 onServiceConnected方法 传递一个 IBinder对象
   aidl定义的接口.Stub.asInterface(binder) 调用接口里面的方法
   注意
     服务端与客户端aidl的包名要一致
     
     
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
   
   
   
   https://zhuanlan.zhihu.com/p/70494771
   
   