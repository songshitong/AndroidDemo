package sst.example.lib;

import java.util.Locale;

public class StringTest {

    public static void main(String[] args) {
        String str = "a\nbnc\n";
        String[] strs = str.split("\\n");
        for (String s : strs) {
            System.out.println(s);

        }
        //int 转string
        int a = 4;
        String b = String.valueOf(a);
        //string转char[]
        char[] array = b.toCharArray();
        for (int i = 0; i < array.length; i++) {
            System.out.println(array[i]);
        }

        //String.format
        String formatted = String.format(Locale.getDefault(),"%s今年%d岁。", "小李", 30); // "小李今年30岁。"
        System.out.println(formatted);
        //%d整数  %s/%S字符串     %f浮点数   https://segmentfault.com/a/1190000013654676
        //补0   个位数字，格式为2个，不够前面补0
        String str0 = String.format(Locale.getDefault(),"%05d", 123); //补齐到5位数字，不足的用0补
        System.out.println(str0); //输出  00123

        //todo java8 虚拟机 http://hg.openjdk.java.net/jdk8/jdk8/hotspot/file/87ee5ee27509
        // 其他类型虚拟机  https://stackoverflow.com/questions/2026093/is-jvm-open-source-code

//        Swift 的POP  比如接口、mixin、组合模式、多继承等
//        https://bestswifter.com/pop/

    }
}
