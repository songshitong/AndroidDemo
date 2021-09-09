RST_STREAM 帧（type=0x3）
• HTTP2 多个流共享同一连接，RST 帧允许立刻终止一个未完成的流     不能关闭连接，会影响其他流，只能关闭流
• RST_STRAM 帧不使用任何 flag
• RST_STREAM 帧的格式
  --------------
  Error Code(32)
  --------------
  
 常见错误码（1） 
 • NO_ERROR (0x0): 没有错误。GOAWAY帧优雅关闭连接时可以使用此错误码   RST_STREAM帧，数据帧也可以使用
 • PROTOCOL_ERROR (0x1): 检测到不识别的协议字段    协议版本不匹配
 • INTERNAL_ERROR (0x2):内部错误      
 • FLOW_CONTROL_ERROR (0x3): 检测到对端没有遵守流控策略   超过最大字节数
 • SETTINGS_TIMEOUT (0x4): 某些设置帧发出后需要接收端应答，在期待时间 内没有得到应答则由此错误码表示
 
 
 
 常见错误码（2）
 • STREAM_CLOSED (0x5): 当Stream已经处于半关闭状态不再接收Frame帧时， 又接收到了新的Frame帧
 • FRAME_SIZE_ERROR (0x6): 接收到的Frame Size不合法  默认16k，使用16M需要显示的声明
 • REFUSED_STREAM (0x7): 拒绝先前的Stream流的执行  可以进行重试
 • CANCEL (0x8): 表示Stream不再存在
 • COMPRESSION_ERROR (0x9): 对HPACK压缩算法执行失败   动态表中找不到索引号
 
 
 
 常见错误码（3）
 • CONNECT_ERROR (0xa): 连接失败    针对connect请求
 • ENHANCE_YOUR_CALM (0xb): 检测到对端的行为可能导致负载的持续增加， 提醒对方“冷静”一点   负载增加太快，要超出可处理的范围了
 • INADEQUATE_SECURITY (0xc): 安全等级不够    tls中的某些安全算法等级不够
 • HTTP_1_1_REQUIRED (0xd): 对端只能接受HTTP/1.1协议   不接受http/2