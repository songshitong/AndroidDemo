
https://www.jianshu.com/p/a5a39a747669
Retrofit天生支持RxJava类型的Call，DefaultCall，Guava等Call的回调，这是因为我们在为retrofit添加它所支持的每一个call都通过一个适配器模式，
  使它们转换成了一种类型的call，也就是多种输入，一种输出。

先看一个Call接口  call代表网络请求和其响应
```
public interface Call<T> extends Cloneable {
  //同步方法
  Response<T> execute() throws IOException;
  //异步方法
  void enqueue(Callback<T> callback);
  boolean isExecuted();
  void cancel();
  boolean isCanceled();
  Call<T> clone();
  Request request();
  Timeout timeout();
}
//响应回调
public interface Callback<T> {
  void onResponse(Call<T> call, Response<T> response);
  void onFailure(Call<T> call, Throwable t);
}
```
call的实现是OkHttpCall，可以通过Retrofit.Builder.callFactory自定义call


看一下CallAdapter的接口
```
public interface CallAdapter<R, T> {
  Type responseType();
  //泛型的使用
  T adapt(Call<R> call);

  abstract class Factory {
   
    public abstract @Nullable CallAdapter<?, ?> get(
        Type returnType, Annotation[] annotations, Retrofit retrofit);

    protected static Type getParameterUpperBound(int index, ParameterizedType type) {
      return Utils.getParameterUpperBound(index, type);
    }
    protected static Class<?> getRawType(Type type) {
      return Utils.getRawType(type);
    }
  }
}
```
CallAdapter的关键方法adapt 会将传入的Call<R>适配为T
Factory.get用于获取CallAdapter

这里看一下DefaultCallAdapterFactory
```
final class DefaultCallAdapterFactory extends CallAdapter.Factory {
  private final @Nullable Executor callbackExecutor;

  DefaultCallAdapterFactory(@Nullable Executor callbackExecutor) {
    this.callbackExecutor = callbackExecutor;
  }

  @Override
  public @Nullable CallAdapter<?, ?> get(
      Type returnType, Annotation[] annotations, Retrofit retrofit) {
    if (getRawType(returnType) != Call.class) {
      return null;
    }
    if (!(returnType instanceof ParameterizedType)) {
      throw new IllegalArgumentException(
          "Call return type must be parameterized as Call<Foo> or Call<? extends Foo>");
    }
    final Type responseType = Utils.getParameterUpperBound(0, (ParameterizedType) returnType);

    final Executor executor =
        Utils.isAnnotationPresent(annotations, SkipCallbackExecutor.class)
            ? null
            : callbackExecutor;

    return new CallAdapter<Object, Call<?>>() {
      @Override
      public Type responseType() {
        return responseType;
      }

      @Override
      public Call<Object> adapt(Call<Object> call) {
        return executor == null ? call : new ExecutorCallbackCall<>(executor, call);
      }
    };
  }
```
可以看到DefaultCallAdapterFactory用于处理返回值为Call,通过adapt方法将call转为ExecutorCallbackCall
  ExecutorCallbackCall就是将回调方法放入指定的executor中执行,可以看一下retrofit的解析



CallAdapter的初始化
1.添加自定义CallAdapter
```
 public Builder addCallAdapterFactory(CallAdapter.Factory factory) {
      callAdapterFactories.add(Objects.requireNonNull(factory, "factory == null"));
      return this;
    }
```
2.默认
 android21 DefaultCallAdapterFactory
 android24 增加CompletableFutureCallAdapterFactory


CallAdapter的应用
CallAdapter的创建与使用
```
//HttpServiceMethod
static <ResponseT, ReturnT> HttpServiceMethod<ResponseT, ReturnT> parseAnnotations(
      Retrofit retrofit, Method method, RequestFactory requestFactory) {
    ...
    Type adapterType;
    ... 确定Adapter的类型
    //创建CallAdapter
    CallAdapter<ResponseT, ReturnT> callAdapter =
        createCallAdapter(retrofit, method, adapterType, annotations);
    ...
    okhttp3.Call.Factory callFactory = retrofit.callFactory;
    if (!isKotlinSuspendFunction) {
      返回CallAdapted
      return new CallAdapted<>(requestFactory, callFactory, responseConverter, callAdapter);
    } ...
  }
   //获取CallAdapter  也就是自定义的和默认的DefaultCallAdapterFactory，CompletableFutureCallAdapterFactory
   private static <ResponseT, ReturnT> CallAdapter<ResponseT, ReturnT> createCallAdapter(
      Retrofit retrofit, Method method, Type returnType, Annotation[] annotations) {
    try {
      //noinspection unchecked
      return (CallAdapter<ResponseT, ReturnT>) retrofit.callAdapter(returnType, annotations);
    } ..
  } 
```
adapt方法的调用
```
  //Retrofit
  public <T> T create(final Class<T> service) {
    validateServiceInterface(service);
    return (T)
        Proxy.newProxyInstance(
            service.getClassLoader(),
            new Class<?>[] {service},
            new InvocationHandler() {
              private final Object[] emptyArgs = new Object[0];

              @Override
              public @Nullable Object invoke(Object proxy, Method method, @Nullable Object[] args)
                  throws Throwable {
                ...
                args = args != null ? args : emptyArgs;
                Platform platform = Platform.get();
                return platform.isDefaultMethod(method)
                    ? platform.invokeDefaultMethod(method, service, proxy, args)
                    : loadServiceMethod(method).invoke(args); //调用HttpServiceMehtod的invoke方法
              }
            });
  }
  
  //HttpServiceMehtod
  final @Nullable ReturnT invoke(Object[] args) {
    Call<ResponseT> call = new OkHttpCall<>(requestFactory, args, callFactory, responseConverter);
    return adapt(call, args);
  }
```
再看一下HttpServiceMethod的adapt   泛型
```
abstract class HttpServiceMethod<ResponseT, ReturnT> extends ServiceMethod<ReturnT> {
   protected abstract @Nullable ReturnT adapt(Call<ResponseT> call, Object[] args);
   
   static final class CallAdapted<ResponseT, ReturnT> extends HttpServiceMethod<ResponseT, ReturnT> {
        private final CallAdapter<ResponseT, ReturnT> callAdapter;
        @Override
        protected ReturnT adapt(Call<ResponseT> call, Object[] args) {
          return callAdapter.adapt(call);
        } 
   }
}
```
HttpServiceMethod的adapt实现由子类实现，CallAdapted的实现是调用CallAdapter的adapt实现
在结合上面CallAdapter的创建

