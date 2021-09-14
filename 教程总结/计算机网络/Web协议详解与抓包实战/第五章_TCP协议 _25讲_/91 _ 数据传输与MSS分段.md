传输层之下的网络层和数据链路层所使用的内存是有限的，TCP必须把从应用层接受到的任意长度的字节流，切分成许多个报文段


TCP 应用层编程示例   直接使用TCP的socket库


TCP 流的操作  操作系统中发生的操作    拆分为多个segment报文段
• read
• write


TCP 流与报文段
• 流分段的依据
  • MSS：防止 IP 层分段   最大报文段大小    ip层分段是非常没有效率的
  • 流控：接收端的能力   接收端不能及时处理



MSS：Max Segment Size
• 定义：仅指 TCP 承载数据，不包含 TCP 头部的大小，参见 RFC879
• MSS 选择目的
    • 尽量每个 Segment 报文段携带更多的数据，以减少头部空间占用比率   tcp固定头部20字节，IP头部20字节
    • 防止 Segment 被某个设备的 IP 层基于 MTU 拆分    IP 层基于 MTU效率低，发生丢包时需要重传
• 默认 MSS：536 字节（默认 MTU576 字节，20 字节 IP 头部，20 字节 TCP 头部）   576-20-20
• 握手阶段协商 MSS       MAXIMUM SEGMENT SIZE
• MSS 分类
    • 发送方最大报文段 SMSS：SENDER MAXIMUM SEGMENT SIZE
    • 接收方最大报文段 RMSS：RECEIVER MAXIMUM SEGMENT SIZE

TCP 握手常用选项
类型 总长度(字节) 数据  描述
2   4          MSS值 握手时发送端告知可以接收的最大报文段大小


tcp握手时确定client或sever的mss
1	0.000000	10.24.61.65	119.3.206.60	TCP	78		65235 → 80 [SYN] Seq=0 Win=65535 Len=0 MSS=1460 WS=64 TSval=1203475551 TSecr=0 SACK_PERM=1
2	0.017234	119.3.206.60	10.24.61.65	TCP	74		80 → 65235 [SYN, ACK] Seq=0 Ack=1 Win=28960 Len=0 MSS=1460 SACK_PERM=1 TSval=4271534177 TSecr=1203475551 WS=128
3	0.017298	10.24.61.65	119.3.206.60	TCP	66		65235 → 80 [ACK] Seq=1 Ack=1 Win=131712 Len=0 TSval=1203475568 TSecr=4271534177
4	0.017383	10.24.61.65	119.3.206.60	HTTP	271		GET /isoftstone-dsv-ecnet/gwader-pro/gwadar-app.git/info/refs?service=git-upload-pack HTTP/1.1


J.Smile
Ip分段效率低的原因是什么呢？
作者回复: 1个分片丢包后，所有分片都得重传，效率低

kissingers

老师，默认MTU不是1500吗？
作者回复: 以太网是1500，其他网络并不是这样。第6部分有一节介绍IP层分片的课程有介绍


Douglas
比如， 很多书上都有讲tcp时全双工的，可以同时实现数据的双向传输，那么 seq 由接收的变量， 同时也有发送的seq变量，类似这种需要维护双份么
作者回复: 是的，三次握手建立连接时会同步两端的seq



加载中……
老师好，请教个问题：
以下是我理解的：
MTU：是指整个IP数据包(包含IP头和IP数据两部分)的最大值
MSS: 是TCP数据部分(不包含TCP头)的最大值

场景：
比如在以太网下MTU是1500，握手协商出来的MSS是1460，如果TCP层把1460字节数据放到TCP数据部分，组装步骤如下
步骤1、TCP报文=TCP头(20)+TCP数据(1460)=1480
步骤2、IP报文=IP头(20)+TCP segement=1500
这个是正好1500，没有问题。
有另个疑问：
因为TCP和IP的基本头部都是固定20字节，但TCP和IP都有扩展头部，所以会有大于20字节的时候
1、在步骤1中：如果TCP还有扩展头部(4个字节)要发送，TCP头=20+4,TCP层是会把数据部分减去4字节保证最终segment大小是1480，还是会封装成1484的TCP包给IP层
2、如果TCP包是1480，到IP层有扩展头部(总头部大于20字节)，IP层就会分组后再传给数据链路层吧？
作者回复: 理解没问题。避免IP分片是TCP的重要工作，TCP选项当然也是IP的负载，所以不能1484字节给IP层



报文分段
2856	35.578523	116.62.160.193	192.168.22.53	TCP	372		80 → 8101 [PSH, ACK] Seq=1 Ack=401 Win=30336 Len=318 [TCP segment of a reassembled PDU]

当我们基于TCP在传输消息时，对于上面的应用层如果出于某些原因（如超过MSS）TCP Segment不能一次包含全部的应用层PDU，
而要把一个完整消息分成多个段，就会将除了最后一个分段（segment）的所有其他分段都打上“TCP segment of a reassembled PDU”。
https://blog.csdn.net/thollych/article/details/103210932