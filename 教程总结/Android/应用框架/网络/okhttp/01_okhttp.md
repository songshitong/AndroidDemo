https://github.com/xfhy/Android-Notes/blob/master/Blogs/Android/%E4%B8%89%E6%96%B9%E5%BA%93%E5%8E%9F%E7%90%86/OkHttp3_%E5%8E%9F%E7%90%86%E6%8E%A2%E7%A9%B6.md
version: 4.9.3

1. 前言
   Okhttp3 俨然已成为Android的主流网络请求开源框架,它的设计非常巧妙,而且非常灵活,功能强大.它有如下默认特性:

支持HTTP/2，允许所有同一个主机地址的请求共享同一个Socket连接
连接池减少请求延时
透明的GZIP压缩减少响应数据的大小
缓存响应内容，避免一些完全重复的请求
现在的Android项目基本上都是以OkHttp来进行高效的网络请求.当然,在使用的同时我们需要去研究它的底层实现,从而让我们写出更好的代码.


2. 基本使用
   这里简单介绍2种,GET和POST.推荐让 OkHttpClient 保持单例，用同一个 OkHttpClient 实例来执行你的所有请求，
   因为每一个 OkHttpClient 实例都拥有自己的连接池和线程池，重用这些资源可以减少延时和节省资源，如果为每个请求创建一个 OkHttpClient实例，
   显然就是一种资源的浪费。
   

1. 使用GET方式请求
同步call  线程会进行阻塞，直到响应回调
```
Response response = client.newCall(request).execute()
```
异步call
```
public static final String URL = "http://www.baidu.com";
private OkHttpClient mOkHttpClient = new OkHttpClient();
private final Request mRequest = new Request.Builder().url(URL).build();
 mOkHttpClient.newCall(mRequest)
            //异步请求
            .enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    Log.w(TAG, "onResponse: " + response.body().string());
                }
            });
```
2. 使用POST请求
```
public static final String URL = "https://api.github.com/markdown/raw";
private OkHttpClient mOkHttpClient = new OkHttpClient.Builder()
        .build();
MediaType mMediaType = MediaType.parse("text/x-markdown; charset=utf-8");
String requestBody = "I am xfhy.";
private final Request mRequest = new Request.Builder()
        .url(URL)
        .post(RequestBody.create(mMediaType, requestBody))
        .build();

//每一个Call（其实现是RealCall）只能执行一次，否则会报异常
    mOkHttpClient.newCall(mRequest).enqueue(new Callback() {
        @Override
        public void onFailure(Call call, IOException e) {
            e.printStackTrace();
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            Log.w(TAG, "onResponse: " + response.body().string());
        }
    });
```


3. interceptor 拦截器-精髓
   使用OkHttp3请求网络还是比较简单,而且异步请求也比较轻松.

3.1 构建OkHttpClient
正如名字所描述的,OkHttpClient像是一个请求网络的客户端.它内部有很多很多的配置信息(支持协议、任务调度器、连接池、超时时间等),
 通过构造器模式初始化的这些配置信息.(这里穿插一下,正如你所看到的这种一个类里面很多很多属性需要初始化的,一般就用构造器模式)
```
OkHttpClient.kt
{
constructor() : this(Builder())
}

class Builder constructor() {
    //任务调度器
    internal var dispatcher: Dispatcher = Dispatcher()
    //连接池
    internal var connectionPool: ConnectionPool = ConnectionPool()
    internal val interceptors: MutableList<Interceptor> = mutableListOf()
    internal val networkInterceptors: MutableList<Interceptor> = mutableListOf()
    internal var eventListenerFactory: EventListener.Factory = EventListener.NONE.asFactory()
    internal var retryOnConnectionFailure = true
    internal var authenticator: Authenticator = Authenticator.NONE
    internal var followRedirects = true
    internal var followSslRedirects = true
    internal var cookieJar: CookieJar = CookieJar.NO_COOKIES
    internal var cache: Cache? = null
    internal var dns: Dns = Dns.SYSTEM
    internal var proxy: Proxy? = null
    internal var proxySelector: ProxySelector? = null
    internal var proxyAuthenticator: Authenticator = Authenticator.NONE
    internal var socketFactory: SocketFactory = SocketFactory.getDefault()
    internal var sslSocketFactoryOrNull: SSLSocketFactory? = null
    internal var x509TrustManagerOrNull: X509TrustManager? = null
    internal var connectionSpecs: List<ConnectionSpec> = DEFAULT_CONNECTION_SPECS
    //支持的协议
    internal var protocols: List<Protocol> = DEFAULT_PROTOCOLS
    internal var hostnameVerifier: HostnameVerifier = OkHostnameVerifier
    internal var certificatePinner: CertificatePinner = CertificatePinner.DEFAULT
    internal var certificateChainCleaner: CertificateChainCleaner? = null
    internal var callTimeout = 0
     //超时时间
    internal var connectTimeout = 10_000
    internal var readTimeout = 10_000
    internal var writeTimeout = 10_000
    internal var pingInterval = 0
    internal var minWebSocketMessageToCompress = RealWebSocket.DEFAULT_MINIMUM_DEFLATE_SIZE
    internal var routeDatabase: RouteDatabase? = null
 }
```

