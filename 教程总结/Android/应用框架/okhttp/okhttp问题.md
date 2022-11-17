
https://zhuanlan.zhihu.com/p/355898458
https://www.jianshu.com/p/d7b2666b4b50
https://blog.csdn.net/lyabc123456/article/details/87368161

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