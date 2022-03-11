rfc 768
UDP   User Datagram Protocol  用户数据报协议
   一个简单的面向数据报的通信协议
   在IP协议之上，位于传输层

This User Datagram  Protocol  (UDP)  is  defined  to  make  available  a
datagram   mode  of  packet-switched   computer   communication  in  the
environment  of  an  interconnected  set  of  computer  networks.   This
protocol  assumes  that the Internet  Protocol  (IP)  [1] is used as the
underlying protocol.

This protocol  provides  a procedure  for application  programs  to send
messages  to other programs  with a minimum  of protocol mechanism    udp提供了最小的传输机制


TCP与UDP区别总结：   https://blog.csdn.net/ls5718/article/details/52141571
1、TCP面向连接（如打电话要先拨号建立连接）;UDP是无连接的，即发送数据之前不需要建立连接
2、TCP提供可靠的服务。也就是说，通过TCP连接传送的数据，无差错，不丢失，不重复，且按序到达;UDP尽最大努力交付，即不保证可靠交付
3、TCP面向字节流，实际上是TCP把数据看成一连串无结构的字节流;UDP是面向报文的 
UDP没有拥塞控制，因此网络出现拥塞不会使源主机的发送速率降低（对实时应用很有用，如IP电话，实时视频会议等）
4、每一条TCP连接只能是点到点的;UDP支持一对一，一对多，多对一和多对多的交互通信
5、TCP首部开销20字节;UDP的首部开销小，只有8个字节
6、TCP的逻辑通信信道是全双工的可靠信道，UDP则是不可靠信道
7.udp没有分片，很容易造成IP的MTU分片


如何看待TCP面向字节，udp面向报文    https://www.nowcoder.com/questionTerminal/b44462e37ee74efc8cdbaf52aed7692b
UDP是面向报文的：发送方的UDP对应用程序交下来的报文，在添加了首部之后就向下交付，UDP对应用层交付下来的报文即不合并也不拆分，
而是保留这些报文的边界，应用层交给UDP多长的报文，UDP就照样发送，即一次发送一个报文，接收方UDP对下方交上来的UDP用户数据报，
在去除首部之后就原封不动的交付给上层的应用程序，一次交付一个完整报文，所以是UDP是面向报文的

TCP是面向字节的：发送方TCP对应用程序交下来的报文数据块，视为无结构的字节流（无边界约束，可拆分/合并），但维持各字节流顺序
（相对顺序没有变），TCP发送方有一个发送缓冲区，当应用程序传输的数据块太长，TCP就可以把它划分端一些再传输，
如果应用程序一次只传输一个字节，那么TCP可以等待积累足够多的字节后再构成报文端发送出去，所以TCP的面向字节的


udp应用
quic
包总量较少的通信(DNS,SNMP)
视频、音频等多媒体通信(即时通信)
限定于LAN等特定网络中的应用通信
广播通信(广播，多播)
物联网   Google 旗下的 Nest 建立了 Thread Group，推出了物联网通信协议 Thread，该协议就是基于 UDP 的。
移动通信领域 在 4G 网络里，通过移动通信传输数据面对的协议 GTP-U 就是基于 UDP 的


王者荣耀关于udp的使用
https://www.easemob.com/news/868

UDP实现的可靠传输
UDT（UDP-based Data Transfer Protocol）