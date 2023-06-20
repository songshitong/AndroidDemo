
https://juejin.cn/post/6844903894271705095#heading-13
https://github.com/Tencent/Shadow/wiki
使用
运行：sample\source\sample-host(可测试的功能多)    sample\dynamic-apk\sample-hello-host(功能少)

插件框架有两个作用：一是“自解耦”，二是“免安装”。
自解耦指的是一个应用原本由一份代码编译而成，希望改成将其中的一些功能单独编译，像插件一样动态插在主应用上。这样一来可是使主应用体积变小，下载安装更方便。
  二来可以是比较独立的功能可以单独开发调试，甚至单独更新版本。所以这个“自解耦”的需求并不是在Android平台上出现的，早在PC开发时代就广泛存在了。
免安装指的一个应用原本需要安装过程才能启动运行，希望改为无需安装即可从一个已经安装运行的App中启动起来。这一需求的主要目的是提高流量复用的能力。
  比如在一个用户量已经非常大的应用中推荐安装另一新应用，用户点击入口后如果等待下载然后弹出了安装界面，这就会流失非常多的用户。
  因为用户总会觉得安装有成本，不那么愿意安装新应用。即使你有信心用户一旦安装了你的应用就会留下来，也很可能会因为这个安装过程而流失掉。
  免安装这个场景其实是Shadow服务于我们自身业务时的主要场景。
“像Web一样开发App”则是一个我们后期达成的目标，这大概是“自解耦”和“免安装”的组合形式。单纯的免安装方式去运行一个原本需要安装才能运行的App，
   不可避免的需要用户等待这个App的安装包下载到本地才能运行。而我们知道Web一般是不用这样的，打开哪个页面就去下载哪个页面的资源，
   不用把整个网站都下载到本地才能打开第一个页面。因此，我们在业务中也实践了“自解耦”，将我们的业务插件拆解成了十几个插件。
   由此达到下载一部分启动一部分的能力。Shadow目前开源的代码已经支持这一能力了


https://juejin.cn/post/6844903877871927304
为什么插件放在单独的进程
将组件放置在单独的进程中有很多优点，基本上都是围绕进程具有单独的内存资源的。对于插件框架来说，有两点十分必要。一是插件一般都是热更新的，
质量上要求可能会降低一些，一旦出现Crash不会影响其他进程的组件。比如说在宿主的主进程显示一个大厅界面，其中某个按钮跳转到插件。
插件在单独进程启动后如果出现Crash，宿主的大厅界面不会受到任何影响。如果插件也在宿主的主进程，就会导致大厅界面也会因进程重启而重新创建。
二是Android的JVM虚拟机不支持Native动态库反加载，所以在同一个进程中相同so库的不同版本即不能同时加载，也不能换着加载，会造成插件和宿主存在so库冲突。

多进程也带来更多复杂性，就是它的缺点了。比如，跨进程调用的所有参数都必须是可序列化对象；跨进程通信时对面的进程可能没有启动，也可能已经死了；
跨进程通信出现异常，整个跨进程调用的堆栈不会是连着的，而且异常对象通常是不能序列化跨进程传输的。如何控制插件进程退出或重启供另一业务使用。
另外，进程的启动速度也比较慢。

PluginProcessService为什么设计为service
进程的启动必须由一个组件触发。那么一个没有界面的Service就是一个不错的选择，因为我们通常要对插件进行“预加载”，可能会静默启动插件的Application对象，
或启动插件的Service等。还有要想让系统知道这个插件进程是有用的，就必须有活跃的组件在这个进程。我们的插件中的组件全都是没有安装的组件，
系统都不知道他们存在，肯定不能靠它们了。靠插件的壳子代理组件也不行，因为我们是一个全动态插件框架，那些壳子代理组件也是插件的一部分，还没有加载呢，
所以也不能靠它们。这就需要有一个专门负责启动插件进程的Service，所以它就叫PluginProcessService

插件service的跨进程通信，通过单独进程的PPS进行binder转发


Shadow解决插件和宿主有同名View的方法解析  https://juejin.cn/post/6844903879197327374
重写LayoutInflater，相同名字的View在缓存中配置不同的Key


Shadow支持WebView使用file:///android_asset/协议加载插件资源的方法   解决webview加载插件中asset失败
https://juejin.cn/post/6844903880027832327
先通过Shadow Transform将App中用到的WebView都换成ShadowWebView。再Override ShadowWebView的loadUrl方法。
将请求来的file:///android_asset/协议都修改成http://android.asset/协议。然后就可以采用“Web离线包”的方法，
从插件的Assets中拿出需要的资源返回给这个请求了。之所以要将file协议换成http协议，是因为这种拦截本地请求的能力只支持http协议。

https://juejin.cn/post/6844903893130821640
插件和宿主如何避免data目录冲突  
当businessName为空时，Shadow就认为这个插件跟宿主是同一个业务，这个插件直接使用宿主的data目录。当businessName设置了值时，
Shadow就会在宿主的data目录中以businessName为参数新建一个子目录作为插件的data根目录使用。这样相同businessName的插件就会使用同一个data目录了，
不同businessName的插件的data目录就相当于是隔离的了

