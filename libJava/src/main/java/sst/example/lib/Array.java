package sst.example.lib;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.function.ToIntFunction;

public class Array {
    //二维数组 http://c.biancheng.net/view/916.html
    //在 Java 中二维数组被看作数组的数组，即二维数组为一个特殊的一维数组，其每个元素又是一个一维数组。Java 并不直接支持二维数组，
    // 但是允许定义数组元素是一维数组的一维数组，以达到同样的效果。声明二维数组的语法如下：
    //type array[][];
    //type[][] array;
    //int[][] temp={{1,2},{3,4}};    2行2列的数组
    // 获取行数 temp.length;  获取列数temp[0].length
    // 获取第一行第二个temp[0][2]


    public static void main(String[] args) {
        int[] a1 =new int[]{1,2,3};
        for(int i=0;i<a1.length;i++){
           for (int j=0;j<i;j++){
               System.out.println("j "+j);
               //退出内循环
               if(j==1){
                   break;
               }
           }
        }

        out:
        for(int i=0;i<a1.length;i++){
            for (int j=0;j<i;j++){
                System.out.println("j "+j);
                //退出外循环
                break out;
            }
        }

        byte[] barr = new byte[]{1,0,1};
        System.out.println("barr "+barr);


        PriorityQueue<Integer> queue = new PriorityQueue<>(
            (o1, o2) -> o2 - o1);
        queue.add(10);
        queue.add(5);

        System.out.println("queue1 "+queue.poll());
        System.out.println("queue2 "+queue.poll());

    }


}
