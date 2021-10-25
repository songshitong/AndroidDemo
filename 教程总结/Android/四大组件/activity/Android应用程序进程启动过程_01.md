http://liuwangshu.cn/framework/applicationprocess/1.html   android7.0
在此前我讲过Android系统的启动流程，系统启动后，我们就比较关心应用程序是如何启动的，这一篇我们来一起学习Android7.0 应用程序进程启动过程，
需要注意的是“应用程序进程启动过程”，而不是应用程序启动过程。关于应用程序启动过程，我会在后续系列的文章中讲到

应用程序进程概述
要想启动一个应用程序，首先要保证这个应用程序所需要的应用程序进程已经被启动。ActivityManagerService在启动应用程序时会检查这个应用程序需要的应用程序进程是否存在，
不存在就会请求Zygote进程将需要的应用程序进程启动。在Android系统启动流程（二）解析Zygote进程启动过程这篇文章中，我提到了Zygote的Java框架层中，
会创建一个Server端的Socket，这个Socket用来等待ActivityManagerService来请求Zygote来创建新的应用程序进程的。
我们知道Zygote进程通过fock自身创建的应用程序进程，这样应用程序程序进程就会获得Zygote进程在启动时创建的虚拟机实例。
当然，在应用程序创建过程中除了获取虚拟机实例，还可以获得Binder线程池和消息循环，这样运行在应用进程中应用程序就可以方便的使用Binder进行进程间通信以及消息处理机制了。
关于Binder线程池和消息循环是如何启动或者创建的会在下一篇文章给出答案。先给出应用程序进程启动过程的时序图，然后对每一个步骤进行详细分析，如下图所示
应用程序进程启动过程.png

//todo android uid机制
应用程序进程创建过程
发送创建应用程序进程请求
ActivityManagerService会通过调用startProcessLocked函数来向Zygote进程发送请求，如下所示。
frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java
```
private final void startProcessLocked(ProcessRecord app, String hostingType,
          String hostingNameStr, String abiOverride, String entryPoint, String[] entryPointArgs) {
      ...
      try {
          try {
              final int userId = UserHandle.getUserId(app.uid);
              AppGlobals.getPackageManager().checkPackageStartable(app.info.packageName, userId);
          } catch (RemoteException e) {
              throw e.rethrowAsRuntimeException();
          }

          int uid = app.uid;//1
          int[] gids = null;
          int mountExternal = Zygote.MOUNT_EXTERNAL_NONE;
          if (!app.isolated) {
            ...
            /**
            * 2 对gids进行创建和赋值
            */
              if (ArrayUtils.isEmpty(permGids)) {
                  gids = new int[2];
              } else {
                  gids = new int[permGids.length + 2];
                  System.arraycopy(permGids, 0, gids, 2, permGids.length);
              }
              gids[0] = UserHandle.getSharedAppGid(UserHandle.getAppId(uid));
              gids[1] = UserHandle.getUserGid(UserHandle.getUserId(uid));
          }
       
         ...
          if (entryPoint == null) entryPoint = "android.app.ActivityThread";//3
          Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, "Start proc: " +
                  app.processName);
          checkTime(startTime, "startProcess: asking zygote to start proc");
          /**
          * 4
          */
          Process.ProcessStartResult startResult = Process.start(entryPoint,
                  app.processName, uid, uid, gids, debugFlags, mountExternal,
                  app.info.targetSdkVersion, app.info.seinfo, requiredAbi, instructionSet,
                  app.info.dataDir, entryPointArgs);
         ...
      } catch (RuntimeException e) {
        ...
      }
  }
 ...
  }

```


在注释1处的达到创建应用程序进程的用户ID，在注释2处对用户组ID：gids进行创建和赋值。注释3处如果entryPoint 为null则赋值为”android.app.ActivityThread”。
在注释4处调用Process的start函数，将此前得到的应用程序进程用户ID和用户组ID传进去，第一个参数entryPoint我们得知是”android.app.ActivityThread”，
后文会再次提到它。接下来我们来查看Process的start函数，如下所示。
frameworks/base/core/java/android/os/Process.java
```
public static final ProcessStartResult start(final String processClass,
                              final String niceName,
                              int uid, int gid, int[] gids,
                              int debugFlags, int mountExternal,
                              int targetSdkVersion,
                              String seInfo,
                              String abi,
                              String instructionSet,
                              String appDataDir,
                              String[] zygoteArgs) {
    try {
        return startViaZygote(processClass, niceName, uid, gid, gids,
                debugFlags, mountExternal, targetSdkVersion, seInfo,
                abi, instructionSet, appDataDir, zygoteArgs);
    } catch (ZygoteStartFailedEx ex) {
      ...
    }
}
```

