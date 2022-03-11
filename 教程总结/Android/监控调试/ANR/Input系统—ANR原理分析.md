http://gityuan.com/2017/01/01/input-anr/
android 8.0
一. 概述
当input事件处理得慢就会触发ANR，那ANR内部原理是什么，哪些场景会产生ANR呢。 


二. ANR处理流程    findFocusedWindowTargetsLocked是dispatcher派发事件到目标窗口的方法
ANR时间区间便是指当前这次的事件dispatch过程中执行findFocusedWindowTargetsLocked()方法到下一次执行resetANRTimeoutsLocked()的时间区间. 
  以下5个时机会reset. 都位于InputDispatcher.cpp文件:
  1 resetAndDropEverythingLocked
  2 releasePendingEventLocked
  3 setFocusedApplication
  4 dispatchOnceInnerLocked
  5 setInputDispatchMode
简单来说, 主要是以下4个场景,会有机会执行resetANRTimeoutsLocked:
 1 解冻屏幕, 系统开/关机的时刻点 (thawInputDispatchingLw, setEventDispatchingLw)
 2 wms聚焦app的改变 (WMS.setFocusedApp, WMS.removeAppToken)
 3 设置input filter的过程 (IMS.setInputFilter)
 4 再次分发事件的过程(dispatchOnceInnerLocked)
当InputDispatcher线程 findFocusedWindowTargetsLocked()过程调用到handleTargetsNotReadyLocked，且满足超时5s的情况则会调用onANRLocked().
  handleTargetsNotReadyLocked会调用onANRLocked

2.1 onANRLocked
frameworks/native/services/inputflinger/InputDispatcher.cpp
```
void InputDispatcher::onANRLocked(
        nsecs_t currentTime, const sp<InputApplicationHandle>& applicationHandle,
        const sp<InputWindowHandle>& windowHandle,
        nsecs_t eventTime, nsecs_t waitStartTime, const char* reason) {
    float dispatchLatency = (currentTime - eventTime) * 0.000001f;
    float waitDuration = (currentTime - waitStartTime) * 0.000001f;

    ALOGI("Application is not responding: %s. "
            "It has been %0.1fms since event, %0.1fms since wait started. Reason: %s",
            getApplicationWindowLabelLocked(applicationHandle, windowHandle).string(),
            dispatchLatency, waitDuration, reason);

    //捕获ANR的现场信息
    time_t t = time(NULL);
    struct tm tm;
    localtime_r(&t, &tm);
    char timestr[64];
    strftime(timestr, sizeof(timestr), "%F %T", &tm);
    mLastANRState.clear();
    mLastANRState.append(INDENT "ANR:\n");
    mLastANRState.appendFormat(INDENT2 "Time: %s\n", timestr);
    mLastANRState.appendFormat(INDENT2 "Window: %s\n",
            getApplicationWindowLabelLocked(applicationHandle, windowHandle).string());
    mLastANRState.appendFormat(INDENT2 "DispatchLatency: %0.1fms\n", dispatchLatency);
    mLastANRState.appendFormat(INDENT2 "WaitDuration: %0.1fms\n", waitDuration);
    mLastANRState.appendFormat(INDENT2 "Reason: %s\n", reason);
    dumpDispatchStateLocked(mLastANRState);

    //将ANR命令加入mCommandQueue
    CommandEntry* commandEntry = postCommandLocked(
            & InputDispatcher::doNotifyANRLockedInterruptible);
    commandEntry->inputApplicationHandle = applicationHandle;
    commandEntry->inputWindowHandle = windowHandle;
    commandEntry->reason = reason;
}
```
发生ANR调用onANRLocked()的过程会将doNotifyANRLockedInterruptible加入mCommandQueue。 
  在下一轮InputDispatcher.dispatchOnce的过程中会先执行runCommandsLockedInterruptible()方法，
  取出 mCommandQueue队列的所有命令逐一执行。那么ANR所对应的命令doNotifyANRLockedInterruptible，接下来看该方法。

3.2 doNotifyANRLockedInterruptible
```
void InputDispatcher::doNotifyANRLockedInterruptible(
        CommandEntry* commandEntry) {
    mLock.unlock();

    //[见小节3.3]  mPolicy是指NativeInputManager
    nsecs_t newTimeout = mPolicy->notifyANR(
            commandEntry->inputApplicationHandle, commandEntry->inputWindowHandle,
            commandEntry->reason);

    mLock.lock();
    //newTimeout =5s [见小节3.8]
    resumeAfterTargetsNotReadyTimeoutLocked(newTimeout,
            commandEntry->inputWindowHandle != NULL
                    ? commandEntry->inputWindowHandle->getInputChannel() : NULL);
}
```

