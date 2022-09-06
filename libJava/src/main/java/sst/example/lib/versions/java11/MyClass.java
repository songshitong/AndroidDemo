package sst.example.lib.versions.java11;


public class MyClass {
  public static void main(String[] args) {
    //在lambda中支持使用var
    Thread.UncaughtExceptionHandler uncaughtExceptionHandler =
        (var x, var y) -> System.out.println(x + "");
  }
}
