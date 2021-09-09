package com.sst.libkotlin.clasz

//委托，委托属性与代理模式
// 代理模式  可以在代理的前后进行数据处理    切面相关，切面可以借助动态代理实现
///静态代理  动态代理   cglib代理 cglib解决JDK只能动态代理接口的缺陷

//by 关键字
//by lazy关键字
//todo 查看其它委托

interface Base {
    fun print()
}

class BaseImpl(val x: Int) : Base {
    override fun print() { print(x) }
}

//通过by 减少了代理模式的样板代码
//Derived的编译代码
//public final class Derived implements Base {
//    // $FF: synthetic field
//    private final Base $$delegate_0;
//
//    public Derived(@NotNull Base b) {
//        Intrinsics.checkNotNullParameter(b, "b");
//        super();
//        this.$$delegate_0 = b;
//    }
//
//    public void print() {
//        this.$$delegate_0.print();
//    }
//}
class Derived(b: Base) : Base by b
class MDelegate {
    fun testBy(){
        val b = BaseImpl(0)
        Derived(b).print()
    }


    //延迟属性(lazy properties)是kotlin标准库中的标准委托之一，可以通过by lazy来实现。其中lazy()是一个函数，可以接受
    //一个Lamba表达式作为参数，第一次调用时会执行Lambda表达式，以后调用该属性会返回之前的结果
    //by lazy的应用场景
    //by lazy
    //返回实例
    //处理 dao  greendao room 单例对象
    //activity  延迟初始化view(findview/createview) 优化性能

    //查看lazy代理的实现
    //public actual fun <T> lazy(initializer: () -> T): Lazy<T> = SynchronizedLazyImpl(initializer)
    //查看SynchronizedLazyImpl
    //@Volatile private var _value  Volatile解决多线程的可见性问题，一个线程写完，另一个线程可以立即读到
    //private val lock = lock ?: this 多线程并发解决，加锁
    ///查看get方法  不等于UNINITIALIZED_VALUE返回对应的值，然后进行初始化
    ///为什么没有set方法，实现延迟初始化的功能，在使用时get方法只初始化一次
//    override val value: T
//        get() {
//            val _v1 = _value
//            if (_v1 !== UNINITIALIZED_VALUE) {
//                @Suppress("UNCHECKED_CAST")
//                return _v1 as T
//            }
//            return synchronized(lock) {
//                val _v2 = _value
//                if (_v2 !== UNINITIALIZED_VALUE) {
//                    @Suppress("UNCHECKED_CAST") (_v2 as T)
//                } else {
//                    val typedValue = initializer!!()
//                    _value = typedValue
//                    initializer = null
//                    typedValue
//                }
//            }
//        }
    val lazyStr:String by lazy {
        print("111")
        "lazyStr"
    }
}