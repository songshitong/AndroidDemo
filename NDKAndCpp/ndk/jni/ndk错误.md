
https://github.com/xfhy/Android-Notes/blob/master/Blogs/Android/NDK/4_JNI_NDK%E5%85%A5%E9%97%A8%E8%AF%A6%E8%A7%A3.md

NDK Crash错误定位
当NDK发生错误某种致命的错误的时候,会导致APP闪退.这类错误非常不好查问题,比如内存地址访问错误,使用野指针,内存泄露,堆栈溢出,
  数字除0等native错误都会导致APP崩溃.

虽然这些NDK错误不好排查,但是我们在NDK错误发生后,拿到logcat输出的堆栈日志,再结合下面的2款调试工具--addr2line和ndk-stack,
  能够精确地定位到相应发生错误的代码行数,然后迅速找到问题.

首先到ndk目录下,来到sdk/ndk/21.0.6113669/toolchains/目录,我本地的目录如下,可以看到NDK交叉编译器工具链的目录结构.
```
aarch64-linux-android-4.9
arm-linux-androideabi-4.9
llvm
renderscript
x86-4.9
x86_64-4.9
```
其中ndk-stack放在$NDK_HOME目录下，与ndk-build同级目录。addr2line在ndk的交叉编译器工具链目录下.NDK针对不同的CPU架构实现了多套工具.
在使用addr2line工具时,需要根据当前手机cpu架构来选择.我的手机是aarch64的,则使用aarch64-linux-android-4.9目录下的工具.
查看手机的cpu信息的命令:adb shell cat /proc/cpuinfo

在介绍两款调试工具之前,我们得先写好能崩溃的native代码,方便看效果. 我在demo的native-lib.cpp里面写了如下代码:
```
void willCrash() {
    JNIEnv *env = NULL;
    int version = env->GetVersion();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_xfhy_allinone_jni_CallMethodActivity_nativeCrashTest(JNIEnv *env, jobject thiz) {
    LOGI("崩溃前");
    willCrash();
    //后面的代码是执行不到的,因为崩溃了
    LOGI("崩溃后");
    printf("oooo");
}
```
这段代码,是很明显的空指针错误.我本来想搞一个除0错误的,但是除0死活不崩溃,,,不知道为啥(进化了?). 然后运行起来,错误日志如下:
```
2020-06-07 17:05:25.230 12340-12340/? A/DEBUG: *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** ***
2020-06-07 17:05:25.230 12340-12340/? A/DEBUG: Build fingerprint: 'Xiaomi/dipper/dipper:10/QKQ1.190828.002/V11.0.8.0.QEACNXM:user/release-keys'
2020-06-07 17:05:25.230 12340-12340/? A/DEBUG: Revision: '0'
2020-06-07 17:05:25.230 12340-12340/? A/DEBUG: ABI: 'arm64'
2020-06-07 17:05:25.237 12340-12340/? A/DEBUG: Timestamp: 2020-06-07 17:05:25+0800
2020-06-07 17:05:25.237 12340-12340/? A/DEBUG: pid: 11527, tid: 11527, name: m.xfhy.allinone  >>> com.xfhy.allinone <<<
2020-06-07 17:05:25.237 12340-12340/? A/DEBUG: uid: 10319
2020-06-07 17:05:25.237 12340-12340/? A/DEBUG: signal 11 (SIGSEGV), code 1 (SEGV_MAPERR), fault addr 0x0
2020-06-07 17:05:25.237 12340-12340/? A/DEBUG: Cause: null pointer dereference
2020-06-07 17:05:25.237 12340-12340/? A/DEBUG:     x0  0000000000000000  x1  0000007fd29ffd40  x2  0000000000000005  x3  0000000000000003
2020-06-07 17:05:25.237 12340-12340/? A/DEBUG:     x4  0000000000000000  x5  8080800000000000  x6  fefeff6fb0ce1f1f  x7  7f7f7f7fffff7f7f
2020-06-07 17:05:25.237 12340-12340/? A/DEBUG:     x8  0000000000000000  x9  a95a4ec0adb574df  x10 0000007fd29ffee0  x11 000000000000000a
2020-06-07 17:05:25.237 12340-12340/? A/DEBUG:     x12 0000000000000018  x13 ffffffffffffffff  x14 0000000000000004  x15 ffffffffffffffff
2020-06-07 17:05:25.237 12340-12340/? A/DEBUG:     x16 0000006fc6476c50  x17 0000006fc64513cc  x18 00000070b21f6000  x19 000000702d069c00
2020-06-07 17:05:25.237 12340-12340/? A/DEBUG:     x20 0000000000000000  x21 000000702d069c00  x22 0000007fd2a00720  x23 0000006fc6ceb127
2020-06-07 17:05:25.237 12340-12340/? A/DEBUG:     x24 0000000000000004  x25 00000070b1cf2020  x26 000000702d069cb0  x27 0000000000000001
2020-06-07 17:05:25.237 12340-12340/? A/DEBUG:     x28 0000007fd2a004b0  x29 0000007fd2a00420
2020-06-07 17:05:25.237 12340-12340/? A/DEBUG:     sp  0000007fd2a00410  lr  0000006fc64513bc  pc  0000006fc64513e0
2020-06-07 17:05:25.788 12340-12340/? A/DEBUG: backtrace:
2020-06-07 17:05:25.788 12340-12340/? A/DEBUG:       #00 pc 00000000000113e0  /data/app/com.xfhy.allinone-4VScOmUWz8wLqqwBWZCP2w==/lib/arm64/libnative-lib.so (_JNIEnv::GetVersion()+20) (BuildId: b1130c28a8b45feda869397e55c5b6d754410c8d)
2020-06-07 17:05:25.788 12340-12340/? A/DEBUG:       #01 pc 00000000000113b8  /data/app/com.xfhy.allinone-4VScOmUWz8wLqqwBWZCP2w==/lib/arm64/libnative-lib.so (willCrash()+24) (BuildId: b1130c28a8b45feda869397e55c5b6d754410c8d)
2020-06-07 17:05:25.788 12340-12340/? A/DEBUG:       #02 pc 0000000000011450  /data/app/com.xfhy.allinone-4VScOmUWz8wLqqwBWZCP2w==/lib/arm64/libnative-lib.so (Java_com_xfhy_allinone_jni_CallMethodActivity_nativeCrashTest+84) (BuildId: b1130c28a8b45feda869397e55c5b6d754410c8d)
2020-06-07 17:05:25.788 12340-12340/? A/DEBUG:       #03 pc 000000000013f350  /apex/com.android.runtime/lib64/libart.so (art_quick_generic_jni_trampoline+144) (BuildId: 2bc2e11d57f839316bf2a42bbfdf943a)
2020-06-07 17:05:25.788 12340-12340/? A/DEBUG:       #04 pc 0000000000136334  /apex/com.android.runtime/lib64/libart.so (art_quick_invoke_stub+548) (BuildId: 2bc2e11d57f839316bf2a42bbfdf943a)
```
看到这种错误,首先找到关键信息Cause: null pointer dereference,但是我们不知道发生在具体哪里,只知道是这个错误.


