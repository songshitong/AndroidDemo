如何及时获得更新？从轮询到通知

http/1.1  定时轮询    
   长时间不更新，服务器流量压力大，带宽的占用高
   轮询时间长，更新不及时
   
websocket 主动通知
 websocket使用一条TCP连接，减少了http的rtt时间


websocket测试
http://www.websocket.org/echo.html
打开两个页面，一个发送消息，另一个接受消息

   

Chrome 请求列表：分析 WebSocket

• 过滤器
  • 按类型：WS     选中ws
  • 属性过滤：is: running

• 表格列   message里面
  • Data： 消息负载。 如果消息为纯文本，则在此处显示。 对于二进制操作码，此列将显示操作码的名称和代码。 支持以下操作码：
      Continuation Frame、Binary Frame、Connection Close Frame、Ping Frame 和 Pong Frame。
  • Length： 消息负载的长度（以字节为单位）。
  • Time： 收到或发送消息的时间。

• 消息颜色  message里面
  • 发送至服务器的文本消息为浅绿色。
  • 接收到的文本消息为白色。
  • WebSocket 操作码为浅黄色。
  • 错误为浅红色。   


WebSocket 规范定义了一种 API，可在网络浏览器和服务器之间建立“套接字”连接。简单地说：客户端和服务器之间存在持久的连接，
而且双方都可以随时开始发送数据  
WebSocket 提供了类似socket套接字的接口   websocket工作在应用层
  
  
支持双向通讯的 WebSocket    不止在浏览器可以使用，Android，ios都可以

• rfc6455（2011.12）   
• 双向通讯的优劣？  
    实现服务器推送，相比http/2.0简单
    相比http的request，response可伸缩性差，建立连接后服务器扩容，增加节点方案复杂
• 如何管理会话？
    会话时从http/1.1升级过来的，关闭需要双向关闭
• 如何维持长连接？
    心跳
• 兼容 HTTP 协议  由http/1.1升级而来
  • 端口复用   
• 支持扩展
  • 如 permessage-deflate 扩展   压缩
  

websocket 的缺点
 由于过于简单很多功能和性能上的问题没有得到解决，http/2对ws做了很多改进      
 tcp的问题

其他问题
对于服务器 长连接资源消耗多
不够成熟  存在浏览器支持不足的问题   生态不足，使得原本支持http的组件不能正常工作了

ws 的进化
ws over http2   https://datatracker.ietf.org/doc/html/rfc8441
chrome 支持 wss over http2
https://www.chromestatus.com/feature/6251293127475200


代理服务器
每一种新技术在出现时，都会伴随着一系列问题。WebSocket 也不例外，它与大多数公司网络中用于调解 HTTP 连接的代理服务器不兼容。
WebSocket 协议使用 HTTP 升级系统（通常用于 HTTP/SSL）将 HTTP 连接“升级”为 WebSocket 连接。某些代理服务器不支持这种升级，
并会断开连接。因此，即使指定的客户端使用了 WebSocket 协议，可能也无法建立连接
https://www.html5rocks.com/zh/tutorials/websockets/basics/


http2 与ws对比   https://cloud.tencent.com/developer/article/1445895

        HTTP / 2  WebSocket 
头    压缩（HPACK） 没有
二进制 是     二进制或文本
复    是     是
优先级 是     没有
压缩   是    是
方向 客户端/服务器+服务器推送  双向
全双工 是   是

http2不能替代ws  http2的推送只能推送到浏览器，不能推送到应用层     解决http2+sse，比ws高效简单



Edward Lee
因为 http://demos.kaazing.com/echo/index.html 访问受限了，
因此我找了另外一个 ws demo 网站 http://www.websocket.org/echo.html
作者回复: 确实是，谢谢你的分享！

  







