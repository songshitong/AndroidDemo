import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.Result.Companion
import kotlin.coroutines.suspendCoroutine
import kotlin.jvm.Throws
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

//https://juejin.cn/post/6913429237147893774
//何为Kotlin协程？
//协程是一种并发设计模式，Kotlin协程是一个线程框架。
//为什么需要Kotlin协程？
//提供方便的线程操作API，编写逻辑清晰且简洁的线程代码。
//协程是Google在 Android 上进行异步编程的推荐解决方案。具有如下特点：
//
//轻量：您可以在单个线程上运行多个协程，因为协程支持挂起，不会使正在运行协程的线程阻塞。挂起比阻塞节省内存，且支持多个并行操作。
//内存泄漏更少：使用结构化并发机制在一个作用域内执行多项操作。
//内置取消支持：取消操作会自动在运行中的整个协程层次结构内传播。
//Jetpack 集成：许多 Jetpack 库都包含提供全面协程支持的扩展。某些库还提供自己的协程作用域，可供您用于结构化并发。 todo 扩展作用域

//Coroutines   [kəru:'ti:n]  协程；协同例程；共行程序
public class MCoroutines{
  public  companion object {
        private const val TAG:String = "MCoroutines ===="

      @JvmStatic
       fun main(args: Array<String>) {
//          testLaunch()
//          testLaunch1()

//          testRunBlocking()

          testScope()

//          testMultiCoroutines()

//            testSuspend()

//             testCoroutineScope()
//          testAsync()

//            testMultiAsync()

//            testWithContext()
          //项目常见例子
//          testCase()

          //suspendCoroutine  todo  CPS https://www.jianshu.com/p/06703abc56b1

          testCustomThread()

          //等待其他协程的完成
          testJoinAll()

          //mutex
          //Mutex的实现基于挂起函数和协程的概念。当一个协程请求进入受Mutex保护的临界区时，如果Mutex已经被占用，请求的协程将被挂起，
         // 直到Mutex可用。这样可以避免多个协程同时访问共享资源，确保线程安全。
          // https://kotlinlang.org/docs/shared-mutable-state-and-concurrency.html#mutual-exclusion
          //https://mp.weixin.qq.com/s/fQoBmVYZHKqHPNikPGEEzw
          val mutex = Mutex()
          var count = 0
          runBlocking {
              withContext(Dispatchers.IO){
                  repeat(10000){
                      launch {
                          mutex.withLock {
                            count++
                          }
                      }
                  }
              }
              println("test mutex: count is $count") //使用mutex保护，结果是10000  不使用mutex保护，结果一般小于10000
          }

          //Semaphore
          //信号量来保证协程的安全
          var count1 =0
          val semaphore = Semaphore(1,0) //总的信号量(至少有1个)和已经使用的信号量
          CoroutineScope(Dispatchers.Default).launch {
              List(10000){//创建10000个协程
                  launch {
                      semaphore.withPermit { //对semaphore的封装，里面有acquire()，release()两个方法
                          count1++
                      }
                  }
              }.joinAll() //等待
              println("Semaphore count1 $count1")
          }

          //suspendCancellableCoroutine
          //https://juejin.cn/post/7047635719065436167
          runBlocking {
              println("before")
              // susFun() //写入susFun后，after不会执行  即协程挂起了并且没有回复
              susDelayFun() //1秒后执行after，类似delay函数的实现
              println("after")
          }
      }

      suspend fun susFun(){
         //suspendCoroutine与suspendCancellableCoroutine类似，后者支持cancel
         //用于实现一个挂起函数
         suspendCancellableCoroutine<Unit> {

         }
      }

      suspend fun susDelayFun(){
         suspendCancellableCoroutine<String> {
             Thread.sleep(1000)
             //手动恢复协程，并返回String值
             it.resume("success")  //需要import kotlin.coroutines.resume
             // it.resumeWith(Result.success("2233"))
             // it.resumeWith(Result.success(Unit)) 返回Unit类型
             // it.resumeWithException(Exception("error")) //返回异常类型
         }
      }

      private fun testJoinAll() {
          runBlocking {
              val job1 = GlobalScope.launch {
                  delay(1000)
                  println("job1")
              }
              val job2 = GlobalScope.launch {
                  delay(500)
                  println("job2")
              }
              joinAll(job1,job2) //等待两个job的完成
              println("job1和job2都已经完成")
          }

      }

      private fun testCustomThread() {
          //在单一的子线程运行
          val thread = Executors.newFixedThreadPool(1).asCoroutineDispatcher()
          runBlocking {
              withContext(thread){
                  println("testCustomThread 111")
              }
              withContext(thread){
                  println("testCustomThread 222")
              }
          }
      }

      private fun testWithContext() {
          val a=null
          println(a ?: "aa")
          //withContext可以创建子协程，并将最后一行作为返回值
          runBlocking {
              val result = withContext(Dispatchers.Default) {
                  5 + 5
              }
              println(result)
              //结果10
          }
      }

      private fun testMultiAsync() {
          // async 函数先后调用时是一种串行关系，可能多次阻塞线程，每次用到结果时再去await()可以减少阻塞时间
          runBlocking {
              val start = System.currentTimeMillis()
              val deferred1 = async {
                  delay(1000)
                  5 + 5
              }
              val result1 = deferred1.await()


              val deferred2 = async {
                  delay(1000)
                  4 + 6
              }
              val result2 = deferred2.await()
//              println("result is ${deferred1.await() + deferred2.await()}")
              println("result is ${result1 + result2}")
              val end = System.currentTimeMillis()
              println("cost: ${end - start} ms.")
              //当两个await() 先后调用时，结果是
//              result is 20
//              cost: 2022 ms.

              //当 两个await()一起调用时，结果是
//              result is 20
//              cost: 1024 ms.
          }
      }
      private fun testAsync() {
          val coroutineScope = CoroutineScope(Dispatchers.Default)
          coroutineScope.launch {
              println("there is launch")
          }

          // async启动的Job是Deferred类型，它可以有返回结果，通过await方法获取  需要在suspend函数或协程中使用
          val result = coroutineScope.async {
              delay(1000)
              return@async "aaa"
          }
          runBlocking {
              //await() 可能会阻塞线程
              println("协程结果: ${result.await()}")
          }
          //结果
//          there is launch
//          协程结果: aaa
      }

      private fun testCoroutineScope(){
          runBlocking {
              //自定义的coroutineScope，可以在里面使用launch函数
              realCoroutineScope()
          }
      }

      private suspend fun realCoroutineScope()= coroutineScope {
          println("there is a coroutine scope")
          delay(1000)
          //launch只能运行在协程作用域
          launch {
              println()
          }
      }


      private fun testSuspend() {
          runBlocking {
              //当函数不在协程作用域声明，使用suspend声明为挂起函数，但是该函数没有协程作用域
              realSuspend()
          }
      }

      private suspend fun realSuspend(){
          delay(1000)
          println("after delay")
      }

      //      https://juejin.cn/post/6963190541471186957#heading-12
      private fun testMultiCoroutines() {
          //在当前的协程作用域下创建子协程  创建多个协程同时运行
          //两个子协程的日志是交替打印的，实现类似多线程并发的效果，实际上都运行在main线程
          runBlocking {
              launch {
                  println("launch1 ${Thread.currentThread().name}")
                  delay(1000)
                  println("launch1 finished")
              }

              launch {
                  println("launch2 ${Thread.currentThread().name}")
                  delay(1000)
                  println("launch2 finished")
              }
          }
          //结果
//          launch1 main
//          launch2 main
//          launch1 finished
//          launch2 finished
      }

      private fun testCase() {
          ///切换线程
//            主线程 => IO线程 => 主线程。这种场景开发过程中使用最多，比如后台获取一张照片，然后前台显示。
          // 主线程内启动一个协程
          //ide-AndroidStudio 表示的箭头是suspend function call  挂起函数调用

          //当前环境没有初始化Android，没有handler.prepare，会报错
          CoroutineScope(Dispatchers.Main).launch {
              // 切换到IO线程
              withContext(Dispatchers.IO) {
                  delay(1000)
                  println("$TAG processIO in ${Thread.currentThread().name}")
              }
              // 自动切回主线程
              println("$TAG processUI in ${Thread.currentThread().name}")
              //运行结果
//              MCoroutines ==== processIO in DefaultDispatcher-worker-1
//              MCoroutines ==== processUI in main
          }
          Thread.sleep(2000)
      }

      private fun testScope() {
          // 推荐使用，通过 CoroutineContext 参数去管理和控制协程的生命周期
          // 例如：context = Dispatchers.Default + EmptyCoroutineContext
          val coroutineScope = CoroutineScope(Dispatchers.Default)
          coroutineScope.launch {
              println("thread name ${Thread.currentThread().name}")
          }
          Thread.sleep(100)
      }

      private fun testRunBlocking() {
          // 线程阻塞，适用于单元测试的场景
          runBlocking {
              delay(1000)
              println("this isBlocking")
          }
      }

      private fun testLaunch1() {
          GlobalScope.launch {
              println("codes run in coroutine scope")
              delay(1500)
              println("codes run in coroutine scope finished")
          }
          Thread.sleep(1000)
          //结果：  codes run in coroutine scope
          // 主线程却只阻塞了 1 秒，协程挂起1.5秒，还没来得及运行，程序就结束了
          //todo 这是怎么回事  和delay有关系吗
      }

      private fun testLaunch() {
          // 不会阻塞线程，但在 Android 中不推荐，因为它的生命周期会和 app 一致
          GlobalScope.launch {
              println("this is coroutine")
          }
          Thread.sleep(100)//确保上面协程运行，否则没有打印   todo为什么加这个，下面的直接起一个线程是可以的，协程的源码
          Thread {
              println("start")
          }.start()
      }
  }
}

