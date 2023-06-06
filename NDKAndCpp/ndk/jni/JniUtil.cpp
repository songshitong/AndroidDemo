//
// Created by songshitong on 2023/6/6.
//
#include <unistd.h>
#include <jni.h>

#include "JniUtil.h"
class JniUtil {
    //将jstring转为std::string 释放jni的引用
    std::string convertJString(JNIEnv *env,jstring str){
        char *charStr = const_cast<char *>(env->GetStringUTFChars(str, nullptr));
        std::string result(charStr, env->GetStringLength(str));
        env->ReleaseStringUTFChars(str, charStr);
        return result;
    }

//    多次获取bean的属性  clazz是bean的类，obj是bean的实例,propertyName一般为"getUserName"形式
//获取string类型属性
    const char* getStringProperty(JNIEnv *env,jclass clazz,jobject obj,const char* propertyName){
        jmethodID methodId = env->GetMethodID(clazz,propertyName,"()Ljava/lang/String;");
        auto jResult = (jstring)env->CallObjectMethod(obj, methodId);
        const char *result_str = env->GetStringUTFChars(jResult, nullptr);
        env->DeleteLocalRef(jResult);
        return result_str;
    }

    int getIntProperty(JNIEnv *env,jclass clazz,jobject obj,const char* propertyName){
        jmethodID methodId = env->GetMethodID(clazz,propertyName,"()I");
        int jResult = env->CallIntMethod(obj, methodId);
        return jResult;
    }
};