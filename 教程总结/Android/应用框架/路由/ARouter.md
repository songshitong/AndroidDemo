https://mp.weixin.qq.com/s/sFM3OCorZ8HzV0R89cUAEw

版本1.5.2

Arouter缺点：
1 没有结束activity的能力，只有跳转
2 没有处理onActivityResult
3 activity的跳转逻辑分散在各处，如果多次处理输入参数，容易造成异常，最好有个中转站，不用每次调用arouter封装参数 
只有通过传入requestCode，没有处理onActivityResult，可以通过添加一个隐藏的fragment做为中转和回调
```
arouter.navigation(Activity mContext, int requestCode)
```
https://blog.csdn.net/sinat_33680954/article/details/117322797
3 没有适配androidX的ActivityResultLauncher

初始化
```
fun initRouter(application: Application,isDebug: Boolean){
    if (isDebug){
        ARouter.openLog();
        ARouter.openDebug();
        //需要在init之前配置才有效
    }
    ARouter.init(application);
}

class AppModuleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initRouter(this,BuildConfig.DEBUG)
    }
}
```
使用  activity或者fragment都可以
```
@Route(path = "/login/LoginActivity")
class LoginActivity : AppCompatActivity() {

}
ARouter.getInstance().build("/login/LoginActivity").navigation()
```

使用拦截器
要想定义拦截器需要实现IInterceptor，添加@Interceptor，如登录校验，运行时权限等
关于注解的参数priority: 拦截器可以定义优先级，如果有多个拦截器，会依次执行拦截器    一个优先级只能有一个拦截器，priority 的值越小，拦截器的优先级就越高，越早执行
以运行时权限校验为例，代码如下
```
@Interceptor(priority = 0)
class PermissionInterceptor : IInterceptor {
    private var context: Context? = null
    private var postcard: Postcard? = null
    private var callback: InterceptorCallback? = null
    override fun init(context: Context) {
        this.context = context
        log("PermissionInterceptor.init")
    }

    override fun process(postcard: Postcard, callback: InterceptorCallback) {
        log("PermissionInterceptor.process")
        this.postcard = postcard
        this.callback = callback
        if (postcard.path == PATH_ACTIVITY_WEB) {
            log("PermissionInterceptor.process: path匹配，开始校验运行时权限")
            requestMyPermissions(
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                )
            ) {
                if (it) {
                    log("允许了权限申请")
                    callback.onContinue(postcard)
                } else {
                    log("拒绝了权限申请")
                }
            }
        }else{
            log("PermissionInterceptor.process: path不匹配，无需拦截")
            callback.onContinue(postcard)
        }
    }
}
```
拦截器和服务的异同
拦截器和服务所需要实现的接口不同，但是结构类似，都存在 init(Context context) 方法，但是两者的调用时机不同；
1 拦截器因为其特殊性，会被任何一次路由所触发，拦截器会在ARouter初始化的时候异步初始化，如果第一次路由的时候拦截器还没有初始化结束，
 路由会等待，直到初始化完成；
2 服务没有该限制，某一服务可能在App整个生命周期中都不会用到，所以服务只有被调用的时候才会触发初始化操作；

预处理服务
在路由navigation之前进行干扰路由，需要实现PretreatmentService接口,
重写onPretreatment方法，跳转前预处理，如果需要自行处理跳转，该方法返回 false 即可；
拦截器功能和预处理服务功能是有点像的，只不过预处理服务是早于拦截器；
```
@Route(path = "/pretreatment/test")
class TestPretreatmentServiceImpl : PretreatmentService {
    override fun init(context: Context?) {
        log("TestPretreatmentServiceImpl.init")
    }
    override fun onPretreatment(context: Context?, postcard: Postcard?): Boolean {
        log("TestPretreatmentServiceImpl.onPretreatment")
        if (postcard?.path == PATH_ACTIVITY_WEB) {
            if (!ApplicationUtil.instance.isLogin) {
                Toast.makeText(
                    ApplicationUtil.instance.getAppContext(),
                    "还没有登录哦",
                    Toast.LENGTH_SHORT
                ).show()
                routerNavigation(PATH_ACTIVITY_LOGIN)
                return false
            }
        }
        return true
    }
}
```

解析自定义对象
1 要想传递自定义对象还需要实现一个SerializationService
2 通过Gson或Fastjson解析
```
Route(path = "/serialization/gson")//这里和之前的路由定义规则一样，自己控制好group不要重复就行
class GsonSerializationServiceImpl : SerializationService {
    override fun init(context: Context?) {
        log("GsonSerializationServiceImpl.init")
    }

    override fun <T : Any?> json2Object(input: String?, clazz: Class<T>?): T? {
        log("GsonSerializationServiceImpl.json2Object")
        return GsonUtils.fromJson(input, clazz)
    }

    override fun object2Json(instance: Any?): String {
        log("GsonSerializationServiceImpl.object2Json")
        return GsonUtils.toJson(instance)
    }

    override fun <T : Any?> parseObject(input: String?, clazz: Type?): T? {
        log("GsonSerializationServiceImpl.parseObject")
        return GsonUtils.fromJson(input, clazz)
    }
}
```


