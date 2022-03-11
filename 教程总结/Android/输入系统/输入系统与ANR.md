Android 8.1  深入理解Android 卷三

//todo 5.3.1查找窗口的流程   这是派发到目标窗口 还是寻找焦点窗口？？
目标窗口查找时，作为派发目标的窗口必须已经准备好接收新的输入事件，否则判定窗口处于未响应状态，终止事件的派发过程，并在一段
时间后重试。倘若5S后窗口仍然未准备好接收输入事件，将导致ANR。直接引发ANR的原因有很多，例如Activity生命周期函数调用超时，
服务启动超时，输入事件处理超时等

从输入超时的角度讨论ANR产生原因与过程

按焦点查找目标窗口的函数
/frameworks/native/services/inputflinger/InputDispatcher.cpp
```
const nsecs_t STREAM_AHEAD_EVENT_TIMEOUT = 500 * 1000000LL;
int32_t InputDispatcher::findFocusedWindowTargetsLocked(nsecs_t currentTime,
          const EventEntry* entry, Vector<InputTarget>& inputTargets, nsecs_t* nextWakeupTime) {
      //检查窗口是否准备好接受新事件
      reason = checkWindowReadyForMoreInputLocked(currentTime,
              mFocusedWindowHandle, entry, "focused");
      if (!reason.isEmpty()) {
       //如果尚未准备好 通过handleTargetsNotReadyLocked对原因进行记录，并安排时间尝试重试派发，或者引发ANR
          injectionResult = handleTargetsNotReadyLocked(currentTime, entry,
                  mFocusedApplicationHandle, mFocusedWindowHandle, nextWakeupTime, reason.string());
          goto Unresponsive;
      }
     }

 String8 InputDispatcher::checkWindowReadyForMoreInputLocked(nsecs_t currentTime,
           const sp<InputWindowHandle>& windowHandle, const EventEntry* eventEntry,
           const char* targetType) {
    // If the window is paused then keep waiting.
    if (windowHandle->getInfo()->paused) {
        return String8::format("Waiting because the %s window is paused.", targetType);
    }

    // If the window's connection is not registered then keep waiting.
    ssize_t connectionIndex = getConnectionIndexLocked(windowHandle->getInputChannel());
    if (connectionIndex < 0) {
        return String8::format("Waiting because the %s window's input channel is not "
                "registered with the input dispatcher.  The window may be in the process "
                "of being removed.", targetType);
    }

    // If the connection is dead then keep waiting.
    sp<Connection> connection = mConnectionsByFd.valueAt(connectionIndex);
    if (connection->status != Connection::STATUS_NORMAL) {
        return String8::format("Waiting because the %s window's input connection is %s."
                "The window may be in the process of being removed.", targetType,
                connection->getStatusLabel());
    }
  // 共通原因 inputPublisher被阻塞
  if (connection->inputPublisherBlocked) {
            return String8::format("Waiting because the %s window's input channel is full.  "
                    "Outbound queue length: %d.  Wait queue length: %d.",
                    targetType, connection->outboundQueue.count(), connection->waitQueue.count());
        }
   //对按键事件来说，要求connection必须处于空闲状态
   if (eventEntry->type == EventEntry::TYPE_KEY) {
        if (!connection->outboundQueue.isEmpty() || !connection->waitQueue.isEmpty()) {
              return String8::format("Waiting to send key event because the %s window has not "
                      "finished processing all of the input events that were previously "
                      "delivered to it.  Outbound queue length: %d.  Wait queue length: %d.",
                      targetType, connection->outboundQueue.count(), connection->waitQueue.count());
   }else{
      //对motion事件来说，可以发送事件的条件相对宽松些，只要窗口能做0.5内发送反馈即可
      if (!connection->waitQueue.isEmpty()
                        && currentTime >= connection->waitQueue.head->deliveryTime
                                + STREAM_AHEAD_EVENT_TIMEOUT) {
                    return String8::format("Waiting to send non-key event because the %s window has not "
                            "finished processing certain input events that were delivered to it over "
                            "%0.1fms ago.  Wait queue length: %d.  Wait queue head age: %0.1fms.",
                            targetType, STREAM_AHEAD_EVENT_TIMEOUT * 0.000001f,
                            connection->waitQueue.count(),
                            (currentTime - connection->waitQueue.head->deliveryTime) * 0.000001f);
                }
   }
 }
```
可以看出判断窗口是否可以接受事件的依据有两个：inputPublisher是否被阻塞以及Connection两个队列的状态
InputPublisher的工作是将事件信息写入InputChannel中，如果窗口端因为某种原因迟迟未能从InputChannel中将事件读取就会导致
  SocketPair的写入缓冲区满
