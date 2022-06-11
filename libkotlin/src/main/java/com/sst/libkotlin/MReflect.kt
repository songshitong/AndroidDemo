package com.sst.libkotlin

import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.*
import kotlin.reflect.jvm.isAccessible

fun topFun(){

}
//kotlin的反射要单独依赖，减小内存使用
//kotlin版本和使用的版本一致
//implementation "org.jetbrains.kotlin:kotlin-reflect:1.5.21"
class MReflect {
    //私有成员变量
    private val privateInt = 10
    //成员变量
    val norInt = 10
    //可变属性
    var age =11

    private fun pFun(param:String){
        println("this is pFun param is $param")
    }

    companion object{
        @JvmStatic
        fun main(args: Array<String>) {
            //https://juejin.cn/post/7056936075339497503#heading-2
            //KClass  类似Java的Class
//            表示一个类具有内省功能，然后该类的实例可以通过 ::class 语法获取
            //内省就是通过KClass能获取这个类的信息

            val mReflectClass:KClass<MReflect> = MReflect::class
            //使用反射创建对象
            val mReflect:MReflect = mReflectClass.createInstance()
            println("createInstance mReflect $mReflect")
            //转为Java Class<T>
            val javaC = mReflectClass.java
            //java class转为KClass
            val kC = MReflect.javaClass.kotlin
            println("typeParameters has ===")
            //成员属性
            mReflectClass.memberProperties.forEach {
                println(it)
            }
            //memberProperties是KClass的扩展
            // 结果对所有非static成员进行了过滤，要求不是扩展并且是KProperty1才可以
            //val <T : Any> KClass<T>.memberProperties: Collection<KProperty1<T, *>>
            //    get() = (this as KClassImpl<T>).data().allNonStaticMembers.filter { it.isNotExtension && it is KProperty1<*, *> } as Collection<KProperty1<T, *>>


//            KCallable
            //KCallable表示一个可调用的实体，比如函数或者属性,它的子接口有KFunction，KProperty
           val pFun = mReflectClass.declaredFunctions.filter {
                it.name =="pFun"
            }
           //由于pFun是私有的，需要设置isAccessible为true
           pFun.first().isAccessible=true
           pFun.first().call(mReflect,"hhhh")

           //通过::获取KCallable
           val norInt:KCallable<Int> = MReflect::norInt
           println("::获取norInt ${norInt.call(mReflect)}")
           //使用::获取方法
           val pfun: (MReflect, String) -> Unit = MReflect::pFun
           //获取顶级函数
           val tFun:()->Unit = ::topFun


           //KMutableProperty修改属性
           val ageP = MReflect::age
           println("ageP is ${ageP.get(mReflect)}")
           ageP.set(mReflect,111)
           println("ageP is ${ageP.get(mReflect)}")
        }
    }
}

//KClass常用方法
//一共实现了3个接口，接口
//public actual interface KClass<T : Any> : KDeclarationContainer, KAnnotatedElement, KClassifier {
//    //简单名字
//    public actual val simpleName: String?
//    //类的全名
//    public actual val qualifiedName: String?
//    //类以及父类定义的所有属性和方法
//    //其中类型是KCallable
//    override val members: Collection<KCallable<*>>
//    //所有构造函数
//    //类型KFunction
//    public val constructors: Collection<KFunction<T>>
//    //内部定义的所有类，包括内部类和静态嵌套类
//    public val nestedClasses: Collection<KClass<*>>
//    //该类的类型参数，即泛型类的类型参数
//    public val typeParameters: List<KTypeParameter>
//    //该类直接的父类类型列表
//    public val supertypes: List<KType>
//    //假如这个类是密封类，获取其所有子类
//    public val sealedSubclasses: List<KClass<out T>>
//    //该类的可见修饰符，也就是PUBLIC PROTECT等4种情况
//    public val visibility: KVisibility?
//    //是否是final，Kotlin的类默认就是final，无法继承
//    public val isFinal: Boolean
//    //是否是Open，和isFinal反过来
//    public val isOpen: Boolean
//    //是否是抽象的类
//    public val isAbstract: Boolean
//    //是否是密封类
//    public val isSealed: Boolean
//    //是否是数据类
//    public val isData: Boolean
//    //是否是内部类
//    public val isInner: Boolean
//    //是否是伴生对象
//    public val isCompanion: Boolean
//    //类是否是一个Kotlin函数接口
//    public val isFun: Boolean
//    //是否是value class，这个是1.5才推出的新内容
//    public val isValue: Boolean
//}


