package sst.example.lib.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;


public class DecimalFormatTest {

  //https://www.jianshu.com/p/c1dec1796062
  //DecimalFormat 是 NumberFormat 的一个具体子类，用于格式化十进制数字。
  //支持西方语言、阿拉伯语和印度语数字的支持。它还支持不同类型的数，包括整数 (123)、定点数 (123.4)、科学记数法表示的数 (1.23E4)、
  //百分数 (12%) 和金额 ($123)。所有这些内容都可以本地化。
  //
  //舍入
  //DecimalFormat 提供 RoundingMode 中定义的舍入模式进行格式化。默认情况下，它使用 RoundingMode.HALF_EVEN。

  public static void main(String[] args) {
    //pattern可以使用0或#
    //0使用
    //占位符比实际数字多
    System.out.println("0000使用====");
    System.out.println(new DecimalFormat("00.00").format(3.14567));//结果：03.15
    System.out.println(new DecimalFormat("0.000").format(3.14));//结果：3.140
    System.out.println(new DecimalFormat("00.000").format(3.14));//结果：03.140

    //占位符比实际数字少
    System.out.println(new DecimalFormat("00.00").format(13.14567));//结果：13.15
    System.out.println(new DecimalFormat("0.000").format(13.14567));//结果：13.146
    System.out.println(new DecimalFormat("0.00").format(13.14567));//结果：13.15
    //使用0占位符的时候：
    //1、 比实际数字的位数多，不足的地方用0补上。 多的舍去，四舍五入
    //2、 比实际数字的位数少：整数部分不改动，小数部分，四舍五入（其实并不是四舍五入，而是默认的RoundingMode.HALF_EVEN方式，
    //

    System.out.println("#使用========");
    //#使用
    //占位符比实际数字多
    System.out.println(new DecimalFormat("##.##").format(3.14567));//结果：3.15
    System.out.println(new DecimalFormat("#.###").format(3.14));//结果：3.14
    System.out.println(new DecimalFormat("##.###").format(3.14));//结果：3.14

    //占位符比实际数字少
    System.out.println(new DecimalFormat("#.###").format(13.145678));//结果：13.146
    System.out.println(new DecimalFormat("##.##").format(13.14567));//结果：13.15
    System.out.println(new DecimalFormat("#.##").format(13.14567));//结果：13.15

    //使用#占位符的时候：
    //1、 比实际数字的位数多，不变。
    //2、 比实际数字的位数少：整数部分不改动，小数部分，四舍五入（其实并不是四舍五入，而是默认的RoundingMode.HALF_EVEN方式，
    //实际场景：
    //比如：
    //1、格式化数字，保留两位小数，不足的小数部分用0代替，这时候，我们就可以使用："0.00"；
    //2、格式化数字，只保留有效数字，最多保留两位小数，这时候，我们就可以使用："#.##"。


    System.out.println("四舍五入 ==========");
    //当要格式化的数字超过占位符的时候，格式化的结果会进行四舍五入
    DecimalFormat format = new DecimalFormat("#.##");
    //默认 RoundingMode.HALF_EVEN
    format.setRoundingMode(RoundingMode.HALF_EVEN);
    String formatEven = format.format(13.14567);//结果：13.14
    System.out.println("formatEven "+formatEven);
    //指定舍入方式为：RoundingMode.DOWN，直接舍去格式化以外的部分
    format.setRoundingMode(RoundingMode.DOWN);
    String formatDown = format.format(13.14567);//结果：13.14
    System.out.println("formatDown "+formatDown);
    //指定舍入方式为：RoundingMode.HALF_UP，四舍五入
    format.setRoundingMode(RoundingMode.HALF_UP);
    String formatHalfUp = format.format(13.14567);//结果：13.15
    System.out.println("formatHalfUp "+formatHalfUp);

    System.out.println("测试stringAlign=============");
    System.out.println(stringAlign("11.012"));//11.01
    System.out.println(stringAlign("11.019"));//11.02
    System.out.println(stringAlign("11.1"));//11.10
    System.out.println(stringAlign("1.1"));//1.10
  }




  //保留两位自动补0
  public static String stringAlign(String str) {
    if (null == str || str.isEmpty()) {
      return "";
    }
    DecimalFormat format = new DecimalFormat("0.00");
    format.setRoundingMode(RoundingMode.HALF_UP);
    return format.format(new BigDecimal(str));
  }

}
