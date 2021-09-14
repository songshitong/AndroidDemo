http/2可以运行在TLS/SSL协议之上，也可以运行在TCP协议之上
  但是浏览器要求http/2要在TLS/SSL之上

每次连接使用的秘钥是不同的


Chrome 浏览器检测 HTTP/2 插件
 • HTTP/2 and SPDY indicator    检测当前网站是否支持http/2协议
  https://chrome.google.com/webstore/detail/http2-and-spdy-indicator/mpbpobfflnpcgagjijhmgnchggcjblin
  
 https://hpbn.co/http2/  这个网站不支持
 
 
 
在 HTTP/2 应用层协议之下的 TLS 层   
 tls在osi的表现层  tcp/ip模型的应用层
 

TLS1.2 的加密算法
• 常见加密套件

• 对称加密算法：AES_128_GCM    秘钥根据ECDHE生产
  • 每次建立连接后，加密密钥都不一样
• 密钥生成算法：ECDHE
  • 客户端与服务器通过交换部分信息，各自独立生成最终一致的密钥 
  
  

Wireshark 如何解密 TLS 消息？

• 原理：获得 TLS 握手阶段生成的密钥
   • 通过 Chrome 浏览器 DEBUG 日志中的握手信息生成密钥
• 步骤
 • 配置 Chrome 输出 DEBUG 日志    chrome会将ssl生成的秘钥写入这个文件
   • 配置环境变量 SSLKEYLOGFILE   系统的环境变量，指向一个mytest.txt
• 在 Wireshark 中配置解析 DEBUG 日志
   • 编辑->首选项->Protocols->TLS/SSL   将上述的文件配置到wireshark
     • (Pre)-Master-Secret log filename  
     
 Mac需要从terminal启动Chrome      
     https://www.jianshu.com/p/40c7a42c42b0
  
  https://www.cnblogs.com/fanyegong/p/12608983.html    需要指定ssl-key-log-file
 /Applications/Google\ Chrome.app/Contents/MacOS/Google\ Chrome --user-data-dir=/tmp/chrome --ssl-key-log-file=/Users/issmac/sslkeylogfile/keylogfile.log
 配置别名
     alias chromehttp2="xxx"
     
 捕获  www.sina.com.cn     解析log会导致非常卡    wireshark中protocol 为HTTP2
 查看请求 
   Transport Layer Security
   HyperText Transfer Protocol 2    http2运行在TLS之上   http2里面的内容是明文的了   

   
 wireshark要从建立连接开始抓取，要不然取不到秘钥   http2存在长连接
 
有些网站直接抓取域名抓取不到，可以先看udp的dns查询，拿到IP地址
 然后再抓取ip地址的报文
 

