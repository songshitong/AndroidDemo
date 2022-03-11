数据结构 双向链表实现的队列
https://juejin.cn/post/6975435256111300621#heading-5
https://mp.weixin.qq.com/s/sA01gxC4EbgypCsQt5pVog

在java程序中一般用synchronized关键字来实现线程对共享变量的互斥访问。而从JDK1.5以后java并发大师 Doug Lea 开发了
  AbstractQueuedSynchronizer（下文用AQS代替）组件，使用原生java代码实现了synchronized语义

AQS  http://ifeve.com/introduce-abstractqueuedsynchronizer/
提供了一个基于FIFO队列，可以用于构建锁或者其他相关同步装置的基础框架。该同步器（以下简称同步器）利用了一个int来表示状态，
期望它能够成为实现大部分同步需求的基础。使用的方法是继承，子类通过继承同步器并需要实现它的方法来管理其状态，管理的方式就是通过类似
acquire和release的方式来操纵状态

https://mp.weixin.qq.com/s/z0SVcjdxaoGp_MAEvjSD4g
什么是AQS
并发使计算机得以充分利用计算能力，有效率地完成各类程序任务。当深入地学习Java中的并发，不可避免地将学习到锁 —— 使并发的资源能
被正确访问的手段。锁的学习也将分为两部分，一部分是如何加解锁，另一部分是把锁分配给谁。

AQS(AbstractQueuedSynchronizer)也叫“抽象队列同步器”，它提供了“把锁分配给谁"这一问题的一种解决方案，使得锁的开发人员
可以将精力放在“如何加解锁上”，避免陷于把锁进行分配而带来的种种细节陷阱之中。
AQS与其子类配合，子类负责加锁，解锁，AQS负责将锁分配给预期线程

例如JUC中，如CountDownLatch、Semaphore、ReentrantLock、ReentrantReadWriteLock等并发工具，均是借助AQS完成他们的
  所需要的锁分配问题

AQS基于CAS和Volatile

基于CAS的状态更新
AQS要把锁正确地分配给请求者，就需要其他的属性来维护信息，那么自身也要面对并发问题，因为信息将会被更改，而且可能来源于任意线程。
AQS使用了CAS (compare and set) 协助完成自身要维护的信息的更新(后续的源码处处可见)。CAS的意义为：期望对象为某个值并设置为新的值。那么，
 如果不为期望的值或更新值失败，返回false；如果为期望的值并且设置成功，那么返回true。
CAS是硬件层面上提供的原子操作保证，意味着任意时刻只有一个线程能访问CAS操作的对象。那么，AQS使用CAS的原因在于：
1 CAS足够快
2 如果并发时CAS失败时，可能通过自旋再次尝试，因为AQS知道维护信息的并发操作需要等待的时间非常短
3 AQS对信息的维护不能导致其它线程的阻塞
因此，AQS对于自身所需要的各种信息更新，均使用CAS协助并发正确



构造器
```
public abstract class AbstractQueuedSynchronizer
    extends AbstractOwnableSynchronizer
    implements java.io.Serializable {
    protected AbstractQueuedSynchronizer() { }
    }
```

