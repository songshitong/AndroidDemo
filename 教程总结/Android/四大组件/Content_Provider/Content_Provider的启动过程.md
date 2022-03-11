http://liuwangshu.cn/framework/component/5-contentprovider-start.html  android7.0
//只列出了provider启动  查看provider的增删改查



Content Provider做为四大组件之一，通常情况下并没有其他的组件使用频繁，但这不能作为我们不去深入学习它的理由。
关于Content Provider一篇文章是写不完的，这一篇文章先来介绍它的启动过程

1.query方法到AMS的调用过程
Content Provider使用的例子，在Activity中我是使用如下代码调用Content Provider的：
```
public class ContentProviderActivity extends AppCompatActivity {
    private final static String TAG = "ContentProviderActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content_provider);
        Uri uri = Uri.parse("content://com.example.liuwangshu.mooncontentprovide.GameProvider");
        ContentValues mContentValues = new ContentValues();
        mContentValues.put("_id", 2);
        mContentValues.put("name", "大航海时代ol");
        mContentValues.put("describe", "最好玩的航海网游");
        getContentResolver().insert(uri, mContentValues);//1
        Cursor gameCursor = getContentResolver().query(uri, new String[]{"name", "describe"}, null, null, null);
     ...
    }
}
```

要想调用Content Provider，首先需要使用注释1处的getContentResolver方法，如下所示。
frameworks/base/core/Java/android/content/ContextWrapper.java
```
@Override
public ContentResolver getContentResolver() {
    return mBase.getContentResolver();
}
```

这里mBase指的是ContextImpl，ContextImpl的getContentResolver方法如下所示。
frameworks/base/core/java/android/app/ContextImpl.java
```
@Override
public ContentResolver getContentResolver() {
    return mContentResolver;
}
```

上面的代码return了ApplicationContentResolver类型的mContentResolver对象，ApplicationContentResolver是ContextImpl中的静态内部类，
继承自ContentResolver，它在ContextImpl的构造方法中被创建。
当我们调用ContentResolver的insert、query、update等方法时就会启动Content Provider，这里拿query方法来进行举例。
query方法的实现在ApplicationContentResolver的父类ContentResolver中，代码如下所示。
frameworks/base/core/java/android/content/ContentResolver.java
```
public final @Nullable Cursor query(final @RequiresPermission.Read @NonNull Uri uri,
            @Nullable String[] projection, @Nullable String selection,
            @Nullable String[] selectionArgs, @Nullable String sortOrder,
            @Nullable CancellationSignal cancellationSignal) {
        Preconditions.checkNotNull(uri, "uri");
        IContentProvider unstableProvider = acquireUnstableProvider(uri);//1
        ...
        try {
           ...
            try {
                qCursor = unstableProvider.query(mPackageName, uri, projection,
                        selection, selectionArgs, sortOrder, remoteCancellationSignal);//2
            } catch (DeadObjectException e) {
               ...
            }
    ...
   }
```

在注释1处通过acquireUnstableProvider方法返回IContentProvider类型的unstableProvider对象，在注释2处调用unstableProvider的query方法。
我们查看acquireUnstableProvider方法做了什么，如下所示。
frameworks/base/core/java/android/content/ContentResolver.java
```
public final IContentProvider acquireUnstableProvider(Uri uri) {
     if (!SCHEME_CONTENT.equals(uri.getScheme())) {//1
         return null;
     }
     String auth = uri.getAuthority();
     if (auth != null) {
         return acquireUnstableProvider(mContext, uri.getAuthority());//2
     }
     return null;
 }
```
注释1处用来检查Uri的scheme是否等于”content”，如果不是则返回null。注释2处调用了acquireUnstableProvider方法，
这是个抽象方法，它的实现在ContentResolver的子类ApplicationContentResolver中：
frameworks/base/core/java/android/app/ContextImpl.java
```
@Override
protected IContentProvider acquireUnstableProvider(Context c, String auth) {
    return mMainThread.acquireProvider(c,
            ContentProvider.getAuthorityWithoutUserId(auth),
            resolveUserIdFromAuthority(auth), false);
}
```

