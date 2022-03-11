https://github.com/xfhy/Android-Notes/blob/master/Blogs/Android/%E4%B8%89%E6%96%B9%E5%BA%93%E5%8E%9F%E7%90%86/Glide%E4%B8%BB%E6%B5%81%E7%A8%8B%E6%BA%90%E7%A0%81%E8%A7%A3%E6%9E%90.md
Glide版本4.12.0

glide用法
Glide.with(this).load(url).into(imageView);
  with 返回的是RequestManager
  load 返回的是RequestBuilder
  into 返回的target
Request接口

1. 前言
   图片加载可能是现在所有APP都必备的,当然有很多选择,可以自己写一个库用来展示网络图片.也可以用Glide,Fresco,Picasso等等.Glide由于我们公司在用,
   而且这是谷歌也推荐的一个图片加载库,用了也很久了,一直没有去研究它的底层实现逻辑.这次一定要好好研究研究.毕竟,需要用到图片加载的地方实在太多了.

关于如何阅读源码,郭神以前总结过8个字:抽丝剥茧、点到即止.意思就是我们在看的时候,抓关键,主流程,不要沉迷在代码细节中无法自拔.
  本篇文章主要着重于展示网络图片主流程,代码细节就不展开了,实在是太庞大太庞大了,而且细节贼多....

查看Glide源码的方式很简单,新建一个demo,然后通过gradle引入Glide,然后就可以愉快的查看源码了.


2. 阅读前准备
   总所周知,Glide加载图片的基本方式是Glide.with(context).load(url).into(imageView);,三步走. 那么我们阅读主流程也是跟着这三步走,
   去看一下主流程是怎么实现的.

3. with()
```
 // Glide.java
  public static RequestManager with(@NonNull Context context) {
    //分析2
    return getRetriever(context).get(context);
  }
  private static RequestManagerRetriever getRetriever(@Nullable Context context) {
    ...
    //分析1 Glide单例 
    return Glide.get(context).getRequestManagerRetriever();
  }
  
  public RequestManagerRetriever getRequestManagerRetriever() {
    return requestManagerRetriever;
  }
```
首先分析1处进行Glide初始化,它其实里面是通过GlideBuilder进行的初始化
```
 //双校验锁单例
  public static Glide get(@NonNull Context context) {
    if (glide == null) {
      //注解module  //todo GeneratedAppGlideModule
      GeneratedAppGlideModule annotationGeneratedModule =
          getAnnotationGeneratedGlideModules(context.getApplicationContext());
      synchronized (Glide.class) {
        if (glide == null) {
          checkAndInitializeGlide(context, annotationGeneratedModule);
        }
      }
    }

    return glide;
  }
 
   private static void checkAndInitializeGlide(
      @NonNull Context context, @Nullable GeneratedAppGlideModule generatedAppGlideModule) {
    // In the thread running initGlide(), one or more classes may call Glide.get(context).
    // Without this check, those calls could trigger infinite recursion.
    if (isInitializing) {
      throw new IllegalStateException(
          "You cannot call Glide.get() in registerComponents(),"
              + " use the provided Glide instance instead");
    }
    isInitializing = true;
    initializeGlide(context, generatedAppGlideModule);
    isInitializing = false;
  }
  
  private static void initializeGlide(
      @NonNull Context context, @Nullable GeneratedAppGlideModule generatedAppGlideModule) {
    initializeGlide(context, new GlideBuilder(), generatedAppGlideModule);
  }
  
    private static void initializeGlide(
      @NonNull Context context,
      @NonNull GlideBuilder builder,
      @Nullable GeneratedAppGlideModule annotationGeneratedModule) {
    Context applicationContext = context.getApplicationContext();
    List<com.bumptech.glide.module.GlideModule> manifestModules = Collections.emptyList();
    if (annotationGeneratedModule == null || annotationGeneratedModule.isManifestParsingEnabled()) {
      manifestModules = new ManifestParser(applicationContext).parse();
    }

    if (annotationGeneratedModule != null
        && !annotationGeneratedModule.getExcludedModuleClasses().isEmpty()) {
      Set<Class<?>> excludedModuleClasses = annotationGeneratedModule.getExcludedModuleClasses();
      Iterator<com.bumptech.glide.module.GlideModule> iterator = manifestModules.iterator();
      while (iterator.hasNext()) {
        com.bumptech.glide.module.GlideModule current = iterator.next();
        if (!excludedModuleClasses.contains(current.getClass())) {
          continue;
        }
        if (Log.isLoggable(TAG, Log.DEBUG)) {
          Log.d(TAG, "AppGlideModule excludes manifest GlideModule: " + current);
        }
        iterator.remove();
      }
    }

    if (Log.isLoggable(TAG, Log.DEBUG)) {
      for (com.bumptech.glide.module.GlideModule glideModule : manifestModules) {
        Log.d(TAG, "Discovered GlideModule from manifest: " + glideModule.getClass());
      }
    }

    RequestManagerRetriever.RequestManagerFactory factory =
        annotationGeneratedModule != null
            ? annotationGeneratedModule.getRequestManagerFactory()
            : null;
    builder.setRequestManagerFactory(factory);
    for (com.bumptech.glide.module.GlideModule module : manifestModules) {
      module.applyOptions(applicationContext, builder);
    }
    if (annotationGeneratedModule != null) {
      annotationGeneratedModule.applyOptions(applicationContext, builder);
    }
    //使用builder初始化
    Glide glide = builder.build(applicationContext);
    for (com.bumptech.glide.module.GlideModule module : manifestModules) {
      try {
        module.registerComponents(applicationContext, glide, glide.registry);
      } catch (AbstractMethodError e) {
        throw new IllegalStateException(
          。。。。
      }
    }
    if (annotationGeneratedModule != null) {
      annotationGeneratedModule.registerComponents(applicationContext, glide, glide.registry);
    }
    //注册onLowMemory的监听
    applicationContext.registerComponentCallbacks(glide);
    Glide.glide = glide;
  } 
  
```
ComponentCallbacks与ComponentCallbacks2的实现
```
public void onTrimMemory(int level) {
    trimMemory(level);
  }
  
 public void trimMemory(int level) {
    ...
    // Request managers need to be trimmed before the caches and pools, in order for the latter to
    // have the most benefit.
    synchronized (managers) {
      for (RequestManager manager : managers) {
        manager.onTrimMemory(level);
      }
    }
    // memory cache needs to be trimmed before bitmap pool to trim re-pooled Bitmaps too. See #687.
    memoryCache.trimMemory(level);
    bitmapPool.trimMemory(level);
    arrayPool.trimMemory(level);
  }  
  
  @Override
  public void onLowMemory() {
    clearMemory();
  }
  
  public void clearMemory() {
    // memory cache needs to be cleared before bitmap pool to clear re-pooled Bitmaps too. See #687.
    memoryCache.clearMemory();
    bitmapPool.clearMemory();
    arrayPool.clearMemory();
  }
```
看一下build方法
//GlideBuilder.java
```
  Glide build(@NonNull Context context) {
    if (sourceExecutor == null) {
      //加载资源线程池  包括从网络加载
      sourceExecutor = GlideExecutor.newSourceExecutor();
    }

    if (diskCacheExecutor == null) {
      //磁盘缓存线程池
      diskCacheExecutor = GlideExecutor.newDiskCacheExecutor();
    }

    if (animationExecutor == null) {
       //动画线程池
      animationExecutor = GlideExecutor.newAnimationExecutor();
    }

    if (memorySizeCalculator == null) {
      memorySizeCalculator = new MemorySizeCalculator.Builder(context).build();
    }

    if (connectivityMonitorFactory == null) {
      connectivityMonitorFactory = new DefaultConnectivityMonitorFactory();
    }

    if (bitmapPool == null) {
      int size = memorySizeCalculator.getBitmapPoolSize();
      if (size > 0) {
        bitmapPool = new LruBitmapPool(size);
      } else {
        bitmapPool = new BitmapPoolAdapter();
      }
    }

    if (arrayPool == null) {
      arrayPool = new LruArrayPool(memorySizeCalculator.getArrayPoolSizeInBytes());
    }

    if (memoryCache == null) {
      memoryCache = new LruResourceCache(memorySizeCalculator.getMemoryCacheSize());
    }

    if (diskCacheFactory == null) {
      diskCacheFactory = new InternalCacheDiskCacheFactory(context);
    }

    if (engine == null) {
      //注意,这里有一个引擎,后面会使用到
      engine =
          new Engine(
              memoryCache,
              diskCacheFactory,
              diskCacheExecutor,
              sourceExecutor,
              GlideExecutor.newUnlimitedSourceExecutor(),
              animationExecutor,
              isActiveResourceRetentionAllowed);
    }

    if (defaultRequestListeners == null) {
      defaultRequestListeners = Collections.emptyList();
    } else {
      defaultRequestListeners = Collections.unmodifiableList(defaultRequestListeners);
    }

    GlideExperiments experiments = glideExperimentsBuilder.build();
    RequestManagerRetriever requestManagerRetriever =
        new RequestManagerRetriever(requestManagerFactory, experiments);

    return new Glide(
        context,
        engine,
        memoryCache,
        bitmapPool,
        arrayPool,
        requestManagerRetriever,
        connectivityMonitorFactory,
        logLevel,
        defaultRequestOptionsFactory,
        defaultTransitionOptions,
        defaultRequestListeners,
        experiments);
  }
```

Glide就是初始化一些线程池,引擎什么的,稍后会用到.

