DHCP(Dynamic Host Configuratin Protocol) 动态主机设置协议
1）、是什么？
网络管理员只需配置一段共享的 IP 地址，每一台新接入的机器都可以通过 DHCP 来这个共享的 IP 地址里面申请 IP 地址，就可以自动配置。
等用完还回去其它机器也能使用。它的特点如下所示：
1、DHCP 是一个局域网协议。
2、DHCP 是应用 UDP 协议的应用层协议


2）、功能
即插即用联网。
在 IP 配置界面选中 自动获得 IP 地址、自动获得 DNS 服务器地址即可启用 DHCP 协议去获取一个临时 IP（通常是一个内网地址）。
有一个租期，在租期过半时可以续租。

3）、DHCP 的过程
    1）、DHCP 服务器监听默认端口：67。
    2）、主机使用 UDP 协议广播 DHCP 发现报文。
    3）、DHCP 服务器发出 DHCP 提供报文。
    4）、主机向 DHCP 服务器发出 DHCP 请求报文。
    5）、DHCP 服务器回应并提供 IP 地址。

4）、向 DHCP 租用的 IP 地址是有租期的，IP 地址如何实现续租呢？
客户端会在租期过去50%的时候，直接向为其提供 IP 地址的 DHCP 服务器发送 DHCP request 消息报。客户端接收到服务器回应的 DHCP ACK 消息包后，
会根据消息报中提供的新的租期以及其他已经更新的 TCP/IP 参数更新自己的配置。


https://baike.baidu.com/item/DHCP/218195?fr=aladdin
DHCP有三种机制分配IP地址：
1) 自动分配方式（Automatic Allocation），DHCP服务器为主机指定一个永久性的IP地址，一旦DHCP客户端第一次成功从DHCP服务器端租用到IP地址后，就可以永久性的使用该地址。 
2) 动态分配方式（Dynamic Allocation），DHCP服务器给主机指定一个具有时间限制的IP地址，时间到期或主机明确表示放弃该地址时，该地址可以被其他主机使用。 
3) 手工分配方式（Manual Allocation），客户端的IP地址是由网络管理员指定的，DHCP服务器只是将指定的IP地址告诉客户端主机。 
   三种地址分配方式中，只有动态分配可以重复使用客户端不再需要的地址
   
图片DHCP报文格式
OP:
若是 client 送给 server 的封包，设为 1 ，反向则为 2。
HTYPE:
硬件类别，Ethernet 为 1。 
HLEN:
硬件地址长度， Ethernet 为 6。 
HOPS:
若封包需经过 router 传送，每站加 1 ，若在同一网内，为 0。 
TRANSACTION ID:
DHCP REQUEST 时产生的数值，以作 DHCPREPLY 时的依据。
SECONDS:
Client 端启动时间（秒）。 
FLAGS:
从 0 到 15 共 16 bits ，当 bit 为 1 时表示 server 将以广播方式传送封包给 client ，其余尚未使用。
ciaddr:
要是 client 端想继续使用之前取得的IP 地址，则列于这里。
yiaddr:
从 server 送回 client 的 DHCP OFFER 与 DHCPACK封包中，此栏填写分配给 client 的 IP 地址。
siaddr:
若 client 需要透过网络开机，从 server 送出的 DHCP OFFER、DHCPACK、DHCPNACK封包中，此栏填写开机程序代码所在 server 的地址。
giaddr:
若需跨网域进行 DHCP 发放，此栏为 relay agent 的地址，否则为 0。
chaddr:
Client 的硬件地址。 
sname:
Server 的名称字符串，以 0x00 结尾。 
file:
若 client 需要透过网络开机，此栏将指出开机程序名称，稍后以 TFTP 传送。
DHCP Option 常见取值及含义
Options号 Options作用
1     设置子网掩码选项。
3     设置网关地址选项。
6     设置DNS服务器地址选项。
12    设置域名选项。
15    设置域名后缀选项。
33    设置静态路由选项。该选项中包含一组有分类静态路由（即目的地址的掩码固定为自然掩码，不能划分子网），客户端收到该选项后，
          将在路由表中添加这些静态路由。如果存在Option121，则忽略该选项。
