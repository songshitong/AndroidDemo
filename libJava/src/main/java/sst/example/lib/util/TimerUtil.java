package sst.example.lib.util;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author: songshitong
 * @date: 2022/7/29
 * @description:
 */
public class TimerUtil {

  //todo
  //https://cloud.tencent.com/developer/article/1038304
  public static void retryTask(Runnable runnable,int tryCount,long delay,long period){
    final int[] count = { 0 };
    final Timer[] timer = { new Timer() };
    timer[0].schedule(new TimerTask() {
      @Override public void run() {
        runnable.run();
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
    retryTask(() -> System.out.println("执行次数："+ count.getAndIncrement() +" 时间 "+new Date().toString()),5,0,1000);
  }
}
