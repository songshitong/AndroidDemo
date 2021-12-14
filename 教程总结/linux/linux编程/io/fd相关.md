一切皆文件，但 fd 区分类型？
Linux 一切皆文件，但这个文件 fd 也是有类型的，绝大部分人都知道“文件 fd”，知道 socket fd，甚至知道 pipe fd，可
能都不知道 fd 还有这么一种叫做 eventfd 的类型,timerFd,signalFd

eventFd
eventfd是linux 2.6.22后系统提供的一个轻量级的进程间通信的系统调用，eventfd通过一个进程间共享的64位计数器完成进程间通信，
这个计数器由在linux内核空间维护，用户可以通过调用write方法向内核空间写入一个64位的值，也可以调用read方法读取这个值
#include <sys/eventfd.h>
int eventfd(unsigned int initval, int flags);
复制代码创建的时候可以传入一个计数器的初始值initval。
第二个参数flags在linux 2.6.26之前的版本是没有使用的，必须初始化为0，在2.6.27之后的版本flag才被使用
EFD_CLOEXEC(2.6.27～):    close-on-exec=cloexec
   eventfd()会返回一个文件描述符，如果该进程被fork的时候，这个文件描述符也会复制过去，这时候就会有多个的文件描述符指向同一个eventfd对象，
   如果设置了EFD_CLOEXEC标志，在子进程执行exec的时候，会清除掉父进程的文件描述符
EFD_NONBLOCK(2.6.27～): 就如它字面上的意思，如果没有设置了这个标志位，那read操作将会阻塞直到计数器中有值。如果没有设置这个标志位，
   计数器没有值的时候也会立即返回-1；
EFD_SEMAPHORE(2.6.30～): 这个标志位会影响read操作，具体可以看read方法中的解释
提供的方法
read: 读取计数器中的值
如果计数器中的值大于0
   设置了EFD_SEMAPHORE标志位，则返回1，且计数器中的值也减去1。
   没有设置EFD_SEMAPHORE标志位，则返回计数器中的值，且计数器置0。
如果计数器中的值为0
   设置了EFD_NONBLOCK标志位就直接返回-1。 
   没有设置EFD_NONBLOCK标志位就会一直阻塞直到计数器中的值大于0。
write: 向计数器中写入值
  如果写入值的和小于0xFFFFFFFFFFFFFFFE，则写入成功 
  如果写入值的和大于0xFFFFFFFFFFFFFFFE
    设置了EFD_NONBLOCK标志位就直接返回-1。 
    如果没有设置EFD_NONBLOCK标志位，则会一直阻塞知道read操作执行
close: 关闭文件描述符
```
#include <sys/eventfd.h>
#include <unistd.h>
#include <iostream>
int main() {
    //创建一个eventFd写入2 然后读取
    int efd = eventfd(0, EFD_NONBLOCK | EFD_CLOEXEC);
    eventfd_write(efd, 2);
    eventfd_t count;
    eventfd_read(efd, &count);
    std::cout << count << std::endl;
    close(efd);
}
```
eventfd的应用场景
在进行IO多路复用的时候，比如select、poll、epoll等，线程会阻塞在这些监听函数上面。有时候，我们需要在没有监听事件到来时，
将线程从阻塞的监听函数中唤醒。
常用的唤醒方法是：建立一个管道，将管道的一端置于监听函数上，当我们想要唤醒线程时，像管道的另一端写入数据。
eventfd通信调用为上述过程提供了更加方便的实现形式 。只能在父子进程中做简单的消息通知，性能上比pipe好一些。