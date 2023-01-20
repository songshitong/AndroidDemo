package sst.example.lib.versions.java9;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public class MVarHandle {
  private int test =0;
  private static final VarHandle TEST;
  static {
    MethodHandles.Lookup l = MethodHandles.lookup();
    try {
      TEST = l.findVarHandle(MVarHandle.class,"test", int.class);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }
  public static void main(String[] args) {

  //  VarHandle 的必要性
  //  随着Java中的并发和并行编程的不断扩大，我们经常会需要对某个类的字段进行原子或有序操作，但是 JVM 对Java开发者所开放的权限非常有限。
  //  例如：如果要原子性地增加某个字段的值，到目前为止我们可以使用下面三种方式：
  //
  //  使用AtomicInteger来达到这种效果，这种间接管理方式增加了空间开销，还会导致额外的并发问题；
  //  使用原子性的FieldUpdaters，由于利用了反射机制，操作开销也会更大；
  //  使用sun.misc.Unsafe提供的JVM内置函数API，虽然这种方式比较快，但它会损害安全性和可移植性，当然在实际开发中也很少会这么做。
  //
  //  在 VarHandle 出现之前，这些潜在的问题会随着原子API的不断扩大而越来越遭。VarHandle 的出现替代了java.util.concurrent.atomic
  //和sun.misc.Unsafe的部分操作。并且提供了一系列标准的内存屏障操作，用于更加细粒度的控制内存排序。在安全性、可用性、性能上都要优于现有的API。
  //  VarHandle 可以与任何字段、数组元素或静态变量关联，支持在不同访问模型下对这些类型变量的访问，包括简单的 read/write 访问，
  //  volatile 类型的 read/write 访问，和 CAS(compare-and-swap)等。


    //在java.util.concurrent包中对变量的访问基本上都由Unsafe改为了VarHandle

    MVarHandle mVarHandle = new MVarHandle();
    TEST.compareAndSet(mVarHandle,0,1);
    System.out.println("test is "+mVarHandle.test); //test is 1
  }
}
