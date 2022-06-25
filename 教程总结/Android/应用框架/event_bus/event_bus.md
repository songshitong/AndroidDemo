https://juejin.cn/post/7028212222312579103

//todo 进阶  LiveEventBus
https://www.jianshu.com/p/e7b4cae546f3

版本 eventbus 3.3.1



EventBus in 3 steps
1 Define events:   事件的发布与订阅都基于MessageEvent
```
public static class MessageEvent {
 
  }
```

2 Prepare subscribers: Declare and annotate your subscribing method, optionally specify a thread mode:
```
@Subscribe(threadMode = ThreadMode.MAIN)  
public void onMessageEvent(MessageEvent event) {

  };
  
@Override
 public void onStart() {
     super.onStart();
     EventBus.getDefault().register(this);
 }

 @Override
 public void onStop() {
     super.onStop();
     EventBus.getDefault().unregister(this);
 }  
```
3. Post events:
```
EventBus.getDefault().post(new MessageEvent());
```
 
 
 
 流程分析
 注册，注解处理，确定订阅关系，消息分发 
 反注册        
 特殊的注解处理器，粘性事件




订阅的注解
```
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Subscribe {
    //线程模式，默认为 POSTING   在哪发送在哪接收
    ThreadMode threadMode() default ThreadMode.POSTING;

    //粘性事件  将以前的消息发送给订阅者
    boolean sticky() default false;

    //订阅者优先级影响事件传递的顺序。
    //在同一个交付线程（{@link ThreadMode}）内，更高优先级的订阅者会先收到事件
    int priority() default 0;
}
```

//线程模式
```
public enum ThreadMode {
    //默认的 在哪个线程发送事件就在对应的线程处理事件
    POSTING, //POSTING外派 外出点和工作的公司是一个组织

    //主线程
    MAIN,

    //主线程 有序的
    MAIN_ORDERED,

    //子线程 如果是在子线程中发送事件，则直接在该子线程中处理事件。反之，如果是在主线程中发送事件，则需要将该事件入消息队列，
    //切换到子线程，用线程池来有序处理该事件。（如果不是Android中使用，总是使用该模式）
    BACKGROUND,

    //无论是在哪个线程发送事件，都会将该事件入消息队列，通过线程池在子线程上处理事件。如果订阅者方法的执行可能需要一些时间(如网络访问)，
    //则应使用此模式
    ASYNC
}
```


初始化
EventBus.java
```
  public static EventBus getDefault() {
        EventBus instance = defaultInstance;
        if (instance == null) {
            synchronized (EventBus.class) {
                instance = EventBus.defaultInstance;
                if (instance == null) {
                    instance = EventBus.defaultInstance = new EventBus();
                }
            }
        }
        return instance;
    }

  public EventBus() {
        this(DEFAULT_BUILDER);
    }
 
  EventBus(EventBusBuilder builder) {
        logger = builder.getLogger();
        subscriptionsByEventType = new HashMap<>();
        typesBySubscriber = new HashMap<>();
        stickyEvents = new ConcurrentHashMap<>();
        mainThreadSupport = builder.getMainThreadSupport();
        mainThreadPoster = mainThreadSupport != null ? mainThreadSupport.createPoster(this) : null;
        backgroundPoster = new BackgroundPoster(this);
        asyncPoster = new AsyncPoster(this);
        //subscriberInfoIndexes 编译期生成的event与订阅者，减少反射的使用
        //ignoreGeneratedIndex是否强制使用反射
        indexCount = builder.subscriberInfoIndexes != null ? builder.subscriberInfoIndexes.size() : 0;
        subscriberMethodFinder = new SubscriberMethodFinder(builder.subscriberInfoIndexes,
                builder.strictMethodVerification, builder.ignoreGeneratedIndex);
        logSubscriberExceptions = builder.logSubscriberExceptions;
        logNoSubscriberMessages = builder.logNoSubscriberMessages;
        sendSubscriberExceptionEvent = builder.sendSubscriberExceptionEvent;
        sendNoSubscriberEvent = builder.sendNoSubscriberEvent;
        throwSubscriberException = builder.throwSubscriberException;
        eventInheritance = builder.eventInheritance;
        //Executors.newCachedThreadPool()
        executorService = builder.executorService;
    }        
```
EventBus.getDefault()方法其实就是通过单例模式返回EventBus的实例


