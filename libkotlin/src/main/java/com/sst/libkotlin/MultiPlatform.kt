package com.sst.libkotlin

import java.util.*

class MultiPlatform {
    ///actual关键字 代表与平台特性相关
//    actual fun randomUUID() = UUID.randomUUID().toString()

//    例如LazyKt.class定义的lazy函数
//    public fun <T> lazy(mode: kotlin.LazyThreadSafetyMode, initializer: () -> T): kotlin.Lazy<T> { /* compiled code */ }
//      在LazyJVM.kt存在具体的实现
//    public actual fun <T> lazy(lock: Any?, initializer: () -> T): Lazy<T> = SynchronizedLazyImpl(initializer, lock)


  //external代表不是kotlin实现的，可以是jni或js
  // private external fun nInit();
}