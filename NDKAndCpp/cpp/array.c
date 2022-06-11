//
// Created by ISS Mac on 2019-06-27.
//
//数组是数据的集合，在内存中体现为连续的内存区域
//取址 每一个变量都有一个内存位置，每一个内存位置都定义了可使用连字号（&）运算符访问的地址，它表示了在内存中的一个地址
//指针  指针是一个变量，其值为另一个变量的地址，即，内存位置的直接地址
//   声明形式 type *var-name;       int *p;     *p数据 p数据地址
//   指针优先级 ()>[]>*    根据优先级区分 ()数组指针 []指针数组

// 指针数组   数组里面的每个数据都是指针     int *p[3];

//数组指针（行指针） int (*p)[n]  优先级高，首先说明p是一个指针，指向一个整型的一维数组，这个一维数组的长度是n，也可以说p的步长。也就是说执行p+1时，p要
  //跨过n个整型数据的长度
  // int a[3][4];
  // int (*p)[4];  定义一个数组指针，指向一个含有4个元素的一维数组
  // p=a;          将改二维数据的首地址赋给p，也就是a[0]或&a[0][0]
  // p++;          等同于p=p+1;p跨过行a[0][]指向了行a[1][]，二维数组第二行的首地址

#include "stdio.h"  
int main(){

   int arr[] = {100,200,300};
   for(int i=0;i<3;i++){
     printf("数组 %d\n",arr[i]);
   }

   int *p =arr;  //指向arr数据的首地址的数据
   printf("*p %d\n",*p); //数据 arr的首个数据
   printf("p %d\n",p);   //地址 arr的首地址
   *p = 400;
   printf("*p更改后 %d\n",*p);
   printf("arr[0] %d\n",arr[0]);
   printf("p %d\n",p);

  //指针运算
  *(p+1)=500;  //p+1 arr第二个地址 *(p+1) arr第二个数据
  printf("======指针运算后=====\n");
  for(int i=0;i<3;i++){
       printf("数组 %d\n",arr[i]);
     }

  int  *arrIndex[3]; //int 4字节  指针数组每个元素相差4
  for(int i=0;i<3;i++){
         arrIndex[i] = &arr[i];
       }

       for(int i=0;i<3;i++){
              printf("数组 %d\n",arrIndex[i]);
            }

   for(int i=0;i<3;i++){
                 printf("数组 %d\n",*arrIndex[i]); //
               }

}
