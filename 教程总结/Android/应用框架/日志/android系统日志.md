
https://juejin.cn/post/6905368512001556487
https://blog.csdn.net/yiranfeng/article/details/104244900
https://blog.csdn.net/yiranfeng/article/details/104246174

版本9.0.0_r3
http://androidxref.com/9.0.0_r3/xref/frameworks/base/core/java/android/util/Log.java
```
  LOG_ID_MAIN = 0;LOG_ID_RADIO = 1;LOG_ID_EVENTS = 2;LOG_ID_SYSTEM = 3;LOG_ID_CRASH = 4;
  VERBOSE = 2;DEBUG = 3;INFO = 4;WARN = 5;ERROR = 6;ASSERT = 7;
  public static int v(String tag, String msg) {
        return println_native(LOG_ID_MAIN, VERBOSE, tag, msg);
    }
    
  public static native int println_native(int bufID,int priority, String tag, String msg);  
```
frameworks/base/core/jni/android_util_Log.cpp
```
static jint android_util_Log_println_native(JNIEnv* env, jobject clazz,
        jint bufID, jint priority, jstring tagObj, jstring msgObj)
{
   .... //c++接口
    int res = __android_log_buf_write(bufID, (android_LogPriority)priority, tag, msg);
    ...
    return res;
}
```


system/core/liblog/logger_write.c
```
LIBLOG_ABI_PUBLIC int __android_log_buf_write(int bufID, int prio,
                                              const char* tag, const char* msg) {
  struct iovec vec[3];
 ...
  vec[0].iov_base = (unsigned char*)&prio;
  vec[0].iov_len = 1;
  vec[1].iov_base = (void*)tag;
  vec[1].iov_len = strlen(tag) + 1;
  vec[2].iov_base = (void*)msg;
  vec[2].iov_len = strlen(msg) + 1;
  return write_to_log(bufID, vec, 3);
}

iovec定义：system/core/liblog/include/log/uio.h
struct iovec {
  void* iov_base;
  size_t iov_len;
};

static int (*write_to_log)(log_id_t, struct iovec* vec, size_t nr) = __write_to_log_init;

static int __write_to_log_init(log_id_t log_id, struct iovec* vec, size_t nr) {
  int ret, save_errno = errno;
  ...//初始化日志
  __write_to_log_initialize();
....
        __write_to_log_daemon(log_id, vec, nr);
 ...
  return ret;
}

static int __write_to_log_initialize() {
  struct android_log_transport_write* transport;
  struct listnode* n;
  int i = 0, ret = 0;

  __android_log_config_write();
  。。。
  return ret;
}
```
android_log_transport_write是关键，里面有open,close,write
system/core/liblog/logd_writer.c  
```
LIBLOG_HIDDEN struct android_log_transport_write logdLoggerWrite = {
  .node = { &logdLoggerWrite.node, &logdLoggerWrite.node },
  .context.sock = -EBADF,
  .name = "logd",
  .available = logdAvailable,
  .open = logdOpen,
  .close = logdClose,
  .write = logdWrite,
};
```

system/core/liblog/config_write.c
```
LIBLOG_HIDDEN void __android_log_config_write() {
 ...
  if ((__android_log_transport == LOGGER_DEFAULT) ||
      (__android_log_transport & LOGGER_LOGD)) {
#if (FAKE_LOG_DEVICE == 0)
    extern struct android_log_transport_write logdLoggerWrite;
    extern struct android_log_transport_write pmsgLoggerWrite;
    //向__android_log_transport_write添加logdLoggerWrite
    __android_log_add_transport(&__android_log_transport_write,
                                &logdLoggerWrite);
    __android_log_add_transport(&__android_log_persist_write, &pmsgLoggerWrite);
....
  }
 ...
}
```

