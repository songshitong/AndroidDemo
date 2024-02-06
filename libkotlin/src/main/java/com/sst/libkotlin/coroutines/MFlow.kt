package com.sst.libkotlin.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeoutOrNull

class MFlow {

  //flow基于协程的函数式编程
  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      //flow构建flow{}  flowOf(1,2,3)(发射固定值)  (1..3).asFlow()(将集合或序列转换为流)
      runBlocking {
        simpleFlow()
        arrayOf(1, 2, 3).asFlow()
      }
      //流的取消
      runBlocking {
//                try {
        getFlow().collect {
//                        if (it == 3) cancel()  //输出1 2 3
          // 抛出异常Exception in thread "main" kotlinx.coroutines.JobCancellationException: BlockingCoroutine was cancelled; job=BlockingCoroutine{Cancelled}@5fdba6f9
//                        println("test cancel $it")
        }
//                } catch (e: Exception) {
//
//                }

      }
      //超时取消
      runBlocking {
        withTimeoutOrNull(250) { // Timeout after 250ms
          getFlow().collect { value -> println("withTimeoutOrNull $value") }//输出1，2
        }
      }
      //冷流和热流
      //默认构建的flow是冷流，默认不执行 每次collect都执行一次，类似控制水龙头的开关
      //   冷流中的代码直到流被收集的时候才会执行。
      //   热流中的代码会立即执行，不管有没有被收集
      runBlocking {
        getFlow().collect { value -> println("start collect1 $value") }
        getFlow().collect { value -> println("start collect2 $value") }
      }
      //冷流接口
      // public interface Flow<out T> {
      //   public suspend fun collect(collector: FlowCollector<T>)
      // }
      // public fun interface FlowCollector<in T> {
      //   public suspend fun emit(value: T)
      // }
      //创建一个flow
      // public fun <T> flow(@BuilderInference block: suspend FlowCollector<T>.() -> Unit): Flow<T> = SafeFlow(block)

      //冷流使用场景  配合替换Rxjava，某些固定的流程，结果不需要更新
      // GlobalScope.launch(Dispatchers.Main) {
      //   flowOf(bitmap).map { bmp ->
      //     //在子线程中执行耗时操作，存储 bitmap 到本地
      //     saveBitmap(bmp)
      //   }.flowOn(Dispatchers.IO).collect { bitmapLocalPath ->
      //     //在主线程中处理存储 bitmap 后的本地路地址
      //   }
      // }


      //https://juejin.cn/post/7169843775240405022
      // StateFlow 和 SharedFlow 分别是用来处理 状态(state) 和 事件(event) 的，它称呼 StateFlow 是 a state-holder observable：
      // val uiState: StateFlow<LatestNewsUiState>
      // val tickFlow: SharedFlow<Event<String>>
      //对 UI 而言，state 是 UI 组件的状态，它应当始终都有一个值来表示其状态，且当有新的订阅者加入时，它也应知道当前的状态，即 StateFlow 的粘性。
      //而 event 事件只有在发生某些动作后才会触发，不需要有初始值。SharedFlow 默认非粘性的，当新的订阅者加入时，它不会重复发生已经发生过的事件。
      //对于状态而言，即使多次发送，我们只关注最新的状态；对于事件而言，如果多次发送，我们不想丢失任何一个前台的事件



      //https://juejin.cn/post/7217601930917969957#heading-2
      //热流  StateFlow或者SharedFlow
      //SharedFlow实现flow，但是collect不执行, 实现是MutableSharedFlow
      // public interface SharedFlow<out T> : Flow<T> {
      //   public val replayCache: List<T>
      //   override suspend fun collect(collector: FlowCollector<T>): Nothing
      // }
      val shared = MutableSharedFlow<String>(0,0, BufferOverflow.SUSPEND)
      //构建参数
      //replay 新的订阅时，历史数据发送的个数  默认为0，即不支持粘性
      //extraBufferCapacity 缓存区容量
      //onBufferOverflow 缓冲策略，丢弃还是挂起
      CoroutineScope(Dispatchers.Default).launch {
        shared.collect {
          println("sharedFlow $it")
        }
      }
      CoroutineScope(Dispatchers.Default).launch {
        delay(1000)
        shared.emit("1")
      }

      CoroutineScope(Dispatchers.Default).launch {
        delay(2000)
        shared.collect {
          println("sharedFlow delay 粘性监听： $it")  //SharedFlow没有粘性
        }
      }

      //StateFlow继承SharedFlow  可以获取value，实现类MutableStateFlow
      // public interface StateFlow<out T> : SharedFlow<T> {
      //   public val value: T
      // }
      val state = MutableStateFlow("") //有粘性的，提供一个默认空值，方便清空数据
      CoroutineScope(Dispatchers.Default).launch {
        println("stateFlow current:${state.value}")  //当前值，只有stateFlow才有，sharedFlow没有
        state.collect {
          //监听
          println("stateFlow1 $it")
        }
      }
      CoroutineScope(Dispatchers.Default).launch {
        state.emit("2")
        state.collect {
          //监听
          println("stateFlow2 $it")
        }
      }
      CoroutineScope(Dispatchers.Default).launch {
        delay(1000)
        state.collect{
          println("stateFlow delay 粘性监听  $it") //事件发生后监听，存在粘性
        }
      }

      Thread.sleep(20*1000)

      //热流的使用场景
      //热流 SharedFlow 可以用来做事件总线，替换 EventBus
      //热流 StateFlow 可以用来做事件状态更新，替换 LiveData，并结合 MVI 替换 MVVM
    }

    suspend fun simpleFlow() {
      //每100ms，执行一次
//     1671787573413 : 1
//     1671787573513 : 2
//     1671787573613 : 3
//     1671787573714 : 4
//     1671787573814 : 5
      flow { //构建Flow
        //构建块中的代码可以挂起
        for (i in 1..5) {
          delay(100)
          emit(i) //发射数据
        }
      }.collect { //执行flow.collect() 收集数据
        println("${System.currentTimeMillis()} : $it")
      }
    }

    suspend fun getFlow(): Flow<Int> {
      return flow {
        println("getFlow started")
        for (i in 1..5) {
          delay(100)
          emit(i)
        }
      }
    }
  }
}