再从分析2处可以看到,Glide 有很多重载方法,但是都是调用的RequestManagerRetriever的get方法,下面我们随便看一个RequestManagerRetriever的get重载方法
```
  public RequestManager get(@NonNull Context context) {
    if (context == null) {
      throw new IllegalArgumentException("You cannot start a load on a null Context");
    } else if (Util.isOnMainThread() && !(context instanceof Application)) {
      //这些都会调用get方法
      if (context instanceof FragmentActivity) {
        return get((FragmentActivity) context);
      } else if (context instanceof Activity) {
        return get((Activity) context);
      } else if (context instanceof ContextWrapper
          // Only unwrap a ContextWrapper if the baseContext has a non-null application context.
          // Context#createPackageContext may return a Context without an Application instance,
          // in which case a ContextWrapper may be used to attach one.
          && ((ContextWrapper) context).getBaseContext().getApplicationContext() != null) {
        return get(((ContextWrapper) context).getBaseContext());
      }
    }
    //创建一个生命周期是application相同的RequestManager
    return getApplicationManager(context);
  }
  
    public RequestManager get(@NonNull FragmentActivity activity) {
    if (Util.isOnBackgroundThread()) {
      //子线程使用ApplicationContext
      return get(activity.getApplicationContext());
    } else {
      assertNotDestroyed(activity);
      frameWaiter.registerSelf(activity);
      //根据activity获取FragmentManager
      FragmentManager fm = activity.getSupportFragmentManager();
      return supportFragmentGet(activity, fm, /*parentHint=*/ null, isActivityVisible(activity));
    }
  }
  
    private RequestManager getApplicationManager(@NonNull Context context) {
    if (applicationManager == null) {
      synchronized (this) {
        if (applicationManager == null) {
          Glide glide = Glide.get(context.getApplicationContext());
          applicationManager =
              factory.build(
                  glide,
                  new ApplicationLifecycle(),
                  new EmptyRequestManagerTreeNode(),
                  context.getApplicationContext());
        }
      }
    }

    return applicationManager;
  }
```
上面得分2种情况,一种是非application,一种是传入的是application.这两种情况下的生命周期是不太一样的,
非application的会调用下面的fragmentGet方法创建一个fragment观察activity的生命周期,即activity销毁,那么图片就不会继续加载,释放资源等.
而传入的是application的话,生命周期就和application一样了.
```
 private RequestManager supportFragmentGet(
      @NonNull Context context,
      @NonNull FragmentManager fm,
      @Nullable Fragment parentHint,
      boolean isParentVisible) {
    SupportRequestManagerFragment current = getSupportRequestManagerFragment(fm, parentHint);
    RequestManager requestManager = current.getRequestManager();
    if (requestManager == null) {
      Glide glide = Glide.get(context);
      //current.getGlideLifecycle()就是fragment的声明周期
      requestManager =
          factory.build(
              glide, current.getGlideLifecycle(), current.getRequestManagerTreeNode(), context);
      if (isParentVisible) {
        requestManager.onStart();
      }
      //给这个fragment设置一个RequestManager
      current.setRequestManager(requestManager);
    }
    return requestManager;
  }
  
  
    private SupportRequestManagerFragment getSupportRequestManagerFragment(
      @NonNull final FragmentManager fm, @Nullable Fragment parentHint) {
    SupportRequestManagerFragment current =
        (SupportRequestManagerFragment) fm.findFragmentByTag(FRAGMENT_TAG);
    if (current == null) {
      current = pendingSupportRequestManagerFragments.get(fm);
      if (current == null) {
        //构建一个空fragment
        current = new SupportRequestManagerFragment();
        current.setParentFragmentHint(parentHint);
        pendingSupportRequestManagerFragments.put(fm, current);
        //将fragment添加到activity
        fm.beginTransaction().add(current, FRAGMENT_TAG).commitAllowingStateLoss();
        handler.obtainMessage(ID_REMOVE_SUPPORT_FRAGMENT_MANAGER, fm).sendToTarget();
      }
    }
    return current;
  }
```
可以看到,如果传入的是非application那么就去构建一个fragment用于观察加载的那张图片的所在activity的生命周期,如果已经销毁了,
那么肯定就没必要再加载了.最后with返回的是RequestManager.with这里知道这些就OK了.
```
public class SupportRequestManagerFragment extends Fragment {
  private final ActivityFragmentLifecycle lifecycle;

  @Nullable private RequestManager requestManager;

  public SupportRequestManagerFragment() {
    this(new ActivityFragmentLifecycle());
  }

  @VisibleForTesting
  @SuppressLint("ValidFragment")
  public SupportRequestManagerFragment(@NonNull ActivityFragmentLifecycle lifecycle) {
    this.lifecycle = lifecycle;
  }

  public void setRequestManager(@Nullable RequestManager requestManager) {
    this.requestManager = requestManager;
  }
  //对外公布生命周期
  @NonNull
  ActivityFragmentLifecycle getGlideLifecycle() {
    return lifecycle;
  }

  /** Returns the current {@link com.bumptech.glide.RequestManager} or null if none is put. */
  @Nullable
  public RequestManager getRequestManager() {
    return requestManager;
  }

  @Override
  public void onStart() {
    super.onStart();
    lifecycle.onStart();
  }

  @Override
  public void onStop() {
    super.onStop();
    //生命周期的回调
    lifecycle.onStop();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    lifecycle.onDestroy();
    unregisterFragmentWithRoot();
  }
}
```

ActivityFragmentLifecycle主要负责将生命周期回调给监听者
```
class ActivityFragmentLifecycle implements Lifecycle {
  private final Set<LifecycleListener> lifecycleListeners =
      Collections.newSetFromMap(new WeakHashMap<LifecycleListener, Boolean>());
  private boolean isStarted;
  private boolean isDestroyed;

  @Override
  public void addListener(@NonNull LifecycleListener listener) {
    lifecycleListeners.add(listener);

    if (isDestroyed) {
      listener.onDestroy();
    } else if (isStarted) {
      listener.onStart();
    } else {
      listener.onStop();
    }
  }

  @Override
  public void removeListener(@NonNull LifecycleListener listener) {
    lifecycleListeners.remove(listener);
  }

  void onStart() {
    isStarted = true;
    for (LifecycleListener lifecycleListener : Util.getSnapshot(lifecycleListeners)) {
      lifecycleListener.onStart();
    }
  }

  void onStop() {
    isStarted = false;
    for (LifecycleListener lifecycleListener : Util.getSnapshot(lifecycleListeners)) {
      lifecycleListener.onStop();
    }
  }

  void onDestroy() {
    isDestroyed = true;
    for (LifecycleListener lifecycleListener : Util.getSnapshot(lifecycleListeners)) {
      lifecycleListener.onDestroy();
    }
  }
}
```

4. load()
   再来看load方法,它是with返回的RequestManager里面的方法.
```
public RequestBuilder<Drawable> load(@Nullable String string) {
    return asDrawable().load(string);
  }
public RequestBuilder<Drawable> asDrawable() {
    return as(Drawable.class);
  }
public <ResourceType> RequestBuilder<ResourceType> as(
      @NonNull Class<ResourceType> resourceClass) {
    return new RequestBuilder<>(glide, this, resourceClass, context);
}
```   
load方法里面调用的asDrawable(),主要是初始化RequestBuilder

```
  protected RequestBuilder(
      @NonNull Glide glide,
      RequestManager requestManager,
      Class<TranscodeType> transcodeClass,
      Context context) {
    this.glide = glide;
    this.requestManager = requestManager;
    ////注意,这里传入的是Drawable.class
    this.transcodeClass = transcodeClass;
    this.context = context;
    this.transitionOptions = requestManager.getDefaultTransitionOptions(transcodeClass);
    this.glideContext = glide.getGlideContext();

    initRequestListeners(requestManager.getDefaultRequestListeners());
    apply(requestManager.getDefaultRequestOptions());
  }
  
  public RequestBuilder<TranscodeType> load(@Nullable String string) {
    return loadGeneric(string);
  }
  
  private RequestBuilder<TranscodeType> loadGeneric(@Nullable Object model) {
    if (isAutoCloneEnabled()) {
      return clone().loadGeneric(model);
    }
    this.model = model;
    isModelSet = true;
    return selfOrThrowIfLocked();
  }
```
transcode
编码;转码;转换编码;转换;译码

然后调用load进行model的赋值.load流程走完了,就是创建了一个RequestBuilder


5. into()
   下面就比较复杂了,我找网络通信的代码找了特别久才找到了,看Glide源码看得欲仙欲死.如郭神所说,into方法里面有着成吨的操作.
   各种复杂逻辑.前面load流程走完了是返回了一个RequestBuilder,那么into方法就是在RequestBuilder里面的.
RequestBuilder.java   
```
 public ViewTarget<ImageView, TranscodeType> into(@NonNull ImageView view) {
    Util.assertMainThread();
    Preconditions.checkNotNull(view);

    BaseRequestOptions<?> requestOptions = this;
    if (!requestOptions.isTransformationSet()
        && requestOptions.isTransformationAllowed()
        && view.getScaleType() != null) {
      //todo 位运算 与option
      switch (view.getScaleType()) {
        case CENTER_CROP:
          requestOptions = requestOptions.clone().optionalCenterCrop();
          break;
        case CENTER_INSIDE:
          requestOptions = requestOptions.clone().optionalCenterInside();
          break;
        case FIT_CENTER:
        case FIT_START:
        case FIT_END:
          requestOptions = requestOptions.clone().optionalFitCenter();
          break;
        case FIT_XY:
          requestOptions = requestOptions.clone().optionalCenterInside();
          break;
        case CENTER:
        case MATRIX:
        default:
          // Do nothing.
      }
    }
    //这里的transcodeClass是前面的Drawable.class
    //回调的线程池，将任务切换到主线程  内部通过主线程handler的post(runnable)切换到主线程
    return into(
        glideContext.buildImageViewTarget(view, transcodeClass),
        /*targetListener=*/ null,
        requestOptions,
        Executors.mainThreadExecutor());
  }
```   

