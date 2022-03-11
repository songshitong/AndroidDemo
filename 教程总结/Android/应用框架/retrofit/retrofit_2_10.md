
代码版本：2.10.0-SNAPSHOT  
todo kotlin相关语法 从Version 2.5.0 (2018-11-18)这个版本开始正式支持kotlin语法
https://github.com/xfhy/Android-Notes/blob/master/Blogs/Android/%E4%B8%89%E6%96%B9%E5%BA%93%E5%8E%9F%E7%90%86/Retrofit_%E5%8E%9F%E7%90%86%E8%A7%A3%E6%9E%90.md

retrofit的优点
1.超级解耦 ，接口定义、接口参数、接口回调不在耦合在一起
2.可以配置不同的httpClient来实现网络请求，如okhttp、httpclient
3.可以配置不同反序列化工具类来解析不同的数据，如json、xml
4.使用大量注解来简化请求，Retrofit将okhttp请求抽象成java接口，使用注解来配置和描述网络请求参数
5.支持同步、异步、Rxjava
6.rest风格

Retrofit,一个远近闻名的网络框架,它是由Square公司开源的.Square公司,是我们的老熟人了,很多框架都是他开源的,比如OkHttp,picasso,leakcanary等等.
  他们公司的很多开源库,几乎已经成为现在开发Android APP的标配.
简单来说,Retrofit其实是底层还是用的OkHttp来进行网络请求的,只不过他包装了一下,使得开发者在使用访问网络的时候更加方便简单高效.
一句话总结:Retrofit将接口动态生成实现类,该接口定义了请求方式+路径+参数等注解,Retrofit可以方便得从注解中获取这些参数,然后拼接起来,
   通过OkHttp访问网络.

1. 基本使用
   很简单,我就是简单请求一个GET接口,拿点json数据,解析成对象实例.
首先,我们需要定义一个interface.这个interface是网络请求的API接口,里面定义了请求方式,入参,数据,返回值等数据.
我这里使用的接口是鸿神的wanandroid网站的开放接口,地址: https://www.wanandroid.com/blog/show/2 . 
   我使用的是https://wanandroid.com/wxarticle/chapters/json.
```
interface IArticleApi {
     @GET("wxarticle/list/{id}/{page}/json")
    fun getAirticles(@Path("id") id: String, @Path("page") page: String): Call<BaseData>
}
```   
API接口定义好了之后,我们需要构建一个Retrofit实例.
```
private val mRetrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://wanandroid.com/")
            .addConverterFactory(GsonConverterFactory.create())   //数据解析器
            .build()
    }
```
有了Retrofit实例之后,我们需要让Retrofit帮我们把上面定义的API接口转换成实例,然后我们就可以直接把它当做实例来调用了
```
//用接口生成实例
val iArticleApi = mRetrofit.create(IArticleApi::class.java)
//调用方法  返回Call对象
val airticlesCall = iArticleApi.getAirticles("408", "1")
```
调用之后会返回一个Call对象,我们可以拿着这个Call对象去访问网络了
```
//异步请求方式
airticlesCall.enqueue(object : Callback<BaseData> {
    override fun onFailure(call: Call<BaseData>, t: Throwable) {
        //请求失败
        t.printStackTrace()
        Log.e("xfhy", "请求失败")
    }

    override fun onResponse(call: Call<BaseData>, response: Response<BaseData>) {
        //请求成功
        val baseData = response.body()
        Log.e("xfhy", "请求成功 ${baseData?.toString()}")
    }
})

//同步请求
airticlesCall.execute();
```

拿着这个Call对象调用enqueue方法即可异步访问网络,获取结果,非常简单.

2. 构建Retrofit
   ps:这里插播一个小技巧,当我们构建一个对象需要传入很多很多必要的参数才能构建起来的时候,我们需要使用Builder模式(构造器模式).
   如果不太了解的同学,看这里

