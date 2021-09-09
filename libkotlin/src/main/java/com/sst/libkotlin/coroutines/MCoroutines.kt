import kotlinx.coroutines.*
import java.io.PipedInputStream
import java.io.PipedOutputStream
import javax.naming.Context


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
//Jetpack 集成：许多 Jetpack 库都包含提供全面协程支持的扩展。某些库还提供自己的协程作用域，可供您用于结构化并发。

public class MCoroutines{
  public  companion object {
        private const val TAG:String = "MCoroutines ===="

        /** 我是main入口函数 **/
        @JvmStatic
       public suspend fun main(args: Array<String>) {
            // 线程阻塞，适用于单元测试的场景
            runBlocking {  }
            // 不会阻塞线程，但在 Android 中不推荐，因为它的生命周期会和 app 一致
            GlobalScope.launch {  }

            // 推荐使用，通过 CoroutineContext 参数去管理和控制协程的生命周期
// 例如：context = Dispatchers.Default + EmptyCoroutineContext
            val coroutineScope = CoroutineScope(Dispatchers.Default)
            coroutineScope.launch { }

            // async启动的Job是Deferred类型，它可以有返回结果，通过await方法获取
            val id = coroutineScope.async {  }
            id.await()


            ///切换线程
//            主线程 => IO线程 => 主线程。这种场景开发过程中使用最多，比如后台获取一张照片，然后前台显示。
            // 主线程内启动一个协程
            //ide 表示的箭头是suspend function call  挂起函数调用
            GlobalScope.launch(Dispatchers.Main) {
                // 切换到IO线程
                withContext(Dispatchers.IO) {
                    delay(1000)
                    print("$TAG processIO in ${Thread.currentThread().name}")
                }
                // 自动切回主线程
                print("$TAG processUI in ${Thread.currentThread().name}")
            }


        }
    }
}

