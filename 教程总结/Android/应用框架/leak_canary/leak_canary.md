https://github.com/xfhy/Android-Notes/blob/master/Blogs/Android/%E4%B8%89%E6%96%B9%E5%BA%93%E5%8E%9F%E7%90%86/LeakCanary_%E5%8E%9F%E7%90%86%E6%8E%A2%E7%A9%B6.md
版本2.8.1


1. 背景
   Android开发中,内存泄露时常有发生在,有可能是你自己写的,也有可能是三方库里面的.程序中已动态分配的堆内存由于某种特殊原因程序未释放或无法释放,
   造成系统内存的浪费,导致程序运行速度减慢甚至程序崩溃等严重后果.本来Android内存就吃紧,还内存泄露的话,后果不堪设想.
   所以我们要尽量避免内存泄露,一方面我们要学习哪些常见场景下会发生内存泄露,一方面我们引入LeakCanary帮我们自动检测有内存泄露的地方.

LeakCanary是Square公司(对,又是这个公司,OkHttp和Retrofit等都是这家公司开源的)开源的一个库,通过它我们可以在App运行的过程中检测内存泄露,
它把对象内存泄露的引用链也给开发人员分析出来了,我们去修复这个内存泄露非常方面.

ps: LeakCanary直译过来是内存泄露的金丝雀,关于这个名字其实有一个小故事在里面.金丝雀,美丽的鸟儿.她的歌声不仅动听,还曾挽救过无数矿工的生命.
17世纪,英国矿井工人发现,金丝雀对瓦斯这种气体十分敏感.空气中哪怕有极其微量的瓦斯，金丝雀也会停止歌唱;而当瓦斯含量超过一定限度时,
虽然鲁钝的人类毫无察觉,金丝雀却早已毒发身亡.当时在采矿设备相对简陋的条件下，工人们每次下井都会带上一只金丝雀作为"瓦斯检测指标",
以便在危险状况下紧急撤离. 同样的,LeakCanary这只"金丝雀"能非常敏感地帮我们发现内存泄露,从而避免OOM的风险.

2 初始化
1.x
```
public class ExampleApplication extends Application {
  @Override public void onCreate() {
    super.onCreate();
    if (LeakCanary.isInAnalyzerProcess(this)) {
      // This process is dedicated to LeakCanary for heap analysis.
      // You should not init your app in this process.
      return;
    }
    LeakCanary.install(this);
    // Normal app init code...
  }
}
```
2.x 不需要初始化代码，在ContentProvider中初始化
leakcanary-object-watcher-android/src/main/AndroidManifest.xml
```
  <provider
        android:name="leakcanary.internal.MainProcessAppWatcherInstaller"
        android:authorities="${applicationId}.leakcanary-installer"
        android:enabled="@bool/leak_canary_watcher_auto_install"
        android:exported="false"/>
```
```
internal class MainProcessAppWatcherInstaller : ContentProvider() {
  override fun onCreate(): Boolean {
    val application = context!!.applicationContext as Application
    AppWatcher.manualInstall(application)
    return true
  }
}
```
需要注意的是ContentProvider的onCreate执行时机比Application的onCreate执行时机还早.
如果你想在其他时机进行初始化优化启动时间,也是可以的.只需要在app里重写@bool/leak_canary_watcher_auto_install的值为false即可.
 然后手动在合适的地方调用AppWatcher.manualInstall(application).但是LeakCanary本来就是在debug的时候用的,
  所以感觉优化启动时间不是那么必要.

集成后可以查看日志，如果LeakCanary分析的引用链比较简单，还是无法定位内存泄漏的位置的话，则可以把LeakCanary保存的hprof文件导出来，
通过mat工具详细的分析内存泄漏

AppWatcher.kt
```
//retainedDelayMillis的默认为5秒
fun manualInstall(
    application: Application,
    retainedDelayMillis: Long = TimeUnit.SECONDS.toMillis(5),
    watchersToInstall: List<InstallableWatcher> = appDefaultWatchers(application)
  ) {
    ...
    //获取接口函数InternalLeakCanary并执行
    LeakCanaryDelegate.loadLeakCanary(application)
    //初始化InstallableWatcher
    watchersToInstall.forEach {
      it.install()
    }
  }
  
  fun appDefaultWatchers(
    application: Application,
    reachabilityWatcher: ReachabilityWatcher = objectWatcher
  ): List<InstallableWatcher> {
    //初始化四个Watcher，每个都传入了reachabilityWatcher，也就是objectWatcher
    return listOf(
      ActivityWatcher(application, reachabilityWatcher),
      FragmentAndViewModelWatcher(application, reachabilityWatcher),
      RootViewWatcher(reachabilityWatcher),
      ServiceWatcher(reachabilityWatcher)
    )
  }  
```
ActivityWatcher,FragmentAndViewModelWatcher,ServiceWatcher都实现了InstallableWatcher
InstallableWatcher.kt
```
interface InstallableWatcher {
  fun install()
  fun uninstall()
}
```
可以看出，初始化时即安装了一些Watcher，即在默认情况下，我们只会观察Activity,Fragment,RootView,Service这些对象是否泄漏
如果需要观察其他对象，需要手动添加并处理，手动执行manualInstall并传入InstallableWatcher即可

