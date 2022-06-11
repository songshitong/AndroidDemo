http://liuwangshu.cn/framework/aosp/4-import-aosp.html
http://liuwangshu.cn/framework/aosp/5-debug-aosp.html

生成AS的项目配置文件
如果你整编过源码，查看out/host/linux-x86/framework/idegen.jar是否存在，如果不存在，进入源码根目录执行如下的命令：
```
source build/envsetup.sh
lunch aosp_x86-eng   # [选择整编时选择的参数或者数字]
mmm development/tools/idegen/
```

如果没整编过源码，可以直接执行如下命令单编idegen模块：
```
source build/ensetup.sh  
make idegen
```

idegen模块编译成功后，会在 out/host/linux-x86/framework目录下生成idegen.jar，执行如下命令：
```
sudo development/tools/idegen/idegen.sh
```

这时会在源码根目录生成android.iml 和 android.ipr 两个文件，这两个文件一般是只读模式，这里建议改成可读可写，
  否则，在更改一些项目配置的时候可能会出现无法保存的情况。
```
sudo chmod 777 android.iml
sudo chmod 777 android.ipr
```

配置AS的项目配置文件
由于要将所有源码导入AS会导致第一次加载很慢，可以在android.iml中修改excludeFolder配置，将不需要看的源码排除掉。
等源码项目加载完成后，还可以通过AS对Exclude的Module进行调整。如果你的电脑的性能很好，可以不用进行配置。
在android.iml中搜索excludeFolder，在下面加入这些配置
<excludeFolder url="file://$MODULE_DIR$/bionic" />
<excludeFolder url="file://$MODULE_DIR$/kernel" />
```
<excludeFolder url="file://$MODULE_DIR$/bootable" />
<excludeFolder url="file://$MODULE_DIR$/build" />
<excludeFolder url="file://$MODULE_DIR$/cts" />
<excludeFolder url="file://$MODULE_DIR$/dalvik" />
<excludeFolder url="file://$MODULE_DIR$/developers" />
<excludeFolder url="file://$MODULE_DIR$/development" />
<excludeFolder url="file://$MODULE_DIR$/device" />
<excludeFolder url="file://$MODULE_DIR$/docs" />
<excludeFolder url="file://$MODULE_DIR$/external" />
<excludeFolder url="file://$MODULE_DIR$/hardware" />
<excludeFolder url="file://$MODULE_DIR$/out" />
<excludeFolder url="file://$MODULE_DIR$/pdk" />
<excludeFolder url="file://$MODULE_DIR$/platform_testing" />
<excludeFolder url="file://$MODULE_DIR$/prebuilts" />
<excludeFolder url="file://$MODULE_DIR$/sdk" />
<excludeFolder url="file://$MODULE_DIR$/system" />
<excludeFolder url="file://$MODULE_DIR$/test" />
<excludeFolder url="file://$MODULE_DIR$/toolchain" />
<excludeFolder url="file://$MODULE_DIR$/tools" />
<excludeFolder url="file://$MODULE_DIR$/.repo" />
```
后期可以在File -> Project Structure -> Modules -》source->Excluded中变更

导入系统源代码到AS中
在AS安装目录的bin目录下，打开studio64.vmoptions文件，根据自己电脑的实际情况进行设置，这里修改为如下数值：
```
-Xms1024m
-Xmx1024m
```

导入工程    导入前可以先把git插件禁掉，git index loging也需要很久时间
通过AS的Open an existing Android Studio project选项选择android.ipr 

配置项目的JDK、SDK
下载与asop相同版本的sdk和模拟器，工程project进行SDK配置
点击File -> Project Structure–>SDKs配置项目的JDK、SDK。    Platform setting中

创建一个新的JDK,这里取名为1.8(No Libraries)，删除其中classpath标签页下面的所有jar文件
 路径选择1.8，删除键有的在右侧，有的在底部

接着设置将对应版本Android SDK的Java SDK设置为1.8(No Libraries)，这样Android源码使用的Java就是Android源码中的



配置Android FrameWork    不进行配置Event Log处报错且不能消除提醒
修改modules-》modules
manifest file:  /frameworks/base/core/res/AndroidManifest.xml 
resources directory :  /frameworks/base/core/res/res/
assets  directory   :  /frameworks/base/core/res/assets/
建立依赖  Project Structure -> Modules -> android -> Dependencies: 先删除对应Android API Platform之外的所有依赖,
然后点击下图绿色的+号来选择Jars or directories，将frameworks添加进来, 也可添加其他所关注的源码；
Android API Platform一般在最后一个  可以点住第一个，拉倒最后，按住shift点击最后一个，然后删除


Edit Configurations
新增一个Android APP   module android,deploy  nothing, launch options nothing


调试根activity启动过程
开始调试  打开对应版本的模拟器
应用程序的启动时会调用ActivityStarter的startActivityMayWait方法
ctrl+N 查找类ActivityStarter，在ActivityStarter的startActivityMayWait方法上打断点

点击菜单的Run–>Attach Debugger to Android Process或者上方工具条的Attach Debugger to Android Process图标，
勾选Show all processer，选择system_process

在模拟器中点击Gallery应用，我们设的断点就会生效




导入部分源码进行调试
导入最常用的frameworks/base目录
File–>New–>Import project  选择导入frameworks/base目录，一路Next
  这时AS会列出frameworks/base目录下的所有项目，因为要调试ActivityStarter类，
  这里只需要导入frameworks/base/services/core/java就可以了。
剩下一路Next就可以了,项目加载进来后，按照本文1.1小节来配置项目的JDK、SDK。接下来的调试步骤和1.3小节是一样的

新建Android项目进行调试
如果我们没有下载源码，或者不想导入那么多源码，也可以新建一个Android项目来进行调试，步骤如下：
File –> New –> New Project，一路Next就可以了。
新建一个包，因为要调试ActivityStarter类，包名称就为ActivityStarter的包名com.android.server.am。
将9.0版本的ActivityStarter类复制到包中，如果没有下载源码，可以从http://androidxref.com 中下载。
按照1.3节的内容开始调试。