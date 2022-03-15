http://gityuan.com/2016/01/17/systrace/

默认的事件是系统插入的，想获取业务代码的耗时可以自己插入
自定义systrace
java framework层
```
import android.os.Trace;
Trace.traceBegin(long traceTag, String methodName)
Trace.traceEnd(long traceTag)
```
在代码中必须成对出现，一般将traceEnd放入到finally语句块，另外，必须在同一个线程。
这里默认的traceTag为TRACE_TAG_APP，systrace命令通过指定app参数即

native framework层
```
#include<utils/Trace.h>
ATRACE_CALL();
```
如果使用插住的方法，对于log、加解密等在底层非常频繁调用的函数，需要进行过滤，防止出现大量毛刺



常用操作   enter停止收集数据
```
python systrace.py --list-categories   //列举可用类型，对缩写有解释 例如，wm是WindowManager,
python systrace.py --help
python systrace.py gfx view wm am pm ss dalvik  sched -b 90960 -a com.sample.systrace  -o test.log.html
  //-b 是设定buffer   buffer不要过大，可能oom
  // -a APP_NAME, --app=APP_NAME   
  //-o输出
  //-t N, –time=N	执行时间，默认5s
  //-k <KFUNCS>，–ktrace=<KFUNCS>	追踪kernel函数，用逗号分隔

//输出全部trace信息
python systrace.py -b 32768 -t 5 -o mytrace.html gfx input view webview wm am sm audio video camera hal app res dalvik rs bionic power sched irq freq idle disk mmc load sync workq memreclaim regulators  
```


查看报告
使用chrome打开test.log.html
1.打开上方的Processes，过滤进程 sst.example.androiddemo.feature (pid 2956)
  kernel显示CPU，例如8核CPU，显示0-7
2.查看Frames行
在每个app进程，都有一个Frames行，正常情况以绿色的圆点表示。当圆点颜色为黄色或者红色时，意味着这一帧超过16.6ms（即发现丢帧），
  这时需要通过放大那一帧进一步分析问题。
  对于Android 5.0(API level 21)或者更高的设备，该问题主要聚焦在UI Thread和Render Thread这两个线程当中。
  对于更早的版本，则所有工作在UI Thread。
3. 查看Alerts
   Systrace能自动分析trace中的事件，并能自动高亮性能问题作为一个Alerts，建议调试人员下一步该怎么做。
   Alerts可以在右侧的Alerts的tab查看，或者点击第一行的Alerts的圆点查看
   对于丢帧时，点击黄色或红色的Frames圆点便会有相关的提示信息，可以按m键进行标记
4. 查看函数信息
  例如点击inflate函数，可以看到相关信息，最好放大到一定程度才能点击，不然容易点击失效  
```
Title	inflate 
Category	android
User Friendly Category	other
Start	2,452.130 ms
Start (Absolute time)	1,553,390,184.676 ms
Wall Duration	53.505 ms
CPU Duration	53.505 ms
Self Time	51.867 ms
CPU Self Time	51.867 ms
Description	  Constructing a View hierarchy from pre-processed XML via LayoutInflater#layout. 
     This includes constructing all of the View objects in the hierarchy, and applying styled attributes.
```   
   


快捷键操作
导航操作
导航操作	作用
w	放大，[+shift]速度更快
s	缩小，[+shift]速度更快
a	左移，[+shift]速度更快
d	右移，[+shift]速度更快

常用操作	作用
f	放大当前选定区域
m	标记当前选定区域
v	高亮VSync  
g	切换是否显示60hz的网格线
0	恢复trace到初始态，这里是数字0而非字母o


鼠标模式切换
1. Select mode: 双击已选定区能将所有相同的块高亮选中；（对应数字1）
2. Pan mode: 拖动平移视图（对应数字2）   //按住鼠标左键可以进行视图拖动
3. Zoom mode:通过上/下拖动鼠标来实现放大/缩小功能；（对应数字3） //按住鼠标左键拖动，可以进行放大缩小
4. Timing mode:拖动来创建或移除时间窗口线。（对应数字4）


//todo  案例分析 https://zhuanlan.zhihu.com/p/40097338

https://www.androidperformance.com/2019/07/23/Android-Systrace-Pre/#/%E7%B3%BB%E5%88%97%E6%96%87%E7%AB%A0%E7%9B%AE%E5%BD%95
线程信息
在每个方法上面都会有对应的线程状态来标识目前线程所处的状态，Systrace 会用不同的颜色来标识不同的线程状态
 方法上面的细线段就是，没有单独的行
