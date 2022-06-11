package oom;

/**
 * Created By 子牙老师
 * 2021/3/1
 */
public class GCHard {

    int[] arr = new int[100 * 1024 * 1024];

    public static GCHard obj1 = new GCHard();

    public GCHard obj2 = new GCHard();

    public static void main(String[] args) {
        demo();
    }

    public static void demo() {
        GCHard obj3 = new GCHard();
    }
}