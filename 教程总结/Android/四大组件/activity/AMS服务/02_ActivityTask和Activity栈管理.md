
http://liuwangshu.cn/framework/ams/2-activitytask.html  android 7.1.2

ActivityStack
ActivityStack是一个管理类，用来管理系统所有Activity的各种状态，其内部维护了TaskRecord的列表，因此从Activity任务栈这一角度来说，
ActivityStack也可以理解为Activity堆栈。它由ActivityStackSupervisor来进行管理的，而ActivityStackSupervisor在AMS中的构造方法中被创建。
frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java
```
public ActivityManagerService(Context systemContext) {
...
 mStackSupervisor = new ActivityStackSupervisor(this);
... 
}
```

//todo 存储activity的类型
ActivityStack的实例类型
ActivityStackSupervisor中有多种ActivityStack实例，如下所示。
frameworks/base/services/core/java/com/android/server/am/ActivityStackSupervisor.java
```
public final class ActivityStackSupervisor implements DisplayListener {
   ...
    ActivityStack mHomeStack;
    ActivityStack mFocusedStack;
    private ActivityStack mLastFocusedStack;
    ...
}
```

mHomeStack用来存储Launcher App的所有Activity，mFocusedStack表示当前正在接收输入或启动下一个Activity的所有Activity。
mLastFocusedStack表示此前接收输入的所有Activity。

通过ActivityStackSupervisor提供了获取上述ActivityStack的方法，比如要获取mFocusedStack，
只需要调用ActivityStackSupervisor的getFocusedStack方法就可以了：
frameworks/base/services/core/java/com/android/server/am/ActivityStackSupervisor.java
```
ActivityStack getFocusedStack() {
     return mFocusedStack;
 }
```


ActivityState
ActivityStack中通过枚举存储了Activity的所有的状态，如下所示。
frameworks/base/services/core/java/com/android/server/am/ActivityStack.java
```
enum ActivityState {
       INITIALIZING,
       RESUMED,
       PAUSING,
       PAUSED,
       STOPPING,
       STOPPED,
       FINISHING,
       DESTROYING,
       DESTROYED
   }
```

通过名称我们可以很轻易知道这些状态所代表的意义。应用ActivityState的场景会有很多，比如下面的代码：
frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java
```
@Override
 public void overridePendingTransition(IBinder token, String packageName,
         int enterAnim, int exitAnim) {
  ...
         if (self.state == ActivityState.RESUMED
                 || self.state == ActivityState.PAUSING) {//1
             mWindowManager.overridePendingAppTransition(packageName,
                     enterAnim, exitAnim, null);
         }
         Binder.restoreCallingIdentity(origId);
     }
 }
```
overridePendingTransition方法用于设置Activity的切换动画，注释1处可以看到只有ActivityState为RESUMED状态或者PAUSING状态时
才会调用WMS类型的mWindowManager对象的overridePendingAppTransition方法来进行切换动画



特殊状态的Activity
在ActivityStack中定义了一些特殊状态的Activity，如下所示。
```
ActivityRecord mPausingActivity = null;//正在暂停的Activity
ActivityRecord mLastPausedActivity = null;//上一个已经暂停的Activity
ActivityRecord mLastNoHistoryActivity = null;//最近一次没有历史记录的Activity
ActivityRecord mResumedActivity = null;//已经Resume的Activity
ActivityRecord mLastStartedActivity = null;//最近一次启动的Activity
ActivityRecord mTranslucentActivityWaiting = null;//传递给convertToTranslucent方法的最上层的Activity
```

这些特殊的状态都是ActivityRecord类型的，ActivityRecord用来记录一个Activity的所有信息。从Activity任务栈的角度来说，
一个或多个ActivityRecord会组成一个TaskRecord，TaskRecord用来记录Activity的栈，而ActivityStack包含了一个或多个TaskRecord
ActivityStack包含图.png


维护的ArrayList
ActivityStack中维护了很多ArrayList，这些ArrayList中的元素类型主要有ActivityRecord和TaskRecord，其中TaskRecord用来记录Activity的Task。

ArrayList	          元素类型	         说明
mTaskHistory	      TaskRecord	     所有没有被销毁的Task
mLRUActivities	      ActivityRecord	 正在运行的Activity，列表中的第一个条目是最近最少使用的元素
mNoAnimActivities	  ActivityRecord	 不考虑转换动画的Activity
mValidateAppTokens	  TaskGroup	         用于与窗口管理器验证应用令牌