start函数中只调用了startViaZygote函数：
frameworks/base/core/java/android/os/Process.java
```
private static ProcessStartResult startViaZygote(final String processClass,
                               final String niceName,
                               final int uid, final int gid,
                               final int[] gids,
                               int debugFlags, int mountExternal,
                               int targetSdkVersion,
                               String seInfo,
                               String abi,
                               String instructionSet,
                               String appDataDir,
                               String[] extraArgs)
                               throws ZygoteStartFailedEx {
     synchronized(Process.class) {
     /**
     * 1
     */
         ArrayList<String> argsForZygote = new ArrayList<String>();
         argsForZygote.add("--runtime-args");
         argsForZygote.add("--setuid=" + uid);
         argsForZygote.add("--setgid=" + gid);
       ...
         if (gids != null && gids.length > 0) {
             StringBuilder sb = new StringBuilder();
             sb.append("--setgroups=");

             int sz = gids.length;
             for (int i = 0; i < sz; i++) {
                 if (i != 0) {
                     sb.append(',');
                 }
                 sb.append(gids[i]);
             }

             argsForZygote.add(sb.toString());
         }
      ...
         argsForZygote.add(processClass);
         if (extraArgs != null) {
             for (String arg : extraArgs) {
                 argsForZygote.add(arg);
             }
         }
         return zygoteSendArgsAndGetResult(openZygoteSocketIfNeeded(abi), argsForZygote);
     }
 }
```

在注释1处创建了字符串列表argsForZygote ，并将启动应用进程的启动参数保存在argsForZygote中，函数的最后会调用zygoteSendArgsAndGetResult函数，
需要注意的是，zygoteSendArgsAndGetResult函数中第一个参数中调用了openZygoteSocketIfNeeded函数，而第二个参数是保存应用进程的启动参数的argsForZygote。
zygoteSendArgsAndGetResult函数如下所示。
frameworks/base/core/java/android/os/Process.java
```
private static ProcessStartResult zygoteSendArgsAndGetResult(
          ZygoteState zygoteState, ArrayList<String> args)
          throws ZygoteStartFailedEx {
      try {
          final BufferedWriter writer = zygoteState.writer;
          final DataInputStream inputStream = zygoteState.inputStream;
          writer.write(Integer.toString(args.size()));
          writer.newLine();
          int sz = args.size();
          for (int i = 0; i < sz; i++) {
              String arg = args.get(i);
              if (arg.indexOf('\n') >= 0) {
                  throw new ZygoteStartFailedEx(
                          "embedded newlines not allowed");
              }
              writer.write(arg);
              writer.newLine();
          }
          writer.flush();
          // Should there be a timeout on this?
          ProcessStartResult result = new ProcessStartResult();
          result.pid = inputStream.readInt();
          if (result.pid < 0) {
              throw new ZygoteStartFailedEx("fork() failed");
          }
          result.usingWrapper = inputStream.readBoolean();
          return result;
      } catch (IOException ex) {
          zygoteState.close();
          throw new ZygoteStartFailedEx(ex);
      }
  }
```
//todo ZygoteState的socket
zygoteSendArgsAndGetResult函数主要做的就是将传入的应用进程的启动参数argsForZygote，写入到ZygoteState中，
结合上文我们知道ZygoteState其实是由openZygoteSocketIfNeeded函数返回的，那么我们接着来看openZygoteSocketIfNeeded函数，代码如下所示。
frameworks/base/core/java/android/os/Process.java
```
private static ZygoteState openZygoteSocketIfNeeded(String abi) throws ZygoteStartFailedEx {
    if (primaryZygoteState == null || primaryZygoteState.isClosed()) {
        try {
            primaryZygoteState = ZygoteState.connect(ZYGOTE_SOCKET);//1
        } catch (IOException ioe) {
            throw new ZygoteStartFailedEx("Error connecting to primary zygote", ioe);
        }
    }
    if (primaryZygoteState.matches(abi)) {//2
        return primaryZygoteState;
    }
    // The primary zygote didn't match. Try the secondary.
    if (secondaryZygoteState == null || secondaryZygoteState.isClosed()) {
        try {
        secondaryZygoteState = ZygoteState.connect(SECONDARY_ZYGOTE_SOCKET);//3
        } catch (IOException ioe) {
            throw new ZygoteStartFailedEx("Error connecting to secondary zygote", ioe);
        }
    }

    if (secondaryZygoteState.matches(abi)) {
        return secondaryZygoteState;
    }

    throw new ZygoteStartFailedEx("Unsupported zygote ABI: " + abi);
```
//todo 查看connect函数
在讲到Zygote进程启动过程时我们得知，在Zygote的main函数中会创建name为“zygote”的Server端Socket。
在注释1处会调用ZygoteState的connect函数与名称为ZYGOTE_SOCKET的Socket建立连接，这里ZYGOTE_SOCKET的值为“zygote”。
注释2处如果连接name为“zygote”的Socket返回的primaryZygoteState与当前的abi不匹配，则会在注释3处连接name为“zygote_secondary”的Socket。
这两个Socket区别就是：name为”zygote”的Socket是运行在64位Zygote进程中的，而name为“zygote_secondary”的Socket则运行在32位Zygote进程中。
既然应用程序进程是通过Zygote进程fock产生的，当要连接Zygote中的Socket时，也需要保证位数的一致