Retrofit源码中使用了很多Builder模式,像比如接下来要讲的Retrofit 构建,我们看一下它的构建
```
Retrofit.Builder()
        .baseUrl("https://wanandroid.com/")
        .addConverterFactory(GsonConverterFactory.create())   //数据解析器
        .build()
```
Builder()方法内部就不细看了,里面就是获取一下当前是什么平台(Android,Java).我们来看一下baseUrl方法
```
Retrofit#Builder#baseUrl
public Builder baseUrl(String baseUrl) {
  return baseUrl(HttpUrl.get(baseUrl));
}
 
 public Builder baseUrl(HttpUrl baseUrl) {
      Objects.requireNonNull(baseUrl, "baseUrl == null");
      List<String> pathSegments = baseUrl.pathSegments();
      //对baseUrl进行校验,必须以/结尾
      if (!"".equals(pathSegments.get(pathSegments.size() - 1))) {
        throw new IllegalArgumentException("baseUrl must end in /: " + baseUrl);
      }
      this.baseUrl = baseUrl;
      return this;
    }
```
通过HttpUrl的静态get方法构建了一个HttpUrl,传入的是一个baseUrl,HttpUrl里面主要是根据baseUrl获取scheme,host,port,url等等信息的.
```
public static HttpUrl get(String url) {
    return new Builder().parse(null, url).build();
  }

//看一下解析URL的过程  
Builder parse(@Nullable HttpUrl base, String input) {
      int pos = skipLeadingAsciiWhitespace(input, 0, input.length());
      int limit = skipTrailingAsciiWhitespace(input, pos, input.length());

      // Scheme. 获取scheme https或http
      int schemeDelimiterOffset = schemeDelimiterOffset(input, pos, limit);
      if (schemeDelimiterOffset != -1) {
        if (input.regionMatches(true, pos, "https:", 0, 6)) {
          this.scheme = "https";
          pos += "https:".length();
        } else if (input.regionMatches(true, pos, "http:", 0, 5)) {
          this.scheme = "http";
          pos += "http:".length();
        } else {
          throw new IllegalArgumentException("Expected URL scheme 'http' or 'https' but was '"
              + input.substring(0, schemeDelimiterOffset) + "'");
        }
      } else if (base != null) {
        this.scheme = base.scheme;
      } else {
        throw new IllegalArgumentException(
            "Expected URL scheme 'http' or 'https' but no colon was found");
      }

      // Authority. 获取URI中的authority 格式为username:password@host:port
      //Username, password and port are optional 是可选的  host是必选的
      boolean hasUsername = false;
      boolean hasPassword = false;
      int slashCount = slashCount(input, pos, limit);
      if (slashCount >= 2 || base == null || !base.scheme.equals(this.scheme)) {
        pos += slashCount;
        authority:
        while (true) {
          int componentDelimiterOffset = delimiterOffset(input, pos, limit, "@/\\?#");
          int c = componentDelimiterOffset != limit
              ? input.charAt(componentDelimiterOffset)
              : -1;
          switch (c) {
            case '@':
              // User info precedes.
              if (!hasPassword) {
                int passwordColonOffset = delimiterOffset(
                    input, pos, componentDelimiterOffset, ':');
                String canonicalUsername = canonicalize(input, pos, passwordColonOffset,
                    USERNAME_ENCODE_SET, true, false, false, true, null);
                this.encodedUsername = hasUsername
                    ? this.encodedUsername + "%40" + canonicalUsername
                    : canonicalUsername;
                if (passwordColonOffset != componentDelimiterOffset) {
                  hasPassword = true;
                  this.encodedPassword = canonicalize(input, passwordColonOffset + 1,
                      componentDelimiterOffset, PASSWORD_ENCODE_SET, true, false, false, true,
                      null);
                }
                hasUsername = true;
              } else {
                this.encodedPassword = this.encodedPassword + "%40" + canonicalize(input, pos,
                    componentDelimiterOffset, PASSWORD_ENCODE_SET, true, false, false, true, null);
              }
              pos = componentDelimiterOffset + 1;
              break;

            case -1:
            case '/':
            case '\\':
            case '?':
            case '#':
              // Host info precedes.
              int portColonOffset = portColonOffset(input, pos, componentDelimiterOffset);
              if (portColonOffset + 1 < componentDelimiterOffset) {
                host = canonicalizeHost(input, pos, portColonOffset);
                port = parsePort(input, portColonOffset + 1, componentDelimiterOffset);
                if (port == -1) {
                  throw new IllegalArgumentException("Invalid URL port: \""
                      + input.substring(portColonOffset + 1, componentDelimiterOffset) + '"');
                }
              } else {
                host = canonicalizeHost(input, pos, portColonOffset);
                port = defaultPort(scheme);
              }
              if (host == null) {
                throw new IllegalArgumentException(
                    INVALID_HOST + ": \"" + input.substring(pos, portColonOffset) + '"');
              }
              pos = componentDelimiterOffset;
              break authority;
          }
        }
      } else {
        // This is a relative link. Copy over all authority components. Also maybe the path & query.
        this.encodedUsername = base.encodedUsername();
        this.encodedPassword = base.encodedPassword();
        this.host = base.host;
        this.port = base.port;
        this.encodedPathSegments.clear();
        this.encodedPathSegments.addAll(base.encodedPathSegments());
        if (pos == limit || input.charAt(pos) == '#') {
          encodedQuery(base.encodedQuery());
        }
      }

      // Resolve the relative path.   相对路径部分
      int pathDelimiterOffset = delimiterOffset(input, pos, limit, "?#");
      resolvePath(input, pos, pathDelimiterOffset);
      pos = pathDelimiterOffset;

      // Query. 获取query部分,以?开始
      if (pos < limit && input.charAt(pos) == '?') {
        int queryDelimiterOffset = delimiterOffset(input, pos, limit, '#');
        this.encodedQueryNamesAndValues = queryStringToNamesAndValues(canonicalize(
            input, pos + 1, queryDelimiterOffset, QUERY_ENCODE_SET, true, false, true, true, null));
        pos = queryDelimiterOffset;
      }

      // Fragment. 获取fragment部分，以#开始，一般代表次级资源
      if (pos < limit && input.charAt(pos) == '#') {
        this.encodedFragment = canonicalize(
            input, pos + 1, limit, FRAGMENT_ENCODE_SET, true, false, false, false, null);
      }

      return this;
    }  
```

然后是addConverterFactory方法,添加解析器
```
private final List<Converter.Factory> converterFactories = new ArrayList<>();
 public Builder addConverterFactory(Converter.Factory factory) {
      converterFactories.add(Objects.requireNonNull(factory, "factory == null"));
      return this;
    }
```

