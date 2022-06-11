package oom;

public class StackOverFlowTest {

    private int val = 0;

    public void test() {
        val++;

        test();
    }

    public static void main(String[] args) {
        StackOverFlowTest test = new StackOverFlowTest();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            test.test();
        } catch (Throwable t) {
            t.printStackTrace();

            System.out.println(test.val);
        }
    }
}
