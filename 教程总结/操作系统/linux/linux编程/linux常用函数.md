linux system programming

TEMP_FAILURE_RETRY
宏定义  unistd.h
```
#ifndef TEMP_FAILURE_RETRY
#define TEMP_FAILURE_RETRY(expression) ({     \
  __typeof(expression) __result;              \
  do {                                        \
    __result = (expression);                  \
  } while (__result == -1 && errno == EINTR); \
  __result; })
#endif
```
含义为：expression表达式返回值为-1或者错误码为EINTR，则重复执行expression。-1代表操作失败，errno == EINTR代表中断暂停，
两个情况下都应该不断重试。用于忽略系统中断造成的错误。
示例  android hanlder唤醒
ssize_t nWrite = TEMP_FAILURE_RETRY(write(mWakeEventFd, &inc, sizeof(uint64_t)));


文件打开open函数
#include <fcntl.h>
int fd = open("/dev/mydev",O_RDWR);
第二个模式 O_RDWR读写  O_CREAT不存在则创建
第三个权限
S_IRWXU 00700 权限，代表该文件所有者具有可读、可写及可执行的权限。


获取文件大小
https://stackoverflow.com/questions/238603/how-can-i-get-a-files-size-in-c
```
#include <sys/stat.h>
struct stat st;
stat(filename, &st);  //filename为全路径，获取成功返回0，失败返回-1
size = st.st_size;
```


ioctl函数
ioctl
在计算机中，ioctl(input/output control)是一个专用于设备输入输出操作的系统调用,该调用传入一个跟设备有关的请求码，
系统调用的功能完全取决于请求码。举个例子，CD-ROM驱动程序可以弹出光驱，它就提供了一个对应的Ioctl请求码。
设备无关的请求码则提供了内核调用权限。ioctl这名字第一次出现在Unix第七版中，他在很多类unix系统（比如Linux、Mac OSX等）都有提供，
不过不同系统的请求码对应的设备有所不同。
-- 引用自百科 ioctl
https://baike.baidu.com/item/ioctl/6392403
可见ioctl是一个可以控制设备I/O通道的系统调用，通过它用户空间可以跟设备驱动沟通。
至于为什么要有ioctl，主要是为非标准设备考虑的（如binder就是一种非标准外设），详见百科 ioctl 背景

ioctl函数如下：
int ioctl(int fd, ind cmd, …)；
第一个参数fd是文件描述符，如外设文件；
第二个参数cmd则是控制命令
_IOW是一个宏，Linux内核提供了一些宏来方便用户定义指令（传入各种参数进行包装）：
// nr为序号，datatype 为数据类型,如 int
_IO(type, nr ) //没有参数的命令
_IOR(type, nr, datatype) //从驱动中读数据
_IOW(type, nr, datatype) //写数据到驱动
_IOWR(type,nr, datatype) //双向传送
名字很好理解，就是 io read write的缩写。

linux/uaccess.h 头文件在Linux源码中
copy_from_user 将用户空间的数据拷贝到内核空间，这期间会发生一次内存拷贝
unsigned long __copy_from_user (void * to, const void __user * from, unsigned long n);
to内核空间的指针，from用户空间的指针，n拷贝的大小

linux/uaccess.h
copy_to_user   将内核空间的数据拷贝到用户空间，发生一次内存拷贝
这个方式有两个问题，就是发送一次数据需要内存拷贝两次。第二个就是接收进程不知道发送进程要发送多大的数据，所以只能尽可能地往大
了开辟内存或者事先在读取一次消息头来知晓数据大小，不是浪费空间就是浪费时间


