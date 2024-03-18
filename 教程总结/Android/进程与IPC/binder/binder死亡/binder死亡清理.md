


当系统内存不足时,会触发lmk杀进程; 以及系统本身通过AMS也会控制系统中各个状态的进程个数上限. 当进程真正的被杀死之后,
通过binder死亡回调后系统需要清理四大组件和进程信息.

死亡监听
当进程死亡后, 系统是如何知道的呢, 答案就在进程创建之后,会调用AMS.attachApplicationLocked()
https://cs.android.com/android/platform/superproject/+/android-12.0.0_r34:frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java
```
 private boolean attachApplicationLocked(@NonNull IApplicationThread thread,
            int pid, int callingUid, long startSeq) {
    ...
    final String processName = app.processName;
    AppDeathRecipient adr = new AppDeathRecipient(
            app, pid, thread);
    thread.asBinder().linkToDeath(adr, 0);
    app.setDeathRecipient(adr);
   ....        
 }
 
 private final class AppDeathRecipient implements IBinder.DeathRecipient {
        ...
        @Override
        public void binderDied() {          
            synchronized(ActivityManagerService.this) {
                appDiedLocked(mApp, mPid, mAppThread, true, null);
            }
        }
    }
```
recipient  接受者;领受者;承受者
在这个过程中,会创建AppDeathRecipient死亡通告对象,通过binder机制绑定, 当新创建的应用进程死亡后,便会回调binderDied()方法. 
 其中关于binder死亡回调,是指binder server端挂了之后通过binder driver会通知binder client端. 那么对于进程死亡过程, 
binder server端是指应用进程的ApplicationThread, binder client端是指system_server进程中的ApplicationThreadProxy对象.  //todo
接下来从binderDied()方法说起.

binderDied调用后，继续appDiedLocked
```
    @GuardedBy("this")
    final void appDiedLocked(ProcessRecord app, int pid, IApplicationThread thread,
            boolean fromBinderDied, String reason) { //fromBinderDied 此时为true
        final ProcessRecord curProc;
        synchronized (mPidsSelfLocked) {
            curProc = mPidsSelfLocked.get(pid);
        }
        ////检查pid与app是否匹配,不匹配则直接返回
        if (curProc != app) {
            if (!fromBinderDied || !mProcessList.handleDyingAppDeathLocked(app, pid)) {
                Slog.w(TAG, "Spurious death for " + app + ", curProc for " + pid + ": " + curProc);
            }
            return;
        }

        mBatteryStatsService.noteProcessDied(app.info.uid, pid);

        if (!app.isKilled()) {
        //当进程还没有设置已被杀的标记,则进入该分支杀掉相应进程
            if (!fromBinderDied) {//非binder死亡回调,而是上层直接调用该方法,则进入该分支
                killProcessQuiet(pid);
                mProcessList.noteAppKill(app, ApplicationExitInfo.REASON_OTHER,
                        ApplicationExitInfo.SUBREASON_UNKNOWN, reason);
            }
            ProcessList.killProcessGroup(app.uid, pid);
            synchronized (mProcLock) {
                app.setKilled(true);
            }
        }

        // Clean up already done if the process has been re-started.
        IApplicationThread appThread;
        final int setAdj = app.mState.getSetAdj();
        final int setProcState = app.mState.getSetProcState();
        if (app.getPid() == pid && (appThread = app.getThread()) != null
                && appThread.asBinder() == thread.asBinder()) {
            boolean doLowMem = app.getActiveInstrumentation() == null;
            boolean doOomAdj = doLowMem;
            if (!app.isKilledByAm()) {
               //当app不是由am所杀,则往往都是lmk所杀
                reportUidInfoMessageLocked(TAG,
                        "Process " + app.processName + " (pid " + pid + ") has died: "
                        + ProcessList.makeOomAdjString(setAdj, true) + " "
                        + ProcessList.makeProcStateString(setProcState), app.info.uid);
                //设置低内存        
                mAppProfiler.setAllowLowerMemLevelLocked(true);
            } else {
                // Note that we always want to do oom adj to update our state with the
                // new number of procs.
                mAppProfiler.setAllowLowerMemLevelLocked(false);
                doLowMem = false;
            }
            EventLogTags.writeAmProcDied(app.userId, pid, app.processName, setAdj, setProcState);
            ...
            //从ams移除该进程以及connections
            handleAppDiedLocked(app, pid, false, true, fromBinderDied);

            if (doOomAdj) { //更新各个进程的adj
                updateOomAdjLocked(OomAdjuster.OOM_ADJ_REASON_PROCESS_END);
            }
            if (doLowMem) {//只有当mLruProcesses中所有进程都运行在前台,才报告内存信息
                mAppProfiler.doLowMemReportIfNeededLocked(app);
            }
        } else if (app.getPid() != pid) { //新的进程
            // A new process has already been started.
            reportUidInfoMessageLocked(TAG,
                    "Process " + app.processName + " (pid " + pid
                            + ") has died and restarted (pid " + app.getPid() + ").", app.info.uid);

            EventLogTags.writeAmProcDied(app.userId, app.getPid(), app.processName,
                    setAdj, setProcState);
        } ...

        // On the device which doesn't have Cgroup, log LmkStateChanged which is used as a signal
        // for pulling memory stats of other running processes when this process died.
        if (!hasMemcg()) {
            FrameworkStatsLog.write(FrameworkStatsLog.APP_DIED, SystemClock.elapsedRealtime());
        }
    }    
```

