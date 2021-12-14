//
// Created by ISS Mac on 2019-07-03.
//

//extern "C"的主要作用就是为了能够正确实现C++代码调用其他C语言代码。加上extern "C"后，会指示编译器这部分代码按C语言的进行编译，而不是C++的。
//由于C++支持函数重载，因此编译器编译函数的过程中会将函数的参数类型也加到编译后的代码中，而不仅仅是函数名；而C语言并不支持函数重载，
//因此编译C语言代码的函数时不会带上函数的参数类型，一般只包括函数名

extern "C"{   //开启混合编译，告诉编译器，下面的是c文件

#include "" //在c++中引入一个c文件
}



%d              十进制有符号整数
%u              十进制无符号整数
%f              浮点数
%s              字符串
%c              单个字符
%p              指针的值
%e              指数形式的浮点数
%x, %X          无符号以十六进制表示的整数
%0              无符号以八进制表示的整数
%g              自动选择合适的表示法

__android_log_print(ANDROID_LOG_ERROR,"FFmpegCmd","av_version_info %s",av_version_info()); 以字符串输出



//        https://www.runoob.com/cplusplus/cpp-data-types.html
//无符号unsigned  有符号signed
// TODO 两者转换  原码 补码 反码  扩展  截断 溢出
//https://www.zhihu.com/question/20159860
//        https://www.cnblogs.com/x_wukong/p/3952688.html



//宏定义 #define if


goto关键字
goto 语句允许把控制无条件转移到同一函数内的被标记的语句。

注意：在任何编程语言中，都不建议使用 goto 语句。因为它使得程序的控制流难以跟踪，使程序难以理解和难以修改。
任何使用 goto 语句的程序可以改写成不需要使用 goto 语句的写法。
goto label;        //label可以在goto下面也可以在上面
..
.
label: statement;
实例  输出10-19，跳过15
/*
// 局部变量声明
   int a = 10;

   // do 循环执行
   LOOP:do
   {
       if( a == 15)
       {
          // 跳过迭代
          a = a + 1;
          goto LOOP;
       }
       cout << "a 的值：" << a << endl;
       a = a + 1;
   }while( a < 20 );
*/