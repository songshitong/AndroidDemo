深入理解android 卷三     Android8.1
ViewRootImpl实现了ViewParent接口，作为整个控件树的根部，控件的测量，布局，绘制及输入事件的派发处理都由ViewRootImpl触发
另一方面，他是WindowManagerGlobal工作的实际实现者，因此他还需要负责与WMS交互通信以调整窗口的位置大小，以及对来自WMS的
事件(如窗口尺寸改变等)做出相应处理

创建
addView为DecorView
/frameworks/base/core/java/android/view/WindowManagerGlobal.java
```
public void addView(View view, ViewGroup.LayoutParams params,
              Display display, Window parentWindow) {
ViewRootImpl root;
...
 root = new ViewRootImpl(view.getContext(), display);
  view.setLayoutParams(wparams);
  mViews.add(view);
  mRoots.add(root);
  mParams.add(wparams);
}
```

/frameworks/base/core/java/android/view/ViewRootImpl.java
```
public final class ViewRootImpl implements ViewParent,
          View.AttachInfo.Callbacks, ThreadedRenderer.DrawCallbacks

public ViewRootImpl(Context context, Display display) {
//1 获取一个IWindowSession实例，他是ViewRootImpl和WMS进行通信的代理
mWindowSession = WindowManagerGlobal.getWindowSession();
//2后面setView调用中将会把窗口添加到这个Display中
mDisplay = display;
//3 提现了创建ViewRootImpl的线程如何成为UI主线程。
//在ViewRootImpl处理来自控件树的请求时(如请求重新布局，请求重绘，改变焦点等)，会检查发起请求的thread与这个mThread是否
//相同。倘若不同则会拒绝这个请求并抛出一个异常
mThread = Thread.currentThread();
//4 用于收集窗口中的无效区域。 由于数据或状态发生改变时而需要进行重絵的区域
//例如：当应用程序修改了一个TextView的文字时，TextView会将自己的区域标记为无效区域，并通过View.invalidate方法将这块
//区域添加到这里的mDirty中。当下次绘制时，TextView便可以将新的文字绘制在这块区域上
mDirty = new Rect();
mTempRect = new Rect();
mVisRect = new Rect();
//5 描述了当前窗口的位置和尺寸。 与WMS众的windowState.mFrame保持一致
mWinFrame = new Rect();
//6创建一个W类型的实例，w是IWindow.stub的子类。他将在WMS中作为新窗口的ID，并接受来自WMS的回调
mWindow = new W(this);
//7 mAttachInfo是控件系统中很重要的对象。存储了当前控件树所贴附的窗口的各种信息，并且会派发给控件树的每一个控件。
//当需要在一个view中查询与当前窗口有关的信息时，非常值得在mAttachInfo中搜索 windowSession,窗口的实例，ViewRootImpl的实例
//窗口所属的display，窗口的surface以及窗口在屏幕上的位置等
//由ViewRootImpl生成，全局View共享
 mAttachInfo = new View.AttachInfo(mWindowSession, mWindow, display, this, mHandler, this,context);
 //8  一个处理未经任何人消费的输入事件的场所
 //同PhoneWindowManager一样，定义在android.policy包中，实现为PhoneFallbackEventHandler
 mFallbackEventHandler = new PhoneFallbackEventHandler(context);
 //9 创建一个依附于当前线程，即主线程的Choreographer,用于通过VSYNC特性安排重绘
 mChoreographer = Choreographer.getInstance();
}
//10 一个依附于创建ViewRootImpl的线程，即主线程上的，用于将某些必须在主线程进行的操作调度主线程中执行。
 final ViewRootHandler mHandler = new ViewRootHandler();
//11  Surface此时是一个没有任何内容的空壳子，在WMS通过relayoutWindow为其分配一块Surface之前不能使用
 final Surface mSurface = new Surface();

```
mHandler与mChoreographer
//mHandler与mChoreographer的同时存在看似有些重复，但是mChoreographer处理消息时具有VSYNC特性，因此主要用来处理与重绘
//相关的操作，由于需要等待VSYNC的垂直同步事件来触发对下一条消息的处理，因此处理消息的及时性稍逊于mHandler
//mHandler的作用是为了将发生在其他线程的事件安排在主线程执行。发生在其他线程中的事件是指来自与WMS，由继承自IWindow.sub
//的mWindow引发的回调。由于mWindow是一个BInder对象的Bn端，因此，这些回调发生在Binder的线程池中，而这些回调会影响到控件
//系统的重新测量，布局与绘制，因此需要此Handler将回调安排到主线程中
mHandler与mThread
都是为了单线程模型而存在的。Android的UI操作不是线程安全的，而且很多操作也是建立在单线程的假设之上(如scheduleTraversals)
采用单线程模型是降低系统的复杂度，并且降低锁的开销。

scheduleTraversals相关
```
    //ViewRootImpl.java
     void scheduleTraversals() {
         if (!mTraversalScheduled) {
             //此字段保证同时间多次更改只会刷新一次，例如TextView连续两次setText(),也只会走一次绘制流程
             mTraversalScheduled = true;
             //添加同步屏障，屏蔽同步消息，保证VSync到来立即执行绘制
             mTraversalBarrier = mHandler.getLooper().getQueue().postSyncBarrier();
             //mTraversalRunnable是TraversalRunnable实例，最终走到run()，也即doTraversal();
             mChoreographer.postCallback(
                    Choreographer.CALLBACK_TRAVERSAL, mTraversalRunnable, null);
            if (!mUnbufferedInputDispatch) {
                scheduleConsumeBatchedInput();
            }
            notifyRendererOfFramePending();
            pokeDrawLockIfNeeded();
        }
    }

    final class TraversalRunnable implements Runnable {
        @Override
        public void run() {
            doTraversal();
        }
    }
    final TraversalRunnable mTraversalRunnable = new TraversalRunnable();

    void doTraversal() {
        if (mTraversalScheduled) {
            mTraversalScheduled = false;
            //移除同步屏障
            mHandler.getLooper().getQueue().removeSyncBarrier(mTraversalBarrier);
            ...
            //开始三大绘制流程
            performTraversals();
            ...
        }
    }
```
调用了mChoreographer.postCallback()方法，发送一个会在下一帧执行的回调，即在下一个VSync到来时会执行
TraversalRunnable-->doTraversal()--->performTraversals()-->绘制流程

setView方法
```
public void setView(View view, WindowManager.LayoutParams attrs, View panelParentView) {
  synchronized (this) {
      if (mView == null) {
      //1 保存了控件树的根
          mView = view;
          //2 保存了窗口对应的layoutParams
          mWindowAttributes.copyFrom(attrs);
    //在添加窗口之前，先通过requestLayout方法在主线程上安排一次遍历。所谓的遍历是指ViewRootImpl的核心方法performTraversals
    //这个方法实现对控件树进行测量，布局，向WMS申请修改窗口属性以及重绘的所有工作。由于此遍历操作对于初次遍历做了特殊处理，
    //而来自WMS通过mWindow发生的回调会导致一些属性发生变化，如窗口的尺寸，Insets以及窗口焦点等，从而有可能使得初次遍历的现场
    //遭到破坏。因此需要在添加窗口之前，先发送一个遍历消息到主线程
    //在主线程中向主线程的Handler发送消息，如果使用得当，可以产生很精妙的效果。例如本例中可以实现如下的执行顺序
    //添加窗口-》初次遍历-》处理来自WMS的回调
    requestLayout();
    if ((mWindowAttributes.inputFeatures
                  & WindowManager.LayoutParams.INPUT_FEATURE_NO_INPUT_CHANNEL) == 0) {
          //3 初始化inputChannel,接受来自InputDispatcher的输入事件管道，窗口的属性inputFeatures不包含INPUT_FEATURE_NO_INPUT_CHANNEL
           mInputChannel = new InputChannel();
          }
        }
    try{
    //将窗口添加到WMS中。完成这个操作后，mWindow已经被添加到指定的Display中而且mInputChannel已经准备好接收事件
    //只是由于这个窗口没有进行relayout，因此它还没有有效的surface进行绘制
    res = mWindowSession.addToDisplay(mWindow, mSeq, mWindowAttributes,
      getHostVisibility(), mDisplay.getDisplayId(),
      mAttachInfo.mContentInsets, mAttachInfo.mStableInsets,
      mAttachInfo.mOutsets, mInputChannel);
    }
    ...
    if (res < WindowManagerGlobal.ADD_OKAY) {
      //窗口添加失败的错误处理，通常是权限问题，重复添加或者token失效
    }
    //4 如果mInputChannel不为空，则创建mInputEventReceiver用于接收输入事件
    //注意第二个参数传递的是Looper.myLooper，即mInputEventReceiver将在主线程上触发输入事件的读取与onInputEvent
    //这是应用程序可以在onTouch等事件响应中直接进行UI操作等的根本原因
          if (mInputChannel != null) {
          mInputEventReceiver = new WindowInputEventReceiver(mInputChannel,
                  Looper.myLooper());
      }
  //ViewRootImpl作为参数view的parent，所以ViewRootImpl可以从控件树中任何一个控件开始，通过回溯getParent方法得到
  view.assignParent(this);
 }
}
```