接收请求并创建应用程序进程
Socket进行连接成功并匹配abi后会返回ZygoteState类型对象，我们在分析zygoteSendArgsAndGetResult函数中讲过，
会将应用进程的启动参数argsForZygote写入到ZygoteState中，这样Zygote进程就会收到一个创建新的应用程序进程的请求，
我们回到ZygoteInit的main函数，如下所示。
frameworks/base/core/java/com/android/internal/os/ZygoteInit.java
```
public static void main(String argv[]) {
       ...
        try {
         ...       
            //注册Zygote用的Socket
            registerZygoteSocket(socketName);//1
           ...
           //预加载类和资源
           preload();//2
           ...
            if (startSystemServer) {
            //启动SystemServer进程
                startSystemServer(abiList, socketName);//3
            }
            Log.i(TAG, "Accepting command socket connections");
            //等待客户端请求
            runSelectLoop(abiList);//4
            closeServerSocket();
        } catch (MethodAndArgsCaller caller) {
            caller.run();
        } catch (RuntimeException ex) {
            Log.e(TAG, "Zygote died with exception", ex);
            closeServerSocket();
            throw ex;
        }
    }
```

这些内容在Android系统启动流程（二）解析Zygote进程启动过程讲过，但为了更好的理解我再讲一遍。
注释1处通过registerZygoteSocket函数来创建一个Server端的Socket，
  这个name为”zygote”的Socket用来等待ActivityManagerService来请求Zygote来创建新的应用程序进程。
注释2处用来预加载类和资源。注释3处用来启动SystemServer进程，这样系统的关键服务也会由SystemServer进程启动起来。
注释4处调用runSelectLoop函数来等待ActivityManagerService的请求。我们就来查看runSelectLoop函数：
** frameworks/base/core/java/com/android/internal/os/ZygoteInit.java**
```
private static void runSelectLoop(String abiList) throws MethodAndArgsCaller {
       ArrayList<FileDescriptor> fds = new ArrayList<FileDescriptor>();
       ArrayList<ZygoteConnection> peers = new ArrayList<ZygoteConnection>();//2
       fds.add(sServerSocket.getFileDescriptor());
       peers.add(null);
       while (true) {
       ...
           for (int i = pollFds.length - 1; i >= 0; --i) {
               if ((pollFds[i].revents & POLLIN) == 0) {
                   continue;
               }
               if (i == 0) {
                   ZygoteConnection newPeer = acceptCommandPeer(abiList);
                   peers.add(newPeer);
                   fds.add(newPeer.getFileDesciptor());
               } else {
                   boolean done = peers.get(i).runOnce();//1
                   if (done) {
                       peers.remove(i);
                       fds.remove(i);
                   }
               }
           }
       }
   }
```

