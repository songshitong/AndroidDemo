
android-13.0.0_r1    不同版本adj值不同
http://gityuan.com/2016/08/07/android-adj/
Adj
定义在ProcessList.java文件。adj值越大，优先级越低，adj<0的进程都是系统进程。
frameworks/base/services/core/java/com/android/server/am/ProcessList.java
```
ADJ级别	              取值	  解释
UNKNOWN_ADJ	          1001	  一般指将要会缓存进程，无法获取确定值
CACHED_APP_MAX_ADJ	   999	  不可见进程的adj最大值
CACHED_APP_MIN_ADJ	  900 	  不可见进程的adj最小值
SERVICE_B_ADJ	        800	  B List中的Service（较老的、使用可能性更小）
PREVIOUS_APP_ADJ	   700	  上一个App的进程(往往通过按返回键)
HOME_APP_ADJ	       600	 Home进程
SERVICE_ADJ	           500	 服务进程(Service process)
HEAVY_WEIGHT_APP_ADJ	400	 后台的重量级进程，system/rootdir/init.rc文件中设置
BACKUP_APP_ADJ	        300	 备份进程
PERCEPTIBLE_APP_ADJ	    200	 可感知进程，比如后台音乐播放
PERCEPTIBLE_RECENT_FOREGROUND_APP_ADJ 50 从前台应用到前台服务
VISIBLE_APP_ADJ	        100	可见进程(Visible process)
FOREGROUND_APP_ADJ	    0	前台进程（Foreground process)
PERSISTENT_SERVICE_ADJ	-700	关联着系统或persistent进程
PERSISTENT_PROC_ADJ	    -800 系统persistent进程，比如telephony
SYSTEM_ADJ	          -900	系统进程
NATIVE_ADJ	-1000    	native进程（不被系统管理）
```

ProcessState
定义在ActivityManager.java文件，process_state划分18类

//frameworks/base/core/java/com/android/internal/app/procstats/ProcessState.java
frameworks/base/core/java/com/android/internal/app/procstats/ProcessStats.java
```
state级别	                          取值	解释
STATE_CACHED_EMPTY	          15	进程处于cached状态，且为空进程
STATE_CACHED_ACTIVITY_CLIENT	14	进程处于cached状态，且为另一个cached进程(内含Activity)的client进程
STATE_CACHED_ACTIVITY	       13	进程处于cached状态，且内含Activity
STATE_LAST_ACTIVITY	           12	后台进程，且拥有上一次显示的Activity
STATE_HOME	                   11   后台进程，且拥有home Activity
STATE_RECEIVER	               9	后台进程，且正在运行receiver
STATE_SERVICE	               7	后台进程，且正在运行service
STATE_HEAVY_WEIGHT	           10	后台进程，但无法执行restore，因此尽量避免kill该进程
STATE_BACKUP	               6	后台进程，正在运行backup/restore操作
STATE_IMPORTANT_BACKGROUND	   5	对用户很重要的进程，用户不可感知其存在
STATE_IMPORTANT_FOREGROUND	   4	对用户很重要的进程，用户可感知其存在
//STATE_TOP_SLEEPING	5	与PROCESS_STATE_TOP一样，但此时设备正处于休眠状态
//STATE_FOREGROUND_SERVICE	4	拥有一个前台Service
//STATE_BOUND_FOREGROUND_SERVICE	3	拥有一个前台Service，且由系统绑定
STATE_TOP	                   1	拥有当前用户可见的top Activity
//STATE_PERSISTENT_UI	1	persistent系统进程，并正在执行UI操作
STATE_PERSISTENT	         0	persistent系统进程
//STATE_NONEXISTENT	         -1	不存在的进程state级别	取值	解释
```

三大护法
调整进程的adj的3大护法, 也就是ADJ算法的核心方法:
updateOomAdjLocked：更新adj，当目标进程为空，或者被杀则返回false；否则返回true;
computeOomAdjLocked：计算adj，返回计算后RawAdj值;
updateAndTrimProcessLSP：应用adj，当需要杀掉目标进程则返回false；否则返回true。
前面提到调整adj的3大护法，最为常见的方法便是updateOomAdjLocked，这也是其他各个方法在需要更新adj时会调用的方法，该方法有3个不同参数的同名方法，定义如下：
```
无参方法：updateOomAdjLocked()
一参方法：updateOomAdjLocked(ProcessRecord app)
五参方法：updateOomAdjLocked(ProcessRecord app, int cachedAdj,
    ProcessRecord TOP_APP, boolean doingAll, long now)
```
updateOomAdjLocked实现过程中依次会computeOomAdjLocked和updateAndTrimProcessLSP


ADJ的更新时机
先来说说哪些场景下都会触发updateOomAdjLocked来更新进程adj:

2.1 Activity    
frameworks/base/services/core/java/com/android/server/wm/ActivityTaskSupervisor.java
ASS.realStartActivityLocked: 启动Activity   
frameworks/base/services/core/java/com/android/server/wm/ActivityRecord.java
AR.makeActiveIfNeeded: 恢复栈顶Activity
AS.finishCurrentActivityLocked: 结束当前Activity
AR.destroyImmediately: 摧毁当前Activity
2.2 Service
位于ActiveServices.java  frameworks/base/services/core/java/com/android/server/am/ActiveServices.java
realStartServiceLocked: 启动服务
bindServiceLocked: 绑定服务(只更新当前app)
unbindServiceLocked: 解绑服务 (只更新当前app)
bringDownServiceLocked: 结束服务 (只更新当前app)
sendServiceArgsLocked: 在bringup或则cleanup服务过程调用 (只更新当前app)
2.3 broadcast  frameworks/base/services/core/java/com/android/server/am/BroadcastQueue.java
BQ.processNextBroadcast: 处理下一个广播
BQ.processCurBroadcastLocked: 处理当前广播
BQ.deliverToRegisteredReceiverLocked: 分发已注册的广播 (只更新当前app)
2.4 ContentProvider   frameworks/base/services/core/java/com/android/server/am/ContentProviderHelper.java
AMS.removeContentProvider: 移除provider
AMS.publishContentProviders: 发布provider (只更新当前app)
AMS.getContentProviderImpl: 获取provider (只更新当前app)
2.5 Process
位于ActivityManagerService.java   frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java
setSystemProcess: 创建并设置系统进程
addAppLocked: 创建persistent进程
attachApplicationLocked: 进程创建后attach到system_server的过程;
trimApplications: 清除没有使用app
appDiedLocked: 进程死亡
killAllBackgroundProcesses: 杀死所有后台进程.即(ADJ>9或removed=true的普通进程)
killPackageProcessesLSP: 以包名的形式 杀掉相关进程;


updateOomAdjLocked   
在介绍updateOomAdjLocked方法之前,先简单介绍这个过程会遇到的比较重要的参数.
frameworks/base/services/core/java/com/android/server/am/ActivityManagerConstants.java
空进程存活时长： MAX_EMPTY_TIME = 30min
(缓存+空)进程个数上限：MAX_CACHED_PROCESSES = SystemProperties.getInt(“sys.fw.bg_apps_limit”,32) = 32(默认)；
空进程个数上限：CUR_MAX_EMPTY_PROCESSES  = computeEmptyProcessLimit(MAX_CACHED_APPS) = MAX_CACHED_APPS/2 = 16；
trim空进程个数上限：CUR_TRIM_EMPTY_PROCESSES  = computeEmptyProcessLimit(MAX_CACHED_PROCESSES) / 2 = 8；
trim缓存进程个数上限：  CUR_TRIM_CACHED_PROCESSES =
(MAX_CACHED_PROCESSES(32) - computeEmptyProcessLimit(MAX_CACHED_PROCESSES)) / 3 = 5;





frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java
```
 final void updateOomAdjLocked(String oomAdjReason) {
        mOomAdjuster.updateOomAdjLocked(oomAdjReason);
    }
```
frameworks/base/services/core/java/com/android/server/am/OomAdjuster.java
```
void updateOomAdjLocked(String oomAdjReason) {
        synchronized (mProcLock) {
            updateOomAdjLSP(oomAdjReason);
        }
    }

 private void updateOomAdjLSP(String oomAdjReason) {
       ..
        try {
            mOomAdjUpdateOngoing = true;
            performUpdateOomAdjLSP(oomAdjReason);
        } finally {
           ...
        }
    } 
    
   private void performUpdateOomAdjLSP(String oomAdjReason) {
        final ProcessRecord topApp = mService.getTopApp();
        // Clear any pending ones because we are doing a full update now.
        mPendingProcessSet.clear();
        mService.mAppProfiler.mHasPreviousProcess = mService.mAppProfiler.mHasHomeProcess = false;
        updateOomAdjInnerLSP(oomAdjReason, topApp , null, null, true, true);
    }  
    
 
  private void updateOomAdjInnerLSP(String oomAdjReason, final ProcessRecord topApp,
            ArrayList<ProcessRecord> processes, ActiveUids uids, boolean potentialCycles,
            boolean startProfiling) {
       ..
        final long now = SystemClock.uptimeMillis();
        final long nowElapsed = SystemClock.elapsedRealtime();
        final long oldTime = now - mConstants.mMaxEmptyTimeMillis;
        final boolean fullUpdate = processes == null;
        ActiveUids activeUids = uids;
        ArrayList<ProcessRecord> activeProcesses = fullUpdate ? mProcessList.getLruProcessesLOSP()
                : processes;
        final int numProc = activeProcesses.size();
       ....
       for (int  i = activeUids.size() - 1; i >= 0; i--) {
            final UidRecord uidRec = activeUids.valueAt(i);
            ...
            uidRec.reset(); //重置状态mCurProcState为空进程 ActivityManager.PROCESS_STATE_CACHED_EMPTY
        }

       ...
        for (int i = numProc - 1; i >= 0; i--) {
            ProcessRecord app = activeProcesses.get(i);
            final ProcessStateRecord state = app.mState;
            if (!app.isKilledByAm() && app.getThread() != null) {
                state.setProcStateChanged(false);
                //计算adj
                computeOomAdjLSP(app, ProcessList.UNKNOWN_ADJ, topApp, fullUpdate, now, false,
                        computeClients); // It won't enter cycle if not computing clients.
                // if any app encountered a cycle, we need to perform an additional loop later
                retryCycles |= state.containsCycle();
                // Keep the completedAdjSeq to up to date.
                state.setCompletedAdjSeq(mAdjSeq);
            }
        }

       ...
        boolean allChanged = updateAndTrimProcessLSP(now, nowElapsed, oldTime, activeUids,
                oomAdjReason);
        mNumServiceProcs = mNewNumServiceProcs;
        ...
    }
```
computeOomAdjLSP
空进程情况
```
  private boolean computeOomAdjLSP(ProcessRecord app, int cachedAdj,
            ProcessRecord topApp, boolean doingAll, long now, boolean cycleReEval,
            boolean computeClients) {
    final ProcessStateRecord state = app.mState;
        //已经完成
        if (mAdjSeq == state.getAdjSeq()) {
            if (state.getAdjSeq() == state.getCompletedAdjSeq()) {
                // This adjustment has already been computed successfully.
                return false;
            } else {
                // The process is being computed, so there is a cycle. We cannot
                // rely on this process's state.
                state.setContainsCycle(true);
                mProcessesInCycle.add(app);

                return false;
            }
        }

        // 当进程对象为空时，则设置curProcState=PROCESS_STATE_CACHED_EMPTY， curAdj=CACHED_APP_MAX_ADJ 
        if (app.getThread() == null) {
            ...
            state.setCurProcState(PROCESS_STATE_CACHED_EMPTY);
            state.setCurAdj(ProcessList.CACHED_APP_MAX_ADJ);
            state.setCurRawAdj(ProcessList.CACHED_APP_MAX_ADJ);
            ...
            return false;
        }          
}
```
maxAdj<=0情况
```
final int appUid = app.info.uid;
        final int logUid = mService.mCurOomAdjUid;
        int prevAppAdj = state.getCurAdj();
        int prevProcState = state.getCurProcState();
        int prevCapability = state.getCurCapability();
        final ProcessServiceRecord psr = app.mServices;

        if (state.getMaxAdj() <= ProcessList.FOREGROUND_APP_ADJ) {
            // The max adjustment doesn't allow this app to be anything
            // below foreground, so it is not worth doing work for it.
            if (DEBUG_OOM_ADJ_REASON || logUid == appUid) {
                reportOomAdjMessageLocked(TAG_OOM_ADJ, "Making fixed: " + app);
            }
            state.setAdjType("fixed");
            state.setAdjSeq(mAdjSeq);
            state.setCurRawAdj(state.getMaxAdj());
            state.setHasForegroundActivities(false);
            state.setCurrentSchedulingGroup(ProcessList.SCHED_GROUP_DEFAULT);
            state.setCurCapability(PROCESS_CAPABILITY_ALL);
            state.setCurProcState(ActivityManager.PROCESS_STATE_PERSISTENT);
            state.setSystemNoUi(true);
            //顶部的activity就是当前app，则代表正处于展现UI
            if (app == topApp) {
                state.setSystemNoUi(false);
                state.setCurrentSchedulingGroup(ProcessList.SCHED_GROUP_TOP_APP);
                state.setAdjType("pers-top-activity");
            } else if (state.hasTopUi()) {
                // sched group/proc state adjustment is below
                state.setSystemNoUi(false);
                state.setAdjType("pers-top-ui");
            } else if (state.getCachedHasVisibleActivities()) { //进程中的activity个数大于0时
                state.setSystemNoUi(false);
            }
            if (!state.isSystemNoUi()) {
                if (mService.mWakefulness.get() == PowerManagerInternal.WAKEFULNESS_AWAKE
                        || state.isRunningRemoteAnimation()) {
                    // screen on or animating, promote UI
                    state.setCurProcState(ActivityManager.PROCESS_STATE_PERSISTENT_UI);
                    state.setCurrentSchedulingGroup(ProcessList.SCHED_GROUP_TOP_APP);
                } else {
                    // screen off, restrict UI scheduling
                    state.setCurProcState(PROCESS_STATE_BOUND_FOREGROUND_SERVICE);
                    state.setCurrentSchedulingGroup(ProcessList.SCHED_GROUP_RESTRICTED);
                }
            }
            state.setCurRawProcState(state.getCurProcState());
            state.setCurAdj(state.getMaxAdj());
            state.setCompletedAdjSeq(state.getAdjSeq());
            // if curAdj is less than prevAppAdj, then this process was promoted
            return state.getCurAdj() < prevAppAdj || state.getCurProcState() < prevProcState;
        }
```
当maxAdj <=0的情况，也就意味这不允许app将其adj调整到低于前台app的优先级别, 这样场景下执行后将直接返回:  //adj越小，优先级越高
curProcState =PROCESS_STATE_BOUND_FOREGROUND_SERVICE或 PROCESS_STATE_PERSISTENT_UI(存在visible的activity)
curAdj = app.maxAdj (curAdj<=0)

前台的情况
```
 final int PROCESS_STATE_CUR_TOP = mService.mAtmInternal.getTopProcessState();
        int adj;
        int schedGroup;
        int procState;
        int capability = cycleReEval ? app.mState.getCurCapability() : 0;

        boolean foregroundActivities = false;
        boolean hasVisibleActivities = false;
        if (PROCESS_STATE_CUR_TOP == PROCESS_STATE_TOP && app == topApp) {
            // The last app on the list is the foreground app.
            adj = ProcessList.FOREGROUND_APP_ADJ;
            schedGroup = ProcessList.SCHED_GROUP_TOP_APP;
            state.setAdjType("top-activity");
            foregroundActivities = true;
            hasVisibleActivities = true;
            procState = PROCESS_STATE_CUR_TOP;
            ...
        }else if (state.isRunningRemoteAnimation()) {
            adj = ProcessList.VISIBLE_APP_ADJ;
            schedGroup = ProcessList.SCHED_GROUP_TOP_APP;
            state.setAdjType("running-remote-anim");
            procState = PROCESS_STATE_CUR_TOP;
           ...
        } else if (app.getActiveInstrumentation() != null) {
            adj = ProcessList.FOREGROUND_APP_ADJ;
            schedGroup = ProcessList.SCHED_GROUP_DEFAULT;
            state.setAdjType("instrumentation");
            procState = PROCESS_STATE_FOREGROUND_SERVICE;
           ....
        } else if (state.getCachedIsReceivingBroadcast(mTmpBroadcastQueue)) {
            adj = ProcessList.FOREGROUND_APP_ADJ;
            schedGroup = (mTmpBroadcastQueue.contains(mService.mFgBroadcastQueue))
                    ? ProcessList.SCHED_GROUP_DEFAULT : ProcessList.SCHED_GROUP_BACKGROUND;
            state.setAdjType("broadcast");
            procState = ActivityManager.PROCESS_STATE_RECEIVER;
           ...
        } else if (psr.numberOfExecutingServices() > 0) {
            adj = ProcessList.FOREGROUND_APP_ADJ;
            schedGroup = psr.shouldExecServicesFg()
                    ? ProcessList.SCHED_GROUP_DEFAULT : ProcessList.SCHED_GROUP_BACKGROUND;
            state.setAdjType("exec-service");
            procState = PROCESS_STATE_SERVICE;
            ...
        } else if (app == topApp) {
            adj = ProcessList.FOREGROUND_APP_ADJ;
            schedGroup = ProcessList.SCHED_GROUP_BACKGROUND;
            state.setAdjType("top-sleeping");
            foregroundActivities = true;
            procState = PROCESS_STATE_CUR_TOP;
            ...
        } else {
            // As far as we know the process is empty.  We may change our mind later.
            schedGroup = ProcessList.SCHED_GROUP_BACKGROUND;
            // At this point we don't actually know the adjustment.  Use the cached adj
            // value that the caller wants us to.
            adj = cachedAdj;
            procState = PROCESS_STATE_CACHED_EMPTY;
            if (!state.containsCycle()) {
                state.setCached(true);
                state.setEmpty(true);
                state.setAdjType("cch-empty");
            }
            ...
        } 
```
Case	                         adj	procState
当app是当前展示的app	            adj=0	PROCESS_STATE_CUR_TOP
running-remote-anim             adj=100     PROCESS_STATE_CUR_TOP
当instrumentation不为空时      	adj=0	PROCESS_STATE_FOREGROUND_SERVICE
当进程存在正在接收的broadcastrecevier	adj=0	PROCESS_STATE_RECEIVER
当进程存在正在执行的service	        adj=0	     PROCESS_STATE_SERVICE
top-sleeping                    adj=0        PROCESS_STATE_CUR_TOP
以上条件都不符合	              adj=cachedAdj(>=0)	PROCESS_STATE_CACHED_EMPTY


