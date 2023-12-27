
todo
如何提升技术能力

todo
okhttp的ResponseBody 如果没有调用close方法
1. 会有什么问题?
2. 为什么会产生连接泄漏
3. okhttp 是如何检测连接泄漏的
   写一套监控 Closeable对象 未被close的 系统

okhttp设置连接池
OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
clientBuilder.connectionPool(new ConnectionPool(5, 180, TimeUnit.SECONDS));  //连接池数量，连接池里面空闲连接的存活时间
//需要与后端复用时间统一，  假如客户端存活为5分钟，服务器3分钟
服务器连接死掉之后，客户端仍然认为是存活的进行复用，此时连接出错   Socket closed/java.net.SocketException: Connection reset


https://blog.csdn.net/jaynm/article/details/112002901
执行流程：
1 通过构建者构建出OkHttpClient对象,再通过newCall方法获得RealCall请求对象.
2 通过RealCall发起同步或异步请求,而决定是异步还是同步请求的是由线程分发器dispatcher来决定.
3 当发起同步请求时会将请求加入到同步队列中依次执行,所以会阻塞UI线程,需要开发者开启子线程执行，后面开始拦截器调用链
4 当发起异步请求时会创建一个线程池,并且判断请求队列是否大于最大请求队列64,请求主机数是否大于5,如果大于请求添加到异步等待队列中ArrayDeque,
   否则添加到异步执行队列,并执行任务，后面开始拦截器调用链
5 通过拦截器进行一下网络处理，然后发起请求


任务调度
其中Dispatcher有一个线程池,用于执行异步的请求.并且内部还维护了3个双向任务队列,分别是:准备异步执行的任务队列、正在异步执行的任务队列、
  正在同步执行的任务队列.
Dispatcher.kt
```
  private var executorServiceOrNull: ExecutorService? = null

  @get:Synchronized
  @get:JvmName("executorService") val executorService: ExecutorService
    //这个线程池是需要的时候才会被初始化
    get() {
      if (executorServiceOrNull == null) {
         //注意,该线程池没有核心线程,线程数量可以是Integer.MAX_VALUE个(相当于没有限制),超过60秒没干事就要被回收
        //SynchronousQueue 同步的队列， 
        executorServiceOrNull = ThreadPoolExecutor(0, Int.MAX_VALUE, 60, TimeUnit.SECONDS,
            SynchronousQueue(), threadFactory("$okHttpName Dispatcher", false))
      }
      return executorServiceOrNull!!
    }
  /** Ready async calls in the order they'll be run. */
  private val readyAsyncCalls = ArrayDeque<AsyncCall>()

  /** Running asynchronous calls. Includes canceled calls that haven't finished yet. */
  private val runningAsyncCalls = ArrayDeque<AsyncCall>()

  /** Running synchronous calls. Includes canceled calls that haven't finished yet. */
  private val runningSyncCalls = ArrayDeque<RealCall>()
```
任务调度器会将其存入待执行的请求队列(上面提到的readyAsyncCalls)中,
然后条件允许的话再加入到运行中的请求队列(runningAsyncCalls)中,然后将这个请求放到任务调度器中的线程池中进行消费.下面是详细代码
执行中的任务最多是64个
Dispatcher.kt
```
 //添加call到readyAsyncCalls队列
    internal fun enqueue(call: AsyncCall) {
    synchronized(this) {
      readyAsyncCalls.add(call)

      // Mutate the AsyncCall so that it shares the AtomicInteger of an existing running call to
      // the same host.
      if (!call.call.forWebSocket) {
        val existingCall = findExistingCallWithHost(call.host)
        if (existingCall != null) call.reuseCallsPerHostFrom(existingCall)
      }
    }
    promoteAndExecute()
  }
  
   //将call从readyAsyncCalls中移除并添加到runningAsyncCalls，然后在线程池中执行
   private fun promoteAndExecute(): Boolean {
    this.assertThreadDoesntHoldLock()
    //存储运行的call
    val executableCalls = mutableListOf<AsyncCall>()
    val isRunning: Boolean
    synchronized(this) {
      //从待执行队列中取出来
      val i = readyAsyncCalls.iterator()
      while (i.hasNext()) {
        val asyncCall = i.next()
        //如果正在执行的任务>=64  那么就算了,先缓一缓
        if (runningAsyncCalls.size >= this.maxRequests) break // Max capacity.
        if (asyncCall.callsPerHost.get() >= this.maxRequestsPerHost) continue // Host max capacity.

        i.remove()
        asyncCall.callsPerHost.incrementAndGet()
        //加入到运行队列中
        executableCalls.add(asyncCall)       
        runningAsyncCalls.add(asyncCall)
      }
      isRunning = runningCallsCount() > 0
    }

    for (i in 0 until executableCalls.size) {
       //一个个地开始执行    executorService方法是获取线程池
      val asyncCall = executableCalls[i]
      asyncCall.executeOn(executorService)
    }

    return isRunning
  }
```

