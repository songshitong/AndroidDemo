https://segmentfault.com/a/1190000008420938     jdk1.8

总结
LockSupport为每个线程关联一个permit，默认为0,调用unpark时permit变为1,调用park时permit由1变为0,再次调用park时线程阻塞
阻塞时通过unSafe使用pthread_cond_wait进行阻塞

场景变化
1.第一次调用LockSupport.park，permit为0，线程1阻塞
2.其他线程调用LockSupport.unpark(thread)，permit为1，同时唤醒阻塞线程1
3. 线程1发现条件不满足，调用LockSupport.park，permit由1变为0，返回后继续运行
4. 线程1发现条件满足了执行后续，发现条件仍然不满足，调用LockSupport.park，线程阻塞等待
所以park的使用范式
```
while (!canProceed()) { ... LockSupport.park(this); }
```   


park 公园;专用区;园区;(英国)庄园，庭院   停(车);泊(车);坐下(或站着);把…搁置，推迟(在以后的会议上讨论或处理)

使用
```
//阻塞线程
public static void park(Object blocker)
//解除阻塞
public static void unpark(Thread thread)
```

LockSupport 和 CAS 是Java并发包中很多并发工具控制机制的基础，它们底层其实都是依赖Unsafe实现。
LockSupport是用来创建锁和其他同步类的基本线程阻塞原语。LockSupport 提供park()和unpark()方法实现阻塞线程和解除线程阻塞，
LockSupport和每个使用它的线程都与一个许可(permit)关联。permit相当于1，0的开关，默认是0，调用一次unpark就加1变成1，
调用一次park会消费permit, 也就是将1变成0，同时park立即返回。再次调用park会变成block（因为permit为0了，会阻塞在这里，直到permit变为1）, 
这时调用unpark会把permit置为1。每个线程都有一个相关的permit, permit最多只有一个，重复调用unpark也不会积累。
//permit体现为下文的_counter

park()和unpark()不会有 “Thread.suspend和Thread.resume所可能引发的死锁” 问题，由于许可的存在，调用 park 的线程和
另一个试图将其 unpark 的线程之间的竞争将保持活性。
//Thread.suspend和Thread.resume可能引发的死锁，已经被废弃了

如果调用线程被中断，则park方法会返回。同时park也拥有可以设置超时时间的版本。

需要特别注意的一点：park 方法还可以在其他任何时间“毫无理由”地返回，因此通常必须在重新检查返回条件的循环里调用此方法。从这个意义上说，
park 是“忙碌等待”的一种优化，它不会浪费这么多的时间进行自旋，但是必须将它与 unpark 配对使用才更高效。

使用案例
利用park实现一个先进先出非重入锁类的框架
```
class FIFOMutex {
    private final AtomicBoolean locked = new AtomicBoolean(false);
    private final Queue<Thread> waiters
      = new ConcurrentLinkedQueue<Thread>();
 
    public void lock() {
      boolean wasInterrupted = false;
      Thread current = Thread.currentThread();
      waiters.add(current);
 
      // Block while not first in queue or cannot acquire lock
      while (waiters.peek() != current ||
             !locked.compareAndSet(false, true)) {
        LockSupport.park(this);
        if (Thread.interrupted()) // ignore interrupts while waiting
          wasInterrupted = true;
      }

      waiters.remove();
      if (wasInterrupted)          // reassert interrupt status on exit
        current.interrupt();
    }
 
    public void unlock() {
      locked.set(false);
      LockSupport.unpark(waiters.peek());
    }
  }}
```

原理：
park方法
通过UNSAFE.park实现阻塞效果
```
    public static void park(Object blocker) {
        Thread t = Thread.currentThread();
        setBlocker(t, blocker);
        UNSAFE.park(false, 0L);
        setBlocker(t, null);
    }
```
setBlocker
通过UNSAFE将object设置给Thread的parkBlocker字段，用于记录此线程被谁阻塞
```
 private static void setBlocker(Thread t, Object arg) {
        // Even though volatile, hotspot doesn't need a write barrier here.
        UNSAFE.putObject(t, parkBlockerOffset, arg);
    }
```
blocker 对象参数。此对象在线程受阻塞时被记录，以允许监视工具和诊断工具确定线程受阻塞的原因。
  （这样的工具可以使用方法 getBlocker(java.lang.Thread) 访问 blocker。）
建议最好使用这些形式，而不是不带此参数的原始形式。在锁实现中提供的作为 blocker 的普通参数是 this。
有blocker的可以传递给开发人员更多的现场信息，可以查看到当前线程的阻塞对象，方便定位问题。

parkBlockerOffset
parkBlocker就是用于记录线程被谁阻塞的，用于线程监控和分析工具来定位原因的，可以通过LockSupport的getBlocker获取到阻塞的对象
```
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> tk = Thread.class;
            //获取线程parkBlocker字段的偏移量
            parkBlockerOffset = UNSAFE.objectFieldOffset
                (tk.getDeclaredField("parkBlocker"));
        } catch (Exception ex) { throw new Error(ex); }
    }
```
Thread.class
```
volatile Object parkBlocker;
```
为什么要用偏移量来获取对象？干吗不要直接写个get，set方法。多简单？
仔细想想就能明白，这个parkBlocker就是在线程处于阻塞的情况下才会被赋值。线程都已经阻塞了，如果不通过这种内存的方法，
  而是直接调用线程内的方法，线程是不会回应调用的


unPark方法
通过UNSAFE.unpark解除线程阻塞
```
  public static void unpark(Thread thread) {
        if (thread != null)
            UNSAFE.unpark(thread);
    }
```