system/core/liblog/logger_write.c
```
static int __write_to_log_daemon(log_id_t log_id, struct iovec* vec, size_t nr) {
  struct android_log_transport_write* node;
  int ret, save_errno;
  struct timespec ts;
  size_t len, i;
  //计算日志总的长度
  for (len = i = 0; i < nr; ++i) {
    len += vec[i].iov_len;
  }
 ...
  save_errno = errno;
#if defined(__ANDROID__)
  clock_gettime(android_log_clockid(), &ts);
   ...//省略日志格式校验
   //获取时间
  /* simulate clock_gettime(CLOCK_REALTIME, &ts); */
  {
    struct timeval tv;
    gettimeofday(&tv, NULL);
    ts.tv_sec = tv.tv_sec;
    ts.tv_nsec = tv.tv_usec * 1000;
  }
#endif

  ret = 0;
  i = 1 << log_id;
  //写日志
  write_transport_for_each(node, &__android_log_transport_write) {
    if (node->logMask & i) {
      ssize_t retval;  //调用transport的write
      retval = (*node->write)(log_id, &ts, vec, nr);
      if (ret >= 0) {
        ret = retval;
      }
    }
  }

  write_transport_for_each(node, &__android_log_persist_write) {
    if (node->logMask & i) {
      (void)(*node->write)(log_id, &ts, vec, nr);
    }
  }

  errno = save_errno;
  return ret;
}

```

transport的open和write
system/core/liblog/logd_writer.c
```
static int logdOpen() {
  int i, ret = 0;

  i = atomic_load(&logdLoggerWrite.context.sock);
  if (i < 0) {
    //创建socket
    int sock = TEMP_FAILURE_RETRY(
        socket(PF_UNIX, SOCK_DGRAM | SOCK_CLOEXEC | SOCK_NONBLOCK, 0));
    if (sock < 0) {
      ret = -errno;
    } else {
      struct sockaddr_un un;
      memset(&un, 0, sizeof(struct sockaddr_un));
      un.sun_family = AF_UNIX;
      strcpy(un.sun_path, "/dev/socket/logdw");
      //连接socket 路径为/dev/socket/logdw
      if (TEMP_FAILURE_RETRY(connect(sock, (struct sockaddr*)&un,
                                     sizeof(struct sockaddr_un))) < 0) {
       。。。
    }
  }
  return ret;
}
```
logdWrite
```
///system/core/liblog/include/private/android_logger.h  header的结构体
typedef struct __attribute__((__packed__)) {
  typeof_log_id_t id;
  uint16_t tid;
  log_time realtime;
} android_log_header_t;

static int logdWrite(log_id_t logId, struct timespec* ts, struct iovec* vec,
                     size_t nr) {
  ssize_t ret;
  int sock;
  static const unsigned headerLength = 1;
  struct iovec newVec[nr + headerLength];
  android_log_header_t header;
  size_t i, payloadSize;
  static atomic_int_fast32_t dropped;
  static atomic_int_fast32_t droppedSecurity;

  sock = atomic_load(&logdLoggerWrite.context.sock);
...
  header.tid = gettid(); //获取当前线程tid
  header.realtime.tv_sec = ts->tv_sec;
  header.realtime.tv_nsec = ts->tv_nsec;

  newVec[0].iov_base = (unsigned char*)&header;
  newVec[0].iov_len = sizeof(header);

  if (sock >= 0) {
    int32_t snapshot =
        atomic_exchange_explicit(&droppedSecurity, 0, memory_order_relaxed);
    ....
  header.id = logId;
  //备份
  for (payloadSize = 0, i = headerLength; i < nr + headerLength; i++) {
    newVec[i].iov_base = vec[i - headerLength].iov_base;
    payloadSize += newVec[i].iov_len = vec[i - headerLength].iov_len;

    if (payloadSize > LOGGER_ENTRY_MAX_PAYLOAD) {
      newVec[i].iov_len -= payloadSize - LOGGER_ENTRY_MAX_PAYLOAD;
      if (newVec[i].iov_len) {
        ++i;
      }
      break;
    }
  }  
  ...  //调用writev写   todo
  ret = TEMP_FAILURE_RETRY(writev(sock, newVec, i));
  ...
  return ret;
}
```
pmsgLoggerWrite与logdLoggerWrite类似，先不看了