partKey是插件apk的别名。因为插件apk的文件名是有可能因为带了版本号或者什么参数而变化，所以有这样一个partKey作为一个插件apk的不变的别名。
partKey可以用于在表示一个插件依赖另外一个插件时使用。Shadow内部在实现区分多插件逻辑时也会用partKey作为该apk的Key。
在Loader等接口上的partKey参数指的也是这个partKey

dependsOn声明的是当前插件依赖哪些其他插件。指定依赖的插件，要填写插件的partKey。假设插件A依赖插件B，Shadow会将插件B的ClassLoader作为插件A的parent。
这样插件A就可以访问插件B的类了
插件A依赖插件B，还应该能使插件A中的资源依赖插件B中的资源，这应该表现在构造插件A的Resource对象时，
将插件B作为插件A的android.content.pm.ApplicationInfo#sharedLibraryFiles

hostWhiteList就是为了允许插件访问宿主的类而设计的参数。hostWhiteList中设置的是Java类的包名。没有设置dependsOn的插件会将宿主的ClassLoader作为parent，
但是插件的ClassLoader不是正常的双亲委派逻辑。插件ClassLoader同时还将宿主的ClassLoader的parent作为名为specialClassLoader的变量持有。
插件的ClassLoader加载类的主路径是先尝试自己加载，自己加载不到，再用specialClassLoader加载。当要加载的类处于hostWhiteList中则采用正常的双亲委派，
用parent（也就是宿主的ClassLoader）加载。这样设计的目的就是为了让插件和宿主类隔离，又可以允许插件复用宿主的部分类

关于so文件
在同一个进程中加载的多个插件的so，相互之间没有隔离，都相当于是宿主加载的so。因此，插件A依赖插件B中的so，不需要特别声明。插件A同插件B有冲突so，
又需要在同一个进程中工作，也需要so的设计方自行解决。

config.json的设计
config.json是一个可以插件包的描述
跟版本信息相关的字段有：version、compact_version、UUID、UUID_NickName。
Loader、Runtime和Plugin的描述：apkName和hash，partKey、businessName、hostWhiteList

插件包生成Gradle插件
shadow {packagePlugin {}}DSL动态生成config.json

项目结构：
```
├── projects
│ ├── sample // 示例代码
│ │ ├── README.md
│ │ ├── maven
│ │ ├── sample-constant // 定义一些常量
│ │ ├── sample-host // 宿主实现
│ │ ├── sample-manager // PluginManager 实现
│ │ └── sample-plugin // 插件的实现
│ ├── sdk // 框架实现代码
│ │ ├── coding // lint
│ │ ├── core
│ │ │ ├── common
│ │ │ ├── gradle-plugin // gradle 插件
│ │ │ ├── load-parameters
│ │ │ ├── loader // 负责加载插件
│ │ │ ├── manager // 装载插件，管理插件
│ │ │ ├── runtime // 插件运行时需要，包括占位 Activity，占位 Provider 等等
│ │ │ ├── transform // Transform 实现，用于替换插件 Activity 父类等等
│ │ │ └── transform-kit
│ │ └── dynamic // 插件自身动态化实现，包括一些接口的抽象
```



Activity 实现
关于插件 Activity 的实现，我们主要看：
0 替换插件 Activity 的父类
1宿主中如何启动插件 Activity
2插件中如何启动插件 Activity

替换插件 Activity 的父类
Shadow 中有一个比较巧妙的地方，就是插件开发的时候，插件的 Activity 还是正常继承 Activity，在打包的时候，会通过 Transform 替换其父类为 ShadowActivity。
projects/sdk/core/transform 和 projects/sdk/core/transform-kit 两个项目就是 Transform，入口是 ShadowTransform。
这里对 Transform 做了一些封装，提供了友好的开发方式
com/tencent/shadow/core/transform_kit/AbstractTransform.kt
```
    override fun onTransform() {
        mTransformManager.setupAll()
        mTransformManager.fireAll()
    }
```
com/tencent/shadow/core/transform_kit/AbstractTransformManager.kt
```
fun setupAll() {
        mTransformList.forEach {
            it.mClassPool = classPool
            it.setup(allInputClass)
        }
    }

    fun fireAll() {
        mTransformList.flatMap { it.list }.forEach { transform ->
            transform.filter(allInputClass).forEach {
                transform.transform(it)
            }
        }
    }
```
com/tencent/shadow/core/transform/TransformManager.kt
```
override val mTransformList: List<SpecificTransform> = listOf(
        ApplicationTransform(),
        ActivityTransform(),
        ServiceTransform(),
        IntentServiceTransform(),
        InstrumentationTransform(),
        FragmentSupportTransform(),
        DialogSupportTransform(),
        WebViewTransform(),
        ContentProviderTransform(),
        PackageManagerTransform(),
        PackageItemInfoTransform(),
        AppComponentFactoryTransform(),
        LayoutInflaterTransform(),
        KeepHostContextTransform(useHostContext()),
        ActivityOptionsSupportTransform(),
        ReceiverSupportTransform(),
    )
```
TransformManager中，初始化一系列Transform，并调用setup，transform方法
看一下ApplicationTransform和ActivityTransform，这两个继承SimpleRenameTransform
com/tencent/shadow/core/transform/specific/SimpleRenameTransform.kt
```
 final override fun setup(allInputClass: Set<CtClass>) {
        newStep(object : TransformStep {
            ...
            override fun transform(ctClass: CtClass) {
                fromToMap.forEach { //class替换
                    ReplaceClassName.replaceClassName(ctClass, it.key, it.value)
                }
            }
        })
    }
```
ApplicationTransform
1 替换Application 为ShadowApplication
2 替换Application.ActivityLifecycleCallbacks为ShadowActivityLifecycleCallbacks
```
 mapOf(
        "android.app.Application"
                to "com.tencent.shadow.core.runtime.ShadowApplication",
        "android.app.Application\$ActivityLifecycleCallbacks"
                to "com.tencent.shadow.core.runtime.ShadowActivityLifecycleCallbacks"
    )
```
ActivityTransform
1 Activity替换为ShadowActivity
2 NativeActivity替换为ShadowNativeActivity
```
 mapOf(
        "android.app.Activity"
                to "com.tencent.shadow.core.runtime.ShadowActivity",
        "android.app.NativeActivity"
                to "com.tencent.shadow.core.runtime.ShadowNativeActivity"
    )
```
为何插件 Activity 可以不用继承 Activity 呢？
因为在代理 Activity 的方式中，插件 Activity 是被当作一个普通类来使用的，只要负责执行对应的生命周期即可
ShadowActivity的继承路径
ShadowActivity(部分特殊情况的兼容及activity方法重写)
->PluginActivity(插件类，整合HostActivityDelegator和ShadowApplication)
->GeneratedPluginActivity(自动生成的代理类，大部分方法由GeneratedHostActivityDelegator实现,一般子类为HostActivity)
->ShadowContext(重写context的部分方法，例如getResources，getAssets，getClassLoader，startActivity，startService等)
->SubDirContextThemeWrapper(重写context的获取路径为子路径)

