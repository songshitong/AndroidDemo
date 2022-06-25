package sst.example.lib.versions.java8;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

public class MOptional {
  public static void main(String[] args) {
    //Optional 可以减少空指针，支持函数式编程
    //OptionalDouble
    //OptionalInt
    //OptionalLong


    //https://www.itwanger.com/life/2020/03/10/java-Optional.html
    //创建 Optional 对象
    Optional<String> empty = Optional.empty();
    Optional<String> opt = Optional.of("沉默王二");//of的值不能为null，会抛出异常
    Optional<String> optOrNull = Optional.ofNullable(null);//可空对象，传入null时使用empty对象

    //获取值  value为null抛出异常
    opt.get();
    //设置（获取）默认值
    //orElse在值为null时返回默认值
    String name = (String) Optional.ofNullable(null).orElse("沉默王二");
    System.out.println(name); // 输出：沉默王二
    //orElseGet
    System.out.println("orElse");
    Optional.ofNullable(name).orElse(getDefaultValue());
    System.out.println("orElseGet");
    Optional.ofNullable(name).orElseGet(MOptional::getDefaultValue);
    //orElseGet在null时执行对应的::方法，不为null时不执行
    String nullStr = null;
    System.out.println("orElse");
    Optional.ofNullable(nullStr).orElse(getDefaultValue());
    System.out.println("orElseGet");
    Optional.ofNullable(nullStr).orElseGet(MOptional::getDefaultValue);


    //判断值是否存在  判断规则是值存在value != null;
    System.out.println(opt.isPresent()); // 输出：true
    System.out.println(optOrNull.isPresent()); // 输出：false
    //Java 11 后还可以通过方法 isEmpty() 判断与 isPresent() 相反的结果。判断为this.value == null
    //System.out.println(opt.isEmpty()); // 输出：true
    //System.out.println(optOrNull.isEmpty()); // 输出：false

    //非空表达式
    opt.ifPresent(str -> System.out.println(str.length()));
    //等价于
    if(opt.isPresent()){
      System.out.println(opt.get().length());
    }
    //Java 9 后还可以通过方法 ifPresentOrElse(action, emptyAction) 执行两种结果，非空时执行 action，空时执行 emptyAction。
    //opt.ifPresentOrElse(str -> System.out.println(str.length()), () -> System.out.println("为空"));


    //过滤值
    opt.filter(value->value.length()>4).isPresent();

    //转换值
    Optional<Integer> inte =  opt.map(String::length);

    Optional<School> school = Optional.of(new School());
    Optional<Student> student = school.get().student;
    //student.ifPresentOrElse(student1->{
    //  System.out.println(student1.name);
    //},()->{
    //  System.out.println("student is null");
    //});

  }

  public static String getDefaultValue() {
    System.out.println("getDefaultValue");
    return "沉默王二";
  }

  static class School{
    String name;
    Optional<Student> student;

    public School() {
    }
  }

  class Student{
    String name;

    public Student() {
    }
  }
}
