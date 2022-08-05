当没有 URI 时

• 站长 A 欲分享一部电影 Forrest Gump 给 B，需要告诉：
  • 请使用 FTP 协议访问 mysite.net，端口是 8502
  • 登录用户名是user，密码pass
  • 进入到 /shared/movie 目录下
  • 转换为二进制模式        传输模式
  • 下载名为 Forrest Gump.mkv 格式的文件

• 有了 URI：ftp://user:pass@mysite.net:8502/shared/movie/Forrest Gump.mkv



什么是 URI

• URL： RFC1738 （1994.12），Uniform Resource Locator，表示资源的位置， 期望提供查找资源的方法

• URN：RFC2141 （1997.5），Uniform Resource Name，期望为资源提供持久 的、位置无关的标识方式，并允许简单地将多个命名
    空间映射到单个URN命名 空间
   • 例如磁力链接 magnet:?xt=urn:sha1:YNCKHTQC5C

• URI：RFC1630 （1994.6）、RFC3986 （2005.1，取代 RFC2396 和 RFC2732 ），Uniform Resource Identifier，
   用以区分资源，是 URL 和 URN 的超集，用以取代 URL 和 URN 概念
   
   
   

Uniform Resource Identifier 统一资源标识符

• Resource 资源
  • 可以是图片、文档、今天杭州的温度等，也可以是不能通过互联网访问的实体，例如人、 公司、实体书，也可以是抽象的概念，例如亲属
    关系或者数字符号
  • 一个资源可以有多个 URI   资源对应的URI一般不变

• Identifier 标识符
  • 将当前资源与其他资源区分开的名称   这是URI的目标 比如my father，我的关系网

• Uniform 统一
  • 允许不同种类的资源在同一上下文中出现   图片，文字
  • 对不同种类的资源标识符可以使用同一种语义进行解读
  • 引入新标识符时，不会对已有标识符产生影响        新的内容，视频
  • 允许同一资源标识符在不同的、internet 规模下的上下文中出现   
  
  
  
  
  URI 的组成
  
  • 组成：schema, user information, host, port, path, query, fragment
  authority{user information,host,port}
  hierarchical part{authority,path}
  例子：
  https://tools.ietf.org/html/rfc7231?test=1#page-7
  Username, password and port are optional 是可选的  host是必选的
  
  
  
  合法的 URI
  
  • ftp://ftp.is.co.za/rfc/rfc1808.txt
  
  • http://www.ietf.org/rfc/rfc2396.txt
  
  • ldap://[2001:db8::7]/c=GB?objectClass?one
  
  • mailto:John.Doe@example.com     向XX发送邮件
  
  • news:comp.infosystems.www.servers.unix tel:+1-816-555-1212    新闻
  
  • telnet://192.0.2.16:80/
  
  • urn:oasis:names:specification:docbook:dtd:xml:4.1.2
  
  
  
  
URI 格式（一） abnf的定义
 
• URI = scheme ":" hier-part [ "?" query ] [ "#" fragment ]

• scheme = ALPHA *( ALPHA / DIGIT / "+" / "-" / "." )
   • 例如：http, https, ftp,mailto,rtsp,file,telnet

• query = *( pchar / "/" / "?" )   以?开头，包含pchar,/,?

• fragment = *( pchar / "/" / "?" )

示例：https://tools.ietf.org/html/rfc7231?test=1#page-7  


fragment   https://www.jianshu.com/p/2c07fbb52b45
主要资源是由 URI 进行标识，URI 中的 fragment 用来标识次级资源。我理解看来，fragment 主要是用来标识 URI 所标识资源里的某个资源。
在 URI 的末尾通过 hash mark（#）作为 fragment 的开头，其中 # 不属于 fragment 的值。
https://domain/index#L18
这个 URI 中 L18 就是 fragment 的值。这有哪些特殊的地方呢？
# 有别于 ?，? 后面的查询字符串会被网络请求带上服务器，而 fragment 不会被发送的服务器；
fragment 的改变不会触发浏览器刷新页面，但是会生成浏览历史；
fragment 会被浏览器根据文件媒体类型（MIME type）进行对应的处理；
Google 的搜索引擎会忽略 # 及其后面的字符串。

https://www.ruanyifeng.com/blog/2011/03/url_hash.html
1 #代表网页中的一个位置。其右面的字符，就是该位置的标识符。比如，
http://www.example.com/index.html#print
就代表网页index.html的print位置。浏览器读取这个URL后，会自动将print位置滚动至可视区域。
为网页位置指定标识符，有两个方法。一是使用锚点，比如<a name="print"></a>，二是使用id属性，比如<div id="print" >。
2 window.location.hash读取#值
window.location.hash这个属性可读可写。读取时，可以用来判断网页状态是否改变；写入时，则会在不重载网页的前提下，创造一条访问历史记录。



URI 格式（二）

hier-part = "//" authority path-abempty / path-absolute / path-rootless / path-empty

  • authority = [ userinfo "@" ] host [ ":" port ]   验证信息
     • userinfo = *( unreserved / pct-encoded / sub-delims / ":" )
     • host = IP-literal / IPv4address / reg-name      reg-name可以是localhost
     • port = *DIGIT

示例：https://tom:pass@localhost:8080/index.html






URI格式（三）: hier-part

• path = path-abempty/ path-absolute/ path-noscheme / path-rootless / path-empty   5种
   • path-abempty = *( “/” segment )
       • 以/开头的路径或者空路径
   • path-absolute = “/” [ segment-nz *( “/” segment ) ]
       • 以/开头的路径，但不能以//开头
   • path-noscheme = segment-nz-nc *( “/” segment )
       • 以非:号开头的路径
   • path-rootless = segment-nz *( “/” segment )
       • 相对path-noscheme，增加允许以:号开头的路径
   • path-empty = 0<pchar>
       • 空路径
       
       
       
       
       
相对 URI

URI-reference = URI/relative-ref    
  • relative-ref = relative-part [ "?" query ] [ "#" fragment ]
  • relative-part = "//" authority path-abempty / path-absolute / path-noscheme / path-empty

https://tools.ietf.org/html/rfc7231?test=1#page-7  绝对URI
/html/rfc7231?test=1#page-7                        相对URI





https://www.ietf.org/rfc/rfc1738.txt
RFC1738 RFC2141 RFC1630 RFC3986
