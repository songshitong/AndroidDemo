todo
https://www.jianshu.com/p/2fff6fe403dd



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