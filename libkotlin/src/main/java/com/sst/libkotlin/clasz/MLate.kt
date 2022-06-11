package com.sst.libkotlin.clasz

//测试延迟初始化
//lateinit 关键字对一个变量延迟初始化
//1、只能作用于 var 属性，且该属性没有自定义 get 和 set 方法
//2、该属性必须是非空类型，且不能是原生类型，无法修饰Int等
//当你对一个变量使用了 lateinit 关键字,Kotlin 编译器就不会在去检查这个变量是否会为空了，此时你要确保它在被调用之前已经初始化了，
// 否则程序运行的时候会报错  可以通过::object.isInitialized判断是否初始化,此检测仅对可词法级访问的属性可用，即声明位于同一个类型内、
// 位于其中一个 外围类型中或者位于相同文件的顶层的属性
//lateinit修饰仍然需要判空，否则报错kotlin.UninitializedPropertyAccessException: lateinit property intA has
// not been initialized
//
//
// by lazy 委托属性  该属性调用的时候才会初始化，且 lazy 后面的 Lambda 表达式只会执行一次
// 实现看委托属性  只能作用于 val 属性，val只初始化一次

//
class MLate {
    val name="late"
    //lateinit被编译为   public MLate varA;
    //get方法进行初始化校验
//    public final MLate getVarA() {
//        MLate var10000 = this.varA;
//        if (var10000 == null) {
//            Intrinsics.throwUninitializedPropertyAccessException("varA");
//        }
//
//        return var10000;
//    }
    lateinit var varA :MLate
    val varB:String by lazy {
        "varB"
    }
    fun funB(){
        if(::varA.isInitialized){
            println(varA.name)
        }
        //编译为
//        if (((MLate)this).varA != null) {
//            MLate var10000 = this.varA;
//            if (var10000 == null) {
//                Intrinsics.throwUninitializedPropertyAccessException("varA");
//            }
//            String var1 = var10000.name;
//            System.out.println(var1);
//        }
    }
}