宿主中如何启动插件 Activity
com/tencent/shadow/sample/host/MainActivity.java
```
Intent intent = new Intent(MainActivity.this, PluginLoadActivity.class);
//主进程       “KEY_PLUGIN_PART_KEY”和  “sample-base”
intent.putExtra(Constant.KEY_PLUGIN_PART_KEY, PART_KEY_PLUGIN_BASE);
...
“KEY_ACTIVITY_CLASSNAME”
intent.putExtra(Constant.KEY_ACTIVITY_CLASSNAME, "com.tencent.shadow.sample.plugin.app.lib.gallery.splash.SplashActivity");
..
startActivity(intent);
```
com/tencent/shadow/sample/host/PluginLoadActivity.java
```
   @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ...
        startPlugin();
    }

 public void startPlugin() {

        PluginHelper.getInstance().singlePool.execute(new Runnable() {
            @Override
            public void run() {
                //根据pluginmanager.apk创建PluginManager
                HostApplication.getApp().loadPluginManager(PluginHelper.getInstance().pluginManagerFile);

                Bundle bundle = new Bundle();
                //包装参数
                bundle.putString(Constant.KEY_PLUGIN_ZIP_PATH, PluginHelper.getInstance().pluginZipFile.getAbsolutePath());
                bundle.putString(Constant.KEY_PLUGIN_PART_KEY, getIntent().getStringExtra(Constant.KEY_PLUGIN_PART_KEY));
                bundle.putString(Constant.KEY_ACTIVITY_CLASSNAME, getIntent().getStringExtra(Constant.KEY_ACTIVITY_CLASSNAME));
                //调用PluginManager.enter
                HostApplication.getApp().getPluginManager()
                        .enter(PluginLoadActivity.this, Constant.FROM_ID_START_ACTIVITY, bundle, new EnterCallback() {
                            @Override
                            public void onShowLoadingView(final View view) {
                                //加载样式
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mViewGroup.addView(view);
                                    }
                                });
                            }

                            @Override
                            public void onCloseLoadingView() {
                                finish(); //关闭插件加载activity
                            }
                            ...   
```
这里PluginManager的实现为DynamicPluginManager
com/tencent/shadow/sample/host/manager/Shadow.java
```
  public static PluginManager getPluginManager(File apk) {
        final FixedPathPmUpdater fixedPathPmUpdater = new FixedPathPmUpdater(apk);
        File tempPm = fixedPathPmUpdater.getLatest();
        if (tempPm != null) {
            return new DynamicPluginManager(fixedPathPmUpdater);
        }
        return null;
    }
```
com/tencent/shadow/dynamic/host/DynamicPluginManager.java
```
    public void enter(Context context, long fromId, Bundle bundle, EnterCallback callback) {
        if (mLogger.isInfoEnabled()) {
            mLogger.info("enter fromId:" + fromId + " callback:" + callback);
        }
        updateManagerImpl(context);//ManagerImplLoader.load()创建PluginManagerImpl，实现类是SamplePluginManager，也就是mManagerImpl
        mManagerImpl.enter(context, fromId, bundle, callback);
        mUpdater.update();
    }
```
com/tencent/shadow/sample/manager/SamplePluginManager.java
```
 public void enter(final Context context, long fromId, Bundle bundle, final EnterCallback callback) {
        if (fromId == Constant.FROM_ID_NOOP) {
            //do nothing.
        } else if (fromId == Constant.FROM_ID_START_ACTIVITY) {
            onStartActivity(context, bundle, callback);
        } else if (fromId == Constant.FROM_ID_CLOSE) {
            close();
        } else if (fromId == Constant.FROM_ID_LOAD_VIEW_TO_HOST) {
            loadViewToHost(context, bundle);
        } else {
            throw new IllegalArgumentException("不认识的fromId==" + fromId);
        }
    }

 private void onStartActivity(final Context context, Bundle bundle, final EnterCallback callback) {
        //解析参数 
        final String pluginZipPath = bundle.getString(Constant.KEY_PLUGIN_ZIP_PATH);
        final String partKey = bundle.getString(Constant.KEY_PLUGIN_PART_KEY);
        final String className = bundle.getString(Constant.KEY_ACTIVITY_CLASSNAME);
       ...
        final Bundle extras = bundle.getBundle(Constant.KEY_EXTRAS);
        //加载统一的loading
        if (callback != null) {
            final View view = LayoutInflater.from(mCurrentContext).inflate(R.layout.activity_load_plugin, null);
            callback.onShowLoadingView(view);
        }

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    //加载插件
                    InstalledPlugin installedPlugin = installPlugin(pluginZipPath, null, true);
                    //加载binder
                    loadPlugin(installedPlugin.UUID, PART_KEY_PLUGIN_BASE);
                    loadPlugin(installedPlugin.UUID, PART_KEY_PLUGIN_MAIN_APP);
                    //通过binder调用application的onCraete
                    callApplicationOnCreate(PART_KEY_PLUGIN_BASE);
                    callApplicationOnCreate(PART_KEY_PLUGIN_MAIN_APP);
                    //创建新的intent
                    Intent pluginIntent = new Intent();
                    pluginIntent.setClassName(
                            context.getPackageName(),
                            className
                    );
                    if (extras != null) {
                        pluginIntent.replaceExtras(extras);
                    }
                    //binder通信获取宿主intent PluginDefaultProxyActivity
                    Intent intent = mPluginLoader.convertActivityIntent(pluginIntent);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    //通过binder启动Activity    
                    mPluginLoader.startActivityInPluginProcess(intent);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                //加载完成，关闭loading
                if (callback != null) {
                    callback.onCloseLoadingView();
                }
            }
        });
    }
    
  public InstalledPlugin installPlugin(String zip, String hash, boolean odex) throws IOException, JSONException, InterruptedException, ExecutionException {
        //从zip加载
        final PluginConfig pluginConfig = installPluginFromZip(new File(zip), hash);
        ...
    }    
```

