http://liuwangshu.cn/framework/component/3-service-bind.html   android7.0
我们可以通过调用Context的startService来启动Service，也可以通过Context的bindService来绑定Service，
建议阅读此篇文章前请阅读Android深入四大组件（二）Service的启动过程这篇文章，知识点重叠的部分，本篇文章将不再赘述
//todo 查看进程切换
ContextImpl到ActivityManageService的调用过程
我们可以用bindService方法来绑定Service，它的实现在ContextWrapper中，代码如下所示。
frameworks/base/core/java/android/content/ContextWrapper.java
```
@Override
 public boolean bindService(Intent service, ServiceConnection conn,
         int flags) {
     return mBase.bindService(service, conn, flags);
 }
```

这里mBase具体指向就是ContextImpl，不明白的请查看 Android深入四大组件（二）Service的启动过程这篇文章。接着查看ContextImpl的bindService方法：
frameworks/base/core/java/android/app/ContextImpl.java
```
@Override
  public boolean bindService(Intent service, ServiceConnection conn,
          int flags) {
      warnIfCallingFromSystemProcess();
      return bindServiceCommon(service, conn, flags, mMainThread.getHandler(),
              Process.myUserHandle());
  }
```

在bindService方法中，又return了bindServiceCommon方法，代码如下所示。
frameworks/base/core/java/android/app/ContextImpl.java
```
private boolean bindServiceCommon(Intent service, ServiceConnection conn, int flags, Handler
        handler, UserHandle user) {
    IServiceConnection sd;
    if (conn == null) {
        throw new IllegalArgumentException("connection is null");
    }
    if (mPackageInfo != null) {
        sd = mPackageInfo.getServiceDispatcher(conn, getOuterContext(), handler, flags);//1
    } else {
        throw new RuntimeException("Not supported in system context");
    }
    validateServiceIntent(service);
    try {
     ...
     /**
     * 2
     */
        int res = ActivityManagerNative.getDefault().bindService(
            mMainThread.getApplicationThread(), getActivityToken(), service,
            service.resolveTypeIfNeeded(getContentResolver()),
            sd, flags, getOpPackageName(), user.getIdentifier());
      ...
    } catch (RemoteException e) {
        throw e.rethrowFromSystemServer();
    }
}
```
//todo loadedApk 加载程序的apk，可以是自己，可以别人
在注释1处调用了LoadedApk类型的对象mPackageInfo的getServiceDispatcher方法，它的主要作用是将ServiceConnection封装为IServiceConnection类型的对象sd，
从IServiceConnection的名字我们就能得知它实现了Binder机制，这样Service的绑定就支持了跨进程。接着在注释2处我们又看见了熟悉的代码，最终会调用AMS的bindService方法。
ContextImpl到ActivityManageService的调用过程如下面的时序图所示
ContextImpl到ActivityManageService的service调用过程.png

Service的绑定过程
AMS的bindService方法代码如下所示。
frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java
```
  public int bindService(IApplicationThread caller, IBinder token, Intent service,
            String resolvedType, IServiceConnection connection, int flags, String callingPackage,
            int userId) throws TransactionTooLargeException {
        enforceNotIsolatedCaller("bindService");
...
        synchronized(this) {
            return mServices.bindServiceLocked(caller, token, service,
                    resolvedType, connection, flags, callingPackage, userId);
        }
    }
```

bindService方法最后会调用ActiveServices类型的对象mServices的bindServiceLocked方法：
frameworks/base/services/core/java/com/android/server/am/ActiveServices.java
```
 int bindServiceLocked(IApplicationThread caller, IBinder token, Intent service,
            String resolvedType, final IServiceConnection connection, int flags,
            String callingPackage, final int userId) throws TransactionTooLargeException {
 ...
 if ((flags&Context.BIND_AUTO_CREATE) != 0) {
                s.lastActivity = SystemClock.uptimeMillis();
                /**
                *  1
                */
                if (bringUpServiceLocked(s, service.getFlags(), callerFg, false,
                        permissionsReviewRequired) != null) {
                    return 0;
                }
            }
          ...
            if (s.app != null && b.intent.received) {//2
                try {
                    c.conn.connected(s.name, b.intent.binder);//3
                } catch (Exception e) {
                ...
                }
                if (b.intent.apps.size() == 1 && b.intent.doRebind) {//4
                    requestServiceBindingLocked(s, b.intent, callerFg, true);//5
                }
            } else if (!b.intent.requested) {//6
                requestServiceBindingLocked(s, b.intent, callerFg, false);//7
            }
            getServiceMap(s.userId).ensureNotStartingBackground(s);
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
        return 1;
}
```

