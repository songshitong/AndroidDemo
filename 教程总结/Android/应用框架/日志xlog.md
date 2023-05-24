
xlog 1.3.0

https://zhuanlan.zhihu.com/p/25011775
https://juejin.cn/post/6844903654575570951
https://blog.csdn.net/qq372848728/article/details/89215295

xlog使用
```
Xlog xlog = new Xlog();
Log.setLogImp(xlog);
if (BuildConfig.DEBUG) {
    Log.setConsoleLogOpen(true);
  	Log.appenderOpen(Xlog.LEVEL_DEBUG, Xlog.AppednerModeAsync, "", logPath, logFileName, 0);
} 
```



libraries/mars_android_sdk/src/main/java/com/tencent/mars/xlog/Xlog.java
native接口定义
```
	public static native void logWrite(XLoggerInfo logInfo, String log);

	public static native void logWrite2(int level, String tag, String filename, String funcname, int line, int pid, long tid, long maintid, String log);

	public native int getLogLevel();

	public static native void setLogLevel(int logLevel);

	public static native void setAppenderMode(int mode);

	public static native void setConsoleLogOpen(boolean isOpen);	//set whether the console prints log

	public static native void setErrLogOpen(boolean isOpen);	//set whether the  prints err log into a separate file

	public static native void appenderOpen(int level, int mode, String cacheDir, String logDir, String nameprefix, int cacheDays, String pubkey);

	public static native void setMaxFileSize(long size);
	
	public static native void setMaxAliveTime(long duration);

	public native void appenderClose();

	public native void appenderFlush(boolean isSync);
```


初始化appenderOpen
log/jni/Java2C_Xlog.cc
```
DEFINE_FIND_STATIC_METHOD(KXlog_appenderOpenWithMultipathWithLevel, KXlog, "appenderOpen", "(IILjava/lang/String;Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;)V")
JNIEXPORT void JNICALL Java_com_tencent_mars_xlog_Xlog_appenderOpen
	(JNIEnv *env, jclass, jint level, jint mode, jstring _cache_dir, jstring _log_dir, jstring _nameprefix, jint _cache_log_days, jstring _pubkey) {
	...
	appender_open_with_cache((TAppenderMode)mode, cache_dir.c_str(), log_dir_jstr.GetChar(), nameprefix_jstr.GetChar(), _cache_log_days, pubkey);
	xlogger_SetLevel((TLogLevel)level);
}
```
log/src/appender.cc
```
void appender_open_with_cache(TAppenderMode _mode, const std::string& _cachedir, const std::string& _logdir,
                              const char* _nameprefix, int _cache_days, const char* _pub_key) {
  ...
    if (!_cachedir.empty()) {
        sg_cache_logdir = _cachedir;
        boost::filesystem::create_directories(_cachedir);
        //配置过期时间  
        Thread(boost::bind(&__del_timeout_file, _cachedir)).start_after(2 * 60 * 1000);
        Thread(boost::bind(&__move_old_files, _cachedir, _logdir, std::string(_nameprefix))).start_after(3 * 60 * 1000);
    }
  ...
    appender_open(_mode, _logdir.c_str(), _nameprefix, _pub_key);
}


void appender_open(TAppenderMode _mode, const char* _dir, const char* _nameprefix, const char* _pub_key) {
  ...
    xlogger_SetAppender(&xlogger_appender);
    
    boost::filesystem::create_directories(_dir);
    tickcount_t tick;
    tick.gettickcount();
    Thread(boost::bind(&__del_timeout_file, _dir)).start_after(2 * 60 * 1000);
    
    tick.gettickcount();
...
    char mmap_file_path[512] = {0};
    snprintf(mmap_file_path, sizeof(mmap_file_path), "%s/%s.mmap3", sg_cache_logdir.empty()?_dir:sg_cache_logdir.c_str(), _nameprefix);

    bool use_mmap = false; //使用mmap是否成功
    if (OpenMmapFile(mmap_file_path, kBufferBlockLength, sg_mmmap_file))  {
        sg_log_buff = new LogBuffer(sg_mmmap_file.data(), kBufferBlockLength, true, _pub_key);
        use_mmap = true;
    } else {
        //mmap失败 使用buffer
        char* buffer = new char[kBufferBlockLength];
        sg_log_buff = new LogBuffer(buffer, kBufferBlockLength, true, _pub_key);
        use_mmap = false;
    }
    ...


    AutoBuffer buffer;
    sg_log_buff->Flush(buffer);

    ScopedLock lock(sg_mutex_log_file);
    sg_logdir = _dir;
    sg_logfileprefix = _nameprefix;
    sg_log_close = false;
    appender_setmode(_mode);
    lock.unlock();
    
    char mark_info[512] = {0};
    get_mark_info(mark_info, sizeof(mark_info));

    if (buffer.Ptr()) {
        __writetips2file("~~~~~ begin of mmap ~~~~~\n");
        __log2file(buffer.Ptr(), buffer.Length(), false);
        __writetips2file("~~~~~ end of mmap ~~~~~%s\n", mark_info);
    }

    tickcountdiff_t get_mmap_time = tickcount_t().gettickcount() - tick;

    char appender_info[728] = {0};
    snprintf(appender_info, sizeof(appender_info), "^^^^^^^^^^" __DATE__ "^^^" __TIME__ "^^^^^^^^^^%s", mark_info);

    xlogger_appender(NULL, appender_info);
    char logmsg[256] = {0};
    snprintf(logmsg, sizeof(logmsg), "get mmap time: %" PRIu64, (int64_t)get_mmap_time);
    xlogger_appender(NULL, logmsg);

    xlogger_appender(NULL, "MARS_URL: " MARS_URL);
    xlogger_appender(NULL, "MARS_PATH: " MARS_PATH);
    xlogger_appender(NULL, "MARS_REVISION: " MARS_REVISION);
    xlogger_appender(NULL, "MARS_BUILD_TIME: " MARS_BUILD_TIME);
    xlogger_appender(NULL, "MARS_BUILD_JOB: " MARS_TAG);

    snprintf(logmsg, sizeof(logmsg), "log appender mode:%d, use mmap:%d", (int)_mode, use_mmap);
    xlogger_appender(NULL, logmsg);
    
    if (!sg_cache_logdir.empty()) {
        boost::filesystem::space_info info = boost::filesystem::space(sg_cache_logdir);
        snprintf(logmsg, sizeof(logmsg), "cache dir space info, capacity:%" PRIuMAX" free:%" PRIuMAX" available:%" PRIuMAX, info.capacity, info.free, info.available);
        xlogger_appender(NULL, logmsg);
    }
    
    boost::filesystem::space_info info = boost::filesystem::space(sg_logdir);
    snprintf(logmsg, sizeof(logmsg), "log dir space info, capacity:%" PRIuMAX" free:%" PRIuMAX" available:%" PRIuMAX, info.capacity, info.free, info.available);
    xlogger_appender(NULL, logmsg);

    BOOT_RUN_EXIT(appender_close);

}
```