解析器是可以有多个的.
最后就是Retrofit.Builder的build方法了
```
public Retrofit build() {
      if (baseUrl == null) {
        throw new IllegalStateException("Base URL required.");
      }

      Platform platform = Platform.get();

      okhttp3.Call.Factory callFactory = this.callFactory;
      if (callFactory == null) {
        callFactory = new OkHttpClient();
      }

      Executor callbackExecutor = this.callbackExecutor;
      if (callbackExecutor == null) {
        //默认的线程池 MainThreadExecutor
        callbackExecutor = platform.defaultCallbackExecutor();
      }

      // Make a defensive copy of the adapters and add the default Call adapter.
      //添加适配器,为了支持除了Call对象以外的返回类型
      List<CallAdapter.Factory> callAdapterFactories = new ArrayList<>(this.callAdapterFactories);
      List<? extends CallAdapter.Factory> defaultCallAdapterFactories =
          platform.createDefaultCallAdapterFactories(callbackExecutor);
      callAdapterFactories.addAll(defaultCallAdapterFactories);

      // Make a defensive copy of the converters.
       //转换器,用于序列化 和 反序列化
      List<? extends Converter.Factory> defaultConverterFactories =
          platform.createDefaultConverterFactories();
      int defaultConverterFactoriesSize = defaultConverterFactories.size();
      List<Converter.Factory> converterFactories =
          new ArrayList<>(1 + this.converterFactories.size() + defaultConverterFactoriesSize);
      converterFactories.add(new BuiltInConverters());
      converterFactories.addAll(this.converterFactories);
      converterFactories.addAll(defaultConverterFactories);

      return new Retrofit(
          callFactory,
          baseUrl,
          unmodifiableList(converterFactories),
          defaultConverterFactoriesSize,
          unmodifiableList(callAdapterFactories),
          defaultCallAdapterFactories.size(),
          callbackExecutor,
          validateEagerly);
    }
 
  //默认的Android线程池   
  private static final class MainThreadExecutor implements Executor {
    static final Executor INSTANCE = new MainThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    public void execute(Runnable r) {
      handler.post(r);
    }
  }    
```
可以看到,就是将前面的一些参数(baseUrl,转换器,适配器等)什么的都配置到Retrofit对象里面.
build方法的步骤
1. 初始化默认的OKHttpClient
2. 初始化默认的执行线程池，通过handler切换到主线程
3. 添加适配器，支持call以外的对象
   默认Android21 是DefaultCallAdapterFactory  Android24是DefaultCallAdapterFactory与CompletableFutureCallAdapterFactory
   CompletableFutureCallAdapterFactory主要适配Java的CompletableFuture
   DefaultCallAdapterFactory主要支持retrofit的Call
4. 添加转换器   转换器主要用于将okhttp的ResponseBody转换为业务类，或者将业务类转为RequestBody
   顺序是BuiltInConverters,自定义的,默认的
      BuiltInConverters是用于解析ResponseBody，Void，Unit
      默认的转换器在Android24为OptionalConverterFactory用于跳过解析Optional类
   


3. 获取网络请求参数
   接下来的就比较带感了,Retrofit其实是通过我们定义的API interface来获取网络请求的入参的.Retrofit为什么能将接口转换成实现类,让我们调用呢?
   下面来看源码
3.1 构建interface实例
在上面的示例中 mRetrofit.create(IArticleApi::class.java),这一句代码将接口转换成了实现类,我们进去看看
```
public <T> T create(final Class<T> service) {
     //这里传入的必须是接口
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
                // 如果method属于Object，执行普通调用
                if (method.getDeclaringClass() == Object.class) {
                  return method.invoke(this, args);
                }
                args = args != null ? args : emptyArgs;
                Platform platform = Platform.get();
                //读取method中的所有数据  就是是网络请求的所有入参
                return platform.isDefaultMethod(method)
                    ? platform.invokeDefaultMethod(method, service, proxy, args)
                    : loadServiceMethod(method).invoke(args);
              }
            });
  }
```
动态代理的作用是通过动态代理获取接口service的代理类，但是在InvocationHandler没有调用被代理类的方法，因为接口service没有实现类
   也不需要，接口的方法的逻辑由retrofit的Call实现了，通过动态代理+适配的方式，返回接口方法不同的Call,最终网络调用的逻辑
   统一由Call实现,动态代理返回的是代理类，接口定义的方法的返回值为Call，网络请求逻辑在Call里面
通过动态代理的方式,获取其执行时的方法上的注解+形参等数据,并保存于serviceMethod对象中.args(形参的值)存入OkHttpCall中,
先在这里,稍后使用.现在我们来看一下,如何获取到method里面的数据

3.2 ServiceMethod 获取入参
我们从上面的loadServiceMethod方法进入
这里主要看非default的方法的处理loadServiceMethod
```
 //retrofit
 //缓存Method与ServiceMethod,每次根据Method去读取数据比较麻烦,缓存起来,下次进入直接返回,非常高效
 private final Map<Method, ServiceMethod<?, ?>> serviceMethodCache = new ConcurrentHashMap<>();
 ServiceMethod<?> loadServiceMethod(Method method) {
    ServiceMethod<?> result = serviceMethodCache.get(method);
    //有缓存用缓存
    if (result != null) return result;
    synchronized (serviceMethodCache) {
      result = serviceMethodCache.get(method);
      if (result == null) {
        //将method传入,然后去读取它的注解数据
        result = ServiceMethod.parseAnnotations(this, method);
        //将serviceMethod存入缓存
        serviceMethodCache.put(method, result);
      }
    }
    return result;
  }

//HttpServiceMethod
final @Nullable ReturnT invoke(Object[] args) {
    //构建OkHttpCall 各种注解啊,数据啊都传过去    args是方法的参数数据
    Call<ResponseT> call = new OkHttpCall<>(requestFactory, args, callFactory, responseConverter);
    return adapt(call, args);
  }
```
loadServiceMethod主要是可以看到缓存Method与ServiceMethod,每次根据Method去读取数据比较麻烦,缓存起来,下次进入直接返回,
   非常高效.我们去看看它内部读取数据的部分
