package sst.example.lib.reflect;

import sst.example.lib.metadata.MyAnnotation;

import java.lang.annotation.Documented;
@Deprecated
@SuppressWarnings("aaa")
@MyAnnotation(name = "className",value = "ReflectTest")
public class ReflectTest extends ReflectBase implements  ReflectInterface{
    @MyAnnotation(name = "field annotation",value = "name")
    public  String name = "ReflectTest";
    public String age = "age";
    private  int id = 0;
    private String str = "str";
    public static String staticParam = "staticParam";
    @Override
    public void printSelf() {
        System.out.println("this is ReflectTest ");
    }

    private void  printIn(){
        System.out.println(" 内部打印 ");
    }

    private void  printStrIn(String str){
        System.out.println(" 内部打印 str "+str);
    }


    @MyAnnotation(name = "method annotation",value = "annotation of printMsg")
    public void printMsg(@MyAnnotation(name = "method param",value = " param msg") String msg) {
        System.out.println("msg is "+msg);
    }

    public ReflectTest() {
    }

    public ReflectTest(String name) {
        this.name = name;
    }

    public ReflectTest(String name, String age) {
        this.name = name;
        this.age = age;
    }

    private ReflectTest(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
