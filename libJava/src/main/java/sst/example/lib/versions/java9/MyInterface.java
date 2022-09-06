package sst.example.lib.versions.java9;

//https://pdai.tech/md/java/java8up/java9-11.html#jdk9---%E5%85%81%E8%AE%B8%E5%9C%A8%E6%8E%A5%E5%8F%A3%E4%B8%AD%E4%BD%BF%E7%94%A8%E7%A7%81%E6%9C%89%E6%96%B9%E6%B3%95
public interface MyInterface {
  //接口支持私有方法
  private void sayHi(){
    System.out.println("hi");
  }
  default void sayHiDefault() {
    sayHi();
  }
}
