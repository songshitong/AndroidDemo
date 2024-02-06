https://juejin.cn/post/6847902216590721031


那么什么是模块系统？官方的定义是A uniquely named, reusable group of related packages, as well as resources 
(such as images and XML files) and a module descriptor

相比于传统的 jar 文件，模块的根目录下多了一个 module-info.class 文件，也即 module descriptor。 module descriptor 包含以下信息：
模块名称
依赖哪些模块
导出模块内的哪些包（允许直接 import 使用）
开放模块内的哪些包（允许通过 Java 反射访问）
提供哪些服务
依赖哪些服务

```
[open] module <module>{
  exports <package> [to <module1>[,<module2>...]];
  opens <package> [to <module1>[,<module2>...]];
  
  provides <interface | abstract class>
    with <class1>[,<class2>...];
    uses <interface | abstract class>;
}
```

