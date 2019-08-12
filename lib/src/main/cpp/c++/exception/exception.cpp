//
// Created by ISS Mac on 2019-06-28.
//
#include <iostream>
void test(){
  throw "test 异常";
}

void testException(){
 throw Exception("testException 异常");

}

int main(){
 //抛出异常
 throw "异常1";

 throw Exception("异常2");

 //捕获异常
 try{
   test();
 }catch(const char *m){
   std::cout<< m << std::endl;
 }


 try{
    testException();
  }catch(exception &e){
    std::cout<< e.what() << std::endl;
  }

  //随便抛出一个对象都可以

}

//自定义exception
class MyException : public Exception{
  public:
    virtual char const* what() const{
      return "my exception ";
    }
}