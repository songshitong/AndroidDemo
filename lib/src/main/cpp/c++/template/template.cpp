//
// Created by ISS Mac on 2019-06-28.
//

#include "template.h"
#include <iostream>
//模板编程，类似Java的泛型
//函数模板  Java的泛型方法
//java  void T func(T t);

template <typename T> //<class T>也可以
T compare(T i,T,j){
  return i>j?i:j;
}

//类模板    Java的泛型类

template <class T,class E>

class Q {
  T test(T t,E e){
    return t+e;
  }
}

int main(){


  Q<int,int> q;
  std::cout<<q.test(1,2)<<std::endl;

  return 0
}
