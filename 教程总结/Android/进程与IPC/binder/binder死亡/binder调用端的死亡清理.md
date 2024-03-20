


当系统内存不足时,会触发lmk杀进程; 以及系统本身通过AMS也会控制系统中各个状态的进程个数上限. 当进程真正的被杀死之后,
通过binder死亡回调后系统需要清理相关进程的四大组件和进程信息.


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

        mAppProfiler.onAppDiedLocked(app);  //清空AppPofiler的mProfileData
        
        //activity管理栈移除
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
        mAppProfiler.onCleanupApplicationRecordLocked(app); //AppProiler清理相关app
        skipCurrentReceiverLocked(app); //清理广播的receiver
        updateProcessForegroundLocked(app, false, 0, false);  //将app移除前台进程
        mServices.killServicesLocked(app, allowRestart);//清理service信息
        mPhantomProcessList.onAppDied(pid); //PhantomProcessList关闭InputStream和fd

        // If the app is undergoing backup, tell the backup manager about it
        //停止app备份
        final BackupRecord backupTarget = mBackupTargets.get(app.userId);
        if (backupTarget != null && pid == backupTarget.app.getPid()) {
           ....
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

        mProcessList.scheduleDispatchProcessDiedLocked(pid, app.info.uid); //根据pid移除

        // If this is a preceding instance of another process instance
        allowRestart = mProcessList.handlePrecedingAppDiedLocked(app);

        // If the caller is restarting this app, then leave it in its
        // current lists and let the caller take care of it.
        if (restarting) {
            return false;
        }

        if (!app.isPersistent() || app.isolated) {
            ....
            if (!replacingPid) {//根据name移除record
                mProcessList.removeProcessNameLocked(app.processName, app.uid, app);
            }
            //取消Notification
            mAtmInternal.clearHeavyWeightProcessIfEquals(app.getWindowProcessController());
        } else if (!app.isRemoved()) {
            ...
            if (mPersistentStartingProcesses.indexOf(app) < 0) {
                mPersistentStartingProcesses.add(app);
                restart = true;
            }
        }
        ...
        mProcessesOnHold.remove(app); //ArrayList<ProcessRecord> mProcessesOnHold

        mAtmInternal.onCleanUpApplicationRecord(app.getWindowProcessController());
        mProcessList.noteProcessDiedLocked(app); //从Watchdog和AppExitInfoTracker中移除

        if (restart && allowRestart && !app.isolated) { //仍有组件需要运行在该进程中，因此重启该进程
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
            // Goodbye!   //移除该进程相关信息
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
https://cs.android.com/android/platform/superproject/+/android-12.0.0_r34:frameworks/base/services/core/java/com/android/server/am/ProcessList.java
ProcessList
```
 void scheduleDispatchProcessDiedLocked(int pid, int uid) {
        synchronized (mProcessChangeLock) {
            for (int i = mPendingProcessChanges.size() - 1; i >= 0; i--) {
                ProcessChangeItem item = mPendingProcessChanges.get(i);
                if (pid > 0 && item.pid == pid) {
                    mPendingProcessChanges.remove(i);
                    mAvailProcessChanges.add(item);
                }
            }
            mService.mUiHandler.obtainMessage(DISPATCH_PROCESS_DIED_UI_MSG, pid, uid,
                    null).sendToTarget(); //AcitivityManagerService调用dispatchProcessDied
        }
    }
    
  void dispatchProcessDied(int pid, int uid) {
        int i = mProcessObservers.beginBroadcast();
        while (i > 0) {
            i--;
            final IProcessObserver observer = mProcessObservers.getBroadcastItem(i);
            if (observer != null) {
                try { //回调给ProcessObserver
                    observer.onProcessDied(pid, uid);
                } catch (RemoteException e) {
                }
            }
        }
        mProcessObservers.finishBroadcast();
    }  
    
   boolean handlePrecedingAppDiedLocked(ProcessRecord app) {
        synchronized (app) {
            if (app.mSuccessor != null) {
                // We don't allow restart with this ProcessRecord now,
                // because we have created a new one already.
                // If it's persistent, add the successor to mPersistentStartingProcesses
                if (app.isPersistent() && !app.isRemoved()) {
                    if (mService.mPersistentStartingProcesses.indexOf(app.mSuccessor) < 0) {
                        mService.mPersistentStartingProcesses.add(app.mSuccessor);
                    }
                }
                // clean up the field so the successor's proc starter could proceed.
                app.mSuccessor.mPredecessor = null;
                app.mSuccessor = null;
                // Notify if anyone is waiting for it.
                app.notifyAll();
                return false;
            }
        }
        return true;
    }
  
  
     ProcessRecord removeProcessNameLocked(final String name, final int uid,
            final ProcessRecord expecting) {
        ProcessRecord old = mProcessNames.get(name, uid);
        final ProcessRecord record = expecting != null ? expecting : old;
        synchronized (mProcLock) {
            // Only actually remove when the currently recorded value matches the
            // record that we expected; if it doesn't match then we raced with a
            // newly created process and we don't want to destroy the new one.
            if ((expecting == null) || (old == expecting)) {
                mProcessNames.remove(name, uid);
            }
            if (record != null) {
                final UidRecord uidRecord = record.getUidRecord();
                if (uidRecord != null) {
                    uidRecord.removeProcess(record);
                    if (uidRecord.getNumOfProcs() == 0) {
                        // No more processes using this uid, tell clients it is gone.
                        ....
                        mService.enqueueUidChangeLocked(uidRecord, -1,
                                UidRecord.CHANGE_GONE);
                        EventLogTags.writeAmUidStopped(uid);
                        mActiveUids.remove(uid);
                        mService.mFgsStartTempAllowList.removeUid(record.info.uid);
                        mService.noteUidProcessState(uid, ActivityManager.PROCESS_STATE_NONEXISTENT,
                                ActivityManager.PROCESS_CAPABILITY_NONE);
                    }
                    record.setUidRecord(null);
                }
            }
        }
        mIsolatedProcesses.remove(uid);
        mGlobalIsolatedUids.freeIsolatedUidLocked(uid);
        // Remove the (expected) ProcessRecord from the app zygote
        if (record != null && record.appZygote) {
            removeProcessFromAppZygoteLocked(record);
        }

        return old;
    } 
    
 
    void noteProcessDiedLocked(final ProcessRecord app) {
        ...
        Watchdog.getInstance().processDied(app.processName, app.getPid());
        if (app.getDeathRecipient() == null) {
            // If we've done unlinkDeathRecipient before calling into this, remove from dying list.
            mDyingProcesses.remove(app.processName, app.uid);
            app.setDyingPid(0);
        }
        mAppExitInfoTracker.scheduleNoteProcessDied(app);
    }
```
frameworks/base/services/core/java/com/android/server/wm/ActivityTaskManagerService.java
```
 public void handleAppDied(WindowProcessController wpc, boolean restarting,
                Runnable finishInstrumentationCallback) {
            synchronized (mGlobalLockWithoutBoost) {
                mTaskSupervisor.beginDeferResume();
                final boolean hasVisibleActivities;
                try {
                    // Remove this application's activities from active lists.
                    hasVisibleActivities = wpc.handleAppDied();
                } finally {
                    mTaskSupervisor.endDeferResume();
                }

                if (!restarting && hasVisibleActivities) {
                    deferWindowLayout();
                    try {
                        if (!mRootWindowContainer.resumeFocusedTasksTopActivities()) {
                            // If there was nothing to resume, and we are not already restarting
                            // this process, but there is a visible activity that is hosted by the
                            // process...then make sure all visible activities are running, taking
                            // care of restarting this process.
                            mRootWindowContainer.ensureActivitiesVisible(null, 0,
                                    !PRESERVE_WINDOWS);
                        }
                    } finally {
                        continueWindowLayout();
                    }
                }
            }
            if (wpc.isInstrumenting()) {
                finishInstrumentationCallback.run();
            }
        }
```
frameworks/base/services/core/java/com/android/server/wm/WindowProcessController.java
```
  boolean handleAppDied() {
        mAtm.mTaskSupervisor.removeHistoryRecords(this);

        boolean hasVisibleActivities = false;
        final boolean hasInactiveActivities =
                mInactiveActivities != null && !mInactiveActivities.isEmpty();
        final ArrayList<ActivityRecord> activities =
                (mHasActivities || hasInactiveActivities) ? new ArrayList<>() : mActivities;
        if (mHasActivities) {
            activities.addAll(mActivities);
        }
        if (hasInactiveActivities) {
            // Make sure that all activities in this process are handled.
            activities.addAll(mInactiveActivities);
        }
        if (isRemoved()) {
            // The package of the died process should be force-stopped, so make its activities as
            // finishing to prevent the process from being started again if the next top (or being
            // visible) activity also resides in the same process. This must be done before removal.
            for (int i = activities.size() - 1; i >= 0; i--) {
                activities.get(i).makeFinishingLocked();
            }
        }
        for (int i = activities.size() - 1; i >= 0; i--) {
            final ActivityRecord r = activities.get(i);
            if (r.mVisibleRequested || r.isVisible()) {
                // While an activity launches a new activity, it's possible that the old activity
                // is already requested to be hidden (mVisibleRequested=false), but this visibility
                // is not yet committed, so isVisible()=true.
                hasVisibleActivities = true;
            }

            final Task task = r.getTask();
            if (task != null) {
                // There may be a pausing activity that hasn't shown any window and was requested
                // to be hidden. But pausing is also a visible state, it should be regarded as
                // visible, so the caller can know the next activity should be resumed.
                hasVisibleActivities |= task.handleAppDied(this);
            }
            r.handleAppDied();
        }
        clearRecentTasks();
        clearActivities();

        return hasVisibleActivities;
    }
```