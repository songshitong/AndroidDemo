//
// Created by ISS Mac on 2019-06-28.
//
#include <iostream>

//namespace 解决类引入冲突，方法冲突

//c++ 引用vs指针
 // 1 不存在空引用，引用必须连接到一块合法的内存
 // 2  一旦引用被初始化为一个对象，就不能被指向另一个对象；指针可以在任何时候指向另一个对象
 // 3  引用必须在创建时被初始化；指针可以在任何时候被初始化
using namespace std;
namespace first_namespace{
 void func(){
   cout<<"func first_namespace"<<endl;
 }
}

namespace second_namespace{
 void func(){
   cout<<"func second_namespace"<<endl;
 }
}








int main(){

  //调用第一个命名空间的函数
  first_namespace::func();
  //调用的第二命名空间的函数
  second_namespace::func();


  int i =17;

  //指针 type *
  int *p = &i;
  cout<<p<<endl;
    cout<<*p<<endl;

 //引用类型 type&
  int& r = i;
  cout<<r<<endl;
      cout<<*r<<endl;
  return 0;
}