into成吨的逻辑,从最后一行开始.首先进入GlideContext的buildImageViewTarget方法.

GlideContext.java
```
public <X> ViewTarget<ImageView, X> buildImageViewTarget(
      @NonNull ImageView imageView, @NonNull Class<X> transcodeClass) {
    return imageViewTargetFactory.buildTarget(imageView, transcodeClass);
  }

ImageViewTargetFactory.java
public class ImageViewTargetFactory {
  @NonNull
  @SuppressWarnings("unchecked")
  public <Z> ViewTarget<ImageView, Z> buildTarget(@NonNull ImageView view,
      @NonNull Class<Z> clazz) {
    if (Bitmap.class.equals(clazz)) {
      return (ViewTarget<ImageView, Z>) new BitmapImageViewTarget(view);
    } else if (Drawable.class.isAssignableFrom(clazz)) {
      return (ViewTarget<ImageView, Z>) new DrawableImageViewTarget(view);
    } else {
      throw new IllegalArgumentException(
          "Unhandled class: " + clazz + ", try .as*(Class).transcode(ResourceTranscoder)");
    }
  }
}

```

毫无疑问,这里创建的是DrawableImageViewTarget.然后DrawableImageViewTarget被作为参数传入into方法.
```
public class DrawableImageViewTarget extends ImageViewTarget<Drawable> {

  public DrawableImageViewTarget(ImageView view) {
    super(view);
  }

  @Deprecated
  public DrawableImageViewTarget(ImageView view, boolean waitForLayout) {
    super(view, waitForLayout);
  }

  @Override
  protected void setResource(@Nullable Drawable resource) {
    view.setImageDrawable(resource);
  }
}
```

看一下into方法
```
private <Y extends Target<TranscodeType>> Y into(
      @NonNull Y target,
      @Nullable RequestListener<TranscodeType> targetListener,
      BaseRequestOptions<?> options,
      Executor callbackExecutor) {
    Preconditions.checkNotNull(target);
    if (!isModelSet) {
      throw new IllegalArgumentException("You must call #load() before calling #into()");
    }
    //分析1 
    Request request = buildRequest(target, targetListener, options, callbackExecutor);

    Request previous = target.getRequest();
    if (request.isEquivalentTo(previous)
        && !isSkipMemoryCacheWithCompletePreviousRequest(options, previous)) {   
      if (!Preconditions.checkNotNull(previous).isRunning()) {
        previous.begin();
      }
      return target;
    }

    requestManager.clear(target);
    target.setRequest(request);
    //分析2
    requestManager.track(target, request);

    return target;
  }
```
buildRequest的调用链
```
private Request buildRequest(
      Target<TranscodeType> target,
      @Nullable RequestListener<TranscodeType> targetListener,
      BaseRequestOptions<?> requestOptions,
      Executor callbackExecutor) {
    return buildRequestRecursive(
        /*requestLock=*/ new Object(),
        target,
        targetListener,
        /*parentCoordinator=*/ null,
        transitionOptions,
        requestOptions.getPriority(),
        requestOptions.getOverrideWidth(),
        requestOptions.getOverrideHeight(),
        requestOptions,
        callbackExecutor);
  }

  private Request buildRequestRecursive(
      Object requestLock,
      Target<TranscodeType> target,
      @Nullable RequestListener<TranscodeType> targetListener,
      @Nullable RequestCoordinator parentCoordinator,
      TransitionOptions<?, ? super TranscodeType> transitionOptions,
      Priority priority,
      int overrideWidth,
      int overrideHeight,
      BaseRequestOptions<?> requestOptions,
      Executor callbackExecutor) {

    // Build the ErrorRequestCoordinator first if necessary so we can update parentCoordinator.
    ErrorRequestCoordinator errorRequestCoordinator = null;
    if (errorBuilder != null) {
      errorRequestCoordinator = new ErrorRequestCoordinator(requestLock, parentCoordinator);
      parentCoordinator = errorRequestCoordinator;
    }

    Request mainRequest =
        buildThumbnailRequestRecursive(
            requestLock,
            target,
            targetListener,
            parentCoordinator,
            transitionOptions,
            priority,
            overrideWidth,
            overrideHeight,
            requestOptions,
            callbackExecutor);

    if (errorRequestCoordinator == null) {
      return mainRequest;
    }

    int errorOverrideWidth = errorBuilder.getOverrideWidth();
    int errorOverrideHeight = errorBuilder.getOverrideHeight();
    if (Util.isValidDimensions(overrideWidth, overrideHeight) && !errorBuilder.isValidOverride()) {
      errorOverrideWidth = requestOptions.getOverrideWidth();
      errorOverrideHeight = requestOptions.getOverrideHeight();
    }

    Request errorRequest =
        errorBuilder.buildRequestRecursive(
            requestLock,
            target,
            targetListener,
            errorRequestCoordinator,
            errorBuilder.transitionOptions,
            errorBuilder.getPriority(),
            errorOverrideWidth,
            errorOverrideHeight,
            errorBuilder,
            callbackExecutor);
    errorRequestCoordinator.setRequests(mainRequest, errorRequest);
    return errorRequestCoordinator;
  }

  private Request buildThumbnailRequestRecursive(
      Object requestLock,
      Target<TranscodeType> target,
      RequestListener<TranscodeType> targetListener,
      @Nullable RequestCoordinator parentCoordinator,
      TransitionOptions<?, ? super TranscodeType> transitionOptions,
      Priority priority,
      int overrideWidth,
      int overrideHeight,
      BaseRequestOptions<?> requestOptions,
      Executor callbackExecutor) {
    if (thumbnailBuilder != null) {
      // Recursive case: contains a potentially recursive thumbnail request builder.
      if (isThumbnailBuilt) {
        throw new IllegalStateException(
            "You cannot use a request as both the main request and a "
                + "thumbnail, consider using clone() on the request(s) passed to thumbnail()");
      }

      TransitionOptions<?, ? super TranscodeType> thumbTransitionOptions =
          thumbnailBuilder.transitionOptions;

      if (thumbnailBuilder.isDefaultTransitionOptionsSet) {
        thumbTransitionOptions = transitionOptions;
      }

      Priority thumbPriority =
          thumbnailBuilder.isPrioritySet()
              ? thumbnailBuilder.getPriority()
              : getThumbnailPriority(priority);

      int thumbOverrideWidth = thumbnailBuilder.getOverrideWidth();
      int thumbOverrideHeight = thumbnailBuilder.getOverrideHeight();
      if (Util.isValidDimensions(overrideWidth, overrideHeight)
          && !thumbnailBuilder.isValidOverride()) {
        thumbOverrideWidth = requestOptions.getOverrideWidth();
        thumbOverrideHeight = requestOptions.getOverrideHeight();
      }
      //ThumbnailRequestCoordinator实现了RequestCoordinator, Request 用来处理全图与缩略图的加载
      ThumbnailRequestCoordinator coordinator =
          new ThumbnailRequestCoordinator(requestLock, parentCoordinator);
      //使用obtainRequest构建request    
      Request fullRequest =
          obtainRequest(
              requestLock,
              target,
              targetListener,
              requestOptions,
              coordinator,
              transitionOptions,
              priority,
              overrideWidth,
              overrideHeight,
              callbackExecutor);
      isThumbnailBuilt = true;
      // Recursively generate thumbnail requests.
      Request thumbRequest =
          thumbnailBuilder.buildRequestRecursive(
              requestLock,
              target,
              targetListener,
              coordinator,
              thumbTransitionOptions,
              thumbPriority,
              thumbOverrideWidth,
              thumbOverrideHeight,
              thumbnailBuilder,
              callbackExecutor);
      isThumbnailBuilt = false;
      //将fullRequest与thumbRequest组合
      coordinator.setRequests(fullRequest, thumbRequest);
      return coordinator;
    } else if (thumbSizeMultiplier != null) {
      // Base case: thumbnail multiplier generates a thumbnail request, but cannot recurse.
      ThumbnailRequestCoordinator coordinator =
          new ThumbnailRequestCoordinator(requestLock, parentCoordinator);
      Request fullRequest =
          obtainRequest(
              requestLock,
              target,
              targetListener,
              requestOptions,
              coordinator,
              transitionOptions,
              priority,
              overrideWidth,
              overrideHeight,
              callbackExecutor);
      BaseRequestOptions<?> thumbnailOptions =
          requestOptions.clone().sizeMultiplier(thumbSizeMultiplier);

      Request thumbnailRequest =
          obtainRequest(
              requestLock,
              target,
              targetListener,
              thumbnailOptions,
              coordinator,
              transitionOptions,
              getThumbnailPriority(priority),
              overrideWidth,
              overrideHeight,
              callbackExecutor);

      coordinator.setRequests(fullRequest, thumbnailRequest);
      return coordinator;
    } else {
      // Base case: no thumbnail.
      return obtainRequest(
          requestLock,
          target,
          targetListener,
          requestOptions,
          parentCoordinator,
          transitionOptions,
          priority,
          overrideWidth,
          overrideHeight,
          callbackExecutor);
    }
  }
```
分析1处,通过buildRequest构建一个Request,最后会走到RequestBuilder的obtainRequest方法构建一个SingleRequest.
```
  private Request obtainRequest(
      Object requestLock,
      Target<TranscodeType> target,
      RequestListener<TranscodeType> targetListener,
      BaseRequestOptions<?> requestOptions,
      RequestCoordinator requestCoordinator,
      TransitionOptions<?, ? super TranscodeType> transitionOptions,
      Priority priority,
      int overrideWidth,
      int overrideHeight,
      Executor callbackExecutor) {
    return SingleRequest.obtain(
        context,
        glideContext,
        requestLock,
                //String
        model,
        //Drawable.class
        transcodeClass,
        //options
        requestOptions,
        //宽高
        overrideWidth,
        overrideHeight,
        priority,
        //DrawableImageViewTarget
        target,
        targetListener,
        requestListeners,
        requestCoordinator,
        glideContext.getEngine(),
        transitionOptions.getTransitionFactory(),
        callbackExecutor);
  }
```

