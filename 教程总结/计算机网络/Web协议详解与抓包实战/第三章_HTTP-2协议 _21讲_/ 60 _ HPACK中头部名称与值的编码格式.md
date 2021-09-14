frame中 是怎么对header编码的

HEADER 帧的格式
 ?代表可有可无
 E 标志位  exclusive
 header block fragment 头部在这编码
 padding 可选的
 
 
CONTINUATION 持续帧(type=0x9)  当header帧，PUSH_PROMISE太大时,跟一个持续帧
 • 跟在 HEADER 帧或者 PUSH_PROMISE 帧之后，补充完整的 HTTP 头部 
   --------
    Header Block Fragment(*)
   --------
    
    

同一地址空间下的静态表与动态表   
• 静态表：61 项
• 动态表：先入先出的淘汰策略   有上限的，超过淘汰
  • 动态表大小由 SETTINGS_HEADER_TABLE_SIZE 设置帧定义
  • 允许重复项
  • 初始为空   
  
  

字面编码
• 组成
  • header name 和 value 以索引方式编码     索引2表示method为get       2	:method	GET
  • header name 以索引方式编码，而 header value 以字面形式编码    authority   即可以使用Huffman，也可以不使用
  • header name 和 value 都以字面形式编码   name,value都没有在索引中出现
• 可控制是否进入动态表
  • 进入动态表，供后续传输优化使用
  • 不进入动态表
  • 不进入动态表，并约定该头部永远不进入动态表   
  
  

HEADER 二进制编码格式（1）
• 名称与值都在索引表中（包括静态表与动态表）
  • 编码方式：首位传 1，其余 7 位传索引号       对应59讲的n=7 有7位可用
    0  1  2  3  4  5  6  7
    1      Index（7+）    
  • 例如
    • method: GET在静态索引表中序号为 2，其表示应为 1000 0010，HEX 表示为 82  
    
  17	0.049457	192.168.219.147	60.28.216.241	HTTP2	849		HEADERS[1]: GET /
  Header Block Fragment: 82418bf1e3c2e835435c87a57255878440874148b1275ad1ffb7fe6f4f61e935b4ff3f7d…
  下面16进制  右键show as bit
  
  Header: :method: GET       16进制82  二进制10000010    name和value都在索引表中
      Name Length: 7
      Name: :method
      Value Length: 3
      Value: GET
      :method: GET
      [Unescaped: GET]
      Representation: Indexed Header Field
      Index: 2    索引位是2
      
  Header: :scheme: https 索引位是7
  
  
  
  
HEADER 二进制编码格式（2）
• 名称在索引表中，值需要编码传递，同时新增至动态表中
• 前 2 位传 01     6位用来编码
• 名称举例
  • if-none-match 在静态索引表中序号为 41，表示为 01101001，HEX 表示为 69
• 值举例
  • "5cb816f5-19d8”
  • value长度 15 个字节，采用 Huffman 编码(H 为1)   表中的H为1，采用Huffman编码，不采用是0
    • 10001100
  • 8c fe 5b 24 6f 05 c9 5b 58 2f c8 f7 f3   
  
 user-agent 的value非常多，不在静态表中 
   17	0.049457	192.168.219.147	60.28.216.241	HTTP2	849		HEADERS[1]: GET /
    Header: user-agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.107 Safari/537.36
        Name Length: 10
        Name: user-agent
        Value Length: 121
        Value: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.107 Safari/537.36
        user-agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.107 Safari/537.36
        [Unescaped: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.107 Safari/537.36]
        Representation: Literal Header Field with Incremental Indexing - Indexed Name
        Index: 58   索引号58
    58 传01 对应的二进制 01111010   111010=58
    01111010 后面一个代表length  第一位1 表示采用Huffman编码  11011100   1011100=92
     11011100 后面92字节就是value 可以对比wireshark选中的区域，数一下，就是92个
     
   

