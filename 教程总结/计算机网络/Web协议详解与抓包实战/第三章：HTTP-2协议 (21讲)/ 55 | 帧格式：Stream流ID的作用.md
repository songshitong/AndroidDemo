 对比Websocket的帧格式
   websocket的帧格式图
 
 9 字节标准帧头部
   增加stream ID    R是保留位
   frame payload一般是自定义消息头
   
 wireshark中  查看HTTP2层
 
 
 Stream ID 的作用 (1)
 
 • 实现多路复用的关键   
   • 接收端的实现可据此并发组装消息
   • 同一 Stream 内的 frame 必须是有序的（无法并发）
   • SETTINGS_MAX_CONCURRENT_STREAMS 控制着并发 Stream 数
 图中server可以给客户端发送stream1和stream2的消息  
   stream1的帧和stream2的帧是穿插的，根据streamID进行组装
   并发要新启一个stream
   
   
 Stream ID 的作用（2）
  • 推送依赖性请求的关键
    • 由客户端建立的流必须是奇数
    • 由服务器建立的流必须是偶数  
  
   抓包推送的 host http2.taohui.tech   
   headers[1]  streamid为1
   push_promise[1]  确认推送app/poster.jpg的图片
   headers[2]   服务器发起推送 
   
   
  
Stream ID 的作用（3）
• 流状态管理的约束性规定
  • 新建立的流 ID 必须大于曾经建立过的状态为 opened 或者 reserved 的流 ID
  • 在新建立的流上发送帧时，意味着将更小 ID 且为 idle 状态的流置为 closed 状态
  • Stream ID 不能复用，长连接耗尽 ID 应创建新连接    2的31次方
  
  
Stream ID 的作用（4）
• 应用层流控仅影响数据帧
  • Stream ID 为 0 的流仅用于传输控制帧    stream ID为0是控制帧
• 在HTTP/1 升级到 h2c 中，以 ID 为 1 流返回响应，之后流进入half-closed (local)状态  




疯琴
请问老师：TCP是保证有序的，HTTP2是基于TCP的，他是怎么做到可以在stream1未完全到达的时候先处理stream3的？
作者回复: 基于Frame中的Stream ID完成的。同一个ID中的Stream，必须是按序处理，但不同的ID，接收方可以并行处理。因此，接收方不需要等stream1接收完整，可以直接处理任何其他stream


Hurry
一个请求 message 和 该请求的响应 message 可以在不同的 stream 吗？
作者回复: 理论上可以，但实际上浏览器与Web服务器都不会这么做，这增加了HTTP复杂性


