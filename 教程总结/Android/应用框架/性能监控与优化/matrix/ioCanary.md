

https://github.com/Tencent/matrix/wiki/Matrix-Android-IOCanary#%E4%B8%80%E5%8E%9F%E7%90%86%E7%AE%80%E8%BF%B0

IOCanary分为四个检测场景：主线程I/O、读写Buffer过小、重复读、Closeable泄漏监控
上面四个场景中，前面三个可以采用native hook的方式收集I/O信息，在close操作时计算并上报。后者可以借StrictMode的东风，这是Android系统底层自带的监控，
通过简单的hook可以将CloseGuard#reporter替换成自己的实现，然后在其report函数中完成上报即可

在Android中I/O的操作最终都会通过native层的open、read、write以及close函数。所以，我们只需要hook这几个函数，然后获取到与函数相关的入参、返回值等，
基于这些信息我们就可以进行I/O操作的检测。最后检测完成之后进行上报。

模块名matrix-io-canary
IOCanaryPlugin 的初始化、start、stop等函数基本都代理给了 IOCanaryCore 这个类。
com/tencent/matrix/iocanary/IOCanaryPlugin.java
com/tencent/matrix/iocanary/core/IOCanaryCore.java
```
 public void start() {
        initDetectorsAndHookers(mIOConfig);
        synchronized (this) {
            mIsStart = true;
        }
    }
    
  private void initDetectorsAndHookers(IOConfig ioConfig) {
        assert ioConfig != null;

        if (ioConfig.isDetectFileIOInMainThread()
            || ioConfig.isDetectFileIOBufferTooSmall()
            || ioConfig.isDetectFileIORepeatReadSameFile()) {
            IOCanaryJniBridge.install(ioConfig, this);
        }

        //if only detect io closeable leak use CloseGuardHooker is Better
        if (ioConfig.isDetectIOClosableLeak()) {
            mCloseGuardHooker = new CloseGuardHooker(this);
            mCloseGuardHooker.hook();
        }
    }   
```
对于 主线程I/O、读写Buffer过小、重复读 这三种 native hook 的场景，由 IOCanaryJniBridge 进行进一步的 hook，而 Closeable泄漏监控 
则由 CloseGuardHooker 进行具体的 hook。


com/tencent/matrix/iocanary/core/IOCanaryJniBridge.java
```
private static final class JavaContext {
        private final String stack;
        private String threadName;

        private JavaContext() {
            //获取调用栈和线程名
            stack = IOCanaryUtil.getThrowableStack(new Throwable());
            if (null != Thread.currentThread()) {
                threadName = Thread.currentThread().getName();
            }
        }
    }
 public static void install(IOConfig config, OnJniIssuePublishListener listener) {
        ...
        //load lib    io-canary
        if (!loadJni()) { 
            MatrixLog.e(TAG, "install loadJni failed");
            return;
        }
        ...
        enableDetector(DetectorType.MAIN_THREAD_IO);  //0  阈值 500ms ns:500*1000
        ...
        enableDetector(DetectorType.SMALL_BUFFER); //1  阈值4096
        ...
        enableDetector(DetectorType.REPEAT_READ); //2   阈值5次
        ...
            //hook
            doHook();
        ...
    }

    private static native void enableDetector(int detectorType);
    private static native boolean doHook();    
```

native 层的入口位于 io_canary_jni.cc 中。在加载 so 时，首先被调用的就是 JNI_OnLoad 方法。
在 JNI_OnLoad 方法会持有 Java 层一些方法、成员变量的句柄，供后续使用
matrix-io-canary\src\main\cpp\io_canary_jni.cc
```
 JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved){
            __android_log_print(ANDROID_LOG_DEBUG, kTag, "JNI_OnLoad");
            kInitSuc = false;

            // 获取Java层一些方法、成员变量的句柄
            //com/tencent/matrix/iocanary/core/IOCanaryJniBridge
            //com/tencent/matrix/iocanary/core/IOCanaryJniBridge$JavaContext
            if (!InitJniEnv(vm)) {
                return -1;
            }
             // 设置上报回调为OnIssuePublish函数
            iocanary::IOCanary::Get().SetIssuedCallback(OnIssuePublish);

            kInitSuc = true;
            __android_log_print(ANDROID_LOG_DEBUG, kTag, "JNI_OnLoad done");
            return JNI_VERSION_1_6;
        }
```

