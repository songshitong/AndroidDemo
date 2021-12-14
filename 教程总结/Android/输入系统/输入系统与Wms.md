深入理解Android 卷三
android8.1
InputChannel
  InputChannel本质是一对SocketPair(非网络套接字)，用来实现本机内进行进程间通信。SocketPair的两个文件描述符是相同的，
   因此非常适合用来进行进程间的交互式通信
  创建InputChannel
    /frameworks/native/libs/input/InputTransport.cpp
```
status_t InputChannel::openInputChannelPair(const String8& name,
          sp<InputChannel>& outServerChannel, sp<InputChannel>& outClientChannel) {
       int sockets[2];
      //通过socketpair创建一对socketpair，并保存在Sockets数组中
      if (socketpair(AF_UNIX, SOCK_SEQPACKET, 0, sockets)) {
      //错误处理
      }
      ...//配置两个套接字的读写缓冲区尺寸
      //创建server端的InputChannel
      outServerChannel = new InputChannel(serverChannelName, sockets[0]);
      //创建client端的InputChannel
      outClientChannel = new InputChannel(clientChannelName, sockets[1]);
          }
```
收发消息
/frameworks/native/libs/input/InputTransport.cpp
```
status_t InputChannel::sendMessage(const InputMessage* msg) {
}

status_t InputChannel::receiveMessage(InputMessage* msg) {
}
```


InputChannel完成InputDispatcher和窗口之间的连接
InputChannel一个注册给IMS是服务端，另一个通过outInputChannel交给调用者，是客户端
InputDispatcher和窗口连接的建立应该从客户端和服务端两个方面讨论
/frameworks/base/services/core/java/com/android/server/wm/WindowManagerService.java
```
public class WindowManagerService extends IWindowManager.Stub{
  final InputMonitor mInputMonitor = new InputMonitor(this);

  public int addWindow(Session session, IWindow client, int seq,
                WindowManager.LayoutParams attrs, int viewVisibility, int displayId,
                Rect outContentInsets, Rect outStableInsets, Rect outOutsets,
                InputChannel outInputChannel) {
  //打开inputChannel
    final boolean openInputChannels = (outInputChannel != null
                        && (attrs.inputFeatures & INPUT_FEATURE_NO_INPUT_CHANNEL) == 0);
                if  (openInputChannels) {
                    win.openInputChannel(outInputChannel);
                }
  ...
  //将所有窗口信息更新到InputManagerService
  mInputMonitor.updateInputWindowsLw(false /*force*/);
  ...

  }
}


```
openInputChannel的过程
/frameworks/base/services/core/java/com/android/server/wm/WindowState.java
```
    void openInputChannel(InputChannel outInputChannel) {
          String name = getName();
          InputChannel[] inputChannels = InputChannel.openInputChannelPair(name);
          mInputChannel = inputChannels[0];
          mClientChannel = inputChannels[1];
          mInputWindowHandle.inputChannel = inputChannels[0];
          mClientChannel.transferTo(outInputChannel);
          mClientChannel.dispose();
          mClientChannel = null;
          mService.mInputManager.registerInputChannel(mInputChannel, mInputWindowHandle);
      }
```
可以看出WMS添加窗口时，会创建一对InputChannel，其中一个保存在windowState中，并注册给IMS，他是服务端。另一个则通过
传出参数outInputChannel交给调用者，是客户端。