ViewRootImpl创建过程是由构造函数和setView两个环节构成的
构造函数主要进行成员的初始化
setView主要是创建窗口，建立输入事件接收机制的场所，同时触发第一次遍历操作的消息已经发送给主线程，在随后的第一次遍历完成后
ViewRootImpl将会完成对控件树的第一次测量，布局，并从WMS获取窗口的surface以进行控件树的初次绘制工作
//todo surface的初始化


控件系统的心跳performTraversals
ViewRootImpl中接收的各种变化，如来自WMS的窗口属性变化，来自控件树的尺寸变化及重绘请求等都会引发performTraversals的调用，
并在其中完成处理。View类及子类中的onMeasure,onLayout以及onDraw等回调也是在performTraversals的执行过程中直接或间接地引发
一旦此方法无法正常执行，整个控件树都将处于僵死状态
工作阶段
1 预测量阶段  对控件树进行第一次测量。结果可通过mView.getMeasuredWidth/Heigeht获得。在此阶段中将会计算出控件树为显示
  其内容所需的尺寸，即期望的窗口尺寸。这个阶段中View及子类的onMeasure方法会调用
2 布局窗口阶段  根据预测量的结果，通过IWindowSession.relayout方法向WMS请求调整窗口的尺寸等属性，这将引发WMS对窗口的
  重新布局，并将布局结果返回给ViewRootImpl
3 最终测量阶段。   预测量的结果是控件树所期望的窗口尺寸。然后由于在WMS中影响窗口布局的因素很多，WMS不一定会将窗口准确的布局
  为控件树所需要的尺寸，而迫于WMS作为系统服务的强势地位，控件树不得不接收WMS的布局结果。因此这个阶段中，performTraversals
  将以窗口的实际尺寸对控件进行最终测量。这个阶段中View及子类的onMeasure方法会调用
4 布局控件树阶段。  测量确定的是控件的尺寸，而布局确定的是控件的位置。这个阶段View及子类的onLayout方法会调用
5 绘制阶段  view及子类的onDraw方法会被调用


预测量与测量原理
1 预测量参数的候选
  预测量与最终测量的区别在于参数不同，实际的测量工作在View及子类的onMeasure中完成，并且其测量结果受限于父控件的指示
  widthSpec与heightSpec，被称为MeasureSpec的复合整形变量
  32,31位代表SPEC_MODE   30,1位代表SPEC_SIZE
  SPEC_MODE取决于控件的LayoutParams.width/height,可以有三个值
     MeasureSpec.UNSPECIFIED(0) 在进行测量时，可以无视SPEC_SIZE，可以是它期望的任意尺寸
     MeasureSpec.EXACTLY(1) 子空间必须是SPEC_SIZE所指定的尺寸。当控件的LayoutParams.width/height为一确定值，重复添加或者
        是MATH_PARENT时，对应的MeasureSpec参数会使用这个SPEC_MODE
     MeasureSpec.AT_MOST(3) 子控件可以是它所期望的尺寸，但是不得大于SPEC_SIZE。当控件的LayoutParams.width/height
        为WARP_CONTENT时，对应的MeasureSpec参数会使用这个SPEC_MODE
  android 提供了一个MeasureSpec类用于组合两个分量成为一个MeasureSpec,或者从MeasureSpec分离任何一个分量
  ```
  // 1. 获取测量模式（Mode）
  int specMode = MeasureSpec.getMode(measureSpec)
  // 2. 获取测量大小（Size）
  int specSize = MeasureSpec.getSize(measureSpec)
  // 3. 通过Mode 和 Size 生成新的SpecMode
  int measureSpec=MeasureSpec.makeMeasureSpec(size, mode);
  ```

  ViewRootImpl如何为控件树的根mView准备其MeasureSpec？
  /frameworks/base/core/java/android/view/ViewRootImpl.java
 ```
 private void performTraversals() {
 //将mView保存在局部变量host中，以提高mView的访问效率
   final View host = mView;
   //代表窗口的最新尺寸
   Rect frame = mWinFrame;
   //第一次遍历，此时窗口刚刚被添加到WMS，此时窗口尚未进行relayout,因此mWinFrame没有存储有效的窗口尺寸
   if (mFirst) {
      final Configuration config = mContext.getResources().getConfiguration();
      if (shouldUseDisplaySize(lp)) {
        //为状态栏设置desiredWindowWidth/height ，大小为屏幕尺寸
                  Point size = new Point();
                  mDisplay.getRealSize(size);
                  desiredWindowWidth = size.x;
                  desiredWindowHeight = size.y;
              } else {
         //第一次遍历的测量，采用了应用可以使用的最大尺寸作为SPEC_SIZE的候选
                  desiredWindowWidth = dipToPx(config.screenWidthDp);
                  desiredWindowHeight = dipToPx(config.screenHeightDp);
              }
    //由于这是第一次进行遍历，控件即将第一次被显示在窗口上，下面的代码填充了mAttachInfo中的一些字段，然后通过mView发起
    //dispatchAttachedToWindow的调用，之后每一个位于控件树的控件都会回调onAttachedToWindow
   }else{
     //2 在非第一次遍历的情况下，会采用窗口的最新尺寸作为SPEC_SIZE的候选
         desiredWindowWidth = frame.width();
              desiredWindowHeight = frame.height();
        //如果窗口的最新尺寸与ViewRootImpl中的现有尺寸不同，说明WMS单方面改变了窗口的尺寸
              if (desiredWindowWidth != mWidth || desiredWindowHeight != mHeight) {
                  //需要进行完整的重绘以适应新的窗口尺寸
                  mFullRedrawNeeded = true;
                  //需要对控件进行重新布局
                  mLayoutRequested = true;
                  //控件树有可能拒绝接受新的窗口尺寸，比如在随后的预测量中给出不同于窗口尺寸的测量结果。
                  //产生这种情况就需要在窗口布局阶段尝试设置新的窗口尺寸
                  windowSizeMayChange = true;
              }
   }
   //执行位于RunQueue中的回调。RunQueue是ViewRoot•工mpl的一个辞态成员，就是说它是进程唯一的，
   //并且可以在进程的任何位置访问RunQueue。在进行多线程任务时，开发者可以通过调用View.post ()或View. postDelayed ()方法
   //将一个Runnable对象发送到主线程执行。这两个方法的原理是将Runnable 对象发送到ViewRootlmpl的mHandler。
   //当控件巳经加入控件树时，可以通过Attachlnfo轻易获取 这个Handler。而当控件没有位于控件树中时，则没有mAttachlnfo可用，
   //此时执行View.post。/PostDelay() 方法，Runnable将会被添加到这个RunQueue队列中。
   //  在这里，ViewRootlmpl将会把RunQueue中的Runnable发送到mHandler,进而得到执行。所以无 论控件是否显示在控件树中，
   //View.post()/postDelay 0方法都是可用的，除非当前进程中没有任何处于 活动状态的ViewRootImpl
   getRunQueue().executeActions(mAttachInfo.mHandler);
   boolean layoutRequested = mLayoutRequested && (!mStopped || mReportNextDraw);
   //当layoutRequested为ture开始预测量。代表进行遍历之前requestLayout方法被调用过
   //requestLayout方法用于要求ViewRootImpl进行一次遍历并对控件树重新进行测量与布局
    if (layoutRequested) {
       final Resources res = mView.getContext().getResources();
       if (mFirst) {
         //确定控件树是否需要进入TouchMode 后面介绍
       }else{
         //检查WMS是否单方面改变了ContentInsets和VisibleInsets
         //ContentInsets描述了控件在布局时必须预留的控件，这样会影响控件树的布局，以此作为是否进行控件布局的条件之一
         //VisibleInsetsm描述了被遮挡的控件，ViewRootImpl在进行绘制时，需要调整绘制位置以保证关键控件或区域
         //如正在进行输入的TextView等不被遮挡，这样mVisibleInsets的变化不会导致重新布局，所以这里仅仅进行保存
         //以便绘制时使用
         if (!mPendingContentInsets.equals(mAttachInfo.mContentInsets)) {
                             insetsChanged = true;
                         }
          if (!mPendingVisibleInsets.equals(mAttachInfo.mVisibleInsets)) {
                                mAttachInfo.mVisibleInsets.set(mPendingVisibleInsets);
                            }
         //当窗口的with或height被指定为WRAP_CONTENT，表示为一个悬浮窗口
         //此时会对desiredWindowWidth/height进行调整。在前面，这两个值被设置为窗口的当前尺寸。而根据MeasureSpec的要求
         //测量结果不得大于SPEC——SIZE，然而如果这个悬浮窗口需要更大的尺寸以完整显示其内容时，例如为AlerDialog设置了一个
         //更长的消息内容，如此取值将导致无法得到足够大的测量结果，内容无法完整显示
         //对于此类窗口，ViewRootImpl会调整desiredWindowWidth/height为此应用可以使用的最大尺寸
         if (lp.width == ViewGroup.LayoutParams.WRAP_CONTENT
                           || lp.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
                       //悬浮窗口的尺寸取决于测量结果，有可能向WMS申请改变窗口的尺寸
                       windowSizeMayChange = true;
                       if (shouldUseDisplaySize(lp)) {
                       } else {
                        //3 设置悬浮窗口SPEC_SIZE的候选为应用可以使用的最大尺寸
                           Configuration config = res.getConfiguration();
                           desiredWindowWidth = dipToPx(config.screenWidthDp);
                           desiredWindowHeight = dipToPx(config.screenHeightDp);
                       }
                   }
        //4 进行预测量
        windowSizeMayChange |= measureHierarchy(host, lp, res,
                              desiredWindowWidth, desiredWindowHeight);
       }
    }
 }
 ```
 预测量时SPEC——Size按照如下取值：
 1第一次遍历时，使用应用可用的最大尺寸最为SPEC_Size的候选
 2 此窗口是一个悬浮窗口，即LayoutParams.widht/height其中之一被指定为WRAP_CONTENT，使用应用可用的最大尺寸作为SPEC_SIZE候选
 3 其他情况下，使用窗口最新尺寸作为SPEC_Size的候选
 4 最后通过measureHierarchy进行测量
 Hierarchy  [ˈhaɪərɑːki]  等级，层次层级