Activity栈管理
我们知道Activity是由任务栈来进行管理的，有了栈管理，我们可以对应用程序进行操作，应用可以复用自身应用中以及其他应用的Activity，
节省了资源。比如我们使用一款社交应用，这个应用的联系人详情界面提供了联系人的邮箱，当我们点击邮箱时会跳到发送邮件的界面
应用跳转示例.png

社交应用和系统Email中的Activity是处于不同应用程序进程的，而有了栈管理，就可以把发送邮件界面放到社交应用中详情界面所在栈的栈顶，来做到跨进程操作。
为了更灵活的进行栈管理，Android系统提供了很多配置，下面分别对它们进行介绍

Launch Mode
Launch Mode都不会陌生，用于设定Activity的启动方式，无论是哪种启动方式，所启动的Activity都会位于Activity栈的栈顶。有以下四种：

standerd：默认模式，每次启动Activity都会创建一个新的Activity实例。
singleTop：如果要启动的Activity已经在栈顶，则不会重新创建Activity，同时该Activity的onNewIntent方法会被调用。
    如果要启动的Activity不在栈顶，则会重新创建该Activity的实例。
singleTask：如果要启动的Activity已经存在于它想要归属的栈中，那么不会创建该Activity实例，将栈中位于该Activity上的所有的Activity出栈，
   同时该Activity的onNewIntent方法会被调用。如果要启动的Activity不存在于它想要归属的栈中，并且该栈存在，则会重新创建该Activity的实例。
   如果要启动的Activity想要归属的栈不存在，则首先要创建一个新栈，然后创建该Activity实例并压入到新栈中。
singleInstance：和singleTask基本类似，不同的是启动Activity时，首先要创建在一个新栈，然后创建该Activity实例并压入新栈中，
   新栈中只会存在这一个Activity实例。


Intent的FLAG
Intent中定义了很多了FLAG，其中有几个FLAG也可以设定Activity的启动方式，如果Launch Mode设定和FLAG设定的Activity的启动方式有冲突，
  则以FLAG设定的为准。
FLAG_ACTIVITY_SINGLE_TOP：和Launch Mode中的singleTop效果是一样的。
FLAG_ACTIVITY_NEW_TASK：和Launch Mode中的singleTask效果是一样的。
FLAG_ACTIVITY_CLEAR_TOP：Launch Mode中没有与此对应的模式，如果要启动的Activity已经存在于栈中，
  则将所有位于它上面的Activity出栈。singleTask默认具有此标记位的效果


除了这三个FLAG，还有一些FLAG对我们分析栈管理有些帮助。

FLAG_ACTIVITY_NO_HISTORY：Activity一旦退出，就不会存在于栈中。同样的，也可以在AndroidManifest.xml中设置“android:noHistory”。
FLAG_ACTIVITY_MULTIPLE_TASK：需要和FLAG_ACTIVITY_NEW_TASK一同使用才有效果，系统会启动一个新的栈来容纳新启动的Activity.
FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS：Activity不会被放入到“最近启动的Activity”列表中。
FLAG_ACTIVITY_BROUGHT_TO_FRONT：这个标志位通常不是由应用程序中的代码设置的，而是Launch Mode为singleTask时，由系统自动加上的。
FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY：这个标志位通常不是由应用程序中的代码设置的，而是从历史记录中启动的（长按Home键调出）。
FLAG_ACTIVITY_CLEAR_TASK：需要和FLAG_ACTIVITY_NEW_TASK一同使用才有效果，用于清除与启动的Activity相关栈的所有其他Activity。



接下来通过系统源码来查看FLAG的应用，在Android深入四大组件（一）应用程序启动过程（后篇）中讲过，根Activity启动时会调用AMS的startActivity方法，
经过层层调用会调用ActivityStarter的startActivityUnchecked方法，如下面的时序图所示。 
图自己找一下。。
frameworks/base/services/core/java/com/android/server/am/ActivityStarter.java
```
 private int startActivityUnchecked(final ActivityRecord r, ActivityRecord sourceRecord,
            IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor,
            int startFlags, boolean doResume, ActivityOptions options, TaskRecord inTask) {
        setInitialState(r, options, inTask, doResume, startFlags, sourceRecord, voiceSession,
                voiceInteractor);//1
        computeLaunchingTaskFlags();//2
        computeSourceStack();
        mIntent.setFlags(mLaunchFlags);//3
 ...       
}

```


