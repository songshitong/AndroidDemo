package com.sst.libkotlin.coroutines

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.RENDEZVOUS
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.broadcast
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select

//https://juejin.cn/post/7041835887897870373
//https://juejin.cn/post/7171272840426029063
class MChannel {
  companion object{

    @JvmStatic
    fun main(args: Array<String>){
       `test know channel`()

       //channel遍历
       val channelA = Channel<String>()
       val iterator = channelA.iterator()
       val job1 = GlobalScope.launch {
         while (iterator.hasNext()){
            println("channelA find with iterator ${iterator.next()}")
           break
         }
       }
      val job2 =GlobalScope.launch{
         for (e in channelA){
           println("channelA find in for $e")
           break
         }
       }
      runBlocking {
        channelA.send("a")
        channelA.send("B")
        joinAll(job1,job2)
      }

       //channel是一个并发安全的队列，它可以用来连接协程，实现不同协程之间的通信   类似java的BlockingQueue

      //channel接口
      //public interface Channel<E> : SendChannel<E>, ReceiveChannel<E> {}
      //最重要的方法send(element: E),receive(): E,cancel(),close()

      //构建参数
      val testChannel = Channel<Int>(
        capacity = RENDEZVOUS, //缓存大小 默认为0
        onBufferOverflow= BufferOverflow.SUSPEND, //缓冲满的策略 默认为挂起
        onUndeliveredElement = null //未发送到消费者的处理
      )
      //根据capacity类型，创建四种channel
      // 比较一下四种 Channel 类型：
      // RendezvousChannel: 翻译成约会类型，缓存区大小为0，且指定为 SUSPEND 挂起策略。发送者和接收者一对一出现，接收者没出现，则发送者 send 会被挂起；
      //    发送者没出现，则接收者 receive 会被挂起。
      // ConflatedChannel: 混合类型。发送者不会挂起，它只有一个 value 值，会被新的值覆盖掉；如果没有数据，则接收者会被挂起。
      // LinkedListChannel: 不限缓存区大小的类型。发送者不会挂起，能一直往队列里存数据；队列无数据时接收者会被挂起。
      // ArrayChannel: 指定缓存区大小的类型。当缓存区满时，发送者根据 BufferOverflow 策略来处理(是否挂起)；当缓存区空时，接收者会被挂起。

      // BroadcastChannel  一个元素可以被多个接受端接收到 而Channel，一个元素只被一个端消费，存在多端时消费不同的元素
      `test broadcast`()

    }

    //写一个简单的生产者，消费者
    //GlobalScope.produce<> {  } 这是指定生产者消费者快捷写法
    //GlobalScope.actor<> {  }
    fun `test know channel`() = runBlocking {
      val channel = Channel<Int>()
      //生产者
      val producer = GlobalScope.launch {
        var i = 0
        while (true) {
          delay(500)
          channel.send(++i)
          println("生产者send $i")
          if(i>=10){
            channel.close()
            break
          }
        }
      }

      //消费者
      val consumer = GlobalScope.launch {
        while (true) {
          if(channel.isClosedForSend){
            break
          }
          val element = channel.receive()
          println("消费者receive $element")
        }
      }
      joinAll(producer, consumer)
    }


    fun `test broadcast`() = runBlocking<Unit> {
      //val broadcastChannel = BroadcastChannel<Int>(Channel.BUFFERED)
      val channel = Channel<Int>() //这里使用默认缓存区大小
      //初始化三个消费者
      val broadcastChannel = channel.broadcast(3) //创建BroadcastChannel
      val producer = GlobalScope.launch {
        List(3){
          delay(100)
          broadcastChannel.send(it)
        }
        //由主导方管理生命周期
        broadcastChannel.close()
      }

      //创建三个消费者
      List(3){ index ->
        GlobalScope.launch {
          val receiveChannel = broadcastChannel.openSubscription() //创建ReceiveChannel
          for (i in receiveChannel){
            println("[#$index] received: $i")
          }
        }
      }.joinAll() //等待三个消费者完成
    }

  }
}