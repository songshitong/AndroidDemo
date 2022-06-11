
TimeUnit
TimeUnit提供了可读性更好的线程暂停操作，通常用来替换Thread.sleep()
Thread.sleep()是一个重载方法，可以接收长整型毫秒和长整型的纳秒参数，这样对程序员造成的一个问题就是很难知道到底当前线程
  是睡眠了多少秒、分、小时或者天
```
Thread.sleep（2400000）
```
TimeUnit的使用
```
TimeUnit.MINUTES.sleep(4);
```

TimeUnit提供的时间维度
DAYS、HOURS、MINUTES,SECONDS、MILLISECONDS和NANOSECONDS
维度转换
toMillis,toSeconds....


TimeUnit.MINUTES.sleep 
最终执行的是Thread.sleep
```
public void sleep(long timeout) throws InterruptedException {
        if (timeout > 0) {
            long ms = toMillis(timeout);
            int ns = excessNanos(timeout, ms);
            Thread.sleep(ms, ns);
        }
    }
```

timedJoin方法
最终执行的是thread.join方法
```
 public void timedJoin(Thread thread, long timeout)
            throws InterruptedException {
        if (timeout > 0) {
            long ms = toMillis(timeout);
            int ns = excessNanos(timeout, ms);
            thread.join(ms, ns);
        }
    }
```

timedWait方法
最终执行的是Object.wait方法
```
public void timedWait(Object obj, long timeout)
            throws InterruptedException {
        if (timeout > 0) {
            long ms = toMillis(timeout);
            int ns = excessNanos(timeout, ms);
            obj.wait(ms, ns);
        }
    }
```