注册
```
public void register(Object subscriber) {
        ...
        //获取订阅者类
        Class<?> subscriberClass = subscriber.getClass();
        //根据订阅者类来获取到订阅方法
        List<SubscriberMethod> subscriberMethods = subscriberMethodFinder.findSubscriberMethods(subscriberClass);
        synchronized (this) {
            for (SubscriberMethod subscriberMethod : subscriberMethods) {
                //遍历订阅方法，调用订阅方法
                subscribe(subscriber, subscriberMethod);
            }
        }
    }
```
方法中的参数subscriber就是我们调用方法是传入的this，所以也就是表示Activity、Fragment。简单概括一下就是：我们通过获取订阅者的类对象，
然后找到其订阅方法，调用subscribe订阅方法进行订阅。
所以重点就要看看他是怎么找到订阅方法以及怎么订阅方法里面做了什么？往下走：


找到订阅方法
SubscriberMethodFinder.java
```
 List<SubscriberMethod> findSubscriberMethods(Class<?> subscriberClass) {
        //先从缓存里找订阅方法
        List<SubscriberMethod> subscriberMethods = METHOD_CACHE.get(subscriberClass);
        if (subscriberMethods != null) {
            //如果缓存里有，直接返回使用
            return subscriberMethods;
        }
        //是否强制使用反射而忽略Subscriber Index，ignoreGeneratedIndex默认为false
        if (ignoreGeneratedIndex) {
            subscriberMethods = findUsingReflection(subscriberClass);
        } else {
            subscriberMethods = findUsingInfo(subscriberClass);
        }
        if (subscriberMethods.isEmpty()) {
        //如果没有找到任何订阅方法，抛出异常，提醒用户使用 @Subscribe 方法来声明订阅方法
        //也就是说，如果用户register注册了，但是没有任何@Subscribe订阅方法，会抛出异常来提示用户
            throw new EventBusException("Subscriber " + subscriberClass
                    + " and its super classes have no public methods with the @Subscribe annotation");
        } else {
            //如果订阅方法不为空，放入缓存中，以方便下次复用，key为订阅类的类名
            METHOD_CACHE.put(subscriberClass, subscriberMethods);
            return subscriberMethods;
        }
    }
```
接下来看看是怎么找到并返回订阅者列表的，先看看findUsingReflection方法，即直接使用反射，不使用 subscriber index：
```
    private List<SubscriberMethod> findUsingReflection(Class<?> subscriberClass) {
        //从FIND_STATE_POOL取出findState 如果没有就新建一个
        FindState findState = prepareFindState();
        //复用后的属性重置
        findState.initForSubscriber(subscriberClass);
        while (findState.clazz != null) {
            findUsingReflectionInSingleClass(findState);
            findState.moveToSuperclass();
        }
        //findState回收复用并返回订阅方法列表
        return getMethodsAndRelease(findState);
    }
  
   private static final int POOL_SIZE = 4;
   private static final FindState[] FIND_STATE_POOL = new FindState[POOL_SIZE];
    private FindState prepareFindState() {
        synchronized (FIND_STATE_POOL) {
            //POOL_SIZE=4
            for (int i = 0; i < POOL_SIZE; i++) {
                FindState state = FIND_STATE_POOL[i];
                if (state != null) {
                    FIND_STATE_POOL[i] = null;
                    return state;
                }
            }
        }
        return new FindState();
    }    
   
    
   private List<SubscriberMethod> getMethodsAndRelease(FindState findState) {
        List<SubscriberMethod> subscriberMethods = new ArrayList<>(findState.subscriberMethods);
        //findState回收复用
        findState.recycle();
        synchronized (FIND_STATE_POOL) {
            for (int i = 0; i < POOL_SIZE; i++) {
                if (FIND_STATE_POOL[i] == null) {
                    FIND_STATE_POOL[i] = findState;
                    break;
                }
            }
        }
        return subscriberMethods;
    }   
 
 private void findUsingReflectionInSingleClass(FindState findState) {
        Method[] methods;
        try {
            // This is faster than getMethods, especially when subscribers are fat classes like Activities
            //返回类声明的方法，包括 public, protected, default (package)，但不包括继承的方法
            methods = findState.clazz.getDeclaredMethods();
        } catch (Throwable th) {
           //获取失败
            // Workaround for java.lang.NoClassDefFoundError, see https://github.com/greenrobot/EventBus/issues/149
            try {
                返回由类或接口声明的以及从超类和超接口继承的所有公共方法。
                methods = findState.clazz.getMethods();
            } catch (LinkageError error) { // super class of NoClassDefFoundError to be a bit more broad...
                String msg = "Could not inspect methods of " + findState.clazz.getName();
                if (ignoreGeneratedIndex) {
                    msg += ". Please consider using EventBus annotation processor to avoid reflection.";
                } else {
                    msg += ". Please make this class visible to EventBus annotation processor to avoid reflection.";
                }
                
                throw new EventBusException(msg, error);
            }
            // 因为getMethods()方法已经获取了超类的方法，所以这里设置不再去检查超类
            findState.skipSuperClasses = true;
        }
        //遍历找到的方法
        for (Method method : methods) {
            //获取方法修饰符: public->1;private->2;protected->4;static->8;final->16
            int modifiers = method.getModifiers();
            //如果是public，且不是 abstract | static 类的
            //private static final int BRIDGE = 0x40;
            // private static final int SYNTHETIC = 0x1000;
            //MODIFIERS_IGNORE = Modifier.ABSTRACT | Modifier.STATIC | BRIDGE | SYNTHETIC; 
            if ((modifiers & Modifier.PUBLIC) != 0 && (modifiers & MODIFIERS_IGNORE) == 0) {
                //获取方法参数类型
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length == 1) {
                    //获取方法的注解 Subscribe
                    Subscribe subscribeAnnotation = method.getAnnotation(Subscribe.class);
                    if (subscribeAnnotation != null) {
                        //第一个参数为eventType
                        Class<?> eventType = parameterTypes[0];
                        //检查是否已经添加了订阅该类型事件的订阅方法，true->没有添加；false->已添加 
                        if (findState.checkAdd(method, eventType)) {
                            //没有添加过                    
                            ThreadMode threadMode = subscribeAnnotation.threadMode();
                            //根据找到的参数来新建一个订阅方法对象，加入 subscriberMethods 列表中
                            findState.subscriberMethods.add(new SubscriberMethod(method, eventType, threadMode,
                                    subscribeAnnotation.priority(), subscribeAnnotation.sticky()));
                        }
                    }
                } else if (strictMethodVerification && method.isAnnotationPresent(Subscribe.class)) {
                    //方法必须只有1个参数
                    String methodName = method.getDeclaringClass().getName() + "." + method.getName();
                    throw new EventBusException("@Subscribe method " + methodName +
                            "must have exactly 1 parameter but has " + parameterTypes.length);
                }
            } else if (strictMethodVerification && method.isAnnotationPresent(Subscribe.class)) {
                String methodName = method.getDeclaringClass().getName() + "." + method.getName();
                throw new EventBusException(methodName +
                        " is a illegal @Subscribe method: must be public, non-static, and non-abstract");
            }
        }
    }    
```
关于 checkAdd方法，进一步深入：
```
 boolean checkAdd(Method method, Class<?> eventType) {
            // 2 level check: 1st level with event type only (fast), 2nd level with complete signature when required.
            // Usually a subscriber doesn't have methods listening to the same event type.
            //anyMethodByEventType 是HashMap
            Object existing = anyMethodByEventType.put(eventType, method);
            if (existing == null) {
                //表示还没有存在订阅该类型事件的订阅方法
                return true;
            } else {
                //已经存在订阅该类型事件的订阅方法了
                //existing就是先存入anyMethodByEventType的订阅统一类型事件的订阅方法
                if (existing instanceof Method) {
                    if (!checkAddWithMethodSignature((Method) existing, eventType)) {
                        // Paranoia check
                        throw new IllegalStateException();
                    }
                    // Put any non-Method object to "consume" the existing Method
                    anyMethodByEventType.put(eventType, this);
                }
                return checkAddWithMethodSignature(method, eventType);
            }
        }
        
private boolean checkAddWithMethodSignature(Method method, Class<?> eventType) {
     methodKeyBuilder.setLength(0);
     methodKeyBuilder.append(method.getName());
     methodKeyBuilder.append('>').append(eventType.getName());
    // "方法名>事件类型"
    // 意图：这样如果存在在同一个类中，有多个订阅方法订阅了同一个事件，这样当这个事件分发的时候，所有的订阅方法都会收到该事件。
     String methodKey = methodKeyBuilder.toString();
     Class<?> methodClass = method.getDeclaringClass();
     Class<?> methodClassOld = subscriberClassByMethodKey.put(methodKey, methodClass);
     
     //isAssignableFrom() 方法说明：  
     //当前的Class对象所表示的类，是不是参数中传递的Class对象所表示的类的父类，超接口，或者是相同的类型。
     //是则返回true，否则返回false。
     if (methodClassOld == null || methodClassOld.isAssignableFrom(methodClass)) {
         // Only add if not already found in a sub class
         // 只有在子类中没有找到的情况下才添加
         return true;
     } else {
         // Revert the put, old class is further down the class hierarchy
         subscriberClassByMethodKey.put(methodKey, methodClassOld);
         return false;
 }        
```

