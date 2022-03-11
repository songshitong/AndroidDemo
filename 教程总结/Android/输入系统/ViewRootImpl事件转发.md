https://blog.csdn.net/Double2hao/article/details/117062015

android8.0
ViewRootImpl通过InputChannel将事件分发到view体系的

frameworks/base/core/java/android/view/ViewRootImpl.java
```
public void setView(View view, WindowManager.LayoutParams attrs, View panelParentView) {
   ...
     //创建InputChannel
     if ((mWindowAttributes.inputFeatures
                        & WindowManager.LayoutParams.INPUT_FEATURE_NO_INPUT_CHANNEL) == 0) {
                    mInputChannel = new InputChannel();
                }
     try {
        mOrigWindowType = mWindowAttributes.type;
        mAttachInfo.mRecomputeGlobalAttributes = true;
        collectViewAttributes();
        res = mWindowSession.addToDisplay(mWindow, mSeq, mWindowAttributes,
                getHostVisibility(), mDisplay.getDisplayId(),
                mAttachInfo.mContentInsets, mAttachInfo.mStableInsets,
                mAttachInfo.mOutsets, mInputChannel);
         }           
    ...            
      //创建WindowInputEventReceiver接收事件
      if (mInputChannel != null) {
                    if (mInputQueueCallback != null) {
                        mInputQueue = new InputQueue();
                        mInputQueueCallback.onInputQueueCreated(mInputQueue);
                    }
                    mInputEventReceiver = new WindowInputEventReceiver(mInputChannel,
                            Looper.myLooper());
                }
   ...
    //对不同事件的分发  各种stage形成责任链
    mSyntheticInputStage = new SyntheticInputStage();
    InputStage viewPostImeStage = new ViewPostImeInputStage(mSyntheticInputStage);
    InputStage nativePostImeStage = new NativePostImeInputStage(viewPostImeStage,
            "aq:native-post-ime:" + counterSuffix);
    InputStage earlyPostImeStage = new EarlyPostImeInputStage(nativePostImeStage);
    InputStage imeStage = new ImeInputStage(earlyPostImeStage,
            "aq:ime:" + counterSuffix);
    InputStage viewPreImeStage = new ViewPreImeInputStage(imeStage);
    InputStage nativePreImeStage = new NativePreImeInputStage(viewPreImeStage,
            "aq:native-pre-ime:" + counterSuffix);

    mFirstInputStage = nativePreImeStage;
    mFirstPostImeInputStage = earlyPostImeStage;             
}
```
看一下WindowInputEventReceiver
```
final class WindowInputEventReceiver extends InputEventReceiver {
        public WindowInputEventReceiver(InputChannel inputChannel, Looper looper) {
            super(inputChannel, looper);
        }

        @Override
        public void onInputEvent(InputEvent event) {
            //processImmediately为true
            enqueueInputEvent(event, this, 0, true);
        }
       ...
    }
    
void enqueueInputEvent(InputEvent event,
            InputEventReceiver receiver, int flags, boolean processImmediately) {
        adjustInputEventForCompatibility(event);
        QueuedInputEvent q = obtainQueuedInputEvent(event, receiver, flags);
        QueuedInputEvent last = mPendingInputEventTail;
        if (last == null) {
            mPendingInputEventHead = q;
            mPendingInputEventTail = q;
        } else {
            last.mNext = q;
            mPendingInputEventTail = q;
        }
        mPendingInputEventCount += 1;
        Trace.traceCounter(Trace.TRACE_TAG_INPUT, mPendingInputEventQueueLengthCounterName,
                mPendingInputEventCount);

        if (processImmediately) {
            doProcessInputEvents();
        } else {
            scheduleProcessInputEvents();
        }
    }    
    
 void doProcessInputEvents() {
        // Deliver all pending input events in the queue.
        while (mPendingInputEventHead != null) {
            QueuedInputEvent q = mPendingInputEventHead;
            mPendingInputEventHead = q.mNext;
            if (mPendingInputEventHead == null) {
                mPendingInputEventTail = null;
            }
            q.mNext = null;

            mPendingInputEventCount -= 1;
            Trace.traceCounter(Trace.TRACE_TAG_INPUT, mPendingInputEventQueueLengthCounterName,
                    mPendingInputEventCount);

            long eventTime = q.mEvent.getEventTimeNano();
            long oldestEventTime = eventTime;
            if (q.mEvent instanceof MotionEvent) {
                MotionEvent me = (MotionEvent)q.mEvent;
                if (me.getHistorySize() > 0) {
                    oldestEventTime = me.getHistoricalEventTimeNano(0);
                }
            }
            mChoreographer.mFrameInfo.updateInputEventTime(eventTime, oldestEventTime);
            //开始分发事件了
            deliverInputEvent(q);
        }

        // We are done processing all input events that we can process right now
        // so we can clear the pending flag immediately.
        if (mProcessInputEventsScheduled) {
            mProcessInputEventsScheduled = false;
            mHandler.removeMessages(MSG_PROCESS_INPUT_EVENTS);
        }
    }   
    
 
 private void deliverInputEvent(QueuedInputEvent q) {
        Trace.asyncTraceBegin(Trace.TRACE_TAG_VIEW, "deliverInputEvent",
                q.mEvent.getSequenceNumber());
        if (mInputEventConsistencyVerifier != null) {
            mInputEventConsistencyVerifier.onInputEvent(q.mEvent, 0);
        }

        InputStage stage;
        if (q.shouldSendToSynthesizer()) {
            //mSyntheticInputStage ViewPostImeInputStage
            stage = mSyntheticInputStage;
        } else {
             //mFirstPostImeInputStage是EarlyPostImeInputStage
             //mFirstInputStage是 NativePreImeInputStage
            stage = q.shouldSkipIme() ? mFirstPostImeInputStage : mFirstInputStage;
        }

        if (stage != null) {
            stage.deliver(q);
        } else {
            finishInputEvent(q);
        }
    }    
```

