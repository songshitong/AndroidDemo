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
#include <iostream>

typedef std::function<void(int)> ResultCall;//定义回调函数 里面为函数签名
void add(int a,int b,ResultCall resultCall){
    resultCall(a+b);
};

int doubleFun(int a)
{
    return a*2;
};

class Add
{
public: 
  int member = 10;
  int operator()(int a,int b){ //function持有的类需要重写operator
        return a+b;
  }
  int addInt(int a,int b){
    return a+b;
  }
};

double my_divide (double x, double y) {return x/y;};

struct MyPair {
  double a,b;
  double multiply() {return a*b;}
};

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
      std::cout<<"测试回调"<< result << std::endl;
  });
  //lamada表达式情况
  std::function<int(int,int)> lambda = [](int a,int b){
    return a+b;
  };
  int lambdaResult = lambda(1,2);
  std::cout<<"lambdaResult result = "<<lambdaResult<<std::endl;


  //https://mp.weixin.qq.com/s/dh26mdQdCa_pydIj4mghGQ
  //直接可以接受函数指针的赋值
  std::function<int(int)> f = doubleFun;
  int functionResult = f(1);
  std::cout<<"function result = "<<functionResult<<std::endl;
  //存储类对象
  std::function<int(int,int)> addF = Add();
  int addResult = addF(1,2); //等同于Add::operator()
  std::cout<<"addResult result = "<<addResult<<std::endl;
  //存储成员方法  function的Args里面第一个参数必须要求传递类对象。 调用function时候第一个参数也是必须要是Add类的对象
  std::function<int(Add&,int,int)> addIntFun = &Add::addInt;
  Add test;
  int addIntResult = addIntFun(test,1,2);
  std::cout<<"addIntResult result = "<<addIntResult<<std::endl;
  //存储类成员变量
  std::function<int(Add&)> memberFun = &Add::member;
  Add testMemberAdd;
  int memberResult = memberFun(testMemberAdd);
  std::cout<<"memberResult result = "<<memberResult<<std::endl;


 //std::bind 它是一个函数适配器，接受一个可调用对象（callable object），生成一个新的可调用对象来“适应”原对象的参数列表。
  //将可调用对象和其参数绑定成一个仿函数；
  //只绑定部分参数，减少可调用对象传入的参数。
  //第一个参数 fn     一个function对象，方法指针，或者是类成员变量
  //第二个参数  args 所有参数的集合列表，要买具体的值，要么placeholders
  //返回值 就是一个function的对象，当调用时候调用的就是fn这个方法体和带上传递的参数
  //std::placeholders _1,_2......就代表预置第1，2、、、个参数，后续真正调用function对象时候再传递进去
  using namespace std::placeholders;    // adds visibility of _1, _2, _3,...
  auto fn_five = std::bind (my_divide,10,2);  //执行my_divide
  std::cout << "测试fn_five:" <<fn_five() << '\n'; //5

  //参数1需要执行时传入
  auto fn_half = std::bind (my_divide,_1,2);               // returns x/2
  std::cout<< "test placeholer _1:" << fn_half(20) << '\n';  //10

  //bind类成员
  auto bound_member_fn = std::bind (&MyPair::multiply,_1); // returns x.multiply
  MyPair ten_two {10,2};
  std::cout << "test bind fun: " << bound_member_fn(ten_two) << '\n'; // 20  //相当于调用ten_two.multiply

  auto bound_member_data = std::bind (&MyPair::a,ten_two); // returns ten_two.a (获取a.值)
  std::cout << "test bind member: " << bound_member_data() << '\n'; //10  相当于ten_two.a()
}

