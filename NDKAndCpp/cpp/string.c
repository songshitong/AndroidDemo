#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
//字符串处理  https://wangdoc.com/clang/string.html
int main(){
//   C 语言没有单独的字符串类型，字符串被当作字符数组，即char类型的数组。比如，字符串“Hello”是当作数组{'H', 'e', 'l', 'l', 'o'}处理的。
  
//   编译器会给数组分配一段连续内存，所有字符储存在相邻的内存单元之中。在字符串结尾，C 语言会自动添加一个全是二进制0的字节，写作\0字符，表示字符串结束。
//   字符\0不同于字符0，前者的 ASCII 码是0（二进制形式00000000），后者的 ASCII 码是48（二进制形式00110000）。所以，字符串“Hello”实际储存的数组是{'H', 'e', 'l', 'l', 'o', '\0'}。

// 所有字符串的最后一个字符，都是\0。这样做的好处是，C 语言不需要知道字符串的长度，就可以读取内存里面的字符串，只要发现有一个字符是\0，
//   那么就知道字符串结束了。
 char localString[10];
// 上面示例声明了一个10个成员的字符数组，可以当作字符串。由于必须留一个位置给\0，所以最多只能容纳9个字符的字符串。

// 字符数组的长度，不能小于字符串的实际长度。

 char s1[5] = "hello";//注意使用的编译器
// 上面示例中，字符串数组s的长度是5，小于字符串“hello”的实际长度6，这时编译器会报错。因为如果只将前5个字符写入，而省略最后的结尾符号\0，
//  这很可能导致后面的字符串相关代码出错。

// 字符串的声明
// 字符串变量可以声明成一个字符数组，也可以声明成一个指针，指向字符数组。
// 写法一   由于字符数组的长度可以让编译器自动计算，所以声明时可以省略字符数组的长度  char s[]=""
char s2[14] = "Hello, world!";

// 写法二
char* s3 = "Hello, world!";


//字符串长度
// strlen()函数返回字符串的字节长度，不包括末尾的空字符\0
char s4[50] = "hello";
printf("%d\n", strlen(s4));  // 5
printf("%d\n", sizeof(s4));  // 50


//字符串复制
// 字符串的复制，不能使用赋值运算符，直接将一个字符串赋值给字符数组变量
//因为数组的变量名是一个固定的地址，不能修改，使其指向另一个地址。
char str1[10];
char str2[10];
// str1 = "abc"; // 报错
// str2 = str1;  // 报错
// 如果是字符指针，赋值运算符（=）只是将一个指针的地址复制给另一个指针，而不是复制字符串。
char* s5;
char* s6;
s5 = "abc";
s6 = s5;
// strcpy()函数，用于将一个字符串的内容复制到另一个字符串，相当于字符串赋值


  char *a="a";
  char *b="bbb";

   //比较字符串 按照字典序 0为相等 <0 a小于b  大于0,a大于b
 printf("字符串比较 %d\n",strcmp(a, b)); //-1
 //strncmp 只比较到指定的位置
 printf("strncmp %d \n",strncmp(a, b, 5));

   //将a和b拼接 然后放到c
   //动态申请一个地址空间
    char *c = (char *) malloc(strlen(a) + strlen(b));
    //复制
    //Copy SRC to DEST  strcpy(dest,src)
    strcpy(c, a);

    //添加  将b连接到c的后面
    //Append SRC onto DEST  strcat(dest,src)
    strcat(c, b);
    printf("字符串拼接后 %s\n",c);
    free(c);
    //strncat()  增加了第三个参数，指定最大添加的字符数


  //strncpy  strncpy()跟strcpy()的用法完全一样，只是多了第3个参数，用来指定复制的最大字符数，防止溢出目标字符串变量的边界
   char sncpy1[40];
   char sncpy2[12] = "hello world";

    strncpy(sncpy1, sncpy2, 5);
    sncpy1[5] = '\0';

    printf("%s\n", sncpy1); // hello


//    https://www.cnblogs.com/stonejin/archive/2011/09/16/2179248.html
//    strcpy和memcpy主要有以下3方面的区别。  #include <string.h>
//    1、复制的内容不同。strcpy只能复制字符串，而memcpy可以复制任意内容，例如字符数组、整型、结构体、类等。
//    2、复制的方法不同。strcpy不需要指定长度，它遇到被复制字符的串结束符"\0"才结束，所以容易溢出。memcpy则是根据其第3个参数决定复制的长度。
//    3、用途不同。通常在复制字符串时用strcpy，而需要复制其他类型数据时则一般用memcpy

    //  <string.h> 字符串拷贝  从存储区 str2 复制 n 个字节到存储区 str1   n要被复制的字节数
    //  void *memcpy(void *str1, const void *str2, size_t n) 
     const char src[50] = "http://www.runoob.com";
     char dest[50];
 
    memcpy(dest, src, strlen(src)+1);
    printf("dest = %s\n", dest);//dest = http://www.runoob.com

    //将 s 中第 11 个字符开始的 6个连续字符复制到 d 中:
    char *s="http://www.runoob.com";
    char d[20]; //开辟20个空间
     memcpy(d, s+11, 6);// 从第 11 个字符(r)开始复制，连续复制 6 个字符(runoob)
    // 或者 memcpy(d, s+11*sizeof(char), 6*sizeof(char));
    d[6]='\0';
    printf("%s", d);



//    sprintf()函数跟printf()类似，但是用于将数据写入字符串，而不是输出到显示器。该函数的原型定义在stdio.h头文件里面
    char first[6] = "hello";
    char last[6] = "world";
    char sSprintf[40];

    sprintf(sSprintf, "%s %s", first, last);

    printf("%s\n", sSprintf); // hello world


    // 字符串数组 # 
    // 如果一个数组的每个成员都是一个字符串，需要通过二维的字符数组实现。每个字符串本身是一个字符数组，多个字符串再组成一个数组  
    char weekdays1[7][10] = {
        "Monday",
        "Tuesday",
        "Wednesday",
        "Thursday",
        "Friday",
        "Saturday",
        "Sunday"
        };
    // 因为第一维的长度，编译器可以自动计算，所以可以省略
    char weekdays2[][10] = {
    "Monday",
    "Tuesday",
    "Wednesday",
    "Thursday",
    "Friday",
    "Saturday",
    "Sunday"
    };
    // 数组的第二维，长度统一定为10，有点浪费空间，因为大多数成员的长度都小于10。解决方法就是把数组的第二维，从字符数组改成字符指针
    char* weekdays3[] = {
        "Monday",
        "Tuesday",
        "Wednesday",
        "Thursday",
        "Friday",
        "Saturday",
        "Sunday"
        };
    // 上面的字符串数组，其实是一个一维数组，成员就是7个字符指针，每个指针指向一个字符串（字符数组）
    for (int i = 0; i < 7; i++) {
         printf("%s\n", weekdays3[i]);
    }

    //strerror
//    char *strerror(int errnum) 从内部数组中搜索错误号 errnum，并返回一个指向错误消息字符串的指针。strerror 生成的错误字符串取决于开发平台和编译器
    fopen("111222.txt","r");//打开一个不存在的文件，发生错误会设置errno
    printf("Error: %s\n", strerror(errno)); //打印当前的错误
    //EINTR指被中断唤醒，需要重新读/写
    //EWOULDBLOCK，EAGAIN代表需要重试一次 //https://blog.csdn.net/dongyu_1989/article/details/72840739
    //这个错误经常出现在当应用程序进行一些非阻塞（non-blocking）操作（对文件或socket）的时候。例如，以O_NONBLOCK的标记打开文件/socket/FIFO，
    // 如果你连续做read操作而没有数据可读。此时程序不会阻塞起来等待数据准备就绪返回，read函数会返回一个错误EAGAIN，
    // 提示你的应用程序现在没有数据可读请稍后再试。
    //又例如，当一个系统调用(比如fork)因为没有足够的资源（比如虚拟内存）而执行失败，返回EAGAIN提示其在调用一次（也许下次就能成功）。

    //格式化字符串
    //snprintf与vsnprintf   snprintf函数不是C中的标准函数，不同的编译器可能对该函数的实现不同
    //int vsnprintf(char* const  dest, size_t size, const char* format, va_list ap)  size指定格式化字符串的大小,包括\0
    char* buf;
    char param="p";
    my_snprintf(buf,"param is %s",param);
    return 0;
}

//自定义格式化字符
int my_snprintf(char* buffer, const char* format, ...) {
    va_list ap; //可变参数提取到ap  其他可变参数参考https://blog.csdn.net/zhizhengguan/article/details/110220084
    va_start(ap, format); //开始提取
    int ret = vsnprintf(buffer, strlen(format), format, ap);
    va_end(ap); //结束提取
    return ret;
}