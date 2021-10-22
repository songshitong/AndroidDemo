Android系统中，有硬件WatchDog用于定时检测关键硬件是否正常工作，类似地，在framework层有一个软件WatchDog用于定期检测关键系统服务是否发生死锁事件。
WatchDog功能主要是分析系统核心服务和重要线程是否处于Blocked状态。
 监视reboot广播；
 监视mMonitors关键系统服务是否死锁

//todo http://gityuan.com/2016/06/21/watchdog/