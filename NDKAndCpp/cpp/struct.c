//
// Created by ISS Mac on 2019-06-27.
//
#include <stddef.h>
#include <stdio.h>

//结构体不用像Java一样new变量
//Student相当于类名
//student,a可以不定义，表示结构变量，也就是Student类型的变量  类似java  private Student a/student
struct Student{
  char name[50];
  int  age;
} student,a;

//使用typedef 定义 别名
typedef struct{
   char name[50];
    int  age;
}Student;


int main(){
  struct Student student;
  student.age = 1;
  a.age =2;
  printf("student %d\n",student.age);
  printf("a %d\n",a.age);
  printf("结构体大小 %d\n",sizeof(student));//内存对齐  name 52字节 age 4字节     总共56字节

//  offsetof(type, member-designator) 会生成一个类型为 size_t 的整型常量，它是一个结构成员相对于结构开头的字节偏移量
  //需要头文件#include <stddef.h>
  int ageOffset = offsetof(struct Student,age);
  printf("ageOffset %d\n",ageOffset);
  return 0;
}


//内存对齐
//  对齐跟数据在内存中的位置有关，如果一个变量的内存地址正好位于他长度的整数倍，他就被称作自然对齐。比如在32位CPU下，假设一个整型变量的地址为
//  0x00000004,那他就是自然对齐

//todo https://zhuanlan.zhihu.com/p/30007037

//内存对齐.png  对齐前 占用6字节       对齐后占用9字节  占用内存增多，但是取i，有之前的取值2次变为取值1次
//32位系统中（64位需要兼容32位）数据总线是32位，地址总线是32位，地址总线是32位意味着寻址空间是按4递增的，数据总线32位意味着一次可读写4bytes
//图片对应结构体
struct stu1{
  char c1;
  int  i;
  char c2;
};

//结构体大小
//  当结构体需要内存过大，使用动态内存申请。结构体占用字节数和结构体内字段有关，指针占用内存就是4/8字节，因此传指针比传值效率更高



//结构体存储原则
//结构体变量中成员的偏移量必须是成员大小的整数倍（0被认为是任何数的整数倍）
//结构体大小必须是所有成员大小的整数倍，也即所有成员大小的公倍数