www.sina.com.cn  右键追踪http/2流
32	1.316030	10.24.61.65	36.51.252.81	HTTP2	170		HEADERS[3]: GET /edublk/college_json.js
37	1.403095	36.51.252.81	10.24.61.65	HTTP2	1362		HEADERS[3]: 200 OK, DATA[3]
   [3 Reassembled TCP Segments (4228 bytes): #34(1460), #35(1460), #37(1308)] tcp层发生分包合并
   HyperText Transfer Protocol 2    应用层展示同一个流，不同部分
      Stream: HEADERS, Stream ID: 3, Length 509, 200 OK
      Stream: DATA, Stream ID: 3, Length 3679 (partial entity body)
57	1.713407	36.51.252.81	10.24.61.65	HTTP2	1514		DATA[3], DATA[3]
    [12 Reassembled TCP Segments (16406 bytes): #39(1460), #40(1460), #42(1460), #44(1460), #45(1460), #47(1460), #49(1460), #50(1460), #52(1460), #54(1460), #55(1460), #57(346)]
    HyperText Transfer Protocol 2
         Stream: DATA, Stream ID: 3, Length 8192 (partial entity body)
    [2 Reassembled TLS segments (8201 bytes): #57(8183), #57(18)]
    HyperText Transfer Protocol 2
        Stream: DATA, Stream ID: 3, Length 8192 (partial entity body)   发生两次分包合并
68	1.812950	36.51.252.81	10.24.61.65	HTTP2	1323		DATA[3], DATA[3], DATA[3] (application/x-javascript)

 
 
二进制格式与可见性
• TLS/SSL 降低了可见性门槛      http/1.x有的也基于TLS，已经没有什么可见性可言了
  • 代理服务器没有私钥不能看到内容   
 

 
 
 

到不了的塔
mac系统中环境变量SSLKEYLOGFILE的设置方法和windows，linux不同，shell终端和GUI程序使用两套不同的环境变量设置，shell终端的环境变量可以在～/.bash_profile中添加，而GUI程序的环境变量设置（即本课程应该设置的）可以在命令行执行命令launchctl setenv SSLKEYLOGFILE 环境变量路径，之后就可以生效了
作者回复: 谢谢你的分享^_^

凌空飞起的剪刀腿
老师您好，这个方法可以抓去http1.1版本的https吗？
作者回复: 可以的


kakashi
这个算不算chrome的BUG，毕竟都能这样解密的话，https还有什么作用。
作者回复: 不算的，对于通讯双方而言，一定要能解密信息，只是要防着第三者。其中，浏览器是被多页面共享的，而JS、JavaApplet、Flash等是无法读取debug日志的。当然，如果client主机被攻破，那黑客直接读取页面内容即可，也不需要多此一举解析TLS层的



closer
老师 我在centos服务器上面用tcpdump抓包 那些数据包是https 但是在wireshark导入密钥 无法解密
作者回复: 导入密钥？哪来的密钥？只有RSA协商才有固定的密钥，否则密钥都是会话建立时即时生成的，只有记录下密钥生成时的日志（包括对方传来的公钥和自己生成的私钥），才能计算出密钥。你用的是什么密钥协商算法？ECDHE吗？


 WL
 请问一下老师我今天注意到wireshark抓取HTTP/2协议报文时，看到HTTP/2的请求和响应之间有大量的TCP和TLS的报文，我不太理解这些报文时做什么用的，我之前的理解是建立好HTTP/2的链接后所有报文都通过HTTP/2发送，所以很不理解这些TCP和TLS的报文有啥用处。
 作者回复: HTTP2是运行在TLS之上的，而这里的TLS是运行在TCP之上的，TCP则运行在IP之上的。为了传递HTTP2报文，多个TLS报文需要握手，一个DATA报文常分解为多个TCP报文传输。
 网络分层，各司其职，可以再回顾下第5课
 
 

airmy丶
老师您好！请问该怎么理解这个“可见性”的意思？
您在视频中说提到了 HTTP/1.1因为是用ASCII编码，对头部和内容都可以很容易的修改。那HTTP/2是用的二进制编码，可见性不那么好。不管是从chrom面板中还是wireshark都可以很清晰的看见请求头部字段。
我还从Chrome面板中复制了h2的curl请求(不知道这样合不合理)去通过终端请求，结果得到结果如下：
[root@localhost ~]# curl 'https://http2.akamai.com/demo/h2_demo_frame.html' -H 'authority: http2.akamai.com' -H 'pragma: no-cache' -H 'cache-control: no-cache' -H 'upgrade-insecure-requests: 1' -H 'user-agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.120 Safari/537.36' -H 'sec-fetch-mode: nested-navigate' -H 'accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3' -H 'sec-fetch-site: same-origin' -H 'referer: https://http2.akamai.com/demo' -H 'accept-encoding: gzip, deflate, br' -H 'accept-language: zh-CN,zh;q=0.9' --compressed -I
HTTP/1.1 200 OK
Server: Apache
ETag: "a36c5bf5b522a6a2bd2842c9cafb76d8:1441001436"
Last-Modified: Mon, 31 Aug 2015 06:09:25 GMT
Accept-Ranges: bytes
Content-Length: 39079
push: true
rtt: 127
ghost_ip: 104.71.159.169
ghost_service_ip: 23.3.104.110
client_real_ip: 112.94.43.29
client_ip: 112.94.43.29
myproto:
protocol_negotiation: h1
Expires: Thu, 17 Oct 2019 01:51:57 GMT
Cache-Control: max-age=0, no-cache, no-store
Pragma: no-cache
Date: Thu, 17 Oct 2019 01:51:57 GMT
Connection: keep-alive
Content-Type: text/html;charset=UTF-8
Accept-CH: DPR, Width, Viewport-Width, Downlink, Save-Data
Access-Control-Max-Age: 86400
Access-Control-Allow-Credentials: false
Access-Control-Allow-Headers: *
Access-Control-Allow-Methods: GET,HEAD,POST
Access-Control-Allow-Origin: *
Strict-Transport-Security: max-age=31536000 ; includeSubDomains

为啥响应还是HTTP/1.1 ？
作者回复: 1、这是因为chrome或者wireshark把二进制报文解析出后，以友好的字符串形式向你展示。在你用chrome时，是看不到Frame细节的，例如weight权重。
2、你访问的网站既支持http1也支持http2。如果想使curl使用http2，需要加入参数--http2。当然，首先你的curl版本要支持。可参考这篇文章：https://www.sysgeek.cn/curl-with-http2-support/ 
 
 
 

