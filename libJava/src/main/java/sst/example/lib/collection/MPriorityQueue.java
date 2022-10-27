package sst.example.lib.collection;

import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

/**
 * @author: songshitong
 * @date: 2022/10/3
 * @description:
 */
public class MPriorityQueue {
  public static void main(String[] args) {
    PriorityQueue<User> userPQueue = new PriorityQueue<>(new Comparator<User>() {
      @Override public int compare(User user1, User user2) {
        return user1.priority - user2.priority;
      }
    });
    userPQueue.add(new User(3, "3"));
    userPQueue.add(new User(1, "1"));
    userPQueue.add(new User(2, "2"));

    System.out.println("按照大顶堆输出 =====");
    userPQueue.forEach((user -> System.out.println(user.name)));

    System.out.println("按照优先级升序输出 =====");
    Iterator<User> itr = userPQueue.iterator();
    while(itr.hasNext()){
      User user = userPQueue.poll();
      System.out.println(user.name);
    }

  }

  static class User {
    int priority = 0;
    String name = "";

    public User(int priority, String name) {
      this.priority = priority;
      this.name = name;
    }
  }
}