当有ActivityManagerService的请求数据到来时会调用注释1处的代码，结合注释2处的代码，
我们得知注释1处的代码其实是调用ZygoteConnection的runOnce函数来处理请求的数据：
frameworks/base/core/java/com/android/internal/os/ZygoteConnection.java
```
 boolean runOnce() throws ZygoteInit.MethodAndArgsCaller {
        String args[];
        Arguments parsedArgs = null;
        FileDescriptor[] descriptors;
        try {
            args = readArgumentList();//1
            descriptors = mSocket.getAncillaryFileDescriptors();
        } catch (IOException ex) {
            Log.w(TAG, "IOException on command socket " + ex.getMessage());
            closeSocket();
            return true;
        }
...
        try {
            parsedArgs = new Arguments(args);//2
        ...
        /**
        * 3 
        */
            pid = Zygote.forkAndSpecialize(parsedArgs.uid, parsedArgs.gid, parsedArgs.gids,
                    parsedArgs.debugFlags, rlimits, parsedArgs.mountExternal, parsedArgs.seInfo,
                    parsedArgs.niceName, fdsToClose, parsedArgs.instructionSet,
                    parsedArgs.appDataDir);
        } catch (ErrnoException ex) {
          ....
        }
       try {
            if (pid == 0) {
                // in child
                IoUtils.closeQuietly(serverPipeFd);
                serverPipeFd = null;
                handleChildProc(parsedArgs, descriptors, childPipeFd, newStderr);
                return true;
            } else {
                // in parent...pid of < 0 means failure
                IoUtils.closeQuietly(childPipeFd);
                childPipeFd = null;
                return handleParentProc(pid, descriptors, serverPipeFd, parsedArgs);
            }
        } finally {
            IoUtils.closeQuietly(childPipeFd);
            IoUtils.closeQuietly(serverPipeFd);
        }
    }
```
//todo forkAndSpecialize
在注释1处调用readArgumentList函数来获取应用程序进程的启动参数，并在注释2处将readArgumentList函数返回的字符串封装到Arguments对象parsedArgs中。
注释3处调用Zygote的forkAndSpecialize函数来创建应用程序进程，参数为parsedArgs中存储的应用进程启动参数，返回值为pid。
forkAndSpecialize函数主要是通过fork当前进程来创建一个子进程的，如果pid等于0，则说明是在新创建的子进程中执行的，
就会调用handleChildProc函数来启动这个子进程也就是应用程序进程，如下所示。
frameworks/base/core/java/com/android/internal/os/ZygoteConnection.java
```
private void handleChildProc(Arguments parsedArgs,
           FileDescriptor[] descriptors, FileDescriptor pipeFd, PrintStream newStderr)
           throws ZygoteInit.MethodAndArgsCaller {
     ...
           RuntimeInit.zygoteInit(parsedArgs.targetSdkVersion,
                   parsedArgs.remainingArgs, null /* classLoader */);
       }
   }
```

handleChildProc函数中调用了RuntimeInit的zygoteInit函数，如下所示。
frameworks/base/core/java/com/android/internal/os/RuntimeInit.java
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

注释1处会在新创建的应用程序进程中创建Binder线程池，这个在下一篇文章会详细介绍。在注释2处调用了applicationInit函数：
frameworks/base/core/java/com/android/internal/os/RuntimeInit.java
```
 private static void applicationInit(int targetSdkVersion, String[] argv, ClassLoader classLoader)
           throws ZygoteInit.MethodAndArgsCaller {
...
       final Arguments args;
       try {
           args = new Arguments(argv);
       } catch (IllegalArgumentException ex) {
           Slog.e(TAG, ex.getMessage());       
           return;
       }
       Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
       invokeStaticMain(args.startClass, args.startArgs, classLoader);//1
   }
```

在applicationInit中会在注释1处调用invokeStaticMain函数，需要注意的是第一个参数args.startClass，这里指的就是此篇文章开头提到的参数：
  android.app.ActivityThread。接下来我们查看invokeStaticMain函数，如下所示。
frameworks/base/core/java/com/android/internal/os/RuntimeInit.java
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
    }
    ...
    throw new ZygoteInit.MethodAndArgsCaller(m, argv);//3
}
```
可以看到注释1处通过反射来获得android.app.ActivityThread类，接下来在注释2处来获得ActivityThread的main函数，
  并将main函数传入到注释3处的ZygoteInit中的MethodAndArgsCaller类的构造函数中，
  MethodAndArgsCaller类内部会通过反射调用ActivityThread的main函数，讲到这里，应用程序进程就创建完成了并且运行了代表主线程的实例ActivityThread