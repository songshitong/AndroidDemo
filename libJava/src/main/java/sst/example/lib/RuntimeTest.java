package sst.example.lib;


public class RuntimeTest {
  public static void main(String[] args)  {
    //https://blog.csdn.net/u013256816/article/details/50394923
    //钩子的触发时机
    //程序正常退出
    //使用System.exit()
    //终端使用Ctrl+C触发的中断
    //系统关闭
    //OutOfMemory宕机
    //使用Kill pid命令干掉进程（注：在使用kill -9 pid时，是不会被调用的）
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.out.println("监听到JVM关闭=====");
    }));
    try {
      System.out.println("当前线程正在执行耗时任务");
      Thread.sleep(1000);
    }catch (Exception e){
      e.printStackTrace();
    }

  }
}
