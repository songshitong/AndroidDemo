Cookie 是什么？ 

RFC6265, HTTP State Management Mechanism   状态管理机制       弥补http的无状态

保存在客户端、由浏览器维护、表示应用状态的 HTTP 头部   保存用户信息，用户行为(做了什么)
 • 存放在内存或者磁盘中
 • 服务器端生成 Cookie 在响应中通过 Set-Cookie 头部告知客户端（允许多 个 Set-Cookie 头部传递多个值）
 • 客户端得到 Cookie 后，后续请求都会 自动将 Cookie 头部携带至请求中
 
 

Cookie 与 Set-Cookie头部的定义

• Cookie 头部中可以存放多个 name/value 名值对
  • cookie-header = "Cookie:" OWS cookie-string OWS   ows可选空格
    • cookie-string = cookie-pair *( ";" SP cookie-pair )
      • cookie-pair = cookie-name "=" cookie-value

• Set-Cookie 头部一次只能传递 1 个 name/value 名值对，响应中可以含多个头部
  • set-cookie-header = "Set-Cookie:" SP set-cookie-string
    • set-cookie-string = cookie-pair *( ";" SP cookie-av )   ;分隔
      • cookie-pair = cookie-name "=" cookie-value
      • cookie-av：描述 cookie-pair 的可选属性 
      
      
登录 www.taohui.pub/wp-login.php      
  勾选preserve log  登录成功会重定向到其他页面，需要保留上一个页面日志
   登录请求中，响应头有很多个set-cookie  每一个是cookie pair  cookie对
   其他请求的请求头携带cookie 内容是多个cookie-pair
   
   
 
Set-Cookie 中描述 cookie-pair 的属性

cookie-av = expires-av / max-age-av / domain-av / path-av / secure-av / httponly-av / extension-av
 • expires-av = "Expires=" sane-cookie-date
   • cookie 到日期 sane-cookie-date 后失效    失效后，客户端应该丢弃
 • max-age-av = "Max-Age=" non-zero-digit *DIGIT
   • cookie 经过 *DIGIT 秒后失效。max-age 优先级高于 expires   max-age优先级高
 • domain-av = "Domain=" domain-value
   • 指定 cookie 可用于哪些域名，默认可以访问当前域名   域名可能有很多子域名
 • path-av = "Path=" path-value
   • 指定 Path 路径下才能使用 cookie     只有某些路径可以使用path
 • secure-av = "Secure“           
   • 只有使用 TLS/SSL 协议（https）时才能使用 cookie    
 • httponly-av = "HttpOnly“
   • 不能使用 JavaScript（Document.cookie 、XMLHttpRequest 、Request APIs）访问到 cookie
   
 
 Set-Cookie:path/wp/a;HttpOnly;
 
 
 
 Cookie 使用的限制
 
 • RFC 规范对浏览器使用 Cookie 的要求
   • 每条 Cookie 的长度（包括 name、value 以及描述的属性等总长度）至少要达到 4KB  浏览器要支持4kb
   • 每个域名下至少支持 50 个 Cookie
   • 至少要支持 3000 个 Cookie
 
 • 代理服务器传递 Cookie 时会有限制  代理服务器可能会限制cookie大小，4kb,8kb等
 
 
 
 
 Cookie 在协议设计上的问题 
• Cookie 会被附加在每个 HTTP 请求中，所以无形中增加了流量
• 由于在 HTTP 请求中的 Cookie 是明文传递的，所以安全性成问题（除非用 HTTPS）
• Cookie 的大小不应超过 4KB，故对于复杂的存储需求来说是不够用的    超过可能不支持，超过RFC没有要求
 
 
 
 我行我素
 所以对于记住登录状态的操作(自动过期和刷新)是在Cookie中没有设置过期时间，而是在服务端存储了(假设存在redis中)每次携带过去取值，如果有就说明登录并且刷新过期时间，如果没有就说明登录过期了？
 作者回复: 一般，服务器和客户端都要设过期时间，服务器的过期时间是提供有效期的主要手段，而客户端只是为了提升体验和部分安全性。
 
 
 WL
 请问一下老师RFC对cookie的要求中每个域名至少支持50个cookie, 浏览器至少要支持3000个cookie具体是什么意思, 是说在浏览器的一次请求中需要可以最多携带3000个以上的cookie, 浏览器的一次响应中最多可以携带50个以上的Set-Cookie吗?
 作者回复: 不是，指存储，参见RFC这句话：“Practical user agent implementations have limits on the number and
    size of cookies that they can store”
    
    
 随意吧
 cookie 不是只能通过服务setCookie， 客户端也可以通过js 将数据设置到cookie ，发请求的时候带上啊
 作者回复: 是的，通过JS中的document.cookie属性也可以写入Cookie。如果访问的是第一方域名，Cookie都会携带进请求，第三方域名则要看浏览器策略了
  
 
 小炭
 老师，您好！Cookie丢失最有可能是哪些原因导致的呢
 作者回复: 定位Cookie，先从最本质的抓包查看响应看起，如果http reponse中没有set-cookie字段，客户端是没法取到cookie的。
 或者说，http request没有携带cookie字段，服务器也取不到cookie，这两种都不能算是cookie丢失，你说的Cookie丢失，是指js取不到Cookie吗？
 
 
 
 
    