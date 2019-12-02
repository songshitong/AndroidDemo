普通广播（Normal broadcasts）
普通广播是完全异步的，可以在同一时刻（逻辑上）被所有接收者接收到，消息传递的效率比较高，但缺点是：接收者不能将处理结果传递给下一个接收者，并且无法终止广播Intent的传播
Context.sendBroadcast()  发送的是普通广播，所有订阅者都有机会获得并进行处理

有序广播（Ordered broadcasts）
有序广播是按照接收者声明的优先级别（声明在intent-filter元素的android:priority属性中，数越大优先级别越高,取值范围:-1000到1000。也可以调用IntentFilter对象的setPriority()进行设置），被接收者依次接收广播。
    如：A的级别高于B,B的级别高于C,那么，广播先传给A，再传给B，最后传给C。A得到广播后，可以往广播里存入数据，当广播传给B时,B可以从广播中得到A存入的数据
    
  Context.sendOrderedBroadcast()
  发送的是有序广播，系统会根据接收者声明的优先级别按顺序逐个执行接收者，前面的接收者有权终止广播(BroadcastReceiver.abortBroadcast())，如果广播被前面的接收者终止，后面的接收者就再也无法获取到广播。
  对于有序广播，前面的接收者可以将处理结果通过setResultExtras(Bundle)方法存放进结果对象，然后传给下一个接收者，通过代码：Bundle bundle =getResultExtras(true))可以获取上一个接收者存入在结果对象中的数据。
  系统收到短信，发出的广播属于有序广播。如果想阻止用户收到短信，可以通过设置优先级，让你们自定义的接收者先获取到广播，然后终止广播，这样用户就接收不到短信了
  
生命周期：如果一个广播处理完onReceive 那么系统将认定此对象将不再是一个活动的对象，也就会finished掉它
   调用对象->onReceive->结束

使用步骤：
1，自定义一个类继承BroadcastReceiver
2，重写onReceive方法
3，在manifest.xml中注册


注意 ：BroadcastReceiver生命周期很短
如果需要在onReceiver完成一些耗时操作，应该考虑在Service中开启一个新线程处理耗时操作，不应该在BroadcastReceiver中开启一个新的线程，因为BroadcastReceiver生命周期很短，在执行完onReceiver以后就结束，
  如果开启一个新的线程，可能出现BroadcastRecevier退出以后线程还在，而如果BroadcastReceiver所在的进程结束了，该线程就会被标记为一个空线程，根据Android的内存管理策略，在系统内存紧张的时候，会按照优先级，结束优先级低的线程，
  而空线程无异是优先级最低的，这样就可能导致BroadcastReceiver启动的子线程不能执行完成
  
  


监听电话进行拦截 <uses-permission android:name="android.permission.PROCESS_OUTGOING_CALLS"/>

注意：xml中注册的优先级高于动态注册广播

静态注册和动态注册区别
动态注册广播不是常驻型广播，也就是说广播跟随activity的生命周期。注意: 在activity结束前，移除广播接收器。
静态注册是常驻型，也就是说当应用程序关闭后，如果有信息广播来，程序也会被系统调用自动运行。
当广播为有序广播时：
1 优先级高的先接收
2 同优先级的广播接收器，动态优先于静态
3 同优先级的同类广播接收器，静态：先扫描的优先于后扫描的，动态：先注册的优先于后注册的。
当广播为普通广播时：
1 无视优先级，动态广播接收器优先于静态广播接收器
2 同优先级的同类广播接收器，静态：先扫描的优先于后扫描的，动态：先注册的优先于后注册的

小结
在Android 中如果要发送一个广播必须使用sendBroadCast 向系统发送对其感兴趣的广播接收器中。
使用广播必须要有一个intent 对象必设置其action动作对象
使用广播必须在配置文件中显式的指明该广播对象
每次接收广播都会重新生成一个接收广播的对象
在BroadCastReceiver中尽量不要处理太多逻辑问题，建议复杂的逻辑交给Activity 或者 Service 去处理
如果在AndroidManifest.xml中注册，当应用程序关闭的时候，也会接收到广播。在应用程序中注册就不产生这种情况了


注意
当如果要进行的操作需要花费比较长的时间，则不适合放在BroadcastReceiver中进行处理。
引用网上找到的一段解释：
在 Android 中，程序的响应（ Responsive ）被活动管理器（ Activity Manager ）和窗口管理器（ Window Manager ）这两个系统服务所监视。
 当 BroadcastReceiver 在 10 秒内没有执行完毕，Android 会认为该程序无响应。所以在 BroadcastReceiver 里不能做一些比较耗时的操作，否侧会弹出ANR （ Application No Response ）的对话框。
  如果需要完成一项比较耗时的工作，应该通过发送Intent 给 Service ，由 Service 来完成。而不是使用子线程的方法来解决，因为 BroadcastReceiver 的生命周期很短（在 onReceive() 执行后 BroadcastReceiver 的实例就会被销毁）
  ，子线程可能还没有结束BroadcastReceiver 就先结束了。如果 BroadcastReceiver 结束了，它的宿主进程还在运行，那么子线程还会继续执行。但宿主进程此时很容易在系统需要内存时被优先杀死，因为它属于空进程（没有任何活动组件的进程）
  
