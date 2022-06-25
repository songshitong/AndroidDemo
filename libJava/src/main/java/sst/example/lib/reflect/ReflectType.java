package sst.example.lib.reflect;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Objects;

public class ReflectType {
    //反射类型
    //Type 接口表示Java中的类型，这些类型包括原始类型、参数化类型、数组类型、类型变量和基本类型
    //Type 及其组件用于通过反射来描述Java各种类型，比如描述泛型的定义，获取泛型参数个数、类型、泛型通配符的上下边界等等

    //Type的子类
    //ParameterizedType: 表示参数化类型，即整个泛型的定义，如 List<String> 可以用 ParameterizedType 来描述；
    //TypeVariable: 表示类型变量，即泛型申明中的具体类型，比如: List<E> 用参数化类型来描述，而其中的 E 就用 TypeVariable 来描述；
    //WildcardType: 表示泛型通配符类型，即泛型申明中带 ? 通配符，比如：List<? extends Number> 中 ? extends Number 就用 WildcardType 来描述；
    //GenericArrayType: 表示泛型数组，比如申明一个泛型数组的域 T[] array，它就用 GenericArrayType 来描述；
    //Class: 表示运行时的类或接口
    //class 常用方法
    //c.isPrimitive(); //判断c是否为基本数据类型
    // a.isAssignableFrom(b);  //判断a是不是b的父类或接口，相同的类型   instanceof()方法是从实例继承的角度去判断
    // c.getGenericType(); //得到泛型类型


    //type转为class  或者getRawType()
//    if (type instanceof Class) {
//        Class<?> clazz = (Class<?>) type;
//    }
    String getMethodReturnType(Method method){
        Type type = method.getGenericReturnType();
        if (type instanceof Class<?>) {
            return type.getClass().getName();
        }
        if (type instanceof ParameterizedType) {
            return type.getClass().getName();
        }
        if (type instanceof GenericArrayType) {
            return type.getClass().getName();
        }
        if (type instanceof TypeVariable) {
            return type.getClass().getName();
        }
        if (type instanceof WildcardType) {
            return type.getClass().getName();
        }
        return  "";
    }


    static class Parent{

    }
    static class Child extends Parent{

    }

    public static void main(String[] args) {
        Parent parent = new Parent();
        Child child = new Child();
        System.out.println("isAssignableFrom ====");
        System.out.println(parent.getClass().isAssignableFrom(child.getClass()));
        System.out.println(Parent.class.isAssignableFrom(child.getClass()));
        System.out.println(Parent.class.getClass().isAssignableFrom(child.getClass()));
        System.out.println(Parent.class.isAssignableFrom(Child.class));
        System.out.println(Parent.class.getClass().isAssignableFrom(child.getClass().getClass()));
        //对于type  a.isAssignableFrom((Class<?>type))
        System.out.println("parent.getClass() "+parent.getClass());
        System.out.println("Parent.class "+Parent.class);
        System.out.println("Parent.class.getClass "+Parent.class.getClass());
    }


    static Class<?> getRawType(Type type) {
        Objects.requireNonNull(type, "type == null");

        if (type instanceof Class<?>) {
            // Type is a normal class.
            return (Class<?>) type;
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;

            // I'm not exactly sure why getRawType() returns Type instead of Class. Neal isn't either but
            // suspects some pathological case related to nested classes exists.
            Type rawType = parameterizedType.getRawType();
            if (!(rawType instanceof Class)) throw new IllegalArgumentException();
            return (Class<?>) rawType;
        }
        if (type instanceof GenericArrayType) {
            Type componentType = ((GenericArrayType) type).getGenericComponentType();
            return Array.newInstance(getRawType(componentType), 0).getClass();
        }
        if (type instanceof TypeVariable) {
            // We could use the variable's bounds, but that won't work if there are multiple. Having a raw
            // type that's more general than necessary is okay.
            return Object.class;
        }
        if (type instanceof WildcardType) {
            return getRawType(((WildcardType) type).getUpperBounds()[0]);
        }
         return null;
    }
}
