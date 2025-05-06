binder通信有没有超时机制？ 假如将通信放入单线程池中，没有超时岂不是将队列阻塞了

https://mp.weixin.qq.com/s/2xcSvHlpxS3xgHP2PBQsBA

https://www.jianshu.com/p/adaa1a39a274
ipcSetDataReference  https://juejin.cn/post/6868901776368926734

bbinder到Java层  https://juejin.cn/post/6890088205916307469#heading-3

https://weishu.me/2016/01/12/binder-index-for-newer/
https://github.com/xfhy/Android-Notes/tree/499b9832a344aff12c9976922da7860058204dd1/Blogs/Android/%E5%A4%9A%E8%BF%9B%E7%A8%8B
bpbinder与bbinder 在驱动层的关系https://juejin.cn/post/6867139592739356686#heading-7


为什么有binder
Linux中提供了很多进程间通信机制，主要有管道（pipe）、信号（sinal）、信号量（semophore）、消息队列（Message）、
共享内存（Share Memory)、套接字（Socket）等
原因：
性能方面
性能方面主要影响的因素是拷贝次数，管道、消息队列、Socket的拷贝次书都是两次，性能不是很好，共享内存不需要拷贝，性能最好，
Binder的拷贝次数为1次，性能仅次于内存拷贝。
稳定性方面
Binder是基于C/S架构的，这个架构通常采用两层结构，在技术上已经很成熟了，稳定性是没有问题的。共享内存没有分层，难以控制，
并发同步访问临界资源时，可能还会产生死锁。从稳定性的角度讲，Binder是优于共享内存的。
安全方面
Android是一个开源的系统，并且拥有开放性的平台，市场上应用来源很广，因此安全性对于Android 平台而言极其重要。
传统的IPC接收方无法获得对方可靠的进程用户ID/进程ID（UID/PID），无法鉴别对方身份。Android 为每个安装好的APP分配了自己的UID，
通过进程的UID来鉴别进程身份。另外，Android系统中的Server端会判断UID/PID是否满足访问权限，而对外只暴露Client端，加强了系统的安全性。
//安全性，共享内存可通过反射直接获取，Linux内核内存不能获取则通过binder机制
语言方面
Linux是基于C语言，C语言是面向过程的，Android应用层和Java Framework是基于Java语言，Java语言是面向对象的。
Binder本身符合面向对象的思想，因此作为Android的通信机制更合适不过。


binder通信模式
1 oneway(aidl文件中方法声明关键字)             
客户端只需要把请求发送到服务端就可以立即返回，而不需要等待服务端的结果，这是一种非阻塞方式
```
方法调用有flag
mRemote.transact(Stub.TRANSACTION_invokeMethodInMyService, _data, null, android.os.IBinder.FLAG_ONEWAY);
```
2 普通模式/非oneway    客户端发起调用时，客户端一般会阻塞，直到服务端返回结果


java层

native
客户端(用户空间)
   open  ioctl      mmap(建立服务端与内核的内存映射)   
binder驱动 内核空间  dev/binder   copy_from_user   copy_to_user
服务端(用户空间)      

ioctl(fd, BINDER_SET_MAX_THREADS, &maxThreads) 
  binder驱动进行通信，设置最大线程16  还有其他命令


Binder驱动
binder_init：初始化字符设备  创建dev/binder文件
binder_open 打开驱动设备
binder_mmap：申请内存空间  内存映射
binder_ioctl：执行相应的ioctl操作


https://juejin.cn/post/6890088205916307469#heading-3
内核缓存区和数据接收缓存区都通过mmap映射到服务端进程，为什么
参考资料《写给Android应用工程师的Binder原理剖析》评论区提到：
内核缓存区和数据接收缓存区其实是虚拟内存层面的区分（Linux是使用的虚拟内存寻址方式，虚拟内存需要映射一块真实的物理内存），
内核缓存区和数据接收区的映射就是指向了同一块物理内存。接收方下次也可能是发送方，如果共用一块缓存那么岂不是发送方，
内核，接收方都指向了同一块物理内存，违反了进程隔离的设计原则