测量协商
measureHierarchy()用于测量整个控件树。传入的参数desiredWindowWidth与desiredWindowHeight在前述代码中根据不同的情况作了精心的挑选。
控件树本可以按照这两个参数完成测量，但是measureHierarchy()有自己的考量，即如何将窗口布局地尽可能地优雅。
这是针对将LayoutParams.width设置为了WRAP_CONTENT的悬浮窗口而言。如前文所述，在设置为WRAP_CONTENT时，
指定的desiredWindowWidth是应用可用的最大宽度，如此可能会产生如图6-6左图所示的丑陋布局。这种情况较容易发生在AlertDialog中，
当AlertDialog需要显示一条比较长的消息时，由于给予的宽度足够大，因此它有可能将这条消息以一行显示，并使得其窗口充满了整个屏幕宽度，
在横屏模式下这种布局尤为丑陋。
倘若能够对可用宽度进行适当的限制，迫使AlertDialog将消息换行显示，则产生的布局结果将会优雅得多，如图6-6右图所示。但是，
倘若不分清红皂白地对宽度进行限制，当控件树真正需要足够的横向空间时，会导致内容无法显示完全，或者无法达到最佳的显示效果。
例如当一个悬浮窗口希望尽可能大地显示一张照片时就会出现这样的情况。
丑陋的布局与优雅的布局.png

 那么measureHierarchy如何解决这个问题呢？它采取了与控件树进行协商的办法
 即先使用measureHierarchy所期望的宽度限制尝试对控件树进行测量，然后通过测量
 结果来检查控件树是否能够在此限制下满足其充分显示内容的要求。若无法满足，则
 measureHierarchyO进行让步，放宽对宽度的限制，然后再次进行测量，再做检查。若仍不
 能满足则再度进行让步
```
private boolean measureHierarchy(final View host, final WindowManager.LayoutParams lp,
              final Resources res, final int desiredWindowWidth, final int desiredWindowHeight) {
    intchildWidthMeasureSpec; // 合成后的用于描述宽度的MeasureSpec
    intchildHeightMeasureSpec; // 合成后的用于描述高度的MeasureSpec
    booleanwindowSizeMayChange = false; // 表示测量结果是否可能导致窗口的尺寸发生变化
    booleangoodMeasure = false; // goodMeasure表示了测量是否能满足控件树充分显示内容的要求
    // 测量协商仅发生在LayoutParams.width被指定为WRAP_CONTENT的情况下
    if(lp.width == ViewGroup.LayoutParams.WRAP_CONTENT) {
        /* ① 第一次协商。measureHierarchy()使用它最期望的宽度限制进行测量。这一宽度限制定义为
         一个系统资源。可以在frameworks/base/core/res/res/values/config.xml找到它的定义 */
       res.getValue(com.android.internal.R.dimen.config_prefDialogWidth,mTmpValue, true);
       intbaseSize = 0;
        // 宽度限制被存放在baseSize中
        if(mTmpValue.type == TypedValue.TYPE_DIMENSION) {
           baseSize = (int)mTmpValue.getDimension(packageMetrics);
        }
       if(baseSize != 0 && desiredWindowWidth > baseSize) {
           // 使用getRootMeasureSpec()函数组合SPEC_MODE与SPEC_SIZE为一个MeasureSpec
           childWidthMeasureSpec = getRootMeasureSpec(baseSize, lp.width);
            childHeightMeasureSpec =getRootMeasureSpec(desiredWindowHeight,lp.height);
            //②第一次测量。由performMeasure()方法完成
           performMeasure(childWidthMeasureSpec, childHeightMeasureSpec);
           /* 控件树的测量结果可以通过mView的getmeasuredWidthAndState()方法获取。如果
             控件树对这个测量结果不满意，则会在返回值中添加MEASURED_STATE_TOO_SMALL位 */
           if ((host.getMeasuredWidthAndState()&View.MEASURED_STATE_TOO_SMALL)==0) {
               goodMeasure = true; // 控件树对测量结果满意，测量完成
       } else {
               // ③ 第二次协商。上次测量结果表明控件树认为measureHierarchy()给予的宽度太小，
                 在此适当地放宽对宽度的限制，使用最大宽度与期望宽度的中间值作为宽度限制 */
               baseSize = (baseSize+desiredWindowWidth)/2;
               childWidthMeasureSpec = getRootMeasureSpec(baseSize, lp.width);
               // ④ 第二次测量
               performMeasure(childWidthMeasureSpec, childHeightMeasureSpec);
               // 再次检查控件树是否满足此次测量
               if ((host.getMeasuredWidthAndState()&View.MEASURED_STATE_TOO_SMALL)                                                                                == 0) {
                   goodMeasure = true; // 控件树对测量结果满意，测量完成
               }
             }
        }
    }
     if(!goodMeasure) {
            /* ⑤ 最终测量。当控件树对上述两次协商的结果都不满意时，measureHierarchy()放弃所有限制
              做最终测量。这一次将不再检查控件树是否满意了，因为即便其不满意，measurehierarchy()也没
              有更多的空间供其使用了 */
           childWidthMeasureSpec = getRootMeasureSpec(desiredWindowWidth,lp.width);
           childHeightMeasureSpec = getRootMeasureSpec(desiredWindowHeight,lp.height);
           performMeasure(childWidthMeasureSpec, childHeightMeasureSpec);
        /* 最后，如果测量结果与ViewRootImpl中当前的窗口尺寸不一致，则表明随后可能有必要进行窗口
          尺寸的调整 */
        if(mWidth != host.getMeasuredWidth() || mHeight != host.getMeasuredHeight())
        {
           windowSizeMayChange = true;
        }
    }
    // 返回窗口尺寸是否可能需要发生变化
    return windowSizeMayChange;
    }
```
显然，对于非悬浮窗口，即当LayoutParams.width被设置为MATCH_PARENT时，不存在协商过程，直接使用给定的desiredWindowWidth/Height进行测量即可。
而对于悬浮窗口，measureHierarchy()可以连续进行两次让步。因而在最不利的情况下，在ViewRootImpl的一次“遍历”中，
控件树需要进行三次测量，即控件树中的每一个View.onMeasure()会被连续调用三次之多，如图6-7所示。
所以相对于onLayout()，onMeasure()方法的对性能的影响比较大。
协商测量的三次尝试.png


测量原理
performMeasure()方法的实现非常简单，它直接调用mView.measure()方法，将measureHierarchy()给予的widthSpec与heightSpec
交给mView。
 /frameworks/base/core/java/android/view/ViewRootImpl.java
 ```
 private void performMeasure(int childWidthMeasureSpec, int childHeightMeasureSpec) {
     if (mView == null) {
         return;
     }
     mView.measure(childWidthMeasureSpec, childHeightMeasureSpec);
 }
 ```
