android sdk 30   jdk1.8    
https://juejin.cn/post/6930632285301669895#heading-1
类声明
从 LinkedList 实现的几个接口可以看出来，LinkedList 是支持快速访问，可克隆，可序列化的，而且可以将之看成一个支持有序访问的队列或者栈
```
package java.util;
public class LinkedList<E> extends AbstractSequentialList<E> 
    implements List<E>, Deque<E>, Cloneable, java.io.Serializable
public abstract class AbstractSequentialList<E> extends java.util.AbstractList<E>    
```

LinkedList 内部通过双向链表的数据结构来实现的，每个链表结点除了存储本结点的数据元素外，还有两个指针分别用于指向其上下两个相邻结点，
这个结点就是 LinkedList 中的静态类 Node
```
    private static class Node<E> {
        //当前结点包含的实际元素
        E item;
        //指向下一个结点
        Node<E> next;
        //指向上一个结点
        Node<E> prev;
        //构造器参数 前一个，当前，下一个
        Node(Node<E> prev, E element, Node<E> next) {
            this.item = element;
            this.next = next;
            this.prev = prev;
        }
    }
```


成员变量
```
    //双向链表包含的结点总数，即数据总量
    transient int size = 0;

    //双向链表的头结点
    transient Node<E> first;

    //双向链表的尾结点
    transient Node<E> last;

    //序列化ID
    private static final long serialVersionUID = 876323262645176354L;
```
当只有一个元素时， first 和 last 指向同一个



构造函数
LinkedList 不需要去请求一片连续的内存空间来存储数据，而是在每次有新的元素需要添加时再来动态请求内存空间，因此 LinkedList 的
  两个构造函数都很简单


添加元素
Add(E e) 方法用于向链表的尾部添加结点，因为有 last 指向链表的尾结点，因此向尾部添加新元素只需要修改几个引用即可，
 效率较高
```
    //将元素 e 作为尾结点添加
    //因为 LinkedList 允许添加相同元素，所以此方法固定返回 true
    public boolean add(E e) {
        linkLast(e);
        return true;
    }
    
    //将元素 e 置为尾结点
    void linkLast(E e) {
        //先保存原尾结点
        final Node<E> l = last;
        //构建新的尾结点，并指向链表最后
        final Node<E> newNode = new Node<>(l, e, null);
        last = newNode;
        //如果原尾结点为 null，说明原链表包含的元素个数为 0，则此时插入的尾结点同时即为头结点
        //如果原尾结点不为 null，则将 next 指向新的尾结点
        if (l == null)
            first = newNode;
        else
            l.next = newNode;
        //元素个数加1
        size++;
        modCount++;
    }
```

Add(int index, E element) 方法用于向指定索引处添加元素，需要先通过索引 index 获取相应位置的结点，并在该位置开辟一个新的结点
 来存储元素 element，最后还需要修改相邻结点间的引用
```
     //在索引 index 处插入元素 element
    public void add(int index, E element) {
        //判断索引大小是否合法，不合法则抛出 IndexOutOfBoundsException
        checkPositionIndex(index);
        //如果 index == size，则将 element 作为尾结点来添加
        //否则则在索引 index 前开辟一个新结点
        if (index == size)
            linkLast(element);
        else
            linkBefore(element, node(index));
    }

    //将元素 e 置为 succ 结点的上一个结点
    void linkBefore(E e, Node<E> succ) {
        //保存 succ 的上一个结点信息
        final Node<E> pred = succ.prev;
        //构建元素 e 对应的结点
        final Node<E> newNode = new Node<>(pred, e, succ);
        //将结点 succ 的上一个结点指向 newNode
        succ.prev = newNode;
        //如果 pred 为 null，说明 succ 是头结点，则将 newNode 置为新的头结点
        if (pred == null)
            first = newNode;
        else
            pred.next = newNode;
        //元素个数加1
        size++;
        modCount++;
    }
private void checkElementIndex(int index) {
        if (!isElementIndex(index))
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }

private void checkPositionIndex(int index) {
    if (!isPositionIndex(index))
        throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
}
 private boolean isElementIndex(int index) {
        return index >= 0 && index < size;
    }

private boolean isPositionIndex(int index) {
    return index >= 0 && index <= size;
}  
```