路由中的分组概念
SDK中针对所有的路径(/test/1 /test/2)进行分组，分组只有在分组中的某一个路径第一次被访问的时候，该分组才会被初始化
可以通过 @Route 注解主动指定分组，否则使用路径中第一段字符串(/*/)作为分组
注意：一旦主动指定分组之后，应用内路由需要使用 ARouter.getInstance().build(path, group) 进行跳转，手动指定分组，否则无法找到
@Route(path = "/test/1", group = "app")

截器和服务的异同
拦截器和服务所需要实现的接口不同，但是结构类似，都存在 init(Context context) 方法，但是两者的调用时机不同
拦截器因为其特殊性，会被任何一次路由所触发，拦截器会在ARouter初始化的时候异步初始化，如果第一次路由的时候拦截器还没有初始化结束，
  路由会等待，直到初始化完成。
服务没有该限制，某一服务可能在App整个生命周期中都不会用到，所以服务只有被调用的时候才会触发初始化操作

路由原理
主要是通过编译的时候通过APT扫描注解，并进行相应处理，通过javapoet库生成Java代码;

主要步骤如下：
1 调用ARouter.init方法,在LogisticsCenter中利用DexFile扫描class文件,搜索以"com.alibaba.android.arouter.routes"的文件，
  找到Group(IRouteGroup),Providers(IProviderGroup),Root(IRouteRoot),
  使用Warehouse将其保存到三个不同的HashMap中, Warehouse就相当于路由表, 保存着全部模块的跳转关系;
  Interceptors拦截器，Providers是定义的服务，Root路由Group
2 通过ARouter.navigation封装postcard对象;
3 通过ARouter索引传递到LogisticsCenter(路由中转站),询问是否存在跳转对象;
4 判断是否绿色通行和是否能通过拦截服务;
5 全部通过就会调用ActivityCompat.startActivity方法来跳转到目的Activity；

所以,ARouter实际还是使用原生的Framework机制startActivity,只是通过apt注解的形式制造出跳转规则,并人为的拦截跳转和设置跳转条件;

先看下初始化的过程   ARouter的使用是一个单例，保证跨模块的数据通信，这里的初始化也是_ARouter里面的静态变量
ARouter.java
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

 public static void init(Application application) {
        if (!hasInit) {
            logger = _ARouter.logger;
            _ARouter.logger.info(Consts.TAG, "ARouter init start.");
            //真正的初始化方法
            hasInit = _ARouter.init(application);
            //初始化成功
            if (hasInit) {
                _ARouter.afterInit();
            }

            _ARouter.logger.info(Consts.TAG, "ARouter init over.");
        }
    }
```
_ARouter.java
```
   protected static synchronized boolean init(Application application) {
        mContext = application;
        //传入application和executor进行初始化
        //线程池为ThreadPoolExecutor executor = DefaultPoolExecutor.getInstance();  可以通过ARouter.setExecutor()进行设置
        LogisticsCenter.init(mContext, executor);
        logger.info(Consts.TAG, "ARouter init success!");
        hasInit = true;
        mHandler = new Handler(Looper.getMainLooper());

        return true;
    }
```
Logistics   后勤;物流;组织工作

LogisticsCenter.init代码如下，主要是负责加载路由表
LogisticsCenter.java
```
 public synchronized static void init(Context context, ThreadPoolExecutor tpe) throws HandlerException {
        mContext = context;
        executor = tpe;

        try {
            long startInit = System.currentTimeMillis();
            //load by plugin first
            //判断是不是通过arouter-auto-register自动加载路由表
            loadRouterMap();
            if (registerByPlugin) {
                logger.info(TAG, "Load router map by arouter-auto-register plugin.");
            } else {
                //不是通过插件加载路由表
                
                //声明路由表routerMap
                Set<String> routerMap;

                // It will rebuild router map every times when debuggable.  debug模式每次都创建路由表
                // PackageUtils使用SharedPreferences来保存和对比版本号
                if (ARouter.debuggable() || PackageUtils.isNewVersion(context)) {
                    logger.info(TAG, "Run with debug mode or new install, rebuild router map.");
                    // These class was generated by arouter-compiler.
                    //遍历dex中所有的className,过滤出前缀为com.alibaba.android.arouter.routes，放到set集合里面
                    //ROUTE_ROOT_PAKCAGE = "com.alibaba.android.arouter.routes";
                    routerMap = ClassUtils.getFileNameByPackageName(mContext, ROUTE_ROOT_PAKCAGE);
                    if (!routerMap.isEmpty()) {
                      //将routerMap放到sp中，方便下次直接取
                        context.getSharedPreferences(AROUTER_SP_CACHE_KEY, Context.MODE_PRIVATE).edit().putStringSet(AROUTER_SP_KEY_MAP, routerMap).apply();
                    }
                    //更新最新版本
                    PackageUtils.updateVersion(context);    // Save new version name when router map update finishes.
                } else {
                    //直接在sp中取缓存
                    logger.info(TAG, "Load router map from cache.");
                    routerMap = new HashSet<>(context.getSharedPreferences(AROUTER_SP_CACHE_KEY, Context.MODE_PRIVATE).getStringSet(AROUTER_SP_KEY_MAP, new HashSet<String>()));
                }

                logger.info(TAG, "Find router map finished, map size = " + routerMap.size() + ", cost " + (System.currentTimeMillis() - startInit) + " ms.");
                startInit = System.currentTimeMillis();
                //遍历路由表
                for (String className : routerMap) {
                    if (className.startsWith(ROUTE_ROOT_PAKCAGE + DOT + SDK_NAME + SEPARATOR + SUFFIX_ROOT)) {
                        //将前缀com.alibaba.android.arouter.routes.ARouter$$Root的class放到Warehouse.groupsIndex
                        // This one of root elements, load root.
                        ((IRouteRoot) (Class.forName(className).getConstructor().newInstance())).loadInto(Warehouse.groupsIndex);
                    } else if (className.startsWith(ROUTE_ROOT_PAKCAGE + DOT + SDK_NAME + SEPARATOR + SUFFIX_INTERCEPTORS)) {
                        // Load interceptorMeta
                        // 将前缀com.alibaba.android.arouter.routes.ARouter$$Interceptors的class放到Warehouse.interceptorsIndex
                        ((IInterceptorGroup) (Class.forName(className).getConstructor().newInstance())).loadInto(Warehouse.interceptorsIndex);
                    } else if (className.startsWith(ROUTE_ROOT_PAKCAGE + DOT + SDK_NAME + SEPARATOR + SUFFIX_PROVIDERS)) {
                        // Load providerIndex
                        //将前缀com.alibaba.android.arouter.routes.ARouter$$Providers的class放到Warehouse.providersIndex
                        ((IProviderGroup) (Class.forName(className).getConstructor().newInstance())).loadInto(Warehouse.providersIndex);
                    }
                }
            }
           ...
        } catch (Exception e) {
            throw new HandlerException(TAG + "ARouter init logistics center exception! [" + e.getMessage() + "]");
        }
    }
 
   private static void loadRouterMap() {
        registerByPlugin = false;
        // auto generate register code by gradle plugin: arouter-auto-register
        // looks like below:
        // registerRouteRoot(new ARouter..Root..modulejava());
        // registerRouteRoot(new ARouter..Root..modulekotlin());
    }    
```

看一下route的三个接口
```
//根路由
public interface IRouteRoot {
    void loadInto(Map<String, Class<? extends IRouteGroup>> routes);
}
public interface IRouteGroup {
    void loadInto(Map<String, RouteMeta> atlas);
}
//基础的路由信息  子类是Postcard 明信片
public class RouteMeta {
    private RouteType type;         // Type of route
    private Element rawType;        // Raw type of route
    private Class<?> destination;   // Destination
    private String path;            // Path of route
    private String group;           // Group of route
    private int priority = -1;      // The smaller the number, the higher the priority
    private int extra;              // Extra data
    private Map<String, Integer> paramsType;  // Param type
    private String name;
    private Map<String, Autowired> injectConfig; 
}

//拦截器组
public interface IInterceptorGroup {
    void loadInto(Map<Integer, Class<? extends IInterceptor>> interceptor);
}
public interface IProvider {
    void init(Context context);
}

//拦截器接口
public interface IInterceptor extends IProvider {
    void process(Postcard postcard, InterceptorCallback callback);
}

//服务组
public interface IProviderGroup {
    void loadInto(Map<String, RouteMeta> providers);
}
```

warehouse用于缓存路由，拦截器，服务provider
warehouse  仓库;货栈;货仓
```
class Warehouse {
    // Cache route and metas
    static Map<String, Class<? extends IRouteGroup>> groupsIndex = new HashMap<>();
    static Map<String, RouteMeta> routes = new HashMap<>();

    // Cache provider
    static Map<Class, IProvider> providers = new HashMap<>();
    static Map<String, RouteMeta> providersIndex = new HashMap<>();

    // Cache interceptor    优先级,拦截器     UniqueKeyTreeMap的父类是TreeMap，内部对元素进行了排序
    static Map<Integer, Class<? extends IInterceptor>> interceptorsIndex = new UniqueKeyTreeMap<>("More than one interceptors use same priority [%s]");
    static List<IInterceptor> interceptors = new ArrayList<>();

}
```

从Dex加载路由表的过程   有可能阻塞主线程
ClassUtils.java
```
public static Set<String> getFileNameByPackageName(Context context, final String packageName) throws PackageManager.NameNotFoundException, IOException, InterruptedException {
        final Set<String> classNames = new HashSet<>();
        //获取所有的dex路径
        List<String> paths = getSourcePaths(context);
        final CountDownLatch parserCtl = new CountDownLatch(paths.size());

        for (final String path : paths) {
            //每个dex一个Runnable 使用CountDownLatch做同步  
            DefaultPoolExecutor.getInstance().execute(new Runnable() {
                @Override
                public void run() {
                    DexFile dexfile = null;
                    try {
                       //EXTRACTED_NAME_EXT = ".classes"
                        if (path.endsWith(EXTRACTED_SUFFIX)) {
                            //NOT use new DexFile(path), because it will throw "permission error in /data/dalvik-cache"
                            //使用DexFile加载
                            dexfile = DexFile.loadDex(path, path + ".tmp", 0);
                        } else {
                            dexfile = new DexFile(path);
                        }

                        Enumeration<String> dexEntries = dexfile.entries();
                        while (dexEntries.hasMoreElements()) {
                           //遍历dex里面的class
                            String className = dexEntries.nextElement();
                            if (className.startsWith(packageName)) {
                                classNames.add(className);
                            }
                        }
                    } catch (Throwable ignore) {
                        Log.e("ARouter", "Scan map file in dex files made error.", ignore);
                    } finally {
                        if (null != dexfile) {
                            try {
                                dexfile.close();
                            } catch (Throwable ignore) {
                            }
                        }

                        parserCtl.countDown();
                    }
                }
            });
        }
        parserCtl.await();
        ... 
        return classNames;
    }

//获取所有dex路径
public static List<String> getSourcePaths(Context context) throws PackageManager.NameNotFoundException, IOException {
        ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0);
        File sourceApk = new File(applicationInfo.sourceDir);

        List<String> sourcePaths = new ArrayList<>();
        sourcePaths.add(applicationInfo.sourceDir); //add the default apk path

        //the prefix of extracted file, ie: test.classes
        String extractedFilePrefix = sourceApk.getName() + EXTRACTED_NAME_EXT;

//        如果VM已经支持了MultiDex，就不要去Secondary Folder加载 Classesx.zip了，那里已经么有了
//        通过是否存在sp中的multidex.version是不准确的，因为从低版本升级上来的用户，是包含这个sp配置的
        if (!isVMMultidexCapable()) {
            //the total dex numbers
            //KEY_DEX_NUMBER = "dex.number"
            int totalDexNumber = getMultiDexPreferences(context).getInt(KEY_DEX_NUMBER, 1);
            //SECONDARY_FOLDER_NAME = "code_cache" + File.separator + "secondary-dexes"
            File dexDir = new File(applicationInfo.dataDir, SECONDARY_FOLDER_NAME);

            for (int secondaryNumber = 2; secondaryNumber <= totalDexNumber; secondaryNumber++) {
                //for each dex file, ie: test.classes2.zip, test.classes3.zip...  
                //EXTRACTED_SUFFIX = ".zip"
                String fileName = extractedFilePrefix + secondaryNumber + EXTRACTED_SUFFIX;
                File extractedFile = new File(dexDir, fileName);
                if (extractedFile.isFile()) {
                    //添加类似test.classes2.zip的路径
                    sourcePaths.add(extractedFile.getAbsolutePath());
                    //we ignore the verify zip part
                } else {
                    throw new IOException("Missing extracted secondary dex file '" + extractedFile.getPath() + "'");
                }
            }
        }
        //debug模式添加 instant run
        if (ARouter.debuggable()) { // Search instant run support only debuggable
            sourcePaths.addAll(tryLoadInstantRunDexFile(applicationInfo));
        }
        return sourcePaths;
    } 
    
 private static SharedPreferences getMultiDexPreferences(Context context) {
        //PREFS_FILE = "multidex.version"
        return context.getSharedPreferences(PREFS_FILE, Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB ? Context.MODE_PRIVATE : Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
    }       
```
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
    
 //线程执行结束，顺便看一下有么有什么乱七八糟的异常    future运行可能抛出异常
 protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        if (t == null && r instanceof Future<?>) {
            try {
                ((Future<?>) r).get();
            } catch (CancellationException ce) {
                t = ce;
            } catch (ExecutionException ee) {
                t = ee.getCause();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt(); // ignore/reset
            }
        }
        if (t != null) {
            ARouter.logger.warning(Consts.TAG, "Running task appeared exception! Thread [" + Thread.currentThread().getName() + "], because [" + t.getMessage() + "]\n" + TextUtils.formatStackTrace(t.getStackTrace()));
        }
    }    
```

_ARouter.afterInit
```
  static void afterInit() {
        // Trigger interceptor init, use byName.
        // 这里是获取拦截器的服务实例  构建的服务是InterceptorServiceImpl，后续路由跳转时进行拦截处理
        interceptorService = (InterceptorService) ARouter.getInstance().build("/arouter/service/interceptor").navigation();
    }
//拦截器服务    
public interface InterceptorService extends IProvider {
    //开始做拦截处理
    void doInterceptions(Postcard postcard, InterceptorCallback callback);
}    
```



ARouter.build
再来看看跳转时调用的ARouter.build方法，代码如下
ARouter.java
```
 public Postcard build(Uri url) {
        return _ARouter.getInstance().build(url);
    }
```
_ARouter.java
```
    protected Postcard build(Uri uri) {
        if (null == uri || TextUtils.isEmpty(uri.toString())) {
            throw new HandlerException(Consts.TAG + "Parameter invalid!");
        } else {
            //调用navigation生成PathReplaceService实例
            PathReplaceService pService = ARouter.getInstance().navigation(PathReplaceService.class);
            //如果用户实现了PathReplaceService，则对其跳转的path或uri进行处理
            if (null != pService) {
               //通过PathReplaceService处理path
                uri = pService.forUri(uri);
            }
            //extractGroup就是截取出group
            return new Postcard(uri.getPath(), extractGroup(uri.getPath()), uri, null);
        }
    }
   private String extractGroup(String path) {
        ...
        try {
            String defaultGroup = path.substring(1, path.indexOf("/", 1));
            if (TextUtils.isEmpty(defaultGroup)) {
                throw new HandlerException(Consts.TAG + "Extract the default group failed! There's nothing between 2 '/'!");
            } else {
                return defaultGroup;
            }
        } catch (Exception e) {
          ...
        }
    }    
```

PathReplaceService.java  
```
public interface PathReplaceService extends IProvider {
    String forString(String path);

    Uri forUri(Uri uri);
}
```
PathReplaceService是用户处理路由的服务，例如重定向等
```
@Route(path = "/pathReplace/test")
class TestPathReplaceServiceImpl : PathReplaceService {
    override fun init(context: Context?) {
        log("TestPathReplaceServiceImpl.init")
    }

    override fun forString(path: String): String {
        log("TestPathReplaceServiceImpl.replacePath")
        // 按照一定的规则处理之后返回处理后的结果
        return if (path == PATH_ACTIVITY_MAIN) PATH_ACTIVITY_LOGIN else path
    }

    override fun forUri(uri: Uri?): Uri? {
        log("TestPathReplaceServiceImpl.replaceUri")
        return uri // 按照一定的规则处理之后返回处理后的结果
    }
}
```

Postcard保存了路由跳转需要的所有信息，并且有一系列withXXX方法供我们设置
```
public final class Postcard extends RouteMeta {
    // Base
    private Uri uri;
    private Object tag;             // A tag prepare for some thing wrong. inner params, DO NOT USE!
    private Bundle mBundle;         // Data to transform
    private int flags = 0;         // Flags of route
    private int timeout = 300;      // Navigation timeout, TimeUnit.Second
    private IProvider provider;     // It will be set value, if this postcard was provider.
    private boolean greenChannel;
    private SerializationService serializationService;
    private Context context;        // May application or activity, check instance type before use it.
    private String action;

    // Animation
    private Bundle optionsCompat;    // The transition animation of activity
    private int enterAnim = -1;
    private int exitAnim = -1;
    
    public Object navigation() {
        return navigation(null);
    }
    
    public Object navigation(Context context) {
        return navigation(context, null);
    }
    
    public Object navigation(Context context, NavigationCallback callback) {
        return ARouter.getInstance().navigation(context, this, -1, callback);
    }
    
    public Object navigation(Context context, NavigationCallback callback) {
        return ARouter.getInstance().navigation(context, this, -1, callback);
    }
    
    public Postcard with(Bundle bundle) {
        if (null != bundle) {
            mBundle = bundle;
        }
        return this;
    }
 }
```


ARouter.navigation
build完之后就要调用navigation来执行跳转了
```
  public Object navigation(Context mContext, Postcard postcard, int requestCode, NavigationCallback callback) {
        return _ARouter.getInstance().navigation(mContext, postcard, requestCode, callback);
    }   
```

_ARouter.java
```
protected Object navigation(final Context context, final Postcard postcard, final int requestCode, final NavigationCallback callback) {
        //获取PretreatmentService实例
        PretreatmentService pretreatmentService = ARouter.getInstance().navigation(PretreatmentService.class);
        //如果PretreatmentService实例存在，即用户实现了预处理服务，并且onPretreatment返回了false,则拦截本次跳转
        if (null != pretreatmentService && !pretreatmentService.onPretreatment(context, postcard)) {
            // Pretreatment failed, navigation canceled.
            return null;
        }

        // Set context to postcard.
        postcard.setContext(null == context ? mContext : context);

        try {
            //最主要是调用了这一行  物流中心完善明信片的信息
            LogisticsCenter.completion(postcard);
        } catch (NoRouteFoundException ex) {
            logger.warning(Consts.TAG, ex.getMessage());
            //openDebug则toast一些提示
            if (debuggable()) {
                // Show friendly tips for user.
                runInMainThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mContext, "There's no route matched!\n" +
                                " Path = [" + postcard.getPath() + "]\n" +
                                " Group = [" + postcard.getGroup() + "]", Toast.LENGTH_LONG).show();
                    }
                });
            }
            //如果NavigationCallback存在则调用其onLost,也就是局部降级
            if (null != callback) {
                callback.onLost(postcard);
            } else {
                // NavigationCallback不存在则看看有没有实现DegradeService，就是之前讲的全局降级
               // 当局部降级存在，则全局降级就在本次路由就不会生效了
                // No callback for this invoke, then we use the global degrade service.
                DegradeService degradeService = ARouter.getInstance().navigation(DegradeService.class);
                if (null != degradeService) {
                    degradeService.onLost(context, postcard);
                }
            }

            return null;
        }
        //找到路由的回调 
        if (null != callback) {
            callback.onFound(postcard);
        }

        if (!postcard.isGreenChannel()) {   // It must be run in async thread, maybe interceptor cost too mush time made ANR.
           //如果没有开启绿色通道，则调用拦截器服务，
           //拦截器服务是在上面的_ARouter.afterInit中初始化的
            interceptorService.doInterceptions(postcard, new InterceptorCallback() {
                /**
                 * Continue process
                 *
                 * @param postcard route meta
                 */
                @Override
                public void onContinue(Postcard postcard) {
                    //拦截器回调onContinue，继续执行_navigation
                    _navigation(postcard, requestCode, callback);
                }

                /**
                 * Interrupt process, pipeline will be destory when this method called.
                 *
                 * @param exception Reson of interrupt.
                 */
                @Override
                public void onInterrupt(Throwable exception) {
                    //如果有拦截器回调onInterrupt，继续往外回调onInterrupt
                    if (null != callback) {
                        callback.onInterrupt(postcard);
                    }

                    logger.info(Consts.TAG, "Navigation failed, termination by interceptor : " + exception.getMessage());
                }
            });
        } else {
            return _navigation(postcard, requestCode, callback);
        }

        return null;
    }
