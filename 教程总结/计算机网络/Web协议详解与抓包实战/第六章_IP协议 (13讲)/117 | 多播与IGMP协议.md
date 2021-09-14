当需要把一个报文发送给多个主机时，广播和组播就有了用武之地
 arp协议，数据链路层是支持广播的，事实上也支持组播，IP协议与udp也是支持广播与组播的，所以我们可以借助大部分的编程语言实现多播功能

广播与组播
• 单播
• 广播  发送给所有主机
• 组播  将多个主机组成一组，并且有一个组播IP地址

广播地址
• 以太网地址：ff:ff:ff:ff:ff:ff
• IP 地址
 IP地址全1，以太网地址全1 意味着广播

抓包广播报文  捕获过滤器 broadcast
9	56.836351	10.24.61.65	10.24.61.255	NBNS	92		Name query NB WORKGROUP<1d>
Ethernet II, Src: RealtekS_68:03:c1 (00:e0:4c:68:03:c1), Dst: Broadcast (ff:ff:ff:ff:ff:ff)
    Destination: Broadcast (ff:ff:ff:ff:ff:ff)    以太网地址都是1
    Source: RealtekS_68:03:c1 (00:e0:4c:68:03:c1)
    Type: IPv4 (0x0800)


组播IP地址   d类地址    有了cidr后，abc类不怎么使用了，但是D类地址仍在使用
• 预留组播地址
    • 224.0.0.1：子网内的所有系统组
    • 224.0.0.2：子网内的所有路由器组
    • 224.0.1.1：用于 NTP 同步系统时钟
    • 224.0.0.9：用于 RIP-2 协议

 以太网地址也叫Mac地址
组播以太网地址  8*6=48位  
• 以太网地址：01:00:5e:00:00:00 到 01:00:5e:7f:ff:ff  前25位固定下来了01005e
• 低 23 位：映射 IP 组播地址至以太网地址
以太网中传输组播地址时，组播地址必须是以太网地址  需要将IP地址映射为以太网地址
  映射方式 前25位固定  后23位复制IP地址的低位 ip的高5位未使用

wireshark  抓包  multicast
3	2.180524	10.24.61.45	239.255.255.250	SSDP	170		M-SEARCH * HTTP/1.1
Ethernet II, Src: AsixElec_ad:ae:b2 (00:0e:c6:ad:ae:b2), Dst: IPv4mcast_7f:ff:fa (01:00:5e:7f:ff:fa)
    Destination: IPv4mcast_7f:ff:fa (01:00:5e:7f:ff:fa)   开头是01005e,这是一个组播报文
    Source: AsixElec_ad:ae:b2 (00:0e:c6:ad:ae:b2)
    Type: IPv4 (0x0800)
Internet Protocol Version 4, Src: 10.24.61.45, Dst: 239.255.255.250
    0100 .... = Version: 4
    .... 0101 = Header Length: 20 bytes (5)
    Differentiated Services Field: 0x00 (DSCP: CS0, ECN: Not-ECT)
    Total Length: 156
    Identification: 0x8fb9 (36793)
    Flags: 0x00
    Fragment Offset: 0
    Time to Live: 255
    Protocol: UDP (17)
    Header Checksum: 0xf457 [validation disabled]
    [Header checksum status: Unverified]
    Source Address: 10.24.61.45
    Destination Address: 239.255.255.250   组播发送的IP
01:00:5e:7f:ff:fa 的二进制位数 后23位 和239.255.255.250 的23位是相同的

https://baike.baidu.com/item/简单服务发现协议/23217411?fromtitle=SSDP&fromid=5022449&fr=aladdin
简单服务发现协议（SSDP，Simple Service Discovery Protocol）是一种应用层协议，是构成通用即插即用(UPnP)技术的核心协议之一。
简单服务发现协议提供了在局部网络里面发现设备的机制。控制点（也就是接受服务的客户端）可以通过使用简单服务发现协议，根据自己的需要查询
 在自己所在的局部网络里面提供特定服务的设备。设备（也就是提供服务的服务器端）也可以通过使用简单服务发现协议，向自己所在的局部网络里面的控制点宣告它的存在



IGMP（Internet Group Management Protocol）协议  让路由器知道组播IP的范围
• Type 类型  两个主要字段type代表类型  Group Address IP范围
    • 0x11 Membership Query [RFC3376]
    • 0x22 Version 3 Membership Report [RFC3376]   当前更多使用v3，在IP协议之上，
    • 0x12 Version 1 Membership Report [RFC-1112]
    • 0x16 Version 2 Membership Report [RFC-2236]
    • 0x17 Version 2 Leave Group [RFC-2236]


0x22 Membership Report：状态变更通知
 Group Address 也叫做 Group Record



