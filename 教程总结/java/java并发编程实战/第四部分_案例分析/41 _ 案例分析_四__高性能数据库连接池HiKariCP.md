实际工作中，我们总会难免和数据库打交道；只要和数据库打交道，就免不了使用数据库连接池。业界知名的数据库连接池有不少，
例如 c3p0、DBCP、Tomcat JDBC Connection Pool、Druid 等，不过最近最火的是 HiKariCP。

HiKariCP 号称是业界跑得最快的数据库连接池，这两年发展得顺风顺水，尤其是 Springboot 2.0 将其作为默认数据库连接池后，
江湖一哥的地位已是毋庸置疑了。那它为什么那么快呢？今天咱们就重点聊聊这个话题。

什么是数据库连接池
在详细分析 HiKariCP 高性能之前，我们有必要先简单介绍一下什么是数据库连接池。本质上，数据库连接池和线程池一样，都属于池化资源，
作用都是避免重量级资源的频繁创建和销毁，对于数据库连接池来说，也就是避免数据库连接频繁创建和销毁。如下图所示，
服务端会在运行期持有一定数量的数据库连接，当需要执行 SQL 时，并不是直接创建一个数据库连接，而是从连接池中获取一个；
当 SQL 执行完，也并不是将数据库连接真的关掉，而是将其归还到连接池中。

在实际工作中，我们都是使用各种持久化框架来完成数据库的增删改查，基本上不会直接和数据库连接池打交道，为了能让你更好地理解数据库连接池的工作原理，
下面的示例代码并没有使用任何框架，而是原生地使用 HiKariCP。执行数据库操作基本上是一系列规范化的步骤
通过数据源获取一个数据库连接；
创建 Statement；
执行 SQL；
通过 ResultSet 获取 SQL 执行结果；
释放 ResultSet；
释放 Statement；
释放数据库连接。

下面的示例代码，通过 ds.getConnection() 获取一个数据库连接时，其实是向数据库连接池申请一个数据库连接，而不是创建一个新的数据库连接。
同样，通过 conn.close() 释放一个数据库连接时，也不是直接将连接关闭，而是将连接归还给数据库连接池。

