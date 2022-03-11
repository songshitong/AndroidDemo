http://liuwangshu.cn/framework/component/2-service-start.html  android 7.0

1.ContextImpl到ActivityManageService的调用过程
要启动Service，我们会调用startService方法，它的实现在ContextWrapper中，代码如下所示。
frameworks/base/core/java/android/content/ContextWrapper.java
```
public class ContextWrapper extends Context {
    Context mBase;
...
  @Override
    public ComponentName startService(Intent service) {
        return mBase.startService(service);
    }
...    
}
```

在startService方法中会调用mBase的startService方法，Context类型的mBase对象具体指的是什么呢？在Android深入四大组件（一）应用程序启动过程（后篇）
这篇文章中我们讲过ActivityThread启动Activity时会调用如下代码创建Activity的上下文环境。
frameworks/base/core/java/android/app/ActivityThread.java
```
 private Activity performLaunchActivity(ActivityClientRecord r, Intent customIntent) {
  ...
            if (activity != null) {
                Context appContext = createBaseContextForActivity(r, activity);//1
         ...
                }
                activity.attach(appContext, this, getInstrumentation(), r.token,
                        r.ident, app, r.intent, r.activityInfo, title, r.parent,
                        r.embeddedID, r.lastNonConfigurationInstances, config,
                        r.referrer, r.voiceInteractor, window);
                ...
        }
        return activity;
}
```

在注释1处创建上下文对象appContext ，并传入Activity的attach方法中，将Activity与上下文对象appContext 关联起来，
这个上下文对象appContext 的具体类型是什么，我们接着查看createBaseContextForActivity方法，代码如下所示。
frameworks/base/core/java/android/app/ActivityThread.java
```
private Context createBaseContextForActivity(ActivityClientRecord r, final Activity activity) {
...

    ContextImpl appContext = ContextImpl.createActivityContext(
            this, r.packageInfo, r.token, displayId, r.overrideConfig);
    appContext.setOuterContext(activity);
    Context baseContext = appContext;
    ...
    return baseContext;
}
```
这里可以得出结论，上下文对象appContext 的具体类型就是ContextImpl 。Activity的attach方法中将ContextImpl赋值给ContextWrapper的成员变量mBase中，
因此，mBase具体指向就是ContextImpl 。 
那么，我们紧接着来查看ContextImpl的startService方法，代码如下所示。
frameworks/base/core/java/android/app/ContextImpl.java
```
@Override
public ComponentName startService(Intent service) {
    warnIfCallingFromSystemProcess();
    return startServiceCommon(service, mUser);
}
 private ComponentName startServiceCommon(Intent service, UserHandle user) {
        try {
            validateServiceIntent(service);
            service.prepareToLeaveProcess(this);
            /**
            * 1
            */
            ComponentName cn = ActivityManagerNative.getDefault().startService(
                mMainThread.getApplicationThread(), service, service.resolveTypeIfNeeded(
                            getContentResolver()), getOpPackageName(), user.getIdentifier());
      ...
            return cn;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
```

startService方法中会return startServiceCommon方法，在startServiceCommon方法中会在注释1处调用
ActivityManageService（AMS）的代理对象ActivityManagerProxy（AMP）的startService方法，最终会调用AMS的startService方法。
至于注释1处的代码为何会调用AMS的startService方法，在Android深入四大组件（一）应用程序启动过程（前篇）这篇文章中已经讲过，这里不再赘述。
ContextImpl到ActivityManageService的调用过程如下面的时序图所示
ContextImpl到ActivityManageService的调用过程.png


ActivityThread启动Service
我们接着来查看AMS的startService方法。
frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java
```
Override
public ComponentName startService(IApplicationThread caller, Intent service,
        String resolvedType, String callingPackage, int userId)
        throws TransactionTooLargeException {
 ...
    synchronized(this) {
 ...
        ComponentName res = mServices.startServiceLocked(caller, service,
                resolvedType, callingPid, callingUid, callingPackage, userId);//1
        Binder.restoreCallingIdentity(origId);
        return res;
    }
}
```