```
PretreatmentService预处理服务，路由跳转前的处理
```
public interface PretreatmentService extends IProvider {
    boolean onPretreatment(Context context, Postcard postcard);
}
```
DegradeService  全局降级服务，找不到对应路由的处理
```
public interface DegradeService extends IProvider {
    void onLost(Context context, Postcard postcard);
}
```

LogisticsCenter.completion
```
public synchronized static void completion(Postcard postcard) {
        ...
        //根据path获取RouteMeta信息
        RouteMeta routeMeta = Warehouse.routes.get(postcard.getPath());
        //路由不存在或没有加载
        if (null == routeMeta) {
            // Maybe its does't exist, or didn't load.
            if (!Warehouse.groupsIndex.containsKey(postcard.getGroup())) {
               //如果group没找到
                throw new NoRouteFoundException(TAG + "There is no route match the path [" + postcard.getPath() + "], in group [" + postcard.getGroup() + "]");
            } else {
                // Load route and cache it into memory, then delete from metas.
                try {
                    if (ARouter.debuggable()) {
                        logger.debug(TAG, String.format(Locale.getDefault(), "The group [%s] starts loading, trigger by [%s]", postcard.getGroup(), postcard.getPath()));
                    }
                    //动态添加RouteGroup到Warehouse.groupsIndex和Warehouse.routes
                    addRouteGroupDynamic(postcard.getGroup(), null);

                    if (ARouter.debuggable()) {
                        logger.debug(TAG, String.format(Locale.getDefault(), "The group [%s] has already been loaded, trigger by [%s]", postcard.getGroup(), postcard.getPath()));
                    }
                } catch (Exception e) {
                    throw new HandlerException(TAG + "Fatal exception when loading group meta. [" + e.getMessage() + "]");
                }
                //重试
                completion(postcard);   // Reload
            }
        } else {
            //给postCard设置路由信息
            postcard.setDestination(routeMeta.getDestination());
            postcard.setType(routeMeta.getType());
            postcard.setPriority(routeMeta.getPriority());
            postcard.setExtra(routeMeta.getExtra());

            Uri rawUri = postcard.getUri();
            //分解uri中的参数 设置给postCard
            if (null != rawUri) {   // Try to set params into bundle.
                Map<String, String> resultMap = TextUtils.splitQueryParameters(rawUri);
                Map<String, Integer> paramsType = routeMeta.getParamsType();

                if (MapUtils.isNotEmpty(paramsType)) {
                    // Set value by its type, just for params which annotation by @Param
                    for (Map.Entry<String, Integer> params : paramsType.entrySet()) {
                        //根据路由需要的参数设置
                        setValue(postcard,
                                params.getValue(),
                                params.getKey(),
                                resultMap.get(params.getKey()));
                    }

                    // Save params name which need auto inject.
                    postcard.getExtras().putStringArray(ARouter.AUTO_INJECT, paramsType.keySet().toArray(new String[]{}));
                }

                // Save raw uri
                postcard.withString(ARouter.RAW_URI, rawUri.toString());
            }
            //如果type是Fragment或者IProvider则开启greenChannel，也就是不用拦截器
            switch (routeMeta.getType()) {
                case PROVIDER:  // if the route is provider, should find its instance
                    // Its provider, so it must implement IProvider
                    Class<? extends IProvider> providerMeta = (Class<? extends IProvider>) routeMeta.getDestination();
                    IProvider instance = Warehouse.providers.get(providerMeta);
                    if (null == instance) { // There's no instance of this provider
                       //没有对应的provider实例，使用反射进行初始化，保存到Warehouse
                        IProvider provider;
                        try {
                            provider = providerMeta.getConstructor().newInstance();
                            provider.init(mContext);
                            Warehouse.providers.put(providerMeta, provider);
                            instance = provider;
                        } catch (Exception e) {
                            logger.error(TAG, "Init provider failed!", e);
                            throw new HandlerException("Init provider failed!");
                        }
                    }
                    postcard.setProvider(instance);
                    postcard.greenChannel();    // Provider should skip all of interceptors
                    break;
                case FRAGMENT:
                    postcard.greenChannel();    // Fragment needn't interceptors
                default:
                    break;
            }
        }
    }

  public synchronized static void addRouteGroupDynamic(String groupName, IRouteGroup group) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        if (Warehouse.groupsIndex.containsKey(groupName)){
            // If this group is included, but it has not been loaded
            // load this group first, because dynamic route has high priority.
            //动态路由有高优先级，先加载一次，然后移除，最后才覆盖旧的
            Warehouse.groupsIndex.get(groupName).getConstructor().newInstance().loadInto(Warehouse.routes);
            Warehouse.groupsIndex.remove(groupName);
        }

        // cover old group.
        if (null != group) {
            group.loadInto(Warehouse.routes);
        }
    }    
