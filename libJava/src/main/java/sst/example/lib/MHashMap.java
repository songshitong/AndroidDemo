package sst.example.lib;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MHashMap {

    public static void main(String[] args) {

        Map<Integer, Integer> map = new HashMap<Integer, Integer>();
        map.put(1, 10);
        map.put(2, 20);

        //遍历entrySet
        // Iterating entries using a For Each loop
        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
            System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
        }

        //遍历 iterator
        Iterator<Map.Entry<Integer, Integer>> entries = map.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<Integer, Integer> entry = entries.next();
            System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
        }

        //lambda遍历  android中部分版本不支持
        map.forEach((k, v) -> System.out.println("key: " + k + " value:" + v));


        //遍历 key和value
        // 迭代键
        for (Integer key : map.keySet()) {
            System.out.println("Key = " + key);
        }

        // 迭代值
        for (Integer value : map.values()) {
            System.out.println("Value = " + value);
        }



    }
}
