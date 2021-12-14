http://liuwangshu.cn/framework/ims/3-inputdispatcher.html android8.1


前言
在上一篇文章中，我们学习了输入事件的处理，输入事件会交由InputDispatcher进行分发，那么InputDispatcher是如何进行分发的？这篇文章会给你答案。

1.InputReader的加工类型
在Android输入系统（二）IMS的启动过程和输入事件的处理这篇文章中，我们知道InputReader会对原始输入事件进行加工，如果事件的类型为按键类型的事件，
就会调用如下一段代码。
frameworks/native/services/inputflinger/InputDispatcher.cpp
```
void InputDispatcher::notifyKey(const NotifyKeyArgs* args) {
  ...
    bool needWake;
    {
    ...
    } // release lock
    if (needWake) {
        mLooper->wake();
    }
}
```

InputDispatcher的notifyKey方法用于唤醒InputDispatcherThread，它的参数NotifyKeyArgs是InputReader对按键类型的事件加工后得到的。
frameworks/native/services/inputflinger/InputListener.h
```
struct NotifyKeyArgs : public NotifyArgs {
    nsecs_t eventTime;
    int32_t deviceId;
    uint32_t source;
    uint32_t policyFlags;
    int32_t action;
    int32_t flags;
    int32_t keyCode;
    int32_t scanCode;
    int32_t metaState;
    nsecs_t downTime;
    inline NotifyKeyArgs() { }
    NotifyKeyArgs(nsecs_t eventTime, int32_t deviceId, uint32_t source, uint32_t policyFlags,
            int32_t action, int32_t flags, int32_t keyCode, int32_t scanCode,
            int32_t metaState, nsecs_t downTime);
    NotifyKeyArgs(const NotifyKeyArgs& other);
    virtual ~NotifyKeyArgs() { }
    virtual void notify(const sp<InputListenerInterface>& listener) const;
};
```
可以看到，NotifyKeyArgs结构体继承自NotifyArgs结构体，如下图所示
输入系统_NotifyArgs体系.png

NotifyArgs有三个子类，分别是NotifyKeyArgs、NotifyMotionArgs和NotifySwichArgs，这说明InputReader对原始输入事件加工后，
最终会得出三种事件类型，分别是key事件、Motion事件和Swich事件，这些事件会交由InputDispatcher来进行分发，如下图所示。
InputDispatcher对NotifyArgs的分发.png


2.InputDispatcher的分发过程
不同的事件类型有着不同的分发过程，其中Swich事件的处理是没有派发过程的，在InputDispatcher的notifySwitch函数中会
  将Switch事件交由InputDispatcherPolicy来处理。本系列文章一直讲解key事件相关，这次换一下，以Motion事件的分发过程来进行举例，
  对key事件分发事件有兴趣的可以自行去看源码，本质上都差不多。


2.1 唤醒InputDispatcherThread
InputDispatcher的notifyMotion函数用来唤醒InputDispatcherThread。
frameworks/native/services/inputflinger/InputDispatcher.cpp
```
void InputDispatcher::notifyMotion(const NotifyMotionArgs* args) {
#if DEBUG_INBOUND_EVENT_DETAILS
...
#endif
    //检查Motion事件的参数是否有效
    if (!validateMotionEvent(args->action, args->actionButton,
                args->pointerCount, args->pointerProperties)) {//1
        return;
    }
    uint32_t policyFlags = args->policyFlags;
    policyFlags |= POLICY_FLAG_TRUSTED;
    mPolicy->interceptMotionBeforeQueueing(args->eventTime, /*byref*/ policyFlags);
    bool needWake;
    { // acquire lock
        mLock.lock();
        //Motion事件是否需要交由InputFilter过滤
        if (shouldSendMotionToInputFilterLocked(args)) {//2
            mLock.unlock();
            MotionEvent event;
            //初始化MotionEvent，将NotifyMotionArgs中的参数信息赋值给MotionEvent中的参数
            event.initialize(args->deviceId, args->source, args->action, args->actionButton,
                    args->flags, args->edgeFlags, args->metaState, args->buttonState,
                    0, 0, args->xPrecision, args->yPrecision,
                    args->downTime, args->eventTime,
                    args->pointerCount, args->pointerProperties, args->pointerCoords);
           //表示已经过滤了
            policyFlags |= POLICY_FLAG_FILTERED;
            //开始过滤，如果返回值为false，就会直接return，这次事件不再进行分发
            if (!mPolicy->filterInputEvent(&event, policyFlags)) {//3
                return; // event was consumed by the filter
            }
            mLock.lock();
        }
        /**
        * 4
        */
        MotionEntry* newEntry = new MotionEntry(args->eventTime,
                args->deviceId, args->source, policyFlags,
                args->action, args->actionButton, args->flags,
                args->metaState, args->buttonState,
                args->edgeFlags, args->xPrecision, args->yPrecision, args->downTime,
                args->displayId,
                args->pointerCount, args->pointerProperties, args->pointerCoords, 0, 0);
        needWake = enqueueInboundEventLocked(newEntry);//5
        mLock.unlock();
    } // release lock
    if (needWake) {
        mLooper->wake();//6
    }
}
```