其中Dispatcher有一个线程池,用于执行异步的请求.并且内部还维护了3个双向任务队列,分别是:准备异步执行的任务队列、正在异步执行的任务队列、正在同步执行的任务队列.
Dispatcher.kt
```
  private var executorServiceOrNull: ExecutorService? = null

  @get:Synchronized
  @get:JvmName("executorService") val executorService: ExecutorService
    //这个线程池是需要的时候才会被初始化
    get() {
      if (executorServiceOrNull == null) {
         //注意,该线程池没有核心线程,线程数量可以是Integer.MAX_VALUE个(相当于没有限制),超过60秒没干事就要被回收
        executorServiceOrNull = ThreadPoolExecutor(0, Int.MAX_VALUE, 60, TimeUnit.SECONDS,
            SynchronousQueue(), threadFactory("$okHttpName Dispatcher", false))
      }
      return executorServiceOrNull!!
    }
    
  //SynchronousQueue 是一个没有数据缓冲的BlockingQueue， 生产者线程对其的插入操作put必须等待消费者的移除操作take，反过来也一样
  //todo  SynchronousQueue
  //todo   ArrayDeque的实现
  /** Ready async calls in the order they'll be run. */
  private val readyAsyncCalls = ArrayDeque<AsyncCall>()

  /** Running asynchronous calls. Includes canceled calls that haven't finished yet. */
  private val runningAsyncCalls = ArrayDeque<AsyncCall>()

  /** Running synchronous calls. Includes canceled calls that haven't finished yet. */
  private val runningSyncCalls = ArrayDeque<RealCall>()
  
```


3.2 构建Request
Request感觉就是一个请求的封装.它里面封装了url、method、header、body,该有的都有了.而且它也是用构造器模式来构建的,它默认的请求方式是GET
Request.kt
```
class Request internal constructor(
  @get:JvmName("url") val url: HttpUrl,
  @get:JvmName("method") val method: String,
  @get:JvmName("headers") val headers: Headers,
  @get:JvmName("body") val body: RequestBody?,
  internal val tags: Map<Class<*>, Any>
) {

}

 open class Builder {
    internal var url: HttpUrl? = null
    internal var method: String
    internal var headers: Headers.Builder
    internal var body: RequestBody? = null

    /** A mutable map of tags, or an immutable empty map if we don't have any. */
    internal var tags: MutableMap<Class<*>, Any> = mutableMapOf()

    constructor() {
      //默认GET
      this.method = "GET"
      this.headers = Headers.Builder()
    }
```

3.3 开始请求
我们进入mOkHttpClient的newCall方法,它构造的是一个Call对象,实际上是一个RealCall
OkHttpClient.kt
```
override fun newCall(request: Request): Call = RealCall(this, request, forWebSocket = false)
```

RealCall#enqueue(Callback)
所以示例中的enqueue实际上是RealCall中的方法
RealCall.kt
```
  override fun enqueue(responseCallback: Callback) {
    check(executed.compareAndSet(false, true)) { "Already Executed" }
    //回调监听的callStart 
    callStart()
    //将AsyncCall传入任务调度器
    client.dispatcher.enqueue(AsyncCall(responseCallback))
  }
  
  private fun callStart() {
    this.callStackTrace = Platform.get().getStackTraceForCloseable("response.body().close()")
    eventListener.callStart(this)
  }
```

