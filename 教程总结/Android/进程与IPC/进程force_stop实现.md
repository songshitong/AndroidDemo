
am force-stop pkgName
am force-stop --user 2 pkgName //只杀用户userId=2的相关信息
force-stop命令杀掉所有用户空间下的包名pkgName相关的信息，也可以通过--user来指定用户Id。 当执行上述am指令时，则会触发调用Am.java的main()方法

android-13.0.0_r1
Am.java
frameworks/base/cmds/am/src/com/android/commands/am/Am.java
```
 public static void main(String[] args) {
        (new Am()).run(args);
    }
 
  @Override
    public void onRun() throws Exception {
        String op = nextArgRequired();
        if (op.equals("instrument")) {
            runInstrument();
        } else {
            runAmCmd(getRawArgs());
        }
    }
    
   void runAmCmd(String[] args) throws AndroidException {
        final MyShellCallback cb = new MyShellCallback();
        try {
            mAm.asBinder().shellCommand(FileDescriptor.in, FileDescriptor.out, FileDescriptor.err,
                    args, cb, new ResultReceiver(null) { });
        }...
    }      
```
todo shellcommand  https://www.cnblogs.com/wanghongzhu/p/15067133.html

```
public void forceStopPackage(String packageName) {
        forceStopPackageAsUser(packageName, mContext.getUserId());
    }

   public void forceStopPackageAsUser(String packageName, int userId) {
        try {
            getService().forceStopPackage(packageName, userId);
        } ..
    }

  public static IActivityManager getService() {
        return IActivityManagerSingleton.get();
    }        
```
frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java
```
 @Override
    public void forceStopPackage(final String packageName, int userId) {
       //权限校验
       ...
        final int callingPid = Binder.getCallingPid();
        userId = mUserController.handleIncomingUser(callingPid, Binder.getCallingUid(),
                userId, true, ALLOW_FULL_ONLY, "forceStopPackage", null);
        final long callingId = Binder.clearCallingIdentity();
        try {
            IPackageManager pm = AppGlobals.getPackageManager();
            synchronized(this) {
                int[] users = userId == UserHandle.USER_ALL
                        ? mUserController.getUsers() : new int[] { userId };
                for (int user : users) {
                    if (getPackageManagerInternal().isPackageStateProtected(
                            packageName, user)) {
                        Slog.w(TAG, "Ignoring request to force stop protected package "
                                + packageName + " u" + user);
                        return;
                    }

                    int pkgUid = -1;
                    try {
                    //根据包名和userId来查询相应的uid
                        pkgUid = pm.getPackageUid(packageName, MATCH_DEBUG_TRIAGED_MISSING,
                                user);
                    } catch (RemoteException e) {
                    }
                   ...
                    try {//设置包的状态为stopped
                        pm.setPackageStoppedState(packageName, true, user);
                    }...
                    if (mUserController.isUserRunning(user, 0)) {
                    //根据包名和userId来查询相应的uid
                        forceStopPackageLocked(packageName, pkgUid, "from pid " + callingPid);
                        //发送ACTION_PACKAGE_RESTARTED的广播  用于清理已注册的alarm,notification信息
                        finishForceStopPackageLocked(packageName, pkgUid);
                    }
                }
            }
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }
    
 private void finishForceStopPackageLocked(final String packageName, int uid) {
        Intent intent = new Intent(Intent.ACTION_PACKAGE_RESTARTED,
                Uri.fromParts("package", packageName, null));
        if (!mProcessesReady) {
            intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY
                    | Intent.FLAG_RECEIVER_FOREGROUND);
        }
        intent.putExtra(Intent.EXTRA_UID, uid);
        intent.putExtra(Intent.EXTRA_USER_HANDLE, UserHandle.getUserId(uid));
        broadcastIntentLocked(null, null, null, intent,
                null, null, 0, null, null, null, null, null, OP_NONE,
                null, false, false, MY_PID, SYSTEM_UID, Binder.getCallingUid(),
                Binder.getCallingPid(), UserHandle.getUserId(uid));
    }   
```
这里有一个过程非常重要，那就是setPackageStoppedState()将包的状态设置为stopped，那么所有广播都无法接收，除非带有标记FLAG_INCLUDE_STOPPED_PACKAGES的广播，
系统默认的广播几乎都是不带有该标志，也就意味着被force-stop的应用是无法通过建立手机网络状态或者亮灭的广播来拉起进程。

当使用force stop方式来结束进程时, reason一般都是”from pid “ + callingPid. 当然也有另外,
那就是AMS.clearApplicationUserData方法调用forceStopPackageLocked的reason为”clear data”.

```
 private void forceStopPackageLocked(final String packageName, int uid, String reason) {
        forceStopPackageLocked(packageName, UserHandle.getAppId(uid), false,
                false, true, false, false, UserHandle.getUserId(uid), reason);
    }
    
final boolean forceStopPackageLocked(String packageName, int appId,
            boolean callerWillRestart, boolean purgeCache, boolean doit,
            boolean evenPersistent, boolean uninstalling, int userId, String reason) {
        int i;
       ...
       // 重新获取正确的appId
        if (appId < 0 && packageName != null) {
            appId = UserHandle.getAppId(getPackageManagerInternal().getPackageUid(packageName,
                    MATCH_DEBUG_TRIAGED_MISSING | MATCH_ANY_USER, UserHandle.USER_SYSTEM));
        }

        boolean didSomething;
        if (doit) {
           ...
            mAppErrors.resetProcessCrashTime(packageName == null, appId, userId);
        }

        synchronized (mProcLock) {
            // Notify first that the package is stopped, so its process won't be restarted
            // unexpectedly if there is an activity of the package without attached process
            // becomes visible when killing its other processes with visible activities.
            //清理activity
            didSomething = mAtmInternal.onForceStopPackage(
                    packageName, doit, evenPersistent, userId); //通知ActivityTaskManagerInternal的实现
            //清理进程
            didSomething |= mProcessList.killPackageProcessesLSP(packageName, appId, userId,
                    ProcessList.INVALID_ADJ, callerWillRestart, false /* allowRestart */, doit,
                    evenPersistent, true /* setRemoved */, uninstalling,
                    packageName == null ? ApplicationExitInfo.REASON_USER_STOPPED
                    : ApplicationExitInfo.REASON_USER_REQUESTED,
                    ApplicationExitInfo.SUBREASON_FORCE_STOP,
                    (packageName == null ? ("stop user " + userId) : ("stop " + packageName))
                    + " due to " + reason);
        }
         // 结束该包中的Service
        if (mServices.bringDownDisabledPackageServicesLocked(
                packageName, null /* filterByClasses */, userId, evenPersistent, true, doit)) {
            if (!doit) {
                return true;
            }
            didSomething = true;
        }

        if (packageName == null) {
           //当包名为空, 则移除当前用户的所有sticky broadcasts
            // Remove all sticky broadcasts from this user.
            mStickyBroadcasts.remove(userId);
        }
         //收集providers   
        ArrayList<ContentProviderRecord> providers = new ArrayList<>();
        if (mCpHelper.getProviderMap().collectPackageProvidersLocked(packageName, null, doit,
                evenPersistent, userId, providers)) {
            if (!doit) {
                return true;
            }
            didSomething = true;
        }
        for (i = providers.size() - 1; i >= 0; i--) {
           //清理providers
            mCpHelper.removeDyingProviderLocked(null, providers.get(i), true);
        }

         //移除已获取的跟该package/user相关的临时权限
        // Remove transient permissions granted from/to this package/user
        mUgmInternal.removeUriPermissionsForPackage(packageName, userId, false, false);

        if (doit) {
          // 清理Broadcast
            for (i = mBroadcastQueues.length - 1; i >= 0; i--) {
                didSomething |= mBroadcastQueues[i].cleanupDisabledPackageReceiversLocked(
                        packageName, null, userId, doit);
            }
        }

        if (packageName == null || uninstalling) {
          //清理 pedingIntent
            didSomething |= mPendingIntentController.removePendingIntentsForPackage(
                    packageName, userId, appId, doit);
        }

        if (doit) {
            if (purgeCache && packageName != null) {
                AttributeCache ac = AttributeCache.instance();
                if (ac != null) {
                    ac.removePackage(packageName);
                }
            }
            if (mBooted) {
                 //恢复栈顶的activity
                mAtmInternal.resumeTopActivities(true /* scheduleIdle */);
            }
        }

        return didSomething;
    }    
```
对于didSomething只指当方法中所有行为,则返回true.比如killPackageProcessesLocked(),只要杀过一个进程则代表didSomething为true.

