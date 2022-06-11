package oom;

import java.util.ArrayList;
import java.util.List;

/**
 * JVM花费了98%的时间进行垃圾回收，而只得到2%可用的内存，频繁的进行内存回收(最起码已经进行了5次连续的垃圾回收)，
 * JVM就会曝出ava.lang.OutOfMemoryError: GC overhead limit exceeded错误
 */
public class GCOverheadTest {

    public static void main(String[] args) {
        List<String> list = new ArrayList<String>();

        Integer val = 0;

        for (;;) {
            list.add((val++).toString());
        }
    }
}