CLH队列   //todo clh队列的扩展
CLH队列得名于Craig、Landin 和 Hagersten的名字缩写，他们提出实现了以自旋锁方式在并发中构建一个FIFO(先入先出)队列。在AQS中，
也维护着这样一个同步队列，来记录各个线程对锁的申请状态。
每一记录单元，以AQS的内部类Node作为体现：
```
static final class Node {
    // 表明是共享锁节点
    static final Node SHARED = new Node();
    // 表明是独占锁节点
    static final Node EXCLUSIVE = null;

    // 表示线程取消申请锁
    static final int CANCELLED =  1;
    // 表示线程正在申请锁，等待被分配   表示节点处于被唤醒状态，当其前驱结点释放了同步锁或者被取消后就会通知处于SIGNAL状态的后继节点的线程执行
    static final int SIGNAL    = -1;
    // 表示线程在等待某些条件达成，再进入下一阶段  
    // 调用了await方法后处于等待状态的线程节点会被标记为此种状态，当调用了Condition的singal方法后，CONDITION状态会变为SIGNAL状态，
    //  并且会在适当的时机从等待队列转移到同步队列中
    static final int CONDITION = -2;
    // 表示把对当前节点进行的操作，继续往队列传播下去   这种状态与共享模式有关，在共享模式下，表示节点处于可运行状态
    static final int PROPAGATE = -3;
    // 表示当前线程的状态
    volatile int waitStatus;

    // 指向前一个节点，也叫前驱节点
    volatile Node prev;
    // 指向后一个节点，也叫后继节点
    volatile Node next;

    // 节点代表的线程
    volatile Thread thread;

     // 指向下一个代表要等待某些条件达成时，才进行下阶段的线程的节点
    Node nextWaiter;
    Node() {}

    Node(Node nextWaiter) {
        this.nextWaiter = nextWaiter;
        U.putObject(this, THREAD, Thread.currentThread());
    }

    Node(int waitStatus) {
        U.putInt(this, WAITSTATUS, waitStatus);
        U.putObject(this, THREAD, Thread.currentThread());
    }
   //下一个条件是 SHARED
   final boolean isShared() {
            return nextWaiter == SHARED;
        } 
   //前一个节点 
   final Node predecessor() throws NullPointerException {
            Node p = prev;
            if (p == null)
                throw new NullPointerException();
            else
                return p;
        } 
```
propagate  [ˈprɒpəɡeɪt]  传播;宣传;繁殖;增殖
EXCLUSIVE  [ɪkˈskluːsɪv]  独家;(个人或集体)专用的，专有的，独有的，独占的;排外的;不愿接收新成员(尤指较低社会阶层)的;高档的;豪华的;高级的

以Node的结构来看，prev 和 next 属性将可以支持AQS可以将请求锁的线程构成双向队列，而入队列出队列，以及先入先出的特性，需要方法来支持
入队操作
```
 private transient volatile Node head;
private transient volatile Node tail;
private Node enq(final Node node) {
    for (;;) {
        Node t = tail;
        if (t == null) { 
            // 进入到这里，说明没有head节点，CAS操作创建一个head节点
            // 失败也不要紧，失败说明发生了并发，会走到下面的else
            if (compareAndSetHead(new Node()))
                tail = head;
        } else {
            node.prev = t;
            // 把Node加入到尾部，保证加入到为止，并发会重走
            if (compareAndSetTail(t, node)) {
                t.next = node;
                return t;
            }
        }
    }
}
```
AQS中，以head为CLH队列头部，以tail为CLH队列尾部，当加入节点时，通过CAS和自旋保证节点正确入队
有可能连续遇到失败，插入tail并发失败，插入head并发失败
Java_AQS_入队并发失败

上图解释了插入Node时，可能发生的并发情况和解决过程。AQS支持独占锁和共享锁，那么CLH队列也就需要能区分节点类型。无论那种节点，
都能通过 addWaiter() 将节点插入到队列而不是直接调用enq()。
```
private Node addWaiter(Node mode) {
    Node node = new Node(Thread.currentThread(), mode);
    Node pred = tail;
    if (pred != null) {
        node.prev = pred;
        if (compareAndSetTail(pred, node)) {
            // 如果插入尾部成功，就直接返回
            pred.next = node;
            return node;
        }
    }
    // 通过CAS自旋确保入队
    enq(node);
    return node;
}
```

