package com.sst.libkotlin.clasz

//测试延迟初始化
//lateinit  可以通过::object.isInitialized判断是否初始化,此检测仅对可词法级访问的属性可用，即声明位于同一个类型内、位于其中一个
// 外围类型中或者位于相同文件的顶层的属性
//lateinit修饰仍然需要判空，否则报错kotlin.UninitializedPropertyAccessException: lateinit property intA has
// not been initialized
//
//
// by lazy 委托属性  该属性调用的时候才会初始化，且 lazy 后面的 Lambda 表达式只会执行一次
// 实现看委托属性
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
        //todo isInitialized的编译实现
        if(::varA.isInitialized){
            println(varA.name)
        }
    }
}