/frameworks/base/services/core/java/com/android/server/wm/InputMonitor.java
```
 final class InputMonitor{
   private InputWindowHandle[] mInputWindowHandles;
   private final WindowManagerService mService;
   public InputMonitor(WindowManagerService service) {
             mService = service;
         }
   void updateInputWindowsLw(boolean force) {
     if (inDrag) {
                   final InputWindowHandle dragWindowHandle = mService.mDragState.getInputWindowHandle();
                   if (dragWindowHandle != null) {
                       addInputWindowHandle(dragWindowHandle);
                   } else {
                       Slog.w(TAG_WM, "Drag is in progress but there is no "
                               + "drag window handle.");
                   }
               }
   }
   //添加inputWindowHandle到mInputWindowHandles
   void addInputWindowHandle(final InputWindowHandle inputWindowHandle,
                 final WindowState child, int flags, final int type, final boolean isVisible,
                 final boolean hasFocus, final boolean hasWallpaper) {
 }
 private final class UpdateInputForAllWindowsConsumer implements Consumer<WindowState> {
 private void updateInputWindows(boolean inDrag) {
              navInputConsumer = getInputConsumer(INPUT_CONSUMER_NAVIGATION, DEFAULT_DISPLAY);
              pipInputConsumer = getInputConsumer(INPUT_CONSUMER_PIP, DEFAULT_DISPLAY);
              wallpaperInputConsumer = getInputConsumer(INPUT_CONSUMER_WALLPAPER, DEFAULT_DISPLAY);
              mAddInputConsumerHandle = navInputConsumer != null;
              mAddPipInputConsumerHandle = pipInputConsumer != null;
              mAddWallpaperInputConsumerHandle = wallpaperInputConsumer != null;
              mTmpRect.setEmpty();
              pipTouchableBounds = mAddPipInputConsumerHandle ? mTmpRect : null;
              mDisableWallpaperTouchEvents = false;
              this.inDrag = inDrag;
              wallpaperController = mService.mRoot.mWallpaperController;

              mService.mRoot.forAllWindows(this, true /* traverseTopToBottom */);
              if (mAddWallpaperInputConsumerHandle) {
                  // No visible wallpaper found, add the wallpaper input consumer at the end.
                  addInputWindowHandle(wallpaperInputConsumer.mWindowHandle);
              }
               //更新mInputWindowHandles到InputManagerService
              // Send windows to native code.
              mService.mInputManager.setInputWindows(mInputWindowHandles, mFocusedInputWindowHandle);
              clearInputWindowHandlesLw();
          }
 }
```


/frameworks/base/services/core/java/com/android/server/input/InputManagerService.java
```
 public void registerInputChannel(InputChannel inputChannel,
          nativeRegisterInputChannel(mPtr, inputChannel, inputWindowHandle, false);
      }
 public void setInputWindows(InputWindowHandle[] windowHandles,
              InputWindowHandle focusedWindowHandle) {
          final IWindow newFocusedWindow =
              focusedWindowHandle != null ? focusedWindowHandle.clientWindow : null;
          if (mFocusedWindow != newFocusedWindow) {
              mFocusedWindow = newFocusedWindow;
              if (mFocusedWindowHasCapture) {
                  setPointerCapture(false);
              }
          }
          nativeSetInputWindows(mPtr, windowHandles);
      }
```



/frameworks/base/services/core/jni/com_android_server_input_InputManagerService.cpp
```
status_t NativeInputManager::registerInputChannel(JNIEnv* /* env */,
          const sp<InputChannel>& inputChannel,
          const sp<InputWindowHandle>& inputWindowHandle, bool monitor) {
      ATRACE_CALL();
      return mInputManager->getDispatcher()->registerInputChannel(
              inputChannel, inputWindowHandle, monitor);
  }

void NativeInputManager::setInputWindows(JNIEnv* env, jobjectArray windowHandleObjArray) {
   ...
   mInputManager->getDispatcher()->setInputWindows(windowHandles);
   ...
}
```

/frameworks/native/services/inputflinger/InputDispatcher.cpp
```
status_t InputDispatcher::registerInputChannel(const sp<InputChannel>& inputChannel,
     const sp<InputWindowHandle>& inputWindowHandle, bool monitor) {
    //1 为传入的InputChannel创建一个Connection对象并对其进行封装
    sp<Connection> connection = new Connection(inputChannel, inputWindowHandle, monitor);
    int fd = inputChannel->getFd();
    //以InputChannel的描述符为键，保存在mConnectionsByFd字典中
    mConnectionsByFd.add(fd, connection);
    if (monitor) {
     //在目标窗口的查找时，凡是位于此列表的inputChannel都会被放在目标列表中
        mMonitoringChannels.push(inputChannel);
    }
     //2 监听InputChannel的可读性。mLooper的pollOnce本质上就是epoll_wait,因此looper对象具有监听文件描述符可读性
     事件的能力，在此注册InputChannel可读性事件，并在事件到来时通过handleReceiveCallback毁掉进行处理
       mLooper->addFd(fd, 0, ALOOPER_EVENT_INPUT, handleReceiveCallback, this);
    }
    mLooper->wake();
    return OK;
}
void InputDispatcher::setInputWindows(const Vector<sp<InputWindowHandle> >& inputWindowHandles) {
          ...
          //1 替换现有列表，并将现有列表保存在oldWindowHandles中
          Vector<sp<InputWindowHandle> > oldWindowHandles = mWindowHandles;
          mWindowHandles = inputWindowHandles;
          sp<InputWindowHandle> newFocusedWindowHandle;
          //遍历新的InputWindowHandle，确定处于焦点状态的InputWindowHandle
          for (size_t i = 0; i < mWindowHandles.size(); i++) {
                      const sp<InputWindowHandle>& windowHandle = mWindowHandles.itemAt(i);
                   、//2 将Java层的布局信息搬运到其InputWineowInfo结构体中
                      if (!windowHandle->updateInfo() || windowHandle->getInputChannel() == NULL) {

                      }
                      //记录处于焦点状态的窗口
                      if (windowHandle->getInfo()->hasFocus) {
                          newFocusedWindowHandle = windowHandle;
                      }

                  }
          //3 将焦点窗口保存到mFocusedWindowHandle中
          if (mFocusedWindowHandle != newFocusedWindowHandle) {
              mFocusedWindowHandle = newFocusedWindowHandle;
                }
        //遍历旧的InputWindowHandle列表，检查其中的窗口是否在新列表中已经不存在。如果不存在，则意味着窗口已经被WMS删除，
        //此时需要释放被删除的窗口信息
        for (size_t i = 0; i < oldWindowHandles.size(); i++) {
                      const sp<InputWindowHandle>& oldWindowHandle = oldWindowHandles.itemAt(i);
                      if (!hasWindowHandleLocked(oldWindowHandle)) {
                          oldWindowHandle->releaseInfo();
                      }
                  }
        //因为窗口列表的更新，需要唤醒派发线程重新尝试派发
        mLooper->wake();
}
```


