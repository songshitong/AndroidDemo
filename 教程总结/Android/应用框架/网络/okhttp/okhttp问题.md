
https://zhuanlan.zhihu.com/p/355898458
https://www.jianshu.com/p/d7b2666b4b50
https://blog.csdn.net/lyabc123456/article/details/87368161

unexpected end of stream on Connection
java.io.EOFException: \n not found: limit=0 content=
java.io.IOException: unexpected end of stream on http://www.xx.com.xxapi
```
Request request = new Request.Builder()
                             .url(URL)
                             .header("Connection", "close") //不复用链接  可能服务器没有复用，client复用了，导致异常
单个接口api注解@Headers("Connection: close")

只配置okhttp连接池可能不生效  新生成的socket通道没有进入pool，但是可能很长时间才断掉
connectionPool(ConnectionPool(0, 1, TimeUnit.MILLISECONDS))                             
```

https://github.com/google/ExoPlayer/issues/7880
其他解释
"Unexpected end of stream" normally means that the server response indicated it will provide a certain number of bytes 
(specified in the Content-Length response header), but then closed the connection before providing that many bytes.
That's a server-side issue (i.e., it's either closing the connection when it shouldn't, 
or it's specifying the Content-Length header response incorrectly)


版本4.9.3
unexpected end of stream
```
okhttp3/internal/connection/Exchange.kt
override fun close() {
      if (closed) return
      closed = true
      if (contentLength != -1L && bytesReceived != contentLength) {
        throw ProtocolException("unexpected end of stream")
      }
      try {
        super.close()
        ...
      } catch (e: IOException) {
       ...
      }
    }
```
错误信息，错误代码
```
java.net.ProtocolException: unexpected end of stream
	at okhttp3.internal.connection.Exchange$RequestBodySink.close(Exchange.kt:239)
	at okio.RealBufferedSink.close(RealBufferedSink.kt:286)
	at okhttp3.internal.http.CallServerInterceptor.intercept(CallServerInterceptor.kt:60)
	at okhttp3.internal.http.RealInterceptorChain.proceed(RealInterceptorChain.kt:109)
	at okhttp3.internal.connection.ConnectInterceptor.intercept(ConnectInterceptor.kt:34)
	at okhttp3.internal.http.RealInterceptorChain.proceed(RealInterceptorChain.kt:109)
	at okhttp3.internal.cache.CacheInterceptor.intercept(CacheInterceptor.kt:95)
	at okhttp3.internal.http.RealInterceptorChain.proceed(RealInterceptorChain.kt:109)
	at okhttp3.internal.http.BridgeInterceptor.intercept(BridgeInterceptor.kt:83)
	at okhttp3.internal.http.RealInterceptorChain.proceed(RealInterceptorChain.kt:109)
	at okhttp3.internal.http.RetryAndFollowUpInterceptor.intercept(RetryAndFollowUpInterceptor.kt:76)
	at okhttp3.internal.http.RealInterceptorChain.proceed(RealInterceptorChain.kt:109)
....
	at okhttp3.internal.http.RealInterceptorChain.proceed(RealInterceptorChain.kt:109)
	...
	at okhttp3.internal.http.RealInterceptorChain.proceed(RealInterceptorChain.kt:109)
	...
	at okhttp3.internal.http.RealInterceptorChain.proceed(RealInterceptorChain.kt:109)
	..
	at okhttp3.internal.http.RealInterceptorChain.proceed(RealInterceptorChain.kt:109)
	at okhttp3.internal.connection.RealCall.getResponseWithInterceptorChain$okhttp(RealCall.kt:201)
	at okhttp3.internal.connection.RealCall.execute(RealCall.kt:154)
	at retrofit2.OkHttpCall.execute(OkHttpCall.java:204)
	at retrofit2.DefaultCallAdapterFactory$ExecutorCallbackCall.execute(DefaultCallAdapterFactory.java:108)
	....
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1167)
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
	at java.lang.Thread.run(Thread.java:764)
```


SocketOutputStream
```
private void socketWrite(byte b[], int off, int len) throws IOException {
        if (len <= 0 || off < 0 || off + len > b.length) {
            if (len == 0) {
                return;
            }
            throw new ArrayIndexOutOfBoundsException();
        }
        FileDescriptor fd = impl.acquireFD();
        try {
            socketWrite0(fd, b, off, len);
        } catch (SocketException se) {
            if (se instanceof sun.net.ConnectionResetException) {
                impl.setConnectionResetPending();
                se = new SocketException("Connection reset");
            }
            if (impl.isClosedOrPending()) {
                throw new SocketException("Socket closed");
            } else {
                throw se;
            }
        } ...
    }
  private native void socketWrite0(FileDescriptor fd, byte[] b, int off,
                                     int len) throws IOException;    
```
后面的参考
http://www.njqityun.com/wap/grab/23259


