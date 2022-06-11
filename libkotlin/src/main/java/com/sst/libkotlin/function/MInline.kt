package com.sst.libkotlin.function

class MInline {
    companion object{
        @JvmStatic
        fun main(args: Array<String>) {
            //未使用方法内联inline
            val num1 = 10
            val num2 = 20
            val num3 = numberPlus(num1,num2){ n1,n2->
                n1+n2
            }
            println(num3)
            //编译为
//            public static final int numberPlus(int num1, int num2, @NotNull Function2 func) {
//                Intrinsics.checkNotNullParameter(func, "func");
//                int sum = ((Number)func.invoke(num1, num2)).intValue();
//                return sum;
//            }
            // https://juejin.cn/post/6942251919662383134
//            int sum = numberPlus(num1,num2,new Function(){
//                @Override
//                public Integer invoke(Integer num1,Integer num2){
//                    return num1 + num2;
//                }
           //可以看到每调用一次 Lambda 表达式，都会创建一个新的匿名类实例，这样就会造成额外的内存和性能开销

            val num4 = numberPlusInline(num1,num2){n1,n2->
                n1+n2
            }
            println(num4)
//            编译结果
//            int sum$iv = num1 + num2;
//            System.out.println(sum$iv);
            //内联的优化过程
//            1   Kotlin 编译器会把 Lambda 表达式中的代码替换到函数类型参数调用的地方
//            inline fun numberPlus(num1: Int,num2: Int,func: (Int,Int) -> Int): Int{
//                val sum = num1 + num2
//                return sum
//            }
//            val numberPlus = numberPlus(num1, num2)
//             2 Kotlin 编译器会把内联函数中的全部代码替换到函数调用的地方
//            val numberPlus = num1 + num2


            //禁用内联
//           1 如果在内联函数内部，lambda 表达式参数被其它非内联函数调用，会报编译时错误。这是因为 lambda 表达式已经被拉平
        //            而无法传递给其他非内联函数
            //numberPlusNoInline 里面一个内联函数调用了非内联，如果不增加noinline，func会报错，内联后的lambda无法传递给numberPlus
            numberPlusNoInline(1,2) { n1, n2 -> n1 + n2 }


            //非局部返回（Non-local returns）  局部返回和非局部最终体现的是编译的Java代码返回的范围
//             一个不带标签的 return 语句只能用在 fun 声明的函数中使用，因此在 lambda 表达式中的 return 必须带标签，
//             指明需要 return 的是哪一级的函数：
            //使用隐式标签
            foo()
            //自定义标签返回
            foo1()
            //匿名函数代替lambda
            foo2()

            //crossinline
            runRunnable{
                println("crossinlie 测试")
//                return  标记crossinline后，不允许使用return进行非局部返回
                return@runRunnable//可以使用局部返回
            }
        }
    }
}

fun numberPlus(num1: Int,num2: Int,func: (Int,Int) -> Int): Int{
    val sum = func(num1,num2)
    return sum
}

inline fun numberPlusInline(num1: Int,num2: Int,func: (Int,Int) -> Int): Int{
    val sum = func(num1,num2)
    return sum
}

inline fun numberPlusNoInline(num1: Int, num2: Int, noinline func: (Int, Int) -> Int): Int{
    val sum = numberPlus(num1,num2,func)
    return sum
}


fun song(f: (String) -> Unit) {
    // do something
}

fun foo() {
    listOf(1, 2, 3, 4, 5).forEach {
        if (it == 3) return@forEach // 局部返回到该 lambda 表达式的调用者，即 forEach 循环  forEach作为隐式标签
        if(it ==4) return //非局部返回，返回到foo
        print(it)
    }
    print(" done with explicit label")
}
//编译为
//public static final void foo() {
//    Iterable $this$forEach$iv = (Iterable)CollectionsKt.listOf(new Integer[]{1, 2, 3, 4, 5});
//    int $i$f$forEach = false;
//    Iterator var2 = $this$forEach$iv.iterator();
//    while(var2.hasNext()) {
//        Object element$iv = var2.next();
//        int it = ((Number)element$iv).intValue();
//        int var5 = false;
//        if (it != 3) {
//            if (it == 4) {
//                return;
//            }
//            System.out.print(it);
//        }
//    }
//    String var6 = " done with explicit label";
//    System.out.print(var6);
//}

fun foo1() {
    listOf(1, 2, 3, 4, 5).forEach lit@{
        if (it == 3) return@lit // 局部返回到该 lambda 表达式的调用者，即 forEach 循环
        print(it)
    }
    print(" done with explicit label")
}

fun foo2() {
    listOf(1, 2, 3, 4, 5).forEach(fun(value:Int){
        if(value ==2) return  //匿名函数替代 lambda 表达式   局部返回到匿名函数的调用者，即 forEach 循环
        print(value)
    })
    print(" done with explicit label")
}


//crossinline   crossinline最终体现的是Java的语言缺陷？？
//crossinline 关键字保证内联函数的 Lambda 表达式中一定不会使用 return 关键字，但是还是可以使用 return@Method 语法结构进行局部返回，
// 其他方面和内联函数特性一致
//为什么使用crossinline
//1 runnable的Lambda 表达式在编译的时候会被转换成匿名内部类的方式
//2 在匿名类中调用的函数类型参数，此时是不可能进行外层调用函数返回的，最多是在匿名函数中进行返回
inline fun runRunnable(crossinline block: () -> Unit) {
    println("runRunnable start...")
    val runnable = Runnable {
        block()
    }
    runnable.run()
    println("runRunnable end...")
}
//未加crossinline时，block报错，编译为
//public static final void runRunnable(@NotNull final Function0 block) {
//    int $i$f$runRunnable = 0;
//    Intrinsics.checkNotNullParameter(block, "block");
//    String var2 = "runRunnable start...";
//    System.out.println(var2);
//    Runnable runnable = (Runnable)(new Runnable() {
//        public final void run() {
//            block.invoke();
//        }
//    });
//    runnable.run();
//    String var3 = "runRunnable end...";
//    System.out.println(var3);
//}