//KClass的扩展函数
//返回类的主构造函数，没有主构造函数返回null
//val <T : Any> KClass<T>.primaryConstructor: KFunction<T>?
//    get() = (this as KClassImpl<T>).constructors.firstOrNull {
//        ((it as KFunctionImpl).descriptor as ConstructorDescriptor).isPrimary
//    }
//
////返回伴生对象实例，没有的话返回null
//val KClass<*>.companionObject: KClass<*>?
//    get() = nestedClasses.firstOrNull {
//        (it as KClassImpl<*>).descriptor.isCompanionObject
//    }
//
////返回伴生对象实例，否则为null
//val KClass<*>.companionObjectInstance: Any?
//    get() = companionObject?.objectInstance
//
////返回该类定义的属性和方法，父类中的不计入
//val KClass<*>.declaredMembers: Collection<KCallable<*>>
//    get() = (this as KClassImpl).data().declaredMembers
//
////返回该类以及父类的所有函数，包括静态函数
//val KClass<*>.functions: Collection<KFunction<*>>
//    get() = members.filterIsInstance<KFunction<*>>()
//
////返回该类中的静态函数
//val KClass<*>.staticFunctions: Collection<KFunction<*>>
//    get() = (this as KClassImpl).data().allStaticMembers.filterIsInstance<KFunction<*>>()
//
////返回该类和父类的所有成员函数，即非扩展、非静态的函数
//val KClass<*>.memberFunctions: Collection<KFunction<*>>
//    get() = (this as KClassImpl).data().allNonStaticMembers.filter { it.isNotExtension && it is KFunction<*> } as Collection<KFunction<*>>
//
////返回该类和父类所有的扩展函数
//val KClass<*>.memberExtensionFunctions: Collection<KFunction<*>>
//    get() = (this as KClassImpl).data().allNonStaticMembers.filter { it.isExtension && it is KFunction<*> } as Collection<KFunction<*>>
//
////返回该类的所有函数
//val KClass<*>.declaredFunctions: Collection<KFunction<*>>
//    get() = (this as KClassImpl).data().declaredMembers.filterIsInstance<KFunction<*>>()
//
////返回该类中的非静态、非扩展函数
//val KClass<*>.declaredMemberFunctions: Collection<KFunction<*>>
//    get() = (this as KClassImpl).data().declaredNonStaticMembers.filter { it.isNotExtension && it is KFunction<*> } as Collection<KFunction<*>>
//
////返回该类的扩展函数
//val KClass<*>.declaredMemberExtensionFunctions: Collection<KFunction<*>>
//    get() = (this as KClassImpl).data().declaredNonStaticMembers.filter { it.isExtension && it is KFunction<*> } as Collection<KFunction<*>>
//
////返回该类的静态属性
//val KClass<*>.staticProperties: Collection<KProperty0<*>>
//    get() = (this as KClassImpl).data().allStaticMembers.filter { it.isNotExtension && it is KProperty0<*> } as Collection<KProperty0<*>>
//
////返回该类和父类的所有非扩展属性
//val <T : Any> KClass<T>.memberProperties: Collection<KProperty1<T, *>>
//    get() = (this as KClassImpl<T>).data().allNonStaticMembers.filter { it.isNotExtension && it is KProperty1<*, *> } as Collection<KProperty1<T, *>>
//
////返回该类和父类的扩展属性
//val <T : Any> KClass<T>.memberExtensionProperties: Collection<KProperty2<T, *, *>>
//    get() = (this as KClassImpl<T>).data().allNonStaticMembers.filter { it.isExtension && it is KProperty2<*, *, *> } as Collection<KProperty2<T, *, *>>
//
////返回该类中的非扩展属性
//val <T : Any> KClass<T>.declaredMemberProperties: Collection<KProperty1<T, *>>
//    get() = (this as KClassImpl<T>).data().declaredNonStaticMembers.filter { it.isNotExtension && it is KProperty1<*, *> } as Collection<KProperty1<T, *>>
//
////返回该类的扩展属性
//val <T : Any> KClass<T>.declaredMemberExtensionProperties: Collection<KProperty2<T, *, *>>
//    get() = (this as KClassImpl<T>).data().declaredNonStaticMembers.filter { it.isExtension && it is KProperty2<*, *, *> } as Collection<KProperty2<T, *, *>>
//
////创建实例，通过空参数构造函数或者全参构造函数
//fun <T : Any> KClass<T>.createInstance(): T {
//    val noArgsConstructor = constructors.singleOrNull { it.parameters.all(KParameter::isOptional) }
//        ?: throw IllegalArgumentException("Class should have a single no-arg constructor: $this")
//
//    return noArgsConstructor.callBy(emptyMap())
//}