该方法的主要功能:
Process: 调用AMS.killPackageProcessesLocked()清理该package所涉及的进程;
Activity: 调用ASS.finishDisabledPackageActivitiesLocked()清理该package所涉及的Activity;
Service: 调用AS.bringDownDisabledPackageServicesLocked()清理该package所涉及的Service;
Provider: 调用AMS.removeDyingProviderLocked()清理该package所涉及的Provider;
BroadcastRecevier: 调用BQ.cleanupDisabledPackageReceiversLocked()清理该package所涉及的广播




清理进程
frameworks/base/services/core/java/com/android/server/am/ProcessList.java
killPackageProcessesLSP
```
 boolean killPackageProcessesLSP(String packageName, int appId, int userId, int minOomAdj,
            int reasonCode, int subReason, String reason) {
        return killPackageProcessesLSP(packageName, appId, userId, minOomAdj,
                false /* callerWillRestart */, true /* allowRestart */, true /* doit */,
                false /* evenPersistent */, false /* setRemoved */, false /* uninstalling */,
                reasonCode, subReason, reason);
    }
 
 boolean killPackageProcessesLSP(String packageName, int appId,
            int userId, int minOomAdj, boolean callerWillRestart, boolean allowRestart,
            boolean doit, boolean evenPersistent, boolean setRemoved, boolean uninstalling,
            int reasonCode, int subReason, String reason) {
        final PackageManagerInternal pm = mService.getPackageManagerInternal();
        final ArrayList<Pair<ProcessRecord, Boolean>> procs = new ArrayList<>();

        //遍历当前所有运行中的进程
        final int NP = mProcessNames.getMap().size();
        for (int ip = 0; ip < NP; ip++) {
            SparseArray<ProcessRecord> apps = mProcessNames.getMap().valueAt(ip);
            final int NA = apps.size();
            for (int ia = 0; ia < NA; ia++) {
                ProcessRecord app = apps.valueAt(ia);
                if (app.isPersistent() && !evenPersistent) {
                    // we don't kill persistent processes
                    //不杀persistent进程
                    continue;
                }
                if (app.isRemoved()) {
                    //已标记removed的进程，便是需要被杀的进程，加入procs队列
                    if (doit) {
                        boolean shouldAllowRestart = false;
                        if (!uninstalling && packageName != null) {
                            // This package has a dependency on the given package being stopped,
                            // while it's not being frozen nor uninstalled, allow to restart it.
                            shouldAllowRestart = !app.getPkgList().containsKey(packageName)
                                    && app.getPkgDeps() != null
                                    && app.getPkgDeps().contains(packageName)
                                    && app.info != null
                                    && !pm.isPackageFrozen(app.info.packageName, app.uid,
                                            app.userId);
                        }
                        procs.add(new Pair<>(app, shouldAllowRestart));
                    }
                    continue;
                }
                //不杀adj低于预期的进程
                // Skip process if it doesn't meet our oom adj requirement.
                if (app.mState.getSetAdj() < minOomAdj) {
                    // Note it is still possible to have a process with oom adj 0 in the killed
                    // processes, but it does not mean misjudgment. E.g. a bound service process
                    // and its client activity process are both in the background, so they are
                    // collected to be killed. If the client activity is killed first, the service
                    // may be scheduled to unbind and become an executing service (oom adj 0).
                    continue;
                }

                boolean shouldAllowRestart = false;

                // If no package is specified, we call all processes under the
                // give user id.
                if (packageName == null) {
                    if (userId != UserHandle.USER_ALL && app.userId != userId) {
                        continue;
                    }
                    if (appId >= 0 && UserHandle.getAppId(app.uid) != appId) {
                        continue;
                    }
                    // Package has been specified, we want to hit all processes
                    // that match it.  We need to qualify this by the processes
                    // that are running under the specified app and user ID.
                } else {
                    //已指定包名的情况
                    
                    //PkgDeps: 该进程所依赖的包名
                    final boolean isDep = app.getPkgDeps() != null
                            && app.getPkgDeps().contains(packageName);
                    if (!isDep && UserHandle.getAppId(app.uid) != appId) {
                        continue;
                    }
                    if (userId != UserHandle.USER_ALL && app.userId != userId) {
                        continue;
                    }
                     //pkgList: 运行在该进程的所有包名;
                    final boolean isInPkgList = app.getPkgList().containsKey(packageName);
                    if (!isInPkgList && !isDep) {
                        continue;
                    }
                    if (!isInPkgList && isDep && !uninstalling && app.info != null
                            && !pm.isPackageFrozen(app.info.packageName, app.uid, app.userId)) {
                        // This package has a dependency on the given package being stopped,
                        // while it's not being frozen nor uninstalled, allow to restart it.
                        shouldAllowRestart = true;
                    }
                }

                // Process has passed all conditions, kill it!
                if (!doit) {
                    return true;
                }
                //通过前面所有条件,则意味着该进程需要被杀, 添加到procs队列
                if (setRemoved) {
                    app.setRemoved(true);
                }
                procs.add(new Pair<>(app, shouldAllowRestart));
            }
        }

        int N = procs.size();
        for (int i=0; i<N; i++) {
            final Pair<ProcessRecord, Boolean> proc = procs.get(i);
            removeProcessLocked(proc.first, callerWillRestart, allowRestart || proc.second,
                    reasonCode, subReason, reason);
        }
        //检查AppZygote是否需要停止
        killAppZygotesLocked(packageName, appId, userId, false /* force */);
        //更新adj
        mService.updateOomAdjLocked(OomAdjuster.OOM_ADJ_REASON_PROCESS_END);
        return N > 0;
    }    
```
一般地force-stop会指定包名，该方法会遍历当前所有运行中的进程mProcessNames，以下条件同时都不满足的进程，则会成为被杀的目标进程：
(也就是说满足以下任一条件都可以免死)

