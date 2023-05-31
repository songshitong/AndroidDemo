//
// Created by songshitong on 2023/5/31.
//

#include "condition_variable.h"
#include <iostream>
#include <string>
#include <thread>
#include <mutex>
#include <condition_variable>
//条件变量 C++11标准

//https://segmentfault.com/a/1190000006679917
//条件变量（Condition Variable）的一般用法是：线程 A 等待某个条件并挂起，直到线程 B 设置了这个条件，并通知条件变量，然后线程 A 被唤醒
//这里等待的线程可以是多个，通知线程可以选择一次通知一个（notify_one）或一次通知所有（notify_all）

std::mutex mutex;
std::condition_variable cv;
std::string data;
bool ready = false;  // 条件 多线程共用参数
bool processed = false;  // 条件

//工作线程
void Worker() {
    std::unique_lock<std::mutex> lock(mutex);

    // 等待主线程发送数据。
    cv.wait(lock, [] { return ready; });

    // 等待后，继续拥有锁。
    std::cout << "工作线程正在处理数据..." << std::endl;
    // 睡眠一秒以模拟数据处理。
    std::this_thread::sleep_for(std::chrono::seconds(1));
    data += " 已处理";

    // 把数据发回主线程。
    processed = true;
    std::cout << "工作线程通知数据已经处理完毕。" << std::endl;

    // 通知前，手动解锁以防正在等待的线程被唤醒后又立即被阻塞。
    lock.unlock();

    cv.notify_one();
}

//主线程
int main() {
    std::thread worker(Worker);

    // 把数据发送给工作线程。
    {
        std::lock_guard<std::mutex> lock(mutex);
        std::cout << "主线程正在准备数据..." << std::endl;
        // 睡眠一秒以模拟数据准备。
        std::this_thread::sleep_for(std::chrono::seconds(1));
        data = "样本数据";
        ready = true;
        std::cout << "主线程通知数据已经准备完毕。" << std::endl;
    }
    cv.notify_one();

    // 等待工作线程处理数据。
    {
        std::unique_lock<std::mutex> lock(mutex);
        cv.wait(lock, [] { return processed; });
    }
    std::cout << "回到主线程，数据 = " << data << std::endl;

    worker.join();

    return 0;
}