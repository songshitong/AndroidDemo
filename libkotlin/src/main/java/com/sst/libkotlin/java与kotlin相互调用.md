
kotlin引入Java工程
新建activity——》选择kotlin作为语言，kotlin的插件经常变，最好自动生成


java与kotlin大部分情况是可以直接调用


代码转换
Java 代码转 Kotlin 代码
方式有2：
1）、直接将 Java 代码复制到 Kotlin 文件中，AndroidStudio 会出来提示框询问你是否转换
2）、打开要转换的 Java 文件，在导航栏点击 Code -> Convert Java File to Kotlin File
Kotlin 代码转 Java 代码
打开当前需要转换的 Kotlin 文件，在导航栏点击 Tools -> Kotlin ->Show Kotlin Bytecode ，会出来如下界面：



nullable  nonnull
java的API没有声明可空和非空
kotlin不提示空的风险？？？  kotlin没法直接根据函数判断结果是否可空，需要额外的注解
