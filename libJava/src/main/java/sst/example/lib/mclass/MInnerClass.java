package sst.example.lib.mclass;

//http://c.biancheng.net/view/1022.html
//内部类分为实例内部类，静态内部类，局部内部类，匿名内部类
//实例内部类是指没有用 static 修饰的内部类，有的地方也称为非静态内部类  例如InnerClass
// 在实例内部类中不能定义 static 成员，除非同时使用 final 和 static 修饰

//静态内部类是指使用 static 修饰的内部类

//局部内部类是指在一个方法中定义的内部类LocalInnerClass

//匿名类是指没有类名的内部类，必须在创建时使用 new 语句来声明类
//new <类或接口>() {
//        // 类的主体
//        };
//匿名类有两种实现方式：1 继承一个类，重写其方法  2 实现一个接口（可以是多个），实现其方法
//匿名类中允许使用非静态代码块进行成员初始化操作InnerClass，匿名类的非静态代码块会在父类的构造方法之后被执行
//使用场景，只使用一次，使用匿名类可使代码更加简洁、紧凑，模块化程度更高
//匿名内部类默认持有外部类的引用
public class MInnerClass {

    void anonymousInnerClass(){
        //匿名内部类
        // 继承一个类，重写其方法
        InnerClass ic = new InnerClass(){
            @Override
            void name() {
                super.name();
            }
        };
        ic.name();

        //实现一个接口
        IInnerClassInterface iici = new IInnerClassInterface() {
        };

    }

    void localInnerClass(){
        class LocalInnerClass{
            void name(){}
        }
        LocalInnerClass lic = new LocalInnerClass();
        lic.name();
    }

    public void effectivelyFinal(){
           //effectively final功能
//         Java 中局部内部类和匿名内部类访问的局部变量必须由 final 修饰，以保证内部类和外部类的数据一致性。但从 Java 8 开始，
//         我们可以不加 final 修饰符，由系统默认添加，该变量不可以被重新赋值
//        在 Lambda 表达式中，使用局部变量的时候，也要求该变量必须是 final 的，所以 effectively final 在 Lambda 表达式上下文中非常有用。
//        Lambda 表达式在编程中是经常使用的，而匿名内部类是很少使用的。那么，我们在 Lambda 编程中每一个被使用到的局部变量
//        都去显示定义成 final 吗？显然这不是一个好方法。所以，Java 8 引入了 effectively final 新概念。
         int a=11;
         InnerClass ic = new InnerClass(){
             @Override
             void name() {
                 super.name();
//                 a=12; //再次赋值报错，要求改为array类型，引用这样数组不变，可以改变里面的元素
                 System.out.println(a);
             }
         };
         //class文件的内容为
//        final int a = 11;   //自动添加了final
//        MInnerClass.InnerClass ic = new MInnerClass.InnerClass(){
//            void name() {
//                super.name();
//                System.out.println(a);
//            }
//        };
        //匿名内部类的字节码
//        final int val$a;   //外部的局部变量
//        descriptor: I
//        flags: (0x1010) ACC_FINAL, ACC_SYNTHETIC
//
//        final MInnerClass this$0;  //外部的引用
//        descriptor: LMInnerClass;
//        flags: (0x1010) ACC_FINAL, ACC_SYNTHETIC
    }

   public class InnerClass{
        int i ;
        // 非静态代码块，成员初始化
        {
            i=12;
        }
        void name(){}
    }

    public static class StaticInnerClass{
        void name(){}
    }

   interface IInnerClassInterface{

   }
}
