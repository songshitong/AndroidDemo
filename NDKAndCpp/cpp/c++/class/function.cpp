//https://blog.csdn.net/wangshubo1989/article/details/49134235
//https://zhuanlan.zhihu.com/p/295102090
//https://www.cnblogs.com/DswCnblog/p/5629165.html
//类模版std::function是一种通用、多态的函数封装。std::function的实例可以对任何可以调用的目标实体进行存储、复制、和调用操作，
//这些目标实体包括普通函数、Lambda表达式、函数指针、以及其它函数对象等。std::function对象是对C++中现有的可调用实体的一种类型安全的包裹
//（我们知道像函数指针这类可调用实体，是类型不安全的）。
//
//通常std::function是一个函数对象类，它包装其它任意的函数对象，被包装的函数对象具有类型为T1, …,TN的N个参数，并且返回一个可转换到R类型的值。
//std::function使用 模板转换构造函数接收被包装的函数对象；特别是，闭包类型可以隐式地转换为std::function。
//
//最简单的理解就是：
//
//通过std::function对C++中各种可调用实体（普通函数、Lambda表达式、函数指针、以及其它函数对象等）的封装，形成一个新的可调用的std::function对象；
//让我们不再纠结那么多的可调用实体。

//回调建议使用std::function   使用lambda，无法捕获外部变量，这样lambda的数据无法传递给外部
#include <functional>

int main(){
//    lambda  [](int a, int b) -> bool { return a < b; }
//[capture list] (params list) mutable exception-> return type { function body }
//简化形式：
//    [capture list] {function body}
//    [capture list] (params list) {function body}
//    [capture list] (params list) -> return type {function body}

//    capture list：捕获外部变量列表
//    params list：形参列表
//    mutable指示符：用来说用是否可以修改捕获的变量
//    exception：异常设定
//    return type：返回类型
//    function body：函数体

//  捕获形式
//    捕获形式	说明
//    []	不捕获任何外部变量
//    [变量名, …]	默认以值得形式捕获指定的多个外部变量（用逗号分隔），如果引用捕获，需要显示声明（使用&说明符）
//    [this]	以值的形式捕获this指针
//    [=]	以值的形式捕获所有外部变量
//    [&]	以引用形式捕获所有外部变量
//    [=, &x]	变量x以引用形式捕获，其余变量以传值形式捕获
//    [&, x]	变量x以值的形式捕获，其余变量以引用形式捕获



  //测试回调函数
  add(1,2,[](int result){
      cout<< result;
  });
}

typedef std::function<void(int)> ResultCall;//定义回调函数 里面为函数签名
void add(int a,int b,ResultCall resultCall){
  resultCall(a+b);
}