//
// Created by songshitong on 2023/5/30.
//

#include "mscoped_lock.h"
#include <iostream>
#include <string>
#include <vector>
#include <mutex>
#include <thread>

//https://blog.csdn.net/wangzhicheng1983/article/details/119481595
//（1）将多个锁（std::mutex等）包装成一种锁类型，用于线程一次性申请多个锁，避免死锁。
//
//（2）当程序出现异常，可自动析构，完成锁的是否
class user {
public:
    user(const std::string &id) : id_(id) {
    }

    void exchange_infos(user &other) {
        //锁住自己和other
        std::scoped_lock sl(lock_, other.lock_);
        infos_.swap(other.infos_);
        std::cout << "user id:" << id_ << " exchange user id:" << other.id_ << std::endl;
    }

private:
    std::string id_;
    std::vector <item_t> infos_;
    std::mutex lock_;
}