移除元素
remove() 方法有两种重载形式，其内部都是通过调用 unlink(Node<E> x) 方法来移除指定结点在链表中的引用，
   不同于 ArrayList 在移除元素时可能导致的大量数据移动，LinkedList 只需要通过移除引用即可将指定元素从链表中移除
```
    //移除索引 index 处的结点
    public E remove(int index) {
        //判断索引大小是否合法，不合法则抛出 IndexOutOfBoundsException
        checkElementIndex(index);
        return unlink(node(index));
    }

    //对链表进行正向遍历，移除第一个元素值为 o 的结点
    //如果移除成功则返回 true，否则返回 false
    public boolean remove(Object o) {
        if (o == null) {
            for (Node<E> x = first; x != null; x = x.next) {
                if (x.item == null) {
                    //移除结点 x
                    unlink(x);
                    return true;
                }
            }
        } else {
            for (Node<E> x = first; x != null; x = x.next) {
                if (o.equals(x.item)) {
                    //移除结点 x
                    unlink(x);
                    return true;
                }
            }
        }
        return false;
    }

    //移除结点 x 并返回其包含的元素值
    E unlink(Node<E> x) {
        final E element = x.item;
        final Node<E> next = x.next;
        final Node<E> prev = x.prev;
        //如果 prev == null，说明结点 x 为头结点，则将头结点置为原先的第二个结点
        //如果 prev != null，则移除对结点 x 的引用
        if (prev == null) {
            first = next;
        } else {
            prev.next = next;
            x.prev = null;
        }
        //如果 next == null，则说明结点 x 为尾结点，则将尾结点置为原先的倒数第二个结点
        //如果 next != null，则移除对结点 x 的引用
        if (next == null) {
            last = prev;
        } else {
            next.prev = prev;
            x.next = null;
        }
        //帮助GC回收
        x.item = null;
        //元素个数减1
        size--;
        modCount++;
        return element;
    }
```


随机访问元素
对于单向链表来说，如果想随机定位到某个结点，那么只能通过从头结点开始遍历的方式来定位，最极端的情况下需要遍历整个链表
  才能定位到目标结点。如果是双向链表，则可以选择正向遍历或者反向遍历，最极端的情况下需要遍历一半链表才能定位到目标结点。
  所以，相比数组来说 LinkedList 的随机访问效率并不高
```
    //获取索引 index 处的结点元素
    public E get(int index) {
        //判断索引大小是否合法，不合法则抛出 IndexOutOfBoundsException
        checkElementIndex(index);
        return node(index).item;
    }

    //将索引 index 处的结点包含的元素修改为 element，并返回旧元素
    public E set(int index, E element) {
        //判断索引大小是否合法，不合法则抛出 IndexOutOfBoundsException
        checkElementIndex(index);
        Node<E> x = node(index);
        E oldVal = x.item;
        x.item = element;
        return oldVal;
    }

    //获取索引 index 处的结点
    Node<E> node(int index) {
        //size >> 1 的含义即为：将 size 值除以 2
        //如果 index 靠近链表的头部，则从头部向尾部正向遍历查找结点
        //如果 index 靠近链表的尾部，则从尾部向头部反向遍历查找结点
        //最极端的情况下遍历一半元素才能定位到目标节点
        if (index < (size >> 1)) {
            Node<E> x = first;
            for (int i = 0; i < index; i++)
                x = x.next;
            return x;
        } else {
            Node<E> x = last;
            for (int i = size - 1; i > index; i--)
                x = x.prev;
            return x;
        }
    }
```

