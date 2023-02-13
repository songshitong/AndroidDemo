



java 11
默认buffer为8192byte
write时会先写到缓冲区，执行flush时将buffer写入outStream
通过缓冲区，减少io的写
```
public class BufferedOutputStream extends FilterOutputStream {
  protected byte[] buf;
  protected int count;

  public BufferedOutputStream(OutputStream out) {
    this(out, 8192);
  }
 ...

  private void flushBuffer() throws IOException {
    if (this.count > 0) {
      this.out.write(this.buf, 0, this.count);
      this.count = 0;
    }

  }

  public synchronized void write(int b) throws IOException {
    if (this.count >= this.buf.length) {
      this.flushBuffer();
    }

    this.buf[this.count++] = (byte)b;
  }
  
 ...
  public synchronized void flush() throws IOException {
    this.flushBuffer();
    this.out.flush();
  }
}
```




BufferedInputStream.class
默认缓冲8192
通过缓冲区，减少io的读
```
  public BufferedInputStream(InputStream in) {
    this(in, DEFAULT_BUFFER_SIZE);//8192
  }
  

public synchronized int read() throws IOException {
    //当前位置大于等于buffer的大小,buffer读完了
    if (this.pos >= this.count) {
      //读取新的buffer大小
      this.fill();
      //没有新的内容了，返回
      if (this.pos >= this.count) {
        return -1;
      }
    }
    return this.getBufIfOpen()[this.pos++] & 255; //从buffer读取一个字节
  } 
  

 private void fill() throws IOException {
    byte[] buffer = this.getBufIfOpen();
    int nsz;
    if (this.markpos < 0) {
      //第一次执行
      this.pos = 0;
    } else if (this.pos >= buffer.length) {
      if (this.markpos > 0) {
        //设置了mark   mark->pos的数据，不重复读了
        nsz = this.pos - this.markpos;
        //将buffer从mar->pos的数据 拷贝到buffer的0->(nsz+0)
        System.arraycopy(buffer, this.markpos, buffer, 0, nsz);//最后一个参数是长度
        this.pos = nsz; //当前数据在新buffer的位置
        this.markpos = 0; //拷贝完，只保留marklimit  markpos归0
      } else if (buffer.length >= this.marklimit) {
        //buffer读完后，没有设置mark，或者 可读的limit在buffer内
        //归0操作
        this.markpos = -1;
        this.pos = 0;
      } else {
        //markLimit 超过buffer长度
        if (buffer.length >= MAX_BUFFER_SIZE) {
          throw new OutOfMemoryError("Required array size too large");
        }

        nsz = this.pos <= MAX_BUFFER_SIZE - this.pos ? this.pos * 2 : MAX_BUFFER_SIZE;
        if (nsz > this.marklimit) {
          nsz = this.marklimit;
        }
        //buffer扩容，pos的2倍也就是buf的2倍或者marklimit 取最小 
        byte[] nbuf = new byte[nsz];
        //将buffer拷贝到nbuf
        System.arraycopy(buffer, 0, nbuf, 0, this.pos);
        if (!U.compareAndSetObject(this, BUF_OFFSET, buffer, nbuf)) {
          throw new IOException("Stream closed");
        }

        buffer = nbuf;
      }
    }  
    
    //1 第一次读 pos=0，读一个buffer的大小 count=buffer.length
    //2 如果设置了mark，pos>0，填充buffer的pos->length的长度
    //3 需要扩容，与2类似，将buffer剩余填充 pos->length
    this.count = this.pos;
    nsz = this.getInIfOpen().read(buffer, this.pos, buffer.length - this.pos);
    if (nsz > 0) {
      this.count = nsz + this.pos;
    }
 }  
 
 public synchronized void mark(int readlimit) {
    //设置倒带可以读的大小
    this.marklimit = readlimit;
    //记录当前位置
    this.markpos = this.pos;
  }

  public synchronized void reset() throws IOException {
    this.getBufIfOpen();
    if (this.markpos < 0) {
      throw new IOException("Resetting to invalid mark");
    } else {
      this.pos = this.markpos;
    }
  }  
```