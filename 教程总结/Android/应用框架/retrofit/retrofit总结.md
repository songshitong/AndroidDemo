
https://blog.csdn.net/jaynm/article/details/108614788?spm=1001.2014.3001.5502
Retrofit 流程
1 通过解析网络请求接口的注解，配置网络请求参数
2 通过动态代理生成对应接口的网络请求对象
3 通过GsonRequestBodyConverter将请求体转为json字符
4 通过OkHttpCall发送网络请求
5 通过GsonResponseBodyConverter将json字符响应流转为model
6 通过CallAdapter将结果包装为默认的Call   //多个CallAdapter返回第一个符合返回值的
7 用户在主线程处理返回结果

CallAdapter是实现CallAdapter接口，重写adapt方法，将Call的结果回调包装为另一种形式

Retrofit 通过 java 接口以及注解来描述网络请求，并用动态代理的方式生成网络请求的 request，然后通过 client 调用相应的网络框架
（默认 okhttp）去发起网络请求，并将返回的 response 通过 converterFactory 转换成相应的数据 model，
最后通过calladapter转换成其他数据方式（如 rxjava Observable）

动态代理
生成被代理接口的代理类，对接口方法进行功能增强，也就是进行网络请求的过程，并且每个方法调用网络的流程类似，通过动态代理减少样板代码
动态代理的原理
1 通过ProxyGenerator.generateProxyClass生成实现接口的代理类
代理类重写接口方法，内容是InvocationHandler.invoke
2 将代理类加载到JVM    defineClass0
```
(T)
    Proxy.newProxyInstance(
        service.getClassLoader(),
        new Class<?>[] {service},
        new InvocationHandler() {
          ....
          @Override
          public @Nullable Object invoke(Object proxy, Method method, @Nullable Object[] args)
              throws Throwable {
            ...
            return platform.isDefaultMethod(method)
                ? platform.invokeDefaultMethod(method, service, proxy, args)
                : loadServiceMethod(method).invoke(args);
          }
        });
```
invoke返回网络请求的结果
```
final @Nullable ReturnT invoke(Object[] args) {
    Call<ResponseT> call = new OkHttpCall<>(requestFactory, args, callFactory, responseConverter);
    return adapt(call, args);
  }
```


https://juejin.cn/post/6876968255597051917#heading-28
软件上的很多时都是在做trade-off(交换)，相比编译时注解，运行时注解性能较低，但是比较灵活，实现方便. 并且retrofit虽然使用了反射，
但是性能上损失并不大，比起网络请求与gson解析的时间可以说是很小的一部分。 综合来看，实用运行时注解是一种兼顾了使用与性能的方式.
