什么是 DNS？    Domain Name System

• 一个用于将人类可读的“域名”（例如 www.taohui.pub）与服务器的IP地 址（例如 116.62.160.193）进行映射的数据库
• 递归查询
   • 根域名服务器    com->inacon->www  inacon权威服务器
   • 权威服务器

https://zhuanlan.zhihu.com/p/88260838
DNS服务器
常见的DNS服务器就是两种：权威解析服务器和递归解析服务器。递归解析服务器也可以叫做localDNS   
权威解析服务器
 DNS权威服务器保存着域名空间中部分区域的数据。如果DNS服务器负责管辖一个或多个区域时，称此DNS服务器为这些区域的权威服务器。
    根权威DNS或者二级权威服务器中的资源记录标记被指定为区域权威服务器的DNS服务器。通过资源记录中列出服务器，
    其他服务器就认为它是该区域的权威服务器。这意味着在 NS 资源记录中指定的任何服务器都被其他服务器当作权威的来源，
    并且能肯定应答区域内所含名称的查询。
递归服务器
递归服务器在正常情况下，初始的时候里面没有任何域名解析数据，里面所有的域名解析数据都来自于它到权威解析服务器的查询结果，
  一旦查询完毕，递归服务器就会根据TTL时间在本地形成一条缓存记录，并为用户提供DNS解析的查询服务，这是递归服务器的功能。

小知识：
问：当我们修改DNS解析服务记录的时候，我们应该在权威里面修改还是在递归里面修改？
答：所有的DNS解析记录里面类型的修改都是在权威解析服务器里面做修改。


 dns劫持  传递一个假的DNS server ，查询域名时返回错误的IP地址，错误的IP地址就可以对我们进行攻击，给我们错误的信息，木马
   运营商给我们展示广告，运营商DNS server 传给我们包含代理服务器或者广告地址的信息
   
   
递归查询
  查看 www.example.com
  
  
DNS 报文：查询与响应
• query：查询域名
• response：返回 IP 地址  


dig 工具查询   查询域名信息
dig image.baidu.com
```
; <<>> DiG 9.10.6 <<>> image.baidu.com
;; global options: +cmd
;; Got answer:
;; ->>HEADER<<- opcode: QUERY, status: NOERROR, id: 19545
;; flags: qr rd ra; QUERY: 1, ANSWER: 2, AUTHORITY: 0, ADDITIONAL: 1

;; OPT PSEUDOSECTION:
; EDNS: version: 0, flags:; udp: 4000
;; QUESTION SECTION:
;image.baidu.com.		IN	A

;; ANSWER SECTION:    //wireshark 报文中 answer中可以看到  CNAME的地址
image.baidu.com.	12	IN	CNAME	image.n.shifen.com.
image.n.shifen.com.	300	IN	A	110.242.69.132

;; Query time: 11 msec
;; SERVER: 10.24.10.6#53(10.24.10.6)
;; WHEN: Fri Jul 23 17:50:58 CST 2021
;; MSG SIZE  rcvd: 89
```    


DNS 报文
两部分 请求  questions 只在请求中     

 响应 其他部分都有
 
 wireshark 抓包 过滤器udp    dsn基于udp   不要挂代理，挂代理后默认的dns不是baidu了
  iamge.baidu.com   百度图片
  
  查询 Domain Name System (query)  
   第一个ID 2个字节16位    然后是一段flag
   qdcount 查询多少条       questions
   ancount   answer count  结果条数
   
  响应 Domain Name System(response)
  
  

Questions 格式         header在请求和响应都有，绿色是可变长度    answer只在响应中，query在请求和响应中都有
  qname  域名   以0位结尾 
  qtype,qclass都是2个字节

Answer 格式   绿色是可变长度
  结果把query带过来了，所以 answer 的name可以只引用
  answers的name image.baidu.com 只有c0 0c 指向udp报文的12字节的偏移，也就带过来的query  16进制c=12 二进制是00001100
  69=105 i  61=97 a   16进制=xx 10进制
  qname的长度为63字节，c0 0c 只有0c生效  c0=192
  

 answer 11代表压缩   2进制11  11000000=0xc0
 RFC-1035 的 “4.1.4. Message Compression”
 https://segmentfault.com/a/1190000009369381 参考自
 完整域名表示

比如表示 “www.google.com” 这样一个完整的域名，需要以下16个字节：

B0	B1	B2	B3	B4	B5	B6	B7	B8	B9	B10	B11	B12	B13	B14	B15
\3	w	w	w	\6	g	o	o	g	l	e	\3	c	o	m	\0
注意这里并不是把谷歌的 URL 使用简单的 char * 字符串复制上去，而是将每一段都分割开来。本例子中将域名分成了三段，分别是 www, google, com。
每一段开头都会有一个字节，表示后面跟着的那段域名的字节长度。最后当读到 \0 的时候，表示不再有数据了（这里和 char * 的 \0 含义有一点不同，虽然形式上是一样的）

