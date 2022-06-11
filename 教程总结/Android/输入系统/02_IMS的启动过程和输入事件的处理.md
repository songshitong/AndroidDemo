http://liuwangshu.cn/framework/ims/2-inputevent.html android8.1

前言
在上一篇文章中，我们学习了IMS的诞生（创建），IMS创建后还会进行启动，这篇文章我们来学习IMS的启动过程和输入事件的处理。

1.IMS的启动过程
IMS的创建在SystemServer的startOtherServices方法中，不了解请查看Android输入系统（一）输入事件传递流程和InputManagerService的诞生这篇文章。
frameworks/base/services/java/com/android/server/SystemServer.java
```
 private void startOtherServices() {
 ...
         traceBeginAndSlog("StartInputManagerService");
         inputManager = new InputManagerService(context);
         traceEnd();
 ...
         traceBeginAndSlog("StartInputManager");
         inputManager.setWindowManagerCallbacks(wm.getInputMonitor());
         inputManager.start();
         traceEnd();

}
```

创建IMS后就会紧接着执行IMS的启动。IMS的start方法如下所示。
frameworks/base/services/core/java/com/android/server/input/InputManagerService.java
```
public void start() {
       Slog.i(TAG, "Starting input manager");
       nativeStart(mPtr);
       // Add ourself to the Watchdog monitors.
       Watchdog.getInstance().addMonitor(this);
     ...
   }
```

IMS的start方法中，会将自身添加到Watchdog中进行监控，用于定时检测系统关键服务（AMS和WMS等）是否可能发生死锁。
nativeStart方法对应的JNI层的函数是什么呢？查看com_android_server_input_InputManagerService的gInputManagerMethods数组，
不理解JNI的可以查看深入理解JNI系列文章。
frameworks/base/services/core/jni/com_android_server_input_InputManagerService.cpp
```
static const JNINativeMethod gInputManagerMethods[] = {
...
   { "nativeStart", "(J)V",
            (void*) nativeStart },
...
}
```

nativeStart方法对应的JNI函数为nativeStart：
frameworks/base/services/core/jni/com_android_server_input_InputManagerService.cpp
```
static void nativeStart(JNIEnv* env, jclass /* clazz */, jlong ptr) {
    NativeInputManager* im = reinterpret_cast<NativeInputManager*>(ptr);
    status_t result = im->getInputManager()->start();//1
    if (result) {
        jniThrowRuntimeException(env, "Input manager could not be started.");
    }
}
```
用reinterpret_cast操作符将jlong类型的ptr强制转换为原类型（NativeInputManager指针类型）。
注释1处会调用InputManager的start函数。
frameworks/native/services/inputflinger/InputManager.cpp
```
void InputManager::initialize() {
    mReaderThread = new InputReaderThread(mReader);
    mDispatcherThread = new InputDispatcherThread(mDispatcher);
}
status_t InputManager::start() {
    status_t result = mDispatcherThread->run("InputDispatcher", PRIORITY_URGENT_DISPLAY);
    if (result) {
        ALOGE("Could not start InputDispatcher thread due to error %d.", result);
        return result;
    }
    result = mReaderThread->run("InputReader", PRIORITY_URGENT_DISPLAY);
    if (result) {
        ALOGE("Could not start InputReader thread due to error %d.", result);
        mDispatcherThread->requestExit();
        return result;
    }
    return OK;
}
```
可以看到InputManager的start函数运行了InputReaderThread和InputDispatcherThread，
这两个线程在Android输入系统（一）输入事件传递流程和InputManagerService的诞生提到过，
它们在InputManager的构造函数中被创建，其中InputReaderThread中运行了InputReader，
InputDispatcherThread中运行了InputDispatcher


2.InputDispatcher的启动过程
先来回顾下InputDispatcher和InputReader是在哪创建的，InputManager的构造函数如下所示。
frameworks/native/services/inputflinger/InputManager.cpp
```
InputManager::InputManager(
        const sp<EventHubInterface>& eventHub,
        const sp<InputReaderPolicyInterface>& readerPolicy,
        const sp<InputDispatcherPolicyInterface>& dispatcherPolicy) {
    mDispatcher = new InputDispatcher(dispatcherPolicy);
    mReader = new InputReader(eventHub, readerPolicy, mDispatcher);
    initialize();
}
```