看一下LeakCanaryDelegate.kt
```
val loadLeakCanary by lazy {
    try {
      val leakCanaryListener = Class.forName("leakcanary.internal.InternalLeakCanary")
      //获取InternalLeakCanary并转为函数，上面调用时执行了
      leakCanaryListener.getDeclaredField("INSTANCE")
        .get(null) as (Application) -> Unit
    } catch (ignored: Throwable) {
      NoLeakCanary
    }
  }
```
InternalLeakCanary.kt
```
override fun invoke(application: Application) {
    _application = application
    //创建ObjectWatcher并添加监听
    AppWatcher.objectWatcher.addOnObjectRetainedListener(this)
    //触发GC
    val gcTrigger = GcTrigger.Default

    val configProvider = { LeakCanary.config }
    //子线程的handlder LEAK_CANARY_THREAD_NAME = "LeakCanary-Heap-Dump"
    val handlerThread = HandlerThread(LEAK_CANARY_THREAD_NAME)
    handlerThread.start()
    val backgroundHandler = Handler(handlerThread.looper)

    heapDumpTrigger = HeapDumpTrigger(
      application, backgroundHandler, AppWatcher.objectWatcher, gcTrigger,
      configProvider
    )
    //添加前后台切换的监听
    application.registerVisibilityListener { applicationVisible ->
      this.applicationVisible = applicationVisible
      heapDumpTrigger.onApplicationVisibilityChanged(applicationVisible)
    }
    registerResumedActivityListener(application)
    //添加桌面图标
    addDynamicShortcut(application)
    ...
  }
```

GC触发方式
```
fun interface GcTrigger {

  fun runGc()

  object Default : GcTrigger {
    override fun runGc() {
      //代码摘自AOSP FinalizationTest
      Runtime.getRuntime()
        .gc()
      enqueueReferences()
      System.runFinalization()
    }

    private fun enqueueReferences() {
      // Hack. We don't have a programmatic way to wait for the reference queue daemon to move
      // references to the appropriate queues.
      try {
        Thread.sleep(100)
      } catch (e: InterruptedException) {
        throw AssertionError()
      }
    }
  }
}
```
//todo 显示调用gc，会对性能造成影响，查看虚拟机的实现
看一下ObjectWatcher的初始化
```
  val objectWatcher = ObjectWatcher(
    clock = { SystemClock.uptimeMillis() },
    //传入的线程池
    checkRetainedExecutor = {
      check(isInstalled) {
        "AppWatcher not installed"
      }
      //retainedDelayMillis的设置为5秒  延迟5秒执行
      mainHandler.postDelayed(it, retainedDelayMillis)
    },
    isEnabled = { true }
  )
```


3. 监听泄露的时机
   LeakCanary自动检测以下对象的泄露:
destroyed Activity instances
destroyed Fragment instances
destroyed fragment View instances
cleared ViewModel instances
RootView的监测，例如tooltip,toast 从window中移除view会触发
可以看到,检测的都是些Android开发中容易被泄露的东西.那么它是如何检测的,下面我们来分析一下
Activity检测   
   ActivityWatcher.kt   
```
class ActivityWatcher(
  private val application: Application,
  private val reachabilityWatcher: ReachabilityWatcher
) : InstallableWatcher {

  private val lifecycleCallbacks =
    object : Application.ActivityLifecycleCallbacks by noOpDelegate() {
      override fun onActivityDestroyed(activity: Activity) {
        //通过reachabilityWatcher检测Activity销毁后是否发生泄露
        //reachabilityWatcher也就是ObjectWatcher
        reachabilityWatcher.expectWeaklyReachable(
          activity, "${activity::class.java.name} received Activity#onDestroy() callback"
        )
      }
    }

  override fun install() {
    //注册activity生命周期回调
    application.registerActivityLifecycleCallbacks(lifecycleCallbacks)
  }
}
```   
这里只列出activity的，其他的在最后

4. 监测对象是否泄露
   在讲这个之前得先回顾一个知识点,Java中的WeakReference是弱引用类型,每当发生GC时,它所持有的对象如果没有被其他强引用所持有,
   那么它所引用的对象就会被回收,同时或者稍后的时间这个WeakReference会被入队到ReferenceQueue中.LeakCanary中检测内存泄露就是基于这个原理.

实现要点:
1 当一个对象需要被回收时,生成一个唯一的key,将它们封装进KeyedWeakReference中,并传入自定义的ReferenceQueue
2 将key和KeyedWeakReference放入一个map中
3 过一会儿之后(默认是5秒)，从ReferenceQueue查询已经回收的对象并从map中移除，如果当前的对象activity或者其他没有进入ReferenceQueue，
  检查是否内存泄露
4 主动触发GC,重复3中的移除对象回收步骤
  此时如果map中还有KeyedWeakReference剩余,那么就是没有入队的,也就是说这些KeyedWeakReference所对应的对象还没被回收.
  这是不合理的,这里就产生了内存泄露.
5 将这些内存泄露的对象分析引用链,保存数据
ObjectWatcher.kt
```
 @Synchronized override fun expectWeaklyReachable(
    watchedObject: Any,
    description: String
  ) {
    if (!isEnabled()) {
      return
    }
    //移除引用队列中的所有KeyedWeakReference,同时也将其从map中移除
    removeWeaklyReachableObjects()
    //生成key
    val key = UUID.randomUUID().toString()
    val watchUptimeMillis = clock.uptimeMillis()
    //将watchedObject包装为KeyedWeakReference
    val reference =
      KeyedWeakReference(watchedObject, key, description, watchUptimeMillis, queue)
    ...
    //保存到map    private val watchedObjects = mutableMapOf<String, KeyedWeakReference>()
    watchedObjects[key] = reference
    //5秒后观察对象是否正常回收
    checkRetainedExecutor.execute {
      moveToRetained(key)
    }
  }
  
  @Synchronized private fun moveToRetained(key: String) {
    //先移除回收的对象
    removeWeaklyReachableObjects()
    val retainedRef = watchedObjects[key]
    //如果map的key,value仍然存在，对象没有被回收，可能泄露了
    if (retainedRef != null) {
      retainedRef.retainedUptimeMillis = clock.uptimeMillis()
      onObjectRetainedListeners.forEach { it.onObjectRetained() }
    }
  }
  
  private fun removeWeaklyReachableObjects() {
    var ref: KeyedWeakReference?
    do {
      ref = queue.poll() as KeyedWeakReference?
      if (ref != null) {
        //将进入ReferenceQueue的对象从watchedObjects移除
        watchedObjects.remove(ref.key)
      }
    } while (ref != null)
  }  
  
  //移除回收的，记录未被回收的
  val retainedObjectCount: Int
    @Synchronized get() {
      removeWeaklyReachableObjects()
      return watchedObjects.count { it.value.retainedUptimeMillis != -1L }
    } 
```

