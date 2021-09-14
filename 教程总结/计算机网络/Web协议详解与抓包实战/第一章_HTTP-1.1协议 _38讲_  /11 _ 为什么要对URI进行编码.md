为什么要进行 URI 编码

• 传递数据中，如果存在用作分隔符的保留字符怎么办？
• 对可能产生歧义性的数据编码
  • 不在 ASCII 码范围内的字符
  • ASCII 码中不可显示的字符
  • URI 中规定的保留字符
  • 不安全字符（传输环节中可能会被不正确处理），如空格、引号、尖括号等

示例：
https://www.baidu.com/s?wd=?#!   ?#是URI分隔符
https://www.baidu.com/s?wd=极客 时间 
https://www.baidu.com/s?wd=极客 ‘>时 间

开发库对URI编码可能不同，不同网络环境对URI编码的处理策略可能不同



保留字符与非保留字符

• 保留字符
  • reserved = gen-delims / sub-delims  主要保留字符  非主要保留字符
    • gen-delims = ":" / "/" / "?" / "#" / "[" / "]" / "@"
    • sub-delims = "!" / "$" / "&" / "'" / "(" / ")" / "*" / "+" / "," / ";" / "="

• 非保留字符
  • unreserved = ALPHA / DIGIT / "-" / "." / "_" / "~" 
    • ALPHA: %41-%5A and %61-%7A
    • DIGIT: %30-%39
    • -: %2D .: %2E _: %5F   特殊符号
    • ~: %7E，某些实现将其认为保留字符  

URI 百分号编码
• 百分号编码的方式
  • pct-encoded = "%" HEXDIG HEXDIG    16进制
    • US-ASCII：128 个字符（95 个可显示字符，33 个不可显示字符）
    • 参见：https://zh.wikipedia.org/wiki/ASCII
  • 对于 HEXDIG 十六进制中的字母，大小写等价

• 非 ASCII 码字符（例如中文）：建议先 UTF8 编码，再 US-ASCII 编码

• 对 URI 合法字符，编码与不编码是等价的
   • 例如，“URI 转换”既可以“URI%e8%bd%ac%e6%8d%a”，也可以 “%55%52%49%e8%bd%ac%e6%8d%a2”
      • https://www.baidu.com/s?wd=URI%20%e8%bd%ac%e6%8d%a2
      • https://www.baidu.com/s?wd=%55%52%49%20%e8%bd%ac%e6%8d%a2


ascii
https://zh.wikipedia.org/zh-hans/ASCII




aaa
继续追问blob:http的问题，那这样是不是说这类链接也是一个合法的url？另外关于hier-part有相关的文档格式说明吗？我应该在哪查阅？谢谢
作者回复: 从RFC的ABNF规范上来说，它符合语法。查看RFC上的ABNF定义，https://tools.ietf.org/html/rfc3986

乐只君子
rfc1738 中对 "[" "]" 定义到了 national 中，但是 natiional 却没有使用，请问老师了解过原因吗？
作者回复: 主要是IPv6地址使用[]，而URI中允许出现IPv6地址，所以path中的[]应该编码。有些URL处理中间件会报错。
事实上1738这个文档本身在Unsafe那一节就写明白了，不推荐直接使用[]，应当编码后再用。RFC3986也提过。

bool
老师好，请问基于何种字符编码格式对 uri 进行编码的？
作者回复: 建议使用UTF-8，视频中3分以后有提及