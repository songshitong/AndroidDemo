

pidfd

https://zhuanlan.zhihu.com/p/381302990
历史由来
使用pid来表示进程方式存在着一些问题，其中最大问题是PID会被重复回收利用：当某个进程退出的时候，它释放的PID可能会在一段时间后会重新分配给其他新进程使用。
这就会造成一种竞争问题(称为race-free process signaling)：例如进程server本来是通过进程A的PID给进程A发送信号，但是发送信号前进程A退出了，
且进程A的PID号很快就被重复分配给了另外一个进程B，此时server就会错把信号发送给了B，严重的话会导致B进程被迫退出。
针对这个问题Linux内核在Linux-5.1引入了一个新系统调用pidfd_send_signal(2)以通过操作/proc/<pid>文件描述符的方式来解决该问题

基本原理
Pidfd本质上是文件描述符，但是它在形式上没有一个对应的实际可见的文件路径(dentry)，而是采用anon_inode(匿名节点)方式来实现。

与PID(process ID)的实现不同，pidfd是通过专有的pidfd open函数"打开"目标进程dst task，然后返回dst task对应的文件描述符pidfd；
在当前进程未主动调用close()函数关闭pidfd前不论dst task是否退出，pidfd对于当前任务都是有效的，这就避免了PID"race-free process signaling"的问题。

https://lwn.net/Articles/773459/
https://lwn.net/Articles/784831/

api
```
int pidfd_open(pid_t pid, unsigned int flags);
int pidfd_send_signal(int pidfd, int sig, siginfo_t *info, unsigned int flags)
pidfd_poll() //在pidfd未出现之前，如果不是父子进程关系，进程是无法监控一个另外进程是否退出的；pidfd机制的出现弥补了这一空缺需求。
   //有了文件描述符，内核可很方便的实现poll机制
```