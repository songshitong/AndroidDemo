package sst.example.lib;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

//java 中泛型标记符：   //类型参数只能代表引用型类型，不能是原始类型（像 int、double、char 等）
//        E - Element (在集合中使用，因为集合中存放的是元素)
//        T - Type（Java 类）
//        K - Key（键）
//        V - Value（值）
//        N - Number（数值类型）
//        ？ - 表示不确定的 java 类型
public class MGeneric {

    //定义一个泛型类  在类名的后面加上 <T> 这种语法结构就是定义一个泛型类，泛型可以有任意多个
    public class JavaGenericClass<T> {
        private T a;
        public JavaGenericClass(T a) {
            this.a = a;
        }
    }
    //多个泛型的泛型类
    public class JavaGenericClassMulti<K,V> {
        private K k;
        private V v;
        public JavaGenericClassMulti(K k,V v) {
            this.k = k;
            this.v = v;
        }
    }


    //泛型方法  在方法的返回值前面加上 <T> 这种语法结构就是定义一个泛型方法，泛型可以有任意多个，泛型方法的泛型与它所在的类没有任何关系
    //注意只有声明了<T>的方法才是泛型方法，泛型类中的使用了泛型的成员方法并不是泛型方法。
    public <T> void getName(T t){
        System.out.println(t.getClass().getSimpleName());
    }

    public <T> void getName1(){
        System.out.println("11");
    }

    //带返回值得泛型方法  返回类型为泛型
    public <T> T getNameReturn(T t){
        System.out.println(t.getClass().getSimpleName());
        return t;
    }

    //泛型接口   在接口名的后面加上 <T> 这种语法结构就是定义一个泛型接口，泛型可以有任意多个
    public interface Generator<T> {
        public T next();
    }