```
static <T> ServiceMethod<T> parseAnnotations(Retrofit retrofit, Method method) {
    //接口方法的注解  比如GET,PUT,POST等
    RequestFactory requestFactory = RequestFactory.parseAnnotations(retrofit, method);
    //返回类型
    Type returnType = method.getGenericReturnType();
    if (Utils.hasUnresolvableType(returnType)) {
      throw methodError(
          method,
          "Method return type must not include a type variable or wildcard: %s",
          returnType);
    }
    if (returnType == void.class) {
      throw methodError(method, "Service methods cannot return void.");
    }
    //构建ServiceMethod
    return HttpServiceMethod.parseAnnotations(retrofit, method, requestFactory);
  }
  
 //不支持的返回类型   
static boolean hasUnresolvableType(@Nullable Type type) {
    if (type instanceof Class<?>) {
      return false;
    }
    if (type instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) type;
      for (Type typeArgument : parameterizedType.getActualTypeArguments()) {
        if (hasUnresolvableType(typeArgument)) {
          return true;
        }
      }
      return false;
    }
    if (type instanceof GenericArrayType) {
      return hasUnresolvableType(((GenericArrayType) type).getGenericComponentType());
    }
    if (type instanceof TypeVariable) {
      return true;
    }
    if (type instanceof WildcardType) {
      return true;
    }
    String className = type == null ? "null" : type.getClass().getName();
    throw new IllegalArgumentException(
        "Expected a Class, ParameterizedType, or "
            + "GenericArrayType, but <"
            + type
            + "> is of type "
            + className);
  }  
```

RequestFactory获取接口方法注解的过程
```
 //使用build模式构建RequestFactory
 static RequestFactory parseAnnotations(Retrofit retrofit, Method method) {
    return new Builder(retrofit, method).build();
  }
  Builder(Retrofit retrofit, Method method) {
      this.retrofit = retrofit;
      this.method = method;
      //获取这个方法的注解
      this.methodAnnotations = method.getAnnotations();
      //获取这个方法的参数类型
      this.parameterTypes = method.getGenericParameterTypes();
      //获取这个方法的参数注解  参数上的注解可能是一个数组,因为可能不止一个注解.
      this.parameterAnnotationsArray = method.getParameterAnnotations();
    }

    RequestFactory build() {
      //遍历方法的所有注解  获取http请求的相关信息 url,header,method等
      for (Annotation annotation : methodAnnotations) {
        parseMethodAnnotation(annotation);
      }
      //对http协议进行校验 
      if (httpMethod == null) {
        throw methodError(method, "HTTP method annotation is required (e.g., @GET, @POST, etc.).");
      }
      if (!hasBody) {
        if (isMultipart) {
          throw methodError(
              method,
              "Multipart can only be specified on HTTP methods with request body (e.g., @POST).");
        }
        if (isFormEncoded) {
          throw methodError(
              method,
              "FormUrlEncoded can only be specified on HTTP methods with "
                  + "request body (e.g., @POST).");
        }
      }

      int parameterCount = parameterAnnotationsArray.length;
      parameterHandlers = new ParameterHandler<?>[parameterCount];
      for (int p = 0, lastParameter = parameterCount - 1; p < parameterCount; p++) {
        //解析参数的注解
        parameterHandlers[p] =
            parseParameter(p, parameterTypes[p], parameterAnnotationsArray[p], p == lastParameter);
      }

      if (relativeUrl == null && !gotUrl) {
        throw methodError(method, "Missing either @%s URL or @Url parameter.", httpMethod);
      }
      if (!isFormEncoded && !isMultipart && !hasBody && gotBody) {
        throw methodError(method, "Non-body HTTP method cannot contain @Body.");
      }
      if (isFormEncoded && !gotField) {
        throw methodError(method, "Form-encoded method must contain at least one @Field.");
      }
      if (isMultipart && !gotPart) {
        throw methodError(method, "Multipart method must contain at least one @Part.");
      }
      return new RequestFactory(this);
    }
    
//解析这个方法上的注解是表示的哪种HTTP请求     
private void parseMethodAnnotation(Annotation annotation) {
      //parseHttpMethodAndPath 主要是根据http的method获取相对路径，url参数
      if (annotation instanceof DELETE) {
        parseHttpMethodAndPath("DELETE", ((DELETE) annotation).value(), false);
      } else if (annotation instanceof GET) { 
        parseHttpMethodAndPath("GET", ((GET) annotation).value(), false);
      } 
       ...///省略  HEAD PATCH POST PUT OPTIONS
       else if (annotation instanceof HTTP) {
        HTTP http = (HTTP) annotation;
        parseHttpMethodAndPath(http.method(), http.path(), http.hasBody());
      } else if (annotation instanceof retrofit2.http.Headers) {
        String[] headersToParse = ((retrofit2.http.Headers) annotation).value();
        if (headersToParse.length == 0) {
          throw methodError(method, "@Headers annotation is empty.");
        }
        //解析http header  例如Content-Type
        headers = parseHeaders(headersToParse);
      } else if (annotation instanceof Multipart) {
        if (isFormEncoded) {
          throw methodError(method, "Only one encoding annotation is allowed.");
        }
        //标记请求为Multipart类型
        isMultipart = true;
      } else if (annotation instanceof FormUrlEncoded) {
        if (isMultipart) {
          throw methodError(method, "Only one encoding annotation is allowed.");
        }
        //标记请求为FormEncoded类型  表单请求
        isFormEncoded = true;
      }
    }    
```
parseMethodAnnotation方法首先是判断是哪种HTTP请求的注解,然后通过parseHttpMethodAndPath方法去分析
```
//获取注解上面的值
private void parseHttpMethodAndPath(String httpMethod, String value, boolean hasBody) {
  this.httpMethod = httpMethod;
  this.hasBody = hasBody;
  //获取方法注解里面的值   比如上面的示例是wxarticle/list/{id}/{page}/json
  this.relativeUrl = value;
  //把那种需要替换值的地方找出来   上面的示例获取出来的结果是id和page
  this.relativeUrlParamNames = parsePathParameters(value);
}
```
parseHttpMethodAndPath方法分析获取的是 http请求方式+省略域名的url+需要替换路径中值的地方.