非前台activity的情况
```
        if (!foregroundActivities && state.getCachedHasActivities()) {
            state.computeOomAdjFromActivitiesIfNecessary(mTmpComputeOomAdjWindowCallback,
                    adj, foregroundActivities, hasVisibleActivities, procState, schedGroup,
                    appUid, logUid, PROCESS_STATE_CUR_TOP);

            adj = state.getCachedAdj();
            foregroundActivities = state.getCachedForegroundActivities();
            hasVisibleActivities = state.getCachedHasVisibleActivities();
            procState = state.getCachedProcState();
            schedGroup = state.getCachedSchedGroup();
        }
```
frameworks/base/services/core/java/com/android/server/am/ProcessStateRecord.java
```
 void computeOomAdjFromActivitiesIfNecessary(OomAdjuster.ComputeOomAdjWindowCallback callback,
            int adj, boolean foregroundActivities, boolean hasVisibleActivities, int procState,
            int schedGroup, int appUid, int logUid, int processCurTop) {
        if (mCachedAdj != ProcessList.INVALID_ADJ) {
            return;
        }
        callback.initialize(mApp, adj, foregroundActivities, hasVisibleActivities, procState,
                schedGroup, appUid, logUid, processCurTop);
        final int minLayer = Math.min(ProcessList.VISIBLE_APP_LAYER_MAX,
                mApp.getWindowProcessController().computeOomAdjFromActivities(callback));

        mCachedAdj = callback.adj;
        mCachedForegroundActivities = callback.foregroundActivities;
        mCachedHasVisibleActivities = callback.mHasVisibleActivities ? VALUE_TRUE : VALUE_FALSE;
        mCachedProcState = callback.procState;
        mCachedSchedGroup = callback.schedGroup;

        if (mCachedAdj == ProcessList.VISIBLE_APP_ADJ) {
            mCachedAdj += minLayer;
        }
    }
```
主要逻辑都在ComputeOomAdjWindowCallback
frameworks/base/services/core/java/com/android/server/am/OomAdjuster.java
```
final class ComputeOomAdjWindowCallback
            implements WindowProcessController.ComputeOomAdjCallback {
   public void onVisibleActivity() {
            // App has a visible activity; only upgrade adjustment.
            if (adj > ProcessList.VISIBLE_APP_ADJ) {
                adj = ProcessList.VISIBLE_APP_ADJ;
                mState.setAdjType("vis-activity");
               ...
            }
            if (procState > processStateCurTop) {
                //状态为PROCESS_STATE_CUR_TOP
                procState = processStateCurTop;
                mState.setAdjType("vis-activity");
                ...
            }
           ...
        }  
        
   @Override
        public void onPausedActivity() {
            if (adj > ProcessList.PERCEPTIBLE_APP_ADJ) {
                adj = ProcessList.PERCEPTIBLE_APP_ADJ;
                mState.setAdjType("pause-activity");
                ...
            }
            if (procState > processStateCurTop) {
                procState = processStateCurTop;
                mState.setAdjType("pause-activity");
               ...
            }
           ...
        }  
        
   @Override
        public void onStoppingActivity(boolean finishing) {
            if (adj > ProcessList.PERCEPTIBLE_APP_ADJ) {
                adj = ProcessList.PERCEPTIBLE_APP_ADJ;
                mState.setAdjType("stop-activity");
                ...
            }
            if (!finishing) {
                if (procState > PROCESS_STATE_LAST_ACTIVITY) {
                    procState = PROCESS_STATE_LAST_ACTIVITY;
                    mState.setAdjType("stop-activity");
                   ...
                }
            }
           ...
        }
        
    @Override
        public void onOtherActivity() {
            if (procState > PROCESS_STATE_CACHED_ACTIVITY) {
                procState = PROCESS_STATE_CACHED_ACTIVITY;
                mState.setAdjType("cch-act");
              ...
            }
            mHasVisibleActivities = false;
        }                       
}
```
对于进程中的activity处于非前台情况
当activity可见， 则adj=100,procState=PROCESS_STATE_CUR_TOP；
当activity正在暂停或者已经暂停， 则adj=200,procState=PROCESS_STATE_CUR_TOP；
当activity正在停止， 则adj=200,procState=PROCESS_STATE_LAST_ACTIVITY(且activity尚未finish)；
以上都不满足，否则procState=PROCESS_STATE_CACHED_ACTIVITY


adj > 200的情况
```
 if (adj > ProcessList.PERCEPTIBLE_APP_ADJ
                || procState > PROCESS_STATE_FOREGROUND_SERVICE) {
            if (psr.hasForegroundServices()) {
                // 当存在前台service时
                adj = ProcessList.PERCEPTIBLE_APP_ADJ;
                procState = PROCESS_STATE_FOREGROUND_SERVICE;
                state.setAdjType("fg-service");
                ...
            } else if (state.hasOverlayUi()) {
                // The process is display an overlay UI.
                adj = ProcessList.PERCEPTIBLE_APP_ADJ;
                procState = PROCESS_STATE_IMPORTANT_FOREGROUND;
                ...
            }
        }

if (psr.hasForegroundServices() && adj > ProcessList.PERCEPTIBLE_RECENT_FOREGROUND_APP_ADJ
                && (state.getLastTopTime() + mConstants.TOP_TO_FGS_GRACE_DURATION > now
                || state.getSetProcState() <= PROCESS_STATE_TOP)) {
            adj = ProcessList.PERCEPTIBLE_RECENT_FOREGROUND_APP_ADJ;
            state.setAdjType("fg-service-act");
            ...
        }        
```
当adj > 200的情况的前提下：
当存在前台service时，则adj=200, procState=PROCESS_STATE_FOREGROUND_SERVICE；
存在浮窗，则adj=200, procState=PROCESS_STATE_IMPORTANT_FOREGROUND；
前台应用切换为前台服务，adj=50,procState= PERCEPTIBLE_RECENT_FOREGROUND_APP_ADJ  //优先级暂时提高一部分


toast的情况
```
if (adj > ProcessList.PERCEPTIBLE_APP_ADJ
                || procState > PROCESS_STATE_TRANSIENT_BACKGROUND) {
            if (state.getForcingToImportant() != null) {
                // This is currently used for toasts...  they are not interactive, and
                // we don't want them to cause the app to become fully foreground (and
                // thus out of background check), so we yes the best background level we can.
                adj = ProcessList.PERCEPTIBLE_APP_ADJ;
                procState = PROCESS_STATE_TRANSIENT_BACKGROUND;
                state.setCached(false);
                state.setAdjType("force-imp");
                state.setAdjSource(state.getForcingToImportant());
               ...
            }
        }
```
toast的情况， adj = 200, procState = PROCESS_STATE_TRANSIENT_BACKGROUND


HeavyWeightProces情况
```
if (state.getCachedIsHeavyWeight()) {
            if (adj > ProcessList.HEAVY_WEIGHT_APP_ADJ) {
                // We don't want to kill the current heavy-weight process.
                adj = ProcessList.HEAVY_WEIGHT_APP_ADJ;
                schedGroup = ProcessList.SCHED_GROUP_BACKGROUND;
                state.setCached(false);
                state.setAdjType("heavy");
                ...
            }
            if (procState > ActivityManager.PROCESS_STATE_HEAVY_WEIGHT) {
                procState = ActivityManager.PROCESS_STATE_HEAVY_WEIGHT;
                state.setAdjType("heavy");
                ..
            }
        }
```
当进程为HeavyWeightProcess，则adj=400,procState为PROCESS_STATE_HEAVY_WEIGHT
可以通过查询adb shell dumpsys package | grep "cant_save_state"