44    设置NetBios服务器选项。
46    设置NetBios节点类型选项。
50    设置请求IP选项。
51    设置IP地址租约时间选项。
52    设置Option附加选项。
53    设置DHCP消息类型。
54    设置服务器标识。
55    设置请求参数列表选项。客户端利用该选项指明需要从服务器获取哪些网络配置参数。该选项内容为客户端请求的参数对应的选项值。
58    设置续约T1时间，一般是租期时间的50%。
59    设置续约T2时间。一般是租期时间的87.5%。
60    设置厂商分类信息选项，用于标识DHCP客户端的类型和配置。
61    设置客户端标识选项。
66    设置TFTP服务器名选项，用来指定为客户端分配的TFTP服务器的域名。
67    设置启动文件名选项，用来指定为客户端分配的启动文件名。
77    设置用户类型标识。
121   设置无分类路由选项。该选项中包含一组无分类静态路由（即目的地址的掩码为任意值，可以通过掩码来划分子网），客户端收到该选项后，将在路由表中添加这些静态路由。
148   EasyDeploy中Commander的IP地址。
149   SFTP和FTPS服务器的IP地址。
150   设置TFTP服务器地址选项，指定为客户端分配的TFTP服务器的地址。



抓包dhcp
wireshark配置  udp and port 67
断开网线，重连
//client发送descover的报文
4	8.990965	0.0.0.0	255.255.255.255	DHCP	342		DHCP Discover - Transaction ID 0x31d4e96c
    Dynamic Host Configuration Protocol (Discover)
    Message type: Boot Request (1)
    Hardware type: Ethernet (0x01)
    Hardware address length: 6
    Hops: 0
    Transaction ID: 0x31d4e96c
    Seconds elapsed: 9
    Bootp flags: 0x0000 (Unicast)
    Client IP address: 0.0.0.0
    Your (client) IP address: 0.0.0.0
    Next server IP address: 0.0.0.0
    Relay agent IP address: 0.0.0.0
    Client MAC address: RealtekS_68:03:c1 (00:e0:4c:68:03:c1)
    Client hardware address padding: 00000000000000000000
    Server host name not given
    Boot file name not given
    Magic cookie: DHCP
    Option: (53) DHCP Message Type (Discover)
    Option: (55) Parameter Request List
    Option: (57) Maximum DHCP Message Size
    Option: (61) Client identifier
    Option: (51) IP Address Lease Time
    Option: (12) Host Name
    Option: (255) End
    Padding: 000000000000000000000000


//server响应offer报文，可以提供的IP
5	9.269162	10.24.11.9	10.24.61.65	DHCP	342		DHCP Offer    - Transaction ID 0x31d4e96c
    Dynamic Host Configuration Protocol (Offer)
    Message type: Boot Reply (2)
    Hardware type: Ethernet (0x01)
    Hardware address length: 6
    Hops: 1
    Transaction ID: 0x31d4e96c
    Seconds elapsed: 9
    Bootp flags: 0x0000 (Unicast)
    Client IP address: 0.0.0.0
    Your (client) IP address: 10.24.61.65
    Next server IP address: 0.0.0.0
    Relay agent IP address: 10.24.61.254
    Client MAC address: RealtekS_68:03:c1 (00:e0:4c:68:03:c1)
    Client hardware address padding: 00000000000000000000
    Server host name not given
    Boot file name not given
    Magic cookie: DHCP
    Option: (53) DHCP Message Type (Offer)
    Option: (54) DHCP Server Identifier (10.24.11.9)
    Option: (51) IP Address Lease Time
    Option: (1) Subnet Mask (255.255.255.0)
    Option: (3) Router
    Option: (6) Domain Name Server
    Option: (15) Domain Name
    Option: (255) End
    Padding: 000000000000

