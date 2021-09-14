Wireshark 过滤器

• 捕获过滤器
  • 用于减少抓取的报文体积
  • 使用 BPF 语法，功能相对有限

• 显示过滤器
  • 对已经抓取到的报文过滤显示
  • 功能强大
  
  
  
BPF 过滤器：Wireshark 捕获过滤器   
• Berkeley Packet Filter，在设备驱动级别提供抓包过滤接口，多数抓包工具都支持 此语法  tcpdump支持
• expression 表达式：由多个原语组成  



Expression 表达式
• primitives 原语：由名称或数字，以及描述它的多个限定词组成
  • qualifiers 限定词
    • Type：设置数字或者名称所指示类型，例如 host www.baidu.com     host
    • Dir：设置网络出入方向，例如 dst port 80     dst 发出
    • Proto：指定协议类型，例如 udp
    • 其他
• 原语运算符
  • 与：&& 或者 and
  • 或：|| 或者 or
  • 非：! 或者 not
• 例如：src or dst portrange 6000-8000 && tcp or ip6


限定词
Type：设置数字或者名称所指示类型
  • host、port      host表示ip  port端口
  • net ，设定子网，net 192.168.0.0 mask 255.255.255.0 等价于 net 192.168.0.0/24
  • portrange，设置端口范围，例如 portrange 6000-8000

Dir：设置网络出入方向
 • src、dst、src or dst、src and dst  源地址，目标地址
 • ra、ta、addr1、addr2、addr3、addr4（仅对 IEEE 802.11 Wireless LAN 有效）  网络链路层的

Proto：指定协议类型  支持有限，websocket,dns不支持
 • ether、fddi、tr、 wlan、 ip、 ip6、 arp、 rarp、 decnet、 tcp、udp、icmp、igmp、icmp、 igrp、pim、ah、esp、vrrp

其他
 • gateway：指明网关 IP 地址，等价于 ether host ehost and not host host    
 • broadcast：广播报文，例如 ether broadcast 或者 ip broadcast
 • multicast：多播报文，例如 ip multicast 或者 ip6 multicast
 • less, greater：小于或者大于




基于协议域过滤
• 捕获所有 TCP 中的 RST 报文  不是正常关链接，非4次握手
  • tcp[13]&4==4  代表图片中RST  tcp[13]代表第14个，每行4个字节，与4，表示把其他的去掉，只留下RST相关的 
    4的二进制0100
• 抓取 HTTP GET 报文
  • port 80 and tcp[((tcp[12:1] & 0xf0) >> 2):4] = 0x47455420   tcp的头部不是定长的，可能填充
    • 注意：47455420 是 ASCII 码的 16 进制，表示”GET ”        空格的acsii 20
    • TCP 报头可能不只 20 字节，data offset 提示了承载数据的偏移，但它以 4 字节为单位
    
  data offset  tcp[12:1]    data offset表示Word，右移2位  表示TCP头部后面的内容 :4表示取4个字节
  
  
  
  访问https://www.taohui.pub/    支持80端口   
    wireshark 捕获过滤器port 80 and tcp[((tcp[12:1] & 0xf0) >> 2):4] = 0x47455420
    可以看到get请求
     
  代表目标不可达(类型3)信息的ICMP数据包:     
   icmp[0]==3 //icmp协议头的第一个字节是否等于3
  可以在偏移值的后面以冒号分隔加上一个数值,代表数据长度:
   icmp[0:3]==0x030104
  
  
kakashi
不理解，http是应用层协议，它的全部数据不是封装在TCP的数据字段里吗？为什么可以在TCP的头部里过滤出来？还请老师解答下，谢谢。
作者回复: 这是因为实际的报文是序列化的字符，依次为数据链路层头部、IP头部、TCP头部、HTTP头部、HTTP包体，而捕获过滤器只能处理到TCP部分，这样，当明确了TCP头部长度后，自然就能延伸到HTTP头部，就可以过滤HTTP方法了



