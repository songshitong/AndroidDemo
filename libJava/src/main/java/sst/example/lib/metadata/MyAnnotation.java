package sst.example.lib.metadata;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
//可以使用在方法，参数
@Target({ElementType.TYPE,ElementType.METHOD,ElementType.PARAMETER,ElementType.FIELD})
public @interface MyAnnotation {
    //注解的值必须常用的字面值  public static final String a="aa";  由两个常量拼起来的不行，静态代码块初始化的不行，
    //
    public String name();
    //需要一个值
    public String value();
    //数组
    //String[] array() ;
    //一个值，存在默认，可以不写
    String dValue() default "";
}
