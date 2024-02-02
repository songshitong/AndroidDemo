

https://docs.gradle.org/current/userguide/more_about_tasks.html
新建一个task，将两个module的aar同时输出到build目录
gradle.kts代码
```
tasks.register("assembleAllOfficial"){
  //先执行moudle的打包task
  dependsOn(":moduleA:assembleOfficialRelease")
  dependsOn(":B:assembleOfficialRelease")
  doLast {
    val A = File("./A/build/outputs/aar/A-official-release.aar")
    val B = File("./B/build/outputs/aar/B-official-release.aar")
    val targetDir = File("./build/official")
    if(!targetDir.parentFile.exists()){
      targetDir.parentFile.mkdirs()
    }
    if(!targetDir.exists()){
      targetDir.mkdirs()
    }
    
    //拷贝到目录
    A.copyTo(File("$targetDir/A-official-release.aar").also {
      if(it.exists()){ //已经存在删除
        it.delete()
      }
    })
    B.copyTo(File("$targetDir/B-official-release.aar").also {
      if(it.exists()){
        it.delete()
      }
    })
  }
}
```