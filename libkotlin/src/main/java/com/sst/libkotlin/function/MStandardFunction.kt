package com.sst.libkotlin.function

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class MStandardFunction {
    companion object{
        @JvmStatic
        fun main(args: Array<String>) {
            //https:juejin.cn/post/6942251919662383134
          //标准函数let，also，with，run 和 apply
//            1）、let 函数，必须让某个对象调用，接收一个 Lambda 表达式参数，Lambda 表达式中的参数为当前调用者，且最后一行代码作为返回值
              //根据block(this)，let是T的扩展，this指向泛型T，it指向泛型T
//            public inline fun <T, R> T.let(block: (T) -> R): R {
//                contract {
//                    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
//                }
//                return block(this)
//            }
//            2）、also 函数，必须让某个对象调用，接收一个 Lambda 表达式参数，Lambda 表达式中的参数为当前调用者，无法指定返回值，
        //            这个函数返回的是当前调用对象本身
              //根据block(this)，also是T的扩展，this指向泛型T，it指向泛型T
//            public inline fun <T> T.also(block: (T) -> Unit): T {
//                contract {
//                    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
//                }
//                block(this)
//                return this
//            }
//            3）、with 函数，接收两个参数，第一个为任意类型参数，第二个为 Lambda 表达式参数，Lambda 表达式中拥有第一个参数的上下文 this ，
        //            且最后一行代码作为返回值   //block是T的扩展，this指向泛型T，this的执行域在lambda，lambada的拥有者为T
//            public inline fun <T, R> with(receiver: T, block: T.() -> R): R {
//                contract {
//                    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
//                }
//                return receiver.block()
//            }
//            4）、run 函数，必须让某个对象调用，接收一个 Lambda 表达式参数，Lambda 表达式中拥有当前调用对象的上下文 this ，
        //            且最后一行代码作为返回值   //block是T的扩展，this指向泛型T
//            public inline fun <T, R> T.run(block: T.() -> R): R {
//                contract {
//                    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
//                }
//                return block()
//            }
//            run表达式，this为调用该表达式者的作用域，最后一行为返回值
//            public inline fun <R> run(block: () -> R): R {
//                contract {
//                    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
//                }
//                return block()
//            }
//            5）、apply 函数，必须让某个对象调用，接收一个 Lambda 表达式参数，Lambda 表达式中拥有当前调用对象的上下文 this ，
        //            无法指定返回值，这个函数返回的是当前调用对象本身
              //block是T的扩展，this指向泛型T
//            public inline fun <T> T.apply(block: T.() -> Unit): T {
//                contract {
//                    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
//                }
//                block()
//                return this
//            }
//            注意：在Lambda 表达式中，拥有对象的上下文 this，和拥有该对象是一样的，只不过 this 可省略，而拥有该对象我们可以自定义参数名，
        //            如果不写该参数，默认会有个 it 参数
//            标准函数	函数参数	是否是扩展函数	返回值
//            T.let	    it	      是	         最后一行代码
//            T.also	it	      是	         对象本身
//            with	    this	  否	         最后一行代码
//            T.run	    this	  是	         最后一行代码
//            run       this      否             最后一行代码
//            T.apply	this	  是	         对象本身

            val name = "erdai"
            val age = 20
            //let 返回值为最后一行 String   函数参数it
            var returnValue = StringBuilder().let {
                println("let this ${this.javaClass}")//this class com.sst.libkotlin.function.MStandardFunction$Companion
                it.append(name).append(" ").append(age).toString()
            }
            println(returnValue)

            //also返回对象本身 stringBuilder  函数参数it
            var stringBuilder = StringBuilder().also {
                println("also this ${this.javaClass}") //this this class com.sst.libkotlin.function.MStandardFunction$Companion
                it.append(name).append(" ").append(age)
            }
            println(stringBuilder.toString())

             //with返回最后一行代码 String  函数参数this  StringBuilder
             returnValue = with(StringBuilder()) {
                append(name).append(" ").append(age).toString()
            }
            println(returnValue)

            //run表达式，包含多个语句，用在一些需要表达式的地方，最后一行为返回值
            returnValue = run {
                println("run lambda this ${this.javaClass}") //this class com.sst.libkotlin.function.MStandardFunction$Companion
                StringBuilder().append("").append(age).toString()
            }

            //run扩展返回最后一行代码  String   函数参数this  StringBuilder
            returnValue = StringBuilder().run {
                append(name).append(" ").append(age).toString()
            }
            println(returnValue)

            //apply返回对象本身 StringBuilder  函数参数this  StringBuilder
            stringBuilder = StringBuilder().apply {
                append(name).append(" ").append(age)
            }
            println(stringBuilder.toString())

        }
    }
}