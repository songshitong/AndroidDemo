//
// Created by ISS Mac on 2019-06-28.
//
//容器  stl 标准模板库
  //1 序列式      元素排列顺序与元素本身没有关系，由添加顺序决定
     vector,list,dequeue,queue,stack,priority queue
  //2 关联式
     set,map
#include <vector>
#include <set>
#include <map>
#include <iostream>
using namespace std
int main(){
   vector<int> vec_1;

   //声明一个元素空间
   vector<int> vec_2(5);

   //声明5个元素，每个都是2
      vector<int> vec_3(5,2);

  // 以另外的向量为基础
   vector<int> vec_4(vec_1);

   //添加元素
   vec_4.push_back(10);
   //通过下表获取元素
   vec_4[0];

   //获取队首和队尾
   vec_4.front();
   vec_4.back();

   //清楚向量容器
   vec_4.clear();


   //set
   set<int> set_1= {1,2,3,4};

   //set 大小
   set_1.size();
   //插入元素
   pair<set<int>::iterator,bool> _pair = set_1.insert(5);

   //迭代器
   set<int>::iterator iterator = set_1.begin();

   set_1.end();//指向最后一个元素的下一个元素 null
   for(;iterator!=set1_end;iterator++){
     cout<<*iterator<<endl;
   }



   //map
   map<int,string> map_1 = {{1,"a"},{2,"B"}};//string 来自标准库实现
   //插入元素
   map_1.insert({3,"c"});
   //修改
   map_1[2] = "C";
}