persistent进程：
进程setAdj < minOomAdj(默认为-100)：
非UserHandle.USER_ALL同时, 且进程的userId不相等：多用户模型下，不同用户下不能相互杀；
进程没有依赖该packageName, 且进程的AppId不相等;
进程没有依赖该packageName, 且该packageName没有运行在该进程.
通俗地来说就是：

forceStop不杀系统persistent进程；
当指定用户userId时，不杀其他用户空间的进程；
除此之外，以下情况则必然会成为被杀进程：

进程已标记remove=true的进程，则会被杀；
进程的pkgDeps中包含该packageName，则会被杀；
进程的pkgList中包含该packageName，且该进程与包名所指定的AppId相等则会被杀；
进程的pkgList是在启动组件或者创建进程的过程向该队列添加的，代表的是该应用下有组件运行在该进程。那么pkgDeps是指该进程所依赖的包名，
  调用ClassLoader的过程添加。


removeProcessLocked
```
 boolean removeProcessLocked(ProcessRecord app, boolean callerWillRestart,
            boolean allowRestart, int reasonCode, int subReason, String reason) {
        final String name = app.processName;
        final int uid = app.uid;
        ...
        ProcessRecord old = mProcessNames.get(name, uid);
        if (old != app) { //已经移除
            // This process is no longer active, so nothing to do.
            Slog.w(TAG, "Ignoring remove of inactive process: " + app);
            return false;
        }
        //从mProcessNames移除
        removeProcessNameLocked(name, uid);
        mService.mAtmInternal.clearHeavyWeightProcessIfEquals(app.getWindowProcessController());

        boolean needRestart = false;
        final int pid = app.getPid();
        if ((pid > 0 && pid != ActivityManagerService.MY_PID)
                || (pid == 0 && app.isPendingStart())) {
            if (pid > 0) {
                mService.removePidLocked(pid, app);
                app.setBindMountPending(false);
                mService.mHandler.removeMessages(PROC_START_TIMEOUT_MSG, app);
                mService.mBatteryStatsService.noteProcessFinish(app.processName, app.info.uid);
                if (app.isolated) {
                    mService.mBatteryStatsService.removeIsolatedUid(app.uid, app.info.uid);
                    mService.getPackageManagerInternal().removeIsolatedUid(app.uid);
                }
            }
            boolean willRestart = false;
            if (app.isPersistent() && !app.isolated) {
                if (!callerWillRestart) { //用于标记persistent进程则需重启进程
                    willRestart = true;
                } else {
                    needRestart = true;
                }
            }
            //杀掉该进程
            app.killLocked(reason, reasonCode, subReason, true);
            //清理该进程相关的信息
            mService.handleAppDiedLocked(app, pid, willRestart, allowRestart,
                    false /* fromBinderDied */);
            if (willRestart) {
              //对于persistent进程,则需要重新启动该进程
                removeLruProcessLocked(app);
                mService.addAppLocked(app.info, null, false, null /* ABI override */,
                        ZYGOTE_POLICY_FLAG_EMPTY);
            }
        } else {
            mRemovedProcesses.add(app);
        }

        return needRestart;
    }
```
该方法的主要功能:
从mProcessNames, mPidsSelfLocked队列移除该进程;
移除进程启动超时的消息PROC_START_TIMEOUT_MSG;
调用app.kill()来杀进程会同时调用Process.kill和Process.killProcessGroup, 该过程详见理解杀进程的实现原理 todo 文件改名字
调用handleAppDiedLocked()来清理进程相关的信息, 该过程详见binderDied()过程分析  todo


activity清除：
mAtmInternal.onForceStopPackage
frameworks/base/services/core/java/com/android/server/wm/ActivityTaskManagerService.java
```
 final class LocalService extends ActivityTaskManagerInternal {
   @Override
        public boolean onForceStopPackage(String packageName, boolean doit, boolean evenPersistent,
                int userId) {
            synchronized (mGlobalLock) {
                // In case if setWindowManager hasn't been called yet when booting.
                if (mRootWindowContainer == null) return false;
                return mRootWindowContainer.finishDisabledPackageActivities(packageName,
                        null /* filterByClasses */, doit, evenPersistent, userId,
                        // Only remove the activities without process because the activities with
                        // attached process will be removed when handling process died with
                        // WindowProcessController#isRemoved == true.
                        true /* onlyRemoveNoProcess */);
            }
        }
 }
```
frameworks/base/services/core/java/com/android/server/wm/RootWindowContainer.java
```
boolean finishDisabledPackageActivities(String packageName, Set<String> filterByClasses,
            boolean doit, boolean evenPersistent, int userId, boolean onlyRemoveNoProcess) {
        return mFinishDisabledPackageActivitiesHelper.process(packageName, filterByClasses, doit,
                evenPersistent, userId, onlyRemoveNoProcess);
    }

class FinishDisabledPackageActivitiesHelper implements Predicate<ActivityRecord> {

 boolean process(String packageName, Set<String> filterByClasses,
                boolean doit, boolean evenPersistent, int userId, boolean onlyRemoveNoProcess) {
            reset(packageName, filterByClasses, doit, evenPersistent, userId, onlyRemoveNoProcess);
            //执行test()方法
            forAllActivities(this);

            boolean didSomething = false;
            final int size = mCollectedActivities.size();
            // Keep the finishing order from top to bottom.
            for (int i = 0; i < size; i++) {
                final ActivityRecord r = mCollectedActivities.get(i);
                //传入的mOnlyRemoveNoProcess为true
                if (mOnlyRemoveNoProcess) {
                    if (!r.hasProcess()) {
                        didSomething = true;
                        Slog.i(TAG, "  Force removing " + r);
                        r.cleanUp(false /* cleanServices */, false /* setState */);
                        r.removeFromHistory("force-stop");
                    }
                } else {
                    didSomething = true;
                    Slog.i(TAG, "  Force finishing " + r);
                    r.finishIfPossible("force-stop", true /* oomAdj */);
                }
            }
            mCollectedActivities.clear();
            return didSomething;
        }
        
}    
```

