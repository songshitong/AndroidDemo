package com.sst.libkotlin.clasz

//函数式接口  sam接口   single abstract method
//只有一个抽象方法的接口称为函数式接口或 SAM（单一抽象方法）接口。函数式接口可以有多个非抽象成员，但只能有一个抽象成员。
fun interface KRunnable {
    fun run()
}

class MSAM {
    companion object{
        @JvmStatic
        fun main(args: Array<String>) {
            //SAM 转换
//            使用 lambda 表达式可以替代手动创建实现函数式接口的类。 通过 SAM 转换， Kotlin 可以将其签名与接口的单个抽象方法
//            的签名匹配的任何 lambda 表达式转换为实现该接口的类的实例

//            不使用 SAM 转换
            val run = object : KRunnable{
                override fun run() {
                    print("invoke")
                }
            }
            //使用sam转换  可以自动匹配抽象方法run
            val runSam = KRunnable { print("sam invoke") }


            //调用Java接口  接口中只有一个待实现的方法
            //java 中的匿名内部类
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//
//                }
//            }).start();
            Thread(object : Runnable{
                override fun run() {

                }
            }).start()
            //精简1   因为 Runnable 类中只有一个待实现方法，即使这里没有显示的重写 run() 方法，
            // * Kotlin 也能明白后面的 Lambda 表达式就是要在 run() 方法中实现的内容
            Thread(Runnable{

            }).start()
            //2 //因为是单抽象方法接口，我们可以将接口名进行省略
            Thread({

            }).start()
            //3 //当 Lambda 表达式作为函数的最后一个参数的时候，我们可以把 Lambda 表达式移到函数括号的外面
            Thread(){

            }.start()
            //4 当 Lambda 表达式是函数的唯一参数的时候，函数的括号可以省略
            Thread{ }.start()
        }
    }
}