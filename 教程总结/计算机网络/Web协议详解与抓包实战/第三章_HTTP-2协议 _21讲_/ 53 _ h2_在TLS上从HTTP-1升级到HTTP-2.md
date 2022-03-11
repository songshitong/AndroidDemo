

TLS 通讯过程  见图
  验证身份      client hello       server hello
  达成安全套件共识   check certificate validity
  传递秘钥     key generation
  加密通讯
  

Next Protocol Negotiation (NPN)
 • SPDY 使用的由客户端选择协议的 NPN 扩展  
   服务器返回多种协议，客户端选择使用哪种协议
   
   
Application-Layer Protocol Negotiation Extension  http/2做的改进  应用层协议协商
RFC7301
   客户端告诉支持哪些协议  list  of protocols
   服务器进行选择   selected protocol
   
   
   4	0.004445	192.168.0.102	60.28.216.239	TLSv1.2	571		Client Hello
     TLS中
     Extension: application_layer_protocol_negotiation (len=14)
         Type: application_layer_protocol_negotiation (16)
         Length: 14
         ALPN Extension Length: 12
         ALPN Protocol
             ALPN string length: 2   支持两种协议 h2  http/1.1
             ALPN Next Protocol: h2 
             ALPN string length: 8
             ALPN Next Protocol: http/1.1
             
   6	0.017867	60.28.216.239	192.168.0.102	TLSv1.2	1506		Server Hello
    Extension: application_layer_protocol_negotiation (len=5)
        Type: application_layer_protocol_negotiation (16)
        Length: 5
        ALPN Extension Length: 3
        ALPN Protocol
            ALPN string length: 2
            ALPN Next Protocol: h2   服务器选择h2
            
   后面是magic 帧   setting帧        
   
   
 
 
一步

ALPN是因为HTTP2才出现的，而且ALPN信息是在TLS握手报文进行传输的，这样的话是不是要修改TLS握手报文的格式？还是说以前就留有没有使用的扩展位置，
   现在只是把ALPN信息放到了预留的扩展位置上了？
作者回复: 对


一步

在 HTTP/1.1 的时代， TLS 握手的时候没有发送 ALPN 扩展信息吗？ 来告诉服务器，客户端支持的协议

还是说TLS握手的时候一直都有ALPN 扩展信息，对于不支持 http/2 的服务选择http/1.1，对于支持的选择http/2 ?
作者回复: HTTP/1.1从上世纪就开始大范围使用，ALPN是2014年才出现的，而HTTP/2也是2015年使用，ALPN是因为HTTP2才出现的。
详见：https://tools.ietf.org/html/rfc7301



ChenJZ
老师：您好！有的资料上介绍：1．单向认证：客户端向服务器发送消息，服务器接到消息后，用服务器端的密钥库中的私钥对数据进行加密，然后把加密后的数据和服务器端的公钥一起发送到客户端，客户端用服务器发送来的公钥对数据解密，
然后在用传到客户端的服务器公钥对数据加密传给服务器端，服务器用私钥对数据进行解密，这就完成了客户端和服务器之间通信的安全问题，
但是单向认证没有验证客户端的合法性。
2．双向认证：
（1）客户端向服务器发送消息，首先把消息用客户端证书加密然后连同时把客户端证书一起发送到服务器端
（2）服务器接到消息后用首先用客户端证书把消息解密，然后用服务器私钥把消息加密，把服务器证书和消息一起发送到客户端
（3）客户端用发来的服务器证书对消息进行解密，然后用服务器的证书对消息加密，然后在用客户端的证书对消息在进行一次加密，连同加密消息和客户端证书一起发送到服务器端，
（4）到服务器端首先用客户端传来的证书对消息进行解密，确保消息是这个客户发来的，然后用服务器端的私钥对消息在进行解密这个便得到了明文数据。
我的问题是，1．单向认证主要用于客户验证服务器吧？其中的第三步（用传到客户端的服务器公钥对数据加密传给服务器，...）是不是多余的啊？
2．双向认证主要用于客户端和服务器端的双向认证，第（3）步有必要再传递客户端 的证书吗？第（1 ）步不是已经传过了吗？谢谢！！
作者回复: 1、你说的单向认证在理论上可以起到加密作用，但现实中不会使用，成本太高了，对称加密才是真正解决消息加密的算法；
2、你说的单向、双向认证，主要是对身分的验证，不是对消息格式的验证，原理同上，你可以先看下76和77课。


ray
老师好，
请问老师为什么tls1.3看不到Application-Layer Protocol Negotiation Extension？
我们应该如何察看tls1.3是否有作http2的升级？

谢谢老师的解答^^
作者回复: 对，TLS1.3取消了TLS1.2里通过extension来协商的方式，直接通过选择安全套件就可以了。如果TLS13的服务器主动降低，还会在Random字段的最后6字节做文章。这是它在RFC上的理由：The TLS 1.2 version negotiation mechanism has been deprecated in favor of a 
version list in an extension. This increases compatibility with existing servers that incorrectly implemented version negotiatio
 
 

Terry Hu
老师，今天重做www.sina.com.cn wireshark抓包的时候，发现在Server Hello包里面没有ALPN extensions了，TLS也升级到1.3了：
16 0.024179 49.7.40.183 10.210.5.45 TLSv1.3 304 Server Hello, Change Cipher Spec, Encrypted Extensions, Finished
没法看到server选择了h2协议了。您看看现在抓的包还一样吗？
作者回复: 是的，新浪已经升级到tls1.3了



airmy丶
老师您好！根据抓包的情况，不管是ws协议还是HTTP/2协议，都是需要基于TCP进行一次握手连接，然后才能有后续的h2c协议升级和基于TLS/SSL的h2会话协商。是这样理解的吗？
作者回复: 是的



kissingers

老师，随机数传送不用加密？premaster 传递是用什么加密的？
作者回复: 公钥传送不用加密。详见第四部分课程TLS协议中的DH协商协议

 
 
   
          

