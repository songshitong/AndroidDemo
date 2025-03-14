package com.sst.libkotlin

import androidx.annotation.IntDef
import java.io.IOException
import java.lang.reflect.Method
import java.lang.reflect.Parameter
import kotlin.LazyThreadSafetyMode.SYNCHRONIZED
import kotlin.jvm.Throws

//kotlin中的注解
class MAnnotation private constructor(){


    //抛出Java的IOException
    //翻译为
    //String readFile(String name) throws IOException {...}
    @Throws(IOException::class)
    fun getAStr() {
        "11"
    }

    //加锁 等同Java的Synchronized关键字
    @Synchronized
    fun getStr():String{
      //锁函数
      synchronized(this){
        println("in synchronized function ==")
      }
      return "aa"
    }

    ///可见性 效果等同Java的Volatile关键字
    @Volatile
    var strVolatile ="Volatile"
    companion object{
      @JvmStatic //使用JvmStatic注解生成单例
      val instance: MAnnotation by lazy(mode = SYNCHRONIZED) {
        MAnnotation() //声明构造器为私有的
      }

        @JvmStatic
        val a="a"
        //只能用在伴生对象，修饰伴生对象的属性和函数  告诉编译器编译为真正的JVM静态成员(字节码层面处理)
        //如果用在属性上自动生成get/set方法
        @JvmStatic
        fun main(args: Array<String>) {
        }

        //不对该属性生成getter/setter，并将其暴露为Java的属性 java使用a.field，没有该注解是a.getField()
        @JvmField
        val field="f"

        //要在 Java 代码中使用 Kotlin 代码中作为参数传递的默认值，我们需要使用@JvmOverloads注解
        @JvmOverloads
        fun overloads(str:String="22"){

        }
        //生成默认方法
//        @JvmOverloads
//        public final void overloads(@NotNull String str) {
//            Intrinsics.checkNotNullParameter(str, "str");
//        }
        //生成使用默认值的重载方法
//        @JvmOverloads
//        public final void overloads() {
//            overloads$default(this, (String)null, 1, (Object)null);
//        }
//        // $FF: synthetic method
//        public static void overloads$default(MAnnotation.Companion var0, String var1, int var2, Object var3) {
//            if ((var2 & 1) != 0) {
//                var1 = "22";
//            }
//
//            var0.overloads(var1);
//        }


       //自定义注解
        @IntDef(SLOW, NORMAL, FAST)//限定注解的值
        @Retention(AnnotationRetention.SOURCE) //其他类型 BINARY：保存在二进制但是反射不可见；RUNTIME：反射可见
        //限定注解使用的位置  TYPE_PARAMETER参数类型  VALUE_PARAMETER参数名称
        @Target(AnnotationTarget.FIELD,AnnotationTarget.TYPE_PARAMETER,AnnotationTarget.VALUE_PARAMETER)
        @Repeatable//注解可以多次使用
        @MustBeDocumented //注解包含在生成的文档中
        annotation class Speed
        const val SLOW = 0
        const val NORMAL = 1
        const val FAST = 2

        @Speed
        private var speed: Int=SLOW
        public fun setSpeed(@Speed speed: Int) {
            this.speed = speed
        }


      //方法的注解
      private inline fun <reified T:Annotation> parseMethodAnnotation(method: Method): T? {
        //方法的所有注解
        // method.parameterAnnotations 这是参数的注解
        for (annotation in method.annotations){
          if (annotation is T) {
            // if(annotation is CustomMethod){//可以用泛型替代
            //   annotation.name //拿到注解的name
            // }
            return annotation
          }
        }
        return null
      }

      //参数注解
      private inline fun <reified T:Annotation> parseMethodParameter(param: Parameter): T? {
        for (annotation in param.annotations){
          if (annotation is T) {
            return annotation
          }
        }
        return null
      }
    }

  @Retention(AnnotationRetention.RUNTIME)
  annotation class CustomMethod(val name:String)

  class Box<out T>(val value: T)

  interface Base
  class Derived : Base

  //@JvmSuppressWildcards
  //用来注解类和方法，使得被标记元素的泛型参数不会被编译成通配符
  fun unboxBase(box:Box<@JvmSuppressWildcards Base>): Base = box.value
// 会翻译成
// Base unboxBase(Box<Base> box) { …… }

  //不使用注解翻译
  // Base unboxBase(Box<? extends Base> box) { …… }
}