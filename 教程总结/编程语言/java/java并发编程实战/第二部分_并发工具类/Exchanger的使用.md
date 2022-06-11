
https://www.jianshu.com/p/322a18617c5d
Exchanger用于进行线程间的数据交换，它提供一个同步点，在这个同步点，两个线程可以交换彼此的数据。这两个线程通过exchange 方法交换数据，

如果第一个线程先执行exchange 方法，它会一直等待第二个线程也执行exchange 方法，当两个线程都到达同步点时，这两个线程就可以交换数据
//适用于两个线程交换数据的情况

根据文档描述，有几个要点:

此类提供对外的操作是同步的；

用于成对出现的线程之间交换数据；

可以视作双向的同步队列；

可应用于基因算法、流水线设计等场景


api方法
```
public V exchange(V x) throws InterruptedException  //等待另一个线程到达交换点然后交换数据
public V exchange(V x, long timeout, TimeUnit unit) throws InterruptedException, TimeoutException
```

实战场景
设计一个定时任务，每日凌晨执行。在定时任务中启动两个线程，一个线程负责对业务明细表（xxx_info）进行查询统计，把统计的结果放置在内存缓冲区，
另一个线程负责读取缓冲区中的统计结果并插入到业务统计表（xxx_statistics）中。
亲，这样的场景是不是听起来很有感觉？没错!两个线程在内存中批量交换数据，这个事情我们可以使用Exchanger去做！


数据交换案例
当线程A调用Exchange对象的exchange()方法后，他会陷入阻塞状态，直到线程B也调用了exchange()方法，然后以线程安全的方式交换数据，
之后线程A和B继续运行
```
private static Random random = new Random(System.currentTimeMillis());
    public static void main(String[] args) {
        ExecutorService executor = Executors.newCachedThreadPool();
        final Exchanger exchanger = new Exchanger();
        executor.execute(new Runnable() {
            String data = "data1";

            @Override
            public void run() {
                doExchangeWork(data, exchanger);
            }
        });

        executor.execute(new Runnable() {
            String data = "data2";

            @Override
            public void run() {
                doExchangeWork(data, exchanger);
            }
        });
        executor.shutdown();
    }

    private static void doExchangeWork(String data, Exchanger exchanger) {
        try {
            int time = random.nextInt(5);
            System.out.println(Thread.currentThread().getName() + "正在把数据 " + data + " 交换出去,time:"+time);

            TimeUnit.SECONDS.sleep(time);

            String exchangeData = (String) exchanger.exchange(data);
            System.out.println(Thread.currentThread().getName() + "交换得到数据  " + exchangeData);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
```