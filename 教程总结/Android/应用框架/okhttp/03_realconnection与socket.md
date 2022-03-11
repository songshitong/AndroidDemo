RealConnection是包装socket进行实际网络请求的类  socket使用的是tcp协议

RealConnection.kt
看一下结构
```
class RealConnection(
  val connectionPool: RealConnectionPool,
  private val route: Route
) : Http2Connection.Listener(), Connection {
//用于连接的socket
private var rawSocket: Socket? = null
//对外公开，表示connection正在使用的socket 可能是rawSocket 也可能是sslSocket  SSLSocket用于https   在http/2中一个连接的socket可能被多个call使用
private var socket: Socket? = null
 //外部获取socket
 override fun socket(): Socket = socket!!
 
 //输入输出流
 private var source: BufferedSource? = null
 private var sink: BufferedSink? = null
 
 //不再进行网络请求了
 var noNewExchanges = false
 
 //用于http/2的连接
 private var http2Connection: Http2Connection? = null
 //支持多路复用
 internal val isMultiplexed: Boolean
    get() = http2Connection != null
 
}
```


在02_网络请求与连接池复用.md中的findConnection，新建一个connection后会掉用connect方法进行连接
开始连接
```
 fun connect(
    connectTimeout: Int,
    readTimeout: Int,
    writeTimeout: Int,
    pingIntervalMillis: Int,
    connectionRetryEnabled: Boolean,
    call: Call,
    eventListener: EventListener
  ) {
    ...
    var routeException: RouteException? = null
    val connectionSpecs = route.address.connectionSpecs
    val connectionSpecSelector = ConnectionSpecSelector(connectionSpecs)
    ...
    while (true) {
      try {
        //如果要求http tunnel 开始建立隧道
        if (route.requiresTunnel()) {
          connectTunnel(connectTimeout, readTimeout, writeTimeout, call, eventListener)
          if (rawSocket == null) {
            // We were unable to connect the tunnel but properly closed down our resources.
            break
          }
        } else {
          //创建socket并进行连接
          connectSocket(connectTimeout, readTimeout, call, eventListener)
        }
        //建立协议
        establishProtocol(connectionSpecSelector, pingIntervalMillis, call, eventListener)
        eventListener.connectEnd(call, route.socketAddress, route.proxy, protocol)
        break
      } catch (e: IOException) {
        //连接建立失败
        socket?.closeQuietly()
        rawSocket?.closeQuietly()
        socket = null
        rawSocket = null
        source = null
        sink = null
        handshake = null
        protocol = null
        http2Connection = null
        allocationLimit = 1

        eventListener.connectFailed(call, route.socketAddress, route.proxy, null, e)
        if (routeException == null) {
          routeException = RouteException(e)
        } else {
          routeException.addConnectException(e)
        }
        if (!connectionRetryEnabled || !connectionSpecSelector.connectionFailed(e)) {
          throw routeException
        }
      }
    }
    if (route.requiresTunnel() && rawSocket == null) {
      throw RouteException(ProtocolException(
          "Too many tunnel connections attempted: $MAX_TUNNEL_ATTEMPTS"))
    }
    idleAtNs = System.nanoTime()
  }
```
跳过tunnel，主要看普通的流程
主要有两步
1 创建socket并进行连接
connectSocket(connectTimeout, readTimeout, call, eventListener)
2 建立协议
 establishProtocol()

connectSocket
```
 private fun connectSocket(
    connectTimeout: Int,
    readTimeout: Int,
    call: Call,
    eventListener: EventListener
  ) {
    val proxy = route.proxy
    val address = route.address
    //初始化socket
    val rawSocket = when (proxy.type()) {
      Proxy.Type.DIRECT, Proxy.Type.HTTP -> address.socketFactory.createSocket()!!
      else -> Socket(proxy)
    }
    this.rawSocket = rawSocket

    eventListener.connectStart(call, route.socketAddress, proxy)
    rawSocket.soTimeout = readTimeout
    try {
      //调用socket.connect(address, connectTimeout)进行连接
      Platform.get().connectSocket(rawSocket, route.socketAddress, connectTimeout)
    } catch (e: ConnectException) {
      throw ConnectException("Failed to connect to ${route.socketAddress}").apply {
        initCause(e)
      }
    }

    // The following try/catch block is a pseudo hacky way to get around a crash on Android 7.0
    // More details:
    // https://github.com/square/okhttp/issues/3245
    // https://android-review.googlesource.com/#/c/271775/
    try {
      source = rawSocket.source().buffer()
      sink = rawSocket.sink().buffer()
    } catch (npe: NullPointerException) {
      if (npe.message == NPE_THROW_WITH_NULL) {
        throw IOException(npe)
      }
    }
  }
```