handleAppDiedLocked
```
 final void handleAppDiedLocked(ProcessRecord app, int pid,
            boolean restarting, boolean allowRestart, boolean fromBinderDied) {//后面三个参数false true ture
         //清理应用程序service, BroadcastReceiver, ContentProvider相关信息   
        boolean kept = cleanUpApplicationRecordLocked(app, pid, restarting, allowRestart, -1,
                false /*replacingPid*/, fromBinderDied);
                
        //当应用不需要保持(即不需要重启则不保持), 且不处于正在启动中的状态,
       //则从mLruProcesses移除该应用,以及告诉lmk该pid被移除的信息        
        if (!kept && !restarting) {
            removeLruProcessLocked(app);
            if (pid > 0) {
                ProcessList.remove(pid);
            }
        }

        mAppProfiler.onAppDiedLocked(app);  //todo 

        mAtmInternal.handleAppDied(app.getWindowProcessController(), restarting, () -> {
            Slog.w(TAG, "Crash of app " + app.processName
                    + " running instrumentation " + app.getActiveInstrumentation().mClass);
            Bundle info = new Bundle();
            info.putString("shortMsg", "Process crashed.");
            finishInstrumentationLocked(app, Activity.RESULT_CANCELED, info);
        });
    }
```
cleanUpApplicationRecordLocked
```
    final boolean cleanUpApplicationRecordLocked(ProcessRecord app, int pid,
            boolean restarting, boolean allowRestart, int index, boolean replacingPid,
            boolean fromBinderDied) {
        boolean restart;
        synchronized (mProcLock) {
            if (index >= 0) {
                removeLruProcessLocked(app);
                ProcessList.remove(pid);
            }

            // We don't want to unlinkDeathRecipient immediately, if it's not called from binder
            // and it's not isolated, as we'd need the signal to bookkeeping the dying process list.
            restart = app.onCleanupApplicationRecordLSP(mProcessStats, allowRestart,
                    fromBinderDied || app.isolated /* unlinkDeath */);

            // Cancel pending frozen task if there is any.
            mOomAdjuster.mCachedAppOptimizer.unscheduleFreezeAppLSP(app);
        }
        mAppProfiler.onCleanupApplicationRecordLocked(app);
        skipCurrentReceiverLocked(app);
        updateProcessForegroundLocked(app, false, 0, false);  //将app移除前台进程
        mServices.killServicesLocked(app, allowRestart);//清理service信息
        mPhantomProcessList.onAppDied(pid);

        // If the app is undergoing backup, tell the backup manager about it
        final BackupRecord backupTarget = mBackupTargets.get(app.userId);
        if (backupTarget != null && pid == backupTarget.app.getPid()) {
            if (DEBUG_BACKUP || DEBUG_CLEANUP) Slog.d(TAG_CLEANUP, "App "
                    + backupTarget.appInfo + " died during backup");
            mHandler.post(new Runnable() {
                @Override
                public void run(){
                    try {
                        IBackupManager bm = IBackupManager.Stub.asInterface(
                                ServiceManager.getService(Context.BACKUP_SERVICE));
                        bm.agentDisconnectedForUser(app.userId, app.info.packageName);
                    } catch (RemoteException e) {
                        // can't happen; backup manager is local
                    }
                }
            });
        }

        mProcessList.scheduleDispatchProcessDiedLocked(pid, app.info.uid);

        // If this is a preceding instance of another process instance
        allowRestart = mProcessList.handlePrecedingAppDiedLocked(app);

        // If the caller is restarting this app, then leave it in its
        // current lists and let the caller take care of it.
        if (restarting) {
            return false;
        }

        if (!app.isPersistent() || app.isolated) {
            ....
            if (!replacingPid) {
                mProcessList.removeProcessNameLocked(app.processName, app.uid, app);
            }
            mAtmInternal.clearHeavyWeightProcessIfEquals(app.getWindowProcessController());
        } else if (!app.isRemoved()) {
            // This app is persistent, so we need to keep its record around.
            // If it is not already on the pending app list, add it there
            // and start a new process for it.
            if (mPersistentStartingProcesses.indexOf(app) < 0) {
                mPersistentStartingProcesses.add(app);
                restart = true;
            }
        }
        if ((DEBUG_PROCESSES || DEBUG_CLEANUP) && mProcessesOnHold.contains(app)) Slog.v(
                TAG_CLEANUP, "Clean-up removing on hold: " + app);
        mProcessesOnHold.remove(app);

        mAtmInternal.onCleanUpApplicationRecord(app.getWindowProcessController());
        mProcessList.noteProcessDiedLocked(app);

        if (restart && allowRestart && !app.isolated) {
            // We have components that still need to be running in the
            // process, so re-launch it.
            if (index < 0) {
                ProcessList.remove(pid);
            }

            // Remove provider publish timeout because we will start a new timeout when the
            // restarted process is attaching (if the process contains launching providers).
            mHandler.removeMessages(CONTENT_PROVIDER_PUBLISH_TIMEOUT_MSG, app);

            mProcessList.addProcessNameLocked(app);
            app.setPendingStart(false);
            mProcessList.startProcessLocked(app, new HostingRecord("restart", app.processName),
                    ZYGOTE_POLICY_FLAG_EMPTY);
            return true;
        } else if (pid > 0 && pid != MY_PID) {
            // Goodbye!
            removePidLocked(pid, app);
            mHandler.removeMessages(PROC_START_TIMEOUT_MSG, app);
            mBatteryStatsService.noteProcessFinish(app.processName, app.info.uid);
            if (app.isolated) {
                mBatteryStatsService.removeIsolatedUid(app.uid, app.info.uid);
            }
            app.setPid(0);
        }
        return false;
    }
```