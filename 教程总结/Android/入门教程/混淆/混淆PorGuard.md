

独立混淆
https://www.jianshu.com/p/a8614ff60647
Android 在构建apk的时候，会把每一个aar中的混淆配置文件读取出来，作为总的混淆配置文件，然后以这个总的混淆配置表对全部的Java代码进行混淆。
因此只要我们的aar中包含了混淆配置文件，那么这个aar在打包到apk的时候，其自带的混淆配置就会对全部的Java代码生效。
因此Android是支持aar独立配置自己混淆文件的，只要aar中包含混淆配置文件即可。
配置方式 android->defaultConfig
consumerProguardFiles 'proguard.pro','proguard-b.pro'    或者consumerProguardFiles fileTree(dir: projectDir, include: 'proguard*')








https://www.guardsquare.com/manual/configuration/usage
https://github.com/Guardsquare/proguard
https://www.jianshu.com/p/d64e493cc07f


1 ProGuard简介
ProGuard is an open source command-line tool that shrinks, optimizes and obfuscates Java code.

四部分内容：
1 shrink（删减）：删减无用代码，包括无用的类、无用的变量、方法等
2 optimize（优化）：优化方法字节码
3 obfuscate（混淆）：混淆现有代码
4 preverify（预校验）：给类添加预校验信息，这是J2ME和Java 6及以上要求的

工作流程
Input jars -> shrink -> optimize -> obfuscate -> preverify -> Output jars
Library jars ->        unchanged                           -> Library jars


1.1 Entry Points
entry points就是程序入口点。ProGuard以entry points作为代码扫描入口，遍历所有代码，并最终决定哪些代码需要被丢弃或混淆。
比较典型的 entry points包括：
main方法
applets
midlets
activities

在ProGuard的不同阶段中，entry points起到不同的作用：
shrinking: 在shrinking阶段以entry points作为起点递归遍历所有代码，不可达的代码会被丢弃
optimization：在optimization阶段会对代码进行进一步优化：
   非入口代码可能会被改为private、static、final
   未被使用参数会被删除
   部分方法可能会被优化为内联方法
obfuscation：obfuscation阶段会将非入口代码进行混淆。被标识为入口的代码则会免于被混淆

2 Keep 选项
keep选项是为了在代码混淆的过程中保留部分类及其字段不被混淆以满足程序运行需求。keep选项一共有如下6种规则：
2.1 keep
keep规则用于标识程序入口，被keep规则修饰的类及其成员会被指定为程序入口，从而免于被混淆。
```
-keep public class com.example.MyMain {
    public static void main(java.lang.String[]);
    
    #保留所有的public方法
    # public *;  
}
```

2.2 keepnames
被keepnames修饰的类及其成员不会被混淆，但前提是对应的成员在shrinking类没有被删减掉。比如保留所有实现Serializable接口的类名：
```
-keepnames class * implements java.io.Serializable 
```

2.3 keepclassmembers
keepclassmembers仅保留指定的类成员不被混淆，但类名会被混淆。接着上面的例子，如果我们不仅想保留所有实现Seriablizable接口的类名，
同时还要保留其所有的接口方法：
```
-keepnames class * implements java.io.Serializable 
-keepclassmembers class * implements java.io.Serializable { 
    static final long serialVersionUID; 
    private static final java.io.ObjectStreamField[] serialPersistentFields; 
    !static !transient <fields>; 
    private void writeObject(java.io.ObjectOutputStream); 
    private void readObject(java.io.ObjectInputStream); 
    java.lang.Object writeReplace(); 
    java.lang.Object readResolve(); 
} 
```

2.4 keepclassmembernames
keepclassmembernames保留指定类成员不被混淆，前提是相关的类成员没有在shrinking阶段被删减。  


2.5 keepclasseswithmembers
keepclasseswithmembers会保留类和类成员不被混淆，前提是对应的类包含所有指定的类成员。keepclasseswithmembers适用于指定
  一批拥有功能类成员的方法，而不用一一列举。比如保留所有又main方法的类：
```
-keepclasseswithmembers public class * { 
    public static void main(java.lang.String[]); 
} 
```

2.6 keepclasswithmembernames
keepclasseswithmembernames保留类和类成员不被混淆,前提是对应的类包含所有指定的类成员,同时对应的类成员在shrinking阶段没有被删减。
比如保留所有native方法：
```
-keepclasseswithmembernames class * { 
    native <methods>; 
} 
```
<methods>指代所有方法名，可以查看下面的类说明

2.7 关系梳理
看完上述几个规则一定有点晕，没有关系，记住下面这个表就是：

Keep	                     From being removed or renamed	   From being renamed(shrinking中删除了就没法使用)
Classes and class members	 -keep	                            -keepnames
Class members only	         -keepclassmembers	                -keepclassmembernames
Classes and class members,
if class members present	 -keepclasseswithmembers	        -keepclasseswithmembernames

如果你不清楚到底该用哪个keep规则，建议直接使用keep,被keep标明的类及其类成员不会被删减或重命名。需要注意的是，如果仅仅指明要keep的类，
 而不指明其类成员：那ProGuard仅会保留其类和无参数构造方法不被删减或重命名。
```
keep class yourpackage.demo
```


