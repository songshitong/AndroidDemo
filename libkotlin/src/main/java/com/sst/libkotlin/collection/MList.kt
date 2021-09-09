package com.sst.libkotlin.collection

class MList {
    companion object{
        @JvmStatic
        fun main(args: Array<String>) {
// 方式1：使用arrayOf创建1个数组：[1,2,3]
            val a = arrayOf(1, 2, 3)

// 方式2：使用工厂函数创建1个数组[0,2,4]
            val b = Array(3, { i -> (i * 2) })
// 工厂函数源码分析
// 参数1 = 数组长度，花括号内是一个初始化值的代码块，给出数组下标 & 初始化值
//            public inline constructor(size: Int, init: (Int) -> T)

// 读取数组内容
            println(a[0])    // 输出结果：1
            println(b[1])    // 输出结果：2

// 特别注意：除了类Array，还有ByteArray, ShortArray, IntArray用来表示各个类型的数组
// 优点：省去了装箱操作，因此效率更高
// 具体使用：同Array
            val x: IntArray = intArrayOf(1, 2, 3)


        }
    }
}