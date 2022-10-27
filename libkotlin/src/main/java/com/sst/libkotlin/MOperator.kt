package com.sst.libkotlin


import java.util.*
import kotlin.reflect.KClass


///操作符
class MOperator {
    companion object{
        @JvmStatic
        fun main(args: Array<String>) {
            //结构相等 equals()或==   用于比较变量中存储的值
            val a ="a"
            val b ="b"
            println(a.equals(b)) //编译为a.equals(b)
            println(a==b)
            //编译为Intrinsics.areEqual(a, b);
            //Intrinsics.class
            // public static boolean areEqual(Object first, Object second) {
            //        return first == null ? second == null : first.equals(second);
            //    }
            //引用相等 ===
            println(a===b)
            //在原始类型的情况下，===操作符也会检查值而不是引用
            val numA = 10
            val numB = 10
            println("numA === numB  ${numA===numB}") //检查的值 true
            val iA = Integer.valueOf(1000)
            val iB = Integer.valueOf(1000)
            println("iA === iB  ${iA===iB}") //结果false 检查的对象引用  Integer.valueOf存在缓存，如果10结果为false

            //类型转换 as  as?
            val any: Any = "abc"
            var str: String = any as String


//            使用安全转换操作符as?可以在转换失败时返回null，避免了抛出异常。
            val str1 = "1"
            val str2 = str1 as? String
            println(str2) //输出结果为：null


//            判断一个对象与指定的类型是否一致 is !is
            println(any is String)
            println(any !is Int)
            //编译为!(any instanceof Integer)

            //双冒号::  代表引用一个方法
            val clazz:KClass<String> = String::class//返回的是kotlin的class
            //编译为String.class
            val clazz1= any.javaClass
            //编译为any.getClass()

            //将kotlin的KClass转为Java的class
            val clazz2:Class<String> = String::class.java
            // Class clazz2 = String.class;


            //空安全
            ///非空类型
            val aa :String="a"
            ///可空类型
            var bb :String?=null
            //使用!! 操作符  !!操作符将任何值转换为非空类型，若该值为空则抛出异常   最好确定非空再使用!!，不然会NPE
            //表示告诉Kotlin我这里一定不会为空,你不用进行检测了，如果为空，则抛出空指针异常
            //同时要提醒一下自己，是否存在更好的实现方式，因为使用这种操作符，还是会存在潜在的空指针异常
            var aNull = null
//            val bNull:String = aNull!!   //此时输出为NPE异常
            //编译为
//            Intrinsics.checkNotNull(aNull);
//            String bNull = (String)aNull;

            ///安全调用操作符，表示如果若不为null才继续调用
            bb?.lowercase(Locale.ROOT)
            //与let结合
            bb?.let {
                println(it)
            }
            //安全调用操作符的缺点，只处理了安全的情况，对于异常场景丢弃了
            //同时处理有值和异常
            bb = null
            //1
            if(null == bb?.run {
                  println("this has value")
                }){
               println("this is null")
            }
            //2
            bb?.let {  }?: kotlin.run {
                println("this is null in run")
            }
            //3
            bb?.let {  }?:fun(){}()
            bb?.let {  }?:fun(){}.invoke()


            //?:  Elvis操作符  变量为空时使用默认值
            val aNu =null
            println("aNu is ${aNu?:"aa"}") //结果为aa
            //编译为
//            Void aNu = null;
//            String var14 = "aNu is " + "aa";

            //运算符重载
            val money1 = Money(15)
            val money2 = Money(20)
            val money3 = money1 + money2
            val money4 = money3 + 15
            println("money+money ${money3.value}")
            print("money+money ${money4.value}")
        }
    }
}

//运算符重载
//Kotlin 的运算符重载允许我们让任意两个对象进行相加，或者是进行其他更多的运算操作
//运算符重载使用的是 operator 关键字，我们只需要在指定函数前面加上 operator 关键字，就可以实现运算符重载的功能了
class Money(val value: Int) {

    //实现运算符重载 Money + Money
    operator fun plus(money: Money): Money {
        val sum = value + money.value
        return Money(sum)
    }

    //实现运算符重载 Money + Int
    operator fun plus(money: Int): Money{
        val sum = value + money
        return Money(sum)
    }
}