参数注解的解析
```
  for (int p = 0, lastParameter = parameterCount - 1; p < parameterCount; p++) {
        parameterHandlers[p] =
            parseParameter(p, parameterTypes[p], parameterAnnotationsArray[p], p == lastParameter);
      }
```
获取参数的注解+参数类型,一起传入parseParameter方法进行解析
```
  private @Nullable ParameterHandler<?> parseParameter(
        int p, Type parameterType, @Nullable Annotation[] annotations, boolean allowContinuation) {
      ParameterHandler<?> result = null;
      if (annotations != null) {
        for (Annotation annotation : annotations) {
          ParameterHandler<?> annotationAction =
              parseParameterAnnotation(p, parameterType, annotations, annotation);

          if (annotationAction == null) {
            continue;
          }

          if (result != null) {
            throw parameterError(
                method, p, "Multiple Retrofit annotations found, only one allowed.");
          }

          result = annotationAction;
        }
      }

      if (result == null) {
        if (allowContinuation) {
          try {
            //kotlin suspend方法  todo Continuation
            if (Utils.getRawType(parameterType) == Continuation.class) {
              isKotlinSuspendFunction = true;
              return null;
            }
          } catch (NoClassDefFoundError ignored) {
          }
        }
        throw parameterError(method, p, "No Retrofit annotation found.");
      }

      return result;
    }
```
parseParameter里面主要就是调用parseParameterAnnotation生成ParameterHandler
```
    private ParameterHandler<?> parseParameterAnnotation(
        int p, Type type, Annotation[] annotations, Annotation annotation) {
      if (annotation instanceof Url) {
        validateResolvableType(p, type);
        if (gotUrl) {
          throw parameterError(method, p, "Multiple @Url method annotations found.");
        }
        if (gotPath) {
          throw parameterError(method, p, "@Path parameters may not be used with @Url.");
        }
        if (gotQuery) {
          throw parameterError(method, p, "A @Url parameter must not come after a @Query.");
        }
        if (gotQueryName) {
          throw parameterError(method, p, "A @Url parameter must not come after a @QueryName.");
        }
        if (gotQueryMap) {
          throw parameterError(method, p, "A @Url parameter must not come after a @QueryMap.");
        }
        if (relativeUrl != null) {
          throw parameterError(method, p, "@Url cannot be used with @%s URL", httpMethod);
        }

        gotUrl = true;

        if (type == HttpUrl.class
            || type == String.class
            || type == URI.class
            || (type instanceof Class && "android.net.Uri".equals(((Class<?>) type).getName()))) {
          return new ParameterHandler.RelativeUrl(method, p);
        } else {
          throw parameterError(
              method,
              p,
              "@Url must be okhttp3.HttpUrl, String, java.net.URI, or android.net.Uri type.");
        }

      } else if (annotation instanceof Path) {
        validateResolvableType(p, type);
        if (gotQuery) {
          throw parameterError(method, p, "A @Path parameter must not come after a @Query.");
        }
        if (gotQueryName) {
          throw parameterError(method, p, "A @Path parameter must not come after a @QueryName.");
        }
        if (gotQueryMap) {
          throw parameterError(method, p, "A @Path parameter must not come after a @QueryMap.");
        }
        if (gotUrl) {
          throw parameterError(method, p, "@Path parameters may not be used with @Url.");
        }
        if (relativeUrl == null) {
          throw parameterError(
              method, p, "@Path can only be used with relative url on @%s", httpMethod);
        }
        gotPath = true;
        //注解类型为Path
        Path path = (Path) annotation;
        //获取注解里面value的值
        String name = path.value();
        validatePathName(p, name);

        Converter<?, String> converter = retrofit.stringConverter(type, annotations);
        return new ParameterHandler.Path<>(method, p, name, converter, path.encoded());

      } 
     
   ... 
   else if (annotation instanceof Body) {
        ...
        Converter<?, RequestBody> converter;
        try {
          //默认是GsonRequestBodyConverter
          converter = retrofit.requestBodyConverter(type, annotations, methodAnnotations);
        } catch (RuntimeException e) {
          // Wide exception range because factories are user code.
          throw parameterError(method, e, p, "Unable to create @Body converter for %s", type);
        }
        gotBody = true;
        return new ParameterHandler.Body<>(method, p, converter);

      }
      ....    
      //省略Query QueryName QueryMap Header HeaderMap Field FieldMap Part PartMap Body Tag
}
```
解析参数上的注解,这个注解可能的类型比较多,比如Path或者Query等等.所以parseParameterAnnotation方法里面有很多if..else..,
  我只列举了其中几种,其他的逻辑都差不多的,感兴趣可以阅读源码进行查看.