绿色 : 运行中
只有在该状态的线程才可能在 cpu 上运行。而同一时刻可能有多个线程处于可执行状态，这些线程的 task_struct 结构被放入对应 cpu 的可执行队列中
  （一个线程最多只能出现在一个 cpu 的可执行队列中）。调度器的任务就是从各个 cpu 的可执行队列中分别选择一个线程在该cpu 上运行
作用：我们经常会查看 Running 状态的线程，查看其运行的时间，与竞品做对比，分析快或者慢的原因：
1 是否频率不够？
2 是否跑在了小核上？
3 是否频繁在 Running 和 Runnable 之间切换？为什么？
4 是否频繁在 Running 和 Sleep 之间切换？为什么？
5 是否跑在了不该跑的核上面？比如不重要的线程占用了超大核
点击蓝色的细线段可以显示详细信息
```
Thread Timeslice (1)

Running process:	sst.example.androiddemo.feature (pid 2956)   //运行的进程
Running thread:	UI thread    //运行的线程
State:	Running               //线程的状态
Start:	2,544.560 ms
Duration:	13.129 ms
On CPU:	CPU 7                //线程执行在哪个CPU
Running instead:
```

蓝色 : 可运行
线程可以运行但当前没有安排，在等待 cpu 调度
作用：Runnable 状态的线程状态持续时间越长，则表示 cpu 的调度越忙，没有及时处理到这个任务：
1 是否后台有太多的任务在跑？
2 没有及时处理是因为频率太低？
3 没有及时处理是因为被限制到某个 cpuset 里面，但是 cpu 很满？
4 此时 Running 的任务是什么？为什么？
```
Thread Timeslice (1)

Running process:	sst.example.androiddemo.feature (pid 2956)
Running thread:	RenderThread
State:	Runnable
Start:	2,590.562 ms
Duration:	1.690 ms
On CPU:     //on cpu是空的，没有对应的CPU执行
Running instead:	com.miui.home
```

白色 : 休眠中
线程没有工作要做，可能是因为线程在互斥锁上被阻塞。
作用 ： 这里一般是在等事件驱动
```
Thread Timeslice (1)

Running process:	sst.example.androiddemo.feature (pid 2956)
Running thread:	RenderThread
State:	Sleeping
Start:	2,584.194 ms
Duration:	2.469 ms
On CPU:
Running instead:	dp_rx_thread_0
```
点击dp_rx_thread_0展示的信息
```
CPU Slice (1)

Running process:	[dp_rx_thread_0] (pid 23815)
Running thread:	UI thread
Start:	2,584.194 ms
Duration:	0.058 ms
Active slices:	Click to select
Args:	
{comm: "dp_rx_thread_0",tid: 23815,prio: 119,stateWhenDescheduled: "S"}
```

橘色 : 不可中断的睡眠态 IO Block
线程在I / O上被阻塞或等待磁盘操作完成，一般底线都会标识出此时的 callsite ：wait_on_page_locked_killable
作用：这个一般是标示 io 操作慢，如果有大量的橘色不可中断的睡眠态出现，那么一般是由于进入了低内存状态，申请内存的时候触发 pageFault,
  linux 系统的 page cache 链表中有时会出现一些还没准备好的 page(即还没把磁盘中的内容完全地读出来) , 而正好此时用户在访问
  这个 page 时就会出现 wait_on_page_locked_killable 阻塞了. 只有系统当 io 操作很繁忙时, 每笔的 io 操作都需要等待排队时, 
  极其容易出现且阻塞的时间往往会比较长
```
Thread Timeslice (1)

Running process:	sst.example.androiddemo.feature (pid 2956)
Running thread:	RenderThread
State:	Uninterruptible Sleep - Block I/O
Start:	2,650.885 ms
Duration:	0.722 ms
On CPU:
Running instead:
Args:	
{kernel callsite when blocked:: "__lock_page+0x1b0/0x240"}
```

紫色 : 不可中断的睡眠态
线程在另一个内核操作（通常是内存管理）上被阻塞。
作用：一般是陷入了内核态，有些情况下是正常的，有些情况下是不正常的，需要按照具体的情况去分析
```
Thread Timeslice (1)

Running process:	sst.example.androiddemo.feature (pid 2956)
Running thread:	UI thread
State:	Uninterruptible Sleep
Start:	
2,373.061 ms
Duration:	
0.843 ms
On CPU:
Running instead:	kworker/7:2H
```

