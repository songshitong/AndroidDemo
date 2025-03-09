package com.sst.libkotlin.coroutines

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select

class MSelect {
  companion object{

    @JvmStatic
    fun main(args:Array<String>){
      //select 的多路复用   复用多个 await 和 Channel、SelectClause 及使用 Flow
      //select
      //支持的method
      // Receiver  Suspending function   Select clause
      // Job       join                   onJoin
      // Deferred  await                  onAwait
      // SendChannel send                 onSend
      // ReceiveChannel receive           onReceive
      // ReceiveChannel receiveCatching   onReceiveCatching
      // none        delay                onTimeout

      //返回最先有结果的channel
      `test select channel`()
    }

    //https://juejin.cn/post/7041835887897870373
    fun `test select channel`() = runBlocking<Unit> {
      val channels = listOf(Channel<Int>(), Channel<Int>())
      GlobalScope.launch {
        delay(100)
        channels[0].send(200)
      }

      GlobalScope.launch {
        delay(50)
        channels[1].send(100)
      }

      val result = select<Int?> {
        channels.forEach { channel ->
          channel.onReceive { it }
        }
      }
      println(result) //结果为100
    }


  }
}