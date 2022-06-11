TCP 的作用
从客户端网络，到服务器网络，每个segment，报文都可能丢掉或改变，TCP需要保证准确到达

TCP协议的分层
• TCP：面向连接的、可靠的、基于字节流的传输层通信协议   1对1链接，一定能到达，消息没有边界，多大都能传输，有序的，前一个没收到，先收到后一个字节，也不能丢给应用层处理
• IP：根据IP地址穿越网络传送数据

 udp可以1对多




rfc
TCP is a connection-oriented, end-to-end reliable protocol designed to
fit into a layered hierarchy of protocols which support multi-network
applications.  The TCP provides for reliable inter-process
communication between pairs of processes in host computers attached to
distinct but interconnected computer communication networks.


层层嵌套的“信封”：报文头部   tcp如何做到这么多功能
   每一层只负责自己的事情，自己的报文对于下层类似于自定义数据，自己也不关心上传的数据格式进行封装传递



报文头部的层层组装与卸载    tcp面临的问题
• 不可靠的网络传输
  • 网络设备
  • 主机
  • 物理链路


TCP 协议特点  或者说TCP要解决的问题
• 在 IP 协议之上，解决网络通讯可依赖问题
  • 点对点（不能广播、多播），面向连接    链接存在才能通信
  • 双向传递（全双工）  http/1.1是单向的，websocket将TCP双向传递的能力暴露给应用层
  • 字节流：打包成报文段、保证有序接收、重复报文自动丢弃   8位一组
     • 缺点：不维护应用报文的边界（对比 HTTP、GRPC）   http自己定义content-length,/r/n来标志结尾 处理粘包？
     • 优点：不强制要求应用必须离散的创建数据块，不限制数据块大小   无穷无尽的二进制流，需要应用层处理粘包和拆包
  • 流量缓冲：解决速度不匹配问题    客户端和服务器硬件配置不同，处理速度不同
  • 可靠的传输服务（保证可达，丢包时通过重发进而增加时延实现可靠性） 主要通过重发
  • 拥塞控制     整个网络的问题