看一下
```
abstract class InputStage {
  private final InputStage mNext;
  public InputStage(InputStage next) {
            mNext = next;
        }
  public final void deliver(QueuedInputEvent q) {
            if ((q.mFlags & QueuedInputEvent.FLAG_FINISHED) != 0) {
                //转发给下一个
                forward(q);
            } else if (shouldDropInputEvent(q)) {
                finish(q, false);
            } else {
                apply(q, onProcess(q));
            }
   }
   
    //当前stage的处理
    protected int onProcess(QueuedInputEvent q) {
            return FORWARD;
      }
   
    //forward分发给下一个
    protected void forward(QueuedInputEvent q) {
            onDeliverToNext(q);
        }
   
   protected void onDeliverToNext(QueuedInputEvent q) {
            if (DEBUG_INPUT_STAGES) {
                Log.v(mTag, "Done with " + getClass().getSimpleName() + ". " + q);
            }
            if (mNext != null) {
                mNext.deliver(q);
            } else {
                finishInputEvent(q);
            }
        } 
  //apply根据结果判断是否继续转发给下一个还是结束      
  protected void apply(QueuedInputEvent q, int result) {
            if (result == FORWARD) {
                forward(q);
            } else if (result == FINISH_HANDLED) {
                finish(q, true);
            } else if (result == FINISH_NOT_HANDLED) {
                finish(q, false);
            } else {
                throw new IllegalArgumentException("Invalid result: " + result);
            }
        }      
}
```
对于事件的分发主要在ViewPostImeInputStage
```
final class ViewPostImeInputStage extends InputStage {
        public ViewPostImeInputStage(InputStage next) {
            super(next);
        }
  @Override
  protected int onProcess(QueuedInputEvent q) {
    //KeyEvent
    if (q.mEvent instanceof KeyEvent) {
        return processKeyEvent(q);
    } else {
       //MotionEvent
        final int source = q.mEvent.getSource();
        if ((source & InputDevice.SOURCE_CLASS_POINTER) != 0) {
            return processPointerEvent(q);
        } else if ((source & InputDevice.SOURCE_CLASS_TRACKBALL) != 0) {
            return processTrackballEvent(q);
        } else {
            return processGenericMotionEvent(q);
        }
    }
  }      
  
  private int processPointerEvent(QueuedInputEvent q) {
        final MotionEvent event = (MotionEvent)q.mEvent;

        mAttachInfo.mUnbufferedDispatchRequested = false;
        mAttachInfo.mHandlingPointerEvent = true;
        //交给mView的dispatchPointerEvent处理了
        boolean handled = mView.dispatchPointerEvent(event);
        maybeUpdatePointerIcon(event);
        maybeUpdateTooltip(event);
        mAttachInfo.mHandlingPointerEvent = false;
        if (mAttachInfo.mUnbufferedDispatchRequested && !mUnbufferedInputDispatch) {
            mUnbufferedInputDispatch = true;
            if (mConsumeBatchedInputScheduled) {
                scheduleConsumeBatchedInputImmediately();
            }
        }
        return handled ? FINISH_HANDLED : FORWARD;
  } 
}
```
//q.mEvent为InputEvent，有两种类型  KeyEvent和MotionEvent
  这里主要看了MotionEvent触摸事件


