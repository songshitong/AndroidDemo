
浏览器中输入一部分即提示全部的地址，这是浏览器将访问过的地址存到本地数据库

时序图
展示了浏览器请求的细节

Hypertext Transfer Protocol (HTTP) 协议  超文本传输协议   transfer转义  
//todo 为啥是传输协议
https://www.cnblogs.com/gudi/p/6959715.html
a stateless application-level request/response protocol that uses extensible semantics and 
self-descriptive message payloads for flexible interaction with network-based hypertext information 
systems （RFC7230 2014.6）
RFC是所有协议的最权威的定义的文档
Request For Comments，缩写为RFC，是由互联网工程任务组（IETF）发布的一系列备忘录。文件收集了有关互联网相关信息，
以及UNIX和互联网社群的软件文件，以编号排定。目前RFC文件是由互联网协会（ISOC）赞助发行
https://baike.baidu.com/item/RFC/2798645?fr=aladdin

ietf
国际互联网工程任务组（The Internet Engineering Task Force，简称 IETF）
互联网工程任务组，成立于1985年底，是全球互联网最具权威的技术标准化组织，主要任务是负责互联网相关技术规范的研发和制定，
当前绝大多数国际互联网技术标准出自IETF

一种无状态的、应用层的、以请求/应答方式运行的协议，它使用可扩展的语义和自描述消息格式，与基于网络的超文本信息系统灵活的互动
上面图片中黄色的关键字列出 
  stateless，下一个请求不依赖上一个请求的字段
  request/response 基于一个连接，由客户端发起的，由服务器响应
  extensible semantics 语义可扩展的   新增header，只要客户端和服务端达成一致即可
  self-descriptive  传递的消息是一个自描述的消息  本身即可表示信息，类似xml  不要其他文件配合
  hypertext information  不止可以传输文本，视频，图片
  
参考连接
https://datatracker.ietf.org/doc/html/rfc7230