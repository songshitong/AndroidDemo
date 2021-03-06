
//POSIX 可移植操作系统接口（英语：Portable Operating System Interface，缩写为POSIX）是IEEE为要在各种UNIX操作系统上运行软件
//unix,linux,window各有实现

//线程创建  创建的线程id，线程参数，调用的函数，传入的函数参数
//pthread_create (thread, attr, start_routine, arg)

//线程终止
// 如果 main() 是在它所创建的线程之前结束，并通过 pthread_exit() 退出，那么其他线程将继续执行。否则，它们将在 main() 结束时自动被终止
//pthread_exit (status)

//线程链接  阻塞的方式等待thread指定的线程结束，thread指定的线程必须是joinable的。
// pthread_join() 子程序阻碍调用程序，直到指定的 threadid 线程终止为止。当创建一个线程时，它的某个属性会定义它是否是可连接的（joinable）或可分离的（detached）。
// 只有创建时定义为可连接的线程才可以被连接。如果线程创建时被定义为可分离的，则它永远也不能被连接
// pthread_join(pthread_t)

//线程分离
// 比如在Web服务器中当主线程为每个新来的链接创建一个子线程进行处理的时候，主线程并不希望因为调用pthread_join而阻塞（因为还要继续处理之后到来的链接），这时可以在子线程中加入代码 
// pthread_detach(pthread_self()) 或者父线程调用 pthread_detach(thread_id)（非阻塞，可立即返回） 
// 这将该子线程的状态设置为detached,则该线程运行结束后会自动释放所有资源
// pthread_detach(threadid) 



#include <iostream>
// 必须的头文件
#include <pthread.h>

using namespace std;

#define NUM_THREADS 5

//互斥锁
pthread_mutex_t mutexT;
// 线程的运行函数
void *say_hello(void *args)
{
    // 加锁
    if (pthread_mutex_lock(&mutexT) != 0)
    {
        fprintf(stdout, "lock error!\n");
    }
    // 对传入的参数进行强制类型转换，由无类型指针变为整形数指针，然后再读取
    int tid = *((int *)args);
    cout << "Hello Runoob！" << tid << endl;
    // 解锁
    pthread_mutex_unlock(&mutexT);
    //退出子线程
    pthread_exit(NULL);
}

int main()
{
    // 定义线程的 id 变量，多个变量使用数组
    pthread_t tids[NUM_THREADS];
    int ret = pthread_mutex_init(&mutexT, NULL);
    if (ret != 0)
    {
        cout << "mutex 初始化失败" << endl;
        return 0;
    }
    int indexes[NUM_THREADS]; // 用数组来保存i的值
    pthread_attr_t attr;
    // 初始化并设置线程为可连接的（joinable）
     pthread_attr_init(&attr);
     pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_JOINABLE);
    cout << "code 1  " << endl;
    for (int i = 0; i < NUM_THREADS; i++)
    {
        indexes[i] = i; //先保存i的值 不保存i，线程运行时，I的指针地址的数据已经改变
        //参数依次是：创建的线程id，线程参数，调用的函数，传入的函数参数
        int ret = pthread_create(&tids[i], NULL, say_hello, (void *)&(indexes[i]));
        if (ret != 0)
        {
            cout << "pthread create error: error_code=" << ret << endl;
        }
    }
    // 删除属性，并等待其他线程
    pthread_attr_destroy(&attr);
    //code 2 输出时线程可能还在进行中
    cout << "code 2  " << endl;
    void *status;
    for (int i = 0; i <NUM_THREADS; i++)
    {
        //主线程中等待其他线程完成
        pthread_join(tids[i], &status);
        cout <<"thread complete id: "<< i << "  exiting with status : " << status << endl;
    }
    //code 3输出时，子线程已经执行完毕
    cout << "code 3  " << endl;

    int result = pthread_mutex_destroy(&mutexT);
    //等各个线程退出后，进程才结束，否则进程强制结束了，线程可能还没反应过来；
    pthread_exit(NULL);
}
