HTTP/2 是不是必须基于 TLS/SSL 协议？
• IETF 标准不要求必须基于TLS/SSL协议
• 浏览器要求必须基于TLS/SSL协议   安全 握手，即便不使用TLS也要经历一次必不可少的握手
• 在 TLS 层 ALPN (Application Layer Protocol Negotiation)扩展做协商，只认 HTTP/1.x 的代理服务器不会干扰 HTTP/2
• shema：http://和 https:// 默认基于 80 和 443 端口
• h2：基于 TLS 协议运行的 HTTP/2 被称为 h2
• h2c：直接在 TCP 协议之上运行的 HTTP/2 被称为 h2c


h2 与 h2c 
 h2 基于 ALPN升级
 h2c 与websocket相似 基于http/1.1升级
 
 
H2C：不使用 TLS 协议进行协议升级（1）
• 客户端测试工具：curl（7.46.0版本）
  • curl http://nghttp2.org -–http2 -v    支持h2c格式  图片中的地址是错误的
  
  curl --version       结果7.64.1
  
  tcpdump 抓取curl的报文
   tcpdump -i eth0 port 80 and host nghttp2.org -w h2c.pcap
    老师提供h2c.pcap中
     第5个请求 get  做的是协议升级  Upgrade: h2c
     Frame 5: 226 bytes on wire (1808 bits), 226 bytes captured (1808 bits)
     Ethernet II, Src: Xensourc_1c:06:1c (00:16:3e:1c:06:1c), Dst: ee:ff:ff:ff:ff:ff (ee:ff:ff:ff:ff:ff)
     Internet Protocol Version 4, Src: 172.16.20.227, Dst: 139.162.123.134
     Transmission Control Protocol, Src Port: 38186, Dst Port: 80, Seq: 1, Ack: 1, Len: 160
     Hypertext Transfer Protocol
         GET / HTTP/1.1\r\n
         Host: nghttp2.org\r\n
         User-Agent: curl/7.46.0\r\n
         Accept: */*\r\n
         Connection: Upgrade, HTTP2-Settings\r\n
         Upgrade: h2c\r\n
         HTTP2-Settings: AAMAAABkAAQAAP__\r\n
         \r\n
         [Full request URI: http://nghttp2.org/]
         [HTTP request 1/1]
         [Response in frame: 7]
   
   响应 101 Switching Protocols 
   Frame 7: 170 bytes on wire (1360 bits), 170 bytes captured (1360 bits)
   Ethernet II, Src: ee:ff:ff:ff:ff:ff (ee:ff:ff:ff:ff:ff), Dst: Xensourc_1c:06:1c (00:16:3e:1c:06:1c)
   Internet Protocol Version 4, Src: 139.162.123.134, Dst: 172.16.20.227
   Transmission Control Protocol, Src Port: 80, Dst Port: 38186, Seq: 1, Ack: 161, Len: 104
   Hypertext Transfer Protocol
       HTTP/1.1 101 Switching Protocols\r\n
       Connection: Upgrade\r\n
       Upgrade: h2c\r\n
       \r\n
       [HTTP response 1/1]
       [Time since request: 0.134257000 seconds]
       [Request in frame: 5]
       [Request URI: http://nghttp2.org/]
   HyperText Transfer Protocol 2


 H2C：客户端发送的 Magic 帧   接收到101，协议升级仍没有完成，需要客户端发送magic帧
 • Preface（ASCII 编码，12字节）
   • 何时发送？
     • 接收到服务器发送来的 101 Switching Protocols
     • TLS 握手成功后
   • Preface 内容
     • 0x505249202a20485454502f322e300d0a0d0a534d0d0a0d0a
     • PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n
   • 发送完毕后，应紧跟 SETTING 帧
   
   9	1.252352	172.16.20.227	139.162.123.134	HTTP2	90		Magic
   Frame 9: 90 bytes on wire (720 bits), 90 bytes captured (720 bits)
   Ethernet II, Src: Xensourc_1c:06:1c (00:16:3e:1c:06:1c), Dst: ee:ff:ff:ff:ff:ff (ee:ff:ff:ff:ff:ff)
   Internet Protocol Version 4, Src: 172.16.20.227, Dst: 139.162.123.134
   Transmission Control Protocol, Src Port: 38186, Dst Port: 80, Seq: 161, Ack: 105, Len: 24
   HyperText Transfer Protocol 2
       Stream: Magic
           Magic: PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n   对应的16进制是0x505249202a20485454502f322e300d0a0d0a534d0d0a0d0a
   
   将16进制转为ASCII码 0x505249202a20485454502f322e300d0a0d0a534d0d0a0d0a  是PRI * HTTP/2.0\r\n
   
   setting帧
   10	1.252369	172.16.20.227	139.162.123.134	HTTP2	87		SETTINGS[0]
   
   设置帧详见56将
   
   
 

统一的连接过程  见图




一步

建立 http2 连接后，在发送这个Magic 报文是起到什么作用的？
作者回复: 这是client作最终确认所用。参见RFC文档：In HTTP/2, each endpoint is required to send a connection preface as 
a final confirmation of the protocol in use and to establish the initial settings for the HTTP/2 connection.   





ray
老师好，
1. 请问浏览器要如何得知服务器可以运行在http2协定上，从而进行升级呢？
2. 我打开chrome的抓包面板进入某些站点，发现某些站点似乎一点进去就直接使用http2协定，看不到他有进行升级的动作，请问这可能是什么原因呢？

谢谢老师的解答^^
作者回复: 你可以看下53课，通过TLS握手时的ALPN extension实现的


WL
请问一下老师设置帧是设置啥内容用的？还有设定帧客户端发送是HTTP/2的协议为啥服务端返回的是TCP的协议，为啥不是HTTP/2的协议？
作者回复: 设置帧参见56课



测试curl  返回101 协议升级
curl http://nghttp2.org --http2 -v | grep 101
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
  0     0    0     0    0     0      0      0 --:--:-- --:--:-- --:--:--     0*   Trying 139.162.123.134...
* TCP_NODELAY set
* Connected to nghttp2.org (139.162.123.134) port 80 (#0)
> GET / HTTP/1.1
> Host: nghttp2.org
> User-Agent: curl/7.64.1
> Accept: */*
> Connection: Upgrade, HTTP2-Settings
> Upgrade: h2c
> HTTP2-Settings: AAMAAABkAARAAAAAAAIAAAAA
> 
< HTTP/1.1 101 Switching Protocols
< Connection: Upgrade
< Upgrade: h2c
* Received 101
* Using HTTP2, server supports multi-use
* Connection state changed (HTTP/2 confirmed)
* Copying HTTP/2 data in stream buffer to connection buffer after upgrade: len=6890
* Connection state changed (MAX_CONCURRENT_STREAMS == 100)!
< HTTP/2 200 
< date: Mon, 26 Jul 2021 13:00:43 GMT
< content-type: text/html
< last-modified: Sun, 18 Jul 2021 04:17:59 GMT
< etag: "60f3ab77-19d8"
< accept-ranges: bytes
< content-length: 6616
< x-backend-header-rtt: 0.001602
< server: nghttpx
< via: 2 nghttpx
< alt-svc: h3-29=":443"; ma=3600
< x-frame-options: SAMEORIGIN
< x-xss-protection: 1; mode=block
< x-content-type-options: nosniff
< 
{ [6616 bytes data]
100  6616  100  6616    0     0  28517      0 --:--:-- --:--:-- --:--:-- 28517
* Connection #0 to host nghttp2.org left intact
* Closing connection 0

