package sst.example.lib;

public class Test {

    public static void main(String[] args) {
        String str = "a\nbnc\n";
        String[] strs = str.split("\\n");
        for (String s : strs) {
            System.out.println(s);

        }
    }
}
