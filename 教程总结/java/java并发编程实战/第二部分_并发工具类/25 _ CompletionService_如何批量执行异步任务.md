在《23 | Future：如何用多线程实现最优的“烧水泡茶”程序？》的最后，我给你留了道思考题，如何优化一个询价应用的核心代码？
如果采用“ThreadPoolExecutor+Future”的方案，你的优化结果很可能是下面示例代码这样：用三个线程异步执行询价，
通过三次调用 Future 的 get() 方法获取询价结果，之后将询价结果保存在数据库中。

// 创建线程池
ExecutorService executor =
  Executors.newFixedThreadPool(3);
// 异步向电商S1询价
Future<Integer> f1 = 
  executor.submit(
    ()->getPriceByS1());
// 异步向电商S2询价
Future<Integer> f2 = 
  executor.submit(
    ()->getPriceByS2());
// 异步向电商S3询价
Future<Integer> f3 = 
  executor.submit(
    ()->getPriceByS3());
    
// 获取电商S1报价并保存
r=f1.get();
executor.execute(()->save(r));
  
// 获取电商S2报价并保存
r=f2.get();
executor.execute(()->save(r));
  
// 获取电商S3报价并保存  
r=f3.get();
executor.execute(()->save(r));


上面的这个方案本身没有太大问题，但是有个地方的处理需要你注意，那就是如果获取电商 S1 报价的耗时很长，那么即便获取电商 S2 报价的耗时很短，
也无法让保存 S2 报价的操作先执行，因为这个主线程都阻塞在了 f1.get() 操作上。这点小瑕疵你该如何解决呢？

估计你已经想到了，增加一个阻塞队列，获取到 S1、S2、S3 的报价都进入阻塞队列，然后在主线程中消费阻塞队列，这样就能保证先获取到的报价先保存到数据库了。
下面的示例代码展示了如何利用阻塞队列实现先获取到的报价先保存到数据库。

// 创建阻塞队列
BlockingQueue<Integer> bq =
  new LinkedBlockingQueue<>();
//电商S1报价异步进入阻塞队列  
executor.execute(()->
  bq.put(f1.get()));
//电商S2报价异步进入阻塞队列  
executor.execute(()->
  bq.put(f2.get()));
//电商S3报价异步进入阻塞队列  
executor.execute(()->
  bq.put(f3.get()));
//异步保存所有报价  
for (int i=0; i<3; i++) {
  Integer r = bq.take();
  executor.execute(()->save(r));
}  

利用 CompletionService 实现
询价系统不过在实际项目中，并不建议你这样做，因为 Java SDK 并发包里已经提供了设计精良的 CompletionService。利用 CompletionService 
不但能帮你解决先获取到的报价先保存到数据库的问题，而且还能让代码更简练。
CompletionService 的实现原理也是内部维护了一个阻塞队列，当任务执行结束就把任务的执行结果加入到阻塞队列中，
不同的是 CompletionService 是把任务执行结果的 Future 对象加入到阻塞队列中，而上面的示例代码是把任务最终的执行结果放入了阻塞队列中。

那到底该如何创建 CompletionService 呢？CompletionService 接口的实现类是 ExecutorCompletionService，这个实现类的构造方法有两个，
分别是：
ExecutorCompletionService(Executor executor)；
ExecutorCompletionService(Executor executor, BlockingQueue<Future<V>> completionQueue)。

这两个构造方法都需要传入一个线程池，如果不指定 completionQueue，那么默认会使用无界的 LinkedBlockingQueue。
任务执行结果的 Future 对象就是加入到 completionQueue 中。

下面的示例代码完整地展示了如何利用 CompletionService 来实现高性能的询价系统。其中，我们没有指定 completionQueue，
因此默认使用无界的 LinkedBlockingQueue。之后通过 CompletionService 接口提供的 submit() 方法提交了三个询价操作，
这三个询价操作将会被 CompletionService 异步执行。最后，我们通过 CompletionService 接口提供的 take() 方法获取一个 Future 对象
（前面我们提到过，加入到阻塞队列中的是任务执行结果的 Future 对象），调用 Future 对象的 get() 方法就能返回询价操作的执行结果了

