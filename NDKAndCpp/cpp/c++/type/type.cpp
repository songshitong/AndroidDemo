//
// Created by ISS Mac on 2019-06-28.
//

//c的强转
//c风格的强制类型转换（TypeCast）很简单，不管什么类型的转换，统统是 TYPE b = (TYPE)a;


//c++的类型转换
//c++风格的类型转换提供了四种类型转换操作符   TYPE b= const_cast<TYPE>(a);  TYPE b= static_cast<TYPE>(a); .....
  //1 const_cast  去const属性                                修改类型的const或volatile
  //2 static_cast  静态类型转换，例如int转为char               基础类型之间的转换；指针和void之间互换；子类指针/引用与父类指针/引用互换
  //3 dynamic_cast  动态类型转换，例如子类和父类之间的多态类型转换 将基类指针、引用安全地转为派生类；在运行期对可疑的转型操作进行安全检查，仅对多态有效
  //4 reinterpret_cast 仅仅重新解释型，但没有进行二进制的转换     对指针、引用进行原始转换
#include <iostream>
#include<string>

int pad(int s){
  return ((s)+3)&~3;
};
//二进制可视化 https://bitwisecmd.com/
// 6&~3
// 6	        110	0x6		
// &	~3	    100	-4		
// =	4	      100	0x4		


int main(){

  const char *a; //const 类似Java的final

  char* b = const_cast<char*>(a);

  //c++11 int转string
  int num =1;
  std::string ch = std::to_string(num);

  int* p ;
  if(p == nullptr){
     //判断为空指针
  }
 
 //对齐为4的倍数
 std::cout << "pad 3: " << pad(3) << std::endl;
 std::cout << "pad 4: " << pad(4) << std::endl;
 std::cout << "pad 11: " << pad(11) << std::endl;


 return 0;
}

