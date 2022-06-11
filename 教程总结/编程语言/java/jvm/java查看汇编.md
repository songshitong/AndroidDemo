
https://www.jianshu.com/p/972b0d04cc64
1 JITWatch使用
1.1根据文档下载源码到本地，通过三种方式运行：需要添加对应的环境变量
https://github.com/AdoptOpenJDK/jitwatch
```
//ant编译
ant clean compile test run
//maven编译
mvn clean compile test exec:java
//gradle编译
./gradlew clean build run
```
1.2使用任一方式运行即可打开JITWatch
 1 配置config,Source locations选项Add Folder工程的java目录
 2 Class locations为功能的classes目录
 3 配置vm调试
```
 -XX:+LogCompilation JIT的编译日志输出
 -XX:LogFile=jit.log日志重定向到日志文件
 //前提设置
 -XX:+UnlockDiagnosticVMOptions 解锁诊断功能
```
1.3按照以上配置完毕运行程序获取log日志后添加到JITWatch的openLog中
1.4点击start运行即可获取源码，字节码，汇编代码对比图



2.HSDIS(Hotspot Disassembler)
HSDIS使用
1 该插件是由Sun官方推荐的HotSpot虚拟机JIT编译代码的反汇编插件，实际上就是一个动态库；
2 下载hsdis地址：hsdis-amd64.dylib，downLoad下载完毕放到JDK安装路径bin/文件下即可
https://github.com/evolvedmicrobe/benchmarks/blob/master/hsdis-amd64.dylib
3 使用IDE运行程序时配置VM参数如下：  vm options
```
-Xcomp : 让JVM以编译模式执行代码，即JVM会在第一次运行时即将所有字节码编译为本地代码
-XX:+UnlockDiagnosticVMOptions : 解锁诊断功能
-XX:+PrintAssembly : 输出反汇编后的汇编指令
```
//也可以使用Java命令  java -XX:+UnlockDiagnosticVMOptions -XX:+PrintAssembly Main (Main是class文件)
直接使用这几个命令会报错
```
Java HotSpot(TM) 64-Bit Server VM warning: PrintAssembly is enabled; turning on DebugNonSafepoints to gain additional output 
Could not load hsdis-amd64.dylib; library  not loadable; PrintAssembly is disabled
```
4 配置完成运行程序即可
```
public class VolatileDemo {
    private static volatile int i = 0;
    public static void n(){
        i++;
    }
    public static synchronized void m(){}
    public static void main(String[] args) {
        //热点代码，编译成本地代码
        for (int j = 0; j < 1_000_000; j++) {
            n();
            m();
        }
    }
}

//如上代码运行即可在run中输出汇编码，搜索‘n'即可查看到该条汇编语句，这就是valotile关键字的汇编码实现方式 //n是函数名
lock addl $0x0,(%rsp)     ;*putstatic i
```