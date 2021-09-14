package com.sst.libkotlin.IO

import java.io.File
import java.io.InputStreamReader
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
            val fileIo = File("11")
//            InputStreamReader
            fileIo.reader()
            //OutputStreamWriter
            fileIo.writer()

            //buffer流
            fileIo.bufferedReader()
            fileIo.bufferedWriter()

            //读写文件
            fileIo.writeText("aa")
            fileIo.writeBytes(kotlin.ByteArray(1024))
            fileIo.readText()
            fileIo.readBytes()
            //追加
            fileIo.appendBytes(kotlin.byteArrayOf(1,2,3))
            fileIo.appendText("append")

            //读取行
            fileIo.useLines{lines->{}}
            fileIo.forEachLine {  }

           //文件遍历  遍历深度1  文件名不能为XX
            //FileTreeWalk的实现
            fileIo.walk().maxDepth(1).onEnter { it.name!="XX" }.filter { file->file.isFile  }.forEach {

            }


            //复制
            fileIo.copyTo(File("222"))
            //递归复制
            fileIo.copyRecursively(File("222"))

            //删除
            fileIo.delete()
            //递归删除
            fileIo.deleteRecursively()

            //网络读取  通过url自己的openStream获取input流
//            public final InputStream openStream() throws IOException {
//                return this.openConnection().getInputStream();
//            }
            URL("url").readBytes()

        }
    }
}