刚刚看了直接调用findUsingReflection方法利用反射来找到订阅方法，接着我们看看如何利用 Subscribe index 来找到订阅方法。
EventBus 推荐你使用注解处理器，避免在运行时使用反射来查找订阅方法，而是在编译的时候查找。
添加自己的subscriber index  省略了gradle的配置
使用subscriber index可以避免在运行时使用反射进行昂贵的订阅者方法查找。
https://greenrobot.org/eventbus/documentation/subscriber-index/
```
EventBus.builder().addIndex(new MyEventBusIndex()).installDefaultEventBus();

EventBus eventBus = EventBus.getDefault();
```
使用annotationProcessor来生成index   文件是EventBus自动生成的，将订阅信息存储在SubscriberInfoIndex.getSubscriberInfo
```
/** This class is generated by EventBus, do not edit. */
public class MyEventBusIndex implements SubscriberInfoIndex {
    private static final Map<Class<?>, SubscriberInfo> SUBSCRIBER_INDEX;

    static {
        //key -> 订阅者类对象；value -> 订阅信息
        SUBSCRIBER_INDEX = new HashMap<Class<?>, SubscriberInfo>();
        //构建SimpleSubscriberInfo
        putIndex(new SimpleSubscriberInfo(MainActivity.class, true, new SubscriberMethodInfo[] {
            new SubscriberMethodInfo("firstOnTestEvent", TestEvent.class, ThreadMode.MAIN),
            new SubscriberMethodInfo("messageEvent", MessageEvent.class, ThreadMode.BACKGROUND, 6, true),
        }));

        putIndex(new SimpleSubscriberInfo(SecondActivity.class, true, new SubscriberMethodInfo[] {
            new SubscriberMethodInfo("onTestEvent", TestEvent.class),
        }));

    }

    private static void putIndex(SubscriberInfo info) {
        SUBSCRIBER_INDEX.put(info.getSubscriberClass(), info);
    }

    @Override
    public SubscriberInfo getSubscriberInfo(Class<?> subscriberClass) {
        SubscriberInfo info = SUBSCRIBER_INDEX.get(subscriberClass);
        if (info != null) {
            return info;
        } else {
            return null;
        }
    }
}

```