加载中……
关于那个GET过滤：
我得想法是这样的，假设 dataSet的的值是0001，但单位是4个字节，那么0001代表4个字节。
这个时候呢，经过如下运算：
tcp[12:1]取的是从第12个(从0计数)字节开始的1个字节(8位)：
dataset:0001(4位)
Reserved:000(3位)
NS: 0(1位)
也就是 0001 0000 & 0xf0 其实也就确定前4位的值就可以了，因为后4位也是0，结果是：
0001 0000 右移两位就是0000 0100 值就是4。可以看到经过运算后确实是4个字节。

结合上面的运算再想想原理应该是这样的：
tcp[12:1] 相当于把dataset 左移动(取了一个字节,而不是前4位)了4位(相当于放大了16倍)，实际上我们只需要放大4倍(单位是4字节)的值。这个时候只要右移2位，再缩小4倍，就是我们要的值了。
作者回复: 是的，不过对https就无能为力了



[ ]
tcp[12:1]&0xf0)>>2 这里的 >>2 可以这么理解吧，先右移 >>4 取前 4 位 (data offset 由一个字节前四位表示) 再 *4 (相当于 <<2 ) 算出 header 总字节数 ，最后结果相当于 >>2
作者回复: >>2 的原因，是data offset的单位不是字节，而是word，也就是4个字节的意思，因此>>2是乘以4的意思



我来也
我最近也有个疑问：tcpdump工作在哪一层？

查的资料好像说是工作在网络层。不知道对不对。
老师说的驱动层是数据链路层么？
（七层网络模型中的层概念）

tcpdump截取的包在wireshark上看，一个包是可以超过1500字节的。（不知道是不是软件显示层面做的效果）
现在也忘记tcpdump终端上显示的包长有没有超过1500了。😭
作者回复: 没有明确的层，因为：
1、tcpdump通过libpcap获取到报文头，它的BPF过滤器也是通过libpcap实现的；
2、如同wireshark，tcpdump也可以基于每层网络报文头进行分析。



Edward Lee
找到了一个不错的 BPF syntax 文档说明

https://biot.com/capstats/bpf.html
作者回复: 谢谢Edward的分享  



旺旺
用
gateway 192.168.1.1
指明gateway时提示'gateway' requires a name.
意思是gateway上面的用法不对吗？
作者回复: gateway后面跟着的不是IP地址，而是必须可以解析为MAC地址的名字，当下很少被使用



铿然
抓包后追踪TCP流后由于消息格式是gzip，显示是乱码，网上找了几个帖子基本解决思路都一样， 但是按照文章没有解决问题，见： https://blog.csdn.net/chrycoder/article/details/86525316

这个乱码问题要咋解决呢？
作者回复: gzip使用了huffman和LZ77两个压缩算法，基本上压缩后的内容增加或者减少1个字符都会导致完整解压失败。你可以从gzip解压失败由哪个字符导致入手，找到从wireshark里提取错误的位置


孜孜
对于get来说，我的抓包是data offset 是0101，然后&0xf0 就是0101 0000，>>2结果就是00010100，这个就是20，所以为什么不直接tcp[20:4]呢？
作者回复: 默认TCP头部是20字节，但它是可扩展的，当拥有timestamp等选项时，20个字节后并不是HTTP内容。详见第86课。


Geek_68d3d2
好蒙啊 为什么tcp[a:b]中为什么b一会儿以4位为单位一会儿又以8位为单位 这还要看a截取到的字节在报文中的语义而定吗？？
作者回复: 是的，不只TCP是这样，IP也是这样，有些长度是4字节为单位，有些是8字节为单位。这是因为这两个协议都是底层报文，其头部必须把消耗的字节最小化。
你可以再读下86课和115课做个对比，加深了解。


LEON
老师请教个问题。通过tcpdump 在抓包的时候能否定制策略如果数据包中有某一个head在进行抓包？目的是根据定位缩小抓包数量。
作者回复: 目前的tcpdump还做不到，tcpdump的捕获过滤器与wireshark是一样的


