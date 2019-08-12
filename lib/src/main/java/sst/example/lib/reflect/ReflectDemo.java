package sst.example.lib.reflect;

import sst.example.lib.metadata.MyAnnotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
//反射是指计算机程序在运行时可以访问、检测和修改它本身状态或行为的一种能力
//反射机制直接创建对象即使这个对象在编译期是未知的，
//        反射的核心：是 JVM 在运行时 才动态加载的类或调用方法或属性，他不需要事先（写代码的时候或编译期）知道运行对象是谁
//使用 Java 反射机制可以在运行时期检查 Java 类的信息，检查 Java 类的信息往往是你在使用 Java 反射机制的时候所做的第一件事情，
//        通过获取类的信息你可以获取以下相关的内容：
//        Class 对象
//        类名
//        修饰符
//        包信息
//        父类
//        实现的接口
//        构造器
//        方法
//        变量
//        注解
//全部的信息你可以查看相应的文档 JavaDoc for java.lang.Class 里面有详尽的描述。
public class ReflectDemo {
    public static void main(String[] args) {

//        在你想检查一个类的信息之前，你首先需要获取类的 Class 对象
        //获取class 对象
        //1  直接获取某一个对象的 class
        Class intClass = int.class;
        Class integerClass = Integer.TYPE;
        Class strClass = String.class;
        //2 使用 Class 类的 forName 静态方法  class的完整名称
        Class reflectTest = null;
        try {
            reflectTest = Class.forName("sst.example.lib.reflect.ReflectTest");
        } catch (ClassNotFoundException e) {
            //路径不对，类找不到
            e.printStackTrace();
        }
        //3 调用某个对象的 getClass() 方法
        StringBuilder str = new StringBuilder("123");
        Class<?> sbClass = str.getClass();

//        创建实例的方法有两种，一种使用java.lang.reflect.Constructor.newInstance()创建一种使用Class.newInstance()创建。
//        两种主要的区别是
//
//        Class.newInstance() 只能调用无参数的构造器，Constructor.newInstance()可以接收多个参数。
//        Class.newInstance() 抛出原生异常，Constructor.newInstance()会使用InvocationTargeException包装异常
//        Class.newInstance() 只能使用public，protecte构造器，Constructor.newInstance()在某些情况下可以调用private构造器。

        //获取类的标识符 public,private,static
        int modifiers  =  reflectTest.getModifiers();
//        修饰符都被包装成一个int类型的数字，这样每个修饰符都是一个位标识(flag bit)，这个位标识可以设置和清除修饰符的类型
//        使用 java.lang.reflect.Modifier 类中的方法来检查修饰符的类型
        System.out.println("isPublic "+Modifier.isPublic(modifiers));
        System.out.println("isPublic "+Modifier.isPrivate(modifiers));
        System.out.println("isPublic "+Modifier.isInterface(modifiers));

        //包信息
        Package packageInfo = reflectTest.getPackage();
        System.out.println("packageInfo "+packageInfo.getName() +" tostring"+packageInfo.toString());


        //父类
        Class superClass = reflectTest.getSuperclass();
        System.out.println("superClass "+superClass.getName());

        //接口
        Class[] interfaces = reflectTest.getInterfaces();
//        由于一个类可以实现多个接口，因此 getInterfaces(); 方法返回一个 Class 数组，在 Java 中接口同样有对应的 Class 对象。
//        注意：getInterfaces() 方法仅仅只返回当前类所实现的接口。当前类的父类如果实现了接口，这些接口是不会在返回的 Class 集合中的，尽管实际上当前类其实已经实现了父类接口
        System.out.println("interfaces "+interfaces[0]);


//        访问一个类的构造方法  只能访问public的构造器
        Constructor[] constructors = reflectTest.getConstructors();
        System.out.println("constructors "+constructors[0]);
        try {
            Constructor constructor =
                    reflectTest.getConstructor(new Class[]{String.class,String.class});//指定参数类型
            System.out.println("指定 constructors "+constructor);
            Class[] parameterTypes = constructor.getParameterTypes(); //获取指定构造方法的方法参数信息
            System.out.println("parameterTypes "+parameterTypes.length);

            //利用 Constructor 对象实例化一个类
//            调用构造方法的时候你必须提供精确的参数，即形参与实参必须一一对应
            ReflectTest rt = (ReflectTest) constructor.newInstance("name","age");
            System.out.println("Constructor create class  rt "+rt);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        //访问私有构造器  获取所有声明的构造器public，private
        Constructor[] privateConstructors = reflectTest.getDeclaredConstructors();
        for (Constructor privateConstructor : privateConstructors) {
            System.out.println("privateConstructors "+privateConstructor);

        }



//        访问一个类的所有public方法
        Method[] methods  = reflectTest.getMethods();
        for (Method method : methods) {
            System.out.println("methods 有 "+method);
        }
//        通过参数类型来获取指定的方法    方法名称+方法参数     无参，设为null
        try {
            Method printMsgMethod = reflectTest.getMethod("printMsg",new Class[]{String.class});
            System.out.println("printMsgMethod is "+printMsgMethod +" 方法返回类型 "+printMsgMethod.getReturnType()+" 方法参数有 "+printMsgMethod.getParameterTypes());
//            调用一个方法   一个静态方法调用的话则可以用 null 代替指定对象，
            //object is not an instance of declaring class 报错可能为调用类未初始化,必须是类的初始化对象，不能是类类型
            printMsgMethod.invoke(reflectTest.newInstance(),"printMsgMethod invoke");

        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }

        ReflectTest reflectTest2 = new ReflectTest();
        //访问一个类的私有方法   getDeclaredMethod 返回所有的方法，public，private，
        //getDeclaredMethod  第一个参数为方法名称，第二个是方法所需要的参数
        try {
            Method privateMethod = reflectTest.getDeclaredMethod("printStrIn",String.class);
            privateMethod.setAccessible(true);
            privateMethod.invoke(reflectTest2,"测试private method");

            Method declaredMethod = reflectTest.getDeclaredMethod("printIn",null);
            declaredMethod.setAccessible(true);
            System.out.println("获取私有方法 declaredMethod "+declaredMethod);
            declaredMethod.invoke(reflectTest2,null);

        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }


//        访问一个类的成员变量
        Field[] fields = reflectTest.getFields();//获取public修饰的变量
        System.out.println("fields "+fields[0]);
//        获取指定的变量
        try {
            Field age = reflectTest.getField("age");
            System.out.println("age "+age + " field name "+age.getName()+" field type "+age.getType());

//            通过调用 Field.get()或 Field.set()方法，获取或者设置变量的值
//            如果变量是静态变量的话(public static)那么在调用 Field.get()/Field.set()方法的时候传入 null 做为参数而不用传递拥有该变量的类的实例
            ReflectTest reflectTest1 = new ReflectTest("rt","1111");
            //获取feflectTest1的age 值
            Object ageObject = age.get(reflectTest1);
            System.out.println("ageObject "+ageObject);
            //改变feflectTest1的age 值
            age.set(reflectTest1,"2222");
            System.out.println("ageObject 改变age "+reflectTest1.age);
            Field staticParam = reflectTest.getField("staticParam");
            System.out.println("staticParam 是 "+staticParam.get(null));
            staticParam.set(null,"this is staticParam");
            System.out.println("staticParam 改变后 "+staticParam.get(null));
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        //获取类的私有变量   getDeclaredFields 返回所有的变量包括public，private
        Field[] declaredFileds =  reflectTest.getDeclaredFields();
        for (Field declaredFiled : declaredFileds) {
            System.out.println(" 私有变量 declaredFiled "+declaredFiled);
        }
        ReflectTest reflectTest1 = new ReflectTest();
        reflectTest1.setId(111);
        // ！！！！！！ 调用方法或获取变量值要用具体的对象而不类类型
        //！！！
        //！！！
        //！！
        //！
        //！！！
        //！！！
        //！！！
        //！！！
        //！！！
        //！！！
        //！！！
        //！！！

        try {
            Field idField = reflectTest1.getClass().getDeclaredField("id");
//            通过调用 setAccessible()方法会关闭指定类 Field 实例的反射访问检查，这行代码执行之后不论是私有的、受保护的以及包访问的作用域，你都可以在任何地方访问，
//            即使你不在他的访问权限作用域之内。但是你如果你用一般代码来访问这些不在你权限作用域之内的代码依然是不可以的，在编译的时候就会报错。
            idField.setAccessible(true);
            System.out.println("获取具体私有变量 idField "+idField.toString()+" value "+idField.get(reflectTest1));
            Field strField = reflectTest1.getClass().getDeclaredField("str");
            strField.setAccessible(true);
            System.out.println("获取具体私有变量 strField "+strField.get(reflectTest1));

        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }


//        访问一个类的注解 类的注释，其他的不行
        Annotation[] annotations = reflectTest.getAnnotations();
        System.out.println("annotations "+annotations[0]);
        //等同于   getAnnotation 获取特定注解在在该该类的使用
        MyAnnotation annotation = (MyAnnotation)reflectTest.getAnnotation(MyAnnotation.class);
        //获取自定义注解的定义的name和value
        System.out.println("annotation "+annotation + " name "+annotation.name()+" value "+annotation.value());

        // 获取方法的注解
        try {
            Method printMsgMethod = reflectTest.getMethod("printMsg",new Class[]{String.class});
            System.out.println("printMsgMethod "+printMsgMethod);
            Annotation[] printMsgAnnotations = printMsgMethod.getAnnotations();
            for (int i = 0; i < printMsgAnnotations.length; i++) {
                System.out.println(" printMsgAnnotations i "+i+" "+printMsgAnnotations[i]);
            }

            //获取方法的参数的注解
            //1
            Annotation[][] printMsgParamAnnotions = printMsgMethod.getParameterAnnotations();
            Class[] parameterTypes = printMsgMethod.getParameterTypes();
            int i=0;
            for(Annotation[] paramAnnotations : printMsgParamAnnotions){
                Class parameterType = parameterTypes[i++];
                for(Annotation paramAnnotation : paramAnnotations){
                    if(paramAnnotation instanceof MyAnnotation){
                        MyAnnotation myAnnotation = (MyAnnotation) paramAnnotation;
                        System.out.println("printMsgMethod annotation param: " + parameterType.getName());
                        System.out.println("printMsgMethod annotation name : " + myAnnotation.name());
                        System.out.println("printMsgMethod annotation value: " + myAnnotation.value());
                    }
                }
            }
            //2
            // 获取方法的参数1
            Parameter[] printMsgParms =  printMsgMethod.getParameters();
            for (int i1 = 0; i1 < printMsgParms.length; i1++) {
                Parameter printMsgParm = printMsgParms[i1];
                Annotation[] printMsgParmAnnotations = printMsgParm.getAnnotations();
                for (Annotation printMsgParmAnnotation : printMsgParmAnnotations) {
                    System.out.println("printMsg method printMsgParm  "+printMsgParm+" annotation "+printMsgParmAnnotation );

                }

            }

            //获取变量的注释
            Field nameField = reflectTest.getField("name");
            Annotation[] nameFieldAnnotations = nameField.getAnnotations();
            for (Annotation nameFieldAnnotation : nameFieldAnnotations) {
                System.out.println("nameFieldAnnotation "+nameFieldAnnotation);
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }


        //getter 和 setter
//        检查一个类所有的方法来判断哪个方法是 getters 和 setters
//        Getter 方法的名字以 get 开头，没有方法参数，返回一个值
//        Setter 方法的名字以 set 开头，有一个方法参数
//        setters 方法有可能会有返回值也有可能没有，一些 Setter 方法返回 void，一些用来设置值，有一些对象的 setter 方法在方法链中被调用
//         （译者注：这类的 setter 方法必须要有返回值），因此你不应该妄自假设 setter 方法的返回值，一切应该视情况而定
        Method[] methods1 = reflectTest.getMethods();
        for (Method method : methods1) {
            if(ReflectUtil.isGetter(method)){
                System.out.println("getter 方法 "+method);
            }else if (ReflectUtil.isSetter(method)){
                System.out.println("setter 方法 "+method);
            }
        }


        //数组反射
//        创建一个数组 Java 反射机制通过 java.lang.reflect.Array 类来创建数组
          //创建一个长度为5，类型为int的数组
         int[] arry1 = (int[]) Array.newInstance(int.class,5);
         //改变数组元素
         Array.set(arry1,0,0);
         Array.set(arry1,1,11);
         Array.set(arry1,2,22);
         //获取数组元素
        System.out.println("arry1[0] = " + Array.get(arry1, 0));
        System.out.println("arry1[1] = " + Array.get(arry1, 1));
        System.out.println("arry1[2] = " + Array.get(arry1, 2));
        //获取数组的class对象
        //1 不通过反射
        Class stringArrayClass = String[].class;
        //2 通过反射
        try {
            Class intArray = Class.forName("[I");
            // 在 JVM 中字母 I 代表 int 类型，左边的‘[’代表我想要的是一个int类型的数组，这个规则同样适用于其他的原生数据类型。

            //对于普通对象类型的数组有一点细微的不同
            Class stringArrayClassss = Class.forName("[Ljava.lang.String;");
//            注意‘[L’的右边是类名，类名的右边是一个‘;’符号。这个的含义是一个指定类型的数组。
//            需要注意的是，你不能通过 Class.forName()方法获取一个原生数据类型的 Class 对象
//            下面这两个例子都会报 ClassNotFoundException：
//            Class intClass1 = Class.forName("I");
//            Class intClass2 = Class.forName("int");
//            通常会用下面这个方法[getClass]来获取普通对象以及原生对象的 Class 对象


//            一旦你获取了类型的 Class 对象，你就有办法轻松的获取到它的数组的 Class 对象，你可以通过指定的类型创建一个空的数组，
//            然后通过这个空的数组来获取数组的 Class 对象。这样做有点讨巧，不过很有效。如下例
            Class theClass = getClass("String");
            Class stringArrayClazz1 = Array.newInstance(theClass, 0).getClass();
            System.out.println("is array: " + stringArrayClazz1.isArray()+" stringArrayClazz1 "+stringArrayClazz1);


//            这是一个特别的方式来获取指定类型的指定数组的Class对象。无需使用类名或其他方式来获取这个 Class 对象。
//            为了确保 Class 对象是不是代表一个数组，你可以使用 Class.isArray()方法来进行校验
            Class stringArrayClazz2 = Array.newInstance(String.class, 0).getClass();
            System.out.println("is array: " + stringArrayClazz2.isArray()+" stringArrayClazz2 "+stringArrayClazz2);

//            获取数组的成员类型   通过Class.getComponentType()方法获取这个数组的成员类型。成员类型就是数组存储的数据类型
            String[] strings = new String[3];
            Class stringArrayClzz3 = strings.getClass();
            Class stringArrayComponentType = stringArrayClzz3.getComponentType();
            System.out.println("stringArrayComponentType "+stringArrayComponentType);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        //todo 动态代理，范型

    }

    public static Class getClass(String className) throws ClassNotFoundException {
        if("int" .equals(className)) return int .class;
        if("long".equals(className)) return long.class;
        if("String".equals(className)) return String.class;
        if("double".equals(className)) return double.class;
        if("float".equals(className)) return float.class;
        if("boolean".equals(className)) return boolean.class;
        if("byte".equals(className)) return byte.class;

        return Class.forName(className);
    }

}



