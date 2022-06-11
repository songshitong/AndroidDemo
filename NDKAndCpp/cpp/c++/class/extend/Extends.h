//
// Created by ISS Mac on 2019-06-28.
//

#ifndef ANDROIDDEMO_EXTENDS_H
#define ANDROIDDEMO_EXTENDS_H
#include <iostream>
class Parent{
  public:
   void test(){
     cout<<"parent test"<<endl;
   }
   //虚函数
   virtual  void test1(){
        cout<<"parent test1"<<endl;
      }
   //纯虚函数  表示test2在当前类没有实现，子类必须实现，类似Java的abstract
   virtual void  test2() =0;

}

class Parent1{
  public:
   void test(){
     cout<<"parent1 test"<<endl;

   }

   void test1(){
           cout<<"parent1 test1"<<endl;
         }
}

//c++ 支持多继承
//公有继承
class Child :public Parent,Parent1{
   public:
     void test(){
            cout<<"Child test"<<endl;
     }
     void test1(){
             cout<<"Child test1"<<endl;
           }

}

//私有继承
class PrivateChild : private Parent{

}

#endif //ANDROIDDEMO_EXTENDS_H
