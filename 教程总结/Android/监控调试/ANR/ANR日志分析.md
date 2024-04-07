https://mp.weixin.qq.com/s/kT0hZaYRlW9X8fIVEQKJLQ
https://mp.weixin.qq.com/s/TDtjQdOktLcUYec3ldhh5g   来自公众号vivo互联网技术
https://www.jianshu.com/p/082045769443

anr获取
```
//android 7.0
adb shell kill -S QUIT PID
adb pull /data/anr/traces.txt
//高版本  默认保存在手机的/bugreports，可以直接查看和pull到电脑
adb bugreport E:\Reports\MyBugReports  
//目录示例 bugreport-lavender-WORKOS-2022-04-25-16-35-34/FS/data/anr   里面有arn时间的文件anr_2022-04-25-16-04-23-482
```

干货：ANR日志分析全面解析
一、概述
解决ANR一直是Android 开发者需要掌握的重要技巧，一般从三个方面着手。
   开发阶段：通过工具检查各个方法的耗时，卡顿情况，发现一处修改一处。
   线上阶段：这个阶段主要依靠监控工具发现ANR并上报，比如matrix。
   分析阶段：如果线上用户发生ANR，并且你获取了一份日志，这就涉及了本文要分享的内容——ANR日志分析技巧。

二、ANR产生机制
一般来说，ANR按产生机制，分为4类：
2.1 输入事件超时(5s)
InputEvent Timeout
```
a.InputDispatcher发送key事件给 对应的进程的 Focused Window ，对应的window不存在、处于暂停态、或通道(input channel)占满、
   通道未注册、通道异常、或5s内没有处理完一个事件，就会发生ANR
b.InputDispatcher发送MotionEvent事件有个例外之处：当对应Touched Window的 input waitQueue中有超过0.5s的事件，
   inputDispatcher会暂停该事件，并等待5s，如果仍旧没有收到window的‘finish’事件，则触发ANR
c.下一个事件到达，发现有一个超时事件才会触发ANR
```
2.2 广播类型超时（前台15s，后台60s）
BroadcastReceiver Timeout
```
a.静态注册的广播和有序广播会ANR，动态注册的非有序广播并不会ANR
b.广播发送时，会判断该进程是否存在，不存在则创建，创建进程的耗时也算在超时时间里
c.只有当进程存在前台显示的Activity才会弹出ANR对话框，否则会直接杀掉当前进程
d.当onReceive执行超过阈值（前台15s，后台60s），将产生ANR
e.如何发送前台广播：Intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
```
2.3 服务超时（前台20s，后台200s）
Service Timeout   
```
a.Service的以下方法都会触发ANR：onCreate(),onStartCommand(), onStart(), onBind(), onRebind(), onTaskRemoved(), onUnbind(),onDestroy().
b.前台Service超时时间为20s，后台Service超时时间为200s
c.如何区分前台、后台执行————当前APP处于用户态，此时执行的Service则为前台执行。
d.用户态：有前台activity、有前台广播在执行、有foreground service执行
```
2.4 ContentProvider 类型
```
a.ContentProvider创建发布超时并不会ANR
b.使用ContentProviderclient来访问ContentProverder可以自主选择触发ANR，超时时间自己定
  client.setDetectNotResponding(PROVIDER_ANR_TIMEOUT);
```

ps：Activity生命周期超时会不会ANR？——经测试并不会。   //模拟器不会，有的手机会anr,比如红米K30Pro 
```
override fun onCreate(savedInstanceState: Bundle?) {
       Thread.sleep(60000)
       super.onCreate(savedInstanceState)
       setContentView(R.layout.activity_main)
   }
```

三、导致ANR的原因
很多开发者认为，那就是耗时操作导致ANR，全部是app应用层的问题。实际上，线上环境大部分ANR由系统原因导致。
3.1 应用层导致ANR（耗时操作）   
```
a. 函数阻塞：如死循环、主线程IO、处理大数据    根据下载的回调去更新页面，一直有回调，一直更新页面，主线程卡死，应该是动画每隔16ms去取一下进度
b. 锁出错：主线程等待子线程的锁
c. 内存紧张：系统分配给一个应用的内存是有上限的，长期处于内存紧张，会导致频繁内存交换，进而导致应用的一些操作超时
```
3.2 系统导致ANR
```
a. CPU被抢占：一般来说，前台在玩游戏，可能会导致你的后台广播被抢占CPU
b. 系统服务无法及时响应：比如获取系统联系人等，系统的服务都是Binder机制，服务能力也是有限的，有可能系统服务长时间不响应导致ANR
c. 其他应用占用的大量内存
```

分析流程
1 根据log确认发生ANR的进程、发生时间、大概在做什么操作，同时关注此时CPU、内存、IO的情况。
2 分析trace，先看时间是否能对的上，判断是不是案发现场，然后关注主线程是否存在耗时、死锁、等锁等问题，可以基本看出是APP问题还是系统问题。
3 如果是系统问题导致的，再结合binder_sample、dvm_lock_sample来分别定位下binder call耗时 和 系统持锁耗时的问题。
4 结合代码或源码来具体分析有问题的点。

