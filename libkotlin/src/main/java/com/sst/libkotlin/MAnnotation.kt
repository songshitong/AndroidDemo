package com.sst.libkotlin

import java.io.IOException
import java.lang.NullPointerException
import kotlin.jvm.Throws

//kotlin中的注解
//
class MAnnotation {
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
    }

    //抛出Java的IOException
    //翻译为
    //String readFile(String name) throws IOException {...}
    @Throws(IOException::class)
    fun getAStr() {
        "11"
    }
}