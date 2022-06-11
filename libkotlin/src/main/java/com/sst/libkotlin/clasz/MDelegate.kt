package com.sst.libkotlin.clasz

import kotlin.properties.Delegates
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

//委托，委托属性与代理模式
// 代理模式  可以在代理的前后进行数据处理    切面相关，切面可以借助动态代理实现
///静态代理  动态代理   cglib代理 cglib解决JDK只能动态代理接口的缺陷

//by 关键字   委托分为类委托和属性委托
// 类委托 把一个类的具体实现委托给另外一个类，使用 by 关键字进行委托
//属性委托 将一个属性的具体实现委托给另一个类去完成
// 1 val/var <属性名>: <类型> by <表达式>
// 2 延迟属性委托  by lazy
// 3 可观察属性委托 by Delegates.observable()
// 4 把多个属性储存在一个映射（map）中，而不是每个存在单独的字段中  by map  属性名是map的key，属性的值是map的value
// 5 ReadOnlyProperty / ReadWriteProperty    by object : ReadOnlyProperty/ReadWriteProperty
//    对于 val 变量使用 ReadOnlyProperty，而 var 变量实现ReadWriteProperty，使用这两个接口可以方便地让 IDE 帮你生成函数签名

//todo 委托的应用 https://juejin.cn/post/6958346113552220173#heading-13
interface Base {
    fun print()
}

class BaseImpl(val x: Int) : Base {
    override fun print() { print(x) }
}

//类委托
//通过by 减少了代理模式的样板代码    Derived的Base方法实现委托给了b
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
//Derived   [dɪˈraɪvd]  获得;取得;得到;(使)起源;(使)产生
class Derived(b: Base) : Base by b
class MDelegate {
    fun testBy(){
        val b = BaseImpl(0)
        Derived(b).print()
    }

    //属性委托
    //将 p 属性的具体实现委托给了 Delegate 去完成
    //Delegate 需要实现getValue,setValue  val只重写getValue即可 var需要getValue和setValue
    // getValue  thisRef表示在什么类中使用，KProperty代表属性p相关信息，例如KProperty.name
    // setValue 相比getValue 增加一个要给属性p的赋值，类型与属性P的类型，getValue的返回值相同
    var p: String by Delegate()


    //延迟属性(lazy properties)是kotlin标准库中的标准委托之一，可以通过by lazy来实现。其中lazy()是一个函数，可以接受
    //一个Lambda表达式作为参数，第一次调用时会执行Lambda表达式，以后调用该属性会返回之前的结果
    //by lazy的应用场景
    //by lazy
    //返回实例
    //处理 dao  greendao room 单例对象
    //activity  延迟初始化view(findview/createview) 优化性能

    //todo 查看lazy的重载函数  根据线程mode执行不同的方法
    //查看lazy代理的实现  LazyJVM.kt
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
                  //加锁后再次校验
//                if (_v2 !== UNINITIALIZED_VALUE) {
//                    @Suppress("UNCHECKED_CAST") (_v2 as T)
//                } else {
                      //初始化  initializer就是by lazy传入的lambda
//                    val typedValue = initializer!!()
//                    _value = typedValue
//                    initializer = null
//                    typedValue
//                }
//            }
//        }
    val lazyStr:String by lazy {
        println("lazyStr init") //只执行一遍
        //最后一行为返回值，每次返回相同    与相同：return@lazy "lazyStr"
         "lazyStr"
    }

    //todo 实现
    var name: String by Delegates.observable("这是初始值") { prop, old, new ->
        println("旧值：$old -> 新值：$new")
    }

    val read by object : ReadOnlyProperty<Any?,String>{
        override fun getValue(thisRef: Any?, property: KProperty<*>): String {
           return "this is read"
        }
    }

    var readWrite by object : ReadWriteProperty<Any?,String>{
        override fun getValue(thisRef: Any?, property: KProperty<*>): String {
            return "this is readWrite"
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
            println("setValue: $value")
        }

    }

    companion object{
        @JvmStatic
        fun main(args: Array<String>) {
            val m =MDelegate()
            //by 属性委托
            println("属性委托===")
            println(m.p)

            //by lazy
            println(m.lazyStr)
            println(m.lazyStr)

            //by Delegates.observable
            println("Delegates.observable ")
            println("init ${m.name}")
            m.name ="第一次改变值"

            //by map
            val map = mutableMapOf("name" to "tom","age" to 20)
            val user = UserByMap(map)
            println("by map====")
            println(user.name)
            println(user.age)
            map["age"]=30
            println("new age ${user.age}")

            //by ReadProperty
            println("by ReadProperty")
            println("read: ${m.read}")
            println("by ReadWriteProperty")
            println("readWrite: ${m.readWrite}")
            m.readWrite="new readWrite"
        }
    }
}

//by map   将name和age映射到map的key,value
class UserByMap(private val map: Map<String, Any?>) {
    val name: String by map
    val age: Int     by map
}


class Delegate {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        return "$thisRef, thank you for delegating '${property.name}' to me!"
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
        println("$value has been assigned to '${property.name}' in $thisRef.")
    }
}