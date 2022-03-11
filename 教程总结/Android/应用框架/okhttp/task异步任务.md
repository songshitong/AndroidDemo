
任务执行 RealConnectionPool.kt
```
private val cleanupQueue: TaskQueue = taskRunner.newQueue()
 private val cleanupTask = object : Task("$okHttpName ConnectionPool") {
    override fun runOnce() = cleanup(System.nanoTime())
  }
cleanupQueue.schedule(cleanupTask)
```

Task是任务单元,可以执行一次或多次
runOnce返回-1，任务执行一次， >-1代表任务延迟多久后执行
默认任务可以取消 cancelable=true
Task.kt
```
abstract class Task(
  val name: String,
  val cancelable: Boolean = true
) {
  // Guarded by the TaskRunner.
  internal var queue: TaskQueue? = null

  /** Undefined unless this is in [TaskQueue.futureTasks]. */
  internal var nextExecuteNanoTime = -1L

  /** Returns the delay in nanoseconds until the next execution, or -1L to not reschedule. */
  abstract fun runOnce(): Long

  internal fun initQueue(queue: TaskQueue) {
    if (this.queue === queue) return
    ...
    this.queue = queue
  }
  ...
}
```

TaskQueue是任务顺序执行的队列，不推荐一个task在多个TaskQueue
   这相当于每个队列都有一个用于其工作的专用线程；实际上，一组队列可以共享一组线程以节省资源。
```
class TaskQueue internal constructor(
  internal val taskRunner: TaskRunner,
  internal val name: String
) {
  //队列停止
  internal var shutdown = false
  
  //当前正在运行的task，可能为null
   /** This queue's currently-executing task, or null if none is currently executing. */
  internal var activeTask: Task? = null
  
  //运行的task执行完是否应该被取消  被取消后就不重复执行了
  /** True if the [activeTask] should be canceled when it completes. */
  internal var cancelActiveTask = false
  
  //按照执行时间的顺序的task列表
  /** Scheduled tasks ordered by [Task.nextExecuteNanoTime]. */
  internal val futureTasks = mutableListOf<Task>()
}
```

TaskRunner一组工作的线程，可以被多个TaskQueue使用
  对使用守护进程线程的任务使用[TaskRunner.INSTANCE]。当前没有非守护进程线程的共享实例。
  TaskRunner还负责在卸载库时释放保留的线程。 这有利于实现代码卸载的容器环境
  大多数应用程序都应该共享一个进程范围的[TaskRunner]，并使用队列处理每个客户端的工作
```
class TaskRunner(
  val backend: Backend
) {
  private var nextQueueName = 10000
  //协调者是否正在等待，如果等待中需要先调用notify进行唤醒  taskRunner作为traskQueue和task的协调者
  private var coordinatorWaiting = false
  //记录taskRunner唤醒的时间
  private var coordinatorWakeUpAt = 0L
  //正在执行TaskQueue.activeTask的TaskQueue列表
  /** Queues with tasks that are currently executing their [TaskQueue.activeTask]. */
  private val busyQueues = mutableListOf<TaskQueue>()

  //TaskQueue.futureTasks不为空的TaskQueue列表
  /** Queues not in [busyQueues] that have non-empty [TaskQueue.futureTasks]. */
  private val readyQueues = mutableListOf<TaskQueue>()
  
  //新建一个queue队列
  fun newQueue(): TaskQueue {
    val name = synchronized(this) { nextQueueName++ }
    return TaskQueue(this, "Q$name")
  }
  
  
}
```

Backend负责线程池创建，TaskRunner的等待和唤起，将任务Runnable添加到线程池
```
interface Backend {
    fun beforeTask(taskRunner: TaskRunner)
    fun nanoTime(): Long
    //notify与wait一起使用，用于TaskRunner的等待和唤起
    fun coordinatorNotify(taskRunner: TaskRunner)
    fun coordinatorWait(taskRunner: TaskRunner, nanos: Long)
    fun execute(runnable: Runnable)
  }

  class RealBackend(threadFactory: ThreadFactory) : Backend {
    private val executor = ThreadPoolExecutor(
        0, // corePoolSize.
        Int.MAX_VALUE, // maximumPoolSize.
        60L, TimeUnit.SECONDS, // keepAliveTime.
        SynchronousQueue(),
        threadFactory
    )

    override fun beforeTask(taskRunner: TaskRunner) {
    }

    override fun nanoTime() = System.nanoTime()

    override fun coordinatorNotify(taskRunner: TaskRunner) {
      taskRunner.notify()
    }

    //等待nanoseconds的时间，当时间为0时，不进行等待，而java.lang.Object.wait会一直等待
    @Throws(InterruptedException::class)
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    override fun coordinatorWait(taskRunner: TaskRunner, nanos: Long) {
      val ms = nanos / 1_000_000L
      val ns = nanos - (ms * 1_000_000L)
      if (ms > 0L || nanos > 0) {
        (taskRunner as Object).wait(ms, ns.toInt())
      }
    }

    override fun execute(runnable: Runnable) {
      executor.execute(runnable)
    }

    fun shutdown() {
      executor.shutdown()
    }
  }
```