接着我们回过头看利用 Subscriber Index查找订阅方法findUsingInfo()
```
 private List<SubscriberMethod> findUsingInfo(Class<?> subscriberClass) {
        FindState findState = prepareFindState();
        findState.initForSubscriber(subscriberClass);
        //这里使用了一个while循环，表示子类查找完了，会去父类继续查找
        while (findState.clazz != null) {
            //去 index 文件中查找订阅信息
            findState.subscriberInfo = getSubscriberInfo(findState);
            if (findState.subscriberInfo != null) {
                SubscriberMethod[] array = findState.subscriberInfo.getSubscriberMethods();
                //遍历订阅方法  
                for (SubscriberMethod subscriberMethod : array) {
                    //检查是否已经添加了该订阅方法
                    if (findState.checkAdd(subscriberMethod.method, subscriberMethod.eventType)) {
                       //未添加，将找到的订阅方法添加到订阅方法列表中
                        findState.subscriberMethods.add(subscriberMethod);
                    }
                }
            } else {
                //如果EventBusIndex返回的订阅方法为空，则使用反射方法来查找订阅方法
                //这样可以把高频的方法放入EventBusIndex，低频的使用反射，即减少反射性能损耗，又能缩短编译期的时间
                findUsingReflectionInSingleClass(findState);
            }
            //查找父类
            findState.moveToSuperclass();
        }
        //返回订阅方法列表
        return getMethodsAndRelease(findState);
    }

   private SubscriberInfo getSubscriberInfo(FindState findState) {
        //subscriberInfo 不为空，表示已经找到了订阅信息，则这次需要往父类查找
        //上面EventBusIndex构建的是SimpleSubscriberInfo，这里没有传入父类，如果自己继承AbstractSubscriberInfo
        if (findState.subscriberInfo != null && findState.subscriberInfo.getSuperSubscriberInfo() != null) {
            SubscriberInfo superclassInfo = findState.subscriberInfo.getSuperSubscriberInfo();
            //确定此次查找的正是父类
            if (findState.clazz == superclassInfo.getSubscriberClass()) {
                return superclassInfo;
            }
        }
        //subscriberInfoIndexes 就是 EventBus.addIndex(MyEventBusIndex()) 加进来的
        if (subscriberInfoIndexes != null) {
            for (SubscriberInfoIndex index : subscriberInfoIndexes) {
               //就是执行 MyEventBusIndex 类中的 getSubscriberInfo 方法，来获取订阅信息
                SubscriberInfo info = index.getSubscriberInfo(findState.clazz);
                if (info != null) {
                    return info;
                }
            }
        }
        return null;
    }
```
原理也很简单，就是在编译期间生成了index文件，这样我们就不需要在运行时通过反射来查找了，直接通过index文件来查找。
另外， 通过生成的index文件，我们也可以很清晰的看到我们声明的订阅方法分布情况。
使用 Subscriber Index注意事项：
@Subscribe 方法及其类必须是公共的。
事件类必须是公共的。
@Subscribe 不能在匿名类中使用。



