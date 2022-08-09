package sst.example.lib.mclass;

public abstract class ClassA {
   abstract  ClassB getClassB();
   // TODO: 2019-06-27 面向过程、面向对象、面向函数区别 、面向接口

   //静态代码块的使用  将多个变量声明为一堆，减少遗漏的可能
   //缺点：这样声明Java中不是constant value，部分场景例如注解中无法使用
   public static final String BASE_URL ;
   public static final String PLATFORM_BASE_URL ;
   public static final String APPNO_VALUE ;
   //dev
   static {
      BASE_URL = "http://...";
      PLATFORM_BASE_URL = "http://...";
      APPNO_VALUE = "1232";
   }
   //测试环境
   static {
      //BASE_URL = "..";
      //PLATFORM_BASE_URL = "...";
      //APPNO_VALUE = "12321";
   }
}
