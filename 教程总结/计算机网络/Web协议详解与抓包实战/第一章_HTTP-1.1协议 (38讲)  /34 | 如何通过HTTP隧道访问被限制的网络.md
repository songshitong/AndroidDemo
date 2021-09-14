
Http Tunnel 隧道
• 用于通过 HTTP 连接传输非 HTTP 协议格式的消息，常用于穿越防火墙
  • 建立隧道后，由于传输的并非 HTTP 消息，因此不再遵循请求/ 响应模式，已变为双向传输 
  
  
请求行
request-line = method SP request-target SP HTTP-version CRLF
  request-target = origin-form / absolute-form / authority-form / asterisk-form
    • origin-form = absolute-path [ "?" query ]
      • 向源服务器发起的请求，path 为空时必须传递 /
    • absolute-form = absolute-URI
      • 仅用于向正向代理 proxy 发起请求时，详见正向代理与隧道
    • authority-form = authority
        • authority = [ userinfo “@” ] host [ “:” port ]，指定源服务器
        • 仅用于 CONNECT 方法，例如 CONNECT www.example.com:80 HTTP/1.1
    • asterisk-form = "*“
        • 仅用于 OPTIONS 方法  
        
   
tunnel 隧道的常见用途：传递 SSL 消息 
 • 防火墙拒绝 SSL 流量怎么办？               图片左侧
 • 代理服务器没有证书，如何转发 SSL 流量？      图片右侧 
 
//todo socks5代理和http代理
//todo SSH协议
//SSH抓包 使用ssh进行远程登录

Http Tunnel 隧道的认证   需要用户名，密码   

穿透防火墙
查看443的端口监听 netstat -an | grep 443



我行我素
老师，请问下当ip被墙了之后，除了更换ip之外还有别的解决方案吗？
作者回复: 搭个代理


子杨
老师好，网关到服务器之间的连接是 HTTP 还是 TCP 的？我看流程图里写的是“打开到端口 443 的 TCP 连接”，如果是 TCP 连接服务器如何处理客户端的请求呢？
作者回复: HTTP协议没有连接的概念，所谓的连接，都是指TCP协议。
因为HTTP协议是基于TCP协议实现的，这门课没有按照通常的思路，先讲底层协议，再讲应用层协议，是因为希望大家可以更快的对照应用场景。如果你对连接有疑问的话，可以先跳到第5部分前2课先看一看连接的概念。


哈哈
翻墙软件是不是就是用的隧道
作者回复: 有些翻墙软件是用tunnel的
