http/2.0 解决由stateless导致的http/1.1重复的头部信息   头压缩hpack


HPACK 头部压缩
• RFC7541
• 三种压缩方式
  • 静态字典
  • 动态字典
  • 压缩算法：Huffman 编码（最高压缩比 8:5）  hufffman压缩程度有限，最多8:5
  
静态字典
 • https://http2.github.io/http2spec/compression.html#static.table.definition    61项静态字典
 • 含有 value 的表项    
     Index16    Header Name accept-encoding    Header Value gzip,deflate   可以用16表示，节省大量空间
 • 不含有 value 的表项   value的值比较广泛，会占用大量空间，没有写在静态表中
 
 static table
 https://datatracker.ietf.org/doc/html/rfc7541#appendix-A
 
 

HPACK 压缩示意
  • 同一个索引空间的 HEADER 表 
  62代表静态表中的user-agent
  静态表和动态表都没有时，采用Huffman编码
  
索引表用法示意
  只有path改变时，其他的头部使用index索引代替
  
  
HPACK 压缩比：h2load
h2load https://blog.cloudflare.com -n 4
```
starting benchmark...
spawning thread #0: 1 total client(s). 4 total requests
TLS Protocol: TLSv1.3
Cipher: TLS_AES_256_GCM_SHA384
Server Temp Key: X25519 253 bits
Application protocol: h2
progress: 25% done
progress: 50% done
progress: 75% done
progress: 100% done

finished in 2.32s, 1.72 req/s, 65.38KB/s
requests: 4 total, 4 started, 4 done, 4 succeeded, 0 failed, 0 errored, 0 timeout
status codes: 4 2xx, 0 3xx, 0 4xx, 0 5xx
traffic: 151.90KB (155541) total, 1.29KB (1316) headers (space savings 53.86%), 150.32KB (153924) data
                     min         max         mean         sd        +/- sd
time for request:   369.25ms    675.02ms    451.70ms    148.99ms    75.00%
time for connect:   515.74ms    515.74ms    515.74ms         0us   100.00%
time to 1st byte:      1.08s       1.08s       1.08s         0us   100.00%
req/s           :       1.72        1.72        1.72        0.00   100.00%    
```
 traffic: 151.90KB (155541) total, 1.29KB (1316) headers (space savings 53.86%), 150.32KB (153924) data
 节省空间 53.86%
  

访问一次  h2load https://blog.cloudflare.com -n 1 | tail -6 | head -1    访问一次只有静态表和Huffman编码起作用
traffic: 38.20KB (39118) total, 525B (525) headers (space savings 26.68%), 37.58KB (38481) data
访问两次 h2load https://blog.cloudflare.com -n 2 | tail -6 | head -1     访问两次动态表开始发挥作用
traffic: 76.06KB (77885) total, 748B (748) headers (space savings 47.55%), 75.16KB (76962) data
访问四次  h2load https://blog.cloudflare.com -n 4 | tail -6 | head -1
traffic: 151.85KB (155499) total, 1.24KB (1274) headers (space savings 55.33%), 150.32KB (153924) data
  
 h2load 安装 https://nghttp2.org/documentation/h2load-howto.html
  HTTP/2 benchmarking tool 
  
  
  

一步

Mac 电脑直接执行 brew install nghttp2 ,就可以使用 h2load 工具了
作者回复: 很好的分享！


Geek_Maple
求老师解释：访问四次为什么会更高呢？第二次就有动态表了，是不是说h2load是把几次访问的节省空间做的加权平均。
作者回复: 这是因为HTTP请求的头部是变化的，这与网站、客户端都有关系。如果是不变的，第二次与第四次没有区别。有些头部多次访问中可能会有更新，访问越多不变的概率提高，所以压缩率有所增加


02帆
老师，您好，两端同时维护一张静态一张动态两张表，这样是不是时刻有内存，cpu等资源消耗呢，这样会不会给server端有较大的负载呢...测试环境的公司首页用h2load进行h2的10W请求，100并发的压测，报头压缩只有23%...
作者回复: 是的，主要是内存消耗，所以每个TCP连接最多只能处理http2_max_requests个连接，就会强制关闭重连。



Binary Pikachu
老师请问 动态表是在服务端还是客户端呢，他们如何能保证读到的动态表的内容是一致的呢？我感觉是不是客户端和服务端各维持一份（对同一个connection）然后通过相同的算法，在两侧保持一致？
作者回复: 是的，两边各维护一份，通过相同的算法保持一致！





  