logd进程
system/core/logd/logd.rc
```
service logd /system/bin/logd
    socket logd stream 0666 logd logd
    socket logdr seqpacket 0666 logd logd
    socket logdw dgram+passcred 0222 logd logd
    file /proc/kmsg r
    file /dev/kmsg w
    user logd
    group logd system package_info readproc
    writepid /dev/cpuset/system-background/tasks

...
```
建立三个socket
logd 接收logcat 传递的指令然后处理 ，比如logcat -g， logcat -wrap等
logdr logcat从此buffer中读取buffer
logdw 日志写入的buffer

system/core/logd/main.cpp 
```
int main(int argc, char* argv[]) {
    //默认timezone为utc
    setenv("TZ", "UTC", 1);
    ...
    static const char dev_kmsg[] = "/dev/kmsg";
    fdDmesg = android_get_control_file(dev_kmsg); //打开/dev/kmsg
    ...
    int fdPmesg = -1;
    bool klogd = __android_logger_property_get_bool(
        "ro.logd.kernel",
        BOOL_DEFAULT_TRUE | BOOL_DEFAULT_FLAG_ENG | BOOL_DEFAULT_FLAG_SVELTE);
    if (klogd) {
        static const char proc_kmsg[] = "/proc/kmsg";
        fdPmesg = android_get_control_file(proc_kmsg); // 打开/proc/kmsg
       。。。
    }

    // 创建线程 reinit_thread_start 用于重新初始化logBuf
    ...
    pthread_attr_t attr;
    if (!pthread_attr_init(&attr)) {
        ...
        pthread_attr_setschedpolicy(&attr, SCHED_BATCH);
        if (!pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED)) {
            pthread_t thread;
            reinit_running = true;
            if (pthread_create(&thread, &attr, reinit_thread_start, nullptr)) {
                reinit_running = false;
            }
        }
        pthread_attr_destroy(&attr);
    }

    bool auditd =
        __android_logger_property_get_bool("ro.logd.auditd", BOOL_DEFAULT_TRUE);
    if (drop_privs(klogd, auditd) != 0) {
        return -1;
    }

    //读取的最后日志时间
    LastLogTimes* times = new LastLogTimes();
    //LogBuffer是负责保存所有日志项的对象
    logBuf = new LogBuffer(times);

    signal(SIGHUP, reinit_signal_handler);

    if (__android_logger_property_get_bool(
            "logd.statistics", BOOL_DEFAULT_TRUE | BOOL_DEFAULT_FLAG_PERSIST |
                                   BOOL_DEFAULT_FLAG_ENG |
                                   BOOL_DEFAULT_FLAG_SVELTE)) {
        logBuf->enableStatistics();
    }

    //监听dev/socket/logdr
    LogReader* reader = new LogReader(logBuf);
    if (reader->startListener()) {
        exit(1);
    }

    //监听/dev/socket/logdw
    LogListener* swl = new LogListener(logBuf, reader);
    ...

    //监听传入的logd 的command，即监听是否有命令发送给logd
    CommandListener* cl = new CommandListener(logBuf, reader, swl);
    ...
    LogAudit* al = nullptr;
    //启动LogAudit，LogAudit 在NETLINK_AUDIT的socket上侦听selinux启动的日志消息
    if (auditd) {
        al = new LogAudit(logBuf, reader,
                          __android_logger_property_get_bool(
                              "ro.logd.auditd.dmesg", BOOL_DEFAULT_TRUE)
                              ? fdDmesg
                              : -1);
    }
    //启动LogKlog，用来存储内核日志
    LogKlog* kl = nullptr;
    if (klogd) {
        kl = new LogKlog(logBuf, reader, fdDmesg, fdPmesg, al != nullptr);
    }
    //通过 LogAudit和 LogKlog来分别读取selinux和kernel的日志
    readDmesg(al, kl);
    ...
}


static void* reinit_thread_start(void* /*obj*/) {
    ...
    while (reinit_running && !sem_wait(&reinit) && reinit_running) {
         ...
        //在/dev/kmsg写入logd.daemon: renit
        if (fdDmesg >= 0) {
            static const char reinit_message[] = { KMSG_PRIORITY(LOG_INFO),
                                                   'l',
                                                   'o',
                                                   'g',
                                                   'd',
                                                   '.',
                                                   'd',
                                                   'a',
                                                   'e',
                                                   'm',
                                                   'o',
                                                   'n',
                                                   ':',
                                                   ' ',
                                                   'r',
                                                   'e',
                                                   'i',
                                                   'n',
                                                   'i',
                                                   't',
                                                   '\n' };
            write(fdDmesg, reinit_message, sizeof(reinit_message));
        }
        //重新初始化各个log buffer的大小，以及其他参数的初始化
        if (logBuf) {
            logBuf->init();
            logBuf->initPrune(nullptr);
        }
        android::ReReadEventLogTags();
    }
    return nullptr;
}
```
system/core/logd/LogBuffer.cpp
logBuf->init()
```
void LogBuffer::init() {
    log_id_for_each(i) {
        mLastSet[i] = false;
        mLast[i] = mLogElements.begin();
        if (setSize(i, __android_logger_get_buffer_size(i))) {
            //system/core/include/private/android_logger.h  64K
            setSize(i, LOG_BUFFER_MIN_SIZE);
        }
    }
    ...
}
```