return了ActivityThread类型的mMainThread对象的acquireProvider方法：
frameworks/base/core/java/android/app/ActivityThread.java
```
public final IContentProvider acquireProvider(
         Context c, String auth, int userId, boolean stable) {
     final IContentProvider provider = acquireExistingProvider(c, auth, userId, stable);//1
     if (provider != null) {
         return provider;
     }
     IActivityManager.ContentProviderHolder holder = null;
     try {
         holder = ActivityManagerNative.getDefault().getContentProvider(
                 getApplicationThread(), auth, userId, stable);//2
     } catch (RemoteException ex) {
         throw ex.rethrowFromSystemServer();
     }
     if (holder == null) {
         Slog.e(TAG, "Failed to find provider info for " + auth);
         return null;
     }
     holder = installProvider(c, holder, holder.info,
             true /*noisy*/, holder.noReleaseNeeded, stable);//3
     return holder.provider;
 }
```

final ArrayMap<ProviderKey, ProviderClientRecord> mProviderMap
          = new ArrayMap<ProviderKey, ProviderClientRecord>();

注释1处检查ActivityThread中的ArrayMap类型的mProviderMap中是否有目标ContentProvider存在，有则返回，
没有就会在注释2处调用AMP的getContentProvider方法，最终会调用AMS的getContentProvider方法。
注释3处的installProvider方法用来将注释2处返回的ContentProvider相关的数据存储在mProviderMap中，起到缓存的作用， 
这样使用相同的Content Provider时，就不需要每次都要调用AMS的getContentProvider方法。  installProvider最后有介绍
使用我们接着查看AMS的getContentProvider方法，代码如下所示。
frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java
```
@Override
public final ContentProviderHolder getContentProvider(
        IApplicationThread caller, String name, int userId, boolean stable) {
 ...
    return getContentProviderImpl(caller, name, null, stable, userId);
}
```

getContentProvider方法return了getContentProviderImpl方法：
frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java
```
   private ContentProviderHolder getContentProviderImpl(IApplicationThread caller,
            String name, IBinder token, boolean stable, int userId) {
...
       ProcessRecord proc = getProcessRecordLocked(
                                cpi.processName, cpr.appInfo.uid, false);//1
                        if (proc != null && proc.thread != null && !proc.killed) {
                            ...
                            if (!proc.pubProviders.containsKey(cpi.name)) {
                                checkTime(startTime, "getContentProviderImpl: scheduling install");
                                proc.pubProviders.put(cpi.name, cpr);
                                try {
                                    proc.thread.scheduleInstallProvider(cpi);//2
                                } catch (RemoteException e) {
                                }
                            }
                        } else {
                            checkTime(startTime, "getContentProviderImpl: before start process");
                            proc = startProcessLocked(cpi.processName,
                                    cpr.appInfo, false, 0, "content provider",
                                    new ComponentName(cpi.applicationInfo.packageName,
                                            cpi.name), false, false, false);//3
                            checkTime(startTime, "getContentProviderImpl: after start process");
                          ...
                        }
             ...           
                        
}
```

getContentProviderImpl方法的代码很多，这里截取了关键的部分。
注释1处通过getProcessRecordLocked方法来获取目标ContentProvider的应用程序进程信息，这些信息用ProcessRecord类型的proc来表示，
如果该应用进程已经启动就会调用注释2处的代码，否则就会调用注释3的startProcessLocked方法来启动进程。
这里我们假设ContentProvider的应用进程还没有启动，
关于应用进程启动过程，我在Android应用程序进程启动过程（前篇）已经讲过，最终会调用ActivityThread的main方法，代码如下所示。
 //关于新建进程，最终通过AMS向zygote创建新进程

