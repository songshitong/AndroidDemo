icmp是ip协议的辅助协议，IP协议聚焦性能，诸如传递错误和和其他信息的功能就由ICMP承担了

ICMP：Internet Control Message Protocol
• RFC792
• IP 助手
   • 告知错误
   • 传递信息
图片中A向B传递消息，R3出现问题，通过ICMP向A返回错误信息，该信息不会被中间R2和R1处理，因为ICMP是IP之上，R3明确的指出报文目的是A


ICMP协议格式
• 承载在 IP 之上
• 组成字段
  • 类型
  • 子类型   code是针对type的子类型  有些type没有子类型0，8   ping的请求和相应
  • 校验和


ICMPv4 报文类型     ipv4的ICMP协议
• 错误报文    路由跟踪工具遇到 3，11
    • 3：目的地不可达
    • 4：发生拥塞，要求发送方降低速率
    • 5：告诉主机更好的网络路径
    • 11：路径超出 TTL 限制
    • 12：其他问题

• 信息报文 
    • 0：连通性测试中的响应
    • 8：连通性测试中的请求
    • 9：路由器通告其能力
    • 10：路由器通知请求
    • 13：时间戳请求
    • 14：时间戳应答
    • 17：掩码请求
    • 18：掩码应答
    • 30：Traceroute



目的地不可达报文：Type=3  包含了IP头部
• 常用子类型 Code
    • 0：网络不可达
    • 1：主机不可达
    • 2：协议不可达
    • 3：端口不可达
    • 4：要分片但 DF 为1     ping 设置了 -f
    • 10：不允许向特定主机通信
    • 13：管理受禁


Echo 与 Echo Reply 报文
• ping 联通性测试
  type 0或8


TTL 超限：Type=11   默认64/128
• traceroute/tracert
  每经过一个路由器-1，ttl超限的路由器将发送icmp报文  返回时协议出错路由的IP
  traceroute原理 将ttl设为1 发现R1  设为2 发现R2 一点点增加TTL 这样A到B所经过的路由的IP 就列出来了


抓包 icmp or udp    icmp还会发送udp协议
traceroute  www.taohui.pub

traceroute --version
Version 1.4a12+Darwin
自己的报文 icmp的请求是udp报文
11	3.022366	10.24.61.65	129.28.62.166	UDP	66		62893 → 33435 Len=24
Time to Live: 1
[Expert Info (Note/Sequence): "Time To Live" only 1]


ttl设为1的报文
12	3.024732	10.24.61.254	10.24.61.65	ICMP	70		Time-to-live exceeded (Time to live exceeded in transit)
Internet Protocol Version 4, Src: 10.24.61.254, Dst: 10.24.61.65
    0100 .... = Version: 4
    .... 0101 = Header Length: 20 bytes (5)
    Differentiated Services Field: 0xc0 (DSCP: CS6, ECN: Not-ECT)
    Total Length: 56
    Identification: 0xd34c (54092)
    Flags: 0x00
    Fragment Offset: 0
    Time to Live: 255
    Protocol: ICMP (1)    协议时ICMP
    Header Checksum: 0x5849 [validation disabled]
    [Header checksum status: Unverified]
    Source Address: 10.24.61.254
    Destination Address: 10.24.61.65
Internet Control Message Protocol
    Type: 11 (Time-to-live exceeded)     ttl超限的报文
    Code: 0 (Time to live exceeded in transit)
    Checksum: 0xfc4c [correct]
    [Checksum Status: Good]
    Unused: 00000000
    Internet Protocol Version 4, Src: 10.24.61.65, Dst: 129.28.62.166
    0100 .... = Version: 4
    .... 0101 = Header Length: 20 bytes (5)
    Differentiated Services Field: 0x00 (DSCP: CS0, ECN: Not-ECT)
    Total Length: 52
    Identification: 0xf5ae (62894)
    Flags: 0x00
    Fragment Offset: 0
    Time to Live: 1             ttl设为1
    Protocol: UDP (17)
    Header Checksum: 0xbcef [validation disabled]
    [Header checksum status: Unverified]
    Source Address: 10.24.61.65         原先的IP报文的请求又写回来了 
    Destination Address: 129.28.62.166
    User Datagram Protocol, Src Port: 62893, Dst Port: 33435

