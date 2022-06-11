
加锁的总结
1 如果目标Object的monitor(指向一个LockWord对象)还没有上锁(状态为KUnlocked)，则设置monitor_为一个瘦锁，其状态为kStateThinLocked，
并且保存拥有该锁的线程的Id。同时，设置上锁次数为0。 使用CAS更新

2 如果目标Object的monitor_的锁状态为kStateThinLocked(暂且不考虑上锁次数超过4096的情况，此时，monitor_依然是一个瘦锁)。
则检查调用线程和拥有该锁的线程是否为同一个。
如果是同一个线程再次上锁，则只需增加上锁次数即可。
如果是其他线程试图获取该锁，则先尽量让新的竞争者(也就是当前的调用线程)让出最多50次CPU资源(sched_yield())。如果50次后依然无法得到该锁，
则需要将瘦锁变成一个胖锁。此时，monitor_的锁状态将变成kKFatLocked。

3 如果目标Object的monitor_的锁状态为kFatLocked，则调用对应Monitor对象的Lock函数进行抢锁。
Lock函数内部会使用比如futex或pthread_mutex_t等来实现抢锁。一旦Lock函数返回，则调用线程就获得了该锁。


volatile
jit模式
读  volatile  Atomic.LoadSequentiallyConsistent-》std::memory_order_seq_cst  顺序一致性，禁止重排
非volatile  Atomic.LoadJavaData ->std::memory_order_relaxed  松散模型，编译器可以重排优化
写  volatile Atomic.StoreSequentiallyConsistent->std::memory_order_seq_cst
非volatile Atomic.StoreJavaData ->std::memory_order_relaxed
Aot模式的处理
读    mov读到寄存器   kLoadAny(LoadLoad | LoadStore)    防止后面的普通读和普通写重排到前面
写    kAnyStore(LoadStore | StoreStore)   mov写入内存(读到内存)  kAnyAny(StoreLoad)
//x86只会发生store-load的重排，所以对volatile的处理，在x86平台只有写完时插入lock的内存屏障   防止store重排到其他load的后面