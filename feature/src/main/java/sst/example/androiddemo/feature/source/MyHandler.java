package sst.example.androiddemo.feature.source;//核心类
//handler
//message
//looper
//MessageQueue

import android.os.Message;
import android.os.MessageQueue;

//java 层     c++层
//HandlerThread
//threadlocal

//生产者 消费者
class A{
    Message msg;
    ThreadLocal<MyLooper> threadLocal = new ThreadLocal<>();
    MessageQueue mq;
        }