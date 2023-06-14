
#include <iostream>
#include <ctime>

//https://www.runoob.com/cplusplus/cpp-date-time.html
//c++的日期  C++ 标准库没有提供所谓的日期类型。C++ 继承了 C 语言用于日期和时间操作的结构和函数
int main(){
    //当前时间 格式化为yyyy-mm-dd-hh-mm-ss
    time_t t = time(nullptr); //标准时间
    char tmp[21] = {'\0'};
    strftime(tmp, sizeof(tmp), "%Y-%m-%d-%H-%M-%S", localtime(&t));
}