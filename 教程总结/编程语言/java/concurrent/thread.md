

https://www.cnblogs.com/yhlx/articles/2168188.html
为什么 Thread.suspend 和 Thread.resume 被废弃了？
Thread.suspend 天生容易引起死锁。如果目标线程挂起时在保护系统关键资源的监视器上持有锁，那么其他线程在目标线程恢复之前都无法访问这个资源。
如果要恢复目标线程的线程在调用 resume 之前试图锁定这个监视器，死锁就发生了。这种死锁一般自身表现为“冻结（ frozen ）”进程。

关于 Thread.destroy 如何呢？
Thread.destroy 从未被实现。如果它被实现了，它将和 Thread.suspend 一样易于死锁（事实上，它大致上等同于没有后续 Thread.resume 的 Thread.suspend ）。
我们现在既没有实现，也没有废除它（防止将来它被实现）。虽然它确实易于发生死锁，有人争论过，在有些情况下程序可能愿意冒死锁的险而不是直接退出。