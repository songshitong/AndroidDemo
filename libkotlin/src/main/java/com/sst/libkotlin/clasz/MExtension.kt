package com.sst.libkotlin.clasz



//扩展机制
//Kotlin 能够扩展一个类的新功能而无需继承该类或者使用像装饰者这样的设计模式。 这通过叫做 扩展 的特殊声明完成。 例如，
//你可以为一个你不能修改的、来自第三方库中的类编写一个新的函数。 这个新增的函数就像那个原始类本来就有的函数一样，
//可以用普通的方法调用。 这种机制称为 扩展函数 。此外，也有 扩展属性 ， 允许你为一个已经存在的类添加新的属性

//扩展是 Kotlin 的一种语言特性，即：在不修改类 / 不继承类的情况下，向一个类添加新函数或者新属性。扩展使我们可以合理地遵循开闭原则，
// 在大多数情况下是比继承更好的选择  https://juejin.cn/post/6935027613542907941#heading-4

//定义 声明一个扩展函数，我们需要用一个 接收者类型 也就是被扩展的类型来作为他的前缀   Array<out T>.filterTo
//this指向 this关键字在扩展函数内部对应到接收者对象（传过来的在点符号前的对象）
// 一般指向扩展函数调用者对象 array.filter  如果是T.test，this指向T
// 扩展函数默认拥有这个类的上下文环境

//扩展作用域
//当你定义了一个扩展之后，它不会自动在整个项目内生效。在其它包路径下，需要使用improt导入
//import Utils.exchange
//目前测试会自动导入例如，  import com.sst.libkotlin.clasz.sumIsEven

//扩展的限制
//1、扩展函数不能访问 private 或 protected 成员
//扩展函数或扩展属性本质上是定义在类外部的静态方法，因此扩展不可能打破类的封装性而去调用 private 或 protected 成员
//2、不能重写扩展函数
//扩展函数在 Java 中会被编译为静态函数，并不是类的一部分，不具备多态性。尽管你可以给父类和子类都定义一个同名的扩展函数，
//看起来像是方法重写，但实际上两个函数没有任何关系。当这个函数被调用时，具体调用的函数版本取决于变量的 「静态类型」，而不是 「动态类型」。
//java 静态方法调用在编译后生成invokestatic 字节码指令，它的处理逻辑如下：
//  1、编译阶段：确定方法的符号引用，并固化到字节码中方法调用指令的参数中；
//  2、类加载解析阶段：根据符号引用中类名，在对应的类中找到简单名称与描述符相符合的方法，如果找到则将符号引用转换为直接引用；
//    否则，按照继承关系从下往上依次在各个父类中搜索；
//  3、调用阶段：符号引用已经转换为直接引用；调用invokestatic不需要将对象加载到操作数栈，只需要将所需要的参数入栈就可以执行invokestatic指令
//3、如果类的成员函数和扩展函数拥有相同的签名，成员函数优先
//4、扩展属性没有支持字段，不会保存任何状态
//扩展属性是没有状态的，必须定义 getter 访问器。因为不可能给现有的 Java 类添加额外的字段，所以也就没有地方可以存储支持字段
//val MutableList<Int>?.sumIsEven: Boolean = true // (X) Initializer is not allowed here because this property has no backing field
//    get() = if (null == this)
//        false
//    else
//        this.sum() % 2 == 0

//应用场景
//替换Util类
//减少模板代码
//例如将findViewById封装为扩展方法
//fun Int.onClick(click: () -> Unit) {
//    findViewById<View>(this).apply {
//        setOnClickListener {
//            click()
//        }
//    }
//}
//可以直接使用R.id.*来绑定点击事件（R.id* 本质就是一个整数类型
//R.id.btn_login.onClick {
//    // do something
//}
//kotlin-android-extension  可以直接用组件的 id 来操作 View 实例   todo原理？？
//btn_login.setOnClickListener{
//    // do something
//}



class MExtension {

  //查看array filter的实现
//  public inline fun <T, C : MutableCollection<in T>> Array<out T>.filterTo(destination: C, predicate: (T) -> Boolean): C {
//      for (element in this) if (predicate(element)) destination.add(element)
//      return destination
//  }
  fun testExtension(){
      //扩展方法
      val array = arrayOf(1,2,3)
      array.filter { it==1 }
      "aa".strExtension()

      //扩展属性
      val arrayy = mutableListOf<Int>(1,2,3)
      println(arrayy.sumIsEven)

      //可空类型的扩展
      val array1:MutableList<Int>? = null
      array1.exchange(0,1)

      //java中调用扩展方法  kotlin类名.扩展方法()
//      List<Integer> list = new ArrayList();
//      MExtensionKt.exchange(list,0,1);
  }
}

///扩展函数的编译  编译为静态方法   调用MExtensionKt.strExtension("aa");
//public final class MExtensionKt {
//    public static final void strExtension(@NotNull String $this$strExtension) {
//        Intrinsics.checkNotNullParameter($this$strExtension, "$this$strExtension");
//        String var1 = "strExtension";
//        boolean var2 = false;
//        System.out.print(var1);
//    }
//}
//扩展函数的语法结构如下：
//fun ClassName.methodName(params1: Int, params2: Int) : Int{
//
//}
//一般我们要定义哪个类的扩展函数，我们就定义一个同名的 Kotlin 文件，便于后续查找，虽然说也可以定义在任何一个类中，
// 但是更推荐将它定义成顶层方法，这样可以让扩展方法拥有全局的访问域
public fun String.strExtension():Unit{
    print("strExtension")
}

//扩展属性   类名.新增的属性
val MutableList<Int>?.sumIsEven
    get() = if (null == this)
        false
    else
        this.sum() % 2 == 0


//可空接收者类型的扩展函数，需要在内部使用null == this来对接收者对象进行判空
fun <T : Any?> MutableList<T>?.exchange(fromIndex: Int, toIndex: Int) {
   if(null == this) return
   val temp = this[fromIndex]
   this[fromIndex]= this[toIndex]
   this[toIndex] = temp
}