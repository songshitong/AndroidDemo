BufferedOutputStream.java  对于buffer的池化复用


ByteBufferUtil

https://www.jianshu.com/p/4de87ebf5104
RecyclableBufferedInputStream.java
com/bumptech/glide/load/resource/bitmap/RecyclableBufferedInputStream.java
```
  public RecyclableBufferedInputStream(@NonNull InputStream in, @NonNull ArrayPool byteArrayPool) {
    //默认  int STANDARD_BUFFER_SIZE_BYTES = 64 * 1024;
    this(in, byteArrayPool, ArrayPool.STANDARD_BUFFER_SIZE_BYTES);
  }

  RecyclableBufferedInputStream(
      @NonNull InputStream in, @NonNull ArrayPool byteArrayPool, int bufferSize) {
    super(in);
    this.byteArrayPool = byteArrayPool;
    buf = byteArrayPool.get(bufferSize, byte[].class);
  }
  
 public synchronized int read() throws IOException {
    // Use local refs since buf and in may be invalidated by an
    // unsynchronized close()
    byte[] localBuf = buf;
    InputStream localIn = in;
   ...
    // Are there buffered bytes available?
    if (pos >= count && fillbuf(localIn, localBuf) == -1) {
      // no, fill buffer
      return -1;
    }
    ...
    // Did filling the buffer fail with -1 (EOF)?
    if (count - pos > 0) {
      return localBuf[pos++] & 0xFF;
    }
    return -1;
  }  
  
 private int fillbuf(InputStream localIn, byte[] localBuf) throws IOException {
    if (markpos == -1 || pos - markpos >= marklimit) {
      // Mark position not put or exceeded readlimit
      int result = localIn.read(localBuf);
      if (result > 0) {
        markpos = -1;
        pos = 0;
        count = result;
      }
      return result;
    }
    // Added count == localBuf.length so that we do not immediately double the buffer size before
    // reading any data
    // when marklimit > localBuf.length. Instead, we will double the buffer size only after
    // reading the initial
    // localBuf worth of data without finding what we're looking for in the stream. This allows
    // us to put a
    // relatively small initial buffer size and a large marklimit for safety without causing an
    // allocation each time
    // read is called.
    if (markpos == 0 && marklimit > localBuf.length && count == localBuf.length) {
      // Increase buffer size to accommodate the readlimit
      //扩容为2倍
      int newLength = localBuf.length * 2;
      if (newLength > marklimit) {
        newLength = marklimit;
      }
      byte[] newbuf = byteArrayPool.get(newLength, byte[].class);
      System.arraycopy(localBuf, 0, newbuf, 0, localBuf.length);
      byte[] oldbuf = localBuf;
      // Reassign buf, which will invalidate any local references
      // FIXME: what if buf was null?
      localBuf = buf = newbuf;
      byteArrayPool.put(oldbuf);
    } else if (markpos > 0) {
      System.arraycopy(localBuf, markpos, localBuf, 0, localBuf.length - markpos);
    }
    // Set the new position and mark position
    pos -= markpos;
    count = markpos = 0;
    int bytesread = localIn.read(localBuf, pos, localBuf.length - pos);
    count = bytesread <= 0 ? pos : pos + bytesread;
    return bytesread;
  } 
```
获得一个初始的64kb大小的缓冲区。而这个缓冲区很明显对于加载大图片时候不够用。但是别忘了Glide是读了5遍图片数据流。
读取第一次的的时候这个inputstream，每一次读到了极限值的时候，发现没有读完，将会把整个读取极限扩容*2，最后一定足够读完整个流。

我刚才说的5遍读取数据流又是哪5次呢？这里指出来：
//自己debug了一下 一张jpg read函数读了7次
1.获取jpeg图片的信息头exif，这里设计很精妙，每一次读1个字节，一遍解析，一遍获取到自己想要信息。
 只是调用了数次的inputstream.read()方法。根本不需要读完整个流，加快了获取速度。
com/bumptech/glide/load/resource/bitmap/Downsampler.java
```
int orientation = imageReader.getImageOrientation();
```
2.获取图片的宽高，但是这个时候设置了options.inJustDecodeBounds = true;图片压根没有存到内存里面，仅仅只是获取了图片一些options。
com/bumptech/glide/load/resource/bitmap/Downsampler.java
```
int[] sourceDimensions = getDimensions(imageReader, options, callbacks, bitmapPool);
    int sourceWidth = sourceDimensions[0];
    int sourceHeight = sourceDimensions[1];
```
3.根据宽高创建出原始的Bitmap
```
Bitmap downsampled = decodeStream(imageReader, options, callbacks, bitmapPool);
```
4.是在第三点的时候，判断图片类型，再读了一次头信息
5.根据绘画出来的图片通过画布根据放置方向，方正再一次绘画一次。
```
 Bitmap rotated = null;
            if (downsampled != null) {
                rotated = TransformationUtils.rotateImageExif(downsampled, pool, orientation);

                if (!downsampled.equals(rotated) && !pool.put(downsampled)) {
                    downsampled.recycle();
                }
            }
```
为什么Glide显示的比Picasso慢，主要因为Glide读了5次流，绘画了2次bitmap，而Picasso只是读了一次流，绘画了一次bitmap