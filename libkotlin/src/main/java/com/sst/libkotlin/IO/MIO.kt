package com.sst.libkotlin.IO

import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.net.URL

class MIO {
    companion object{
        @JvmStatic
        fun main(args: Array<String>) {
            //打印   System.out.println(message)
            println("1")
            //读取终端 LineReader.readLine(System.`in`, Charset.default
            readLine()
            //获取文件流
            val mFile = File("11")
//            InputStreamReader
            mFile.reader()
            //OutputStreamWriter
            mFile.writer()

            //buffer流
            mFile.bufferedReader()
            mFile.bufferedWriter()

            //读写文件
            mFile.writeText("aa")
            mFile.writeBytes(kotlin.ByteArray(1024))
            mFile.readText()
            mFile.readBytes()
            //追加
            mFile.appendBytes(kotlin.byteArrayOf(1,2,3))
            mFile.appendText("append")

            //读取行
            mFile.useLines{ lines->{}}
            mFile.forEachLine {  }

           //文件遍历  遍历深度1  文件名不能为XX
            //FileTreeWalk的实现
            mFile.walk().maxDepth(1).onEnter { it.name!="XX" }.filter { file->file.isFile  }.forEach {

            }


            //复制
            mFile.copyTo(File("222"))
            //递归复制
            mFile.copyRecursively(File("222"))

            //删除
            mFile.delete()
            //递归删除
            mFile.deleteRecursively()

            //网络读取  通过url自己的openStream获取input流
//            public final InputStream openStream() throws IOException {
//                return this.openConnection().getInputStream();
//            }
            URL("url").readBytes()

            //kotlin对closeable的封装： use  use是Closeable的扩展方法，使用完后自动关闭
            //上面的方法如mFile.writeText已经使用了use函数，可以放心使用，不用担心关闭问题
            val reader = BufferedReader(FileReader(""))
            reader.use {
                reader.lineSequence().sumOf { it.length }
            }
        }
    }
}