订阅
上面我们看了是如何找到订阅方法的，接着就是进一步看看订阅方法里面到底是如何实现订阅动作的。
EventBus.java
```
 private void subscribe(Object subscriber, SubscriberMethod subscriberMethod) {
         //通过订阅方法获得事件类型参数   订阅方法的第一个参数
        Class<?> eventType = subscriberMethod.eventType;
        //通过订阅者与订阅方法来构造出一个 订阅对象
        Subscription newSubscription = new Subscription(subscriber, subscriberMethod);
        //通过事件类型，找到 订阅对象的集合，这边是以 CopyOnWriteArrayList 的形式
        //存储类型是 事件类型->[(订阅者，订阅方法), ...]
        //Map<Class<?>, CopyOnWriteArrayList<Subscription>> subscriptionsByEventType
        CopyOnWriteArrayList<Subscription> subscriptions = subscriptionsByEventType.get(eventType);
        //订阅关系一般是一次确定，多次使用，这里使用CopyOnWriteArrayList
        if (subscriptions == null) {
            //如果订阅对象集合为空，则表明还没有注册过订阅了该类型事件的订阅方法。
            //新建一个list，然后将 该事件类型与这个新建的list，放入 subscriptionsByEventType Map 中
            subscriptions = new CopyOnWriteArrayList<>();
            subscriptionsByEventType.put(eventType, subscriptions);
        } else {
            //抛出异常来提示用户，该订阅者已经订阅了这个类型的事件
            if (subscriptions.contains(newSubscription)) {
                throw new EventBusException("Subscriber " + subscriber.getClass() + " already registered to event "
                        + eventType);
            }
        }
         //遍历 订阅列表
        int size = subscriptions.size();
        for (int i = 0; i <= size; i++) {
            //如果订阅方法中有声明优先级，则根据优先级，将该订阅方法加入到指定位置
           //否则，将该订阅方法加入到订阅对象列表的末尾
            if (i == size || subscriberMethod.priority > subscriptions.get(i).subscriberMethod.priority) {
                subscriptions.add(i, newSubscription);
                break;
            }
        }
        //通过订阅者来找到 其订阅的所有事件的类型列表 subscribedEvents    Map<Object, List<Class<?>>>
        //存储类型是 订阅者->[订阅事件类型, ...]
        List<Class<?>> subscribedEvents = typesBySubscriber.get(subscriber);
        if (subscribedEvents == null) {
            subscribedEvents = new ArrayList<>();
            typesBySubscriber.put(subscriber, subscribedEvents);
        }
        //将该事件类型加入到 事件类型列表中
        subscribedEvents.add(eventType);

        //如果订阅方法支持粘性事件
        if (subscriberMethod.sticky) {
            //是否考虑事件类的层次结构，默认为true 
            if (eventInheritance) {
                // Existing sticky events of all subclasses of eventType have to be considered.
                // Note: Iterating over all events may be inefficient with lots of sticky events,
                // thus data structure should be changed to allow a more efficient lookup
                // (e.g. an additional map storing sub classes of super classes: Class -> List<Class>).
                //注释意思是遍历不够高效，可以另加一个map记录子类与父类的关系，这样遍历就不用遍历全量事件类型
                //通过postSticky(Object event)发布粘性事件会添加进stickyEvents
                Set<Map.Entry<Class<?>, Object>> entries = stickyEvents.entrySet();
                //遍历所有事件，如果是他的超类，也进行发送
                for (Map.Entry<Class<?>, Object> entry : entries) {
                    Class<?> candidateEventType = entry.getKey();
                    if (eventType.isAssignableFrom(candidateEventType)) {
                        Object stickyEvent = entry.getValue();
                        //检查发送粘性事件 
                        checkPostStickyEventToSubscription(newSubscription, stickyEvent);
                    }
                }
            } else {
                 //根据事件类型获取粘性事件
                Object stickyEvent = stickyEvents.get(eventType);
                //检查发送粘性事件 
                checkPostStickyEventToSubscription(newSubscription, stickyEvent);
            }
        }
    }
```