最后构建SingleRequest. 这个Request也就是我们要进行的请求,猜测大部分逻辑都是在这里面.

继续回到分析2处,requestManager.track(target, request);
RequestManager.java
```
  synchronized void track(@NonNull Target<?> target, @NonNull Request request) {
    //targetTracker是记录activity中的target，并将activity的声明周期回调给target
    targetTracker.track(target);
    //RequestTracker是记录Request,并对request进行canceling，restarting ，completed,failed操作
    requestTracker.runRequest(request);
  }
  
  RequestTracker.java
  public void runRequest(@NonNull Request request) {
    requests.add(request);
    if (!isPaused) {
      //开始搞事情
      request.begin();
    } else {
      request.clear();
      if (Log.isLoggable(TAG, Log.VERBOSE)) {
        Log.v(TAG, "Paused, delaying request");
      }
      pendingRequests.add(request);
    }
  }
```
除了开始请求，RequestManager还监听声明周期，实现请求的停止，重新加载
```
  public synchronized void onStart() {
    resumeRequests();
    targetTracker.onStart();
  }
 public synchronized void onStop() {
    pauseRequests();
    targetTracker.onStop();
  }
  @Override
  public synchronized void onDestroy() {
    targetTracker.onDestroy();
    for (Target<?> target : targetTracker.getAll()) {
      clear(target);
    }
    targetTracker.clear();
    //onDestroy清除请求，不再继续
    requestTracker.clearRequests();
    lifecycle.removeListener(this);
    lifecycle.removeListener(connectivityMonitor);
    Util.removeCallbacksOnUiThread(addSelfToLifecycle);
    glide.unregisterRequestManager(this);
  }

```
RequestTracker.java 移除后续的请求
```
  public void clearRequests() {
    for (Request request : Util.getSnapshot(requests)) {
      // It's unsafe to recycle the Request here because we don't know who might else have a
      // reference to it.
      clearAndRemove(request);
    }
    pendingRequests.clear();
  }
```
调用了Request的begin,看样子是要开始了,上面分析了这里是SingleRequest,所以直接看SingleRequest的begin().
SingleRequest.java
```
public void begin() {
    synchronized (requestLock) {
      assertNotCallingCallbacks();
      stateVerifier.throwIfRecycled();
      startTime = LogTime.getLogTime();
      if (model == null) {
        if (Util.isValidDimensions(overrideWidth, overrideHeight)) {
          width = overrideWidth;
          height = overrideHeight;
        }
        
        int logLevel = getFallbackDrawable() == null ? Log.WARN : Log.DEBUG;
        onLoadFailed(new GlideException("Received null model"), logLevel);
        return;
      }

      if (status == Status.RUNNING) {
        throw new IllegalArgumentException("Cannot restart a running request");
      }

      if (status == Status.COMPLETE) {
        //已加载完成   回调
        onResourceReady(
            resource, DataSource.MEMORY_CACHE, /* isLoadedFromAlternateCacheKey= */ false);
        return;
      }

      status = Status.WAITING_FOR_SIZE;
      //测量一下ImageView的大小
      if (Util.isValidDimensions(overrideWidth, overrideHeight)) {
        onSizeReady(overrideWidth, overrideHeight);
      } else {
        target.getSize(this);
      }

      if ((status == Status.RUNNING || status == Status.WAITING_FOR_SIZE)
          && canNotifyStatusChanged()) {
        target.onLoadStarted(getPlaceholderDrawable());
      }
      if (IS_VERBOSE_LOGGABLE) {
        logV("finished run method in " + LogTime.getElapsedMillis(startTime));
      }
    }
  }
```
测量一下ImageView的大小,如果在调用Glide加载图片时设置override设置宽高,那么直接会走onSizeReady方法处.如果没有写,
那么走target.getSize(this); 测量一下宽高,然后在里面还是会调用SingleRequest的onSizeReady()方法.
```
public void onSizeReady(int width, int height) {
    stateVerifier.throwIfRecycled();
    synchronized (requestLock) {
      ...
      if (status != Status.WAITING_FOR_SIZE) {
        return;
      }
      status = Status.RUNNING;

      float sizeMultiplier = requestOptions.getSizeMultiplier();
      this.width = maybeApplySizeMultiplier(width, sizeMultiplier);
      this.height = maybeApplySizeMultiplier(height, sizeMultiplier);
     ....
     //开始用引擎
      loadStatus =
          engine.load(
              glideContext,
              model,
              requestOptions.getSignature(),
              this.width,
              this.height,
              requestOptions.getResourceClass(),
              transcodeClass,
              priority,
              requestOptions.getDiskCacheStrategy(),
              requestOptions.getTransformations(),
              requestOptions.isTransformationRequired(),
              requestOptions.isScaleOnlyOrNoTransform(),
              requestOptions.getOptions(),
              requestOptions.isMemoryCacheable(),
              requestOptions.getUseUnlimitedSourceGeneratorsPool(),
              requestOptions.getUseAnimationPool(),
              requestOptions.getOnlyRetrieveFromCache(),
              this,
              callbackExecutor);

      // This is a hack that's only useful for testing right now where loads complete synchronously
      // even though under any executor running on any thread but the main thread, the load would
      // have completed asynchronously.
      if (status != Status.RUNNING) {
        loadStatus = null;
      }
      if (IS_VERBOSE_LOGGABLE) {
        logV("finished onSizeReady in " + LogTime.getElapsedMillis(startTime));
      }
    }
  }
```

