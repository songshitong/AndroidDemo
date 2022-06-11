
https://blog.csdn.net/carson_ho/article/details/96965702
kotlin特点：
简洁：相对于Java，大大减少代码数量
安全：在编译期就处理了可能会产生空指针的情况，若产生空指针，会编译不通过，从而避免了执行代码时出现空指针异常
互操作性：与Java语言互通(能相互调用)，可使用所有Java写的代码库，在同一项目中可同时使用kotlin和Java混合编程
工具友好：可使用任何Java IDE或命令行构建，例如AndroidStudio

在Kotlin中，有一些观念是和Java存在较大区别的，一些基本观念需要注意的：

操作对象：在Kotlin中，所有变量的成员方法和属性都是对象，若无返回值则返回Unit对象，大多数情况下Uint可以省略；
Kotlin 中没有 new 关键字

数据类型 & 转换：在Java中通过装箱和拆箱在基本数据类型和包装类型之间相互转换；在Kotlin中，而不管是常量还是变量在声明是都必须具有类型注释或者初始化，
   如果在声明 & 进行初始化时，会自行推导其数据类型。

编译的角度：和Java一样，Kotlin同样基于JVM。区别在于：后者是静态类型语言，意味着所有变量和表达式类型在编译时已确定。

撰写：在Kotlin中，一句代码结束后不用添加分号 “；”；而在Java中，使用分号“;”标志一句代码结束


基本类型：
1 数值类型（Numbers）
Kotlin的基本数值类型有六种：Byte、Short、Int、Long、Float、Double
类型    位宽度(Bit)  位宽度(Byte)   示例
Byte    8            1           val byte:Byte = 1
Short   16           2           val short:Short = 1  
Int     32           4           val int:Int = 1
Long    64           8           val long:Long =1
Float   32           4           val float:Float = 0.01F
Double  64           8           val double:Double = 0.001
2 字符类型（Characters）
Kotlin中的字符类型采用 Char 表示，必须使用单引号’ '包含起来使用
val char:Char = 'c'
3 字符串类型（Strings）
val a ="aa"
字符串模板
val b ="a length is ${a.length}"
4 布尔类型（Boolean）
val bool:Boolean =true
5 数组类型（Arrays）
val a = arrayOf(1,2,3)
val b = Array(3,{i->(i*2)})


kotlin 的类型声明置后  变量，方法，类
var a :String ="1"
class A : Object
fun addA():Int

Kotlin 中的解构是什么意思？
解构是一种从存储在（可能是嵌套的）对象和数组中的数据中提取多个值的便捷方式。它可用于接收数据的位置（例如分配的左侧）。
有时将一个对象分解为多个变量是很方便的，例如：
val (name, age) = developer
现在，我们可以独立使用姓名和年龄，如下所示：
println(name)
println(age)


自动类型转换  鸭子编程 ，类型推导    若直接赋值，可不指定其数据类型，则能自动进行类型转换
var a = "aaa"  //a的类型为String

变量与常量
kotlin
val a=1  只读
var b = getName()  可读可写
java
final a =1
String b = getName()

kotlin常量
Kotlin 中定义一个常量需要满足三个条件
1）、使用 const val 来修饰，并初始化
2）、修饰的类型只能是字符串和基础对象类型
3）、只能修饰顶层的常量，object 修饰的成员，companion object 的成员


类相关  https://juejin.cn/post/6942251919662383134
为啥 Kotlin 中要显示的去声明一个非抽象类可继承，而不像 Java 那样定义的类默认可继承？
因为一个类默认可被继承的话，它无法预知子类会如何去实现，因此存在一些未知的风险。类比 val 关键字是同样的道理，在 Java 中，
除非你主动给变量声明 final 关键字，否则这个变量就是可变的，随着项目复杂度增加，多人协作开发，你永远不知道一个可变的变量会在什么时候被谁修改了，
即使它原本不应该修改，也很难去排查问题。因此 Kotlin 这样的设计是为了让程序更加的健壮，也更符合高质量编码的规范

Effective Java 这本书中提到：如果一个类不是专门为继承而设计的，那么就应该主动将它加上 final 声明，禁止他可以被继承

