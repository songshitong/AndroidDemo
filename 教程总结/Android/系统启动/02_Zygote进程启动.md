http://liuwangshu.cn/framework/booting/2-zygote.html  Android7.0

Zygote简介
在Android系统中，DVM(Dalvik虚拟机)、应用程序进程以及运行系统的关键服务的SystemServer进程都是由Zygote进程来创建的，
我们也将它称为孵化器。它通过fork(复制进程)的形式来创建应用程序进程和SystemServer进程，由于Zygote进程在启动时会创建DVM，
因此通过fork而创建的应用程序进程和SystemServer进程可以在内部获取一个DVM的实例拷贝。
关于init启动zygote我们在上一篇文章已经提到了，这里就不赘述了，这篇文章主要分析Android7.0 Zygote的启动流程

AppRuntime分析
我们从上篇文章得知init启动zygote时主要是调用app_main.cpp的main函数中的AppRuntime的start来启动zygote进程的，
我们就从app_main.cpp的main函数开始分析，如下所示。
frameworks/base/cmds/app_process/app_main.cpp
```
int main(int argc, char* const argv[])
{
...
    AppRuntime runtime(argv[0], computeArgBlockSize(argc, argv));
   ...
     Vector<String8> args;
    if (!className.isEmpty()) {
        args.add(application ? String8("application") : String8("tool"));
        runtime.setClassNameAndArgs(className, argc - i, argv + i);
    } else {
        // We're in zygote mode.
        maybeCreateDalvikCache();
        if (startSystemServer) {    
            args.add(String8("start-system-server"));//1
        }
        char prop[PROP_VALUE_MAX];
        if (property_get(ABI_LIST_PROPERTY, prop, NULL) == 0) {
            LOG_ALWAYS_FATAL("app_process: Unable to determine ABI list from property %s.",
                ABI_LIST_PROPERTY);
            return 11;
        }
        String8 abiFlag("--abi-list=");
        abiFlag.append(prop);
        args.add(abiFlag);
        for (; i < argc; ++i) {
            args.add(String8(argv[i]));
        }
    }
    if (!niceName.isEmpty()) {
        runtime.setArgv0(niceName.string());
        set_process_name(niceName.string());
    }
    if (zygote) {
        runtime.start("com.android.internal.os.ZygoteInit", args, zygote);//2
    } else if (className) {
        runtime.start("com.android.internal.os.RuntimeInit", args, zygote);
    } else {
        fprintf(stderr, "Error: no class name or --zygote supplied.\n");
        app_usage();
        LOG_ALWAYS_FATAL("app_process: no class name or --zygote supplied.");
        return 10;
    }
}
```
startSystemServer bool类型，在省略的参数处理中
注释1处如果startSystemServer为true的话(默认为true)，将”start-system-server”放入启动的参数args。
注释2处调用注释2处这里调用runtime的start函数来启动zygote进程，并将args传入，这样启动zygote进程后，zygote进程会将SystemServer进程启动。
我们知道runtime指的就是AppRuntime，AppRuntime声明也在app_main.cpp中，它继承AndroidRuntime，
  也就是我们调用start其实是调用AndroidRuntime的start函数

frameworks/base/core/jni/AndroidRuntime.cpp
```
void AndroidRuntime::start(const char* className, const Vector<String8>& options, bool zygote)
{
    ...
    /* start the virtual machine */
    JniInvocation jni_invocation;
    jni_invocation.Init(NULL);
    JNIEnv* env;
    if (startVm(&mJavaVM, &env, zygote) != 0) {//1
        return;
    }
    onVmCreated(env);
    if (startReg(env) < 0) {//2
        ALOGE("Unable to register all android natives\n");
        return;
    }
    jclass stringClass;
    jobjectArray strArray;
    jstring classNameStr;

    stringClass = env->FindClass("java/lang/String");
    assert(stringClass != NULL);
    //创建数组
    strArray = env->NewObjectArray(options.size() + 1, stringClass, NULL);
    assert(strArray != NULL);
    //从app_main的main函数得知className为com.android.internal.os.ZygoteInit
    classNameStr = env->NewStringUTF(className);
    assert(classNameStr != NULL);
    env->SetObjectArrayElement(strArray, 0, classNameStr);

    for (size_t i = 0; i < options.size(); ++i) {
        jstring optionsStr = env->NewStringUTF(options.itemAt(i).string());
        assert(optionsStr != NULL);
        env->SetObjectArrayElement(strArray, i + 1, optionsStr);
    }
    char* slashClassName = toSlashClassName(className);
    jclass startClass = env->FindClass(slashClassName);
    if (startClass == NULL) {
        ALOGE("JavaVM unable to locate class '%s'\n", slashClassName);
        /* keep going */
    } else {
    //找到ZygoteInit的main函数
        jmethodID startMeth = env->GetStaticMethodID(startClass, "main",
            "([Ljava/lang/String;)V");//3
        if (startMeth == NULL) {
            ALOGE("JavaVM unable to find main() in '%s'\n", className);
            /* keep going */
        } else {
        //通过JNI调用ZygoteInit的main函数
            env->CallStaticVoidMethod(startClass, startMeth, strArray);//4

#if 0
            if (env->ExceptionCheck())
                threadExitUncaughtException(env);
#endif
        }
    }
  ...
}
```
注释1处调用startVm函数来创建JavaVm(DVM)，
注释2处调用startReg函数用来为DVM注册JNI。
注释3处的代码用来找到ZygoteInit的main函数，其中startClass从app_main的main函数得知为com.android.internal.os.ZygoteInit。
注释4处通过JNI调用ZygoteInit的main函数，因为ZygoteInit的main函数是Java编写的，因此需要通过JNI调用



