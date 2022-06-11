//
// Created by ISS Mac on 2019-06-28.
//
#include "Student.h"
#include <iostream>
#include "SingleInstance.h"
void test(Student* stu){
  stu->j = 100;
}


//方法重载
void func(int i){

}
void func(float j){

}

//操作符重载

void testOperator(){
  Operator p1;
  p1.i=100;
  Operator p2;
  p2.i=200;
  Operator p3 = p1+p2;

  std::cout<< p3.i <<std::endl;

}






int main(){

  Student stu(10,20);
  test(&stu);

 //没有std的命名空间
 std::cout<< stu.getJ()<<std::endl;


//单例调用
 SingleInstance* instance = SingleInstance::getInstance();

 return 0;
}