    public static void main(String[] args) {
        //泛型擦除后，两个class都被编译为
//        Class a = (new ArrayList()).getClass();
//        Class b = (new ArrayList()).getClass();
        Class a = new ArrayList<String>().getClass();
        Class b = new ArrayList<Integer>().getClass();

        System.out.println("a = " + a);
        System.out.println("b = " + b);
        System.out.println("a == b: " + (a == b));
//        结果:
//        a = class java.util.ArrayList
//        b = class java.util.ArrayList
//        a == b: true

        //擦除的步骤   //https://juejin.cn/post/6950042154496425992#heading-18
//        1）、擦除所有类型参数信息，如果类型参数是有界的，则将每个参数替换为其第一个边界；如果类型参数是无界的，
//        则将其替换为 Object类型擦除的规则：
//        <T> 擦除后变为 Object
//        <T extends A> 擦除后变为 A
//        <? extends A> 擦除后变为 A
//        <? super A> 擦除后变为Object
//        2）、（必要时）插入类型转换，以保持类型安全
//        3）、（必要时）生成桥接方法以在子类中保留多态性
         //插入类型转换
         testGenericWipe1();
         //桥接方法
         testGenericBridge();

         //伪泛型
//        Java 中的泛型是一种特殊的语法糖，通过类型擦除实现，这种泛型称为伪泛型，我们可以反射绕过编译器泛型检查，添加一个不同类型的参数
        testGenericReflect();

        //动态创建的泛型擦除与获取
        getGeneric1();
        //匿名内部类的泛型与实例类的泛型
        getGeneric2();
        //泛型获取总结
//        如果在编译的时候就保存了泛型类型到字节码中，那么在运行时我们就可以通过反射获取到，如果在运行时传入实际的泛型类型，
//        这个时候就会被擦除，反射获取不到当前传入的泛型实际类型
        //getGeneric1指定了泛型的实际类型为 String，编译的时候就将它存储到了字节码文件中，因此我们获取到了泛型类型
        //匿名内部类，同样在编译的时候会进行创建并保存了实际的泛型到字节码中，因此我们可以获取到
        //parentGeneric1 是在运行时创建的，虽然 ParentGeneric 声明的泛型 T 在编译时也保留在了字节码文件中，但是它传入的实际类型被擦除了，
        // 这种泛型也是无法通过反射获取的

        //如果我们想要获取某个泛型类型，我们可以通过子类的帮助去取出该泛型类型，一种良好的编程实践就是把当前需要获取的泛型类用 abstract 声明
        getGenericSummary();


        //边界
        //边界就是在泛型的参数上设置限制条件，这样可以强制泛型可以使用的类型，更重要的是可以按照自己的边界类型来调用方法
        //语法 <T extends Bound> ，Bound 可以是类和接口，如果不指定边界，默认边界为 Object
        //设置多个边界，中间使用 & 连接，多个边界中只能有一个边界是类，且类必须放在最前面 <T extends ClassBound & InterfaceBound1 & InterfaceBound2>
        new MGeneric().testMultiBound();

        //泛型的协变，逆变，不变
        Number number = new Integer(666);
        //ArrayList<Number> numberList = new ArrayList<Integer>();//编译器报错 type mismatch
        //如何修正？  使用通配符，让泛型具备协变
        ArrayList<? extends  Number> numberList = new ArrayList<Integer>();
        //为啥 Number 的对象可以由 Integer 实例化，而 ArrayList<Number> 的对象却不能由 ArrayList<Integer> 实例化？
        //定义 https://www.cnblogs.com/en-heng/p/5041124.html
//        逆变与协变用来描述类型转换（type transformation）后的继承关系，其定义：如果A、B表示类型，f(⋅)表示类型转换，
//           ≤表示继承关系（比如，A≤B表示A是由B派生出来的子类）；
//        f(⋅)是逆变（contravariant）的，当A≤B时有f(B)≤f(A)成立；
//        f(⋅)是协变（covariant）的，当A≤B时有f(A)≤f(B)成立；
//        f(⋅)是不变（invariant）的，当A≤B时上述两个式子均不成立，即f(A)与f(B)相互之间没有继承关系。
        //Java泛型T是不变的，令f(A)=ArrayList<A>，Integer和Number有父子关系，但是ArrayList<Number>和ArrayList<Integer>没有父子关系
        //java数组是协变的，f(A)=[]A，Integer和Number有父子关系，同时数组也有父子关系Number[] numbers = new Integer[3];

        //java泛型如何支持协变，逆变？ 使用通配符
        //泛型的上边界通配符 <? extends Bound>，使得泛型支持协变，它限定的类型是当前上边界类或者其子类，
        // 如果是接口的话就是当前上边界接口或者实现类，使用上边界通配符的变量只读，不可以写，可以添加 null ，但是没意义
        //为啥使用上边界通配符的变量只读，而不能写
        // 1 <? extends Bound>,它限定的类型是当前上边界类或者其子类，它无法确定自己具体的类型，因此编译器无法验证类型的安全，所以不能写
        // 2 假设可以写，我们向它里面添加若干个子类，然后用一个具体的子类去接收，势必会造成类型转换异常
        // 上界的缺点  https://itimetraveler.github.io/2016/12/27/%E3%80%90Java%E3%80%91%E6%B3%9B%E5%9E%8B%E4%B8%AD%20extends%20%E5%92%8C%20super%20%E7%9A%84%E5%8C%BA%E5%88%AB%EF%BC%9F/
//        读取出来的东西只能存放在Bound或它的基类里
        testUpBound();

        //泛型的下边界通配符 <? super Bound>，使得泛型支持逆变，它限定的类型是当前下边界类或者其父类，
        // 如果是接口的话就是当前下边界接口或者其父接口，使用下边界通配符的变量只写，不建议读
        //为啥使用下边界通配符的变量可以写，而不建议读
        //1 <? super Bound>，它限定的类型是当前下边界类或者其父类，虽然它也无法确定自己具体的类型，但根据多态，
        //   它能保证自己添加的元素是安全的，因此可以写
        //2 获取值的时候，会返回一个 Object 类型的值，而不能获取实际类型参数代表的类型，因此建议不要去读，如果你实在要去读也行，
        //   但是要注意类型转换异常，用instanceof进行类型判断
        //下界的缺点https://itimetraveler.github.io/2016/12/27/%E3%80%90Java%E3%80%91%E6%B3%9B%E5%9E%8B%E4%B8%AD%20extends%20%E5%92%8C%20super%20%E7%9A%84%E5%8C%BA%E5%88%AB%EF%BC%9F/
//        下界规定了元素的最小粒度的下限，实际上是放松了容器元素的类型控制。既然元素是Bound的基类，那往里存粒度比Bound小的都可以。
//        但往外读取元素就费劲了，只有所有类的基类Object对象才能装下。但这样的话，元素的类型信息就全部丢失
        testDownBound();

        //泛型的无边界通配符 <?>，实际上它等价于 <? extends Object>，也就是说它的上边界是 Object 或其子类，
        // 因此使用无界通配符的变量同样只读，不能写，可以添加 null ，但是没意义
        // 如果一个类在声明时限定<T extends Customer>，此时?相当于<? extend Merchant>
        //通配符<?>和类型参数的区别就在于，https://itimetraveler.github.io/2016/12/27/%E3%80%90Java%E3%80%91%E6%B3%9B%E5%9E%8B%E4%B8%AD%20extends%20%E5%92%8C%20super%20%E7%9A%84%E5%8C%BA%E5%88%AB%EF%BC%9F/
        // 对编译器来说所有的T都代表同一种类型，多个T要么是String，要么是其他的
        //通配符<?>没有这种约束，单纯的就表示：里面放了一个东西，是什么我不知道
        testNoBound();

        //PECS 原则
//        泛型代码的设计，应遵循PECS原则（Producer extends Consumer super）：
//        1）、如果只需要获取元素，使用 <? extends T>
//        2）、如果只需要存储，使用 <? super T>
//        3)既要取又要写，就不用通配符（即extends与super都不用）
        //这是 Collections.java 中 copy 方法的源码
//        public static <T> void copy(List<? super T> dest, List<? extends T> src) {
//            //...
//        }
        // 从src 拷贝 到 dest   src生成元素，dest消费元素
        //src 表示原始集合，使用了 <? extends T>，只能从中读取元素，dest 表示目标集合，只能往里面写元素，充分的体现了 PECS 原则

        //todo 泛型的限制
        // 类型擦除引起的问题及解决方法https://www.cnblogs.com/wuqinglong/p/9456193.html
//        由于 Java 中的泛型存在类型擦除的情况，任何在运行时需要知道泛型确切类型信息的操作都没法用了。
//        比如你不能检查一个对象是否为泛型类型 T 的实例：
//        <T> void printIfTypeMatch(Object item) {
//            if (item instanceof T) { // 👈 IDE 会提示错误，illegal generic type for instanceof
//                System.out.println(item);
//            }
//        }
//        在 Java 中的解决方案通常是额外传递一个 Class<T> 类型的参数，然后通过 Class#isInstance 方法来检查：
//        <T> void check(Object item, Class<T> type) {
//            if (type.isInstance(item)) {
//               👆
//                System.out.println(item);
//            }
//        }

//        类型边界和通配符边界有什么区别？
//        类型边界可以有多个，通配符边界只能有一个

//        List<?> 和 List<Object> 一样吗？
//        不一样
//        1、 List<Object> 可读写，但是 List<?> 只读
//        2、List<?>可以有很多子类，但是 List<Object> 没有
        List<Object> list = new ArrayList();
        list.add("ss");
        list.add(new MGeneric());
        Object obj = list.get(0);
        //List<?>的子类
//        List<Object> list1 = new ArrayList<String>(); //报错 Incompatible types. Found: 'java.util.ArrayList<java.lang.String>', required: 'java.util.List<java.lang.Object>'
        List<?> list1 = new ArrayList<String>();
//        list1.add("str"); //报错 只能读
        List<?> list2 = new ArrayList<Integer>();
    }