可以看到InputDispatcher和InputReader是有关联的，InputDispatcher会作为一个参数传入到InputReader中。
InputDispatcher是在InputReader之前创建的，这个顺序不能改变，因为要确保InputReader将加工后的输入事件交给InputDispatcher时，
InputDispatcher已经被创建。
InputDispatcher的定义如下所示。
frameworks/native/services/inputflinger/InputDispatcher.h
```
class InputDispatcherThread : public Thread {
public:
    explicit InputDispatcherThread(const sp<InputDispatcherInterface>& dispatcher);
    ~InputDispatcherThread();
private:
    virtual bool threadLoop();
    sp<InputDispatcherInterface> mDispatcher;
};
}
```

InputDispatcher.h中定义了threadLoop纯虚函数，InputDispatcher继承了Thread。native的Thread内部有一个循环，
当线程运行时，会调用threadLoop函数，如果它返回true并且没有调用requestExit函数，就会接着循环调用threadLoop函数。
查看InputDispatcherThread的threadLoop函数是如何实现的。
frameworks/native/services/inputflinger/InputDispatcher.cpp
```
bool InputDispatcherThread::threadLoop() {
    mDispatcher->dispatchOnce();
    return true;
}
```

threadLoop函数中只调用了InputDispatcher的dispatchOnce函数：
frameworks/native/services/inputflinger/InputDispatcher.cpp
```
void InputDispatcher::dispatchOnce() {
    nsecs_t nextWakeupTime = LONG_LONG_MAX;
    { // acquire lock
        AutoMutex _l(mLock);
        mDispatcherIsAliveCondition.broadcast();
        if (!haveCommandsLocked()) {//1
            dispatchOnceInnerLocked(&nextWakeupTime);//2
        }
        if (runCommandsLockedInterruptible()) {
            nextWakeupTime = LONG_LONG_MIN;
        }
    } // release lock
    nsecs_t currentTime = now();//3
    int timeoutMillis = toMillisecondTimeoutDelay(currentTime, nextWakeupTime);//4
    //利用epoll阻塞线程，就是handler的native Looper
    mLooper->pollOnce(timeoutMillis);
}
```

注释1处用于检查InputDispatcher的缓存队列中是否有等待处理 的命令，如果没有就会执行注释2处的dispatchOnceInnerLocked函数，
  用来将输入事件分发给合适的Window。
注释3处获取当前的时间，结合注释4处，得出InputDispatcherThread需要睡眠的时间为timeoutMillis。
    最后调用Looper的pollOnce函数使InputDispatcherThread进入睡眠状态，并将它的最长的睡眠的时间设置为timeoutMillis。
    当有输入事件产生时，InputReader就会将睡眠状态的InputDispatcher唤醒，InputDispatcher会重新开始分发输入事件。
那么InputReader是如何唤醒InputDispatcherThread的呢? 我们接着往下看

3.InputReader处理事件过程
InputReader是在InputReaderThread中启动的，InputReaderThread和InputDispatcherThread的定义是类似的，
也是继承了Thread并定义了threadLoop纯虚函数。如果处理的事件为键盘输入事件，则调用时序图如下所示
InputReader调用时序图.png

InputReaderThread的threadLoop函数如下所示。
frameworks/native/services/inputflinger/InputReader.cpp
```
bool InputReaderThread::threadLoop() {
    mReader->loopOnce();
    return true;
}
```

threadLoop函数中只调用了InputReader的loopOnce函数：
frameworks/native/services/inputflinger/InputReader.cpp
```
void InputReader::loopOnce() {
  ...
    //通过EventHub的getEvents函数获取事件信息存在mEventBuffer中
    size_t count = mEventHub->getEvents(timeoutMillis, mEventBuffer, EVENT_BUFFER_SIZE);//1
    { // acquire lock
        AutoMutex _l(mLock);
        mReaderIsAliveCondition.broadcast();
        if (count) {
            //如果有事件信息，调用processEventsLocked函数对事件进行加工处理
            processEventsLocked(mEventBuffer, count);//2
        }
    ...
}
```

注释1处调用EventHub的getEvents函数来获取设备节点的事件信息到mEventBuffer中，事件信息主要有两种，一种是设备节点的增删事件（设备事件），
  一种是原始输入事件。
注释2处的processEventsLocked函数用于对mEventBuffer中的原始输入事件信息进行加工处理，加工后的输入事件会交由InputDispatcher来处理，
  processEventsLocked函数如下所示。