frameworks/base/services/core/java/com/android/server/wm/ActivityRecord.java
```
 void cleanUp(boolean cleanServices, boolean setState) {
         //清除引用
        getTaskFragment().cleanUpActivityReferences(this);
        clearLastParentBeforePip();

        // Clean up the splash screen if it was still displayed.  清除闪屏
        cleanUpSplashScreen();

        deferRelaunchUntilPaused = false;
        frozenBeforeDestroy = false;

        if (setState) {
            setState(DESTROYED, "cleanUp");
            if (DEBUG_APP) Slog.v(TAG_APP, "Clearing app during cleanUp for activity " + this);
            detachFromProcess();
        }

        // Inform supervisor the activity has been removed.
        mTaskSupervisor.cleanupActivity(this);

        // Remove any pending results.
        if (finishing && pendingResults != null) {
            for (WeakReference<PendingIntentRecord> apr : pendingResults) {
                PendingIntentRecord rec = apr.get();
                if (rec != null) {
                    mAtmService.mPendingIntentController.cancelIntentSender(rec,
                            false /* cleanActivity */);
                }
            }
            pendingResults = null;
        }

        if (cleanServices) {
            cleanUpActivityServices();
        }

        // Get rid of any pending idle timeouts.
        removeTimeouts();
        // Clean-up activities are no longer relaunching (e.g. app process died). Notify window
        // manager so it can update its bookkeeping.
        clearRelaunching();
    }
```
mTaskSupervisor.cleanupActivity
frameworks/base/services/core/java/com/android/server/wm/ActivityTaskSupervisor.java
```
 void cleanupActivity(ActivityRecord r) {
        // Make sure this record is no longer in the pending finishes list.
        // This could happen, for example, if we are trimming activities
        // down to the max limit while they are still waiting to finish.
        mFinishingActivities.remove(r);

        stopWaitingForActivityVisible(r);
    }
 
   void stopWaitingForActivityVisible(ActivityRecord r) {
        reportActivityLaunched(false /* timeout */, r, WaitResult.INVALID_DELAY,
                WaitResult.LAUNCH_STATE_UNKNOWN);
    } 
 
  void reportActivityLaunched(boolean timeout, ActivityRecord r, long totalTime,
            @WaitResult.LaunchState int launchState) {
        boolean changed = false;
        for (int i = mWaitingActivityLaunched.size() - 1; i >= 0; i--) {
            final WaitInfo info = mWaitingActivityLaunched.get(i);
            if (!info.matches(r)) {
                continue;
            }
            final WaitResult w = info.mResult;
            w.timeout = timeout;
            w.who = r.mActivityComponent;
            w.totalTime = totalTime;
            w.launchState = launchState;
            mWaitingActivityLaunched.remove(i);
            changed = true;
        }
        if (changed) {
            mService.mGlobalLock.notifyAll();
        }
    }     
```

```
 void removeFromHistory(String reason) {
       //返回activity结果
        finishActivityResults(Activity.RESULT_CANCELED,
                null /* resultData */, null /* resultGrants */);
        makeFinishingLocked();
        ... 
        takeFromHistory();
        removeTimeouts();
        ProtoLog.v(WM_DEBUG_STATES, "Moving to DESTROYED: %s (removed from history)",
                this);
        setState(DESTROYED, "removeFromHistory");
        if (DEBUG_APP) Slog.v(TAG_APP, "Clearing app during remove for activity " + this);
        detachFromProcess();
        // Resume key dispatching if it is currently paused before we remove the container.
        resumeKeyDispatchingLocked();
        mDisplayContent.removeAppToken(token);
        //断开与service连接 
        cleanUpActivityServices();
        removeUriPermissionsLocked();
    }

void detachFromProcess() {
        //从WindowProcessController移除activity
        if (app != null) {
            app.removeActivity(this, false /* keepAssociation */);
        }
        app = null;
        mInputDispatchingTimeoutMillis = DEFAULT_DISPATCHING_TIMEOUT_MILLIS;
    }
```
frameworks/base/services/core/java/com/android/server/wm/WindowProcessController.java
```
void removeActivity(ActivityRecord r, boolean keepAssociation) {
        if (keepAssociation) {
            if (mInactiveActivities == null) {
                mInactiveActivities = new ArrayList<>();
                mInactiveActivities.add(r);
            } else if (!mInactiveActivities.contains(r)) {
                mInactiveActivities.add(r);
            }
        } else if (mInactiveActivities != null) {
            mInactiveActivities.remove(r);
        }
        mActivities.remove(r);
        mHasActivities = !mActivities.isEmpty();
        updateActivityConfigurationListener();
    }
```
这的activity清除大多是清除不可见activity



Service清除
mServices.bringDownDisabledPackageServicesLocked
frameworks/base/services/core/java/com/android/server/am/ActiveServices.java
```
 boolean bringDownDisabledPackageServicesLocked(String packageName, Set<String> filterByClasses,
            int userId, boolean evenPersistent, boolean fullStop, boolean doit) {
        boolean didSomething = false;

        if (mTmpCollectionResults != null) {
            mTmpCollectionResults.clear();
        }

        if (userId == UserHandle.USER_ALL) {
            for (int i = mServiceMap.size() - 1; i >= 0; i--) {
               //从mServiceMap中查询到所有属于该包下的service
                didSomething |= collectPackageServicesLocked(packageName, filterByClasses,
                        evenPersistent, doit, mServiceMap.valueAt(i).mServicesByInstanceName);
                if (!doit && didSomething) {
                    return true;
                }
                if (doit && filterByClasses == null) {
                    forceStopPackageLocked(packageName, mServiceMap.valueAt(i).mUserId);
                }
            }
        } else {
            ServiceMap smap = mServiceMap.get(userId);
            if (smap != null) {
                ArrayMap<ComponentName, ServiceRecord> items = smap.mServicesByInstanceName;
                didSomething = collectPackageServicesLocked(packageName, filterByClasses,
                        evenPersistent, doit, items);
            }
            if (doit && filterByClasses == null) {
                forceStopPackageLocked(packageName, userId);
            }
        }

        if (mTmpCollectionResults != null) {
            final int size = mTmpCollectionResults.size();
            for (int i = size - 1; i >= 0; i--) {
                //结束掉所有收集到的Service
                bringDownServiceLocked(mTmpCollectionResults.get(i), true);
            }
            if (size > 0) {
                //更新adj
                mAm.updateOomAdjPendingTargetsLocked(OomAdjuster.OOM_ADJ_REASON_UNBIND_SERVICE);
            }
            if (fullStop && !mTmpCollectionResults.isEmpty()) {
                // if we're tearing down the app's entire service state, account for possible
                // races around FGS notifications by explicitly tidying up in a separate
                // pass post-shutdown
                final ArrayList<ServiceRecord> allServices =
                        (ArrayList<ServiceRecord>) mTmpCollectionResults.clone();
                mAm.mHandler.postDelayed(() -> {
                    for (int i = 0; i < allServices.size(); i++) {
                        allServices.get(i).cancelNotification();
                    }
                }, 250L);
            }
            mTmpCollectionResults.clear();
        }
        return didSomething;
    }
```