Args参数解析
```
Args:{comm: "dp_rx_thread_0",tid: 23815,prio: 119,stateWhenDescheduled: "S"}
```
args的参数tid，线程id是23815
stateWhenDescheduled 进程状态
Linux 常见的进程状态
1 D 无法中断的休眠状态（通常 IO 的进程）；
2 R 正在可运行队列中等待被调度的；
3 S 处于休眠状态；
4 T 停止或被追踪；
5 W 进入内存交换 （从内核2.6开始无效）；
6 X 死掉的进程 （基本很少見）；
7 Z 僵尸进程；
8 < 优先级高的进程
9 N 优先级较低的进程
10 L 有些页被锁进内存
11 s 进程的领导者（在它之下有子进程）
12 l 多进程的（使用 CLONE_THREAD, 类似 NPTL pthreads）
13 + 位于后台的进程组
14 I (Idle)，空闲状态  
  用在不可中断睡眠的内核线程上硬件交互导致的不可中断进程用 D 表示，但对某些内核线程，它们有可能实际上并没有任何负载，
  用Idle 正是为了区分这种情况。
  要注意，D 状态的进程会导致平均负载升高，I 状态的进程却不会


线程唤醒信息分析
Systrace 会标识出一个非常有用的信息，可以帮助我们进行线程调用等待相关的分析。
Systrace 可以标示出这个的一个原因是，一个任务在进入 Running 状态之前，会先进入 Runnable 状态进行等待，
   而 Systrace 会把这个状态也标示在 Systrace 上（非常短，需要放大进行看，进入sleep，然后是Running状态，中间有一段蓝色的Runnable标识被谁唤醒了）

一个线程被唤醒的信息往往比较重要，知道他被谁唤醒，那么我们也就知道了他们之间的调用等待关系，如果一个线程出现一段比较长的 sleep 情况，
然后被唤醒，那么我们就可以去看是谁唤醒了这个线程，对应的就可以查看唤醒者的信息，看看为什么唤醒者这么晚才唤醒。

一个常见的情况是：应用主线程程使用 Binder 与 SystemServer 的 AMS 进行通信，但是恰好 AMS 的这个函数正在等待锁释放
（或者这个函数本身执行时间很长），那么应用主线程就需要等待比较长的时间，那么就会出现性能问题，比如响应慢或者卡顿，
这就是为什么后台有大量的进程在运行，或者跑完 Monkey 之后，整机性能会下降的一个主要原因

另外一个场景的情况是：应用主线程在等待此应用的其他线程执行的结果，这时候线程唤醒信息就可以用来分析主线程到底被哪个线程 Block 住了，
 比如下面这个场景，这一帧 doFrame 执行了 93ms(可以对这个方法进行M键标记查看上方的时间)，有明显的异常，但是大部分时间是在 sleep
查看图中存在一段线程sleeping状态,而后是绿色的进行状态 
android_systrace_线程唤醒1.png
放大后(非常小，需要多次放大)，查看蓝色的Runnable状态，然后进行m键标记
android_systrace_线程唤醒2.png
```
Running process:	sst.example.androiddemo.feature (pid 2956)
Running thread:	UI thread
State:	Runnable
Start:	2,607.604 ms
Duration:	0.042 ms
On CPU:
Running instead:	Binder:2956_2
Args:	{wakeup from tid: 3009}    //被线程3009唤醒
```
根据args信息，线程是被3009唤醒的,如果牵扯到了 App 自身的代码逻辑，需要去确定该线程做了什么事情

根据M键标记的范围，拉倒最上方的CPU任务行，查看每个任务的信息与线程，查看  UI线程被RenderThread唤醒   也可以直接搜索线程号查找
android_systrace_线程唤醒3.png
```
Running process:	sst.example.androiddemo.feature (pid 2956)
Running thread:	RenderThread
Start:	2,607.515 ms
Duration:	0.094 ms
Active slices:	Click to select
Args:	
{comm: "RenderThread",
 tid: 3009,
 prio: 110,
 stateWhenDescheduled: "S"}
```
此时查看RenderThread的任务，第一次执行setSurface,Creating EGLContext
配合g键的60hz，查看那些方法超过了16ms，按m查看具体的耗时
可以查看主线程任务的状态，正在等待被调度
```
Running process:	sst.example.androiddemo.feature (pid 2956)
Running thread:	UI thread
Start:	
2,607.646 ms
Duration:	5.109 ms
Active slices:	Click to select
Args:	
{comm: "oiddemo.feature",
 tid: 2956,
 prio: 110,
 stateWhenDescheduled: "R"}  //正在可运行队列中等待被调度的
```