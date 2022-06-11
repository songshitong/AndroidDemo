https://mp.weixin.qq.com/s?__biz=MzkxNDEyOTI0OQ==&mid=2247484425&idx=1&sn=9325222b97f0125160ab4098c686c586&source=41#wechat_redirect

总结
（1）自己动手写一个锁需要做准备：一个变量、一个队列、Unsafe类。

（2）原子更新变量为1说明获得锁成功；

（3）原子更新变量为1失败说明获得锁失败，进入队列排队；

（4）更新队列尾节点的时候是多线程竞争的，所以要使用原子更新；

（5）更新队列头节点的时候只有一个线程，不存在竞争，所以不需要使用原子更新；

（6）队列节点中的前一个节点prev的使用很巧妙，没有它将很难实现一个锁，只有写过的人才明白，不信你试试^^


锁的概念
互斥锁
互斥：同一时刻只有一个线程执行
一段需要互斥执行的代码称为临界区。线程在进入临界区之前，首先尝试加锁 lock()，如果成功，则进入临界区，此时我们称这个线程持有锁；
否则呢就等待，直到持有锁的线程解锁；持有锁的线程执行完临界区的代码后，执行解锁 unlock()


问题
（1）自己动手写一个锁需要哪些知识？

（2）自己动手写一个锁到底有多简单？

（3）自己能不能写出来一个完美的锁？

简介
本篇文章的目标一是自己动手写一个锁，这个锁的功能很简单，能进行正常的加锁、解锁操作。

本篇文章的目标二是通过自己动手写一个锁，能更好地理解后面章节将要学习的AQS及各种同步器实现的原理。

分析
自己动手写一个锁需要准备些什么呢？

首先，在上一章学习synchronized的时候我们说过它的实现原理是更改对象头中的MarkWord，标记为已加锁或未加锁。

但是，我们自己是无法修改对象头信息的，那么我们可不可以用一个变量来代替呢？

比如，这个变量的值为1的时候就说明已加锁，变量值为0的时候就说明未加锁，我觉得可行。

其次，我们要保证多个线程对上面我们定义的变量的争用是可控的，所谓可控即同时只能有一个线程把它的值修改为1，
   且当它的值为1的时候其它线程不能再修改它的值，这种是不是就是典型的CAS操作，所以我们需要使用Unsafe这个类来做CAS操作。

然后，我们知道在多线程的环境下，多个线程对同一个锁的争用肯定只有一个能成功，那么，其它的线程就要排队，所以我们还需要一个队列。

最后，这些线程排队的时候干嘛呢？它们不能再继续执行自己的程序，那就只能阻塞了，阻塞完了当轮到这个线程的时候还要唤醒，
  所以我们还需要Unsafe这个类来阻塞（park）和唤醒（unpark）线程。

基于以上四点，我们需要的神器大致有：一个变量、一个队列、执行CAS/park/unpark的Unsafe类。

大概的流程图如下图所示：
java_自己实现一个锁1.jpeg


解决
一个变量
这个变量只支持同时只有一个线程能把它修改为1，所以它修改完了一定要让其它线程可见，因此，这个变量需要使用volatile来修饰。
private volatile int state;
//当前是否被线程占用了0没有；1有，此时使用cas更新失败

CAS
这个变量的修改必须是原子操作，所以我们需要CAS更新它，我们这里使用Unsafe来直接CAS更新int类型的state。

当然，这个变量如果直接使用AtomicInteger也是可以的，不过，既然我们学习了更底层的Unsafe类那就应该用（浪）起来。


一个队列
队列的实现有很多，数组、链表都可以，我们这里采用链表，毕竟链表实现队列相对简单一些，不用考虑扩容等问题。

这个队列的操作很有特点：

放元素的时候都是放到尾部，且可能是多个线程一起放，所以对尾部的操作要CAS更新；

唤醒一个元素的时候从头部开始，但同时只有一个线程在操作，即获得了锁的那个线程，所以对头部的操作不需要CAS去更新。
```
private static class Node{
  Thread thread;//存储线程
  Node prev;//前一个节点(可以没有，但实现起来很困难)
  Node next;//后一个节点
  public Node(){}
  public Node(Thread thread,Node prev){
    this.thread = thread;
    this.prev = prev;
  }
}
private volatile Node head;//链表头
private volatile Node tail;//链表尾

private boolean compareAndSetTail(Node expect,Node update){
 return unsafe.compareAndSwapObject(this,tailOffset,expect,update);
}
```
这个队列很简单，存储的元素是线程，需要有指向下一个待唤醒的节点，前一个节点可有可无，但是没有实现起来很困难，
不信学完这篇文章你试试

