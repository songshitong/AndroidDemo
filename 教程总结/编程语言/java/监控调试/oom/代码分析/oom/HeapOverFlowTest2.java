package oom;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class HeapOverFlowTest2 {

    int[] intArr = new int[1024 * 128];

    public static void main(String[] args) {
        run();
    }

    public static void run() {
        List<HeapOverFlowTest2> objs = new ArrayList<>();

        for (;;) {
            try {
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            objs.add(new HeapOverFlowTest2());
        }
    }
}