collectPackageServicesLocked
```
 private boolean collectPackageServicesLocked(String packageName, Set<String> filterByClasses,
            boolean evenPersistent, boolean doit, ArrayMap<ComponentName, ServiceRecord> services) {
        boolean didSomething = false;
        for (int i = services.size() - 1; i >= 0; i--) {
            ServiceRecord service = services.valueAt(i);
            final boolean sameComponent = packageName == null
                    || (service.packageName.equals(packageName)
                        && (filterByClasses == null
                            || filterByClasses.contains(service.name.getClassName())));
            if (sameComponent
                    && (service.app == null || evenPersistent || !service.app.isPersistent())) {
                if (!doit) {
                    return true;
                }
                didSomething = true;
                Slog.i(TAG, "  Force stopping service " + service);
                if (service.app != null && !service.app.isPersistent()) {
                    stopServiceAndUpdateAllowlistManagerLocked(service);
                }
                service.setProcess(null, null, 0, null);
                service.isolationHostProc = null;
                if (mTmpCollectionResults == null) {
                    mTmpCollectionResults = new ArrayList<>();
                }
                //将满足条件service放入mTmpCollectionResults
                mTmpCollectionResults.add(service);
            }
        }
        return didSomething;
    }
```
bringDownServiceLocked
```
private void bringDownServiceLocked(ServiceRecord r, boolean enqueueOomAdj) {
        ...
        // Report to all of the connections that the service is no longer
        // available.
        ArrayMap<IBinder, ArrayList<ConnectionRecord>> connections = r.getConnections();
        for (int conni = connections.size() - 1; conni >= 0; conni--) {
            ArrayList<ConnectionRecord> c = connections.valueAt(conni);
            for (int i=0; i<c.size(); i++) {
                ConnectionRecord cr = c.get(i);
                // There is still a connection to the service that is
                // being brought down.  Mark it as dead.
                cr.serviceDead = true;
                cr.stopAssociation();
                final ComponentName clientSideComponentName =
                        cr.aliasComponent != null ? cr.aliasComponent : r.name;
                try {
                   // 断开service的连接
                    cr.conn.connected(r.name, null, true);
                } catch (Exception e) {
                    Slog.w(TAG, "Failure disconnecting service " + r.shortInstanceName
                          + " to connection " + c.get(i).conn.asBinder()
                          + " (in " + c.get(i).binding.client.processName + ")", e);
                }
            }
        }

        boolean oomAdjusted = false;
        // Tell the service that it has been unbound.
        if (r.app != null && r.app.getThread() != null) {
            for (int i = r.bindings.size() - 1; i >= 0; i--) {
                IntentBindRecord ibr = r.bindings.valueAt(i);
                if (DEBUG_SERVICE) Slog.v(TAG_SERVICE, "Bringing down binding " + ibr
                        + ": hasBound=" + ibr.hasBound);
                if (ibr.hasBound) {
                    try {
                        oomAdjusted |= bumpServiceExecutingLocked(r, false, "bring down unbind",
                                OomAdjuster.OOM_ADJ_REASON_UNBIND_SERVICE);
                        ibr.hasBound = false;
                        ibr.requested = false;
                         //最终调用目标Service.onUnbind()方法
                        r.app.getThread().scheduleUnbindService(r,
                                ibr.intent.getIntent());
                    } catch (Exception e) {
                        Slog.w(TAG, "Exception when unbinding service "
                                + r.shortInstanceName, e);
                        serviceProcessGoneLocked(r, enqueueOomAdj);
                        break;
                    }
                }
            }
        }

        // Check to see if the service had been started as foreground, but being
        // brought down before actually showing a notification.  That is not allowed.
        if (r.fgRequired) {
            Slog.w(TAG_SERVICE, "Bringing down service while still waiting for start foreground: "
                    + r);
            r.fgRequired = false;
            r.fgWaiting = false;
            synchronized (mAm.mProcessStats.mLock) {
                ServiceState stracker = r.getTracker();
                if (stracker != null) {
                    stracker.setForeground(false, mAm.mProcessStats.getMemFactorLocked(),
                            SystemClock.uptimeMillis());
                }
            }
            mAm.mAppOpsService.finishOperation(AppOpsManager.getToken(mAm.mAppOpsService),
                    AppOpsManager.OP_START_FOREGROUND, r.appInfo.uid, r.packageName, null);
            mAm.mHandler.removeMessages(
                    ActivityManagerService.SERVICE_FOREGROUND_TIMEOUT_MSG, r);
            if (r.app != null) {
                Message msg = mAm.mHandler.obtainMessage(
                        ActivityManagerService.SERVICE_FOREGROUND_CRASH_MSG);
                SomeArgs args = SomeArgs.obtain();
                args.arg1 = r.app;
                args.arg2 = r.toString();
                args.arg3 = r.getComponentName();

                msg.obj = args;
                mAm.mHandler.sendMessage(msg);
            }
        }

       ...
        r.destroyTime = SystemClock.uptimeMillis();
        ...

        final ServiceMap smap = getServiceMapLocked(r.userId);
        ServiceRecord found = smap.mServicesByInstanceName.remove(r.instanceName);

        // Note when this method is called by bringUpServiceLocked(), the service is not found
        // in mServicesByInstanceName and found will be null.
        if (found != null && found != r) {
            // This is not actually the service we think is running...  this should not happen,
            // but if it does, fail hard.
            smap.mServicesByInstanceName.put(r.instanceName, found);
            throw new IllegalStateException("Bringing down " + r + " but actually running "
                    + found);
        }
        smap.mServicesByIntent.remove(r.intent);
        r.totalRestartCount = 0;
        //从待重启列表mRestartingServices移除
        unscheduleServiceRestartLocked(r, 0, true);

        // Also make sure it is not on the pending list.
        //确保service不在pending队列
        for (int i=mPendingServices.size()-1; i>=0; i--) {
            if (mPendingServices.get(i) == r) {
                mPendingServices.remove(i);
               。。。。
            }
        }
        
        if (mPendingBringups.remove(r) != null) {
            if (DEBUG_SERVICE) Slog.v(TAG_SERVICE, "Removed pending bringup: " + r);
        }
        //取消service相关的通知
        cancelForegroundNotificationLocked(r);
        final boolean exitingFg = r.isForeground;
        if (exitingFg) {
            decActiveForegroundAppLocked(smap, r);
            synchronized (mAm.mProcessStats.mLock) {
                ServiceState stracker = r.getTracker();
                if (stracker != null) {
                    stracker.setForeground(false, mAm.mProcessStats.getMemFactorLocked(),
                            SystemClock.uptimeMillis());
                }
            }
            mAm.mAppOpsService.finishOperation(
                    AppOpsManager.getToken(mAm.mAppOpsService),
                    AppOpsManager.OP_START_FOREGROUND, r.appInfo.uid, r.packageName, null);
            unregisterAppOpCallbackLocked(r);
            r.mFgsExitTime = SystemClock.uptimeMillis();
            logFGSStateChangeLocked(r,
                    FrameworkStatsLog.FOREGROUND_SERVICE_STATE_CHANGED__STATE__EXIT,
                    r.mFgsExitTime > r.mFgsEnterTime
                            ? (int) (r.mFgsExitTime - r.mFgsEnterTime) : 0,
                    FGS_STOP_REASON_STOP_SERVICE);
            mAm.updateForegroundServiceUsageStats(r.name, r.userId, false);
        }

        r.isForeground = false;
        r.mFgsNotificationWasDeferred = false;
        dropFgsNotificationStateLocked(r);
        r.foregroundId = 0;
        r.foregroundNoti = null;
        resetFgsRestrictionLocked(r);
        // Signal FGS observers *after* changing the isForeground state, and
        // only if this was an actual state change.
        if (exitingFg) {
            signalForegroundServiceObserversLocked(r);
        }

        // Clear start entries.
        r.clearDeliveredStartsLocked();
        r.pendingStarts.clear();
        smap.mDelayedStartList.remove(r);

        if (r.app != null) {
            mAm.mBatteryStatsService.noteServiceStopLaunch(r.appInfo.uid, r.name.getPackageName(),
                    r.name.getClassName());
            stopServiceAndUpdateAllowlistManagerLocked(r);
            if (r.app.getThread() != null) {
                // Bump the process to the top of LRU list
                mAm.updateLruProcessLocked(r.app, false, null);
                updateServiceForegroundLocked(r.app.mServices, false);
                try {
                    oomAdjusted |= bumpServiceExecutingLocked(r, false, "destroy",
                            oomAdjusted ? null : OomAdjuster.OOM_ADJ_REASON_UNBIND_SERVICE);
                    mDestroyingServices.add(r);
                    r.destroying = true;
                    // 最终调用目标service的onDestroy()
                    r.app.getThread().scheduleStopService(r);
                } catch (Exception e) {
                    Slog.w(TAG, "Exception when destroying service "
                            + r.shortInstanceName, e);
                    serviceProcessGoneLocked(r, enqueueOomAdj);
                }
            } ...
        } ....

        if (!oomAdjusted) {
            mAm.enqueueOomAdjTargetLocked(r.app);
            if (!enqueueOomAdj) {
                mAm.updateOomAdjPendingTargetsLocked(OomAdjuster.OOM_ADJ_REASON_UNBIND_SERVICE);
            }
        }
        if (r.bindings.size() > 0) {
            r.bindings.clear();
        }

        if (r.restarter instanceof ServiceRestarter) {
           ((ServiceRestarter)r.restarter).setService(null);
        }

        synchronized (mAm.mProcessStats.mLock) {
            final int memFactor = mAm.mProcessStats.getMemFactorLocked();
            if (r.tracker != null) {
                final long now = SystemClock.uptimeMillis();
                r.tracker.setStarted(false, memFactor, now);
                r.tracker.setBound(false, memFactor, now);
                if (r.executeNesting == 0) {
                    r.tracker.clearCurrentOwner(r, false);
                    r.tracker = null;
                }
            }
        }
        smap.ensureNotStartingBackgroundLocked(r);
    }
```