标号表示
前文我们提到，域名的每一段，最长不能超过 63 个字节，因此在表示域名段长度的这个字节的最高两位（0xC0），必然是 0。这就引申出了这里的第二种用法。
这种表示法中，相当于一个指针，指代 DNS 报文中的某一个域名段。在解析一段 RR 数据段时，需要判断域长度嘛，判断的逻辑是：
如果最高两位是 00，则表示上面第一种
如果最高两位是 11，则表示这是一个压缩表示法。这一个字节去掉最高两位后剩下的6位，以及接下来的 8 位总共 14 位长的数据，指向 DNS 数据报文中的某一段域名（不一定是完整域名，参见第三种），可以算是指针吧。
比如 0xC150，表示从 DNS 正文（UDP payload）的 offset = 0x0150 处所表示的域名。0x0150 是将 0xC150 最高两位清零得到的数字。


混合表示
这就是上面两种的混合表示。比如说，我们假设前文表示 www.google.com 的完整域名的数据段处于 DNS 报文偏移 0x20 处，那么有以下几种可能的用法：
0xC020：自然就表示 www.google.com 了
0xC024：从完整域名的第二段开始，指代 google.com
0x016DC024：其中 0x6d 就是字符 m，因而 0x016D单独指代字符串 m；而第二段 0xC024 则指代 google.com，因此整段表示 m.google.com
 
 
 
 
ray
老师好，
请问Answer resource records, Authority resource records及Additional resource records的区别在哪？

谢谢老师的解答^^
作者回复: Answer就是你要查的域名；
Authority就是管理域名的权威DNS服务器；
Additional就是你并没有查询，但是服务器根据你查询的信息，认为有必要让你知道的。比如，Answer或者Authority告诉你一些域名，Additional可以返回域名的IP地址。


 WL
 请问老师在例子中的DNS响应中, 为什么image.baidu.com要首先定位到image.n.shifen.com再定位到IP地址而不是直接定位到IP地址, 中间的image.n.shifen.com起到的作用是什么?
 作者回复: 这是百度网络部门采用的一种域名解析策略，多了一层CNAME解析，更多的考虑可能是基于解耦合。
 
 
 龍少²⁰¹⁹
 浏览器对域名也有缓存吧？又是怎么缓存的呢，以及有什么缓存策略呢？
 
 另外，根域名响应是什么呢？还有各种类型除了A类型，MX，CNAME分别是什么作用呢？
 作者回复: 每种浏览器（包括不同的版本）都有独立的缓存策略，缓存时间从几十分钟到几小时不等，你需要根据浏览器去查询它的实现。
 MX用于邮件服务，CNAME表示别名指向，比如用一个域名指向另一个域名。
 
 
 qzmone
 老师，image.n.shifen.com 这个编码用QNAME规则编号后不是17个字节吧，应该是19个字节，而且对照wiresark抓到的包，对照老师最后讲的说17个字节，前面 image.n.shifen编码都能对应上，但是com编码为啥是c0 18呢，不应该得是4个字节吗，一个表示数量，另外是com的三个字节的ASICII编号，抓包这里没看明白。
 作者回复: 并不是完整的域名才能用cxxx来引用，部分域名也可以如此，你可以从UDP内容数18也就是24个字节，此时正好是com第一次出现，且以00结尾。这是一种编码技巧，可以节约更多的空间。
 



//todo
dns 服务器  8.8.8.8        dns与dhcp的关系 
https://developers.google.com/speed/public-dns/docs/using

dns-over-https
https://draveness.me/whys-the-design-dns-udp-tcp/
dns-over-quic

阿里 httpdns
https://help.aliyun.com/document_detail/30103.html?spm=a2c4g.11186623.6.543.119117f6f4YWgV

httpdns
简单来说自己做域名解析的工作，通过 HTTP 请求后台去拿到域名对应的 IP 地址，直接解决上述所有问题
解决 运营商LocalDNS劫持，域名递归查询的问题     
https://segmentfault.com/a/1190000039220772
http://www.52im.net/thread-2121-1-1.html

微信 Android高手DNS
解析慢并不是默认 LocalDNS 最大的“原罪”，它还存在一些其他问题：  //递归查询
稳定性。UDP 协议，无状态，容易域名劫持（难复现、难定位、难解决），每天至少几百万个域名被劫持，一年至少十次大规模事件。
准确性。LocalDNS 调度经常出现不准确，比如北京的用户调度到广东 IP，移动的运营商调度到电信的 IP，跨运营商调度会导致访问慢，甚至访问不了。
及时性。运营商可能会修改 DNS 的 TTL，导致 DNS 修改生效延迟。不同运营商的服务实现不一致，我们也很难保证 DNS 解析的耗时。