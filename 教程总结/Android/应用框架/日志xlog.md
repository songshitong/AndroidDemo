
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
    xlogger_SetAppender(&xlogger_appender); //将appender设置给xlogger_SetAppender
    
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
        //sg_mmmap_file.data()为映射地址的指针
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
    //写入一些额外信息
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
    ....
    BOOT_RUN_EXIT(appender_close);

}
```
OpenMmapFile
comm/mmap_util.cc
```
bool OpenMmapFile(const char* _filepath, unsigned int _size, boost::iostreams::mapped_file& _mmmap_file) {
   ...
    boost::iostreams::basic_mapped_file_params<boost::filesystem::path> param;
    param.path = boost::filesystem::path(_filepath);
    param.flags = boost::iostreams::mapped_file_base::readwrite;

    bool file_exist = boost::filesystem::exists(_filepath);
    if (!file_exist) {
        param.new_file_size = _size;
    }
    //打开
    _mmmap_file.open(param);

    bool is_open = IsMmapFileOpenSucc(_mmmap_file);
...
    return is_open;
}
```
boost/libs/iostreams/src/mapped_file.cpp
```
void mapped_file_impl::open(param_type p)
{
    if (is_open()) {
        mars_boost::throw_exception(BOOST_IOSTREAMS_FAILURE("file already open"));
        return;
    }
    p.normalize();
    //打开文件,文件描述符handle_ 拿到size 
    open_file(p);
    map_file(p);  // May modify p.hint
    params_ = p;
}

void mapped_file_impl::map_file(param_type& p)
{
    BOOST_TRY {
        try_map_file(p);
    } BOOST_CATCH (const std::exception&) {
        if (p.hint) {
            p.hint = 0;
            try_map_file(p);
        } else {
            BOOST_RETHROW;
        }
    }
    BOOST_CATCH_END
}

void mapped_file_impl::try_map_file(param_type p)
{
    bool priv = p.flags == mapped_file::priv;
    bool readonly = p.flags == mapped_file::readonly;
   ... //BOOST_IOSTREAMS_FD_MMAP为定义的mmap   boost/iostreams/detail/config/rtl.hpp
    void* data = 
        ::BOOST_IOSTREAMS_FD_MMAP( 
            const_cast<char*>(p.hint), 
            size_, //映射文件大小的内存
            readonly ? PROT_READ : (PROT_READ | PROT_WRITE), //是否为只读
            priv ? MAP_PRIVATE : MAP_SHARED, //是否进程共享
            handle_, //文件
            p.offset ); //偏移量
    if (data == MAP_FAILED) {
        cleanup_and_throw("failed mapping file");
        return;
    }
#endif
    data_ = static_cast<char*>(data);
}
```



写日志
logWrite2
log/jni/Java2C_Xlog.cc
```
DEFINE_FIND_STATIC_METHOD(KXlog_logWrite2, KXlog, "logWrite2", "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;IIJJLjava/lang/String;)V")
JNIEXPORT void JNICALL Java_com_tencent_mars_xlog_Xlog_logWrite2
  (JNIEnv *env, jclass, int _level, jstring _tag, jstring _filename,
		  jstring _funcname, jint _line, jint _pid, jlong _tid, jlong _maintid, jstring _log) {
   ...
	XLoggerInfo xlog_info;
	gettimeofday(&xlog_info.timeval, NULL);
	xlog_info.level = (TLogLevel)_level;
	xlog_info.line = (int)_line;
	xlog_info.pid = (int)_pid;
	xlog_info.tid = LONGTHREADID2INT(_tid);
	xlog_info.maintid = LONGTHREADID2INT(_maintid);
	....

	xlog_info.tag = NULL == tag_cstr ? "" : tag_cstr;
	xlog_info.filename = NULL == filename_cstr ? "" : filename_cstr;
	xlog_info.func_name = NULL == funcname_cstr ? "" : funcname_cstr;
	xlogger_Write(&xlog_info, NULL == log_cstr ? "NULL == log" : log_cstr);
   ....
}
```
comm/xlogger/xloggerbase.c
```
void xlogger_Write(const XLoggerInfo* _info, const char* _log) {
	if (NULL != &__xlogger_Write_impl)
		__xlogger_Write_impl(_info, _log);
}