    private static void testNoBound() {
        List<String> list1 = new ArrayList<String>();
        List<Number> list2 = new ArrayList<Number>();
        List<Integer> list3 = new ArrayList<Integer>();
        //无界通配符 Merchant在声明时限定<T extends Customer>，此时?相当于<? extend Merchant>
        Merchant<?> merchant = new Merchant<VIP>();
        list1.add("1");
        list2.add(2);
        list3.add(3);
        noBound(list1);
        noBound(list2);
        noBound(list3);

    }

    private static void noBound(List<?> data) {
        System.out.println(data.get(0));
//        data.add(1); // 报错 'add(capture<?>)' in 'java.util.List' cannot be applied to '(int)'
    }

    private static void testDownBound() {
        List<Object> list1 = new ArrayList<Object>();
        List<Number> list2 = new ArrayList<Number>();
        list1.add(1);
        list2.add(2);
        downBound(list1);
        downBound(list2);
    }

    private static void downBound(List<? super Number> data) {
        data.add(11);
        //存入比number粒度更小的是可以的
        data.add(0.1f);
        //不建议读，如果读注意使用instanceof进行类型判断
        if(data.get(0) instanceof  Number){
            Number number = (Number) data.get(0);
            System.out.println(number.intValue());
        }

    }

    private static void testUpBound() {
        List<Integer> list1 = new ArrayList<Integer>();
        List<Number> list2 = new ArrayList<Number>();
        list1.add(1);
        list2.add(2);
        upBound(list1);
        upBound(list2);
    }

