package sst.example.lib.versions.java10;

import java.util.ArrayList;

public class MyClass {
  public static void main(String[] args) {
    //局部变量类型推断
    //限制：
    //只能用于局部变量上
    //声明时必须初始化
    //不能用作方法参数
    //不能在 Lambda 表达式中使用
    var list1 = new ArrayList<String>();
  }
}