KeyedWeakReference继承WeakReference
```
class KeyedWeakReference(
  referent: Any,
  val key: String,
  val description: String,
  val watchUptimeMillis: Long,
  referenceQueue: ReferenceQueue<Any>
) : WeakReference<Any>(
  referent, referenceQueue
) {
  //在ObjectWatcher.moveToRetained()时更新时间，当对象被移除时清空
  @Volatile  
  var retainedUptimeMillis = -1L

  override fun clear() {
    super.clear()
    retainedUptimeMillis = -1L
  }

  companion object {
    @Volatile
    @JvmStatic var heapDumpUptimeMillis = 0L
  }
}
```

检查是否内存泄露
InternalLeakCanary.kt
```
  override fun onObjectRetained() = scheduleRetainedObjectCheck()

  fun scheduleRetainedObjectCheck() {
    if (this::heapDumpTrigger.isInitialized) {
      heapDumpTrigger.scheduleRetainedObjectCheck()
    }
  }
```
HeapDumpTrigger.kt
```
  fun scheduleRetainedObjectCheck(
    delayMillis: Long = 0L
  ) {
    val checkCurrentlyScheduledAt = checkScheduledAt
    if (checkCurrentlyScheduledAt > 0) {
      return
    }
    checkScheduledAt = SystemClock.uptimeMillis() + delayMillis
    //通过子线程handler执行 “LeakCanary-Heap-Dump”
    backgroundHandler.postDelayed({
      checkScheduledAt = 0
      checkRetainedObjects()
    }, delayMillis)
  }
  
private fun checkRetainedObjects() {
    ...
    var retainedReferenceCount = objectWatcher.retainedObjectCount
    //未被回收的对象大于0
    if (retainedReferenceCount > 0) {
      //手动触发GC,这里触发GC时还延迟了100ms,给那些回收了的对象入引用队列一点时间,好让结果更准确.
      gcTrigger.runGc()
      retainedReferenceCount = objectWatcher.retainedObjectCount
    }

    if (checkRetainedCount(retainedReferenceCount, config.retainedVisibleThreshold)) return

    val now = SystemClock.uptimeMillis()
    val elapsedSinceLastDumpMillis = now - lastHeapDumpUptimeMillis
    //WAIT_BETWEEN_HEAP_DUMPS_MILLIS = 60_000L
    if (elapsedSinceLastDumpMillis < WAIT_BETWEEN_HEAP_DUMPS_MILLIS) {
     //1分钟之内才dump过,再过会儿再来
      onRetainInstanceListener.onEvent(DumpHappenedRecently)
      //提示通知
      showRetainedCountNotification(
        objectCount = retainedReferenceCount,
        contentText = application.getString(R.string.leak_canary_notification_retained_dump_wait)
      )
      //再次检查
      scheduleRetainedObjectCheck(
        delayMillis = WAIT_BETWEEN_HEAP_DUMPS_MILLIS - elapsedSinceLastDumpMillis
      )
      return
    }

    dismissRetainedCountNotification()
    val visibility = if (applicationVisible) "visible" else "not visible"
    //开始dump
    dumpHeap(
      retainedReferenceCount = retainedReferenceCount,
      retry = true,
      reason = "$retainedReferenceCount retained objects, app is $visibility"
    )
  }  
  
private fun checkRetainedCount(
    retainedKeysCount: Int,
    retainedVisibleThreshold: Int,
    nopeReason: String? = null
  ): Boolean {
    val countChanged = lastDisplayedRetainedObjectCount != retainedKeysCount
    lastDisplayedRetainedObjectCount = retainedKeysCount
    if (retainedKeysCount == 0) {
      if (countChanged) {
        //未被回收的对象数是0,展示无泄漏的通知
        SharkLog.d { "All retained objects have been garbage collected" }
        onRetainInstanceListener.onEvent(NoMoreObjects)
        showNoMoreRetainedObjectNotification()
      }
      return true
    }

    val applicationVisible = applicationVisible
    val applicationInvisibleLessThanWatchPeriod = applicationInvisibleLessThanWatchPeriod
    ...
     //retainedVisibleThreshold默认是5
    if (retainedKeysCount < retainedVisibleThreshold) {
      //如果app在前台 或者不可见少于5秒
      if (applicationVisible || applicationInvisibleLessThanWatchPeriod) {
        if (countChanged) {
          onRetainInstanceListener.onEvent(BelowThreshold(retainedKeysCount))
        }
        showRetainedCountNotification(
          objectCount = retainedKeysCount,
          contentText = application.getString(
            R.string.leak_canary_notification_retained_visible, retainedVisibleThreshold
          )
        )
        //WAIT_FOR_OBJECT_THRESHOLD_MILLIS = 2_000L
        //2秒后再次检查
        scheduleRetainedObjectCheck(
          delayMillis = WAIT_FOR_OBJECT_THRESHOLD_MILLIS
        )
        return true
      }
    }
    return false
  }  
```
看一下Dump的过程
```
private fun dumpHeap(
    retainedReferenceCount: Int,
    retry: Boolean,
    reason: String
  ) {
    val directoryProvider =
      InternalLeakCanary.createLeakDirectoryProvider(InternalLeakCanary.application)
    val heapDumpFile = directoryProvider.newHeapDumpFile()

    val durationMillis: Long
    try {
      InternalLeakCanary.sendEvent(DumpingHeap(currentEventUniqueId))
      if (heapDumpFile == null) {
        throw RuntimeException("Could not create heap dump file")
      }
      saveResourceIdNamesToMemory()
      val heapDumpUptimeMillis = SystemClock.uptimeMillis()
      KeyedWeakReference.heapDumpUptimeMillis = heapDumpUptimeMillis
      durationMillis = measureDurationMillis {
        //通过Debug.dumpHprofData(filePath)  dump堆文件
        configProvider().heapDumper.dumpHeap(heapDumpFile)
      }
      if (heapDumpFile.length() == 0L) {
        throw RuntimeException("Dumped heap file is 0 byte length")
      }
      lastDisplayedRetainedObjectCount = 0
      lastHeapDumpUptimeMillis = SystemClock.uptimeMillis()
      //清除一下这次dump开始之前的所有引用
      objectWatcher.clearObjectsWatchedBefore(heapDumpUptimeMillis)
      currentEventUniqueId = UUID.randomUUID().toString()
      //开始解析dumpFile
      InternalLeakCanary.sendEvent(HeapDump(currentEventUniqueId, heapDumpFile, durationMillis, reason))
    } catch (throwable: Throwable) {
      ...
    }
  }
```
AndroidDebugHeapDumper.kt
```
object AndroidDebugHeapDumper : HeapDumper {
  override fun dumpHeap(heapDumpFile: File) {
    Debug.dumpHprofData(heapDumpFile.absolutePath)
  }
}
```