/frameworks/base/core/java/android/view/View.java
```
public final void measure(int widthMeasureSpec, int heightMeasureSpec) {
/* 仅当给予的MeasureSpec发生变化，或要求强制重新布局时，才会进行测量。
      所谓强制重新布局，是指当控件树中的一个子控件的内容发生变化时，需要进行重新的测量和布局的情况
      在这种情况下，这个子控件的父控件（以及其父控件的父控件）所提供的MeasureSpec必定与上次测量
      时的值相同，因而导致从ViewRootImpl到这个控件的路径上的父控件的measure()方法无法得到执行
      进而导致子控件无法重新测量其尺寸或布局。因此，当子控件因内容发生变化时，从子控件沿着控件树回溯
      到ViewRootImpl，并依次调用沿途父控件的requestLayout()方法，在这个方法中，会在
     mPrivateFlags中加入标记PFLAG_FORCE_LAYOUT，从而使得这些父控件的measure()方法得以顺利
      执行，进而这个子控件有机会进行重新测量与布局。这便是强制重新布局的意义 */
 final boolean forceLayout = (mPrivateFlags & PFLAG_FORCE_LAYOUT) == PFLAG_FORCE_LAYOUT;
final boolean specChanged = widthMeasureSpec != mOldWidthMeasureSpec
       || heightMeasureSpec != mOldHeightMeasureSpec;
final boolean isSpecExactly = MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY
       && MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY;
final boolean matchesSpecSize = getMeasuredWidth() == MeasureSpec.getSize(widthMeasureSpec)
       && getMeasuredHeight() == MeasureSpec.getSize(heightMeasureSpec);
final boolean needsLayout = specChanged
       && (sAlwaysRemeasureExactly || !isSpecExactly || !matchesSpecSize);
if (forceLayout || needsLayout) {
  /* ① 准备工作。从mPrivateFlags中将PFLAG_MEASURED_DIMENSION_SET标记去除。
         PFLAG_MEASURED_DIMENSION_SET标记用于检查控件在onMeasure()方法中是否通过
         调用setMeasuredDimension()将测量结果存储下来 */
  mPrivateFlags &= ~PFLAG_MEASURED_DIMENSION_SET;
  /* ② 对本控件进行测量 每个View子类都需要重载这个方法以便正确地对自身进行测量。
            View类的onMeasure()方法仅仅根据背景Drawable或style中设置的最小尺寸作为测量结果*/
         onMeasure(widthMeasureSpec, heightMeasureSpec);
 /* ③ 检查onMeasure()的实现是否调用了setMeasuredDimension()
          setMeasuredDimension()会将PFLAG_MEASURED_DIMENSION_SET标记重新加入
          mPrivateFlags中。之所以做这样的检查，是由于onMeasure()的实现可能由开发者完成，
           而在Android看来，开发者是不可信的 */
  if((mPrivateFlags & PFLAG_MEASURED_DIMENSION_SET)!=PFLAG_MEASURED_DIMENSION_SET) {
             throw new IllegalStateException(......);
          }
  // ④ 将PFLAG_LAYOUT_REQUIRED标记加入mPrivateFlags。这一操作会对随后的布局操作放行
         mPrivateFlags |= PFLAG_LAYOUT_REQUIRED;
}
// 记录父控件给予的MeasureSpec，用以检查之后的测量操作是否有必要进行
   mOldWidthMeasureSpec = widthMeasureSpec;
   mOldHeightMeasureSpec = heightMeasureSpec;
}
```
View.measure()方法没有实现任何测量算法，它的作用在于引发onMeasure()的调用，并对onMeasure()行为的正确性进行检查。
另外，在控件系统看来，一旦控件执行了测量操作，那么随后必须进行布局操作，因此在完成测量之后，将PFLAG_LAYOUT_REQUIRED标记
加入mPrivateFlags，以便View.layout()方法可以顺利进行
/frameworks/base/core/java/android/view/View.java
```
 protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
          setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec),
                  getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec));
      }
```
onMeasure()的结果通过setMeasuredDimension()方法尽行保存
setMeasuredDimension调用 setMeasuredDimensionRaw
/frameworks/base/core/java/android/view/View.java
```
  private void setMeasuredDimensionRaw(int measuredWidth, int measuredHeight) {
      //1 测量结果被分别保存在成员变量mMeasuredWidth与mMeasuredHeight中
          mMeasuredWidth = measuredWidth;
          mMeasuredHeight = measuredHeight;
     //2 向mPrivateFlags中添加PFALG_MEASURED_DIMENSION_SET，以此证明onMeasure()保存了测量结果
          mPrivateFlags |= PFLAG_MEASURED_DIMENSION_SET;
      }
```
存储测量结果的两个变量可以通过getMeasuredWidthAndState()与getMeasuredHeightAndState()两个方法获得
/frameworks/base/core/java/android/view/View.java
```
 public final int getMeasuredWidthAndState() {
          return mMeasuredWidth;
      }
```
与MeasureSpec类似，测量结果不仅仅是一个尺寸，而是一个测量状态与尺寸的复合整、变量。其0至30位表示了测量结果的尺寸，
而31、32位则表示了控件对测量结果是否满意，即父控件给予的MeasureSpec是否可以使得控件完整地显示其内容。当控件对测量结果满意时，
直接将尺寸传递给setMeasuredDimension()即可，注意要保证31、32位为0。倘若对测量结果不满意，
则使用View.MEASURED_STATE_TOO_SMALL | measuredSize 作为参数传递给setMeasuredDimension()以告知父控件对MeasureSpec进行可能的调整

onMeasure()方法应当如何实现的呢？对于非ViewGroup的控件来说其实现相对简单，只要按照MeasureSpec的原则如实计算其所需的尺寸即可。
而对于ViewGroup类型的控件来说情况则复杂得多，因为它不仅拥有自身需要显示的内容（如背景），它的子控件也是其需要测量的内容。
因此它不仅需要计算自身显示内容所需的尺寸，还有考虑其一系列子控件的测量结果。为此它必须为每一个子控件准备MeasureSpec，
并调用每一个子控件的measure()函数。

由于各种控件所实现的效果形形色色，开发者还可以根据需求自行开发新的控件，因此onMeasure()中的测量算法也会变化万千。
不过从Android系统实现的角度仍能得到如下的onMeasure()算法的一些实现原则：
·  控件在进行测量时，控件需要将它的Padding尺寸计算在内，因为Padding是其尺寸的一部分。
·  ViewGroup在进行测量时，需要将子控件的Margin尺寸计算在内。因为子控件的Margin尺寸是父控件尺寸的一部分。
· ViewGroup为子控件准备MeasureSpec时，SPEC_MODE应取决于子控件的LayoutParams.width/height的取值。
   取值为MATCH_PARENT或一个确定的尺寸时应为EXACTLY，WRAP_CONTENT时应为AT_MOST。至于SPEC_SIZE，应理解为ViewGroup对子控件尺寸的限制，
   即ViewGroup按照其实现意图所允许子控件获得的最大尺寸。并且需要扣除子控件的Margin尺寸。
·  虽然说测量的目的在于确定尺寸，与位置无关。但是子控件的位置是ViewGroup进行测量时必须要首先考虑的。
  因为子控件的位置即决定了子控件可用的剩余尺寸，也决定了父控件的尺寸（当父控件的LayoutParams.width/height为WRAP_CONTENT时）。
·  在测量结果中添加MEASURED_STATE_TOO_SMALL需要做到实事求是。当一个方向上的空间不足以显示其内容时应考虑利用另一个方向上的空间，
    例如对文字进行换行处理，因为添加这个标记有可能导致父控件对其进行重新测量从而降低效率。
·  当子控件的测量结果中包含MEASURED_STATE_TOO_SMALL标记时，只要有可能，父控件就应当调整给予子控件的MeasureSpec，
   并进行重新测量。倘若没有调整的余地，父控件也应当将MEASURED_STATE_TOO_SMALL加入到自己的测量结果中，让它的父控件尝试进行调整。
·  ViewGroup在测量子控件时必须调用子控件的measure()方法，而不能直接调用其onMeasure()方法。
   直接调用onMeasure()方法的最严重后果是子控件的PFLAG_LAYOUT_REQUIRED标识无法加入到mPrivateFlag中，
   从而导致子控件无法进行布局。
综上所述，测量控件树的实质是测量控件树的根控件。完成控件树的测量之后，ViewRootImpl便得知了控件树对窗口尺寸的需求。



确定是否需要改变窗口尺寸
接下来回到performTraversals()方法。在ViewRootImpl.measureHierarchy()执行完毕之后，ViewRootImpl了解了控件树所需的空间。
于是便可确定是否需要改变窗口窗口尺寸以便满足控件树的空间要求。前述的代码中多处设置windowSizeMayChange变量为true。w
indowSizeMayChange仅表示有可能需要改变窗口尺寸。而接下来的这段代码则用来确定窗口是否需要改变尺寸
/frameworks/base/core/java/android/view/ViewRootImpl.java
```
 private void performTraversals() {
  ......// 测量控件树的代码
    /* 标记mLayoutRequested为false。因此在此之后的代码中，倘若控件树中任何一个控件执行了
     requestLayout()，都会重新进行一次“遍历” */
    if (layoutRequested) {
        mLayoutRequested = false;
    }
    // 确定窗口是否确实需要进行尺寸的改变
    boolean windowShouldResize = layoutRequested && windowSizeMayChange
                  && ((mWidth != host.getMeasuredWidth() || mHeight != host.getMeasuredHeight())
                      || (lp.width == ViewGroup.LayoutParams.WRAP_CONTENT &&
                              frame.width() < desiredWindowWidth && frame.width() != mWidth)
                      || (lp.height == ViewGroup.LayoutParams.WRAP_CONTENT &&
                              frame.height() < desiredWindowHeight && frame.height() != mHeight));
}
```

确定窗口尺寸是否确实需要改变的必要条件
·  layoutRequested为true，即ViewRootImpl.requestLayout()方法被调用过。View中也有requestLayout()方法。
当控件内容发生变化从而需要调整其尺寸时，会调用其自身的requestLayout()，并且此方法会沿着控件树向根部回溯，
最终调用到ViewRootImp.requestLayout()，从而引发一次performTraversals()调用。之所以这是一个必要条件，
是因为performTraversals()还有可能因为控件需要重绘时被调用。当控件仅需要重绘而不需要重新布局时（例如背景色或前景色发生变化时），
会通过invalidate()方法回溯到ViewRootImpl，此时不会通过performTraversals()触发performTraversals()调用，
而是通过scheduleTraversals()进行触发。在这种情况下layoutRequested为false，即表示窗口尺寸不需发生变化。
·  windowSizeMayChange为true，如前文所讨论的，这意味着WMS单方面改变了窗口尺寸而控件树的测量结果与这一尺寸有差异，
或当前窗口为悬浮窗口，其控件树的测量结果将决定窗口的新尺寸。
在满足上述两个条件的情况下，以下两个条件满足其一：
·  测量结果与ViewRootImpl中所保存的当前尺寸有差异。
·  悬浮窗口的测量结果与窗口的最新尺寸有差异