LogReader创建
system/core/logd/LogReader.cpp 
建立logdr的socket服务端
```
LogReader::LogReader(LogBuffer* logbuf)
    : SocketListener(getLogSocket(), true), mLogbuf(*logbuf) {
}
int LogReader::getLogSocket() {
    static const char socketName[] = "logdr";
    int sock = android_get_control_socket(socketName);

    if (sock < 0) {
        sock = socket_local_server(
            socketName, ANDROID_SOCKET_NAMESPACE_RESERVED, SOCK_SEQPACKET);
    }
    return sock;
}
```
对于SocketListener 当数据来临时调用onDataAvailable
/system/core/libsysutils/src/SocketListener.cpp
```
int SocketListener::startListener(int backlog) {
   ...
   //启动线程
    if (pthread_create(&mThread, NULL, SocketListener::threadStart, this)) {
        SLOGE("pthread_create (%s)", strerror(errno));
        return -1;
    }
    return 0;
}

void *SocketListener::threadStart(void *obj) {
    SocketListener *me = reinterpret_cast<SocketListener *>(obj);
    me->runListener();
    pthread_exit(NULL);
    return NULL;
}

void SocketListener::runListener() {
    while(1) {
       。。。
        for (it = mClients->begin(); it != mClients->end(); ++it) {
            SocketClient* c = *it;
            // NB: calling out to an other object with mClientsLock held (safe)
            int fd = c->getSocket();
            if (FD_ISSET(fd, &read_fds)) {
                pendingList.push_back(c);
                c->incRef();
            }
        }
        pthread_mutex_unlock(&mClientsLock);

        while (!pendingList.empty()) {
            /* Pop the first item from the list */
            it = pendingList.begin();
            SocketClient* c = *it;
            pendingList.erase(it);
            //回调onDataAvailable
            if (!onDataAvailable(c)) {
                release(c, false);
            }
            c->decRef();
        }
    }
}
```