根据前面的内容，Node.waitStatus表示Node处于什么样的状态，意味着状态是可以改变的，那么CLH队列中的节点也是可以取消等待的：
```
 private void cancelAcquire(Node node) {
        if (node == null)
            return;
        node.thread = null;
        Node pred = node.prev;
        // 首先，找到当前节点前面未取消等待的节点
        while (pred.waitStatus > 0)  //>0是CANCELLED取消
            node.prev = pred = pred.prev;

        // 方便操作
        Node predNext = pred.next;
        // 记录当前节点状态为取消，这样，如果发生并发，也能正确地处理掉
        node.waitStatus = Node.CANCELLED;

        //如果当前节点为tail，通过CAS将tail设置为找到的没被取消的pred节点
        if (node == tail && compareAndSetTail(node, pred)) {
            compareAndSetNext(pred, predNext, null);
        } else {
          //非尾节点
            int ws;
            if (pred != head &&
                ((ws = pred.waitStatus) == Node.SIGNAL ||
                 (ws <= 0 && compareAndSetWaitStatus(pred, ws, Node.SIGNAL))) &&
                pred.thread != null) {
                // ① 
                Node next = node.next;
                if (next != null && next.waitStatus <= 0)
                    // 移除掉找到的CANCELLED节点，整理CLH队列
                    compareAndSetNext(pred, predNext, next);
            } else {
                // 表示当pred头节点，唤醒下一节点
                unparkSuccessor(node);
            }
            node.next = node; // help GC
        }
    }
```
对于代码中①处进入的情况为：
1pred不为头节点
2pred记录的线程不为空
3及pred的状态为SIGNAL，即等待分配到锁
4或及pred的状态小于0是，能通过CAS设置为SIGNAL

java_AQS_取消等待.webp
cancelAcquire()将CLH队列整理成了新的状态，完成了并发状态下将已取消等待的节点的移除操作。那么，AQS的CLH队列如何完成FIFO的呢？


恢复与挂起
前面提到，AQS只解决锁分配的问题，锁的加解锁控制就由子类进行控制，为了便于阅读，子类要实现的方法就先一笔带过。
```
 public final void acquire(int arg) {
        // 如果获取到锁，获取锁的成程序就执行下去
        // 如果获取不到锁，插入代表当前线程的Node节点放入队列中，并请求锁
        if (!tryAcquire(arg) &&
            acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
            // 中断
            selfInterrupt();
    }
```
以独占锁请求锁的实现方法acquire()来看，tryAcquire()是子类要实现的控制的锁获取成功与否逻辑。addWaiter()，将新的代表当前线程的独占锁Node
  加入到CLH队列中，然后请求锁。
acquire(int arg)中arg有通过tryAcquire(int arg)传给子类了
```
 final boolean acquireQueued(final Node node, int arg) {
        boolean failed = true;
        try {
            boolean interrupted = false;
            for (;;) {
                // 自旋
                // 读取前驱结点，因为前驱节点可能发生了改变，如取消等待操作
                final Node p = node.predecessor();
                if (p == head && tryAcquire(arg)) {
                    // 只有当前驱节点为head时，才有资格获取锁
                    // 设置head为当前节点
                    setHead(node);
                    p.next = null; // help GC
                    failed = false;
                    // 返回是否发生过中断
                    return interrupted;
                }
                // 更新当前节点状态，并检查线程是否发生过中断
                if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt())
                    interrupted = true;
            }
        } finally {
            if (failed)
            // 说明发生了意料之外的异常，将节点移除，避免影响到其他节点
                cancelAcquire(node);
        }
    }
```
acquireQueued()表达的逻辑为：
1只有当自己的前驱节点为head时，才有资格去获取锁，这表达了FIFO。
2获取锁成功后，会返回线程是否被中断过，结合acquire()看，如果线程被中断过，会让线程回到中断状态。
3以acquireQueued()看，请求锁是的过程是公平的，按照队列排列顺序申请锁。
4以acquire()看，请求锁的过程是不公平的，因为acquire()会先尝试获取锁再入队，意味着将在某一时刻，有线程完成插队。

那么，shouldParkAfterFailedAcquire()是把Node状态更新，parkAndCheckInterrupt则将线程挂起，恢复后返回线程是否被中断过。
```
private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
        int ws = pred.waitStatus;
        if (ws == Node.SIGNAL)
            前驱节点状态为SIGNAL直接返回
            return true;
        if (ws > 0) {
            // 这里和cancelAcquire()类似，整合移除node之前被取消的节点
            do {
                node.prev = pred = pred.prev;
            } while (pred.waitStatus > 0);
            pred.next = node;
        } else {
            // CAS设置前驱节点状态为SIGNAL
            compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
        }
        return false;
    }

    private final boolean parkAndCheckInterrupt() {
        // 挂起当前线程
        LockSupport.park(this);
        return Thread.interrupted();
    }
```

