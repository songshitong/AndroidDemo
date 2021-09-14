HTTP 包体：承载的消息内容

• 请求或者响应都可以携带包体
  • HTTP-message = start-line *( header-field CRLF ) CRLF [ message-body ]
    • message-body = *OCTET：二进制字节流

• 以下消息不能含有包体
  • HEAD 方法请求对应的响应
  • 1xx、204、304 对应的响应
  • CONNECT 方法对应的 2xx 响应
  
  
 
两种传输 HTTP 包体的方式（一）

• 发送 HTTP 消息时已能够确定包体的全部长度
  • 使用 Content-Length 头部明确指明包体长度
    • Content-Length = 1*DIGIT    一个或多个数字
      • 用 10 进制（不是 16 进制）表示包体中的字节个数，且必须与实际传输的包体长度一致
        在header中使用10进制，体现了可见性
• 优点：接收端处理更简单  
   防火墙只基于Content-Length做处理，如果不通过Content-Length，可能有漏网之鱼








//Content-Length必须与内容一致
//python3 testsvr.py 启动
//HelloWorld 10个字符
//抓包本机地址 npcap loopback adapter    port 12345
//
//Content-Length 设为6   浏览器显示HelloW
// wireshark查看 tcp层数据  hellworld 是有的
//Content-Length 设为11  浏览器 该网页无法正常运作
//  chrome response为空
//  wireshark 没有详细的http协议的内容

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
        response = 'HTTP/1.1 200 OK\r\nContent-Length: 10\r\n\r\nHelloWorld'
        conn.send(response.encode())
    finally:
        conn.close()
        
        
  
  
吃饭饭
感觉 Content-Length 指定长度的这种在实际业务场景很少能用到吧？应该不定长才是主流吧
作者回复: 定长场景很多，例如将静态资源事先压缩好，再传输；例如后端渲染好的HTML页面；
  例如POST表单提交等        
  
  
温岭钟汉良
那个老师我能问下关于代码最后为啥要对response进行编码吗
作者回复: reponse分为两部分，第1部分叫做status和header，一定要编码，这是由RFC规范定义的，否则接收的浏览器或者中间件无法解析。第2部分是body，
  如果有多个message放在一个body中就需要编码，否则不是必须的  
  
WL
请问一下老师对于防火墙来说为什么当请求不通过Content_Length传输包体长度时防火墙就会把这些请求漏出去, 防火墙为啥这么设计, 这不是太容易就漏出去了吗
作者回复: 因为有些非商业级的开源waf的实现没有严格遵循RFC规范  