establishProtocol()
```
private fun establishProtocol(
    connectionSpecSelector: ConnectionSpecSelector,
    pingIntervalMillis: Int,
    call: Call,
    eventListener: EventListener
  ) {
    if (route.address.sslSocketFactory == null) {
      //非https的
      if (Protocol.H2_PRIOR_KNOWLEDGE in route.address.protocols) {
        socket = rawSocket
        protocol = Protocol.H2_PRIOR_KNOWLEDGE
        startHttp2(pingIntervalMillis)
        return
      }

      socket = rawSocket
      protocol = Protocol.HTTP_1_1
      return
    }

    eventListener.secureConnectStart(call)
    //tls握手
    connectTls(connectionSpecSelector)
    eventListener.secureConnectEnd(call, handshake)

    if (protocol === Protocol.HTTP_2) {
      //http2访问
      startHttp2(pingIntervalMillis)
    }
  }
```
对于http的请求可以直接开始了
对于https需要先进行tls握手，然后才能开始
  对于http2，使用startHttp2初始化http2的一些配置才能开始访问


看一下connectTls tls握手
```
private fun connectTls(connectionSpecSelector: ConnectionSpecSelector) {
    val address = route.address
    val sslSocketFactory = address.sslSocketFactory
    var success = false
    var sslSocket: SSLSocket? = null
    try {
      //创建sslSocket
      // Create the wrapper over the connected socket.
      sslSocket = sslSocketFactory!!.createSocket(
          rawSocket, address.url.host, address.url.port, true /* autoClose */) as SSLSocket
      //配置socket的加密套件，tls版本等 这些存储在ConnectionSpec，然后设置给SSLSocket
      // Configure the socket's ciphers, TLS versions, and extensions.
      val connectionSpec = connectionSpecSelector.configureSecureSocket(sslSocket)
      if (connectionSpec.supportsTlsExtensions) {
        Platform.get().configureTlsExtensions(sslSocket, address.url.host, address.protocols)
      }
      //开始进行tls的握手
      // Force handshake. This can throw!
      sslSocket.startHandshake()
      // block for session establishment
      val sslSocketSession = sslSocket.session
      val unverifiedHandshake = sslSocketSession.handshake()
      //校验证书
      // Verify that the socket's certificates are acceptable for the target host.
      if (!address.hostnameVerifier!!.verify(address.url.host, sslSocketSession)) {
        val peerCertificates = unverifiedHandshake.peerCertificates
        if (peerCertificates.isNotEmpty()) {
          val cert = peerCertificates[0] as X509Certificate
          throw SSLPeerUnverifiedException("""
              |Hostname ${address.url.host} not verified:
              |    certificate: ${CertificatePinner.pin(cert)}
              |    DN: ${cert.subjectDN.name}
              |    subjectAltNames: ${OkHostnameVerifier.allSubjectAltNames(cert)}
              """.trimMargin())
        } else {
          throw SSLPeerUnverifiedException(
              "Hostname ${address.url.host} not verified (no certificates)")
        }
      }

      val certificatePinner = address.certificatePinner!!
      //保存握手使用的加密套件
      handshake = Handshake(unverifiedHandshake.tlsVersion, unverifiedHandshake.cipherSuite,
          unverifiedHandshake.localCertificates) {
        certificatePinner.certificateChainCleaner!!.clean(unverifiedHandshake.peerCertificates,
            address.url.host)
      }

      // Check that the certificate pinner is satisfied by the certificates presented.
      certificatePinner.check(address.url.host) {
        handshake!!.peerCertificates.map { it as X509Certificate }
      }
      
       //握手成功 保存socket和 alpn协议
      // Success! Save the handshake and the ALPN protocol.
      val maybeProtocol = if (connectionSpec.supportsTlsExtensions) {
        Platform.get().getSelectedProtocol(sslSocket)
      } else {
        null
      }
      socket = sslSocket
      source = sslSocket.source().buffer()
      sink = sslSocket.sink().buffer()
      protocol = if (maybeProtocol != null) Protocol.get(maybeProtocol) else Protocol.HTTP_1_1
      success = true
    } finally {
      if (sslSocket != null) {
        Platform.get().afterHandshake(sslSocket)
      }
      if (!success) {
        sslSocket?.closeQuietly()
      }
    }
  }
```
connectTls主要是tls的握手流程
1. 创建sslSocket  配置socket的加密套件，tls版本等
2. 开始进行tls的握手
3. 握手完成后校验证书，保存握手使用的加密套件，socket和 alpn协议


