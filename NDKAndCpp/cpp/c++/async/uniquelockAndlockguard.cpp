//
// Created by songshitong on 2023/6/1.
//

#include "uniquelockAndlockguard.h"

#include <iostream>
#include <thread>
#include <string>
#include <mutex>
using namespace std;

//c++11  https://zhuanlan.zhihu.com/p/340348726
//lock_guard<mutex> guard(mt);
//这个也是构造互斥锁的写法，就是会在lock_guard构造函数里加锁，在析构函数里解锁，之所以搞了这个写法，
//C++委员会的解释是防止使用mutex加锁解锁的时候，忘记解锁unlock了

//__mutex_base
// 构造器自动枷锁
//lock_guard
//explicit lock_guard(mutex_type& __m) _LIBCPP_THREAD_SAFETY_ANNOTATION(acquire_capability(__m))
//: __m_(__m) {__m_.lock();}

// 析构自动解锁
//lock_guard
//~lock_guard() _LIBCPP_THREAD_SAFETY_ANNOTATION(release_capability()) {__m_.unlock();}

//缺陷，在定义lock_guard的地方会调用构造函数加锁，在离开定义域的话lock_guard就会被销毁，调用析构函数解锁。这就产生了一个问题，
//如果这个定义域范围很大的话，那么锁的粒度就很大，很大程序上会影响效率


//unique_lock对lock_guard的优化
//在构造函数加锁，然后可以利用unique.unlock()来解锁，所以当你觉得锁的粒度太多的时候，可以利用这个来解锁，而析构的时候会判断当前锁的状态来决定是否解锁，
// 如果当前状态已经是解锁状态了，那么就不会再次解锁，而如果当前状态是加锁状态，就会自动调用unique.unlock()来解锁。而lock_guard在析构的时候一定会解锁，
// 也没有中途解锁的功能。
//当然，方便肯定是有代价的，unique_lock内部会维护一个锁的状态，所以在效率上肯定会比lock_guard慢。
//unique_lock
//explicit unique_lock(mutex_type& __m)
//: __m_(_VSTD::addressof(__m)), __owns_(true) {__m_->lock();}

//unique_lock
//~unique_lock()
//{
//    if (__owns_)
//        __m_->unlock();
//}
mutex mt;
void thread_task()
{
    for (int i = 0; i < 10; i++)
    {
        lock_guard<mutex> guard(mt);
        cout << "print thread: " << i << endl;
    }//大括号{}结束的时 调用析构函数的解锁
}

int main()
{
    testLockGuard();
    return 0;
}
void testLockGuard(){
    thread t(thread_task);
    for (int i = 0; i > -10; i--)
    {
        lock_guard<mutex> guard(mt);
        cout << "print main: " << i << endl;
    }
    t.join();
}