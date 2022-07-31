
2022-07-18
https://blog.csdn.net/qq_39604302/article/details/89888022
两种打包方式
mono和il2cpp
mono是c#的跨平台框架
中间语言  
IL的全称是 Intermediate Language，CIL（Common Intermediate Language，特指在.Net平台下的IL标准）
它是一种属于通用语言架构和.NET框架的低阶（lowest-level）的人类可读的编程语言。目标为.NET框架的语言被编译成CIL，然后汇编成字节码。


2022-07-26
unityLibrary打包AAR
1.修改gradle版本，可能出现不兼容的情况 gradle/wrapper/gradle-wrapper.properties
2. unityLibrary修改manifest.xml，去掉launcher相关，否则会有多个桌面icon
3. ndk版本设置，设置兼容的ndk
4. unityLibrary-release.aar改为unityLibrary.aar
5. aar进入主工程，clean，gradle sync