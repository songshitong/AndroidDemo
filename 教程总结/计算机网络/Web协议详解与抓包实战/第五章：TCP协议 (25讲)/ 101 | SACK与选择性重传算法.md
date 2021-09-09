tcp的序列号采用的是累积确认的，上一讲的接受方没有收到报文5，但收到了报文6，7，8，他只能反复跟发送方说需要报文5，这样发送方
  知道报文5没有发过去，其实并不知道报文6，7，8有没有发过去，发送方要么采用消极的方法重传5，6，7，8，要么采用积极的方法只重传5，
  选择性重传只重传丢失的报文


仅重传丢失段 保守乐观
• 累积确认 Sequence 序号的问题
    • Client 无法告知收到了 Part4   
    • Server 发送窗口/Client 接收窗口停止
  当第4部分也丢失了，效率出现了问题

重传所有段 --积极悲观
• 重传所有段：积极悲观
   • 可能浪费带宽
• 仅重传丢失段：保守乐观
   • 大量丢包时效率低下


SACK：TCP Selective Acknowledgment  解决积极悲观和保守乐观
 • RFC2018
  4表示支持sack的功能
  5表示已经收到了哪些失序的报文段

引入SACK
  • 选择性确认
  第6步 ack num=201 表示第3部分201-300没有收到  sack=361-500表示第四部分361-500收到了
    server知道第4部分收到了，只需要重发第3部分即可 


SACK   left-right是确认的sequence number范围  中间可能有多个报文
• Left Edge of Block
• Right Edge of Block
抓包中老师     
 TCP OPTION - SACK
  left edge
  right edge
 TCP Dup Ack 重复报文确认
    查看client的next sequence number 以及 server的ack number 是14679
    但是下一个请求从20255开始，中间发生了丢包


meta-algorithmX
积极悲观：Go-Back-N【也称作 Reject (REJ)】
保守乐观：selective repeat ARQ【也称作 Selective Reject (SREJ)】

https://tools.ietf.org/html/rfc3366#page-8

另外，如果开启SACK，会存在针对SACK发送者的攻击手段：
攻击者可以给发送者一直发送SACK，遍历已经发送过的历史报文，从而消耗发送方的计算资源。详情见下面文章：
https://www.ibm.com/developerworks/cn/linux/l-tcp-sack/
作者回复: 很好的补充，谢谢meta-algorithmX同学




安民
老师，是不是有个TCP New Reno没有介绍
作者回复: 对，其实丢包驱动的拥塞控制算法非常的多，你可以看下wiki上列出的算法，原理大都相同，但是具体设置阀值、降低速度的参数不尽相同

