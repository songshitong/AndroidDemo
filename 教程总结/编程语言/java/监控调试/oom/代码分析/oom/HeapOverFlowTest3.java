package oom;

import java.util.ArrayList;
import java.util.List;

public class HeapOverFlowTest3 {

    char c = 10;
    int i = 20;

    public static void main(String[] args) {
        System.out.println("hihello");
    }

    public static void run() {
        List<HeapOverFlowTest3> objs = new ArrayList<>();

        objs.add(new HeapOverFlowTest3());
    }
}
