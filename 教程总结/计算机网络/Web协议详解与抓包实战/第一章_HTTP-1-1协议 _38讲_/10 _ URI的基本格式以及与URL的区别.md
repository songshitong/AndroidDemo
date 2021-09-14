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
