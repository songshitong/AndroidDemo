#include "iostream"
using namespace std;

int main(){
    // https://cloud.tencent.com/developer/article/1660750
    // C++98 auto
    //  早在C++98标准中就存在了auto关键字，那时的auto用于声明变量为自动变量，自动变量意为拥有自动的生命期，这是多余的，因为就算不使用auto声明，变量依旧拥有自动的生命期：

    //   int a =10 ;  //拥有自动生命期
    //    auto int b = 20 ;//拥有自动生命期
    //   static int c = 30 ;//延长了生命期
    // C++98中的auto多余且极少使用，C++11已经删除了这一用法，取而代之的是全新的auto：变量的自动类型推断。

    // C++11 auto
    //  auto可以在声明变量的时候根据变量初始值的类型自动为此变量选择匹配的类型，类似的关键字还有decltype。
    //  举个例子：

    int a = 10;
    auto au_a = a;//自动类型推断，au_a为int类型
    cout << typeid(au_a).name() << endl; 
}