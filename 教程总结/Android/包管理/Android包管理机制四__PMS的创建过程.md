前言
PMS的创建过程分为两个部分进行讲解，分别是SyetemServer处理部分和PMS构造方法。其中SyetemServer处理部分和AMS和WMS的创建过程是类似的，
可以将它们进行对比，这样可以更好的理解和记忆这一知识点。

1. SyetemServer处理部分
   PMS是在SyetemServer进程中被创建的，SyetemServer进程用来创建系统服务，不了解它的可以查看Android系统启动流程（三）解析SyetemServer进程启动过程这篇文章。
   从SyetemServer的入口方法main方法开始讲起，如下所示。
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
    try {
        ...
        //创建消息Looper
         Looper.prepareMainLooper();
        //加载了动态库libandroid_servers.so
        System.loadLibrary("android_servers");//1
        performPendingShutdown();
        // 创建系统的Context
        createSystemContext();
        // 创建SystemServiceManager
        mSystemServiceManager = new SystemServiceManager(mSystemContext);//2
        mSystemServiceManager.setRuntimeRestarted(mRuntimeRestart);
        LocalServices.addService(SystemServiceManager.class, mSystemServiceManager);
        SystemServerInitThreadPool.get();
    } finally {
        traceEnd(); 
    }
    try {
        traceBeginAndSlog("StartServices");
        //启动引导服务
        startBootstrapServices();//3
        //启动核心服务
        startCoreServices();//4
        //启动其他服务
        startOtherServices();//5
        SystemServerInitThreadPool.shutdown();
    } catch (Throwable ex) {
        Slog.e("System", "******************************************");
        Slog.e("System", "************ Failure starting system services", ex);
        throw ex;
    } finally {
        traceEnd();
    }
    ...
}
```
在注释1处加载了动态库libandroid_servers.so。接下来在注释2处创建SystemServiceManager，它会对系统的服务进行创建、启动和生命周期管理。
在注释3中的startBootstrapServices方法中用SystemServiceManager启动了ActivityManagerService、PowerManagerService、PackageManagerService等服务。
在注释4处的startCoreServices方法中则启动了DropBoxManagerService、BatteryService、UsageStatsService和WebViewUpdateService。
注释5处的startOtherServices方法中启动了CameraService、AlarmManagerService、VrManagerService等服务。
这些服务的父类均为SystemService。从注释3、4、5的方法可以看出，官方把系统服务分为了三种类型，分别是引导服务、核心服务和其他服务，
其中其他服务是一些非紧要和一些不需要立即启动的服务。这些系统服务总共有100多个，我们熟知的AMS属于引导服务，WMS属于其他服务，
本文要讲的PMS属于引导服务

查看启动引导服务的注释3处的startBootstrapServices方法。
frameworks/base/services/java/com/android/server/SystemServer.java
```
private void startBootstrapServices() {
    ...
     String cryptState = SystemProperties.get("vold.decrypt");//1
     if (ENCRYPTING_STATE.equals(cryptState)) {
            Slog.w(TAG, "Detected encryption in progress - only parsing core apps");
            mOnlyCore = true;
        } else if (ENCRYPTED_STATE.equals(cryptState)) {
            Slog.w(TAG, "Device encrypted - only parsing core apps");
            mOnlyCore = true;
        }
    ...    
    traceBeginAndSlog("StartPackageManagerService");
    mPackageManagerService = PackageManagerService.main(mSystemContext, installer,
            mFactoryTestMode != FactoryTest.FACTORY_TEST_OFF, mOnlyCore);//2
    mFirstBoot = mPackageManagerService.isFirstBoot();//3
    mPackageManager = mSystemContext.getPackageManager();
    traceEnd();
    ...
}
```
注释1处读取init.rc的vold.decrypt属性，如果它的值为”trigger_restart_min_framework”，说明我们加密了设备，这时mOnlyCore的值为true，
表示只运行“核心”程序，这是为了创建一个极简的启动环境。
注释2处的PMS的main方法主要用来创建PMS，注释3处获取boolean类型的变量mFirstBoot，它用于表示PMS是否首次被启动。
  mFirstBoot是后续WMS创建时所需要的参数，从这里就可以看出系统服务之间是有依赖关系的，它们的启动顺序不能随意被更改。

2. PMS构造方法
   PMS的main方法如下所示。
   frameworks/base/services/core/java/com/android/server/pm/PackageManagerService.java
```
public static PackageManagerService main(Context context, Installer installer,
         boolean factoryTest, boolean onlyCore) {
     PackageManagerServiceCompilerMapping.checkProperties();
     PackageManagerService m = new PackageManagerService(context, installer,
             factoryTest, onlyCore);
     m.enableSystemUserPackages();
     ServiceManager.addService("package", m);
     return m;
 }
