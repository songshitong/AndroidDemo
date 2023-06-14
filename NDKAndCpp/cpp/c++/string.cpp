//
// Created by songshitong on 2023/6/14.
//
#include <cstring>
#include <iostream>
int main(){
  //int转string  其他long等也试用
  std::to_string(1);
  //使用ostringstream 和istringstream 也可以

  //string 转char
  std::string charS("123");
  charS.c_str();

  //字符串比较
  std::string a("1"); //返回0(等于)、正数(大于)或负数(小于)
  cout<< a.compare("1"); //0
  cout<< "\n";
  cout<< a.compare("2"); //-1
  cout<< "\n";
  cout<< a.compare("-1"); //4

  //字符串截取 substr
  std::string b("123456");
  cout<< b.substr(2,2);  //从第3位置 截取2个  34
  cout<< b.substr(3); //从第四个截取到末尾  456

  //字符拼接
  std::string canat;
  canat.append("this is");
  canat.append("a");
}