okhttp的拦截器实现不同的功能与扩展
添加用户定义的拦截器   预处理，拿到响应结果
RetryAndFollowUpInterceptor 负责请求的重定向操作，用于处理网络请求中，请求失败后的重试机制。   重定向的最大次数20
BridgeInterceptor 主要是添加一些header,request构建,response构建  添加gzip压缩,Keep-Alive  支持gzip的响应使用Okio解析
   gzip使用了huffman和LZ77两个压缩算法 todo 压缩算法
CacheInterceptor 负责缓存  
  如果有缓存使用缓存，没有缓存将新的response进行缓存
     如果根据缓存策略strategy禁止使用网络，且有缓存则直接使用缓存
     如果本地有缓存，服务器返回304状态码（说明缓存还没过期或服务器资源没修改）
ConnectInterceptor 打开与目标服务器的连接    从ConnectionPool复用一个连接或者新建一个
这里还有一个网络拦截器,也是可以用户自定义的  拿到访问前的请求
CallServerInterceptor 最后一个拦截器,负责请求网络  最终使用socket进行请求头，请求体的写入，然后进行接收


拦截器的构建
```
 internal fun getResponseWithInterceptorChain(): Response {
    // Build a full stack of interceptors.
    //用来盛放所有的拦截器的
    val interceptors = mutableListOf<Interceptor>()
    //1. 添加用户定义的拦截器
    interceptors += client.interceptors
    //2. 添加一些OkHttp自带的拦截器
    interceptors += RetryAndFollowUpInterceptor(client)
    interceptors += BridgeInterceptor(client.cookieJar)
    interceptors += CacheInterceptor(client.cache)
    interceptors += ConnectInterceptor
    if (!forWebSocket) {
      //这里还有一个网络拦截器,也是可以用户自定义的
      interceptors += client.networkInterceptors
    }
    //最终访问服务器的拦截器
    interceptors += CallServerInterceptor(forWebSocket)

   //3. 将拦截器,当前拦截器索引等传入Interceptor.Chain
    val chain = RealInterceptorChain(
        call = this,
        interceptors = interceptors,
        index = 0,
        exchange = null,
        request = originalRequest,
        connectTimeoutMillis = client.connectTimeoutMillis,
        readTimeoutMillis = client.readTimeoutMillis,
        writeTimeoutMillis = client.writeTimeoutMillis
    )

    var calledNoMoreExchanges = false
    try {
      //4. 请求访问下一个拦截器
      val response = chain.proceed(originalRequest)
      ...
      return response
    } catch (e: IOException) {
       ...
    }
  }
```
proceed过程
```
override fun proceed(request: Request): Response {
    check(index < interceptors.size)
    calls++
    ...
    // Call the next interceptor in the chain.
    //调用下一个interceptor.注意到,这里的index索引+1了的,所以是下一个interceptor
    val next = copy(index = index + 1, request = request)
    //当前interceptor
    val interceptor = interceptors[index]
    //调用interceptor的intercept方法
    val response = interceptor.intercept(next) ?: throw NullPointerException(
        "interceptor $interceptor returned null")
    ...
    return response
  }
```



底层使用Socket连接，RealConnection负责封装socket，可以进行连接复用  mutableListOf
空闲连接keepAlive时间超过5分钟，或者空闲连接数超过5个
对链接是否要回收复用采用引用计数法
RealConnection中的mutableListOf<Reference<RealCall>>()
RealCall.kt
```
  相当于acquire操作 增加计数
  fun acquireConnectionNoEvents(connection: RealConnection) {
    ...
    //CallReference实现WeakReference接口
    connection.calls.add(CallReference(this, callStackTrace))
  }
  相当于release操作  减少计数
  internal fun releaseConnectionNoEvents(): Socket? {
    ...
    val index = calls.indexOfFirst { it.get() == this@RealCall }
     ...
    calls.removeAt(index)
    ...
    return null
  }
```

判断是否是android平台
```
val isAndroid: Boolean
get() = "Dalvik" == System.getProperty("java.vm.name")
```
