http://liuwangshu.cn/framework/booting/3-syetemserver.html  android7.0

Zygote启动SyetemServer进程
在上一篇文章中我们讲到在ZygoteInit.java的startSystemServer函数中启动了SyetemServer进程，如下所示。
frameworks/base/core/java/com/android/internal/os/ZygoteInit.java
```
private static boolean startSystemServer(String abiList, String socketName)
           throws MethodAndArgsCaller, RuntimeException {
    ...
       if (pid == 0) {
           if (hasSecondZygote(abiList)) {
               waitForSecondaryZygote(socketName);
           }
           handleSystemServerProcess(parsedArgs);
       }
       return true;
   }
```
在startSystemServer函数中调用handleSystemServerProcess来启动SyetemServer进程


SyetemServer进程启动过程
handleSystemServerProcess函数的代码如下所示
```
private static void handleSystemServerProcess(
          ZygoteConnection.Arguments parsedArgs)
          throws ZygoteInit.MethodAndArgsCaller {
      closeServerSocket();//1
    ...
      if (parsedArgs.invokeWith != null) {
         ...
      } else {
          ClassLoader cl = null;
          if (systemServerClasspath != null) {
              cl = createSystemServerClassLoader(systemServerClasspath,
                                                 parsedArgs.targetSdkVersion);
              Thread.currentThread().setContextClassLoader(cl);
          }
          RuntimeInit.zygoteInit(parsedArgs.targetSdkVersion, parsedArgs.remainingArgs, cl);//2
      }
  }
```
SyetemServer进程是复制了Zygote进程的地址空间，因此也会得到Zygote进程创建的Socket，这个Socket对于SyetemServer进程没有用处，
   因此，需要注释1处的代码来关闭该Socket。
在注释2处调用RuntimeInit的zygoteInit函数，它的代码如下所示
/frameworks/base/core/java/com/android/internal/os/RuntimeInit.java
```
public static final void zygoteInit(int targetSdkVersion, String[] argv, ClassLoader classLoader)
            throws ZygoteInit.MethodAndArgsCaller {
        if (DEBUG) Slog.d(TAG, "RuntimeInit: Starting application from zygote");
        Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, "RuntimeInit");
        redirectLogStreams();
        commonInit();
        nativeZygoteInit();//1
        applicationInit(targetSdkVersion, argv, classLoader);//2
    }
```

注释1处调用nativeZygoteInit函数，一看函数的名称就知道调用Native层的代码
启动Binder线程池
接着我们来查看nativeZygoteInit函数对用的JNI文件，如下所示。
frameworks/base/core/jni/AndroidRuntime.cpp
```
static const JNINativeMethod gMethods[] = {
    { "nativeFinishInit", "()V",
        (void*) com_android_internal_os_RuntimeInit_nativeFinishInit },
    { "nativeZygoteInit", "()V",
        (void*) com_android_internal_os_RuntimeInit_nativeZygoteInit },
    { "nativeSetExitWithoutCleanup", "(Z)V",
        (void*) com_android_internal_os_RuntimeInit_nativeSetExitWithoutCleanup },
};
```
通过JNI的gMethods数组，可以看出nativeZygoteInit函数对应的是JNI文件AndroidRuntime.cpp的com_android_internal_os_RuntimeInit_nativeZygoteInit函数：
```
...
static AndroidRuntime* gCurRuntime = NULL;
...
static void com_android_internal_os_RuntimeInit_nativeZygoteInit(JNIEnv* env, jobject clazz)
{
    gCurRuntime->onZygoteInit();
}

```
这里gCurRuntime是AndroidRuntime类型的指针，AndroidRuntime的子类AppRuntime在app_main.cpp中定义，
我们来查看AppRuntime的onZygoteInit函数，代码如下所示。
frameworks/base/cmds/app_process/app_main.cpp
```
virtual void onZygoteInit()
   {
       sp<ProcessState> proc = ProcessState::self();
       ALOGV("App process: starting thread pool.\n");
       proc->startThreadPool();//1
   }
```
注释1处的代码用来启动一个Binder线程池，这样SyetemServer进程就可以使用Binder来与其他进程进行通信了。
看到这里我们知道RuntimeInit.java的nativeZygoteInit函数主要做的就是启动Binder线程池



