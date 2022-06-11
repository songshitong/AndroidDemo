#include <iostream>
#include <map>
#include <string>
#include <chrono>
#include <thread>
#include <mutex>

//https://www.apiref.com/cpp-zh/cpp/thread/mutex.html   
// mutex 类是能用于保护共享数据免受从多个线程同时访问的同步原语  C++11

//lock  锁定互斥，若互斥不可用则阻塞

//try_lock 尝试锁定互斥，若互斥不可用则返回

//unlock  解锁互斥

//注意 通常不直接使用 std::mutex ： std::unique_lock 、 std::lock_guard 或 std::scoped_lock (C++17 起)
//以更加异常安全的方式管理锁定


 
std::map<std::string, std::string> g_pages;
std::mutex g_pages_mutex;
 
void save_page(const std::string &url)
{
    // 模拟长页面读取
    std::this_thread::sleep_for(std::chrono::seconds(2));
    std::string result = "fake content";
    //使用lock_guard
    std::lock_guard<std::mutex> guard(g_pages_mutex);
    g_pages[url] = result;
}
 
int main() 
{
    std::thread t1(save_page, "http://foo");
    std::thread t2(save_page, "http://bar");
    t1.join();
    t2.join();
 
    // 现在访问g_pages是安全的，因为线程t1/t2生命周期已结束
    for (const auto &pair : g_pages) {
        std::cout << pair.first << " => " << pair.second << '\n';
    }
}