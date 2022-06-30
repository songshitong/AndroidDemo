package sst.example.lib.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class CalendarTest {
  public static void main(String[] args) {
    //https://baijiahao.baidu.com/s?id=1720079457547968191&wfr=spider&for=pc&searchword=Java%20calendar%E4%BD%BF%E7%94%A8
    //设置时区
    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
    //calendar.setTime(new Date());
    //如何设置时间： month – the month between 0-11.
    //calendar.set(2021, 11, 25);
    //calendar.set(Calendar.DAY_OF_MONTH, 1)



    //获取calender对应的时间
    System.out.println("当前时间: " + calendar.getTime());
    System.out.println("当前时间戳: " + calendar.getTimeInMillis());
    // 1、取得本地时间：
    //java.util.Calendar cal = java.util.Calendar.getInstance();
    //// 2、取得时间偏移量：
    //int zoneOffset = cal.get(java.util.Calendar.ZONE_OFFSET);
    //// 3、取得夏令时差：
    //int dstOffset = cal.get(java.util.Calendar.DST_OFFSET);
    //// 4、从本地时间里扣除这些差量，即可以取得UTC时间：
    //cal.add(java.util.Calendar.MILLISECOND, -(zoneOffset + dstOffset));

    System.out.println("年份: " + calendar.getWeekYear() + " 或者: " + calendar.get(Calendar.YEAR));

    //月份从0-11
    System.out.println("月份: " + calendar.get(Calendar.MONTH));

    System.out.println("今年现在的周数: " + calendar.getWeeksInWeekYear());

    System.out.println("今天星期几: " + calendar.get(Calendar.DAY_OF_WEEK));

    System.out.println("今天是月份的多少天: " + calendar.get(Calendar.DAY_OF_MONTH));
    System.out.println("今天是今年的多少天: " + calendar.get(Calendar.DAY_OF_YEAR));

    //日期加减
    calendar.add(Calendar.DAY_OF_MONTH, -3);
    System.out.println("3天前："+calendar.get(Calendar.DAY_OF_MONTH));

    //比较是否是同一天
    //1
    System.out.println("是否是同一天："+(calendar.get(Calendar.DAY_OF_YEAR) == ((Calendar)calendar.clone()).get(Calendar.DAY_OF_YEAR)));
    //2
    SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
    //return fmt.format(date1).equals(fmt.format(date2))
    //3  使用 Java 8 的新日期时间 API，我们可以使用LocalDate对象。这是一个不可变对象，表示没有时间的日期
    //LocalDate localDate1 = date1.toInstant().atZone(ZoneId.systemDefault())
    //    .toLocalDate();
    //LocalDate localDate2 = date2.toInstant().atZone(ZoneId.systemDefault())
    //    .toLocalDate();
    //boolean isSameDay = localDate1.isEqual(localDate2);
    //System.out.printf("时间 %s 与 %s 是否为同一天 %s \n", date1, date2, isSameDay);
    //4 isSameDay()

    //比较时间前后
    //不能判断是否同一天，这种不靠谱，如果设置了小时以下的值就不对了
    System.out.println(calendar.compareTo((Calendar) calendar.clone()));


    //复制一份calendar 写日历时表示多个时间可以用
    Calendar newC = (Calendar) calendar.clone();

    //GregorianCalendar 子类 GregorianCalendar 则实现了 Calendar 中的相关抽象方法
    Calendar gregorianCalendar = GregorianCalendar.getInstance();

    testDate();
  }

  private static void testDate() {
    //the year minus 1900.
    //month   the month between 0-11.
    //date    the day of the month between 1-31.
    Date date = new Date(2022-1900,5,5);
  }

  public static boolean isSameDay(Date a, Date b) {
    if (a == null || b == null) return false;
    //date.getDay()是一周的第几天  getDate()是一个月第几天
    return a.getYear() == b.getYear() && a.getMonth() == b.getMonth() && a.getDate() == b.getDate();
  }
}