invokeStaticMain
我们再回到RuntimeInit.java的代码，在注释2处调用了applicationInit函数，代码如下所示。
frameworks/base/core/java/com/android/internal/os/RuntimeInit.java
```
  private static void applicationInit(int targetSdkVersion, String[] argv, ClassLoader classLoader)
            throws ZygoteInit.MethodAndArgsCaller {
...
        invokeStaticMain(args.startClass, args.startArgs, classLoader);
    }
```

applicationInit函数中主要调用了invokeStaticMain函数：
```
private static void invokeStaticMain(String className, String[] argv, ClassLoader classLoader)
         throws ZygoteInit.MethodAndArgsCaller {
     Class<?> cl;
     try {
         cl = Class.forName(className, true, classLoader);//1
     } catch (ClassNotFoundException ex) {
         throw new RuntimeException(
                 "Missing class when invoking static main " + className,
                 ex);
     }
     Method m;
     try {
         m = cl.getMethod("main", new Class[] { String[].class });//2
     } catch (NoSuchMethodException ex) {
         throw new RuntimeException(
                 "Missing static main on " + className, ex);
     } catch (SecurityException ex) {
         throw new RuntimeException(
                 "Problem getting static main on " + className, ex);
     }
     int modifiers = m.getModifiers();
     if (! (Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers))) {
         throw new RuntimeException(
                 "Main method is not public and static on " + className);
     }
     throw new ZygoteInit.MethodAndArgsCaller(m, argv);//3   m为main函数
 }
```

注释1处className为“com.android.server.SystemServer”，因此通过反射返回的cl为SystemServer类。
注释2处找到SystemServer中的main函数。
在注释3处将找到的main函数传入到MethodAndArgsCaller异常中并抛出该异常。
  截获MethodAndArgsCaller异常的代码在ZygoteInit.java的main函数中，如下所示     可以查看zygote启动
frameworks/base/core/java/com/android/internal/os/ZygoteInit.java

```
public static void main(String argv[]) {
     ...
          closeServerSocket();
      } catch (MethodAndArgsCaller caller) {
          caller.run();//1
      } catch (RuntimeException ex) {
          Log.e(TAG, "Zygote died with exception", ex);
          closeServerSocket();
          throw ex;
      }
  }
```
在注释1处调用了MethodAndArgsCaller的run函数
```
   public void run() {
        try {
            mMethod.invoke(null, new Object[] { mArgs });
        } catch (IllegalAccessException ex) {
         ...
        }
    }
}
```

这里mMethod指的就是SystemServer的main函数，因此main函数被动态调用。       main函数被当做异常抛出，捕获后执行



解析SyetemServer进程
我们先来查看SystemServer的main函数：
frameworks/base/services/java/com/android/server/SystemServer.java
```
public static void main(String[] args) {
       new SystemServer().run();
   }
```

run函数
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
run函数代码很多，关键就是在注释1处加载了libandroid_servers.so。  
接下来在注释2处创建SystemServiceManager，它会对系统的服务进行创建、启动和生命周期管理。启动系统的各种服务，
在注释3中的startBootstrapServices函数中用SystemServiceManager启动了ActivityManagerService、PowerManagerService、PackageManagerService等服务。
在注释4处的函数中则启动了BatteryService、UsageStatsService和WebViewUpdateService。
注释5处的startOtherServices函数中则启动了CameraService、AlarmManagerService、VrManagerService等服务，
   这些服务的父类为SystemService。
