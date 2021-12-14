https://segmentfault.com/a/1190000011826846    android8.1
//深入理解Android 卷三
EventHub的作用是将来源不同的各种信息，转化成为一种类型的信息，然后将这些信息提交到上层，给上层做处理。也就是说在输入设备中，
各种类型的输入信息，通过EventHub进行一个处理之后，将信息转化为同一种类型的信息传递到上层
EventHub初始化
01_输入事件传递流程和InputManagerService的诞生.md
frameworks/base/services/core/jni/com_android_server_input_InputManagerService.cpp
```
NativeInputManager::NativeInputManager(jobject contextObj,
        jobject serviceObj, const sp<Looper>& looper) :
        mLooper(looper), mInteractive(true) {
    ...
    sp<EventHub> eventHub = new EventHub();
    mInputManager = new InputManager(eventHub, this, this);
}
```

/frameworks/native/services/inputflinger/EventHub.cpp
```
static const char *DEVICE_PATH = "/dev/input";
EventHub::EventHub(void) :
      mBuiltInKeyboardId(NO_BUILT_IN_KEYBOARD), mNextDeviceId(1), mControllerNumbers(),
        mOpeningDevices(0), mClosingDevices(0),
        mNeedToSendFinishedDeviceScan(false),
        mNeedToReopenDevices(false), mNeedToScanDevices(true),
        mPendingEventCount(0), mPendingEventIndex(0), mPendingINotify(false) {

    acquire_wake_lock(PARTIAL_WAKE_LOCK, WAKE_LOCK_ID);
    //创建一个epoll句柄
    mEpollFd = epoll_create(EPOLL_SIZE_HINT);

    mINotifyFd = inotify_init();

    //监视dev/input目录的变化删除和创建变化
    int result = inotify_add_watch(mINotifyFd, DEVICE_PATH, IN_DELETE | IN_CREATE);

    struct epoll_event eventItem;
    memset(&eventItem, 0, sizeof(eventItem));
    eventItem.events = EPOLLIN;
    eventItem.data.u32 = EPOLL_ID_INOTIFY;

    //把inotify的句柄加入到epoll监测
    result = epoll_ctl(mEpollFd, EPOLL_CTL_ADD, mINotifyFd, &eventItem);

    //创建匿名管道
    int wakeFds[2];
    result = pipe(wakeFds);
    mWakeReadPipeFd = wakeFds[0];
    mWakeWritePipeFd = wakeFds[1];

    //将管道的读写端设置为非阻塞
    result = fcntl(mWakeReadPipeFd, F_SETFL, O_NONBLOCK);
    result = fcntl(mWakeWritePipeFd, F_SETFL, O_NONBLOCK);

    eventItem.data.u32 = EPOLL_ID_WAKE;

    //将管道的读端加入到epoll监测
    result = epoll_ctl(mEpollFd, EPOLL_CTL_ADD, mWakeReadPipeFd, &eventItem);

    int major, minor;
    getLinuxRelease(&major, &minor);
    // EPOLLWAKEUP was introduced in kerel 3.5
    mUsingEpollWakeup = major > 3 || (major == 3 && minor >= 5);
}

```
构造函数首先创建了epoll句柄，然后创建了inotify句柄，然后创建了一个匿名管道，并将匿名管道设置为非阻塞，
inotify是Linux下的一个监控目录和文件变化的机制，这里监控了/dev/input目录，当这个目录发生变化，就表明有输入设备加入或者移除。
至此，EventHub只是进行了一些监控操作的处理。而对于EventHub相关事件处理部分的调用则是在创建ReaderThread的时候
EventHub是由InputReader驱动的 InputReader::loopOnce  03_InputReader的加工类型和InputDispatcher的分发过程.md

getEvents方法
```
size_t EventHub::getEvents(int timeoutMillis, RawEvent* buffer, size_t bufferSize) {
    //event指针指向在buffer中下一个可用于存储事件的RawEvent结构体。每存储一个事件，event指针都会向后偏移一个元素
    RawEvent* event = buffer;
    //capacity记录了buffer中剩余的元素数量。 当capacity为0时，表示buffer已满，此时需要停止继续处理新事件，并将已处理的事件
    //返回给调用者
        size_t capacity = bufferSize;
        bool awoken = false;
    //接下来的循环时getEvents()函数的主体。在这个循环中，会先将可用事件放入buffer中并返回。如果没有可用事件，则进入epoll_wait()
    //等待事件到来，epoll_wait()返回后会重新执行循环体，并将新事件放入buffer
        for (;;) {
            nsecs_t now = systemTime(SYSTEM_TIME_MONOTONIC);
          //1 首先进行与设备相关的工作。某些情况下，如EventHub创建后第一次执行getEvents时，需要扫描/dev/input文件下
          //的所有设备节点并将这些设备节点打开。另外，当设备节点的增删事件发生时，会将这些事件存入buffer中
          ...
          //2 处理未被inputReader取走的输入事件与设备事件。 epoll_wait所取出的epoll_event存储在mPendingEventItems中，
          //mPendingEventCount指定mPendingEventItems数组所存储的事件个数.而mPendingEventIndex指定尚未处理的
          //epoll_event索引
          while (mPendingEventIndex < mPendingEventCount) {
            const struct epoll_event& eventItem = mPendingEventItems[mPendingEventIndex++];
           //在这里分析每一个epoll_event,如果表示设备节点可读，则读取原始事件并放置到buffer中。如果表示mINotifyFd可读，
           //则设置mPendingInotify为true，当InputReader将现有的输入事件都取出后读取mInotifyFD中的事件，并加载与卸载相应
           //的设备。另外，如果此epoll_event表示wakeFDs的读取端有数据可读，则设置awake标志true,此时无论此次get_Events
           //调用是否取到事件，都不会调用epoll_event进行事件等待
          }
          //3 如果mInotifyFD有数据可读，说明设备节点发生了增删操作
           if (mPendingINotify && mPendingEventIndex >= mPendingEventCount) {
             //读取mInotifyFd中的事件，同时对输入设备进行相应的加载与卸载操作。这个操作必须当InputReader将现有的输入事件
             //读取并处理完成后才能进行，因为现有的输入事件可能来自于需要被卸载的输入设备，inputReader处理这些事件依赖于
             //对应的设备信息
                        mPendingINotify = false;
                        readNotifyLocked();
                        deviceChanged = true;
                    }

           //设备节点增删操作发生时，则重新执行循环体，以便将设备变化的事件放入buffer中
           if (deviceChanged) {
                        continue;
          }
          //如果此次getEvent调用成功获取了一些事件，或者要求唤醒InputReader，则退出循环并结束getEvent的调用，使InputReader
          //可以立即处理事件
           if (event != buffer || awoken) {
                        break;
                    }
         //4 如果此次getEvent调用没能获取事件，说明mPendingEventItem没有事件可用。于是执行epoll_wait函数等待新的事件到来，
         //将结果存储到mPendingEventItems里，并重置mPendingEventIndex为0
          mPendingEventIndex = 0;

         int pollResult = epoll_wait(mEpollFd, mPendingEventItems, EPOLL_MAX_EVENTS, timeoutMillis);
          mPendingEventCount = size_t(pollResult);
         //从epoll_wait中得到新的事件后，重新循环，对新事件进行处理
         }
         //返回本次getEvents调用所读取的事件数量
          return event - buffer;
}
```