frameworks/base/core/java/android/view/View.java
```
  //返回值true代表事件已经被处理了
  public final boolean dispatchPointerEvent(MotionEvent event) {
        if (event.isTouchEvent()) {
            return dispatchTouchEvent(event);
        } else {
            return dispatchGenericMotionEvent(event);
        }
    }
    
  public boolean dispatchTouchEvent(MotionEvent event) {
      ....
        if (onFilterTouchEventForSecurity(event)) {
        ...
        //走到了熟悉的onTouchEvent()
            if (!result && onTouchEvent(event)) {
                result = true;
            }
        }

       ...
        return result;
    }  
```

ViewRootImpl的mView是DecorView
看一下DecorView的处理
frameworks/base/core/java/com/android/internal/policy/DecorView.java
```
   @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        final Window.Callback cb = mWindow.getCallback();
        return cb != null && !mWindow.isDestroyed() && mFeatureId < 0
                ? cb.dispatchTouchEvent(ev) : super.dispatchTouchEvent(ev);
    }
```
Callback进行各种回调，KeyEvent，MotionEvent，onCreatePanelView，onWindowAttributesChanged，onWindowFocusChanged...
```
   public void setCallback(Callback callback) {
        mCallback = callback;
    }
    public final Callback getCallback() {
        return mCallback;
    }
```
满足特殊条件时将事件进行了转发,Window.Callback的初始化是在Activity.attach
frameworks/base/core/java/android/app/Activity.java
```
 final void attach(Context context, ActivityThread aThread,
            Instrumentation instr, IBinder token, int ident,
            Application application, Intent intent, ActivityInfo info,
            CharSequence title, Activity parent, String id,
            NonConfigurationInstances lastNonConfigurationInstances,
            Configuration config, String referrer, IVoiceInteractor voiceInteractor,
            Window window, ActivityConfigCallback activityConfigCallback) {
        attachBaseContext(context);

        mFragments.attachHost(null /*parent*/);

        mWindow = new PhoneWindow(this, window, activityConfigCallback);
        mWindow.setWindowControllerCallback(this);
        mWindow.setCallback(this);
        ...
 }
 
 public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            onUserInteraction();
        }
        if (getWindow().superDispatchTouchEvent(ev)) {
            return true;
        }
        return onTouchEvent(ev);
    }
    
  public boolean onTouchEvent(MotionEvent event) {
        if (mWindow.shouldCloseOnTouch(this, event)) {
            finish();
            return true;
        }

        return false;
    }  
  //当key, touch, or trackball事件分发到activity时就会调用该方法，用来得知存在用户交互  
  public void onUserInteraction() {
    }   
```
所以大部分情况下DecorView会将事件转发给Activity
1 调用到window的superDispatchTouchEvent()，如果已经处理的事件，那么久直接return。
2 如果window没有处理事件，那么会走到activity的onTouchEvent()逻辑。
3 onTouchEvent()逻辑中就是判断window的shouldCloseOnTouch()是否返回true，如果是true，就直接关闭activity，
  并且return true表示该事件已经处理过。反之就return false，表示该事件没有处理过

