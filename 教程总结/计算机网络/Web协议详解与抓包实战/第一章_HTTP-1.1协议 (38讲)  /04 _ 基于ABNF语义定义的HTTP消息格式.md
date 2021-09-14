http请求
GET / HTTP/1.1    get方法 空格 path路径(这是根路径) 空格 http版本号 /r/n      请求行  request-line
Host: time.geedbang.org  host头部 域名 /r/n

http响应
HTTP/1.1 200 OK             200状态码 空格 描述(OK) /r/n                   响应行  status-line
Date: Tue, 26 Mar 2019 02:39:11 GMT      http头部
Content-Type: application/octet-stream 
Transfer-Encoding: chunked 
Connection: keep-alive

stat-line 分为request-line和status-line

上面的是口语化的表达方式  比如有的服务器实现是空格，有的是tab  Content-Type有的中划线，有的是下划线
那么怎么样的实现是合法的


ABNF{
  操作符
  核心规则
}
ABNF （扩充巴科斯-瑙尔范式）操作符

• 空白字符：用来分隔定义中的各个元素
   • method SP request-target SP HTTP-version CRLF  

• 选择 /：表示多个规则都是可供选择的规则
   • start-line = request-line / status-line  start-line即可以是请求行也可以是响应行

• 值范围 %c##-## ：  0-7等价于16进制30-37
    • OCTAL = “0” / “1” / “2” / “3” / “4” / “5” / “6” / “7” 与 OCTAL = %x30-37 等价

• 序列组合 ()：将规则组合起来，视为单个元素

• 不定量重复 m*n：
  • * 元素表示零个或更多元素： *( header-field CRLF )    http头部可以有，没有 以CRLF换行结尾
  • 1* 元素表示一个或更多元素，2*4 元素表示两个至四个元素

• 可选序列 []：   请求，http响应
  • [ message-body ]
  
  
ABNF （扩充巴科斯-瑙尔范式）核心规则
常用的
ALPHA
GIGIT
SP
HTAB
VCHAR
CR mac换行
LF linux换行  
CRLF
OCTET  8位数  一般二进制数据


基于 ABNF 描述的 HTTP 协议格式
HTTP-message = start-line *( header-field CRLF ) CRLF [ message-body ]  http消息
• start-line = request-line / status-line     起始行
  • request-line = method SP request-target SP HTTP-version CRLF
  • status-line = HTTP-version SP status-code SP reason-phrase CRLF

• header-field = field-name ":" OWS field-value OWS 
    • OWS = *( SP / HTAB )    空格或者tab
    • field-name = token
    • field-value = *( field-content / obs-fold )
· message-body = *OCTET  


mac  telnet   window xshell
输入telnet www.taohui.pub 80
出现
Trying 129.28.62.166...
Connected to www.taohui.pub.
Escape character is '^]'.

输入 GET /wp-content/plugins/Pure-Highlightjs_1.0/assets/pure-highlight.css?ver=0.1.0 HTTP/1.1
回车后继续输入   Host:www.taohui.pub
输入完毕，按两次回车即可

图片是请求报文与基于 ABNF 描述的 HTTP 协议格式 的对比


wireshark
microsoft: WLAN 网卡
npcap loopback adapter  127.0.0.1本地环回地址

port 80      bpf过滤器  捕获WLAN 网卡
找到对应的域名www.taohui.pub
双击这条请求进入详情
找到 Hypertext transfer Protocol  http协议
 点击Host: www.taohui.pub
   下面的16进制数据选中，开头48 6f 结尾 0d 0a   0d 0a是换行
    0d 0a 后面还有一个 0d 0a 是header和body的空行





ABNF(Augmented BNF) 官方文档
https://www.ietf.org/rfc/rfc5234.txt

巴科斯范式 以美国人巴科斯(Backus)和丹麦人诺尔(Naur)的名字命名的一种形式化的语法表示方法，用来描述语法的一种形式体系，是一种典型的元语言。
又称巴科斯-诺尔形式(Backus-Naur form)。它不仅能严格地表示语法规则，而且所描述的语法是与上下文无关的。它具有语法简单，表示明确，
便于语法分析和编译的特点                                          
                

