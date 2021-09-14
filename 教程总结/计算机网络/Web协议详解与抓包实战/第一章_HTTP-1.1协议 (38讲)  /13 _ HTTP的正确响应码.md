HTTP 响应行
status-line = HTTP-version SP status-code SP reason-phrase CRLF
• status-code = 3DIGIT   3个数字
• reason-phrase = *( HTAB / SP / VCHAR / obs-text )



响应码分类：1xx

• 响应码规范：RFC6585 （2012.4）、RFC7231 （2014.6）  可能存在自定义的情况

• 1xx：请求已接收到，需要进一步处理才能完成，HTTP1.0 不支持
  • 100 Continue：上传大文件前使用    迅雷中使用，可以抓包查看 
     • 由客户端发起请求中携带 Expect: 100-continue 头部触发
  • 101 Switch Protocols：协议升级使用   
     • 由客户端发起请求中携带 Upgrade: 头部触发，如升级 websocket 或者 http/2.0
  • 102 Processing：WebDAV 请求可能包含许多涉及文件操作的子请求，需要很长时间
    才能完成请求。该代码表示服务器已经收到并正在处理请求，但无响应可用。这样可 以防止客户端超时，并假设请求丢失
    
    
 
 
 
响应码分类： 2xx（一）

• 2xx：成功处理请求
  • 200 OK: 成功返回响应。
  • 201 Created: 有新资源在服务器端被成功创建。    http.response.code==201
  • 202 Accepted: 服务器接收并开始处理请求，但请求未处理完成。这样一个模糊的概念是有意如此设计，可以覆盖更多的场景。例如异步、
     需要长时间处理 的任务。
  • 203 Non-Authoritative Information：当代理服务器修改了 origin server 的
     原始响应包体时（例如更换了HTML中的元素值），代理服务器可以通过修改 200为203的方式告知客户端这一事实，方便客户端
     为这一行为作出相应的处理。 203响应可以被缓存。 
     203不被广泛接受
  • 204 No Content：成功执行了请求且不携带响应包体，并暗示客户端无需更新当前的页面视图。
  • 205 Reset Content：成功执行了请求且不携带响应包体，同时指明客户端需要更新当前页面视图。
  • 206 Partial Content：使用 range 协议时返回部分响应内容时的响应码     多段下载
  • 207 Multi-Status：RFC4918 ，在 WEBDAV 协议中以 XML 返回多个资源 的状态。        http.response.code==207
  • 208 Already Reported：RFC5842 ，为避免相同集合下资源在207响应码下重复上报，使用 208 可以使用父集合的响应码。
  
  
  

响应码分类： 3xx（一）

• 3xx：重定向使用 Location 指向的资源或者缓存中的资源。在 RFC2068 中规定客户端重定向次数不应超过 5 次，以防止死循环。
 • 300 Multiple Choices：资源有多种表述，通过 300 返回给客户端后由其 自行选择访问哪一种表述。由于缺乏明确的细节，
    300 很少使用。
 • 301 Moved Permanently：资源永久性的重定向到另一个 URI 中。
 • 302 Found：资源临时的重定向到另一个 URI 中。
 • 303 See Other：重定向到其他资源，常用于 POST/PUT 等方法的响应中。
 • 304 Not Modified：当客户端拥有可能过期的缓存时，会携带缓存的标识 etag、时间等信息询问服务器缓存是否仍可复用，
    而304是告诉客户端可以 复用缓存。
 • 307 Temporary Redirect：类似302，但明确重定向后请求方法必须与原请求方法相同，不得改变。
 • 308 Permanent Redirect：类似301，但明确重定向后请求方法必须与原请求方法相同，不得改变。          
    
    
    
    
吃饭饭
老师我想咨询个问题：当我使用这个指令 curl www.baidu.com -X OPTIONS -I，会显示 302，有的显示301，，这个错误码是不允许跨域导致的吗？
作者回复: 不是跨域，而是百度的/资源不允许OPTIONS方法访问。关于跨域，后续课程会演示，复杂请求需要先以OPTIONS方法判明访问权限    


bug maker🙄 wyc
put方法不是更新操作吗？为什么会返回201 created？
作者回复: PUT不只是更新资源，也可以创建资源。
RFC7231中对此有定义：The PUT method requests that the state of the target resource be created or replaced with the state defined by the representation enclosed in the request message payload.

If the target resource does not have a current representation and the PUT successfully creates one, then the origin server MUST inf
orm the user agent by sending a 201 (Created) response.


我行我素
老师，想请问下，如果想判断几百个域名是否可用，是将请求方式设置为HEAD是最优解？还有更快更节约资源的方式吗
作者回复: "可用"的定义不明确，如果是主机可达，用ICMP协议（例如ping命令）更节约资源。如果是进程可达，建立TCP连接却可。如果是提供HTTP服务，那就要看服务的实现方式了，尽量命中CACHE、尽量减少访问次数等都是优化目标