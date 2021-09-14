响应码分类： 4xx（一）

• 4xx：客户端出现错误
 • 400 Bad Request：服务器认为客户端出现了错误，但不能明确判断为以下哪种错误时使用此错误码。例如HTTP请求格式错误。
 • 401 Unauthorized：用户认证信息缺失或者不正确，导致服务器无法处理请求。 例如uri中缺少user，pwd
 • 407 Proxy Authentication Required：对需要经由代理的请求，认证信息未通过代理服务器的验证  机场，网吧
 • 403 Forbidden：服务器理解请求的含义，但没有权限执行此请求   服务器没有某个目录的权限
 • 404 Not Found：服务器没有找到对应的资源
 • 410 Gone：服务器没有找到对应的资源，且明确的知道该位置永久性找不到该资源  对于404的补充
 • 405 Method Not Allowed：服务器不支持请求行中的 method 方法   trace方法
 • 406 Not Acceptable：对客户端指定的资源表述不存在（例如对语言或者编码有要求），服务器返回表述列表供客户端选择。 
     比如不支持德文，日文
 • 408 Request Timeout：服务器接收请求超时  比如1分钟没发完请求
 • 409 Conflict：资源冲突，例如上传文件时目标位置已经存在版本更新的资源
 • 411 Length Required：如果请求含有包体且未携带 Content-Length 头部，且不属于 chunk类请求时，返回 411
 • 412 Precondition Failed：复用缓存时传递的 If-Unmodified-Since 或 IfNone-Match 头部不被满足
 • 413 Payload Too Large/Request Entity Too Large：请求的包体超出服务器能处理的最大长度
    wordpress的附件2M，很容易超出
 • 414 URI Too Long：请求的 URI 超出服务器能接受的最大长度   老的服务器4K，NGINX 32K
 • 415 Unsupported Media Type：上传的文件类型不被服务器支持
    wordpress 拒绝exe文件的上传，防止攻击
 • 416 Range Not Satisfiable：无法提供 Range 请求中指定的那段包体 文件100M，指定1G
 • 417 Expectation Failed：对于 Expect 请求头部期待的情况无法满足时的 响应码
 • 421 Misdirected Request：服务器认为这个请求不该发给它，因为它没有能力 处理。
 • 426 Upgrade Required：服务器拒绝基于当前 HTTP 协议提供服务，通过 Upgrade 头部告知客户端必须升级协议才能继续处理。
   比如http0.9太低了
 • 428 Precondition Required：用户请求中缺失了条件类头部，例如 If-Match
 • 429 Too Many Requests：客户端发送请求的速率过快     服务器限流，限速一般发送503
 • 431 Request Header Fields Too Large：请求的 HEADER 头部大小超过限制   服务器一般返回414，定义的级别太细了
 • 451 Unavailable For Legal Reasons：RFC7725 ，由于法律原因资源不可访问  商业上的原因
 
 telnet www.baohui.pub 80
 GET / HSFSFFDF
 结果400
  
 curl www.sina.con.cn -X TRACE -I 
 结果405
 
 
 
响应码分类： 5xx（一）

• 5xx：服务器端出现错误
  • 500 Internal Server Error：服务器内部错误，且不属于以下错误类型   没办法细分的错误
  • 501 Not Implemented：服务器不支持实现请求所需要的功能        需要服务器升级到相应的功能
  • 502 Bad Gateway：代理服务器无法获取到合法响应              或者 代理服务器连接不到源服务器  无法从上有获得响应 502是失败 504是超时
  • 503 Service Unavailable：服务器资源尚未准备好处理当前请求   限速，并发连接的限制
  • 504 Gateway Timeout：代理服务器无法及时的从上游获得响应      超时了，代理服务器设置的时间可能过短
  • 505 HTTP Version Not Supported：请求使用的 HTTP 协议版本不支持  可能不支持http/2.0
  • 507 Insufficient Storage：服务器没有足够的空间处理请求      磁盘满了，通常见不到，有安全问题，暴露服务器信息
  • 508 Loop Detected：访问资源时检测到循环
  • 511 Network Authentication Required：代理服务器发现客户端需要进行身份验证才能获得网络访问权限  机场，网吧
  

一般不认识的响应码，执行对应系列的00码  收到555，执行500的逻辑