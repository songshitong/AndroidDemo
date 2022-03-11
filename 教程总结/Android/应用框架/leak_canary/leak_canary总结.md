
Java中的WeakReference是弱引用类型,每当发生GC时,它所持有的对象如果没有被其他强引用所持有,
那么它所引用的对象就会被回收,同时或者稍后的时间这个WeakReference会被入队到ReferenceQueue中

检查原理
1 当一个对象需要被回收时,生成一个唯一的key,将它们封装进KeyedWeakReference中,并传入自定义的ReferenceQueue，保存在Map中
```
val key = UUID.randomUUID().toString()
```
2 将key和KeyedWeakReference放入一个map中
```
 private val watchedObjects = mutableMapOf<String, KeyedWeakReference>()
```
3 过一会儿之后(默认是5秒)，从ReferenceQueue出队已经回收的对象并从map中移除，如果当前的对象activity或者其他没有进入ReferenceQueue，
   检查是否内存泄露
4 主动触发GC,重复3中的移除对象回收步骤
   此时如果map中还有KeyedWeakReference剩余,那么就是没有入队的,也就是说这些KeyedWeakReference所对应的对象还没被回收.
   这是不合理的,这里就产生了内存泄露.
5 将这些内存泄露的对象分析引用链,保存数据



可以监控的对象有四类 
Activity，Fragment，ViewModel，RootView，Service
对应的watcher是ActivityWatcher
  注册Application.ActivityLifecycleCallbacks回调，监听onActivityDestroyed后进行检查
FragmentAndViewModelWatcher
   1. 注册FragmentManager.registerFragmentLifecycleCallbacks监听，
      在onFragmentDestroyed或者onFragmentViewDestroyed时，对fragment或fragment.view进行检测
   2. 将ViewModelClearedWatcher添加到Fragment的ViewModelStore，然后在其onCleared后进行检测
RootViewWatcher
ServiceWatcher
 通过hook  ActivityThread.Handler拿到service的消息，在serviceDoneExecuting后进行检查


GC触发方式 
Runtime.gc
System.runFinalization
```
fun interface GcTrigger {

  fun runGc()

  object Default : GcTrigger {
    override fun runGc() {
      //代码摘自AOSP FinalizationTest
      Runtime.getRuntime().gc()
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