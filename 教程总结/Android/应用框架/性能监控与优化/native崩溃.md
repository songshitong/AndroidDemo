
http://blog.chinaunix.net/uid-24098129-id-312659.html 参考？？
https://mp.weixin.qq.com/s/g-WzYF3wWAljok1XjPoo7w?
https://jekton.github.io/2019/04/06/native-crash-catching/

native空指针 https://www.jianshu.com/p/22490ef69039
```
 int *p = 0; //空指针
 *p = 1; //写空指针指向的内存，产生SIGSEGV信号，造成crash
```

信号机制
1.程序奔溃
在Unix-like系统中，所有的崩溃都是编程错误或者硬件错误相关的，系统遇到不可恢复的错误时会触发崩溃机制让程序退出，如除零、段地址错误等。
异常发生时，CPU通过异常中断的方式，触发异常处理流程。不同的处理器，有不同的异常中断类型和中断处理方式。
linux把这些中断处理，统一为信号量，可以注册信号量向量进行处理。
信号机制是进程之间相互传递消息的一种方法，信号全称为软中断信号。

信号机制
函数运行在用户态，当遇到系统调用、中断或是异常的情况时，程序会进入内核态。信号涉及到了这两种状态之间的转换
native崩溃_信号机制.jpg
(1) 信号的接收
接收信号的任务是由内核代理的，当内核接收到信号后，会将其放到对应进程的信号队列中，同时向进程发送一个中断，使其陷入内核态。注意，此时信号还只是在队列中，
对进程来说暂时是不知道有信号到来的。

(2) 信号的检测
进程陷入内核态后，有两种场景会对信号进行检测：
进程从内核态返回到用户态前进行信号检测
进程在内核态中，从睡眠状态被唤醒的时候进行信号检测
当发现有新信号时，便会进入下一步，信号的处理。

(3) 信号的处理
信号处理函数是运行在用户态的，调用处理函数前，内核会将当前内核栈的内容备份拷贝到用户栈上，并且修改指令寄存器（eip）将其指向信号处理函数。
接下来进程返回到用户态中，执行相应的信号处理函数。

信号处理函数执行完成后，还需要返回内核态，检查是否还有其它信号未处理。如果所有信号都处理完成，就会将内核栈恢复（从用户栈的备份拷贝回来），
同时恢复指令寄存器（eip）将其指向中断前的运行位置，最后回到用户态继续执行进程。

至此，一个完整的信号处理流程便结束了，如果同时有多个信号到达，上面的处理流程会在第2步和第3步骤间重复进行。

(4) 常见信号量类型   https://www.jianshu.com/p/730989a7302e
编号	信号名称	缺省动作	说明
1	SIGHUP	终止	终止控制终端或进程
2	SIGINT	终止	键盘产生的中断(Ctrl-C)
3	SIGQUIT	dump	键盘产生的退出
4	SIGILL	dump	非法指令
5	SIGTRAP	dump	debug中断
6	SIGABRT／SIGIOT	dump	异常中止
7	SIGBUS／SIGEMT	dump	总线异常/EMT指令
8	SIGFPE	dump	浮点运算溢出
9	SIGKILL	终止	强制进程终止
10	SIGUSR1	终止	用户信号,进程可自定义用途
11	SIGSEGV	dump	非法内存地址引用
12	SIGUSR2	终止	用户信号，进程可自定义用途
13	SIGPIPE	终止	向某个没有读取的管道中写入数据
14	SIGALRM	终止	时钟中断(闹钟)
15	SIGTERM	终止	进程终止
16	SIGSTKFLT	终止	协处理器栈错误
17	SIGCHLD	忽略	子进程退出或中断
18	SIGCONT	继续	如进程停止状态则开始运行
19	SIGSTOP	停止	停止进程运行
20	SIGSTP	停止	键盘产生的停止
21	SIGTTIN	停止	后台进程请求输入
22	SIGTTOU	停止	后台进程请求输出
23	SIGURG	忽略	socket发生紧急情况
24	SIGXCPU	dump	CPU时间限制被打破
25	SIGXFSZ	dump	文件大小限制被打破
26	SIGVTALRM	终止	虚拟定时时钟
27	SIGPROF	终止	profile timer clock
28	SIGWINCH	忽略	窗口尺寸调整
29	SIGIO/SIGPOLL	终止	I/O可用
30	SIGPWR	终止	电源异常
31	SIGSYS／SYSUNUSED	dump	系统调用异常