void __xlogger_Write_impl(const XLoggerInfo* _info, const char* _log) {
   ...
    if (NULL == _log) {
        if (_info) {
            XLoggerInfo* info = (XLoggerInfo*)_info;
            info->level = kLevelFatal;
        }
        gs_appender(_info, "NULL == _log");
    } else {
        gs_appender(_info, _log);
    }
}
```
gs_appender的设置在appender_open的xlogger_SetAppender(&xlogger_appender);
log/src/appender.cc
```
void xlogger_appender(const XLoggerInfo* _info, const char* _log) {
    ...
    static Tss s_recursion_str(free);

    if (sg_consolelog_open) ConsoleLog(_info,  _log);

    if (2 <= (int)recursion.Get() && NULL == s_recursion_str.get()) {
        if ((int)recursion.Get() > 10) return;
        char* strrecursion = (char*)calloc(16 * 1024, 1);
        s_recursion_str.set((void*)(strrecursion));

        XLoggerInfo info = *_info;
        info.level = kLevelFatal;

        char recursive_log[256] = {0};
        snprintf(recursive_log, sizeof(recursive_log), "ERROR!!! xlogger_appender Recursive calls!!!, count:%d", (int)recursion.Get());

        PtrBuffer tmp(strrecursion, 0, 16*1024);
        log_formater(&info, recursive_log, tmp);

        strncat(strrecursion, _log, 4096);
        strrecursion[4095] = '\0';

        ConsoleLog(&info,  strrecursion);
    } else {
        if (NULL != s_recursion_str.get()) {
            char* strrecursion = (char*)s_recursion_str.get();
            s_recursion_str.set(NULL);

            __writetips2file(strrecursion);
            free(strrecursion);
        }

        if (kAppednerSync == sg_mode)
            __appender_sync(_info, _log);
        else
            __appender_async(_info, _log);
    }
}


static void __appender_async(const XLoggerInfo* _info, const char* _log) {
    ScopedLock lock(sg_mutex_buffer_async);
   ...
    char temp[16*1024] = {0};       //tell perry,ray if you want modify size.
    PtrBuffer log_buff(temp, 0, sizeof(temp));
    log_formater(_info, _log, log_buff);

    if (sg_log_buff->GetData().Length() >= kBufferBlockLength*4/5) {
       int ret = snprintf(temp, sizeof(temp), "[F][ sg_buffer_async.Length() >= BUFFER_BLOCK_LENTH*4/5, len: %d\n", (int)sg_log_buff->GetData().Length());
       log_buff.Length(ret, ret);
    }

    if (!sg_log_buff->Write(log_buff.Ptr(), (unsigned int)log_buff.Length())) return;

    if (sg_log_buff->GetData().Length() >= kBufferBlockLength*1/3 || (NULL!=_info && kLevelFatal == _info->level)) {
       sg_cond_buffer_async.notifyAll();
    }
```
log/src/log_buffer.cc
sg_log_buff->Write
```
bool LogBuffer::Write(const void* _data, size_t _length) {
   ...
    size_t before_len = buff_.Length();
    size_t write_len = _length;
    
    if (is_compress_) {
        cstream_.avail_in = (uInt)_length;
        cstream_.next_in = (Bytef*)_data;

        uInt avail_out = (uInt)(buff_.MaxLength() - buff_.Length());
        cstream_.next_out = (Bytef*)buff_.PosPtr();
        cstream_.avail_out = avail_out;

        if (Z_OK != deflate(&cstream_, Z_SYNC_FLUSH)) {
            return false;
        }

        write_len = avail_out - cstream_.avail_out;
    } else {
        buff_.Write(_data, _length);
    }
    
    before_len -= remain_nocrypt_len_;
    
    AutoBuffer out_buffer;
    size_t last_remain_len = remain_nocrypt_len_;
    log_crypt_->CryptAsyncLog((char*)buff_.Ptr() + before_len, write_len + remain_nocrypt_len_, out_buffer, remain_nocrypt_len_);
    
    buff_.Write(out_buffer.Ptr(), out_buffer.Length(), before_len);
    
    before_len += out_buffer.Length();
    buff_.Length(before_len, before_len);
   
    log_crypt_->UpdateLogLen((char*)buff_.Ptr(), (uint32_t)(out_buffer.Length() - last_remain_len));

    return true;
}
```
buff_.Write
comm/ptrbuffer.cc
```
void PtrBuffer::Write(const void* _pBuffer, size_t _nLen) {
    Write(_pBuffer, _nLen, Pos());
    Seek(_nLen, kSeekCur);
}

void PtrBuffer::Write(const void* _pBuffer, size_t _nLen, off_t _nPos) {
     ...
    size_t copylen = min(_nLen, max_length_ - _nPos);
    length_ = max(length_, copylen + _nPos);
    //ptr为sg_log_buff创建的logbuffer的mmap的文件指针
    memcpy((unsigned char*)Ptr() + _nPos, _pBuffer, copylen);
}

void*  PtrBuffer::Ptr() {
    return parray_;  //parray_的地址指向创建时传入的指针
}

const void*  PtrBuffer::Ptr() const {
    return parray_;
}
```