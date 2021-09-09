package com.sst.libkotlin

class MString {
    companion object{

        @JvmStatic
        fun main(args: Array<String>) {
            val a ="aa"
            //模板字符串
            var b = "this is $a  length is ${a.length}"
        }
    }
}