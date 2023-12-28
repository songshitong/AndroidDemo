


https://www.jianshu.com/p/2fff6fe403dd
IO缓冲小结
IO缓冲区的存在，减少了系统调用。也就是说，如果缓冲区能满足读入/写出需求，则不需要进行系统调用，维护系统读写数据的习惯。
//每次读写io需要进行用户态，内核态的切换  这部分比较耗时，java缓冲区减少切换
//内核读取磁盘通常是批量读取
局部性原理：操作系统在访问磁盘时，由于局部性原理，操作系统不会每次只读取一个字节（代价太大），而是借助硬件直接存储器存取（DMA）
一次性读取一片（一个或者若干个磁盘块）数据。因此，就需要有一个“中间缓冲区”——即内核缓冲区。先把数据从磁盘读到内核缓冲区中，
然后再把数据从内核缓冲区搬到用户缓冲区

BufferedInputStream 没有
BufferedOutputStream
```
    public synchronized void write(byte b[], int off, int len) throws IOException {
        if (len >= buf.length) {
            // 要写出的数据大于缓冲区的容量，也不用缓冲区策略
            // 先将缓冲区数据写出
            flushBuffer();
            // 再直接通过输出流out直接将数据写出
            out.write(b, off, len);
            return;
        }
        if (len > buf.length - count) {
            // 要写出的数据大于缓冲区还可写入的容量，将缓冲区数据写出
            flushBuffer();
        }
        // 将要写出的数据写入到缓冲区
        System.arraycopy(b, off, buf, count, len);
        // 更新缓冲区已添加的数据容量
        count += len;
    }
```

不管是读入还是写出，缓冲区的存在必然涉及copy的过程，而如果涉及双流操作，比如从一个输入流读入，再写入到一个输出流，那么这种情况下，
在缓冲存在的情况下，数据走向是：
-> 从输入流读出到缓冲区
-> 从输入流缓冲区copy到 b[]
-> 将 b[] copy 到输出流缓冲区
-> 输出流缓冲区读出数据到输出流
上面情况存在冗余copy操作，Okio应运而生

Okio实现
在Okio里，解决了双流操作时，中间数据 b[] 存在冗余拷贝的问题



okio source reset
支持mark,reset的source
https://github.com/square/okio/blob/master/samples/src/jvmTest/java/okio/samples/SourceMarkerTest.java


版本2.8.0
okio-2.8.0-sources.jar


接口类
jvmMain\okio\Sink.kt
写入接口
```
actual interface Sink : Closeable, Flushable {
  @Throws(IOException::class)
  actual fun write(source: Buffer, byteCount: Long)

  @Throws(IOException::class)
  actual override fun flush()

  actual fun timeout(): Timeout

  @Throws(IOException::class)
  actual override fun close()
}
```
读取接口
jvmMain/okio/Source.kt
```
actual interface Source : Closeable {
  @Throws(IOException::class)
  actual fun read(sink: Buffer, byteCount: Long): Long

  actual fun timeout(): Timeout

  @Throws(IOException::class)
  actual override fun close()
}
```



commonMain/okio/Segment.kt
Okio使用Segment来作为数据存储手段。Segment 实际上也是对 byte[] 进行封装，再通过各种属性来记录各种状态。在交换时，如果可以，
将Segment整体作为数据传授媒介，这样就没有具体数据的copy过程，而是交换了对应的Segment引用

Segment被设计成可以被分割，在将Segment分割成两个Segment时，就会进行数据分享，即使用相同的byte[] 数组，只不过 pos ~ limit 标记不同罢了
```
internal class Segment {
 //数据array
  @JvmField val data: ByteArray
  // 有效数据索引起始位置
  @JvmField var pos: Int = 0
  // 有效数据索引结束位置
  @JvmField var limit: Int = 0

  @JvmField var shared: Boolean = false
  // 指示当前Segment是否为数据拥有者，与shared互斥
  // 默认构造函数的Segment owner为true，当把数据分享
  // 出去时，被分享的Segment的owner标记为false
  @JvmField var owner: Boolean = false

  @JvmField var next: Segment? = null

  @JvmField var prev: Segment? = null
  
   constructor() {
    this.data = ByteArray(SIZE) //一个segment大小为8192
    this.owner = true
    this.shared = false
  }
  }
```
split分割
```
fun split(byteCount: Int): Segment {
    require(byteCount > 0 && byteCount <= limit - pos) { "byteCount out of range" }
    val prefix: Segment

    // We have two competing performance goals:
    //  - Avoid copying data. We accomplish this by sharing segments.
    //  - Avoid short shared segments. These are bad for performance because they are readonly and
    //    may lead to long chains of short segments.
    // To balance these goals we only share segments when the copy will be large.
    if (byteCount >= SHARE_MINIMUM) { //1024
      prefix = sharedCopy() //返回一个copy
    } else {
      prefix = SegmentPool.take() //从池子里面复用一个
      data.copyInto(prefix.data, startIndex = pos, endIndex = pos + byteCount) //data拷贝到prefix
    }
    //矫正limit和pos
    prefix.limit = prefix.pos + byteCount
    pos += byteCount
    prev!!.push(prefix) //prefix放在prev的后面
    return prefix
  }
  
   //返回新的Segment，共用data，可以矫正pos和limit但是不允许写，不可以被pool
  fun sharedCopy(): Segment {
    shared = true
    return Segment(data, pos, limit, true, false)
  }
  
  fun push(segment: Segment): Segment {
    segment.prev = this
    segment.next = next
    next!!.prev = segment
    next = segment
    return segment
  }
```
jvmMain/okio/SegmentPool.kt
Segment缓存池
与大多数缓存池一样，SegmentPool避免的内存的重新分配。SegmentPool存储的大小为 64 * 1024， Semgent数据存储大小为 8192，因此最多存下8个Segment。

