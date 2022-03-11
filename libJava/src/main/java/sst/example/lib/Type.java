package sst.example.lib;

//基本类型
public class Type {
    public static void main(String[] args) {

        //int的拆箱，装箱
        Integer integer = Integer.valueOf(2);
        int intValue = integer.intValue();
        //List.remove()方法，可以是index也可以是object  如果元素是int时最好进行装箱操作
    }
}
