package sst.example.androiddemo.feature.DataStructureAlgorithm.lru;

import java.util.LinkedHashMap;

public class MyLRUCache {
//    LinkedHashMap提供了按照访问顺序排序的方案，内部也是使用HashMap+双向链表
    LinkedHashMap linkedHashMap;
    public MyLRUCache(int capacity) {
        linkedHashMap = new LinkedHashMap(capacity){
            @Override
            protected boolean removeEldestEntry(Entry eldest) {
                return size() > capacity;
            }
        };
    }

    public int get(int key) {
        int result;
        Object obj = linkedHashMap.get(key);
        if( null ==obj ){
            result = -1;
        }else{
            result = (int) obj;
        }
        return  result;
    }

    public void put(int key, int value) {
          linkedHashMap.put(key,value);
    }

    public static void main(String[] args) {
        MyLRUCache cache = new MyLRUCache(2);
        cache.put(1, 1);
        cache.put(2, 2);
        System.out.println("args = [" + cache.get(1) + "]");      // returns 1
        cache.put(3, 3);    // evicts key 2
        System.out.println("args = [" + cache.get(2) + "]");       // returns -1 (not found)
        cache.put(4, 4);    // evicts key 1
        System.out.println("args = [" + cache.get(1) + "]");// returns -1 (not found)
        System.out.println("args = [" + cache.get(3) + "]");     // returns 3
        System.out.println("args = [" + cache.get(4) + "]");       // returns 4
    }
}