enableDetector 函数会向 IOCanary 这个单例对象中添加对应的 detector 实例
```
        JNIEXPORT void JNICALL
        Java_com_tencent_matrix_iocanary_core_IOCanaryJniBridge_enableDetector(JNIEnv *env, jclass type, jint detector_type) {
            iocanary::IOCanary::Get().RegisterDetector(static_cast<DetectorType>(detector_type));
        }
```

cpp/core/io_canary.cc
```
void IOCanary::RegisterDetector(DetectorType type) {
        switch (type) {
            case DetectorType::kDetectorMainThreadIO:
                detectors_.push_back(new FileIOMainThreadDetector());
                break;
            case DetectorType::kDetectorSmallBuffer:
                detectors_.push_back(new FileIOSmallBufferDetector());
                break;
            case DetectorType::kDetectorRepeatRead:
                detectors_.push_back(new FileIORepeatReadDetector());
                break;
            default:
                break;
        }
    }
```


调用 xHook 来 hook 对应 so 的对应函数
doHook处理
cpp/io_canary_jni.cc
```
//原始函数
 static int (*original_open) (const char *pathname, int flags, mode_t mode);
    static int (*original_open64) (const char *pathname, int flags, mode_t mode);
    static ssize_t (*original_read) (int fd, void *buf, size_t size);
    static ssize_t (*original_read_chk) (int fd, void* buf, size_t count, size_t buf_size);
    static ssize_t (*original_write) (int fd, const void *buf, size_t size);
    static ssize_t (*original_write_chk) (int fd, const void* buf, size_t count, size_t buf_size);
    static int (*original_close) (int fd);
    static int (*original_android_fdsan_close_with_tag) (int fd, uint64_t ownerId);
    
 JNIEXPORT jboolean JNICALL
        Java_com_tencent_matrix_iocanary_core_IOCanaryJniBridge_doHook(JNIEnv *env, jclass type) {
            __android_log_print(ANDROID_LOG_INFO, kTag, "doHook");

            for (int i = 0; i < TARGET_MODULE_COUNT; ++i) {
                //"libopenjdkjvm.so",
                //"libjavacore.so",
                //"libopenjdk.so"
                const char* so_name = TARGET_MODULES[i];
                __android_log_print(ANDROID_LOG_INFO, kTag, "try to hook function in %s.", so_name);
                //使用xhook打开so
                void* soinfo = xhook_elf_open(so_name);
                if (!soinfo) {
                    __android_log_print(ANDROID_LOG_WARN, kTag, "Failure to open %s, try next.", so_name);
                    continue;
                }
                //替换oepn
                xhook_got_hook_symbol(soinfo, "open", (void*)ProxyOpen, (void**)&original_open);
                xhook_got_hook_symbol(soinfo, "open64", (void*)ProxyOpen64, (void**)&original_open64);

                bool is_libjavacore = (strstr(so_name, "libjavacore.so") != nullptr);
                if (is_libjavacore) {
                    //替换read
                    if (xhook_got_hook_symbol(soinfo, "read", (void*)ProxyRead, (void**)&original_read) != 0) {
                        __android_log_print(ANDROID_LOG_WARN, kTag, "doHook hook read failed, try __read_chk");
                        if (xhook_got_hook_symbol(soinfo, "__read_chk", (void*)ProxyReadChk, (void**)&original_read_chk) != 0) {
                            __android_log_print(ANDROID_LOG_WARN, kTag, "doHook hook failed: __read_chk");
                            xhook_elf_close(soinfo);
                            return JNI_FALSE;
                        }
                    }
                    //替换write  
                    if (xhook_got_hook_symbol(soinfo, "write", (void*)ProxyWrite, (void**)&original_write) != 0) {
                        __android_log_print(ANDROID_LOG_WARN, kTag, "doHook hook write failed, try __write_chk");
                        if (xhook_got_hook_symbol(soinfo, "__write_chk", (void*)ProxyWriteChk, (void**)&original_write_chk) != 0) {
                            __android_log_print(ANDROID_LOG_WARN, kTag, "doHook hook failed: __write_chk");
                            xhook_elf_close(soinfo);
                            return JNI_FALSE;
                        }
                    }
                }
                //替换close
                xhook_got_hook_symbol(soinfo, "close", (void*)ProxyClose, (void**)&original_close);
                xhook_got_hook_symbol(soinfo,"android_fdsan_close_with_tag",(void *)Proxy_android_fdsan_close_with_tag,(void**)&original_android_fdsan_close_with_tag);

                xhook_elf_close(soinfo);
            }

            __android_log_print(ANDROID_LOG_INFO, kTag, "doHook done.");
            return JNI_TRUE;
        }
```
xHook使用流程          todo hook了解
调用 xhook_elf_open 打开对应的 so
调用 xhook_hook_symbol hook 对应的方法
调用 xhook_elf_close close 资源，防止资源泄漏
如果需要还原 hook，也是调用 xhook_hook_symbol 进行 hook 点的还原