开始用引擎load
Engine.java
```
public <R> LoadStatus load(
      GlideContext glideContext,
      Object model,
      Key signature,
      int width,
      int height,
      Class<?> resourceClass,
      Class<R> transcodeClass,
      Priority priority,
      DiskCacheStrategy diskCacheStrategy,
      Map<Class<?>, Transformation<?>> transformations,
      boolean isTransformationRequired,
      boolean isScaleOnlyOrNoTransform,
      Options options,
      boolean isMemoryCacheable,
      boolean useUnlimitedSourceExecutorPool,
      boolean useAnimationPool,
      boolean onlyRetrieveFromCache,
      ResourceCallback cb,
      Executor callbackExecutor) {
    long startTime = VERBOSE_IS_LOGGABLE ? LogTime.getLogTime() : 0;

    EngineKey key =
        keyFactory.buildKey(
            model,
            signature,
            width,
            height,
            transformations,
            resourceClass,
            transcodeClass,
            options);

    EngineResource<?> memoryResource;
    synchronized (this) {
      memoryResource = loadFromMemory(key, isMemoryCacheable, startTime);

      if (memoryResource == null) {
        return waitForExistingOrStartNewJob(
            glideContext,
            model,
            signature,
            width,
            height,
            resourceClass,
            transcodeClass,
            priority,
            diskCacheStrategy,
            transformations,
            isTransformationRequired,
            isScaleOnlyOrNoTransform,
            options,
            isMemoryCacheable,
            useUnlimitedSourceExecutorPool,
            useAnimationPool,
            onlyRetrieveFromCache,
            cb,
            callbackExecutor,
            key,
            startTime);
      }
    }

    // Avoid calling back while holding the engine lock, doing so makes it easier for callers to
    // deadlock.
    cb.onResourceReady(
        memoryResource, DataSource.MEMORY_CACHE, /* isLoadedFromAlternateCacheKey= */ false);
    return null;
  }
  
   private EngineResource<?> loadFromMemory(
      EngineKey key, boolean isMemoryCacheable, long startTime) {
    if (!isMemoryCacheable) {
      return null;
    }
   //从弱引用中找,有就直接返回
    EngineResource<?> active = loadFromActiveResources(key);
    if (active != null) {
      if (VERBOSE_IS_LOGGABLE) {
        logWithTimeAndKey("Loaded resource from active resources", startTime, key);
      }
      return active;
    }
    //缓存中
    EngineResource<?> cached = loadFromCache(key);
    if (cached != null) {
      if (VERBOSE_IS_LOGGABLE) {
        logWithTimeAndKey("Loaded resource from cache", startTime, key);
      }
      return cached;
    }

    return null;
  }
  
  private <R> LoadStatus waitForExistingOrStartNewJob(
      GlideContext glideContext,
      Object model,
      Key signature,
      int width,
      int height,
      Class<?> resourceClass,
      Class<R> transcodeClass,
      Priority priority,
      DiskCacheStrategy diskCacheStrategy,
      Map<Class<?>, Transformation<?>> transformations,
      boolean isTransformationRequired,
      boolean isScaleOnlyOrNoTransform,
      Options options,
      boolean isMemoryCacheable,
      boolean useUnlimitedSourceExecutorPool,
      boolean useAnimationPool,
      boolean onlyRetrieveFromCache,
      ResourceCallback cb,
      Executor callbackExecutor,
      EngineKey key,
      long startTime) {
    //查找之前缓存的EngineJob
    EngineJob<?> current = jobs.get(key, onlyRetrieveFromCache);
    if (current != null) {
      current.addCallback(cb, callbackExecutor);
      if (VERBOSE_IS_LOGGABLE) {
        logWithTimeAndKey("Added to existing load", startTime, key);
      }
      return new LoadStatus(cb, current);
    }
      //engineJob是为decodeJob干事情的,管理下载过程以及状态
    EngineJob<R> engineJob =
        engineJobFactory.build(
            key,
            isMemoryCacheable,
            useUnlimitedSourceExecutorPool,
            useAnimationPool,
            onlyRetrieveFromCache);
    
    //build方法最后一个参数是DecodeJob.Callback，resource加载完成后回调给engineJob
    DecodeJob<R> decodeJob =
        decodeJobFactory.build(
            glideContext,
            model,
            key,
            signature,
            width,
            height,
            resourceClass,
            transcodeClass,
            priority,
            diskCacheStrategy,
            transformations,
            isTransformationRequired,
            isScaleOnlyOrNoTransform,
            onlyRetrieveFromCache,
            options,
            engineJob);

    jobs.put(key, engineJob);
    //注册ResourceCallback
    engineJob.addCallback(cb, callbackExecutor);
    //开始真正的工作
    engineJob.start(decodeJob);

    if (VERBOSE_IS_LOGGABLE) {
      logWithTimeAndKey("Started new load", startTime, key);
    }
    return new LoadStatus(cb, engineJob);
  }
```
首先是从各种缓存中拿之前的数据,有则返回,没有则创建DecodeJob开始工作.
```
//EngineJob.java
public void start(DecodeJob<R> decodeJob) {
    this.decodeJob = decodeJob;
    GlideExecutor executor = decodeJob.willDecodeFromCache()
        ? diskCacheExecutor
        : getActiveSourceExecutor();
    executor.execute(decodeJob);
}
```
//todo 缓存的情况
我们这里暂时不考虑缓存的情况,那么使用的这里使用的GlideExecutor是GlideBuilder的build方法中初始化的
sourceExecutor = GlideExecutor.newSourceExecutor();
其实就是一个线程池. 然后DecodeJob是Runnable,放到线程池中去执行,所以我们知道找重点,去DecodeJob的run方法看
DecodeJob.java
```
 public void run() {  
    GlideTrace.beginSectionFormat("DecodeJob#run(model=%s)", model);
    DataFetcher<?> localFetcher = currentFetcher;
    try {
      if (isCancelled) {
      //已经取消
        notifyFailed();
        return;
      }
      //重点 这里是执行
      runWrapped();
    } catch (CallbackException e) {
      throw e;
    } catch (Throwable t) {     
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(
            TAG,
            "DecodeJob threw unexpectedly" + ", isCancelled: " + isCancelled + ", stage: " + stage,
            t);
      }
      // When we're encoding we've already notified our callback and it isn't safe to do so again.
      if (stage != Stage.ENCODE) {
        throwables.add(t);
        notifyFailed();
      }
      if (!isCancelled) {
        throw t;
      }
      throw t;
    } finally {
      if (localFetcher != null) {
        localFetcher.cleanup();
      }
      GlideTrace.endSection();
    }
  }
```