注释1处用于初始化启动Activity的各种配置，在初始化前会重置各种配置再进行配置，这些配置包括：ActivityRecord、Intent、
TaskRecord和LaunchFlags（启动的FLAG）等等。注释2处的computeLaunchingTaskFlags方法用于计算出启动的FLAG，并将计算的值赋值给mLaunchFlags。
在注释3处将mLaunchFlags设置给Intent，达到设定Activity的启动方式的目的。接着来查看computeLaunchingTaskFlags方法。
frameworks/base/services/core/java/com/android/server/am/ActivityStarter.java
```
private void computeLaunchingTaskFlags() {
...
      if (mInTask == null) {//1
            if (mSourceRecord == null) {//2
                if ((mLaunchFlags & FLAG_ACTIVITY_NEW_TASK) == 0 && mInTask == null) {//3
                    Slog.w(TAG, "startActivity called from non-Activity context; forcing " +
                            "Intent.FLAG_ACTIVITY_NEW_TASK for: " + mIntent);
                    mLaunchFlags |= FLAG_ACTIVITY_NEW_TASK;
                }
            } else if (mSourceRecord.launchMode == LAUNCH_SINGLE_INSTANCE) {//4
                mLaunchFlags |= FLAG_ACTIVITY_NEW_TASK;
            } else if (mLaunchSingleInstance || mLaunchSingleTask) {//5
                mLaunchFlags |= FLAG_ACTIVITY_NEW_TASK;
            }
        }
}
```

计算启动的FLAG的逻辑比较复杂，这里只截取了一小部分，注释1处的TaskRecord类型的mInTask为null时，说明Activity要加入的栈不存在。
  因此，这一小段代码主要解决的问题就是Activity要加入的栈不存在时如何计算出启动的FLAG。
注释2处，ActivityRecord类型的mSourceRecord用于描述“初始Activity”，什么是“初始Activity”呢？比如ActivityA启动了ActivityB，
  ActivityA就是初始Activity。同时满足注释2和注释3的条件则需要创建一个新栈。
注释4处，如果“初始Activity”所在的栈只允许有一个Activity实例，则也需要创建一个新栈。
注释5处，如果Launch Mode设置了singleTask或singleInstance，则也要创建一个新栈



taskAffinity
我们可以在AndroidManifest.xml设置android:taskAffinity，用来指定Activity希望归属的栈， 默认情况下，同一个应用程序的所有的Activity都有着相同的taskAffinity。
taskAffinity在下面两种情况时会产生效果。

taskAffinity与FLAG_ACTIVITY_NEW_TASK或者singleTask配合。如果新启动Activity的taskAffinity和栈的taskAffinity相同（栈的taskAffinity取决于根Activity的taskAffinity）则加入到该栈中。如果不同，就会创建新栈。
taskAffinity与allowTaskReparenting配合。如果allowTaskReparenting为true，说明Activity具有转移的能力。拿此前的邮件为例，
当社交应用启动了发送邮件的Activity，此时发送邮件的Activity是和社交应用处于同一个栈中。如果发送邮件的Activity的allowTaskReparenting设置为true，
此后邮件程序所在的栈位于前台，这个时候发送邮件的Activity就会由社交应用的栈中转移到与它更亲近的邮件程序（taskAffinity相同）所在的栈中

接着通过系统源码来查看taskAffinity的应用。ActivityStackSupervisor的findTaskLocked方法用于找到Activity最匹配的栈，
最终会调用ActivityStack的findTaskLocked方法。
frameworks/base/services/core/java/com/android/server/am/ActivityStack.java
```
 void findTaskLocked(ActivityRecord target, FindTaskResult result) {
...
   for (int taskNdx = mTaskHistory.size() - 1; taskNdx >= 0; --taskNdx) {//1
     final TaskRecord task = mTaskHistory.get(taskNdx);//2
   ...
     else if (!isDocument && !taskIsDocument
                    && result.r == null && task.canMatchRootAffinity()) {
                if (task.rootAffinity.equals(target.taskAffinity)) {//3
                    if (DEBUG_TASKS) Slog.d(TAG_TASKS, "Found matching affinity candidate!");
                    result.r = r;
                    result.matchedByRootAffinity = true;
                }
            } else if (DEBUG_TASKS) Slog.d(TAG_TASKS, "Not a match: " + task);
        }
    }
```

这个方法的逻辑比较复杂，这里截取了和taskAffinity相关的部分。注释1处遍历mTaskHistory列表，列表的元素为TaskRecord，
用于存储没有被销毁的Task。注释2处得到某一个Task的信息。注释3处将Task的rootAffinity（初始的taskAffinity）和目标Activity的taskAffinity做对比，
如果相同，则将FindTaskResult的matchedByRootAffinity 属性设置为true，说明找到了匹配的Task。