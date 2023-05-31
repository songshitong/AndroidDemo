package sst.example.lib.collection;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MList {
  public static void main(String[] args) {
    List<Person> list = Arrays.asList(
        new Person("John", "Smith"),
        new Person("Anna", "Martinez"),
        new Person("Paul", "Watson ")
    );

    //将list的 item中某一部分 通过连接符连接为string
    String joinedFirstNames = list.stream()
        .map(Person::getFirstName)
        .collect(Collectors.joining(", ")); // "John, Anna, Paul"
    //Collectors 常用收集器，定义了一系列行为
    System.out.println("joinedFirstNames "+joinedFirstNames);
  }

  static class Person{
    private String firstName;
    private String secondName;

    public Person(String firstName, String secondName) {
      this.firstName = firstName;
      this.secondName = secondName;
    }

    public String getFirstName() {
      return firstName;
    }

    public void setFirstName(String firstName) {
      this.firstName = firstName;
    }

    public String getSecondName() {
      return secondName;
    }

    public void setSecondName(String secondName) {
      this.secondName = secondName;
    }
  }
}
