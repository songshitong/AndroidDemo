不同请求对用户体验的优先级不同    http/2可以对请求进行优先级调整
css 比较高    js 比较低   图片最低

Priority 优先级设置帧   
• 帧类型：type=0x2
• 不使用 flag 标志位字段   不能使用9字节的flag
• Stream Dependency：依赖流    依赖流完成后才去做
• Weight权重：取值范围为 1 到 256。默认权重16    值越大优先级越高
• 仅针对 Stream 流，若 ID 为 0 试图影响连接，则接收端必须报错
• 在 idle 和 closed 状态下，仍然可以发送 Priority 帧
header帧中
------------------------
|E|   Stream Dependency(31)
------------------------
|Weigth(8)|
________________________



数据流优先级
• 每个数据流有优先级（1-256）
• 数据流间可以有依赖关系
  流控的资源分配  a 12  b 4    a分配资源 12/16   b分配资源4/16
  c依赖于d  d完成了才能去做c    c完成又可以做a和b 此时a,b的资源分配才有意义
   假如E又加入了  e没有完成时，只要c完成是可以开始a,b的
   

exclusive 标志位     exclusive独家的，优势
  表示独占
  新加入的D，并且标志位为1表示 d独占a 此时b,c依赖于a,改变为依赖于d  
  
  
  
  
 
 神马翔
 js css image 的优先级是http协议内部控制的么 开发者无感知 没有做设置
 作者回复: 是浏览器等实现软件控制的，http协议并不关心传输的文件格式到底是什么 