startHttp2
```
private fun startHttp2(pingIntervalMillis: Int) {
    val socket = this.socket!!
    val source = this.source!!
    val sink = this.sink!!
    socket.soTimeout = 0 // HTTP/2 connection timeouts are set per-stream.
    //创建http2Connection
    val http2Connection = Http2Connection.Builder(client = true, taskRunner = TaskRunner.INSTANCE)
        .socket(socket, route.address.url.host, source, sink)
        .listener(this)
        .pingIntervalMillis(pingIntervalMillis)
        .build()
    this.http2Connection = http2Connection
    this.allocationLimit = Http2Connection.DEFAULT_SETTINGS.getMaxConcurrentStreams()
    //开始http2连接
    http2Connection.start()
  }
```

http2 一个连接中存在多个流
Http2Connection.kt
先看一下结构
```
class Http2Connection internal constructor(builder: Builder) : Closeable {
  //当前连接中的流
  internal val streams = mutableMapOf<Int, Http2Stream>()
  
   //异步执行
   /** For scheduling everything asynchronous. */
  private val taskRunner = builder.taskRunner
  
  //异步写数据贞的队列
  /** Asynchronously writes frames to the outgoing socket. */
  private val writerQueue = taskRunner.newQueue()

  //确保push promise callbacks在每条流按顺序  tcp的有序性  push promis用于在发送方打算发起的流之前提前通知对端
  /** Ensures push promise callbacks events are sent in order per stream. */
  private val pushQueue = taskRunner.newQueue()

  //通知settings改变
  /** Notifies the listener of settings changes. */
  private val settingsListenerQueue = taskRunner.newQueue()
  
  //配置滑动窗口INITIAL_WINDOW_SIZE=7字节     OKHTTP_CLIENT_WINDOW_SIZE为16 * 1024 * 1024=16M
  val okHttpSettings = Settings().apply {
    // Flow control was designed more for servers, or proxies than edge clients. If we are a client,
    // set the flow control window to 16MiB.  This avoids thrashing window updates every 64KiB, yet
    // small enough to avoid blowing up the heap.
    if (builder.client) {
      set(Settings.INITIAL_WINDOW_SIZE, OKHTTP_CLIENT_WINDOW_SIZE)
    }
  }
  
  //用于心跳机制的ping/pong
  // Total number of pings send and received of the corresponding types. All guarded by this.
  private var intervalPingsSent = 0L
  private var intervalPongsReceived = 0L
  private var degradedPingsSent = 0L
  private var degradedPongsReceived = 0L
  private var awaitPingsSent = 0L
  private var awaitPongsReceived = 0L
  /** Consider this connection to be unhealthy if a degraded pong isn't received by this time. */
  private var degradedPongDeadlineNs = 0L
}
```
start方法
```
 fun start(sendConnectionPreface: Boolean = true, taskRunner: TaskRunner = TaskRunner.INSTANCE) {
    //默认发送连接配置
    if (sendConnectionPreface) {
      //发送连接配置
      writer.connectionPreface()
      //发送初始化窗口配置
      writer.settings(okHttpSettings)
      val windowSize = okHttpSettings.initialWindowSize
      //DEFAULT_INITIAL_WINDOW_SIZE = 65535  64KB
      if (windowSize != DEFAULT_INITIAL_WINDOW_SIZE) {
       //发送窗口更新
        writer.windowUpdate(0, (windowSize - DEFAULT_INITIAL_WINDOW_SIZE).toLong())
      }
    }
    //运行readerRunnable,准备接收响应流，不断读取数据祯
    //没有使用okhttp clinet的dispatcher，因为他在ConnectionPool中，可能在多个client共享
    // Thread doesn't use client Dispatcher, since it is scoped potentially across clients via
    // ConnectionPool.
    taskRunner.newQueue().execute(name = connectionName, block = readerRunnable)
  }
```
preface   [ˈprefəs]  前言;(书的)序言