HomeProcess情况
```
 if (state.getCachedIsHomeProcess()) {
            if (adj > ProcessList.HOME_APP_ADJ) {
                adj = ProcessList.HOME_APP_ADJ;
                schedGroup = ProcessList.SCHED_GROUP_BACKGROUND;
                state.setCached(false);
                state.setAdjType("home");
               ..
            }
            if (procState > ActivityManager.PROCESS_STATE_HOME) {
                procState = ActivityManager.PROCESS_STATE_HOME;
                state.setAdjType("home");
                ..
            }
        }
```
adj=600 procState=PROCESS_STATE_HOME

PreviousProcess情况
```
if (state.getCachedIsPreviousProcess() && state.getCachedHasActivities()) {
            if (adj > ProcessList.PREVIOUS_APP_ADJ) {
                // This was the previous process that showed UI to the user.
                // We want to try to keep it around more aggressively, to give
                // a good experience around switching between two apps.
                adj = ProcessList.PREVIOUS_APP_ADJ;
                schedGroup = ProcessList.SCHED_GROUP_BACKGROUND;
                state.setCached(false);
                state.setAdjType("previous");
               ..
            }
            if (procState > PROCESS_STATE_LAST_ACTIVITY) {
                procState = PROCESS_STATE_LAST_ACTIVITY;
                state.setAdjType("previous");
               ..
            }
        }
```
adj=700, procSate=PROCESS_STATE_LAST_ACTIVITY

备份进程情况
```
 final BackupRecord backupTarget = mService.mBackupTargets.get(app.userId);
        if (backupTarget != null && app == backupTarget.app) {
            // If possible we want to avoid killing apps while they're being backed up
            if (adj > ProcessList.BACKUP_APP_ADJ) {
                if (DEBUG_BACKUP) Slog.v(TAG_BACKUP, "oom BACKUP_APP_ADJ for " + app);
                adj = ProcessList.BACKUP_APP_ADJ;
                if (procState > PROCESS_STATE_TRANSIENT_BACKGROUND) {
                    procState = PROCESS_STATE_TRANSIENT_BACKGROUND;
                }
                state.setAdjType("backup");
                ..
            }
            if (procState > ActivityManager.PROCESS_STATE_BACKUP) {
                procState = ActivityManager.PROCESS_STATE_BACKUP;
                state.setAdjType("backup");
                ..
            }
        }
```
adj=300,procState=PROCESS_STATE_TRANSIENT_BACKGROUND


