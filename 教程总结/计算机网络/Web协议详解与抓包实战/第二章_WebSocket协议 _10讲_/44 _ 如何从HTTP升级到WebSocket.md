URI 格式
• ws-URI = "ws:" "//" host [ ":" port ] path [ "?" query ]
  • 默认 port 端口 80
• wss-URI = "wss:" "//" host [ ":" port ] path [ "?" query ]
  • 默认 port 端口 443
• 客户端提供信息
  • host 与 port：主机名与端口
  • shema：是否基于 SSL
  • 访问资源：URI
  • 握手随机数：Sec-WebSocket-Key        保证非常随机，不会出现异常请求
  • 选择子协议： Sec-WebSocket-Protocol   非必选
  • 扩展协议： Sec-WebSocket-Extensions   非必选
  • CORS 跨域：Origin
  
  
  websocket 基于http/1.1  需要进行协议升级
建立握手
 第一类 红色   必选的     GET  HTTP/1.1  不能是1.0
   Sec-WebSocket-Version:13 目标13是必选的
   服务器返回必须是 101
   Upgrade: websocket
 第二类 绿色  必选项
   服务器必须根据客户端传的 Sec-WebSocket-Key新生成base64
 第三类 orgin 跨域信息  
 第四类  extension
 
 

如何证明握手被服务器接受？预防意外
• 请求中的 Sec-WebSocket-Key 随机数
  • 例如 Sec-WebSocket-Key: A1EEou7Nnq6+BBZoAZqWlg==  给随机数编码base64
• 响应中的 Sec-WebSocket-Accept 证明值
  • GUID（RFC4122）：258EAFA5-E914-47DA-95CA-C5AB0DC85B11 RFC写死的值
  • 值构造规则：BASE64(SHA1(Sec-WebSocket-KeyGUID))
       • 拼接值：A1EEou7Nnq6+BBZoAZqWlg==258EAFA5-E914-47DA-95CA-C5AB0DC85B11
       • SHA1 值：713f15ece2218612fcadb1598281a35380d1790f   将拼接值sha1编码
       • BASE 64 值：cT8V7OIhhhL8rbFZgoGjU4DReQ8=    将16进制的sha1进行base64编码，必须是接受16进制的base64才行
       • 最终头部：Sec-WebSocket-Accept: cT8V7OIhhhL8rbFZgoGjU4DReQ8= 
       
       

一步
老师上一个问题回答，被识别成http协议是正常的，那如果没有省略呢，开头的schema 是ws://，这个不是websocket协议吗，这样怎么识别成http协议的呢？
作者回复: schema是不能这样传到request line中的，参见第12课      


一步
对于websocket建立握手那张图
请求行是 GET /?encoding=text HTTP/1.1
但是实际请求行为下面这样：
GET ws://demos.kaazing.com/echo?.kl=Y HTTP/1.1

那么对于 websocket建立握手， 请求行中 schema://host[:port] 是不能省略的吗？
如果省略会被识别成 http 协议吗？
 
作者回复: 可以省略，本来就是http/1.1协议，被识别成http协议才是正常的，只是接下来会升级到websocket协议，http2也是一样的 