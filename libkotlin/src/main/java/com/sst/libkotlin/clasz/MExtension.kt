package com.sst.libkotlin.clasz

///扩展函数的编译  编译为静态方法   调用MExtensionKt.strExtension("aa");
//public final class MExtensionKt {
//    public static final void strExtension(@NotNull String $this$strExtension) {
//        Intrinsics.checkNotNullParameter($this$strExtension, "$this$strExtension");
//        String var1 = "strExtension";
//        boolean var2 = false;
//        System.out.print(var1);
//    }
//}
public fun String.strExtension():Unit{
    print("strExtension")
}

//扩展机制
//Kotlin 能够扩展一个类的新功能而无需继承该类或者使用像装饰者这样的设计模式。 这通过叫做 扩展 的特殊声明完成。 例如，
//你可以为一个你不能修改的、来自第三方库中的类编写一个新的函数。 这个新增的函数就像那个原始类本来就有的函数一样，
//可以用普通的方法调用。 这种机制称为 扩展函数 。此外，也有 扩展属性 ， 允许你为一个已经存在的类添加新的属性

//定义 声明一个扩展函数，我们需要用一个 接收者类型 也就是被扩展的类型来作为他的前缀   Array<out T>.filterTo
//this指向 this关键字在扩展函数内部对应到接收者对象（传过来的在点符号前的对象）一般指向扩展函数调用者对象 arry.filter
class MExtension {

  //查看array filter的实现
//  public inline fun <T, C : MutableCollection<in T>> Array<out T>.filterTo(destination: C, predicate: (T) -> Boolean): C {
//      for (element in this) if (predicate(element)) destination.add(element)
//      return destination
//  }
  fun testExtension(){
      val array = arrayOf(1,2,3)
      array.filter { it==1 }
      "aa".strExtension()
  }
}