Provider清理
collectPackageProvidersLocked(packageName, null, doit,
evenPersistent, userId, providers)
removeDyingProviderLocked(null, providers.get(i), true)

收集
frameworks/base/services/core/java/com/android/server/am/ProviderMap.java
```
 private boolean collectPackageProvidersLocked(String packageName,
            Set<String> filterByClasses, boolean doit, boolean evenPersistent,
            HashMap<ComponentName, ContentProviderRecord> providers,
            ArrayList<ContentProviderRecord> result) {
        boolean didSomething = false;
        for (ContentProviderRecord provider : providers.values()) {
            final boolean sameComponent = packageName == null
                    || (provider.info.packageName.equals(packageName)
                        && (filterByClasses == null
                            || filterByClasses.contains(provider.name.getClassName())));
            if (sameComponent
                    && (provider.proc == null || evenPersistent || !provider.proc.isPersistent())) {
                if (!doit) {
                    return true;
                }
                didSomething = true;
                result.add(provider);
            }
        }
        return didSomething;
    }
```
清理
frameworks/base/services/core/java/com/android/server/am/ContentProviderHelper.java
```
 boolean removeDyingProviderLocked(ProcessRecord proc, ContentProviderRecord cpr,
            boolean always) {
        boolean inLaunching = mLaunchingProviders.contains(cpr);
        if (inLaunching && !always && ++cpr.mRestartCount > ContentProviderRecord.MAX_RETRY_COUNT) {
            // It's being launched but we've reached maximum attempts, force the removal
            always = true;
        }
        //唤醒cpr, 并从mProviderMap中移除provider相关信息
        if (!inLaunching || always) {
            synchronized (cpr) {
                cpr.launchingApp = null;
                cpr.notifyAll();
                cpr.onProviderPublishStatusLocked(false);
                mService.mHandler.removeMessages(
                        ActivityManagerService.WAIT_FOR_CONTENT_PROVIDER_TIMEOUT_MSG, cpr);
            }
            final int userId = UserHandle.getUserId(cpr.uid);
            // Don't remove from provider map if it doesn't match
            // could be a new content provider is starting
            if (mProviderMap.getProviderByClass(cpr.name, userId) == cpr) {
                mProviderMap.removeProviderByClass(cpr.name, userId);
            }
            String[] names = cpr.info.authority.split(";");
            for (int j = 0; j < names.length; j++) {
                // Don't remove from provider map if it doesn't match
                // could be a new content provider is starting
                if (mProviderMap.getProviderByName(names[j], userId) == cpr) {
                    mProviderMap.removeProviderByName(names[j], userId);
                }
            }
        }

        for (int i = cpr.connections.size() - 1; i >= 0; i--) {
            ContentProviderConnection conn = cpr.connections.get(i);
            if (conn.waiting) {
                // If this connection is waiting for the provider, then we don't
                // need to mess with its process unless we are always removing
                // or for some reason the provider is not currently launching.
                if (inLaunching && !always) {
                    continue;
                }
            }
            ProcessRecord capp = conn.client;
            final IApplicationThread thread = capp.getThread();
            conn.dead = true;
            if (conn.stableCount() > 0) {
                final int pid = capp.getPid();
                if (!capp.isPersistent() && thread != null
                        && pid != 0 && pid != ActivityManagerService.MY_PID) {
                    //杀掉依赖该provider的client进程    
                    capp.killLocked(
                            "depends on provider " + cpr.name.flattenToShortString()
                            + " in dying proc " + (proc != null ? proc.processName : "??")
                            + " (adj " + (proc != null ? proc.mState.getSetAdj() : "??") + ")",
                            ApplicationExitInfo.REASON_DEPENDENCY_DIED,
                            ApplicationExitInfo.SUBREASON_UNKNOWN,
                            true);
                }
            } else if (thread != null && conn.provider.provider != null) {
                try {
                    thread.unstableProviderDied(conn.provider.provider.asBinder());
                } catch (RemoteException e) {
                }
                // In the protocol here, we don't expect the client to correctly
                // clean up this connection, we'll just remove it.
                cpr.connections.remove(i);
                if (cpr.proc != null && !hasProviderConnectionLocked(cpr.proc)) {
                    cpr.proc.mProfile.clearHostingComponentType(HOSTING_COMPONENT_TYPE_PROVIDER);
                }
                if (conn.client.mProviders.removeProviderConnection(conn)) {
                    mService.stopAssociationLocked(capp.uid, capp.processName,
                            cpr.uid, cpr.appInfo.longVersionCode, cpr.name, cpr.info.processName);
                }
            }
        }

        if (inLaunching && always) {
            mLaunchingProviders.remove(cpr);
            cpr.mRestartCount = 0;
            inLaunching = false;
        }
        return inLaunching;
    }
```
当其他app使用该provider, 且建立stable的连接, 那么对于非persistent进程,则会由于依赖该provider的缘故而被杀