SegmentPool复用IO操作中分配到的内存，也是得益于Segment的设计，当涉及到多流操作时，效果明显。 
//个人：多个流时，每个流的缓冲区得到复用，glide中也有类似的设计
```
  private val LOCK = Segment(ByteArray(0), pos = 0, limit = 0, shared = false, owner = false)
 private val HASH_BUCKET_COUNT =
    Integer.highestOneBit(Runtime.getRuntime().availableProcessors() * 2 - 1)
  private val hashBuckets: Array<AtomicReference<Segment?>> = Array(HASH_BUCKET_COUNT) {
    AtomicReference<Segment?>()
  }
  
  private fun firstRef(): AtomicReference<Segment?> {
    // Get a value in [0..HASH_BUCKET_COUNT).
    val hashBucket = (Thread.currentThread().id and (HASH_BUCKET_COUNT - 1L)).toInt()
    //不同线程的第一个Segment
    return hashBuckets[hashBucket]
  }
  
  actual fun take(): Segment {
    val firstRef = firstRef()
    val first = firstRef.getAndSet(LOCK) //取数据时，第一个设为LOCK
    when {
      first === LOCK -> {
        // We didn't acquire the lock. Don't take a pooled segment.
        return Segment() //新建
      }
      first == null -> {
        // We acquired the lock but the pool was empty. Unlock and return a new segment.
        firstRef.set(null) //头重置
        return Segment() //新建
      }
      else -> {
        // We acquired the lock and the pool was not empty. Pop the first element and return it.
        firstRef.set(first.next) //取出链表的元素  头重置
        first.next = null
        first.limit = 0
        return first
      }
    }
  }
  
   actual fun recycle(segment: Segment) {
        ...
        //共享segment不回收
        if (segment.shared) return // This segment cannot be recycled.
    
        val firstRef = firstRef()
        val first = firstRef.get()
        if (first === LOCK) return // A take() is currently in progress.
        val firstLimit = first?.limit ?: 0
        if (firstLimit >= MAX_SIZE) return // Pool is full.
    
        segment.next = first //放到first前面，形成单向链表
        segment.pos = 0
        segment.limit = firstLimit + Segment.SIZE
    
        if (!firstRef.compareAndSet(first, segment)) segment.next = null  
        // If we raced another operation: Don't recycle this segment.
  }
```

Buffer
Okio使用了Segment作为数据存储的方式，自然要提供对应的缓冲方式来操作Segment，Segment在Buffer中以双向链表形式存在。
Buffer则负责此项事务。Buffer也实现了BufferedSource和BufferedSink，这是因在使用Okio提供的输入/输出缓冲时，都需要进行缓冲处理，
均由Buffer来处理，这样使API对应。
常见的读写api通过buffer操作segment实现
```
actual class Buffer : BufferedSource, BufferedSink, Cloneable, ByteChannel {
  @JvmField internal actual var head: Segment? = null
}
```