Service情况
```
int capabilityFromFGS = 0; // capability from foreground service.
        boolean boundByNonBgRestricted = state.isCurBoundByNonBgRestrictedApp();
        boolean scheduleLikeTopApp = false;
        for (int is = psr.numberOfRunningServices() - 1;
                is >= 0 && (adj > ProcessList.FOREGROUND_APP_ADJ
                        || schedGroup == ProcessList.SCHED_GROUP_BACKGROUND
                        || procState > PROCESS_STATE_TOP);
                is--) {
            ServiceRecord s = psr.getRunningServiceAt(is);
            if (s.startRequested) {
                state.setHasStartedServices(true);
                if (procState > PROCESS_STATE_SERVICE) {
                    procState = PROCESS_STATE_SERVICE;
                    state.setAdjType("started-services");
                    ..
                }
                if (!s.mKeepWarming && state.hasShownUi() && !state.getCachedIsHomeProcess()) {
                    // If this process has shown some UI, let it immediately
                    // go to the LRU list because it may be pretty heavy with
                    // UI stuff.  We'll tag it with a label just to help
                    // debug and understand what is going on.
                    if (adj > ProcessList.SERVICE_ADJ) {
                        state.setAdjType("cch-started-ui-services");
                    }
                } else {
                    if (s.mKeepWarming
                            || now < (s.lastActivity + mConstants.MAX_SERVICE_INACTIVITY)) {
                        // This service has seen some activity within
                        // recent memory, so we will keep its process ahead
                        // of the background processes.
                        if (adj > ProcessList.SERVICE_ADJ) {
                            adj = ProcessList.SERVICE_ADJ;
                            state.setAdjType("started-services");
                            ..
                            state.setCached(false);
                        }
                    }
                    // If we have let the service slide into the background
                    // state, still have some text describing what it is doing
                    // even though the service no longer has an impact.
                    if (adj > ProcessList.SERVICE_ADJ) {
                        state.setAdjType("cch-started-services");
                    }
                }
            }

            if (s.isForeground) {
                final int fgsType = s.foregroundServiceType;
                if (s.mAllowWhileInUsePermissionInFgs) {
                    capabilityFromFGS |=
                            (fgsType & FOREGROUND_SERVICE_TYPE_LOCATION)
                                    != 0 ? PROCESS_CAPABILITY_FOREGROUND_LOCATION : 0;

                    final boolean enabled = state.getCachedCompatChange(
                            CACHED_COMPAT_CHANGE_CAMERA_MICROPHONE_CAPABILITY);
                    if (enabled) {
                        capabilityFromFGS |=
                                (fgsType & FOREGROUND_SERVICE_TYPE_CAMERA)
                                        != 0 ? PROCESS_CAPABILITY_FOREGROUND_CAMERA : 0;
                        capabilityFromFGS |=
                                (fgsType & FOREGROUND_SERVICE_TYPE_MICROPHONE)
                                        != 0 ? PROCESS_CAPABILITY_FOREGROUND_MICROPHONE : 0;
                    } else {
                        capabilityFromFGS |= PROCESS_CAPABILITY_FOREGROUND_CAMERA
                                | PROCESS_CAPABILITY_FOREGROUND_MICROPHONE;
                    }
                }
            }

            ArrayMap<IBinder, ArrayList<ConnectionRecord>> serviceConnections = s.getConnections();
            for (int conni = serviceConnections.size() - 1;
                    conni >= 0 && (adj > ProcessList.FOREGROUND_APP_ADJ
                            || schedGroup == ProcessList.SCHED_GROUP_BACKGROUND
                            || procState > PROCESS_STATE_TOP);
                    conni--) {
                ArrayList<ConnectionRecord> clist = serviceConnections.valueAt(conni);
                for (int i = 0;
                        i < clist.size() && (adj > ProcessList.FOREGROUND_APP_ADJ
                                || schedGroup == ProcessList.SCHED_GROUP_BACKGROUND
                                || procState > PROCESS_STATE_TOP);
                        i++) {
                    // XXX should compute this based on the max of
                    // all connected clients.
                    ConnectionRecord cr = clist.get(i);
                    if (cr.binding.client == app) {
                        // Binding to oneself is not interesting.
                        continue;
                    }

                    boolean trackedProcState = false;

                    ProcessRecord client = cr.binding.client;
                    final ProcessStateRecord cstate = client.mState;
                    if (computeClients) {
                        computeOomAdjLSP(client, cachedAdj, topApp, doingAll, now,
                                cycleReEval, true);
                    } else {
                        cstate.setCurRawAdj(cstate.getCurAdj());
                        cstate.setCurRawProcState(cstate.getCurProcState());
                    }

                    int clientAdj = cstate.getCurRawAdj();
                    int clientProcState = cstate.getCurRawProcState();

                    final boolean clientIsSystem = clientProcState < PROCESS_STATE_TOP;

                    boundByNonBgRestricted |= cstate.isCurBoundByNonBgRestrictedApp()
                            || clientProcState <= PROCESS_STATE_BOUND_TOP
                            || (clientProcState == PROCESS_STATE_FOREGROUND_SERVICE
                                    && !cstate.isBackgroundRestricted());

                    if (client.mOptRecord.shouldNotFreeze()) {
                        // Propagate the shouldNotFreeze flag down the bindings.
                        app.mOptRecord.setShouldNotFreeze(true);
                    }

                    if ((cr.flags & Context.BIND_WAIVE_PRIORITY) == 0) {
                        if (cr.hasFlag(Context.BIND_INCLUDE_CAPABILITIES)) {
                            capability |= cstate.getCurCapability();
                        }

                        // If an app has network capability by default
                        // (by having procstate <= BFGS), then the apps it binds to will get
                        // elevated to a high enough procstate anyway to get network unless they
                        // request otherwise, so don't propagate the network capability by default
                        // in this case unless they explicitly request it.
                        if ((cstate.getCurCapability() & PROCESS_CAPABILITY_NETWORK) != 0) {
                            if (clientProcState <= PROCESS_STATE_BOUND_FOREGROUND_SERVICE) {
                                if ((cr.flags & Context.BIND_BYPASS_POWER_NETWORK_RESTRICTIONS)
                                        != 0) {
                                    capability |= PROCESS_CAPABILITY_NETWORK;
                                }
                            } else {
                                capability |= PROCESS_CAPABILITY_NETWORK;
                            }
                        }

                        if (shouldSkipDueToCycle(app, cstate, procState, adj, cycleReEval)) {
                            continue;
                        }

                        if (clientProcState >= PROCESS_STATE_CACHED_ACTIVITY) {
                            // If the other app is cached for any reason, for purposes here
                            // we are going to consider it empty.  The specific cached state
                            // doesn't propagate except under certain conditions.
                            clientProcState = PROCESS_STATE_CACHED_EMPTY;
                        }
                        String adjType = null;
                        if ((cr.flags&Context.BIND_ALLOW_OOM_MANAGEMENT) != 0) {
                            // Similar to BIND_WAIVE_PRIORITY, keep it unfrozen.
                            if (clientAdj < ProcessList.CACHED_APP_MIN_ADJ) {
                                app.mOptRecord.setShouldNotFreeze(true);
                            }
                            // Not doing bind OOM management, so treat
                            // this guy more like a started service.
                            if (state.hasShownUi() && !state.getCachedIsHomeProcess()) {
                                // If this process has shown some UI, let it immediately
                                // go to the LRU list because it may be pretty heavy with
                                // UI stuff.  We'll tag it with a label just to help
                                // debug and understand what is going on.
                                if (adj > clientAdj) {
                                    adjType = "cch-bound-ui-services";
                                }
                                state.setCached(false);
                                clientAdj = adj;
                                clientProcState = procState;
                            } else {
                                if (now >= (s.lastActivity
                                        + mConstants.MAX_SERVICE_INACTIVITY)) {
                                    // This service has not seen activity within
                                    // recent memory, so allow it to drop to the
                                    // LRU list if there is no other reason to keep
                                    // it around.  We'll also tag it with a label just
                                    // to help debug and undertand what is going on.
                                    if (adj > clientAdj) {
                                        adjType = "cch-bound-services";
                                    }
                                    clientAdj = adj;
                                }
                            }
                        }
                        if (adj > clientAdj) {
                            // If this process has recently shown UI, and
                            // the process that is binding to it is less
                            // important than being visible, then we don't
                            // care about the binding as much as we care
                            // about letting this process get into the LRU
                            // list to be killed and restarted if needed for
                            // memory.
                            if (state.hasShownUi() && !state.getCachedIsHomeProcess()
                                    && clientAdj > ProcessList.PERCEPTIBLE_APP_ADJ) {
                                if (adj >= ProcessList.CACHED_APP_MIN_ADJ) {
                                    adjType = "cch-bound-ui-services";
                                }
                            } else {
                                int newAdj;
                                if ((cr.flags&(Context.BIND_ABOVE_CLIENT
                                        |Context.BIND_IMPORTANT)) != 0) {
                                    if (clientAdj >= ProcessList.PERSISTENT_SERVICE_ADJ) {
                                        newAdj = clientAdj;
                                    } else {
                                        // make this service persistent
                                        newAdj = ProcessList.PERSISTENT_SERVICE_ADJ;
                                        schedGroup = ProcessList.SCHED_GROUP_DEFAULT;
                                        procState = ActivityManager.PROCESS_STATE_PERSISTENT;
                                        cr.trackProcState(procState, mAdjSeq);
                                        trackedProcState = true;
                                    }
                                } else if ((cr.flags & Context.BIND_NOT_PERCEPTIBLE) != 0
                                        && clientAdj <= ProcessList.PERCEPTIBLE_APP_ADJ
                                        && adj >= ProcessList.PERCEPTIBLE_LOW_APP_ADJ) {
                                    newAdj = ProcessList.PERCEPTIBLE_LOW_APP_ADJ;
                                } else if ((cr.flags & Context.BIND_ALMOST_PERCEPTIBLE) != 0
                                        && clientAdj < ProcessList.PERCEPTIBLE_APP_ADJ
                                        && adj >= ProcessList.PERCEPTIBLE_MEDIUM_APP_ADJ) {
                                    newAdj = ProcessList.PERCEPTIBLE_MEDIUM_APP_ADJ;
                                } else if ((cr.flags&Context.BIND_NOT_VISIBLE) != 0
                                        && clientAdj < ProcessList.PERCEPTIBLE_APP_ADJ
                                        && adj >= ProcessList.PERCEPTIBLE_APP_ADJ) {
                                    newAdj = ProcessList.PERCEPTIBLE_APP_ADJ;
                                } else if (clientAdj >= ProcessList.PERCEPTIBLE_APP_ADJ) {
                                    newAdj = clientAdj;
                                } else if (cr.hasFlag(BIND_TREAT_LIKE_VISIBLE_FOREGROUND_SERVICE)
                                        && clientAdj <= ProcessList.VISIBLE_APP_ADJ
                                        && adj > ProcessList.VISIBLE_APP_ADJ) {
                                    newAdj = ProcessList.VISIBLE_APP_ADJ;
                                } else {
                                    if (adj > ProcessList.VISIBLE_APP_ADJ) {
                                        // Is this too limiting for apps bound from TOP?
                                        newAdj = Math.max(clientAdj, ProcessList.VISIBLE_APP_ADJ);
                                    } else {
                                        newAdj = adj;
                                    }
                                }
                                if (!cstate.isCached()) {
                                    state.setCached(false);
                                }
                                if (adj >  newAdj) {
                                    adj = newAdj;
                                    state.setCurRawAdj(adj);
                                    adjType = "service";
                                }
                            }
                        }
                        if ((cr.flags & (Context.BIND_NOT_FOREGROUND
                                | Context.BIND_IMPORTANT_BACKGROUND)) == 0) {
                            // This will treat important bound services identically to
                            // the top app, which may behave differently than generic
                            // foreground work.
                            final int curSchedGroup = cstate.getCurrentSchedulingGroup();
                            if (curSchedGroup > schedGroup) {
                                if ((cr.flags&Context.BIND_IMPORTANT) != 0) {
                                    schedGroup = curSchedGroup;
                                } else {
                                    schedGroup = ProcessList.SCHED_GROUP_DEFAULT;
                                }
                            }
                            if (clientProcState < PROCESS_STATE_TOP) {
                                // Special handling for above-top states (persistent
                                // processes).  These should not bring the current process
                                // into the top state, since they are not on top.  Instead
                                // give them the best bound state after that.
                                if (cr.hasFlag(BIND_TREAT_LIKE_VISIBLE_FOREGROUND_SERVICE)) {
                                    clientProcState = PROCESS_STATE_FOREGROUND_SERVICE;
                                } else if (cr.hasFlag(Context.BIND_FOREGROUND_SERVICE)) {
                                    clientProcState = PROCESS_STATE_BOUND_FOREGROUND_SERVICE;
                                } else if (mService.mWakefulness.get()
                                        == PowerManagerInternal.WAKEFULNESS_AWAKE
                                        && (cr.flags & Context.BIND_FOREGROUND_SERVICE_WHILE_AWAKE)
                                                != 0) {
                                    clientProcState = PROCESS_STATE_BOUND_FOREGROUND_SERVICE;
                                } else {
                                    clientProcState =
                                            PROCESS_STATE_IMPORTANT_FOREGROUND;
                                }
                            } else if (clientProcState == PROCESS_STATE_TOP) {
                                // Go at most to BOUND_TOP, unless requested to elevate
                                // to client's state.
                                clientProcState = PROCESS_STATE_BOUND_TOP;
                                final boolean enabled = cstate.getCachedCompatChange(
                                        CACHED_COMPAT_CHANGE_PROCESS_CAPABILITY);
                                if (enabled) {
                                    if (cr.hasFlag(Context.BIND_INCLUDE_CAPABILITIES)) {
                                        // TOP process passes all capabilities to the service.
                                        capability |= cstate.getCurCapability();
                                    } else {
                                        // TOP process passes no capability to the service.
                                    }
                                } else {
                                    // TOP process passes all capabilities to the service.
                                    capability |= cstate.getCurCapability();
                                }
                            }
                        } else if ((cr.flags & Context.BIND_IMPORTANT_BACKGROUND) == 0) {
                            if (clientProcState <
                                    PROCESS_STATE_TRANSIENT_BACKGROUND) {
                                clientProcState =
                                        PROCESS_STATE_TRANSIENT_BACKGROUND;
                            }
                        } else {
                            if (clientProcState <
                                    PROCESS_STATE_IMPORTANT_BACKGROUND) {
                                clientProcState =
                                        PROCESS_STATE_IMPORTANT_BACKGROUND;
                            }
                        }

                        if (schedGroup < ProcessList.SCHED_GROUP_TOP_APP
                                && (cr.flags & Context.BIND_SCHEDULE_LIKE_TOP_APP) != 0
                                && clientIsSystem) {
                            schedGroup = ProcessList.SCHED_GROUP_TOP_APP;
                            scheduleLikeTopApp = true;
                        }

                        if (!trackedProcState) {
                            cr.trackProcState(clientProcState, mAdjSeq);
                        }

                        if (procState > clientProcState) {
                            procState = clientProcState;
                            state.setCurRawProcState(procState);
                            if (adjType == null) {
                                adjType = "service";
                            }
                        }
                        if (procState < PROCESS_STATE_IMPORTANT_BACKGROUND
                                && (cr.flags & Context.BIND_SHOWING_UI) != 0) {
                            app.setPendingUiClean(true);
                        }
                        if (adjType != null) {
                            state.setAdjType(adjType);
                            state.setAdjTypeCode(ActivityManager.RunningAppProcessInfo
                                    .REASON_SERVICE_IN_USE);
                            state.setAdjSource(cr.binding.client);
                            state.setAdjSourceProcState(clientProcState);
                            state.setAdjTarget(s.instanceName);
                           ..
                        }
                    } else { // BIND_WAIVE_PRIORITY == true
                        // BIND_WAIVE_PRIORITY bindings are special when it comes to the
                        // freezer. Processes bound via WPRI are expected to be running,
                        // but they are not promoted in the LRU list to keep them out of
                        // cached. As a result, they can freeze based on oom_adj alone.
                        // Normally, bindToDeath would fire when a cached app would die
                        // in the background, but nothing will fire when a running process
                        // pings a frozen process. Accordingly, any cached app that is
                        // bound by an unfrozen app via a WPRI binding has to remain
                        // unfrozen.
                        if (clientAdj < ProcessList.CACHED_APP_MIN_ADJ) {
                            app.mOptRecord.setShouldNotFreeze(true);
                        }
                    }
                    if ((cr.flags&Context.BIND_TREAT_LIKE_ACTIVITY) != 0) {
                        psr.setTreatLikeActivity(true);
                    }
                    final ActivityServiceConnectionsHolder a = cr.activity;
                    if ((cr.flags&Context.BIND_ADJUST_WITH_ACTIVITY) != 0) {
                        if (a != null && adj > ProcessList.FOREGROUND_APP_ADJ
                                && a.isActivityVisible()) {
                            adj = ProcessList.FOREGROUND_APP_ADJ;
                            state.setCurRawAdj(adj);
                            if ((cr.flags&Context.BIND_NOT_FOREGROUND) == 0) {
                                if ((cr.flags&Context.BIND_IMPORTANT) != 0) {
                                    schedGroup = ProcessList.SCHED_GROUP_TOP_APP_BOUND;
                                } else {
                                    schedGroup = ProcessList.SCHED_GROUP_DEFAULT;
                                }
                            }
                            state.setCached(false);
                            state.setAdjType("service");
                            state.setAdjTypeCode(ActivityManager.RunningAppProcessInfo
                                    .REASON_SERVICE_IN_USE);
                            state.setAdjSource(a);
                            state.setAdjSourceProcState(procState);
                            state.setAdjTarget(s.instanceName);
                           ...
                        }
                    }
                }
            }
        }
```