frameworks/base/core/java/android/app/ActivityThread.java
```
public static void main(String[] args) {
     ...
       Looper.prepareMainLooper();//1
       ActivityThread thread = new ActivityThread();//2
       thread.attach(false);
       if (sMainThreadHandler == null) {
           sMainThreadHandler = thread.getHandler();
       }
       if (false) {
           Looper.myLooper().setMessageLogging(new
                   LogPrinter(Log.DEBUG, "ActivityThread"));
       }
       // End of event ActivityThreadMain.
       Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
       Looper.loop();//3
       throw new RuntimeException("Main thread loop unexpectedly exited");
   }
```

注释1处通过prepareMainLooper方法在ThreadLocal中获取Looper，并在注释3处开启消息循环。在注释2处创建了ActivityThread并调用了它的attach方法：
frameworks/base/core/java/android/app/ActivityThread.java
```
  private void attach(boolean system) {
  ...
    final IActivityManager mgr = ActivityManagerNative.getDefault();//1
            try {
                mgr.attachApplication(mAppThread);//2
            } catch (RemoteException ex) {
                throw ex.rethrowFromSystemServer();
            }
  ...          
}
```
注释1处最终会得到AMS，在注释2处调用AMS的attachApplication方法，并将ApplicationThread类型的mAppThread对象传进去。
query方法到AMS的调用过程，如下面时序图所示（省略应用程序进程启动过程）
ContentResolver到AMS过程.png



AMS启动Content Provider的过程
我们接着来查看AMS的attachApplication方法，如下所示。
frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java
```
@Override
public final void attachApplication(IApplicationThread thread) {
    synchronized (this) {
        int callingPid = Binder.getCallingPid();
        final long origId = Binder.clearCallingIdentity();
        attachApplicationLocked(thread, callingPid);
        Binder.restoreCallingIdentity(origId);
    }
}
```

attachApplication方法中又调用了attachApplicationLocked方法：
frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java
```
   private final boolean attachApplicationLocked(IApplicationThread thread,
            int pid) {
   ...
   thread.bindApplication(processName, appInfo, providers, app.instrumentationClass,
                    profilerInfo, app.instrumentationArguments, app.instrumentationWatcher,
                    app.instrumentationUiAutomationConnection, testMode,
                    mBinderTransactionTrackingEnabled, enableTrackAllocation,
                    isRestrictedBackupMode || !normalMode, app.persistent,
                    new Configuration(mConfiguration), app.compat,
                    getCommonServicesLocked(app.isolated),
                    mCoreSettingsObserver.getCoreSettingsLocked());
...
}
```

attachApplicationLocked方法中调用了thread的bindApplication方法，thread是IApplicationThread类型的，
从类型名字就可以看出来是用于进程间通信，这里实现bindApplication方法的是ApplicationThreadProxy类，它实现了IApplicationThread接口。
frameworks/base/core/java/android/app/ApplicationThreadNative.java
```
class ApplicationThreadProxy implements IApplicationThread {
...
    @Override
    public final void bindApplication(String packageName, ApplicationInfo info,
            List<ProviderInfo> providers, ComponentName testName, ProfilerInfo profilerInfo,
            Bundle testArgs, IInstrumentationWatcher testWatcher,
            IUiAutomationConnection uiAutomationConnection, int debugMode,
            boolean enableBinderTracking, boolean trackAllocation, boolean restrictedBackupMode,
            boolean persistent, Configuration config, CompatibilityInfo compatInfo,
            Map<String, IBinder> services, Bundle coreSettings) throws RemoteException {
      ...
        //private final IBinder mRemote;
        mRemote.transact(BIND_APPLICATION_TRANSACTION, data, null,
                IBinder.FLAG_ONEWAY);
        data.recycle();
    }
...
}
```

