//
// Created by songshitong on 2023/6/2.
//

#ifndef ANDROIDDEMO_CRASH_MONITOR_H
#define ANDROIDDEMO_CRASH_MONITOR_H

typedef void (*CrashCallG)( );

class CrashMonitor {
public:
    void init(CrashCallG call);
    ~CrashMonitor();
};


#endif //ANDROIDDEMO_CRASH_MONITOR_H
