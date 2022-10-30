

callTimeout 整个网络请求，请求需要重定向和重试需要走完整个周期   默认0，没有限制
包括：
resolving DNS, connecting, writing the request body, server processing, and reading the response body.

connectTimeout 建立tcp连接  默认10秒
connecting a TCP socket to the target host

readTimeout 读response的io 默认10秒
the TCP socket and for individual read IO operations including on Source of the Response.

writeTimeout 写io 默认10秒
individual write IO operations



okhttp\4.9.1\...\okhttp3\internal\connection\RealCall.kt
callTimeout全局唯一
```
private val timeout = object : AsyncTimeout() {
    override fun timedOut() {
      cancel()
    }
  }.apply {
    timeout(client.callTimeoutMillis.toLong(), MILLISECONDS)
  }
```
读写的超时
okhttp\4.9.1\...\okhttp3\internal\connection\RealConnection.kt
```
  internal fun newCodec(client: OkHttpClient, chain: RealInterceptorChain): ExchangeCodec {
    val socket = this.socket!!
    val source = this.source!!
    val sink = this.sink!!
    val http2Connection = this.http2Connection

    return if (http2Connection != null) {
      Http2ExchangeCodec(client, this, chain, http2Connection)
    } else {
      socket.soTimeout = chain.readTimeoutMillis()
      source.timeout().timeout(chain.readTimeoutMillis.toLong(), MILLISECONDS)
      sink.timeout().timeout(chain.writeTimeoutMillis.toLong(), MILLISECONDS)
      Http1ExchangeCodec(client, this, source, sink)
    }
  }
```
connect超时
okhttp3/internal/connection/RealConnection.kt
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
    ..
    connectSocket(connectTimeout, readTimeout, call, eventListener)
    ..
  }
  

 private fun connectSocket(
    connectTimeout: Int,
    readTimeout: Int,
    call: Call,
    eventListener: EventListener
  ) {
     ...
      Platform.get().connectSocket(rawSocket, route.socketAddress, connectTimeout)
    ...
}  

okhttp3/internal/platform/AndroidPlatform.kt
  override fun connectSocket(
    socket: Socket,
    address: InetSocketAddress,
    connectTimeout: Int
  ) {
    ...
      socket.connect(address, connectTimeout)
    ...
  }
```


okhttp有三种RequestBody
RequestBody.kt   okhttp3/RequestBody.kt  普通的请求体，传入MediaType，将string转为RequestBody
```
 fun String.toRequestBody(contentType: MediaType? = null): RequestBody {
      var charset: Charset = UTF_8
      var finalContentType: MediaType? = contentType
      if (contentType != null) {
        val resolvedCharset = contentType.charset()
        if (resolvedCharset == null) {
          charset = UTF_8
          finalContentType = "$contentType; charset=utf-8".toMediaTypeOrNull()
        } else {
          charset = resolvedCharset
        }
      }
      val bytes = toByteArray(charset)
      return bytes.toRequestBody(finalContentType, 0, bytes.size)
    }
```
FormBody  okhttp3/FormBody.kt  表单结构的请求
```
  companion object {
    private val CONTENT_TYPE: MediaType = "application/x-www-form-urlencoded".toMediaType()
  }
```
MultipartBody okhttp3/MultipartBody.kt
支持上传下载的请求体
```
//默认类型
val FORM = "multipart/form-data".toMediaType()
//为contentType增加boundary
private val contentType: MediaType = "$type; boundary=$boundary".toMediaType()
//默认的boundary由UUID生成
class Builder @JvmOverloads constructor(boundary: String = UUID.randomUUID().toString()) {}
```