广播清理
cleanupDisabledPackageReceiversLocked
frameworks/base/services/core/java/com/android/server/am/BroadcastDispatcher.java
```
 boolean cleanupDisabledPackageReceiversLocked(final String packageName,
            Set<String> filterByClasses, final int userId, final boolean doit) {
        // Note: fast short circuits when 'doit' is false, as soon as we hit any
        // "yes we would do something" circumstance
        boolean didSomething = cleanupBroadcastListDisabledReceiversLocked(mOrderedBroadcasts,
                packageName, filterByClasses, userId, doit);
        if (doit || !didSomething) {
            ArrayList<BroadcastRecord> lockedBootCompletedBroadcasts = new ArrayList<>();
            for (int u = 0, usize = mUser2Deferred.size(); u < usize; u++) {
                SparseArray<BroadcastRecord> brs =
                        mUser2Deferred.valueAt(u).mDeferredLockedBootCompletedBroadcasts;
                for (int i = 0, size = brs.size(); i < size; i++) {
                    lockedBootCompletedBroadcasts.add(brs.valueAt(i));
                }
            }
            didSomething = cleanupBroadcastListDisabledReceiversLocked(
                    lockedBootCompletedBroadcasts,
                    packageName, filterByClasses, userId, doit);
        }
        if (doit || !didSomething) {
            ArrayList<BroadcastRecord> bootCompletedBroadcasts = new ArrayList<>();
            for (int u = 0, usize = mUser2Deferred.size(); u < usize; u++) {
                SparseArray<BroadcastRecord> brs =
                        mUser2Deferred.valueAt(u).mDeferredBootCompletedBroadcasts;
                for (int i = 0, size = brs.size(); i < size; i++) {
                    bootCompletedBroadcasts.add(brs.valueAt(i));
                }
            }
            didSomething = cleanupBroadcastListDisabledReceiversLocked(bootCompletedBroadcasts,
                    packageName, filterByClasses, userId, doit);
        }
        if (doit || !didSomething) {
            didSomething |= cleanupDeferralsListDisabledReceiversLocked(mAlarmBroadcasts,
                    packageName, filterByClasses, userId, doit);
        }
        if (doit || !didSomething) {
            didSomething |= cleanupDeferralsListDisabledReceiversLocked(mDeferredBroadcasts,
                    packageName, filterByClasses, userId, doit);
        }
        if ((doit || !didSomething) && mCurrentBroadcast != null) {
            didSomething |= mCurrentBroadcast.cleanupDisabledPackageReceiversLocked(
                    packageName, filterByClasses, userId, doit);
        }

        return didSomething;
    }

private boolean cleanupBroadcastListDisabledReceiversLocked(ArrayList<BroadcastRecord> list,
            final String packageName, Set<String> filterByClasses, final int userId,
            final boolean doit) {
        boolean didSomething = false;
        for (BroadcastRecord br : list) {
            didSomething |= br.cleanupDisabledPackageReceiversLocked(packageName,
                    filterByClasses, userId, doit);
            if (!doit && didSomething) {
                return true;
            }
        }
        return didSomething;
    }
```
该方法主要功能：
清理有序广播队列mOrderedBroadcasts
清理BootCompletedBroadcasts
清理AlarmBroadcasts
清理DeferredBroadcasts

frameworks/base/services/core/java/com/android/server/am/BroadcastRecord.java
```
 @VisibleForTesting
    boolean cleanupDisabledPackageReceiversLocked(
            String packageName, Set<String> filterByClasses, int userId, boolean doit) {
        if (receivers == null) {
            return false;
        }

        final boolean cleanupAllUsers = userId == UserHandle.USER_ALL;
        final boolean sendToAllUsers = this.userId == UserHandle.USER_ALL;
        if (this.userId != userId && !cleanupAllUsers && !sendToAllUsers) {
            return false;
        }

        boolean didSomething = false;
        Object o;
        for (int i = receivers.size() - 1; i >= 0; i--) {
            o = receivers.get(i);
            if (!(o instanceof ResolveInfo)) {
                continue;
            }
            ActivityInfo info = ((ResolveInfo)o).activityInfo;

            final boolean sameComponent = packageName == null
                    || (info.applicationInfo.packageName.equals(packageName)
                    && (filterByClasses == null || filterByClasses.contains(info.name)));
            if (sameComponent && (cleanupAllUsers
                    || UserHandle.getUserId(info.applicationInfo.uid) == userId)) {
                if (!doit) {
                    return true;
                }
                didSomething = true;
                receivers.remove(i);
                if (i < nextReceiver) {
                    nextReceiver--;
                }
            }
        }
        nextReceiver = Math.min(nextReceiver, receivers.size());
        return didSomething;
    }
```
清除receiver


Alarm清
finishForceStopPackageLocked
frameworks/base/apex/jobscheduler/service/java/com/android/server/alarm/AlarmManagerService.java
```
 class UninstallReceiver extends BroadcastReceiver {
        public UninstallReceiver() {
            IntentFilter filter = new IntentFilter();
            ...
            filter.addAction(Intent.ACTION_PACKAGE_RESTARTED);
           ...
            getContext().registerReceiverForAllUsers(this, filter,
                    /* broadcastPermission */ null, /* scheduler */ null);
            ...
        }
        
      @Override
        public void onReceive(Context context, Intent intent) {
            final int uid = intent.getIntExtra(Intent.EXTRA_UID, -1);
            synchronized (mLock) {
                String pkgList[] = null;
                switch (intent.getAction()) {
                   ....
                    case Intent.ACTION_PACKAGE_RESTARTED:
                        final Uri data = intent.getData();
                        if (data != null) {
                            final String pkg = data.getSchemeSpecificPart();
                            if (pkg != null) {
                            //解析包名
                                pkgList = new String[]{pkg};
                            }
                        }
                        break;
                }
                if (pkgList != null && (pkgList.length > 0)) {
                    for (String pkg : pkgList) {
                        if (uid >= 0) {
                            // package-removed and package-restarted case
                            mAppWakeupHistory.removeForPackage(pkg, UserHandle.getUserId(uid));
                            mAllowWhileIdleHistory.removeForPackage(pkg, UserHandle.getUserId(uid));
                            mAllowWhileIdleCompatHistory.removeForPackage(pkg,
                                    UserHandle.getUserId(uid));
                            mTemporaryQuotaReserve.removeForPackage(pkg, UserHandle.getUserId(uid));
                            removeLocked(uid, REMOVE_REASON_UNDEFINED);
                        } else {
                            // external-applications-unavailable case  移除pkg
                            removeLocked(pkg);
                        }
                        mPriorities.remove(pkg);
                        for (int i = mBroadcastStats.size() - 1; i >= 0; i--) {
                            ArrayMap<String, BroadcastStats> uidStats = mBroadcastStats.valueAt(i);
                            if (uidStats.remove(pkg) != null) {
                                if (uidStats.size() <= 0) {
                                    mBroadcastStats.removeAt(i);
                                }
                            }
                        }
                    }
                }
            }
        }    
}
```
调用AlarmManagerService中的removeLocked()方法，从mPendingBackgroundAlarms队列中移除包所相关的alarm

