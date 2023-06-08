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


.inc文件 https://www.cnblogs.com/shHome/p/14093846.html
 C/C++的标准惯例是将class、function的声明信息写在.h文件中。.c文件写class实现、function实现、变量定义等等。

然而对于template来说，它既不是class也不是function，而是可以生成一组class或function的东西。编译器（compiler）为了给template生成代码，
他需要看到声明（declaration ）和定义（definition ），因此他们必须不被包含在.h里面。

为了使声明、定义分隔开，定义写在自己文件内部，即.inc文件，然后在.h文件的末尾包含进来。当然除了.inc的形式，还可能有许多其他的写法.inc, .imp, .impl, .tpp, etc.
 在编译器预处理阶段会将 .h，.inc 文件内容合并到 .i 文件中，虽然我们在写代码时将模板代码分开了，但是在编译器预处理阶段又被合并，所以这是合理的.