看一下任务的执行流程
cleanupQueue.schedule(cleanupTask)

TaskQueue.kt
```
class TaskQueue internal constructor(
  internal val taskRunner: TaskRunner,
  internal val name: String
) {
  //一个任务只能计划一次未来执行。如果任务已在队列中，则使用最早的执行时间。
  //目标执行时间是在尽力而为的基础上实现的
  //如果到达该时间时此队列中的另一个任务正在运行，则允许完成此任务之后再启动新任务。同样，如果主机缺少计算资源，任务将延迟
   fun schedule(task: Task, delayNanos: Long = 0L) {
    synchronized(taskRunner) {
      if (shutdown) {
        //队列停止时不执行任务
        if (task.cancelable) {
          taskLog(task, this) { "schedule canceled (queue is shutdown)" }
          return
        }
        taskLog(task, this) { "schedule failed (queue is shutdown)" }
        throw RejectedExecutionException()
      }
      //决定task是否入队和立即执行
      if (scheduleAndDecide(task, delayNanos, recurrence = false)) {
        taskRunner.kickCoordinator(this)
      }
    }
  }
  
  //支持lambda的schedule
   inline fun schedule(
    name: String,
    delayNanos: Long = 0L,
    crossinline block: () -> Long
  ) {
    schedule(object : Task(name) {
      override fun runOnce() = block()
    }, delayNanos)
  }
  
   //执行一次任务
    inline fun execute(
    name: String,
    delayNanos: Long = 0L,
    cancelable: Boolean = true,
    crossinline block: () -> Unit
  ) {
    schedule(object : Task(name, cancelable) {
      override fun runOnce(): Long {
        block()
        return -1L
      }
    }, delayNanos)
  }
}
```

scheduleAndDecide
```
internal fun scheduleAndDecide(task: Task, delayNanos: Long, recurrence: Boolean): Boolean {
    task.initQueue(this)

    val now = taskRunner.backend.nanoTime()
    //参数task的执行时间
    val executeNanoTime = now + delayNanos

    // If the task is already scheduled, take the earlier of the two times.
    val existingIndex = futureTasks.indexOf(task)
    //task已经存在了
    if (existingIndex != -1) {
      if (task.nextExecuteNanoTime <= executeNanoTime) {
        //已经存在的task时间在前，直接返回 新的task不再入队
        taskLog(task, this) { "already scheduled" }
        return false
      }
      //已经存在的task在后，移除他
      futureTasks.removeAt(existingIndex) // Already scheduled later: reschedule below!
    }
    //不存在相同的task
    task.nextExecuteNanoTime = executeNanoTime
    taskLog(task, this) {
      if (recurrence) "run again after ${formatDuration(executeNanoTime - now)}"
      else "scheduled after ${formatDuration(executeNanoTime - now)}"
    }

    //按时间顺序插入。始终比较增量，因为允许使用nanoTime（）
    // Insert in chronological order. Always compare deltas because nanoTime() is permitted to wrap.
    //找到第一个在delayNanos之后执行的task,然后在他前面插入
    var insertAt = futureTasks.indexOfFirst { it.nextExecuteNanoTime - now > delayNanos }
    if (insertAt == -1) insertAt = futureTasks.size
    futureTasks.add(insertAt, task)
    //如果task位于第一个，返回true
    // Impact the coordinator if we inserted at the front.
    return insertAt == 0
  }
```

taskRunner.kickCoordinator

TaskRunner.kt
```
 internal fun kickCoordinator(taskQueue: TaskQueue) {
    //taskQueue没有正在执行的任务
    if (taskQueue.activeTask == null) {
      if (taskQueue.futureTasks.isNotEmpty()) {
        //taskQueue有等待的任务，添加到readyQueues的后面
        readyQueues.addIfAbsent(taskQueue)
      } else {
        //从readyQueues移除，马上就要执行了
        readyQueues.remove(taskQueue)
      }
    }

    if (coordinatorWaiting) {
      //taskRunner处于waiting状态，需要进行唤醒
      backend.coordinatorNotify(this@TaskRunner)
    } else {
      //backend执行runnable
      backend.execute(runnable)
    }
  }
```

