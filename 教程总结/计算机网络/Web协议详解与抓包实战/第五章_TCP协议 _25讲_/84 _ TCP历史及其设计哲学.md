

TCP/IP 的前身 ARPA：NCP 协议    美国国防部的协议
• Advanced Research Projects Agency Network
问题 没有IP功能，一台主机对另外一台主机的通信
    没有容错功能，网络扩大时，出错率增加


1973：TCP/IP 协议
• 文顿·格雷·瑟夫（Vinton Gray Cerf）
• 罗伯特·艾略特·卡恩（Robert Elliot Kahn）


TCP/IP 协议发展
  IP一出来就是V4 了。。。

TCPv4 协议分层后的互联网世界
  udp协议非常简单，着重TCP协议    路由器基于内存，内存溢出就会丢包，容错要高


TCP/IP 的七个设计理念  重点前三个

David D Clark：《The Design Philosophy of The DARPA Internet Protocols》
1. Internet communication must continue despite loss of networks or gateways.  能够容错
2. The Internet must support multiple types of communications service.   支持不同的通信设备   华为，思科的交换机，路由器
3. The Internet architecture must accommodate a variety of networks.      链接不同的网络，WiFi，光纤
4. The Internet architecture must permit distributed management of its resources.
5. The Internet architecture must be cost effective.
6. The Internet architecture must permit host attachment with a low level of effort.
7. The resources used in the internet architecture must be accountable.





爱因诗贤
老师，请教一个问题。请问三次握手和四次挥手中的四次挥手在什么情况下可以合并为三次挥手？
作者回复: 如果被动关闭端调用close/shutdown函数非常及时，内核在很大概率上也会将ACK与FIN放在一个报文中发送，这就变成三次挥手了。
还有就是两端同时关闭，当然这个概率实在太小了。