ContentProvider情况
```
 final ProcessProviderRecord ppr = app.mProviders;
 //当adj>0 或 schedGroup为后台线程组 或procState>PROCESS_STATE_TOP时
        for (int provi = ppr.numberOfProviders() - 1;
                provi >= 0 && (adj > ProcessList.FOREGROUND_APP_ADJ
                        || schedGroup == ProcessList.SCHED_GROUP_BACKGROUND
                        || procState > PROCESS_STATE_TOP);
                provi--) {
            ContentProviderRecord cpr = ppr.getProviderAt(provi);
            for (int i = cpr.connections.size() - 1;
                    i >= 0 && (adj > ProcessList.FOREGROUND_APP_ADJ
                            || schedGroup == ProcessList.SCHED_GROUP_BACKGROUND
                            || procState > PROCESS_STATE_TOP);
                    i--) {
                ContentProviderConnection conn = cpr.connections.get(i);
                ProcessRecord client = conn.client;
                final ProcessStateRecord cstate = client.mState;
                // 当client与当前app同一个进程，则continue;
                if (client == app) {
                    // Being our own client is not interesting.
                    continue;
                }
                // 计算client进程的adj
                if (computeClients) {
                    computeOomAdjLSP(client, cachedAdj, topApp, doingAll, now, cycleReEval, true);
                } else {
                    cstate.setCurRawAdj(cstate.getCurAdj());
                    cstate.setCurRawProcState(cstate.getCurProcState());
                }

                if (shouldSkipDueToCycle(app, cstate, procState, adj, cycleReEval)) {
                    continue;
                }

                int clientAdj = cstate.getCurRawAdj();
                int clientProcState = cstate.getCurRawProcState();
                //当client进程procState >=PROCESS_STATE_CACHED_ACTIVITY，则设置成procState =PROCESS_STATE_CACHED_EMPTY 
                if (clientProcState >= PROCESS_STATE_CACHED_ACTIVITY) {
                    // If the other app is cached for any reason, for purposes here
                    // we are going to consider it empty.
                    clientProcState = PROCESS_STATE_CACHED_EMPTY;
                }
                if (client.mOptRecord.shouldNotFreeze()) {
                    // Propagate the shouldNotFreeze flag down the bindings.
                    app.mOptRecord.setShouldNotFreeze(true);
                }

                boundByNonBgRestricted |= cstate.isCurBoundByNonBgRestrictedApp()
                        || clientProcState <= PROCESS_STATE_BOUND_TOP
                        || (clientProcState == PROCESS_STATE_FOREGROUND_SERVICE
                                && !cstate.isBackgroundRestricted());

                String adjType = null;
                if (adj > clientAdj) {
                    if (state.hasShownUi() && !state.getCachedIsHomeProcess()
                            && clientAdj > ProcessList.PERCEPTIBLE_APP_ADJ) {
                        adjType = "cch-ui-provider";
                    } else {
                         //没有ui展示，则保证adj >=0
                        adj = clientAdj > ProcessList.FOREGROUND_APP_ADJ
                                ? clientAdj : ProcessList.FOREGROUND_APP_ADJ;
                        state.setCurRawAdj(adj);
                        adjType = "provider";
                    }
                    state.setCached(state.isCached() & cstate.isCached());
                }

                if (clientProcState <= PROCESS_STATE_FOREGROUND_SERVICE) {
                    if (adjType == null) {
                        adjType = "provider";
                    }
                    if (clientProcState == PROCESS_STATE_TOP) {
                      //当client进程状态为前台时，则设置procState=PROCESS_STATE_BOUND_TOP
                        clientProcState = PROCESS_STATE_BOUND_TOP;
                    } else {
                       //当client进程状态 < PROCESS_STATE_TOP时，则clientProcState=PROCESS_STATE_BOUND_FOREGROUND_SERVICE；
                        clientProcState = PROCESS_STATE_BOUND_FOREGROUND_SERVICE;
                    }
                }

                conn.trackProcState(clientProcState, mAdjSeq);
                //procState 比client进程值更大时，则取client端的状态值
                if (procState > clientProcState) {
                    procState = clientProcState;
                    state.setCurRawProcState(procState);
                }
                ...
            }
            // If the provider has external (non-framework) process
            // dependencies, ensure that its adjustment is at least
            // FOREGROUND_APP_ADJ.
            //当contentprovider存在外部进程依赖(非framework)时
            if (cpr.hasExternalProcessHandles()) {
               //设置adj =0, procState=PROCESS_STATE_IMPORTANT_FOREGROUND
                if (adj > ProcessList.FOREGROUND_APP_ADJ) {
                    adj = ProcessList.FOREGROUND_APP_ADJ;
                    state.setCurRawAdj(adj);                    
                    state.setAdjType("ext-provider");
                    ..
                }
                if (procState > PROCESS_STATE_IMPORTANT_FOREGROUND) {
                    procState = PROCESS_STATE_IMPORTANT_FOREGROUND;
                    state.setCurRawProcState(procState);
                   ..
                }
            }
        }
        //最近的provider，adj=700,procState=PROCESS_STATE_LAST_ACTIVITY
        if (ppr.getLastProviderTime() > 0
                && (ppr.getLastProviderTime() + mConstants.CONTENT_PROVIDER_RETAIN_TIME) > now) {
            if (adj > ProcessList.PREVIOUS_APP_ADJ) {
                adj = ProcessList.PREVIOUS_APP_ADJ;
                state.setAdjType("recent-provider");
               ..
            }
            if (procState > PROCESS_STATE_LAST_ACTIVITY) {
                procState = PROCESS_STATE_LAST_ACTIVITY;
                state.setAdjType("recent-provider");
               ..
            }
        }
```
当adj>0 或 schedGroup为后台线程组 或procState>PROCESS_STATE_TOP时，双重循环遍历：
当client与当前app同一个进程，则continue;
当client进程procState >=PROCESS_STATE_CACHED_ACTIVITY，则把client进程设置成procState =PROCESS_STATE_CACHED_EMPTY
没有ui展示，则保证adj >=0
当client进程状态=PROCESS_STATE_TOP前台时，则client进程procState=PROCESS_STATE_BOUND_TOP(空进程)
当client进程状态<PROCESS_STATE_TOP时，则clientProcState=PROCESS_STATE_BOUND_FOREGROUND_SERVICE；
procState 比clientProcState更大时，则取client端的状态值。
当contentprovider存在外部进程依赖(非framework)时，则设置adj =0, procState=PROCESS_STATE_IMPORTANT_FOREGROUND