加锁
```
public void lock(){
        // 尝试更新state字段，更新成功说明占有了锁  当前没有其他线程抢占锁
        if(compareAndSetState(0,1)){
            return;
        }
        //未更新成功则入队  记录要求加锁的线程
        Node node = enqueue();
        Node prev = node.prev;
        //再次尝试获取锁，需要检测上一个节点是不是head，按入队顺序加锁
        while (node.prev != head || !compareAndSetState(0,1)){
            //未获取到锁，阻塞
            unsafe.park(false,0L);
        }
        //获取到锁了，且上一个节点是head   条件node.prev != head || !compareAndSetState(0,1)的非是node.prev == head && compareAndSetState(0,1)
        //下面不需要原子更新了，因为同时只有一个线程可以访问到这里
        //head后移一位，指向加锁成功的节点   // head是虚拟节点
        head = node;
        //清空当前节点的内容，协助gc  将node变为虚拟节点
        node.thread=null;
        //将上一个节点从链表中剔除，协助gc
        node.prev= null;
        prev.next=null;
    }
```

（1）尝试获取锁，成功了就直接返回；

（2）未获取到锁，就进入队列排队；

（3）入队之后，再次尝试获取锁；

（4）如果不成功，就阻塞；

（5）如果成功了，就把头节点后移一位，并清空当前节点的内容，且与上一个节点断绝关系；

（6）加锁结束；


其他线程恶意解锁怎么办  出现问题，解析在后面
解锁
```
  public void unlock(){
       //把state更新为0,这里不需要原子更新，因为同时只有一个线程访问到这里
       state =0;
       //下一个待唤醒的节点
       Node next = head.next;
       //下一个节点不为null才唤醒他
       if(next !=null){
           unsafe.unpark(next.thread);
       }
    }
```
（1）把state改成0，这里不需要CAS更新，因为现在还在加锁中，只有一个线程去更新，在这句之后就释放了锁；

（2）如果有下一个节点就唤醒它；

（3）唤醒之后就会接着走上面lock()方法的while循环再去尝试获取锁；

（4）唤醒的线程不是百分之百能获取到锁的，因为这里state更新成0的时候就解锁了，之后可能就有线程去尝试加锁了。
  //新的线程和队列中的线程抢锁，可能是新的线程成功了


疑问：
（1）我们实现的锁支持可重入吗？

答：不可重入，因为我们每次只把state更新为1。如果要支持可重入也很简单，获取锁时检测锁是不是被当前线程占有着，如果是就把state的值加1，
   释放锁时每次减1即可，减为0时表示锁已释放。

（2）我们实现的锁是公平锁还是非公平锁？

答：非公平锁，因为获取锁的时候我们先尝试了一次，这里并不是严格的排队，所以是非公平锁



测试
上面完整的锁的实现就完了，是不是很简单，但是它是不是真的可靠呢，敢不敢来试试？！

直接上测试代码：
```
    private static int count = 0;

    public static void main(String[] args) throws InterruptedException {
        MyLock lock = new MyLock();

        CountDownLatch countDownLatch = new CountDownLatch(1000);

        IntStream.range(0, 1000).forEach(i -> new Thread(() -> {
            lock.lock();

            try {
                IntStream.range(0, 10000).forEach(j -> {
                    count++;
                });
            } finally {
                lock.unlock();
            }
//            System.out.println(Thread.currentThread().getName());
            countDownLatch.countDown();
        }, "tt-" + i).start());

        countDownLatch.await();

        System.out.println(count);
    }
```