在注释1处会bringUpServiceLocked方法，在bringUpServiceLocked方法中又会调用realStartServiceLocked方法，
最终由ActivityThread来调用Service的onCreate方法启动Service，这一过程在Service的启动过程这篇文章中已经讲过，这里不再赘述。
在注释2处s.app != null 表示Service已经运行，其中s是ServiceRecord类型对象，app是ProcessRecord类型对象。
   b.intent.received表示当前应用程序进程的Client端已经接收到绑定Service时返回的Binder，
   这样应用程序进程的Client端就可以通过Binder来获取要绑定的Service的访问接口。
注释3处调用c.conn的connected方法，其中c.conn指的是IServiceConnection，它的具体实现为ServiceDispatcher.InnerConnection，
  其中ServiceDispatcher是LoadedApk的内部类，InnerConnection的connected方法内部会调用H的post方法向主线程发送消息，
  从而解决当前应用程序进程和Service跨进程通信的问题，在后面会详细介绍这一过程。
在注释4处如果当前应用程序进程的Client端第一次与Service进行绑定的，并且Service已经调用过onUnBind方法，则需要调用注释5的代码。
注释6处如果应用程序进程的Client端没有发送过绑定Service的请求，则会调用注释7的代码，注释7和注释5的代码区别就是最后一个参数rebind为false，表示不是重新绑定。
接着我们查看注释7的requestServiceBindingLocked方法，代码如下所示。
frameworks/base/services/core/java/com/android/server/am/ActiveServices.java
```
private final boolean requestServiceBindingLocked(ServiceRecord r, IntentBindRecord i,
        boolean execInFg, boolean rebind) throws TransactionTooLargeException {
   ...
    if ((!i.requested || rebind) && i.apps.size() > 0) {//1
        try {
            bumpServiceExecutingLocked(r, execInFg, "bind");
            r.app.forceProcessStateUpTo(ActivityManager.PROCESS_STATE_SERVICE);
            r.app.thread.scheduleBindService(r, i.intent.getIntent(), rebind,
                    r.app.repProcState);//2
           ...
        } 
        ...
    }
    return true;
}
```

注释1处i.requested表示是否发送过绑定Service的请求，从前面的代码得知是没有发送过，因此，!i.requested为true。从前面的代码得知rebind值为false，
那么(!i.requested || rebind)的值为true。如果IntentBindRecord中的应用程序进程记录大于0，则会调用注释2的代码，
r.app.thread的类型为IApplicationThread，它的实现我们已经很熟悉了，是ActivityThread的内部类ApplicationThread，scheduleBindService方法如下所示。
frameworks/base/core/java/android/app/ActivityThread.java
```
public final void scheduleBindService(IBinder token, Intent intent,
              boolean rebind, int processState) {
          updateProcessState(processState, false);
          BindServiceData s = new BindServiceData();
          s.token = token;
          s.intent = intent;
          s.rebind = rebind;
          if (DEBUG_SERVICE)
              Slog.v(TAG, "scheduleBindService token=" + token + " intent=" + intent + " uid="
                      + Binder.getCallingUid() + " pid=" + Binder.getCallingPid());
          sendMessage(H.BIND_SERVICE, s);
      }
```

首先将Service的信息封装成BindServiceData对象，需要注意的BindServiceData的成员变量rebind的值为false，后面会用到它。
接着将BindServiceData传入到sendMessage方法中。sendMessage向H发送消息，我们接着查看H的handleMessage方法。
frameworks/base/core/java/android/app/ActivityThread.java
```
public void handleMessage(Message msg) {
          if (DEBUG_MESSAGES) Slog.v(TAG, ">>> handling: " + codeToString(msg.what));
          switch (msg.what) {
          ...
              case BIND_SERVICE:
                    Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, "serviceBind");
                    handleBindService((BindServiceData)msg.obj);
                    Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
                    break;
          ...
           }
        ...
        }
     ...   
}
```


H在接收到BIND_SERVICE类型消息时，会在handleMessage方法中会调用handleBindService方法：
frameworks/base/core/java/android/app/ActivityThread.java
```
private void handleBindService(BindServiceData data) {
       Service s = mServices.get(data.token);//1
       if (DEBUG_SERVICE)
           Slog.v(TAG, "handleBindService s=" + s + " rebind=" + data.rebind);
       if (s != null) {
           try {
               data.intent.setExtrasClassLoader(s.getClassLoader());
               data.intent.prepareToEnterProcess();
               try {
                   if (!data.rebind) {//2
                       IBinder binder = s.onBind(data.intent);//3
                       ActivityManagerNative.getDefault().publishService(
                               data.token, data.intent, binder);//4
                   } else {
                       s.onRebind(data.intent);//5
                       ActivityManagerNative.getDefault().serviceDoneExecuting(
                               data.token, SERVICE_DONE_EXECUTING_ANON, 0, 0);
                   }
                   ensureJitEnabled();
               } 
               ...
           } 
           ...
       }
   }
```
//todo onbind干了啥
注释1处获取要绑定的Service 。注释2处的BindServiceData的成员变量rebind的值为false，这样会调用注释3处的代码来调用Service的onBind方法，
这样Service处于绑定状态了。如果rebind的值为true就会调用注释5处的Service的onRebind方法，结合前文的bindServiceLocked方法的注释4处，
我们得知如果当前应用程序进程的Client端第一次与Service进行绑定，并且Service已经调用过onUnBind方法，则会调用Service的onRebind方法。
接着查看注释4的代码，实际上是调用AMS的publishService方法。
讲到这，先给出这一部分的代码时序图（不包括Service启动过程）
Ams绑定service流程_01.png



