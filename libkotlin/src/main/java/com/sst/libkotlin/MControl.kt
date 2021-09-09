package com.sst.libkotlin

//控制语句
class MControl {

    fun mIf(){
        ///kotlin的if本身是一个表达式，存在返回值
        val a=1
        val b=2
        val c = if (a > b) 3 else 4 //等同于 Java的 a > b ? 3 : 4  kotlin没有三目运算符

    }

    fun MWhen(){
        val a = 0
        when (a) {
            0 -> {}
            1 -> {}
            2,3 -> {}
            else -> {}
        }
    }

    fun MFor(){
        //范围控制
        //in
        for(i in 1..4){

        }
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
        for(i in array1.indices){

        }
    }

    fun MWhile(){
        var i = 5

        while(i in 1..4){

        }

        do{
            val a=1
        }while(i in 1..4)

    }
}