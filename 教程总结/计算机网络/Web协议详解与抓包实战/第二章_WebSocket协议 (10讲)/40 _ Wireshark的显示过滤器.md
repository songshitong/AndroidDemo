显示过滤器的过滤属性   只能用于wireshark

• 任何在报文细节面板中解析出的字段名，都可以作为过滤属性
  • 在视图->内部->支持的协议面板里，可以看到各字段名对应的属性名   view->inernals->supported protocols 支持2600的协议
    • 例如，在报文细节面板中 TCP 协议头中的 Source Port，对应着过滤属性为 tcp.srcport
    
    
过滤值比较符号
  英文和符号都可以 
  contains 包含    只能使用英文
  matches 支持正则    
  
  

过滤值类型   过滤出来的类型是怎样的   支持的协议面板可以看到过滤值类型
 • Unsigned integer：无符号整型，例如 ip.len le 1500
 • Signed integer：有符号整型
 • Boolean：布尔值，例如 tcp.flags.syn
 • Ethernet address：以:、-或者.分隔的 6 字节地址，例如 eth.dst == ff:ff:ff:ff:ff:ff
 • IPv4 address：例如 ip.addr == 192.168.0.1
 • IPv6 address：例如 ipv6.addr == ::1
 • Text string：例如 http.request.uri == "https://www.wireshark.org/"  
    
 boolean类型直接输入就可以了，http.request
 
 
 

多个表达式间的组合
  
  
  
其他常用操作符
• 大括号{}集合操作符
   • 例如 tcp.port in {443 4430..4434} ，实际等价于 tcp.port == 443 || (tcp.port >= 4430 && tcp.port ⇐ 4434)
• 中括号[]Slice 切片操作符
   • [n:m]表示 n 是起始偏移量，m 是切片长度
     • eth.src[0:3] == 00:00:83
   • [n-m]表示 n 是起始偏移量，m 是截止偏移量
     • eth.src[1-2] == 00:83
   • [:m]表示从开始处至 m 截止偏移量
     • eth.src[:4] == 00:00:83:00
   • [m:]表示 m 是起始偏移量，至字段结尾
     • eth.src[4:] == 20:20
   • [m]表示取偏移量 m 处的字节
     • eth.src[2] == 83
   • [,]使用逗号分隔时，允许以上方式同时出现
     • eth.src[0:3,1-2,:4,4:,2] ==00:00:83:00:83:00:00:83:00:20:20:83    
     
     
可用函数
upper   Converts a string field to uppercase.
lower   Converts a string field to lowercase.
len     Returns the byte length of a string or bytes field.
count   Returns the number of field occurrences in a frame.
string  Converts a non-string field to a string.       



显示过滤器的可视化对话框   记不住所有的协议
  analyze->display filter expression  或者 搜索右边的表达式
  



加载中……
tcp.port in {443 4430..4434} 和tcp.port == 443 || (tcp.port >= 4430 && tcp.port ⇐ 4434) 应该不是等价的。如果有一个报文src的端口是56789，dest的端口是80
第一个过滤器匹配的结果是false，因为这个过滤的时候时候使用同一个字段来匹配。
第二个过滤器匹配的结果是true，因为这个过滤的时候是用整个报文的字段来匹配，56789>=4430 && 80⇐ 4434
作者回复: 非常严谨，谢谢指正：-）


  