替换函数的逻辑
```
int ProxyOpen(const char *pathname, int flags, mode_t mode) {
            if(!IsMainThread()) {
                return original_open(pathname, flags, mode);
            }

            int ret = original_open(pathname, flags, mode);

            if (ret != -1) {
                DoProxyOpenLogic(pathname, flags, mode, ret);
            }

            return ret;
        }

     static void DoProxyOpenLogic(const char *pathname, int flags, mode_t mode, int ret) {
                ...//从java context获取statck和线程名称
                jstring j_stack = (jstring) env->GetObjectField(java_context_obj, kFieldIDStack);
                jstring j_thread_name = (jstring) env->GetObjectField(java_context_obj, kFieldIDThreadName);

                char* thread_name = jstringToChars(env, j_thread_name);
                char* stack = jstringToChars(env, j_stack);
                JavaContext java_context(GetCurrentThreadId(), thread_name == NULL ? "" : thread_name, stack == NULL ? "" : stack);
                free(stack);
                free(thread_name);
                 //回调OnOpen
                iocanary::IOCanary::Get().OnOpen(pathname, flags, mode, ret, java_context);
                ...
            }
        }        
```
当 open 等操作执行成功时，才会进入统计、检测流程。
在 open 操作中，会将入参与出参一起作为参数向下层传递，这里的返回值 ret 实际上是指文件描述符 fd。

cpp/core/io_canary.cc
```
  void IOCanary::OnOpen(const char *pathname, int flags, mode_t mode,
                          int open_ret, const JavaContext& java_context) {
        collector_.OnOpen(pathname, flags, mode, open_ret, java_context);
    }
```
core/io_info_collector.cc
```
 void IOInfoCollector::OnOpen(const char *pathname, int flags, mode_t mode
            , int open_ret, const JavaContext& java_context) {
         ...
        std::shared_ptr<IOInfo> info = std::make_shared<IOInfo>(pathname, java_context);
        info_map_.insert(std::make_pair(open_ret, info));
    }
```
以 fd 为 key， pathname、java_context 等值组成的对象 IOInfo 作为 value，保存到 info_map_ 这个 map 中

对于read/write的hook
cpp/core/io_info_collector.cc
```
 void IOInfoCollector::OnRead(int fd, const void *buf, size_t size,
                                 ssize_t read_ret, long read_cost) {
         ...  
        CountRWInfo(fd, FileOpType::kRead, size, read_cost);
    }
    
    void IOInfoCollector::OnWrite(int fd, const void *buf, size_t size,
                                  ssize_t write_ret, long write_cost) {
        ...
        CountRWInfo(fd, FileOpType::kWrite, size, write_cost);
    }    
```
CountRWInfo 会记录 IOInfo 所代表的文件的累计读写操作次数、累计buffer size、累计操作耗时、单次读写最大耗时、当前连续读写操作耗时、最大连续读写操作耗时、
本次操作时间戳、最大操作buffer size、操作类型这些数据
```
void IOInfoCollector::CountRWInfo(int fd, const FileOpType &fileOpType, long op_size, long rw_cost) {
        if (info_map_.find(fd) == info_map_.end()) {
            return;
        }
        // 获取系统的当前时间，单位微秒(us)
        const int64_t now = GetSysTimeMicros();
         // 累计读写操作次数累加   
        info_map_[fd]->op_cnt_ ++;
        // 累计size
        info_map_[fd]->op_size_ += op_size;
        // 累计文件读写耗时
        info_map_[fd]->rw_cost_us_ += rw_cost;
        // 单次文件读写最大耗时
        if (rw_cost > info_map_[fd]->max_once_rw_cost_time_μs_) {
            info_map_[fd]->max_once_rw_cost_time_μs_ = rw_cost;
        }

        // 连续读写耗时，若两次操作超过阈值（8000us，约为一帧耗时16.6667ms的一半），则不累计
        if (info_map_[fd]->last_rw_time_μs_ > 0 && (now - info_map_[fd]->last_rw_time_μs_) < kContinualThreshold) {
            info_map_[fd]->current_continual_rw_time_μs_ += rw_cost;
        } else {
            info_map_[fd]->current_continual_rw_time_μs_ = rw_cost;
        }
        // 最大连续读写耗时
        if (info_map_[fd]->current_continual_rw_time_μs_ > info_map_[fd]->max_continual_rw_cost_time_μs_) {
            info_map_[fd]->max_continual_rw_cost_time_μs_ = info_map_[fd]->current_continual_rw_time_μs_;
        }
        // 本次读写记录的时间戳
        info_map_[fd]->last_rw_time_μs_ = now;
        // 最大读写操作buffer size
        if (info_map_[fd]->buffer_size_ < op_size) {
            info_map_[fd]->buffer_size_ = op_size;
        }
        // 读写操作类型
        if (info_map_[fd]->op_type_ == FileOpType::kInit) {
            info_map_[fd]->op_type_ = fileOpType;
        }
    }
```

