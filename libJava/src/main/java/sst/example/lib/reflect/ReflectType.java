package sst.example.lib.reflect;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

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
    //type转为class
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
}