接口PluginLoader的远程进程  
PluginProcessPPS和Plugin2ProcessPPS都继承PluginProcessService
com/tencent/shadow/dynamic/host/PluginProcessService
PluginProcessService->com/tencent/shadow/dynamic/loader/impl/PluginLoaderBinder->DynamicPluginLoader
com/tencent/shadow/sample/manager/SamplePluginManager.java
```
 protected String getPluginProcessServiceName(String partKey) {
        //sample-plugin-app
        if (PART_KEY_PLUGIN_MAIN_APP.equals(partKey)) {
            return "com.tencent.shadow.sample.host.PluginProcessPPS";
        } else if (PART_KEY_PLUGIN_BASE.equals(partKey)) {
        //sample-base
            return "com.tencent.shadow.sample.host.PluginProcessPPS";
        } else if (PART_KEY_PLUGIN_ANOTHER_APP.equals(partKey)) {
         //sample-plugin-app2
            return "com.tencent.shadow.sample.host.Plugin2ProcessPPS";//在这里支持多个插件
        } else {
            //如果有默认PPS，可用return代替throw
            throw new IllegalArgumentException("unexpected plugin load request: " + partKey);
        }
    }
```
进程声明在sample\source\sample-host\src\main\AndroidManifest.xml
```
  <service
            android:name="com.tencent.shadow.sample.host.PluginProcessPPS"
            android:process=":plugin" />
        <service
            android:name="com.tencent.shadow.sample.host.Plugin2ProcessPPS"
            android:process=":plugin2" />
```
PluginProcessService的PpsBinder调用PluginProcessService的方法
com/tencent/shadow/dynamic/host/PpsBinder.java

对应关系
PpsController与PpsBinder交互，调用PluginProcessService的方法
PpsController.getPluginLoader获取PluginProcessService的PluginLoaderBinder，最终调用DynamicPluginLoader