注意ViewRootImpl对是否需要调整窗口尺寸的判断是非常小心的。第4章介绍WMS的布局子系统时曾经介绍过，调整窗口尺寸所必须调用的
performLayoutAndPlaceSurfacesLocked()函数会导致WMS对系统中的所有窗口新型重新布局，而且会引发至少一个动画帧渲染，
其计算开销相当之大。因此ViewRootImpl仅在必要时才会惊动WMS。 //todo wms performLayoutAndPlaceSurfacesLocked
至此，预测量阶段完成了
总结
这一阶段的工作内容是为了给后续阶段做参数的准备并且其中最重要的工作是对控件树的预测量，至此ViewRootImpl得知了控件树对窗口尺寸的要求。
另外，这一阶段还准备了后续阶段所需的其他参数：
·  viewVisibilityChanged。即View的可见性是否发生了变化。由于mView是窗口的内容，因此mView的可见性即是窗口的可见性。
当这一属性发生变化时，需要通过通过WMS改变窗口的可见性。
·   LayoutParams。预测量阶段需要收集应用到LayoutParams的改动，这些改动一方面来自于WindowManager.updateViewLayout()，
而另一方面则来自于控件树。以SystemUIVisibility为例，View.setSystemUIVisibility()所修改的设置需要反映到LayoutParams中，
而这些设置确却保存在控件自己的成员变量里。在预测量阶段会通过ViewRootImpl.collectViewAttributes()方法遍历控件树中的所有控件以收集这些设置，
然后更新LayoutParams。
//todo  viewVisibilityChanged和LayoutParams的收集

布局窗口与最终测量
接下来进入窗口布局阶段与最终测量阶段。 窗口布局阶段以 relayoutWindow方法为核
心 ， 并根据布局结果进行相应处理 。而当布局结果使得窗口尺寸发生改变时，最终测量阶段
将会被执行。最终测量使用performMeasure()方法完成，因此其过程与预测量完全一致，区
别仅在于MeasureSpec参数的不同 ，所以本小节将一并探讨这两个阶段 。 另外，由于布局窗
口会对Surface产生影响 ，这个阶段中会出现与硬件加速相关的代码 。 关于硬件加速的详细内容后面介绍

布局窗口的条件
如前文所述，窗口布局的开销很大，因此必须限制窗口布局阶段的执行。另外，倘大不
需要进行窗口布局，则WMS不会在预测量之后修改窗口的尺寸。在这种情况下预测量的结
果是有效的，因此不再需要进行最终测量。参考如下代码:
/frameworks/base/core/java/android/view/ViewRootImpl.java
```
private void performTraversals() {
//进行布局窗口的条件
if (mFirst || windowShouldResize || insetsChanged ||
                  viewVisibilityChanged || params != null || mForceNextWindowRelayout) {
 ...//布局窗口与最终测量阶段代码
}else{
   //倘若不符合执行布局窗口的条件，则说明窗口的尺寸不需要调整。有可能是窗口的位置发生了变化，需要将窗口的最新位置保存在mattachInfo
   maybeHandleWindowMove(frame);
 }
 ...//布局控件树阶段与绘制阶段代码
}

private void maybeHandleWindowMove(Rect frame) {
          final boolean windowMoved = mAttachInfo.mWindowLeft != frame.left
                  || mAttachInfo.mWindowTop != frame.top;
          if (windowMoved) {
              if (mTranslator != null) {
                  mTranslator.translateRectInScreenToAppWinFrame(frame);
              }
              mAttachInfo.mWindowLeft = frame.left;
              mAttachInfo.mWindowTop = frame.top;
          }
      }
```
在进行窗口布局时，以下5个条件满足其一即进入布局窗口阶段，此5个条件的意义
1 mFirst，即表示这是窗口创建以来的第一次“遍历*，此时窗口仅仅是添加到WMS中,但尚未进行窗口布局，并且没有有效的Surface进行内容绘制。
因此必须进行窗口布局。
2 windowShouldResize，正如在预测量阶段所述，当控件树的测量结果与窗口的当前尺寸有差异时，需要通过布局窗口阶段向WMS
 提出修改窗口尺寸的请求以满足控件树的要求。
3 insetsChanged，表示WMS单方面改变了窗口的ContentImsets。这种情况一般发生在SystemUI的可见性发生了变化或输入法窗口弹出
  或关闭的情况下(请参考第4章)。 //todo 第四章
  严格来说，在这种情况下不需要重新进行窗口布局，只不过当ContentInsets发生变化时，需要执行一段渐变动画使窗口的内容过渡
  到新的ContentImsets下，而这段动画的启动动作发生在窗口布局阶段。稍后的代码分析中将介绍ContentInsets的影响，以及这段动画
  的实现
4 params!=null，在进入performTraversals方法时，params变量被置为null，窗口的使用者通过WindowManager.updateViewLayoutO函数
  修改窗口的LayoutParams，或者在预测量阶段通过collectViewAttributes()函数收集到的控件属性使得LayoutParams发生变化时，
  params将被设置为新的LayoutParams，此时需要将新的LayoutParams通过窗口布局更新到WMS中使其对窗口依照新的属性进行重新布局。
当上述4个条件全部不满足时，表示窗口布局是不必要的，而且窗口的尺寸也没有发生变化，因此仅需将窗口的新位置(如果发生了变化)
更新到mAttachInfo中以供控件树查询。
//todo mForceNextWindowRelayout

布局窗口前的准备工作
```
private void performTraversals() {
   //1 记录下在布局窗口之前surface的版本号
   final int surfaceGenerationId = mSurface.getGenerationId();
  if（引发布局窗口的条件）{
    //2 记录下在布局窗口之前是否拥有一块有效的surface
    boolean hadSurface = mSurface.isValid();
    //3 布局窗口
    relayoutResult = relayoutWindow(params, viewVisibility, insetsPending);
  }
}
```
上述代码体现了在布局窗口前做两个准备工作:
hadSurface，保存在布局窗口之前是否拥有一个有效的Surface。当窗口第一次进行“遍历”，或当前正处于不可见状态
  (mView的Visibility为INVISIBLE或GONE)时不存在有效的Surface。此变量可以在完成窗口布局后决定是否初始化或销毁用于绘制
  的HardwareRenderer。
surfaceGenerationld，即Surface的版本号。每当WMS为窗口重新分配Surface时都会使得Surface的版本号增加。
  当完成窗口布局后Surface的版本号发生改变，在原Surface上建立的HardwareRenderer以及在其上进行的绘制都将无效，因此此变量用
  于决定窗口布局后是否需要将Surface重新设置到HardwareRenderer以及是否需要进行一次完整绘制。
可以看出，布局窗口的准备工作目的是保存布局前的状态，以便在布局后判断状态变化并做出相应处理，准备工作的另一个部分已经隐式
的准备好了，mWidth/mHeight,mAttachInfo.mLeft/mTop/mContent-Insets/mVisibleInsets


布局窗口
```
 private int relayoutWindow(WindowManager.LayoutParams params, int viewVisibility,
              boolean insetsPending) throws RemoteException {
int relayoutResult = mWindowSession.relayout(
                  mWindow, mSeq, params,
                  (int) (mView.getMeasuredWidth() * appScale + 0.5f),
                  (int) (mView.getMeasuredHeight() * appScale + 0.5f),
                  viewVisibility, insetsPending ? WindowManagerGlobal.RELAYOUT_INSETS_PENDING : 0,
                  mWinFrame, mPendingOverscanInsets, mPendingContentInsets, mPendingVisibleInsets,
                  mPendingStableInsets, mPendingOutsets, mPendingBackDropFrame,
                  mPendingMergedConfiguration, mSurface);
              }
return relayoutResult;
```
最终调用WMS.relayoutWindow方法

relayoutWindowO并没有直接将预测量的结果区给WMS，而是乘以了appScale这个系数。appScale用于在兼容模式下显示一个窗口。
当窗口在设备的屏幕尺寸下显示异常时，Android会尝试使用兼容尺寸显示它(例如320X480)，此时测量与布局控件树都将以此兼容尺寸为准。
为了充分利用屏幕，或避免窗口内容显示在屏幕外，Android计算了用以使兼容尺寸变换到屏幕尺寸的一个缩放系数，即appScale。
这时窗口测量、控件树的布局都将以兼容尺寸进行以保证布局的正确性，而生成Surface，在绘制过程中将会使用appScale进行缩放，
以保证最终显示的内容能够充分利用屏幕
另外，传出参数mPendingConfiguration在之前的章节中并没有做过详细介绍。作为一个Configuration类型的实例，其意义是
WMS给予窗口的当前配置。其中的字段描述了设备当前的语言、屏幕尺寸、输入方式(触屏或键盘)、UI模式(夜间模式、车载模式等)、dpi等。
其中WMS有可能更改的最常用的字段是orientation，即屏幕方向。
//todo WMS的Configuration