park 公园;专用区;园区;(英国)庄园，庭院   停(车);泊(车);坐下(或站着);把…搁置，推迟(在以后的会议上讨论或处理)
那么，获取锁的过程就清晰了，进入到acquireQueued()的方法，可能预见的情况如下图：

java_AQS_acquireQueued情况分析.webp
情况一：Node的前驱节点为head，那么直接拿到锁，调用acquire()的线程继续执行。
情况二：Node的前驱节点不为head，并且也是申请锁状态，那么在parkAndCheckInterrupt()中此线程将被挂机。等到线程从parkAndCheckInterrupt()中回复后，
   再次中acquireQueued()的自旋逻辑，此时可能发生情况一、情况二、情况三。
情况三：Node的前驱节点被取消了，那么通过shouldParkAfterFailedAcquire()整合CLH队列后，走到情况一。
目前，没有申请到锁的Node在CLH队列中排队，其线程阻塞在parkAndCheckInterrupt()等待唤醒，然后继续尝试获取锁。

那么，在何时恢复线程？
```
private void unparkSuccessor(Node node) {
        int ws = node.waitStatus;
        if (ws < 0)
            // CAS 修改节点状态为0
            compareAndSetWaitStatus(node, ws, 0);

        Node s = node.next;
        if (s == null || s.waitStatus > 0) {
            // 如果s的后继节点为空或者状态大于0
            s = null;
            for (Node t = tail; t != null && t != node; t = t.prev)
                // 从tail开始，找到最靠近head的状态不为0的节点
                if (t.waitStatus <= 0)
                    s = t;
        }
        if (s != null)
            // 唤醒节点中记录的线程
            LockSupport.unpark(s.thread);
    }
```
线程唤醒发生在取消请求时cancelAcquire()，或释放锁时，对unparkSuccessor()的调用。

unparkSuccessor()将从CLH队里中唤醒最靠前的应该被唤醒的Node记录的线程，此之后，线程从parkAndCheckInterrupt()继续执行下去。

这里也以独占锁的释放锁的方法看unparkSuccessor()的调用
```
public final boolean release(int arg) {
        // 子类的实现，尝试解锁
        if (tryRelease(arg)) {
            Node h = head;
            if (h != null && h.waitStatus != 0)
                // 释放锁，唤醒下一线程
                unparkSuccessor(h);
            return true;
        }
        return false;
    }
```

其他特性

上面借助独占锁的 acquire() 和 release()，说明了 AQS 如何通过CLH队列对锁进行分配。此外，AQS还支持了其他的特性。

可中断向AQS请求锁的线程是可以中断的，从parkAndCheckInterrupt()会检查恢复的线程的中断状态，以让更上层的调用决定如何处理。
   以acquire()来看，它会让已中断过的线程回到中断状态。

可重入性控制可以通过isHeldExclusively()设置可重入性控制，在AQS中是为了共享锁服务的。当然，也可以在子类tryAcquire()等加锁的方法中，
  借助setExclusiveOwnerThread()和getExclusiveOwnerThread()一起实现是否可重入。
```
protected boolean isHeldExclusively() {
        throw new UnsupportedOperationException();
    }
 AQS的父类
 public abstract class AbstractOwnableSynchronizer
    implements java.io.Serializable{
    //独占模式的线程
    private transient Thread exclusiveOwnerThread;
    protected final void setExclusiveOwnerThread(Thread thread) {
        exclusiveOwnerThread = thread;
    }
    protected final Thread getExclusiveOwnerThread() {
        return exclusiveOwnerThread;
    }
 }   
```

可控获取锁时间申请锁的时间，也可以控制，实现只需要通过在申请不到锁入队时，设置线程唤醒时间即可。AQS提供了其他版本的申请锁方法，流程大体一致。

并发量控制AQS通过属性 state 来提供控制并发量的方式，state只能通过原子性的操作修改。子类控制加解锁操作时，可以通过控制state来做出判断。
```
public abstract class AbstractQueuedSynchronizer {
    //同步状态
    private volatile int state;
    protected final int getState() {
        return state;
    }
    protected final void setState(int newState) {
        state = newState;
    }
    protected final boolean compareAndSetState(int expect, int update) {
      return U.compareAndSwapInt(this, STATE, expect, update);
    }
}
```