//数据库连接池配置
HikariConfig config = new HikariConfig();
config.setMinimumIdle(1);
config.setMaximumPoolSize(2);
config.setConnectionTestQuery("SELECT 1");
config.setDataSourceClassName("org.h2.jdbcx.JdbcDataSource");
config.addDataSourceProperty("url", "jdbc:h2:mem:test");
// 创建数据源
DataSource ds = new HikariDataSource(config);
Connection conn = null;
Statement stmt = null;
ResultSet rs = null;
try {
  // 获取数据库连接
  conn = ds.getConnection();
  // 创建Statement 
  stmt = conn.createStatement();
  // 执行SQL
  rs = stmt.executeQuery("select * from abc");
  // 获取结果
  while (rs.next()) {
    int id = rs.getInt(1);
    ......
  }
} catch(Exception e) {
   e.printStackTrace();
} finally {
  //关闭ResultSet
  close(rs);
  //关闭Statement 
  close(stmt);
  //关闭Connection
  close(conn);
}
//关闭资源
void close(AutoCloseable rs) {
  if (rs != null) {
    try {
      rs.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
}


HiKariCP 官方网站解释了其性能之所以如此之高的秘密。微观上 HiKariCP 程序编译出的字节码执行效率更高，站在字节码的角度去优化 Java 代码，
HiKariCP 的作者对性能的执着可见一斑，不过遗憾的是他并没有详细解释都做了哪些优化。而宏观上主要是和两个数据结构有关，一个是 FastList，
另一个是 ConcurrentBag。下面我们来看看它们是如何提升 HiKariCP 的性能的。
https://github.com/brettwooldridge/HikariCP/wiki/Down-the-Rabbit-Hole

FastList 解决了哪些性能
问题按照规范步骤，执行完数据库操作之后，需要依次关闭 ResultSet、Statement、Connection，但是总有粗心的同学只是关闭了 Connection，
而忘了关闭 ResultSet 和 Statement。为了解决这种问题，最好的办法是当关闭 Connection 时，能够自动关闭 Statement。为了达到这个目标，
Connection 就需要跟踪创建的 Statement，最简单的办法就是将创建的 Statement 保存在数组 ArrayList 里，这样当关闭 Connection 的时候，
就可以依次将数组中的所有 Statement 关闭。

HiKariCP 觉得用 ArrayList 还是太慢，当通过 conn.createStatement() 创建一个 Statement 时，需要调用 ArrayList 的 add() 方法加入到
 ArrayList 中，这个是没有问题的；但是当通过 stmt.close() 关闭 Statement 的时候，需要调用 ArrayList 的 remove() 方法来将其从
  ArrayList 中删除，这里是有优化余地的。
  
假设一个 Connection 依次创建 6 个 Statement，分别是 S1、S2、S3、S4、S5、S6，按照正常的编码习惯，关闭 Statement 的顺序一般是逆序的，
关闭的顺序是：S6、S5、S4、S3、S2、S1，而 ArrayList 的 remove(Object o) 方法是顺序遍历查找，逆序删除而顺序查找，
这样的查找效率就太慢了。如何优化呢？很简单，优化成逆序查找就可以了  

HiKariCP 中的 FastList 相对于 ArrayList 的一个优化点就是将 remove(Object element) 方法的查找顺序变成了逆序查找。
除此之外，FastList 还有另一个优化点，是 get(int index) 方法没有对 index 参数进行越界检查，HiKariCP 能保证不会越界，
所以不用每次都进行越界检查。

整体来看，FastList 的优化点还是很简单的。下面我们再来聊聊 HiKariCP 中的另外一个数据结构 ConcurrentBag，看看它又是如何提升性能的。

ConcurrentBag 解决了哪些性能问题
如果让我们自己来实现一个数据库连接池，最简单的办法就是用两个阻塞队列来实现，一个用于保存空闲数据库连接的队列 idle，
另一个用于保存忙碌数据库连接的队列 busy；获取连接时将空闲的数据库连接从 idle 队列移动到 busy 队列，而关闭连接时将数据库连接从 busy 移动到 idle。
这种方案将并发问题委托给了阻塞队列，实现简单，但是性能并不是很理想。因为 Java SDK 中的阻塞队列是用锁实现的，而高并发场景下锁的争用对性能影响很大。

//忙碌队列
BlockingQueue<Connection> busy;
//空闲队列
BlockingQueue<Connection> idle;

HiKariCP 并没有使用 Java SDK 中的阻塞队列，而是自己实现了一个叫做 ConcurrentBag 的并发容器。ConcurrentBag 的设计最初源自 C#，
它的一个核心设计是使用 ThreadLocal 避免部分并发问题，不过 HiKariCP 中的 ConcurrentBag 并没有完全参考 C# 的实现，
下面我们来看看它是如何实现的。
ConcurrentBag 中最关键的属性有 4 个，分别是：用于存储所有的数据库连接的共享队列 sharedList、线程本地存储 threadList、
等待数据库连接的线程数 waiters 以及分配数据库连接的工具 handoffQueue。其中，handoffQueue 用的是 Java SDK 提供的 SynchronousQueue，
SynchronousQueue 主要用于线程之间传递数据。

//用于存储所有的数据库连接
CopyOnWriteArrayList<T> sharedList;
//线程本地存储中的数据库连接
ThreadLocal<List<Object>> threadList;
//等待数据库连接的线程数
AtomicInteger waiters;
//分配数据库连接的工具
SynchronousQueue<T> handoffQueue;

当线程池创建了一个数据库连接时，通过调用 ConcurrentBag 的 add() 方法加入到 ConcurrentBag 中，下面是 add() 方法的具体实现，
逻辑很简单，就是将这个连接加入到共享队列 sharedList 中，如果此时有线程在等待数据库连接，那么就通过 handoffQueue 将这个连接分配给等待的线程。
yield   noun
        1. 产量
        2. 屈服
        verb
        1. 产生
        2. 屈服
handoff  手递手传球

//将空闲连接添加到队列
void add(final T bagEntry){
  //加入共享队列
  sharedList.add(bagEntry);
  //如果有等待连接的线程，
  //则通过handoffQueue直接分配给等待的线程
  while (waiters.get() > 0 
    && bagEntry.getState() == STATE_NOT_IN_USE 
    && !handoffQueue.offer(bagEntry)) {
      yield();
  }
}

通过 ConcurrentBag 提供的 borrow() 方法，可以获取一个空闲的数据库连接，borrow() 的主要逻辑是：
首先查看线程本地存储是否有空闲连接，如果有，则返回一个空闲的连接；
如果线程本地存储中无空闲连接，则从共享队列中获取。
如果共享队列中也没有空闲的连接，则请求线程需要等待。

需要注意的是，线程本地存储中的连接是可以被其他线程窃取的，所以需要用 CAS 方法防止重复分配。在共享队列中获取空闲连接，
也采用了 CAS 方法防止重复分配。


T borrow(long timeout, final TimeUnit timeUnit){
  // 先查看线程本地存储是否有空闲连接
  final List<Object> list = threadList.get();
  for (int i = list.size() - 1; i >= 0; i--) {
    final Object entry = list.remove(i);
    final T bagEntry = weakThreadLocals 
      ? ((WeakReference<T>) entry).get() 
      : (T) entry;
    //线程本地存储中的连接也可以被窃取，
    //所以需要用CAS方法防止重复分配
    if (bagEntry != null 
      && bagEntry.compareAndSet(STATE_NOT_IN_USE, STATE_IN_USE)) {
      return bagEntry;
    }
  }

  // 线程本地存储中无空闲连接，则从共享队列中获取
  final int waiting = waiters.incrementAndGet();
  try {
    for (T bagEntry : sharedList) {
      //如果共享队列中有空闲连接，则返回
      if (bagEntry.compareAndSet(STATE_NOT_IN_USE, STATE_IN_USE)) {
        return bagEntry;
      }
    }
    //共享队列中没有连接，则需要等待
    timeout = timeUnit.toNanos(timeout);
    do {
      final long start = currentTime();
      final T bagEntry = handoffQueue.poll(timeout, NANOSECONDS);
      if (bagEntry == null 
        || bagEntry.compareAndSet(STATE_NOT_IN_USE, STATE_IN_USE)) {
          return bagEntry;
      }
      //重新计算等待时间
      timeout -= elapsedNanos(start);
    } while (timeout > 10_000);
    //超时没有获取到连接，返回null
    return null;
  } finally {
    waiters.decrementAndGet();
  }
}

释放连接需要调用 ConcurrentBag 提供的 requite() 方法，该方法的逻辑很简单，首先将数据库连接状态更改为 STATE_NOT_IN_USE，
之后查看是否存在等待线程，如果有，则分配给等待线程；如果没有，则将该数据库连接保存到线程本地存储里。

//释放连接
void requite(final T bagEntry){
  //更新连接状态
  bagEntry.setState(STATE_NOT_IN_USE);
  //如果有等待的线程，则直接分配给线程，无需进入任何队列
  for (int i = 0; waiters.get() > 0; i++) {
    if (bagEntry.getState() != STATE_NOT_IN_USE 
      || handoffQueue.offer(bagEntry)) {
        return;
    } else if ((i & 0xff) == 0xff) {
      parkNanos(MICROSECONDS.toNanos(10));
    } else {
      yield();
    }
  }
  //如果没有等待的线程，则进入线程本地存储
  final List<Object> threadLocalList = threadList.get();
  if (threadLocalList.size() < 50) {
    threadLocalList.add(weakThreadLocals 
      ? new WeakReference<>(bagEntry) 
      : bagEntry);
  }
}

总结
HiKariCP 中的 FastList 和 ConcurrentBag 这两个数据结构使用得非常巧妙，虽然实现起来并不复杂，但是对于性能的提升非常明显，
根本原因在于这两个数据结构适用于数据库连接池这个特定的场景。FastList 适用于逆序删除场景；而 ConcurrentBag 通过 ThreadLocal 做一次预分配，
避免直接竞争共享资源，非常适合池化资源的分配。
在实际工作中，我们遇到的并发问题千差万别，这时选择合适的并发数据结构就非常重要了。当然能选对的前提是对特定场景的并发特性有深入的了解，
只有了解到无谓的性能消耗在哪里，才能对症下药。


空知
线程本地的连接会被窃取
这个我觉得是因为 如果 Tl里面没有空闲的 会去 sharedList查找处于 Not_In_Use的连接 这个连接可能已经在其他TL里面存在了 所以就会出现线程T2
从sharedList获取到了 T1存在TL里面存放的没有使用的连接这种情况

拯救地球好累
支持高性能并发的软件通常首先会关注整体的并发设计模式，并发设计模式将影响整个软件的设计架构，比如RateLimiter并非采用较为复杂的生产者消费者模式，
而是用细粒度的互斥锁来实现令牌桶算法；Netty采用了Reactor模式而非阻塞的等待-通知机制的一些实现。对设计模式的考量应当根据实际需求先考虑线程分工，
再从避免共享的模式考虑到一些无锁的模式，再到细粒度的锁控制，再到复杂的同步和互斥模式。
从高性能队列和高性能数据连接池中，可以看到，性能的提高通常会从几方面着手（实际场景中应当测试优于猜测，再根据阿姆达尔定律从性能瓶颈处先着手）：
并发设计模式；内存分配算法；缓存利用率；GC情况（有GC的语言）；数据结构与算法的效率等。

阿姆达尔定律
该定律是指：系统中对某一部件采用更快执行方式所能获得的系统性能改进程度，取决于这种执行方式被使用的频率，或所占总执行时间的比例

晓杰
同问为什么线程本地的会被其他线程窃取，麻烦老师解释一下
作者回复: sharedlist和其他线程的threadlocal里有可能都有同一个连接，从前者取到连接，就相当于窃取了后者

多襄丸

老师， 我看文中提到的是调用requite()释放链接的时候将这个链接添加到本地存储中。
那我想问，如果不是调用requite()方法释放连接的情况下，这个连接第一次被放入threadlocal是什么时候啊？ 是第一次获取连接的时候吗？
作者回复: 只有requite的时候会放到threatlocal里

Simple life
看了第二遍，有个疑问，在一半WEB项目中，每次请求SPRING都新建一个线程服务，所以ThreadLocal中的线程并不能重用，这块性能提升就无效了，
都去COW中CAS获取可用线程了，CAS在高并发环境中表现并不好
作者回复: 多次请求之间是不共享任何数据都没有，性能提高只是一次请求范围之内

poordickey
这里讲的是连接池 但是很想知道一个数据库连接从拿到到归还的整个过程细节，从一个连接池拿到一个连接，connect之后，执行了SQL，并close了，
归还到线程池之后又是怎么一直和数据库保持连接的呢
作者回复: 并没有真的close，你调用了一个被hack的方法