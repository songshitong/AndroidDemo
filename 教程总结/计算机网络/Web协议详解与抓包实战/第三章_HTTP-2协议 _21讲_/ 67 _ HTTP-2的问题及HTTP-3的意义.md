TCP 以及 TCP+TLS 建链握手过多的问题
  tcp 三次握手 
  tls 握手  1.2 三次往返    1.3中一次往返
  
  
多路复用与 TCP 的队头阻塞问题
 3个流  ip报文是交错的   共用一个tcp连接   tcp保证有序的，发出的报文顺序和收到的报文顺序是一致的
   红色的报文丢失了，操作系统不会把后续的报文给应用层，一定等待红色的重发，导致队头阻塞了
  前面的阻塞了，反而导致后面的不可用，队头阻塞
   
 
TCP的问题
 • 由操作系统内核实现，更新缓慢     系统更新成本高，很多问题不能及时更新和解决
 
 
QUIC 协议在哪一层？  http/3
  quic上层仍然是http/2  API没有变化     quic 发音同quick
  quic基于udp 实现了TLS和tcp的功能
  
  

使 Chrome 支持 QUIC
 • chrome://flags/#enable-quic  默认是default 改为enable
 
 打开bilibili 视频
  默认default 是 http/1.1
  enable  chorem显示很多 http2+quic/43   
  
 抓包，很多是udp报文     
   选中报文，解码为  端口443  当前GQUIC Google的quic   默认不识别
   
   
quic 分为goole quic  ietf quic
IETF QUIC 协议草案
• IETF draft 20: https://tools.ietf.org/html/draft-ietf-quic-http-20
• https://datatracker.ietf.org/doc/draft-ietf-quic-transport/


QUIC 协议组的 Milestones



android  cronet 实现quic
https://github.com/xiaojuanmao/quic-bench   