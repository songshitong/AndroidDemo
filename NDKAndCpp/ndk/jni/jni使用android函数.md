
在C中打印日志
在CMakeLists.txt中加入
```
find_library(
             log-lib
              log )

target_link_libraries( 
                       native-lib
                       ${log-lib} )
```
然后在cpp文件中加入
```
#include "android/log.h"

#define LOG_TAG "JNI_TEST"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
```
使用方式:
```
int a = 10;
LOGE("xfhy   我是C代码中的日志    a=%d", a);
LOGE("我是xfhy");
```