```   

main方法主要做了两件事，一个是创建PMS对象，另一个是将PMS注册到ServiceManager中。
PMS的构造方法大概有600多行，分为5个阶段，每个阶段会打印出相应的EventLog，EventLog用于打印Android系统的事件日志。
1 BOOT_PROGRESS_PMS_START（开始阶段）
2 BOOT_PROGRESS_PMS_SYSTEM_SCAN_START（扫描系统阶段）
3 BOOT_PROGRESS_PMS_DATA_SCAN_START（扫描Data分区阶段）
4 BOOT_PROGRESS_PMS_SCAN_END（扫描结束阶段）
5 BOOT_PROGRESS_PMS_READY（准备阶段）


2.1 开始阶段
PMS的构造方法中会获取一些包管理需要属性，如下所示。
frameworks/base/services/core/java/com/android/server/pm/PackageManagerService.java
```
public PackageManagerService(Context context, Installer installer,
            boolean factoryTest, boolean onlyCore) {
        LockGuard.installLock(mPackages, LockGuard.INDEX_PACKAGES);
        Trace.traceBegin(TRACE_TAG_PACKAGE_MANAGER, "create package manager");
        //打印开始阶段日志
        EventLog.writeEvent(EventLogTags.BOOT_PROGRESS_PMS_START,
                SystemClock.uptimeMillis())
        ...
        //用于存储屏幕的相关信息
        mMetrics = new DisplayMetrics();
        //Settings用于保存所有包的动态设置
        mSettings = new Settings(mPackages);
	    //在Settings中添加多个默认的sharedUserId
        mSettings.addSharedUserLPw("android.uid.system", Process.SYSTEM_UID,
                ApplicationInfo.FLAG_SYSTEM, ApplicationInfo.PRIVATE_FLAG_PRIVILEGED);//1
        mSettings.addSharedUserLPw("android.uid.phone", RADIO_UID,
                ApplicationInfo.FLAG_SYSTEM, ApplicationInfo.PRIVATE_FLAG_PRIVILEGED);
        mSettings.addSharedUserLPw("android.uid.log", LOG_UID,
                ApplicationInfo.FLAG_SYSTEM, ApplicationInfo.PRIVATE_FLAG_PRIVILEGED);
        ...
        mInstaller = installer;
        //创建Dex优化工具类
        mPackageDexOptimizer = new PackageDexOptimizer(installer, mInstallLock, context,
                "*dexopt*");
        mDexManager = new DexManager(this, mPackageDexOptimizer, installer, mInstallLock);
        mMoveCallbacks = new MoveCallbacks(FgThread.get().getLooper());
        mOnPermissionChangeListeners = new OnPermissionChangeListeners(
                FgThread.get().getLooper());
        getDefaultDisplayMetrics(context, mMetrics);
        Trace.traceBegin(TRACE_TAG_PACKAGE_MANAGER, "get system config");
        //得到全局系统配置信息。
        SystemConfig systemConfig = SystemConfig.getInstance();
        //获取全局的groupId 
        mGlobalGids = systemConfig.getGlobalGids();
        //获取系统权限
        mSystemPermissions = systemConfig.getSystemPermissions();
        mAvailableFeatures = systemConfig.getAvailableFeatures();
        Trace.traceEnd(TRACE_TAG_PACKAGE_MANAGER);
        mProtectedPackages = new ProtectedPackages(mContext);
        //安装APK时需要的锁，保护所有对installd的访问。
        synchronized (mInstallLock) {//1
        //更新APK时需要的锁，保护内存中已经解析的包信息等内容
        synchronized (mPackages) {//2
            //创建后台线程ServiceThread
            mHandlerThread = new ServiceThread(TAG,
                    Process.THREAD_PRIORITY_BACKGROUND, true /*allowIo*/);
            mHandlerThread.start();
            //创建PackageHandler绑定到ServiceThread的消息队列
            mHandler = new PackageHandler(mHandlerThread.getLooper());//3
            mProcessLoggingHandler = new ProcessLoggingHandler();
            //将PackageHandler添加到Watchdog的检测集中
            Watchdog.getInstance().addThread(mHandler, WATCHDOG_TIMEOUT);//4

            mDefaultPermissionPolicy = new DefaultPermissionGrantPolicy(this);
            mInstantAppRegistry = new InstantAppRegistry(this);
            //在Data分区创建一些目录
            File dataDir = Environment.getDataDirectory();//5
            mAppInstallDir = new File(dataDir, "app");
            mAppLib32InstallDir = new File(dataDir, "app-lib");
            mAsecInternalPath = new File(dataDir, "app-asec").getPath();
            mDrmAppPrivateInstallDir = new File(dataDir, "app-private");
            //创建多用户管理服务
            sUserManager = new UserManagerService(context, this,
                    new UserDataPreparer(mInstaller, mInstallLock, mContext, mOnlyCore), mPackages);
             ...
               mFirstBoot = !mSettings.readLPw(sUserManager.getUsers(false))//6
          ...     
}
```

在开始阶段中创建了很多PMS中的关键对象并赋值给PMS中的成员变量，下面简单介绍这些成员变量。
mSettings ：用于保存所有包的动态设置。注释1处将系统进程的sharedUserId添加到Settings中，sharedUserId用于进程间共享数据，
   比如两个App的之间的数据是不共享的，如果它们有了共同的sharedUserId，就可以运行在同一个进程中共享数据。
mInstaller ：Installer继承自SystemService，和PMS、AMS一样是系统的服务（虽然名称不像是服务），PMS很多的操作都是由Installer来完成的，
  比如APK的安装和卸载。在Installer内部，通过IInstalld和installd进行Binder通信，由位于nativie层的installd来完成具体的操作。
systemConfig：用于得到全局系统配置信息。比如系统的权限就可以通过SystemConfig来获取。
mPackageDexOptimizer ： Dex优化的工具类。
mHandler（PackageHandler类型） ：PackageHandler继承自Handler，在注释3处它绑定了后台线程ServiceThread的消息队列。P
   MS通过PackageHandler驱动APK的复制和安装工作，具体的请看在Android包管理机制（三）PMS处理APK的安装这篇文章。
PackageHandler处理的消息队列如果过于繁忙，有可能导致系统卡住， 因此在注释4处将它添加到Watchdog的监测集中。
Watchdog主要有两个用途，一个是定时检测系统关键服务（AMS和WMS等）是否可能发生死锁，还有一个是定时检测线程的消息队列是否长时间处于工作状态（可能阻塞等待了很长时间）。
   如果出现上述问题，Watchdog会将日志保存起来，必要时还会杀掉自己所在的进程，也就是SystemServer进程。
sUserManager（UserManagerService类型） ：多用户管理服务。

除了创建这些关键对象，在开始阶段还有一些关键代码需要去讲解：
注释1处和注释2处加了两个锁，其中mInstallLock是安装APK时需要的锁，保护所有对installd的访问；mPackages是更新APK时需要的锁，
  保护内存中已经解析的包信息等内容。
注释5处后的代码创建了一些Data分区中的子目录，比如/data/app。
注释6处会解析packages.xml等文件的信息，保存到Settings的对应字段中。packages.xml中记录系统中所有安装的应用信息，
  包括基本信息、签名和权限。如果packages.xml有安装的应用信息，那么注释6处Settings的readLPw方法会返回true，mFirstBoot的值为false，
  说明PMS不是首次被启动。


2.2 扫描系统阶段
```
...
public PackageManagerService(Context context, Installer installer,
            boolean factoryTest, boolean onlyCore) {
...
            //打印扫描系统阶段日志
            EventLog.writeEvent(EventLogTags.BOOT_PROGRESS_PMS_SYSTEM_SCAN_START,
                    startTime);
            ...
            //在/system中创建framework目录
            File frameworkDir = new File(Environment.getRootDirectory(), "framework");
            ...
            //扫描/vendor/overlay目录下的文件
            scanDirTracedLI(new File(VENDOR_OVERLAY_DIR), mDefParseFlags
                    | PackageParser.PARSE_IS_SYSTEM
                    | PackageParser.PARSE_IS_SYSTEM_DIR
                    | PackageParser.PARSE_TRUSTED_OVERLAY, scanFlags | SCAN_TRUSTED_OVERLAY, 0);
            mParallelPackageParserCallback.findStaticOverlayPackages();
            //扫描/system/framework 目录下的文件
            scanDirTracedLI(frameworkDir, mDefParseFlags
                    | PackageParser.PARSE_IS_SYSTEM
                    | PackageParser.PARSE_IS_SYSTEM_DIR
                    | PackageParser.PARSE_IS_PRIVILEGED,
                    scanFlags | SCAN_NO_DEX, 0);
            final File privilegedAppDir = new File(Environment.getRootDirectory(), "priv-app");
            //扫描 /system/priv-app 目录下的文件
            scanDirTracedLI(privilegedAppDir, mDefParseFlags
                    | PackageParser.PARSE_IS_SYSTEM
                    | PackageParser.PARSE_IS_SYSTEM_DIR
                    | PackageParser.PARSE_IS_PRIVILEGED, scanFlags, 0);
            final File systemAppDir = new File(Environment.getRootDirectory(), "app");
            //扫描/system/app 目录下的文件
            scanDirTracedLI(systemAppDir, mDefParseFlags
                    | PackageParser.PARSE_IS_SYSTEM
                    | PackageParser.PARSE_IS_SYSTEM_DIR, scanFlags, 0);
            File vendorAppDir = new File("/vendor/app");
            try {
                vendorAppDir = vendorAppDir.getCanonicalFile();
            } catch (IOException e) {
                // failed to look up canonical path, continue with original one
            }
            //扫描 /vendor/app 目录下的文件
            scanDirTracedLI(vendorAppDir, mDefParseFlags
                    | PackageParser.PARSE_IS_SYSTEM
                    | PackageParser.PARSE_IS_SYSTEM_DIR, scanFlags, 0);

           //扫描/oem/app 目录下的文件
            final File oemAppDir = new File(Environment.getOemDirectory(), "app");
            scanDirTracedLI(oemAppDir, mDefParseFlags
                    | PackageParser.PARSE_IS_SYSTEM
                    | PackageParser.PARSE_IS_SYSTEM_DIR, scanFlags, 0);

            //这个列表代表有可能有升级包的系统App
            final List<String> possiblyDeletedUpdatedSystemApps = new ArrayList<String>();//1
            if (!mOnlyCore) {
                Iterator<PackageSetting> psit = mSettings.mPackages.values().iterator();
                while (psit.hasNext()) {
                    PackageSetting ps = psit.next();                 
                    if ((ps.pkgFlags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                        continue;
                    }
                    //这里的mPackages的是PMS的成员变量，代表scanDirTracedLI方法扫描上面那些目录得到的 
                    final PackageParser.Package scannedPkg = mPackages.get(ps.name);
                    if (scannedPkg != null) {           
                        if (mSettings.isDisabledSystemPackageLPr(ps.name)) {//2
                           ...
                            //将这个系统App的PackageSetting从PMS的mPackages中移除
                            removePackageLI(scannedPkg, true);
                            //将升级包的路径添加到mExpectingBetter列表中
                            mExpectingBetter.put(ps.name, ps.codePath);
                        }
                        continue;
                    }
                   
                    if (!mSettings.isDisabledSystemPackageLPr(ps.name)) {
                       ...   
                    } else {
                        final PackageSetting disabledPs = mSettings.getDisabledSystemPkgLPr(ps.name);
                        //这个系统App升级包信息在mDisabledSysPackages中,但是没有发现这个升级包存在
                        if (disabledPs.codePath == null || !disabledPs.codePath.exists()) {//5
                            possiblyDeletedUpdatedSystemApps.add(ps.name);//
                        }
                    }
                }
            }
            ...        
}
```

/system可以称作为System分区，里面主要存储谷歌和其他厂商提供的Android系统相关文件和框架。Android系统架构分为应用层、应用框架层、
系统运行库层（Native 层）、硬件抽象层（HAL层）和Linux内核层，除了Linux内核层在Boot分区，其他层的代码都在System分区。
下面列出 System分区的部分子目录。
| 目录| 含义|
| :——–: | :——–:| :–: |
| app| 存放系统App，包括了谷歌内置的App也有厂商或者运营商提供的App |
| framework| 存放应用框架层的jar包 |
| priv-app| 存放特权App |
| lib| 存放so文件 |
| fonts| 存放系统字体文件 |
| media| 存放系统的各种声音，比如铃声、提示音，以及系统启动播放的动画 |

上面的代码还涉及到/vendor 目录，它用来存储厂商对Android系统的定制部分。


系统扫描阶段的主要工作有以下3点：
1 创建/system的子目录，比如/system/framework、/system/priv-app和/system/app等等
2 扫描系统文件，比如/vendor/overlay、/system/framework、/system/app等等目录下的文件。
3 对扫描到的系统文件做后续处理。
主要来说第3点，一次OTA升级对于一个系统App会有三种情况：
1 这个系统APP无更新。
2 这个系统APP有更新。
3 新的OTA版本中，这个系统APP已经被删除。

当系统App升级，PMS会将该系统App的升级包设置数据（PackageSetting）存储到Settings的mDisabledSysPackages列表中（具体见PMS的replaceSystemPackageLIF方法），
mDisabledSysPackages的类型为ArrayMap<String, PackageSetting>。
mDisabledSysPackages中的信息会被PMS保存到packages.xml中的<updated-package>标签下（具体见Settings的writeDisabledSysPackageLPr方法）。
注释2处说明这个系统App有升级包，那么就将该系统App的PackageSetting从mDisabledSysPackages列表中移除，并将系统App的升级包的路径添加到mExpectingBetter列表中，
  mExpectingBetter的类型为ArrayMap<String, File>等待后续处理。
注释5处如果这个系统App的升级包信息存储在mDisabledSysPackages列表中，但是没有发现这个升级包存在，则将它加入到possiblyDeletedUpdatedSystemApps列表中，
  意为“系统App的升级包可能被删除”，之所以是“可能”，是因为系统还没有扫描Data分区，只能暂放到possiblyDeletedUpdatedSystemApps列表中，
  等到扫描完Data分区后再做处理。

2.3 扫描Data分区阶段
```
public PackageManagerService(Context context, Installer installer,
            boolean factoryTest, boolean onlyCore) {
    ...        
    mSettings.pruneSharedUsersLPw();
    //如果没有加密设备，那么就开始扫描Data分区。
    if (!mOnlyCore) {
        //打印扫描Data分区阶段日志
        EventLog.writeEvent(EventLogTags.BOOT_PROGRESS_PMS_DATA_SCAN_START,
                SystemClock.uptimeMillis());
        //扫描/data/app目录下的文件       
        scanDirTracedLI(mAppInstallDir, 0, scanFlags | SCAN_REQUIRE_KNOWN, 0);
        //扫描/data/app-private目录下的文件   
        scanDirTracedLI(mDrmAppPrivateInstallDir, mDefParseFlags
                | PackageParser.PARSE_FORWARD_LOCK,
                scanFlags | SCAN_REQUIRE_KNOWN, 0);
        //扫描完Data分区后，处理possiblyDeletedUpdatedSystemApps列表
        for (String deletedAppName : possiblyDeletedUpdatedSystemApps) {
            PackageParser.Package deletedPkg = mPackages.get(deletedAppName);
            // 从mSettings.mDisabledSysPackages变量中移除去此应用
            mSettings.removeDisabledSystemPackageLPw(deletedAppName);
            String msg;
          //1：如果这个系统App的包信息不在PMS的变量mPackages中，说明是残留的App信息，后续会删除它的数据。
            if (deletedPkg == null) {
                msg = "Updated system package " + deletedAppName
                        + " no longer exists; it's data will be wiped";
                // Actual deletion of code and data will be handled by later
                // reconciliation step
            } else {
            //2：如果这个系统App在mPackages中，说明是存在于Data分区，不属于系统App，那么移除其系统权限。
                msg = "Updated system app + " + deletedAppName
                        + " no longer present; removing system privileges for "
                        + deletedAppName;
                deletedPkg.applicationInfo.flags &= ~ApplicationInfo.FLAG_SYSTEM;
                PackageSetting deletedPs = mSettings.mPackages.get(deletedAppName);
                deletedPs.pkgFlags &= ~ApplicationInfo.FLAG_SYSTEM;
            }
            logCriticalInfo(Log.WARN, msg);
        }
         //遍历mExpectingBetter列表
        for (int i = 0; i < mExpectingBetter.size(); i++) {
            final String packageName = mExpectingBetter.keyAt(i);
            if (!mPackages.containsKey(packageName)) {
                //得到系统App的升级包路径
                final File scanFile = mExpectingBetter.valueAt(i);
                logCriticalInfo(Log.WARN, "Expected better " + packageName
                        + " but never showed up; reverting to system");
                int reparseFlags = mDefParseFlags;
                //3：根据系统App所在的目录设置扫描的解析参数
                if (FileUtils.contains(privilegedAppDir, scanFile)) {
                    reparseFlags = PackageParser.PARSE_IS_SYSTEM
                            | PackageParser.PARSE_IS_SYSTEM_DIR
                            | PackageParser.PARSE_IS_PRIVILEGED;
                } 
                ...
                //将packageName对应的包设置数据（PackageSetting）添加到mSettings的mPackages中
                mSettings.enableSystemPackageLPw(packageName);//4
                try {
                    //扫描系统App的升级包
                    scanPackageTracedLI(scanFile, reparseFlags, scanFlags, 0, null);//5
                } catch (PackageManagerException e) {
                    Slog.e(TAG, "Failed to parse original system package: "
                            + e.getMessage());
                }
            }
        }
    }
   //清除mExpectingBetter列表
    mExpectingBetter.clear();
...
}
```

/data可以称为Data分区，它用来存储所有用户的个人数据和配置文件。下面列出Data分区部分子目录：

| 目录| 含义|
| :——–: | :——–:| :–: |
| app| 存储用户自己安装的App|
| data| 存储所有已安装的App数据的目录，每个App都有自己单独的子目录 |
| app-private| App的私有存储空间 |
| app-lib|存储所有App的Jni库 |
| system| 存放系统配置文件 |
| anr| 用于存储ANR发生时系统生成的traces.txt文件 |

扫描Data分区阶段主要做了以下几件事：
扫描/data/app和/data/app-private目录下的文件。
遍历possiblyDeletedUpdatedSystemApps列表，注释1处如果这个系统App的包信息不在PMS的变量mPackages中，说明是残留的App信息，
  后续会删除它的数据。注释2处如果这个系统App的包信息在mPackages中，说明是存在于Data分区，不属于系统App，那么移除其系统权限。
遍历mExpectingBetter列表，注释3处根据系统App所在的目录设置扫描的解析参数，注释4处的方法内部会将packageName对应的包设置数据（PackageSetting）
   添加到mSettings的mPackages中。注释5处扫描系统App的升级包，最后清除mExpectingBetter列表。


2.4 扫描结束阶段
```
//打印扫描结束阶段日志
EventLog.writeEvent(EventLogTags.BOOT_PROGRESS_PMS_SCAN_END,
                  SystemClock.uptimeMillis());
          Slog.i(TAG, "Time to scan packages: "
                  + ((SystemClock.uptimeMillis()-startTime)/1000f)
                  + " seconds");
          int updateFlags = UPDATE_PERMISSIONS_ALL;
          // 如果当前平台SDK版本和上次启动时的SDK版本不同，重新更新APK的授权
          if (ver.sdkVersion != mSdkVersion) {
              Slog.i(TAG, "Platform changed from " + ver.sdkVersion + " to "
                      + mSdkVersion + "; regranting permissions for internal storage");
              updateFlags |= UPDATE_PERMISSIONS_REPLACE_PKG | UPDATE_PERMISSIONS_REPLACE_ALL;
          }
          updatePermissionsLPw(null, null, StorageManager.UUID_PRIVATE_INTERNAL, updateFlags);
          ver.sdkVersion = mSdkVersion;
         //如果是第一次启动或者是Android M升级后的第一次启动，需要初始化所有用户定义的默认首选App
          if (!onlyCore && (mPromoteSystemApps || mFirstBoot)) {
              for (UserInfo user : sUserManager.getUsers(true)) {
                  mSettings.applyDefaultPreferredAppsLPw(this, user.id);
                  applyFactoryDefaultBrowserLPw(user.id);
                  primeDomainVerificationsLPw(user.id);
              }
          }
         ...
          //OTA后的第一次启动，会清除代码缓存目录。
          if (mIsUpgrade && !onlyCore) {
              Slog.i(TAG, "Build fingerprint changed; clearing code caches");
              for (int i = 0; i < mSettings.mPackages.size(); i++) {
                  final PackageSetting ps = mSettings.mPackages.valueAt(i);
                  if (Objects.equals(StorageManager.UUID_PRIVATE_INTERNAL, ps.volumeUuid)) {
                      clearAppDataLIF(ps.pkg, UserHandle.USER_ALL,
                              StorageManager.FLAG_STORAGE_DE | StorageManager.FLAG_STORAGE_CE
                                      | Installer.FLAG_CLEAR_CODE_CACHE_ONLY);
                  }
              }
              ver.fingerprint = Build.FINGERPRINT;
          }
          ...
         // 把Settings的内容保存到packages.xml中
          mSettings.writeLPr();
          Trace.traceEnd(TRACE_TAG_PACKAGE_MANAGER);
```
扫描结束结束阶段主要做了以下几件事：
1 如果当前平台SDK版本和上次启动时的SDK版本不同，重新更新APK的授权。
2 如果是第一次启动或者是Android M升级后的第一次启动，需要初始化所有用户定义的默认首选App。
3 TA升级后的第一次启动，会清除代码缓存目录。
4 把Settings的内容保存到packages.xml中，这样此后PMS再次创建时会读到此前保存的Settings的内容
  //todo 保存到packages.xml

2.5 准备阶段
```
 EventLog.writeEvent(EventLogTags.BOOT_PROGRESS_PMS_READY,
                SystemClock.uptimeMillis());
    ... 
    mInstallerService = new PackageInstallerService(context, this);//1
    ...
    Runtime.getRuntime().gc();//2
    Trace.traceEnd(TRACE_TAG_PACKAGE_MANAGER);
    Trace.traceBegin(TRACE_TAG_PACKAGE_MANAGER, "loadFallbacks");
    FallbackCategoryProvider.loadFallbacks();
    Trace.traceEnd(TRACE_TAG_PACKAGE_MANAGER);
    mInstaller.setWarnIfHeld(mPackages);
    LocalServices.addService(PackageManagerInternal.class, new PackageManagerInternalImpl());//3
    Trace.traceEnd(TRACE_TAG_PACKAGE_MANAGER);
}
```
注释1处创建PackageInstallerService，PackageInstallerService是用于管理安装会话的服务，它会为每次安装过程分配一个SessionId，
  在Android包管理机制（二）PackageInstaller安装APK这篇文章中提到过PackageInstallerService。
注释2处进行一次垃圾收集。注释3处将PackageManagerInternalImpl（PackageManager的本地服务）添加到LocalServices中，
LocalServices用于存储运行在当前的进程中的本地服务。  //todo 本地服务


3. 总结
   本篇文章介绍了PMS的创建过程，分为两个部分，分别是SyetemServer处理部分和PMS构造方法，PMS构造方法又分为5个部分，
    分别是开始阶段、扫描系统阶段、扫描Data分区阶段、扫描结束阶段和准备阶段。