布局窗口后的处理-Insets
relayoutWindow 方法完成布局窗口后，回到performTraversals，对布局结果进行处理。首先是ContentInsets和VisibleInsets
```
private void performTraversals() {
    //todo 其他inset的改变是啥
    //1 对比布局结果检查insets是否发生了变化
    contentInsetsChanged = !mPendingContentInsets.equals(mAttachInfo.mContentInsets);
    final boolean visibleInsetsChanged = !mPendingVisibleInsets.equals(mAttachInfo.mVisibleInsets);
    /*启动过渡动画以避免内容发生突开的拌动。其条件非常多，总结一下:
    1>布局窗口之前mwidth/mHeight有效，即之前曾经完成过布局，即布局有效，此时可以依照Insets发生变化前的布局进行绘制。
    2>systemiUIVisibility没有指定要求隐藏状态栏或导航栏。因为当指定了此类systemUIVisibility后，控件树布局时将
      不会考虑ContentInsets而是充满屏幕。
    3>拥有有效的Surface，这个条件是不言而喻的。
    4>此窗口采用硬件加速方式进行绘制，并且其HardwareRenderer处于有效状态。因为这一过渡动画是以硬件加速方式实现的*/
    5>窗口的Surface不得支持透明度。因为Android当前的硬件实现不支持在支持透明度的Surface上进行透明度变换。而这个过渡动画
       正是一个透明度动画*/
    /*启动一个透明度动画，使得ContentInsets发生变化时产生的画面移位不那么突兀。在介绍硬件加速绘制之后再讨论这一动画的细节*/
    if (contentInsetsChanged) {
      //2 将最新的ContentInset保存到AttachInfo中
      AttachInfo.mContentInsets.set(mPendingContentInsets);
     }
    if (contentInsetsChanged || mLastSystemUiVisibility !=
          mAttachInfo.mSystemUiVisibility || mApplyInsetsRequested
          || mLastOverscanRequested != mAttachInfo.mOverscanRequested
          || outsetsChanged) {
      mLastSystemUiVisibility = mAttachInfo.mSystemUiVisibility;
      mLastOverscanRequested = mAttachInfo.mOverscanRequested;
      mAttachInfo.mOutsets.set(mPendingOutsets);
      mApplyInsetsRequested = false;
      //3 要mView及子控件适应这一ContentInsets
      //View.fitSystemwindow()将会把ContentInsets作为其Padding属性保存下来。
      Padding是指控件的边界到其内容边界的距离。在测量布局及绘制时需杰将Pandding属性
      计算在内*/
      dispatchApplyInsets(host);
   }
  /*4 如果VisibleInsets发生变化，将其保存到mAttachInfo中 如前所述，VisibleInsets不影响测量与布局，
  而仅仅影响绘制时的偏移，因此除了将其保存下来供绘制时使用以外无须进行其他操作*/
   if (visibleInsetsChanged) {
      mAttachInfo.mVisibleInsets.set(mPendingVisibleInsets);
     }
}

 void dispatchApplyInsets(View host) {
         //最终调用view.fitSystemwindow
          host.dispatchApplyWindowInsets(getWindowInsets(true /* forceConstruct */));
      }
```
可见，当ContentInsets发生变化时，会进行如下动作:
1 启动一个过渡动画以避免突元的画面位移。参考图6-8，当状态栏被隐藏时窗口顶部的ContentImsets为0，内容布局在屏幕的顶部。
当状态栏重新显示时窗口顶部的ContentInsets为状态栏的高度，此时内容将会向下错位，布局在状态栏的底部。在用户看来，
ContentInsets变化所导致的是窗口内容的突死抖动，因此ViewRootImpl通过一个过渡动画使得画面从左图以渐变的方式过渡到右图
以减轻这种突兀感。这一动画的渲染过程将在6.4节介绍。
ContentInsets变化导致的内容错位.png
2 保存新的ContentInsets到mAttachInfo。
3 通过执行mView.fitSystemWindows()使控件树应用新的ContentInsets。其实质是设置控件的Padding属性以便在其进行测量布局与绘制时
让出ContentInsets所指定的空间。从此方法在ViewGroup中的实现可以看出它与onMeasure()一样会沿着控件树遍历给所有的控件。
不过在Activity、Dialog等由Android辅助添加的窗口中，此操作会被其控件树的根控件DecorView阻断，即DecorView完成其Padding设置
后便不会再将此操作传递给其子控件。其合理性在于DecorView作为整个控件树的根，只要它设置好Padding值并在测量布局以及绘制时
让出ContentInsets指定的空间，其子控件必然会位于ContentInsets之外，因此子控件不需要对ContentInsets进行处理。
当使用WindowManager.addView()方法添加一个控件作为窗口时，可以调用此控件的makeOptionalFitsSystemWindows()方法
使此控件拥有DecorView的这一特性。
//todo ViewGroup和DecorView fitSystemWindows    makeOptionalFitsSystemWindows

而当VisibleInsets发生变化时则简单得多，仅需保存新的值即可。在绘制前会根据此值计算绘制偏移，以保证关键控件或区域位于可视区域之内。



布局窗口后的处理--surface
完成Insets的处理后，便会处理surface在窗口布局过程中的变化
```
private void performTraversals() {
if (!hadSurface) {
  if (mSurface.isValid()) {
      //1 布局窗口前没有有效的surface，而布局窗口之后有了
      // 当此窗口启用硬件加速时(mThreadedRenderer != null),将使用新的surface初始化用于硬件渲染的ThreadedRenderer
      // 第一次遍历或窗口从不可见变为可见时符合这种情况
      if (mAttachInfo.mThreadedRenderer != null) {
          try {
              hwInitialized = mAttachInfo.mThreadedRenderer.initialize(
                      mSurface);
          } catch (OutOfResourcesException e) {
            ...
          }
      }
  }
}else if(!mSurface.isValid()){
 //2 布局窗口之前拥有有效的surface，但布局窗口之后没有了
 //当窗口启用硬件加速时则销毁当前使用的HardwareRenderer，因为不需要再进行绘制。
 //当窗口从可见弯为不可由或窗口被移除时符合此种情况
 if (mAttachInfo.mThreadedRenderer != null &&
          mAttachInfo.mThreadedRenderer.isEnabled()) {
      mAttachInfo.mThreadedRenderer.destroy();
  }
}else if((surfaceGenerationId != mSurface.getGenerationId()
                                   || surfaceSizeChanged || windowRelayoutWasForced)
                                   && mSurfaceHolder == null
                                   && mAttachInfo.mThreadedRenderer != null){
    //3 经过窗口布局后，surface发生了改变
    //底层的surface被置换了，此时需要将surface重新设置给ThreadedRenderer以将其与底层新的surface进行绑定
    try{
     mAttachInfo.mThreadedRenderer.updateSurface(mSurface);
    }...
}
```
显然，处理Surface的变化是为了硬件加速而服务的。其原因在于以软件方式进行绘制时可以通过Surface.lockCanvas()函数直接获得，
因此仅需在绘制前判断一下Surface.isValid决定是否绘制即可。
而在硬件加速绘制的情况下，将绘制过程委托给ThreadedRenderer并且需要将其与一个有效的Surface进行绑定。因此每当Surface的
状态变换都需要通知ThreadedRenderer。
软件与硬件加速的绘制原理将在6.4节中介绍。
另外，代码中的mHolder是一个空的SurfaceHolder子类的实例，其getSurface方法所返回的Surface就是mSurface。
在这段代码之后，还有一段类似的代码处理Surface状态的变化，并将状态变化通过一个SurfaceHolder汇报给mSurfaceHolderCallback。
这段代码为了支持一种叫作Native-Activity的机制而实现的。NativeActivity是一类Activity，其逻辑实现由C++完成并由NDK进行编译。
由于NativeActivity可以直接访问Surface，ViewRootImpl需要通过这段代码通知其Surface的状态变化
//todo NativeActivity

布局窗口后的处理--更新尺寸
接下来，ViewRootImpl会保存最新的窗口位置与尺寸，如果窗口采用了硬件加速，则更新其坐标系以适应新的窗口尺寸
```
private void performTraversals() {
     //1 保存窗口的位置与尺寸
     mAttachInfo.mWindowLeft = frame.left;
     mAttachInfo.mWindowTop = frame.top;
     if (mWidth != frame.width() || mHeight != frame.height()) {
                       mWidth = frame.width();
                       mHeight = frame.height();
                   }
     /2 更新尺寸信息到threadedRenderer
     //threadedRenderer的setUp方法将会使用此宽高信息来设置ViewPort,这样便可以在Surface上建立(0,0,mWidth,mHeight)的坐标系
     final ThreadedRenderer threadedRenderer = mAttachInfo.mThreadedRenderer;
      if (threadedRenderer != null && threadedRenderer.isEnabled()) {
          if (hwInitialized
                  || mWidth != threadedRenderer.getWidth()
                  || mHeight != threadedRenderer.getHeight()
                  || mNeedsRendererSetup) {
              threadedRenderer.setup(mWidth, mHeight, mAttachInfo,
                      mWindowAttributes.surfaceInsets);
              mNeedsRendererSetup = false;
          }
      }
}
```
在最终的窗口位置与尺寸得到更新后，便进入下一阶段--窗口的最终测量


