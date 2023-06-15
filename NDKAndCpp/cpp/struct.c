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
// https://zhuanlan.zhihu.com/p/30007037
//计算机系统对基本类型数据在内存中存放的位置有限制，它们会要求这些数据的首地址的值是某个数k（通常它为4或8）的倍数
//大部分处理器并不是按字节块来存取内存的.它一般会以双字节,四字节,8字节,16字节甚至32字节为单位来存取内存，我们将上述这些存取单位称为内存存取粒度
//内存对齐规则
//每个特定平台上的编译器都有自己的默认“对齐系数”（也叫对齐模数）。gcc中默认#pragma pack(4)，可以通过预编译命令#pragma pack(n)，
//n = 1,2,4,8,16来改变这一系数。
//有效对其值：是给定值#pragma pack(n)和结构体中最长数据类型长度中较小的那个。有效对齐值也叫对齐单位。
//为什么需要内存对齐 https://blog.csdn.net/www_dong/article/details/112549336
// 1 某些平台(例如Alpha等)若读取的数据未内存对齐，将拒绝访问或抛出异常
// 2 内存对齐占用空间更多，但是读取内存更高效
//  一个int数放在1的位置1,2,3,4 计算机读取时读0-3,4-7 然后分别取出1-3,4  最后合并为1-4
//  内存对齐后int的数放在4,5,6,7  计算机读取时，直接读4-7就可以了

//  对齐跟数据在内存中的位置有关，如果一个变量的内存地址正好位于他长度的整数倍，他就被称作自然对齐。比如在32位CPU下，假设一个整型变量的地址为
//  0x00000004,那他就是自然对齐

//内存对齐.png  对齐前 占用6字节       对齐后占用9字节  占用内存增多，但是取i，有之前的取值2次变为取值1次
//32位系统中（64位需要兼容32位）数据总线是32位，地址总线是32位，地址总线是32位意味着寻址空间是按4递增的，数据总线32位意味着一次可读写4bytes
//图片对应结构体
//缺点：内存对齐可能造成内存空缺，占用更多内存，视情况而用
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