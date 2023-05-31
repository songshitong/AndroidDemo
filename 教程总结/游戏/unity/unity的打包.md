
2022-07-18
https://blog.csdn.net/qq_39604302/article/details/89888022
两种打包方式
mono和il2cpp
mono是c#的跨平台框架
Mono是一个由Xamarin公司所主持的自由开放源码项目。
Mono的目标是在尽可能多的平台上使.net标准的东西能正常运行的一套工具
https://github.com/mono/mono
https://www.mono-project.com/

中间语言  
IL的全称是 Intermediate Language，CIL（Common Intermediate Language，特指在.Net平台下的IL标准）
它是一种属于通用语言架构和.NET框架的低阶（lowest-level）的人类可读的编程语言。目标为.NET框架的语言被编译成CIL，然后汇编成字节码。

mono与Xamarin  https://www.zhihu.com/question/268377070
.NET Framework、Mono、.NET Core 都是 .NET 的实现，后两者可跨平台，而 Xamarin 是基于 Mono 的一个 APP 开发框架




2022-07-26
unityLibrary打包AAR
1 修改gradle版本，可能出现不兼容的情况 gradle/wrapper/gradle-wrapper.properties
2 unityLibrary修改manifest.xml，去掉launcher相关，否则会有多个桌面icon
3 ndk版本设置，设置兼容的ndk
4 unityLibrary-release.aar改为unityLibrary.aar
5 aar进入主工程，clean，gradle sync