java.net.SocketException: Broken pipe
```
at java.net.SocketOutputStream.socketWrite0(Native Method)
	at java.net.SocketOutputStream.socketWrite(SocketOutputStream.java:117)
	at java.net.SocketOutputStream.write(SocketOutputStream.java:161)
	at okio.OutputStreamSink.write(JvmOkio.kt:53)
	at okio.AsyncTimeout$sink$1.write(AsyncTimeout.kt:103)
	at okio.RealBufferedSink.flush(RealBufferedSink.kt:267)
	at okhttp3.internal.http1.Http1ExchangeCodec$KnownLengthSink.flush(Http1ExchangeCodec.kt:276)
	at okio.ForwardingSink.flush(ForwardingSink.kt:32)
	at okhttp3.internal.connection.Exchange$RequestBodySink.flush(Exchange.kt:228)
	at okio.RealBufferedSink.flush(RealBufferedSink.kt:269)
	...
	at okhttp3.MultipartBody.writeOrCountBytes(MultipartBody.kt:157)
	at okhttp3.MultipartBody.writeTo(MultipartBody.kt:93)
	at okhttp3.internal.http.CallServerInterceptor.intercept(CallServerInterceptor.kt:59)
	at okhttp3.internal.http.RealInterceptorChain.proceed(RealInterceptorChain.kt:109)
	at okhttp3.internal.connection.ConnectInterceptor.intercept(ConnectInterceptor.kt:34)
	at okhttp3.internal.http.RealInterceptorChain.proceed(RealInterceptorChain.kt:109)
	at okhttp3.internal.cache.CacheInterceptor.intercept(CacheInterceptor.kt:95)
	at okhttp3.internal.http.RealInterceptorChain.proceed(RealInterceptorChain.kt:109)
	at okhttp3.internal.http.BridgeInterceptor.intercept(BridgeInterceptor.kt:83)
	at okhttp3.internal.http.RealInterceptorChain.proceed(RealInterceptorChain.kt:109)
	at okhttp3.internal.http.RetryAndFollowUpInterceptor.intercept(RetryAndFollowUpInterceptor.kt:76)
	at okhttp3.internal.http.RealInterceptorChain.proceed(RealInterceptorChain.kt:109)
	...
	at okhttp3.internal.http.RealInterceptorChain.proceed(RealInterceptorChain.kt:109)
	...
	at okhttp3.internal.http.RealInterceptorChain.proceed(RealInterceptorChain.kt:109)
	...
	at okhttp3.internal.http.RealInterceptorChain.proceed(RealInterceptorChain.kt:109)
...
	at okhttp3.internal.http.RealInterceptorChain.proceed(RealInterceptorChain.kt:109)
	at okhttp3.internal.connection.RealCall.getResponseWithInterceptorChain$okhttp(RealCall.kt:201)
	at okhttp3.internal.connection.RealCall.execute(RealCall.kt:154)
	at retrofit2.OkHttpCall.execute(OkHttpCall.java:204)
	at retrofit2.DefaultCallAdapterFactory$ExecutorCallbackCall.execute(DefaultCallAdapterFactory.java:108)
	...
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1167)
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
	at java.lang.Thread.run(Thread.java:764)
```



Retrofit. java.net.ProtocolException: expected 123 bytes but received 456
https://stackoverflow.com/questions/44845883/retrofit-java-net-protocolexception-expected-bytes-but-received
1 文件上传时 文件发生了改动，例如增减内容
解决：等待文件完成 或者创建文件快照，确保快照不发生变化
注意：文件上传完成后，删除临时文件
2 header中 content-length与文件大小不一致


java.net.SocketException: Software caused connection abort
https://stackoverflow.com/questions/135919/java-net-socketexception-software-caused-connection-abort-recv-failed
This error occurs when a connection is closed abruptly (when a TCP connection is reset while there is still data in the send buffer).
The condition is very similar to a much more common 'Connection reset by peer'. 
It can happen sporadically when connecting over the Internet, but also systematically if the timing is right 
(e.g. with keep-alive connections on localhost)
解决：关闭HttpClient 然后重新创建请求


okhttp设置连接池
OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
clientBuilder.connectionPool(new ConnectionPool(5, 180, TimeUnit.SECONDS));  //连接池数量，连接池里面空闲连接的存活时间
//需要与后端复用时间统一，  假如客户端存活为5分钟，服务器3分钟
服务器连接死掉之后，客户端仍然认为是存活的进行复用，此时连接出错   Socket closed/java.net.SocketException: Connection reset

