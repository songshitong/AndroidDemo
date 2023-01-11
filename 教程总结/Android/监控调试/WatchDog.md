Android系统中，有硬件WatchDog用于定时检测关键硬件是否正常工作，类似地，在framework层有一个软件WatchDog用于定期检测关键系统服务是否发生死锁事件。
WatchDog功能主要是分析系统核心服务和重要线程是否处于Blocked状态。
 监视reboot广播；
 监视mMonitors关键系统服务是否死锁

http://gityuan.com/2016/06/21/watchdog/
使用
http://aospxref.com/android-13.0.0_r3/xref/frameworks/base/services/java/com/android/server/SystemServer.java#1058
```
private void startBootstrapServices(@NonNull TimingsTraceAndSlog t) {
   ...
   final Watchdog watchdog = Watchdog.getInstance();
          watchdog.start();
          mDumper.addDumpable(watchdog);
  ...
  watchdog.init(mSystemContext, mActivityManagerService);        
}
```

http://aospxref.com/android-13.0.0_r3/xref/frameworks/base/services/core/java/com/android/server/Watchdog.java#mThread
```
public class Watchdog implements Dumpable {
 private final Thread mThread;
 private final ArrayList<HandlerCheckerAndTimeout> mHandlerCheckers = new ArrayList<>();
 private final HandlerChecker mMonitorChecker;
 
      private Watchdog() {
         mThread = new Thread(this::run, "watchdog");
         //将前台线程加入队列
         mMonitorChecker = new HandlerChecker(FgThread.getHandler(),
                 "foreground thread");
         mHandlerCheckers.add(withDefaultTimeout(mMonitorChecker));  
         //将主线程加入队列    
         mHandlerCheckers.add(withDefaultTimeout(
                 new HandlerChecker(new Handler(Looper.getMainLooper()), "main thread")));
         //将ui线程加入队列        
         mHandlerCheckers.add(withDefaultTimeout(
                 new HandlerChecker(UiThread.getHandler(), "ui thread")));
         //将i/o线程加入队列        
         mHandlerCheckers.add(withDefaultTimeout(
                 new HandlerChecker(IoThread.getHandler(), "i/o thread")));
         //将display线程加入队列        
         mHandlerCheckers.add(withDefaultTimeout(
                 new HandlerChecker(DisplayThread.getHandler(), "display thread")));
         //将动画线程加入队列        
         mHandlerCheckers.add(withDefaultTimeout(
                  new HandlerChecker(AnimationThread.getHandler(), "animation thread")));
         //将surface动画线程加入队列         
         mHandlerCheckers.add(withDefaultTimeout(
                 new HandlerChecker(SurfaceAnimationThread.getHandler(),
                     "surface animation thread")));
         addMonitor(new BinderThreadMonitor());
 
         mInterestingJavaPids.add(Process.myPid());
         ... 
         mTraceErrorLogger = new TraceErrorLogger();
     }
}
```
//todo 各种handler thread

HandlerChecker 实现runnable方法
```
public final class HandlerChecker implements Runnable {
    private final Handler mHandler; //Handler对象
    private final String mName; //线程描述名
    private final long mWaitMax; //最长等待时间
    //记录着监控的服务
    private final ArrayList<Monitor> mMonitors = new ArrayList<Monitor>();
    private final ArrayList<Monitor> mMonitorQueue = new ArrayList<Monitor>();
    private boolean mCompleted; //开始检查时先设置成false
    private Monitor mCurrentMonitor;
    private long mStartTime; //开始准备检查的时间点

    HandlerChecker(Handler handler, String name, long waitMaxMillis) {
        mHandler = handler;
        mName = name;
        mWaitMax = waitMaxMillis;
        mCompleted = true;
    }
}
```

addMonitor
```
 public void addMonitor(Monitor monitor) {
          synchronized (mLock) {
              //添加到foreground thread
              mMonitorChecker.addMonitorLocked(monitor);
          }
      }
 
public final class HandlerChecker implements Runnable {
     void addMonitorLocked(Monitor monitor) {
             mMonitorQueue.add(monitor);
         }
}      
      
      public interface Monitor {
          void monitor();
      }
      
    private static final class BinderThreadMonitor implements Watchdog.Monitor {
          @Override
          public void monitor() {
              Binder.blockUntilThreadAvailable();
          }
      }
  
http://aospxref.com/android-13.0.0_r3/xref/frameworks/native/libs/binder/IPCThreadState.cpp#526
 void IPCThreadState::blockUntilThreadAvailable()
  {
      pthread_mutex_lock(&mProcess->mThreadCountLock);
      mProcess->mWaitingForThreads++;
      while (mProcess->mExecutingThreadsCount >= mProcess->mMaxThreads) {
        //等待正在执行的binder线程小于进程最大binder线程上限(16个)
          ALOGW("Waiting for thread to be free. mExecutingThreadsCount=%lu mMaxThreads=%lu\n",
                  static_cast<unsigned long>(mProcess->mExecutingThreadsCount),
                  static_cast<unsigned long>(mProcess->mMaxThreads));
          pthread_cond_wait(&mProcess->mThreadCountDecrement, &mProcess->mThreadCountLock);
      }
      mProcess->mWaitingForThreads--;
      pthread_mutex_unlock(&mProcess->mThreadCountLock);
  }  
```
监控Binder线程, 将monitor添加到HandlerChecker的成员变量mMonitors列表中。 在这里是将BinderThreadMonitor对象加入该线程。
blockUntilThreadAvailable最终调用的是IPCThreadState，等待有空闲的binder线程
可见addMonitor(new BinderThreadMonitor())是将Binder线程添加到android.fg线程的handler(mMonitorChecker)来检查是否工作正常。


init  //注册reboot广播接收者
```
  public void init(Context context, ActivityManagerService activity) {
          mActivity = activity;
          context.registerReceiver(new RebootRequestReceiver(),
                  new IntentFilter(Intent.ACTION_REBOOT),
                  android.Manifest.permission.REBOOT, null);
      }
 
 final class RebootRequestReceiver extends BroadcastReceiver {
         @Override
         public void onReceive(Context c, Intent intent) {
             if (intent.getIntExtra("nowait", 0) != 0) {
                 rebootSystem("Received ACTION_REBOOT broadcast");
                 return;
             }
             Slog.w(TAG, "Unsupported ACTION_REBOOT broadcast: " + intent);
         }
     }
     
    void rebootSystem(String reason) {
          Slog.i(TAG, "Rebooting system because: " + reason);
          IPowerManager pms = (IPowerManager)ServiceManager.getService(Context.POWER_SERVICE);
          try {
             //通过PowerManager执行reboot操作
              pms.reboot(false, reason, false);
          } catch (RemoteException ex) {
          }
      }         
```


Watchdog检测机制
当调用Watchdog.getInstance().start()时，则进入线程“watchdog”的run()方法, 该方法分成两部分:
前半部用于监测是否触发超时;
后半部当触发超时则输出各种信息。