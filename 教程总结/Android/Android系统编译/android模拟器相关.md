
qemu:Quick Emulator
android模拟器基于qemu定制，增加android的特性 例如(OpenGL, GPS, GSM, Sensors等)的模拟，访问Google Play Store，
phone calls 和 text messages等，开源的，可以自定制
https://android.googlesource.com/platform/external/qemu/+/2db80f7c1921a6f5d48b998378e3792e16c968a4/README.md


官网
https://www.qemu.org/

模拟器内核
 QEMU 1 (goldfish), QEMU 2 (ranchu).

内核版本，建议使用和sdk编译时使用的版本

https://developer.android.com/studio/run/emulator-commandline#system-filedir
默认镜像
windows:C:\Users\..\AppData\Local\Android\Sdk\system-images
Mac OS X and Linux: ~/Library/Android/sdk/system-images/android-apiLevel/variant/arch/

androidStudio创建的模拟器位置
C:\Users\...\.android\avd\Pixel_6_API_33.avd    AVD的位置
C:\Users\..\AppData\Local\Android\Sdk\emulator  模拟器程序


模拟器版本
模拟器help->emulator version: 31.3.12-9126400
命令查看：emulator -version

运行系统的版本：系统设置-》about emulated device->
model:sdk_gphone64_x86_64
build number:sdk_gphone64_x86_64-userdebug 13 ....



模拟器设置网络延迟
emulator -netdelay gprs
emulator -netdelay 40,100
网络速度
network speed 14.4 80


命令行
https://developer.android.com/studio/run/emulator-commandline