    private static void upBound(List<? extends Number> data) {
        System.out.println(data.get(0));
//        data.add(22); //报错'add(capture<? extends java.lang.Number>)' in 'java.util.List' cannot be applied to '(int)'
    }

    private  void testMultiBound() {
        RealBound rb = new RealBound();
        //对泛型有多个限制，要求实现接口并且是某个类的子类
        MultiBound<RealBound> mb = new MultiBound<RealBound>(rb);
        mb.test1();
        mb.test2();
        mb.test3();
    }

    private static void getGenericSummary() {
        //定义一个子类继承泛型父类，并给这个泛型一个实际的类型
        class SubClass extends ParentGeneric<String>{

        }

        //匿名内部类，其实我们定义的这个匿名内部类也是一个子类，它继承了泛型父类，并给这个泛型一个实际的类型
        ParentGeneric<String> parentGeneric2 = new ParentGeneric<String>(){};
    }

    private static void getGeneric2() {
        ParentGeneric<String> parentGeneric1 = new ParentGeneric<String>();
        ParentGeneric<String> parentGeneric2 = new ParentGeneric<String>(){};
        //打印 parentGeneric1 获取的泛型
        System.out.println("parentGeneric1: " + findGenericType(parentGeneric1.getClass()));
        //打印 parentGeneric2 获取的泛型
        System.out.println("parentGeneric2: " + findGenericType(parentGeneric2.getClass()));
        //结果
//        parentGeneric1: null
//        parentGeneric2: class java.lang.String
        //ParentGeneric的字节码
//        class sst.example.lib.MGeneric$ParentGeneric<T extends java.lang.Object> extends java.lang.Object
//        ParentGeneric 声明的泛型 T 在编译的时候其实是保留在了字节码文件中，parentGeneric1 是在运行时创建的，由于泛型擦除，
//        我们无法通过反射获取其中的类型，因此打印了 null
//        这个地方可能大家又会有个疑问了，你既然保留了泛型类型为 T，那么我获取的时候应该为 T 才是，为啥打印的结果是 null 呢？
//        findGenericType中getGenericSuperclass()返回的是class java.lang.Object，不是ParameterizedType类型，返回null

        //parentGeneric2 在创建的时候后面加了 {}，这就使得 parentGeneric2 成为了一个匿名内部类，且父类就是 ParentGeneric，
        // 因为匿名内部类是在编译时创建的，那么在编译的时候就会创建并携带具体的泛型信息，因此 parentGeneric2 可以获取其中的泛型类型
        //匿名内部类创建文件：外部类$1.class
        //parentGeneric2 匿名内部类的字节码  匿名内部类为ParentGeneric的泛型进行了赋值String
//        class MGeneric$1 extends ParentGeneric<String> {
//            MGeneric$1() {
//            }
//        }
        //findGenericType中getGenericSuperclass()返回的是Test$ParentGeneric<java.lang.String>，类型为ParameterizedTypeImpl
        //可以获取实际的泛型类型java.lang.String
    }