开始解析dumpFile
InternalLeakCanary.kt
```
 fun sendEvent(event: Event) {
    for(listener in LeakCanary.config.eventListeners) {
      listener.onEvent(event)
    }
  }
```
eventListeners的初始化
LeakCanary.kt
```
//对于Event注册不同的监听，Event的子类包括DumpingHeap，HeapDumpFailed，HeapAnalysisProgress，HeapAnalysisDone，
  //HeapAnalysisSucceeded,HeapAnalysisFailed
val eventListeners: List<EventListener> = listOf(
      LogcatEventListener,
      ToastEventListener,
      LazyForwardingEventListener {
        if (InternalLeakCanary.formFactor == TV) TvEventListener else NotificationEventListener
      },
      when {
          RemoteWorkManagerHeapAnalyzer.remoteLeakCanaryServiceInClasspath ->
            RemoteWorkManagerHeapAnalyzer
          WorkManagerHeapAnalyzer.workManagerInClasspath -> WorkManagerHeapAnalyzer
          else -> BackgroundThreadHeapAnalyzer
      }
    ),
```
listener的顺序是RemoteWorkManagerHeapAnalyzer，WorkManagerHeapAnalyzer，BackgroundThreadHeapAnalyzer
前两个使用WorkManager做任务管理，第一个是跨进程的
RemoteWorkManagerHeapAnalyzer.kt
```
  private const val REMOTE_SERVICE_CLASS_NAME = "leakcanary.internal.RemoteLeakCanaryWorkerService"
  //校验RemoteLeakCanaryWorkerService是否存在
  internal val remoteLeakCanaryServiceInClasspath by lazy {
    try {
      Class.forName(REMOTE_SERVICE_CLASS_NAME)
      true
    } catch (ignored: Throwable) {
      false
    }
  }
```
看一下BackgroundThreadHeapAnalyzer的实现
```
object BackgroundThreadHeapAnalyzer : EventListener {
  //子线程HeapAnalyzer
  internal val heapAnalyzerThreadHandler by lazy {
    val handlerThread = HandlerThread("HeapAnalyzer")
    handlerThread.start()
    Handler(handlerThread.looper)
  }

  override fun onEvent(event: Event) {
    if (event is HeapDump) {
      heapAnalyzerThreadHandler.post {
        //使用AndroidDebugHeapAnalyzer分析
        val doneEvent = AndroidDebugHeapAnalyzer.runAnalysisBlocking(event) { event ->
          InternalLeakCanary.sendEvent(event)
        }
        //解析完成发送doneEvent
        InternalLeakCanary.sendEvent(doneEvent)
      }
    }
  }
}
```
AndroidDebugHeapAnalyzer.kt
```
fun runAnalysisBlocking(
    heapDumped: HeapDump,
    isCanceled: () -> Boolean = { false },
    progressEventListener: (HeapAnalysisProgress) -> Unit
  ): HeapAnalysisDone<*> {
    val progressListener = OnAnalysisProgressListener { step ->
      val percent = (step.ordinal * 1.0) / OnAnalysisProgressListener.Step.values().size
      progressEventListener(HeapAnalysisProgress(heapDumped.uniqueId, step, percent))
    }

    val heapDumpFile = heapDumped.file
    val heapDumpDurationMillis = heapDumped.durationMillis
    val heapDumpReason = heapDumped.reason

    val heapAnalysis = if (heapDumpFile.exists()) {
      analyzeHeap(heapDumpFile, progressListener, isCanceled)
    } else {
      missingFileFailure(heapDumpFile)
    }

    val fullHeapAnalysis = when (heapAnalysis) {
      is HeapAnalysisSuccess -> heapAnalysis.copy(
        dumpDurationMillis = heapDumpDurationMillis,
        metadata = heapAnalysis.metadata + ("Heap dump reason" to heapDumpReason)
      )
      is HeapAnalysisFailure -> {
        val failureCause = heapAnalysis.exception.cause!!
        if (failureCause is OutOfMemoryError) {
          heapAnalysis.copy(
            dumpDurationMillis = heapDumpDurationMillis,
            exception = HeapAnalysisException(
             ....
            )
          )
        } else {
          heapAnalysis.copy(dumpDurationMillis = heapDumpDurationMillis)
        }
      }
    }
    progressListener.onAnalysisProgress(REPORTING_HEAP_ANALYSIS)

    val db = LeaksDbHelper(application).writableDatabase
    val id = HeapAnalysisTable.insert(db, heapAnalysis)

    val analysisDoneEvent = when (fullHeapAnalysis) {
      is HeapAnalysisSuccess -> {
        val showIntent = LeakActivity.createSuccessIntent(application, id)
        val leakSignatures = fullHeapAnalysis.allLeaks.map { it.signature }.toSet()
        val leakSignatureStatuses = LeakTable.retrieveLeakReadStatuses(db, leakSignatures)
        val unreadLeakSignatures = leakSignatureStatuses.filter { (_, read) ->
          !read
        }.keys
          // keys returns LinkedHashMap$LinkedKeySet which isn't Serializable
          .toSet()
        HeapAnalysisSucceeded(
          heapDumped.uniqueId,
          fullHeapAnalysis,
          unreadLeakSignatures,
          showIntent
        )
      }
      is HeapAnalysisFailure -> {
        val showIntent = LeakActivity.createFailureIntent(application, id)
        HeapAnalysisFailed(heapDumped.uniqueId, fullHeapAnalysis, showIntent)
      }
    }
    // Can't leverage .use{} because close() was added in API 16 and we're min SDK 14.
    db.releaseReference()
    LeakCanary.config.onHeapAnalyzedListener.onHeapAnalyzed(fullHeapAnalysis)
    return analysisDoneEvent
  }
 
 //使用shark的 HeapAnalyzer解析Hprof文件
private fun analyzeHeap(
    heapDumpFile: File,
    progressListener: OnAnalysisProgressListener,
    isCanceled: () -> Boolean
  ): HeapAnalysis {
    val config = LeakCanary.config
    val heapAnalyzer = HeapAnalyzer(progressListener)
    val proguardMappingReader = try {
      ProguardMappingReader(application.assets.open(PROGUARD_MAPPING_FILE_NAME))
    } catch (e: IOException) {
      null
    }

    progressListener.onAnalysisProgress(PARSING_HEAP_DUMP)

    val sourceProvider =
      ConstantMemoryMetricsDualSourceProvider(ThrowingCancelableFileSourceProvider(heapDumpFile) {
        if (isCanceled()) {
          throw RuntimeException("Analysis canceled")
        }
      })

    val closeableGraph = try {
      sourceProvider.openHeapGraph(proguardMapping = proguardMappingReader?.readProguardMapping())
    } catch (throwable: Throwable) {
      return HeapAnalysisFailure(
        heapDumpFile = heapDumpFile,
        createdAtTimeMillis = System.currentTimeMillis(),
        analysisDurationMillis = 0,
        exception = HeapAnalysisException(throwable)
      )
    }
    return closeableGraph
      .use { graph ->
        val result = heapAnalyzer.analyze(
          heapDumpFile = heapDumpFile,
          graph = graph,
          leakingObjectFinder = config.leakingObjectFinder,
          referenceMatchers = config.referenceMatchers,
          computeRetainedHeapSize = config.computeRetainedHeapSize,
          objectInspectors = config.objectInspectors,
          metadataExtractor = config.metadataExtractor
        )
        if (result is HeapAnalysisSuccess) {
          val lruCacheStats = (graph as HprofHeapGraph).lruCacheStats()
          val randomAccessStats =
            "RandomAccess[" +
              "bytes=${sourceProvider.randomAccessByteReads}," +
              "reads=${sourceProvider.randomAccessReadCount}," +
              "travel=${sourceProvider.randomAccessByteTravel}," +
              "range=${sourceProvider.byteTravelRange}," +
              "size=${heapDumpFile.length()}" +
              "]"
          val stats = "$lruCacheStats $randomAccessStats"
          result.copy(metadata = result.metadata + ("Stats" to stats))
        } else result
      }
  }  
```
解析完成会有通知提示
NotificationEventListener.kt
```
verride fun onEvent(event: Event) {
    // TODO Unify Notifications.buildNotification vs Notifications.showNotification
    // We need to bring in the retained count notifications first though.
    if (!Notifications.canShowNotification) {
      return
    }
    when (event) {
      is DumpingHeap -> {
        ...
        notificationManager.notify(R.id.leak_canary_notification_dumping_heap, notification)
      }
      is HeapDumpFailed, is HeapDump -> {
        notificationManager.cancel(R.id.leak_canary_notification_dumping_heap)
      }
      is HeapAnalysisProgress -> {
        ...
        notificationManager.notify(R.id.leak_canary_notification_analyzing_heap, notification)
      }
      is HeapAnalysisDone<*> -> {
        notificationManager.cancel(R.id.leak_canary_notification_analyzing_heap)
        ...
        showHeapAnalysisResultNotification(contentTitle,pendingIntent)
      }
    }
  }
```



