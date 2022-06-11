
架构总览
http://gityuan.com/android/

AMS activity启动过程
1. Launcher startActivity   通过binder向AMS发送START_ACTIVITY_TRANSACTION
2. AMS startActivity   AMS通过socket(名字为zygote)向zygote发送应用进程的启动参数，包括uid,gid等
3. Zygote fork 进程
4. ActivityThread main()     启动binder线程池，通过反射创建ActivityThread类，然后调用main方法
   4.1. ActivityThread attach    Looper.prepareMainLooper;ActivityThread.attach;Looper.loop()启动主线程looper
   4.2. handleBindApplication    创建Application
   4.3  attachBaseContext          Application.attachBaseContext
   4.4. installContentProviders    安装ContentProvider
   4.5. Application onCreate   //在onCreate前，ContentProvider要准备好
5. ActivityThread 进入loop循环
6. ActivityThread通过反射创建Activity    performLaunchActivity
   Activity生命周期回调，onCreate、onStart、onResume... 
//todo 增加activity stack   


//todo BadTokenException   为什么Application的Context不能show dialog
https://juejin.cn/post/6873669458742525960

几个类的关系
PhoneWindow继承自Window，
Window通过setWindowManager方法与WindowManager发生关联。
WindowManager继承自接口ViewManager，
WindowManagerImpl是WindowManager接口的实现类，但是具体的功能都会委托给WindowManagerGlobal来实现。
WindowManagerGlobal 将view给RootViewImpl
RootViewImpl通过WindowSession与WMS通信

WindowManager.addView添加过程
WindowManager.addView(mStatusBarView, mLp)
WindowManagerImpl.addView
WindowManagerGlobal.addView
ViewRootImpl.setView
WindowSession.addToDisplay   binder通信
WindowManagerService.addWindow
校验type类型，子窗口为1000到1999
创建并校验WindowToken是否正确
创建了WindowState，它存有窗口的所有的状态信息，将WindowToken和WindowState相关联

WindowToken主要有两个作用：
可以理解为窗口令牌，当应用程序想要向WMS申请新创建一个窗口，则需要向WMS出示有效的WindowToken。
AppWindowToken作为WindowToken的子类，主要用来描述应用程序的WindowToken结构，应用程序中每个Activity都对应一个AppWindowToken。
WindowToken会将相同组件（比如Activity）的窗口（WindowState）集合在一起，方便管理


Apk安装过程
PackageInstaller初始化的过程：
1 根据Uri的Scheme协议不同，跳转到不同的界面，content协议跳转到InstallStart，其他的跳转到PackageInstallerActivity。
    如果是Android7.0以及更高版本会跳转到InstallStart。
2 InstallStart将content协议的Uri转换为File协议，然后跳转到PackageInstallerActivity。
3 PackageInstallerActivity会分别对package协议和file协议的Uri进行处理，如果是file协议会解析APK文件得到包信息PackageInfo。
4 根据获取的包信息，PackageInstallerActivity中会对未知来源进行处理，如果允许安装未知来源或者根据Intent判断得出该APK不是未知来源，
就会初始化安装确认界面，如果管理员限制来自未知源的安装, 就弹出提示Dialog或者跳转到设置界面


PackageInstaller安装APK的过程，简单来说就两步：
1 将APK的信息通过IO流的形式写入到PackageInstaller.Session中。
2 调用PackageInstaller.Session的commit方法，将APK的信息交由PMS处理。   binder通信

PMS通过向PackageHandler发送消息来驱动APK的复制和安装工作。
1 PMS发送INIT_COPY和MCS_BOUND类型的消息，控制PackageHandler来绑定DefaultContainerService，完成复制APK等工作。
   DefaultContainerService运行在com.android.defcontainer进程
   使用io流将APK复制到临时存储目录并重命名为base.apk，比如/data/app/vmdl18300388.tmp/base.apk
2 复制APK完成后，会开始进行安装APK的流程，包括安装前的检查、安装APK和安装后的收尾工作。
   安装目录onSd：安装到SD卡， onInt：内部存储即Data分区，ephemeral：安装到临时存储（Instant Apps安装）
   安装前的检查：
     检查APK是否存在，如果存在进行替换安装
     如果安装过该APK，则需要校验APK的签名信息
   安装
      临时文件重新命名，比如前面提到的/data/app/vmdl18300388.tmp/base.apk，重命名为/data/app/包名-1/base.apk。
      这个新命名的包名会带上一个数字后缀1，每次升级一个已有的App，这个数字会不断的累加
   更新该APK对应的Settings信息，Settings用于保存所有包的动态设置，system/packages.xml
   安装后
     安装成功创建AppData目录
     安装失败删除APK



IMS
总结
EventHub
  EventHub通过Linux内核的INotify与Epoll机制监听设备节点dev/input，通过EventHub的getEvent函数读取设备节点的增删事件和原始输入事件
InputManager{
   InputReader   工作在InputReaderThread线程
      InputReader会不断循环读取EventHub中的原始输入事件，将这些原始输入事件进行加工后交由InputDispatcher
      线程不断执行threadLoop，事件读取后交由InputDispatcher，用epoll阻塞looper,就是handler的native looper
   InputDispatcher  工作在InputDispatcherThread线程
     InputDispatcher中保存了WMS中的所有Window信息，（WMS会将窗口的信息实时的更新到InputDispatcher中），
     这样InputDispatcher就可以将输入事件派发给合适的Window  通过InputChannel发送
}
InputManagerService   运行在SystemServer进程，里面的mHandler运行在android.display线程
  InputManagerService跟inputManager的关系
  InputManagerService，创建了InputManager

IMS启动了InputDispatcherThread和InputReaderThread，分别用来运行InputDispatcher和InputReader。
InputDispatcher先于InputReader被创建，InputDispatcher的dispatchOnceInnerLocked函数用来将事件分发给合适的Window。
   InputDispatcher没有输入事件处理时会进入睡眠状态，等待InputReader通知唤醒。
InputReader通过EventHub的getEvents函数获取事件信息，如果是原始输入事件，就将这些原始输入事件交由不同的InputMapper来处理，
  最终交由InputDispatcher来进行分发。
InputDispatcher的notifyKey函数中会根据按键数据来判断InputDispatcher是否要被唤醒，InputDispatcher被唤醒后，
   会重新调用dispatchOnceInnerLocked函数将输入事件分发给合适的Window。



渲染
view.invalidate
RootViewImpl.scheduleTraversals
mChoreographer.postCallback   监听Vsync的回调   通过DisplayEventReceiver注册Vsync
doTraversal->performTraversals 开始测量布局绘制
 measure  layout  draw
----软件绘制
RootViewImpl.drawSoftware
mSurface.lockCanvas(dirty)   
BufferQueueProducer从BufferQueue取出一个GraphicBuffer进行绘制，创建SkBitmap, 并让其像素数据指向 GraphicBuffer 的内存地址
mView.draw(canvas)  ViewGroup和子view在SkiaCanvas进行绘制
Surface.unlockCanvasAndPost  将GraphicBuffer送入BufferQueue
SurfaceFlinger 在BufferQueueConsumer取出GraphicBuffer，layer合成操作，送入HWComposer进行绘制
----硬件绘制
ThreadedRenderer.draw
将View 的绘制操作(drawLine...)抽象成 DrawOp 操作并存入 DisplayList 中
首先分配缓存区(同软件绘制)，然后将 Surface 绑定到 Render 线程，最后通过 GPU 渲染 DrawOp 数据    opengl/skia/vulkan
交给SurfaceFlinger绘制