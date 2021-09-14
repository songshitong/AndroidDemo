当要求IP层传输超过MTU大小报文时，IP将报文拆分为多个报文传输

IP 报文格式
• IHL：头部长度，单位字  header length  一个字表示4个字节
• TL：总长度，单位字节  包含data的总长度
• Id：分片标识  id,flags,fo用来做分片的  没有分片的情况下没有意义
• Flags：分片控制
   • DF 为1：不能分片
   • MF 为1：中间分片
• FO：分片内偏移，单位 8 字节  分片后是无序的报文，根据fo组装起来
• TTL：路由器跳数生存期  每经过一个路由减1，到0还没有传输到目标路由进行丢弃 路由跟踪工具就是根据TTL实现的，icmp中会详细介绍
• Protocol：承载协议  IP层上层使用的什么协议
• HC：校验和

df  do-not-fragment

Internet Protocol Version 4, Src: 192.168.22.53, Dst: 116.62.160.193
   0100 .... = Version: 4         ipv4
   .... 0101 = Header Length: 20 bytes (5)   101=5 单位4字节  5*4=20
   Differentiated Services Field: 0x00 (DSCP: CS0, ECN: Not-ECT)
   Total Length: 52  ip头20字节 tcp头20字节 tcp的option 12字节
   Identification: 0x486e (18542)
   Flags: 0x40, Don't fragment
   Fragment Offset: 0
   Time to Live: 128
   Protocol: TCP (6)    承载协议tcp
   Header Checksum: 0xc678 [validation disabled]
   [Header checksum status: Unverified]
   Source Address: 192.168.22.53
   Destination Address: 116.62.160.193
Transmission Control Protocol, Src Port: 3024, Dst Port: 22, Seq: 1, Ack: 325, Len: 0
      Source Port: 3024
      Destination Port: 22
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
      [SEQ/ACK analysis]
      [Timestamps]



MTU（Maximum Transmission Unit）分片    Transmission 传送
• MTU 最大传输单元（ RFC791 ：>=576 字节）
• ping 命令
  • -f：设置 DF 标志位为 1   不能分片
  • -l：指定负载中的数据长度    字节    mac -s 默认56字节
使用ping验证mtu  
ping www.baidu.com -f -l 1000 
ping www.baidu.com -f -l 2000
  需要拆分数据包但是设置DF



常见网络 MTU
以太网 1500


可能出现多次分片
第5部分讲到tcp在避免网络层的分片，因为网络层分片存在很多问题
 问题1 可能出现多次分片，效率低效
   a向b 发送12000分片
     本地网络MTU是3300 分为4个报文  最后 一个2100
     中间网络MTU是1300 分为11个报文  前三个分为9个报文，最后一个分为2个报文
     传到B的网络时MTU是3300  任然是11个报文


IP 分片示例
• 分片主体
  • 源主机
  • 路由器  发现下一个MTU小于当前报文就可能分片
• 重组主体  重组的只有目的主机
  • 目的主机

分片实例 3280+20头部=3300 
offset 单位是8个字节  410=3280/8
抓包 host 10.2.8.2 本地    发送一个很大的数据需要被分片
ping 10.2.8.2 -l 8000    为什么ping可以测试分片  ping使用的icmp工作在IP协议之上，可以验证分片

icmp 报文
  identification: 0x0e28  分片表示
  flags  More Fragments: Set 置为1   设为分片
         Fragment offset: 0    第一个分片
  data 是 a-z的反复填充

  identification: 0x0e28
  flags  More Fragments:  Not set  最后一个分片
         Fragment offset: 925    



孜孜
既然IP会分片，那么tcp不分段可不可以？完全让IP搞定？如果可以这样做，如果一个IP分片丢失，会发生什么？
作者回复: 如果一个IP分片丢失，整个IP报文的所有分片都得重传。
IP分片性能低下，网络设备只负责分片不会重组，只有接收主机才会重组


tongmin_tsai
老师，假如发送方发送了3000字节报文，那么会分2个包发送，那接收端收到的报文，也是按照mtu分割收到的，那报文在tcp层会自动组装这2个报文吗？还是直接分别把这2个报文放操作系统缓冲区？如果直接放缓冲区，那报文的组装由应用层的应用程序自己来接收，自己组装吗？
作者回复: 不是在TCP层自动组装，而是在IP层自动组装。由IP层分片的，必须要由IP层组装。IP层由操作系统实现，是放在操作系统缓冲区实现的。

Ping
老师我mac用ping -f的时候报错不允许'-f'，可以改设置解决吗？
作者回复: 不好意思，我一直用windows，没用过mac系统，不清楚mac的ping程序是否参数不同，也没法帮你验证哈。你可以查查看MAC上如何禁用IP分片
李文彬
没有权限，加sudo


一周思进
什么情况下需要设置不允许分片，是不是都允许设置分片就行，这样数据包过中间小的MTU，也就不怕丢包了？
作者回复: 测试路径最大MTU时会设置不允许分片


kissingers
老师，MTU 概念中T是指发送，那么实际中比如以太网1500字节，那么能接收的包的大小也受这个值影响吗？
作者回复: 是的


加载中……
陶老师好，我在用ping 8.8.8.8 -s 8000的时候，这边是mac， -s是制定报文大小的。
在wireshark中看到是分成了几个fragment，控制台显示
ping 8.8.8.8 -s 8000
PING 8.8.8.8 (8.8.8.8): 8000 data bytes
Request timeout for icmp_seq 0
Request timeout for icmp_seq 1
Request timeout for icmp_seq 2
Request timeout for icmp_seq 3
在wireshark中(没有设置任何过滤器)也没看到错误的报文
是不是因为 中间路由器或什么设备直接把包丢弃了，并且也没有返回任何报文。所以我这边抓包看不到任何响应？
作者回复: 8.8.8.8存在主机吗？内核要构造数据链路层，需要MAC地址，它会发广播ARP去问有没有8.8.8.8，如果没有，自然构造不出来。参见112课。