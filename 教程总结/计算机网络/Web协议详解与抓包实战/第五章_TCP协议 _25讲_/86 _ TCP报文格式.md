消息传输的核心要素

• 寄件人与收件人信息
  • IP 地址
  • TCP(UDP)端口
  • HTTP Host/URI 等
• 物流订单号
  • IP 序列号
  • TCP 序列号
• 物流系统需求


IP头部
  source address     destination address

UDP 头部
  source port        destination port



TCP 协议的任务

• 主机内的进程寻址    基于端口
• 创建、管理、终止连接    面向链接的
• 处理并将字节（8bit）流打包成报文段（如 IP 报文）
• 传输数据
• 保持可靠性与传输质量   
• 流控制与拥塞控制   流控制，发送接收方处理速度不一致    拥塞控制，防止整个网络出现恶性拥塞


如何标识一个连接？

• TCP 四元组（源地址，源端口，目的地址，目的端口）
  • 对于 IPv4 地址，单主机最大 TCP 连接数为 2(32+16+32+16)   次方    ip地址32+端口16  客户端+服务器=(32+16)*2
• 没有连接 ID：QUIC 协议  quic通过连接ID认为一个连接，即便地址，端口发生了变化  (便于网络扩容，扩容时发生连接断开和重连)

tcp四元组的缺点
设计时间较早，不适用移动互联网
 前往不同的区域，IP地址发生变化
 进入地铁，电梯等发生连接中断，重连，大概率IP变更
 开关飞行模式


常用端口
21  ftp
22  ssh
23  telnet
25  smtp
53  DNS
80  http
109 pop2
110 pop3
443 https
990 ftps
995 pop3s

TCP Segment 报文段
• 控制信息
  • 寻址
  • 滑动窗口
  • Flags
  • 校验和
• 数据


常用选项   TCP options 报文20个字节之后
 0 无意义  结尾
 1 无意义  对齐使用
 8  长肥网络paw

  lenth 整个长度   length=2   类型+长度2的2个字节


magicnum
老师，flag里短C和E是新增的吗，我看RFC里面以前是没有的
作者回复: 是的，2001年9月的RFC 3168中从Reserved字段中分出2位新增的


刘政伟

老师，一个TCP连接由源地址，源端口，目标地址和目标端口组成，但是一台机器上的端口是有限的（65535个），理论上的最大并发应该就是这么多吧，能达到十万或百万的并发吗？
作者回复: 如果是一台机器对多台机器，就可以达到百万并发了。比如一台服务器面对数万用户的浏览器


龍少²⁰¹⁹
> 单主机最大TCP连接数为 2^(32+16+32+16)

单主机它自己的IP是已经确定的了吧，理论最大值是不是 2^(16+32+16)
作者回复: 不一定，一台主机可以有很多IP地址




Transmission Control Protocol, Src Port: 3024, Dst Port: 22, Seq: 1, Ack: 325, Len: 0
Source Port: 3024
Destination Port: 22   端口
[Stream index: 0]
[TCP Segment Len: 0]
Sequence Number: 1    (relative sequence number)
Sequence Number (raw): 936796163
[Next Sequence Number: 1    (relative sequence number)]
Acknowledgment Number: 325    (relative ack number)
Acknowledgment number (raw): 3730664350
1000 .... = Header Length: 32 bytes (8)
Flags: 0x010 (ACK)
Window: 257
[Calculated window size: 257]
[Window size scaling factor: -1 (unknown)]
Checksum: 0x0c6f [unverified]
[Checksum Status: Unverified]
Urgent Pointer: 0
Options: (12 bytes), No-Operation (NOP), No-Operation (NOP), SACK
    TCP Option - SACK 1-325
    Kind: SACK (5)
    Length: 10
    left edge = 1 (relative)
    right edge = 325 (relative)
    [TCP SACK Count: 1]
    [D-SACK Left Edge = 1 (relative)]
    [D-SACK Right Edge = 325 (relative)]
    D-SACK Sequence
[SEQ/ACK analysis]
[Timestamps]



https://juejin.cn/post/6844904155107049486
TCP 协议细节之 TCP 协议的四个定时器

1）、超时定时器
2）、坚持定时器
3）、时间等待定时器
4）、保活定时器：服务端一般都会设置一个保活定时器，它是为了保活 TCP 连接而设计的，可以防止 TCP 连接的两端出现长时间的空闲，
  当一方出现变化或故障时，另一方没有察觉的情况。

当服务端每次收到对方的数据则重置这个定时器，如果定时器超时，则会发送弹出报文段，以此探测客户端是否在线，如果没有收到响应的话，
  那么则认为客户端已经断开连接了，因此服务端也会终止这个链接。现如今，很多的分布式系统都会使用保活定时器来检测其它节点是否在线还是已经故障
  或者其它节点也会每隔一段时间向主节点上报心跳信息以证明在线

