可运行在服务器，支持bpf语法


如果你的类 Unix 操作系统默认没有安装 tcpdump，可以采用以下方法安装
centos   yum install tcpdump
ubuntu   apt-get install tcpdump
其他系统使用源代码编译安装 https://www.tcpdump.org/#latest-release


捕获及停止条件   直接运行tcpdump 就可以抓取
• -D 列举所有网卡设备
• -i 选择网卡设备      不指定时，默认选择第一个eth0
• -c 抓取多少条报文
• --time-stamp-precision 指定捕获时的时间精度，默认毫秒 micro，可选纳秒 nano
• -s 指定每条报文的最大字节数，默认 262144 字节


抓取环回地址  tcpdump -i lo0       另一个命令行telnet 127.0.0.1 80
tcpdump -i lo0 -c 2    抓取两条后停止
tcpdump -i lo0 port 80   支持bpf 端口80


文件操作
• -w 输出结果至文件（可被Wireshark读取分析）
• -C 限制输入文件的大小，超出后以后缀加 1 等数字的形式递增。 注意单位是 1,000,000 字节  1M
• -W 指定输出文件的最大数量，到达后会重新覆写第 1 个文件
• -G 指定每隔N秒就重新输出至新文件，注意-w 参数应基于 strftime 参数指定文件名   右侧图片的百分号
• -r 读取一个抓包文件
• -V 将待读取的多个文件名写入一个文件中，通过读取该文件同时读取多个文件

tcpdump -c -w a.pcap  文件后缀可以任意
tcpdump -r a.pcap   读取文件
tcpdump -V d   d包含a,b的文件名，就可以同时读多个文件

tcpdump -C 1 -W 3 -w abc  最大1M，最多3个文件，超过后覆盖第一个文件
  abc0 abc1 abc2
tcpdump -G 3 -w def%M-%S  每隔3秒输出到一个新的文件
 def27-28   def27-31  def27-34


输出时间戳格式
• -t 不显示时间戳
• -tt 自 1970年 1 月 1 日 0 点至今的秒数
• -ttt 显示邻近两行报文间经过的秒数
• -tttt 带日期的完整时间
• -ttttt 自第一个抓取的报文起经历的秒数
tcpdump -r a -t  读文件,不带时间戳
tcpdump -r a -ttt  距上个报文的时间
tcpdump -r a -ttttt  距第一个报文的时间


分析信息详情
• -e 显示数据链路层头部     默认不显示
• -q 不显示传输层信息       默认显示网络层
• -v 显示网络层头部更多的信息，如 TTL、id 等
• -n 显示 IP 地址、数字端口代替 hostname 等
• -S TCP 信息以绝对序列号替代相对序列号
• -A 以 ASCII 方式显示报文内容，适用 HTTP 分析
• -x 以 16 进制方式显示报文内容，不显示数据链路层
• -xx 以 16 进制方式显示报文内容，显示数据链路层
• -X 同时以 16 进制及 ACII 方式显示报文内容，不显示数据链路层
• -XX 同时以 16 进制及 ACII 方式显示报文内容，显示数据链路层

tcpdump -i en0 -n  显示IP，不显示域名
tcpdump -i lo0 -c 1 -A  以ASCII显示报文     另一个调用  curl localhost
tcpdump -i lo0 -c 1 -X  左边16进制，右边ASCII

tcpdump -i lo0 -c 1 -X port 80
tcpdump: verbose output suppressed, use -v or -vv for full protocol decode
listening on lo0, link-type NULL (BSD loopback), capture size 262144 bytes
11:31:37.635416 IP6 localhost.63321 > localhost.http: Flags [S], seq 617113762, win 65535, options [mss 16324,nop,wscale 6,nop,nop,TS val 1087909058 ecr 0,sackOK,eol], length 0
0x0000:  6007 bf0c 002c 0640 0000 0000 0000 0000  `....,.@........
0x0010:  0000 0000 0000 0001 0000 0000 0000 0000  ................
0x0020:  0000 0000 0000 0001 f759 0050 24c8 68a2  .........Y.P$.h.
0x0030:  0000 0000 b002 ffff 0034 0000 0204 3fc4  .........4....?.
0x0040:  0103 0306 0101 080a 40d8 2cc2 0000 0000  ........@.,.....
0x0050:  0402 0000                                ....
1 packet captured
88 packets received by filter
0 packets dropped by kernel





kissingers

老师，tshark ，就是wireshark的命令行形式在您经历中有具体的应用场景吗？
作者回复: 它其实是tcpdump的增强版。
由于我比较习惯用tcpdump，而且复杂问题我都是先把tcpdump抓包文件传到windows系统上，再搭配Wireshark分析，所以tshark的功能对我没啥吸引力，一般在生产开发环境中tcpdump更常见，故我一般不用tshark。



Geek_007

老师你好。-s 指定每条报文的最大字节数，每条报文的最大字节数不应该是一个mss吗？
作者回复: 这个是对tcpdump而言的，复制解析报文是需要缓存的。
另外，tcpdump虽然名为tcp，但它可以抓取任何协议，mss是tcp中的概念，不能搞混