独占锁如何实现

在前文中，借用独占锁的例子 acquire() 和 release() 说明了 AQS是如何运作的。这里主要为其他补充。
可中断、可控获取锁时间这样的特性，提供了不同的入口方法，也实现了不同版本的acquireQueued()，其仅有少处不同。下面以中断的方式获取锁为例子抛砖引玉
```
private void doAcquireInterruptibly(int arg)
        throws InterruptedException {
        ......
                if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt())
                    // 与acquireQueued()主要不同，向上抛出了异常
                    throw new InterruptedException();
         ......
    }
```
中断方式获取锁关联方法为：
acquireInterruptibly()
```
public final void acquireInterruptibly(int arg)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        if (!tryAcquire(arg))
            doAcquireInterruptibly(arg);
    }
```
doAcquireInterruptibly()
```
private void doAcquireInterruptibly(int arg)
        throws InterruptedException {
        final Node node = addWaiter(Node.EXCLUSIVE);
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null; // help GC
                    return;
                }
                if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt())
                    throw new InterruptedException();
            }
        } catch (Throwable t) {
            cancelAcquire(node);
            throw t;
        }
    }
```

可控获取锁时间关联方法为：
doAcquireNanos()   以独占定时模式获取
```
 private boolean doAcquireNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (nanosTimeout <= 0L)
            return false;
        final long deadline = System.nanoTime() + nanosTimeout;
        final Node node = addWaiter(Node.EXCLUSIVE);
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null; // help GC
                    return true;
                }
                nanosTimeout = deadline - System.nanoTime();
                if (nanosTimeout <= 0L) {
                    cancelAcquire(node);
                    return false;
                }
                if (shouldParkAfterFailedAcquire(p, node) &&
                    nanosTimeout > SPIN_FOR_TIMEOUT_THRESHOLD)
                    LockSupport.parkNanos(this, nanosTimeout);
                if (Thread.interrupted())
                    throw new InterruptedException();
            }
        } catch (Throwable t) {
            cancelAcquire(node);
            throw t;
        }
    }
```
doAcquireSharedNanos()


共享锁如何实现   
与独占锁的实现相比，共享锁的实现更复杂一些。从申请锁看。
```
private void doAcquireShared(int arg) {
        final Node node = addWaiter(Node.SHARED);
        try {
            boolean interrupted = false;
            for (;;) {
                final Node p = node.predecessor();
                 // 与独占锁相比差异为这一段
                if (p == head) {
                // 尝试获取锁，r表示资源情况
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
                     //  获取到了锁，重新设置head，并传播
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        if (interrupted)
                            selfInterrupt();
                        return;
                    }
                }
                if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt())
                    interrupted = true;
            }
        } catch (Throwable t) {
            cancelAcquire(node);
            throw t;
        }
    }

    private void setHeadAndPropagate(Node node, int propagate) {
        Node h = head; 
        // 重新设置head
        setHead(node);

        if (propagate > 0 || h == null || h.waitStatus < 0 ||
            (h = head) == null || h.waitStatus < 0) {
            // ①
            Node s = node.next;
            if (s == null || s.isShared())
                // 唤醒其他的Node
                doReleaseShared();
        }
    }
```
在共享锁的情况下，申请锁成功后，还需要考虑到有更多的资源能支持更多的并发，那么，可以唤醒Node。
进入①处可能为一下任意情况：
有更多的资源，即 propagate > 0
旧的head为空或未被取消
新的head为空或未被取消