convertActivityIntent的实现  转为宿主的activity
com/tencent/shadow/dynamic/loader/impl/DynamicPluginLoader.kt
```
    fun convertActivityIntent(pluginActivityIntent: Intent): Intent? {
        return mPluginLoader.mComponentManager.convertPluginActivityIntent(pluginActivityIntent)
    }
```
com/tencent/shadow/core/loader/managers/ComponentManager.kt
```
   override fun convertPluginActivityIntent(pluginIntent: Intent): Intent {
        //判断Intent是一个插件内的组件
        return if (pluginIntent.isPluginComponent()) {
            pluginIntent.toActivityContainerIntent()
        } else {
            pluginIntent
        }
    }
    
    private fun Intent.toActivityContainerIntent(): Intent {
        val bundleForPluginLoader = Bundle()
        val pluginActivityInfo = pluginActivityInfoMap[component]!!
        bundleForPluginLoader.putParcelable(CM_ACTIVITY_INFO_KEY, pluginActivityInfo)
        return toContainerIntent(bundleForPluginLoader)
    }
 
  private fun Intent.toContainerIntent(bundleForPluginLoader: Bundle): Intent {
      。。。
        val containerIntent = Intent(this)
        //遍历插件的manifest.xml,确定对应关系，默认为PluginDefaultProxyActivity
        containerIntent.component = containerComponent //宿主的component 
        
        //CM_CLASS_NAME
        bundleForPluginLoader.putString(CM_CLASS_NAME_KEY, className)
        //CM_PACKAGE_NAME
        bundleForPluginLoader.putString(CM_PACKAGE_NAME_KEY, packageName)

        containerIntent.putExtra(CM_EXTRAS_BUNDLE_KEY, pluginExtras)
        containerIntent.putExtra(CM_BUSINESS_NAME_KEY, businessName)
        containerIntent.putExtra(CM_PART_KEY, partKey)
        containerIntent.putExtra(CM_LOADER_BUNDLE_KEY, bundleForPluginLoader)
        containerIntent.putExtra(LOADER_VERSION_KEY, BuildConfig.VERSION_NAME)//local
        containerIntent.putExtra(PROCESS_ID_KEY, DelegateProviderHolder.sCustomPid)
        //进程号 为了防止系统有一定概率出现进程号重启后一致的问题，我们使用开机时间作为进程号来判断进程是否重启
        return containerIntent
    }  
```
PluginDefaultProxyActivity继承PluginContainerActivity
PluginContainerActivity继承GeneratedPluginContainerActivity
com/tencent/shadow/core/runtime/container/PluginContainerActivity.java
生命周期通过HostActivityDelegate分发
```
 public PluginContainerActivity() {
        HostActivityDelegate delegate;
        DelegateProvider delegateProvider = DelegateProviderHolder.getDelegateProvider(getDelegateProviderKey());
        if (delegateProvider != null) {
            delegate = delegateProvider.getHostActivityDelegate(this.getClass());
            delegate.setDelegator(this);
        } else {
            Log.e(TAG, "PluginContainerActivity: DelegateProviderHolder没有初始化");
            delegate = null;
        }
        super.hostActivityDelegate = delegate;
        hostActivityDelegate = delegate;
    }
```
com/tencent/shadow/core/loader/delegates/ShadowActivityDelegate.kt
```
 override fun onCreate(savedInstanceState: Bundle?) {
        ...
        mPartKey = partKey
        //设置classLoader，application，resources 等等
        mDI.inject(this, partKey)
        mDependenciesInjected = true

        val bundleForPluginLoader = pluginInitBundle.getBundle(CM_LOADER_BUNDLE_KEY)!!
        mBundleForPluginLoader = bundleForPluginLoader
        bundleForPluginLoader.classLoader = this.javaClass.classLoader
        val pluginActivityClassName = bundleForPluginLoader.getString(CM_CLASS_NAME_KEY)!!
        val pluginActivityInfo: PluginManifest.ActivityInfo =
            bundleForPluginLoader.getParcelable(CM_ACTIVITY_INFO_KEY)!!
        mPluginActivityInfo = pluginActivityInfo

        mCurrentConfiguration = Configuration(resources.configuration)
        mPluginHandleConfigurationChange =
            (pluginActivityInfo.configChanges
                    or ActivityInfo.CONFIG_SCREEN_SIZE//系统本身就会单独对待这个属性，不声明也不会重启Activity。
                    or ActivityInfo.CONFIG_SMALLEST_SCREEN_SIZE//系统本身就会单独对待这个属性，不声明也不会重启Activity。
                    or 0x20000000 //见ActivityInfo.CONFIG_WINDOW_CONFIGURATION 系统处理属性
                    )
        if (savedInstanceState == null) {
            mRawIntentExtraBundle = pluginInitBundle.getBundle(CM_EXTRAS_BUNDLE_KEY)
            mHostActivityDelegator.intent.replaceExtras(mRawIntentExtraBundle)
        }
        mHostActivityDelegator.intent.setExtrasClassLoader(mPluginClassLoader)

        try {
           // 创建插件 activity    (ShadowActivity) cl.loadClass(className).newInstance();
            val pluginActivity = mAppComponentFactory.instantiateActivity(
                mPluginClassLoader,
                pluginActivityClassName,
                mHostActivityDelegator.intent
            )
            //设置pluginActity的Resources，ClassLoader，Application，theme等
            initPluginActivity(pluginActivity, pluginActivityInfo)
            super.pluginActivity = pluginActivity
            ...
            //使PluginActivity替代ContainerActivity接收Window的Callback
            mHostActivityDelegator.window.callback = pluginActivity

            //设置插件AndroidManifest.xml 中注册的WindowSoftInputMode
            mHostActivityDelegator.window.setSoftInputMode(pluginActivityInfo.softInputMode)

            //Activity.onCreate调用之前应该先收到onWindowAttributesChanged。
            if (mCallOnWindowAttributesChanged) {
                pluginActivity.onWindowAttributesChanged(
                    mBeforeOnCreateOnWindowAttributesChangedCalledParams
                )
                mBeforeOnCreateOnWindowAttributesChangedCalledParams = null
            }

            val pluginSavedInstanceState: Bundle? =
                savedInstanceState?.getBundle(PLUGIN_OUT_STATE_KEY)
            pluginSavedInstanceState?.classLoader = mPluginClassLoader
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                notifyPluginActivityPreCreated(pluginActivity, pluginSavedInstanceState)
            }
            //调用onCreate
            pluginActivity.onCreate(pluginSavedInstanceState)
            mPluginActivityCreated = true
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
```

