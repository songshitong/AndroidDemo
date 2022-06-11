
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


#include <thread>
#include <atomic>
#include <cassert>
#include "iostream"

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
}