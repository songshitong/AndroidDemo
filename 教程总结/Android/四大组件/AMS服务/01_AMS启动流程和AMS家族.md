http://liuwangshu.cn/framework/ams/1-ams.html  android 7.1.2

概述
AMS是系统的引导服务，应用进程的启动、切换和调度、四大组件的启动和管理都需要AMS的支持。
从这里可以看出AMS的功能会十分的繁多，当然它并不是一个类承担这个重责，它有一些关联类，这在文章后面会讲到。
AMS的涉及的知识点非常多，这篇文章主要会讲解AMS的以下几个知识点：
  AMS的启动流程
  AMS与进程启动  通过socket与zygote进行通信，然后fork进程
  AMS家族

AMS的启动流程
AMS的启动是在SyetemServer进程中启动的，在Android系统启动流程（三）解析SyetemServer进程启动过程这篇文章中提及过，
这里从SyetemServer的main方法开始讲起：
frameworks/base/services/java/com/android/server/SystemServer.java
```
public static void main(String[] args) {
       new SystemServer().run();
   }
```

main方法中只调用了SystemServer的run方法，如下所示。
frameworks/base/services/java/com/android/server/SystemServer.java
```
private void run() {
       ...
           System.loadLibrary("android_servers");//1
       ...
           mSystemServiceManager = new SystemServiceManager(mSystemContext);//2
           LocalServices.addService(SystemServiceManager.class, mSystemServiceManager);
       ...    
        try {
           Trace.traceBegin(Trace.TRACE_TAG_SYSTEM_SERVER, "StartServices");
           startBootstrapServices();//3
           startCoreServices();//4
           startOtherServices();//5
       } catch (Throwable ex) {
           Slog.e("System", "******************************************");
           Slog.e("System", "************ Failure starting system services", ex);
           throw ex;
       } finally {
           Trace.traceEnd(Trace.TRACE_TAG_SYSTEM_SERVER);
       }
       ...
   }
```

在注释1处加载了动态库libandroid_servers.so。接下来在注释2处创建SystemServiceManager，它会对系统的服务进行创建、启动和生命周期管理。
在注释3中的startBootstrapServices方法中用SystemServiceManager启动了ActivityManagerService、PowerManagerService、PackageManagerService等服务。
在注释4处的startCoreServices方法中则启动了BatteryService、UsageStatsService和WebViewUpdateService。
注释5处的startOtherServices方法中启动了CameraService、AlarmManagerService、VrManagerService等服务。这些服务的父类均为SystemService。
从注释3、4、5的方法可以看出，官方把系统服务分为了三种类型，分别是引导服务、核心服务和其他服务，其中其他服务是一些非紧要和一些不需要立即启动的服务。
  系统服务总共大约有80多个，我们主要来查看引导服务AMS是如何启动的，注释3处的startBootstrapServices方法如下所示。
frameworks/base/services/java/com/android/server/SystemServer.java
```
private void startBootstrapServices() {
     Installer installer = mSystemServiceManager.startService(Installer.class);
     // Activity manager runs the show.
     mActivityManagerService = mSystemServiceManager.startService(
             ActivityManagerService.Lifecycle.class).getService();//1
     mActivityManagerService.setSystemServiceManager(mSystemServiceManager);
     mActivityManagerService.setInstaller(installer);
   ...
 }
```

在注释1处调用了SystemServiceManager的startService方法，方法的参数是ActivityManagerService.Lifecycle.class：
frameworks/base/services/core/java/com/android/server/SystemServiceManager.java
```
@SuppressWarnings("unchecked")
  public <T extends SystemService> T startService(Class<T> serviceClass) {
      try {
         ...
          final T service;
          try {
              Constructor<T> constructor = serviceClass.getConstructor(Context.class);//1
              service = constructor.newInstance(mContext);//2
          } catch (InstantiationException ex) {
            ...
          }
          // Register it.
          mServices.add(service);//3
          // Start it.
          try {
              service.onStart();//4
          } catch (RuntimeException ex) {
              throw new RuntimeException("Failed to start service " + name
                      + ": onStart threw an exception", ex);
          }
          return service;
      } finally {
          Trace.traceEnd(Trace.TRACE_TAG_SYSTEM_SERVER);
      }
  }
```

startService方法传入的参数是Lifecycle.class，Lifecycle继承自SystemService。首先，通过反射来创建Lifecycle实例，
注释1处得到传进来的Lifecycle的构造器constructor，
在注释2处调用constructor的newInstance方法来创建Lifecycle类型的service对象。
接着在注释3处将刚创建的service添加到ArrayList类型的mServices对象中来完成注册。
最后在注释4处调用service的onStart方法来启动service，并返回该service。Lifecycle是AMS的内部类，代码如下所示。
frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java
```
public static final class Lifecycle extends SystemService {
     private final ActivityManagerService mService;
     public Lifecycle(Context context) {
         super(context);
         mService = new ActivityManagerService(context);//1
     }
     @Override
     public void onStart() {
         mService.start();//2
     }
     public ActivityManagerService getService() {
         return mService;//3
     }
 }
```

