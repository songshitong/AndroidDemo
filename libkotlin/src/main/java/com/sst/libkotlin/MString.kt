package com.sst.libkotlin

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.withPermit
import java.math.BigInteger
import java.security.MessageDigest
import java.util.Base64
import kotlin.Result.Companion
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MString {

    companion object{

        @JvmStatic
        fun main(args: Array<String>) {
          CoroutineScope(Dispatchers.Default).launch {
             delay(1000)
             suspendCancellableCoroutine<String> {
               it.resume("success")
             }
          }
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

          //base64
          val encoder = Base64.getEncoder()
          val encoded: String = encoder.encodeToString("this is base64".toByteArray())
          println("Encoded Data: $encoded")

          val decoder: Base64.Decoder = Base64.getDecoder()
          val decoded = String(decoder.decode(encoded))
          println("Decoded Data: $decoded")

          //md5
          val md = MessageDigest.getInstance("MD5")
          val mdStr = BigInteger(1, md.digest("this is md5 ".toByteArray())).toString(16).padStart(32, '0').uppercase()
          println("mdstr: $mdStr")
        }
    }
}