

sourceSets
更改 Gradle 为源代码集的每个组件收集文件的位置
```
android {
  ...
  sourceSets {
    main {
      java.srcDirs = ['other/java']
      res.srcDirs = ['other/res1', 'other/res2']
      manifest.srcFile 'other/AndroidManifest.xml'
      jniLibs.srcDirs = ['libs']  //默认为jniLibs  不用手动配置
      ...
    }
    androidTest {
      setRoot 'src/tests'
      ...
    }
  }
}
...
```
1 查看可配置的  gradle sync完可以看到所有的task
Tasks > android，然后双击 sourceSets
以main的构建为例  其他的有release，test，debug等...
```
main
----
Compile configuration: compile
build.gradle name: android.sourceSets.main
Java sources: [feature\src\main\java]
Kotlin sources: [feature\src\main\kotlin, feature\src\main\java]
Manifest file: feature\src\main\AndroidManifest.xml
Android resources: [feature\src\main\res]
Assets: [feature\src\main\assets]
AIDL sources: [feature\src\main\aidl]
RenderScript sources: [feature\src\main\rs]
JNI sources: [feature\src\main\jni]
JNI libraries: [feature\src\main\jniLibs]
Java-style resources: [feature\src\main\resources]
```
2 查看gradleapi 以7.2为例
https://developer.android.com/reference/tools/gradle-api/7.2/com/android/build/api/dsl/AndroidSourceSet?hl=zh-cn