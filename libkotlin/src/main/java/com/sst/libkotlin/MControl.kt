package com.sst.libkotlin

import kotlinx.coroutines.runBlocking

//控制语句
class MControl {
    private val name ="aa"
    private val age =20
    fun mIf(){
        ///kotlin的if本身是一个表达式，存在返回值，最后一行
        val a=1
        val b=2
        val c = if (a > b) 3 else 4 //等同于 Java的 a > b ? 3 : 4  kotlin没有三目运算符
       //if后面跟代码块
        val d= if(a>b){
            1+2
        }else{
            2+2
        }
    }

    //kotlin的when 类似于Java的switch
    //最后一行可以作为返回值，当条件体里面只有一行代码的时候，条件体的 {} 可省略
    //when 条件语句允许传入任意类型的参数
    //java switch
//    1 Switch 语句只能支持一些特定的类型，如整型，短于整型，字符串，枚举类型。如果我们使用的并非这几种类型，Switch 并不可用
//    2 Switch 语句的 case 条件都要在最后加上一个 break
    fun MWhen(){
//        val a = 0
//        when (a) {
//            //当只有一行代码时，条件体的{}可省略
//            0 -> "111"
//            1 -> {
//                1+2
//            }
//            2,3 -> {}
              //最好加上else
//            else -> {}
//        }

        //when 中无参数的情况，Kotlin 中判断字符串或者对象是否相等，直接使用 == 操作符即可
        val mControl = MControl()
        fun getName() = when{
            mControl.name == "aa" -> "name is aa"
            mControl.age == 20 -> "name is 20"
           else -> "name is default"
       }
       //when的case编译为
//        Intrinsics.areEqual(mControl.name, "aa") ? "name is aa" : (mControl.age == 20 ? "name is 20" : "name is default")

    }

    fun MFor(){
        //范围控制
        //in
        for(i in 1..4){
           println(i)
        }
        val paramI = 2
        if(paramI in 1 .. 5){
          println("paramI 在1到5之间")
        }
        //!in
        val arr = intArrayOf(1,2,3)
        if (2 !in arr)
            println("in list")
        //编译为
//        int[] arr = new int[]{1, 2, 3};
//        if (!ArraysKt.contains(arr, 2)) {
//            String var7 = "in list";
//            System.out.println(var7);
//        }

        //until
        for(i in 1 until 2){
        }
        //downTo
        for(i in 5 downTo 1){

        }
        //step 步长
        for(i in 1..5 step  2){

        }

        val array1 = arrayOf(1, 2, 3)
        //array的下标集合indices
        for(i in array1.indices){

        }

        //index和value
        for ((index, value) in array1.withIndex()) {
            println("the element at $index is $value")
        }
    }

    //while语句
    fun MWhile(){
        var i = 5

        while(i in 1..4){
          print(i)
        }

        do{
            print(i)
        }while(i in 1..4)

    }
}