动态代理的Invoke方法就是将Call通过DefaultCallAdapterFactory适配为ExecutorCallbackCall


返回值为CompletableFuture的例子
```
  interface Service {
    @GET("/")
    CompletableFuture<String> body();
  }
  service = retrofit.create(Service.class);
  CompletableFuture<String> future = service.body();
  future.get()//获取结果
```

看一下CompletableFutureCallAdapterFactory
```
final class CompletableFutureCallAdapterFactory extends CallAdapter.Factory {
  @Override
  public @Nullable CallAdapter<?, ?> get(
      Type returnType, Annotation[] annotations, Retrofit retrofit) {
    //非CompletableFuture的返回值，返回null  
    if (getRawType(returnType) != CompletableFuture.class) {
      return null;
    }
    if (!(returnType instanceof ParameterizedType)) {
      throw new IllegalStateException(
          "CompletableFuture return type must be parameterized"
              + " as CompletableFuture<Foo> or CompletableFuture<? extends Foo>");
    }
    Type innerType = getParameterUpperBound(0, (ParameterizedType) returnType);

    if (getRawType(innerType) != Response.class) {
      // Generic type is not Response<T>. Use it for body-only adapter.
      //非Response<T>
      return new BodyCallAdapter<>(innerType);
    }

    // Generic type is Response<T>. Extract T and create the Response version of the adapter.
    if (!(innerType instanceof ParameterizedType)) {
      throw new IllegalStateException(
          "Response must be parameterized" + " as Response<Foo> or Response<? extends Foo>");
    }
    Type responseType = getParameterUpperBound(0, (ParameterizedType) innerType);
    return new ResponseCallAdapter<>(responseType);
  }
```
看一下将call适配为CompletableFuture
```
//实现CallAdapter接口
private static final class ResponseCallAdapter<R>
      implements CallAdapter<R, CompletableFuture<Response<R>>> {
    private final Type responseType;

    //实现adapt方法，将call适配为CompletableFuture<Response<R>>
    @Override
    public CompletableFuture<Response<R>> adapt(final Call<R> call) {
      CompletableFuture<Response<R>> future = new CallCancelCompletableFuture<>(call);
      call.enqueue(new ResponseCallback(future));
      return future;
    }

    //ResponseCallback 将Callback的回调onResponse或onFailure改为CompletableFuture通知
    @IgnoreJRERequirement
    private class ResponseCallback implements Callback<R> {
      private final CompletableFuture<Response<R>> future;

      @Override
      public void onResponse(Call<R> call, Response<R> response) {
        future.complete(response);
      }

      @Override
      public void onFailure(Call<R> call, Throwable t) {
        future.completeExceptionally(t);
      }
    }
  }
```
ResponseCallAdapter就将Call的回调onResponse或onFailure经过CompletableFuture的调用


CallCancelCompletableFuture是CompletableFuture的取消调用Call的cancel
```
 private static final class CallCancelCompletableFuture<T> extends CompletableFuture<T> {
    private final Call<?> call;
    public boolean cancel(boolean mayInterruptIfRunning) {
      if (mayInterruptIfRunning) {
        call.cancel();
      }
      return super.cancel(mayInterruptIfRunning);
    }
  }
```

BodyCallAdapter与上面的相似，就是BodyCallback的处理不同
```
private static final class BodyCallAdapter<R> implements CallAdapter<R, CompletableFuture<R>> {
    private final Type responseType;

    @Override
    public CompletableFuture<R> adapt(final Call<R> call) {
      CompletableFuture<R> future = new CallCancelCompletableFuture<>(call);
      call.enqueue(new BodyCallback(future));
      return future;
    }

    @IgnoreJRERequirement
    private class BodyCallback implements Callback<R> {
      private final CompletableFuture<R> future;

      @Override
      public void onResponse(Call<R> call, Response<R> response) {
        if (response.isSuccessful()) {
          future.complete(response.body());
        } else {
          future.completeExceptionally(new HttpException(response));
        }
      }

      @Override
      public void onFailure(Call<R> call, Throwable t) {
        future.completeExceptionally(t);
      }
    }
  }
```