3.3 NativeInputManager.notifyANR
frameworks/base/services/core/jni/com_android_server_input_InputManagerService.cpp
```
nsecs_t NativeInputManager::notifyANR(const sp<InputApplicationHandle>& inputApplicationHandle,
        const sp<InputWindowHandle>& inputWindowHandle, const String8& reason) {
    JNIEnv* env = jniEnv();

    jobject inputApplicationHandleObj =
            getInputApplicationHandleObjLocalRef(env, inputApplicationHandle);
    jobject inputWindowHandleObj =
            getInputWindowHandleObjLocalRef(env, inputWindowHandle);
    jstring reasonObj = env->NewStringUTF(reason.string());

    //调用Java方法[见小节3.4]
    jlong newTimeout = env->CallLongMethod(mServiceObj,
                gServiceClassInfo.notifyANR, inputApplicationHandleObj, inputWindowHandleObj,
                reasonObj);
    if (checkAndClearExceptionFromCallback(env, "notifyANR")) {
        newTimeout = 0; //抛出异常,则清理并重置timeout
    }
    ...
    return newTimeout;
}
```
先看看register_android_server_InputManager过程：
```
int register_android_server_InputManager(JNIEnv* env) {
    int res = jniRegisterNativeMethods(env, "com/android/server/input/InputManagerService",
            gInputManagerMethods, NELEM(gInputManagerMethods));

    jclass clazz;
    FIND_CLASS(clazz, "com/android/server/input/InputManagerService");
    ...
    GET_METHOD_ID(gServiceClassInfo.notifyANR, clazz,
            "notifyANR",
            "(Lcom/android/server/input/InputApplicationHandle;Lcom/android/server/input/InputWindowHandle;Ljava/lang/String;)J");
    ...
}
```
可知gServiceClassInfo.notifyANR是指IMS.notifyANR

3.4 IMS.notifyANR
frameworks/base/services/core/java/com/android/server/input/InputManagerService.java
```
 private long notifyANR(InputApplicationHandle inputApplicationHandle,
            InputWindowHandle inputWindowHandle, String reason) {
        //此处mWindowManagerCallbacks是指InputMonitor对象    
        return mWindowManagerCallbacks.notifyANR(
                inputApplicationHandle, inputWindowHandle, reason);
    }
```

3.5 InputMonitor.notifyANR
frameworks/base/services/core/java/com/android/server/wm/InputMonitor.java
```
public long notifyANR(InputApplicationHandle inputApplicationHandle,
            InputWindowHandle inputWindowHandle, String reason) {
        AppWindowToken appWindowToken = null;
        WindowState windowState = null;
        boolean aboveSystem = false;
        synchronized (mService.mWindowMap) {
            if (inputWindowHandle != null) {
                windowState = (WindowState) inputWindowHandle.windowState;
                if (windowState != null) {
                    appWindowToken = windowState.mAppToken;
                }
            }
            if (appWindowToken == null && inputApplicationHandle != null) {
                appWindowToken = (AppWindowToken)inputApplicationHandle.appWindowToken;
            }
           //输出input事件分发超时log
            if (windowState != null) {
                Slog.i(TAG_WM, "Input event dispatching timed out "
                        + "sending to " + windowState.mAttrs.getTitle()
                        + ".  Reason: " + reason);
                int systemAlertLayer = mService.mPolicy.getWindowLayerFromTypeLw(
                        TYPE_APPLICATION_OVERLAY, windowState.mOwnerCanAddInternalSystemWindow);
                aboveSystem = windowState.mBaseLayer > systemAlertLayer;
            } else if (appWindowToken != null) {
                Slog.i(TAG_WM, "Input event dispatching timed out "
                        + "sending to application " + appWindowToken.stringName
                        + ".  Reason: " + reason);
            } else {
                Slog.i(TAG_WM, "Input event dispatching timed out "
                        + ".  Reason: " + reason);
            }
            mService.saveANRStateLocked(appWindowToken, windowState, reason);
        }
        mService.mAmInternal.saveANRState(reason);

        if (appWindowToken != null && appWindowToken.appToken != null) {
            final AppWindowContainerController controller = appWindowToken.getController();
            //【见小节3.6.1】
            final boolean abort = controller != null
                    && controller.keyDispatchingTimedOut(reason,
                            (windowState != null) ? windowState.mSession.mPid : -1);
            if (!abort) {
                return appWindowToken.mInputDispatchingTimeoutNanos;
            }
        } else if (windowState != null) {
            try {
                //【见小节3.6.2】
                long timeout = ActivityManager.getService().inputDispatchingTimedOut(
                        windowState.mSession.mPid, aboveSystem, reason);
                if (timeout >= 0) {
                    return timeout * 1000000L; // nanoseconds
                }
            } catch (RemoteException ex) {
            }
        }
        return 0;
    }
```
发生input相关的ANR时在system log输出ANR信息,并且tag为WindowManager. 主要有3类log:
1 Input event dispatching timed out sending to [windowState.mAttrs.getTitle()]
2 Input event dispatching timed out sending to application [appWindowToken.stringName)]
3 Input event dispatching timed out sending.