// 创建线程池
ExecutorService executor = 
  Executors.newFixedThreadPool(3);
// 创建CompletionService
CompletionService<Integer> cs = new 
  ExecutorCompletionService<>(executor);
// 异步向电商S1询价
cs.submit(()->getPriceByS1());
// 异步向电商S2询价
cs.submit(()->getPriceByS2());
// 异步向电商S3询价
cs.submit(()->getPriceByS3());
// 将询价结果异步保存到数据库
for (int i=0; i<3; i++) {
  Integer r = cs.take().get();
  executor.execute(()->save(r));
}


CompletionService 接口说明下面我们详细地介绍一下 CompletionService 接口提供的方法，CompletionService 接口提供的方法有 5 个，
这 5 个方法的方法签名如下所示。

Future<V> submit(Callable<V> task);
Future<V> submit(Runnable task, V result);
Future<V> take() 
  throws InterruptedException;
Future<V> poll();
Future<V> poll(long timeout, TimeUnit unit) 
  throws InterruptedException;
  
其中，submit() 相关的方法有两个。一个方法参数是Callable<V> task，前面利用 CompletionService 实现询价系统的示例代码中，
我们提交任务就是用的它。另外一个方法有两个参数，分别是Runnable task和V result，这个方法类似于 ThreadPoolExecutor 的 
<T> Future<T> submit(Runnable task, T result) ，这个方法在《23 | Future：如何用多线程实现最优的“烧水泡茶”程序？》中
我们已详细介绍过，这里不再赘述。  

CompletionService 接口其余的 3 个方法，都是和阻塞队列相关的，take()、poll() 都是从阻塞队列中获取并移除一个元素；
它们的区别在于如果阻塞队列是空的，那么调用 take() 方法的线程会被阻塞，而 poll() 方法会返回 null 值。 
poll(long timeout, TimeUnit unit) 方法支持以超时的方式获取并移除阻塞队列头部的一个元素，如果等待了 timeout unit 时间，
阻塞队列还是空的，那么该方法会返回 null 值。

利用 CompletionService 实现 Dubbo 中的 Forking Cluster
Dubbo 中有一种叫做 Forking 的集群模式，这种集群模式下，支持并行地调用多个查询服务，只要有一个成功返回结果，整个服务就可以返回了。
例如你需要提供一个地址转坐标的服务，为了保证该服务的高可用和性能，你可以并行地调用 3 个地图服务商的 API，然后只要有 1 个正确返回了结果 r，
那么地址转坐标这个服务就可以直接返回 r 了。这种集群模式可以容忍 2 个地图服务商服务异常，但缺点是消耗的资源偏多。

dubbo Apache Dubbo 是一款高性能、轻量级的开源 Java 服务框架
提供了六大核心能力：面向接口代理的高性能RPC调用，智能容错和负载均衡，服务自动注册和发现，高度可扩展能力，运行期流量调度，可视化的服务治理与运维


geocoder(addr) {
  //并行执行以下3个查询服务， 
  r1=geocoderByS1(addr);
  r2=geocoderByS2(addr);
  r3=geocoderByS3(addr);
  //只要r1,r2,r3有一个返回
  //则返回
  return r1|r2|r3;
}

利用 CompletionService 可以快速实现 Forking 这种集群模式，比如下面的示例代码就展示了具体是如何实现的。首先我们创建了一个线程池 executor 
、一个 CompletionService 对象 cs 和一个Future<Integer>类型的列表 futures，每次通过调用 CompletionService 的 submit() 
方法提交一个异步任务，会返回一个 Future 对象，我们把这些 Future 对象保存在列表 futures 中。通过调用 cs.take().get()，
我们能够拿到最快返回的任务执行结果，只要我们拿到一个正确返回的结果，就可以取消所有任务并且返回最终结果了。

// 创建线程池
ExecutorService executor =
  Executors.newFixedThreadPool(3);
// 创建CompletionService
CompletionService<Integer> cs =
  new ExecutorCompletionService<>(executor);
// 用于保存Future对象
List<Future<Integer>> futures =
  new ArrayList<>(3);
//提交异步任务，并保存future到futures 
futures.add(
  cs.submit(()->geocoderByS1()));