上面的代码结合SystemServiceManager的startService方法来分析，当通过反射来创建Lifecycle实例时，会调用注释1处的方法创建AMS实例，
当调用Lifecycle类型的service的onStart方法时，实际上是调用了注释2处AMS的start方法。
在SystemServer的startBootstrapServices方法的注释1处，调用了如下代码：
```
mActivityManagerService = mSystemServiceManager.startService(
               ActivityManagerService.Lifecycle.class).getService();
```
我们知道SystemServiceManager的startService方法最终会返回Lifecycle类型的对象，紧接着又调用了Lifecycle的getService方法，
这个方法会返回AMS类型的mService对象，见注释3处，这样AMS实例就会被创建并且返回。




AMS与进程启动
在Android系统启动流程（二）解析Zygote进程启动过程这篇文章中，我提到了Zygote的Java框架层中，会创建一个Server端的Socket，
这个Socket用来等待AMS来请求Zygote来创建新的应用程序进程。要启动一个应用程序，首先要保证这个应用程序所需要的应用程序进程已经被启动。
AMS在启动应用程序时会检查这个应用程序需要的应用程序进程是否存在，不存在就会请求Zygote进程将需要的应用程序进程启动。
Service的启动过程中会调用ActiveServices的bringUpServiceLocked方法，如下所示。
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
              ...
            }
        } else {
            app = r.isolatedProc;
        }
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

在注释1处得到ServiceRecord的processName的值赋值给procName ，其中ServiceRecord用来描述Service的android:process属性。
注释2处将procName和Service的uid传入到AMS的getProcessRecordLocked方法中，来查询是否存在一个与Service对应的ProcessRecord类型的对象app，
ProcessRecord主要用来记录运行的应用程序进程的信息。
注释5处判断Service对应的app为null则说明用来运行Service的应用程序进程不存在，则调用注释6处的AMS的startProcessLocked方法来创建对应的应用程序进程，
具体的过程请查看Android应用程序进程启动过程（前篇）


AMS家族
ActivityManager是一个和AMS相关联的类，它主要对运行中的Activity进行管理，这些管理工作并不是由ActivityManager来处理的，而是交由AMS来处理，
ActivityManager中的方法会通过ActivityManagerNative（以后简称AMN）的getDefault方法来得到ActivityManagerProxy(以后简称AMP)，
通过AMP就可以和AMN进行通信，而AMN是一个抽象类，它会将功能交由它的子类AMS来处理，因此，AMP就是AMS的代理类。AMS作为系统核心服务，
很多API是不会暴露给ActivityManager的，因此ActivityManager并不算是AMS家族一份子。
为了讲解AMS家族，这里拿Activity的启动过程举例，Activity的启动过程中会调用Instrumentation的execStartActivity方法，如下所示。
frameworks/base/core/java/android/app/Instrumentation.java
```
public ActivityResult execStartActivity(
            Context who, IBinder contextThread, IBinder token, Activity target,
            Intent intent, int requestCode, Bundle options) {
      ...
        try {
            intent.migrateExtraStreamToClipData();
            intent.prepareToLeaveProcess(who);
            int result = ActivityManagerNative.getDefault()
                .startActivity(whoThread, who.getBasePackageName(), intent,
                        intent.resolveTypeIfNeeded(who.getContentResolver()),
                        token, target != null ? target.mEmbeddedID : null,
                        requestCode, 0, null, options);
            checkStartActivityResult(result, intent);
        } catch (RemoteException e) {
            throw new RuntimeException("Failure from system", e);
        }
        return null;
    }
```

execStartActivity方法中会调用AMN的getDefault来获取AMS的代理类AMP。接着调用了AMP的startActivity方法，
先来查看AMN的getDefault方法做了什么，如下所示。
frameworks/base/core/java/android/app/ActivityManagerNative.java
```
 static public IActivityManager getDefault() {
        return gDefault.get();
    }
    private static final Singleton<IActivityManager> gDefault = new Singleton<IActivityManager>() {
        protected IActivityManager create() {
            IBinder b = ServiceManager.getService("activity");//1
            if (false) {
                Log.v("ActivityManager", "default service binder = " + b);
            }
            IActivityManager am = asInterface(b);//2
            if (false) {
                Log.v("ActivityManager", "default service = " + am);
            }
            return am;
        }+
    };
}
```


getDefault方法调用了gDefault的get方法，我们接着往下看，gDefault 是一个Singleton类。注释1处得到名为”activity”的Service引用，也就是IBinder类型的AMS的引用。
接着在注释2处将它封装成AMP类型对象，并将它保存到gDefault中，此后调用AMN的getDefault方法就会直接获得AMS的代理对象AMP。
注释2处的asInterface方法如下所示。
frameworks/base/core/java/android/app/ActivityManagerNative.java
```
static public IActivityManager asInterface(IBinder obj) {
    if (obj == null) {
        return null;
    }
    IActivityManager in =
        (IActivityManager)obj.queryLocalInterface(descriptor);
    if (in != null) {
        return in;
    }
    return new ActivityManagerProxy(obj);
}
```