四、分析日志
发生ANR的时候，系统会产生一份anr日志文件（手机的/data/anr 目录下，文件名称可能各厂商不一样，业内大多称呼为trace文件），内含如下几项重要信息。
查看anr时间 搜索am_anr
```
04-25 16:23:46.525  1000  2308  2327 I am_anr  : [0,7564,包名,818462278,Input dispatching timed out (类名, Waiting because the touched window's input channel is not registered with the input dispatcher.  The window may be in the process of being removed.)]
```
查看anr的进程 ANR in
```
04-25 16:23:50.297  1000  2308  2327 E ActivityManager: ANR in 包名.recording (包名.recording/.MainActivity)
04-25 16:23:50.297  1000  2308  2327 E ActivityManager: PID: 7564
04-25 16:23:50.297  1000  2308  2327 E ActivityManager: Reason: Input dispatching timed out (包名.recording/包名.recording.MainActivity, Waiting because the touched window's input channel is not registered with the input dispatcher.  The window may be in the process of being removed.)
04-25 16:23:50.297  1000  2308  2327 E ActivityManager: Load: 6.43 / 6.38 / 6.47
04-25 16:23:50.297  1000  2308  2327 E ActivityManager: CPU usage from 58547ms to 0ms ago (2022-04-25 16:22:47.932 to 2022-04-25 16:23:46.478):
04-25 16:23:50.297  1000  2308  2327 E ActivityManager:   9.3% 3595/com.qiyou.id: 7% user + 2.3% kernel
04-25 16:23:50.297  1000  2308  2327 E ActivityManager:   8.1% 804/surfaceflinger: 4.9% user + 3.2% kernel / faults: 2 minor
04-25 16:23:50.297  1000  2308  2327 E ActivityManager:   4.7% 2308/system_server: 2.9% user + 1.8% kernel / faults: 126 minor
```
4.1 CPU 负载
```
Load: 2.62 / 2.55 / 2.25
CPU usage from 0ms to 1987ms later (2020-03-10 08:31:55.169 to 2020-03-10 08:32:17.156):
  41% 2080/system_server: 28% user + 12% kernel / faults: 76445 minor 180 major
  26% 9378/com.xiaomi.store: 20% user + 6.8% kernel / faults: 68408 minor 68 major
........省略N行.....
66% TOTAL: 20% user + 15% kernel + 28% iowait + 0.7% irq + 0.7% softirq
```
如上所示：
第一行：1、5、15 分钟内正在使用和等待使用CPU 的活动进程的平均数
第二行：表明负载信息抓取在ANR发生之后的0~1987ms。同时也指明了ANR的时间点：2020-03-10 08:31:55.169
中间部分：各个进程占用的CPU的详细情况
最后一行：各个进程合计占用的CPU信息。

名词解释：   //todo 名词-操作系统
```
a. user:用户态,kernel:内核态
b. faults:内存缺页，minor——轻微的，major——重度，需要从磁盘拿数据
c. iowait:IO使用（等待）占比
d. irq:硬中断，softirq:软中断
```
注意：
iowait占比很高，意味着有很大可能，是io耗时导致ANR，具体进一步查看有没有进程faults major比较多。
单进程CPU的负载并不是以100%为上限，而是有几个核，就有百分之几百，如4核上限为400%。


4.2 内存信息
```
Total number of allocations 476778　　进程创建到现在一共创建了多少对象
Total bytes allocated 52MB　进程创建到现在一共申请了多少内存
Total bytes freed 52MB　　　进程创建到现在一共释放了多少内存
Free memory 777KB　　　 不扩展堆的情况下可用的内存
Free memory until GC 777KB　　GC前的可用内存
Free memory until OOME 383MB　　OOM之前的可用内存
Total memory 当前总内存（已用+可用）
Max memory 384MB  进程最多能申请的内存
```
从含义可以得出结论：Free memory until OOME 的值很小的时候，已经处于内存紧张状态。应用可能是占用了过多内存。
另外，除了trace文件中有内存信息，普通的eventlog日志中，也有内存信息（不一定打印）   //todo am_meminfo在哪打印
```
04-02 22:00:08.195  1531  1544 I am_meminfo: [350937088,41086976,492830720,427937792,291887104]
```
以上四个值分别指的是： Cached ,Free, Zram, (Kernel,Native)

Cached+Free的内存代表着当前整个手机的可用内存，如果值很小，意味着处于内存紧张状态。
  一般低内存的判定阈值为：4G 内存手机以下阀值：350MB，以上阀值则为：450MB

ps:如果ANR时间点前后，日志里有打印onTrimMemory，也可以作为内存紧张的一个参考判断   //todo onTrimMemory的触发


