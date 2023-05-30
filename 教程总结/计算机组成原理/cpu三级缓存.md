
CPU缓存分为L1,L2,L3
L1,L2是每个cpu独有的，L3是多个CPU共有的   //todo 比如超线程的CPU中，L1Cache是独占的，L2是Core共享的?
L1缓存分为L1i,L1d    i指的是instruction指令缓存，d是数据data缓存

CPU缓存的芯片叫做SRAM（Static Random-Access Memory，静态随机存取存储器），断电后数据消失
在 SRAM 里面，一个比特的数据，需要 6～8 个晶体管


内存的芯片叫做DRAM（Dynamic Random Access Memory，动态随机存取存储器）
DRAM 的一个比特，只需要一个晶体管和一个电容就能存储
数据是存储在电容里的，电容会不断漏电，所以需要定时刷新充电，才能保持数据不丢失
内存条的数据是缓慢消失的，不是断电后立即消失
https://juejin.cn/post/7001751364711743519

cpu,cpu缓存，内存，硬盘的关系
大脑相当于cpu
正在处理的在寄存器
短期记忆相当于L1 cache
长期记忆相当于L2/L3 cache
书桌/书房相当于内存   书/资料相当于数据
图书馆相等于SSD/HDD硬盘
其中存储的东西越多，速度越慢，但是价格越低，同时与CPU的距离越远