插件中如何启动插件 Activity
插件 Activity 会在打包过程中替换其父类为 ShadowActivity，很明显了，在插件中启动 Activity 即调用 startActivity，
自然就是调用 ShadowActivity 的 startActivity 了。startActivity 在其父类 ShadowContext 里实现，我们来具体看下。
com/tencent/shadow/core/runtime/ShadowContext.java
```
    public void startActivity(Intent intent, Bundle options) {
        final Intent pluginIntent = new Intent(intent);
        pluginIntent.setExtrasClassLoader(mPluginClassLoader);
        final boolean success = mPluginComponentLauncher.startActivity(this, pluginIntent, options);
        if (!success) {
            super.startActivity(intent, options);
        }
    }
```
com/tencent/shadow/core/loader/managers/ComponentManager.kt
```
override fun startActivity(
        shadowContext: ShadowContext,
        pluginIntent: Intent,
        option: Bundle?
    ): Boolean {
        return if (pluginIntent.isPluginComponent()) {
            shadowContext.superStartActivity(pluginIntent.toActivityContainerIntent(), option)
            true
        } else {
            false
        }
    }
```
com/tencent/shadow/core/runtime/ShadowContext.java
```
    @android.annotation.TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void superStartActivity(Intent intent, Bundle options) {
        //调用系统activity
        super.startActivity(intent, options);
    }
```
通过调用 toActivityContainerIntent 转化 intent 为代理 Activity 的 intent，然后调用系统 startActivity 启动代理 Activity



Service 实现
Service 的实现，我们直接看 插件中如何启动的即可。看一下 ShadowContext 中的 startService 实现：
com/tencent/shadow/core/runtime/ShadowContext.java
```
    @Override
    public ComponentName startService(Intent service) {
        if (service.getComponent() == null) {
            return super.startService(service);
        }
        Pair<Boolean, ComponentName> ret = mPluginComponentLauncher.startService(this, service);
        if (!ret.first)
            return super.startService(service);
        return ret.second;
    }
```
com/tencent/shadow/core/loader/managers/ComponentManager.kt
```
   override fun startService(
        context: ShadowContext,
        service: Intent
    ): Pair<Boolean, ComponentName?> {
        if (service.isPluginComponent()) {
            // 插件service intent不需要转换成container service intent，直接使用intent
            val component = mPluginServiceManager!!.startPluginService(service)
            if (component != null) {
                return Pair(true, component)
            }
        }
        return Pair(false, service.component)
    }
```
com/tencent/shadow/core/loader/managers/PluginServiceManager.kt
```
   fun startPluginService(service: Intent) =
        execInMainThread {
            delegate.startPluginService(service)
        }
        
    private inline fun <reified T> execInMainThread(crossinline action: () -> T): T {
        if (Thread.currentThread() === Looper.getMainLooper().thread) {
            return action()
        } else {
            val countDownLatch = CountDownLatch(1)
            val result = arrayOfNulls<T>(1)
            mainThreadHandler.post {
                result[0] = action()
                countDownLatch.countDown()
            }
            //子线程等待结果
            countDownLatch.await()
            return result[0] as T
        }
    }     
 
     fun startPluginService(intent: Intent): ComponentName? {
        val componentName = intent.component!!
        // 检查所请求的service是否已经存在
        if (!mAliveServicesMap.containsKey(componentName)) {
            // 不存在则创建
            val service = createServiceAndCallOnCreate(intent)
            mAliveServicesMap[componentName] = service
            // 通过startService启动集合
            mServiceStartByStartServiceSet.add(componentName)
        }
        mAliveServicesMap[componentName]?.onStartCommand(intent, 0, getNewStartId())
        return componentName
    }   

    private fun createServiceAndCallOnCreate(intent: Intent): ShadowService {
        val service = newServiceInstance(intent)
        service.onCreate()
        return service
    }        
```
在 Shadow 中对 Service 的处理很简单，直接调用其生命周期方法