创建类
kotlin  val user:User = User()  //kotlin不使用new关键字
java    User user = new User();   

java 一个文件只能有一个类
kotlin 一个文件可以有多个类


Kotlin 中的初始化块是什么？
init块是在主构造函数执行后立即执行的初始化块。一个类文件可以有一个或多个将串行执行的初始化块。如果你想在主构造函数中执行一些操作，
那么在 Kotlin 中是不可能的，为此你需要使用init块。

https://blog.csdn.net/qxf865618770/article/details/123059463
Kotlin 中的构造函数有哪些类型？
主构造函数：这些构造函数是在类头中定义的，你不能在其中执行一些操作，这与 Java 的构造函数不同。
辅助构造函数：这些构造函数是在类体内使用构造函数关键字声明的。您必须从辅助构造函数显式调用主构造函数。此外，
不能在辅助构造函数中声明类的属性。Kotlin 中可以有多个二级构造函数。

主构造函数和次构造函数之间有什么关系吗？
当使用辅助构造函数时，您需要显式调用主构造函数

构造函数中使用的默认参数类型是什么？
默认情况下，val 中构造函数的参数类型。但是您可以将其显式更改为 var


Kotlin 中的中缀函数是什么？
中缀函数用于在不使用任何括号或括号的情况下调用该函数。您需要使用中缀关键字才能使用中缀功能
```
class Operations {
    var x = 10; 
    infix fun minus(num: Int) {
    this.x = this.x - num
     } 
}
fun main() {
    val opr = Operations()
    opr minus 8
    print(opr.x)
}
```

关键字
代理
//by 关键字   委托分为类委托和属性委托
// 类委托 把一个类的具体实现委托给另外一个类，使用 by 关键字进行委托
//属性委托 将一个属性的具体实现委托给另一个类去完成
// 1 val/var <属性名>: <类型> by <表达式>
// 2 延迟属性委托  by lazy
// 3 可观察属性委托 by Delegates.observable()
// 4 把多个属性储存在一个映射（map）中，而不是每个存在单独的字段中  by map  属性名是map的key，属性的值是map的value
// 5 ReadOnlyProperty / ReadWriteProperty    by object : ReadOnlyProperty/ReadWriteProperty


Kotlin 中的内联函数是什么？
内联函数指示编译器在代码中使用该函数的任何位置插入完整的函数体。要使用 Inline 函数，您只需在函数声明的开头添加一个 inline 关键字即可
为什么内联?
每调用一次 Lambda 表达式，都会创建一个新的匿名类实例，这样就会造成额外的内存和性能开销

Kotlin 中的 noinline 是什么？
在使用内联函数并希望传递一些 lambda 函数而不是所有 lambda 函数作为内联函数时，您可以明确告诉编译器它不应该内联哪个 lambda

Kotlin 中的具体化类型是什么？
当您使用泛型的概念将某个类作为参数传递给某个函数并且您需要访问该类的类型时，您需要使用 Kotlin 中的 reified 关键字
```
inline fun < T> genericsExample(value: T) {
 println(value)
 println("Type of T: ${T::class.java}")
}
fun main() {
 genericsExample<String>("Learning Generics!")
 genericsExample<Int>(100)
}
```

泛型相关
//泛型协变，逆变和不变
//1 泛型协变的语法规则：<out T> 类似于 Java 的 <? extends Bound>，它限定的类型是当前上边界类或者其子类，
// 如果是接口的话就是当前上边界接口或者实现类，协变的泛型变量只读，不可以写，可以添加 null ，但是没意义 ,例如SimpleDataOut

//2 泛型逆变的语法规则：<in T> 类似于 Java 的 <? super Bound>，它限定的类型是当前下边界类或者其父类，
// 如果是接口的话就是当前下边界接口或者其父接口，逆变的泛型变量只能写，不建议读，例如SimpleDataIn

//3 泛型不变 默认的泛型T是不变的

//4 Kotlin 使用 <*> 这种语法结构来表示无界通配符，它等价于 <out Any>，类似于 Java 中的 <?>，
//  在定义一个类的时候你如果使用<out T : Number> ，那么 * 就相当于 <out Number>

