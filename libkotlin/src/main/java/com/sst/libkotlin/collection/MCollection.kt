package com.sst.libkotlin.collection

import java.util.*

class MCollection {
    companion object{
        @JvmStatic
        fun main(args: Array<String>) {
//            集合的创建和遍历
//            1）、不可变集合：在集合初始化之后，我们不能对其进行增删改操作
//            2）、可变集合：在集合初始化之后，我们还能对其进行增删改操作
//            不可变集合   可变集合
//             listOf    mutableListOf
//             setOf     mutableSetOf
//             mapOf     mutableMapOf


           //list集合
           //不可变list      与Array区分，arrayOf创建数组
           val list1 = listOf("apple","banana","coco")
           //编译为CollectionsKt.listOf(new String[]{"apple", "banana"}); 返回List
           //list1只能读取
           //可变list
           val list2 = mutableListOf("orange","pear")
           //变为CollectionsKt.mutableListOf(new String[]{"orange", "pear"}) 返回MutableList
           list2.add("watermelon")
            println("list2: ")
           for(item in list2){
               print("list2: $item")
            }
          println("=====")
          //处理indexOutOfBound异常
          try {
            val index10000= list1[10000]
          }catch (e:Exception){
            e.printStackTrace()
          }
          //超过后返回null
          println("选择超过length ${list1.getOrNull(10000)}")
          //超过后返回自定义的默认值
          val index1000 = list1.getOrElse(10000){
            "默认值"
          }
          println("选择超过length $index1000")

          //first()和firstOrNull()
          //first()在集合为空的情况下会抛出异常NoSuchElementException
          try {
            val first = arrayListOf<String>().first()
          }catch (e:Exception){
            e.printStackTrace()
          }
          //firstOrNull在集合为空的情况下返回null
          val firstOrNull = arrayListOf<String>().firstOrNull()
          println("firstOrNull : $firstOrNull")

            //Set 集合和 List 集合用法完全一样
           //定义一个不可变 Set 集合
            val set1 = setOf("Apple","Banana","Orange","Pear","Grape")
            //编为Set set1 = SetsKt.setOf(new String[]{"Apple", "Banana", "Orange", "Pear", "Grape"}) 返回set
            //定义一个可变 Set 集合
            val set2 = mutableSetOf("Apple","Banana","Orange","Pear","Grape")
            //编为Set set2 = SetsKt.mutableSetOf(new String[]{"Apple", "Banana", "Orange", "Pear", "Grape"}); 返回MutableSet
            //添加元素
            set2.add("Watermelon")
            println("set2: ")
            for (i in set2) {
                print("$i ")
            }

            //map集合
            //不可变map
            val map1 = mapOf("1" to "1","2" to "2") //生成LinkedHashMap  map接口只能读
//          map1["3"]="3"
            //编为Map map1 = MapsKt.mapOf(new Pair[]{TuplesKt.to("1", "1"), TuplesKt.to("2", "2")}) 返回map

            //可变map
            val map2 = mutableMapOf("1" to "1","2" to "2") //LinkedHashMap MutableMap接口可读可写
            //编为 Map map2 = MapsKt.mutableMapOf(new Pair[]{TuplesKt.to("1", "1"), TuplesKt.to("2", "2")}); 返回MutableMap


            val linkedMap = linkedMapOf("2" to "2","1" to "1")//LinkedHashMap
            val hashMap = hashMapOf("1" to "1")//hashMap

            //双键map  java可以使用guava的table Table<R,C,V>    一个数据需要两个或多个主键/关键字才能确定
            //对于多个键的 可以data class MKey(val key1,val key2..)   val map = mapof<MKey,String>
            val mapScore = mapOf<Pair<String,String>,Int>(("小明" to "数学") to 100, ("小明" to "语文") to 80,("小李" to "英语")to 88)
            println(" ==========")
            println("双键map $mapScore")

            //当前 key 存在则修改元素，不存在则添加元素
            map2["Watermelon"] = ""
            println("map2 ")
            for ((key,value) in map2) {
                print("$key: $value ")
            }
            println("测试不存在的值 map1 3 ${map1["3"]}")
            map1.getValue("1")//如果不存在会抛出异常，返回值不为空所以不存在会抛出异常
            println("")
//            Kotlin 中的 FlatMap 和 Map 有什么区别？
//            FlatMap 用于将列表的所有项目组合成一个列表。
//            Map 用于根据特定条件转换列表
            val containers = listOf(
                listOf("one", "two", "three"),  
                listOf("four", "five", "six"),
                listOf("seven", "eight")
            )
            println("flatmap ${containers.flatMap { it }}")//flatmap [one, two, three, four, five, six, seven, eight]

          //map整合
          val mapPlus = map1.plus(maps)

            //fold和reduce  两个函数都是对集合的遍历，只是遍历完成之后能得到一个结果
//            reduce的返回值类型必须和集合的元素类型相符。
//            fold的返回值类型则不受约束
            val numbers = listOf(1, 1, 1)
            val result = numbers.reduce { a: Int, b: Int -> a + b }
            println("reduceResult=$result")//result=3

            val numbersFold = listOf(1, 1, 1)
            val resultFold = numbersFold.fold(StringBuilder()) {
                    str: StringBuilder, i: Int -> str.append(i).append(" ")
            }
            println("foldResult=$resultFold")

            //runningFold
            val runningFoldList = listOf(1,2,3,4)
            println("runningFold ${runningFoldList.runningFold(-1){a,b -> a+b}}")
            //[-1, 0, 2, 5, 9]  -1,1-1,1+2-1,1+2+3-1,1+2+3+4-1

            //associateWith  将list转为关联的map
            val associateWithList = listOf("one","two")
            println("associateWith ${associateWithList.associateWith { it.uppercase() }}") //{one=ONE, two=TWO}

            //list分割 chunked
            val chunkedList = listOf(1,2,3,4)
            println("chunkedList ${chunkedList.chunked(2)}") //每两个元素一组

            //zip函数 将两个list配对
            val zip1 = listOf(1,2,3)
            val zip2 = listOf("红","绿","蓝")
            println("zip ${zip1.zip(zip2)}") //[(1, 红), (2, 绿), (3, 蓝)]
            //unzip 将pair分割
            val unzipList = listOf("one" to 1, "two" to 2, "three" to 3, "four" to 4)
            println("unzipList ${unzipList.unzip()}")  //([one, two, three, four], [1, 2, 3, 4])

            //函数式API
            var maxLengthFruit = list1.maxByOrNull {
                it.length
            }
            println(maxLengthFruit)
//            函数的由来
            val lambda = {fruit: String -> fruit.length}
           //1 maxBy 函数实际上接收的是一个函数类型的参数，也就是我们这里可以传入一个 Lambda 表达式
            maxLengthFruit = list1.maxByOrNull(lambda)
            //2 替换 lambda
            maxLengthFruit = list1.maxByOrNull({fruit: String -> fruit.length})
            //3 当 Lambda 表达式作为函数的最后一个参数的时候，我们可以把 Lambda 表达式移到函数括号的外面
            maxLengthFruit = list1.maxByOrNull(){fruit: String -> fruit.length}
            //4 当 Lambda 表达式是函数的唯一参数的时候，函数的括号可以省略
            maxLengthFruit = list1.maxByOrNull{fruit: String -> fruit.length}
            //5 当 Lambda 表达式的参数列表中只有一个参数的时候，我们可以把参数给省略，默认会有个 it 参数
            maxLengthFruit = list1.maxByOrNull{ it.length }
             //经过上面 1->2->3->4->5 这几个步骤，我们最终得到了 5 的这种写法


         //  map 操作   把一个元素映射成一个新的元素
           val newList = list1.map { it.uppercase(Locale.getDefault()) }
            println("map操作===")
           for(item in newList){
               print(" $item ")
           }

         //filter 筛选操作，筛选长度小于等于5的字符串
          val filterList = list1.filter { it.length<=5 }
          println("filter操作:")
          for(item in filterList){
              print(" $item ")
          }

          //排序相关
          println("sortedBy: 长度正序 ${
            list1.sortedBy{it->it.length}
          }")
          println("sortedBy: 长度降序 ${
            list1.sortedByDescending{it->it.length}
          }")
          println("sortWith: 长度正序${
            list1.sortedWith(Comparator { a, b -> a.length-b.length })
          }")

          //join  支持多种方式
          println("join ${list1.joinToString(",")}")
        }
    }
}