//KCallable
//KCallable表示一个可调用的实体，比如函数或者属性
//把一个函数和属性它的公共点给抽离出来，包括属性/方法的可见性、参数、返回值等等
//public actual interface KCallable<out R> : KAnnotatedElement {
//
//    //这个可调用对象在代码中声明的名称
//    public actual val name: String
//
//    //调用这个可调用对象所需要的参数，假如调用这个对象需要this实例或者扩展接收器参数，把这些
//    //参数类型放在列表首位
//    public val parameters: List<KParameter>
//
//    //这个可调用对象的返回类型
//    public val returnType: KType
//
//    //类型参数列表，也就是使用泛型时会用到
//    public val typeParameters: List<KTypeParameter>
//
//    //使用指定的参数列表调用这个可调用对象并且返回结果
//    public fun call(vararg args: Any?): R
//
//    //访问权限可见性，即public、protected等
//    public val visibility: KVisibility?
//
//    //是否是final
//    public val isFinal: Boolean
//
//    //是否是open
//    public val isOpen: Boolean
//
//    //是否是抽象的
//    public val isAbstract: Boolean
//
//    //是否是挂起函数
//    public val isSuspend: Boolean
//}


//KFunction 接口
//public actual interface KFunction<out R> : KCallable<R>, Function<R> {
      //是否内联
//    public val isInline: Boolean
//
//    public val isExternal: Boolean
//
//    public val isOperator: Boolean
//
//    public val isInfix: Boolean
//    public override val isSuspend: Boolean
//}


//KProperty
////Kotlin反射表示属性，继承至KCallable
//public actual interface KProperty<out V> : KCallable<V> {
//
//    //该属性是否是延迟初始化
//    public val isLateinit: Boolean
//
//    //该属性是否是const
//    public val isConst: Boolean
//
//    //get函数
//    public val getter: Getter<V>
//}

////Kotlin中属性有val和var，对于var定义的属性
//public actual interface KMutableProperty<V> : KProperty<V> {
//    public val setter: Setter<V>
//    public interface Setter<V> : KProperty.Accessor<V>, KFunction<Unit>
//}

//KProperty0，KMutableProperty0
//无接收的属性，这种情况是定义在顶层函数中的属性或者属性定义时它自带get方法
//public actual interface KProperty0<out V> : KProperty<V>, () -> V {
//    //不用传递参数即可获取属性的值
//    public actual fun get(): V
//}
//
//public actual interface KMutableProperty0<V> : KProperty0<V>, KMutableProperty<V> {
//    //直接传递属性值即可赋值
//    public actual fun set(value: V)
//}


//KProperty1，KMutableProperty1
//一个接收者的属性，这种也是最常见的，比如定义在类中的非扩展属性，定义的扩展属性，这种属性获取其值必须要传入其接收者
//public actual interface KProperty1<T, out V> : KProperty<V>, (T) -> V {
//    //必须传递一个接收者，才可以获取属性值
//    public actual fun get(receiver: T): V
//}
//
//public actual interface KMutableProperty1<T, V> : KProperty1<T, V>, KMutableProperty<V> {
//    //必须传递一个接收者，也就是给哪个对象的属性设置值
//    public actual fun set(receiver: T, value: V)
//}


//KProperty2，KMutableProperty2
//2个接收者的属性，这种比较少见，只有定义在类中的扩展属性才可以，当然要获取这种属性的值或者给它赋值需要传入2个接收者
//public actual interface KProperty2<D, E, out V> : KProperty<V>, (D, E) -> V {
//
//    public actual fun get(receiver1: D, receiver2: E): V
//}
//public actual interface KMutableProperty2<D, E, V> : KProperty2<D, E, V>, KMutableProperty<V> {
//
//    public actual fun set(receiver1: D, receiver2: E, value: V)
//}