在文件经过 open、read、write 之后，终于来到了 close。close 时我们会对整个文件生命周期的一些操作进行最后的统计并通知 detector 进行检测上报。
cpp/io_canary_jni.cc
```
int ProxyClose(int fd) {
    if(!IsMainThread()) {
        return original_close(fd);
    }

    int ret = original_close(fd);

    //__android_log_print(ANDROID_LOG_DEBUG, kTag, "ProxyClose fd:%d ret:%d", fd, ret);
    iocanary::IOCanary::Get().OnClose(fd, ret);

    return ret;
}
```
cpp/core/io_canary.cc
```
 void IOCanary::OnClose(int fd, int close_ret) {
        //统计
        std::shared_ptr<IOInfo> info = collector_.OnClose(fd, close_ret);
        if (info == nullptr) {
            return;
        }

        OfferFileIOInfo(info);
    }

 void IOCanary::OfferFileIOInfo(std::shared_ptr<IOInfo> file_io_info) {
        std::unique_lock<std::mutex> lock(queue_mutex_);
        queue_.push_back(file_io_info);
        queue_cv_.notify_one();
        lock.unlock();
    }    
```
cpp/core/io_info_collector.cc
```
std::shared_ptr<IOInfo> IOInfoCollector::OnClose(int fd, int close_ret) {

        if (info_map_.find(fd) == info_map_.end()) {
            //__android_log_print(ANDROID_LOG_DEBUG, kTag, "OnClose fd:%d not in info_map_", fd);
            return nullptr;
        }

        info_map_[fd]->total_cost_μs_ = GetSysTimeMicros() - info_map_[fd]->start_time_μs_;
        //通过stat函数获取文件大小
        info_map_[fd]->file_size_ = GetFileSize(info_map_[fd]->path_.c_str());
        std::shared_ptr<IOInfo> info = info_map_[fd];
        info_map_.erase(fd);

        return info;
    }
    
int GetFileSize(const char* file_path) {
        struct stat stat_buf;
        if (-1 == stat(file_path, &stat_buf)) {
            return -1;
        }
        return stat_buf.st_size;
    }    
```
这里先调用了 IOInfoCollector#OnClose 方法进行最后的统计操作。
   具体操作为：通过当前系统时间减去 IOInfo 创建的时间得到的文件操作的生命周期的总时间，以及 stat 函数获取文件的 size。最后从 map 中移除并返回该对象。
然后通过 OfferFileIOInfo 方法将此 IOInfo 提交给检测线程中让各个 detector 进行检测


