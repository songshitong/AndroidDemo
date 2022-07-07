package sst.example.lib.util;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class CalendarTest {
  public static void main(String[] args) {
    //https://baijiahao.baidu.com/s?id=1720079457547968191&wfr=spider&for=pc&searchword=Java%20calendar%E4%BD%BF%E7%94%A8
    //设置时区
    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
    //calendar.setTime(new Date());
    //如何设置时间： month – the month between 0-11.
    //calendar.set(2021, 11, 25);
    //calendar.set(Calendar.DAY_OF_MONTH, 1) //明天
    //calendar.set(Calendar.DAY_OF_MONTH, -1) //昨天



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
    System.out.println("3天前："+calendar.getTime());

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

    //相差几天 将相差的时间段转为天数
    //1
    Calendar calendar1 = (Calendar) Calendar.getInstance().clone();
    calendar1.set(2022-1900, 1,3,10,10);
    Calendar calendar2 = (Calendar) Calendar.getInstance().clone();
    calendar2.set(2022-1900, 1,6,8,8); //hour改为12结果就是3天 hour为8就是2天
    System.out.println("相差几天 "+ChronoUnit.DAYS.between(calendar1.toInstant(),calendar2.toInstant()));
    //2
    LocalDateTime date1 = LocalDateTime.from(convertCalendar(calendar1));
    LocalDateTime date2 = LocalDateTime.from(convertCalendar(calendar2));
    long daysBetween = Duration.between(date1, date2).toDays();
    System.out.println ("Days: " + daysBetween);
    //3
    long days = TimeUnit.DAYS.convert(calendar1.getTimeInMillis()-calendar2.getTimeInMillis(),TimeUnit.MILLISECONDS);
    System.out.println ("Days timeunit: " + days);
    //4
    float betweenTime = (float) (calendar1.getTimeInMillis() - calendar2.getTimeInMillis()) / (1000 * 60 * 60 * 24);
    System.out.println("betweenTime "+betweenTime);
    System.out.println("betweenTime round"+Math.round(betweenTime));
    System.out.println("betweenTime floor"+Math.floor(betweenTime));
    System.out.println("betweenTime floor"+ (int)betweenTime);
    System.out.println("2.2 "+Math.ceil(2.2f));
    //4 转为数字 不对啊  20220601-20220529  相差不为1


    //复制一份calendar 写日历时表示多个时间可以用
    Calendar newC = (Calendar) calendar.clone();

    //GregorianCalendar 子类 GregorianCalendar 则实现了 Calendar 中的相关抽象方法
    Calendar gregorianCalendar = GregorianCalendar.getInstance();

    testDate();
    testInstant();
    testLocalTime();
  }

  static LocalDateTime convertCalendar(Calendar calendar){
    return LocalDateTime.ofInstant(calendar.toInstant(),calendar.getTimeZone().toZoneId());
  }

  private static void testLocalTime() {
    //LocalDateTime  https://blog.csdn.net/fragrant_no1/article/details/83988042
    //它表示的是不带时区的 日期及时间，替换之前的Calendar
    //两个人都在2013年7月2日11点出生，第一个人是在英国出生，而第二个是在加尼福利亚，如果我们问他们是在什么时候出生的话，
    // 则他们看上去都是 在同样的时间出生（就是LocalDateTime所表达的），但如果我们根据时间线（如格林威治时间线）去仔细考察，
    // 则会发现在出生的人会比在英国出生的人稍微晚几个小时（这就是Instant所表达的概念，并且要将其转换为UTC格式的时间）
    LocalDateTime localDateTime = LocalDateTime.now();
    System.out.println(localDateTime+" localDateTime1");
    //当前时间加上5小时，分钟等一样的用法，支持链式编程
    LocalDateTime localDateTime1 = localDateTime.plusHours(5);
    System.out.println(localDateTime1+" localDateTime1");
    //当前时间加上5小时，分钟等一样的用法，支持链式编程 但是这里localtime只是时间，不展示年月日，只展示如：15:26:50.398 时分秒毫秒
    LocalTime localDateTime2 = localDateTime.toLocalTime().plusHours(5);
    //当前时间加上5天，只展示年月日，不展示时分秒毫秒,下面是两种写法，都可以
    LocalDate localDate = localDateTime.toLocalDate().plusDays(5);
    System.out.println(localDateTime2+" localDateTime2 "+localDate+ " localDate");
    LocalDate plus = localDateTime.toLocalDate().plus(Period.ofDays(5));
    System.out.println(plus+"  plus");

  }

  private static void testInstant() {
    //https://blog.csdn.net/fragrant_no1/article/details/83988042
   // Instant 类代表的是某个时间（有点像 java.util.Date），准确的说是：”是不带时区的即时时间点“，它是精确到纳秒的
    // （而不是象旧版本的Date精确到毫秒）
    Instant now = Instant.now();//Instant.now()使用等是UTC时间
    Date nowDate = Date.from(now);
    Instant nowInstant = nowDate.toInstant();
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
