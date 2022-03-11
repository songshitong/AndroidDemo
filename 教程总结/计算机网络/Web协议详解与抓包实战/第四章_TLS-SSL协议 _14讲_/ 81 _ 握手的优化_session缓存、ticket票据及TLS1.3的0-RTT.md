TLS消耗的1个或2个RTT的时间是用于安全性，对于应用层的信息传递并没有意义，TLS提供了缓存，ticket用于优化RTT时间

session 缓存：以服务器生成的 session ID 为依据
  第一次握手后会生成session ID，一段时间内使用缓存的session ID 取到秘钥，不重复生成
  需要client和sever在一定时间内存储 session ID，加密秘钥

 host www.baidu.com
  第二次访问百度
   client hello 
      带有sessionID
   server hello
   Change Cipher Spec
     This session reuses previously  negotiated keys  使用上一次的秘钥
 

 问题1   server存储sessionID 是在内存中，不同主机没办法分享秘钥信息
     需要通过
 问题2  server存储sessionID是消耗内存的，应该缓存多久呢


session ticket  使用session ticket 
 服务器共用一个密码key用来对session加密，客户保存，发送密文
 服务器只用来校验，解密使用


TLS1.3 的 0RTT 握手  第二次握手才可以完成，使用上一次的缓存信息
  本次请求的第一次握手就将秘钥和请求一起发送


0-RTT 面临的重放攻击   session id,session ticket 都有
  攻击者保存客户端改变数据库数据的post请求的报文
  然后不断向server发送该报文来改变用户状态
解决 需要设置一个合适的缓存时间 



一步

TLS1.3的 0RTT, 和 session 都需要服务器端为每个连接缓存一些信息，当连接数量级非常大的时候，需要消耗的服务端资源就非常多了

是不是应为这个弊端会导致现在 TLS1.3的 0RTT, 和 session 用的不是很多的？
作者回复: 也有安全性上面临重放攻击的原因


阳光梦
session tiket 没听明白，服务端和客户端如何得到对称密钥？使用什么解密的？谢谢
作者回复: 1、首次握手正常生成对称密钥，server将其放入ticket中，加密后发给client，client解密后保存。
2、后续握手时，client将加密ticket发给server，server解密后获得对称密钥。此时双方都有对称密钥，可以通讯。
3、为防止client恶意修改ticket信息，实际server还有一个加密hash，防止client延长ticket超时时间。
具体参见https://tools.ietf.org/html/rfc5077第4部分。


ray
老师好，
关于重放攻击我有个疑问，如果只是拦截加密封包，然后再次传输给server去更改DB资料的状态，是不是不需要0RTT就可以做到了？在没有密钥的情况下，任何时候拦截到的封包应该都是无法解密密的封包，只需要把这封包一直丢给server是不是就可以实现重放攻击，未必要通过0RTT才能实现？

谢谢老师的解答^^
作者回复: TLS通讯过程中是有加密后的序列号保护的，如果不是从握手处做重放攻击是无法生效的。


LEON
老师我的理解默认每一个TCP链接都需要一个SSL握手，如果多个链接是同一个事务。就需要复用SSL SESSION ID。这样理解对吗？在哪个连接上复用SL SESSION ID是服务器控制还是浏览器控制呢？
作者回复: 同一个事务为什么要使用多个TCP连接呢？
浏览器控制的，服务器只要接收到有效的session id，就复用之前的加密参数。








https://www.cnblogs.com/Janly/p/13858451.html
Session ticket重用
在会话ticket复用中，服务器不用为每个session保存状态，它用一个blob数据保存状态，然后将它发给客户端用来维护后来连接，会话ticket允许服务器将其存储状态委托给客户端，类似HTTP cookie一样。

一个会话ticket是一个加密的数据blob，其中包含需要重用的TLS连接信息，如会话key等，它一般是使用ticket key加密，因为ticket key服务器端也知道，在初始握手中服务器发送一个会话ticket到客户端，存储到客户端本地，当重用会话时，客户端发送会话ticket到服务器，服务器解密然后重用会话。

Session Ticket的安全考虑
会话ticket有潜在的安全问题，一些TLS加密组件如ECDHE-RSA-AES128-SHA256提供一个安全属性成为向前安全forward secrecy，如果黑客获得了服务器的证书私钥，他们也不能获得会话来破解。

使用TLS 会话ticket，偷窃了ticket key1后不会允许黑客来解密先前的会话，这是的ticket key非常有价值，为了保持向前安全forward secrecy, ticket key应该经常轮换。

会话ticket重用在Apache中可以用SSLTicketKeyDefault 配置，在nginx中使用ssl_session_tickets，它们都没有自动轮换ticket key的自动机制，只能通过重启apache nginx来重新加载或创建新的随机key。

负载平衡
使用负载平衡器时，这些复用技术会遇到挑战，对于一个服务器复用一个连接，它需要先前会话的key，如果先前会话在其他服务器上，新的服务器必须得到原来会话的key。

这个问题被CloudFlare 和 Twitter使用系统产生一个集中统一的key来解决，ticket key被一个集中的统一的服务器定期创建，安全地发给所有服务器，实现会话ticket共享需要你的架构有一个定制系统的抉择。