4.3 堆栈消息
堆栈信息是最重要的一个信息，展示了ANR发生的进程当前所有线程的状态。  
```
suspend all histogram:  Sum: 2.834s 99% C.I. 5.738us-7145.919us Avg: 607.155us Max: 41543us
DALVIK THREADS (248):
"main" prio=5 tid=1 Native   
  | group="main" sCount=1 dsCount=0 flags=1 obj=0x74b17080 self=0x7bb7a14c00
  | sysTid=2080 nice=-2 cgrp=default sched=0/0 handle=0x7c3e82b548
  | state=S schedstat=( 757205342094 583547320723 2145008 ) utm=52002 stm=23718 core=5 HZ=100
  | stack=0x7fdc995000-0x7fdc997000 stackSize=8MB
  | held mutexes=
  kernel: __switch_to+0xb0/0xbc
  kernel: SyS_epoll_wait+0x288/0x364
  kernel: SyS_epoll_pwait+0xb0/0x124
  kernel: cpu_switch_to+0x38c/0x2258
  native: #00 pc 000000000007cd8c  /system/lib64/libc.so (__epoll_pwait+8)
  native: #01 pc 0000000000014d48  /system/lib64/libutils.so (android::Looper::pollInner(int)+148)
  native: #02 pc 0000000000014c18  /system/lib64/libutils.so (android::Looper::pollOnce(int, int*, int*, void**)+60)
  native: #03 pc 0000000000127474  /system/lib64/libandroid_runtime.so (android::android_os_MessageQueue_nativePollOnce(_JNIEnv*, _jobject*, long, int)+44)
  at android.os.MessageQueue.nativePollOnce(Native method)
  at android.os.MessageQueue.next(MessageQueue.java:330)
  at android.os.Looper.loop(Looper.java:169)
  at com.android.server.SystemServer.run(SystemServer.java:508)
  at com.android.server.SystemServer.main(SystemServer.java:340)
  at java.lang.reflect.Method.invoke(Native method)
  at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:536)
  at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:856)
   
  ........省略N行.....
   
  "OkHttp ConnectionPool" daemon prio=5 tid=251 TimedWaiting
  | group="main" sCount=1 dsCount=0 flags=1 obj=0x13daea90 self=0x7bad32b400
  | sysTid=29998 nice=0 cgrp=default sched=0/0 handle=0x7b7d2614f0
  | state=S schedstat=( 951407 137448 11 ) utm=0 stm=0 core=3 HZ=100
  | stack=0x7b7d15e000-0x7b7d160000 stackSize=1041KB
  | held mutexes=
  at java.lang.Object.wait(Native method)
  - waiting on <0x05e5732e> (a com.android.okhttp.ConnectionPool)
  at com.android.okhttp.ConnectionPool$1.run(ConnectionPool.java:103)
  - locked <0x05e5732e> (a com.android.okhttp.ConnectionPool)
  at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1167)
  at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
  at java.lang.Thread.run(Thread.java:764)
```
线程自身信息    https://www.cnblogs.com/huansky/p/13944132.html
线程优先级：prio=5
线程ID： tid=1
线程状态：Sleeping
线程组名称：group="main"
线程被挂起的次数：sCount=1
线程被调试器挂起的次数：dsCount=0
线程的java的对象地址：obj= 0x7682ab30
线程本身的Native对象地址：self=0x7bd3815c00
线程调度信息：
Linux系统中内核线程ID: sysTid=6317与主线程的进程号相同
线程调度优先级：nice=-10
线程调度组：cgrp=default
线程调度策略和优先级：sched=0/0
线程处理函数地址：handle= 0x7c59fc8548
线程的上下文信息：
线程调度状态：state=S
线程在CPU中的执行时间、线程等待时间、线程执行的时间片长度：schedstat=(1009468742 32888019 224)
线程在用户态中的调度时间值：utm=91
线程在内核态中的调度时间值：stm=9
最后执行这个线程的CPU核序号：core=4
线程的堆栈信息：
堆栈地址和大小：stack=0x7ff27e1000-0x7ff27e3000 stackSize=8MB

如上日志所示，本文截图了两个线程信息，一个是主线程main，它的状态是native。另一个是OkHttp ConnectionPool，它的状态是TimeWaiting。

众所周知，教科书上说线程状态有5种：新建、就绪、执行、阻塞、死亡。
而Java中的线程状态有6种，这6种状态都定义在：java.lang.Thread.State中
状态       解释
NEW       线程刚被创建，但是并未启动。还没调用start方法。
RUNNABLE  线程可以在java虚拟机中运行的状态，可能正在运行自己代码，也可能没有，这取决于操作系统处理器.
BLOCKED   当一个线程试图获取一个对象锁，而该对象锁被其他的线程持有，则该线程进入Blocked状态;当该线程持有锁时，该线程将变成Runnable状态
WATING    一个线程在等待另一个线程执行一个(唤醒)动作时，该线程进入Waiting状态。进入这个状态后是不能自动唤醒的，必须等待另一个线程调用notify或者notifyAll方法才能够
TIMED_WAITING  同waiting状态，有超时参数，超时后自动唤醒，比如Thread.Sleep(1000)
TERMINATED     线程已经执行完毕

