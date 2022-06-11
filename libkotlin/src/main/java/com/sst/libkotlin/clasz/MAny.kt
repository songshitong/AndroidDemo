package com.sst.libkotlin.clasz

class MAny {
//    public open class Any {
//        public open operator fun equals(other: kotlin.Any?): Boolean
//
//        public open fun hashCode(): Int
//        public open fun toString(): String
//    }
    //Any是kotlin的基类，所有类的父类都有一个Any，类似Java的Object
    companion object{
        @JvmStatic
        fun main(args: Array<String>) {
            val any:Any = Any()
            println(any.hashCode())
            //编译为
//            Object any = new Object();
//            int var3 = any.hashCode();
//            System.out.println(var3);
        }
    }
}