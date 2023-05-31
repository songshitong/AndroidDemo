
https://blog.csdn.net/ucxiii/article/details/52447945
okhttp异常： java.lang.IllegalStateException: closed
okhttp的responseBody是一个流，string()只能调用一次然后流就关闭了 再次使用就会出错，可能被其他拦截器关闭

https://github.com/hongyangAndroid/okhttputils


全局拦截器设置token
缺点 获取token接口可能携带token   
不好灵活控制
没办法不同接口不同的token


//缓存
默认没有配置缓存
okhttp3/Cache.kt 代码示例
强制请求网络
```
Request request = new Request.Builder()
    .cacheControl(new CacheControl.Builder().noCache().build())
    .url("http://publicobject.com/helloworld.txt")
    .build();
```
服务器决定缓存是否有效
```
Request request = new Request.Builder()
    .cacheControl(new CacheControl.Builder()
        .maxAge(0, TimeUnit.SECONDS)
        .build())
    .url("http://publicobject.com/helloworld.txt")
    .build();
```
强制请求缓存
```
Request request = new Request.Builder()
    .cacheControl(new CacheControl.Builder()
        .onlyIfCached()
        .build())
    .url("http://publicobject.com/helloworld.txt")
    .build();
Response forceCacheResponse = client.newCall(request).execute();
if (forceCacheResponse.code() != 504) {
  // The resource was cached! Show it.
} else {
  // The resource was not cached.
}
```
可以使用陈旧的数据
```
Request request = new Request.Builder()
    .cacheControl(new CacheControl.Builder()
        .maxStale(365, TimeUnit.DAYS)
        .build())
    .url("http://publicobject.com/helloworld.txt")
    .build();
```