注销
看完了注册，我们接着看看注销
```
public synchronized void unregister(Object subscriber) {
        //通过订阅者找到其订阅的所有事件类型列表
        List<Class<?>> subscribedTypes = typesBySubscriber.get(subscriber);
        if (subscribedTypes != null) {
            //遍历事件类型列表
            for (Class<?> eventType : subscribedTypes) {
              //通过事件类型，注销订阅者
                unsubscribeByEventType(subscriber, eventType);
            }
            //将该订阅者从typesBySubscriber map 中移除
            typesBySubscriber.remove(subscriber);
        } else {
            logger.log(Level.WARNING, "Subscriber to unregister was not registered before: " + subscriber.getClass());
        }
    }
    
    private void unsubscribeByEventType(Object subscriber, Class<?> eventType) {
        //通过事件类型来找到相关的订阅对象列表
        List<Subscription> subscriptions = subscriptionsByEventType.get(eventType);
        if (subscriptions != null) {
            int size = subscriptions.size();
            //遍历订阅对象列表
            for (int i = 0; i < size; i++) {
                Subscription subscription = subscriptions.get(i);
                if (subscription.subscriber == subscriber) {
                    //从订阅对象列表中找到该订阅者，将其 active 状态改为 false，并从订阅对象列表中移除
                    subscription.active = false;
                    subscriptions.remove(i);
                    i--;
                    size--;
                }
            }
        }
    }    
    
```