mmap内存映射
映射就是内存映射（mmap），是一种内存映射文件的方法，即将一个文件或者其他对象映射到进程的地址空间，实现文件磁盘地址和应用程序
进程虚拟地址空间中一段虚拟地址的一一映射关系。实现这样的映射关系后，进程就可以采用指针的方式读写操作这一段内存，而系统会自动
回写脏页面到对应的文件磁盘上，即完成了对文件的操作而不必再调用read,write等系统调用函数。相反，内核空间对这段区域的修改也直接反映用户空间，
从而可以实现不同进程间的文件共享。
说得好，不过什么意思？
简单地说就是存在映射关系的双方，只要修改其中一方的内容，另一方也会发生改变
它会在内核虚拟地址空间中申请一块与用户虚拟内存相同大小的内存，然后再申请物理内存，将同一块物理内存分别映射到内核虚拟地址空间和用户虚拟内存空间，
实现了内核虚拟地址空间和用户虚拟内存空间的数据同步操作，程序只要操作用户空间的数据由系统自动同步到文件，省去了copy_from_user,copy_to_user
```//原型
include <sys/mman.h>
/*
addr: 代表映射到进程地址空间的起始地址，当值等于0则由内核选择合适地址，此处为0；
size: 代表需要映射的内存地址空间的大小 必须大于0
prot: 代表内存映射区的读写等属性值，此处为PROT_READ(可读取);
    PROT_EXEC内容可以被执行；
    PROT_READ:内容可以被读取；
    PROT_WRITE:内容可以被写入;
    PROT_NONE:内容不可访问
flags: 标志位，此处为MAP_PRIVATE(私有映射，多进程间不共享内容的改变)和 MAP_NORESERVE(不保留交换空间)
    MAP_SHARED:共享；MAP_PRIVATE:私用；MAP_ANONYMOUS:匿名映射(不基于文件)，fd传入-1
fd: 代表mmap所关联的文件描述符，打开文件的句柄
offset：偏移量，此处为0。 必须是4k的整数倍，一个物理页映射是4k
void* mmap(void* addr, size_t size, int prot, int flags, int fd, off_t offset)
//mVMStart = mmap(0, BINDER_VM_SIZE, PROT_READ, MAP_PRIVATE | MAP_NORESERVE, mDriverFD, 0) 
if(mVMStart == MAP_FAILED){
}
```
https://blog.csdn.net/yangle4695/article/details/52139585
MAP_PRIVATE 对映射区域的写入操作会产生一个映射文件的复制，即私人的“写入时复制”（copy on write）对此区域作的任何修改都不会写回原来的文件内容

解除内存映射
munmap(m_ptr, oldSize);
https://juejin.cn/s/mmap%E5%A4%B1%E8%B4%A5%E5%8E%9F%E5%9B%A0
mmap失败原因：
文件不存在：如果要映射的文件不存在，则mmap调用将失败。
文件大小为0：如果要映射的文件大小为0，则无法映射，并且mmap调用将失败。
没有权限：如果当前进程没有读写文件的权限，则mmap调用将失败。
内存不足：如果系统内存不足，则mmap调用将失败。
系统限制：如果系统限制了映射的地址空间大小，则mmap调用将失败。
其他错误：如果发生其他错误，则mmap调用将失败，例如文件系统故障、磁盘空间不足等



stat函数
函数原型  #include <sys/stat.h>
int stat(const char *restrict pathname, struct stat *restrict buf);
提供文件名字，获取文件对应属性。
int fstat(int filedes, struct stat *buf);
通过文件描述符获取文件对应的属性。
int lstat(const char *restrict pathname, struct stat *restrict buf);
类似于stat.但是当命名的文件是一个符号链接时，lstat返回该符号链接的有关信息，而不是由该符号链接引用文件
函数说明: 通过文件名filename获取文件信息，并保存在buf所指的结构体stat中返回值:
执行成功则返回0，失败返回-1，错误代码存于errno
第二个参数是个指针，它指向一个我们应提供的结构。这些函数填写由buf指向的结构。

struct stat这个结构体是用来描述一个linux系统文件系统中的文件属性的结构
struct stat sb = {};
int result = stat(path, &sb)


fork()创建子进程  返回进程ID

umask
定义函数：mode_t umask(mode_t mask);
函数说明：umask()会将系统umask值设成参数mask&0777后的值, 然后将先前的umask值返回。在使用open()建立新文件时, 该参数mode 并非真正建立文件的权限, 而是
(mode&~umask)的权限值。
例如：
在建立文件时指定文件权限为0666, 通常umask 值默认为022, 则该文件的真正权限则为0666&～022＝0644, 也就是rw-r--r--返回值此调用不会有错误值返回. 返回值为原先系统的umask 值



execve
execve的主要作用为：
1.分配进程新的地址空间，将环境变量、main参数等拷贝到新地址空间的推栈中；
2.解析可执行文件，将代码、数据装入/映射到内存
3.进程新环境的设置，如关闭设置FD_CLOEXEC的文件等
4.设置execve函数返回到用户态时的执行地址；解析器入口地址或程序的入口地址



create_socket
listen


register_epoll_handler epoll使用


memcmp
freecon


__system_property_add
__system_property_update


strncmp


pthread_key_t  线程相关


inotify 监控文件系统操作，比如读取、写入和创建