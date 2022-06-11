package com.sst.libkotlin.clasz

//https://www.bilibili.com/video/BV1Yq4y1U7rS?p=4
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

//泛型上限
// 为泛型指定上界，我们可以使用 <T : Class> 这种语法结构，如果不指定泛型的上界，默认为 Any? 类型
//如果有多个边界，可以使用 where 关键字，中间使用 : 隔开，多个边界中只能有一个边界是类，且类必须放在最前面 例如MultiBoundClass

//泛型实化    https://juejin.cn/post/6950042154496425992#heading-22
//Java 中的泛型存在类型擦除的情况，任何在运行时需要知道泛型确切类型信息的操作都没法用
//kotlin可以通过inline和reified关键字对泛型进行实化  例如getGenericType
//reify ['ri:ɪfaɪ] vt. 使具体化; 变形
//Kotlin 中可以实现泛型实化，是因为使用的内联函数会对代码进行替换，那么在内联函数中使用泛型，最终也会使用实际的类型进行替换
//泛型实化对代码的优化
//val intent = Intent(mContext,TestActivity::class.java)
//mContext.startActivity(intent)
//优化为
//定义一个顶层函数
//inline fun <reified T> startActivity(mContext: Context){
//    val intent = Intent(mContext,T::class.java)
//    mContext.startActivity(intent)
//}
////使用的时候
//startActivity<TestActivity>(mContext)
// 对于携带参数的情况
//val intent  = Intent(mContext,TestActivity::class.java)
//intent.putExtra("params1","erdai")
//intent.putExtra("params2","666")
//mContext.startActivity(intent)
//inline fun <reified T> startActivity(mContext: Context, block: Intent.() -> Unit){
//    val intent = Intent(mContext,T::class.java)
//    intent.block()
//    mContext.startActivity(intent)
//}
////使用的时候
//startActivity<SecondActivity>(mContext){
//    putExtra("params1","erdai")
//    putExtra("params2","666")
//}

//泛型协变，逆变和不变
//1 泛型协变的语法规则：<out T> 类似于 Java 的 <? extends Bound>，它限定的类型是当前上边界类或者其子类，
// 如果是接口的话就是当前上边界接口或者实现类，协变的泛型变量只读，不可以写，可以添加 null ，但是没意义 ,例如SimpleDataOut

//2 泛型逆变的语法规则：<in T> 类似于 Java 的 <? super Bound>，它限定的类型是当前下边界类或者其父类，
// 如果是接口的话就是当前下边界接口或者其父接口，逆变的泛型变量只能写，不建议读，例如SimpleDataIn

//3 泛型不变 默认的泛型T是不变的

//4 Kotlin 使用 <*> 这种语法结构来表示无界通配符，它等价于 <out Any>，类似于 Java 中的 <?>，
//  在定义一个类的时候你如果使用<out T : Number> ，那么 * 就相当于 <out Number>


//kotlin的PECS规则  Producer-Extends, Consumer-Super
//1 如果只读用out
//2 如果只写用in
//3 既读又写使用泛型T
class MGeneric {


    //泛型方法  在方法名的前面加上 <T>
    //我们指定了泛型的上界为 Number, 那么我们就只能传入数字类型的参数了
    fun <T : Number> method(params: T) {}
    //编译为
//    public final void method(@NotNull Number params) {
//        Intrinsics.checkNotNullParameter(params, "params");
//    }
    fun <T> method1(params: T) {}
    //编译为  Any编译为Java的Object
//    public final void method1(Object params) {
//    }

    companion object{
        @JvmStatic
        fun main(args: Array<String>) {
           //泛型方法
           val mGeneric = MGeneric()
           mGeneric.method1<Int>(1)
           mGeneric.method(2)//根据Kotlin 类型推导机制，可以把泛型给省略

           //泛型实化
           println(getGenericType<String>())
           println(getGenericType<Number>())
//           编译为
//            int $i$f$getGenericType = false;
//            Class var4 = String.class;
//            System.out.println(var4);
//            $i$f$getGenericType = false;
//            var4 = Number.class;
//            System.out.println(var4);

           //协变
           //基础关系
           val  person:Person = Student()
           //协变关系
           val personGenericOut:SimpleDataOut<Person> = SimpleDataOut<Student>()
           val list1:ArrayList<out Person> = ArrayList<Student>()

           //逆变
           //逆变关系
           val personGenericIn:SimpleDataIn<Student> = SimpleDataIn<Person>()
           val list2:ArrayList<in Student> = ArrayList<Person>()

           //不变 默认的泛型是不变的
//           val personGenericNormal:SimpleDataNormal<Person> = SimpleDataNormal<Student>()  //编辑器不支持

           //无界通配符*
            // 无界通配符 等价于 <out Any>，但是SimpleDataOutWithUpBound这个类限制了泛型边界为 Person，
            // 因此这里相当于 <out Person>
           val noBound1:SimpleDataOutWithUpBound<*> = SimpleDataOutWithUpBound<Student>()
            //SimpleDataOutWithUpBound限定的上限，Any超出上限， 泛型的*与Any也不符合协变规则
//         val noBound: SimpleDataOutWithUpBound<*> = SimpleDataOutWithUpBound<Any>()
          //报错： Type argument is not within its bounds: should be subtype of 'Person'


        }
    }
}

//泛型类  定义一个泛型类，在类名后面使用 <T>
class GenericClass<T>{
    fun method(param:T) {}
}

//泛型接口 在接口名后面加上 <T>
interface GenericInterface<T>{
    fun method(param:T)
}

open class Animal
interface Food
interface Food2
class MultiBoundClass<T> where T:Animal,T:Food,T:Food2{

}

inline fun <reified T> getGenericType() = T::class.java
//编译为 将T编译为class
//public final class MGenericKt {
//    // $FF: synthetic method
//    public static final Class getGenericType() {
//        int $i$f$getGenericType = 0;
//        Intrinsics.reifiedOperationMarker(4, "T");
//        return Object.class;
//    }
//}

open class Person
class Student: Person()
class Teacher: Person()

class SimpleDataOut<out T>{

}

class SimpleDataOutWithUpBound<out T:Person>{

}

class SimpleDataIn<in T>{

}

class SimpleDataNormal<T>{

}