将AsyncCall(这个我们稍后再说)传入任务调度器,任务调度器会将其存入待执行的请求队列(上面提到的readyAsyncCalls)中,
然后条件允许的话再加入到运行中的请求队列(runningAsyncCalls)中,然后将这个请求放到任务调度器中的线程池中进行消费.下面是详细代码
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
        //域名的call大于5个退出
        if (asyncCall.callsPerHost.get() >= this.maxRequestsPerHost) continue // Host max capacity.

        i.remove()
        asyncCall.callsPerHost.incrementAndGet() //增加计数
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

上面我们提到了很多次AsyncCall,它其实是一个RealCall的非静态内部类,所以能直接访问到RealCall的属性啥的,方便.同时,
  AsyncCall实现了Runnable.
RealCall.kt
```
internal inner class AsyncCall(
    private val responseCallback: Callback
  ) : Runnable {
   override fun run() {
      //设置了自己线程的名字,实属方便管理.
      threadName("OkHttp ${redactedUrl()}") {
        var signalledCallback = false
        //todo okio的AsyncTimeout
        timeout.enter()
        try {
          // 通过拦截器链条,获取最终的网络请求结果
          val response = getResponseWithInterceptorChain()
          //标记已执行   不能再执行第二次了
          signalledCallback = true
          //将结果回调给调用处
          responseCallback.onResponse(this@RealCall, response)
        } catch (e: IOException) {
          if (signalledCallback) {
            // Do not signal the callback twice!
            Platform.get().log("Callback failure for ${toLoggableString()}", Platform.INFO, e)
          } else {
            responseCallback.onFailure(this@RealCall, e)
          }
        } catch (t: Throwable) {
          cancel()
          if (!signalledCallback) {
            val canceledException = IOException("canceled due to $t")
            canceledException.addSuppressed(t)
            responseCallback.onFailure(this@RealCall, canceledException)
          }
          throw t
        } finally {
          client.dispatcher.finished(this)
        }
      }
    }
    
    //在线程池中执行这个AsyncCall
    fun executeOn(executorService: ExecutorService) {
      client.dispatcher.assertThreadDoesntHoldLock()

      var success = false
      try {
        executorService.execute(this)
        success = true
      } catch (e: RejectedExecutionException) {
        val ioException = InterruptedIOException("executor rejected")
        ioException.initCause(e)
        noMoreExchanges(ioException)
        responseCallback.onFailure(this@RealCall, ioException)
      } finally {
        if (!success) {
          client.dispatcher.finished(this) // This call is no longer running!
        }
      }
    }
  
  }
```
开始了,开始了,重点来了,通过getResponseWithInterceptorChain方法这条拦截器链路可以获取到网络请求的结果.然后我们通过CallBack接口回调回调用处.

在开始之前,大家先看两张图,这张图是整个拦截器的流程,也是OkHttp的精华,设计之巧妙.
android_三方库_okhttp_拦截器流程.png
android_三方库_okhttp_拦截器调用流程.png
//前面几个拦截器都调用了chain.proceed方法，获取下一个拦截器的处理结果，最后一个拦截器返回最终的response


从上面的代码也可以看到,getResponseWithInterceptorChain方法是获取到了网络请求的最终数据的.紧接着根据我画了两张图,
这两张图主要是描绘了从getResponseWithInterceptorChain进去之后发生的事,它内部会串行的执行一些特定的拦截器(interceptors),
每个拦截器负责一个特殊的职责.最后那个拦截器负责请求服务器,然后服务器返回了数据再根据这个拦截器的顺序逆序返回回去,最终就得到了网络数据.

下面先简单介绍一下这些拦截器,方便后面的源码梳理
添加用户定义的拦截器
RetryAndFollowUpInterceptor 负责请求的重定向操作，用于处理网络请求中，请求失败后的重试机制。
BridgeInterceptor 主要是添加一些header
CacheInterceptor 负责缓存
ConnectInterceptor 打开与目标服务器的连接
这里还有一个网络拦截器,也是可以用户自定义的
CallServerInterceptor 最后一个拦截器,负责请求网络


3.4 进入拦截器调用链
有了上面的简单介绍,我们直接进入getResponseWithInterceptorChain方法一探究竟.
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
      if (isCanceled()) {
        response.closeQuietly()
        throw IOException("Canceled")
      }
      return response
    } catch (e: IOException) {
      calledNoMoreExchanges = true
      throw noMoreExchanges(e) as Throwable
    } finally {
      if (!calledNoMoreExchanges) {
        noMoreExchanges(null)
      }
    }
  }
