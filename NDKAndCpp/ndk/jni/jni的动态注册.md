http://liuwangshu.cn/framework/jni/1-mediarecorder_register.html

动态注册
JNI中有一种结构用来记录Java的Native方法和JNI方法的关联关系，它就是JNINativeMethod，它在jni.h中被定义：
```
typedef struct {
    const char* name;//Java方法的名字
    const char* signature;//Java方法的签名信息
    void*       fnPtr;//JNI中对应的方法指针
} JNINativeMethod;
```
系统的MediaRecorder采用的就是动态注册，我们来查看它的JNI层是怎么做的。
frameworks/base/media/jni/android_media_MediaRecorder.cpp
```
static const JNINativeMethod gMethods[] = {
...
    {"start",            "()V",      (void *)android_media_MediaRecorder_start},//1
    {"stop",             "()V",      (void *)android_media_MediaRecorder_stop},
   ...
};
```
上面定义了一个JNINativeMethod类型的gMethods数组，里面存储的就是MediaRecorder的Native方法与JNI层方法的对应关系
”()V”是start方法的签名信息
只定义JNINativeMethod 类型的数组是没有用的，还需要注册它，注册的方法为register_android_media_MediaRecorder：
frameworks/base/media/jni/android_media_MediaRecorder.cpp
```
int register_android_media_MediaRecorder(JNIEnv *env)
{
    return AndroidRuntime::registerNativeMethods(env,
                "android/media/MediaRecorder", gMethods, NELEM(gMethods));
}
```
register_android_media_MediaRecorder方法中return了AndroidRuntime的registerNativeMethods方法，如下所示。
frameworks/base/core/jni/AndroidRuntime.cpp
```
/*static*/ int AndroidRuntime::registerNativeMethods(JNIEnv* env,
    const char* className, const JNINativeMethod* gMethods, int numMethods)
{
    return jniRegisterNativeMethods(env, className, gMethods, numMethods);
}
```
registerNativeMethods方法中又return了jniRegisterNativeMethods方法：
libnativehelper/JNIHelp.cpp
```
extern "C" int jniRegisterNativeMethods(JNIEnv* env, const char* className,
    const JNINativeMethod* gMethods, int numMethods)
{
   ...
    if (env->RegisterNatives(c.get(), gMethods, numMethods) < 0) {//1
        char* msg;
        (void)asprintf(&msg, "RegisterNatives failed for '%s'; aborting...", className);
        env->FatalError(msg);
    }
    return 0;
}
```
可以看出，最终调用的JNIEnv的RegisterNatives方法
register_android_media_MediaRecorder方法是在哪被调用的呢？答案在register_android_media_MediaRecorder方法的注释上：
JNI_OnLoad in android_media_MediaPlayer.cpp。这个JNI_OnLoad方法会在System.loadLibrary方法后调用，
因为多媒体框架中的很多框架都要进行JNINativeMethod数组注册，因此，注册方法就被统一定义在android_media_MediaPlayer.cpp中的
JNI_OnLoad方法中，如下所示。
frameworks/base/media/jni/android_media_MediaPlayer.cpp
```
jint JNI_OnLoad(JavaVM* vm, void* /* reserved */)
{
    JNIEnv* env = NULL;
    jint result = -1;
    if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        ALOGE("ERROR: GetEnv failed\n");
        goto *bail;
    }
    assert(env != NULL);
    ...
    if (register_android_media_MediaPlayer(env) < 0) {
        ALOGE("ERROR: MediaPlayer native registration failed\n");
        goto *bail;
    }
    if (register_android_media_MediaRecorder(env) < 0) {//1
        ALOGE("ERROR: MediaRecorder native registration failed\n");
        goto *bail;
    }
  ...
   result = JNI_VERSION_1_4;
bail:
    return result;
}
```
在JNI_OnLoad方法中调用了整个多媒体框架的注册JNINativeMethod数组的方法，注释1处的调用了register_android_media_MediaRecorder方法，
同样的，MediaPlayer框架的注册JNINativeMethod数组的方法register_android_media_MediaPlayer也被调用了