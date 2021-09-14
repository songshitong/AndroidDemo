将IP地址转为Mac地址的arp和rarp协议

链路层MAC地址
• 链路层地址 MAC（Media Access Control Address）
   • 实现本地网络设备间的直接传输
• 网络层地址 IP（Internet Protocol address）
   • 实现大型网络间的传输，
• 查看 MAC 地址
   • Windows: ipconfig /all
   • Linux：ifconfig


2.5 层协议 ARP：从 IP 地址寻找 MAC 地址  ip工作在第三层，Mac工作在第二层  一般工作在局域网
• 动态地址解析协议 ARP（RFC826）
   • Address Resolution Protocol
• 动态地址解析：广播
  a想和b通信，只有b的IP地址，没有Mac地址而交换机只认Mac地址，所以主机a只有先获取b的Mac地址，才能组装出合适数据链路层帧
   这个帧才能被交换机识别，然后传输给b
  a通过arp协议发出广播， 他把ip b放在广播包中去询问整个网络，谁是ip b,c和d都收到发现自己不是b,所以c和d不回答，b做出回答并返回自己的mac地址


2.5 层协议：ARP
1. 检查本地缓存    广播的性能损耗很大，发送完广播查询后进行缓存
    • Windows: arp –a
    • Linux: arp –nv
    • Mac: arp -nla
2. 广播形式的请求   请求时广播的，应答是单播的
3. 单播形式的应答

清除arp缓存
   arp -ad


ARP 报文格式：FrameType=0x0806
• 硬件类型，如 1 表示以太网
• 协议类型，如 0x0800 表示 IPv4
• 硬件地址长度，如 6
• 协议地址长度，如 4 表示 IPv4
• 操作码，如 1 表示请求，2 表示应答
• 发送方硬件地址
• 发送方协议地址   IPV4
• 目标硬件地址
• 目标协议地址




硬件类型与操作码
• 硬件类型取值  1是以太网
• 操作码取值

wireshark 抓包 捕获过滤器 arp   一台主机ping另一台





1	0.000000	Dell_9c:e5:07	Broadcast	ARP	60		Who has 10.24.61.254? Tell 10.24.61.107
    Ethernet II, Src: Dell_9c:e5:07 (14:18:77:9c:e5:07), Dst: Broadcast (ff:ff:ff:ff:ff:ff)
        Destination: Broadcast (ff:ff:ff:ff:ff:ff)  广播类型 255.255.255.255
        Source: Dell_9c:e5:07 (14:18:77:9c:e5:07)
        Type: ARP (0x0806)
        Padding: 000000000000000000000000000000000000
    Address Resolution Protocol (request)
        Hardware type: Ethernet (1)   以太网
        Protocol type: IPv4 (0x0800)     协议类型IPV4
        Hardware size: 6
        Protocol size: 4
        Opcode: request (1)
        Sender MAC address: Dell_9c:e5:07 (14:18:77:9c:e5:07)
        Sender IP address: 10.24.61.107
        Target MAC address: 00:00:00_00:00:00 (00:00:00:00:00:00)
        Target IP address: 10.24.61.254   想知道10.24.61.254的Mac地址是多少

6	235.427245	RealtekS_68:03:c1	AsixElec_ad:ae:b2	ARP	42		10.24.61.65 is at 00:e0:4c:68:03:c1
    Ethernet II, Src: RealtekS_68:03:c1 (00:e0:4c:68:03:c1), Dst: AsixElec_ad:ae:b2 (00:0e:c6:ad:ae:b2)
        Destination: AsixElec_ad:ae:b2 (00:0e:c6:ad:ae:b2)
        Source: RealtekS_68:03:c1 (00:e0:4c:68:03:c1)
        Type: ARP (0x0806)
    Address Resolution Protocol (reply)
        Hardware type: Ethernet (1)
        Protocol type: IPv4 (0x0800)
        Hardware size: 6
        Protocol size: 4
        Opcode: reply (2)  应答单播
        Sender MAC address: RealtekS_68:03:c1 (00:e0:4c:68:03:c1)
        Sender IP address: 10.24.61.65
        Target MAC address: AsixElec_ad:ae:b2 (00:0e:c6:ad:ae:b2)
        Target IP address: 10.24.61.45

另一个抓包
7	82.553289	HuaweiTe_82:5e:b2	RealtekS_68:03:c1	ARP	60		Who has 10.24.61.65? Tell 10.24.61.254
Address Resolution Protocol (request)
Hardware type: Ethernet (1)
Protocol type: IPv4 (0x0800)
Hardware size: 6
Protocol size: 4
Opcode: request (1)
Sender MAC address: HuaweiTe_82:5e:b2 (f8:6e:ee:82:5e:b2)
Sender IP address: 10.24.61.254
Target MAC address: RealtekS_68:03:c1 (00:e0:4c:68:03:c1)
Target IP address: 10.24.61.65

8	82.553306	RealtekS_68:03:c1	HuaweiTe_82:5e:b2	ARP	42		10.24.61.65 is at 00:e0:4c:68:03:c1
Address Resolution Protocol (reply)
Hardware type: Ethernet (1)
Protocol type: IPv4 (0x0800)
Hardware size: 6
Protocol size: 4
Opcode: reply (2)
Sender MAC address: RealtekS_68:03:c1 (00:e0:4c:68:03:c1)
Sender IP address: 10.24.61.65
Target MAC address: HuaweiTe_82:5e:b2 (f8:6e:ee:82:5e:b2)
Target IP address: 10.24.61.254


2.5 层协议 RARP：从 MAC 地址中寻找 IP 地址
• 动态地址解析协议 RARP（RFC903）
 • Reverse Address Resolution Protocol
  有一个无盘工作站 打印机，启动时没有IP地址，有一个服务器自动给他配置地址，他就会发送一个rarp的广播，询问我的Mac地址是这样的，我的IP地址是什么
    此时rarp的server会响应你的IP是xx



RARP 的工作流程
1. 广播形式的请求
2. 单播形式的应答


RARP 报文格式：FrameType=0x8035  与arp报文相接近，主要差别在opcode
• 硬件类型，如 1 表示以太网
• 协议类型，如 0x0800 表示 IPv4
• 硬件地址长度，如 6
• 协议地址长度，如 4 表示 IPv4
• 操作码，如 3 表示请求，4 表示应答
• 发送方硬件地址
• 发送方协议地址
• 目标硬件地址
• 目标协议地址


ARP 欺骗（ARP spoofing/poisoning）
  本地网络中 ALice 和 Bob进行通信
  攻击者 charlie主动应答 Alice和bob的arp报文，对alice的应答10.0.0.2的Mac地址是cc 对bob的应答 10.0.0.1的Mac地址是cc
    此时alice和bob的数据链路层帧会发送给charlie
正当的应用，酒店中提供网络，但需要身份登录，可以通过这种方式



天心
如果一个ARP报文有多个响应，比如A和B（B是恶意攻击者）都返回了自己的Mac地址，此时请求方如何判断呢？
作者回复: 很难判断！真的遇到这种攻击时，一种常用且简单的解决方案，是网管通过其他途径拿到网络中的所有MAC地址，然后在路由器或者交换机上设立MAC白名单

我在你的视线里
什么是公有云，什么是私有云呢？他们的区别是什么？阿里云是公有云吗？华为的私有云做的也不错。评价云好的指标是什么？
作者回复: 对外提供服务则为公有云，反之为私有云。阿里云是公有云，当然它也有私有云产品，华为也一样。
云是一个商业产品，可以从其商业价值上判断
