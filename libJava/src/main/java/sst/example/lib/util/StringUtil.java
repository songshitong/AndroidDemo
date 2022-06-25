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
}