```
//解码数据的阶段
private enum Stage {
    /** The initial stage. */
    INITIALIZE,
    /** Decode from a cached resource. */
    RESOURCE_CACHE,
    /** Decode from cached source data. */
    DATA_CACHE,
    /** Decode from retrieved source. */
    SOURCE,
    /** Encoding transformed resources after a successful load. */
    ENCODE,
    /** No more viable stages. */
    FINISHED,
  }
 //获取下一个阶段就是把stage走一遍 
 private Stage getNextStage(Stage current) {
    switch (current) {
      case INITIALIZE:
        return diskCacheStrategy.decodeCachedResource()
            ? Stage.RESOURCE_CACHE
            : getNextStage(Stage.RESOURCE_CACHE);
      case RESOURCE_CACHE:
        return diskCacheStrategy.decodeCachedData()
            ? Stage.DATA_CACHE
            : getNextStage(Stage.DATA_CACHE);
      case DATA_CACHE:
        // Skip loading from source if the user opted to only retrieve the resource from cache.
        return onlyRetrieveFromCache ? Stage.FINISHED : Stage.SOURCE;
      case SOURCE:
      case FINISHED:
        return Stage.FINISHED;
      default:
        throw new IllegalArgumentException("Unrecognized stage: " + current);
    }
  }  
private void runWrapped() {
    //runReason在DecodeJob初始化的时候初始值是INITIALIZE,上面没有带大家看,这不是重点.
    switch (runReason) {
      case INITIALIZE:
        //获取下一个state
        stage = getNextStage(Stage.INITIALIZE);
        //这里会去获取一个SourceGenerator
        currentGenerator = getNextGenerator();
        runGenerators();
        break;
      case SWITCH_TO_SOURCE_SERVICE:
        runGenerators();
        break;
      case DECODE_DATA:
        decodeFromRetrievedData();
        break;
      default:
        throw new IllegalStateException("Unrecognized run reason: " + runReason);
    }
  }
  
  //一次完整的请求,会把这里的都走完.
  private DataFetcherGenerator getNextGenerator() {
    switch (stage) {
      case RESOURCE_CACHE:
        return new ResourceCacheGenerator(decodeHelper, this);
      case DATA_CACHE:
        return new DataCacheGenerator(decodeHelper, this);
      case SOURCE:
        //会走到这里  this是FetcherReadyCallback回调
        return new SourceGenerator(decodeHelper, this);
      case FINISHED:
        return null;
      default:
        throw new IllegalStateException("Unrecognized stage: " + stage);
    }
  }
  
 private void runGenerators() {
    currentThread = Thread.currentThread();
    startFetchTime = LogTime.getLogTime();
    boolean isStarted = false;
    while (!isCancelled
        && currentGenerator != null
        //重点
        && !(isStarted = currentGenerator.startNext())) {
      stage = getNextStage(stage);
      currentGenerator = getNextGenerator();

      if (stage == Stage.SOURCE) {
        reschedule();
        return;
      }
    }
    // We've run out of stages and generators, give up.
    if ((stage == Stage.FINISHED || isCancelled) && !isStarted) {
      notifyFailed();
    }

    // Otherwise a generator started a new load and we expect to be called back in
    // onDataFetcherReady.
  }  
```
Glide定义了几个步骤,在枚举类Stage里面.这是每一步的状态,当执行完一个状态的时候,又去获取下一个状态进行执行相应的逻辑.
在runGenerators方法里面有一个while控制.我们第一步是SourceGenerator,所以会走到SourceGenerator的startNext()里面.
SourceGenerator.java
```
public boolean startNext() {
    if (dataToCache != null) {
      Object data = dataToCache;
      dataToCache = null;
      cacheData(data);
    }
    //有缓存
    if (sourceCacheGenerator != null && sourceCacheGenerator.startNext()) {
      return true;
    }
    sourceCacheGenerator = null;

    loadData = null;
    boolean started = false;
    while (!started && hasNextModelLoader()) {
      loadData = helper.getLoadData().get(loadDataListIndex++);
      if (loadData != null
          && (helper.getDiskCacheStrategy().isDataCacheable(loadData.fetcher.getDataSource())
              || helper.hasLoadPath(loadData.fetcher.getDataClass()))) {
        started = true;
        startNextLoad(loadData);
      }
    }
    return started;
  }
 
   private void startNextLoad(final LoadData<?> toStart) {
   //这里的fetcher是HttpUrlFetcher
    loadData.fetcher.loadData(
        helper.getPriority(),
        new DataCallback<Object>() {
          @Override
          public void onDataReady(@Nullable Object data) {
            if (isCurrentRequest(toStart)) {
              onDataReadyInternal(toStart, data);
            }
          }

          @Override
          public void onLoadFailed(@NonNull Exception e) {
            if (isCurrentRequest(toStart)) {
              onLoadFailedInternal(toStart, e);
            }
          }
        });
  } 
```
主流程是根据HttpUrlFetcher去加载数据,这里就不分析为啥HttpUrlFetcher,比较麻烦,篇幅收不住.
HttpUrlFetcher.java
```
  public void loadData(
      @NonNull Priority priority, @NonNull DataCallback<? super InputStream> callback) {
    long startTime = LogTime.getLogTime();
    try {
      获取到了网络数据了
      InputStream result = loadDataWithRedirects(glideUrl.toURL(), 0, null, glideUrl.getHeaders());
            //将数据返回回去
      callback.onDataReady(result);
    } catch (IOException e) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "Failed to load data for url", e);
      }
      callback.onLoadFailed(e);
    } finally {
      if (Log.isLoggable(TAG, Log.VERBOSE)) {
        Log.v(TAG, "Finished http url fetcher fetch in " + LogTime.getElapsedMillis(startTime));
      }
    }
  }
```
终于,感觉要开始请求网络了,并且结果还通过回调回去了.来看一下loadDataWithRedirects方法
```
private InputStream loadDataWithRedirects(
      URL url, int redirects, URL lastUrl, Map<String, String> headers) throws HttpException {
    if (redirects >= MAXIMUM_REDIRECTS) {
      throw new HttpException(
          "Too many (> " + MAXIMUM_REDIRECTS + ") redirects!", INVALID_STATUS_CODE);
    } else {
      // Comparing the URLs using .equals performs additional network I/O and is generally broken.
      // See http://michaelscharf.blogspot.com/2006/11/javaneturlequals-and-hashcode-make.html.
      try {
        if (lastUrl != null && url.toURI().equals(lastUrl.toURI())) {
          throw new HttpException("In re-direct loop", INVALID_STATUS_CODE);
        }
      } catch (URISyntaxException e) {
        // Do nothing, this is best effort.
      }
    }

    urlConnection = buildAndConfigureConnection(url, headers);

    try {
      // Connect explicitly to avoid errors in decoders if connection fails.
      urlConnection.connect();
      // Set the stream so that it's closed in cleanup to avoid resource leaks. See #2352.
      stream = urlConnection.getInputStream();
    } catch (IOException e) {
      throw new HttpException(
          "Failed to connect or obtain data", getHttpStatusCodeOrInvalid(urlConnection), e);
    }

    if (isCancelled) {
      return null;
    }

    final int statusCode = getHttpStatusCodeOrInvalid(urlConnection);
    if (isHttpOk(statusCode)) {
      //重点   网络请求OK
      return getStreamForSuccessfulRequest(urlConnection);
    } else if (isHttpRedirect(statusCode)) {
      String redirectUrlString = urlConnection.getHeaderField(REDIRECT_HEADER_FIELD);
      if (TextUtils.isEmpty(redirectUrlString)) {
        throw new HttpException("Received empty or null redirect url", statusCode);
      }
      URL redirectUrl;
      try {
        redirectUrl = new URL(url, redirectUrlString);
      } catch (MalformedURLException e) {
        throw new HttpException("Bad redirect url: " + redirectUrlString, statusCode, e);
      }
      // Closing the stream specifically is required to avoid leaking ResponseBodys in addition
      // to disconnecting the url connection below. See #2352.
      cleanup();
      return loadDataWithRedirects(redirectUrl, redirects + 1, url, headers);
    } else if (statusCode == INVALID_STATUS_CODE) {
      throw new HttpException(statusCode);
    } else {
      try {
        throw new HttpException(urlConnection.getResponseMessage(), statusCode);
      } catch (IOException e) {
        throw new HttpException("Failed to get a response message", statusCode, e);
      }
    }
  }
  
  private InputStream getStreamForSuccessfulRequest(HttpURLConnection urlConnection)
      throws HttpException {
    try {
      if (TextUtils.isEmpty(urlConnection.getContentEncoding())) {
        int contentLength = urlConnection.getContentLength();
        stream = ContentLengthInputStream.obtain(urlConnection.getInputStream(), contentLength);
      } else {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
          Log.d(TAG, "Got non empty content encoding: " + urlConnection.getContentEncoding());
        }
        stream = urlConnection.getInputStream();
      }
    } catch (IOException e) {
      throw new HttpException(
          "Failed to obtain InputStream", getHttpStatusCodeOrInvalid(urlConnection), e);
    }
    return stream;
  }
```
这个就比较熟悉了,就是通过HttpURLConnection进行的网络请求,并且结果就是通过urlConnection.getInputStream();拿到的.
这里拿到了结果后,就返回了,然后就是上面的callback.onDataReady(result);进行回调,处理结果
//SourceGenerator.java
```
 void onDataReadyInternal(LoadData<?> loadData, Object data) {
    DiskCacheStrategy diskCacheStrategy = helper.getDiskCacheStrategy();
    if (data != null && diskCacheStrategy.isDataCacheable(loadData.fetcher.getDataSource())) {
      dataToCache = data;.
      cb.reschedule();
    } else {
      /没有缓存  走这里   
      //这里的cb是上面private DataFetcherGenerator getNextGenerator()中初始化SourceGenerator时传入的DecodeJob
      cb.onDataFetcherReady(
          loadData.sourceKey,
          data,
          loadData.fetcher,
          loadData.fetcher.getDataSource(),
          originalKey);
    }
  }
```
DecodeJob.java
```
public void onDataFetcherReady(
      Key sourceKey, Object data, DataFetcher<?> fetcher, DataSource dataSource, Key attemptedKey) {
    this.currentSourceKey = sourceKey;
    this.currentData = data;
    this.currentFetcher = fetcher;
    this.currentDataSource = dataSource;
    this.currentAttemptingKey = attemptedKey;
    this.isLoadingFromAlternateCacheKey = sourceKey != decodeHelper.getCacheKeys().get(0);

    if (Thread.currentThread() != currentThread) {
      runReason = RunReason.DECODE_DATA;
      callback.reschedule(this);
    } else {
      GlideTrace.beginSection("DecodeJob.decodeFromRetrievedData");
      try {
        //上面的都不看了,会走到这里来
        decodeFromRetrievedData();
      } finally {
        GlideTrace.endSection();
      }
    }
  }
  
private void decodeFromRetrievedData() {
    if (Log.isLoggable(TAG, Log.VERBOSE)) {
      logWithTimeAndKey(
          "Retrieved data",
          startFetchTime,
          "data: "
              + currentData
              + ", cache key: "
              + currentSourceKey
              + ", fetcher: "
              + currentFetcher);
    }
    Resource<R> resource = null;
    try {
      //重点1
      //从数据中解码得到资源
      resource = decodeFromData(currentFetcher, currentData, currentDataSource);
    } catch (GlideException e) {
      e.setLoggingDetails(currentAttemptingKey, currentDataSource);
      throwables.add(e);
    }
    if (resource != null) {
      //重点2
      notifyEncodeAndRelease(resource, currentDataSource, isLoadingFromAlternateCacheKey);
    } else {
      runGenerators();
    }
  }  
```
拿到数据之后,经过了几个方法,到了重点1处,开始从数据中解码.
```
  private <Data> Resource<R> decodeFromData(
      DataFetcher<?> fetcher, Data data, DataSource dataSource) throws GlideException {
    try {
      if (data == null) {
        return null;
      }
      long startTime = LogTime.getLogTime();
            //这里又把解码包了一下
      Resource<R> result = decodeFromFetcher(data, dataSource);
      if (Log.isLoggable(TAG, Log.VERBOSE)) {
        logWithTimeAndKey("Decoded result " + result, startTime);
      }
      return result;
    } finally {
      fetcher.cleanup();
    }
  }
  
  private <Data> Resource<R> decodeFromFetcher(Data data, DataSource dataSource)
      throws GlideException {
    LoadPath<Data, ?, R> path = decodeHelper.getLoadPath((Class<Data>) data.getClass());
    //重点代码
    //将解码交给LoadPath对象去搞
    return runLoadPath(data, dataSource, path);
  }  
```
//todo 解耦方法
```
 private <Data, ResourceType> Resource<R> runLoadPath(
      Data data, DataSource dataSource, LoadPath<Data, ResourceType, R> path)
      throws GlideException {
    Options options = getOptionsWithHardwareConfig(dataSource);
    //将数据又包装了一下,,,,,,好多好多包装啊
    DataRewinder<Data> rewinder = glideContext.getRegistry().getRewinder(data);
    try {
      // ResourceType in DecodeCallback below is required for compilation to work with gradle.
      //在LoadPath对象中load  解码
      return path.load(
          rewinder, options, width, height, new DecodeCallback<ResourceType>(dataSource));
    } finally {
      rewinder.cleanup();
    }
  }
```
各种包装,各种转,最后将数据交给LoadPath对象对象去解码.

