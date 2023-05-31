《C++Concurrency in Action》 c++11
《C++标准库第二版》
《深入理解C++11 C++11新特性解析与应用》

native调试
1 Edit Configurations→Debugger,修改Debug type,并配置C++ service so的路径
如果debug不成功，清空缓存，重启，refresh linked c++ projects

2 LLDB Startup Commands修改
如果service so是本地编译的,就不需要修改"LLDB Startup Commands"
如果是云编译的C++ service so,则需要修改"LLDB Startup Commands",如下:
settings set target.source-map  云代码路径 本地代码路径
