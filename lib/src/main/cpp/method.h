//
// Created by ISS Mac on 2019-06-27.
//
//头文件类似Java的接口，只有方法的声明，没有方法的实现
//头文件使用  自己定义#indclude "method.h"     使用系统头文件#indclude <method.h>
//为了解决程序的耦合程度，不参与编译，引入头文件就可以调用程序而不用引入程序文件
int func(void);
int func(void* pathName,int age);