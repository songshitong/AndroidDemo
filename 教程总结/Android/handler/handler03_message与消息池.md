http://gityuan.com/2015/12/26/handler-message-framework/
Message
对象结构
每个消息用Message表示，Message主要包含以下内容：
数据类型	   成员变量	解释
int	       what	    消息类别
long	   when	    消息触发时间
int	       arg1	    参数1
int	       arg2	    参数2
Object	   obj	   消息内容
Handler	   target	消息响应方
Runnable   callback	回调方法


消息池
在代码中，可能经常看到recycle()方法，咋一看，可能是在做虚拟机的gc()相关的工作，其实不然，这是用于把消息加入到消息池的作用。
这样的好处是，当消息池不为空时，可以直接从消息池中获取Message对象，而不是直接创建，提高效率。

静态变量sPool的数据类型为Message，通过next成员变量，维护一个消息池；静态变量MAX_POOL_SIZE代表消息池的可用大小；
消息池的默认大小为50。

消息池常用的操作方法是obtain()和recycle()

obtain
从消息池中获取消息
/frameworks/base/core/java/android/os/Message.java
```
public static Message obtain() {
    synchronized (sPoolSync) {
        if (sPool != null) {
            Message m = sPool;
            sPool = m.next;
            m.next = null; //从sPool中取出一个Message对象，并消息链表断开
            m.flags = 0; // 清除in-use flag
            sPoolSize--; //消息池的可用大小进行减1操作
            return m;
        }
    }
    return new Message(); // 当消息池为空时，直接创建Message对象
}
```
obtain()，从消息池取Message，都是把消息池表头的Message取走，再把表头指向next;


recycle
把不再使用的消息加入消息池
```
 public void recycle() {
          if (isInUse()) {
              return;
          }
          recycleUnchecked();
      }
      
 //对于不再使用的消息，加入到消息池
void recycleUnchecked() {
    //将消息标示位置为IN_USE，并清空消息所有的参数。
    flags = FLAG_IN_USE;
    what = 0;
    arg1 = 0;
    arg2 = 0;
    obj = null;
    replyTo = null;
    sendingUid = -1;
    when = 0;
    target = null;
    callback = null;
    data = null;
    synchronized (sPoolSync) {
        if (sPoolSize < MAX_POOL_SIZE) { //当消息池没有满时，将Message对象加入消息池
            next = sPool;
            sPool = this;
            sPoolSize++; //消息池的可用大小进行加1操作
        }
    }
}     
```
recycle()，将Message加入到消息池的过程，都是把Message加到链表的表头；

recycle的使用   looper在消息被分发之后，也就是执行了dispatchMessage方法之后，进行了消息回收
/frameworks/base/core/java/android/os/Looper.java
```
public static void loop() {
    for (;;) {
        Message msg = queue.next(); 
        try {
            msg.target.dispatchMessage(msg);
        } 
        msg.recycleUnchecked();
    }
}
```