几个常用的方法
```
    //判断是否包含元素 o
    public boolean contains(Object o) {
        return indexOf(o) != -1;
    }

    //获取元素个数
    public int size() {
        return size;
    }
    
    //清空链表元素，将各个结点之间的引用都切断
    public void clear() {
        for (Node<E> x = first; x != null; ) {
            Node<E> next = x.next;
            x.item = null;
            x.next = null;
            x.prev = null;
            x = next;
        }
        first = last = null;
        size = 0;
        modCount++;
    }
    
    //返回第一个元素值为 o 的结点所在的索引值
    //如果查找不到，则返回 -1
    public int indexOf(Object o) {
        int index = 0;
        if (o == null) {
            for (Node<E> x = first; x != null; x = x.next) {
                if (x.item == null)
                    return index;
                index++;
            }
        } else {
            for (Node<E> x = first; x != null; x = x.next) {
                if (o.equals(x.item))
                    return index;
                index++;
            }
        }
        return -1;
    }

    //返回最后一个元素值为 o 的结点所在的索引值
    //如果查找不到，则返回 -1
    public int lastIndexOf(Object o) {
        int index = size;
        if (o == null) {
            for (Node<E> x = last; x != null; x = x.prev) {
                index--;
                if (x.item == null)
                    return index;
            }
        } else {
            for (Node<E> x = last; x != null; x = x.prev) {
                index--;
                if (o.equals(x.item))
                    return index;
            }
        }
        return -1;
    }
```


原文链接：https://blog.csdn.net/devnn/article/details/82716447
Deque是一个双端队列接口，继承自Queue接口，Deque的实现类是LinkedList、ArrayDeque、LinkedBlockingDeque，
其中LinkedList是最常用的
Deque有三种用途：
普通队列(一端进另一端出):
Queue queue = new LinkedList()或Deque deque = new LinkedList()
双端队列(两端都可进出)
Deque deque = new LinkedList()
堆栈
Deque deque = new LinkedList()

Deque 接口
以上介绍的几个方法都是 List 接口中所声明的，接下来看下 Deque 接口中的方法
其实 Deque 接口中很多方法的含义都是类似的，且一些方法都是相互调用的，并不算复杂
```
    //将元素 e 置为头结点
    public void addFirst(E e) {
        linkFirst(e);
    }
    private void linkFirst(E e) {
        final Node<E> f = first;
        final Node<E> newNode = new Node<>(null, e, f);
        first = newNode;
        if (f == null)
            last = newNode;
        else
            f.prev = newNode;
        size++;
        modCount++;
    }

    //将元素 e 置为尾结点
    public void addLast(E e) {
        linkLast(e);
    }
    
    //将元素 e 作为尾结点添加
    public boolean offer(E e) {
        return add(e);
    }

    //将元素 e 作为头结点添加
    public boolean offerFirst(E e) {
        addFirst(e);
        return true;
    }

    //将元素 e 作为尾结点添加
    public boolean offerLast(E e) {
        addLast(e);
        return true;
    }

    //获取头部结点的元素值
    public E peekFirst() {
        final Node<E> f = first;
        return (f == null) ? null : f.item;
    }

    //获取尾部结点的元素值
    public E peekLast() {
        final Node<E> l = last;
        return (l == null) ? null : l.item;
    }

    //获取头部结点的元素值，并将之从链表中移除
    public E pollFirst() {
        final Node<E> f = first;
        return (f == null) ? null : unlinkFirst(f);
    }
    
    private E unlinkFirst(Node<E> f) {
        // assert f == first && f != null;
        final E element = f.item;
        final Node<E> next = f.next;
        f.item = null;
        f.next = null; // help GC
        first = next;
        if (next == null)
            last = null;
        else
            next.prev = null;
        size--;
        modCount++;
        return element;
    }

    //获取尾部结点的元素值，并将之从链表中移除
    public E pollLast() {
        final Node<E> l = last;
        return (l == null) ? null : unlinkLast(l);
    }
    
    private E unlinkLast(Node<E> l) {
        // assert l == last && l != null;
        final E element = l.item;
        final Node<E> prev = l.prev;
        l.item = null;
        l.prev = null; // help GC
        last = prev;
        if (prev == null)
            first = null;
        else
            prev.next = null;
        size--;
        modCount++;
        return element;
    }
    
    //将元素 e 作为头结点添加
    public void push(E e) {
        addFirst(e);
    }

    //获取头部结点的元素值，并将之从链表中移除
    public E pop() {
        return removeFirst();
    }

    //从链表头部向尾部正向遍历，移除第一个元素值为 o 的结点
    //如果移除成功则返回 true，否则返回 false
    public boolean removeFirstOccurrence(Object o) {
        return remove(o);
    }
    
    //移除第一个并返回
    public E removeFirst() {
        final Node<E> f = first;
        if (f == null)
            throw new NoSuchElementException();
        return unlinkFirst(f);
    }
    
    //移除最后一个并返回
    public E removeLast() {
        final Node<E> l = last;
        if (l == null)
            throw new NoSuchElementException();
        return unlinkLast(l);
    }

    //从链表尾部向头部反向遍历，移除第一个元素值为 o 的结点
    //如果移除成功则返回 true，否则返回 false
    public boolean removeLastOccurrence(Object o) {
        if (o == null) {
            for (Node<E> x = last; x != null; x = x.prev) {
                if (x.item == null) {
                    unlink(x);
                    return true;
                }
            }
        } else {
            for (Node<E> x = last; x != null; x = x.prev) {
                if (o.equals(x.item)) {
                    unlink(x);
                    return true;
                }
            }
        }
        return false;
    }
```


