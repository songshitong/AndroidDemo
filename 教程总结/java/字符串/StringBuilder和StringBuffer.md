参考https://github.com/xfhy/Android-Notes/blob/master/Blogs/Java/%E5%9F%BA%E7%A1%80/String.md

1.String,StringBuilder,StringBuffer的区别
StringBuilder和StringBuffer的核心代码逻辑都是一样的,都在父类AbstractStringBuilder里面,父类维护着一个char类型的数组,
需要操作字符串数据的时候其实就是在操作这个数组里面的数据.
String里面也维护着一个char类型的数组,只不过是final修饰的,任何change操作都会新创建String,而不是在原来的基础上修改.

String是不可变的字符序列,StringBuilder和StringBuffer是可变的字符序列.
StringBuffer是线程安全的,
StringBuilder是线程不安全的.
速度上: StringBuilder > StringBuffer > String


分析
  StringBuffer和StringBuilder都是继承了AbstractStringBuilder
  AbstractStringBuilder 同样通过char数组保存数据，后面改为byte数组
  
  StringBuffer 线程安全的实现  使用synchronized给方法加锁

  默认容器大小 16 
  public StringBuffer() {
          super(16);
      }
  public StringBuilder() {
          super(16);
      }
      
  
  追加方法
  public AbstractStringBuilder append(String str) {
          if (str == null)
              return appendNull();
          int len = str.length();
          ///判断是否需要扩容，字符数达到容器大小时进行扩容  扩容时复制原内容到新数组中
          ensureCapacityInternal(count + len);
          ///将str复制到value
          ///0,len 需要复制的开始和结尾    value复制到的地方  count复制到目标地的开始位置
          str.getChars(0, len, value, count);
          ///当前已有的字符数，不是容器大小
          count += len;
          return this;
      }        
 
 
  扩容
     private void ensureCapacityInternal(int minimumCapacity) {
           // overflow-conscious code
           if (minimumCapacity - value.length > 0) {
               value = Arrays.copyOf(value,
                       newCapacity(minimumCapacity));
           }
       }
     //获取容器新的大小
     private int newCapacity(int minCapacity) {
             // overflow-conscious code
             //扩容2倍+2
             //小知识点:这里可能会溢出,溢出后是负数哈,注意
             int newCapacity = (value.length << 1) + 2;
             if (newCapacity - minCapacity < 0) {
                 newCapacity = minCapacity;
             }
             ///MAX_ARRAY_SIZE大小时Integer.MAX_VALUE - 8
             return (newCapacity <= 0 || MAX_ARRAY_SIZE - newCapacity < 0)
                 ? hugeCapacity(minCapacity)
                 : newCapacity;
         }
     ///newCapacity存在溢出的情况
     ///newCapacity <= 0 或者  MAX_ARRAY_SIZE < newCapacity
     private int hugeCapacity(int minCapacity) {
             if (Integer.MAX_VALUE - minCapacity < 0) { // overflow
                 throw new OutOfMemoryError();
             }
             ///newCapacity <= 0  返回MAX_ARRAY_SIZE
             ///MAX_ARRAY_SIZE < newCapacity  返回newCapacity
             return (minCapacity > MAX_ARRAY_SIZE)
                 ? minCapacity : MAX_ARRAY_SIZE;
         }    