connection 相关
/frameworks/native/services/inputflinger/InputDispatcher.h
```
 class Connection : public RefBase {
   InputPublisher inputPublisher;
   Queue<DispatchEntry> outboundQueue
   Queue<DispatchEntry> waitQueue
 }
```
Connection类描述了从InputDispatcher到目标窗口的一个连接，保存了向窗口发送事件的状态信息。
 inputPublisher 封装了InputChannel并直接对其进行写入和读取。另外，他也负责InputMessage结构体的封装和解析
 outboundQueue  用于保持等待通过此Connection进行发送的事件队列
 waitQueue  用于保存已经通过此Connection将事件发送给窗口，正在等待窗口反馈的事件队列


registerInputChannel将InputChannel的可读性事件注册到mLooper中。当来自窗口的反馈到来时，派发线程mLooper->pollOnce
将被唤醒，并回调handleReceiveCallback进行处理。因此，窗口反馈的到来也会导致派发线程进入下一次派发循环，
不过是在handleReceiveCallback完成后

InputChannel注册小结
完成服务端的InputChannel的注册之后，InputDispatcher便拥有了向客户端的InputChannel发送InputMessage,以及通过回调
handleReceiveCallback相应来自客户端的反馈的能力。
这个过程是，现将输入事件放入Connection的outboundQueue队列里，然后再由inputPublisher依次将队列中的事件封装为InputMessasge
并写入InputChannel,直到队列为空，或InputChannel的写入缓冲区满。写入事件将被转移到waitQueue队列里。随后派发线程陷入休眠状态。
当窗口在另一端读取事件并发来反馈后，派发线程因InputChannel可读而被唤醒，并在handleReceiveCallback中通过Connection的
inputPublisher读取反馈信息，将其与waitQueue中等待反馈的事件进行配对成功后，将事件从waitQuue中移除，完成事件派发
输入系统_Connection工作原理.png 实线描述了事件发送过程，虚线描述了反馈接受的过程


布局信息的更新
setInputWindows函数的结果
1更新mInputWindowHandles列表，让其可以正确地反映当前时刻WMS中所有窗口的布局状态。这个列表为按坐标点的方式查找目标窗口提供了依据
2更新mFocusedWindowHandle成员。他为按照焦点查找目标窗口提供了依据
setInputWindows为事件派发的目标提供了新的选择。如果派发线程此时正在为无法将mPendingEvent派发给一个窗口而处于休眠状态，
 此时可以将其唤醒，以重新尝试派发动作，这就是最后调用mLooper->wake的原因

服务端的连接总结
 输入系统服务端的连接通过registerInputChannel和setInputWindow两个步骤完成
  1 registerInputChannel为inputChannel 创建了一个connection，并监听了InputChannel的可读事件，使得InputDispatcher
  拥有了将事件发送给InputChannel并接受反馈的能力
  2 setInputWindows将窗口的布局信息更新至InputDispatcher，使窗口处于目标窗口的候选列表


