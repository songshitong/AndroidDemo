
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

                    loadPlugin(installedPlugin.UUID, PART_KEY_PLUGIN_BASE);
                    loadPlugin(installedPlugin.UUID, PART_KEY_PLUGIN_MAIN_APP);
                    //调用application的onCraete
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
                    Intent intent = mPluginLoader.convertActivityIntent(pluginIntent);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    //启动插件 Activity
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
```