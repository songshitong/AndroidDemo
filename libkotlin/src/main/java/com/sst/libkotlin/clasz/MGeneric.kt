package com.sst.libkotlin.clasz

//kotlin的泛型
//todo 书籍<数学与泛型编程>  高效编程的奥秘
//
// 参数化类型 将类型由原来的具体的类型参数化，类似于方法中的变量参数，此时类型也定义成参数形式(可称之为类型参数)，然后在使用/调用时
// 传入具体的类型(类型实参)

// 类型擦除
// java的泛型是伪泛型，这是因为Java在编译期间，所有的泛型信息都会被擦除掉，正确理解泛型概念的首要前提是理解泛型擦除。Java的
//泛型基本上都是在编译器这个层次上实现的，在生成字节码中是不包含泛型中的类型信息的，使用泛型的时候加上类型参数，在编译器编译的时候
//会去掉，这个过程称为类型擦除。如在代码中定义list和list<T>等类型，在编译后都会变成list，JVM看到的只是list，而由泛型附加
//的类型信息对JVM是看不到的
class MGeneric {
}