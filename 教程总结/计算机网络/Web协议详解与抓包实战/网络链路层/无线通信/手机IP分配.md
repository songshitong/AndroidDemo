https://zhuanlan.zhihu.com/p/87253246

手机IP分配
GGSN/PGW（Gateway GPRS Support Node/PDN GateWay，网关GPRS支持节点/PDN网关）网元动态分配
这种方式下，就是GGSN/PGW网元在用户上网请求流程中，从一个事先定义好的“地址池”当中随机选择一个IP来供用户在“本次”上网过程中使用，用户“下线”或关机后，
该IP会被“回收”到地址池当中，再被别的用户重复使用。由于在这种方式下，用户每次上网获取的私网IP不尽相同，因此称为动态分配。目前，
电信运营商大都采用这种手机IP地址分配方式。