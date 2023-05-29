//
// Created by ISS Mac on 2019-06-28.
//
//容器  stl 标准模板库
  //1 序列式      元素排列顺序与元素本身没有关系，由添加顺序决定
//     vector,list,dequeue,queue,stack,priority queue
  //2 关联式
//     set,map
#include <vector>
#include <set>
#include <map>
#include <iostream>
#include <unordered_map>
#include <memory>
using namespace std;

int main(){
   //将数字以单个字节逐个拷贝的方式放到指定的内存中去
   char* dp = new char[10];
   memset(dp,0,sizeof(dp));
   //int类型的变量一般占用4个字节，对每一个字节赋值0的话就变成了“00000000 00000000 000000000 00000000” （即10进制数中的0）
   //赋值为-1的话，放的是 “11111111 11111111 11111111 11111111 ”（十进制的-1）
   //赋值为1，dp数组的内容为 00000001 00000001 00000001 00000001 转化为十进制后不为1
   //赋值为127  “01111111 01111111 01111111 01111111”，（10进制的2139062143）  一个极大数
   //赋值为128 10000000 10000000 10000000 10000000  10进制-2139062144  一个极小数


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

   //删除最后一个
   vec_4.pop_back();
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
   for(;iterator != set_1.end();iterator++){
     cout<<*iterator<<endl;
   }



   //map
   map<int,string> map_1 = {{1,"a"},{2,"B"}};//string 来自标准库实现
   //插入元素
   map_1.insert({3,"c"});
   //修改
   map_1[2] = "C";

//   unordered_map 是关联容器，含有带唯一键的键-值 pair 。搜索、插入和元素移除拥有平均常数时间复杂度。
//   元素在内部不以任何特定顺序排序，而是组织进桶中。元素放进哪个桶完全依赖于其键的哈希。这允许对单独元素的快速访问，因为一旦计算哈希，
//   则它准确指代元素所放进的桶。
//头文件 #include <unordered_map>
// 创建三个 string 的 unordered_map （映射到 string ）
   std::unordered_map<std::string, std::string> u = {
           {"RED","#FF0000"},
           {"GREEN","#00FF00"},
           {"BLUE","#0000FF"}
   };
 //emplace  若容器中无拥有该关键的元素，则插入以给定的 args 原位构造的新元素到容器
   (&u)->emplace("BLACK2","#000000");

   // 迭代并打印 unordered_map 的关键和值
   for( const auto& n : u ) {
      std::cout << "Key:[" << n.first << "] Value:[" << n.second << "]\n";
   }

   // 添加新入口到 unordered_map
   u["BLACK"] = "#000000";
   u["WHITE"] = "#FFFFFF";

  
   // 用关键输出值
   std::cout << "The HEX of color RED is:[" << u["RED"] << "]\n";
   std::cout << "The HEX of color BLACK is:[" << u["BLACK"] << "]\n";
}