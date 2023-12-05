
//https://www.jianshu.com/p/2dcff3634326
//treeMap 可以按key进行排序，默认字符升序，可以自定义

//treeMap底层是红黑树
//hashMap是数组+链表

import java.util.TreeMap;
import java.util.SortedMap;

public class MTreeMap {

  public static void main(String[] args) {
    TreeMap<String,String> map = new TreeMap<String,String>();
    map.put("b","123");
    map.put("f","111");
    map.put("a","222");
    map.put("g","00");
    System.out.println("map is:"+map.toString());
    //结果  {a=222, b=123, f=111, g=00}


    String firstKey = map.firstKey();//获取集合内第一个元素
    System.out.println("firstKey is:"+firstKey.toString());  //firstKey is:a

    String lastKey =map.lastKey();//获取集合内最后一个元素
    System.out.println("lastKey is:"+lastKey.toString()); //lastKey is:g

    String lowerKey =map.lowerKey("b");//获取集合内的key小于"jiaboyan"的key
    System.out.println("lowerKey is:"+lowerKey.toString());  //lowerKey is:a


    String ceilingKey =map.ceilingKey("b");//获取集合内的key大于等于"b"的key
    System.out.println("ceilingKey is:"+ceilingKey.toString());  //ceilingKey is:b


    SortedMap<String,Integer> sortedMap =map.subMap("a","f"); //获取a-f的集合
    System.out.println("sortedMap is:"+sortedMap.toString()); //sortedMap is:{a=222, b=123}
  }
}