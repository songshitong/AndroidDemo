```
       ThrowsException  SpecialValue  Blocks  TimesOut
Insert  add(e)          offer(e)      put(e)  offer(e, time, unit)
Remove  remove()        poll()        take()  poll(time, unit)
Examine element()       peek()not    applicable not applicable

add 与offer的区别，都是非阻塞的
When using a capacity-restricted queue, it is generally preferable to use offer.
```

public interface BlockingQueue<E> extends Queue<E> {
boolean add(E e);
boolean offer(E e);
}