```

真正的跳转方法
之前的_navigation最后一行调用了_navigation(postcard, requestCode, callback)，代码如下，可以看到其是真正处理跳转的方法
_ARouter.java
```
private Object _navigation(final Postcard postcard, final int requestCode, final NavigationCallback callback) {
        final Context currentContext = postcard.getContext();
        //根据路由类型进行跳转 
        switch (postcard.getType()) {
            case ACTIVITY:
                // Build intent
                final Intent intent = new Intent(currentContext, postcard.getDestination());
                intent.putExtras(postcard.getExtras());

                // Set flags.
                int flags = postcard.getFlags();
                if (0 != flags) {
                    intent.setFlags(flags);
                }

                // Non activity, need FLAG_ACTIVITY_NEW_TASK
                if (!(currentContext instanceof Activity)) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }

                // Set Actions
                String action = postcard.getAction();
                if (!TextUtils.isEmpty(action)) {
                    intent.setAction(action);
                }
                //activity路由在主线程执行startActivity
                // Navigation in main looper.
                runInMainThread(new Runnable() {
                    @Override
                    public void run() {
                        startActivity(requestCode, currentContext, intent, postcard, callback);
                    }
                });

                break;
            case PROVIDER:
                //provider路由返回LogisticsCenter.completion中从Warehouse拿到或初始化的provider实例
                return postcard.getProvider();
            //广播，内容提供者  返回目标实例   
            case BOARDCAST:
            case CONTENT_PROVIDER:
            case FRAGMENT:
                Class<?> fragmentMeta = postcard.getDestination();
                try {
                    Object instance = fragmentMeta.getConstructor().newInstance();
                    //fragment执行setArguments，然后返回对应的实例 
                    if (instance instanceof Fragment) {
                        ((Fragment) instance).setArguments(postcard.getExtras());
                    } else if (instance instanceof android.support.v4.app.Fragment) {
                        ((android.support.v4.app.Fragment) instance).setArguments(postcard.getExtras());
                    }

                    return instance;
                } catch (Exception ex) {
                    logger.error(Consts.TAG, "Fetch fragment instance error, " + TextUtils.formatStackTrace(ex.getStackTrace()));
                }
            //METHOD，SERVICE路由什么都不做    
            case METHOD:
            case SERVICE:
            default:
                return null;
        }

        return null;
    }
    
 private void startActivity(int requestCode, Context currentContext, Intent intent, Postcard postcard, NavigationCallback callback) {
        if (requestCode >= 0) {  // Need start for result
            //需要启动结果
            if (currentContext instanceof Activity) {
               //使用的是android.support.v4包
                ActivityCompat.startActivityForResult((Activity) currentContext, intent, requestCode, postcard.getOptionsBundle());
            } else {
                logger.warning(Consts.TAG, "Must use [navigation(activity, ...)] to support [startActivityForResult]");
            }
        } else {
            ActivityCompat.startActivity(currentContext, intent, postcard.getOptionsBundle());
        }

        if ((-1 != postcard.getEnterAnim() && -1 != postcard.getExitAnim()) && currentContext instanceof Activity) {    // Old version.
            //activity 动画
            ((Activity) currentContext).overridePendingTransition(postcard.getEnterAnim(), postcard.getExitAnim());
        }
        //回调onArrival
        if (null != callback) { // Navigation over.
            callback.onArrival(postcard);
        }
    }    
