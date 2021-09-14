服务器之间通讯也可以用http/2    grpc就是运行在http/2之上


gRPC：支持多语言编程、基于 HTTP/2 通讯的中间件 见图



gRPC 测试
• 官网：https://grpc.io/
• 基于 Python 语言搭建测试环境
  • https://grpc.io/docs/quickstart/python/
  • 测试程序
     • git clone -b v1.21.0 https://github.com/grpc/grpc
     • cd grpc/examples/python/helloworld
     • 服务器：python greeter_server.py  端口50051
     • 客户端：python greeter_client.py
 • 注意
    • wireshark欲抓取环回报文，请安装时勾选【install Npcap in Winpcap API-Compatible Mode 】
    • 如果 Npcap Loopback Adapter 未抓取到环回报文，请尝试其他接口    老师的就抓不到。。。
    • 若 50051 端口未被识别为 http/2，请手动设置“解码为 HTTP/2”
    
   soure destination 都是ipv6的地址::1   本地进程通讯
   50051默认不会被解析为HTTP报文   右键 decode as   port改为50051  current改为http2,默认为none
   
   
 
helloworld.proto  protobuff定义的消息结果



Protocol Buffers 编码：消息结构
 每个field分为tag和value
 tag
 
Protocol Buffers 编码：数据类型 Wire Type  


Protocol Buffers 字符串编码举例




自己：protocol buffers的应用场景越来越多，需要了解pb的报文才能更好的使用 
协议升级怎么做到的
只有http/1.1支持   对应的header Connection: Upgrade
https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Protocol_upgrade_mechanism 

aaa
感觉grpc协议没怎么讲啊。而且protobuf编码和http关系不大，个人感觉可以简单一带而过的
作者回复: grpc不是协议，就是对http header有了一些新的定义，比如当body是protobuf时，content-type 的值为application/grpc。另外，由于grpc允许流式推送消息，所以body中并不是直接放protobuf，而是先放了一层Length-Prefixed Message。具体可以看这篇：https://time.geekbang.org/column/article/247812

kyo
原来还能手动转换协议, 学到了. 感谢!
作者回复: ^_^

