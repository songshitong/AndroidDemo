
https://android.googlesource.com/platform/libcore/+/master/dalvik/src/main/java/dalvik/annotation/optimization/FastNative.java
标记为私有的，需要自己手动拷贝到项目使用
红米K30PRO 骁龙865 100*10000次的jni调用优化为为几十毫秒

FastNative和CriticalNative对比
1 @FastNative 注解支持非静态方法。支持jobject 作为参数或返回值进行访问
2 @CriticalNative更快，但是存在限制
  方法必须是静态方法 - 没有参数、返回值或隐式 this 的对象。
  仅将基元类型传递给原生方法。
  原生方法在其函数定义中不使用 JNIEnv 和 jclass 参数。
  方法必须使用 RegisterNatives 进行注册，而不是依靠动态 JNI 链接

RegisterNatives示例
```
JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *) {
    JNIEnv *env; //获取env
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) [[unlikely]] {
        return JNI_ERR;
    }
    //方法名称和签名  string作为参数
    JNINativeMethod methodsInternal[] = {
            { "nLog", "(Ljava/lang/String;)V", (void *) nLog },
    };
    //注册
    env->RegisterNatives(
            env->FindClass("com/XXX"),
            methodsInternal,
            sizeof(methodsInternal) / sizeof(JNINativeMethod));
    return JNI_VERSION_1_6;
}
```