LogListener的创建
system/core/logd/LogListener.cpp
建立logdw的服务端
```
LogListener::LogListener(LogBufferInterface* buf, LogReader* reader)
    : SocketListener(getLogSocket(), false), logbuf(buf), reader(reader) {
}
int LogListener::getLogSocket() {
    static const char socketName[] = "logdw";
    int sock = android_get_control_socket(socketName);
    if (sock < 0) {  // logd started up in init.sh
        sock = socket_local_server(
            socketName, ANDROID_SOCKET_NAMESPACE_RESERVED, SOCK_DGRAM);

        int on = 1;
        if (setsockopt(sock, SOL_SOCKET, SO_PASSCRED, &on, sizeof(on))) {
            return -1;
        }
    }
    return sock;
}


bool LogListener::onDataAvailable(SocketClient* cli) {
    ...
    char buffer[sizeof_log_id_t + sizeof(uint16_t) + sizeof(log_time) +
                LOGGER_ENTRY_MAX_PAYLOAD + 1];
    struct iovec iov = { buffer, sizeof(buffer) - 1 };

    alignas(4) char control[CMSG_SPACE(sizeof(struct ucred))];
    struct msghdr hdr = {
        NULL, 0, &iov, 1, control, sizeof(control), 0,
    };

    int socket = cli->getSocket();

    ...
    ssize_t n = recvmsg(socket, &hdr, 0);
    if (n <= (ssize_t)(sizeof(android_log_header_t))) {
        return false;
    }

    buffer[n] = 0;

    struct ucred* cred = NULL;
    ...//省略日志检查
    // Check credential validity, acquire corrected details if not supplied.
    //设置uid,pid
    if (cred->pid == 0) {
        cred->pid = logbuf ? logbuf->tidToPid(header->tid)
                           : android::tidToPid(header->tid);
        if (cred->pid == getpid()) {
           ...
            return false;  // ignore self
        }
    }
    if (cred->uid == DEFAULT_OVERFLOWUID) {
        uid_t uid =
            logbuf ? logbuf->pidToUid(cred->pid) : android::pidToUid(cred->pid);
        if (uid == AID_LOGD) {
            uid = logbuf ? logbuf->pidToUid(header->tid)
                         : android::pidToUid(cred->pid);
        }
        if (uid != AID_LOGD) cred->uid = uid;
    }

    char* msg = ((char*)buffer) + sizeof(android_log_header_t);
    n -= sizeof(android_log_header_t);
    ...
    if (logbuf != nullptr) {
        //调用logbuf.log保存日志
        int res = logbuf->log(
            logId, header->realtime, cred->uid, cred->pid, header->tid, msg,
            ((size_t)n <= USHRT_MAX) ? (unsigned short)n : USHRT_MAX);
        if (res > 0 && reader != nullptr) {
            //通知reader有新日志生成
            reader->notifyNewLog(static_cast<log_mask_t>(1 << logId));
        }
    }
    return true;
}
```
system/core/logd/LogBuffer.cpp
```
int LogBuffer::log(log_id_t log_id, log_time realtime, uid_t uid, pid_t pid,
                   pid_t tid, const char* msg, unsigned short len) {
    ...
    ...
    LogBufferElement* elem =
        new LogBufferElement(log_id, realtime, uid, pid, tid, msg, len);
    if (log_id != LOG_ID_SECURITY) {
        int prio = ANDROID_LOG_INFO;
        const char* tag = nullptr;
        size_t tag_len = 0;
        if (log_id == LOG_ID_EVENTS || log_id == LOG_ID_STATS) {
            tag = tagToName(elem->getTag()); //拿到msg的tag
            if (tag) {
                tag_len = strlen(tag);
            }
        } else {
            prio = *msg;
            tag = msg + 1;
            tag_len = strnlen(tag, len - 1);
        }
        if (!__android_log_is_loggable_len(prio, tag, tag_len,
                                           ANDROID_LOG_VERBOSE)) {
            // Log traffic received to total
            wrlock();
            stats.addTotal(elem); //统计增加
            unlock();
            delete elem;
            return -EACCES;
        }
    }

    。。。
    lastLoggedElements[log_id] = new LogBufferElement(*elem);

    log(elem);
    unlock();

    return len;
}

void LogBuffer::log(LogBufferElement* elem) {
    ... //添加到LogBufferElementCollection    定义为typedef std::list<LogBufferElement*> LogBufferElementCollection;
    mLogElements.push_back(elem);
    ...
}
```



