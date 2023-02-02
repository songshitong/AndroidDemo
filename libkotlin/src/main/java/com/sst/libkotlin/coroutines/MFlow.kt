package com.sst.libkotlin.coroutines

import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
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
            //默认构建的flow是冷流，如果不执行 每次collect都执行一次，类似控制水龙头的开关
            //   冷流中的代码直到流被收集的时候才会执行。
            //   热流中的代码会立即执行，不管有没有被收集
            runBlocking {
                getFlow().collect { value -> println("start collect1 $value") }
                getFlow().collect { value -> println("start collect2 $value") }

            }


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