BroadcastReceiver 实现
广播的实现也比较常规，在插件中动态注册和发送广播，直接调用系统的方法即可，因为广播不涉及生命周期等复杂的内容。需要处理的就是在 Manifest 中静态注册的广播
，解析 Manifest 然后进行动态注册。
com/tencent/shadow/core/runtime/ShadowApplication.java
``` 
    //设置receiver
    public void setBroadcasts(PluginManifest.ReceiverInfo[] receiverInfos) {
        Map<String, String[]> classNameToActions = new HashMap<>();
        if (receiverInfos != null) {
            for (PluginManifest.ReceiverInfo receiverInfo : receiverInfos) {
                classNameToActions.put(receiverInfo.className, receiverInfo.actions);
            }
        }
        mBroadcasts = classNameToActions;
    }
    
 public void onCreate() {

        isCallOnCreate = true;

        for (Map.Entry<String, String[]> entry : mBroadcasts.entrySet()) {
            try {
                String receiverClassname = entry.getKey();
                BroadcastReceiver receiver = mAppComponentFactory.instantiateReceiver(
                        mPluginClassLoader,
                        receiverClassname,
                        null);

                IntentFilter intentFilter = new IntentFilter();
                String[] receiverActions = entry.getValue();
                if (receiverActions != null) {
                    for (String action : receiverActions) {
                        intentFilter.addAction(action);
                    }
                }
                //注册receiver
                registerReceiver(receiver, intentFilter);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
       ....
    }    
```

ContentProvider 实现
关于 ContentProvider 的实现，也是通过注册代理 ContentProvider 然后分发给插件 Provider
\sample\source\sample-host\src\main\AndroidManifest.xml
```
  <provider
            android:authorities="${applicationId}.contentprovider.authority.dynamic"
            android:name="com.tencent.shadow.core.runtime.container.PluginContainerContentProvider"
            android:grantUriPermissions="true"
            android:process=":plugin" />
```
com/tencent/shadow/core/runtime/container/PluginContainerContentProvider.java
内部通过HostContentProviderDelegate实现
```
 public PluginContainerContentProvider() {
        ContentProviderDelegateProviderHolder.setDelegateProviderHolderPrepareListener(new ContentProviderDelegateProviderHolder.DelegateProviderHolderPrepareListener() {
            @Override
            public void onPrepare() {
                HostContentProviderDelegate delegate;
                if (ContentProviderDelegateProviderHolder.contentProviderDelegateProvider != null) {
                    delegate = ContentProviderDelegateProviderHolder.contentProviderDelegateProvider.getHostContentProviderDelegate();
                    delegate.onCreate();
                } else {
                    Log.e(TAG, "PluginContainerContentProvider: DelegateProviderHolder没有初始化");
                    delegate = null;
                }
                hostContentProviderDelegate = delegate;
            }
        });
    }
```
com/tencent/shadow/core/loader/delegates/ShadowContentProviderDelegate.kt
以查询为例
```
 override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
       //转换为插件的Uri
        val pluginUri = mProviderManager.convert2PluginUri(uri)
        //调用插件ContentProvider的query
        return mProviderManager.getPluginContentProvider(pluginUri.authority!!)!!
            .query(pluginUri, projection, selection, selectionArgs, sortOrder)
    }
```
com/tencent/shadow/core/loader/managers/PluginContentProviderManager.kt
```
  fun convert2PluginUri(uri: Uri): Uri {
        val containerAuthority: String? = uri.authority
        if (!providerAuthorityMap.values.contains(containerAuthority)) {
            throw IllegalArgumentException("不能识别的uri Authority:$containerAuthority")
        }
        val uriString = uri.toString()
        return Uri.parse(uriString.replace("$containerAuthority/", ""))
    }
 
  fun getPluginContentProvider(pluginAuthority: String): ContentProvider? {
        return providerMap[pluginAuthority]
    }
```
插件contentProvider的注册
com/tencent/shadow/core/loader/managers/ComponentManager.kt
```
 fun addPluginApkInfo(
        pluginManifest: PluginManifest,
        loadParameters: LoadParameters,
        archiveFilePath: String
    ) {
     ...
        pluginManifest.providers?.forEach {
            val componentName = ComponentName(applicationPackageName, it.className)
            mPluginContentProviderManager!!.addContentProviderInfo(
                loadParameters.partKey,
                it,
                onBindContainerContentProvider(componentName)
            )
        }
        ...
    }
```
com/tencent/shadow/core/loader/managers/PluginContentProviderManager.kt
```
    fun addContentProviderInfo(
        partKey: String,
        pluginProviderInfo: PluginManifest.ProviderInfo,
        containerProviderInfo: ContainerProviderInfo
    ) {
        if (providerMap.containsKey(pluginProviderInfo.authorities)) {
            throw RuntimeException("重复添加 ContentProvider")
        }
        //保存到map  authority为PackageName()+".contentprovider.authority.dynamic")
        providerAuthorityMap[pluginProviderInfo.authorities] = containerProviderInfo.authority
        var pluginProviderInfos: HashSet<PluginManifest.ProviderInfo>? = null
        if (pluginProviderInfoMap.containsKey(partKey)) {
            pluginProviderInfos = pluginProviderInfoMap[partKey]
        } else {
            pluginProviderInfos = HashSet()
        }
        pluginProviderInfos?.add(pluginProviderInfo)
        pluginProviderInfoMap.put(partKey, pluginProviderInfos)
    }
```
com/tencent/shadow/sample/plugin/loader/SampleComponentManager.java
```
    /**
     * 配置对应宿主中预注册的壳子contentProvider的信息
     */
    @Override
    public ContainerProviderInfo onBindContainerContentProvider(ComponentName pluginContentProvider) {
        return new ContainerProviderInfo(
                "com.tencent.shadow.core.runtime.container.PluginContainerContentProvider",
                context.getPackageName() + ".contentprovider.authority.dynamic");
    }
```




