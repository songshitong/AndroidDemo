package sst.example.lib.util;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * @author: songshitong
 * @date: 2022/7/29
 * @description:
 */
public class TimerUtil {

  //
  //https://cloud.tencent.com/developer/article/1038304
  //实例：
  //    XGTimeUtil.retryTask((timer) -> {
  //      if (true) {
  //        dosomething();
  //      }else{
  //        timer.cancel();
  //      }
  //    }, 10, 0, 3000);
  public static void retryTask(Consumer<Timer> consumer,int tryCount,long delay,long period){
    final int[] count = { 0 };
    final Timer[] timer = { new Timer() };
    timer[0].schedule(new TimerTask() {
      @Override public void run() {
        consumer.accept(timer[0]);
        count[0]++;
        if(count[0]>=tryCount){
          timer[0].cancel();
          timer[0] =null;
        }
      }
    }, delay, period);
  }

  public static void main(String[] args) {
    AtomicInteger count= new AtomicInteger();
    retryTask((timer) -> System.out.println("执行次数："+ count.getAndIncrement() +" 时间 "+ new Date()),5,0,1000);
  }
}