Notification清理
frameworks/base/services/core/java/com/android/server/notification/NotificationManagerService.java
```
 private final BroadcastReceiver mPackageIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                return;
            }

            boolean queryRestart = false;
            boolean queryRemove = false;
            boolean packageChanged = false;
            boolean cancelNotifications = true; //默认为TRUE
            boolean hideNotifications = false;
            boolean unhideNotifications = false;
            int reason = REASON_PACKAGE_CHANGED;

            if (action.equals(Intent.ACTION_PACKAGE_ADDED)
                    || (queryRemove=action.equals(Intent.ACTION_PACKAGE_REMOVED))
                    || action.equals(Intent.ACTION_PACKAGE_RESTARTED)
                    || (packageChanged=action.equals(Intent.ACTION_PACKAGE_CHANGED))
                    || (queryRestart=action.equals(Intent.ACTION_QUERY_PACKAGE_RESTART))
                    || action.equals(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE)
                    || action.equals(Intent.ACTION_PACKAGES_SUSPENDED)
                    || action.equals(Intent.ACTION_PACKAGES_UNSUSPENDED)
                    || action.equals(Intent.ACTION_DISTRACTING_PACKAGES_CHANGED)) {
                int changeUserId = intent.getIntExtra(Intent.EXTRA_USER_HANDLE,
                        UserHandle.USER_ALL);
                String pkgList[] = null;
                int uidList[] = null;
                boolean removingPackage = queryRemove &&
                        !intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
                ...对于不需要移除notification的情况cancelNotifications=true
                if (pkgList != null && (pkgList.length > 0)) {
                    if (cancelNotifications) {
                        for (String pkgName : pkgList) {
                            cancelAllNotificationsInt(MY_UID, MY_PID, pkgName, null, 0, 0,
                                    !queryRestart, changeUserId, reason, null);
                        }
                    } else if (hideNotifications && uidList != null && (uidList.length > 0)) {
                        hideNotificationsForPackages(pkgList, uidList);
                    } else if (unhideNotifications && uidList != null && (uidList.length > 0)) {
                        unhideNotificationsForPackages(pkgList, uidList);
                    }
                }

                mHandler.scheduleOnPackageChanged(removingPackage, changeUserId, pkgList, uidList);
            }
        }
    };

```
调用NotificationManagerService.java 中的cancelAllNotificationsInt()方法，从mNotificationList和mEnqueuedNotifications
队列中移除包所相关的Notification.


作者案例：
级联诛杀
有BAT的某浏览器大厂(具体名称就匿了)，浏览器会因为另一个app被杀而导致自己无辜被牵连所杀，并怀疑是ROM定制化导致的bug，于是发邮件向我厂请教缘由。
出于好奇，帮他们进一步调查了下这个问题，发现并非无辜被杀，而是force-stop的级联诛杀所导致的。
简单来说就是App1调用了getClassLoader()来加载App2，那么App1所运行的进程便会在其pkgDeps队列中增加App2的包名，在前面已经提到pkgDeps，
杀进程的过程中会遍历该队列，当App2被forceStop所杀时，便是级联诛杀App1。App1既然会调用App2的ClassLoader来加载其方法，那么就建立了一定的联系，
这是Google有意赋予forceStop这个强力杀的功能。
这个故事是想告诉大家在插件化或者反射的过程中要注意这种情况，防止不必要的误伤

建立依赖：
frameworks/base/core/java/android/app/LoadedApk.java
```
 public ClassLoader getClassLoader() {
        synchronized (mLock) {
            if (mClassLoader == null) {
                createOrUpdateClassLoaderLocked(null /*addedPaths*/);
            }
            return mClassLoader;
        }
    }
    
 
   @GuardedBy("mLock")
    private void createOrUpdateClassLoaderLocked(List<String> addedPaths) {
        ...
        if (mRegisterPackage) {
            try {
                ActivityManager.getService().addPackageDependency(mPackageName);
            } ..
        }
        ...
}   
```
frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java
```
@Override
    public void addPackageDependency(String packageName) {
        int callingPid = Binder.getCallingPid();
        if (callingPid == myPid()) {
            //  Yeah, um, no.
            return;
        }
        ProcessRecord proc;
        synchronized (mPidsSelfLocked) {
            proc = mPidsSelfLocked.get(Binder.getCallingPid());
        }
        if (proc != null) {
            ArraySet<String> pkgDeps = proc.getPkgDeps();
            synchronized (this) {
                synchronized (mProcLock) {
                    if (pkgDeps == null) {
                        proc.setPkgDeps(pkgDeps = new ArraySet<String>(1));
                    }
                     //将该包名添加到pkgDeps
                    pkgDeps.add(packageName);
                }
            }
        }
    }
```
调用ClassLoader来加载启动包名时，则会将该包名加入到进程的pkgDeps。


forceStop的功能如下:
清理Process: 调用AMS.killPackageProcessesLocked()清理该package所涉及的进程;
清理Activity: 调用ASS.finishDisabledPackageActivitiesLocked()清理该package所涉及的Activity;
清理Service: 调用AS.bringDownDisabledPackageServicesLocked()清理该package所涉及的Service;
清理Provider: 调用AMS.removeDyingProviderLocked()清理该package所涉及的Provider;
清理BroadcastRecevier: 调用BQ.cleanupDisabledPackageReceiversLocked()清理该package所涉及的广播
清理发送广播ACTION_PACKAGE_RESTARTED，用于停止已注册的alarm,notification

persistent进程的特殊待遇:
进程: AMS.killPackageProcessesLocked()不杀进程
Service: ActiveServices.collectPackageServicesLocked()不移除不清理service
Provider: ProviderMap.collectPackageProvidersLocked()不收集不清理provider. 且不杀该provider所连接的client的persistent进程;



功能点归纳：
force-stop并不会杀persistent进程；
当app被force-stop后，无法接收到任何普通广播，那么也就常见的监听手机网络状态的变化或者屏幕亮灭的广播来拉起进程肯定是不可行；
当app被force-stop后，那么alarm闹钟一并被清理，无法实现定时响起的功能；
app被force-stop后，四大组件以及相关进程都被一一剪除清理，即便多进程架构的app也无法拉起自己；
级联诛杀：当app通过ClassLoader加载另一个app，则会在force-stop的过程中会被级联诛杀；
生死与共：当app与另个app使用了share uid，则会在force-stop的过程，任意一方被杀则另一方也被杀，建立起生死与共的强关系