逆变与协变用来描述类型转换（type transformation）后的继承关系，其定义：如果A、B表示类型，f(⋅)表示类型转换，
≤表示继承关系（比如，A≤B表示A是由B派生出来的子类）；
f(⋅)是逆变（contravariant）的，当A≤B时有f(B)≤f(A)成立；
f(⋅)是协变（covariant）的，当A≤B时有f(A)≤f(B)成立；
f(⋅)是不变（invariant）的，当A≤B时上述两个式子均不成立，即f(A)与f(B)相互之间没有继承关系。


//kotlin的PECS规则  Producer-Extends, Consumer-Super
//1 如果只读用out
//2 如果只写用in
//3 既读又写使用泛型T




https://juejin.cn/post/6963190541471186957#heading-12
协程作用域的作用
协程必须在协程作用域中才能启动，协程作用域中定义了一些父子协程的规则，Kotlin 协程通过协程作用域来管控域中的所有协程
协程作用域间可并列或包含，组成一个树状结构，这就是 Kotlin 协程中的结构化并发，规则如下：
作用域细分：
有下述三种：
1）、顶级作用域：没有父协程的协程所在的作用域
2）、协同作用域：协程中启动新协程(即子协程)，此时子协程所在的作用域默认为协同作用域，子协程抛出的未捕获异常都将传递给父协程处理，
   父协程同时也会被取消；
3）、主从作用域：与协同作用域父子关系一致，区别在于子协程出现未捕获异常时不会向上传递给父协程

父子协程间的规则
1）、父协程如果取消或结束了，那么它下面的所有子协程均被取消或结束
2）、父协程需等待子协程执行完毕后才会最终进入完成状态，而不管父协程本身的代码块是否已执行完
3）、子协程会继承父协程上下文中的元素，如果自身有相同 Key 的成员，则覆盖对应 Key，覆盖效果仅在自身范围内有效

coroutineScope函数
coroutineScope 函数会继承外部的协程作用域并创建一个子作用域
coroutineScope 函数也是一个挂起函数，因此我们可以在任何其他挂起函数中调用

launch函数
使用 launch 函数在当前的协程作用域下创建子协程    
launch 函数是 CoroutineScope 的一个扩展函数，因此只要拥有协程作用域，就可以调用 launch 函数
只能工作在协程作用域

Delay 函数
delay 函数是一个非阻塞式挂起函数，它可以让当前协程延迟到指定的时间执行，且只能在协程的作用域或者其他挂起函数中调用
对比 Thread.sleep() 函数，delay 函数只会挂起当前协程，并不会影响其他协程的运行，而 Thread.sleep() 函数会阻塞当前线程，
  那么该线程下的所有协程都会被阻塞

runBlocking函数
使用 runBlocking 函数创建一个能阻塞当前线程的协程作用域，可以保证在协程作用域内的所有代码和子协程没有全部执行完之前一直阻塞当前线程

suspend关键字
suspend 关键字将一个函数声明成挂起函数,挂起函数是一个可以启动、暂停和恢复的函数  
挂起函数必须在协程或者另一个挂起函数里被调用


async函数
launch 函数只能用于执行一段逻辑，却不能获取执行的结果，因为它的返回值永远是一个 Job 对象  //发射：一劳永逸  async：执行任务并返回结果
async 函数必须在协程作用域下才能调用
async 函数会创建一个子协程并返回一个 Deferred 对象，如果需要获取 async 函数代码块中的执行结果，
   只需要调用 Deferred 对象的 await() 方法即可
async 函数在调用后会立刻执行，当调用 await() 方法时，如果代码块中的代码还没执行完，那么 await() 方法会将当前协程阻塞住，
   直到可以获取 async 函数中的执行结果

withContext函数
withContext 函数是一个挂起函数，并且强制要求我们指定一个协程上下文参数，这个调度器其实就是指定协程具体的运行线程
withContext 函数在调用后会立刻执行，它可以保证其作用域内的所有代码和子协程在全部执行完之前，一直阻塞当前协程
withContext 函数会创建一个子协程并将最后一行的执行结果作为返回值