```
至此整个navigation过程就完成了



看一下通过Class直接获取服务的过程
```
protected <T> T navigation(Class<? extends T> service) {
        try {
            //从Warehouse中查询路由信息，然后构建postcard
            Postcard postcard = LogisticsCenter.buildProvider(service.getName());

            // Compatible 1.0.5 compiler sdk.
            // Earlier versions did not use the fully qualified name to get the service
            if (null == postcard) {
                // No service, or this service in old version.
                postcard = LogisticsCenter.buildProvider(service.getSimpleName());
            }

            if (null == postcard) {
                return null;
            }
            //设置context
            // Set application to postcard.
            postcard.setContext(mContext);
            //完善postcard的信息
            LogisticsCenter.completion(postcard);
            //返回provider的实例
            return (T) postcard.getProvider();
        } catch (NoRouteFoundException ex) {
            logger.warning(Consts.TAG, ex.getMessage());
            return null;
        }
    }
```
LogisticsCenter.java
```
public static Postcard buildProvider(String serviceName) {
        RouteMeta meta = Warehouse.providersIndex.get(serviceName);
        if (null == meta) {
            return null;
        } else {
            return new Postcard(meta.getPath(), meta.getGroup());
        }
    }
```

InterceptorServiceImpl
拦截器服务interceptorService实现类为InterceptorServiceImpl，在其init方法中,通过线程池遍历了 Warehouse.interceptorsIndex
类型为Map<Integer, Class<? extends IInterceptor>>，创建拦截器实例放到集合List<IInterceptor>中，也就是Warehouse.interceptors
InterceptorServiceImpl.java
```
public void init(final Context context) {
        LogisticsCenter.executor.execute(new Runnable() {
            @Override
            public void run() {
                if (MapUtils.isNotEmpty(Warehouse.interceptorsIndex)) {
                    //遍历Warehouse.interceptorsIndex
                    for (Map.Entry<Integer, Class<? extends IInterceptor>> entry : Warehouse.interceptorsIndex.entrySet()) {
                        Class<? extends IInterceptor> interceptorClass = entry.getValue();
                        try {
                            //初始化IInterceptor
                            IInterceptor iInterceptor = interceptorClass.getConstructor().newInstance();
                            iInterceptor.init(context);
                            //添加到Warehouse.interceptors
                            Warehouse.interceptors.add(iInterceptor);
                        } catch (Exception ex) {
                            throw new HandlerException(TAG + "ARouter init interceptor error! name = [" + interceptorClass.getName() + "], reason = [" + ex.getMessage() + "]");
                        }
                    }

                    interceptorHasInit = true;

                    logger.info(TAG, "ARouter interceptors init over.");

                    synchronized (interceptorInitLock) {
                        interceptorInitLock.notifyAll();
                    }
                }
            }
        });
    }
