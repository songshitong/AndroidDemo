package com.sst.libkotlin

///操作符
class MOperator {
    companion object{
        @JvmStatic
        fun main(args: Array<String>) {
           //结构相等 equals()或==
            val a ="a"
            val b ="b"
            print(a.equals(b))
            print(a==b)
           //引用相等 ===
           print(a===b)

           //类型转换 as  as?
            val any: Any = "abc"
            var str: String = any as String


//            使用安全转换操作符as?可以在转换失败时返回null，避免了抛出异常。
            val str1 = null
            val str2 = str1 as? String
            println(str2) //输出结果为：null


//            判断一个对象与指定的类型是否一致 is !is
            print(any is String)
            print(any !is Int)

           //双冒号::  代表引用一个方法
            val clazz = String::class
            val clazz1= any.javaClass
        }
    }
}