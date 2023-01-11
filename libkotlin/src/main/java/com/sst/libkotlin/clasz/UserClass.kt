package com.sst.libkotlin.clasz

import kotlin.jvm.internal.Intrinsics

//kotlin常量 编译期常量
//1 定义一个顶层的常量，这个常量不放在任何的类中
const val CONSTANT = "This is a constant"

//编译为
//public final class UserClassKt {
//    public static final String CONSTANT = "This is a constant";
//}
//2 定义一个 object 修饰的单例类，类中定义一个常量
object SingeTon {
    const val CONSTANT = "This is a constant"
}

class KotlinPractice {
    //3 定义一个 companion object 修饰的伴生对象，里面定义一个常量
    companion object {
        const val CONSTANT = "This is a constant"
    }
}

// 主构造器constructor   age是默认参数
//1 主构造函数的特点是没有函数体，直接跟在类名的后面即可，如果需要在主构造函数里面做逻辑，复写 init 函数即可
//2 主构造函数中声明成 val 或者 var 的参数将自动成为该类的字段，如果不加，那么该字段的作用域仅限定在主构造函数中
//默认情况下，val 中构造函数的参数类型。但是您可以将其显式更改为 var
//3 当一个类没有显示的定义主构造函数，但是定义了次构造函数时，那么被继承的类后面不需要加 ()
//若主构造函数无任何注解 / 可见性修饰符，可省略 constructor 关键字

//类默认不可以被继承，使用open表示可以继承  abstract默认可以继承，不用增加open
//    private ：本类内部都可见
//    protected ：本类内部 & 子类中可见
//    public：能见到类声明的任何客户端都可以见（public成员）(默认)
//    internal：能见到类声明的本模块内的任何客户端都可见（public成员）
//    区别于Java，Kotlin的可见修饰符少了default，多了internal：该成员只在相同模块内可见。（注：一个模块 = 编译在一起的一套 Kotlin 文件：
//    一个 IntelliJ IDEA 模块；
//    一个 Maven 项目；
//    一个 Gradle 源集；
//    一次 ＜kotlinc＞ Ant 任务执行所编译的一套文件

open class User1(name: String, age: Int) {} //省略constructor的情况

//声明一个私有构造器
open class User private constructor(userName: String, age: Int = 10) :
    User1(userName, age) {//10为默认参数
//主构造器的代码必须放到init代码块
//init块是在主构造函数执行后立即执行的初始化块。一个类文件可以有一个或多个将串行执行的初始化块。如果你想在主构造函数中执行一些操作，
// 那么在 Kotlin 中是不可能的，为此你需要使用init块
init {
    //...   没有主构造器，init也调用，查看TestClass
    println("this is user init")
}


    //当使用辅助构造函数时，您需要显式调用主构造函数
    //构造器默认公开的
    // 次构造函数1：可通过this调主构造函数
    constructor() : this("hjc")

    // 次构造函数2：可通过this调主构造函数
    constructor(age: Int) : this("hjc") {
        println(age)
    }

    // 次构造函数3：通过this调主构造函数
    constructor(sex: String, age: Int, like: String) : this("hjc") {
        println("$sex$age$like")
    }

    //也可以调用父类构造器,没有主构造器时
    // constructor(userName: String, age: Int = 10,sex: Long) : super(userName, age) {}

    ///variable变量   User().userName = "hjc"  // 设置该属性 = Java的setter方法
    lateinit var userName1: String

    ///value 只读    User().sex  // 使用该属性 = Java的getter方法
    private var sex: String = "男"

    //声明一个属性的完整语法 []代表可选  只读属性不允许 setter   get/set可以限制可见范围
//    var <propertyName>[: <PropertyType>] [= <property_initializer>]
//    [<getter>]
//    [<setter>]
    var sexValue: String = ""
        //对外公开sex
        get() = "sex is $sex"
        protected set(value) {
            //更新sex
            this.sex = value
            //field指向seValue
            field = value
        }
    //编译为对应的get,set方法
//    public final String getSexValue() {
//        return "sex is " + this.sex;
//    }
//    protected final void setSexValue(@NotNull String value) {
//        Intrinsics.checkNotNullParameter(value, "value");
//        this.sex = value;
//    }

    ///方法默认不可以重写，子类重写使用override关键字，并且父类的方法使用open
    open fun signIn() {
    }

    fun signIn2() {
    }

    //**
    // * 嵌套类（内部类）
    // * 标识：关键字inner
    // * 使用：通过外部类的实例调用嵌套类,外部类需要先实例化才能使用   User().User2()
    // */
    //编译为 User里面 public final class User2
    //内部类的继承
    // class ExClass : AbClass() {  //外部继承，内部也继承
    //   private inner class T : AbClass.Test() {
    //
    //   }
    // }
    inner class User2 {

    }

}

///接口  关键字interface声明
interface UserInterface {
    fun getName(): String // 无默认方法体，必须重写
    fun getAge(): Int {    // 有默认方法体，可不重写
        return 22
    }
}

interface UserInterface2 {}

//继承,实现接口使用冒号   继承为单继承，接口为多实现
//为啥 User 后面会有一个括号呢？因为子类的构造函数必须调用父类中的构造函数，在 Java 中，子类的构造函数会隐式的去调用
//一种特殊情况：当一个类没有显示的定义主构造函数，但是定义了次构造函数时，那么被继承的类后面不需要加 ()
//class Student : Person{
//    constructor() : super(){
//
//    }
//}
class UserChild : User(), UserInterface, UserInterface2 {

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
//      1. getter/setter方法；   boolean的isEnable isEnable()和setEnable   boolean的enable对应为getEnable()和setEnable
//              boolean的命名最好不带is
//      2. equals() / hashCode() 对；
//      3. toString() ：输出"类名(参数+参数值)"；
//      4. copy() 函数：复制一个对象&改变它的一些属性，但其余部分保持不变 浅克隆 注意使用copy()后某些场景不能使用list.indexOf，此时引用变了
// 特别注意
// 1. 主构造方法至少要有一个参数，且参数必须标记为val或var
// 2. 数据类不能用open、abstract、sealed(封闭类)、inner标识
data class UserData(var userName: String, var age: Int, var enable:Boolean=true)
//equal方法判断
// if (this !== var1){ //不是同一个对象，对比属性
//   if (var1 is UserData) {
//     val (userName, age) = var1 as UserData
//     if (Intrinsics.areEqual(this.userName, userName) && this.age == age) {
//       return true
//     }
//   }
//   return false
// } else { 同一个对象，地址引用相同
//   return true
// }




//使用copy方法
val userData1: UserData = UserData("li", 20)
val userData2: UserData = userData1.copy(age = 30)

//android中 使用注解@Parcelize并实现接口Parcelable，可以自动生成接口所需方法  需要使用插件plugin id 'kotlin-parcelize'
//注意：：import kotlinx.parcelize.Parcelize
//@Parcelize
//data class TestD(val name: String?):Parcelable {
//}


public class TestClass {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val noConstruct = NoConstructClass("aa", 10)
            `in`("2233")
        }

        //声明关键字的方法，使用``
        fun `in`(name: String) {
            println("this is in function $name")
        }
    }
}

open class Class1 {
    constructor(name: String)
}

class NoConstructClass : Class1 {
    constructor(name: String, age: Int) : super(name)

    init {
        println("this is init method")
    }
}