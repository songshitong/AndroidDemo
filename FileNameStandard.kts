import java.io.File
import java.util.*
import kotlin.collections.ArrayList

println("start ====")

val renameList = arrayListOf("教程总结","libJava/src/main/java/sst/example/lib/IO","libJava/src/main/java/sst/example/lib/NIO")
checkStandard(renameList)

fun checkStandard(renameList: List<String>) {
    renameList.forEach {it0->
        val file = File(it0)
        file.walk().forEach { it2 ->
//             println("walk it2 $it2")
            checkFileName(it2)
        }
    }
}

fun checkFileName(file: File) {
    println("checkFileName $file")
    val specialList = arrayListOf(":","：")
    specialList.forEach {
        if (file.name.contains(it)){
            renameFile(file,it)
        }
    }

}


fun renameFile(file: File,specialStr:String) {
    val filePath = file.path
    val newName = filePath.replace(specialStr,"_")
    file.canonicalPath
    println("renameFile $filePath to $newName ")
    file.renameTo(File(newName));
}