框架自身动态化
Shadow 框架还有一个特点，就是 框架本身也实现了动态化，这里的实现主要是三步：
1 抽象接口类
2 在插件中实现工厂类
3 通过工厂类动态创建接口的实现
我们以 PluginLoaderImpl 为例来看看，在前面介绍 Activity 启动流程的时候，有说到 mPluginLoader.convertActivityIntent 用来转换 插件 intent 为代理 Activity 的 intent，
这里的 mPluginLoader 就是动态创建的。我们来看一下创建过程。
创建入口在 PluginProcessService.loadPluginLoader 里
com/tencent/shadow/dynamic/host/PluginProcessService.java
```
    void loadPluginLoader(String uuid) throws FailedException {
        ...
            PluginLoaderImpl pluginLoader = new LoaderImplLoader().load(installedApk, uuid, getApplicationContext());
            pluginLoader.setUuidManager(mUuidManager);
            mPluginLoader = pluginLoader;
        }...
    }
```
com/tencent/shadow/dynamic/host/LoaderImplLoader.java
```
 PluginLoaderImpl load(InstalledApk installedApk, String uuid, Context appContext) throws Exception {
        ApkClassLoader pluginLoaderClassLoader = new ApkClassLoader(
                installedApk,
                LoaderImplLoader.class.getClassLoader(),
                loadWhiteList(installedApk),
                1
        );
        //通过反射创建LoaderFactory的实现LoaderFactoryImpl
        LoaderFactory loaderFactory = pluginLoaderClassLoader.getInterface(
                LoaderFactory.class,
                sLoaderFactoryImplClassName //com.tencent.shadow.dynamic.loader.impl.LoaderFactoryImpl
        );
         // 调用工厂类方法创建 PluginLoaderImpl 实例
        return loaderFactory.buildLoader(uuid, appContext);
    }
```
从上面的代码和注释来看，其实很简单，创建插件的 ClassLoader，通过 ClassLoader 创建一个工厂类的实例，然后调用工厂类方法生成 PluginLoaderImpl。
而工厂类和 PluginLoaderImpl 的实现都在插件中，就达到了框架自身的动态化。
PluginManagerImpl 也是一样的道理，在 DynamicPluginManager.updateManagerImpl 中通过 ManagerImplLoader.load 加载



https://juejin.cn/post/6844903896129732622
packageManager的处理
替换插件中packageManager的方法  需要处理packageManager中获取包信息相关的方法，例如全局theme，dataDir，nativeLibraryDir，sourceDir
com/tencent/shadow/core/transform/specific/PackageManagerTransform.kt
```
//替换的类
 const val AndroidPackageManagerClassname = "android.content.pm.PackageManager"
        const val ShadowAndroidPackageManagerClassname =
            "com.tencent.shadow.core.runtime.PackageManagerInvokeRedirect"
//需要处理的方法
override fun setup(allInputClass: Set<CtClass>) {
        setupPackageManagerTransform(
            arrayOf(
                "getApplicationInfo",
                "getActivityInfo",
                "getServiceInfo",
                "getProviderInfo",
                "getPackageInfo",
                "resolveContentProvider",
                "queryContentProviders",
                "resolveActivity",
                "resolveService",
            )
        )
    }
```
com/tencent/shadow/core/runtime/PackageManagerInvokeRedirect.java
```
public class PackageManagerInvokeRedirect {

    public static PluginPackageManager getPluginPackageManager(ClassLoader classLoaderOfInvokeCode) {
        return PluginPartInfoManager.getPluginInfo(classLoaderOfInvokeCode).packageManager;
    }

    public static ApplicationInfo getApplicationInfo(ClassLoader classLoaderOfInvokeCode, String packageName, int flags) throws PackageManager.NameNotFoundException {
        return getPluginPackageManager(classLoaderOfInvokeCode).getApplicationInfo(packageName, flags);
    }
    ...
 }
```
PluginPackageManager接口的实现
com/tencent/shadow/core/loader/managers/PluginPackageManagerImpl.kt
```
   override fun getApplicationInfo(packageName: String, flags: Int): ApplicationInfo =
        if (packageName.isPlugin()) {
            getPluginApplicationInfo(flags)
        } else {
            //不是插件，使用宿主的getApplicationInfo
            hostPackageManager.getApplicationInfo(packageName, flags)
        }
        
   private fun getPluginApplicationInfo(flags: Int): ApplicationInfo {
        //pluginApplicationInfoFromPluginManifest在加载插件时，根据宿主的applicationInfo和插件的manifest获取
        val copy = ApplicationInfo(pluginApplicationInfoFromPluginManifest)

        val needMetaData = flags and PackageManager.GET_META_DATA != 0
        if (needMetaData) {
           //需要needMetaData，使用宿主的
            val packageInfo = hostPackageManager.getPackageArchiveInfo(
                pluginArchiveFilePath,
                PackageManager.GET_META_DATA
            )!!
            val metaData = packageInfo.applicationInfo.metaData
            copy.metaData = metaData
        }
        return copy
    }     
```