```

可以看到,OkHttp这个拦截器链的大体流程,最开始是用户自定义的拦截器,然后才是OkHttp自己默认的拦截器(需要注意的是,最后一个拦截器是CallServerInterceptor).
然后将拦截器集合和当前拦截器的索引等数据传入RealInterceptorChain,调用RealInterceptorChain对象的proceed,并最终得到执行结果.
看来逻辑在RealInterceptorChain的proceed方法内部
RealInterceptorChain.kt
```
class RealInterceptorChain(
  internal val call: RealCall,
  private val interceptors: List<Interceptor>,
  private val index: Int,
  internal val exchange: Exchange?,
  internal val request: Request,
  internal val connectTimeoutMillis: Int,
  internal val readTimeoutMillis: Int,
  internal val writeTimeoutMillis: Int
) : Interceptor.Chain {

 private var calls: Int = 0
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

  internal fun copy(
    index: Int = this.index,
    exchange: Exchange? = this.exchange,
    request: Request = this.request,
    connectTimeoutMillis: Int = this.connectTimeoutMillis,
    readTimeoutMillis: Int = this.readTimeoutMillis,
    writeTimeoutMillis: Int = this.writeTimeoutMillis
  ) = RealInterceptorChain(call, interceptors, index, exchange, request, connectTimeoutMillis,
      readTimeoutMillis, writeTimeoutMillis)
}
```
//todo 拦截器变形
在proceed方法里面主要是将下一个拦截器的RealInterceptorChain构建出来,然后传入当前拦截器的intercept方法里面,
 方便在intercept方法里面执行下一个RealInterceptorChain的proceed方法.intercept方法返回的是获取数据之后的Response.

Interceptor接口
```
fun interface Interceptor {
  fun intercept(chain: Chain): Response

  companion object {
    /**
     * Constructs an interceptor for a lambda. This compact syntax is most useful for inline
     * interceptors.
     *
     * ```
     * val interceptor = Interceptor { chain: Interceptor.Chain ->
     *     chain.proceed(chain.request())
     * }
     * ```
     */
    inline operator fun invoke(crossinline block: (chain: Chain) -> Response): Interceptor =
      Interceptor { block(it) }
  }

  interface Chain {
    fun request(): Request

    @Throws(IOException::class)
    fun proceed(request: Request): Response

    fun connection(): Connection?

    fun call(): Call

    fun connectTimeoutMillis(): Int
    ...
  }
}
```

下面进入intercept方法内部,Interceptor其实是一个接口,然后所有的拦截器都实现了这个接口Interceptor.如果没有用户自定义的拦截器,
那么第一个拦截器就是RetryAndFollowUpInterceptor

RetryAndFollowUpInterceptor#intercept
```
override fun intercept(chain: Interceptor.Chain): Response {
    val realChain = chain as RealInterceptorChain
    var request = chain.request
    val call = realChain.call
    var followUpCount = 0
    var priorResponse: Response? = null
    var newExchangeFinder = true
    var recoveredFailures = listOf<IOException>()
    //死循环  直到达到重定向的最大次数20
    while (true) {
      call.enterNetworkInterceptorExchange(request, newExchangeFinder)

      var response: Response
      var closeActiveExchange = true
      try {
        if (call.isCanceled()) {
          throw IOException("Canceled")
        }

        try {
        //调用下一个拦截器
          response = realChain.proceed(request)
          newExchangeFinder = true
        } catch (e: RouteException) {
           //下面是一些失败,然后又重新请求的代码
          // The attempt to connect via a route failed. The request will not have been sent.
          //确定是否可以恢复，不能恢复进行重试或抛出异常
          if (!recover(e.lastConnectException, call, request, requestSendStarted = false)) {
            throw e.firstConnectException.withSuppressed(recoveredFailures)
          } else {
            recoveredFailures += e.firstConnectException
          }
          newExchangeFinder = false
          continue
        } catch (e: IOException) {
          // An attempt to communicate with a server failed. The request may have been sent.
          if (!recover(e, call, request, requestSendStarted = e !is ConnectionShutdownException)) {
            throw e.withSuppressed(recoveredFailures)
          } else {
            recoveredFailures += e
          }
          newExchangeFinder = false
          continue
        }

        // Attach the prior response if it exists. Such responses never have a body.
        if (priorResponse != null) {
          response = response.newBuilder()
              .priorResponse(priorResponse.newBuilder()
                  .body(null)
                  .build())
              .build()
        }

        val exchange = call.interceptorScopedExchange
        val followUp = followUpRequest(response, exchange)

        if (followUp == null) {
          if (exchange != null && exchange.isDuplex) {
            call.timeoutEarlyExit()
          }
          closeActiveExchange = false
          return response
        }

        val followUpBody = followUp.body
        if (followUpBody != null && followUpBody.isOneShot()) {
          closeActiveExchange = false
          return response
        }

        response.body?.closeQuietly()
        //20次最大重定向
        if (++followUpCount > MAX_FOLLOW_UPS) {
          throw ProtocolException("Too many follow-up requests: $followUpCount")
        }

        request = followUp
        priorResponse = response
      } finally {
        call.exitNetworkInterceptorExchange(closeActiveExchange)
      }
    }
  }

