如何测量 RTT？ Round-Trip Time  往返时延
  简单方法 ack tcb - syn tcb


如何在重传下有效测量 RTT？
• RTT 测量的第 2 种方法
  • 发送时间
  • 数据包中 Timestamp 选项的回显时间
 
 a,b是两种重发的场景   重发的时机与收到ack的时机


RTO（ Retransmission TimeOut ）应当设多大？
 • RTO 应当略大于 RTT     rtt经常变化


RTO 应当更平滑
• 平滑 RTO：RFC793，降低瞬时变化
  • SRTT （smoothed round-trip time） = ( α * SRTT ) + ((1 - α) * RTT)
     • α 从 0到 1（RFC 推荐 0.9），越大越平滑
  • RTO = min[ UBOUND, max[ LBOUND, (β * SRTT) ] ]
     • 如 UBOUND为1分钟，LBOUND为 1 秒钟， β从 1.3 到 2 之间
  • 不适用于 RTT 波动大（方差大）的场景   大部分操作系统并没使用这种方案



追踪 RTT 方差
• RFC6298（RFC2988），其中α = 1/8， β = 1/4，K = 4，G 为最小时间颗粒：  数值通过大量统计数据得来的
  • 首次计算 RTO，R为第 1 次测量出的 RTT
     • SRTT（smoothed round-trip time） = R
     • RTTVAR（round-trip time variation） = R/2
     • RTO = SRTT + max (G, K*RTTVAR)
  • 后续计算 RTO，R’为最新测量出的 RTT
     • SRTT = (1 - α) * SRTT + α * R’
     • RTTVAR = (1 - β) * RTTVAR + β * |SRTT - R’|
     • RTO = SRTT + max (G, K*RTTVAR)




一步

有关 TCP 的 timestamp 特性 老师没有详细讲解，不是很明白timestamp值是怎么传递和计算的
作者回复: options中可以携带数据发送时间戳与ACK接收时间戳。抓包可以参看：https://www.cloudshark.org/captures/7e751e01085a?filter=frame.number==4
cloudshark类似于wireshark的web版，但是收费的


一步

RTT 这个值的计算是只针对 TCP 吗？ 对于 UDP 和 HTTP 有 RTT 这个概念吗？
作者回复: 严格来说RTT针对的是网络特性，不局限在TCP，需要测量报文往返时都会用到。HTTP/1协议只站在应用层，对它来说没必要引入RTT概念，但HTTP/3开始关注传输层和网络层，它可能引入RTT。


hellojd
课程里的rtt,感觉和socket里的sockettimeout 是对应的
作者回复: 前者是内核层的参数，与后者API应用层的概念完全不同

