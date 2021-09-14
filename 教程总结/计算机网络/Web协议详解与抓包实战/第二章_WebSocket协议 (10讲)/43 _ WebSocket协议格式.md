帧格式示意图    rfc提供的
  • 红色是 2 字节必然存在的帧首部     0 1 2 3 表示0-30i  是4个字节
  chrome 看不到帧头部 
   wireshark    代理VPN后，代理端口默认不是80
     host echo.websocket.org and port 80
   
   protocol websocket是ws的请求
    内容 line-based text data
    WebSocket  81 1c 一定存在与帧首部
    
    
数据帧格式：RSV 保留值
 RSV1/RSV2/RSV3：默认为 0，仅当使用 extension 扩展时，由扩展决定其值  
 
 WebSocket   .000 = Reserved: 0x0    Reserve 储备，保留
 
 

数据帧格式：帧类型   opcode
• 持续帧
  • 0：继续前一帧
• 非控制帧
  • 1：文本帧（UTF8）
  • 2：二进制帧
  • 3-7：为非控制帧保留
• 控制帧
 • 8：关闭帧
 • 9：心跳帧 ping
 • A：心跳帧 pong
 • B-F：为控制帧保留
 
 WebSocket
 //文本帧
 .... 0001 = Opcode: Text (1)
 二进制帧
 .... 0010 = Opcode: Binary (2)



ABNF 描述的帧格式
• ws-frame = frame-fin ; 1 bit in length 
     frame-rsv1 ; 1 bit in length 
     frame-rsv2 ; 1 bit in length 
     frame-rsv3 ; 1 bit in length 
     frame-opcode ; 4 bits in length 
     frame-masked ; 1 bit in length 
     frame-payload-length ; 3 种长度    payload不是必须有的
     [ frame-masking-key ] ; 32 bits in length  可选的 掩码
     frame-payload-data ; n*8 bits in ; length, where ; n >= 0
    
    
    
 
码农Kevin亮
请问老师，数据帧（frame）与数据包（packet）究竟有什么区别？越学越糊涂了，请老师解惑
作者回复: 在不同的网络层次下，讨论的概念就不相同。
在IP层，我们叫Packet，在Websocket层，我们叫frame，需要清楚：frame下有TCP层，而tcp层下有ip层，因此，一个frame可能分为多个packet。
到第五、六部分课程学完可能会有更清晰的了解。



一步
websocket 帧 的每一个字节每一位代表的意思 只能靠经验硬记吗？有没有好的办法
作者回复: wireshark已经自动帮我们解析好，无需记忆
    