最终测量
最终测量与预测量阶段一样使用performMeasure，区别在于参数不同
预测量使用了屏幕的可用空间或窗口的当前尺寸作为候选，使用measureHierarchy()方法以协商的方式确定MeasureSpec参数，
并且其测量结果体现了控件树所期望的窗口尺寸。在窗口布局时这一期望尺寸交给WMS以期能将窗口重新布局为这一尺
寸，然而由于WMS布局窗口时所考虑的因素很多，这一期望不一定如愿。
在最终测量阶段，控件树将被迫接受WMS的布局结果，以最新的窗口尺寸作为MeasureSpec参数进行测量。这一测量结果将是
  随后布局阶段中设置控件边界时的依据。参考这一阶段的代码:
```
private void performTraversals() {
 if (!mStopped || mReportNextDraw) {
   //窗口布局还会影响TouchMode的变化，将在6.5介绍
   boolean focusChangedDueToTouchMode = ensureTouchModeLocally(
                           (relayoutResult&WindowManagerGlobal.RELAYOUT_RES_IN_TOUCH_MODE) != 0);
    //1 进行最终测量的条件:TouchMode发生变化，最新窗口的尺寸不符合预期测量的结果,contentInset发生变化(导致Padding发生变化)
    //todo updatedConfiguration新增
    if (focusChangedDueToTouchMode || mWidth != host.getMeasuredWidth()
                             || mHeight != host.getMeasuredHeight() || contentInsetsChanged ||
                             updatedConfiguration) {
     //2 最终测量的参数为窗口的最新尺寸
     int childWidthMeasureSpec = getRootMeasureSpec(mWidth, lp.width);
     int childHeightMeasureSpec = getRootMeasureSpec(mHeight, lp.height);
     //3 再次测量
     performMeasure(childWidthMeasureSpec, childHeightMeasureSpec);
     ...
     }
 }
}
```
之所以最终测量使用performMeasure而不是measureHierarchy，是因为measureHierarchy0包含协商的算法，以期确定最佳的测量尺寸。
而在最终测量中，窗口尺寸已然确定下来，是没有协商余地的。
(8)总结
布局窗口与最终测量两个阶段至此便分析完毕。
布局窗口阶段得以进行的原因是控件系统有修改窗口属性的需求，
如第一次“遍历”需要确定窗口的尺寸以及一块Surface，预测量结果与窗口当前尺寸不一致需要进行窗口尺寸更改，
mView可见性发生变化需要将窗口隐藏或显示，LayoutParams发生变化需要WMS以新的参数进行重新布局。
而最终测量阶段得以进行的原因是窗口布局阶段确定的窗口尺寸与控件树的期望尺寸不一致，控件树需要对窗口尺寸进行妥协。

完成这两个阶段之后，performTraversals中剩余的变数已所剩无几，窗口的尺寸，控件树中控件的尺寸都已最终确定。
接下来便是控件树的布局与绘制了。



布局控件树阶段
经过前面的测量，控件树中的控件对于自己的尺寸显然已经了然于胸，而且父控件对于子控件的位置也有了眉目〈因为为子控件准备
MeasureSpec时有可能需要计算子控件的位置)。
布局阶段将会把测量结果付诸行动，即把测量结果转化为控件的实际位置与尺寸。控件的实际位置与尺寸由View的mLeft、mTop、mRight以及mBottom
4个成员变量存储的坐标值来表示。因此，控件树的布局过程就是根据测量结果为每一个控件设置这4个成员变量的过程。

必须时刻注意一个事实:mLeft、mTop、mRight以及mBottom这些坐标值是相对于其父控件的左上角的距离，也就是说这些坐标值是
以父控件左上角为坐标原点进行计算的。另一种说法是这些坐标位于父控件的坐标系中。倘若需要获取控件在窗口坐标系中的位置可以使用
View.getLocationInWindow(0)方法，相应，也可以通过View.getLocationOnScreen()方法获取控件在屏幕坐标系下的位置。
这两个方法的实现原理是一个沿着控件树向根部进行递归调用，其递归过程可以简单总结为控件在窗口中的位置等于在父窗口中的位置
加上父窗口在窗口中的位置。

```
private void performTraversals() {
  //todo mReportNextDraw
  //1 布局控件树阶段的条件是layoutRequested
  final boolean didLayout = layoutRequested && (!mStopped || mReportNextDraw);
  if (didLayout) {
   //2 通过performLayout进行控件树的布局
     performLayout(lp, mWidth, mHeight);
     //3 如果有必要计算窗口中的透明区域，并将此透明区域设置给WMS
     if ((host.mPrivateFlags & View.PFLAG_REQUEST_TRANSPARENT_REGIONS) != 0) {
        host.getLocationInWindow(mTmpLocation);
        //透明区域被初始化为整个mView的区域
        mTransparentRegion.set(mTmpLocation[0], mTmpLocation[1],
                mTmpLocation[0] + host.mRight - host.mLeft,
                mTmpLocation[1] + host.mBottom - host.mTop);
        //遍历控件树的每一个控件，倘若有内容需要绘制，则将其所在区域从 mTransparentRegion去除
        host.gatherTransparentRegion(mTransparentRegion);
        if (mTranslator != null) {
         //mTransparentRegion目前位于窗口坐标系中，将其映射到屏幕坐标系中
         //因为WMS管理窗口是在屏幕坐标系中进行
          mTranslator.translateRegionInWindowToScreen(mTransparentRegion);
        }
        //将透明区域设置到WMS
        if (!mTransparentRegion.equals(mPreviousTransparentRegion)) {
              mPreviousTransparentRegion.set(mTransparentRegion);
              mFullRedrawNeeded = true;
              try {
                  mWindowSession.setTransparentRegion(mWindow, mTransparentRegion);
              } catch (RemoteException e) {
              }
          }
     }
   }
}
```
布局控件树的条件并不像布局窗口那么严格。只要layoutRequested为tue，即调用过requestLayout()方法即可。
requestLayout()的用途在于当一个控件因为某种原因(如内容的尺才发生变化)而需要调整自身尺寸时，向ViewRootImpl申请进行
一次新的“遍历”以便使此控件得到一次新的测量布局与绘制。所以，只要requestLayout被调用则布局控件树阶段一定会执行。
而requestLayoutO是否引发布局窗口阶段则取决于前述的4个条件是否满足。
为什么当一个控件调整尺寸时需要通过requestLayout()使ViewRootImpl对整个控件树都做同样的事情呢?
从之前阶段的分析可知，一个控件的测量结果可能直接影响控件树上各个父控件的测量结果，甚至是窗口的布局。所以为了能够完整地
处理一个控件的变化所产生的影响，ViewRootImpl都会将整个过程从头来一遍。
这可能会引发对运行效率的担心，不过不用担心，requestLayout所调用的用于引发一次“遍历”的sheduleTraversals()会检查是否在
主线程上已经安排了一次“遍历”。因此，倘若在一段代码中一次性地调用10个TextView的setText()函数可能会导致requestLayoutO)被调用10次，
而scheduleTraversals的检查则会保证随后仅执行一次“遍历”。

布局控件树阶段主要做了两件事情:
进行控件树布局。
设置窗口的透明区域。


控件树布局  performLayout的实现
```
private void performLayout(WindowManager.LayoutParams lp, int desiredWindowWidth,
              int desiredWindowHeight) {
 final View host = mView;
 try {
 //调用View.Layout方法启动布局
               host.layout(0, 0, host.getMeasuredWidth(), host.getMeasuredHeight());
  }...
}
```

/frameworks/base/core/java/android/view/View.java
```
public void layout(int l, int t, int r, int b) {
    //保存原始坐标
   int oldL = mLeft;
    int oldT = mTop;
    int oldB = mBottom;
    int oldR = mRight;
    //1 setFrame 将坐标设置到mLeft,mTop,mRight,mBottom
    boolean changed = isLayoutModeOptical(mParent) ?
                      setOpticalFrame(l, t, r, b) : setFrame(l, t, r, b);
    /*是否还记得PFLRAG_LAYOUT_REQUIRED标记?它在View.measure()方法中被添加到mPrivateFlags。
    按照常理来说，当此控件的布局没有发生改变时是没有必要继续对子控件进行布局的，而这个标记则会
    将其放行，以保证真正需要布局的子控件得到布局*/
    if (changed || (mPrivateFlags & PFLAG_LAYOUT_REQUIRED) == PFLAG_LAYOUT_REQUIRED) {
      //2执行onLayout 如果是ViewGroup,需要依次调用子控件的layout方法
      onLayout(changed, l, t, r, b);
      //清除PFLAG_LAYOUT_REQUIRED标记
      mPrivateFlags &= ~PFLAG_LAYOUT_REQUIRED;
      //3 通知每一个对此控件的布局变化感兴趣的监听者
      ListenerInfo li = mListenerInfo;
        if (li != null && li.mOnLayoutChangeListeners != null) {
            ArrayList<OnLayoutChangeListener> listenersCopy =
                    (ArrayList<OnLayoutChangeListener>)li.mOnLayoutChangeListeners.clone();
            int numListeners = listenersCopy.size();
            for (int i = 0; i < numListeners; ++i) {
                listenersCopy.get(i).onLayoutChange(this, l, t, r, b, oldL, oldT, oldR, oldB);
            }
        }
    }
}
```