补充：
Fragment，Fragment View，ViewModel检测
FragmentAndViewModelWatcher.kt
```
override fun install() {
    application.registerActivityLifecycleCallbacks(lifecycleCallbacks)
  }
  private val lifecycleCallbacks =
    object : Application.ActivityLifecycleCallbacks by noOpDelegate() {
      override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?
      ) {
        //activity创建后，将activity添加到watcher,这里的wather也是接口函数
        for (watcher in fragmentDestroyWatchers) {
          watcher(activity)
        }
      }
    }  
    
 private val fragmentDestroyWatchers: List<(Activity) -> Unit> = run {
    val fragmentDestroyWatchers = mutableListOf<(Activity) -> Unit>()

    if (SDK_INT >= O) {
      //android O以上 添加AndroidOFragmentDestroyWatcher
      fragmentDestroyWatchers.add(
        AndroidOFragmentDestroyWatcher(reachabilityWatcher)
      )
    }
    //通过反射获取watcher
    //ANDROIDX_FRAGMENT_CLASS_NAME = "androidx.fragment.app.Fragment"
    //ANDROIDX_FRAGMENT_DESTROY_WATCHER_CLASS_NAME = "leakcanary.internal.AndroidXFragmentDestroyWatcher"
    getWatcherIfAvailable(
      ANDROIDX_FRAGMENT_CLASS_NAME,
      ANDROIDX_FRAGMENT_DESTROY_WATCHER_CLASS_NAME,
      reachabilityWatcher
    )?.let {
      fragmentDestroyWatchers.add(it)
    }
    //ANDROID_SUPPORT_FRAGMENT_CLASS_NAME = "android.support.v4.app.Fragment"
    //ANDROID_SUPPORT_FRAGMENT_DESTROY_WATCHER_CLASS_NAME = "leakcanary.internal.AndroidSupportFragmentDestroyWatcher"
    getWatcherIfAvailable(
      ANDROID_SUPPORT_FRAGMENT_CLASS_NAME,
      ANDROID_SUPPORT_FRAGMENT_DESTROY_WATCHER_CLASS_NAME,
      reachabilityWatcher
    )?.let {
      fragmentDestroyWatchers.add(it)
    }
    fragmentDestroyWatchers
  }  
  
private fun getWatcherIfAvailable(
    fragmentClassName: String,
    watcherClassName: String,
    reachabilityWatcher: ReachabilityWatcher
  ): ((Activity) -> Unit)? {

    return if (classAvailable(fragmentClassName) &&
      classAvailable(watcherClassName)
    ) {
      val watcherConstructor =
        Class.forName(watcherClassName).getDeclaredConstructor(ReachabilityWatcher::class.java)
      //又转为接口函数
      watcherConstructor.newInstance(reachabilityWatcher) as (Activity) -> Unit
    } else {
      null
    }
  }   
```
AndroidSupportFragmentDestroyWatcher.kt使用support包android.support.v4.app.FragmentManager
AndroidOFragmentDestroyWatcher.kt使用SDK包android.app.FragmentManager
AndroidXFragmentDestroyWatcher.kt使用androidx.fragment，并额外检测了监听了ViewModel
AndroidXFragmentDestroyWatcher.kt
```
internal class AndroidXFragmentDestroyWatcher(
  private val reachabilityWatcher: ReachabilityWatcher
) : (Activity) -> Unit {

  private val fragmentLifecycleCallbacks = object : FragmentManager.FragmentLifecycleCallbacks() {

    override fun onFragmentCreated(
      fm: FragmentManager,
      fragment: Fragment,
      savedInstanceState: Bundle?
    ) {
      ViewModelClearedWatcher.install(fragment, reachabilityWatcher)
    }

    override fun onFragmentViewDestroyed(
      fm: FragmentManager,
      fragment: Fragment
    ) {
      val view = fragment.view
      if (view != null) {
        reachabilityWatcher.expectWeaklyReachable(
          view, "${fragment::class.java.name} received Fragment#onDestroyView() callback " +
          "(references to its views should be cleared to prevent leaks)"
        )
      }
    }

    override fun onFragmentDestroyed(
      fm: FragmentManager,
      fragment: Fragment
    ) {
      reachabilityWatcher.expectWeaklyReachable(
        fragment, "${fragment::class.java.name} received Fragment#onDestroy() callback"
      )
    }
  }

  override fun invoke(activity: Activity) {
    if (activity is FragmentActivity) {
      //注册fragment的声明周期
      val supportFragmentManager = activity.supportFragmentManager
      supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentLifecycleCallbacks, true)
      //将activity添加进ViewModelClearedWatcher
      ViewModelClearedWatcher.install(activity, reachabilityWatcher)
    }
  }
}
```
在onFragmentDestroyed或者onFragmentViewDestroyed时调用reachabilityWatcher检测fragment或fragment.view内存情况

