https://www.jianshu.com/p/51aaa65d5d25
普通广播（Normal broadcasts）
普通广播是完全异步的，可以在同一时刻（逻辑上）被所有接收者接收到，消息传递的效率比较高，但缺点是：接收者不能将处理结果传递给下一个接收者，
  并且无法终止广播Intent的传播
Intent intent = new Intent()
intent.setAction();
Context.sendBroadcast(intent)  发送的是普通广播，所有订阅者都有机会获得并进行处理

有序广播（Ordered broadcasts）
有序广播是按照接收者声明的优先级别（声明在intent-filter元素的android:priority属性中，数越大优先级别越高,取值范围:-1000到1000。
   也可以调用IntentFilter对象的setPriority()进行设置），被接收者依次接收广播。
    如：A的级别高于B,B的级别高于C,那么，广播先传给A，再传给B，最后传给C。A得到广播后，可以往广播里存入数据，当广播传给B时
   ,B可以从广播中得到A存入的数据
    
  Context.sendOrderedBroadcast()
  发送的是有序广播，系统会根据接收者声明的优先级别按顺序逐个执行接收者，前面的接收者有权终止广播(BroadcastReceiver.abortBroadcast())，
     如果广播被前面的接收者终止，后面的接收者就再也无法获取到广播。
  对于有序广播，前面的接收者可以将处理结果通过setResultExtras(Bundle)方法存放进结果对象，然后传给下一个接收者，
    通过代码：Bundle bundle =getResultExtras(true))可以获取上一个接收者存入在结果对象中的数据。
  系统收到短信，发出的广播属于有序广播。如果想阻止用户收到短信，可以通过设置优先级，让你们自定义的接收者先获取到广播，然后终止广播，
     这样用户就接收不到短信了
  
生命周期：如果一个广播处理完onReceive 那么系统将认定此对象将不再是一个活动的对象，也就会finished掉它
   调用对象->onReceive->结束

广播接收者
使用步骤：
1，自定义一个类继承BroadcastReceiver
2，重写onReceive方法
3，在manifest.xml中注册   静态注册
```
public class MyBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("fuck","intent-action : " + intent.getAction());
        if(intent.getAction().equals("test")){
            Toast.makeText(context,"fuck",Toast.LENGTH_LONG).show();
        }
    }
}
//广播接收器
<receiver android:name=".broadcast.MyBroadcastReceiver">

    <intent-filter>
        <action android:name="android.intent.action.ACTION_POWER_CONNECTED" />
        <action android:name="test"/>//这里自定义一个广播动作
    </intent-filter>

</receiver>
```
广播还可以通过动态注册：
```
registerReceiver(new MyBroadcastReceiver(),new IntentFilter("test"));

取消注册 无法检查receiver是否注册
 try {
      mContext.unregisterReceiver(receiver);
    }catch (Exception e){
      Logger.e("receiver未注册");
    }
```
一定要加上这个权限（坑）  监听电话进行拦截
```
<uses-permission android:name="android.permission.PROCESS_OUTGOING_CALLS"/>
```
注意：xml中注册的优先级高于动态注册广播
发送广播
```
 Intent intent = new Intent("test");
 sendBroadcast(intent);
```

注意 ：BroadcastReceiver生命周期很短
当如果要进行的操作需要花费比较长的时间，则不适合放在BroadcastReceiver中进行处理。
引用网上找到的一段解释：
在 Android 中，程序的响应（ Responsive ）被活动管理器（ Activity Manager ）和窗口管理器（ Window Manager ）这两个系统服务所监视。
当 BroadcastReceiver 在 10 秒内没有执行完毕，Android 会认为该程序无响应。所以在 BroadcastReceiver 里不能做一些比较耗时的操作，
否侧会弹出ANR （ Application No Response ）的对话框。
如果需要在onReceiver完成一些耗时操作，应该考虑在Service中开启一个新线程处理耗时操作，不应该在BroadcastReceiver中开启一个新的线程，
  因为BroadcastReceiver生命周期很短，在执行完onReceiver以后就结束，
  如果开启一个新的线程，可能出现BroadcastReceiver退出以后线程还在，而如果BroadcastReceiver所在的进程结束了，
  该线程就会被标记为一个空线程，根据Android的内存管理策略，在系统内存紧张的时候，会按照优先级，结束优先级低的线程，
  而空线程无异是优先级最低的，这样就可能导致BroadcastReceiver启动的子线程不能执行完成



静态注册和动态注册区别
动态注册广播不是常驻型广播，也就是说广播跟随activity的生命周期。注意: 在activity结束前，移除广播接收器。
静态注册是常驻型，也就是说当应用程序关闭后，如果有信息广播来，程序也会被系统调用自动运行，在应用尚未启动的时候就可以接收到相应广播
当广播为有序广播时：
1 优先级高的先接收
2 同优先级的广播接收器，动态优先于静态
3 同优先级的同类广播接收器，静态：先扫描的优先于后扫描的，动态：先注册的优先于后注册的。
当广播为普通广播时：
1 无视优先级，动态广播接收器优先于静态广播接收器
2 同优先级的同类广播接收器，静态：先扫描的优先于后扫描的，动态：先注册的优先于后注册的

https://juejin.cn/post/6844903602696224782
显式广播（Explicit Broadcast）：发送的Intent是显示Intent的广播。通过指定Intent组件名称来实现的，它一般用在知道目标组件名称的前提下，
去调用以下方法。意图明确，指定了要激活的组件是哪个组件，一般是在相同的应用程序内部实现的。
```
Intent.setComponent()
Intent.setClassName()
Intent.setClass()
new Intent(A.this,B.class)
```
隐式广播（Implicit Broadcast）：通过Intent Filter来实现的，它一般用在没有明确指出目标组件名称的前提下。
Android系统会根据隐式意图中设置的动作(action)、类别(category)、数据（URI和数据类型）找到最合适的组件来处理这个意图。
一般是用于在不同应用程序之间

Android8.0后，当App targetSDK >= 26，几乎禁止了所有的隐式广播的静态注册监听
解决方法
按照官方推荐，对于隐式广播，通过以下方法进行替换。
1 动态通过调用 Context.registerReceiver()注册广播接收器而不是在清单中声明接收器。
2 使用JobScheduler。


小结
在Android 中如果要发送一个广播必须使用sendBroadCast 向系统发送对其感兴趣的广播接收器中。
使用广播必须要有一个intent 对象必设置其action动作对象
使用广播必须在配置文件中显式的指明该广播对象
每次接收广播都会重新生成一个接收广播的对象
在BroadCastReceiver中尽量不要处理太多逻辑问题，建议复杂的逻辑交给Activity 或者 Service 去处理
如果在AndroidManifest.xml中注册，当应用程序关闭的时候，也会接收到广播。在应用程序中注册就不产生这种情况了




应用内广播/进程内广播
应用内广播安全，速度快。缺点是只能在应用的一个进程中使用，不能跨进程使用。
```
//1. 自定义广播接收者
public class LocalReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        ...
    }
}
LocalReceiver localReceiver = new LocalReceiver();


