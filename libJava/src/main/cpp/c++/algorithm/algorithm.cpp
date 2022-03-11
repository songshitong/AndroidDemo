#include<algorithm>
#include<iostream>
using namespace std;
bool cmp(int a,int b);
 main()
  {
 //排序函数
 //第一个参数first：是要排序的数组的起始地址
 //第二个参数last：是结束的地址（最后一个数据的后一个数据的地址）
 //第三个参数采用默认从小到大
  int a[]={45,12,34,77,90,11,2,4,5,55};
  sort(a,a+10);
  for(int i=0;i<10;i++)
   cout<<a[i]<<" ";

  //实现从大到小
  sort(a,a+10,cmp);
  for(int i=0;i<10;i++)
     cout<<a[i]<<" ";
}

//自定义函数
bool cmp(int a,int b){
  return a>b;
}