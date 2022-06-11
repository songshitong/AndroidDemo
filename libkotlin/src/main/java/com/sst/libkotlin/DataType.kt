package com.sst.libkotlin

//基本类型
class DataType {
    companion object{
        @JvmStatic
        fun main(args: Array<String>) {
            //数值类型（Numbers） 父类是Numbers
            //数值类型可以相互转换
//            public abstract class Number {
//                public abstract fun toDouble(): Double
//                public abstract fun toFloat(): Float
//                public abstract fun toLong(): Long
//                public abstract fun toInt(): Int
//                public abstract fun toChar(): Char
//                public abstract fun toShort(): Short
//                public abstract fun toByte(): Byte
//            }
            val byte:Byte = 1
            println("Byte:max ${Byte.MAX_VALUE} min ${Byte.MIN_VALUE} bit ${Byte.SIZE_BITS} byte ${Byte.SIZE_BYTES}")

            val short:Short = 1
            println("Short:max ${Short.MAX_VALUE} min ${Short.MIN_VALUE} bit ${Short.SIZE_BITS} byte ${Short.SIZE_BYTES}")

            val int:Int = 1
            println("Int:max ${Int.MAX_VALUE} min ${Int.MIN_VALUE} bit ${Int.SIZE_BITS} byte ${Int.SIZE_BYTES}")

            val long:Long =1
            println("Long:max ${Long.MAX_VALUE} min ${Long.MIN_VALUE} bit ${Long.SIZE_BITS} byte ${Long.SIZE_BYTES}")

            val float:Float = 0.010000001F
            println("Float:max ${Float.MAX_VALUE} min ${Float.MIN_VALUE} bit ${Float.SIZE_BITS} byte ${Float.SIZE_BYTES}")

            val double:Double = 0.00000000001
            println("Double:max ${Double.MAX_VALUE} min ${Double.MIN_VALUE} bit ${Double.SIZE_BITS} byte ${Double.SIZE_BYTES}")

            //区别于Java，在Kotlin中字符（char）不属于数值类型，是一个独立的数据类型
            //Kotlin中的字符类型采用 Char 表示，必须使用单引号’ '包含起来使用
            val char:Char = 'c'
            println("char 相关----")
            println(char+1)
            //char转为其他类型
            println(char.code.toInt())
            println(char.code.toByte())
            println(char.code.toString())
            println("char 相关----")


            //布尔类型
            // Kotlin的Boolean类似于Java的boolean类型，其值只有true 、false
            val bool:Boolean = true
            //Boolean内置的函数逻辑运算  ||,&&,!
            println("Boolean.not: ${bool.not()}")
            println("Boolean.and: ${bool.and(false)}") //编译为bool & false
            println("Boolean.or: ${bool.or(false)}")  //编译为bool | false


            //数组类型（Arrays）
            //1 使用arrayOf创建  类Array
            val a = arrayOf(1,2,3)
            //类array支持泛型
            val iaa:Array<Int> = arrayOf(1,2,3)
            //编译为  Integer[] var10000 = new Integer[]{1, 2, 3};
            //2 使用工厂函数创建
            val b = Array(3,{i->(i*2)})

            //工厂函数public inline constructor(size: Int, init: (Int) -> T)
            //参数1 = 数组长度，花括号内是一个初始化值的代码块，给出数组下标 & 初始化值
            //编译为
//            byte var14 = 3;
//            Integer[] var15 = new Integer[var14];
//            for(int var16 = 0; var16 < var14; ++var16) {
//                int var18 = false;
//                Integer var21 = var16 * 2;
//                var15[var16] = var21;
//            }
            println("数组读取----")
            //[] 重载了 get 和 set 方法，可通过下标获取 / 设置数组值
            println(a[1])
            println(a.get(1))
            println("数组设置----")
            a[1]=6
            a.set(1,6)
            println("数组长度：${a.size}")
            //除了类Array，还有ByteArray, ShortArray, IntArray,CharArray,LongArray,FloatArray,DoubleArray,BooleanArray
            // 用来表示各个类型的数组
           // 优点：省去了装箱操作，因此效率更高
            val intArray:IntArray = intArrayOf(1,2)
            //编译为int[] var10000 = new int[]{1, 2};  数组为int数组
            val ia:Array<Int> = arrayOf(1,2,3)
            //编译为Integer[] var34 = new Integer[]{1, 2, 3}; 数组为Integer对象数组

            var a1 = arrayOf(1,2,3)
            val a2 = arrayOf(1,2,3,5)
            a1 = a2
            println(a1)
        }
    }
}