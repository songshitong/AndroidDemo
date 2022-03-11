

ANR信息收集   android8.0
http://gityuan.com/2016/12/02/app-not-response/
一. ANR场景
无论是四大组件或者进程等只要发生ANR，最终都会调用AMS.mAppErrors.appNotResponding()方法，下面从这个方法说起。
以下场景都会触发调用AMS.mAppErrors.appNotResponding方法:
Service Timeout:比如前台服务在20s内未执行完成；
BroadcastQueue Timeout：比如前台广播在10s内未执行完成
InputDispatching Timeout: 输入事件分发超时5s，包括按键和触摸事件

二. appNotResponding处理流程
frameworks/base/services/core/java/com/android/server/am/AppErrors.java
1. AppErrors.appNotResponding
```
final void appNotResponding(ProcessRecord app, ActivityRecord activity, ActivityRecord parent, boolean aboveSystem, 
   final String annotation) {
     boolean showBackground = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ANR_SHOW_BACKGROUND, 0) != 0;

     boolean isSilentANR;
      ...//前台ANR 与后台ANR
      isSilentANR = !showBackground && !isInterestingForBackgroundTraces(app);
    ...
    //第一次 更新cpu统计信息
    if (ActivityManagerService.MONITOR_CPU_USAGE) {
            mService.updateCpuStatsNow();
    } 
    synchronized (this) {
      // 跳过一些场景下的ANR
      if (mService.mShuttingDown) {
      //PowerManager.reboot() 会阻塞很长时间，因此忽略关机时的ANR
                return;
        } else if (app.notResponding) {  // 已经有一个ANR弹出框时
            return;
        } else if (app.crashing) { // 处在一个正在crashing的进程
            return;
        } else if (app.killedByAm) { 当进程被activity manager kill
            return;
        } else if (app.killed) { 进程已经被kill
            return;
        }
      //记录ANR到EventLog
      EventLog.writeEvent(EventLogTags.AM_ANR, app.userId, app.pid,
              app.processName, app.info.flags, annotation);
              
      // 将当前进程添加到firstPids
      firstPids.add(app.pid);
      int parentPid = app.pid;
      
      //将system_server进程添加到firstPids
      if (MY_PID != app.pid && MY_PID != parentPid) firstPids.add(MY_PID);
      
      for (int i = mLruProcesses.size() - 1; i >= 0; i--) {
          ProcessRecord r = mLruProcesses.get(i);
          if (r != null && r.thread != null) {
              int pid = r.pid;
              if (pid > 0 && pid != app.pid && pid != parentPid && pid != MY_PID) {
                  if (r.persistent) {
                      firstPids.add(pid); //将persistent进程添加到firstPids    //todo persistent进程是啥
                  } else {
                      lastPids.put(pid, Boolean.TRUE); //其他进程添加到lastPids
                  }
              }
          }
      }
    }
    
    // 记录ANR输出到main log
    StringBuilder info = new StringBuilder();
    info.setLength(0);
    info.append("ANR in ").append(app.processName);
    if (activity != null && activity.shortComponentName != null) {
        info.append(" (").append(activity.shortComponentName).append(")");
    }
    info.append("\n");
    info.append("PID: ").append(app.pid).append("\n");
    if (annotation != null) {
        info.append("Reason: ").append(annotation).append("\n");
    }
    if (parent != null && parent != activity) {
        info.append("Parent: ").append(parent.shortComponentName).append("\n");
    }
    
    //创建CPU tracker对象
     ProcessCpuTracker processCpuTracker = new ProcessCpuTracker(true);
    //输出traces信息【见小节2】
    // For background ANRs, don't pass the ProcessCpuTracker to
        // avoid spending 1/2 second collecting stats to rank lastPids.
    File tracesFile = mService.dumpStackTraces(true, firstPids,
                                                   (isSilentANR) ? null : processCpuTracker,
                                                   (isSilentANR) ? null : lastPids,
                                                   nativePids);
     String cpuInfo = null;
    if (ActivityManagerService.MONITOR_CPU_USAGE) {
        //第二次更新cpu统计信息
        mService.updateCpuStatsNow();
        //记录当前各个进程的CPU使用情况
        synchronized (mService.mProcessCpuTracker) {
            cpuInfo = mService.mProcessCpuTracker.printCurrentState(anrTime);
        }
        info.append(processCpuTracker.printCurrentLoad()); //记录当前CPU负载情况
        info.append(cpuInfo);
    }

    info.append(processCpuTracker.printCurrentState(anrTime));  //记录从anr时间开始的Cpu使用情况      
    //输出当前ANR的reason，以及CPU使用率、负载信息
    Slog.e(TAG, info.toString()); 
    
    //将traces文件 和 CPU使用率信息保存到dropbox，即data/system/dropbox目录      //todo dropbox
    addErrorToDropBox("anr", app, app.processName, activity, parent, annotation,
            cpuInfo, tracesFile, null);
            
    synchronized (mService) {
            mService.mBatteryStatsService.noteProcessAnr(app.processName, app.uid);

            if (isSilentANR) { //后台ANR的情况, 则直接杀掉
                app.kill("bg anr", true);
                return;
            }
           //设置app的ANR状态，并查询错误报告receiver
            makeAppNotRespondingLocked(app,
                    activity != null ? activity.shortComponentName : null,
                    annotation != null ? "ANR " + annotation : "ANR",
                    info.toString());

            //弹出ANR对话框
            Message msg = Message.obtain();
            HashMap<String, Object> map = new HashMap<String, Object>();
            msg.what = ActivityManagerService.SHOW_NOT_RESPONDING_UI_MSG;
            msg.obj = map;
            msg.arg1 = aboveSystem ? 1 : 0;
            map.put("app", app);
            if (activity != null) {
                map.put("activity", activity);
            }
            //向ui线程发送，内容为SHOW_NOT_RESPONDING_MSG的消息
            mService.mUiHandler.sendMessage(msg);
        }
 }
 static boolean isInterestingForBackgroundTraces(ProcessRecord app) {
        // The system_server is always considered interesting.
        if (app.pid == MY_PID) {
            return true;
        }

        // A package is considered interesting if any of the following is true :
        //
        // - It's displaying an activity.
        // - It's the SystemUI.
        // - It has an overlay or a top UI visible.
        //
        // NOTE: The check whether a given ProcessRecord belongs to the systemui
        // process is a bit of a kludge, but the same pattern seems repeated at
        // several places in the system server.
        return app.isInterestingToUserLocked() ||
            (app.info != null && "com.android.systemui".equals(app.info.packageName)) ||
            (app.hasTopUi || app.hasOverlayUi);
    }
```

