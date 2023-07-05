
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
  header.tid = gettid();
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
    // logd is written under the assumption that the timezone is UTC.
    // If TZ is not set, persist.sys.timezone is looked up in some time utility
    // libc functions, including mktime. It confuses the logd time handling,
    // so here explicitly set TZ to UTC, which overrides the property.
    setenv("TZ", "UTC", 1);
    // issue reinit command. KISS argument parsing.
    if ((argc > 1) && argv[1] && !strcmp(argv[1], "--reinit")) {
        return issueReinit();
    }

    static const char dev_kmsg[] = "/dev/kmsg";
    fdDmesg = android_get_control_file(dev_kmsg);
    if (fdDmesg < 0) {
        fdDmesg = TEMP_FAILURE_RETRY(open(dev_kmsg, O_WRONLY | O_CLOEXEC));
    }

    int fdPmesg = -1;
    bool klogd = __android_logger_property_get_bool(
        "ro.logd.kernel",
        BOOL_DEFAULT_TRUE | BOOL_DEFAULT_FLAG_ENG | BOOL_DEFAULT_FLAG_SVELTE);
    if (klogd) {
        static const char proc_kmsg[] = "/proc/kmsg";
        fdPmesg = android_get_control_file(proc_kmsg);
        if (fdPmesg < 0) {
            fdPmesg = TEMP_FAILURE_RETRY(
                open(proc_kmsg, O_RDONLY | O_NDELAY | O_CLOEXEC));
        }
        if (fdPmesg < 0) android::prdebug("Failed to open %s\n", proc_kmsg);
    }

    // Reinit Thread
    sem_init(&reinit, 0, 0);
    sem_init(&uidName, 0, 0);
    sem_init(&sem_name, 0, 1);
    pthread_attr_t attr;
    if (!pthread_attr_init(&attr)) {
        struct sched_param param;

        memset(&param, 0, sizeof(param));
        pthread_attr_setschedparam(&attr, &param);
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

    // Serves the purpose of managing the last logs times read on a
    // socket connection, and as a reader lock on a range of log
    // entries.

    LastLogTimes* times = new LastLogTimes();

    // LogBuffer is the object which is responsible for holding all
    // log entries.

    logBuf = new LogBuffer(times);

    signal(SIGHUP, reinit_signal_handler);

    if (__android_logger_property_get_bool(
            "logd.statistics", BOOL_DEFAULT_TRUE | BOOL_DEFAULT_FLAG_PERSIST |
                                   BOOL_DEFAULT_FLAG_ENG |
                                   BOOL_DEFAULT_FLAG_SVELTE)) {
        logBuf->enableStatistics();
    }

    // LogReader listens on /dev/socket/logdr. When a client
    // connects, log entries in the LogBuffer are written to the client.

    LogReader* reader = new LogReader(logBuf);
    if (reader->startListener()) {
        exit(1);
    }

    // LogListener listens on /dev/socket/logdw for client
    // initiated log messages. New log entries are added to LogBuffer
    // and LogReader is notified to send updates to connected clients.

    LogListener* swl = new LogListener(logBuf, reader);
    // Backlog and /proc/sys/net/unix/max_dgram_qlen set to large value
    if (swl->startListener(600)) {
        exit(1);
    }

    // Command listener listens on /dev/socket/logd for incoming logd
    // administrative commands.

    CommandListener* cl = new CommandListener(logBuf, reader, swl);
    if (cl->startListener()) {
        exit(1);
    }

    // LogAudit listens on NETLINK_AUDIT socket for selinux
    // initiated log messages. New log entries are added to LogBuffer
    // and LogReader is notified to send updates to connected clients.

    LogAudit* al = nullptr;
    if (auditd) {
        al = new LogAudit(logBuf, reader,
                          __android_logger_property_get_bool(
                              "ro.logd.auditd.dmesg", BOOL_DEFAULT_TRUE)
                              ? fdDmesg
                              : -1);
    }

    LogKlog* kl = nullptr;
    if (klogd) {
        kl = new LogKlog(logBuf, reader, fdDmesg, fdPmesg, al != nullptr);
    }

    readDmesg(al, kl);

    // failure is an option ... messages are in dmesg (required by standard)

    if (kl && kl->startListener()) {
        delete kl;
    }

    if (al && al->startListener()) {
        delete al;
    }

    TEMP_FAILURE_RETRY(pause());

    exit(0);
}
```