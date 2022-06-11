package com.sst.libkotlin

class MString {

    companion object{

        @JvmStatic
        fun main(args: Array<String>) {

            val a ="ab"
            //通过索引访问的字符串中的字符：s [i]
            println("索引1的字符为"+a[1])

            //字符串的for循环迭代
            println("字符串for遍历：")
            for(t in a){
                println(t)
            }
            //编译为
//            int var6 = a.length();
//            for(int var4 = 0; var4 < var6; ++var4) {
//                char t = var5.charAt(var4);
//                System.out.println(t);
//            }

            //三个引号 """拼接多行字符串
            val text = """
            |字符串1
            |字符串2
            """
            //编译为  text = "\n            |字符串1\n            |字符串2\n            ";
            println(text)   // 输出存在一些前置空格
            //trimMargin()删除多余空白  默认删除|前的空格
            println(text.trimMargin())


            //字符串模板
            //即在字符串内通过一些小段代码求值并把结果合并到字符串中
            //模板表达式以美元符（$）开头
            //编译为(new StringBuilder()).append("this is ").append(a).append("  length is ").append(a.length()).toString();
            var b = "this is $a  length is ${a.length}"


        }
    }
}