Group Record 格式
• Record Type 类型
  • 当前状态
    • 1: MODE_IS_INCLUDE    加入一些IP
    • 2: MODE_IS_EXCLUDE    排除一些IP
  • 过滤模式变更（如从 INCLUDE 奕为 EXCLUDE）
    • 3: CHANGE_TO_INCLUDE
    • 4: CHANGE_TO_EXCLUDE
  • 源地址列表变更（过滤模式同时决定状态）
    • 5: ALLOW_NEW_SOURCES   添加新的源
    • 6: BLOCK_OLD_SOURCES

抓包  捕获过滤器 igmp
1	0.000000	10.24.61.105	224.0.0.22	IGMPv3	60		Membership Report / Leave group 224.0.0.252
Internet Group Management Protocol
    [IGMP Version: 3]
    Type: Membership Report (0x22)
    Reserved: 00
    Checksum: 0xfa01 [correct]
    [Checksum Status: Good]
    Reserved: 0000
    Num Group Records: 1
    Group Record : 224.0.0.252  Change To Include Mode
        Record Type: Change To Include Mode (3)
        Aux Data Len: 0
        Num Src: 0
        Multicast Address: 224.0.0.252

2	0.014915	10.24.61.105	224.0.0.22	IGMPv3	60		Membership Report / Join group 224.0.0.252 for any sources
Internet Group Management Protocol
    [IGMP Version: 3]            v3版本
    Type: Membership Report (0x22)
    Reserved: 00
    Checksum: 0xf901 [correct]
    [Checksum Status: Good]
    Reserved: 0000
    Num Group Records: 1
    Group Record : 224.0.0.252  Change To Exclude Mode
        Record Type: Change To Exclude Mode (4)
        Aux Data Len: 0
        Num Src: 0
        Multicast Address: 224.0.0.252

事实上通过编程改变igmp的组播地址是非常简单的
multicast_send.py
```
import socket
import argparse

#python multicast_send.py --mcast-group '224.1.1.2'

def run(group, port):
MULTICAST_TTL = 20
sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP)
sock.setsockopt(socket.IPPROTO_IP, socket.IP_MULTICAST_TTL, MULTICAST_TTL)
sock.sendto(b'from multicast_send.py: ' +
f'group: {group}, port: {port}'.encode(), (group, port))


if __name__ == '__main__':
parser = argparse.ArgumentParser()
parser.add_argument('--mcast-group', default='224.1.1.1')
parser.add_argument('--port', default=19900)
args = parser.parse_args()
run(args.mcast_group, args.port)
```

multicast_recv.py
```
import socket
import struct
import argparse


def run(groups, port, iface=None, bind_group=None):
    # generally speaking you want to bind to one of the groups you joined in
    # this script,
    # but it is also possible to bind to group which is added by some other
    # programs (like another python program instance of this)

    # assert bind_group in groups + [None], \
    #     'bind group not in groups to join'
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP)

    # allow reuse of socket (to allow another instance of python running this
    # script binding to the same ip/port)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

    sock.bind(('' if bind_group is None else bind_group, port))
    for group in groups:
        mreq = struct.pack(
            '4sl' if iface is None else '4s4s',
            socket.inet_aton(group),
            socket.INADDR_ANY if iface is None else socket.inet_aton(iface))

        sock.setsockopt(socket.IPPROTO_IP, socket.IP_ADD_MEMBERSHIP, mreq)

    while True:
        print(sock.recv(10240))


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--port', type=int, default=19900)
    parser.add_argument('--join-mcast-groups', default=[], nargs='*',
                        help='multicast groups (ip addrs) to listen to join')
    parser.add_argument(
        '--iface', default=None,
        help='local interface to use for listening to multicast data; '
        'if unspecified, any interface would be chosen')
    parser.add_argument(
        '--bind-group', default=None,
        help='multicast groups (ip addrs) to bind to for the udp socket; '
        'should be one of the multicast groups joined globally '
        '(not necessaril y joined in this python program) '
        'in the interface specified by --iface. '
        'If unspecified, bind to 0.0.0.0 '
        '(all addresses (all multicast addresses) of that interface)')
    args = parser.parse_args()
    run(args.join_mcast_groups, args.port, args.iface, args.bind_group)
```


广播风暴  https://www.jianshu.com/p/654e7ba3dc73
同单播和多播相比，广播几乎占用了子网内网络的所有带宽。拿开会打一个比方吧，在会场上只能有一个人发言，想象一下如果所有的人同时都用麦克风发言，
那会场上就会乱成一锅粥。集线器由于其工作原理决定了不可能过滤广播风暴，一般的交换机也没有这一功能，不过现在有的网络交换机（如全向的QS系列交换机）
也有过滤广播风暴功能了，路由器本身就有隔离广播风暴的作用。
广播风暴不能完全杜绝，但是只能在同一子网内传播，就好像喇叭的声音只能在同一会场内传播一样，因此在由几百台甚至上千台电脑构成的大中型局域网中，
一般进行子网划分，就像将一个大厅用墙壁隔离成许多小厅一样，以达到隔离广播风暴的目的。
