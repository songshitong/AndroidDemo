http://liuwangshu.cn/framework/system-architecture.html

1.Android系统架构
Android系统架构分为五层，从上到下依次是应用层、应用框架层、系统运行库层、硬件抽象层和Linux内核层

应用层
系统内置的应用程序以及非系统级的应用程序都是属于应用层。负责与用户进行直接交互，通常都是用Java进行开发的。

应用框架层（Java Framework)
应用框架层为开发人员提供了可以开发应用程序所需要的API，我们平常开发应用程序都是调用的这一层所提供的API，当然也包括系统的应用。
这一层的是由Java代码编写的，可以称为Java Framework。下面来看这一层所提供的主要的组件
名称	                         功能描述
Activity Manager(活动管理器)	管理各个应用程序生命周期以及通常的导航回退功能
Location Manager(位置管理器)	提供地理位置以及定位功能服务
Package Manager(包管理器)	    管理所有安装在Android系统中的应用程序
Notification Manager(通知管理器)	使得应用程序可以在状态栏中显示自定义的提示信息
Resource Manager（资源管理器）	    提供应用程序使用的各种非代码资源，如本地化字符串、图片、布局文件、颜色文件等
Telephony Manager(电话管理器)  	管理所有的移动设备功能
Window Manager（窗口管理器）	    管理所有开启的窗口程序
Content Providers（内容提供器）	使得不同应用程序之间可以共享数据
View System（视图系统）	         构建应用程序的基本组件


系统运行库层（Native)
系统运行库层分为两部分，分别是C/C++程序库和Android运行时库

1.C/C++程序库
C/C++程序库能被Android系统中的不同组件所使用，并通过应用程序框架为开发者提供服务，主要的C/C++程序库如下表2所示。
名称	功能描述
OpenGL ES	 3D绘图函数库
Libc	     从BSD继承来的标准C系统函数库，专门为基于嵌入式Linux的设备定制
Media Framework	多媒体库，支持多种常用的音频、视频格式录制和回放。
SQLite	     轻型的关系型数据库引擎
SGL	         底层的2D图形渲染引擎
SSL	         安全套接层，是为网络通信提供安全及数据完整性的一种安全协议
FreeType	 可移植的字体引擎，它提供统一的接口来访问多种字体格式文件

2.Android运行时库
运行时库又分为核心库和ART(5.0系统之后，Dalvik虚拟机被ART取代)。核心库提供了Java语言核心库的大多数功能，这样开发者可以使用
Java语言来编写Android应用。相较于JVM，Dalvik虚拟机是专门为移动设备定制的，允许在有限的内存中同时运行多个虚拟机的实例，
并且每一个Dalvik 应用作为一个独立的Linux 进程执行。独立的进程可以防止在虚拟机崩溃的时候所有程序都被关闭。而替代Dalvik虚拟机的ART 的机制与Dalvik 不同。
在Dalvik下，应用每次运行的时候，字节码都需要通过即时编译器转换为机器码，这会拖慢应用的运行效率，而在ART 环境中，应用在第一次安装的时候，
字节码就会预先编译成机器码，使其成为真正的本地应用。


硬件抽象层（HAL)
硬件抽象层是位于操作系统内核与硬件电路之间的接口层，其目的在于将硬件抽象化，为了保护硬件厂商的知识产权，它隐藏了特定平台的硬件接口细节，
为操作系统提供虚拟硬件平台，使其具有硬件无关性，可在多种平台上进行移植。 从软硬件测试的角度来看，软硬件的测试工作都可分别基于硬件抽象层来完成，
使得软硬件测试工作的并行进行成为可能。通俗来讲，就是将控制硬件的动作放在硬件抽象层中

Linux内核层
Android 的核心系统服务基于Linux 内核，在此基础上添加了部分Android专用的驱动。系统的安全性、内存管理、进程管理、网络协议栈和驱动模型等都依赖于该内核。
Android系统的五层架构就讲到这，了解以上的知识对以后分析系统源码有很大的帮助


2.Android系统源码目录
整体结构
各个版本的源码目录基本是类似，如果是编译后的源码目录会多增加一个out文件夹，用来存储编译产生的文件。Android7.0的根目录结构说明如下表所示
Android源码根目录	描述
abi	应用程序二进制接口
art	全新的ART运行环境
bionic	系统C库
bootable	启动引导相关代码
build	存放系统编译规则及generic等基础开发包配置
cts	Android兼容性测试套件标准
dalvik	dalvik虚拟机
developers	开发者目录
development	应用程序开发相关
device	设备相关配置
docs	参考文档目录
external	开源模组相关文件
frameworks	应用程序框架，Android系统核心部分，由Java和C++编写
hardware	主要是硬件抽象层的代码
libcore	核心库相关文件
libnativehelper	动态库，实现JNI库的基础
ndk	NDK相关代码，帮助开发人员在应用程序中嵌入C/C++代码
out	编译完成后代码输出在此目录
packages	应用程序包
pdk	Plug Development Kit 的缩写，本地开发套件
platform_testing	平台测试
prebuilts	x86和arm架构下预编译的一些资源
sdk	sdk和模拟器
system	底层文件系统库、应用和组件
toolchain	工具链文件
tools	工具文件
Makefile	全局Makefile文件，用来定义编译规则

