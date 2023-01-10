

总结
Watchdog是一个运行在system_server进程的名为”watchdog”的线程:：

Watchdog运作过程，当阻塞时间超过1分钟则触发一次watchdog，会杀死system_server,触发上层重启；
mHandlerCheckers记录所有的HandlerChecker对象的列表，包括foreground, main, ui, i/o, display线程的handler;
mHandlerChecker.mMonitors记录所有Watchdog目前正在监控Monitor，所有的这些monitors都运行在foreground线程。
有两种方式加入Watchdog监控：
addThread()：用于监测Handler线程，默认超时时长为60s.这种超时往往是所对应的handler线程消息处理得慢；
addMonitor(): 用于监控实现了Watchdog.Monitor接口的服务.这种超时可能是”android.fg”线程消息处理得慢，也可能是monitor迟迟拿不到锁；
以下情况,即使触发了Watchdog,也不会杀掉system_server进程:

monkey: 设置IActivityController,拦截systemNotResponding事件, 比如monkey.
hang: 执行am hang命令,不重启;
debugger: 连接debugger的情况, 不重启;


常见的线程
```
public class ServiceThread extends HandlerThread {
    private static final String TAG = "ServiceThread";
    private final boolean mAllowIo;
    public ServiceThread(String name, int priority, boolean allowIo) {
        super(name, priority);
        mAllowIo = allowIo;
    }

    @Override
    public void run() {
        Process.setCanSelfBackground(false);
        if (!mAllowIo) {
            //不允许io
            StrictMode.initThreadDefaults(null);
        }
        super.run();
    }
}
```
前台线程android.fg
frameworks/base/services/core/java/com/android/server/FgThread.java
```
   private FgThread() {
        super("android.fg", android.os.Process.THREAD_PRIORITY_DEFAULT, true /*allowIo*/);
    }
```
应用在AdbService.java
ThermalManagerService 热缓解服务(拿到手机温度等https://blog.csdn.net/feelabclihu/article/details/107873407)

ui线程android.ui 用来展示Ui
frameworks/base/services/core/java/com/android/server/UiThread.java
```
    private UiThread() {
        super("android.ui", Process.THREAD_PRIORITY_FOREGROUND, false /*allowIo*/);
    }
```
frameworks/base/services/core/java/com/android/server/clipboard/ClipboardService.java
```
 private void showAccessNotificationLocked(String callingPackage, int uid, @UserIdInt int userId,
            PerUserClipboard clipboard) {
    Toast toastToShow;
    if (SafetyProtectionUtils.shouldShowSafetyProtectionResources(getContext())) {
       ....
        toastToShow = Toast.makeCustomToastWithIcon(getContext(),
                UiThread.get().getLooper(), message,
                Toast.LENGTH_SHORT, safetyProtectionIcon);
    } else {
        toastToShow = Toast.makeText(
                getContext(), UiThread.get().getLooper(), message,
                Toast.LENGTH_SHORT);
    }
    toastToShow.show();               
 }
```

IoThread android.io  非后台服务执行网络io
frameworks/base/services/core/java/com/android/server/IoThread.java
```
public class HandlerExecutor implements Executor {
    private final Handler mHandler;
    public HandlerExecutor(@NonNull Handler handler) {
        mHandler = Preconditions.checkNotNull(handler);
    }
    @Override
    public void execute(Runnable command) {
        if (!mHandler.post(command)) {
            throw new RejectedExecutionException(mHandler + " is shutting down");
        }
    }
}
private static HandlerExecutor sHandlerExecutor;
  private IoThread() {
        super("android.io", android.os.Process.THREAD_PRIORITY_DEFAULT, true /*allowIo*/);
    }
```
示例
frameworks/base/services/core/java/com/android/server/notification/NotificationManagerService.java
```
 protected void handleSavePolicyFile() {
        if (!IoThread.getHandler().hasCallbacks(mSavePolicyFile)) {
            IoThread.getHandler().postDelayed(mSavePolicyFile, 250);
        }
    }

    private final class SavePolicyFileRunnable implements Runnable {
        @Override
        public void run() {
            synchronized (mPolicyFile) {
                final FileOutputStream stream;
                try {
                    stream = mPolicyFile.startWrite();
                } catch (IOException e) {
                   ...
                }
                try {
                    writePolicyXml(stream, false /*forBackup*/, UserHandle.USER_ALL);
                    mPolicyFile.finishWrite(stream);
                } catch (IOException e) {
                   ...
                }
            }
            BackupManager.dataChanged(getContext().getPackageName());
        }
    }
```

DisplayThread android.display  需要一个最短的延迟    WindowManager,DisplayManager, and InputManager
frameworks/base/services/core/java/com/android/server/DisplayThread.java
```
 private DisplayThread() {
        super("android.display", Process.THREAD_PRIORITY_DISPLAY + 1, false /*allowIo*/);
    }
```
frameworks/base/services/core/java/com/android/server/wm/WindowManagerService.java
```
    public static WindowManagerService main(final Context context, final InputManagerService im,
            final boolean showBootMsgs, final boolean onlyCore, WindowManagerPolicy policy,
            ActivityTaskManagerService atm, DisplayWindowSettingsProvider
            displayWindowSettingsProvider, Supplier<SurfaceControl.Transaction> transactionFactory,
            Function<SurfaceSession, SurfaceControl.Builder> surfaceControlFactory) {
        final WindowManagerService[] wms = new WindowManagerService[1];
        DisplayThread.getHandler().runWithScissors(() ->
                wms[0] = new WindowManagerService(context, im, showBootMsgs, onlyCore, policy,
                        atm, displayWindowSettingsProvider, transactionFactory,
                        surfaceControlFactory), 0);
        return wms[0];
    }
```

用于window animations
frameworks/base/services/core/java/com/android/server/AnimationThread.java
```
  private AnimationThread() {
        super("android.anim", THREAD_PRIORITY_DISPLAY, false /*allowIo*/);
    }
```


frameworks/base/services/core/java/com/android/server/Watchdog.java
```
private static final boolean DB = false;
private static final long DEFAULT_TIMEOUT = DB ? 10 * 1000 : 60 * 1000;
private volatile long mWatchdogTimeoutMillis = DEFAULT_TIMEOUT; //默认超时为60S

private void run() {
        boolean waitedHalf = false;

        while (true) {
            List<HandlerChecker> blockedCheckers = Collections.emptyList();
            String subject = "";
            boolean allowRestart = true;
            int debuggerWasConnected = 0;
            boolean doWaitedHalfDump = false;
            final long watchdogTimeoutMillis = mWatchdogTimeoutMillis; //默认30s
            final long checkIntervalMillis = watchdogTimeoutMillis / 2;
            final ArrayList<Integer> pids;
            synchronized (mLock) {
                long timeout = checkIntervalMillis;         
                for (int i=0; i<mHandlerCheckers.size(); i++) {
                    HandlerCheckerAndTimeout hc = mHandlerCheckers.get(i);
                    //checker()获取HandlerChecker        
                   //执行所有的Checker的监控方法, 每个Checker记录当前的mStartTime          
                    hc.checker().scheduleCheckLocked(hc.customTimeoutMillis()
                            .orElse(watchdogTimeoutMillis * Build.HW_TIMEOUT_MULTIPLIER));
                }

                if (debuggerWasConnected > 0) {
                    debuggerWasConnected--;
                }
                //通过循环,保证执行30s才会继续往下执行
                long start = SystemClock.uptimeMillis();
                while (timeout > 0) {
                    if (Debug.isDebuggerConnected()) {
                        debuggerWasConnected = 2;
                    }
                    try {
                        mLock.wait(timeout); //等待30s
                    } catch (InterruptedException e) {
                        Log.wtf(TAG, e);
                    }
                    if (Debug.isDebuggerConnected()) {
                        debuggerWasConnected = 2;
                    }
                    timeout = checkIntervalMillis - (SystemClock.uptimeMillis() - start); //过去了timeout的时间
                }
                //评估Checker状态
                final int waitState = evaluateCheckerCompletionLocked();
                if (waitState == COMPLETED) {
                    waitedHalf = false;
                    continue;
                } else if (waitState == WAITING) {
                    continue;
                } else if (waitState == WAITED_HALF) {
                    if (!waitedHalf) {
                       //首次进入等待时间过半的状态
                        Slog.i(TAG, "WAITED_HALF");
                        waitedHalf = true;
                        //查询所有的WAITED_HALF的checker
                        blockedCheckers = getCheckersWithStateLocked(WAITED_HALF);
                        subject = describeCheckersLocked(blockedCheckers);
                        pids = new ArrayList<>(mInterestingJavaPids);
                        doWaitedHalfDump = true; //输出system_server(示例中的当前进程mInterestingJavaPids.add(Process.myPid())和17个native进程的traces
                    } else {
                        continue;
                    }
                } else {
                    //查询所有的OVERDUE的checker
                    blockedCheckers = getCheckersWithStateLocked(OVERDUE);
                    //输出checker的信息
                    subject = describeCheckersLocked(blockedCheckers);
                    allowRestart = mAllowRestart;
                    pids = new ArrayList<>(mInterestingJavaPids);
                }
            } // END synchronized (mLock)

            //进入这里，意味着Watchdog已超时
            logWatchog(doWaitedHalfDump, subject, pids);

            if (doWaitedHalfDump) {
                continue;
            }

           ....
            //当debugger没有attach时，才杀死进程
            if (Debug.isDebuggerConnected()) {
                debuggerWasConnected = 2;
            }
            if (debuggerWasConnected >= 2) {
                Slog.w(TAG, "Debugger connected: Watchdog is *not* killing the system process");
            } else if (debuggerWasConnected > 0) {
                Slog.w(TAG, "Debugger was connected: Watchdog is *not* killing the system process");
            } else if (!allowRestart) {
                Slog.w(TAG, "Restart not allowed: Watchdog is *not* killing the system process");
            } else {
                Slog.w(TAG, "*** WATCHDOG KILLING SYSTEM PROCESS: " + subject);
                WatchdogDiagnostics.diagnoseCheckers(blockedCheckers);
                Slog.w(TAG, "*** GOODBYE!");
                if (!Build.IS_USER && isCrashLoopFound()
                        && !WatchdogProperties.should_ignore_fatal_count().orElse(false)) {
                    breakCrashLoop();
                }
                //杀死进程system_server
                Process.killProcess(Process.myPid());
                System.exit(10);
            }

            waitedHalf = false;
        }
    }
 
  //获取符合completionState的列表
   private ArrayList<HandlerChecker> getCheckersWithStateLocked(int completionState) {
        ArrayList<HandlerChecker> checkers = new ArrayList<HandlerChecker>();
        for (int i=0; i<mHandlerCheckers.size(); i++) {
            HandlerChecker hc = mHandlerCheckers.get(i).checker();
            if (hc.getCompletionStateLocked() == completionState) {
                checkers.add(hc);
            }
        }
        return checkers;
    } 
    
     private String describeCheckersLocked(List<HandlerChecker> checkers) {
        StringBuilder builder = new StringBuilder(128);
        for (int i=0; i<checkers.size(); i++) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(checkers.get(i).describeBlockedStateLocked());//添加handler或monitor的名字
        }
        return builder.toString();
    }    
    
   //当输出的信息是Blocked in handler,意味着相应的线程处理当前消息时间超过1分钟;
   //当输出的信息是Blocked in monitor,意味着相应的线程处理当前消息时间超过1分钟,或者monitor迟迟拿不到锁; 
   String describeBlockedStateLocked() {
            if (mCurrentMonitor == null) {//非前台线程进入该分支
                return "Blocked in handler on " + mName + " (" + getThread().getName() + ")";
            } else {//前台线程进入该分支
                return "Blocked in monitor " + mCurrentMonitor.getClass().getName()
                        + " on " + mName + " (" + getThread().getName() + ")";
            }
        }  


```
该方法主要功能:
1 执行所有的Checker的监控方法scheduleCheckLocked()
  当mMonitor个数为0(除了android.fg线程之外都为0)且处于poll状态,则设置mCompleted = true;
  当上次check还没有完成, 则直接返回.
2 等待30s后, 再调用evaluateCheckerCompletionLocked来评估Checker状态;
3 根据waitState状态来执行不同的操作:
  当COMPLETED或WAITING,则相安无事;
  当WAITED_HALF(超过30s)且为首次, 则输出system_server和Native进程的traces;
  当OVERDUE, 则输出更多信息.
由此,可见当触发一次Watchdog, 则必然会调用两次AMS.dumpStackTraces, 也就是说system_server和Native进程的traces 的traces信息会输出两遍,且时间间隔超过30s.


scheduleCheckLocked
```
public final class HandlerChecker implements Runnable {
   public void scheduleCheckLocked(long handlerCheckerTimeoutMillis) {
            mWaitMax = handlerCheckerTimeoutMillis;
            if (mCompleted) {
                mMonitors.addAll(mMonitorQueue);
                mMonitorQueue.clear();
            }
            if ((mMonitors.size() == 0 && mHandler.getLooper().getQueue().isPolling())
                    || (mPauseCount > 0)) {   
               //当目标looper正在轮询状态则返回            
                mCompleted = true;
                return;
            }
            //有一个check正在处理中，则无需重复发送
            if (!mCompleted) {
                return;
            }
            mCompleted = false;
            mCurrentMonitor = null;
            // 记录当下的时间
            mStartTime = SystemClock.uptimeMillis();
            //发送消息，插入消息队列最开头， 见下方的run()方法
            mHandler.postAtFrontOfQueue(this);
        }
        
        
     public void run() {
            final int size = mMonitors.size();
            for (int i = 0 ; i < size ; i++) {
                synchronized (mLock) {
                    mCurrentMonitor = mMonitors.get(i);
                }
                //回调具体服务的monitor方法
                mCurrentMonitor.monitor();
            }

            synchronized (mLock) {
                mCompleted = true;
                mCurrentMonitor = null;
            }
        }     
}
```
服务实现monitor方法示例
frameworks/base/services/core/java/com/android/server/input/InputManagerService.java
```
   //执行monitor()时测试可以获得锁，防止死锁，如果不能获得锁，就会超时
    @Override
    public void monitor() {
        synchronized (mInputFilterLock) { }
        synchronized (mAssociationsLock) { /* Test if blocked by associations lock. */}
        synchronized (mLidSwitchLock) { /* Test if blocked by lid switch lock. */ }
        synchronized (mInputMonitors) { /* Test if blocked by input monitor lock. */ }
        synchronized (mAdditionalDisplayInputPropertiesLock) { /* Test if blocked by props lock */ }
        mNative.monitor();
    }
```

该方法主要功能: 向Watchdog的监控线程的Looper池的最头部执行该HandlerChecker.run()方法, 在该方法中调用monitor(),
执行完成后会设置mCompleted = true. 那么当handler消息池当前的消息, 导致迟迟没有机会执行monitor()方法, 则会触发watchdog.

其中postAtFrontOfQueue(this)，该方法输入参数为Runnable对象，根据消息机制， 最终会回调HandlerChecker中的run方法，
该方法会循环遍历所有的Monitor接口，具体的服务实现该接口的monitor()方法。

可能的问题,如果有其他消息不断地调用postAtFrontOfQueue()也可能导致watchdog没有机会执行;或者是每个monitor消耗一些时间,
累加起来超过1分钟造成的watchdog. 这些都是非常规的Watchdog.



evaluateCheckerCompletionLocked  获取mHandlerCheckers列表中等待状态值最大的state.
```
    private int evaluateCheckerCompletionLocked() {
        int state = COMPLETED;
        for (int i=0; i<mHandlerCheckers.size(); i++) {
            HandlerChecker hc = mHandlerCheckers.get(i).checker();
            state = Math.max(state, hc.getCompletionStateLocked());
        }
        return state;
    }
   
  private static final int COMPLETED = 0;
  private static final int WAITING = 1;
  private static final int WAITED_HALF = 2;
  private static final int OVERDUE = 3;   
  
 class HandlerChecker{
     public int getCompletionStateLocked() {
            if (mCompleted) {
                return COMPLETED;
            } else {
                long latency = SystemClock.uptimeMillis() - mStartTime;
                if (latency < mWaitMax/2) {
                    return WAITING;
                } else if (latency < mWaitMax) {
                    return WAITED_HALF;
                }
            }
            return OVERDUE;
        }
 }   
```

logWatchog
```
 private void logWatchog(boolean halfWatchdog, String subject, ArrayList<Integer> pids) {
        // Get critical event log before logging the half watchdog so that it doesn't
        // occur in the log.
        String criticalEvents =
                CriticalEventLog.getInstance().logLinesForSystemServerTraceFile();
        final UUID errorId = mTraceErrorLogger.generateErrorId();
        if (mTraceErrorLogger.isAddErrorIdEnabled()) {
            mTraceErrorLogger.addErrorIdToTrace("system_server", errorId);
            mTraceErrorLogger.addSubjectToTrace(subject, errorId);
        }

        final String dropboxTag;
        if (halfWatchdog) {
            dropboxTag = "pre_watchdog";
            CriticalEventLog.getInstance().logHalfWatchdog(subject);
        } else {
            dropboxTag = "watchdog";
            CriticalEventLog.getInstance().logWatchdog(subject, errorId);
            EventLog.writeEvent(EventLogTags.WATCHDOG, subject);
            // Log the atom as early as possible since it is used as a mechanism to trigger
            // Perfetto. Ideally, the Perfetto trace capture should happen as close to the
            // point in time when the Watchdog happens as possible.
            FrameworkStatsLog.write(FrameworkStatsLog.SYSTEM_SERVER_WATCHDOG_OCCURRED, subject);
        }

        long anrTime = SystemClock.uptimeMillis();
        StringBuilder report = new StringBuilder();
        report.append(MemoryPressureUtil.currentPsiState());
        ProcessCpuTracker processCpuTracker = new ProcessCpuTracker(false);
        StringWriter tracesFileException = new StringWriter();
        final File stack = ActivityManagerService.dumpStackTraces(
                pids, processCpuTracker, new SparseArray<>(), getInterestingNativePids(),
                tracesFileException, subject, criticalEvents);
        // Give some extra time to make sure the stack traces get written.
        // The system's been hanging for a whlie, another second or two won't hurt much.
        SystemClock.sleep(5000);
        processCpuTracker.update();
        report.append(processCpuTracker.printCurrentState(anrTime));
        report.append(tracesFileException.getBuffer());

        if (!halfWatchdog) {
            // Trigger the kernel to dump all blocked threads, and backtraces on all CPUs to the
            // kernel log
            doSysRq('w');
            doSysRq('l');
        }

        //输出dropbox信息
        // Try to add the error to the dropbox, but assuming that the ActivityManager
        // itself may be deadlocked.  (which has happened, causing this statement to
        // deadlock and the watchdog as a whole to be ineffective)
        Thread dropboxThread = new Thread("watchdogWriteToDropbox") {
                public void run() {
                    // If a watched thread hangs before init() is called, we don't have a
                    // valid mActivity. So we can't log the error to dropbox.
                    if (mActivity != null) {
                        mActivity.addErrorToDropBox(
                                dropboxTag, null, "system_server", null, null, null,
                                null, report.toString(), stack, null, null, null,
                                errorId);
                    }
                }
            };
        dropboxThread.start();
        try {
            dropboxThread.join(2000);  // wait up to 2 seconds for it to return.
        } catch (InterruptedException ignored) { }
    }
    
    
//获取NATIVE_STACKS_OF_INTEREST的进程id列表
 static ArrayList<Integer> getInterestingNativePids() {
        HashSet<Integer> pids = new HashSet<>();
        addInterestingAidlPids(pids);
        addInterestingHidlPids(pids);

        int[] nativePids = Process.getPidsForCommands(NATIVE_STACKS_OF_INTEREST);
        if (nativePids != null) {
            for (int i : nativePids) {
                pids.add(i);
            }
        }

        return new ArrayList<Integer>(pids);
    }

  public static final String[] NATIVE_STACKS_OF_INTEREST = new String[] {
        "/system/bin/audioserver",
        "/system/bin/cameraserver",
        "/system/bin/drmserver",
        "/system/bin/keystore2",
        "/system/bin/mediadrmserver",
        "/system/bin/mediaserver",
        "/system/bin/netd",
        "/system/bin/sdcard",
        "/system/bin/surfaceflinger",
        "/system/bin/vold",
        "media.extractor", // system/bin/mediaextractor
        "media.metrics", // system/bin/mediametrics
        "media.codec", // vendor/bin/hw/android.hardware.media.omx@1.0-service
        "media.swcodec", // /apex/com.android.media.swcodec/bin/mediaswcodec
        "media.transcoding", // Media transcoding service
        "com.android.bluetooth",  // Bluetooth service
        "/apex/com.android.os.statsd/bin/statsd",  // Stats daemon
    };    
```


CriticalEventLog.getInstance().logWatchdog
将Watchdog写入/data/misc/critical-events


AMS.dumpStackTraces
https://cs.android.com/android/platform/superproject/+/android-13.0.0_r1:out/soong/.intermediates/frameworks/base/services/core/services.core.unboosted/android_common/xref32/srcjars.xref/frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java;l=3445;drc=d3cf93d7c01809c12525f096ef1e1e840267b33a
```
static File dumpStackTraces(ArrayList<Integer> firstPids,
            ProcessCpuTracker processCpuTracker, SparseArray<Boolean> lastPids,
            ArrayList<Integer> nativePids, StringWriter logExceptionCreatingFile,
            long[] firstPidOffsets, String subject, String criticalEventSection) {
        ArrayList<Integer> extraPids = null;
        ....
        File tracesFile; // 路径为data/anr/anr_yyyy-MM-dd-HH-mm-ss-SSS
        try {
            tracesFile = createAnrDumpFile(tracesDir);
        } catch (IOException e) {
           ....
        }

        if (subject != null || criticalEventSection != null) {
            try (FileOutputStream fos = new FileOutputStream(tracesFile, true)) {
                if (subject != null) {
                    String header = "Subject: " + subject + "\n\n";
                    fos.write(header.getBytes(StandardCharsets.UTF_8));
                }
                if (criticalEventSection != null) {
                    fos.write(criticalEventSection.getBytes(StandardCharsets.UTF_8));
                }
            } catch (IOException e) {
                Slog.w(TAG, "Exception writing to ANR dump file:", e);
            }
        }
        //输出traces信息
        Pair<Long, Long> offsets = dumpStackTraces(
                tracesFile.getAbsolutePath(), firstPids, nativePids, extraPids);
        ...
        return tracesFile;
    }
```
输出system_server和native进程的traces信息

doSysRq   //todo
```
 private void doSysRq(char c) {
        try {
            FileWriter sysrq_trigger = new FileWriter("/proc/sysrq-trigger");
            sysrq_trigger.write(c);
            sysrq_trigger.close();
        } catch (IOException e) {
            Slog.w(TAG, "Failed to write to /proc/sysrq-trigger", e);
        }
    }    
```

通过向节点/proc/sysrq-trigger写入字符，触发kernel来dump所有阻塞线程，输出所有CPU的backtrace到kernel log

mActivity.addErrorToDropBox
//todo dropbox http://gityuan.com/2016/06/12/DropBoxManagerService/


killProcess  //todo http://gityuan.com/2016/04/16/kill-signal/
当杀死system_server进程，从而导致zygote进程自杀，进而触发init执行重启Zygote进程，这便出现了手机framework重启的现象


监控Handler线程
Watchdog监控的线程有：默认地DEFAULT_TIMEOUT=60s，调试时才为10s方便找出潜在的ANR问题。
线程名	      对应handler	                        说明	          Timeout
main	      new Handler(Looper.getMainLooper())	当前主线程	1min
android.fg	  FgThread.getHandler	                 前台线程	1min
android.ui	  UiThread.getHandler	UI线程	1min
android.io	   IoThread.getHandler	I/O线程	1min
android.display	DisplayThread.getHandler	display线程	1min
ActivityManager	AMS.MainHandler	AMS线程	1min
PowerManagerService	PMS.PowerManagerHandler	PMS线程	1min
PackageManager	PKMS.PackageHandler	PKMS线程	10min


监控同步锁
能够被Watchdog监控的系统服务都实现了Watchdog.Monitor接口，并实现其中的monitor()方法。运行在android.fg线程, 系统中实现该接口类主要有：
ActivityManagerService
WindowManagerService
InputManagerService
PowerManagerService
NetworkManagementService
MountService
NativeDaemonConnector
BinderThreadMonitor
MediaProjectionManagerService
MediaRouterService
MediaSessionService
BinderThreadMonitor