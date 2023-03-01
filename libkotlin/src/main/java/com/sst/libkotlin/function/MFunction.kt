package com.sst.libkotlin.function

import org.jetbrains.annotations.NotNull
import java.util.logging.Handler
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

//Kotlin 函数都是头等的，这意味着它们可以存储在变量与数据结构中、作为参数传递给其他高阶函数以及从其他高阶函数返回
//高阶函数
//高阶函数是将函数用作参数或返回值的函数

//顶层函数
fun getName():String{
    return ""
}
//编译为  类名为*Kt，然后是定义的方法
//public final class MFunctionKt {
//    @NotNull
//    public static final String getName() {
//        return "";
//    }
//}

class MFunction {
    companion object{
        @JvmStatic
        fun main(args: Array<String>) {
            val mf = MFunction()
            println(mf.fo1)//结果为kotlin.Unit

            println("this ===")
            //lambda中的this
            Thread {
                println(this.javaClass) //this class com.sst.libkotlin.function.MFunction$Companion
            }.start()
            //模拟handler中的写法
            postDelay({},10000)
            run {
               println(this.javaClass) //this class com.sst.libkotlin.function.MFunction$Companion
            }
            "11".run {
                println(this.javaClass)//this 为class java.lang.String
            }
        }

      fun postDelay(runnable: Runnable,delay:Long){
        println("this is postDelay")
      }
    }
    //定义一个函数
    // 模板：
//    fun 函数名（参数名：参数类型）：返回值类型{
//        函数体
//        return 返回值
//    }
    fun add(a: Int, b: Int): Int {
        return a + b
    }
    // 简写：若函数体只有一条语句 & 有返回值，那么可省略函数体的大括号，变成单表达式函数 使用等号简化
    fun add1(a: Int, b: Int) = a + b


    //默认参数
    // 给int参数指定默认值为1
    fun foo(str: String, int: Int = 1) {
        println("$str  $int")
        println(this.javaClass)
        //this为 class com.sst.libkotlin.function.MFunction
    }
    // 调用
    val fo= foo(str = "hello")  // 使用参数的命名来指定值
    val fo1 = foo("hello") //默认指定为第一个参数
//    val fo2 = foo(1)  //出现编译错误

    //无返回值
    fun foo1(){
        1+2
    }
    //无返回值，但声明为Unit  Unit相当于Java的void
    fun foo2():Unit{
        1+2
    }


//    lambda
//    lambda 表达式与匿名函数是“函数字面值”，即未声明的函数， 但立即做为表达式传递。
    //1 Lambda 完整的表达式的语法结构：{ 参数名1：参数类型，参数名2：参数类型 -> 函数体 }
    //val sum: (Int, Int) -> Int = { x: Int, y: Int -> x + y }
    //2 Lambda 表达式的参数列表中只有一个参数的时候，我们可以把参数给省略，默认会有个 it 参数，此时只有一个函数体{函数体}
    //ints.filter { it > 0 }
    //3 当 Lambda 表达式作为函数的最后一个参数的时候，我们可以把 Lambda 表达式移到函数括号的外面  也叫做拖尾lambda
    //  val product = items.fold(1) { acc, e -> acc * e }  本来应该在fold函数里面的
    //4 当 Lambda 表达式是函数的唯一参数的时候，函数的括号可以省略
    //  run { println("...") }
    //5 如果 lambda 表达式的参数未使用，那么可以用下划线取代其名称：
    //  map.forEach { _, value -> println("$value!") }

    //this的指向
    // this在函数声明时，指向函数本身，例如foo
    // this在lambda中，指向lambda的拥有者  例如扩展，block:T.()->Unit，block()，lambda中的this指向T
    //  lambda没有指定的拥有者，普通调用时，this指向声明的类对象（顶级函数除外），main函数编译的Companion
    val bool:Boolean = max("") { a, b -> a.length < b.length }


    //    函数 max 是一个高阶函数，它接受一个函数作为第二个参数。 其第二个参数是一个表达式，它本身是一个函数，
//    即函数字面值，它等价于以下具名函数：
    fun compare(a: String, b: String): Boolean = a.length < b.length

    fun max(str:String, f: (String, String) -> Boolean): Boolean {
     return  true
    }


//    闭包
//    Lambda 表达式或者匿名函数（以及局部函数和对象表达式） 可以访问其 闭包 ，即在外部作用域中声明的变量。
//    在 lambda 表达式中可以修改闭包中捕获的变量：
//    var sum = 0
//    ints.filter { it > 0 }.forEach {
//        sum += it
//    }
//    print(sum)

//    函数类型  () -> Unit就是onClick的函数类型
     val onClick: () -> Unit = { }

     //可空的类型
     val onClick1 :(()->Unit)? = null


//    类型别名给函数类型起一个别称
//   typealias ClickHandler = (Button, ClickEvent) -> Unit

//    函数类型实例调用
//    函数类型的值可以通过f.invoke(x) 或者直接 f(x)
   val stringPlus: (String, String) -> String = String::plus
//    println(stringPlus.invoke("<-", "->"))
//    println(stringPlus("Hello, ", "world!"))




//    中缀表示法   中缀函数时可以省略圆点以及圆括号等程序符号，让语句更自然，更接近于使用英语 A to B 这样的语法结构
//    标有 infix 关键字的函数也可以使用中缀表示法（忽略该调用的点与圆括号）调用。中缀函数必须满足以下要求：
//    1 它们必须是成员函数或扩展函数；
//    2 它们必须只有一个参数；
//    3 其参数不得接受可变数量的参数且不能有默认值。
//      中缀函数总是要求指定接收者与参数。当使用中缀表示法在当前接收者上调用方法时，需要显式使用 this；不能像常规方法调用那样省略。
//      这是确保非模糊解析所必需的

    //优先级  https://www.kotlincn.net/docs/reference/functions.html
    // 1 中缀函数调用的优先级低于算术操作符、类型转换以及 rangeTo 操作符
    // 2 中缀函数调用的优先级高于布尔操作符 && 与 ||、is- 与 in- 检测以及其他一些操作符
    infix fun add(s: String) { /*……*/ }
    //调用  a.add("str") 等同于  a add "str"
    fun build() {
        this add "abc"   // 正确  使用中缀表示必须指明接收 add  参数
        add("abc")       // 正确
        //add "abc"        // 错误：必须指定接收者
    }


    //可变参数 vararg
    //编译为Java
//    public final void varArgParamFun(@NotNull int... arr)
    fun varArgParamFun(vararg arr:Int,str:String){
        println("str is $str")
        arr.forEach {
          println("arr $it index ${arr.indexOf(it)}")
        }
    }


}