发送事件
```
  private final ThreadLocal<PostingThreadState> currentPostingThreadState = new ThreadLocal<PostingThreadState>() {
        @Override
        protected PostingThreadState initialValue() {
            return new PostingThreadState();
        }
    };
    
public void post(Object event) {
        //PostingThreadState 是事件与发送状态的封装类  使用ThreadLocal保证线程安全
        PostingThreadState postingState = currentPostingThreadState.get();
        List<Object> eventQueue = postingState.eventQueue;
        //将该事件添加到事件队列中  其实是个ArrayList
        eventQueue.add(event);

        if (!postingState.isPosting) {
            //检查是否在主线程中 
            postingState.isMainThread = isMainThread();
            postingState.isPosting = true;
            if (postingState.canceled) {
                throw new EventBusException("Internal error. Abort state was not reset");
            }
            try {
                while (!eventQueue.isEmpty()) {
                    //遍历事件队列，将事件逐一发送
                    postSingleEvent(eventQueue.remove(0), postingState);
                }
            } finally {
                //重置发送状态
                postingState.isPosting = false;
                postingState.isMainThread = false;
            }
        }
    }
    
private void postSingleEvent(Object event, PostingThreadState postingState) throws Error {
        //获取事件的类对象
        Class<?> eventClass = event.getClass();
        boolean subscriptionFound = false;
        //是否考虑事件类的层次结构，默认为true     如果事件有继承关系，考虑事件的超类
        if (eventInheritance) {
            //查找超类的所有事件类型
            List<Class<?>> eventTypes = lookupAllEventTypes(eventClass);
            int countTypes = eventTypes.size();
            for (int h = 0; h < countTypes; h++) {
                Class<?> clazz = eventTypes.get(h);
                 //将事件根据事件类型发送出去
                subscriptionFound |= postSingleEventForEventType(event, postingState, clazz);
            }
        } else {
            subscriptionFound = postSingleEventForEventType(event, postingState, eventClass);
        }
         //没有发现订阅该类型事件的订阅对象，也就是没有存在订阅该类型事件的订阅方法
        if (!subscriptionFound) {
            if (logNoSubscriberMessages) {
                logger.log(Level.FINE, "No subscribers registered for event " + eventClass);
            }
            if (sendNoSubscriberEvent && eventClass != NoSubscriberEvent.class &&
                    eventClass != SubscriberExceptionEvent.class) {
                post(new NoSubscriberEvent(this, event));
            }
        }
    } 
    
    
private boolean postSingleEventForEventType(Object event, PostingThreadState postingState, Class<?> eventClass) {
        //订阅对象列表
        CopyOnWriteArrayList<Subscription> subscriptions;
        synchronized (this) {
           //根据事件类型，查询订阅该类型事件的订阅者列表
            subscriptions = subscriptionsByEventType.get(eventClass);
        }
        //如果订阅对象列表不为空，则将事件逐一发送给这些订阅者
        if (subscriptions != null && !subscriptions.isEmpty()) {
            for (Subscription subscription : subscriptions) {
                postingState.event = event;
                postingState.subscription = subscription;
                boolean aborted;
                try {
                    //将事件发送给订阅对象
                    postToSubscription(subscription, event, postingState.isMainThread);
                    aborted = postingState.canceled;
                } finally {
                    //重置状态
                    postingState.event = null;
                    postingState.subscription = null;
                    postingState.canceled = false;
                }
                if (aborted) {
                    break;
                }
            }
            return true;
        }
        return false;
    }  
    
private void postToSubscription(Subscription subscription, Object event, boolean isMainThread) {
        //根据订阅者选择的线程模式来选择使用那种线程方式来分发处理该事件
        switch (subscription.subscriberMethod.threadMode) {
            case POSTING:
                //直接利用反射调用订阅方法
                invokeSubscriber(subscription, event);
                break;
            case MAIN:
                if (isMainThread) {
                     //如果当前处于主线程，直接反射调用订阅方法
                    invokeSubscriber(subscription, event);
                } else {
                   //利用Handler切换到主线程，最终还是执行invokeSubscriber
                    mainThreadPoster.enqueue(subscription, event);
                }
                break;
            case MAIN_ORDERED:
                if (mainThreadPoster != null) {
                   //将事件入队列，在主线程上有序执行
                    mainThreadPoster.enqueue(subscription, event);
                } else {
                    // temporary: technically not correct as poster not decoupled from subscriber
                    invokeSubscriber(subscription, event);
                }
                break;
            case BACKGROUND:
                if (isMainThread) {
                    //如果当前处于主线程中，将利用线程池，切换到子线程中处理，最终还是会调用invokeSubscriber
                    backgroundPoster.enqueue(subscription, event);
                } else {
                    //如果当前处于子线程，则直接在该子线程中处理事件
                    invokeSubscriber(subscription, event);
                }
                break;
            case ASYNC:
                //无论处于什么线程，最终都是利用线程池，切换到子线程中处理，最终还是会调用invokeSubscriber
                asyncPoster.enqueue(subscription, event);
                break;
            default:
                throw new IllegalStateException("Unknown thread mode: " + subscription.subscriberMethod.threadMode);
        }
    } 
    
void invokeSubscriber(Subscription subscription, Object event) {
    try {
        //利用反射调用订阅方法
        subscription.subscriberMethod.method.invoke(subscription.subscriber, event);
    } catch (InvocationTargetException e) {
        handleSubscriberException(subscription, event, e.getCause());
    } catch (IllegalAccessException e) {
        throw new IllegalStateException("Unexpected exception", e);
    }
}          
```
简单的用一句话概括一下：根据事件的类型，找到对应的订阅对象列表并遍历列表，然后根据订阅对象的线程模式来决定是在哪个线程处理该事件

看一下邮递者
```
interface Poster {
    void enqueue(Subscription subscription, Object event);
}
```

mainThreadPoster默认是DefaultAndroidMainThreadSupport 
```
public class DefaultAndroidMainThreadSupport implements MainThreadSupport {

    @Override
    public boolean isMainThread() {
        return Looper.getMainLooper() == Looper.myLooper();
    }

    @Override
    public Poster createPoster(EventBus eventBus) {
        //传入主线程的looper  时间限制10ms
        return new HandlerPoster(eventBus, Looper.getMainLooper(), 10);
    }
}
```
HandlerPoster
```
public class HandlerPoster extends Handler implements Poster {

    public void enqueue(Subscription subscription, Object event) {
        //PendingPost使用ArrayList池化进行对象复用，最大是10000
        PendingPost pendingPost = PendingPost.obtainPendingPost(subscription, event);
        synchronized (this) {
            queue.enqueue(pendingPost);
           
            if (!handlerActive) {
                handlerActive = true;
                //发送消息，通知主线程处理
                if (!sendMessage(obtainMessage())) {
                    throw new EventBusException("Could not send handler message");
                }
            }
            //如果handler正在处理中，先不发送消息
        }
    }

    @Override
    public void handleMessage(Message msg) {
        boolean rescheduled = false;
        try {
            long started = SystemClock.uptimeMillis();
            while (true) {
                PendingPost pendingPost = queue.poll();
                if (pendingPost == null) {
                    synchronized (this) {
                        // Check again, this time in synchronized
                        pendingPost = queue.poll();
                        if (pendingPost == null) {
                            //没有待发送的，标记handerActive为false
                            handlerActive = false;
                            return;
                        }
                    }
                }
                //调用eventBus的invokeSubscriber
                eventBus.invokeSubscriber(pendingPost);
                long timeInMethod = SystemClock.uptimeMillis() - started;
                //如果发送事件超过10ms，再次发送消息，标记handlerActive为true   
                //handler的msg是链表，此时不再派发事件，让handler的msg有机会运行，派发事件重新入队
                if (timeInMethod >= maxMillisInsideHandleMessage) {
                    if (!sendMessage(obtainMessage())) {
                        throw new EventBusException("Could not send handler message");
                    }
                    rescheduled = true;
                    return;
                }
            }
        } finally {
            handlerActive = rescheduled;
        }
    }
}
```