frameworks/native/services/inputflinger/InputReader.cpp
```
void InputReader::processEventsLocked(const RawEvent* rawEvents, size_t count) {
    //遍历所有的事件
    for (const RawEvent* rawEvent = rawEvents; count;) {
        int32_t type = rawEvent->type;
        size_t batchSize = 1;
        //事件类型分为原始输入事件和设备事件，这个条件语句对原始输入事件进行处理
        if (type < EventHubInterface::FIRST_SYNTHETIC_EVENT) {
            int32_t deviceId = rawEvent->deviceId;
            while (batchSize < count) {
                if (rawEvent[batchSize].type >= EventHubInterface::FIRST_SYNTHETIC_EVENT
                        || rawEvent[batchSize].deviceId != deviceId) {
                    break;
                }
                batchSize += 1;
            }
#if DEBUG_RAW_EVENTS
            ALOGD("BatchSize: %d Count: %d", batchSize, count);
#endif
         //处理deviceId所对应的设备的原始输入事件
            processEventsForDeviceLocked(deviceId, rawEvent, batchSize);//1
        } else {
        //对设备事件进行处理
            switch (rawEvent->type) {
            case EventHubInterface::DEVICE_ADDED:
                addDeviceLocked(rawEvent->when, rawEvent->deviceId);
                break;
            case EventHubInterface::DEVICE_REMOVED:
                removeDeviceLocked(rawEvent->when, rawEvent->deviceId);
                break;
            case EventHubInterface::FINISHED_DEVICE_SCAN:
                handleConfigurationChangedLocked(rawEvent->when);
                break;
            default:
                ALOG_ASSERT(false); // can't happen
                break;
            }
        }
        count -= batchSize;
        rawEvent += batchSize;
    }
}
```
//todo addDeviceLocked EventHub相关
InputReader的processEventsLocked函数首先遍历了所有的事件，这些事件用RawEvent对象来表示，将原始输入事件和设备事件分开处理，
其中设备事件分为DEVICE_ADDED、DEVICE_REMOVED和FINISHED_DEVICE_SCAN，这些事件是在EventHub的getEvent函数中生成的。
如果是DEVICE_ADDED事件（设备添加事件），InputReader会新建InputDevice对象，用来存储设备信息，并且会将InputDevice存储在
KeyedVector类型的容器mDevices中。
同一个设备的输入事件交给processEventsForDeviceLocked函数来处理。
frameworks/native/services/inputflinger/InputReader.cpp
```
void InputReader::processEventsForDeviceLocked(int32_t deviceId,
        const RawEvent* rawEvents, size_t count) {
    ssize_t deviceIndex = mDevices.indexOfKey(deviceId);//1
    if (deviceIndex < 0) {
        ALOGW("Discarding event for unknown deviceId %d.", deviceId);
        return;
    }
    InputDevice* device = mDevices.valueAt(deviceIndex);//2
    if (device->isIgnored()) {
        //ALOGD("Discarding event for ignored deviceId %d.", deviceId);
        return;
    }
    device->process(rawEvents, count);
}
```

