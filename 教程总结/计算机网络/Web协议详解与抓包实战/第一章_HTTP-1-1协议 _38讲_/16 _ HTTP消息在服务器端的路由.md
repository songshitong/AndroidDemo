Host 头部

• Host = uri-host [ ":" port ]   port可选
• HTTP/1.1 规范要求，不传递 Host 头部则返回 400 错误响应码   http/1.0没有规定host,当时域名较少
• 为防止陈旧的代理服务器，发向正向代理的请求 request-target 必须以 absolute-form 形式出现
  • request-line = method SP request-target SP HTTP-version CRLF
  • absolute-form = absolute-URI
     • absolute-URI = scheme ":" hier-part [ "?" query ]
     
     
规范与实现间是有差距的

• 关于 Host 头部：https://tools.ietf.org/html/rfc7230#section-5.4
• A client MUST send a Host header field in all HTTP/1.1 request messages.
• A server MUST respond with a 400 (Bad Request) status code to any HTTP/1.1 request message that lacks a Host header
   field and to any request message that contains more than one Host header field or a Host header field with an invalid
    field-value.     
    
  
Host 头部与消息的路由
  ngix处理流程
  
  
  
WL
请问一下老师Host头部部分的PPT中有一句话是为防止陈旧的代理服务器, 这个防止是防止陈旧服务器什么呢? 目的是让陈旧的代理服务器正确处理请求吗? 为什么绝对路径就会正确处理, 相对路径就不会呢? 是因为域名的相对路径在DNS服务器中匹配不到IP地址吗?
作者回复: 只识别http/1.0的代理服务器，是不认识Host头部的，但它会识别绝对URI包括里面的域名。
WL你学习过《Nginx核心知识100讲》，你可以试验下用telnet构造请求，Nginx如果找到绝对URI中的域名，就不会使用Host中的域名。所以URI中域名优先级更高。      