unsafe相关  https://zhuanlan.zhihu.com/p/425621127
http://hg.openjdk.java.net/jdk8/jdk8/hotspot/file/tip/src/share/vm/prims/unsafe.cpp
```
UNSAFE_ENTRY(void, Unsafe_Park(JNIEnv *env, jobject unsafe, jboolean isAbsolute, jlong time)) {
  //省略部分代码
  thread->parker()->park(isAbsolute != 0, time);
  //省略部分代码
} UNSAFE_END

UNSAFE_ENTRY(void, Unsafe_Unpark(JNIEnv *env, jobject unsafe, jobject jthread)) {
  Parker* p = NULL;
  //省略部分代码
  if (p != NULL) {
    HOTSPOT_THREAD_UNPARK((uintptr_t) p);
    p->unpark();
  }
} UNSAFE_END
```
线程的阻塞和唤醒其实是与hotspot.share.runtime中的Parker类相关
https://hg.openjdk.java.net/jdk8/jdk8/hotspot/file/87ee5ee27509/src/share/vm/runtime/park.hpp
```
class Parker : public os::PlatformParker {
private:
  volatile int _counter ;//该变量非常重要，下文我们会具体描述
     //省略部分代码
protected:
  ~Parker() { ShouldNotReachHere(); }
public:
  // For simplicity of interface with Java, all forms of park (indefinite,
  // relative, and absolute) are multiplexed into one call.
  void park(bool isAbsolute, jlong time);
  void unpark();
  //省略部分代码

}
```
在上述代码中，volatile int _counter该字段的值非常重要，一定要注意其用volatile修饰
//用来表示Parker的状态，park方法执行完成为0，unpark方法执行完成为1

针对线程的阻塞和唤醒，不同操作系统有着不同的实现
这里选择对Linux下的平台下进行分析。也就是选择hotspot.os.linux包下的os_linux.cpp文件进行分析
Linux下的park实现
https://hg.openjdk.java.net/jdk8/jdk8/hotspot/file/87ee5ee27509/src/os/linux/vm/os_linux.cpp
```
void Parker::park(bool isAbsolute, jlong time) {

  //(1)如果_counter的值大于0，那么直接返回  再次调用才会阻塞线程
  if (Atomic::xchg(0, &_counter) > 0) return;

  //获取当前线程
  Thread* thread = Thread::current();
  JavaThread *jt = (JavaThread *)thread;

  //(2)如果当前线程已经中断，直接返回。
  if (Thread::is_interrupted(thread, false)) {
    return;
  }

  //(3)判断时间，如果时间小于0，或者在绝对时间情况下，时间为0直接返回
  struct timespec absTime;
  if (time < 0 || (isAbsolute && time == 0)) { // don't wait at all
    return;
  }
  //如果时间大于0，判断阻塞超时时间或阻塞截止日期，同时将时间赋值给absTime
  if (time > 0) {
    to_abstime(&absTime, time, isAbsolute);
  }
  //(4)如果当前线程已经中断，或者申请互斥锁失败，则直接返回
  if (Thread::is_interrupted(thread, false) ||
      pthread_mutex_trylock(_mutex) != 0) {
    return;
  }

  //(5)如果是时间等于0,那么就直接阻塞线程，
  if (time == 0) {
    _cur_index = REL_INDEX; // arbitrary choice when not timed
    status = pthread_cond_wait(&_cond[_cur_index], _mutex);
  }
  //(6)根据absTime之前计算的时间，阻塞线程相应时间
  else {
    _cur_index = isAbsolute ? ABS_INDEX : REL_INDEX;
    status = pthread_cond_timedwait(&_cond[_cur_index], _mutex, &absTime);
  }

  //省略部分代码
  //(7)当线程阻塞超时，或者到达截止日期时，直接唤醒线程  _counter计数归0
  _counter = 0;
  status = pthread_mutex_unlock(_mutex);

 //省略部分代码
}
```
Linux下的park方法分为以下七个步骤：
（1）调用Atomic::xchg方法，将_counter的值赋值为0，其方法的返回值为之前_counter的值，如果返回值大于0
   （因为有其他线程操作过_counter的值，也就是其他线程调用过unPark方法)，那么就直接返回。
（2）如果当前线程已经中断，直接返回。也就是说如果当前线程已经中断了，那么调用park()方法来阻塞线程就会无效。
（3） 判断其设置的时间是否合理，如果合理，判断阻塞超时时间或阻塞截止日期，同时将时间赋值给absTime
（4） 在实际对线程进行阻塞前，再一次判断如果当前线程已经中断，或者申请互斥锁失败，则直接返回
（5） 如果是时间等于0（时间为0，表示一直阻塞线程，除非调用unPark方法唤醒），那么就直接阻塞线程，
（6）根据absTime之前计算的时间，并调用pthread_cond_timedwait方法阻塞线程相应的时间。
（7） 当线程阻塞相应时间后，通过pthread_mutex_unlock方法直接唤醒线程,同时将_counter赋值为0。

Linux下的unpark实现
```
void Parker::unpark() {
  int status = pthread_mutex_lock(_mutex);
  assert_status(status == 0, status, "invariant");
  const int s = _counter;
  //将_counter的值赋值为1
  _counter = 1;
  // must capture correct index before unlocking
  int index = _cur_index;
  status = pthread_mutex_unlock(_mutex);
  assert_status(status == 0, status, "invariant");
  //省略部分代码
}
```
最终唤醒其线程的方法为pthread_mutex_unlock(_mutex)同时将_counter的值赋值为1, 那么结合我们上文所讲的park(将线程进行阻塞)方法，
那么我们可以得知整个线程的唤醒与阻塞，在Linux系统下，其实是受到Parker类中的_counter的值的影响的。