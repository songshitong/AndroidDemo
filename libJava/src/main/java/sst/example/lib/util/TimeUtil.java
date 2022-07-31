package sst.example.lib.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author: songshitong
 * @date: 2022/6/17
 * @description:
 */
public class TimeUtil {
  private static final int HOUR_TO_MS = 60 * 60 * 1000;
  private static final int MINUTE_TO_MS = 60 * 1000;
  private static final int SECOND_TO_MS = 1000;



  //返回当前时间 20220616162104
  public static String getCurrentTime() {
    String result = "";
    Date date = Calendar.getInstance().getTime();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
    result = sdf.format(date);
    return result;
  }

  //毫秒时长格式化为00:20:14
  public static String formatDuration2HHMMSS(long duration) {
    int hour = 0;
    int last = (int) duration;
    if (last >= HOUR_TO_MS) {
      hour = (int) (last / HOUR_TO_MS);
      last = (int) (last - hour * HOUR_TO_MS);
    }
    int minute = 0;
    if (last >= MINUTE_TO_MS) {
      minute = last / MINUTE_TO_MS;
      last = last - minute * MINUTE_TO_MS;
    }

    int second = 0;

    if (last >= SECOND_TO_MS) {
      second = last / SECOND_TO_MS;
    }
    StringBuilder sb = new StringBuilder();
    sb.append( checkNum(hour)).append(":").append( checkNum(minute)).append(":").append(checkNum(second));
    return sb.toString();
  }
  private static String checkNum(int num){
    if(num<10){
      return "0"+num;
    }else{
      return ""+num;
    }
  }

  //将一种格式转为另一种          例如20220606170500转为2022-06-06 17:05:00
  public static String timeFormat2YYYYMMDDHHMMSS(String str){
    String result = "";
    if (null == str) {
      return result;
    } else {
      DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
      try {
        Date date = df.parse(str);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        result = sdf.format(date);
      } catch (ParseException e) {
        //Logger.w("parse exception "+e.toString());
      }
      return result;
    }
  }

  public static String formatTimeStamp(long timeStamp){
    SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    String sd = sdf.format(new Date(Long.parseLong(String.valueOf(timeStamp))));      // 时间戳转换成时间
    //
    //String sd = sdf.format(Long.parseLong(String.valueOf(timeStamp)));
    //String sd = sdf.format(timeStamp)
    System.out.println("格式化结果：" + sd);
    return sd;
  }



  public static void main(String[] args) {
    System.out.println(getCurrentTime());
    System.out.println("====");
    System.out.println(formatDuration2HHMMSS(1000));
    System.out.println(formatDuration2HHMMSS(60*1000));
    System.out.println(formatDuration2HHMMSS(60*60*1000));
    System.out.println("======");
    System.out.println("格式化时间戳: "+formatTimeStamp(System.currentTimeMillis()));


  }
}
