//
// Created by ISS Mac on 2019-06-28.
//

#include "Student.h"
#include<iostream>
using namespace std; //标准输入输出函数，比printf更高效

//类似activity的onCreate
Student::Student(int i ,int j):i(i),j(j) //设置声明的属性
  ,k(i+j){ //实现头文件中声明的方法
  //构造方法:i(i) 是将传进的i赋值给i
  //等同于 this->i=i;     this.自动变为this->代表类的属性   shis是一个指针

   cout <<"我是输出函数"<< endl;   //<<endl是换行符号

}


//::表示作用域在Student内
int Student::getM(){
    return i+k;
}

int Student::add(){
    return i+::z; //表示当前类实例中的变量
}

void Student::setJ(int j){
   this->j=j;
}
//常量函数
//表示不会也不允许去修改类中的成员
void Student::up()const{
 //只能进行逻辑操作
 //不能改变类内属性的值，只能读取
 // this->j=j;
 cout<<"常量函数"<<endl;
}

//类似activity的onDestroy
Student::~Student(){

}