窗口端连接的建立
 获取InputChannel后，使用它创建一个InputEventReceiver对象，InputEventReceiver对象可以接受来自InputChannel的
 输入事件，并触发onInputEvent回调
 使用
 /frameworks/base/core/java/android/view/ViewRootImpl.java
 ```
 public void setView(View view, WindowManager.LayoutParams attrs, View panelParentView) {
  ((mWindowAttributes.inputFeatures
                           & WindowManager.LayoutParams.INPUT_FEATURE_NO_INPUT_CHANNEL) == 0) {
                       mInputChannel = new InputChannel();
                   }
    mInputEventReceiver = new WindowInputEventReceiver(mInputChannel,Looper.myLooper());
 }

 final class WindowInputEventReceiver extends InputEventReceiver {
           public WindowInputEventReceiver(InputChannel inputChannel, Looper looper) {
               super(inputChannel, looper);
           }

           @Override
           public void onInputEvent(InputEvent event, int displayId) {
               enqueueInputEvent(event, this, 0, true);
           }
    }
 ```
InputEventReceiver的工作原理
/frameworks/base/core/java/android/view/InputEventReceiver.java
```
public InputEventReceiver(InputChannel inputChannel, Looper looper) {
          mInputChannel = inputChannel;
          mMessageQueue = looper.getQueue();
          mReceiverPtr = nativeInit(new WeakReference<InputEventReceiver>(this),
                  inputChannel, mMessageQueue);
      }
```

/frameworks/base/core/jni/android_view_InputEventReceiver.cpp
```
tatic jlong nativeInit(JNIEnv* env, jclass clazz, jobject receiverWeak,
          jobject inputChannelObj, jobject messageQueueObj) {
  sp<NativeInputEventReceiver> receiver = new NativeInputEventReceiver(env,
              receiverWeak, inputChannel, messageQueue);
      status_t status = receiver->initialize();
         }
```
/frameworks/base/core/jni/android_view_InputEventReceiver.cpp
```
class NativeInputEventReceiver : public LooperCallback {
  private:
   virtual int handleEvent(int receiveFd, int events, void* data);
}
NativeInputEventReceiver::NativeInputEventReceiver(JNIEnv* env,
          jobject receiverWeak, const sp<InputChannel>& inputChannel,
          const sp<MessageQueue>& messageQueue) :
          mReceiverWeakGlobal(env->NewGlobalRef(receiverWeak)),
          mInputConsumer(inputChannel), mMessageQueue(messageQueue),
          mBatchedInputEventPending(false), mFdEvents(0) {

  }
  status_t NativeInputEventReceiver::initialize() {
        ///frameworks/native/include/android/looper.h   ALOOPER_EVENT_INPUT = 1 << 0
        setFdEvents(ALOOPER_EVENT_INPUT);
        return OK;
    }
 void NativeInputEventReceiver::setFdEvents(int events) {
      if (mFdEvents != events) {
          mFdEvents = events;
          int fd = mInputConsumer.getChannel()->getFd();
          if (events) {
          //通过looper注册inputChannel的可读性事件
              mMessageQueue->getLooper()->addFd(fd, 0, events, this, NULL);
          } else {
              mMessageQueue->getLooper()->removeFd(fd);
          }
      }
  }
```
//todo looper callback
InputConsumer与InputPublisher一样，封装了InputChannel，负责对其进行写入和读取操作，也负责InputMessage的封装与解析，
不过他们的功能相反，InputConsumer接受的是输入事件，发送的是反馈

setFdEvents
将InputChannel的可读事件注册到Looper中，然后在事件到来时从InputChannel中读取InputMessage,并翻译成InputEvent,
然后回调InputEventReceiver的onInputEvent
通过Looper监听InputChannel的可读性事件，当InputMessage可读时，NativeInputEventReceiver的handleEvent会被Looper
调用，此时通过InputConsumer从InputChannel中读取事件，然后回调给Java层的onInputEvent函数.在Java层完成事件处理后，
便可以通过InputConsumer发送处理完毕的反馈给InputDispatcher

在窗口端，输入事件的回调的发起者是创建InputEventReceiver时所使用的looper.因此这个looper就是输入系统中的第三台水泵

窗口端的连接.png

InputDispatcher与窗口连接的总结
 通过输入系统_Connection工作原理.png和窗口端的连接.png，便构成了InputDispatcher到窗口的发送循环

//todo 事件的发送