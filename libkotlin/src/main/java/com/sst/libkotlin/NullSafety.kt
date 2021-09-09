package com.sst.libkotlin

import java.util.*

class NullSafety {
    companion object{
        @JvmStatic
        fun main(args: Array<String>) {
            ///非空类型
            val a :String="a"
            ///可空类型
            val b :String?=null

            ///安全调用操作符，表示如果若不为null才继续调用
            b?.toLowerCase(Locale.ROOT)
            //与let结合
            b?.let {
                print(it)
            }
        }
    }
}