系统源码分类清晰，并且内容庞大且复杂。接下来分析packages中的内容，也就是应用层部分。

应用层部分
应用层位于整个Android系统的最上层，开发者开发的应用程序以及系统内置的应用程序都是在应用层。源码根目录中的packages目录对应着系统应用层。
它的目录结构如表4所示
packages目录	描述
apps	核心应用程序
experimental	第三方应用程序
inputmethods	输入法目录
providers	内容提供者目录
screensavers	屏幕保护
services	通信服务
wallpapers	墙纸
packages目录存放着系统核心应用程序、第三方的应用程序和输入法等等，这些应用都是运行在系统应用层的，因此packages目录对应着系统的应用层


应用框架层部分
应用框架层是系统的核心部分，一方面向上提供接口给应用层调用，另一方面向下与C/C++程序库以及硬件抽象层等进行衔接。 
应用框架层的主要实现代码在/frameworks/base和/frameworks/av目录下，其中/frameworks/base目录结构如表5所示
/frameworks/base目录	描述	           /frameworks/base目录	描述
api	定义API	                        cmds	重要命令：am、app_proce等
core	核心库	                    data	字体和声音等数据文件
docs	文档                     	graphics	图形图像相关
include	头文件	                    keystore	和数据签名证书相关
libs	库	                        location	地理位置相关库
media	多媒体相关库	                native	本地库
nfc-extras	NFC相关	                obex	蓝牙传输
opengl	2D/3D 图形API	            packages	设置、TTS、VPN程序
sax	XML解析器	                    services	系统服务
telephony	电话通讯管理	            test-runner	测试工具相关
tests	测试相关                   	tools	工具
wifi	wifi无线网络

/frameworks/base/libs目录
hwui  硬件加速的UI   https://blog.csdn.net/wind_hzx/article/details/20307093
hwui主要是android用于2d硬件绘图而加入的一个模块，在hwui之前，android主要是用skia来进行软件绘制，后由于绘制性能等问题，
现在android的绘图几乎都是使用了hwui硬件加速绘图。hwui主要则是使用opengles来进行gpu硬件绘图，提升整个系统的绘制性能，
主要有以下方式：直接渲染，显示列表渲染，延时渲染列表，分别代表的类为：OpenGLRenderer，DisplayListRenderer，DeferredDisplayList。



C/C++程序库部分
系统运行库层（Native)中的 C/C++程序库的类型繁多，功能强大，C/C++程序库并不完全在一个目录中，这里给出几个常用且比较重要的C/C++程序库所在的目录位置


目录位置	描述
bionic/	Google开发的系统C库，以BSD许可形式开源。
/frameworks/av/media	系统媒体库
/frameworks/native/opengl	第三方图形渲染库
/frameworks/native/services/surfaceflinger	图形显示库，主要负责图形的渲染、叠加和绘制等功能
external/sqlite	轻量型关系数据库SQLite的C++实现

讲完 C/C++程序库部分，剩下的部分我们在表3已经给出：Android运行时库的代码放在art/目录中。硬件抽象层的代码在hardware/目录中，
这一部分是手机厂商改动最大的一部分，根据手机终端所采用的硬件平台会有不同的实现


jdk在AOSP中的定义
android6.0路径libcore/libart/src/main/java/java/lang/Thread.java
Google后来迁移到了OpenJDK   ojluni是OpenJDK,java.lang/java.util/java.net/java.io OpenJDK核心库的意思
/libcore/ojluni/src/main/java/java/lang/Thread.java
https://www.zhihu.com/question/39010777



aosp与内核的关系
https://zhuanlan.zhihu.com/p/245131105

GKI 通用内核Generic Kernel Image  为了解决碎片化和Android升级慢的问题
https://source.android.com/devices/architecture/kernel/generic-kernel-image

Android 通用内核
common-android-mainline
https://source.android.google.cn/devices/architecture/kernel/android-common

goldfish地址在线地址
https://android.googlesource.com/kernel/goldfish/+/refs/heads/android-goldfish-3.4/drivers/staging/android/binder.c