

https://juejin.cn/post/6863089679969812488
https://www.guardsquare.com/manual/tools/retrace

retrace工具使用
```
retrace是官方提供的一款工具，可自动根据 mapping.txt 和 stacktrace.txt 转换崩溃栈信息。
java -jar retrace.jar[options...] mapping_file[stacktrace_file]
命令格式如上，另外：
-verbose 指定输出更详细的调用栈信息
-regex 指定的日志中特定的崩溃栈格式，默认解析常规的崩溃栈格式
```

R8
Android Gradle Plugin 3.4.0之后，默认使用R8替代原来的ProGuard，R8在编译过程中主要执行：
Code shrinking (or tree-shaking): 检测及删除无用代码
Resource shrinking: 检测及删除无用资源，包括 Code shrinking 删除的代码中引用到的资源
Obfuscation: 混淆即使用简单字符替代原来的类名、方法名和变量名，减少（复用）字符串常量
Optimization: 代码优化，例如方法内联等

同时R8编译之后也会压缩LineNumberTable，导致我们读取崩溃栈信息的时候异常头疼，因为代码行数有可能完全对不上。好在R8在混淆的时候会另外输出
一个mapping.txt文件，记录混淆的信息(包括代码行数变化信息)，因此我们可以通过该文件逆向解析崩溃栈。
```
classline
    fieldline * N
    methodline * M
```
classline 是类信息，紧接着是N个fieldline的变量信息，以及M个methodline的方法信息

classline的格式：由一个 -> 分隔：前半部分是原始类名，后半部分是混淆之后的类名
```
originalfieldtype originalfieldname -> obfuscatedfieldname
```

fieldline的格式：同样由 -> 分隔：前半部分是原始变量类型和变量名，后半部分是混淆之后的变量名，变量类型的混淆信息在其__classline__信息中。
```
    [startline:endline:]originalreturntype [originalclassname.]originalmethodname(originalargumenttype,...)[:originalstartline[:originalendline]] -> obfuscatedmethodname
```


methodline的格式较为复杂，但同样是通过 -> 分隔：
[startline:endline:]  一般表示混淆之后的代码行数(也就是日志中崩溃栈的行数)
originalreturntype  表示原始的方法返回值类型
[originalclassname.]  表示原始的方法定义类；如果是当前类，则可省略
originalmethodname  表示原始的方法名称
(originalargumenttype,...)  表示原始的方法参数列表
[:originalstartline[:originalendline]]  表示原始的方法行数
obfuscatedmethodname  表示混淆之后方法的名称

另外：
如果方法的行数保持不变，则 [:originalstartline[:originalendline]] 信息可省略
如果方法的行数被删除，则省略 [startline:endline:] 信息
如果是inline代码块，则只有 [:originalstartline] 信息，表示被调用的(原始)行数
同一个方法的信息可能划分为多个 methodline 输出（压缩了中间冗余行数信息）
不同的方法行数可以相同，这时候 retrace 可以通过方法名称进行识别区分


区分是否是inline代码块：
如果连续两个或以上methodline 的 [startline:endline:] 信息一样，而且除了第一行 
[:originalstartline[:originalendline]] __有两个值之外，后面的 __[:originalstartline[:originalendline]] __都只有一个值， 
则表示这部分代码是__inline__代码。此时 [startline:endline:] 不再是方法所在行数，而是方法的  原始行数 + 1000 * K
（K可为0，这样的取值是为了区分这些__inline__代码块）。在这种情况下，方法所在的行数以及原始调用行数信息是放在 
[:originalstartline[:originalendline]] 上的

示例：
```
// Main类未混淆
com.example.application.Main -> com.example.application.Main:
    // 变量configuration混淆为a
    com.example.application.Configuration configuration -> a
    // 构造函数未混淆，其行数不变，与原始行数一样是50-60
    50:66:void <init>(com.example.application.Configuration) -> <init>
    // execute()方法混淆为a，代码行数不变，与原始行数一样是74-228
    74:228:void execute() -> a
    // 将GPL类的check()方法39-56行内联至execute()方法的76行
    2039:2056:void com.example.application.GPL.check():39:56 -> a
    2039:2056:void execute():76 -> a
    // 将当前类的printConfiguration()方法236-252行内联至execute()方法的80行
    2236:2252:void printConfiguration():236:252 -> a
    2236:2252:void execute():80 -> a
    // 这是一个嵌套内联
    // 将PrintWriter类的createPrintWriterOut方法内联至printConfiguration方法块的243行，然后该方法块又被内联至execute()方法的80行
    // 80和243指的是原始调用行数，40-42是代码的原始行数范围
    3040:3042:java.io.PrintWriter com.example.application.util.PrintWriterUtil.createPrintWriterOut(java.io.File):40:42 -> a
    3040:3042:void printConfiguration():243 -> a
    3040:3042:void execute():80 -> a
    // 将当前类的readInput()方法260-268行内联至execute()方法的97行
    3260:3268:void readInput():260:268 -> a
    3260:3268:void execute():97 -> a
```
