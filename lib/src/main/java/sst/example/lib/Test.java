package sst.example.lib;

public class Test {

    public static void main(String[] args) {
        String str = "a\nbnc\n";
        String[] strs = str.split("\\n");
        for (String s : strs) {
            System.out.println(s);

        }

        //todo java8 虚拟机 http://hg.openjdk.java.net/jdk8/jdk8/hotspot/file/87ee5ee27509
        // 其他类型虚拟机  https://stackoverflow.com/questions/2026093/is-jvm-open-source-code

        //TODO 语言常用概念
//        Swift 的POP  比如接口、mixin、组合模式、多继承等
//        https://bestswifter.com/pop/
    }
}
