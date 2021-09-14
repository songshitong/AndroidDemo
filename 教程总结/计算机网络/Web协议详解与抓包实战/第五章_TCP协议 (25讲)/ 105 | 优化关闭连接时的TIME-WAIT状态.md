Time-wait 一般存在2min，这对于同时处理大量TCP链接的服务器是一个非常大的负担

TIME-WAIT状态过短或者不存在会怎么样？
• MSL(Maximum Segment Lifetime)
   • 报文最大生存时间
• 维持 2MSL 时长的 TIME-WAIT 状态
   • 保证至少一次报文的往返时间内端口是不可复用  2=往返

报文seq3丢失了，加入msl很端，端口关闭后又重新复用了，假设seq3不是丢失了而是在网络中延迟了，会造成接收端数据错乱的问题
TIME-wait是有保护作用的，延迟发过来的数据不会扰乱新连接
TIME-WAIT一般是2MSL的时间



linux下TIME_WAIT优化：tcp_tw_reuse
• net.ipv4.tcp_tw_reuse = 1
   • 开启后，作为客户端时新连接可以使用仍然处于 TIME-WAIT 状态的端口    
   • 由于 timestamp 的存在，操作系 统可以拒绝迟到的报文
     • net.ipv4.tcp_timestamps = 1

TIME_WAIT 优化
• net.ipv4.tcp_tw_recycle = 0
   • 开启后，同时作为客户端和服务器都可以使用 TIME-WAIT 状态的端口
   • 不安全，无法避免报文延迟、重复等给新连接造成混乱
• net.ipv4.tcp_max_tw_buckets = 262144
   • time_wait 状态连接的最大数量
   • 超出后直接关闭连接


RST 复位报文   遇到异常情况使用rst关闭连接
  进程关掉了，严重的异常，RST置为1来表示复位报文

  tcp报文flag Reset 为1




Geek_007
老师，内核参数tw_reuse和socket编程中的socket参数SO_REUSEPORT是一样的效果么？
作者回复: 你是说tcp_tw_reuse吗？这个参数是用于新建连接时，复用TIME_WAIT状态端口而设的。而SO_REUSEPORT是用于多进程监听同一端口，在建立连接时使用的，你可以参考《Nginx核心知识100讲》第122课。


加载中……
老师好，有几个问题不太明白：
1、如何根据timestamps判断某个segment不是本次连接的？
通过比较大小不能判断吧
2、一个TCP连接建立后，对于TCP的两个端点是不是没有那个是服务端那个是客户端之分？
因为连接建立好了，都可以向对方发送segment
3、为什么客户端复用time_wait端口风险小，服务器端复用就会无法避免混乱
4、如果出现数据包混乱(比如复用time_wait端口导致的)，TCP协议会把新的连接直接发送RST报文么，还是怎么处理？
作者回复: 1、timestamps扩展包含两个字段，发送时间和ACK时间，相减得到RTT，可以通过RTT判断是否过延迟报文。
2、是的，建立过程中有，建立连接完成后不作区分。
3、概率上相差很大，TCP四元组中作为客户端有65535种可能，且自身知道打开了reuse功能可控，但作为服务器遇到同ip同端口客户端是不可控的。
4、RST报文




