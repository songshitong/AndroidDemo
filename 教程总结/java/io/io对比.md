






NIO/14.Java NIO vs. IO .md
IO	                             NIO
Stream oriented	            Buffer oriented
Blocking IO	                Non blocking IO
                            Selectors
IO是面向流的，默认没有缓冲区，处理数据不能前后移动
IO是阻塞式的，当一个线程调用 read() 或 write() 时，该线程将被阻塞，直到有一些数据要读取，或者数据被完全写入。线程在此期间不能做任何其他事情

NIO是面向缓冲区的，处理数据可以前后移动
NIO 的非阻塞模式使线程可以请求从通道读取数据，如果当前没有数据可用，则只获取当前可用的内容，或者根本不获取。线程可以继续执行其他操作，
  而不是在数据可供读取之前一直处于阻塞状态
NIO 的Selectors允许单个线程监视多个输入通道，使单个线程可以轻松管理多个通道，减少线程数量和上下文切换
  Selectors在Linux基于epoll机制