asInterface方法的主要作用就是将IBinder类型的AMS引用封装成AMP，AMP的构造方法如下所示。
frameworks/base/core/java/android/app/ActivityManagerNative.java
```
class ActivityManagerProxy implements IActivityManager
{
    public ActivityManagerProxy(IBinder remote)
    {
        mRemote = remote;
    }
...
 }
```

AMP的构造方法中将AMS的引用赋值给变量mRemote ，这样在AMP中就可以使用AMS了。
其中IActivityManager是一个接口，AMN和AMP都实现了这个接口，用于实现代理模式和Binder通信。
再回到Instrumentation的execStartActivity方法，来查看AMP的startActivity方法，AMP是AMN的内部类，代码如下所示。
frameworks/base/core/java/android/app/ActivityManagerNative.java
```
public int startActivity(IApplicationThread caller, String callingPackage, Intent intent,
           String resolvedType, IBinder resultTo, String resultWho, int requestCode,
           int startFlags, ProfilerInfo profilerInfo, Bundle options) throws RemoteException {
     ...
       data.writeInt(requestCode);
       data.writeInt(startFlags);
     ...
       mRemote.transact(START_ACTIVITY_TRANSACTION, data, reply, 0);//1
       reply.readException();+
       int result = reply.readInt();
       reply.recycle();
       data.recycle();
       return result;
   }
```

首先会将传入的参数写入到Parcel类型的data中。在注释1处，通过IBinder类型对象mRemote（AMS的引用）向服务端的AMS发送一个
  START_ACTIVITY_TRANSACTION类型的进程间通信请求。那么服务端AMS就会从Binder线程池中读取我们客户端发来的数据，
最终会调用AMN的onTransact方法，如下所示。
frameworks/base/core/java/android/app/ActivityManagerNative.java
```
@Override
public boolean onTransact(int code, Parcel data, Parcel reply, int flags)
        throws RemoteException {
    switch (code) {
    case START_ACTIVITY_TRANSACTION:
    {
    ...
        int result = startActivity(app, callingPackage, intent, resolvedType,
                resultTo, resultWho, requestCode, startFlags, profilerInfo, options);
        reply.writeNoException();
        reply.writeInt(result);
        return true;
    }
}
```

onTransact中会调用AMS的startActivity方法，如下所示。
frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java
```
@Override
public final int startActivity(IApplicationThread caller, String callingPackage,
        Intent intent, String resolvedType, IBinder resultTo, String resultWho, int requestCode,
        int startFlags, ProfilerInfo profilerInfo, Bundle bOptions) {
    return startActivityAsUser(caller, callingPackage, intent, resolvedType, resultTo,
            resultWho, requestCode, startFlags, profilerInfo, bOptions,
            UserHandle.getCallingUserId());
}
```


startActivity方法会最后return startActivityAsUser方法，如下所示。
frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java
```
@Override
public final int startActivityAsUser(IApplicationThread caller, String callingPackage,
        Intent intent, String resolvedType, IBinder resultTo, String resultWho, int requestCode,
        int startFlags, ProfilerInfo profilerInfo, Bundle bOptions, int userId) {
    enforceNotIsolatedCaller("startActivity");
    userId = mUserController.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(),
            userId, false, ALLOW_FULL_ONLY, "startActivity", null);
    return mActivityStarter.startActivityMayWait(caller, -1, callingPackage, intent,
            resolvedType, null, null, resultTo, resultWho, requestCode, startFlags,
            profilerInfo, null, null, bOptions, false, userId, null, null);
 }           
```

startActivityAsUser方法最后会return ActivityStarter的startActivityMayWait方法，这一调用过程已经脱离了本节要讲的AMS家族，
因此这里不做介绍了，具体的调用过程可以查看Android深入四大组件（一）应用程序启动过程（后篇）这篇文章。

在Activity的启动过程中提到了AMP、AMN和AMS，它们共同组成了AMS家族的主要部分，如下图所示。
Android7.0_AMS家族.png

AMP是AMN的内部类，它们都实现了IActivityManager接口，这样它们就可以实现代理模式，具体来讲是远程代理：AMP和AMN是运行在两个进程的，
AMP是Client端，AMN则是Server端，而Server端中具体的功能都是由AMN的子类AMS来实现的，因此，AMP就是AMS在Client端的代理类。
AMN又实现了Binder类，这样AMP可以和AMS就可以通过Binder来进行进程间通信。

ActivityManager通过AMN的getDefault方法得到AMP，通过AMP就可以和AMN进行通信，也就是间接的与AMS进行通信。除了ActivityManager，
其他想要与AMS进行通信的类都需要通过AMP，如下图所示
Android7.0_AMS远程代理.png