注释1处用于检查Motion事件的参数是否有效，其内部会检查触控点的数量pointerCount是否在合理范围内（小于1或者大于16都是不合理的），
以及触控点的ID是否在合理范围内（小于0或者大于31都是不合理的）。
注释2处如果Motion事件需要交由InputFilter过滤，就会初始化MotionEvent，其作用就是用NotifyMotionArgs中的事件参数信息构造一个MotionEvent，
   接着MotionEven会交给注释3处的方法进行过滤，如果返回值为false，这次Motion事件就会被忽略掉。
注释4处，用NotifyMotionArgs中的事件参数信息构造一个MotionEntry对象。
注释5处将MotionEntry传入到enqueueInboundEventLocked函数中，其内部会将MotionEntry添加到InputDispatcher的mInboundQueue队列的末尾，
并返回一个值needWake，代表InputDispatcherThread是否需要唤醒，如果需要唤醒就调用注释6处的代码来唤醒InputDispatcherThread
//todo enqueueInboundEventLocked

2.2 InputDispatcher进行分发
InputDispatcherThread被唤醒后，会执行InputDispatcherThread的threadLoop函数：
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
    mLooper->pollOnce(timeoutMillis);
}
```
//todo pollOnce
注释1处用于检查InputDispatcher的缓存队列中是否有等待处理的命令，如果没有就会执行注释2处的dispatchOnceInnerLocked函数，
  用来将输入事件分发给合适的。
注释3处获取当前的时间，结合注释4处，得出InputDispatcherThread需要睡眠的时间为timeoutMillis。
   最后调用Looper的pollOnce函数使InputDispatcherThread进入睡眠状态，并将它的最长的睡眠的时间设置为timeoutMillis。
   当有输入事件产生时，InputReader就会将睡眠状态的InputDispatcherThread唤醒，InputDispatcher会重新开始分发输入事件。
查看注释2处的dispatchOnceInnerLocked函数是如何进行事件分发的。
frameworks/native/services/inputflinger/InputDispatcher.cpp
```
void InputDispatcher::dispatchOnceInnerLocked(nsecs_t* nextWakeupTime) {
    ...
    // 如果InputDispatcher被冻结，则不进行派发操作
    if (mDispatchFrozen) {
#if DEBUG_FOCUS
        ALOGD("Dispatch frozen.  Waiting some more.");
#endif
        return;
    }
    //如果isAppSwitchDue为true，说明没有及时响应HOME键等操作
   bool isAppSwitchDue = mAppSwitchDueTime <= currentTime;//1
    if (mAppSwitchDueTime < *nextWakeupTime) {//2
        *nextWakeupTime = mAppSwitchDueTime;
    }
   //如果还没有待分发的事件，去mInboundQueue中取出一个事件
    if (! mPendingEvent) {
        //如果mInboundQueue为空，并且没有待分发的事件，就return
        if (mInboundQueue.isEmpty()) {
            ...
            if (!mPendingEvent) {
                return;
            }
        } else {
            //如果mInboundQueue不为空，取队列头部的EventEntry赋值给mPendingEvent
            mPendingEvent = mInboundQueue.dequeueAtHead();
            traceInboundQueueLengthLocked();
        }
        if (mPendingEvent->policyFlags & POLICY_FLAG_PASS_TO_USER) {
            pokeUserActivityLocked(mPendingEvent);
        }
        resetANRTimeoutsLocked();
    }
    ALOG_ASSERT(mPendingEvent != NULL);
    bool done = false;
    DropReason dropReason = DROP_REASON_NOT_DROPPED;//3
   ...
    switch (mPendingEvent->type) {//4
    ...
    case EventEntry::TYPE_MOTION: {
        MotionEntry* typedEntry = static_cast<MotionEntry*>(mPendingEvent);
        //如果没有及时响应窗口切换操作
        if (dropReason == DROP_REASON_NOT_DROPPED && isAppSwitchDue) {
            dropReason = DROP_REASON_APP_SWITCH;
        }
        //事件过期
        if (dropReason == DROP_REASON_NOT_DROPPED
                && isStaleEventLocked(currentTime, typedEntry)) {
            dropReason = DROP_REASON_STALE;
        }
        //阻碍其他窗口获取事件
        if (dropReason == DROP_REASON_NOT_DROPPED && mNextUnblockedEvent) {
            dropReason = DROP_REASON_BLOCKED;
        }
        done = dispatchMotionLocked(currentTime, typedEntry,
                &dropReason, nextWakeupTime);//5
        break;
    }
    default:
        ALOG_ASSERT(false);
        break;
    }

    if (done) {
        if (dropReason != DROP_REASON_NOT_DROPPED) {
            dropInboundEventLocked(mPendingEvent, dropReason);
        }
        mLastDropReason = dropReason;
        //释放本次事件处理的对象
        releasePendingEventLocked();//6
        //使得InputDispatcher能够快速处理下一个分发事件
        *nextWakeupTime = LONG_LONG_MIN;//7
}
```

InputDispatcher的dispatchOnceInnerLocked函数的代码比较长，这里截取了和Motion事件的分发相关的主要源码。主要做了以下几件事。

InputDispatcher的冻结处理
  如果当前InputDispatcher被冻结，则不进行派发操作，InputDispatcher有三种状态，分别是正常状态、冻结状态和禁用状态，
   可以通过InputDispatcher的setInputDispatchMode函数来设置。
窗口切换操作处理
   注释1处的mAppSwitchDueTime ，代表了App最近发生窗口切换操作时（比如按下Home键、挂断电话），该操作事件最迟的分发时间。
   如果这个时候，mAppSwitchDueTime小于等于当前系统时间，说明没有及时响应窗口切换操作，则isAppSwitchDue的值设置为true。
   注释2处，如果mAppSwitchDueTime小于nextWakeupTime（下一次InputDispatcherThread醒来的时间），就将mAppSwitchDueTime赋值给nextWakeupTime，
   这样当InputDispatcher处理完分发事件后，会第一时间处理窗口切换操作。
取出事件
  如果没有待分发的事件，就从mInboundQueue中取出一个事件，如果mInboundQueue为空，并且没有待分发的事件，就return，
  如果mInboundQueue不为空，取队列头部的EventEntry赋值给mPendingEvent，mPendingEvent的类型为EventEntry对象指针。
事件丢弃
  注释3处的dropReason代表了事件丢弃的原因，它的默认值为DROP_REASON_NOT_DROPPED，代表事件不被丢弃。
  注释4处根据mPendingEvent的type做区分处理，这里主要截取了对Motion类型的处理。经过过滤，会调用注释5处的dispatchMotionLocked函数
  为这个事件寻找合适的窗口。
后续处理
  如果注释5处的事件分发成功，则会在注释6处调用releasePendingEventLocked函数，其内部会将mPendingEvent的值设置为Null，
  并将mPendingEvent指向的对象内存释放掉。注释7处将nextWakeupTime的值设置为LONG_LONG_MIN，
  这是为了让InputDispatcher能够快速处理下一个分发事件。

后记
本文讲解了InputReader的加工类型和InputDispatcher的分发过程，由于文章篇幅的原因，InputDispatcher的分发过程还有一部分没有讲解，
这一部分就是事件分发到目标窗口的过程，会在本系列的下一篇文章进行讲解