3.6 DispatchingTimedOut
3.6.1 controller.keyDispatchingTimedOut
```
frameworks/base/services/core/java/com/android/server/wm/AppWindowContainerController.java
 boolean keyDispatchingTimedOut(String reason, int windowPid) {
        return mListener != null && mListener.keyDispatchingTimedOut(reason, windowPid);
    }

frameworks/base/services/core/java/com/android/server/am/ActivityRecord.java   
    public boolean keyDispatchingTimedOut(String reason, int windowPid) {
        ActivityRecord anrActivity;
        ProcessRecord anrApp;
        boolean windowFromSameProcessAsActivity;
        synchronized (service) {
            anrActivity = getWaitingHistoryRecordLocked();
            anrApp = app;
            windowFromSameProcessAsActivity =
                    app == null || app.pid == windowPid || windowPid == -1;
        }
        if (windowFromSameProcessAsActivity) {
            return service.inputDispatchingTimedOut(anrApp, anrActivity, this, false, reason);
        } else {
            return service.inputDispatchingTimedOut(windowPid, false /* aboveSystem */, reason) < 0;
        }
    } 
```
3.6.2 AMS.inputDispatchingTimedOut
frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java
```
public long inputDispatchingTimedOut(int pid, final boolean aboveSystem, String reason) {
    ...
    ProcessRecord proc;
    long timeout;
    synchronized (this) {
        synchronized (mPidsSelfLocked) {
            proc = mPidsSelfLocked.get(pid); //根据pid查看进程record
        }
        timeout = getInputDispatchingTimeoutLocked(proc);
    }
    //【见小节3.7】
    if (!inputDispatchingTimedOut(proc, null, null, aboveSystem, reason)) {
        return -1;
    }
    return timeout;
}
```
inputDispatching的超时为KEY_DISPATCHING_TIMEOUT，即timeout = 5s
```
// How long we wait until we timeout on key dispatching.
    static final int KEY_DISPATCHING_TIMEOUT = 5*1000;

    // How long we wait until we timeout on key dispatching during instrumentation.
    static final int INSTRUMENTATION_KEY_DISPATCHING_TIMEOUT = 60*1000;
  public static long getInputDispatchingTimeoutLocked(ProcessRecord r) {
        if (r != null && (r.instr != null || r.usingWrapper)) {
            return INSTRUMENTATION_KEY_DISPATCHING_TIMEOUT;
        }
        return KEY_DISPATCHING_TIMEOUT;
    }
```

3.7 AMS.inputDispatchingTimedOut
```
public boolean inputDispatchingTimedOut(final ProcessRecord proc, final ActivityRecord activity, final ActivityRecord parent, final boolean aboveSystem, String reason) {
    ...
    final String annotation;
    if (reason == null) {
        annotation = "Input dispatching timed out";
    } else {
        annotation = "Input dispatching timed out (" + reason + ")";
    }

    if (proc != null) {
        ...
        //通过handler机制，交由“ActivityManager”线程执行ANR处理过程。
        mHandler.post(new Runnable() {
            public void run() {
                mAppErrors.appNotResponding(proc, activity, parent, aboveSystem, annotation);
            }
        });
    }
    return true;
}
```
appNotResponding会输出现场的重要进程的trace等信息。 再回到【小节3.2】处理完ANR后再调用resumeAfterTargetsNotReadyTimeoutLocked。
appNotResponding后面走ANR信息收集流程