判断新旧head来调用doReleaseShared()的原因在于，如果旧的head已经被释放，不去检查新的head的状态，就有可能少唤醒一个Node。
```
 private void doReleaseShared() {
        for (;;) {
            Node h = head;
            if (h != null && h != tail) {
                int ws = h.waitStatus;
                if (ws == Node.SIGNAL) {
                    // 设置头为 0
                    if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0))
                        continue;
                    // 唤醒下一节点
                    unparkSuccessor(h);
                }
                else if (ws == 0 &&
                    // ②
                         !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))
                    continue;                
            }
            if (h == head)                   
                break;
        }
    }
```
②，如图：
java_AQS_共享锁唤醒下一个节点.webp
unparkSuccessor()需要唤醒一个状态小于0的节点，设想某一时刻，A、B在申请锁，C释放了锁，A拿到了锁，head状态被设置为0。
时间片分配给了D，D释放锁，但是发现head状态为0，因此不进行唤醒。A获得了时间片，继续调用setHeadAndPropagate()，
传入的propagate值为0，也不进行唤醒。而我们期望的结果是，B被唤醒
如果不进行处理，那么随着程序运行，将不断地减少并发量。那么，②处将头节点状态设置为PROPAGATE就避免了这个问题。在上面的例子中，
  A进入到setHeadAndPropagate()后将唤醒B。因此，PROPAGATE就表示了将某个行为传播下去。
与独占锁类似，其他的特性也提供了对应的入口，这里就不放出源码： 可中断方式获取锁的方法为：
acquireSharedInterruptibly()
doAcquireSharedInterruptibly()
可控获取锁时间关联方法为：
tryAcquireSharedNanos()
doAcquireSharedNanos()

条件处理
与独占锁不同的是，共享锁需要支持条件，即有时候，需要达到一些条件后，线程才应继续运行下去。Condition就表达了这一协作关系，它提供了模板方法，其中：
await()系列：表示等待条件的完成
signal()、signalAll()：表示条件达成的信号
AQS以ConditionObject实现了Condition的语义
```
public class ConditionObject implements Condition, java.io.Serializable {
        private transient Node firstWaiter;
        private transient Node lastWaiter;
    }

    static final class Node {
        // 表示下一个CONDITION状态的节点
        Node nextWaiter;
    }
```
ConditionObject维护了一个单向队列，用来记录等待Condition达成的节点。
```
private Node addConditionWaiter() {
            Node t = lastWaiter;
            if (t != null && t.waitStatus != Node.CONDITION) {
                // 如果尾部节点已经不为CONDITION，那么把这些节点移除
                unlinkCancelledWaiters();
                // 重新指向尾部节点
                t = lastWaiter;
            }
            Node node = new Node(Thread.currentThread(), Node.CONDITION);
            if (t == null)
                // 作为头节点
                firstWaiter = node;
            else
                // 作为下一节点
                t.nextWaiter = node;
            // 更新尾部节点
            lastWaiter = node;
            return node;
        }
```
```
 private void unlinkCancelledWaiters() {
            Node t = firstWaiter;
            Node trail = null;
            while (t != null) {
                // 从头结点开始，移除所有不为Node.CONDITION的节点
                Node next = t.nextWaiter;
                if (t.waitStatus != Node.CONDITION) {
                    t.nextWaiter = null;
                    if (trail == null)
                        firstWaiter = next;
                    else
                        trail.nextWaiter = next;
                    if (next == null)
                        lastWaiter = trail;
                }
                else
                    trail = t;
                t = next;
            }
        }
```
CONDITION节点的插入的操作较简单，移除操作如图：
java_AQS_共享锁_condition移除节点.webp

任意时刻，如果条件达成，则 signal() -> doSignal()。
```
private void doSignal(Node first) {
            do {
                if ( (firstWaiter = first.nextWaiter) == null)
                    lastWaiter = null;
                // 移除first节点的下一记录
                first.nextWaiter = null;
            } while (!transferForSignal(first)/*加入CLH队列*/ &&
                     (first = firstWaiter) != null);
        }

        final boolean transferForSignal(Node node) {
        // 更新 node 的状态
        if (!compareAndSetWaitStatus(node, Node.CONDITION, 0))
            return false;
        // 将节点加入CLH队列
        Node p = enq(node);
        int ws = p.waitStatus;
        if (ws > 0 || !compareAndSetWaitStatus(p, ws, Node.SIGNAL))
            // 如果取消等待或者不能设置为SIGNAL，唤起线程
            LockSupport.unpark(node.thread);
        return true;
        }
```
当Conditon条件达成时，将把节点从ConditionObject维护的队列移动到CLH队列，这样，当有资源时，才可被正确唤醒。 挂起处位于：
```
 public final void await() throws InterruptedException {

            if (Thread.interrupted())
                // 如果线程中断了，抛出异常
                throw new InterruptedException();
            // 加入到CONDITION队列中
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            // 记录中断的场景
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                // 自旋
                // 如果没有被加入到CLH队列中，那么挂起线程
                LockSupport.park(this);
                // 更新中断场景
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
            }
            // 尝试获取锁，此时Node已经在CLH队列中了
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null) // clean up if cancelled
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                // 根据中断场景做不同的处理
                reportInterruptAfterWait(interruptMode);
        }
```
因此，当Condition达成时，被唤醒的线程将从 while (!isOnSyncQueue(node)){...} 自旋中继续执行，粗略地来看，其过程为：
java_AQS_共享锁_挂起与唤醒.webp