读取日志
读取日志时，主要通过logcat工具来抓取。
Logcat是通过liblog 连接 "/dev/socket/logdr" 来获取日志
system/core/logcat/logcat_main.cpp
```
int main(int argc, char** argv, char** envp) {
    ..
    int retval = android_logcat_run_command(ctx, -1, -1, argc, argv, envp);
   ...
    return ret;
}
```
system/core/logcat/logcat.cpp
```
int android_logcat_run_command(android_logcat_context ctx,
                               int output, int error,
                               int argc, char* const* argv,
                               char* const* envp) {
    android_logcat_context_internal* context = ctx;
    context->output_fd = output;
    context->error_fd = error;
    context->argc = argc;
    context->argv = argv;
    context->envp = envp;
    context->stop = false;
    context->thread_stopped = false;
    return __logcat(context);
}

static int __logcat(android_logcat_context_internal* context) {
   ...
   struct logger_list* logger_list;
   ...
   ret = getopt_long_r(argc, argv, ":cdDhLt:T:gG:sQf:r:n:v:b:BSpP:m:e:",
                            long_options, &option_index, &optctx);
   switch (ret) { //解析命令
     ...
     case 'c':
                clearLog = true;
                mode |= ANDROID_LOG_WRONLY;
                break;

            case 'L':
                mode |= ANDROID_LOG_RDONLY | ANDROID_LOG_PSTORE |
                        ANDROID_LOG_NONBLOCK;
                break;
     ...           
   }
   ...
  while (!context->stop &&
           (!context->maxCount || (context->printCount < context->maxCount))) {
        struct log_msg log_msg;
        //读取log
        int ret = android_logger_list_read(logger_list, &log_msg);
   ...                                         
}
```
system/core/liblog/logger_read.c
```
LIBLOG_ABI_PUBLIC int android_logger_list_read(struct logger_list* logger_list,
                                               struct log_msg* log_msg) {
  struct android_log_transport_context* transp;
  struct android_log_logger_list* logger_list_internal =
      (struct android_log_logger_list*)logger_list;

  int ret = init_transport_context(logger_list_internal);
  ...
  
  return android_transport_read(logger_list_internal, transp, log_msg);
}

static int init_transport_context(struct android_log_logger_list* logger_list) {
  struct android_log_transport_read* transport;
  struct listnode* node;
  ...
  __android_log_lock();
  /* mini __write_to_log_initialize() to populate transports */
  if (list_empty(&__android_log_transport_read) &&
      list_empty(&__android_log_persist_read)) {
    ..//加载  transport  一般为logdLoggerRead，pmsgLoggerRead，localLoggerRead
    __android_log_config_read();
  }
  __android_log_unlock();

 ... //配置transp
    transp = calloc(1, sizeof(*transp));
    if (!transp) {
      return -ENOMEM;
    }
    transp->parent = logger_list;
    transp->transport = transport;
    transp->logMask = logMask;
    transp->ret = 1;
    list_add_tail(&logger_list->transport, &transp->node);
  }
  ..
  return 0;
}
```
/system/core/liblog/config_read.c
```
LIBLOG_HIDDEN void __android_log_config_read() {
  if (__android_log_transport & LOGGER_LOCAL) {
    extern struct android_log_transport_read localLoggerRead;

    __android_log_add_transport(&__android_log_transport_read, &localLoggerRead);
  }

#if (FAKE_LOG_DEVICE == 0)
  if ((__android_log_transport == LOGGER_DEFAULT) ||
      (__android_log_transport & LOGGER_LOGD)) {
    extern struct android_log_transport_read logdLoggerRead;
    extern struct android_log_transport_read pmsgLoggerRead;
     
    __android_log_add_transport(&__android_log_transport_read, &logdLoggerRead);
    __android_log_add_transport(&__android_log_persist_read, &pmsgLoggerRead);
  }
#endif
}
```
system/core/liblog/logger_read.c
```
static int android_transport_read(struct android_log_logger_list* logger_list,
                                  struct android_log_transport_context* transp,
                                  struct log_msg* log_msg) {
  //执行read方法                                
  int ret = (*transp->transport->read)(logger_list, transp, log_msg);
  ...
}
```
system/core/liblog/logd_reader.c
```
LIBLOG_HIDDEN struct android_log_transport_read logdLoggerRead = {
  .node = { &logdLoggerRead.node, &logdLoggerRead.node },
  .name = "logd",
  .available = logdAvailable,
  .version = logdVersion,
  .read = logdRead,
  .poll = logdPoll,
  .close = logdClose,
  .clear = logdClear,
  .getSize = logdGetSize,
  .setSize = logdSetSize,
  .getReadableSize = logdGetReadableSize,
  .getPrune = logdGetPrune,
  .setPrune = logdSetPrune,
  .getStats = logdGetStats,
};

static int logdRead(struct android_log_logger_list* logger_list,
                    struct android_log_transport_context* transp,
                    struct log_msg* log_msg) {
  int ret, e;
  struct sigaction ignore;
  struct sigaction old_sigaction;
  unsigned int old_alarm = 0;
  //打开socket
  ret = logdOpen(logger_list, transp);
  ...
  //接受 todo
  ret = recv(ret, log_msg, LOGGER_ENTRY_MAX_LEN, 0);
  e = errno;

  if (new_alarm) {
    if ((ret == 0) || (e == EINTR)) {
      e = EAGAIN;
      ret = -1;
    }
    alarm(old_alarm);
    sigaction(SIGALRM, &old_sigaction, NULL);
  }

  if ((ret == -1) && e) {
    return -e;
  }
  return ret;
}

static int logdOpen(struct android_log_logger_list* logger_list,
                    struct android_log_transport_context* transp) {
  struct android_log_logger* logger;
  struct sigaction ignore;
  struct sigaction old_sigaction;
  unsigned int old_alarm = 0;
  char buffer[256], *cp, c;
  int e, ret, remaining, sock;
   //创建logdr的客户端
  sock = socket_local_client("logdr", ANDROID_SOCKET_NAMESPACE_RESERVED,
                             SOCK_SEQPACKET);
  。。。
  strcpy(buffer, (logger_list->mode & ANDROID_LOG_NONBLOCK) ? "dumpAndClose"
                                                            : "stream");
  cp = buffer + strlen(buffer);

  strcpy(cp, " lids");
  cp += 5;
  c = '=';
  remaining = sizeof(buffer) - (cp - buffer);
 。。。
  return sock;
}
```