对于shouldCloseOnTouch
frameworks/base/core/java/android/view/Window.java
```
   private boolean mCloseOnTouchOutside = false;
   public boolean shouldCloseOnTouch(Context context, MotionEvent event) {
        if (mCloseOnTouchOutside && event.getAction() == MotionEvent.ACTION_DOWN
                && isOutOfBounds(context, event) && peekDecorView() != null) {
            return true;
        }
        return false;
    }
    
    public void setCloseOnTouchOutside(boolean close) {
        mCloseOnTouchOutside = close;
        mSetCloseOnTouchOutside = true;
    }    
```
mCloseOnTouchOutside默认为false也就是点击window外不关闭，可以通过activity的setFinishOnTouchOutside(boolean finish)进行设置


实际的window是PhoneWindow，处理superDispatchTouchEvent  
frameworks/base/core/java/com/android/internal/policy/PhoneWindow.java
```
 @Override
    public boolean superDispatchTouchEvent(MotionEvent event) {
        return mDecor.superDispatchTouchEvent(event);
    }
```
PhoneWindow又调用的DecorView的superDispatchTouchEvent()方法
frameworks/base/core/java/com/android/internal/policy/DecorView.java
```
 public boolean superDispatchTouchEvent(MotionEvent event) {
        return super.dispatchTouchEvent(event);
    }
```
DecorView 又调用ViewGroup的dispatchTouchEvent，也就是一开始没有进行转发的另一个分支


为什么DecorView的事件传递要如此麻烦，最终不还是调用到View.dispatchTouchEvent()?
回答：
根据调用栈可以看到，事件传给了activity与window，这表示activity与window都可以对事件进行拦截，或者做特殊处理。
比如这几个方法：
activity.onUserInteraction()
window.superDispatchTouchEvent()
window.shouldCloseOnTouch()


onUserInteraction的使用场景
onUserInteraction 方法在 Activity 接收到 ACTION_DOWN 事件的时候才会被调用，这可以用于某些需要知道 Activity 是否处于长期“闲置”状态的需求。
  例如，如果我们需要在 Activity 没有被操作一段时间后自动隐藏标题栏的话，就可以用该方法来设置一个定时任务控制标题栏的隐藏状态
  //来一个定时器，每次调用onUserInteraction，定时器重置，这样就可以在计时完成时确定空闲一段时间了

  相反，可以用来收集用户正在操作的需求



https://juejin.cn/post/6931914294980411406#heading-22
事件为什么是由外向内？
在上面提供的例子里，当点击 MyTextView 区域时，最外层的 MyRelativeLayout 还是会最先接收到触摸事件。那么，
为什么 Android 系统要将事件分发机制设计成由外向内的形式呢？能不能是由内向外的形式？或者是直接只交于点击区域所在的 View 进行处理呢？
将触摸事件只交于点击区域所在的 View 进行处理是肯定不行的。想像个场景，一个 ViewPager 包含了多个 Fragment，
每个 Fragment 均包含一个 RecyclerView，如果将触摸事件只交于 RecyclerView 处理的话，那么 RecyclerView 可以正常响应上下滑动的事件，
但是 ViewPager 就无法左右滑动了，因为左右滑动的事件都被 RecyclerView 给消费掉了，即使该事件对于 RecyclerView 本身来说是不需要的。
所以事件分发机制必须要包含一个在父容器和子内容区域之间流转触摸事件的流程，各个 View 根据各自所需来进行选择性消费

**那能不能是由内向外的形式呢？也不合适。**一个ViewGroup 可能包含一个到多个 View，ViewGroup 需要通过判断触摸点的坐标系位于哪个 View 区域内来确定触摸事件的下一个接收者。
而我们知道，触摸事件按照从外向内的传递顺序是： DecorView -> Activity -> PhoneWindow -> DecorView -> ContentView，
由于触摸事件的早期接收者已经是处于外层的 DecorView 了，所以按照从外向内进行传递会更加合适（这也只是我自己的个人见解，有误的话欢迎指出）

//todo ios或其他的事件实现机制 html
  