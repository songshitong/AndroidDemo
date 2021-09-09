
https://blog.csdn.net/carson_ho/article/details/96965702

在Kotlin中，有一些观念是和Java存在较大区别的，一些基本观念需要注意的：

操作对象：在Kotlin中，所有变量的成员方法和属性都是对象，若无返回值则返回Unit对象，大多数情况下Uint可以省略；Kotlin 中没有 new 关键字

数据类型 & 转换：在Java中通过装箱和拆箱在基本数据类型和包装类型之间相互转换；在Kotlin中，而不管是常量还是变量在声明是都必须具有类型注释或者初始化，如果在声明 & 进行初始化时，会自行推导其数据类型。

编译的角度：和Java一样，Kotlin同样基于JVM。区别在于：后者是静态类型语言，意味着所有变量和表达式类型在编译时已确定。

撰写：在Kotlin中，一句代码结束后不用添加分号 “；”；而在Java中，使用分号“;”标志一句代码结束

kotlin 的类型申明置后  变量，方法，类
var a :String ="1"
class A : Object
fun addA():Int

java 一个文件只能有一个类
kotlin 一个文件可以有多个类


val  name = getName() 鸭子编程 ，类型推导