    private static void getGeneric1() {
//        https://juejin.cn/post/6950042154496425992
//        1、 SubClass 相当于对 ParentGeneric 做了赋值操作 T = String，我们通过反射获取到了泛型类型为 String
//        2、SubClass2 对 ParentGeneric没有做赋值操作 ，我们通过反射获取到了泛型类型为 T
//        1、为啥 1 中没有传入任何泛型的信息却能获取到泛型类型呢？
//        2、为啥 2 中我创建对象的时候传入的泛型是 Integer ，获取的时候变成了 T 呢？
//       类型擦除其实只是擦除 Code 属性中的泛型信息，在类常量池属性中还保留着泛型信息，因此上面的 SubClass 和SubClass2
//       在编译的时候其实会保留各自的泛型到字节码文件中，一个是 String，一个是 T 。而 subClass 和 subClass2 是运行时动态创建的，
//       这个时候你即使传入了泛型类型，也会被擦除掉，因此才会出现上面的结果

        SubClass subClass = new SubClass();
        SubClass2<Integer> subClass2 = new SubClass2<Integer>();
        //打印 subClass 获取的泛型
        System.out.println("subClass: " + findGenericType(subClass.getClass()));
        //打印subClass2获取的泛型
        System.out.println("subClass2: " + findGenericType(subClass2.getClass()));
        //结果
//        subClass: class java.lang.String
//        subClass2: T
    }

    private static void testGenericReflect() {
        List<String> list = new ArrayList<>();
        list.add("aa");
        list.add("bb");
        try {
            Method m=list.getClass().getMethod("add",Object.class);
            m.invoke(list,1);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        System.out.println("testGenericReflect =======");
        //结果
//        aa
//        bb
//        1  使用反射成功绕过了泛型，在string的泛型添加了数字1
        Iterator iterator = list.iterator();
        while (iterator.hasNext()){
            System.out.println(iterator.next());
        }
    }

    private static void testGenericBridge() {
        Merchant m = new VIPOnlyMerchant();
        m.actionPrice(0.1, new VIP() {
            @Override
            public boolean isVIP() {
                return false;
            }
        });
        //VIPOnlyMerchant的字节码会增加一个桥接方法
//        public double actionPrice(Customer customer) {
//            return actionPrice((VIP) customer);
//        }
        //该方法调用原来的重写方法
//        public double actionPrice(double price, VIP customer) {
//            return 0.1;
//        }
    }

    private static void testGenericWipe1() {
        List<String> stringList = new ArrayList<>();
        stringList.add("erdai");
        stringList.add("666");

        for (String s : stringList) {
            System.out.println(s);
        }
        //编译后
//        Iterator var2 = stringList.iterator();
//        while(var2.hasNext()) {
//            //编译器给我们做了强转的工作
//            String s = (String)var2.next();
//            System.out.println(s);
//        }

    }

    //获取实际的泛型类型，没有实际的泛型例如Object返回null
    public static <T> Type findGenericType(Class<T> cls) {
        //获取当前带有泛型的父类
        Type genType = cls.getGenericSuperclass();
        Type finalNeedType = null;
        //如果当前 genType 是参数化类型则进入到条件体
        if (genType instanceof ParameterizedType) {
            //获取参数类型 <> 里面的那些值,例如 Map<K,V> 那么就得到 [K,V]的一个数组
            Type[] params = ((ParameterizedType) genType).getActualTypeArguments();
            //将第一个泛型类型赋值给 finalNeedType
            finalNeedType = params[0];
        }
        return finalNeedType;
    }


     interface Customer  {
        boolean isVIP();

    }

     interface VIP extends Customer{

    }

    static class Merchant<T extends Customer> {
        public double actionPrice(double price, T customer) {
            return 0.1;
        }
    }
    static class VIPOnlyMerchant extends Merchant<VIP> {
        @Override
        public double actionPrice(double price, VIP customer) {
            return 0.1;
        }
    }


    static class ParentGeneric<T> {

    }

    static class SubClass extends ParentGeneric<String>{

    }

    static class SubClass2<T> extends ParentGeneric<T> {

    }


    abstract class ClassBound{
        public abstract void test1();
    }

    interface InterfaceBound1{
        void test2();
    }

    interface InterfaceBound2{
        void test3();
    }

     class MultiBound<T extends ClassBound & InterfaceBound1 & InterfaceBound2>{
        private final T item;

        public MultiBound(T item) {
            this.item = item;
        }

        public void test1(){
            item.test1();
        }

        public void test2(){
            item.test2();
        }

        public void test3(){
            item.test3();
        }
    }

     class RealBound extends ClassBound implements InterfaceBound1,InterfaceBound2 {

        @Override
        public void test1() {
            System.out.println("test1");
        }

        @Override
        public void test2() {
            System.out.println("test2");
        }

        @Override
        public void test3() {
            System.out.println("test3");
        }
    }


}
