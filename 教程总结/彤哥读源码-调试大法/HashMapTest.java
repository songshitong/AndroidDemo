package sst.example.androiddemo.feature;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

class HashMapTest {
    public static void main(String[] args) throws InterruptedException {
        HashMap<Integer,String> map = new HashMap<>(8);
        CountDownLatch countDownLatch = new CountDownLatch(2);
        ///实战2
        ///map.put(17,"17");  //插入间隔为8

        ///实战3 扩容系数0.75 第一次填入6个，第7个就会扩容
        map.put(2,"2");
        map.put(3,"3");
        map.put(4,"4");
        map.put(5,"5");
        map.put(6,"6");
        map.put(7,"7");

        new Thread(()->{
            map.put(1,"1");
            countDownLatch.countDown();
        }).start();
        new Thread(()->{
            map.put(9,"9");
            countDownLatch.countDown();
        }).start();
        ///阻塞主线程
        countDownLatch.await();
        System.out.println("map  "+map);
    }
}