reader侧
notifyNewLog
system/core/logd/LogReader.cpp
```
void LogReader::notifyNewLog(log_mask_t logMask) {
    FlushCommand command(*this, logMask);
    runOnEachSocket(&command); //调用socket接口，最终进入 logd的runSocketCommand()  todo
}
```
后续触发封装数据监听的onDataAvailable
```
bool LogReader::onDataAvailable(SocketClient* cli) {
    static bool name_set;
   ...
    char buffer[255];
    //读取客户端传入参数
    int len = read(cli->getSocket(), buffer, sizeof(buffer) - 1);
    ..
    buffer[len] = '\0';

   ...
     //LogBuffer中的日志写入传入的client，即写入logcat
        logbuf().flushTo(cli, sequence, nullptr, FlushCommand::hasReadLogs(cli),
                         FlushCommand::hasSecurityLogs(cli),
                         logFindStart.callback, &logFindStart);

       ..
    }

    ...
    FlushCommand command(*this, nonBlock, tail, logMask, pid, sequence, timeout);

    // Set acceptable upper limit to wait for slow reader processing b/27242723
    struct timeval t = { LOGD_SNDTIMEO, 0 };
    setsockopt(cli->getSocket(), SOL_SOCKET, SO_SNDTIMEO, (const char*)&t,
               sizeof(t));

    command.runSocketCommand(cli);
    return true;
}
```

根据tid获取名字
/system/core/logd/LogBufferElement.cpp
```
char* android::tidToName(pid_t tid) {
    char* retval = NULL;
    char buffer[256];
    snprintf(buffer, sizeof(buffer), "/proc/%u/comm", tid);
    int fd = open(buffer, O_RDONLY);
    if (fd >= 0) {
        ssize_t ret = read(fd, buffer, sizeof(buffer));
        if (ret >= (ssize_t)sizeof(buffer)) {
            ret = sizeof(buffer) - 1;
        }
        while ((ret > 0) && isspace(buffer[ret - 1])) {
            --ret;
        }
        if (ret > 0) {
            buffer[ret] = '\0';
            retval = strdup(buffer);
        }
        close(fd);
    }

    // if nothing for comm, check out cmdline
    char* name = android::pidToName(tid);
    if (!retval) {
        retval = name;
        name = NULL;
    }

    if (name) {
        // impossible for retval to be NULL if name not NULL
        size_t retval_len = strlen(retval);
        size_t name_len = strlen(name);
        // KISS: ToDo: Only checks prefix truncated, not suffix, or both
        if ((retval_len < name_len) &&
            !fastcmp<strcmp>(retval, name + name_len - retval_len)) {
            free(retval);
            retval = name;
        } else {
            free(name);
        }
    }
    return retval;
}
```