问题来了，上述main线程的native是什么状态，哪来的？
其实trace文件中的状态是是CPP代码中定义的状态，下面是一张对应关系表。  //todo 对照怎么来的
java thread状态 cpp thread状态     说明
TERMINATED     ZOMBIE             线程死亡，终止运行
RUNNABLE       RUNNING/RUNNABLE   线程可运行或正在运行
TIMED_WAITING  TIMED_WAIT         执行了带有超时参数的wait、sleep或join函数
BLOCKED        MONITOR             线程阻塞，等待获取对象锁
WAITING        WAIT               执行了无超时参数的wait函数
NEW            INITIALIZING        新建，正在初始化，为其分配资源
NEW            STARTING            新建，正在启动
RUNNABLE       NATIVE              正在执行JNI本地函数
WAITING        VMWAIT              正在等待VM资源
RUNNABLE       SUSPENDED           线程暂停，通常是由于GC或debug被暂停
                UNKNOWN             未知状态
由此可知，main函数的native状态是正在执行JNI函数。堆栈信息是我们分析ANR的第一个重要的信息，一般来说：
1 main线程处于 BLOCK、WAITING、TIMEWAITING状态，那基本上是函数阻塞导致ANR；
2 如果main线程无异常，则应该排查CPU负载和内存环境。


五、典型案例分析
5.1 主线程无卡顿，处于正常状态堆栈
```
main" prio=5 tid=1 Native
  | group="main" sCount=1 dsCount=0 flags=1 obj=0x74b38080 self=0x7ad9014c00
  | sysTid=23081 nice=0 cgrp=default sched=0/0 handle=0x7b5fdc5548
  | state=S schedstat=( 284838633 166738594 505 ) utm=21 stm=7 core=1 HZ=100
  | stack=0x7fc95da000-0x7fc95dc000 stackSize=8MB
  | held mutexes=
  kernel: __switch_to+0xb0/0xbc
  kernel: SyS_epoll_wait+0x288/0x364
  kernel: SyS_epoll_pwait+0xb0/0x124
  kernel: cpu_switch_to+0x38c/0x2258
  native: #00 pc 000000000007cd8c  /system/lib64/libc.so (__epoll_pwait+8)
  native: #01 pc 0000000000014d48  /system/lib64/libutils.so (android::Looper::pollInner(int)+148)
  native: #02 pc 0000000000014c18  /system/lib64/libutils.so (android::Looper::pollOnce(int, int*, int*, void**)+60)
  native: #03 pc 00000000001275f4  /system/lib64/libandroid_runtime.so (android::android_os_MessageQueue_nativePollOnce(_JNIEnv*, _jobject*, long, int)+44)
  at android.os.MessageQueue.nativePollOnce(Native method)
  at android.os.MessageQueue.next(MessageQueue.java:330)
  at android.os.Looper.loop(Looper.java:169)
  at android.app.ActivityThread.main(ActivityThread.java:7073)
  at java.lang.reflect.Method.invoke(Native method)
  at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:536)
  at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:876)
```
上述主线程堆栈就是一个很正常的空闲堆栈，表明主线程正在等待新的消息。如果ANR日志里主线程是这样一个状态，那可能有两个原因：
1 该ANR是CPU抢占或内存紧张等其他因素引起
2 这份ANR日志抓取的时候，主线程已经恢复正常
遇到这种空闲堆栈，可以按照第3节的方法去分析CPU、内存的情况。其次可以关注抓取日志的时间和ANR发生的时间是否相隔过久，时间过久这个堆栈就没有分析意义了。

5.2 主线程执行耗时操作
```
"main" prio=5 tid=1 Runnable
  | group="main" sCount=0 dsCount=0 flags=0 obj=0x72deb848 self=0x7748c10800
  | sysTid=8968 nice=-10 cgrp=default sched=0/0 handle=0x77cfa75ed0
  | state=R schedstat=( 24783612979 48520902 756 ) utm=2473 stm=5 core=5 HZ=100
  | stack=0x7fce68b000-0x7fce68d000 stackSize=8192KB
  | held mutexes= "mutator lock"(shared held)
  at com.example.test.MainActivity$onCreate$2.onClick(MainActivity.kt:20)——关键行！！！
  at android.view.View.performClick(View.java:7187)
  at android.view.View.performClickInternal(View.java:7164)
  at android.view.View.access$3500(View.java:813)
  at android.view.View$PerformClick.run(View.java:27640)
  at android.os.Handler.handleCallback(Handler.java:883)
  at android.os.Handler.dispatchMessage(Handler.java:100)
  at android.os.Looper.loop(Looper.java:230)
  at android.app.ActivityThread.main(ActivityThread.java:7725)
  at java.lang.reflect.Method.invoke(Native method)
  at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:526)
  at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:1034)
```
上述日志表明，主线程正处于执行状态，看堆栈信息可知不是处于空闲状态，发生ANR是因为一处click监听函数里执行了耗时操作。
其他情况
主线程执行耗时操作是应用ANR类型问题里遇到最多的，比如网络访问，访问数据库之类的，都很容易造成主线程堵塞，
这里以访问数据库来说，这类型引起的ANR，一般来讲看当时的CPU使用情况会发现user占比较高，看trace中主线程当时的信息会发现会有一些比如query像ContentProvider这种数据库的动作。
  这种情况下，还可以去看eventlog或者mainlog，在ANR发生前后打印出来的信息，比如访问数据库这种，在eventlog中搜索"am_anr",然后看前后片段，
  会发现发生ANR的这个进程有很多数据库相关的信息，说明在发生ANR前后主线程一直在忙于访问数据库，这类型的问题常见于图库，联系人，彩短信应用。