当发生ANR时, 会按顺序依次执行:
1 输出ANR Reason信息到EventLog. 也就是说ANR触发的时间点最接近的就是EventLog中输出的am_anr信息;
2 收集并输出重要进程列表中的各个线程的traces信息，该方法较耗时; 【见小节2】
3 输出当前各个进程的CPU使用情况以及CPU负载情况;
4 将traces文件和 CPU使用情况信息保存到dropbox，即data/system/dropbox目录
5 根据进程类型,来决定直接后台杀掉,还是弹框告知用户.

ANR输出重要进程的traces信息，这些进程包含:
1 firstPids队列：第一个是ANR进程，第二个是system_server，剩余是所有persistent进程；
2 Native队列：是指/system/bin/目录的mediaserver,sdcard 以及surfaceflinger进程；
3 lastPids队列: 是指mLruProcesses中的不属于firstPids的所有进程。


2. AMS.dumpStackTraces
frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java
```
public static File dumpStackTraces(boolean clearTraces, ArrayList<Integer> firstPids, ProcessCpuTracker processCpuTracker, SparseArray<Boolean> lastPids, String[] nativeProcs) {
    //默认为 data/anr/traces.txt
    String tracesPath = SystemProperties.get("dalvik.vm.stack-trace-file", null);
    if (tracesPath == null || tracesPath.length() == 0) {
        return null;
    }

    File tracesFile = new File(tracesPath);
    try {
        //当clearTraces，则删除已存在的traces文件
        if (clearTraces && tracesFile.exists()) tracesFile.delete();
        //创建traces文件
        tracesFile.createNewFile();
        FileUtils.setPermissions(tracesFile.getPath(), 0666, -1, -1);
    } catch (IOException e) {
        return null;
    }
    //输出trace内容【见小节3】
    dumpStackTraces(tracesPath, firstPids, processCpuTracker, lastPids, nativeProcs);
    return tracesFile;
}
```   
这里会保证data/anr/traces.txt文件内容是全新的方式，而非追加。

