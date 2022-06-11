package com.sst.libkotlin

import java.io.IOException
import java.lang.NullPointerException
import kotlin.jvm.Throws

//kotlin中的注解
//
class MAnnotation {

    //加锁 等同Java的Synchronized关键字
    @Synchronized
    fun getStr():String{return "aa"}

    ///可见性 效果等同Java的Volatile关键字
    @Volatile
    var strVolatile ="Volatile"
    companion object{
        @JvmStatic
        val a="a"
        //只能用在伴生对象，修饰伴生对象的属性和函数  告诉编译器编译为真正的JVM静态成员(字节码层面处理)
        @JvmStatic
        fun main(args: Array<String>) {
        }

        //不对该属性生成getter/setter，并将其暴露为Java的属性
        @JvmField
        val field="f"

        //要在 Java 代码中使用 Kotlin 代码中作为参数传递的默认值，我们需要使用@JvmOverloads注解
        @JvmOverloads
        fun overloads(str:String="22"){

        }
        //生成默认方法
//        @JvmOverloads
//        public final void overloads(@NotNull String str) {
//            Intrinsics.checkNotNullParameter(str, "str");
//        }
        //生成使用默认值的重载方法
//        @JvmOverloads
//        public final void overloads() {
//            overloads$default(this, (String)null, 1, (Object)null);
//        }
//        // $FF: synthetic method
//        public static void overloads$default(MAnnotation.Companion var0, String var1, int var2, Object var3) {
//            if ((var2 & 1) != 0) {
//                var1 = "22";
//            }
//
//            var0.overloads(var1);
//        }
    }

    //抛出Java的IOException
    //翻译为
    //String readFile(String name) throws IOException {...}
    @Throws(IOException::class)
    fun getAStr() {
        "11"
    }
}