所以这种问题的解决，一般考虑的是异步解决，异步解决并不是简单的new一个线程，要根据业务场景以及频率来决定，Android常见的异步AsyncTask,
    IntentService, 线程池(官方四种或自定义), new thread等，一般来说不建议直接new thread


5.3 主线程被锁阻塞     搜索Blocked
```
"main" prio=5 tid=1 Blocked
  | group="main" sCount=1 dsCount=0 flags=1 obj=0x72deb848 self=0x7748c10800
  | sysTid=22838 nice=-10 cgrp=default sched=0/0 handle=0x77cfa75ed0
  | state=S schedstat=( 390366023 28399376 279 ) utm=34 stm=5 core=1 HZ=100
  | stack=0x7fce68b000-0x7fce68d000 stackSize=8192KB
  | held mutexes=
  at com.example.test.MainActivity$onCreate$1.onClick(MainActivity.kt:15)
  - waiting to lock <0x01aed1da> (a java.lang.Object) held by thread 3 ——————关键行！！！
  at android.view.View.performClick(View.java:7187)
  at android.view.View.performClickInternal(View.java:7164)
  at android.view.View.access$3500(View.java:813)
  at android.view.View$PerformClick.run(View.java:27640)
  at android.os.Handler.handleCallback(Handler.java:883)
  at android.os.Handler.dispatchMessage(Handler.java:100)
  at android.os.Looper.loop(Looper.java:230)
  at android.app.ActivityThread.main(ActivityThread.java:7725)
  at java.lang.reflect.Method.invoke(Native method)
  at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:526)
  at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:1034)
  ........省略N行.....

  "WQW TEST" prio=5 tid=3 TimeWating
  | group="main" sCount=1 dsCount=0 flags=1 obj=0x12c44230 self=0x772f0ec000
  | sysTid=22938 nice=0 cgrp=default sched=0/0 handle=0x77391fbd50
  | state=S schedstat=( 274896 0 1 ) utm=0 stm=0 core=1 HZ=100
  | stack=0x77390f9000-0x77390fb000 stackSize=1039KB
  | held mutexes=
  at java.lang.Thread.sleep(Native method)
  - sleeping on <0x043831a6> (a java.lang.Object)
  at java.lang.Thread.sleep(Thread.java:440)
  - locked <0x043831a6> (a java.lang.Object)
  at java.lang.Thread.sleep(Thread.java:356)
  at com.example.test.MainActivity$onCreate$2$thread$1.run(MainActivity.kt:22)
  - locked <0x01aed1da> (a java.lang.Object)————————————————————关键行！！！
  at java.lang.Thread.run(Thread.java:919)
```
这是一个典型的主线程被锁阻塞的例子；
waiting to lock <0x01aed1da> (a java.lang.Object) held by thread 3
其中等待的锁是<0x01aed1da>，这个锁的持有者是线程 3。进一步搜索 “tid=3” 找到线程3， 发现它正在TimeWating。
那么ANR的原因找到了：线程3持有了一把锁，并且自身长时间不释放，主线程等待这把锁发生超时。在线上环境中，常见因锁而ANR的场景是SharePreference写入。
对于死锁，即形成了头尾相连，互相等待的情况，对于这种问题以及上面案例的解决，一般会尝试将锁改为超时锁，比如lock的trylock，
  超时会自动释放锁，从而避免一直持有锁的情况发生

5.4 CPU被抢占
```
CPU usage from 0ms to 10625ms later (2020-03-09 14:38:31.633 to 2020-03-09 14:38:42.257):
  543% 2045/com.alibaba.android.rimet: 54% user + 89% kernel / faults: 4608 minor 1 major ————关键行！！！
  99% 674/android.hardware.camera.provider@2.4-service: 81% user + 18% kernel / faults: 403 minor
  24% 32589/com.wang.test: 22% user + 1.4% kernel / faults: 7432 minor 1 major
  ........省略N行.....
```
如上日志，第二行是钉钉的进程，占据CPU高达543%，抢占了大部分CPU资源，因而导致发生ANR


5.5.1 内存紧张导致ANR
如果有一份日志，CPU和堆栈都很正常（不贴出来了），仍旧发生ANR，考虑是内存紧张。
从CPU第一行信息可以发现，ANR的时间点是2020-10-31 22:38:58.468—CPU usage from 0ms to 21752ms later (2020-10-31 22:38:58.468 to 2020-10-31 22:39:20.220)
接着去系统日志里搜索am_meminfo， 这个没有搜索到。再次搜索onTrimMemory，果然发现了很多条记录；
```
10-31 22:37:19.749 20733 20733 E Runtime : onTrimMemory level:80,pid:com.xxx.xxx:Launcher0
10-31 22:37:33.458 20733 20733 E Runtime : onTrimMemory level:80,pid:com.xxx.xxx:Launcher0
10-31 22:38:00.153 20733 20733 E Runtime : onTrimMemory level:80,pid:com.xxx.xxx:Launcher0
10-31 22:38:58.731 20733 20733 E Runtime : onTrimMemory level:80,pid:com.xxx.xxx:Launcher0
10-31 22:39:02.816 20733 20733 E Runtime : onTrimMemory level:80,pid:com.xxx.xxx:Launcher0
```