Layout()方法主要做了三件事情:
1 通过setFrame0设置布局的4个坐标。
2 调用onLayout()方法，使子类得到布局变更的通知。如果此类是一个ViewGroup，则需要在onLayout()方法中依次调用每一个子控件
  的layout()方法使其得到布局。切记不能调用子控件的onLayout()方法，这会导致子控件没有机会调用setFrame，从而使得此控件的
  坐标信息无法得到更新。
3 通知每一个对此控件的布局变化感兴趣的监听者。可以通过调用View.addOnLayoutChangeListener0加入对此控件的监听

-
注意
监听者的代码与控件树布局的核心带辑无关，仍然将其帖附在这里的原因是由于它的实现很值得开发者借鉴。注意到layout()方法必定
会在主线程中被performTraversals0)调用，而addOnLayoutChangeListener没有限定调用线程，却没有增加任何同步锁的保护。
那么如何保证两个方法对mListenerInfo访问的同步呢?
layout的策略是将mListenerInfo通过clone()做一份措贝，然后遍历这份拷贝，从而避免遍历过程中mListenerInfo发生变化而导致的越界。
ArrayList内部也采用类似的方法来保证线程安全。
另外，既然已经有onLayout方法监听布局的变化，为什么还需要监听者呢?
onLayout()有它的局限性，即只能在类内部访问，因此它更适合做类内部的监听与处理。而监听者则给予类外部的对象监听其内部状态变化的能力，二者并不重复。
-
//todo 线程安全 这个与copy on write的关系   copy on read ??  写多读少？？

对比测量与布局两个过程有助于加深对它们的理解。
1 测量确定的是控件的尺寸，并在一定程度上确定了子控件的位置。而布局则是针对测量结果来实施，并最终确定子控件的位置。
2测量结果对布局过程没有约束力。虽说子控件在onMeasure()方法中计算出了自己应有的尺寸，但是由于layout()方法是由父控件调用，
   因此控件的位置尺寸的最终决定权在父控件手中，测量结果仅仅是一个参考。
3 一般来说，子控件的测量结果影响父控件的测量结果，因此测量过程是后根遍历。而父控件的布局结果影响子控件的布局结果(例如位置)，
  所以布局过程是先根遍历。
完成performLayout()调用之后控件树的所有控件都已经确定了其最终位置，只等绘制了。

窗口透明区域   //todo 第4章WMS   performLayout之后的gatherTransparentRegion
布局阶段另一个工作是计算并设置窗口的透明区域，这一功能主要是为SurfaceView服务。设想一个视频播放器的窗口，
它包含一系列的控制按钮，位于主窗口上，而其进行视频内容泻染的SurfaceView所建立的子窗口则位于主窗口之下(参考第4章)。
为了保证负责显示视频的子窗口能够透过主窗口显示出来，Android引入了窗口透明区域的机制。
所谓的透明区域是指Surface上的一块特定区域，在SurfaceFlinger进行混层时，Surface上的这个块区域将会被忽略，
就好似在Surface上切下一个洞一般，如图6-9所示。
带有透明区域窗口与其他窗口的合成效果.png

当控件树中存在SurfaceView时，它会通过调用ViewParent.requestTransparentRegion()方法启用这一机制。这一方法的调用
会沿着控件树回溯到ViewRootImpl，并沿途将PFLAG_REQUEST_TRANSPARENTREGIONS标记加入父控件的mPrivateFlags字段中。
此标记会导致ViewRootImpl完成控件树的布局后将进行透明区域的计算与设置。
透明区域的计算由View.gatherTransparentRegion()方法完成。透明区域的计算采用了挖洞法，及默认整个窗口都是透明区域，
在gatherTransparentRegion遍历到一个控件时，如果这个此控件有内容需要绘制，则将其所在的区域从当前透明区域中删除，
就好似在纸上裁出一个洞一样。当遍历完成后，剩余的区域就是最终的透明区域。
这个透明区域将会被设置到WMS中，进而被WMS设置给SurfaceFlinger。SurfaceFlinger在进行Surface的混合时，本窗口的透明区域部分
将被忽略，从而用户能够透过这部分区域看到后面窗口(如SurfaceView的窗口)的内容。
-
注意
作为透明区域的主要服务对象，在gatherTransparentRegion()中SurfaceView与其他类型控件的做法正好相反。SurfaceView会将
其区域并到当前透明区域中。因此，先于SurfaceView被遍历的控件所在的区域有可能被SurfaceView所设置的透明区域履盖，
此时这些控件被覆盖的区域将不会被SurfaceFlinger泻染。读者可以通过对比View类与SurfaceView类的gatherTransparentRegion()
方法实现的差异加深对透明区域的理解。
-
//todo  gatherTransparentRegion  view和surface的



绘制阶段
 经历前面4个阶段，每一个控件都已经确定好了自己的尺寸与位置，接下来就是最终的绘制阶段
 ```
 private void performTraversals() {
     //todo 老版本的这还有取消window动画 现在去哪了
     mFirst = false;
     mWillDrawSoon = false;
     mNewSurfaceNeeded = false;
     mActivityRelaunched = false;
     mViewVisibility = viewVisibility;
     mHadWindowFocus = hasWindowFocus;
     //确定是否需要向WMS发送绘侧完成的通知。第4章介绍过窗口的绘制状态，当窗口初次获得Surface时其绘制状态被设置为
     DRAW_PENDING，仅当WMS接收到窗口的finishDrawingWindow()回调时，才会使窗口迁移到COMMIT_DRRW_PENDING，
     进而迁移到READY_TO_SHOW。如果没有调用WMS.finishDrawingWindow()，即便在Surface上绘制了内容，WMS也会因为
     窗口的绘制状态不为READY_TO_SHOW而不会将窗口显示出来。
     mReportNextDraw是ViewRootImp1用来确定是否需要向WMS发起finishDrawingWindow()回调的条件。在这里，窗口初次获得
     了一块Surface，此时窗口绘制状态必然为DRAW_PENDING，因此将nReportNextDraw设置为true*/
     if ((relayoutResult & WindowManagerGlobal.RELAYOUT_RES_FIRST_TIME) != 0) {
                   reportNextDraw()
      }
     //当mview不可见时，也不需要进行绘制
       boolean cancelDraw = mAttachInfo.mTreeObserver.dispatchOnPreDraw() || !isViewVisible;
       if (!cancelDraw && !newSurface) {
         //performDraw负责整个控件树的绘制
         performDraw();
       } else {
       // 如果窗口在此次遍历中获取了surface，则跳过本次遍历的绘制，并且通过scheduleTraversals方法重新做一次遍历，
       //并在新的遍历中完成绘制
         if (isViewVisible) {
             scheduleTraversals();
       }
 }
 ```
 可见，同其他阶段一样，绘制也是有可能被跳过的。下面介绍跳过绘制的原因。
 1 skipDraw:当窗口处于动画状态时，skipDraw会被置tue使得跳过绘制。在Android
 看来，用户很容易注意到窗口动画的平滑性，因此它跳过了窗口的绘制使得更多
 的CPU/GPU资源用来处理动画，在这个过程中窗口的内容是被冻结的。另外需要
 注意到，skipDraw的设置会因为mReportNextDraw而失效。上面的代码分析中介
 绍mReportNextDraw的作用是为了在窗口是DRAW_PENDING状态时向WMS发起
 finishDrawingWindow()回调。因此mReportNextDraw为true时窗口的Surface尚未被
 显示出来并且没有任何内容。倘若此时不进行绘制工作会导致窗口迟迟不能迁移到
 COMMIT_DRAW_PENDING状态进而被显示出来，那么窗口动画也就无从谈起了。  //todo skipDraw新版取消了
 2 cancelDraw:当mView不可见时，自然也不需要进行绘制。
 3 newSurface:newSurface表明窗口在本次“遍历”中获取了一块Surface(可能由于这
 是第一次“遍历”或者mView从不可见变为可见)。在这种情况下，ViewRootImpl选
 择通过调用scheduleTraversals在下次“遍历”中进行绘制，而不是在本次进行绘制。
 可见，绘制阶段的限制条件相对于前4个阶段来说要宽松得多，在常态下只要per-
 formTraversals()被调用，则一定会执行绘制阶段。
 performDraw()方法就是控件树的绘制人口。由于控件树的绘制十分复杂，因此，perform-
 Draw0方法的工作原理将在6.4节中单独介绍。

performTraversals 方法总结
performTraversals_5个工作阶段的工作流程.png
可见，前4个阶段以layoutRequested为执行条件，即在“遍历”之前调用了request-
Layout()方法。这是由于前4个阶段的主要工作目的是确定控件的位置与尺寸。因此，仅
当一个或多个控件有改变位置或尺寸的需求时(此时会调用requestLayoutO)才有执行的
必要。

即使requestLayout未被调用过，绘制阶段也会被执行。因为很多时候需要在不改
变控件位置与尺寸下进行重绘，例如某个控件改变了其文字颜色或背景色的情况下。与
requestLayout()方法相对，这种情况发生在控件调用了invalidate()方法的时候。
··
注意
即便requestLayout()没有被调用过，有可能由于LayoutParams，mView的可见性等与
窗口有关的属性发生变化时,“遍历”流程仍会进入第二阶段。由于这些属性与布局
没有直接联系，因此图6-10并没有体现这一点。
··