3. AMS.dumpStackTraces
```
private static void dumpStackTraces(String tracesPath, ArrayList<Integer> firstPids,
            ProcessCpuTracker processCpuTracker, SparseArray<Boolean> lastPids,
            ArrayList<Integer> nativePids) {.
        DumpStackFileObserver observer = new DumpStackFileObserver(tracesPath);
        //所有信息必须在20秒内收集完成
        long remainingTime = 20 * 1000;
        try {
            observer.startWatching();

             //首先，获取最重要进程的stacks
            if (firstPids != null) {
                int num = firstPids.size();
                for (int i = 0; i < num; i++) {
                    //向目标进程发送signal来输出traces 
                    final long timeTaken = observer.dumpWithTimeout(firstPids.get(i), remainingTime);
                    remainingTime -= timeTaken;             
                }
            }

            //下一步，获取native进程的stacks
            if (nativePids != null) {
                for (int pid : nativePids) {
                    final long nativeDumpTimeoutMs = Math.min(
                            DumpStackFileObserver.NATIVE_DUMP_TIMEOUT_MS, remainingTime);
                    final long start = SystemClock.elapsedRealtime();
                    //输出native进程的trace【见小节4】
                    Debug.dumpNativeBacktraceToFileTimeout(
                            pid, tracesPath, (int) (nativeDumpTimeoutMs / 1000));
                    final long timeTaken = SystemClock.elapsedRealtime() - start;

                    remainingTime -= timeTaken;
                    if (remainingTime <= 0) {                     
                        return;
                    }

                }
            }

            //测量CPU使用情况
            if (processCpuTracker != null) {
                processCpuTracker.init();   //todo cpu使用如何测量
                System.gc();
                processCpuTracker.update();
                try {
                    synchronized (processCpuTracker) {
                        processCpuTracker.wait(500); // measure over 1/2 second.
                    }
                } catch (InterruptedException e) {
                }
                processCpuTracker.update();

                final int N = processCpuTracker.countWorkingStats();
                int numProcs = 0;
                //从lastPids中选取CPU使用率 top 5的进程，输出这些进程的stacks
                for (int i=0; i<N && numProcs<5; i++) {
                    ProcessCpuTracker.Stats stats = processCpuTracker.getWorkingStats(i);
                    if (lastPids.indexOfKey(stats.pid) >= 0) {
                        numProcs++;
                        final long timeTaken = observer.dumpWithTimeout(stats.pid, remainingTime);
                        remainingTime -= timeTaken;
                        if (remainingTime <= 0) {
                            return;
                        }
                    } else if (DEBUG_ANR) {
                    }
                }
            }
        } finally {
            observer.stopWatching();
        }
    }
    
    
 public static class DumpStackFileObserver extends FileObserver {
        // Keep in sync with frameworks/native/cmds/dumpstate/utils.cpp
        private static final int TRACE_DUMP_TIMEOUT_MS = 10000; // 10 seconds
        static final int NATIVE_DUMP_TIMEOUT_MS = 2000; // 2 seconds;
        public long dumpWithTimeout(int pid, long timeout) {
            //向目标进程发送signal来输出traces  Process.SIGNAL_QUIT=3 也就是signal 3信号
            sendSignal(pid, SIGNAL_QUIT);
            final long start = SystemClock.elapsedRealtime();
            // //等待直到写关闭，或者超时 
            final long waitTime = Math.min(timeout, TRACE_DUMP_TIMEOUT_MS);
            synchronized (this) {
                try {
                    wait(waitTime); // Wait for traces file to be closed.
                } catch (InterruptedException e) {
                    Slog.wtf(TAG, e);
                }
            }

            final long timeWaited = SystemClock.elapsedRealtime() - start;
            if (timeWaited >= timeout) {
                return timeWaited;
            }

            if (!mClosed) {             
                final long nativeDumpTimeoutMs = Math.min(
                        NATIVE_DUMP_TIMEOUT_MS, timeout - timeWaited);

                Debug.dumpNativeBacktraceToFileTimeout(pid, mTracesPath,
                        (int) (nativeDumpTimeoutMs / 1000));
            }
            final long end = SystemClock.elapsedRealtime();
            mClosed = false;

            return (end - start);
        }
    }   
```
该方法的主要功能，依次输出：
1 收集firstPids进程的stacks；
  1 第一个是发生ANR进程；
  2 第二个是system_server；
  3 mLruProcesses中所有的persistent进程；
2 收集Native进程的stacks；(dumpNativeBacktraceToFile)
  依次是mediaserver,sdcard,surfaceflinger进程；
3 收集lastPids进程的stacks;；
  依次输出CPU使用率top 5的进程；