okio
对外api
Okio.sink(new File("yourSaveFilePath"))
jvmMain/okio/JvmOkio.kt
```
fun File.sink(append: Boolean = false): Sink = FileOutputStream(this, append).sink()
private class OutputStreamSink(
  private val out: OutputStream,
  private val timeout: Timeout
) : Sink {

  override fun write(source: Buffer, byteCount: Long) {
    checkOffsetAndCount(source.size, 0, byteCount)
    var remaining = byteCount
    while (remaining > 0) {
      timeout.throwIfReached()
      val head = source.head!!
      val toCopy = minOf(remaining, head.limit - head.pos).toInt()
      //使用OutputStream流，将输入写入 buffer的data
      out.write(head.data, head.pos, toCopy)

      head.pos += toCopy
      remaining -= toCopy
      source.size -= toCopy
      if (head.pos == head.limit) {
        source.head = head.pop()
        //回收
        SegmentPool.recycle(head)
      }
    }
  }

  override fun flush() = out.flush()

  override fun close() = out.close()

  override fun timeout() = timeout

  override fun toString() = "sink($out)"
}
```
Okio.source(new File("yourFilePath"))
```
fun File.source(): Source = inputStream().source()

fun InputStream.source(): Source = InputStreamSource(this, Timeout())

private class InputStreamSource(
  private val input: InputStream,
  private val timeout: Timeout
) : Source {

  override fun read(sink: Buffer, byteCount: Long): Long {
    if (byteCount == 0L) return 0
    require(byteCount >= 0) { "byteCount < 0: $byteCount" }
    try {
      timeout.throwIfReached()
      val tail = sink.writableSegment(1) //从segmentPool获取
      val maxToCopy = minOf(byteCount, Segment.SIZE - tail.limit).toInt()
      val bytesRead = input.read(tail.data, tail.limit, maxToCopy) //读取数据
      if (bytesRead == -1) {
        if (tail.pos == tail.limit) {
          // We allocated a tail segment, but didn't end up needing it. Recycle!
          sink.head = tail.pop()
          SegmentPool.recycle(tail)//回收
        }
        return -1
      }
      tail.limit += bytesRead
      sink.size += bytesRead
      return bytesRead.toLong()
    } catch (e: AssertionError) {
      if (e.isAndroidGetsocknameError) throw IOException(e)
      throw e
    }
  }

  override fun close() = input.close()

  override fun timeout() = timeout

  override fun toString() = "source($input)"
}

writableSegment的最终实现
internal inline fun Buffer.commonWritableSegment(minimumCapacity: Int): Segment {
  require(minimumCapacity >= 1 && minimumCapacity <= Segment.SIZE) { "unexpected capacity" }

  if (head == null) {
    val result = SegmentPool.take() // Acquire a first segment.
    head = result
    result.prev = result
    result.next = result
    return result
  }

  var tail = head!!.prev
  if (tail!!.limit + minimumCapacity > Segment.SIZE || !tail.owner) {
    tail = tail.push(SegmentPool.take()) // Append a new empty segment to fill up.
  }
  return tail
}
```
其他api
```
fun Source.buffer(): BufferedSource = RealBufferedSource(this)
fun Sink.buffer(): BufferedSink = RealBufferedSink(this)
```

数据读取
RealBufferedSource.read(RealBufferedSink.data,xx,xx)
jvmMain/okio/RealBufferedSource.kt
```
internal actual class RealBufferedSource actual constructor(
  @JvmField actual val source: Source  //对于文件是FileInputStream
) : BufferedSource {
  override fun read(sink: Buffer, byteCount: Long): Long = commonRead(sink, byteCount)
}
```
commonMain/okio/internal/RealBufferedSource.kt
```
internal inline fun RealBufferedSource.commonRead(sink: Buffer, byteCount: Long): Long {
  require(byteCount >= 0) { "byteCount < 0: $byteCount" }
  check(!closed) { "closed" }
  // 缓冲区没有数据了
  if (buffer.size == 0L) {
    // 从输入流中读取buffer
    val read = source.read(buffer, Segment.SIZE.toLong())
    if (read == -1L) return -1L //没有内容，退出
  }
  val toRead = minOf(byteCount, buffer.size)
  // 从Buffer 中读取数据，到sink
  return buffer.read(sink, toRead)
}
```
buffer.read
jvmMain/okio/Buffer.kt
```
internal inline fun Buffer.commonRead(sink: Buffer, byteCount: Long): Long {
  var byteCount = byteCount
  require(byteCount >= 0) { "byteCount < 0: $byteCount" }
  if (size == 0L) return -1L
  if (byteCount > size) byteCount = size
  sink.write(this, byteCount) //将buffer写入sink
  return byteCount
}

override fun write(source: Buffer, byteCount: Long): Unit = commonWrite(source, byteCount)
```
commonMain/okio/internal/Buffer.kt   将source写入buffer
```
internal inline fun Buffer.commonWrite(source: Buffer, byteCount: Long) {
  var byteCount = byteCount
  while (byteCount > 0L) {
    // Is a prefix of the source's head segment all that we need to move?
    if (byteCount < source.head!!.limit - source.head!!.pos) {
      val tail = if (head != null) head!!.prev else null
      if (tail != null && tail.owner &&
        byteCount + tail.limit - (if (tail.shared) 0 else tail.pos) <= Segment.SIZE) {
        // Our existing segments are sufficient. Move bytes from source's head to our tail.
        source.head!!.writeTo(tail, byteCount.toInt())
        source.size -= byteCount
        size += byteCount
        return
      } else {
        // We're going to need another segment. Split the source's head
        // segment in two, then move the first of those two to this buffer.
        source.head = source.head!!.split(byteCount.toInt())
      }
    }

    // Remove the source's head segment and append it to our tail.
    val segmentToMove = source.head
    val movedByteCount = (segmentToMove!!.limit - segmentToMove.pos).toLong()
    source.head = segmentToMove.pop()
    if (head == null) {
      head = segmentToMove
      segmentToMove.prev = segmentToMove
      segmentToMove.next = segmentToMove.prev
    } else {
      var tail = head!!.prev
      tail = tail!!.push(segmentToMove)
      tail.compact()
    }
    source.size -= movedByteCount
    size += movedByteCount
    byteCount -= movedByteCount
  }
}
```