3.8 resumeAfterTargetsNotReadyTimeoutLocked
frameworks/native/services/inputflinger/InputDispatcher.cpp
```
void InputDispatcher::resumeAfterTargetsNotReadyTimeoutLocked(nsecs_t newTimeout,
        const sp<InputChannel>& inputChannel) {
    if (newTimeout > 0) {
        //超时时间增加5s
        mInputTargetWaitTimeoutTime = now() + newTimeout;
    } else {
        // Give up.
        mInputTargetWaitTimeoutExpired = true;

        // Input state will not be realistic. Mark it out of sync.
        if (inputChannel.get()) {
            ssize_t connectionIndex = getConnectionIndexLocked(inputChannel);
            if (connectionIndex >= 0) {
                sp<Connection> connection = mConnectionsByFd.valueAt(connectionIndex);
                sp<InputWindowHandle> windowHandle = connection->inputWindowHandle;

                if (windowHandle != NULL) {
                    const InputWindowInfo* info = windowHandle->getInfo();
                    if (info) {
                        ssize_t stateIndex = mTouchStatesByDisplay.indexOfKey(info->displayId);
                        if (stateIndex >= 0) {
                            mTouchStatesByDisplay.editValueAt(stateIndex).removeWindow(
                                    windowHandle);
                        }
                    }
                }

                if (connection->status == Connection::STATUS_NORMAL) {
                    CancelationOptions options(CancelationOptions::CANCEL_ALL_EVENTS,
                            "application not responding");
                    synthesizeCancelationEventsForConnectionLocked(connection, options);
                }
            }
        }
    }
}
```



四. input死锁监测机制
4.1 IMS.start      //todo wathdog机制     死锁如何检测 请求一次和释放一次锁？？
frameworks/base/services/core/java/com/android/server/input/InputManagerService.java
```
public void start() {
    ...
    Watchdog.getInstance().addMonitor(this);
    ...
}
```
InputManagerService实现了Watchdog.Monitor接口, 并且在启动过程将自己加入到了Watchdog线程的monitor队列.

4.2 IMS.monitor
Watchdog便会定时调用IMS.monitor()方法.
```
public void monitor() {
    synchronized (mInputFilterLock) { }
    nativeMonitor(mPtr);
}
```
nativeMonitor经过JNI调用,进如如下方法:
frameworks/base/services/core/jni/com_android_server_input_InputManagerService.cpp
```
static void nativeMonitor(JNIEnv*, jclass, jlong ptr) {
    NativeInputManager* im = reinterpret_cast<NativeInputManager*>(ptr);
    im->getInputManager()->getReader()->monitor(); //见小节4.3
    im->getInputManager()->getDispatcher()->monitor(); //见小节4.4
}
```

4.3 InputReader.monitor
```
frameworks/native/services/inputflinger/InputReader.cpp
void InputReader::monitor() {
    //请求和释放一次mLock,来确保reader没有发生死锁的问题
    mLock.lock();
    mEventHub->wake();
    mReaderIsAliveCondition.wait(mLock);
    mLock.unlock();

    //监测EventHub[见小节4.3.1]
    mEventHub->monitor();
}
```
获取mLock之后进入Condition类型的wait()方法,等待InputReader线程的loopOnce()中的broadcast()来唤醒.
```
void InputReader::loopOnce() {
    size_t count = mEventHub->getEvents(timeoutMillis, mEventBuffer, EVENT_BUFFER_SIZE);
    ...
    {
        AutoMutex _l(mLock);
        mReaderIsAliveCondition.broadcast();
        if (count) {
            processEventsLocked(mEventBuffer, count);
        }
    }
    ...
    mQueuedListener->flush();
}
```

4.3.1 EventHub.monitor
```
void EventHub::monitor() {
    //请求和释放一次mLock,来确保reader没有发生死锁的问题
    mLock.lock();
    mLock.unlock();
}
```