addr2line
有了错误日志,现在我们使用工具addr2line来定位位置.

需要执行命令 /Users/xfhy/development/sdk/ndk/21.0.6113669/toolchains/aarch64-linux-android-4.9/prebuilt/darwin-x86_64/bin/
aarch64-linux-android-addr2line -e /Users/xfhy/development/AllInOne/app/libnative-lib.so 00000000000113e0 00000000000113b8
,这其中-e是指定so文件的位置,然后末尾的00000000000113e0和00000000000113b8是出错位置的汇编指令地址.

执行之后,结果如下:
```
/Users/xfhy/development/sdk/ndk/21.0.6113669/toolchains/llvm/prebuilt/darwin-x86_64/sysroot/usr/include/jni.h:497
/Users/xfhy/development/AllInOne/app/src/main/cpp/native-lib.cpp:260
```
可以看到是native-lib.cpp的260行出的问题.我们只需要去找到这个位置,修复这个bug即可.完美,瞬间就找到了.



ndk-stack
还有一种更简单的方式,直接输入命令
adb logcat | ndk-stack -sym /Users/xfhy/development/AllInOne/app/build/intermediates/cmake/debug/obj/arm64-v8a,末尾是so文件的位置.
执行完命令,然后在手机上产生native错误就能在这个so文件中定位到这个错误点,如下:
```
********** Crash dump: **********
Build fingerprint: 'Xiaomi/dipper/dipper:10/QKQ1.190828.002/V11.0.8.0.QEACNXM:user/release-keys'
#00 0x00000000000113e0 /data/app/com.xfhy.allinone-oVu0tjta-aW9LYa08eoK1Q==/lib/arm64/libnative-lib.so (_JNIEnv::GetVersion()+20) (BuildId: b1130c28a8b45feda869397e55c5b6d754410c8d)
                                                                                                        _JNIEnv::GetVersion()
                                                                                                        /Users/xfhy/development/sdk/ndk/21.0.6113669/toolchains/llvm/prebuilt/darwin-x86_64/sysroot/usr/include/jni.h:497:14
#01 0x00000000000113b8 /data/app/com.xfhy.allinone-oVu0tjta-aW9LYa08eoK1Q==/lib/arm64/libnative-lib.so (willCrash()+24) (BuildId: b1130c28a8b45feda869397e55c5b6d754410c8d)
                                                                                                        willCrash()
                                                                                                        /Users/xfhy/development/AllInOne/app/src/main/cpp/native-lib.cpp:260:24
#02 0x0000000000011450 /data/app/com.xfhy.allinone-oVu0tjta-aW9LYa08eoK1Q==/lib/arm64/libnative-lib.so (Java_com_xfhy_allinone_jni_CallMethodActivity_nativeCrashTest+84) (BuildId: b1130c28a8b45feda869397e55c5b6d754410c8d)
                                                                                                        Java_com_xfhy_allinone_jni_CallMethodActivity_nativeCrashTest
                                                                                                        /Users/xfhy/development/AllInOne/app/src/main/cpp/native-lib.cpp:267:5
```
可以看出是willCrash()方法出的错,它的代码行数是260行(甚至连列都打印出来了,,第24列).

