
jvmMain/okio/Timeout.kt
```
actual open class Timeout {
  private var hasDeadline = false
  private var deadlineNanoTime = 0L
  private var timeoutNanos = 0L
  
  //设置超时
  open fun timeout(timeout: Long, unit: TimeUnit): Timeout {
    timeoutNanos = unit.toNanos(timeout)
    return this
  }
  
  //设置deadline
  open fun deadlineNanoTime(deadlineNanoTime: Long): Timeout {
    this.hasDeadline = true
    this.deadlineNanoTime = deadlineNanoTime
    return this
  }
  
  
  @Throws(IOException::class) //在deadline后抛出异常
  open fun throwIfReached() {
    if (Thread.interrupted()) {
      Thread.currentThread().interrupt() // Retain interrupted status.
      throw InterruptedIOException("interrupted")
    }

    if (hasDeadline && deadlineNanoTime - System.nanoTime() <= 0) {
      throw InterruptedIOException("deadline reached")
    }
  }
  
  //等待
  @Throws(InterruptedIOException::class)
  fun waitUntilNotified(monitor: Any) {
    try {
      val hasDeadline = hasDeadline()
      val timeoutNanos = timeoutNanos()
      //没有deadline和timeout，一直等待
      if (!hasDeadline && timeoutNanos == 0L) {
        (monitor as Object).wait() // There is no timeout: wait forever.
        return
      }

      //计算等待时间
      // Compute how long we'll wait.
      val start = System.nanoTime()
      val waitNanos = if (hasDeadline && timeoutNanos != 0L) {
        val deadlineNanos = deadlineNanoTime() - start
        minOf(timeoutNanos, deadlineNanos)
      } else if (hasDeadline) {
        deadlineNanoTime() - start
      } else {
        timeoutNanos
      }

      // 使用monitor 等待waitNanos的时间
      var elapsedNanos = 0L
      if (waitNanos > 0L) {
        val waitMillis = waitNanos / 1000000L
        (monitor as Object).wait(waitMillis, (waitNanos - waitMillis * 1000000L).toInt())
        elapsedNanos = System.nanoTime() - start
      }

      //monitor唤醒后，时间超时
      if (elapsedNanos >= waitNanos) {
        throw InterruptedIOException("timeout")
      }
    } catch (e: InterruptedException) {
      Thread.currentThread().interrupt() // Retain interrupted status.
      throw InterruptedIOException("interrupted")
    }
    
    
  //将另一个timeout与当前timeout的超时时间最短的，运行block  
  inline fun intersectWith(other: Timeout, block: () -> Unit) {
    val originalTimeout = this.timeoutNanos()
    //设置最短的超时
    this.timeout(minTimeout(other.timeoutNanos(), this.timeoutNanos()), TimeUnit.NANOSECONDS)

    if (this.hasDeadline()) {
      val originalDeadline = this.deadlineNanoTime()
      if (other.hasDeadline()) {
        //设置最短的deadline
        this.deadlineNanoTime(Math.min(this.deadlineNanoTime(), other.deadlineNanoTime()))
      }
      try {
        block()//执行block
      } finally {
       //复原timeout deadline
        this.timeout(originalTimeout, TimeUnit.NANOSECONDS)
        if (other.hasDeadline()) {
          this.deadlineNanoTime(originalDeadline)
        }
      }
    } else {
     ....
    }
  }
}
```

jvmMain/okio/AsyncTimeout.kt
耗时任务放在子线程的timeout
```
open class AsyncTimeout : Timeout() {
  private var inQueue = false
  private var next: AsyncTimeout? = null
  private var timeoutAt = 0L //监控的超时
  
   protected open fun timedOut() {} //子类执行超时方法
  
  fun enter() {
    ...
    inQueue = true
    scheduleTimeout(this, timeoutNanos, hasDeadline)
  }


 private class Watchdog internal constructor() : Thread("Okio Watchdog") {
    init {
      isDaemon = true
    }
    override fun run() {
      while (true) {
        try {
          var timedOut: AsyncTimeout? = null
          synchronized(AsyncTimeout::class.java) {
            //等待超时的节点
            timedOut = awaitTimeout()         
            if (timedOut === head) { //head为哨兵节点，不调用timedOut方法
              //排队的Timeout已清空，退出线程   再次调用scheduleTimeout时重新创建watchDog
              head = null
              return
            }
          }

          // Close the timed out node, if one was found.
          timedOut?.timedOut() //调用子类，执行timeOut时的方法
        } catch (ignored: InterruptedException) {
        }
      }
    }
  }
  
  @Throws(InterruptedException::class)
  internal fun awaitTimeout(): AsyncTimeout? {
      // Get the next eligible node.
      val node = head!!.next
      //超时队列为空，
      if (node == null) {
        val startNanos = System.nanoTime()
        (AsyncTimeout::class.java as Object).wait(IDLE_TIMEOUT_MILLIS) //等待60ms
        return if (head!!.next == null && System.nanoTime() - startNanos >= IDLE_TIMEOUT_NANOS) {
          head // The idle timeout elapsed.  //60ms后仍然没有入队唤醒，返回head
        } else {
          null // The situation has changed.
        }
      }

      var waitNanos = node.remainingNanos(System.nanoTime())

      // The head of the queue hasn't timed out yet. Await that.
      if (waitNanos > 0) {
        // Waiting is made complicated by the fact that we work in nanoseconds,
        // but the API wants (millis, nanos) in two arguments.
        val waitMillis = waitNanos / 1000000L
        waitNanos -= waitMillis * 1000000L  //等待至剩余时间
        (AsyncTimeout::class.java as Object).wait(waitMillis, waitNanos.toInt())
        return null
      }
      //超时为负的，节点已经超时
      // The head of the queue has timed out. Remove it.
      head!!.next = node.next
      node.next = null
      return node
    }
  
  
  companion object {
     private var head: AsyncTimeout? = null
     
   private fun scheduleTimeout(node: AsyncTimeout, timeoutNanos: Long, hasDeadline: Boolean) {
      synchronized(AsyncTimeout::class.java) {
        // Start the watchdog thread and create the head node when the first timeout is scheduled.
        if (head == null) { //没有头结点 进行创建  watchdog开始监控
          head = AsyncTimeout()
          Watchdog().start()
        }

        //计算超时时间
        val now = System.nanoTime()
        if (timeoutNanos != 0L && hasDeadline) {
          // Compute the earliest event; either timeout or deadline. Because nanoTime can wrap
          // around, minOf() is undefined for absolute values, but meaningful for relative ones.
          node.timeoutAt = now + minOf(timeoutNanos, node.deadlineNanoTime() - now)
        } else if (timeoutNanos != 0L) {
          node.timeoutAt = now + timeoutNanos
        } else if (hasDeadline) {
          node.timeoutAt = node.deadlineNanoTime()
        } else {
          throw AssertionError()
        }

        // 计算剩余时间  timeoutAt - now
        val remainingNanos = node.remainingNanos(now)
        var prev = head!!
        while (true) {
          //按照剩余时间插入
          if (prev.next == null || remainingNanos < prev.next!!.remainingNanos(now)) {
            node.next = prev.next
            prev.next = node
            if (prev === head) { 
              // Wake up the watchdog when inserting at the front.
              (AsyncTimeout::class.java as Object).notify()
            }
            break  //插入节点
          }
          prev = prev.next!!
        }
      }
    }
  
  }
}
  
```