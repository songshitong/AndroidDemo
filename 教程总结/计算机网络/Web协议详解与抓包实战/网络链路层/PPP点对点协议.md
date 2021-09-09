PPP   Point-to-Point protocol
1对1连接计算机的协议

PPP属于纯粹的数据链路层，与物理层没有任何关系，仅有PPP无法实现通信，还需要物理层的支持
PPP可以使用电话线或ISDN、专线、ATM线路。近年更多的ADSL或有线电视通过PPPoE实现互联网接入

在开始进行数据传输前，要先建立一个PPP级的连接。当这个链接建立以后就可以进行身份认证、压缩与加密。


PPP子协议
LCP   Link Control Protocol  不依赖上传协议
NCP   Network Control Protocol  依赖上层协议，如果上层协议为IP，叫做IPCP(Ip control protocol)

Lcp 负责建立和断开连接、设置最大接受单元(MRU,maximum receive unit)、设置验证协议(pap或chap)以及设置是否进行通信质量的监控
IPCP 负责IP地址设置以及是否进行TCP/ip首部压缩等设备

通过PPP连接时，通常需要进行用户名密码的验证，并且通过通信两端进行双方向的验证。
验证协议：
PAP password authentication protocol
CHAP Challenge Handshake  Authentication Protocol

PAP是PPP连接建立时，通过两次握手进行用户名和密码验证。其中密码是明文传输，一般用于安全要求不是很高的环境，否则存在盗用和窃听连接的风险
CHAP使用一次性密码OTP(one time password)，可以有效防止窃听。此外在建立连接之后可以进行定期的密码交换，用来检验对端是否中途被替换


PPP的帧格式
标志         地址        控制      类型    数据        FCS       标志
01111110   11111111   00000011  2字节   0-1500字节  4字节       01111110
PPP基于HDLC定制，每个帧前后是固定的标识码用来区分帧。两个标识码中间不允许出现6个以上的1.当出现连续的5个1时后面必须插入一个0
  接受端收到连续的5个1且后面跟的是0，就必须删除。
 
在通过电脑进行拨号时，PPP已在软件中实现。因此插入或删除0的操作或FCS计算都交由电脑的CPU处理，这也是为什么人们常说PPP会给计算机
带来大量负荷的原因所在





PPPoE (PPP over Ethernet)  
部分互联网接入服务商在以太网上利用pppoe提供ppp功能
在这种互联网接入服务中，通信线路由以太网模拟。由于以太网越来越普及，加上他的网络设备与相应的NIC价格比较便宜，因此ISP能够提供一个
  单价更低的互联网接入服务。
单纯的以太网没有验证功能，也没有建立和断开连接的处理，因此无法按时计费。如果采用PPPoE管理以太网，就可以利用PPP的验证等功能使各家
ISP可以有效管理终端用户的使用。

以太网首部          PPPoE首部   PPP协议      数据           FCS
14字节             6字节      2字节0xc021  38-1492字节
一台类型0x8864

PPPoE首部 6字节
版本   类型   编码   会话ID    长度
4比特 4比特   1字节  2字节     2字节