BackgroundPoster
```
final class BackgroundPoster implements Runnable, Poster {

    public void enqueue(Subscription subscription, Object event) {
        PendingPost pendingPost = PendingPost.obtainPendingPost(subscription, event);
        synchronized (this) {
            //入队
            queue.enqueue(pendingPost);
            if (!executorRunning) {
                executorRunning = true;
                //使用线程池执行
                eventBus.getExecutorService().execute(this);
            }
            //如果线程池正在工作中，先不发送
        }
    }

    @Override
    public void run() {
        try {
            try {
                while (true) {
                    //从队列中取出，最大等待1000ms
                    PendingPost pendingPost = queue.poll(1000);
                    if (pendingPost == null) {
                        synchronized (this) {
                            // Check again, this time in synchronized
                            pendingPost = queue.poll();
                            if (pendingPost == null) {
                            //队列为空，标记线程池停止运行
                                executorRunning = false;
                                return;
                            }
                        }
                    }
                    //最终调用eventBus的invokeSubscriber
                    eventBus.invokeSubscriber(pendingPost);
                }
            } catch (InterruptedException e) {
                eventBus.getLogger().log(Level.WARNING, Thread.currentThread().getName() + " was interruppted", e);
            }
        } finally {
            executorRunning = false;
        }
    }
}
```

AsyncPoster
```
class AsyncPoster implements Runnable, Poster {

    public void enqueue(Subscription subscription, Object event) {
        PendingPost pendingPost = PendingPost.obtainPendingPost(subscription, event);
        queue.enqueue(pendingPost);
        eventBus.getExecutorService().execute(this);
    }

    @Override
    public void run() {
        PendingPost pendingPost = queue.poll();
        if(pendingPost == null) {
            throw new IllegalStateException("No pending post available");
        }
        eventBus.invokeSubscriber(pendingPost);
    }
}
```



发送粘性事件
如果你在发送普通事件前没有注册过订阅者，那么这时你发送的事件是不会被接收执行的，这个事件也就被回收了。
而粘性事件就不一样了，你可以在发送粘性事件后，再去注册订阅者，一旦完成订阅，这个订阅者就会接收到这个粘性事件。
让我们从源码中看看，是如何实现的吧！
```
EventBus.getDefault().postSticky(new MessageEvent("Hello everyone!"));
```
EventBus.java
```
  public void postSticky(Object event) {
        synchronized (stickyEvents) {
           //同步锁，将粘性事件存入 stickyEvents
            stickyEvents.put(event.getClass(), event);
        }
        //事件加入后，与普通事件一样，调用 post() 方法发送事件
        // Should be posted after it is putted, in case the subscriber wants to remove immediately
        post(event);
    }
```
用了一个stickyEvents集合来保存粘性事件，存入后，与普通事件一样同样调用post()方法。
？？ 嗯  ？？，这时我就有疑问了，针对上面的使用场景，我先发送粘性事件，然后再去注册订阅，这时执行post方法去发送事件，
根本就没有对应的订阅者啊，肯定是发送失败的。所以，细想一下，想达到这样效果，订阅者注册订阅后应该再将这个存入下来的事件发送一下。
回到register -> subscribe方法：
checkPostStickyEventToSubscription(newSubscription, stickyEvent);
```
 private void checkPostStickyEventToSubscription(Subscription newSubscription, Object stickyEvent) {
        if (stickyEvent != null) {
            // If the subscriber is trying to abort the event, it will fail (event is not tracked in posting state)
            // --> Strange corner case, which we don't take care of here.
            //又到了根据线程模式发送事件
            postToSubscription(newSubscription, stickyEvent, isMainThread());
        }
    }
```