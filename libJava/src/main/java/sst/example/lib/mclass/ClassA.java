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

   public static void main(String[] args) {
      //普通代码块
      {
         int a =1;
         int b = ++a;
         System.out.println("b is "+b);
      }
      ClassA classA = new ClassA() {
         @Override ClassB getClassB() {
            return null;
         }
      };
   }

   //https://cloud.tencent.com/developer/article/1423768
   //构造代码块：直接在类中定义且没有加static关键字的代码块称为{}构造代码块。构造代码块在创建对象时被调用，每次创建对象都会被调用，
   // 并且构造代码块的执行次序优先于类构造函数。如果存在多个构造代码块，执行顺序由他们在代码中出现的次序决定，先出现先执行。
   {
     int a =111;
     System.out.println("a is "+a);
   }
}