```
在doInterceptions中通过CountDownLatch（可以理解为一个递减锁存器的计数，如果计数到达零，则释放所有等待的线程）调用来_execute，
```
 public void doInterceptions(final Postcard postcard, final InterceptorCallback callback) {
        if (MapUtils.isNotEmpty(Warehouse.interceptorsIndex)) {
            //阻塞线程直到执行初始化InterceptorServiceImpl.init()
            checkInterceptorsInitStatus();
            //超过10秒没有初始化成功，回调onInterrupt
            if (!interceptorHasInit) {
                callback.onInterrupt(new HandlerException("Interceptors initialization takes too much time."));
                return;
            }
            //每执行一次navaigation，先线程池添加一个拦截器的任务
            LogisticsCenter.executor.execute(new Runnable() {
                @Override
                public void run() {
                    //所有拦截器数量的CancelableCountDownLatch        
                    CancelableCountDownLatch interceptorCounter = new CancelableCountDownLatch(Warehouse.interceptors.size());
                    try {
                        //递归执行_execute，从index=0开始执行拦截器，不断减少interceptorCounter的计数
                        _execute(0, interceptorCounter, postcard);
                        interceptorCounter.await(postcard.getTimeout(), TimeUnit.SECONDS);
                        //CancelableCountDownLatch的等待是可以超时的
                        if (interceptorCounter.getCount() > 0) {    // Cancel the navigation this time, if it hasn't return anythings.
                            //存在拦截器
                            callback.onInterrupt(new HandlerException("The interceptor processing timed out."));
                        } else if (null != postcard.getTag()) {    // Maybe some exception in the tag.
                             //由于CancelableCountDownLatch是可取消的，查询拦截器回调的异常信息，往外回调
                            callback.onInterrupt((Throwable) postcard.getTag());
                        } else {
                            //回调正常的onContinue
                            callback.onContinue(postcard);
                        }
                    } catch (Exception e) {
                        callback.onInterrupt(e);
                    }
                }
            });
        } else {
            callback.onContinue(postcard);
        }
    }
    
    private static void checkInterceptorsInitStatus() {
        synchronized (interceptorInitLock) {
            while (!interceptorHasInit) {
                try {
                    interceptorInitLock.wait(10 * 1000);
                } catch (InterruptedException e) {
                    throw new HandlerException(TAG + "Interceptor init cost too much time error! reason = [" + e.getMessage() + "]");
                }
            }
        }
    }    
    
 private static void _execute(final int index, final CancelableCountDownLatch counter, final Postcard postcard) {
        if (index < Warehouse.interceptors.size()) {
            //获取第index个的拦截器  
            IInterceptor iInterceptor = Warehouse.interceptors.get(index);
            //执行拦截器
            iInterceptor.process(postcard, new InterceptorCallback() {
                @Override
                public void onContinue(Postcard postcard) {
                    // Last interceptor excute over with no exception.
                    //拦截器回调onContinue
                    //CancelableCountDownLatch计数减一
                    counter.countDown();
                    //递归执行index增加1
                    _execute(index + 1, counter, postcard);  // When counter is down, it will be execute continue ,but index bigger than interceptors size, then U know.
                }

                @Override
                public void onInterrupt(Throwable exception) {
                    // Last interceptor execute over with fatal exception.
                    //拦截器回调onInterrupt  onInterrupt后就不执行后面的拦截器了
                    //给postcard设置tag为exaption异常信息，来自拦截器的异常信息
                    postcard.setTag(null == exception ? new HandlerException("No message.") : exception);    // save the exception message for backup.
                    //执行CancelableCountDownLatch的cancel  cancel会把CancelableCountDownLatch的计数归0
                    counter.cancel();
                    // Be attention, maybe the thread in callback has been changed,
                    // then the catch block(L207) will be invalid.
                    // The worst is the thread changed to main thread, then the app will be crash, if you throw this exception!
//                    if (!Looper.getMainLooper().equals(Looper.myLooper())) {    // You shouldn't throw the exception if the thread is main thread.
//                        throw new HandlerException(exception.getMessage());
//                    }
                }
            });
        }
    }    
