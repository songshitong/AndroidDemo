package com.sst.libkotlin


//饿汉模式
object  MSingleton {
}
//编译为 Java 饿汉模式
//public final class MSingleton {
//    @NotNull
//    public static final MSingleton INSTANCE;
//
//    private MSingleton() {
//    }
//
//    static {
//        MSingleton var0 = new MSingleton();
//        INSTANCE = var0;
//    }
//}


//懒汉模式
class SingletonDemo private constructor() {
    companion object {
        private var instance: SingletonDemo? = null
            get() {
                if (field == null) {
                    field = SingletonDemo()
                }
                return field
            }
        fun get(): SingletonDemo{
            //细心的小伙伴肯定发现了，这里不用getInstance作为为方法名，是因为在伴生对象声明时，内部已有getInstance方法，所以只能取其他名字
            return instance!!
        }
    }
}

///线程安全的懒汉
class SingletonDemo1 private constructor() {
    companion object {
        private var instance: SingletonDemo1? = null
            get() {
                if (field == null) {
                    field = SingletonDemo1()
                }
                return field
            }
        @Synchronized
        fun get(): SingletonDemo1{
            return instance!!
        }
    }

}

///双重校验锁
class SingletonDemo2 private constructor() {
    companion object {
        val instance: SingletonDemo2 by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            SingletonDemo2() }
    }
}


///静态内部类
class SingletonDemo3 private constructor() {
    companion object {
        val instance = SingletonHolder3.holder
    }

    private object SingletonHolder3 {
        val holder= SingletonDemo3()
    }

}