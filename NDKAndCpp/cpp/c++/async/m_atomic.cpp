
//以下来自深入理解Android：Java虚拟机ART (邓凡平)
//atomic问题也可以借助mutex来解决。 不过，从更抽象的层次来考虑，mutex是用来同步代码逻辑的，而atomic是用来同步数据操作的。
//用mutex来同步数据操作，有些杀鸡用牛刀的感觉
//        C++11一共定义了六种内存顺序类
//·memory_order_seq_cst：seq cst是sequential consistent的缩写，意为顺序一致性。它是内存顺序要求中最严格的。
//使用它的话就能防止代码重排的问题。它是atomic store与load函数的默认取值。所以，上面这段代码就不会再有order的问题。
//缺点：在某些CPU平台下会影响性能，可以使用memory_order_acquire，memory_order_release优化
//·memory_order_relaxed：松散模型。这种模型不对内存order有什么限制，编译器会根据目标CPU的情况做优化。
//·memory_order_acquire：使用它的线程中，后续的所有读操作必须在本条原子操作后执行。
//·memory_order_release：使用它的线程中，之前的所有写操作必须在本条原子操作前执行完。
//·memory_order_acq_rel：同时包含上面acquire和release的要求。
//·memory_order_consume：使用它的线程中，后续有关的原子操作必须在本原子操作完成后执行。
//更多 https://en.cppreference.com/w/cpp/atomic/memory_order。

//std::atomic_flag的使用  类似于atomic<bool>      https://www.cnblogs.com/haippy/p/3252056.html
//bool test_and_set (memory_order sync = memory_order_seq_cst) noexcept;
//在上锁的时候，如果 lock.test_and_set 返回 false，则表示上锁成功（此时 while 不会进入自旋状态），因为此前 lock 的标志位为 false
//(即没有线程对 lock 进行上锁操作)，但调用 test_and_set 后 lock 的标志位为 true，说明某一线程已经成功获得了 lock 锁。
//如果在该线程解锁（即调用 lock.clear(std::memory_order_release)） 之前，另外一个线程也调用 lock.test_and_set(std::memory_order_acquire)
//试图获得锁，则 test_and_set(std::memory_order_acquire) 返回 true，则 while 进入自旋状态。如果获得锁的线程解锁
//（即调用了 lock.clear(std::memory_order_release)）之后，某个线程试图调用 lock.test_and_set(std::memory_order_acquire) 并且返回 false，
//则 while 不会进入自旋，此时表明该线程成功地获得了锁
//void clear (memory_order sync = memory_order_seq_cst) 操作完成清除内部标记

#include <thread>
#include <atomic>
#include <cassert>
#include "iostream"
#include <vector>

std::atomic<bool> x = {false};
std::atomic<bool> y = {false};
std::atomic<int> z = {0};

void write_x()
{
    x.store(true, std::memory_order_seq_cst);
}

void write_y()
{
    y.store(true, std::memory_order_seq_cst);
}

void read_x_then_y()
{
    while (!x.load(std::memory_order_seq_cst))
        ;
    //如果x为false，一直循环，x为true，往下执行
    //如果x为true，++z
    if (y.load(std::memory_order_seq_cst)) {
        ++z;
    }
}

void read_y_then_x()
{
    while (!y.load(std::memory_order_seq_cst))
        ;
    //如果y为false，一直循环；为true往下执行
    //如果x为true，++z
    if (x.load(std::memory_order_seq_cst)) {
        ++z;
    }
}

int main()
{
    //顺序一致性模型示例
    std::thread a(write_x);
    std::thread b(write_y);
    std::thread c(read_x_then_y);
    std::thread d(read_y_then_x);
    a.join(); b.join(); c.join(); d.join();
    assert(z.load() != 0);  // will never happen
    std::cout << "z is " << z.load() << std::endl;
    //结果  z is2

    testAtomicFlag();
}

std::atomic_flag lock = ATOMIC_FLAG_INIT;
void f(int n)
{
    for (int cnt = 0; cnt < 100; ++cnt) {
        while (lock.test_and_set(std::memory_order_acquire))  // acquire lock
            ; // spin  返回true代表其他线程获得锁，一直自旋
        std::cout << "Output from thread " << n << '\n';
        lock.clear(std::memory_order_release);               // release lock

        //等价于：
        while (!lock.test_and_set(std::memory_order_acquire)){ //初始为false，进入
            std::cout << "Output from thread " << n << '\n';  //当前true
            lock.clear(std::memory_order_release); //清除true，让其他线程可以获得锁
        }
    }
}
void testAtomicFlag{
        std::vector<std::thread> v;
        for (int n = 0; n < 10; ++n) {
            v.emplace_back(f, n);
        }
        for (auto& t : v) {
            t.join();
        }
}