private fun recover(
    e: IOException,
    call: RealCall,
    userRequest: Request,
    requestSendStarted: Boolean
  ): Boolean {
    // The application layer has forbidden retries. //不允许重试
    if (!client.retryOnConnectionFailure) return false

    // We can't send the request body again.
    if (requestSendStarted && requestIsOneShot(e, userRequest)) return false

    // This exception is fatal.
    if (!isRecoverable(e, requestSendStarted)) return false

    // No more routes to attempt.
    if (!call.retryAfterFailure()) return false

    // For failure recovery, use the same route selector with a new connection.
    return true
  }  


 //ProtocolException,CertificateException,SSLPeerUnverifiedException不进行重试
 private fun isRecoverable(e: IOException, requestSendStarted: Boolean): Boolean {
    // If there was a protocol problem, don't recover.
    if (e is ProtocolException) {
      return false
    }

    // If there was an interruption don't recover, but if there was a timeout connecting to a route
    // we should try the next route (if there is one).
    if (e is InterruptedIOException) {
      return e is SocketTimeoutException && !requestSendStarted
    }

    // Look for known client-side or negotiation errors that are unlikely to be fixed by trying
    // again with a different route.
    if (e is SSLHandshakeException) {
      // If the problem was a CertificateException from the X509TrustManager,
      // do not retry.
      if (e.cause is CertificateException) {
        return false
      }
    }
    if (e is SSLPeerUnverifiedException) {
      // e.g. a certificate pinning error.
      return false
    }
    // An example of one we might want to retry with a different route is a problem connecting to a
    // proxy and would manifest as a standard IOException. Unless it is one we know we should not
    // retry, we return true and try a new route.
    return true
  }
  
```
//todo 看一下重定向和重试的实现

RetryAndFollowUpInterceptor主要是负责错误处理,以及重定向.当然重定向是有最大次数的,OkHttp规定是20次.

RetryAndFollowUpInterceptor执行proceed方法是来到了BridgeInterceptor,它是一个连接桥.添加了很多header.request构建，response构建
BridgeInterceptor#intercept
```
override fun intercept(chain: Interceptor.Chain): Response {
    val userRequest = chain.request()
    val requestBuilder = userRequest.newBuilder()
    //进行header的包装
    val body = userRequest.body
    if (body != null) {
      val contentType = body.contentType()
      if (contentType != null) {
        requestBuilder.header("Content-Type", contentType.toString())
      }

      val contentLength = body.contentLength()
      if (contentLength != -1L) {
        requestBuilder.header("Content-Length", contentLength.toString())
        requestBuilder.removeHeader("Transfer-Encoding")
      } else {
        requestBuilder.header("Transfer-Encoding", "chunked")
        requestBuilder.removeHeader("Content-Length")
      }
    }

    if (userRequest.header("Host") == null) {
      requestBuilder.header("Host", userRequest.url.toHostHeader())
    }
    //添加默认Keep-Alive
    if (userRequest.header("Connection") == null) {
      requestBuilder.header("Connection", "Keep-Alive")
    }
     //添加Accept-Encoding：gzip   添加默认gzip
    // If we add an "Accept-Encoding: gzip" header field we're responsible for also decompressing
    // the transfer stream.
    var transparentGzip = false
    if (userRequest.header("Accept-Encoding") == null && userRequest.header("Range") == null) {
      transparentGzip = true
      requestBuilder.header("Accept-Encoding", "gzip")
    }
    //创建OkhttpClient配置的cookieJar
    val cookies = cookieJar.loadForRequest(userRequest.url)
    if (cookies.isNotEmpty()) {
      requestBuilder.header("Cookie", cookieHeader(cookies))
    }

    //设置默认的userAgent  "okhttp/${OkHttp.VERSION}"
    if (userRequest.header("User-Agent") == null) {
      requestBuilder.header("User-Agent", userAgent)
    }
    //执行下一个Interceptor
    val networkResponse = chain.proceed(requestBuilder.build())

    cookieJar.receiveHeaders(userRequest.url, networkResponse.headers)

    val responseBuilder = networkResponse.newBuilder()
        .request(userRequest)
    //先判断服务器是否支持gzip压缩,支持则交给Okio处理
    if (transparentGzip &&
        "gzip".equals(networkResponse.header("Content-Encoding"), ignoreCase = true) &&
        networkResponse.promisesBody()) {
      val responseBody = networkResponse.body
      if (responseBody != null) {
        val gzipSource = GzipSource(responseBody.source())
        val strippedHeaders = networkResponse.headers.newBuilder()
            .removeAll("Content-Encoding")
            .removeAll("Content-Length")
            .build()
        responseBuilder.headers(strippedHeaders)
        val contentType = networkResponse.header("Content-Type")
        responseBuilder.body(RealResponseBody(contentType, -1L, gzipSource.buffer()))
      }
    }
    //最后将结果返回
    return responseBuilder.build()
  }