可以看出，在发生ANR的时间点前后，内存都处于紧张状态，level等级是80，查看Android API 文档；   //todo 常见level
```
 /**
    * Level for {@link #onTrimMemory(int)}: the process is nearing the end
    * of the background LRU list, and if more memory isn't found soon it will
    * be killed.
    */
   static final int TRIM_MEMORY_COMPLETE = 80;
```
可知80这个等级是很严重的，应用马上就要被杀死，被杀死的这个应用从名字可以看出来是桌面，连桌面都快要被杀死，那普通应用能好到哪里去呢？
一般来说，发生内存紧张，会导致多个应用发生ANR，所以在日志中如果发现有多个应用一起ANR了，可以初步判定，此ANR与你的应用无关。

5.5.1 内存紧张导致的频繁GC
 大量GC占用CPU资源，导致卡死
搜索GC time
```
Total mutator paused time: 1.246s
Total time waiting for GC to complete: 1.458s
Total GC count: 2914
Total GC time: 238.825s
Total blocking GC count: 28
Total blocking GC time: 10.019s
```
读写内存，文件，频繁gc 
场景：主线程接受蓝牙数据，保存在内存中(发生频繁扩容和拷贝数据,List保存到文件会写入很多空数据，内部的数组没有填满)，然后写入文件  可能是几兆的拷贝
解决拿到数据的大小，byte[] a = new byte[size]; 这样就不用频繁扩容和拷贝数据了
```
suspend all histogram:	Sum: 3.291s 99% C.I. 0.002ms-5.455ms Avg: 1.398ms Max: 7.181ms
DALVIK THREADS (43):
"Binder:16460_6" prio=5 tid=17 Runnable
  | group="main" sCount=0 ucsCount=0 flags=2 obj=0x13342970 self=0xb400006fbb49f400
  | sysTid=17003 nice=0 cgrp=default sched=0/0 handle=0x6fa42e7cb0
  | state=R schedstat=( 5067801514 832518623 5911 ) utm=174 stm=332 core=7 HZ=100
  | stack=0x6fa41f0000-0x6fa41f2000 stackSize=991KB
  | held mutexes= "mutator lock"(shared held)
  native: #00 pc 000000000056acfc  /apex/com.android.art/lib64/libart.so (art::DumpNativeStack(std::__1::basic_ostream<char, std::__1::char_traits<char> >&, int, BacktraceMap*, char const*, art::ArtMethod*, void*, bool)+144)
  native: #01 pc 0000000000685fa8  /apex/com.android.art/lib64/libart.so (art::Thread::DumpStack(std::__1::basic_ostream<char, std::__1::char_traits<char> >&, bool, BacktraceMap*, bool) const+368)
  native: #02 pc 00000000006a43dc  /apex/com.android.art/lib64/libart.so (art::DumpCheckpoint::Run(art::Thread*)+924)
  native: #03 pc 0000000000686f40  /apex/com.android.art/lib64/libart.so (art::Thread::RunCheckpointFunction()+180)
  native: #04 pc 000000000074cc24  /apex/com.android.art/lib64/libart.so (art::JniMethodFastEnd(unsigned int, art::Thread*)+120)
  at java.lang.System.arraycopy(Native method)
  at ....ble.cmd.impl.FileDataMsgHandler.addToDatas(FileDataMsgHandler.java:140)
  at .......ble.cmd.impl.FileDataMsgHandler.handleInner(FileDataMsgHandler.java:57)
  at .......ble.cmd.MessageHandlerBase.handle(MessageHandlerBase.java:45)
  at .......ble.MessageService.handleMessage(MessageService.java:116)
  at .......ble.BleManager$2.onCharacteristicChanged(BleManager.java:192)
  at android.bluetooth.BluetoothGatt$1$8.run(BluetoothGatt.java:490)
  at android.bluetooth.BluetoothGatt.runOrQueueCallback(BluetoothGatt.java:823)
  at android.bluetooth.BluetoothGatt.access$200(BluetoothGatt.java:47)
  at android.bluetooth.BluetoothGatt$1.onNotify(BluetoothGatt.java:484)
  at android.bluetooth.IBluetoothGattCallback$Stub.onTransact(IBluetoothGattCallback.java:315)
  at android.os.Binder.execTransactInternal(Binder.java:1187)
  at android.os.Binder.execTransact(Binder.java:1146)
```

