多线程调试法，就是通过断点控制不同线程的执行顺序？？？？
 比如通过断点控制不同线程代码同时执行，来验证多线程问题

1.实现一个生产者消费者模式，并说说他的原理
  缓冲区选择并发安全的容器
  BlockingQueue  阻塞队列
  ConcurrentLinkedDeque 
  线程安全的list也可以，不过一般用队列实现
  
  
  疑问：
  1.如果队列是空的，这时候我去取数据，他是怎么工作的
  2.如果队列为满的，这时候我继续放数据，他是怎么工作的
  
  
  对于第一种情况debug，将put和take放上断点，并将断点的suspend调整为thread 这样断点对每个线程都进行阻塞
   当断点在生产者挂起时，切换线程到生产者，查看take的运行， 当运行到notEmpty.await()时 消费线程的状态由running变为wait
   切换到生产线程，f8产生一个数据，查看BlockingQueue的运行，运行return dequeue();并且finally解决运行
   
  第二种情况，一个生产者生产100完100个即是缓冲的100
    切换到生产者0，将断点移到System.out.println("producer ->"+num+" finished") 程序挂起时，缓冲区就满了
    切换到生产者1，查看put方法 执行 notFull.await(); 按下f8，线程变为wait状态
    切换到消费者0，消费一个，dequeue-》notFull.signal() 唤醒生产者
      此时生产者1 没有变为running状态，出现假死现象，debug不能用了
     
   换一种debug方式  
     将断点移到System.out.println，执行完生产者处于wait状态
     在put的enqueue放一个断点
     切到消费者，拿出一个元素，注意不要进入take方法，编辑器容易进入假死
     查看生产者，有一个进入了running状态