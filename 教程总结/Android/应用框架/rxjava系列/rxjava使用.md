https://blog.csdn.net/u014624241/article/details/78432762
onNext()： RxJava的事件回调方法，针对普通事件。
onCompleted(): 事件队列完结。RxJava 不仅把每个事件单独处理，还会把它们看做一个队列。RxJava 规定，当不会再有新的 onNext() 发出时，
   需要触发 onCompleted() 方法作为标志。
onError(): 事件队列异常。在事件处理过程中出异常时，onError() 会被触发，同时队列自动终止，不允许再有事件发出。
在一个正确运行的事件序列中,onCompleted() 和 onError() 有且只有一个，并且是事件序列中的最后一个。需要注意的是，
    onCompleted() 和 onError() 二者也是互斥的，即在队列中调用了其中一个，就不应该再调用另一个。


todo
https://mp.weixin.qq.com/s/ZJMVB2_H-E3LgCNdb3MOSg