到目前为止，上面的调用过程还是在AMS进程中执行的，因此，需要通过IBinder类型的mRemote对象向新创建的应用程序进程（目标Content Provider所在的进程）
发送BIND_APPLICATION_TRANSACTION类型的通信请求。处理这个通信请求的是在新创建的应用程序进程中执行的ApplicationThread的bindApplication方法，如下所示。
frameworks/base/core/java/android/app/ActivityThread.java
```
public final void bindApplication(String processName, ApplicationInfo appInfo,
               List<ProviderInfo> providers, ComponentName instrumentationName,
               ProfilerInfo profilerInfo, Bundle instrumentationArgs,
               IInstrumentationWatcher instrumentationWatcher,
               IUiAutomationConnection instrumentationUiConnection, int debugMode,
               boolean enableBinderTracking, boolean trackAllocation,
               boolean isRestrictedBackupMode, boolean persistent, Configuration config,
               CompatibilityInfo compatInfo, Map<String, IBinder> services, Bundle coreSettings) {
               ...
               sendMessage(H.BIND_APPLICATION, data);
       }
```

调用sendMessage方法像H发送BIND_APPLICATION类型消息，H的handleMessage方法如下所示。
frameworks/base/core/java/android/app/ActivityThread.java
```
   public void handleMessage(Message msg) {
    if (DEBUG_MESSAGES) Slog.v(TAG, ">>> handling: " + codeToString(msg.what));
            switch (msg.what) {
            ...
            case BIND_APPLICATION:
                    Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, "bindApplication");
                    AppBindData data = (AppBindData)msg.obj;
                    handleBindApplication(data);
                    Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
                    break;
  ...
  }
  ... 
}
```
我们接着查看handleBindApplication方法：
frameworks/base/core/java/android/app/ActivityThread.java
```
private void handleBindApplication(AppBindData data) {
 ...
      final ContextImpl appContext = ContextImpl.createAppContext(this, data.info);//1
       try {
              final ClassLoader cl = instrContext.getClassLoader();
              mInstrumentation = (Instrumentation)
                  cl.loadClass(data.instrumentationName.getClassName()).newInstance();//2
          } catch (Exception e) {
           ...
          }
          final ComponentName component = new ComponentName(ii.packageName, ii.name);
          mInstrumentation.init(this, instrContext, appContext, component,
                  data.instrumentationWatcher, data.instrumentationUiAutomationConnection);//3
         ...
          Application app = data.info.makeApplication(data.restrictedBackupMode, null);//4
          mInitialApplication = app;
          if (!data.restrictedBackupMode) {
              if (!ArrayUtils.isEmpty(data.providers)) {
                  installContentProviders(app, data.providers);//5
                  mH.sendEmptyMessageDelayed(H.ENABLE_JIT, 10*1000);
              }
          }
        ...
         mInstrumentation.callApplicationOnCreate(app);//6
        ... 
}
```
Instrumentation   [ˌɪnstrəmenˈteɪʃn]  仪器;仪表;仪器仪表;检测;插装
handleBindApplication方法的代码很长，这里截取了主要的部分。注释1处创建了ContextImpl 。
注释2处通过反射创建Instrumentation并在注释3处初始化Instrumentation。
注释4处创建Application并且在注释6处调用Application的onCreate方法，这意味着Content Provider所在的应用程序进程已经启动完毕，
在这之前，注释5处调用installContentProviders方法来启动Content Provider，代码如下所示。
frameworks/base/core/java/android/app/ActivityThread.java
```
private void installContentProviders(
        Context context, List<ProviderInfo> providers) {
    final ArrayList<IActivityManager.ContentProviderHolder> results =
        new ArrayList<IActivityManager.ContentProviderHolder>();

    for (ProviderInfo cpi : providers) {//1
        ...
        IActivityManager.ContentProviderHolder cph = installProvider(context, null, cpi,
                false /*noisy*/, true /*noReleaseNeeded*/, true /*stable*/);//2
      ...
    }

    try {
        ActivityManagerNative.getDefault().publishContentProviders(
            getApplicationThread(), results);//3
    } catch (RemoteException ex) {
        throw ex.rethrowFromSystemServer();
    }
}
```
/frameworks/base/core/java/android/app/IActivityManager.java
String descriptor = "android.app.IActivityManager";
//android7.0
ActivityManagerNative
 /frameworks/base/core/java/android/app/ActivityManagerNative.java