//client请求ip地址
6	10.272254	0.0.0.0	255.255.255.255	DHCP	342		DHCP Request  - Transaction ID 0x31d4e96c
    Dynamic Host Configuration Protocol (Request)
    Message type: Boot Request (1)
    Hardware type: Ethernet (0x01)
    Hardware address length: 6
    Hops: 0
    Transaction ID: 0x31d4e96c
    Seconds elapsed: 11
    Bootp flags: 0x0000 (Unicast)
    Client IP address: 0.0.0.0
    Your (client) IP address: 0.0.0.0
    Next server IP address: 0.0.0.0
    Relay agent IP address: 0.0.0.0
    Client MAC address: RealtekS_68:03:c1 (00:e0:4c:68:03:c1)
    Client hardware address padding: 00000000000000000000
    Server host name not given
    Boot file name not given
    Magic cookie: DHCP
    Option: (53) DHCP Message Type (Request)
    Option: (55) Parameter Request List
    Option: (57) Maximum DHCP Message Size
    Option: (61) Client identifier
    Option: (50) Requested IP Address (10.24.61.65)
    Option: (54) DHCP Server Identifier (10.24.11.9)
    Option: (12) Host Name
    Option: (255) End
    Padding: 000000000000

//server响应ack
7	10.280081	10.24.11.9	10.24.61.65	DHCP	342		DHCP ACK      - Transaction ID 0x31d4e96c
    Dynamic Host Configuration Protocol (ACK)
    Message type: Boot Reply (2)
    Hardware type: Ethernet (0x01)
    Hardware address length: 6
    Hops: 1
    Transaction ID: 0x31d4e96c
    Seconds elapsed: 11
    Bootp flags: 0x0000 (Unicast)
    Client IP address: 0.0.0.0
    Your (client) IP address: 10.24.61.65
    Next server IP address: 0.0.0.0
    Relay agent IP address: 10.24.61.254
    Client MAC address: RealtekS_68:03:c1 (00:e0:4c:68:03:c1)
    Client hardware address padding: 00000000000000000000
    Server host name not given
    Boot file name not given
    Magic cookie: DHCP
    Option: (53) DHCP Message Type (ACK)
    Option: (54) DHCP Server Identifier (10.24.11.9)
    Option: (51) IP Address Lease Time
    Option: (1) Subnet Mask (255.255.255.0)
    Option: (3) Router
    Option: (6) Domain Name Server
    Option: (15) Domain Name
    Option: (255) End
    Padding: 000000000000



https://blog.csdn.net/qq_43153465/article/details/94596584
其他类型的报文    放在DHCP的option中 Option: (53) DHCP Message Type
DHCP NAK：
服务器对客户端的 DHCP REQUEST 报文的拒绝响应报文，比如服务器对客户端分配的 IP 地址已超过使用租借期限（服务器没有找到相应的租约记录）或者由于某些原
因无法正常分配 IP 地址，则发送 DHCP NAK 报文作为应答（客户端移到了另一个新的网络）。通知 DHCP 客户端无法分配合适 IP 地址。DHCP 客户端需要重新发送
DHCP DISCOVERY 报文来申请新的 IP 地址。

DHCP DECLINE：
当客户端发现服务器分配给它的 IP 地址发生冲突时会通过发送此报文来通知服务器，并且会重新向服务器申请地址。

DHCP RELEASE：
客户端可通过发送此报文主动释放服务器分配给它的 IP 地址，当服务 器收到此报文后，可将这个 IP 地址分配给其它的客户端。

DHCP INFORM：
客户端已经获得了 IP 地址，发送此报文的目的是为了从服务器获得其他的一些网络配置信息，比如网关地址、DNS 服务器地址等。
以上 8 种类型报文的格式相同，只是某些字段的取值不同。DHCP 报文格式基于 BOOTP
的报文格式。