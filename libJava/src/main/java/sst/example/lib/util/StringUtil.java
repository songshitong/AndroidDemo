package sst.example.lib.util;

import java.util.regex.Pattern;

public class StringUtil {

  //https://github.com/Blankj/AndroidUtilCode/blob/66a4c0488ca6da273098058d70dabb0fe9b9bd8d/lib/utilcode/src/main/java/com/blankj/utilcode/constant/RegexConstants.java
  public static final String REGEX_MOBILE_EXACT  = "^((13[0-9])|(14[579])|(15[0-35-9])|(16[2567])|(17[0-35-8])|(18[0-9])|(19[0-35-9]))\\d{8}$";

  public static boolean isMobile(String phone){
    return isMatch(REGEX_MOBILE_EXACT,phone);
  }

  public static boolean isMatch(final String regex, final CharSequence input) {
    return input != null && input.length() > 0 && Pattern.matches(regex, input);
  }

  //https://www.cnblogs.com/ae6623/p/4757099.html
  //正则表达式中，替换字符串，括号的意思是分组，在replace()方法中，参数二中可以使用$n(n为数字)来依次引用模式串中用括号定义的字串。
  //    "(\d{3})\d{4}(\d{4})", "$1***$2"的这个意思就是用括号，分为(前3个数字)中间4个数字(最后4个数字)替换为(第一组数值，保持不变$1)(中间为)(第二组数值，保持不变$2)
  public static String hideMobileMiddle(String phone){
    return phone.replaceAll("(\\d{3})\\d{4}(\\d{4})","$1****$2");
  }
}
