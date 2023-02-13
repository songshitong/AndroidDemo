

Java sdk集成  未裁剪，依赖库很大
1 https://github.com/opencv/opencv/tree/master 下载sdk
2 androidStudio->new module->import->OpenCV-android-sdk/sdk
3 配置ndk，module依赖等
implementation project(path: ':opencv')
4 测试是否集成成功
boolean success = OpenCVLoader.initDebug();
也可以使用OpenCVNativeLoader

Native集成   需要自己编写jni方法  https://juejin.cn/post/7082320749977042980
如果项目中只需要在native层编写OpenCV代码，则只需要集成native层即可，可以删除上述的java目录以减小包体积。
项目中的opencv_sdk模块中只需要引入OpenCV头文件（在官网产物的OpenCV-android-sdk/sdk/native/jni/include目录下）和OpenCV动态库
动态库重复问题
```
android {
    packagingOptions {
        pickFirst 'lib/arm64-v8a/libc++_shared.so'
        pickFirst 'lib/armeabi-v7a/libc++_shared.so'
        pickFirst 'lib/arm64-v8a/libopencv_java4.so'
        pickFirst 'lib/armeabi-v7a/libopencv_java4.so'
    }
}
```