Connection两个队列的状态体现了发送循环的状态。如果两个队列至少有一个为空，则表示Connection正处于发送循环的过程中，
  否则处于空闲状态。
  对按键事件来说，仅当Connection处于空闲状态，也就是窗口已经完成之前事件的相应之后才会发送给窗口。因为之前的输入事件有可能
    会影响焦点窗口，进而影响按键事件的接收者。例如，用户快速的按下了两次BACK键，第一个back键将会发送给位于顶端的窗口，这个事件
    可能会导致窗口关闭，因此第一个BACk键的处理行为决定了第二个BACK键应该发送给哪个窗口。因此按键事件的发送要求窗口完成对
    所有之前事件的处理
  而motion事件的条件相对宽松些。允许Connection处于发送循环的过程中，但是如果等待队列的第一个事件没能在0.5s获得反馈，则
    判定窗口处于未响应状态。这是因为Motion事件具有实时性的特点---用户的意图就是希望输入事件发送给他看到的窗口，所以不在乎之前
    事件的处理结果



重试派发与ANR的引发
如果checkWindowReadyForMoreInputLocked判定窗口无法接收事件，则调用handleTargetsNotReadyLocked安排重试或引发ANR
/frameworks/native/services/inputflinger/InputDispatcher.cpp
```
int32_t InputDispatcher::handleTargetsNotReadyLocked(nsecs_t currentTime,
          const EventEntry* entry,
          const sp<InputApplicationHandle>& applicationHandle,
          const sp<InputWindowHandle>& windowHandle,
          nsecs_t* nextWakeupTime, const char* reason) {
 if (applicationHandle == NULL && windowHandle == NULL) {
   //这种情况说明系统尚未完成启动，可忽略
 }
 //1 如果是第一次发生窗口未响应的情况，则记录下未响应的窗口信息，并设置引发ANR的时间点
 if (mInputTargetWaitCause != INPUT_TARGET_WAIT_CAUSE_APPLICATION_NOT_READY) {
      nsecs_t timeout;
     if (windowHandle != NULL) {
       //如果有目标窗口，则获取由窗口指定的超时时间
         timeout = windowHandle->getDispatchingTimeout(DEFAULT_INPUT_DISPATCHING_TIMEOUT);
     } else if (applicationHandle != NULL) {
       //如果没有目标窗口，则从AMS获取超时时间
         timeout = applicationHandle->getDispatchingTimeout(
                 DEFAULT_INPUT_DISPATCHING_TIMEOUT);
     } else {
         timeout = DEFAULT_INPUT_DISPATCHING_TIMEOUT;
     }
      mInputTargetWaitCause = INPUT_TARGET_WAIT_CAUSE_APPLICATION_NOT_READY;
      //检测到未响应的时间
      mInputTargetWaitStartTime = currentTime;
      //设置引发anr的时间
      mInputTargetWaitTimeoutTime = currentTime + timeout;
      mInputTargetWaitTimeoutExpired = false;
      mInputTargetWaitApplicationHandle.clear();
 }
   //2检查是否引发ANR
     if (currentTime >= mInputTargetWaitTimeoutTime) {
     //当前时间大于引发ANR的时间后，则引发ANR
          onANRLocked(currentTime, applicationHandle, windowHandle,
                  entry->eventTime, mInputTargetWaitStartTime, reason);
          *nextWakeupTime = LONG_LONG_MIN;
          return INPUT_EVENT_INJECTION_PENDING;
      } else {
      //如果尚未到达引发ANR的时间点，设置nextWakeupTime后返回，等待下次再试
          if (mInputTargetWaitTimeoutTime < *nextWakeupTime) {
              *nextWakeupTime = mInputTargetWaitTimeoutTime;
          }
          return INPUT_EVENT_INJECTION_PENDING;
      }
}
```
//1 当第一次发生无法将事件发送给窗口的情况时，在这里设置了引发ANR的时间点。在随后的重试过程中，将当前时间点与引发ANR的时间点
  进行比对，并决定是否引发ANR或再次重试


将checkWindowReadyForMoreInputLocked和handleTargetsNotReadyLocked放入派发流程：
 InputDispatcher从派发队列中获取了一个事件mPendingEvent,并为他查找目标窗口，然后通过checkWindowReadyForMoreInputLocked
 确定此窗口是否可以接收事件。如果可以将事件放入窗口的Connection对象的发送队列中并启动发送循环，否则调用handleTargetsNotReadyLocked
 计算引发ANR的时间点，然后通过返回INPUT_EVENT_INJECTION_PENDING停止对mPendingEvent的派发工作，并通过设置
 nextWakeupTime使派发循环进入休眠状态。
 休眠的过程中有可能因为窗口反馈到来,新输入事件到来或新的窗口信息到来而唤醒，派发线程便重新开始对mPendingEvent的派发过程，
   进而重新寻找目标窗口，再通过checkWindowReadyForMoreInputLocked检查目标窗口是否准备好接收事件，如果可以接收事件，
   则将其提交给Connection进行发送，并重置之前所设置的ANR信息。否则再次进入handleTargetsNotReadyLocked，这时将当前时间
   与ANR时间进行对比，以决定引发ANR还是再次使派发线程进入休眠


