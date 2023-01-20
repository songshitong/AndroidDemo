https://github.com/xfhy/Android-Notes/blob/master/Blogs/Java/%E5%B9%B6%E5%8F%91/Future.md
https://www.jianshu.com/p/50c03b1226fe

FutureTask 整个构造类似适配器和装饰器
RunnableFuture 将run和结果获取整合在一起
FutureTask通过callable，将Runnable的run增强，增加了结果返回
结果的等待通过链表和LockSupport实现

Future作用
比如当做一定运算的时候,运算过程比较耗时,有时会去查数据库,或是繁重的计算,比如压缩,加密等,在这种情况下,如果我们一直在原地等待方法返回,
显然是不明智的,整体程序的运行效率会大大降低.我们可以把运算的过程放到子线程去执行,再通过Future去控制子线程执行的计算过程,
最后获取计算结果.提高运行效率,是一种异步的思想.

Callable和Future的关系
Callable的返回结果需要通过Future的get方法来获取.Future相当于一个存储器,存储了call方法的任务结果.除此之外,还可以通过Future的isDone方法来判断任务是否已经执行完毕了,
还可以通过cancel方法取消这个任务,或限时获取任务结果等.

代码示例：
```
public class OneFuture {
    public static void main(String[] args) {
        ExecutorService service = Executors.newFixedThreadPool(10);
        Future<Integer> future = service.submit(new CallableTask());
        try {
            System.out.println(future.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        service.shutdown();
    }

    static class CallableTask implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            Thread.sleep(3000);
            return new Random().nextInt();
        }
    }
}
```

http://hg.openjdk.java.net/jdk8/jdk8/jdk/file/687fd7c7986d/src/share/classes/java/util/concurrent/Future.java
```
public interface Future<V> {
    boolean cancel(boolean mayInterruptIfRunning);
    boolean isCancelled();
    boolean isDone();
    
    V get() throws InterruptedException, ExecutionException;
    V get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutExceptio
}
```
get() 获取结果
主要是获取任务执行的结果,该方法在执行时的行为取决于Callable任务的状态.

最常见: 当执行get的时候,任务已经执行完毕了.可以立刻返回,获取到任务执行的结果.
任务还没有结果,可能任务还在线程池的队列中还没开始执行. 或者任务正在执行中,也是没有结果的. 无论任务还没开始,还是任务正在执行中,
    调用get的时候都会把当前的线程阻塞,直到任务完成再把结果返回.
任务执行过程中抛出异常,一旦出现这种情况,我们再去调用get方法时,就会抛出ExecutionException异常,不管我们执行call方法时里面抛出的异常类型是什么,
   在执行get方法时所获得的异常都是ExecutionException.
任务被取消了,如果任务已被取消,则调用get方法会抛出CancellationException
任务超时,get有一个重载方法,带延迟参数的.调用这个带延迟参数的get方法后,如果在时间内完成任务会正常返回;如果到了指定时间还没完成任务,
  就会抛出TimeoutException,代表超时了.

cancel() 取消任务的执行
当任务还没开始执行时,调用cancel,任务会被正常取消,未来也不会执行,那么cancel方法返回true.
如果任务已经完成,或者之前已经被取消过,那么cancel方法就代表取消失败,返回false.
当任务正在执行,这时调用cancel方法是不会直接取消这个任务的,而是会根据传入的参数做判断.cancel方法必须传入一个参数mayInterruptIfRunning,
  如果是true则执行任务的线程就会收到一个中断的信号,正在执行的任务可能会有一些处理中断的逻辑,进而停止.如果是false,则就代表不中断正在运行的任务,
  本次cancel不会有任何效果,同时cancel方法返回false.


jdk 11
Executors中future的创建
java\util\concurrent\AbstractExecutorService.class
```
 public <T> Future<T> submit(Callable<T> task) {
    if (task == null) {
      throw new NullPointerException();
    } else {
      RunnableFuture<T> ftask = this.newTaskFor(task);
      this.execute(ftask);
      return ftask;
    }
  }

  protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
    return new FutureTask(callable);
  }  
```
通过callable创建实现RunnableFuture的FutureTask    RunnableFuture实现Runnable的run方法和Future接口