如何使用
AQS解决了锁的分配过程，加解锁的过程就需要子类自行实现。子类可以根据需要，提供独占锁或共享锁的实现。
tryAcquire(int)：获取独占锁
tryRelease(int)：释放独占锁
tryAcquireShared(int)：获取共享锁
tryReleaseShared(int)：释放共享锁

子类要实现的方法中，都带有int参数，一般而言，此int参数用于辅助控制AQS的state属性，也就是说，可以通过保证更改state的状态为原子性操作，
即可保证并发状态。AQS也提供了compareAndSetState()的CAS操作对state进行更改。
利用AQS自己实现一个同步工具
```
public class LeeLock  {

    private static class Sync extends AbstractQueuedSynchronizer {
        @Override
        protected boolean tryAcquire (int arg) {
            return compareAndSetState(0, 1);
        }

        @Override
        protected boolean tryRelease (int arg) {
            setState(0);
            return true;
        }

        @Override
        protected boolean isHeldExclusively () {
            return getState() == 1;
        }
    }

    private Sync sync = new Sync();

    public void lock () {
        sync.acquire(1);
    }

    public void unlock () {
        sync.release(1);
    }
}
```
测试类
```
public class LeeMain {

    static int count = 0;
    static LeeLock leeLock = new LeeLock();

    public static void main (String[] args) throws InterruptedException {

        Runnable runnable = new Runnable() {
            @Override
            public void run () {
                try {
                    leeLock.lock();
                    for (int i = 0; i < 10000; i++) {
                        count++;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    leeLock.unlock();
                }

            }
        };
        Thread thread1 = new Thread(runnable);
        Thread thread2 = new Thread(runnable);
        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();
        System.out.println(count);
    }
}
```
上述代码每次运行结果都会是20000。通过简单的几行代码就能实现同步功能，这就是AQS的强大之处。

总结：
AQS是解决并发过程中锁分配的问题，使锁的实现者可以聚焦于加解锁的实现上。AQS的实现概要为：
维护一个CLH队列里，记录每一个需要获取锁的线程；在首次请求锁时，是不公平的；在队列里的锁请求时，是公平的。
当Node锁代表的线程没有请求到锁时，将被挂起，等被唤醒后，尝试再次请求锁，如果还是没有获取到锁，重复此过程。
当一个Node入队时，将从队尾移除取消等待的节点，直到找到第一个未取消等待的节点，插入此节点后。
当释放锁时，从CLH队里头部开始，找到第一个未取消等待的节点，唤醒。
对于共享锁，如果需要等待条件，则Node进入一个单项队列，自旋，挂起；待条件达成后，将Node加入到CLH队里，请求锁；若请求到锁，继续执行线程。

此外，AQS的还支持的特性为：
通过CAS和自旋控制自身状态并发，足够快
支持重入性判断，通过控制isHeldExclusively()，其代码位于操作CONDITION节点的各处，较零碎，因此没有将代码放出。可在tryAcquire()等子类的加锁方法中，
    借助setExclusiveOwnerThread()和getExclusiveOwnerThread()一起实现是否可重入
支持中断。
支持锁的获取时间控制。


查询是否有线程等待获取的时间长于当前线程
```
public final boolean hasQueuedPredecessors() {
        Node t = tail; // Read fields in reverse initialization order
        Node h = head;
        Node s;
        return h != t &&
            ((s = h.next) == null || s.thread != Thread.currentThread());
    }
```