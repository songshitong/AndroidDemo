
https://juejin.cn/post/7056983987859750919#heading-19
监听 Activity 的生命周期
通常思路	  具体	                                            缺点
基础	     直接覆写 Activity 对应的生命周期函数	                繁琐、高耦合
进阶	    利用 Application#registerLifecycleCallback 统一管理	回调固定、需要区分各 Activity、逻辑侵入到 Application
        Lifecycle 框架

androidx.lifecycle 2.4.0  OnLifecycleEvent已经弃用，要么依赖反射，要么使用编译期apt会降低编译速度，推荐DefaultLifecycleObserver
使用示例   onCreate 的时候执行初始化，onStart 的时候开始连接，onPause 的时候断开连接
```
class MyLifecycleObserver(
    private val lifecycle: Lifecycle
) : LifecycleObserver {
    ...
    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun init() {
        lifecycle.addObserver(this)
        enabled = checkStatus()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun start() {
        if (enabled) {
            connect()
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun stop() {
        if (connected) {
            disconnect()
        }
    }
    
     @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun onDestroy() {
        lifecycle.removeObserver(this)
    }
}
```
activity使用
```
 override fun onCreate(savedInstanceState: Bundle) {
        MyLifecycleObserver(lifecycle)
    }
```


https://blog.csdn.net/vitaviva/article/details/121224946
注解解析过程
Event 分发时，怎么就会回到到注解对应的方法的？

通过 addObserver 添加的 LifecycleObserver ，都会转为一个 LifecycleEventObserver ，LifecycleOwner 通过调用
  其 onStateChanged 分发 Event

在 Lifecycling#lifecycleEventObserver 中处理转换
```
public class Lifecycling {
    
    @NonNull
    static LifecycleEventObserver lifecycleEventObserver(Object object) {
        boolean isLifecycleEventObserver = object instanceof LifecycleEventObserver;
        boolean isFullLifecycleObserver = object instanceof FullLifecycleObserver;
        // 观察者是 FullLifecycleObserver
        if (isLifecycleEventObserver && isFullLifecycleObserver) {
            return new FullLifecycleObserverAdapter((FullLifecycleObserver) object,
                    (LifecycleEventObserver) object);
        }

        // 观察者是 LifecycleEventObserver
        if (isFullLifecycleObserver) {
            return new FullLifecycleObserverAdapter((FullLifecycleObserver) object, null);
        }

        if (isLifecycleEventObserver) {
            return (LifecycleEventObserver) object;
        }

        final Class<?> klass = object.getClass();
        int type = getObserverConstructorType(klass);

        // 观察者是通过 apt 产生的类
        if (type == GENERATED_CALLBACK) {
            List<Constructor<? extends GeneratedAdapter>> constructors =
                    sClassToAdapters.get(klass);
            if (constructors.size() == 1) {
                GeneratedAdapter generatedAdapter = createGeneratedAdapter(
                        constructors.get(0), object);
                return new SingleGeneratedAdapterObserver(generatedAdapter);
            }
            GeneratedAdapter[] adapters = new GeneratedAdapter[constructors.size()];
            for (int i = 0; i < constructors.size(); i++) {
                adapters[i] = createGeneratedAdapter(constructors.get(i), object);
            }
            return new CompositeGeneratedAdaptersObserver(adapters);
        }
        
        // 观察者需要通过反射生成一个 wrapper
        return new ReflectiveGenericLifecycleObserver(object);
    }

    ...

    public static String getAdapterName(String className) {
        return className.replace(".", "_") + "_LifecycleAdapter";
    }
}
```
逻辑很清晰，根据 LifecycleObserver 类型不用转成不同的 LifecycleEventObserver,

注解有两种使用用途。

