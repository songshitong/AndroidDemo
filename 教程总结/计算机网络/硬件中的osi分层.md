https://www.tianmaying.com/tutorial/NetWorkInstrument

物理层
  HUB集线器和中继器

数据链路层
  网桥，交换机

网络层
  路由器


HUB 集线器就是一种共享设备,HUB本身不能识别目的地址,当同一局域网内的A主机给B主机传输数据时,数据包在以HUB为架构的网络上是以广播方式传输的,
由每一台终端通过验证数据包头的地址信息来确定是否接收。也就是说,在这种工作方式下,同一时刻网络上只能传输一组数据帧的通讯,如果发生碰撞还得重试。
这种方式就是共享网络带宽

中继器只是对所接收的信号进行放大，然后直接发送到另一个端口连接的电缆上，主要用于扩展网络的物理连接范围


网桥除了可以扩展网络的物理连接范围外，还可以对MAC 地址进行分区，隔离不同物理网段之间的碰撞（也就是隔离“冲突域”）


交换机具有集线器一样的集中连接功能，同时它又具有网桥的数据交换功能
    1当交换机从某个端口收到一个数据帧后，先读取帧头部的源MAC 地址，并与自己缓存中的映射表（CAM 表）进行比较，如果没有找到，
       则在CAM 表中添加一个该源MAC 地址与发送该帧的源端口映射表项。这就是交换机的MAC 地址自动学习功能。
    2如果在CAM 表项查到了帧中源MAC 地址，则继续查看是否有帧中目的MAC 地址所对应的映射表项。如果有，则直接把该帧转发到目的MAC 地址节点所连接的交换机端口，
      然后由该端口发送到目的主机。
    3如果在交换机CAM 表中没有找到帧中目的MAC 地址所对应的表项，则把该数据帧向除源端口外的其他所有端口上进行泛洪
    4当MAC 地址与帧中目的MAC 地致的主机接收了该数据帧后就会向源主机产生一个应答帧，交换机获取该应答帧后从其中的源MAC 地址中获取了对应的MAC地址和所连接端口的映射关系，
       并添加到CAM 表中。这样下次再有MAC 地址为这个MAC 地址的帧发送时交换机就可以直接从CAM 表中找到对应的转发端口，直接转发，不用再泛洪了

路由器（Router）是用于连接多个逻辑上分开的网络，所谓逻辑网络是代表一个单独的网络或者一个子网。当数据从一个子网传输到另一个子网时，
可通过路由器来完成。因此，路由器具有判断网络地址和选择路径的功能，它能在多网络互联环境中，建立灵活的连接，
可用完全不同的数据分组和介质访问方法连接各种子网，路由器只接受源站或其他路由器的信息，属网络层的一种互联设备。它不关心各子网使用的硬件设备，
但要求运行与网络层协议相一致的软件

路由器的主要工作就是为经过路由器的每个数据帧寻找一条最佳传输路径，并将该数据有效地传送到目的站点。 路由器的基本功能是，把数据（IP 报文）传送到正确的网络，细分则包括：
    IP 数据报的转发，包括数据报的寻径和传送；
    子网隔离，抑制广播风暴；
    维护路由表，并与其它路由器交换路由信息，这是 IP 报文转发的基础；
    IP 数据报的差错处理及简单的拥塞控制；
    实现对 IP 数据报的过滤和记帐。

路由器之间的信息传递
  工作站A需要向工作站B传送信息（并假定工作站B的IP地址为120．0.5），它们之间需要通过多个路由器的接力传递 工作过程如下所示：
  工作站A将工作站B的地址120.0.5连同数据信息以数据帧的形式发送给路由器1。
  路由器1收到工作站A的数据帧后，先从报头中取出地址120.0.5，并根据路径表计算出发往工作站B的最佳路径：R1－R2－R5－B；并将数据帧发往路由器2。
  路由器2重复路由器1的工作，并将数据帧转发给路由器5。
  路由器5同样取出目的地址，发现120.0.5就在该路由器所连接的网段上，于是将该数据帧直接交给工作站B。
  工作站B收到工作站A的数据帧，一次通信过程宣告结束。

路由器包含网络层，数据链路层，物理层  主要工作内容在网络层，同时网络层也依赖下两层的功能。
  路由器为网络层设计的