java\util\concurrent\FutureTask.class
```
public class FutureTask<V> implements RunnableFuture<V> {
//异步任务
 private Callable<V> callable;
 //结果
 private Object outcome;
 //当前运行的线程，执行run时绑定
 private volatile Thread runner;
 //等待结果的列表
 private volatile WaitNode waiters;
 static final class WaitNode {
    volatile Thread thread = Thread.currentThread();
    volatile WaitNode next;
  }
  
  //状态 volatile修饰，多线程数据同步
  private volatile int state;
  private static final int NEW = 0;
  private static final int COMPLETING = 1; //结果同步中
  private static final int NORMAL = 2; //同步完成
  private static final int EXCEPTIONAL = 3; //异常状态
  private static final int CANCELLED = 4;
  private static final int INTERRUPTING = 5;
  private static final int INTERRUPTED = 6;
 
 static{ //对于state，runner，waiters同步控制
      MethodHandles.Lookup l = MethodHandles.lookup();
      STATE = l.findVarHandle(FutureTask.class, "state", Integer.TYPE);
      RUNNER = l.findVarHandle(FutureTask.class, "runner", Thread.class);
      WAITERS = l.findVarHandle(FutureTask.class, "waiters", WaitNode.class);
 }

 
  public FutureTask(Callable<V> callable) {
        // 保存外部传来的任务, 待会在run()方法中调用
        this.callable = callable;
        this.state = NEW;       // ensure visibility of callable
    }
 
  public void run() {
        ...
        Callable<V> c = callable;
        //FutureTak的执行逻辑委托给用户提供的真正任务
        V result = c.call(); 
        // 设置异步任务结果
        set(result);
    }
  
  protected void set(V v) {
    if (STATE.compareAndSet(this, 0, 1)) { //状态改为同步中
      //将结果保存到outcome
      this.outcome = v;
      STATE.setRelease(this, 2); //状态改为同步完成
      this.finishCompletion(); //通知等待者结果
    }
  }  
  
  //其实就是做一些收尾工作，同时等待者
 private void finishCompletion() {
    while(true) {
      WaitNode q;
      if ((q = this.waiters) != null) {
        if (!WAITERS.weakCompareAndSet(this, q, (Void)null)) { //其他线程在处理，继续检查
          continue;            //其他线程处理完，会退出work线程的while，所有就不在执行了
        }

        while(true) {
          Thread t = q.thread;
          if (t != null) {
            q.thread = null; //唤醒线程
            LockSupport.unpark(t);
          }

          WaitNode next = q.next;
          if (next == null) { //到头了，退出while
            break;
          }

          q.next = null; //切断链表
          q = next;
        }
      }
      //结果都已经通知完成
      this.done(); //进入自定义的done
      this.callable = null;
      return; //退出线程
    }
  } 
  
  protected void done() { }    
}
```

异步任务执行完毕后如何主动通知调用者? **
其实我们只要扩展FutureTask, 再给扩展对象设置一个回调对象, 然后重写done()方法, 在done()方法内调用回调对象就可以了.
Guava的com.google.common.util.concurrent.ListenableFutureTask:
```
public class ListenableFutureTask<V> extends FutureTask<V> implements ListenableFuture<V> {

  //存储异步任务完成后的回调以及回调所在的Executor
  private final ExecutionList executionList = new ExecutionList(); 

  // 其他代码省略 ...

  //添加异步任务完成后的回调和回调所在的Executor
  @Override
  public void addListener(Runnable listener, Executor exec) {
    executionList.add(listener, exec);
  }

  @Override
  protected void done() {
    // 异步任务完成后回调
    executionList.execute();
  }
}
```
需要注意的是FutureTask的done()方法是在Worker线程中执行的, 一般我们获取结果是在其他线程, 因此需要把计算结果挪到指定的线程中去.
 因此不仅需要指定任务完成的回调, 还需要指定任务完成的回调所在的线程


结果获取
```
 public V get() throws InterruptedException, ExecutionException {
    int s = this.state;
    if (s <= 1) { //执行中，等待
      s = this.awaitDone(false, 0L);
    }

    return this.report(s);
  }
  
  private V report(int s) throws ExecutionException {
    Object x = this.outcome;
    if (s == 2) { //执行正常，返回
      return x;
    } else if (s >= 4) { //异常状态，返回
      throw new CancellationException();
    } else {
      throw new ExecutionException((Throwable)x);
    }
  }
  
  //timed 是否设置超时
  private int awaitDone(boolean timed, long nanos) throws InterruptedException {
    long startTime = 0L;
    WaitNode q = null;
    boolean queued = false;

    while(true) {
      int s = this.state;
      //等待完成，返回
      if (s > 1) {
        if (q != null) {
          q.thread = null;
        }
        return s;
      }
      //结果同步中，马上完成，稍后再次检查
      if (s == 1) {
        Thread.yield();
      } else {
        if (Thread.interrupted()) { //任务中断了，
          this.removeWaiter(q);
          throw new InterruptedException();
        }

        if (q == null) {
          if (timed && nanos <= 0L) { //设置超时但是<=0，直接返回
            return s;
          }
          q = new WaitNode(); //头结点
        } else if (!queued) { //没有进入链表，添加节点
          queued = WAITERS.weakCompareAndSet(this, q.next = this.waiters, q);
        } else if (timed) {
          long parkNanos; //设置超时时间
          if (startTime == 0L) {
            startTime = System.nanoTime();
            if (startTime == 0L) {
              startTime = 1L;
            }
            parkNanos = nanos;
          } else {
            long elapsed = System.nanoTime() - startTime;
            if (elapsed >= nanos) { //时间到了，返回结果，检查状态，移除节点
              this.removeWaiter(q);
              return this.state;
            }

            parkNanos = nanos - elapsed;
          }
          if (this.state < 1) { //等待到超时
            LockSupport.parkNanos(this, parkNanos);
          }
        } else {
          //没设置超时时间，一直阻塞查询结果的线程
          LockSupport.park(this);
        }
      }
    }
  } 
```