调整adj
```
//当procState>= PROCESS_STATE_CACHED_EMPTY时，
if (procState >= PROCESS_STATE_CACHED_EMPTY) {
            if (psr.hasClientActivities()) {
               //当进程存在client activity，则设置procState=PROCESS_STATE_CACHED_ACTIVITY_CLIENT；
                // This is a cached process, but with client activities.  Mark it so.
                procState = PROCESS_STATE_CACHED_ACTIVITY_CLIENT;
                state.setAdjType("cch-client-act");
            } else if (psr.isTreatedLikeActivity()) {
               //当进程可以像activity一样对待时，则设置procState=PROCESS_STATE_CACHED_ACTIVITY；
                // This is a cached process, but somebody wants us to treat it like it has
                // an activity, okay!
                procState = PROCESS_STATE_CACHED_ACTIVITY;
                state.setAdjType("cch-as-act");
            }
        }
         //当adj = SERVICE_ADJ(-700)时 
        if (adj == ProcessList.SERVICE_ADJ) {
            if (doingAll && !cycleReEval) {
                //当A类Service个数 > service/3时，则加入到B类Service
                state.setServiceB(mNewNumAServiceProcs > (mNumServiceProcs / 3));
                mNewNumServiceProcs++;
                if (!state.isServiceB()) {
                   //当对于低RAM设备，则把该service直接放入B类Service
                    // This service isn't far enough down on the LRU list to
                    // normally be a B service, but if we are low on RAM and it
                    // is large we want to force it down since we would prefer to
                    // keep launcher over it.
                    if (!mService.mAppProfiler.isLastMemoryLevelNormal()
                            && app.mProfile.getLastPss()
                            >= mProcessList.getCachedRestoreThresholdKb()) {
                        state.setServiceHighRam(true);
                        state.setServiceB(true);
                    } else {
                        mNewNumAServiceProcs++;
                    }
                } else {
                    state.setServiceHighRam(false);
                }
            }
            /调整adj=800
            if (state.isServiceB()) {
                adj = ProcessList.SERVICE_B_ADJ;
            }
        }
        //将计算得到的adj赋给curRawAdj
        state.setCurRawAdj(adj);
        //对于hasAboveClient=true，则降低该进程adj
        adj = psr.modifyRawOomAdj(adj);
        //当adj大小上限为maxAdj
        if (adj > state.getMaxAdj()) {
            adj = state.getMaxAdj();
            if (adj <= ProcessList.PERCEPTIBLE_LOW_APP_ADJ) {
                schedGroup = ProcessList.SCHED_GROUP_DEFAULT;
            }
        }
        ...
         //返回进程的curRawAdj
        // if curAdj or curProcState improved, then this process was promoted
        return state.getCurAdj() < prevAppAdj || state.getCurProcState() < prevProcState
                || state.getCurCapability() != prevCapability;
    }
```
frameworks/base/services/core/java/com/android/server/am/ProcessServiceRecord.java
```
int modifyRawOomAdj(int adj) {
        if (mHasAboveClient) {
            // If this process has bound to any services with BIND_ABOVE_CLIENT,
            // then we need to drop its adjustment to be lower than the service's
            // in order to honor the request.  We want to drop it by one adjustment
            // level...  but there is special meaning applied to various levels so
            // we will skip some of them.
            if (adj < ProcessList.FOREGROUND_APP_ADJ) {
                // System process will not get dropped, ever
            } else if (adj < ProcessList.VISIBLE_APP_ADJ) {
                adj = ProcessList.VISIBLE_APP_ADJ;
            } else if (adj < ProcessList.PERCEPTIBLE_APP_ADJ) {
                adj = ProcessList.PERCEPTIBLE_APP_ADJ;
            } else if (adj < ProcessList.PERCEPTIBLE_LOW_APP_ADJ) {
                adj = ProcessList.PERCEPTIBLE_LOW_APP_ADJ;
            } else if (adj < ProcessList.CACHED_APP_MIN_ADJ) {
                adj = ProcessList.CACHED_APP_MIN_ADJ;
            } else if (adj < ProcessList.CACHED_APP_MAX_ADJ) {
                adj++;
            }
        }
        return adj;
    }
```
主要工作：计算进程的adj和procState
进程为空的情况
maxAdj<=0
计算各种状态下(当前显示activity, 症结接收的广播/service等)的adj和procState
非前台activity的情况
adj > 2的情况
HeavyWeightProces情况
HomeProcess情况
PreviousProcess情况
备份进程情况
Service情况
ContentProvider情况
调整adj

原则1：取大优先，Android给进程优先级评级策略是选择最高的优先级，例如：当进程既有后台Service，也有前台Activity时，
该进程的优先级则会评定为前台进程(adj=0)，而非服务进程(adj=5).

原则2：一个进程的级别可能会因其他进程对它的依赖而有所提高，即服务于另一进程的进程其级别永远不会低于其所服务的进程。
例如，如果进程A的ContentProvider为进程B的客户端提供服务，或者如果进程A中的Service 绑定到进程B的组件，则进程A的重要性至少与进程B相等

``` 
  //更新
  private boolean updateAndTrimProcessLSP(final long now, final long nowElapsed,
            final long oldTime, final ActiveUids activeUids) {
        ArrayList<ProcessRecord> lruList = mProcessList.getLruProcessesLOSP();
        final int numLru = lruList.size();
        ...
        final int emptyProcessLimit = doKillExcessiveProcesses
                ? mConstants.CUR_MAX_EMPTY_PROCESSES : Integer.MAX_VALUE;
        final int cachedProcessLimit = doKillExcessiveProcesses
                ? (mConstants.CUR_MAX_CACHED_PROCESSES - emptyProcessLimit) : Integer.MAX_VALUE;
        int lastCachedGroup = 0;
        int lastCachedGroupUid = 0;
        int numCached = 0;
        int numCachedExtraGroup = 0;
        int numEmpty = 0;
        int numTrimming = 0;

        for (int i = numLru - 1; i >= 0; i--) {
            ProcessRecord app = lruList.get(i);
            final ProcessStateRecord state = app.mState;
            if (!app.isKilledByAm() && app.getThread() != null) {
                // We don't need to apply the update for the process which didn't get computed
                if (state.getCompletedAdjSeq() == mAdjSeq) {
                    //应用adj
                    applyOomAdjLSP(app, true, now, nowElapsed);
                }

                final ProcessServiceRecord psr = app.mServices;
                //根据当前进程procState状态来决策
                switch (state.getCurProcState()) {
                    case PROCESS_STATE_CACHED_ACTIVITY:
                    case ActivityManager.PROCESS_STATE_CACHED_ACTIVITY_CLIENT:
                        mNumCachedHiddenProcs++;
                        numCached++;
                        ...
                        // 当cached进程超过上限(cachedProcessLimit)，则杀掉该进程 app为ProcessRecord
                        if ((numCached - numCachedExtraGroup) > cachedProcessLimit) {
                            app.killLocked("cached #" + numCached,
                                    "too many cached",
                                    ApplicationExitInfo.REASON_OTHER,
                                    ApplicationExitInfo.SUBREASON_TOO_MANY_CACHED,
                                    true);
                        }
                        break;
                    case PROCESS_STATE_CACHED_EMPTY:
                    // 当空进程超过上限(CUR_TRIM_EMPTY_PROCESSES)，且空闲时间超过30分钟，则杀掉该进程
                    //oldTime为当前时间-最大空闲存活
                        if (numEmpty > mConstants.CUR_TRIM_EMPTY_PROCESSES
                                && app.getLastActivityTime() < oldTime) {
                            app.killLocked("empty for " + ((now
                                    - app.getLastActivityTime()) / 1000) + "s",
                                    "empty for too long",
                                    ApplicationExitInfo.REASON_OTHER,
                                    ApplicationExitInfo.SUBREASON_TRIM_EMPTY,
                                    true);
                        } else {
                        // 当空进程超过上限(emptyProcessLimit)，则杀掉该进程
                            numEmpty++;
                            if (numEmpty > emptyProcessLimit) {
                                app.killLocked("empty #" + numEmpty,
                                        "too many empty",
                                        ApplicationExitInfo.REASON_OTHER,
                                        ApplicationExitInfo.SUBREASON_TOO_MANY_EMPTY,
                                        true);
                            }
                        }
                        break;
                    default:
                        mNumNonCachedProcs++;
                        break;
                }

                if (app.isolated && psr.numberOfRunningServices() <= 0
                        && app.getIsolatedEntryPoint() == null) {
                   //没有services运行的孤立进程，则直接杀掉
                    app.killLocked("isolated not needed", ApplicationExitInfo.REASON_OTHER,
                            ApplicationExitInfo.SUBREASON_ISOLATED_NOT_NEEDED, true);
                } else {
                    // Keeping this process, update its uid.
                    updateAppUidRecLSP(app);
                }

                if (state.getCurProcState() >= ActivityManager.PROCESS_STATE_HOME
                        && !app.isKilledByAm()) {
                    numTrimming++;
                }
            }
        }
        //更新是否触发low momery   todo
        return mService.mAppProfiler.updateLowMemStateLSP(numCached, numEmpty, numTrimming);
    }          
```