OfferFileIOInfo 的相关实现，这是 C++ 实现的一个生产者消费者模型
cpp/core/io_canary.cc
```
 IOCanary::IOCanary() {
        exit_ = false;
        std::thread detect_thread(&IOCanary::Detect, this); //创建线程检测
        detect_thread.detach();
    }
    
  void IOCanary::Detect() {
        std::vector<Issue> published_issues;
        std::shared_ptr<IOInfo> file_io_info;
        while (true) {
            published_issues.clear();
            //阻塞等待file_io_info生成
            int ret = TakeFileIOInfo(file_io_info);

            if (ret != 0) {
                break;
            }
            //各个detector进行检测
            for (auto detector : detectors_) {
                detector->Detect(env_, *file_io_info, published_issues);
            }
            // 若可以上报，则进行上报
            if (issued_callback_ && !published_issues.empty()) {
                issued_callback_(published_issues);
            }

            file_io_info = nullptr;
        }
    }   
 
 // 消费者
 int IOCanary::TakeFileIOInfo(std::shared_ptr<IOInfo> &file_io_info) {
        std::unique_lock<std::mutex> lock(queue_mutex_);
        //队列为空等待  
        while (queue_.empty()) {
            queue_cv_.wait(lock);
            if (exit_) {
                return -1;
            }
        }
        //唤醒后拿到信息
        file_io_info = queue_.front();
        queue_.pop_front();
        return 0;
    }
 
 //生产者 
 void IOCanary::OfferFileIOInfo(std::shared_ptr<IOInfo> file_io_info) {
        std::unique_lock<std::mutex> lock(queue_mutex_);
        queue_.push_back(file_io_info);
        //唤醒等待线程
        queue_cv_.notify_one();
        lock.unlock();
    }    
```



检测规则
我们前面说到，native 的部分负责 主线程I/O、读写Buffer过小、重复读 这三个方面的检测。实际上，在跟代码的时候，我们就找到了对应的负责检测的类。
主线程I/O
FileIOMainThreadDetector
读写Buffer过小
FileIOSmallBufferDetector
重复读
FileIORepeatReadDetector

主线程I/O
主线程 I/O ，代码里面判定的规则如下：
单次读写最长耗时不得超过13ms
或者连续读写最长耗时不得超过500ms

耗时的 IO 操作不能占据主线程太久。检测条件：
1. 操作线程为主线程
2. 连续读写耗时超过一定阈值或单次 write\read 耗时超过一定阈值

cpp/detector/main_thread_detector.cc
```
void FileIOMainThreadDetector::Detect(const IOCanaryEnv &env, const IOInfo &file_io_info,
                                          std::vector<Issue>& issues) {
        if (GetMainThreadId() == file_io_info.java_context_.thread_id_) {
            int type = 0;
            //单次耗时
            if (file_io_info.max_once_rw_cost_time_μs_ > IOCanaryEnv::kPossibleNegativeThreshold) {
                type = 1;
            }
            //连续读写
            if(file_io_info.max_continual_rw_cost_time_μs_ > env.GetMainThreadThreshold()) {
                type |= 2;
            }

            if (type != 0) {
                Issue issue(kType, file_io_info);
                issue.repeat_read_cnt_ = type;  //use repeat to record type
                PublishIssue(issue, issues);
            }
        }
    }
```


读写Buffer过小
读写Buffer过小，代码里面判定的规则如下：
1 文件累计读写次数超过20次
2 且平均读写buffer小于4096
3 且文件最大连续读写耗时大于等于13ms

Buffer 过小，会导致 read/write 的次数增多，从而影响了性能。 
  多次执行io，io的速度比cpu小很多，代码执行时间会变长
  设置buffer后，会先将内容写入buffer，然后整体写入磁盘，默认的Output流没有缓冲
检测条件：
1. buffer 小于一定阈值
2. read/write 的次数超过一定的阈值
cpp/detector/small_buffer_detector.cc
```
 void FileIOSmallBufferDetector::Detect(const IOCanaryEnv &env, const IOInfo &file_io_info,
                                           std::vector<Issue>& issues) {
        //20次                                     
        if (file_io_info.op_cnt_ > env.kSmallBufferOpTimesThreshold && (file_io_info.op_size_ / file_io_info.op_cnt_) < env.GetSmallBufferThreshold()
                && file_io_info.max_continual_rw_cost_time_μs_ >= env.kPossibleNegativeThreshold) {

            PublishIssue(Issue(kType, file_io_info), issues);
        }
    }
```


重复读
重复读的检测相对于上面两个检测来说，就复杂那么一丢丢。原因在于重复读检测的不仅仅是文件的一个生命周期，而是需要保存一次次检测的输入文件，
然后再整个应用的生命周期内进行检测。

那么，略过一些准备工作，重复读的核心检测语句，在代码里面判定的规则如下：
1 同一文件两次检测的间隔不超过17ms
2 且文件在同一位置(堆栈判断)读取次数超过5次

