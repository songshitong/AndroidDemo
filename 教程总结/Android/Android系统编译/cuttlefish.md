https://source.android.com/docs/setup/create/cuttlefish
https://juejin.cn/post/7160930401844723720
Cuttlefish 是一种可配置的虚拟 Android 设备，既可以远程运行（使用第三方云产品，如 Google Cloud Engine），又可以在本地运行（在 Linux x86 机器上）

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


