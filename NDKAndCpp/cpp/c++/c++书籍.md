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


http://c.biancheng.net/view/2193.html
std库的由来
C++ 是在C语言的基础上开发的，早期的 C++ 还不完善，不支持命名空间，没有自己的编译器，而是将 C++ 代码翻译成C代码，再通过C编译器完成编译。
这个时候的 C++ 仍然在使用C语言的库，stdio.h、stdlib.h、string.h 等头文件依然有效；此外 C++ 也开发了一些新的库，增加了自己的头文件，例如：
iostream.h：用于控制台输入输出头文件。
fstream.h：用于文件操作的头文件。
complex.h：用于复数计算的头文件。

和C语言一样，C++ 头文件仍然以.h为后缀，它们所包含的类、函数、宏等都是全局范围的。

后来 C++ 引入了命名空间的概念，计划重新编写库，将类、函数、宏等都统一纳入一个命名空间，这个命名空间的名字就是std。std 是 standard 的缩写，
意思是“标准命名空间”。

但是这时已经有很多用老式 C++ 开发的程序了，它们的代码中并没有使用命名空间，直接修改原来的库会带来一个很严重的后果：
程序员会因为不愿花费大量时间修改老式代码而极力反抗，拒绝使用新标准的 C++ 代码。

C++ 开发人员想了一个好办法，保留原来的库和头文件，它们在 C++ 中可以继续使用，然后再把原来的库复制一份，在此基础上稍加修改，把类、函数、宏等纳入命名空间
std 下，就成了新版 C++ 标准库。这样共存在了两份功能相似的库，使用了老式 C++ 的程序可以继续使用原来的库，新开发的程序可以使用新版的 C++ 库。

为了避免头文件重名，新版 C++ 库也对头文件的命名做了调整，去掉了后缀.h，所以老式 C++ 的iostream.h变成了iostream，fstream.h变成了fstream。
而对于原来C语言的头文件，也采用同样的方法，但在每个名字前还要添加一个c字母，所以C语言的stdio.h变成了cstdio，stdlib.h变成了cstdlib。

需要注意的是，旧的 C++ 头文件是官方所反对使用的，已明确提出不再支持，但旧的C头文件仍然可以使用，以保持对C的兼容性。实际上，
编译器开发商不会停止对客户现有软件提供支持，可以预计，旧的 C++ 头文件在未来数年内还是会被支持。