HEADER 二进制编码格式（3）
• 名称、值都需要编码传递，同时新增至动态表中
  • 前 2 位传 01      2-7填0即可
  
 Header: upgrade-insecure-requests: 1       名和值都不在静态表的
     Name Length: 25
     Name: upgrade-insecure-requests
     Value Length: 1
     Value: 1
     [Unescaped: 1]
     Representation: Literal Header Field with Incremental Indexing - New Name
 第一个字节 01000000   后面一个代表namde的length 10010010  1使用Huffman编码   10010=18
   name的长度18   往后数18个字节 正好剩两个字节
   00000001 00110001    value不使用Huffman编码，长度为1个字节   1比较简单不需要编码了
     00110001 是ascii编码
     
  
     
HEADER 二进制编码格式（4）
• 名称在索引表中，值需要编码传递，且不更新至动态表中
  • 前 4 位传 0000   使用后4位  4位在静态索引表   超出后使用59的编码
  

HEADER 二进制编码格式（5）
• 名称、值都需要编码传递，且不更新至动态表中
  • 前 4 位传 0000  
  
  
HEADER 二进制编码格式（6）
• 名称在索引表中，值需要编码传递，且永远不更新至动态表中
  • 前 4 位传 0001
  
  
HEADER 二进制编码格式（7）
• 名称、值都需要编码传递，且永远不更新至动态表中
  • 前 4 位传 0001  
  
  

动态表大小的两种控制方式
 •在 HEADER 帧中直接修改   对第三位的使用1
  0 1 2 3 4 5 6 7 8 9
  0 0 1  Max size(5+)
 •在 SETTING 帧中修改
   • SETTINGS_MAX_HEADER_LIST_SIZE (0x6)  
   
   

一步

对于HEADERS帧中，有个Weight 的值，为什么 在 wireshark 抓包显示还有个 Weight real 的值呢？
作者回复: 规范中值是1-256，但实际8bit只能表达2^8，也就是0-255，所以wireshark将实际值加1转换成规范中的值


一步

这个编码格式算法，几种规则完全记不住啊，之后又忘了
作者回复: 不用记住，知道这样的思想即可，这种编码思路对于底层协议设计很有参考意义


LearnAndTry
老师静态表中没value值的或者不在静态表中的name，在一次发送后存储到动态表中，二次发送的时候是不是直接发送动态表的index就可以
作者回复: 是的

子杨
陶老师，索引表是怎么传输的？在一个 stream 中，传输每个消息都需要传递索引表吗？这个索引表消耗的性能有多大？
作者回复: 各自按照相同规则独立生成索引表。不过一些CPU指令，没有什么计算量，性能消耗很小


airmy丶
老师您好，动态表初始的时候是空的，然后在头部解压缩的时候加入动态表。有两个问题想问下：
1、这个动态表是保存在哪里的呢？是在操作系统的内存吗？
2、是否会为每个连接都生成一个动态表？
作者回复: 是的


ray
老师好，
目前理解在http2协定下，假设client第一次发送一个request，server会依据request的header的编码格式，将request记录到索引表中。

若以上理解正确，在上述情境下，
请问client自己是否也会记录相同request的索引表呢？
如果不会client又是如何得知这次request将哪些header记录进索引表中的呢？
server回response时，client是不是也是用上述request的记录方式在记录response header呢？

谢谢老师的解答^^
作者回复: 会记录，这是动态表的基础


林帆
老师，你一开始是说`动态表大小由SETTINGS_HEADER_TABLE_SIZE进行定义`;
到后面你说的是`动态表大小由SETTINGS_MAX_HEADER_LIST_SIZE`进行控制修改`；
应该是SETTINGS_HEADER_TABLE_SIZE才对吧？
有些疑惑，求解答
作者回复: 为了便于理解，我说动态表中的头部个数可以控制。其实从实现层面，限制头部只是为了防止内存使用过大，因为传输路径上的层层代理服务器都涉及内存消耗。
当并发连接过大时，内存是很紧张的。所以，这两个设置帧都在从字节数上做限制，它们的单位都是Byte，其中前者是限制HPACK压缩后的字节数，默认4096字节，后者是压缩前的字节数，默认不限制，
而且后者是建议性的，并不强制各软件实现。细节可以查看https://tools.ietf.org/html/rfc7540#section-6.5.1





   