Zygote的Java框架层
上文我们通过JNI调用ZygoteInit的main函数后，Zygote便进入了Java框架层，此前没有任何代码进入过Java框架层，换句换说Zygote开创了Java框架层。
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
注释1处通过registerZygoteSocket函数来创建一个Server端的Socket，这个name为”zygote”的Socket用来等待ActivityManagerService来请求Zygote来创建新的应用程序进程。
注释2处用来预加载类和资源。
注释3处用来启动SystemServer进程，这样系统的关键服务也会由SystemServer进程启动起来。
注释4处调用runSelectLoop函数来等待客户端请求。由此得知，ZygoteInit的main函数主要做了4件事
registerZygoteSocket
```
private static void registerZygoteSocket(String socketName) {
     if (sServerSocket == null) {
         int fileDesc;
         final String fullSocketName = ANDROID_SOCKET_PREFIX + socketName;
         try {
             String env = System.getenv(fullSocketName);
             fileDesc = Integer.parseInt(env);
         } catch (RuntimeException ex) {
             throw new RuntimeException(fullSocketName + " unset or invalid", ex);
         }
         try {
             FileDescriptor fd = new FileDescriptor();
             fd.setInt$(fileDesc);
             sServerSocket = new LocalServerSocket(fd);//1
         } catch (IOException ex) {
             throw new RuntimeException(
                     "Error binding to local socket '" + fileDesc + "'", ex);
         }
     }
 
```
注释1处用来创建LocalServerSocket，也就是服务端的Socket。当Zygote进程将SystemServer进程启动后，
就会在这个服务端的Socket上来等待ActivityManagerService请求Zygote进程来创建新的应用程序进程



启动SystemServer进程
接下来查看startSystemServer函数，代码如下所示
```
 private static boolean startSystemServer(String abiList, String socketName)
            throws MethodAndArgsCaller, RuntimeException {
...
        /* Hardcoded command line to start the system server */
         /*1*/
        String args[] = {
            "--setuid=1000",
            "--setgid=1000",
            "--setgroups=1001,1002,1003,1004,1005,1006,1007,1008,1009,1010,1018,1021,1032,3001,3002,3003,3006,3007,3009,3010",
            "--capabilities=" + capabilities + "," + capabilities,
            "--nice-name=system_server",
            "--runtime-args",
            "com.android.server.SystemServer",
        };
        ZygoteConnection.Arguments parsedArgs = null;

        int pid;

        try {
            parsedArgs = new ZygoteConnection.Arguments(args);//2
            ZygoteConnection.applyDebuggerSystemProperty(parsedArgs);
            ZygoteConnection.applyInvokeWithSystemProperty(parsedArgs);

            /*3*/
            pid = Zygote.forkSystemServer(
                    parsedArgs.uid, parsedArgs.gid,
                    parsedArgs.gids,
                    parsedArgs.debugFlags,
                    null,
                    parsedArgs.permittedCapabilities,
                    parsedArgs.effectiveCapabilities);
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException(ex);
        }
        if (pid == 0) {
            if (hasSecondZygote(abiList)) {
                waitForSecondaryZygote(socketName);
            }

            handleSystemServerProcess(parsedArgs);//4
        }

        return true;
    }
```
注释1处的代码用来创建args数组，这个数组用来保存启动SystemServer的启动参数，其中可以看出SystemServer进程的用户id和用户组id被设置为1000；
  并且拥有用户组10011010，1018、1021、1032、30013010的权限；进程名为system_server；启动的类名为com.android.server.SystemServer。
在注释2处将args数组封装成Arguments对象并供注释3的forkSystemServer函数调用。
注释3处调用Zygote的forkSystemServer，主要通过fork函数在当前进程创建一个子进程，如果返回的pid 为0，也就是表示在新创建的子进程中执行的，
  则执行注释4处的handleSystemServerProcess来启动SystemServer进程
   handleSystemServerProcess放在SystemServer中介绍