```
  public void publishContentProviders(IApplicationThread caller,
              List<ContentProviderHolder> providers) throws RemoteException
      {
          Parcel data = Parcel.obtain();
          Parcel reply = Parcel.obtain();
          data.writeInterfaceToken(IActivityManager.descriptor);
          data.writeStrongBinder(caller != null ? caller.asBinder() : null);
          data.writeTypedList(providers);
          mRemote.transact(PUBLISH_CONTENT_PROVIDERS_TRANSACTION, data, reply, 0);
          reply.readException();
          data.recycle();
          reply.recycle();
      }
```

ActivityManagerService的publishContentProviders 
/frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java
```
   public final void publishContentProviders(IApplicationThread caller,
              List<ContentProviderHolder> providers) {
          synchronized (this) {
              final ProcessRecord r = getRecordForAppLocked(caller);
              final long origId = Binder.clearCallingIdentity();
              final int N = providers.size();
              for (int i = 0; i < N; i++) {
                  ContentProviderHolder src = providers.get(i);
                  if (src == null || src.info == null || src.provider == null) {
                      continue;
                  }
                  ContentProviderRecord dst = r.pubProviders.get(src.info.name);
                  if (dst != null) {
                      ComponentName comp = new ComponentName(dst.info.packageName, dst.info.name);
                      mProviderMap.putProviderByClass(comp, dst);
                      String names[] = dst.info.authority.split(";");
                     for (int j = 0; j < names.length; j++) {
                          mProviderMap.putProviderByName(names[j], dst);
                      }
                  }
              }
 
          }
     }
```
注释1处遍历当前应用程序进程的ProviderInfo列表，得到每个Content Provider的ProviderInfo（存储Content Provider的信息），
并在注释2处调用installProvider方法来启动这些Content Provider。
在注释3处通过AMS的publishContentProviders方法将这些Content Provider存储在AMS的mProviderMap中，这个mProviderMap在前面提到过，
起到缓存的作用，防止每次使用相同的Content Provider时都会调用AMS的getContentProvider方法。
来查看installProvider方法时如何启动Content Provider的，installProvider方法如下所示
frameworks/base/core/java/android/app/ActivityThread.java
```
private IActivityManager.ContentProviderHolder installProvider(Context context,
           IActivityManager.ContentProviderHolder holder, ProviderInfo info,
           boolean noisy, boolean noReleaseNeeded, boolean stable) {
       ContentProvider localProvider = null;
  ...
               final java.lang.ClassLoader cl = c.getClassLoader();
               localProvider = (ContentProvider)cl.
                   loadClass(info.name).newInstance();//1
               provider = localProvider.getIContentProvider();
               if (provider == null) {
                 ...
                   return null;
               }
               if (DEBUG_PROVIDER) Slog.v(
                   TAG, "Instantiating local provider " + info.name);
               localProvider.attachInfo(c, info);//2
           } catch (java.lang.Exception e) {
              ...
               }
               return null;
           }
       }
          ...
       return retHolder;
   }
```
在注释1处通过反射来创建ContentProvider类型的localProvider对象，并在注释2处调用了它的attachInfo方法：
frameworks/base/core/java/android/content/ContentProvider.java
```
private void attachInfo(Context context, ProviderInfo info, boolean testing) {
     ...
          ContentProvider.this.onCreate();
      }
  }
```
在attachInfo方法中调用了onCreate方法，它是一个抽象方法。这样Content Provider就启动完毕。
最后给出AMS启动Content Provider的时序图
AMS启动Content_Provider.png