WebSocket 的成本
• 实时性与可伸缩性
  • 牺牲了简单性
    客户端与负载均衡只建立了一个长连接   负载与服务器的交互很复杂
      接入层+实现层
      接入层 将ws消息进行转换为消息系统识别的消息
• 网络效率与无状态：请求 2 基于请求 1
  • 牺牲了简单性与可见性
    如果做到无状态，每个请求都携带重复消息
    如果请求2基于请求1，牺牲了可见性和简单性，单个请求没办法找到全部信息
    
  
长连接的心跳保持   ws的心跳会复杂一点
• HTTP 长连接只能基于简单的超时（常见为 65 秒）
• WebSocket 连接基于 ping/pong 心跳机制维持    


兼容 HTTP 协议   建立连接需要握手
• 默认使用 80 或者 443 端口  
• 协议升级
• 代理服务器可以简单支持  tcp暴露给应用层



设计哲学：在 Web 约束下暴露 TCP 给上层  不做各种封装，http/2.0将tcp进行各种封装给应用层    由http/1.1升级，暴露tcp的功能
• 元数据去哪了？
  • 对比：HTTP 协议头部会存放元数据
  • 由 WebSocket 上传输的应用层存放元数据   ws没有规定元数据放在哪
• 基于帧：不是基于流（HTTP、TCP）  ascii字节流，每个字节是有序的
  • 每一帧要么承载字符数据，要么承载二进制数据   字符也是ascii
• 基于浏览器的同源策略模型（非浏览器无效）
  • 可以使用 Access-Control-Allow-Origin 等头部
• 基于 URI、子协议支持同主机同端口上的多个服务
  子协议  Sec-WebSocket-Protocol: xxx
  
  
  
  
WL
请问一下老师两个问题:
1. 我没太理解websocket协议实现可伸缩性性引入消息分发系统的目的是因为无法增加接入server的数量而只能增加实现server的数量吗?
2. 在网络效率和无状态上, 为啥请求1与请求2无状态就要传递大量的重复消息呢? 为啥请求2要基于请求1, 如果不基于请求1, 请求2会受到什么影响吗?
作者回复: 1、这里是指，如何多个SERVER服务于一个长连接。
2、例如，同一连接上请求1是html，请求2是css文件，那么请求1中携带了user_agent，这样请求2是不用携带user-agent的。 所谓“基于”是指业务依赖，比如请求1是登录，请求2是用户信息列表展示


安排
websocket的成本那张图中，负载均衡是指七层负载均衡吗？还是四层负载均衡？
作者回复: 没有具体的指代，四层或者七层负载均衡都可以


WL
还有一个问题: websocket协议没定义元数据放在哪里和需要我们自己定义元数据是啥意思, 是说需要我们在程序中把元数据写到TCP的报文里面吗?
作者回复: 比如Content-Type，描述DATA是视频还是音频，这就是元数据。这里我是基于第一部分描述REST架构时的思路，HTTP/1协议里就含有了元数据。




  