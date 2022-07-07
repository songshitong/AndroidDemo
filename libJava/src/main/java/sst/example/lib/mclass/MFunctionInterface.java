package sst.example.lib.mclass;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class MFunctionInterface {
//    https://mp.weixin.qq.com/s/l3XNXXppfNCyh0rPIgN9OQ
      //更多函数参考https://www.runoob.com/java/java8-functional-interfaces.html
//    Function 函数式接口
//    使用注解@FunctionalInterface标识，并且只包含一个抽象方法的接口是函数式接口。
//    函数式接口主要分为Supplier供给型函数、Consumer消费型函数、Runnable无参无返回型函数和Function有参有返回型函数
//    Supplier供给型函数
//      Supplier的表现形式为不接受参数、只返回数据
//    @FunctionalInterface
//    public interface Supplier<T> {
//        T get();
//    }

//    Consumer消费型函数
//    Consumer消费型函数和Supplier刚好相反。Consumer接收一个参数，没有返回值
//    public interface Consumer<T> {
//        void accept(T var1);
//    }
      //BiConsumer 接受两个参数无返回
    //public interface BiConsumer<T, U> {
    //void accept(T var1, U var2);
    //}

//    Runnable无参无返回型函数
//      Runnable的表现形式为即没有参数也没有返回值
//    @FunctionalInterface
//    public interface Runnable {
//        void run();
//    }
     //Callable 带异常的无参无返回值

//    Function函数的表现形式为接收一个参数，并返回一个值。
//      Supplier、Consumer和Runnable可以看作Function的一种特殊表现形式
//    public interface Function<T, R> {
//        R apply(T var1);
//    }
    //BiFunction<T,U,R>
    //代表了一个接受两个输入参数的方法，并且返回一个结果

    public static void main(String[] args) {
        MFunctionInterface mf = new MFunctionInterface();
        mf.testSupplier();
        mf.testConsumer();
        mf.testFunction();
        mf.testIfFunction();
    }

    void testSupplier(){
        //这里是lambda形式  也可以是()->{}
        Supplier<Integer> a = ()-> 1;
        System.out.println(a.get());
    }

    void testConsumer(){
        System.out.println("testConsumer ==========");
        Consumer<Integer> a = (it)->{
           int b = it++;
           System.out.println("it "+it);
       };

       //先执行andThen，而后执行accept
       a.andThen((it)->{
           System.out.println("andThen "+it);
       }).accept(2);
    }

    void testFunction(){
        System.out.println("testFunction ==========");
        Function<Integer,Integer> addFun= (it)->{
            System.out.println("it "+it);
            return ++it;
        };
        addFun.andThen((it)->{
            System.out.println("andThen after "+it);
            return it;
        }).compose((it)->{
            System.out.println("compose before "+it);
           return (Integer) it;
        }).apply(3);
    }

    void testIfFunction(){
//        if (...){
//            throw new RuntimeException("出现异常了")；
//        }
        //使用函数式优化if抛出异常的分支
        MUtil.isTure(false).throwMessage("模拟异常");
    }

    void testBranchFunction(){
//        if (...){
//            doSomething();
//        } else {
//            doOther();
//        }
        MUtil.isTureOrFalse(true).trueOrFalseHandle(()->{
            System.out.println("this is doSomething");
        },()->{
            System.out.println("this is doOther");
        });
    }

    void testPresentOrElseFunction(){
//        if (str == null || str.length() == 0){
//            doSomeThing();
//        } else {
//            doOther();
//        }
        //System.out::println是一个接收值得函数，可以直接使用
       MUtil.isBlankOrNoBlank("111").presentOrElseHandle(System.out::println,()->{
           System.out.println("doOther");
       });
    }


    // 抛出异常的形式的函数式接口
    @FunctionalInterface
    public interface ThrowExceptionFunction {
        void throwMessage(String message);
    }

    //分支操作的函数式接口
    @FunctionalInterface
    public interface BranchHandle {
        void trueOrFalseHandle(Runnable trueHandle, Runnable falseHandle);

    }

    //空值与非空值分支处理
    public interface PresentOrElseHandler<T extends Object> {
        void presentOrElseHandle(Consumer<? super T> action, Runnable emptyAction);

    }

    static class MUtil{
        //使用函数式抛出异常
        public static ThrowExceptionFunction isTure(boolean b){
            return (errorMessage) -> {
                if (b){
                    throw new RuntimeException(errorMessage);
                }
            };
        }

        //使用函数式进行if，else分支
        public static BranchHandle isTureOrFalse(boolean b){
            return (trueHandle, falseHandle) -> {
                if (b){
                    trueHandle.run();
                } else {
                    falseHandle.run();
                }
            };
        }

        //使用函数式处理空值和非空值
        public static PresentOrElseHandler<?> isBlankOrNoBlank(String str){

            return (consumer, runnable) -> {
                if (str == null || str.length() == 0){
                    runnable.run();
                } else {
                    consumer.accept(str);
                }
            };
        }
    }
}