ViewModel的检测通过实现ViewModel的onCleared，然后调用reachabilityWatcher来检查
```
internal class ViewModelClearedWatcher(
  storeOwner: ViewModelStoreOwner
  private val reachabilityWatcher: ReachabilityWatcher
) : ViewModel() {
  //实现ViewModel的onCleared方法，
  override fun onCleared() {
    viewModelMap?.values?.forEach { viewModel ->
      reachabilityWatcher.expectWeaklyReachable(
        viewModel, "${viewModel::class.java.name} received ViewModel#onCleared() callback"
      )
    }
  }
  
 private val viewModelMap: Map<String, ViewModel>? = try {
    //通过反射拿到该fragment的所有ViewModel   storeOwner就是这个fragment或activity
    val mMapField = ViewModelStore::class.java.getDeclaredField("mMap")
    mMapField.isAccessible = true
    mMapField[storeOwner.viewModelStore] as Map<String, ViewModel>
  } catch (ignored: Exception) {
    null
  }
  
  companion object {
    fun install(
      storeOwner: ViewModelStoreOwner,
      reachabilityWatcher: ReachabilityWatcher
    ) {
     //新的ViewModel，注意storeOwner是外部传来的fragment还有activity
      val provider = ViewModelProvider(storeOwner, object : Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
          ViewModelClearedWatcher(storeOwner, reachabilityWatcher) as T
      })
      //provider.get()在没有对应ViewModel的情况下会使用Factory新建一个，然后放到ViewModelStore
      provider.get(ViewModelClearedWatcher::class.java)
    }
  }
}
```

 RootViewWatcher.kt  监测window中view的泄漏情况
