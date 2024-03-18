https://source.android.com/docs/setup/create/cuttlefish
https://juejin.cn/post/7160930401844723720
Cuttlefish 是一种可配置的虚拟 Android 设备，既可以远程运行（使用第三方云产品，如 Google Cloud Engine），
又可以在本地运行（在 Linux x86 机器上）
Cuttlefish 的目标
使平台和应用开发者不再依赖于物理硬件来开发和验证代码更改。
通过与核心框架保持高度一致，以高保真度为重点来复制真实设备的基于框架的行为。
支持 API 级别 28 之后的所有 API 级别。
在各个 API 级别达到一致的功能水平，与物理硬件上的行为保持一致。
实现规模化：
    能够并行运行多台设备。
    能够并发执行测试，实现高保真度且入门成本较低。
提供可配置的设备，能够调整外形规格、RAM、CPU 等。

Cuttlefish 与其他设备的对比情况
Cuttlefish 和 Android 模拟器
Cuttlefish 与 Android 模拟器有许多相似之处，但 Cuttlefish 可以保证 Android 框架（无论这是纯 AOSP，还是您自己的树中的自定义实现）实现全保真。
在实际应用中，这意味着 Cuttlefish 应该会在操作系统级别响应您的互动，就像使用相同的自定义或纯 Android OS 源代码构建的实体手机目标一样。

Android 模拟器围绕简化应用开发的用例构建而成，它包含许多功能钩子来迎合 Android 应用开发者的用例。如果您要使用您的自定义 Android 框架来构建模拟器，
这可能会带来一些挑战。如果您需要能够代表您的自定义平台/框架代码或 Android 树形结构的虚拟设备，那么 Cuttlefish 虚拟设备是理想的选择。
它是用于表示当前 AOSP 开发状态的规范设备。

Cuttlefish 和物理设备
Cuttlefish 虚拟设备与实体设备之间的主要区别在于硬件抽象层 (HAL) 级别，以及与任何自定义硬件互动的任何软件。除了硬件专用实现之外，
您应该会发现 Cuttlefish 和实体设备表现出在功能上等效的行为。



需要支持虚拟化
grep -c -w "vmx\|svm" /proc/cpuinfo


安装依赖
```
sudo apt install -y git devscripts config-package-dev debhelper-compat golang curl
git clone https://github.com/google/android-cuttlefish.git
cd android-cuttlefish
for dir in base frontend; do
  cd $dir
  debuild -i -us -uc -b -d
  cd ..
done
sudo dpkg -i ./cuttlefish-base_*_*64.deb || sudo apt-get install -f
sudo dpkg -i ./cuttlefish-user_*_*64.deb || sudo apt-get install -f
sudo usermod -aG kvm,cvdnetwork,render $USER
sudo reboot
```

编译aosp镜像
source build/envsetup.sh && lunch aosp_cf_x86_64_phone-eng
编译命令
m   //不推荐设置j，m会自动选择,多了内存溢出，少了速度慢

模拟器开机
```
launch_cvd --start_webrtc=true
launch_cvd --start_webrtc=true --num_instances= 2  //多个实例
stop_cvd //停止
```
https://localhost:8443 打开网页

如果提示 launch_cvd 找不到，执行 source build/envsetup.sh && lunch aosp_cf_x86_64_phone-eng
source build/envsetup.sh && lunch aosp_cf_x86_64_phone-userdebug


//开发示例
修改framework的setting
在DashboardFragment.java的onCrate中增加toast
import android.widget.Toast;
Toast.makeText(getPrefContext(), "Hello Cuttlfish", Toast.LENGTH_SHORT).show();
重新编译后，打开setting，可以看到提示


