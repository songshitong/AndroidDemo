
implementation "androidx.lifecycle:lifecycle-process:2.4.1"
其他监听方式参考 https://stackoverflow.com/questions/4414171/how-to-detect-when-an-android-app-goes-to-the-background-and-come-back-to-the-fo
监听进程前后台
registerActivityLifecycleCallbacks 注册activity，前后台修改计数器，mStartedCounter为0是后台，大于1是前台
```
void activityStarted() {
        mStartedCounter++;
        if (mStartedCounter == 1 && mStopSent) {
            mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
            mStopSent = false;
        }
    }

 void activityPaused() {
        mResumedCounter--;
        if (mResumedCounter == 0) {
            mHandler.postDelayed(mDelayedPauseRunnable, TIMEOUT_MS); //700MS的延时
        }
    }    
```