我们接着来查看AMS的publishService方法，代码如下所示。
frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java
```
public void publishService(IBinder token, Intent intent, IBinder service) {
  ...
    synchronized(this) {
        if (!(token instanceof ServiceRecord)) {
            throw new IllegalArgumentException("Invalid service token");
        }
        mServices.publishServiceLocked((ServiceRecord)token, intent, service);
    }
}
```

publishService方法中，调用了ActiveServices类型的mServices对象的publishServiceLocked方法：
frameworks/base/services/core/java/com/android/server/am/ActiveServices.java
```
void publishServiceLocked(ServiceRecord r, Intent intent, IBinder service) {
       final long origId = Binder.clearCallingIdentity();
       try {
          ...
                   for (int conni=r.connections.size()-1; conni>=0; conni--) {
                       ArrayList<ConnectionRecord> clist = r.connections.valueAt(conni);
                       for (int i=0; i<clist.size(); i++) {
                        ...
                           try {
                               c.conn.connected(r.name, service);//1
                           } catch (Exception e) {
                            ...
                           }
                       }
                   }
               }
               serviceDoneExecutingLocked(r, mDestroyingServices.contains(r), false);
           }
       } finally {
           Binder.restoreCallingIdentity(origId);
       }
   }
```

注释1处的代码，我在前面介绍过，c.conn指的是IServiceConnection，它的具体实现为ServiceDispatcher.InnerConnection，
其中ServiceDispatcher是LoadedApk的内部类，ServiceDispatcher.InnerConnectiond的connected方法的代码如下所示。
frameworks/base/core/java/android/app/LoadedApk.java
```
static final class ServiceDispatcher {
     ...
        private static class InnerConnection extends IServiceConnection.Stub {
            final WeakReference<LoadedApk.ServiceDispatcher> mDispatcher;
            InnerConnection(LoadedApk.ServiceDispatcher sd) {
                mDispatcher = new WeakReference<LoadedApk.ServiceDispatcher>(sd);
            }
            public void connected(ComponentName name, IBinder service) throws RemoteException {
                LoadedApk.ServiceDispatcher sd = mDispatcher.get();
                if (sd != null) {
                    sd.connected(name, service);//1
                }
            }
        }
 ...
 }       
```
在注释1处调用了ServiceDispatcher 类型的sd对象的connected方法，代码如下所示。
frameworks/base/core/java/android/app/LoadedApk.java
```
public void connected(ComponentName name, IBinder service) {
           if (mActivityThread != null) {
               mActivityThread.post(new RunConnection(name, service, 0));//1
           } else {
               doConnected(name, service);
           }
       }
```
//如果是主线程就切换到主线程
注释1处调用Handler类型的对象mActivityThread的post方法，mActivityThread实际上指向的是H。因此，
   通过调用H的post方法将RunConnection对象的内容运行在主线程中。RunConnection的定义如下所示。
frameworks/base/core/java/android/app/LoadedApk.java
```
private final class RunConnection implements Runnable {
      RunConnection(ComponentName name, IBinder service, int command) {
          mName = name;
          mService = service;
          mCommand = command;
      }
      public void run() {
          if (mCommand == 0) {
              doConnected(mName, mService);
          } else if (mCommand == 1) {
              doDeath(mName, mService);
          }
      }
      final ComponentName mName;
      final IBinder mService;
      final int mCommand;
  }
```

在RunConnection的run方法中调用了doConnected方法：
frameworks/base/core/java/android/app/LoadedApk.java
```
public void doConnected(ComponentName name, IBinder service) {
  ...
    // If there was an old service, it is not disconnected.
    if (old != null) {
        mConnection.onServiceDisconnected(name);
    }
    // If there is a new service, it is now connected.
    if (service != null) {
        mConnection.onServiceConnected(name, service);//1
    }
}
```
//todo mConnection
在注释1处调用了ServiceConnection类型的对象mConnection的onServiceConnected方法，
  这样在客户端中实现了ServiceConnection接口的类的onServiceConnected方法就会被执行。至此，Service的绑定过程就分析到这。
最后给出剩余部分的代码时序图
AMS_pushService过程.png