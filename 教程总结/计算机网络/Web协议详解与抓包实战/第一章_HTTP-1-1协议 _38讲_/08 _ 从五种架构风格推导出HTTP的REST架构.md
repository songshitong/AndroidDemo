分布式的架构
5 种架构风格   某一类类似的风格
• 数据流风格 Data-flow Styles  协议的分层，ngix都是数据流风格
  • 优点：简单性、可进化性、可扩展性、可配置性、可重用性
• 复制风格 Replication Styles  
  • 优点：用户可察觉的性能、可伸缩性，网络效率、可靠性也可以提到提升
• 分层风格 Hierarchical Styles   
  • 优点：简单性、可进化性、可伸缩性
• 移动代码风格 Mobile Code Styles   
  • 优点：可移植性、可扩展性、网络效率
• 点对点风格 Peer-to-Peer Styles
  • 优点：可进化性、可重用性、可扩展性、可配置性
  
  
数据流风格 Data-flow Styles
• 管道与过滤器 Pipe And Filter，PF
  • 每个 Filter 都有输入端和输出端，只能从输入端读取数据，处理后再从输出端产生数据
• 统一接口的管道与过滤器 Uniform Pipe And Filter，UPF
  • 在 PF 上增加了统一接口的约束，所有 Filter 过滤器必须具备同样的接口 
  
  数据流风格在不同评估的得分项
  

复制风格 Replication Styles 主要有两种

• 复制仓库 Replicated Repository, RR    可靠性，可伸缩性(随意增减节点)
  • 多个进程提供相同的服务，通过反向代理对外提供集中服务   mysql冷热备份
• 缓存 $                    
  • RR的变体，通过复制请求的结果，为后续请求复用   
  
  
  

分层风格Hierarchical Styles （一）

• 客户端服务器 Client-Server，CS   cs架构  server负责资源的管理  client资源的展示
  • 由 Client 触发请求，Server 监听到请求后产生响应，Client 一直等待收到响应后，会话结束
  • 分离关注点隐藏细节，良好的简单性、可伸缩性、可进化性     server可添加节点，独立进化

• 分层系统 Layered System ，LS
   • 每一层为其之上的层服务，并使用在其之下的层所提供的服务，例如 TCP/IP   osi模型

• 分层客户端服务器 Layered Client-Server ， LCS
  • LS+CS，例如正向代理和反向代理，从空间上分为外部层与内部层  从物理空间分为内外层

• 无状态、客户端服务器 Client-Stateless-Server CSS   无状态的cs
  • 基于 CS，服务器上不允许有session state会话状态   第二个请求不依赖第一个请求的的数据
  • 提升了可见性、可伸缩性、可靠性，但重复数据导致降低网络性能   每个请求都携带数据，性能降低
    http2.0-http1.1的升级  解决重复头部，例如cookie  提升性能，但是做不到无状态，可伸缩，可见性降低
 
• 缓存、无状态、客户端服务器 Client-Cache-Stateless-Server C$SS   $缓存
  • 提升性能    缓存重复数据

• 分层、缓存、无状态、客户端服务器 Layered-Client-Cache-Stateless-Server,LC$SS  总和




分层风格 Hierarchical Styles （二）

• 远程会话 Remote Session, RS  
  • CS 变体，服务器保存 Application state 应用状态  ftp,每次请求保存当前目录
  • 可伸缩性、可见性差

• 远程数据访问 Remote Data Access ， RDA  
  • CS 变体， Application state 应用状态同时分布在客户端与服务器   sql访问，游标查询数据
  • 巨大的数据集有可能通过迭代而减少
  • 简单性、可伸缩性差  传统数据库很难支持分布式
  
  
  
  
  
移动代码风格 Mobile Code Styles  实际运行代码可以在client，server执行
• 虚拟机 Virtual Machine， VM
  • 分离指令与实现

• 远程求值 Remote Evaluation， REV   可能充满恶意代码
  • 基于 CS 的 VM，将代码发送至服务器执行

• 按需代码 Code on Demand， COD    js   浏览器需要执行js时，从服务器拉起代码
  • 服务器在响应中发回处理代码，在客户端执行  
  • 优秀的可扩展性和可配置性，提升用户可察觉性能和网络效率

• 分层、按需代码、缓存、无状态、客户端服务器
  Layered-Code-on-Demand-Client-Cache-Stateless-Server， LCODC$SS
    • LC$SS+COD

• 移动代理 Mobile Agent， MA
  • 相当于 REV+COD 
  
  
  
  
点对点风格 Peer-to-Peer Styles

• Event-based Integration ，EBI：
  • 基于事件集成系统，如由类似 Kafka 这样的消息系统 + 分发订阅来消除耦合
  • 优秀的可重用性、可扩展性、可进化性
  • 缺乏可理解性   收到消息，不知道哪条订阅产生的
  • 由于消息广播等因素造成的消息风暴，可伸缩性差

• Chiron-2,C2 
  参见论文《A Component- and Message-Based Architectural Style for GUI Software》
    • 相当于 EBI+LCS，控制了消息的方向

• Distributed Objects ，DO
  • 组件结对交互

• Brokered Distributed Objects ，BDO
  • 引入名字解析组件来简化 DO，例如 CORBA   corba分布式架构
  
  
风格演化
从原点出发  增加各个功能，进入每个分支   
   如何分层，备份，统一接口  
   最后结果REST架构
  
  

vulture
老师提到http2.0做不到无状态，那http2.0是不是就不能被称作RESTful的协议呢？
作者回复: http2.0协议在同一连接上不同消息间是有状态的，但http2.0把这个状态封装在协议处理模块里了，并没有把状态暴露给应用。
你可以这么理解，应用仍然可以像http/1.1一样去拿URI/Method/Header，虽然发送方可能并没有发送完整的内容（比如57课Header要靠动态表去取之前Stream传输时存下的内容）。所以，http2.0只是协议通讯，它并不影响HTTP RESTful API。  

SpaceX
有点太抽象了，能不能介绍点简单具体的例子
作者回复: 比如mysql这种需要客户端和服务器同时保存应用状态就得被REST抛弃，或者像kafka这样的消息订阅、推送也不行；或者REST选中javascript或者javaapplet都是因为COD架构下，允许提供更好的交互体验及扩展性  
  
  
参考链接
https://users.soe.ucsc.edu/~ejw/papers/c2-icse17.pdf   