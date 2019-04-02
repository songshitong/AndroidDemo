//什么是binder
//Android ipc机制
//linux的驱动  /dev/binder
//android系统中的Binder类，实现了IBinder接口

//为什么要增加binder机制

// 常见的通信方式有  socket/消息队列/管道/共享内存

//原因 主要是基于性能，安全性，稳定性几方面考虑

// 为什么Java多进程内存不共享      安全性，共享内存可通过反射直接获取，Linux内核内存不能获取则通过binder机制