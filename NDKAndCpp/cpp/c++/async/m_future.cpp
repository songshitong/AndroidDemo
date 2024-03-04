//
// Created by songshitong on 2024/2/23.
//

#include <iostream>       // std::cout
#include <future>         // std::async, std::future

bool is_prime (int x) {
  std::cout << "Calculating. Please, wait...\n";
  for (int i=2; i<x; ++i) if (x%i==0) return false;
  return true;
};

void print_int (std::future<int>& fut) {
  std::cout<<"child thread  print_int"<<std::endl;
  int x = fut.get(); //阻塞函数
  std::cout << "value: " << x << '\n';
}

int main(){
    //获取线程返回值的类std::future，用来访问异步操作的结果  一般配合future或者promise使用
    //https://mp.weixin.qq.com/s/JQnWblsNPLGexUMDQ8iijg

    //std::async,线程异步操作函数std::async，可用来创建异步task，其返回结果保存到future对象中

    std::future<bool> fut = std::async(is_prime,313222313);
    std::cout << "Checking whether 313222313 is prime.\n";
    bool ret = fut.get();      // waits for is_prime to return  阻塞函数
    //使用wait_for可以获得状态  while (fut.wait_for(span)==std::future_status::timeout)

    if (ret) std::cout << "It is prime!\n";
    else std::cout << "It is not prime.\n";


    //协助线程赋值的类std::promise，将数据和future绑定。在线程函数中，为外面传入的promise对象赋值，线程函数执行完毕后，就可以通过promise的future获取该值
    //future生成和传递更加灵活
     std::promise<int> prom;                      // create promise
     std::future<int> fut = prom.get_future();    // engagement with future
     std::thread th1 (print_int, std::ref(fut));  // send future to new thread   创建线程和参数
     std::cout<<"main thread start prom.set_value (10)"<<std::endl;
     prom.set_value (10);                         // fulfill promise
                                               // (synchronizes with getting the future
     th1.join();
  return 0;
}