main/java/leakcanary/RootViewWatcher.kt
```
private val listener = OnRootViewAddedListener { rootView ->
    //需要监听的view有tooltip,toast等
    val trackDetached = when(rootView.windowType) {
      PHONE_WINDOW -> {
        when (rootView.phoneWindow?.callback?.wrappedCallback) {
          is Activity -> false
          is Dialog -> {
            //根据配置
            val resources = rootView.context.applicationContext.resources
            resources.getBoolean(R.bool.leak_canary_watcher_watch_dismissed_dialogs)
          }
          else -> true
        }
      }
      POPUP_WINDOW -> false
      TOOLTIP, TOAST, UNKNOWN -> true
    }
    if (trackDetached) {
      rootView.addOnAttachStateChangeListener(object : OnAttachStateChangeListener {
        val watchDetachedView = Runnable {
          //执行对象监测
          reachabilityWatcher.expectWeaklyReachable(
            rootView, "${rootView::class.java.name} received View#onDetachedFromWindow() callback"
          )
        }
        override fun onViewAttachedToWindow(v: View) {
          mainHandler.removeCallbacks(watchDetachedView)
        }
        //监听view从window中移除
        override fun onViewDetachedFromWindow(v: View) {
          mainHandler.post(watchDetachedView)
        }
      })
    }
  }
```
todo OnRootViewAddedListener的添加在哪

//todo 反射+动态代理
service的检测ServiceWatcher.kt
```
  //获取ActivityThreadServices的mServices
  private val activityThreadServices by lazy {
    val mServicesField =
      activityThreadClass.getDeclaredField("mServices").apply { isAccessible = true }

    mServicesField[activityThreadInstance] as Map<IBinder, Service>
  }
  
override fun install() {
    ...
    try {
      //hook  ActivityThread.Handler拿到service的消息
      swapActivityThreadHandlerCallback { mCallback ->
        uninstallActivityThreadHandlerCallback = {
          swapActivityThreadHandlerCallback {
            mCallback
          }
        }
        //返回Handler.Callback
        Handler.Callback { msg ->
          // https://github.com/square/leakcanary/issues/2114
          // On some Motorola devices (Moto E5 and G6), the msg.obj returns an ActivityClientRecord
          // instead of an IBinder. This crashes on a ClassCastException. Adding a type check
          // here to prevent the crash.
          if (msg.obj !is IBinder) {
            return@Callback false
          }
          //处理STOP_SERVICE的消息
          if (msg.what == STOP_SERVICE) {
            val key = msg.obj as IBinder
            //ActivityThread对应的mServices里面key的值不为null
            activityThreadServices[key]?.let {
              //调用onServicePreDestroy
              // 记录service到servicesToBeDestroyed这个map中
              onServicePreDestroy(key, it)
            }
          }
          mCallback?.handleMessage(msg) ?: false
        }
      }
      swapActivityManager { activityManagerInterface, activityManagerInstance ->
        uninstallActivityManager = {
          swapActivityManager { _, _ ->
            activityManagerInstance
          }
        }
        Proxy.newProxyInstance(
          activityManagerInterface.classLoader, arrayOf(activityManagerInterface)
        ) { _, method, args ->
          //动态代理方法serviceDoneExecuting
          //METHOD_SERVICE_DONE_EXECUTING = "serviceDoneExecuting"
          if (METHOD_SERVICE_DONE_EXECUTING == method.name) {
            val token = args!![0] as IBinder
            //servicesToBeDestroyed是一个WeakHashMap<IBinder, WeakReference<Service>>()
            if (servicesToBeDestroyed.containsKey(token)) {
              //调用onServiceDestoryed,将service从servicesToBeDestroyed中移除
              onServiceDestroyed(token)
            }
          }
          try {
            //调用代理方法
            if (args == null) {
              method.invoke(activityManagerInstance)
            } else {
              method.invoke(activityManagerInstance, *args)
            }
          } catch (invocationException: InvocationTargetException) {
            throw invocationException.targetException
          }
        }
      }
    } catch (ignored: Throwable) {
      SharkLog.d(ignored) { "Could not watch destroyed services" }
    }
  }
  
  private val activityThreadClass by lazy { Class.forName("android.app.ActivityThread") }
   
  private fun swapActivityThreadHandlerCallback(swap: (Handler.Callback?) -> Handler.Callback?) {
    val mHField =
      activityThreadClass.getDeclaredField("mH").apply { isAccessible = true }
    //拿到android.app.ActivityThread的mH  
    val mH = mHField[activityThreadInstance] as Handler

    //替换mH的mCallback
    val mCallbackField =
      Handler::class.java.getDeclaredField("mCallback").apply { isAccessible = true }
    val mCallback = mCallbackField[mH] as Handler.Callback?
    mCallbackField[mH] = swap(mCallback)
  } 
  
  private fun swapActivityManager(swap: (Class<*>, Any) -> Any) {
    val singletonClass = Class.forName("android.util.Singleton")
    val mInstanceField =
      singletonClass.getDeclaredField("mInstance").apply { isAccessible = true }

    val singletonGetMethod = singletonClass.getDeclaredMethod("get")
    
    //获取ActivityManager或ActivityManagerNative
    val (className, fieldName) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      "android.app.ActivityManager" to "IActivityManagerSingleton"
    } else {
      "android.app.ActivityManagerNative" to "gDefault"
    }

    val activityManagerClass = Class.forName(className)
    val activityManagerSingletonField =
      activityManagerClass.getDeclaredField(fieldName).apply { isAccessible = true }
    val activityManagerSingletonInstance = activityManagerSingletonField[activityManagerClass]

    // Calling get() instead of reading from the field directly to ensure the singleton is
    // created.
    val activityManagerInstance = singletonGetMethod.invoke(activityManagerSingletonInstance)

    val iActivityManagerInterface = Class.forName("android.app.IActivityManager")
    //swap的参数是IActivityManager，ActivityManager.IActivityManagerSingleton/ActivityManagerNative.gDefault
    mInstanceField[activityManagerSingletonInstance] =
      swap(iActivityManagerInterface, activityManagerInstance!!)
  }
  
  private fun onServicePreDestroy(
    token: IBinder,
    service: Service
  ) {
    servicesToBeDestroyed[token] = WeakReference(service)
  }

  private fun onServiceDestroyed(token: IBinder) {
    servicesToBeDestroyed.remove(token)?.also { serviceWeakReference ->
      //拿到未被回收的service
      serviceWeakReference.get()?.let { service ->
        //检测这个service是否发生内存泄露
        reachabilityWatcher.expectWeaklyReachable(
          service, "${service::class.java.name} received Service#onDestroy() callback"
        )
      }
    }
  }
```