4.4 InputDispatcher
frameworks/native/services/inputflinger/InputDispatcher.cpp
```
void InputDispatcher::monitor() {
    mLock.lock();
    mLooper->wake();
    mDispatcherIsAliveCondition.wait(mLock);
    mLock.unlock();
}
```
获取mLock之后进入Condition类型的wait()方法,等待IInputDispatcher线程的dispatchOnce()中的broadcast()来唤醒.
```
void InputDispatcher::dispatchOnce() {
    nsecs_t nextWakeupTime = LONG_LONG_MAX;
    {
        AutoMutex _l(mLock);
        mDispatcherIsAliveCondition.broadcast();
        if (!haveCommandsLocked()) {
            dispatchOnceInnerLocked(&nextWakeupTime);
        }
        if (runCommandsLockedInterruptible()) {
            nextWakeupTime = LONG_LONG_MIN;
        }
    }

    nsecs_t currentTime = now();
    int timeoutMillis = toMillisecondTimeoutDelay(currentTime, nextWakeupTime);
    mLooper->pollOnce(timeoutMillis); //进入epoll_wait
}
```
4.5 小节
通过将InputManagerService加入到Watchdog的monitor队列,定时监测是否发生死锁. 整个监测过涉及EventHub, InputReader, 
  InputDispatcher, InputManagerService的死锁监测. 监测的原理很简单,通过尝试获取锁并释放锁的方式.

最后, 可通过adb shell dumpsys input来查看手机当前的input状态, 输出内容分别为EventHub.dump(), InputReader.dump(),
  InputDispatcher.dump()这3类,另外如果发生过input ANR,那么也会输出上一个ANR的状态.

其中mPendingEvent代表的当下正在处理的事件.


五. 总结
5.1 ANR分类
由小节[3.5] InputMonitor.notifyANR完成, 当发生ANR时system log中会出现以下信息, 并且TAG=WindowManager:

Input event dispatching timed out xxx. Reason: + reason, 其中xxx取值:
 1 窗口类型: sending to windowState.mAttrs.getTitle()
 2 应用类型: sending to application appWindowToken.stringName
 3 其他类型: 则为空.

至于Reason主要有以下类型:
5.1.1 reason类型     checkWindowReadyForMoreInputLocked可以查看输入系统与ANR
由小节[2.3.1]checkWindowReadyForMoreInputLocked完成， ANR reason主要有以下几类：
1 无窗口, 有应用：Waiting because no window has focus but there is a focused application that may eventually add a window when 
   it finishes starting up.
2 窗口暂停: Waiting because the [targetType] window is paused.
3 窗口未连接: Waiting because the [targetType] window’s input channel is not registered with the input dispatcher. 
    The window may be in the process of being removed.
4 窗口连接已死亡：Waiting because the [targetType] window’s input connection is [Connection.Status]. 
   The window may be in the process of being removed.
5 窗口连接已满：Waiting because the [targetType] window’s input channel is full. Outbound queue length: [outboundQueue长度]. 
  Wait queue length: [waitQueue长度].
6 按键事件，输出队列或事件等待队列不为空：Waiting to send key event because the [targetType] window has not finished processing all of the input events 
   that were previously delivered to it. Outbound queue length: [outboundQueue长度]. Wait queue length: [waitQueue长度].
7 非按键事件，事件等待队列不为空且头事件分发超时500ms：
  Waiting to send non-key event because the [targetType] window has not finished processing certain input events that
  were delivered to it over 500ms ago. Wait queue length: [waitQueue长度]. Wait queue head age: [等待时长].

其中
targetType: 取值为”focused”或者”touched”
Connection.Status: 取值为”NORMAL”，”BROKEN”，”ZOMBIE”
另外, findFocusedWindowTargetsLocked, findTouchedWindowTargetsLocked这两个方法中可以通过实现 updateDispatchStatisticsLocked()来分析anr问题.
 //todo


5.2 drop事件分类      dropInboundEventLocked参考03_InputReader的加工类型和InputDispatcher的分发过程.md
由小节[2.1.2] dropInboundEventLocked完成，输出事件丢弃的原因：
 1 DROP_REASON_POLICY: “inbound event was dropped because the policy consumed it”;
 2 DROP_REASON_DISABLED: “inbound event was dropped because input dispatch is disabled”;
 3 DROP_REASON_APP_SWITCH: “inbound event was dropped because of pending overdue app switch”;
 4 DROP_REASON_BLOCKED: “inbound event was dropped because the current application is not responding and the user has started interacting with a different application”;
 5 DROP_REASON_STALE: “inbound event was dropped because it is stale”;


其他:
doDispatchCycleFinishedLockedInterruptible的过程, 会记录分发时间超过2s的事件,
findFocusedWindowTargetsLocked的过程, 可以统计等待时长信息.