LoadPath.java
```
  public Resource<Transcode> load(
      DataRewinder<Data> rewinder,
      @NonNull Options options,
      int width,
      int height,
      DecodePath.DecodeCallback<ResourceType> decodeCallback)
      throws GlideException {
    List<Throwable> throwables = Preconditions.checkNotNull(listPool.acquire());
    try {
    //核心代码
      return loadWithExceptionList(rewinder, options, width, height, decodeCallback, throwables);
    } finally {
      listPool.release(throwables);
    }
  }
  
private Resource<Transcode> loadWithExceptionList(
      DataRewinder<Data> rewinder,
      @NonNull Options options,
      int width,
      int height,
      DecodePath.DecodeCallback<ResourceType> decodeCallback,
      List<Throwable> exceptions)
      throws GlideException {
    Resource<Transcode> result = null;
    //noinspection ForLoopReplaceableByForEach to improve perf
    for (int i = 0, size = decodePaths.size(); i < size; i++) {
      DecodePath<Data, ResourceType, Transcode> path = decodePaths.get(i);
      try {
         //核心代码   
        //将解码任务又转给了DecodePath去做  我擦,,,,转了好几手了
        result = path.decode(rewinder, width, height, options, decodeCallback);
      } catch (GlideException e) {
        exceptions.add(e);
      }
      if (result != null) {
        break;
      }
    }

    if (result == null) {
      throw new GlideException(failureMessage, new ArrayList<>(exceptions));
    }

    return result;
  }  
```
又转了一手,到DecodePath手中去解码
DecodePath.java
```
  public Resource<Transcode> decode(
      DataRewinder<DataType> rewinder,
      int width,
      int height,
      @NonNull Options options,
      DecodeCallback<ResourceType> callback)
      throws GlideException {
    Resource<ResourceType> decoded = decodeResource(rewinder, width, height, options);
    Resource<ResourceType> transformed = callback.onResourceDecoded(decoded);
    return transcoder.transcode(transformed, options);
  }

private Resource<ResourceType> decodeResource(
      DataRewinder<DataType> rewinder, int width, int height, @NonNull Options options)
      throws GlideException {
    List<Throwable> exceptions = Preconditions.checkNotNull(listPool.acquire());
    try {
      return decodeResourceWithList(rewinder, width, height, options, exceptions);
    } finally {
      listPool.release(exceptions);
    }
  }

  @NonNull
  private Resource<ResourceType> decodeResourceWithList(
      DataRewinder<DataType> rewinder,
      int width,
      int height,
      @NonNull Options options,
      List<Throwable> exceptions)
      throws GlideException {
    Resource<ResourceType> result = null;
    //noinspection ForLoopReplaceableByForEach to improve perf
    //遍历解码器
    for (int i = 0, size = decoders.size(); i < size; i++) {
      ResourceDecoder<DataType, ResourceType> decoder = decoders.get(i);
      try {
        DataType data = rewinder.rewindAndGet();
        //终于在这里开始解码了....
          //根据DataType, ResourceType区别,分发给不同的解码器
        if (decoder.handles(data, options)) {
          data = rewinder.rewindAndGet();
          result = decoder.decode(data, width, height, options);
        }
        // Some decoders throw unexpectedly. If they do, we shouldn't fail the entire load path, but
        // instead log and continue. See #2406 for an example.
      } catch (IOException | RuntimeException | OutOfMemoryError e) {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
          Log.v(TAG, "Failed to decode data for " + decoder, e);
        }
        exceptions.add(e);
      }

      if (result != null) {
        break;
      }
    }

    if (result == null) {
      throw new GlideException(failureMessage, new ArrayList<>(exceptions));
    }
    return result;
  }  
```
//TODO 解码器
终于在这里开始调用解码器开始解码了,,,,上面的decoder其实是StreamBitmapDecoder,在初始化Glide的时候就初始化好了....一直放那里没用它.
来看StreamBitmapDecoder的decode方法
StreamBitmapDecoder.java
```
public Resource<Bitmap> decode(
      @NonNull InputStream source, int width, int height, @NonNull Options options)
      throws IOException {

    // Use to fix the mark limit to avoid allocating buffers that fit entire images.
    final RecyclableBufferedInputStream bufferedStream;
    final boolean ownsBufferedStream;
    if (source instanceof RecyclableBufferedInputStream) {
      bufferedStream = (RecyclableBufferedInputStream) source;
      ownsBufferedStream = false;
    } else {
      bufferedStream = new RecyclableBufferedInputStream(source, byteArrayPool);
      ownsBufferedStream = true;
    }

    // Use to retrieve exceptions thrown while reading.
    // TODO(#126): when the framework no longer returns partially decoded Bitmaps or provides a
    // way to determine if a Bitmap is partially decoded, consider removing.
    ExceptionPassthroughInputStream exceptionStream =
        ExceptionPassthroughInputStream.obtain(bufferedStream);

    // Use to read data.
    // Ensures that we can always reset after reading an image header so that we can still
    // attempt to decode the full image even when the header decode fails and/or overflows our read
    // buffer. See #283.
    MarkEnforcingInputStream invalidatingStream = new MarkEnforcingInputStream(exceptionStream);
    UntrustedCallbacks callbacks = new UntrustedCallbacks(bufferedStream, exceptionStream);
    try {
      //这里又将解码任务交给了Downsampler
      return downsampler.decode(invalidatingStream, width, height, options, callbacks);
    } finally {
      exceptionStream.release();
      if (ownsBufferedStream) {
        bufferedStream.release();
      }
    }
  }
```
//Downsampler.java
```
public Resource<Bitmap> decode(
      InputStream is,
      int requestedWidth,
      int requestedHeight,
      Options options,
      DecodeCallbacks callbacks)
      throws IOException {
    return decode(
        new ImageReader.InputStreamImageReader(is, parsers, byteArrayPool),
        requestedWidth,
        requestedHeight,
        options,
        callbacks);
  }

  @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
  public Resource<Bitmap> decode(
      ParcelFileDescriptor parcelFileDescriptor, int outWidth, int outHeight, Options options)
      throws IOException {
    return decode(
        new ImageReader.ParcelFileDescriptorImageReader(
            parcelFileDescriptor, parsers, byteArrayPool),
        outWidth,
        outHeight,
        options,
        EMPTY_CALLBACKS);
  }

  private Resource<Bitmap> decode(
      ImageReader imageReader,
      int requestedWidth,
      int requestedHeight,
      Options options,
      DecodeCallbacks callbacks)
      throws IOException {
    byte[] bytesForOptions = byteArrayPool.get(ArrayPool.STANDARD_BUFFER_SIZE_BYTES, byte[].class);
    BitmapFactory.Options bitmapFactoryOptions = getDefaultOptions();
    bitmapFactoryOptions.inTempStorage = bytesForOptions;

    DecodeFormat decodeFormat = options.get(DECODE_FORMAT);
    PreferredColorSpace preferredColorSpace = options.get(PREFERRED_COLOR_SPACE);
    DownsampleStrategy downsampleStrategy = options.get(DownsampleStrategy.OPTION);
    boolean fixBitmapToRequestedDimensions = options.get(FIX_BITMAP_SIZE_TO_REQUESTED_DIMENSIONS);
    boolean isHardwareConfigAllowed =
        options.get(ALLOW_HARDWARE_CONFIG) != null && options.get(ALLOW_HARDWARE_CONFIG);

    try {
    //->  重点4
      Bitmap result =
          decodeFromWrappedStreams(
              imageReader,
              bitmapFactoryOptions,
              downsampleStrategy,
              decodeFormat,
              preferredColorSpace,
              isHardwareConfigAllowed,
              requestedWidth,
              requestedHeight,
              fixBitmapToRequestedDimensions,
              callbacks);
      // 解码得到Bitmap对象后，包装成BitmapResource对象返回，
      // 通过内部的get方法得到Bitmap对象        
      return BitmapResource.obtain(result, bitmapPool);
    } finally {
      releaseOptions(bitmapFactoryOptions);
      byteArrayPool.put(bytesForOptions);
    }
  }
```
解码任务又转手了,,,转到了Downsampler,
```
private Bitmap decodeFromWrappedStreams(
      ImageReader imageReader,
      BitmapFactory.Options options,
      DownsampleStrategy downsampleStrategy,
      DecodeFormat decodeFormat,
      PreferredColorSpace preferredColorSpace,
      boolean isHardwareConfigAllowed,
      int requestedWidth,
      int requestedHeight,
      boolean fixBitmapToRequestedDimensions,
      DecodeCallbacks callbacks)
      throws IOException {
    long startTime = LogTime.getLogTime();

    int[] sourceDimensions = getDimensions(imageReader, options, callbacks, bitmapPool);
    int sourceWidth = sourceDimensions[0];
    int sourceHeight = sourceDimensions[1];
    String sourceMimeType = options.outMimeType;

    // If we failed to obtain the image dimensions, we may end up with an incorrectly sized Bitmap,
    // so we want to use a mutable Bitmap type. One way this can happen is if the image header is so
    // large (10mb+) that our attempt to use inJustDecodeBounds fails and we're forced to decode the
    // full size image.
    if (sourceWidth == -1 || sourceHeight == -1) {
      isHardwareConfigAllowed = false;
    }

    int orientation = imageReader.getImageOrientation();
    int degreesToRotate = TransformationUtils.getExifOrientationDegrees(orientation);
    boolean isExifOrientationRequired = TransformationUtils.isExifOrientationRequired(orientation);

    int targetWidth =
        requestedWidth == Target.SIZE_ORIGINAL
            ? (isRotationRequired(degreesToRotate) ? sourceHeight : sourceWidth)
            : requestedWidth;
    int targetHeight =
        requestedHeight == Target.SIZE_ORIGINAL
            ? (isRotationRequired(degreesToRotate) ? sourceWidth : sourceHeight)
            : requestedHeight;

    ImageType imageType = imageReader.getImageType();
    //Bitmap压缩的代码
    calculateScaling(
        imageType,
        imageReader,
        callbacks,
        bitmapPool,
        downsampleStrategy,
        degreesToRotate,
        sourceWidth,
        sourceHeight,
        targetWidth,
        targetHeight,
        options);
    calculateConfig(
        imageReader,
        decodeFormat,
        isHardwareConfigAllowed,
        isExifOrientationRequired,
        options,
        targetWidth,
        targetHeight);

    boolean isKitKatOrGreater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    // Prior to KitKat, the inBitmap size must exactly match the size of the bitmap we're decoding.
    if ((options.inSampleSize == 1 || isKitKatOrGreater) && shouldUsePool(imageType)) {
      int expectedWidth;
      int expectedHeight;
      if (sourceWidth >= 0
          && sourceHeight >= 0
          && fixBitmapToRequestedDimensions
          && isKitKatOrGreater) {
        expectedWidth = targetWidth;
        expectedHeight = targetHeight;
      } else {
        float densityMultiplier =
            isScaling(options) ? (float) options.inTargetDensity / options.inDensity : 1f;
        int sampleSize = options.inSampleSize;
        int downsampledWidth = (int) Math.ceil(sourceWidth / (float) sampleSize);
        int downsampledHeight = (int) Math.ceil(sourceHeight / (float) sampleSize);
        expectedWidth = Math.round(downsampledWidth * densityMultiplier);
        expectedHeight = Math.round(downsampledHeight * densityMultiplier);

        if (Log.isLoggable(TAG, Log.VERBOSE)) {
         ...
        }
      }
      // If this isn't an image, or BitmapFactory was unable to parse the size, width and height
      // will be -1 here.
      if (expectedWidth > 0 && expectedHeight > 0) {
        setInBitmap(options, bitmapPool, expectedWidth, expectedHeight);
      }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      boolean isP3Eligible =
          preferredColorSpace == PreferredColorSpace.DISPLAY_P3
              && options.outColorSpace != null
              && options.outColorSpace.isWideGamut();
      options.inPreferredColorSpace =
          ColorSpace.get(isP3Eligible ? ColorSpace.Named.DISPLAY_P3 : ColorSpace.Named.SRGB);
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      options.inPreferredColorSpace = ColorSpace.get(ColorSpace.Named.SRGB);
    }
    //通过BitmapFactory.decodeStream搞到Bitmap
    Bitmap downsampled = decodeStream(imageReader, options, callbacks, bitmapPool);
    callbacks.onDecodeComplete(bitmapPool, downsampled);

    //Bitmap旋转处理
    Bitmap rotated = null;
    if (downsampled != null) {
      // If we scaled, the Bitmap density will be our inTargetDensity. Here we correct it back to
      // the expected density dpi.
      downsampled.setDensity(displayMetrics.densityDpi);

      rotated = TransformationUtils.rotateImageExif(bitmapPool, downsampled, orientation);
      if (!downsampled.equals(rotated)) {
        bitmapPool.put(downsampled);
      }
    }

    return rotated;
  }

```
Glide在拿到InputStream之后各种包装,各种转手,终于还是压缩处理之后通过BitmapFactory.decodeStream生成出Bitmap.
最后这里是交给了Downsampler在处理,它的decode方法是返回到了DecodeJob的run方法，然后使用了notifyEncodeAndRelease()方法对Resource对象进行了回调.
DecodeJob.java
```
 private void notifyEncodeAndRelease(
      Resource<R> resource, DataSource dataSource, boolean isLoadedFromAlternateCacheKey) {
    if (resource instanceof Initializable) {
      ((Initializable) resource).initialize();
    }

    Resource<R> result = resource;
    LockedResource<R> lockedResource = null;
    if (deferredEncodeManager.hasResourceToEncode()) {
      lockedResource = LockedResource.obtain(resource);
      result = lockedResource;
    }

    notifyComplete(result, dataSource, isLoadedFromAlternateCacheKey);

    stage = Stage.ENCODE;
    try {
      if (deferredEncodeManager.hasResourceToEncode()) {
        deferredEncodeManager.encode(diskCacheProvider, options);
      }
    } finally {
      if (lockedResource != null) {
        lockedResource.unlock();
      }
    }
    // Call onEncodeComplete outside the finally block so that it's not called if the encode process
    // throws.
    onEncodeComplete();
  }
  
   private void notifyComplete(
      Resource<R> resource, DataSource dataSource, boolean isLoadedFromAlternateCacheKey) {
    setNotifiedOrThrow();
     //这里的callback其实是EngineJob,在初始化DecodeJob的时候传入的
    callback.onResourceReady(resource, dataSource, isLoadedFromAlternateCacheKey);
  }
```
开始通过DecodeJob进行回调,然后又传到了EngineJob
EngineJob.java
```
 public void onResourceReady(
      Resource<R> resource, DataSource dataSource, boolean isLoadedFromAlternateCacheKey) {
    synchronized (this) {
      this.resource = resource;
      this.dataSource = dataSource;
      this.isLoadedFromAlternateCacheKey = isLoadedFromAlternateCacheKey;
    }
    notifyCallbacksOfResult();
  }
 void notifyCallbacksOfResult() {
    ResourceCallbacksAndExecutors copy;
    Key localKey;
    EngineResource<?> localResource;
    synchronized (this) {
      stateVerifier.throwIfRecycled();
      if (isCancelled) {
        // TODO: Seems like we might as well put this in the memory cache instead of just recycling
        // it since we've gotten this far...
        resource.recycle();
        release();
        return;
      } else if (cbs.isEmpty()) {
        throw new IllegalStateException("Received a resource without any callbacks to notify");
      } else if (hasResource) {
        throw new IllegalStateException("Already have resource");
      }
      engineResource = engineResourceFactory.build(resource, isCacheable, key, resourceListener);     
      hasResource = true;
      copy = cbs.copy();
      incrementPendingCallbacks(copy.size() + 1);

      localKey = key;
      localResource = engineResource;
    }

    engineJobListener.onEngineJobComplete(this, localKey, localResource);
    //ResourceCallbackAndExecutor里面保存的是ResourceCallback和Executor
    //在loadStatus = engine.load中传入的是SingleRequest.java
    for (final ResourceCallbackAndExecutor entry : copy) {
      //通过回调CallResourceReady
      entry.executor.execute(new CallResourceReady(entry.cb));
    }
    decrementPendingCallbacks();
  }  
  
  private class CallResourceReady implements Runnable {
    @Override
    public void run() {
      // Make sure we always acquire the request lock, then the EngineJob lock to avoid deadlock
      // (b/136032534).
      synchronized (cb.getLock()) {
        synchronized (EngineJob.this) {
          if (cbs.contains(cb)) {
            // Acquire for this particular callback.
            engineResource.acquire();
            //进行cb的回调
            callCallbackOnResourceReady(cb);
            removeCallback(cb);
          }
          decrementPendingCallbacks();
        }
      }
    }
  }
  
  void callCallbackOnResourceReady(ResourceCallback cb) {
    try {
      cb.onResourceReady(engineResource, dataSource, isLoadedFromAlternateCacheKey);
    } catch (Throwable t) {
      throw new CallbackException(t);
    }
  }
```
onResourceReady回调传回来的时候,切换到主线程,然后通过SingleRequest的onResourceReady方法将数据传递回去
SingleRequest.java
```
public void onResourceReady(
      Resource<?> resource, DataSource dataSource, boolean isLoadedFromAlternateCacheKey) {
    stateVerifier.throwIfRecycled();
    Resource<?> toRelease = null;
    try {
      synchronized (requestLock) {
        loadStatus = null;
        if (resource == null) {
          GlideException exception =
              ....
          onLoadFailed(exception);
          return;
        }

        Object received = resource.get();
        if (received == null || !transcodeClass.isAssignableFrom(received.getClass())) {
          toRelease = resource;
          this.resource = null;
          GlideException exception =
              ...
          onLoadFailed(exception);
          return;
        }

        if (!canSetResource()) {
          toRelease = resource;
          this.resource = null;
          // We can't put the status to complete before asking canSetResource().
          status = Status.COMPLETE;
          return;
        }

        onResourceReady(
            (Resource<R>) resource, (R) received, dataSource, isLoadedFromAlternateCacheKey);
      }
    } finally {
      if (toRelease != null) {
        engine.release(toRelease);
      }
    }
  }
  
 private void onResourceReady(
      Resource<R> resource, R result, DataSource dataSource, boolean isAlternateCacheKey) {
    // We must call isFirstReadyResource before setting status.
    boolean isFirstResource = isFirstReadyResource();
    status = Status.COMPLETE;
    this.resource = resource;

    if (glideContext.getLogLevel() <= Log.DEBUG) {
     ....
    }

    isCallingCallbacks = true;
    try {
      boolean anyListenerHandledUpdatingTarget = false;
      if (requestListeners != null) {
        for (RequestListener<R> listener : requestListeners) {
          anyListenerHandledUpdatingTarget |=
              listener.onResourceReady(result, model, target, dataSource, isFirstResource);
        }
      }
      anyListenerHandledUpdatingTarget |=
          targetListener != null
              && targetListener.onResourceReady(result, model, target, dataSource, isFirstResource);

      if (!anyListenerHandledUpdatingTarget) {
        Transition<? super R> animation = animationFactory.build(dataSource, isFirstResource);
        //这里的target是初始化SingleRequest时传入的DrawableImageViewTarget
        target.onResourceReady(result, animation);
      }
    } finally {
      isCallingCallbacks = false;
    }

    notifyLoadSuccess();
  }  
```
在SingleRequest中做了一些判断,然后将数据传递给DrawableImageViewTarget去回调处理
ImageViewTarget.java
```
//下面2个方法是DrawableImageViewTarget的父类ImageViewTarget中的
 public void onResourceReady(@NonNull Z resource, @Nullable Transition<? super Z> transition) {
    if (transition == null || !transition.transition(resource, this)) {
      setResourceInternal(resource);
    } else {
      maybeUpdateAnimatable(resource);
    }
  }
 private void setResourceInternal(@Nullable Z resource) {
    // Order matters here. Set the resource first to make sure that the Drawable has a valid and
    // non-null Callback before starting it.
    setResource(resource);
    maybeUpdateAnimatable(resource);
  }
 
 //DrawableImageViewTarget.java
protected void setResource(@Nullable Drawable resource) {
    view.setImageDrawable(resource);
} 
```

这里的view就是我们在into时传入的ImageView对象....我擦,,,,终于把数据放到ImageView上了....不容易啊.

6. 总结
   到这里,Glide的从网络加载图片到ImageView上的这个主流程是走完了.真的是成吨成吨的操作,各种复杂逻辑,各种封装,各种转换....
   看代码看得欲仙欲死....大量逻辑集中在into里面,很容易被绕晕..当然,我只是了解其中的主流程,细节真的太多太多了.

ps: 太久没写博客了,,,,感觉写的不是很好,这写的是什么垃圾玩意儿....