效率测试
上面说过，LinkedList 相比 ArrayList 在添加和移除元素时效率上会高很多，但随机访问元素的效率要比 ArrayList 低，
这里也来做个测试，验证两者之间的差别
分别向 ArrayList 和 LinkedList 存入同等数据量的数据，然后各自移除 100 个元素以及遍历 10000 个元素，观察两者所用的时间
ArrayList：
```
    public static void main(String[] args) {
        List<String> stringArrayList = new ArrayList<>();
        for (int i = 0; i < 300000; i++) {
            stringArrayList.add("leavesC " + i);
        }

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            stringArrayList.remove(100 + i);
        }
        long endTime = System.currentTimeMillis();
        System.out.println("移除 ArrayList 中的100个元素,用时：" + (endTime - startTime) + "毫秒");

        startTime = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            stringArrayList.get(i);
        }
        endTime = System.currentTimeMillis();
        System.out.println("遍历 ArrayList 中的10000个元素,用时：" + (endTime - startTime) + "毫秒");
    }
```
LinkedList：
```
    public static void main(String[] args) {
        List<String> stringLinkedList = new LinkedList<>();
        for (int i = 0; i < 300000; i++) {
            stringLinkedList.add("leavesC " + i);
        }

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            stringLinkedList.remove(100 + i);
        }
        long endTime = System.currentTimeMillis();
        System.out.println("移除 LinkedList 中的100个元素,用时：" + (endTime - startTime) + "毫秒");

        startTime = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            stringLinkedList.get(i);
        }
        endTime = System.currentTimeMillis();
        System.out.println("遍历 LinkedList 中的10000个元素,用时：" + (endTime - startTime) + "毫秒");
    }
```
结果  cpu： AMD Ryzen 7 5700U with Radeon Graphics
```
移除 ArrayList 中的100个元素,用时：5毫秒
遍历 ArrayList 中的10000个元素,用时：1毫秒

移除 LinkedList 中的100个元素,用时：0毫秒
遍历 LinkedList 中的10000个元素,用时：192毫秒
```
ArrayList 适合频繁访问，少量增删的
LinkedList适合频繁增删的，少量访问的