```


BridgeInterceptor就跟它的名字那样，它是一个连接桥.它负责把用户构造的请求转换成发送给服务器的请求,就是添加了不少的header,其中还有gzip等.

BridgeInterceptor的下一个拦截器是CacheInterceptor
CacheInterceptor#intercept
```
override fun intercept(chain: Interceptor.Chain): Response {
    val call = chain.call()
    //如果配置了缓存：优先从缓存中读取Response
    val cacheCandidate = cache?.get(chain.request())

    val now = System.currentTimeMillis()
    //缓存策略，该策略通过某种规则来判断缓存是否有效
    val strategy = CacheStrategy.Factory(now, chain.request(), cacheCandidate).compute()
    //为null代表不使用网络, request==null
    val networkRequest = strategy.networkRequest
    //缓存策略的Response
    val cacheResponse = strategy.cacheResponse

    cache?.trackResponse(strategy)
    val listener = (call as? RealCall)?.eventListener ?: EventListener.NONE

    if (cacheCandidate != null && cacheResponse == null) {
      // The cache candidate wasn't applicable. Close it.
      cacheCandidate.body?.closeQuietly()
    }
    //如果根据缓存策略strategy禁止使用网络，并且缓存无效，直接返回空的Response
    // If we're forbidden from using the network and the cache is insufficient, fail.
    if (networkRequest == null && cacheResponse == null) {
      return Response.Builder()
          .request(chain.request())
          .protocol(Protocol.HTTP_1_1)
          .code(HTTP_GATEWAY_TIMEOUT)
          .message("Unsatisfiable Request (only-if-cached)")
          .body(EMPTY_RESPONSE)
          .sentRequestAtMillis(-1L)
          .receivedResponseAtMillis(System.currentTimeMillis())
          .build().also {
            listener.satisfactionFailure(call, it)
          }
    }
   //如果根据缓存策略strategy禁止使用网络，且有缓存则直接使用缓存
    // If we don't need the network, we're done.
    if (networkRequest == null) {
      return cacheResponse!!.newBuilder()
          .cacheResponse(stripBody(cacheResponse))
          .build().also {
            listener.cacheHit(call, it)
          }
    }

    if (cacheResponse != null) {
      listener.cacheConditionalHit(call, cacheResponse)
    } else if (cache != null) {
      listener.cacheMiss(call)
    }
    //需要网络
    var networkResponse: Response? = null
    try {
      //执行下一个拦截器,发起网路请求
      networkResponse = chain.proceed(networkRequest)
    } finally {
      // If we're crashing on I/O or otherwise, don't leak the cache body.
      if (networkResponse == null && cacheCandidate != null) {
        cacheCandidate.body?.closeQuietly()
      }
    }
    //本地有缓存
    // If we have a cache response too, then we're doing a conditional get.
    if (cacheResponse != null) {
       //并且服务器返回304状态码（说明缓存还没过期或服务器资源没修改）   
      if (networkResponse?.code == HTTP_NOT_MODIFIED) {
        //使用缓存数据
        val response = cacheResponse.newBuilder()
            .headers(combine(cacheResponse.headers, networkResponse.headers))
            .sentRequestAtMillis(networkResponse.sentRequestAtMillis)
            .receivedResponseAtMillis(networkResponse.receivedResponseAtMillis)
            .cacheResponse(stripBody(cacheResponse))
            .networkResponse(stripBody(networkResponse))
            .build()

        networkResponse.body!!.close()

        // Update the cache after combining headers but before stripping the
        // Content-Encoding header (as performed by initContentStream()).
        cache!!.trackConditionalCacheHit()
        cache.update(cacheResponse, response)
        return response.also {
          listener.cacheHit(call, it)
        }
      } else {
        cacheResponse.body?.closeQuietly()
      }
    }
   //如果网络资源已经修改：使用网络响应返回的最新数据
    val response = networkResponse!!.newBuilder()
        .cacheResponse(stripBody(cacheResponse))
        .networkResponse(stripBody(networkResponse))
        .build()
    //将最新的数据缓存起来,同时返回response
    if (cache != null) {
      if (response.promisesBody() && CacheStrategy.isCacheable(response, networkRequest)) {
        // Offer this request to the cache.
        val cacheRequest = cache.put(response)
        return cacheWritingResponse(cacheRequest, response).also {
          if (cacheResponse != null) {
            // This will log a conditional cache miss only.
            listener.cacheMiss(call)
          }
        }
      }

      if (HttpMethod.invalidatesCache(networkRequest.method)) {
        try {
          cache.remove(networkRequest)
        } catch (_: IOException) {
          // The cache cannot be written.
        }
      }
    }
    //返回最新的数据
    return response
  }