5.6 系统服务超时导致ANR  或者主线程Binder调用等待超时
系统服务超时一般会包含BinderProxy.transactNative关键字，请看如下日志：
```
"main" prio=5 tid=1 Native
  | group="main" sCount=1 dsCount=0 flags=1 obj=0x727851e8 self=0x78d7060e00
  | sysTid=4894 nice=0 cgrp=default sched=0/0 handle=0x795cc1e9a8
  | state=S schedstat=( 8292806752 1621087524 7167 ) utm=707 stm=122 core=5 HZ=100
  | stack=0x7febb64000-0x7febb66000 stackSize=8MB
  | held mutexes=
  kernel: __switch_to+0x90/0xc4
  kernel: binder_thread_read+0xbd8/0x144c
  kernel: binder_ioctl_write_read.constprop.58+0x20c/0x348
  kernel: binder_ioctl+0x5d4/0x88c
  kernel: do_vfs_ioctl+0xb8/0xb1c
  kernel: SyS_ioctl+0x84/0x98
  kernel: cpu_switch_to+0x34c/0x22c0
  native: #00 pc 000000000007a2ac  /system/lib64/libc.so (__ioctl+4)
  native: #01 pc 00000000000276ec  /system/lib64/libc.so (ioctl+132)
  native: #02 pc 00000000000557d4  /system/lib64/libbinder.so (android::IPCThreadState::talkWithDriver(bool)+252)
  native: #03 pc 0000000000056494  /system/lib64/libbinder.so (android::IPCThreadState::waitForResponse(android::Parcel*, int*)+60)
  native: #04 pc 00000000000562d0  /system/lib64/libbinder.so (android::IPCThreadState::transact(int, unsigned int, android::Parcel const&, android::Parcel*, unsigned int)+216)
  native: #05 pc 000000000004ce1c  /system/lib64/libbinder.so (android::BpBinder::transact(unsigned int, android::Parcel const&, android::Parcel*, unsigned int)+72)
  native: #06 pc 00000000001281c8  /system/lib64/libandroid_runtime.so (???)
  native: #07 pc 0000000000947ed4  /system/framework/arm64/boot-framework.oat (Java_android_os_BinderProxy_transactNative__ILandroid_os_Parcel_2Landroid_os_Parcel_2I+196)
  at android.os.BinderProxy.transactNative(Native method) ————————————————关键行！！！
  at android.os.BinderProxy.transact(Binder.java:804)
  at android.net.IConnectivityManager$Stub$Proxy.getActiveNetworkInfo(IConnectivityManager.java:1204)—关键行！
  at android.net.ConnectivityManager.getActiveNetworkInfo(ConnectivityManager.java:800)
  at com.xiaomi.NetworkUtils.getNetworkInfo(NetworkUtils.java:2)
  at com.xiaomi.frameworkbase.utils.NetworkUtils.getNetWorkType(NetworkUtils.java:1)
  at com.xiaomi.frameworkbase.utils.NetworkUtils.isWifiConnected(NetworkUtils.java:1)
```
从堆栈可以看出获取网络信息发生了ANR：getActiveNetworkInfo。
前文有讲过：系统的服务都是Binder机制（16个线程），服务能力也是有限的，有可能系统服务长时间不响应导致ANR。如果其他应用占用了所有Binder线程，
   那么当前应用只能等待。     //应该是15个  Binder 线程数限制，默认为15个 binder 线程  /frameworks/native/libs/binder/ProcessState.cpp
//系统服务压力过大还有一种，应用开启了保活相关操作，频繁拉起进程，系统服务类进程会在SystemServer中启动，注册在ServiceManager
可进一步搜索：blockUntilThreadAvailable关键字：
```
at android.os.Binder.blockUntilThreadAvailable(Native method)
```
如果有发现某个线程的堆栈，包含此字样，可进一步看其堆栈，确定是调用了什么系统服务。此类ANR也是属于系统环境的问题，如果某类型机器上频繁发生此问题，
  应用层可以考虑规避策略。
sysTid 代表系统进程号 4894 查找相关进程信息  
与4894通信的进程 在anr/binderinfo  找到与4894通信的进程，假如是1666号进程    找到可以继续下trace中查看1666的调用栈 
      context binder //binder相关信息
         thread 4894: 1 10 need_return 0 tr 0   
         outgoing transaction 5011: d6a12d00 from 4896:4896 to 1666:1758 ...

另一个博主对binder线程用完的判断
判断Binder是否用完，可以在trace中搜索关键字"binder_f"，如果搜索到则表示已经用完，然后就要找log其他地方看是谁一直在消耗binder或者是有死锁发生
之前有遇到过压力测试手电筒应用，出现BInder线程池被占满情况，解决的思路就是降低极短时间内大量Binder请求的发生，修复的手法是发送BInder请求的函数中做时间差过滤，
   限定在500ms内最多执行一次