比如,我们就只分析一下Path的
```
gotPath = true;
Path path = (Path) annotation;
//获取注解里面value的值
String name = path.value();

Converter<?, String> converter = retrofit.stringConverter(type, annotations);
return new ParameterHandler.Path<>(name, converter, path.encoded());
```
如果是Path则将获取到的数据放到了ParameterHandler.Path中,如果是Query则将数据放到ParameterHandler.Query中.每个注解都有一个属于自己的类型.



ServiceMethod的构建
```
static <ResponseT, ReturnT> HttpServiceMethod<ResponseT, ReturnT> parseAnnotations(
      Retrofit retrofit, Method method, RequestFactory requestFactory) {
    boolean isKotlinSuspendFunction = requestFactory.isKotlinSuspendFunction;
    boolean continuationWantsResponse = false;
    boolean continuationBodyNullable = false;

    Annotation[] annotations = method.getAnnotations();
    //适配类型
    Type adapterType;
    if (isKotlinSuspendFunction) {
      //kotlin suspend方法 
      Type[] parameterTypes = method.getGenericParameterTypes();
      Type responseType =
          Utils.getParameterLowerBound(
              0, (ParameterizedType) parameterTypes[parameterTypes.length - 1]);
      if (getRawType(responseType) == Response.class && responseType instanceof ParameterizedType) {
        // Unwrap the actual body type from Response<T>.
        responseType = Utils.getParameterUpperBound(0, (ParameterizedType) responseType);
        continuationWantsResponse = true;
      } else {
        // figure out if type is nullable or not
        // Metadata metadata = method.getDeclaringClass().getAnnotation(Metadata.class)
        // Find the entry for method
        // Determine if return type is nullable or not
      }

      adapterType = new Utils.ParameterizedTypeImpl(null, Call.class, responseType);
      annotations = SkipCallbackExecutorImpl.ensurePresent(annotations);
    } else {
       //根据返回值类型确定CallAdapter  
      adapterType = method.getGenericReturnType();
    }
    // 获取传入的适配器 如果没有传入则使用默认的DefaultCallAdapterFactory,用于处理返回值为Call
    CallAdapter<ResponseT, ReturnT> callAdapter =
        createCallAdapter(retrofit, method, adapterType, annotations);
    //获取接口的返回值类型    
    Type responseType = callAdapter.responseType();
    if (responseType == okhttp3.Response.class) {
      throw methodError(
          method,
          "'"
              + getRawType(responseType).getName()
              + "' is not a valid response body type. Did you mean ResponseBody?");
    }
    if (responseType == Response.class) {
      throw methodError(method, "Response must include generic type (e.g., Response<String>)");
    }
    //support Unit for Kotlin?
    if (requestFactory.httpMethod.equals("HEAD") && !Void.class.equals(responseType)) {
      throw methodError(method, "HEAD method must use Void as response type.");
    }
    //获取转换器  示例传入的是GsonResponseBodyConverter
    Converter<ResponseBody, ResponseT> responseConverter =
        createResponseConverter(retrofit, method, responseType);

    okhttp3.Call.Factory callFactory = retrofit.callFactory;
    //构建不同的ServiceMethod子类
    if (!isKotlinSuspendFunction) {
      //不支持kotlin suspend
      return new CallAdapted<>(requestFactory, callFactory, responseConverter, callAdapter);
    } else if (continuationWantsResponse) {
      //response为 kotlin suspend
      return (HttpServiceMethod<ResponseT, ReturnT>)
          new SuspendForResponse<>(
              requestFactory,
              callFactory,
              responseConverter,
              (CallAdapter<ResponseT, Call<ResponseT>>) callAdapter);
    } else {
      //body类型为 kotlin suspend
      return (HttpServiceMethod<ResponseT, ReturnT>)
          new SuspendForBody<>(
              requestFactory,
              callFactory,
              responseConverter,
              (CallAdapter<ResponseT, Call<ResponseT>>) callAdapter,
              continuationBodyNullable);
    }
  }
```
然后ServiceMethod构建,就是将上面这些获取到的所有数据全部存进去,包括方法注解，参数注解，适配器callAdapter，转换器Converter
然后我们回到动态代码的那个方法,我已经放到下面来了.
```
//HttpServiceMethod
final @Nullable ReturnT invoke(Object[] args) {
    Call<ResponseT> call = new OkHttpCall<>(requestFactory, args, callFactory, responseConverter);
    return adapt(call, args);
  }
```
OkHttpCall是Retrofit中的一个类,最后我们将ServiceMethod里面的数据(注解，适配器，转换器等)和args(形参数据)都放进了OkHttpCall对象中.

serviceMethod.adapt最终返回的是将serviceMethod和okHttpCall绑在了一起,
```
//CallAdapted  这里是非kotlin suspend
 protected ReturnT adapt(Call<ResponseT> call, Object[] args) {
      return callAdapter.adapt(call);
    }
```
我初始化Retrofit时没有传addCallAdapterFactory(CallAdapterFactory),所以这里是默认的DefaultCallAdapterFactory,
  然后DefaultCallAdapterFactory的adapt方法是就是返回了一个ExecutorCallbackCall对象
```
//DefaultCallAdapterFactory  默认的executor是MainThreadExecutor
public Call<Object> adapt(Call<Object> call) {
        return executor == null ? call : new ExecutorCallbackCall<>(executor, call);
      }     
```
ExecutorCallbackCall就是将回调方法放入指定的executor中执行,后面有解析

到这里,网络请求的入参已经基本解析完了,其实还差一点点,下面会说到.把这些获取到的入参全部封装了起来

4. 请求网络
   我们的示例是从下面这段代码进行网络请求的
