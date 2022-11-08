
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