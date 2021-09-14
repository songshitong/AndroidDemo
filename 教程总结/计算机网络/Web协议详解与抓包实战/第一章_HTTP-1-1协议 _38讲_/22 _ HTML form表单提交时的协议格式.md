HTML FORM 表单
• HTML：HyperText Markup Language，结构化的标记语言（非编程语言） 文本文档
 • 浏览器可以将 HTML 文件渲染为可视化网页

///文档与表单的不同
• FORM 表单：HTML 中的元素，提供了交互控制元件用来向服务器通过 HTTP 协议提交信 息，常见控件有：
  • Text Input Controls：文本输入控件
  • Checkboxes Controls：复选框控件
  • Radio Box Controls ：单选按钮控件
  • Select Box Controls：下拉列表控件
  • File Select boxes：选取文件控件
  • Clickable Buttons：可点击的按钮控件
  • Submit and Reset Button：提交或者重置按钮控件
  
  

HTML FORM 表单提交请求时的关键属性   这三个关键属性与http请求相关的
  • action：提交时发起 HTTP 请求的 URI
  • method：提交时发起 HTTP 请求的 http 方法
    • GET：通过 URI，将表单数据以 URI 参数的方式提交
    • POST：将表单数据放在请求包体中提交   
  • enctype：在 POST 方法下，对表单内容在请求包体中的编码方式
    • application/x-www-form-urlencoded
      • 数据被编码成以 ‘&’ 分隔的键-值对, 同时以 ‘=’ 分隔键和值，字符以 URL 编码方式编码
    • multipart/form-data
      • boundary 分隔符
      • 每部分表述皆有HTTP头部描述子包体，例如 Content-Type
      • last boundary 结尾  
      

访问 protocol.taohui.tech/app/form.html   
  出现一个表单  表单的action都指定一个URL
     post提交
     get提交
     post+application/x-www-form-urlencoded 提交  
     post+multipart/form-data 提交
        文件+性别+姓名
        
        
    
multipart(RFC1521)：一个包体中多个资源表述

• Content-type 头部指明这是一个多表述包体
  • Content-type: multipart/form-data; boundary=----WebKitFormBoundaryRRJKeWfHPGrS4LKe

• Boundary 分隔符的格式
  • boundary := 0*69<bchars> bcharsnospace  0-69个
    • bchars := bcharsnospace / " "
    • bcharsnospace := DIGIT / ALPHA / "'" / "(" / ")" / "+" / "_" / "," / "-" / "." / "/" / ":"
     / "=" / "?"     
     
     
  
Multipart 包体格式(RFC822)

• multipart-body = preamble 1*encapsulation close-delimiter epilogue   preamble和epilogue通常不使用，被丢弃
  • preamble := discard-text
  • epilogue := discard-text
    • discard-text := *(*text CRLF)
  • 每部分包体格式：encapsulation = delimiter body-part CRLF
    • delimiter = "--" boundary CRLF      两个减号
    • body-part = fields *( CRLF *text )
       • field = field-name ":" [ field-value ] CRLF
         • content-disposition: form-data; name="xxxxx“
         • content-type 头部指明该部分包体的类型
  • close-delimiter = "--" boundary "--" CRLF    结束分隔符    
  
  

文件的抓包 Encapsulation资源表述
 包体  MIME Multipart Media Encapsulation, Type: multipart/form-data, Boundary: "---..."
  每一部分
   first boundary: ----  分隔符
   Encapsulated multipart part:
     content-disposition: form-data;name="name"\r\n\r\n        属性
   boundary： ---  
   Encapsulated multipart part: (text/plain)
     content-disposition: form-data;name="file1";filename="1.txt\r\n\r\n  文本文件
     
     
     

鸟人
我发现上传图片 文件之类 基本都是multipart/form-data 那么可以用application/x-www-form-urlencoded 上传图片文件么？为什么？
作者回复: 理论上可以，但实际上没有可行性，因为没有必要做URL编码（URL编码请参照第11课），URL编码效率很低，空间大消耗带宽也大CPU消耗也大
     

小炭
上传大文件实现进度条的效果是什么原理呢
作者回复: 按照Content-Length得到总上传字节数，再根据TCP write函数返回的累加字节数，可以得到已上传百分比     
    
    
Aaaaaaaaaaayou
如果上传很大的文件会把文件一次性全读取出来上传吗，还是边读边传，如果是边读边传，那具体是怎么工作的呢？
作者回复: 要看具体的编程语言了。
1、HTML表单是一个协议规范，各种语言都可以使用。自由度很大的语言里，你可以选择边读边传，具体做法的原因，基于你读取文件时，可以设置offset偏移量。比如1个100M的文件，第1次0-2M，第2次读2-4M，以此类推。
2、如Linux操作系统，还可以通过sendfile方法，由操作系统帮你完成这一操作。具体做法是，把 tcp连接socket，以及open file后的句柄都传给sendfile，即可。
3、如果你是用自由度很低的语言，例如js，这受制于浏览器的策略（防止恶意js脚本读取本地文件），你可以参考较新的版本，例如HTML5来操作。     


WL
老师再问一下问什么服务器中间件把表单内容直接映射到数据库中时, 了解表单提交时的包体格式对于解决安全问题有帮助, 有什么帮助, 这一点我好像没太理解.
作者回复: 简单来说，有些实现不当的代理服务器，希望获取表单内容，但没有正确的解析出表单


WL
老师请问一下, 表单提交的Content-type只有 application/x-www-form-urlencoded和multipart/form-data这两种形式吗? 如果请求头的Content-Type: application/json;charset=UTF-8, 这样以json方式传输数据算是表单提交吗, 如果不算表单提交, 那算是什么方式, 跟表单提交的优势劣势各是啥
作者回复: 不算，表单提交只是浏览器在帮忙编码，由于广为使用，服务器框架对其支持都比较好。如果自行编码，只是实现会稍麻烦些。



我在你的视线里
如何分析报文呢？那是16进制的数据表示什么呢？
作者回复: 实际传输的是二进制，以16进制显示时，主要用于分析报文头部，例如websocket帧就必须分析16进制格式及2进制格式（43课），而http2的帧格式也需要，
例如hpack头部压缩格式（57-60课），第5、6部分课程必须全程分析16进制报文头部


WL
老师再请教一下, boundary的值是浏览器自动指定的吗?
作者回复: 对


一步

一次 multipart/form-data 请求的 boundary 资源分隔符是一样的吗？除了 Last Boundary 后面多加了两个 --
作者回复: 对


旺旺
get到了，原来上传一个文件是这样传输给服务器的。
如果是一个大文件，那会发好多次的请求给服务器端吗？
作者回复: 就1个请求，但TCP协议层会自动拆分为多个IP报文。
在第3部分HTTP2协议可以看到，HTTP2会将1个请求拆分成多个DATA FRAME帧