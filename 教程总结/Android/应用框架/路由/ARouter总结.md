
路由原理
主要是通过编译的时候通过APT扫描注解，并进行相应处理，通过javapoet库生成Java代码;
生成的代码
com.alibaba.android.arouter.routes.ARouter$$Root
com.alibaba.android.arouter.routes.ARouter$$Interceptors
com.alibaba.android.arouter.routes.ARouter$$Providers

主要步骤如下：
1 调用ARouter.init方法,在LogisticsCenter中生成三个文件,Group(IRouteGroup),Providers(IProviderGroup),Root(IRouteRoot),
使用Warehouse将文件保存到三个不同的HashMap中, Warehouse就相当于路由表, 保存着全部模块的跳转关系;
  通过DexFile扫描安装包获取APT生成的路由信息，然后使用SharePreference保存，在debug模式或新版本会重新扫描
```
class Warehouse {
    // Cache route and metas
    static Map<String, Class<? extends IRouteGroup>> groupsIndex = new HashMap<>();
    static Map<String, RouteMeta> routes = new HashMap<>();

    // Cache provider
    static Map<Class, IProvider> providers = new HashMap<>();
    static Map<String, RouteMeta> providersIndex = new HashMap<>();

    // Cache interceptor
    static Map<Integer, Class<? extends IInterceptor>> interceptorsIndex = new UniqueKeyTreeMap<>("More than one interceptors use same priority [%s]");
    static List<IInterceptor> interceptors = new ArrayList<>();
```
2 通过ARouter.navigation封装postcard对象;  
  1.是否存在预处理服务PretreatmentService
  2.给postCard设置路由信息,目的地，类型，优先级，uri参数，绿色通道
```
 LogisticsCenter.completion(postcard);
```
3 通过ARouter索引传递到LogisticsCenter(路由中转站),询问是否存在跳转对象;
    1.如果不存在，先查询NavigationCallback.onLast的局部降级，然后查询DegradeService.onLost的全局降级服务
4 判断是否绿色通行和是否能通过拦截服务;  
   没有绿色通行则按优先级执行拦截服务
5 全部通过就会调用ActivityCompat.startActivity方法来跳转到目的Activity
  如果设置了activity动画，也会通过Activity.overridePendingTransition设置动画


ARouter使用单例在组件和模块之间进行数据传递
```
private volatile static ARouter instance = null;
private volatile static boolean hasInit = false;
public static ARouter getInstance() {
    if (!hasInit) {
        throw new InitException("ARouter::Init::Invoke init(context) first!");
    } else {
        if (instance == null) {
            synchronized (ARouter.class) {
                if (instance == null) {
                    instance = new ARouter();
                }
            }
        }
        return instance;
    }
}
```

线程池的使用
默认的线程池
DefaultPoolExecutor.java
```
 public static DefaultPoolExecutor getInstance() {
        if (null == instance) {
            synchronized (DefaultPoolExecutor.class) {
                if (null == instance) {
                    instance = new DefaultPoolExecutor(
                            //CPU_COUNT + 1
                            INIT_THREAD_COUNT,
                            //CPU_COUNT + 1
                            MAX_THREAD_COUNT,
                            //30L
                            SURPLUS_THREAD_LIFE,
                            TimeUnit.SECONDS,
                            new ArrayBlockingQueue<Runnable>(64),
                            new DefaultThreadFactory());
                }
            }
        }
        return instance;
    }
}    
```    
1. Dex加载路由表的过程
   ClassUtils.getFileNameByPackageName
   线程池+CountDownLatch     CountDownLatch阻塞主线程
2. 拦截服务执行
   InterceptorServiceImpl.doInterceptions
   线程池+CountDownLatch完成所有拦截器异步执行   只执行了一个Runnable，CountDownLatch阻塞子线程