//
// Created by songshitong on 2023/6/2.
//

#ifndef ANDROIDDEMO_CRASH_MONITOR_H
#define ANDROIDDEMO_CRASH_MONITOR_H

#include <jni.h>
#include <functional>

typedef std::function<void(char*)> CrashCallG;
class CrashMonitor {
public:
    void init(CrashCallG call);
    ~CrashMonitor();
};

#endif //ANDROIDDEMO_CRASH_MONITOR_H