注释1处调用mServices的startServiceLocked方法，mServices的类型是ActiveServices，ActiveServices的startServiceLocked方法代码如下所示。
frameworks/base/services/core/java/com/android/server/am/ActiveServices.java
```
ComponentName startServiceLocked(IApplicationThread caller, Intent service, String resolvedType,
            int callingPid, int callingUid, String callingPackage, final int userId)
            throws TransactionTooLargeException {
      ...
        return startServiceInnerLocked(smap, service, r, callerFg, addToStarting);
    }

   ComponentName startServiceInnerLocked(ServiceMap smap, Intent service, ServiceRecord r,
            boolean callerFg, boolean addToStarting) throws TransactionTooLargeException {
 
     ...
        String error = bringUpServiceLocked(r, service.getFlags(), callerFg, false, false);
     ...
        return r.name;
    }
```

startServiceLocked方法的末尾return了startServiceInnerLocked方法，而startServiceInnerLocked方法中又调用了bringUpServiceLocked方法：
frameworks/base/services/core/java/com/android/server/am/ActiveServices.java
```
  private String bringUpServiceLocked(ServiceRecord r, int intentFlags, boolean execInFg,
            boolean whileRestarting, boolean permissionsReviewRequired)
            throws TransactionTooLargeException {
...
  final String procName = r.processName;//1
  ProcessRecord app;
  if (!isolated) {
            app = mAm.getProcessRecordLocked(procName, r.appInfo.uid, false);//2
            if (DEBUG_MU) Slog.v(TAG_MU, "bringUpServiceLocked: appInfo.uid=" + r.appInfo.uid
                        + " app=" + app);
            if (app != null && app.thread != null) {//3
                try {
                    app.addPackage(r.appInfo.packageName, r.appInfo.versionCode,
                    mAm.mProcessStats);
                    realStartServiceLocked(r, app, execInFg);//4
                    return null;
                } catch (TransactionTooLargeException e) {
                    throw e;
                } catch (RemoteException e) {
                    Slog.w(TAG, "Exception when starting service " + r.shortName, e);
                }
            }
        } else {
            app = r.isolatedProc;
        }
 //新建service进程，查询不到对应的ProcessRecord
 if (app == null && !permissionsReviewRequired) {//5
            if ((app=mAm.startProcessLocked(procName, r.appInfo, true, intentFlags,
                    "service", r.name, false, isolated, false)) == null) {//6
              ...
            }
            if (isolated) {
                r.isolatedProc = app;
            }
        }

 ...     
}
```

在注释1处得到ServiceRecord的processName的值赋值给procName ，其中processName用来描述Service想要在哪个进程运行，默认是当前进程，
我们也可以在AndroidManifes配置文件中设置android:process属性来新开启一个进程运行Service。

注释2处将procName和Service的uid传入到AMS的getProcessRecordLocked方法中，来查询是否存在一个与Service对应的ProcessRecord类型的对象app，
  ProcessRecord主要用来记录运行的应用程序进程的信息。
注释5处需要判断两个条件，一个是用来运行Service的应用程序进程不存在，另一个是应用程序之间的组件调用不需要检查权限，
  满足这两个条件则调用注释6处的AMS的startProcessLocked方法来创建对应的应用程序进程。  
  //新建进程的service，最终通过AMS向zygote创建新进程
  关于创建应用程序进程请查看Android应用程序进程启动过程（前篇） 和Android应用程序进程启动过程（后篇）这两篇文章。