futures.add(
  cs.submit(()->geocoderByS2()));
futures.add(
  cs.submit(()->geocoderByS3()));
// 获取最快返回的任务执行结果
Integer r = 0;
try {
  // 只要有一个成功返回，则break
  for (int i = 0; i < 3; ++i) {
    r = cs.take().get();
    //简单地通过判空来检查是否成功返回
    if (r != null) {
      break;
    }
  }
} finally {
  //取消所有任务
  for(Future<Integer> f : futures)
    f.cancel(true);
}
// 返回结果
return r;

总结
当需要批量提交异步任务的时候建议你使用 CompletionService。CompletionService 将线程池 Executor 和阻塞队列 BlockingQueue
 的功能融合在了一起，能够让批量异步任务的管理更简单。除此之外，CompletionService 能够让异步任务的执行结果有序化，
 先执行完的先进入阻塞队列，利用这个特性，你可以轻松实现后续处理的有序性，避免无谓的等待，同时还可以快速实现诸如 Forking Cluster 这样的需求。
 CompletionService 的实现类 ExecutorCompletionService，需要你自己创建线程池，虽看上去有些啰嗦，
 但好处是你可以让多个 ExecutorCompletionService 的线程池隔离，这种隔离性能避免几个特别耗时的任务拖垮整个应用的风险。
 
Dubbo 中的Forking Cluster  并行调用多个服务器，成功一个就返回 
 
CompletionService 的实现类 ExecutorCompletionService，需要你自己创建线程池，虽看上去有些啰嗦，但好处是你可以让多个 
ExecutorCompletionService 的线程池隔离，这种隔离性能避免几个特别耗时的任务拖垮整个应用的风险 
课后思考
本章使用 CompletionService 实现了一个询价应用的核心功能，后来又有了新的需求，需要计算出最低报价并返回，下面的示例代码尝试实现这个需求，
你看看是否存在问题呢？

// 创建线程池
ExecutorService executor = 
  Executors.newFixedThreadPool(3);
// 创建CompletionService
CompletionService<Integer> cs = new 
  ExecutorCompletionService<>(executor);
// 异步向电商S1询价
cs.submit(()->getPriceByS1());
// 异步向电商S2询价
cs.submit(()->getPriceByS2());
// 异步向电商S3询价
cs.submit(()->getPriceByS3());
// 将询价结果异步保存到数据库
// 并计算最低报价
AtomicReference<Integer> m =
  new AtomicReference<>(Integer.MAX_VALUE);
for (int i=0; i<3; i++) {
  executor.execute(()->{
    Integer r = null;
    try {
      r = cs.take().get();
    } catch (Exception e) {}
    save(r);
    m.set(Integer.min(m.get(), r));
  });
}
return m;


小华
看老师的意图是要等三个比较报假的线程都执行完才能执行主线程的的return m，但是代码无法保证三个线程都执行完，和主线程执行return的顺序，
因此，m的值不是准确的，可以加个线程栈栏，线程执行完计数器，来达到这效果

西行寺咕哒子
试过返回值是2147483647，也就是int的最大值。没有等待操作完成就猴急的返回了。 m.set(Integer.min(m.get(), r)... 这个操作也不是原子操作。
试着自己弄了一下：
public Integer run(){
        // 创建线程池
        ExecutorService executor = Executors.newFixedThreadPool(3);
        // 创建 CompletionService
        CompletionService<Integer> cs = new ExecutorCompletionService<>(executor);
        AtomicReference<Integer> m = new AtomicReference<>(Integer.MAX_VALUE);
        // 异步向电商 S1 询价
        cs.submit(()->getPriceByS1());
        // 异步向电商 S2 询价
        cs.submit(()->getPriceByS2());
        // 异步向电商 S3 询价
        cs.submit(()->getPriceByS3());
        // 将询价结果异步保存到数据库
        // 并计算最低报价
        for (int i=0; i<3; i++) {
            Integer r = logIfError(()->cs.take().get());
            executor.execute(()-> save(r));
            m.getAndUpdate(v->Integer.min(v, r));
        }
        return m.get();
    }
不知道可不可行
作者回复: 👍