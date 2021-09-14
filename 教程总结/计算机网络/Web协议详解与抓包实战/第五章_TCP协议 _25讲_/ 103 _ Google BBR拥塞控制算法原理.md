BBR 在 Youtube 上的应用：吞吐量提升
  对于日本，吞吐量上升14%

BBR 在 Youtube 上的应用：RTT 时延变短
  对有些国家，RTT时延，50%以上的降低


BBR 在 Youtube 上的应用：重新缓冲时间间隔变长   发生丢包可能发生重新缓冲，bbr是丢包减少了


最佳控制点在哪
 虽然在1979提出，但是大家认为当RTT变高，带宽不变的情况下，是不是由于TCP链路发生了变化呢，这是很难搞清楚的，所以当时认为这件事做不到
   2016，bbr推出，可以做到了


BBR 如何找到准确的 RTprop 和 BtlBw？
• RTT 里有排队噪声
  • ACK 延迟确认、网络设备排队     
• 什么是 RTprop？是物理属性  将RTT中排队和延迟去除就是RTprop,从发送数据到接受整个链路的时间
  RTT=RTprop+噪声
  多次测量后，取噪声的最小值，可以近似得到rtprop

BtlBw  最大传输带宽
  多次测量，取最大的发送速率

有了RTprop 和 BtlBw可以找到最佳控制点



基于 pacing_gain 调整    tcp链路发生调整后，RTprop 和 BtlBw的测量就失效了，发送端怎么知道这件事情呢，基于pacing_gain调整
• 700 ms内的测量 
  • 10-Mbps, 40-ms链路
• 如何检测带宽变大？
  • 定期提升pacing_gain
 图中横坐标是时间，
 定期提升或降低发送速率，如果链路发生变化，RTprop 和 BtlBw也会发送变化
  提升或降低的速率，来于图中的cycle gain 1.25/0.75


当线路变换时 pacing_gain 的作用  发现RTT异常，重新测量调整
• 20 秒：10-Mbps, 40-ms 升至 20 Mbps   带宽上升
• 40 秒：又降至 10-Mbps     线路带宽下降



对比 CUBIC 下的慢启动
• 10-Mbps, 40-ms   理想是0.4比特 0.05MB字节
• 慢启动
  • startup
  • drain
  • probe BW
  图片上部分 y轴是发送或收到的字节数  蓝色的是ack,绿色的是bbr,红色是cubic
  图片下部分 y轴是RTT 
    bbr 感知到带宽开始积压，开始降低发送速率，排空后进入平滑的阶段
    cubic 无法做到，很快进入拥塞控制，仍然持续飞行中的报文
    probe bw 就是探测带宽是否发生变化



多条初始速度不同的 TCP 链路快速的平均分享带宽
 • 100-Mbps/10-ms
  当有多条TCP线路都使用bbr时，先后速度不同，一开始没法均分带宽，由于pacing_gain经过周期性调整，很快就均分整个带宽



Google B4 WAN实践
• 2-25 倍吞吐量提升
  • 累积分布函数
• 75%连接受限于 linux kerner 接收缓存
• 在美国-欧洲路径上提升 linux kernal 接收缓存上限后有 133 倍提升

 图中cdf 累积分布图


RTT 大幅下降
 • 10-Mbps, 40-ms


不同丢包率下的吞吐量：CUBIC VS BBR
• 100-Mbps/100-ms
• 红色 CUBIC
• 绿色 BBR
 cubic随着丢包率的上升，吞吐量下降的很快
 bbr 丢包率上升到5%才有明显的下降，15%急剧下降，


Youtube 一周以上 2 亿次播放数据统计
 bbr 相当于cubic的1/4



SGSN 移动网络
• 128-Kbps/40-ms
• 新连接建立困难
  三次握手建立连接的时候，重发次数和超时时间都是有限的，一旦丢包率增大，RTT时延增大，都会导致新连接困难
  使用bbr后新连接的建立也会更容易



收到 Ack 时   bbr算法的执行主要有两点
• 更新 RTprop 、BtlBw

function onAck(packet) 
   rtt = now - packet.sendtime 
   update_min_filter(RTpropFilter, rtt)   更新RTprop
   delivered += packet.size 
   delivered_time = now 
   deliveryRate=(delivered-packet.delivered)/(delivered_timepacket.delivered_time) 
   if (deliveryRate > BtlBwFilter.currentMax || ! packet.app_limited) 
     update_max_filter(BtlBwFilter, deliveryRate)   更新brlbw
   if (app_limited_until > 0) 
     app_limited_until = app_limited_until - packet.size



当发送数据时
• pacing_gain
  • 5/4, 3/4, 1, 1, 1, 1, 1, 1

function send(packet) 
  bdp = BtlBwFilter.currentMax × RTpropFilter.currentMin 
  if (inflight >= cwnd_gain × bdp)  发送速率超出带宽
   // wait for ack or retransmission timeout 
   return 
  if (now >= nextSendTime) 
    packet = nextPacketToSend() 
    if (! packet) 
       app_limited_until = inflight 
    return 
    packet.app_limited = (app_limited_until > 0) 
    packet.sendtime = now 
    packet.delivered = delivered 
    packet.delivered_time = delivered_time 
    ship(packet) 
    nextSendTime = now + packet.size / (pacing_gain × BtlBwFilter.currentMax) 使用pacing_gain探测
  timerCallbackAt(send, nextSendTime)


想使用bbr 1.更新系统内核  2.http3在应用层提供了支持 quic


安远
pacing_gain，在拥塞的边缘来回试探
作者回复: 没错^_^