https://baike.baidu.com/item/MAC%E5%9C%B0%E5%9D%80/1254181
MAC地址（英语：Media Access Control Address），直译为媒体存取控制位址，也称为局域网地址（LAN Address），MAC位址，
以太网地址（Ethernet Address）或物理地址（Physical Address），它是一个用来确认网络设备位置的位址。在OSI模型中，第三层网络层负责IP地址，
第二层数据链路层则负责MAC位址 [1]  。MAC地址用于在网络中唯一标示一个网卡，一台设备若有一或多个网卡，则每个网卡都需要并会有一个唯一的MAC地址 [2]

MAC地址也叫物理地址、硬件地址，由网络设备制造商生产时烧录在网卡(Network lnterface Card)的EPROM(一种闪存芯片，通常可以通过程序擦写)




网络中每台设备都有一个唯一的网络标识，这个地址叫MAC地址或网卡地址，由网络设备制造商生产时写在硬件内部。MAC地址则是48位的（6个字节），
通常表示为12个16进制数，每2个16进制数之间用冒号隔开，如08：00：20：0A：8C：6D就是一个MAC地址。具体如下图所示，
其前3字节(24位)表示OUI（Organizationally Unique Identifier），是IEEE的注册管理机构给不同厂家分配的代码，区分不同的厂家。
后3字节由厂家自行分配
可查看图片  IEEE802 48 位 MAC 地址映射主机地址（EUI-64）

oui 查询 http://standards-oui.ieee.org/oui/oui.txt


ip与Mac比较
IP 是地址，有定位功能；MAC 是身份证，无定位功能；   定位功能由IP的寻址特性实现

https://mp.weixin.qq.com/s/_sdt1JqIVvO0LD3ckQGp4A
有了IP地址，为什么还要用MAC地址？
简而言之，标识网络中的一台计算机，比较常用的就是IP地址和MAC地址，但计算机的IP地址可由用户自行更改，管理起来就相对困难，而MAC地址不可更改，
   所以一般会把IP地址和MAC地址组合起来使用。
那只使用MAC地址不用IP地址行不行呢？不行的！因为最早就是MAC地址先出现的，并且当时并不用IP地址，只用MAC地址，后来随着网络中的设备越来越多，
   整个路由过程越来越复杂，便出现了子网的概念。对于目的地址在其他子网的数据包，路由只需要将数据包送到那个子网即可。
那为什么要用IP地址呢？是因为IP地址是和地域相关的，对于同一个子网上的设备，IP地址的前缀都是一样的，这样路由器通过IP地址的前缀就知道设备在在哪个子网上了，
    而只用MAC地址的话，路由器则需要记住每个MAC地址在哪个子网，这需要路由器有极大的存储空间，是无法实现的。
IP地址可以比作为地址，MAC地址为收件人，在一次通信过程中，两者是缺一不可的。

个人ipv4地址是不足的，Mac地址+私有IP


为什么Mac地址不需要全球唯一
https://draveness.me/whys-the-design-non-unique-mac-address/
保证子网内可以区分设备即可