runSelectLoop
启动启动SystemServer进程后，最后进入runSelectLoop函数
```
private static void runSelectLoop(String abiList) throws MethodAndArgsCaller {
        ArrayList<FileDescriptor> fds = new ArrayList<FileDescriptor>();
        ArrayList<ZygoteConnection> peers = new ArrayList<ZygoteConnection>();
        fds.add(sServerSocket.getFileDescriptor());//1
        peers.add(null);

        while (true) {
            StructPollfd[] pollFds = new StructPollfd[fds.size()];
            for (int i = 0; i < pollFds.length; ++i) {//2
                pollFds[i] = new StructPollfd();
                pollFds[i].fd = fds.get(i);
                pollFds[i].events = (short) POLLIN;
            }
            try {
                Os.poll(pollFds, -1);
            } catch (ErrnoException ex) {
                throw new RuntimeException("poll failed", ex);
            }
            for (int i = pollFds.length - 1; i >= 0; --i) {//3
                if ((pollFds[i].revents & POLLIN) == 0) {
                    continue;
                }
                if (i == 0) {
                    ZygoteConnection newPeer = acceptCommandPeer(abiList);//4
                    peers.add(newPeer);
                    fds.add(newPeer.getFileDesciptor());
                } else {
                    boolean done = peers.get(i).runOnce();//5
                    if (done) {
                        peers.remove(i);
                        fds.remove(i);
                    }
                }
            }
        }
    }
```
注释1处中的sServerSocket就是我们在registerZygoteSocket函数中创建的服务端Socket，调用sServerSocket.getFileDescriptor()
   用来获得该Socket的fd字段的值并添加到fd列表fds中。接下来无限循环用来等待ActivityManagerService请求Zygote进程创建新的应用程序进程。
注释2处通过遍历将fds存储的信息转移到pollFds数组中。
最后在注释3处对pollFds进行遍历，如果i==0则说明服务端Socket与客户端连接上，也就是当前Zygote进程与ActivityManagerService建立了连接。
则在注释4处通过acceptCommandPeer函数得到ZygoteConnection类并添加到Socket连接列表peers中，接着将该ZygoteConnection的fd添加到fd列表fds中，
 以便可以接收到ActivityManagerService发送过来的请求。如果i的值大于0，则说明ActivityManagerService向Zygote进程发送了一个创建应用进程的请求，
//todo socket编程
则在注释5处调用ZygoteConnection的runOnce函数来创建一个新的应用程序进程。并在成功创建后将这个连接从Socket连接列表peers和fd列表fds中清除

Zygote启动流程就讲到这，Zygote进程共做了如下几件事：
1.创建AppRuntime并调用其start方法，启动Zygote进程。
2.创建DVM并为DVM注册JNI.
3.通过JNI调用ZygoteInit的main函数进入Zygote的Java框架层。
4.通过registerZygoteSocket函数创建服务端Socket，并通过runSelectLoop函数等待ActivityManagerService的请求来创建新的应用程序进程。
   //todo activity m s 请求的过程
5.启动SystemServer进程



preload的主要内容
```
static void preload() {
193          Log.d(TAG, "begin preload");
194          Trace.traceBegin(Trace.TRACE_TAG_DALVIK, "BeginIcuCachePinning");
195          beginIcuCachePinning();
196          Trace.traceEnd(Trace.TRACE_TAG_DALVIK);
197          Trace.traceBegin(Trace.TRACE_TAG_DALVIK, "PreloadClasses");
198          preloadClasses();
199          Trace.traceEnd(Trace.TRACE_TAG_DALVIK);
200          Trace.traceBegin(Trace.TRACE_TAG_DALVIK, "PreloadResources");
201          preloadResources();
202          Trace.traceEnd(Trace.TRACE_TAG_DALVIK);
203          Trace.traceBegin(Trace.TRACE_TAG_DALVIK, "PreloadOpenGL");
204          preloadOpenGL();
205          Trace.traceEnd(Trace.TRACE_TAG_DALVIK);
206          preloadSharedLibraries();
207          preloadTextResources();
208          // Ask the WebViewFactory to do any initialization that must run in the zygote process,
209          // for memory sharing purposes.
210          WebViewFactory.prepareWebViewInZygote();
211          endIcuCachePinning();
212          warmUpJcaProviders();
213          Log.d(TAG, "end preload");
214      }
```

startVm分析
```
int AndroidRuntime::startVm(JavaVM** pJavaVM, JNIEnv** pEnv, bool zygote, bool primary_zygote)
  {
    ...解析参数
    if (JNI_CreateJavaVM(pJavaVM, pEnv, &initArgs) < 0) {
         ALOGE("JNI_CreateJavaVM failed\n");
          return -1;
     }
    }
```
