TCP 的 Keep-Alive 功能    长连接，怎么关闭长连接
• Linux 的 tcp keepalive
    • 发送心跳周期
       • Linux: net.ipv4.tcp_keepalive_time = 7200  2个小时，超时开启keepalive检查功能
    • 探测包发送间隔   没有收到应答，间隔75s，再次探测
       • net.ipv4.tcp_keepalive_intvl = 75
    • 探测包重试次数    
       • net.ipv4.tcp_keepalive_probes = 9


违反分层原则的校验和   
• 对关键头部数据（12字节）+TCP 数据执行校验和计算
 • 计算中假定 checksum 为0   默认是累加算法，tcp option可以改变算法，位数
  TCP header中check-sum校验 校验内容：
   tcp header,tcp data
   ip层的部分内容   违反分层原则，校验了其他层的内容     如果发现不是发给自己的，可以通过校验和更快进行处理
      ip source   ip destination    ip protcol



应用调整 TCP 发送数据的时机   PSH
  假设发送10M数据，被拆分为多个segment，最大mss,最后一个报文tcp头部psh置为1，表示接受到这个报文后，请接受方尽快把缓冲区的内容
    交给应用层使用，而不是等待缓冲区达到多少字节才开始处理   一般应用调用write方法就是完整的请求，需要接收方处理

紧急处理数据  URG   urgent
  telnet中敲击很多字符，网络中可能出现拥塞，此时按ctrl+c,应该终止telnet链接，此时使用urg标志位，接收方会优先处理
  ftp中，下载一个大文件，但是临时取消了，此时urg应该置为1


加载中……
老师好，请教个问题。
对于TCP连接来说，是不是没有长连接和短连接之分？还是说也是有个参数可以控制这个TCP是长连接还是短连接
作者回复: TCP没有长短连接的概念，这是HTTP/1的概念


刘政伟
老师，系统中的net.ipv4.tcp_keepalive_time和nginx中的keepalive_timeout同时开启的话，以哪个为断开连接依据呀？
作者回复: 你是说keepalive_timeout指令吗？那个是http/1.1中的keepalive，是多个应用层的请求复用同一个TCP连接时的最大间隔时间。而tcp_keepalive_time是TCP管理传输层空闲连接的方式，它对应着listen指令后的so_keepalive选项，它是通过setsockopt设置到内核的，优先级更高。


holyman
老师有个问题，有些不太理解，既然TCP没有长短连接的概念，那么这个keep-alive描述的含义是什么，2h不断开，难道不是说明这个连接不是长连接吗？
作者回复: 这是为了处理故障定义的。每个长期空闲的TCP连接都需要消耗服务器内存、端口等资源，由于服务器是一对多的处理方式，一旦某个客户端网络断开，服务器就会永远无法释放socket相关资源了。因此才定义了keepalive保活机制。


ray
老师好，请问
1. tcp预设的连线时间是不是都是依据"服务器"的net.ipv4.tcp_keepalive_time的设定来决定，与客户端的net.ipv4.tcp_keepalive_time无关？

2. 如果服务器超出net.ipv4.tcp_keepalive_time的设定时间没有发送任何报文，则依据服务器配置的net.ipv4.tcp_keepalive_intvl和net.ipv4.tcp_keepalive_probes这两个设定来决定是否继续维持该连接？

3. 如果client在server的net.ipv4.tcp_keepalive_time设定到期之前发送FIN或是RST则会提早端闭连接？


谢谢老师的解答^^
作者回复: 1、keepalive检测是双向的，两边都可以检测。
2、对。
3、会。



ray
老师好，
您在我的上个发问中提到net.ipv4.tcp_keepalive_time检测是双向的。这是不是意味着，我可以在自己的电脑配置net.ipv4.tcp_keepalive_time，当我用自己的电脑作为client和server通讯时，就会依据我自己设定net.ipv4.tcp_keepalive_time来决定我的tcp keep alive time。如果我的tcp keep alive time比server小，我就会先发出探测包。
反过来说，如果server设定的tcp keep alive time比client大，探测包就会由server先发送。

我这样说是不是对的呢？

谢谢老师的解答^^
作者回复: 对的




