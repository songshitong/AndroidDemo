package com.sst.libkotlin.coroutines

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.util.concurrent.Executors

/**
 * @description:
 * @author: songshitong
 * @date: 2024/2/5
 */
class MJob {
  companion object{
    private val singleThreadScope = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    @JvmStatic
    fun main(args: Array<String>) {
      //结构化并发https://en.wikipedia.org/wiki/Structured_concurrency
      //The core concept is the encapsulation of concurrent threads of execution by way of control flow constructs
      // that have clear entry and exit points and that ensure all spawned threads have completed before exit.

      //https://juejin.cn/post/7042549383006519303
       //1和2 测试不同时调用
      // testJob1()
      // testJob2()

      //分析
      //testJob1 构成的job树结构分析
      //  job1和job2是new job的子节点，与job0没有关系  代码中将job0取消了，但是job1和job2正常运行
      //testJob2构成的job树结构分析
      //  job1和job2都是job0的子节点  代码中将job0取消了，所以job2没有打印

      //Job和SupervisorJob  https://juejin.cn/post/7046191497628090399
      //Job的子协程发生异常被取消(手动调用cancel结果相同)会同时取消Job的其它子协程，而SupervisorJob不会。
      // testJobException()
      testSupervisorException()
      testInvokeOnCompletion()

      //job.cancel只能作用于挂起的协程/suspend 对于Thread.sleep不生效
      //可以使用yield   ensureActive检查job状态，在cancel后会抛出异常CancellationException
      testCancel()
    }

    private fun testCancel() {
      val job1 = CoroutineScope(Dispatchers.IO).launch {
        Thread.sleep(2000)
        yield()
        ensureActive()
        println("Cancel job1 end")
      }
      CoroutineScope(singleThreadScope).launch{
        delay(1000)
        job1.cancel()
        println("Cancel job2 end")
      }
    }

    private fun testInvokeOnCompletion() {
      //invokeOnCompletion方法
      //该方法的作用是给Job注册一个回调，当Job执行完成后执行回调函数
      CoroutineScope(singleThreadScope).launch {
        println("start scope")
      }.invokeOnCompletion {
        println("launch complete")
      }
    }

    private fun testJobException() {
      val errorHandler = CoroutineExceptionHandler { _, throwable ->
        println("error:${throwable}")
      }
      val scope = CoroutineScope(singleThreadScope+ Job()+errorHandler) //todo context的作用，将不同类型整合
      scope.launch {
        println("job1 start")
        delay(2000)
        println("job1 end")
      }
      scope.launch {
        println("job2 start")
        delay(1000)
        1/0 //发生错误时，将job1,job2都取消了
        println("job2 end")
      }
      //结果打印：
      // job1 start
      // job2 start
      // error:java.lang.ArithmeticException: / by zero
    }

    private fun testSupervisorException() {
      val errorHandler = CoroutineExceptionHandler { _, throwable ->
        println("error:${throwable}")
      }
      val scope = CoroutineScope(singleThreadScope+ SupervisorJob() +errorHandler)
      scope.launch {
        println("job1 start")
        delay(2000)
        println("job1 end")
      }
      scope.launch {
        println("job2 start")
        delay(1000)
        1/0 //发生错误时，job1执行完成，只有job2取消了
        println("job2 end")
      }
      //结果打印：
      //job1 start
      //job2 start
      //error:java.lang.ArithmeticException: / by zero
      //job1 end
    }

    private fun testJob1() {
      val scope = CoroutineScope(singleThreadScope) //job0
      scope.launch(Job()){//new job
        launch {
          delay(2000)
          println("this is job1")  //job1和job2都打印
          scope.cancel()
        }
        launch {
          delay(3000)
          println("this is job2")
        }
      }
    }

    private fun testJob2() {
      val scope = CoroutineScope(singleThreadScope)//job0
      scope.launch{
        launch {
          delay(2000)
          println("this is job1")  //只有job1打印
          scope.cancel()
        }
        launch {
          delay(3000)
          println("this is job2")
        }
      }
    }
  }
}