Wiki描述
如果频繁地读某个文件，证明这个文件的内容很常被用到，可以通过缓存来提高效率。检测条件如下：
1. 同一线程读某个文件的次数超过一定阈值

代码中的判断与 Wiki 中并不完全匹配。检测代码以及部分注释如下：
cpp/detector/repeat_read_detector.cc
```
void FileIORepeatReadDetector::Detect(const IOCanaryEnv &env,
                                          const IOInfo &file_io_info,
                                          std::vector<Issue>& issues) {

        const std::string& path = file_io_info.path_;
        // 若没有发现操作记录且最大连续读写耗时小于13ms则直接return；否则以path作为key保存重复读的记录  
        if (observing_map_.find(path) == observing_map_.end()) {
            if (file_io_info.max_continual_rw_cost_time_μs_ < env.kPossibleNegativeThreshold) {
                return;
            }

            observing_map_.insert(std::make_pair(path, std::vector<RepeatReadInfo>()));
        }
         // 若当前文件操作为写操作，则不构成读，更不用说重复读了。清空key对应的数组
        std::vector<RepeatReadInfo>& repeat_infos = observing_map_[path];
        if (file_io_info.op_type_ == FileOpType::kWrite) {
            repeat_infos.clear();
            return;
        }
        // 构造RepeatReadInfo对象  当前可能为重复读
        RepeatReadInfo repeat_read_info(file_io_info.path_, file_io_info.java_context_.stack_, file_io_info.java_context_.thread_id_,
                                      file_io_info.op_size_, file_io_info.file_size_);

        // 若key对应的数组为空数组，则说明是首次读操作，肯定不会有重复，直接push后结束  
        if (repeat_infos.size() == 0) {
            repeat_infos.push_back(repeat_read_info);
            return;
        }

        // 检查当前时间与栈顶元素的构造时间进行比较，若时间差超过17ms，则说明不构成重复读操作，清除数组。  
        if((GetTickCount() - repeat_infos[repeat_infos.size() - 1].op_timems) > 17) {   //17ms 
            repeat_infos.clear();
        }

        // 从栈中找到与当前对象相等的info，累加重复读次数
        // 注意这里重载了RepeatIOInfo的==操作，java_stack字段也参与了判定，也就是说同一文件不同的使用位置不认为是同一个相等的对象
        bool found = false;
        int repeatCnt;
        for (auto& info : repeat_infos) {
            if (info == repeat_read_info) {
                found = true;

                info.IncRepeatReadCount();

                repeatCnt = info.GetRepeatReadCount();
                break;
            }
        }

        if (!found) {
            repeat_infos.push_back(repeat_read_info);
            return;
        }
         // 检查重复读次数是否超过阈值（5），若超过则进行上报
        if (repeatCnt >= env.GetRepeatReadThreshold()) {
            Issue issue(kType, file_io_info);
            issue.repeat_read_cnt_ = repeatCnt;
            issue.stack = repeat_read_info.GetStack();
            PublishIssue(issue, issues);
        }
    }
```





Closeable泄漏监控
com/tencent/matrix/iocanary/detect/CloseGuardHooker.java
```
 public void hook() {
        MatrixLog.i(TAG, "hook sIsTryHook=%b", mIsTryHook);
        if (!mIsTryHook) {
            boolean hookRet = tryHook();
            MatrixLog.i(TAG, "hook hookRet=%b", hookRet);
            mIsTryHook = true;
        }
    }

private boolean tryHook() {
        try {
            Class<?> closeGuardCls = Class.forName("dalvik.system.CloseGuard");
            Class<?> closeGuardReporterCls = Class.forName("dalvik.system.CloseGuard$Reporter");
            // CloseGuard#getReporter方法
            @SuppressLint("SoonBlockedPrivateApi")
            Method methodGetReporter = closeGuardCls.getDeclaredMethod("getReporter");
            // CloseGuard#setReporter方法
            Method methodSetReporter = closeGuardCls.getDeclaredMethod("setReporter", closeGuardReporterCls);
            Method methodSetEnabled = closeGuardCls.getDeclaredMethod("setEnabled", boolean.class);
            // 保存原始的reporter对象
            sOriginalReporter = methodGetReporter.invoke(null);
            // CloseGuard#setEnabled方法 设置为true
            methodSetEnabled.invoke(null, true);

             // 开启MatrixCloseGuard，这是类似于CloseGuard的一个东西，但是没有用到
            // open matrix close guard also
            MatrixCloseGuard.setEnabled(true);

            ClassLoader classLoader = closeGuardReporterCls.getClassLoader();
            if (classLoader == null) {
                return false;
            }
            //动态代理拿到 reporter方法的调用
            methodSetReporter.invoke(null, Proxy.newProxyInstance(classLoader,
                new Class<?>[]{closeGuardReporterCls},
                new IOCloseLeakDetector(issueListener, sOriginalReporter)));

            return true;
        } catch (Throwable e) {
            MatrixLog.e(TAG, "tryHook exp=%s", e);
        }

        return false;
    }
```
利用反射，把 warnIfOpen 那个 ENABLED 值设为 true
利用动态代理，把 REPORTER 替换成自定义的 proxy
原理参考https://github.com/Tencent/matrix/wiki/Matrix-Android-IOCanary#%E4%BA%8C%E6%97%A0%E4%BE%B5%E5%85%A5%E5%AE%9E%E7%8E%B0%E5%80%9Fstrictmode%E4%B8%9C%E9%A3%8E