```


动态注册路由信息
适用于部分插件化架构的App以及需要动态注册路由信息的场景，可以通过 ARouter 提供的接口实现动态注册 路由信息，目标页面和服务可以不标注 @Route 注解，
注意：同一批次仅允许相同 group 的路由信息注册
```
//1. 一个没有被@Route注解的Activity
class RegisterActivity : AppCompatActivity() {
   
}
//2. addRouteGroup 动态注册路由
ARouter.getInstance().addRouteGroup {
    it[PATH_ACTIVITY_REGISTER] = RouteMeta.build(
        RouteType.ACTIVITY,// 路由信息
        Class.forName("com.login.RegisterActivity"),// 目标的 Class
        PATH_ACTIVITY_REGISTER, // Path
        PATH_ACTIVITY_REGISTER.split("/")[1],// Group, 尽量保持和 path 的第一段相同
        0, // 优先级，暂未使用
        0// Extra，用于给页面打标
    )
}
//3. 进行路由跳转
routerNavigation(PATH_ACTIVITY_REGISTER)
```
看一下addRouteGroup的实现
ARouter.java
```
  public boolean addRouteGroup(IRouteGroup group) {
        return _ARouter.getInstance().addRouteGroup(group);
    }
```
_ARouter.java
```
boolean addRouteGroup(IRouteGroup group) {
        ...
        String groupName = null;

        try {
            // Extract route meta.
            Map<String, RouteMeta> dynamicRoute = new HashMap<>();
            //将group信息添加到dynamicRoute
            group.loadInto(dynamicRoute);

            // Check route meta.
            //遍历dynamicRoute
            for (Map.Entry<String, RouteMeta> route : dynamicRoute.entrySet()) {
                String path = route.getKey();
                //提取group
                String groupByExtract = extractGroup(path);
                RouteMeta meta = route.getValue();

                if (null == groupName) {
                    groupName = groupByExtract;
                }
                //校验groupName 非空；同一批次仅允许相同 group 的路由信息注册，如果同意批次存在不同的group退出
                if (null == groupName || !groupName.equals(groupByExtract) || !groupName.equals(meta.getGroup())) {
                    // Group name not consistent
                    return false;
                }
            }
            //动态添加RouteGroup到Warehouse.groupsIndex和Warehouse.routes
            LogisticsCenter.addRouteGroupDynamic(groupName, group);
            。。。。 
            return true;
        } catch (Exception exception) {
            logger.error(Consts.TAG, "Add route group dynamic exception!", exception);
        }

        return false;
    }
```