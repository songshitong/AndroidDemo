//
// Created by ISS Mac on 2019-06-28.
//

#include "Extends.h"

int main(){
  Child child;
  child.test();

  //静态多态 调用parent的test不调用child的test     编译时就确定了类型，child的类型是parent，而不是到运行时确定
  Parent* child = new Child();
  child->test();

  //动态多态
  //虚函数 virtual 运行时确定类型
  //构造方法永远不要设为虚函数
  //析构方法一般声明为虚函数，需要释放实际运行时的内存
   Parent* child1 = new Child();
    child1->test1();

  return 0;
}
