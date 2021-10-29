刘望舒
https://www.bilibili.com/video/BV1Yr4y1c7o5?spm_id_from=333.999.0.0
能拧螺丝的不一定造不了飞机，造的了飞机一定能拧螺丝
http://liuwangshu.cn/batcoder/aosp/2-download-aosp.html

工具  sourceinsight   在线网站
https://cs.android.com/
http://aospxref.com/
https://github.com/aosp-mirror

代码搜索技巧https://developers.google.com/code-search/user/getting-started
使用AndroidStudio 可以调试

官方编译指南
https://source.android.com/setup/build/building

AOSP
AOSP(Android Open Source Project)是Google开放的Android 开源项目，中文官网为：https://source.android.google.cn/。
AOSP通俗来讲就是一个Android系统源码项目，通过它可以定制 Android 操作系统，国内手机厂商都是在此基础上开发的定制系统。因为墙的缘故，
如果无法连接谷歌服务器获取AOSP源码，可以从 清华大学镜像站或者 中科大镜像

内核源码
AOSP源码中并不包括内核源码，需要单独下载，内核源码有很多版本，比如common是通用的Linux内核，msm是用于使用高通MSM芯片的Android设备，
goldfish是用于Android模拟器的内核源码

repo工具
Android源码包含数百个git库，光是下载这么多的git库就是一项繁重的任务，所以Google开发了repo，它是用于管理Android版本库的一个工具，
使用了Python对git进行了一定的封装，简化了对多个Git版本库的管理
repo会在执行目录生成 .repo配置  更改代码目录需要删除

编译平台  linux  macos  
docker  支持Mac和windows,比虚拟机快，编好后共享给主机,环境隔离不同Android版本对依赖库要求不同，编译成果可以在多台设备共享，指定具体的ubuntu版本
编译系统概述
Makefile
Android平台的编译系统，其实就是用Makefile写出来的一个独立项目。它定义了编译的规则，实现了“自动化编译”，不仅把分散在数百个Git库中的代码整合起来、统一编译， 而且还把产物分门别类地输出到一个目录，打包成手机ROM，还可以生成应用开发时所使用的SDK、NDK等。
因此，采用Makefile编写的编译系统，也可以称为Makefile编译系统。

Android.mk
Makefile编译系统的一部分，Android.mk是android编译环境下的一种特殊的“makefile”文件, 它是经过了android编译系统处理的。Android.mk中定义了一个模块的必要参数，使模块随着平台编译。通俗来讲就是告诉编译系统，以什么样的规则编译你的源代码，并生成对应的目标文件。

Ninja
Ninja是一个致力于速度的小型编译系统，如果把其他的编译系统看作高级语言，那么Ninja 目标就是汇编。使用Ninja 主要目的就是因为其编译速度快。
Android7.0引入

Soong
Soong是谷歌用来替代此前的Makefile编译系统的替代品，负责解析Android.bp文件，并将之转换为Ninja文件

Blueprint
Blueprint用来解析Android.bp文件翻译成Ninja语法文件。

kati
kati是谷歌专门为了Android而开发的一个小项目，基于Golang和C++。 目的是把Android中的Makefile，转换成Ninja文件。

Android.bp
Android.bp，是用来替换Android.mk的配置文件

老的分支
MakeFile or Android.mk -> kati -> ninja
新的分支
Android.bp -> BluePrint -> Soong ->ninja

jack Server
https://link.zhihu.com/?target=https%3A//source.android.com/setup/build/jack
Jack 是一种新型 Android 工具链，用于将 Java 源代码编译成 Android dex 字节码。 它取代了之前由 javac、ProGuard、jarjar 和 dx 等多种工具组成的 Android 工具链。
Jack 工具链具有以下优势：
完全开放源代码 它是在 AOSP 中提供的；并且欢迎用户贡献资源。
提高编译速度 Jack 提供以下具体支持来减少编译时间：dex 预处理、增量编译和 Jack 编译服务器。
支持压缩、混淆、重新打包和多 dex 处理 不再需要使用单独的软件包（如 ProGuard）



VirtualBox安装虚拟机
设置虚拟核数
系统开启虚拟化kvm  vitrualbox系统-》处理器开启VT-x/AMD-v
安装ubuntu无法点击继续按钮，按住win/alt+鼠标左键拖动，分辨率不匹配导致的
增强工具  共享文件，使用全屏模式
设置方式
vitrual box 设置共享文件夹，用户界面开启顶部显示
virtual box 存储挂载addition.iso,设备-》安装增强功能，ubuntu文件打开virtualbox->运行文件，重启后看到sf_share

系统下载脚本
将export REPO_URL='https://mirrors.tuna.tsinghua.edu.cn/git/git-repo/' 添加到~/.bashrc
#export REPO_URL='https://mirrors.bfsu.edu.cn/git/git-repo'

sudo apt-get update
sudo apt-get install git
mkdir ~/bin
PATH=~/bin:$PATH
sudo apt-get install curl
curl https://mirrors.tuna.tsinghua.edu.cn/git/git-repo > ~/bin/repo
curl https://mirrors.bfsu.edu.cn/git/git-repo  > ~/bin/repo
chmod a+x ~/bin/repo
#到Android11  aosp使用的Python脚本为python2   python3需要单独安装
sudo apt-get install python
mkdir aosp
cd aosp
git config --global user.email "1295208856@qq.com"
git config --global user.name "songshitong"
repo init -u https://aosp.tuna.tsinghua.edu.cn/platform/manifest -b android-8.0.0_r36  #不指定版本，默认最新  android-11.0.0_r1
#repo init -u https://mirrors.bfsu.edu.cn/git/AOSP/platform/manifest -b android-8.0.0_r36 