```
airticlesCall.enqueue(object : Callback<BaseData> {
    override fun onFailure(call: Call<BaseData>, t: Throwable) {
        t.printStackTrace()
        Log.e("xfhy", "请求失败")
    }
    override fun onResponse(call: Call<BaseData>, response: Response<BaseData>) {
        val body = response.body()
        Log.e("xfhy", "请求成功 ${body?.toString()}")
    }
})
```   
进行Call的enqueue方法,这里的Call其实是ExecutorCallbackCall对象,因为在上面的动态代理中返回了这个对象的实例,
所以就是调用的ExecutorCallbackCall的enqueue方法
//可以看一下CallAdapter相关.md

```
    public void enqueue(final Callback<T> callback) {
      Objects.requireNonNull(callback, "callback == null");
        //这里的delegate是之前传入的OkHttpCall对象
      delegate.enqueue(
          new Callback<T>() {
            public void onResponse(Call<T> call, final Response<T> response) {
              callbackExecutor.execute(
                  () -> {
                    if (delegate.isCanceled()) {
                      callback.onFailure(ExecutorCallbackCall.this, new IOException("Canceled"));
                    } else {
                      callback.onResponse(ExecutorCallbackCall.this, response);
                    }
                  });
            }

            public void onFailure(Call<T> call, final Throwable t) {
              callbackExecutor.execute(() -> callback.onFailure(ExecutorCallbackCall.this, t));
            }
          });
    }
```
ExecutorCallbackCall的enqueue方法中调用了之前传入的OkHttpCall的enqueue方法,代理模式
```
 public void enqueue(final Callback<T> callback) {
    Objects.requireNonNull(callback, "callback == null");

    okhttp3.Call call;
    Throwable failure;

    synchronized (this) {
      if (executed) throw new IllegalStateException("Already executed.");
      executed = true;
      //rawCall是okhttp的，所以最终的异步实现交由okhttp
      call = rawCall;
      failure = creationFailure;
      //在开始之前,需要构建okhttp3.Call对象
      if (call == null && failure == null) {
        try {
          call = rawCall = createRawCall();
        } catch (Throwable t) {
          throwIfFatal(t);
          failure = creationFailure = t;
        }
      }
    }

    if (failure != null) {
      callback.onFailure(this, failure);
      return;
    }

    if (canceled) {
      call.cancel();
    }

    call.enqueue(
        new okhttp3.Callback() {
          public void onResponse(okhttp3.Call call, okhttp3.Response rawResponse) {
            Response<T> response;
            try {
              response = parseResponse(rawResponse);
            } catch (Throwable e) {
              throwIfFatal(e);
              callFailure(e);
              return;
            }

            try {
              callback.onResponse(OkHttpCall.this, response);
            } catch (Throwable t) {
              throwIfFatal(t);
              t.printStackTrace(); //  this is not great
            }
          }

          @Override
          public void onFailure(okhttp3.Call call, IOException e) {
            callFailure(e);
          }

          private void callFailure(Throwable e) {
            try {
              callback.onFailure(OkHttpCall.this, e);
            } catch (Throwable t) {
              throwIfFatal(t);
              t.printStackTrace(); //  this is not great
            }
          }
        });
  }
```
上面一开始就需要构建OKHttp3的Call对象,因为最后还是需要用OkHttp来访问网络
```
  private final okhttp3.Call.Factory callFactory;
  private okhttp3.Call createRawCall() throws IOException {
    //callFactory 是okhttp3的类
    okhttp3.Call call = callFactory.newCall(requestFactory.create(args));
    if (call == null) {
      throw new NullPointerException("Call.Factory returned null.");
    }
    return call;
  }
```
requestFactory.create用于构建OkHttp的请求
```
okhttp3.Request create(Object[] args) throws IOException {
    @SuppressWarnings("unchecked") // It is an error to invoke a method with the wrong arg types.
    //这是之前创建的ParameterHandler 数组  里面装的是方法参数的注解value值
    ParameterHandler<Object>[] handlers = (ParameterHandler<Object>[]) parameterHandlers;

    int argumentCount = args.length;
    if (argumentCount != handlers.length) {
      ...
    }

    RequestBuilder requestBuilder =
        new RequestBuilder(
            httpMethod,
            baseUrl,
            relativeUrl,
            headers,
            contentType,
            hasBody,
            isFormEncoded,
            isMultipart);

    if (isKotlinSuspendFunction) {
      // The Continuation is the last parameter and the handlers array contains null at that index.
      argumentCount--;
    }

    List<Object> argumentList = new ArrayList<>(argumentCount);
    //因为上面示例的方法参数注解为Path,所以apply方法就是将url中的需要替换的id和page替换成真实的数据
    for (int p = 0; p < argumentCount; p++) {
      argumentList.add(args[p]);
      handlers[p].apply(requestBuilder, args[p]);
    }

    return requestBuilder.get().tag(Invocation.class, new Invocation(method, argumentList)).build();
  }  
```
在构建OkHttp的Call之前,需要将url啊那些东西全部搞好,比如示例中的参数注解是Path,那么就需要先将url中的id和page换成真实的数据放在那里.然后
requestBuilder.get()
```
Request.Builder get() {
    HttpUrl url;
    HttpUrl.Builder urlBuilder = this.urlBuilder;
    if (urlBuilder != null) {
      url = urlBuilder.build();
    } else {
      // No query parameters triggered builder creation, just combine the relative URL and base URL.
      //noinspection ConstantConditions Non-null if urlBuilder is null.
      url = baseUrl.resolve(relativeUrl);
      if (url == null) {
        throw new IllegalArgumentException(
            "Malformed URL. Base: " + baseUrl + ", Relative: " + relativeUrl);
      }
    }

    RequestBody body = this.body;
    if (body == null) {
      // Try to pull from one of the builders.
      if (formBuilder != null) {
        body = formBuilder.build();
      } else if (multipartBuilder != null) {
        body = multipartBuilder.build();
      } else if (hasBody) {
        // Body is absent, make an empty body.
        body = RequestBody.create(null, new byte[0]);
      }
    }

    MediaType contentType = this.contentType;
    if (contentType != null) {
      if (body != null) {
        body = new ContentTypeOverridingRequestBody(body, contentType);
      } else {
        headersBuilder.add("Content-Type", contentType.toString());
      }
    }
     //requestBuilder是Request.Builder对象,在构造方法里面就初始化好了的
    //这里就是正常的OkHttp的网络请求该干的事儿了   封装url,method
    return requestBuilder.url(url).headers(headersBuilder.build()).method(method, body);
  }
   然后Request构建出来
   public Request build() {
      if (url == null) throw new IllegalStateException("url == null");
      return new Request(this);
    }
  //okHttp3的request构造器  
  Request(Builder builder) {
    this.url = builder.url;
    this.method = builder.method;
    this.headers = builder.headers.build();
    this.body = builder.body;
    this.tags = Util.immutableMap(builder.tags);
  }    
```
到了这里,就是把之前获取的数据传入Request对象中,进行正常的OkHttp网络请求,构建一个Request对象.

