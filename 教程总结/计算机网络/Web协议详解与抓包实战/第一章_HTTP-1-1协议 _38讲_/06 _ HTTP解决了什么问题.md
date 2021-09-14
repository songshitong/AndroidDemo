Roy Thomas Fielding 与 HTTP/1.1
2000年 REST架构

Form Follows Function：HTTP 协议为什么是现在这个样子？  形式追随功能
 method path version
 GET  / Http/1.1   http的形式为什么是这样的，实现了什么功能
 
HTTP 解决了什么问题？
  万维网创始人Tim Berners Lee
  web的主要目的是使人和机器共享信息
  
  人以可视化的形式解读http的数据内容
  
  html为什么是结构化 
  js为什么是本地执行  便于人阅读，体验更好
  css的格式化       使文字有色彩  人的阅读体验
  
  
  解决 WWW 信息交互必须面对的需求： 非功能需求
  • 低门槛    Java applet 使用门槛高  js流行
  • 可扩展性：巨大的用户群体，超长的寿命   
  • 分布式系统下的 Hypermedia：大粒度数据的网络传输  超媒体，视频，图片
  • Internet 规模  跨越国家，组织，公司，种族
     • 无法控制的 scalability  
        • 不可预测的负载、非法格式的数据、恶意消息   明星事件，网络攻击
        • 客户端不能保持所有服务器信息，服务器不能保持多个请求间的状态信息
     • 独立的组件部署：新老组件并存 服务器ngix,tomcat,jetty
  • 向前兼容：自 1993 年起 HTTP0.9\1.0（1996）已经被广泛使用  ie6
   



巨大的网络：2005 年初的网络拓扑图
   线代表  两个IP地址的时间
   颜色  不同域名







请问 你的网络拓扑图 是从哪里来的？
作者回复: wiki，https://en.wikipedia.org/wiki/Network_topology#/media/File:Internet_map_1024.jpg

fmouse
太棒了，讲解HTTP前先了解它产出的背景和目的，这样后面遇到疑惑和不解为什么这样做、这样设计时再回头看其背景和目的就很容易理解了。
作者回复: 从历史逻辑中可以推断未来^_^

论文链接
https://www.ics.uci.edu/~fielding/pubs/dissertation/fielding_dissertation.pdf