```
//todo cache相关操作
如果有缓存使用缓存，没有缓存将新的response进行缓存
  1 如果根据缓存策略strategy禁止使用网络，且有缓存则直接使用缓存
  2 如果本地有缓存，服务器返回304状态码（说明缓存还没过期或服务器资源没修改）

CacheInterceptor是进行一些缓存上面的处理,接下来是ConnectInterceptor
ConnectInterceptor#intercept
```
override fun intercept(chain: Interceptor.Chain): Response {
    val realChain = chain as RealInterceptorChain
    //从pool找到或新建一个conntion用于接下来的网络请求和响应
    val exchange = realChain.call.initExchange(chain)
    val connectedChain = realChain.copy(exchange = exchange)
    //执行下一个拦截器
    return connectedChain.proceed(realChain.request)
  }
```


ConnectInterceptor的下一个拦截器就是最后一个拦截器CallServerInterceptor了.
CallServerInterceptor#intercept
```
override fun intercept(chain: Interceptor.Chain): Response {
    val realChain = chain as RealInterceptorChain
    //上一个拦截器已经知道了一个用于请求的connection
    val exchange = realChain.exchange!!
    val request = realChain.request
    val requestBody = request.body
    val sentRequestMillis = System.currentTimeMillis()
    //整理请求头并写入
    exchange.writeRequestHeaders(request)

    var invokeStartEvent = true
    var responseBuilder: Response.Builder? = null
    if (HttpMethod.permitsRequestBody(request.method) && requestBody != null) {
      // If there's a "Expect: 100-continue" header on the request, wait for a "HTTP/1.1 100
      // Continue" response before transmitting the request body. If we don't get that, return
      // what we did get (such as a 4xx response) without ever transmitting the request body.
      if ("100-continue".equals(request.header("Expect"), ignoreCase = true)) {
        exchange.flushRequest()
        responseBuilder = exchange.readResponseHeaders(expectContinue = true)
        exchange.responseHeadersStart()
        invokeStartEvent = false
      }
      if (responseBuilder == null) {
        if (requestBody.isDuplex()) {
          // Prepare a duplex body so that the application can send a request body later.
          exchange.flushRequest()
          val bufferedRequestBody = exchange.createRequestBody(request, true).buffer()
          requestBody.writeTo(bufferedRequestBody)
        } else {
          // Write the request body if the "Expect: 100-continue" expectation was met.
          val bufferedRequestBody = exchange.createRequestBody(request, false).buffer()
          requestBody.writeTo(bufferedRequestBody)
          bufferedRequestBody.close()
        }
      } else {
        exchange.noRequestBody()
        if (!exchange.connection.isMultiplexed) {
          // If the "Expect: 100-continue" expectation wasn't met, prevent the HTTP/1 connection
          // from being reused. Otherwise we're still obligated to transmit the request body to
          // leave the connection in a consistent state.
          exchange.noNewExchangesOnConnection()
        }
      }
    } else {
      exchange.noRequestBody()
    }

    if (requestBody == null || !requestBody.isDuplex()) {
      //发送最终的请求
      exchange.finishRequest()
    }
    if (responseBuilder == null) {
      //响应头
      responseBuilder = exchange.readResponseHeaders(expectContinue = false)!!
      if (invokeStartEvent) {
        exchange.responseHeadersStart()
        invokeStartEvent = false
      }
    }
    var response = responseBuilder
        .request(request)
        .handshake(exchange.connection.handshake())
        .sentRequestAtMillis(sentRequestMillis)
        .receivedResponseAtMillis(System.currentTimeMillis())
        .build()
    var code = response.code
    if (code == 100) {
      // Server sent a 100-continue even though we did not request one. Try again to read the actual
      // response status.
      responseBuilder = exchange.readResponseHeaders(expectContinue = false)!!
      if (invokeStartEvent) {
        exchange.responseHeadersStart()
      }
      //
      response = responseBuilder
          .request(request)
          .handshake(exchange.connection.handshake())
          .sentRequestAtMillis(sentRequestMillis)
          .receivedResponseAtMillis(System.currentTimeMillis())
          .build()
      code = response.code
    }

    exchange.responseHeadersEnd(response)

    response = if (forWebSocket && code == 101) {
      // Connection is upgrading, but we need to ensure interceptors see a non-null response body.
      response.newBuilder()
          .body(EMPTY_RESPONSE)
          .build()
    } else {
      response.newBuilder()
          .body(exchange.openResponseBody(response))
          .build()
    }
    //断开连接
    if ("close".equals(response.request.header("Connection"), ignoreCase = true) ||
        "close".equals(response.header("Connection"), ignoreCase = true)) {
      exchange.noNewExchangesOnConnection()
    }
    //抛出协议异常
    if ((code == 204 || code == 205) && response.body?.contentLength() ?: -1L > 0L) {
      throw ProtocolException(
          "HTTP $code had non-zero Content-Length: ${response.body?.contentLength()}")
    }
    return response
  }