com/tencent/matrix/iocanary/detect/IOCloseLeakDetector.java
```
 @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        MatrixLog.i(TAG, "invoke method: %s", method.getName());
        //调用report方法
        if (method.getName().equals("report")) {
            ...
            Throwable throwable = (Throwable) args[1]; //closegaurd上报的异常

            String stackKey = IOCanaryUtil.getThrowableStack(throwable);
            if (isPublished(stackKey)) {
                MatrixLog.d(TAG, "close leak issue already published; key:%s", stackKey);
            } else {
               //将close没有关闭的异常上报
                Issue ioIssue = new Issue(SharePluginInfo.IssueType.ISSUE_IO_CLOSABLE_LEAK);
                ...
                publishIssue(ioIssue);
                MatrixLog.i(TAG, "close leak issue publish, key:%s", stackKey);
                markPublished(stackKey);
            }
            return null;
        }
        //原来方法的调用
        return method.invoke(originalReporter, args);
    }
```
使用CloseGuard可以发现比如文件资源没 close ， Cursor 没有 close 等等


Android P 以上版本的兼容
Matrix 目前只兼容到了Android P，也就是 Android 9。 下面对 Android 9 以上版本的适配，做了一定的兼容，当然下面的代码仅仅说明了适配的方向，
可能还有一些其他问题需要处理。

对于 Closeable 泄露监控来说，在 Android 10 及上无法兼容的原因是 CloseGuard#getReporter 无法直接通过反射获取， 
reporter 字段也是无法直接通过反射获取。如果无法获取到原始的 reporter，那么原始的 reporter 在我们 hook 之后就会失效。
如果我们狠下决心，这也是可以接受的，但是对于这种情况我们应该尽量避免。

那么我们现在的问题就是如何在高版本上获取到原始的 reporter，那么有办法吗？有的，因为我们前面说到了无法直接通过反射获取，但是可以间接获取到。
这里我们可以通过 反射的反射 来获取。实例如下
```
private static void doHook() throws Exception {
    Class<?> clazz = Class.forName("dalvik.system.CloseGuard");
    Class<?> reporterClass = Class.forName("dalvik.system.CloseGuard$Reporter");

    Method setEnabledMethod = clazz.getDeclaredMethod("setEnabled", boolean.class);
    setEnabledMethod.invoke(null, true);

    // 直接反射获取reporter
//        Method getReporterMethod = clazz.getDeclaredMethod("getReporter");
//        final Object originalReporter = getReporterMethod.invoke(null);

    // 反射的反射获取
    Method getDeclaredMethodMethod = Class.class.getDeclaredMethod("getDeclaredMethod", String.class, Class[].class);
    Method getReporterMethod = (Method) getDeclaredMethodMethod.invoke(clazz, "getReporter", null);
    final Object originalReporter = getReporterMethod.invoke(null);

    Method setReporterMethod = clazz.getDeclaredMethod("setReporter", reporterClass);
    Object proxy = Proxy.newProxyInstance(
            reporterClass.getClassLoader(),
            new Class<?>[]{reporterClass},
            new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    return method.invoke(originalReporter, args);
                }
            }
    );
    setReporterMethod.invoke(null, proxy);
}

```