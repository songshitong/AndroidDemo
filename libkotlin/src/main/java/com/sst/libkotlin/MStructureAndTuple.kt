package com.sst.libkotlin

class MStructureAndTuple {
    //解构
    companion object{
        @JvmStatic
        fun main(args: Array<String>) {
//            解构是一种从存储在（可能是嵌套的）对象和数组中的数据中提取多个值的便捷方式。它可用于接收数据的位置（例如分配的左侧）。
//            有时将一个对象分解为多个变量是很方便的，例如：
            val developer = Developer("aa","20")
            val (name, age) = developer
            //编译为
//            MStructure.Developer developer = new MStructure.Developer("aa", "20");
//            String name = developer.component1();
//            String age = developer.component2();

//            现在，我们可以独立使用姓名和年龄，如下所示：
            println(name)
            println(age)
//            aa
//            20

            //解构用在for中
            val collection= mutableListOf<Developer>(developer)
            for ((a, b) in collection) {
                println("collection a is $a b is $b")
                //结果 collection a is aa b is 20
            }
            val map = mutableMapOf<String,String>("mm" to "11")
            for ((k,v) in map){
                println("map key value is $k $v")  //map key value is mm 11
            }
            //在 lambda 表达式中解构
            map.mapValues { entry -> "${entry.value}!" }
            map.mapValues { (key, value) -> "$value!" }
//            如果解构的参数中的一个组件未使用，那么可以将其替换为下划线，以避免编造其名称：
            map.mapValues { (_, value) -> "$value!" }
//            你可以指定整个解构的参数的类型或者分别指定特定组件的类型：
            map.mapValues { (_, value): Map.Entry<String, String> -> "$value!" }
            map.mapValues { (_, value: String) -> "$value!" }

            //解构接收函数的返回值
            val (a,b)= getDeveloper()
            println("function result is $a $b")//function result is bb 30


            //元组   省去定义class的过程
//            可以把多个值同时赋给一个变量，或者同时给多个变量赋值。kotlin中元组分为二元元组（Pair）和三元元组（Triple）
//            public data class Pair<out A, out B>(
//                public val first: A,
//                public val second: B
//            ) : Serializable {
            val (p1,p2)=Pair<String,String>("1","2")
            val pa3 = Pair<String,String>("2","3")
            val student = Triple<String,String,Int>("name:11","sex:nan",10)
            println("student ${student.first} ${student.second} ${student.third}")
        }

        fun getDeveloper():Developer{
            return Developer("bb","30")
        }
    }

   data class Developer(val name:String,val age:String)
}