注释3处判断如果用来运行Service的应用程序进程存在，则调用注释4处的realStartServiceLocked方法：
frameworks/base/services/core/java/com/android/server/am/ActiveServices.java
```
private final void realStartServiceLocked(ServiceRecord r,
        ProcessRecord app, boolean execInFg) throws RemoteException {
   ... //anr的埋炸弹      
    bumpServiceExecutingLocked(r, execInFg, "create");     
   ...
    try {
       ...
        app.thread.scheduleCreateService(r, r.serviceInfo,
                mAm.compatibilityInfoForPackageLocked(r.serviceInfo.applicationInfo),
                app.repProcState);
        r.postNotification();
        created = true;
    } catch (DeadObjectException e) {
      ...
    } 
    ...
}
```

在realStartServiceLocked方法中调用了app.thread的scheduleCreateService方法。其中app.thread是IApplicationThread类型的，
它的实现是ActivityThread的内部类ApplicationThread，其中ApplicationThread继承了ApplicationThreadNative，
而ApplicationThreadNative继承了Binder并实现了IApplicationThread接口。ApplicationThread的scheduleCreateService方法如下所示。
frameworks/base/core/java/android/app/ActivityThread.java
```
public final void scheduleCreateService(IBinder token,
         ServiceInfo info, CompatibilityInfo compatInfo, int processState) {
     updateProcessState(processState, false);
     CreateServiceData s = new CreateServiceData();
     s.token = token;
     s.info = info;
     s.compatInfo = compatInfo;
     sendMessage(H.CREATE_SERVICE, s);
 }
```

首先将要启动的信息封装成CreateServiceData 对象并传给sendMessage方法，sendMessage方法向H发送CREATE_SERVICE消息，
H是ActivityThread的内部类并继承Handler。这个过程和应用程序的启动过程（根Activity启动过程）是类似的。我们接着查看H的handleMessage方法。
frameworks/base/core/java/android/app/ActivityThread.java
```
public void handleMessage(Message msg) {
          if (DEBUG_MESSAGES) Slog.v(TAG, ">>> handling: " + codeToString(msg.what));
          switch (msg.what) {
          ...
             case CREATE_SERVICE:
                  Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, 
                  ("serviceCreate: " + String.valueOf(msg.obj)));
                  handleCreateService((CreateServiceData)msg.obj);
                  Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
                  break;
          ...
           }
        ...
        }
     ...   
}  
```

handleMessage方法根据消息类型，调用了handleCreateService方法：
frameworks/base/core/java/android/app/ActivityThread.java
```
private void handleCreateService(CreateServiceData data) {
       unscheduleGcIdler();
       LoadedApk packageInfo = getPackageInfoNoCheck(
               data.info.applicationInfo, data.compatInfo);//1
       Service service = null;
       try {
           java.lang.ClassLoader cl = packageInfo.getClassLoader();//2
           service = (Service) cl.loadClass(data.info.name).newInstance();//3
       } catch (Exception e) {
          ...
           }
       }
       try {
           if (localLOGV) Slog.v(TAG, "Creating service " + data.info.name);

           ContextImpl context = ContextImpl.createAppContext(this, packageInfo);//4
           context.setOuterContext(service);

           Application app = packageInfo.makeApplication(false, mInstrumentation);
           service.attach(context, this, data.info.name, data.token, app,
                   ActivityManagerNative.getDefault());//5
           service.onCreate();//6
           mServices.put(data.token, service);//7
        ...
       } catch (Exception e) {
           ...
       }
   }
```
注释1处获取要启动Service的应用程序的LoadedApk，LoadedApk是一个APK文件的描述类。
注释2处通过调用LoadedApk的getClassLoader方法来获取类加载器。
接着在注释3处根据CreateServiceData对象中存储的Service信息，将Service加载到内存中。
注释4处创建Service的上下文环境ContextImpl对象。
注释5处通过Service的attach方法来初始化Service。注释6处调用Service的onCreate方法，这样Service就启动了。
在注释7处将启动的Service加入到ActivityThread的成员变量mServices中，其中mServices是ArrayMap类型
最后给出这一节的时序图
ActivityManagerService启动service过程.png