跨进程初始化
注意,AppWatcherInstaller有两个子类,MainProcess与LeakCanaryProcess
其中默认使用MainProcess,会在App进程初始化
有时我们考虑到LeakCanary比较耗内存，需要在独立进程初始化
使用leakcanary-android-process模块的时候，会在一个新的进程中去开启LeakCanary
```
    <service
      android:name="leakcanary.internal.RemoteLeakCanaryWorkerService"
      android:exported="false"
      android:process=":leakcanary" />
```
RemoteLeakCanaryWorkerService应用在RemoteWorkManagerHeapAnalyzer


参考资料
https://zhuanlan.zhihu.com/p/346338268#:~:text=http%3A//hg.openjdk.java.net/jdk6/jdk6/jdk/raw-file/tip/src/share/demo/jvmti/hprof/manual.html%23mozTocId848088

//todo hprof文件解析
https://zhuanlan.zhihu.com/p/360944586

leak_canary缺点
用于线下，dump堆栈信息耗时并且APP会冻结，生成的hprof文件大
查看Koom的优化  解决dump耗时，fork主进程，Linux采用copy-on-write来fork子线程
https://github.com/KwaiAppTeam/KOOM
https://mp.weixin.qq.com/s/WqYS_RQeE-PeDPPyOhvgsw

https://www.jianshu.com/p/e5c2fe8c8c8a
1.每次内存泄漏以后，都会生成一个.hprof文件，然后解析，并将结果写入.hprof.result。增加手机负担，引起手机卡顿等问题。
2.多次调用GC，可能会对线上性能产生影响
3.同样的泄漏问题，会重复生成 .hprof 文件，重复分析并写入磁盘。
4..hprof文件较大，信息回捞成问题。

了解了这些问题，我们可以尝试提出一些解决方案：
1.可以根据手机信息来设定一个内存阈值 M ，当已使用内存小于 M时，如果此时有内存泄漏，只将泄漏对象的信息放入内存当中保存，
  不生成.hprof文件。当已使用大于 M 时，生成.hprof文件
2.当引用链路相同时，可根据实际情况去重。
3.不直接回捞.hprof文件，可以选择回捞分析的结果
4.可以尝试将已泄漏对象存储在数据库中，一个用户同一个泄漏只检测一次，减少对用户的影响



debug.md也有
https://juejin.cn/post/6867335105322188813
Debug.dumpHprofData() 最终会通过 JNI 调用到 native 方法：
```
// art/runtime/native/dalvik_system_VMDebug.cc
static void VMDebug_dumpHprofData(JNIEnv* env, jclass, jstring javaFilename, jint javaFd) {
  // Only one of these may be null.
  // 忽略一些判断代码
  hprof::DumpHeap(filename.c_str(), fd, false);
}

// art/runtime/hprof/hprof.cc
void DumpHeap(const char* filename, int fd, bool direct_to_ddms) {
  // 忽略一些判断代码
  ScopedSuspendAll ssa(__FUNCTION__, true /* long suspend */);
  Hprof hprof(filename, fd, direct_to_ddms);
  // 开始执行 Dump 操作
  hprof.Dump();
}
```
从源码中，我们可以看到在进行 Dump 操作之前，会构造一个 ScopedSuspendAll 对象，用来暂停所有的线程，然后再析构方法中恢复：
```
// 暂停所有线程
ScopedSuspendAll::ScopedSuspendAll(const char* cause, bool long_suspend) {
  Runtime::Current()->GetThreadList()->SuspendAll(cause, long_suspend);
}

// 恢复所有线程
ScopedSuspendAll::~ScopedSuspendAll() {
  Runtime::Current()->GetThreadList()->ResumeAll();
}
```