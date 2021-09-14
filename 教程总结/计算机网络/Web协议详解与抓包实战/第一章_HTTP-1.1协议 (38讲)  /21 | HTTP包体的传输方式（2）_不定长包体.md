两种传输 HTTP 包体的方式（二）
• 发送 HTTP 消息时不能确定包体的全部长度
 • 使用 Transfer-Encoding 头部指明使用 Chunk 传输方式
   • 含 Transfer-Encoding 头部后 Content-Length 头部应被忽略

• 优点
• 基于长连接持续推送动态内容
• 压缩体积较大的包体时，不必完全压缩完（计算出头部）再发送，可以边发送边压缩   体积包大时，没法计算content-length
• 传递必须在包体传输完才能计算出的 Trailer 头部



不定长包体的 chunk 传输方式

• Transfer-Encoding头部
  • transfer-coding = "chunked" / "compress" / "deflate" / "gzip" / transfer-extension
  • Chunked transfer encoding 分块传输编码： Transfer-Encoding：chunked
    • chunked-body = *chunk last-chunk trailer-part CRLF
    • chunk = chunk-size [ chunk-ext ] CRLF chunk-data CRLF
       • chunk-size = 1*HEXDIG：注意这里是 16 进制而不是10进制
       • chunk-data = 1*OCTET   二进制
    • last-chunk = 1*("0") [ chunk-ext ] CRLF   1个或多个0
    • trailer-part = *( header-field CRLF )
    
  
  
打开openresty.taohui.tech/chunkbody     返回helloworld
 查看控制台 Transfer-encoding:chunked
   选中网络抓包， port 80
   http协议
     HTTP chunked response  有两段
        data chunk (5 octets)     chunk size 5   0d0a结束
        data chunk (6 octets) 
        end of chunked encoding(last chunk)   0 0d0a 
        
        
Trailer 头部的传输  http/1.1不支持

• TE 头部：客户端在请求在声明是否接收 Trailer 头部  需要客户端支持
  • TE: trailers  客户端是支持的

• Trailer 头部：服务器告知接下来 chunk 包体后会传输哪些 Trailer 头部
  • Trailer: Date

• 以下头部不允许出现在 Trailer 的值中：
  • 用于信息分帧的首部 (例如 Transfer-Encoding 和 Content-Length)
  • 用于路由用途的首部 (例如 Host)
  • 请求修饰首部 (例如控制类和条件类的，如 Cache-Control，Max-Forwards，或者 TE)
  • 身份验证首部 (例如 Authorization 或者 Set-Cookie)
  • Content-Encoding, Content-Type, Content-Range，以及 Trailer 自身        
                         
                         
                         
                         
MIME

• MIME（ Multipurpose Internet Mail Extensions ） 媒体类型
• content := "Content-Type" ":" type "/" subtype *(";" parameter)
  • type := discrete-type / composite-type
     • discrete-type := "text" / "image" / "audio" / "video" / "application" / extension-token
     • composite-type := "message" / "multipart" / extension-token
     • extension-token := ietf-token / x-token

  • subtype := extension-token / iana-token
  • parameter := attribute "=" value

• 大小写不敏感，但通常是小写
• 例如： Content-type: text/plain; charset="us-ascii“
• https://www.iana.org/assignments/media-types/media-types.xhtml  更详细的类型




Content-Disposition 头部(RFC6266)   附件的形式
• disposition-type = "inline" | "attachment" | disp-ext-type
  • inline：指定包体是以 inline 内联的方式，作为页面的一部分展示
  • attachment：指定浏览器将包体以附件的方式下载
    • 例如： Content-Disposition: attachment
    • 例如： Content-Disposition: attachment; filename=“filename.jpg”
  • 在 multipart/form-data 类型应答中，可以用于子消息体部分
    • 如 Content-Disposition: form-data; name="fieldName"; filename="filename.jpg"
    

///增加附件
/// helloworld 存为aaa.jpg  图片软件打不开，不是正确的图片格式
testsvr.py
#! /usr/bin/python
# -*- coding: utf-8 -*-
import socket
sock = socket.socket(socket.AF_INET,socket.SOCK_STREAM)
server_address = ("127.0.0.1",12345)
sock.bind(server_address)
sock.listen(100)  //处理100个链接


while True:
    conn,client_address = sock.accept()
    try:
        data = conn.recv(4096) //处理4096字节
        response = 'HTTP/1.1 200 OK\r\nContent-Disposition: attachment; filename=“aaa.jpg”\r\nContent-Length: 10\r\n\r\nHelloWorld'
        conn.send(response.encode())
    finally:
        conn.close()  
        
        
        
        
cyper
立刻想去看express.js 的sendFile(...)源码，是怎么实现的。会不会用了Transfer-Encoding: chunked 🤔
作者回复: 学习就应该这样。先抓包，确认结果后，再调试源码：-）

Laputa
老师，http传输过程中，一次只能传一个chunk还是可以传多个chunk
作者回复: 多个。body是一段字符流，而chunk只是更小的一段字符流，对于TCP协议而言，它们没有分别                             