看一下 execute(runnable)
```
private val runnable: Runnable = object : Runnable {
    override fun run() {
      while (true) {
        //找到一个可以立即执行的task，有多个会另起线程去运行，没有的话线程进程等待task的运行时间
        val task = synchronized(this@TaskRunner) {
          awaitTaskToRun()
        } ?: return

        logElapsed(task, task.queue!!) {
          var completedNormally = false
          try {
            runTask(task)
            completedNormally = true
          } finally {
            // If the task is crashing start another thread to service the queues.
            if (!completedNormally) {
              //运行出错了，提交到其他线程运行
              backend.execute(this)
            }
          }
        }
      }
    }
  }
```
先通过awaitTaskToRun找到一个可以立即执行的task，然后通过runTask(task)去执行

```
fun awaitTaskToRun(): Task? {
    ...
    while (true) {
      //readyQueues为空，返回null
      if (readyQueues.isEmpty()) {
        return null // Nothing to do.
      }

      val now = backend.nanoTime()
      var minDelayNanos = Long.MAX_VALUE
      //要执行的task
      var readyTask: Task? = null
      //ture 有多个要执行的task
      var multipleReadyTasks = false

      // Decide what to run. This loop's goal wants to:
      //  * Find out what this thread should do (either run a task or sleep)
      //  * Find out if there's enough work to start another thread.
      //遍历所有的queue
      eachQueue@ for (queue in readyQueues) {
        val candidate = queue.futureTasks[0]
        val candidateDelay = maxOf(0L, candidate.nextExecuteNanoTime - now)

        when {
          // Compute the delay of the soonest-executable task.
          candidateDelay > 0L -> {
            //记录minDelayNanos
            minDelayNanos = minOf(candidateDelay, minDelayNanos)
            //继续遍历queues
            continue@eachQueue
          }

          // If we already have more than one task, that's enough work for now. Stop searching.
          readyTask != null -> {
            multipleReadyTasks = true
            //有多个要执行的task,跳出遍历queues
            break@eachQueue
          }

          // We have a task to execute when we complete the loop.
          else -> {
            //记录task 这是可以立即执行的
            readyTask = candidate
          }
        }
      }

      // Implement the decision.
      when {
        // We have a task ready to go. Get ready.
        readyTask != null -> {
          beforeRun(readyTask)
           
          //有多个task，再次提交到线程池运行runnalbe来执行task 
          // Also start another thread if there's more work or scheduling to do.
          if (multipleReadyTasks || !coordinatorWaiting && readyQueues.isNotEmpty()) {
            backend.execute(runnable)
          }
          //返回task
          return readyTask
        }

        // Notify the coordinator of a task that's coming up soon.
        coordinatorWaiting -> {
          //如果task运行时还没唤醒，唤醒taskrunner
          if (minDelayNanos < coordinatorWakeUpAt - now) {
            backend.coordinatorNotify(this@TaskRunner)
          }
          return null
        }

        // No other thread is coordinating. Become the coordinator!
        else -> {
          //没有task可以立即执行，taskRunner进行阻塞
          coordinatorWaiting = true
          coordinatorWakeUpAt = now + minDelayNanos
          try {
            backend.coordinatorWait(this@TaskRunner, minDelayNanos)
          } catch (_: InterruptedException) {
            // Will cause all tasks to exit unless more are scheduled!
            cancelAll()
          } finally {
            coordinatorWaiting = false
          }
        }
      }
    }
  }
 
 //task运行前将其从readyQueues转移到busyQueues，将其从queue.futureTasks中移除 
 private fun beforeRun(task: Task) {
    ...
    task.nextExecuteNanoTime = -1L
    val queue = task.queue!!
    queue.futureTasks.remove(task)
    readyQueues.remove(queue)
    queue.activeTask = task
    busyQueues.add(queue)
  }  
```

看一下runTask(task)
主要是运行task的runOnce()函数，运行完成后进行重新调度，决定task是否重复运行
```
 private fun runTask(task: Task) {
    ...
    val currentThread = Thread.currentThread()
    val oldName = currentThread.name
    currentThread.name = task.name

    var delayNanos = -1L
    try {
      delayNanos = task.runOnce()
    } finally {
      synchronized(this) {
        afterRun(task, delayNanos)
      }
      currentThread.name = oldName
    }
  }
  
 private fun afterRun(task: Task, delayNanos: Long) {
    ..
    val queue = task.queue!!

    val cancelActiveTask = queue.cancelActiveTask
    queue.cancelActiveTask = false
    queue.activeTask = null
    //从busyQueues中移除queue
    busyQueues.remove(queue)

    if (delayNanos != -1L && !cancelActiveTask && !queue.shutdown) {
      //调度queue里面task所在的列表，决定task是否重新运行
      queue.scheduleAndDecide(task, delayNanos, recurrence = true)
    }
    //queue中还有要运行的task，将其加入readyQueues进行调度
    if (queue.futureTasks.isNotEmpty()) {
      readyQueues.add(queue)
    }
  }
```