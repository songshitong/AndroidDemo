请求行（一）

request-line = method SP request-target SP HTTP-version CRLF
 • method 方法：指明操作目的，动词
 • request-target = origin-form / absolute-form / authority-form / asterisk-form
 
 
request-target = origin-form / absolute-form / authority-form / asterisk-form

• origin-form = absolute-path [ "?" query ]
   • 向 origin server 发起的请求，path 为空时必须传递 /    实际产生内容的服务器

• absolute-form = absolute-URI    完整的URI
   • 仅用于向正向代理 proxy 发起请求时，详见正向代理与隧道

• authority-form = authority   建立VPN隧道时使用
   • 仅用于 CONNECT 方法，例如 CONNECT www.example.com:80 HTTP/1.1

• asterisk-form = "*“   
  • 仅用于 OPTIONS 方法 
  
  
  
  
HTTP-version 版本号发展历史：https://www.w3.org/Protocols/History.html
• HTTP/0.9：1991 只支持 GET 方法，过时
• HTTP/ 1.0：RFC1945，1996， 常见使用于代理服务器（例如 Nginx 默认配置）
• HTTP/ 1.1：RFC2616，1999    缓存，长连接，域名支持
• HTTP/ 2.0：2015.5 正式发布   



https://cloud.tencent.com/developer/article/1513007
http/1.0 增加post

https://www.runoob.com/http/http-methods.html
HTTP1.0 定义了三种请求方法： GET, POST 和 HEAD 方法。
HTTP1.1 新增了六种请求方法：OPTIONS、PUT、PATCH、DELETE、TRACE 和 CONNECT 方法


常见方法（RFC7231）
• GET：主要的获取信息方法，大量的性能优化都针对该方法，幂等方法    幂等，调用一次多次结果是相同的，对于分布式设计是有意义的
• HEAD：类似 GET 方法，但服务器不发送 BODY，用以获取 HEAD 元数据，幂等方法
• POST：常用于提交 HTML FORM 表单、新增资源等
• PUT：更新资源，带条件时是幂等方法
• DELETE：删除资源，幂等方法
• CONNECT：建立 tunnel 隧道
• OPTIONS：显示服务器对访问资源支持的方法，幂等方法   跨域访问，判断新的域支持什么方法
• TRACE：回显服务器收到的请求，用于定位问题。有安全风险 Changes with nginx 0.5.17 02 Apr 2007 *) Change: 
       now nginx always returns the 405 status for the TRACE method.
       nginx不再支持
 
查看支持的方法 option方法     
curl baidu.com -X OPTIONS -I
Allow:GET,HEAD,PUT ...  





用于文档管理的 WEBDAV 方法(RFC2518)  用于设计restfulapi   

• PROPFIND：从 Web 资源中检索以 XML 格式存储的属性。它也被重载，以允 许一个检索远程系统的集合结构（
    也叫目录层次结构）      通常目录都是树状结构

• PROPPATCH：在单个原子性动作中更改和删除资源的多个属性

• MKCOL：创建集合或者目录      make  columns

• COPY：将资源从一个 URI 复制到另一个 URI

• MOVE：将资源从一个 URI 移动到另一个 URI

• LOCK：锁定一个资源。WebDAV 支持共享锁和互斥锁。    支持多人协作的文件管理，锁定，解锁

• UNLOCK：解除资源的锁定     



WEBDAV 验证环境    验证webdav环境搭建

• 服务器
  • Nginx
  • http_dav_module 模块
  • nginx-dav-ext-module 模块

• 客户端
  • winscp
  
  xshell 链接站点，选择协议webdav
  

抓包：
1.11 webdav协议抓包.pcapng 过滤  
http.host==static.taohui.tech  
请求中 info
   MOVE /html/mirror.txt HTTP/1.1 移动文件