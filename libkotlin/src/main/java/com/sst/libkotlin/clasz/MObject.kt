package com.sst.libkotlin.clasz

//kotlin object关键字的用法
//1、单例
//2、伴生对象   Companion伴侣
object MObjectSingleton{

}

class MObject {
//    https://www.bilibili.com/video/BV1Yq4y1U7rS?p=2
    //伴生对象 --类内部的对象声明
    //可以忽略名称
    //每一个类只能有一个伴生对象
    //不能用在object关键字里面
    //
    //原理：查看反编字节码  伴生对象编译为本类内部的静态单例内部类Factory
    //伴生对象的意义
    //kotlin的一个特性：没有静态成员
    //静态成员在Java中有很大的作用，因为Java没有全局变量，也不存在包级函数，一切属性和方法都是在类里面，所以在写一些工具函数和全局变量时
    //都需要用到static关键字修饰
    //Kotlin之所以能够抛弃静态成员，主要原因在于它允许包级属性和函数的存在
    //经典做法：kotlin允许在类中使用compainon object创建伴生对象，用伴生对象的成员来代替静态成员
    companion object Factory {
        //定义Java中的静态方法
        fun create(): MObject = MObject()

        @JvmStatic
        fun main(args: Array<String>) {
            //调用
            MObject.Factory.create()
        }
    }
}

class  MObject1{
    companion object{
        fun create():Unit {}
    }
}