从注释3、4、5的函数可以看出，官方把系统服务分为了三种类型，分别是引导服务、核心服务和其他服务，其中其他服务为一些非紧要和一些不需要立即启动的服务。
系统服务大约有80多个，这里列出部分系统服务以及它们的作用如下表所示：
引导服务	作用
Installer	系统安装apk时的一个服务类，启动完成Installer服务之后才能启动其他的系统服务
ActivityManagerService	负责四大组件的启动、切换、调度。
PowerManagerService	计算系统中和Power相关的计算，然后决策系统应该如何反应
LightsService	管理和显示背光LED
DisplayManagerService	用来管理所有显示设备
UserManagerService	多用户模式管理
SensorService	为系统提供各种感应器服务
PackageManagerService	用来对apk进行安装、解析、删除、卸载等等操作
核心服务
BatteryService	管理电池相关的服务
UsageStatsService	收集用户使用每一个APP的频率、使用时常
WebViewUpdateService	WebView更新服务
其他服务
CameraService	摄像头相关服务
AlarmManagerService	全局定时器管理服务
InputManagerService	管理输入事件
WindowManagerService	窗口管理服务
VrManagerService	VR模式管理服务
BluetoothService	蓝牙管理服务
NotificationManagerService	通知管理服务
DeviceStorageMonitorService	存储相关管理服务
LocationManagerService	定位管理服务
AudioService	音频相关管理服务


比如要启动PowerManagerService则会调用如下代码：
```
mPowerManagerService = mSystemServiceManager.startService(PowerManagerService.class);
```

SystemServiceManager的startService函数启动了PowerManagerService，startService函数如下所示。
frameworks/base/services/core/java/com/android/server/SystemServiceManager.java
```
  public <T extends SystemService> T startService(Class<T> serviceClass) {
  ...
            final T service;
            try {
                Constructor<T> constructor = serviceClass.getConstructor(Context.class);
                service = constructor.newInstance(mContext);//1
            } catch (InstantiationException ex) {
                throw new RuntimeException("Failed to create service " + name
                        + ": service could not be instantiated", ex);
            }
...
            // Register it.
            mServices.add(service);//2
            // Start it.
            try {
                service.onStart();
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

除了用mSystemServiceManager的startService函数来启动系统服务外，也可以通过如下形式来启动系统服务，以PackageManagerService为例：
```
mPackageManagerService = PackageManagerService.main(mSystemContext, installer,
mFactoryTestMode != FactoryTest.FACTORY_TEST_OFF, mOnlyCore);
```
直接调用了PackageManagerService的main函数：
frameworks/base/services/core/java/com/android/server/pm/PackageManagerService.java
```
public static PackageManagerService main(Context context, Installer installer,
        boolean factoryTest, boolean onlyCore) {
    // Self-check for initial settings.
    PackageManagerServiceCompilerMapping.checkProperties();
    PackageManagerService m = new PackageManagerService(context, installer,
            factoryTest, onlyCore);//1
    m.enableSystemUserPackages();
    // Disable any carrier apps. We do this very early in boot to prevent the apps from being
    // disabled after already being started.
    CarrierAppUtils.disableCarrierAppsUntilPrivileged(context.getOpPackageName(), m,
            UserHandle.USER_SYSTEM);
    ServiceManager.addService("package", m);//2
    return m;
}
```

注释1处直接创建PackageManagerService并在注释2处将PackageManagerService注册到ServiceManager中，
ServiceManager用来管理系统中的各种Service，用于系统C/S架构中的Binder机制通信：Client端要使用某个Service，
  则需要先到ServiceManager查询Service的相关信息，然后根据Service的相关信息与Service所在的Server进程建立通讯通路，
  这样Client端就可以使用Service了。还有的服务是直接注册到ServiceManager中的，如下所示。
frameworks/base/services/java/com/android/server/SystemServer.java
```
telephonyRegistry = new TelephonyRegistry(context);
ServiceManager.addService("telephony.registry", telephonyRegistry);
```

总结SyetemServer进程
SyetemServer在启动时做了如下工作：
1.启动Binder线程池，这样就可以与其他进程进行通信。
2.创建SystemServiceManager用于对系统的服务进行创建、启动和生命周期管理。
3.启动各种系统服务。



/frameworks/base/services/core/java/com/android/server/SystemService.java
```
SystemService{
  
  public abstract void onStart()
}
```