applyOomAdjLSP
```
 private boolean applyOomAdjLSP(ProcessRecord app, boolean doingAll, long now,
            long nowElapsed, String oomAdjReson) {
        boolean success = true;
        final ProcessStateRecord state = app.mState;
        final UidRecord uidRec = app.getUidRecord();
        if (state.getCurRawAdj() != state.getSetRawAdj()) {
            state.setSetRawAdj(state.getCurRawAdj());
        }
        int changes = 0;
       ....

        if (state.getCurAdj() != state.getSetAdj()) {
           //将adj值 发送给lmkd守护进程
            ProcessList.setOomAdj(app.getPid(), app.uid, state.getCurAdj());
            ...
            state.setSetAdj(state.getCurAdj());
            if (uidRec != null) {
                uidRec.noteProcAdjChanged();
            }
            state.setVerifiedAdj(ProcessList.INVALID_ADJ);
        }

        final int curSchedGroup = state.getCurrentSchedulingGroup();
        //情况为： waitingToKill
        if (state.getSetSchedGroup() != curSchedGroup) {
            int oldSchedGroup = state.getSetSchedGroup();
            state.setSetSchedGroup(curSchedGroup);
            ....
            if (app.getWaitingToKill() != null && app.mReceivers.numberOfCurReceivers() == 0
                    && state.getSetSchedGroup() == ProcessList.SCHED_GROUP_BACKGROUND) {
                //杀进程，并设置applyOomAdjLocked过程失败    
                app.killLocked(app.getWaitingToKill(), ApplicationExitInfo.REASON_USER_REQUESTED,
                        ApplicationExitInfo.SUBREASON_REMOVE_TASK, true);
                success = false;
            } else {
                int processGroup;
                switch (curSchedGroup) {
                    case ProcessList.SCHED_GROUP_BACKGROUND:
                        processGroup = THREAD_GROUP_BACKGROUND;
                        break;
                    case ProcessList.SCHED_GROUP_TOP_APP:
                    case ProcessList.SCHED_GROUP_TOP_APP_BOUND:
                        processGroup = THREAD_GROUP_TOP_APP;
                        break;
                    case ProcessList.SCHED_GROUP_RESTRICTED:
                        processGroup = THREAD_GROUP_RESTRICTED;
                        break;
                    default:
                        processGroup = THREAD_GROUP_DEFAULT;
                        break;
                }
                 //设置进程组信息
                mProcessGroupHandler.sendMessage(mProcessGroupHandler.obtainMessage(
                        0 /* unused */, app.getPid(), processGroup, app.processName));
                try {
                    final int renderThreadTid = app.getRenderThreadTid();
                    if (curSchedGroup == ProcessList.SCHED_GROUP_TOP_APP) {
                        // do nothing if we already switched to RT
                        if (oldSchedGroup != ProcessList.SCHED_GROUP_TOP_APP) {
                            app.getWindowProcessController().onTopProcChanged();
                            if (mService.mUseFifoUiScheduling) {
                                // Switch UI pipeline for app to SCHED_FIFO
                                state.setSavedPriority(Process.getThreadPriority(app.getPid()));
                                mService.scheduleAsFifoPriority(app.getPid(), true);
                                if (renderThreadTid != 0) {
                                    mService.scheduleAsFifoPriority(renderThreadTid,
                                            /* suppressLogs */true);
                                    ...
                                } ...
                            } else {
                                //调整线程优先级
                                // Boost priority for top app UI and render threads
                                setThreadPriority(app.getPid(), THREAD_PRIORITY_TOP_APP_BOOST);
                                if (renderThreadTid != 0) {
                                    try {
                                        setThreadPriority(renderThreadTid,
                                                THREAD_PRIORITY_TOP_APP_BOOST);
                                    } ...
                                }
                            }
                        }
                    } else if (oldSchedGroup == ProcessList.SCHED_GROUP_TOP_APP &&
                            curSchedGroup != ProcessList.SCHED_GROUP_TOP_APP) {
                        app.getWindowProcessController().onTopProcChanged();
                        if (mService.mUseFifoUiScheduling) {
                            try {
                                // Reset UI pipeline to SCHED_OTHER
                                setThreadScheduler(app.getPid(), SCHED_OTHER, 0);
                                setThreadPriority(app.getPid(), state.getSavedPriority());
                                if (renderThreadTid != 0) {
                                    setThreadScheduler(renderThreadTid,
                                            SCHED_OTHER, 0);
                                }
                            } ....
                        } else {
                            // Reset priority for top app UI and render threads
                            setThreadPriority(app.getPid(), 0);
                        }

                        if (renderThreadTid != 0) {
                            setThreadPriority(renderThreadTid, THREAD_PRIORITY_DISPLAY);
                        }
                    }
                } ...
            }
        }
        if (state.hasRepForegroundActivities() != state.hasForegroundActivities()) {
            state.setRepForegroundActivities(state.hasForegroundActivities());
            changes |= ActivityManagerService.ProcessChangeItem.CHANGE_ACTIVITIES;
        }

        updateAppFreezeStateLSP(app, oomAdjReson);

        if (state.getReportedProcState() != state.getCurProcState()) {
            state.setReportedProcState(state.getCurProcState());
            if (app.getThread() != null) {
                try {
                   ... //设置进程状态
                    app.getThread().setProcessState(state.getReportedProcState());
                } catch (RemoteException e) {
                }
            }
        }    
      ...
        return success;
    }
```

frameworks/base/services/core/java/com/android/server/am/ProcessList.java
```
 public static void setOomAdj(int pid, int uid, int amt) {
        ...
        long start = SystemClock.elapsedRealtime();
        ByteBuffer buf = ByteBuffer.allocate(4 * 4);
        buf.putInt(LMK_PROCPRIO);
        buf.putInt(pid);
        buf.putInt(uid);
        buf.putInt(amt);
        writeLmkd(buf, null);
       ...
    }
 
 private static boolean writeLmkd(ByteBuffer buf, ByteBuffer repl) {
        if (!sLmkdConnection.isConnected()) {  //等待lmkd的连接，重试    
            // try to connect immediately and then keep retrying
            sKillHandler.sendMessage(
                    sKillHandler.obtainMessage(KillHandler.LMKD_RECONNECT_MSG));

            // wait for connection retrying 3 times (up to 3 seconds)
            if (!sLmkdConnection.waitForConnection(3 * LMKD_RECONNECT_DELAY_MS)) {
                return false;
            }
        }

        return sLmkdConnection.exchange(buf, repl);
    }   
```

进程杀掉  后续查看杀进程实现
ProcessRecord.killLocked
frameworks/base/services/core/java/com/android/server/am/ProcessRecord.java
```
 void killLocked(String reason, String description, @Reason int reasonCode,
            @SubReason int subReason, boolean noisy) {
        if (!mKilledByAm) {
           ..
            if (mPid > 0) {
                mService.mProcessList.noteAppKill(this, reasonCode, subReason, description);
                ...
                Process.killProcessQuiet(mPid);
                ProcessList.killProcessGroup(uid, mPid);
            } else {
                mPendingStart = false;
            }
            ...
        }
    }
```