```


这是链中最后一个拦截器，它向 服务器 发起了一次网络访问.负责向服务器发送请求数据、从服务器读取响应数据.拿到数据之后再沿着链返回.
4. 总结
   OkHttp的拦截器链设计得非常巧妙,是典型的责任链模式.并最终由最后一个链处理了网络请求,并拿到结果.本文主要是对OkHttp主流程进行了梳理,
   通过本文能对OkHttp有一个整体的了解.


同步流程补充 
1.构建RealCall
2.添加到Dispatcher的runningSyncCalls同步请求队列    
   由于是阻塞的添加进runningSyncCalls，完成后移除，它的作用是什么？  
    多个同步请求添加后，可以使用Dispatcher.runningCalls获取所有的call以及Dispatcher.runningCallsCount判断是否存在未完成的任务
3.根据RealCall开始拦截器调用链开始请求，成功后移除RealCall
```
Response response = client.newCall(request).execute()
```

RealCall.kt
```
  override fun execute(): Response {
    ...
    //回调事件 callStart
    callStart()
    try {
      //添加到Dispatcher的runningSyncCalls  记录用
      client.dispatcher.executed(this)
      return getResponseWithInterceptorChain()
    } finally {
      //完成后移除请求
      client.dispatcher.finished(this)
    }
  }
  
  //Dispatcher.kt
 @Synchronized internal fun executed(call: RealCall) {
    runningSyncCalls.add(call)
  }
  
  internal fun getResponseWithInterceptorChain(): Response {
   ... 调用链开始请求，查看上面的分析
  }
```



请求的取消cancel
okhttp3/internal/connection/RealCall.kt
```
 override fun cancel() {
    if (canceled) return // Already canceled.

    canceled = true
    exchange?.cancel()
    
    //okhttp3/internal/connection/RealConnection.kt  socket取消
    connectionToCancel?.cancel()

    eventListener.canceled(this)
  }
```
okhttp3/internal/connection/Exchange.kt
```
 fun cancel() {
    codec.cancel()
  }
  
okhttp3/internal/http1/Http1ExchangeCodec.kt  
  override fun cancel() {
    connection.cancel()
  }
  
okhttp3/internal/http2/Http2ExchangeCodec.kt
 override fun cancel() {
    canceled = true
    //htttp2关闭流
    stream?.closeLater(ErrorCode.CANCEL)
  }
```