查看系统耗时关键字:binder_sample,dvm_lock_sample,am_lifecycle_sample,binder thread
监控每个进程的主线程的binder transaction的耗时情况, 当超过阈值（比如：500ms）时,则输出相应的目标调用信息
```
2754 2754 I binder_sample: [android.app.IActivityManager,35,2900,android.process.media,5]
1.主线程2754
2.执行android.app.IActivityManager接口,<br>
3.所对应方法code =35(即STOP_SERVICE_TRANSACTION),<br>
4.所花费时间为2900ms,<br>
5.该block所在package为 android.process.media.<br>
最后一个参数是sample比例(没有太大价值)</p>
```
dvm_lock_sample:当某个线程等待lock的时间blocked超过阈值（比如：500ms）,则输出当前的持锁状态.
```
dvm_lock_sample: [system_server,1,Binder_9,1500,ActivityManagerService.java,6403,-,1448,0]
说明：system_server: Binder_9,执行到ActivityManagerService.java的6403行代码,一直在等待AMS锁, "-"代表持锁的是同一个文件，
即该锁被同一文件的1448行代码所持有, 从而导致Binder_9线程被阻塞1500ms.
```
am_lifecycle_sample： 当app在主线程的生命周期回调方法执行时间超过阈值（比如：3000ms）,则输出相应信息.
```
02-23 11:02:35.876 8203 8203 I am_lifecycle_sample: [0,com.android.systemui,114,3837]
说明： pid=8203, processName=com.android.systemui, MessageCode=114(CREATE_SERVICE), 耗时3.827s
注意: MessageCode=200 (并行广播onReceive耗时), 其他Code见 ActivityThread.H类
```
binder thread：当system_server等进程的线程池使用完, 无空闲线程时, 则binder通信都处于饥饿状态, 则饥饿状态超过一定阈值则输出信息.
```
1232 1232 "binder thread pool (16 threads) starved for 100 ms"
说明:  system_server进程的 线程池已满的持续长达100ms
以上的binder call 信息,我们查找日志中的持锁信息
12-17 02:08:44.559 1999 4899 I dvm_lock_sample: [system_server,1,Binder:1999_16,68916,PackageManagerService.java,3537,UserManagerService.java,3380,0]
12-17 02:08:44.696 1999 4131 I dvm_lock_sample: [system_server,1,Binder:1999_14,61042,PackageManagerService.java,3537,UserManagerService.java,3380,0]
PackageManagerService.java第3537需要的锁被UserManagerService.java中的3380行持有,我们再结合源码查看UserManagerService.java中的3380行是因为什么原因导致持锁耗时
注意:binder call出现耗时,有binder在通信过程中因为繁忙造成,也有可能因为对端持锁或者执行一些耗时的操作耗时,
日志中打印binder call信息表明binder call与远程端通信已经结束,出现binder call信息也不不代表framework出现问题,
需要根据日志分析准确定位.
```

5.7  io阻塞
卡在IO上        //todo  mainlog
这种情况一般是和文件操作相关，判断是否是这种情况，可以看mainlog中搜索关键字"ANR in",看这段信息的最下边，比如下面的信息
ANRManager: 100% TOTAL: 2% user + 2.1% kernel + 95% iowait + 0.1% softirq
很明显，IO占比很高，这个时候就需要查看trace日志看当时的callstack，或者在这段ANR点往前看0~4s，看看当时做的什么文件操作
   ，这种场景有遇到过，常见解决方法是对耗时文件操作采取异步操作

5.8
JE或者NE导致ANR     Java layer exception/Native layer exception      //todo android异常机制https://www.cnblogs.com/xiyuan2016/p/6740623.html
这种场景有遇到过，ANR前出现频繁NE，NE所在的进程与ANR的进程有交互，在解决了NE后，ANR也不复存在，对于这类在ANR前有JE或者NE，
  一般思路是先解决JE或NE，因为JE/NE发生时会去dump一大堆异常信息，本身也会加重CPU loading，修改完异常后再来看ANR是否还存在，
  如果还存在，那么就看trace 堆栈，如果不存在，则可以基本判定是JE或NE导致
//todo android 各种log https://www.jianshu.com/p/f753ee1ffc87?ivk_sa=1024320u


5.9
只存在于Monkey测试下
有些问题是只有在Monkey环境下才能跑出来，平时的user版本用户使用是不会出现的，这种问题的话就没有改动的意义。
```
ActivityManager: Not finishing activity because controller resumed
03-18 07:25:50.901 810 870 I am_anr : [0,25443,android.process.media,1086897733,Input dispatching timed out 
   (Waiting because no window has focus but there is a focused application that may eventually add a window when
    it finishes starting up.)]
```
发生这个ANR的原因是Contoller将resume的操作给拦截了, 导致Focus不过去, 从而导致ANR，User版本不会有Contoller, 所以不会出现这个 ANR. 
所以这个 ANR 可以忽略.



6.0
设置UncaughtExceptionHandler后没有调用系统默认的handler
```
Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(final Thread t, Throwable e) {
                Log.i("uncaught", Thread.currentThread().toString());
                Log.i("uncaught", t.toString());
            }
        });
```
堆栈表现
```
at android.os.MessageQueue.nativePollOnce(Native method)
at android.os.MessageQueue.next(MessageQueue.java:323)
at android.os.Looper.loop(Looper.java:135)
at android.app.ActivityThread.main(ActivityThread.java:5417)
at java.lang.reflect.Method.invoke!(Native method)
at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:726)
at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:616)
```

6.1
日志中每次调用gson解析和binder通信，日志频繁时可能anr
对于固定bean的toJson，可以手动拼接，不调用GSON等三方库，减少反射等性能损耗