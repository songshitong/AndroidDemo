

https://fqzhanghao.github.io/post/android-zhong-yu-zhi-dao-log-xian-shi-bu-quan-de-yuan-yin-liao/
Android系统的单条日志打印长度是有限的,默认为4K
Logger.h
```
#define LOGGER_ENTRY_MAX_LEN        (4*1024)  
#define LOGGER_ENTRY_MAX_PAYLOAD    \\  
    (LOGGER_ENTRY_MAX_LEN - sizeof(struct logger_entry))
```
Logcat使用的liblog资源包也提到，使用Log打印的message有可能被log内核驱动缩短：
The message may have been truncated by the kernel log driver.
自己打印日志，需要对日志进行截断