场景一：runtime 时期使用反射生成 wrapper
```
class ReflectiveGenericLifecycleObserver implements LifecycleEventObserver {
    private final Object mWrapped;
    private final CallbackInfo mInfo;

    ReflectiveGenericLifecycleObserver(Object wrapped) {
        mWrapped = wrapped;
        mInfo = ClassesInfoCache.sInstance.getInfo(mWrapped.getClass());
    }

    @Override
    public void onStateChanged(LifecycleOwner source, Event event) {
        mInfo.invokeCallbacks(source, event, mWrapped);
    }
}
```

CallbackInfo 是关键，通过反射收集当前 LifecycleObserver 的回调信息。onStateChanged 中通过反射调用时，不会因为因为缺少 method 报错。

场景二：编译时使用 apt 生成 className + _LifecycleAdapter
除了利用反射， Lifecycle 还提供了 apt 方式处理注解。

添加 gradle 依赖：
```
dependencies {
    // java 写法
    annotationProcessor "androidx.lifecycle:lifecycle-compiler:2.3.1"
    // kotlin 写法
    kapt "androidx.lifecycle:lifecycle-compiler:2.3.1"
}
```
这样在编译器就会根据 LifecyceObserver 类名生成一个添加 _LifecycleAdapter 后缀的类。 
比如我们加了 onCreat 和 onStart 的注解，生成的代码如下：
```
public class MyEventObserver_LifecycleAdapter implements GeneratedAdapter {
  final MyEventObserver mReceiver;

  MyEventObserver_LifecycleAdapter(MyEventObserver receiver) {
    this.mReceiver = receiver;
  }

  @Override
  public void callMethods(LifecycleOwner owner, Lifecycle.Event event, boolean onAny,
      MethodCallsLogger logger) {
    boolean hasLogger = logger != null;
    if (onAny) {
      return;
    }
    if (event == Lifecycle.Event.ON_CREATE) {
      if (!hasLogger || logger.approveCall("onCreate", 1)) {
        mReceiver.onCreate();
      }
      return;
    }
    if (event == Lifecycle.Event.ON_START) {
      if (!hasLogger || logger.approveCall("onStart", 1)) {
        mReceiver.onStart();
      }
      return;
    }
  }
}
```
apt 减少了反射的调用，性能更好，当然会牺牲一些编译速度。

为什么要使用注解
生命周期的 Event 种类很多，我们往往不需要全部实现，如过不使用注解，可能需要实现所有方法，产生额外的无用代码

上面代码中的 FullLifecycleObserver 就是一个全部方法的接口
```
interface FullLifecycleObserver extends LifecycleObserver {

    void onCreate(LifecycleOwner owner);

    void onStart(LifecycleOwner owner);

    void onResume(LifecycleOwner owner);

    void onPause(LifecycleOwner owner);

    void onStop(LifecycleOwner owner);

    void onDestroy(LifecycleOwner owner);
}
```
从接口不是 public 的( java 代码 ) 可以看出，官方也无意让我们使用这样的接口，增加开发者负担。

遭废弃的原因
既然注解这么好，为什么又要废弃呢？

This annotation required the usage of code generation or reflection, which should be avoided.

从官方文档的注释可以看到，注解要么依赖反射降低运行时性能，要么依靠 APT 降低编译速度，不是完美的方案。

我们之所引入注解，无非是不想多实现几个空方法。早期 Android 工程不支持 Java8 编译，接口没有 default 方法， 现如今 Java8 已经是默认配置，
可以为接口添加 default 方法，此时注解已经失去了存在的意义。

如今官方推荐使用 DefaultLifecycleObserver 接口来定义你的 LifecycleObserver
```
public interface DefaultLifecycleObserver extends FullLifecycleObserver {

    @Override
    default void onCreate(@NonNull LifecycleOwner owner) {
    }

    @Override
    default void onStart(@NonNull LifecycleOwner owner) {
    }

    @Override
    default void onResume(@NonNull LifecycleOwner owner) {
    }

    @Override
    default void onPause(@NonNull LifecycleOwner owner) {
    }

    @Override
    default void onStop(@NonNull LifecycleOwner owner) {
    }

    @Override
    default void onDestroy(@NonNull LifecycleOwner owner) {
    }
}
```