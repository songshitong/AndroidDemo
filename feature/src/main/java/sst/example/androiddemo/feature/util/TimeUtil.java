package sst.example.androiddemo.feature.util;

import android.util.Log;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeUtil {
    private static final String STRDATEFORMAT = "yyyy年MM月dd日 HH:mm";

    private static final String STRDAYFORMAT = "yyyy年MM月dd日";

    private static final String TIMEFORMAT_HM = "HH:mm";
    private static final String TIMEFORMAT_MS = "mm:ss";

    /**
     * The constant TIMEFORMAT_HM. 全局时间格式
     */
    public static final String  TIMEFORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    public static final String TAG = "TimeUtil";
    /**
     * Get date from string string. 格式化时间，返回yyyy年MM月dd日 HH:mm
     *
     * @param str the str
     * @return the string
     */
    public static String getDateFromString(String str) {
        String result = "";
        if (null == str) {
            return result;
        } else {
            DateFormat df = new SimpleDateFormat(TIMEFORMAT);
            try {
                Date date = df.parse(str);
                SimpleDateFormat sdf = new SimpleDateFormat(STRDATEFORMAT);
                result = sdf.format(date);
            } catch (ParseException e) {
                Log.w(TAG,"parse exception "+e.toString());
            }
            return result;
        }
    }

    /**
     * Get raw date from string date.
     *
     * @param str the str
     * @return the date
     */
    public static Date getRawDateFromString(String str) {
        Date date = null;
        if (null == str) {
            date = new Date();
            return date;
        } else {
            DateFormat df = new SimpleDateFormat(TIMEFORMAT);
            try {
                date = df.parse(str);
            } catch (ParseException e) {
                date = new Date();
                Log.w(TAG,"ParseException"+e.toString());
            }
            return date;
        }
    }

    /**
     * Get day from string string.  格式化为年月日
     *
     * @param str the str
     * @return the string
     */
    public static String getDayFromString(String str) {
        String result = "";
        if (null == str) {
            return result;
        } else {
            DateFormat df = new SimpleDateFormat(TIMEFORMAT);
            try {
                Date date = df.parse(str);
                SimpleDateFormat sdf = new SimpleDateFormat(STRDAYFORMAT);
                result = sdf.format(date);
            } catch (ParseException e) {
                Log.w(TAG,"ParseException"+e.toString());
            }
            return result;
        }
    }

    public static String getTimeFromString(String str) {
        String result = "";
        if (null == str) {
            return result;
        } else {
            DateFormat df = new SimpleDateFormat(TIMEFORMAT);
            try {
                Date date = df.parse(str);
                SimpleDateFormat sdf = new SimpleDateFormat(TIMEFORMAT_HM);
                result = sdf.format(date);
            } catch (ParseException e) {
                Log.w(TAG,"ParseException"+e.toString());
            }
            return result;
        }
    }

    /**
     * 将毫秒值转为00:00
     * @param milseconds
     * @return
     */
    public  static String getMsFromMilSeconds(int milseconds){
        Date date = new Date();
        date.setTime(milseconds);
        DateFormat df = new SimpleDateFormat(TIMEFORMAT_MS);
        return df.format(date);
    }

    /**
     * 获取时间戳
     * @param str
     * @return
     */
    public static long getLongTimeFromString(String str){
        DateFormat df = new SimpleDateFormat(TIMEFORMAT);
        Date date;
        try {
            date = df.parse(str);
        } catch (ParseException e) {
            Log.w(TAG,"getLongTimeFromString ParseException "+e.toString());
            date = new Date();
        }
        return date.getTime();
    }

    /**
     * Is same day boolean. 格式化年月日后，判断是不是同一天, 判断天数是否一致
     *
     * @param day1 the day 1
     * @param day2 the day 2
     * @return the boolean
     */
    public static boolean isSameDay(String day1, String day2) {
        return day1.equalsIgnoreCase(day2);
    }

    public static boolean isSameDayWithFormat(String day1, String day2) {
        return isSameDay(getDayFromString(day1), getDayFromString(day2));
    }



    /**
     * 将秒转换为小时
     * @param second
     * @return
     */
    public static String second2Hour(String second) {
        int s = Integer.valueOf(second);

        return String.valueOf(s/3600);
    }

    /**
     * 将分钟转换为小时
     * @param second
     * @return
     */
    public static String min2Hour(String second) {
        int s = Integer.valueOf(second);

        return String.valueOf(s/60);
    }
}
