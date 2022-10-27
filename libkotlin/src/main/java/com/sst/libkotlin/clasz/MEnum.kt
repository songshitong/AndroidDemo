package com.sst.libkotlin.clasz

// 为枚举类指定值
//val name: String //获取枚举名称
// val ordinal: Int //获取枚举值在所有枚举数组中定义的顺序
enum class MEnum(val rgb: Int,myName:String) {
    RED(0xFF0000,"red"), GREEN(0x00FF00,"green"), BLUE(0x0000FF,"blue")
}

enum class MENUM1(){
    RED, GREEN, BLUE
}

class Test{
    companion object{
        @JvmStatic
        fun main(args: Array<String>) {
            println("rgb ${MEnum.BLUE.rgb}")
            // MEnum.BLUE.myName  myName不可访问
        }
    }
}