完整代码:
```
public class MyLock {

    private static int count = 0;

    public static void main(String[] args) throws InterruptedException {
        MyLock lock = new MyLock();

        CountDownLatch countDownLatch = new CountDownLatch(1000);

        IntStream.range(0, 1000).forEach(i -> new Thread(() -> {
            lock.lock();

            try {
                IntStream.range(0, 10000).forEach(j -> {
                    count++;
                });
            } finally {
                lock.unlock();
            }
//            System.out.println(Thread.currentThread().getName());
            countDownLatch.countDown();
        }, "tt-" + i).start());

        countDownLatch.await();

        System.out.println(count);
    }



    private static class Node{
        Thread thread;//存储线程
        Node prev;//前一个节点(可以没有，但实现起来很困难)
        Node next;//后一个节点
        public Node(){}
        public Node(Thread thread,Node prev){
            this.thread = thread;
            this.prev = prev;
        }
    }
    static final Node EMPTY = new Node();
    private volatile Node head;//链表头
    private volatile Node tail;//链表尾

    public MyLock() {
        this.head = this.tail = EMPTY;
    }

    //通过cas更新尾节点tail
    private boolean compareAndSetTail(Node expect, Node update){
        return unsafe.compareAndSwapObject(this,tailOffset,expect,update);
    }

    //通过cas更新state
    private boolean compareAndSetState(int expect,int update){
        return unsafe.compareAndSwapInt(this,stateOffset,expect,update);
    }

    //当前是否被线程占用了0没有；1有，此时使用cas更新失败
    private volatile int state;

    public void lock(){
        // 尝试更新state字段，更新成功说明占有了锁  当前没有其他线程抢占锁
        if(compareAndSetState(0,1)){
            return;
        }
        //未更新成功则入队  记录要求加锁的线程
        Node node = enqueue();
        Node prev = node.prev;
        //再次尝试获取锁，需要检测上一个节点是不是head，按入队顺序加锁
        while (node.prev != head || !compareAndSetState(0,1)){
            //未获取到锁，阻塞
            unsafe.park(false,0L);
        }
        //获取到锁了，且上一个节点是head   条件node.prev != head || !compareAndSetState(0,1)的非是node.prev == head && compareAndSetState(0,1)
        //下面不需要原子更新了，因为同时只有一个线程可以访问到这里
        //head后移一位，指向加锁成功的节点   // head是虚拟节点
        head = node;
        //清空当前节点的内容，协助gc  将node变为虚拟节点
        node.thread=null;
        //将上一个节点从链表中剔除，协助gc
        node.prev= null;
        prev.next=null;
    }

    //解锁
    public void unlock(){
       //把state更新为0,这里不需要原子更新，因为同时只有一个线程访问到这里
       state =0;
       //下一个待唤醒的节点
       Node next = head.next;
       //下一个节点不为null才唤醒他
       if(next !=null){
           unsafe.unpark(next.thread);
       }
    }

    //入队
    private Node enqueue() {
       while(true){
           //获取尾节点  更新失败获取新的尾节点，然后创建新的节点
           Node t= tail;
           Node node = new Node(Thread.currentThread(),t);
           //不断尝试原子更新尾节点
           if(compareAndSetTail(t,node)){
               //更新尾节点成功了，让原节点的next指向当前节点
               t.next = node;
               return node;
           }
       }
    }


    private  static Unsafe unsafe;
    private  static   long tailOffset;
    private  static   long stateOffset;
    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafe= (Unsafe) f.get(null);
            tailOffset = unsafe.objectFieldOffset(MyLock.class.getDeclaredField("tail"));
            stateOffset = unsafe.objectFieldOffset(MyLock.class.getDeclaredField("state"));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
```


其他线程恶意解锁，怎么办
测试
```
node增加hashCode  
 @Override
public int hashCode() {
    return Objects.hash(thread);
}
unlock返回下一个节点
  public Node unlock(){
       //把state更新为0,这里不需要原子更新，因为同时只有一个线程访问到这里
       state =0;
       //下一个待唤醒的节点
       Node next = head.next;
       //下一个节点不为null才唤醒他
       if(next !=null){
           unsafe.unpark(next.thread);
       }
       return next;
    }
```
代码测试
```
 public static void main(String[] args) throws InterruptedException {
        MyLock lock = new MyLock();

        CountDownLatch countDownLatch = new CountDownLatch(1000);

        IntStream.range(0, 1000).forEach(i -> new Thread(() -> {
            lock.lock();
            try {
                IntStream.range(0, 10000).forEach(j -> {
                    count++;
                });
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } finally {
                Node node =lock.unlock();
                int hash =0;
                if(node!=null) hash=node.hashCode();
                System.out.println(Thread.currentThread().getName()+" 正常unlock "+hash);
            }
//            System.out.println(Thread.currentThread().getName());
            countDownLatch.countDown();
        }, "tt-" + i).start());
        //恶意线程进行恶意解锁
        IntStream.range(0, 1000).forEach(i -> new Thread(() -> {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Node node =lock.unlock();
           int hash =0;
           if(node!=null) hash=node.hashCode();
            System.out.println(Thread.currentThread().getName()+" 恶意unlock "+hash);
        }, "tt-err" + i).start());
        countDownLatch.await();

        System.out.println(count);
    }
```
3次结果  同时可以看到日志恶意线程解锁成功的log  例如恶意unlock 31
```
9959537
9976906
9983154
```
代码改进
加锁保存当前线程，解锁时校验  持有锁的线程只有一个，所以是个普通变量
```
 Thread lockThread;
 public void lock(){
    if(compareAndSetState(0,1)){
        lockThread=Thread.currentThread();
        return;
    }
    Node node = enqueue();
    Node prev = node.prev;
    while (node.prev != head || !compareAndSetState(0,1)){
        unsafe.park(false,0L);
    }
    lockThread = Thread.currentThread();
    head = node;
    node.thread=null;
    node.prev= null;
    prev.next=null;
}
public Node unlock(){
   if(lockThread != Thread.currentThread()){
        return null;
    }
   //把state更新为0,这里不需要原子更新，因为同时只有一个线程访问到这里
   state =0;
   //下一个待唤醒的节点
   Node next = head.next;
   //下一个节点不为null才唤醒他
   if(next !=null){
       unsafe.unpark(next.thread);
   }
   return next;
} 
```
结果正常
```
10000000
```