老师的报文  发现第一个路由后还会发送一个NBNS的查询协议 其实是一个udp协议
   DST Port:137  目的端口是 137  但是目标10.2.8.1不支持137端口，没有开放
   10.2.8.1 会回复一个报文 ICMP  Destination unreachable(Port unreachable)
    icmp 层
       type:3  code:3  目的不可达

NBNS 协议
NetBIOS，Network Basic Input/Output System的缩写，一般指用于局域网通信的一套API，相关RFC文档包括 RFC 1001, RFC 1002.
RFC 1001主要对NetBIOS及相关协议和服务进行解释说明，RFC 1002给出了相关协议和服务的数据组包格式。
NBNS 简介
NBNS是NetBIOS name service的缩写，是NetBIOS的命名服务，用于将NetBIOS名称映射到IP地址上，是NetBIOS-over-TCP(NBT)协议族的一份子。
NBNS是动态DNS的一种，Microsoft的NBNS实现称为WINS。路由器可以通过发送NBNS状态请求以获取设备名，windows PC 接收到后通过
WINS或将本地缓存发送命名信息给路由器。
https://baike.baidu.com/item/NBNS/96581?fr=aladdin
NetBIOS是一个网络协议，在上世纪80年代早期由IBM和Sytec联合开发，用于所谓的PC-Network。虽然公开发表的文档很少，协议的API却成
为了事实上的标准。
随着PC-Network被令牌环和以太网取代，NetBIOS也应该退出历史舞台。但是，由于很多软件使用了NetBIOS的API，所以NetBIOS被适配到
了各种其他的协议上，比如IPX/SPX和TCP/IP。

ping www.taohui.pub
30	13.002394	10.24.61.65	129.28.62.166	ICMP	98		Echo (ping) request  id=0x2476, seq=0/0, ttl=64 (reply in 31)
Internet Control Message Protocol
    Type: 8 (Echo (ping) request)
    Code: 0
    Checksum: 0x6d11 [correct]
31	13.043477	129.28.62.166	10.24.61.65	ICMP	98		Echo (ping) reply    id=0x2476, seq=0/0, ttl=51 (request in 30)
Internet Control Message Protocol
    Type: 0 (Echo (ping) reply)
    Code: 0
    Checksum: 0x7511 [correct]



ray
老师好，
您在前面的章节提到IP协议是不支持可靠传输的，这节课有提到，如果路由器接收到的IP packet发生问题，它会透过ICMP协议告知原始路由器此IP packet出现问题。

我的问题是，告知原始路由器传输封包发生的问题意义何在，既然是不可靠传输，为什么还需要告知传输过程中发生什么问题呢？

谢谢老师的解答^^
作者回复: 所谓不可靠，是指发出后，如果接收主机没收到，IP协议是不管的。但如果是传输路径上出现了错误，那么告知路由器或者发送方就有意义。
比如，TCP的MSS就依赖IP包上的DF标志设为1，它是告诉传输路径上的所有路由器，如果大于MTU要拆包，就要告知发送端，因为IP拆包的代价非常大，参见115课。


ray
老师好，
根据前一个问题所述，我是不是可以将IP协议理解为发送端送出packet后，不会管接收方是否收到，但是会关注在发送"过程中"packet出现的问题。如果出现问题，路由器会负责告至发送端这次传输出现什么问题，由发送端来决定应该做什么处理，也可以不做处理，任由packet丢失。这样讲是否正确呢？

谢谢老师的解答^^
作者回复: 正确！