3 类说明(Class Specification)
类说明(class specification)是一个用于描述要keep的类及其成员的描述模板，其完整的格式如下所示：
```
[@annotationtype] [[!]public|final|abstract|@ ...] [!]interface|class|enum classname
    [extends|implements [@annotationtype] classname]
[{
    [@annotationtype] [[!]public|private|protected|static|volatile|transient ...] <fields> |
                                                                      (fieldtype fieldname);
    [@annotationtype] [[!]public|private|protected|static|synchronized|native|abstract|strictfp ...] <methods> |
                                                                                           <init>(argumenttype,...) |
                                                                                           classname(argumenttype,...) |
                                                                                           (returntype methodname(argumenttype,...));
    [@annotationtype] [[!]public|private|protected|static ... ] *;
    ...
}]
```
类说明模板有很多符号，理解这些符号的作用很有必要：
[]表示可选项
！表示非
| 表示或，如public|private表示要修饰的对象是public或private


理解了基本的符号含义，再来看这个模板就简单些了，整个表达式分为两部分：
类描述
类成员描述


3.1 类描述
类描述用于限定类本身，其对应的是上面完整表达式的：
```
[@annotationtype] [[!]public|final|abstract|@ ...] [!]interface|class|enum classname
    [extends|implements [@annotationtype] classname]
```
[@annotationtype]用于描述类注解（可选）
[[!]public|final|abstract|@ ...] 用于描述类的访问权限
[!]interface|class|enum用于描述要类的类型：
  class：可以表示任何类或接口
  interface：仅表示接口
  enum：枚举
classname 类名
[extends|implements [@annotationtype] classname]用于描述继承、实现关系，通常用于描述一组类，
   如上文中提到过的所有实现Serializable接口的类：`class * implements java.io.Serializable

这个类描述中所涉及到的两个classname都支持通配符，用以指定一组类，其通配符使用说明如下：

通配符	描述
？	匹配所有的单个字符。比如mypackage.test?可以指代mypackage.test1或mypackage.test2，但不能指代mypackage.test12
*	匹配任意长度的类名，但不包括分隔符.
     比如mypackage.*Test*可以描述mypackage.Test或mypackage.YourTestApplication。
     但无法描述mypackage.mysubpackage.MyTest。通常的用法是，用mypackage.*来描述mypacakge包下的所有类，但不包括其子包中的类
**	匹配任意长度的类名，包括分隔符.
     mypackage.**用于描述mypackage包下的所有类，也包括其子包中的类。



3.2 类成员描述
类成员描述对应于上文中的:
```
[{
    [@annotationtype] [[!]public|private|protected|static|volatile|transient ...] <fields> |
                                                                      (fieldtype fieldname);
    [@annotationtype] [[!]public|private|protected|static|synchronized|native|abstract|strictfp ...] <methods> |
                                                                                           <init>(argumenttype,...) |
                                                                                           classname(argumenttype,...) |
                                                                                           (returntype methodname(argumenttype,...));
    [@annotationtype] [[!]public|private|protected|static ... ] *;
    ...
}]
```
首先，整个类成员描述部分都是可选的，这部分不写也是可以的，比如keep class mypackage.MyTest，
   这种情况下keep或保留类名和类的无参数构造方法不被移除或混淆。

类成员描述的形式大致有三种，也就是类成员描述模板中用;分割开来的三个表达式

3.2.1 类成员变量描述
```
[@annotationtype] [[!]public|private|protected|static|volatile|transient ...] <fields> |
                                                                      (fieldtype fieldname);
```
用于描述类的成员变量
[@annotationtype]、[[!]public|private|protected|static|volatile|transient ...]用于限定变量的注解类型和访问权限，均为可选。
而变量名的描述也有两种方式：
<fields>：指代所有的变量
(fieldtype fieldname):指定具体的某个变量。注意，fieldtype和fieldname必须成对出现

3.2.2 类成员方法描述
```
    [@annotationtype] [[!]public|private|protected|static|synchronized|native|abstract|strictfp ...] <methods> |
                                                                                           <init>(argumenttype,...) |
                                                                                           classname(argumenttype,...) |
                                                                                           (returntype methodname(argumenttype,...));
```
前半部分和类成员变量的描述一致，[@annotationtype]、[[!]public|private|protected|static|...]用于限定方法的注解类型和访问权限。
而方法名的描述有四种方式：
<methods>：指代所有方法
<init>(argumenttype,...)：指代构造方法。<init>指代所有的构造方法，(argumenttype,...)描述方法的参数列表
classname(argumenttype,...):另一种指代构造方法的方式，因为只有构造方法才没有返回类型
(returntype methodname(argumenttype,...))：指代特定成员方法

3.2.3 类成员描述通配符
类成员的描述也支持通配符:

通配符	描述
%	描述任何的原型类型，如：boolean、int、但不包括void
？	描述任意的单个字符
*	匹配任意长度的类名，但不包括分隔符.
**	匹配任意长度的类名，包括分隔符.
***	匹配所有的数据类型，（原型类型或非原型类型，数组或非数组）
...	匹配任意长度的任意类型参数列表


属性配置https://www.guardsquare.com/manual/configuration/attributes
attribute   异常，内部类，泛型，废弃，行表，注解，闭包等
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,
SourceFile,LineNumberTable,*Annotation*,EnclosingMethod
SourceFile class的来源，配置后名称上报到stack traces