4. dumpNativeBacktraceToFile
```
frameworks/base/core/java/android/os/Debug.java 
public static native void dumpNativeBacktraceToFileTimeout(int pid, String file, int timeoutSecs);

frameworks/base/core/jni/android_os_Debug.cpp
static void android_os_Debug_dumpNativeBacktraceToFileTimeout(JNIEnv* env, jobject clazz,
    jint pid, jstring fileName, jint timeoutSecs)
{
    ...
    dump_backtrace_to_file_timeout(pid, fd, timeoutSecs);
}
```   
Debug.dumpNativeBacktraceToFile(pid, tracesPath)经过JNI调用如下方法：
system/core/debuggerd/client/debuggerd_client.cpp
```
int dump_backtrace_to_file(pid_t tid, int fd) {
    return dump_backtrace_to_file_timeout(tid, fd, 0);
}
int dump_backtrace_to_file_timeout(pid_t tid, int fd, int timeout_secs) {
  android::base::unique_fd copy(dup(fd));
  int timeout_ms = timeout_secs > 0 ? timeout_secs * 1000 : 0;
  return debuggerd_trigger_dump(tid, std::move(copy), kDebuggerdBacktrace, timeout_ms) ? 0 : -1;
}
bool debuggerd_trigger_dump(pid_t pid, unique_fd output_fd, DebuggerdDumpType dump_type,
                            unsigned int timeout_ms) {
 ...
 //创建socket                           
sockfd.reset(socket(AF_LOCAL, SOCK_SEQPACKET, 0));  
//连接服务器   socket name = “kTombstonedInterceptSocketName” 的socket服务端在system/core/debuggerd/tombstoned/tombstoned.cpp的main中初始化
if (socket_local_client_connect(set_timeout(sockfd.get()), kTombstonedInterceptSocketName,
                                  ANDROID_SOCKET_NAMESPACE_RESERVED, SOCK_SEQPACKET) == -1) {
    return false;
  } 
//初始化管道  
unique_fd pipe_read, pipe_write;
if (!Pipe(&pipe_read, &pipe_write)) {
  return false;
}
//初始化请求
InterceptRequest req = {.pid = pid }; 
ssize_t rc = SendFileDescriptors(set_timeout(sockfd), &req, sizeof(req), pipe_write.get());
InterceptResponse response;   
bool backtrace = dump_type == kDebuggerdBacktrace;
send_signal(pid, backtrace);  //根据dump类型发送信号  sigqueue向需要dump堆栈的进程发送SIGQUIT信号，也就是signal 3信号  //todo sigqueue
rc = TEMP_FAILURE_RETRY(recv(set_timeout(sockfd.get()), &response, sizeof(response), MSG_TRUNC));
 while (true) {
    struct pollfd pfd = {
        .fd = pipe_read.get(), .events = POLLIN, .revents = 0,
    };
    rc = poll(&pfd, 1, remaining_ms);
    char buf[1024];
    //读取到服务端发送过来的数据，并写入buffer
    rc = TEMP_FAILURE_RETRY(read(pipe_read.get(), buf, sizeof(buf)));
   
   //再将buffer数据输出到traces.txt文件 
   if (!android::base::WriteFully(output_fd.get(), buf, rc)) {
      return false;
    } 
 }                       
}

static bool send_signal(pid_t pid, bool backtrace) {
  sigval val;
  val.sival_int = backtrace;
  if (sigqueue(pid, DEBUGGER_SIGNAL, val) != 0) {
    return false;
  }
  return true;
}
```
可见，这个过程主要是通过向debuggerd守护进程发送命令， debuggerd收到该命令，
    在子进程中调用 dump_backtrace()来输出backtrace，更多内容见Native进程之Trace原理
//todo  http://gityuan.com/2016/11/27/native-traces/

三. 总结
触发ANR时系统会输出关键信息：(这个较耗时,可能会有10s)
1 将am_anr信息,输出到EventLog.(ANR开始起点看EventLog)
2 获取重要进程trace信息，保存到/data/anr/traces.txt；(会先删除老的文件)
   Java进程的traces;
   Native进程的traces;
3 ANR reason以及CPU使用情况信息，输出到main log;
4 再将CPU使用情况和进程trace文件信息，再保存到/data/system/dropbox；

//todo trace过程
整个过程中进程Trace的输出是最为核心的环节，Java和Native进程采用不同的策略，如下：
进程类型	trace命令	文章	描述
Java	kill -3 [pid]	解读Java进程的Trace文件	不适用于Native进程        http://gityuan.com/2016/11/26/art-trace/
Native	debuggerd -b [pid]	Native进程之Trace原理	也适用于Java进程          http://gityuan.com/2016/11/27/native-traces/

说明：kill -3命令需要虚拟机的支持，所以无法输出Native进程traces.而debuggerd -b [pid]也可用于Java进程，但信息量远没有kill -3多。 
   总之，ANR信息最为重要的是dropbox信息，比如system_server_anr。

重要节点：
进程名：cat /proc/[pid]/cmdline
线程名：cat /proc/[tid]/comm
Kernel栈：cat /proc/[tid]/stack
Native栈： 解析 /proc/[pid]/maps