注释1处根据deviceId从mDevices中获取对应的deviceIndex，
注释2处再根据这个deviceIndex从mDevices中获取对应的InputDevice.最后会调用InputDevice的process函数：
frameworks/native/services/inputflinger/InputReader.cpp
```
void InputDevice::process(const RawEvent* rawEvents, size_t count) {*
    size_t numMappers = mMappers.size();
    //遍历处理该InputDevice所有的事件
    for (const RawEvent* rawEvent = rawEvents; count--; rawEvent++) {
#if DEBUG_RAW_EVENTS
        ALOGD("Input event: device=%d type=0x%04x code=0x%04x value=0x%08x when=%lld",
                rawEvent->deviceId, rawEvent->type, rawEvent->code, rawEvent->value,
                rawEvent->when);
#endif
        //mDropUntilNextSync的值默认为false，如果设备的输入事件缓冲区溢出，这个值会置为true。
        if (mDropUntilNextSync) {
            ...
        } else {
            for (size_t i = 0; i < numMappers; i++) {//1
                InputMapper* mapper = mMappers[i];
                mapper->process(rawEvent);//2
            }
        }
    }
}
```
首先会遍历InputDevice中的所有的事件，真正加工原始输入事件的是InputMapper对象，由于原始输入事件的类型很多，因此在InputMapper有很多子类，用于加工不同的原始输入事件，比如KeyboardInputMapper用于处理键盘输入事件，TouchInputMapper用于处理触摸输入事件。
注释1处遍历所有的InputMapper，在注释2处将原始输入事件交由这些InputMapper来处理，至于是哪个InputMapper来处理，InputReader并不关心。
这里就以处理键盘输入事件为例，KeyboardInputMapper的process函数如下所示。
frameworks/native/services/inputflinger/InputReader.cpp
```
void KeyboardInputMapper::process(const RawEvent* rawEvent) {
    switch (rawEvent->type) {
    case EV_KEY: {//1
        int32_t scanCode = rawEvent->code;
        int32_t usageCode = mCurrentHidUsage;
        mCurrentHidUsage = 0;
        if (isKeyboardOrGamepadKey(scanCode)) {
            processKey(rawEvent->when, rawEvent->value != 0, scanCode, usageCode);//2
        }
        break;
    }
   ...
    }
}
```
注释1处，如果事件的类型为按键类型的事件，就会调用注释2处的KeyboardInputMapper的processKey函数。
frameworks/native/services/inputflinger/InputReader.cpp
```
void KeyboardInputMapper::processKey(nsecs_t when, bool down, int32_t scanCode,
        int32_t usageCode) {
   ...
    NotifyKeyArgs args(when, getDeviceId(), mSource, policyFlags,
            down ? AKEY_EVENT_ACTION_DOWN : AKEY_EVENT_ACTION_UP,
            AKEY_EVENT_FLAG_FROM_SYSTEM, keyCode, scanCode, keyMetaState, downTime);
    getListener()->notifyKey(&args);//1
}
```

processKey函数会将加工后的键盘输入事件封装为NotifyKeyArgs，将NotifyKeyArgs通知给InputListenerInterface。
InputDispatcher继承了InputDispatcherInterface，而InputDispatcherInterface继承了InputListenerInterface，
因此注释1处实际上是调用了InputDispatcher的notifyKey函数，将NotifyKeyArgs交给InputDispatcher处理。
frameworks/native/services/inputflinger/InputDispatcher.cpp
```
void InputDispatcher::notifyKey(const NotifyKeyArgs* args) {
  ...
    bool needWake;
    { // acquire lock
        mLock.lock();
        if (shouldSendKeyToInputFilterLocked(args)) {
            mLock.unlock();
            policyFlags |= POLICY_FLAG_FILTERED;
            if (!mPolicy->filterInputEvent(&event, policyFlags)) {
                return; // event was consumed by the filter
            }
            mLock.lock();
        }
        int32_t repeatCount = 0;
        KeyEntry* newEntry = new KeyEntry(args->eventTime,
                args->deviceId, args->source, policyFlags,
                args->action, flags, keyCode, args->scanCode,
                metaState, repeatCount, args->downTime);//1
        needWake = enqueueInboundEventLocked(newEntry);//2
        mLock.unlock();
    } // release lock
    if (needWake) {
        //唤醒looper，查看handler的native looper
        mLooper->wake();
    }
}
```
代码块中采用Mutex互斥锁的形式，在注释1处根据NotifyKeyArgs，重新封装一个KeyEntry对象，代表一次按键数据。
注释2处根据KeyEntry，来判断是否需要将睡眠中的InputDispatcherThread唤醒，如果需要，就调用Looper的wake函数进行唤醒，
  InputDispatcherThread被唤醒后就会重新对输入事件的分发，具体的回头查看第2小节


总结
本文涉及到了四个关键的类，分别是IMS、EventHub、InputDispatcher和InputReader，它们做了如下的工作：

IMS启动了InputDispatcherThread和InputReaderThread，分别用来运行InputDispatcher和InputReader。
InputDispatcher先于InputReader被创建，InputDispatcher的dispatchOnceInnerLocked函数用来将事件分发给合适的Window。
   InputDispatcher没有输入事件处理时会进入睡眠状态，等待InputReader通知唤醒。
InputReader通过EventHub的getEvents函数获取事件信息，如果是原始输入事件，就将这些原始输入事件交由不同的InputMapper来处理，
   最终交由InputDispatcher来进行分发。
InputDispatcher的notifyKey函数中会根据按键数据来判断InputDispatcher是否要被唤醒，InputDispatcher被唤醒后，
   会重新调用dispatchOnceInnerLocked函数将输入事件分发给合适的Window。

IMS输入事件处理.md