然后通过这个Request对象创建Call对象,回到上面的OkHttpCall中的方法,只展示了剩下的逻辑
```
public void enqueue(final Callback<T> callback) {
    ...
    call.enqueue(
        new okhttp3.Callback() {
          public void onResponse(okhttp3.Call call, okhttp3.Response rawResponse) {
            Response<T> response;
            try {
              //解析响应
              response = parseResponse(rawResponse);
            } catch (Throwable e) {
              throwIfFatal(e);
              callFailure(e);
              return;
            }

            try {
              //这里是我们示例中传过来的那个CallBack对象,网络请求成功
              callback.onResponse(OkHttpCall.this, response);
            } catch (Throwable t) {
              throwIfFatal(t);
              t.printStackTrace(); //  this is not great
            }
          }

          @Override
          public void onFailure(okhttp3.Call call, IOException e) {
            //网络请求失败
            callFailure(e);
          }

          private void callFailure(Throwable e) {
            try {
              callback.onFailure(OkHttpCall.this, e);
            } catch (Throwable t) {
              throwIfFatal(t);
              t.printStackTrace(); //  this is not great
            }
          }
        });
  }
  
Response<T> parseResponse(okhttp3.Response rawResponse) throws IOException {
    ResponseBody rawBody = rawResponse.body();
    // Remove the body's source (the only stateful object) so we can pass the response along.
    rawResponse =
        rawResponse
            .newBuilder()
            .body(new NoContentResponseBody(rawBody.contentType(), rawBody.contentLength()))
            .build();

    int code = rawResponse.code();
    //失败的响应
    if (code < 200 || code >= 300) {
      try {
        // Buffer the entire body to avoid future I/O.
        ResponseBody bufferedBody = Utils.buffer(rawBody);
        return Response.error(bufferedBody, rawResponse);
      } finally {
        rawBody.close();
      }
    }
    //成功的响应，没有body
    if (code == 204 || code == 205) {
      rawBody.close();
      return Response.success(null, rawResponse);
    }
    //ExceptionCatchingResponseBody对网络响应的io进行了异常捕获
    ExceptionCatchingResponseBody catchingBody = new ExceptionCatchingResponseBody(rawBody);
    try {
     //使用responseConverter将原始的结果解析转换  将ResponseBody转为对应的类
      T body = responseConverter.convert(catchingBody);
      return Response.success(body, rawResponse);
    } catch (RuntimeException e) {
      // If the underlying source threw an exception, propagate that rather than indicating it was
      // a runtime exception.
      catchingBody.throwIfCaught();
      throw e;
    }
  }  
```

同步请求
```
  public Response<T> execute() throws IOException {
    okhttp3.Call call;

    synchronized (this) {
      if (executed) throw new IllegalStateException("Already executed.");
      executed = true;
      //获取okhttp的call
      call = getRawCall();
    }

    if (canceled) {
      call.cancel();
    }
    //同步执行，并解析response   同步执行是okhttp实现的
    return parseResponse(call.execute());
  }
  
private okhttp3.Call getRawCall() throws IOException {
    okhttp3.Call call = rawCall;
    //call已经初始化了，直接返回
    if (call != null) return call;

    // Re-throw previous failures if this isn't the first attempt.
    if (creationFailure != null) {
      if (creationFailure instanceof IOException) {
        throw (IOException) creationFailure;
      } else if (creationFailure instanceof RuntimeException) {
        throw (RuntimeException) creationFailure;
      } else {
        throw (Error) creationFailure;
      }
    }

    // 新建一个call
    try {
      return rawCall = createRawCall();
    } catch (RuntimeException | Error | IOException e) {
      throwIfFatal(e); // Do not assign a fatal error to creationFailure.
      creationFailure = e;
      throw e;
    }
  }  
```

5. 总结
   Retrofit主要是利用动态代理模式来实现了接口方法,根据这个方法获取了网络访问请求所有的入参,然后再将入参组装配置OkHttp的请求方式,
   最终实现利用OkHttp来请求网络.方便开发者使用.代码封装得及其好,厉害.
   