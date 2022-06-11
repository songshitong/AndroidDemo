package oom;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class HeapOverFlowTest {

    int[] intArr = new int[1024 * 128];

    public static void main(String[] args) {
        List<HeapOverFlowTest> objs = new ArrayList<>();

        for (;;) {
            try {
                TimeUnit.MILLISECONDS.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            objs.add(new HeapOverFlowTest());
        }
    }
}
