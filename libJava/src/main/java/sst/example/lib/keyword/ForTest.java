package sst.example.lib.keyword;

public class ForTest {
    public static void main(String[] args) {
        //多个条件
        int a = 2,b = 5; // 逗号运算符一般是用来将几个条件彼此分开
        // for循环中多条件限定
        for (int i = 0, j = 0; i < a && j < b; i++, j++) { // i<a并且j<b同时成立才循环
            System.out.println("i:" + i + " - j:" + j);
        }
    }
}