repo sync
#可以写个脚本循环下载，容易因为网络失败
#!/bin/bash
#repo sync -j4
#while [ $? -ne 0 ]
#do
#echo "======sync failed ,re-sync again======"
#sleep 3
#repo sync -j4
#done
#也可以使用echo 优化写文件的过程
#touch repo.sh  # 1. 创建 repo.sh 文件
#vim repo.sh # 2. 复制上面的脚本内容到 repo.sh 里面，这里你可以使用你自己喜欢的方法打开并修改文件，比如 vscode
#chmod a+x repo.sh #3. 修改权限
#./repo.sh # 4. 运行脚本，万事大吉

#开始下载内核
mkdir kernel
cd kernel
git clone https://aosp.tuna.tsinghua.edu.cn/kernel/goldfish.git
cd goldfish
#查看可以使用的分支
#git branch -a
git checkout remotes/origin/android-goldfish-3.4




lunch命令是envsetup.sh里定义的一个命令，用来让用户选择编译目标
输入lunch后，会有编译目标的提示
编译目标的格式，编译目标的格式组成为BUILD-BUILDTYPE，比如aosp_arm-eng的BUILD为aosp_arm，BUILDTYPE为eng。
其中BUILD表示编译出的镜像可以运行在什么环境，aosp代表Android开源项目，arm表示系统是运行在arm架构的处理器上。
更多参考官方文档。https://source.android.google.cn/source/running.html#selecting-device-build
BUILDTYPE 指的是编译类型，有以下三种：
  user：用来正式发布到市场的版本，权限受限，如没有 root 权限，不能 dedug，adb默认处于停用状态。
  userdebug：在user版本的基础上开放了 root 权限和 debug 权限，adb默认处于启用状态。一般用于调试真机。
  eng：开发工程师的版本，拥有最大的权限(root等)，具有额外调试工具的开发配置。一般用于模拟器。
如果你没有Nexus /ˈneksəs/ 设备，只想编译完后运行在模拟器查看，那么BUILD可以选择aosp_x86

编译结果
out/target/product/generic_x86/目录生成了三个重要的镜像文件： system.img、userdata.img、ramdisk.img。
  system.img：系统镜像，里面包含了Android系统主要的目录和文件，通过init.c进行解析并mount挂载到/system目录下。
  userdata.img：用户镜像，是Android系统中存放用户数据的，通过init.c进行解析并mount挂载到/data目录下。
  ramdisk.img：根文件系统镜像，包含一些启动Android系统的重要文件，比如init.rc

编译脚本
sudo apt-get update
sudo apt-get install openjdk-8-jdk
sudo apt-get install git-core gnupg flex bison gperf build-essential zip curl zlib1g-dev gcc-multilib g++-multilib libc6-dev-i386 lib32ncurses5-dev x11proto-core-dev libx11-dev lib32z-dev ccache libgl1-mesa-dev libxml2-utils xsltproc unzip
#初始化环境，这个脚本会引入其他的执行脚本
source build/envsetup.sh
#清除缓存
make clobber
lunch aosp_x86-eng
#并行任务数   任务数越大速度越快，需要的内存和机器配置越高
make -j6
#启动模拟器
emulator

编译后下一次启动
source build/envsetup.sh
lunch aosp_x86-eng
emulator


源码单编
比如我们要编译系统的Settings应用模块，
在AOSP根目录执行：
```
source build/envsetup.sh
lunch aosp_x86-eng
cd packages/apps/Settings
mm
```
编译结果
Settings.odex文件，
还会在out/target/product/generic_x86/system/priv-app/Settings目录下生成Settings.apk
单编命令
mm编译当前目录下的模块，不编译依赖模块
mmm：编译指定目录下的模块，不编译它所依赖的其它模块。
mma：编译当前目录下的模块及其依赖项。
mmma：编译指定路径下所有模块，并且包含依赖

如果你修改了源码，想查看生成的APK文件，有两种方式：
通过adb push或者adb install 来安装APK。
使用make snod命令，重新生成 system.img，运行模拟器查看


模拟器路径
/aosp/prebuilts/android-emulator/





docker编译
参考http://qiushao.net/2019/11/14/Linux/docker-aosp-build-env/
根据docker file创建容器
git clone https://github.com/qiushao/aosp_builder.git
cd aosp_builder
#Build an image from a Dockerfile
docker build --network host -t aosp_builder:V1.0 .

登录镜像
docker run -it --network host  --name aosp_builder -v /c/develop/aosp/source:/home/builder/source -u builder aosp_builder:V1.0 /bin/bash
-v文件映射  /c/develop/aosp/source宿主机   /home/builder/source镜像目录  
-u 以builder身份登录
docker run -it --network host  --name aosp_builder -v \\wsl.localhost\Ubuntu\home\song\aosp:/home/builder/source -u builder aosp_builder:V1.0 /bin/bash

启动镜像
docker start aosp_builder
docker exec -it aosp_builder /bin/bash