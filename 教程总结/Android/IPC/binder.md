https://juejin.cn/post/6897868762410811405
https://blog.csdn.net/universus/article/details/6211589
http://liuwangshu.cn/framework/binder/1-intro.html
binder使用


binder原理
为什么有binder



binder驱动相关      Binder跟键盘、显示器一样属于一种外设（没有实体的外设） linux在/dev下标记外设
binder驱动文件
通过adb shell进入Android设备，看下他的/dev目录长啥样
```lmi:/dev $ ls | grep "binder"
binder -> /dev/binderfs/binder
binderfs
hwbinder
vndbinder
```
可以看到有binder，标黄部分的3个分别是binder、hwbinder、vndbinder     binder是个链接


ioctl   int ioctl(int fd, ind cmd, …)；
Android自定义了自己的ioctl驱动
ProcessState.cpp中
status_t result = ioctl(fd, BINDER_VERSION, &vers);
result = ioctl(fd, BINDER_SET_MAX_THREADS, &maxThreads);

第二个参数cmd则是控制命令，如指令BINDER_SET_MAX_THREADS是“设置线程数”，最后的省略号则是各指令所需的参数，
如maxThreads表示最大线程数为 15。
指令BINDER_SET_MAX_THREADS的定义如下：   
#define BINDER_SET_MAX_THREADS _IOW('b', 5, __u32)








