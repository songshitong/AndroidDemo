package com.sst.libkotlin.clasz

// 主构造器constructor   age是默认参数
//若主构造函数无任何注解 / 可见性修饰符，可省略 constructor 关键字

//类默认不可以被继承，使用open表示可以继承

//    private ：本类内部都可见
//    protected ：本类内部 & 子类中可见
//    public：能见到类声明的任何客户端都可以见（public成员）
//    internal：能见到类声明的本模块内的任何客户端都可见（public成员）
//    区别于Java，Kotlin的可见修饰符少了default，多了internal：该成员只在相同模块内可见。（注：一个模块 = 编译在一起的一套 Kotlin 文件：
//    一个 IntelliJ IDEA 模块；
//    一个 Maven 项目；
//    一个 Gradle 源集；
//    一次 ＜kotlinc＞ Ant 任务执行所编译的一套文件
open class User constructor(userName: String,age :Int =10) {
    //主构造器的代码必须放到init代码块
    init {
        //...
    }

    //构造器默认公开的
    // 次构造函数1：可通过this调主构造函数
    constructor() : this("hjc")

    // 次构造函数2：可通过this调主构造函数
    constructor(age: Int) : this("hjc") {
        println(age)
    }

    // 次构造函数3：通过this调主构造函数
    constructor(sex: String, age: Int,like:String) : this("hjc") {
        println("$sex$age$like")
    }

    ///variable变量
    lateinit var userName1: String
    ///value 只读
    val sex: String = "男"



    ///方法默认不可以重写，子类重写使用override关键字，并且父类的方法使用open
    open fun  signIn() {
    }


    //编译为 User里面 public final class User2
    inner  class User2{

    }
}

///接口
interface UserInterface{
    fun getName(): String // 无默认方法体，必须重写
    fun getAge(): Int{    // 有默认方法体，可不重写
        return 22
    }
}

class UserChild : User(),UserInterface {

    override fun signIn() {
        super.signIn()
    }

    override fun getName(): String {
       return "aa"
    }
}



/**
 * 3. 数据类
 * 作用：保存数据
 * 标识：关键字data
 */
// 使用：创建类时会自动创建以下方法：
//      1. getter/setter方法；
//      2. equals() / hashCode() 对；
//      3. toString() ：输出"类名(参数+参数值)"；
//      4. copy() 函数：复制一个对象&改变它的一些属性，但其余部分保持不变 浅克隆

data class UserData(var userName: String, var age: Int)