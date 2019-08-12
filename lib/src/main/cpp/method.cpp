//
// Created by ISS Mac on 2019-06-27.
#include "method.h"
#include <iostream>

// c 是面向过程的，核心载体是函数
 //  函数是一组一起执行一个任务的语句。每个c程序都至少有一个函数，即主函数main(),所有简单的程序都可以定义其他额外的函数
 //  函数调用要在函数声明后才能使用，c是从上至下加载，如果想调用在声明之前可以增加头文件
 //  java是面向对象的，整个class的mehtod会加载进内存
//
//cout 看成是console out    <<流插入运算符 运算符右侧的项目被插入到输出流中，该输出流被发送到cout以在屏幕上显示
// cout<< "a"<<"b"  发送多个项目到cout
// cout<<"a"<<endl  发送一个endl的流操作符  endl(end line)  每次cout遇到一个endl流操作符，将输出提前到下一行的开始以便继续打印

int main()
{
   func();
	func(13);
   cout << "Hello World";
   return 0;
}

   //void 代表没有参数
   int func(void){
     printf("这是函数");
     return 1;
   }

    //函数重载

int func(void* pathName,int age){
     printf("这是函数");
     return 1;
   }

   //指针函数
   //带指针的函数，本质是一个函数，函数返回类型是某一类型的指针
   //类型标识符 *函数名(参数表)     返回类型是带指针的*
   // int      *add(int a){
    //  int *p =0;
     // return p;
   //}

   //函数指针
   //指向函数的指针变量，本质是一个指针变量
   void (*f)(); //声明一个函数指针    void返回值，（）参数表     *f指针
   f = func; //将func函数的首地址赋值给指针f 赋值过程不会调用
   f();    // 调用函数func


   //java 无法改变传入函数的基本类型的值      c可以传入值的指针而改变值，基本上指针无所不能